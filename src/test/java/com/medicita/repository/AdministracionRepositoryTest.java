package com.medicita.repository;

import com.medicita.entity.Especialidad;
import com.medicita.entity.HorarioDisponible;
import com.medicita.entity.Medico;
import com.medicita.entity.Paciente;
import com.medicita.entity.Sede;
import com.medicita.enums.DiaSemana;
import com.medicita.enums.EstadoCita;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@RepositoryTest
@DisplayName("Consultas de administración: registro, médicos y especialidades (MySQL real)")
class AdministracionRepositoryTest {

    @Autowired
    private PacienteRepository pacienteRepository;
    @Autowired
    private MedicoRepository medicoRepository;
    @Autowired
    private EspecialidadRepository especialidadRepository;
    @Autowired
    private HorarioDisponibleRepository horarioRepository;
    @Autowired
    private CitaRepository citaRepository;
    @Autowired
    private SedeRepository sedeRepository;
    @Autowired
    private TestEntityManager em;

    private Especialidad especialidad;
    private Sede sede;
    private Medico medico;

    @BeforeEach
    void prepararDatos() {
        especialidad = Datos.especialidad(em, "Neurología de prueba");
        sede = Datos.sede(em);
        medico = Datos.medico(em, "dr.admin@test.pe", "CMP-A001", especialidad, sede);
    }

    @Test
    @DisplayName("Paciente: existsByDni detecta un DNI ya registrado")
    void existePorDni() {
        Datos.paciente(em, "pac.admin@test.pe", "70000201");

        assertThat(pacienteRepository.existsByDni("70000201")).isTrue();
        assertThat(pacienteRepository.existsByDni("70000299")).isFalse();
    }

    @Test
    @DisplayName("Especialidad: el nombre se compara sin distinguir mayúsculas y puede excluir al propio registro")
    void nombreDeEspecialidad() {
        assertThat(especialidadRepository.existsByNombreIgnoreCase("NEUROLOGÍA DE PRUEBA")).isTrue();
        assertThat(especialidadRepository.existsByNombreIgnoreCase("Otra cosa")).isFalse();
        assertThat(especialidadRepository.existsByNombreIgnoreCaseAndIdNot("Neurología de prueba", especialidad.getId())).isFalse();
        assertThat(especialidadRepository.existsByNombreIgnoreCaseAndIdNot("Neurología de prueba", -1L)).isTrue();
    }

    @Test
    @DisplayName("Médico: CMP repetido (sin distinguir mayúsculas) y especialidad en uso")
    void cmpYEspecialidadEnUso() {
        assertThat(medicoRepository.existsByCmpIgnoreCase("cmp-a001")).isTrue();
        assertThat(medicoRepository.existsByCmpIgnoreCaseAndIdNot("cmp-a001", medico.getId())).isFalse();
        assertThat(medicoRepository.existsByEspecialidadId(especialidad.getId())).isTrue();

        Especialidad vacia = Datos.especialidad(em, "Especialidad sin médicos");
        assertThat(medicoRepository.existsByEspecialidadId(vacia.getId())).isFalse();
    }

    @Test
    @DisplayName("Médicos por especialidad: cuenta agrupando por especialidad")
    void contarPorEspecialidad() {
        Datos.medico(em, "dr.admin2@test.pe", "CMP-A002", especialidad, sede);
        em.clear();

        var fila = medicoRepository.contarPorEspecialidad().stream()
                .filter(f -> especialidad.getId().equals(f[0])).findFirst().orElseThrow();

        assertThat((Long) fila[1]).isEqualTo(2L);
    }

    @Test
    @DisplayName("Horarios: borrarDeMedico elimina solo los horarios de ese médico")
    void borrarHorarios() {
        Medico otro = Datos.medico(em, "dr.admin3@test.pe", "CMP-A003", especialidad, sede);
        em.persistAndFlush(horario(medico, DiaSemana.LUNES));
        em.persistAndFlush(horario(medico, DiaSemana.MARTES));
        em.persistAndFlush(horario(otro, DiaSemana.LUNES));
        em.clear();

        horarioRepository.borrarDeMedico(medico.getId());
        em.clear();

        assertThat(horarioRepository.findByMedicoId(medico.getId())).isEmpty();
        assertThat(horarioRepository.findByMedicoId(otro.getId())).hasSize(1);
    }

