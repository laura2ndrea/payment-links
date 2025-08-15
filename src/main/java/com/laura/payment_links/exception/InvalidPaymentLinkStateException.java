package com.laura.payment_links.exception;

// Clase personalizada para códigos 409 CONFLICT
public class InvalidPaymentLinkStateException extends RuntimeException {
    public InvalidPaymentLinkStateException(String message) {
        super(message);
    }
}