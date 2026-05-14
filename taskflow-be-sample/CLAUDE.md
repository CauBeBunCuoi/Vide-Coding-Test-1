# Teammate B — Backend Workspace

# Rules
@import .claude/rules/database.md
@import .claude/rules/api.md
@import .claude/rules/conventions.md

## Project Context

**Stack:** Java 21 + Spring Boot 4.0.5 + Gradle (Kotlin DSL) + Spring Data JPA (Hibernate 7) + PostgreSQL 17.5 + Flyway 11 + Spring Security + JJWT

**Base package:** `haonguyen.taskflow_be`

**Folder layout:** `controller/` → `service/` → `repository/` (thin controller, all logic in service). DTOs in `dto/request/` and `dto/response/`. Entities in `entity/`. Enums in `enums/`. Custom exceptions in `exception/` with `GlobalExceptionHandler`. Mappers in `mapper/`. JWT auth in `security/`. Config in `config/`.

**Database:** PostgreSQL 17.5 via Docker. Flyway owns schema (V1–V11 migrations + V11 seed). Hibernate validates only (`ddl-auto: validate`). Dev DB: `localhost:5432/taskflow`, user `taskflow`, pass `taskflow_dev`.

**Run backend:** `./gradlew bootRun --args='--spring.profiles.active=dev'` → http://localhost:8080

**Run tests:** `./gradlew test` (requires Docker for Testcontainers)

**Seed users (password `Test1234!`):** alex@example.com (alex_lead, project owner), sam@example.com (sam_dev, member), jordan@example.com (jordan_free, member)
