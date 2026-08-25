# Contract: Report Card DTO

**Feature**: `001-database-results-baseline`
**Consumers**: Report rendering module (PDF/HTML generation), Admin UI
**Version**: v1

---

## Overview

The `ReportCardDTO` is the **only** data structure that crosses the boundary between
the Database & Results domain and the rendering layer. The rendering module MUST NOT
call any repository or database directly — it consumes this DTO exclusively.

The DTO is produced by `ReportCardService.generateReportCard(studentId, periodId)`
and by `ReportCardService.getApprovedReportCard(reportCardId)`.

---

## ReportCardDTO

```
ReportCardDTO
├── id                      Long
├── status                  String          "DRAFT" | "APPROVED"
├── generatedAt             OffsetDateTime
├── generatedBy             String
├── approvedAt              OffsetDateTime  (null if DRAFT)
├── approvedBy              String          (null if DRAFT)
├── hasIncompleteCourses    boolean
│
├── student                 StudentSummaryDTO
│   ├── id                  Long
│   ├── studentNumber       String
│   ├── firstName           String
│   ├── lastName            String
│   └── dateOfBirth         LocalDate
│
├── reportingPeriod         ReportingPeriodSummaryDTO
│   ├── id                  Long
│   ├── name                String
│   ├── academicYear        String
│   ├── startDate           LocalDate
│   └── endDate             LocalDate
│
├── overallAverage          BigDecimal      (null if all courses incomplete)
├── overallGradePoints      BigDecimal      (GPA, null if all incomplete)
├── overallLetterGrade      String          (null if all incomplete)
│
└── courseResults           List<CourseResultDTO>
    └── CourseResultDTO
        ├── courseId         Long
        ├── courseCode       String
        ├── courseName       String
        ├── teacherName      String
        ├── courseAverage    BigDecimal      (null if incomplete)
        ├── letterGrade      String          (null if incomplete)
        ├── gradePoints      BigDecimal      (null if incomplete)
        ├── classRank        Integer         (null if incomplete)
        ├── classSize        Integer
        └── isIncomplete     boolean
```

---

## JSON Representation

Example of a fully populated `ReportCardDTO` (APPROVED):

```json
{
  "id": 88,
  "status": "APPROVED",
  "generatedAt": "2026-08-25T16:00:00Z",
  "generatedBy": "a.jones@school.edu",
  "approvedAt": "2026-08-25T17:00:00Z",
  "approvedBy": "a.jones@school.edu",
  "hasIncompleteCourses": false,
  "student": {
    "id": 42,
    "studentNumber": "STU-2024-0042",
    "firstName": "Jane",
    "lastName": "Doe",
    "dateOfBirth": "2008-03-14"
  },
  "reportingPeriod": {
    "id": 3,
    "name": "Semester 1 2026",
    "academicYear": "2026-2027",
    "startDate": "2026-02-01",
    "endDate": "2026-06-30"
  },
  "overallAverage": 79.25,
  "overallGradePoints": 3.10,
  "overallLetterGrade": "B",
  "courseResults": [
    {
      "courseId": 7,
      "courseCode": "MATH101",
      "courseName": "Mathematics",
      "teacherName": "T. Smith",
      "courseAverage": 82.00,
      "letterGrade": "B",
      "gradePoints": 3.00,
      "classRank": 3,
      "classSize": 32,
      "isIncomplete": false
    },
    {
      "courseId": 11,
      "courseCode": "ENG101",
      "courseName": "English",
      "teacherName": "M. Brown",
      "courseAverage": 76.50,
      "letterGrade": "C+",
      "gradePoints": 2.30,
      "classRank": 12,
      "classSize": 30,
      "isIncomplete": false
    }
  ]
}
```

Example with an incomplete course:

```json
{
  "id": 91,
  "status": "DRAFT",
  "hasIncompleteCourses": true,
  "courseResults": [
    {
      "courseId": 15,
      "courseCode": "SCI101",
      "courseName": "Science",
      "teacherName": "R. Patel",
      "courseAverage": null,
      "letterGrade": null,
      "gradePoints": null,
      "classRank": null,
      "classSize": 28,
      "isIncomplete": true
    }
  ]
}
```

---

## Report Card Service API (internal)

The service boundary exposed to the rendering module and admin controller:

```
ReportCardService
│
├── generateReportCard(studentId: Long, periodId: Long): ReportCardDTO
│     Idempotent — returns existing DRAFT if one exists unchanged.
│     Throws ReportingPeriodNotFoundException if period not found.
│     Throws StudentNotFoundException if student not found.
│
├── approveReportCard(reportCardId: Long, approvedBy: String): ReportCardDTO
│     Transitions status DRAFT → APPROVED.
│     Throws ReportCardNotFoundException if not found.
│     Throws IllegalStateException if status is already APPROVED.
│
├── getReportCard(reportCardId: Long): ReportCardDTO
│     Fetches by ID regardless of status.
│
└── listReportCards(studentId: Long, periodId: Long): List<ReportCardDTO>
      Returns all report cards for a student, optionally filtered by period.
```

---

## Report Card REST Endpoints

### POST `/api/v1/report-cards/generate`

Generate (or retrieve existing) report card for a student and period.

**Request body**:
```json
{
  "studentId": 42,
  "reportingPeriodId": 3
}
```

**Response** — `200 OK` or `201 Created`: `ReportCardDTO`

### POST `/api/v1/report-cards/{id}/approve`

Approve a DRAFT report card.

**Response** — `200 OK`: `ReportCardDTO` with `"status": "APPROVED"`

**Errors**:
- `404 Not Found` — report card not found
- `409 Conflict` — already approved

### GET `/api/v1/report-cards/{id}`

Retrieve a report card by ID.

**Response** — `200 OK`: `ReportCardDTO`

### GET `/api/v1/report-cards`

List report cards with optional filtering.

**Query params**: `studentId`, `reportingPeriodId`, `status`, `page`, `size`

**Response** — `200 OK`: paginated list of `ReportCardDTO`

---

## Invariants the Rendering Module MUST Observe

1. **Never query the database.** All data comes from `ReportCardDTO` fields.
2. **`isIncomplete: true` courses MUST be rendered with a clear "Results Pending"
   indicator** — rendering null averages as "0" or "—" is incorrect.
3. **Status field drives display mode**: `DRAFT` cards MUST carry a visible
   "DRAFT — not official" watermark; `APPROVED` cards MUST carry the approval date.
4. **`overallAverage` / `overallLetterGrade` are `null` when all courses are
   incomplete** — the renderer MUST handle this gracefully.
