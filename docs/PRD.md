# PRD: Indoor Navigation and Path Optimization System

| Field | Value |
|---|---|
| Status | Draft v0.1, awaiting owner approval on flagged decisions |
| Audience | Four-person student team and AI coding agents |
| Source of truth for | Scope, behaviour, data and acceptance criteria |
| Companion docs | ARCHITECTURE.md, DESIGN.md, DATABASE.md, API.md, AGENTS.md (to be created after this PRD is approved) |

## 0. How to read this document

- **Requirement IDs** (FR-, NFR-, US-) are stable. Reference them in commits, PRs and tests.
- **Priority** uses MoSCoW: **Must** (MVP cannot ship without it), **Should** (MVP if time allows), **Could** (post-MVP).
- **[ASSUMPTION A-n]** marks something assumed because it was not specified. Correct any that are wrong.
- **[DECISION D-n]** marks a choice that needs the owner's approval before implementation. All are collected in Section 18, each with a recommended default. Until approved, agents must use the recommended default and must not silently choose otherwise.
- This PRD defines *what* and *why*. Technology choices are fixed by the Master Project Prompt and are not repeated here beyond what affects requirements. This document contains no code.
- The Master Project Prompt was truncated in the final integration-test step (step 10). [ASSUMPTION A-1] The intended step 10 is: "Confirm that the application selects an alternative route, or reports that no route is available." Please confirm or supply the missing text.

---

## 1. Product overview

### 1.1 Problem

People inside large indoor spaces (malls, hospitals, campuses, airports, offices, convention centres) cannot rely on GPS, and static signage or floor-plan boards do not adapt to the individual or to changing conditions. Visitors lose time finding a destination, struggle to move between floors, and people with mobility needs cannot easily tell which routes are usable for them. When a corridor, lift or staircase is closed, signage rarely reflects it.

### 1.2 Value proposition

A reusable indoor-navigation platform that lets a person pick a place, pick a destination, and get an optimized, preference-aware route across floors, with that route updated whenever the space's operator marks part of it unavailable. The same platform serves any indoor environment through data and configuration, without changes to core logic.

### 1.3 Product principle: platform first

The **generic platform** is the product. The **shopping mall** is only the first sample dataset and demo scenario.

- Core entities, APIs, screens and the routing engine must use neutral concepts: building, floor, space/room, point of interest (POI), navigation node, connection, obstacle.
- Mall-specific names, categories and sample content live only in seed data and configuration, never in core code, schema or API contracts.
- Documentation describes platform capabilities first, then illustrates them with the mall example, and labels clearly what is platform versus mall-only.

### 1.4 Target users

| User | Description | Primary need |
|---|---|---|
| **Visitor** | Anyone moving through a covered environment (shopper, patient visitor, student, traveller) | Find a destination quickly and reliably |
| **Visitor with mobility needs** | Wheelchair users, people with strollers or limited stamina | Routes that exclude stairs and other unsuitable connections |
| **Administrator (operator)** | Facility staff responsible for a building | Mark connections or areas closed or reopened so routes stay truthful |
| **Developer / integrator** (secondary) | A team onboarding a new environment | Add a new environment via data, not code changes |

### 1.5 Goals and non-goals

**Goals:** correct and fast routing; multi-floor support; accessibility-aware routing; dynamic obstacle handling with rerouting; clean mobile UX; a working demo; configurable for new environments.

**Non-goals:** live positioning, real-time sensing, social or commercial features (see Section 17).

---

## 2. Core user journeys

**J1. Navigate to a destination (primary).**
Open app → select building → search for a destination → choose or confirm start location → choose route preference (default SHORTEST) → view route on map with distance and estimated time → follow step-by-step instructions, switching floors as indicated.

**J2. Accessible navigation.**
As J1, but the visitor selects ACCESSIBLE (or enables the accessibility setting, which pre-selects it). The route excludes inaccessible connections. If none exists, the app states this clearly.

**J3. Encounter a blocked route.**
A visitor requests a route. The administrator has blocked a connection on the usual path. The app returns an alternative route with a visible warning explaining the detour, or a clear "no route available" message if the destination is unreachable.

**J4. Administrator manages an obstacle.**
Administrator signs in → opens obstacle management → creates an obstacle on a connection or node (type, description) → obstacle becomes active → later resolves or deletes it. Subsequent route requests reflect the change.

