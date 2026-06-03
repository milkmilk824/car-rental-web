package com.example.carrental.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RedisCircuitBreaker {

    private final int failureThreshold;
    private final Duration openDuration;
    private final AtomicInteger failures = new AtomicInteger();
    private volatile long openUntilMillis;

    public RedisCircuitBreaker(
            @Value("${app.redis-circuit.failure-threshold:3}") int failureThreshold,
            @Value("${app.redis-circuit.open-duration:PT30S}") Duration openDuration
    ) {
        this.failureThreshold = Math.max(1, failureThreshold);
        this.openDuration = openDuration.isNegative() || openDuration.isZero() ? Duration.ofSeconds(30) : openDuration;
    }

    public boolean allowRequest() {
        return System.currentTimeMillis() >= openUntilMillis;
    }

    public void recordSuccess() {
        failures.set(0);
        openUntilMillis = 0;
    }

    public void recordFailure() {
        if (failures.incrementAndGet() >= failureThreshold) {
            openUntilMillis = System.currentTimeMillis() + openDuration.toMillis();
        }
    }
}
