# Rate Limiting Service

A production-grade distributed rate limiting service built with Spring Boot 4.x, Bucket4j, Redis, and PostgreSQL. Enforces configurable per-app token bucket rate limits and provides an admin API for plan management.

---

## How It Works

Downstream services call this service before processing a request. The service checks a token bucket stored atomically in Redis via Lua scripts (Bucket4j + Lettuce). If tokens are available the request is allowed; otherwise it is blocked with a `429 Too Many Requests`. Every decision is audit-logged to PostgreSQL.

```
Downstream Service → POST /api/v1/ratelimit/check
                          │
                    Redis (Bucket4j Lua)
                          │
                    allow / block
                          │
                    Audit Log → PostgreSQL
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 4.x, Java 21 |
| Rate Limiting | Bucket4j 8.x (token bucket, Lua scripts) |
| Cache / Bucket State | Redis via Lettuce |
| Database | PostgreSQL 16 |
| ORM | Spring Data JPA + Hibernate |
| Schema Migrations | Liquibase |
| Security | Spring Security 6, JWT (HS256/RS256) |
| Build | Maven |

---

## Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL 16+
- Redis 7+

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/Alluraviteja/rate-limiting-service.git
cd rate-limiting-service
```

### 2. Configure the database

Create the PostgreSQL database and user:

```sql
CREATE USER app_user WITH PASSWORD 'app_password';
CREATE DATABASE rate_limiter OWNER app_user;
```

### 3. Configure environment

Copy and edit the application config:

```bash
cp src/main/resources/application.yml src/main/resources/application-local.yml
```

Update `application-local.yml` with your local PostgreSQL and Redis connection details. This file is git-ignored and safe for local secrets.

### 4. Run the application

```bash
./mvnw spring-boot:run
```

Liquibase will automatically apply all schema migrations on startup.

---

## Configuration

| Property | Default | Description |
|---|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/rate_limiter` | PostgreSQL URL |
| `spring.data.redis.host` | `redis` | Redis host |
| `spring.data.redis.port` | `6379` | Redis port |
| `app.ratelimit.redis.failure-strategy` | `fail-open` | `fail-open` allows requests when Redis is down; `fail-closed` blocks them |

---

## Project Structure

```
src/main/java/com/app/ratelimiter/
├── config/          # Redis, JPA auditing, Security configuration
├── controller/      # REST controllers
├── dto/
│   ├── request/     # Inbound request DTOs
│   └── response/    # Outbound response DTOs
├── exception/       # Custom exceptions + GlobalExceptionHandler
├── filter/          # JWT auth filter, correlation ID filter
├── mapper/          # DTO ↔ Entity mappers
├── model/           # JPA entities (RateLimitPlan, RateLimitLog)
├── repository/      # Spring Data JPA repositories
├── security/        # JWT provider, SecurityConfig
└── service/         # Business logic (interfaces + implementations)
```

---

## API Overview

| Method | Endpoint | Role | Description |
|---|---|---|---|
| `POST` | `/api/v1/ratelimit/check` | `ROLE_SERVICE` | Check and consume a token for an app |
| `GET` | `/api/v1/plans` | `ROLE_ADMIN` | List all rate limit plans |
| `POST` | `/api/v1/plans` | `ROLE_ADMIN` | Create a new rate limit plan |
| `GET` | `/api/v1/plans/{appId}` | `ROLE_ADMIN` | Get a plan by app ID |
| `PUT` | `/api/v1/plans/{appId}` | `ROLE_ADMIN` | Update an existing plan |
| `DELETE` | `/api/v1/plans/{appId}` | `ROLE_ADMIN` | Delete a plan |
| `GET` | `/api/v1/logs` | `ROLE_ADMIN` | Query audit logs (paginated) |
| `GET` | `/actuator/health` | Public | Health check |

---

## Rate Limit Plan Model

```json
{
  "appId": "payments-service",
  "capacity": 100,
  "refillRate": 10,
  "refillPeriodSeconds": 60,
  "description": "10 requests per minute, burst up to 100",
  "enabled": true
}
```

| Field | Description |
|---|---|
| `capacity` | Maximum tokens in the bucket (burst size) |
| `refillRate` | Tokens added per refill period |
| `refillPeriodSeconds` | Refill window in seconds |
| `enabled` | Kill switch — set to `false` to disable rate limiting for an app |

---

## Redis Key Convention

Bucket state is stored in Redis under:

```
rate_limit:{appId}
```

Keys are managed exclusively by Bucket4j and expire automatically based on the plan's refill period.

---

## Database Schema

Managed by Liquibase. Migrations are in `src/main/resources/db/changelog/`.

| Table | Purpose |
|---|---|
| `rate_limit_plan` | Per-app rate limit configuration |
| `rate_limit_log` | Audit log of every allowed/blocked request |

---

## Architecture Notes

See [CLAUDE.md](./CLAUDE.md) for the full architectural guardrail including layering rules, coding conventions, security guidelines, and code generation rules.
