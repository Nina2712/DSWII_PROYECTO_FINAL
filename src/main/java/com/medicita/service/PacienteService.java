package com.medicita.service;

import com.medicita.dto.FichaPacienteResponse;
import com.medicita.dto.PacienteCatalogoResponse;
import com.medicita.dto.RegistroPacienteRequest;
import com.medicita.entity.Paciente;
import com.medicita.entity.Usuario;
import com.medicita.enums.Rol;
import com.medicita.exception.RecursoNoEncontradoException;
import com.medicita.exception.ReglaNegocioException;
import com.medicita.exception.SolicitudInvalidaException;
import com.medicita.repository.CitaRepository;
import com.medicita.repository.PacienteRepository;
import com.medicita.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;

/** Alta de pacientes: la comparten el registro propio (público) y el alta hecha por recepción. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PacienteService {

    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final CitaRepository citaRepository;
    private final ContextoUsuario contexto;
    private final Clock clock;

    /** Crea el usuario (siempre con rol PACIENTE) y su perfil de paciente. */
    @Transactional
    public PacienteCatalogoResponse crear(RegistroPacienteRequest request) {
        Claves.error(request.password()).ifPresent(m -> {
            throw new SolicitudInvalidaException(m);
        });
        String email = request.email().trim().toLowerCase();
        String dni = request.dni().trim();

        if (usuarioRepository.existsByEmail(email)) {
            throw new ReglaNegocioException("Ya existe una cuenta con ese correo");
        }
        if (pacienteRepository.existsByDni(dni)) {
            throw new ReglaNegocioException("Ya hay un paciente registrado con ese DNI");
        }

        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .nombres(request.nombres().trim())
                .apellidos(request.apellidos().trim())
                .rol(Rol.PACIENTE)
                .build());

        String telefono = request.telefono() == null || request.telefono().isBlank() ? null : request.telefono().trim();
        Paciente paciente = pacienteRepository.save(Paciente.builder()
                .usuario(usuario)
                .dni(dni)
                .telefono(telefono)
                .fechaNacimiento(request.fechaNacimiento())
                .build());

        log.info("Paciente {} registrado ({})", paciente.getId(), email);
        return new PacienteCatalogoResponse(paciente.getId(),
                usuario.getNombres() + " " + usuario.getApellidos(), paciente.getDni());
    }

    /**
     * Ficha del paciente para quien lo atiende. Recepción y administración la ven siempre;
     * un médico solo si tiene o tuvo alguna cita con ese paciente.
     */
    @Transactional(readOnly = true)
    public FichaPacienteResponse ficha(Long id) {
        Actor actor = contexto.actual();
        Paciente p = pacienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente no encontrado: " + id));
        if (actor.rol() == Rol.MEDICO && (actor.medicoId() == null
                || !citaRepository.existsByMedicoIdAndPacienteId(actor.medicoId(), id))) {
            // se responde como si no existiera, igual que con las citas ajenas
            throw new RecursoNoEncontradoException("Paciente no encontrado: " + id);
        }
        Integer edad = p.getFechaNacimiento() == null ? null
                : Period.between(p.getFechaNacimiento(), LocalDate.now(clock)).getYears();
        return new FichaPacienteResponse(p.getId(),
                p.getUsuario().getNombres() + " " + p.getUsuario().getApellidos(), p.getDni(),
                p.getFechaNacimiento(), edad, p.getTelefono(), p.getDireccion(), p.getTipoSeguro(),
                p.getAlergias(), p.getContactoEmergenciaNombre(), p.getContactoEmergenciaTelefono());
    }
}
