package com.laura.payment_links.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laura.payment_links.dto.request.CreatePaymentLinkRequest;
import com.laura.payment_links.dto.response.PaymentLinkDetailsResponse;
import com.laura.payment_links.dto.response.PaymentLinkResponse;
import com.laura.payment_links.model.Merchant;
import com.laura.payment_links.model.PaymentLink;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.jmx.export.metadata.InvalidMetadataException;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@Mapper(componentModel = "spring")
public interface PaymentLinkMapper {

    // Mapeos básicos

    // Se convierte una entidad PaymentLink a su DTO básico (para listados/respuestas simples).
    @Mapping(target = "amountCents", source = "amountCents")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "expiresAt", source = "expiresAt")
    PaymentLinkResponse toResponse(PaymentLink entity);

    // Se convierte una entidad PaymentLink a su DTO detallado (incluye metadata, fechas, etc.).
    @Mapping(target = "merchantId", source = "merchant.id")
    @Mapping(target = "metadata", expression = "java(convertJsonToMap(entity.getMetadata()))")
    PaymentLinkDetailsResponse toDetailsResponse(PaymentLink entity);

    // Convierte un CreatePaymentLinkRequest a la entidad PaymentLink (para creación).
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "CREATED")
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "metadata", expression = "java(convertMapToJson(request.getMetadata()))")
    PaymentLink fromCreateRequest(CreatePaymentLinkRequest request, @Context Merchant merchant);

    // Métodos de ayuda

    default Map<String, Object> convertJsonToMap(String json) {
        if (json == null) return null;
        try {
            return new ObjectMapper().readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new InvalidMetadataException("Metadata inválida");
        }
    }

    default String convertMapToJson(Map<String, Object> map) {
        if (map == null) return null;
        try {
            return new ObjectMapper().writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new InvalidMetadataException("Error al serializar metadata");
        }
    }
}
