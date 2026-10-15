package com.medicita.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambioPasswordRequest(
        @NotBlank(message = "Escribe tu contraseña actual")
        String actual,

        @NotBlank(message = "La contraseña nueva es obligatoria")
        @Size(max = 72, message = "La contraseña no puede superar 72 caracteres")
        String nueva) {
}
