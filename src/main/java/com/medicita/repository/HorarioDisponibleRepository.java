package com.medicita.repository;

import com.medicita.entity.HorarioDisponible;
import com.medicita.enums.DiaSemana;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HorarioDisponibleRepository extends JpaRepository<HorarioDisponible, Long> {

    List<HorarioDisponible> findByMedicoIdAndDiaSemana(Long medicoId, DiaSemana diaSemana);

    List<HorarioDisponible> findByMedicoId(Long medicoId);

    @Modifying
    @Query("DELETE FROM HorarioDisponible h WHERE h.medico.id = :medicoId")
    void borrarDeMedico(@Param("medicoId") Long medicoId);
}
