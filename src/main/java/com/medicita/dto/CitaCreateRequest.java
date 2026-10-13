package com.medicita.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CitaCreateRequest(
        /* Solo lo usan recepción y administración; el paciente reserva siempre a su nombre. */
        Long pacienteId,

        @NotNull(message = "El médico es obligatorio")
        Long medicoId,

        @NotNull(message = "La fecha y hora son obligatorias")
        LocalDateTime fechaHora,

        @Size(max = 255, message = "El motivo no puede superar 255 caracteres")
        String motivo) {
}
