package com.imbilalbutt.springauthdev.Config;


import com.imbilalbutt.springauthdev.AuthService.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final UserRepository userRepository;

//    // 1. Tells Spring Security WHERE to load users from
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }


    // 3. Tells Spring Security the AUTHENTICATION STRATEGY (username/password)
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService());  // Where to get users
        try {
            authProvider.setPasswordEncoder(passwordEncoder());  // How to check password
        } catch (NoSuchMethodError e) {
            // For older versions, create with constructor
            authProvider = new DaoAuthenticationProvider(userDetailsService());
        }
        return authProvider;
    }

    // 4. Creates the AuthenticationManager responsible for authentication
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }


    // 2. Tells Spring Security HOW to encode/verify passwords
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
