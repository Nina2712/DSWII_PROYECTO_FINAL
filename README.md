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
| POST | `/api/auth/registro` | Crear cuenta de paciente (siempre rol PACIENTE) y entrar de inmediato | público |
| GET | `/api/auth/me` | Perfil del usuario | autenticado |
| GET | `/api/citas` | Listar con filtros (`estado`, `medicoId`, `pacienteId`, `desde`, `hasta`) y paginación (`page`, `size`, `orden`) | todos (cada rol ve lo suyo) |
| GET | `/api/citas/{id}` | Obtener una cita | todos (cada rol ve lo suyo) |
| POST | `/api/citas` | Reservar una cita | paciente, recepción, admin |
| PUT | `/api/citas/{id}` | Reprogramar, cambiar estado u observaciones | según rol |
| DELETE | `/api/citas/{id}` | Cancelar (`?definitivo=true` elimina, solo admin) | paciente, recepción, admin |
| GET | `/api/dashboard/resumen` | Indicadores y series del panel (últimos 30 y próximos 30 días) | todos (cada rol ve lo suyo) |
| GET | `/api/catalogo/medicos` | Médicos con especialidad, sede y horarios | todos |
| GET | `/api/catalogo/pacientes` | Pacientes (para reservar a su nombre) | recepción, admin |
| GET | `/api/catalogo/medicos/{id}/horas-libres?fecha=AAAA-MM-DD` | Bloques de 30 min libres de un médico | todos |
| GET | `/api/catalogo/sedes` | Sedes de la clínica | todos |
| POST | `/api/pacientes` | Registrar a un paciente que llega a la clínica | recepción, admin |
| GET | `/api/especialidades` | Especialidades con médicos activos y sedes donde se atienden | todos |
| POST / PUT / DELETE | `/api/especialidades[/{id}]` | Crear, editar y eliminar (no si tiene médicos) | admin |
| GET | `/api/sedes` | Sedes con sus médicos y las especialidades que ofrece cada una | autenticado |
| GET | `/api/sedes/{id}/resumen` | Panel de la sede: citas de hoy, por atender, resultados de 30 días, médicos y próximas citas | admin |
| POST / PUT / DELETE | `/api/sedes[/{id}]` | Crear, editar y eliminar (nombre único; no si tiene médicos) | admin |
| GET / POST | `/api/usuarios` | Listar y crear administradores y recepcionistas | admin |
| PUT | `/api/usuarios/{id}` | Editar, cambiar rol, activar/desactivar o restablecer contraseña | admin |
| GET / POST | `/api/medicos` | Listar (incluye desactivados) y dar de alta médicos con sus horarios | admin |
| GET / PUT | `/api/medicos/{id}` | Ver y editar un médico, reemplazar horarios, activar o desactivar | admin |

**Reglas de negocio:** bloques de 30 minutos dentro del horario del médico, sin cruces de horario (médico ni paciente),
el paciente solo modifica o cancela hasta 2 horas antes, y flujo de estados PENDIENTE → CONFIRMADA → ATENDIDA / NO_ASISTIO (o CANCELADA).

