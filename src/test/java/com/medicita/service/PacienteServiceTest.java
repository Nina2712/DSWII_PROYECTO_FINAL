package com.medicita.service;

import com.medicita.entity.Cita;
import com.medicita.entity.Especialidad;
import com.medicita.entity.Medico;
import com.medicita.entity.Paciente;
import com.medicita.entity.Sede;
import com.medicita.entity.Usuario;
import com.medicita.enums.EstadoCita;
import com.medicita.enums.Rol;
import com.medicita.exception.RecursoNoEncontradoException;
import com.medicita.repository.CitaRepository;
import com.medicita.repository.PacienteRepository;
import com.medicita.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("PacienteService: historial clínico y ficha (sin base de datos)")
class PacienteServiceTest {

    private PacienteRepository pacientes;
    private CitaRepository citas;
    private ContextoUsuario contexto;
    private PacienteService servicio;

    @BeforeEach
    void preparar() {
        pacientes = mock(PacienteRepository.class);
        citas = mock(CitaRepository.class);
        contexto = mock(ContextoUsuario.class);
        servicio = new PacienteService(mock(UsuarioRepository.class), pacientes, mock(PasswordEncoder.class),
                citas, contexto, Clock.systemDefaultZone());
        when(pacientes.existsById(7L)).thenReturn(true);
    }

    private static Cita atencion(long id, long medicoId, String nombreMedico, String especialidad) {
        Usuario u = Usuario.builder().nombres(nombreMedico).apellidos("Pérez").rol(Rol.MEDICO).build();
        Medico m = Medico.builder().id(medicoId).usuario(u)
                .especialidad(Especialidad.builder().nombre(especialidad).build())
                .sede(Sede.builder().nombre("Sede Miraflores").build()).cmp("CMP-1").build();
        return Cita.builder().id(id).medico(m).paciente(Paciente.builder().id(7L).build())
                .fechaHora(LocalDateTime.of(2026, 10, 1, 9, 0)).estado(EstadoCita.ATENDIDA)
                .motivo("Control").observaciones("Notas de " + nombreMedico).build();
    }

    @Test
    @DisplayName("El médico ve todo el historial y solo marca como propias las suyas")
    void historialConVariosMedicos() {
        when(contexto.actual()).thenReturn(new Actor(1L, "dr@test.pe", Rol.MEDICO, null, 10L));
        when(citas.existsByMedicoIdAndPacienteId(10L, 7L)).thenReturn(true);
        when(citas.historialDelPaciente(eq(7L), any(Pageable.class)))
                .thenReturn(List.of(atencion(1, 10, "Ricardo", "Cardiología"), atencion(2, 20, "Mónica", "Pediatría")));

        var h = servicio.historial(7L);

        assertThat(h).hasSize(2);
        assertThat(h.get(0).propia()).isTrue();
        assertThat(h.get(1).propia()).isFalse();
        assertThat(h.get(1).especialidad()).isEqualTo("Pediatría");
        assertThat(h.get(1).medicoNombre()).contains("Mónica");
    }

    @Test
    @DisplayName("Un médico que nunca atendió al paciente no ve su historial")
    void medicoAjeno() {
        when(contexto.actual()).thenReturn(new Actor(1L, "dr@test.pe", Rol.MEDICO, null, 10L));
        when(citas.existsByMedicoIdAndPacienteId(10L, 7L)).thenReturn(false);
        assertThatThrownBy(() -> servicio.historial(7L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("El administrador lo ve, pero ninguna atención es suya")
    void admin() {
        when(contexto.actual()).thenReturn(new Actor(2L, "admin@test.pe", Rol.ADMIN, null, null));
        when(citas.historialDelPaciente(eq(7L), any(Pageable.class))).thenReturn(List.of(atencion(1, 10, "Ricardo", "Cardiología")));
        assertThat(servicio.historial(7L)).extracting(a -> a.propia()).containsExactly(false);
    }
}
