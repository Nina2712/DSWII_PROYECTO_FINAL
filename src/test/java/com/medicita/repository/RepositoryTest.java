package com.medicita.repository;

import com.medicita.config.JpaConfig;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configuración común de las pruebas de la capa de datos.
 * <ul>
 *   <li>Usa MySQL real (esquema {@code medicita_test}), no una base en memoria.</li>
 *   <li>Flyway crea el esquema con las migraciones reales, así que también las valida.</li>
 *   <li>Cada prueba corre en una transacción que se revierte: no deja datos.</li>
 *   <li>Se importa {@link JpaConfig} porque los tests de "slice" no cargan las @Configuration
 *       del proyecto y sin ella la auditoría (creadoEn / actualizadoEn) no funcionaría.</li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(JpaConfig.class)
public @interface RepositoryTest {
}
