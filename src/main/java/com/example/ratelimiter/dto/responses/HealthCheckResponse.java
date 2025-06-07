package com.example.ratelimiter.dto.responses;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HealthCheckResponse {
    private String code;
    private String message;
    private String healthStatus;
    private boolean isMaintenanceModeEnabled;
}
