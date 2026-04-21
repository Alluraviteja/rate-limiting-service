# CLAUDE.md — Rate Limiting Service

Architectural guardrail for all code generation. Follow every rule strictly. When in doubt, ask before deviating.

> **Strict Mode:** If any request violates this architecture, refuse it, explain the specific violation, and propose a compliant alternative.

---

## Project Overview

A production-grade **rate limiting service** built on Spring Boot 4.x. It enforces configurable per-app token bucket rate limits, persists audit logs to PostgreSQL, and stores bucket state in Redis via Bucket4j + Lettuce. Downstream services call this service as a sidecar or gateway filter.

**Goals:**
- Per-app rate limiting with configurable capacity, refill rate, and refill period
- Redis-backed distributed bucket state (Bucket4j + Lettuce)
- Audit logging of every request (allowed and blocked) to PostgreSQL
- Admin API to manage `RateLimitPlan` records (CRUD)
- JWT-secured endpoints with role-based access
- Observable via structured JSON logs

---

## Architecture

### Layered Architecture (strict top-down)

```
HTTP Request
    │
    ▼
[Filter / Security]          ← JWT auth, request pre-processing
    │
    ▼
[Controller]                 ← Input validation, HTTP mapping, DTO in/out
    │
    ▼
[Service]                    ← Business logic, orchestration, transactions
    │
    ▼
[Repository]                 ← Data access only (JPA / Redis)
    │
    ▼
[Database / Redis]
```

**Rules:**
- Controllers MUST NOT contain business logic.
- Services MUST NOT extend `JpaRepository` or reference HTTP types (`HttpServletRequest` etc.) unless injected for a specific cross-cutting concern.
- Repositories MUST NOT contain business logic.
- No layer may skip a layer (e.g., Controller → Repository is forbidden).
- Filters/interceptors handle cross-cutting concerns (auth, logging, tracing).

---

## Package Structure

```
com.app.ratelimiter
├── config/              # Spring @Configuration classes (Redis, Security, Bucket4j, etc.)
├── controller/          # @RestController classes
├── dto/
│   ├── request/         # Inbound DTOs (validated with Bean Validation)
│   └── response/        # Outbound DTOs
├── exception/           # Custom exceptions + GlobalExceptionHandler
├── filter/              # Servlet filters (JWT extraction, request logging)
├── model/               # JPA @Entity classes
├── repository/          # Spring Data JPA interfaces
├── service/             # @Service classes (interfaces + implementations)
│   └── impl/
├── mapper/              # DTO ↔ Entity mappers (manual or MapStruct)
├── security/            # JWT utilities, UserDetailsService, SecurityConfig
└── util/                # Pure utility/helper classes (no Spring beans)
```

**Rules:**
- One class per file, always.
- Subpackages are allowed for grouping related classes (e.g., `service/ratelimit/`, `service/audit/`).
- No circular dependencies between packages.

---

## DTO Usage and Mapping

- **Never** expose `@Entity` classes directly in controller responses or request bodies.
- All controller methods accept request DTOs and return response DTOs.
- DTOs live in `dto/request/` and `dto/response/` respectively.
- DTOs are plain Java records or Lombok `@Data` classes — no JPA annotations.
- Mapping between DTO ↔ Entity is done in the `mapper/` package.
- Prefer manual mappers for clarity and compile-time safety; MapStruct is acceptable for large models.
- Mapper classes are Spring `@Component` beans named `<Entity>Mapper` (e.g., `RateLimitPlanMapper`).

**Example mapper contract:**
```java
@Component
public class RateLimitPlanMapper {
    public RateLimitPlan toEntity(RateLimitPlanRequest request) { ... }
    public RateLimitPlanResponse toResponse(RateLimitPlan entity) { ... }
}
```

---

## Validation Standards

- Use Jakarta Bean Validation (`jakarta.validation.*`) on all request DTOs.
- Apply `@Valid` on every controller method parameter that is a request body or path/query wrapper.
- Use constraint annotations directly on DTO fields: `@NotBlank`, `@NotNull`, `@Positive`, `@Min`, `@Max`, `@Size`, `@Pattern`.
- Create custom `@Constraint` validators only when built-in annotations are insufficient.
- Validation failures are handled globally in `GlobalExceptionHandler` — do NOT catch `MethodArgumentNotValidException` inside controllers.
- Never validate inside service methods what can be validated declaratively via Bean Validation.

**Example DTO:**
```java
public record RateLimitPlanRequest(
    @NotBlank String appId,
    @Positive @Max(1_000_000) int capacity,
    @Positive int refillRate,
    @Positive int refillPeriodSeconds,
    String description
) {}
```

---

## Exception Handling

### Custom Exception Hierarchy

