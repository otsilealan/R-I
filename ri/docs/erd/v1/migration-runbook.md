# Migration Runbook — School Management Portal (Database & Results Domain)

**Version**: 1.0 | **Owner**: Member 4 – Database & Results Lead | **Date**: 2026-08-25

---

## Migration Naming Convention

All versioned migrations follow the pattern:

```
V{n}__{short_description}.sql
```

- `{n}` — next sequential integer after scanning existing V-scripts
- `__` — two underscores (Flyway requirement)
- `{short_description}` — snake_case, max 50 characters
- Repeatable scripts: `R__{description}.sql` (views, triggers, functions)

**Current migrations**:

| Script | Purpose |
|---|---|
| `V1__create_core_tables.sql` | students, teachers, reporting_periods, courses, enrollments |
| `V2__create_grade_scale_tables.sql` | grade_scales, grade_scale_entries |
| `V3__create_exam_results.sql` | exam_results (append-only, supersedes_id) |
| `V4__create_grades.sql` | grades (resolved letter grades) |
| `V5__create_report_cards.sql` | report_cards, report_card_items, mv_refresh_log |
| `V6__add_sort_priority_to_grade_scale_entries.sql` | Example: safe NOT NULL addition |
| `R__create_views.sql` | v_active_exam_results, mv_course_aggregates, v_student_period_summary |
| `R__create_triggers.sql` | trg_revert_report_card_on_correction |

---

## PR Checklist — Before Opening a Schema PR

- [ ] New migration file created: `V{next}__{description}.sql`
- [ ] Migration file tested against a clean local Docker DB (`docker compose up -d db`, then `./mvnw flyway:migrate`)
- [ ] If `NOT NULL` column added: migration includes `DEFAULT` or data-backfill step before NOT NULL enforcement
- [ ] ERD regenerated (see below) and committed under `docs/erd/v{version}/`
- [ ] ERD diff included in PR description
- [ ] Corresponding JPA entity updated with new column mapping (`@Column` explicit)
- [ ] All tests pass: `./mvnw verify`
- [ ] Coverage still ≥ 80% on service and repository layers
- [ ] At least one reviewer has confirmed schema constraints match entity annotations

---

## How to Regenerate the ERD (SchemaSpy)

Prerequisites: Docker running, local DB running (`docker compose up -d db`).

```bash
docker run --rm --network host \
  -v "%cd%/docs/erd/v1:/output" \
  schemaspy/schemaspy:latest \
  -t pgsql \
  -host localhost -port 5432 \
  -db school_portal \
  -u portal_user -p portal_pass \
  -schemas public \
  -o /output
```

On Windows PowerShell, replace `%cd%` with `${PWD}`.

The generated `diagrams/summary/relationships.real.compact.png` is the primary
artefact to commit. Copy or symlink it as `docs/erd/v1/school_portal_erd.png`.

---

## Safe NOT NULL Column Addition Pattern

Never add a bare `NOT NULL` column to a table with existing rows without a default:

```sql
-- WRONG — will fail on non-empty tables:
ALTER TABLE students ADD COLUMN middle_name VARCHAR(100) NOT NULL;

-- CORRECT — add with default first, then optionally drop default later:
ALTER TABLE students ADD COLUMN middle_name VARCHAR(100) NOT NULL DEFAULT '';
-- If default should not persist:
-- ALTER TABLE students ALTER COLUMN middle_name DROP DEFAULT;
```

---

## Never Edit a Committed V-Script

Once a `V__` migration has been applied and committed, it MUST NOT be modified.
Flyway stores the checksum and will halt on the next run with:

```
Validate failed: Migration checksum mismatch for migration version N
```

To fix a mistake: create `V{n+1}__fix_{description}.sql`.

---

## Contacts

- Database Lead: Member 4 (see project team roster)
- For urgent schema issues in production: escalate to project supervisor
