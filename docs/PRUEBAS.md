# Pruebas automáticas

## Ejecutar

```powershell
.\mvnw.cmd clean verify
```

Java 21 y conexión a Maven Central en la primera ejecución. No necesitas Docker ni SMS real para la suite local.

| Conjunto | Qué comprueba |
|---|---|
| CryptoServiceTest | AES-GCM, nonce nuevo, contenido manipulado, contexto/clave incorrectos, persistencia local de clave y huella |
| FileInspectorTest | Tipo real vs extensión, rutas Windows/Unix, rechazo de binarios en TXT y formatos no permitidos |
| RateLimiterTest | Cuotas por cliente y solicitudes concurrentes |
| ChatSecurityIntegrationTest | Login real, BCrypt, CSRF, acceso entre usuarios, envío, permisos de descarga, revocación, consultas parametrizadas, historial y auditoría |
| SmsRegistrationIntegrationTest | Cuenta pendiente bloqueada; solo respuesta aprobada permite acceso |
| TwilioVerifyGatewayTest | Contrato HTTP del proveedor, aprobación, expiración y fallo de credenciales |
| SupabaseStorageTest | Bucket privado, creación del bucket y rutas de objetos autenticadas |

La suite de integración usa las mismas migraciones que producción. H2 es útil para pruebas rápidas; por eso la automatización también arranca un PostgreSQL real y vuelve a ejecutar la suite sobre él.

## PostgreSQL de prueba propio

Usa una base descartable exclusiva para pruebas. No apuntes estas variables a los datos de producción.

```powershell
$env:TEST_DB_URL = 'jdbc:postgresql://localhost:5432/boveda_test'
$env:TEST_DB_USERNAME = 'boveda_test'
$env:TEST_DB_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Contrasena de la base de pruebas' -AsSecureString)).Password
.\mvnw.cmd clean verify
```

## Recorrido de navegador automatizado

GitHub Actions tambien ejecuta `scripts/test-ui.py`. Arranca el JAR real con una base temporal y prueba registro, login, dos participantes, lectura, archivos, tercero sin permiso, revocacion, auditoria y pantalla movil.

Para repetirlo manualmente despues de construir el JAR:

```powershell
python -m pip install -r scripts/ui-requirements.txt
python -m playwright install chromium
python scripts/test-ui.py
```

Python y Playwright solo son necesarios para este recorrido de pruebas, no para ejecutar ni desplegar la aplicacion.

## Resultados

- JUnit/XML y texto: `target/surefire-reports/`.
- Cobertura HTML JaCoCo: `target/site/jacoco/index.html`.
- GitHub Actions conserva esos resultados como artefactos y además `trivy-image.json` y `trivy-source.json`.
- El estado de la verificación ejecutada para este paquete se documenta en `VERIFICACION.md`.

## Lo que una prueba sustituida no demuestra

Las pruebas HTTP de Supabase y Twilio usan respuestas controladas. Comprueban solicitudes y manejo de respuestas, pero no prueban tus credenciales, la entrega de un SMS ni la disponibilidad de tus cuentas. Eso se verifica después de configurar el despliegue.

Trivy revisa CVEs de dependencias/imagen, secretos y configuraciones. No sustituye una revisión completa del código, un pentest ni garantiza ausencia de vulnerabilidades. La política bloquea HIGH/CRITICAL con parche disponible; las vulnerabilidades sin parche siguen necesitando evaluación aunque `--ignore-unfixed` las excluya de esa puerta. Los límites de tasa de la aplicación están diseñados para una sola instancia y su estado en memoria se reinicia al redeploy.

## Recorrido manual de aceptación

1. Abre dos perfiles de navegador. Registra a Ana y Juan con nombres únicos.
2. Ana busca a Juan y abre una conversación. Envía texto y una imagen.
3. Juan recibe el mensaje y lo abre; Ana ve la confirmación de lectura.
4. Ana sube un TXT o PDF y lo comparte. Juan descarga el original.
5. Una tercera cuenta intenta abrir la URL del documento: recibe 404.
6. Ana retira el acceso; Juan ya no puede descargarlo. Ana conserva acceso.
7. Prueba un archivo de más de 5 MB y un ejecutable renombrado como PNG.
8. Entra como administrador y consulta los eventos.
9. En producción, recarga después de un redeploy y comprueba que mensajes y archivos persisten.
10. Si activas SMS, completa además el recorrido de `SMS.md`.

No ejecutes pruebas de saturación o escaneos contra la infraestructura compartida de Render. Las pruebas de permisos trabajan con la propia aplicación y cuentas autorizadas.
