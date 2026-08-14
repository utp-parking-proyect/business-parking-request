package com.utp.request.service.impl;

import com.utp.request.generated.model.VehicleAvailabilityIn;
import com.utp.request.generated.model.VehicleDetail;
import com.utp.request.generated.model.VehicleDetailList;
import com.utp.request.generated.model.VehicleIn;
import com.utp.request.mapper.ParkingRequestInformationMapper;
import com.utp.request.model.entity.Vehicle;
import com.utp.request.model.entity.VehicleType;
import com.utp.request.repository.VehicleRepository;
import com.utp.request.repository.VehicleTypeRepository;
import com.utp.request.service.VehicleService;
import com.utp.request.util.Constants;
import com.utp.request.util.NumberPlateValidator;
import com.utp.request.util.error.ConflictException;
import com.utp.request.util.error.ForbiddenException;
import com.utp.request.util.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

  private final VehicleRepository vehicleRepository;
  private final VehicleTypeRepository vehicleTypeRepository;
  private final TransactionalOperator transactionalOperator;
  private final ParkingRequestInformationMapper parkingRequestInformationMapper;

  @Override
  public Mono<VehicleDetailList> getMyVehicles(Long authenticatedUserId) {
    return vehicleRepository.findAllByIdUserOrderByIdVehicle(authenticatedUserId.intValue())
        .collectList()
        .flatMap(this::toDetailList);
  }

  @Override
  public Mono<VehicleDetail> registerVehicle(Long authenticatedUserId, VehicleIn vehicle) {
    Integer userId = authenticatedUserId.intValue();
    String numberPlate = NumberPlateValidator.normalize(vehicle.getNumberPlate());

    return NumberPlateValidator.validate(numberPlate, vehicle.getVehicleType())
        .then(Mono.defer(() -> validatePlateIsAvailable(userId, numberPlate)))
        .then(Mono.defer(() -> validateVehicleLimit(userId)))
        .then(Mono.defer(() -> createVehicle(userId, numberPlate, vehicle.getVehicleType())))
        .as(transactionalOperator::transactional)
        .flatMap(this::toDetail);
  }

  @Override
  public Mono<VehicleDetail> updateVehicleAvailability(Long authenticatedUserId, Integer vehicleId,
                                                       VehicleAvailabilityIn availability) {
    if (availability == null || availability.getActive() == null) {
      return Mono.error(new IllegalArgumentException(Constants.ERROR_VEHICLE_ACTIVE_REQUIRED));
    }

    return findOwnedVehicle(authenticatedUserId, vehicleId)
        .flatMap(vehicle -> {
          if (availability.getActive().equals(vehicle.getActive())) {
            return Mono.just(vehicle);
          }
          log.info("Updating availability of vehicle {} to {}", vehicleId, availability.getActive());
          return vehicleRepository.updateActive(vehicleId, availability.getActive())
              .then(Mono.defer(() -> vehicleRepository.findById(vehicleId)));
        })
        .flatMap(this::toDetail);
  }

  private Mono<Vehicle> findOwnedVehicle(Long authenticatedUserId, Integer vehicleId) {
    return vehicleRepository.findById(vehicleId)
        .switchIfEmpty(Mono.error(new NotFoundException(Constants.ERROR_VEHICLE_NOT_FOUND)))
        .flatMap(vehicle -> isOwnedBy(vehicle, authenticatedUserId)
            ? Mono.just(vehicle)
            : Mono.error(new ForbiddenException(Constants.ERROR_VEHICLE_NOT_OWNED)));
  }

  private boolean isOwnedBy(Vehicle vehicle, Long authenticatedUserId) {
    return vehicle.getIdUser() != null && authenticatedUserId.equals(vehicle.getIdUser().longValue());
  }

  private Mono<Void> validatePlateIsAvailable(Integer userId, String numberPlate) {
    return vehicleRepository.findByNumberPlate(numberPlate)
        .flatMap(existing -> existing.getIdUser().equals(userId)
            ? Mono.error(new ConflictException(Constants.ERROR_VEHICLE_ALREADY_REGISTERED))
            : Mono.error(new ForbiddenException(Constants.ERROR_VEHICLE_OWNED_BY_ANOTHER_USER)))
        .then();
  }

  private Mono<Void> validateVehicleLimit(Integer userId) {
    return vehicleRepository.countByIdUser(userId)
        .defaultIfEmpty(0L)
        .flatMap(registered -> registered >= Constants.MAX_VEHICLES_PER_USER
            ? Mono.error(new ConflictException(Constants.ERROR_MAX_VEHICLES_REACHED))
            : Mono.empty());
  }

  private Mono<Vehicle> createVehicle(Integer userId, String numberPlate, Integer idVehicleType) {
    return vehicleRepository.insertVehicle(idVehicleType, userId, numberPlate)
        .flatMap(vehicleRepository::findById);
  }

  private Mono<VehicleDetail> toDetail(Vehicle vehicle) {
    return vehicleTypeRepository.findById(vehicle.getIdVehicleType())
        .map(vehicleType -> parkingRequestInformationMapper.toVehicleDetail(vehicle, vehicleType))
        .defaultIfEmpty(parkingRequestInformationMapper.toVehicleDetail(vehicle, null));
  }

  private Mono<VehicleDetailList> toDetailList(List<Vehicle> vehicles) {
    if (vehicles.isEmpty()) {
      return Mono.just(emptyList());
    }

    Set<Integer> vehicleTypeIds = vehicles.stream()
        .map(Vehicle::getIdVehicleType).collect(Collectors.toSet());

    return vehicleTypeRepository.findAllById(vehicleTypeIds)
        .collectMap(VehicleType::getIdVehicleType)
        .map(vehicleTypes -> toDetailList(vehicles, vehicleTypes));
  }

  private VehicleDetailList toDetailList(List<Vehicle> vehicles, Map<Integer, VehicleType> vehicleTypes) {
    List<VehicleDetail> details = vehicles.stream()
        .map(vehicle -> parkingRequestInformationMapper
            .toVehicleDetail(vehicle, vehicleTypes.get(vehicle.getIdVehicleType())))
        .toList();

    return new VehicleDetailList()
        .vehicles(details)
        .registeredVehicles(details.size())
        .maxVehicles(Constants.MAX_VEHICLES_PER_USER);
  }

  private VehicleDetailList emptyList() {
    return new VehicleDetailList()
        .vehicles(List.of())
        .registeredVehicles(0)
        .maxVehicles(Constants.MAX_VEHICLES_PER_USER);
  }
}
