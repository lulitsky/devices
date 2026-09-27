# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Spring Boot component for device management (CRUD over a `Device` resource — name, brand, state,
creation date). The running app (`bootRun`, the Docker image) connects to a local Postgres
database; tests run against an in-memory H2 database instead (see below). Java 21 / Spring Boot
4.1.1 / Gradle (Kotlin DSL).

Keep this file in sync: after any change to endpoints, architecture, test coverage, dependencies, or configuration, update the relevant section before finishing.

## Commands

```bash
./gradlew build                 # Compile + run tests
./gradlew test                  # Run all tests
./gradlew test --tests "org.ulitzky.devices.service.DeviceServiceTest"   # Run a single test class
./gradlew test --tests "org.ulitzky.devices.service.DeviceServiceTest.methodName"  # Single test method
./gradlew bootRun                # Start the app (http://localhost:8080)
```

- `Dockerfile` — multi-stage build; `eclipse-temurin:21-jdk` builds the boot jar via the Gradle
  wrapper, `eclipse-temurin:21-jre` runs it (`ENTRYPOINT java -jar app.jar`), exposing port 8080.
  `.dockerignore` excludes `build/`, `.gradle/`, `.git/`, `.idea/`. Build/run with:
  ```bash
  docker build -t device-service .
  docker run -p 8080:8080 device-service
  ```
- `docker-compose.yml` — two services: `postgres` (official `postgres:16` image, db/user/password
  all `devices`, port 5432 published to the host, named volume `postgres-data` for persistence, a
  `pg_isready` healthcheck) and `device-service` (builds from the `Dockerfile`, port 8080, waits on
  `postgres`'s healthcheck via `depends_on: condition: service_healthy`, and sets `DB_HOST=postgres`
  — see Database below for why). Run the whole stack with `docker compose up --build`; publishing
  Postgres's port also means `docker compose up postgres` alone plus a host-side `./gradlew
  bootRun` works, since `localhost:5432` then reaches the same container.
- Swagger UI: `http://localhost:8080/swagger-ui/index.html` (raw OpenAPI JSON at
  `/v3/api-docs`) — `springdoc-openapi-starter-webmvc-ui` is declared in `build.gradle.kts` and
  `springdoc.swagger-ui.enabled=true` in `application.properties`. API metadata (title/description/
  version) comes from `config/OpenApiConfig` (`OpenAPI` bean); `DeviceController` carries
  `@Tag`/`@Operation`/`@Parameter` annotations and `DeviceResource` carries `@Schema` annotations to
  enrich the generated docs.

### Database

