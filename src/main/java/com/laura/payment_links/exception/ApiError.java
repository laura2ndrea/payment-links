package com.laura.payment_links.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.net.URI;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
    private URI type;
    private String title;
    private int status;
    private String detail;
    private String code;
    private Map<String, Object> errors;

    // Constructor para errores simples
    public ApiError(String type, String title, int status, String detail, String code,  Map<String, Object> errors) {
        this.type = URI.create(type);
        this.title = title;
        this.status = status;
        this.detail = detail;
        this.code = code;
    }

    // Constructor para errores de validación
    public ApiError(String type, String title, int status, String detail, String code) {
        this(type, title, status, detail, code, null);
    }
}

