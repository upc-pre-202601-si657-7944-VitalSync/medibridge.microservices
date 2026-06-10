# MediBridge API Tests

Karate integration and acceptance tests for `iam-service` and `profiles-service`.

## Prerequisites

Start infrastructure:

```powershell
docker compose -f docker/docker-compose.yml up -d
```

Start the microservices in separate terminals:

```powershell
.\mvnw.cmd -f services/iam-service/pom.xml spring-boot:run
```

```powershell
.\mvnw.cmd -f services/profiles-service/pom.xml spring-boot:run
```

## Run Tests

```powershell
.\mvnw.cmd -f tests/api-tests/pom.xml test
```

Reports are generated under:

```text
tests/api-tests/target/karate-reports/
```

## Configuration

Default URLs are configured in `src/test/java/karate-config.js`:

```text
iamBaseUrl=http://localhost:8081/api/v1
profilesBaseUrl=http://localhost:8082/api/v1
```

Override them with Maven properties when needed:

```powershell
.\mvnw.cmd -f tests/api-tests/pom.xml test "-Diam.baseUrl=http://localhost:8081/api/v1" "-Dprofiles.baseUrl=http://localhost:8082/api/v1"
```
