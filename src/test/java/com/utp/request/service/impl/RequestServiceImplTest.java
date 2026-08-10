package com.utp.request.service.impl;

import com.utp.request.client.portal.PortalServiceClient;
import com.utp.request.generated.client.users.model.CampusResponse;
import com.utp.request.generated.client.users.model.CycleResponse;
import com.utp.request.generated.client.users.model.Role;
import com.utp.request.generated.client.users.model.UserResponse;
import com.utp.request.generated.model.ParkingRequestIn;
import com.utp.request.mapper.ParkingRequestInformationMapperImpl;
import com.utp.request.model.entity.Request;
import com.utp.request.model.entity.Status;
import com.utp.request.model.entity.Vehicle;
import com.utp.request.model.entity.VehicleType;
import com.utp.request.repository.RequestRepository;
import com.utp.request.repository.StatusRepository;
import com.utp.request.repository.VehicleRepository;
import com.utp.request.repository.VehicleTypeRepository;
import com.utp.request.repository.WorkflowRepository;
import com.utp.request.util.error.ConflictException;
import com.utp.request.util.error.ForbiddenException;
import com.utp.request.util.error.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {

  private static final String CURRENT_CYCLE_NAME = "2026-1";
  private static final Long APPLICANT_ID = 10L;
  private static final Long CAMPUS_ID = 7L;

  @Mock
  private RequestRepository requestRepository;
  @Mock
  private VehicleRepository vehicleRepository;
  @Mock
  private WorkflowRepository workflowRepository;
  @Mock
  private StatusRepository statusRepository;
  @Mock
  private VehicleTypeRepository vehicleTypeRepository;
  @Mock
  private PortalServiceClient portalServiceClient;
  @Mock
  private TransactionalOperator transactionalOperator;

  @InjectMocks
  private RequestServiceImpl requestService;

  @BeforeEach
  void mockTransactionalOperator() {
    Mockito.lenient().when(transactionalOperator.transactional(any(Mono.class)))
        .thenAnswer(invocation -> invocation.getArgument(0, Mono.class));
  }

  @BeforeEach
  void useRealMapper() {
    ReflectionTestUtils.setField(requestService, "parkingRequestInformationMapper",
        new ParkingRequestInformationMapperImpl());
  }

  @BeforeEach
  void mockApplicant() {
    Mockito.lenient().when(portalServiceClient.getUserById(APPLICANT_ID))
        .thenReturn(Mono.just(applicant(APPLICANT_ID, CAMPUS_ID)));
  }

  private Vehicle vehicle(Integer idVehicle, Integer idUser) {
    Vehicle vehicle = new Vehicle();
    vehicle.setIdVehicle(idVehicle);
    vehicle.setIdUser(idUser);
    vehicle.setIdVehicleType(1);
    vehicle.setNumberPlate("HNC-234");
    vehicle.setActive(true);
    return vehicle;
  }

  private CycleResponse cycle(Integer idCycle) {
    CycleResponse cycle = new CycleResponse();
    cycle.setIdCycle(idCycle.longValue());
    cycle.setNameCycle(CURRENT_CYCLE_NAME);
    return cycle;
  }

  private Request request(Integer idRequest, Integer idVehicle, Integer idCycle, Integer idStatus) {
    Request request = new Request();
    request.setIdRequest(idRequest);
    request.setIdVehicle(idVehicle);
    request.setIdCycle(idCycle);
    request.setIdStatus(idStatus);
    return request;
  }

  private UserResponse saeUser(Long idUser) {
    Role role = new Role();
    role.setIdRole(3L);
    role.setName("ROLE_SAE");
    UserResponse user = new UserResponse();
    user.setIdUser(idUser);
    user.setUsername("sae" + idUser);
    user.setName("Nombre");
    user.setLastname("Apellido");
    user.setRoles(List.of(role));
    return user;
  }

  private UserResponse applicant(Long idUser, Long idCampus) {
    UserResponse user = new UserResponse();
    user.setIdUser(idUser);
    user.setUsername("applicant" + idUser);
    user.setName("Nombre");
    user.setLastname("Apellido");
    if (idCampus != null) {
      CampusResponse campus = new CampusResponse();
      campus.setIdCampus(idCampus);
      campus.setNameCampus("Campus Central");
      user.setCampus(campus);
    }
    return user;
  }

  @Test
  void testSaveNewRequest_NewVehicle_AssignsAcceptorWithFewerRequests() {
    ParkingRequestIn requestIn = new ParkingRequestIn().numberPlate("HNC-234").vehicleType(1);
    Vehicle createdVehicle = vehicle(100, 10);
    Request finalRequest = request(200, 100, 5, 2);
    finalRequest.setIdAcceptor(21);

    when(portalServiceClient.getCurrentCycle()).thenReturn(Mono.just(cycle(5)));
    when(vehicleRepository.findByNumberPlate("HNC-234")).thenReturn(Mono.empty());
    when(vehicleRepository.insertVehicle(1, 10, "HNC-234")).thenReturn(Mono.just(100));
    when(vehicleRepository.findById(100)).thenReturn(Mono.just(createdVehicle));
    when(requestRepository.findByIdVehicleAndIdCycle(100, 5)).thenReturn(Mono.empty());
    when(requestRepository.insertRequest(any(), any(), any(), any())).thenReturn(Mono.just(200));
    when(workflowRepository.saveWorkflow(any(), any(), any(), any())).thenReturn(Mono.empty());
    when(portalServiceClient.getEligibleAcceptors(CAMPUS_ID)).thenReturn(Flux.just(saeUser(20L), saeUser(21L)));
    when(requestRepository.countByIdAcceptorAndIdStatus(20, 2)).thenReturn(Mono.just(3L));
    when(requestRepository.countByIdAcceptorAndIdStatus(21, 2)).thenReturn(Mono.just(1L));
    when(requestRepository.updateAcceptorAndStatus(200, 21, 2)).thenReturn(Mono.empty());
    when(requestRepository.findById(200)).thenReturn(Mono.just(finalRequest));

    StepVerifier.create(requestService.saveNewRequest(APPLICANT_ID, requestIn))
        .assertNext(saved -> {
          assert saved.getIdRequest().equals(200);
          assert saved.getIdAcceptor().equals(21);
        })
        .verifyComplete();
  }

  @Test
  void testSaveNewRequest_VehicleOwnedByAnotherUser_IsForbidden() {
    ParkingRequestIn requestIn = new ParkingRequestIn().numberPlate("HNC-234").vehicleType(1);
    when(portalServiceClient.getCurrentCycle()).thenReturn(Mono.just(cycle(5)));
    when(vehicleRepository.findByNumberPlate("HNC-234")).thenReturn(Mono.just(vehicle(100, 99)));

    StepVerifier.create(requestService.saveNewRequest(APPLICANT_ID, requestIn))
        .expectError(ForbiddenException.class)
        .verify();
  }

  @Test
  void testSaveNewRequest_ExistingInReviewRequest_IsConflict() {
    ParkingRequestIn requestIn = new ParkingRequestIn().numberPlate("HNC-234").vehicleType(1);
    when(portalServiceClient.getCurrentCycle()).thenReturn(Mono.just(cycle(5)));
    when(vehicleRepository.findByNumberPlate("HNC-234")).thenReturn(Mono.just(vehicle(100, 10)));
    when(requestRepository.findByIdVehicleAndIdCycle(100, 5))
        .thenReturn(Mono.just(request(200, 100, 5, 2)));

    StepVerifier.create(requestService.saveNewRequest(APPLICANT_ID, requestIn))
        .expectError(ConflictException.class)
        .verify();
  }

  @Test
  void testSaveNewRequest_ExistingRejectedRequest_SuggestsResubmit() {
    ParkingRequestIn requestIn = new ParkingRequestIn().numberPlate("HNC-234").vehicleType(1);
    when(portalServiceClient.getCurrentCycle()).thenReturn(Mono.just(cycle(5)));
    when(vehicleRepository.findByNumberPlate("HNC-234")).thenReturn(Mono.just(vehicle(100, 10)));
    when(requestRepository.findByIdVehicleAndIdCycle(100, 5))
        .thenReturn(Mono.just(request(200, 100, 5, 4)));

    StepVerifier.create(requestService.saveNewRequest(APPLICANT_ID, requestIn))
        .expectErrorMatches(error -> error instanceof ConflictException
            && error.getMessage().toLowerCase().contains("reenv"))
        .verify();
  }

  @Test
  void testSaveNewRequest_NoAcceptorAvailable_IsConflict() {
    ParkingRequestIn requestIn = new ParkingRequestIn().numberPlate("HNC-234").vehicleType(1);
    when(portalServiceClient.getCurrentCycle()).thenReturn(Mono.just(cycle(5)));
    when(vehicleRepository.findByNumberPlate("HNC-234")).thenReturn(Mono.just(vehicle(100, 10)));
    when(requestRepository.findByIdVehicleAndIdCycle(100, 5)).thenReturn(Mono.empty());
    when(requestRepository.insertRequest(any(), any(), any(), any())).thenReturn(Mono.just(200));
    when(workflowRepository.saveWorkflow(any(), any(), any(), any())).thenReturn(Mono.empty());
    when(portalServiceClient.getEligibleAcceptors(CAMPUS_ID)).thenReturn(Flux.empty());

    StepVerifier.create(requestService.saveNewRequest(APPLICANT_ID, requestIn))
        .expectError(ConflictException.class)
        .verify();
  }

  @Test
  void testSaveNewRequest_UsersServiceUnavailable_PropagatesError() {
    ParkingRequestIn requestIn = new ParkingRequestIn().numberPlate("HNC-234").vehicleType(1);
    when(portalServiceClient.getCurrentCycle()).thenReturn(Mono.error(new RuntimeException("unavailable")));

    StepVerifier.create(requestService.saveNewRequest(APPLICANT_ID, requestIn))
        .expectError(RuntimeException.class)
        .verify();

    Mockito.verifyNoInteractions(vehicleRepository, requestRepository, workflowRepository);
  }

  @Test
  void testResubmitRequest_Rejected_KeepsSameRequestId() {
    Request existing = request(200, 100, 5, 4);
    Request finalRequest = request(200, 100, 5, 2);
    finalRequest.setIdAcceptor(20);

    when(portalServiceClient.getCurrentCycle()).thenReturn(Mono.just(cycle(5)));
    when(requestRepository.findById(200)).thenReturn(Mono.just(existing), Mono.just(finalRequest));
    when(vehicleRepository.findById(100)).thenReturn(Mono.just(vehicle(100, 10)));
    when(requestRepository.updateStatusAndResponse(200, 1, null)).thenReturn(Mono.empty());
    when(workflowRepository.saveWorkflow(any(), any(), any(), any())).thenReturn(Mono.empty());
    when(portalServiceClient.getEligibleAcceptors(CAMPUS_ID)).thenReturn(Flux.just(saeUser(20L)));
    when(requestRepository.countByIdAcceptorAndIdStatus(20, 2)).thenReturn(Mono.just(0L));
    when(requestRepository.updateAcceptorAndStatus(200, 20, 2)).thenReturn(Mono.empty());

    StepVerifier.create(requestService.resubmitRequest(APPLICANT_ID, 200, "Documentación corregida"))
        .assertNext(saved -> assertEquals(200, saved.getIdRequest()))
        .verifyComplete();

    Mockito.verify(requestRepository, Mockito.never()).insertRequest(anyInt(), anyInt(), anyInt(), any());
  }

  @Test
  void testResubmitRequest_NotFound() {
    when(portalServiceClient.getCurrentCycle()).thenReturn(Mono.just(cycle(5)));
    when(requestRepository.findById(200)).thenReturn(Mono.empty());

    StepVerifier.create(requestService.resubmitRequest(APPLICANT_ID, 200, null))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void testResubmitRequest_NotRejected_IsConflict() {
    Request existing = request(200, 100, 5, 2);
    when(portalServiceClient.getCurrentCycle()).thenReturn(Mono.just(cycle(5)));
    when(requestRepository.findById(200)).thenReturn(Mono.just(existing));
    when(vehicleRepository.findById(100)).thenReturn(Mono.just(vehicle(100, 10)));

    StepVerifier.create(requestService.resubmitRequest(APPLICANT_ID, 200, null))
        .expectError(ConflictException.class)
        .verify();
  }

  @Test
  void testResubmitRequest_WrongCycle_IsConflict() {
    Request existing = request(200, 100, 5, 4);
    when(portalServiceClient.getCurrentCycle()).thenReturn(Mono.just(cycle(6)));
    when(requestRepository.findById(200)).thenReturn(Mono.just(existing));
    when(vehicleRepository.findById(100)).thenReturn(Mono.just(vehicle(100, 10)));

    StepVerifier.create(requestService.resubmitRequest(APPLICANT_ID, 200, null))
        .expectError(ConflictException.class)
        .verify();
  }

  @Test
  void testResubmitRequest_VehicleOwnedByAnotherUser_IsForbidden() {
    Request existing = request(200, 100, 5, 4);
    when(portalServiceClient.getCurrentCycle()).thenReturn(Mono.just(cycle(5)));
    when(requestRepository.findById(200)).thenReturn(Mono.just(existing));
    when(vehicleRepository.findById(100)).thenReturn(Mono.just(vehicle(100, 99)));

    StepVerifier.create(requestService.resubmitRequest(APPLICANT_ID, 200, null))
        .expectError(ForbiddenException.class)
        .verify();
  }

  @Test
  void testGetParkingRequestsByAcceptor_Success() {
    UserResponse acceptor = saeUser(20L);
    Request req = request(200, 100, 5, 2);

    Status status = new Status();
    status.setIdStatus(2);
    status.setNameStatus("EN_REVISION");

    VehicleType vehicleType = new VehicleType();
    vehicleType.setIdVehicleType(1);
    vehicleType.setNameVehicleType("Motocicleta");

    when(portalServiceClient.getUserById(20L)).thenReturn(Mono.just(acceptor));
    when(portalServiceClient.hasSaeRole(acceptor)).thenReturn(true);
    when(requestRepository.findAllByIdAcceptor(20)).thenReturn(Flux.just(req));
    when(vehicleRepository.findAllById(Set.of(100))).thenReturn(Flux.just(vehicle(100, 10)));
    when(statusRepository.findAllById(Set.of(2))).thenReturn(Flux.just(status));
    when(portalServiceClient.getCycleById(5L)).thenReturn(Mono.just(cycle(5)));
    when(vehicleTypeRepository.findAllById(Set.of(1))).thenReturn(Flux.just(vehicleType));

    StepVerifier.create(requestService.getParkingRequestsByAcceptor(20))
        .assertNext(list -> {
          assert list.getParkingRequests().size() == 1;
          assert list.getParkingRequests().get(0).getIdRequest().equals(200);
          assert list.getParkingRequests().get(0).getStatus().equals("EN_REVISION");
        })
        .verifyComplete();
  }

  @Test
  void testGetParkingRequestsByAcceptor_NotSae_IsForbidden() {
    UserResponse notSae = new UserResponse();
    notSae.setIdUser(20L);
    notSae.setRoles(List.of());

    when(portalServiceClient.getUserById(20L)).thenReturn(Mono.just(notSae));
    when(portalServiceClient.hasSaeRole(notSae)).thenReturn(false);

    StepVerifier.create(requestService.getParkingRequestsByAcceptor(20))
        .expectError(ForbiddenException.class)
        .verify();
  }

  @Test
  void testGetParkingRequestsByAcceptor_AcceptorNotFound() {
    when(portalServiceClient.getUserById(20L)).thenReturn(Mono.empty());

    StepVerifier.create(requestService.getParkingRequestsByAcceptor(20))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void testGetParkingRequestsByApplicant_Success() {
    Request req = request(200, 100, 5, 2);

    Status status = new Status();
    status.setIdStatus(2);
    status.setNameStatus("EN_REVISION");

    VehicleType vehicleType = new VehicleType();
    vehicleType.setIdVehicleType(1);
    vehicleType.setNameVehicleType("Motocicleta");

    when(requestRepository.findAllByApplicantUserId(10)).thenReturn(Flux.just(req));
    when(vehicleRepository.findAllById(Set.of(100))).thenReturn(Flux.just(vehicle(100, 10)));
    when(statusRepository.findAllById(Set.of(2))).thenReturn(Flux.just(status));
    when(portalServiceClient.getCycleById(5L)).thenReturn(Mono.just(cycle(5)));
    when(vehicleTypeRepository.findAllById(Set.of(1))).thenReturn(Flux.just(vehicleType));

    StepVerifier.create(requestService.getParkingRequestsByApplicant(10))
        .assertNext(list -> {
          assert list.getParkingRequests().size() == 1;
          assert list.getParkingRequests().get(0).getIdRequest().equals(200);
          assert list.getParkingRequests().get(0).getStatus().equals("EN_REVISION");
        })
        .verifyComplete();
  }

  @Test
  void testGetParkingRequestsByApplicant_ApplicantNotFound() {
    when(portalServiceClient.getUserById(10L)).thenReturn(Mono.empty());

    StepVerifier.create(requestService.getParkingRequestsByApplicant(10))
        .expectError(NotFoundException.class)
        .verify();
  }
}
