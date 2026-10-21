# ARCHITECTURE: Indoor Navigation and Path Optimization System

| Field | Value |
|---|---|
| Status | Draft v0.1, awaiting owner approval on flagged decisions |
| Depends on | PRD.md, DESIGN.md, Master Project Prompt |
| Audience | Four-person team and AI coding agents |
| Scope | How the system is built: components, boundaries, data flow, conventions. No code is defined here. |

## 0. How to use this document

- This file defines **how** to build what PRD.md specifies and DESIGN.md styles. When they conflict on behaviour, PRD.md wins; on visuals, DESIGN.md wins; on structure and technology, this file wins.
- **[ASSUMPTION AA-n]** marks something assumed. **[DECISION AD-n]** marks a choice needing owner approval. All are collected in Section 19. Agents follow the recommended default until a decision is made.
- Items are labelled **MVP** (build now) or **Future** (do not build now; the design leaves room for it). Section 18 lists the Future items.
- The product is a generic platform. No core code, table, endpoint or screen may be mall-specific; mall content exists only as seed data.

---

## 1. Architecture summary

A **three-tier system with one isolated library**:

1. **Flutter mobile app** (Android and iOS, one codebase) presents data and never computes routes.
2. **Spring Boot backend**, a single deployable service, exposes a JSON REST API, enforces security and validation, and orchestrates routing.
3. **PostgreSQL** is the only datastore.
4. **Algorithm engine**, a plain Java library with no framework, database or HTTP dependency, is called in-process by the backend.

It is a **modular monolith**: one backend process, one database, one library module. There are no microservices, queues, caches or message brokers in the MVP.

### Design principles

1. **Simplest reliable option wins.** Every dependency and moving part must justify itself against the PRD.
2. **Isolated engine.** Pathfinding is testable with in-memory graphs and knows nothing about HTTP or SQL.
3. **Relational truth.** The graph lives in normalised tables, not opaque documents.
4. **Fresh state, no stale caches.** A route calculation always sees the latest committed obstacle state (PRD FR-32).
5. **Data-driven platform.** New environments arrive as data and configuration, never as code.
6. **Contracts first.** API.md and the generated OpenAPI document are agreed before integration.
7. **Student-readable code.** Plain Java, records for DTOs, manual mapping, no annotation-processing magic.

---

## 2. Technology stack and reasoning

The stack is fixed by the Master Project Prompt. One item (Spring Boot version) needs approval (AD-1).

| Layer | Choice | Why this, and why not something heavier |
|---|---|---|
| Mobile | Flutter, Dart, Material 3 | One codebase for both platforms (PRD NFR-10); Material 3 maps directly to DESIGN.md |
| State management | Riverpod (`flutter_riverpod`, plain Notifier APIs, no code generation) | Testable, no BuildContext dependency, fewer moving parts than generator-based setups |
| Navigation | GoRouter | Declarative, supports the stack-based model in DESIGN.md Section 10 |
| HTTP | Dio | Interceptors for auth header and error mapping; timeouts |
| Map rendering | `flutter_svg` for the floor plan, `CustomPainter` for route, markers and blocked overlays, inside one pan/zoom container | Per the prompt; avoids a map SDK or GIS stack |
| Local settings | SharedPreferences | Only simple device-level preferences (PRD FR-36). SQLite/Drift not needed |
| Backend language | Java 21 LTS | Mandated; records, modern collections, long-term support |
| Backend framework | Spring Boot (version per AD-1), Spring Web, Spring Data JPA, Spring Validation, Spring Security (admin endpoints only) | Mandated; mature, well documented, suits layered architecture |
| API docs | springdoc OpenAPI / Swagger UI (release line matching the Boot version) | Generated contract that the mobile developer reads |
| Build | Maven multi-module | Backend depends on engine without circular dependencies |
| Database | PostgreSQL (current stable major, AA-2) | Relational integrity, constraints, indexes; mandated |
| Persistence | Hibernate through Spring Data JPA | Mandated; schema validated, not generated |
| Migrations | Flyway | Mandated; versioned, repeatable, reviewed in PRs |
| Containers | Docker, Docker Compose | One-command local environment (PRD NFR-14) |
| CI | GitHub Actions | Mandated; runs tests on every pull request |
| Backend tests | JUnit 5, Mockito, Spring test slices, Testcontainers (AD-5) | Real PostgreSQL in tests avoids H2 divergence |
| Mobile tests | `flutter_test`, `integration_test`, `mocktail` | No code generation required for mocks |

### Deliberately not used

Lombok and MapStruct (hidden generation hurts student readability), Redis or any cache, message brokers, GraphQL, gRPC, Kubernetes, API gateway, service mesh, external graph libraries (Dijkstra and A* are implemented in-house), Firebase or any BaaS, third-party map SDKs, analytics SDKs, code-generated JSON models (AD-7), secure-storage plugins (admin token is held in memory only, Section 11).

---

## 3. System context and components

```
┌──────────────────────┐   HTTPS / JSON    ┌──────────────────────────────────────┐   JDBC   ┌────────────┐
│  Flutter app         │ ────────────────► │  Spring Boot backend                 │ ───────► │ PostgreSQL │
│  (Android, iOS)      │ ◄──────────────── │  controller → service → repository   │ ◄─────── │            │
│  - UI, map rendering │                   │        │                             │          └────────────┘
│  - device settings   │                   │        ▼ in-process call             │
└──────────────────────┘                   │  algorithm-engine (Java library)     │
                                           └──────────────────────────────────────┘
```

