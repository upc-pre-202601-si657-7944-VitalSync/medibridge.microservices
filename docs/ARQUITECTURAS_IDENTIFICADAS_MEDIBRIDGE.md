# Arquitecturas identificadas en MediBridge

Este documento identifica las arquitecturas y patrones presentes en el proyecto MediBridge a partir de la estructura real del repositorio, los servicios Spring Boot, las integraciones HTTP, RabbitMQ, WebSocket, Stripe y la configuracion Docker.

La forma de lectura recomendada es:

- Arquitectura identificada: patron encontrado.
- Evidencia en mi proyecto: archivos, carpetas o dependencias que lo demuestran.
- Por que aplica: razon tecnica por la que el patron corresponde al proyecto.

## 1. Resumen ejecutivo

MediBridge esta construido principalmente como una arquitectura de microservicios con servicios Spring Boot independientes, comunicacion REST, mensajeria asincrona con RabbitMQ, patrones tacticos de DDD, separacion por capas y una aproximacion a Clean Architecture / Onion / Hexagonal dentro de cada servicio.

Servicios identificados:

| Servicio | Responsabilidad principal |
|---|---|
| `iam-service` | Autenticacion, usuarios, roles y emision/validacion de contexto de identidad |
| `profiles-service` | Perfiles de pacientes, doctores, familiares y relaciones de cuidado |
| `payments-service` | Planes, suscripciones, pagos, facturacion e integracion con Stripe |
| `appointments-service` | Agendamiento, gestion y resumen de citas |
| `medication-service` | Medicamentos, horarios, dosis y alertas de stock |
| `healthmonitoring-service` | Signos vitales, observaciones clinicas y alertas |
| `communication-service` | Chat, mensajeria en tiempo real y eventos de comunicacion |
| `reports-analytics-service` | Reportes clinicos, dashboards analiticos y agregacion de datos |

Patrones presentes con evidencia fuerte:

- Microservicios.
- Arquitectura en capas.
- N-Tier / 3-Tier.
- REST.
- Request/Response.
- Mensajeria asincrona.
- Message Broker con RabbitMQ.
- Queue.
- Publish/Subscribe.
- Event-Driven Architecture.
- Circuit Breaker con Resilience4j.
- DDD tactico.
- Bounded Contexts.
- Ubiquitous Language.
- Context Mapping.
- Anti-Corruption Layer.
- CQRS ligero.
- WebSockets en `communication-service`.
- Integration Service / Adapter.

Patrones parcialmente presentes:

- Clean Architecture.
- Onion Architecture.
- Arquitectura Hexagonal.
- Core Domain / Supporting Subdomain / Generic Subdomain.

## 2. Arquitecturas de estructura

### Microservicios

| Campo | Descripcion |
|---|---|
| Arquitectura identificada | Microservicios |
| Evidencia en mi proyecto | Carpeta `services/` con `appointments-service`, `communication-service`, `healthmonitoring-service`, `iam-service`, `medication-service`, `payments-service`, `profiles-service` y `reports-analytics-service`. Cada uno tiene `pom.xml`, `Dockerfile` y `src`. |
| Por que aplica | El sistema esta separado en servicios independientes por capacidad de negocio. Cada servicio puede compilarse, contenerizarse y desplegarse por separado. |

Ejemplos:

- `services/iam-service`
- `services/profiles-service`
- `services/payments-service`
- `services/reports-analytics-service`
- `docker/docker-compose.yml`

### Arquitectura en capas / Layered Architecture

| Campo | Descripcion |
|---|---|
| Arquitectura identificada | Arquitectura en capas |
| Evidencia en mi proyecto | En los servicios se repite la estructura `domain`, `application`, `infrastructure` e `interfaces`. Tambien existen controladores REST, servicios de aplicacion, repositorios JPA, entidades/agregados y manejadores de excepciones. |
| Por que aplica | El codigo separa responsabilidades: entrada HTTP/WebSocket en `interfaces`, casos de uso en `application`, reglas y modelos en `domain`, y detalles tecnicos en `infrastructure`. |

