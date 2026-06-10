# Guia de evidencias para pruebas Karate

Esta guia indica que capturas tomar para sustentar las pruebas de integracion/aceptacion con Karate en el documento del sprint.

## 1. Objetivo de las evidencias

Las evidencias deben demostrar:

- Que existe un proyecto de pruebas Karate.
- Que las pruebas estan escritas en Gherkin (`.feature`).
- Que los microservicios requeridos estan levantados.
- Que las pruebas de IAM pasan.
- Que las pruebas de Profiles pasan.
- Que el reporte HTML de Karate muestra resultados exitosos.

## 2. Evidencia de estructura del proyecto

Captura recomendada:

```text
tests/api-tests/src/test/java/pe/edu/upc/medibridge
```

Debe verse:

```text
MedibridgeApiTest.java
common/create-user-token.feature
iam/IamRunner.java
iam/iam.feature
profiles/ProfilesRunner.java
profiles/profiles.feature
```

Uso en el informe:

```text
Figura X. Estructura del proyecto de pruebas de integracion/aceptacion con Karate para IAM y Profiles.
```

## 3. Evidencia de configuracion Maven

Archivo para abrir:

```text
tests/api-tests/pom.xml
```

Captura recomendada:

- Dependencia `com.intuit.karate:karate-junit5`.
- Plugin `maven-surefire-plugin`.
- Configuracion de `testResources` usando `src/test/java`.

Uso en el informe:

```text
Figura X. Configuracion Maven del proyecto `api-tests` para ejecutar Karate con JUnit 5.
```

## 4. Evidencia de configuracion de URLs

Archivo para abrir:

```text
tests/api-tests/src/test/java/karate-config.js
```

Captura recomendada:

- `iamBaseUrl`.
- `profilesBaseUrl`.

Uso en el informe:

```text
Figura X. Configuracion de URLs base para las pruebas Karate de los microservicios.
```

## 5. Evidencias de microservicios levantados

### Captura de Docker

Comando:

```powershell
docker compose -f docker/docker-compose.yml ps
```

Captura esperada:

```text
medibridge-postgres   Up ... (healthy)
medibridge-rabbitmq   Up ... (healthy)
```

Uso en el informe:

```text
Figura X. Infraestructura local de pruebas levantada con Docker Compose.
```

### Captura de health IAM

Comando:

```powershell
Invoke-RestMethod http://localhost:8081/actuator/health
```

Resultado esperado:

```json
{
  "status": "UP"
}
```

Uso en el informe:

```text
Figura X. Health check exitoso de `iam-service`.
```

### Captura de health Profiles

Comando:

```powershell
Invoke-RestMethod http://localhost:8082/actuator/health
```

Resultado esperado:

```json
{
  "status": "UP"
}
```

Uso en el informe:

```text
Figura X. Health check exitoso de `profiles-service`.
```

## 6. Evidencias de pruebas IAM

### Archivo a mostrar

Abrir:

```text
tests/api-tests/src/test/java/pe/edu/upc/medibridge/iam/iam.feature
```

Capturas recomendadas:

1. Parte superior del archivo con `Feature: IAM Service API`.
2. Escenario `Sign up and sign in a user`.
3. Escenario `Reject protected users endpoint without token`.
4. Escenario `Check internal user existence endpoint`.

Uso en el informe:

```text
Figura X. Feature de Karate para pruebas de aceptacion del microservicio IAM.
```

### Ejecutar solo IAM

Comando:

```powershell
.\mvnw.cmd -f tests/api-tests/pom.xml "-Dtest=IamRunner" test
```

Captura esperada:

```text
feature: classpath:pe/edu/upc/medibridge/iam/iam.feature
scenarios: 5 | passed: 5 | failed: 0
BUILD SUCCESS
```

Uso en el informe:

```text
Figura X. Ejecucion exitosa de las pruebas Karate para `iam-service`.
```

### Reporte HTML IAM

Despues de ejecutar las pruebas, abrir:

```text
tests/api-tests/target/karate-reports/pe.edu.upc.medibridge.iam.iam.html
```

Capturas recomendadas:

- Cabecera del reporte con nombre de feature.
- Tabla de escenarios mostrando estado `passed`.
- Resumen donde figure `failed: 0`.

Uso en el informe:

```text
Figura X. Reporte HTML de Karate para los escenarios de IAM.
```

## 7. Evidencias de pruebas Profiles

### Archivo a mostrar

Abrir:

```text
tests/api-tests/src/test/java/pe/edu/upc/medibridge/profiles/profiles.feature
```

Capturas recomendadas:

