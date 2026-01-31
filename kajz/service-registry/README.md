# Service Registry (Eureka Server)

This module hosts a **Netflix Eureka Server** used for client-side service discovery for microservices in the backend ecosystem.   Services register themselves with this server and discover other services dynamically.

This is not needed if you are using Kubernetes or another service mesh that provides built-in service discovery.

---

## Prerequisites
- **Java 25+**
- **Maven 3.9+**
- (Optional) Docker

---

## Run Locally

From the repository root:

```
cd kajz/service-registry
mvn spring-boot:run
```

Or use the docker compose file from the project root.

Default URL:

```
http://localhost:8761
```

You should see the Eureka dashboard with registered services.
