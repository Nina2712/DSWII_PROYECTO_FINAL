package com.medicita.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
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
}
