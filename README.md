# Employee Data Service

A small backend microservice for storing and managing employee records for an internal HR system, with particular attention to how the social security number (SSN) is protected at rest and in transit.

## Tech stack

- Java 25, Spring Boot 4.1 (Web MVC, Data JPA, Validation)
- PostgreSQL 16 (via Docker Compose)
- Flyway (versioned schema migrations)
- AES-256-GCM (SSN encryption at rest)
- JUnit 5, Mockito, AssertJ, Testcontainers

## Running locally

### Prerequisites

- JDK 25
- Docker Desktop (for the database, and for the Testcontainers-based integration test)

### 1. Start the database

```bash
docker compose up -d
```

This starts a `postgres:16-alpine` container on port `5432` with database/user/password all set to `employee_data`.

### 2. Provide an encryption key

The SSN encryption key is **not** committed to the repository and is not defaulted to anything weak — the app fails fast on startup if it's missing. Generate a random AES-256 key (32 bytes, base64-encoded) and export it:

```bash
export APP_ENCRYPTION_KEY=$(openssl rand -base64 32)
```

On Windows PowerShell:

```powershell
$env:APP_ENCRYPTION_KEY = [Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
```

If you're running the app from an IDE, set `APP_ENCRYPTION_KEY` as an environment variable on the run configuration instead.

### 3. Run the application

```bash
./mvnw spring-boot:run
```

The API is available at `http://localhost:8080`.

### 4. Run the tests

```bash
export APP_ENCRYPTION_KEY=$(openssl rand -base64 32)
./mvnw test
```

Most tests need the Docker Postgres container from step 1 running. The integration test (`EmployeeIntegrationTest`) additionally starts its **own** disposable Postgres container via Testcontainers, so Docker must be running for that test to pass — no other manual setup is required for it.

## API

| Method | Path              | Description                          |
|--------|-------------------|--------------------------------------|
| POST   | `/employees`       | Create a new employee record         |
| GET    | `/employees/{id}`   | Retrieve a single employee            |
| GET    | `/employees`        | List employees (paginated)            |

Example:

```bash
curl -X POST http://localhost:8080/employees \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Jan","lastName":"Kowalski","dateOfBirth":"1990-01-01","gender":"MALE","socialSecurityNumber":"123-45-6789"}'
```

```json
{
  "id": "a92894c9-ba11-4bb0-a1aa-0dab9216ba13",
  "firstName": "Jan",
  "lastName": "Kowalski",
  "dateOfBirth": "1990-01-01",
  "gender": "MALE",
  "maskedSsn": "***-**-6789"
}
```

The full SSN is never present in any response — only `maskedSsn` (last 4 digits) is returned.

