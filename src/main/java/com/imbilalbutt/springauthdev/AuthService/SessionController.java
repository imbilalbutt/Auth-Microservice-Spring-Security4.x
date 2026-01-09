package com.imbilalbutt.springauthdev.AuthService;


import com.imbilalbutt.springauthdev.Session.Redis.SessionRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/session")
@RequiredArgsConstructor
public class SessionController {

    private final SessionRegistry sessionRegistry;
    final WebUserService webUserService;

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> loginWithSession(
            @Valid @RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(webUserService.authenticate(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String sessionId) {
        sessionRegistry.invalidateSession(sessionId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshSession(@RequestHeader("Authorization") String sessionId) {
        sessionRegistry.refreshSession(sessionId);
        return ResponseEntity.ok().build();
    }
}