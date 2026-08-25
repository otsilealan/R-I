# Data Model: Database & Results Baseline

**Date**: 2026-08-25
**Feature**: `001-database-results-baseline`
**Contracts**: [result-recording-api.md](contracts/result-recording-api.md) |
[report-card-dto.md](contracts/report-card-dto.md)

---

## Entity Overview

```
students ──< enrollments >── courses ──< exam_results
                                   │
                              grade_scales ──< grade_scale_entries
                                   │
                         exam_results >── grades
                              │
                         reporting_periods
                              │
                         report_cards ──< report_card_items
```

---

## Tables

### `students`

Stores the identity record for each enrolled student.

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | Surrogate key |
| `student_number` | `VARCHAR(20)` | UNIQUE NOT NULL | Natural business key |
| `first_name` | `VARCHAR(100)` | NOT NULL | |
| `last_name` | `VARCHAR(100)` | NOT NULL | |
| `date_of_birth` | `DATE` | NOT NULL | |
| `email` | `VARCHAR(255)` | UNIQUE NOT NULL | |
| `created_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT now() | |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT now() | |

**Indexes**: `student_number` (unique), `email` (unique)
**JPA Entity**: `Student` — business key: `studentNumber`

---

### `teachers`

Stores identity records for teaching staff.

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | |
| `employee_number` | `VARCHAR(20)` | UNIQUE NOT NULL | Natural business key |
| `first_name` | `VARCHAR(100)` | NOT NULL | |
| `last_name` | `VARCHAR(100)` | NOT NULL | |
| `email` | `VARCHAR(255)` | UNIQUE NOT NULL | |
| `created_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT now() | |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT now() | |

**JPA Entity**: `Teacher` — business key: `employeeNumber`

---

### `reporting_periods`

Defines named academic intervals with open/close dates for grade submission.

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | |
| `name` | `VARCHAR(100)` | NOT NULL | e.g., "Semester 1 2026" |
| `academic_year` | `VARCHAR(9)` | NOT NULL | e.g., "2026-2027" |
| `start_date` | `DATE` | NOT NULL | |
| `end_date` | `DATE` | NOT NULL | |
| `grade_submission_close` | `TIMESTAMPTZ` | NOT NULL | Deadline for grade entry |
| `is_active` | `BOOLEAN` | NOT NULL DEFAULT true | |
| `created_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT now() | |

**Constraints**: `CHECK (end_date > start_date)`,
`CHECK (grade_submission_close >= end_date::timestamptz)`
**JPA Entity**: `ReportingPeriod` — business key: `(name, academicYear)`

---

### `courses`

An academic unit offered within a specific reporting period by one teacher.

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | |
| `course_code` | `VARCHAR(20)` | NOT NULL | e.g., "MATH101" |
| `course_name` | `VARCHAR(200)` | NOT NULL | |
| `reporting_period_id` | `BIGINT` | FK → `reporting_periods.id` NOT NULL | |
| `teacher_id` | `BIGINT` | FK → `teachers.id` NOT NULL | |
| `max_score` | `NUMERIC(5,2)` | NOT NULL DEFAULT 100.00 | Full-mark reference |
| `created_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT now() | |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT now() | |

**Indexes**: `idx_courses_reporting_period_id`, `idx_courses_teacher_id`
**Unique**: `(course_code, reporting_period_id)`
**JPA Entity**: `Course` — business key: `(courseCode, reportingPeriod)`

---

### `enrollments`

Junction table binding a student to a course for a specific period.

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | |
| `student_id` | `BIGINT` | FK → `students.id` NOT NULL | |
| `course_id` | `BIGINT` | FK → `courses.id` NOT NULL | |
| `status` | `VARCHAR(20)` | NOT NULL DEFAULT 'ACTIVE' | ACTIVE / WITHDRAWN / REPEAT |
| `enrolled_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT now() | |

**Unique**: `(student_id, course_id)` — one enrollment per student per course per period
(course already scoped to period, so this is sufficient)
**Indexes**: `idx_enrollments_student_id`, `idx_enrollments_course_id`
**JPA Entity**: `Enrollment` — business key: `(student, course)`

---

### `grade_scales`

Header record for a grading scheme, scoped to an institution (and optionally a period).

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | |
| `name` | `VARCHAR(100)` | UNIQUE NOT NULL | e.g., "Standard 2026" |
| `description` | `VARCHAR(500)` | NULL | |
| `is_active` | `BOOLEAN` | NOT NULL DEFAULT true | |
| `created_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT now() | |
| `created_by` | `VARCHAR(100)` | NOT NULL | User identity from auth context |

