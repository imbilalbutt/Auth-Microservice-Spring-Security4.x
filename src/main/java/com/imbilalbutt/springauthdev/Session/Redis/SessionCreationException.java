package com.imbilalbutt.springauthdev.Session.Redis;

public class SessionCreationException extends RuntimeException {

    public SessionCreationException(String message) {
        super(message);
    }

    public SessionCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
