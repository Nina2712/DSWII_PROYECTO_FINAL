package com.medicita.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Fotos: reglas de la foto de perfil")
class FotosTest {

    private static String jpeg(int bytes) {
        byte[] datos = new byte[bytes];
        datos[0] = (byte) 0xFF;
        datos[1] = (byte) 0xD8;
        datos[2] = (byte) 0xFF;
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(datos);
    }

    @Test
    @DisplayName("Vacía o nula significa quitar la foto")
    void vacia() {
        assertThat(Fotos.error(null)).isEmpty();
        assertThat(Fotos.error("  ")).isEmpty();
        assertThat(Fotos.normalizar("  ")).isNull();
        assertThat(Fotos.normalizar(null)).isNull();
    }

    @Test
    @DisplayName("Un JPEG pequeño es válido")
    void valida() {
        assertThat(Fotos.error(jpeg(2000))).isEmpty();
    }

    @Test
    @DisplayName("Rechaza otros formatos, base64 roto, cabecera falsa y fotos grandes")
    void rechaza() {
        assertThat(Fotos.error("data:image/png;base64,AAAA")).isPresent();
        assertThat(Fotos.error("http://sitio.com/foto.jpg")).isPresent();
        assertThat(Fotos.error("data:image/jpeg;base64,@@@@")).isPresent();
        assertThat(Fotos.error("data:image/jpeg;base64," + Base64.getEncoder().encodeToString("no es jpeg".getBytes()))).isPresent();
        assertThat(Fotos.error(jpeg(20000))).isPresent();
    }
}
