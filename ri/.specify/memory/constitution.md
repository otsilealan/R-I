<!--
SYNC IMPACT REPORT
==================
Version change:    (none) → 1.0.0  [initial ratification]
Added sections:    Core Principles (I–VI), Technology Standards, Database Quality & Workflow, Governance
Removed sections:  n/a
Follow-up TODOs:   None — all placeholders resolved.
-->

# School Management Portal – Database & Results Lead Constitution

## Core Principles

### I. Schema-First Design (NON-NEGOTIABLE)

The PostgreSQL schema is the single source of truth for all data in the system.
Every structural decision — column types, nullability, constraints, relationships —
MUST be defined explicitly in a versioned migration script before any application
code is written or modified.

- All tables MUST have a surrogate primary key (`BIGSERIAL` or `UUID`).
- Foreign-key constraints MUST be declared in the database, not only enforced at the
  application layer.
- `NOT NULL` is the default assumption; nullable columns require an explicit written
  justification in the migration comment.
- No schema changes may be applied directly to any environment database; changes MUST
  go through a numbered Flyway/Liquibase migration file committed to version control.

Rationale: Schema drift is the leading cause of data-corruption bugs. A code-reviewed,
migration-based workflow makes every structural change auditable and reproducible
across all environments (dev, test, production).

### II. ERD as Living Documentation

An up-to-date Entity–Relationship Diagram MUST accompany every schema release.

- The ERD MUST be stored as a versioned artefact in `/docs/erd/` alongside each
  migration.
- The ERD MUST be reviewed and signed off by at least one other team member before a
  schema PR is merged.
- Tools that auto-generate the ERD from the live schema (e.g., SchemaSpy, DBeaver
  export) are encouraged; hand-drawn approximations are not acceptable as the sole
  documentation.
- The ERD MUST cover all entities: `Student`, `Teacher`, `Course`, `Enrollment`,
  `Grade`, `ExamResult`, `ReportCard`, and any junction or audit tables.

Rationale: An ERD turns implicit knowledge into explicit contracts, preventing
misaligned assumptions between the database layer and application developers.

### III. JPA Entity Parity

Every database table MUST have a corresponding JPA entity class that faithfully
mirrors the schema.

- Entity class names MUST match table names (singular, PascalCase vs. snake_case table
  names), e.g., `ExamResult` ↔ `exam_result`.
- Column mappings (`@Column`) MUST specify `name`, `nullable`, and `length`/`precision`
  wherever applicable — no implicit defaults.
- Relationships (`@OneToMany`, `@ManyToOne`, `@ManyToMany`) MUST use `FetchType.LAZY`
  by default; `EAGER` fetching requires written justification.
- All entities MUST implement `equals()` and `hashCode()` based on the natural business
  key, not the surrogate primary key, to avoid Hibernate identity issues.
- Bidirectional associations MUST manage both sides of the relationship via helper
  methods on the owning entity.

Rationale: Mismatches between the JPA model and the schema are a silent source of
runtime failures and data corruption. Explicit mappings are testable; implicit
defaults are not.

### IV. Grades & Results Integrity (NON-NEGOTIABLE)

Grade and exam-result data is legally and academically sensitive. Integrity rules are
non-negotiable.

- Raw scores MUST be stored as `NUMERIC(5,2)` (or equivalent) — never as `FLOAT` or
  `DOUBLE`, to avoid floating-point rounding errors in academic computations.
- Letter grades and grade boundaries MUST be derived from a `grade_scale` reference
  table; hard-coded boundary logic in application code is forbidden.
- Every `Grade` and `ExamResult` record MUST carry `created_at`, `updated_at`, and
  `created_by` audit columns populated automatically via JPA lifecycle callbacks.
- Grade corrections MUST be recorded as new rows with a `supersedes_id` foreign key;
  no grade record may be deleted or overwritten once it has been issued.
- Aggregation queries (GPA, class average, rank) MUST be implemented as PostgreSQL
  views or materialized views, not inline application-layer arithmetic, to ensure
  consistency across all consumers.

Rationale: Errors in grade data can have serious academic and legal consequences.
Immutability, audit trails, and reference-table-driven logic are the minimum safeguards.

### V. Report Card Generation

Report cards are structured outputs derived entirely from the grades and results
stored in the database.

- The report-card generation process MUST be a deterministic query against the
  database at a specific `reporting_period`; the same period MUST always produce the
  same report card (idempotency).
- Generated report cards MUST be stored as records in a `report_card` table (with a
  `generated_at` timestamp and `generated_by` user reference) once approved; they
  MUST NOT be regenerated silently.
