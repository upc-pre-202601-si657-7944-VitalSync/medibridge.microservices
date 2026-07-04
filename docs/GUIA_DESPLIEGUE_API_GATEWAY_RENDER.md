# Guia de despliegue del API Gateway en Render

Esta guia explica, paso a paso, como desplegar `api-gateway` en Render y como probar que realmente esta conectando con los microservicios desplegados.

## Objetivo

La arquitectura final esperada es:

```text
Cliente / Frontend / Swagger
        |
        v
api-gateway publico en Render
        |
        v
microservicios internos en Render
```

El unico punto publico para endpoints de negocio debe ser:

```text
api-gateway
```

Los microservicios que debe enrutar el gateway son:

```text
iam-service
profiles-service
payments-service
appointments-service
healthmonitoring-service
medication-service
reports-analytics-service
```

Servicios presentes en este repo:

```text
services/iam-service
services/profiles-service
services/payments-service
services/appointments-service
services/healthmonitoring-service
services/medication-service
services/reports-analytics-service
services/api-gateway
```

Nota importante para tu caso actual:

```text
Los servicios que ya estan desplegados en Render desde otro repo se pueden usar para probar el gateway.
No necesitas redeplegarlos solo por estar en otro repo si tienen los mismos endpoints.
reports-analytics-service ya existe en este repo, pero si aun no esta desplegado en Render, sus pruebas fallaran hasta que lo despliegues.
```

## Paso 1. Confirmar que el gateway tiene rutas para todos los servicios

Archivo:

```text
services/api-gateway/src/main/resources/application.yml
```

Rutas principales:

```text
/api/v1/authentication/**        -> iam-service
/api/v1/users/**                 -> iam-service
/api/v1/roles/**                 -> iam-service
/api/v1/profiles/**              -> profiles-service
/api/v1/subscriptions/**         -> payments-service
/api/v1/invoices/**              -> payments-service
/api/v1/stripe-webhooks/**       -> payments-service
/api/v1/appointments/**          -> appointments-service
/api/v1/health-monitoring/**     -> healthmonitoring-service
/api/v1/medications/**           -> medication-service
/api/v1/medication-schedules/**  -> medication-service
/api/v1/dose-administrations/**  -> medication-service
/api/v1/clinical-reports/**      -> reports-analytics-service
/api/v1/analytics-dashboards/**  -> reports-analytics-service
```

Swagger/OpenAPI consolidado:

```text
/docs/iam/v3/api-docs
/docs/profiles/v3/api-docs
/docs/payments/v3/api-docs
/docs/appointments/v3/api-docs
/docs/healthmonitoring/v3/api-docs
/docs/medication/v3/api-docs
/docs/reports-analytics/v3/api-docs
```

## Paso 2. Decidir como vas a probar en Render

Tienes dos formas validas.

Opcion A: probar con servicios ya desplegados como Web Services publicos.

```text
Esto sirve para validar que el gateway conecta con los servicios reales ya desplegados.
Mientras sigan como Web Services publicos, tambien se podran consumir directo por sus URLs onrender.com.
Eso no rompe la prueba del gateway, pero no es el aislamiento final.
```

Opcion B: arquitectura final con Private Services.

```text
Cada microservicio queda como Private Service.
El api-gateway queda como Web Service publico.
El cliente solo puede entrar por el gateway.
```

Recomendacion practica:

```text
Primero prueba el gateway con los servicios que ya tienes vivos en Render.
Cuando todo enrute bien, migras los microservicios a Private Service.
```

## Paso 3. Preparar URLs de los microservicios

Si usaras servicios ya desplegados como Web Services publicos, copia sus URLs publicas:

```text
https://<iam-service-actual>.onrender.com
https://<profiles-service-actual>.onrender.com
https://<payments-service-actual>.onrender.com
https://<appointments-service-actual>.onrender.com
https://<healthmonitoring-service-actual>.onrender.com
https://<medication-service-actual>.onrender.com
```

Si `reports-analytics-service` todavia no esta desplegado, no inventes URL para ese servicio. Dejalo pendiente y no ejecutes sus pruebas hasta desplegarlo.

Si usaras Private Services, copia el Internal Address que Render muestra para cada microservicio. Debe verse parecido a:

