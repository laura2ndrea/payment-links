package com.laura.payment_links.controller;

import com.laura.payment_links.dto.request.*;
import com.laura.payment_links.dto.response.*;
import com.laura.payment_links.model.PaymentLinkStatus;
import com.laura.payment_links.service.MerchantAuthService;
import com.laura.payment_links.service.PaymentLinkService;
import com.laura.payment_links.util.PaymentLinkFilter;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/payment-links")
public class PaymentLinkController {

    private final PaymentLinkService paymentLinkService;
    private final MerchantAuthService merchantAuthService;

    public PaymentLinkController(PaymentLinkService paymentLinkService, MerchantAuthService merchantAuthService) {
        this.paymentLinkService = paymentLinkService;
        this.merchantAuthService = merchantAuthService;
    }

    /**
     * Endpoint 1: Crear link de pago
     */
    @PostMapping
    public ResponseEntity<PaymentLinkResponse> createPaymentLink(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody @Valid CreatePaymentLinkRequest request) {

        UUID merchantId = extractMerchantIdFromAuth(authHeader);
        PaymentLinkResponse response = paymentLinkService.createPaymentLink(merchantId, request);
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Endpoint 2: Listar links con filtros
     */
    @GetMapping
    public ResponseEntity<Page<PaymentLinkResponse>> getPaymentLinks(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) PaymentLinkStatus status,
            @RequestParam(required = false) Integer minAmount,
            @RequestParam(required = false) Integer maxAmount,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            Pageable pageable) {

        UUID merchantId = extractMerchantIdFromAuth(authHeader);

        PaymentLinkFilter filter = PaymentLinkFilter.builder()
                .status(status)
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .fromDate(parseInstant(fromDate))
                .toDate(parseInstant(toDate))
                .build();

        Page<PaymentLinkResponse> response = paymentLinkService.getPaymentLinks(merchantId, filter, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint 3: Obtener detalles de un link
     */
    @GetMapping("/{identifier}")
    public ResponseEntity<PaymentLinkDetailsResponse> getPaymentLinkDetails(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String identifier) {

        UUID merchantId = extractMerchantIdFromAuth(authHeader);
        PaymentLinkDetailsResponse response = paymentLinkService.getPaymentLinkDetails(merchantId, identifier);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint 4: Pagar un link (simulación)
     */
    @PostMapping("/{id}/pay")
    public ResponseEntity<PaymentAttemptResponse> payPaymentLink(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable UUID id,
            @RequestBody @Valid PayPaymentLinkRequest request) {

        UUID merchantId = extractMerchantIdFromAuth(authHeader);
        PaymentAttemptResponse response = paymentLinkService.payPaymentLink(merchantId, id, request, idempotencyKey);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint 5: Cancelar un link
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<PaymentLinkResponse> cancelPaymentLink(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id) {

        UUID merchantId = extractMerchantIdFromAuth(authHeader);
        PaymentLinkResponse response = paymentLinkService.cancelPaymentLink(merchantId, id);
        return ResponseEntity.ok(response);
    }

    // Métodos auxiliares
    private UUID extractMerchantIdFromAuth(String authHeader) {
        // Implementación basada en MerchantAuthService
        String token = authHeader.replace("Bearer ", "");
        return merchantAuthService.validateTokenAndGetMerchantId(token);
    }

    private Instant parseInstant(String dateStr) {
        return dateStr != null ? Instant.parse(dateStr) : null;
    }
}
