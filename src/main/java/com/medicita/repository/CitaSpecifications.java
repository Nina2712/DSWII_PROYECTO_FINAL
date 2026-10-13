package com.medicita.repository;

import com.medicita.entity.Cita;
import com.medicita.enums.EstadoCita;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Construye filtros dinámicos: solo se aplican los parámetros que no son nulos. */
public final class CitaSpecifications {

    private CitaSpecifications() {
    }

    public static Specification<Cita> con(EstadoCita estado, Long medicoId, Long pacienteId,
                                          LocalDate desde, LocalDate hasta) {
        List<Specification<Cita>> filtros = new ArrayList<>();
        if (estado != null) {
            filtros.add((root, query, cb) -> cb.equal(root.get("estado"), estado));
        }
        if (medicoId != null) {
            filtros.add((root, query, cb) -> cb.equal(root.get("medico").get("id"), medicoId));
        }
        if (pacienteId != null) {
            filtros.add((root, query, cb) -> cb.equal(root.get("paciente").get("id"), pacienteId));
        }
        if (desde != null) {
            LocalDateTime inicio = desde.atStartOfDay();
            filtros.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.<LocalDateTime>get("fechaHora"), inicio));
        }
        if (hasta != null) {
            LocalDateTime fin = hasta.plusDays(1).atStartOfDay();
            filtros.add((root, query, cb) -> cb.lessThan(root.<LocalDateTime>get("fechaHora"), fin));
        }
        return Specification.allOf(filtros);
    }
}