    @Test
    @DisplayName("Citas por atender: cuenta pendientes y confirmadas futuras, no las canceladas ni las atendidas")
    void citasPorAtender() {
        Paciente paciente = Datos.paciente(em, "pac.admin2@test.pe", "70000202");
        Datos.cita(em, paciente, medico, Datos.F1, EstadoCita.PENDIENTE);
        Datos.cita(em, paciente, medico, Datos.F2, EstadoCita.CONFIRMADA);
        Datos.cita(em, paciente, medico, Datos.F3, EstadoCita.CANCELADA);
        Datos.cita(em, paciente, medico, Datos.F3.plusHours(1), EstadoCita.ATENDIDA);
        em.clear();

        assertThat(citaRepository.contarPorAtenderDelMedico(medico.getId(), LocalDateTime.of(2030, 1, 1, 0, 0))).isEqualTo(2);
        // Desde después de la primera cita ya solo queda una por atender
        assertThat(citaRepository.contarPorAtenderDelMedico(medico.getId(), Datos.F1.plusMinutes(1))).isEqualTo(1);
    }

    private static HorarioDisponible horario(Medico m, DiaSemana dia) {
        return HorarioDisponible.builder().medico(m).diaSemana(dia)
                .horaInicio(LocalTime.of(8, 0)).horaFin(LocalTime.of(12, 0)).build();
    }

    @Test
    @DisplayName("Sede: el nombre se compara sin distinguir mayúsculas y se sabe si tiene médicos")
    void sedeEnUso() {
        assertThat(sedeRepository.existsByNombreIgnoreCase("SEDE DE PRUEBA")).isTrue();
        assertThat(sedeRepository.existsByNombreIgnoreCase("Sede inexistente")).isFalse();
        assertThat(sedeRepository.existsByNombreIgnoreCaseAndIdNot("Sede de prueba", sede.getId())).isFalse();
        assertThat(sedeRepository.existsByNombreIgnoreCaseAndIdNot("Sede de prueba", -1L)).isTrue();

        assertThat(medicoRepository.existsBySedeId(sede.getId())).isTrue();
        Sede vacia = em.persistAndFlush(Sede.builder().nombre("Sede vacía").direccion("Calle 1").distrito("Surco").build());
        assertThat(medicoRepository.existsBySedeId(vacia.getId())).isFalse();
    }

    @Test
    @DisplayName("Citas por atender del médico: con paciente, ordenadas y sin las canceladas ni las pasadas")
    void listarPorAtender() {
        Paciente paciente = Datos.paciente(em, "pac.admin3@test.pe", "70000203");
        Datos.cita(em, paciente, medico, Datos.F2, EstadoCita.CONFIRMADA);
        Datos.cita(em, paciente, medico, Datos.F1, EstadoCita.PENDIENTE);
        Datos.cita(em, paciente, medico, Datos.F3, EstadoCita.CANCELADA);
        em.clear();

        var citas = citaRepository.listarPorAtenderDelMedico(medico.getId(), LocalDateTime.of(2030, 1, 1, 0, 0));

        assertThat(citas).extracting(c -> c.getFechaHora()).containsExactly(Datos.F1, Datos.F2);
        assertThat(citas.get(0).getPaciente().getUsuario().getNombres()).isEqualTo("Nombre"); // ya cargado, sin consultas extra
    }

    @Test
    @DisplayName("Sede: cuenta las citas por estado y por médico solo de esa sede")
    void conteosPorSede() {
        Paciente paciente = Datos.paciente(em, "pac.sede@test.pe", "70000203");
        Sede otraSede = em.persistAndFlush(Sede.builder().nombre("Otra sede").direccion("Calle 2").distrito("Lince").build());
        Medico deOtraSede = Datos.medico(em, "dr.otra@test.pe", "CMP-A009", especialidad, otraSede);
        Datos.cita(em, paciente, medico, Datos.F1, EstadoCita.PENDIENTE);
        Datos.cita(em, paciente, medico, Datos.F2, EstadoCita.ATENDIDA);
        Datos.cita(em, paciente, medico, Datos.F3, EstadoCita.CANCELADA);
        Datos.cita(em, paciente, deOtraSede, Datos.F1, EstadoCita.PENDIENTE);
        em.clear();

        LocalDateTime desde = LocalDateTime.of(2030, 1, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2030, 2, 1, 0, 0);

        var porEstado = citaRepository.contarPorEstadoEnSede(sede.getId(), desde, hasta);
        assertThat(porEstado).hasSize(3);
        assertThat(porEstado).anySatisfy(f -> {
            assertThat(f[0]).isEqualTo(EstadoCita.PENDIENTE);
            assertThat(f[1]).isEqualTo(1L);
        });

        var porMedico = citaRepository.contarPorMedicoEnSede(sede.getId(), desde, hasta,
                java.util.List.of(EstadoCita.PENDIENTE, EstadoCita.ATENDIDA));
        assertThat(porMedico).hasSize(1);
        assertThat(porMedico.get(0)[0]).isEqualTo(medico.getId());
        assertThat(porMedico.get(0)[1]).isEqualTo(2L);
    }

