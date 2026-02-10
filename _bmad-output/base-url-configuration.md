# Base URL Configuration

## Overview
The URL shortener now supports configurable base URL for shortened links through environment variables and properties files.

## Configuration Priority

The base URL is resolved in the following order:

1. **Environment Variable**: `SHORT_URL_BASE` (highest priority)
2. **Properties File**: `app.base-url` in `application.yml` 
3. **Default**: `http://localhost:8080/`

## Usage Examples

### Local Development
Default configuration uses `http://localhost:8080/`:
```bash
mvn spring-boot:run
```

### Production with Environment Variable
```bash
export SHORT_URL_BASE=https://short.mydomain.com/
java -jar target/url-shortener-1.0.0-SNAPSHOT.jar
```

### Docker Compose
Set the environment variable before starting:
```bash
export SHORT_URL_BASE=https://short.mydomain.com/
docker-compose up
```

Or modify `docker-compose.yml` to set a custom default:
```yaml
environment:
  SHORT_URL_BASE: https://short.mydomain.com/
```

### Docker Run
```bash
docker run -e SHORT_URL_BASE=https://short.mydomain.com/ url-shortener
```

## Implementation Details

### Files Modified
1. **src/main/resources/application.yml**
   - Changed: `app.base-url: ${SHORT_URL_BASE:http://localhost:8080/}`
   - Reads from env var `SHORT_URL_BASE`, defaults to `http://localhost:8080/`

2. **src/main/java/com/example/urlshortener/service/UrlShortenerServiceImpl.java**
   - Added: `@Value("${app.base-url}") String baseUrl` constructor parameter
   - Handles trailing slashes automatically
   - Uses configured base URL in `toDto()` method

3. **docker-compose.yml**
   - Added: `SHORT_URL_BASE: ${SHORT_URL_BASE:-http://localhost:3000/}`
   - Allows override via host environment variable

4. **src/test/resources/application-test.properties**
   - Added: `app.base-url=http://localhost:8080/` for test consistency

5. **src/test/java/com/example/urlshortener/service/UrlShortenerServiceImplTest.java**
   - Updated: Manual service instantiation with base URL parameter
   - All 202 tests pass

## Behavior

### Trailing Slash Handling
The implementation automatically removes trailing slashes from the base URL to prevent double slashes:
- Input: `http://localhost:8080/` → Stored: `http://localhost:8080`
- Input: `http://localhost:8080` → Stored: `http://localhost:8080`
- Short URL always constructed as: `baseUrl + "/" + shortCode`

### Example Response
With `SHORT_URL_BASE=https://short.example.com/`:
```json
{
  "shortCode": "aB3xK9",
  "shortUrl": "https://short.example.com/aB3xK9"
}
```

## Testing

All tests pass (202 tests):
```bash
mvn test
```

Specific service tests:
```bash
mvn test -Dtest=UrlShortenerServiceImplTest
```
