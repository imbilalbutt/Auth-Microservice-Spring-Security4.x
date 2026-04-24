package com.imbilalbutt.springauthdev.AuthService;

import com.imbilalbutt.springauthdev.commons.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

@DisplayName("UserConverter Unit Tests")
class UserConverterTest {

    private UserConverter converter;

    @BeforeEach
    void setUp() {
        converter = new UserConverter();
    }

    @Nested
    @DisplayName("toDto Tests")
    class ToDtoTests {

        @Test
        @DisplayName("Should convert user to DTO with all fields")
        void shouldConvertUserToDtoWithAllFields() {
            LocalDateTime now = LocalDateTime.now();
            User user = User.builder()
                    .id(1)
                    .firstname("John")
                    .lastname("Doe")
                    .email("john@example.com")
                    .password("password")
                    .role(Role.USER)
                    .enabled(true)
                    .locked(false)
                    .dateOfBirth(LocalDate.of(1990, 5, 15))
                    .createdDate(now)
                    .failedLoginAttempts(0)
                    .emailVerified(true)
                    .build();

            UserDTO dto = converter.toDto(user);

            assertThat(dto).isNotNull();
            assertThat(dto.getId()).isEqualTo(1);
            assertThat(dto.getFirstname()).isEqualTo("John");
            assertThat(dto.getLastname()).isEqualTo("Doe");
            assertThat(dto.getEmail()).isEqualTo("john@example.com");
            assertThat(dto.getRole()).isEqualTo(Role.USER);
            assertThat(dto.isEnabled()).isTrue();
            assertThat(dto.isLocked()).isFalse();
            assertThat(dto.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
            assertThat(dto.getCreatedDate()).isEqualTo(now);
            assertThat(dto.getFailedLoginAttempts()).isEqualTo(0);
            assertThat(dto.isEmailVerified()).isTrue();
        }

        @Test
        @DisplayName("Should convert ADMIN user to DTO")
        void shouldConvertAdminUserToDto() {
            User user = User.builder()
                    .id(2)
                    .firstname("Admin")
                    .lastname("User")
                    .email("admin@example.com")
                    .password("password")
                    .role(Role.ADMIN)
                    .enabled(true)
                    .locked(false)
                    .build();

            UserDTO dto = converter.toDto(user);

            assertThat(dto.getRole()).isEqualTo(Role.ADMIN);
        }
    }

    @Nested
    @DisplayName("toEntity Tests")
    class ToEntityTests {

        @Test
        @DisplayName("Should convert DTO to user with all fields")
        void shouldConvertDtoToUserWithAllFields() {
            LocalDateTime now = LocalDateTime.now();
            UserDTO dto = UserDTO.builder()
                    .id(1)
                    .firstname("John")
                    .lastname("Doe")
                    .email("john@example.com")
                    .role(Role.USER)
                    .enabled(true)
                    .locked(false)
                    .dateOfBirth(LocalDate.of(1990, 5, 15))
                    .createdDate(now)
                    .failedLoginAttempts(0)
                    .emailVerified(true)
                    .build();

            User user = converter.toEntity(dto);

            assertThat(user).isNotNull();
            assertThat(user.getId()).isEqualTo(1);
            assertThat(user.getFirstname()).isEqualTo("John");
            assertThat(user.getLastname()).isEqualTo("Doe");
            assertThat(user.getEmail()).isEqualTo("john@example.com");
            assertThat(user.getRole()).isEqualTo(Role.USER);
            assertThat(user.isEnabled()).isTrue();
            assertThat(user.isLocked()).isFalse();
            assertThat(user.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
            assertThat(user.getCreatedDate()).isEqualTo(now);
            assertThat(user.getFailedLoginAttempts()).isEqualTo(0);
            assertThat(user.isEmailVerified()).isTrue();
        }

        @Test
        @DisplayName("Should convert ADMIN DTO to user")
        void shouldConvertAdminDtoToUser() {
            UserDTO dto = UserDTO.builder()
                    .id(2)
                    .firstname("Admin")
                    .lastname("User")
                    .email("admin@example.com")
                    .role(Role.ADMIN)
                    .enabled(true)
                    .locked(false)
                    .build();

            User user = converter.toEntity(dto);

            assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        }
    }

    @Nested
    @DisplayName("UserDTO Tests")
    class UserDTOTests {

        @Test
        @DisplayName("Should get full name")
        void shouldGetFullName() {
            UserDTO dto = UserDTO.builder()
                    .firstname("John")
                    .lastname("Doe")
                    .build();

            assertThat(dto.getFullName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("Should handle null fields in full name")
        void shouldHandleNullFieldsInFullName() {
            UserDTO dto = UserDTO.builder()
                    .firstname("John")
                    .build();

            assertThat(dto.getFullName()).contains("John");
        }
    }
}