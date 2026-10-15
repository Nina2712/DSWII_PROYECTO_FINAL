package com.medicita.repository;

import com.medicita.entity.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    Optional<Paciente> findByUsuarioId(Long usuarioId);

    /** Pacientes con su usuario en una sola consulta, ordenados por apellido. */
    @Query("""
           SELECT p FROM Paciente p
           JOIN FETCH p.usuario u
           ORDER BY u.apellidos, u.nombres
           """)
    List<Paciente> listarConUsuario();

    boolean existsByDni(String dni);
}
