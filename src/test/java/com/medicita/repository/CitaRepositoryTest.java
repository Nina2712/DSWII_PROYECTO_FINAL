package com.medicita.repository;

import com.medicita.entity.Cita;
import com.medicita.entity.Medico;
import com.medicita.entity.Paciente;
import com.medicita.enums.EstadoCita;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;

import static com.medicita.repository.Datos.F1;
import static com.medicita.repository.Datos.F2;
import static com.medicita.repository.Datos.F3;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RepositoryTest
@DisplayName("CitaRepository (MySQL real)")
class CitaRepositoryTest {

    @Autowired
    private CitaRepository citaRepository;
    @Autowired
    private TestEntityManager em;

    private Paciente paciente1;
    private Paciente paciente2;
    private Medico medico1;
    private Medico medico2;

    @BeforeEach
    void prepararDatos() {
        var especialidad = Datos.especialidad(em, "Especialidad de prueba");
        var sede = Datos.sede(em);
        medico1 = Datos.medico(em, "dr1@test.pe", "CMP-T001", especialidad, sede);
        medico2 = Datos.medico(em, "dr2@test.pe", "CMP-T002", especialidad, sede);
        paciente1 = Datos.paciente(em, "p1@test.pe", "70000001");
        paciente2 = Datos.paciente(em, "p2@test.pe", "70000002");
    }

    // ------------------------------------------------------------ INSERTAR

    @Test
    @DisplayName("Insertar: guarda la cita con valores por defecto y auditoría")
    void insertar() {
        Cita nueva = Cita.builder()
                .paciente(paciente1).medico(medico1).fechaHora(F1).motivo("Dolor de cabeza").build();

        Cita guardada = citaRepository.saveAndFlush(nueva);
        em.clear();

        assertThat(guardada.getId()).isNotNull();
        Cita leida = citaRepository.findById(guardada.getId()).orElseThrow();
        assertThat(leida.getEstado()).isEqualTo(EstadoCita.PENDIENTE);
        assertThat(leida.getDuracionMin()).isEqualTo(30);
        assertThat(leida.getMotivo()).isEqualTo("Dolor de cabeza");
        assertThat(leida.getFechaHora()).isEqualTo(F1);
        assertThat(leida.getCreadoEn()).isNotNull();
        assertThat(leida.getActualizadoEn()).isNotNull();
    }

    // ---------------------------------------------------------- ACTUALIZAR

