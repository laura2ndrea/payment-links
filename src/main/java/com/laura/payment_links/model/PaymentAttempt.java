package com.laura.payment_links.model;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_attempts",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"payment_link_id", "idempotency_key"})
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_link_id", nullable = false)
    private PaymentLink paymentLink;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentAttemptStatus status;

    private String reason; // Motivo del fallo (opcional)

    @Column(name = "idempotency_key")
    private String idempotencyKey; // Para evitar procesamiento duplicado

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
