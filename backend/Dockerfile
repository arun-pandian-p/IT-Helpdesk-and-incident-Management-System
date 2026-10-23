# Multi-stage Dockerfile for Spring Boot + SQLite backend
# Stage 1: Build Java application with Maven
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app
COPY pom.xml .
# Pre-fetch dependencies
RUN mvn dependency:go-offline -B

COPY src ./src
# Build the application jar, skipping tests during container image build
RUN mvn clean package -DskipTests

# Stage 2: Minimal Java runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Create data directory for SQLite persistent database
RUN mkdir -p /data

COPY --from=builder /app/target/service-desk-backend-1.0.0.jar app.jar

ENV PORT=8000
ENV DB_PATH=/data/helpdesk.db
ENV CORS_ORIGINS=http://localhost:5173,http://localhost:80,http://localhost:3000

EXPOSE 8000

HEALTHCHECK --interval=10s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8000/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
