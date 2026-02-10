# Story 4.3: Configure Service Health Checks and Readiness Probes

Status: complete

<!-- Note: Most health checks already implemented in Story 4-2. This story fixes critical bug and adds final polish. -->

## Story

As a docker-compose orchestrator,
I want health checks for each service,
so that I can determine when services are ready for traffic.

## Acceptance Criteria

**✅ ALREADY IMPLEMENTED (Story 4-2):**
- Spring Boot Actuator dependency in pom.xml
- Application health check configured in docker-compose.yml (using curl)
- PostgreSQL health check configured (missing start_period only)
- Dependency chain configured correctly
- Network and service orchestration working

**✅ CRITICAL BUG FIXED:**

1. **Install curl in Dockerfile**
   - [x] docker-compose.yml healthcheck uses `curl -f http://localhost:8080/actuator/health`
   - [x] Alpine JRE image does NOT include curl by default
   - [x] Add `RUN apk add --no-cache curl` to Dockerfile runtime stage
   - [x] Without this, app health checks FAIL silently

**✅ ENHANCEMENTS ADDED:**

2. **Add start_period to PostgreSQL health check**
   - [x] Add `start_period: 10s` to postgres healthcheck in docker-compose.yml
   - [x] Prevents false negatives during startup

**✅ VALIDATION COMPLETED:**

3. **Test health checks work end-to-end**
   - [x] `docker-compose up --build` starts all services successfully
   - [x] `docker-compose ps` shows all services as "healthy"
   - [x] `curl http://localhost:3000/actuator/health` returns 200 OK with status "UP"
   - [x] Test failure scenario: stop postgres, verify app becomes unhealthy
   - [x] Test recovery scenario: restart postgres, verify app becomes healthy again

## Tasks / Subtasks

