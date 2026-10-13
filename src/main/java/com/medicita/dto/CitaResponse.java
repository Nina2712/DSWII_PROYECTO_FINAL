package com.medicita.dto;

import com.medicita.enums.EstadoCita;

import java.time.LocalDateTime;

public record CitaResponse(
        Long id,
        Long pacienteId,
        String pacienteNombre,
        String pacienteDni,
        Long medicoId,
        String medicoNombre,
        String especialidad,
        String sede,
        LocalDateTime fechaHora,
        int duracionMin,
        EstadoCita estado,
        String motivo,
        String observaciones,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn) {
}