Estructura comun observada:

```text
services/<service>/src/main/java/pe/edu/upc/medibridge/<context>/
  application/
  domain/
  infrastructure/
  interfaces/
```

Ejemplos:

- `services/reports-analytics-service/src/main/java/pe/edu/upc/medibridge/reportsanalytics/application`
- `services/reports-analytics-service/src/main/java/pe/edu/upc/medibridge/reportsanalytics/domain`
- `services/reports-analytics-service/src/main/java/pe/edu/upc/medibridge/reportsanalytics/infrastructure`
- `services/reports-analytics-service/src/main/java/pe/edu/upc/medibridge/reportsanalytics/interfaces`

### N-Tier / 3-Tier

| Campo | Descripcion |
|---|---|
| Arquitectura identificada | N-Tier / 3-Tier |
| Evidencia en mi proyecto | Hay clientes HTTP/Swagger, servicios backend Spring Boot, bases de datos por servicio y Docker Compose para levantar infraestructura. |
| Por que aplica | Existe separacion fisica/logica entre capa de presentacion o cliente, capa de APIs/backend y capa de datos. |

Capas identificadas:

| Tier | Evidencia |
|---|---|
| Presentacion / cliente | Swagger UI y clientes HTTP que consumen `/api/v1/**` |
| Logica de negocio | Microservicios Spring Boot dentro de `services/` |
| Datos / infraestructura | Bases de datos y RabbitMQ definidos en `docker/docker-compose.yml` |

### Clean Architecture, Onion y Hexagonal

| Campo | Descripcion |
|---|---|
| Arquitectura identificada | Clean Architecture / Onion Architecture / Arquitectura Hexagonal, con implementacion parcial |
| Evidencia en mi proyecto | El dominio expone modelos, comandos, queries y contratos de servicio; la aplicacion implementa casos de uso; infraestructura contiene JPA, Feign, RabbitMQ, Stripe y seguridad; interfaces contiene REST/WebSocket. Tambien existen adaptadores ACL y puertos de salida. |
| Por que aplica | Las dependencias conceptuales apuntan hacia el dominio y los detalles externos se ubican en adaptadores. No es una implementacion completamente pura porque el proyecto usa Spring/JPA en algunas clases del dominio, pero el estilo general corresponde a estas arquitecturas. |

Evidencia:

- Puertos de salida: `application/internal/outboundservices/acl`.
- Adaptadores: `infrastructure/acl`.
- API externa Stripe: `services/payments-service/src/main/java/pe/edu/upc/medibridge/payments/infrastructure/stripe/StripePaymentGatewayAdapter.java`.
- Entrada REST: `interfaces/rest/controllers`.
- Mensajeria: `infrastructure/messaging`.

Lectura por patron:

| Patron | Estado | Evidencia |
|---|---|---|
| Hexagonal | Parcial/presente | Puertos de salida y adaptadores para IAM, Profiles, Payments, Stripe y RabbitMQ |
| Clean Architecture | Parcial/presente | Capas `domain`, `application`, `infrastructure`, `interfaces` |
| Onion Architecture | Parcial/presente | Dominio como nucleo conceptual y capas externas para tecnologia |

## 3. Arquitecturas y patrones de dominio

### Domain-Driven Design - DDD

| Campo | Descripcion |
|---|---|
| Arquitectura identificada | DDD tactico |
| Evidencia en mi proyecto | El codigo usa conceptos de negocio como `Appointment`, `PatientProfile`, `Subscription`, `Medication`, `ClinicalReport`, `AnalyticsDashboard`, `HealthObservation`, `ChatRoom`, `Invoice`, `PaymentMethod`. Tambien hay paquetes `domain/model/aggregates`, `domain/model/entities`, `domain/model/valueobjects`, `domain/model/commands`, `domain/model/queries` y `domain/services`. |
| Por que aplica | Los modelos y servicios estan expresados con lenguaje del negocio medico, pagos, perfiles, reportes y comunicacion. |

Ejemplos:

