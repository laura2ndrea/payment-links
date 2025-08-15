package com.laura.payment_links.service;

import com.laura.payment_links.dto.request.CreatePaymentLinkRequest;
import com.laura.payment_links.dto.response.PaymentLinkResponse;
import com.laura.payment_links.model.Merchant;
import com.laura.payment_links.model.PaymentLink;
import com.laura.payment_links.model.PaymentLinkStatus;
import com.laura.payment_links.repository.MerchantRepository;
import com.laura.payment_links.repository.PaymentAttemptRepository;
import com.laura.payment_links.repository.PaymentLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class PaymentLinkService {
    private final PaymentLinkRepository paymentLinkRepository;
    private final MerchantRepository merchantRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final ReferenceGenerator referenceGenerator;

    // === Métodos Principales === //

    /**
     * Crea un nuevo link de pago para un comercio.
     */
    public PaymentLinkResponse createPaymentLink(UUID merchantId, CreatePaymentLinkRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(merchantId));

        PaymentLink paymentLink = PaymentLink.builder()
                .merchant(merchant)
                .amountCents(request.getAmountCents())
                .currency(request.getCurrency().toUpperCase()) // Aseguramos formato ISO
                .description(request.getDescription())
                .expiresAt(Instant.now().plus(request.getExpiresInMinutes(), ChronoUnit.MINUTES))
                .metadata(convertMetadataToJson(request.getMetadata()))
                .reference(referenceGenerator.generateReference())
                .status(PaymentLinkStatus.CREATED)
                .build();

        paymentLink = paymentLinkRepository.save(paymentLink);
        return PaymentLinkMapper.toResponse(paymentLink);
    }

    /**
     * Obtiene links de pago con filtros y paginación.
     */
    public Page<PaymentLinkResponse> getPaymentLinks(UUID merchantId, PaymentLinkFilter filter, Pageable pageable) {
        return paymentLinkRepository.search(
                merchantId,
                filter.getStatus(),
                filter.getFromDate(),
                filter.getToDate(),
                filter.getMinAmount(),
                filter.getMaxAmount(),
                pageable
        ).map(PaymentLinkMapper::toResponse);
    }

    /**
     * Obtiene los detalles completos de un link de pago.
     */
    public PaymentLinkDetailsResponse getPaymentLinkDetails(UUID merchantId, String identifier) {
        PaymentLink paymentLink = findByIdOrReference(merchantId, identifier);
        return PaymentLinkMapper.toDetailsResponse(paymentLink);
    }

    /**
     * Procesa un pago sobre un link existente.
     */
    public PaymentAttemptResponse payPaymentLink(UUID merchantId, UUID paymentLinkId,
                                                 PayPaymentLinkRequest request, String idempotencyKey) {
        PaymentLink paymentLink = paymentLinkRepository.findByIdAndMerchantId(paymentLinkId, merchantId)
                .orElseThrow(() -> new PaymentLinkNotFoundException(paymentLinkId));

        // Validación de estado
        if (!paymentLink.getStatus().equals(PaymentLinkStatus.CREATED)) {
            throw new InvalidPaymentLinkStateException("El link no está en estado CREATED");
        }

        // Validación de expiración
        if (paymentLink.getExpiresAt().isBefore(Instant.now())) {
            throw new PaymentLinkExpiredException(paymentLink.getExpiresAt());
        }

        // Idempotencia: Evitar pagos duplicados
        paymentAttemptRepository.findByPaymentLinkIdAndIdempotencyKey(paymentLinkId, idempotencyKey)
                .ifPresent(attempt -> {
                    throw new DuplicatePaymentAttemptException();
                });

        // Procesar pago (simulado)
        PaymentAttemptStatus status = request.getPaymentToken().startsWith("ok_")
                ? PaymentAttemptStatus.SUCCESS
                : PaymentAttemptStatus.FAILED;

        PaymentAttempt attempt = PaymentAttempt.builder()
                .paymentLink(paymentLink)
                .status(status)
                .reason(status.equals(PaymentAttemptStatus.FAILED) ? "Pago rechazado" : null)
                .idempotencyKey(idempotencyKey)
                .build();

        // Actualizar estado del link si es exitoso
        if (status.equals(PaymentAttemptStatus.SUCCESS)) {
            paymentLink.setStatus(PaymentLinkStatus.PAID);
            paymentLink.setPaidAt(Instant.now());
            paymentLinkRepository.save(paymentLink);
        }

        paymentAttemptRepository.save(attempt);
        return PaymentLinkMapper.toAttemptResponse(attempt);
    }

    /**
     * Cancela un link de pago.
     */
    public PaymentLinkResponse cancelPaymentLink(UUID merchantId, UUID paymentLinkId) {
        PaymentLink paymentLink = paymentLinkRepository.findByIdAndMerchantId(paymentLinkId, merchantId)
                .orElseThrow(() -> new PaymentLinkNotFoundException(paymentLinkId));

        if (!paymentLink.getStatus().equals(PaymentLinkStatus.CREATED)) {
            throw new InvalidPaymentLinkStateException("Solo se pueden cancelar links en estado CREATED");
        }

        paymentLink.setStatus(PaymentLinkStatus.CANCELLED);
        paymentLinkRepository.save(paymentLink);
        return PaymentLinkMapper.toResponse(paymentLink);
    }

    /**
     * Job que expira links vencidos.
     */
    public int expirePaymentLinks() {
        int expiredCount = paymentLinkRepository.expireLinks(Instant.now());
        log.info("Expiraron {} links de pago", expiredCount);
        return expiredCount;
    }

    // === Métodos Auxiliares === //

    private PaymentLink findByIdOrReference(UUID merchantId, String identifier) {
        try {
            UUID id = UUID.fromString(identifier);
            return paymentLinkRepository.findByIdAndMerchantId(id, merchantId)
                    .orElseThrow(() -> new PaymentLinkNotFoundException(id));
        } catch (IllegalArgumentException e) {
            return paymentLinkRepository.findByReferenceAndMerchantId(identifier, merchantId)
                    .orElseThrow(() -> new PaymentLinkNotFoundException(identifier));
        }
    }

    private String convertMetadataToJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return new ObjectMapper().writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new InvalidMetadataException("Formato de metadata inválido");
        }
    }
}