**JPA Entity**: `GradeScale` — business key: `name`

---

### `grade_scale_entries`

Individual score-boundary-to-letter-grade mappings within a scale.

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | |
| `grade_scale_id` | `BIGINT` | FK → `grade_scales.id` NOT NULL | |
| `letter_grade` | `VARCHAR(5)` | NOT NULL | e.g., "A+", "B", "F" |
| `min_score` | `NUMERIC(5,2)` | NOT NULL | Inclusive lower bound |
| `max_score` | `NUMERIC(5,2)` | NOT NULL | Inclusive upper bound |
| `grade_points` | `NUMERIC(3,2)` | NOT NULL | e.g., 4.00 for A |
| `pass_indicator` | `BOOLEAN` | NOT NULL DEFAULT true | |

**Constraints**: `CHECK (max_score > min_score)`;
non-overlapping boundaries enforced by service-layer validation before persist.
**Indexes**: `idx_grade_scale_entries_grade_scale_id`
**JPA Entity**: `GradeScaleEntry` — business key: `(gradeScale, letterGrade)`

---

### `exam_results`

Core append-only table. Each row is one score submission for one student in one course.
Corrections are new rows with `supersedes_id` pointing to the row being corrected.

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | |
| `student_id` | `BIGINT` | FK → `students.id` NOT NULL | |
| `course_id` | `BIGINT` | FK → `courses.id` NOT NULL | |
| `exam_name` | `VARCHAR(200)` | NOT NULL | e.g., "Midterm", "Final" |
| `raw_score` | `NUMERIC(5,2)` | NOT NULL | |
| `max_score` | `NUMERIC(5,2)` | NOT NULL | Snapshot of course max at entry time |
| `supersedes_id` | `BIGINT` | FK → `exam_results.id` NULL | NULL = original entry |
| `created_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT now() | |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT now() | |
| `created_by` | `VARCHAR(100)` | NOT NULL | Teacher identity |

**Constraints**: `CHECK (raw_score >= 0)`, `CHECK (raw_score <= max_score)`,
`CHECK (supersedes_id <> id)` (no self-reference)
**Indexes**: `idx_exam_results_student_id`, `idx_exam_results_course_id`,
`idx_exam_results_supersedes_id`
**JPA Entity**: `ExamResult` — business key: `(student, course, examName, createdAt)`

---

### `grades`

Resolved letter grade for each active exam result, derived via the active grade scale.

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | |
| `exam_result_id` | `BIGINT` | FK → `exam_results.id` UNIQUE NOT NULL | One grade per result |
| `grade_scale_entry_id` | `BIGINT` | FK → `grade_scale_entries.id` NOT NULL | Scale entry used |
| `letter_grade` | `VARCHAR(5)` | NOT NULL | Snapshot of letter at resolution time |
| `grade_points` | `NUMERIC(3,2)` | NOT NULL | Snapshot of grade points |
| `resolved_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT now() | |
| `resolved_by` | `VARCHAR(100)` | NOT NULL | System or user identity |

**Indexes**: `idx_grades_exam_result_id` (unique), `idx_grades_grade_scale_entry_id`
**JPA Entity**: `Grade` — business key: `examResult`

---

### `report_cards`

