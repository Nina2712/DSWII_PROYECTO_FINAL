package com.medicita.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Disponibilidad de un médico en una fecha: horas libres (HH:mm) y, si ese día no tiene ninguna,
 * la próxima fecha en la que sí atiende con un bloque libre ({@code null} si no hay en los próximos 60 días).
 */
public record DisponibilidadMedicoResponse(Long medicoId, LocalDate fecha, List<String> horas, LocalDate proximaFecha) {
}
