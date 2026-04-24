package com.imbilalbutt.springauthdev.Config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Set test values using reflection
        try {
            var secretKeyField = JwtService.class.getDeclaredField("secretKey");
            secretKeyField.setAccessible(true);
            secretKeyField.set(jwtService, "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");

            var jwtExpirationField = JwtService.class.getDeclaredField("jwtExpiration");
            jwtExpirationField.setAccessible(true);
            jwtExpirationField.set(jwtService, 86400000L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("Generate Token Tests")
    class GenerateTokenTests {

        @Test
        @DisplayName("Should generate token with default claims")
        void shouldGenerateTokenWithDefaultClaims() {
            UserDetails userDetails = createTestUserDetails("john@example.com");

            String token = jwtService.generateToken(userDetails);

            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
        }

        @Test
        @DisplayName("Should generate token with extra claims")
        void shouldGenerateTokenWithExtraClaims() {
            UserDetails userDetails = createTestUserDetails("john@example.com");
            Map<String, Object> extraClaims = new HashMap<>();
            extraClaims.put("customClaim", "testValue");
            extraClaims.put("userId", 123);

            String token = jwtService.generateToken(extraClaims, userDetails);

            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
        }

        @Test
        @DisplayName("Should generate different tokens for same user")
        void shouldGenerateDifferentTokensForSameUser() {
            UserDetails userDetails = createTestUserDetails("john@example.com");

            String token1 = jwtService.generateToken(userDetails);
            String token2 = jwtService.generateToken(userDetails);

            // Tokens should be different due to different timestamps
            assertThat(token1).isNotEqualTo(token2);
        }
    }

    @Nested
    @DisplayName("Extract Claims Tests")
    class ExtractClaimsTests {

        @Test
        @DisplayName("Should extract username from token")
        void shouldExtractUsernameFromToken() {
            UserDetails userDetails = createTestUserDetails("john@example.com");
            String token = jwtService.generateToken(userDetails);

            String username = jwtService.extractUsername(token);

            assertThat(username).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("Should extract expiration date from token")
        void shouldExtractExpirationDateFromToken() {
            UserDetails userDetails = createTestUserDetails("john@example.com");
            String token = jwtService.generateToken(userDetails);

            Date expiration = jwtService.extractClaim(token, claims -> claims.getExpiration());

            assertThat(expiration).isAfter(new Date());
        }
    }

    @Nested
    @DisplayName("Validate Token Tests")
    class ValidateTokenTests {

        @Test
        @DisplayName("Should return true for valid token")
        void shouldReturnTrueForValidToken() {
            UserDetails userDetails = createTestUserDetails("john@example.com");
            String token = jwtService.generateToken(userDetails);

            boolean isValid = jwtService.isTokenValid(token, userDetails);

            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should return false for token with wrong user")
        void shouldReturnFalseForTokenWithWrongUser() {
            UserDetails userDetails = createTestUserDetails("john@example.com");
            String token = jwtService.generateToken(userDetails);
            UserDetails wrongUser = createTestUserDetails("jane@example.com");

            boolean isValid = jwtService.isTokenValid(token, wrongUser);

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should return false for invalid token format")
        void shouldReturnFalseForInvalidTokenFormat() {
            UserDetails userDetails = createTestUserDetails("john@example.com");
            String invalidToken = "invalid.token.here";

            boolean isValid = jwtService.isTokenValid(invalidToken, userDetails);

            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle token with extra claims")
        void shouldHandleTokenWithExtraClaims() {
            UserDetails userDetails = createTestUserDetails("john@example.com");
            Map<String, Object> extraClaims = new HashMap<>();
            extraClaims.put("scope", "user:read user:write");

            String token = jwtService.generateToken(extraClaims, userDetails);
            String username = jwtService.extractUsername(token);

            assertThat(username).isEqualTo("john@example.com");
        }
    }

    private UserDetails createTestUserDetails(String username) {
        return User.builder()
                .username(username)
                .password("password")
                .authorities(new org.springframework.security.core.GrantedAuthority() {
                    @Override
                    public String getAuthority() {
                        return "USER";
                    }
                })
                .build();
    }
}