---
name: add-endpoint
description: How to add a new REST endpoint in this project
---

# How to Add a New Endpoint

1. Define the request DTO in `src/main/java/haonguyen/taskflow_be/dto/request/` with Jakarta Validation annotations
2. Define the response DTO in `src/main/java/haonguyen/taskflow_be/dto/response/`
3. Add the repository method in `src/main/java/haonguyen/taskflow_be/repository/` (if a new query is needed)
4. Implement the service method in `src/main/java/haonguyen/taskflow_be/service/` with business logic and permission checks
5. Add the mapper conversion in `src/main/java/haonguyen/taskflow_be/mapper/`
6. Add the controller method in `src/main/java/haonguyen/taskflow_be/controller/` with correct `@RequestMapping`, status code, and `@AuthenticationPrincipal`
7. Write a service unit test and a controller integration test
8. If schema changes: create a new Flyway migration file (see `.claude/rules/database.md`)

## References
- `references/api-contract.md` — full API contract for all endpoints (methods, paths, request/response shapes, error codes)
- `references/domain-model.md` — database schema, table definitions, indexes, cascade rules
- `.claude/rules/api.md` — HTTP status codes, error format, pagination, controller rules
- `.claude/rules/database.md` — migration standards, enum conventions, cascade delete order
- `.claude/rules/conventions.md` — naming conventions, permission check pattern, layer responsibilities
