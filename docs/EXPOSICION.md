# Exponer Bóveda Chat

## Inicio: el problema (1 minuto)

“Necesitábamos compartir mensajes y documentos entre personas sin que conocer una dirección de descarga diera acceso al archivo. Partimos de una bóveda personal y agregamos conversaciones privadas, permisos y registro de actividad.”

Abre el sitio antes de la presentación para despertar los servicios gratuitos. Ten dos cuentas en perfiles separados y una tercera para probar denegación. Usa archivos pequeños sin información sensible. Si no activaste SMS, presenta esa parte como integración opcional probada con respuestas controladas, no como un SMS real recibido.

## Demostración (4 a 6 minutos)

1. Ana busca a Juan por usuario y abre su chat. Envía un mensaje.
2. Ana sube una imagen y un documento. Juan abre el chat y descarga el documento.
3. Muestra la huella del archivo y explica que el servidor lo cifra antes de guardarlo.
4. Copia el enlace e intenta abrirlo con la tercera cuenta. El sistema no permite acceder.
5. Ana retira acceso. Juan tampoco puede hacer otra descarga; una copia ya descargada no puede borrarse de su equipo.
6. Entra como administrador y revisa LOGIN_OK, FILE_UPLOADED, FILE_SHARED y DOWNLOAD_DENIED.
7. Abre GitHub Actions: muestra los tests H2/PostgreSQL, la imagen y el reporte de Trivy. Explica un caso de prueba relevante.

## Temas de los PDF y aplicación concreta

| Material del curso | Aplicación en el proyecto | Evidencia |
|---|---|---|
| Semana 1, cifrado y hashes | AES-GCM en mensajes/archivos; SHA-256 de archivos | CryptoServiceTest y archivo recuperado |
| Semana 2, tráfico y protocolos | HTTPS y validación del certificado de PostgreSQL | Certificado del sitio y configuración JDBC |
| Semana 4, IAM/AAA y Zero Trust | Identidad, permisos por recurso y auditoría | Usuario ajeno recibe 404; registro de eventos |
| Semana 5, bastionado web | Cabeceras, CSP, nosniff y errores sin stacktrace | Test de cabeceras / herramientas de navegador |
| Semana 7, DevSecOps y nube | Docker en la nube, Actions, Trivy y despliegue tras checks | Pipeline y reportes |
| Semana 8, auditoría de vulnerabilidades | Pruebas de acceso, entrada SQL y validación de archivos | Suite de integración |
| Semanas 9 y 10, disponibilidad/mitigación | Límites de intentos y cuotas como medidas parciales | Respuesta 429 y RateLimiterTest |

La aplicación permite explicar muchos conceptos del curso. Eso no demuestra automáticamente un porcentaje de cobertura de toda la materia; debe contrastarse con la rúbrica del profesor. VLAN, WiFi, NAC, Nmap y laboratorios de DoS no fueron implementados por tener roles o rate limiting en Spring.

## Preguntas que pueden hacerte

**¿Por qué el frontend está integrado?** Reduce la cantidad de servicios y facilita mantener una sola aplicación desplegada. Thymeleaf sirve las páginas y JavaScript actualiza el chat mediante peticiones cada cinco segundos.

**¿Por qué no WebSocket?** Elegimos actualización periódica para una demostración pequeña; mantiene el alcance y permite usar sesiones HTTP convencionales. WebSocket sería una mejora si necesitamos menor latencia y más tráfico.

**¿Por qué Supabase?** PostgreSQL conserva datos relacionales y Storage conserva blobs; ambos sobreviven a reinicios de Render. El esquema y el bucket son propios del proyecto.

**¿Es cifrado extremo a extremo?** No. Ciframos al guardar y usamos HTTPS en tránsito; el servidor tiene la clave y puede descifrar. Los metadatos siguen visibles para la operación del sistema.

**¿El código al celular es doble factor?** Verifica control del número durante el registro. Para llamarlo MFA en cada inicio habría que exigir un segundo factor también al iniciar sesión.

**¿Qué prueba vale más?** La que intenta acceder con la tercera cuenta: demuestra autorización en el backend, incluso cuando alguien copia la URL o modifica una solicitud.

**¿Trivy garantiza que es segura?** No. Busca vulnerabilidades conocidas y problemas de configuración/secretos; las pruebas de negocio y permisos complementan ese análisis.

## Cierre (30 segundos)

“La seguridad se aplica en varias decisiones: autenticar a la persona, autorizar cada recurso, proteger lo almacenado, registrar acciones y repetir comprobaciones cuando cambia el código. El siguiente paso sería reforzar recuperación de cuentas, gestión de claves y controles para un uso real.”
