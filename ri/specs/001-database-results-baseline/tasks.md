# Tasks: Database & Results Baseline

**Input**: Design documents from `specs/001-database-results-baseline/`

**Prerequisites**: plan.md ✅ | spec.md ✅ | research.md ✅ | data-model.md ✅ | contracts/ ✅ | quickstart.md ✅

**Tests**: Included — the spec and constitution both mandate ≥ 80% coverage with
Testcontainers-backed integration tests. Test tasks are interleaved with
implementation tasks as described below.

**Organization**: Tasks are grouped by user story to enable independent
implementation and testing of each story.

---

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (independent files, no incomplete dependencies)
- **[Story]**: Which user story this task belongs to (US1 – US5)
- All file paths are relative to the project/module root

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Establish the project skeleton, build configuration, and shared tooling
that everything else depends on.

- [x] T001 Add required dependencies to `pom.xml` / `build.gradle`: Spring Data JPA, Flyway 10.x, PostgreSQL JDBC, HikariCP, Testcontainers (postgresql), MapStruct, Lombok, Bean Validation, JUnit 5, Mockito
- [x] T002 [P] Create Spring Boot application configuration `src/main/resources/application.yml` with HikariCP pool settings, Flyway location, JPA dialect (PostgreSQLDialect), and `spring.jpa.ddl-auto=validate`
- [x] T003 [P] Create `src/main/resources/application-local.yml` with local Docker Compose datasource (`localhost:5432/school_portal`)
- [x] T004 [P] Create `src/test/resources/application-test.yml` with Testcontainers datasource properties (`spring.datasource.url=` placeholder overridden by container)
- [x] T005 [P] Add `docker-compose.yml` at project root with `postgres:15-alpine` service (port 5432, env vars for `school_portal` db, `portal_user`, `portal_pass`)
- [x] T006 Create base package structure: `src/main/java/com/school/portal/domain/entity/`, `domain/enums/`, `repository/`, `service/`, `dto/`, `exception/`, `controller/`
- [x] T007 [P] Create `src/main/java/com/school/portal/exception/` classes: `ResourceNotFoundException`, `BusinessRuleViolationException`, `ClosedPeriodException`, `DuplicateReportCardException`
- [x] T008 [P] Create global exception handler `src/main/java/com/school/portal/exception/GlobalExceptionHandler.java` (@RestControllerAdvice) mapping domain exceptions to HTTP status codes per contracts
- [x] T009 [P] Create `src/test/java/com/school/portal/` base test support: abstract `AbstractIntegrationTest.java` annotated with `@Testcontainers`, `@DataJpaTest` (or `@SpringBootTest`) wiring a `@Container PostgreSQLContainer`

**Checkpoint**: Build compiles, tests run (empty suite passes), Docker Compose starts the DB.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core schema migrations and shared JPA infrastructure that ALL user
stories depend on. No user-story work can begin until this phase is complete.

**⚠️ CRITICAL**: Complete and verify every task in this phase before Phase 3.

