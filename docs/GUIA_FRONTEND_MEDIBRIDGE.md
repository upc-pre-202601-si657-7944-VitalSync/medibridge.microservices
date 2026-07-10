# Guia frontend MediBridge: Web Clinica y App Familiar

Esta guia define como avanzar el frontend de MediBridge en dos clientes separados:

- **Web clinica para doctores y personal medico**: React + Vite + TypeScript.
- **App movil para familiares**: React Native.

Ambos clientes consumen el mismo backend de microservicios, pero no deben tener las mismas pantallas ni la misma prioridad funcional. El usuario medico necesita una interfaz densa, orientada a gestion clinica. El familiar necesita una experiencia movil simple, enfocada en seguimiento, medicacion, citas, chat y notificaciones.

## 1. Division de productos

| Cliente | Tecnologia | Usuario objetivo | Objetivo principal |
|---|---|---|---|
| Web clinica | React + Vite + TypeScript | Doctores y personal medico | Gestionar pacientes, registrar informacion clinica, monitorear salud, generar reportes |
| App familiar | React Native | Familiares/cuidadores | Seguir el estado del paciente, gestionar visitas, ver medicacion, recibir alertas y comunicarse |

## 2. Backend compartido

El backend esta dividido en microservicios, pero los frontends deben consumirlos a traves del API Gateway. En local, la URL publica del backend para web y movil es `http://localhost:8080`. Los puertos internos de cada microservicio quedan para comunicacion backend, health checks y depuracion puntual.

| Entrada | Local URL | Uso principal |
|---|---|---|
| API Gateway | `http://localhost:8080` | Entrada unica para login, perfiles, pagos, citas, salud, medicacion, reportes y comunicacion |
| IAM interno | `http://localhost:8081` | Solo backend: login, usuarios, roles, JWKS |
| Profiles interno | `http://localhost:8082` | Solo backend: pacientes, doctores, familiares, care team |
| Payments interno | `http://localhost:8083` | Solo backend: suscripciones, planes, facturas |
| Appointments interno | `http://localhost:8084` | Solo backend: citas medicas y visitas familiares |
| Health Monitoring interno | `http://localhost:8085` | Solo backend: signos vitales, observaciones, alertas |
| Medication interno | `http://localhost:8086` | Solo backend: medicamentos, horarios, dosis, stock |
| Reports Analytics interno | `http://localhost:8087` | Solo backend: reportes, PDF, dashboard analitico |
| Communication interno | `http://localhost:8088` | Solo backend: chat, conectados, notificaciones |

## 3. Variables de entorno

### 3.1 Web clinica con Vite

Archivo `.env` del frontend web:

```env
VITE_API_BASE_URL=http://localhost:8080
```

### 3.2 App familiar con React Native

Si usas Expo, usa variables `EXPO_PUBLIC_*`:

```env
EXPO_PUBLIC_API_BASE_URL=http://localhost:8080
```

Importante para movil:

- En emulador Android, `localhost` apunta al emulador, no a tu PC. Normalmente usa `http://10.0.2.2:8080`.
- En dispositivo fisico, usa la IP local de tu PC, por ejemplo `http://192.168.1.20:8080`.
- No envies `X-Internal-Token` desde los frontends. Ese header lo agrega el API Gateway cuando llama a los microservicios.

## 4. Autenticacion compartida

Ambos frontends usan IAM.

| Accion | Metodo | Servicio | Endpoint |
|---|---|---|---|
| Registro | POST | Gateway -> IAM | `/api/v1/authentication/sign-up` |
| Login | POST | Gateway -> IAM | `/api/v1/authentication/sign-in` |
| Obtener usuario | GET | Gateway -> IAM | `/api/v1/users/{userId}` |
| Listar roles | GET | Gateway -> IAM | `/api/v1/roles` |

Request login:

```json
{
  "username": "doctor@test.com",
  "password": "Password123!"
}
```

Response:

```json
{
  "id": 5,
  "username": "doctor@test.com",
  "token": "jwt..."
}
```

