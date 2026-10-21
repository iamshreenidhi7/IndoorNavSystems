# API: Indoor Navigation and Path Optimization System

| Field | Value |
|---|---|
| Status | Draft v0.1, awaiting owner approval on flagged decisions |
| Depends on | PRD.md, ARCHITECTURE.md (Sections 8, 11, 12, 13, 15), DATABASE.md |
| Audience | Mobile developer, backend developer, AI coding agents, API testers |
| Scope | The contract between the Flutter app and the Spring Boot backend, plus rules for any integration. No code is defined here. |

## 0. How to use this document

- This file is the human-readable contract. The backend also generates an OpenAPI document; **CI compares the two**. If they disagree, fix one of them in the same pull request. Changes to this file are agreed with the mobile developer **before** implementation.
- **[ASSUMPTION APA-n]** marks something assumed. **[DECISION APD-n]** marks a choice needing owner approval. Both are collected in Section 15. Agents follow the recommended default until a decision is made.
- The JSON examples in the Master Project Prompt are illustrative. Where they differ from this document, this document wins.
- Priorities follow the PRD: **Must** endpoints are required for the MVP; **Should** and **Could** are noted per endpoint.
- This document describes data in field tables, not code. Field names are exactly as they appear on the wire.

---

## 1. API overview

| Property | Value |
|---|---|
| Style | REST over HTTPS, JSON (UTF-8) |
| Base path | `/api` (unversioned for the MVP, ARCHITECTURE.md AD-9) |
| Clients | One: the Flutter app (Android and iOS). Not a browser, so no CORS |
| Authentication | Public read endpoints; administrator endpoints use a bearer token (Section 3) |
| State | Stateless: no server sessions, no cookies |
| Real-time | None. The app learns of changes only by asking (ARCHITECTURE.md 9.6) |
| Documentation | OpenAPI document and Swagger UI, enabled in development, disabled or protected in production |
| External APIs | **None** (Section 10) |

### 1.1 Endpoint index

| # | Method and path | Access | Purpose | Priority |
|---|---|---|---|---|
| 1 | `GET /api/health` | Public | Liveness and database reachability | Must |
| 2 | `GET /api/buildings` | Public | List buildings | Must |
| 3 | `GET /api/buildings/{buildingId}` | Public | Building details | Must |
| 4 | `GET /api/buildings/{buildingId}/floors` | Public | Ordered floors of a building | Must |
| 5 | `GET /api/floors/{floorId}/map` | Public | Everything needed to draw one floor | Must |
| 6 | `GET /api/points-of-interest` | Public | Search or browse destinations | Must |
| 7 | `GET /api/points-of-interest/categories` | Public | Destination categories of a building | Should |
| 8 | `GET /api/buildings/{buildingId}/nodes` | Public | Labelled nodes for choosing a start | Must |
| 9 | `POST /api/navigation/route` | Public | Compute a route | Must |
| 10 | `POST /api/auth/login` | Public | Administrator sign-in | Must |
| 11 | `GET /api/obstacles` | Admin | List obstacles | Must |
| 12 | `POST /api/obstacles` | Admin | Create an obstacle | Must |
| 13 | `PUT /api/obstacles/{obstacleId}` | Admin | Update or resolve an obstacle | Must |
| 14 | `DELETE /api/obstacles/{obstacleId}` | Admin | Delete an obstacle | Must |
| 15 | `GET /api/rooms` | Public | List rooms of a building or floor | Could (APD-1) |
| 16 | `GET /assets/floorplans/{path}` | Public | Floor-plan SVG files | Must |

Endpoint 15 is reserved in the contract but not built unless needed, because the floor map already carries rooms (APD-1). Endpoint 16 is a static file route outside `/api`.

---

## 2. Conventions

### 2.1 Wire format

| Topic | Rule |
|---|---|
| Content type | `application/json; charset=UTF-8` for all request bodies and responses. Other content types on requests with a body return 415 |
| Property names | camelCase |
| IDs | Positive 64-bit integers as JSON numbers, stable across requests and restarts (DATABASE.md Section 2) |
| Distances | Metres; route totals rounded to one decimal place |
| Durations | Whole seconds (rounded to nearest) |
| Coordinates | Plan units, up to two decimals, in the floor's own coordinate system (DATABASE.md 4.2, 4.4) |
| Timestamps | ISO-8601 in UTC with a `Z` suffix |
| Enumerations | Upper-case strings (for example `FOUND`, `ACTIVE`). Category values are lower-case slugs |
| Text | Server trims leading and trailing spaces; blank values are rejected where a value is required |
| Unknown request fields | Ignored |
| Unknown response fields | **Clients must ignore them**, so additive changes are safe |
| Unknown enumeration values in responses | **Clients must handle them gracefully** with a generic fallback (for example an unknown obstacle type or warning code), so new values are not breaking |
| Nulls | Fields marked *nullable* are always present and null when empty. Fields marked *conditional* are omitted when not applicable |

### 2.2 Standard headers

