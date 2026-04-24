package com.imbilalbutt.springauthdev.Session.Redis;

import com.imbilalbutt.springauthdev.Config.SecurityAuditLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionRegistry {

    private final ValueOperations<String, String> redisSessionStorage;
    private final RedisTemplate<String, String> redisTemplate;
    private final SecurityAuditLogger auditLogger;

    private static final long SESSION_TIMEOUT_HOURS = 24;
    private static final String SESSION_KEY_PREFIX = "session:";

    public SessionRegistry(RedisTemplate<String, String> redisTemplate, SecurityAuditLogger auditLogger) {
        redisSessionStorage = redisTemplate.opsForValue();
        this.redisTemplate = redisTemplate;
        this.auditLogger = auditLogger;
    }

    public String registerSession(final String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        final String sessionId = generateSessionId();
        final String sessionKey = SESSION_KEY_PREFIX + sessionId;

        try {
            redisTemplate.opsForValue().set(
                sessionKey,
                username,
                SESSION_TIMEOUT_HOURS,
                TimeUnit.HOURS
            );
            log.info("Session created for user: {} with ID: {}", username, sessionId);
            auditLogger.logSessionCreated(username, sessionId);
        } catch (final Exception e) {
            log.error("Failed to create session in Redis for user: {}", username, e);
            throw new SessionCreationException("Failed to create session. Please try again.", e);
        }

        return sessionId;
    }

    public String getUsernameForSession(final String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return null;
        }

        final String sessionKey = SESSION_KEY_PREFIX + sessionId;
        try {
            return redisTemplate.opsForValue().get(sessionKey);
        } catch (final Exception e) {
            log.error("Failed to retrieve session: {}", sessionId, e);
            return null;
        }
    }

    private String generateSessionId() {
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
    }

    public void invalidateSession(final String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return;
        }

        final String sessionKey = SESSION_KEY_PREFIX + sessionId;
        try {
            // Get username before deleting for audit log
            String username = redisTemplate.opsForValue().get(sessionKey);
            
            Boolean deleted = redisTemplate.delete(sessionKey);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("Session invalidated: {}", sessionId);
                if (username != null) {
                    auditLogger.logSessionInvalidated(username, sessionId);
                }
            }
        } catch (final Exception e) {
            log.error("Failed to invalidate session: {}", sessionId, e);
        }
    }

    public void refreshSession(final String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return;
        }

        final String sessionKey = SESSION_KEY_PREFIX + sessionId;
        try {
            Boolean exists = redisTemplate.hasKey(sessionKey);
            if (Boolean.TRUE.equals(exists)) {
                redisTemplate.expire(
                    sessionKey,
                    SESSION_TIMEOUT_HOURS,
                    TimeUnit.HOURS
                );
            }
        } catch (final Exception e) {
            log.error("Failed to refresh session: {}", sessionId, e);
        }
    }

    public boolean isSessionValid(final String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return false;
        }

        final String sessionKey = SESSION_KEY_PREFIX + sessionId;
        try {
            Boolean exists = redisTemplate.hasKey(sessionKey);
            return Boolean.TRUE.equals(exists);
        } catch (final Exception e) {
            log.error("Failed to check session validity: {}", sessionId, e);
            return false;
        }
    }
}