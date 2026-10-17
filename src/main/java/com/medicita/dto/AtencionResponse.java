package com.medicita.dto;

import java.time.LocalDateTime;

/**
 * Una atención ya realizada, para el historial clínico del paciente.
 * {@code propia} indica que la atendió el médico que consulta (el único que puede corregir esas notas).
 */
public record AtencionResponse(
        Long citaId,
        LocalDateTime fechaHora,
        Long medicoId,
        String medicoNombre,
        String especialidad,
        String sede,
        String motivo,
        String observaciones,
        boolean propia) {
}
