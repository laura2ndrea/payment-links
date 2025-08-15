package com.laura.payment_links.exception;

import java.util.UUID;

// Clase personalizada para el código 404 NOT_FOUND
public class PaymentLinkNotFoundException extends RuntimeException {
    public PaymentLinkNotFoundException(UUID id) {
        super("PaymentLink con ID " + id + " no encontrado");
    }

    public PaymentLinkNotFoundException(String reference) {
        super("PaymentLink con referencia " + reference + " no encontrado");
    }
}
