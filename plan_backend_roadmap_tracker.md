# Plan de Implementación — Backend Roadmap Tracker

> **Proyecto:** Roadmap Tracker Backend  
> **Stack:** Java 21 + Spring Boot + Maven + PostgreSQL + Docker  
> **Objetivo:** Backend simple, seguro y ligero para guardar el progreso del roadmap desde cualquier dispositivo.  
> **Alcance:** Uso personal, sin multi-tenant, con acceso restringido a 3 correos permitidos.

---

## 1. Objetivo del backend

Construir una API sencilla que permita:

- Iniciar sesión con un correo permitido.
- Validar que solo 3 correos autorizados puedan acceder.
- Guardar el estado del tracker.
- Recuperar el último estado guardado.
- Mantener bajo consumo de recursos en VPS.
- Correr con Docker Compose junto con PostgreSQL.
- Exponer la API detrás de Nginx o directamente por puerto interno.

La prioridad es simplicidad y estabilidad.

No se busca construir una plataforma SaaS, ni multi-tenant, ni un sistema complejo de usuarios.

---

## 2. Arquitectura general

```text
React/Vite Frontend
    ↓ HTTPS
Nginx / Reverse Proxy
    ↓ /api
Spring Boot Backend
    ↓
PostgreSQL
```

---

## 3. Responsabilidades del backend

```text
1. Autenticación simple.
2. Validación de correos permitidos.
3. Generación de JWT.
4. Persistencia del estado del tracker en JSONB.
5. Consulta del estado guardado.
6. Health check.
7. Configuración CORS.
8. Logs básicos.
```

---

## 4. Decisión de autenticación

## Login recomendado

Usar:

```text
Email + Password
```

Con estas reglas:

```text
- Solo pueden iniciar sesión correos declarados en configuración.
- Las contraseñas se guardan hasheadas con BCrypt.
- Al iniciar sesión correctamente, el backend devuelve un JWT.
- El frontend guarda el JWT y lo manda en Authorization Bearer.
```

### ¿Por qué no magic links?

Magic links son cómodos, pero requieren configurar envío de correo, tokens temporales, expiración y más piezas.

Para un tracker personal, eso es más complejidad de la necesaria.

### ¿Por qué no OAuth con Google?

También es viable, pero implica configurar proyecto en Google Cloud, client IDs, callbacks y validación de tokens.

Para este MVP, la ruta más simple es email + password + allowlist.

---

## 5. Correos permitidos

Los correos permitidos deben vivir en variables de entorno.

Ejemplo:

```env
APP_ALLOWED_EMAILS=correo1@gmail.com,correo2@gmail.com,correo3@gmail.com
```

Solo esos correos pueden existir como usuarios válidos.

Si alguien intenta login con otro correo:

```json
{
  "error": "Unauthorized email"
}
```

---

## 6. Estrategia de usuarios

Como no será multi-tenant, basta con una tabla simple de usuarios.

```sql
CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    email VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### Creación inicial de usuarios

Hay dos opciones.

## Opción A — Seed manual en base de datos

Generar hashes BCrypt y hacer inserts manuales.

Ventaja:

```text
Simple.
No hay lógica extra en el backend.
```

Desventaja:

```text
Menos cómodo para cambiar contraseñas.
```

## Opción B — Seed automático desde variables de entorno

Variables:

```env
APP_USER_1_EMAIL=correo1@gmail.com
APP_USER_1_PASSWORD_HASH=$2a$10$...

APP_USER_2_EMAIL=correo2@gmail.com
APP_USER_2_PASSWORD_HASH=$2a$10$...

