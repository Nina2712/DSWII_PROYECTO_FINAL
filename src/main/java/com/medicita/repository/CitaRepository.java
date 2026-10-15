package com.medicita.repository;

import com.medicita.entity.Cita;
import com.medicita.enums.EstadoCita;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CitaRepository extends JpaRepository<Cita, Long>, JpaSpecificationExecutor<Cita> {

    /** Carga paciente, médico, especialidad y sede en una sola consulta (evita el problema N+1). */
    @Override
    @EntityGraph(attributePaths = {"paciente.usuario", "medico.usuario", "medico.especialidad", "medico.sede"})
    Page<Cita> findAll(Specification<Cita> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"paciente.usuario", "medico.usuario", "medico.especialidad", "medico.sede"})
    Optional<Cita> findById(Long id);

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

    /** ¿El médico ya tiene otra cita activa a esa hora? (excluirId = cita que se está reprogramando, o -1). */
    @Query("""
           SELECT COUNT(c) > 0 FROM Cita c
           WHERE c.medico.id = :medicoId
             AND c.fechaHora = :fechaHora
             AND c.id <> :excluirId
             AND c.estado NOT IN (com.medicita.enums.EstadoCita.CANCELADA,
                                  com.medicita.enums.EstadoCita.NO_ASISTIO)
           """)
    boolean existeCitaActivaDelMedico(@Param("medicoId") Long medicoId,
                                      @Param("fechaHora") LocalDateTime fechaHora,
                                      @Param("excluirId") Long excluirId);

    /** ¿El paciente ya tiene otra cita activa a esa hora (con cualquier médico)? */
    @Query("""
           SELECT COUNT(c) > 0 FROM Cita c
           WHERE c.paciente.id = :pacienteId
             AND c.fechaHora = :fechaHora
             AND c.id <> :excluirId
             AND c.estado NOT IN (com.medicita.enums.EstadoCita.CANCELADA,
                                  com.medicita.enums.EstadoCita.NO_ASISTIO)
           """)
    boolean existeCitaActivaDelPaciente(@Param("pacienteId") Long pacienteId,
                                        @Param("fechaHora") LocalDateTime fechaHora,
                                        @Param("excluirId") Long excluirId);

    /** Citas por atender (pendientes o confirmadas) de un médico desde un momento dado. */
    @Query("""
           SELECT COUNT(c) FROM Cita c
           WHERE c.medico.id = :medicoId
             AND c.fechaHora >= :desde
             AND c.estado IN (com.medicita.enums.EstadoCita.PENDIENTE,
                              com.medicita.enums.EstadoCita.CONFIRMADA)
           """)
    long contarPorAtenderDelMedico(@Param("medicoId") Long medicoId, @Param("desde") LocalDateTime desde);

    /** Citas por atender de un médico desde un momento dado, con paciente, de la más próxima a la más lejana. */
    @Query("""
           SELECT c FROM Cita c
           JOIN FETCH c.paciente p
           JOIN FETCH p.usuario
           WHERE c.medico.id = :medicoId
             AND c.fechaHora >= :desde
             AND c.estado IN (com.medicita.enums.EstadoCita.PENDIENTE,
                              com.medicita.enums.EstadoCita.CONFIRMADA)
           ORDER BY c.fechaHora
           """)
    List<Cita> listarPorAtenderDelMedico(@Param("medicoId") Long medicoId, @Param("desde") LocalDateTime desde);
}
