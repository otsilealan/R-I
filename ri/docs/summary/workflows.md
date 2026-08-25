# Workflows — School Management Portal (Database & Results Domain)

---

## Workflow 1 — Administrator Creates a Grade Scale

```mermaid
sequenceDiagram
    actor Admin
    participant GradeScaleController
    participant GradeScaleService
    participant GradeScaleRepository
    participant DB as PostgreSQL

    Admin->>GradeScaleController: POST /api/v1/grade-scales {name, entries}
    GradeScaleController->>GradeScaleService: createGradeScale(request)
    GradeScaleService->>GradeScaleService: validateEntries() — check overlaps
    alt Overlapping entries
        GradeScaleService-->>GradeScaleController: throws BusinessRuleViolationException
        GradeScaleController-->>Admin: 400 Bad Request
    else Valid entries
        GradeScaleService->>GradeScaleRepository: save(GradeScale + entries)
        GradeScaleRepository->>DB: INSERT grade_scales, INSERT grade_scale_entries
        DB-->>GradeScaleRepository: persisted
        GradeScaleRepository-->>GradeScaleService: GradeScale
        GradeScaleService-->>GradeScaleController: GradeScaleDTO
        GradeScaleController-->>Admin: 201 Created
    end
```

**Key rule**: Entries are validated for non-overlapping `(min_score, max_score)` ranges before any insert. A grade scale entry cannot be deleted once any `grades` row references it.

---

## Workflow 2 — Teacher Submits an Exam Result

```mermaid
sequenceDiagram
    actor Teacher
    participant ExamResultController
    participant ExamResultService
    participant GradeScaleService
    participant DB as PostgreSQL

    Teacher->>ExamResultController: POST /api/v1/results {studentId, courseId, examName, rawScore}
    ExamResultController->>ExamResultService: submitResult(request)
    ExamResultService->>DB: Check enrollment (student enrolled in course?)
    alt Not enrolled
        ExamResultService-->>ExamResultController: throws BusinessRuleViolationException
        ExamResultController-->>Teacher: 422 Unprocessable Entity
    end
    ExamResultService->>DB: Check period open (grade_submission_close > now?)
    alt Period closed
        ExamResultService-->>ExamResultController: throws ClosedPeriodException
        ExamResultController-->>Teacher: 409 Conflict
    end
    ExamResultService->>ExamResultService: validateScoreRange(rawScore, maxScore)
    ExamResultService->>DB: INSERT exam_results (supersedes_id = NULL)
    ExamResultService->>GradeScaleService: resolveEntry(activeScaleId, rawScore)
    GradeScaleService->>DB: SELECT grade_scale_entries WHERE min_score <= pct <= max_score
    DB-->>GradeScaleService: GradeScaleEntry
    ExamResultService->>DB: INSERT grades (exam_result_id, grade_scale_entry_id, letter_grade, grade_points)
    ExamResultService-->>ExamResultController: ExamResultDTO
    ExamResultController-->>Teacher: 201 Created
```

---

## Workflow 3 — Teacher Corrects an Exam Result

```mermaid
sequenceDiagram
    actor Teacher
    participant ExamResultController
    participant ExamResultService
    participant DB as PostgreSQL

    Teacher->>ExamResultController: POST /api/v1/results/{id}/correct {rawScore}
    ExamResultController->>ExamResultService: correctResult(id, request)
    ExamResultService->>DB: SELECT exam_results WHERE id = ?
    alt Not found
        ExamResultService-->>ExamResultController: throws ResourceNotFoundException
        ExamResultController-->>Teacher: 404 Not Found
    end
    ExamResultService->>DB: INSERT exam_results (supersedes_id = original.id, rawScore = new)
    Note over DB: DB trigger fires:<br/>trg_revert_report_card_on_correction<br/>Sets APPROVED report cards → DRAFT
    ExamResultService->>DB: INSERT grades (for correction row)
    ExamResultService-->>ExamResultController: ExamResultDTO (correction, supersedesId set)
    ExamResultController-->>Teacher: 201 Created
```

**Immutability guarantee**: The original `exam_results` row is never modified. The view `v_active_exam_results` automatically excludes it (the correction row references it via `supersedes_id`).

---

## Workflow 4 — Aggregate Refresh and Query

