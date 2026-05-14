# Test Scope

<!-- Define what is in and out of scope for testing in this project. -->

## In Scope
- All API endpoints (integration tests against real DB)
- Auth flows (login, logout, token refresh)
- Task CRUD operations
- Background job logic (unit tests with mocked dependencies)
- Frontend critical paths: login, task creation, task detail

## Out of Scope
- Third-party service internals (mock at boundary)
- Email delivery (assert email queued, not delivered)

## Coverage Targets
- Unit: 80% minimum
- Integration: all happy paths + key error paths
- E2E: login flow, create task flow, complete task flow
