package com.example.carrental.security;

import com.example.carrental.common.BusinessException;
import com.example.carrental.domain.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {

    private final Map<String, CurrentUser> sessions = new ConcurrentHashMap<>();
    private final Map<String, Long> blacklistedTokens = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final String sessionStore;
    private final String redisPrefix;
    private final Duration tokenTtl;

    public TokenService(
            ObjectMapper objectMapper,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            @Value("${app.session.store:auto}") String sessionStore,
            @Value("${app.session.redis-prefix:drivepilot:session:}") String redisPrefix,
            @Value("${app.session.token-ttl:PT12H}") Duration tokenTtl
    ) {
        this.objectMapper = objectMapper;
        this.redisTemplateProvider = redisTemplateProvider;
        this.sessionStore = sessionStore == null ? "auto" : sessionStore.trim().toLowerCase();
        this.redisPrefix = redisPrefix;
        this.tokenTtl = tokenTtl;
    }

    public String issue(User user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        CurrentUser currentUser = new CurrentUser(user.getId(), user.getUsername(), user.getRole());
        boolean storedInRedis = false;
        if (redisConfigured()) {
            storedInRedis = storeInRedis(token, currentUser);
        }
        if (redisRequired() && !storedInRedis) {
            throw BusinessException.badRequest("Redis 会话存储不可用");
        }
        if (!redisRequired() || !storedInRedis) {
            sessions.put(token, currentUser);
        }
        return token;
    }

    public Optional<CurrentUser> resolve(String rawHeader) {
        String token = extract(rawHeader);
        if (token == null) {
            return Optional.empty();
        }
        if (isBlacklisted(token)) {
            return Optional.empty();
        }
        if (redisConfigured()) {
            Optional<CurrentUser> redisUser = resolveFromRedis(token);
            if (redisUser.isPresent()) {
                return redisUser;
            }
            if (redisRequired()) {
                return Optional.empty();
            }
        }
        return Optional.ofNullable(sessions.get(token));
    }

    public CurrentUser require(String rawHeader) {
        return resolve(rawHeader).orElseThrow(() -> BusinessException.unauthorized("登录已过期或 Token 无效"));
    }

    public void revoke(String rawHeader) {
        String token = extract(rawHeader);
        if (token == null) {
            return;
        }
        sessions.remove(token);
        blacklistedTokens.put(token, System.currentTimeMillis() + tokenTtl.toMillis());
        if (redisConfigured()) {
            revokeFromRedis(token);
        }
    }

    private boolean redisConfigured() {
        return "redis".equals(sessionStore) || "auto".equals(sessionStore);
    }

    private boolean redisRequired() {
        return "redis".equals(sessionStore);
    }

    private boolean storeInRedis(String token, CurrentUser currentUser) {
        try {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate == null) {
                return false;
            }
            redisTemplate.opsForValue().set(redisKey(token), objectMapper.writeValueAsString(currentUser), tokenTtl);
            return true;
        } catch (RuntimeException | JsonProcessingException ex) {
            return false;
        }
    }

    private Optional<CurrentUser> resolveFromRedis(String token) {
        try {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate == null) {
                return Optional.empty();
            }
            String payload = redisTemplate.opsForValue().get(redisKey(token));
            if (payload == null || payload.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(payload, CurrentUser.class));
        } catch (RuntimeException | JsonProcessingException ex) {
            return Optional.empty();
        }
    }

    private void revokeFromRedis(String token) {
        try {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate == null) {
                return;
            }
            redisTemplate.delete(redisKey(token));
            redisTemplate.opsForValue().set(blacklistKey(token), "1", tokenTtl);
        } catch (RuntimeException ignored) {
            // Local blacklist still protects the current JVM when Redis is unavailable.
        }
    }

    private boolean isBlacklisted(String token) {
        Long expiresAt = blacklistedTokens.get(token);
        if (expiresAt != null) {
            if (expiresAt > System.currentTimeMillis()) {
                return true;
            }
            blacklistedTokens.remove(token);
        }
        if (!redisConfigured()) {
            return false;
        }
        try {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            return redisTemplate != null && Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey(token)));
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private String extract(String rawHeader) {
        if (rawHeader == null || rawHeader.isBlank()) {
            return null;
        }
        String token = rawHeader.startsWith("Bearer ") ? rawHeader.substring(7) : rawHeader;
        return token.isBlank() ? null : token;
    }

    private String redisKey(String token) {
        return redisPrefix + token;
    }

    private String blacklistKey(String token) {
        return redisPrefix + "blacklist:" + token;
    }
}
