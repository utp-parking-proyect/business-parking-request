package com.utp.request.repository;

import com.utp.request.model.entity.Workflow;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface WorkflowRepository extends R2dbcRepository<Workflow, Integer> {

  @Query(value = """
      INSERT INTO workflow (id_request, id_status, date_status_change, observation)
      VALUES (:requestId, :statusId, :dateStatusChange, :observation);
      """)
  Mono<Void> saveWorkflow(@Param("requestId") Integer requestId,
                          @Param("statusId") Integer statusId,
                          @Param("dateStatusChange") LocalDateTime dateStatusChange,
                          @Param("observation") String observation);
}
