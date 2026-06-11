# Guia de ejecucion de iam-service y profiles-service

Esta guia explica como levantar localmente los microservicios `iam-service` y `profiles-service` en el orden correcto.

## 1. Puertos usados

| Componente | Puerto local | Uso |
| --- | --- | --- |
| `iam-service` | `8081` | Autenticacion, usuarios, roles, JWKS |
| `profiles-service` | `8082` | Pacientes, doctores, familiares |
| PostgreSQL | `5433` | Base de datos local mediante Docker |
| RabbitMQ | `5672` | Broker de eventos |
| RabbitMQ Management | `15672` | Consola web de RabbitMQ |

URLs principales:

```text
IAM health:      http://localhost:8081/actuator/health
IAM Swagger:     http://localhost:8081/swagger-ui.html
Profiles health: http://localhost:8082/actuator/health
Profiles Swagger: http://localhost:8082/swagger-ui.html
RabbitMQ UI:     http://localhost:15672
```

Credenciales de RabbitMQ:

```text
usuario: medibridge
password: medibridge
```

## 2. Requisitos previos

Antes de ejecutar los microservicios debes tener:

- Docker Desktop abierto.
- Java 21 configurado.
- Terminal PowerShell ubicada en la raiz del repositorio.

Raiz esperada del repositorio:

```text
C:\Users\Sebas\IdeaProjects\medibridge\medibridge.microservices
```

Validar Java:

```powershell
java -version
```

Debe mostrar Java `21`.

## 3. Orden correcto de ejecucion

El orden correcto es:

```text
1. Levantar Docker/PostgreSQL/RabbitMQ
2. Levantar iam-service
3. Validar iam-service
4. Levantar profiles-service
5. Validar profiles-service
6. Ejecutar pruebas Karate o probar endpoints manualmente
```

No levantes `profiles-service` antes de `iam-service`, porque Profiles usa IAM para validar tokens JWT y consultar usuarios.

## 4. Levantar Docker

Desde la raiz del repositorio:

```powershell
docker compose -f docker/docker-compose.yml up -d
```

Validar que los contenedores esten activos:

```powershell
docker compose -f docker/docker-compose.yml ps
```

Resultado esperado:

```text
medibridge-postgres   Up ... healthy
medibridge-rabbitmq   Up ... healthy
```

Si aun aparece `starting`, espera unos segundos y vuelve a ejecutar el comando.

## 5. Levantar iam-service en puerto 8081

Abrir una terminal nueva en la raiz del repositorio y ejecutar:

```powershell
.\mvnw.cmd -f services/iam-service/pom.xml spring-boot:run
```

Esperar un mensaje similar:

```text
Tomcat started on port 8081
Started IamServiceApplication
```

Validar health en otra terminal:

```powershell
Invoke-RestMethod http://localhost:8081/actuator/health
```

Resultado esperado:

```json
{
  "status": "UP"
}
```

Abrir Swagger:

```text
http://localhost:8081/swagger-ui.html
```

## 6. Levantar profiles-service en puerto 8082

Primero confirma que IAM ya esta en `UP`.

Luego abre otra terminal nueva en la raiz del repositorio y ejecuta:

```powershell
.\mvnw.cmd -f services/profiles-service/pom.xml spring-boot:run
```

Esperar un mensaje similar:

```text
Tomcat started on port 8082
Started ProfilesServiceApplication
```

Validar health en otra terminal:

```powershell
Invoke-RestMethod http://localhost:8082/actuator/health
```

Resultado esperado:

```json
{
  "status": "UP"
}
```

Abrir Swagger:

```text
http://localhost:8082/swagger-ui.html
```

## 7. Verificar que los puertos estan ocupados

Puedes validar los puertos con PowerShell:

```powershell
Get-NetTCPConnection -LocalPort 8081
Get-NetTCPConnection -LocalPort 8082
Get-NetTCPConnection -LocalPort 5433
Get-NetTCPConnection -LocalPort 5672
```

