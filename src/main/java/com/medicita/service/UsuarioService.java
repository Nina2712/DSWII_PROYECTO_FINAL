package com.medicita.service;

import com.medicita.dto.UsuarioRequest;
import com.medicita.dto.UsuarioResponse;
import com.medicita.dto.UsuarioUpdateRequest;
import com.medicita.entity.Usuario;
import com.medicita.enums.Rol;
import com.medicita.exception.RecursoNoEncontradoException;
import com.medicita.exception.ReglaNegocioException;
import com.medicita.exception.SolicitudInvalidaException;
import com.medicita.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Personal del sistema: administradores y recepcionistas. Los pacientes se registran solos o en recepción,
 * y los médicos se gestionan desde su propia pantalla, por eso no aparecen aquí.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {

    static final List<Rol> PERSONAL = List.of(Rol.ADMIN, Rol.RECEPCIONISTA);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ContextoUsuario contexto;

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findByRolInOrderByNombresAscApellidosAsc(PERSONAL).stream()
                .map(UsuarioService::aRespuesta)
                .toList();
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        exigirRolDePersonal(request.rol());
        Claves.error(request.password()).ifPresent(m -> {
            throw new SolicitudInvalidaException(m);
        });
        String email = request.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(email)) {
            throw new ReglaNegocioException("Ya existe una cuenta con ese correo");
        }
        Usuario u = usuarioRepository.save(Usuario.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .nombres(request.nombres().trim())
                .apellidos(request.apellidos().trim())
                .rol(request.rol())
                .build());
        log.info("Usuario {} creado con rol {}", u.getId(), u.getRol());
        return aRespuesta(u);
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioUpdateRequest request) {
        exigirRolDePersonal(request.rol());
        Usuario u = usuarioRepository.findById(id)
                .filter(x -> PERSONAL.contains(x.getRol()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));

        boolean cambiaRol = u.getRol() != request.rol();
        boolean desactiva = u.isActivo() && !request.activo();

        if (u.getId().equals(contexto.actual().usuarioId()) && (cambiaRol || desactiva)) {
            throw new ReglaNegocioException("No puedes cambiar tu propio rol ni desactivar tu propia cuenta");
        }
        boolean dejaDeSerAdminActivo = u.getRol() == Rol.ADMIN && u.isActivo()
                && (request.rol() != Rol.ADMIN || !request.activo());
        if (dejaDeSerAdminActivo && usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN) <= 1) {
            throw new ReglaNegocioException("Debe quedar al menos un administrador activo");
        }

        String password = request.password();
        if (password != null && !password.isBlank()) {
            Claves.error(password).ifPresent(m -> {
                throw new SolicitudInvalidaException(m);
            });
            u.setPassword(passwordEncoder.encode(password));
        }
        u.setNombres(request.nombres().trim());
        u.setApellidos(request.apellidos().trim());
        u.setRol(request.rol());
        u.setActivo(request.activo());
        log.info("Usuario {} actualizado (rol {}, activo {})", u.getId(), u.getRol(), u.isActivo());
        return aRespuesta(usuarioRepository.save(u));
    }

    private static void exigirRolDePersonal(Rol rol) {
        if (!PERSONAL.contains(rol)) {
            throw new SolicitudInvalidaException("Aquí solo se crean administradores y recepcionistas");
        }
    }

    private static UsuarioResponse aRespuesta(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getEmail(), u.getNombres(), u.getApellidos(), u.getRol(), u.isActivo());
    }
}
