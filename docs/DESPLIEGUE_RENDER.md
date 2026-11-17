# Desplegar Bóveda Chat en Render

El código está preparado para desplegarse, pero necesita las credenciales de tus cuentas. Docker se construye en Render/GitHub; no hace falta Docker Desktop en tu portátil.

## 1. Publicar el código

Extrae el ZIP. La carpeta que subes debe tener `pom.xml`, `Dockerfile`, `render.yaml`, `src/` y `.github/` en la raíz. Abre esa carpeta con GitHub Desktop, crea un repositorio nuevo y publica. No subas el ZIP como único archivo del repositorio.

Ve a **Actions → Calidad y seguridad**. Se ejecutan las pruebas y los escaneos sin credenciales externas. Si una comprobación falla, abre el paso en rojo y conserva su mensaje. Trivy puede señalar vulnerabilidades nuevas con el paso del tiempo; corrige o actualiza la dependencia identificada y vuelve a ejecutar. No desactives el fallo solo para obtener un estado verde.

## 2. Elegir un proyecto Supabase

Puedes usar un proyecto existente activo: el backend utiliza el esquema **boveda_chat** y un bucket privado dedicado **boveda-chat**. No uses nombres de buckets de tus otras aplicaciones. Si tu proyecto está pausado, primero reanúdalo.

En el panel **Connect**, elige PostgreSQL **Session pooler / Session mode**, puerto **5432**. Copia por separado:

- Host del pooler.
- Usuario completo que muestra Supabase; puede contener un punto y la referencia del proyecto.
- Contraseña de PostgreSQL que tú configuraste. No es la clave de la API.
- Nombre de la base de datos, normalmente `postgres`.

La conexión directa puede usar IPv6. Usamos Session pooler compatible con IPv4 y con las sesiones de JDBC/Hibernate. No confundas el puerto 5432 de Session mode con 6543 de Transaction mode.

En **Project Settings → API / API Keys** copia:

- Project URL, por ejemplo `https://referencia.supabase.co`.
- **Secret key** para servidor (`sb_secret_...` cuando esté disponible). También es compatible la clave heredada `service_role`; nunca uses `anon` o `publishable` para este adaptador. No se envía al navegador.

El servidor crea el bucket si no existe. Si ya existe, comprueba que sea privado. Los blobs son archivos `.enc`, de tipo `application/octet-stream`: no configures el bucket para aceptar solamente `image/*` porque lo que subimos ya está cifrado.

## 3. Certificado de la base de datos

Descarga el certificado CA de la base desde las opciones SSL de Supabase (Database settings / SSL configuration). En Render, agrégalo como **Secret File** con el nombre `supabase-ca.crt` y pega su contenido PEM completo.

Render lo monta en `/etc/secrets/supabase-ca.crt`. El Dockerfile ya agrega al usuario sin privilegios al grupo 1000 para poder leer los Secret Files de Render. La aplicación usará `sslmode=verify-full` para verificar también la identidad del servidor PostgreSQL. No publiques claves privadas; este archivo es el certificado CA público.

## 4. Crear la clave de cifrado una sola vez