APP_USER_3_EMAIL=correo3@gmail.com
APP_USER_3_PASSWORD_HASH=$2a$10$...
```

Al arrancar, el backend crea o actualiza esos usuarios.

Recomendación para este proyecto:

```text
Usar opción B.
```

Es más cómoda para VPS y mantiene el sistema simple.

---

## 7. Persistencia del tracker

El tracker actual maneja un estado tipo JSON:

```json
{
  "statuses": {},
  "notes": {},
  "metrics": {
    "leetcode": 0,
    "ocpTopics": 0,
    "ddiaChapters": 0,
    "artifacts": 0,
    "mocks": 0,
    "applications": 0
  },
  "selectedWeek": 1,
  "selectedPhase": 0,
  "lastUpdate": null
}
```

Ese estado se guardará como JSONB en PostgreSQL.

---

## 8. Tabla principal

```sql
CREATE TABLE roadmap_progress (
    id VARCHAR(64) PRIMARY KEY,
    state JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

Como solo hay un roadmap principal, se puede usar:

```text
id = "main-roadmap"
```

No hace falta asociarlo a usuario.

Aunque entren tres correos distintos, todos ven y actualizan el mismo progreso.

---

## 9. Tabla opcional de auditoría

No es obligatoria, pero puede ser útil.

```sql
CREATE TABLE roadmap_progress_history (
    id UUID PRIMARY KEY,
    progress_id VARCHAR(64) NOT NULL,
    updated_by VARCHAR(180) NOT NULL,
    state JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### Recomendación

Para el MVP:

```text
No crear historial todavía.
```

Mantenerlo simple.

Después se puede agregar como mejora.

---

## 10. Endpoints

## Auth

### POST `/api/auth/login`

Request:

```json
{
  "email": "correo1@gmail.com",
  "password": "password"
}
```

Response:

```json
{
  "accessToken": "jwt",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "email": "correo1@gmail.com"
}
```

Errores:

```http
401 Unauthorized
```

---

### GET `/api/auth/me`

Headers:

```http
Authorization: Bearer <token>
```

Response:

```json
{
  "email": "correo1@gmail.com",
  "authenticated": true
}
```

---

## Roadmap Progress

### GET `/api/roadmap/progress`

Headers:

```http
Authorization: Bearer <token>
```

Response:

```json
{
  "id": "main-roadmap",
  "state": {
    "statuses": {},
    "notes": {},
    "metrics": {
      "leetcode": 0,
      "ocpTopics": 0,
      "ddiaChapters": 0,
      "artifacts": 0,
      "mocks": 0,
      "applications": 0
    },
    "selectedWeek": 1,
    "selectedPhase": 0,
    "lastUpdate": null
  },
  "updatedAt": "2026-05-17T20:00:00Z"
}
```

---

### PUT `/api/roadmap/progress`

Headers:

```http
Authorization: Bearer <token>
```

Request:

```json
{
  "state": {
    "statuses": {
      "1-project": "done",
      "1-java": "partial"
    },
    "notes": {
      "1": "Configuré el proyecto y avancé con Java/OCP."
    },
    "metrics": {
      "leetcode": 3,
      "ocpTopics": 2,
      "ddiaChapters": 1,
      "artifacts": 1,
      "mocks": 0,
      "applications": 0
    },
    "selectedWeek": 1,
    "selectedPhase": 0,
    "lastUpdate": "2026-05-17T20:00:00Z"
  }
}
```

Response:

```json
{
  "id": "main-roadmap",
  "state": {
    "statuses": {
      "1-project": "done",
      "1-java": "partial"
    },
    "notes": {
      "1": "Configuré el proyecto y avancé con Java/OCP."
    },
    "metrics": {
      "leetcode": 3,
      "ocpTopics": 2,
      "ddiaChapters": 1,
      "artifacts": 1,
      "mocks": 0,
      "applications": 0
    },
    "selectedWeek": 1,
    "selectedPhase": 0,
    "lastUpdate": "2026-05-17T20:00:00Z"
  },
  "updatedAt": "2026-05-17T20:00:00Z"
}
```

---

## Health

### GET `/api/health`

Response:

```json
{
  "status": "UP"
}
```

Este endpoint puede quedar público.

---

## 11. Paquetes sugeridos

```text
com.sebastian.roadmaptracker
├── RoadmapTrackerApplication.java
├── config
│   ├── SecurityConfig.java
│   ├── CorsConfig.java
│   └── JwtProperties.java
├── auth
│   ├── AuthController.java
│   ├── AuthService.java
│   ├── JwtService.java
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   └── AuthenticatedUserResponse.java
├── user
│   ├── AppUser.java
│   ├── AppUserRepository.java
│   └── UserSeeder.java
├── roadmap
│   ├── RoadmapProgress.java
│   ├── RoadmapProgressRepository.java
│   ├── RoadmapProgressService.java
│   ├── RoadmapProgressController.java
│   ├── ProgressRequest.java
│   └── ProgressResponse.java
├── common
│   ├── ApiError.java
│   ├── GlobalExceptionHandler.java
│   └── TimeProvider.java
└── health
    └── HealthController.java
```

---

## 12. Dependencias Maven

Usar Spring Initializr con:

```text
- Spring Web
- Spring Security
- Spring Data JPA
- PostgreSQL Driver
- Validation
- Lombok
- Actuator
- Flyway Migration
```

Dependencia adicional para JWT:

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>

<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>

<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

---

## 13. Manejo de JSONB

Para simplificar, se puede guardar el `state` como `String` JSON validado.

Entidad:

```java
@Entity
@Table(name = "roadmap_progress")
public class RoadmapProgress {

    @Id
    private String id;

    @Column(name = "state", nullable = false, columnDefinition = "jsonb")
    private String state;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
```

Ventaja:

```text
Simple.
Evita pelearse con mapeos avanzados de JSONB.
El frontend ya maneja el shape del JSON.
```

Validación mínima:

```text
- El body debe traer "state".
- El JSON no debe ser null.
- Tamaño máximo razonable, por ejemplo 1 MB.
```

---

## 14. Seguridad con Spring Security

Reglas:

```text
POST /api/auth/login       público
GET  /api/health           público
GET  /actuator/health      público

Todo lo demás:
  requiere JWT válido
```

### CORS

Permitir solo el dominio del frontend:

```env
APP_CORS_ALLOWED_ORIGINS=https://tracker.tudominio.com,http://localhost:5173
```

---

## 15. Variables de entorno

```env
SPRING_PROFILES_ACTIVE=prod

SERVER_PORT=8080

DB_HOST=postgres
DB_PORT=5432
DB_NAME=roadmap_tracker
DB_USER=roadmap_user
DB_PASSWORD=change_me

JWT_SECRET=change_me_super_long_random_secret
JWT_EXPIRATION_SECONDS=86400

APP_CORS_ALLOWED_ORIGINS=https://tracker.tudominio.com,http://localhost:5173

APP_USER_1_EMAIL=correo1@gmail.com
APP_USER_1_PASSWORD_HASH=$2a$10$...

APP_USER_2_EMAIL=correo2@gmail.com
APP_USER_2_PASSWORD_HASH=$2a$10$...

APP_USER_3_EMAIL=correo3@gmail.com
APP_USER_3_PASSWORD_HASH=$2a$10$...
```

---

## 16. Docker Compose recomendado

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: roadmap-postgres
    environment:
      POSTGRES_DB: roadmap_tracker
      POSTGRES_USER: roadmap_user
      POSTGRES_PASSWORD: change_me
    volumes:
      - roadmap_pg_data:/var/lib/postgresql/data
    ports:
      - "5434:5432"
    restart: unless-stopped

  roadmap-api:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: roadmap-api
    env_file:
      - .env
    depends_on:
      - postgres
    ports:
      - "8088:8080"
    restart: unless-stopped

volumes:
  roadmap_pg_data:
```

### Nota

Usar puerto externo `8088` para evitar conflictos en tu VPS.

Nginx puede apuntar a:

```text
http://localhost:8088
```

---

## 17. Dockerfile

```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN ./mvnw dependency:go-offline

COPY src src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 18. Flyway migrations

### `V1__create_app_user.sql`

```sql
CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    email VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### `V2__create_roadmap_progress.sql`

```sql
CREATE TABLE roadmap_progress (
    id VARCHAR(64) PRIMARY KEY,
    state JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### `V3__insert_default_progress.sql`

```sql
INSERT INTO roadmap_progress (id, state)
VALUES (
    'main-roadmap',
    '{
      "statuses": {},
      "notes": {},
      "metrics": {
        "leetcode": 0,
        "ocpTopics": 0,
        "ddiaChapters": 0,
        "artifacts": 0,
        "mocks": 0,
        "applications": 0
      },
      "selectedWeek": 1,
      "selectedPhase": 0,
      "lastUpdate": null
    }'::jsonb
)
ON CONFLICT (id) DO NOTHING;
```

---

## 19. Implementación por fases

## Fase 1 — Bootstrap del proyecto

Objetivo:

```text
Crear proyecto Spring Boot base.
```

Tareas:

```text
- Crear proyecto Maven con Java 21.
- Agregar dependencias.
- Configurar application.yml.
- Agregar Dockerfile.
- Agregar docker-compose.yml.
- Levantar PostgreSQL.
- Validar /actuator/health.
```

Criterio de salida:

```text
Backend levanta en local y responde health check.
```

---

## Fase 2 — Base de datos y Flyway

Objetivo:

```text
Persistencia lista.
```

Tareas:

```text
- Configurar datasource.
- Agregar Flyway.
- Crear migraciones.
- Validar tablas app_user y roadmap_progress.
- Insertar estado inicial main-roadmap.
```

Criterio de salida:

```text
La base se crea automáticamente al levantar el proyecto.
```

---

## Fase 3 — Auth

Objetivo:

```text
Login funcional para 3 correos permitidos.
```

Tareas:

```text
- Crear AppUser entity.
- Crear AppUserRepository.
- Crear UserSeeder desde env vars.
- Configurar PasswordEncoder BCrypt.
- Crear AuthController.
- Crear AuthService.
- Crear JwtService.
- Crear SecurityConfig.
- Proteger endpoints.
```

Criterio de salida:

```text
POST /api/auth/login devuelve JWT solo si el correo está permitido y la contraseña es correcta.
```

---

## Fase 4 — Progress API

Objetivo:

```text
Guardar y leer estado del tracker.
```

Tareas:

```text
- Crear RoadmapProgress entity.
- Crear repository.
- Crear service.
- Crear controller.
- Implementar GET /api/roadmap/progress.
- Implementar PUT /api/roadmap/progress.
- Validar que requiere JWT.
```

Criterio de salida:

```text
El frontend puede recuperar y guardar estado.
```

---

## Fase 5 — CORS, errores y logs

Objetivo:

```text
Dejar API lista para frontend real.
```

Tareas:

```text
- Configurar CORS con dominios permitidos.
- Agregar GlobalExceptionHandler.
- Estandarizar errores.
- Agregar logs básicos.
- Validar con curl o Postman.
```

Criterio de salida:

```text
Frontend puede consumir API desde dominio autorizado.
```

---

## Fase 6 — Deploy en VPS

Objetivo:

```text
Backend corriendo estable en VPS.
```

Tareas:

```text
- Crear .env de producción.
- Levantar docker compose.
- Configurar Nginx reverse proxy.
- Habilitar HTTPS.
- Validar login desde navegador.
- Validar persistencia.
```

Criterio de salida:

```text
API disponible detrás de HTTPS.
```

---

## 20. Nginx sugerido

Ejemplo si el frontend vive en el mismo dominio:

```nginx
server {
    server_name tracker.tudominio.com;

    root /var/www/roadmap-tracker;
    index index.html;

    location / {
        try_files $uri /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8088/api/;
        proxy_http_version 1.1;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Después:

```bash
sudo certbot --nginx -d tracker.tudominio.com
```

---

## 21. Consumo de recursos esperado

Este proyecto debe ser ligero.

```text
Spring Boot:
  256 MB - 512 MB RAM recomendado

PostgreSQL:
  128 MB - 256 MB para uso personal

Total esperado:
  400 MB - 800 MB dependiendo del VPS y configuración
```

Para reducir consumo:

```text
- No agregar Redis.
- No agregar servicios innecesarios.
- No usar multi-tenant.
- No usar colas.
- No usar workers.
- Mantener una sola API.
- Guardar snapshots JSON.
```

---

## 22. Pruebas mínimas

### Unitarias

```text
- AuthService login correcto.
- AuthService rechaza correo no permitido.
- JwtService genera y valida token.
- RoadmapProgressService guarda estado.
```

### Integración

```text
- Login + GET progress.
- Login + PUT progress + GET progress.
- Request sin token devuelve 401.
- Request con token inválido devuelve 401.
```

---

## 23. Contratos frontend-backend

El frontend espera:

```text
- POST /api/auth/login
- GET /api/auth/me
- GET /api/roadmap/progress
- PUT /api/roadmap/progress
```

El backend no necesita conocer el significado interno de todo el tracker.

Solo guarda y devuelve el JSON.

Eso permite cambiar el frontend sin migrar tablas cada dos días.

---

## 24. Mejoras futuras

No implementar al inicio, pero dejar anotadas:

```text
- Historial de cambios.
- Exportar progreso en Markdown.
- Reporte mensual automático.
- Recuperación de contraseña.
- Login con Google.
- Streak real por día.
- Gráficas avanzadas.
- WebSocket/SSE si algún día hace falta.
```

---

## 25. Resumen brutal

```text
Backend MVP:
- Spring Boot
- PostgreSQL
- Flyway
- Spring Security
- JWT
- 3 usuarios permitidos
- 1 tabla de progreso JSONB
- 2 endpoints principales
- Docker Compose
- Nginx + HTTPS
```

No más.

La meta no es construir otro monstruo enterprise.

La meta es que el tracker funcione, guarde progreso y no moleste.

