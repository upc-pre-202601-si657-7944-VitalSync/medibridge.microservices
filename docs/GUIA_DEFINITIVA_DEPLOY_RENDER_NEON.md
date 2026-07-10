# Guia definitiva de deploy MediBridge en Render, Vercel, Neon y CloudAMQP

Esta guia consolida el despliegue de todo MediBridge:

- Backend de microservicios Spring Boot en Render.
- API Gateway publico en Render.
- Microservicios internos como Private Services en Render.
- Bases PostgreSQL en Neon.
- RabbitMQ en CloudAMQP.
- MongoDB para `communication-service`.
- Front web clinico en Vercel.
- Front familiar Expo Web en Vercel.

## 1. Arquitectura final recomendada

```text
Usuarios web / app familiar web
        |
        v
Front clinico en Vercel        Front familiar web en Vercel
        |                              |
        +--------------+---------------+
                       |
                       v
          api-gateway publico en Render
                       |
                       v
       microservicios privados en Render
                       |
       +----------------+----------------+
       |                |                |
  Neon PostgreSQL   CloudAMQP      MongoDB Atlas
  servicios SQL     RabbitMQ       communication
```

Decision importante:

| Componente | Tipo en Render | Motivo |
|---|---|---|
| `api-gateway` | Web Service publico | Es la unica entrada publica al backend. |
| `iam-service` | Private Service | Solo debe recibir trafico del gateway y otros servicios. |
| `profiles-service` | Private Service | Solo gateway y servicios internos. |
| `payments-service` | Private Service | Solo gateway y servicios internos. |
| `appointments-service` | Private Service | Solo gateway y servicios internos. |
| `healthmonitoring-service` | Private Service | Solo gateway y servicios internos. |
| `medication-service` | Private Service | Solo gateway y servicios internos. |
| `reports-analytics-service` | Private Service | Solo gateway y servicios internos. |
| `communication-service` | Private Service | Solo gateway y servicios internos. |
| Web clinica Vite | Vercel Project publico | Frontend publico. |
| App familiar Expo Web | Vercel Project publico | Frontend publico web. |

Si Render no te deja usar Private Services por plan/cuenta, puedes desplegar temporalmente los microservicios como Web Services publicos, pero no es la arquitectura final. En ese caso el gateway funciona, pero cada microservicio tambien tendra URL publica directa.

## 2. Fuentes oficiales usadas

- Render Web Services: https://render.com/docs/web-services
- Render Private Services: https://render.com/docs/private-services
- Render Private Network: https://render.com/docs/private-network
- Render Monorepo Support: https://render.com/docs/monorepo-support
- Render Environment Variables: https://render.com/docs/configure-environment-variables
- Render Health Checks: https://render.com/docs/health-checks
- Vercel Vite deployments: https://vercel.com/docs/frameworks/frontend/vite
- Vercel build configuration: https://vercel.com/docs/builds/configure-a-build
- Vercel environment variables: https://vercel.com/docs/environment-variables
- Vercel project configuration: https://vercel.com/docs/project-configuration
- Neon PostgreSQL connections: https://neon.com/docs/connect/connect-from-any-app
- CloudAMQP RabbitMQ docs: https://www.cloudamqp.com/docs/rabbitmq-server.html
- Render + MongoDB Atlas: https://render.com/docs/connect-to-mongodb-atlas
- MongoDB connection strings: https://www.mongodb.com/docs/manual/reference/connection-string/
- Expo web export: https://docs.expo.dev/router/web/static-rendering/
- Vite production build: https://vite.dev/guide/build

## 3. Predeploy local obligatorio

Desde:

```powershell
cd C:\Users\Sebas\IdeaProjects\medibridge\medibridge.microservices
```

Ejecuta unit tests por microservicio:

```powershell
$services = @(
  'api-gateway',
  'iam-service',
  'profiles-service',
  'payments-service',
  'appointments-service',
  'healthmonitoring-service',
  'medication-service',
  'reports-analytics-service',
  'communication-service'
)

foreach ($service in $services) {
  .\mvnw.cmd -q -f "services/$service/pom.xml" test
  if ($LASTEXITCODE -ne 0) { throw "$service fallo" }
}
```

