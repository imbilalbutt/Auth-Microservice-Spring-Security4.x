package com.imbilalbutt.springauthdev.Config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("UsernameNotFoundException Tests")
    class UsernameNotFoundExceptionTest {

        @Test
        @DisplayName("Should return 401 with INVALID_CREDENTIALS code")
        void shouldReturn401WithInvalidCredentialsCode() {
            UsernameNotFoundException exception = new UsernameNotFoundException("User not found");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleUsernameNotFound(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo("INVALID_CREDENTIALS");
        }

        @Test
        @DisplayName("Should include exception message")
        void shouldIncludeExceptionMessage() {
            UsernameNotFoundException exception = new UsernameNotFoundException("User john@example.com not found");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleUsernameNotFound(exception);

            assertThat(response.getBody().getMessage()).contains("john@example.com");
        }
    }

    @Nested
    @DisplayName("BadCredentialsException Tests")
    class BadCredentialsExceptionTest {

        @Test
        @DisplayName("Should return 401 with INVALID_CREDENTIALS code")
        void shouldReturn401WithInvalidCredentialsCode() {
            BadCredentialsException exception = new BadCredentialsException("Invalid credentials");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleBadCredentials(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo("INVALID_CREDENTIALS");
        }

        @Test
        @DisplayName("Should return generic message")
        void shouldReturnGenericMessage() {
            BadCredentialsException exception = new BadCredentialsException("Invalid credentials");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleBadCredentials(exception);

            assertThat(response.getBody().getMessage()).isEqualTo("Invalid email or password");
        }
    }

    @Nested
    @DisplayName("DisabledException Tests")
    class DisabledExceptionTest {

        @Test
        @DisplayName("Should return 403 with ACCOUNT_DISABLED code")
        void shouldReturn403WithAccountDisabledCode() {
            DisabledException exception = new DisabledException("Account is disabled");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDisabledAccount(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo("ACCOUNT_DISABLED");
        }
    }

    @Nested
    @DisplayName("LockedException Tests")
    class LockedExceptionTest {

        @Test
        @DisplayName("Should return 423 with ACCOUNT_LOCKED code")
        void shouldReturn423WithAccountLockedCode() {
            LockedException exception = new LockedException("Account is locked");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleLockedAccount(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo("ACCOUNT_LOCKED");
        }
    }

    @Nested
    @DisplayName("IllegalArgumentException Tests")
    class IllegalArgumentExceptionTest {

        @Test
        @DisplayName("Should return 400 with VALIDATION_ERROR code")
        void shouldReturn400WithValidationErrorCode() {
            IllegalArgumentException exception = new IllegalArgumentException("Invalid input");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleIllegalArgument(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
        }

        @Test
        @DisplayName("Should include custom message")
        void shouldIncludeCustomMessage() {
            IllegalArgumentException exception = new IllegalArgumentException("Email already exists");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleIllegalArgument(exception);

            assertThat(response.getBody().getMessage()).isEqualTo("Email already exists");
        }
    }

    @Nested
    @DisplayName("Generic Exception Tests")
    class GenericExceptionTest {

        @Test
        @DisplayName("Should return 500 with INTERNAL_ERROR code")
        void shouldReturn500WithInternalErrorCode() {
            Exception exception = new RuntimeException("Something went wrong");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleGenericException(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_ERROR");
        }

        @Test
        @DisplayName("Should return generic message for security")
        void shouldReturnGenericMessageForSecurity() {
            Exception exception = new RuntimeException("Sensitive information");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleGenericException(exception);

            assertThat(response.getBody().getMessage()).doesNotContain("Sensitive");
            assertThat(response.getBody().getMessage()).contains("unexpected error");
        }
    }

    @Nested
    @DisplayName("Error Response Tests")
    class ErrorResponseTests {

        @Test
        @DisplayName("Should create error response with code and message")
        void shouldCreateErrorResponseWithCodeAndMessage() {
            GlobalExceptionHandler.ErrorResponse response = new GlobalExceptionHandler.ErrorResponse("CODE", "Message");

            assertThat(response.getCode()).isEqualTo("CODE");
            assertThat(response.getMessage()).isEqualTo("Message");
            assertThat(response.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("Should create error response with all fields constructor")
        void shouldCreateErrorResponseWithAllFieldsConstructor() {
            GlobalExceptionHandler.ErrorResponse response = new GlobalExceptionHandler.ErrorResponse("CODE", "Message");

            assertThat(response).isNotNull();
        }
    }

    @Nested
    @DisplayName("Validation Error Response Tests")
    class ValidationErrorResponseTests {

        @Test
        @DisplayName("Should create validation error response")
        void shouldCreateValidationErrorResponse() {
            List<GlobalExceptionHandler.FieldErrorDTO> errors = List.of(
                    new GlobalExceptionHandler.FieldErrorDTO("email", "Invalid email"),
                    new GlobalExceptionHandler.FieldErrorDTO("password", "Weak password")
            );

            GlobalExceptionHandler.ValidationErrorResponse response = 
                    new GlobalExceptionHandler.ValidationErrorResponse("VALIDATION_FAILED", errors);

            assertThat(response.getCode()).isEqualTo("VALIDATION_FAILED");
            assertThat(response.getErrors()).hasSize(2);
            assertThat(response.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("Should create field error DTO")
        void shouldCreateFieldErrorDTO() {
            GlobalExceptionHandler.FieldErrorDTO error = 
                    new GlobalExceptionHandler.FieldErrorDTO("email", "Invalid email");

            assertThat(error.getField()).isEqualTo("email");
            assertThat(error.getMessage()).isEqualTo("Invalid email");
        }
    }
}