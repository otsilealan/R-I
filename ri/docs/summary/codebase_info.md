# Codebase Info — School Management Portal (Database & Results Domain)

**Feature**: `001-database-results-baseline` | **Date**: 2026-08-25

---

## Technology Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Java | 17 (LTS) |
| Framework | Spring Boot | 3.x |
| ORM | Spring Data JPA / Hibernate | 6.x |
| Database | PostgreSQL | 15+ |
| Migrations | Flyway | 10.x |
| Connection Pool | HikariCP | Bundled with Spring Boot |
| DTO Mapping | MapStruct | Compile-time |
| Boilerplate | Lombok | Latest |
| Validation | Jakarta Bean Validation | Bundled |
| Build | Maven | 3.9+ |
| Testing | JUnit 5 + Mockito + Testcontainers | — |
| Containerisation | Docker / Docker Compose | 24+ / v2+ |

---

## Project Structure

```
ri/
├── src/
│   ├── main/
│   │   ├── java/com/school/portal/
│   │   │   ├── SchoolPortalApplication.java      # Entry point
│   │   │   ├── config/
│   │   │   │   └── JpaConfig.java                # @EnableJpaAuditing, AuditorAware
│   │   │   ├── controller/                       # REST controllers
│   │   │   ├── service/                          # Business logic
│   │   │   ├── repository/                       # Spring Data JPA repositories
│   │   │   ├── domain/
│   │   │   │   ├── entity/                       # JPA entities (11 classes)
│   │   │   │   └── enums/                        # EnrollmentStatus, ReportCardStatus
│   │   │   ├── dto/                              # DTOs + MapStruct mappers
│   │   │   └── exception/                        # Domain exceptions + GlobalExceptionHandler
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-local.yml
│   │       └── db/migration/                     # Flyway scripts (V1–V6, R__ repeatable)
│   └── test/
│       └── java/com/school/portal/
│           ├── AbstractIntegrationTest.java      # Testcontainers base
│           ├── controller/                       # @SpringBootTest controller tests
│           ├── service/                          # Service unit + integration tests
│           ├── repository/                       # @DataJpaTest repository tests
│           └── domain/                           # Entity constraint + equality tests
├── docs/
│   ├── erd/v1/                                   # SchemaSpy ERD + migration runbook
│   ├── plantuml/                                 # PlantUML diagram sources
│   └── summary/                                  # This documentation set
├── specs/001-database-results-baseline/          # Feature spec, plan, contracts, tasks
├── docker-compose.yml                            # Local PostgreSQL container
└── pom.xml
```

---

## Local Dev Setup

**1. Start PostgreSQL:**
```bash
docker compose up -d db
```

**2. Run application (auto-runs Flyway migrations):**
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```
App starts on `http://localhost:8080`.

**3. Run tests (requires Docker running):**
```bash
./mvnw verify
```

---

## Testing Strategy

| Test type | Annotation | DB |
|---|---|---|
| Entity constraint unit tests | Plain JUnit 5 | None |
| Repository integration | `@DataJpaTest` + Testcontainers | Real PostgreSQL |
| Service unit | JUnit 5 + Mockito | Mocked repositories |
| Service integration | `@SpringBootTest(webEnv=NONE)` + Testcontainers | Real PostgreSQL |
| Controller integration | `@SpringBootTest` + Testcontainers | Real PostgreSQL |

**Coverage requirement**: Repository and service layers ≥ 80% (enforced in CI).

---

## Migration Files

| Script | Tables / Objects Created |
|---|---|
| `V1__create_core_tables.sql` | `students`, `teachers`, `reporting_periods`, `courses`, `enrollments` |
| `V2__create_grade_scale_tables.sql` | `grade_scales`, `grade_scale_entries` |
| `V3__create_exam_results.sql` | `exam_results` |
| `V4__create_grades.sql` | `grades` |
| `V5__create_report_cards.sql` | `report_cards`, `report_card_items`, `mv_refresh_log` |
| `V6__add_sort_priority_to_grade_scale_entries.sql` | `sort_priority` column (safe NOT NULL example) |
| `R__create_views.sql` | `v_active_exam_results`, `mv_course_aggregates`, `v_student_period_summary` |
| `R__create_triggers.sql` | `trg_revert_report_card_on_correction` |

---

## Configuration Files

| File | Purpose |
|---|---|
| `application.yml` | HikariCP, Flyway location, JPA dialect, `ddl-auto=validate` |
| `application-local.yml` | Datasource pointing to `localhost:5432/school_portal` |
| `docker-compose.yml` | `postgres:15-alpine`, port 5432, db `school_portal`, user `portal_user` |

---

## Constraints & Non-negotiables

- No H2 or in-memory DB for integration tests — Testcontainers only.
- No floating-point columns for scores — `NUMERIC(5,2)` minimum.
- No direct DDL outside Flyway migration scripts.
- No entity exposed to the rendering layer — `ReportCardDTO` only.
- ERD must be regenerated and committed in the same PR as any `V__` migration.
- Never modify a committed `V__` script — create a new `V{n+1}` instead.