## Dashboard web
Interfaz servida por el mismo Spring Boot desde `src/main/resources/static` (sin Node, sin librerías externas): abrir la raíz `/` de la aplicación
(local: http://localhost:8080/, nube: la URL pública de Render). Se despliega sola con cada `git push`.

| Vista | Qué muestra |
|---|---|
| Inicio de sesión | Login con JWT y cuentas de demostración |
| Resumen | Indicadores (citas de hoy, pendientes, confirmadas, asistencia), citas por día, por estado, por especialidad y por médico, próximas citas |
| Agenda | Calendario semanal y mensual con las citas coloreadas por estado; filtro por médico (recepción y administración) |
| Citas | Tabla con filtros (estado, médico, fechas, orden) y paginación |
| Detalle de cita | Panel lateral con acciones según el rol: confirmar, atender, no asistió, reprogramar, cancelar, observaciones, eliminar (admin) |
| Nueva cita | Reserva con médicos, horas libres calculadas por el servidor y validación de cruces |

Cada rol ve solo lo suyo (paciente: sus citas; médico: su agenda; recepción y administración: toda la clínica). Incluye modo claro y oscuro
y se adapta a celular. Los permisos los aplica siempre el backend; la interfaz solo oculta los botones que no corresponden.


Las pruebas de la capa de datos (`@DataJpaTest`) usan **MySQL real** en un esquema aparte, `medicita_test`, para no tocar los datos de desarrollo.
Cubren insertar, actualizar, eliminar y listar (con paginación y filtros), además de restricciones únicas, integridad referencial y reglas de horario.

1. **Una sola vez**, en MySQL Workbench como `root`, ejecutar `docs/crear-bd-pruebas.sql`.
2. Ejecutar las pruebas:
   - Terminal: `.\gradlew.bat test` (Windows) o `./gradlew test` (Linux/Mac).
   - IntelliJ: clic derecho sobre `src/test/java` → *Run 'All Tests'*.
3. Informe HTML: `build/reports/tests/test/index.html`.

Cada prueba se revierte al terminar (no deja datos) y Flyway crea el esquema con las migraciones reales, por lo que también las valida.
Variables opcionales: `TEST_DB_URL`, `TEST_DB_USER`, `TEST_DB_PASSWORD`.

### Altas de usuarios

- **Pacientes:** se registran solos desde "Crear cuenta" en la pantalla de inicio de sesión, o los registra recepción
  (sección *Pacientes* o desde el diálogo de reserva). La contraseña pide 8 o más caracteres con una letra y un número.
- **Médicos:** solo el administrador los da de alta (sección *Médicos*), con correo, contraseña inicial, CMP, especialidad,
  sede y horarios. Si falta la especialidad, se crea desde el mismo formulario o en la sección *Especialidades*.
- Un médico no se elimina (conserva su historial de citas): se **desactiva**, y solo si no tiene citas por atender.
  Un médico desactivado no aparece al reservar y no puede iniciar sesión.

## Pruebas con Postman
Importar `postman/MediCita.postman_collection.json` y usar **Run collection** con la aplicación en marcha:
cada petición incluye pruebas automáticas y la colección deja la base de datos como estaba.

## Despliegue en la nube
Ver [docs/DESPLIEGUE.md](docs/DESPLIEGUE.md) (Render + Aiven MySQL, variables de entorno, entorno Postman `postman/MediCita.nube.postman_environment.json`).

### Sedes y cambios de horario
- La vista **Sedes** (admin) muestra por sede su dirección, los médicos activos y qué especialidades ofrece, más una matriz de cobertura especialidad × sede. En **Especialidades** aparece en qué sedes se atiende cada una.
- Al guardar un horario nuevo para un médico, la respuesta incluye `citasFueraDeHorario`: las citas PENDIENTE/CONFIRMADA que ya no caben en sus tramos. El sistema **no las cancela ni las mueve**; solo avisa para que recepción las reprograme.

### Personal del sistema y panel por sede (admin)
- **Usuarios:** un administrador puede crear otros administradores y recepcionistas, cambiarles el rol, desactivarlos y restablecer su contraseña. Reglas: nadie puede cambiar su propio rol ni desactivarse, y siempre debe quedar al menos un administrador activo. Los médicos y pacientes no se gestionan aquí.
- **Alcance del admin:** ve y gestiona *todas* las sedes (ningún usuario está atado a una sede). Para entender cada una, **Sedes → Ver panel** muestra citas de hoy, por atender, atendidas y % de inasistencia de los últimos 30 días, el equipo médico con su carga y las próximas citas.
- Control del admin *por sede* (cada admin/recepción limitado a una sede) queda como mejora futura.
