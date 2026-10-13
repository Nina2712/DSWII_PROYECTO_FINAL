package com.medicita.repository;

import com.medicita.entity.HorarioDisponible;
import com.medicita.enums.DiaSemana;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HorarioDisponibleRepository extends JpaRepository<HorarioDisponible, Long> {

    List<HorarioDisponible> findByMedicoIdAndDiaSemana(Long medicoId, DiaSemana diaSemana);
}
