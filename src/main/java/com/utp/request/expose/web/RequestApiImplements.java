package com.utp.request.expose.web;

import com.utp.request.generated.api.RequestApi;
import com.utp.request.generated.model.ParkingRequest;
import com.utp.request.generated.model.ParkingRequestResponse;
import com.utp.request.model.dto.RequestDto;
import com.utp.request.service.RequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Controller
@Slf4j
@RequiredArgsConstructor
public class RequestApiImplements implements RequestApi {

  private final RequestService requestService;

  @Override
  public Mono<ResponseEntity<ParkingRequestResponse>> createParkingRequest(
      String requestID,
      String requestDate,
      String appCode,
      String callerName,
      Mono<ParkingRequest> parkingRequest,
      ServerWebExchange exchange) {

    return parkingRequest
        .flatMap(pr -> requestService.saveNewRequest(RequestDto.builder()
                .numberPlate(pr.getNumberPlate())
                .idApplicant(pr.getIdApplicant())
                .vehicleType(pr.getVehicleType())
                .isNew(pr.getIsNew())
                .build())
            .map(savedRequest -> {
              log.info("Parking request registered successfully - RequestId: {}, NumberPlate: {}",
                  savedRequest.getIdRequest(), savedRequest.getNumberPlate());
              return ResponseEntity.status(HttpStatus.CREATED)
                  .header("Request-ID", requestID)
                  .header("request-date", requestDate)
                  .header("app-code", appCode)
                  .header("caller-name", callerName)
                  .body(new ParkingRequestResponse().parkingRequestId(savedRequest.getIdRequest()));
            }));
  }
}
