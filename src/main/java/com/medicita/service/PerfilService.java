package com.medicita.service;

import com.medicita.dto.CambioPasswordRequest;
import com.medicita.dto.PerfilCompletoResponse;
import com.medicita.dto.PerfilUpdateRequest;
import com.medicita.entity.Medico;
import com.medicita.entity.Paciente;
import com.medicita.entity.Usuario;
import com.medicita.enums.Rol;
import com.medicita.exception.RecursoNoEncontradoException;
import com.medicita.exception.ReglaNegocioException;
import com.medicita.exception.SolicitudInvalidaException;
import com.medicita.repository.MedicoRepository;
import com.medicita.repository.PacienteRepository;
import com.medicita.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** "Mi perfil": cada usuario ve y edita sus propios datos y cambia su contraseña. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PerfilService {

    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final PasswordEncoder passwordEncoder;
    private final ContextoUsuario contexto;

    @Transactional(readOnly = true)
    public PerfilCompletoResponse obtener() {
        Actor actor = contexto.actual();
        Usuario u = usuario(actor);
        return switch (u.getRol()) {
            case PACIENTE -> aRespuesta(u, paciente(u), null);
            case MEDICO -> aRespuesta(u, null, medico(u));
            case RECEPCIONISTA, ADMIN -> aRespuesta(u, null, null);
        };
    }

    @Transactional
    public PerfilCompletoResponse actualizar(PerfilUpdateRequest r) {
        Actor actor = contexto.actual();
        Usuario u = usuario(actor);
        u.setNombres(r.nombres().trim());
        u.setApellidos(r.apellidos().trim());
        usuarioRepository.save(u);

        Paciente paciente = null;
        Medico medico = null;
        if (u.getRol() == Rol.PACIENTE) {
            paciente = paciente(u);
            paciente.setTelefono(vacioANull(r.telefono()));
            paciente.setFechaNacimiento(r.fechaNacimiento());
            paciente.setDireccion(vacioANull(r.direccion()));
            paciente.setTipoSeguro(r.tipoSeguro());
            paciente.setAlergias(vacioANull(r.alergias()));
            paciente.setContactoEmergenciaNombre(vacioANull(r.contactoEmergenciaNombre()));
            paciente.setContactoEmergenciaTelefono(vacioANull(r.contactoEmergenciaTelefono()));
            pacienteRepository.save(paciente);
        } else if (u.getRol() == Rol.MEDICO) {
            medico = medico(u);
            medico.setTelefono(vacioANull(r.telefono()));
            medico.setDescripcion(vacioANull(r.descripcion()));
            medicoRepository.save(medico);
        }
        log.info("Perfil actualizado por {} ({})", u.getEmail(), u.getRol());
        return aRespuesta(u, paciente, medico);
    }

    @Transactional
    public void cambiarPassword(CambioPasswordRequest r) {
        Usuario u = usuario(contexto.actual());
        if (!passwordEncoder.matches(r.actual(), u.getPassword())) {
            throw new ReglaNegocioException("La contraseña actual no es correcta");
        }
        Claves.error(r.nueva()).ifPresent(m -> {
            throw new SolicitudInvalidaException(m);
        });
        if (r.nueva().equals(r.actual())) {
            throw new SolicitudInvalidaException("La contraseña nueva debe ser distinta de la actual");
        }
        u.setPassword(passwordEncoder.encode(r.nueva()));
        usuarioRepository.save(u);
        log.info("Contraseña cambiada por {}", u.getEmail());
    }

    private Usuario usuario(Actor actor) {
        return usuarioRepository.findById(actor.usuarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    private Paciente paciente(Usuario u) {
        return pacienteRepository.findByUsuarioId(u.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Tu usuario no tiene un perfil de paciente asociado"));
    }

    private Medico medico(Usuario u) {
        return medicoRepository.findByUsuarioId(u.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Tu usuario no tiene un perfil de médico asociado"));
    }

    private static String vacioANull(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static PerfilCompletoResponse aRespuesta(Usuario u, Paciente p, Medico m) {
        return new PerfilCompletoResponse(u.getEmail(), u.getNombres(), u.getApellidos(), u.getRol().name(),
                p != null ? p.getTelefono() : m != null ? m.getTelefono() : null,
                p != null ? p.getDni() : null,
                p != null ? p.getFechaNacimiento() : null,
                p != null ? p.getDireccion() : null,
                p != null ? p.getTipoSeguro() : null,
                p != null ? p.getAlergias() : null,
                p != null ? p.getContactoEmergenciaNombre() : null,
                p != null ? p.getContactoEmergenciaTelefono() : null,
                m != null ? m.getCmp() : null,
                m != null ? m.getEspecialidad().getNombre() : null,
                m != null ? m.getSede().getNombre() : null,
                m != null ? m.getDescripcion() : null);
    }
}
