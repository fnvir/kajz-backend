# Build stage
FROM quay.io/keycloak/keycloak:26.4.7 AS builder

# Enable health and metrics support
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

# Enable HTTP
ENV KC_HTTP_RELATIVE_PATH=/
ENV KC_HTTP_ENABLED=true

# Use local cache
ENV KC_CACHE=local

# Configure database vendor
ENV KC_DB=postgres

WORKDIR /opt/keycloak

RUN /opt/keycloak/bin/kc.sh build

# Final stage
FROM quay.io/keycloak/keycloak:26.4.7

COPY --from=builder /opt/keycloak/ /opt/keycloak/

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"] 