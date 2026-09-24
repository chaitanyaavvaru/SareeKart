# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Cache dependencies
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/sareekart-0.0.1-SNAPSHOT.jar sareekart.jar
ENV JAVA_TOOL_OPTIONS="-Xmx384m -Xms128m -XX:+UseSerialGC -XX:TieredStopAtLevel=1"
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "sareekart.jar"]
