package com.medicita.service;

import com.medicita.dto.CitaFueraDeHorarioResponse;
import com.medicita.dto.HorarioRequest;
import com.medicita.dto.MedicoAdminResponse;
import com.medicita.dto.MedicoRequest;
import com.medicita.entity.Especialidad;
import com.medicita.entity.HorarioDisponible;
import com.medicita.entity.Medico;
import com.medicita.entity.Sede;
import com.medicita.entity.Usuario;
import com.medicita.enums.Rol;
import com.medicita.exception.RecursoNoEncontradoException;
import com.medicita.exception.ReglaNegocioException;
import com.medicita.exception.SolicitudInvalidaException;
import com.medicita.repository.CitaRepository;
import com.medicita.repository.EspecialidadRepository;
import com.medicita.repository.HorarioDisponibleRepository;
import com.medicita.repository.MedicoRepository;
import com.medicita.repository.SedeRepository;
import com.medicita.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Administración de médicos (solo ADMIN): alta, edición, horarios y activación. */
@Slf4j
@Service
@RequiredArgsConstructor
public class MedicoService {

    private static final int DURACION_MIN = 30;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final MedicoRepository medicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EspecialidadRepository especialidadRepository;
    private final SedeRepository sedeRepository;
    private final HorarioDisponibleRepository horarioRepository;
    private final CitaRepository citaRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<MedicoAdminResponse> listar() {
        Map<Long, List<HorarioDisponible>> horarios = new HashMap<>();
        for (HorarioDisponible h : horarioRepository.findAll()) {
            horarios.computeIfAbsent(h.getMedico().getId(), k -> new ArrayList<>()).add(h);
        }
        return medicoRepository.listarConDetalle().stream()
                .map(m -> aRespuesta(m, horarios.getOrDefault(m.getId(), List.of()), List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public MedicoAdminResponse obtener(Long id) {
        Medico m = buscar(id);
        return aRespuesta(m, horarioRepository.findByMedicoId(id), List.of());
    }

    @Transactional
    public MedicoAdminResponse crear(MedicoRequest request) {
        validarHorarios(request.horarios());
        Claves.error(request.password()).ifPresent(m -> {
            throw new SolicitudInvalidaException(m);
        });
        String email = request.email().trim().toLowerCase();
        String cmp = request.cmp().trim().toUpperCase();
        if (usuarioRepository.existsByEmail(email)) {
            throw new ReglaNegocioException("Ya existe una cuenta con ese correo");
        }
        if (medicoRepository.existsByCmpIgnoreCase(cmp)) {
            throw new ReglaNegocioException("Ya hay un médico registrado con el CMP " + cmp);
        }
        Especialidad especialidad = especialidad(request.especialidadId());
        Sede sede = sede(request.sedeId());

        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .nombres(request.nombres().trim())
                .apellidos(request.apellidos().trim())
                .rol(Rol.MEDICO)
                .build());
        Medico medico = medicoRepository.save(Medico.builder()
                .usuario(usuario).especialidad(especialidad).sede(sede).cmp(cmp).build());
        List<HorarioDisponible> horarios = guardarHorarios(medico, request.horarios());

        log.info("Médico {} creado: {} ({})", medico.getId(), email, especialidad.getNombre());
        return aRespuesta(medico, horarios, List.of());
    }

    @Transactional
    public MedicoAdminResponse actualizar(Long id, MedicoRequest request) {
        validarHorarios(request.horarios());
        Medico medico = buscar(id);
        Usuario usuario = medico.getUsuario();
        String email = request.email().trim().toLowerCase();
        String cmp = request.cmp().trim().toUpperCase();

        if (!email.equalsIgnoreCase(usuario.getEmail()) && usuarioRepository.existsByEmail(email)) {
            throw new ReglaNegocioException("Ya existe una cuenta con ese correo");
        }
        if (medicoRepository.existsByCmpIgnoreCaseAndIdNot(cmp, id)) {
            throw new ReglaNegocioException("Ya hay un médico registrado con el CMP " + cmp);
        }
        boolean desactivar = Boolean.FALSE.equals(request.activo()) && usuario.isActivo();
        if (desactivar) {
            long porAtender = citaRepository.contarPorAtenderDelMedico(id, LocalDateTime.now(clock));
            if (porAtender > 0) {
                throw new ReglaNegocioException("No se puede desactivar: el médico tiene " + porAtender
                        + (porAtender == 1 ? " cita por atender" : " citas por atender")
                        + ". Cancélalas o reprográmalas primero");
            }
        }
        if (request.password() != null && !request.password().isBlank()) {
            Claves.error(request.password()).ifPresent(m -> {
                throw new SolicitudInvalidaException(m);
            });
            usuario.setPassword(passwordEncoder.encode(request.password()));
        }

        usuario.setEmail(email);
        usuario.setNombres(request.nombres().trim());
        usuario.setApellidos(request.apellidos().trim());
        if (request.activo() != null) {
            usuario.setActivo(request.activo());
        }
        usuarioRepository.save(usuario);

        medico.setCmp(cmp);
        medico.setEspecialidad(especialidad(request.especialidadId()));
        medico.setSede(sede(request.sedeId()));
        medicoRepository.save(medico);

        horarioRepository.borrarDeMedico(id);
        List<HorarioDisponible> horarios = guardarHorarios(medico, request.horarios());

        // Las citas por atender que ya no caben en el horario nuevo NO se tocan: se avisa para que recepción las reprograme
        List<CitaFueraDeHorarioResponse> fuera = citaRepository
                .listarPorAtenderDelMedico(id, LocalDateTime.now(clock)).stream()
                .filter(c -> !Horarios.dentro(request.horarios(), c.getFechaHora(), c.getDuracionMin()))
                .map(c -> new CitaFueraDeHorarioResponse(c.getId(), c.getFechaHora(),
                        c.getPaciente().getUsuario().getNombres() + " " + c.getPaciente().getUsuario().getApellidos(),
                        c.getEstado()))
                .toList();

        log.info("Médico {} actualizado (activo={}, citas fuera de horario={})", id, usuario.isActivo(), fuera.size());
        return aRespuesta(medico, horarios, fuera);
    }

    // ------------------------------------------------------------------ apoyo

    private Medico buscar(Long id) {
        return medicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Médico no encontrado: " + id));
    }

    private Especialidad especialidad(Long id) {
        return especialidadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Especialidad no encontrada: " + id));
    }

    private Sede sede(Long id) {
        return sedeRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sede no encontrada: " + id));
    }

    private static void validarHorarios(List<HorarioRequest> horarios) {
        Horarios.primerError(horarios, DURACION_MIN).ifPresent(m -> {
            throw new SolicitudInvalidaException(m);
        });
    }

    private List<HorarioDisponible> guardarHorarios(Medico medico, List<HorarioRequest> horarios) {
        return horarioRepository.saveAll(horarios.stream()
                .map(h -> HorarioDisponible.builder()
                        .medico(medico).diaSemana(h.dia()).horaInicio(h.inicio()).horaFin(h.fin()).build())
                .toList());
    }

    private static MedicoAdminResponse aRespuesta(Medico m, List<HorarioDisponible> horarios,
                                                  List<CitaFueraDeHorarioResponse> fuera) {
        Usuario u = m.getUsuario();
        List<MedicoAdminResponse.Horario> tramos = horarios.stream()
                .sorted(Comparator.comparing(HorarioDisponible::getDiaSemana).thenComparing(HorarioDisponible::getHoraInicio))
                .map(h -> new MedicoAdminResponse.Horario(
                        h.getDiaSemana(), h.getHoraInicio().format(HORA), h.getHoraFin().format(HORA)))
                .toList();
        return new MedicoAdminResponse(
                m.getId(), u.getNombres(), u.getApellidos(), u.getEmail(), m.getCmp(),
                m.getEspecialidad().getId(), m.getEspecialidad().getNombre(),
                m.getSede().getId(), m.getSede().getNombre(),
                u.isActivo(), tramos, fuera);
    }
}
