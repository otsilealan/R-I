# Feature Specification: Database & Results Baseline

**Feature Branch**: `001-database-results-baseline`

**Created**: 2026-08-25

**Status**: Draft

**Input**: User description: "Create baseline specification for Member 4 – Database & Results Lead covering PostgreSQL schema, ERD, JPA entities, grades, results, report cards and database quality."

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 – School Administrator Defines Grading Scale (Priority: P1)

A school administrator sets up the grading boundaries and letter-grade mappings that
will govern how numeric scores are translated into letter grades across all courses
and reporting periods.

**Why this priority**: Every downstream feature — grade recording, aggregation, and
report-card generation — depends on a correctly configured grading scale. Without it,
no grade can be meaningfully interpreted.

**Independent Test**: Can be tested in isolation by navigating to the grade-scale
management area, entering a set of score boundaries and their corresponding letter
grades, saving them, and confirming that the stored mappings are retrieved accurately
on a subsequent lookup.

**Acceptance Scenarios**:

1. **Given** no grade scale exists for a school term, **When** the administrator
   enters score boundaries (e.g., 90–100 → A, 80–89 → B, …) and submits,
   **Then** the system stores each boundary-to-letter mapping and confirms success.

2. **Given** an existing grade scale, **When** the administrator modifies a boundary
   and saves, **Then** the old mapping is preserved in history and the new mapping
   becomes active for subsequent grade calculations.

3. **Given** a grade scale with overlapping boundaries, **When** the administrator
   attempts to save, **Then** the system rejects the submission and displays a clear
   validation message identifying the conflict.

4. **Given** an active grade scale, **When** a grade record already references it,
   **Then** the system prevents deletion of that scale and informs the administrator.

---

### User Story 2 – Teacher Records Exam Results for a Course (Priority: P1)

A teacher enters or updates individual student scores for an exam within a specific
course and reporting period. The system validates, stores, and audits every entry.

**Why this priority**: Grade entry is the most frequent operation in the results
domain. Correctness, auditability, and immutability of each entry are foundational
requirements for report-card generation and academic records.

**Independent Test**: Can be tested by having a teacher select a course, choose an
exam, enter a numeric score for one student, submit, and then verify the stored record
contains the correct score, audit fields, and the teacher's identity.

**Acceptance Scenarios**:

1. **Given** a teacher is assigned to a course, **When** they submit a valid numeric
   score (e.g., 78.50) for an enrolled student, **Then** the system stores the result
   with `created_at`, `created_by`, and `updated_at` audit fields populated correctly.

2. **Given** a teacher submits a score outside the valid range (e.g., −5 or 105),
   **When** the form is submitted, **Then** the system rejects the entry and reports
   the valid range to the teacher.

3. **Given** an existing exam result, **When** the teacher submits a corrected score,
   **Then** the original record is not deleted or overwritten; a new record is created
   that references the original via a correction link, and the new record becomes
   the active result.

4. **Given** a student is not enrolled in a course, **When** a teacher attempts to
   record a result for that student in that course, **Then** the system rejects the
   entry with a clear message.

---

### User Story 3 – System Calculates Grade Aggregates (Priority: P2)

The system automatically derives per-student averages, class averages, and rankings
for a course and reporting period, making these aggregates available to teachers and
administrators without manual computation.

**Why this priority**: Aggregates are consumed directly by report-card generation
and academic dashboards. They must be consistent and reproducible across all
consumers of the data.

**Independent Test**: Can be tested by entering a known set of scores for a class,
triggering aggregate recalculation, and confirming the outputs (individual averages,
class mean, student rank) match the expected values from manual calculation.

**Acceptance Scenarios**:

1. **Given** a full set of exam results for a course and period, **When** aggregates
   are requested, **Then** the system returns each student's weighted average, the
   class mean, and each student's rank within the class — all matching expected
   values within a tolerance of ±0.01.

2. **Given** one or more students have no recorded result for an exam, **When**
   aggregates are computed, **Then** the system applies the configured missing-score
   policy (default: exclude from ranking, flag as incomplete) without crashing or
   producing silent incorrect results.

3. **Given** a grade correction has been recorded, **When** aggregates are
   recalculated, **Then** only the most recent active result for each student is
   used; superseded records do not contribute to the aggregate.

---

### User Story 4 – Administrator Generates a Student Report Card (Priority: P2)

An administrator initiates report-card generation for a student and reporting period.
The system assembles all relevant grades and aggregates into a structured report card
record that can be reviewed, approved, and distributed.

**Why this priority**: Report cards are the primary deliverable of the results domain.
They are official documents requiring determinism, approval workflow, and
immutability once issued.

**Independent Test**: Can be tested by seeding a complete set of grades for a student
across all enrolled courses for one reporting period, generating the report card,
and verifying the output data matches the seeded grades and derived aggregates exactly.

**Acceptance Scenarios**:

