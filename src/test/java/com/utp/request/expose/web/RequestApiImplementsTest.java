package com.utp.request.expose.web;

import com.utp.request.generated.model.ParkingRequestIn;
import com.utp.request.generated.model.ParkingRequestResubmitIn;
import com.utp.request.model.entity.Request;
import com.utp.request.service.RequestService;
import com.utp.request.util.security.AuthenticatedUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestApiImplementsTest {

  @Mock
  private RequestService requestService;

  @Mock
  private AuthenticatedUserProvider authenticatedUserProvider;

  @Mock
  private ServerWebExchange exchange;

  @InjectMocks
  private RequestApiImplements controller;

  @Test
  void testCreateParkingRequest_Success() {
    ParkingRequestIn parkingRequest = new ParkingRequestIn()
        .numberPlate("HNC-234")
        .vehicleType(1);

    Request savedRequest = new Request();
    savedRequest.setIdRequest(1);

    when(authenticatedUserProvider.getAuthenticatedUserId()).thenReturn(Mono.just(10L));
    when(requestService.saveNewRequest(eq(10L), any())).thenReturn(Mono.just(savedRequest));

    StepVerifier.create(controller.createParkingRequest(
            "550e8400-e29b-41d4-a716-446655440000",
            "2025-01-10T14:02:03.987-0500",
            "P0",
            "atlas-cross-services",
            Mono.just(parkingRequest),
            exchange))
        .assertNext(response -> {
          assertEquals(HttpStatus.CREATED, response.getStatusCode());
          assertNotNull(response.getBody());
          assertEquals(1, response.getBody().getParkingRequestId());
        })
        .verifyComplete();
  }

  @Test
  void testResubmitParkingRequest_Success() {
    ParkingRequestResubmitIn resubmitIn = new ParkingRequestResubmitIn().observation("Documentación corregida");

    Request savedRequest = new Request();
    savedRequest.setIdRequest(1);

    when(authenticatedUserProvider.getAuthenticatedUserId()).thenReturn(Mono.just(10L));
    when(requestService.resubmitRequest(eq(10L), eq(1), any())).thenReturn(Mono.just(savedRequest));

    StepVerifier.create(controller.resubmitParkingRequest(
            "550e8400-e29b-41d4-a716-446655440000",
            "2025-01-10T14:02:03.987-0500",
            "P0",
            "atlas-cross-services",
            1,
            Mono.just(resubmitIn),
            exchange))
        .assertNext(response -> {
          assertEquals(HttpStatus.OK, response.getStatusCode());
          assertNotNull(response.getBody());
          assertEquals(1, response.getBody().getParkingRequestId());
        })
        .verifyComplete();
  }

  @Test
  void testGetParkingRequestsByAcceptor_DelegatesToService() {
    when(authenticatedUserProvider.getAuthenticatedUserId()).thenReturn(Mono.just(20L));
    when(requestService.getParkingRequestsByAcceptor(eq(20L), anyInt()))
        .thenReturn(Mono.just(new com.utp.request.generated.model.ParkingRequestInformationList()));

    StepVerifier.create(controller.getParkingRequestsByAcceptor(
            "550e8400-e29b-41d4-a716-446655440000",
            "2025-01-10T14:02:03.987-0500",
            "P0",
            "atlas-cross-services",
            20,
            exchange))
        .assertNext(response -> assertEquals(HttpStatus.OK, response.getStatusCode()))
        .verifyComplete();
  }

  @Test
  void testGetParkingRequestsByApplicant_DelegatesToService() {
    when(authenticatedUserProvider.getAuthenticatedUserId()).thenReturn(Mono.just(10L));
    when(requestService.getParkingRequestsByApplicant(eq(10L), anyInt()))
        .thenReturn(Mono.just(new com.utp.request.generated.model.ParkingRequestInformationList()));

    StepVerifier.create(controller.getParkingRequestsByApplicant(
            "550e8400-e29b-41d4-a716-446655440000",
            "2025-01-10T14:02:03.987-0500",
            "P0",
            "atlas-cross-services",
            10,
            exchange))
        .assertNext(response -> assertEquals(HttpStatus.OK, response.getStatusCode()))
        .verifyComplete();
  }
}
