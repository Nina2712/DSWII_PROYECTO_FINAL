package com.medicita.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** Respuesta paginada con formato estable (no se serializa PageImpl directamente). */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <T> PageResponse<T> of(Page<T> pagina) {
        return new PageResponse<>(pagina.getContent(), pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages());
    }
}
