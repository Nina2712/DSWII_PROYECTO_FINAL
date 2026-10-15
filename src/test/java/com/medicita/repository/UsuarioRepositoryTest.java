package com.medicita.repository;

import com.medicita.entity.Usuario;
import com.medicita.enums.Rol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RepositoryTest
@DisplayName("UsuarioRepository (MySQL real)")
class UsuarioRepositoryTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private TestEntityManager em;

    private Usuario nuevo(String email, Rol rol) {
        return Usuario.builder()
                .email(email).password(encoder.encode("Medicita2026*"))
                .nombres("Ana").apellidos("Torres").rol(rol).build();
    }

    @Test
    @DisplayName("Insertar: guarda el usuario con la contraseña cifrada con BCrypt")
    void insertar() {
        Usuario guardado = usuarioRepository.saveAndFlush(nuevo("ana@test.pe", Rol.PACIENTE));
        em.clear();

        Usuario leido = usuarioRepository.findById(guardado.getId()).orElseThrow();
        assertThat(leido.getEmail()).isEqualTo("ana@test.pe");
        assertThat(leido.isActivo()).isTrue();
        assertThat(leido.getCreadoEn()).isNotNull();
        assertThat(leido.getPassword()).startsWith("$2").isNotEqualTo("Medicita2026*");
        assertThat(encoder.matches("Medicita2026*", leido.getPassword())).isTrue();
        assertThat(encoder.matches("otra-clave", leido.getPassword())).isFalse();
    }

    @Test
    @DisplayName("Buscar por correo: findByEmail y existsByEmail")
    void buscarPorEmail() {
        usuarioRepository.saveAndFlush(nuevo("busca@test.pe", Rol.RECEPCIONISTA));

        assertThat(usuarioRepository.findByEmail("busca@test.pe")).isPresent()
                .get().extracting(Usuario::getRol).isEqualTo(Rol.RECEPCIONISTA);
        assertThat(usuarioRepository.findByEmail("no-existe@test.pe")).isEmpty();
        assertThat(usuarioRepository.existsByEmail("busca@test.pe")).isTrue();
        assertThat(usuarioRepository.existsByEmail("no-existe@test.pe")).isFalse();
    }

    @Test
    @DisplayName("Actualizar: cambia datos, rol y estado activo")
    void actualizar() {
        Usuario guardado = usuarioRepository.saveAndFlush(nuevo("cambia@test.pe", Rol.PACIENTE));
        em.clear();

        Usuario cargado = usuarioRepository.findById(guardado.getId()).orElseThrow();
        cargado.setNombres("Lucía");
        cargado.setRol(Rol.ADMIN);
        cargado.setActivo(false);
        usuarioRepository.saveAndFlush(cargado);
        em.clear();

        Usuario actualizado = usuarioRepository.findById(guardado.getId()).orElseThrow();
        assertThat(actualizado.getNombres()).isEqualTo("Lucía");
        assertThat(actualizado.getRol()).isEqualTo(Rol.ADMIN);
        assertThat(actualizado.isActivo()).isFalse();
    }

    @Test
    @DisplayName("Eliminar: borra el usuario")
    void eliminar() {
        Usuario guardado = usuarioRepository.saveAndFlush(nuevo("borra@test.pe", Rol.PACIENTE));
        long antes = usuarioRepository.count();

        usuarioRepository.deleteById(guardado.getId());
        usuarioRepository.flush();
        em.clear();

        assertThat(usuarioRepository.findById(guardado.getId())).isEmpty();
        assertThat(usuarioRepository.count()).isEqualTo(antes - 1);
    }

    @Test
    @DisplayName("Listar: findAll devuelve los usuarios guardados")
    void listar() {
        long base = usuarioRepository.count();
        usuarioRepository.save(nuevo("a@test.pe", Rol.PACIENTE));
        usuarioRepository.save(nuevo("b@test.pe", Rol.MEDICO));
        usuarioRepository.saveAndFlush(nuevo("c@test.pe", Rol.ADMIN));

        assertThat(usuarioRepository.findAll()).hasSize((int) base + 3)
                .extracting(Usuario::getEmail).contains("a@test.pe", "b@test.pe", "c@test.pe");
    }

    @Test
    @DisplayName("El correo es único: no se permite duplicarlo")
    void correoDuplicado() {
        usuarioRepository.saveAndFlush(nuevo("repetido@test.pe", Rol.PACIENTE));

        assertThatThrownBy(() -> usuarioRepository.saveAndFlush(nuevo("repetido@test.pe", Rol.MEDICO)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Integridad referencial: no se borra un usuario que tiene perfil de paciente")
    void noSeBorraUsuarioConPerfil() {
        var paciente = Datos.paciente(em, "conperfil@test.pe", "70000009");
        Long usuarioId = paciente.getUsuario().getId();
        // Se vacía la memoria de Hibernate: si el paciente siguiera cargado, Hibernate se quejaría
        // antes de llegar a la base. Así la restricción (FOREIGN KEY) la aplica MySQL.
        em.clear();

        assertThatThrownBy(() -> {
            usuarioRepository.deleteById(usuarioId);
            usuarioRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Personal: lista solo los roles pedidos y cuenta los administradores activos")
    void personalYAdministradoresActivos() {
        long adminsAntes = usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN);
        usuarioRepository.saveAndFlush(nuevo("admin2@test.pe", Rol.ADMIN));
        Usuario inactivo = nuevo("admin3@test.pe", Rol.ADMIN);
        inactivo.setActivo(false);
        usuarioRepository.saveAndFlush(inactivo);
        usuarioRepository.saveAndFlush(nuevo("recep@test.pe", Rol.RECEPCIONISTA));
        usuarioRepository.saveAndFlush(nuevo("paciente@test.pe", Rol.PACIENTE));
        em.clear();

        assertThat(usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN)).isEqualTo(adminsAntes + 1);
        assertThat(usuarioRepository.findByRolInOrderByNombresAscApellidosAsc(java.util.List.of(Rol.ADMIN, Rol.RECEPCIONISTA)))
                .extracting(Usuario::getEmail)
                .contains("admin2@test.pe", "admin3@test.pe", "recep@test.pe")
                .doesNotContain("paciente@test.pe");
    }
}
