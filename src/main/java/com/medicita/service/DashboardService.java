package com.medicita.service;

import com.medicita.dto.CitaResponse;
import com.medicita.dto.ResumenResponse;
import com.medicita.entity.Cita;
import com.medicita.enums.EstadoCita;
import com.medicita.enums.Rol;
import com.medicita.exception.RecursoNoEncontradoException;
import com.medicita.repository.CitaRepository;
import com.medicita.repository.CitaSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Indicadores del dashboard. Respeta el alcance del rol, igual que el listado de citas. */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int DIAS_ATRAS = 29;
    private static final int DIAS_ADELANTE = 30;
    private static final int DIAS_GRAFICO_ATRAS = 6;
    private static final int DIAS_GRAFICO_ADELANTE = 7;
    private static final int MAX_PROXIMAS = 6;
    private static final int MAX_CITAS_VENTANA = 1000;

    private final CitaRepository citaRepository;
    private final ContextoUsuario contexto;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ResumenResponse resumen() {
        Actor actor = contexto.actual();

        Long pacienteId = null;
        Long medicoId = null;
        if (actor.rol() == Rol.PACIENTE) {
            pacienteId = actor.pacienteId();
            if (pacienteId == null) {
                throw new RecursoNoEncontradoException("Tu usuario no tiene un perfil de paciente asociado");
            }
        } else if (actor.rol() == Rol.MEDICO) {
            medicoId = actor.medicoId();
            if (medicoId == null) {
                throw new RecursoNoEncontradoException("Tu usuario no tiene un perfil de médico asociado");
            }
        }

        LocalDate hoy = LocalDate.now(clock);
        LocalDateTime ahora = LocalDateTime.now(clock);

        Specification<Cita> spec = CitaSpecifications.con(
                null, medicoId, pacienteId, hoy.minusDays(DIAS_ATRAS), hoy.plusDays(DIAS_ADELANTE));
        List<CitaResponse> citas = citaRepository
                .findAll(spec, PageRequest.of(0, MAX_CITAS_VENTANA, Sort.by("fechaHora").and(Sort.by("id"))))
                .getContent().stream()
                .map(CitaMapper::toResponse)
                .toList();

        // --- indicadores
        long pendientes = contar(citas, EstadoCita.PENDIENTE);
        long confirmadas = contar(citas, EstadoCita.CONFIRMADA);
        long atendidas = contar(citas, EstadoCita.ATENDIDA);
        long canceladas = contar(citas, EstadoCita.CANCELADA);
        long noAsistio = contar(citas, EstadoCita.NO_ASISTIO);
        long citasHoy = citas.stream()
                .filter(c -> c.fechaHora().toLocalDate().equals(hoy) && c.estado() != EstadoCita.CANCELADA)
                .count();
        Double tasa = (atendidas + noAsistio) == 0
                ? null
                : Math.round(1000.0 * atendidas / (atendidas + noAsistio)) / 10.0;
        ResumenResponse.Kpis kpis = new ResumenResponse.Kpis(
                citas.size(), citasHoy, pendientes, confirmadas, atendidas, canceladas, noAsistio, tasa);

        // --- gráficos
        List<ResumenResponse.Conteo> porEstado = new ArrayList<>();
        for (EstadoCita estado : EstadoCita.values()) {
            porEstado.add(new ResumenResponse.Conteo(estado.name(), contar(citas, estado)));
        }

        List<CitaResponse> noCanceladas = citas.stream().filter(c -> c.estado() != EstadoCita.CANCELADA).toList();
        List<ResumenResponse.Conteo> porEspecialidad = agrupar(noCanceladas, CitaResponse::especialidad);
        boolean veTodo = actor.rol() == Rol.ADMIN || actor.rol() == Rol.RECEPCIONISTA;
        List<ResumenResponse.Conteo> porMedico = veTodo ? agrupar(noCanceladas, CitaResponse::medicoNombre) : List.of();

        List<ResumenResponse.PuntoDia> porDia = new ArrayList<>();
        for (LocalDate dia = hoy.minusDays(DIAS_GRAFICO_ATRAS); !dia.isAfter(hoy.plusDays(DIAS_GRAFICO_ADELANTE)); dia = dia.plusDays(1)) {
            final LocalDate fecha = dia;
            List<CitaResponse> delDia = citas.stream().filter(c -> c.fechaHora().toLocalDate().equals(fecha)).toList();
            long canceladasDia = delDia.stream().filter(c -> c.estado() == EstadoCita.CANCELADA).count();
            porDia.add(new ResumenResponse.PuntoDia(fecha, delDia.size(), canceladasDia));
        }

        List<CitaResponse> proximas = citas.stream()
                .filter(c -> c.fechaHora().isAfter(ahora)
                        && (c.estado() == EstadoCita.PENDIENTE || c.estado() == EstadoCita.CONFIRMADA))
                .limit(MAX_PROXIMAS)
                .toList();

        return new ResumenResponse(hoy, kpis, porEstado, porEspecialidad, porMedico, porDia, proximas);
    }

    private static long contar(List<CitaResponse> citas, EstadoCita estado) {
        return citas.stream().filter(c -> c.estado() == estado).count();
    }

    private static List<ResumenResponse.Conteo> agrupar(List<CitaResponse> citas, Function<CitaResponse, String> clave) {
        Map<String, Long> mapa = citas.stream().collect(Collectors.groupingBy(clave, Collectors.counting()));
        return mapa.entrySet().stream()
                .sorted((a, b) -> {
                    int porTotal = Long.compare(b.getValue(), a.getValue());
                    return porTotal != 0 ? porTotal : a.getKey().compareTo(b.getKey());
                })
                .map(e -> new ResumenResponse.Conteo(e.getKey(), e.getValue()))
                .toList();
    }
}
