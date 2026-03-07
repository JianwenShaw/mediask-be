# AGENTS.md - MediAsk Backend Agent Guide
This file defines default instructions for coding agents working in `mediask-be`.

## 1) Repository Snapshot
- Stack: Java 21, Spring Boot 3.5.x, Maven multi-module, MyBatis-Plus, Redis/Redisson, MapStruct, Lombok.
- Modules: 
  - `mediask-api`
  - `mediask-service`
  - `mediask-domain`
  - `mediask-infra`
  - `mediask-dal`
  - `mediask-common`
  - `mediask-worker`.
- Architecture: DDD layering with strict dependency boundaries.
- API docs: `api-docs/openapi.json`, `api-docs/README.md`, `http://localhost:8989/doc.html`.

## 2) Platform-Aware Maven Rule (Mandatory)
Always detect OS first:
```bash
python3 scripts/os_detect.py
```
- If output is `macOS`, run Maven via `./scripts/m21.sh`.
- Otherwise (Linux/Windows/CI), use `mvn` directly.
- `scripts/m21.sh` exports JDK 21 and delegates to Maven.
- In commands below, `<MVN>` means `./scripts/m21.sh` on macOS, otherwise `mvn`.

## 3) Build / Test / Lint Commands
### 3.1 Build and run
```bash
# Full verification (matches CI gate)
<MVN> clean verify

# Fast local compile
<MVN> clean compile -DskipTests

# Build artifacts
<MVN> clean package

# Run API module
<MVN> spring-boot:run -pl mediask-api
```

### 3.2 Test commands (single-test focused)
```bash
# All tests (all modules)
<MVN> test

# All tests in one module
<MVN> test -pl mediask-domain

# One test class
<MVN> test -Dtest=DoctorScheduleTest

# One test class in one module
<MVN> test -pl mediask-domain -Dtest=DoctorScheduleTest

# One test method
<MVN> test -Dtest=DoctorScheduleTest#shouldCreateScheduleWhenInputIsValid

# One test method in one module
<MVN> test -pl mediask-service -Dtest=SchedulePlanApplicationServiceTest#shouldGeneratePlanWhenRulesAreComplete
```

### 3.3 Coverage and reports
```bash
# JaCoCo report generation is bound to test phase
<MVN> test
```
- Typical report path: `**/target/site/jacoco/index.html`.
- Surefire XML output: `**/target/surefire-reports/*.xml`.

### 3.4 Lint / static checks
- No dedicated Checkstyle/Spotless/PMD plugin is configured in current `pom.xml`.
- Use `clean verify` as the effective quality gate.
- If lint plugins are added later, run lint before commit.

## 4) CI Behavior (Mirror Locally)
- GitHub Actions (`.github/workflows/ci.yml`) runs `mvn -B -U verify` on Ubuntu + JDK 21.
- CI uploads Surefire and JaCoCo XML artifacts.
- Before finishing major changes, run commands that are as close to CI as practical.

## 5) DDD Dependency Constraints
Allowed:
- API -> Service + Common
- Service -> Domain + Infra + Common
- Domain -> Common (keep Spring usage minimal)
- Infra -> Domain + DAL + Common
- DAL -> Common

Prohibited:
- API -> Domain or Infra directly
- Service -> DAL Mapper directly (must go via Domain repository interfaces)
- Domain -> DAL or Infra
- Cross-layer shortcuts that bypass Application Service orchestration

## 6) Naming Conventions
- API contracts: `XxxRequest`, `XxxResponse`
- Service data: `XxxDTO`
- Application service: `XxxApplicationService`
- Query service: `XxxQueryService`
- Domain service: `XxxDomainService`
- Domain models/VOs: business names (`DoctorSchedule`, `DoctorId`)
- DAL: `XxxDO`, `XxxMapper`
- Repository impl: `XxxRepositoryImpl`
- Converter: `XxxConverter`
- Helpers/utilities: `XxxHelper`, `XxxUtils`
- Avoid pinyin, cryptic abbreviations, and generic names like `data`, `obj`, `tmp`.

Service naming rule:
- Use `XxxApplicationService` for write use cases, transaction boundaries, orchestration across aggregates, and domain event publishing.
- Use `XxxQueryService` for read-only queries and DTO/view-model assembly.
- Use plain `XxxService` or a more specific suffix such as `XxxDiagnosticService` / `XxxParser` for technical capabilities that are not business use-case orchestration.

## 7) Code Style
- Follow Alibaba Java coding conventions.
- Encoding/line endings: UTF-8 + LF.
- Indentation: 4 spaces (no tabs).
- Max line length: 120 chars.
- Keep classes cohesive and focused on one responsibility.

Imports:
- Group in order: JDK, third-party, project.
- No wildcard imports.
- Keep import order deterministic.

Types and mapping:
- Use `record` for immutable DTO-style carriers when appropriate.
- Use `Optional` for nullable returns, not as entity fields.
- Use `var` only when readability remains clear.
- Prefer MapStruct over manual field-copy boilerplate.
- Do not leak `DO` objects into Service/API contracts.

## 8) Validation, Errors, Transactions
- API layer: enforce validation with `@Valid` / `@Validated`.
- Service layer: enforce business preconditions via assertions/utilities.
- Business failures: throw `BizException` with `ErrorCode`.
- Never swallow exceptions; log meaningful context.
- Use `@Transactional(rollbackFor = Exception.class)` where transactional consistency is required.
- Keep error messages actionable and non-sensitive.

## 9) Logging and Reliability
- Logging stack: SLF4J + Logback.
- Use structured placeholders (`{}`), not string concatenation.
- INFO: business milestones; WARN: recoverable anomalies; ERROR: failures.
- Reuse existing distributed lock/idempotency patterns for concurrency-critical flows.

## 10) Testing Expectations
- Frameworks: JUnit 5, Spring Boot Test, Mockito.
- Coverage targets: Domain >= 90%, Service >= 85%, overall >= 80%.
- Naming pattern: `should<ExpectedBehavior>When<Condition>`.
- Cover happy path, validation failures, business rule violations, and boundaries.

## 11) API Documentation Sync Rule (Mandatory)
When REST APIs in `mediask-api` change, update all:
1. `api-docs/openapi.json`
2. `api-docs/README.md`
3. Validation constraints/examples/fields so docs match implementation

## 12) Diagram and Language Rules
- Use Mermaid for diagrams; do not use ASCII diagrams.
- If the user communicates in Chinese, respond in Chinese.

## 13) Cursor / Copilot Rules Check
Repository scan found no extra instruction files in:
- `.cursor/rules/`
- `.cursorrules`
- `.github/copilot-instructions.md`
If such files are added later, treat them as higher-priority repo rules and merge into this guide.

## 14) Useful References
- `MediAskDocs/docs/01-OVERVIEW.md`
- `MediAskDocs/docs/02-CODE_STANDARDS.md`
- `MediAskDocs/docs/03-CONFIGURATION.md`
- `MediAskDocs/docs/05-TESTING.md`
