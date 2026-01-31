# Kajz Platform Backend (a pet project)

**Backend** for Kajz - an online services marketplace where users register as sellers to offer services and customers discover and book tailored solutions from independent experts.


## Overview

Kajz is a multi-vendor platform enabling:

- **Sellers/Workers** - Register, create service offerings (gigs), manage availability and orders
- **Customers/Clients** - Search services according to their needs, book timeslots, place orders, pay via Stripe

The backend is built as a **microservices** architecture with Spring Boot 4 and Java 25. Payment integration with Stripe.

---

## Technologies

- **Core Language:** Java 25
- **Frameworks:** Spring Boot 4
- **Service Discovery & Gateway:** Spring Cloud Eureka, Spring Cloud Gateway
- **Security & Auth:** Spring Security, Keycloak
- **Persistence:** PostgreSQL, Redis, Flyway
- **Messaging & Streaming:** Apache Kafka
- **Cloud & Storage:** AWS S3, Azure Blob Storage
- **Containerization:** Docker, Kubernetes (WIP)
- **CI/CD & Build Tools:** Maven, GitHub Actions
- **Testing:** JUnit 6, Mockito, Testcontainers
- **Version Control:** Git, GitHub
- **Observability:** Spring Boot Actuator, Prometheus
- **API Docs:** SpringDoc OpenAPI 3, aggregated Swagger UI at gateway

---

## Architecture

*TODO: Add architecture diagram here*

---

## Microservices & Integrations

| Status | Service | Purpose |
|--------|---------|---------|
| [x] | [service-registry](./kajz/service-registry/) | Eureka server for service discovery |
| [x] | [api-gateway](./kajz/api-gateway/) | Gateway, rate limit, CORS, Swagger aggregation |
| [x] | [auth-service](./kajz/auth-service/) | User onboarding, OTP verification/reset, Keycloak admin |
| [x] | [notification-service](./kajz/notification-service/) | Email, SMS, push (SSE); Kafka events; cursor-paginated API |
| [x] | [storage-service](./kajz/storage-service/) | Initiate/complete upload, download, presigned URLs, metadata; S3/Azure |
| [ ] | **seller-service** | Seller/Worker profiles, verification, dashboards |
| [ ] | **customer-service** | Customer/Client profiles, preferences, history |
| [ ] | **gig-service** | Service listings (products), categories, pricing |
| [ ] | **search-service** | Full-text search with Elasticsearch |
| [ ] | **cart-service** | Cart and checkout validation |
| [ ] | **order-service** | Orders, fulfillment, status lifecycle |
| [ ] | **payout-service** | Seller payouts, Stripe Connect (planned) |
| [ ] | **chat-service** | Buyer–seller messaging |

**Planned tech:** gRPC/OpenFeign for inter-service communication; **Stripe** for payments.

### Integrations
- **Keycloak** - OAuth2/OIDC identity provider for user management and authentication
- **Apache Kafka** - Event-driven architecture, decoupled services, async processing
- **AWS S3 / Azure Blob Storage** - Scalable object storage for user uploads
- **Redis** - Caching, rate limiting, session management
- **Elasticsearch** - Full-text search engine (planned)
- **PostgreSQL** - Relational database for structured data
- **MongoDB** - NoSQL database for chat messages (planned)


### Features by service

- [api-gateway](./kajz/api-gateway/README.md) - API Gateway with rate limiting for all services 
- [auth-service](./kajz/auth-service/README.md) - User registration and authentication; Reset password via OTP email; Keycloak admin integration
- [notification-service](./kajz/notification-service/README.md) - Central notification hub for microservices; Email Notifications, SMS Notifications, or Push Notifications. Real time notification delivery with Server-Sent Events (SSE); Cursor-based pagination for efficient notification retrieval
- [storage-service](./kajz/storage-service/README.md) - Centralized file storage management for microservices; Supports AWS S3 and Azure Blob Storage

- More Upcoming...

To learn more, see each service's `README.md`.


---

## Configuration

- **Root env:** `kajz/.env` (see [.env.example`](./kajz/.env.example)) - DB names, ports, Keycloak, Redis, Stripe placeholders, AWS/Azure, Twilio, SMTP.
- **Per service:** `kajz/<service>/src/main/resources/application.yaml` (and profile-specific files).
- **Compose:** `kajz/compose.yml` - all services, DBs, Keycloak, Redis, Kafka, Eureka, gateway, pgAdmin, etc.

---

## Quick Start

### Prerequisites: Docker (recommended)

0. **Clone repo**

    ```sh
    git clone https://github.com/fnvir/kajz-backend
    ```

1. **CD and set environment**

    ```bash
    cd kajz
    cp .env.example .env
    # Edit .env: DB credentials, Keycloak, Redis, AWS/Azure, Twilio, SMTP
    ```

2. **Start infrastructure and services**

    ```bash
    docker compose up -d --build
    ```

    This starts the whole backend and all dependencies.

### Optional: Run services locally

If you need to run a service outside Docker, check its README or `application.yml` for prerequisites (e.g. kafka/keycloak), then run:

```bash
cd kajz/<service-name>
./mvnw clean spring-boot:run
```

---

## CI/CD

GitHub Actions workflows (`.github/workflows/`) build and test each service independently.

---

## Documentation
Each service has its own `README.md` with run instructions, config tables, and API notes.

---

## License

See [LICENSE](LICENSE) file.
