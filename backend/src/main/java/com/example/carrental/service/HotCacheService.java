package com.example.carrental.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
public class HotCacheService {

    private final Map<String, CacheEntry> localCache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final RedisCircuitBreaker redisCircuitBreaker;
    private final String cachePrefix;

    public HotCacheService(
            ObjectMapper objectMapper,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            RedisCircuitBreaker redisCircuitBreaker,
            @Value("${app.cache.redis-prefix:drivepilot:cache:}") String cachePrefix
    ) {
        this.objectMapper = objectMapper;
        this.redisTemplateProvider = redisTemplateProvider;
        this.redisCircuitBreaker = redisCircuitBreaker;
        this.cachePrefix = cachePrefix;
    }

    public <T> T getOrLoad(String key, Duration ttl, TypeReference<T> type, Supplier<T> loader) {
        String namespacedKey = cachePrefix + key;
        T redisValue = getFromRedis(namespacedKey, type);
        if (redisValue != null) {
            return redisValue;
        }
        T localValue = getFromLocal(namespacedKey, type);
        if (localValue != null) {
            return localValue;
        }
        T value = loader.get();
        cache(namespacedKey, value, ttl);
        return value;
    }

    public void evictPrefix(String keyPrefix) {
        String namespacedPrefix = cachePrefix + keyPrefix;
        evictLocalPrefix(namespacedPrefix);
        try {
            if (!redisCircuitBreaker.allowRequest()) {
                return;
            }
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate == null) {
                return;
            }
            scanAndDelete(redisTemplate, namespacedPrefix + "*");
            redisCircuitBreaker.recordSuccess();
        } catch (RuntimeException ignored) {
            redisCircuitBreaker.recordFailure();
            // Cache eviction must not break business writes.
        }
    }

    private <T> T getFromRedis(String key, TypeReference<T> type) {
        try {
            if (!redisCircuitBreaker.allowRequest()) {
                return null;
            }
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate == null) {
                return null;
            }
            String payload = redisTemplate.opsForValue().get(key);
            redisCircuitBreaker.recordSuccess();
            if (payload == null || payload.isBlank()) {
                return null;
            }
            return objectMapper.readValue(payload, type);
        } catch (JsonProcessingException ex) {
            return null;
        } catch (RuntimeException ex) {
            redisCircuitBreaker.recordFailure();
            return null;
        }
    }

    private <T> T getFromLocal(String key, TypeReference<T> type) {
        CacheEntry entry = localCache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expiresAt() <= System.currentTimeMillis()) {
            localCache.remove(key);
            return null;
        }
        try {
            return objectMapper.readValue(entry.payload(), type);
        } catch (JsonProcessingException ex) {
            localCache.remove(key);
            return null;
        }
    }

    private void cache(String key, Object value, Duration ttl) {
        try {
            String payload = objectMapper.writeValueAsString(value);
            localCache.put(key, new CacheEntry(payload, System.currentTimeMillis() + ttl.toMillis()));
            if (redisCircuitBreaker.allowRequest()) {
                StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
                if (redisTemplate != null) {
                    redisTemplate.opsForValue().set(key, payload, ttl);
                    redisCircuitBreaker.recordSuccess();
                }
            }
        } catch (JsonProcessingException ignored) {
            // Cache serialization failures should never fail the request that produced the value.
        } catch (RuntimeException ignored) {
            redisCircuitBreaker.recordFailure();
            // Cache write failures should never fail the request that produced the value.
        }
    }

    private void evictLocalPrefix(String prefix) {
        Iterator<String> keys = localCache.keySet().iterator();
        while (keys.hasNext()) {
            if (keys.next().startsWith(prefix)) {
                keys.remove();
            }
        }
    }

    private void scanAndDelete(StringRedisTemplate redisTemplate, String pattern) {
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(500).build();
            List<byte[]> batch = new ArrayList<>(500);
            try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                while (cursor.hasNext()) {
                    batch.add(cursor.next());
                    if (batch.size() >= 500) {
                        connection.keyCommands().del(batch.toArray(new byte[0][]));
                        batch.clear();
                    }
                }
            }
            if (!batch.isEmpty()) {
                connection.keyCommands().del(batch.toArray(new byte[0][]));
            }
            return null;
        });
    }

    private record CacheEntry(String payload, long expiresAt) {
    }
}