Errors follow [RFC 7807 `ProblemDetail`](https://www.rfc-editor.org/rfc/rfc7807):

```json
{
  "title": "Invalid request",
  "status": 400,
  "detail": "Validation failed",
  "errors": ["socialSecurityNumber: must match format XXX-XX-XXXX"]
}
```

## Technology choices and why

**PostgreSQL over a NoSQL store.** The data is small, fixed-shape, and relational in nature (a single well-defined `Employee` record) — there's no variable schema or document-nesting need that would justify a document store. PostgreSQL also gave a straightforward path to Testcontainers-based integration testing.

**Flyway over `hibernate.ddl-auto=update`.** A versioned migration (`V1__create_employee_table.sql`) is the schema's single source of truth; Hibernate is set to `ddl-auto=validate` so it only checks the entity mapping against what Flyway created, it never mutates the schema itself. This is a small amount of extra ceremony for a take-home exercise, but it's the difference between "the schema is whatever Hibernate inferred" and "the schema is an auditable, reviewable artifact" — the latter is how I'd want a real HR system's schema managed.

**AES-256-GCM encryption over one-way hashing for the SSN.** Both were explicitly allowed by the assignment; the choice comes down to a real tradeoff:

- *Hashing* (e.g. BCrypt/Argon2) is a good fit when you only ever need to *check* a value against a stored form (like a password) and never need it back. It's a poor fit for a 9-digit SSN: the input space is small enough (well under 10^9, and further constrained by real allocation rules) that even a slow, salted hash is within reach of an offline guessing attack if the database ever leaks. It's also strictly one-way — there's no legitimate way to later produce a masked view, or supply the real number to a downstream system (payroll, tax reporting) that legitimately needs it.
- *Encryption* (AES-256-GCM here) is reversible under a key that's kept outside the database, which is exactly the property needed for occasional legitimate re-use of the value, while the value at rest is still ciphertext an attacker can't recover without the key. GCM specifically gives authenticated encryption (tamper-evidence) and a fresh random nonce per row means two employees with the same SSN produce different ciphertext — no accidental leakage of duplicates.

The key is read from an environment variable (`APP_ENCRYPTION_KEY`) and never committed or defaulted; missing it is a hard startup failure rather than a silent fallback to something insecure. A `key_version` column is already in the schema so key rotation is possible later without a data migration (see "What I'd do differently" below — rotation itself isn't implemented yet).

**UUID primary keys over auto-increment.** Sequential integer IDs leak information (roughly how many employees exist, and the order they were created) through a public-facing API. UUIDs generated by Hibernate application-side avoid that at negligible cost for this data volume.

**`ProblemDetail` (RFC 7807) over a hand-rolled error DTO.** Spring 6/Boot 3+ ships this as a first-class return type from `@ExceptionHandler` methods, so it's less code than a custom error shape and it's a recognized standard rather than another bespoke JSON format for API consumers to learn.

**Package-by-feature-ish layering.** `employee` (entity, repository, service, controller, DTOs), `crypto` (encryption service, JPA converter, masking), `common` (cross-cutting exception handling). For a single-entity service this is a light structure, not a strict hexagonal/clean-architecture split — that felt like unnecessary ceremony at this scale, but the crypto concern is still cleanly isolated from the employee domain code.

## What I'd do differently with more time

- **Key rotation.** The `key_version` column exists but nothing reads or acts on it yet; a real rotation flow (re-encrypt on read with an old key, write with the current one) is missing.
- **Locale-independent validation messages.** Bean Validation's default messages are resolved against the JVM's default locale, which surfaced Polish validation messages in one of my manual test runs purely because of the host machine's locale. I'd pin `Locale.ROOT`/English explicitly (or make it content-negotiated) so API consumers get consistent messages regardless of where the service happens to run.
- **Deterministic lookup support for the SSN**, if a real requirement ever needed "does this SSN already exist" without a full-table decrypt scan — an additional HMAC-SHA256(SSN, separate secret) column purely for equality lookups, alongside the AES-GCM ciphertext used for the value itself.
- **Containerizing the app itself**, not just the database, so `docker compose up` alone (no local JDK) is enough to run the whole thing.
- **CI** (GitHub Actions) running `mvn test` — the Testcontainers-based integration test is exactly the kind of thing that should run on every push, not just locally.
- **OpenAPI/Swagger** documentation for the three endpoints.
- **`@ConfigurationProperties`** instead of a raw `@Value` for the encryption key, mostly for testability/override convenience in more complex configurations.

## AI tool usage

I used **Claude Code** (Anthropic's CLI agent) throughout, working stage-by-stage from an implementation plan we wrote together up front (data model, SSN encryption, REST API, persistence, tests), rather than asking for the whole service in one shot. Each stage was actually compiled and exercised against a real, running PostgreSQL container before moving on — not just "does it compile," but real `curl` calls against the running app and real rows inspected in `psql`.

That verification step mattered in practice: this project pins **Spring Boot 4.1**, which turned out to have quietly modularized several things a "Spring Boot 2.x/3.x textbook" approach gets wrong. The AI's first pass repeatedly reached for the familiar shape and was wrong in specific, checkable ways:

- `flyway-core` alone compiles fine but silently does nothing at runtime — the actual Spring autoconfiguration that wires up and runs Flyway moved to a separate `spring-boot-starter-flyway` module. This was only caught because the migration logs were conspicuously absent from a real app run, not because anything failed to compile.
- Testcontainers 2.x renamed its module artifacts (`org.testcontainers:postgresql` → `org.testcontainers:testcontainers-postgresql`) and made `PostgreSQLContainer` non-generic, breaking the "textbook" `PostgreSQLContainer<>` usage.
- `@MockBean` no longer exists in this Spring generation — the AI's first draft of the controller slice test used it, and it doesn't compile; the working replacement is `@MockitoBean` from a different package.
- Jackson's `ObjectMapper` moved to a `tools.jackson.databind` package in this Spring Boot generation, not the familiar `com.fasterxml.jackson.databind` one.

I rejected/corrected each of these rather than accepting the first plausible-looking code, specifically by actually running things (compiling, executing tests, hitting live endpoints) rather than trusting that syntactically reasonable Spring Boot code would behave the way an older mental model of Spring Boot suggests it should. That's the concrete "changed or rejected an AI suggestion" example for this exercise: not a design disagreement, but catching several instances where the generated code was outdated for the pinned framework version and only verification against a real running instance exposed it.
