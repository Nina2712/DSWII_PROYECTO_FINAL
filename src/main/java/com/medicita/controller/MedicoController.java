package com.medicita.controller;

import com.medicita.dto.MedicoAdminResponse;
import com.medicita.dto.MedicoRequest;
import com.medicita.service.MedicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/medicos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Médicos (administración)", description = "Alta, edición, horarios y activación de médicos. Solo ADMIN")
@SecurityRequirement(name = "bearerAuth")
public class MedicoController {

    private final MedicoService medicoService;

    @GetMapping
    @Operation(summary = "Todos los médicos, incluidos los desactivados")
    public ResponseEntity<List<MedicoAdminResponse>> listar() {
        return ResponseEntity.ok(medicoService.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ficha de un médico")
    public ResponseEntity<MedicoAdminResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(medicoService.obtener(id));
    }

    @PostMapping
    @Operation(summary = "Dar de alta a un médico con su usuario, especialidad, sede y horarios")
    public ResponseEntity<MedicoAdminResponse> crear(@Valid @RequestBody MedicoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicoService.crear(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar un médico, reemplazar sus horarios o activarlo/desactivarlo",
            description = "No se elimina un médico (tiene historial de citas): se desactiva con activo=false. "
                    + "Solo se puede desactivar si no tiene citas por atender.")
    public ResponseEntity<MedicoAdminResponse> actualizar(@PathVariable Long id,
                                                          @Valid @RequestBody MedicoRequest request) {
        return ResponseEntity.ok(medicoService.actualizar(id, request));
    }
}
