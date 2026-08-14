package com.utp.request.service.impl;

import com.utp.request.generated.model.VehicleAvailabilityIn;
import com.utp.request.generated.model.VehicleIn;
import com.utp.request.mapper.ParkingRequestInformationMapperImpl;
import com.utp.request.model.entity.Vehicle;
import com.utp.request.model.entity.VehicleType;
import com.utp.request.repository.VehicleRepository;
import com.utp.request.repository.VehicleTypeRepository;
import com.utp.request.util.Constants;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleServiceImplTest {

  private static final Long OWNER_ID = 10L;
  private static final Long ANOTHER_USER_ID = 99L;

  @Mock
  private VehicleRepository vehicleRepository;
  @Mock
  private VehicleTypeRepository vehicleTypeRepository;
  @Mock
  private TransactionalOperator transactionalOperator;

  @InjectMocks
  private VehicleServiceImpl vehicleService;

  @BeforeEach
  void mockTransactionalOperator() {
    Mockito.lenient().when(transactionalOperator.transactional(any(Mono.class)))
        .thenAnswer(invocation -> invocation.getArgument(0, Mono.class));
  }

  @BeforeEach
  void useRealMapper() {
    ReflectionTestUtils.setField(vehicleService, "parkingRequestInformationMapper",
        new ParkingRequestInformationMapperImpl());
  }

  private Vehicle vehicle(Integer idVehicle, Integer idUser, String numberPlate, boolean active) {
    Vehicle vehicle = new Vehicle();
    vehicle.setIdVehicle(idVehicle);
    vehicle.setIdUser(idUser);
    vehicle.setIdVehicleType(1);
    vehicle.setNumberPlate(numberPlate);
    vehicle.setActive(active);
    return vehicle;
  }

  private VehicleType vehicleType() {
    VehicleType vehicleType = new VehicleType();
    vehicleType.setIdVehicleType(1);
    vehicleType.setNameVehicleType("Automóvil");
    return vehicleType;
  }

  @Test
  void testGetMyVehicles_ReturnsActiveAndInactiveWithCounters() {
    when(vehicleRepository.findAllByIdUserOrderByIdVehicle(10)).thenReturn(Flux.just(
        vehicle(1, 10, "ABC-123", true),
        vehicle(2, 10, "XYZ-456", false)));
    when(vehicleTypeRepository.findAllById(Set.of(1))).thenReturn(Flux.just(vehicleType()));

    StepVerifier.create(vehicleService.getMyVehicles(OWNER_ID))
        .assertNext(list -> {
          assertEquals(2, list.getVehicles().size());
          assertEquals(2, list.getRegisteredVehicles());
          assertEquals(Constants.MAX_VEHICLES_PER_USER, list.getMaxVehicles());
          assertTrue(list.getVehicles().getFirst().getActive());
          assertFalse(list.getVehicles().getLast().getActive());
          assertEquals("Automóvil", list.getVehicles().getFirst().getVehicleType());
        })
        .verifyComplete();
  }

  @Test
  void testGetMyVehicles_WithoutVehicles_ReturnsEmptyList() {
    when(vehicleRepository.findAllByIdUserOrderByIdVehicle(10)).thenReturn(Flux.empty());

    StepVerifier.create(vehicleService.getMyVehicles(OWNER_ID))
        .assertNext(list -> {
          assertTrue(list.getVehicles().isEmpty());
          assertEquals(0, list.getRegisteredVehicles());
          assertEquals(Constants.MAX_VEHICLES_PER_USER, list.getMaxVehicles());
        })
        .verifyComplete();
  }

  @Test
  void testRegisterVehicle_FirstVehicle_IsCreatedActive() {
    VehicleIn vehicleIn = new VehicleIn().numberPlate("abc-123").vehicleType(1);

    when(vehicleRepository.findByNumberPlate("ABC-123")).thenReturn(Mono.empty());
    when(vehicleRepository.countByIdUser(10)).thenReturn(Mono.just(0L));
    when(vehicleRepository.insertVehicle(1, 10, "ABC-123")).thenReturn(Mono.just(1));
    when(vehicleRepository.findById(1)).thenReturn(Mono.just(vehicle(1, 10, "ABC-123", true)));
    when(vehicleTypeRepository.findById(1)).thenReturn(Mono.just(vehicleType()));

    StepVerifier.create(vehicleService.registerVehicle(OWNER_ID, vehicleIn))
        .assertNext(detail -> {
          assertEquals(1, detail.getIdVehicle());
          assertEquals("ABC-123", detail.getNumberPlate());
          assertTrue(detail.getActive());
        })
        .verifyComplete();
  }

  @Test
  void testRegisterVehicle_SixthVehicle_IsConflict() {
    VehicleIn vehicleIn = new VehicleIn().numberPlate("ABC-123").vehicleType(1);

    when(vehicleRepository.findByNumberPlate("ABC-123")).thenReturn(Mono.empty());
    when(vehicleRepository.countByIdUser(10)).thenReturn(Mono.just(5L));

    StepVerifier.create(vehicleService.registerVehicle(OWNER_ID, vehicleIn))
        .expectErrorMatches(error -> error instanceof ConflictException
            && error.getMessage().equals(Constants.ERROR_MAX_VEHICLES_REACHED))
        .verify();

    Mockito.verify(vehicleRepository, Mockito.never()).insertVehicle(anyInt(), anyInt(), any());
  }

  @Test
  void testRegisterVehicle_DisabledVehiclesCountTowardsTheLimit() {
    VehicleIn vehicleIn = new VehicleIn().numberPlate("ABC-123").vehicleType(1);

    when(vehicleRepository.findByNumberPlate("ABC-123")).thenReturn(Mono.empty());
    when(vehicleRepository.countByIdUser(10)).thenReturn(Mono.just(5L));

    StepVerifier.create(vehicleService.registerVehicle(OWNER_ID, vehicleIn))
        .expectError(ConflictException.class)
        .verify();

    Mockito.verify(vehicleRepository).countByIdUser(10);
  }

  @Test
  void testRegisterVehicle_PlateAlreadyRegisteredByOwner_IsConflict() {
    VehicleIn vehicleIn = new VehicleIn().numberPlate("ABC-123").vehicleType(1);

    when(vehicleRepository.findByNumberPlate("ABC-123"))
        .thenReturn(Mono.just(vehicle(1, 10, "ABC-123", true)));

    StepVerifier.create(vehicleService.registerVehicle(OWNER_ID, vehicleIn))
        .expectErrorMatches(error -> error instanceof ConflictException
            && error.getMessage().equals(Constants.ERROR_VEHICLE_ALREADY_REGISTERED))
        .verify();

    Mockito.verify(vehicleRepository, Mockito.never()).insertVehicle(anyInt(), anyInt(), any());
  }

  @Test
  void testRegisterVehicle_PlateOwnedByAnotherUser_IsForbidden() {
    VehicleIn vehicleIn = new VehicleIn().numberPlate("ABC-123").vehicleType(1);

    when(vehicleRepository.findByNumberPlate("ABC-123"))
        .thenReturn(Mono.just(vehicle(1, 99, "ABC-123", true)));

    StepVerifier.create(vehicleService.registerVehicle(OWNER_ID, vehicleIn))
        .expectError(ForbiddenException.class)
        .verify();

    Mockito.verify(vehicleRepository, Mockito.never()).insertVehicle(anyInt(), anyInt(), any());
  }

  @Test
  void testRegisterVehicle_MotorcycleWithCarPlate_IsInvalid() {
    VehicleIn vehicleIn = new VehicleIn().numberPlate("ABC-123").vehicleType(2);

    StepVerifier.create(vehicleService.registerVehicle(OWNER_ID, vehicleIn))
        .expectErrorMatches(error -> error instanceof IllegalArgumentException
            && error.getMessage().equals(Constants.ERROR_INVALID_NUMBER_PLATE_MOTORCYCLE))
        .verify();

    Mockito.verifyNoInteractions(vehicleRepository);
  }

  @Test
  void testUpdateVehicleAvailability_Disable_KeepsTheVehicleRegistered() {
    when(vehicleRepository.findById(1))
        .thenReturn(Mono.just(vehicle(1, 10, "ABC-123", true)),
            Mono.just(vehicle(1, 10, "ABC-123", false)));
    when(vehicleRepository.updateActive(1, false)).thenReturn(Mono.empty());
    when(vehicleTypeRepository.findById(1)).thenReturn(Mono.just(vehicleType()));

    StepVerifier.create(vehicleService.updateVehicleAvailability(OWNER_ID, 1,
            new VehicleAvailabilityIn().active(false)))
        .assertNext(detail -> {
          assertEquals(1, detail.getIdVehicle());
          assertEquals("ABC-123", detail.getNumberPlate());
          assertFalse(detail.getActive());
        })
        .verifyComplete();

    Mockito.verify(vehicleRepository, Mockito.never()).deleteById(anyInt());
    Mockito.verify(vehicleRepository).updateActive(eq(1), eq(false));
  }

  @Test
  void testUpdateVehicleAvailability_Enable_MakesItSelectableAgain() {
    when(vehicleRepository.findById(1))
        .thenReturn(Mono.just(vehicle(1, 10, "ABC-123", false)),
            Mono.just(vehicle(1, 10, "ABC-123", true)));
    when(vehicleRepository.updateActive(1, true)).thenReturn(Mono.empty());
    when(vehicleTypeRepository.findById(1)).thenReturn(Mono.just(vehicleType()));

    StepVerifier.create(vehicleService.updateVehicleAvailability(OWNER_ID, 1,
            new VehicleAvailabilityIn().active(true)))
        .assertNext(detail -> assertTrue(detail.getActive()))
        .verifyComplete();
  }

  @Test
  void testUpdateVehicleAvailability_AnotherUsersVehicle_IsForbidden() {
    when(vehicleRepository.findById(1)).thenReturn(Mono.just(vehicle(1, 10, "ABC-123", true)));

    StepVerifier.create(vehicleService.updateVehicleAvailability(ANOTHER_USER_ID, 1,
            new VehicleAvailabilityIn().active(false)))
        .expectErrorMatches(error -> error instanceof ForbiddenException
            && error.getMessage().equals(Constants.ERROR_VEHICLE_NOT_OWNED))
        .verify();

    Mockito.verify(vehicleRepository, Mockito.never()).updateActive(anyInt(), any());
  }

  @Test
  void testUpdateVehicleAvailability_UnknownVehicle_IsNotFound() {
    when(vehicleRepository.findById(1)).thenReturn(Mono.empty());

    StepVerifier.create(vehicleService.updateVehicleAvailability(OWNER_ID, 1,
            new VehicleAvailabilityIn().active(false)))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void testUpdateVehicleAvailability_SameValue_DoesNotWriteAgain() {
    when(vehicleRepository.findById(1)).thenReturn(Mono.just(vehicle(1, 10, "ABC-123", true)));
    when(vehicleTypeRepository.findById(1)).thenReturn(Mono.just(vehicleType()));

    StepVerifier.create(vehicleService.updateVehicleAvailability(OWNER_ID, 1,
            new VehicleAvailabilityIn().active(true)))
        .assertNext(detail -> assertTrue(detail.getActive()))
        .verifyComplete();

    Mockito.verify(vehicleRepository, Mockito.never()).updateActive(anyInt(), any());
  }

  @Test
  void testUpdateVehicleAvailability_WithoutActive_IsInvalid() {
    StepVerifier.create(vehicleService.updateVehicleAvailability(OWNER_ID, 1,
            new VehicleAvailabilityIn()))
        .expectError(IllegalArgumentException.class)
        .verify();

    Mockito.verifyNoInteractions(vehicleRepository);
  }

  @Test
  void testGetMyVehicles_UnknownVehicleType_StillReturnsTheVehicle() {
    when(vehicleRepository.findAllByIdUserOrderByIdVehicle(10))
        .thenReturn(Flux.fromIterable(List.of(vehicle(1, 10, "ABC-123", true))));
    when(vehicleTypeRepository.findAllById(Set.of(1))).thenReturn(Flux.empty());

    StepVerifier.create(vehicleService.getMyVehicles(OWNER_ID))
        .assertNext(list -> assertEquals("ABC-123", list.getVehicles().getFirst().getNumberPlate()))
        .verifyComplete();
  }
}