```mermaid
sequenceDiagram
    actor Admin
    participant ExamResultController
    participant AggregateService
    participant AggregateRepository
    participant DB as PostgreSQL

    Admin->>ExamResultController: POST /api/v1/results/aggregates/refresh
    ExamResultController->>AggregateService: refreshAggregates(refreshedBy)
    AggregateService->>AggregateRepository: refreshMaterializedView()
    AggregateRepository->>DB: REFRESH MATERIALIZED VIEW CONCURRENTLY mv_course_aggregates
    DB-->>AggregateRepository: done
    AggregateService->>AggregateRepository: recordRefresh("mv_course_aggregates", refreshedBy)
    AggregateRepository->>DB: UPSERT mv_refresh_log
    AggregateService-->>ExamResultController: AggregateRefreshDTO {refreshedAt, refreshedBy}
    ExamResultController-->>Admin: 200 OK

    Note over Admin,DB: Later — query aggregates

    Admin->>ExamResultController: GET /api/v1/results/aggregates?courseId=7
    ExamResultController->>AggregateService: getCourseAggregates(7)
    AggregateService->>AggregateRepository: findCourseAggregates(7)
    AggregateRepository->>DB: SELECT * FROM mv_course_aggregates WHERE course_id = 7
    AggregateService->>AggregateRepository: findLastRefreshedAt("mv_course_aggregates")
    AggregateRepository->>DB: SELECT last_refreshed_at FROM mv_refresh_log
    AggregateService-->>ExamResultController: CourseAggregateDTO {classAverage, rankings, lastRefreshedAt}
    ExamResultController-->>Admin: 200 OK
```

**Important**: Refreshing the materialized view is **always explicit**. A grade correction does **not** automatically refresh `mv_course_aggregates`. The `lastRefreshedAt` timestamp reflects only explicit refresh calls.

---

## Workflow 5 — Report Card Generation and Approval Lifecycle

```mermaid
stateDiagram-v2
    [*] --> DRAFT : generateReportCard()
    DRAFT --> APPROVED : approveReportCard()
    APPROVED --> DRAFT : DB trigger fires on grade correction
    DRAFT --> DRAFT : generateReportCard() called again\n(idempotent — same id returned)
```

### Detailed Generation Flow

```mermaid
sequenceDiagram
    actor Admin
    participant ReportCardController
    participant ReportCardService
    participant AggregateRepository
    participant DB as PostgreSQL

    Admin->>ReportCardController: POST /api/v1/report-cards/generate {studentId, periodId}
    ReportCardController->>ReportCardService: generateReportCard(studentId, periodId)
    ReportCardService->>DB: SELECT report_cards WHERE student_id=? AND reporting_period_id=?
    alt DRAFT already exists
        ReportCardService-->>ReportCardController: existing ReportCardDTO
        ReportCardController-->>Admin: 200 OK (same id — idempotent)
    else No existing card
        ReportCardService->>AggregateRepository: findCourseAggregates via v_student_period_summary
        AggregateRepository->>DB: SELECT * FROM v_student_period_summary WHERE student_id=? AND reporting_period_id=?
        DB-->>AggregateRepository: List of course summaries
        ReportCardService->>ReportCardService: compute overallAverage, hasIncompleteCourses
        ReportCardService->>DB: INSERT report_cards (status='DRAFT')
        ReportCardService->>DB: INSERT report_card_items (one per enrolled course)
        ReportCardService-->>ReportCardController: ReportCardDTO
        ReportCardController-->>Admin: 201 Created
    end
```

### Approval and Auto-Revert

1. **Approve**: `POST /api/v1/report-cards/{id}/approve` → status `DRAFT → APPROVED`, `approved_at` and `approved_by` set.
2. **Auto-revert**: When a teacher submits a grade correction for a student whose report card is `APPROVED`, the PostgreSQL trigger `trg_revert_report_card_on_correction` fires on the `exam_results` INSERT and sets `status = 'DRAFT'`, clears `approved_at`/`approved_by`. No application code is involved in the revert.
3. **Idempotency**: `(student_id, reporting_period_id)` UNIQUE constraint on `report_cards` prevents duplicate rows. The service returns the existing DRAFT rather than inserting a new one.

---

## Workflow 6 — Schema Migration

```mermaid
flowchart TD
    A[Developer writes V{n+1}__description.sql] --> B[Test against clean Docker DB]
    B --> C{All migrations succeed?}
    C -- No --> D[Fix migration script]
    D --> B
    C -- Yes --> E[Regenerate ERD with SchemaSpy]
    E --> F[Update JPA entity with new column]
    F --> G[Run full test suite — mvnw verify]
    G --> H{Coverage ≥ 80%?}
    H -- No --> I[Add missing tests]
    I --> G
    H -- Yes --> J[Open PR with migration + ERD + entity update]
    J --> K[Reviewer confirms schema constraints match entity annotations]
    K --> L[Merge]
```

**Rules**:
- Never edit a committed `V__` script — Flyway checksum validation will reject it.
- `R__` scripts (views, triggers) can be updated; their checksum updates on re-run.
- ERD must be regenerated in the same PR as every `V__` migration (SC-009).
- Safe NOT NULL addition: `ALTER TABLE ... ADD COLUMN col TYPE NOT NULL DEFAULT value` — the DEFAULT backfills existing rows atomically.
