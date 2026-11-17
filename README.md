# Bóveda Chat

Bóveda Chat es una aplicación web académica para conversar y compartir documentos con acceso controlado. Integra una interfaz web con un backend en Java y servicios en la nube para el almacenamiento y la persistencia de datos.

## Funcionalidades

- Registro e inicio de sesión con usuario y contraseña.
- Conversaciones privadas entre dos participantes.
- Bóveda personal para guardar y descargar documentos e imágenes.
- Envío de archivos en conversaciones y control de acceso para las personas autorizadas.
- Registro de eventos y consulta de auditoría para el administrador.

## Seguridad

Las contraseñas se almacenan mediante **BCrypt**. Los mensajes y archivos se cifran en el servidor con **AES-256-GCM**; los archivos también cuentan con una huella **SHA-256** que se comprueba al descargarlos.

La aplicación valida los permisos en el servidor y utiliza protección CSRF, cabeceras de seguridad y límites de solicitudes. El almacenamiento en Supabase se configura mediante un bucket privado.

El cifrado se realiza en el servidor; el proyecto no implementa cifrado de extremo a extremo.

## Tecnologías

| Área | Tecnologías |
| --- | --- |
| Backend | Java 21, Spring Boot, Spring Security, Spring Data JPA |
| Interfaz | Thymeleaf, HTML, CSS y JavaScript |
| Datos | PostgreSQL, Flyway y H2 para ejecución local |
| Archivos | Supabase Storage |
| Pruebas | JUnit, MockMvc, Mockito y JaCoCo |
| Automatización y despliegue | GitHub Actions, Trivy, Docker y Render |

## Ejecución local

Se requiere JDK 21 o superior. En Windows, desde la carpeta del proyecto:

```powershell
.\mvnw.cmd spring-boot:run
```

En Linux o macOS:

```bash
./mvnw spring-boot:run
```

La aplicación queda disponible en `http://localhost:8080`. El perfil local utiliza H2 y guarda los archivos cifrados en el directorio `.local/`.

## Pruebas

Para ejecutar las pruebas y generar los reportes:

```powershell
.\mvnw.cmd clean verify
```

Los resultados se generan en `target/surefire-reports/` y la cobertura en `target/site/jacoco/`. El repositorio incluye un flujo de GitHub Actions para pruebas con H2 y PostgreSQL, recorrido de interfaz y análisis de seguridad con Trivy.

## Configuración y documentación

El despliegue utiliza variables de entorno para conectar PostgreSQL y Supabase y para administrar la clave de cifrado. Las contraseñas y las claves deben mantenerse fuera del repositorio. La clave de cifrado debe conservarse entre despliegues para poder recuperar la información existente.

- [Despliegue en Render](docs/DESPLIEGUE_RENDER.md)
- [Pruebas](docs/PRUEBAS.md)
- [Seguridad y límites](docs/SEGURIDAD.md)
- [Rutas de la aplicación](docs/API.md)