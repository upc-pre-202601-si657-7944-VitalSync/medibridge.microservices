# Guia paso a paso del API Gateway

Esta guia sirve para levantar `iam-service`, `profiles-service` y `api-gateway` localmente sin errores de puertos ocupados, y luego probar que el gateway enruta correctamente.

## Paso 1. Ubicarse en la raiz del proyecto

Abre PowerShell en:

```powershell
cd C:\Users\Sebas\IdeaProjects\medibridge\medibridge.microservices
```

Confirma que estas en la raiz:

```powershell
Get-ChildItem
```

Debes ver carpetas como:

```text
docker
docs
services
tests
```

## Paso 2. Revisar si los puertos ya estan ocupados

Antes de levantar servicios, revisa los puertos `8080`, `8081` y `8082`:

```powershell
Get-NetTCPConnection -LocalPort 8080,8081,8082 -State Listen -ErrorAction SilentlyContinue |
  ForEach-Object {
    $process = Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue
    [PSCustomObject]@{
      Port = $_.LocalPort
      PID = $_.OwningProcess
      Process = $process.ProcessName
      Path = $process.Path
    }
  }
```

Interpretacion:

```text
8080 debe estar libre antes de levantar api-gateway.
8081 debe estar libre antes de levantar iam-service.
8082 debe estar libre antes de levantar profiles-service.
```

Si un puerto aparece ocupado por una instancia anterior de este proyecto, ve a la terminal donde esta corriendo y presiona:

```text
Ctrl + C
```

Si no tienes esa terminal abierta, deten el proceso por PID:

```powershell
Stop-Process -Id <PID>
```

Ejemplo:

```powershell
Stop-Process -Id 18904
```

No sigas al siguiente paso hasta que `8080`, `8081` y `8082` esten libres o hasta que tengas claro que ese servicio ya esta corriendo y no necesitas levantarlo otra vez.

## Paso 3. Levantar PostgreSQL y RabbitMQ

En la misma terminal ubicada en la raiz:

```powershell
docker compose -f docker/docker-compose.yml up -d
```

Valida:

```powershell
docker compose -f docker/docker-compose.yml ps
```

Resultado esperado:

```text
medibridge-postgres   Up ... healthy
medibridge-rabbitmq   Up ... healthy
```

Si aparece `starting`, espera unos segundos y vuelve a ejecutar el comando de validacion.

## Paso 4. Levantar IAM en una terminal nueva

Abre una nueva terminal PowerShell en la raiz del proyecto:

```powershell
cd C:\Users\Sebas\IdeaProjects\medibridge\medibridge.microservices
```

Ejecuta:

```powershell
.\mvnw.cmd -f services/iam-service/pom.xml spring-boot:run
```

No cierres esta terminal.

Espera hasta ver algo parecido a:

```text
Tomcat started on port 8081
Started IamServiceApplication
```

Luego valida desde otra terminal:

```powershell
Invoke-RestMethod http://localhost:8081/actuator/health
```

Resultado esperado:

```json
{
  "status": "UP"
}
```

Si no responde exactamente como Spring Boot y ves algo como `Cannot GET /actuator/health`, entonces `8081` no es IAM. Vuelve al Paso 2 y libera el puerto.

## Paso 5. Levantar Profiles en otra terminal nueva

Abre otra terminal PowerShell en la raiz del proyecto:

```powershell
cd C:\Users\Sebas\IdeaProjects\medibridge\medibridge.microservices
```

Ejecuta:

```powershell
.\mvnw.cmd -f services/profiles-service/pom.xml spring-boot:run
```

No cierres esta terminal.

Espera hasta ver:

```text
Tomcat started on port 8082
Started ProfilesServiceApplication
```

Luego valida:

```powershell
Invoke-RestMethod http://localhost:8082/actuator/health
```

Resultado esperado:

```json
{
  "status": "UP"
}
```

Si aparece este error:

```text
Port 8082 was already in use
```

significa que ya hay otro proceso usando `8082`. No levantes otro Profiles encima. Vuelve al Paso 2, identifica el PID y detenlo, o usa el Profiles que ya estaba corriendo si su health responde `UP`.

