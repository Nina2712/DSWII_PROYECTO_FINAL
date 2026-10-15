package com.medicita.service;

import com.medicita.dto.SedeDetalleResponse;
import com.medicita.dto.SedeRequest;
import com.medicita.dto.SedeResumenResponse;
import com.medicita.entity.Cita;
import com.medicita.entity.Medico;
import com.medicita.entity.Sede;
import com.medicita.enums.EstadoCita;
import com.medicita.exception.RecursoNoEncontradoException;
import com.medicita.exception.ReglaNegocioException;
import com.medicita.repository.CitaRepository;
import com.medicita.repository.MedicoRepository;
import com.medicita.repository.SedeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Sedes de la clínica: listado con sus especialidades y administración (solo ADMIN). */
@Slf4j
@Service
@RequiredArgsConstructor
public class SedeService {

    private static final int PROXIMAS = 8;

    private final SedeRepository sedeRepository;
    private final MedicoRepository medicoRepository;
    private final CitaRepository citaRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<SedeDetalleResponse> listar() {
        List<Medico> medicos = medicoRepository.listarConDetalle();
        return sedeRepository.findAll().stream()
                .sorted(Comparator.comparing(s -> s.getNombre().toLowerCase()))
                .map(s -> aRespuesta(s, medicos))
                .toList();
    }

