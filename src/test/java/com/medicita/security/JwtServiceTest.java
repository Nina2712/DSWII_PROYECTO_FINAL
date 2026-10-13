package com.medicita.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "clave-de-prueba-con-mas-de-treinta-y-dos-caracteres-123";

    private final UserDetails usuario = User.withUsername("admin@medicita.pe")
            .password("x").authorities("ROLE_ADMIN").build();

    @Test
    void generaYValidaToken() {
        JwtService jwt = new JwtService(SECRET, 5);
        String token = jwt.generarToken(usuario);

        assertEquals("admin@medicita.pe", jwt.extraerUsername(token));
        assertTrue(jwt.esValido(token, usuario));
    }

    @Test
    void rechazaTokenExpirado() {
        JwtService jwt = new JwtService(SECRET, -1);
        String token = jwt.generarToken(usuario);

        assertFalse(jwt.esValido(token, usuario));
    }

    @Test
    void rechazaTokenAlterado() {
        JwtService jwt = new JwtService(SECRET, 5);
        String token = jwt.generarToken(usuario);
        String alterado = token.substring(0, token.length() - 10) + "xxxxxxxxxx";

        assertFalse(jwt.esValido(alterado, usuario));
    }

    @Test
    void rechazaTokenFirmadoConOtraClave() {
        JwtService otro = new JwtService("otra-clave-distinta-con-mas-de-treinta-y-dos-caracteres", 5);
        String token = otro.generarToken(usuario);

        assertFalse(new JwtService(SECRET, 5).esValido(token, usuario));
    }
}
