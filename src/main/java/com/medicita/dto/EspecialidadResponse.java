package com.medicita.dto;

import java.util.List;

/**
 * Especialidad con sus médicos activos y las sedes donde se atiende.
 * {@code enUso} indica si tiene algún médico asignado (activo o no): en ese caso no se puede eliminar.
 */
public record EspecialidadResponse(
        Long id,
        String nombre,
        String descripcion,
        long medicos,
        boolean enUso,
        List<String> sedes) {
}
