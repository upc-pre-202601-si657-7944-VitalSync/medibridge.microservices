# Guia de ejecucion: IAM + Profiles + Health Monitoring + Payments + Gateway

Esta guia asume que estas en:

```text
C:\Users\Sebas\IdeaProjects\medibridge\medibridge.microservices
```

## 1. Servicios incluidos

```text
services/iam-service
services/profiles-service
services/healthmonitoring-service
services/payments-service
services/api-gateway
```

Puertos locales:

```text
api-gateway:              8080
iam-service:              8081
profiles-service:         8082
payments-service:         8083
healthmonitoring-service: 8085
PostgreSQL Docker:        5433
RabbitMQ Docker:          5672
RabbitMQ Management:      15672
```

## 2. Levantar infraestructura local

Desde la raiz del repositorio:

```powershell
docker compose -f docker/docker-compose.yml up -d
```

Validar:

```powershell
docker compose -f docker/docker-compose.yml ps
```

Esperado:

```text
medibridge-postgres   Up ... healthy
medibridge-rabbitmq   Up ... healthy
```

Si tu volumen de PostgreSQL es antiguo y no tiene las databases nuevas, puedes limpiar datos locales:

```powershell
docker compose -f docker/docker-compose.yml down -v
docker compose -f docker/docker-compose.yml up -d
```

Usa `down -v` solo si puedes borrar la data local.

## 3. Levantar servicios en orden

Abre una terminal por servicio.

### 3.1 IAM

```powershell
.\mvnw.cmd -f services/iam-service/pom.xml spring-boot:run
```

Validar:

```powershell
Invoke-RestMethod http://localhost:8081/actuator/health
```

### 3.2 Profiles

```powershell
.\mvnw.cmd -f services/profiles-service/pom.xml spring-boot:run
```

Validar:

```powershell
Invoke-RestMethod http://localhost:8082/actuator/health
```

### 3.3 Health Monitoring

Levanta Health Monitoring antes de crear pacientes/asignaciones nuevas en Profiles, porque consume eventos de Profiles.

```powershell
.\mvnw.cmd -f services/healthmonitoring-service/pom.xml spring-boot:run
```

Validar:

```powershell
Invoke-RestMethod http://localhost:8085/actuator/health
```

### 3.4 Payments

```powershell
.\mvnw.cmd -f services/payments-service/pom.xml spring-boot:run
```

Validar:

```powershell
Invoke-RestMethod http://localhost:8083/actuator/health
```

### 3.5 API Gateway

```powershell
.\mvnw.cmd -f services/api-gateway/pom.xml spring-boot:run
```

Validar:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Swagger consolidado:

```text
http://localhost:8080/swagger-ui.html
```

Debe mostrar:

```text
IAM Service
Profiles Service
Payments Service
Health Monitoring Service
```

## 4. Validar compilacion

Estos comandos ya deben pasar:

```powershell
.\mvnw.cmd -f services/healthmonitoring-service/pom.xml -DskipTests package
.\mvnw.cmd -f services/payments-service/pom.xml -DskipTests package
.\mvnw.cmd -f services/api-gateway/pom.xml -DskipTests package
```

## 5. Flujo manual de prueba

Puedes probar por Swagger directo o por Gateway.

Swagger directo:

```text
IAM:               http://localhost:8081/swagger-ui.html
Profiles:          http://localhost:8082/swagger-ui.html
Payments:          http://localhost:8083/swagger-ui.html
Health Monitoring: http://localhost:8085/swagger-ui.html
Gateway:           http://localhost:8080/swagger-ui.html
```

### 5.1 Crear usuario en IAM

Endpoint:

```text
POST /api/v1/authentication/sign-up
```

Body:

```json
{
  "username": "health-payments-test-1",
  "password": "Password123",
  "roles": [
    "ROLE_USER"
  ]
}
```

Guarda el `id` retornado como `userId`.

### 5.2 Login en IAM

Endpoint:

```text
POST /api/v1/authentication/sign-in
```

Body:

```json
{
  "username": "health-payments-test-1",
  "password": "Password123"
}
```

Copia el `token`.

En Swagger de Profiles, Payments y Health Monitoring, usa:

```text
Bearer <token>
```

### 5.3 Crear paciente en Profiles

Endpoint:

```text
POST /api/v1/profiles/patients
```

Body:

```json
{
  "fullName": "Paciente Health Demo"
}
```