    /** Panel de una sede: carga de hoy, citas por atender, resultados de 30 días, equipo médico y próximas citas. */
    @Transactional(readOnly = true)
    public SedeResumenResponse resumen(Long id) {
        Sede s = buscar(id);
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDate hoy = ahora.toLocalDate();
        LocalDateTime inicioHoy = hoy.atStartOfDay();
        LocalDateTime finHoy = hoy.plusDays(1).atStartOfDay();
        LocalDateTime desde30 = hoy.minusDays(29).atStartOfDay();
        LocalDateTime lejano = ahora.plusYears(5);

        Set<EstadoCita> activasHoy = Set.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA, EstadoCita.ATENDIDA);
        Set<EstadoCita> porAtender = Set.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA);

        Map<EstadoCita, Long> hoyPorEstado = contarPorEstado(id, inicioHoy, finHoy);
        Map<EstadoCita, Long> mes = contarPorEstado(id, desde30, finHoy);
        long citasHoy = activasHoy.stream().mapToLong(e -> hoyPorEstado.getOrDefault(e, 0L)).sum();
        Map<EstadoCita, Long> futuras = contarPorEstado(id, ahora, lejano);
        long pendientes = porAtender.stream().mapToLong(e -> futuras.getOrDefault(e, 0L)).sum();

        Map<Long, Long> hoyPorMedico = contarPorMedico(id, inicioHoy, finHoy, activasHoy);
        Map<Long, Long> pendientesPorMedico = contarPorMedico(id, ahora, lejano, porAtender);

        List<SedeResumenResponse.MedicoDeSede> medicos = medicoRepository.listarConDetalle().stream()
                .filter(m -> m.getSede().getId().equals(id))
                .sorted(Comparator.comparing((Medico m) -> !m.getUsuario().isActivo())
                        .thenComparing(m -> m.getUsuario().getApellidos().toLowerCase()))
                .map(m -> new SedeResumenResponse.MedicoDeSede(m.getId(),
                        m.getUsuario().getNombres() + " " + m.getUsuario().getApellidos(),
                        m.getEspecialidad().getNombre(), m.getCmp(), m.getUsuario().isActivo(),
                        hoyPorMedico.getOrDefault(m.getId(), 0L), pendientesPorMedico.getOrDefault(m.getId(), 0L)))
                .toList();

        List<SedeResumenResponse.ProximaCita> proximas = citaRepository
                .proximasDeSede(id, ahora, PageRequest.of(0, PROXIMAS)).stream()
                .map(SedeService::aProxima)
                .toList();

        return new SedeResumenResponse(s.getId(), s.getNombre(), s.getDireccion(), s.getDistrito(),
                citasHoy, pendientes,
                mes.getOrDefault(EstadoCita.ATENDIDA, 0L),
                mes.getOrDefault(EstadoCita.CANCELADA, 0L),
                mes.getOrDefault(EstadoCita.NO_ASISTIO, 0L),
                medicos, proximas);
    }

    private Map<EstadoCita, Long> contarPorEstado(Long sedeId, LocalDateTime desde, LocalDateTime hasta) {
        Map<EstadoCita, Long> r = new EnumMap<>(EstadoCita.class);
        for (Object[] fila : citaRepository.contarPorEstadoEnSede(sedeId, desde, hasta)) {
            r.put((EstadoCita) fila[0], (Long) fila[1]);
        }
        return r;
    }

    private Map<Long, Long> contarPorMedico(Long sedeId, LocalDateTime desde, LocalDateTime hasta, Set<EstadoCita> estados) {
        Map<Long, Long> r = new HashMap<>();
        for (Object[] fila : citaRepository.contarPorMedicoEnSede(sedeId, desde, hasta, estados)) {
            r.put((Long) fila[0], (Long) fila[1]);
        }
        return r;
    }

    private static SedeResumenResponse.ProximaCita aProxima(Cita c) {
        return new SedeResumenResponse.ProximaCita(c.getId(), c.getFechaHora(),
                c.getPaciente().getUsuario().getNombres() + " " + c.getPaciente().getUsuario().getApellidos(),
                c.getMedico().getUsuario().getNombres() + " " + c.getMedico().getUsuario().getApellidos(),
                c.getMedico().getEspecialidad().getNombre(), c.getEstado());
    }

    @Transactional
    public SedeDetalleResponse crear(SedeRequest request) {
        String nombre = normalizar(request.nombre());
        if (sedeRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ReglaNegocioException("Ya existe la sede \"" + nombre + "\"");
        }
        Sede s = sedeRepository.save(Sede.builder()
                .nombre(nombre)
                .direccion(request.direccion().trim())
                .distrito(request.distrito().trim())
                .build());
        log.info("Sede {} creada: {}", s.getId(), nombre);
        return aRespuesta(s, List.of());
    }

    @Transactional
    public SedeDetalleResponse actualizar(Long id, SedeRequest request) {
        Sede s = buscar(id);
        String nombre = normalizar(request.nombre());
        if (sedeRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ReglaNegocioException("Ya existe la sede \"" + nombre + "\"");
        }
        s.setNombre(nombre);
        s.setDireccion(request.direccion().trim());
        s.setDistrito(request.distrito().trim());
        s = sedeRepository.save(s);
        return aRespuesta(s, medicoRepository.listarConDetalle());
    }

    @Transactional
    public void eliminar(Long id) {
        Sede s = buscar(id);
        if (medicoRepository.existsBySedeId(id)) {
            throw new ReglaNegocioException(
                    "No se puede eliminar \"" + s.getNombre() + "\" porque tiene médicos asignados. Cámbialos de sede primero");
        }
        sedeRepository.delete(s);
        log.info("Sede {} eliminada: {}", id, s.getNombre());
    }

    private Sede buscar(Long id) {
        return sedeRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sede no encontrada: " + id));
    }

    private static String normalizar(String nombre) {
        return nombre.trim().replaceAll("\\s+", " ");
    }

    /** Médicos activos de la sede y las especialidades que ofrecen, de mayor a menor cantidad de médicos. */
    private static SedeDetalleResponse aRespuesta(Sede s, List<Medico> todos) {
        List<Medico> propios = todos.stream().filter(m -> m.getSede().getId().equals(s.getId())).toList();
        List<Medico> activos = propios.stream().filter(m -> m.getUsuario().isActivo()).toList();

        Map<Long, SedeDetalleResponse.EspecialidadEnSede> porEspecialidad = new LinkedHashMap<>();
        for (Medico m : activos) {
            porEspecialidad.merge(m.getEspecialidad().getId(),
                    new SedeDetalleResponse.EspecialidadEnSede(m.getEspecialidad().getId(), m.getEspecialidad().getNombre(), 1),
                    (a, b) -> new SedeDetalleResponse.EspecialidadEnSede(a.id(), a.nombre(), a.medicos() + 1));
        }
        List<SedeDetalleResponse.EspecialidadEnSede> especialidades = porEspecialidad.values().stream()
                .sorted(Comparator.comparingLong(SedeDetalleResponse.EspecialidadEnSede::medicos).reversed()
                        .thenComparing(SedeDetalleResponse.EspecialidadEnSede::nombre))
                .toList();
        return new SedeDetalleResponse(s.getId(), s.getNombre(), s.getDireccion(), s.getDistrito(),
                activos.size(), !propios.isEmpty(), especialidades);
    }
}
