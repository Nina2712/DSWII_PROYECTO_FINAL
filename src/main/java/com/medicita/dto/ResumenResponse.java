package com.medicita.dto;

import java.time.LocalDate;
import java.util.List;

/** Datos agregados para el dashboard. El alcance (todo / solo mi agenda / solo mis citas) lo decide el rol. */
public record ResumenResponse(
        LocalDate hoy,
        Kpis kpis,
        List<Conteo> porEstado,
        List<Conteo> porEspecialidad,
        List<Conteo> porMedico,
        List<PuntoDia> porDia,
        List<CitaResponse> proximas) {

    /** Indicadores de los últimos 30 días y los próximos 30. tasaAsistencia = atendidas / (atendidas + no asistió). */
    public record Kpis(
            long total,
            long hoy,
            long pendientes,
            long confirmadas,
            long atendidas,
            long canceladas,
            long noAsistio,
            Double tasaAsistencia) {
    }

    public record Conteo(String etiqueta, long total) {
    }

    public record PuntoDia(LocalDate fecha, long total, long canceladas) {
    }
}
