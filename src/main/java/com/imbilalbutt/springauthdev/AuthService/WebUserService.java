package com.imbilalbutt.springauthdev.AuthService;

public interface WebUserService {

    AuthenticationResponse register(RegisterRequest request);

    AuthenticationResponse authenticate(AuthenticationRequest request);

    User createUserAccount(RegisterRequest request);

    boolean userExists(String email);

}
