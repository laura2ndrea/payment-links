package com.laura.payment_links.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super(String.format("El email %s ya está registrado", email));
    }
}