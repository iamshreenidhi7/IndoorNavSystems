# Indoor Navigation — Project Tasks

**Stack:** Flutter (Dart) · Java 21 + Spring Boot · Standalone Java pathfinding engine · PostgreSQL · Docker · GitHub Actions

## Team and ownership

| ID | Role | Module | Main tech |
|----|------|--------|-----------|
| **P1** | Mobile frontend | `mobile-app/` | Flutter, Dart, Material 3, Riverpod, Dio, GoRouter |
| **P2** | Java backend | `backend/` | Spring Boot, REST, validation, JPA |
| **P3** | Pathfinding engine | `algorithm-engine/` | Java, graphs, Dijkstra, A* |
| **P4** | Database, DevOps, QA | `database/`, CI, Docker | PostgreSQL, Flyway, Docker, GitHub Actions |

**Legend:** `[ ]` to do · `[x]` done · `(P#)` owner · `(ALL)` whole team

**Timeline:** The plan below assumes about 12 weeks. Phase lengths are suggestions; stretch or compress them to your deadline, but keep the order.

## Phase overview

| Phase | Name | Duration | Goal |
|-------|------|----------|------|
| 0 | Setup and contracts | Week 1 | Repo, docs, agreed API, CI skeleton |
| 1 | Foundations | Weeks 2–3 | Each module works alone with mock/sample data |
| 2 | Core features | Weeks 4–6 | Real single-floor routing end to end |
| 3 | Integration | Week 7 | All modules connected, no mocks |
| 4 | Advanced features | Weeks 8–9 | Multi-floor, obstacles, accessibility, A* |
| 5 | Testing and hardening | Weeks 10–11 | Quality, performance, security, polish |
| 6 | Deployment and release | Week 12 | Deployed backend, installable Android/iOS builds |

---

## Phase 0 — Setup and contracts (Week 1)

**Goal:** Everyone can start working in parallel without blocking each other.

### Tasks

- [ ] (P4) Create the GitHub repository with the agreed folder structure:
  ```
  indoor-navigation/
  ├── mobile-app/
  ├── backend/
  ├── algorithm-engine/
  ├── database/
  ├── docs/
  └── docker-compose.yml
  ```
- [ ] (P4) Set up branch protection on `main`: pull requests required, at least one review, CI must pass
- [ ] (P4) Add `.gitignore`, `README.md`, PR template and issue templates
- [ ] (ALL) Write `docs/ARCHITECTURE.md`: modules, responsibilities, data flow, coding rules
- [ ] (ALL) Define the shared data model in `docs/DATABASE.md`: Building, Floor, Node, Edge, Room, Obstacle, Connector (stairs/elevator)
- [ ] (P2 + P1) Write `docs/API.md` with request and response examples for every endpoint:
  - `GET /buildings`
  - `GET /buildings/{id}/floors`
  - `GET /buildings/{id}/floors/{floorId}/map`
  - `GET /rooms/search?q=`
  - `POST /routes` (start, destination, accessibility options)
  - `POST /obstacles` / `DELETE /obstacles/{id}`
- [ ] (P2 + P3) Define the engine interface (Java interface or JAR boundary) the backend will call, for example `RouteResult findRoute(RouteRequest request)`
- [ ] (P1) Initialize the Flutter project and confirm it runs on Android (and on iOS if a Mac is available)
- [ ] (P2) Initialize the Spring Boot project (Java 21, Maven or Gradle) with a health endpoint
- [ ] (P3) Initialize the engine as a standalone Java module with a unit-test setup
- [ ] (P4) Add a basic `docker-compose.yml` (PostgreSQL + backend placeholder)
- [ ] (P4) Add a first GitHub Actions workflow: build and test each module on pull requests
- [ ] (ALL) Agree on conventions: naming, commit message style, branch naming (`feature/<module>-<name>`), code formatting

### Exit criteria

- [ ] All four projects build locally and in CI
- [ ] `API.md` and `ARCHITECTURE.md` are reviewed and approved by everyone
- [ ] Each person has a clear module they own

