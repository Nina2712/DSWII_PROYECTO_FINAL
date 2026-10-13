package com.medicita.service;

import com.medicita.dto.CitaResponse;
import com.medicita.entity.Cita;
import com.medicita.entity.Medico;
import com.medicita.entity.Paciente;
import com.medicita.entity.Usuario;

/** Convierte la entidad Cita en su DTO de respuesta (se usa dentro de una transacción). */
public final class CitaMapper {

    private CitaMapper() {
    }

    public static CitaResponse toResponse(Cita c) {
        Paciente p = c.getPaciente();
        Medico m = c.getMedico();
        return new CitaResponse(
                c.getId(),
                p.getId(), nombreCompleto(p.getUsuario()), p.getDni(),
                m.getId(), nombreCompleto(m.getUsuario()),
                m.getEspecialidad().getNombre(), m.getSede().getNombre(),
                c.getFechaHora(), c.getDuracionMin(), c.getEstado(),
                c.getMotivo(), c.getObservaciones(),
                c.getCreadoEn(), c.getActualizadoEn());
    }

    private static String nombreCompleto(Usuario u) {
        return u.getNombres() + " " + u.getApellidos();
    }
}
