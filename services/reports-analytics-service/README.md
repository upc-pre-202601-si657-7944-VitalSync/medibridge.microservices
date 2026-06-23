# Reports and Analytics Service

Microservicio de reportes y analitica de MediBridge. Es responsable de generar reportes clinicos, dashboards analiticos y consumir eventos relevantes de otros bounded contexts.

## Endpoints principales

- `POST /api/v1/clinical-reports`
- `POST /api/v1/clinical-reports/{reportId}/pdf`
- `GET /api/v1/clinical-reports/{reportId}`
- `GET /api/v1/clinical-reports/{reportId}/pdf`
- `GET /api/v1/clinical-reports/patients/{patientId}`
- `GET /api/v1/analytics-dashboards/patients/{patientId}`

## Integraciones

- Valida pacientes contra `profiles-service` por Feign.
- Consulta resumen clinico de `healthmonitoring-service` por Feign.
- Consulta resumen de medicamentos de `medication-service` por Feign.
- Consulta resumen de citas de `appointments-service` por Feign.
- Valida JWT usando el JWK publico de `iam-service`.
- Consume eventos RabbitMQ desde `medibridge.events`:
  - `appointment.scheduled`
  - `observation.recorded`
  - `alert.critical.triggered`
  - `medication.registered`
  - `dose.administered`
  - `dose.skipped`
  - `stock.low`
- Publica eventos RabbitMQ en `medibridge.events`:
  - `clinical-report.generated`

## Variables externas

- `REPORTS_DB_URL`: por defecto `jdbc:postgresql://localhost:5433/medibridge_reports`
- `REPORTS_DB_USERNAME`: por defecto `postgres`
- `REPORTS_DB_PASSWORD`: por defecto `12345678`
- `IAM_JWK_SET_URI`: por defecto `http://localhost:8081/api/v1/jwks/.well-known/jwks.json`
- `PROFILES_SERVICE_URL`: por defecto `http://localhost:8082`
- `APPOINTMENTS_SERVICE_URL`: por defecto `http://localhost:8084`
- `HEALTHMONITORING_SERVICE_URL`: por defecto `http://localhost:8085`
- `MEDICATION_SERVICE_URL`: por defecto `http://localhost:8086`
- `INTERNAL_SERVICE_TOKEN`: token compartido que debe enviar el API Gateway y las llamadas Feign internas.
- `RABBITMQ_HOST`: por defecto `localhost`
- `RABBITMQ_PORT`: por defecto `5672`
- `RABBITMQ_USER`: por defecto `medibridge`
- `RABBITMQ_PASSWORD`: por defecto `medibridge`
- `RABBITMQ_VHOST`: por defecto `/`

## Acceso

Los endpoints de negocio y Swagger estan protegidos por `X-Internal-Token`.

En local, consumelos por el API Gateway:

- `http://localhost:8080/api/v1/clinical-reports/**`
- `http://localhost:8080/api/v1/analytics-dashboards/**`
- `http://localhost:8080/docs/reports-analytics/v3/api-docs`

El acceso directo a `http://localhost:8087/swagger-ui.html` o a endpoints de negocio debe devolver `403` si no envias el token interno.

## Build

Desde la raiz del workspace:

```powershell
.\mvnw.cmd -f services/reports-analytics-service/pom.xml -DskipTests package
```

## Run local

```powershell
.\mvnw.cmd -f services/reports-analytics-service/pom.xml spring-boot:run
```