```text
http://medibridge-iam-service:8081
http://medibridge-profiles-service:8082
http://medibridge-payments-service:8083
http://medibridge-appointments-service:8084
http://medibridge-healthmonitoring-service:8085
http://medibridge-medication-service:8086
http://medibridge-reports-analytics-service:8087
```

Regla importante:

```text
En Render nunca uses localhost para apuntar a otro microservicio.
localhost dentro del gateway significa "el contenedor del gateway", no IAM ni Profiles.
```

## Paso 4. Configurar token interno

Define un secreto largo. Ejemplo:

```text
mbg-prod-REEMPLAZA-ESTO-POR-UN-SECRETO-LARGO
```

Variable:

```text
INTERNAL_SERVICE_TOKEN=<secret-compartido>
```

Debe existir en:

```text
api-gateway
iam-service
profiles-service
payments-service
appointments-service
healthmonitoring-service
medication-service
reports-analytics-service
```

Si un servicio desplegado desde otro repo todavia no valida `INTERNAL_SERVICE_TOKEN`, no pasa nada para la prueba: el gateway enviara el header y el servicio lo ignorara.

Si el servicio si valida el token interno, el valor debe ser exactamente igual en gateway y microservicio.

No uses en produccion:

```text
local-internal-token
```

## Paso 5. Crear el Web Service del API Gateway

En Render:

```text
New
  -> Web Service
  -> selecciona el repo
```

Configura:

```text
Name: medibridge-api-gateway
Environment: Docker
Branch: main
Root Directory: services/api-gateway
Dockerfile Path: Dockerfile
```

No configures `PORT` manualmente. Render lo inyecta automaticamente y el gateway ya usa:

```yaml
server:
  port: ${PORT:${SERVER_PORT:8080}}
```

## Paso 6. Variables de entorno del API Gateway

En Render, entra a:

```text
medibridge-api-gateway
  -> Environment
```

Regla obligatoria:

```text
No dejes ninguna variable *_SERVICE_URL creada y vacia.
Si un microservicio aun no esta desplegado, elimina esa variable del gateway por ahora o pon una URL real temporal.
Una variable vacia rompe el arranque con un error tipo RouteProperties.getUri() is null.
```

Agrega estas variables si vas a probar con los servicios ya desplegados como Web Services publicos:

```text
IAM_SERVICE_URL=https://<iam-service-actual>.onrender.com
PROFILES_SERVICE_URL=https://<profiles-service-actual>.onrender.com
PAYMENTS_SERVICE_URL=https://<payments-service-actual>.onrender.com
APPOINTMENTS_SERVICE_URL=https://<appointments-service-actual>.onrender.com
HEALTHMONITORING_SERVICE_URL=https://<healthmonitoring-service-actual>.onrender.com
MEDICATION_SERVICE_URL=https://<medication-service-actual>.onrender.com

INTERNAL_SERVICE_TOKEN=<secret-compartido>
GATEWAY_CORS_ALLOWED_ORIGINS=*
```

Cuando `reports-analytics-service` ya este desplegado, agrega tambien:

```text
REPORTS_ANALYTICS_SERVICE_URL=https://<reports-analytics-service>.onrender.com
```

Si usas Private Services, las variables deben apuntar a Internal Addresses:

```text
IAM_SERVICE_URL=http://medibridge-iam-service:8081
PROFILES_SERVICE_URL=http://medibridge-profiles-service:8082
PAYMENTS_SERVICE_URL=http://medibridge-payments-service:8083
APPOINTMENTS_SERVICE_URL=http://medibridge-appointments-service:8084
HEALTHMONITORING_SERVICE_URL=http://medibridge-healthmonitoring-service:8085
MEDICATION_SERVICE_URL=http://medibridge-medication-service:8086
REPORTS_ANALYTICS_SERVICE_URL=http://medibridge-reports-analytics-service:8087

INTERNAL_SERVICE_TOKEN=<secret-compartido>
GATEWAY_CORS_ALLOWED_ORIGINS=*
```

Si tienes frontend desplegado, reemplaza el `*`:

```text
GATEWAY_CORS_ALLOWED_ORIGINS=https://tu-frontend.onrender.com
```

## Paso 7. Health check del gateway

En Render configura:

```text
Health Check Path: /actuator/health
```

Despliega:

```text
Manual Deploy
  -> Deploy latest commit
```

Espera a que Render muestre:

```text
Live
```