Reglas:

- Guardar `userId`, `username` y `token`.
- Enviar `Authorization: Bearer <token>` en endpoints protegidos.
- Si el backend devuelve `401`, cerrar sesion y pedir login otra vez.
- `userId` siempre es el id de IAM. No confundirlo con `patientId`, `doctorProfileId` o `familyMemberProfileId`.

## 5. Web clinica: React + Vite + TypeScript

### 5.1 Stack

| Capa | Recomendacion |
|---|---|
| Framework | React + TypeScript |
| Build | Vite |
| Routing | React Router |
| HTTP | Axios o Fetch wrapper |
| Server state | TanStack Query |
| Forms | React Hook Form + Zod |
| UI | Tailwind + shadcn/ui o componentes propios |
| Auth storage | `localStorage` o session storage controlado |
| Charts | Recharts, Tremor o ECharts |
| PDF | Descarga por blob desde Reports |

### 5.2 Estructura sugerida

```text
medibridge-clinical-web/
  src/
    app/
      App.tsx
      router.tsx
      providers.tsx
    config/
      env.ts
    shared/
      api/
        httpClient.ts
        serviceUrls.ts
      components/
      layout/
      types/
      utils/
    modules/
      auth/
      dashboard/
      patients/
      care-team/
      appointments/
      medication/
      health/
      reports/
      analytics/
      subscriptions/
      communication/
```

### 5.3 Navegacion web clinica

| Ruta | Pantalla | Objetivo |
|---|---|---|
| `/login` | Login medico | Iniciar sesion |
| `/register` | Registro medico | Crear usuario medico |
| `/onboarding/doctor` | Crear perfil medico | Crear `DoctorProfile` |
| `/dashboard` | Dashboard clinico | Resumen de pacientes, alertas, citas y reportes |
| `/patients` | Gestion de pacientes | Buscar/seleccionar pacientes |
| `/patients/new` | Nuevo paciente | Crear paciente |
| `/patients/:patientId` | Vista 360 clinica | Resumen completo del paciente |
| `/patients/:patientId/care-team` | Equipo de cuidado | Asignar doctores/familiares |
| `/patients/:patientId/appointments` | Citas | Crear y consultar citas medicas |
| `/patients/:patientId/medications` | Medicacion | Medicamentos, horarios, dosis y stock |
| `/patients/:patientId/health` | Monitoreo clinico | Registrar signos vitales y ver alertas |
| `/patients/:patientId/reports` | Reportes | Generar, listar y descargar reportes |
| `/patients/:patientId/analytics` | Analitica | Ver metricas y tendencias |
| `/subscriptions` | Suscripcion institucional | Ver plan medico/institucional |
| `/chat` | Chat | Comunicacion con familiares/care team |
| `/notifications` | Notificaciones | Alertas y eventos |

### 5.4 Flujo medico

1. Medico se registra en IAM.
2. Inicia sesion.
3. Crea perfil doctor: `POST /api/v1/profiles/doctors`.
4. Crea o valida suscripcion institucional: `POST /api/v1/subscriptions`.
5. Crea paciente: `POST /api/v1/profiles/patients`.
6. Se asigna al paciente: `POST /api/v1/profiles/patients/{patientId}/doctors/{doctorProfileId}`.
7. Gestiona citas, medicacion y monitoreo.
8. Genera reportes clinicos y PDF.
9. Usa chat y notificaciones.

### 5.5 Pantallas web y endpoints

#### Login y registro medico

| Pantalla | Accion | Metodo | Endpoint |
|---|---|---|---|
| Login | Iniciar sesion | POST | IAM `/api/v1/authentication/sign-in` |
| Registro | Crear cuenta | POST | IAM `/api/v1/authentication/sign-up` |

Registro recomendado:

```json
{
  "username": "doctor@test.com",
  "password": "Password123!",
  "roles": ["ROLE_USER"]
}
```

#### Onboarding medico

