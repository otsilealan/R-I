# Documentation Index — School Management Portal (Database & Results Domain)

**Feature**: `001-database-results-baseline` | **Generated**: 2026-08-25 | **Version**: 1.0

---

## How to Use This Documentation (for AI Assistants)

This index is the **primary entry point** for understanding the codebase. Each section below tells you which file to consult and what question it answers — read this file first, then open only the specific file you need.

| Question you have | File to read |
|---|---|
| What does this system do and how is it structured? | [`architecture.md`](architecture.md) |
| What are all the classes and their responsibilities? | [`components.md`](components.md) |
| What REST endpoints exist? What do they accept/return? | [`interfaces.md`](interfaces.md) |
| What tables, columns, views, triggers exist? | [`data_models.md`](data_models.md) |
| How does a grade get submitted? How does a report card get generated? | [`workflows.md`](workflows.md) |
| What libraries are used and why? | [`dependencies.md`](dependencies.md) |
| How do I run and validate the system locally? | [`codebase_info.md`](codebase_info.md) |

---

## Table of Contents

| File | Summary |
|---|---|
| [`codebase_info.md`](codebase_info.md) | Tech stack, project structure, dev setup, test strategy, quickstart scenarios |
| [`architecture.md`](architecture.md) | Layered architecture, design principles, key decisions (immutability, MV pattern, DTO boundary) |
| [`components.md`](components.md) | Every class with its package, role, key methods, and relationships |
| [`interfaces.md`](interfaces.md) | All REST endpoints with request/response shapes, error codes, and validation rules |
| [`data_models.md`](data_models.md) | All 12 tables, 3 DB views, 2 triggers, enums, constraints, and JPA entity mappings |
| [`workflows.md`](workflows.md) | 5 end-to-end flows: grade scale setup, exam submission, correction, aggregation, report card lifecycle |
| [`dependencies.md`](dependencies.md) | All Maven dependencies with version rationale and usage context |

---

## Domain Summary

The **Database & Results domain** is one module of a larger School Management Portal Spring Boot application. It owns:

- **Schema management** via Flyway versioned migrations (PostgreSQL 15+)
- **Grade scale configuration** — score-boundary-to-letter-grade reference tables
- **Exam result recording** — append-only, immutable, audited score entries
- **Grade resolution** — mapping raw scores to letter grades via the active scale
- **Aggregate computation** — per-student averages and class rankings via materialized views
- **Report card generation** — deterministic, per-student, per-period summaries with DRAFT/APPROVED lifecycle

Other modules (authentication, PDF rendering, calendar management) are **out of scope** for this domain. This module consumes an authenticated identity context and exposes `ReportCardDTO` as the sole data contract to the rendering module.

---

## Key Design Invariants

These facts must hold true across all code changes:

1. **No floating-point scores** — all score columns are `NUMERIC(5,2)`.
2. **Grade records are immutable** — corrections create new rows via `supersedes_id`; originals are never updated or deleted.
3. **Aggregates come from DB views** — `mv_course_aggregates` (materialized) and `v_student_period_summary`; no application-layer re-computation.
4. **Report cards use DTOs only** — the rendering layer never queries the database; it receives `ReportCardDTO` exclusively.
5. **No in-memory DB in tests** — all integration tests use Testcontainers PostgreSQL.
6. **All FK columns are indexed**.
7. **All JPA relationships default to `FetchType.LAZY`**.
8. **`equals()`/`hashCode()` use business keys, not `id`**.

---

## File Relationships

```
index.md  ──► architecture.md  ──► components.md
                    │                    │
                    ▼                    ▼
              data_models.md  ◄──  workflows.md
                    │
                    ▼
              interfaces.md  ──► dependencies.md
```
