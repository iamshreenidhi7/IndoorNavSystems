# Product Requirements Document: Indoor Navigation App

| | |
|---|---|
| **Status** | Draft v0.1 |
| **Team size** | 4 |
| **Platforms** | Android, iOS |
| **Stack** | Flutter, Java 21 + Spring Boot, standalone Java pathfinding engine, PostgreSQL |

> Items marked **(Assumption)** were not specified in the original project brief. Confirm or change them before development starts.

---

## 1. Overview

### 1.1 Summary
A cross-platform mobile app that helps people find their way inside buildings. Users search for a room, see it on an indoor map, and get a route from their start point to the destination, including across floors. The route accounts for blocked paths (obstacles) and accessibility needs.

### 1.2 Problem statement
GPS does not work reliably indoors, and large buildings such as campuses, hospitals and malls are hard to navigate. Static signage and printed maps are hard to follow and go out of date when corridors are closed or rooms are reassigned.

### 1.3 Product vision
One app, one codebase for Android and iOS, backed by a routing engine that always returns the shortest *usable* path through a building.

---

## 2. Goals and Non-Goals

### 2.1 Goals
1. Let users find a room or place by search in under a few seconds.
2. Display an indoor map per floor with clear floor switching.
3. Compute and display the shortest route between two points, including multi-floor routes.
4. Respect obstacles (closed corridors, blocked doors) and accessibility constraints when routing.
5. Ship one Flutter app that runs on both Android and iOS against a single backend API.

### 2.2 Non-goals (for MVP)
- Real-time indoor positioning (BLE beacons, Wi-Fi fingerprinting, UWB). **(Assumption)** The user's start point is selected manually in the MVP.
- Turn-by-turn voice guidance.
- Outdoor navigation or GPS-based routing.
- Multi-building campus routing (outdoor links between buildings).
- Web or desktop clients.

---

## 3. Target Users

| Persona | Needs |
|---|---|
| **Visitor / new user** | Find a room in an unfamiliar building quickly, with simple directions. |
| **Regular occupant** (student, staff) | Fast search and repeat routes to frequently visited places. |
| **User with accessibility needs** | Routes that avoid stairs and use lifts or ramps only. |
| **Administrator** | Add or update buildings, floors, rooms, and mark or clear obstacles. |

---

## 4. User Stories

**Search and map**
- As a user, I can search for a room by name or number so I can find it on the map.
- As a user, I can switch between floors to view each floor's layout.

**Routing**
- As a user, I can choose a start and a destination and see the route drawn on the map.
- As a user, I get a route across floors when the destination is on a different floor.
- As a user, I can request an accessible route that avoids stairs.
- As a user, I get a different route automatically if a corridor on the default route is blocked.

**Administration**
- As an admin, I can mark a path or area as blocked and clear it later.
- As an admin, I can load or update building layout data. **(Assumption)** via database seed/migrations in the MVP, with an admin UI after MVP.

---

## 5. Functional Requirements

### 5.1 Mobile app (Flutter)

| ID | Requirement | Priority |
|---|---|---|
| M-1 | Display an indoor map for the selected building and floor. | Must |
| M-2 | Floor switcher control to change the visible floor. | Must |
| M-3 | Room search with suggestions as the user types. | Must |
| M-4 | Select start and destination points (from search or by tapping the map). | Must |
| M-5 | Request a route from the backend and draw it on the map. | Must |
| M-6 | Show step-by-step route instructions (text list). | Should |
| M-7 | Navigation view that follows the route and shows the next step. | Should |
| M-8 | Toggle for accessible routing (avoid stairs). | Must |
| M-9 | Display obstacle markers on the map. | Should |
| M-10 | Handle loading, empty, offline and error states clearly. | Must |
| M-11 | Cache the last viewed building data locally. | Could |
| M-12 | Save recent searches and favourite places locally. | Could |

### 5.2 Backend API (Java 21 + Spring Boot)

| ID | Requirement | Priority |
|---|---|---|
| B-1 | REST endpoints to list buildings, floors and rooms. | Must |
| B-2 | Room search endpoint. | Must |
| B-3 | Route endpoint that accepts start, destination and options, and returns a path. | Must |
| B-4 | Obstacle management endpoints (create, list, clear). | Must |
| B-5 | Request validation and consistent error responses. | Must |
| B-6 | Authentication for admin endpoints. **(Assumption)** Admin-only; user-facing endpoints are public. | Should |
| B-7 | Load graph data from PostgreSQL and supply it to the routing engine. | Must |
| B-8 | API documentation (`docs/API.md`, optionally OpenAPI). | Must |

### 5.3 Pathfinding engine (Java)

