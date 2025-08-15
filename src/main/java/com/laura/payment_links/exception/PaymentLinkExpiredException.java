package com.laura.payment_links.exception;

import java.time.Instant;

// Excepción para cuando el link expira
public class PaymentLinkExpiredException extends RuntimeException {
    private final Instant expiresAt;

    public PaymentLinkExpiredException(Instant expiresAt) {
        super(String.format("El link de pago expiró el %s", expiresAt));
        this.expiresAt = expiresAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}