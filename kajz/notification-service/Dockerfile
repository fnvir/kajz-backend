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

# Runtime Stage (Distroless)
FROM gcr.io/distroless/java25-debian13
WORKDIR /app

# Copy the built jar layers from the build stage
COPY --from=buildstage /app/build/dependencies/ .
COPY --from=buildstage /app/build/spring-boot-loader/ .
COPY --from=buildstage /app/build/snapshot-dependencies/ .
COPY --from=buildstage /app/build/application/ .

# Expose the application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
