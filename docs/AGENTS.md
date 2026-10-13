# AGENTS.md — Indoor Navigation

Single source of instructions for every AI coding agent and teammate on this project.
Read **Part 1 (Project-wide rules)** first, then read only the **module section** for the part you are working on.
If they conflict, the module section wins for module-internal details; Part 1 wins for anything cross-module.

## Contents
- Part 1: Project-wide rules
- Part 2: Mobile app (Flutter)
- Part 3: Backend (Java 21 + Spring Boot)
- Part 4: Pathfinding engine (pure Java)
- Part 5: Database, DevOps and testing

---

# Part 1: Project-wide rules

## Project summary
Indoor navigation app: a user picks a start and destination inside a multi-floor building and sees the shortest route, with accessibility and obstacle awareness.

## Architecture
```
Flutter app (Android + iOS)  --HTTPS/JSON-->  Spring Boot backend  --Java call-->  Pathfinding engine
                                                    |
                                                PostgreSQL
```
- The backend never knows or cares whether the client is Android or iOS.
- The pathfinding engine is a plain Java library: no Spring, no database, no HTTP.

## Repository layout
```
indoor-navigation/
├── mobile-app/          # Flutter + Dart            (Person 1)
├── backend/             # Java 21 + Spring Boot     (Person 2)
├── algorithm-engine/    # Pure Java library         (Person 3)
├── database/            # Schema, migrations, seed  (Person 4)
├── docs/                # ARCHITECTURE.md, API.md, DATABASE.md
├── .github/workflows/   # CI                        (Person 4)
└── docker-compose.yml
```

## Ownership
| Module | Owner | Others may |
|---|---|---|
| mobile-app | Person 1 | read; propose changes via PR |
| backend | Person 2 | read; propose changes via PR |
| algorithm-engine | Person 3 | read; propose changes via PR |
| database, CI, Docker | Person 4 | read; propose changes via PR |

**Stay inside your assigned module.** If your task needs a change in another module, stop, describe the change needed, and ask the human to coordinate. Do not edit another module's files on your own.

## Source of truth for contracts
1. `docs/API.md` — REST request/response shapes. Frontend and backend both code against this.
2. `docs/DATABASE.md` + `database/migrations/` — schema.
3. `algorithm-engine` public interfaces — `RoutingEngine`, `Graph`, `RouteRequest`, `RouteResult`.

Never change a contract silently. A contract change needs: (a) the doc updated in the same PR, (b) the PR labeled `contract-change`, (c) every affected module's owner tagged as reviewer.

## Core domain vocabulary (use these exact names everywhere)
- **Building** → has many **Floors**
- **Floor** → has many **Nodes** (rooms, junctions, doors, landmarks)
- **Node** → a point on a floor with `x`, `y`
- **Edge** → a walkable connection between two nodes, with `distance` and `accessible` flag
- **Connector** → an edge between floors (`STAIRS`, `ELEVATOR`, `RAMP`)
- **Obstacle** → a temporarily or permanently blocked node or edge
- **Route** → ordered list of nodes plus total distance and estimated time

## Working rules for AI agents
1. Make the smallest change that completes the task. No drive-by refactors.
2. Before writing code, read the existing code in the module and match its style.
3. Do not invent endpoints, fields, tables or dependencies. If something isn't in the contracts, ask.
4. Do not add a new library without stating why and getting human approval.
5. Never commit secrets, API keys, passwords or `.env` files. Use `.env.example`.
6. Every behavior change needs a test. Do not delete or weaken a failing test to make it pass.
7. Run the module's build and test commands before saying a task is done, and report the result honestly. If you could not run something, say so.
8. If requirements are ambiguous, ask one clear question rather than guessing.
9. Prefer clear, boring code over clever code. Comment the *why*, not the *what*.

## Git workflow
- Branches: `feature/<module>-<short-name>`, `fix/<module>-<short-name>`
- Never push directly to `main`. Open a pull request; at least one teammate reviews.
- Commit messages: `<module>: <imperative summary>` — e.g. `backend: add route request validation`
- Keep PRs small and focused on one module where possible.

