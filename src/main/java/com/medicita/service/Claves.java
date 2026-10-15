package com.medicita.service;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** Reglas de contraseña (puras, sin Spring): longitud, letra y número, y el límite de 72 bytes de BCrypt. */
public final class Claves {

    public static final int MINIMO = 8;
    public static final int MAXIMO_BYTES = 72;

    private Claves() {
    }

    /** Devuelve el motivo por el que la contraseña no sirve, o vacío si es válida. */
    public static Optional<String> error(String clave) {
        if (clave == null || clave.isBlank()) {
            return Optional.of("La contraseña es obligatoria");
        }
        if (clave.length() < MINIMO) {
            return Optional.of("La contraseña debe tener al menos " + MINIMO + " caracteres");
        }
        if (clave.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES) {
            return Optional.of("La contraseña es demasiado larga");
        }
        boolean letra = clave.chars().anyMatch(Character::isLetter);
        boolean numero = clave.chars().anyMatch(Character::isDigit);
        if (!letra || !numero) {
            return Optional.of("La contraseña debe incluir al menos una letra y un número");
        }
        return Optional.empty();
    }
}
