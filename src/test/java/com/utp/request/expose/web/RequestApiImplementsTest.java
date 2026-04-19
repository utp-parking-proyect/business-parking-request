package com.utp.request.expose.web;

import com.utp.request.generated.model.ParkingRequest;
import com.utp.request.model.entity.Request;
import com.utp.request.service.RequestService;
import com.utp.request.util.error.ErrorResponseHandler;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestApiImplementsTest {

  @Mock
  private RequestService requestService;

  @Mock
  private ErrorResponseHandler errorResponseHandler;

  @Mock
  private ServerWebExchange exchange;

  @InjectMocks
  private RequestApiImplements controller;

  @Test
  void testCreateParkingRequest_Success() {
    // Arrange
    ParkingRequest parkingRequest = new ParkingRequest();
    parkingRequest.setNumberPlate("HNC-234");
    parkingRequest.setIdApplicant(1);
    parkingRequest.setVehicleType(1);
    parkingRequest.setIsNew(true);

    Request savedRequest = new Request();
    savedRequest.setIdRequest(1);
    savedRequest.setNumberPlate("HNC-234");

    when(requestService.saveNewRequest(any()))
        .thenReturn(Mono.just(savedRequest));

    // Act & Assert
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
}