Valida que Karate compile:

```powershell
.\mvnw.cmd -q -f tests/api-tests/pom.xml test-compile
```

Karate completo requiere todos los servicios vivos:

```powershell
.\mvnw.cmd -f tests/api-tests/pom.xml test
```

Si un contenedor o servicio se queda en timeout, no lo tomes como visto bueno. Primero reinicia ese servicio y confirma `/actuator/health`.

## 4. Preparar Neon PostgreSQL

Crear estas bases PostgreSQL en Neon:

```text
medibridge_iam
medibridge_profiles
medibridge_payments
medibridge_appointments
medibridge_healthmonitoring
medibridge_medication
medibridge_reports
```

`api-gateway` no usa base de datos.

`communication-service` no usa PostgreSQL. Usa MongoDB, por eso debe ir en MongoDB Atlas o un MongoDB desplegado aparte. No lo puedes pasar a Neon sin refactor porque el codigo usa Spring Data MongoDB.

Formato JDBC correcto para Neon:

```text
jdbc:postgresql://<NEON_HOST>:5432/<DATABASE>?sslmode=require&channel_binding=require
```

Ejemplo:

```text
jdbc:postgresql://ep-demo.us-east-1.aws.neon.tech:5432/medibridge_iam?sslmode=require&channel_binding=require
```

No pongas `@` al inicio del host. Esto esta mal:

```text
jdbc:postgresql://@ep-demo.us-east-1.aws.neon.tech/medibridge_iam?sslmode=require&channel_binding=require
```

El `@` solo aparece en el connection string completo que Neon muestra cuando incluye usuario y password:

```text
postgresql://<NEON_USER>:<NEON_PASSWORD>@<NEON_HOST>/<DATABASE>?sslmode=require&channel_binding=require
```

Para Spring Boot en este proyecto no uses ese formato completo. Pon usuario y password en variables separadas:

```text
IAM_DB_USERNAME=<NEON_USER>
IAM_DB_PASSWORD=<NEON_PASSWORD>
```

Si copias desde Neon, separalo asi:

```text
Neon completo:
postgresql://neondb_owner:password@ep-demo.us-east-1.aws.neon.tech/medibridge_iam?sslmode=require&channel_binding=require

En Render:
IAM_DB_URL=jdbc:postgresql://ep-demo.us-east-1.aws.neon.tech:5432/medibridge_iam?sslmode=require&channel_binding=require
IAM_DB_USERNAME=neondb_owner
IAM_DB_PASSWORD=password
```

## 5. Preparar RabbitMQ en CloudAMQP

Crea o usa una instancia CloudAMQP y copia:

```text
RABBITMQ_HOST=<host>
RABBITMQ_PORT=5672
RABBITMQ_USER=<user>
RABBITMQ_PASSWORD=<password>
RABBITMQ_VHOST=<vhost>
```

El `RABBITMQ_VHOST` debe ser exacto. Si CloudAMQP muestra un vhost con slash o nombre raro, copialo igual.

## 6. Preparar MongoDB para Communication

Usa MongoDB Atlas para produccion.

Variable:

```text
COMMUNICATION_MONGODB_URI=mongodb+srv://<user>:<password>@<cluster>/<database>?retryWrites=true&w=majority
```

Recomendacion de database:

```text
medibridge_communication
```

Si Atlas bloquea conexion por IP, agrega los outbound IPs de Render o permite temporalmente acceso amplio mientras pruebas. Luego cierralo a lo necesario.

## 7. Preparar claves JWT de IAM

En produccion no dejes que IAM genere claves efimeras. Si reinicia con claves nuevas, los tokens anteriores dejan de validar en otros servicios.

Genera claves RSA:

```powershell
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out iam-private.pem
openssl rsa -pubout -in iam-private.pem -out iam-public.pem
```

En Render, pega el contenido completo en:

```text
IAM_JWT_PRIVATE_KEY=<contenido de iam-private.pem>
IAM_JWT_PUBLIC_KEY=<contenido de iam-public.pem>
IAM_JWT_ISSUER=medibridge-iam
IAM_JWT_KEY_ID=medibridge-iam-rsa-1
IAM_JWT_EXPIRATION_DAYS=7
```