- `services/profiles-service/src/main/java/pe/edu/upc/medibridge/profiles/domain/model/aggregates`
- `services/medication-service/src/main/java/pe/edu/upc/medibridge/medicationmanagement/domain/model/commands`
- `services/reports-analytics-service/src/main/java/pe/edu/upc/medibridge/reportsanalytics/domain/services`

### Bounded Contexts

| Campo | Descripcion |
|---|---|
| Arquitectura identificada | Bounded Contexts |
| Evidencia en mi proyecto | Cada servicio representa un contexto: IAM, Profiles, Payments, Appointments, Medication Management, Health Monitoring, Communication, Reports/Analytics. |
| Por que aplica | Cada contexto tiene su propio modelo, reglas, endpoints, repositorios e integraciones. Por ejemplo, `patientId` se usa como referencia en varios servicios, pero la autoridad del perfil del paciente esta en `profiles-service`. |

Mapa de contextos:

| Contexto | Servicio |
|---|---|
| Identidad y acceso | `iam-service` |
| Perfiles y relaciones de cuidado | `profiles-service` |
| Pagos y suscripciones | `payments-service` |
| Citas | `appointments-service` |
| Medicacion | `medication-service` |
| Monitoreo clinico | `healthmonitoring-service` |
| Comunicacion | `communication-service` |
| Reportes y analitica | `reports-analytics-service` |

### Ubiquitous Language

| Campo | Descripcion |
|---|---|
| Arquitectura identificada | Ubiquitous Language |
| Evidencia en mi proyecto | Los nombres de clases, endpoints y eventos usan terminos del dominio: `PatientRegistered`, `DoctorAssignedToPatient`, `ClinicalAlertTriggered`, `DoseAdministered`, `ClinicalReportGenerated`, `SubscriptionActivated`. |
| Por que aplica | El lenguaje del negocio esta reflejado directamente en el codigo y en las APIs. |

Ejemplos de lenguaje ubicuo:

- `GenerateClinicalReportCommand`
- `CreatePatientProfileCommand`
- `AssignDoctorToPatientCommand`
- `RecordDoseAdministrationCommand`
- `GetAnalyticsDashboardQuery`
- `PatientAccessDeniedException`
- `PremiumSubscriptionRequiredException`

### Core Domain, Supporting Subdomain y Generic Subdomain

| Campo | Descripcion |
|---|---|
| Arquitectura identificada | Clasificacion de subdominios |
| Evidencia en mi proyecto | Los servicios se agrupan alrededor de capacidades de salud, soporte operativo e integraciones genericas. |
| Por que aplica | No todos los servicios aportan el mismo tipo de valor. Algunos representan el nucleo medico del producto y otros soportan autenticacion, pagos o reportes. |

Clasificacion propuesta:

| Tipo | Servicios | Justificacion |
|---|---|---|
| Core Domain | `profiles-service`, `appointments-service`, `medication-service`, `healthmonitoring-service` | Gestionan pacientes, cuidado, citas, medicacion y monitoreo clinico, que son el valor principal de MediBridge |
| Supporting Subdomain | `reports-analytics-service`, `communication-service` | Apoyan el uso del sistema con reportes, dashboards y comunicacion, pero dependen de datos de otros contextos |
| Generic Subdomain | `iam-service`, parte de `payments-service` | Autenticacion y pagos son capacidades comunes que podrian apoyarse en soluciones externas, aunque aqui estan implementadas como servicios propios |

### Context Mapping

| Campo | Descripcion |
|---|---|
| Arquitectura identificada | Context Mapping |
| Evidencia en mi proyecto | Los servicios se integran por HTTP interno, Feign/WebClient y eventos RabbitMQ. Ejemplo: Reports consume Health Monitoring, Medication, Appointments, Profiles y Payments. Profiles consulta Payments para validar suscripciones. |
| Por que aplica | Hay relaciones explicitas entre contextos sin compartir directamente el modelo interno de cada servicio. |

Ejemplos:

