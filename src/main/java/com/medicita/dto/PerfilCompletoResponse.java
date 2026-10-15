package com.medicita.dto;

import com.medicita.enums.TipoSeguro;

import java.time.LocalDate;

/**
 * Perfil propio. Los campos que no corresponden al rol van en null
 * (por ejemplo, {@code dni} solo para pacientes y {@code cmp} solo para médicos).
 */
public record PerfilCompletoResponse(
        String email,
        String nombres,
        String apellidos,
        String rol,
        String telefono,
        // paciente
        String dni,
        LocalDate fechaNacimiento,
        String direccion,
        TipoSeguro tipoSeguro,
        String alergias,
        String contactoEmergenciaNombre,
        String contactoEmergenciaTelefono,
        // médico
        String cmp,
        String especialidad,
        String sede,
        String descripcion) {
}
