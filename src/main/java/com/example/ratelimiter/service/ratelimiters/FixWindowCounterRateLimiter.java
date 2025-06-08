package com.example.ratelimiter.service.ratelimiters;

import com.example.ratelimiter.configurations.ratelimiter.FixWindowRateLimiterConfigs;
import com.example.ratelimiter.domain.enums.ApiName;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


// Spring Bean is by default singleton, else we can implement singleton design pattern here
@Component
public class FixWindowCounterRateLimiter {
    private static Map<String, Long> rateLimitCounterMap;
    private static EnumMap<ApiName, String> bucketRegistryMap;

    public FixWindowCounterRateLimiter() {
        this.rateLimitCounterMap = new HashMap<>();
        this.bucketRegistryMap = new EnumMap<>(ApiName.class);
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
        String currentBucketNumber = getBucketKey(windowInterval);
        String key = getApiUniqueKey(apiName, currentBucketNumber);
        long requestReceivedWithinCurrentWindow = rateLimitCounterMap.getOrDefault(key, 0L);

        // new window started
        if (requestReceivedWithinCurrentWindow == 0) {
            handleNewWindowTransition(apiName, currentBucketNumber);
        }

        // update the request counter for currently running window
        rateLimitCounterMap.put(key, requestReceivedWithinCurrentWindow + 1);

        return requestReceivedWithinCurrentWindow >= maxRequestAllowed;
    }


    private String getBucketKey(long windowInterval) {
        long bucketNumber = System.currentTimeMillis() / (windowInterval * 1000);
        return Long.toString(bucketNumber);
    }

    private String getApiUniqueKey(ApiName apiName, String currentBucketNumber) {
        return "RATE_LIMIT::" + apiName.name() + "::" + currentBucketNumber;
    }

    @Async
    private void handleNewWindowTransition(ApiName apiName, String newBucketNumber) {
        // clean old bucket from rateLimitCounterMap if present
        if (bucketRegistryMap.containsKey(apiName)) {
            rateLimitCounterMap.remove(getApiUniqueKey(apiName, bucketRegistryMap.get(apiName)));
        }

        // update new bucket number within bucketRegistryMap
        bucketRegistryMap.put(apiName, newBucketNumber);
    }
}
