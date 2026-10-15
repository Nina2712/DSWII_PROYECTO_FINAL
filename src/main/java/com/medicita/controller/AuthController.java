package com.medicita.controller;

import com.medicita.dto.LoginRequest;
import com.medicita.dto.LoginResponse;
import com.medicita.dto.PerfilResponse;
import com.medicita.dto.RegistroPacienteRequest;
import com.medicita.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Login con JWT y registro de pacientes")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Valida correo y contraseña (BCrypt) y devuelve un token JWT")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/registro")
    @Operation(summary = "Crear cuenta de paciente",
            description = "Registro público: siempre crea un usuario con rol PACIENTE y devuelve el token para entrar de inmediato")
    public ResponseEntity<LoginResponse> registro(@Valid @RequestBody RegistroPacienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Perfil del usuario autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PerfilResponse> perfil(Authentication authentication) {
        return ResponseEntity.ok(authService.perfil(authentication.getName()));
    }
}
