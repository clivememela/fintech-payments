# Multi-stage build for Transfer Service
# Note: This Dockerfile should be built from the project root directory with:
# docker build -f fintech-payments-transfer-service/Dockerfile -t transfer-service .

FROM gradle:8.14.3-jdk21-alpine AS builder

# Set working directory
WORKDIR /app

# Copy root project files
COPY build.gradle settings.gradle gradle.properties ./
COPY gradle gradle

# Copy all submodules (needed for multi-module build)
COPY fintech-payments-transfer-service fintech-payments-transfer-service/
COPY fintech-payments-ledger-service fintech-payments-ledger-service/

# Build the specific module
RUN gradle :fintech-payments-transfer-service:clean :fintech-payments-transfer-service:build -x test --no-daemon

# Production stage
FROM eclipse-temurin:21-jre-alpine

# Install curl for health checks (Alpine uses apk, not apt-get)
RUN apk add --no-cache curl

# Create app user (Alpine uses addgroup and adduser)
RUN addgroup -S appuser && adduser -S appuser -G appuser

# Set working directory
WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/fintech-payments-transfer-service/build/libs/*.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs && chown -R appuser:appuser /app
USER appuser

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# JVM optimization for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Run the application
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]