La app acepta PEM con headers. No subas esos archivos al repo.

## 8. Orden correcto de despliegue

Orden recomendado:

```text
1. Neon PostgreSQL.
2. CloudAMQP.
3. MongoDB Atlas.
4. iam-service.
5. payments-service.
6. profiles-service.
7. appointments-service.
8. healthmonitoring-service.
9. medication-service.
10. reports-analytics-service.
11. communication-service.
12. api-gateway.
13. Front web clinico en Vercel.
14. Front familiar Expo Web en Vercel.
15. Karate contra Render.
```

`iam-service` va primero porque emite tokens y publica JWKS. Los demas necesitan `IAM_JWK_SET_URI`.

## 9. Configuracion comun para servicios Spring Boot

Para cada servicio backend en Render:

```text
Environment: Docker
Branch: main
Dockerfile Path: Dockerfile
Health Check Path: /actuator/health
```

Si el repo es monorepo, configura `Root Directory` con la carpeta del servicio.

No configures `PORT` manualmente. Render inyecta `PORT` y todos los servicios ya usan:

```yaml
server:
  port: ${PORT:${SERVER_PORT:xxxx}}
```

Variables comunes para todos los microservicios excepto `api-gateway`:

```text
JPA_DDL_AUTO=validate
RABBITMQ_HOST=<CLOUDAMQP_HOST>
RABBITMQ_PORT=5672
RABBITMQ_USER=<CLOUDAMQP_USER>
RABBITMQ_PASSWORD=<CLOUDAMQP_PASSWORD>
RABBITMQ_VHOST=<CLOUDAMQP_VHOST>
INTERNAL_SERVICE_TOKEN=<secret-largo>
```

Usa un secreto real:

```text
INTERNAL_SERVICE_TOKEN=mbg-prod-cambia-esto-por-un-token-largo
```

No uses:

```text
local-internal-token
```

## 10. Microservicios privados

Crear estos servicios como Private Services:

| Servicio | Root Directory | Puerto interno | DB |
|---|---|---:|---|
| `medibridge-iam-service` | `services/iam-service` | 8081 | `medibridge_iam` |
| `medibridge-payments-service` | `services/payments-service` | 8083 | `medibridge_payments` |
| `medibridge-profiles-service` | `services/profiles-service` | 8082 | `medibridge_profiles` |
| `medibridge-appointments-service` | `services/appointments-service` | 8084 | `medibridge_appointments` |
| `medibridge-healthmonitoring-service` | `services/healthmonitoring-service` | 8085 | `medibridge_healthmonitoring` |
| `medibridge-medication-service` | `services/medication-service` | 8086 | `medibridge_medication` |
| `medibridge-reports-analytics-service` | `services/reports-analytics-service` | 8087 | `medibridge_reports` |
| `medibridge-communication-service` | `services/communication-service` | 8088 | MongoDB Atlas |

En Render, el Internal Address se vera parecido a:

```text
http://medibridge-iam-service:8081
http://medibridge-profiles-service:8082
http://medibridge-payments-service:8083
http://medibridge-appointments-service:8084
http://medibridge-healthmonitoring-service:8085
http://medibridge-medication-service:8086
http://medibridge-reports-analytics-service:8087
http://medibridge-communication-service:8088
```

Usa esos valores en las variables internas. Nunca uses `localhost` entre servicios en Render.

## 11. Variables por microservicio

### 11.1 iam-service

Tipo:

```text
Private Service
Root Directory: services/iam-service
```

Variables:

```text
IAM_DB_URL=jdbc:postgresql://<NEON_HOST>:5432/medibridge_iam?sslmode=require
IAM_DB_USERNAME=<NEON_USER>
IAM_DB_PASSWORD=<NEON_PASSWORD>
JPA_DDL_AUTO=validate

RABBITMQ_HOST=<CLOUDAMQP_HOST>
RABBITMQ_PORT=5672
RABBITMQ_USER=<CLOUDAMQP_USER>
RABBITMQ_PASSWORD=<CLOUDAMQP_PASSWORD>
RABBITMQ_VHOST=<CLOUDAMQP_VHOST>

INTERNAL_SERVICE_TOKEN=<secret-compartido>

IAM_JWT_ISSUER=medibridge-iam
IAM_JWT_KEY_ID=medibridge-iam-rsa-1
IAM_JWT_EXPIRATION_DAYS=7
IAM_JWT_PRIVATE_KEY=<PEM_PRIVADO>
IAM_JWT_PUBLIC_KEY=<PEM_PUBLICO>
```

