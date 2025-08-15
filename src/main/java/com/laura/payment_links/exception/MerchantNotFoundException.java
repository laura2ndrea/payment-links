package com.laura.payment_links.exception;

import java.util.UUID;

// Clase personalizada para cuando no se consigue un Merchant
public class MerchantNotFoundException extends RuntimeException {
    public MerchantNotFoundException(UUID merchantId) {
        super("Comercio con ID " + merchantId + " no encontrado");
    }

    public MerchantNotFoundException(String email) {
        super("Comercio con email " + email + " no encontrado");
    }
}
