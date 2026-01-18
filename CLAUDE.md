# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MediAsk - 智能医疗辅助问诊系统 (Intelligent Medical Assistant Consultation System). A Java 21 + Spring Boot 3.5 modular monolith implementing DDD architecture with AI-powered medical consultation features (RAG-based knowledge base, appointment scheduling, medical records).

## Build Commands

```bash
# Build with tests
mvn clean verify

# Run tests only
mvn test

# Run specific module
mvn spring-boot:run -pl mediask-api

# Build with profile
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

| Module | Purpose | Location |
|--------|---------|----------|
| `mediask-api` | REST controllers, JWT auth, security config | Web entry point |
| `mediask-service` | Application services, business orchestration | Use case implementation |
| `mediask-domain` | Entities, value objects, domain services, repository interfaces | Core business logic |
| `mediask-infra` | Repository implementations, Redis, MQ, AI clients | Technical implementations |
| `mediask-dal` | DO entities, MyBatis-Plus mappers | Data access |
| `mediask-common` | Utilities, exceptions, constants, response wrapper | Shared code |
| `mediask-worker` | Scheduled jobs, MQ consumers | Async tasks |

**Critical Constraints**:
- API layer cannot directly depend on Domain or Infra
- Domain layer cannot depend on DAL or Infra (only Common)
- No Spring annotations in Domain layer (except `@Service` for domain services)
- No Mapper usage in Service layer (use Repository interface)

## Naming Conventions

| Type | Convention | Example |
|------|------------|---------|
| API Request | `XxxRequest` | `LoginRequest` |
| API Response | `XxxResponse` | `LoginResponse` |
| Service Request | `XxxRequest` | `CreateScheduleRequest` |
| Service DTO | `XxxDTO` | `LoginResponseDTO` |
| Domain Service | `XxxDomainService` | `AutoScheduleDomainService` |
| Domain Entity | Business Name | `DoctorSchedule` |
| Value Object | Business Name | `DoctorId`, `TimePeriod` |
| Data Object | `XxxDO` | `UserDO` |
| Mapper | `XxxMapper` | `UserMapper` |
| Repository Impl | `XxxRepositoryImpl` | `DoctorScheduleRepositoryImpl` |
| Converter | `XxxConverter` | `ScheduleConverter` |
| Utility | `XxxUtils` | `DateUtils` |

## Common Patterns

**Response Wrapper**: Use `R<T>` for all API responses
```java
R.ok(data)
R.fail(ErrorCode.NOT_FOUND)
```

**Business Exception**: Use `BizException` with `ErrorCode`
```java
throw new BizException(ErrorCode.NOT_FOUND);
```

**Distributed Lock** (use `@DistributedLockable` annotation in `mediask-infra`):
```java
// Annotation-based approach (RECOMMENDED)
@DistributedLockable(
    key = "'appt:create:' + #request.scheduleId",
    waitTime = 3,
    leaseTime = 30
)
public AppointmentResultDTO createAppointment(CreateAppointmentRequest request) {
    // business logic - lock automatically acquired and released
}

// SpEL expression examples:
// - "'appt:' + #dto.scheduleId"
// - "'user:' + #user.id"
// - "'order:' + #userId + ':' + #productId"
```

**Idempotency**: Use `@Idempotent` annotation with Redis for critical operations

**Object Mapping**: Use MapStruct converters (`XxxConverter`)

## Key Dependencies

- Spring Boot 3.5.8, Java 21
- MyBatis-Plus 3.5.15 (ORM)
- Redis 7.x with Redisson (distributed lock, cache)
- Milvus 2.4+ (vector database for RAG)
- RocketMQ 5.0+ (message queue)
- LangChain4j 1.9.1 (AI integration)
- Spring Security + JWT 0.12.6 (authentication)

## Test Configuration

- JUnit 5 + Spring Boot Test + Mockito
- Minimum coverage: Domain >= 90%, Service >= 85%, Overall >= 80%
- TestContainers available for integration tests

## Important Notes

- **Bilingual support**: Respond in Chinese when user queries are in Chinese
- **Diagrams**: Use Mermaid syntax only, never ASCII diagrams
- **Virtual threads**: Use cautiously for DB operations (may cause performance degradation)
- **API docs**: http://localhost:8989/doc.html (dev profile)
- **Code style**: Follow Alibaba Java Development规范
- **Reuse existing infrastructure**: Check `mediask-infra/src/main/java/me/jianwen/mediask/infra/` for common implementations before creating new ones
- **Sync documentation**: When modifying APIs, always update the corresponding documentation in `api-docs/` directory
- **Reuse code**: Prefer using existing utilities and infrastructure components over creating new ones
- **Correct code placement**: Generate code in the proper module and layer following DDD conventions

## Documentation

See `MediAskDocs/` directory for detailed architecture, coding standards, configuration, and deployment guides.
