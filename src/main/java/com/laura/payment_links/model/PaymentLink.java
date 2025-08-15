package com.laura.payment_links.model;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_links",
        indexes = {
                @Index(name = "idx_payment_links_expires_at", columnList = "expires_at"),
                @Index(name = "idx_payment_links_merchant_status", columnList = "merchant_id, status")
        },
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "reference")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentLink {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(name = "amount_cents", nullable = false)
    @Min(1)
    private Integer amountCents;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentLinkStatus status = PaymentLinkStatus.CREATED;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(columnDefinition = "jsonb") // Cambiado a jsonb para PostgreSQL (más eficiente)
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

