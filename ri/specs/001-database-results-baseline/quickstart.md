# Quickstart & Validation Guide: Database & Results Baseline

**Feature**: `001-database-results-baseline`
**Date**: 2026-08-25

This guide documents how to set up, run, and validate the Database & Results domain
end-to-end. It is written for developers joining the project and for reviewers
verifying that the implementation meets the specification.

---

## Prerequisites

| Requirement | Version | Notes |
|---|---|---|
| Java JDK | 17 (LTS) | Verify: `java -version` |
| Maven or Gradle | Maven 3.9+ / Gradle 8+ | Project build tool |
| Docker | 24+ | Required for Testcontainers and local PostgreSQL |
| Docker Compose | v2+ | For local dev database |
| PostgreSQL client | 15+ (optional) | `psql` for manual inspection |

---

## Local Development Setup

### 1 — Start the local PostgreSQL instance

```bash
# From project root — starts a PostgreSQL 15 container on port 5432
docker compose up -d db
```

`docker-compose.yml` (excerpt — confirm actual location in project root):
```yaml
services:
  db:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: school_portal
      POSTGRES_USER: portal_user
      POSTGRES_PASSWORD: portal_pass
    ports:
      - "5432:5432"
```

### 2 — Run Flyway migrations

Migrations run automatically on Spring Boot startup. To run them manually:

```bash
# Maven
./mvnw flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/school_portal \
  -Dflyway.user=portal_user -Dflyway.password=portal_pass

# Gradle
./gradlew flywayMigrate
```

**Expected output**: Each migration listed with status `SUCCESS` in
`flyway_schema_history`.

### 3 — Start the application

```bash
# Maven
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Gradle
./gradlew bootRun --args='--spring.profiles.active=local'
```

Application starts on `http://localhost:8080`.

---

## Validation Scenarios

Run these scenarios in order — each one proves a distinct requirement from the spec.

---

### Scenario 1 — Schema integrity (FR-001 to FR-004)

**Verify migrations applied cleanly**:

```sql
-- Connect: psql -h localhost -U portal_user -d school_portal
SELECT version, description, installed_on, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

Expected: All rows have `success = true`. No gaps in `installed_rank`.

**Verify FK constraints exist** (spot-check):

```sql
SELECT conname, contype
FROM pg_constraint
WHERE conrelid = 'exam_results'::regclass AND contype = 'f';
```

Expected: At least `fk_exam_results_student_id`, `fk_exam_results_course_id`,
`fk_exam_results_supersedes_id`.

**Verify indexes on FK columns**:

```sql
SELECT indexname FROM pg_indexes
WHERE tablename = 'exam_results'
  AND indexname LIKE 'idx_%';
```

Expected: `idx_exam_results_student_id`, `idx_exam_results_course_id`,
`idx_exam_results_supersedes_id` all present.

---

### Scenario 2 — Grade scale creation and validation (FR-005, FR-006, FR-007)

**Create a valid grade scale**:

```bash
curl -X POST http://localhost:8080/api/v1/grade-scales \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-token>" \
  -d '{
    "name": "Standard 2026",
    "entries": [
      { "letterGrade": "A+", "minScore": 90.00, "maxScore": 100.00, "gradePoints": 4.00, "pass": true },
      { "letterGrade": "A",  "minScore": 80.00, "maxScore": 89.99,  "gradePoints": 3.70, "pass": true },
      { "letterGrade": "B",  "minScore": 70.00, "maxScore": 79.99,  "gradePoints": 3.00, "pass": true },
      { "letterGrade": "C",  "minScore": 60.00, "maxScore": 69.99,  "gradePoints": 2.00, "pass": true },
      { "letterGrade": "F",  "minScore": 0.00,  "maxScore": 59.99,  "gradePoints": 0.00, "pass": false }
    ]
  }'
```

Expected: `201 Created` with the persisted grade scale including assigned `id`.

**Attempt overlapping boundaries** (must fail):

```bash
curl -X POST http://localhost:8080/api/v1/grade-scales \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-token>" \
  -d '{
    "name": "Bad Scale",
    "entries": [
      { "letterGrade": "A", "minScore": 80.00, "maxScore": 100.00, "gradePoints": 4.00, "pass": true },
      { "letterGrade": "B", "minScore": 75.00, "maxScore": 90.00,  "gradePoints": 3.00, "pass": true }
    ]
  }'
```

Expected: `400 Bad Request` with a message identifying the overlapping range.

---

### Scenario 3 — Exam result submission and immutable correction (FR-008 to FR-010)

**Submit a valid score**:

```bash
curl -X POST http://localhost:8080/api/v1/results \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <teacher-token>" \
  -d '{ "studentId": 1, "courseId": 1, "examName": "Midterm", "rawScore": 78.50 }'
```

Expected: `201 Created`. Response body contains `id`, `rawScore: 78.50`,
`supersedesId: null`, `createdAt`, `createdBy`, `letterGrade`.

**Submit a correction**:

```bash
# Replace {resultId} with the id returned above
curl -X POST http://localhost:8080/api/v1/results/{resultId}/correct \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <teacher-token>" \
  -d '{ "rawScore": 82.00, "correctionReason": "Marking error" }'