- `reports-analytics-service` llama a:
  - `healthmonitoring-service` mediante `/api/v1/internal/health-monitoring/patients/{patientId}/summary`.
  - `medication-service` mediante `/api/v1/internal/medications/patients/{patientId}/summary`.
  - `appointments-service` mediante `/api/v1/internal/appointments/patients/{patientId}/summary`.
  - `profiles-service` para validar acceso y obtener datos del paciente.
  - `payments-service` para validar suscripcion.
- `profiles-service` consulta `payments-service` para reglas de planes y limites.
- `appointments-service`, `medication-service`, `healthmonitoring-service` y `reports-analytics-service` consultan `profiles-service` para acceso al paciente.

### Anti-Corruption Layer

| Campo | Descripcion |
|---|---|
| Arquitectura identificada | Anti-Corruption Layer |
| Evidencia en mi proyecto | Paquetes `infrastructure/acl` y `application/internal/outboundservices/acl`; recursos como `IamUserResponse`, `SubscriptionResponse`, `PlanResponse`; clientes Feign/WebClient y adaptadores. |
| Por que aplica | El dominio no consume directamente todos los DTO externos. Los adaptadores traducen respuestas de otros servicios hacia decisiones internas del contexto. |

Ejemplos:

- `services/reports-analytics-service/src/main/java/pe/edu/upc/medibridge/reportsanalytics/infrastructure/acl`
- `services/healthmonitoring-service/src/main/java/pe/edu/upc/medibridge/healthmonitoring/infrastructure/acl`
- `services/profiles-service/src/main/java/pe/edu/upc/medibridge/profiles/infrastructure/acl`
- `services/payments-service/src/main/java/pe/edu/upc/medibridge/payments/application/internal/outboundservices/acl/IamExternalSubscriptionService.java`

### CQRS ligero

| Campo | Descripcion |
|---|---|
| Arquitectura identificada | CQRS a nivel de aplicacion, no CQRS completo |
| Evidencia en mi proyecto | Hay paquetes y clases `commands`, `queries`, `CommandService` y `QueryService`. Ejemplos: `GenerateClinicalReportCommand`, `GetReportByIdQuery`, `SubscriptionCommandService`, `SubscriptionQueryService`. |
| Por que aplica | El proyecto separa intenciones de escritura y lectura en objetos y servicios distintos. No es CQRS completo porque no se identifican bases de datos separadas, modelos de lectura independientes o proyecciones dedicadas. |

Ejemplos:

- `domain/model/commands`
- `domain/model/queries`
- `domain/services/*CommandService.java`
- `domain/services/*QueryService.java`
- `application/internal/commandservices`
- `application/internal/queryservices`

### Shared Kernel

| Campo | Descripcion |
|---|---|
| Arquitectura identificada | Parcial o limitado |
| Evidencia en mi proyecto | Hay paquetes `shared` dentro de servicios, por ejemplo para excepciones REST, configuracion o recursos comunes locales. |
| Por que aplica parcialmente | Se comparten conceptos tecnicos dentro de cada servicio, pero no se identifica una libreria comun de dominio versionada y compartida entre varios servicios. |

## 4. Arquitecturas y patrones de comunicacion

### REST

| Campo | Descripcion |
|---|---|
| Arquitectura identificada | REST |
| Evidencia en mi proyecto | Controladores con `@RestController`, endpoints `/api/v1/**`, recursos JSON y metodos HTTP `GET`, `POST`, `PUT`, `DELETE`. |
| Por que aplica | Los servicios exponen APIs HTTP consumibles por Swagger, curl y otros servicios. |

Ejemplos:

- `services/reports-analytics-service/src/main/java/pe/edu/upc/medibridge/reportsanalytics/interfaces/rest/controllers`
- `services/payments-service/src/main/java/pe/edu/upc/medibridge/payments/interfaces/rest/controllers`
- `services/iam-service/src/main/java/pe/edu/upc/medibridge/iam/interfaces/rest/controllers`

### Request/Response