| ID | Requirement | Priority |
|---|---|---|
| P-1 | Represent each building as a weighted graph (nodes and edges). | Must |
| P-2 | Dijkstra's algorithm for shortest path. | Must |
| P-3 | A\* algorithm with a suitable heuristic for faster routing. | Should |
| P-4 | Multi-floor routing through connectors (stairs, lifts, ramps). | Must |
| P-5 | Exclude blocked edges and nodes when obstacles are active. | Must |
| P-6 | Accessibility constraints: exclude stairs when accessible routing is on. | Must |
| P-7 | Return the ordered list of nodes plus total distance and estimated time. | Must |
| P-8 | Return a clear "no route found" result when the destination is unreachable. | Must |
| P-9 | Run as a standalone module with its own unit tests, independent of Spring. | Must |

### 5.4 Database and DevOps

| ID | Requirement | Priority |
|---|---|---|
| D-1 | PostgreSQL schema for buildings, floors, rooms, graph nodes, edges and obstacles. | Must |
| D-2 | Versioned migrations (e.g. Flyway or Liquibase). **(Assumption)** | Must |
| D-3 | Sample dataset for at least one building with multiple floors. | Must |
| D-4 | `docker-compose.yml` to start backend and database with one command. | Must |
| D-5 | GitHub Actions pipeline running build and tests on every pull request. | Must |
| D-6 | Deployment setup for the backend and database. | Should |

---

## 6. Non-Functional Requirements

| Area | Requirement |
|---|---|
| **Performance** | Route calculation for a single building should return in under 1 second at typical building size. **(Assumption)** Map screens should render smoothly during pan and zoom. |
| **Reliability** | Backend returns structured errors; the app never crashes on a failed request. |
| **Compatibility** | Supports current and recent Android and iOS versions. **(Assumption)** Exact minimum versions to be set by the team. |
| **Security** | HTTPS for all traffic; validated inputs; secured admin endpoints; no secrets committed to the repository. |
| **Maintainability** | Modular repository, shared architecture doc, code reviewed through pull requests. |
| **Testability** | Unit tests for the pathfinding engine and backend services; Flutter widget and integration tests for the main flows. |
| **Accessibility (UI)** | Readable text sizes, sufficient contrast, labels for screen readers. |

---

## 7. System Architecture

```
Flutter mobile app (Android + iOS, one codebase)
        │
        │  HTTPS · JSON REST API
        ▼
Java Spring Boot backend
  API · Services · Validation
        │
        ├──▶ Java pathfinding engine (Graph + Dijkstra + A*)
        │
        ▼
PostgreSQL
  Buildings · Floors · Rooms · Graph · Obstacles
```

The backend does not need to know whether a request came from Android or iOS. Both apps call the same API, and the pathfinding engine is a separate module that can be tested on its own.

### 7.1 Technology choices

| Layer | Choice |
|---|---|
| Mobile | Flutter + Dart, Material 3 |
| HTTP client | Dio |
| State management | Riverpod |
| Navigation | GoRouter |
| Map rendering | `CustomPainter` or `flutter_svg` |
| Local storage | SharedPreferences (SQLite if needed) |
| Data models | Freezed / JSON serialization (optional) |
| Backend | Java 21, Spring Boot |
| Routing engine | Java (plain module) |
| Database | PostgreSQL |
| Tooling | GitHub, Docker, GitHub Actions |
| Testing | Flutter test, `integration_test`, JUnit **(Assumption)** |

### 7.2 Repository structure

```
indoor-navigation/
├── mobile-app/             # Flutter
├── backend/                # Spring Boot
├── algorithm-engine/       # Java pathfinding
├── database/               # Schema + migrations
├── docs/
│   ├── ARCHITECTURE.md
│   ├── API.md
│   └── DATABASE.md
├── PRD.md
└── docker-compose.yml
```

---

## 8. Data Model (Draft)

| Entity | Key fields |
|---|---|
| **Building** | id, name, address/description |
| **Floor** | id, building_id, level number, label, map asset reference |
| **Room** | id, floor_id, name, number, category, linked node_id, map position |
| **Node** | id, floor_id, x, y, type (corridor, door, stairs, lift, ramp, room entrance) |
| **Edge** | id, from_node_id, to_node_id, weight (distance), is_accessible, is_bidirectional |
| **Obstacle** | id, node_id or edge_id, reason, active flag, created/cleared timestamps |

Floor connectors (stairs, lifts, ramps) are nodes of the matching type, linked by edges between floors. Accessible routing excludes edges where `is_accessible = false`.

---

## 9. API Contract (Draft)

