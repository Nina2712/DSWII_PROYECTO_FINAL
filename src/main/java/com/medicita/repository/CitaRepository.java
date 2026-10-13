package com.medicita.repository;

import com.medicita.entity.Cita;
import com.medicita.enums.EstadoCita;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    Page<Cita> findByPacienteId(Long pacienteId, Pageable pageable);

    List<Cita> findByMedicoIdAndFechaHoraBetween(Long medicoId, LocalDateTime desde, LocalDateTime hasta);

    long countByEstado(EstadoCita estado);

    @Query("""
           SELECT COUNT(c) > 0 FROM Cita c
           WHERE c.medico.id = :medicoId
             AND c.fechaHora = :fechaHora
             AND c.estado NOT IN (com.medicita.enums.EstadoCita.CANCELADA,
                                  com.medicita.enums.EstadoCita.NO_ASISTIO)
           """)
    boolean existeCitaActiva(@Param("medicoId") Long medicoId, @Param("fechaHora") LocalDateTime fechaHora);
}
