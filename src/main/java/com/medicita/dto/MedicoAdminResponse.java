package com.medicita.dto;

import com.medicita.enums.DiaSemana;

import java.util.List;

/**
 * Ficha completa de un médico para la administración (incluye los desactivados).
 * {@code citasFueraDeHorario} solo se llena al editar: son las citas por atender que quedaron fuera del
 * horario nuevo (no se cancelan ni se mueven: recepción debe reprogramarlas).
 */
public record MedicoAdminResponse(
        Long id,
        String nombres,
        String apellidos,
        String email,
        String cmp,
        Long especialidadId,
        String especialidad,
        Long sedeId,
        String sede,
        boolean activo,
        List<Horario> horarios,
        List<CitaFueraDeHorarioResponse> citasFueraDeHorario,
        String foto) {

    public record Horario(DiaSemana dia, String inicio, String fin) {
    }
}
