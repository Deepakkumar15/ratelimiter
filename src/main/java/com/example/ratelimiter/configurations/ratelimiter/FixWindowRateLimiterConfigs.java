package com.example.ratelimiter.configurations.ratelimiter;

import com.example.ratelimiter.domain.enums.ApiName;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class FixWindowRateLimiterConfigs {
    private FixWindowRateLimiterConfigs() {}

    private static final EnumMap<ApiName, List<Long>> configRegistry = new EnumMap<>(ApiName.class);
    static {
        configRegistry.put(ApiName.HEALTH_CHECK, List.of(10L, 3L));
    }

    public static List<Long> fetchRateLimitConfigs(ApiName apiName) {
        return configRegistry.getOrDefault(apiName, new ArrayList<>());
    }
}
