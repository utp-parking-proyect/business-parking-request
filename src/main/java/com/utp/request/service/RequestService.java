package com.utp.request.service;

import com.utp.request.model.entity.Request;
import com.utp.request.model.dto.RequestDto;
import reactor.core.publisher.Mono;

public interface RequestService {
  Mono<Request> saveNewRequest(RequestDto request);
}
