package com.utp.request.service;

import com.utp.request.generated.model.ParkingRequestDetail;
import com.utp.request.generated.model.ParkingRequestIn;
import com.utp.request.generated.model.ParkingRequestInformationList;
import com.utp.request.model.entity.Request;
import reactor.core.publisher.Mono;

public interface RequestService {

  Mono<Request> saveNewRequest(Long authenticatedUserId, ParkingRequestIn request);
  Mono<Request> resubmitRequest(Long authenticatedUserId, Integer requestId, String observation);
  Mono<ParkingRequestInformationList> getParkingRequestsByAcceptor(Long authenticatedUserId,
      Integer acceptorId);

  Mono<ParkingRequestInformationList> getParkingRequestsByApplicant(Long authenticatedUserId,
      Integer applicantId);

  Mono<ParkingRequestDetail> getParkingRequestById(Long authenticatedUserId, Integer requestId);
}
