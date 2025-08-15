package com.laura.payment_links.service;

import com.laura.payment_links.dto.request.CreatePaymentLinkRequest;
import com.laura.payment_links.dto.request.PayPaymentLinkRequest;
import com.laura.payment_links.dto.response.PaymentAttemptResponse;
import com.laura.payment_links.dto.response.PaymentLinkDetailsResponse;
import com.laura.payment_links.dto.response.PaymentLinkResponse;
import com.laura.payment_links.exception.*;
import com.laura.payment_links.model.*;
import com.laura.payment_links.repository.MerchantRepository;
import com.laura.payment_links.repository.PaymentAttemptRepository;
import com.laura.payment_links.repository.PaymentLinkRepository;
import com.laura.payment_links.util.PaymentLinkFilter;
import com.laura.payment_links.util.PaymentLinkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PaymentLinkService {
    private final PaymentLinkRepository paymentLinkRepository;
    private final MerchantRepository merchantRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final ReferenceGenerator referenceGenerator;
    private final PaymentLinkMapper mapper;

    // Métodos principales

    /**
     * Crea un nuevo link de pago para un comercio.
     * @param merchantId UUID del comercio (validado previamente por JWT).
     * @param request Datos para crear el link (amount, currency, etc.).
     * @return PaymentLinkResponse con referencia y estado.
     * @throws MerchantNotFoundException (404) si el comercio no existe.
     */
    public PaymentLinkResponse createPaymentLink(UUID merchantId, CreatePaymentLinkRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(merchantId));

        PaymentLink paymentLink = mapper.fromCreateRequest(request, merchant);
        paymentLink.setReference(referenceGenerator.generateReference()); // Genera PL-2023-0001

        return mapper.toResponse(paymentLinkRepository.save(paymentLink));
    }

    /**
     * Obtiene links de pago con filtros avanzados y paginación.
     * @param merchantId UUID del comercio (para seguridad).
     * @param filter Filtros opcionales (status, fechas, montos).
     * @param pageable Configuración de paginación (page, size, sort).
     * @return Página de PaymentLinkResponse.
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
        ).map(mapper::toResponse);
    }

    /**
     * Obtiene detalles completos de un link por ID o referencia.
     * @param merchantId UUID del comercio (seguridad).
     * @param identifier ID (UUID) o referencia (PL-2023-0001).
     * @return PaymentLinkDetailsResponse con todos los campos.
     * @throws PaymentLinkNotFoundException (404) si no existe o no pertenece al comercio.
     */
    public PaymentLinkDetailsResponse getPaymentLinkDetails(UUID merchantId, String identifier) {
        PaymentLink paymentLink = findByIdOrReference(merchantId, identifier);
        return mapper.toDetailsResponse(paymentLink);
    }

    /**
     * Procesa un intento de pago sobre un link (simulación).
     * @param merchantId UUID del comercio (validado por JWT).
     * @param paymentLinkId UUID del link a pagar.
     * @param request Contiene el payment_token ("ok_" o "fail_").
     * @param idempotencyKey UUID para evitar duplicados.
     * @return PaymentAttemptResponse con el resultado.
     * @throws PaymentLinkNotFoundException (404) si el link no existe.
     * @throws InvalidPaymentLinkStateException (409) si ya está pagado/vencido.
     * @throws DuplicatePaymentAttemptException (409) si idempotencyKey ya se usó.
     */
    public PaymentAttemptResponse payPaymentLink(UUID merchantId, UUID paymentLinkId,
                                                 PayPaymentLinkRequest request, String idempotencyKey) {
        PaymentLink paymentLink = paymentLinkRepository.findByIdAndMerchantId(paymentLinkId, merchantId)
                .orElseThrow(() -> new PaymentLinkNotFoundException(paymentLinkId));

        validatePaymentLinkState(paymentLink); // Método auxiliar
        checkIdempotency(paymentLinkId, idempotencyKey); // Método auxiliar

        PaymentAttempt attempt = processPaymentAttempt(paymentLink, request, idempotencyKey); // Método auxiliar
        return mapper.toAttemptResponse(attempt);
    }

    /**
     * Cancela un link de pago (solo si está en estado CREATED).
     * @param merchantId UUID del comercio.
     * @param paymentLinkId UUID del link a cancelar.
     * @return PaymentLinkResponse con estado actualizado.
     * @throws PaymentLinkNotFoundException (404) si no existe.
     * @throws InvalidPaymentLinkStateException (409) si no está en CREATED.
     */
    public PaymentLinkResponse cancelPaymentLink(UUID merchantId, UUID paymentLinkId) {
        PaymentLink paymentLink = paymentLinkRepository.findByIdAndMerchantId(paymentLinkId, merchantId)
                .orElseThrow(() -> new PaymentLinkNotFoundException(paymentLinkId));

        if (paymentLink.getStatus() != PaymentLinkStatus.CREATED) {
            throw new InvalidPaymentLinkStateException(
                    "Solo se pueden cancelar links en estado CREATED. Estado actual: " + paymentLink.getStatus()
            );
        }

        paymentLink.setStatus(PaymentLinkStatus.CANCELLED);
        return mapper.toResponse(paymentLinkRepository.save(paymentLink));
    }

    /**
     * Job programado: Expira links vencidos (status CREATED y expires_at < ahora).
     * @return Número de links expirados.
     */
    public int expirePaymentLinks() {
        int expiredCount = paymentLinkRepository.expireLinks(Instant.now());
        log.info("Expiraron {} links de pago", expiredCount);
        return expiredCount;
    }

    // Métodos auxiliaries //

    /**
     * Busca un link por ID (UUID) o referencia (PL-XXXX).
     * @UsedBy getPaymentLinkDetails()
     */
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

    /**
     * Valida que un link esté en estado CREATED y no vencido.
     * @UsedBy payPaymentLink()
     * @throws InvalidPaymentLinkStateException (409) si no está en CREATED.
     * @throws PaymentLinkExpiredException (409) si expires_at es pasado.
     */
    private void validatePaymentLinkState(PaymentLink paymentLink) {
        if (paymentLink.getStatus() != PaymentLinkStatus.CREATED) {
            throw new InvalidPaymentLinkStateException(
                    "El link debe estar en estado CREATED. Estado actual: " + paymentLink.getStatus()
            );
        }

        if (paymentLink.getExpiresAt().isBefore(Instant.now())) {
            throw new PaymentLinkExpiredException(paymentLink.getExpiresAt());
        }
    }

    /**
     * Verifica que no exista un intento previo con la misma idempotencyKey.
     * @UsedBy payPaymentLink()
     * @throws DuplicatePaymentAttemptException (409) si ya existe.
     */
    private void checkIdempotency(UUID paymentLinkId, String idempotencyKey) {
        if (paymentAttemptRepository.findByPaymentLinkIdAndIdempotencyKey(paymentLinkId, idempotencyKey).isPresent()) {
            throw new DuplicatePaymentAttemptException(idempotencyKey);
        }
    }

    /**
     * Procesa el intento de pago (simulación) y actualiza el estado del link si es exitoso.
     * @UsedBy payPaymentLink()
     */
    private PaymentAttempt processPaymentAttempt(PaymentLink paymentLink, PayPaymentLinkRequest request, String idempotencyKey) {
        PaymentAttemptStatus status = request.getPaymentToken().startsWith("ok_")
                ? PaymentAttemptStatus.SUCCESS
                : PaymentAttemptStatus.FAILED;

        PaymentAttempt attempt = PaymentAttempt.builder()
                .paymentLink(paymentLink)
                .status(status)
                .reason(status == PaymentAttemptStatus.FAILED ? "Pago rechazado" : null)
                .idempotencyKey(idempotencyKey)
                .build();

        if (status == PaymentAttemptStatus.SUCCESS) {
            paymentLink.setStatus(PaymentLinkStatus.PAID);
            paymentLink.setPaidAt(Instant.now());
            paymentLinkRepository.save(paymentLink);
        }

        return paymentAttemptRepository.save(attempt);
    }
}