Two datasource configs, kept intentionally separate by resource file rather than by Spring
profile — `src/main/resources/application.properties` (used by `bootRun` and the Docker image) and
`src/test/resources/application.properties` (used by every test, since Gradle puts test resources
ahead of main resources on the test classpath, so the test file's `application.properties` fully
shadows main's rather than merging with it):

- **Main (`bootRun`/Docker) → Postgres**: `runtimeOnly("org.postgresql:postgresql")` in
  `build.gradle.kts`; `spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:5432/devices`,
  user/password `devices`/`devices`, `spring.jpa.hibernate.ddl-auto=update` (schema is
  created/updated in place, not dropped — data persists across restarts). Only the hostname varies
  between the two ways of running the app: bare `bootRun` defaults `DB_HOST` to `localhost` (needs
  a Postgres reachable there, e.g. `docker compose up postgres`, or any local install, with a
  `devices` database and matching credentials); `docker-compose.yml` sets `DB_HOST=postgres` for
  the Docker image, since a plain `docker run` of the image alone won't have anything named
  `postgres` to resolve. Port, db name, and credentials stay identical in both paths.
- **Tests → in-memory H2** (unchanged from before): `testRuntimeOnly("com.h2database:h2")`;
  `spring.datasource.url=jdbc:h2:mem:testdb`, user `sa`, no password,
  `spring.jpa.hibernate.ddl-auto=create-drop` (schema recreated fresh for every test run, no
  persistent data, no `data.sql` seed file). H2 console: `http://localhost:8080/h2-console` (only
  reachable while a test that boots the full Spring context, e.g.
  `DevicesApplicationIntegrationTests`, is running).

## Architecture

Layered structure under `org.ulitzky.devices`:

- `api/v1/resource/` — API-facing DTOs (`DeviceResource`) and its own copy of the `DeviceState`
  enum (with a human-readable `description` per value, e.g. `IN_USE` → `"In-use"`).
- `api/v1/controller/` — `DeviceController`, mapped at `/v1/device`. Talks to `DeviceService` and
  `DeviceMapper` directly (no service interface/impl split). `findById`/`update`/`patch`/`delete`
  declare `throws DeviceNotFoundException`; `update`/`patch`/`delete` also declare
  `throws DeviceNotValidForRequestedChangeException`. Neither is caught in the controller — both are checked
  exceptions that propagate to the client via their `@ResponseStatus`.
- `service/` — `DeviceService` holds all business logic (create/find/update/patch/delete).
  `service/mapper/DeviceMapper` is a MapStruct interface (`componentModel = SPRING`) that converts
  between `Device` (entity) and `DeviceResource` (API DTO); the implementation is generated at
  build time via the `mapstruct-processor` annotation processor — regenerate by rebuilding rather
  than looking for a hand-written impl. Its manual `String`↔`UUID` `id` conversion returns `null`
  for a blank or malformed id string instead of throwing — `DeviceResource.id` is read-only
  (`@Schema(accessMode = READ_ONLY)`) and unused by `DeviceService.create`/`update`, so a bad
  client-supplied id is simply dropped rather than causing a 500.
- `exception/` — `DeviceNotFoundException` (checked, `@ResponseStatus(NOT_FOUND)`) thrown by
  `DeviceService.findById`/`update`/`patch`/`delete` when the id doesn't exist, and
  `DeviceNotValidForRequestedChangeException` (checked, `@ResponseStatus(BAD_REQUEST)`) thrown by
  `DeviceService.update`/`patch`/`delete` when the device's state isn't valid for the requested
  change (see `DeviceState.isValidForDeletionOrUpdate()` below). Both propagate straight through
  the controller layer.
- `model/` — `Device` JPA entity (H2, UUID primary key generated via `GenerationType.UUID`) and
  `model/enums/DeviceState` — a **second, separate** `DeviceState` enum (no `description` field,
  but each constant carries a `validForDeletionOrUpdate` boolean, e.g. `IN_USE(false)`) used by the
  entity/repository layer. The resource layer and model layer intentionally have their own
  `DeviceState` types; MapStruct maps between them by name.
  - `Device.dateCreated` is `updatable = false` and set via a `@PrePersist onCreate()` lifecycle
    callback (always `LocalDateTime.now()` at insert time) — any client-supplied `dateCreated` on
    create is silently overridden, and it can't change on later updates.
    `DeviceService.update` enforces this by comparing the request's `dateCreated` to the stored
    value truncated to minute precision (`ChronoUnit.MINUTES`), matching the precision
    `DeviceResource.dateCreated` actually round-trips at (`@JsonFormat` pattern `"yyyy-MM-dd HH:mm"`)
    — a plain full-precision `equals()` would spuriously reject every GET-then-PUT round trip.
  - `Device.isValidForDeletionOrUpdate()` delegates to `getState().isValidForDeletionOrUpdate()`.
- `dao/` — `DeviceRepository extends JpaRepository<Device, UUID>` with derived-query finders
  (`findByBrand`, `findByState`, `findByBrandAndState`).
- `config/` — `OpenApiConfig`, an `OpenAPI` bean supplying Swagger/OpenAPI metadata (title,
  description, version).

### Request flow

`DeviceController` → maps `DeviceResource` to `Device` via `DeviceMapper` → `DeviceService`
(business logic, talks to `DeviceRepository`) → maps result `Device` back to `DeviceResource` for
the response. IDs cross the API boundary as `String` but are stored/queried as `UUID` in the
entity and repository. A malformed path id is parsed via `UUID.fromString` in
`DeviceService.findById` and converted to `DeviceNotFoundException` (404) rather than propagating
the raw `IllegalArgumentException`; a malformed body `id` (on create/update) is parsed by
`DeviceMapper.map(String)`, which treats it the same as a missing id and maps it to `null` (see
`DeviceMapper` above).

### Endpoints (`/v1/device`)

- `POST /v1/device` — create (any `dateCreated` in the request body is ignored; see `Device`
  above)
- `GET /v1/device/{deviceId}` — get by id; 404 (`DeviceNotFoundException`) if unknown
- `GET /v1/device?brand=&state=` — list, optionally filtered by brand, by state, or by both
  together (`DeviceService.findAllBy` uses `findByBrandAndState` when both are given, else
  `findByBrand`/`findByState`/`findAll`)
- `PUT /v1/device/{deviceId}` — full update; 404 (`DeviceNotFoundException`) if the id is unknown,
  since `DeviceService.update` calls `findById` first
- `PATCH /v1/device/{deviceId}` — partial update via query params (`name`, `brand`, `state`), not a
  request body; 404 if the id is unknown
- `DELETE /v1/device/{deviceId}` — delete; 404 if the id is unknown, 400
  (`DeviceNotValidForRequestedChangeException`) if the device's current state isn't deletable
  (currently only `IN_USE` blocks deletion)

## Testing

- `DeviceServiceTest` — unit tests for `DeviceService`, using Mockito to mock `DeviceRepository`.
  Covers all public methods including the `DeviceNotFoundException` paths (`findById`, `patch`,
  `delete`) and the `DeviceNotValidForRequestedChangeException` path (deleting a device in
  `IN_USE` state) —
  note `delete()` now calls `findById()` internally, so its tests must stub
  `deviceRepository.findById(...)` too, not just `deleteById(...)`.
- `DeviceControllerTest` — controller tests, using Mockito to mock `DeviceService` (real
  `DeviceMapperImpl` for entity/resource mapping). Covers the happy path for every endpoint plus
  exception propagation (`DeviceNotFoundException`/`DeviceNotValidForRequestedChangeException`/generic
  exceptions thrown by the mocked service bubble straight through the controller uncaught) and, for
  `update`, an `ArgumentCaptor`-based check that the mapped entity passed to the service matches the
  request DTO.
- `DevicesApplicationIntegrationTests` — full-stack `@SpringBootTest` exercising `DeviceController`
  end-to-end against the real (in-memory H2) `DeviceRepository`/JPA layer, with no mocks. The class
  is `@Transactional` so each test method's DB writes roll back automatically at the end — this is
  required for isolation, since all test methods share one Spring context/H2 instance and rows
  otherwise leak between tests. Covers create→fetch-by-id (including that `dateCreated` is set on
  create), `findAllBy` filtering (by brand, by state, and by both, for a created device — not the
  no-match/not-found case), delete removing a device from all `findAllBy` views, the
  `DeviceNotValidForRequestedChangeException` path for deleting an `IN_USE` device, and the
  not-found exceptions for `findById`/`delete`/`patch`. Does not cover `dateCreated`
  override-on-create (a client-supplied value being ignored) beyond asserting it's non-null.
- `util/TestDataFactory` — shared builder/factory for constructing test `Device`/`DeviceResource`
  instances (`validDevice()`, `validDeviceResource()`, plus a no-id `validDeviceResourceForCreation()`
  for creation-flow integration tests); use it instead of hand-rolling fixtures in new tests.

## Recovery note (2026-09-27)

This project's working tree (everything under `device-service/`) was unexpectedly wiped from disk
mid-session, with no git remote, no Trash copy, and no local Time Machine snapshot available. The
files below were rebuilt from this document's own architecture description plus the handful of
files still held verbatim in the assistant's conversation context at the time
(`DeviceResource.java`, `DeviceController.java`, this file). Everything else — `DeviceService`,
`Device`, both `DeviceState` enums, the exceptions, the mapper, the repository, build files, and
the entire test suite — is a best-effort reconstruction from this spec, not a recovery of the
original code. Treat it as a fresh implementation to review, not as ground truth of what was there
before.
