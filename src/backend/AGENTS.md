# Repository Guidelines

## Project Structure & Module Organization
This repository is a Spring Boot backend (`Java 21`) built with Maven.
- Source code: `src/main/java/com/trucdnd/gpu_hub_backend`
- Runtime config and SQL: `src/main/resources` (`application.yaml`, `schema.sql`)
- Tests: `src/test/java/com/trucdnd/gpu_hub_backend`
- Build output: `target/` (generated; do not edit)

Code is organized by domain (`auth`, `cluster`, `project`, `team`, `user`, `workload`, `policy`) with a consistent layering pattern: `controller`, `service`, `repository`, `entity`, `dto`.

## Build, Test, and Development Commands
Use the Maven wrapper so local Maven versions do not matter.
- `./mvnw spring-boot:run` - run the API locally on port `9000`
- `./mvnw test` - run unit/integration tests
- `./mvnw clean package` - compile, test, and build the JAR
- `./mvnw clean package -DskipTests` - fast packaging when tests are intentionally skipped

Set DB/JWT values via env vars (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`) instead of hardcoding.

## Coding Style & Naming Conventions
Follow existing Spring conventions and keep modules cohesive.
- Use 4-space indentation and standard Java formatting.
- Class names: `PascalCase`; methods/fields: `camelCase`; constants: `UPPER_SNAKE_CASE`.
- Request/response models end with `Request`, `Response`, or `Dto`.
- Keep package naming as `com.trucdnd.gpu_hub_backend` (underscore, not hyphen).
- All constants (e.g. status of entity, role of user) are defined at src/main/java/com/trucdnd/gpu_hub_backend/common/constants

## Testing Guidelines
Current tests use JUnit 5, Mockito, and Spring Boot test support.
- Name test classes `*Test` and mirror production package paths.
- Prefer focused service tests with mocked repositories.
- Run `./mvnw test` before opening a PR.
- Add tests for new service logic and controller behavior changes.

## Commit & Pull Request Guidelines
Recent history favors short, imperative commit messages (for example: `Update schema file`, `refactor repo`).
- Write concise commit titles in imperative mood.
- Keep commits scoped to one concern.
- PRs should include: summary, affected modules, test evidence (`./mvnw test`), and related issue/task.
- For API changes, include request/response examples or endpoint notes.
