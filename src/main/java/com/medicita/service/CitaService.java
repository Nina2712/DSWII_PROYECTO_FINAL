package com.medicita.service;

import com.medicita.dto.CitaCreateRequest;
import com.medicita.dto.CitaFiltro;
import com.medicita.dto.CitaResponse;
import com.medicita.dto.CitaUpdateRequest;
import com.medicita.dto.PageResponse;
import com.medicita.entity.Cita;
import com.medicita.entity.HorarioDisponible;
import com.medicita.entity.Medico;
import com.medicita.entity.Paciente;
import com.medicita.enums.DiaSemana;
import com.medicita.enums.EstadoCita;
import com.medicita.enums.Rol;
import com.medicita.exception.RecursoNoEncontradoException;
import com.medicita.exception.ReglaNegocioException;
import com.medicita.exception.SolicitudInvalidaException;
import com.medicita.repository.CitaRepository;
import com.medicita.repository.CitaSpecifications;
import com.medicita.repository.HorarioDisponibleRepository;
import com.medicita.repository.MedicoRepository;
import com.medicita.repository.PacienteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Lógica de negocio de las citas. Reglas principales:
 * <ul>
 *   <li>Bloques de 30 minutos, dentro del horario del médico y en el futuro.</li>
 *   <li>Un médico (o un paciente) no puede tener dos citas activas a la misma hora.</li>
 *   <li>El paciente solo modifica o cancela hasta 2 horas antes y solo ve sus propias citas.</li>
 *   <li>Flujo de estados: PENDIENTE → CONFIRMADA → ATENDIDA / NO_ASISTIO, y CANCELADA.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CitaService {

    private static final int DURACION_MIN = 30;
    private static final int HORAS_MIN_ANTELACION = 2;
    private static final int TAMANIO_MAX_PAGINA = 50;

    private static final Set<EstadoCita> FINALES =
            EnumSet.of(EstadoCita.ATENDIDA, EstadoCita.CANCELADA, EstadoCita.NO_ASISTIO);

    private static final Map<EstadoCita, Set<EstadoCita>> TRANSICIONES = Map.of(
            EstadoCita.PENDIENTE, EnumSet.of(EstadoCita.CONFIRMADA, EstadoCita.CANCELADA),
            EstadoCita.CONFIRMADA, EnumSet.of(EstadoCita.ATENDIDA, EstadoCita.NO_ASISTIO, EstadoCita.CANCELADA));

    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final HorarioDisponibleRepository horarioRepository;
    private final ContextoUsuario contexto;
    private final Clock clock;

    // ------------------------------------------------------------------ GET

    @Transactional(readOnly = true)
    public PageResponse<CitaResponse> listar(CitaFiltro filtro, int page, int size, boolean ascendente) {
        Actor actor = contexto.actual();

        // El alcance lo decide el rol, no el cliente: se ignoran los filtros que intenten salirse de él
        Long pacienteId = filtro.pacienteId();
        Long medicoId = filtro.medicoId();
        if (actor.rol() == Rol.PACIENTE) {
            pacienteId = requerirPacienteId(actor);
        } else if (actor.rol() == Rol.MEDICO) {
            medicoId = requerirMedicoId(actor);
        }

        Specification<Cita> spec = CitaSpecifications.con(
                filtro.estado(), medicoId, pacienteId, filtro.desde(), filtro.hasta());
        Sort orden = Sort.by(ascendente ? Sort.Direction.ASC : Sort.Direction.DESC, "fechaHora")
                .and(Sort.by("id"));
        Pageable pageable = PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), TAMANIO_MAX_PAGINA), orden);

        return PageResponse.of(citaRepository.findAll(spec, pageable).map(CitaMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public CitaResponse obtener(Long id) {
        return CitaMapper.toResponse(obtenerAccesible(id, contexto.actual()));
    }

    // ----------------------------------------------------------------- POST

    @Transactional
    public CitaResponse crear(CitaCreateRequest request) {
        Actor actor = contexto.actual();
        Paciente paciente = resolverPaciente(actor, request.pacienteId());
        Medico medico = medicoRepository.findById(request.medicoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Médico no encontrado: " + request.medicoId()));
        if (!medico.getUsuario().isActivo()) {
            throw new ReglaNegocioException("El médico no está disponible para nuevas citas");
        }

        validarFechaYDisponibilidad(medico, paciente, request.fechaHora(), null);

        Cita cita = Cita.builder()
                .paciente(paciente)
                .medico(medico)
                .fechaHora(request.fechaHora())
                .duracionMin(DURACION_MIN)
                .motivo(limpiar(request.motivo()))
                .build();
        cita = citaRepository.saveAndFlush(cita);

        log.info("Cita {} creada por {} ({}) para el paciente {} con el médico {}",
                cita.getId(), actor.email(), actor.rol(), paciente.getId(), medico.getId());
        return CitaMapper.toResponse(cita);
    }

    // ------------------------------------------------------------------ PUT

    @Transactional
    public CitaResponse actualizar(Long id, CitaUpdateRequest request) {
        Actor actor = contexto.actual();
        Cita cita = obtenerAccesible(id, actor);

        if (request.sinCambios()) {
            throw new SolicitudInvalidaException("Envía al menos un campo para actualizar");
        }
        validarPermisosDeEdicion(actor, request);

        boolean soloObservaciones = request.fechaHora() == null && request.motivo() == null && request.estado() == null;
        boolean anotacionClinica = cita.getEstado() == EstadoCita.ATENDIDA && soloObservaciones
                && (actor.rol() == Rol.MEDICO || actor.rol() == Rol.ADMIN);
        if (FINALES.contains(cita.getEstado()) && !anotacionClinica) {
            throw new ReglaNegocioException("La cita está " + cita.getEstado() + " y ya no se puede modificar");
        }

        if (request.fechaHora() != null && !request.fechaHora().equals(cita.getFechaHora())) {
            if (actor.rol() == Rol.PACIENTE) {
                validarAntelacion(cita);
            }
            validarFechaYDisponibilidad(cita.getMedico(), cita.getPaciente(), request.fechaHora(), cita.getId());
            cita.setFechaHora(request.fechaHora());
            if (actor.rol() == Rol.PACIENTE) {
                cita.setEstado(EstadoCita.PENDIENTE); // reprogramar obliga a confirmar de nuevo
            }
        }
        if (request.motivo() != null) {
            cita.setMotivo(limpiar(request.motivo()));
        }
        if (request.observaciones() != null) {
            cita.setObservaciones(limpiar(request.observaciones()));
        }
        if (request.estado() != null) {
            cambiarEstado(cita, request.estado(), actor);
        }

        cita = citaRepository.saveAndFlush(cita);
        log.info("Cita {} actualizada por {} ({})", cita.getId(), actor.email(), actor.rol());
        return CitaMapper.toResponse(cita);
    }

    // --------------------------------------------------------------- DELETE

    /**
     * Por defecto cancela la cita (borrado lógico, conserva el historial).
     * Con {@code definitivo=true} la elimina de la base de datos (solo ADMIN).
     */
    @Transactional
    public void eliminar(Long id, boolean definitivo) {
        Actor actor = contexto.actual();
        Cita cita = obtenerAccesible(id, actor);

        if (definitivo) {
            if (actor.rol() != Rol.ADMIN) {
                throw new AccessDeniedException("Solo el administrador puede eliminar definitivamente una cita");
            }
            citaRepository.delete(cita);
            log.info("Cita {} eliminada definitivamente por {}", id, actor.email());
            return;
        }

        if (FINALES.contains(cita.getEstado())) {
            throw new ReglaNegocioException("La cita ya está " + cita.getEstado() + " y no se puede cancelar");
        }
        if (actor.rol() == Rol.PACIENTE) {
            validarAntelacion(cita);
        }
        cita.setEstado(EstadoCita.CANCELADA);
        citaRepository.saveAndFlush(cita);
        log.info("Cita {} cancelada por {} ({})", id, actor.email(), actor.rol());
    }

    // ------------------------------------------------------- reglas privadas

    /** Devuelve la cita solo si el actor puede verla; si no, responde como si no existiera (no filtra datos). */
    private Cita obtenerAccesible(Long id, Actor actor) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada: " + id));
        boolean ajena = switch (actor.rol()) {
            case PACIENTE -> !Objects.equals(cita.getPaciente().getId(), actor.pacienteId());
            case MEDICO -> !Objects.equals(cita.getMedico().getId(), actor.medicoId());
            case RECEPCIONISTA, ADMIN -> false;
        };
        if (ajena) {
            throw new RecursoNoEncontradoException("Cita no encontrada: " + id);
        }
        return cita;
    }

    private Paciente resolverPaciente(Actor actor, Long pacienteIdSolicitado) {
        if (actor.rol() == Rol.PACIENTE) {
            Long propio = requerirPacienteId(actor);
            if (pacienteIdSolicitado != null && !pacienteIdSolicitado.equals(propio)) {
                throw new AccessDeniedException("Solo puedes reservar citas a tu nombre");
            }
            return buscarPaciente(propio);
        }
        if (actor.rol() == Rol.RECEPCIONISTA || actor.rol() == Rol.ADMIN) {
            if (pacienteIdSolicitado == null) {
                throw new SolicitudInvalidaException("El campo pacienteId es obligatorio para reservar a nombre de un paciente");
            }
            return buscarPaciente(pacienteIdSolicitado);
        }
        throw new AccessDeniedException("Tu rol no puede reservar citas");
    }

    private void validarPermisosDeEdicion(Actor actor, CitaUpdateRequest request) {
        if (actor.rol() == Rol.PACIENTE && (request.estado() != null || request.observaciones() != null)) {
            throw new AccessDeniedException("El paciente solo puede reprogramar la cita o cambiar el motivo");
        }
        if (actor.rol() == Rol.MEDICO && (request.fechaHora() != null || request.motivo() != null)) {
            throw new AccessDeniedException("El médico solo registra el estado y las observaciones");
        }
    }

    private void cambiarEstado(Cita cita, EstadoCita nuevo, Actor actor) {
        if (nuevo == cita.getEstado()) {
            return;
        }
        if (actor.rol() == Rol.MEDICO && nuevo != EstadoCita.ATENDIDA && nuevo != EstadoCita.NO_ASISTIO) {
            throw new AccessDeniedException("El médico solo puede marcar la cita como atendida o sin asistencia");
        }
        Set<EstadoCita> permitidos = TRANSICIONES.getOrDefault(cita.getEstado(), Set.of());
        if (!permitidos.contains(nuevo)) {
            throw new ReglaNegocioException("No se puede pasar una cita de " + cita.getEstado() + " a " + nuevo);
        }
        boolean cierraAtencion = nuevo == EstadoCita.ATENDIDA || nuevo == EstadoCita.NO_ASISTIO;
        if (cierraAtencion && cita.getFechaHora().toLocalDate().isAfter(LocalDate.now(clock))) {
            throw new ReglaNegocioException("No se puede marcar como " + nuevo + " una cita de un día futuro");
        }
        cita.setEstado(nuevo);
    }

    private void validarAntelacion(Cita cita) {
        LocalDateTime limite = LocalDateTime.now(clock).plusHours(HORAS_MIN_ANTELACION);
        if (cita.getFechaHora().isBefore(limite)) {
            throw new ReglaNegocioException(
                    "Solo puedes modificar o cancelar la cita hasta " + HORAS_MIN_ANTELACION + " horas antes");
        }
    }

    private void validarFechaYDisponibilidad(Medico medico, Paciente paciente,
                                             LocalDateTime fechaHora, Long citaIdExcluir) {
        if (!fechaHora.isAfter(LocalDateTime.now(clock))) {
            throw new ReglaNegocioException("La fecha y hora de la cita deben ser futuras");
        }
        boolean alineada = fechaHora.getSecond() == 0 && fechaHora.getNano() == 0
                && fechaHora.getMinute() % DURACION_MIN == 0;
        if (!alineada) {
            throw new ReglaNegocioException("Las citas se programan en bloques de 30 minutos (por ejemplo 10:00 o 10:30)");
        }

        DiaSemana dia = DiaSemana.values()[fechaHora.getDayOfWeek().getValue() - 1]; // lunes = 1
        LocalTime inicio = fechaHora.toLocalTime();
        LocalTime fin = inicio.plusMinutes(DURACION_MIN);
        List<HorarioDisponible> horarios = horarioRepository.findByMedicoIdAndDiaSemana(medico.getId(), dia);
        boolean dentroDeHorario = !fin.isBefore(inicio) && horarios.stream()
                .anyMatch(h -> !inicio.isBefore(h.getHoraInicio()) && !fin.isAfter(h.getHoraFin()));
        if (!dentroDeHorario) {
            throw new ReglaNegocioException("El médico no atiende en ese día u horario");
        }

        Long excluir = citaIdExcluir == null ? -1L : citaIdExcluir;
        if (citaRepository.existeCitaActivaDelMedico(medico.getId(), fechaHora, excluir)) {
            throw new ReglaNegocioException("El médico ya tiene una cita en ese horario");
        }
        if (citaRepository.existeCitaActivaDelPaciente(paciente.getId(), fechaHora, excluir)) {
            throw new ReglaNegocioException("El paciente ya tiene otra cita en ese horario");
        }
    }

    private Paciente buscarPaciente(Long id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente no encontrado: " + id));
    }

    private Long requerirPacienteId(Actor actor) {
        if (actor.pacienteId() == null) {
            throw new RecursoNoEncontradoException("Tu usuario no tiene un perfil de paciente asociado");
        }
        return actor.pacienteId();
    }

    private Long requerirMedicoId(Actor actor) {
        if (actor.medicoId() == null) {
            throw new RecursoNoEncontradoException("Tu usuario no tiene un perfil de médico asociado");
        }
        return actor.medicoId();
    }

    private static String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
