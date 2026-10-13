package com.medicita.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** Reloj único de la aplicación: siempre hora de Lima, aunque el servidor en la nube esté en UTC. */
@Configuration
public class TimeConfig {

    public static final ZoneId ZONA = ZoneId.of("America/Lima");

    @Bean
    public Clock clock() {
        return Clock.system(ZONA);
    }
}
