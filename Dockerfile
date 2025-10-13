# ===============================================
# DOCKERFILE FINAL - POKEMON CARD PLANNING BACKEND
# Version qui évite complètement l'exécution des tests
# ===============================================

FROM maven:3.9.5-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy pom.xml first (for dependency caching)
COPY pom.xml ./

# Download dependencies offline
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build WITHOUT running any tests or test compilation
RUN mvn clean package -DskipTests -Dmaven.test.skip=true -Dmaven.compile.skip.tests=true -B

# ========== PRODUCTION STAGE ==========
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -g 1001 -S spring && \
    adduser -u 1001 -S spring -G spring && \
    apk add --no-cache wget

# Copy the jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership to spring user
RUN chown spring:spring app.jar

# Switch to non-root user
USER spring

# Health check endpoint
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# Environment variables
ENV SPRING_PROFILES_ACTIVE=docker

# Startup command with logging
ENTRYPOINT ["sh", "-c", "echo '🚀 Starting Pokemon Card Planning Backend...' && exec java $JAVA_OPTS -jar app.jar"]