    @Test
    @DisplayName("Sede: las próximas citas son solo pendientes o confirmadas, ordenadas y limitadas")
    void proximasDeSede() {
        Paciente paciente = Datos.paciente(em, "pac.prox@test.pe", "70000204");
        Datos.cita(em, paciente, medico, Datos.F2, EstadoCita.CONFIRMADA);
        Datos.cita(em, paciente, medico, Datos.F1, EstadoCita.PENDIENTE);
        Datos.cita(em, paciente, medico, Datos.F3, EstadoCita.CANCELADA);
        em.clear();

        var proximas = citaRepository.proximasDeSede(sede.getId(), LocalDateTime.of(2030, 1, 1, 0, 0),
                org.springframework.data.domain.PageRequest.of(0, 5));

        assertThat(proximas).extracting(c -> c.getFechaHora()).containsExactly(Datos.F1, Datos.F2);
        assertThat(citaRepository.proximasDeSede(sede.getId(), LocalDateTime.of(2030, 1, 1, 0, 0),
                org.springframework.data.domain.PageRequest.of(0, 1))).hasSize(1);
    }

    @Test
    @DisplayName("Ficha y perfil: se guardan los datos clínicos del paciente y la presentación del médico")
    void fichaYPerfil() {
        Paciente paciente = Datos.paciente(em, "pac.ficha@test.pe", "70000205");
        paciente.setDireccion("Av. Lima 123");
        paciente.setTipoSeguro(com.medicita.enums.TipoSeguro.ESSALUD);
        paciente.setAlergias("Penicilina");
        paciente.setContactoEmergenciaNombre("Luis Torres");
        paciente.setContactoEmergenciaTelefono("999888777");
        em.persistAndFlush(paciente);
        medico.setTelefono("987000111");
        medico.setDescripcion("Cardiólogo con 10 años de experiencia");
        em.persistAndFlush(medico);
        em.clear();

        Paciente leido = pacienteRepository.findById(paciente.getId()).orElseThrow();
        assertThat(leido.getTipoSeguro()).isEqualTo(com.medicita.enums.TipoSeguro.ESSALUD);
        assertThat(leido.getAlergias()).isEqualTo("Penicilina");
        assertThat(leido.getContactoEmergenciaTelefono()).isEqualTo("999888777");
        assertThat(medicoRepository.findById(medico.getId()).orElseThrow().getDescripcion()).contains("Cardiólogo");
    }

    @Test
    @DisplayName("Ficha: un médico solo ve a pacientes con los que tiene o tuvo citas")
    void medicoConCitasDelPaciente() {
        Paciente suyo = Datos.paciente(em, "pac.suyo@test.pe", "70000206");
        Paciente ajeno = Datos.paciente(em, "pac.ajeno@test.pe", "70000207");
        Datos.cita(em, suyo, medico, Datos.F1, EstadoCita.ATENDIDA);
        em.clear();

        assertThat(citaRepository.existsByMedicoIdAndPacienteId(medico.getId(), suyo.getId())).isTrue();
        assertThat(citaRepository.existsByMedicoIdAndPacienteId(medico.getId(), ajeno.getId())).isFalse();
    }

    @Test
    @DisplayName("Historial clínico: solo atenciones realizadas, de varios médicos, de la más reciente a la más antigua")
    void historialDelPaciente() {
        Paciente paciente = Datos.paciente(em, "pac.hist@test.pe", "70000208");
        Especialidad otra = Datos.especialidad(em, "Dermatología de prueba");
        Medico otroMedico = Datos.medico(em, "dr.hist2@test.pe", "CMP-A010", otra, sede);
        Datos.cita(em, paciente, medico, Datos.F1, EstadoCita.ATENDIDA);
        Datos.cita(em, paciente, otroMedico, Datos.F2, EstadoCita.ATENDIDA);
        Datos.cita(em, paciente, medico, Datos.F3, EstadoCita.CONFIRMADA);
        Datos.cita(em, paciente, medico, Datos.F3.plusHours(2), EstadoCita.CANCELADA);
        em.clear();

        var h = citaRepository.historialDelPaciente(paciente.getId(), org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(h).extracting(c -> c.getFechaHora()).containsExactly(Datos.F2, Datos.F1);
        assertThat(h.get(0).getMedico().getEspecialidad().getNombre()).isEqualTo("Dermatología de prueba");
    }
}