| Header | Direction | Rule |
|---|---|---|
| `Authorization: Bearer <token>` | Request | Required on admin endpoints only |
| `Content-Type` | Request | Required on POST and PUT |
| `X-Request-Id` | Response | Always present. **Generated by the server**; client-supplied values are ignored (APD-4). Quoted in error bodies and logs, and can be given to support |
| `Cache-Control` | Response | `no-store` on every JSON response, so the app never shows stale obstacle state (PRD FR-32). Floor-plan files are the exception (Section 8.1) |
| `Retry-After` | Response | On 429 and 503 where a wait is meaningful |
| `Location` | Response | On 201 from obstacle creation |
| `X-Content-Type-Options: nosniff` | Response | Always |

### 2.3 Endpoint rules

- Reads are `GET` and never change data. `POST /api/navigation/route` is a **read-only computation**; it is POST only because it carries a body.
- Collections are returned as a plain list in a documented fixed order. There are no client-controlled sort parameters.
- Filters are query parameters in camelCase. A missing **required** filter returns 400. Unknown query parameters are ignored.
- Path IDs must be positive integers; anything else returns 400.

---

## 3. Authentication and authorization

Implements ARCHITECTURE.md Section 11.

### 3.1 Model

| Aspect | Rule |
|---|---|
| Roles | Anonymous visitor and `ADMIN`. No registration, no other roles |
| Credentials | One administrator defined by environment configuration (username and password hash) |
| Token | A signed, short-lived bearer token issued by `POST /api/auth/login`; lifetime from `JWT_TTL_MINUTES` (default 60), no refresh |
| Token contents | Subject (administrator username), role, issued-at, expiry. **No personal data and no secrets** |
| Verification | On every admin request the server verifies signature, expiry and role. Failures never reveal which check failed |
| Client storage | The app keeps the token **in memory only** and never writes it to disk. Restarting the app requires signing in again |
| CSRF | Not applicable: the token travels in a header and no cookies are used |
| Default | Any endpoint not listed as public is **denied** |

### 3.2 Authorization matrix (enforced server-side)

| Endpoint group | Visitor (no token) | Administrator |
|---|---|---|
| Health, buildings, floors, floor map, POI, nodes, route, plan files | Allowed | Allowed |
| Login | Allowed | Allowed |
| Obstacle list, create, update, delete | **401** | Allowed |

Rules:

- The UI hiding admin controls is a convenience only. **Every** authorization decision is made by the backend, on every request.
- 401 means "no valid token" (missing, malformed, expired, or wrong signature). 403 means "authenticated but not permitted"; it is defined for future roles and is not reachable in the MVP.
- 401 responses carry a `WWW-Authenticate` header naming the Bearer scheme.
- Public map and route responses expose blocked connections with their type and visitor-facing description. This satisfies PRD US-12 without making the obstacle list public. They never expose administrative data (creation times, versions, internal notes).
- The graph tables are read-only at the API level for everyone, including the administrator (PRD FR-39).

---

## 4. Validation and error conventions

### 4.1 Validation layers

| Layer | Checks |
|---|---|
| Request DTO validation | Required fields, types, lengths, enum membership, format. Errors list every offending field |
| Service validation | Existence of referenced rows, same-building consistency, obstacle type versus target, uniqueness of active obstacles |
| Database constraints | Final safety net (DATABASE.md Section 5). A constraint violation that reaches the API is mapped to 409 or 400, never to a raw database message |

### 4.2 Error body (all non-2xx responses)

| Field | Type | Notes |
|---|---|---|
| `timestamp` | string | UTC |
| `status` | integer | HTTP status |
| `code` | string | Stable machine-readable code from the catalogue below. **Clients branch on this, not on `message`** |
| `message` | string | Plain-language English summary; safe to show or ignore (the app uses its own copy per DESIGN.md Section 11.2) |
| `path` | string | Request path (no query string) |
| `requestId` | string | Same as the `X-Request-Id` header |
| `details` | list, conditional | For validation errors only: each item has `field`, `message` |

Error bodies **never** contain stack traces, SQL, class names, file paths, tokens, or whether a username exists.

### 4.3 Error code catalogue

| HTTP | Code | When |
|---|---|---|
| 400 | `VALIDATION_FAILED` | A field is missing, blank, out of range or malformed; `details` lists them |
| 400 | `MALFORMED_REQUEST` | Body is not valid JSON, has the wrong shape, or contains an unknown enumeration value; or a path or query value has the wrong type |
| 400 | `NODES_NOT_IN_SAME_BUILDING` | Route start and destination belong to different buildings |
| 401 | `UNAUTHORIZED` | Missing, invalid or expired token; failed login |
| 403 | `FORBIDDEN` | Authenticated but not permitted (reserved) |
| 404 | `RESOURCE_NOT_FOUND` | The building, floor, node, or obstacle does not exist (including a route start or destination node) |
| 405 | `METHOD_NOT_ALLOWED` | Wrong method for the path |
| 409 | `CONFLICT` | Optimistic-lock mismatch on an obstacle update, or an active obstacle already exists on that target |
| 413 | `PAYLOAD_TOO_LARGE` | Request body exceeds the limit (APA-6) |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | Request body is not JSON |
| 429 | `TOO_MANY_REQUESTS` | Rate or login throttle exceeded (Section 11) |
| 500 | `INTERNAL_ERROR` | Unexpected failure; generic message, details only in logs |
| 503 | `SERVICE_UNAVAILABLE` | Database unreachable, or route computation exceeded its time limit |

