package com.medicita.dto;

import com.medicita.enums.EstadoCita;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Panel de una sede para el administrador: carga de trabajo de hoy, citas por atender, resultados de los
 * últimos 30 días, el equipo médico y las próximas citas.
 */
public record SedeResumenResponse(
        Long id,
        String nombre,
        String direccion,
        String distrito,
        long citasHoy,
        long porAtender,
        long atendidas30d,
        long canceladas30d,
        long noAsistio30d,
        List<MedicoDeSede> medicos,
        List<ProximaCita> proximas) {

    public record MedicoDeSede(Long id, String nombre, String especialidad, String cmp, boolean activo,
                               long citasHoy, long porAtender) {
    }

    public record ProximaCita(Long id, LocalDateTime fechaHora, String paciente, String medico,
                              String especialidad, EstadoCita estado) {
    }
}
