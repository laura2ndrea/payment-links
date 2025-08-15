package com.laura.payment_links.exception;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Errores de autenticación / autorización
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiError> handleAuthException(AuthException ex) {
        ApiError error = new ApiError(
                "https://api.payment.com/errors/unauthorized",
                "Unauthorized",
                HttpStatus.UNAUTHORIZED.value(),
                ex.getMessage(),
                "UNAUTHORIZED"
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    // Errores de validación
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.groupingBy(
                        fieldError -> fieldError.getField(),
                        Collectors.mapping(
                                fieldError -> fieldError.getDefaultMessage(),
                                Collectors.toList()
                        )
                ))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().toArray(String[]::new)
                ));

        ApiError error = new ApiError(
                "https://api.payment.com/errors/validation_error",
                "Validation Error",
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "Campos inválidos en la solicitud",
                "VALIDATION_ERROR",
                errors
        );
        return ResponseEntity.unprocessableEntity().body(error);
    }

    // Errores de negocio //
    @ExceptionHandler(PaymentLinkNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(PaymentLinkNotFoundException ex) {
        ApiError error = new ApiError(
                "https://api.payment.com/errors/not_found",
                "Not Found",
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                "PAYMENT_LINK_NOT_FOUND"
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(InvalidPaymentLinkStateException.class)
    public ResponseEntity<ApiError> handleConflict(InvalidPaymentLinkStateException ex) {
        ApiError error = new ApiError(
                "https://api.payment.com/errors/conflict",
                "Conflict",
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                "CONFLICT"
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // Error cuando no se consigue un Merchant
    @ExceptionHandler(MerchantNotFoundException.class)
    public ResponseEntity<ApiError> handleMerchantNotFound(MerchantNotFoundException ex) {
        ApiError error = new ApiError(
                "https://api.payment.com/errors/not_found",
                "Not Found",
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                "MERCHANT_NOT_FOUND" // Código personalizado
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // Error cuando se intenta duplicar un pago
    @ExceptionHandler(DuplicatePaymentAttemptException.class)
    public ResponseEntity<ApiError> handleDuplicatePayment(DuplicatePaymentAttemptException ex) {
        ApiError error = new ApiError(
                "https://api.payment.com/errors/duplicate-payment",
                "Duplicate Payment Attempt",
                HttpStatus.CONFLICT.value(),  // 409
                ex.getMessage(),
                "DUPLICATE_OPERATION"
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // Excepción cuando un link expira
    @ExceptionHandler(PaymentLinkExpiredException.class)
    public ResponseEntity<ApiError> handlePaymentLinkExpired(PaymentLinkExpiredException ex) {
        ApiError error = new ApiError(
                "https://api.payment.com/errors/payment-link-expired",
                "Payment Link Expired",
                HttpStatus.CONFLICT.value(), // 409 Conflict
                ex.getMessage(),
                "LINK_EXPIRED",
                Map.of("expiresAt", ex.getExpiresAt())
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // Excepción para avisar sobre email que ya existe
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        ApiError error = new ApiError(
                "https://api.payment.com/errors/conflict",
                "Conflict",
                HttpStatus.CONFLICT.value(), // 409
                ex.getMessage(),
                "EMAIL_ALREADY_EXISTS"
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // Error genérico (500) //
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleInternalError(Exception ex) {
        ApiError error = new ApiError(
                "https://api.payment.com/errors/internal_error",
                "Internal Server Error",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Ocurrió un error inesperado",
                "INTERNAL_ERROR"
        );
        return ResponseEntity.internalServerError().body(error);
    }
}