- [x] T010 Create Flyway migration `src/main/resources/db/migration/V1__create_core_tables.sql` — tables: `students`, `teachers`, `reporting_periods` (with CHECK constraints), `courses` (with FK + unique constraint), `enrollments` (with unique constraint)
- [x] T011 Create Flyway migration `src/main/resources/db/migration/V2__create_grade_scale_tables.sql` — tables: `grade_scales`, `grade_scale_entries` (with CHECK constraint on min/max)
- [x] T012 Create Flyway migration `src/main/resources/db/migration/V3__create_exam_results.sql` — table: `exam_results` with all FK, CHECK constraints, and self-referencing `supersedes_id`; all FK indexes (`idx_exam_results_*`)
- [x] T013 Create Flyway migration `src/main/resources/db/migration/V4__create_grades.sql` — table: `grades` with FK to `exam_results` and `grade_scale_entries`; unique index on `exam_result_id`
- [x] T014 Create Flyway migration `src/main/resources/db/migration/V5__create_report_cards.sql` — tables: `report_cards` (unique on `student_id, reporting_period_id`), `report_card_items` (unique on `report_card_id, course_id`), `mv_refresh_log`; all FK indexes
- [x] T015 Create repeatable Flyway migration `src/main/resources/db/migration/R__create_views.sql` — views `v_active_exam_results`, `mv_course_aggregates` (materialized, with unique index), `v_student_period_summary`
- [x] T016 Create repeatable Flyway migration `src/main/resources/db/migration/R__create_triggers.sql` — PostgreSQL trigger `trg_revert_report_card_on_correction` on `exam_results` INSERT that sets `report_cards.status = 'DRAFT'` when an approved report card exists for the affected student and period
- [x] T017 [P] Create JPA entity `src/main/java/com/school/portal/domain/entity/Student.java` — all columns with explicit `@Column(name, nullable, length)`, business-key `equals/hashCode` on `studentNumber`, `@EntityListeners(AuditingEntityListener.class)`
- [x] T018 [P] Create JPA entity `src/main/java/com/school/portal/domain/entity/Teacher.java` — same standards as Student; business key: `employeeNumber`
- [x] T019 [P] Create JPA entity `src/main/java/com/school/portal/domain/entity/ReportingPeriod.java` — all columns; business key: `(name, academicYear)`
- [x] T020 [P] Create JPA entity `src/main/java/com/school/portal/domain/entity/Course.java` — FKs to `ReportingPeriod` and `Teacher` with `FetchType.LAZY`; business key: `(courseCode, reportingPeriod)`
- [x] T021 [P] Create JPA entity `src/main/java/com/school/portal/domain/entity/Enrollment.java` — FKs to `Student` and `Course`; `EnrollmentStatus` enum; business key: `(student, course)`
- [x] T022 [P] Create enum `src/main/java/com/school/portal/domain/enums/EnrollmentStatus.java` — values: `ACTIVE`, `WITHDRAWN`, `REPEAT`
- [x] T023 [P] Create enum `src/main/java/com/school/portal/domain/enums/ReportCardStatus.java` — values: `DRAFT`, `APPROVED`
- [x] T024 [P] Create Spring Data repositories `src/main/java/com/school/portal/repository/`: `StudentRepository`, `TeacherRepository`, `ReportingPeriodRepository`, `CourseRepository`, `EnrollmentRepository` — all extending `JpaRepository`
- [x] T025 Write integration test `src/test/java/com/school/portal/repository/MigrationIntegrityTest.java` — verifies all migrations apply cleanly, `flyway_schema_history` has no failures, FK indexes exist (Scenario 1 from quickstart.md)
- [x] T026 Write unit tests `src/test/java/com/school/portal/domain/entity/EntityConstraintTest.java` — verifies Bean Validation annotations on Student, Teacher, ReportingPeriod, Course, Enrollment using `Validator`
- [x] T027 Enable Spring Data JPA Auditing: create `src/main/java/com/school/portal/config/JpaConfig.java` with `@EnableJpaAuditing` and an `AuditorAware` bean that reads identity from the security context

**Checkpoint**: `./mvnw verify` passes. All migrations apply. All entity tests green. `v_active_exam_results` view exists. DB trigger installed.

---

## Phase 3: User Story 1 – Administrator Defines Grading Scale (Priority: P1) 🎯 MVP

**Goal**: Administrators can create, view, and manage grading scales with validated
non-overlapping score-boundary-to-letter-grade mappings. The grade scale is the
reference table that all downstream grade resolution depends on.

**Independent Test**: POST a valid 5-entry grade scale → `201 Created`. POST an
overlapping scale → `400 Bad Request`. Attempt to DELETE a scale referenced by a
`grades` row → `409 Conflict`. GET `/api/v1/grade-scales` returns the created scale.
(Quickstart Scenario 2)

