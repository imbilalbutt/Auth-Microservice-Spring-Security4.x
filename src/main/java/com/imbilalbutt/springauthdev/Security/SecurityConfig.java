package com.imbilalbutt.springauthdev.Security;
//
//import com.imbilalbutt.springauthdev.Config.JwtAuthenticationFilter;
//import com.imbilalbutt.springauthdev.Session.SessionAuthenticationFilter;
////import com.imbilalbutt.springauthdev.Session.SessionFilter;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.authentication.AuthenticationProvider;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//import org.springframework.security.web.context.SecurityContextPersistenceFilter;
//
//@Configuration
//@EnableWebSecurity
//@RequiredArgsConstructor
//public class SecurityConfig{
//
//    private final JwtAuthenticationFilter jwtAuthFilter;
//    private final AuthenticationProvider authenticationProvider;
//    private final SessionAuthenticationFilter sessionFilter;
////    private final SessionFilter sessionFilter;
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(csrf -> csrf.disable())
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/api/v1/auth/**").permitAll()
//                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
//                        // Allow public access to UI pages
//                        .requestMatchers("/ui/**").permitAll()
//                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
//                        .anyRequest().authenticated()
//                )
//                /**
//                 * For REDIS only
//                 */
////                .sessionManagement(sess -> sess
////                        .sessionCreationPolicy(SessionCreationPolicy.ALWAYS) // Or SessionCreationPolicy.IF_REQUIRED
////                        .maximumSessions(1)
////                        .maxSessionsPreventsLogin(false)
////                )
////                        .authenticationProvider(authenticationProvider)
////                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
////                .addFilterBefore(sessionFilter, JwtAuthenticationFilter.class);;
//
//                /**
//                 * For JWT only
//                 */
////                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
////                .authenticationProvider(authenticationProvider)
////                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
//
//                .sessionManagement(sess -> sess
//                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // For APIs
//                        .sessionFixation().none()
//                )
//                .authenticationProvider(authenticationProvider)
//                // Order matters: Session filter for web, JWT filter for API
//                .addFilterBefore(sessionFilter, SecurityContextPersistenceFilter.class)
//                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
//
//
//        return http.build();
//    }
//}



import com.imbilalbutt.springauthdev.Config.JwtAuthenticationFilter;
import com.imbilalbutt.springauthdev.Session.Redis.SessionAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final SessionAuthenticationFilter sessionFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // PUBLIC ENDPOINTS
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()

                        // API ENDPOINTS (JWT protected)
                        .requestMatchers("/api/v1/**").authenticated()

                        // ADMIN ENDPOINTS
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .sessionManagement(sess -> sess
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // For APIs
                        .sessionFixation().none()
                )
                .authenticationProvider(authenticationProvider)
                // Order matters: Session filter for web, JWT filter for API
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();

    }

    // Separate bean for API security (optional, for more granular control)
    @Bean
    @Order(2)
    public SecurityFilterChain uiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/ui/**", "/", "/home", "/login", "/register")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/ui/auth/**").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/ui/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/ui/auth/redis/login")
                        .loginProcessingUrl("/ui/auth/redis/login")
                        .defaultSuccessUrl("/ui/auth/redis/dashboard")
                        .failureUrl("/ui/auth/redis/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/ui/auth/redis/logout")
                        .logoutSuccessUrl("/ui/auth/redis/login?logout=true")
                        .deleteCookies("SESSION_ID", "JSESSIONID")
                        .invalidateHttpSession(true)
                        .permitAll()
                )
                .sessionManagement(sess -> sess
                        .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                        .maximumSessions(1)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(sessionFilter, UsernamePasswordAuthenticationFilter.class); // SecurityContextPersistenceFilter.class


        return http.build();
    }
}