package com.laura.payment_links.dto.request;

import lombok.*;
import jakarta.validation.constraints.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayPaymentLinkRequest {
    @NotBlank(message = "El token de pago es requerido")
    private String paymentToken;  // Ej: "tok_visa_4242" o "fail_insufficient_funds"
}
