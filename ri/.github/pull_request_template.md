## Summary

<!-- Describe what this PR does and why. -->

## Type of change

- [ ] Schema migration (new/altered tables, views, triggers)
- [ ] JPA entity change
- [ ] Service/repository change
- [ ] Bug fix
- [ ] Refactor / polish
- [ ] Documentation

## Schema migration checklist (complete if this PR includes a Flyway migration)

- [ ] New migration file created as `V{n}__{description}.sql` — never edited an existing V-script
- [ ] Tested against a clean local Docker DB: `docker compose up -d db && ./mvnw flyway:migrate`
- [ ] `NOT NULL` column additions include a `DEFAULT` value or data-backfill step
- [ ] ERD regenerated and committed under `docs/erd/v{version}/`
- [ ] ERD diff screenshot or description included below

## JPA entity checklist (complete if this PR changes an entity)

- [ ] All `@Column` annotations specify `name`, `nullable`, and `length`/`precision` — no implicit defaults
- [ ] All relationships use `FetchType.LAZY` (or justification provided)
- [ ] `equals()` and `hashCode()` based on business key, not `id`

## Test checklist

- [ ] Unit tests added/updated for service logic
- [ ] Integration tests run against real PostgreSQL via Testcontainers — no H2
- [ ] `./mvnw verify` passes locally (all tests green, coverage ≥ 80%)

## ERD diff

<!-- Paste or link the updated ERD image, or describe what changed in the schema. -->

## Reviewer notes

<!-- Anything the reviewer should pay special attention to. -->
