# API Gateway

Servicio de entrada publica para los microservicios MediBridge. Enruta las APIs REST publicas de `iam-service` y `profiles-service`, bloquea los endpoints internos y expone una vista Swagger UI con los contratos OpenAPI de ambos servicios.

## Rutas publicas

- `POST /api/v1/authentication/sign-up` -> `iam-service`
- `POST /api/v1/authentication/sign-in` -> `iam-service`
- `GET /api/v1/users/**` -> `iam-service`
- `GET /api/v1/roles/**` -> `iam-service`
- `GET /api/v1/jwks/**` -> `iam-service`
- `/api/v1/profiles/**` -> `profiles-service`

El gateway responde `403` para `/api/v1/internal/**`. Esos endpoints deben quedar solo para comunicacion servicio-a-servicio dentro de la red privada.

## Swagger

- Gateway Swagger UI: `http://localhost:8080/swagger-ui.html`
- IAM OpenAPI proxied: `http://localhost:8080/docs/iam/v3/api-docs`
- Profiles OpenAPI proxied: `http://localhost:8080/docs/profiles/v3/api-docs`

## Variables externas

- `IAM_SERVICE_URL`: por defecto `http://localhost:8081`
- `PROFILES_SERVICE_URL`: por defecto `http://localhost:8082`
- `GATEWAY_CORS_ALLOWED_ORIGINS`: por defecto `*`
- `INTERNAL_SERVICE_TOKEN`: por defecto `local-internal-token`
- `SERVER_PORT` o `PORT`: por defecto `8080`

En Render, `IAM_SERVICE_URL` y `PROFILES_SERVICE_URL` deben usar los Internal URLs o Service Address exactos de los servicios privados.
El mismo `INTERNAL_SERVICE_TOKEN` debe configurarse en `api-gateway`, `iam-service` y `profiles-service`.

## Build

Desde la raiz del workspace:

```powershell
.\mvnw.cmd -f services/api-gateway/pom.xml -DskipTests package
```

## Run local

Levanta primero `iam-service` y `profiles-service`. Luego:

```powershell
.\mvnw.cmd -f services/api-gateway/pom.xml spring-boot:run
```
