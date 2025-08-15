package com.laura.payment_links.util;

import com.laura.payment_links.model.PaymentLinkStatus;
import lombok.Data;

import java.time.Instant;

@Data
public class PaymentLinkFilter {
    private PaymentLinkStatus status;       // CREATED, PAID, CANCELLED, EXPIRED
    private Instant fromDate;               // Fecha mínima de creación
    private Instant toDate;                 // Fecha máxima de creación
    private Integer minAmount;              // Monto mínimo en centavos
    private Integer maxAmount;              // Monto máximo en centavos
}