### 11.2 payments-service

Tipo:

```text
Private Service
Root Directory: services/payments-service
```

Variables:

```text
PAYMENTS_DB_URL=jdbc:postgresql://<NEON_HOST>:5432/medibridge_payments?sslmode=require
PAYMENTS_DB_USERNAME=<NEON_USER>
PAYMENTS_DB_PASSWORD=<NEON_PASSWORD>
JPA_DDL_AUTO=validate

RABBITMQ_HOST=<CLOUDAMQP_HOST>
RABBITMQ_PORT=5672
RABBITMQ_USER=<CLOUDAMQP_USER>
RABBITMQ_PASSWORD=<CLOUDAMQP_PASSWORD>
RABBITMQ_VHOST=<CLOUDAMQP_VHOST>

INTERNAL_SERVICE_TOKEN=<secret-compartido>
IAM_SERVICE_URL=http://medibridge-iam-service:8081
IAM_JWK_SET_URI=http://medibridge-iam-service:8081/api/v1/jwks/.well-known/jwks.json

FRONTEND_APP_URL=https://<front-familiar-o-web>.vercel.app
STRIPE_SECRET_KEY=<stripe-secret-key>
STRIPE_WEBHOOK_SECRET=<stripe-webhook-secret>
PAYMENTS_MOCK_ENABLED=false
```

Para pruebas internas puedes usar:

```text
PAYMENTS_MOCK_ENABLED=true
```

Para deploy final productivo, dejalo en `false`.

### 11.3 profiles-service

Tipo:

```text
Private Service
Root Directory: services/profiles-service
```

Variables:

```text
PROFILES_DB_URL=jdbc:postgresql://<NEON_HOST>:5432/medibridge_profiles?sslmode=require
PROFILES_DB_USERNAME=<NEON_USER>
PROFILES_DB_PASSWORD=<NEON_PASSWORD>
JPA_DDL_AUTO=validate

RABBITMQ_HOST=<CLOUDAMQP_HOST>
RABBITMQ_PORT=5672
RABBITMQ_USER=<CLOUDAMQP_USER>
RABBITMQ_PASSWORD=<CLOUDAMQP_PASSWORD>
RABBITMQ_VHOST=<CLOUDAMQP_VHOST>

INTERNAL_SERVICE_TOKEN=<secret-compartido>
IAM_SERVICE_URL=http://medibridge-iam-service:8081
PAYMENTS_SERVICE_URL=http://medibridge-payments-service:8083
IAM_JWK_SET_URI=http://medibridge-iam-service:8081/api/v1/jwks/.well-known/jwks.json
```

### 11.4 appointments-service

Tipo:

```text
Private Service
Root Directory: services/appointments-service
```

Variables:

```text
APPOINTMENTS_DB_URL=jdbc:postgresql://<NEON_HOST>:5432/medibridge_appointments?sslmode=require
APPOINTMENTS_DB_USERNAME=<NEON_USER>
APPOINTMENTS_DB_PASSWORD=<NEON_PASSWORD>
JPA_DDL_AUTO=validate

RABBITMQ_HOST=<CLOUDAMQP_HOST>
RABBITMQ_PORT=5672
RABBITMQ_USER=<CLOUDAMQP_USER>
RABBITMQ_PASSWORD=<CLOUDAMQP_PASSWORD>
RABBITMQ_VHOST=<CLOUDAMQP_VHOST>

INTERNAL_SERVICE_TOKEN=<secret-compartido>
IAM_SERVICE_URL=http://medibridge-iam-service:8081
PROFILES_SERVICE_URL=http://medibridge-profiles-service:8082
IAM_JWK_SET_URI=http://medibridge-iam-service:8081/api/v1/jwks/.well-known/jwks.json
```

### 11.5 healthmonitoring-service