## Paso 8. Probar que el gateway esta vivo

En PowerShell:

```powershell
$GatewayUrl = "https://medibridge-api-gateway.onrender.com"
Invoke-RestMethod "$GatewayUrl/actuator/health"
```

Respuesta esperada:

```json
{
  "status": "UP"
}
```

Importante:

```text
Esto solo confirma que el gateway esta vivo.
No confirma que llegue a los microservicios.
```

## Paso 9. Probar conexion real con microservicios por OpenAPI

Ejecuta uno por uno:

```powershell
$GatewayUrl = "https://medibridge-api-gateway.onrender.com"

Invoke-RestMethod "$GatewayUrl/docs/iam/v3/api-docs" | Select-Object openapi
Invoke-RestMethod "$GatewayUrl/docs/profiles/v3/api-docs" | Select-Object openapi
Invoke-RestMethod "$GatewayUrl/docs/payments/v3/api-docs" | Select-Object openapi
Invoke-RestMethod "$GatewayUrl/docs/appointments/v3/api-docs" | Select-Object openapi
Invoke-RestMethod "$GatewayUrl/docs/healthmonitoring/v3/api-docs" | Select-Object openapi
Invoke-RestMethod "$GatewayUrl/docs/medication/v3/api-docs" | Select-Object openapi
```

Resultado esperado:

```text
Cada comando debe devolver una version OpenAPI, por ejemplo 3.0.1 o 3.1.0.
```

Interpretacion:

```text
Si /docs/iam/v3/api-docs responde, el gateway esta llegando a IAM.
Si /docs/profiles/v3/api-docs responde, el gateway esta llegando a Profiles.
Si /docs/payments/v3/api-docs responde, el gateway esta llegando a Payments.
Si /docs/appointments/v3/api-docs responde, el gateway esta llegando a Appointments.
Si /docs/healthmonitoring/v3/api-docs responde, el gateway esta llegando a Health Monitoring.
Si /docs/medication/v3/api-docs responde, el gateway esta llegando a Medication.
```

Para Reports/Analytics ejecuta esto solo cuando ya este desplegado y hayas configurado `REPORTS_ANALYTICS_SERVICE_URL`:

```powershell
Invoke-RestMethod "$GatewayUrl/docs/reports-analytics/v3/api-docs" | Select-Object openapi
```

Si Reports/Analytics aun no esta desplegado, esta prueba puede fallar. Eso es esperado y no significa que IAM, Profiles, Payments, Appointments, Health Monitoring o Medication esten mal.

## Paso 10. Abrir Swagger consolidado

Abre:

```text
https://medibridge-api-gateway.onrender.com/swagger-ui.html
```

Debe aparecer un selector con:

```text
IAM Service
Profiles Service
Payments Service
Appointments Service
Health Monitoring Service
Medication Service
Reports Analytics Service
```

Si `Reports Analytics Service` aparece pero aun no lo desplegaste, al seleccionarlo puede fallar. Es normal hasta que exista su URL en Render.

## Paso 11. Confirmar conexion usando logs de Render

Esta es la forma mas clara de comprobar que el gateway esta hablando con un microservicio desplegado.

Abre logs de `iam-service` en Render.

Luego ejecuta:

```powershell
$GatewayUrl = "https://medibridge-api-gateway.onrender.com"
Invoke-RestMethod "$GatewayUrl/docs/iam/v3/api-docs"
```

En los logs de IAM debe aparecer actividad.

Repite con Profiles:

```powershell
Invoke-RestMethod "$GatewayUrl/docs/profiles/v3/api-docs"
```

Si ves actividad en el microservicio mientras llamas la URL del gateway, confirmaste el camino real:

```text
PowerShell -> api-gateway -> microservicio desplegado en Render
```

## Paso 12. Probar autenticacion por gateway

Crear usuario:

```powershell
$GatewayUrl = "https://medibridge-api-gateway.onrender.com"
$Username = "gateway-render-" + [guid]::NewGuid().ToString()
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

$CreatedUser
```

Resultado esperado:

```text
Debe devolver el usuario creado.
```

Iniciar sesion:

```powershell
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
$Auth
```

Resultado esperado:

```text
Debe devolver un JWT.
```

Consultar usuario autenticado:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "$GatewayUrl/api/v1/users/$($CreatedUser.id)" `
  -Headers @{ Authorization = "Bearer $Token" }
```

