package com.utp.request.service.impl;

import com.utp.request.model.entity.Request;
import com.utp.request.model.dto.RequestDto;
import com.utp.request.repository.CycleRepository;
import com.utp.request.repository.RequestRepository;
import com.utp.request.repository.WorkflowRepository;
import com.utp.request.service.RequestService;
import com.utp.request.util.Constants;
import com.utp.request.util.CycleUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

import static com.utp.request.util.Constants.ERROR_ALREADY_APPROVED;
import static com.utp.request.util.Constants.ERROR_MAX_REQUESTS_REACHED;
import static com.utp.request.util.Constants.ERROR_NO_RESPONSE_YET;
import static com.utp.request.util.Constants.ID_STATUS_IN_REVISION;

@Service
@Slf4j
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

  private final RequestRepository requestRepository;
  private final CycleRepository cycleRepository;
  private final WorkflowRepository workflowRepository;

  @Override
  @Transactional
  public Mono<Request> saveNewRequest(RequestDto request) {
    return validateRequest(request.getNumberPlate())
        .flatMap(validationResult -> {
          if (!validationResult.equals(Constants.PLATE_VALID)) {
            return Mono.error(new IllegalArgumentException(validationResult));
          }
          return cycleRepository.getCycleByNameCycle(CycleUtil.determineCycle())
              .flatMap(cycle -> checkRequestCount(request.getIdApplicant(), cycle.getIdCycle())
                  .then(saveRequestAndWorkflow(request, cycle.getIdCycle()))
                  .flatMap(savedRequest -> requestRepository.getAcceptorWithFewerRequests()
                      .flatMap(idAcceptor -> updateAcceptorAndSaveWorkflow(savedRequest.getIdRequest(),
                          idAcceptor))
                      .thenReturn(savedRequest)));
        });
  }

  private Mono<String> validateRequest(String numberPlate) {
    log.info("Validating request for number plate: {}", numberPlate);
    return requestRepository.findByNumberPlate(numberPlate)
        .flatMap(request -> {
          if (request.getDateResponse() == null) {
            log.error(ERROR_NO_RESPONSE_YET);
            return Mono.just(ERROR_NO_RESPONSE_YET);
          }
          if (Boolean.TRUE.equals(request.getApproved())) {
            log.error(ERROR_ALREADY_APPROVED);
            return Mono.just(ERROR_ALREADY_APPROVED);
          }
          log.info("Number plate {} is valid", numberPlate);
          return Mono.just(Constants.PLATE_VALID);
        })
        .defaultIfEmpty(Constants.PLATE_VALID);
  }

  private Mono<Void> checkRequestCount(Integer idApplicant, Integer idCycle) {
    return requestRepository.countByApplicantAndCycle(idApplicant, idCycle)
        .flatMap(count -> {
          if (count >= 2) {
            log.error(ERROR_MAX_REQUESTS_REACHED);
            return Mono.error(new IllegalArgumentException(ERROR_MAX_REQUESTS_REACHED));
          }
          return Mono.empty();
        });
  }

  private Mono<Request> saveRequestAndWorkflow(RequestDto request, Integer cycleId) {
    request.setIdCycle(cycleId);
    request.setDateRequest(LocalDateTime.now());
    request.setApproved(Constants.ID_STATUS_NOT_APPROVED);
    request.setIdStatus(Constants.ID_STATUS_REGISTERED);
    return requestRepository.saveNewRequest(request)
        .flatMap(requestId -> workflowRepository.selectWorkflowBefore(request.getNumberPlate())
            .flatMap(workflowId -> workflowRepository
                .updateDateUpdateInWorkflow(workflowId, LocalDateTime.now()))
            .then(workflowRepository
                .saveWorkflow(requestId, Constants.ID_STATUS_REGISTERED, LocalDateTime.now()))
            .then(requestRepository.findById(requestId)));
  }

  private Mono<Void> updateAcceptorAndSaveWorkflow(Integer requestId, Integer idAcceptor) {
    return requestRepository.updateAcceptorInRequest(requestId, idAcceptor, ID_STATUS_IN_REVISION)
        .then(requestRepository.findById(requestId)
            .flatMap(request -> workflowRepository.selectWorkflowBefore(request.getNumberPlate()))
            .flatMap(workflowId -> workflowRepository
                .updateDateUpdateInWorkflow(workflowId, LocalDateTime.now()))
            .then(workflowRepository
                .saveWorkflow(requestId, ID_STATUS_IN_REVISION, LocalDateTime.now())));
  }
}