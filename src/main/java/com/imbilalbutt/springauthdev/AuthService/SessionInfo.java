package com.imbilalbutt.springauthdev.AuthService;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionInfo {
    private String username;
    private String displayName;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;
}