# ============================================================
# Stage 1: Maven Builder (Build the JAR)
# ============================================================
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy Maven files for dependency caching
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for faster builds)
RUN mvn clean package -DskipTests -B

# Verify JAR exists
RUN ls -lh target/*.jar

# ============================================================
# Stage 2: Runtime Image (Optimized for Render Free Tier)
# ============================================================
FROM eclipse-temurin:21-jre-alpine

# Metadata
LABEL maintainer="EsportTournament"
LABEL description="Esport Tournament Backend - Render Deployment"

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Copy JAR from builder
COPY --from=builder /build/target/EsportTournament-0.0.1-SNAPSHOT.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs && \
    chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Render uses port 10000 internally
EXPOSE 10000

# Health check for Render
HEALTHCHECK --interval=60s --timeout=10s --start-period=180s --retries=5 \
  CMD curl -f http://localhost:${PORT:-10000}/actuator/health || exit 1

# Memory-optimized JVM settings for Render Free Tier (512MB)
ENV JAVA_OPTS="-Xms128m \
               -Xmx256m \
               -XX:MaxMetaspaceSize=256m \
               -XX:ReservedCodeCacheSize=32m \
               -XX:+UseSerialGC \
               -XX:+TieredCompilation \
               -XX:TieredStopAtLevel=1 \
               -XX:+UseStringDeduplication \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.jmx.enabled=false \
               -Dfile.encoding=UTF-8"

# Entry point
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
