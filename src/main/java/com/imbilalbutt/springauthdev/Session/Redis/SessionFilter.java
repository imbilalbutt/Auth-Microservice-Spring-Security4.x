//package com.imbilalbutt.springauthdev.Session;
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpHeaders;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//
//@Component
//@RequiredArgsConstructor
//public class SessionFilter extends OncePerRequestFilter {
//
//    private final SessionRegistry sessionRegistry;
//    private final UserDetailsService userDetailsService;
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request,
//                                    HttpServletResponse response,
//                                    FilterChain chain) throws ServletException, IOException {
//
//        final String sessionId = request.getHeader(HttpHeaders.AUTHORIZATION);
//
//        if (sessionId == null || sessionId.isEmpty()) {
//            chain.doFilter(request, response);
//            return;
//        }
//
//        final String username = sessionRegistry.getUsernameForSession(sessionId);
//
//        if (username == null) {
//            chain.doFilter(request, response);
//            return;
//        }
//
//        final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
//
//        final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
//                userDetails,
//                null,
//                userDetails.getAuthorities()
//        );
//
//        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//        SecurityContextHolder.getContext().setAuthentication(auth);
//
//        chain.doFilter(request, response);
//    }
//}