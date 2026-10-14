package com.medicita.controller;

import com.medicita.dto.ResumenResponse;
import com.medicita.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Indicadores para el panel web (requiere token JWT)")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/resumen")
    @Operation(summary = "Resumen del panel",
            description = "Indicadores y series de los últimos 30 y próximos 30 días. Cada rol ve solo lo suyo.")
    public ResponseEntity<ResumenResponse> resumen() {
        return ResponseEntity.ok(dashboardService.resumen());
    }
}
