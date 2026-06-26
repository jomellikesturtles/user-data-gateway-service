# Stage 1: Build
FROM maven:3-eclipse-temurin-26 AS builder
WORKDIR /app
COPY pom.xml .
# Cache dependencies
RUN mvn dependency:go-offline -B
COPY src ./src

RUN echo "Dependencies installation done."

RUN echo "Running clean package"
# Build application jar (skip tests as per verify check standards)
RUN mvn clean package -DskipTests -Pcli

RUN echo "Proceeding to runtime..."
# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Run as non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

RUN echo "Copying .jar"
COPY --from=builder /app/target/user-data-gateway-service-*.jar app.jar

# Expose HTTP port (8081) and gRPC port (6005)
EXPOSE 8081 6005

ENTRYPOINT ["java", "-jar", "app.jar"]
