# Liquibase Migration Guide

## Table of Contents
1. [Overview](#overview)
2. [Current Changelog Structure](#current-changelog-structure)
3. [Changeset Evolution](#changeset-evolution)
4. [Best Practices Compliance](#best-practices-compliance)
5. [How to Add New Migrations](#how-to-add-new-migrations)
6. [Rollback Procedures](#rollback-procedures)
7. [Testing Migrations](#testing-migrations)
8. [Troubleshooting](#troubleshooting)

## Overview

This project uses **Liquibase 4.25+** for database schema version control and migrations. Liquibase automatically applies database changes during application startup, ensuring the database schema stays in sync with the application code.

### Key Configuration

**Location:** `src/main/resources/db/changelog/db.changelog-master.yaml`

**Spring Boot Configuration** (`application.yml`):
```yaml
spring:
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.yaml
```

**Execution:**
- Liquibase runs automatically on application startup
- Changes are applied in order, only once
- Executed changesets are tracked in `DATABASECHANGELOG` table
- Application fails fast if migrations fail (prevents deployment with broken schema)

## Current Changelog Structure

### Organizational Approach: Inline Changesets

**Decision:** All changesets are defined inline within the master changelog file.

**Rationale:**
- ✅ **Simplicity:** Single file to navigate (currently 4 changesets)
- ✅ **Clarity:** Entire migration history visible in one place
- ✅ **No path issues:** No include directive complexity
- ✅ **Appropriate scale:** Suitable for projects with <10 changesets
- ⚠️ **Future consideration:** May split into separate files if changelog grows beyond 10-15 changesets

**Alternative Approach (Split Files):**
When the project scales (>10 changesets), consider splitting into individual files:
```yaml
databaseChangeLog:
  - include:
      file: db/changelog/001-create-urls-table.yaml
  - include:
      file: db/changelog/002-create-normalized-url-index.yaml
```

Benefits of splitting:
- Better for large teams (fewer merge conflicts)
- Clearer file structure
- Easier to locate specific migrations

**Current decision:** Keep inline approach. Document when to split in this guide.

### Master Changelog File Structure

```yaml
databaseChangeLog:
  - changeSet:
      id: 001-create-urls-table
      author: slavaz
      changes: [...]
  
  - changeSet:
      id: 002-create-normalized-url-index
      author: slavaz
      changes: [...]
  
  - changeSet:
      id: 003-add-table-comments
      author: slavaz
      changes: [...]
  
  - changeSet:
      id: 004-update-url-index-for-app-normalization
      author: slavaz
      changes: [...]
```

## Changeset Evolution

This section explains the **why** behind each changeset and the architectural decisions made.

### Changeset 001: Create urls Table
**Purpose:** Initial schema creation for URL shortening service

**Changes:**
- Creates `urls` table with 3 columns
- `short_code` (VARCHAR(10), PRIMARY KEY): Base62-encoded Snowflake ID
- `original_url` (TEXT, NOT NULL): Original destination URL
- `created_at` (TIMESTAMP, NOT NULL, DEFAULT CURRENT_TIMESTAMP): Creation timestamp

**Constraints:**
- Primary key on `short_code` for fast lookups
- NOT NULL constraints on all columns
- Default value for `created_at`

**Rollback:** Implicitly handled by Liquibase (dropTable)

**Key Decision:** No explicit rollback needed - Liquibase auto-generates for createTable

### Changeset 002: Create Normalized URL Index
**Purpose:** Enforce uniqueness of URLs using database-side normalization

**Changes:**
- Creates unique index: `idx_original_url_normalized`
- Expression: `LOWER(TRIM(original_url))`
- Prevents duplicate URLs that differ only in case or whitespace

**Implementation Details:**
- Uses **raw SQL** (not YAML): Liquibase YAML doesn't support expression-based indexes
- PostgreSQL-specific feature: Expression indexes
- Index ensures uniqueness at database level

**Rollback:**
```yaml
rollback:
  sql: DROP INDEX IF EXISTS idx_original_url_normalized
```

**Key Decision:** Database handles normalization - application can submit URLs in any case

### Changeset 003: Add Table Comments
**Purpose:** Document database schema at the database level

**Changes:**
- Adds table comment explaining purpose
- Adds column comments describing each field
- PostgreSQL COMMENT ON syntax

**Benefits:**
- Database self-documenting (comments visible in DB tools)
- Helps DBAs understand schema without code access
- Supports database governance and auditing

**Rollback:** Not critical (comments don't affect functionality)

**Key Decision:** Database-level documentation complements code documentation

### Changeset 004: Update URL Index for Application Normalization
**Purpose:** **CRITICAL ARCHITECTURAL CHANGE** - Move normalization to application layer

**Changes:**
1. Drops expression-based index: `LOWER(TRIM(original_url))`
2. Creates simple unique index on `original_url`

**Why This Change?**

**Problem with Changeset 002 (Expression Index):**
- Database normalizes automatically
- Application queries must match expression exactly
- Complex query patterns: `WHERE LOWER(TRIM(original_url)) = ?`
- Service layer loses control over normalization logic
- Harder to extend normalization rules

**Solution (Changeset 004):**
- Database enforces uniqueness on **pre-normalized** values
- **Application normalizes BEFORE** database operations
- Service layer: `url.toLowerCase().trim()` before insert/query
- Simple index on plain column
- Full control over normalization rules in Java code

**Impact on Code:**
- ✅ Service layer MUST normalize URLs: `url.toLowerCase().trim()`
- ✅ Repository methods receive pre-normalized URLs
- ✅ Critical for Story 3.3 (JPA Entity/Repository implementation)
- ✅ Critical for Story 2.5 (Database-Enforced Idempotency)

**Rollback:**
```yaml
rollback:
  sql: |
    DROP INDEX IF EXISTS idx_original_url_normalized;
    CREATE UNIQUE INDEX idx_original_url_normalized ON urls(LOWER(TRIM(original_url)));
```
Rollback restores database-side normalization (reverts to changeset 002 approach)

**Key Decision:** Application-level normalization provides better control and simpler queries

## Best Practices Compliance

### 2024 Liquibase Best Practices ✅

**1. Immutable Changesets**
- ✅ Never modify executed changesets
- ✅ Changeset 004 adds NEW migration (doesn't modify 002)
- ✅ Liquibase tracks checksums - modification causes errors

**2. Unique Identifiers**
- ✅ All changesets have unique IDs (001, 002, 003, 004)
- ✅ IDs are descriptive (shows purpose)
- ✅ Author field populated (traceability)

**3. Rollback Support**
- ✅ Explicit rollback scripts for destructive changes (002, 004)
- ✅ Implicit rollbacks for simple operations (001 createTable)
- ✅ Production safety - can revert changes if needed

**4. PostgreSQL-Specific Features**
- ✅ Raw SQL for expression indexes (YAML limitation)
- ✅ Raw SQL for table/column comments (PostgreSQL-specific)
- ✅ Proper transaction handling (PostgreSQL supports DDL transactions)

**5. Version Control**
- ✅ All changesets in Git
- ✅ Changelog evolution documented
- ✅ Changes linked to stories (traceability)

### Spring Boot 3.2 Integration ✅

**1. Configuration**
- ✅ `spring.liquibase.enabled: true`
- ✅ `spring.liquibase.change-log: classpath:db/changelog/db.changelog-master.yaml`
- ✅ Fail-fast on startup (catches migration errors early)

**2. JPA Coordination**
- ✅ `spring.jpa.hibernate.ddl-auto: validate`
- ✅ Liquibase controls schema (Hibernate only validates)
- ✅ Prevents schema drift

**3. Docker Integration**
- ✅ Standalone Liquibase container in docker-compose.yml
- ✅ Runs before application startup
- ✅ Health checks ensure database readiness

## How to Add New Migrations

### Step 1: Identify Schema Change Need
- Link change to story/task (traceability)
- Document why change is needed
- Consider impact on existing data

### Step 2: Create New Changeset

**DO NOT modify existing changesets!** Add a new changeset to the master file.

**Naming Convention:**
- Pattern: `{sequence}-{descriptive-action}`
- Examples: `005-add-user-id-column`, `006-create-analytics-table`

**Template:**
```yaml
  - changeSet:
      id: 005-descriptive-name
      author: your-username
      changes:
        - [your changes here]
      rollback:
        - [rollback script if needed]
```

### Step 3: Choose Change Type

**For standard operations (use Liquibase YAML):**
```yaml
changes:
  - addColumn:
      tableName: urls
      columns:
        - column:
            name: user_id
            type: BIGINT
  - createIndex:
      tableName: urls
      indexName: idx_user_id
      columns:
        - column:
            name: user_id
```

**For PostgreSQL-specific features (use raw SQL):**
```yaml
changes:
  - sql:
      sql: |
        CREATE UNIQUE INDEX idx_custom ON urls(LOWER(column_name));
        COMMENT ON COLUMN urls.new_field IS 'Description';
```

### Step 4: Add Rollback Script

**Simple operations:** Auto-rollback
```yaml
# Liquibase auto-generates rollback for:
# - createTable (dropTable)
# - addColumn (dropColumn)
# - createIndex (dropIndex)
```

**Complex operations:** Explicit rollback
```yaml
changes:
  - sql:
      sql: DROP INDEX IF EXISTS old_index; CREATE INDEX new_index ...
rollback:
  - sql:
      sql: DROP INDEX IF EXISTS new_index; CREATE INDEX old_index ...
```

### Step 5: Test Migration

**Local testing:**
```bash
# Start fresh database
docker-compose down -v
docker-compose up postgres -d

# Run application (Liquibase auto-applies)
./mvnw spring-boot:run

# Verify DATABASECHANGELOG
docker exec -it url-shortener-db psql -U urlshortener -d urlshortener -c "SELECT * FROM databasechangelog;"
```

**Integration test:**
```java
@SpringBootTest
@Testcontainers
class LiquibaseMigrationTest {
    // Test validates migrations apply correctly
}
```

### Step 6: Document Decision

Add entry to **Dev Notes** or **Migration Guide** explaining:
- What changed
- Why the change was needed
- Impact on application code
- Related stories

## Rollback Procedures

### Automatic Rollback (Development Only)

⚠️ **WARNING:** Rollbacks should be tested in non-production environments first!

**Using Liquibase CLI:**
```bash
# Rollback last changeset
liquibase --changelog-file=db/changelog/db.changelog-master.yaml \
          --url=jdbc:postgresql://localhost:5432/urlshortener \
          --username=urlshortener \
          --password=urlshortener_pass \
          rollback-count 1

# Rollback to specific tag
liquibase rollback-to-tag v1.0.0
```

**Using Docker:**
```bash
docker run --rm --network url-shortener-network \
  -v $(pwd)/src/main/resources/db/changelog:/liquibase/changelog \
  liquibase/liquibase:4.25-alpine \
  --changelog-file=changelog/db.changelog-master.yaml \
  --url=jdbc:postgresql://postgres:5432/urlshortener \
  --username=urlshortener \
  --password=urlshortener_pass \
  rollback-count 1
```

### Production Rollback Strategy

**Best Practice:** Don't rollback in production. Instead:

1. **Write forward-fixing migration**
   ```yaml
   - changeSet:
       id: 007-fix-issue-from-006
       author: username
       changes:
         - [corrective changes]
   ```

2. **Use feature flags** to disable broken features

3. **Deploy new version** with fixing changeset

**Why avoid rollback in production?**
- Data may have changed after migration
- Rollback may cause data loss
- Forward-fixes are safer and traceable

## Testing Migrations

### Unit Test: Validate Changelog Parses

```java
@Test
void changelogShouldParseSuccessfully() throws Exception {
    ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor();
    DatabaseChangeLog changeLog = ChangeLogParserFactory
        .getInstance()
        .getParser("yaml", resourceAccessor)
        .parse("db/changelog/db.changelog-master.yaml", 
               new ChangeLogParameters(), 
               resourceAccessor);
    
    assertThat(changeLog).isNotNull();
    assertThat(changeLog.getChangeSets()).hasSize(4);
}
```

### Integration Test: Apply Migrations

```java
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = Replace.NONE)
class LiquibaseMigrationIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    
    @Autowired
    private DataSource dataSource;
    
    @Test
    void migrationsShouldApplySuccessfully() throws Exception {
        // Given: Fresh database
        
        // When: Application starts (Liquibase auto-runs)
        
        // Then: Verify schema exists
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            // Verify urls table exists
            ResultSet tables = metaData.getTables(null, null, "urls", null);
            assertThat(tables.next()).isTrue();
            
            // Verify columns
            ResultSet columns = metaData.getColumns(null, null, "urls", null);
            List<String> columnNames = new ArrayList<>();
            while (columns.next()) {
                columnNames.add(columns.getString("COLUMN_NAME"));
            }
            assertThat(columnNames).containsExactlyInAnyOrder(
                "short_code", "original_url", "created_at"
            );
            
            // Verify indexes
            ResultSet indexes = metaData.getIndexInfo(null, null, "urls", false, false);
            List<String> indexNames = new ArrayList<>();
            while (indexes.next()) {
                indexNames.add(indexes.getString("INDEX_NAME"));
            }
            assertThat(indexNames).contains("idx_original_url_normalized");
        }
    }
    
    @Test
    void changelogTableShouldContainAllChangesets() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        
        List<String> changesetIds = jdbc.queryForList(
            "SELECT id FROM databasechangelog ORDER BY orderexecuted",
            String.class
        );
        
        assertThat(changesetIds).containsExactly(
            "001-create-urls-table",
            "002-create-normalized-url-index",
            "003-add-table-comments",
            "004-update-url-index-for-app-normalization"
        );
    }
}
```

### Manual Testing Checklist

- [ ] Fresh database: `docker-compose down -v && docker-compose up -d postgres`
- [ ] Application starts: `./mvnw spring-boot:run`
- [ ] No Liquibase errors in logs
- [ ] DATABASECHANGELOG contains 4 entries
- [ ] `urls` table exists with correct structure
- [ ] Index `idx_original_url_normalized` exists
- [ ] Constraints work (try inserting duplicate normalized URL)

## Troubleshooting

### Error: "Checksum mismatch"

**Cause:** Existing changeset was modified

**Solution:**
```sql
-- DO NOT DO THIS IN PRODUCTION
DELETE FROM databasechangelog WHERE id = 'problematic-changeset-id';
```

**Better solution:** Revert changes to original changeset (use Git)

### Error: "Table already exists"

**Cause:** Schema was created outside Liquibase

**Solution:**
```yaml
- changeSet:
    id: 001-create-urls-table
    preConditions:
      onFail: MARK_RAN
      not:
        tableExists:
          tableName: urls
    changes: [...]
```

### Error: Migration fails mid-execution

**Cause:** PostgreSQL transaction rolled back

**Solution:**
1. Check Liquibase logs for specific error
2. Fix changeset SQL
3. Clear failed entry: `DELETE FROM databasechangelog WHERE id = 'failed-id';`
4. Re-run migration

### Application won't start after migration

**Cause:** JPA entity doesn't match schema

**Check:**
```bash
# Compare schema with entity
./mvnw spring-boot:run -Dspring.jpa.hibernate.ddl-auto=validate
```

**Fix:** Update entity or add missing migration

## References

- [Liquibase Documentation](https://docs.liquibase.com/)
- [Spring Boot Liquibase Integration](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization.migration-tool.liquibase)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/16/)
- Project Architecture: `_bmad-output/planning-artifacts/architecture.md`
- Database Schema Design: `docs/DATABASE_SCHEMA_DESIGN.md`

---

**Last Updated:** 2024-02-09  
**Maintainer:** Development Team  
**Related Stories:** Epic 3 (Database Integration & Storage)
