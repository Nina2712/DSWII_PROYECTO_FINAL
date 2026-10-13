package com.medicita.dto;

import com.medicita.enums.EstadoCita;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** Todos los campos son opcionales: solo se modifican los que se envían. */
public record CitaUpdateRequest(
        LocalDateTime fechaHora,

        @Size(max = 255, message = "El motivo no puede superar 255 caracteres")
        String motivo,

        @Size(max = 1000, message = "Las observaciones no pueden superar 1000 caracteres")
        String observaciones,

        EstadoCita estado) {

    public boolean sinCambios() {
        return fechaHora == null && motivo == null && observaciones == null && estado == null;
    }
}
