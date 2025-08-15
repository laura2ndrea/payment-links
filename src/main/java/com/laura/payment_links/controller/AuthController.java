package com.laura.payment_links.controller;

import com.laura.payment_links.dto.request.MerchantRegistrationRequest;
import com.laura.payment_links.service.MerchantAuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class AuthController {

    private final MerchantAuthService merchantAuthService;

    public AuthController(MerchantAuthService merchantAuthService) {
        this.merchantAuthService = merchantAuthService;
    }

    @PostMapping("/payment-links/register")
    public ResponseEntity<String> registerMerchant(@RequestBody @Valid MerchantRegistrationRequest request) {
        UUID merchantId = merchantAuthService.registerMerchant(request);
        return ResponseEntity.ok("Comercio registrado con el ID: " + merchantId);
    }

    @PostMapping("/payment-links/login")
    public ResponseEntity<String> authenticate(
            @RequestParam String email,
            @RequestParam String password) {

        String token = merchantAuthService.authenticate(email, password);
        return ResponseEntity.ok(token);
    }
}