**J5. Onboard a new environment (developer).**
Add a building, floors, nodes, connections, POIs and floor-plan assets as data. The app lists and navigates it with no changes to core logic.

---

## 3. User stories

| ID | Story | Priority |
|---|---|---|
| US-01 | As a visitor, I can see the list of available buildings and choose one, so I navigate in the right place. | Must |
| US-02 | As a visitor, I can view each floor's map, so I understand the layout. | Must |
| US-03 | As a visitor, I can search destinations by name (and browse by category), so I find where I need to go. | Must |
| US-04 | As a visitor, I can set my start location manually (list or map tap), so I get a route from where I am. | Must |
| US-05 | As a visitor, I can request a route and see it drawn on the map with distance and estimated time. | Must |
| US-06 | As a visitor, I can choose a route preference (SHORTEST, FASTEST, ACCESSIBLE, AVOID_STAIRS, PREFER_ELEVATOR). | Must |
| US-07 | As a visitor, I can navigate between floors, with the map showing the route segment on each floor and a clear transition instruction. | Must |
| US-08 | As a visitor, I see readable step-by-step instructions, so I can follow the route without watching the map constantly. | Must |
| US-09 | As a visitor, I am warned when my route is affected by an obstacle, and I receive an alternative when one exists. | Must |
| US-10 | As a visitor, I get a clear message when no route exists, with the reason when known. | Must |
| US-11 | As a visitor, I can save a default route preference or accessibility setting that persists on my device. | Should |
| US-12 | As a visitor, I can see blocked connections on the map. | Should |
| US-13 | As an administrator, I can sign in to a protected obstacle-management area. | Must |
| US-14 | As an administrator, I can create, edit, resolve and delete obstacles. | Must |
| US-15 | As an administrator, I can see which obstacles are currently active per building. | Must |
| US-16 | As a visitor, I can set my start by scanning a QR code at a known location. | Could |
| US-17 | As a developer, I can add a new environment through seed data and configuration only. | Must |

### Key acceptance criteria

- **US-05:** Given a valid start and destination in the same connected graph, the response includes the ordered path, distance (metres), estimated duration (seconds) and selected preference; the app draws the path from node coordinates.
- **US-07:** For a route spanning floors, the response identifies each floor transition (from floor, to floor, connection type); the app shows the correct floor plan for each segment and lets the user move between segments.
- **US-09:** After an administrator activates an obstacle on an edge used by the previously returned route, a new request for the same start, destination and preference returns a path that does not use that edge, and includes a warning.
- **US-10:** If the destination is unreachable under the selected preference and active obstacles, the API returns a structured "no route" result (not a server error) and the app shows a dedicated message.
- **US-14:** Obstacle changes persist across backend restarts.

---

## 4. Features and functional requirements

### 4.1 Environment, floors and maps

| ID | Requirement | Priority |
|---|---|---|
| FR-01 | The system lists all configured buildings. | Must |
| FR-02 | A building exposes its ordered floors (floor number or level, display name). | Must |
| FR-03 | Each floor has a map the app can render: a floor-plan background plus nodes, connections, POIs and route overlay in one consistent coordinate system. | Must |
| FR-04 | Maps support zoom and pan. | Must |
| FR-05 | The map can show start, destination, route, entrances, stairs and elevators, and blocked connections. | Must |
| FR-06 | Navigation nodes can be toggled visible for debugging or demonstration. | Could |

### 4.2 Destination search and selection

| ID | Requirement | Priority |
|---|---|---|
| FR-07 | Users can search rooms and POIs within the selected building by name, case-insensitive, partial match. | Must |
| FR-08 | Users can browse destinations by category (category labels come from data, not code). | Should |
| FR-09 | Each searchable destination is linked to exactly one navigation node used for routing. | Must |
| FR-10 | Search results show name, floor and category. | Must |

### 4.3 Start location

| ID | Requirement | Priority |
|---|---|---|
| FR-11 | Users choose a start location manually, from a searchable list or by tapping a node on the map. | Must |
| FR-12 | The system does not claim automatic positioning; the UI labels the start as user-selected. | Must |
| FR-13 | QR-code start selection at known locations. | Could |

