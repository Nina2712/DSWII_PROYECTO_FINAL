package com.medicita.controller;

import com.medicita.dto.CambioPasswordRequest;
import com.medicita.dto.FotoRequest;
import com.medicita.dto.PerfilCompletoResponse;
import com.medicita.dto.PerfilUpdateRequest;
import com.medicita.service.PerfilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/perfil")
@RequiredArgsConstructor
@Tag(name = "Mi perfil", description = "Cada usuario ve y edita sus propios datos y cambia su contraseña")
@SecurityRequirement(name = "bearerAuth")
public class PerfilController {

    private final PerfilService perfilService;

    @GetMapping
    @Operation(summary = "Ver mi perfil (los campos dependen del rol)")
    public ResponseEntity<PerfilCompletoResponse> obtener() {
        return ResponseEntity.ok(perfilService.obtener());
    }

    @PutMapping
    @Operation(summary = "Editar mi perfil",
            description = "Paciente: ficha (contacto, seguro, alergias, contacto de emergencia). Médico: teléfono y presentación. Todos: nombres y apellidos.")
    public ResponseEntity<PerfilCompletoResponse> actualizar(@Valid @RequestBody PerfilUpdateRequest request) {
        return ResponseEntity.ok(perfilService.actualizar(request));
    }

    @PutMapping("/foto")
    @Operation(summary = "Cambiar o quitar mi foto de perfil", description = "JPEG pequeño como data URL; vacía para quitarla")
    public ResponseEntity<PerfilCompletoResponse> foto(@RequestBody FotoRequest request) {
        return ResponseEntity.ok(perfilService.actualizarFoto(request.foto()));
    }

    @PutMapping("/password")
    @Operation(summary = "Cambiar mi contraseña", description = "Exige la contraseña actual")
    public ResponseEntity<Void> cambiarPassword(@Valid @RequestBody CambioPasswordRequest request) {
        perfilService.cambiarPassword(request);
        return ResponseEntity.noContent().build();
    }
}
