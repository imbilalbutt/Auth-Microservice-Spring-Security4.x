package com.imbilalbutt.springauthdev.Session.Redis;

import com.imbilalbutt.springauthdev.AuthService.SessionInfo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class SessionRegistry {
    private static final HashMap<String, String> SESSIONS = new HashMap<>();
    private final ValueOperations<String, String> redisSessionStorage;
    private final RedisTemplate<String, String> redisTemplate;

    private static final long SESSION_TIMEOUT_HOURS = 24; // 24 hours

    public SessionRegistry(RedisTemplate<String, String> redisTemplate) {
        redisSessionStorage = redisTemplate.opsForValue();
        this.redisTemplate = redisTemplate;
    }

    public String registerSession(final String username) {
        if (username == null) {
            throw new RuntimeException("Username needs to be provided");
        }

        final String sessionId = generateSessionId();

        try {
            redisTemplate.opsForValue().set(sessionId, username);
        } catch (final Exception e) {
            e.printStackTrace();
            SESSIONS.put(sessionId, username);
        }

        return sessionId;
    }

    public String getUsernameForSession(final String sessionId) {
        try {
            return redisTemplate.opsForValue().get(sessionId);
        } catch (final Exception e) {
            e.printStackTrace();
            return SESSIONS.get(sessionId);
        }
    }

    private String generateSessionId() {
        return new String(
                Base64.getEncoder().encode(
                        UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)
                )
        );
    }

    public void invalidateSession(final String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return;
        }

        final String sessionKey = sessionId;
        redisTemplate.delete(sessionKey);
    }

    public void refreshSession(final String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return;
        }

        final String sessionKey = sessionId;
        final String username = redisSessionStorage.get(sessionKey);

        if (username != null) {
            // Reset expiration time
            redisTemplate.expire(
                    sessionKey,
                    SESSION_TIMEOUT_HOURS,
                    TimeUnit.HOURS
            );
        }
    }
}