## Definition of done
- [ ] Builds with no errors or new warnings
- [ ] Tests added/updated and passing
- [ ] Contracts and docs updated if affected
- [ ] No secrets, debug prints, or commented-out code
- [ ] PR description says what changed, why, and how it was tested

## Parallel development with mocks
- Mobile uses a mock repository returning data shaped exactly like `docs/API.md` until the backend is ready.
- Backend uses a stub `RoutingEngine` until the real engine is ready.
- Engine is tested with in-memory graphs, independent of the database.
- Switch from mock to real only through configuration, never by deleting the mock.

---

# Part 2: Mobile app (Flutter)

*Applies to this module only. Part 1 rules also apply.*

## Scope
Single Flutter codebase for Android and iOS: building/floor selection, room search, indoor map rendering, route display, and navigation UI. No business logic that belongs to the backend or engine — the app never computes routes itself.

## Stack
- Flutter (stable channel) + Dart, Material 3
- State management: **Riverpod**
- HTTP: **Dio**
- Navigation: **GoRouter**
- Map rendering: **CustomPainter** (or `flutter_svg` for static floor plans)
- Local storage: `shared_preferences` (SQLite only if clearly needed)
- Models: plain Dart classes, optionally `freezed` + `json_serializable`
- Tests: `flutter_test`, `integration_test`

Do not add packages beyond this list without approval.

## Structure (feature-first)
```
lib/
├── main.dart
├── app/                 # app widget, theme, router
├── core/                # api client, config, errors, utils
├── data/
│   ├── models/          # Building, Floor, Node, Route (match docs/API.md)
│   └── repositories/    # abstract + Api + Mock implementations
└── features/
    ├── building_select/
    ├── search/
    ├── map/             # floor view, CustomPainter, route overlay
    └── navigation/      # turn-by-turn / route steps
test/
integration_test/
```
Each feature: `presentation/` (widgets, screens), `application/` (Riverpod providers/notifiers).

## Rules
1. **Code against `docs/API.md`.** Model field names must match the API exactly. If the API is missing something, ask — do not invent fields.
2. **Repository pattern.** Screens never call Dio directly. They use a repository interface; provide `MockRouteRepository` and `ApiRouteRepository`, switched via config.
3. **No hardcoded URLs.** Base URL comes from build config (`--dart-define=API_BASE_URL=...`).
4. **Handle every state:** loading, success, empty, error. Show friendly error messages; never show raw exceptions.
5. **Keep widgets small.** Extract widgets instead of building 300-line `build()` methods. Use `const` constructors wherever possible.
6. **No business logic in widgets.** Put it in Riverpod notifiers.
7. **Map rendering:** keep coordinates in building units and convert to screen space in one place (a single transform helper). Support pan/zoom and floor switching.
8. **Accessibility:** semantic labels on interactive elements, sufficient contrast, text scales with system settings, and an "accessible route only" toggle.
9. **Platform parity:** do not write platform-specific code unless unavoidable. Test layouts on both a small Android phone and an iPhone-sized screen.
10. Run `dart format .` and `flutter analyze` — both must be clean.

## Commands
```
flutter pub get
flutter analyze
dart format .
flutter test
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080   # Android emulator -> host
flutter test integration_test
```

## Notes
- Android emulator reaches the host machine at `10.0.2.2`; iOS simulator uses `localhost`.
- Building and signing for iOS requires macOS with Xcode. Do not attempt iOS release builds on other OSes.

## Do not
- Edit `backend/`, `algorithm-engine/` or `database/`.
- Implement pathfinding, even "temporarily".
- Commit generated files (`*.g.dart`, `*.freezed.dart`) unless the team decides otherwise.

---

# Part 3: Backend (Java 21 + Spring Boot)

*Applies to this module only. Part 1 rules also apply.*

## Scope
REST API, validation, business logic, persistence, and orchestration of the pathfinding engine. The backend loads building data from PostgreSQL, builds/caches the graph, calls the engine, and returns the result. It does **not** implement pathfinding algorithms itself.

## Stack
- Java 21, Spring Boot 3.x, Maven
- Spring Web, Spring Data JPA, Bean Validation
- PostgreSQL driver, **Flyway** (migrations live in `database/migrations/`)
- Springdoc OpenAPI (optional), JUnit 5, Mockito, Testcontainers

