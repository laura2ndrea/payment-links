package com.laura.payment_links.service;

import com.laura.payment_links.model.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {

    // Idempotencia para evitar procesar el mismo pago dos veces
    Optional<PaymentAttempt> findByPaymentLinkIdAndIdempotencyKey(
            UUID paymentLinkId,
            String idempotencyKey
    );

    // Historial de intentos por link
    List<PaymentAttempt> findByPaymentLinkIdOrderByCreatedAtDesc(UUID paymentLinkId);
}