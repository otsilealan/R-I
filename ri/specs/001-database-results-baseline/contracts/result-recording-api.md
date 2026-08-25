# Contract: Result Recording API

**Feature**: `001-database-results-baseline`
**Consumers**: Teacher-facing UI module, Admin module
**Version**: v1

---

## Overview

The Result Recording API exposes endpoints through which teachers submit, correct,
and query exam results. All endpoints require an authenticated user context (provided
by the authentication module). The results domain does not own authentication — it
consumes an identity token to populate audit fields.

---

## Base Path

```
/api/v1/results
```

---

## Endpoints

### POST `/api/v1/results`

Submit a new exam result for a student.

**Request body**:

```json
{
  "studentId": 42,
  "courseId": 7,
  "examName": "Midterm",
  "rawScore": 78.50
}
```

| Field | Type | Required | Validation |
|---|---|---|---|
| `studentId` | long | yes | Must exist; student must be enrolled in `courseId` |
| `courseId` | long | yes | Must exist; associated reporting period must be open |
| `examName` | string | yes | 1–200 characters |
| `rawScore` | decimal | yes | ≥ 0 and ≤ course `max_score`; max 2 decimal places |

**Success response** — `201 Created`:

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

**Error responses**:

| Status | Condition |
|---|---|
| `400 Bad Request` | Validation failure (invalid score range, missing field) |
| `403 Forbidden` | Authenticated user is not assigned to this course |
| `404 Not Found` | Student or course not found |
| `409 Conflict` | Reporting period is closed |
| `422 Unprocessable Entity` | Student not enrolled in the course |

---

### POST `/api/v1/results/{id}/correct`

Submit a correction to an existing exam result. Creates a new row that supersedes
the given `id`.

**Path parameter**: `id` — the ID of the result being corrected.

**Request body**:

```json
{
  "rawScore": 82.00,
  "correctionReason": "Marking error on question 3"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `rawScore` | decimal | yes | New score; same validation as POST |
| `correctionReason` | string | no | Up to 500 characters; stored in `exam_name` suffix for audit |

**Success response** — `201 Created`: same shape as POST response, with
`"supersedesId": {id}`.

**Error responses**:

| Status | Condition |
|---|---|
| `404 Not Found` | Original result not found |
| `409 Conflict` | Reporting period is closed |

---

### GET `/api/v1/results`

Query active results. Supports filtering.

**Query parameters**:

| Parameter | Type | Description |
|---|---|---|
| `courseId` | long | Filter by course |
| `studentId` | long | Filter by student |
| `examName` | string | Filter by exam name (exact match) |
| `page` | int | 0-indexed page number (default: 0) |
| `size` | int | Page size (default: 20, max: 100) |

**Success response** — `200 OK`:

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

### GET `/api/v1/results/aggregates`

Return class-level aggregate statistics for a course.

**Query parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `courseId` | long | yes | Course to aggregate |

**Success response** — `200 OK`:

```json
{
  "courseId": 7,
  "courseName": "Mathematics",
  "studentCount": 32,
  "classAverage": 74.35,
  "lastRefreshedAt": "2026-08-25T14:00:00Z",
  "studentRankings": [
    {
      "studentId": 42,
      "studentName": "Jane Doe",
      "average": 82.00,
      "rank": 3
    }
  ]
}
```

**Note**: `lastRefreshedAt` reflects the last time `mv_course_aggregates` was
refreshed. If `null`, the materialized view has not been populated yet.

---

### POST `/api/v1/results/aggregates/refresh`

Trigger an explicit refresh of the `mv_course_aggregates` materialized view.
Restricted to `ADMIN` role.

**Success response** — `200 OK`:

```json
{
  "refreshedAt": "2026-08-25T17:20:00Z",
  "refreshedBy": "a.jones@school.edu"
}
```

---

## Grade Scale Endpoints

### GET `/api/v1/grade-scales`

List all active grade scales.

**Success response** — `200 OK`:

```json
[
  {
    "id": 1,
    "name": "Standard 2026",
    "entries": [
      { "letterGrade": "A+", "minScore": 90.00, "maxScore": 100.00, "gradePoints": 4.00, "pass": true },
      { "letterGrade": "A",  "minScore": 85.00, "maxScore": 89.99,  "gradePoints": 3.70, "pass": true }
    ]
  }
]
```

### POST `/api/v1/grade-scales`

Create a new grade scale with its entries. Restricted to `ADMIN` role.

**Validation**: Entries must be non-overlapping and must collectively cover the full
range 0–100. The service rejects the request if any gap or overlap is detected.

### DELETE `/api/v1/grade-scales/{id}`

Delete a grade scale. Fails with `409 Conflict` if any `grades` row references an
entry in this scale.

---

## Error Response Shape (all endpoints)

```json
{
  "timestamp": "2026-08-25T17:20:00Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Student 42 is not enrolled in course 7",
  "path": "/api/v1/results"
}
```
