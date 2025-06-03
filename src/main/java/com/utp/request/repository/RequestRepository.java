package com.utp.request.repository;

import com.utp.request.model.entity.Request;
import com.utp.request.model.dto.RequestDto;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface RequestRepository extends R2dbcRepository<Request, Integer> {

  @Query(value = """
      INSERT INTO requests (id_applicant, id_vehicle_type, id_cycle, number_plate, date_request,
                is_new, id_status, approved)
      VALUES (:#{#request.idApplicant},
              :#{#request.vehicleType},
              :#{#request.idCycle},
              :#{#request.numberPlate},
              :#{#request.dateRequest},
              :#{#request.isNew},
              :#{#request.idStatus},
              :#{#request.approved})
      RETURNING id_request;
      """)
  Mono<Integer> saveNewRequest(@Param("request") RequestDto request);

  @Query(value = """
      UPDATE requests
      SET id_acceptor = :id_acceptor
      WHERE id_request = :id_request;
      """)
  Mono<Void> updateAcceptorInRequest(@Param("id_request") int requestId,
                                     @Param("id_acceptor") int idAcceptor);

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

  @Query("SELECT * FROM requests WHERE number_plate = :numberPlate;")
  Mono<Request> findByNumberPlate(@Param("numberPlate") String numberPlate);

  @Query("""
      SELECT COUNT(*) FROM requests WHERE id_applicant = :idApplicant
      AND id_cycle = :idCycle;
      """)
  Mono<Long> countByApplicantAndCycle(Integer idApplicant, Integer idCycle);

  @Query(value = """
      SELECT ur.id_user AS id_acceptor,
             COALESCE(COUNT(r.id_request), 0) AS pending_requests
      FROM user_roles ur
      JOIN role ro ON ur.id_role = ro.id_role
      LEFT JOIN requests r ON ur.id_user = r.id_acceptor AND r.date_response IS NULL
      WHERE ro.id_role = 3
      GROUP BY ur.id_user
      ORDER BY pending_requests
      LIMIT 1;
      """)
  Mono<Integer> getAcceptorWithFewerRequests();
}