1. **Given** all grades for a student and period are recorded, **When** an
   administrator initiates generation, **Then** the system creates a report-card
   record in `DRAFT` status containing every course result and aggregate, with a
   `generated_at` timestamp and `generated_by` identity.

2. **Given** a report card in `DRAFT` status, **When** an administrator approves it,
   **Then** the status transitions to `APPROVED` and the record becomes immutable.

3. **Given** an `APPROVED` report card exists, **When** a teacher submits a grade
   correction for that student and period, **Then** the report card's status
   automatically reverts to `DRAFT` and the administrator is notified.

4. **Given** a report card is regenerated for the same student and period,
   **Then** the output is identical to the previous generation if no underlying
   grades have changed (idempotency).

5. **Given** a student has incomplete grades (some courses missing results), **When**
   generation is attempted, **Then** the system warns the administrator of the
   incomplete courses but allows generation to proceed with those courses flagged.

---

### User Story 5 – Database Lead Applies a Schema Migration (Priority: P1)

A developer on the database team applies a new Flyway migration script to introduce
or alter a table in the schema. The migration executes cleanly, the ERD is updated,
and downstream JPA entities continue to pass all tests.

**Why this priority**: Schema evolution is a continuous activity. A clean, repeatable
migration workflow prevents environment drift and is the gating step before any
new data feature can be developed or tested.

**Independent Test**: Can be tested by applying a new migration script against a clean
database instance and verifying: the target table structure matches the script, no
prior migration checksums are invalidated, and all existing JPA entity tests pass.

**Acceptance Scenarios**:

1. **Given** a new `V{n}__description.sql` migration file, **When** Flyway runs on a
   clean schema, **Then** all migrations apply in order without errors and
   `flyway_schema_history` records each migration with status `SUCCESS`.

2. **Given** a previously applied migration, **When** an attempt is made to modify
   the script file, **Then** Flyway detects the checksum mismatch on the next run
   and halts with an error, preventing silent schema drift.

3. **Given** a migration adds a new column with a `NOT NULL` constraint, **When**
   the migration runs against a table that already contains rows, **Then** the
   migration either includes a default value or a data-backfill step — the migration
   MUST NOT leave the database in an inconsistent state.

---

### Edge Cases

- What happens when a reporting period is closed and a teacher attempts to submit new
  results? The system MUST reject the submission and display the period's close date.
- What happens if the grade-scale table contains no entry for a computed numeric
  score? The system MUST flag the result as `UNGRADED` rather than returning null
  or crashing.
- How does the system handle a student enrolled in the same course twice (e.g., repeat
  year)? Each enrollment instance MUST be treated as a distinct record tied to a
  specific academic year and period.
- What happens when the materialized view for aggregates is stale? The system MUST
  expose a refresh mechanism and indicate the last-refreshed timestamp to consumers.
- What happens if two administrators trigger report-card generation for the same
  student and period simultaneously? The system MUST enforce an idempotency check and
  return the existing draft rather than creating a duplicate.

---

## Requirements *(mandatory)*

### Functional Requirements

**Schema & Migrations**

- **FR-001**: The system MUST manage all schema changes through versioned, sequentially
  numbered migration scripts; direct DDL execution against any environment database is
  forbidden.
- **FR-002**: Every table MUST have a surrogate primary key; foreign-key constraints
  MUST be enforced at the database level.
- **FR-003**: Nullable columns MUST be explicitly declared; all other columns default
  to `NOT NULL`.
- **FR-004**: The system MUST maintain a `flyway_schema_history` (or equivalent) table
  and reject any run where a previously applied script's checksum has changed.

**Grade Scale**

- **FR-005**: The system MUST store grading boundaries and letter-grade mappings in a
  dedicated reference table (`grade_scale`); no boundary logic may be hard-coded in
  application code.
- **FR-006**: The system MUST validate that grade-scale boundaries are non-overlapping
  and contiguous before persisting.
- **FR-007**: The system MUST prevent deletion of a grade-scale entry that is
  referenced by existing grade records.

**Grade & Exam Result Recording**

- **FR-008**: The system MUST store raw numeric scores as fixed-precision decimals
  (minimum two decimal places); floating-point types are not permitted for score
  columns.
- **FR-009**: Every grade and exam-result record MUST carry `created_at`,
  `updated_at`, and `created_by` fields, populated automatically by the persistence
  layer.
- **FR-010**: Grade corrections MUST be recorded as new rows linked to the original
  via a `supersedes_id` reference; original records MUST NOT be deleted or updated.
- **FR-011**: The system MUST reject score submissions for students not enrolled in the
  target course for the target period.
- **FR-012**: The system MUST reject score submissions for closed reporting periods.

**Aggregation**

- **FR-013**: Per-student averages, class averages, and student rankings MUST be
  derived from database-level views or materialized views; application-layer
  re-implementation of the same arithmetic is forbidden.
- **FR-014**: When computing aggregates, the system MUST use only the most recent
  active (non-superseded) result for each student-course-exam combination.
