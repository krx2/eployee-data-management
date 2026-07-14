# Plan implementacji — Employee Data Service

Architektura: Java + Spring Boot, PostgreSQL na Dockerze.

Wybrane warianty modułów:

| Moduł | Wybrany wariant |
|---|---|
| Model danych | A — anemiczna encja JPA + Spring Data Repository |
| Obsługa danych wrażliwych (SSN) | B — szyfrowanie symetryczne AES-256-GCM |
| REST API | B — `ProblemDetail` (RFC 9457) |
| Persystencja | B — Flyway (wersjonowane migracje) |
| Testy | A (unit + `@WebMvcTest`), w drugiej kolejności B (Testcontainers) |

## Mapowanie wymagań na moduły

| Wymaganie (z zadania) | Moduł/komponent |
|---|---|
| 1. Model danych | `employee` (Entity + DTO) |
| 2. Bezpieczne SSN | `crypto` (AttributeConverter/Service) |
| 3. REST API | `employee` (Controller) |
| 4. Persystencja | PostgreSQL + Docker Compose + Flyway |
| 5. Walidacja/błędy | Bean Validation + `GlobalExceptionHandler` |
| 6. Testy | JUnit/Mockito + `@WebMvcTest` (+ Testcontainers) |

## Uzasadnienie wyboru wariantów

### Model danych — A (anemiczna encja JPA)
Prostsza, mniej boilerplate'u niż pełny podział domain model / persistence model (opcja B z hexagonal architecture). Uzasadnione skalą projektu (jedna encja) i czasem na zadanie ("kilka godzin"). DTO i tak oddziela encję od odpowiedzi API, więc SSN nie wycieka przez serializację mimo anemicznego modelu.

### Obsługa SSN — B (AES-256-GCM, szyfrowanie)
Alternatywa: hashing jednokierunkowy (BCrypt/Argon2) — odrzucony, bo SSN ma zbyt niską entropię (podatność na rainbow table/brute-force nawet z adaptacyjnym hashem) i nieodwracalność uniemożliwia legalne przypadki użycia (np. maskowanie, integracja z systemem płacowym). Szyfrowanie symetryczne pozwala na kontrolowany decrypt i przygotowuje grunt pod rotację kluczy (`key_version`), przy założeniu, że klucz jest zarządzany poza repozytorium (zmienna środowiskowa / secret manager).

### REST API — B (ProblemDetail)
Standard RFC 9457 wbudowany w Spring 6+/Boot 4 — mniej własnego kodu niż customowy `ErrorResponse`, spójny z resztą ekosystemu Springa.

### Persystencja — B (Flyway)
Wersjonowany schemat zamiast `hibernate.ddl-auto=update` — pokazuje świadome zarządzanie zmianami schematu, taniej kosztuje niż `ddl-auto` przy ocenie jakości kodu.

### Testy — A, potem B
Najpierw szybkie testy jednostkowe/slice (szyfrowanie, walidacja, `@WebMvcTest` kontrolera) — pokrywają wymaganie minimalne przy najniższym koszcie czasowym. Test integracyjny z Testcontainers (pełny przepływ `POST`→baza→`GET`) dogrywany w drugiej kolejności, jeśli zostaje czas — mocniejszy sygnał jakości, ale kosztowniejszy.

## Etapy implementacji

Kolejność odzwierciedla zależności — każdy etap da się zweryfikować osobno.

### Etap 0 — Szkielet infrastruktury
Cel: projekt się buduje i łączy z bazą, zanim pojawi się logika.

- `pom.xml`: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `org.postgresql:postgresql`, `flyway-core` + `flyway-database-postgresql`, (testy) `testcontainers:postgresql`, `testcontainers:junit-jupiter`.
- `docker-compose.yml`: serwis `postgres:16-alpine` (port 5432, env `POSTGRES_DB/USER/PASSWORD`, wolumen na dane).
- `application.properties`: `spring.datasource.url/username/password`, `spring.jpa.hibernate.ddl-auto=validate` (schemat wyłącznie z Flyway).

Weryfikacja: `docker compose up -d` + `mvn spring-boot:run` startuje bez błędów.

### Etap 1 — Persystencja: schemat (Flyway)
Spełnia wymaganie #4.

- `src/main/resources/db/migration/V1__create_employee_table.sql`:
  - `id UUID PRIMARY KEY`
  - `first_name`, `last_name VARCHAR NOT NULL`
  - `date_of_birth DATE NOT NULL`
  - `gender VARCHAR NOT NULL`
  - `ssn_ciphertext VARCHAR NOT NULL` (base64 AES-GCM: nonce+ciphertext+tag)
  - `key_version SMALLINT NOT NULL DEFAULT 1` (przygotowanie pod rotację klucza)

