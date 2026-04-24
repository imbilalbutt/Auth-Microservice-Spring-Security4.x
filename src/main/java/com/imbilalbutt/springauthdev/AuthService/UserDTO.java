package com.imbilalbutt.springauthdev.AuthService;

import com.imbilalbutt.springauthdev.commons.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {

    private Integer id;
    private String firstname;
    private String lastname;
    private String email;
    private Role role;
    private boolean enabled;
    private boolean locked;
    private LocalDate dateOfBirth;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
    private Integer failedLoginAttempts;
    private LocalDateTime accountLockedUntil;
    private boolean emailVerified;

    public String getFullName() {
        return firstname + " " + lastname;
    }
}