---

## Phase 1 — Foundations (Weeks 2–3)

**Goal:** Each module works independently using mock or sample data.

### P1 — Mobile frontend

- [ ] Set up app structure (feature folders, theme, Material 3)
- [ ] Add Riverpod, Dio, GoRouter and wire them up
- [ ] Create typed data models matching `API.md` (Freezed/JSON serialization optional)
- [ ] Build a mock API service returning hard-coded JSON
- [ ] Build screens as static/mock versions:
  - Home / building selection
  - Floor map view
  - Room search with results list
  - Route display
- [ ] Render a simple floor map from sample SVG or `CustomPainter` data
- [ ] Add floor switcher UI

### P2 — Backend

- [ ] Project layout: controller / service / repository / dto packages
- [ ] Implement endpoints with in-memory sample data
- [ ] Add request validation and a consistent error response format
- [ ] Add global exception handling
- [ ] Add OpenAPI/Swagger documentation
- [ ] Add a `MockRoutingEngine` implementing the agreed engine interface

### P3 — Pathfinding engine

- [ ] Implement graph classes: `Node`, `Edge`, `Graph`
- [ ] Implement Dijkstra's algorithm for single-floor graphs
- [ ] Define `RouteRequest` and `RouteResult` (path nodes, total distance, estimated time, step list)
- [ ] Load a hard-coded sample graph for testing
- [ ] Write unit tests: shortest path, unreachable destination, same start and end

### P4 — Database, DevOps, testing

- [ ] Design the PostgreSQL schema and ER diagram
- [ ] Set up Flyway (or Liquibase) migrations
- [ ] Create tables: buildings, floors, nodes, edges, rooms, obstacles, connectors
- [ ] Write seed data for one sample building with 2–3 floors
- [ ] Document one-command local setup in the README
- [ ] Extend CI: run tests and report coverage for each module

### Exit criteria

- [ ] Flutter app shows screens with mock data
- [ ] Backend serves sample data via Swagger
- [ ] Engine finds correct shortest paths on a test graph
- [ ] Database starts with migrations and seed data via Docker

---

## Phase 2 — Core features (Weeks 4–6)

**Goal:** Real single-floor navigation works through the full stack.

### P1 — Mobile frontend

- [ ] Replace mock service with real Dio calls (configurable base URL per environment)
- [ ] Render real floor maps from backend data (rooms, corridors, doors)
- [ ] Implement room search with debouncing and result selection
- [ ] Let the user pick start and destination (search, tap on map, or "current location" placeholder)
- [ ] Draw the computed route on the map
- [ ] Show turn-by-turn instruction list
- [ ] Add loading, empty and error states for every screen
- [ ] Add pan and zoom to the map

### P2 — Backend

- [ ] Connect to PostgreSQL with JPA/Hibernate and repositories
- [ ] Replace in-memory data with database queries
- [ ] Implement `POST /routes`: validate input, load the graph, call the engine, map the result to a DTO
- [ ] Implement room search (name, number, type)
- [ ] Add caching for building graphs (for example Caffeine) to avoid reloading on each request
- [ ] Add logging and request tracing

### P3 — Pathfinding engine

- [ ] Add a graph loader that builds the graph from backend data
- [ ] Generate human-readable steps (go straight, turn left, arrive at destination)
- [ ] Calculate distance in metres and estimated walking time
- [ ] Add edge weights and handle bidirectional edges
- [ ] Package the engine as a library the backend depends on
- [ ] Add edge-case tests (disconnected graph, missing nodes, large graph)

### P4 — Database, DevOps, testing

- [ ] Create realistic seed data for the target building (real floor plan coordinates if available)
- [ ] Write a tool or script to import floor-plan data (CSV/JSON → nodes and edges)
- [ ] Add database indexes for search and graph queries
- [ ] Add integration tests with Testcontainers (backend + PostgreSQL)
- [ ] Add Docker images for the backend and engine
- [ ] Set up `docker-compose` to run the full backend stack with one command