**Not an error:** "no route available" is returned as **HTTP 200** with `status` = `NO_ROUTE` (PRD Section 11; ARCHITECTURE.md AD-12).

### 4.4 Validation limits (summary)

| Input | Rule |
|---|---|
| Any ID in a path or body | Positive integer |
| `query` (search text) | 1 to 60 characters after trimming; if absent or blank, the endpoint lists instead of searching |
| `category` | Lower-case slug (letters, digits, hyphens, starting with a letter), up to 50 characters |
| `limit` | Integer from 1 to 50 |
| Obstacle `description` | 3 to 200 characters after trimming (DATABASE.md DBD-8) |
| Login `username` | 1 to 100 characters |
| Login `password` | 1 to 200 characters |
| Request body | At most 16 KB (APA-6); all legitimate bodies are far smaller |

---

## 5. Pagination, filtering and sorting

**No pagination in the MVP.** Every collection is small by design (DATABASE.md 6.4), and the only unbounded input, search text, is capped by `limit`.

| Collection | Bound | Default order |
|---|---|---|
| Buildings | Handful | Name, ascending |
| Floors of a building | Tens | Level, ascending (the app reverses it for the floor switcher) |
| Floor map arrays | Up to a few hundred items per floor | ID, ascending |
| Destination search results | `limit`, default 30, maximum 50 (APA-5) | Prefix matches first, then other matches, then name ascending |
| Categories | Tens | Slug, ascending |
| Labelled nodes | `limit`, default 50, maximum 100 | Label, ascending |
| Obstacles of a building | Up to a few hundred | Last updated, newest first |

**Filtering rules:**

- Text search is case-insensitive "contains" matching. `%` and `_` in the search text are treated as ordinary characters, not wildcards.
- `category` is an exact match on the slug.
- A search that matches nothing returns 200 with an empty list, never 404.

**Future:** if a collection can grow beyond comfortable size, cursor-based pagination using `limit` and `cursor` parameters is reserved. It is introduced as an additive change.

---

## 6. Public endpoints

### 6.1 `GET /api/health`

Liveness and database reachability, used by Docker Compose and monitoring.

| Response field | Type | Notes |
|---|---|---|
| `status` | string | `UP` or `DOWN` |
| `database` | string | `UP` or `DOWN` |

Returns 200 when both are `UP`, 503 (`SERVICE_UNAVAILABLE` body fields plus the same two status fields) otherwise. It exposes **no** version, configuration, host or build details.

### 6.2 `GET /api/buildings`

Lists all buildings (journey J1, screen 3).

| Response item field | Type | Notes |
|---|---|---|
| `id` | integer | |
| `name` | string | |
| `description` | string, nullable | |
| `buildingType` | string | Display label only |
| `address` | string, nullable | |
| `floorCount` | integer | |

Errors: 503.

### 6.3 `GET /api/buildings/{buildingId}`

| Response field | Type | Notes |
|---|---|---|
| (all fields of 6.2) | | |
| `defaultFloorId` | integer | The floor to open first: the lowest-level floor containing a building entrance, otherwise the lowest-level floor (APA-1) |

Errors: 400, 404, 503.

### 6.4 `GET /api/buildings/{buildingId}/floors`

| Response item field | Type | Notes |
|---|---|---|
| `id` | integer | |
| `buildingId` | integer | |
| `level` | integer | Ground is 0; basements negative |
| `displayName` | string | Label from data, for example "G" or "Floor 1" |

Errors: 400, 404, 503.

### 6.5 `GET /api/floors/{floorId}/map`

Everything the app needs to draw one floor with a single request (journeys J1 to J3, screen 5).

**Response fields:**

| Field | Type | Notes |
|---|---|---|
| `floor` | object | `id`, `buildingId`, `level`, `displayName` |
| `plan` | object | `url` (relative path under `/assets/floorplans/`), `width`, `height`, `metresPerUnit`. The SVG's coordinate system equals `width` by `height` |
| `nodes` | list | Nodes **on this floor** |
| `edges` | list | Connections **touching this floor**, including vertical ones |
| `rooms` | list | Rooms on this floor |
| `pointsOfInterest` | list | POIs located at nodes on this floor |

`nodes` item: `id`, `x`, `y`, `type`, `name` (nullable), `accessible`, `blocked` (boolean), `obstacle` (conditional: present when blocked, an object with `id`, `type`, `description`).

`edges` item: `id`, `fromNodeId`, `toNodeId`, `connectionType` (the connection type code), `accessible`, `bidirectional`, `otherFloorId` (nullable; set for vertical connections), `blocked`, `obstacle` (conditional, same shape as above).

- For a vertical connection, only the endpoint on this floor appears in `nodes`; the other endpoint's floor is `otherFloorId`. The app draws the transition marker at the endpoint on this floor.

