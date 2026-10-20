package com.medicita.service;

import com.medicita.dto.DisponibilidadMedicoResponse;
import com.medicita.dto.HorasLibresResponse;
import com.medicita.dto.MedicoCatalogoResponse;
import com.medicita.dto.PacienteCatalogoResponse;
import com.medicita.dto.SedeResponse;
import com.medicita.entity.HorarioDisponible;
import com.medicita.entity.Medico;
import com.medicita.entity.Sede;
import com.medicita.entity.Usuario;
import com.medicita.enums.DiaSemana;
import com.medicita.enums.EstadoCita;
import com.medicita.exception.RecursoNoEncontradoException;
import com.medicita.repository.CitaRepository;
import com.medicita.repository.HorarioDisponibleRepository;
import com.medicita.repository.MedicoRepository;
import com.medicita.repository.PacienteRepository;
import com.medicita.repository.SedeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Listas de apoyo para la interfaz: médicos, pacientes y horas libres para reservar. */
@Service
@RequiredArgsConstructor
public class CatalogoService {

    private static final int DURACION_MIN = 30;
    private static final int VENTANA_DIAS = 60;   // hasta dónde se busca la "próxima fecha libre"
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final MedicoRepository medicoRepository;
    private final SedeRepository sedeRepository;
    private final PacienteRepository pacienteRepository;
    private final HorarioDisponibleRepository horarioRepository;
    private final CitaRepository citaRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<MedicoCatalogoResponse> medicos() {
        Map<Long, List<MedicoCatalogoResponse.Horario>> horariosPorMedico = new HashMap<>();
        List<HorarioDisponible> todos = new ArrayList<>(horarioRepository.findAll());
        todos.sort(Comparator.comparing(HorarioDisponible::getDiaSemana).thenComparing(HorarioDisponible::getHoraInicio));
        for (HorarioDisponible h : todos) {
            horariosPorMedico.computeIfAbsent(h.getMedico().getId(), k -> new ArrayList<>())
                    .add(new MedicoCatalogoResponse.Horario(
                            h.getDiaSemana(), h.getHoraInicio().format(HORA), h.getHoraFin().format(HORA)));
        }
        return medicoRepository.listarConDetalle().stream()
                .filter(m -> m.getUsuario().isActivo())   // los médicos desactivados no reciben citas nuevas
                .map(m -> new MedicoCatalogoResponse(
                        m.getId(), nombre(m.getUsuario()), m.getCmp(),
                        m.getEspecialidad().getId(), m.getEspecialidad().getNombre(), m.getSede().getNombre(),
                        horariosPorMedico.getOrDefault(m.getId(), List.of()),
                        m.getDescripcion(), m.getUsuario().getFoto()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SedeResponse> sedes() {
        return sedeRepository.findAll().stream()
                .sorted(Comparator.comparing(Sede::getNombre))
                .map(s -> new SedeResponse(s.getId(), s.getNombre(), s.getDireccion(), s.getDistrito()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PacienteCatalogoResponse> pacientes() {
        return pacienteRepository.listarConUsuario().stream()
                .map(p -> new PacienteCatalogoResponse(p.getId(), nombre(p.getUsuario()), p.getDni()))
                .toList();
    }

    @Transactional(readOnly = true)
    public HorasLibresResponse horasLibres(Long medicoId, LocalDate fecha) {
        Medico medico = medicoRepository.findById(medicoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Médico no encontrado: " + medicoId));

        if (!medico.getUsuario().isActivo()) {   // un médico desactivado no recibe citas nuevas
            return new HorasLibresResponse(medico.getId(), fecha, List.of());
        }

        DiaSemana dia = DiaSemana.values()[fecha.getDayOfWeek().getValue() - 1]; // lunes = 1
        List<Agenda.Tramo> tramos = horarioRepository.findByMedicoIdAndDiaSemana(medico.getId(), dia).stream()
                .map(h -> new Agenda.Tramo(h.getHoraInicio(), h.getHoraFin()))
                .toList();

        Set<LocalTime> ocupadas = new HashSet<>();
        citaRepository.findByMedicoIdAndFechaHoraBetween(medico.getId(), fecha.atStartOfDay(), fecha.atTime(23, 59, 59))
                .stream()
                .filter(c -> c.getEstado() != EstadoCita.CANCELADA && c.getEstado() != EstadoCita.NO_ASISTIO)
                .forEach(c -> ocupadas.add(c.getFechaHora().toLocalTime()));

        List<String> horas = Agenda.huecosLibres(tramos, ocupadas, fecha, LocalDateTime.now(clock), DURACION_MIN).stream()
                .map(h -> h.format(HORA))
                .toList();
        return new HorasLibresResponse(medico.getId(), fecha, horas);
    }

    /** Médicos activos de una especialidad con sus horas libres en la fecha y, si no hay, la próxima fecha libre. */
    @Transactional(readOnly = true)
    public List<DisponibilidadMedicoResponse> disponibilidad(Long especialidadId, LocalDate fecha) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        return medicoRepository.listarConDetalle().stream()
                .filter(m -> m.getEspecialidad().getId().equals(especialidadId) && m.getUsuario().isActivo())
                .map(m -> disponibilidadDe(m, fecha, ahora))
                .toList();
    }

    private DisponibilidadMedicoResponse disponibilidadDe(Medico medico, LocalDate fecha, LocalDateTime ahora) {
        Map<DiaSemana, List<Agenda.Tramo>> tramos = new EnumMap<>(DiaSemana.class);
        for (HorarioDisponible h : horarioRepository.findByMedicoId(medico.getId())) {
            tramos.computeIfAbsent(h.getDiaSemana(), k -> new ArrayList<>())
                    .add(new Agenda.Tramo(h.getHoraInicio(), h.getHoraFin()));
        }

        // una sola consulta de citas para toda la ventana (la fecha elegida y los días siguientes)
        Map<LocalDate, Set<LocalTime>> ocupadas = new HashMap<>();
        citaRepository.findByMedicoIdAndFechaHoraBetween(
                        medico.getId(), fecha.atStartOfDay(), fecha.plusDays(VENTANA_DIAS).atTime(23, 59, 59)).stream()
                .filter(c -> c.getEstado() != EstadoCita.CANCELADA && c.getEstado() != EstadoCita.NO_ASISTIO)
                .forEach(c -> ocupadas.computeIfAbsent(c.getFechaHora().toLocalDate(), k -> new HashSet<>())
                        .add(c.getFechaHora().toLocalTime()));

        List<String> horas = Agenda.huecosLibres(
                        tramos.getOrDefault(Agenda.diaDe(fecha), List.of()),
                        ocupadas.getOrDefault(fecha, Set.of()), fecha, ahora, DURACION_MIN).stream()
                .map(h -> h.format(HORA))
                .toList();
        LocalDate proxima = horas.isEmpty()
                ? Agenda.proximoDiaConHueco(tramos, ocupadas, fecha.plusDays(1), ahora, DURACION_MIN, VENTANA_DIAS).orElse(null)
                : null;
        return new DisponibilidadMedicoResponse(medico.getId(), fecha, horas, proxima);
    }

    private static String nombre(Usuario u) {
        return u.getNombres() + " " + u.getApellidos();
    }
}
