# TaskFlow — Development Workflows

## Prerequisites

Verify these tools are installed before starting:

```bash
java --version          # Java 24.0.2+
gradle --version        # Gradle 8.14+
node --version          # Node 20+
npm --version           # npm 10+
psql --version          # PostgreSQL client 17+
docker --version        # Docker 28.5+
git --version           # Git 2.40+
```

---

## 1. Initial Project Setup

### 1.1 Clone and Structure

The repository contains two modules at the root level:

```
taskflow/
├── backend/            ← Spring Boot project
├── frontend/           ← React + Vite project
├── docker-compose.yml  ← PostgreSQL container
└── README.md
```

### 1.2 Start PostgreSQL

PostgreSQL runs in a Docker container managed by docker-compose.

```bash
# Start the database container
docker compose up -d

# Verify it's running
docker compose ps
# Should show: taskflow-postgres running, port 5432
```

The `docker-compose.yml` should define:
```yaml
services:
  postgres:
    image: postgres:17.5
    container_name: taskflow-postgres
    environment:
      POSTGRES_DB: taskflow
      POSTGRES_USER: taskflow
      POSTGRES_PASSWORD: taskflow_dev
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

Connection details for `application-dev.yml`:
```
Host: localhost
Port: 5432
Database: taskflow
Username: taskflow
Password: taskflow_dev
```

### 1.3 Backend Setup

```bash
cd backend

# Build the project (downloads dependencies, compiles, runs tests)
./gradlew build

# If tests fail due to missing database, skip tests for first build:
./gradlew build -x test
```

The `application-dev.yml` should contain:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/taskflow
    username: taskflow
    password: taskflow_dev
  jpa:
    hibernate:
      ddl-auto: validate     # Flyway handles schema — Hibernate only validates
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  flyway:
    enabled: true
    locations: classpath:db/migration

jwt:
  secret: dev-secret-key-minimum-32-characters-long-for-hs256
  access-token-expiration: 900000      # 15 minutes in milliseconds
  refresh-token-expiration: 604800000  # 7 days in milliseconds

server:
  port: 8080
```

### 1.4 Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Create .env.local for development
echo "VITE_API_BASE_URL=http://localhost:8080/api/v1" > .env.local
```

---

## 2. Running the Development Servers

### 2.1 Start Backend

```bash
cd backend

# Run with dev profile
./gradlew bootRun --args='--spring.profiles.active=dev'
```

The backend starts on `http://localhost:8080`. Flyway runs migrations automatically on startup.

To verify:
```bash
# Health check
curl http://localhost:8080/actuator/health

# Check Flyway migration status
curl http://localhost:8080/actuator/flyway
```

### 2.2 Start Frontend

```bash
cd frontend

npm run dev
```

The frontend starts on `http://localhost:5173` with hot module reload. Vite proxies API requests to the backend (configure in `vite.config.ts`):

```typescript
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
```

With this proxy, the frontend Axios base URL can simply be `/api/v1` during development, avoiding CORS issues entirely.

### 2.3 Full Stack Startup Order