`rooms` item: `id`, `name`, `category`, `description` (nullable), `bounds` (object with `x`, `y`, `width`, `height`).

`pointsOfInterest` item: `id`, `name`, `category`, `nodeId`, `roomId` (nullable).

Errors: 400, 404, 503.

Notes: the response reflects the **current** obstacle state on every call. The response is compressible (APA-8).

### 6.6 `GET /api/points-of-interest`

Destination search and browse (journey J1, screen 6).

| Query parameter | Required | Rules |
|---|---|---|
| `buildingId` | Yes | Positive integer |
| `query` | No | Search text (Section 4.4). Absent or blank means browse |
| `category` | No | Exact category slug |
| `limit` | No | Default 30, maximum 50 |

| Response item field | Type | Notes |
|---|---|---|
| `id` | integer | POI ID |
| `name` | string | |
| `category` | string | Slug |
| `nodeId` | integer | **The ID to send as `destinationNodeId` to the route endpoint** |
| `floorId` | integer | From the linked node |
| `floorDisplayName` | string | |

Errors: 400 (missing `buildingId`, bad `limit`), 404 (unknown building), 503. An empty result is 200 with an empty list.

### 6.7 `GET /api/points-of-interest/categories`

Category chips for the search screen.

| Query parameter | Required | Rules |
|---|---|---|
| `buildingId` | Yes | Positive integer |

Response: a list of category slugs present in the building, ascending. Errors: 400, 404, 503. The app maps known slugs to icons and uses a generic icon for unknown ones (DESIGN.md Section 5.5).

### 6.8 `GET /api/buildings/{buildingId}/nodes`

Labelled nodes the user can choose as a **start** from a searchable list (PRD FR-11). A node is labelled if it has a display name or a linked POI.

| Query parameter | Required | Rules |
|---|---|---|
| `query` | No | Search text on the label |
| `limit` | No | Default 50, maximum 100 |

| Response item field | Type | Notes |
|---|---|---|
| `id` | integer | **The ID to send as `startNodeId`** |
| `label` | string | The node's display name, otherwise its POI's name |
| `type` | string | Node type |
| `floorId` | integer | |
| `floorDisplayName` | string | |
| `x`, `y` | number | Plan units |

Errors: 400, 404, 503.

### 6.9 `POST /api/navigation/route`

Computes a route with the Java engine (journeys J1 to J3; screens 7 to 10). The result is an **estimate**; it is never live tracking (PRD FR-21).

**Request fields:**

| Field | Type | Required | Rules |
|---|---|---|---|
| `startNodeId` | integer | Yes | Existing node |
| `destinationNodeId` | integer | Yes | Existing node in the same building as the start |
| `preference` | string | No | One of `SHORTEST`, `FASTEST`, `ACCESSIBLE`, `AVOID_STAIRS`, `PREFER_ELEVATOR`; defaults to `SHORTEST` |

**Response fields (always present):**

| Field | Type | Notes |
|---|---|---|
| `status` | string | `FOUND` or `NO_ROUTE` |
| `preference` | string | The preference that was applied |

**When `status` is `FOUND`, these fields are also present:**

| Field | Type | Notes |
|---|---|---|
| `pathNodes` | list | Ordered nodes: `nodeId`, `floorId`, `x`, `y`, `type`, `name` (nullable). The app draws the route from these without further calls |
| `distanceMeters` | number | Total, one decimal |
| `estimatedDurationSeconds` | integer | Estimate |
| `floorTransitions` | list | Possibly empty. Each: `fromFloorId`, `toFloorId`, `connectionType`, `pathIndex` (index in `pathNodes` of the node where the transition starts) |
| `instructions` | list | Ordered steps (below). Never empty |
| `warnings` | list | Possibly empty (below) |

`instructions` item:

| Field | Type | Notes |
|---|---|---|
| `type` | string | `START`, `WALK`, `FLOOR_CHANGE`, `ARRIVE` |
| `text` | string | Default English text (ARCHITECTURE.md AD-10) |
| `distanceMeters` | number, conditional | For `WALK` |
| `floorId` | integer | Floor the step takes place on (for `FLOOR_CHANGE`, the floor you leave) |
| `toFloorId` | integer, conditional | For `FLOOR_CHANGE` |
| `connectionType` | string, conditional | For `FLOOR_CHANGE` (for example `ELEVATOR`) |
| `turn` | string, conditional | `LEFT`, `RIGHT`, or `STRAIGHT`; present only when turn wording is supported (PRD FR-28, Should) |
| `startPathIndex`, `endPathIndex` | integer | The range of `pathNodes` the step covers, so the app can highlight and centre it |

`warnings` item:

| Field | Type | Notes |
|---|---|---|
| `code` | string | `DETOUR` (an active obstacle makes this route differ from the unobstructed one) or `STAIRS_REQUIRED` (AVOID_STAIRS had no stair-free route) |
| `message` | string | English summary |
| `obstacleIds` | list of integer | Obstacles involved; empty if none |
| `extraDistanceMeters` | number, nullable | Additional distance compared with the unobstructed route, for `DETOUR` |

