# URL Shortener Service

A minimalist, production-ready REST API for URL shortening built with Spring Boot, PostgreSQL, and Docker. This project demonstrates industry-standard patterns including Snowflake ID generation, database-enforced idempotency, and containerized deployment.

## 🎯 Core Philosophy: "HashMap-via-REST"

This service is fundamentally a **persistent key-value store exposed through a RESTful HTTP interface**. It focuses on doing one thing exceptionally well: transforming long URLs into short, shareable links with guaranteed consistency.

## ✨ Features

- **Collision-Free ID Generation:** DIY Snowflake algorithm with Base62 encoding
- **Guaranteed Idempotency:** Same URL always returns the same short code (database-enforced)
- **Fast Redirects:** Sub-100ms response time with PostgreSQL indexed lookups
- **Production-Ready Deployment:** Fully containerized with Docker Compose
- **Clean Architecture:** Layered design with clear separation of concerns
- **Database Migrations:** Version-controlled schema changes with Liquibase

## 🏗️ Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 21 |
| Framework | Spring Boot | 3.2+ |
| Build Tool | Maven | 3.9+ |
| Database | PostgreSQL | 16 |
| Migrations | Liquibase | 4.25+ |
| Container | Docker | 24.0+ |
| Orchestration | docker-compose | 3.8+ |

## 🚀 Quick Start

### Prerequisites

