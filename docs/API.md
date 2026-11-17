# Rutas principales

Todos los cambios requieren token CSRF. Las rutas `/api` requieren sesión; sin sesión responden 401. Los archivos o conversaciones que no existen o no pertenecen al usuario responden 404.

| Método | Ruta | Uso |
|---|---|---|
| GET/POST | /registro | Crear cuenta |
| GET/POST | /login | Formulario / autenticación |
| POST | /logout | Cerrar sesión |
| GET/POST | /verificar | Formulario / comprobar código SMS |
| POST | /verificar/reenviar | Reenviar a la cuenta pendiente de esta sesión |
| GET/POST | /verificar/retomar | Retomar usando usuario y contraseña |
| GET | /app | Interfaz integrada |
| GET | /api/me | Identidad de la sesión, sin teléfono ni hash |
| GET | /api/people?q=ana | Buscar por prefijo de usuario |
| GET/POST | /api/conversations | Listar / iniciar conversación por username |
| GET | /api/conversations/{id}/messages?after=0&before=0 | Historial de 50 mensajes; un solo cursor a la vez |
| POST | /api/conversations/{id}/messages | Enviar `{text,attachmentId}`; al menos uno |
| POST | /api/conversations/{id}/read | Confirmar hasta `{lastSeenId}` |
| GET/POST | /api/files | Bóveda / subir multipart con campo `file` |
| GET | /api/files/{id}/download | Descargar original autorizado |
| GET | /documentos/{id}/descargar | Descarga desde la interfaz; muestra página HTML si se deniega |
| GET | /api/files/{id}/preview | Mostrar imagen autorizada |
| PATCH | /api/files/{id}/sharing | Cambiar `{revoked:true/false}` como propietario |
| DELETE | /api/files/{id} | Eliminar archivo propio que no se compartió |
| GET | /admin/auditoria | Últimos 200 eventos, rol ADMIN |
| GET | /actuator/health | Estado de salud público, sin detalles |
