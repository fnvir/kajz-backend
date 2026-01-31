
# API Gateway

API gateway for the backend microservices. Routes traffic to internal services, applies rate limits, and centralizes observability and discovery integration. 


## Features

- **Spring Cloud Gateway** with service discovery-based routing
- **Rate limiting** backed by Redis
- **Circuit breaker** support (Resilience4j)
- **OpenAPI aggregation** for downstream services with Swagger UI
- **Actuator + Prometheus** metrics and health probes
- **HTTP/2** and **virtual threads** enabled

## Responsibilities
- Terminates TLS and forwards requests to internal services
- Enforces global request policies (rate limits, timeouts, retries)
- Aggregates API docs for a single Swagger UI entry point
- Provides consistent error shaping and tracing headers

## Tech Stack

- **Java 25**, **Spring Boot 4**
- **Spring Cloud Gateway (WebFlux)**
- **Redis Reactive**
- **Resilience4j**
- **Eureka Client**
- **Micrometer Prometheus**

---

### Request Flow

Client → Gateway → Service Discovery → Downstream Service → Gateway → Client
- Routes are resolved via Eureka
- Filters apply authentication, rate limiting, and circuit breaker policies
- Metrics are exported via Actuator/Prometheus

## Prerequisites

- **JDK 25+**
- **Maven 3.9+**
- **Redis**
- **Eureka** (required for service discovery)

---

## Configuration

Key settings live in [src/main/resources/application.yaml](src/main/resources/application.yaml):

- Gateway routes for auth, notification, and storage services
- Redis connection for rate limiting
- CORS policy
- Service discovery endpoint
- Actuator/Prometheus exposure

---

## Run

```bash
./mvnw clean spring-boot:run
```

Or use Docker from the root [compose.yml](../compose.yml).

---

## License

See repository root for license information.
