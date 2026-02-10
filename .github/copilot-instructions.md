# GitHub Copilot Instructions - URL Shortener Project

```xml
<!--
  GitHub Copilot Instructions for URL Shortener Service
  Version: 1.0.0
  Model Target: GPT-5 mini (optimized for token efficiency)
  Last Updated: 2026-02-10
  Framework: BMAD v6.0.0-Beta.7
  
  PROMPT ENGINEERING NOTES:
  - XML structure enables hierarchical instruction inheritance
  - Child elements inherit constraints from parent elements
  - priority="critical" means MUST follow, priority="high" means SHOULD follow
  - applies-to attribute scopes instruction context
  - Use few-shot examples for complex patterns
  - Optimized for GPT-5 mini: concise, explicit, structured
-->

<copilot-instructions 
  project="url-shortener" 
  version="1.0.0"
  language="Java 21"
  framework="Spring Boot 3.2.2">

<!-- ============================================================================
     SECTION 0: META-INSTRUCTIONS
     How Copilot should interpret and apply these instructions
     ============================================================================ -->

<meta-instructions>
  <interpretation priority="critical">
    <rule>Read this file hierarchically: child elements inherit parent constraints</rule>
    <rule>priority="critical" means MUST follow (violations break builds/tests)</rule>
    <rule>priority="high" means SHOULD follow (best practices, not blockers)</rule>
    <rule>When multiple instructions conflict, follow the most specific (child over parent)</rule>
    <rule>Code examples show CORRECT patterns - never generate anti-patterns</rule>
  </interpretation>
  
  <optimization model="gpt-5-mini">
    <constraint>Token efficiency: Be concise while maintaining clarity</constraint>
    <constraint>Explicit over implicit: State requirements clearly</constraint>
    <constraint>Examples first: Show pattern, then explain</constraint>
    <constraint>Structured output: Use hierarchical thinking</constraint>
  </optimization>
  
  <bmad-integration priority="high">
    <principle>When tasks are complex (>30min) or require research, suggest BMAD agents</principle>
    <principle>BMAD agents are specialists - use them for their expertise</principle>
    <principle>This project uses BMAD v6.0.0-Beta.7 with BMM module installed</principle>
  </bmad-integration>
</meta-instructions>

<!-- ============================================================================
     SECTION 1: PROJECT OVERVIEW
     Core philosophy and base rules inherited by all code generation
     ============================================================================ -->

<project-overview>
  <identity>
    <name>URL Shortener Service</name>
    <philosophy>"HashMap-via-REST" - A persistent key-value store via HTTP</philosophy>
    <mission>Transform long URLs into short, shareable links with guaranteed consistency</mission>
    <approach>Do one thing exceptionally well with production-grade patterns</approach>
  </identity>
  
  <core-principles priority="critical">
    <!-- ALL code generation MUST follow these principles -->
    <principle id="collision-free">Use Snowflake IDs (Base62 encoded) - NEVER random/UUID</principle>
    <principle id="idempotency">Same URL → same short code (database-enforced via UNIQUE constraint)</principle>
    <principle id="clean-arch">Layered architecture: Controller → Service → Repository → Database</principle>
    <principle id="no-hibernate-ddl">Database changes ONLY via Liquibase - NEVER Hibernate DDL</principle>
    <principle id="test-coverage">All code MUST have tests - minimum 80% coverage (JaCoCo enforced)</principle>
  </core-principles>
  
  <documentation>
    <location>See README.md for comprehensive project overview</location>
    <location>See docs/INDEX.md for documentation index</location>
    <location>See docs/DEVELOPMENT.md for implementation examples</location>
  </documentation>
</project-overview>

<!-- ============================================================================
     SECTION 2: TECHNOLOGY STACK
     Technologies and versions with usage patterns
     ============================================================================ -->

<technology-stack>
  <language name="Java" version="21" priority="critical">
    <feature name="records" when-to-use="DTOs, immutable data classes">
      <example>
        // ✅ CORRECT - Use records for DTOs
        public record ShortenRequest(
            @NotBlank(message = "URL is required")
            @Size(max = 2048, message = "URL too long")
            String url
        ) {}
      </example>
      <constraint>Records are IMMUTABLE - use for DTOs, not entities</constraint>
    </feature>
    
    <feature name="text-blocks" when-to-use="SQL queries, JSON, multi-line strings">
      <example>
        String sql = """
            SELECT short_code, original_url
            FROM urls
            WHERE short_code = ?
            """;
      </example>
    </feature>
    
    <feature name="pattern-matching" when-to-use="Type checking, optional handling">
      <example>
        if (entity instanceof UrlEntity url) {
            return url.getShortCode();
        }
      </example>
    </feature>
    
    <constraint priority="critical">Use Java 21 syntax - NO Java 8 patterns (no Optional.orElse abuse)</constraint>
  </language>
  
  <framework name="Spring Boot" version="3.2.2" priority="critical">
    <pattern name="dependency-injection" applies-to="all-components">
      <rule>ALWAYS use constructor injection - NEVER field injection (@Autowired fields)</rule>
      <example>
        // ✅ CORRECT - Constructor injection
        @Service
        public class UrlShortenerServiceImpl implements UrlShortenerService {
            private final UrlRepository repository;
            private final SnowflakeIdGenerator generator;
            
            public UrlShortenerServiceImpl(UrlRepository repository, 
                                          SnowflakeIdGenerator generator) {
                this.repository = repository;
                this.generator = generator;
            }
        }
        
        // ❌ WRONG - Field injection
        @Service
        public class BadService {
            @Autowired private UrlRepository repository; // NEVER do this
        }
      </example>
    </pattern>
    
    <pattern name="transactions" applies-to="service-layer">
      <rule>Use @Transactional on service methods, NOT repository methods</rule>
      <rule>Propagation.REQUIRES_NEW for independent transactions (idempotency pattern)</rule>
      <example>
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public ShortenResponse tryInsert(String normalizedUrl) {
            // Independent transaction - will commit/rollback separately
        }
      </example>
    </pattern>
    
    <pattern name="validation" applies-to="controllers">
      <rule>Use @Valid on @RequestBody for automatic validation</rule>
      <rule>Define validation in DTO records, not controller methods</rule>
      <example>
        @PostMapping("/api/shorten")
        public ResponseEntity&lt;ShortenResponse&gt; shortenUrl(
                @Valid @RequestBody ShortenRequest request) {
            // @Valid triggers validation defined in ShortenRequest record
        }
      </example>
    </pattern>
    
    <library name="Spring Data JPA" version="3.2.2">
      <rule>Repository interfaces extend JpaRepository&lt;Entity, ID&gt;</rule>
      <rule>Use query methods for simple queries: findByFieldName, existsByFieldName</rule>
      <rule>Use @Query for complex queries with JPQL or native SQL</rule>
    </library>
  </framework>
  
  <database name="PostgreSQL" version="16" priority="critical">
    <constraint>ALL schema changes via Liquibase changesets in src/main/resources/db/changelog/</constraint>
    <constraint>NEVER use spring.jpa.hibernate.ddl-auto=update (ALWAYS set to validate)</constraint>
    <constraint>Use TEXT for URLs (unlimited length), VARCHAR for codes (limited length)</constraint>
    <constraint>Use UNIQUE constraints for idempotency enforcement</constraint>
    <constraint>Use B-tree indexes on lookup columns (short_code, normalized_url)</constraint>
  </database>
  
  <migration name="Liquibase" version="4.25" priority="critical">
    <rule>Create new changeset file for EACH schema change</rule>
    <rule>Never modify existing changesets (they've been applied in production)</rule>
    <rule>Use SQL format for clarity: src/main/resources/db/changelog/changes/*.sql</rule>
    <rule>Reference in db.changelog-master.yaml with include tag</rule>
    <example>
      <!-- db.changelog-master.yaml -->
      databaseChangeLog:
        - include:
            file: db/changelog/001-create-urls-table.sql
        - include:
            file: db/changelog/002-add-index-on-normalized-url.sql
    </example>
  </migration>
  
  <testing-stack>
    <framework name="JUnit 5" when-to-use="All tests">
      <rule>Use @ExtendWith(MockitoExtension.class) for unit tests</rule>
      <rule>Use @SpringBootTest for integration tests</rule>
      <rule>Test class location: src/test/java/ (mirror src/main/java/ structure)</rule>
    </framework>
    
    <framework name="Testcontainers" when-to-use="Integration tests with database">
      <rule>Use Testcontainers for PostgreSQL in integration tests</rule>
      <rule>Extend AbstractIntegrationTest base class for shared setup</rule>
      <example>
        @SpringBootTest
        @Testcontainers
        class UrlRepositoryTest {
            @Container
            static PostgreSQLContainer&lt;?&gt; postgres = 
                new PostgreSQLContainer&lt;&gt;("postgres:16-alpine");
        }
      </example>
    </framework>
    
    <coverage name="JaCoCo" minimum="80%">
      <rule>Minimum 80% line coverage enforced by Maven build</rule>
      <rule>Run: mvn clean test jacoco:report</rule>
      <rule>View: target/site/jacoco/index.html</rule>
    </coverage>
  </testing-stack>
  
  <deployment name="Docker" priority="high">
    <service name="postgres">Database service (PostgreSQL 16)</service>
    <service name="liquibase">Migration service (runs once, applies changesets)</service>
    <service name="app">Spring Boot application</service>
    <command name="start">docker-compose up --build</command>
    <command name="stop">docker-compose down</command>
    <command name="logs">docker-compose logs -f app</command>
    <command name="db-shell">docker exec -it url-shortener-db psql -U urlshortener -d urlshortener</command>
  </deployment>
</technology-stack>

<!-- ============================================================================
     SECTION 3: ARCHITECTURE & PATTERNS
     Layered architecture with pattern inheritance
     ============================================================================ -->

<architecture>
  <layers priority="critical">
    <!-- Dependency rule: Each layer can ONLY depend on layers below it -->
    <layer name="controller" depends-on="service">
      <responsibility>Handle HTTP requests/responses, validation, URL building</responsibility>
      <must-do>
        <rule>Use @RestController annotation</rule>
        <rule>Use @RequestMapping for base path</rule>
        <rule>Use @Valid for request validation</rule>
        <rule>Return ResponseEntity&lt;T&gt; for explicit HTTP status</rule>
        <rule>Delegate business logic to service layer - NO logic in controllers</rule>
      </must-do>
      <must-not>
        <rule>NEVER access repository directly - ALWAYS through service</rule>
        <rule>NEVER implement business logic - controllers are thin</rule>
        <rule>NEVER handle transactions - services handle transactions</rule>
      </must-not>
    </layer>
    
    <layer name="service" depends-on="repository">
      <responsibility>Business logic, transactions, orchestration</responsibility>
      <must-do>
        <rule>Use @Service annotation</rule>
        <rule>Use @Transactional for transaction boundaries</rule>
        <rule>Implement interface for testability</rule>
        <rule>Validate business rules (not just input validation)</rule>
      </must-do>
      <must-not>
        <rule>NEVER return entities directly - convert to DTOs</rule>
        <rule>NEVER expose persistence exceptions - wrap in custom exceptions</rule>
      </must-not>
    </layer>
    
    <layer name="repository" depends-on="entity">
      <responsibility>Data access, queries, persistence</responsibility>
      <must-do>
        <rule>Interface extends JpaRepository&lt;Entity, ID&gt;</rule>
        <rule>Use @Repository annotation (optional with Spring Data JPA)</rule>
        <rule>Use query methods or @Query for custom queries</rule>
      </must-do>
      <must-not>
        <rule>NEVER implement business logic in repositories</rule>
        <rule>NEVER return custom DTOs - return entities or projections</rule>
      </must-not>
    </layer>
    
    <layer name="entity" depends-on="none">
      <responsibility>JPA entities, database mapping</responsibility>
      <must-do>
        <rule>Use @Entity annotation</rule>
        <rule>Use @Table(name="table_name") to specify table name</rule>
        <rule>Use @Column(name="column_name") to specify column names</rule>
        <rule>Use Lombok (@Data, @NoArgsConstructor, @AllArgsConstructor) for boilerplate</rule>
      </must-do>
      <must-not>
        <rule>NEVER include business logic in entities</rule>
        <rule>NEVER use @Transactional on entities</rule>
      </must-not>
    </layer>
  </layers>
  
  <package-structure>
    <base-package>com.example.urlshortener</base-package>
    <package name="controller">REST controllers (ShortenController, RedirectController)</package>
    <package name="service">Service interfaces and implementations</package>
    <package name="repository">Spring Data JPA repositories</package>
    <package name="entity">JPA entities (UrlEntity)</package>
    <package name="dto">Data Transfer Objects (ShortenRequest, ShortenResponse, ErrorResponse)</package>
    <package name="generator">Snowflake ID generator (SnowflakeIdGenerator, Base62Encoder)</package>
    <package name="exception">Custom exceptions and @ControllerAdvice</package>
    <package name="config">Configuration classes (if needed)</package>
  </package-structure>
  
  <pattern name="snowflake-id" priority="critical">
    <description>64-bit time-sortable IDs encoded in Base62 for collision-free short codes</description>
    <structure>
      [41 bits timestamp] | [10 bits instance ID] | [13 bits sequence]
    </structure>
    <implementation>
      <class>com.example.urlshortener.generator.SnowflakeIdGenerator</class>
      <method>generateShortCode() → returns Base62 string (7 chars typical)</method>
    </implementation>
    <must-do>
      <rule>ALWAYS use SnowflakeIdGenerator.generateShortCode() for new URLs</rule>
      <rule>NEVER use UUID.randomUUID() or Random - not collision-free</rule>
      <rule>Generator is @Component singleton - inject via constructor</rule>
    </must-do>
    <example>
      @Service
      public class UrlShortenerServiceImpl {
          private final SnowflakeIdGenerator generator;
          
          public UrlShortenerServiceImpl(SnowflakeIdGenerator generator) {
              this.generator = generator;
          }
          
          public ShortenResponse shortenUrl(String url) {
              String shortCode = generator.generateShortCode(); // ✅ CORRECT
              // ... save to database
          }
      }
    </example>
  </pattern>
  
  <pattern name="database-enforced-idempotency" priority="critical">
    <description>Same URL always returns same short code via UNIQUE constraint + try-insert-catch-select</description>
    <database-constraint>
      UNIQUE INDEX idx_urls_normalized_url ON urls(normalized_url);
    </database-constraint>
    <implementation-flow>
      1. Normalize URL (trim, lowercase scheme/host)
      2. Try to INSERT new mapping with generated short code
      3. If DataIntegrityViolationException (UNIQUE constraint violation):
         - Catch exception
         - SELECT existing mapping for normalized URL
         - Return existing short code
      4. Else: Return new short code
    </implementation-flow>
    <must-do>
      <rule>ALWAYS normalize URL before checking/inserting</rule>
      <rule>Use @Transactional(propagation = Propagation.REQUIRES_NEW) for tryInsert</rule>
      <rule>Use repository.flush() after save to force constraint check</rule>
      <rule>Catch DataIntegrityViolationException, then query for existing</rule>
    </must-do>
    <example>
      // See UrlShortenerServiceImpl.shortenUrl() for full implementation
      @Override
      public ShortenResponse shortenUrl(String url) {
          String normalized = normalizeUrl(url);
          try {
              return tryInsert(normalized); // New transaction
          } catch (DataIntegrityViolationException e) {
              return findExisting(normalized); // New read-only transaction
          }
      }
    </example>
  </pattern>
  
  <pattern name="http-301-redirect" priority="high">
    <description>Permanent redirects (301) for URL shortener responses</description>
    <rule>Use HTTP 301 (Moved Permanently) for redirects - NOT 302</rule>
    <rule>Browsers cache 301 redirects - improves performance</rule>
    <example>
      @GetMapping("/{shortCode}")
      public ResponseEntity&lt;Void&gt; redirect(@PathVariable String shortCode) {
          String originalUrl = service.getOriginalUrl(shortCode);
          return ResponseEntity
              .status(HttpStatus.MOVED_PERMANENTLY) // 301
              .location(URI.create(originalUrl))
              .build();
      }
    </example>
  </pattern>
</architecture>

<!-- ============================================================================
     SECTION 4: CODING STANDARDS
     Specific conventions with inheritance from core principles
     ============================================================================ -->

<coding-standards priority="critical">
  <java-conventions>
    <naming>
      <rule>Classes: PascalCase (UrlShortenerService)</rule>
      <rule>Methods: camelCase (generateShortCode)</rule>
      <rule>Constants: UPPER_SNAKE_CASE (MAX_URL_LENGTH)</rule>
      <rule>Packages: lowercase (com.example.urlshortener)</rule>
    </naming>
    
    <records-vs-lombok>
      <rule>Use records for DTOs (immutable, public data)</rule>
      <rule>Use Lombok @Data for JPA entities (need setters, JPA requires no-arg constructor)</rule>
      <anti-pattern>NEVER use Lombok @Data on records (redundant)</anti-pattern>
    </records-vs-lombok>
    
    <constructor-injection priority="critical">
      <rule>ALWAYS use constructor injection for dependencies</rule>
      <rule>NEVER use @Autowired on fields</rule>
      <reason>Constructor injection ensures immutability, testability, explicit dependencies</reason>
    </constructor-injection>
  </java-conventions>
  
  <spring-boot-conventions>
    <controller-naming>
      <rule>Name: {Entity}Controller (e.g., ShortenController, RedirectController)</rule>
      <rule>Annotation: @RestController</rule>
      <rule>Base path: @RequestMapping("/api") or @RequestMapping("/")</rule>
    </controller-naming>
    
    <service-naming>
      <rule>Interface: {Entity}Service (e.g., UrlShortenerService)</rule>
      <rule>Implementation: {Entity}ServiceImpl (e.g., UrlShortenerServiceImpl)</rule>
      <rule>Annotation: @Service (on implementation)</rule>
    </service-naming>
    
    <repository-naming>
      <rule>Name: {Entity}Repository (e.g., UrlRepository)</rule>
      <rule>Extends: JpaRepository&lt;UrlEntity, String&gt;</rule>
      <rule>Annotation: @Repository (optional with Spring Data JPA)</rule>
    </repository-naming>
  </spring-boot-conventions>
  
  <error-handling priority="critical">
    <global-exception-handler>
      <rule>Use @RestControllerAdvice for centralized exception handling</rule>
      <rule>Use @ExceptionHandler methods for specific exceptions</rule>
      <rule>Return ErrorResponse DTO with status, message, timestamp</rule>
      <example>
        @RestControllerAdvice
        public class GlobalExceptionHandler {
            @ExceptionHandler(ShortCodeNotFoundException.class)
            public ResponseEntity&lt;ErrorResponse&gt; handleNotFound(
                    ShortCodeNotFoundException ex) {
                ErrorResponse error = new ErrorResponse(
                    HttpStatus.NOT_FOUND.value(),
                    ex.getMessage(),
                    Instant.now()
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
        }
      </example>
    </global-exception-handler>
    
    <custom-exceptions>
      <rule>Create custom exceptions for domain errors</rule>
      <rule>Extend RuntimeException (unchecked exceptions)</rule>
      <rule>Include meaningful messages</rule>
      <example>
        public class ShortCodeNotFoundException extends RuntimeException {
            public ShortCodeNotFoundException(String shortCode) {
                super("Short code not found: " + shortCode);
            }
        }
      </example>
    </custom-exceptions>
  </error-handling>
  
  <testing-conventions priority="critical">
    <location>
      <rule>Unit tests: src/test/java/{package}/{Class}Test.java</rule>
      <rule>Integration tests: src/test/java/{package}/{Class}IntegrationTest.java</rule>
    </location>
    
    <naming>
      <rule>Test class: {Class}Test or {Class}IntegrationTest</rule>
      <rule>Test method: testMethodName_givenCondition_thenExpectedBehavior</rule>
      <example>
        @Test
        void shortenUrl_givenValidUrl_thenReturnsShortCode() {
            // Arrange, Act, Assert
        }
      </example>
    </naming>
    
    <structure>
      <rule>Use Arrange-Act-Assert (AAA) pattern</rule>
      <rule>One assertion per test (or related assertions)</rule>
      <rule>Use AssertJ assertions (assertThat)</rule>
    </structure>
    
    <coverage>
      <rule>Minimum 80% line coverage (enforced by JaCoCo)</rule>
      <rule>Test happy path, edge cases, error cases</rule>
      <rule>Test boundary conditions (null, empty, max length)</rule>
    </coverage>
  </testing-conventions>
  
  <database-rules priority="critical">
    <constraint>NEVER use spring.jpa.hibernate.ddl-auto=update or create-drop</constraint>
    <constraint>ALWAYS use Liquibase for schema changes</constraint>
    <constraint>NEVER modify entity @Column definitions without Liquibase changeset</constraint>
    <workflow>
      1. Create Liquibase changeset (.sql file)
      2. Update entity @Column if needed
      3. Test migration with: docker-compose up liquibase
      4. Verify schema with: docker exec -it url-shortener-db psql ...
    </workflow>
  </database-rules>
</coding-standards>

<!-- ============================================================================
     SECTION 5: BUILD & RUN COMMANDS
     Common commands for development workflow
     ============================================================================ -->

<build-and-run-commands>
  <maven>
    <command name="clean-install">mvn clean install</command>
    <command name="test">mvn test</command>
    <command name="test-with-coverage">mvn clean test jacoco:report</command>
    <command name="view-coverage">open target/site/jacoco/index.html</command>
    <command name="run-locally">mvn spring-boot:run</command>
    <command name="package">mvn clean package (creates JAR in target/)</command>
  </maven>
  
  <docker>
    <command name="start-all-services">docker-compose up --build</command>
    <command name="start-in-background">docker-compose up -d --build</command>
    <command name="stop-all-services">docker-compose down</command>
    <command name="view-logs">docker-compose logs -f app</command>
    <command name="view-postgres-logs">docker-compose logs -f postgres</command>
    <command name="restart-app">docker-compose restart app</command>
  </docker>
  
  <database-access>
    <command name="postgres-shell">docker exec -it url-shortener-db psql -U urlshortener -d urlshortener</command>
    <command name="list-tables">\dt (inside psql)</command>
    <command name="describe-table">\d urls (inside psql)</command>
    <command name="query-urls">SELECT * FROM urls LIMIT 10;</command>
  </database-access>
  
  <troubleshooting>
    <command name="clean-docker">docker-compose down -v (removes volumes)</command>
    <command name="rebuild-without-cache">docker-compose build --no-cache</command>
    <command name="check-container-status">docker-compose ps</command>
    <command name="inspect-network">docker network inspect url-shortener-network</command>
  </troubleshooting>
</build-and-run-commands>

<!-- ============================================================================
     SECTION 6: BMAD FRAMEWORK INTEGRATION
     BMAD workflow and agent usage patterns
     ============================================================================ -->

<bmad-framework version="6.0.0-Beta.7" priority="high">
  <overview>
    <what>BMAD (Brainstorming, Modeling, Analysis, Development) - AI-powered software development framework</what>
    <why>Accelerates development with specialized AI agents for different SDLC phases</why>
    <how>Workflow-driven agent orchestration with structured artifacts</how>
    <philosophy>Each agent is a domain expert - use them like consulting specialists</philosophy>
  </overview>
  
  <directory-structure>
    <dir name="_bmad/core/">Core BMAD framework files (tasks, workflows, prompts)</dir>
    <dir name="_bmad/bmm/">BMAD Module Manager - project-specific agents and configuration</dir>
    <dir name="_bmad/bmm/agents/">Available agents (dev.md, analyst.md, architect.md, etc.)</dir>
    <dir name="_bmad/bmm/workflows/">Project workflows (dev-story, code-review, etc.)</dir>
    <dir name="_bmad/bmm/config.yaml">Project configuration (user_name, output paths)</dir>
    <dir name="_bmad-output/">Generated artifacts (planning, implementation, analysis)</dir>
  </directory-structure>
  
  <configuration-variables>
    <var name="{project-root}">/home/slavaz/projects/godel_course_copilot/copilot_task</var>
    <var name="{user_name}">Slavaz (from config.yaml)</var>
    <var name="{communication_language}">English</var>
    <var name="{output_folder}">{project-root}/_bmad-output</var>
    <var name="{planning_artifacts}">{project-root}/_bmad-output/planning-artifacts</var>
    <var name="{implementation_artifacts}">{project-root}/_bmad-output/implementation-artifacts</var>
  </configuration-variables>
  
  <available-agents>
    <agent name="bmad-master" role="Orchestrator">
      <when-to-use>Complex multi-phase projects requiring coordination across agents</when-to-use>
      <capabilities>Task decomposition, agent delegation, progress tracking</capabilities>
      <example>Use for: "Plan and execute Epic 5: Add URL expiration feature"</example>
    </agent>
    
    <agent name="analyst" role="Business Analyst">
      <when-to-use>Requirements gathering, user story creation, acceptance criteria</when-to-use>
      <capabilities>Requirements analysis, story writing, AC definition</capabilities>
      <example>Use for: "Create user stories for URL analytics dashboard"</example>
    </agent>
    
    <agent name="architect" role="Solution Architect">
      <when-to-use>Design decisions, architecture patterns, technology choices</when-to-use>
      <capabilities>Architecture design, TDRs, pattern selection, scalability planning</capabilities>
      <example>Use for: "Design Redis caching layer for high-traffic endpoints"</example>
    </agent>
    
    <agent name="dev" role="Developer">
      <when-to-use>Implementing stories with tests, following story file tasks/subtasks</when-to-use>
      <capabilities>Code generation, test writing, strict story adherence</capabilities>
      <example>Use for: "Implement story-url-expiration.md"</example>
      <constraint priority="critical">Dev agent MUST follow story file tasks/subtasks in order</constraint>
    </agent>
    
    <agent name="qa" role="QA Engineer">
      <when-to-use>Test strategy, test plan creation, testing best practices</when-to-use>
      <capabilities>Test planning, coverage analysis, integration test design</capabilities>
      <example>Use for: "Create comprehensive test plan for idempotency edge cases"</example>
    </agent>
    
    <agent name="tech-writer" role="Technical Writer">
      <when-to-use>Documentation creation/updates, API documentation, guides</when-to-use>
      <capabilities>Documentation writing, README updates, developer guides</capabilities>
      <example>Use for: "Update API documentation with new endpoints"</example>
    </agent>
    
    <agent name="ux-designer" role="UX Designer">
      <when-to-use>User experience design, UI patterns, user flows</when-to-use>
      <capabilities>UX design, user flow diagrams, interaction patterns</capabilities>
      <example>Use for: "Design admin dashboard UI for URL management"</example>
    </agent>
    
    <agent name="quick-flow-solo-dev" role="Full-Stack Solo Developer">
      <when-to-use>Quick prototypes, small features, experiments (not production code)</when-to-use>
      <capabilities>Rapid end-to-end implementation without formal stories</capabilities>
      <example>Use for: "Prototype URL QR code generation endpoint"</example>
    </agent>
  </available-agents>
  
  <workflow-execution>
    <command name="activate-agent">Type agent command (e.g., "DS" for dev-story)</command>
    <command name="help">/bmad-help [optional context]</command>
    <command name="agent-menu">Shows numbered menu after activation</command>
    <pattern>
      1. User activates agent (e.g., "DS")
      2. Agent displays menu with options
      3. User selects option by number or keyword
      4. Agent executes workflow
      5. Artifacts saved to _bmad-output/
    </pattern>
  </workflow-execution>
  
  <when-to-suggest-bmad>
    <scenario>Complex feature (>30min) → Suggest analyst + architect + dev workflow</scenario>
    <scenario>Architecture decision needed → Suggest architect agent</scenario>
    <scenario>Requirements unclear → Suggest analyst agent</scenario>
    <scenario>Story implementation → Suggest dev agent with "DS" command</scenario>
    <scenario>Documentation update → Suggest tech-writer agent</scenario>
    <example>
      User: "I need to add URL expiration feature"
      Copilot: "This is a complex feature. I suggest:
        1. Activate analyst agent to create story
        2. Activate architect for expiration strategy design
        3. Activate dev agent to implement story
      Or use bmad-master to orchestrate all phases."
    </example>
  </when-to-suggest-bmad>
</bmad-framework>

<!-- ============================================================================
     SECTION 7: CODE GENERATION GUIDELINES
     Task templates with step-by-step workflows
     ============================================================================ -->

<code-generation-rules priority="critical">
  <must-do>
    <rule priority="critical">Follow layered architecture: Controller → Service → Repository</rule>
    <rule priority="critical">Use constructor injection for dependencies</rule>
    <rule priority="critical">Write tests for every new method (unit + integration)</rule>
    <rule priority="critical">Use Liquibase for ALL database schema changes</rule>
    <rule priority="critical">Return DTOs from service layer, NOT entities</rule>
    <rule priority="critical">Use @Valid for request validation</rule>
    <rule priority="critical">Handle exceptions with @RestControllerAdvice</rule>
    <rule priority="critical">Use SnowflakeIdGenerator for short codes</rule>
    <rule priority="critical">Normalize URLs before storage/lookup</rule>
    <rule priority="critical">Use @Transactional on service methods</rule>
  </must-do>
  
  <must-not>
    <rule priority="critical">NEVER use field injection (@Autowired on fields)</rule>
    <rule priority="critical">NEVER access repository from controller</rule>
    <rule priority="critical">NEVER return entities from controllers</rule>
    <rule priority="critical">NEVER use Hibernate DDL (spring.jpa.hibernate.ddl-auto)</rule>
    <rule priority="critical">NEVER use UUID or Random for short codes</rule>
    <rule priority="critical">NEVER skip tests</rule>
    <rule priority="critical">NEVER expose stack traces in API responses</rule>
    <rule priority="critical">NEVER use @Transactional on repositories</rule>
    <rule priority="critical">NEVER modify existing Liquibase changesets</rule>
    <rule priority="critical">NEVER hardcode URLs or configuration</rule>
  </must-not>
  
  <task-template name="add-rest-endpoint" priority="high">
    <step n="1">Define DTO records for request/response (in dto/ package)</step>
    <step n="2">Add method to service interface</step>
    <step n="3">Implement method in service implementation with @Transactional</step>
    <step n="4">Add controller method with @GetMapping/@PostMapping/@PutMapping/@DeleteMapping</step>
    <step n="5">Add @ExceptionHandler in GlobalExceptionHandler for new exceptions</step>
    <step n="6">Write unit tests for service method (mock dependencies)</step>
    <step n="7">Write integration test for controller endpoint (with Testcontainers)</step>
    <step n="8">Update API documentation in README.md</step>
    <step n="9">Run mvn test to verify all tests pass</step>
    
    <example>
      // 1. DTO (dto/GetUrlDetailsRequest.java)
      public record GetUrlDetailsRequest(String shortCode) {}
      
      // 2. Service interface (service/UrlShortenerService.java)
      UrlDetails getUrlDetails(String shortCode);
      
      // 3. Service implementation (service/UrlShortenerServiceImpl.java)
      @Transactional(readOnly = true)
      public UrlDetails getUrlDetails(String shortCode) {
          UrlEntity entity = repository.findById(shortCode)
              .orElseThrow(() -> new ShortCodeNotFoundException(shortCode));
          return new UrlDetails(entity.getShortCode(), entity.getOriginalUrl(), entity.getCreatedAt());
      }
      
      // 4. Controller (controller/UrlDetailsController.java)
      @GetMapping("/api/details/{shortCode}")
      public ResponseEntity&lt;UrlDetails&gt; getDetails(@PathVariable String shortCode) {
          UrlDetails details = service.getUrlDetails(shortCode);
          return ResponseEntity.ok(details);
      }
      
      // 5-7. Tests omitted for brevity
    </example>
  </task-template>
  
  <task-template name="database-schema-change" priority="critical">
    <step n="1">Design schema change (new table, column, index, constraint)</step>
    <step n="2">Create Liquibase changeset SQL file in src/main/resources/db/changelog/changes/</step>
    <step n="3">Name file with sequence number: NNN-descriptive-name.sql</step>
    <step n="4">Write SQL in changeset (CREATE TABLE, ALTER TABLE, CREATE INDEX, etc.)</step>
    <step n="5">Add include to db.changelog-master.yaml</step>
    <step n="6">Update JPA entity if needed (@Column, @Table annotations)</step>
    <step n="7">Test migration: docker-compose up liquibase</step>
    <step n="8">Verify schema: docker exec -it url-shortener-db psql -U urlshortener -d urlshortener</step>
    <step n="9">Run \d table_name to verify changes</step>
    <step n="10">Write integration test to verify schema change</step>
    
    <example>
      -- File: src/main/resources/db/changelog/changes/003-add-expiration-column.sql
      --liquibase formatted sql
      
      --changeset slavaz:003-add-expiration-column
      ALTER TABLE urls ADD COLUMN expires_at TIMESTAMP;
      COMMENT ON COLUMN urls.expires_at IS 'Expiration timestamp for temporary URLs';
      
      --rollback ALTER TABLE urls DROP COLUMN expires_at;
    </example>
    
    <bmad-integration>
      For complex schema changes, consider using architect agent to design schema first
    </bmad-integration>
  </task-template>
  
  <task-template name="add-service-method" priority="high">
    <step n="1">Add method signature to service interface</step>
    <step n="2">Implement method in service implementation</step>
    <step n="3">Add @Transactional if method modifies data</step>
    <step n="4">Inject required dependencies via constructor</step>
    <step n="5">Handle errors with custom exceptions</step>
    <step n="6">Convert entities to DTOs before returning</step>
    <step n="7">Write unit test with mocked dependencies</step>
    <step n="8">Test happy path, edge cases, error cases</step>
    
    <example>
      // Interface
      public interface UrlShortenerService {
          boolean deleteShortCode(String shortCode);
      }
      
      // Implementation
      @Transactional
      public boolean deleteShortCode(String shortCode) {
          if (!repository.existsById(shortCode)) {
              throw new ShortCodeNotFoundException(shortCode);
          }
          repository.deleteById(shortCode);
          return true;
      }
    </example>
  </task-template>
  
  <task-template name="write-tests" priority="critical">
    <step n="1">Create test class in src/test/java/ mirroring src/main/java/ structure</step>
    <step n="2">Use @ExtendWith(MockitoExtension.class) for unit tests</step>
    <step n="3">Use @SpringBootTest for integration tests</step>
    <step n="4">Use @Mock for dependencies, @InjectMocks for class under test</step>
    <step n="5">Write test methods: testMethodName_givenCondition_thenExpectedBehavior</step>
    <step n="6">Use Arrange-Act-Assert pattern</step>
    <step n="7">Test happy path first</step>
    <step n="8">Test edge cases (null, empty, boundary values)</step>
    <step n="9">Test error cases (exceptions, invalid input)</step>
    <step n="10">Run mvn test to verify tests pass</step>
    <step n="11">Check coverage: mvn test jacoco:report</step>
    
    <example>
      @ExtendWith(MockitoExtension.class)
      class UrlShortenerServiceImplTest {
          @Mock private UrlRepository repository;
          @Mock private SnowflakeIdGenerator generator;
          @InjectMocks private UrlShortenerServiceImpl service;
          
          @Test
          void shortenUrl_givenValidUrl_thenReturnsShortCode() {
              // Arrange
              String url = "https://example.com";
              when(generator.generateShortCode()).thenReturn("abc123");
              when(repository.save(any())).thenReturn(new UrlEntity("abc123", url, Instant.now()));
              
              // Act
              ShortenResponse response = service.shortenUrl(url);
              
              // Assert
              assertThat(response.shortCode()).isEqualTo("abc123");
              verify(repository).save(any(UrlEntity.class));
          }
      }
    </example>
  </task-template>
</code-generation-rules>

<!-- ============================================================================
     SECTION 8: API REFERENCE
     Current API endpoints and contracts
     ============================================================================ -->

<api-reference>
  <endpoint method="POST" path="/api/shorten">
    <description>Shorten a long URL</description>
    <request>
      <content-type>application/json</content-type>
      <body>
        {
          "url": "https://example.com/very/long/url/path"
        }
      </body>
      <validation>
        <rule>url: required, not blank, max 2048 chars</rule>
        <rule>url: must be valid HTTP/HTTPS URL</rule>
        <rule>url: no user credentials allowed</rule>
      </validation>
    </request>
    <response status="200">
      <content-type>application/json</content-type>
      <body>
        {
          "shortCode": "8M0kX",
          "shortUrl": "http://localhost:3000/8M0kX"
        }
      </body>
    </response>
    <business-rules>
      <rule>Same URL always returns same short code (idempotent)</rule>
      <rule>URLs are normalized before storage (lowercase scheme/host)</rule>
      <rule>Short code generated using Snowflake ID + Base62 encoding</rule>
    </business-rules>
    <example>
      curl -X POST http://localhost:3000/api/shorten \
        -H "Content-Type: application/json" \
        -d '{"url": "https://github.com/spring-projects/spring-boot"}'
    </example>
  </endpoint>
  
  <endpoint method="GET" path="/{shortCode}">
    <description>Redirect to original URL</description>
    <path-parameter name="shortCode">The short code (e.g., "8M0kX")</path-parameter>
    <response status="301">
      <description>Permanent redirect to original URL</description>
      <header name="Location">Original URL</header>
    </response>
    <response status="404">
      <description>Short code not found</description>
      <body>
        {
          "status": 404,
          "message": "Short code not found: xyz123",
          "timestamp": "2026-02-10T10:30:00Z"
        }
      </body>
    </response>
    <example>
      curl -L http://localhost:3000/8M0kX
      # -L flag follows redirect
    </example>
  </endpoint>
</api-reference>

<!-- ============================================================================
     SECTION 9: PROJECT DOCUMENTATION
     Quick reference to project documentation
     ============================================================================ -->

<project-documentation>
  <doc name="README.md" path="/README.md">
    <description>Main project documentation</description>
    <contains>Quick start, API reference, architecture, testing, Docker commands</contains>
  </doc>
  
  <doc name="DEVELOPMENT.md" path="/docs/DEVELOPMENT.md">
    <description>Implementation guide with code examples</description>
    <contains>Snowflake generator, JPA entities, services, controllers, testing examples</contains>
  </doc>
  
  <doc name="INDEX.md" path="/docs/INDEX.md">
    <description>Documentation index and navigation guide</description>
    <contains>All documentation organized by topic and task</contains>
  </doc>
  
  <doc name="architecture.md" path="/_bmad-output/planning-artifacts/architecture.md">
    <description>Comprehensive architecture documentation</description>
    <contains>System overview, TDRs, component architecture, deployment architecture</contains>
  </doc>
  
  <doc name="PRD.md" path="/_bmad-output/planning-artifacts/PRD.md">
    <description>Product Requirements Document</description>
    <contains>Requirements, scope, acceptance criteria, out-of-scope features</contains>
  </doc>
  
  <doc name="DATABASE_SCHEMA_DESIGN.md" path="/docs/DATABASE_SCHEMA_DESIGN.md">
    <description>Database schema design and rationale</description>
    <contains>Table structures, indexes, constraints, design decisions</contains>
  </doc>
  
  <doc name="LIQUIBASE_MIGRATION_GUIDE.md" path="/docs/LIQUIBASE_MIGRATION_GUIDE.md">
    <description>Database migration guide</description>
    <contains>Liquibase usage, changeset creation, migration workflow</contains>
  </doc>
  
  <doc name="JPA_ENTITY_MAPPING_GUIDE.md" path="/docs/JPA_ENTITY_MAPPING_GUIDE.md">
    <description>JPA entity implementation guide</description>
    <contains>Entity annotations, column mappings, relationships</contains>
  </doc>
</project-documentation>

<!-- ============================================================================
     SECTION 10: OUT OF SCOPE
     Features deferred to v2.0+ (DO NOT implement in v1.0)
     ============================================================================ -->

<out-of-scope version="1.0">
  <feature name="URL expiration" deferred-to="v2.0">
    <reason>MVP focuses on core shortening - expiration adds complexity</reason>
  </feature>
  
  <feature name="Custom short codes" deferred-to="v2.0">
    <reason>User-defined codes require collision handling, validation</reason>
  </feature>
  
  <feature name="Analytics" deferred-to="v2.0">
    <reason>Click tracking, geo-location require additional infrastructure</reason>
  </feature>
  
  <feature name="Rate limiting" deferred-to="v2.0">
    <reason>Production deployment concern, not MVP</reason>
  </feature>
  
  <feature name="Caching (Redis)" deferred-to="v2.0">
    <reason>PostgreSQL performance sufficient for MVP scale</reason>
  </feature>
  
  <feature name="URL validation (malware check)" deferred-to="v2.0">
    <reason>Requires external API integration</reason>
  </feature>
  
  <feature name="User authentication" deferred-to="v2.0">
    <reason>MVP is public API - auth adds significant complexity</reason>
  </feature>
  
  <feature name="Batch URL shortening" deferred-to="v2.0">
    <reason>Single URL focus for MVP</reason>
  </feature>
  
  <constraint priority="critical">
    DO NOT implement out-of-scope features without updating PRD and architecture docs
  </constraint>
</out-of-scope>

<!-- ============================================================================
     SECTION 11: QUICK REFERENCE
     Frequently used patterns and commands
     ============================================================================ -->

<quick-reference>
  <common-patterns>
    <pattern name="Create DTO">
      public record MyRequest(@NotBlank String field) {}
    </pattern>
    
    <pattern name="Create Service">
      @Service
      public class MyServiceImpl implements MyService {
          private final MyRepository repository;
          public MyServiceImpl(MyRepository repository) { this.repository = repository; }
      }
    </pattern>
    
    <pattern name="Create Controller">
      @RestController
      @RequestMapping("/api")
      public class MyController {
          private final MyService service;
          public MyController(MyService service) { this.service = service; }
      }
    </pattern>
    
    <pattern name="Create Custom Exception">
      public class MyException extends RuntimeException {
          public MyException(String message) { super(message); }
      }
    </pattern>
    
    <pattern name="Create Test">
      @ExtendWith(MockitoExtension.class)
      class MyServiceTest {
          @Mock private MyRepository repository;
          @InjectMocks private MyServiceImpl service;
      }
    </pattern>
  </common-patterns>
  
  <common-commands>
    <cmd>mvn clean test</cmd>
    <cmd>mvn spring-boot:run</cmd>
    <cmd>docker-compose up --build</cmd>
    <cmd>docker-compose down</cmd>
    <cmd>docker exec -it url-shortener-db psql -U urlshortener -d urlshortener</cmd>
  </common-commands>
</quick-reference>

</copilot-instructions>
```

---

## 🎯 How to Use These Instructions

### For GitHub Copilot
This file is automatically read by GitHub Copilot when suggesting code. The XML structure helps Copilot understand:
- **Hierarchical rules**: Parent constraints inherited by children
- **Priority levels**: Critical vs. high priority rules
- **Context scoping**: When rules apply (applies-to attribute)
- **Pattern examples**: Few-shot learning for code generation

### For Developers
You can also reference this file manually when:
- **Learning project patterns**: See examples of correct implementations
- **Understanding architecture**: Review layered architecture and patterns
- **Using BMAD agents**: Learn when to use which agent
- **Debugging issues**: Check coding standards and common patterns

### For BMAD Integration
When Copilot detects complex tasks, it will suggest appropriate BMAD agents:
- **Complex feature?** → Analyst + Architect + Dev workflow
- **Architecture decision?** → Architect agent
- **Story implementation?** → Dev agent with "DS" command

---

**File Version**: 1.0.0  
**Last Updated**: 2026-02-10  
**Maintained by**: Project Tech Lead  
**BMAD Framework**: v6.0.0-Beta.7