- **Docker 24.0+** ([installation guide](https://docs.docker.com/get-docker/))
- **docker-compose 3.8+** (usually bundled with Docker Desktop)
- Java 21 (for local development only)
- Maven 3.9+ (for local development only)

**Verify installation:**
```bash
docker --version

# Docker Compose v1 (standalone)
docker-compose --version

# OR Docker Compose v2 (integrated, recommended)
docker compose version
```

> **Note:** This project works with both `docker-compose` (v1) and `docker compose` (v2). Use whichever is available on your system.

### Run with Docker (Recommended)

```bash
# Clone the repository
git clone <repository-url>
cd copilot_task

# Build and start all services
docker-compose up --build

# Service will be available at http://localhost:3000
```

That's it! The service will automatically:
1. Start PostgreSQL database
2. Run Liquibase migrations
3. Launch the Spring Boot application

**Stop the service:**
```bash
# Stop with Ctrl+C, then clean up containers
docker-compose down
```

### Run Locally (Development)

```bash
# Start PostgreSQL (via Docker)
docker run -d \
  --name url-shortener-db \
  -e POSTGRES_DB=urlshortener \
  -e POSTGRES_USER=urlshortener \
  -e POSTGRES_PASSWORD=urlshortener_pass \
  -p 5432:5432 \
  postgres:16-alpine

# Build and run the application
mvn clean install
mvn spring-boot:run

# Service will be available at http://localhost:8080
```

## 📚 API Reference

### Create Short URL

**Endpoint:** `POST /api/shorten`  
**Content-Type:** `application/json`

**Request:**
```json
{
  "url": "https://github.com/spring-projects/spring-boot"
}
```

**Response (200 OK):**
```json
{
  "shortCode": "aB3xK9",
  "shortUrl": "http://localhost:3000/aB3xK9"
}
```

**Example:**
```bash
curl -X POST http://localhost:3000/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://github.com/spring-projects/spring-boot"}'
```

**Business Rules:**
- Same URL always returns the same short code (idempotent)
- URLs are normalized (lowercase, trimmed)
- Only HTTP/HTTPS URLs are accepted

---

### Redirect to Original URL

**Endpoint:** `GET /{shortCode}`

**Response (301 Moved Permanently):**
```
HTTP/1.1 301 Moved Permanently
Location: https://github.com/spring-projects/spring-boot
```

**Example:**
```bash
# View redirect headers
curl -I http://localhost:3000/aB3xK9

# Or simply open in browser
open http://localhost:3000/aB3xK9
```

**Behavior:**
- Returns HTTP 301 (permanent redirect) if short code exists
- Returns HTTP 404 (not found) if short code doesn't exist
- Browsers may cache the redirect

## 🔧 Troubleshooting

### Port 3000 Already in Use

**Problem:** `Error: bind: address already in use`

**Solutions:**
```bash
# Option 1: Stop the conflicting service
# Find what's using port 3000
lsof -i :3000  # macOS/Linux
netstat -ano | findstr :3000  # Windows

# Kill the process or stop the conflicting service

# Option 2: Change the port in docker-compose.yml
# Edit the ports section:
ports:
  - "8080:8080"  # Change 3000 to any available port

# Then update APP_BASE_URL
docker-compose up --build
```

### Docker Permission Denied

**Problem:** `permission denied while trying to connect to Docker daemon socket`

**Solutions:**
```bash
# Linux: Add your user to docker group
sudo usermod -aG docker $USER
newgrp docker  # Activate the changes

# Or use sudo (not recommended for regular use)
sudo docker-compose up --build

# macOS/Windows: Ensure Docker Desktop is running
```

### Database Migration Errors

**Problem:** Liquibase fails with changelog errors

**Solutions:**
```bash
# View Liquibase logs
docker-compose logs liquibase

# Common fixes:

# 1. Clean slate - remove database and restart
docker-compose down -v
docker-compose up --build

# 2. Check database connectivity
docker exec -it url-shortener-db psql -U urlshortener -d urlshortener

# 3. Verify changelog file syntax
cat src/main/resources/db/changelog/db.changelog-master.yaml
```

### Application Won't Start / Health Check Failing

**Problem:** App container keeps restarting

**Solutions:**
```bash
# Check application logs
docker-compose logs -f app

# Check health endpoint manually
docker exec -it url-shortener-app curl http://localhost:8080/actuator/health

# Common causes:
# - Database not ready: Wait 30-60 seconds after first start
# - Migration failed: Check liquibase logs
# - Port conflict: Ensure internal port 8080 is free inside container
# - Memory issues: Check Docker resource limits in docker-compose.yml
```

### Service Stuck / Not Responding

**Problem:** Commands hang or services don't respond

**Solutions:**
```bash
# Force restart all services
docker-compose restart

# Nuclear option - full cleanup
docker-compose down
docker system prune -a  # Warning: removes all unused Docker resources
docker-compose up --build

# Check if containers are actually running
docker-compose ps

# Check system resources
docker stats
```

## 🏛️ Architecture Overview

```
┌─────────────────────────────────────┐
│         API Clients                 │
└────────────┬────────────────────────┘
             │ HTTP/REST
             ▼
┌─────────────────────────────────────┐
│    Spring Boot Application          │
│  ┌──────────────────────────────┐   │
│  │  Controllers                 │   │
│  │  • POST /api/shorten         │   │
│  │  • GET /{shortCode}          │   │
│  └─────────┬────────────────────┘   │
│            │                         │
│  ┌─────────▼────────────────────┐   │
│  │  Service Layer               │   │
│  │  • URL validation            │   │
│  │  • Idempotency handling      │   │
│  └─────────┬────────────────────┘   │
│            │                         │
│  ┌─────────▼────────────────────┐   │
│  │  Snowflake ID Generator      │   │
│  │  • Time-based IDs            │   │
│  │  • Base62 encoding           │   │
│  └─────────┬────────────────────┘   │
│            │                         │
│  ┌─────────▼────────────────────┐   │
│  │  Repository Layer            │   │
│  │  • Spring Data JPA           │   │
│  └─────────┬────────────────────┘   │
└────────────┼────────────────────────┘
             │ JDBC
             ▼
┌─────────────────────────────────────┐
│      PostgreSQL Database            │
│  ┌──────────────────────────────┐   │
│  │  urls table                  │   │
│  │  • short_code (PK)           │   │
│  │  • original_url              │   │
│  │  • created_at                │   │
│  └──────────────────────────────┘   │
└─────────────────────────────────────┘
```

## 🔑 Key Design Decisions

### 1. Snowflake ID Generation
- **41-bit timestamp** (milliseconds since 2024-01-01) = 69 years capacity
- **10-bit instance ID** (hardcoded 0 for MVP) = supports 1,024 instances
- **13-bit sequence** = 8,192 IDs per millisecond per instance
- **Base62 encoding** = short, URL-safe identifiers (~7 characters)

### 2. Database-Enforced Idempotency
- UNIQUE index on `LOWER(TRIM(original_url))` prevents duplicates
- Try-insert-catch-select pattern handles concurrent requests gracefully
- Database constraint ensures atomicity without application-level locks

### 3. Three-Service Docker Architecture
```
postgres (healthy) → liquibase (completed) → app (running)
```
- PostgreSQL starts first, waits for health check
- Liquibase runs migrations, exits on completion
- Application starts after migrations succeed

### 4. HTTP 301 Permanent Redirect
- SEO-friendly (search engines pass link authority)
- Browser-cacheable (performance optimization)
- Industry standard for URL shorteners

## 📁 Project Structure

```
copilot_task/
├── src/
│   └── main/
│       ├── java/com/example/urlshortener/
│       │   ├── controller/          # REST endpoints
│       │   ├── service/              # Business logic
│       │   ├── repository/           # Data access
│       │   ├── entity/               # JPA entities
│       │   ├── dto/                  # Data transfer objects
│       │   ├── generator/            # Snowflake ID generator
│       │   └── UrlShortenerApplication.java
│       └── resources/
│           ├── db/changelog/         # Liquibase migrations
│           │   └── db.changelog-master.yaml
│           └── application.yml       # Spring configuration
├── docker-compose.yml                # Multi-service orchestration
├── Dockerfile                        # Multi-stage app container
├── pom.xml                           # Maven dependencies
└── README.md                         # This file
```

## 🧪 Testing

### Manual Testing with curl

```bash
# 1. Create a short URL
SHORT_URL=$(curl -s -X POST http://localhost:3000/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://www.example.com"}' | jq -r '.shortUrl')

echo "Short URL: $SHORT_URL"

# 2. Test redirect
curl -I $SHORT_URL

# 3. Test idempotency (same URL returns same short code)
curl -X POST http://localhost:3000/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://www.example.com"}'

# 4. Test invalid URL
curl -X POST http://localhost:3000/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "not-a-valid-url"}'
```

### Unit & Integration Tests

```bash
# Run all tests
mvn test

# Run with coverage report
mvn test jacoco:report
# Coverage report: target/site/jacoco/index.html
```

## 🐳 Docker Commands

### View Service Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f app
docker-compose logs -f postgres
docker-compose logs -f liquibase
```

### Database Management
```bash
# Connect to PostgreSQL
docker exec -it url-shortener-db psql -U urlshortener -d urlshortener

# View tables
\dt

# View urls table structure
\d urls

# Query all URLs
SELECT * FROM urls;

# Exit psql
\q
```

### Stop and Clean Up
```bash
# Stop services (preserves data)
docker-compose down

# Stop and remove volumes (deletes data)
docker-compose down -v

# Rebuild from scratch
docker-compose down -v && docker-compose up --build
```

## 🔧 Configuration

### Application Properties

**File:** `src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/urlshortener
    username: urlshortener
    password: urlshortener_pass
  
  jpa:
    hibernate:
      ddl-auto: validate  # Schema managed by Liquibase
    show-sql: false
    properties:
      hibernate:
        format_sql: true

server:
  port: 8080

# Custom configuration
app:
  snowflake:
    epoch: 2024-01-01T00:00:00Z
    instance-id: 0
```

### Environment Variables (Docker)

Override configuration via environment variables in `docker-compose.yml`:

```yaml
environment:
  SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/urlshortener
  SPRING_DATASOURCE_USERNAME: urlshortener
  SPRING_DATASOURCE_PASSWORD: urlshortener_pass
  SERVER_PORT: 8080
```

## 📈 Performance Characteristics

| Metric | Value | Notes |
|--------|-------|-------|
| **Redirect Latency** | <100ms p95 | PostgreSQL indexed lookup |
| **ID Generation** | 8,192/ms | Snowflake sequence capacity |
| **Throughput** | Hundreds req/s | Limited by DB connection pool |
| **URL Capacity** | Billions | 41-bit timestamp = 69 years |
| **Short Code Length** | ~7 characters | Base62-encoded Snowflake ID |

## 🛡️ Security Considerations

### Current State (MVP)
- ✅ SQL injection prevention (JPA parameterized queries)
- ✅ URL validation (Java `URL()` class)
- ✅ Input sanitization (trim, lowercase)
- ⚠️ No authentication/authorization (public API)
- ⚠️ No rate limiting (acceptable for learning project)
- ⚠️ No malicious URL detection (no content filtering)

### Production Recommendations
- Implement API key authentication
- Add rate limiting (Spring Boot Actuator)
- Use environment-specific credentials (Docker secrets)
- Enable HTTPS/TLS
- Add URL blacklist/whitelist
- Implement abuse detection

## 🚫 Out of Scope (Deferred to v2.0)

The following features are intentionally excluded from MVP to maintain focus:

- ❌ **Caching layer** (Redis/Caffeine) - PostgreSQL is fast enough
- ❌ **Visit tracking** - Adds complexity without core learning value
- ❌ **Link expiration** - Can add later via Liquibase migration
- ❌ **Custom short codes** - Vanity URLs feature
- ❌ **Analytics dashboard** - Real-time metrics and graphs
- ❌ **Bulk operations** - Batch URL shortening API
- ❌ **Link editing** - Update destination URL

These features may be added in future iterations once core functionality is proven.

## 📖 Learning Resources

### Architectural Concepts
- **Snowflake ID Algorithm:** [Twitter's Announcement](https://blog.twitter.com/engineering/en_us/a/2010/announcing-snowflake)
- **Base62 Encoding:** [Wikipedia](https://en.wikipedia.org/wiki/Base62)
- **Database Constraints:** [PostgreSQL UNIQUE Constraints](https://www.postgresql.org/docs/current/ddl-constraints.html)

### Technologies Used
- **Spring Boot:** [Official Guides](https://spring.io/guides)
- **Spring Data JPA:** [Reference Documentation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- **Liquibase:** [Getting Started](https://docs.liquibase.com/start/home.html)
- **Docker Compose:** [Overview](https://docs.docker.com/compose/)

## 🤝 Contributing

This is a learning project. Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is created for educational purposes as part of a programming course.

## 👥 Authors

- **Developer:** Slavaz
- **Course:** Gödel Technologies Programming Course
- **Date:** 2026-02-06

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- PostgreSQL community for the robust database
- Docker for containerization technology
- Twitter engineering for the Snowflake algorithm

---

**Built with ❤️ for learning and demonstrating production-grade software engineering practices.**