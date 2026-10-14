package com.medicita.service;

import com.medicita.dto.HorasLibresResponse;
import com.medicita.dto.MedicoCatalogoResponse;
import com.medicita.dto.PacienteCatalogoResponse;
import com.medicita.entity.HorarioDisponible;
import com.medicita.entity.Medico;
import com.medicita.entity.Usuario;
import com.medicita.enums.DiaSemana;
import com.medicita.enums.EstadoCita;
import com.medicita.exception.RecursoNoEncontradoException;
import com.medicita.repository.CitaRepository;
import com.medicita.repository.HorarioDisponibleRepository;
import com.medicita.repository.MedicoRepository;
import com.medicita.repository.PacienteRepository;
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
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final MedicoRepository medicoRepository;
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
                .map(m -> new MedicoCatalogoResponse(
                        m.getId(), nombre(m.getUsuario()), m.getCmp(),
                        m.getEspecialidad().getNombre(), m.getSede().getNombre(),
                        horariosPorMedico.getOrDefault(m.getId(), List.of())))
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

    private static String nombre(Usuario u) {
        return u.getNombres() + " " + u.getApellidos();
    }
}
