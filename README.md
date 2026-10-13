# MediCita – Plataforma de reservas y gestión de citas médicas

Proyecto de Investigación Aplicada – Desarrollo de Servicios Web II (CIBERTEC).
Stack: Java 21, Spring Boot 3.5, Spring Data JPA, Spring MVC, Spring Security (BCrypt + JWT), Lombok, MySQL 8.4, Flyway, Gradle.

## Ejecución local
1. `docker compose up -d`  (levanta MySQL en el puerto 3306)
2. Abrir el proyecto en IntelliJ IDEA (Open → build.gradle.kts) y ejecutar `MedicitaApplication`.
3. Conectar MySQL Workbench: localhost:3306, usuario `medicita`, contraseña `medicita123`, base `medicita`.

Flyway crea las tablas y carga los datos de demostración al arrancar.
Usuarios demo (contraseña `Medicita2026*`): admin@medicita.pe, recepcion@medicita.pe, dr.vargas@medicita.pe, ana.torres@correo.com