Tipo:

```text
Private Service
Root Directory: services/healthmonitoring-service
```

Variables:

```text
HEALTHMONITORING_DB_URL=jdbc:postgresql://<NEON_HOST>:5432/medibridge_healthmonitoring?sslmode=require
HEALTHMONITORING_DB_USERNAME=<NEON_USER>
HEALTHMONITORING_DB_PASSWORD=<NEON_PASSWORD>
JPA_DDL_AUTO=validate

RABBITMQ_HOST=<CLOUDAMQP_HOST>
RABBITMQ_PORT=5672
RABBITMQ_USER=<CLOUDAMQP_USER>
RABBITMQ_PASSWORD=<CLOUDAMQP_PASSWORD>
RABBITMQ_VHOST=<CLOUDAMQP_VHOST>

INTERNAL_SERVICE_TOKEN=<secret-compartido>
IAM_SERVICE_URL=http://medibridge-iam-service:8081
PAYMENTS_SERVICE_URL=http://medibridge-payments-service:8083
PROFILES_SERVICE_URL=http://medibridge-profiles-service:8082
IAM_JWK_SET_URI=http://medibridge-iam-service:8081/api/v1/jwks/.well-known/jwks.json
```

### 11.6 medication-service

Tipo:

```text
Private Service
Root Directory: services/medication-service
```

Variables:

```text
MEDICATION_DB_URL=jdbc:postgresql://<NEON_HOST>:5432/medibridge_medication?sslmode=require
MEDICATION_DB_USERNAME=<NEON_USER>
MEDICATION_DB_PASSWORD=<NEON_PASSWORD>
JPA_DDL_AUTO=validate

RABBITMQ_HOST=<CLOUDAMQP_HOST>
RABBITMQ_PORT=5672
RABBITMQ_USER=<CLOUDAMQP_USER>
RABBITMQ_PASSWORD=<CLOUDAMQP_PASSWORD>
RABBITMQ_VHOST=<CLOUDAMQP_VHOST>

INTERNAL_SERVICE_TOKEN=<secret-compartido>
IAM_SERVICE_URL=http://medibridge-iam-service:8081
PROFILES_SERVICE_URL=http://medibridge-profiles-service:8082
IAM_JWK_SET_URI=http://medibridge-iam-service:8081/api/v1/jwks/.well-known/jwks.json
```

### 11.7 reports-analytics-service

Tipo:

```text
Private Service
Root Directory: services/reports-analytics-service
```

Variables:

```text
REPORTS_DB_URL=jdbc:postgresql://<NEON_HOST>:5432/medibridge_reports?sslmode=require
REPORTS_DB_USERNAME=<NEON_USER>
REPORTS_DB_PASSWORD=<NEON_PASSWORD>
JPA_DDL_AUTO=validate

RABBITMQ_HOST=<CLOUDAMQP_HOST>
RABBITMQ_PORT=5672
RABBITMQ_USER=<CLOUDAMQP_USER>
RABBITMQ_PASSWORD=<CLOUDAMQP_PASSWORD>
RABBITMQ_VHOST=<CLOUDAMQP_VHOST>

INTERNAL_SERVICE_TOKEN=<secret-compartido>
IAM_SERVICE_URL=http://medibridge-iam-service:8081
PAYMENTS_SERVICE_URL=http://medibridge-payments-service:8083
PROFILES_SERVICE_URL=http://medibridge-profiles-service:8082
APPOINTMENTS_SERVICE_URL=http://medibridge-appointments-service:8084
HEALTHMONITORING_SERVICE_URL=http://medibridge-healthmonitoring-service:8085
MEDICATION_SERVICE_URL=http://medibridge-medication-service:8086
IAM_JWK_SET_URI=http://medibridge-iam-service:8081/api/v1/jwks/.well-known/jwks.json

REPORTS_PDF_STORAGE_PATH=/tmp/reports
```

Nota: Render Private Services no deben depender de almacenamiento local para datos criticos. Los PDFs pueden regenerarse desde DB; si decides conservarlos, usa un disk o storage externo. Para la version actual, `/tmp/reports` evita fallos por permisos y el endpoint puede regenerar/servir segun la logica actual.

