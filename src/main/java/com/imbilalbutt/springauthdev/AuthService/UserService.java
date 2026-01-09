package com.imbilalbutt.springauthdev.AuthService;


public interface UserService {

    AuthenticationResponse register(RegisterRequest request);

    AuthenticationResponse authenticate(AuthenticationRequest request);

    User createUserAccount(RegisterRequest request);

    boolean userExists(String email);
}