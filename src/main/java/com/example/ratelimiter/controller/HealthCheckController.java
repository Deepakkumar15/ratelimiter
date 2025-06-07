package com.example.ratelimiter.controller;

import com.example.ratelimiter.dto.responses.HealthCheckResponse;
import com.example.ratelimiter.service.HealthCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/actuator")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class HealthCheckController {
    private final HealthCheckService healthCheckService;


    @GetMapping("/health")
    public @ResponseBody HealthCheckResponse checkHealth() {
        return healthCheckService.fetchServiceHealth();
    }
}
