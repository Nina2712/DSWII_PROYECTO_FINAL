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
| GET | `/api/catalogo/especialidades/{id}/disponibilidad?fecha=AAAA-MM-DD` | Por cada médico activo de la especialidad: horas libres ese día y próxima fecha con horas | todos |
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
| GET / PUT | `/api/perfil` | Ver y editar mi perfil (paciente: ficha clínica básica; médico: teléfono y presentación) | autenticado |
| PUT | `/api/perfil/password` | Cambiar mi contraseña (exige la actual) | autenticado |
| PUT | `/api/perfil/foto` | Cambiar o quitar mi foto (JPEG 128x128 en data URL, máx. 12 000 caracteres) | autenticado |
| GET | `/api/pacientes/{id}/historial` | Historial clínico: atenciones con cualquier médico, con médico, especialidad y notas; `propia` marca las del médico que consulta | médico (solo si tiene/tuvo cita con el paciente), admin |
| GET | `/api/pacientes/{id}/ficha` | Alergias, seguro y contacto de emergencia del paciente | médico (solo si tiene/tuvo cita con él), recepción, admin |
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

### Mi perfil, ficha del paciente y atención
- **Mi perfil** (todos los roles): cada uno edita sus datos y cambia su contraseña. El paciente completa su ficha (dirección, seguro, **alergias**, contacto de emergencia) y ve sus últimas atenciones con las indicaciones del médico. El médico edita su teléfono y presentación. El correo, el DNI y el CMP no se pueden cambiar desde aquí.
- **Al atender una cita**, el médico (y recepción/admin) ve la ficha del paciente con las alergias resaltadas. Un médico solo puede consultar la ficha de pacientes con los que tiene o tuvo citas. "Marcar como atendida" guarda también las observaciones escritas.
- Migración `V3__perfil_y_ficha_clinica.sql`: nuevas columnas en `paciente` y `medico` (todas opcionales).

### Edición de perfil, foto e historial clínico
- **Mi perfil** se ve en solo lectura; "Editar perfil" habilita los campos y muestra "Guardar cambios" / "Cancelar". Al guardar vuelve a solo lectura.
- **Foto para todos los roles**: el navegador recorta y reduce la imagen a 128x128 JPEG y se guarda en `usuario.foto` (migración `V4__foto_usuario.sql`). Se guarda en la base de datos porque el disco de Render es efímero. Aparece en el menú lateral, en Mi perfil y en la ficha del paciente.
- **Historial clínico**: al abrir una cita, el médico y el administrador ven las atenciones anteriores del paciente con cualquier médico (fecha, médico, especialidad, sede, motivo y notas), en solo lectura.
- **Foto del médico al reservar**: el catálogo de médicos (`GET /api/catalogo/medicos`) ahora incluye `foto` y `descripcion`. Al elegir médico en "Nueva cita" todos los roles (paciente, recepción y admin) ven su foto; también aparece en la lista y el detalle de citas y en la lista de médicos del admin. Solo el **paciente** ve el enlace "Ver perfil del médico", que despliega una mini tarjeta (foto, especialidad, sede, CMP, horarios y presentación).
- **Reserva por especialidad**: en "Nueva cita" primero se elige la especialidad, luego la fecha, y se listan los médicos de esa especialidad con sus horas libres (los que tienen horas van primero). Si un médico no atiende ese día se muestra su próxima fecha libre ("Próxima fecha libre") y, si ninguno de la especialidad tiene horas, un aviso con la fecha más cercana. Al reprogramar, el médico queda fijo. Código en `static/js/reserva.js` y `static/css/reserva.css`; el catálogo de médicos incluye ahora `especialidadId`.
- **Login con fotos**: el lado izquierdo muestra fotos reales de la clínica (`static/img/fotos`) que cambian junto con las frases sobre el cuidado de la salud, con paralaje y una luz que siguen al puntero, y la línea de latido animada encima. Para añadir fotos, copia el archivo a `img/fotos` y agrégalo a la lista `FOTOS` de `js/views/login.js`. El lado derecho tiene el mismo fondo difuminado. Respeta `prefers-reduced-motion`. Estilos en `static/css/login.css`.
- **Encabezado de bienvenida**: el Resumen abre con la foto grande del usuario, saludo según la hora, reloj y fecha de Lima, clima de Lima (Open-Meteo, opcional), citas de hoy, próxima cita y pendientes (`js/hero.js`, `css/hero.css`). La barra superior muestra fecha, hora y el usuario con su foto, y el menú lateral una foto más grande.
- **Observaciones clínicas**: solo las escribe el médico de la cita. Recepción y administrador no pueden editarlas (el servidor responde 403).