The team should agree on this before coding so the mobile app can use mock responses while the backend is built. Final version lives in `docs/API.md`.

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/buildings` | List buildings |
| GET | `/api/buildings/{id}/floors` | List floors and map data |
| GET | `/api/buildings/{id}/rooms?query=` | Search rooms |
| POST | `/api/routes` | Calculate a route |
| GET | `/api/obstacles?buildingId=` | List active obstacles |
| POST | `/api/obstacles` | Create an obstacle (admin) |
| DELETE | `/api/obstacles/{id}` | Clear an obstacle (admin) |

**Route request (example)**
```json
{
  "buildingId": 1,
  "fromRoomId": 101,
  "toRoomId": 342,
  "accessibleOnly": false
}
```

**Route response (example)**
```json
{
  "found": true,
  "distanceMeters": 128.5,
  "estimatedSeconds": 110,
  "path": [
    { "nodeId": 10, "floor": 1, "x": 12.0, "y": 4.5, "type": "room_entrance" },
    { "nodeId": 24, "floor": 1, "x": 30.0, "y": 4.5, "type": "lift" },
    { "nodeId": 124, "floor": 3, "x": 30.0, "y": 4.5, "type": "lift" }
  ],
  "steps": [
    "Walk along the corridor to the lift",
    "Take the lift to floor 3"
  ]
}
```

---

## 10. Team and Ownership

| Role | Owner | Responsibilities |
|---|---|---|
| Cross-platform mobile frontend | Person 1 | Flutter screens, map rendering, floor switching, search, route display, navigation UI, Android/iOS testing |
| Java backend | Person 2 | Controllers, services, validation, API responses, authentication, database and engine integration |
| Pathfinding engine | Person 3 | Graph structures, Dijkstra, A\*, multi-floor routing, accessibility constraints, route optimization |
| Database, DevOps and testing | Person 4 | Schema, sample data, migrations, Docker setup, CI/CD, integration testing |

### 10.1 Working agreements
- Agree on the route API format first; Person 1 builds against mock responses and Person 2 against a mock routing engine until real ones are ready.
- Every AI coding agent receives the same `docs/ARCHITECTURE.md` and works only inside its assigned module unless a change is agreed.
- Use feature branches and pull requests; no direct pushes to `main`.

---

## 11. Milestones **(Assumption: durations to be set by the team)**

| Phase | Deliverables |
|---|---|
| **0. Foundations** | Repository, `ARCHITECTURE.md`, `API.md`, `DATABASE.md`, agreed route API, Docker Compose, CI skeleton |
| **1. Parallel build** | Flutter app against mock API; backend with mock engine; engine with Dijkstra and unit tests; schema and sample data |
| **2. Integration** | Real engine wired into backend; app connected to real API; end-to-end route on one floor |
| **3. Multi-floor and constraints** | Multi-floor routing, accessible routing, obstacle management, obstacle display in app |
| **4. Polish and release** | A\* optimization, error and offline states, test coverage, deployment, release builds |

---

## 12. Success Metrics **(Assumption)**

- A user can find a room and view a route in under 30 seconds from opening the app.
- Routes are correct for 100% of test cases in the engine test suite (single-floor, multi-floor, blocked, accessible).
- Route API responds in under 1 second for the sample buildings.
- The same app build flow works on both Android and iOS with no platform-specific frontend code for core features.
- CI passes on every merged pull request.

---

## 13. Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Teammates' modules don't integrate | Delays | Agree API contract first; use mocks; shared architecture doc; regular integration testing |
| Building map data is hard to create | Blocks testing | Start with one small sample building; define a simple graph format early |
| No real indoor positioning | Users must pick their start point | Document as an MVP limitation; consider beacons or QR-code anchors later |
| iOS builds require macOS and Xcode | Cannot build or sign iOS app on Windows/Linux | Identify a macOS machine or CI runner early; plan Apple developer account and signing |
| Map rendering complexity in Flutter | Frontend slowdown | Start with a simple SVG or `CustomPainter` floor plan; iterate |
| AI-generated code drifts across modules | Inconsistent code | Module boundaries, PR reviews, shared conventions |

---

## 14. Open Questions

1. Which building(s) will be mapped first, and who creates the floor plans and graph data?
2. Is user location needed (manual start, QR codes, beacons), or is manual selection enough?
3. Is authentication required for regular users, or only admins?
4. Is an admin interface needed, or is database seeding enough for the first release?
5. How will the app be distributed (store release, internal testing, demo only)?
6. What are the minimum supported Android and iOS versions?

---

## 15. Out of Scope for Future Consideration

- Real-time positioning with beacons, Wi-Fi or UWB
- Voice-guided navigation
- Multi-building and outdoor-to-indoor routing
- Web-based admin dashboard
- Crowd-aware or time-aware routing
- Multi-language support
