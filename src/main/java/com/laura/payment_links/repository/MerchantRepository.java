package com.laura.payment_links.repository;

import com.laura.payment_links.model.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    // Busca un comercio por email (para autenticación/registro)
    Optional<Merchant> findByEmail(String email);

    // Verifica existencia de email (evita duplicados)
    boolean existsByEmail(String email);
}
