package com.medicita.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Impide arrancar en producción con la clave JWT de desarrollo o con una clave demasiado corta. */
@Component
@Profile("prod")
public class ValidacionProduccion {

    public ValidacionProduccion(@Value("${app.jwt.secret}") String secreto) {
        if (secreto.startsWith("clave-solo-para-desarrollo") || secreto.length() < 32) {
            throw new IllegalStateException(
                    "En producción hay que definir JWT_SECRET con al menos 32 caracteres "
                            + "(no se permite la clave de desarrollo)");
        }
    }
}
