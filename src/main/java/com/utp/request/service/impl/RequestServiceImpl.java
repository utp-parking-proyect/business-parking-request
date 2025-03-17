package com.utp.request.service.impl;

import com.utp.request.model.entity.Request;
import com.utp.request.model.dto.RequestDto;
import com.utp.request.repository.CycleRepository;
import com.utp.request.repository.RequestRepository;
import com.utp.request.service.RequestService;
import com.utp.request.util.Constants;
import com.utp.request.util.CycleUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static com.utp.request.util.Constants.ERROR_ALREADY_APPROVED;
import static com.utp.request.util.Constants.ERROR_MAX_REQUESTS_REACHED;
import static com.utp.request.util.Constants.ERROR_NO_RESPONSE_YET;

@Service
@Slf4j
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final CycleRepository cycleRepository;

    private Mono<String> validateRequest(String numberPlate) {
        log.info("Validating request for number plate: {}", numberPlate);
        return requestRepository.findByNumberPlate(numberPlate)
            .flatMap(request -> {
                if (request.getDateResponse() == null) {
                    return Mono.just(ERROR_NO_RESPONSE_YET);
                }
                if (request.getApproved()) {
                    return Mono.just(ERROR_ALREADY_APPROVED);
                }
                return Mono.just("VALID");
            })
            .defaultIfEmpty("VALID");
    }

    @Override
    public Mono<Request> saveNewRequest(RequestDto request) {
        return validateRequest(request.getNumberPlate())
            .flatMap(validationResult -> {
                if (!validationResult.equals("VALID")) {
                    return Mono.error(new IllegalArgumentException(validationResult));
                }
                String cycleName = CycleUtil.determineCycle();
                return cycleRepository.getCycleByNameCycle(cycleName)
                    .flatMap(cycle -> requestRepository.countByApplicantAndCycle(request.getIdApplicant(),
                                    cycle.getIdCycle())
                        .flatMap(count -> {
                            if (count >= 2) {
                                return Mono.error(new IllegalArgumentException(ERROR_MAX_REQUESTS_REACHED));
                            }
                            request.setIdCycle(cycle.getIdCycle());
                            request.setDateRequest(LocalDateTime.now());
                            request.setApproved(Constants.ID_STATUS_NOT_APPROVED);
                            request.setIdStatus(Constants.ID_STATUS_REGISTERED);
                            return requestRepository.saveNewRequest(request)
                                .flatMap(requestId -> requestRepository.saveWorkflow(
                                            requestId,
                                            Constants.ID_STATUS_REGISTERED,
                                            LocalDateTime.now())
                                        .then(requestRepository.findById(requestId)));
                        }));
            });
    }
}
