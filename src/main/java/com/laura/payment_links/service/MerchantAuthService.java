package com.laura.payment_links.service;

import com.laura.payment_links.dto.request.MerchantRegistrationRequest;
import com.laura.payment_links.exception.AuthException;
import com.laura.payment_links.exception.EmailAlreadyExistsException;
import com.laura.payment_links.model.Merchant;
import com.laura.payment_links.repository.MerchantRepository;
import com.laura.payment_links.security.Constants;
import com.laura.payment_links.security.JWTAuthtenticationConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MerchantAuthService {

    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTAuthtenticationConfig jwtAuthenticationConfig;

    public MerchantAuthService(MerchantRepository merchantRepository,
                               PasswordEncoder passwordEncoder,
                               JWTAuthtenticationConfig jwtAuthenticationConfig) {
        this.merchantRepository = merchantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtAuthenticationConfig = jwtAuthenticationConfig;
    }

    /**
     * Registra un nuevo comercio en el sistema.
     * @param request Datos de registro (nombre, email, contraseña)
     * @return UUID del comerciante registrado
     * @throws EmailAlreadyExistsException Si el email ya está registrado
     */
    public UUID registerMerchant(MerchantRegistrationRequest request) {
        // Verifica si el email ya existe
        if (merchantRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        // Crea y guarda el nuevo comerciante con contraseña encriptada
        Merchant merchant = Merchant.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        return merchantRepository.save(merchant).getId();
    }

    /**
     * Autentica un comerciante y genera su token JWT.
     * @param email Email del comerciante
     * @param password Contraseña sin encriptar
     * @return Token JWT (con prefijo "Bearer ")
     * @throws AuthException Si las credenciales son inválidas
     */
    public String authenticate(String email, String password) {
        // Busca el comerciante por email
        Merchant merchant = merchantRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("Credenciales inválidas"));

        // Verifica la contraseña
        if (!passwordEncoder.matches(password, merchant.getPasswordHash())) {
            throw new AuthException("Credenciales inválidas");
        }

        // Genera el token JWT usando la configuración existente
        return jwtAuthenticationConfig.getJWTToken(merchant.getEmail());
    }

    /**
     * Valida un token JWT y extrae el ID del comerciante.
     * @param token Token JWT (con o sin prefijo "Bearer ")
     * @return UUID del comerciante
     * @throws AuthException Si el token es inválido o está expirado
     */
    public UUID validateTokenAndGetMerchantId(String token) {
        try {
            // Remueve el prefijo "Bearer " si está presente
            String jwtToken = token.replace(Constants.TOKEN_BEARER_PREFIX, "");

            // Parsea y valida el token usando la clave secreta
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Constants.getSigningKey(Constants.SUPER_SECRET_KEY))
                    .build()
                    .parseClaimsJws(jwtToken)
                    .getBody();

            // El ID del comerciante se almacena en el claim "jti" (ID del token)
            return UUID.fromString(claims.getId());
        } catch (JwtException | IllegalArgumentException e) {
            throw new AuthException("Token inválido: " + e.getMessage());
        }
    }
}
