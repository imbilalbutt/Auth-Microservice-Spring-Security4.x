package com.imbilalbutt.springauthdev.AuthService;

import com.imbilalbutt.springauthdev.Config.JwtService;
import com.imbilalbutt.springauthdev.Config.SecurityAuditLogger;
import com.imbilalbutt.springauthdev.commons.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiUserServiceImpl implements ApiUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final SecurityAuditLogger auditLogger;

    @Value("${rate.limit.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${rate.limit.lockout-duration-minutes:30}")
    private int lockoutDurationMinutes;

    @Override
    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        log.info("Attempting to register user with email: {}", request.getEmail());
        
        if (userExists(request.getEmail())) {
            log.warn("Registration failed - user already exists: {}", request.getEmail());
            throw new IllegalArgumentException("User with email " + request.getEmail() + " already exists");
        }

        var user = createUserAccount(request);
        var jwtToken = jwtService.generateToken(user);

        auditLogger.logRegistration(user.getEmail());
        log.info("User registered successfully: {}", request.getEmail());

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .tokenType("Bearer")
                .email(user.getEmail())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .role(user.getRole())
                .build();
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        log.info("Authentication attempt for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    auditLogger.logAuthenticationFailure(request.getEmail(), "User not found");
                    return new UsernameNotFoundException("User not found");
                });

        if (!user.isEnabled()) {
            auditLogger.logAuthenticationFailure(request.getEmail(), "Account disabled");
            throw new BadCredentialsException("Account is disabled");
        }

        if (!user.isAccountNonLocked()) {
            auditLogger.logAuthenticationFailure(request.getEmail(), "Account locked");
            throw new LockedException("Account is locked");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            userRepository.resetFailedLoginAttempts(user.getId());
            auditLogger.logAuthenticationSuccess(user.getEmail());
            log.info("Authentication successful for: {}", request.getEmail());

        } catch (BadCredentialsException e) {
            user.incrementFailedLoginAttempts();
            
            if (user.getFailedLoginAttempts() >= maxLoginAttempts) {
                LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(lockoutDurationMinutes);
                user.setLocked(true);
                user.setAccountLockedUntil(lockedUntil);
                userRepository.save(user);
                auditLogger.logAccountLockout(user.getEmail(), user.getFailedLoginAttempts());
                log.warn("Account locked due to too many failed attempts: {}", request.getEmail());
                throw new LockedException("Account locked due to too many failed login attempts");
            }
            
            userRepository.save(user);
            auditLogger.logAuthenticationFailure(request.getEmail(), "Invalid credentials");
            log.warn("Authentication failed for: {} (attempt {} of {})", request.getEmail(), 
                    user.getFailedLoginAttempts(), maxLoginAttempts);
            throw new BadCredentialsException("Invalid email or password");
        }

        var jwtToken = jwtService.generateToken(user);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .tokenType("Bearer")
                .email(user.getEmail())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .role(user.getRole())
                .build();
    }

    @Override
    @Transactional
    public User createUserAccount(RegisterRequest request) {
        var user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : Role.USER)
                .enabled(true)
                .locked(false)
                .createdDate(LocalDateTime.now())
                .dateOfBirth(request.getDateOfBirth())
                .failedLoginAttempts(0)
                .emailVerified(false)
                .build();

        return userRepository.save(user);
    }

    @Override
    public boolean userExists(String email) {
        return userRepository.existsByEmail(email);
    }
}