| Accion | Metodo | Endpoint |
|---|---|---|
| Crear doctor | POST | Profiles `/api/v1/profiles/doctors` |
| Consultar doctor por id | GET | Profiles `/api/v1/profiles/doctors/{doctorProfileId}` |
| Crear suscripcion institucional | POST | Payments `/api/v1/subscriptions` |
| Consultar suscripcion activa | GET | Payments `/api/v1/subscriptions/users/{userId}/active` |

Crear doctor:

```json
{
  "fullName": "Dra. Demo"
}
```

Suscripcion institucional:

```json
{
  "userId": 5,
  "commercialLine": "INSTITUTION",
  "planType": "INSTITUTION_BASIC",
  "billingCycle": "MONTHLY"
}
```

#### Pacientes y vista 360

| Accion | Metodo | Endpoint |
|---|---|---|
| Crear paciente | POST | Profiles `/api/v1/profiles/patients` |
| Obtener paciente | GET | Profiles `/api/v1/profiles/patients/{patientId}` |
| Obtener care team | GET | Profiles `/api/v1/internal/profiles/patients/{patientId}/care-team-members` |
| Validar acceso | GET | Profiles `/api/v1/internal/profiles/users/{userId}/can-access/{patientId}` |

Crear paciente:

```json
{
  "fullName": "Paciente Geriatrico Demo"
}
```

Nota: actualmente no hay endpoint publico para listar todos los pacientes accesibles por usuario. Para una primera version, puedes usar paciente activo guardado localmente o agregar despues `GET /api/v1/profiles/users/{userId}/patients`.

#### Equipo de cuidado

| Accion | Metodo | Endpoint |
|---|---|---|
| Asignar doctor | POST | Profiles `/api/v1/profiles/patients/{patientId}/doctors/{doctorProfileId}` |
| Vincular familiar | POST | Profiles `/api/v1/profiles/patients/{patientId}/family-members/{familyMemberProfileId}` |

Reglas:

- El doctor requiere suscripcion institucional activa.
- Si devuelve `403`, mostrar bloqueo por plan o acceso.

#### Citas medicas

| Accion | Metodo | Endpoint |
|---|---|---|
| Crear cita medica | POST | Appointments `/api/v1/appointments/medical` |
| Obtener cita | GET | Appointments `/api/v1/appointments/{appointmentId}` |
| Listar citas del paciente | GET | Appointments `/api/v1/appointments/patient/{patientId}` |

Request:

```json
{
  "patientId": 9,
  "doctorProfileId": 3,
  "startsAt": "2026-07-01T12:00:00",
  "durationInMinutes": 45,
  "reason": "Control geriatrico"
}
```

#### Medicacion

| Accion | Metodo | Endpoint |
|---|---|---|
| Registrar medicamento | POST | Medication `/api/v1/medications` |
| Obtener medicamento | GET | Medication `/api/v1/medications/{medicationId}` |
| Listar medicamentos | GET | Medication `/api/v1/medications/patients/{patientId}` |
| Actualizar stock | PATCH | Medication `/api/v1/medications/{medicationId}/stock` |
| Listar bajo stock | GET | Medication `/api/v1/medications/patients/{patientId}/low-stock` |
| Crear horario | POST | Medication `/api/v1/medication-schedules` |
| Horarios activos | GET | Medication `/api/v1/medication-schedules/patients/{patientId}/active` |
| Registrar dosis | POST | Medication `/api/v1/dose-administrations` |
| Saltar dosis | POST | Medication `/api/v1/dose-administrations/skip` |
| Dosis por medicamento | GET | Medication `/api/v1/dose-administrations/medications/{medicationId}` |

#### Monitoreo clinico

| Accion | Metodo | Endpoint |
|---|---|---|
| Registrar observacion | POST | Health `/api/v1/health-monitoring/patients/{patientId}/observations` |
| Listar observaciones | GET | Health `/api/v1/health-monitoring/patients/{patientId}/observations` |
| Alertas activas | GET | Health `/api/v1/health-monitoring/patients/{patientId}/alerts/active` |
| Resumen clinico | GET | Health `/api/v1/health-monitoring/patients/{patientId}/summary` |

