# User Story: Implement Automated Testing with GitHub Actions

**Story ID:** US-001  
**Created:** 2026-02-10  
**Project:** URL Shortener Service  
**Status:** Draft

---

## User Story

**As a** software developer on the URL Shortener Service team  
**I want** automated tests to run on every code push via GitHub Actions  
**So that** I can quickly identify breaking changes and maintain code quality without manual intervention

---

## Business Value

- **Faster Feedback:** Developers receive immediate test results after pushing code
- **Quality Assurance:** Automated testing catches regressions before they reach production
- **Code Coverage Visibility:** JaCoCo reports provide insights into test coverage trends
- **Reduced Manual Effort:** Eliminates need for developers to remember to run tests locally
- **CI/CD Foundation:** Establishes the groundwork for future deployment automation

---

## Acceptance Criteria

### AC1: Workflow Triggers on Push Events
**Given** a developer pushes commits to any branch in the repository  
**When** the push is completed  
**Then** the GitHub Actions workflow should automatically trigger

### AC2: Environment Setup
**Given** the workflow has been triggered  
**When** the job execution begins  
**Then** the workflow should:
- Use Ubuntu latest runner
- Set up Java 21 (Temurin distribution recommended)
- Cache Maven dependencies for faster builds

### AC3: Test Execution
**Given** the environment is configured  
**When** the test phase executes  
**Then** the workflow should:
- Run `mvn test` successfully
- Execute all unit and integration tests
- Complete without errors for a passing build

### AC4: Code Coverage Reporting
**Given** tests have completed execution  
**When** JaCoCo plugin generates the coverage report  
**Then** the workflow should:
- Generate coverage metrics using the existing JaCoCo configuration
- Make coverage reports available as workflow artifacts
- Display coverage summary in the job logs

### AC5: Build Status Visibility
**Given** the workflow has completed (pass or fail)  
**When** a developer views the repository  
**Then** the workflow status should:
- Be visible as a check mark (✓) or X (✗) on the commit
- Show detailed logs accessible from the Actions tab
- Fail the workflow if any tests fail

---

## Technical Notes

### Technology Stack
- **CI Platform:** GitHub Actions
- **Java Version:** 21
- **Build Tool:** Maven 3.x
- **Test Framework:** JUnit 5 (via Spring Boot Test)
- **Coverage Tool:** JaCoCo 0.8.11 (already configured in pom.xml)
- **Test Types:** Unit tests and integration tests with Testcontainers

### Maven Commands
```bash
mvn clean test              # Run all tests
mvn test jacoco:report      # Generate coverage report
```

### JaCoCo Configuration
The project already has JaCoCo configured with:
- `prepare-agent` goal for instrumentation
- `report` goal bound to test phase
- Reports generated in `target/site/jacoco/`

### Testcontainers Consideration
- Integration tests use Testcontainers for PostgreSQL
- GitHub Actions runner needs Docker support (included in ubuntu-latest)
- May require additional setup time for container downloads

---

## Definition of Done

- [ ] GitHub Actions workflow file (`.github/workflows/ci.yml`) is created
- [ ] Workflow triggers on push to all branches
- [ ] Java 21 is properly configured in the workflow
- [ ] Maven dependencies are cached for performance
- [ ] `mvn test` executes successfully
- [ ] All existing tests pass in the CI environment
- [ ] JaCoCo coverage report is generated
- [ ] Coverage report is uploaded as workflow artifact
- [ ] Workflow status badge can be added to README (optional)
- [ ] Workflow runs successfully for at least one test commit
- [ ] Documentation updated (if needed) about the CI process

---

## Out of Scope

- Pull request triggers (only push events)
- Deployment automation
- Notification integrations (Slack, email, etc.)
- Branch protection rules
- Code quality tools (SonarQube, Checkstyle, etc.)
- Performance testing
- Security scanning

---

## Dependencies

- GitHub repository with Actions enabled
- Existing test suite (✓ already in place)
- JaCoCo configuration (✓ already in pom.xml)
- Docker support for Testcontainers tests

---

## Estimated Effort

**Story Points:** 2-3  
**Estimated Time:** 2-4 hours

**Breakdown:**
- Workflow file creation: 30-60 min
- Testing and debugging: 60-120 min
- Documentation: 30-60 min

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Testcontainers may be slow in CI | Medium | Use caching, consider test optimization |
| Docker rate limiting | Low | Use authenticated Docker Hub access if needed |
| Flaky tests in CI environment | Medium | Identify and fix unstable tests separately |

---

## Additional Notes

- Consider adding a status badge to README.md once workflow is stable
- Future enhancements could include PR checks, branch protection, and deployment
- Monitor workflow execution time and optimize if it exceeds 5-10 minutes

---

**Prepared by:** Mary, Business Analyst  
**For:** Slavaz  
**Language:** English
