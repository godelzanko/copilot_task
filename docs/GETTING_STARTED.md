# Getting Started Guide

## Prerequisites

Before you begin, ensure you have the following installed:

- **Docker** 24.0 or higher ([Install Docker](https://docs.docker.com/get-docker/))
- **Docker Compose** 3.8 or higher (usually bundled with Docker Desktop)
- **Java 21** (for local development) ([Download Java](https://adoptium.net/))
- **Maven 3.9+** (for local development) ([Download Maven](https://maven.apache.org/download.cgi))
- **Git** (for version control)

## Quick Start (Docker - Recommended)

The fastest way to get the URL Shortener service running:

### 1. Clone and Navigate
```bash
git clone <repository-url>
cd copilot_task
```

### 2. Start All Services
```bash
docker-compose up --build
```

This single command will:
- Build the Spring Boot application
- Start PostgreSQL database
- Run Liquibase migrations
- Launch the application

### 3. Wait for Startup
Watch the logs until you see:
```
url-shortener-app | Started UrlShortenerApplication in X.XXX seconds
```

### 4. Test the Service
```bash
# Create a short URL
curl -X POST http://localhost:3000/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://www.example.com"}'

# Expected response:
# {"shortCode":"aB3xK9","shortUrl":"http://localhost:3000/aB3xK9"}

# Test the redirect
curl -I http://localhost:3000/aB3xK9

# Expected response:
# HTTP/1.1 301 Moved Permanently
# Location: https://www.example.com
```

🎉 **Success!** Your URL shortener is running.

---

## Local Development Setup

For active development without Docker:

### 1. Start PostgreSQL Database
```bash
docker run -d \
  --name url-shortener-db \
  -e POSTGRES_DB=urlshortener \
  -e POSTGRES_USER=urlshortener \
  -e POSTGRES_PASSWORD=urlshortener_pass \
  -p 5432:5432 \
  postgres:16-alpine
```

### 2. Verify Database Connection
```bash
docker exec -it url-shortener-db psql -U urlshortener -d urlshortener -c "\l"
```

### 3. Build the Application
```bash
mvn clean install
```

### 4. Run Database Migrations (Liquibase)
```bash
mvn liquibase:update
```

### 5. Run the Application
```bash
mvn spring-boot:run
```

The service will start on `http://localhost:8080`.

### 6. Run Tests
```bash
# Unit tests
mvn test

# Integration tests (requires Testcontainers)
mvn verify

# Generate code coverage report
mvn test jacoco:report
# View report at: target/site/jacoco/index.html
```

---

## Project Structure Overview

```
copilot_task/
├── src/
│   ├── main/
│   │   ├── java/com/example/urlshortener/
│   │   │   ├── UrlShortenerApplication.java    # Main entry point
│   │   │   ├── controller/                     # REST endpoints (TO BE CREATED)
│   │   │   ├── service/                        # Business logic (TO BE CREATED)
│   │   │   ├── repository/                     # Data access (TO BE CREATED)
│   │   │   ├── entity/                         # JPA entities (TO BE CREATED)
│   │   │   ├── dto/                            # Request/Response DTOs (TO BE CREATED)
│   │   │   ├── generator/                      # Snowflake ID generator (TO BE CREATED)
│   │   │   ├── config/                         # Spring configuration (TO BE CREATED)
│   │   │   └── exception/                      # Custom exceptions (TO BE CREATED)
│   │   └── resources/
│   │       ├── application.yml                 # Spring Boot configuration ✅
│   │       └── db/changelog/
│   │           └── db.changelog-master.yaml    # Liquibase migrations ✅
│   └── test/                                   # Test classes (TO BE CREATED)
├── _bmad-output/
│   └── planning-artifacts/
│       ├── PRD.md                              # Product Requirements Document
│       └── architecture.md                     # Architecture Documentation ✅
├── docker-compose.yml                          # Multi-service orchestration ✅
├── Dockerfile                                  # Application container ✅
├── pom.xml                                     # Maven configuration ✅
└── README.md                                   # Project documentation ✅
```

✅ = Created by this foundational setup
TO BE CREATED = Implementation tasks

---

## Next Development Steps

Now that the foundation is in place, follow these steps to implement the URL Shortener:

### Phase 1: Core Components (Week 1)

#### 1. Implement Snowflake ID Generator
**File:** `src/main/java/com/example/urlshortener/generator/SnowflakeIdGenerator.java`

**Key Features:**
- 41-bit timestamp (custom epoch: 2024-01-01)
- 10-bit instance ID (hardcoded 0)
- 13-bit sequence counter
- Base62 encoding
- Thread-safe implementation

**Estimated Time:** 2-3 hours

---

#### 2. Create JPA Entity
**File:** `src/main/java/com/example/urlshortener/entity/UrlEntity.java`

**Schema Mapping:**
```java
@Entity
@Table(name = "urls")
public class UrlEntity {
    @Id
    private String shortCode;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String originalUrl;
    
    @CreationTimestamp
    private Instant createdAt;
}
```

**Estimated Time:** 30 minutes

---

#### 3. Create Repository
**File:** `src/main/java/com/example/urlshortener/repository/UrlRepository.java`

**Custom Query:**
```java
public interface UrlRepository extends JpaRepository<UrlEntity, String> {
    @Query("SELECT u FROM UrlEntity u WHERE LOWER(TRIM(u.originalUrl)) = LOWER(TRIM(:url))")
    Optional<UrlEntity> findByNormalizedUrl(@Param("url") String url);
}
```

**Estimated Time:** 20 minutes

---

#### 4. Create DTOs
**Files:**
- `src/main/java/com/example/urlshortener/dto/ShortenRequest.java`
- `src/main/java/com/example/urlshortener/dto/ShortenResponse.java`

**Example:**
```java
public record ShortenRequest(@NotBlank String url) {}
public record ShortenResponse(String shortCode, String shortUrl) {}
```

**Estimated Time:** 15 minutes

---

#### 5. Implement Service Layer
**File:** `src/main/java/com/example/urlshortener/service/UrlShortenerService.java`

**Key Methods:**
- `shortenUrl(String originalUrl)` → ShortenResponse
- `getOriginalUrl(String shortCode)` → String

**Patterns:**
- Try-insert-catch-select for idempotency
- URL normalization (lowercase, trim)
- Transaction management

**Estimated Time:** 2-3 hours

---

#### 6. Create REST Controllers
**Files:**
- `src/main/java/com/example/urlshortener/controller/ShortenController.java`
- `src/main/java/com/example/urlshortener/controller/RedirectController.java`

**Endpoints:**
- `POST /api/shorten` → Create short URL
- `GET /{shortCode}` → Redirect to original URL

**Estimated Time:** 1-2 hours

---

#### 7. Exception Handling
**Files:**
- `src/main/java/com/example/urlshortener/exception/UrlNotFoundException.java`
- `src/main/java/com/example/urlshortener/exception/InvalidUrlException.java`
- `src/main/java/com/example/urlshortener/exception/GlobalExceptionHandler.java`

**Estimated Time:** 1 hour

---

### Phase 2: Testing (Week 1-2)

#### 8. Unit Tests
- SnowflakeIdGenerator tests
- Service layer tests
- URL validation tests

**Estimated Time:** 3-4 hours

---

#### 9. Integration Tests
- End-to-end API tests
- Testcontainers with PostgreSQL
- Idempotency tests

**Estimated Time:** 3-4 hours

---

### Phase 3: Documentation & Polish (Week 2)

#### 10. API Documentation
- Add Swagger/OpenAPI annotations
- Generate API docs

**Estimated Time:** 1-2 hours

---

#### 11. Docker Testing
- Test full docker-compose deployment
- Verify health checks
- Test migrations

**Estimated Time:** 1 hour

---

## Troubleshooting

### Docker Issues

**Problem:** Port 5432 already in use
```bash
# Solution: Stop existing PostgreSQL
docker ps | grep postgres
docker stop <container-id>
# Or use different port in docker-compose.yml
```

**Problem:** Permission denied for Docker
```bash
# Solution: Add user to docker group (Linux)
sudo usermod -aG docker $USER
# Log out and back in
```

**Problem:** Build fails with "Cannot connect to Docker daemon"
```bash
# Solution: Start Docker Desktop or Docker service
# Windows/Mac: Start Docker Desktop
# Linux: sudo systemctl start docker
```

---

### Maven Issues

**Problem:** `JAVA_HOME` not set
```bash
# Solution: Set JAVA_HOME
export JAVA_HOME=/path/to/jdk-21
export PATH=$JAVA_HOME/bin:$PATH
```

**Problem:** Dependency download fails
```bash
# Solution: Clear Maven cache and retry
rm -rf ~/.m2/repository
mvn clean install
```

---

### Database Issues

**Problem:** Database connection refused
```bash
# Solution: Verify PostgreSQL is running
docker ps | grep postgres

# Check logs
docker logs url-shortener-db

# Verify connection
docker exec -it url-shortener-db pg_isready -U urlshortener
```

**Problem:** Liquibase migrations fail
```bash
# Solution: Check changelog syntax
mvn liquibase:validate

# Rollback and retry
mvn liquibase:rollback -Dliquibase.rollbackCount=1
mvn liquibase:update
```

---

## Useful Commands

### Docker Operations
```bash
# View all logs
docker-compose logs -f

# View app logs only
docker-compose logs -f app

# Restart a service
docker-compose restart app

# Rebuild and restart
docker-compose up --build -d

# Stop all services
docker-compose down

# Stop and remove volumes (data loss!)
docker-compose down -v
```

### Database Operations
```bash
# Connect to database
docker exec -it url-shortener-db psql -U urlshortener -d urlshortener

# Run SQL query
docker exec -it url-shortener-db psql -U urlshortener -d urlshortener -c "SELECT * FROM urls;"

# Backup database
docker exec url-shortener-db pg_dump -U urlshortener urlshortener > backup.sql

# Restore database
docker exec -i url-shortener-db psql -U urlshortener -d urlshortener < backup.sql
```

### Maven Operations
```bash
# Clean build
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Run specific test
mvn test -Dtest=SnowflakeIdGeneratorTest

# Generate dependency tree
mvn dependency:tree

# Update dependencies
mvn versions:display-dependency-updates
```

---

## Additional Resources

- **Architecture Documentation:** `_bmad-output/planning-artifacts/architecture.md`
- **Product Requirements:** `_bmad-output/planning-artifacts/PRD.md`
- **Brainstorming Session:** `_bmad-output/brainstorming/brainstorming-session-2026-02-06.md`

- **Spring Boot Guides:** https://spring.io/guides
- **Liquibase Docs:** https://docs.liquibase.com
- **Docker Compose:** https://docs.docker.com/compose/
- **Snowflake Algorithm:** https://en.wikipedia.org/wiki/Snowflake_ID

---

**Ready to start coding? Follow the "Next Development Steps" section to implement the URL Shortener!** 🚀
