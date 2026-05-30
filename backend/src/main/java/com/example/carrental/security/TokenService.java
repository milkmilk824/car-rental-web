package com.example.carrental.security;

import com.example.carrental.common.BusinessException;
import com.example.carrental.domain.User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {

    private final Map<String, CurrentUser> sessions = new ConcurrentHashMap<>();

    public String issue(User user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, new CurrentUser(user.getId(), user.getUsername(), user.getRole()));
        return token;
    }

    public Optional<CurrentUser> resolve(String rawHeader) {
        if (rawHeader == null || rawHeader.isBlank()) {
            return Optional.empty();
        }
        String token = rawHeader.startsWith("Bearer ") ? rawHeader.substring(7) : rawHeader;
        return Optional.ofNullable(sessions.get(token));
    }

    public CurrentUser require(String rawHeader) {
        return resolve(rawHeader).orElseThrow(() -> BusinessException.unauthorized("登录已过期或 Token 无效"));
    }
}