Si un comando devuelve informacion, ese puerto esta en uso.

## 8. Ejecutar pruebas Karate despues de levantar ambos servicios

Cuando Docker, IAM y Profiles esten activos, puedes ejecutar todas las pruebas:

```powershell
.\mvnw.cmd -f tests/api-tests/pom.xml test
```

Solo IAM:

```powershell
.\mvnw.cmd -f tests/api-tests/pom.xml "-Dtest=IamRunner" test
```

Solo Profiles:

```powershell
.\mvnw.cmd -f tests/api-tests/pom.xml "-Dtest=ProfilesRunner" test
```

Guia completa de Karate:

```text
docs/GUIA_KARATE_API_TESTS_EJECUCION.md
```

## 9. Apagar servicios

Para detener `iam-service` y `profiles-service`, ir a cada terminal donde estan corriendo y presionar:

```text
Ctrl + C
```

Para detener Docker sin borrar datos:

```powershell
docker compose -f docker/docker-compose.yml down
```

Para detener Docker y borrar las bases locales:

```powershell
docker compose -f docker/docker-compose.yml down -v
```

Usa `down -v` solo si quieres limpiar datos locales y empezar desde cero.

## 10. Errores comunes

### Error: Connection to localhost:5433 refused

Causa:

```text
PostgreSQL de Docker no esta levantado.
```

Solucion:

```powershell
docker compose -f docker/docker-compose.yml up -d
docker compose -f docker/docker-compose.yml ps
```

Luego vuelve a levantar el microservicio.

### Error: Port 8081 already in use

Causa:

```text
Ya existe otro proceso usando el puerto de iam-service.
```

Validar:

```powershell
Get-NetTCPConnection -LocalPort 8081
```

Solucion:

- Detener el proceso anterior desde IntelliJ o con `Ctrl + C`.
- Si estaba corriendo como Spring Boot en IntelliJ, presionar `Stop`.

### Error: Port 8082 already in use

Causa:

```text
Ya existe otro proceso usando el puerto de profiles-service.
```

Validar:

```powershell
Get-NetTCPConnection -LocalPort 8082
```

Solucion:

- Detener el proceso anterior desde IntelliJ o con `Ctrl + C`.
- Si estaba corriendo como Spring Boot en IntelliJ, presionar `Stop`.

### Error en Profiles relacionado a JWKS o JWT

Causa:

```text
profiles-service necesita consultar la llave publica de iam-service.
```

Profiles usa esta URL:

```text
http://localhost:8081/api/v1/jwks/.well-known/jwks.json
```

Solucion:

1. Levantar primero `iam-service`.
2. Validar:

```powershell
Invoke-RestMethod http://localhost:8081/actuator/health
```

3. Luego levantar `profiles-service`.

### Error de Flyway validate

Causa posible:

```text
La base local quedo con una version anterior del esquema.
```

Si estas en entorno local de pruebas y puedes borrar datos:

```powershell
docker compose -f docker/docker-compose.yml down -v
docker compose -f docker/docker-compose.yml up -d
```

Luego vuelve a levantar IAM y Profiles.

### Error: Java no detectado

Validar:

```powershell
java -version
```

Si no aparece Java 21, configura el JDK en IntelliJ:

```text
File > Project Structure > Project SDK > Java 21
```

Tambien valida que las configuraciones de Spring Boot usen Java 21.

## 11. Resumen rapido

Comandos principales desde la raiz del repo:

```powershell
docker compose -f docker/docker-compose.yml up -d
.\mvnw.cmd -f services/iam-service/pom.xml spring-boot:run
.\mvnw.cmd -f services/profiles-service/pom.xml spring-boot:run
```

Validaciones:

```powershell
Invoke-RestMethod http://localhost:8081/actuator/health
Invoke-RestMethod http://localhost:8082/actuator/health
```

Pruebas Karate:

```powershell
.\mvnw.cmd -f tests/api-tests/pom.xml test
```
