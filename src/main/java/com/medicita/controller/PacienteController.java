package com.medicita.controller;

import com.medicita.dto.AtencionResponse;
import com.medicita.dto.FichaPacienteResponse;
import com.medicita.dto.PacienteCatalogoResponse;
import com.medicita.dto.RegistroPacienteRequest;
import com.medicita.service.PacienteService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pacientes")
@RequiredArgsConstructor
@Tag(name = "Pacientes", description = "Alta de pacientes por recepción o administración")
@SecurityRequirement(name = "bearerAuth")
public class PacienteController {

    private final PacienteService pacienteService;

    @PostMapping
    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN')")
    @Operation(summary = "Registrar a un paciente que llega a la clínica (RECEPCIONISTA o ADMIN)")
    public ResponseEntity<PacienteCatalogoResponse> crear(@Valid @RequestBody RegistroPacienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pacienteService.crear(request));
    }

    @GetMapping("/{id}/ficha")
    @PreAuthorize("hasAnyRole('MEDICO','RECEPCIONISTA','ADMIN')")
    @Operation(summary = "Ficha del paciente (alergias, seguro, contacto de emergencia)",
            description = "El médico solo puede verla si tiene o tuvo una cita con ese paciente.")
    public ResponseEntity<FichaPacienteResponse> ficha(@PathVariable Long id) {
        return ResponseEntity.ok(pacienteService.ficha(id));
    }

    @GetMapping("/{id}/historial")
    @PreAuthorize("hasAnyRole('MEDICO','ADMIN')")
    @Operation(summary = "Historial clínico del paciente (atenciones previas con su médico y especialidad)",
            description = "El médico solo lo ve si tiene o tuvo una cita con ese paciente. Incluye las atenciones de todos los médicos.")
    public ResponseEntity<List<AtencionResponse>> historial(@PathVariable Long id) {
        return ResponseEntity.ok(pacienteService.historial(id));
    }
}
