# Implementation Plan: Database & Results Baseline

**Branch**: `001-database-results-baseline` | **Date**: 2026-08-25 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-database-results-baseline/spec.md`

---

## Summary

Establish the complete PostgreSQL schema, JPA entity layer, grade/result recording
logic, aggregate views, and report-card generation pipeline for the School Management
Portal's Database & Results domain. The design is migration-first (Flyway), enforces
immutable grade records, derives all aggregates from database views, and exposes
report-card data exclusively through a typed DTO layer consumed by the rendering
module owned by other team members.

---

## Technical Context

**Language/Version**: Java 17 (LTS) with Spring Boot 3.x

**Primary Dependencies**:
- Spring Data JPA / Hibernate 6.x (ORM)
- Flyway 10.x (schema migrations)
- PostgreSQL JDBC Driver (org.postgresql:postgresql)
- HikariCP (connection pooling, bundled with Spring Boot)
- Testcontainers (`testcontainers-postgresql`) for integration tests
- Bean Validation (jakarta.validation) for entity constraints
- MapStruct (DTO mapping, compile-time)
- Lombok (boilerplate reduction on entities/DTOs)

**Storage**: PostgreSQL 15+ (single RDBMS, no in-memory substitute permitted)

**Testing**:
- JUnit 5 + Mockito (unit tests)
- `@DataJpaTest` with Testcontainers PostgreSQL (integration/repository tests)
- Spring Boot Test slice (`@SpringBootTest`) for service-layer integration tests

**Target Platform**: Linux server (CI) / Docker (local dev via Docker Compose)

**Project Type**: Web service module — REST API backend, part of a larger Spring Boot
monolith or multi-module Maven/Gradle project

**Performance Goals**:
- Single grade-entry round-trip: < 3 s (SC-002)
- Aggregate recalculation (200 students × 10 exams): < 5 s (SC-003)
- Report-card generation (single student/period): < 10 s (SC-004)
- Integration test suite (full, clean DB): < 5 min (SC-007)

**Constraints**:
- No floating-point score storage (NUMERIC(5,2) minimum)
- No in-memory DB (H2) for integration tests
- All FK columns must be indexed
- Code coverage ≥ 80% on repository and service layers
- ERD updated in same PR as every migration

**Scale/Scope**:
- Single institution (school), v1 — no multi-tenancy
- Expected steady-state: ~2,000 students, ~200 courses per period,
  ~10 exams per course → ~4,000,000 exam_result rows after 5 years
- Concurrent users: up to 50 simultaneous grade-entry sessions

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| # | Principle | Status | Notes |
|---|---|---|---|
| I | Schema-First Design | ✅ PASS | All tables defined via Flyway V-scripts; no entity auto-DDL |
| II | ERD as Living Documentation | ✅ PASS | ERD artefact planned under `/docs/erd/v1/`; PR gate enforced |
| III | JPA Entity Parity | ✅ PASS | All 9 entities mapped; explicit `@Column`, lazy fetch default |
| IV | Grades & Results Integrity | ✅ PASS | NUMERIC(5,2), immutable rows, `supersedes_id`, audit columns, DB views |
| V | Report Card Generation | ✅ PASS | Deterministic query, stored record, DTO-only rendering contract |
| VI | Database Quality Gates | ✅ PASS | Testcontainers, ≥80% coverage target, query-plan tests planned |

**Gate result**: ALL PASS — proceed to Phase 1 design.

---

## Project Structure

### Documentation (this feature)

```text
specs/001-database-results-baseline/
├── plan.md              # This file
├── research.md          # Phase 0 — technology decisions
├── data-model.md        # Phase 1 — entity definitions and schema
├── quickstart.md        # Phase 1 — validation and run guide
├── contracts/
│   ├── result-recording-api.md
│   └── report-card-dto.md
└── tasks.md             # Phase 2 — created by /speckit.tasks
```

### Source Code (repository root)

```text
src/
├── main/
│   ├── java/com/school/portal/
│   │   ├── domain/
│   │   │   ├── entity/          # JPA entities (Student, Teacher, Course, …)
│   │   │   └── enums/           # ReportCardStatus, EnrollmentStatus, …
│   │   ├── repository/          # Spring Data JPA repositories
│   │   ├── service/
│   │   │   ├── GradeScaleService.java
│   │   │   ├── ExamResultService.java
│   │   │   ├── AggregateService.java
│   │   │   └── ReportCardService.java
│   │   ├── dto/                 # DTOs + MapStruct mappers
│   │   └── exception/           # Domain-specific exceptions
│   └── resources/
│       └── db/migration/        # Flyway V-scripts
│           ├── V1__create_core_tables.sql
│           ├── V2__create_grade_scale.sql
│           ├── V3__create_exam_results.sql
│           ├── V4__create_report_cards.sql
│           └── V5__create_aggregate_views.sql
├── test/
│   └── java/com/school/portal/
│       ├── repository/          # @DataJpaTest + Testcontainers
│       ├── service/             # @SpringBootTest service tests
│       └── domain/              # Entity constraint unit tests

docs/
└── erd/
    └── v1/
        └── school_portal_erd.png   # SchemaSpy / pgAdmin export
```

**Structure Decision**: Single Spring Boot module (Option 1 adapted for Java). The
`domain/entity/` package holds all JPA entities; repositories are thin Spring Data
interfaces; business logic lives exclusively in the `service/` package to allow
isolated unit testing. No `EntityManager` calls in controllers or services.

---

## Complexity Tracking

No constitution violations identified. Section omitted.