Request:

```json
{
  "recordedByDoctorProfileId": 3,
  "systolicBloodPressure": 160,
  "diastolicBloodPressure": 95,
  "bodyTemperature": 37.8,
  "painLevel": 4,
  "emotionalState": "CONFUSED",
  "emotionalNotes": "Desorientacion leve",
  "clinicalNotes": "Controlar presion arterial",
  "recordedAt": "2026-07-01T09:00:00"
}
```

#### Reportes y analitica

| Accion | Metodo | Endpoint |
|---|---|---|
| Generar reporte | POST | Reports `/api/v1/clinical-reports` |
| Generar y descargar PDF | POST | Reports `/api/v1/clinical-reports/{reportId}/pdf` |
| Volver a descargar PDF | GET | Reports `/api/v1/clinical-reports/{reportId}/pdf` |
| Obtener reporte | GET | Reports `/api/v1/clinical-reports/{reportId}` |
| Listar reportes | GET | Reports `/api/v1/clinical-reports/patients/{patientId}` |
| Dashboard analitico | GET | Reports `/api/v1/analytics-dashboards/patients/{patientId}` |

Request reporte:

```json
{
  "patientId": 9,
  "reportType": "FULL_CLINICAL",
  "startDate": "2026-07-01",
  "endDate": "2026-07-31"
}
```

## 6. App familiar: React Native

### 6.1 Stack

| Capa | Recomendacion |
|---|---|
| Framework | React Native |
| Opcion practica | Expo + TypeScript |
| Navegacion | React Navigation |
| HTTP | Axios o Fetch wrapper |
| Server state | TanStack Query |
| Forms | React Hook Form + Zod |
| Storage seguro | Expo SecureStore o AsyncStorage con criterio |
| Notificaciones locales | Expo Notifications, si se agregan push/local |
| Realtime | WebSocket/STOMP si se requiere chat en vivo |

### 6.2 Estructura sugerida

```text
medibridge-family-mobile/
  src/
    app/
      navigation/
      providers/
    config/
      env.ts
    shared/
      api/
      components/
      hooks/
      types/
    features/
      auth/
      onboarding/
      home/
      patients/
      appointments/
      medication/
      health/
      reports/
      chat/
      notifications/
      subscription/
```

### 6.3 Navegacion movil familiar

| Stack/Tab | Pantalla | Objetivo |
|---|---|---|
| AuthStack | Login | Iniciar sesion |
| AuthStack | Registro | Crear cuenta familiar |
| OnboardingStack | Crear perfil familiar | Crear `FamilyMemberProfile` |
| OnboardingStack | Elegir plan | Crear plan `FREE` o `FAMILY_PREMIUM` |
| MainTabs | Inicio | Resumen del paciente y alertas |
| MainTabs | Citas | Ver y programar visitas familiares |
| MainTabs | Medicacion | Ver medicamentos, horarios y registrar dosis |
| MainTabs | Salud | Ver observaciones/resumen/alertas |
| MainTabs | Chat | Conversacion con medico/care team |
| MainTabs | Notificaciones | Eventos y alertas recibidas |
| Stack detalle | Reportes | Ver reportes generados y descargar/abrir PDF |
| Stack detalle | Suscripcion | Ver o cambiar plan familiar |

### 6.4 Flujo familiar

1. Familiar se registra en IAM.
2. Inicia sesion.
3. Crea perfil familiar: `POST /api/v1/profiles/family-members`.
4. Crea suscripcion `FREE` o `FAMILY_PREMIUM`.
5. Se vincula a un paciente existente o crea uno nuevo si el flujo lo permite.
6. Consulta citas, medicacion, alertas y reportes.
7. Programa visitas familiares.
8. Usa chat y notificaciones.

### 6.5 Pantallas moviles y endpoints

#### Login y registro familiar

