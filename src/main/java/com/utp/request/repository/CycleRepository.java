package com.utp.request.repository;

import com.utp.request.model.entity.Cycle;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface CycleRepository extends R2dbcRepository<Cycle, Integer> {
  Mono<Cycle> getCycleByNameCycle(String nameCycle);
}