1. `docker compose up -d` (PostgreSQL)
2. `cd backend && ./gradlew bootRun --args='--spring.profiles.active=dev'` (wait for "Started DemoApplication")
3. `cd frontend && npm run dev` (open browser to http://localhost:5173)

---

## 3. Database Operations

### 3.1 Running Migrations

Flyway migrations run automatically on Spring Boot startup. To run them manually:

```bash
cd backend
./gradlew flywayMigrate -Dflyway.url=jdbc:postgresql://localhost:5432/taskflow -Dflyway.user=taskflow -Dflyway.password=taskflow_dev
```

### 3.2 Checking Migration Status

```bash
./gradlew flywayInfo -Dflyway.url=jdbc:postgresql://localhost:5432/taskflow -Dflyway.user=taskflow -Dflyway.password=taskflow_dev
```

### 3.3 Resetting the Database

When you need a fresh database (useful during development when migration files change):

```bash
# Option 1: Flyway clean + migrate
./gradlew flywayClean flywayMigrate -Dflyway.url=jdbc:postgresql://localhost:5432/taskflow -Dflyway.user=taskflow -Dflyway.password=taskflow_dev -Dflyway.cleanDisabled=false

# Option 2: Destroy and recreate the container
docker compose down -v    # -v removes the volume
docker compose up -d
# Then restart the backend to trigger Flyway
```

### 3.4 Seed Data

The seed migration (`V11__seed_data.sql`) runs automatically with all other migrations.

Seed users all have the password `Test1234!` — use any of these to log in during development:

| Email | Username | Notes |
|-------|----------|-------|
| alex@example.com | alex_lead | Owns "TaskFlow MVP" and "Marketing Site" |
| sam@example.com | sam_dev | Member of "TaskFlow MVP", has task assignments |
| jordan@example.com | jordan_free | Member of both projects |

### 3.5 Connecting via psql

```bash
psql -h localhost -p 5432 -U taskflow -d taskflow
# Password: taskflow_dev

# Useful queries during development:
SELECT * FROM users;
SELECT * FROM projects;
SELECT t.title, t.status, t.priority, u.username AS assignee FROM tasks t LEFT JOIN users u ON t.assignee_id = u.id;
```

---

## 4. Creating New Flyway Migrations

When you need to modify the schema:

1. Never edit an existing `V*__*.sql` file that has already been applied
2. Create a new file with the next version number:
   ```
   src/main/resources/db/migration/V12__add_column_to_tasks.sql
   ```
3. Write idempotent SQL where possible:
   ```sql
   ALTER TABLE tasks ADD COLUMN IF NOT EXISTS updated_by BIGINT;
   ```
4. Restart the backend — Flyway picks up the new migration automatically
5. Verify: `./gradlew flywayInfo`

File naming:
- `V{number}__{description}.sql` — versioned, runs once in order
- `R__{description}.sql` — repeatable, re-runs when content changes (use for views, stored procedures)

---

## 5. Testing Workflows

### 5.1 Backend Tests

```bash
cd backend

# Run all tests
./gradlew test

# Run a specific test class
./gradlew test --tests "com.example.demo.service.TaskServiceTest"

# Run tests with verbose output
./gradlew test --info

# Run only unit tests (services with mocks)
./gradlew test --tests "com.example.demo.service.*"

# Run only controller tests (WebMvcTest)
./gradlew test --tests "com.example.demo.controller.*"

# Run only repository tests (DataJpaTest with Testcontainers)
./gradlew test --tests "com.example.demo.repository.*"

# Run integration tests (full context with Testcontainers)
./gradlew test --tests "com.example.demo.integration.*"
```

**Test database:** Tests use Testcontainers, which spins up a temporary PostgreSQL container. No need for a separate test database. The `application-test.yml` configures this:

```yaml
spring:
  datasource:
    url: # Set by Testcontainers dynamically
  flyway:
    clean-disabled: false
```

**Test structure:**
- Service tests: mock repositories with Mockito, test business logic
- Controller tests: `@WebMvcTest` with mocked services, test HTTP behavior (status codes, request validation, response shapes)
- Repository tests: `@DataJpaTest` with Testcontainers, test custom queries
- Integration tests: `@SpringBootTest` with Testcontainers, test full request → response flow

### 5.2 Frontend Tests

```bash
cd frontend

# Run unit/component tests (Vitest)
npm run test

# Run tests in watch mode
npm run test:watch

# Run tests with coverage
npm run test:coverage

# Run a specific test file
npx vitest run src/features/board/KanbanBoard.test.tsx

# Run E2E tests (Playwright)
npx playwright test

# Run E2E in headed mode (see the browser)
npx playwright test --headed

# Run a specific E2E test
npx playwright test tests/auth.spec.ts
```

**Test structure:**
- Component tests: React Testing Library — render component, assert DOM output, simulate user interactions
- Hook tests: `renderHook` from React Testing Library — test custom hooks in isolation
- E2E tests: Playwright — full browser tests against the running dev environment (backend + frontend must be running)

### 5.3 Test File Location Convention

**Backend:**
Test files mirror the source structure:
```
src/test/java/com/example/demo/
├── service/TaskServiceTest.java
├── controller/TaskControllerTest.java
├── repository/TaskRepositoryTest.java
└── integration/TaskIntegrationTest.java
```

**Frontend:**
Test files sit next to the component they test:
```
src/features/board/
├── KanbanBoard.tsx
├── KanbanBoard.test.tsx      ← component test
├── TaskCard.tsx
└── TaskCard.test.tsx
```

E2E tests live in a top-level `/tests` directory:
```
tests/
├── auth.spec.ts
├── projects.spec.ts
├── tasks.spec.ts
└── comments.spec.ts
```

---

## 6. API Testing During Development

### 6.1 Manual Testing with curl

```bash
# Register a new user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"Test1234!"}'

# Login and capture the access token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alex@example.com","password":"Test1234!"}' \
  -c cookies.txt | jq -r '.accessToken')

# Use the token for authenticated requests
curl http://localhost:8080/api/v1/projects \
  -H "Authorization: Bearer $TOKEN"

# Create a project
curl -X POST http://localhost:8080/api/v1/projects \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"My New Project","description":"Test project"}'

# Create a task
curl -X POST http://localhost:8080/api/v1/projects/1/tasks \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"First task","priority":"HIGH","assigneeId":2}'
```

### 6.2 OpenAPI / Swagger UI

SpringDoc generates OpenAPI documentation automatically. Access it at:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

---

## 7. Code Quality Checks

### 7.1 Backend

```bash
cd backend

# Compile check
./gradlew compileJava

# Run tests
./gradlew test

# Full build (compile + test + package)
./gradlew build
```

### 7.2 Frontend

```bash
cd frontend

# Type checking
npx tsc --noEmit

# Lint
npm run lint

# Format check (if using Prettier)
npx prettier --check "src/**/*.{ts,tsx}"

# Full quality check
npm run lint && npx tsc --noEmit && npm run test
```

---

## 8. Common Development Scenarios

### Adding a new API endpoint (backend)

1. Define the request DTO in `dto/request/` with Jakarta Validation annotations
2. Define the response DTO in `dto/response/`
3. Add the repository method (if a new query is needed)
4. Implement the service method with business logic
5. Add the controller method with correct mapping and status code
6. Add the mapper conversion method
7. Write a service unit test
8. Write a controller integration test
9. Update the OpenAPI annotations if using them

### Adding a new page (frontend)

1. Create the page component in `pages/`
2. Add the route in `App.tsx`
3. Create feature components in `features/`
4. Add API functions in `api/`
5. Create query/mutation hooks in the feature folder
6. Define TypeScript types in `types/`
7. Write component tests
8. Add E2E test for the critical path

### Adding a new database table

1. Create a new Flyway migration file: `V{next}__{description}.sql`
2. Create the JPA entity in `entity/`
3. Create the repository in `repository/`
4. Restart the backend (Flyway applies the migration)
5. Write a repository test with Testcontainers
6. Add service and controller layers as needed

---

## 9. Troubleshooting

**"Flyway migration checksum mismatch"**
You edited an already-applied migration. Either:
- Reset the database (`docker compose down -v && docker compose up -d`)
- Or run `flywayRepair` to update the checksum (risky if the change is structural)

**"Port 5432 already in use"**
Another PostgreSQL instance is running. Stop it or use a different port in docker-compose.yml:
```yaml
ports:
  - "5433:5432"
```
Then update `application-dev.yml` accordingly.

**"CORS error in browser console"**
If not using Vite proxy, ensure `WebConfig.java` allows `http://localhost:5173`. Better approach: use the Vite proxy configuration described in section 2.2.

**"JWT token expired" during development**
The access token lives only 15 minutes. If using curl, re-run the login command to get a fresh token. The frontend handles this automatically via the refresh interceptor.

**"Testcontainers: Docker is not running"**
Backend tests require Docker for Testcontainers. Ensure Docker Desktop is running before running tests.
