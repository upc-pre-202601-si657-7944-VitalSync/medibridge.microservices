# Guia de despliegue: Health Monitoring + Payments en Render, Neon y CloudAMQP

Esta guia es solo para desplegar:

- `healthmonitoring-service`
- `payments-service`

Repo real:

```text
C:\Users\Sebas\IdeaProjects\medibridge\medibridge.microservices
```

Los servicios base ya existen en Render. No los vuelvas a desplegar con esta guia.

## 1. Que hacer con los deploys antiguos

Si los deploys antiguos de Health Monitoring y Payments apuntan al repo de prueba, lo mas limpio es:

```text
1. Crear dos Web Services nuevos en Render apuntando al repo real.
2. Copiar las mismas variables de entorno.
3. Probar que los nuevos servicios respondan bien.
4. Recien ahi borrar los Web Services antiguos.
```

No borres los antiguos antes de probar los nuevos.

Si el servicio antiguo esta en el mismo repo real y solo cambio la carpeta, puedes editar:

```text
Render Dashboard
  -> Web Service
  -> Settings
  -> Build & Deploy
  -> Root Directory
```

Pero si el repo Git es otro, crea un Web Service nuevo. Render documenta la seleccion del repo durante la creacion del servicio y la edicion de `Root Directory` para monorepos; no trates de arreglar un deploy viejo apuntando al repo equivocado si puedes crear uno limpio.

## 2. Requisitos

Necesitas:

- El repo real subido a GitHub.
- Las databases en Neon:

```text
medibridge_healthmonitoring
medibridge_payments
```

- La instancia CloudAMQP ya usada por MediBridge.
- La URL publica del servicio de autenticacion ya desplegado.
- La URL JWK ya desplegada, con este formato:

```text
https://<auth-service>.onrender.com/api/v1/jwks/.well-known/jwks.json
```

## 3. Variables comunes

CloudAMQP:

```text
RABBITMQ_HOST=<CLOUDAMQP_HOST>
RABBITMQ_PORT=5672
RABBITMQ_USER=<CLOUDAMQP_USER>
RABBITMQ_PASSWORD=<CLOUDAMQP_PASSWORD>
RABBITMQ_VHOST=<CLOUDAMQP_VHOST>
```

JWK:

```text
IAM_JWK_SET_URI=https://<auth-service>.onrender.com/api/v1/jwks/.well-known/jwks.json
```

En Render, cada variable se agrega como fila:

```text
Key:   RABBITMQ_HOST
Value: <CLOUDAMQP_HOST>
```

No pegues `KEY=value` completo dentro del campo `Value`.

## 4. URLs JDBC de Neon

Neon suele darte algo asi:

```text
postgresql://neondb_owner:PASSWORD@ep-example.us-east-1.aws.neon.tech/medibridge_payments?sslmode=require&channel_binding=require
```

Separalo asi:

```text
Usuario:  neondb_owner
Password: PASSWORD
Host:     ep-example.us-east-1.aws.neon.tech
Puerto:   5432
Database: medibridge_payments
```

El puerto `5432` va despues del host:

```text
HOST:5432/DATABASE
```

Correcto:

```text
jdbc:postgresql://ep-example.us-east-1.aws.neon.tech:5432/medibridge_payments?sslmode=require
```

Tambien es valido dejar `channel_binding` en la misma linea:

```text
jdbc:postgresql://ep-example.us-east-1.aws.neon.tech:5432/medibridge_payments?sslmode=require&channel_binding=require
```

Incorrecto:

```text
jdbc:postgresql://neondb_owner:PASSWORD@ep-example.us-east-1.aws.neon.tech/medibridge_payments
jdbc:postgresql://ep-example.us-east-1.aws.neon.tech:5432/medibridge_payments
sslmode=require&channel_binding=require
```

## 5. Desplegar healthmonitoring-service

En Render:

```text
New
  -> Web Service
  -> Git Provider
  -> selecciona el repo real
```

Configura:

