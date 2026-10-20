package com.medicita.service;

import com.medicita.enums.DiaSemana;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Cálculo puro (sin Spring ni base de datos) de los bloques libres de un día. */
public final class Agenda {

    private Agenda() {
    }

    /** Tramo de atención de un médico (por ejemplo 08:00 a 13:00). */
    public record Tramo(LocalTime inicio, LocalTime fin) {
    }

    /**
     * Devuelve los inicios de bloque libres: dentro de algún tramo, alineados a la duración,
     * sin cita activa y posteriores al momento actual.
     */
    public static List<LocalTime> huecosLibres(List<Tramo> tramos, Set<LocalTime> ocupadas,
                                               LocalDate fecha, LocalDateTime ahora, int duracionMin) {
        List<LocalTime> libres = new ArrayList<>();
        for (Tramo tramo : tramos) {
            LocalTime inicio = tramo.inicio();
            while (!inicio.plusMinutes(duracionMin).isBefore(inicio)           // evita dar la vuelta a medianoche
                    && !inicio.plusMinutes(duracionMin).isAfter(tramo.fin())) {
                boolean futuro = fecha.atTime(inicio).isAfter(ahora);
                if (futuro && !ocupadas.contains(inicio) && !libres.contains(inicio)) {
                    libres.add(inicio);
                }
                inicio = inicio.plusMinutes(duracionMin);
            }
        }
        libres.sort(LocalTime::compareTo);
        return libres;
    }

    /** Día de la semana de una fecha en el enum del sistema (lunes = primero). */
    public static DiaSemana diaDe(LocalDate fecha) {
        return DiaSemana.values()[fecha.getDayOfWeek().getValue() - 1];
    }

    /**
     * Primer día, desde {@code desde} y dentro de {@code maxDias}, en el que el médico tiene al menos un bloque libre.
     * Sirve para ofrecer "la próxima fecha disponible" cuando el día elegido no tiene horas.
     */
    public static Optional<LocalDate> proximoDiaConHueco(Map<DiaSemana, List<Tramo>> tramosPorDia,
                                                         Map<LocalDate, Set<LocalTime>> ocupadasPorFecha,
                                                         LocalDate desde, LocalDateTime ahora,
                                                         int duracionMin, int maxDias) {
        for (int i = 0; i < maxDias; i++) {
            LocalDate dia = desde.plusDays(i);
            List<Tramo> tramos = tramosPorDia.getOrDefault(diaDe(dia), List.of());
            if (!tramos.isEmpty()
                    && !huecosLibres(tramos, ocupadasPorFecha.getOrDefault(dia, Set.of()), dia, ahora, duracionMin).isEmpty()) {
                return Optional.of(dia);
            }
        }
        return Optional.empty();
    }
}
