# MediCita – Plataforma de reservas y gestión de citas médicas

Proyecto de Investigación Aplicada – Desarrollo de Servicios Web II (CIBERTEC).

**Stack:** Java 21, Spring Boot 3.5, Spring Data JPA, Spring MVC, Spring Security (BCrypt + JWT), Lombok, MySQL, Flyway, Gradle.
Todo el proyecto funciona con IntelliJ IDEA **Community** (gratuito).

## Ejecución local
1. Tener MySQL 8 con la base `medicita` y el usuario `medicita` (contraseña `medicita123`),
   o levantar uno con `docker compose up -d` (si el puerto 3306 está ocupado, usar 3307 y la variable `DB_URL`).
2. Abrir la carpeta en IntelliJ (*Open → build.gradle.kts*) y ejecutar `MedicitaApplication`.
3. Flyway crea las tablas y carga los datos de demostración al arrancar.

Swagger: http://localhost:8080/swagger-ui/index.html

## Usuarios de demostración (contraseña `Medicita2026*`)
| Rol | Correo |
|---|---|
| Administrador | admin@medicita.pe |
| Recepcionista | recepcion@medicita.pe |
| Médico | dr.vargas@medicita.pe |
| Paciente | ana.torres@correo.com / luis.ramos@correo.com |

## API
| Método | Endpoint | Descripción | Roles |
|---|---|---|---|
| POST | `/api/auth/login` | Login, devuelve JWT | público |
| GET | `/api/auth/me` | Perfil del usuario | autenticado |
| GET | `/api/citas` | Listar con filtros (`estado`, `medicoId`, `pacienteId`, `desde`, `hasta`) y paginación (`page`, `size`, `orden`) | todos (cada rol ve lo suyo) |
| GET | `/api/citas/{id}` | Obtener una cita | todos (cada rol ve lo suyo) |
| POST | `/api/citas` | Reservar una cita | paciente, recepción, admin |
| PUT | `/api/citas/{id}` | Reprogramar, cambiar estado u observaciones | según rol |
| DELETE | `/api/citas/{id}` | Cancelar (`?definitivo=true` elimina, solo admin) | paciente, recepción, admin |

**Reglas de negocio:** bloques de 30 minutos dentro del horario del médico, sin cruces de horario (médico ni paciente),
el paciente solo modifica o cancela hasta 2 horas antes, y flujo de estados PENDIENTE → CONFIRMADA → ATENDIDA / NO_ASISTIO (o CANCELADA).

## Pruebas con Postman
Importar `postman/MediCita.postman_collection.json` y usar **Run collection** con la aplicación en marcha:
cada petición incluye pruebas automáticas y la colección deja la base de datos como estaba.
