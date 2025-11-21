# ============================================================
# Stage 1: Maven Builder
# ============================================================
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy pom.xml first for better layer caching
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for faster builds)
RUN mvn clean package -DskipTests -B

# Verify JAR was created
RUN ls -la target/*.jar

# ============================================================
# Stage 2: Runtime Image
# ============================================================
FROM eclipse-temurin:21-jre-alpine

# Add metadata
LABEL maintainer="EsportTournament"
LABEL description="Grand Battle Arena Backend - Spring Boot Application"

# Install wget for healthcheck (Alpine doesn't include it by default)
RUN apk add --no-cache wget

# Create app user for security (non-root)
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /build/target/EsportTournament-0.0.1-SNAPSHOT.jar app.jar

# Change ownership to spring user
RUN mkdir -p /app/logs && \
    chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose port (Railway will set PORT env var)
EXPOSE 8080

# Health check (using wget, install if needed, or use curl)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM optimizations for containerized environments
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:+UseStringDeduplication \
               -Djava.security.egd=file:/dev/./urandom"

# Entry point
ENTRYPOINT ["sh", "-c", "mkdir -p /app/logs && java $JAVA_OPTS -jar app.jar"]