## Package structure
```
com.indoornav.backend
├── config/          # beans, CORS, properties
├── controller/      # REST controllers — thin
├── service/         # business logic, engine orchestration
├── repository/      # Spring Data repositories
├── entity/          # JPA entities (never returned from controllers)
├── dto/             # request/response objects matching docs/API.md
├── mapper/          # entity <-> dto, entity -> engine graph
└── exception/       # custom exceptions + @RestControllerAdvice
```

## Rules
1. **`docs/API.md` is the contract.** Endpoints, field names, status codes and error shapes must match it exactly.
2. **Thin controllers.** Controllers only handle HTTP, validation and delegation to services.
3. **Never expose entities.** Always map to DTOs.
4. **Validate all input** with Bean Validation (`@Valid`, `@NotNull`, etc.). Return `400` with a consistent error body for invalid input, `404` for unknown building/node, `422` when no route exists.
5. **Consistent error format** via a single `@RestControllerAdvice`:
   `{ "timestamp", "status", "error", "message", "path" }`
6. **Engine access only through the `RoutingEngine` interface** from `algorithm-engine`. Ship a `StubRoutingEngine` (profile `stub`) so the API works before the real engine is ready.
7. **Schema changes** are done by the database owner via Flyway migrations — never use `spring.jpa.hibernate.ddl-auto=create/update` outside throwaway local tests. Use `validate`.
8. **Configuration via environment variables** (`DB_URL`, `DB_USER`, `DB_PASSWORD`, `SERVER_PORT`). No secrets in `application.yml`.
9. **Constructor injection only.** No field `@Autowired`.
10. **Logging:** use SLF4J; no `System.out.println`; never log secrets.
11. **Cache the built graph** per building and invalidate it when nodes, edges or obstacles change.
12. Enable CORS only for configured origins.

## Testing
- Unit tests for services (Mockito for repositories and engine).
- `@WebMvcTest` for controllers — check status codes and JSON shape against the API doc.
- Integration tests with Testcontainers PostgreSQL for repositories and one end-to-end route request.
- New endpoint = at least: success case, validation failure, not-found case.

## Commands
```
./mvnw clean verify
./mvnw spring-boot:run -Dspring-boot.run.profiles=stub
./mvnw test
```

## Do not
- Edit `mobile-app/`, `algorithm-engine/` internals, or migration files owned by `database/`.
- Put routing/graph algorithm code in this module.
- Change the API shape without the contract-change process in Part 1.

---

# Part 4: Pathfinding engine (pure Java)

*Applies to this module only. Part 1 rules also apply.*

## Scope
A standalone, dependency-free Java library that computes routes over a multi-floor building graph. It is consumed by `backend` as a Maven dependency.

## Hard constraints
- **No Spring, no JPA, no HTTP, no database, no file or network I/O.** Pure Java 21 + JUnit only.
- **Deterministic:** the same input must always produce the same output (define tie-breaking explicitly).
- **Immutable inputs:** never mutate the graph passed in.
- **Thread-safe by design:** a built `Graph` can be shared across requests.

## Public API (stable — changes follow the contract-change process)
```java
public interface RoutingEngine {
    RouteResult findRoute(Graph graph, RouteRequest request);
}
```
- `RouteRequest`: `startNodeId`, `endNodeId`, `accessibleOnly` (boolean), optional `avoidNodeIds` / `avoidEdgeIds`
- `RouteResult`: ordered `List<Node>` path, `totalDistance`, `estimatedSeconds`, `floorsVisited`, or a clear "no route" outcome (never `null`, never an exception for "no route")
- Domain types: `Graph`, `Node`, `Edge`, `Connector` (`STAIRS`, `ELEVATOR`, `RAMP`), `Obstacle`

## Package structure
```
com.indoornav.engine
├── model/        # Node, Edge, Graph, Connector, Obstacle (immutable)
├── routing/      # RoutingEngine, RouteRequest, RouteResult
├── algorithm/    # DijkstraRouter, AStarRouter, Heuristic
└── util/
```

