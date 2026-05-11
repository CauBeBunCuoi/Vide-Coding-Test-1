# Teammate B — Backend Workspace

> **Template note:** File names in `@import` paths below are examples only. The `context/`, `skills/`, and `rules/` folder structure is fixed, but file names inside are yours to define per project.

# Context
@import .claude/context/api-list.md
@import .claude/context/domain-model.md
@import .claude/context/background-jobs.md

# Skills
@import .claude/skills/add-endpoint.md

# Rules
@import .claude/rules/database.md
@import .claude/rules/api.md
@import .claude/rules/conventions.md

## Project Context

**Stack:** Java 24 + Spring Boot 4.0.5 + Gradle (Kotlin DSL) + Spring Data JPA (Hibernate 7) + PostgreSQL 17.5 + Flyway 11 + Spring Security + JJWT

**Base package:** `com.example.demo`

**Folder layout:** `controller/` → `service/` → `repository/` (thin controller, all logic in service). DTOs in `dto/request/` and `dto/response/`. Entities in `entity/`. Enums in `enums/`. Custom exceptions in `exception/` with `GlobalExceptionHandler`. Mappers in `mapper/`. JWT auth in `security/`.

**Database:** PostgreSQL 17.5 via Docker. Flyway owns the schema (V1–V11 migrations). Hibernate validates only (`ddl-auto: validate`). Dev DB: `localhost:5432/taskflow`, user `taskflow`, pass `taskflow_dev`.

**Run backend:** `./gradlew bootRun --args='--spring.profiles.active=dev'` → http://localhost:8080

**Run tests:** `./gradlew test` (requires Docker for Testcontainers)