### 4.4 Routing

| ID | Requirement | Priority |
|---|---|---|
| FR-14 | The backend computes routes using the Java pathfinding engine over a weighted graph; the mobile app never computes routes. | Must |
| FR-15 | Route request inputs: start node, destination node, preference. Preference defaults to SHORTEST. | Must |
| FR-16 | Route response contains: ordered path nodes, distance, estimated duration, selected preference, warnings, floor transitions, and instructions. | Must |
| FR-17 | The engine implements Dijkstra and A* with an admissible heuristic, and both return the same cost for the same query. | Must |
| FR-18 | Start equal to destination returns a valid zero-distance route. | Must |
| FR-19 | Invalid or unknown start/destination returns a validation or not-found error, not a route. | Must |
| FR-20 | Blocked nodes and edges are excluded from routing. | Must |
| FR-21 | Estimated time is labelled as an estimate, not live tracking. | Must |

### 4.5 Route preferences (semantics)

| Preference | Behaviour |
|---|---|
| **SHORTEST** | Minimises total distance. |
| **FASTEST** | Minimises estimated travel time, using per-connection-type speeds and fixed transition times. Can produce a different route than SHORTEST. |
| **ACCESSIBLE** | **Hard constraint.** Connections or nodes not flagged accessible (for example stairs) are excluded. Among remaining routes, minimises time. If none remain, return "no route". |
| **AVOID_STAIRS** | Stairs are heavily penalised, not forbidden: used only when no stair-free route exists, and the response then carries an explicit warning. **[DECISION D-4]** |
| **PREFER_ELEVATOR** | Soft preference: elevators receive a cost advantage over stairs for vertical movement, while stairs remain allowed. |

Requirement FR-22 (Must): the exact penalty and advantage values are documented, configurable and covered by tests, and they must keep all edge costs positive.

### 4.6 Multi-floor navigation

| ID | Requirement | Priority |
|---|---|---|
| FR-23 | Floors are connected in one unified graph through vertical connections (stairs, elevators, ramps). | Must |
| FR-24 | Vertical connections have an explicit cost, as distance cannot be derived from 2-D coordinates alone. | Must |
| FR-25 | The route response lists each transition with from-floor, to-floor and connection type. | Must |
| FR-26 | The heuristic must never overestimate across floors. | Must |

### 4.7 Instructions

| ID | Requirement | Priority |
|---|---|---|
| FR-27 | The response provides ordered instructions covering start, distance per segment, floor changes (for example "Take the elevator to Floor 2") and arrival. | Must |
| FR-28 | Instructions include left/right turn wording derived from geometry. | Should |

### 4.8 Obstacles and rerouting

| ID | Requirement | Priority |
|---|---|---|
| FR-29 | An obstacle references exactly one affected edge or node, has a type, a description and a status (active or resolved). | Must |
| FR-30 | Supported types: blocked corridor, closed room, broken elevator, closed staircase, construction area, restricted connection. | Must |
| FR-31 | Administrators can create, update, resolve and delete obstacles; changes persist. | Must |
| FR-32 | The next route calculation after a change uses the updated state, with no caching that serves stale graphs. | Must |
| FR-33 | If the previously optimal path is affected, the app shows an obstacle warning and the alternative route. | Must |
| FR-34 | If no route exists, the response states so structurally and the app explains it. | Must |
| FR-35 | Obstacles take effect and clear only by manual administrator action in the MVP (no schedules, no sensors). | Must |

### 4.9 Preferences and accessibility settings (device-level)

| ID | Requirement | Priority |
|---|---|---|
| FR-36 | The app stores last-used building, default route preference and an "accessible routes" setting locally on the device. | Should |
| FR-37 | Enabling the accessibility setting pre-selects ACCESSIBLE on route requests. | Should |

### 4.10 Administration

| ID | Requirement | Priority |
|---|---|---|
| FR-38 | Obstacle management is available only to authenticated administrators. | Must |
| FR-39 | Building, floor, node and connection data is managed through database migrations and seed data in the MVP, with no in-app editor. | Must |

---

## 5. Non-functional requirements