Guarda el `id` como `patientId`.

### 5.4 Crear doctor en Profiles

Endpoint:

```text
POST /api/v1/profiles/doctors
```

Body, reemplazando `1` por el `userId` real:

```json
{
  "userId": 1,
  "fullName": "Doctor Health Demo"
}
```

Guarda el `id` como `doctorProfileId`.

### 5.5 Asignar doctor al paciente

Endpoint:

```text
POST /api/v1/profiles/patients/{patientId}/doctors/{doctorProfileId}
```

Este paso publica:

```text
doctor.assigned.patient
```

Health Monitoring debe consumir ese evento para permitir registrar observaciones.

### 5.6 Registrar observacion en Health Monitoring

Endpoint:

```text
POST /api/v1/health-monitoring/patients/{patientId}/observations
```

Body, reemplazando `recordedByDoctorProfileId`:

```json
{
  "recordedByDoctorProfileId": 1,
  "systolicBloodPressure": 120,
  "diastolicBloodPressure": 80,
  "bodyTemperature": 36.7,
  "painLevel": 2,
  "emotionalState": "CALM",
  "emotionalNotes": "Paciente tranquilo",
  "clinicalNotes": "Control sin hallazgos criticos",
  "recordedAt": "2026-06-10T10:00:00"
}
```

Validar consultas:

```text
GET /api/v1/health-monitoring/patients/{patientId}/observations
GET /api/v1/health-monitoring/patients/{patientId}/alerts/active
GET /api/v1/health-monitoring/patients/{patientId}/summary
```

### 5.7 Crear suscripcion gratuita en Payments

Endpoint:

```text
POST /api/v1/subscriptions
```

Body, reemplazando `1` por el `userId` real:

```json
{
  "userId": 1,
  "commercialLine": "FAMILY",
  "planType": "FREE",
  "billingCycle": "MONTHLY"
}
```

Este flujo no necesita Stripe.

Validar:

```text
GET /api/v1/subscriptions/users/{userId}/active
GET /api/v1/invoices/users/{userId}
```

## 6. Probar por Gateway

Con todos los servicios levantados, puedes usar el gateway:

```text
http://localhost:8080/swagger-ui.html
```

Variables por defecto del gateway:

```text
IAM_SERVICE_URL=http://localhost:8081
PROFILES_SERVICE_URL=http://localhost:8082
PAYMENTS_SERVICE_URL=http://localhost:8083
HEALTHMONITORING_SERVICE_URL=http://localhost:8085
```

Rutas nuevas en gateway:

```text
/api/v1/subscriptions/**
/api/v1/invoices/**
/api/v1/stripe-webhooks
/api/v1/health-monitoring/**
/api/v1/internal/health-monitoring/**
```

Docs nuevas en Swagger consolidado:

```text
/docs/payments/v3/api-docs
/docs/healthmonitoring/v3/api-docs
```

## 7. RabbitMQ

RabbitMQ Management:

```text
http://localhost:15672
```

Credenciales:

```text
usuario: medibridge
password: medibridge
```

Queues esperadas:

```text
payments.user-registered
healthmonitoring.patient-registered
healthmonitoring.patient-deactivated
healthmonitoring.doctor-assigned-patient
iam.subscription-activated
```

Exchange:

```text
medibridge.events
```

## 8. Problemas comunes

### Health Monitoring no encuentra paciente o doctor asignado

Crea paciente, doctor y asignacion despues de levantar `healthmonitoring-service`. Si los creaste antes, Health Monitoring no recibio esos eventos.

### Payments no crea suscripcion

Verifica que `iam-service` este arriba:

```powershell
Invoke-RestMethod http://localhost:8081/actuator/health
```

Payments valida el usuario con:

```text
http://localhost:8081/api/v1/internal/users/{userId}/exists
```

### Planes pagos fallan en Payments

Para planes pagos necesitas:

```text
STRIPE_SECRET_KEY
STRIPE_WEBHOOK_SECRET
```

Para probar sin Stripe usa:

```text
commercialLine=FAMILY
planType=FREE
billingCycle=MONTHLY
```

### Flyway dice que la database no existe

Limpia el volumen local si puedes borrar data:

```powershell
docker compose -f docker/docker-compose.yml down -v
docker compose -f docker/docker-compose.yml up -d
```

El archivo `docker/postgres-init.sql` ya crea:

```text
medibridge_healthmonitoring
medibridge_payments
```
