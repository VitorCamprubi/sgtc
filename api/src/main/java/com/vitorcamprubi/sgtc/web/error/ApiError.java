package com.vitorcamprubi.sgtc.web.error;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Formato unificado de erro retornado pela API.
 *
 * Exemplo:
 * {
 *   "code": "VALIDATION_ERROR",
 *   "message": "Dados invalidos",
 *   "status": 400,
 *   "path": "/api/grupos",
 *   "timestamp": "2026-05-04T12:34:56Z",
 *   "fields": [
 *     { "field": "titulo", "message": "must not be blank" }
 *   ]
 * }
 */
public record ApiError(
        String code,
        String message,
        int status,
        String path,
        OffsetDateTime timestamp,
        List<FieldError> fields
) {
    public record FieldError(String field, String message) {}
}