### 11.8 communication-service

Tipo:

```text
Private Service
Root Directory: services/communication-service
```

Variables:

```text
COMMUNICATION_MONGODB_URI=mongodb+srv://<MONGO_USER>:<MONGO_PASSWORD>@<MONGO_CLUSTER>/medibridge_communication?retryWrites=true&w=majority

RABBITMQ_HOST=<CLOUDAMQP_HOST>
RABBITMQ_PORT=5672
RABBITMQ_USER=<CLOUDAMQP_USER>
RABBITMQ_PASSWORD=<CLOUDAMQP_PASSWORD>
RABBITMQ_VHOST=<CLOUDAMQP_VHOST>

INTERNAL_SERVICE_TOKEN=<secret-compartido>
IAM_SERVICE_BASE_URL=http://medibridge-iam-service:8081
PROFILES_SERVICE_BASE_URL=http://medibridge-profiles-service:8082
IAM_JWK_SET_URI=http://medibridge-iam-service:8081/api/v1/jwks/.well-known/jwks.json
```

## 12. API Gateway publico

Crear como Web Service publico:

```text
Name: medibridge-api-gateway
Environment: Docker
Root Directory: services/api-gateway
Dockerfile Path: Dockerfile
Health Check Path: /actuator/health
```

Variables:

```text
IAM_SERVICE_URL=http://medibridge-iam-service:8081
PROFILES_SERVICE_URL=http://medibridge-profiles-service:8082
PAYMENTS_SERVICE_URL=http://medibridge-payments-service:8083
APPOINTMENTS_SERVICE_URL=http://medibridge-appointments-service:8084
HEALTHMONITORING_SERVICE_URL=http://medibridge-healthmonitoring-service:8085
MEDICATION_SERVICE_URL=http://medibridge-medication-service:8086
REPORTS_ANALYTICS_SERVICE_URL=http://medibridge-reports-analytics-service:8087
COMMUNICATION_SERVICE_URL=http://medibridge-communication-service:8088

INTERNAL_SERVICE_TOKEN=<secret-compartido>
GATEWAY_CORS_ALLOWED_ORIGINS=https://<front-web>.vercel.app,https://<front-family>.vercel.app
```

Si estas desplegando los frontends despues y aun no sabes sus URLs, usa temporalmente:

```text
GATEWAY_CORS_ALLOWED_ORIGINS=*
```

Cuando los frontends esten vivos, cambia `*` por sus dominios reales y redeploy del gateway.

No dejes variables vacias. Esto esta mal:

```text
REPORTS_ANALYTICS_SERVICE_URL=
```

Si existe una variable vacia, el gateway puede fallar al arrancar.

## 13. Front web clinico en Vercel

Repo local:

```text
C:\Users\Sebas\IdeaProjects\MEDIBRIDGE-APP-WEB
```

Crear en Vercel:

```text
New Project
Framework Preset: Vite
Root Directory: vacio si el repo es solo este frontend
Build Command: npm ci && npm run build
Output Directory: dist
```

Variables:

```text
VITE_API_BASE_URL=https://medibridge-api-gateway.onrender.com
VITE_ENABLE_PAYMENT_MOCKS=false
```

Importante: este frontend ya agrega `/api/v1/...` en el cliente HTTP, por eso `VITE_API_BASE_URL` debe ser solo la URL base del gateway, sin `/api/v1`.

Prueba local antes:

```powershell
cd C:\Users\Sebas\IdeaProjects\MEDIBRIDGE-APP-WEB
npm ci
npm run build
```

Si al abrir una ruta interna directo, por ejemplo `/dashboard`, Vercel devuelve 404, agrega `vercel.json` en la raiz del frontend web:

```json
{
  "rewrites": [
    { "source": "/(.*)", "destination": "/index.html" }
  ]
}
```

Cuando Vercel genere la URL final, vuelve al gateway en Render y actualiza:

```text
GATEWAY_CORS_ALLOWED_ORIGINS=https://<front-web>.vercel.app,https://<front-family>.vercel.app
```

## 14. Front familiar Expo Web en Vercel

Repo local:

```text
C:\Users\Sebas\IdeaProjects\medibridge-mobile\medibridge-family-app
```

