# AGENTS.md - Agent Development Guide

## Build Commands

### Platform-Aware Rule (IMPORTANT)

- Detect platform via: `python3 scripts/os_detect.py`
- If output is `macOS`, you **MUST** use `./scripts/m21.sh` instead of `mvn` for all build/test/verify commands.
- On non-macOS platforms, use `mvn`.

```bash
# Full build with tests and coverage
mvn clean verify

# Run all tests
mvn test

# Run single test class
mvn test -Dtest=ClassNameTest

# Run single test method
mvn test -Dtest=ClassNameTest#methodName

# Run tests for specific module
mvn test -pl mediask-domain

# Run application
mvn spring-boot:run -pl mediask-api

# Build with specific profile
mvn clean package -Pprod
```

## Architecture (DDD Layered)

```
API → Service → Domain
     ↓         ↓
   Common   Infra → Domain, DAL, Common
              ↓
            DAL → Common
```

**Dependency Rules:**
- API layer: Service + Common only
- Service layer: Domain + Infra + Common
- Domain layer: Common only (no Spring, no DAL)
- Infra layer: Domain + DAL + Common
- DAL layer: Common only

**STRICTLY PROHIBITED:**
- API → Domain or Infra (bypass Service)
- Service → DAL (use Repository interface)
- Service → Mapper (use Repository interface)
- Domain → DAL or Infra
- Spring annotations in Domain (except @Service for domain services)

## Naming Conventions

| Type | Convention | Example |
|------|------------|---------|
| API Request | `XxxRequest` | `LoginRequest` |
| API Response | `XxxResponse` | `LoginResponse` |
| Service Request | `XxxRequest` | `CreateScheduleRequest` |
| Service DTO | `XxxDTO` | `LoginResponseDTO` |
| Domain Entity | Business Name | `DoctorSchedule` |
| Value Object | Business Name | `DoctorId`, `TimePeriod` |
| Domain Service | `XxxDomainService` | `AutoScheduleDomainService` |
| Application Service | `XxxApplicationService` | `ScheduleApplicationService` |
| Data Object | `XxxDO` | `UserDO`, `DoctorScheduleDO` |
| Mapper | `XxxMapper` | `UserMapper`, `DoctorScheduleMapper` |
| Repository Impl | `XxxRepositoryImpl` | `DoctorScheduleRepositoryImpl` |
| Converter | `XxxConverter` | `ScheduleConverter` |
| Utility | `XxxUtils` | `DateUtils`, `StringUtils` |
| Helper | `XxxHelper` | `EncryptHelper` |

## Code Style

**General:**
- Follow Alibaba Java Development规范
- Use meaningful names (NO abbreviations, NO pinyin)
- Single Responsibility Principle
- High cohesion, low coupling

**Formatting:**
- UTF-8 encoding, LF line endings
- 4-space indentation (NO tabs)
- 120 char line max
- Use Text Blocks for multi-line strings

**Imports:**
- Group: standard, third-party, project modules
- NO wildcard imports (`import java.util.*`)
- Sort alphabetically within groups

**Types:**
- Use `Optional` for nullable values
- Use `record` for immutable DTOs (JDK 21)
- Use `var` for local types (maintain readability)

**Error Handling:**
- Use `BizException` with `ErrorCode` for business errors
- NEVER swallow exceptions
- Log exceptions with context
- Transaction: `@Transactional(rollbackFor = Exception.class)`

**Validation:**
- API layer: `@Valid` or `@Validated`
- Service layer: `AssertUtil` for business rules
- Throw clear exceptions on validation failure

## Common Patterns

**Response Wrapper:**
```java
return R.ok(data);  // Success
return R.fail(ErrorCode.NOT_FOUND);  // Failure
```

**Distributed Lock (Redisson):**
```java
@DistributedLockable(
    key = "'appt:' + #request.scheduleId",
    waitTime = 3,
    leaseTime = 30
)
public Result method(Request request) { ... }
```

**Object Mapping:**
- Use MapStruct converters (`XxxConverter`)
- NO manual getter/setter mapping

**Logging:**
- SLF4J + Logback
- INFO for key business operations
- ERROR for exceptions with context

## JDK 21 Features

**Recommended:**
- `record` for immutable DTOs
- Pattern matching for switch
- Text blocks for multi-line strings
- Sealed classes for value objects

**Caution:**
- Virtual threads may cause DB performance degradation
- Use traditional thread pool for DB operations
- Performance first, don't blindly use new features

## Testing

- JUnit 5 + Spring Boot Test + Mockito
- Minimum coverage: Domain >= 90%, Service >= 85%, Overall >= 80%
- Test naming: `should<ExpectedBehavior>When<UnderWhatCondition>`

## Diagrams

- MUST use Mermaid syntax for ALL diagrams
- NO ASCII diagrams ever
- Verify in Typora/mermaid.js/GitHub

## Language Support

- Respond in Chinese when user queries are in Chinese
- Use Chinese for all explanations in Chinese contexts

## API Documentation Requirements ⚠️ IMPORTANT

**CRITICAL: When modifying API interfaces, you MUST synchronize the API documentation!**

When you change any REST API in the `mediask-api` module:
1. **Update `api-docs/openapi.json`** - OpenAPI 3.0 specification for AI code generation
2. **Update `api-docs/README.md`** - Quick index and documentation for developers
3. **Keep consistency** - Request/Response fields, data types, validation rules must match actual code

**Documentation Location:**
- `api-docs/openapi.json` - Full OpenAPI 3.0 spec
- `api-docs/README.md` - Human-readable API index
- http://localhost:8989/doc.html - Interactive Swagger/Knife4j docs (auto-generated)

**Never commit API changes without updating documentation!**

## Before Committing

Always run:
```bash
# macOS
m21 clean verify

# non-macOS
mvn clean verify
```

If linting is added, run lint command before committing.

## Key Files

- Architecture: `MediAskDocs/docs/01-ARCHITECTURE_OVERVIEW.md`
- Code standards: `MediAskDocs/docs/02-CODE_STANDARDS.md`
- Configuration: `MediAskDocs/docs/03-CONFIGURATION.md`
- API docs: http://localhost:8989/doc.html (dev)
