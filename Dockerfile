# Stage 1: Build
# Purpose: Compile source code and package application into executable JAR
# Uses full JDK + Maven for building, but artifacts aren't included in final image
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml first to leverage Docker layer caching
# Dependencies are re-downloaded only when pom.xml changes
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build application
# Tests are skipped here as they run separately in CI/CD pipeline
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run
# Purpose: Minimal runtime image with JRE only (no build tools)
# Alpine Linux base provides security and small footprint (~200MB total)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy only the built JAR from build stage
# Multi-stage pattern excludes Maven, JDK, and source code from final image
COPY --from=build /app/target/*.jar app.jar

# Expose application port
EXPOSE 8080

# Run the application with container-optimized JVM flags
# -XX:+UseContainerSupport: Enables container-aware memory detection
# -XX:MaxRAMPercentage=75.0: Limits JVM heap to 75% of container memory to prevent OOM kills
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