Crear en Vercel:

```text
New Project
Framework Preset: Other
Root Directory: medibridge-family-app si el repo raiz es medibridge-mobile
Build Command: npm ci && npx expo export --platform web
Output Directory: dist
```

Variables:

```text
EXPO_PUBLIC_API_BASE_URL=https://medibridge-api-gateway.onrender.com/api/v1
```

Importante: este frontend espera la base con `/api/v1`, porque sus endpoints se construyen desde esa base.

Prueba local antes:

```powershell
cd C:\Users\Sebas\IdeaProjects\medibridge-mobile\medibridge-family-app
npm ci
npm run check
npx expo export --platform web
```

Para app nativa Android/iOS, Vercel tampoco genera APK/IPA. Vercel sirve la version web de Expo. Para binarios moviles usa Expo EAS Build.

Si recargar una ruta interna de Expo Web devuelve 404, agrega `vercel.json` en la raiz de `medibridge-family-app`:

```json
{
  "rewrites": [
    { "source": "/(.*)", "destination": "/index.html" }
  ]
}
```

## 15. Verificacion backend en Render

Cuando el gateway este Live:

```powershell
$GatewayUrl = "https://medibridge-api-gateway.onrender.com"
Invoke-RestMethod "$GatewayUrl/actuator/health"
```

Debe devolver:

```json
{"status":"UP"}
```

Probar OpenAPI por gateway:

```powershell
Invoke-RestMethod "$GatewayUrl/docs/iam/v3/api-docs" | Select-Object openapi
Invoke-RestMethod "$GatewayUrl/docs/profiles/v3/api-docs" | Select-Object openapi
Invoke-RestMethod "$GatewayUrl/docs/payments/v3/api-docs" | Select-Object openapi
Invoke-RestMethod "$GatewayUrl/docs/appointments/v3/api-docs" | Select-Object openapi
Invoke-RestMethod "$GatewayUrl/docs/healthmonitoring/v3/api-docs" | Select-Object openapi
Invoke-RestMethod "$GatewayUrl/docs/medication/v3/api-docs" | Select-Object openapi
Invoke-RestMethod "$GatewayUrl/docs/reports-analytics/v3/api-docs" | Select-Object openapi
Invoke-RestMethod "$GatewayUrl/docs/communication/v3/api-docs" | Select-Object openapi
```

Probar auth:

```powershell
$Username = "render-" + [guid]::NewGuid().ToString()
$Password = "Test123456!"

$SignUpBody = @{
  username = $Username
  password = $Password
  roles = @("ROLE_USER")
} | ConvertTo-Json

$CreatedUser = Invoke-RestMethod `
  -Method Post `
  -Uri "$GatewayUrl/api/v1/authentication/sign-up" `
  -ContentType "application/json" `
  -Body $SignUpBody

$SignInBody = @{
  username = $Username
  password = $Password
} | ConvertTo-Json

$Auth = Invoke-RestMethod `
  -Method Post `
  -Uri "$GatewayUrl/api/v1/authentication/sign-in" `
  -ContentType "application/json" `
  -Body $SignInBody

$Token = $Auth.token

Invoke-RestMethod `
  -Method Get `
  -Uri "$GatewayUrl/api/v1/users/$($CreatedUser.id)" `
  -Headers @{ Authorization = "Bearer $Token" }
```

Probar Profiles:

```powershell
$PatientBody = @{ fullName = "Paciente Render Deploy" } | ConvertTo-Json

$Patient = Invoke-RestMethod `
  -Method Post `
  -Uri "$GatewayUrl/api/v1/profiles/patients" `
  -ContentType "application/json" `
  -Headers @{ Authorization = "Bearer $Token" } `
  -Body $PatientBody

$Patient
```

## 16. Karate contra Render

Con todo desplegado:

