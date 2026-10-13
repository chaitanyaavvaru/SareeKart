# ============================================================================
# SareeKart Backend — Dockerfile
# Java 17 / Spring Boot 3 · Multi-stage build
# Step 3.2: non-root user, improved dependency caching, native HEALTHCHECK
# ============================================================================

# ── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy POM first so the dependency layer is only invalidated when pom.xml changes.
# Using dependency:resolve + dependency:resolve-plugins instead of go-offline:
#   - go-offline misses annotation processors and Maven plugin dependencies
#   - resolve + resolve-plugins downloads both compile/runtime and plugin classpaths
COPY pom.xml .
RUN mvn dependency:resolve dependency:resolve-plugins -B -q

# Copy source and build the application JAR (tests are run separately in CI).
COPY src ./src
RUN mvn package -DskipTests -B -q

# ── Stage 2: Run ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy AS run
WORKDIR /app

# Create a dedicated non-root system user and group.
# UID/GID 1001 is chosen to avoid collisions with common system accounts (1000=ubuntu).
# --no-create-home: no home directory needed for a service account.
# --shell /bin/false: no interactive login permitted.
RUN groupadd --system --gid 1001 appgroup \
 && useradd  --system --uid 1001 --gid appgroup --no-create-home --shell /bin/false appuser

# Copy the built JAR from the build stage.
COPY --from=build /app/target/sareekart-0.0.1-SNAPSHOT.jar sareekart.jar

# Memory and JVM optimisation for container-constrained environments.
ENV JAVA_TOOL_OPTIONS="-Xmx384m -Xms128m -XX:+UseSerialGC -XX:TieredStopAtLevel=1"

# Drop privileges before starting the process.
USER appuser

EXPOSE 8081

# Native HEALTHCHECK — polled by Docker daemon and compatible with Swarm/Compose.
# curl is available in eclipse-temurin:17-jre-jammy (Ubuntu Jammy base).
HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "sareekart.jar"]
