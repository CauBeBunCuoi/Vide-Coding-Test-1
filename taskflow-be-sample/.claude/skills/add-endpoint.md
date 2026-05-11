---
name: add-endpoint
description: How to add a new REST endpoint to the TaskFlow Spring Boot backend
---

# How to Add a New Endpoint

Follow these steps in order. All layers must be present before writing tests.

1. **Request DTO** — create in `dto/request/` with Jakarta Validation annotations:
   ```java
   public record CreateFooRequest(
       @NotBlank @Size(max = 200) String title,
       Long assigneeId  // nullable fields need no annotation
   ) {}
   ```

2. **Response DTO** — create in `dto/response/`:
   ```java
   public record FooResponse(Long id, String title, UserSummaryResponse assignee) {}
   ```

3. **Repository method** — add to the Spring Data JPA interface in `repository/` only if a new query is needed. Use JPQL `@Query` for anything beyond simple derived names. Avoid native SQL unless absolutely necessary.

4. **Service method** — implement in `service/`. Business logic lives here:
   - Load entity (throw `ResourceNotFoundException` if missing)
   - Check membership/ownership (throw `AccessDeniedException`)
   - Validate business rules (throw `BusinessRuleException`)
   - Persist via repository
   - Return entity (not DTO)

5. **Mapper method** — add to the relevant mapper in `mapper/`. Convert entity → response DTO. Keep mappers as static methods or `@Component` classes; no business logic in mappers.

6. **Controller method** — add to the relevant controller in `controller/`. Controllers only:
   - Extract path vars, query params, and request body
   - Call the service
   - Map entity to response DTO via mapper
   - Return `ResponseEntity` with correct HTTP status

7. **Service unit test** — in `src/test/.../service/`. Mock repositories with Mockito. Test the happy path and all error branches (permission denied, not found, business rule violations).

8. **Controller integration test** — in `src/test/.../controller/`. Use `@WebMvcTest` with mocked service. Test HTTP status codes, request validation rejection, and response shape.

9. **Update `api-list.md`** — add the new endpoint to `.claude/context/api-list.md`.

> Check `.claude/rules/api.md` for error response format and HTTP status conventions.
> Check `.claude/rules/conventions.md` for naming conventions and the layered architecture rules.
