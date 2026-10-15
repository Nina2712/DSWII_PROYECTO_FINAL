package com.medicita.service;

import com.medicita.dto.CambioPasswordRequest;
import com.medicita.dto.PerfilUpdateRequest;
import com.medicita.entity.Paciente;
import com.medicita.entity.Usuario;
import com.medicita.enums.Rol;
import com.medicita.enums.TipoSeguro;
import com.medicita.exception.ReglaNegocioException;
import com.medicita.exception.SolicitudInvalidaException;
import com.medicita.repository.MedicoRepository;
import com.medicita.repository.PacienteRepository;
import com.medicita.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("PerfilService: mi perfil y cambio de contraseña (sin base de datos)")
class PerfilServiceTest {

    private UsuarioRepository usuarios;
    private PacienteRepository pacientes;
    private PasswordEncoder encoder;
    private ContextoUsuario contexto;
    private PerfilService servicio;
    private Usuario usuario;
    private Paciente paciente;

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepository.class);
        pacientes = mock(PacienteRepository.class);
        encoder = mock(PasswordEncoder.class);
        contexto = mock(ContextoUsuario.class);
        servicio = new PerfilService(usuarios, pacientes, mock(MedicoRepository.class), encoder, contexto);

        usuario = Usuario.builder().id(5L).email("ana@test.pe").password("hash-actual")
                .nombres("Ana").apellidos("Torres").rol(Rol.PACIENTE).build();
        paciente = Paciente.builder().id(7L).usuario(usuario).dni("70123456").build();
        when(contexto.actual()).thenReturn(new Actor(5L, "ana@test.pe", Rol.PACIENTE, 7L, null));
        when(usuarios.findById(5L)).thenReturn(Optional.of(usuario));
        when(pacientes.findByUsuarioId(5L)).thenReturn(Optional.of(paciente));
        when(encoder.encode(any())).thenReturn("hash-nuevo");
        when(encoder.matches("Actual2026x", "hash-actual")).thenReturn(true);
    }

    @Test
    @DisplayName("El paciente guarda su ficha; los textos vacíos quedan en null")
    void actualizaFicha() {
        var req = new PerfilUpdateRequest(" Ana María ", "Torres", "987654321", null, " Av. Lima 123 ",
                TipoSeguro.EPS, "  ", "Luis Torres", "999888777", "ignorado");
        var res = servicio.actualizar(req);

        assertThat(res.nombres()).isEqualTo("Ana María");
        assertThat(res.direccion()).isEqualTo("Av. Lima 123");
        assertThat(res.tipoSeguro()).isEqualTo(TipoSeguro.EPS);
        assertThat(res.alergias()).isNull();
        assertThat(res.contactoEmergenciaNombre()).isEqualTo("Luis Torres");
        assertThat(res.dni()).isEqualTo("70123456");
        assertThat(res.cmp()).isNull();
    }

    @Test
    @DisplayName("Contraseña: la actual debe ser correcta")
    void actualIncorrecta() {
        assertThatThrownBy(() -> servicio.cambiarPassword(new CambioPasswordRequest("otra", "Nueva2026x")))
                .isInstanceOf(ReglaNegocioException.class);
        verify(usuarios, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Contraseña: la nueva debe cumplir las reglas y ser distinta")
    void nuevaInvalida() {
        assertThatThrownBy(() -> servicio.cambiarPassword(new CambioPasswordRequest("Actual2026x", "corta")))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> servicio.cambiarPassword(new CambioPasswordRequest("Actual2026x", "Actual2026x")))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    @DisplayName("Contraseña: con datos válidos se guarda cifrada")
    void cambia() {
        servicio.cambiarPassword(new CambioPasswordRequest("Actual2026x", "Nueva2026x"));
        assertThat(usuario.getPassword()).isEqualTo("hash-nuevo");
    }
}
