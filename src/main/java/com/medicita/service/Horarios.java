package com.medicita.service;

import com.medicita.dto.HorarioRequest;
import com.medicita.enums.DiaSemana;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Validación pura (sin Spring ni base de datos) de los horarios semanales de un médico. */
public final class Horarios {

    private Horarios() {
    }

    /**
     * Devuelve el primer problema encontrado, o vacío si los horarios son correctos:
     * el fin debe ser posterior al inicio, cada tramo debe poder contener al menos una cita
     * y dos tramos del mismo día no pueden solaparse.
     */
    public static Optional<String> primerError(List<HorarioRequest> horarios, int duracionMin) {
        for (HorarioRequest h : horarios) {
            if (!h.fin().isAfter(h.inicio())) {
                return Optional.of("En el horario del " + etiqueta(h) + ", la hora de fin debe ser posterior a la de inicio");
            }
            if (h.inicio().plusMinutes(duracionMin).isAfter(h.fin())) {
                return Optional.of("El horario del " + etiqueta(h) + " debe durar al menos " + duracionMin + " minutos");
            }
        }
        List<HorarioRequest> ordenados = horarios.stream()
                .sorted(Comparator.comparing(HorarioRequest::dia).thenComparing(HorarioRequest::inicio))
                .toList();
        for (int i = 1; i < ordenados.size(); i++) {
            HorarioRequest previo = ordenados.get(i - 1);
            HorarioRequest actual = ordenados.get(i);
            if (previo.dia() == actual.dia() && actual.inicio().isBefore(previo.fin())) {
                return Optional.of("Los horarios del " + etiqueta(actual) + " se solapan");
            }
        }
        return Optional.empty();
    }

    /**
     * ¿La cita (inicio y duración) cabe completa dentro de algún tramo del día de la semana que le corresponde?
     * Las horas de {@code HorarioRequest} se usan tal cual (el día se compara por nombre del enum).
     */
    public static boolean dentro(List<HorarioRequest> horarios, LocalDateTime inicio, int duracionMin) {
        String dia = DiaSemana.values()[inicio.getDayOfWeek().getValue() - 1].name();
        LocalTime desde = inicio.toLocalTime();
        LocalTime hasta = desde.plusMinutes(duracionMin);
        if (hasta.isBefore(desde)) {
            return false; // la cita cruzaría la medianoche
        }
        return horarios.stream().anyMatch(h -> h.dia().name().equals(dia)
                && !desde.isBefore(h.inicio()) && !hasta.isAfter(h.fin()));
    }

    private static String etiqueta(HorarioRequest h) {
        return h.dia().name().toLowerCase() + " (" + hora(h.inicio()) + " a " + hora(h.fin()) + ")";
    }

    private static String hora(LocalTime t) {
        return String.format("%02d:%02d", t.getHour(), t.getMinute());
    }
}
