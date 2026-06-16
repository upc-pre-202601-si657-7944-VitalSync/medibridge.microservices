# Guia de ejecucion de Karate API Tests

Esta guia explica como ejecutar las pruebas de integracion/aceptacion con Karate para los microservicios `iam-service`, `profiles-service` y `api-gateway`.

## 1. Estructura creada

Las pruebas estan en un proyecto Maven independiente dentro del mismo repositorio:

```text
tests/api-tests/
  pom.xml
  README.md
  src/test/java/
    karate-config.js
    logback-test.xml
    pe/edu/upc/medibridge/
      MedibridgeApiTest.java
      common/create-user-token.feature
      gateway/GatewayRunner.java
      gateway/gateway.feature
      iam/IamRunner.java
      iam/iam.feature
      profiles/ProfilesRunner.java
      profiles/profiles.feature
```

## 2. Archivos principales

### Runner general

Archivo:

```text
tests/api-tests/src/test/java/pe/edu/upc/medibridge/MedibridgeApiTest.java
```

Este runner ejecuta todas las features bajo:

```text
classpath:pe/edu/upc/medibridge
```

Es el equivalente al ejemplo `CenterTest` que ejecuta varias pruebas en conjunto.

### Runner de IAM

Archivo:

```text
tests/api-tests/src/test/java/pe/edu/upc/medibridge/iam/IamRunner.java
```

Ejecuta:

```text
tests/api-tests/src/test/java/pe/edu/upc/medibridge/iam/iam.feature
```

### Runner de Profiles

Archivo:

```text
tests/api-tests/src/test/java/pe/edu/upc/medibridge/profiles/ProfilesRunner.java
```

Ejecuta:

```text
tests/api-tests/src/test/java/pe/edu/upc/medibridge/profiles/profiles.feature
```

### Runner de Gateway

Archivo:

```text
tests/api-tests/src/test/java/pe/edu/upc/medibridge/gateway/GatewayRunner.java
```

Ejecuta:

```text
tests/api-tests/src/test/java/pe/edu/upc/medibridge/gateway/gateway.feature
```

### Configuracion de URLs

Archivo:

```text
tests/api-tests/src/test/java/karate-config.js
```

Valores por defecto:

```js
iamBaseUrl: 'http://localhost:8081/api/v1'
profilesBaseUrl: 'http://localhost:8082/api/v1'
gatewayUrl: 'http://localhost:8080'
gatewayBaseUrl: 'http://localhost:8080/api/v1'
```

## 3. Requisitos previos

Antes de ejecutar Karate deben estar activos:

1. Docker Desktop.
2. PostgreSQL y RabbitMQ mediante `docker-compose`.
3. `iam-service` en el puerto `8081`.
4. `profiles-service` en el puerto `8082`.
5. `api-gateway` en el puerto `8080`.

## 3.1. Configurar IntelliJ si no deja ejecutar los runners

Si al abrir `IamRunner.java` o `ProfilesRunner.java` aparece este mensaje:

```text
Java file is located outside of the module source root, so it won't be compiled
```

significa que IntelliJ todavia no importo `tests/api-tests` como modulo Maven.

Solucion recomendada:

1. Abrir la ventana `Maven` de IntelliJ.
2. Clic en `+` o `Add Maven Project`.
3. Seleccionar este archivo:

```text
tests/api-tests/pom.xml
```

4. Clic en `Reload All Maven Projects`.
5. Verificar que `tests/api-tests/src/test/java` aparezca como carpeta de test.
6. Volver a abrir `IamRunner.java` o `ProfilesRunner.java`.

Solucion alternativa:

1. Clic derecho sobre:

```text
tests/api-tests/src/test/java
```

2. Seleccionar `Mark Directory as`.
3. Seleccionar `Test Sources Root`.

La opcion Maven es mejor porque tambien carga las dependencias de Karate.

Para correr desde IntelliJ sin depender del boton verde del archivo:

1. Ir a `Run > Edit Configurations`.
2. Crear una configuracion `Maven`.
3. Usar como working directory la raiz del repositorio.
4. Para IAM, usar este comando:

```text
-f tests/api-tests/pom.xml -Dtest=IamRunner test
```

5. Para Profiles, usar este comando:

```text
-f tests/api-tests/pom.xml -Dtest=ProfilesRunner test
```

6. Para Gateway, usar este comando:

```text
-f tests/api-tests/pom.xml -Dtest=GatewayRunner test
```

7. Para todo el suite, usar este comando:

```text
-f tests/api-tests/pom.xml test
```

## 3.2. Error comun: localhost:5433 refused

Si al arrancar `iam-service` aparece:

```text
Connection to localhost:5433 refused
```

no es un problema de Karate. Significa que `iam-service` intento conectarse a PostgreSQL, pero la base de datos del `docker-compose` no esta levantada.

Primero ejecuta:

```powershell
docker compose -f docker/docker-compose.yml up -d
```

Luego valida:

```powershell
docker compose -f docker/docker-compose.yml ps
```

Despues recien levanta `iam-service`.

## 4. Levantar infraestructura

Desde la raiz del repositorio:

```powershell
docker compose -f docker/docker-compose.yml up -d
```

Validar estado:

```powershell
docker compose -f docker/docker-compose.yml ps
```

Debe verse algo similar:

```text
medibridge-postgres   Up ... (healthy)
medibridge-rabbitmq   Up ... (healthy)
```

## 5. Levantar microservicios

Guia detallada para levantar ambos servicios paso a paso:

```text
docs/GUIA_EJECUCION_IAM_Y_PROFILES.md
```