### Dependency rules (enforced in review)

- `mobile-app` → backend REST API only. It has no code dependency on any other module.
- `backend` → `algorithm-engine`. Never the reverse.
- `algorithm-engine` → JDK only (plus test libraries). No Spring, JPA, Jackson or servlet classes.
- Controllers → services → repositories. Controllers never touch repositories; services never touch HTTP types.
- Entities never leave the service layer; the API exposes DTOs only.
- The database is accessed only by the backend. The mobile app never sees SQL or entities.

---

## 4. Repository and folder structure

Single GitHub monorepo. Directory ownership follows the four-person split.

```
indoor-navigation/
├── pom.xml                      Maven parent (modules: algorithm-engine, backend)
├── docker-compose.yml           Local environment: database + backend
├── .env.example                 Documented variables, no real secrets
├── .gitignore
├── README.md                    Quick start, platform run instructions
├── .github/
│   ├── CODEOWNERS               Maps directories to owners
│   ├── pull_request_template.md Checklist incl. contract, tests, docs
│   └── workflows/               backend.yml, mobile.yml
├── docs/
│   ├── PRD.md  DESIGN.md  ARCHITECTURE.md
│   ├── API.md  DATABASE.md  AGENTS.md
│   └── testing/                 Test plan, demo script, scenario checklist
├── algorithm-engine/            Owner: Person 3
│   └── src/{main,test}/java/…   model, graph, algorithm, cost, result packages
├── backend/                     Owner: Person 2
│   ├── Dockerfile
│   └── src/{main,test}/…        See Section 6
├── database/                    Owner: Person 4
│   ├── migrations/              Versioned schema migrations (source of truth)
│   ├── seed/                    Demo data (mall) as migration-style scripts
│   └── floorplans/              SVG floor plans referenced by floors (AD-4)
├── infrastructure/              Owner: Person 4
│   └── …                        Compose helpers, scripts, environment notes
└── mobile-app/                  Owner: Person 1
    └── lib/ test/ integration_test/   See Section 9
```

Notes:

- **Migrations location (AD-6):** `database/migrations` is the single source of truth. The backend build packages it so Flyway finds it on the classpath. Nothing duplicates it inside `backend/`.
- **Seed data** lives in `database/seed` and is loaded only when demo seeding is enabled (Section 7.5).
- **Branching:** `main` always runs. Work happens on `feature/*` branches merged by pull request, with at least one reviewer outside the owning domain for cross-module changes.

---

## 5. Algorithm engine architecture (MVP)

### 5.1 Purpose and boundary

A library that turns an in-memory weighted graph plus a routing request into a result. It owns pathfinding, cost models, constraints and route metrics. It owns no persistence and no wording of user-facing text.

### 5.2 Contract

**Input (routing request):**
- Immutable navigation graph (nodes, edges)
- Start node ID, destination node ID
- Preference (SHORTEST, FASTEST, ACCESSIBLE, AVOID_STAIRS, PREFER_ELEVATOR)
- Blocked node IDs and blocked edge IDs (supplied by the caller from active obstacles)
- Cost configuration (speeds, fixed transition times, penalty factors)

**Output (routing result):**
- Status: FOUND or NO_ROUTE
- If FOUND: ordered node IDs, ordered edge IDs, total distance (metres), total estimated time (seconds), floor transitions (from floor, to floor, connection type, positions in the path)
- If NO_ROUTE: no path; the backend determines the reason (Section 6.4)

**Errors:** unknown start or destination node raises an engine-level invalid-input exception; the backend translates it. Start equal to destination returns FOUND with zero distance.

**Entry point:** one stable interface with two interchangeable implementations (A* and Dijkstra) behind it. The backend uses A*. Dijkstra remains for verification tests and as a fallback.

### 5.3 Graph model

- Immutable after construction; adjacency list keyed by node.
- Nodes: ID, floor ID, X, Y (floor plan units), type, accessible flag.
- Edges: ID, two node IDs, distance (metres), time (seconds), connection type, accessible flag, bidirectional flag. A bidirectional edge appears in both nodes' adjacency lists.
- The engine's model is **separate from the JPA entities**. The backend contains the mapper.

### 5.4 Cost model

Each preference defines one scalar objective and a filter. Distance and time are always computed and reported regardless of objective, since they are different quantities (Master Prompt, Section 5).

| Preference | Objective minimised | Hard filter | Surcharge |
|---|---|---|---|
| SHORTEST | Distance | None | None |
| FASTEST | Time | None | None |
| ACCESSIBLE | Time | Exclude edges and nodes not flagged accessible | None |
| AVOID_STAIRS | Time | None | Large multiplier on stair edges (AD in PRD D-4; default in AA-6) |
| PREFER_ELEVATOR | Time | None | Moderate multiplier on stair edges |

