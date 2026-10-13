package com.medicita.config;

import com.medicita.entity.Usuario;
import com.medicita.enums.Rol;
import com.medicita.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea (o actualiza la contraseña de) el administrador a partir de las variables de entorno
 * ADMIN_EMAIL y ADMIN_PASSWORD. Así en la nube no hace falta dejar la clave pública de los datos demo.
 * Si las variables no están definidas, no hace nada. Es idempotente: se puede ejecutar en cada arranque.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInicial implements ApplicationRunner {

    private static final int LONGITUD_MINIMA = 10;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String email;

    @Value("${app.admin.password:}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            return;
        }
        if (password.length() < LONGITUD_MINIMA) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD debe tener al menos " + LONGITUD_MINIMA + " caracteres");
        }
        String correo = email.trim().toLowerCase();

        Usuario existente = usuarioRepository.findByEmail(correo).orElse(null);
        if (existente == null) {
            usuarioRepository.save(Usuario.builder()
                    .email(correo)
                    .password(passwordEncoder.encode(password))
                    .nombres("Administrador")
                    .apellidos("MediCita")
                    .rol(Rol.ADMIN)
                    .build());
            log.info("Administrador inicial creado: {}", correo);
            return;
        }
        if (existente.getRol() != Rol.ADMIN) {
            throw new IllegalStateException(
                    "ADMIN_EMAIL (" + correo + ") ya existe con el rol " + existente.getRol());
        }
        if (!passwordEncoder.matches(password, existente.getPassword()) || !existente.isActivo()) {
            existente.setPassword(passwordEncoder.encode(password));
            existente.setActivo(true);
            usuarioRepository.save(existente);
            log.info("Contraseña del administrador {} actualizada desde las variables de entorno", correo);
        }
    }
}
