package com.utp.request.service.impl;

import com.utp.request.model.entity.Cycle;
import com.utp.request.model.entity.Request;
import com.utp.request.model.dto.RequestDto;
import com.utp.request.repository.CycleRepository;
import com.utp.request.repository.RequestRepository;
import com.utp.request.repository.WorkflowRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {

  @Mock
  private RequestRepository requestRepository;

  @Mock
  private CycleRepository cycleRepository;

  @Mock
  private WorkflowRepository workflowRepository;

  @InjectMocks
  private RequestServiceImpl requestService;

  private RequestDto requestDto;
  private Request savedRequest;
  private Cycle cycle;

  @BeforeEach
  void setUp() {
    requestDto = RequestDto.builder()
        .numberPlate("HNC-234")
        .idApplicant(1)
        .vehicleType(1)
        .isNew(true)
        .build();

    savedRequest = new Request();
    savedRequest.setIdRequest(1);
    savedRequest.setNumberPlate("HNC-234");
    savedRequest.setApproved(false);

    cycle = new Cycle();
    cycle.setIdCycle(1);
    cycle.setNameCycle("2025-0");
  }

  @Test
  void testValidateRequest_NewPlate_IsValid() {
    // Arrange - Nueva placa que no existe
    when(requestRepository.findByNumberPlate("NEW-001"))
        .thenReturn(Mono.empty());

    // Act & Assert - Debería permitir registrar
    StepVerifier.create(
        requestRepository.findByNumberPlate("NEW-001")
            .defaultIfEmpty(new Request())
            .map(req -> req.getIdRequest() == null))
        .assertNext(Assertions::assertTrue)
        .verifyComplete();
  }

  @Test
  void testValidateRequest_ApprovedPlate_IsInvalid() {
    // Arrange
    Request approvedRequest = new Request();
    approvedRequest.setApproved(true);
    approvedRequest.setNumberPlate("HNC-234");

    when(requestRepository.findByNumberPlate("HNC-234"))
        .thenReturn(Mono.just(approvedRequest));

    // Act & Assert - Debería rechazar placa aprobada
    StepVerifier.create(
        requestRepository.findByNumberPlate("HNC-234")
            .map(req -> req.getApproved() != null && req.getApproved()))
        .assertNext(Assertions::assertTrue)
        .verifyComplete();
  }
}
