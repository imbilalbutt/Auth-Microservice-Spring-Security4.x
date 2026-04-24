package com.imbilalbutt.springauthdev.Config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityAuditLogger {

    private static final String AUDIT_LOGGER = "AUDIT";

    public void logAuthenticationSuccess(String username) {
        String ipAddress = getClientIpAddress();
        log.info("AUTH_SUCCESS: user={} ip={}", username, ipAddress);
    }

    public void logAuthenticationFailure(String username, String reason) {
        String ipAddress = getClientIpAddress();
        log.warn("AUTH_FAILURE: user={} ip={} reason={}", username, ipAddress, reason);
    }

    public void logLogout(String username) {
        String ipAddress = getClientIpAddress();
        log.info("LOGOUT: user={} ip={}", username, ipAddress);
    }

    public void logPasswordChange(String username) {
        String ipAddress = getClientIpAddress();
        log.info("PASSWORD_CHANGE: user={} ip={}", username, ipAddress);
    }

    public void logPasswordResetRequest(String email) {
        String ipAddress = getClientIpAddress();
        log.info("PASSWORD_RESET_REQUEST: email={} ip={}", email, ipAddress);
    }

    public void logPasswordResetSuccess(String username) {
        String ipAddress = getClientIpAddress();
        log.info("PASSWORD_RESET_SUCCESS: user={} ip={}", username, ipAddress);
    }

    public void logEmailVerification(String username) {
        String ipAddress = getClientIpAddress();
        log.info("EMAIL_VERIFIED: user={} ip={}", username, ipAddress);
    }

    public void logAccountLockout(String username, int failedAttempts) {
        String ipAddress = getClientIpAddress();
        log.warn("ACCOUNT_LOCKED: user={} ip={} failedAttempts={}", username, ipAddress, failedAttempts);
    }

    public void logAccountUnlock(String username) {
        String ipAddress = getClientIpAddress();
        log.info("ACCOUNT_UNLOCKED: user={} ip={}", username, ipAddress);
    }

    public void logRegistration(String email) {
        String ipAddress = getClientIpAddress();
        log.info("USER_REGISTERED: email={} ip={}", email, ipAddress);
    }

    public void logSessionCreated(String username, String sessionId) {
        String ipAddress = getClientIpAddress();
        log.info("SESSION_CREATED: user={} sessionId={} ip={}", username, sessionId, ipAddress);
    }

    public void logSessionInvalidated(String username, String sessionId) {
        String ipAddress = getClientIpAddress();
        log.info("SESSION_INVALIDATED: user={} sessionId={} ip={}", username, sessionId, ipAddress);
    }

    public void logSuspiciousActivity(String activity, String username) {
        String ipAddress = getClientIpAddress();
        log.error("SUSPICIOUS_ACTIVITY: activity={} user={} ip={}", activity, username, ipAddress);
    }

    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Could not determine client IP address", e);
        }
        return "unknown";
    }

    public String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                    return ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
                }
                return principal.toString();
            }
        } catch (Exception e) {
            log.debug("Could not determine current username", e);
        }
        return "anonymous";
    }
}