| Campo | Descripcion |
|---|---|
| Arquitectura identificada | Request/Response |
| Evidencia en mi proyecto | Swagger ejecuta requests y recibe respuestas inmediatas. Los servicios tambien llaman endpoints internos mediante Feign/WebClient y esperan respuesta. |
| Por que aplica | Operaciones como login, crear reporte, consultar suscripcion, validar paciente o consultar dashboard dependen de respuesta sincrona. |

Ejemplos:

- `GET /api/v1/subscriptions/users/{userId}`
- `POST /api/v1/clinical-reports`
- `GET /api/v1/analytics-dashboards/patients/{patientId}`
- Endpoints internos `/api/v1/internal/**`.

### Mensajeria asincrona, Message Broker, Queue y Publish/Subscribe

| Campo | Descripcion |
|---|---|
| Arquitectura identificada | Mensajeria asincrona con RabbitMQ |
| Evidencia en mi proyecto | Dependencia `spring-boot-starter-amqp`, clases `RabbitMQConfiguration`, `RabbitTemplate`, publishers y consumidores con `@RabbitListener`. |
| Por que aplica | Los servicios publican eventos de negocio y otros servicios los consumen sin bloquear la respuesta principal de todos los flujos. |

Evidencia:

- `services/*/infrastructure/messaging/RabbitMQConfiguration.java`
- `services/*/infrastructure/messaging/publishers/*IntegrationEventPublisher.java`
- `@RabbitListener` en `application/internal/eventhandlers` o `application/eventhandlers`.

Clasificacion:

| Patron | Estado | Evidencia |
|---|---|---|
| Message Broker | Presente | RabbitMQ en Docker y uso de Spring AMQP |
| Queue / Cola | Presente | Consumidores con `@RabbitListener(queues = ...)` |
| Publish/Subscribe | Presente | Eventos publicados por un servicio y consumidos por otros contextos |
| Mensajeria asincrona | Presente | Publicacion con `RabbitTemplate` y procesamiento posterior por listeners |

Eventos identificados:

- `UserRegistered`
- `PatientRegistered`
- `PatientDeactivated`
- `DoctorAssignedToPatient`
- `FamilyMemberAssignedToPatient`
- `SubscriptionActivated`
- `AppointmentScheduled`
- `ObservationRecorded`
- `ClinicalAlertTriggered`
- `MedicationRegistered`
- `DoseAdministered`
- `DoseSkipped`
- `StockLow`
- `ClinicalReportGenerated`

### Event-Driven Architecture

| Campo | Descripcion |
|---|---|
| Arquitectura identificada | Event-Driven Architecture |
| Evidencia en mi proyecto | Hay eventos de negocio publicados en RabbitMQ y handlers que reaccionan a esos eventos en otros servicios. |
| Por que aplica | Algunos servicios reaccionan a hechos ocurridos en otros contextos. Por ejemplo, Communication y Reports reaccionan a eventos clinicos o de medicacion; Appointments y Health Monitoring reaccionan a eventos de perfiles. |

Ejemplos:

- `reports-analytics-service/application/internal/eventhandlers`
- `communication-service/application/internal/eventhandlers`
- `appointments-service/application/internal/eventhandlers`
- `healthmonitoring-service/application/internal/eventhandlers`
- `medication-service/application/eventhandlers`

### WebSockets

| Campo | Descripcion |
|---|---|
| Arquitectura identificada | WebSockets |
| Evidencia en mi proyecto | `communication-service` tiene dependencia `spring-boot-starter-websocket`, controladores con `@MessageMapping` y uso de `SimpMessagingTemplate`. |
| Por que aplica | El chat y la presencia de usuarios requieren comunicacion persistente/en tiempo real. |

Ejemplos:

- `services/communication-service/src/main/java/pe/edu/upc/medibridge/communication/interfaces/websocket/controllers/ChatWebSocketController.java`
- `services/communication-service/src/main/java/pe/edu/upc/medibridge/communication/interfaces/websocket/controllers/ConnectedUserWebSocketController.java`

### Circuit Breaker

