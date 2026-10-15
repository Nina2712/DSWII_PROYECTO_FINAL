package com.medicita.repository;

import com.medicita.entity.Medico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MedicoRepository extends JpaRepository<Medico, Long> {

    Optional<Medico> findByUsuarioId(Long usuarioId);

    /** Médicos con usuario, especialidad y sede en una sola consulta, ordenados por apellido. */
    @Query("""
           SELECT m FROM Medico m
           JOIN FETCH m.usuario u
           JOIN FETCH m.especialidad
           JOIN FETCH m.sede
           ORDER BY u.apellidos, u.nombres
           """)
    List<Medico> listarConDetalle();

    boolean existsByCmpIgnoreCase(String cmp);

    boolean existsByCmpIgnoreCaseAndIdNot(String cmp, Long id);

    boolean existsByEspecialidadId(Long especialidadId);

    /** Cantidad de médicos por especialidad: pares [especialidadId, total]. */
    @Query("SELECT m.especialidad.id, COUNT(m) FROM Medico m GROUP BY m.especialidad.id")
    List<Object[]> contarPorEspecialidad();

    boolean existsBySedeId(Long sedeId);
}
