
# Auth Service

Authentication and user-account management microservice. Integrates with **Keycloak** for identity management, issues verification and recovery flows via **OTP**, and emits email events through **Kafka** to the notification pipeline.

---

## Features

- **User onboarding** — Registration flow with age validation.
- **Email verification** — OTP-based verification with secure expiry.
- **Password recovery** — OTP-driven reset flow.
- **Keycloak admin integration** — Creates users, updates email verification, resets credentials, and assigns realm roles.
- **Role assignment** — Default role attachment for new users (buyer/seller realm roles).
- **Email delivery via Kafka** — Publishes email events to the notification system.
- **OTP caching** — Hot-cache via Caffeine and distributed TTL storage in Redis.
- **Templated email content** — Thymeleaf templates for verification and reset emails.
- **Service discovery ready** — Eureka client integration.
- **Observability** — Actuator health and Prometheus metrics.
- **API versioning** — Header-based versioning with `X-API-Version`.
- **Virtual threads** — Enabled for efficient concurrency.

---

## Tech Stack

- **Java 25**, **Spring Boot 4**
- **Keycloak Admin Client**
- **Kafka** (email events)
- **Redis** + **Caffeine** (OTP cache)
- **Thymeleaf** (email templates)
- **Spring Cloud** (Eureka client)
- **Actuator + Micrometer Prometheus**
- **JUnit 6 / Mockito** (tests)

---

## Prerequisites

- **JDK 25+**
- **Maven 3.9+**
- **Redis**
- **Kafka**
- **Keycloak** (admin credentials + realm)
- **Eureka** (optional; required if discovery is enabled)

Or simply use Docker with the root [compose.yml](../compose.yml).

---

## Configuration

Key settings (env or [application.yaml](./src/main/resources/application.yaml)):

| Purpose | Property / Env | Notes |
|--------|----------------|------|
| Redis | `spring.data.redis.host`, `spring.data.redis.port`, `spring.data.redis.password` | OTP persistence |
| Kafka | `spring.kafka.bootstrap-servers` | Email event publishing |
| Keycloak server | `keycloak.server-url` | Admin endpoint |
| Admin realm | `keycloak.admin-realm` | Typically `master` |
| Admin client | `keycloak.admin-client-id` | Admin CLI/client |
| Admin user/pass | `keycloak.admin-username`, `keycloak.admin-password` | Admin credentials |
| User realm | `keycloak.user-realm` | Target realm for users |
| User client | `keycloak.user-client-id` | Client for user realm |
| Eureka | `eureka.client.serviceUrl.defaultZone` | Service registry URL |

OTP TTL defaults to ~6 minutes (see cache config). Adjust by updating cache/Redis TTL settings.

---

## Running the Service

### With Maven

```bash
./mvnw clean spring-boot:run
```

### With Docker

From the project root:

```bash
docker compose up -d --build auth-service
```

---

## Testing

```bash
./mvnw clean test
```

---

## Notes

- Email templates are located under [src/main/resources/templates](src/main/resources/templates).
- The service publishes email events to the Kafka topic `notification.email`.
- Built for internal use behind the API Gateway; external exposure is not required.

---

## License

See repository root for license information.
