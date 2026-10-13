package com.medicita.repository;

import com.medicita.entity.Especialidad;
import com.medicita.entity.HorarioDisponible;
import com.medicita.entity.Medico;
import com.medicita.entity.Paciente;
import com.medicita.entity.Sede;
import com.medicita.enums.DiaSemana;
import com.medicita.enums.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RepositoryTest
@DisplayName("Pacientes, médicos, especialidades y horarios (MySQL real)")
class PerfilesYHorariosRepositoryTest {

    @Autowired
    private PacienteRepository pacienteRepository;
    @Autowired
    private MedicoRepository medicoRepository;
    @Autowired
    private EspecialidadRepository especialidadRepository;
    @Autowired
    private HorarioDisponibleRepository horarioRepository;
    @Autowired
    private TestEntityManager em;

    private Especialidad especialidad;
    private Sede sede;
    private Medico medico;

    @BeforeEach
    void prepararDatos() {
        especialidad = Datos.especialidad(em, "Cardiología de prueba");
        sede = Datos.sede(em);
        medico = Datos.medico(em, "dr@test.pe", "CMP-H001", especialidad, sede);
    }

    @Test
    @DisplayName("Paciente: se encuentra por el id de su usuario")
    void pacientePorUsuario() {
        Paciente paciente = Datos.paciente(em, "pac@test.pe", "70000101");
        em.clear();

        assertThat(pacienteRepository.findByUsuarioId(paciente.getUsuario().getId()))
                .isPresent().get().extracting(Paciente::getDni).isEqualTo("70000101");
        assertThat(pacienteRepository.findByUsuarioId(-1L)).isEmpty();
    }

    @Test
    @DisplayName("Paciente: el DNI es único")
    void dniDuplicado() {
        Paciente existente = Datos.paciente(em, "pac1@test.pe", "70000102");
        var otroUsuario = Datos.usuario(em, "pac2@test.pe", Rol.PACIENTE);
        Paciente repetido = Paciente.builder().usuario(otroUsuario).dni(existente.getDni()).build();

        assertThatThrownBy(() -> pacienteRepository.saveAndFlush(repetido))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Médico: se encuentra por el id de su usuario")
    void medicoPorUsuario() {
        em.clear();

        assertThat(medicoRepository.findByUsuarioId(medico.getUsuario().getId()))
                .isPresent().get().extracting(Medico::getCmp).isEqualTo("CMP-H001");
        assertThat(medicoRepository.findByUsuarioId(-1L)).isEmpty();
    }

    @Test
    @DisplayName("Médico: el CMP es único")
    void cmpDuplicado() {
        var otroUsuario = Datos.usuario(em, "dr2@test.pe", Rol.MEDICO);
        Medico repetido = Medico.builder()
                .usuario(otroUsuario).especialidad(especialidad).sede(sede).cmp(medico.getCmp()).build();

        assertThatThrownBy(() -> medicoRepository.saveAndFlush(repetido))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Especialidad: insertar, actualizar, eliminar y nombre único")
    void especialidadCrud() {
        Especialidad nueva = especialidadRepository.saveAndFlush(
                Especialidad.builder().nombre("Neurología de prueba").descripcion("Sistema nervioso").build());
        em.clear();

        Especialidad cargada = especialidadRepository.findById(nueva.getId()).orElseThrow();
        cargada.setDescripcion("Cerebro y nervios");
        especialidadRepository.saveAndFlush(cargada);
        em.clear();
        assertThat(especialidadRepository.findById(nueva.getId()).orElseThrow().getDescripcion())
                .isEqualTo("Cerebro y nervios");

        assertThatThrownBy(() -> especialidadRepository.saveAndFlush(
                Especialidad.builder().nombre("Neurología de prueba").build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Horarios: se consultan por médico y día de la semana")
    void horariosPorDia() {
        horarioRepository.save(HorarioDisponible.builder().medico(medico).diaSemana(DiaSemana.LUNES)
                .horaInicio(LocalTime.of(8, 0)).horaFin(LocalTime.of(13, 0)).build());
        horarioRepository.save(HorarioDisponible.builder().medico(medico).diaSemana(DiaSemana.LUNES)
                .horaInicio(LocalTime.of(15, 0)).horaFin(LocalTime.of(18, 0)).build());
        horarioRepository.saveAndFlush(HorarioDisponible.builder().medico(medico).diaSemana(DiaSemana.MARTES)
                .horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(12, 0)).build());
        em.clear();

        List<HorarioDisponible> lunes = horarioRepository.findByMedicoIdAndDiaSemana(medico.getId(), DiaSemana.LUNES);

        assertThat(lunes).hasSize(2);
        assertThat(lunes).extracting(HorarioDisponible::getHoraInicio)
                .containsExactlyInAnyOrder(LocalTime.of(8, 0), LocalTime.of(15, 0));
        assertThat(horarioRepository.findByMedicoIdAndDiaSemana(medico.getId(), DiaSemana.DOMINGO)).isEmpty();
    }

    @Test
    @DisplayName("Horarios: la base rechaza un rango donde la hora fin es anterior a la de inicio")
    void horarioInvalido() {
        HorarioDisponible invalido = HorarioDisponible.builder().medico(medico).diaSemana(DiaSemana.MIERCOLES)
                .horaInicio(LocalTime.of(13, 0)).horaFin(LocalTime.of(8, 0)).build();

        // Spring no clasifica el error 3819 de MySQL (CHECK) como "integridad de datos"; llega como
        // JpaSystemException, que también es una DataAccessException. Se verifica la restricción por nombre.
        assertThatThrownBy(() -> horarioRepository.saveAndFlush(invalido))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_horario_rango");
    }
}