- [x] T028 [P] Create JPA entity `src/main/java/com/school/portal/domain/entity/GradeScale.java` — columns per data model; `@OneToMany(mappedBy="gradeScale", fetch=LAZY)` to `GradeScaleEntry`; business key: `name`
- [x] T029 [P] Create JPA entity `src/main/java/com/school/portal/domain/entity/GradeScaleEntry.java` — columns per data model including `min_score NUMERIC(5,2)`, `max_score NUMERIC(5,2)`, `grade_points NUMERIC(3,2)`; `@ManyToOne(fetch=LAZY)` to `GradeScale`; business key: `(gradeScale, letterGrade)`
- [x] T030 [P] Create Spring Data repository `src/main/java/com/school/portal/repository/GradeScaleRepository.java` and `GradeScaleEntryRepository.java`
- [x] T031 [P] Create DTOs `src/main/java/com/school/portal/dto/GradeScaleDTO.java`, `GradeScaleEntryDTO.java`, `CreateGradeScaleRequest.java` with Bean Validation annotations
- [x] T032 [P] Create MapStruct mapper `src/main/java/com/school/portal/dto/GradeScaleMapper.java` mapping `GradeScale` ↔ `GradeScaleDTO`
- [x] T033 Implement `src/main/java/com/school/portal/service/GradeScaleService.java` — methods: `createGradeScale(request)` (validates non-overlapping entries before persist), `listActiveScales()`, `deleteGradeScale(id)` (guards against deletion when referenced by `grades`), `resolveLetterGrade(scaleId, rawScore)` (returns `UNGRADED` when no entry matches)
- [x] T034 [P] [US1] Write unit tests `src/test/java/com/school/portal/service/GradeScaleServiceTest.java` — overlapping boundary detection, gap detection, successful creation, deletion guard logic (mocked repository)
- [x] T035 [P] [US1] Write integration test `src/test/java/com/school/portal/repository/GradeScaleRepositoryTest.java` (@DataJpaTest + Testcontainers) — persist valid scale, attempt to delete referenced entry (FK constraint test)
- [x] T036 [US1] Implement REST controller `src/main/java/com/school/portal/controller/GradeScaleController.java` — `GET /api/v1/grade-scales`, `POST /api/v1/grade-scales`, `DELETE /api/v1/grade-scales/{id}` per `contracts/result-recording-api.md`
- [x] T037 [US1] Write integration test `src/test/java/com/school/portal/controller/GradeScaleControllerTest.java` (@SpringBootTest + Testcontainers) — Scenario 2 from quickstart.md (valid scale creation, overlapping rejection, delete conflict)

**Checkpoint**: User Story 1 fully functional and independently testable. Grade scales can be created and validated in isolation.

---

## Phase 4: User Story 5 – Database Lead Applies a Schema Migration (Priority: P1)

**Goal**: Schema migration workflow is verified end-to-end: Flyway versioning,
checksum integrity, and safe `NOT NULL` column addition with backfill.

**Independent Test**: Apply all migrations against a clean container; verify
`flyway_schema_history`; modify a migration file and verify Flyway rejects the run;
apply a migration that adds a `NOT NULL` column with a default. (Quickstart Scenario 1)

_Note: Most migration artefacts were created in Phase 2. This phase adds the
workflow verification layer and developer tooling._

- [x] T038 [P] [US5] Write integration test `src/test/java/com/school/portal/repository/FlywayChecksumTest.java` — starts a fresh Testcontainers PostgreSQL, applies all migrations, asserts all `flyway_schema_history.success = true` and no `installed_rank` gaps
- [x] T039 [P] [US5] Write integration test `src/test/java/com/school/portal/repository/FlywayChecksumTamperTest.java` — copies migrations to a temp dir, mutates one script, asserts `FlywayException` is thrown on Flyway.migrate() (validates FR-004)
- [x] T040 [US5] Create example `NOT NULL` additive migration `src/main/resources/db/migration/V6__add_display_order_to_grade_scale_entries.sql` — adds `display_order INTEGER NOT NULL DEFAULT 0` to `grade_scale_entries` (demonstrates safe NOT NULL addition pattern with DEFAULT backfill)
- [x] T041 [US5] Update `GradeScaleEntry` entity with `displayOrder` field; update `GradeScaleEntryDTO`; re-run migration test to confirm T038 still passes
- [x] T042 [P] [US5] Write developer runbook entry in `docs/erd/v1/migration-runbook.md` — migration naming convention, checklist before PR, how to regenerate ERD with SchemaSpy

**Checkpoint**: Flyway integrity enforcement is automated and verified. ERD runbook exists for team reference.

---

## Phase 5: User Story 2 – Teacher Records Exam Results (Priority: P1)

