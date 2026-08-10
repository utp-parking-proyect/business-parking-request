package com.utp.request.client.users;

import com.utp.request.generated.client.users.model.CycleResponse;
import com.utp.request.generated.client.users.model.Role;
import com.utp.request.generated.client.users.model.UserResponse;
import com.utp.request.util.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UsersServiceClient {

  private final WebClient usersWebClient;

  public Mono<UserResponse> getUserById(Long id) {
    return usersWebClient.get()
        .uri("/users/{id}", id)
        .retrieve()
        .bodyToMono(UserResponse.class);
  }

  public Flux<UserResponse> getEligibleAcceptors(Long idCampus) {
    return usersWebClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/users/by-role/{roleName}")
            .queryParamIfPresent("idCampus", Optional.ofNullable(idCampus))
            .build(Constants.ROLE_NAME_SAE))
        .retrieve()
        .bodyToFlux(UserResponse.class);
  }

  public boolean hasSaeRole(UserResponse user) {
    if (user.getRoles() == null) {
      return false;
    }
    return user.getRoles().stream()
        .map(Role::getName)
        .anyMatch(Constants.ROLE_NAME_SAE::equalsIgnoreCase);
  }

  public Mono<CycleResponse> getCurrentCycle() {
    return usersWebClient.get()
        .uri("/cycles/current")
        .retrieve()
        .bodyToMono(CycleResponse.class);
  }

  public Mono<CycleResponse> getCycleById(Long id) {
    return usersWebClient.get()
        .uri("/cycles/{id}", id)
        .retrieve()
        .bodyToMono(CycleResponse.class);
  }
}
