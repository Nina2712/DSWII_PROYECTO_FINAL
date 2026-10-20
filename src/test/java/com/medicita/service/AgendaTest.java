package com.medicita.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.medicita.enums.DiaSemana;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Prueba unitaria pura (no necesita base de datos). */
class AgendaTest {

    private static final LocalDate DIA = LocalDate.of(2030, 1, 7);
    private static final LocalDateTime ANTES = LocalDateTime.of(2030, 1, 1, 8, 0);
    private static final int BLOQUE = 30;

    @Test
    @DisplayName("Genera bloques de 30 minutos dentro del tramo y el último termina a la hora fin")
    void generaBloquesDelTramo() {
        var tramos = List.of(new Agenda.Tramo(LocalTime.of(8, 0), LocalTime.of(10, 0)));

        var libres = Agenda.huecosLibres(tramos, Set.of(), DIA, ANTES, BLOQUE);

        assertThat(libres).containsExactly(
                LocalTime.of(8, 0), LocalTime.of(8, 30), LocalTime.of(9, 0), LocalTime.of(9, 30));
    }

    @Test
    @DisplayName("Excluye los bloques ocupados")
    void excluyeOcupados() {
        var tramos = List.of(new Agenda.Tramo(LocalTime.of(8, 0), LocalTime.of(10, 0)));

        var libres = Agenda.huecosLibres(tramos, Set.of(LocalTime.of(8, 30)), DIA, ANTES, BLOQUE);

        assertThat(libres).doesNotContain(LocalTime.of(8, 30)).hasSize(3);
    }

    @Test
    @DisplayName("Excluye los bloques que ya pasaron el mismo día")
    void excluyePasados() {
        var tramos = List.of(new Agenda.Tramo(LocalTime.of(8, 0), LocalTime.of(10, 0)));
        var ahora = LocalDateTime.of(2030, 1, 7, 9, 0);

        var libres = Agenda.huecosLibres(tramos, Set.of(), DIA, ahora, BLOQUE);

        assertThat(libres).containsExactly(LocalTime.of(9, 30));
    }

    @Test
    @DisplayName("Un tramo más corto que un bloque no genera horas y un día sin tramos queda vacío")
    void tramosInsuficientes() {
        var corto = List.of(new Agenda.Tramo(LocalTime.of(8, 0), LocalTime.of(8, 20)));

        assertThat(Agenda.huecosLibres(corto, Set.of(), DIA, ANTES, BLOQUE)).isEmpty();
        assertThat(Agenda.huecosLibres(List.of(), Set.of(), DIA, ANTES, BLOQUE)).isEmpty();
    }

    @Test
    @DisplayName("Combina varios tramos sin repetir horas y en orden")
    void variosTramos() {
        var tramos = List.of(
                new Agenda.Tramo(LocalTime.of(14, 0), LocalTime.of(15, 0)),
                new Agenda.Tramo(LocalTime.of(8, 0), LocalTime.of(9, 0)));

        var libres = Agenda.huecosLibres(tramos, Set.of(), DIA, ANTES, BLOQUE);

        assertThat(libres).containsExactly(
                LocalTime.of(8, 0), LocalTime.of(8, 30), LocalTime.of(14, 0), LocalTime.of(14, 30));
    }

    @Test
    @DisplayName("diaDe: 7 de enero de 2030 es lunes y 12 es sábado")
    void diaDeLaSemana() {
        assertThat(Agenda.diaDe(DIA)).isEqualTo(DiaSemana.LUNES);
        assertThat(Agenda.diaDe(DIA.plusDays(5))).isEqualTo(DiaSemana.SABADO);
    }

    @Test
    @DisplayName("Próxima fecha libre: salta los días sin atención y los que ya están llenos")
    void proximoDiaConHueco() {
        // atiende lunes y miércoles de 8:00 a 9:00 (dos bloques)
        var tramos = Map.of(
                DiaSemana.LUNES, List.of(new Agenda.Tramo(LocalTime.of(8, 0), LocalTime.of(9, 0))),
                DiaSemana.MIERCOLES, List.of(new Agenda.Tramo(LocalTime.of(8, 0), LocalTime.of(9, 0))));
        // el lunes 14 está lleno; el miércoles 9 tiene un solo bloque ocupado
        var ocupadas = Map.of(
                DIA.plusDays(7), Set.of(LocalTime.of(8, 0), LocalTime.of(8, 30)),
                DIA.plusDays(2), Set.of(LocalTime.of(8, 0)));

        // desde el martes 8: el miércoles 9 todavía tiene el bloque de las 8:30
        assertThat(Agenda.proximoDiaConHueco(tramos, ocupadas, DIA.plusDays(1), ANTES, BLOQUE, 30))
                .contains(DIA.plusDays(2));
        // desde el jueves 10: el lunes 14 está lleno, así que gana el miércoles 16
        assertThat(Agenda.proximoDiaConHueco(tramos, ocupadas, DIA.plusDays(3), ANTES, BLOQUE, 30))
                .contains(DIA.plusDays(9));
    }

    @Test
    @DisplayName("Próxima fecha libre: vacía si el médico no atiende en la ventana")
    void sinProximoDia() {
        assertThat(Agenda.proximoDiaConHueco(Map.of(), Map.of(), DIA, ANTES, BLOQUE, 60)).isEmpty();
        var soloLunes = Map.of(DiaSemana.LUNES, List.of(new Agenda.Tramo(LocalTime.of(8, 0), LocalTime.of(9, 0))));
        assertThat(Agenda.proximoDiaConHueco(soloLunes, Map.of(), DIA.plusDays(1), ANTES, BLOQUE, 3)).isEmpty();
    }
}