**When `status` is `NO_ROUTE`, this field is present instead:**

| Field | Type | Notes |
|---|---|---|
| `noRoute` | object | `reasonCode` (`OBSTACLE`, `ACCESSIBILITY_CONSTRAINT`, `DISCONNECTED`), `message`, `obstacles` (list of `id`, `type`, `description`; the active obstacles on the otherwise-available route, empty unless the reason is `OBSTACLE`) |

**Behaviour:**

- Start equal to destination returns `FOUND` with a single path node, zero distance, zero duration, and one `ARRIVE` step.
- The server loads the building graph and active obstacles in one consistent snapshot, runs A*, and never serves a cached result (ARCHITECTURE.md 6.3).
- Reason codes come from the diagnosis in ARCHITECTURE.md 6.4.
- The same request can be repeated safely: it changes no data.

**Errors:** 400 (`VALIDATION_FAILED`, `MALFORMED_REQUEST`, `NODES_NOT_IN_SAME_BUILDING`), 404 (unknown node), 429, 503 (database unavailable or computation timed out; APA-4), 500.

### 6.10 `GET /api/rooms` (reserved, Could)

`GET /api/rooms` lists rooms for `buildingId` (required) and optional `floorId`, returning the same `rooms` item shape as the floor map plus `floorId`. It is **not built in the MVP** unless a use appears (APD-1).

---

## 7. Administrator endpoints

### 7.1 `POST /api/auth/login`

| Request field | Type | Required | Rules |
|---|---|---|---|
| `username` | string | Yes | 1 to 100 characters |
| `password` | string | Yes | 1 to 200 characters |

| Response field | Type | Notes |
|---|---|---|
| `accessToken` | string | Signed bearer token |
| `tokenType` | string | `Bearer` |
| `expiresInSeconds` | integer | From `JWT_TTL_MINUTES` |

Errors: 400, 401 (`UNAUTHORIZED`, with the same generic message whether the username or the password is wrong), 429 (login throttle), 503.

Rules: the password is compared against the configured hash only; passwords and tokens are never logged; response time does not differ noticeably between an unknown username and a wrong password.

### 7.2 Obstacle resource

Used by every obstacle endpoint below.

| Field | Type | Notes |
|---|---|---|
| `id` | integer | |
| `buildingId` | integer | Derived by the server from the target |
| `targetType` | string | `EDGE` or `NODE` |
| `edgeId` | integer, nullable | Set when `targetType` is `EDGE` |
| `nodeId` | integer, nullable | Set when `targetType` is `NODE` |
| `floorIds` | list of integer | Floors the target touches (one, or two for a vertical connection) |
| `targetSummary` | string | Readable target, for example the labelled end nodes of a connection, for the admin list |
| `obstacleType` | string | `BLOCKED_CORRIDOR`, `CLOSED_ROOM`, `BROKEN_ELEVATOR`, `CLOSED_STAIRCASE`, `CONSTRUCTION_AREA`, `RESTRICTED_CONNECTION` |
| `description` | string | Visitor-facing; 3 to 200 characters |
| `status` | string | `ACTIVE` or `RESOLVED` |
| `createdAt`, `updatedAt` | string | UTC timestamps |
| `version` | integer | Required on update (optimistic locking, APD-3) |

### 7.3 `GET /api/obstacles`

| Query parameter | Required | Rules |
|---|---|---|
| `buildingId` | Yes | Existing building |
| `status` | No | `ACTIVE` or `RESOLVED`; absent means all |

Response: list of obstacle resources, newest update first. Errors: 400, 401, 404, 503.

### 7.4 `POST /api/obstacles`

| Request field | Type | Required | Rules |
|---|---|---|---|
| `targetType` | string | Yes | `EDGE` or `NODE` |
| `targetId` | integer | Yes | Existing edge or node |
| `obstacleType` | string | Yes | Must be compatible with the target (for example `BROKEN_ELEVATOR` on an elevator connection or elevator node) |
| `description` | string | Yes | 3 to 200 characters |

The client **does not send** `buildingId` or `status`: the building is derived from the target (so it can never disagree) and a new obstacle is always `ACTIVE` (APD-2).

Response: **201** with the obstacle resource and a `Location` header. Errors: 400, 401, 404 (target not found), 409 (an active obstacle already exists on this target), 503.

### 7.5 `PUT /api/obstacles/{obstacleId}`

Updates the mutable fields, including resolving or reactivating.

| Request field | Type | Required | Rules |
|---|---|---|---|
| `obstacleType` | string | Yes | As in 7.4 |
| `description` | string | Yes | 3 to 200 characters |
| `status` | string | Yes | `ACTIVE` or `RESOLVED` |
| `version` | integer | Yes | The version last read |

The **target cannot be changed**: delete and create a new obstacle instead.

Response: 200 with the updated resource (new `version`). Errors: 400, 401, 404, 409 (version mismatch, meaning someone else changed it, or reactivating while another active obstacle exists on the target), 503.

### 7.6 `DELETE /api/obstacles/{obstacleId}`