### Exit criteria

- [ ] A user can search for a room, pick a destination and see a correct route on one floor
- [ ] Backend tests run against a real PostgreSQL instance in CI
- [ ] Route response time is acceptable on the seed building (target under 500 ms)

---

## Phase 3 — Integration (Week 7)

**Goal:** All modules are connected with no mocks, and the whole team can demo the system.

### Tasks

- [ ] (P2 + P3) Replace `MockRoutingEngine` with the real engine in every environment
- [ ] (P1 + P2) Run the app against the real backend on Android emulator and a real device
- [ ] (P1) Verify the app on iOS simulator (requires macOS and Xcode)
- [ ] (P4) Write end-to-end tests: app → API → engine → database
- [ ] (P4) Set up a shared dev environment (hosted or Docker-based) everyone can reach
- [ ] (ALL) Fix contract mismatches found during integration and update `API.md`
- [ ] (ALL) Hold an integration review: walk through three or four full user flows together
- [ ] (P2) Review API error handling against what the app actually needs
- [ ] (P1) Handle offline and slow-network cases gracefully

### Exit criteria

- [ ] Full flow works on a real Android device
- [ ] Full flow works on iOS simulator
- [ ] No mock data is used in the integrated build
- [ ] `API.md` matches the real implementation

---

## Phase 4 — Advanced features (Weeks 8–9)

**Goal:** Deliver the features that make this more than a basic shortest-path app.

### Multi-floor routing

- [ ] (P3) Model stairs, elevators and ramps as connector edges between floors
- [ ] (P3) Route across floors, choosing the best connector
- [ ] (P2) Return floor transitions in the route response
- [ ] (P1) Show the route per floor with automatic floor switching and "take elevator to floor 3" steps
- [ ] (P4) Add multi-floor seed data and tests

### Obstacle management

- [ ] (P2) Implement endpoints to create, update, list and remove obstacles (blocked corridors, closed doors, temporary hazards)
- [ ] (P3) Exclude blocked edges and nodes during route calculation
- [ ] (P3) Return a clear "no route available" result with a reason
- [ ] (P1) Show obstacles on the map and let the user report one (or provide an admin screen)
- [ ] (P4) Add tests for rerouting around obstacles

### Accessibility options

- [ ] (P3) Add route options: avoid stairs, prefer elevators, minimum corridor width
- [ ] (P2) Accept accessibility parameters in `POST /routes`
- [ ] (P1) Add a route preferences screen
- [ ] (P1) Check screen-reader labels and text scaling

### A* optimization

- [ ] (P3) Implement A* with a suitable heuristic (Euclidean within a floor, connector-aware across floors)
- [ ] (P3) Benchmark Dijkstra against A* on larger graphs and document the results
- [ ] (P3) Let the engine select the algorithm via configuration

### Optional enhancements

- [ ] (P1) Recent searches and favourite locations (SharedPreferences)
- [ ] (P1) Dark mode
- [ ] (P2) Authentication for admin endpoints (obstacle management)
- [ ] (P1 + P2) Positioning support (QR codes at known points, or Wi-Fi/BLE if hardware is available)

### Exit criteria

- [ ] Routes work correctly across multiple floors
- [ ] Obstacles change routes in real time
- [ ] Accessibility options measurably change the result
- [ ] A* is faster than Dijkstra on benchmark graphs with identical route quality

---

## Phase 5 — Testing and hardening (Weeks 10–11)

**Goal:** The system is reliable, secure and ready for real users.

### Quality

- [ ] (P1) Widget tests and integration tests for the main flows
- [ ] (P2) Unit tests for services, controller tests, and contract tests against `API.md`
- [ ] (P3) Property-based or randomized tests comparing A* and Dijkstra results
- [ ] (P4) Test coverage thresholds enforced in CI
- [ ] (ALL) Manual test pass on at least two Android devices and one iPhone

### Performance

