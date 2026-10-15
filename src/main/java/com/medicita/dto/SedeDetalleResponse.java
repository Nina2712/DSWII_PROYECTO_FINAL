package com.medicita.dto;

import java.util.List;

/**
 * Sede con los médicos activos que atienden en ella y las especialidades que ofrece.
 * {@code enUso} indica si tiene algún médico asignado (activo o no): en ese caso no se puede eliminar.
 */
public record SedeDetalleResponse(
        Long id,
        String nombre,
        String direccion,
        String distrito,
        long medicos,
        boolean enUso,
        List<EspecialidadEnSede> especialidades) {

    public record EspecialidadEnSede(Long id, String nombre, long medicos) {
    }
}
