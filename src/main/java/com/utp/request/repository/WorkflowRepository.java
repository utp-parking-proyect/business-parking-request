package com.utp.request.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface WorkflowRepository {
  @Query(value = """
      INSERT INTO workflow (id_request, id_status, date_create)
      VALUES (:requestId, :statusId, :dateCreate);
      """)
  Mono<Void> saveWorkflow(@Param("requestId") int requestId,
                          @Param("statusId") int statusId,
                          @Param("dateCreate") LocalDateTime dateCreate);

  @Query(value = """
      UPDATE workflow
      SET date_update = :dateUpdate
      WHERE id_workflow = :workflowId
      """)
  Mono<Void> updateDateUpdateInWorkflow(@Param("workflowId") int workflowId,
                                        @Param("dateUpdate") LocalDateTime dateUpdate);

  @Query(value = """
      SELECT w.id_workflow
      FROM workflow w
      JOIN `requests` r ON w.id_request = r.id_request
      WHERE r.number_plate = :numberPlate
      AND date_update IS NULL;
      """)
  Mono<Integer> selectWorkflowBefore(@Param("numberPlate") String numberPlate);
}