| ID | Category | Requirement |
|---|---|---|
| NFR-01 | Performance | Route calculation completes in under 1 second at the 95th percentile for graphs up to about 2,000 nodes. [ASSUMPTION A-2] |
| NFR-02 | Performance | Non-routing API calls respond in under 500 ms at p95 on the local demo setup. |
| NFR-03 | Performance | Map rendering stays responsive during zoom and pan on a mid-range device. |
| NFR-04 | Correctness | Dijkstra and A* agree on cost for every tested query; results are deterministic for identical input. |
| NFR-05 | Reliability | Obstacle state survives restarts; failed requests never leave partial data. |
| NFR-06 | Security | Administrative endpoints require authentication; no secrets in Git; inputs validated; entities never exposed directly. |
| NFR-07 | Privacy | The MVP collects no personal data and does not track visitor location. Route history is not stored. |
| NFR-08 | Usability | Primary journey (J1) completable in under 6 taps from app launch once a building is selected. |
| NFR-09 | Accessibility (UI) | Text contrast meets WCAG AA, text scales with system font size, interactive elements have semantic labels and minimum touch targets. |
| NFR-10 | Compatibility | One Flutter codebase for Android and iOS; works on phone screen sizes. Minimum OS versions are set in ARCHITECTURE.md. |
| NFR-11 | Configurability | Backend base URL is configurable per environment; adding an environment requires only data. |
| NFR-12 | Maintainability | Layered backend; engine isolated from HTTP, database and UI; readable code suitable for students; documented API. |
| NFR-13 | Testability | Engine testable with in-memory graphs; unit, backend, widget and end-to-end tests exist per Section 12 of this PRD's success criteria. |
| NFR-14 | Operability | One command starts backend and database locally; health endpoint available; CI runs tests on pull requests. |
| NFR-15 | Localization | English only in the MVP. Strings should be kept centralised to ease future localisation. |
| NFR-16 | Connectivity | An active connection to the backend is required; offline use is out of scope. |

---

## 6. Pages and screens

| # | Screen | Purpose | Key elements |
|---|---|---|---|
| 1 | Splash | Startup, config check, restore saved settings | Logo, loading indicator |
| 2 | Home | Entry point | Continue with last building, choose building, accessibility toggle, access to admin |
| 3 | Building selection | Choose environment | List of buildings with name and short description |
| 4 | Floor selection | Switch floors | Floor switcher (also embedded in map screen) |
| 5 | Indoor map | Core canvas | Floor plan, zoom/pan, POIs, start and destination markers, route overlay, blocked connections, floor switcher |
| 6 | Destination search | Find destination | Search field, category filter, results list |
| 7 | Route preferences | Choose routing mode | Preference options with plain-language descriptions |
| 8 | Route details | Summary before or during navigation | Distance, estimated time, preference, floor transitions, warnings |
| 9 | Navigation instructions | Step-by-step guidance | Ordered steps, current floor segment, next-segment control |
| 10 | Obstacle warning / rerouting | Communicate disruption | Warning banner or dialog, alternative route, "no route" state |
| 11 | Admin sign-in | Authenticate administrator | Credentials form |
| 12 | Admin obstacle management | Manage obstacles | List of obstacles by status, create/edit form, resolve and delete actions |

Screens 11 and 12 may be a protected area of the same app **[DECISION D-3]**.

Screens 1 to 10 map to the ten required screens in the Master Project Prompt. Screens 11 and 12 implement its administrative interface.

---

## 7. Roles and permissions

| Capability | Visitor (no sign-in) | Administrator |
|---|---|---|
| View buildings, floors, maps | Yes | Yes |
| Search destinations | Yes | Yes |
| Request routes | Yes | Yes |
| View active obstacles affecting routes | Yes (as warnings) | Yes (full list) |
| Create, edit, resolve, delete obstacles | No | Yes |
| Edit buildings, nodes, connections | No (data managed via migrations) | No in MVP |
| Manage administrator accounts | No | No in MVP (single account configured outside code) |

- **[ASSUMPTION A-3]** Visitors do not register or sign in. There are no user accounts in the MVP.
- **[ASSUMPTION A-4]** One administrator role exists, with credentials supplied through environment configuration, never committed to Git.
- **[DECISION D-3]** Authentication mechanism and where the admin UI lives (see Section 18).

---

## 8. Data requirements