Response: **204**, no body. Errors: 401, 404, 503. Deleting an unknown or already deleted obstacle returns 404.

### 7.7 Effect of obstacle changes

After any successful create, update or delete is committed, **the next** route or map request reflects it (PRD FR-32). There is no push; the app refreshes when the user requests a route, changes preference, or returns to the foreground during navigation (ARCHITECTURE.md 9.6).

---

## 8. Static assets

### 8.1 `GET /assets/floorplans/{path}`

Serves the SVG floor plans referenced by `plan.url` (DATABASE.md 5.2, ARCHITECTURE.md AD-4).

| Aspect | Rule |
|---|---|
| Access | Public, read-only |
| Path handling | The path must match the stored pattern (relative, lower-case, ends in `.svg`, no parent-directory segments) **and** resolve inside the configured floor-plan directory; anything else returns 404, never revealing other files |
| Content type | `image/svg+xml` |
| Caching | Cacheable with a validator (entity tag) and a modest lifetime (APA-7), since plans change only with releases |
| Hardening headers | `X-Content-Type-Options: nosniff` and a restrictive content security policy that forbids scripts, so a plan opened in a browser cannot execute code |
| Errors | 404 |

---

## 9. Internal interfaces

Beyond the REST API, three internal boundaries exist. They are not network APIs.

| Boundary | Style | Contract |
|---|---|---|
| Backend to algorithm engine | In-process library call | One stable interface taking a graph, start, destination, preference, blocked node and edge IDs, and cost configuration, and returning `FOUND` or `NO_ROUTE` with path, edge IDs, distance, time and transitions (ARCHITECTURE.md 5.2). The engine has no HTTP, Spring or database dependency |
| Backend to database | JDBC through JPA, schema validated | DATABASE.md. Runtime role may write only `obstacles` (DATABASE.md 8.2) |
| Mobile app to backend | The REST API above | The only integration the mobile app has |

---

## 10. External APIs, integrations and webhooks

### 10.1 External APIs

**None at runtime.** The product calls no third-party API: no maps, positioning, analytics, push, payment, email, or identity provider (PRD Section 9; ARCHITECTURE.md Section 14). Consequently there are no outbound API keys or secrets beyond the database credentials and the admin and token secrets.

Build-time and process tooling only: GitHub (source and Actions), package registries, and optionally a container registry.

### 10.2 Webhooks

**None.** The backend neither sends nor receives webhooks in the MVP, and **no public callback endpoint exists**. Obstacle changes are made only through the authenticated admin endpoints.

### 10.3 Standing rules if an integration is ever added

These apply to any future external call or webhook and need an explicit decision and an update to this document before anything is built.

| Topic | Rule |
|---|---|
| Inbound webhooks | Dedicated path; HTTPS only; verify a **signature** computed with a shared secret over the raw body, checked in constant time; require a timestamp and reject anything outside a short window (replay protection); deduplicate by event ID (idempotent handling); reject unverified requests with 401 before any processing; respond 2xx quickly and process asynchronously |
| Outbound calls | Explicit connect and read timeouts; a small number of retries with backoff **only for idempotent requests**; a circuit breaker or fail-fast after repeated failures; the core routing path must **never depend** on an external call |
| Secrets | Server-side environment variables only; never sent to or stored in the app |
| Degradation | An integration failure degrades only its own feature; navigation continues |
| Data | Send the minimum; no personal data without a privacy review |

---

## 11. Rate limiting and abuse controls

| Control | MVP behaviour |
|---|---|
| Login throttle (in the application) | After 5 failed attempts within 15 minutes from one client address, further attempts return 429 with `Retry-After` until the window passes (APA-2). Counters live in memory and reset on restart. This is the one limit the application enforces itself |
| General request limit (hosted deployments) | Enforced by the reverse proxy in front of the backend, not by the application: about 60 requests per minute per client address overall, and about 20 per minute for `POST /api/navigation/route` (APA-3). Exceeding it returns 429 |
| Local development | No proxy limit; login throttle still applies |
| Request size | Bodies over 16 KB return 413 |
| Query bounds | Search `limit` capped at 50 (100 for nodes); search text capped at 60 characters |
| Computation bound | A route calculation that exceeds 10 seconds is abandoned and returns 503 (APA-4); graphs are bounded by data policy (about 2,000 nodes) |
| Client address | The application trusts forwarded-address headers **only** when configured to run behind a trusted proxy (`TRUST_FORWARDED_HEADERS`), so the throttle cannot be bypassed or poisoned by spoofed headers |

**Client behaviour on 429:** the app shows its generic "try again shortly" state (DESIGN.md Section 11), honours `Retry-After` if present, and does not retry automatically. A 429 produced by the proxy may not carry the standard error body, so clients must key on the status code.

---

## 12. Required environment variables

All server variables are read at startup into typed configuration, and the application **fails fast** with a clear message if a required value is missing or invalid for its profile. Defaults in the repository are safe for local use only.

### 12.1 Backend

