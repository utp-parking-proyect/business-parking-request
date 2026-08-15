package com.utp.request.service.impl;

import com.utp.request.client.portal.PortalServiceClient;
import com.utp.request.generated.client.users.model.CycleResponse;
import com.utp.request.generated.client.users.model.UserResponse;
import com.utp.request.generated.model.ApplicantInformation;
import com.utp.request.generated.model.ParkingRequestDetail;
import com.utp.request.generated.model.ParkingRequestIn;
import com.utp.request.generated.model.ParkingRequestInformation;
import com.utp.request.generated.model.ParkingRequestInformationList;
import com.utp.request.generated.model.VehicleInformation;
import com.utp.request.generated.model.WorkflowEntry;
import com.utp.request.mapper.ParkingRequestInformationMapper;
import com.utp.request.model.entity.Request;
import com.utp.request.model.entity.Status;
import com.utp.request.model.entity.Vehicle;
import com.utp.request.model.entity.VehicleType;
import com.utp.request.repository.RequestRepository;
import com.utp.request.repository.StatusRepository;
import com.utp.request.repository.VehicleRepository;
import com.utp.request.repository.VehicleTypeRepository;
import com.utp.request.repository.WorkflowRepository;
import com.utp.request.service.AcceptorSelector;
import com.utp.request.service.RequestService;
import com.utp.request.service.WorkflowService;
import com.utp.request.util.Constants;
import com.utp.request.util.NumberPlateValidator;
import com.utp.request.util.error.ConflictException;
import com.utp.request.util.error.ForbiddenException;
import com.utp.request.util.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

  private final RequestRepository requestRepository;
  private final VehicleRepository vehicleRepository;
  private final WorkflowRepository workflowRepository;
  private final StatusRepository statusRepository;
  private final VehicleTypeRepository vehicleTypeRepository;
  private final PortalServiceClient portalServiceClient;
  private final AcceptorSelector acceptorSelector;
  private final WorkflowService workflowService;
  private final TransactionalOperator transactionalOperator;
  private final ParkingRequestInformationMapper parkingRequestInformationMapper;

  @Override
  public Mono<Request> saveNewRequest(Long authenticatedUserId, ParkingRequestIn request) {
    Integer userId = authenticatedUserId.intValue();
    return Mono.zip(portalServiceClient.getCurrentCycle(), portalServiceClient.getUserById(authenticatedUserId))
        .flatMap(tuple -> createRequestForCycle(userId, request, tuple.getT1().getIdCycle().intValue(),
            resolveCampusId(tuple.getT2()))
            .as(transactionalOperator::transactional));
  }

  private Mono<Request> createRequestForCycle(Integer userId, ParkingRequestIn request, Integer idCycle,
                                              Long idCampus) {
    String numberPlate = NumberPlateValidator.normalize(request.getNumberPlate());

    return validateRequestsPerCycleLimit(userId, idCycle)
        .then(Mono.defer(() -> resolveVehicle(userId, numberPlate, request.getVehicleType())))
        .flatMap(vehicle -> validateNoActiveRequest(vehicle.getIdVehicle(), idCycle)
            .then(Mono.defer(() -> createRequestWithWorkflow(vehicle.getIdVehicle(), userId, idCycle,
                idCampus))));
  }

  private Mono<Void> validateRequestsPerCycleLimit(Integer userId, Integer idCycle) {
    return requestRepository.countByApplicantUserIdAndIdCycle(userId, idCycle)
        .defaultIfEmpty(0L)
        .flatMap(count -> count >= Constants.MAX_REQUESTS_PER_CYCLE
            ? Mono.error(new ConflictException(Constants.ERROR_MAX_REQUESTS_PER_CYCLE_REACHED))
            : Mono.empty());
  }

  @Override
  public Mono<Request> resubmitRequest(Long authenticatedUserId, Integer requestId, String observation) {
    Integer userId = authenticatedUserId.intValue();
    String resolvedObservation = (observation == null || observation.isBlank())
        ? Constants.OBSERVATION_RESUBMITTED_DEFAULT
        : observation;

    return Mono.zip(portalServiceClient.getCurrentCycle(), portalServiceClient.getUserById(authenticatedUserId))
        .flatMap(tuple -> resubmitRequestForCycle(userId, requestId, resolvedObservation,
            tuple.getT1().getIdCycle().intValue(), resolveCampusId(tuple.getT2()))
            .as(transactionalOperator::transactional));
  }

  private Mono<Request> resubmitRequestForCycle(Integer userId, Integer requestId, String observation,
                                                Integer currentIdCycle, Long idCampus) {
    return requestRepository.findById(requestId)
        .switchIfEmpty(Mono.error(new NotFoundException(Constants.ERROR_REQUEST_NOT_FOUND)))
        .flatMap(existing -> validateResubmit(userId, existing, currentIdCycle)
            .then(Mono.defer(() -> {
              LocalDateTime now = LocalDateTime.now();
              return requestRepository.updateStatusAndResponse(requestId, Constants.ID_STATUS_RESUBMITTED, null)
                  .then(workflowRepository.saveWorkflow(requestId, Constants.ID_STATUS_RESUBMITTED, now,
                      observation))
                  .then(Mono.defer(() -> assignAcceptor(requestId, idCampus)))
                  .then(Mono.defer(() -> requestRepository.findById(requestId)));
            })));
  }

  private Long resolveCampusId(UserResponse applicant) {
    return applicant.getCampus() == null ? null : applicant.getCampus().getIdCampus();
  }

  @Override
  public Mono<ParkingRequestInformationList> getParkingRequestsByAcceptor(Long authenticatedUserId,
                                                                          Integer acceptorId) {
    return validateSelf(authenticatedUserId, acceptorId, Constants.ERROR_NOT_ACCEPTOR)
        .then(Mono.defer(() -> portalServiceClient.getUserById(acceptorId.longValue())
            .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty())
            .switchIfEmpty(Mono.error(new NotFoundException(Constants.ERROR_ACCEPTOR_NOT_FOUND)))
            .flatMap(this::validateSae)
            .thenMany(requestRepository.findAllByIdAcceptor(acceptorId))
            .collectList()
            .flatMap(this::buildInformationList)));
  }

  @Override
  public Mono<ParkingRequestInformationList> getParkingRequestsByApplicant(Long authenticatedUserId,
                                                                           Integer applicantId) {
    return validateSelf(authenticatedUserId, applicantId, Constants.ERROR_NOT_APPLICANT)
        .then(Mono.defer(() -> portalServiceClient.getUserById(applicantId.longValue())
            .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty())
            .switchIfEmpty(Mono.error(new NotFoundException(Constants.ERROR_APPLICANT_NOT_FOUND)))
            .thenMany(requestRepository.findAllByApplicantUserId(applicantId))
            .collectList()
            .flatMap(this::buildInformationList)));
  }

  @Override
  public Mono<ParkingRequestDetail> getParkingRequestById(Long authenticatedUserId,
                                                          Integer requestId) {
    return requestRepository.findById(requestId)
        .switchIfEmpty(Mono.error(new NotFoundException(Constants.ERROR_REQUEST_NOT_FOUND)))
        .flatMap(request -> validateDetailAccess(authenticatedUserId, request)
            .then(Mono.defer(() -> buildDetail(request))));
  }

  private Mono<Void> validateDetailAccess(Long authenticatedUserId, Request request) {
    if (isSameUser(authenticatedUserId, request.getIdAcceptor())) {
      return Mono.empty();
    }

    return isSameUser(authenticatedUserId, request.getIdApplicant())
        ? Mono.empty()
        : Mono.error(new ForbiddenException(Constants.ERROR_REQUEST_NOT_OWNED));
  }

  private boolean isSameUser(Long authenticatedUserId, Integer userId) {
    return userId != null && authenticatedUserId.equals(userId.longValue());
  }

  private Mono<ParkingRequestDetail> buildDetail(Request request) {
    return Mono.zip(
            buildInformationList(List.of(request)).map(list -> list.getParkingRequests().getFirst()),
            workflowService.toEntries(workflowRepository.findAllByRequestId(request.getIdRequest())))
        .map(tuple -> toDetail(tuple.getT1(), tuple.getT2()));
  }

  private ParkingRequestDetail toDetail(ParkingRequestInformation information,
                                        List<WorkflowEntry> workflow) {
    return new ParkingRequestDetail()
        .idRequest(information.getIdRequest())
        .applicant(information.getApplicant())
        .vehicle(information.getVehicle())
        .dateRequest(information.getDateRequest())
        .dateResponse(information.getDateResponse())
        .status(information.getStatus())
        .workflow(workflow);
  }

  private Mono<Void> validateSelf(Long authenticatedUserId, Integer requestedUserId,
                                  String errorMessage) {
    return requestedUserId != null && authenticatedUserId.equals(requestedUserId.longValue())
        ? Mono.empty()
        : Mono.error(new ForbiddenException(errorMessage));
  }

  private Mono<Vehicle> resolveVehicle(Integer userId, String numberPlate, Integer idVehicleType) {
    return vehicleRepository.findByNumberPlate(numberPlate)
        .flatMap(vehicle -> {
          if (vehicle.getIdUser() == null) {
            return Mono.error(new ConflictException(Constants.ERROR_PLATE_REGISTERED_UNASSIGNED));
          }
          return userId.equals(vehicle.getIdUser())
              ? validateVehicleIsActive(vehicle)
              : Mono.error(new ForbiddenException(Constants.ERROR_VEHICLE_OWNED_BY_ANOTHER_USER));
        })
        .switchIfEmpty(Mono.defer(() -> registerVehicle(userId, numberPlate, idVehicleType)));
  }

  private Mono<Vehicle> registerVehicle(Integer userId, String numberPlate, Integer idVehicleType) {
    return NumberPlateValidator.validate(numberPlate, idVehicleType)
        .then(Mono.defer(() -> validateActiveVehicleLimit(userId)))
        .then(Mono.defer(() -> vehicleRepository.insertVehicle(idVehicleType, userId, numberPlate,
            Constants.ID_VEHICLE_STATUS_ACTIVE)))
        .flatMap(vehicleRepository::findById);
  }

  private Mono<Void> validateActiveVehicleLimit(Integer userId) {
    return vehicleRepository
        .countByIdUserAndIdVehicleStatus(userId, Constants.ID_VEHICLE_STATUS_ACTIVE)
        .defaultIfEmpty(0L)
        .flatMap(active -> active >= Constants.MAX_ACTIVE_VEHICLES_PER_USER
            ? Mono.error(new ConflictException(Constants.ERROR_MAX_ACTIVE_VEHICLES_REACHED))
            : Mono.empty());
  }

  private Mono<Vehicle> validateVehicleIsActive(Vehicle vehicle) {
    if (Constants.ID_VEHICLE_STATUS_UNASSIGNED.equals(vehicle.getIdVehicleStatus())) {
      return Mono.error(new ConflictException(Constants.ERROR_VEHICLE_ALREADY_UNASSIGNED));
    }
    return Constants.ID_VEHICLE_STATUS_ACTIVE.equals(vehicle.getIdVehicleStatus())
        ? Mono.just(vehicle)
        : Mono.error(new ConflictException(Constants.ERROR_VEHICLE_INACTIVE));
  }

  private Mono<Void> validateNoActiveRequest(Integer idVehicle, Integer idCycle) {
    return requestRepository.findByIdVehicleAndIdCycle(idVehicle, idCycle)
        .flatMap(existing -> Constants.ID_STATUS_REJECTED.equals(existing.getIdStatus())
            ? Mono.error(new ConflictException(Constants.ERROR_REQUEST_REJECTED_USE_RESUBMIT))
            : Mono.error(new ConflictException(Constants.ERROR_REQUEST_ALREADY_EXISTS)))
        .then();
  }

  private Mono<Request> createRequestWithWorkflow(Integer idVehicle, Integer idApplicant,
                                                 Integer idCycle, Long idCampus) {
    LocalDateTime now = LocalDateTime.now();
    return requestRepository.insertRequest(idVehicle, idApplicant, idCycle,
            Constants.ID_STATUS_REGISTERED, now)
        .flatMap(requestId -> workflowRepository
            .saveWorkflow(requestId, Constants.ID_STATUS_REGISTERED, now, Constants.OBSERVATION_REGISTERED)
            .then(Mono.defer(() -> assignAcceptor(requestId, idCampus)))
            .then(Mono.defer(() -> requestRepository.findById(requestId))));
  }

  private Mono<Void> validateResubmit(Integer userId, Request existing, Integer currentIdCycle) {
    return findOwnedVehicle(userId, existing.getIdVehicle())
        .flatMap(vehicle -> validateCurrentCycle(existing.getIdCycle(), currentIdCycle)
            .then(Mono.defer(() -> validateVehicleIsActive(vehicle)))
            .then(Mono.defer(() -> validateRejectedStatus(existing.getIdStatus()))));
  }

  private Mono<Vehicle> findOwnedVehicle(Integer userId, Integer idVehicle) {
    return vehicleRepository.findById(idVehicle)
        .switchIfEmpty(Mono.error(new NotFoundException(Constants.ERROR_REQUEST_NOT_FOUND)))
        .flatMap(vehicle -> userId.equals(vehicle.getIdUser())
            ? Mono.just(vehicle)
            : Mono.error(new ForbiddenException(Constants.ERROR_VEHICLE_OWNED_BY_ANOTHER_USER)));
  }

  private Mono<Void> validateCurrentCycle(Integer idCycle, Integer currentIdCycle) {
    return currentIdCycle.equals(idCycle)
        ? Mono.empty()
        : Mono.error(new ConflictException(Constants.ERROR_REQUEST_WRONG_CYCLE));
  }

  private Mono<Void> validateRejectedStatus(Integer idStatus) {
    if (!Constants.ID_STATUS_REJECTED.equals(idStatus)) {
      return Mono.error(new ConflictException(Constants.ERROR_REQUEST_NOT_REJECTED));
    }
    return Mono.empty();
  }

  private Mono<Void> assignAcceptor(Integer requestId, Long idCampus) {
    return acceptorSelector.selectLeastLoaded(idCampus)
        .flatMap(acceptorId -> {
          log.info("Assigning acceptor {} to request {}", acceptorId, requestId);
          return requestRepository.updateAcceptorAndStatus(requestId, acceptorId, Constants.ID_STATUS_IN_REVISION)
              .then(workflowRepository.saveWorkflow(requestId, Constants.ID_STATUS_IN_REVISION,
                  LocalDateTime.now(), Constants.OBSERVATION_IN_REVISION));
        });
  }

  private Mono<Void> validateSae(UserResponse acceptor) {
    return portalServiceClient.hasSaeRole(acceptor)
        ? Mono.empty()
        : Mono.error(new ForbiddenException(Constants.ERROR_ACCEPTOR_NOT_SAE));
  }

  private Mono<ParkingRequestInformationList> buildInformationList(List<Request> requests) {
    if (requests.isEmpty()) {
      return Mono.just(new ParkingRequestInformationList().parkingRequests(List.of()));
    }

    Set<Integer> vehicleIds = requests.stream().map(Request::getIdVehicle).collect(Collectors.toSet());
    Set<Integer> statusIds = requests.stream().map(Request::getIdStatus).collect(Collectors.toSet());
    Set<Integer> cycleIds = requests.stream().map(Request::getIdCycle).collect(Collectors.toSet());

    Mono<Map<Integer, Vehicle>> vehiclesMono = vehicleRepository.findAllById(vehicleIds)
        .collectMap(Vehicle::getIdVehicle);
    Mono<Map<Integer, Status>> statusesMono = statusRepository.findAllById(statusIds)
        .collectMap(Status::getIdStatus);
    Mono<Map<Integer, CycleResponse>> cyclesMono = Flux.fromIterable(cycleIds)
        .flatMap(idCycle -> portalServiceClient.getCycleById(idCycle.longValue()))
        .collectMap(cycle -> cycle.getIdCycle().intValue());

    return Mono.zip(vehiclesMono, statusesMono, cyclesMono)
        .flatMap(tuple -> {
          Map<Integer, Vehicle> vehicles = tuple.getT1();
          Map<Integer, Status> statuses = tuple.getT2();
          Map<Integer, CycleResponse> cycles = tuple.getT3();

          Set<Integer> vehicleTypeIds = vehicles.values().stream()
              .map(Vehicle::getIdVehicleType).collect(Collectors.toSet());
          Set<Long> userIds = requests.stream()
              .map(request -> request.getIdApplicant().longValue()).collect(Collectors.toSet());

          Mono<Map<Integer, VehicleType>> vehicleTypesMono = vehicleTypeRepository.findAllById(vehicleTypeIds)
              .collectMap(VehicleType::getIdVehicleType);
          Mono<Map<Long, UserResponse>> usersMono = Flux.fromIterable(userIds)
              .flatMap(portalServiceClient::getUserById)
              .collectMap(UserResponse::getIdUser);

          return Mono.zip(vehicleTypesMono, usersMono)
              .map(inner -> toInformationList(requests, vehicles, statuses, cycles, inner.getT1(), inner.getT2()));
        });
  }

  private ParkingRequestInformationList toInformationList(List<Request> requests,
                                                          Map<Integer, Vehicle> vehicles, Map<Integer, Status> statuses, Map<Integer, CycleResponse> cycles,
                                                          Map<Integer, VehicleType> vehicleTypes, Map<Long, UserResponse> users) {
    List<ParkingRequestInformation> items = requests.stream()
        .map(request -> toInformation(request, vehicles, statuses, cycles, vehicleTypes, users))
        .toList();
    return new ParkingRequestInformationList().parkingRequests(items);
  }

  private ParkingRequestInformation toInformation(Request request, Map<Integer, Vehicle> vehicles,
                                                  Map<Integer, Status> statuses, Map<Integer, CycleResponse> cycles, Map<Integer, VehicleType> vehicleTypes,
                                                  Map<Long, UserResponse> users) {
    Vehicle vehicle = vehicles.get(request.getIdVehicle());
    UserResponse applicant = users.get(request.getIdApplicant().longValue());
    VehicleType vehicleType = vehicle == null ? null : vehicleTypes.get(vehicle.getIdVehicleType());
    Status status = statuses.get(request.getIdStatus());
    CycleResponse cycle = cycles.get(request.getIdCycle());

    VehicleInformation vehicleInformation = parkingRequestInformationMapper.toVehicleInformation(vehicle,
        vehicleType);
    ApplicantInformation applicantInformation = parkingRequestInformationMapper
        .toApplicantInformation(applicant, cycle);

    return parkingRequestInformationMapper.toParkingRequestInformation(request, status, applicantInformation,
        vehicleInformation);
  }
}