En PowerShell, dentro del proyecto:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\clave-render.ps1
```

Copia el valor mostrado a **BOVEDA_KEY** de Render y guárdalo de forma segura. El script conserva el mismo valor si lo ejecutas otra vez. No uses la clave del ejemplo de las pruebas.

## 5. Crear el servicio Render

Puedes usar **New → Blueprint**, conectar tu repositorio y aplicar `render.yaml`. Antes de desplegar, agrega el Secret File del paso 3. Si el primer intento inicia antes de agregarlo y falla, agrega el archivo y usa **Manual Deploy → Deploy latest commit**.

También puedes usar **New → Web Service**, conectar GitHub y elegir:

- Runtime: **Docker**.
- Dockerfile: `./Dockerfile`; Docker context: `.`.
- Plan: **Free**.
- Health check: `/actuator/health`.
- Auto-Deploy: **After CI Checks Pass**.

No configures Build Command ni Start Command para el runtime Docker: el Dockerfile ya los define.

## 6. Variables de entorno

| Variable | Valor |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL` | `jdbc:postgresql://HOST-POOLER:5432/postgres?sslmode=verify-full&sslrootcert=/etc/secrets/supabase-ca.crt` |
| `DB_USERNAME` | Usuario exacto de Session pooler |
| `DB_PASSWORD` | Contraseña PostgreSQL |
| `BOVEDA_KEY` | El valor Base64 generado una sola vez |
| `SUPABASE_URL` | URL del proyecto, sin barra final |
| `SUPABASE_SECRET_KEY` | Clave secreta de servidor |
| `SUPABASE_BUCKET` | `boveda-chat` |
| `ADMIN_USERNAME` | Un usuario nuevo, por ejemplo `admin_boveda` |
| `ADMIN_PASSWORD` | Una contraseña de al menos 12 caracteres, hasta 72 bytes |
| `SMS_ENABLED` | `false` para el primer despliegue |
| `SMS_DAILY_BUDGET` | `20` |

No hace falta definir `PORT`; Render lo proporciona. La aplicación escucha en todas las interfaces. Si el nombre real de tu base es otro, reemplaza `postgres` en DB_URL. Las credenciales se pasan separadas, por lo que no debes pegarlas dentro de la URL ni codificar la contraseña como URL.

## 7. Primer arranque

Se aplican migraciones Flyway en `boveda_chat`, se comprueba el bucket privado y se crea el administrador indicado. No se alteran tablas existentes en otros esquemas. En los logs debe aparecer `Started BovedaChatApplication`.

Después, puedes ejecutar `infra/supabase-restringir-api.sql` desde SQL Editor para cerrar el acceso de las funciones públicas de la Data API a ese esquema. No agregues `boveda_chat` a los esquemas expuestos de la API. La aplicación usa JDBC.

Entra a `https://TU-SERVICIO.onrender.com`, crea dos cuentas normales y prueba desde dos navegadores. El administrador ve la auditoría desde el icono del escudo. Si aún no configuraste SMS, el registro no pide celular y nunca lo marca como verificado.

## 8. Activar SMS después

Sigue [SMS.md](SMS.md), agrega las tres variables Twilio y cambia `SMS_ENABLED=true`. Render reinicia el servicio para aplicar la configuración. Las cuentas ya existentes conservan su estado: activar SMS no convierte un teléfono sin verificar en verificado ni obliga retroactivamente a verificar cuentas creadas sin celular.

## Errores frecuentes

| Error | Qué revisar |
|---|---|
| Password authentication failed | Usuario completo del pooler y contraseña PostgreSQL |
| Network unreachable | Usa Session pooler IPv4, no el host directo IPv6 |
| Certificado no encontrado / PKIX / hostname | Secret File, ruta sslrootcert, certificado CA y host exacto del pooler |
| No se pudo acceder a Storage | URL del proyecto y secret key; el proyecto debe estar activo |
| El bucket debe ser privado | Crea uno dedicado privado; no reutilices un bucket público |
| Falta BOVEDA_KEY | Ejecuta el script y configura el mismo valor en Render |
| No se pudo verificar integridad después de redeploy | La clave no coincide con la usada al guardar los datos |
| Unsupported class / release version 21 | En local, `mvnw -v` debe usar JDK 21. En Render se usa la imagen JDK 21 |
| Arranque lento al abrir el enlace | El servicio gratuito puede estar despertando; espera y vuelve a entrar |

El plan gratuito tiene memoria y horas compartidas. Render puede suspender servicios tras inactividad y Supabase puede pausar proyectos gratuitos poco usados. No guardamos archivos de producción en el disco efímero de Render. Para exponer, abre el sitio antes y verifica las cuentas y conexiones.

Consulta las condiciones actuales: [Render Free](https://render.com/docs/free), [planes de Render](https://render.com/docs/new-workspace-plans), [pooler Supabase](https://supabase.com/docs/guides/database/connecting-to-postgres), [Supabase pricing](https://supabase.com/pricing).
