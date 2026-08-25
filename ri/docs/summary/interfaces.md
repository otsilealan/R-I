# Interfaces — School Management Portal (Database & Results Domain)

**Base path**: `/api/v1`

---

## Grade Scale Endpoints

### `GET /api/v1/grade-scales`
List all active grade scales.

**Response 200:**
```json
[
  {
    "id": 1,
    "name": "Standard 2026",
    "entries": [
      { "letterGrade": "A+", "minScore": 90.00, "maxScore": 100.00, "gradePoints": 4.00, "pass": true },
      { "letterGrade": "A",  "minScore": 80.00, "maxScore": 89.99,  "gradePoints": 3.70, "pass": true }
    ]
  }
]
```

---

### `POST /api/v1/grade-scales`
Create a new grade scale. Role: `ADMIN`.

**Request:**
```json
{
  "name": "Standard 2026",
  "entries": [
    { "letterGrade": "A+", "minScore": 90.00, "maxScore": 100.00, "gradePoints": 4.00, "pass": true },
    { "letterGrade": "F",  "minScore": 0.00,  "maxScore": 59.99,  "gradePoints": 0.00, "pass": false }
  ]
}
```

**Validation**: Entries must be non-overlapping. Service rejects gaps or overlaps.

| Status | Condition |
|---|---|
| 201 | Created successfully |
| 400 | Overlapping/invalid entries |
| 409 | Name already exists |

---

### `DELETE /api/v1/grade-scales/{id}`
Delete a grade scale. Fails if any `grades` row references an entry in this scale.

| Status | Condition |
|---|---|
| 204 | Deleted |
| 404 | Not found |
| 409 | Referenced by existing grade records |

---

## Exam Result Endpoints

### `POST /api/v1/results`
Submit a new exam result for a student.

**Request:**
```json
{
  "studentId": 42,
  "courseId": 7,
  "examName": "Midterm",
  "rawScore": 78.50
}
```

| Field | Validation |
|---|---|
| `studentId` | Must exist; student must be enrolled in `courseId` |
| `courseId` | Must exist; associated reporting period must be open |
| `examName` | 1–200 characters |
| `rawScore` | ≥ 0 and ≤ course `max_score`; max 2 decimal places |

**Response 201:**
```json
{
  "id": 301,
  "studentId": 42,
  "courseId": 7,
  "examName": "Midterm",
  "rawScore": 78.50,
  "maxScore": 100.00,
  "supersedesId": null,
  "createdAt": "2026-08-25T15:30:00Z",
  "createdBy": "t.smith@school.edu",
  "letterGrade": "C+",
  "gradePoints": 2.30
}
```

| Status | Condition |
|---|---|
| 201 | Created |
| 400 | Validation failure (bad score range, missing field) |
| 404 | Student or course not found |
| 409 | Reporting period is closed |
| 422 | Student not enrolled in the course |

---

### `POST /api/v1/results/{id}/correct`
Submit a correction to an existing exam result. Creates a new row with `supersedes_id = {id}`.

**Request:**
```json
{
  "rawScore": 82.00,
  "correctionReason": "Marking error on question 3"
}
```

**Response 201:** Same shape as `POST /api/v1/results`, with `"supersedesId": {id}`.

| Status | Condition |
|---|---|
| 201 | Correction created |
| 404 | Original result not found |
| 409 | Reporting period is closed |

---

### `GET /api/v1/results`
Query active results (queries `v_active_exam_results`). Paginated.

**Query params**: `courseId`, `studentId`, `examName`, `page` (0-indexed, default 0), `size` (default 20, max 100)

**Response 200:**
```json
{
  "content": [
    {
      "id": 301,
      "studentId": 42,
      "studentName": "Jane Doe",
      "courseId": 7,
      "courseName": "Mathematics",
      "examName": "Midterm",
      "rawScore": 82.00,
      "maxScore": 100.00,
      "letterGrade": "B",
      "gradePoints": 3.00,
      "createdAt": "2026-08-25T15:30:00Z",
      "createdBy": "t.smith@school.edu"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "page": 0,
  "size": 20
}
```

---

## Aggregate Endpoints

### `GET /api/v1/results/aggregates`
Return class-level aggregate stats for a course (from `mv_course_aggregates`).

**Query params**: `courseId` (required)

**Response 200:**
```json
{
  "courseId": 7,
  "courseName": "Mathematics",
  "studentCount": 32,
  "classAverage": 74.35,
  "lastRefreshedAt": "2026-08-25T14:00:00Z",
  "studentRankings": [
    { "studentId": 42, "studentName": "Jane Doe", "average": 82.00, "rank": 3 }
  ]
}
```

`lastRefreshedAt` is `null` if the materialized view has never been refreshed.

| Status | Condition |
|---|---|
| 200 | OK |
| 404 | Course not found |

---

### `POST /api/v1/results/aggregates/refresh`
Trigger explicit refresh of `mv_course_aggregates`. Role: `ADMIN`.

**Response 200:**
```json
{
  "refreshedAt": "2026-08-25T17:20:00Z",
  "refreshedBy": "a.jones@school.edu"
}
```

---

## Report Card Endpoints

### `POST /api/v1/report-cards/generate`
Generate (or retrieve existing) report card for a student and period. **Idempotent** — returns the same `id` on repeated calls with unchanged grades.

**Request:**
```json
{
  "studentId": 42,
  "reportingPeriodId": 3
}
```

**Response 201 / 200:** Full `ReportCardDTO` (see Data Models for shape).

| Status | Condition |
|---|---|
| 201 | New report card created |
| 200 | Existing DRAFT returned unchanged |
| 404 | Student or period not found |

---

### `POST /api/v1/report-cards/{id}/approve`
Approve a DRAFT report card → transitions to `APPROVED`.

**Response 200:** `ReportCardDTO` with `"status": "APPROVED"`, `approvedAt` populated.

| Status | Condition |
|---|---|
| 200 | Approved |
| 404 | Not found |
| 409 | Already approved |

---

### `GET /api/v1/report-cards/{id}`
Retrieve a report card by ID.

**Response 200:** `ReportCardDTO`

---

### `GET /api/v1/report-cards`
List report cards. Paginated.

**Query params**: `studentId`, `reportingPeriodId`, `status` (`DRAFT`/`APPROVED`), `page`, `size`

**Response 200:** Paginated list of `ReportCardDTO`

---

## Standard Error Response Shape

All error responses follow this structure:

```json
{
  "timestamp": "2026-08-25T17:20:00Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Student 42 is not enrolled in course 7",
  "path": "/api/v1/results"
}
```

---

## Service API (Internal — for rendering module)

The `ReportCardService` exposes these methods as the contract with the rendering module:

```
ReportCardService.generateReportCard(studentId, periodId)   → ReportCardDTO
ReportCardService.approveReportCard(reportCardId, approvedBy) → ReportCardDTO
ReportCardService.getReportCard(reportCardId)               → ReportCardDTO
ReportCardService.listReportCards(studentId, periodId)      → List<ReportCardDTO>
```

The rendering module **must not** call any repository or execute any SQL directly.

---

## Rendering Module Invariants

1. `isIncomplete: true` courses must show "Results Pending" — never render `null` as `0`.
2. `DRAFT` status requires a visible "DRAFT — not official" watermark.
3. `APPROVED` status requires the approval date displayed.
4. `overallAverage` and `overallLetterGrade` may be `null` when all courses are incomplete — the renderer must handle this.
