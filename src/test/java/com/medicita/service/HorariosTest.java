package com.medicita.service;

import com.medicita.dto.HorarioRequest;
import com.medicita.enums.DiaSemana;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Prueba unitaria pura (no necesita base de datos). */
class HorariosTest {

    private static HorarioRequest h(DiaSemana dia, int hi, int hf) {
        return new HorarioRequest(dia, LocalTime.of(hi, 0), LocalTime.of(hf, 0));
    }

    @Test
    @DisplayName("Acepta tramos válidos, incluso dos en el mismo día si no se tocan")
    void aceptaTramosValidos() {
        var horarios = List.of(h(DiaSemana.LUNES, 8, 13), h(DiaSemana.LUNES, 14, 18), h(DiaSemana.MARTES, 8, 12));

        assertThat(Horarios.primerError(horarios, 30)).isEmpty();
    }

    @Test
    @DisplayName("Tramos pegados (uno termina cuando empieza el otro) no se consideran solapados")
    void tramosContiguos() {
        var horarios = List.of(h(DiaSemana.JUEVES, 8, 12), h(DiaSemana.JUEVES, 12, 15));

        assertThat(Horarios.primerError(horarios, 30)).isEmpty();
    }

    @Test
    @DisplayName("Rechaza un fin igual o anterior al inicio")
    void finAnteriorAlInicio() {
        assertThat(Horarios.primerError(List.of(h(DiaSemana.LUNES, 13, 8)), 30))
                .hasValueSatisfying(m -> assertThat(m).contains("hora de fin"));
        assertThat(Horarios.primerError(List.of(h(DiaSemana.LUNES, 9, 9)), 30)).isPresent();
    }

    @Test
    @DisplayName("Rechaza un tramo más corto que una cita")
    void tramoMuyCorto() {
        var corto = new HorarioRequest(DiaSemana.VIERNES, LocalTime.of(8, 0), LocalTime.of(8, 20));

        assertThat(Horarios.primerError(List.of(corto), 30))
                .hasValueSatisfying(m -> assertThat(m).contains("al menos 30 minutos"));
    }

    @Test
    @DisplayName("Rechaza tramos solapados del mismo día, aunque lleguen desordenados")
    void tramosSolapados() {
        var horarios = List.of(h(DiaSemana.MIERCOLES, 10, 14), h(DiaSemana.MIERCOLES, 8, 11));

        assertThat(Horarios.primerError(horarios, 30))
                .hasValueSatisfying(m -> assertThat(m).contains("se solapan"));
    }

    @Test
    @DisplayName("El mismo horario en días distintos no es un solape")
    void diasDistintos() {
        var horarios = List.of(h(DiaSemana.LUNES, 8, 12), h(DiaSemana.MARTES, 8, 12));

        assertThat(Horarios.primerError(horarios, 30)).isEmpty();
    }

    @Test
    @DisplayName("dentro(): la cita debe caber completa en un tramo del mismo día de la semana")
    void citaDentroDelHorario() {
        var horarios = List.of(h(DiaSemana.LUNES, 8, 13));
        var lunes = LocalDateTime.of(2030, 1, 7, 9, 0);   // 7 de enero de 2030 es lunes

        assertThat(Horarios.dentro(horarios, lunes, 30)).isTrue();
        assertThat(Horarios.dentro(horarios, lunes.withHour(8).withMinute(0), 30)).isTrue();   // justo al inicio
        assertThat(Horarios.dentro(horarios, lunes.withHour(12).withMinute(30), 30)).isTrue(); // termina justo a las 13:00
        assertThat(Horarios.dentro(horarios, lunes.withHour(12).withMinute(45), 30)).isFalse(); // se pasa del fin
        assertThat(Horarios.dentro(horarios, lunes.withHour(7).withMinute(30), 30)).isFalse();  // antes del inicio
        assertThat(Horarios.dentro(horarios, lunes.plusDays(1), 30)).isFalse();                 // martes: no atiende
    }
}
