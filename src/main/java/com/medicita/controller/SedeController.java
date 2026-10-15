package com.medicita.controller;

import com.medicita.dto.SedeDetalleResponse;
import com.medicita.dto.SedeRequest;
import com.medicita.dto.SedeResumenResponse;
import com.medicita.service.SedeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sedes")
@RequiredArgsConstructor
@Tag(name = "Sedes", description = "Listado para todos los usuarios; alta, edición y baja solo para ADMIN")
@SecurityRequirement(name = "bearerAuth")
public class SedeController {

    private final SedeService sedeService;

    @GetMapping
    @Operation(summary = "Sedes con sus médicos activos y las especialidades que se atienden en cada una")
    public ResponseEntity<List<SedeDetalleResponse>> listar() {
        return ResponseEntity.ok(sedeService.listar());
    }

    @GetMapping("/{id}/resumen")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Panel de una sede: citas de hoy, por atender, resultados de 30 días, médicos y próximas citas (ADMIN)")
    public ResponseEntity<SedeResumenResponse> resumen(@PathVariable Long id) {
        return ResponseEntity.ok(sedeService.resumen(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear una sede (ADMIN)")
    public ResponseEntity<SedeDetalleResponse> crear(@Valid @RequestBody SedeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sedeService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Editar una sede (ADMIN)")
    public ResponseEntity<SedeDetalleResponse> actualizar(@PathVariable Long id, @Valid @RequestBody SedeRequest request) {
        return ResponseEntity.ok(sedeService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar una sede sin médicos (ADMIN)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        sedeService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
