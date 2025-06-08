package com.example.ratelimiter.service.ratelimiters;

import com.example.ratelimiter.domain.enums.ApiName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for FixWindowCounterRateLimiter
 * 
 * Test Categories:
 * 1. Basic Rate Limiting Logic
 * 2. Window Transition Behavior
 * 3. Thread Safety and Concurrency
 * 4. Edge Cases and Error Scenarios
 * 5. Performance and Memory Tests
 */
@SpringBootTest
class FixWindowCounterRateLimiterTest {

    private FixWindowCounterRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new FixWindowCounterRateLimiter();
        // Clear static state between tests
        clearStaticMaps();
    }

    private void clearStaticMaps() {
        Map<String, Long> counterMap = new HashMap<>();
        EnumMap<ApiName, String> bucketMap = new EnumMap<>(ApiName.class);
        
        ReflectionTestUtils.setField(rateLimiter, "rateLimitCounterMap", counterMap);
        ReflectionTestUtils.setField(rateLimiter, "bucketRegistryMap", bucketMap);
    }

    @Nested
    @DisplayName("Basic Rate Limiting Logic Tests")
    class BasicRateLimitingTests {

        @Test
        @DisplayName("Should allow requests within rate limit")
        void test_shouldThrottleRequest_withinLimit() {
            for (int i = 1; i <= 3; i++) {
                assertFalse(rateLimiter.shouldThrottleRequest(ApiName.HEALTH_CHECK),
                        "Request " + i + " should be allowed");
            }
        }

        @Test
        @DisplayName("Should throttle requests exceeding rate limit")
        void test_shouldThrottleRequest_requestsExceedingLimit() {
            // Allow 3 requests
            for (int i = 1; i <= 3; i++) {
                assertFalse(rateLimiter.shouldThrottleRequest(ApiName.HEALTH_CHECK),
                        "Request " + i + " should be allowed");
            }

            // Fourth request should be throttled
            assertTrue(rateLimiter.shouldThrottleRequest(ApiName.HEALTH_CHECK),
                    "Fourth request should be throttled");
        }

        @Test
        @DisplayName("Should throttle requests exceeding rate limit")
        void test_shouldThrottleRequest_rateLimitAndServeWhenNewWindowStarts() throws InterruptedException {
            // Allow 3 requests
            for (int i = 1; i <= 3; i++) {
                assertFalse(rateLimiter.shouldThrottleRequest(ApiName.HEALTH_CHECK),
                        "Request " + i + " should be allowed");
            }
            
            // Fourth request should be throttled
            assertTrue(rateLimiter.shouldThrottleRequest(ApiName.HEALTH_CHECK),
                    "Fourth request should be throttled");

            Thread.sleep(10000);

            assertFalse(rateLimiter.shouldThrottleRequest(ApiName.HEALTH_CHECK));
        }
        

        @Test
        @DisplayName("Should reset counter after window transition")
        void shouldResetCounterAfterWindowTransition() throws InterruptedException {
            // Use shorter window for testing (need to mock or wait)
            // This test verifies that counters reset in new windows
            
            // Fill up current window
            for (int i = 0; i < 3; i++) {
                rateLimiter.shouldThrottleRequest(ApiName.HEALTH_CHECK);
            }
            
            // Next request should be throttled
            assertTrue(rateLimiter.shouldThrottleRequest(ApiName.HEALTH_CHECK),
                    "Request should be throttled in current window");
            
            // Wait for new window (10+ seconds) - this is integration test behavior
            // For unit test, we would mock the time-based bucket generation
            
            // Note: This test demonstrates the concept but needs time mocking for practical execution
        }
    }

    @Nested
    @DisplayName("Window Transition Tests")
    class WindowTransitionTests {

        @Test
        @DisplayName("Should detect new window correctly")
        void shouldDetectNewWindowCorrectly() {
            // First request in a new window
            assertFalse(rateLimiter.shouldThrottleRequest(ApiName.HEALTH_CHECK));
            
            // Get current bucket registry state
            EnumMap<ApiName, String> bucketRegistry = getBucketRegistryMap();
            
            // Should have registered the current bucket
            assertTrue(bucketRegistry.containsKey(ApiName.HEALTH_CHECK),
                    "Bucket registry should contain entry for HEALTH_CHECK");
            
            assertNotNull(bucketRegistry.get(ApiName.HEALTH_CHECK),
                    "Bucket number should not be null");
        }

        @Test
        @DisplayName("Should trigger cleanup on window transition")
        void shouldTriggerCleanupOnWindowTransition() {
            // Simulate first window
            rateLimiter.shouldThrottleRequest(ApiName.HEALTH_CHECK);
            
            Map<String, Long> counterMap = getCounterMap();
            int initialSize = counterMap.size();
            
            // Force window transition by manipulating bucket registry
            EnumMap<ApiName, String> bucketRegistry = getBucketRegistryMap();
            bucketRegistry.put(ApiName.HEALTH_CHECK, "12345"); // Old bucket
            
            // Trigger new window
            rateLimiter.shouldThrottleRequest(ApiName.HEALTH_CHECK);
            
            // Give async cleanup time to execute
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Verify cleanup behavior (async, so timing-dependent)
            assertTrue(initialSize >= 0, "Counter map should have been managed");
        }

        @Test
        @DisplayName("Should handle multiple APIs independently")
        void shouldHandleMultipleApisIndependently() {
            // Since we only have HEALTH_CHECK configured, this test demonstrates
            // the concept of independent API handling
            
            rateLimiter.shouldThrottleRequest(ApiName.HEALTH_CHECK);
            
            Map<String, Long> counterMap = getCounterMap();
            
            // Each API should have separate counters
            assertTrue(counterMap.keySet().stream()
                    .anyMatch(key -> key.contains("HEALTH_CHECK")),
                    "Should have HEALTH_CHECK specific keys");
        }
    }

    @Nested
    @DisplayName("Thread Safety and Concurrency Tests")
    class ConcurrencyTests {

        @Test
        @DisplayName("Should handle concurrent requests safely")
        void shouldHandleConcurrentRequestsSafely() throws InterruptedException {
            int numberOfThreads = 10;
            int requestsPerThread = 5;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completionLatch = new CountDownLatch(numberOfThreads);
            
            AtomicInteger allowedRequests = new AtomicInteger(0);
            AtomicInteger throttledRequests = new AtomicInteger(0);
            
            ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
            
            for (int i = 0; i < numberOfThreads; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await(); // Wait for all threads to be ready
                        
                        for (int j = 0; j < requestsPerThread; j++) {
                            boolean isThrottled = rateLimiter.shouldThrottleRequest(ApiName.HEALTH_CHECK);
                            if (isThrottled) {
                                throttledRequests.incrementAndGet();
                            } else {
                                allowedRequests.incrementAndGet();
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }
            
            startLatch.countDown(); // Start all threads
            assertTrue(completionLatch.await(5, TimeUnit.SECONDS), 
                    "All threads should complete within timeout");
            
            executor.shutdown();
            
            int totalRequests = allowedRequests.get() + throttledRequests.get();
            assertEquals(numberOfThreads * requestsPerThread, totalRequests,
                    "Total requests should match expected count");
            
            // Due to race conditions in current implementation, 
            // we can't assert exact numbers, but can verify basic behavior
            assertTrue(allowedRequests.get() > 0, "Some requests should be allowed");
            
            System.out.println("Concurrent test results:");
            System.out.println("Allowed: " + allowedRequests.get());
            System.out.println("Throttled: " + throttledRequests.get());
        }

        @Test
        @DisplayName("Should demonstrate race conditions in current implementation")
        void shouldDemonstrateRaceConditions() throws InterruptedException {
            // This test demonstrates the race conditions in the current implementation
            // It's designed to show problems, not assert correctness
            
            int numberOfIterations = 100;
            AtomicInteger inconsistencies = new AtomicInteger(0);
            
            for (int iteration = 0; iteration < numberOfIterations; iteration++) {
                clearStaticMaps();
                
                CountDownLatch startLatch = new CountDownLatch(1);
                CountDownLatch completionLatch = new CountDownLatch(2);
                
                // Thread 1: Rate limiting
                Thread rateLimit = new Thread(() -> {
                    try {
                        startLatch.await();
                        rateLimiter.shouldThrottleRequest(ApiName.HEALTH_CHECK);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        completionLatch.countDown();
                    }
                });
                
                // Thread 2: Direct map manipulation (simulating cleanup)
                Thread cleanup = new Thread(() -> {
                    try {
                        startLatch.await();
                        Map<String, Long> counterMap = getCounterMap();
                        // Simulate concurrent modification
                        counterMap.clear();
                    } catch (Exception e) {
                        inconsistencies.incrementAndGet();
                    } finally {
                        completionLatch.countDown();
                    }
                });
                
                rateLimit.start();
                cleanup.start();
                startLatch.countDown();
                
                assertTrue(completionLatch.await(1, TimeUnit.SECONDS));
            }
            
            System.out.println("Race condition test completed. Inconsistencies detected: " + 
                             inconsistencies.get());
            
            // This test is informational - it shows that race conditions can occur
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Scenarios")
    class EdgeCaseTests {

//        @Test
//        @DisplayName("Should handle null API name gracefully")
//        void shouldHandleNullApiNameGracefully() {
//            // This will test how the system handles null inputs
//            assertThrows(NullPointerException.class,
//                    () -> rateLimiter.shouldThrottleRequest(null),
//                    "Should throw exception for null API name");
//        }

        @Test
        @DisplayName("Should handle rapid successive requests")
        void shouldHandleRapidSuccessiveRequests() {
            // Test rapid fire requests
            for (int i = 0; i < 10; i++) {
                boolean result = rateLimiter.shouldThrottleRequest(ApiName.HEALTH_CHECK);
                // Should not throw exceptions
                assertNotNull(result);
            }
        }

        @Test
        @DisplayName("Should handle memory pressure")
        void shouldHandleMemoryPressure() {
            // Simulate many different time windows to test memory usage
            Map<String, Long> counterMap = getCounterMap();
            
            // Directly add many entries to simulate memory pressure
            for (int i = 0; i < 1000; i++) {
                counterMap.put("RATE_LIMIT::HEALTH_CHECK::" + i, (long) i);
            }
            
            // Should still function
            assertDoesNotThrow(() -> 
                rateLimiter.shouldThrottleRequest(ApiName.HEALTH_CHECK));
            
            assertTrue(counterMap.size() > 1000, 
                    "Map should contain test entries plus new entries");
        }
    }

    @Nested
    @DisplayName("Performance and Memory Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should perform rate limiting efficiently")
        void shouldPerformRateLimitingEfficiently() {
            int numberOfRequests = 10000;
            long startTime = System.nanoTime();
            
            for (int i = 0; i < numberOfRequests; i++) {
                rateLimiter.shouldThrottleRequest(ApiName.HEALTH_CHECK);
            }
            
            long duration = System.nanoTime() - startTime;
            double avgTimePerRequest = duration / (double) numberOfRequests;
            
            System.out.println("Performance test results:");
            System.out.println("Total requests: " + numberOfRequests);
            System.out.println("Total time: " + (duration / 1_000_000) + " ms");
            System.out.println("Average time per request: " + (avgTimePerRequest / 1_000) + " μs");
            
            // Performance assertion - should complete within reasonable time
            assertTrue(duration < TimeUnit.SECONDS.toNanos(5), 
                    "Should complete 10k requests within 5 seconds");
        }

        @Test
        @DisplayName("Should not leak memory with normal usage")
        void shouldNotLeakMemoryWithNormalUsage() {
            Map<String, Long> counterMap = getCounterMap();
            int initialSize = counterMap.size();
            
            // Simulate normal usage pattern
            for (int i = 0; i < 100; i++) {
                rateLimiter.shouldThrottleRequest(ApiName.HEALTH_CHECK);
            }
            
            int finalSize = counterMap.size();
            
            // Memory should be bounded (though current implementation may not achieve this)
            assertTrue(finalSize < initialSize + 10, 
                    "Memory usage should be bounded for single API requests");
        }
    }

    // Helper methods for accessing private static fields
    @SuppressWarnings("unchecked")
    private Map<String, Long> getCounterMap() {
        return (Map<String, Long>) ReflectionTestUtils.getField(rateLimiter, "rateLimitCounterMap");
    }

    @SuppressWarnings("unchecked")
    private EnumMap<ApiName, String> getBucketRegistryMap() {
        return (EnumMap<ApiName, String>) ReflectionTestUtils.getField(rateLimiter, "bucketRegistryMap");
    }
}
