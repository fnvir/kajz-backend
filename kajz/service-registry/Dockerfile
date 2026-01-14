# Build Stage
FROM maven:3.9.12-eclipse-temurin-25-noble AS buildstage
WORKDIR /app

# Copy pom.xml first and download dependencies (Leverages Docker layer cache)
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2,id=maven-cache \
    mvn -B dependency:resolve-plugins dependency:resolve

# Copy the rest of the source code and build the application
COPY src src
RUN --mount=type=cache,target=/root/.m2,id=maven-cache \
    mvn -B clean package -DskipTests

# Extract the layers from the built jar
RUN java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination build


# Runtime Stage
FROM eclipse-temurin:25-jre-noble
WORKDIR /app

# Install curl
RUN apt update && \
    apt install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Copy the built jar layers from the build stage
COPY --from=buildstage /app/build/dependencies/ .
COPY --from=buildstage /app/build/spring-boot-loader/ .
COPY --from=buildstage /app/build/snapshot-dependencies/ .
COPY --from=buildstage /app/build/application/ .

# Expose the application port
EXPOSE 8080

# Run as non-root for security
RUN useradd -r -s /bin/false spring
USER spring


# Execute the AOT cache training run
# https://docs.spring.io/spring-boot/reference/packaging/aot-cache.html#packaging.aot-cache.aot-cache
RUN java -XX:AOTCacheOutput=app.aot -Dspring.context.exit=onRefresh org.springframework.boot.loader.launch.JarLauncher

# Run the application
ENTRYPOINT ["java", "-XX:AOTCache=app.aot", "org.springframework.boot.loader.launch.JarLauncher"]