- **PREFER_ELEVATOR's "advantage" is implemented as a stair surcharge, never as a discount on elevators.** Surcharges only raise costs, so costs stay positive and the heuristic stays admissible.
- Blocked nodes and edges are excluded for every preference.
- When AVOID_STAIRS forces a stair edge, the result flags it so the backend can add a warning (PRD 4.5).
- Edge time comes from the connection type's speed applied to distance, plus any fixed time (for example elevator wait and ride), unless the edge stores an explicit override.

### 5.5 Heuristic (A*)

- **SHORTEST:** straight-line horizontal distance in metres between the current node and the goal, using each floor's metres-per-unit scale.
- **Time objectives:** the same distance divided by the fastest configured horizontal speed.
- **Why it is admissible:** surcharges only increase real costs; vertical edges carry explicit costs at least as large as their horizontal displacement; and all floors share one aligned coordinate frame (AA-3).
- **Data invariant (validated by tests and at seed load):** every edge's distance is greater than or equal to the straight-line horizontal distance between its end nodes, and every cost is strictly positive.
- If a building cannot satisfy the aligned-frame assumption, its heuristic is set to zero and A* behaves as Dijkstra for that building. Results stay correct.

### 5.6 Engine test obligations

Direct path, multiple alternatives, disconnected graph, start equals destination, unknown nodes, blocked edges and nodes, each preference (including cases where SHORTEST and FASTEST differ), accessibility exclusion, multi-floor routes, and **A* versus Dijkstra cost equality over many randomly generated graphs** (Section 16).

---

## 6. Backend architecture (MVP)

### 6.1 Layers and packages

Layered, as in the Master Prompt. One base package, with these sub-packages:

| Package | Responsibility |
|---|---|
| `controller` | HTTP only: parse, validate, delegate, return DTOs and status codes. No business logic |
| `service` | Business logic and transactions: building queries, destination search, route orchestration, obstacle management, instruction building |
| `routing` | Adapter between the backend and the engine: graph loader, entity-to-engine mapper, result-to-DTO mapper |
| `repository` | Spring Data JPA repositories only |
| `entity` | JPA entities, one per table |
| `dto` | Request and response records; the API contract |
| `mapper` | Manual entity-to-DTO mapping |
| `security` | Security configuration, token issue and verification, admin user lookup |
| `config` | Typed configuration properties, OpenAPI metadata |
| `exception` | Domain exceptions and the global error handler |

### 6.2 Responsibilities by use case

| Use case | Controller | Service | Notes |
|---|---|---|---|
| Browse buildings and floors | Read endpoints | Query services | Read-only transactions |
| Floor map | Map endpoint | Map service | Assembles plan metadata, nodes, edges, POIs, rooms, blocked flags |
| Destination search | POI and room endpoints | Search service | Case-insensitive partial match, result limit |
| Route | Route endpoint | Route service | Orchestrates Section 6.3 |
| Obstacles | CRUD endpoints (admin) | Obstacle service | Validates target, transactional writes, optimistic locking |

### 6.3 Route orchestration

1. Validate the request (nodes exist, belong to the **same building**, preference is valid).
2. Load, in one transaction, the building's nodes, edges and connection types (graph topology) and the **active obstacles** for the building.
3. Translate active obstacles to blocked node IDs and blocked edge IDs.
4. Map to the engine graph and call the engine (A*) with the preference.
5. If FOUND: build the response (path with coordinates, distance, duration, floor transitions, instructions, warnings).
6. If active obstacles exist, **also compute the baseline route without obstacles** for the same preference. If the baseline differs and used a now-blocked element, add a DETOUR warning with the extra distance and the affected obstacle. This implements PRD FR-33 without the server having to remember earlier requests.
7. If NO_ROUTE: diagnose the reason (Section 6.4) and return a NO_ROUTE result.
8. Never cache a result across requests (AD-3 covers graph loading).

### 6.4 No-route diagnosis

When the engine returns NO_ROUTE, the backend re-runs, in order, until one succeeds:

1. Same preference **without obstacles**: success means reason OBSTACLE, with the affected obstacle types for the message.
2. SHORTEST **without obstacles and without accessibility filtering**: success means reason ACCESSIBILITY_CONSTRAINT.
3. Otherwise reason DISCONNECTED.

This gives the app the information DESIGN.md Section 11.2 needs ("{reason, if known}"). Diagnosis runs only on failure.

### 6.5 Instructions

The backend builds ordered instruction steps from the path: start, walk segments (with distance), floor changes (connection type, from and to floor), and arrival. Each step carries structured fields (type, distance, floors, connection type, node IDs) **and** a default English text (AD-10). Left and right turn wording is a Should item (PRD FR-28) derived from path geometry in the same builder.

### 6.6 Transactions and concurrency

- Services own transactions. Read endpoints use read-only transactions.
- Route calculation reads graph and obstacles in one transaction so it sees one consistent snapshot.
- Obstacle entities carry a version for **optimistic locking**; a concurrent edit by two administrators returns a conflict error instead of silently overwriting.
- Open-session-in-view is disabled; lazy loading never leaks into controllers.

### 6.7 Routing configuration

Speeds, fixed transition times and surcharge factors are loaded from the `connection_types` table (speeds and times) and typed application properties (surcharges), never hard-coded in the engine. Defaults are in AA-6.

---

## 7. Database interaction (MVP)

### 7.1 Principles

