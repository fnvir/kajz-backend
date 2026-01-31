# Build Stage
FROM maven:3.9.12-eclipse-temurin-25-noble AS buildstage
WORKDIR /app

COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2,id=maven-cache \
    mvn -B dependency:resolve-plugins dependency:resolve

COPY src src
RUN --mount=type=cache,target=/root/.m2,id=maven-cache \
    mvn -B clean package -DskipTests

RUN java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination build

# Runtime Stage (Distroless)
FROM gcr.io/distroless/java25-debian13
WORKDIR /app

COPY --from=buildstage /app/build/dependencies/ .
COPY --from=buildstage /app/build/spring-boot-loader/ .
COPY --from=buildstage /app/build/snapshot-dependencies/ .
COPY --from=buildstage /app/build/application/ .

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
