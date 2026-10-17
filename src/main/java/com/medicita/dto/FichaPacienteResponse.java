package com.medicita.dto;

import com.medicita.enums.TipoSeguro;

import java.time.LocalDate;

/** Datos del paciente que el personal necesita ver al atenderlo. */
public record FichaPacienteResponse(
        Long id,
        String nombre,
        String dni,
        LocalDate fechaNacimiento,
        Integer edad,
        String telefono,
        String direccion,
        TipoSeguro tipoSeguro,
        String alergias,
        String contactoEmergenciaNombre,
        String contactoEmergenciaTelefono,
        String foto) {
}
