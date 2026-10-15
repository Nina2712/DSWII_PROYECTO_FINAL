package com.medicita.dto;

import com.medicita.enums.EstadoCita;

import java.time.LocalDateTime;

/** Cita por atender que quedó fuera del horario nuevo de su médico. */
public record CitaFueraDeHorarioResponse(
        Long id,
        LocalDateTime fechaHora,
        String pacienteNombre,
        EstadoCita estado) {
}
