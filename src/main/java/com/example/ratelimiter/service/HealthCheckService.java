package com.example.ratelimiter.service;

import com.example.ratelimiter.domain.enums.ApiName;
import com.example.ratelimiter.dto.responses.HealthCheckResponse;
import com.example.ratelimiter.service.ratelimiters.FixWindowCounterRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class HealthCheckService {
    private final FixWindowCounterRateLimiter rateLimiter;

    public HealthCheckResponse fetchServiceHealth() {
        if (rateLimiter.shouldThrottleRequest(ApiName.HEALTH_CHECK)) {
            return HealthCheckResponse.builder()
                    .code("429")
                    .message("Request rate limit exceeded. Please wait and try again later.")
                    .build();
        }
        return HealthCheckResponse.builder()
                .code("200")
                .message("health check successful")
                .healthStatus("UP")
                .isMaintenanceModeEnabled(false)
                .build();
    }
}
