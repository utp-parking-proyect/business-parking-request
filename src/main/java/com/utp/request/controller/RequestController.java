package com.utp.request.controller;

import com.utp.request.model.dto.RequestDto;
import com.utp.request.service.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    @PostMapping
    public Mono<ResponseEntity<Map<String, Object>>> saveNewRequest(@RequestBody RequestDto request) {
        return requestService.saveNewRequest(request)
                .map(savedRequest -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("message", "Request registered successfully!");
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(IllegalArgumentException.class, e -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("message", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));
                });
    }
}