The data model is conceptual here; DATABASE.md defines the schema. The relational structure is required: the navigation graph is **not** stored as an opaque document.

### 8.1 Entities

| Entity | Purpose | Key attributes | Rules |
|---|---|---|---|
| **Building** | An indoor environment | Name, description, type label, address (optional), coordinate scale | A building has one or more floors. |
| **Floor** | One level of a building | Building, level number, display name, floor-plan asset reference, metres-per-unit scale | Belongs to exactly one building. Level numbers unique within a building. |
| **Room / Space** | A named area on a floor | Floor, name, category, description (optional) | Belongs to a floor. |
| **Point of Interest** | A searchable destination | Name, category, floor, linked node, optional room | Linked to exactly one node. Rooms and POIs may be modelled as one concept if simpler **[DECISION D-6]**. |
| **Navigation node** | A navigable point | Floor, X and Y, node type, display name (optional), accessible flag | Belongs to one floor. Types include corridor junction, room entrance, stair landing, elevator entrance, building entrance, transition, POI. |
| **Connection (edge)** | A link between two nodes | From node, to node, connection type, distance, traversal time or cost, accessible flag, bidirectional flag | Both nodes must exist. Cost positive. Vertical connections link nodes on different floors with an explicit cost. |
| **Obstacle** | A restriction | Target (one edge or one node), type, description, status, created and updated time | Must reference a valid target. Active obstacles exclude the target from routing. |

Connection types for the MVP: corridor, stairs, elevator, ramp, doorway. The list must be extendable through data.

### 8.2 Data rules

- IDs are stable across API requests and restarts.
- Schema evolves only through migrations; sample data is loaded through seed scripts.
- Foreign keys, uniqueness and positivity constraints are enforced in the database as well as in application code.
- Coordinates are in a per-floor 2-D system; a per-floor scale converts units to metres.
- [ASSUMPTION A-5] Connections are bidirectional unless flagged otherwise.
- [ASSUMPTION A-6] Walking speed defaults to about 1.2 m/s, with slower stair speed, a fixed elevator wait-plus-ride time and a ramp speed close to walking speed. Values are configurable, documented, and used only for estimates.
- [ASSUMPTION A-7] Distance is derived from coordinates and scale for horizontal edges, and set explicitly for vertical edges.

### 8.3 Sample dataset requirements (mall)

The seed data must be realistic and represent **one building named as a shopping mall, with at least two floors** (three recommended).

- Multiple shops and facilities (for example food court, restrooms, information desk), several entrances, corridors with loops, at least one staircase and at least one elevator, optionally a ramp.
- At least two alternative routes between important pairs of destinations.
- Demonstration scenarios the data must support:
  1. **Blocked edge:** blocking a specific corridor changes the route to a longer alternative.
  2. **Shortest versus fastest differ:** at least one pair where SHORTEST and FASTEST give different paths (for example short stairs versus longer elevator trip).
  3. **Accessible versus non-accessible:** at least one pair where ACCESSIBLE avoids stairs and differs from SHORTEST.
  4. **Broken elevator:** blocking the only elevator makes ACCESSIBLE routing across floors return "no route" while SHORTEST still succeeds via stairs.
  5. **Unreachable destination:** a destination can be isolated through obstacles to demonstrate the no-route state.
- All mall-specific content lives in seed data only.

### 8.4 Data not stored in the MVP

User accounts, visitor identities, location history, route history, analytics. These are out of scope unless a later decision adds them.

---

## 9. Integrations

| Integration | Direction | MVP status |
|---|---|---|
| Flutter app ↔ Backend | REST over HTTPS, JSON | Required |
| Backend ↔ Pathfinding engine | In-process Java library dependency | Required |
| Backend ↔ PostgreSQL | Database access with migrations | Required |
| API documentation | OpenAPI/Swagger published by backend | Required |
| CI | GitHub Actions on pull requests | Required |
| Local environment | Docker Compose for backend and database | Required |
| API testing | Postman collection or equivalent | Should |
| QR scanning | Device camera on mobile | Could (post-MVP) |
| Third-party map, positioning, analytics, push or payment providers | None | Not used |

No external third-party service is required for the MVP.

---

## 10. Loading, empty and error states

General rules: every screen that loads data shows a loading indicator; no failure produces a blank screen; error messages are plain-language and say what the user can do next; technical details are logged, not shown.

