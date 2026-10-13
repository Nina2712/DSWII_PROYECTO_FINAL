package com.medicita.dto;

import java.time.LocalDateTime;
import java.util.Map;

/** Formato único de error para toda la API. */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String mensaje,
        String ruta,
        Map<String, String> errores) {

    public static ErrorResponse of(int status, String error, String mensaje, String ruta) {
        return new ErrorResponse(LocalDateTime.now(), status, error, mensaje, ruta, null);
    }

    public static ErrorResponse of(int status, String error, String mensaje, String ruta,
                                   Map<String, String> errores) {
        return new ErrorResponse(LocalDateTime.now(), status, error, mensaje, ruta, errores);
    }
}