Resultado esperado:

```text
Debe devolver el usuario creado.
```

## Paso 13. Probar Profiles por gateway

Crear paciente:

```powershell
$PatientBody = @{
  fullName = "Paciente Render Gateway"
} | ConvertTo-Json

$Patient = Invoke-RestMethod `
  -Method Post `
  -Uri "$GatewayUrl/api/v1/profiles/patients" `
  -ContentType "application/json" `
  -Headers @{ Authorization = "Bearer $Token" } `
  -Body $PatientBody

$Patient
```

Consultar paciente:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "$GatewayUrl/api/v1/profiles/patients/$($Patient.id)" `
  -Headers @{ Authorization = "Bearer $Token" }
```

Resultado esperado:

```text
Debe devolver el paciente creado.
```

## Paso 14. Probar Reports/Analytics cuando este desplegado

Este paso requiere que ya exista `reports-analytics-service` en Render y que el gateway tenga:

```text
REPORTS_ANALYTICS_SERVICE_URL=<url-real-de-reports-analytics>
```

Primero prueba OpenAPI:

```powershell
Invoke-RestMethod "$GatewayUrl/docs/reports-analytics/v3/api-docs" | Select-Object openapi
```

Luego prueba un endpoint de lectura:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "$GatewayUrl/api/v1/analytics-dashboards/patients/$($Patient.id)" `
  -Headers @{ Authorization = "Bearer $Token" }