| Campo | Descripcion |
|---|---|
| Arquitectura identificada | Circuit Breaker |
| Evidencia en mi proyecto | Todos los servicios tienen dependencia `resilience4j-spring-boot3` y anotaciones `@CircuitBreaker` en llamadas a otros servicios, Stripe y publishers RabbitMQ. |
| Por que aplica | Evita que fallos de servicios externos o dependencias compartidas causen caidas en cascada. El fallback transforma errores tecnicos en errores controlados. |

Evidencia:

- `resilience4j.version` en los `pom.xml`.
- `@CircuitBreaker(name = "iamService", ...)`.
- `@CircuitBreaker(name = "profilesService", ...)`.
- `@CircuitBreaker(name = "paymentsService", ...)`.
- `@CircuitBreaker(name = "stripeApi", ...)`.
- `@CircuitBreaker(name = "rabbitMqPublisher", ...)`.

Ejemplos concretos:

- `services/payments-service/src/main/java/pe/edu/upc/medibridge/payments/infrastructure/stripe/StripePaymentGatewayAdapter.java`
- `services/reports-analytics-service/src/main/java/pe/edu/upc/medibridge/reportsanalytics/application/internal/outboundservices/acl/HealthMonitoringExternalService.java`
- `services/iam-service/src/main/java/pe/edu/upc/medibridge/iam/infrastructure/messaging/publishers/UserIntegrationEventPublisher.java`
- `services/profiles-service/src/main/java/pe/edu/upc/medibridge/profiles/infrastructure/acl/SubscriptionServiceAdapter.java`

### Integration Service / Adapter

| Campo | Descripcion |
|---|---|
| Arquitectura identificada | Integration Service / Adapter |
| Evidencia en mi proyecto | Clientes Feign/WebClient, adaptadores ACL, `StripePaymentGatewayAdapter` y servicios externos de aplicacion. |
| Por que aplica | Las integraciones con otros servicios o APIs externas estan encapsuladas en clases especificas, no mezcladas directamente en controladores. |

Ejemplos:

- `PaymentsServiceClient`
- `ProfilesServiceClient`
- `IamServiceClient`
- `HealthMonitoringServiceClient`
- `MedicationServiceClient`
- `AppointmentsServiceClient`
- `StripePaymentGatewayAdapter`

## 5. Checklist aplicado a MediBridge

| Pregunta | Respuesta en MediBridge | Patron identificado |
|---|---|---|
| Veo controller, service, repository? | Si | Layered Architecture |
| El dominio esta aislado de frameworks y BD? | Parcialmente | Hexagonal / Clean / Onion parcial |
| Tengo varios servicios independientes? | Si | Microservicios |
| Cada servicio representa una capacidad del negocio? | Si | DDD / Bounded Contexts |
| Tengo eventos de negocio? | Si | Event-Driven Architecture |
| Uso RabbitMQ, Kafka o ActiveMQ? | Si, RabbitMQ | Message Broker |
| Un mensaje lo procesa un solo consumidor? | Si, mediante colas | Queue |
| Un evento lo consumen varios servicios? | Si, segun bindings/listeners | Publish/Subscribe |
| Tengo endpoints HTTP con JSON? | Si | REST |
| Tengo comunicacion en vivo? | Si, en `communication-service` | WebSockets |
| Tengo integracion con sistemas externos? | Si, Stripe y servicios internos | Integration Service / Adapter / ACL |
| Tengo comandos y consultas separados? | Si | CQRS ligero |
| Uso Resilience4j, Hystrix o Polly? | Si, Resilience4j | Circuit Breaker |

## 6. Conclusiones

La arquitectura principal de MediBridge es:

```text
Microservicios + DDD tactico + Layered/Clean/Onion parcial
+ REST request/response
+ RabbitMQ event-driven
+ WebSocket realtime en Communication
+ Resilience4j Circuit Breaker
+ ACL/Adapters para integraciones internas y externas
```

La decision mas importante del proyecto es separar capacidades de negocio en servicios independientes y proteger las dependencias externas mediante adaptadores y circuit breakers. Esto permite que fallos en IAM, Profiles, Payments, Stripe, RabbitMQ o servicios clinicos no se propaguen como errores no controlados sin contexto.