| Screen / situation | Loading | Empty | Error |
|---|---|---|---|
| Splash | Progress indicator while checking configuration | n/a | "Can't reach the server" with Retry |
| Building selection | Skeleton list or spinner | "No buildings are available yet." | Load failed, with Retry |
| Floor selection / map | Spinner while map data loads | "No map available for this floor." | Map failed to load, with Retry; fallback to floor list |
| Destination search | Inline spinner during search | "No destinations match '…'." Suggest checking spelling or browsing categories. | Search failed, with Retry |
| Route request | Blocking progress state with cancel | n/a | Network or server error with Retry; invalid selection prompts the user to choose again |
| No route available | n/a | Dedicated state: "No route available with your current settings." States the reason when known (for example "Elevator out of service"), and offers to change preference or destination. | n/a |
| Route with warning | n/a | n/a | Non-blocking warning banner naming the affected area and the detour (see Section 4.8) |
| Admin sign-in | Button progress | n/a | Invalid credentials; account unavailable |
| Obstacle list | Spinner | "No active obstacles." | Load failed, with Retry |
| Obstacle create/update/delete | Button progress | n/a | Validation errors shown at field level; conflict or not-found messages; no partial updates |
| Unauthorized admin action | n/a | n/a | Session expired, redirect to sign-in |

---

## 11. Error handling expectations (API behaviour)

| Situation | Expected behaviour |
|---|---|
| Invalid input | Client-error status with structured error body listing the field problems |
| Unknown building, floor, node or obstacle | Not-found status with structured error body |
| No route available | A defined, non-error "no route" outcome including the reason; the app must not treat it as a crash |
| Unauthenticated or unauthorised admin call | Authentication or authorisation error status |
| Unexpected failure | Generic server-error body with no stack trace or internals |

---

## 12. MVP scope

### 12.1 In scope (MVP)

- Generic multi-environment data model, with one fully populated sample building (mall, at least two floors)
- Flutter app (Android and iOS) with screens 1 to 12
- Manual start selection and destination search
- Routing with Dijkstra and A*, all five preferences, multi-floor routes, estimated distance and time, instructions
- Obstacles with administrator management, persistence and rerouting, warnings and no-route handling
- Local device settings for default preference and accessibility
- REST API with OpenAPI docs, validation and structured errors
- PostgreSQL with Flyway migrations and seed data
- Docker Compose, CI, and tests at all levels
- Documentation: README, architecture, API, database, agent guidance

### 12.2 Post-MVP (Could)

- QR-code start selection
- Left/right turn wording refinement
- In-app editor for buildings and graph data
- Additional sample environments (campus, hospital)
- Scheduled or time-bound obstacles
- Live updates via push or WebSockets
- Route history and user accounts
- Multiple languages

---

## 13. Out of scope

- Automatic indoor positioning (Wi-Fi, BLE, UWB, sensors, GPS) and live location tracking
- Real-time obstacle detection or crowd data
- Outdoor or street navigation, and routing between separate buildings
- Offline mode
- GIS tooling and automated floor-plan ingestion
- Advertising, payments, shop promotions or any commercial mall features
- Social features, reviews, favourites
- Voice guidance and augmented reality
- Redis, Kubernetes, microservices or other additional infrastructure
- Visitor accounts and personal data collection

---

## 14. Success criteria

The MVP is successful when all of the following are demonstrably true.

**Functional**

1. A user can select the sample building, search a destination, set a start, choose each of the five preferences and see a correct route on the map.
2. Routes between floors show floor transitions and the correct floor plan for each segment.
3. All five demonstration scenarios in Section 8.3 work live.
4. After an administrator blocks a connection, the same request returns a different route with a warning; after the connection is unblocked, the original route returns.
5. Unreachable destinations produce the dedicated no-route state, not an error.

**Technical**

6. Dijkstra and A* produce identical costs across the engine test suite, including multi-floor, blocked and accessibility cases.
7. Engine, backend, Flutter widget and end-to-end tests pass in CI on the main branch.
8. A fresh clone starts backend and database with a single Docker Compose command and loads sample data, following the README.
9. The app runs on an Android emulator or device, and on iOS where macOS and Xcode are available.
10. No secrets are present in the repository.