**Goal**: Teachers can submit, correct, and query exam results. All records are
immutable; corrections create linked rows via `supersedes_id`. Audit fields are
auto-populated. Closed-period and un-enrolled-student submissions are rejected.

**Independent Test**: Submit a score → verify audit fields. Correct it → verify
`supersedes_id` set, original unchanged. Submit to closed period → `409`. Submit
for un-enrolled student → `422`. Query `v_active_exam_results` → only corrected
row visible. (Quickstart Scenarios 3 and 6)

- [x] T043 [P] Create JPA entity `src/main/java/com/school/portal/domain/entity/ExamResult.java` — all columns per data model; `NUMERIC(5,2)` for `rawScore`/`maxScore`; self-referencing `@ManyToOne(fetch=LAZY) supersedes`; `@CreatedDate`, `@CreatedBy`, `@LastModifiedDate` auditing; business key: `(student, course, examName, createdAt)`
- [x] T044 [P] Create JPA entity `src/main/java/com/school/portal/domain/entity/Grade.java` — FK to `ExamResult` (unique) and `GradeScaleEntry`; snapshots `letterGrade` and `gradePoints`; business key: `examResult`
- [x] T045 [P] Create Spring Data repositories `src/main/java/com/school/portal/repository/ExamResultRepository.java` and `GradeRepository.java` — add custom queries: `findActiveByStudentAndCourse(studentId, courseId)`, `existsActiveByStudentCourseAndExam(...)`, `findSupersededChain(id)`
- [x] T046 [P] Create DTOs `src/main/java/com/school/portal/dto/ExamResultDTO.java`, `SubmitResultRequest.java`, `CorrectResultRequest.java`, `ResultsPageDTO.java`
- [x] T047 [P] Create MapStruct mapper `src/main/java/com/school/portal/dto/ExamResultMapper.java`
- [x] T048 Implement `src/main/java/com/school/portal/service/ExamResultService.java` — `submitResult(request, submittedBy)`: validates enrollment, validates period open, validates score range, persists `ExamResult`, resolves and persists `Grade` via `GradeScaleService.resolveLetterGrade`; `correctResult(originalId, request, correctedBy)`: loads original, creates new row with `supersedes_id`, re-resolves `Grade`; `queryResults(filter, pageable)`: queries `v_active_exam_results` via repository
- [x] T049 [P] [US2] Write unit tests `src/test/java/com/school/portal/service/ExamResultServiceTest.java` — enrollment validation, closed-period rejection, score-range rejection, correction immutability (original not mutated), supersession chain (mocked repositories)
- [x] T050 [P] [US2] Write integration test `src/test/java/com/school/portal/repository/ExamResultRepositoryTest.java` (@DataJpaTest + Testcontainers) — persist original, persist correction, query `v_active_exam_results`, assert only correction is active; verify `supersedes_id` FK exists
- [x] T051 [US2] Implement REST controller `src/main/java/com/school/portal/controller/ExamResultController.java` — `POST /api/v1/results`, `POST /api/v1/results/{id}/correct`, `GET /api/v1/results` per `contracts/result-recording-api.md`
- [x] T052 [US2] Write integration test `src/test/java/com/school/portal/controller/ExamResultControllerTest.java` (@SpringBootTest + Testcontainers) — Scenario 3 (submit → correct → verify immutability) and Scenario 6 (closed period rejection) from quickstart.md

**Checkpoint**: User Story 2 fully functional. Grade entry, correction, and audit trail work independently of aggregates and report cards.

---

## Phase 6: User Story 3 – System Calculates Grade Aggregates (Priority: P2)

**Goal**: The system exposes class-level aggregate statistics (per-student average,
class mean, rankings) derived from the `mv_course_aggregates` materialized view.
Aggregates use only active (non-superseded) results. Refresh is explicit and
timestamped. Stale-view state is surfaced to consumers.

**Independent Test**: Seed known scores for 5 students in one course, trigger
refresh, GET aggregates → verify averages and ranks against manual calculation.
Correct a score without refreshing → confirm `lastRefreshedAt` unchanged (FR-015).
(Quickstart Scenario 4)

