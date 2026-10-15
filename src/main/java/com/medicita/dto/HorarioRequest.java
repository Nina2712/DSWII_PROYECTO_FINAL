package com.medicita.dto;

import com.medicita.enums.DiaSemana;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/** Tramo de atención semanal de un médico (por ejemplo lunes de 08:00 a 13:00). */
public record HorarioRequest(
        @NotNull(message = "El día del horario es obligatorio")
        DiaSemana dia,

        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime inicio,

        @NotNull(message = "La hora de fin es obligatoria")
        LocalTime fin) {
}
