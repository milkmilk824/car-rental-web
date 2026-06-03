package com.example.carrental.service;

import com.example.carrental.dto.CarDtos;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class HotCacheServiceTest {

    @Test
    void redisHitSkipsSourceLoader() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        ObjectMapper mapper = objectMapper();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("drivepilot:cache:car:categories"))
                .thenReturn(mapper.writeValueAsString(List.of(new CarDtos.CategoryResponse(1L, "SUV", "城市 SUV"))));
        HotCacheService cacheService = new HotCacheService(mapper, provider(redisTemplate), circuitBreaker(), "drivepilot:cache:");
        AtomicInteger loads = new AtomicInteger();

        List<CarDtos.CategoryResponse> result = cacheService.getOrLoad(
                "car:categories",
                Duration.ofMinutes(10),
                new TypeReference<>() {
                },
                () -> {
                    loads.incrementAndGet();
                    return List.of();
                }
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).categoryName()).isEqualTo("SUV");
        assertThat(loads).hasValue(0);
    }

    @Test
    void redisMissLoadsSourceAndWritesCache() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("drivepilot:cache:car:categories")).thenReturn(null);
        HotCacheService cacheService = new HotCacheService(objectMapper(), provider(redisTemplate), circuitBreaker(), "drivepilot:cache:");

        List<CarDtos.CategoryResponse> result = cacheService.getOrLoad(
                "car:categories",
                Duration.ofMinutes(10),
                new TypeReference<>() {
                },
                () -> List.of(new CarDtos.CategoryResponse(2L, "商务", "商务接待"))
        );

        assertThat(result).hasSize(1);
        verify(valueOperations).set(eq("drivepilot:cache:car:categories"), contains("商务"), eq(Duration.ofMinutes(10)));
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    private RedisCircuitBreaker circuitBreaker() {
        return new RedisCircuitBreaker(3, Duration.ofSeconds(1));
    }

    @SuppressWarnings("NullableProblems")
    private ObjectProvider<StringRedisTemplate> provider(StringRedisTemplate redisTemplate) {
        return new ObjectProvider<>() {
            @Override
            public StringRedisTemplate getObject(Object... args) {
                return redisTemplate;
            }

            @Override
            public StringRedisTemplate getIfAvailable() {
                return redisTemplate;
            }

            @Override
            public StringRedisTemplate getIfUnique() {
                return redisTemplate;
            }

            @Override
            public StringRedisTemplate getObject() {
                return redisTemplate;
            }
        };
    }
}
