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
            INSERT INTO workflow (id_request, id_status, date_create)
            VALUES (:requestId, :statusId, :dateCreate);
            """)
    Mono<Void> saveWorkflow(@Param("requestId") int requestId,
                            @Param("statusId") int statusId,
                            @Param("dateCreate") LocalDateTime dateCreate);

    @Query(value = """
            SELECT * FROM requests WHERE number_plate = :numberPlate;
            """)
    Mono<Request> findByNumberPlate(@Param("numberPlate") String numberPlate);

    @Query("SELECT COUNT(*) FROM requests WHERE id_applicant = :idApplicant AND id_cycle = :idCycle")
    Mono<Long> countByApplicantAndCycle(Integer idApplicant, Integer idCycle);
}
