package com.example.ratelimiter.service.ratelimiters;

import com.example.ratelimiter.configurations.ratelimiter.FixWindowRateLimiterConfigs;
import com.example.ratelimiter.domain.enums.ApiName;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


// Spring Bean is by default singleton, else we can implement singleton design pattern here
@Component
public class FixWindowCounterRateLimiter {
    private static Map<String, Long> rateLimitCounterMap;

    public FixWindowCounterRateLimiter() {
        this.rateLimitCounterMap = new HashMap<>();
    }


    public boolean shouldThrottleRequest(ApiName apiName) {
        return doRateLimit(apiName);
    }


    private boolean doRateLimit(ApiName apiName) {
        List<Long> rateLimitConfigs = FixWindowRateLimiterConfigs.fetchRateLimitConfigs(apiName);
        if (rateLimitConfigs.isEmpty()) {
            return false;
        }

        long windowInterval = rateLimitConfigs.get(0);
        long maxRequestAllowed = rateLimitConfigs.get(1);
        String key = "RATE_LIMIT::" + apiName.name() + "::" + getBucketKey(windowInterval);
        long requestReceivedWithinCurrentWindow = rateLimitCounterMap.getOrDefault(key, 0L);
        // update the request counter for currently running window
        rateLimitCounterMap.put(key, requestReceivedWithinCurrentWindow + 1);

        return requestReceivedWithinCurrentWindow >= maxRequestAllowed;
    }


    private String getBucketKey(long windowInterval) {
        long bucketNumber = System.currentTimeMillis() / (windowInterval * 1000);
        return Long.toString(bucketNumber);
    }
}
