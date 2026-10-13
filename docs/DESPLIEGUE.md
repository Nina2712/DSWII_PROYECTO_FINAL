# Despliegue en la nube (gratis): Render + Aiven MySQL

Arquitectura: **Render** ejecuta el contenedor Docker de la API; **Aiven** aloja MySQL. Ambos tienen plan gratuito sin tarjeta (condiciones verificadas en octubre de 2026; revísalas en sus páginas antes de desplegar).

## 0. Antes de empezar
- Hacer `git pull`, `git add .`, `git commit` y `git push` con los cambios de este paso.
- Si el repositorio es privado y pertenece a **Nina2712**, ella debe autorizar a Render (GitHub → Settings → Applications) o el repo debe ser público; si no, Render no lo verá.

## 1. Base de datos en Aiven
1. Crear cuenta en aiven.io → **Create service → MySQL → Free plan** (1 GB).
2. Esperar a que esté *Running*. En **Overview** copiar: Host, Port, User (`avnadmin`), Password, Database (`defaultdb`).
3. La URL JDBC queda así (confirmar que el servicio exige SSL):
   `jdbc:mysql://HOST:PORT/defaultdb?sslMode=REQUIRED&connectionTimeZone=America/Lima&characterEncoding=utf8`
4. Limitaciones: Aiven puede apagar servicios gratuitos inactivos (avisa por correo; se enciende de nuevo desde la consola). Revisarlo antes de la demo.

## 2. Secreto JWT
PowerShell:
```powershell
$b = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```
Guardar el resultado como `JWT_SECRET`. En producción la app **no arranca** si el secreto es el de desarrollo o tiene menos de 32 caracteres.

## 3. Servicio web en Render
1. render.com → **New → Web Service** → conectar el repositorio.
2. **Runtime: Docker** (usa el `Dockerfile` de la raíz), plan **Free**.
3. **Health Check Path:** `/actuator/health`
4. Variables de entorno:

| Variable | Valor |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL` | URL JDBC del paso 1 |
| `DB_USER` / `DB_PASSWORD` | datos de Aiven |
| `JWT_SECRET` | del paso 2 |
| `FLYWAY_LOCATIONS` | `classpath:db/migration,classpath:db/seed` (con datos demo) o solo `classpath:db/migration` |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | opcional; crea un ADMIN al arrancar (clave ≥ 10 caracteres) |
| `SWAGGER_ENABLED` | `true` / `false` |

`PORT` lo define Render automáticamente.

5. **Create Web Service**. El primer build tarda varios minutos. Probar:
   - `https://TU-APP.onrender.com/actuator/health` → `{"status":"UP"}`
   - `https://TU-APP.onrender.com/swagger-ui.html`

## 4. Verificar con Postman
Importar `postman/MediCita.nube.postman_environment.json`, poner tu URL en `baseUrl`, seleccionar el entorno y ejecutar la colección. **La colección necesita los datos demo** (ids de pacientes y médico), por eso usa `db/seed` en `FLYWAY_LOCATIONS` para esa prueba.

## 5. Advertencias importantes
- **Flyway:** decide al inicio si usas seed. Si ya se aplicó `V2` y luego lo quitas de `FLYWAY_LOCATIONS`, Flyway falla por validación.
- **Contraseña pública:** los usuarios demo comparten `Medicita2026*`, visible en GitHub. Con datos reales, usa solo `db/migration` y `ADMIN_EMAIL`/`ADMIN_PASSWORD`.
- **Arranque en frío:** Render gratis se duerme tras 15 min sin tráfico (~1 min en despertar). Abrir `/actuator/health` antes de grabar el video.
- **Fallo de arranque por `ValidacionProduccion` o `DB_URL`:** revisar variables en Render → Logs.

## 6. Probar la imagen en tu PC (opcional, requiere Docker Desktop)
```powershell
docker build -t medicita .
docker run --rm -p 8080:8080 -e SPRING_PROFILES_ACTIVE=prod `
  -e DB_URL="jdbc:mysql://host.docker.internal:3306/medicita?connectionTimeZone=America/Lima&allowPublicKeyRetrieval=true&useSSL=false" `
  -e DB_USER=medicita -e DB_PASSWORD=medicita123 `
  -e JWT_SECRET="una-clave-de-prueba-de-al-menos-32-caracteres-xx" `
  -e FLYWAY_LOCATIONS="classpath:db/migration,classpath:db/seed" medicita
```
(Se incluye el seed porque tu BD local ya lo tiene aplicado.)

## Alternativa
Railway: prueba de 30 días con crédito y luego de pago; no se eligió para mantener el costo en cero.