```text
Name: medibridge-healthmonitoring-service
Environment: Docker
Branch: main
Root Directory: services/healthmonitoring-service
Dockerfile Path: Dockerfile
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

IAM_JWK_SET_URI=https://<auth-service>.onrender.com/api/v1/jwks/.well-known/jwks.json
```

No configures `PORT`. Render lo inyecta automaticamente.

Health check:

```text
https://medibridge-healthmonitoring-service.onrender.com/actuator/health
```

Swagger:

```text
https://medibridge-healthmonitoring-service.onrender.com/swagger-ui.html
```

## 6. Desplegar payments-service

En Render:

```text
New
  -> Web Service
  -> Git Provider
  -> selecciona el repo real
```

Configura:

```text
Name: medibridge-payments-service
Environment: Docker
Branch: main
Root Directory: services/payments-service
Dockerfile Path: Dockerfile
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

IAM_SERVICE_URL=https://<auth-service>.onrender.com
IAM_JWK_SET_URI=https://<auth-service>.onrender.com/api/v1/jwks/.well-known/jwks.json

STRIPE_SECRET_KEY=
STRIPE_WEBHOOK_SECRET=
```

Para probar el plan gratuito, Stripe puede quedar vacio.

No uses planes pagos si `STRIPE_SECRET_KEY` esta vacio.

Health check:

```text
https://medibridge-payments-service.onrender.com/actuator/health
```

Swagger:

```text
https://medibridge-payments-service.onrender.com/swagger-ui.html
```

## 7. Orden recomendado

```text
1. Confirmar databases en Neon.
2. Confirmar CloudAMQP.
3. Crear Web Service nuevo para healthmonitoring-service desde el repo real.
4. Crear Web Service nuevo para payments-service desde el repo real.
5. Probar health checks.
6. Probar Swagger.
7. Cuando todo funcione, borrar los Web Services antiguos del repo de prueba.
```

## 8. Prueba rapida de Payments

Con un token valido:

```text
POST /api/v1/subscriptions
```

Body:

```json
{
  "userId": 1,
  "commercialLine": "FAMILY",
  "planType": "FREE",
  "billingCycle": "MONTHLY"
}
```

Reemplaza `1` por un `userId` real.

Este flujo:

- Valida el usuario.
- Crea una suscripcion gratuita.
- Crea una factura.
- Publica `subscription.activated`.

## 9. Prueba rapida de Health Monitoring

Con un token valido y datos base ya creados:

```text
POST /api/v1/health-monitoring/patients/{patientId}/observations
```

Body:

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

Reemplaza:

```text
patientId
recordedByDoctorProfileId
```

por IDs reales.

## 10. Problemas comunes

### Render apunta al repo viejo

Crea un Web Service nuevo desde el repo real. Despues de probar, borra el viejo.

### Error de PostgreSQL

Verifica que el valor sea:

```text
jdbc:postgresql://HOST:5432/DATABASE?sslmode=require
```

No incluyas:

```text
USER:PASSWORD@
```

El usuario y password van separados:

```text
SERVICE_DB_USERNAME
SERVICE_DB_PASSWORD
```

### Error de RabbitMQ

Verifica:

```text
RABBITMQ_HOST
RABBITMQ_PORT
RABBITMQ_USER
RABBITMQ_PASSWORD
RABBITMQ_VHOST
```

El `RABBITMQ_VHOST` debe copiarse exactamente desde CloudAMQP.

### Payments falla con planes pagos

Usa plan gratuito para validar despliegue:

```text
commercialLine=FAMILY
planType=FREE
billingCycle=MONTHLY
```

Los planes pagos requieren Stripe configurado.

### Health Monitoring no encuentra referencias

Usa IDs reales ya existentes y confirma que el servicio estaba desplegado antes de crear los datos que necesita consumir por eventos.

## 11. Fuentes oficiales

- Render Web Services: https://render.com/docs/web-services
- Render GitHub: https://render.com/docs/github
- Render Monorepo Support: https://render.com/docs/monorepo-support
- Neon conexion general: https://neon.com/docs/connect/connect-from-any-app
- CloudAMQP Docs: https://www.cloudamqp.com/docs/
