package com.utp.request.service;

import com.utp.request.generated.model.ParkingRequestIn;
import com.utp.request.model.entity.Request;
import reactor.core.publisher.Mono;

public interface RequestService {
  Mono<Request> saveNewRequest(ParkingRequestIn request);
}
