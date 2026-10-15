package com.medicita.dto;

import com.medicita.enums.Rol;

public record UsuarioResponse(Long id, String email, String nombres, String apellidos, Rol rol, boolean activo) {
}
