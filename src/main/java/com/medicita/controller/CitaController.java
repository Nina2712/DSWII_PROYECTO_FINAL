package com.medicita.controller;

import com.medicita.dto.CitaCreateRequest;
import com.medicita.dto.CitaFiltro;
import com.medicita.dto.CitaResponse;
import com.medicita.dto.CitaUpdateRequest;
import com.medicita.dto.PageResponse;
import com.medicita.enums.EstadoCita;
import com.medicita.service.CitaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
@Tag(name = "Citas", description = "Reserva y gestión de citas médicas (requiere token JWT)")
@SecurityRequirement(name = "bearerAuth")
public class CitaController {

    private final CitaService citaService;

    @GetMapping
    @Operation(summary = "Listar citas",
            description = "Paginado y con filtros. El paciente solo ve las suyas y el médico solo su agenda.")
    public ResponseEntity<PageResponse<CitaResponse>> listar(
            @RequestParam(required = false) EstadoCita estado,
            @RequestParam(required = false) Long medicoId,
            @RequestParam(required = false) Long pacienteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String orden) {
        CitaFiltro filtro = new CitaFiltro(estado, medicoId, pacienteId, desde, hasta);
        return ResponseEntity.ok(citaService.listar(filtro, page, size, "asc".equalsIgnoreCase(orden)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una cita por id")
    public ResponseEntity<CitaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(citaService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PACIENTE','RECEPCIONISTA','ADMIN')")
    @Operation(summary = "Reservar una cita",
            description = "El paciente reserva a su nombre; recepción y administración indican pacienteId.")
    public ResponseEntity<CitaResponse> crear(@Valid @RequestBody CitaCreateRequest request) {
        CitaResponse creada = citaService.crear(request);
        return ResponseEntity.created(URI.create("/api/citas/" + creada.id())).body(creada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una cita",
            description = "Reprogramar, cambiar motivo, estado u observaciones según el rol. Solo se modifican los campos enviados.")
    public ResponseEntity<CitaResponse> actualizar(@PathVariable Long id,
                                                   @Valid @RequestBody CitaUpdateRequest request) {
        return ResponseEntity.ok(citaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PACIENTE','RECEPCIONISTA','ADMIN')")
    @Operation(summary = "Cancelar una cita",
            description = "Cancela la cita (conserva el historial). Con definitivo=true la elimina de la base de datos (solo ADMIN).")
    public ResponseEntity<Void> eliminar(@PathVariable Long id,
                                         @RequestParam(defaultValue = "false") boolean definitivo) {
        citaService.eliminar(id, definitivo);
        return ResponseEntity.noContent().build();
    }
}
