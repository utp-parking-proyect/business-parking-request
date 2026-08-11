package com.utp.request.util.security;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
public class AuthenticatedUserProvider {

  public Mono<Long> getAuthenticatedUserId() {
    return ReactiveSecurityContextHolder.getContext()
        .map(context -> Objects.requireNonNull(context.getAuthentication()).getPrincipal())
        .cast(Jwt.class)
        .map(jwt -> jwt.getClaim("userId"))
        .cast(Number.class)
        .map(Number::longValue);
  }
}
