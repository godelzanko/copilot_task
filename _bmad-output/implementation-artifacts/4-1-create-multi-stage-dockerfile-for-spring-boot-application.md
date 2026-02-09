# Story 4.1: Create Multi-Stage Dockerfile for Spring Boot Application

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a deployment engineer,
I want a minimal Docker image for the Spring Boot application,
so that deployments are fast and images are secure.

## Acceptance Criteria

1. **Build Stage**
   - [ ] Base image: `FROM maven:3.9-eclipse-temurin-21 AS build`
   - [ ] Working directory: `WORKDIR /app`
   - [ ] Copy dependency file: `COPY pom.xml .`
   - [ ] Download dependencies: `RUN mvn dependency:go-offline -B`
   - [ ] Copy source code: `COPY src ./src`
   - [ ] Build JAR: `RUN mvn clean package -DskipTests`
   - [ ] Skip tests in Docker build (run separately in CI/CD)

2. **Runtime Stage**
   - [ ] Base image: `FROM eclipse-temurin:21-jre-alpine`
   - [ ] Working directory: `WORKDIR /app`
   - [ ] Copy JAR from build stage: `COPY --from=build /app/target/*.jar app.jar`
   - [ ] Expose port: `EXPOSE 8080`
   - [ ] Entrypoint: `ENTRYPOINT ["java", "-jar", "app.jar"]`

3. **Image Optimization**
   - [ ] Build stage not included in final image (multi-stage pattern)
   - [ ] Final image size: ~200MB (JRE + JAR only)
   - [ ] No Maven, no source code in final image
   - [ ] Alpine Linux base (lightweight, secure)

4. **Dependency Caching**
   - [ ] Separate COPY for `pom.xml` enables layer caching
   - [ ] Dependencies re-downloaded only when `pom.xml` changes
   - [ ] Speeds up iterative builds

5. **JVM Configuration (Optional)**
   - [ ] Consider adding JVM flags:
     ```dockerfile
     ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
     ```
   - [ ] Container-aware JVM flags optimize memory usage

## Tasks / Subtasks

