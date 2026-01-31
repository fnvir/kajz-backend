# Notification Service

A centralized microservice for managing multi-channel notifications. Built with Spring Boot 4, it delivers **email**, **SMS**, and **push notifications** through an event-driven Kafka architecture and exposes real-time updates via Server-Sent Events (SSE).

---

## Features

### What it supports

- Notification CRUD with cursor-based pagination
- Read/delete operations with ownership validation
- Admin-only publish endpoints for email, SMS, and push notifications
- Kafka-based event publishing and consumers with retry + DLQ handlers
- Email sending via SMTP with provider auto-config (Gmail/Microsoft/Basic)
- Microsoft 365 OAuth2 SMTP via MSAL
- SMS sending via Twilio with strict phone format validation
- Push notifications via SSE with replay and heartbeat
- Per-user SSE buffering with bounded history and idle cleanup
- OAuth2 resource server (Keycloak issuer URI) with role mapping
- Postgres persistence with JSONB metadata fields
- UUIDv7 identifiers for notifications
- Flyway migrations (profile-based)
- Eureka client integration (profile-based)
- Prometheus metrics + health probes via Actuator
- Virtual thread support enabled in configuration
- OpenAPI/Swagger UI

### Notification Channels

| Channel | Description |
|---------|-------------|
| **Email** | SMTP-based delivery. Supports Gmail, Microsoft 365 (OAuth2), or custom SMTP |
| **SMS** | Twilio integration for text message delivery |
| **Push** | In-app notifications with role-based filtering (Worker, Client, Admin) |

### Core Capabilities

- **Event-driven architecture** — Consumes events from Kafka topics (`notification.email`, `notification.sms`, `notification.push`) and publishes notifications via REST
- **Real-time push (SSE)** — Server-Sent Events stream for live notifications with Last-Event-ID reconnection support
- **Notification CRUD** — Cursor-based pagination, mark-as-read, delete
- **Role-based filtering** — Notifications filtered by recipient role (WORKER, CLIENT, ADMIN)
- **OAuth2 / JWT** — Keycloak-based authentication with realm roles
- **Service discovery** — Eureka client for registration in microservice mesh
- **Caching** — Caffeine cache for performance
- **Observability** — Actuator health checks, Prometheus metrics
- **Database** — PostgreSQL with Flyway migrations and UUIDv7 primary keys

---

## Tech Stack

- **Java 25** · **Spring Boot 4** · **Spring WebFlux**
- **Spring Kafka** · **Spring Security (OAuth2 Resource Server)**
- **Spring Data JPA** · **PostgreSQL** · **Flyway**
- **Spring Cloud** (Eureka, OpenFeign)
- **Twilio** (SMS) · **JavaMail** (Email)
- **SpringDoc OpenAPI 3** (Swagger UI)
- **JUnit 6** · **Mockito** · **Testcontainers**

---

## Prerequisites

- **Java 25**
- **Maven 3.9+**
- **PostgreSQL**
- **Kafka**
- **Keycloak** (OAuth2 issuer)
- **Eureka** (optional; disabled in dev profile)

---

## Configuration

### Environment Variables

| Variable | Description |
|----------|-------------|
| `MAIL_USERNAME` | SMTP username |
| `MAIL_PASSWORD` | SMTP password (use [Gmail App Password](https://support.google.com/mail/answer/185833) for Gmail) |
| `MAIL_ADDRESS` | Sender email address |
| `TWILIO_ACC_SID` | Twilio Account SID (SMS) |
| `TWILIO_AUTH_TOKEN` | Twilio Auth Token |
| `TWILIO_PHN_NUMBER` | Twilio phone number |

### Profiles

| Profile | Description |
|---------|-------------|
| `dev` | Default. Eureka disabled, Twilio disabled, SQL logging enabled |
| `test` | Test configuration |
| `prod` | Eureka enabled, Twilio enabled, Flyway enabled |

---

## Running the Service

### With Maven

```bash
./mvnw spring-boot:run
```

Or with a specific profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### With Docker

Use the [compose.yml](compose.yml) to run with Docker Compose.

```bash
docker compose up -d --build notification-service
```

---

## API Documentation

Interactive API documentation is available via **Swagger UI** when the service is running:

- **Swagger UI:** `http://localhost:8082/notification-service/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8082/notification-service/v3/api-docs`
- **OpenAPI YAML:** `http://localhost:8082/notification-service/v3/api-docs.yaml`

Use Swagger UI to explore all endpoints, request/response schemas, and test authenticated calls with a JWT Bearer token.

---

## Testing
- Unit and integration tests with JUnit 6 and Mockito
- Testcontainers for PostgreSQL and Kafka in integration tests

Run tests with:

```bash
./mvnw clean verify
```

## License

See project root for license information.