- [x] T053 [P] Create repository `src/main/java/com/school/portal/repository/AggregateRepository.java` — native query to read from `mv_course_aggregates` and `mv_refresh_log`; method `refreshMaterializedView()` (calls `REFRESH MATERIALIZED VIEW CONCURRENTLY mv_course_aggregates`)
- [x] T054 [P] Create DTOs `src/main/java/com/school/portal/dto/CourseAggregateDTO.java`, `StudentRankingDTO.java`, `AggregateRefreshDTO.java`
- [x] T055 Implement `src/main/java/com/school/portal/service/AggregateService.java` — `getCourseAggregates(courseId)`: reads from `mv_course_aggregates` + `mv_refresh_log`, returns `CourseAggregateDTO`; `refreshAggregates(refreshedBy)`: calls native refresh, updates `mv_refresh_log`, returns `AggregateRefreshDTO`
- [x] T056 [P] [US3] Write unit tests `src/test/java/com/school/portal/service/AggregateServiceTest.java` — missing-score policy (student with no result excluded from ranking), stale-view timestamp behaviour (mocked repository)
- [x] T057 [P] [US3] Write integration test `src/test/java/com/school/portal/repository/AggregateRepositoryTest.java` (@SpringBootTest + Testcontainers) — seed 5 students × 3 exams, refresh, assert `percentage_avg` and `class_rank` match expected values within ±0.01; assert correction before refresh does not change `lastRefreshedAt`
- [x] T058 [US3] Implement REST controller endpoints in `src/main/java/com/school/portal/controller/ExamResultController.java` — `GET /api/v1/results/aggregates`, `POST /api/v1/results/aggregates/refresh` per `contracts/result-recording-api.md`
- [x] T059 [US3] Write integration test `src/test/java/com/school/portal/controller/AggregateControllerTest.java` (@SpringBootTest + Testcontainers) — Scenario 4 from quickstart.md (seed → refresh → assert aggregates → correct score → assert `lastRefreshedAt` unchanged)

**Checkpoint**: User Story 3 independently testable. Aggregates are DB-view-driven and refresh is explicit.

---

## Phase 7: User Story 4 – Administrator Generates Report Cards (Priority: P2)

**Goal**: Administrators can generate, review, and approve per-student per-period
report cards. Generation is idempotent and deterministic. Approval locks the record.
A grade correction after approval automatically reverts status to `DRAFT`. Incomplete
courses are flagged without aborting generation.

**Independent Test**: Seed a student with grades across 2 courses, generate report
card → `DRAFT` with both courses; generate again → same `id` returned (idempotency);
approve → `APPROVED`; correct a grade → status reverts to `DRAFT`. Generate for
student with one missing course → `DRAFT` with `hasIncompleteCourses: true`.
(Quickstart Scenario 5)