**Team and process**

11. Each of the four owners has meaningful, reviewable contributions in their domain.
12. API contract documents match actual behaviour.

**Platform (reuse)**

13. A second small environment can be added through seed data alone, with no change to engine, API or screens (verified once, even with a minimal dataset).

---

## 15. Assumptions summary

| ID | Assumption |
|---|---|
| A-1 | The truncated step 10 of the integration test list ends with confirming an alternative route or a no-route outcome. |
| A-2 | Target graph size is up to about 2,000 nodes. |
| A-3 | Visitors do not sign in; no user accounts. |
| A-4 | A single administrator role with credentials from environment configuration. |
| A-5 | Connections are bidirectional by default. |
| A-6 | Walking speed about 1.2 m/s, with slower stairs, fixed elevator time, all configurable. |
| A-7 | Horizontal distance is derived from coordinates and scale; vertical connection cost is set explicitly. |
| A-8 | An "indoor environment" equals one building in the MVP; multi-building campuses are not supported. |
| A-9 | The sample environment is a three-floor mall unless you specify two. |
| A-10 | Admin use is mobile-first and light; a separate web admin is not required. |

---

## 16. Risks

| Risk | Impact | Mitigation |
|---|---|---|
| Floor-plan data creation is time-consuming | Delays demo | Use a simple, schematic mall with a limited number of nodes; build it early |
| Coordinate system mismatch between SVG and node data | Route drawn in the wrong place | Single coordinate convention per floor, documented in DESIGN.md, verified visually in a test screen |
| Heuristic overestimates in multi-floor or time-based costs | Wrong A* results | Define the heuristic per cost model, add tests comparing A* with Dijkstra |
| Mall-specific logic leaks into the core | Platform value lost | Review checklist item on every PR; only seed data may name mall concepts |
| Four developers diverge on contracts | Integration failures | Freeze API.md before integration; contract changes documented first |
| iOS build requires macOS and Xcode | Not everyone can test iOS | Document requirements; treat Android as the primary test target |
| Scope creep | Missed deadline | Strict MVP list; Section 12.2 items need approval |

---

## 17. Open questions

1. Confirm the truncated step 10 (A-1).
2. Is there a hard deadline or demo date that affects scope? [ASSUMPTION: none given]
3. Which test devices are available (Android only, or iOS too)?

---

## 18. Decisions requiring your approval

Each has a recommended default that agents will follow until you decide otherwise.

| ID | Decision | Options | Recommended default |
|---|---|---|---|
| **D-1** | Floor-map representation | (a) SVG floor-plan background with node coordinates in the same system; (b) fully data-driven geometry for rooms and corridors | (a) SVG background plus node and POI overlay. Simpler and consistent with the prompt's "manual SVG or JSON" guidance. |
| **D-2** | Start location in the MVP | Manual only; manual plus QR | Manual only. QR is a Could. |
| **D-3** | Admin authentication and UI location | (a) Sign-in screen in the same app with one configured admin account; (b) static API key; (c) no authentication for demo | (a). Matches "protected interface" and keeps Spring Security limited to admin endpoints. |
| **D-4** | AVOID_STAIRS semantics | Hard prohibition; heavy penalty with warning when stairs are unavoidable | Heavy penalty with warning, with ACCESSIBLE as the strict mode. Keeps the two modes meaningfully different. |
| **D-5** | Number of floors in the sample mall | Two; three | Three, to better demonstrate multi-floor and transition logic, provided data effort is acceptable. |
| **D-6** | Rooms versus POIs as separate entities | Separate tables per the prompt's schema list; a single combined concept | Separate: rooms are spaces, POIs are searchable destinations linked to a node. Collapse to one only if the team finds it redundant. |
| **D-7** | Turn-by-turn detail | Segment and floor-change instructions only; plus left/right turns | Segment and floor-change instructions as Must; left/right turns as Should. |
| **D-8** | Obstacle target | Edge or node; edge only | Edge or node, as the prompt requires blocked nodes where supported. |
| **D-9** | Obstacle lifecycle | Manual resolve or delete only; scheduled start/end | Manual only, as per the prompt. |
| **D-10** | Persistence of visitor settings | Device-local only; server-side | Device-local only. |