```
RuntimeException
└── AppException                       ← base (message + HttpStatus)
    ├── ResourceNotFoundException       ← 404
    ├── ResourceAlreadyExistsException  ← 409
    ├── RateLimitExceededException      ← 429
    └── InvalidPlanConfigException      ← 422
```

All custom exceptions extend `AppException` and set their own default `HttpStatus`.

### Global Exception Handler

- Single `@RestControllerAdvice` class: `GlobalExceptionHandler` in `exception/`.
- Handles: `AppException`, `MethodArgumentNotValidException`, `ConstraintViolationException`, `NoHandlerFoundException`, `Exception` (fallback).
- Always returns a standard error response DTO:

```java
public record ErrorResponse(
    String status,
    int code,
    String message,
    Map<String, String> fieldErrors,   // populated for validation errors
    Instant timestamp
) {}
```

- Log `WARN` for 4xx errors, `ERROR` (with stack trace) for 5xx errors.
- Never expose stack traces or internal class names in the response body.

---

## Logging Standards

- Use **SLF4J** only: `private static final Logger log = LoggerFactory.getLogger(ClassName.class);`
- With Lombok: `@Slf4j` on the class is acceptable.
- **Never** use `System.out`, `System.err`, or `java.util.logging`.
- Log levels:
  - `ERROR` — unexpected exceptions, system failures
  - `WARN` — recoverable issues, rate limit exceeded, 4xx from downstream
  - `INFO` — service startup/shutdown, significant business events (plan created, plan disabled)
  - `DEBUG` — request/response details, bucket state transitions (disabled in production)
  - `TRACE` — low-level Redis/DB interactions (never in production)
- **Never** log sensitive data: passwords, tokens, full request bodies containing credentials.
- Always use parameterized logging: `log.info("Plan created for appId={}", appId)` — never string concatenation.
- Include `appId` and `clientIp` in log messages where applicable for traceability.
- Target structured JSON logging in production (configure Logback with `logstash-logback-encoder`).

---

## Security Guidelines

### Authentication
- Spring Security 6 with stateless JWT authentication.
- No `HttpSession` — `SessionCreationPolicy.STATELESS` always.
- JWT extracted from `Authorization: Bearer <token>` header in a `JwtAuthenticationFilter` (extends `OncePerRequestFilter`).
- Invalid or missing tokens return `401 Unauthorized` via `AuthenticationEntryPoint`.
- Insufficient permissions return `403 Forbidden` via `AccessDeniedHandler`.

### Roles
- `ROLE_ADMIN` — full CRUD on `RateLimitPlan`; access to audit logs
- `ROLE_SERVICE` — check rate limit endpoint only

### Security Configuration (SecurityFilterChain pattern)
```java
http
    .csrf(AbstractHttpConfigurer::disable)
    .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/v1/ratelimit/check").hasRole("SERVICE")
        .requestMatchers("/api/v1/plans/**").hasRole("ADMIN")
        .requestMatchers("/actuator/health").permitAll()
        .anyRequest().authenticated()
    )
    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

### JWT Utilities
- `JwtTokenProvider` in `security/` — handles token generation, parsing, validation.
- Use a strong secret (`HS256` minimum, prefer `RS256` with key pair in production).
- Token expiry, issuer, and audience must be validated on every request.
- Never store JWT secrets in source code — use environment variables or a secrets manager.

### General Security Rules
- Enable CORS explicitly via `CorsConfigurationSource` bean — never wildcard `*` in production.
- Sanitize all user input before use in dynamic queries.
- Use parameterized queries / Spring Data JPA — never `String`-concatenated JPQL or native SQL.
- Actuator endpoints: expose only `health` and `info` publicly; secure all others.

---

## Database Conventions

### PostgreSQL + JPA

- Table names: `snake_case`, plural (e.g., `rate_limit_plans`, `rate_limit_logs`).
- Column names: `snake_case`.
- Use `BIGSERIAL` for surrogate primary keys (mapped to `@GeneratedValue(strategy = IDENTITY)`).
- Timestamps: always `TIMESTAMP WITH TIME ZONE` in DDL, mapped to `Instant` in Java.
- Boolean columns: `BOOLEAN NOT NULL DEFAULT false/true` — never nullable booleans.
- String columns: specify `length` in `@Column` and match it in DDL `VARCHAR(n)`.
- Never use `@GeneratedValue(strategy = AUTO)` — always be explicit.
- `ddl-auto: none` always — schema is exclusively managed by Liquibase.
- Use `FetchType.LAZY` for all relationships; fetch eagerly only with explicit `JOIN FETCH` in queries.
- Avoid N+1 queries — use `@EntityGraph` or JPQL join fetch where collections are needed.

### Liquibase

- All schema changes via Liquibase changesets in `src/main/resources/db/changelog/`.
- Master changelog: `db/changelog/master.xml` — includes all sub-changelogs.
- Sub-changelogs per concern: `tables/`, `indexes/`, `data/`.
- Changeset ID format: zero-padded 13-digit integer (e.g., `0000000000001`).
- Author: `System` for automated changesets, developer name for manual ones.
- Never modify an already-applied changeset — always add a new one.
- Include `rollback` blocks in every changeset where rollback is possible.
- Test changesets against a local PostgreSQL instance before committing.

---

## Bucket4j & Redis — How They Work Together

### The Atomicity Problem
A rate limit check is a **read-modify-write** sequence:
1. Read current token count from Redis
2. Decide allow or block
3. Write back the decremented count

Without atomicity, two concurrent requests across different app instances can both read "1 token left" and both be allowed — a race condition. Bucket4j solves this by executing the entire sequence as a **single atomic Lua script** via Redis `EVALSHA`.

### How Bucket4j Uses Lua Scripts
When `bucket.tryConsume(1)` is called, Bucket4j serializes the bucket state and sends a Lua script to Redis that does all of the following in one atomic operation:

```
1. GET the current bucket state (serialized bytes) by key
2. Deserialize state → apply token refill logic based on elapsed time
3. Check if enough tokens are available
4. If yes → decrement tokens, serialize new state, SET back with TTL
5. Return {allowed, remainingTokens, nanosToWaitForRefill}
```

Redis executes Lua scripts atomically — no other command can interleave. This guarantees correctness under high concurrency with zero locking overhead.

### Key Naming Convention
All bucket keys in Redis must follow this exact format:

```
rate_limit:{appId}
```

Examples: `rate_limit:payments-service`, `rate_limit:mobile-app`

- Keys are managed exclusively by `LettuceBasedProxyManager` — never write to them directly.
- TTL is set automatically by Bucket4j to the `refillPeriodSeconds` of the plan.
- Never use `KEYS rate_limit:*` in production — use `SCAN` with a cursor if iteration is needed.

### Wiring: ProxyManager + BucketConfiguration

```
RateLimitPlan (loaded from PostgreSQL)
        │
        ▼
