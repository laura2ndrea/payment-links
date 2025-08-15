package com.laura.payment_links.exception;

// Clase personalizada para el 401 código UNAUTHORIZED
public class AuthException extends RuntimeException {
    public AuthException(String message) {
        super(message);
    }
}