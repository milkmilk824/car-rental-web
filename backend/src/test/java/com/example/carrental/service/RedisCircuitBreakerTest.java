package com.example.carrental.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RedisCircuitBreakerTest {

    @Test
    void opensAfterConsecutiveFailuresAndRecoversAfterWindow() throws Exception {
        RedisCircuitBreaker circuitBreaker = new RedisCircuitBreaker(2, Duration.ofMillis(120));

        assertThat(circuitBreaker.allowRequest()).isTrue();

        circuitBreaker.recordFailure();
        assertThat(circuitBreaker.allowRequest()).isTrue();

        circuitBreaker.recordFailure();
        assertThat(circuitBreaker.allowRequest()).isFalse();

        Thread.sleep(150);
        assertThat(circuitBreaker.allowRequest()).isTrue();

        circuitBreaker.recordSuccess();
        assertThat(circuitBreaker.allowRequest()).isTrue();
    }
}
