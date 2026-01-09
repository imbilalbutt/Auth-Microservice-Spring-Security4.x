package com.imbilalbutt.springauthdev.AuthService;

import com.imbilalbutt.springauthdev.Session.Redis.SessionRegistry;
import com.imbilalbutt.springauthdev.commons.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WebUserServiceImpl implements WebUserService{

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final SessionRegistry sessionRegistry;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        // Check if user already exists
        if (userExists(request.getEmail())) {
            throw new IllegalArgumentException("User with email " + request.getEmail() + " already exists");
        }

        // Create user account
        var user = createUserAccount(request);

        // Generate Session token
        var sessionId = sessionRegistry.registerSession(user.getEmail());

        return AuthenticationResponse.builder()
                .sessionToken(sessionId)
                .tokenType("Session")
                .email(user.getEmail())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .role(user.getRole())
                .build();
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
            // Authenticate credentials
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            // Get user
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Create session
            String sessionId = sessionRegistry.registerSession(user.getEmail());

        return AuthenticationResponse.builder()
                .sessionToken(sessionId)
                .tokenType("Session")
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
                .dateOfBirth(LocalDate.now())  // Add this if it's NOT NULL in DB
                .build();

        return userRepository.save(user);
    }

    @Override
    public boolean userExists(String email) {
        return userRepository.existsByEmail(email);
    }
}
