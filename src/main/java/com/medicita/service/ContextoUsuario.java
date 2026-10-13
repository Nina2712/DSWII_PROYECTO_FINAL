package com.medicita.service;

import com.medicita.entity.Medico;
import com.medicita.entity.Paciente;
import com.medicita.entity.Usuario;
import com.medicita.enums.Rol;
import com.medicita.repository.MedicoRepository;
import com.medicita.repository.PacienteRepository;
import com.medicita.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Resuelve quién hace la petición actual a partir del token JWT ya validado. */
@Component
@RequiredArgsConstructor
public class ContextoUsuario {

    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;

    public Actor actual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Debes iniciar sesión");
        }
        Usuario usuario = usuarioRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("Usuario no válido"));

        Long pacienteId = null;
        Long medicoId = null;
        if (usuario.getRol() == Rol.PACIENTE) {
            pacienteId = pacienteRepository.findByUsuarioId(usuario.getId()).map(Paciente::getId).orElse(null);
        } else if (usuario.getRol() == Rol.MEDICO) {
            medicoId = medicoRepository.findByUsuarioId(usuario.getId()).map(Medico::getId).orElse(null);
        }
        return new Actor(usuario.getId(), usuario.getEmail(), usuario.getRol(), pacienteId, medicoId);
    }
}