```

Expected: `201 Created`. New result has `supersedesId: {resultId}`.

**Verify original is preserved** (immutability):

```sql
SELECT id, raw_score, supersedes_id FROM exam_results
WHERE course_id = 1 AND student_id = 1 AND exam_name = 'Midterm'
ORDER BY created_at;
```

Expected: Two rows — original `(id=X, raw_score=78.50, supersedes_id=NULL)` and
correction `(id=Y, raw_score=82.00, supersedes_id=X)`.

**Verify the view shows only the active result**:

```sql
SELECT id, raw_score FROM v_active_exam_results
WHERE course_id = 1 AND student_id = 1 AND exam_name = 'Midterm';
```

Expected: One row — `(id=Y, raw_score=82.00)`.

---

### Scenario 4 — Aggregate calculation (FR-013 to FR-015)

**Seed at least 5 students with results for course 1, then refresh**:

```bash
curl -X POST http://localhost:8080/api/v1/results/aggregates/refresh \
  -H "Authorization: Bearer <admin-token>"
```

Expected: `200 OK` with `refreshedAt` timestamp.

**Query aggregates**:

```bash
curl "http://localhost:8080/api/v1/results/aggregates?courseId=1" \
  -H "Authorization: Bearer <teacher-token>"
```

Expected: `classAverage` matches manual calculation; each student has a `rank`;
`lastRefreshedAt` matches the refresh call above.

**Verify materialized view is not stale when a correction is applied**:
After submitting a correction (Scenario 3), re-query aggregates. `lastRefreshedAt`
should be the timestamp of the most recent explicit refresh, not the correction time.
This confirms the view does not auto-refresh silently — refresh is explicit (FR-015).

---

### Scenario 5 — Report card generation and idempotency (FR-016 to FR-020)

**Generate a report card**:

```bash
curl -X POST http://localhost:8080/api/v1/report-cards/generate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-token>" \
  -d '{ "studentId": 1, "reportingPeriodId": 1 }'
```

Expected: `201 Created` or `200 OK` (if draft already exists). Body is a
`ReportCardDTO` with `status: "DRAFT"`, all enrolled courses listed, overall average
computed.

**Call generate again for the same student/period** (idempotency):

```bash
# Same request as above
```

Expected: `200 OK`. Same `id` returned. No duplicate row in `report_cards`.

**Approve the report card**:

```bash
curl -X POST http://localhost:8080/api/v1/report-cards/{id}/approve \
  -H "Authorization: Bearer <admin-token>"
```

Expected: `200 OK`, `status: "APPROVED"`, `approvedAt` populated.

**Submit a grade correction after approval — verify revert to DRAFT**:

```bash
curl -X POST http://localhost:8080/api/v1/results/{resultId}/correct \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <teacher-token>" \
  -d '{ "rawScore": 85.00 }'
```

Then fetch the report card:

```bash
curl http://localhost:8080/api/v1/report-cards/{id} \
  -H "Authorization: Bearer <admin-token>"
```

Expected: `status: "DRAFT"` — the correction triggered automatic revert (FR-018).

---

### Scenario 6 — Closed period rejection (edge case)

Update the test reporting period's `grade_submission_close` to a past timestamp
(direct DB update in dev/test env only):

```sql
UPDATE reporting_periods SET grade_submission_close = now() - interval '1 hour'
WHERE id = 1;
```

Attempt to submit a result:

```bash
curl -X POST http://localhost:8080/api/v1/results \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <teacher-token>" \
  -d '{ "studentId": 1, "courseId": 1, "examName": "Final", "rawScore": 70.00 }'
```

Expected: `409 Conflict` with a message including the period's close date.

---

## Running the Test Suite

### Unit tests only (no Docker required)

```bash
# Maven
./mvnw test -pl domain -Dgroups="unit"

# Gradle
./gradlew test --tests "*.unit.*"
```

### Integration tests (Testcontainers — requires Docker)

```bash
# Maven
./mvnw verify -Dgroups="integration"

# Gradle
./gradlew integrationTest
```

Expected: All tests pass. PostgreSQL container starts automatically via
Testcontainers; no manual DB setup needed for tests.

### Full suite with coverage report

```bash
# Maven
./mvnw verify -Pcoverage

# Gradle
./gradlew test jacocoTestReport
```

Coverage report written to `target/site/jacoco/index.html` (Maven) or
`build/reports/jacoco/test/html/index.html` (Gradle).

**Pass criterion**: Repository and service layer coverage ≥ 80% (SC-008).

---

## ERD Validation

After running migrations, generate a fresh ERD and compare to
`docs/erd/v1/school_portal_erd.png`:

```bash
# Using SchemaSpy (Docker image)
docker run --network host \
  -v "$(pwd)/docs/erd/v1:/output" \
  schemaspy/schemaspy:latest \
  -t pgsql \
  -host localhost -port 5432 \
  -db school_portal -u portal_user -p portal_pass \
  -schemas public -o /output
```

The generated `diagrams/summary/relationships.real.compact.png` should match the
documented data model. Any new table not shown in the existing ERD is a blocker for
merge.

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| Flyway checksum mismatch on startup | A committed migration file was edited after being applied | Revert the edit; create a new `V{n+1}` script instead |
| `LazyInitializationException` in tests | Entity accessed outside Hibernate session | Ensure service method is `@Transactional`; check fetch strategy |
| Testcontainers fails to start | Docker not running | Start Docker daemon |
| `409 Conflict` on grade submit | Reporting period closed | Check `grade_submission_close` in `reporting_periods` table |
| Aggregate view returns stale data | Materialized view not refreshed | Call `POST /api/v1/results/aggregates/refresh` |
| Report card not reverting to DRAFT | DB trigger not installed | Verify `V{n}__create_report_card_revert_trigger.sql` migration applied |
