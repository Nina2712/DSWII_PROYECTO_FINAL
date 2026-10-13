package com.medicita.dto;

import com.medicita.enums.EstadoCita;

import java.time.LocalDate;

/** Filtros opcionales para listar citas. */
public record CitaFiltro(
        EstadoCita estado,
        Long medicoId,
        Long pacienteId,
        LocalDate desde,
        LocalDate hasta) {
}
