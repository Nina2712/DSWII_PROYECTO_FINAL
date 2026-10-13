package com.medicita.service;

import com.medicita.enums.Rol;

/** Usuario que realiza la petición, con los identificadores de su perfil (si lo tiene). */
public record Actor(Long usuarioId, String email, Rol rol, Long pacienteId, Long medicoId) {
}
