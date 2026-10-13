package com.medicita.repository;

import com.medicita.entity.Medico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MedicoRepository extends JpaRepository<Medico, Long> {

    Optional<Medico> findByUsuarioId(Long usuarioId);
}
