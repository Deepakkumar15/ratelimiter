package com.example.ratelimiter.service;

import com.example.ratelimiter.dto.responses.HealthCheckResponse;
import org.springframework.stereotype.Service;

@Service
public class HealthCheckService {
    public HealthCheckResponse fetchServiceHealth() {
        return HealthCheckResponse.builder()
                .code("200")
                .message("health check successful")
                .healthStatus("UP")
                .isMaintenanceModeEnabled(false)
                .build();
    }
}
