package com.laura.payment_links.dto.response;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentLinkResponse {
    private UUID id;
    private String reference;
    private String status;  // CREATED, PAID, etc.
    private Instant expiresAt;
    private Integer amountCents;
    private String currency;
}
