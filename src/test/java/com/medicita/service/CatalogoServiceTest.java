package com.medicita.service;

import com.medicita.entity.Cita;
import com.medicita.entity.Especialidad;
import com.medicita.entity.HorarioDisponible;
import com.medicita.entity.Medico;
import com.medicita.entity.Usuario;
import com.medicita.enums.DiaSemana;
import com.medicita.enums.EstadoCita;
import com.medicita.enums.Rol;
import com.medicita.repository.CitaRepository;
import com.medicita.repository.HorarioDisponibleRepository;
import com.medicita.repository.MedicoRepository;
import com.medicita.repository.PacienteRepository;
import com.medicita.repository.SedeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("CatalogoService: disponibilidad por especialidad (sin base de datos)")
class CatalogoServiceTest {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final LocalDate LUNES = LocalDate.of(2030, 1, 7);

    private MedicoRepository medicos;
    private HorarioDisponibleRepository horarios;
    private CitaRepository citas;
    private CatalogoService servicio;
    private final Especialidad cardiologia = Especialidad.builder().id(2L).nombre("Cardiología").build();

    @BeforeEach
    void preparar() {
        medicos = mock(MedicoRepository.class);
        horarios = mock(HorarioDisponibleRepository.class);
        citas = mock(CitaRepository.class);
        Clock reloj = Clock.fixed(LUNES.minusDays(3).atTime(9, 0).atZone(LIMA).toInstant(), LIMA);
        servicio = new CatalogoService(medicos, mock(SedeRepository.class), mock(PacienteRepository.class), horarios, citas, reloj);
        when(citas.findByMedicoIdAndFechaHoraBetween(anyLong(), any(), any())).thenReturn(List.of());
    }

    private Medico medico(long id, String nombre, Especialidad esp, boolean activo) {
        Usuario u = Usuario.builder().nombres(nombre).apellidos("Prueba").rol(Rol.MEDICO).activo(activo).build();
        return Medico.builder().id(id).usuario(u).especialidad(esp).build();
    }

    private HorarioDisponible tramo(Medico m, DiaSemana dia, int desde, int hasta) {
        return HorarioDisponible.builder().medico(m).diaSemana(dia)
                .horaInicio(LocalTime.of(desde, 0)).horaFin(LocalTime.of(hasta, 0)).build();
    }

    @Test
    @DisplayName("Lista solo médicos activos de la especialidad, con sus horas libres del día")
    void horasPorMedico() {
        Medico a = medico(1, "Ana", cardiologia, true);
        Medico inactivo = medico(2, "Beto", cardiologia, false);
        Medico otraEsp = medico(3, "Carla", Especialidad.builder().id(9L).nombre("Pediatría").build(), true);
        when(medicos.listarConDetalle()).thenReturn(List.of(a, inactivo, otraEsp));
        when(horarios.findByMedicoId(1L)).thenReturn(List.of(tramo(a, DiaSemana.LUNES, 8, 10)));
        Cita ocupada = Cita.builder().medico(a).fechaHora(LUNES.atTime(8, 0)).estado(EstadoCita.CONFIRMADA).build();
        when(citas.findByMedicoIdAndFechaHoraBetween(anyLong(), any(), any())).thenReturn(List.of(ocupada));

        var r = servicio.disponibilidad(2L, LUNES);

        assertThat(r).hasSize(1);
        assertThat(r.get(0).medicoId()).isEqualTo(1L);
        assertThat(r.get(0).horas()).containsExactly("08:30", "09:00", "09:30");
        assertThat(r.get(0).proximaFecha()).isNull();
    }

    @Test
    @DisplayName("Si el médico no atiende ese día, ofrece la próxima fecha en la que sí tiene horas libres")
    void proximaFecha() {
        Medico a = medico(1, "Ana", cardiologia, true);
        when(medicos.listarConDetalle()).thenReturn(List.of(a));
        when(horarios.findByMedicoId(1L)).thenReturn(List.of(tramo(a, DiaSemana.MIERCOLES, 8, 9)));

        var r = servicio.disponibilidad(2L, LUNES);   // el lunes no atiende

        assertThat(r.get(0).horas()).isEmpty();
        assertThat(r.get(0).proximaFecha()).isEqualTo(LUNES.plusDays(2));   // miércoles 9
    }

    @Test
    @DisplayName("Un día lleno también ofrece la próxima fecha libre; sin horario no hay ninguna")
    void diaLlenoYSinHorario() {
        Medico a = medico(1, "Ana", cardiologia, true);
        Medico b = medico(2, "Beto", cardiologia, true);
        when(medicos.listarConDetalle()).thenReturn(List.of(a, b));
        when(horarios.findByMedicoId(1L)).thenReturn(List.of(tramo(a, DiaSemana.LUNES, 8, 9)));
        when(horarios.findByMedicoId(2L)).thenReturn(List.of());
        List<Cita> llenas = List.of(
                Cita.builder().medico(a).fechaHora(LUNES.atTime(8, 0)).estado(EstadoCita.PENDIENTE).build(),
                Cita.builder().medico(a).fechaHora(LUNES.atTime(8, 30)).estado(EstadoCita.CONFIRMADA).build());
        when(citas.findByMedicoIdAndFechaHoraBetween(anyLong(), any(), any())).thenAnswer(inv -> inv.getArgument(0).equals(1L) ? llenas : List.<Cita>of());

        var r = servicio.disponibilidad(2L, LUNES);

        assertThat(r.get(0).horas()).isEmpty();
        assertThat(r.get(0).proximaFecha()).isEqualTo(LUNES.plusDays(7));   // el lunes siguiente
        assertThat(r.get(1).horas()).isEmpty();
        assertThat(r.get(1).proximaFecha()).isNull();
    }
}
