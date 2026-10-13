package com.medicita.dto;

public record LoginResponse(
        String token,
        String tipo,
        long expiraEnMinutos,
        String email,
        String nombres,
        String apellidos,
        String rol) {
}
