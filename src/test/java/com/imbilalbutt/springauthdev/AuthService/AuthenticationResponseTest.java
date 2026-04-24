package com.imbilalbutt.springauthdev.AuthService;

import com.imbilalbutt.springauthdev.commons.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthenticationResponse Tests")
class AuthenticationResponseTest {

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should build with JWT token")
        void shouldBuildWithJwtToken() {
            AuthenticationResponse response = AuthenticationResponse.builder()
                    .token("jwt-token")
                    .tokenType("Bearer")
                    .email("john@example.com")
                    .firstname("John")
                    .lastname("Doe")
                    .role(Role.USER)
                    .build();

            assertThat(response.getToken()).isEqualTo("jwt-token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getEmail()).isEqualTo("john@example.com");
            assertThat(response.getFirstname()).isEqualTo("John");
            assertThat(response.getLastname()).isEqualTo("Doe");
            assertThat(response.getRole()).isEqualTo(Role.USER);
        }

        @Test
        @DisplayName("Should build with session token")
        void shouldBuildWithSessionToken() {
            AuthenticationResponse response = AuthenticationResponse.builder()
                    .sessionToken("session-token")
                    .tokenType("Session")
                    .email("john@example.com")
                    .firstname("John")
                    .lastname("Doe")
                    .role(Role.USER)
                    .build();

            assertThat(response.getSessionToken()).isEqualTo("session-token");
            assertThat(response.getTokenType()).isEqualTo("Session");
        }

        @Test
        @DisplayName("Should build with all null values")
        void shouldBuildWithAllNullValues() {
            AuthenticationResponse response = AuthenticationResponse.builder().build();

            assertThat(response.getToken()).isNull();
            assertThat(response.getSessionToken()).isNull();
            assertThat(response.getTokenType()).isNull();
            assertThat(response.getEmail()).isNull();
            assertThat(response.getFirstname()).isNull();
            assertThat(response.getLastname()).isNull();
            assertThat(response.getRole()).isNull();
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create with allargs constructor")
        void shouldCreateWithAllArgsConstructor() {
            AuthenticationResponse response = new AuthenticationResponse(
                    "token",
                    "sessionToken",
                    "Bearer",
                    "john@example.com",
                    "John",
                    "Doe",
                    Role.USER
            );

            assertThat(response.getToken()).isEqualTo("token");
            assertThat(response.getSessionToken()).isEqualTo("sessionToken");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getEmail()).isEqualTo("john@example.com");
            assertThat(response.getFirstname()).isEqualTo("John");
            assertThat(response.getLastname()).isEqualTo("Doe");
            assertThat(response.getRole()).isEqualTo(Role.USER);
        }

        @Test
        @DisplayName("Should create with noargs constructor")
        void shouldCreateWithNoArgsConstructor() {
            AuthenticationResponse response = new AuthenticationResponse();

            assertThat(response).isNotNull();
        }
    }

    @Nested
    @DisplayName("Setters Tests")
    class SettersTests {

        @Test
        @DisplayName("Should set token")
        void shouldSetToken() {
            AuthenticationResponse response = new AuthenticationResponse();
            response.setToken("new-token");

            assertThat(response.getToken()).isEqualTo("new-token");
        }

        @Test
        @DisplayName("Should set session token")
        void shouldSetSessionToken() {
            AuthenticationResponse response = new AuthenticationResponse();
            response.setSessionToken("session-id");

            assertThat(response.getSessionToken()).isEqualTo("session-id");
        }

        @Test
        @DisplayName("Should set role")
        void shouldSetRole() {
            AuthenticationResponse response = new AuthenticationResponse();
            response.setRole(Role.ADMIN);

            assertThat(response.getRole()).isEqualTo(Role.ADMIN);
        }
    }
}