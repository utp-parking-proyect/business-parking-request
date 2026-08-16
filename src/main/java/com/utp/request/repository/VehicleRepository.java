package com.utp.request.repository;

import com.utp.request.model.entity.Vehicle;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface VehicleRepository extends R2dbcRepository<Vehicle, Integer> {

  Mono<Vehicle> findByNumberPlate(String numberPlate);

  Mono<Long> countByIdUserAndIdVehicleStatus(Integer idUser, Integer idVehicleStatus);

  @Query(value = """
      INSERT INTO vehicles (id_vehicle_type, id_user, number_plate, id_vehicle_status)
      VALUES (:idVehicleType, :idUser, :numberPlate, :idVehicleStatus)
      RETURNING id_vehicle;
      """)
  Mono<Integer> insertVehicle(@Param("idVehicleType") Integer idVehicleType,
                              @Param("idUser") Integer idUser,
                              @Param("numberPlate") String numberPlate,
                              @Param("idVehicleStatus") Integer idVehicleStatus);
}
