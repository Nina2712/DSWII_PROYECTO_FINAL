package com.medicita.service;

import com.medicita.dto.EspecialidadRequest;
import com.medicita.dto.EspecialidadResponse;
import com.medicita.entity.Especialidad;
import com.medicita.entity.Medico;
import com.medicita.exception.RecursoNoEncontradoException;
import com.medicita.exception.ReglaNegocioException;
import com.medicita.repository.EspecialidadRepository;
import com.medicita.repository.MedicoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

@Slf4j
@Service
@RequiredArgsConstructor
public class EspecialidadService {

    private final EspecialidadRepository especialidadRepository;
    private final MedicoRepository medicoRepository;

    @Transactional(readOnly = true)
    public List<EspecialidadResponse> listar() {
        List<Medico> medicos = medicoRepository.listarConDetalle();
        return especialidadRepository.findAll().stream()
                .sorted(Comparator.comparing(e -> e.getNombre().toLowerCase()))
                .map(e -> aRespuesta(e, medicos))
                .toList();
    }

    @Transactional
    public EspecialidadResponse crear(EspecialidadRequest request) {
        String nombre = normalizar(request.nombre());
        if (especialidadRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ReglaNegocioException("Ya existe la especialidad \"" + nombre + "\"");
        }
        Especialidad e = especialidadRepository.save(Especialidad.builder()
                .nombre(nombre)
                .descripcion(limpiar(request.descripcion()))
                .build());
        log.info("Especialidad {} creada: {}", e.getId(), nombre);
        return aRespuesta(e, List.of());
    }

    @Transactional
    public EspecialidadResponse actualizar(Long id, EspecialidadRequest request) {
        Especialidad e = buscar(id);
        String nombre = normalizar(request.nombre());
        if (especialidadRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ReglaNegocioException("Ya existe la especialidad \"" + nombre + "\"");
        }
        e.setNombre(nombre);
        e.setDescripcion(limpiar(request.descripcion()));
        e = especialidadRepository.save(e);
        return aRespuesta(e, medicoRepository.listarConDetalle());
    }

    @Transactional
    public void eliminar(Long id) {
        Especialidad e = buscar(id);
        if (medicoRepository.existsByEspecialidadId(id)) {
            throw new ReglaNegocioException(
                    "No se puede eliminar \"" + e.getNombre() + "\" porque tiene médicos asignados. Reasígnalos primero");
        }
        especialidadRepository.delete(e);
        log.info("Especialidad {} eliminada: {}", id, e.getNombre());
    }

    private Especialidad buscar(Long id) {
        return especialidadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Especialidad no encontrada: " + id));
    }

    private static String normalizar(String nombre) {
        return nombre.trim().replaceAll("\\s+", " ");
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    /** Cuenta los médicos activos y reúne las sedes donde se atiende (solo con médicos activos). */
    private static EspecialidadResponse aRespuesta(Especialidad e, List<Medico> todos) {
        List<Medico> propios = todos.stream().filter(m -> m.getEspecialidad().getId().equals(e.getId())).toList();
        List<Medico> activos = propios.stream().filter(m -> m.getUsuario().isActivo()).toList();
        List<String> sedes = new TreeSet<>(activos.stream().map(m -> m.getSede().getNombre()).toList()).stream().toList();
        return new EspecialidadResponse(e.getId(), e.getNombre(), e.getDescripcion(),
                activos.size(), !propios.isEmpty(), sedes);
    }
}
