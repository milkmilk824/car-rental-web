package com.example.carrental.security;

import com.example.carrental.common.Enums.UserRole;
import com.example.carrental.domain.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class TokenServiceRedisTest {

    @Test
    void issueStoresSessionInRedisWhenRedisModeIsEnabled() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        TokenService tokenService = new TokenService(
                new ObjectMapper(),
                provider(redisTemplate),
                "redis",
                "drivepilot:test:session:",
                Duration.ofHours(2)
        );

        String token = tokenService.issue(user(12L, "admin", UserRole.ADMIN));

        assertThat(token).isNotBlank();
        verify(valueOperations).set(
                eq("drivepilot:test:session:" + token),
                any(String.class),
                eq(Duration.ofHours(2))
        );
    }

    @Test
    void resolveReadsSessionFromRedisAndSupportsBearerHeader() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("drivepilot:test:session:abc123"))
                .thenReturn(new ObjectMapper().writeValueAsString(new CurrentUser(7L, "staff", UserRole.STORE_STAFF)));

        TokenService tokenService = new TokenService(
                new ObjectMapper(),
                provider(redisTemplate),
                "redis",
                "drivepilot:test:session:",
                Duration.ofHours(2)
        );

        CurrentUser currentUser = tokenService.resolve("Bearer abc123").orElseThrow();

        assertThat(currentUser.id()).isEqualTo(7L);
        assertThat(currentUser.username()).isEqualTo("staff");
        assertThat(currentUser.role()).isEqualTo(UserRole.STORE_STAFF);
    }

    @Test
    void autoModeFallsBackToLocalSessionWhenRedisIsUnavailable() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis down"));

        TokenService tokenService = new TokenService(
                new ObjectMapper(),
                provider(redisTemplate),
                "auto",
                "drivepilot:test:session:",
                Duration.ofHours(2)
        );

        String token = tokenService.issue(user(3L, "zhangsan", UserRole.USER));

        CurrentUser currentUser = tokenService.resolve("Bearer " + token).orElseThrow();
        assertThat(currentUser.id()).isEqualTo(3L);
        assertThat(currentUser.username()).isEqualTo("zhangsan");
        assertThat(currentUser.role()).isEqualTo(UserRole.USER);
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

    private User user(Long id, String username, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        return user;
    }
}
