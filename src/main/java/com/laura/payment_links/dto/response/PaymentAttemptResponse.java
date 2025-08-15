package com.laura.payment_links.dto.response;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAttemptResponse {
    private UUID id;
    private String status;  // SUCCESS o FAILED
    private String reason;  // Null si fue exitoso
    private Instant createdAt;
    private String paymentLinkId;  // Referencia al link de pago
}
