package com.medicita.service;

import com.medicita.dto.SedeDetalleResponse;
import com.medicita.dto.SedeRequest;
import com.medicita.entity.Medico;
import com.medicita.entity.Sede;
import com.medicita.exception.RecursoNoEncontradoException;
import com.medicita.exception.ReglaNegocioException;
import com.medicita.repository.MedicoRepository;
import com.medicita.repository.SedeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Sedes de la clínica: listado con sus especialidades y administración (solo ADMIN). */
@Slf4j
@Service
@RequiredArgsConstructor
public class SedeService {

    private final SedeRepository sedeRepository;
    private final MedicoRepository medicoRepository;

    @Transactional(readOnly = true)
    public List<SedeDetalleResponse> listar() {
        List<Medico> medicos = medicoRepository.listarConDetalle();
        return sedeRepository.findAll().stream()
                .sorted(Comparator.comparing(s -> s.getNombre().toLowerCase()))
                .map(s -> aRespuesta(s, medicos))
                .toList();
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
