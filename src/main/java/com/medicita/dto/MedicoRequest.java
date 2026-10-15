package com.medicita.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Alta o edición de un médico (solo ADMIN).
 * En el alta la contraseña es obligatoria; en la edición solo se cambia si se envía una nueva.
 * {@code activo} se ignora en el alta (siempre queda activo).
 */
public record MedicoRequest(
        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 80, message = "Los nombres no pueden superar 80 caracteres")
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 80, message = "Los apellidos no pueden superar 80 caracteres")
        String apellidos,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 120, message = "El correo no puede superar 120 caracteres")
        String email,

        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,

        @NotBlank(message = "El CMP es obligatorio")
        @Pattern(regexp = "^[A-Za-z0-9-]{3,10}$", message = "El CMP debe tener de 3 a 10 caracteres (letras, números o guion)")
        String cmp,

        @NotNull(message = "La especialidad es obligatoria")
        Long especialidadId,

        @NotNull(message = "La sede es obligatoria")
        Long sedeId,

        @NotEmpty(message = "Indica al menos un horario de atención")
        @Valid
        List<HorarioRequest> horarios,

        Boolean activo) {
}
