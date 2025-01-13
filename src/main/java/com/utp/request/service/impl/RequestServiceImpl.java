package com.utp.request.service.impl;

import com.utp.request.model.Request;
import com.utp.request.model.dto.RequestDto;
import com.utp.request.repository.RequestRepository;
import com.utp.request.service.RequestService;
import com.utp.request.util.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;

    @Override
    public Mono<Request> saveNewRequest(RequestDto request) {
        request.setDateRequest(LocalDateTime.now());
        request.setApproved(Constants.ID_STATUS_NOT_APPROVED);
        request.setIdStatus(Constants.ID_STATUS_REGISTERED);
        return requestRepository.saveNewRequest(request)
                .flatMap(requestId -> requestRepository.saveWorkflow(
                                requestId,
                                Constants.ID_STATUS_REGISTERED,
                                LocalDateTime.now())
                        .then(requestRepository.findById(requestId)));
    }
}
