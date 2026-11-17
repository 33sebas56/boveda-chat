# Modelo de seguridad

## Alcance

Demostración académica de mensajería con autorización, cifrado en servidor y automatización. No se presenta como producto auditado para datos sensibles ni como mensajería con cifrado de extremo a extremo.

## Cifrado

- AES-256-GCM con nonce aleatorio de 12 bytes para cada cifrado y etiqueta de autenticación de 128 bits.
- El sobre contiene versión, nonce y contenido autenticado. Usa contexto AAD para asociar archivos a su ID y mensajes a conversación/remitente.
- Archivos cifrados en Storage; mensajes cifrados en PostgreSQL. Nombres, tamaños, participantes y fechas son metadatos y permanecen consultables.
- SHA-256 se calcula sobre el archivo original y se contrasta tras descifrar. La etiqueta GCM autentica criptográficamente el contenido; el hash por sí solo no impediría que un atacante con acceso de escritura cambiara contenido y huella a la vez.
- BOVEDA_KEY está en el servidor. Quien controle ese servidor y su clave puede descifrar; por eso no es E2EE. No hay rotación de claves incorporada: conserva la misma clave y planifica una migración antes de cambiarla.

## Identidad, permisos y sesiones

- Contraseñas BCrypt con costo 12; longitud mínima 12 y máximo 72 bytes para evitar truncamiento de BCrypt.
- SMS opcional verifica el celular en el registro; no constituye MFA en cada login.
- Roles USER y ADMIN. Ambos deben ser participantes para acceder a un chat: el rol ADMIN no evita esta comprobación.
- El destinatario de un adjunto queda autorizado por su participación en el chat donde el propietario lo compartió. Solo el propietario puede compartir ese objeto o cambiar su revocación.
- Revocar acceso no retira copias que ya estén en otro dispositivo.
- Cookies HttpOnly/SameSite Lax; Secure en prod; sesión de 30 minutos y protección de fijación de sesión de Spring Security.
- CSRF activo en formularios y API. El frontend obtiene el token de la vista y lo adjunta en cambios.
- Contenido del usuario se inserta con textContent; las vistas usan escape de Thymeleaf. La política CSP evita scripts inline y objetos.

## Archivos y almacenamiento

Solo PDF, PNG, JPG y TXT; tipo derivado del contenido, límite de tamaño y de píxeles en imágenes, cuota por cuenta y nombres de objeto aleatorios. Los PDF/TXT se descargan como adjuntos; imágenes autorizadas pueden servirse inline con MIME derivado y nosniff. No se incluye un antivirus: comprobar cabecera y extensión no garantiza que un PDF esté libre de malware.

Supabase exige bucket privado. La secret key permanece en el servidor y puede omitir RLS, de modo que los permisos de la aplicación son obligatorios. El esquema boveda_chat no necesita exponerse por la Data API. Usa un usuario PostgreSQL dedicado con privilegios sobre ese esquema si administras el entorno con varias aplicaciones; el usuario propietario del proyecto tiene privilegios más amplios.

Objetos y PostgreSQL no participan en una transacción distribuida. El código limpia subidas si falla la transacción y borra objetos después de confirmar la eliminación de metadatos. Si falla la limpieza externa, puede quedar un objeto cifrado huérfano identificado en el log; requiere revisión administrativa. Los adjuntos ya compartidos conservan su metadato y se revocan en lugar de eliminar referencias del historial.

## Operación

Rate limiting básico en memoria, presupuesto SMS diario persistido, auditoría sin cuerpos de mensajes ni códigos. Una instancia es el alcance previsto. Para varias instancias se requieren sesiones y límites compartidos, así como controles adicionales de abuso y protección de infraestructura.

El proveedor termina TLS del navegador. JDBC usa verify-full con CA y Storage usa HTTPS. Docker corre como usuario sin privilegios. Actuator expone solo health sin detalles. GitHub Actions no necesita secretos de producción.

No se han configurado VLAN, WiFi, NAC ni un firewall de red propio mediante este proyecto. El despliegue propuesto tampoco ejecuta un Nginx administrado por nosotros. Las cabeceras y límites aplicados por Spring corresponden a la capa de aplicación.

## Datos de otras aplicaciones

El nombre de esquema y bucket reduce interferencia accidental, pero compartir proyecto también comparte cuotas, administración y parte del impacto de una caída. No alteres políticas de los buckets existentes. Este proyecto no migra la antigua bóveda ni sus claves automáticamente.
