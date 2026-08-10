package com.utp.request.expose.web;

import com.utp.request.generated.api.RequestApi;
import com.utp.request.generated.model.ParkingRequestIn;
import com.utp.request.generated.model.ParkingRequestInformationList;
import com.utp.request.generated.model.ParkingRequestOut;
import com.utp.request.generated.model.ParkingRequestResubmitIn;
import com.utp.request.service.RequestService;
import com.utp.request.util.security.AuthenticatedUserProvider;
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
  private final AuthenticatedUserProvider authenticatedUserProvider;

  @Override
  public Mono<ResponseEntity<ParkingRequestOut>> createParkingRequest(
      String requestID,
      String requestDate,
      String appCode,
      String callerName,
      Mono<ParkingRequestIn> parkingRequest,
      ServerWebExchange exchange) {

    return Mono.zip(authenticatedUserProvider.getAuthenticatedUserId(), parkingRequest)
        .flatMap(tuple -> requestService.saveNewRequest(tuple.getT1(), tuple.getT2()))
        .map(savedRequest -> {
          log.info("Parking request registered successfully - RequestId: {}", savedRequest.getIdRequest());
          return ResponseEntity.status(HttpStatus.CREATED)
              .header("Request-ID", requestID)
              .header("request-date", requestDate)
              .header("app-code", appCode)
              .header("caller-name", callerName)
              .body(new ParkingRequestOut().parkingRequestId(savedRequest.getIdRequest()));
        });
  }

  @Override
  public Mono<ResponseEntity<ParkingRequestOut>> resubmitParkingRequest(
      String requestID,
      String requestDate,
      String appCode,
      String callerName,
      Integer requestId,
      Mono<ParkingRequestResubmitIn> parkingRequestResubmitIn,
      ServerWebExchange exchange) {

    return Mono.zip(authenticatedUserProvider.getAuthenticatedUserId(),
            parkingRequestResubmitIn.defaultIfEmpty(new ParkingRequestResubmitIn()))
        .flatMap(tuple -> requestService.resubmitRequest(tuple.getT1(), requestId, tuple.getT2().getObservation()))
        .map(savedRequest -> {
          log.info("Parking request resubmitted successfully - RequestId: {}", savedRequest.getIdRequest());
          return ResponseEntity.status(HttpStatus.OK)
              .header("Request-ID", requestID)
              .header("request-date", requestDate)
              .header("app-code", appCode)
              .header("caller-name", callerName)
              .body(new ParkingRequestOut().parkingRequestId(savedRequest.getIdRequest()));
        });
  }

  @Override
  public Mono<ResponseEntity<ParkingRequestInformationList>> getParkingRequestsByAcceptor(
      String requestID,
      String requestDate,
      String appCode,
      String callerName,
      Integer acceptorId,
      ServerWebExchange exchange) {
    return requestService.getParkingRequestsByAcceptor(acceptorId)
        .map(ResponseEntity::ok);
  }

  @Override
  public Mono<ResponseEntity<ParkingRequestInformationList>> getParkingRequestsByApplicant(
      String requestID,
      String requestDate,
      String appCode,
      String callerName,
      Integer applicantId,
      ServerWebExchange exchange) {
    return requestService.getParkingRequestsByApplicant(applicantId)
        .map(ResponseEntity::ok);
  }
}
