# Guia de ejecucion de Karate API Tests

Esta guia explica como ejecutar las pruebas de integracion/aceptacion con Karate para los microservicios `iam-service` y `profiles-service`.

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

### Configuracion de URLs

Archivo:

```text
tests/api-tests/src/test/java/karate-config.js
```

Valores por defecto:

```js
iamBaseUrl: 'http://localhost:8081/api/v1'
profilesBaseUrl: 'http://localhost:8082/api/v1'
```

## 3. Requisitos previos

Antes de ejecutar Karate deben estar activos:

1. Docker Desktop.
2. PostgreSQL y RabbitMQ mediante `docker-compose`.
3. `iam-service` en el puerto `8081`.
4. `profiles-service` en el puerto `8082`.

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

## 6. Ejecutar todas las pruebas Karate

En una tercera terminal, desde la raiz del repositorio:

```powershell
.\mvnw.cmd -f tests/api-tests/pom.xml test
```

Resultado esperado:

```text
features:     2
scenarios:   12
passed:      12
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

## 9. Reportes generados

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
```

## 10. Que hace cada feature

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

## 11. Apagar entorno

Detener microservicios:

```powershell
Ctrl + C
```

en cada terminal donde esten corriendo `iam-service` y `profiles-service`.

Detener Docker:

```powershell
docker compose -f docker/docker-compose.yml down
```

Si quieres borrar datos persistidos de Postgres/RabbitMQ:

```powershell
docker compose -f docker/docker-compose.yml down -v
```

Usa `down -v` solo si quieres limpiar las bases y empezar desde cero.
