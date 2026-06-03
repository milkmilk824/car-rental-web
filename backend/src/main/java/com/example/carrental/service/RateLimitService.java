package com.example.carrental.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private final Map<String, LocalCounter> localCounters = new ConcurrentHashMap<>();
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final RedisCircuitBreaker redisCircuitBreaker;
    private final String keyPrefix;

    public RateLimitService(
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            RedisCircuitBreaker redisCircuitBreaker,
            @Value("${app.rate-limit.redis-prefix:drivepilot:rate-limit:}") String keyPrefix
    ) {
        this.redisTemplateProvider = redisTemplateProvider;
        this.redisCircuitBreaker = redisCircuitBreaker;
        this.keyPrefix = keyPrefix;
    }

    public boolean allow(String key, int limit, Duration window) {
        if (limit <= 0) {
            return true;
        }
        String namespacedKey = keyPrefix + key;
        Boolean redisAllowed = allowWithRedis(namespacedKey, limit, window);
        if (redisAllowed != null) {
            return redisAllowed;
        }
        return allowWithLocal(namespacedKey, limit, window);
    }

    private Boolean allowWithRedis(String key, int limit, Duration window) {
        try {
            if (!redisCircuitBreaker.allowRequest()) {
                return null;
            }
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate == null) {
                return null;
            }
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == null) {
                return null;
            }
            if (count == 1L) {
                redisTemplate.expire(key, window);
            }
            redisCircuitBreaker.recordSuccess();
            return count <= limit;
        } catch (RuntimeException ex) {
            redisCircuitBreaker.recordFailure();
            return null;
        }
    }

    private boolean allowWithLocal(String key, int limit, Duration window) {
        long now = System.currentTimeMillis();
        long windowMillis = window.toMillis();
        LocalCounter counter = localCounters.compute(key, (ignored, existing) -> {
            if (existing == null || existing.windowStart() + windowMillis <= now) {
                return new LocalCounter(now, 1);
            }
            return new LocalCounter(existing.windowStart(), existing.count() + 1);
        });
        return counter.count() <= limit;
    }

    private record LocalCounter(long windowStart, int count) {
    }
}
