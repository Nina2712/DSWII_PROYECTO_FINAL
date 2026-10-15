package com.medicita.dto;

import com.medicita.enums.TipoSeguro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Cambios que cada usuario puede hacer a su propio perfil. Solo se aplican los campos que corresponden a su rol:
 * el paciente edita su ficha, el médico su teléfono y presentación; el personal, solo nombre y apellidos.
 * El correo y el DNI no se pueden cambiar desde aquí.
 */
public record PerfilUpdateRequest(
        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 80, message = "Los nombres no pueden superar 80 caracteres")
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 80, message = "Los apellidos no pueden superar 80 caracteres")
        String apellidos,

        @Pattern(regexp = "^$|^[0-9+ ]{7,15}$", message = "El teléfono solo puede tener números (7 a 15 dígitos)")
        String telefono,

        @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
        LocalDate fechaNacimiento,

        @Size(max = 200, message = "La dirección no puede superar 200 caracteres")
        String direccion,

        TipoSeguro tipoSeguro,

        @Size(max = 500, message = "Las alergias no pueden superar 500 caracteres")
        String alergias,

        @Size(max = 100, message = "El nombre del contacto no puede superar 100 caracteres")
        String contactoEmergenciaNombre,

        @Pattern(regexp = "^$|^[0-9+ ]{7,15}$", message = "El teléfono del contacto solo puede tener números (7 a 15 dígitos)")
        String contactoEmergenciaTelefono,

        @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
        String descripcion) {
}
