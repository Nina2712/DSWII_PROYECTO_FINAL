package com.medicita.service;

import java.util.Base64;
import java.util.Optional;

/** Reglas de la foto de perfil: un JPEG pequeño en formato data URL (la interfaz lo recorta a 128x128). */
public final class Fotos {

    public static final int MAXIMO_CARACTERES = 12000;
    static final String PREFIJO = "data:image/jpeg;base64,";

    private Fotos() {
    }

    /** Devuelve el motivo del rechazo, o vacío si la foto es válida. Una foto vacía significa "quitar". */
    public static Optional<String> error(String foto) {
        if (foto == null || foto.isBlank()) {
            return Optional.empty();
        }
        if (foto.length() > MAXIMO_CARACTERES) {
            return Optional.of("La foto es demasiado grande. Elige otra imagen");
        }
        if (!foto.startsWith(PREFIJO)) {
            return Optional.of("La foto debe ser una imagen JPEG");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(foto.substring(PREFIJO.length()));
        } catch (IllegalArgumentException e) {
            return Optional.of("La foto no es una imagen válida");
        }
        boolean cabeceraJpeg = bytes.length > 3
                && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
        return cabeceraJpeg ? Optional.empty() : Optional.of("La foto no es una imagen JPEG válida");
    }

    /** Foto lista para guardar: texto vacío se convierte en null (sin foto). */
    public static String normalizar(String foto) {
        return foto == null || foto.isBlank() ? null : foto.trim();
    }
}
