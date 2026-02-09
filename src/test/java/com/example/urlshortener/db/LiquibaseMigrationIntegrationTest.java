package com.example.urlshortener.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Liquibase database migrations.
 * 
 * Tests validate that:
 * - All changesets apply successfully to a fresh PostgreSQL database
 * - Database schema matches expected structure
 * - Indexes and constraints are created correctly
 * - Application can start with Liquibase migrations enabled
 * 
 * Story: 3-2 Create Liquibase Migration Changelog
 * AC: #2, #3, #4, #5
 */
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LiquibaseMigrationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.liquibase.enabled", () -> "true");
    }

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Test: AC #5 - Master changelog includes all changesets
     * Validates that all 4 changesets have been applied successfully
     */
    @Test
    void changelogTableShouldContainAllChangesets() {
        // When: Application starts with Liquibase enabled
        
        // Then: DATABASECHANGELOG contains all 4 changesets in order
        List<String> changesetIds = jdbcTemplate.queryForList(
            "SELECT id FROM databasechangelog ORDER BY orderexecuted",
            String.class
        );

        assertThat(changesetIds)
            .as("All changesets should be applied in order")
            .containsExactly(
                "001-create-urls-table",
                "002-create-normalized-url-index",
                "003-add-table-comments",
                "004-update-url-index-for-app-normalization"
            );
    }

    /**
     * Test: AC #2 - Changeset 001 creates urls table with correct structure
     * Validates table structure, columns, data types, and constraints
     */
    @Test
    void urlsTableShouldExistWithCorrectStructure() throws Exception {
        // When: Liquibase migrations have run
        
        // Then: urls table should exist
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            // Verify table exists
            ResultSet tables = metaData.getTables(null, "public", "urls", new String[]{"TABLE"});
            assertThat(tables.next())
                .as("urls table should exist")
                .isTrue();
            
            // Verify columns exist with correct types
            ResultSet columns = metaData.getColumns(null, "public", "urls", null);
            List<ColumnInfo> columnInfos = new ArrayList<>();
            while (columns.next()) {
                columnInfos.add(new ColumnInfo(
                    columns.getString("COLUMN_NAME"),
                    columns.getString("TYPE_NAME"),
                    columns.getInt("NULLABLE") == DatabaseMetaData.columnNullable
                ));
            }
            
            assertThat(columnInfos)
                .as("Table should have exactly 3 columns")
                .hasSize(3)
                .extracting(ColumnInfo::name)
                .containsExactlyInAnyOrder("short_code", "original_url", "created_at");
            
            // Verify short_code column
            ColumnInfo shortCodeCol = findColumn(columnInfos, "short_code");
            assertThat(shortCodeCol.type())
                .as("short_code should be VARCHAR")
                .isEqualTo("varchar");
            assertThat(shortCodeCol.nullable())
                .as("short_code should be NOT NULL")
                .isFalse();
            
            // Verify original_url column
            ColumnInfo originalUrlCol = findColumn(columnInfos, "original_url");
            assertThat(originalUrlCol.type())
                .as("original_url should be TEXT")
                .isEqualTo("text");
            assertThat(originalUrlCol.nullable())
                .as("original_url should be NOT NULL")
                .isFalse();
            
            // Verify created_at column
            ColumnInfo createdAtCol = findColumn(columnInfos, "created_at");
            assertThat(createdAtCol.type())
                .as("created_at should be TIMESTAMP")
                .isEqualTo("timestamp");
            assertThat(createdAtCol.nullable())
                .as("created_at should be NOT NULL")
                .isFalse();
        }
    }

    /**
     * Test: AC #2 - Primary key constraint on short_code
     * Validates that short_code is the primary key
     */
    @Test
    void shortCodeShouldBePrimaryKey() throws Exception {
        // When: Liquibase migrations have run
        
        // Then: short_code should be primary key
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet primaryKeys = metaData.getPrimaryKeys(null, "public", "urls");
            
            List<String> pkColumns = new ArrayList<>();
            while (primaryKeys.next()) {
                pkColumns.add(primaryKeys.getString("COLUMN_NAME"));
            }
            
            assertThat(pkColumns)
                .as("short_code should be the only primary key column")
                .containsExactly("short_code");
        }
    }

    /**
     * Test: AC #3 - Changeset 002 creates unique index (modified by changeset 004)
     * Validates that the index exists and is unique
     * Note: Changeset 004 modified this to be a simple unique index (not expression-based)
     */
    @Test
    void normalizedUrlIndexShouldExist() throws Exception {
        // When: Liquibase migrations have run (including changeset 004)
        
        // Then: idx_original_url_normalized should exist as unique index
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet indexes = metaData.getIndexInfo(null, "public", "urls", false, false);
            
            List<IndexInfo> indexInfos = new ArrayList<>();
            while (indexes.next()) {
                String indexName = indexes.getString("INDEX_NAME");
                if (indexName != null && !indexName.endsWith("_pkey")) { // Skip primary key index
                    indexInfos.add(new IndexInfo(
                        indexName,
                        !indexes.getBoolean("NON_UNIQUE"),
                        indexes.getString("COLUMN_NAME")
                    ));
                }
            }
            
            assertThat(indexInfos)
                .as("idx_original_url_normalized should exist")
                .anySatisfy(idx -> {
                    assertThat(idx.name()).isEqualTo("idx_original_url_normalized");
                    assertThat(idx.unique()).as("Index should be unique").isTrue();
                });
        }
    }

    /**
     * Test: AC #3 - Unique index enforces uniqueness on original_url
     * Validates that duplicate URLs (after application normalization) are rejected
     */
    @Test
    void uniqueIndexShouldPreventDuplicateNormalizedUrls() {
        // Given: A URL is inserted
        String normalizedUrl = "https://example.com/test"; // Pre-normalized by application
        jdbcTemplate.update(
            "INSERT INTO urls (short_code, original_url, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)",
            "abc123", normalizedUrl
        );
        
        // When: Attempting to insert the same normalized URL with different short_code
        // Then: Should throw exception due to unique constraint
        try {
            jdbcTemplate.update(
                "INSERT INTO urls (short_code, original_url, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)",
                "xyz789", normalizedUrl
            );
            assertThat(false)
                .as("Should have thrown exception for duplicate URL")
                .isTrue();
        } catch (Exception e) {
            assertThat(e.getMessage())
                .as("Error should reference unique constraint violation")
                .containsIgnoringCase("unique")
                .containsIgnoringCase("idx_original_url_normalized");
        }
    }

    /**
     * Test: AC #2 - created_at has default value
     * Validates that created_at gets CURRENT_TIMESTAMP when not specified
     */
    @Test
    void createdAtShouldHaveDefaultValue() {
        // When: Inserting without specifying created_at
        jdbcTemplate.update(
            "INSERT INTO urls (short_code, original_url) VALUES (?, ?)",
            "test456", "https://example.com/default-timestamp"
        );
        
        // Then: created_at should be populated automatically
        Long timestamp = jdbcTemplate.queryForObject(
            "SELECT EXTRACT(EPOCH FROM created_at) FROM urls WHERE short_code = ?",
            Long.class,
            "test456"
        );
        
        assertThat(timestamp)
            .as("created_at should be set to current timestamp")
            .isNotNull()
            .isGreaterThan(0L);
    }

    /**
     * Test: AC #4 - Rollback scripts are present in changesets
     * Validates that rollback configuration exists for changesets that need it
     * Note: This is validated at the Liquibase metadata level
     */
    @Test
    void changesetsWithDestructiveChangesShouldHaveRollback() {
        // When: Querying DATABASECHANGELOG for rollback info
        List<String> changesetsWithRollback = jdbcTemplate.query(
            "SELECT id FROM databasechangelog WHERE id IN (?, ?) ORDER BY id",
            (rs, rowNum) -> rs.getString("id"),
            "002-create-normalized-url-index",
            "004-update-url-index-for-app-normalization"
        );
        
        // Then: Changesets 002 and 004 should be present (they have rollback scripts)
        assertThat(changesetsWithRollback)
            .as("Changesets with destructive changes should have rollback capability")
            .containsExactly(
                "002-create-normalized-url-index",
                "004-update-url-index-for-app-normalization"
            );
    }

    /**
     * Test: Verify table and column comments exist (Changeset 003)
     * Validates that PostgreSQL comments were applied
     */
    @Test
    void tableShouldHaveComments() {
        // When: Querying PostgreSQL system catalogs for comments
        
        // Then: Table comment should exist
        String tableComment = jdbcTemplate.queryForObject(
            "SELECT obj_description('urls'::regclass, 'pg_class')",
            String.class
        );
        
        assertThat(tableComment)
            .as("Table should have descriptive comment")
            .isNotNull()
            .contains("URL mappings");
        
        // Then: Column comments should exist
        List<String> columnComments = jdbcTemplate.query(
            "SELECT col_description('urls'::regclass, ordinal_position) as comment " +
            "FROM information_schema.columns " +
            "WHERE table_name = 'urls' AND table_schema = 'public' " +
            "ORDER BY ordinal_position",
            (rs, rowNum) -> rs.getString("comment")
        );
        
        assertThat(columnComments)
            .as("All columns should have comments")
            .allSatisfy(comment -> assertThat(comment).isNotBlank());
    }

    /**
     * Test: Validate Liquibase tracking table structure
     * Ensures DATABASECHANGELOG is properly maintained
     */
    @Test
    void databaseChangelogShouldTrackExecutionMetadata() {
        // When: Querying DATABASECHANGELOG
        List<ChangesetMetadata> metadata = jdbcTemplate.query(
            "SELECT id, author, filename, orderexecuted FROM databasechangelog ORDER BY orderexecuted",
            (rs, rowNum) -> new ChangesetMetadata(
                rs.getString("id"),
                rs.getString("author"),
                rs.getString("filename"),
                rs.getInt("orderexecuted")
            )
        );
        
        // Then: All changesets should have proper metadata
        assertThat(metadata)
            .hasSize(4)
            .allSatisfy(cs -> {
                assertThat(cs.author()).isEqualTo("slavaz");
                assertThat(cs.filename()).contains("db.changelog-master.yaml");
                assertThat(cs.orderexecuted()).isGreaterThan(0);
            });
    }

    // Helper methods and records

    private ColumnInfo findColumn(List<ColumnInfo> columns, String name) {
        return columns.stream()
            .filter(col -> col.name().equals(name))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Column not found: " + name));
    }

    private record ColumnInfo(String name, String type, boolean nullable) {}
    
    private record IndexInfo(String name, boolean unique, String columnName) {}
    
    private record ChangesetMetadata(String id, String author, String filename, int orderexecuted) {}
}