```

Si no hay datos clinicos suficientes, el endpoint puede responder vacio, 404 o una validacion de dominio segun la logica del servicio. Lo importante para conectividad es que no sea:

```text
502
Connection refused
Host not found
I/O error
```

## Paso 15. Probar que el gateway no expone endpoints internos

Ejecuta:

```powershell
Invoke-WebRequest -UseBasicParsing "$GatewayUrl/api/v1/internal/users/1/exists"
```

Resultado esperado:

```text
403 Forbidden
```

Tambien:

```powershell
Invoke-WebRequest -UseBasicParsing "$GatewayUrl/api/v1/internal/health-monitoring/patients/1/summary"
Invoke-WebRequest -UseBasicParsing "$GatewayUrl/api/v1/internal/appointments/patients/1/summary"
Invoke-WebRequest -UseBasicParsing "$GatewayUrl/api/v1/internal/medications/patients/1/summary"
```

Resultado esperado:

```text
403 Forbidden
```

Eso es correcto. El gateway no debe exponer `/api/v1/internal/**`.

## Paso 16. Probar aislamiento final

Si los microservicios estan como Private Services:

```text
No deben tener URL publica onrender.com.
Solo el gateway debe tener URL publica.
```

En Render verifica:

```text
api-gateway                 Web Service
iam-service                 Private Service
profiles-service            Private Service
payments-service            Private Service
appointments-service        Private Service
healthmonitoring-service    Private Service
medication-service          Private Service
reports-analytics-service   Private Service
```

Si un microservicio sigue como Web Service publico, todavia podras entrar directo por su URL. En ese caso el gateway funciona, pero el aislamiento final aun no esta completo.

## Paso 17. Ejecutar Karate contra Render

Con el gateway desplegado:

```powershell
.\mvnw.cmd -f tests/api-tests/pom.xml `
  "-Dtest=GatewayRunner" `
  "-Dgateway.url=https://medibridge-api-gateway.onrender.com" `
  "-Dgateway.baseUrl=https://medibridge-api-gateway.onrender.com/api/v1" `
  test
```

Resultado esperado:

```text
scenarios: 4 | passed: 4 | failed: 0
BUILD SUCCESS
```

Si Karate falla en Swagger:

```text
Revisa primero /docs/<service>/v3/api-docs desde el Paso 9.
```

Si Karate falla en Profiles:

```text
Revisa IAM_JWK_SET_URI del profiles-service.
Debe apuntar al IAM real que emite los tokens.
```

## Checklist final

```text
[ ] Gateway responde /actuator/health con UP.
[ ] /docs/iam/v3/api-docs responde por gateway.
[ ] /docs/profiles/v3/api-docs responde por gateway.
[ ] /docs/payments/v3/api-docs responde por gateway.
[ ] /docs/appointments/v3/api-docs responde por gateway.
[ ] /docs/healthmonitoring/v3/api-docs responde por gateway.
[ ] /docs/medication/v3/api-docs responde por gateway.
[ ] /docs/reports-analytics/v3/api-docs responde por gateway si Reports ya esta desplegado.
[ ] Swagger consolidado abre en /swagger-ui.html.
[ ] Sign-up funciona por gateway.
[ ] Sign-in devuelve JWT por gateway.
[ ] Endpoint protegido funciona con Authorization Bearer.
[ ] Logs de los microservicios muestran requests cuando llamas al gateway.
[ ] /api/v1/internal/** devuelve 403 desde gateway.
[ ] Para arquitectura final, microservicios estan como Private Services.
[ ] INTERNAL_SERVICE_TOKEN no usa local-internal-token en produccion.
```

## Errores comunes

### El gateway responde UP, pero Swagger de un servicio falla

Causa probable:

```text
La variable *_SERVICE_URL esta mal o ese microservicio no esta Live.
```

Solucion:

```text
1. Copia de nuevo la URL real desde Render.
2. Pegala en la variable correcta del api-gateway.
3. Redeploy del gateway.
4. Prueba /docs/<service>/v3/api-docs otra vez.
```

### En Render configuraste localhost

Sintoma:

```text
El gateway intenta llamar http://localhost:8081, http://localhost:8082, etc.
```

Problema:

```text
En Render, localhost es el propio gateway.
```

Solucion:

```text
Usa URLs publicas onrender.com si estas probando servicios ya desplegados como Web Services.
Usa Internal Addresses si los microservicios son Private Services.
```

### Gateway devuelve 403 al llamar un microservicio

Causa probable:

```text
INTERNAL_SERVICE_TOKEN no coincide entre gateway y microservicio.
```

Solucion:

```text
1. Copia el mismo token en gateway y microservicios.
2. Redeploy de los microservicios afectados.
3. Redeploy del gateway.
```

### Login funciona, pero otro servicio rechaza el JWT

Causa probable:

```text
El microservicio no puede leer el JWKS de IAM.
```

Ejemplo para Profiles:

```text
IAM_JWK_SET_URI=<url-real-iam>/api/v1/jwks/.well-known/jwks.json
INTERNAL_SERVICE_TOKEN=<secret-compartido>
```

### Reports/Analytics falla en Swagger

Si Reports/Analytics aun no esta desplegado:

```text
Es esperado.
Primero despliega services/reports-analytics-service y configura REPORTS_ANALYTICS_SERVICE_URL en el gateway.
```

Si ya esta desplegado:

```text
1. Revisa REPORTS_ANALYTICS_SERVICE_URL en api-gateway.
2. Revisa que reports-analytics-service este Live.
3. Revisa INTERNAL_SERVICE_TOKEN.
4. Revisa que /v3/api-docs responda dentro del servicio.
```

### El gateway falla al arrancar con RouteProperties.getUri() is null

Sintoma en logs:

```text
Cannot invoke "java.net.URI.getScheme()" because the return value of
"org.springframework.cloud.gateway.server.mvc.config.RouteProperties.getUri()" is null
```

Causa probable:

```text
Alguna variable *_SERVICE_URL existe en Render pero esta vacia.
```

Solucion:

```text
1. Entra a api-gateway -> Environment.
2. Revisa IAM_SERVICE_URL, PROFILES_SERVICE_URL, PAYMENTS_SERVICE_URL, APPOINTMENTS_SERVICE_URL, HEALTHMONITORING_SERVICE_URL, MEDICATION_SERVICE_URL y REPORTS_ANALYTICS_SERVICE_URL.
3. Si alguna esta vacia, llenala con una URL real o eliminála.
4. Guarda cambios y redeploy del gateway.
```

Ejemplo incorrecto:

```text
REPORTS_ANALYTICS_SERVICE_URL=
```

Ejemplo correcto si Reports todavia no esta desplegado:

```text
No crear REPORTS_ANALYTICS_SERVICE_URL por ahora.
```

## Fuentes oficiales

- Render Web Services: https://render.com/docs/web-services
- Render Private Services: https://render.com/docs/private-services
- Render Private Network: https://render.com/docs/private-network
- Render Monorepo Support: https://render.com/docs/monorepo-support
- Render Health Checks: https://render.com/docs/health-checks
- Render Environment Variables: https://render.com/docs/configure-environment-variables