## Paso 6. Levantar API Gateway en otra terminal nueva

Abre otra terminal PowerShell en la raiz del proyecto:

```powershell
cd C:\Users\Sebas\IdeaProjects\medibridge\medibridge.microservices
```

Ejecuta:

```powershell
.\mvnw.cmd -f services/api-gateway/pom.xml spring-boot:run
```

No cierres esta terminal.

Espera hasta ver:

```text
Tomcat started on port 8080
Started ApiGatewayApplication
```

Luego valida:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Resultado esperado:

```json
{
  "status": "UP"
}
```

Si aparece este error:

```text
Port 8080 was already in use
```

significa que ya hay otro gateway u otro proceso usando `8080`. Vuelve al Paso 2, identifica el PID y detenlo. Despues vuelve a ejecutar este Paso 6.

## Paso 7. Confirmar que los tres servicios estan arriba

En una terminal adicional, ejecuta:

```powershell
Invoke-RestMethod http://localhost:8081/actuator/health
Invoke-RestMethod http://localhost:8082/actuator/health
Invoke-RestMethod http://localhost:8080/actuator/health
```

Resultado esperado para los tres:

```json
{
  "status": "UP"
}
```

Si uno falla, no sigas. Corrige primero el servicio que falla.

## Paso 8. Probar que IAM y Profiles rechazan acceso directo

Aunque IAM escucha en `8081` y Profiles escucha en `8082`, esos puertos son para comunicacion interna. Un cliente normal no debe poder usar sus endpoints directamente.

Prueba IAM directo:

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8081/api/v1/users
```

Resultado esperado:

```text
403 Forbidden
```

Prueba Swagger directo de IAM:

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8081/v3/api-docs
```

Resultado esperado:

```text
403 Forbidden
```

Prueba Profiles directo:

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8082/api/v1/profiles/patients
```

Resultado esperado:

```text
403 Forbidden
```

Prueba Swagger directo de Profiles:

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8082/v3/api-docs
```

Resultado esperado:

```text
403 Forbidden
```

Nota:

```text
/actuator/health sigue abierto en 8081 y 8082 para validaciones locales y health checks de Render.
```

## Paso 9. Probar Swagger del gateway

Abre en el navegador:

```text
http://localhost:8080/swagger-ui.html
```

Debes ver un selector con:

```text
IAM Service
Profiles Service
```

Tambien puedes probar los JSON OpenAPI:

```powershell
Invoke-RestMethod http://localhost:8080/docs/iam/v3/api-docs
Invoke-RestMethod http://localhost:8080/docs/profiles/v3/api-docs
```

Si falla `/docs/iam/v3/api-docs`, IAM no esta bien levantado en `8081`.

Si falla `/docs/profiles/v3/api-docs`, Profiles no esta bien levantado en `8082`.

## Paso 10. Probar login y rutas por gateway con PowerShell

Crear un usuario por el gateway:

```powershell
$username = "gateway-test-" + [guid]::NewGuid().ToString()
$password = "Test123456!"

$signUpBody = @{
  username = $username
  password = $password
  roles = @("ROLE_USER")
} | ConvertTo-Json

$createdUser = Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8080/api/v1/authentication/sign-up `
  -ContentType "application/json" `
  -Body $signUpBody

$createdUser
```

Resultado esperado:

```text
Debe devolver id, username y roles.
```

Iniciar sesion por el gateway:

```powershell
$signInBody = @{
  username = $username
  password = $password
} | ConvertTo-Json

$auth = Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8080/api/v1/authentication/sign-in `
  -ContentType "application/json" `
  -Body $signInBody

$token = $auth.token
$auth
```

Resultado esperado:

```text
Debe devolver un token JWT.
```

Consultar el usuario por gateway:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/v1/users/$($createdUser.id)" `
  -Headers @{ Authorization = "Bearer $token" }
```

Crear paciente por gateway:

```powershell
$patientBody = @{
  fullName = "Paciente Gateway Test"
} | ConvertTo-Json

$patient = Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8080/api/v1/profiles/patients `
  -ContentType "application/json" `
  -Headers @{ Authorization = "Bearer $token" } `
  -Body $patientBody

$patient
```

