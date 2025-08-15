package com.laura.payment_links.dto.request;

import lombok.*;
import jakarta.validation.constraints.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentLinkRequest {
    @Min(value = 1, message = "El monto debe ser mayor a 0")
    private Integer amountCents;  // Cambiado a Integer para mejor validación

    @NotBlank(message = "La moneda no puede estar vacía")
    @Size(min = 3, max = 3, message = "La moneda debe tener 3 caracteres")
    private String currency;  // ISO 4217 (COP, USD, etc.)

    @NotBlank(message = "La descripción no puede estar vacía")
    @Size(max = 255, message = "La descripción no puede exceder 255 caracteres")
    private String description;

    @Min(value = 1, message = "El tiempo de expiración debe ser mínimo 1 minuto")
    private Integer expiresInMinutes;  // Cambiado a Integer

    private Map<String, Object> metadata;
}