package com.utp.request.service;

import com.utp.request.client.portal.PortalServiceClient;
import com.utp.request.repository.RequestRepository;
import com.utp.request.util.Constants;
import com.utp.request.util.error.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Comparator;

@Component
@RequiredArgsConstructor
public class AcceptorSelector {

  private final PortalServiceClient portalServiceClient;
  private final RequestRepository requestRepository;

  public Mono<Integer> selectLeastLoaded(Long idCampus) {
    return portalServiceClient.getEligibleAcceptors(idCampus)
        .flatMap(acceptor -> {
          assert acceptor.getIdUser() != null;
          return requestRepository
              .countByIdAcceptorAndIdStatus(acceptor.getIdUser().intValue(),
                  Constants.ID_STATUS_IN_REVISION)
              .map(count -> Tuples.of(acceptor.getIdUser().intValue(), count));
        })
        .collectList()
        .flatMap(counts -> counts.stream()
            .min(Comparator.comparing(Tuple2::getT2))
            .map(Tuple2::getT1)
            .map(Mono::just)
            .orElseGet(() -> Mono.error(
                new ConflictException(Constants.ERROR_NO_ACCEPTOR_AVAILABLE))));
  }
}
