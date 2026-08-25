# Research: Database & Results Baseline

**Date**: 2026-08-25
**Feature**: `001-database-results-baseline`

All decisions below are informed by the project constitution (v1.0.0), the feature
specification, Spring Boot / PostgreSQL best practices, and standard academic data
management patterns.

---

## Decision 1 – Score Storage Type

**Decision**: `NUMERIC(5,2)` for all raw score columns.

**Rationale**: IEEE 754 floating-point types (`FLOAT`, `DOUBLE PRECISION`) cannot
represent many decimal fractions exactly, leading to rounding errors in cumulative
aggregations (e.g., 78.1 + 78.2 ≠ 156.3 in floating-point arithmetic). `NUMERIC` is
an exact arbitrary-precision type in PostgreSQL; `NUMERIC(5,2)` allows scores up to
999.99 — sufficient for percentage-based and point-based schemes — while occupying a
predictable storage width.

**Alternatives considered**:
- `DOUBLE PRECISION` — rejected because constitution Principle IV explicitly forbids
  float types for score storage.
- `INTEGER` (store as tenths/hundredths) — rejected; requires application-layer
  scaling, obscures intent, and complicates view arithmetic.

---

## Decision 2 – Grade Immutability Pattern

**Decision**: Append-only corrections via `supersedes_id` self-referencing foreign
key on `exam_results`. A row is "active" when `supersedes_id IS NULL AND` no other
row references it as a superseded record. A database view `v_active_exam_results`
filters to only active rows.

**Rationale**: Academic records are legally significant. Overwriting or deleting
records removes the audit trail needed for grade disputes. The append-only pattern
ensures every historical state is recoverable, and the view abstraction means all
downstream consumers (aggregates, report cards) automatically see only the latest
active result without extra application logic.

**Alternatives considered**:
- Soft-delete (`is_deleted` flag) — rejected; marks the record as gone but doesn't
  capture the corrected value in a linked structure.
- Separate `grade_corrections` table — rejected; splits what is conceptually one
  entity's history into two tables, complicating queries.

---

## Decision 3 – Aggregate Implementation

**Decision**: PostgreSQL `MATERIALIZED VIEW` for class-level aggregates
(`mv_course_aggregates`); a regular `VIEW` (`v_student_period_summary`) for
per-student per-period summaries consumed during report-card generation.

**Rationale**: Materialized views precompute expensive aggregations (weighted
averages, rankings via `RANK() OVER`) and serve them at near-zero query cost.
A regular view for student summaries is acceptable because it is queried once per
report-card generation, not on every page load. Refresh of the materialized view is
explicit (triggered after each batch of grade entries) and the `last_refresh`
metadata is stored in a `mv_refresh_log` table for consumer visibility.

**Alternatives considered**:
- Application-layer aggregation on every request — rejected by constitution
  Principle IV.
- Trigger-based incremental refresh — considered; deferred to v2 as premature
  optimisation; batch refresh is sufficient for ≤ 2,000 students.

---

## Decision 4 – Report Card Idempotency

**Decision**: Before inserting a new `report_card` row, the service checks for an
existing row matching `(student_id, reporting_period_id)`. If one exists in `DRAFT`
state, the service returns it unchanged. If one exists in `APPROVED` state, a
correction that arrives after approval triggers a status revert to `DRAFT` via a
database trigger on `exam_results` (insert trigger, checks for related approved
report cards).

**Rationale**: Concurrent generation attempts (edge case from spec) are resolved at
the database level using a `UNIQUE` constraint on `(student_id, reporting_period_id)`
in `report_cards`. The unique constraint causes any duplicate insert to fail with a
`UniqueConstraintViolationException`, which the service catches and converts to a
"return existing draft" response — no distributed locking required.

**Alternatives considered**:
- Application-level locking (synchronized block / Redis lock) — rejected; adds
  infrastructure complexity not needed at this scale.
- `INSERT … ON CONFLICT DO NOTHING` — considered; chosen for the service layer as
  the primary guard, with the unique constraint as the database-level backstop.

---

## Decision 5 – DTO / Contract Layer

**Decision**: MapStruct compile-time mappers generate `ReportCardDTO`,
`ExamResultDTO`, and `CourseResultDTO`. These DTOs are the only types crossing the
boundary between the results domain and the rendering module.

**Rationale**: Compile-time mapping (MapStruct) is safer than runtime reflection
(ModelMapper) and produces zero-overhead plain method calls in bytecode. Typed DTOs
prevent the rendering layer from accidentally lazy-loading JPA associations (a common
`LazyInitializationException` source when entities escape the service layer).

**Alternatives considered**:
- Expose JPA entities directly — rejected by constitution Principle V; entities must
  not be consumed by the rendering layer directly.
- Jackson `@JsonView` — rejected; couples the domain model to HTTP serialisation
  concerns, making it harder to reuse DTOs for non-HTTP consumers (e.g., batch jobs).

---

## Decision 6 – Migration Tooling

**Decision**: Flyway 10.x with versioned scripts (`V{n}__{description}.sql`) under
`src/main/resources/db/migration/`. Checksum validation on every startup prevents
drift. Repeatable scripts (`R__`) are used only for view definitions so that view
DDL can be updated without a new versioned script.

**Rationale**: Flyway is the constitution-mandated migration tool. Using repeatable
scripts for views (`CREATE OR REPLACE VIEW`) avoids a new `V` script every time a
view query is refined, while the checksum still catches unintended edits.

**Alternatives considered**:
- Liquibase — rejected (constitution mandates Flyway).
- All DDL in versioned scripts including views — acceptable but unnecessarily verbose
  for view changes; repeatable scripts are a standard Flyway pattern for this use case.

---

## Decision 7 – Testing Strategy

**Decision**:
1. `@DataJpaTest` + `@Testcontainers` for all repository-layer tests (real PostgreSQL,
   schema applied by Flyway on container startup).
2. `@SpringBootTest(webEnvironment = NONE)` + Testcontainers for service-layer
   integration tests.
3. Plain JUnit 5 + Mockito for unit tests of domain logic (boundary validation,
   supersession logic) that do not require a database.

**Rationale**: Constitution Principle VI forbids H2 for this domain. Testcontainers
provides a disposable real PostgreSQL instance per test class, ensuring constraint
violations, trigger behaviour, and view queries are tested exactly as they will run
in production.

**Alternatives considered**:
- H2 with PostgreSQL compatibility mode — rejected by constitution.
- Single shared container for all tests — considered; rejected in favour of per-class
  containers to avoid test-order dependencies, at the cost of slightly longer CI time
  (still within the 5-minute target for the suite).