### Terminal 1: IAM

```powershell
.\mvnw.cmd -f services/iam-service/pom.xml spring-boot:run
```

Esperar un mensaje similar:

```text
Tomcat started on port 8081
Started IamServiceApplication
```

Validar health:

```powershell
Invoke-RestMethod http://localhost:8081/actuator/health
```

Resultado esperado:

```json
{
  "status": "UP"
}
```

### Terminal 2: Profiles

```powershell
.\mvnw.cmd -f services/profiles-service/pom.xml spring-boot:run
```

Esperar un mensaje similar:

```text
Tomcat started on port 8082
Started ProfilesServiceApplication
```

Validar health:

```powershell
Invoke-RestMethod http://localhost:8082/actuator/health
```

Resultado esperado:

```json
{
  "status": "UP"
}
```

### Terminal 3: API Gateway

```powershell
.\mvnw.cmd -f services/api-gateway/pom.xml spring-boot:run
```

Esperar un mensaje similar:

```text
Tomcat started on port 8080
Started ApiGatewayApplication
```

Validar health:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Resultado esperado:

```json
{
  "status": "UP"
}
```

## 6. Ejecutar todas las pruebas Karate

En una tercera terminal, desde la raiz del repositorio:

```powershell
.\mvnw.cmd -f tests/api-tests/pom.xml test
```

Resultado esperado:

```text
features:     3
scenarios:   16
passed:      16
failed:       0
BUILD SUCCESS
```

## 7. Ejecutar solo pruebas de IAM

```powershell
.\mvnw.cmd -f tests/api-tests/pom.xml "-Dtest=IamRunner" test
```

Resultado esperado:

```text
feature: classpath:pe/edu/upc/medibridge/iam/iam.feature
scenarios: 5 | passed: 5 | failed: 0
BUILD SUCCESS
```

## 8. Ejecutar solo pruebas de Profiles

```powershell
.\mvnw.cmd -f tests/api-tests/pom.xml "-Dtest=ProfilesRunner" test
```

Resultado esperado:

```text
feature: classpath:pe/edu/upc/medibridge/profiles/profiles.feature
scenarios: 7 | passed: 7 | failed: 0
BUILD SUCCESS
```

## 9. Ejecutar solo pruebas de Gateway

```powershell
.\mvnw.cmd -f tests/api-tests/pom.xml "-Dtest=GatewayRunner" test
```

Resultado esperado:

```text
feature: classpath:pe/edu/upc/medibridge/gateway/gateway.feature
scenarios: 4 | passed: 4 | failed: 0
BUILD SUCCESS
```

## 10. Reportes generados

Despues de ejecutar Karate se genera:

```text
tests/api-tests/target/karate-reports/karate-summary.html
```

Para abrir el reporte desde PowerShell:

```powershell
Start-Process tests/api-tests/target/karate-reports/karate-summary.html
```

Tambien se generan reportes por feature:

```text
tests/api-tests/target/karate-reports/pe.edu.upc.medibridge.iam.iam.html
tests/api-tests/target/karate-reports/pe.edu.upc.medibridge.profiles.profiles.html
tests/api-tests/target/karate-reports/pe.edu.upc.medibridge.gateway.gateway.html
```

## 11. Que hace cada feature

### `iam.feature`

Archivo:

```text
tests/api-tests/src/test/java/pe/edu/upc/medibridge/iam/iam.feature
```

Escenarios incluidos:

- Registrar usuario con `POST /api/v1/authentication/sign-up`.
- Iniciar sesion con `POST /api/v1/authentication/sign-in`.
- Obtener JWKS publico con `GET /api/v1/jwks/.well-known/jwks.json`.
- Validar que `GET /api/v1/users` sin token responde `401`.
- Consultar usuarios, roles y usuario por id con token.
- Validar endpoint interno `GET /api/v1/internal/users/{id}/exists`.

### `profiles.feature`

Archivo:

```text
tests/api-tests/src/test/java/pe/edu/upc/medibridge/profiles/profiles.feature
```

Escenarios incluidos:

- Validar que `POST /api/v1/profiles/patients` sin token responde `401`.
- Crear y consultar paciente.
- Crear y consultar perfil de doctor usando un usuario real de IAM.
- Crear y consultar perfil de familiar usando un usuario real de IAM.
- Asignar doctor a paciente.
- Vincular familiar a paciente.
- Validar endpoints internos de autorizacion de cuidado.
- Validar conflicto `409` al crear perfil doctor duplicado.
- Validar `404` para paciente inexistente.

### `gateway.feature`

Archivo:

```text
tests/api-tests/src/test/java/pe/edu/upc/medibridge/gateway/gateway.feature
```

Escenarios incluidos:

- Enrutar autenticacion y consulta de usuarios de IAM por el gateway.
- Enrutar creacion y consulta de perfiles por el gateway usando token emitido desde el gateway.
- Bloquear endpoints internos `/api/v1/internal/**` desde el gateway.
- Exponer los OpenAPI JSON de IAM y Profiles proxied por el gateway.

## 12. Apagar entorno

Detener microservicios:

```powershell
Ctrl + C
```

en cada terminal donde esten corriendo `iam-service`, `profiles-service` y `api-gateway`.

Detener Docker:

```powershell
docker compose -f docker/docker-compose.yml down
```

Si quieres borrar datos persistidos de Postgres/RabbitMQ:

```powershell
docker compose -f docker/docker-compose.yml down -v
```

Usa `down -v` solo si quieres limpiar las bases y empezar desde cero.
