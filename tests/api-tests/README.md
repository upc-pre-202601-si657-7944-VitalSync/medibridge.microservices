# MediBridge API Tests

Karate integration and acceptance tests for all MediBridge microservices:
`iam-service`, `profiles-service`, `payments-service`, `appointments-service`,
`healthmonitoring-service`, `medication-service`, `reports-analytics-service`,
`communication-service`, and `api-gateway`.

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

```powershell
.\mvnw.cmd -f services/payments-service/pom.xml spring-boot:run "-Dspring-boot.run.arguments=--payments.mock.enabled=true"
```

```powershell
.\mvnw.cmd -f services/appointments-service/pom.xml spring-boot:run
```

```powershell
.\mvnw.cmd -f services/healthmonitoring-service/pom.xml spring-boot:run
```

```powershell
.\mvnw.cmd -f services/medication-service/pom.xml spring-boot:run
```

```powershell
.\mvnw.cmd -f services/reports-analytics-service/pom.xml spring-boot:run
```

```powershell
.\mvnw.cmd -f services/communication-service/pom.xml spring-boot:run
```

```powershell
.\mvnw.cmd -f services/api-gateway/pom.xml spring-boot:run
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
paymentsBaseUrl=http://localhost:8083/api/v1
appointmentsBaseUrl=http://localhost:8084/api/v1
healthMonitoringBaseUrl=http://localhost:8085/api/v1
medicationBaseUrl=http://localhost:8086/api/v1
reportsBaseUrl=http://localhost:8087/api/v1
communicationBaseUrl=http://localhost:8088/api/v1
gatewayUrl=http://localhost:8080
gatewayBaseUrl=http://localhost:8080/api/v1
internalToken=local-internal-token
```

Override them with Maven properties when needed:

```powershell
.\mvnw.cmd -f tests/api-tests/pom.xml test "-Diam.baseUrl=http://localhost:8081/api/v1" "-Dprofiles.baseUrl=http://localhost:8082/api/v1" "-Dpayments.baseUrl=http://localhost:8083/api/v1" "-Dappointments.baseUrl=http://localhost:8084/api/v1" "-Dhealthmonitoring.baseUrl=http://localhost:8085/api/v1" "-Dmedication.baseUrl=http://localhost:8086/api/v1" "-Dreports.baseUrl=http://localhost:8087/api/v1" "-Dcommunication.baseUrl=http://localhost:8088/api/v1" "-Dgateway.url=http://localhost:8080" "-Dgateway.baseUrl=http://localhost:8080/api/v1"
```
