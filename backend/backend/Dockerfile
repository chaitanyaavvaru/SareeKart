# Build stage — JDK matches the pom's <java.version>17</java.version>
# (jammy variant: alpine tags lack arm64 manifests)
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /app

# Dependency caching layer
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B -q

# Source + build
COPY src ./src
RUN ./mvnw clean package -DskipTests -B -q

# Runtime stage — JRE only, non-root, health-checkable
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/* \
    && addgroup --gid 1001 appgroup \
    && adduser --uid 1001 --ingroup appgroup --disabled-password appuser

COPY --from=builder /app/target/*.jar app.jar

RUN mkdir -p /app/uploads/products && chown -R appuser:appgroup /app/uploads

USER appuser

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/api/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]