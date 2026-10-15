package com.medicita.service;

import com.medicita.dto.UsuarioRequest;
import com.medicita.dto.UsuarioUpdateRequest;
import com.medicita.entity.Usuario;
import com.medicita.enums.Rol;
import com.medicita.exception.ReglaNegocioException;
import com.medicita.exception.SolicitudInvalidaException;
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

@DisplayName("UsuarioService: reglas de personal (sin base de datos)")
class UsuarioServiceTest {

    private UsuarioRepository repo;
    private PasswordEncoder encoder;
    private ContextoUsuario contexto;
    private UsuarioService servicio;

    @BeforeEach
    void preparar() {
        repo = mock(UsuarioRepository.class);
        encoder = mock(PasswordEncoder.class);
        contexto = mock(ContextoUsuario.class);
        servicio = new UsuarioService(repo, encoder, contexto);
        when(encoder.encode(any())).thenReturn("hash");
        when(repo.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        // quien hace la petición es el usuario 1
        when(contexto.actual()).thenReturn(new Actor(1L, "yo@test.pe", Rol.ADMIN, null, null));
    }

    private static Usuario usuario(long id, Rol rol, boolean activo) {
        return Usuario.builder().id(id).email("u" + id + "@test.pe").password("x")
                .nombres("Ana").apellidos("Torres").rol(rol).activo(activo).build();
    }

    private static UsuarioUpdateRequest cambio(Rol rol, boolean activo, String password) {
        return new UsuarioUpdateRequest("Ana", "Torres", rol, activo, password);
    }

    @Test
    @DisplayName("No se crean pacientes ni médicos desde aquí")
    void soloPersonal() {
        var req = new UsuarioRequest("Ana", "Torres", "a@test.pe", "Clave2026x", Rol.MEDICO);
        assertThatThrownBy(() -> servicio.crear(req)).isInstanceOf(SolicitudInvalidaException.class);
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("Correo repetido: se rechaza")
    void correoRepetido() {
        when(repo.existsByEmail("a@test.pe")).thenReturn(true);
        var req = new UsuarioRequest("Ana", "Torres", " A@Test.pe ", "Clave2026x", Rol.RECEPCIONISTA);
        assertThatThrownBy(() -> servicio.crear(req)).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("Se crea con el correo en minúsculas y la contraseña cifrada")
    void crea() {
        var res = servicio.crear(new UsuarioRequest(" Ana ", "Torres", " A@Test.pe ", "Clave2026x", Rol.ADMIN));
        assertThat(res.email()).isEqualTo("a@test.pe");
        assertThat(res.rol()).isEqualTo(Rol.ADMIN);
        verify(encoder).encode("Clave2026x");
    }

    @Test
    @DisplayName("No puedes desactivarte ni cambiarte el rol a ti mismo")
    void noSobreUnoMismo() {
        when(repo.findById(1L)).thenReturn(Optional.of(usuario(1, Rol.ADMIN, true)));
        assertThatThrownBy(() -> servicio.actualizar(1L, cambio(Rol.ADMIN, false, null)))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.actualizar(1L, cambio(Rol.RECEPCIONISTA, true, null)))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("Siempre debe quedar un administrador activo")
    void ultimoAdmin() {
        when(repo.findById(2L)).thenReturn(Optional.of(usuario(2, Rol.ADMIN, true)));
        when(repo.countByRolAndActivoTrue(Rol.ADMIN)).thenReturn(1L);
        assertThatThrownBy(() -> servicio.actualizar(2L, cambio(Rol.ADMIN, false, null)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("al menos un administrador");

        when(repo.countByRolAndActivoTrue(Rol.ADMIN)).thenReturn(2L);
        assertThat(servicio.actualizar(2L, cambio(Rol.ADMIN, false, null)).activo()).isFalse();
    }

    @Test
    @DisplayName("Un recepcionista se puede desactivar y la contraseña vacía no se toca")
    void recepcionista() {
        Usuario r = usuario(3, Rol.RECEPCIONISTA, true);
        when(repo.findById(3L)).thenReturn(Optional.of(r));
        var res = servicio.actualizar(3L, cambio(Rol.RECEPCIONISTA, false, "  "));
        assertThat(res.activo()).isFalse();
        assertThat(r.getPassword()).isEqualTo("x");
        verify(encoder, never()).encode(any());
    }

    @Test
    @DisplayName("Contraseña nueva débil: se rechaza; válida: se cifra")
    void restablecerClave() {
        Usuario r = usuario(3, Rol.RECEPCIONISTA, true);
        when(repo.findById(3L)).thenReturn(Optional.of(r));
        assertThatThrownBy(() -> servicio.actualizar(3L, cambio(Rol.RECEPCIONISTA, true, "corta")))
                .isInstanceOf(SolicitudInvalidaException.class);
        servicio.actualizar(3L, cambio(Rol.RECEPCIONISTA, true, "Nueva2026x"));
        assertThat(r.getPassword()).isEqualTo("hash");
    }

    @Test
    @DisplayName("Un paciente no se puede editar por esta vía")
    void noEsPersonal() {
        when(repo.findById(9L)).thenReturn(Optional.of(usuario(9, Rol.PACIENTE, true)));
        assertThatThrownBy(() -> servicio.actualizar(9L, cambio(Rol.RECEPCIONISTA, true, null)))
                .hasMessageContaining("no encontrado");
    }
}
