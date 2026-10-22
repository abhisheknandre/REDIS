# syntax=docker/dockerfile:1

# ---------- Stage 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
COPY src ./src

# BuildKit cache mount keeps ~/.m2 between builds, so dependencies download only once.
# Tests are skipped here: run them in CI (mvn verify) before building the image.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -DskipTests clean package \
    && cp target/*.jar application.jar

# Split the fat jar into layers (dependencies / loader / snapshots / your code),
# so rebuilds after a code change only replace the tiny application layer.
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted


# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:21-jre-alpine

# Non-root user with a fixed numeric UID (Kubernetes runAsNonRoot needs a numeric ID)
RUN addgroup -S -g 10001 spring \
    && adduser -S -u 10001 -G spring -H -s /sbin/nologin spring

WORKDIR /application

# Least-changing layers first for better layer caching
COPY --from=build --chown=10001:10001 /build/extracted/dependencies/          ./
COPY --from=build --chown=10001:10001 /build/extracted/spring-boot-loader/    ./
COPY --from=build --chown=10001:10001 /build/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=10001:10001 /build/extracted/application/           ./

USER 10001:10001

# Container-aware heap sizing; exit on OOM so the orchestrator restarts the container cleanly
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

# Liveness only: it stays UP while Redis is down, matching your PostgreSQL fallback
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health/liveness || exit 1

# Exec form (no shell) so SIGTERM reaches the JVM directly for graceful shutdown
ENTRYPOINT ["java", "-jar", "application.jar"]