1. Parte superior del archivo con `Feature: Profiles Service API`.
2. `Background`, donde se llama a `create-user-token.feature`.
3. Escenario `Create and get patient profile`.
4. Escenario `Create and get doctor profile using an IAM user`.
5. Escenario `Assign doctor and family member to a patient`.
6. Escenario `Reject duplicate doctor profile for the same IAM user`.

Uso en el informe:

```text
Figura X. Feature de Karate para pruebas de aceptacion del microservicio Profiles.
```

### Ejecutar solo Profiles

Comando:

```powershell
.\mvnw.cmd -f tests/api-tests/pom.xml "-Dtest=ProfilesRunner" test
```

Captura esperada:

```text
feature: classpath:pe/edu/upc/medibridge/profiles/profiles.feature
scenarios: 7 | passed: 7 | failed: 0
BUILD SUCCESS
```

Uso en el informe:

```text
Figura X. Ejecucion exitosa de las pruebas Karate para `profiles-service`.
```

### Reporte HTML Profiles

Despues de ejecutar las pruebas, abrir:

```text
tests/api-tests/target/karate-reports/pe.edu.upc.medibridge.profiles.profiles.html
```

Capturas recomendadas:

- Cabecera del reporte con nombre de feature.
- Tabla de escenarios mostrando estado `passed`.
- Resumen donde figure `failed: 0`.

Uso en el informe:

```text
Figura X. Reporte HTML de Karate para los escenarios de Profiles.
```

## 8. Evidencia del suite completo

Ejecutar:

```powershell
.\mvnw.cmd -f tests/api-tests/pom.xml test
```

Captura esperada:

```text
features:     2
scenarios:   12
passed:      12
failed:       0
BUILD SUCCESS
```

Abrir reporte general:

```text
tests/api-tests/target/karate-reports/karate-summary.html
```

Capturas recomendadas:

- Resumen general de Karate.
- Lista de features `iam.feature` y `profiles.feature`.
- Indicador de `failed: 0`.

Uso en el informe:

```text
Figura X. Resumen general de pruebas Karate para los microservicios IAM y Profiles.
```

## 9. Tabla sugerida para documentacion

Puedes usar esta tabla como base en el informe:

| Test ID | Microservicio | Feature file | Escenario | Endpoint principal | Resultado esperado |
| --- | --- | --- | --- | --- | --- |
| IAM-01 | IAM | `iam.feature` | Sign up and sign in a user | `POST /authentication/sign-up`, `POST /authentication/sign-in` | `201`, `200`, token generado |
| IAM-02 | IAM | `iam.feature` | Get public JWKS | `GET /jwks/.well-known/jwks.json` | `200`, llave RSA |
| IAM-03 | IAM | `iam.feature` | Reject protected users endpoint without token | `GET /users` | `401` |
| IAM-04 | IAM | `iam.feature` | Get users, roles and user by id with token | `GET /users`, `GET /roles` | `200` |
| IAM-05 | IAM | `iam.feature` | Check internal user existence endpoint | `GET /internal/users/{id}/exists` | `true` y `false` |
| PRO-01 | Profiles | `profiles.feature` | Reject protected profiles endpoint without token | `POST /profiles/patients` | `401` |
| PRO-02 | Profiles | `profiles.feature` | Create and get patient profile | `POST /profiles/patients`, `GET /profiles/patients/{id}` | `201`, `200` |
| PRO-03 | Profiles | `profiles.feature` | Create and get doctor profile using an IAM user | `POST /profiles/doctors`, `GET /profiles/doctors/{id}` | `201`, `200` |
| PRO-04 | Profiles | `profiles.feature` | Create and get family member profile using an IAM user | `POST /profiles/family-members`, `GET /profiles/family-members/{id}` | `201`, `200` |
| PRO-05 | Profiles | `profiles.feature` | Assign doctor and family member to a patient | `POST /profiles/patients/{id}/doctors/{id}`, `POST /profiles/patients/{id}/family-members/{id}` | `201`, relaciones activas |
| PRO-06 | Profiles | `profiles.feature` | Reject duplicate doctor profile for the same IAM user | `POST /profiles/doctors` | `409` |
| PRO-07 | Profiles | `profiles.feature` | Return not found for missing profile resources | `GET /profiles/patients/{id}` | `404` |

## 10. Commits para la seccion de Testing

Para evidenciar commits en el informe:

```powershell
git log --oneline --decorate -n 10
```

Captura recomendada:

- Commit donde se agregan `tests/api-tests`.
- Commit donde se documenta la guia en `docs`.

Uso en el informe:

```text
Figura X. Commits relacionados con las pruebas de integracion/aceptacion del sprint.
```
