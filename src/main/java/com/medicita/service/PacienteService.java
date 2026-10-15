package com.medicita.service;

import com.medicita.dto.PacienteCatalogoResponse;
import com.medicita.dto.RegistroPacienteRequest;
import com.medicita.entity.Paciente;
import com.medicita.entity.Usuario;
import com.medicita.enums.Rol;
import com.medicita.exception.ReglaNegocioException;
import com.medicita.exception.SolicitudInvalidaException;
import com.medicita.repository.PacienteRepository;
import com.medicita.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Alta de pacientes: la comparten el registro propio (público) y el alta hecha por recepción. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PacienteService {

    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final PasswordEncoder passwordEncoder;

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
}
