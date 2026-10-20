package com.medicita.dto;

import com.medicita.enums.DiaSemana;

import java.util.List;

public record MedicoCatalogoResponse(
        Long id,
        String nombre,
        String cmp,
        Long especialidadId,
        String especialidad,
        String sede,
        List<Horario> horarios,
        String descripcion,
        String foto) {

    public record Horario(DiaSemana dia, String inicio, String fin) {
    }
}