| Variable | Required | Secret | Default | Purpose |
|---|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Yes | No | `dev` locally | Selects dev, test or prod behaviour |
| `SERVER_PORT` | No | No | 8080 | Listening port |
| `DB_URL` | Yes | No | Local Compose value | Database connection |
| `DB_USER` | Yes | No | Local value | Runtime database role (DATABASE.md 8.2) |
| `DB_PASSWORD` | Yes | **Yes** | None in prod | Runtime database password |
| `MIGRATION_DB_USER` | No | No | Falls back to `DB_USER` | Flyway owner role in hosted environments |
| `MIGRATION_DB_PASSWORD` | No | **Yes** | Falls back to `DB_PASSWORD` | Flyway owner password |
| `ADMIN_USERNAME` | Yes (admin features) | No | Local value | The administrator's username |
| `ADMIN_PASSWORD_HASH` | Yes (admin features) | **Yes** | None in prod | Password hash (a modern one-way hash such as BCrypt with cost 10 or higher); the plain password is never configured |
| `JWT_SECRET` | Yes | **Yes** | None in prod | Token-signing key: random, at least 32 bytes; different in every environment |
| `JWT_TTL_MINUTES` | No | No | 60 | Token lifetime |
| `SEED_DEMO_DATA` | No | No | true in dev, false in prod | Load the demo seed (DATABASE.md Section 12) |
| `SWAGGER_ENABLED` | No | No | true in dev, false in prod | Expose OpenAPI and Swagger UI |
| `FLOORPLAN_DIR` | No | No | Packaged location | Directory the plan files are served from |
| `LOGIN_MAX_FAILED_ATTEMPTS` | No | No | 5 | Login throttle threshold |
| `LOGIN_ATTEMPT_WINDOW_MINUTES` | No | No | 15 | Login throttle window |
| `TRUST_FORWARDED_HEADERS` | No | No | false | True only behind a trusted reverse proxy |
| `ROUTING_*` | No | No | Documented defaults | Surcharge factors and the route time limit (for example `ROUTING_TIMEOUT_SECONDS`, default 10) |

Prod profile requirements: `DB_PASSWORD`, `JWT_SECRET` and `ADMIN_PASSWORD_HASH` have **no defaults**; startup fails if any is missing, if `JWT_SECRET` is shorter than 32 bytes, or if it equals a known development value.

### 12.2 Mobile app

| Setting | Required | Secret | Purpose |
|---|---|---|---|
| `API_BASE_URL` (build or run time) | Yes | **No**, it is public | Base URL of the backend |

**The app contains no secrets**: no API keys, no signing keys, no passwords. The admin token is obtained at run time and held in memory only. Development values: the Android emulator reaches the host through its special loopback alias, the iOS simulator uses localhost, and a physical device uses the computer's LAN address. Plain HTTP is allowed only in development builds and is not enabled in release builds.

### 12.3 Rules

- `.env` is never committed; `.env.example` lists every variable with placeholder values and is kept current.
- No variable in the table above may be echoed in logs, error bodies, health output or API responses.
- A secret that is committed by mistake is treated as leaked: rotate it, do not just delete it.

---

## 13. Data privacy

| Topic | Rule |
|---|---|
| Personal data | None collected or returned. Visitors are anonymous; there are no accounts, device IDs, positions or route histories (PRD NFR-07) |
| Request logging | Method, path **without query string**, status, duration and request ID only. No request or response bodies, no authorization headers, no search text, no passwords or tokens |
| Login logging | Failed logins are logged as a count and request ID; the **attempted username and password are not logged** |
| Client addresses | Used only in memory for the login throttle; may appear in reverse-proxy logs, which are retained for a limited time (APA-3 context: up to 14 days) |
| Obstacle descriptions | Public to visitors once created; administrators must not put staff names, security details or personal data in them (the admin form says so, DESIGN.md 10.3) |
| Error and health output | Generic; no internals, no versions, no user existence |
| Data minimisation | Responses carry only what the app needs; administrative metadata (timestamps, versions) appears only on admin endpoints |
| Transport | HTTPS in every hosted environment; HSTS configured at the proxy |
| Retention | No personal data to retain. Obstacle rows persist until deleted by an administrator |

---

## 14. Failure handling

### 14.1 Server-side

| Failure | Behaviour |
|---|---|
| Database unreachable or pool exhausted | 503 `SERVICE_UNAVAILABLE` with `Retry-After`; health reports `DOWN`; no partial writes (each write is one transaction) |
| Database constraint violation | Mapped to 409 or 400 with a clean message; never a raw database error |
| Engine given invalid input | Mapped to 400 or 404 |
| Engine or mapper unexpected failure | 500 `INTERNAL_ERROR`; details only in logs with the request ID |
| Route computation exceeds its limit | Abandoned; 503 |
| Concurrent obstacle edits | 409 `CONFLICT`; the client re-reads and retries deliberately |
| Duplicate create (for example a repeated tap or retry) | The "one active obstacle per target" rule returns 409 instead of creating a duplicate |
| Missing floor-plan file | 404 for that file only; other endpoints are unaffected |
| Startup with invalid configuration | Fails fast with a clear message; does not start in a half-configured state |

### 14.2 Client-side

