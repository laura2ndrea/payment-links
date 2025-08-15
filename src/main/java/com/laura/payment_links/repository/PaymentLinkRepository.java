package com.laura.payment_links.repository;

import com.laura.payment_links.model.PaymentLink;
import com.laura.payment_links.model.PaymentLinkStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PaymentLinkRepository extends JpaRepository<PaymentLink, UUID> {

    // Busca un link por ID y merchant
    Optional<PaymentLink> findByIdAndMerchantId(UUID id, UUID merchantId);

    // Busca por referencia única y merchant
    Optional<PaymentLink> findByReferenceAndMerchantId(String reference, UUID merchantId);

    // Query dinámica con filtros
    @Query("SELECT pl FROM PaymentLink pl WHERE " +
            "pl.merchant.id = :merchantId " +
            "AND (:status IS NULL OR pl.status = :status) " +
            "AND (:fromDate IS NULL OR pl.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR pl.createdAt <= :toDate) " +
            "AND (:minAmount IS NULL OR pl.amountCents >= :minAmount) " +
            "AND (:maxAmount IS NULL OR pl.amountCents <= :maxAmount)")
    Page<PaymentLink> search(
            @Param("merchantId") UUID merchantId,
            @Param("status") PaymentLinkStatus status,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            @Param("minAmount") Integer minAmount,
            @Param("maxAmount") Integer maxAmount,
            Pageable pageable);

    // Job de expiración (marca links vencidos)
    @Modifying
    @Query("UPDATE PaymentLink pl SET pl.status = 'EXPIRED' " +
            "WHERE pl.status = 'CREATED' AND pl.expiresAt < :now")
    int expireLinks(@Param("now") Instant now);
}
