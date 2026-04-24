package com.imbilalbutt.springauthdev.AuthService;

import org.springframework.stereotype.Component;

@Component
public class UserConverter {

    public UserDTO toDto(User user) {
        if (user == null) {
            return null;
        }

        return UserDTO.builder()
                .id(user.getId())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .locked(user.isLocked())
                .dateOfBirth(user.getDateOfBirth())
                .createdDate(user.getCreatedDate())
                .lastModifiedDate(user.getLastModifiedDate())
                .build();
    }

    public User toEntity(UserDTO userDTO) {
        if (userDTO == null) {
            return null;
        }

        return User.builder()
                .id(userDTO.getId())
                .firstname(userDTO.getFirstname())
                .lastname(userDTO.getLastname())
                .email(userDTO.getEmail())
                .role(userDTO.getRole())
                .enabled(userDTO.isEnabled())
                .locked(userDTO.isLocked())
                .dateOfBirth(userDTO.getDateOfBirth())
                .createdDate(userDTO.getCreatedDate())
                .lastModifiedDate(userDTO.getLastModifiedDate())
                .failedLoginAttempts(userDTO.getFailedLoginAttempts())
                .accountLockedUntil(userDTO.getAccountLockedUntil())
                .emailVerified(userDTO.isEmailVerified())
                .build();
    }
}
