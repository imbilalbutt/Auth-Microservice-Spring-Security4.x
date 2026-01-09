package com.imbilalbutt.springauthdev.AuthService;


public interface ApiUserService {

    AuthenticationResponse register(RegisterRequest request);

    AuthenticationResponse authenticate(AuthenticationRequest request);

    User createUserAccount(RegisterRequest request);

    boolean userExists(String email);
}