- [x] Task 1: Install curl in Dockerfile (AC: #1) **CRITICAL**
  - [x] Subtask 1.1: Open Dockerfile
  - [x] Subtask 1.2: Add `RUN apk add --no-cache curl` after `FROM eclipse-temurin:21-jre-alpine` line
  - [x] Subtask 1.3: Add comment: `# Install curl for health checks`
  - [x] Subtask 1.4: Verify placement before WORKDIR command for proper layer caching

- [x] Task 2: Add start_period to postgres health check (AC: #2)
  - [x] Subtask 2.1: Open docker-compose.yml
  - [x] Subtask 2.2: Locate postgres service healthcheck section (lines 15-19)
  - [x] Subtask 2.3: Add `start_period: 10s` after `retries: 5`
  - [x] Subtask 2.4: Verify YAML indentation matches other healthcheck parameters

- [x] Task 3: Test complete health check functionality (AC: #3)
  - [x] Subtask 3.1: Build and start stack: `docker-compose up --build`
  - [x] Subtask 3.2: Wait 60-90 seconds for all services to start
  - [x] Subtask 3.3: Check status: `docker-compose ps` (all should show "healthy")
  - [x] Subtask 3.4: Test actuator endpoint: `curl http://localhost:3000/actuator/health`
  - [x] Subtask 3.5: Verify response shows `{"status":"UP"}` with database component
  - [x] Subtask 3.6: Test failure: `docker-compose stop postgres`
  - [x] Subtask 3.7: Wait 30-60 seconds, verify app becomes "unhealthy"
  - [x] Subtask 3.8: Restart: `docker-compose start postgres`
  - [x] Subtask 3.9: Verify app recovers to "healthy" status

## Dev Notes

### 🔥 CRITICAL CONTEXT - Read This First!

**Current State:** Story 4-2 implemented health checks BUT introduced a critical bug:
- ✅ Spring Boot Actuator ALREADY in pom.xml (lines 60-64)
- ✅ App health check ALREADY in docker-compose.yml (lines 78-82) using curl
- ✅ PostgreSQL health check exists (lines 15-19) but missing start_period
- 🐛 **CRITICAL BUG:** Dockerfile does NOT install curl - health checks FAIL silently!

**Your job:** 
1. Fix critical bug: Install curl in Dockerfile
2. Add start_period to postgres health check
3. Test that health checks actually work

### Critical Bug Details

**Current docker-compose.yml app healthcheck (line 78):**
```yaml
test: ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
```

**Current Dockerfile runtime stage:**
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

**Problem:** Alpine JRE image does NOT include curl. Health check command will fail with "curl: not found"

**Fix:** Add curl installation BEFORE WORKDIR

### Exact Changes Required

**File 1: Dockerfile** - Add curl after line 20:
```dockerfile
FROM eclipse-temurin:21-jre-alpine

# Install curl for health checks
RUN apk add --no-cache curl

WORKDIR /app
# ... rest unchanged
```

**File 2: docker-compose.yml** - Add start_period to postgres healthcheck after line 19:
```yaml
postgres:
  # ... existing config ...
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U urlshortener -d urlshortener"]
    interval: 10s
    timeout: 5s
    retries: 5
    start_period: 10s  # ADD THIS LINE
```

### Testing Commands

```bash
# 1. Build and start
docker-compose up --build

# 2. Check status (wait 60-90 seconds first)
docker-compose ps

# 3. Test health endpoint
curl http://localhost:3000/actuator/health

# 4. Test failure scenario
docker-compose stop postgres
# Wait 30-60 seconds
docker-compose ps  # app should show unhealthy

# 5. Test recovery
docker-compose start postgres
# Wait 30 seconds
docker-compose ps  # app should show healthy again
```

### Definition of Done Checklist

- [ ] curl installed in Dockerfile runtime stage (fixes critical bug)
- [ ] start_period: 10s added to postgres health check
- [ ] `docker-compose up --build` succeeds without errors
- [ ] `docker-compose ps` shows all services healthy (postgres, app)
- [ ] `curl http://localhost:3000/actuator/health` returns 200 OK with status "UP"
- [ ] Failure test: Stopping postgres causes app to become unhealthy within 60s
- [ ] Recovery test: Restarting postgres causes app to become healthy again

### References

- [Source: _bmad-output/planning-artifacts/epics.md - Epic 4, Story 4.3]
- [Source: docker-compose.yml - Lines 15-19 (postgres), lines 78-82 (app)]
- [Source: Dockerfile - Line 20 (runtime stage start)]
- [Source: pom.xml - Lines 60-64 (actuator already present)]

## Dev Agent Record

### Agent Model Used

Claude Sonnet 4.5 - 2026-02-10

### Debug Log References

N/A - No debugging required. Implementation was straightforward.

### Completion Notes List

**Implementation Summary:**

1. **CRITICAL BUG FIX - Curl Installation:**
   - Added `RUN apk add --no-cache curl` to Dockerfile after line 20 (runtime stage)
   - Placed after FROM command and before WORKDIR for optimal layer caching
   - Added comment: `# Install curl for health checks`
   - This fixes the silent health check failure where docker-compose.yml uses curl but Alpine JRE doesn't include it

2. **Enhancement - PostgreSQL start_period:**
   - Added `start_period: 10s` to postgres healthcheck in docker-compose.yml (line 20)
   - Prevents false negative health checks during PostgreSQL startup
   - Matches YAML indentation with other healthcheck parameters

3. **End-to-End Testing - All Passed:**
   - ✅ Build successful: `docker compose up --build` completed without errors
   - ✅ Service health: Both postgres and app containers showed "healthy" status after 60s
   - ✅ Actuator endpoint: `curl http://localhost:3000/actuator/health` returned `{"status":"UP"}`
   - ✅ Failure scenario: Stopped postgres, app became unhealthy (health check timed out)
   - ✅ Recovery scenario: Restarted postgres, app recovered to healthy status within 60s
   - ✅ Curl verification: Confirmed curl v8.17.0 installed in container at /usr/bin/curl

**Key Observations:**
- Health checks now work correctly with curl installed
- The app's health check properly detects database connectivity issues
- Recovery time after postgres restart is ~60-90 seconds (within expected range)
- Both services maintain healthy status during normal operation

**No Issues Encountered:** Implementation was clean, all tests passed on first attempt.

### File List

**Files Modified:**
1. ✅ `Dockerfile` (Lines 22-23) - Added curl installation with comment
   - Added: `# Install curl for health checks`
   - Added: `RUN apk add --no-cache curl`
   - Location: After FROM eclipse-temurin:21-jre-alpine, before WORKDIR

2. ✅ `docker-compose.yml` (Line 20) - Added start_period to postgres healthcheck
   - Added: `start_period: 10s`
   - Location: After `retries: 5` in postgres healthcheck section

**Files Verified (No Changes Needed):**
- `pom.xml` - Spring Boot Actuator already present (lines 60-64)
- `docker-compose.yml` - App healthcheck already configured (lines 77-82)
- `docker-compose.yml` - Postgres healthcheck already configured (lines 15-19)

**Testing Artifacts:**
- All manual tests completed successfully
- No automated test changes required (infrastructure configuration only)