- [x] T060 [P] Create JPA entity `src/main/java/com/school/portal/domain/entity/ReportCard.java` — all columns per data model; `@Enumerated(EnumType.STRING) ReportCardStatus status`; unique constraint on `(student_id, reporting_period_id)`; business key: `(student, reportingPeriod)`
- [x] T061 [P] Create JPA entity `src/main/java/com/school/portal/domain/entity/ReportCardItem.java` — FK to `ReportCard` and `Course`; nullable `courseAverage`, `letterGrade`, `gradePoints`; unique constraint on `(report_card_id, course_id)`; business key: `(reportCard, course)`
- [x] T062 [P] Create Spring Data repositories `src/main/java/com/school/portal/repository/ReportCardRepository.java` and `ReportCardItemRepository.java` — add `findByStudentIdAndReportingPeriodId(studentId, periodId)`, `existsByStudentIdAndReportingPeriodIdAndStatus(...)` queries
- [x] T063 [P] Create DTOs `src/main/java/com/school/portal/dto/ReportCardDTO.java`, `StudentSummaryDTO.java`, `ReportingPeriodSummaryDTO.java`, `CourseResultDTO.java`, `GenerateReportCardRequest.java` — exactly matching `contracts/report-card-dto.md` structure
- [x] T064 [P] Create MapStruct mapper `src/main/java/com/school/portal/dto/ReportCardMapper.java` mapping `ReportCard` + `ReportCardItem` list → `ReportCardDTO`
- [x] T065 Implement `src/main/java/com/school/portal/service/ReportCardService.java` — `generateReportCard(studentId, periodId, generatedBy)`: checks for existing draft (returns it if found), reads `v_student_period_summary`, builds `ReportCard` + `ReportCardItem` rows, sets `hasIncompleteCourses`, persists; `approveReportCard(id, approvedBy)`: transitions `DRAFT→APPROVED`; `getReportCard(id)`; `listReportCards(studentId, periodId, status, pageable)`
- [x] T066 [P] [US4] Write unit tests `src/test/java/com/school/portal/service/ReportCardServiceTest.java` — idempotency check (existing draft returned), incomplete-course flag logic, approve-rejects-if-already-approved, null-safe overall-average when all courses incomplete (mocked repositories)
- [x] T067 [P] [US4] Write integration test `src/test/java/com/school/portal/repository/ReportCardRepositoryTest.java` (@DataJpaTest + Testcontainers) — unique constraint enforcement for concurrent generation (two inserts for same student/period → second throws `DataIntegrityViolationException`)
- [x] T068 [US4] Write integration test `src/test/java/com/school/portal/service/ReportCardServiceIntegrationTest.java` (@SpringBootTest + Testcontainers) — full Scenario 5 from quickstart.md: seed → generate → idempotency → approve → correct grade → assert DRAFT revert (trigger verification)
- [x] T069 [US4] Implement REST controller `src/main/java/com/school/portal/controller/ReportCardController.java` — `POST /api/v1/report-cards/generate`, `POST /api/v1/report-cards/{id}/approve`, `GET /api/v1/report-cards/{id}`, `GET /api/v1/report-cards` per `contracts/report-card-dto.md`
- [x] T070 [US4] Write integration test `src/test/java/com/school/portal/controller/ReportCardControllerTest.java` (@SpringBootTest + Testcontainers) — Scenario 5 via HTTP, assert DTO shape matches `contracts/report-card-dto.md` exactly (all fields present, null handling correct)

**Checkpoint**: User Story 4 fully functional. Report card lifecycle (generate → approve → auto-revert) works end-to-end.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Quality hardening, ERD generation, coverage enforcement, and
documentation that spans all stories.

- [x] T071 [P] Generate ERD using SchemaSpy against local Docker DB and save output to `docs/erd/v1/` — verify it covers all 10 tables and 3 views from data-model.md
- [x] T072 [P] Run JaCoCo (or equivalent) coverage report; ensure repository and service layer line coverage ≥ 80% (SC-008); fix any gaps
- [x] T073 [P] Add `EXPLAIN ANALYZE` query plan comments to `AggregateRepository.java` native queries — verify no sequential scans on `exam_results` when table > 10,000 rows (add a seed of 10,001 rows in a dedicated performance test `src/test/java/com/school/portal/repository/AggregatePerformanceTest.java`)
- [x] T074 [P] Audit all `@Column` annotations across all entities — confirm no implicit `name`, `nullable`, or `length` defaults remain; fix any found
- [x] T075 [P] Audit all `@OneToMany` / `@ManyToOne` relationships — confirm `FetchType.LAZY` everywhere; add `@NamedEntityGraph` where eager loading is genuinely needed with written justification comment
- [x] T076 Verify `equals()` / `hashCode()` implementations on all entities use business keys (not `id`) — write `EntityEqualityTest.java` in `src/test/java/com/school/portal/domain/` that creates two transient instances with same business key and asserts equality
- [x] T077 [P] Run full quickstart.md validation suite against the local Docker DB — document any deviations found
- [x] T078 [P] Add `@Transactional(readOnly = true)` to all read-only service methods; add `@Transactional` to all write methods; verify no `LazyInitializationException` surfaces in integration tests
- [x] T079 Confirm `docs/erd/v1/migration-runbook.md` is up to date with final migration count; add PR checklist (migration + ERD + test) as a GitHub PR template at `.github/pull_request_template.md`

