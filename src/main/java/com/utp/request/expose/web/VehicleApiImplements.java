package com.utp.request.expose.web;

import com.utp.request.generated.api.VehiclesApi;
import com.utp.request.generated.model.VehicleAvailabilityIn;
import com.utp.request.generated.model.VehicleDetail;
import com.utp.request.generated.model.VehicleDetailList;
import com.utp.request.generated.model.VehicleIn;
import com.utp.request.service.VehicleService;
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
public class VehicleApiImplements implements VehiclesApi {

  private final VehicleService vehicleService;
  private final AuthenticatedUserProvider authenticatedUserProvider;

  @Override
  public Mono<ResponseEntity<VehicleDetailList>> getMyVehicles(
      String requestID,
      String requestDate,
      String appCode,
      String callerName,
      ServerWebExchange exchange) {

    return authenticatedUserProvider.getAuthenticatedUserId()
        .flatMap(vehicleService::getMyVehicles)
        .map(ResponseEntity::ok);
  }

  @Override
  public Mono<ResponseEntity<VehicleDetail>> registerVehicle(
      String requestID,
      String requestDate,
      String appCode,
      String callerName,
      Mono<VehicleIn> vehicleIn,
      ServerWebExchange exchange) {

    return Mono.zip(authenticatedUserProvider.getAuthenticatedUserId(), vehicleIn)
        .flatMap(tuple -> vehicleService.registerVehicle(tuple.getT1(), tuple.getT2()))
        .map(vehicle -> {
          log.info("Vehicle registered successfully - VehicleId: {}", vehicle.getIdVehicle());
          return ResponseEntity.status(HttpStatus.CREATED)
              .header("Request-ID", requestID)
              .header("request-date", requestDate)
              .header("app-code", appCode)
              .header("caller-name", callerName)
              .body(vehicle);
        });
  }

  @Override
  public Mono<ResponseEntity<VehicleDetail>> updateVehicleAvailability(
      String requestID,
      String requestDate,
      String appCode,
      String callerName,
      Integer vehicleId,
      Mono<VehicleAvailabilityIn> vehicleAvailabilityIn,
      ServerWebExchange exchange) {

    return Mono.zip(authenticatedUserProvider.getAuthenticatedUserId(), vehicleAvailabilityIn)
        .flatMap(tuple -> vehicleService.updateVehicleAvailability(tuple.getT1(), vehicleId, tuple.getT2()))
        .map(vehicle -> {
          log.info("Vehicle availability updated - VehicleId: {}, active: {}",
              vehicle.getIdVehicle(), vehicle.getActive());
          return ResponseEntity.ok(vehicle);
        });
  }
}
