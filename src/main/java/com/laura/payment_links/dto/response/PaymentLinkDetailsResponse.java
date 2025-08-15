package com.laura.payment_links.dto.response;

import lombok.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PaymentLinkDetailsResponse extends PaymentLinkResponse {
    private String description;
    private Instant paidAt;
    private Instant createdAt;
    private Map<String, Object> metadata;
    private UUID merchantId;
}