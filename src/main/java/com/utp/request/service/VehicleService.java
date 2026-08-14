package com.utp.request.service;

import com.utp.request.generated.model.VehicleAvailabilityIn;
import com.utp.request.generated.model.VehicleDetail;
import com.utp.request.generated.model.VehicleDetailList;
import com.utp.request.generated.model.VehicleIn;
import reactor.core.publisher.Mono;

public interface VehicleService {

  Mono<VehicleDetailList> getMyVehicles(Long authenticatedUserId);

  Mono<VehicleDetail> registerVehicle(Long authenticatedUserId, VehicleIn vehicle);

  Mono<VehicleDetail> updateVehicleAvailability(Long authenticatedUserId, Integer vehicleId,
      VehicleAvailabilityIn availability);
}