Resultado esperado:

```text
Debe devolver id y fullName.
```

## Paso 11. Probar que el gateway bloquea endpoints internos

Ejecuta:

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8080/api/v1/internal/users/1/exists
```

Resultado esperado:

```text
403 Forbidden
```

Esto es correcto. El gateway no debe exponer `/api/v1/internal/**`.

## Paso 12. Ejecutar pruebas Karate del gateway

Con Docker, IAM, Profiles y Gateway activos:

```powershell
.\mvnw.cmd -f tests/api-tests/pom.xml "-Dtest=GatewayRunner" test
```

Resultado esperado:

```text
scenarios: 4 | passed: 4 | failed: 0
BUILD SUCCESS
```

Para ejecutar toda la suite:

```powershell
.\mvnw.cmd -f tests/api-tests/pom.xml test
```

## Paso 13. Apagar todo ordenadamente

En cada terminal donde corre un microservicio, presiona:

```text
Ctrl + C
```

Hazlo en este orden:

```text
1. api-gateway
2. profiles-service
3. iam-service
```

Luego apaga Docker:

```powershell
docker compose -f docker/docker-compose.yml down
```

## Errores comunes

### Error: `Port 8080 was already in use`

Causa:

```text
Ya hay un api-gateway anterior u otro proceso escuchando en 8080.
```

Solucion:

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen
Get-Process -Id <OwningProcess>
Stop-Process -Id <OwningProcess>
```

Luego vuelve al Paso 6.

### Error: `Port 8081 was already in use`

Causa:

```text
Ya hay otro proceso usando el puerto de IAM.
```

Solucion:

```powershell
Get-NetTCPConnection -LocalPort 8081 -State Listen
Get-Process -Id <OwningProcess>
Stop-Process -Id <OwningProcess>
```

Luego vuelve al Paso 4.

### Error: `Port 8082 was already in use`

Causa:

```text
Ya hay un profiles-service anterior u otro proceso escuchando en 8082.
```

Solucion:

```powershell
Get-NetTCPConnection -LocalPort 8082 -State Listen
Get-Process -Id <OwningProcess>
Stop-Process -Id <OwningProcess>
```

Luego vuelve al Paso 5.

### Error: `HTTP/1.1 header parser received no bytes`

Causa:

```text
El gateway esta llamando a un puerto donde no esta el microservicio correcto.
```

Ejemplo real:

```text
8081 ocupado por React Native / Node en vez de iam-service.
```

Solucion:

```powershell
Invoke-RestMethod http://localhost:8081/actuator/health
```

Si no devuelve:

```json
{
  "status": "UP"
}
```

entonces vuelve al Paso 2, libera `8081` y levanta IAM de nuevo.

### Directo a `8081` o `8082` devuelve `403 Forbidden`

Causa:

```text
Es el comportamiento correcto.
```

IAM y Profiles ahora requieren el header interno:

```text
X-Internal-Token
```

El gateway lo agrega automaticamente. Un navegador o cliente directo no lo tiene, por eso recibe `403`.

No pruebes endpoints de negocio por `8081` o `8082`. Pruebalos por:

```text
http://localhost:8080
```

## Variables para Render

En Render, el objetivo es:

```text
api-gateway publico
iam-service privado
profiles-service privado
```

Variables del `api-gateway`:

```text
IAM_SERVICE_URL=<iam-service-service-address>
PROFILES_SERVICE_URL=<profiles-service-service-address>
INTERNAL_SERVICE_TOKEN=<secret-compartido>
```

Variables de `profiles-service`:

```text
IAM_SERVICE_URL=<iam-service-service-address>
IAM_JWK_SET_URI=<iam-service-service-address>/api/v1/jwks/.well-known/jwks.json
INTERNAL_SERVICE_TOKEN=<secret-compartido>
```

Variables de `iam-service`:

```text
INTERNAL_SERVICE_TOKEN=<secret-compartido>
```

En IAM usa claves RSA estables en produccion:

```text
IAM_JWT_PRIVATE_KEY=<private-key>
IAM_JWT_PUBLIC_KEY=<public-key>
```
