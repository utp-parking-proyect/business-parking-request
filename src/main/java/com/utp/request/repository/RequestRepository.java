package com.utp.request.repository;

import com.utp.request.model.entity.Request;
import com.utp.request.model.dto.RequestDto;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

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

  @Transactional
  @Query(value = """
      UPDATE requests
      SET id_acceptor = :id_acceptor,
          id_status = :id_status
      WHERE id_request = :id_request;
      """)
  Mono<Void> updateAcceptorInRequest(@Param("id_request") int requestId,
                                     @Param("id_acceptor") int idAcceptor,
                                    @Param("id_status") int idStatus);

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
