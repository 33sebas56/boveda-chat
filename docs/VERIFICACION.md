# Verificación del paquete

Revisión realizada el 16 de noviembre de 2026.

| Comprobación | Resultado |
|---|---|
| Construcción del JAR con Java 21 | Correcta |
| Suite sobre H2 | 51 pruebas, 0 fallos, 0 errores, 0 omitidas |
| Misma suite sobre PostgreSQL real 16.2 | 51 pruebas, 0 fallos, 0 errores, 0 omitidas |
| Navegador de escritorio y móvil | 7 recorridos/comprobaciones aprobados |
| JavaScript | Sintaxis validada; recorrido sin errores de página |
| YAML de aplicación, Render y GitHub | Validado como YAML |
| Cobertura de líneas Java en la suite H2 | 87.0% (474 de 545 líneas instrumentadas) |

El recorrido de navegador arrancó el JAR real y comprobó registro, inicio de sesión, chat con dos cuentas, texto escapado, lectura, documentos e imágenes, descarga original, denegación a una tercera cuenta, revocación y pantalla móvil sin desbordamiento horizontal.

Se corrigieron problemas encontrados por las pruebas antes de empaquetar, incluido el registro de descargas dentro de transacciones PostgreSQL de solo lectura. El modo H2 por sí solo no habría detectado esa diferencia.

## Límites de esta verificación

- Supabase y Twilio se probaron con contratos HTTP y respuestas controladas. No se usaron credenciales reales ni se envió un SMS real.
- La configuración de GitHub Actions se incluye, pero su primera ejecución en tu repositorio sucede al publicarlo. Allí se prueba PostgreSQL 17, el navegador, la construcción Docker y Trivy.
- En este entorno no se construyó la imagen Docker ni se ejecutó Trivy. Los escaneos permanecen habilitados y bloquean las severidades definidas; no se presenta un resultado verde inventado.
- El despliegue en tu cuenta Render y la conexión real con tu Supabase requieren tus variables y certificado. No están embebidos en el código.
- La cobertura es una medida del código ejecutado por la suite, no una garantía de seguridad ni una auditoría completa.

La evidencia resumida, sin credenciales ni propiedades del equipo, está en `evidencia-pruebas.json`. Al ejecutar `mvnw verify` obtendrás reportes completos nuevos en tu propio entorno.