- PostgreSQL is the single source of truth. Hibernate **validates** the schema against Flyway migrations; it never generates or alters it.
- All schema changes are Flyway migrations, reviewed like code. Applied migrations are never edited.
- Constraints live in the database as well as in application code (PRD Section 8.2).
- IDs are 64-bit integers generated by the database and are stable across requests and restarts.

### 7.2 Tables and relationships

Detailed columns belong in DATABASE.md. The structure is fixed here.

| Table | Purpose | Key relationships and rules |
|---|---|---|
| `buildings` | Indoor environments | Parent of floors |
| `floors` | Levels | FK to building; unique level per building; plan asset path; plan width and height; metres-per-unit scale |
| `connection_types` | Lookup: type code, speed, fixed time, default accessibility, vertical flag | Referenced by edges; new types are data, not code |
| `nodes` | Navigable points | FK to floor; X, Y; type; accessible flag; optional display name |
| `edges` | Connections | FKs to two nodes; FK to connection type; distance > 0; time > 0; accessible; bidirectional; check that the nodes differ |
| `rooms` | Named spaces | FK to floor; category |
| `points_of_interest` | Searchable destinations | FK to floor; **unique FK to one node**; optional FK to room; category |
| `obstacles` | Restrictions | Nullable FK to edge and nullable FK to node with a **check constraint requiring exactly one**; type; description; status; created and updated timestamps; version (AD-11) |

Relationship summary: building 1—n floors; floor 1—n nodes, rooms, POIs; node n—n node through edges; POI 1—1 node; obstacle n—1 (edge or node).

### 7.3 Indexes

Foreign-key indexes (nodes by floor, edges by each node, POIs by floor, obstacles by edge and node), an index on obstacle status, and a lower-cased name index for POI search. Trigram or full-text search is Future.

### 7.4 JPA conventions

- One entity per table; explicit fetch strategy (lazy by default); no entity exposed in any API.
- Queries that load the graph use fetch-joins or dedicated read queries to avoid N+1 selects.
- Custom queries use parameters, never string concatenation.
- Repositories contain no business logic.

### 7.5 Seeding

Demo data (the sample mall) lives in `database/seed`. It is loaded by adding the seed location to Flyway's locations **only when demo seeding is enabled** by configuration (SEED_DEMO_DATA). Production-style runs get the schema only. This keeps mall content out of the core schema history (AD-6).

### 7.6 Data integrity tests

A test loads the seed data and verifies the invariants in Section 5.5, that every POI node exists, that the demonstration scenarios in PRD Section 8.3 behave as documented, and that every obstacle target resolves.

---

## 8. API architecture (MVP)

### 8.1 Conventions

- REST over HTTPS, JSON, UTF-8. Resource-oriented paths under `/api` (AD-9: unversioned for the MVP; breaking changes introduce a version prefix later).
- DTO records for every request and response. Bean validation on request DTOs.
- Plural nouns, IDs in paths, filters in query parameters. Timestamps are ISO-8601 UTC. Distances in metres, durations in seconds.
- OpenAPI is generated from the code; API.md documents behaviour and examples. The JSON examples in the Master Prompt are illustrative; the generated spec is the contract.
- Lists are small in the MVP, so there is no pagination; search endpoints take a result limit.

### 8.2 Endpoints

| Endpoint | Access | Purpose |
|---|---|---|
| `GET /api/health` | Public | Liveness and database reachability |
| `GET /api/buildings` | Public | List buildings |
| `GET /api/buildings/{buildingId}` | Public | Building details |
| `GET /api/buildings/{buildingId}/floors` | Public | Ordered floors |
| `GET /api/floors/{floorId}/map` | Public | Everything needed to draw one floor |
| `GET /api/rooms` | Public | Rooms, filtered by building or floor |
| `GET /api/points-of-interest` | Public | Search destinations: building, query text, category, limit |
| `GET /api/buildings/{buildingId}/nodes` | Public | Named selectable nodes (entrances, POIs, transitions) for start selection |
| `POST /api/navigation/route` | Public | Compute a route (read-only; POST because of the request body) |
| `POST /api/auth/login` | Public | Administrator sign-in, returns a short-lived token |
| `GET /api/obstacles` | Admin | List obstacles by building and status |
| `POST /api/obstacles` | Admin | Create |
| `PUT /api/obstacles/{obstacleId}` | Admin | Update, including resolving |
| `DELETE /api/obstacles/{obstacleId}` | Admin | Delete |

The public map and route responses expose blocked connections and their visitor-facing description, which satisfies "view blocked connections" (US-12) without making the obstacle list public.

### 8.3 Floor map response (fields)

