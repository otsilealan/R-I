# Dependencies — School Management Portal (Database & Results Domain)

---

## Runtime Dependencies

| Dependency | Purpose | Key Rationale |
|---|---|---|
| `spring-boot-starter-data-jpa` | ORM (Hibernate 6.x) + Spring Data repositories | Project-wide framework choice |
| `spring-boot-starter-web` | Embedded Tomcat, REST controllers | REST API serving |
| `spring-boot-starter-validation` | Jakarta Bean Validation on DTOs and entities | Declarative input validation |
| `org.postgresql:postgresql` | PostgreSQL JDBC driver | Only supported DB (PostgreSQL 15+) |
| `org.flywaydb:flyway-core` | Versioned schema migrations | Constitution mandates Flyway; checksum enforcement prevents drift |
| `org.flywaydb:flyway-database-postgresql` | Flyway PostgreSQL dialect support | Required by Flyway 10.x for PostgreSQL |
| `com.zaxxer:HikariCP` | Connection pooling | Bundled with Spring Boot; fastest JDBC pool |
| `org.mapstruct:mapstruct` | Compile-time DTO mapping | Safer and zero-overhead vs runtime reflection mappers; prevents `LazyInitializationException` from leaked entities |
| `org.projectlombok:lombok` | Boilerplate reduction (`@Getter`, `@Builder`, etc.) | Reduces noise on entity and DTO classes |

---

## Test Dependencies

| Dependency | Purpose | Key Rationale |
|---|---|---|
| `spring-boot-starter-test` | JUnit 5, Mockito, AssertJ, Spring test support | Standard Spring Boot test bundle |
| `org.testcontainers:postgresql` | Disposable PostgreSQL container per test class | Constitution forbids H2; real DB required for constraint, trigger, and view tests |
| `org.testcontainers:junit-jupiter` | JUnit 5 integration for Testcontainers lifecycle | Automatic container start/stop per test class |

---

## Build Tooling

| Tool | Version | Notes |
|---|---|---|
| Maven | 3.9+ | Project build and dependency management |
| JaCoCo | Bundled via Maven plugin | Coverage enforcement: repository and service layers ≥ 80% |
| SchemaSpy | Docker image `schemaspy/schemaspy:latest` | ERD generation from live schema; run against local Docker DB |

---

## Dependency Notes

### Why MapStruct over ModelMapper

MapStruct generates plain Java method calls at compile time. ModelMapper uses runtime reflection. MapStruct advantages in this project:
- No performance cost at runtime.
- Compile-time errors on unmapped fields.
- Eliminates the risk of the rendering module receiving a JPA entity with uninitialized lazy collections.

### Why Testcontainers over H2

The schema uses PostgreSQL-specific features: `BIGSERIAL`, `TIMESTAMPTZ`, `NUMERIC`, `MATERIALIZED VIEW`, `REFRESH MATERIALIZED VIEW CONCURRENTLY`, and a PL/pgSQL trigger. H2's PostgreSQL compatibility mode does not support materialized views or PL/pgSQL triggers, making it unsuitable. The project constitution explicitly forbids H2 for this domain.

### Why Flyway `R__` Scripts for Views

View definitions change more frequently than table structures. Using repeatable Flyway scripts (`R__create_views.sql`, `R__create_triggers.sql`) allows `CREATE OR REPLACE VIEW` to be updated without creating a new `V{n+1}` migration each time. The checksum is still validated on each startup, catching unintended edits.

### Why `NUMERIC(5,2)` not `DOUBLE PRECISION`

IEEE 754 floating-point cannot represent many decimal fractions exactly. Cumulative aggregations (class averages across many scores) would accumulate rounding errors. `NUMERIC` is PostgreSQL's exact arbitrary-precision type. `NUMERIC(5,2)` allows scores up to 999.99 with exactly two decimal places.

---

## Version Pinning

All dependency versions are managed via `spring-boot-parent` BOM where possible. Explicit versions are pinned for:
- `flyway-database-postgresql` — must match `flyway-core` minor version exactly.
- `org.mapstruct:mapstruct` — must match `mapstruct-processor` version on the annotation processor path.
- `org.testcontainers:*` — all Testcontainers modules must use the same version to avoid classpath conflicts.
