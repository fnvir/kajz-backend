# Storage Service

Centralized file storage microservice for the Kajz platform. Handles uploads, downloads, and file metadata with pluggable cloud backends (AWS S3, Azure Blob Storage) and configurable access control.

## Features

- **Multi-vendor Cloud Storage Provider** — AWS S3 or Azure Blob Storage; switch via configuration.
- **Pre-signed upload flow** — Initiate upload to get a pre-signed URL, upload directly to the provider, then complete to validate and persist metadata.
- **Pre-signed download URLs** — Generate time-limited download URLs for private files (e.g. 3-minute expiry).
- **Access levels** — Per-file visibility:
  - **Public** — Anyone can access (optional auth).
  - **Protected** — Only authenticated users.
  - **Private** — Only the owner (and admins).
- **File validation** — Allowed MIME types and max file size (configurable; default e.g. images + PDF, 5MB).
- **Direct file serving** — Optional streaming of files through the service (for setups without a CDN).
- **Scheduled background cleanup** — ShedLock-backed job to remove stale/incomplete uploads and soft-deleted records.
- **OAuth2 resource server** — JWT-based authentication (e.g. Keycloak).
- **Service discovery** — Netflix Eureka client for registration and discovery.
- **Observability** — Actuator health and Prometheus metrics.
- **API versioning** — Request header `X-API-Version` (e.g. `1`).

## Tech Stack

- **Java 25**, **Spring Boot 4**
- **PostgreSQL** (JPA + Flyway)
- **Redis** (caching, ShedLock), **Caffeine**
- **SpringDoc OpenAPI 3** (Swagger UI)
- **Apache Tika** (content-type validation)
- **AWS SDK v2**, **Azure SDK for Java**
- **JUnit 6**, **Mockito**, **Testcontainers** (WIP)

## Prerequisites


- JDK 25+
- Maven 3.9+
- PostgreSQL
- Redis
- **Or just Docker**.

For cloud storage, one of:

- **AWS S3**: access key, secret key, region, bucket name  
- **Azure Blob**: connection string, container name  

Make sure to set up CORS policies in your cloud storage. 

## How to Run

### With Maven

1. Set environment variables for your chosen storage provider and DB/Redis (see [Configuration](#configuration)).
2. Start PostgreSQL and Redis (e.g. locally or via Docker).
3. Run the application:

   ```bash
   ./mvnw clean spring-boot:run
   ```

By default the app listens on **port 8083**.

### With Docker

Using the [docker compose file](../compose.yml) (from the project root):

```bash
docker compose up -d --build storage-service
```

Run the container with the required env vars and network (PostgreSQL, Redis, and optionally Eureka/OAuth2 issuer must be reachable). The Dockerfile exposes **port 8080**; override with `server.port` or map as needed.

## Configuration

Key settings (env or [application.yaml](./src/main/resources/application.yaml)):

| Purpose | Property / Env | Notes |
|--------|----------------|--------|
| Storage provider | `storage.provider` | `aws-s3` or `azure-blob` |
| AWS S3 | `AWS_ACCESS_KEY`, `AWS_SECRET_KEY`, `AWS_REGION`, `AWS_BUCKET_NAME` | Used when provider is `aws-s3` |
| Azure Blob | `AZURE_BLOB_CONNECTION_STRING`, `AZURE_BLOB_CONTAINER_NAME` | Used when provider is `azure-blob` |
| Database | `spring.datasource.url`, `username`, `password` | Defaults in `application.yaml` |
| Redis | `spring.data.redis.host`, `port`, `password` | For cache and ShedLock |
| OAuth2 | `spring.security.oauth2.resourceserver.jwt.issuer-uri` | JWT issuer (e.g. Keycloak) |
| Eureka | `eureka.client.serviceUrl.defaultZone` | Service discovery |

See configuration properties of above configs if needed.

Production profile (`application-prod.yaml`) enables Flyway, validates schema, and can disable Swagger.

## API Documentation

All HTTP endpoints, request/response schemas, and security requirements are documented in **Swagger UI**.

- **Local (default port):** [http://localhost:8083/storage-service/swagger-ui.html](http://localhost:8083/storage-service/swagger-ui.html)
- **API spec (OpenAPI 3):** [http://localhost:8083/storage-service/v3/api-docs](http://localhost:8083/storage-service/v3/api-docs)

Use Swagger UI to explore and call the APIs (initiate/complete upload, get file metadata, generate temp download URL, serve file, delete file, etc.).

## Testing
Run unit and integration tests with:

```bash
./mvnw clean test
```

or API testing with Postman (see postman collection in project root).

## License

See repository homepage for license information.
