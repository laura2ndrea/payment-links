package com.laura.payment_links.exception;

// Manejo de excepción para intento de pago duplicado
public class DuplicatePaymentAttemptException extends RuntimeException {
    public DuplicatePaymentAttemptException(String idempotencyKey) {
        super(String.format("Intento de pago duplicado. Key: %s", idempotencyKey));
    }
}