**Checkpoint**: All quickstart scenarios pass. Coverage ≥ 80%. No implicit JPA mappings. ERD committed and reviewed.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately.
- **Phase 2 (Foundational)**: Depends on Phase 1 completion — **BLOCKS all user story phases**.
- **Phase 3 (US1 – Grade Scale)**: Depends on Phase 2 — no dependency on other stories.
- **Phase 4 (US5 – Migration Workflow)**: Depends on Phase 2 — no dependency on other stories.
- **Phase 5 (US2 – Exam Results)**: Depends on Phase 2 AND Phase 3 (requires `GradeScaleService.resolveLetterGrade`).
- **Phase 6 (US3 – Aggregates)**: Depends on Phase 5 (requires `ExamResult` data).
- **Phase 7 (US4 – Report Cards)**: Depends on Phase 5 (requires `ExamResult` and `Grade` data) and Phase 6 (consumes `v_student_period_summary` which joins `mv_course_aggregates`).
- **Phase 8 (Polish)**: Depends on all user story phases complete.

### User Story Dependencies

| Story | Depends On | Can Parallelise With |
|---|---|---|
| US1 – Grade Scale (P3) | Phase 2 | US5 |
| US5 – Migration Workflow (P4) | Phase 2 | US1 |
| US2 – Exam Results (P5) | Phase 2 + US1 (grade resolution) | — |
| US3 – Aggregates (P6) | US2 | — |
| US4 – Report Cards (P7) | US2 + US3 | — |

### Within Each Phase

- Tasks marked `[P]` within a phase can start in parallel.
- Entity tasks (T028/T029, T043/T044, T060/T061) MUST complete before their service tasks.
- Service tasks MUST complete before their controller tasks.
- Integration tests should run against a clean DB per test class.

---

## Parallel Opportunities

### Phase 2 Parallel Batch (after T010–T016 migrations done)

```
T017 Student entity
T018 Teacher entity        ← all run in parallel
T019 ReportingPeriod entity
T020 Course entity
T021 Enrollment entity
T022 EnrollmentStatus enum
T023 ReportCardStatus enum
T024 Repositories (5)
T025 Migration integrity test
T026 Entity constraint tests
```

### Phase 3 Parallel Batch

```
T028 GradeScale entity
T029 GradeScaleEntry entity   ← entities in parallel
T030 Repositories
T031 DTOs
T032 Mapper
```
Then T033 (service) → T036 (controller).
Tests T034, T035 can be written in parallel with T033.

### Phase 5 Parallel Batch

```
T043 ExamResult entity
T044 Grade entity             ← entities in parallel
T045 Repositories
T046 DTOs
T047 Mapper
```
Then T048 (service) → T051 (controller).
Tests T049, T050 can be written in parallel with T048.

---

## Implementation Strategy

### MVP (User Stories 1 + 5 + 2 only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (schema + base entities)
3. Complete Phase 3: US1 – Grade Scale ← **first independently demo-able piece**
4. Complete Phase 4: US5 – Migration Workflow verification
5. Complete Phase 5: US2 – Exam Results ← **core grade entry working**
6. **STOP and VALIDATE**: Run Quickstart Scenarios 1, 2, 3, 6
7. Demo: grade scales configured, scores entered, corrections immutable ✅

### Incremental Delivery

```
Phase 1+2 → Foundation ready (no user-visible features yet)
Phase 3   → Grade scale management live
Phase 4   → Migration workflow locked down
Phase 5   → Grade entry and correction live  ← DEMO POINT
Phase 6   → Class aggregates and rankings live
Phase 7   → Report card generation live      ← DEMO POINT
Phase 8   → Production-ready quality
```

### Parallel Team Strategy (2 developers)

After Phase 2 completes:
- **Developer A**: Phase 3 (US1) → Phase 5 (US2) → Phase 7 (US4)
- **Developer B**: Phase 4 (US5) → Phase 6 (US3) → Phase 8 (Polish)

---

## Notes

- `[P]` tasks operate on independent files — no concurrent file-edit conflicts.
- `[Story]` label maps each task to its user story for traceability and independent testing.
- Never modify a committed Flyway `V__` script — create a new `V{n+1}` instead.
- Commit after each phase checkpoint, not after every individual task.
- Run `./mvnw verify` (full suite) before every PR; do not merge on red CI.
- ERD must be regenerated and committed in the same PR as any `V__` migration.
