package com.medicita.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SedeRequest(
        @NotBlank(message = "El nombre de la sede es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @NotBlank(message = "La dirección es obligatoria")
        @Size(max = 200, message = "La dirección no puede superar 200 caracteres")
        String direccion,

        @NotBlank(message = "El distrito es obligatorio")
        @Size(max = 80, message = "El distrito no puede superar 80 caracteres")
        String distrito) {
}
