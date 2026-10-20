package com.medicita.controller;

import com.medicita.dto.DisponibilidadMedicoResponse;
import com.medicita.dto.HorasLibresResponse;
import com.medicita.dto.MedicoCatalogoResponse;
import com.medicita.dto.PacienteCatalogoResponse;
import com.medicita.dto.SedeResponse;
import com.medicita.service.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/catalogo")
@RequiredArgsConstructor
@Tag(name = "Catálogo", description = "Médicos, pacientes y horas libres para reservar (requiere token JWT)")
@SecurityRequirement(name = "bearerAuth")
public class CatalogoController {

    private final CatalogoService catalogoService;

    @GetMapping("/medicos")
    @Operation(summary = "Médicos con su especialidad, sede y horarios de atención")
    public ResponseEntity<List<MedicoCatalogoResponse>> medicos() {
        return ResponseEntity.ok(catalogoService.medicos());
    }

    @GetMapping("/sedes")
    @Operation(summary = "Sedes de la clínica")
    public ResponseEntity<List<SedeResponse>> sedes() {
        return ResponseEntity.ok(catalogoService.sedes());
    }

    @GetMapping("/pacientes")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN')")
    @Operation(summary = "Pacientes (solo recepción y administración, para reservar a su nombre)")
    public ResponseEntity<List<PacienteCatalogoResponse>> pacientes() {
        return ResponseEntity.ok(catalogoService.pacientes());
    }

    @GetMapping("/medicos/{id}/horas-libres")
    @Operation(summary = "Bloques de 30 minutos libres de un médico en una fecha")
    public ResponseEntity<HorasLibresResponse> horasLibres(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(catalogoService.horasLibres(id, fecha));
    }

    @GetMapping("/especialidades/{id}/disponibilidad")
    @Operation(summary = "Médicos de una especialidad con sus horas libres en una fecha y la próxima fecha libre")
    public ResponseEntity<List<DisponibilidadMedicoResponse>> disponibilidad(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(catalogoService.disponibilidad(id, fecha));
    }
}