- [ ] (P3) Profile the engine on large graphs (thousands of nodes) and optimize hot spots
- [ ] (P2) Load test `POST /routes` and tune caching and DB connection pools
- [ ] (P1) Profile map rendering and fix jank on lower-end devices

### Security and reliability

- [ ] (P2) Input validation review, rate limiting, CORS configuration
- [ ] (P2) Secure all admin endpoints; keep secrets out of the repository
- [ ] (P4) Dependency vulnerability scanning in CI
- [ ] (P4) Database backup and restore procedure documented and tested
- [ ] (P2 + P4) Health checks, structured logging and basic metrics

### Polish

- [ ] (P1) UX review: consistent spacing, error messages, empty states, loading indicators
- [ ] (P1) App icon, splash screen, app name
- [ ] (ALL) Fix the bug backlog; triage into must-fix and later

### Exit criteria

- [ ] No known critical or high-severity bugs
- [ ] CI is green with agreed coverage thresholds
- [ ] Route requests meet the performance target under expected load

---

## Phase 6 — Deployment and release (Week 12)

**Goal:** The system is deployed and the app can be installed on both platforms.

### Backend and infrastructure

- [ ] (P4) Choose a hosting platform and provision it (VM, container service, or managed platform)
- [ ] (P4) Deploy backend, engine and PostgreSQL with Docker
- [ ] (P4) Set up HTTPS and a domain
- [ ] (P4) Add a CD workflow: merge to `main` → build → deploy to staging; tag → production
- [ ] (P4) Configure production environment variables and secrets
- [ ] (P2) Smoke-test production endpoints

### Mobile release

- [ ] (P1) Configure production API URL via build flavors or environment config
- [ ] (P1) Android: create a signing key, build a release APK/AAB, test on devices
- [ ] (P1) iOS: set up an Apple Developer account, signing, and provisioning (macOS and Xcode required)
- [ ] (P1) Distribute test builds (Firebase App Distribution, TestFlight, or direct APK)
- [ ] (P1) Prepare store assets if publishing: screenshots, description, privacy policy

### Documentation and handover

- [ ] (ALL) Finalize `README.md`, `ARCHITECTURE.md`, `API.md`, `DATABASE.md`
- [ ] (P4) Write a deployment and operations guide
- [ ] (P4) Add a guide for onboarding a new building (importing a floor plan)
- [ ] (ALL) Prepare the project demo and a short presentation
- [ ] (ALL) Retrospective: what worked, what to improve, future roadmap

### Exit criteria

- [ ] Backend is live and reachable over HTTPS
- [ ] Release builds install and work on Android and iOS
- [ ] Documentation is complete enough for a new developer to run the project in under an hour

---

## Working agreements

- Work only inside your own module unless a cross-module change is agreed and recorded in the docs.
- Every change goes through a feature branch and a pull request; nobody pushes directly to `main`.
- Update `API.md` **before** changing an endpoint, not after.
- Give every AI coding agent `docs/ARCHITECTURE.md` and `docs/API.md` as context, and tell it which module it may edit.
- Hold a short sync (15 minutes, two or three times a week) to unblock dependencies between modules.
- Review each other's AI-generated code; do not merge code nobody on the team understands.

## Key dependencies between people

| Blocker | Unblocked by | Workaround while waiting |
|---------|--------------|--------------------------|
| P1 needs real data | P2 endpoints, P4 seed data | Mock API service (Phase 1) |
| P2 needs routing | P3 engine | `MockRoutingEngine` (Phase 1) |
| P3 needs real graphs | P4 seed data and import tool | Hard-coded sample graph |
| Everyone needs CI and environments | P4 | Local Docker Compose |

## Risks to watch

- **iOS builds need macOS and Xcode.** Confirm that at least one team member has access to a Mac by Phase 3.
- **API drift.** Mitigated by updating `API.md` first and running contract tests.
- **Floor-plan data quality.** Wrong coordinates or disconnected graphs cause bad routes; validate graphs on import.
- **Scope creep.** Treat the optional enhancements in Phase 4 as stretch goals; protect Phases 3 and 5.
