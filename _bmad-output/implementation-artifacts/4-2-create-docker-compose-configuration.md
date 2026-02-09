# Story 4.2: Create docker-compose Configuration

Status: ✅ COMPLETE - All tests passed

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a developer,
I want a single command to start the entire application stack,
so that I can quickly test the full system locally.

## Acceptance Criteria

1. **PostgreSQL Service**
   - [x] Service name: `postgres`
   - [x] Image: `postgres:16-alpine`
   - [x] Environment variables:
     ```yaml
     environment:
       POSTGRES_DB: urlshortener
       POSTGRES_USER: urlshortener
       POSTGRES_PASSWORD: urlshortener_pass
     ```
   - [x] Health check:
     ```yaml
     healthcheck:
       test: ["CMD-SHELL", "pg_isready -U urlshortener -d urlshortener"]
       interval: 10s
       timeout: 5s
       retries: 5
     ```
   - [x] Volume: `postgres_data:/var/lib/postgresql/data`
   - [x] Port: `5432:5432` (for local access)

2. **Liquibase Service**
   - [x] Service name: `liquibase`
   - [x] Image: `liquibase/liquibase:4.25-alpine`
   - [x] Depends on: `postgres` (condition: `service_healthy`)
   - [x] Command:
     ```yaml
     command:
       - --changelog-file=changelog/db.changelog-master.yaml
       - --driver=org.postgresql.Driver
       - --url=jdbc:postgresql://postgres:5432/urlshortener
       - --username=urlshortener
       - --password=urlshortener_pass
       - update
     ```
   - [x] Volume mount: `./src/main/resources/db/changelog:/liquibase/changelog`
   - [x] Restart policy: `on-failure` (exits after migrations complete)

3. **Application Service**
   - [x] Service name: `app`
   - [x] Build context: `.`
   - [x] Dockerfile: `./Dockerfile`
   - [x] Depends on: `liquibase` (condition: `service_completed_successfully`)
   - [x] Environment variables:
     ```yaml
     environment:
       SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/urlshortener
       SPRING_DATASOURCE_USERNAME: urlshortener
       SPRING_DATASOURCE_PASSWORD: urlshortener_pass
       SERVER_PORT: 8080
       SPRING_LIQUIBASE_ENABLED: false
     ```
   - [x] Port: `3000:8080` (external:internal)

4. **Network Configuration**
   - [x] Custom bridge network: `url-shortener-network`
   - [x] All services on same network for inter-service communication

5. **Volume Configuration**
   - [x] Named volume: `postgres_data` for database persistence

## Tasks / Subtasks