| Pantalla | Accion | Metodo | Endpoint |
|---|---|---|---|
| Login | Iniciar sesion | POST | IAM `/api/v1/authentication/sign-in` |
| Registro | Crear cuenta | POST | IAM `/api/v1/authentication/sign-up` |

Registro:

```json
{
  "username": "family@test.com",
  "password": "Password123!",
  "roles": ["ROLE_USER"]
}
```

#### Onboarding familiar

| Accion | Metodo | Endpoint |
|---|---|---|
| Crear familiar | POST | Profiles `/api/v1/profiles/family-members` |
| Consultar familiar por id | GET | Profiles `/api/v1/profiles/family-members/{familyMemberProfileId}` |
| Crear suscripcion familiar | POST | Payments `/api/v1/subscriptions` |
| Consultar suscripcion activa | GET | Payments `/api/v1/subscriptions/users/{userId}/active` |

Crear familiar:

```json
{
  "fullName": "Familiar Demo"
}
```

Suscripcion free:

```json
{
  "userId": 6,
  "commercialLine": "FAMILY",
  "planType": "FREE",
  "billingCycle": "MONTHLY"
}
```

#### Inicio familiar

| Widget | Metodo | Endpoint |
|---|---|---|
| Usuario actual | GET | IAM `/api/v1/users/{userId}` |
| Suscripcion activa | GET | Payments `/api/v1/subscriptions/users/{userId}/active` |
| Datos del paciente | GET | Profiles `/api/v1/profiles/patients/{patientId}` |
| Citas del paciente | GET | Appointments `/api/v1/appointments/patient/{patientId}` |
| Medicacion del paciente | GET | Medication `/api/v1/medications/patients/{patientId}` |
| Alertas activas | GET | Health `/api/v1/health-monitoring/patients/{patientId}/alerts/active` |
| Notificaciones no leidas | GET | Communication `/api/v1/notifications/recipients/{userId}/unread` |

Nota: igual que en web, falta un endpoint publico para listar pacientes accesibles por usuario. Para app familiar conviene agregar `GET /api/v1/profiles/users/{userId}/patients` antes de una version productiva.

#### Visitas familiares

| Accion | Metodo | Endpoint |
|---|---|---|
| Crear visita familiar | POST | Appointments `/api/v1/appointments/family-visits` |
| Obtener cita/visita | GET | Appointments `/api/v1/appointments/{appointmentId}` |
| Listar citas/visitas del paciente | GET | Appointments `/api/v1/appointments/patient/{patientId}` |

Request:

```json
{
  "patientId": 9,
  "familyMemberProfileId": 2,
  "startsAt": "2026-07-01T10:00:00",
  "durationInMinutes": 60,
  "reason": "Visita familiar semanal"
}
```

#### Medicacion familiar

La app familiar debe priorizar lectura y acciones simples.

| Accion | Metodo | Endpoint |
|---|---|---|
| Ver medicamentos | GET | Medication `/api/v1/medications/patients/{patientId}` |
| Ver horarios activos | GET | Medication `/api/v1/medication-schedules/patients/{patientId}/active` |
| Ver bajo stock | GET | Medication `/api/v1/medications/patients/{patientId}/low-stock` |
| Registrar dosis | POST | Medication `/api/v1/dose-administrations` |
| Saltar dosis | POST | Medication `/api/v1/dose-administrations/skip` |
| Ver historial por medicamento | GET | Medication `/api/v1/dose-administrations/medications/{medicationId}` |

#### Salud familiar

| Accion | Metodo | Endpoint |
|---|---|---|
| Ver observaciones | GET | Health `/api/v1/health-monitoring/patients/{patientId}/observations` |
| Ver alertas activas | GET | Health `/api/v1/health-monitoring/patients/{patientId}/alerts/active` |
| Ver resumen clinico | GET | Health `/api/v1/health-monitoring/patients/{patientId}/summary` |

La app familiar normalmente no deberia registrar observaciones clinicas de doctor. Si se permite en UI, debe ser un flujo controlado por permisos.