Floor identity and display name; plan asset URL; plan width and height; metres-per-unit scale; nodes (ID, X, Y, type, name, accessible); edges touching the floor (ID, end node IDs, connection type, accessible, blocked flag, obstacle type and description when blocked, and the other floor's ID for vertical connections); rooms; POIs.

### 8.4 Route request and response (fields)

**Request:** start node ID, destination node ID, preference (defaults to SHORTEST when omitted).

**Response:**
- `status`: FOUND or NO_ROUTE
- If FOUND: ordered path nodes (ID, floor ID, X, Y, type, name) so the app can draw without further calls; distance in metres; estimated duration in seconds; selected preference; floor transitions; instruction steps; warnings (typed, with code, message, related obstacle IDs, extra distance where relevant)
- If NO_ROUTE: reason code (OBSTACLE, ACCESSIBILITY_CONSTRAINT, DISCONNECTED), related obstacle types, and a message

NO_ROUTE is returned with a **success HTTP status** because it is a designed outcome, not an error (PRD 11; DESIGN 11.3) (AD-12).

### 8.5 Floor-plan assets

SVG floor plans are stored in the repository (`database/floorplans`), referenced by path in the `floors` table, and served by the backend as static files. The SVG's coordinate system equals the floor's plan width and height, so route coordinates align without transformation (AD-4).

---

## 9. Mobile architecture (MVP)

### 9.1 Layering (feature-first)

```
mobile-app/lib/
├── main.dart, app.dart          Entry, root widget, ProviderScope
├── core/
│   ├── config/                  API base URL and build-time settings
│   ├── network/                 Dio client, interceptors, timeouts
│   ├── errors/                  AppFailure types, Dio-to-failure mapping
│   ├── router/                  GoRouter configuration
│   ├── theme/                   Tokens from DESIGN.md, one central theme
│   └── widgets/                 Shared components (DESIGN.md Section 8)
└── features/
    ├── buildings/    floors/    search/    routing/
    ├── map/          settings/  auth/      obstacles_admin/
        each with:  data/ (API + repositories)  domain/ (models)
                    application/ (Riverpod notifiers)  presentation/ (screens, widgets)
```

Tests mirror this structure under `test/`, with end-to-end flows in `integration_test/`.

### 9.2 Rules

- **Presentation** renders state and sends user intents; it contains no networking.
- **Application** (Riverpod notifiers) holds state and calls repositories.
- **Data** repositories call the API client and return domain models or typed failures.
- **Domain models** are plain immutable Dart classes with hand-written JSON parsing (AD-7).
- No colours, sizes or spacing in screens; they come from the theme tokens (DESIGN.md Section 16).
- The app never implements pathfinding and never caches routes.

### 9.3 The map session state

The map screen is driven by one state holder following DESIGN.md Section 8.4:

Explore → DestinationSelected → RoutePreview → Navigating → (Rerouted or NoRoute)

Held in this state: selected building, current floor, start node, destination, preference, latest route result, current instruction index. Rotation and brief backgrounding preserve it. Back steps through these states before leaving the map.

### 9.4 Map rendering

One pan/zoom container holds two layers sharing a single coordinate transform: the SVG floor plan, and a custom-painted overlay (route, markers, blocked connections, optional nodes). Because both are children of the same transformed container, alignment holds at any zoom. Marker hit areas are sized in screen units (not plan units) so the 48 dp minimum holds at every zoom. Label priority and visibility follow DESIGN.md Section 7.5.

### 9.5 API access and errors

- Base URL is a build-time setting (Section 15).
- Timeouts: connect 5 s, receive 10 s; route requests allow 15 s. The UI shows the "taking longer" line after about 10 s (DESIGN 11.3).
- A single interceptor adds the admin token when present, and maps responses to typed failures: no connection, timeout, validation, not found, unauthorised, conflict, server error. Screens map failures to the states in DESIGN.md Section 11. There are no automatic retries; the user retries.

### 9.6 Rerouting trigger

The server holds no per-user state, so the app learns of changes only when it asks. The route is recalculated when the user requests it, when preferences change, and (Should) when the app returns to the foreground during navigation. Polling and push are Future.

### 9.7 Local storage

SharedPreferences holds: last building, default preference, accessibility setting. The admin token is **not** persisted (Section 11).

---

## 10. Data flows

**F1. App start.** Splash reads settings → calls health → loads buildings → Home. On failure, Splash shows the connection error with Retry.

**F2. Open a building.** App fetches floors, then the map for the default floor (lowest or entrance floor) → downloads the SVG plan → renders plan and overlay.

**F3. Destination search.** User types → app queries points-of-interest with building and text (after a short delay) → results list → selection sets the destination and returns to the map.

**F4. Route request.**
1. App sends start, destination and preference.
2. Backend validates, loads graph and active obstacles, runs A*.
3. Backend builds path, instructions, transitions and warnings (and baseline comparison if obstacles exist).
4. App draws the current-floor segment, shows the summary and steps, and marks floors that contain route segments.

**F5. Obstacle created by an administrator, then a visitor re-routes.**
1. Admin signs in, sends a create request with a valid token.
2. Backend validates the target, writes the obstacle in one transaction.
3. Any later route request reads the updated state and returns the alternative route with a DETOUR warning, or NO_ROUTE with reason OBSTACLE.
4. Map responses now mark the connection as blocked.

**F6. Admin sign-in.** App posts credentials → backend verifies against configured credentials → returns a short-lived token → app keeps it in memory → attached to admin calls → on expiry, calls return unauthorised and the app shows the signed-out state.

---

## 11. Authentication and authorization (MVP)

Consistent with PRD Section 7 and decision D-3.

| Aspect | Design |
|---|---|
| Roles | Anonymous visitor (no sign-in) and ADMIN. No other roles, no registration |
| Admin identity | One account defined by environment configuration: username and a **password hash** (never the plain password) |
| Sign-in | `POST /api/auth/login` verifies the credentials with a strong password hasher and returns a signed, short-lived token (default 60 minutes, no refresh) (AD-2) |
| Token | Signed with a secret from configuration; verified by Spring Security on admin endpoints |
| Authorization | Obstacle endpoints require ADMIN; everything else is public. Default for any unlisted endpoint is **deny** |
| Mobile storage | The token lives **in memory only**. Restarting the app requires signing in again, which is acceptable for a rarely used admin function and avoids a secure-storage dependency |
| Login abuse | Failed attempts are throttled with a simple in-memory counter (Should); errors never reveal which credential was wrong |
| Statelessness | No server-side sessions, so the backend remains horizontally scalable |

Future: multiple admin accounts in the database, role management, refresh tokens, secure token storage, per-building permissions.

---

## 12. Security boundaries

### 12.1 Trust zones

| Zone | Trust | Rule |
|---|---|---|
| Mobile app and device | Untrusted | May contain no secrets; all input is validated again by the backend |
| Network | Untrusted | HTTPS everywhere outside local development |
| Backend | Trusted, enforcing | Authenticates, authorises, validates, owns all business rules |
| Algorithm engine | Trusted, in-process | Receives only validated, already-mapped data |
| Database | Private | Reachable only from the backend; not published to the host or internet outside local development |

### 12.2 Controls

| Concern | Control |
|---|---|
| Secrets | Only in environment variables or the platform secret store; `.env` is git-ignored; `.env.example` has placeholders; CI uses GitHub secrets |
| Input | Bean validation on every DTO; IDs checked for existence and same-building consistency; string lengths bounded; unknown enum values rejected |
| Injection | JPA parameterised queries only; no string-built SQL; no HTML rendering of user text |
| Data exposure | DTOs only; error responses carry no stack traces, SQL or class names; logs never contain passwords, tokens or the hash |
| Abuse and cost | Search result limits; request body size limits; bounded graph (AA-4); route request timeout; in-memory login throttle; TLS and rate limiting at the reverse proxy in hosted deployments |
| Transport | HTTPS in production. Plain HTTP is allowed only for local development; Android and iOS need explicit development-only exceptions for it, which are not enabled in release builds |
| CORS | Not enabled; the mobile client is not a browser. Swagger UI is same-origin |
| Documentation exposure | Swagger UI and OpenAPI are enabled in dev and disabled or protected in production |
| Privacy | No personal data, no location tracking, no route history (PRD NFR-07) |
| Supply chain | Dependabot (or equivalent) for Maven, Dart and GitHub Actions; run only supported framework versions (AD-1) |
| Containers | Multi-stage build; run as a non-root user; no secrets in images |

---

## 13. Error handling

### 13.1 Backend

One global exception handler converts every failure to a **single error body**: timestamp, HTTP status, stable error code, human-readable message, request path, field-level details (for validation), and a request ID.

| Situation | Status | Code |
|---|---|---|
| Validation failure | 400 | VALIDATION_FAILED (with field details) |
| Malformed JSON or unknown enum | 400 | MALFORMED_REQUEST |
| Missing or invalid token | 401 | UNAUTHORIZED |
| Authenticated but not permitted | 403 | FORBIDDEN |
| Unknown building, floor, node or obstacle | 404 | RESOURCE_NOT_FOUND |
| Start and destination in different buildings | 400 | NODES_NOT_IN_SAME_BUILDING |
| Concurrent obstacle edit | 409 | CONFLICT |
| Unexpected failure | 500 | INTERNAL_ERROR (generic message, details only in logs) |
| **No route available** | **200** | **Not an error: `status` = NO_ROUTE with a reason** |

A request-ID filter assigns an ID to each request, returns it in a response header and in the error body, and includes it in every log line for that request.

### 13.2 Engine

The engine throws a small, documented set of exceptions (invalid node, invalid configuration) and never returns partial paths. Absence of a route is a normal result, not an exception.

### 13.3 Mobile

Failures are mapped to typed `AppFailure` values in one place and rendered with the patterns and copy from DESIGN.md Section 11. Raw exceptions never reach widgets. Users never see codes or traces; developers see them in logs, and support can quote the request ID.

### 13.4 Logging

Plain-text logs to the console (the container runtime collects them). Levels: ERROR for unexpected failures, WARN for rejected input and auth failures, INFO for startup and obstacle changes, DEBUG for development only. Structured logging and a metrics stack are Future.

---

## 14. External services and AI architecture

**External services (MVP): none at runtime.** The app depends on no map provider, positioning service, analytics, push, payment or identity provider. Build-time and process tooling only: GitHub (source, Actions), Docker registry for images (optional, Should), Maven and pub.dev package registries.

**AI architecture: none needed.** The product has no AI or LLM component. Routing is deterministic graph search. "AI-assisted development" is a team process covered by AGENTS.md (read the docs, respect module ownership, run tests), not a runtime feature. If AI features are ever proposed (for example natural-language destination search), they require a new decision and design; nothing in the MVP architecture needs to change to prepare for them.

---

## 15. Environment configuration

### 15.1 Environments

| Environment | Purpose | Notes |
|---|---|---|
| Local development | Day-to-day work | Docker Compose runs database and backend; demo seed on; Swagger on; Flutter runs separately |
| Test / CI | Automated tests | Testcontainers PostgreSQL; seed on where tests need it; no external access |
| Demo / hosted | Presentation or hosting | Same images; HTTPS via reverse proxy; Swagger off; seed on for a demo, off for a clean installation |

### 15.2 Backend configuration (environment variables)

| Variable | Purpose | Secret |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | dev, test or prod behaviour | No |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Database connection | Password: yes |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD_HASH` | The single admin account | Hash: yes |
| `JWT_SECRET` | Token signing key (long, random) | Yes |
| `JWT_TTL_MINUTES` | Token lifetime (default 60) | No |
| `SEED_DEMO_DATA` | Enable the demo seed location | No |
| `SWAGGER_ENABLED` | Expose API docs | No |
| `SERVER_PORT` | Listening port | No |
| `ROUTING_*` | Optional overrides of surcharge factors | No |

Configuration is read into typed property classes at startup; the application **fails fast** with a clear message if a required value is missing (for example in prod).

### 15.3 Mobile configuration

The API base URL is supplied at build or run time (compile-time define), with no hard-coded production URL. Documented development values: Android emulator reaches the host machine through its special alias address; the iOS simulator uses localhost; a physical device uses the computer's LAN address. No flavors are used in the MVP.

### 15.4 Rules

- `.env` never committed; `.env.example` always up to date.
- No secrets in Dockerfiles, compose files, CI logs or the mobile app.
- Defaults in the repository are safe for local use only and are overridden in any hosted environment.

---

## 16. Testing approach

### 16.1 Levels

| Level | Scope | Tools | Runs in CI |
|---|---|---|---|
| Engine unit | Algorithms, constraints, metrics, in-memory graphs | JUnit 5 | Yes |
| Engine equivalence | A* equals Dijkstra cost on many generated graphs, including multi-floor | JUnit 5 | Yes |
| Backend unit | Services, mappers, instruction builder | JUnit 5, Mockito | Yes |
| Backend web slice | Controllers: status codes, validation, error bodies, auth rules | Spring test slices | Yes |
| Backend persistence | Repositories, migrations, constraints against real PostgreSQL | Testcontainers (AD-5) | Yes |
| Backend integration | Request to database to engine to response, including obstacle change then reroute | Spring Boot test with Testcontainers | Yes |
| Seed and scenario | Seed invariants and the five demonstration scenarios in PRD 8.3 | JUnit 5 | Yes |
| Mobile unit | Notifiers, repositories, failure mapping | `flutter_test`, `mocktail` | Yes |
| Mobile widget | Screens, states, preference selection, warnings | `flutter_test` | Yes |
| End to end | Full flow against running backend (PRD Section 12 flow) | `integration_test`, emulator | Manual or scheduled; documented in `docs/testing` |
| Contract | Generated OpenAPI matches the committed API.md and spec snapshot | CI step | Yes (Should) |
| Accessibility and design | Checklist from DESIGN.md Section 12.5 | Manual | No |

### 16.2 Rules

- Every owner tests their own code; a feature is not done without tests at its level.
- Tests do not depend on each other's order or on external services.
- Bug fixes begin with a failing test.
- Main must be green; failing CI blocks merging.

---

## 17. CI/CD and deployment

### 17.1 CI (GitHub Actions)

- **Backend workflow:** on pull requests and main, set up JDK 21 and Maven caching, build all modules, run all backend and engine tests (Docker is available for Testcontainers), export the OpenAPI document for the contract check.
- **Mobile workflow:** on pull requests and main, set up Flutter (current stable), run static analysis, formatting check and tests.
- Path filters run only the relevant workflow when only one domain changed; changes to `docs/` or the API contract trigger both.
- Branch protection on main: required checks, required review, no direct pushes.

### 17.2 Local run

One command starts the database and backend; Flyway migrates on startup; the demo seed loads in dev. The compose file defines two services: database (with a named volume and a health check) and backend (starts after the database is healthy). Only the backend port is published in non-development profiles.

### 17.3 Build artefacts

- Backend: multi-stage Docker build producing a small runtime image, non-root.
- Mobile: debug builds for emulators; release APK or bundle for Android demos built manually; iOS builds require macOS and Xcode and are documented, not automated, in the MVP.

### 17.4 Hosted deployment (optional for the MVP)

The default MVP deliverable is the **local Docker Compose environment** (AD-13). If the team hosts a demo, the same images run on a single small server or container platform behind a TLS-terminating reverse proxy with a managed or volume-backed PostgreSQL, with scheduled database backups. Migrations run on application startup, which is safe for a single instance.

---

## 18. MVP versus future scalability

| Area | MVP (build now) | Future (do not build now) |
|---|---|---|
| Deployment | Single backend instance, single database | Multiple stateless backend instances behind a load balancer |
| Graph loading | Load per request; obstacles read fresh | Cache building topology in memory, invalidated by data version; obstacles still applied per request |
| Search | Case-insensitive partial match with limit | Trigram or full-text search |
| Spatial | Plain 2-D coordinates and scale | PostGIS, automated floor-plan import |
| Data management | Migrations and seed scripts | Admin console for buildings, nodes, edges; import tools |
| Obstacles | Manual create and resolve | Scheduled start and end; sensor or system integrations |
| Updates | Pull on request and on app resume | WebSockets or push |
| Identity | One configured admin | Multiple accounts, roles, per-building permissions, refresh tokens |
| Positioning | Manual start; QR is a Could | Beacons or other positioning, live progress |
| Multi-building | One building per environment | Campus with inter-building links |
| API | Unversioned `/api`, no pagination | Versioned paths, pagination, rate limiting at the gateway |
| Observability | Request ID, console logs, health endpoint | Structured logs, metrics, tracing, alerting |
| Platform | Android and iOS app | Admin web console |
| Languages | English | Localisation (copy is already centralised) |

**Why the MVP design scales when needed:** the backend is stateless, the engine is a pure function over a graph, state lives only in PostgreSQL, and the API is already DTO-based and documented. Each Future item is an addition, not a rewrite.

---

## 19. Assumptions, decisions and risks

### Assumptions

| ID | Assumption |
|---|---|
| AA-1 | One backend instance serves the MVP |
| AA-2 | PostgreSQL uses the current stable major version at project start (confirm when setting up Compose) |
| AA-3 | All floors of a building share one aligned coordinate frame with a per-floor scale to metres |
| AA-4 | Graphs have at most about 2,000 nodes per building (PRD A-2) |
| AA-5 | Floor-plan SVGs are authored by the team to match each floor's plan width and height |
| AA-6 | Default routing parameters, to be tuned against the sample data: walking speed about 1.2 m/s (PRD A-6); AVOID_STAIRS multiplies stair cost by about 10; PREFER_ELEVATOR multiplies stair cost by about 2; fixed elevator wait-plus-ride time per transition set in the connection type |
| AA-7 | Flutter uses the current stable release; Dart and package versions are pinned in lock files |
| AA-8 | Every team member can run Docker locally |

### Decisions requiring approval

| ID | Decision | Options | Recommended default |
|---|---|---|---|
| **AD-1** | **Spring Boot version.** The Master Prompt requires 3.x, but that line is now out of open-source support | (a) Boot 4.1.x, still supported and compatible with Java 21; (b) Boot 3.5.x, unsupported and receiving no free security patches | **(a) 4.1.x.** This is a deviation from the mandated stack and needs your approval. Expect differences from 3.x tutorials (reorganised starters, newer Jackson); agents must follow the Boot 4.1 reference. Choose (b) only if the team accepts running unpatched software for a coursework project |
| **AD-2** | Admin authentication | Short-lived signed token from a login endpoint, held in memory; HTTP Basic on every call; static API key | Signed short-lived token, in memory (matches PRD D-3) |
| **AD-3** | Graph loading | Load per request; cache topology per building | Load per request. Simplest, no stale-data risk, fast enough at this size |
| **AD-4** | Floor-plan asset hosting | Backend serves SVGs from the repository; bundle plans inside the app | Backend-served, so new environments need no app release |
| **AD-5** | Database tests | Testcontainers PostgreSQL; in-memory H2 | Testcontainers. Same engine as production; slower but trustworthy |
| **AD-6** | Migrations and seed organisation | `database/` as source of truth packaged into the backend, seed loaded by configuration; migrations inside `backend/` | `database/` packaged into the backend; seed only when SEED_DEMO_DATA is on |
| **AD-7** | JSON models and tooling | Hand-written models, records, manual mappers; code generation, Lombok, MapStruct | Hand-written. Fewer tools and more readable for students |
| **AD-8** | Algorithm used by the backend | A* with Dijkstra for verification; Dijkstra only | A*, with Dijkstra retained behind the same interface |
| **AD-9** | API versioning | Unversioned `/api`; `/api/v1` from the start | Unversioned `/api` as defined in the Master Prompt |
| **AD-10** | Instruction text | Backend sends structured steps plus English text; backend sends structure only and the app writes text | Both: structure plus English text, so the API is complete and the app can localise later |
| **AD-11** | Obstacle target in the schema | Two nullable foreign keys with an exactly-one check; polymorphic type and ID columns | Two nullable foreign keys with a check constraint, preserving referential integrity |
| **AD-12** | No-route response | HTTP 200 with status NO_ROUTE; an error status | HTTP 200 with status NO_ROUTE, per PRD |
| **AD-13** | Hosting for the MVP | Local Docker Compose only; hosted demo | Local Compose as the required deliverable; hosting optional |

### Risks

| Risk | Impact | Mitigation |
|---|---|---|
| Boot 4.x differs from many tutorials and AI training data | Wrong dependency names or configuration | AGENTS.md instructs agents to check the Boot 4.1 reference; first backend task proves the skeleton builds and starts |
| Library compatibility with the chosen Boot version (OpenAPI generator, test tooling) | Setup delays | Verify each dependency's compatible release in the first backend task; keep the dependency list short |
| Heuristic invalidated by bad data (edge shorter than straight line) | Wrong A* results | Seed-load invariant test; equivalence test against Dijkstra |
| Coordinate misalignment between SVG and node data | Route drawn in the wrong place | One documented coordinate convention; visual check screen; plan size validated against floor metadata |
| Graph load cost grows with building size | Slow routes | Measure against NFR-01; caching is a documented Future step |
| Contract drift between backend and app | Integration failures | API.md and generated OpenAPI frozen before integration; contract check in CI |
| Docker or Testcontainers unavailable on a team machine or CI runner | Tests cannot run | Document requirements; CI runners provide Docker |