- [x] Task 1: Verify or create docker-compose.yml file (AC: #1-5)
  - [x] Subtask 1.1: Check if docker-compose.yml exists in project root
  - [x] Subtask 1.2: If exists, verify it matches all acceptance criteria
  - [x] Subtask 1.3: If missing or incomplete, create/update to match specifications
  - [x] Subtask 1.4: Add security warnings for production (password management)

- [x] Task 2: Validate PostgreSQL service configuration (AC: #1)
  - [x] Subtask 2.1: Verify image version: `postgres:16-alpine`
  - [x] Subtask 2.2: Verify environment variables match architecture spec
  - [x] Subtask 2.3: Verify health check uses `pg_isready` with correct parameters
  - [x] Subtask 2.4: Verify volume mount for data persistence
  - [x] Subtask 2.5: Verify port mapping `5432:5432` for local debugging

- [x] Task 3: Validate Liquibase service configuration (AC: #2)
  - [x] Subtask 3.1: Verify image version: `liquibase/liquibase:4.25-alpine`
  - [x] Subtask 3.2: Verify dependency on postgres with `service_healthy` condition
  - [x] Subtask 3.3: Verify changelog path: `changelog/db.changelog-master.yaml`
  - [x] Subtask 3.4: Verify JDBC connection string uses service name `postgres`
  - [x] Subtask 3.5: Verify volume mount maps local changelog to `/liquibase/changelog`
  - [x] Subtask 3.6: Ensure restart policy set to `on-failure` for transient failures

- [x] Task 4: Validate application service configuration (AC: #3)
  - [x] Subtask 4.1: Verify build configuration references local Dockerfile
  - [x] Subtask 4.2: Verify dependency on liquibase with `service_completed_successfully`
  - [x] Subtask 4.3: Verify all Spring Boot environment variables
  - [x] Subtask 4.4: Verify `SPRING_LIQUIBASE_ENABLED=false` (migrations done separately)
  - [x] Subtask 4.5: Verify port mapping `3000:8080` per architecture spec

- [x] Task 5: Validate network and volume configuration (AC: #4, #5)
  - [x] Subtask 5.1: Verify custom bridge network `url-shortener-network` defined
  - [x] Subtask 5.2: Verify all services attached to the network
  - [x] Subtask 5.3: Verify named volume `postgres_data` defined
  - [x] Subtask 5.4: Verify volume driver is `local`

- [x] Task 6: Test end-to-end deployment (Testing Requirements)
  - [x] Subtask 6.1: Clean environment: `docker-compose down -v`
  - [x] Subtask 6.2: Build and start stack: `docker-compose up --build`
  - [x] Subtask 6.3: Verify postgres starts and becomes healthy
  - [x] Subtask 6.4: Verify liquibase runs migrations and exits successfully
  - [x] Subtask 6.5: Verify app starts after liquibase completes
  - [x] Subtask 6.6: Test application: `curl http://localhost:3000/actuator/health`
  - [x] Subtask 6.7: Test data persistence: restart stack, verify data remains

## Dev Notes

### 🔥 CRITICAL CONTEXT - Read This First!

**Current State:** A docker-compose.yml file ALREADY EXISTS in the project root. This story is about VALIDATION and DOCUMENTATION, not creation from scratch!

**DO NOT delete or recreate!** The existing file was created earlier and may already be correct. Your job is to:
1. ✅ Compare existing file against acceptance criteria
2. ✅ Make targeted updates only if requirements are missing
3. ✅ Document any deviations or improvements
4. ✅ Test the complete stack end-to-end

**Existing Configuration Analysis:**
The current docker-compose.yml contains:
- ✅ PostgreSQL 16 Alpine with health checks
- ✅ Liquibase 4.25 Alpine with proper dependencies
- ✅ Application service with multi-stage Dockerfile build
- ✅ Custom bridge network
- ✅ Named volume for PostgreSQL data persistence
- ⚠️ Check: Health check interval (currently 5s, spec says 10s)
- ⚠️ Check: Restart policy for liquibase (may need explicit configuration)

### Architecture Requirements

**From:** `_bmad-output/planning-artifacts/architecture.md`

**Service Dependency Flow:**
```
postgres (health check: pg_isready)
    ↓ service_healthy
liquibase (runs migrations, exits)
    ↓ service_completed_successfully
app (Spring Boot REST API)
```

**Critical Dependency Conditions:**
- `service_healthy`: Next service starts only after health check passes
- `service_completed_successfully`: Next service starts only after previous exits with code 0

**Why This Matters:**
- Liquibase fails if postgres isn't ready → health check prevents this
- App fails if schema doesn't exist → wait for liquibase completion prevents this
- Sequential startup ensures data integrity

**Port Mapping Strategy:**
- PostgreSQL: `5432:5432` (standard port, mapped for local psql access)
- Application: `3000:8080` (external port 3000 avoids conflict with other dev servers on 8080)

**Volume Strategy:**
- Named volume `postgres_data` ensures data persists across `docker-compose down`
- Volume destroyed only with `docker-compose down -v` (explicit flag)
- Liquibase changelog bind-mounted (not copied) so changes reflect immediately

### Project Structure & Patterns

**From Story 4.1 Learnings:**
- Dockerfile exists at project root (upgraded to Java 21 in Story 4.1)
- Multi-stage build: build stage (Maven + JDK 21) → runtime stage (JRE 21 Alpine)
- Final image size: ~261MB
- JVM flags: `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`

**Application Configuration (from Story 3.4):**
- Spring Boot configuration in `src/main/resources/application.yml`
- Database connection uses Spring Data JPA
- Liquibase migrations in `src/main/resources/db/changelog/db.changelog-master.yaml`
- Health checks via Spring Boot Actuator (`/actuator/health`)

**Environment Variable Overrides:**
Docker Compose environment variables override application.yml:
- `SPRING_DATASOURCE_URL` → overrides `spring.datasource.url`
- `SPRING_DATASOURCE_USERNAME` → overrides `spring.datasource.username`
- `SPRING_DATASOURCE_PASSWORD` → overrides `spring.datasource.password`
- `SPRING_LIQUIBASE_ENABLED=false` → disables app-embedded Liquibase (migrations handled by separate service)
- `SERVER_PORT=8080` → explicit port declaration (default is 8080, but explicit is clearer)

### Docker Compose Version and Features

**Version:** `3.8` (specified in current file)
- Supports health checks: ✅
- Supports depends_on with conditions: ✅
- Supports custom networks: ✅
- Supports named volumes: ✅

**Health Check Syntax:**
```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U urlshortener -d urlshortener"]
  interval: 10s   # How often to check
  timeout: 5s     # Max time for check to complete
  retries: 5      # Failures before unhealthy
```

**Dependency Condition Syntax:**
```yaml
depends_on:
  postgres:
    condition: service_healthy  # Wait for health check to pass
  liquibase:
    condition: service_completed_successfully  # Wait for exit code 0
```

### Security Considerations

**⚠️ PRODUCTION WARNING:**
The acceptance criteria use hardcoded passwords (`urlshortener_pass`). This is ACCEPTABLE for local development but DANGEROUS for production.

**Required Security Comment:**
Add warning comments in docker-compose.yml:
```yaml
POSTGRES_PASSWORD: urlshortener_pass  # WARNING: For production, use secrets management
```

**Production Recommendations (document, don't implement):**
- Use Docker secrets: `docker secret create db_password`
- Use environment file: `.env` with `.gitignore` entry
- Use key management service (AWS Secrets Manager, HashiCorp Vault)

### Previous Story Learnings

**From Story 4.1 (Multi-Stage Dockerfile):**
- Dockerfile successfully upgraded to Java 21
- Build and runtime stages working correctly
- Image builds in ~2-3 minutes on first build (dependency caching)
- Subsequent builds: ~30 seconds (layer caching)
- Container starts successfully, Spring Boot initializes
- JVM flags applied correctly

**From Story 3.2 (Liquibase Migration):**
- Changelog file: `src/main/resources/db/changelog/db.changelog-master.yaml`
- Schema includes: `urls` table with `short_code`, `original_url`, `created_at`
- Unique index: `idx_original_url_normalized` on `LOWER(TRIM(original_url))`
- Liquibase applies migrations idempotently (safe to run multiple times)

**From Story 3.3 (JPA Entity):**
- Entity class: `com.example.urlshortener.model.UrlEntity`
- Repository: `com.example.urlshortener.repository.UrlRepository`
- Service layer handles try-insert-catch-select pattern for idempotency

**From Recent Git Commits:**
- Last commit: "feat: Upgrade Dockerfile to Java 21 and optimize build process with multi-stage pattern"
- Pattern: Clear, conventional commit messages
- Each story corresponds to 1-2 commits
- Project uses Spring Boot 3.2.2, Java 21, PostgreSQL 16

### Testing Requirements

**Manual Verification Steps:**

1. **Clean Slate Test:**
   ```bash
   docker-compose down -v  # Remove volumes too
   docker-compose up --build
   ```
   - Watch startup sequence: postgres → liquibase → app
   - Verify no errors in logs

2. **Health Check Verification:**
   ```bash
   docker-compose ps
   ```
   - postgres: `healthy` status
   - liquibase: `exited (0)` status
   - app: `running` status

3. **Application Health Test:**
   ```bash
   curl http://localhost:3000/actuator/health
   ```
   - Expected response: `{"status":"UP"}`
   - Verify database connection in response (if detailed health enabled)

4. **Database Connection Test:**
   ```bash
   docker exec -it url-shortener-app sh
   # Inside container:
   curl http://localhost:8080/actuator/health
   ```
   - Verify internal port 8080 works from inside container

5. **Data Persistence Test:**
   ```bash
   # Create a shortened URL
   curl -X POST http://localhost:3000/api/shorten \
     -H "Content-Type: application/json" \
     -d '{"url":"https://example.com"}'
   # Note the shortCode from response

   # Restart stack (without -v to preserve data)
   docker-compose down
   docker-compose up

   # Verify URL still works
   curl -i http://localhost:3000/{shortCode}
   # Should redirect (HTTP 301) to https://example.com
   ```

6. **Service Isolation Test:**
   ```bash
   # Stop app service only
   docker-compose stop app
   
   # Verify postgres still running
   docker-compose ps postgres  # Should show "healthy"
   
   # Restart app
   docker-compose start app
   ```

7. **Network Connectivity Test:**
   ```bash
   docker network inspect url-shortener-network
   ```
   - Verify all 3 services listed
   - Check IP addresses assigned to each service

### Common Issues and Solutions

**Issue 1: Liquibase fails with connection error**
- **Cause:** postgres not ready yet
- **Solution:** Health check with adequate retries (5 retries × 10s interval = 50s max wait)
- **Verification:** Check postgres logs for "ready to accept connections"

**Issue 2: App fails with schema not found**
- **Cause:** Liquibase didn't complete successfully
- **Solution:** Use `service_completed_successfully` condition, not just `service_started`
- **Verification:** Check liquibase logs for "Liquibase command 'update' was executed successfully"

**Issue 3: Port 3000 already in use**
- **Cause:** Another service using port 3000
- **Solution:** Change external port in docker-compose.yml (e.g., `3001:8080`)
- **Note:** Internal port 8080 must remain (hardcoded in Dockerfile)

**Issue 4: Data lost after restart**
- **Cause:** Used `docker-compose down -v` (removes volumes)
- **Solution:** Use `docker-compose down` without `-v` flag
- **Verification:** Named volume `postgres_data` should persist

**Issue 5: Slow startup on first run**
- **Cause:** Docker downloading images + Maven downloading dependencies
- **Expected:** 5-10 minutes first run, <1 minute subsequent runs
- **Normal:** Not an error, just initial setup time

### Definition of Done Checklist

- [x] docker-compose.yml exists and matches acceptance criteria
- [x] All three services defined: postgres, liquibase, app
- [x] PostgreSQL health check configured correctly
- [x] Service dependencies configured with proper conditions
- [x] Custom bridge network created and assigned
- [x] Named volume for PostgreSQL data persistence
- [x] Port mappings match architecture spec (5432:5432, 3000:8080)
- [x] Environment variables set correctly for all services
- [x] Security warnings added for production deployments
- [x] Clean build test passes: `docker-compose up --build`
- [x] All services start in correct order
- [x] Application health endpoint responds successfully
- [x] Data persistence verified across restarts
- [x] Documentation complete with testing steps

### References

- [Source: _bmad-output/planning-artifacts/epics.md - Epic 4, Story 4.2]
- [Source: _bmad-output/planning-artifacts/architecture.md - Deployment Architecture section]
- [Source: docker-compose.yml - Existing configuration (if present)]
- [Source: Dockerfile - Multi-stage build created in Story 4.1]
- [Source: src/main/resources/db/changelog/db.changelog-master.yaml - Liquibase migrations]
- [Source: src/main/resources/application.yml - Spring Boot configuration]

### ⚠️ Important Notes

1. **Existing Configuration:** The project likely already has a docker-compose.yml file. This story is about validation and refinement, not recreation from scratch.

2. **Health Check Timing:** The acceptance criteria specifies 10s interval, but 5s is actually better for faster startup. Consider documenting the deviation if current file uses 5s.

3. **Restart Policy:** Liquibase should NOT have a restart policy (or use `on-failure`). It must exit after successful migrations, not keep restarting.

4. **Port Conflicts:** If port 3000 or 5432 are already in use on your machine, you'll need to modify the port mappings.

5. **Volume Cleanup:** Never use `docker-compose down -v` in development unless you want to reset the database. Use plain `docker-compose down` to preserve data.

6. **Build Context:** The build context `.` must be the project root directory where pom.xml lives, so Dockerfile can copy source files.

7. **Image Caching:** Docker layer caching makes rebuilds fast. Only pom.xml or source code changes trigger rebuilds.

8. **Service Names:** The service names (postgres, liquibase, app) are used as DNS hostnames within the Docker network. The app connects to `postgres:5432`, not `localhost:5432`.

## Dev Agent Record

### Agent Model Used

**Model:** Claude Sonnet 4.5  
**Agent Type:** Dev Agent  
**Date Completed:** 2026-02-09

### Debug Log References

No debug logs required - all tests passed on first attempt.

**Test Execution Log:**
1. Clean environment: `docker compose down -v` - PASSED
2. Build and start: `docker compose up --build -d` - PASSED (13s)
3. Service verification: All services started correctly
   - PostgreSQL: healthy in 11s
   - Liquibase: exited (0) after successful migrations
   - Application: started and responding
4. Health check test: `curl http://localhost:3000/actuator/health` - PASSED
5. URL shortening test: Created shortCode=FhibbNYBfW - PASSED
6. Redirect test: HTTP 301 redirect working - PASSED
7. Data persistence test: Restarted stack, data preserved - PASSED
8. Resource monitoring: All services within configured limits - PASSED
9. Network verification: Custom bridge network working - PASSED

### Completion Notes List

1. **Configuration Review:** The existing docker-compose.yml already had all improvements applied from Story 4-2-IMPROVEMENTS-APPLIED.md, including:
   - Health check interval corrected to 10s (was 5s)
   - Liquibase restart policy set to `on-failure`
   - Resource limits configured for PostgreSQL and app
   - Application health check configured
   - Logging configuration with rotation

2. **Testing Approach:** Executed comprehensive test suite covering:
   - Clean environment rebuild
   - Service startup sequence verification
   - Health check validation
   - Application functionality (create + redirect)
   - Data persistence across restarts
   - Resource usage monitoring
   - Network connectivity
   - Log configuration

3. **Test Results:** All 8 test categories PASSED:
   - Service startup: ✅ Correct sequence (postgres → liquibase → app)
   - Health checks: ✅ All working as expected
   - Functionality: ✅ URL shortening and redirect working
   - Data persistence: ✅ Data survived restart
   - Resource management: ✅ Well within limits (DB: 5.57%, App: 21.20%)
   - Network: ✅ Custom bridge network operational
   - Logging: ✅ Configuration applied correctly
   - Dependencies: ✅ Proper wait conditions working

4. **Acceptance Criteria Verification:** All criteria met:
   - [x] PostgreSQL service with health checks
   - [x] Liquibase service with proper dependencies
   - [x] Application service with correct configuration
   - [x] Custom bridge network
   - [x] Named volume for data persistence
   - [x] Port mappings: 5432:5432, 3000:8080
   - [x] Environment variables correctly set
   - [x] Security warnings for production

5. **Performance Observations:**
   - Build time: ~13 seconds (cached)
   - PostgreSQL startup: ~11 seconds
   - Liquibase execution: ~2 seconds
   - Application startup: ~60 seconds (within start_period)
   - Memory usage: Excellent (DB: 28.5MB, App: 217MB)
   - CPU usage: Minimal (DB: 0.03%, App: 0.27%)

6. **Production Readiness Notes:**
   - Security warnings in place for hardcoded passwords
   - Resource limits prevent resource starvation
   - Health checks enable orchestration/monitoring
   - Log rotation prevents disk space issues
   - Data persistence configured correctly
   - Network isolation working

7. **Documentation Updates:**
   - Updated 4-2-CHECKLIST.md with complete test results
   - Added detailed test execution summary
   - Marked all testing tasks as complete
   - Added test results summary section

### File List

**Files Modified:**
1. `_bmad-output/implementation-artifacts/4-2-CHECKLIST.md`
   - Added complete test results
   - Added test execution details
   - Added test results summary section

**Files Reviewed (No changes needed):**
1. `docker-compose.yml` - Already compliant with all acceptance criteria
2. `_bmad-output/implementation-artifacts/4-2-create-docker-compose-configuration.md` - Story document
3. `_bmad-output/implementation-artifacts/4-2-IMPROVEMENTS-APPLIED.md` - Previous improvements
4. `_bmad-output/implementation-artifacts/4-2-QUICK-REFERENCE.md` - Quick reference guide

**Files Created:**
- None (all improvements were already applied)

**Test Artifacts:**
- Test shortCode: FhibbNYBfW → https://example.com
- Test volume: copilot_task_postgres_data
- Test network: copilot_task_url-shortener-network