- **FR-015**: The system MUST expose the last-refreshed timestamp for any materialized
  view and provide an explicit refresh trigger.

**Report Cards**

- **FR-016**: Report-card generation MUST be deterministic: running generation for
  the same student and reporting period with unchanged underlying grades MUST produce
  an identical output.
- **FR-017**: Generated report cards MUST be persisted as database records with
  `generated_at` and `generated_by` metadata.
- **FR-018**: A report card MUST transition from `APPROVED` back to `DRAFT`
  automatically when any underlying grade for that student and period is corrected
  after approval.
- **FR-019**: The rendering layer MUST receive report-card data exclusively through a
  defined data-transfer object; it MUST NOT execute database queries directly.
- **FR-020**: The system MUST support generation of report cards for students with
  incomplete results, flagging missing-result courses without aborting generation.

**JPA Entities**

- **FR-021**: Every database table MUST have a corresponding JPA entity with explicit
  column mappings; implicit defaults for column name, nullability, or length are not
  permitted.
- **FR-022**: All JPA relationships MUST default to lazy fetching; eager fetching
  requires documented justification.
- **FR-023**: JPA entities MUST implement `equals()` and `hashCode()` based on the
  natural business key.

**Database Quality**

- **FR-024**: Every foreign-key column MUST have a database index.
- **FR-025**: Integration tests for the results domain MUST run against a real
  PostgreSQL instance; in-memory database substitutes are not permitted.
- **FR-026**: Code coverage for repository and service layers in the results domain
  MUST not fall below 80%.

### Key Entities

- **Student**: A person enrolled in the institution; identified by student number and
  linked to enrollments.
- **Teacher**: A staff member who delivers courses and records grades; linked to
  course assignments.
- **Course**: An academic unit offered in a specific academic year and term;
  associated with a teacher and a set of enrolled students.
- **Enrollment**: The relationship between a Student and a Course for a specific
  academic period; carries enrollment status.
- **GradeScale**: A reference table defining score boundaries and their corresponding
  letter grades; scoped to a school or institution.
- **ExamResult**: A single score record for a student in a specific exam within a
  course; carries audit fields and an optional reference to the record it supersedes.
- **Grade**: The resolved letter grade derived from an ExamResult via the active
  GradeScale; linked to the ExamResult and the GradeScale entry used.
- **ReportingPeriod**: A named academic interval (e.g., Semester 1, Term 2) with
  defined open/close dates for grade submission.
- **ReportCard**: A generated, versioned summary of a student's grades and aggregates
  for one ReportingPeriod; carries status (`DRAFT` / `APPROVED`) and generation
  metadata.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All schema changes are applied exclusively through migration scripts;
  zero instances of direct DDL outside of migration files are found in any environment
  across the project lifetime.
- **SC-002**: Grade entry for a single student-exam combination completes and is
  confirmed to the teacher within 3 seconds under normal load.
- **SC-003**: Aggregate recalculation for a full class of up to 200 students across
  10 exams completes within 5 seconds.
- **SC-004**: Report-card generation for a single student and period completes within
  10 seconds, including all grade lookups and aggregate derivation.
- **SC-005**: 100% of grade correction submissions are recorded as new immutable rows;
  no original grade record is modified or deleted over the life of the system.
- **SC-006**: Report-card output for the same student, period, and unchanged grades is
  byte-for-byte equivalent across repeated generation calls (idempotency verified by
  automated test).
- **SC-007**: Integration test suite passes against a fresh PostgreSQL instance with
  no pre-existing data in under 5 minutes.
- **SC-008**: Code coverage for the repository and service layers stays at or above
  80% as measured in the CI pipeline after every merge.
- **SC-009**: The ERD is reviewed and updated within the same PR as every schema
  migration; no migration is merged without an accompanying ERD update.

---

## Assumptions

- The system is built as a Spring Boot application using Spring Data JPA with
  Hibernate; the ORM and persistence framework are not in scope for this specification
  to decide, as they are fixed by the team constitution.
- PostgreSQL 15+ is the only supported database; no portability to other RDBMS is
  required.
- Flyway is the migration tool; migration files follow the `V{n}__{description}.sql`
  naming convention.
- A single institution (school) is in scope for v1; multi-tenancy is out of scope.
- Authentication and user identity management are handled by a separate team member's
  module; this specification assumes that an authenticated user context (with role and
  identity) is available to the persistence layer at the time of any write operation.
- A "reporting period" maps to a semester or term as defined by the school's academic
  calendar; the calendar management itself is out of scope for this member's domain.
- The rendering of report cards to PDF or HTML is out of scope for this specification;
  this domain provides only the structured data (DTO) consumed by the rendering layer.
- Testcontainers is available in the CI environment; network-isolated PostgreSQL
  container startup is expected to take no more than 60 seconds.
- Mobile client support is out of scope for v1; all interactions are assumed to go
  through a web-based interface or REST API consumed by other team members' modules.
