# syntax=docker/dockerfile:1
# ============================================================
# Stage 1: Build
# ============================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy wrapper first — changes rarely, maximises layer cache hits
COPY mvnw .
COPY .mvn/ .mvn/
RUN chmod +x mvnw

# Two-level caching strategy:
#   Layer cache  — Docker skips this RUN entirely when pom.xml is unchanged
#   Mount cache  — persists ~/.m2 on the build host across runs, so even when
#                  pom.xml changes only new/changed deps are downloaded
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    ./mvnw dependency:go-offline -q --no-transfer-progress

# Build (tests run in CI, not here)
COPY src ./src
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    ./mvnw package -DskipTests -q --no-transfer-progress && \
    cp target/rate-limiting-service-*.war target/app.war

# ============================================================
# Stage 2: Runtime — built from pre-compiled WAR (used by CD)
# ============================================================
FROM eclipse-temurin:21-jre-alpine AS runtime-prebuilt

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY target/*.war app.war
RUN chown appuser:appgroup app.war

USER appuser

ARG VERSION=unknown
ARG GIT_COMMIT=unknown
LABEL org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.revision="${GIT_COMMIT}" \
      org.opencontainers.image.title="rate-limiting-service"

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- "http://localhost:${SERVER_PORT:-8081}/actuator/health" || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.war"]

# ============================================================
# Stage 3: Runtime — built from source (local dev / fallback)
# ============================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Non-root user — never run as root in production
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /build/target/app.war app.war
RUN chown appuser:appgroup app.war

USER appuser

# Build metadata (injected by build.yml)
ARG VERSION=unknown
ARG GIT_COMMIT=unknown
LABEL org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.revision="${GIT_COMMIT}" \
      org.opencontainers.image.title="rate-limiting-service"

# Default port — overridable via SERVER_PORT env var at runtime
EXPOSE 8081

# Liveness check via Actuator /health (start-period gives Spring time to boot)
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- "http://localhost:${SERVER_PORT:-8081}/actuator/health" || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.war"]
