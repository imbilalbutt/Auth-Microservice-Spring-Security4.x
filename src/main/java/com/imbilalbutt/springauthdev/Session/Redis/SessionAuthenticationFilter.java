package com.imbilalbutt.springauthdev.Session.Redis;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private final SessionRegistry sessionRegistry;
    private final UserDetailsService userDetailsService;

    private static final String SESSION_COOKIE_NAME = "SESSION_ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // Only process web UI requests (not API requests)
        if (request.getRequestURI().startsWith("/api/") || request.getRequestURI().contains("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        // Try to get session from cookie
        String sessionId = extractSessionIdFromCookie(request);

        if (sessionId == null || sessionId.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        // Validate session
        String username = sessionRegistry.getUsernameForSession(sessionId);

        if (username == null) {
            // Invalid session - clear cookie
            clearSessionCookie(response);
            chain.doFilter(request, response);
            return;
        }

        // Session valid - authenticate user
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Refresh session expiration
        sessionRegistry.refreshSession(sessionId);

        chain.doFilter(request, response);
    }

    private String extractSessionIdFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void clearSessionCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(SESSION_COOKIE_NAME, null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0); // Delete cookie
        response.addCookie(cookie);
    }
}