- The PDF/HTML rendering layer MUST consume a well-defined data-transfer object (DTO)
  produced by the service layer — it MUST NOT issue its own database queries.
- A report card MUST be invalidated (status set to `DRAFT`) automatically if any
  underlying grade for that student/period changes after generation.

Rationale: Report cards are official documents. Determinism and immutability after
approval protect institutional integrity and allow dispute resolution.

### VI. Database Quality Gates

No schema migration or JPA change reaches the main branch without passing all quality
gates.

- Unit tests MUST cover all JPA entity constraint validations (Bean Validation
  annotations and database constraints verified via `@DataJpaTest` or equivalent).
- Integration tests MUST run against an actual PostgreSQL instance (Testcontainers
  preferred) — H2 or in-memory substitutes are not acceptable for the grades/results
  domain.
- Every query used for grade aggregation or report-card generation MUST have an
  associated performance test validating execution plan (no full-table scans on
  tables expected to exceed 10,000 rows).
- Code coverage for the database/results domain MUST not fall below 80% on the
  service and repository layers.

Rationale: Academic data requires higher-than-average correctness guarantees. Weak
tests with surrogate databases hide real constraint violations.

## Technology Standards

The following technology choices are fixed for the database and results domain.
Deviations require a team-wide constitution amendment.

- **Database**: PostgreSQL 15+ (no other RDBMS permitted for the main data store).
- **Migration tool**: Flyway (migration scripts in `src/main/resources/db/migration/`);
  script naming convention `V{version}__{description}.sql`.
- **ORM**: Spring Data JPA with Hibernate 6+; Spring repositories MUST extend
  `JpaRepository` or a custom base repository — no `EntityManager` calls in
  controller or service layers.
- **Connection pooling**: HikariCP with a pool size tuned to the deployment environment
  (documented in `application.yml`; no magic numbers).
- **Schema documentation**: SchemaSpy or pgAdmin ERD export; stored under
  `/docs/erd/v{version}/`.
- **Test containers**: `testcontainers-postgresql` for all integration tests; version
  pinned in the root `pom.xml` / `build.gradle`.

## Database Quality & Workflow

### Migration Workflow

1. Create a new migration file: `V{next}__{short_description}.sql`.
2. Write or update the corresponding JPA entity/repository.
3. Update the ERD export under `/docs/erd/`.
4. Write unit and integration tests covering the change.
5. Open a PR; the ERD diff MUST be included in the PR description.
6. At least one reviewer MUST verify schema constraints match entity annotations.
7. Merge only after all CI quality gates pass.

### Index Policy

- Every foreign-key column MUST have a covering index unless the table is provably
  small (< 500 rows and never joined in a high-frequency query path).
- Composite indexes MUST be justified with an `EXPLAIN ANALYZE` output in the
  migration comment.
- Unused indexes MUST be removed; `pg_stat_user_indexes` is reviewed at each sprint
  end.

### Naming Conventions

| Object | Convention | Example |
|---|---|---|
| Table | snake_case, plural | `exam_results` |
| Column | snake_case | `raw_score` |
| Primary key | `id` | `id BIGSERIAL` |
| Foreign key | `{referenced_table_singular}_id` | `student_id` |
| Index | `idx_{table}_{columns}` | `idx_exam_results_student_id` |
| Sequence | `{table}_id_seq` (auto by PostgreSQL) | — |
| JPA Entity | PascalCase, singular | `ExamResult` |
| Repository | `{Entity}Repository` | `ExamResultRepository` |

## Governance

This constitution governs all work produced by Member 4 – Database & Results Lead on
the School Management Portal project. It supersedes any informal agreement, prior
note, or verbal instruction regarding schema design, grade data handling, or report
card generation.

**Amendment procedure**:
- Any team member may propose an amendment by opening a PR that modifies this file.
- The PR description MUST explain the rationale and identify which principle is
  affected and whether the version bump is MAJOR, MINOR, or PATCH.
- Amendments require approval from at least the Database Lead and one other team
  member before merging.
- MAJOR amendments (principle removals or redefinitions) require full team consensus.

**Versioning policy** (semantic):
- MAJOR: Backward-incompatible governance changes (removing a principle, changing a
  non-negotiable rule).
- MINOR: New principle added, new mandatory technology added, or materially expanded
  guidance.
- PATCH: Clarifications, wording improvements, typo fixes.

**Compliance review**:
- Compliance is verified at every PR review; reviewers MUST flag violations rather
  than merge with "will fix later" notes.
- A sprint-end compliance check reviews index usage, migration naming, ERD freshness,
  and test-coverage metrics.

**Version**: 1.0.0 | **Ratified**: 2026-08-25 | **Last Amended**: 2026-08-25