- [ ] Task 1: Update Dockerfile to Java 21 (AC: #1, #2)
  - [ ] Subtask 1.1: Change build stage base image from `maven:3.9-eclipse-temurin-17` to `maven:3.9-eclipse-temurin-21`
  - [ ] Subtask 1.2: Change runtime stage base image from `eclipse-temurin:17-jre-alpine` to `eclipse-temurin:21-jre-alpine`
  - [ ] Subtask 1.3: Verify all other Dockerfile instructions remain correct (they already follow best practices)
  - [ ] Subtask 1.4: Add comments to each stage explaining purpose and optimization strategy

- [ ] Task 2: Add container-optimized JVM flags (AC: #5)
  - [ ] Subtask 2.1: Update ENTRYPOINT to include `-XX:+UseContainerSupport` flag
  - [ ] Subtask 2.2: Add `-XX:MaxRAMPercentage=75.0` to prevent container OOM kills
  - [ ] Subtask 2.3: Add comment explaining JVM flags and their purpose

- [ ] Task 3: Create .dockerignore file (Best Practice)
  - [ ] Subtask 3.1: Create `.dockerignore` file in project root
  - [ ] Subtask 3.2: Add entries: `target/`, `.git/`, `.idea/`, `*.md`, `.gitignore`, `README.md`
  - [ ] Subtask 3.3: Add comment explaining purpose (reduce build context size)

- [ ] Task 4: Verify build and image size (AC: #3, #4)
  - [ ] Subtask 4.1: Build image: `docker build -t url-shortener .`
  - [ ] Subtask 4.2: Check image size with `docker images url-shortener`
  - [ ] Subtask 4.3: Verify final image is <250MB
  - [ ] Subtask 4.4: Run container to verify application starts: `docker run -p 8080:8080 url-shortener`

## Dev Notes

### 🔥 CRITICAL CONTEXT - Read This First!

**Current State:** A Dockerfile already exists at project root with multi-stage build using Java 17. This story upgrades it to Java 21 per architecture specification while maintaining all existing optimizations.

**DO NOT reinvent the wheel!** The existing Dockerfile already implements:
- ✅ Multi-stage build pattern (build + runtime stages)
- ✅ Dependency caching with separate `pom.xml` copy
- ✅ Alpine-based JRE for minimal footprint
- ✅ Correct Maven offline mode (`-B` flag)
- ✅ Test skipping in Docker build
- ✅ Proper layer ordering for cache optimization

**Your job:** Update Java versions (17→21) and add JVM optimization flags. That's it!

### Architecture Requirements

**From:** `_bmad-output/planning-artifacts/architecture.md`

**Java Version Requirements:**
- Build Stage: `maven:3.9-eclipse-temurin-21` (Temurin = AdoptOpenJDK successor, Eclipse Foundation maintained)
- Runtime Stage: `eclipse-temurin:21-jre-alpine` (JRE only, not full JDK)
- **Critical:** Project uses Java 21 features - do not use older versions

**Multi-Stage Build Philosophy:**
- **Purpose:** Separate build-time dependencies from runtime dependencies
- **Benefit:** Final image contains ONLY JRE + JAR (~200MB vs ~600MB with Maven/JDK)
- **Security:** No source code, no build tools in production image

**Image Size Target:** <250MB (typically achieves ~200MB with Alpine JRE + Spring Boot JAR)

### Project Structure & Patterns

**Current Docker Stack (from docker-compose.yml):**
```
postgres:16-alpine (port 5432)
  ↓ healthcheck: service_healthy
liquibase:4.25-alpine (migrations, exits after completion)
  ↓ condition: service_completed_successfully
app (builds from ./Dockerfile, exposed on port 3000→8080)
```

**Service Coordination:**
1. PostgreSQL starts first, health check verifies readiness
2. Liquibase waits for healthy postgres, runs migrations, exits
3. App waits for liquibase success, starts Spring Boot application

**Environment Variables (from docker-compose.yml):**
- `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/urlshortener`
- `SPRING_LIQUIBASE_ENABLED=false` (migrations handled by separate service)
- `SERVER_PORT=8080` (internal port, mapped to 3000 externally)

### Maven Build Configuration

**From:** `pom.xml` analysis

**Project Details:**
- Spring Boot version: 3.2.2
- Java version: 17 (will upgrade to 21 - verify pom.xml uses Java 21)
- Build plugin: spring-boot-maven-plugin (creates executable JAR)
- Artifact: `url-shortener-0.0.1-SNAPSHOT.jar`

**Key Dependencies:**
- Spring Web (REST API endpoints)
- Spring Data JPA (database access)
- PostgreSQL JDBC driver
- Liquibase Core 4.25+ (database migrations)
- Spring Boot Actuator (health checks)
- Testcontainers (integration tests - not needed in Docker build)

**Build Command in Dockerfile:**
- `mvn clean package -DskipTests` (skips tests for faster builds)
- Tests run separately in CI/CD pipeline
- Offline mode (`-B`) ensures reproducible builds without interactive prompts

### JVM Optimization Flags Explained

**Why add these flags?**
- Default JVM doesn't know it's running in a container
- Can allocate too much memory and trigger OOM kills
- Container-aware flags prevent resource conflicts

**Recommended Flags:**
```dockerfile
-XX:+UseContainerSupport     # Enables container-aware memory detection
-XX:MaxRAMPercentage=75.0    # Use 75% of container memory limit (safe default)
```

**Alternative flags (if needed for debugging):**
```dockerfile
-XX:InitialRAMPercentage=50.0   # Initial heap size as % of container limit
-XX:MinRAMPercentage=50.0       # Minimum heap size for small containers
```

### Previous Story Learnings

**From Story 3.3 (JPA Entity Implementation):**
- Project follows standard Spring Boot package structure: `com.example.urlshortener.*`
- Code lives in `src/main/java/com/example/urlshortener/`
- Resources in `src/main/resources/`
- Tests in `src/test/java/`
- Liquibase changelogs: `src/main/resources/db/changelog/`

**From Recent Git Commits:**
- Last 5 commits show steady progress through Epic 3 (Database & Persistence)
- Pattern: Each story = 1 commit with clear message (e.g., "feat: Implement JPA Entity and Repository...")
- Configuration uses `application.yml` (not .properties)
- Health checks configured via Spring Boot Actuator

### Testing Requirements

**Manual Verification Steps:**
1. **Build Image:**
   ```bash
   docker build -t url-shortener .
   ```
   - Should complete successfully
   - Look for "Successfully tagged url-shortener:latest"

2. **Verify Image Size:**
   ```bash
   docker images url-shortener
   ```
   - Expected: ~200-250MB total
   - Compare to old image (if cached) to verify optimization

3. **Run Container (Standalone Test):**
   ```bash
   docker run --rm -p 8080:8080 url-shortener
   ```
   - Should start Spring Boot application
   - Look for "Started UrlShortenerApplication in X seconds"
   - **NOTE:** Will fail to connect to database (expected - database is separate service)
   - Verify application attempts to start, not build errors

4. **Full Stack Test (via docker-compose):**
   ```bash
   docker-compose up --build
   ```
   - Builds new image with updated Dockerfile
   - Starts all 3 services (postgres, liquibase, app)
   - App should successfully connect to database
   - Test endpoint: `curl http://localhost:3000/actuator/health`
   - Expected response: `{"status":"UP"}`

### Definition of Done Checklist

- [ ] Dockerfile updated to Java 21 (both build and runtime stages)
- [ ] JVM optimization flags added to ENTRYPOINT
- [ ] Comments added explaining each stage and optimization
- [ ] .dockerignore file created (optional but recommended)
- [ ] Image builds successfully without errors
- [ ] Final image size verified (<250MB)
- [ ] Container starts and application initializes
- [ ] Full stack test passes (docker-compose up --build)
- [ ] Health check endpoint responds successfully

### References

- [Source: _bmad-output/planning-artifacts/epics.md - Epic 4, Story 4.1]
- [Source: _bmad-output/planning-artifacts/architecture.md - Deployment & Infrastructure section]
- [Source: docker-compose.yml - Service orchestration and dependencies]
- [Source: pom.xml - Maven build configuration and dependencies]
- [Source: Dockerfile - Existing multi-stage build implementation]

### ⚠️ Important Notes

1. **Java Version Mismatch:** Current project uses Java 17, but Epic 4.1 spec requires Java 21. Verify with user if pom.xml should also be updated to Java 21, or if this is intentional.

2. **Existing Dockerfile:** The project already has a working Dockerfile. This story is an UPGRADE, not a greenfield implementation.

3. **No Breaking Changes:** The updated Dockerfile must remain compatible with docker-compose.yml service dependencies.

4. **Test Strategy:** Tests are skipped in Docker build (`-DskipTests`) because they run separately in CI/CD. This is correct and should not be changed.

5. **.dockerignore Best Practice:** While not in acceptance criteria, creating `.dockerignore` reduces build context size and improves build speed (excludes `target/`, `.git/`, etc.).

## Dev Agent Record

### Agent Model Used

[To be filled by dev agent]

### Debug Log References

[To be filled by dev agent]

### Completion Notes List

[To be filled by dev agent]

### File List

[To be filled by dev agent]
