package com.medicita.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 120, message = "El correo no puede superar 120 caracteres")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(max = 100, message = "La contraseña no puede superar 100 caracteres")
        String password) {
}