    @Test
    @DisplayName("Actualizar: reprograma, cambia estado, motivo y observaciones")
    void actualizar() {
        Cita cita = Datos.cita(em, paciente1, medico1, F1, EstadoCita.PENDIENTE);
        Long id = cita.getId();
        em.clear();

        Cita cargada = citaRepository.findById(id).orElseThrow();
        cargada.setEstado(EstadoCita.CONFIRMADA);
        cargada.setMotivo("Nuevo motivo");
        cargada.setFechaHora(F2);
        cargada.setObservaciones("Traer exámenes previos");
        citaRepository.saveAndFlush(cargada);
        em.clear();

        Cita actualizada = citaRepository.findById(id).orElseThrow();
        assertThat(actualizada.getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
        assertThat(actualizada.getMotivo()).isEqualTo("Nuevo motivo");
        assertThat(actualizada.getFechaHora()).isEqualTo(F2);
        assertThat(actualizada.getObservaciones()).isEqualTo("Traer exámenes previos");
        assertThat(actualizada.getActualizadoEn()).isAfterOrEqualTo(actualizada.getCreadoEn());
    }

    // ------------------------------------------------------------ ELIMINAR

    @Test
    @DisplayName("Eliminar: borra la cita de la base de datos")
    void eliminar() {
        Cita cita = Datos.cita(em, paciente1, medico1, F1, EstadoCita.PENDIENTE);
        Long id = cita.getId();
        long antes = citaRepository.count();

        citaRepository.deleteById(id);
        citaRepository.flush();
        em.clear();

        assertThat(citaRepository.findById(id)).isEmpty();
        assertThat(citaRepository.count()).isEqualTo(antes - 1);
    }

    // -------------------------------------------------------------- LISTAR

    @Test
    @DisplayName("Listar: findAll y búsqueda por paciente")
    void listar() {
        long base = citaRepository.count();
        Datos.cita(em, paciente1, medico1, F1, EstadoCita.PENDIENTE);
        Datos.cita(em, paciente1, medico2, F2, EstadoCita.CONFIRMADA);
        Datos.cita(em, paciente2, medico1, F3, EstadoCita.PENDIENTE);

        assertThat(citaRepository.findAll()).hasSize((int) base + 3);

        Page<Cita> deP1 = citaRepository.findByPacienteId(paciente1.getId(), PageRequest.of(0, 10));
        assertThat(deP1.getTotalElements()).isEqualTo(2);
        assertThat(deP1.getContent()).allMatch(c -> c.getPaciente().getId().equals(paciente1.getId()));
    }

    @Test
    @DisplayName("Listar: paginación (2 por página, 3 citas)")
    void listarPaginado() {
        Datos.cita(em, paciente1, medico1, F1, EstadoCita.PENDIENTE);
        Datos.cita(em, paciente1, medico1, F2, EstadoCita.PENDIENTE);
        Datos.cita(em, paciente1, medico1, F3, EstadoCita.PENDIENTE);

        Page<Cita> p0 = citaRepository.findByPacienteId(paciente1.getId(),
                PageRequest.of(0, 2, Sort.by("fechaHora")));
        Page<Cita> p1 = citaRepository.findByPacienteId(paciente1.getId(),
                PageRequest.of(1, 2, Sort.by("fechaHora")));

        assertThat(p0.getTotalElements()).isEqualTo(3);
        assertThat(p0.getTotalPages()).isEqualTo(2);
        assertThat(p0.getContent()).extracting(Cita::getFechaHora).containsExactly(F1, F2);
        assertThat(p1.getContent()).extracting(Cita::getFechaHora).containsExactly(F3);
    }

    @Test
    @DisplayName("Listar con filtros dinámicos (Specification)")
    void listarConFiltros() {
        Datos.cita(em, paciente1, medico1, F1, EstadoCita.PENDIENTE);
        Datos.cita(em, paciente1, medico2, F2, EstadoCita.CONFIRMADA);
        Datos.cita(em, paciente2, medico1, F3, EstadoCita.CANCELADA);
        PageRequest pagina = PageRequest.of(0, 10);

        Specification<Cita> porMedico = CitaSpecifications.con(null, medico1.getId(), null, null, null);
        assertThat(citaRepository.findAll(porMedico, pagina).getTotalElements()).isEqualTo(2);

        Specification<Cita> confirmadasDeP1 =
                CitaSpecifications.con(EstadoCita.CONFIRMADA, null, paciente1.getId(), null, null);
        assertThat(citaRepository.findAll(confirmadasDeP1, pagina).getContent())
                .extracting(Cita::getFechaHora).containsExactly(F2);

        LocalDate dia8 = LocalDate.of(2030, 1, 8);
        assertThat(citaRepository.findAll(
                CitaSpecifications.con(null, null, paciente1.getId(), dia8, null), pagina)
                .getTotalElements()).isEqualTo(1);
        assertThat(citaRepository.findAll(
                CitaSpecifications.con(null, null, paciente1.getId(), null, LocalDate.of(2030, 1, 7)), pagina)
                .getTotalElements()).isEqualTo(1);
        assertThat(citaRepository.findAll(
                CitaSpecifications.con(null, null, paciente1.getId(), dia8, dia8), pagina)
                .getContent()).extracting(Cita::getFechaHora).containsExactly(F2);
    }

    @Test
    @DisplayName("Consulta por médico y rango de fechas (inclusivo)")
    void citasDeUnMedicoEnUnRango() {
        Datos.cita(em, paciente1, medico1, F1, EstadoCita.PENDIENTE);
        Datos.cita(em, paciente1, medico1, F2, EstadoCita.PENDIENTE);
        Datos.cita(em, paciente1, medico1, F3, EstadoCita.PENDIENTE);

        List<Cita> citas = citaRepository.findByMedicoIdAndFechaHoraBetween(medico1.getId(), F1, F2);

        assertThat(citas).extracting(Cita::getFechaHora).containsExactlyInAnyOrder(F1, F2);
    }

    @Test
    @DisplayName("countByEstado cuenta por estado")
    void contarPorEstado() {
        long antes = citaRepository.countByEstado(EstadoCita.NO_ASISTIO);
        Datos.cita(em, paciente1, medico1, F1, EstadoCita.NO_ASISTIO);
        Datos.cita(em, paciente2, medico2, F2, EstadoCita.NO_ASISTIO);
        Datos.cita(em, paciente2, medico1, F3, EstadoCita.PENDIENTE);

        assertThat(citaRepository.countByEstado(EstadoCita.NO_ASISTIO)).isEqualTo(antes + 2);
    }

    // ----------------------------------------------- reglas de disponibilidad

    @Test
    @DisplayName("existeCitaActiva ignora las citas canceladas")
    void existeCitaActiva() {
        Cita cita = Datos.cita(em, paciente1, medico1, F1, EstadoCita.PENDIENTE);

        assertThat(citaRepository.existeCitaActiva(medico1.getId(), F1)).isTrue();
        assertThat(citaRepository.existeCitaActiva(medico1.getId(), F2)).isFalse();
        assertThat(citaRepository.existeCitaActiva(medico2.getId(), F1)).isFalse();

        cita.setEstado(EstadoCita.CANCELADA);
        citaRepository.saveAndFlush(cita);
        assertThat(citaRepository.existeCitaActiva(medico1.getId(), F1)).isFalse();
    }

    @Test
    @DisplayName("Horario del médico: excluye la propia cita al reprogramar")
    void existeCitaActivaDelMedico() {
        Cita cita = Datos.cita(em, paciente1, medico1, F1, EstadoCita.CONFIRMADA);

        assertThat(citaRepository.existeCitaActivaDelMedico(medico1.getId(), F1, -1L)).isTrue();
        assertThat(citaRepository.existeCitaActivaDelMedico(medico1.getId(), F1, cita.getId())).isFalse();
        assertThat(citaRepository.existeCitaActivaDelMedico(medico2.getId(), F1, -1L)).isFalse();
    }

    @Test
    @DisplayName("Horario del paciente: no puede tener dos citas a la vez con distintos médicos")
    void existeCitaActivaDelPaciente() {
        Cita cita = Datos.cita(em, paciente1, medico1, F1, EstadoCita.PENDIENTE);

        assertThat(citaRepository.existeCitaActivaDelPaciente(paciente1.getId(), F1, -1L)).isTrue();
        assertThat(citaRepository.existeCitaActivaDelPaciente(paciente1.getId(), F1, cita.getId())).isFalse();
        assertThat(citaRepository.existeCitaActivaDelPaciente(paciente2.getId(), F1, -1L)).isFalse();
    }

    // --------------------------------------------- restricciones de la base

    @Test
    @DisplayName("La base impide dos citas activas del mismo médico a la misma hora")
    void restriccionUnicaDeHorario() {
        Datos.cita(em, paciente1, medico1, F1, EstadoCita.PENDIENTE);
        Cita choque = Cita.builder().paciente(paciente2).medico(medico1).fechaHora(F1).build();

        assertThatThrownBy(() -> citaRepository.saveAndFlush(choque))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Una cita cancelada libera el horario para volver a reservarlo")
    void horarioReutilizableTrasCancelar() {
        Cita primera = Datos.cita(em, paciente1, medico1, F1, EstadoCita.PENDIENTE);
        primera.setEstado(EstadoCita.CANCELADA);
        citaRepository.saveAndFlush(primera);

        Cita nueva = citaRepository.saveAndFlush(
                Cita.builder().paciente(paciente2).medico(medico1).fechaHora(F1).build());

        assertThat(nueva.getId()).isNotNull();
    }

    @Test
    @DisplayName("findById carga paciente, médico, especialidad y sede en una sola consulta (sin N+1)")
    void entityGraphCargaRelaciones() {
        Cita cita = Datos.cita(em, paciente1, medico1, F1, EstadoCita.PENDIENTE);
        Long id = cita.getId();
        em.clear();

        Cita leida = citaRepository.findById(id).orElseThrow();

        assertThat(Hibernate.isInitialized(leida.getPaciente().getUsuario())).isTrue();
        assertThat(Hibernate.isInitialized(leida.getMedico().getUsuario())).isTrue();
        assertThat(Hibernate.isInitialized(leida.getMedico().getEspecialidad())).isTrue();
        assertThat(Hibernate.isInitialized(leida.getMedico().getSede())).isTrue();
    }
}
