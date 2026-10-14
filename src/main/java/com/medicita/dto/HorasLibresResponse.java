package com.medicita.dto;

import java.time.LocalDate;
import java.util.List;

/** Horas (formato HH:mm) en las que el médico tiene un bloque de 30 minutos libre ese día. */
public record HorasLibresResponse(Long medicoId, LocalDate fecha, List<String> horas) {
}