One generated record per student per reporting period. Unique constraint prevents
duplicate concurrent generation.

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | |
| `student_id` | `BIGINT` | FK → `students.id` NOT NULL | |
| `reporting_period_id` | `BIGINT` | FK → `reporting_periods.id` NOT NULL | |
| `status` | `VARCHAR(10)` | NOT NULL DEFAULT 'DRAFT' | DRAFT / APPROVED |
| `overall_average` | `NUMERIC(5,2)` | NULL | Computed at generation |
| `overall_grade_points` | `NUMERIC(3,2)` | NULL | GPA snapshot |
| `generated_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT now() | |
| `generated_by` | `VARCHAR(100)` | NOT NULL | Administrator identity |
| `approved_at` | `TIMESTAMPTZ` | NULL | Set on APPROVED transition |
| `approved_by` | `VARCHAR(100)` | NULL | |
| `has_incomplete_courses` | `BOOLEAN` | NOT NULL DEFAULT false | |

**Unique**: `(student_id, reporting_period_id)`
**Indexes**: `idx_report_cards_student_id`, `idx_report_cards_reporting_period_id`
**JPA Entity**: `ReportCard` — business key: `(student, reportingPeriod)`

---

### `report_card_items`

One row per course result included in a report card.

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | |
| `report_card_id` | `BIGINT` | FK → `report_cards.id` NOT NULL | |
| `course_id` | `BIGINT` | FK → `courses.id` NOT NULL | |
| `course_average` | `NUMERIC(5,2)` | NULL | NULL if incomplete |
| `letter_grade` | `VARCHAR(5)` | NULL | NULL if incomplete |
| `is_incomplete` | `BOOLEAN` | NOT NULL DEFAULT false | |

**Unique**: `(report_card_id, course_id)`
**Indexes**: `idx_report_card_items_report_card_id`, `idx_report_card_items_course_id`
**JPA Entity**: `ReportCardItem` — business key: `(reportCard, course)`

---

### `mv_refresh_log` (table, not a view)

Tracks last-refresh timestamp for each materialized view (FR-015).

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `view_name` | `VARCHAR(100)` | PK | |
| `last_refreshed_at` | `TIMESTAMPTZ` | NOT NULL | |
| `refreshed_by` | `VARCHAR(100)` | NOT NULL | |

---

## Views

### `v_active_exam_results` (regular view)

Returns only rows from `exam_results` that have not been superseded.

```sql
CREATE OR REPLACE VIEW v_active_exam_results AS
SELECT er.*
FROM exam_results er
WHERE NOT EXISTS (
    SELECT 1 FROM exam_results newer
    WHERE newer.supersedes_id = er.id
);
```

---

### `mv_course_aggregates` (materialized view)

Per-student per-course aggregate over the active results.
Refreshed explicitly after grade-entry batch operations.

```sql
CREATE MATERIALIZED VIEW mv_course_aggregates AS
SELECT
    aer.student_id,
    aer.course_id,
    COUNT(*)                        AS exam_count,
    AVG(aer.raw_score / aer.max_score * 100)  AS percentage_avg,
    RANK() OVER (
        PARTITION BY aer.course_id
        ORDER BY AVG(aer.raw_score / aer.max_score * 100) DESC
    )                               AS class_rank
FROM v_active_exam_results aer
GROUP BY aer.student_id, aer.course_id
WITH NO DATA;

CREATE UNIQUE INDEX ON mv_course_aggregates (student_id, course_id);
```

---

### `v_student_period_summary` (regular view)

Cross-course summary per student per reporting period, consumed during
report-card generation.

```sql
CREATE OR REPLACE VIEW v_student_period_summary AS
SELECT
    e.student_id,
    c.reporting_period_id,
    c.id                              AS course_id,
    ca.percentage_avg                 AS course_average,
    g.letter_grade,
    g.grade_points,
    (ca.percentage_avg IS NULL)       AS is_incomplete
FROM enrollments e
JOIN courses c ON c.id = e.course_id
LEFT JOIN mv_course_aggregates ca
    ON ca.student_id = e.student_id AND ca.course_id = c.id
LEFT JOIN grades g
    ON g.exam_result_id IN (
        SELECT id FROM v_active_exam_results
        WHERE student_id = e.student_id AND course_id = c.id
        ORDER BY created_at DESC LIMIT 1
    );
```

---

## State Transitions

### `ExamResult` (append-only — no state field)

```
Original row inserted (supersedes_id = NULL)
        │
        └── Correction submitted
                │
                └── New row inserted (supersedes_id = original.id)
                        │
                        └── Original row is now "superseded" (never deleted)
```

### `ReportCard.status`

```
[NOT EXISTS] ──► DRAFT ──► APPROVED
                  ▲              │
                  └──────────────┘
                   (grade correction on an
                    approved card reverts to DRAFT)
```

---

## Validation Rules

| Entity | Rule | Enforcement |
|---|---|---|
| `GradeScaleEntry` | No overlapping `(min_score, max_score)` ranges within a scale | Service layer before persist |
| `ExamResult` | `raw_score` ∈ [0, `max_score`] | DB CHECK + Bean Validation |
| `ExamResult` | Student must be enrolled in the course | Service layer query |
| `ExamResult` | Reporting period must be open (`grade_submission_close` > now) | Service layer |
| `ReportCard` | One per `(student, reporting_period)` | DB UNIQUE constraint |
| `GradeScaleEntry` | Cannot delete if referenced by `grades` | DB FK constraint + service guard |
| `Course` | `max_score` > 0 | DB CHECK |
| `ReportingPeriod` | `end_date` > `start_date` | DB CHECK |