#### Reportes familiares

| Accion | Metodo | Endpoint |
|---|---|---|
| Listar reportes | GET | Reports `/api/v1/clinical-reports/patients/{patientId}` |
| Ver reporte | GET | Reports `/api/v1/clinical-reports/{reportId}` |
| Descargar PDF | GET | Reports `/api/v1/clinical-reports/{reportId}/pdf` |

En movil, generar reportes puede quedar fuera del MVP familiar. La version familiar puede iniciar solo con lectura de reportes generados por el personal medico.

#### Chat familiar

| Accion | Metodo | Endpoint |
|---|---|---|
| Enviar mensaje | POST | Communication `/api/v1/chat/messages` |
| Consultar conversacion | GET | Communication `/api/v1/chat/messages/{senderUserId}/{recipientUserId}` |
| Conectar usuario | POST | Communication `/api/v1/chat/users/connect` |
| Desconectar usuario | POST | Communication `/api/v1/chat/users/disconnect` |
| Listar conectados | GET | Communication `/api/v1/chat/users/connected` |

Request mensaje:

```json
{
  "recipientUserId": 5,
  "content": "Hola doctora, queria consultar sobre la medicacion.",
  "sentAt": "2026-07-01T11:00:00Z"
}
```

#### Notificaciones familiares

| Accion | Metodo | Endpoint |
|---|---|---|
| Listar notificaciones | GET | Communication `/api/v1/notifications/recipients/{recipientUserId}` |
| Listar no leidas | GET | Communication `/api/v1/notifications/recipients/{recipientUserId}/unread` |
| Marcar como leida | PATCH | Communication `/api/v1/notifications/{notificationId}/read` |

## 7. Funcionalidades compartidas entre ambos frontends

Conviene compartir contratos TypeScript manualmente o mediante un paquete comun si luego se trabaja en monorepo.

Modelos base:

```ts
export type AuthenticatedUser = {
  id: number;
  username: string;
  token: string;
};

export type PatientProfile = {
  id: number;
  fullName: string;
};

export type DoctorProfile = {
  id: number;
  userId: number;
  fullName: string;
};

export type FamilyMemberProfile = {
  id: number;
  userId: number;
  fullName: string;
};

export type SubscriptionStatus = "ACTIVE" | "CANCELLED" | "PAST_DUE" | "TRIALING";
export type CommercialLine = "FAMILY" | "INSTITUTION";
export type PlanType = "FREE" | "FAMILY_PREMIUM" | "INSTITUTION_BASIC" | "INSTITUTION_PREMIUM";
export type BillingCycle = "MONTHLY" | "ANNUALLY";

export type ReportType = "VITAL_SIGNS" | "MEDICATION" | "FULL_CLINICAL";
export type EmotionalState = "CALM" | "ANXIOUS" | "SAD" | "IRRITABLE" | "CONFUSED" | "APATHETIC";
```

## 8. Diferencias clave de UX

| Area | Web clinica | App familiar |
|---|---|---|
| Densidad | Alta, tablas, filtros, paneles | Baja/media, cards y acciones rapidas |
| Usuario principal | Doctor/personal medico | Familiar/cuidador |
| Pacientes | Gestion y vista clinica completa | Seguimiento de pacientes vinculados |
| Citas | Crear citas medicas y revisar agenda | Crear visitas familiares y ver agenda |
| Medicacion | Registrar/editar medicamento, horarios, stock | Ver medicacion, registrar dosis, alertas |
| Monitoreo | Registrar observaciones clinicas | Ver observaciones y alertas |
| Reportes | Generar, revisar, PDF, analytics | Leer reportes y abrir PDF |
| Chat | Conversaciones con familiares/care team | Conversaciones con doctores/care team |

## 9. Orden recomendado de implementacion

### Fase 1: base compartida

1. Definir contratos TypeScript.
2. Definir URLs por servicio.
3. Definir cliente HTTP con JWT.
4. Definir manejo comun de errores.