Weryfikacja: Flyway uruchamia migrację przy starcie appki, tabela widoczna w `psql`.

### Etap 2 — Model danych (anemiczna encja JPA)
Spełnia wymaganie #1.

- `employee/Gender.java` — enum (`MALE, FEMALE, OTHER, UNSPECIFIED`).
- `employee/Employee.java` — encja JPA mapująca 1:1 na tabelę z Etapu 1 (pole `ssn` — konwerter szyfrujący podpinany w Etapie 3).
- `employee/EmployeeRepository.java` — `interface EmployeeRepository extends JpaRepository<Employee, UUID>`.

Uwaga: encja i konwerter szyfrujący (`@Convert`) są ze sobą sprzężone — Etapy 2 i 3 realizowane praktycznie razem.

### Etap 3 — Obsługa danych wrażliwych (AES-256-GCM)
Spełnia wymaganie #2 — kluczowy punkt oceny.

- `crypto/SsnEncryptionService.java` — `encrypt`/`decrypt`; klucz AES-256 z env (`APP_ENCRYPTION_KEY`, base64), losowy 96-bit nonce per operacja, format `base64(nonce || ciphertext || authTag)`.
- `crypto/SsnAttributeConverter.java` — `AttributeConverter<String, String>` podpięty na `Employee.ssn`.
- `crypto/SsnMasker.java` — `mask(plaintextSsn)` → `"***-**-1234"`, używana tylko przy mapowaniu do DTO.

Weryfikacja: unit test `SsnEncryptionService` (round-trip, różny ciphertext dla tego samego wejścia, brak oryginału w ciphertext) — jednocześnie pierwszy kawałek Etapu 6a.

### Etap 4 — Warstwa serwisowa i DTO
Most między modelem a API — gwarantuje, że SSN nigdy nie wycieka przez serializację.

- `employee/dto/EmployeeCreateRequest.java` (record) + adnotacje walidacyjne.
- `employee/dto/EmployeeResponse.java` (record) — bez plaintext SSN, opcjonalnie `maskedSsn`.
- `employee/EmployeeMapper.java` — request→encja, encja→response (przez `SsnMasker`, bez wywoływania `decrypt` bez potrzeby).
- `employee/EmployeeService.java` — `create`, `getById`, `list(Pageable)`; rzuca `EmployeeNotFoundException`.

### Etap 5 — REST API (ProblemDetail)
Spełnia wymagania #3 i #5.

- `employee/EmployeeController.java`:
  - `POST /employees` → 201 + `Location`, body `EmployeeResponse`.
  - `GET /employees/{id}` → 200 / 404.
  - `GET /employees?page=&size=` → 200, `Page<EmployeeResponse>`.
- Walidacja na `EmployeeCreateRequest`: `@NotBlank` (imię/nazwisko), `@NotNull @Past` (data urodzenia), `@NotNull` (gender), `@Pattern` (format SSN, np. `\d{3}-\d{2}-\d{4}`).
- `common/GlobalExceptionHandler.java` (`@RestControllerAdvice`) — `MethodArgumentNotValidException` → 400, `EmployeeNotFoundException` → 404, fallback → 500 bez wycieku szczegółów.
- `common/EmployeeNotFoundException.java`.

Weryfikacja: ręczne wywołania curl/Postman — 201/200/404/400 ze spójnym formatem błędu.

### Etap 6a — Testy: unit + web-slice
Pierwsza część wymagania #6, realizowana równolegle z etapami 3 i 5.

- `SsnEncryptionServiceTest` (Etap 3).
- `EmployeeValidationTest` — walidacja DTO.
- `EmployeeControllerWebMvcTest` (`@WebMvcTest` + `@MockBean EmployeeService`) — happy path POST/GET, 404, 400; weryfikacja braku plaintext SSN w JSON.

### Etap 6b — Testy: integracja z Testcontainers
Druga część wymagania #6 — dogrywana na końcu, jeśli zostaje czas.

- `EmployeeIntegrationTest` (`@SpringBootTest` + `@Testcontainers`, `PostgreSQLContainer`) — pełny przepływ POST → realna baza → weryfikacja, że `ssn_ciphertext` nie zawiera plaintextu → GET zwraca dane bez SSN.

### Etap 7 — README i porządki
- Instrukcja uruchomienia (`docker compose up -d`, `mvn spring-boot:run`, wymagana zmienna `APP_ENCRYPTION_KEY`).
- Uzasadnienie encryption vs hashing.
- Sekcja "co bym zrobił inaczej z większą ilością czasu".
- Nota o wykorzystaniu AI.