## Algorithm rules
1. **Implement Dijkstra first**, fully tested. It is the correctness baseline.
2. **Then A\*** using an admissible heuristic: straight-line distance on the same floor; for cross-floor, add the minimum connector cost. A* must return the same total distance as Dijkstra on every test graph.
3. Use `PriorityQueue` and avoid re-scanning arrays. Document time/space complexity in Javadoc.
4. **Accessibility:** when `accessibleOnly` is true, exclude edges with `accessible = false` and stairs connectors.
5. **Obstacles:** blocked nodes/edges are skipped. Never route through them.
6. **Multi-floor:** floor changes happen only through `Connector` edges. Connector cost reflects type (elevator wait, stairs effort) — keep costs configurable, not magic numbers.
7. Edge cases that must be handled and tested: start == end, unknown node IDs, disconnected graph, all routes blocked, zero-length edges, single-node graph.

## Testing
- JUnit 5. Build small in-memory graphs inside tests — no external data files needed.
- Required test groups: shortest path correctness, accessibility filtering, obstacle avoidance, multi-floor routing, no-route handling, Dijkstra-vs-A* equivalence.
- Add a simple benchmark/large-graph test (e.g. generated 10k-node grid) to catch performance regressions.

## Commands
```
./mvnw clean verify
./mvnw test
```

## Do not
- Add third-party dependencies (other than test-scope libraries) without approval.
- Read from a database or parse JSON here — the backend maps data into `Graph`.
- Change public interfaces without notifying the backend owner.

---

# Part 5: Database, DevOps and testing

*Applies to this module only. Part 1 rules also apply.*

## Scope
PostgreSQL schema, migrations, sample data, local environment (Docker), CI pipeline, and cross-module integration testing.

## Stack
- PostgreSQL (use the version pinned in `docker-compose.yml`)
- **Flyway** migrations in `database/migrations/`
- Docker + Docker Compose
- GitHub Actions

## Structure
```
database/
├── migrations/       # V1__init_schema.sql, V2__..., never edited after merge
├── seed/             # sample building data for dev and tests
└── README.md
docs/DATABASE.md      # ER overview, table descriptions
.github/workflows/    # ci.yml
docker-compose.yml
.env.example
```

## Schema rules
1. Tables (initial): `buildings`, `floors`, `nodes`, `edges`, `connectors` (or edges with a connector type), `obstacles`.
2. Use `snake_case` names, plural table names, `id` primary keys (`BIGINT` identity or `UUID` — pick one and be consistent).
3. Add foreign keys with explicit `ON DELETE` behavior, `NOT NULL` where appropriate, and `CHECK` constraints (e.g. `distance > 0`).
4. Index foreign keys and anything used for lookup (`nodes.floor_id`, `edges.from_node_id`, `edges.to_node_id`).
5. **Migrations are append-only.** Never edit a migration that has been merged; add a new one. Name: `V<n>__<description>.sql`.
6. Every migration must run cleanly on an empty database and on top of the previous version.
7. Seed data must include at least: one building, two floors, an elevator and a stairs connector, an accessible and a non-accessible path, and one obstacle — enough to exercise every engine feature.
8. Update `docs/DATABASE.md` in the same PR as any schema change, and notify the backend owner.

## Docker and environment
- `docker-compose.yml` runs PostgreSQL (and optionally the backend). `docker compose up` must give a working dev database with migrations applied and seed data loaded.
- Configuration via `.env`; commit only `.env.example`. **Never commit real credentials.**
- Pin image versions; do not use `latest`.

## CI (GitHub Actions)
On every pull request:
1. Build and test `algorithm-engine`
2. Build and test `backend` (with a PostgreSQL service/Testcontainers)
3. `flutter analyze` and `flutter test` for `mobile-app`
4. Fail the PR if any step fails

Use dependency caching (Maven, Pub). Keep workflow files readable and commented. Branch protection on `main` should require passing CI and one review.

## Integration testing
- Maintain one end-to-end smoke test: start DB + backend, request a known route from the seed data, assert the expected path.
- Keep a shared set of sample route requests and expected results in `docs/` so every module can verify against the same cases.

## Commands
```
docker compose up -d
docker compose down -v          # reset local database
docker compose logs -f db
```

## Do not
- Modify application code in `mobile-app/`, `backend/` or `algorithm-engine/`.
- Edit an already-merged migration.
- Store secrets in the repo, workflow files or Docker images.

