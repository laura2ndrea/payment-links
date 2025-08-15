package com.laura.payment_links.dto.request;

import lombok.*;
import jakarta.validation.constraints.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantRegistrationRequest {
    private String name;
    private String email;
    private String password;
}
