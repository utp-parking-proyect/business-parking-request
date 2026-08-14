package com.utp.request.repository;

import com.utp.request.model.entity.Request;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface RequestRepository extends R2dbcRepository<Request, Integer> {

  @Query(value = """
      INSERT INTO requests (id_vehicle, id_cycle, id_status, date_request)
      VALUES (:idVehicle, :idCycle, :idStatus, :dateRequest)
      RETURNING id_request;
      """)
  Mono<Integer> insertRequest(@Param("idVehicle") Integer idVehicle,
                               @Param("idCycle") Integer idCycle,
                               @Param("idStatus") Integer idStatus,
                               @Param("dateRequest") LocalDateTime dateRequest);

  @Query(value = """
      UPDATE requests
      SET id_acceptor = :idAcceptor,
          id_status = :idStatus
      WHERE id_request = :idRequest;
      """)
  Mono<Void> updateAcceptorAndStatus(@Param("idRequest") Integer idRequest,
                                     @Param("idAcceptor") Integer idAcceptor,
                                     @Param("idStatus") Integer idStatus);

  @Query(value = """
      UPDATE requests
      SET id_status = :idStatus,
          date_response = :dateResponse
      WHERE id_request = :idRequest;
      """)
  Mono<Void> updateStatusAndResponse(@Param("idRequest") Integer idRequest,
                                     @Param("idStatus") Integer idStatus,
                                     @Param("dateResponse") LocalDateTime dateResponse);

  Mono<Request> findByIdVehicleAndIdCycle(Integer idVehicle, Integer idCycle);

  Flux<Request> findAllByIdAcceptor(Integer idAcceptor);

  Mono<Long> countByIdAcceptorAndIdStatus(Integer idAcceptor, Integer idStatus);

  @Query(value = """
      SELECT r.* FROM requests r
      JOIN vehicles v ON v.id_vehicle = r.id_vehicle
      WHERE v.id_user = :userId;
      """)
  Flux<Request> findAllByApplicantUserId(@Param("userId") Integer userId);

  @Query(value = """
      SELECT COUNT(*) FROM requests r
      JOIN vehicles v ON v.id_vehicle = r.id_vehicle
      WHERE v.id_user = :userId AND r.id_cycle = :idCycle;
      """)
  Mono<Long> countByApplicantUserIdAndIdCycle(@Param("userId") Integer userId,
                                              @Param("idCycle") Integer idCycle);
}
