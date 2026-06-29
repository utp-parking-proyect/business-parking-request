package com.utp.request.expose.web;

import com.utp.request.generated.api.RequestApi;
import com.utp.request.generated.model.ParkingRequestIn;
import com.utp.request.generated.model.ParkingRequestOut;
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
  public Mono<ResponseEntity<ParkingRequestOut>> createParkingRequest(
      String requestID,
      String requestDate,
      String appCode,
      String callerName,
      Mono<ParkingRequestIn> parkingRequest,
      ServerWebExchange exchange) {

    return parkingRequest
        .flatMap(request -> requestService.saveNewRequest(request)
            .map(savedRequest -> {
              log.info("Parking request registered successfully - RequestId: {}, NumberPlate: {}",
                  savedRequest.getIdRequest(), savedRequest.getNumberPlate());
              return ResponseEntity.status(HttpStatus.CREATED)
                  .header("Request-ID", requestID)
                  .header("request-date", requestDate)
                  .header("app-code", appCode)
                  .header("caller-name", callerName)
                  .body(new ParkingRequestOut().parkingRequestId(savedRequest.getIdRequest()));
            }));
  }
}