### Fase 2: web clinica MVP

1. Crear proyecto `medibridge-clinical-web` con Vite.
2. Implementar login/registro.
3. Implementar onboarding doctor.
4. Implementar crear paciente y asignar doctor.
5. Implementar vista 360 del paciente.
6. Implementar citas, medicacion y monitoreo.
7. Implementar reportes y dashboard.

### Fase 3: app familiar MVP

1. Crear proyecto `medibridge-family-mobile` con React Native/Expo.
2. Implementar login/registro.
3. Implementar onboarding familiar.
4. Implementar inicio con paciente seleccionado.
5. Implementar visitas familiares.
6. Implementar medicacion de lectura + administrar/saltar dosis.
7. Implementar notificaciones y chat.

### Fase 4: realtime

1. Integrar WebSocket/STOMP en web.
2. Integrar WebSocket/STOMP en movil.
3. Validar reconexion.
4. Validar usuario conectado/desconectado.

## 10. MVP por cliente

### 10.1 MVP web clinica

| Prioridad | Pantalla |
|---|---|
| 1 | Login |
| 1 | Registro |
| 1 | Onboarding doctor |
| 1 | Crear paciente |
| 1 | Vista 360 paciente |
| 1 | Citas medicas |
| 1 | Medicacion |
| 1 | Monitoreo clinico |
| 2 | Reportes clinicos |
| 2 | Dashboard analitico |
| 3 | Chat |
| 3 | Notificaciones |

### 10.2 MVP app familiar

| Prioridad | Pantalla |
|---|---|
| 1 | Login |
| 1 | Registro |
| 1 | Onboarding familiar |
| 1 | Inicio paciente |
| 1 | Visitas familiares |
| 1 | Medicacion y dosis |
| 2 | Alertas y resumen de salud |
| 2 | Notificaciones |
| 2 | Chat |
| 3 | Reportes PDF |
| 3 | Suscripcion familiar |

## 11. Manejo de errores

| Status | UI esperada |
|---|---|
| `400` | Mostrar campos invalidos o mensaje del backend |
| `401` | Cerrar sesion y volver a login |
| `403` | Mostrar acceso denegado, falta de care team o falta de plan |
| `404` | Mostrar recurso no encontrado |
| `409` | Mostrar conflicto de negocio |
| `500` | Mostrar error inesperado |
| `502` | Mostrar dependencia temporalmente no disponible |
| `503` | Mostrar servicio temporalmente no disponible |

Formato comun:

```json
{
  "timestamp": "2026-06-29T01:56:54.876079953Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Unexpected server error",
  "path": "/api/v1/...",
  "details": []
}
```

## 12. Consideraciones importantes

- `userId` es IAM.
- `patientId` es Profiles.
- `doctorProfileId` y `familyMemberProfileId` son Profiles.
- La web clinica debe priorizar flujos de doctor/personal medico.
- La app familiar debe priorizar seguimiento y comunicacion.
- Communication no tiene bloqueo premium.
- Health Monitoring y Reports son flujos premium.
- Appointments y Medication son producto base, pero requieren pertenecer al care team.
- Reports usa rango de fechas en `POST /api/v1/clinical-reports`.
- En React Native, revisar bien URLs locales por emulador/dispositivo fisico.
- Ambos clientes deben usar el API Gateway como unica URL base publica.

## 13. Mejoras backend utiles para ambos frontends

Estas mejoras no bloquean iniciar, pero simplifican mucho el desarrollo:

- `GET /api/v1/authentication/me` para validar token y usuario actual.
- `GET /api/v1/profiles/me` para saber si el usuario autenticado tiene perfil doctor o familiar.
- `GET /api/v1/profiles/users/{userId}/patients` para listar pacientes accesibles.
- Endpoint publico para `care-team-members`, sin usar `/internal`.
- `GET /api/v1/payments/plans` para listar planes disponibles.
- Mantener el API Gateway como entrada unica para web y movil.