| Rule | Detail |
|---|---|
| Timeouts | Connect 5 seconds, receive 10 seconds; route requests 15 seconds (ARCHITECTURE.md 9.5) |
| Retries | **No automatic retries.** Failures show a Retry action. Safe reads and the read-only route request may be retried by the user at will |
| Obstacle creation | Never retried automatically; after an ambiguous failure the app re-reads the obstacle list before offering another attempt |
| Cancellation | The user may cancel a route request; the server may finish it, which is harmless |
| Unknown values | Unknown enumerations and extra fields are tolerated (Section 2.1) |

### 14.3 Mapping to app states (DESIGN.md Section 11)

| Outcome | App failure type | State shown |
|---|---|---|
| No connection, DNS or TLS failure | no connection | "Can't connect" with Retry |
| Timeout | timeout | The relevant error view with Retry; "taking longer" line after about 10 seconds |
| 400 | validation | Field-level errors where a form exists; otherwise "Something went wrong" |
| 401 on an admin call | unauthorized | "Signed out", return to sign-in |
| 401 on login | invalid credentials | "Username or password is incorrect" banner |
| 404 | not found | "No longer available" with a way back (for example a deleted obstacle, or a building removed) |
| 409 | conflict | "This changed while you were editing" with Reload |
| 429 | rate limited | "Try again shortly" |
| 500, 503 | server error | "Something went wrong" or "Can't connect" with Retry |
| 200 with `NO_ROUTE` | (not a failure) | The designed no-route state |
| 200 with `FOUND` and warnings | (not a failure) | Route plus warning banner |

---

## 15. Assumptions, decisions and document corrections

### Assumptions

| ID | Assumption |
|---|---|
| APA-1 | The default floor is the lowest-level floor containing a building entrance, else the lowest-level floor; it is computed by the server so every client behaves the same |
| APA-2 | Login throttle: 5 failed attempts per 15 minutes per client address; both values are configurable |
| APA-3 | Hosted proxy limits: about 60 requests per minute per address overall and 20 per minute for route requests; proxy logs retained up to 14 days. Values are tuned for a demo and a handful of users |
| APA-4 | Route computation time limit is 10 seconds server-side, within the 15-second client timeout |
| APA-5 | Destination search default limit 30, maximum 50 |
| APA-6 | Maximum request body 16 KB |
| APA-7 | Floor-plan files are cached for about one hour with a validator |
| APA-8 | Map responses (the largest, up to a few hundred items) are compressed when served; compression is enabled in the backend or at the proxy |

### Decisions requiring approval

| ID | Decision | Options | Recommended default |
|---|---|---|---|
| **APD-1** | `GET /api/rooms` | Build it; reserve it in the contract but do not build in the MVP | Reserve only. The floor map already includes rooms, so a separate endpoint is not required (PRD FR-07's searchable destinations are POIs) |
| **APD-2** | Obstacle creation payload | Client sends target type and ID, server derives building and sets status ACTIVE; client sends building and status as well | Server derives, so a client cannot create a building and target mismatch |
| **APD-3** | Concurrency control for obstacle updates | `version` field in the body; `If-Match` header with an entity tag | `version` in the body. Simpler for the mobile client and visible in the contract |
| **APD-4** | Request IDs | Always server-generated; accept a client-supplied ID | Server-generated only, so clients cannot inject log content |
| **APD-5** | Where rate limiting lives | Login throttle in the application plus general limits at the reverse proxy; all in the application | As stated. Avoids a rate-limiting dependency in the backend |
| **APD-6** | `defaultFloorId` in building details | Server computes it; the client chooses | Server computes it (APA-1) |
| **APD-7** | Extra endpoint: categories | Add `GET /api/points-of-interest/categories`; derive categories from search results in the app | Add it. DATABASE.md query Q14 and the category chips in DESIGN.md need it, and deriving from limited results would be wrong |
| **APD-8** | Floor-plan files served publicly | Public static route with hardening headers; authenticated route | Public. Plans are public map data and the app loads them without a token |

### Corrections to other documents when this is approved

| Document | Section | Change |
|---|---|---|
| ARCHITECTURE.md | 8.2 | Add the categories endpoint (APD-7); mark `GET /api/rooms` as reserved (APD-1) |
| ARCHITECTURE.md | 15.2 | Add `MIGRATION_DB_USER`, `MIGRATION_DB_PASSWORD`, `FLOORPLAN_DIR`, `LOGIN_MAX_FAILED_ATTEMPTS`, `LOGIN_ATTEMPT_WINDOW_MINUTES`, `TRUST_FORWARDED_HEADERS` |
| ARCHITECTURE.md | 13.1 | Add codes `METHOD_NOT_ALLOWED`, `UNSUPPORTED_MEDIA_TYPE`, `PAYLOAD_TOO_LARGE`, `TOO_MANY_REQUESTS`, `SERVICE_UNAVAILABLE` |
| DATABASE.md | 4.6 | The "room listing endpoint" wording becomes "floor map and the reserved rooms endpoint" |
| DESIGN.md | 11.2 | Add copy for rate-limited ("Try again shortly") and edit-conflict ("This changed while you were editing") states |