BucketConfiguration.builder()
    .addLimit(Bandwidth.builder()
        .capacity(plan.getCapacity())
        .refillGreedy(plan.getRefillRate(),
                      Duration.ofSeconds(plan.getRefillPeriodSeconds()))
        .build())
    .build()
        │
        ▼
LettuceBasedProxyManager<String>   ← bean in RedisConfig, shared singleton
        │
        ▼  proxyManager.builder().build("rate_limit:{appId}", config)
BucketProxy                         ← thin proxy; all state lives in Redis
        │
        ▼  bucket.tryConsume(1)
Redis EVALSHA ──► Lua script (atomic)
        │
        ▼
true (allowed) / false (blocked)
```

### RedisConfig Rules
- Create a single `LettuceBasedProxyManager<String>` bean — do not instantiate per request.
- Use `RedisClient` (Lettuce standalone) or `StatefulRedisConnection` — never Jedis.
- Connection config (host, port, timeout) comes from `application.yml` only — no hardcoded values.
- Use non-blocking Lettuce async operations for the hot check path where possible.

### BucketConfiguration Rules
- Always use `Bandwidth.builder()` with `refillGreedy` — tokens refill continuously, not in batches.
- `refillGreedy(refillRate, Duration.ofSeconds(refillPeriodSeconds))` means `refillRate` tokens are added smoothly over the period.
- Load `RateLimitPlan` from PostgreSQL **once on first request** per `appId` and cache it — never query the DB on every rate limit check.
- If the plan is updated, invalidate the cached `BucketConfiguration` and rebuild.

### Failure Strategy
Redis unavailability must degrade gracefully. Controlled by `app.ratelimit.redis.failure-strategy` in `application.yml`:

| Strategy | Behaviour | Use Case |
|---|---|---|
| `fail-open` (default) | Allow request, log `ERROR` | High-availability services |
| `fail-closed` | Block request, return 503 | Strict quota enforcement |

- Catch `RedisException` / `io.lettuce.core.RedisConnectionException` at the service layer.
- Never let a Redis failure propagate as a 500 to the caller — it must be caught and handled per strategy.
- Always log `ERROR` with the `appId` when Redis is unreachable.

### TTL Rules
- TTL on each bucket key = `refillPeriodSeconds` of the associated plan.
- Set automatically by Bucket4j — do not call `EXPIRE` manually.
- If a plan's `refillPeriodSeconds` changes, the old key will expire naturally; the new config takes effect on the next request after expiry.

---

## Testing Strategy

### Unit Tests (JUnit 5 + Mockito)

- Location: `src/test/java/` mirroring the main package structure.
- Naming: `<ClassName>Test` (e.g., `RateLimitServiceTest`).
- Use `@ExtendWith(MockitoExtension.class)` — no Spring context for unit tests.
- Mock all dependencies with `@Mock`; inject with `@InjectMocks`.
- Test one behavior per test method. Method names: `should<Behavior>_when<Condition>`.
- Use `assertThat` from AssertJ (preferred over JUnit `assertEquals`).
- Cover: happy path, boundary values, exception cases.
- Aim for >80% line coverage on `service/` and `mapper/` packages.

### Integration Tests

- Naming: `<ClassName>IT` (e.g., `RateLimitControllerIT`).
- Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate` or `MockMvc`.
- Use Testcontainers for PostgreSQL and Redis — never mock the database in integration tests.
- Each integration test resets state (truncate tables / flush Redis) via `@BeforeEach`.
- Test the full HTTP layer: request → response, including status codes, headers, and body shape.

