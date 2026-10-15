package com.medicita.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Prueba unitaria pura (no necesita base de datos). */
class ClavesTest {

    @Test
    @DisplayName("Acepta una contraseña de 8 o más caracteres con letra y número")
    void valida() {
        assertThat(Claves.error("Medicita2026*")).isEmpty();
        assertThat(Claves.error("abcdefg1")).isEmpty();
    }

    @Test
    @DisplayName("Rechaza vacía, corta, sin número o sin letra")
    void invalidas() {
        assertThat(Claves.error(null)).isPresent();
        assertThat(Claves.error("   ")).isPresent();
        assertThat(Claves.error("abc12")).hasValueSatisfying(m -> assertThat(m).contains("al menos 8"));
        assertThat(Claves.error("sololetras")).hasValueSatisfying(m -> assertThat(m).contains("letra y un número"));
        assertThat(Claves.error("12345678")).hasValueSatisfying(m -> assertThat(m).contains("letra y un número"));
    }

    @Test
    @DisplayName("Rechaza más de 72 bytes (límite de BCrypt), contando los caracteres con tilde")
    void limiteBcrypt() {
        assertThat(Claves.error("a1".repeat(36))).isEmpty();          // 72 bytes exactos
        assertThat(Claves.error("a1".repeat(36) + "b")).hasValueSatisfying(m -> assertThat(m).contains("demasiado larga"));
        assertThat(Claves.error("ñ1".repeat(25))).hasValueSatisfying(m -> assertThat(m).contains("demasiado larga")); // 75 bytes
    }
}