```powershell
cd C:\Users\Sebas\IdeaProjects\medibridge\medibridge.microservices

.\mvnw.cmd -f tests/api-tests/pom.xml test `
  "-Diam.baseUrl=$GatewayUrl/api/v1" `
  "-Dprofiles.baseUrl=$GatewayUrl/api/v1" `
  "-Dpayments.baseUrl=$GatewayUrl/api/v1" `
  "-Dappointments.baseUrl=$GatewayUrl/api/v1" `
  "-Dhealthmonitoring.baseUrl=$GatewayUrl/api/v1" `
  "-Dmedication.baseUrl=$GatewayUrl/api/v1" `
  "-Dreports.baseUrl=$GatewayUrl/api/v1" `
  "-Dcommunication.baseUrl=$GatewayUrl/api/v1" `
  "-Dgateway.url=$GatewayUrl" `
  "-Dgateway.baseUrl=$GatewayUrl/api/v1"
```

Si los microservicios son privados, Karate debe correr contra el gateway, no contra URLs internas.

## 17. Checklist de visto bueno para deploy

No des visto bueno hasta marcar todo:

```text
[ ] Unit tests de los 9 servicios pasan.
[ ] api-tests hace test-compile.
[ ] Neon tiene todas las DB SQL creadas.
[ ] MongoDB Atlas esta listo para communication-service.
[ ] CloudAMQP esta listo.
[ ] IAM tiene claves RSA persistentes.
[ ] Todos los microservicios privados estan Live.
[ ] Cada microservicio responde /actuator/health en el health check de Render.
[ ] api-gateway esta Live y responde /actuator/health.
[ ] /docs/<service>/v3/api-docs responde para los 8 servicios por gateway.
[ ] Sign-up y sign-in funcionan por gateway.
[ ] Endpoint protegido funciona con Bearer token.
[ ] Front web clinico compila y carga en Vercel.
[ ] Front familiar Expo Web compila/exporta y carga en Vercel.
[ ] CORS del gateway contiene las dos URLs reales de Vercel.
[ ] PAYMENTS_MOCK_ENABLED=false en produccion.
[ ] Stripe esta configurado si se usaran pagos reales.
[ ] INTERNAL_SERVICE_TOKEN es el mismo en gateway y microservicios.
[ ] Ninguna variable *_SERVICE_URL esta vacia.
[ ] Ningun frontend usa X-Internal-Token.
```

Si todo eso esta verde, hay visto bueno para deploy.

## 18. Errores comunes

### Usar localhost entre servicios

En Render, `localhost` significa el mismo contenedor. Para comunicacion backend usa Internal Addresses:

```text
http://medibridge-iam-service:8081
```

No:

```text
http://localhost:8081
```

### JWT invalido despues de reiniciar IAM

Causa:

```text
IAM_JWT_PRIVATE_KEY/IAM_JWT_PUBLIC_KEY no estaban configuradas y IAM genero claves efimeras.
```

Solucion:

```text
Configura claves RSA persistentes y redeploy de IAM.
```

### Gateway arranca mal por URL vacia

Causa:

```text
REPORTS_ANALYTICS_SERVICE_URL=
```

Solucion:

```text
Eliminar la variable o poner un valor real.
```

### Profiles no encuentra usuario autenticado

Revisar:

```text
IAM_SERVICE_URL=http://medibridge-iam-service:8081
IAM_JWK_SET_URI=http://medibridge-iam-service:8081/api/v1/jwks/.well-known/jwks.json
```

Tambien revisa que IAM este Live y que `/api/v1/internal/users/by-username/{username}` responda desde la red interna.

### Communication no conecta a MongoDB

Revisar:

```text
COMMUNICATION_MONGODB_URI
Network Access en MongoDB Atlas
Usuario/password de Atlas
```

### Front web pega a URL incorrecta

Web clinica:

```text
VITE_API_BASE_URL=https://medibridge-api-gateway.onrender.com
```

Family Expo Web:

```text
EXPO_PUBLIC_API_BASE_URL=https://medibridge-api-gateway.onrender.com/api/v1
```

No son iguales porque los clientes construyen rutas de forma distinta.

### La app React Native no genera APK en Vercel

Correcto. Vercel solo despliega la salida web de Expo:

```text
npx expo export --platform web
```

Para Android/iOS instalable:

```text
Usa Expo EAS Build.
```

### Payments mock activo en produccion

Para produccion:

```text
PAYMENTS_MOCK_ENABLED=false
```

Solo usar `true` para demo interna sin Stripe.
