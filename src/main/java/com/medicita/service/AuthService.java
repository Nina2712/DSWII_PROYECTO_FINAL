package com.medicita.service;

import com.medicita.dto.LoginRequest;
import com.medicita.dto.LoginResponse;
import com.medicita.dto.PerfilResponse;
import com.medicita.dto.RegistroPacienteRequest;
import com.medicita.entity.Usuario;
import com.medicita.exception.RecursoNoEncontradoException;
import com.medicita.repository.UsuarioRepository;
import com.medicita.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final PacienteService pacienteService;

    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim();
        // Lanza BadCredentialsException / DisabledException si falla; BCrypt compara el hash
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        Usuario usuario = buscar(userDetails.getUsername());
        log.info("Login exitoso: {} ({})", usuario.getEmail(), usuario.getRol());

        return new LoginResponse(
                jwtService.generarToken(userDetails),
                "Bearer",
                jwtService.getExpiracionMinutos(),
                usuario.getEmail(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getRol().name());
    }

    /** Crea la cuenta de un paciente y lo deja con la sesión iniciada. */
    public LoginResponse registrar(RegistroPacienteRequest request) {
        pacienteService.crear(request);
        return login(new LoginRequest(request.email().trim().toLowerCase(), request.password()));
    }

    public PerfilResponse perfil(String email) {
        Usuario u = buscar(email);
        return new PerfilResponse(u.getEmail(), u.getNombres(), u.getApellidos(), u.getRol().name());
    }

    private Usuario buscar(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + email));
    }
}