### Slice Tests

- `@WebMvcTest` for controller-only tests (mock the service layer).
- `@DataJpaTest` for repository tests with an in-memory or Testcontainers PostgreSQL.

### General Testing Rules

- No `Thread.sleep` in tests — use Awaitility for async assertions.
- No production secrets in test configuration — use `application-test.yml` with test credentials.
- Tests must be deterministic and order-independent.
- CI must fail if any test fails.

---

## Code Generation Rules (Claude-Specific)

1. **Always read existing files before editing.** Never assume file contents.
2. **Follow the package structure exactly.** Do not create classes outside the defined packages.
3. **Generate the full layer stack** when adding a new feature: Entity → Repository → Service interface → Service impl → Mapper → DTO → Controller → unit test skeleton.
4. **Never use field injection (`@Autowired` on fields).** Always use constructor injection. Lombok `@RequiredArgsConstructor` is the preferred shorthand.
5. **All service methods must be on an interface** defined in `service/`, implemented in `service/impl/`.
6. **Never expose `Optional` from service methods** — resolve inside the service and throw `ResourceNotFoundException` if absent.
7. **Never catch and swallow exceptions.** Always rethrow or convert to an `AppException` subclass.
8. **All controller methods must declare their HTTP method and path explicitly** using `@GetMapping`, `@PostMapping`, etc. — never `@RequestMapping` without a method.
9. **Return `ResponseEntity<T>`** from all controller methods to allow explicit status codes.
10. **All new Liquibase changesets** must include a `rollback` block.
11. **No magic numbers or strings in business logic.** Use named constants or enums.
12. **Avoid `var` for complex generic types** — prefer explicit types for readability.
13. **Do not add unnecessary comments.** Code should be self-documenting; comment only non-obvious decisions.
14. **Generate unit tests alongside every service and mapper class.**
15. **Keep methods short** — target ≤20 lines; extract private methods if needed. Controllers should delegate immediately.

---

## Performance Best Practices

- **Redis first** — bucket state lives in Redis; never hit the database on the hot rate-limit check path.
- Use `@Transactional(readOnly = true)` on all service methods that only read data.
- Avoid loading full entity collections when only counts or existence checks are needed (use `existsBy*`, `countBy*`).
- Paginate all list endpoints — never return unbounded lists. Use Spring Data `Pageable`.
- Set explicit `@Column(length = n)` to avoid `TEXT` for short strings (prevents full table scans on some indexes).
- Hikari pool is pre-configured — do not change pool settings without load testing justification.
- Use Redis `EXPIRE` on bucket keys to avoid unbounded memory growth.
- Avoid `@Transactional` on controller methods — transactions belong in the service layer.
- Prefer `findById` over `getOne`/`getReferenceById` when you need the entity data immediately.

---

## Clean Code and SOLID Enforcement

### Single Responsibility
- Each class has one reason to change.
- Controllers: HTTP concern only. Services: business logic only. Repositories: data access only.

### Open/Closed
- Extend behavior via new classes/implementations, not by modifying stable existing ones.
- Use Strategy pattern for pluggable rate limiting algorithms if new algorithms are added.

### Liskov Substitution
- All `AppException` subclasses must be substitutable for `AppException` without breaking handler logic.

### Interface Segregation
- Service interfaces expose only the methods relevant to their consumer.
- Do not create a single massive service interface — split by use case.

### Dependency Inversion
- High-level modules (controllers) depend on abstractions (service interfaces), not implementations.
- Use Spring's IoC for all dependency injection.

### General Clean Code
- Meaningful names: `rateLimitPlanService`, not `service` or `rls`.
- No abbreviations except universally understood ones (`ip`, `id`, `dto`, `http`).
- Avoid boolean parameters in public methods — use enums or builder patterns instead.
- Immutable DTOs preferred (Java records or `final` fields with no setters).
- Fail fast — validate preconditions at the top of methods.
