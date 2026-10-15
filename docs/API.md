# Indoor Navigation REST API Contract

**Status:** Contract baseline 1.0  
**Base path:** `/api/v1`  
**Transport:** HTTPS in deployed environments; JSON request and response bodies  
**Canonical source:** This document defines the REST contract consumed by `mobile-app` and implemented by `backend`.

## 1. Contract conventions

- All paths are relative to `/api/v1` unless shown as a full path.
- JSON field names use `camelCase`; database names use `snake_case`.
- IDs are positive integer values represented as JSON numbers. The database contract uses `BIGINT` identity IDs.
- Timestamps use ISO 8601 / RFC 3339 UTC values, for example `2026-01-01T12:00:00Z`.
- The backend returns DTOs, never persistence entities.
- Collections are returned as JSON arrays. No pagination contract is defined for this baseline.
- `null` means a value is absent or not applicable; clients must not infer fields that are not documented here.
- The client resolves a selected Room to its linked Node before requesting a route. The route endpoint accepts node IDs, not room IDs.

## 2. Endpoint index

| Method | Path | Purpose | Success |
|---|---|---|---|
| `GET` | `/api/v1/buildings` | List available buildings | `200 OK` |
| `GET` | `/api/v1/buildings/{buildingId}/floors` | List floors in a building | `200 OK` |
| `GET` | `/api/v1/buildings/{buildingId}/floors/{floorId}/map` | Retrieve renderable floor data | `200 OK` |
| `GET` | `/api/v1/buildings/{buildingId}/rooms?query={query}` | Search rooms by name or number | `200 OK` |
| `POST` | `/api/v1/routes` | Compute a route | `200 OK` |
| `GET` | `/api/v1/obstacles?buildingId={id}` | List active obstacles in a building | `200 OK` |
| `POST` | `/api/v1/obstacles` | Report/create an obstacle | `201 Created` |
| `DELETE` | `/api/v1/obstacles/{obstacleId}` | Clear an active obstacle | `204 No Content` |

## 3. Buildings

### `GET /api/v1/buildings`

Returns the buildings available to the client.

**Response — `200 OK`**

```json
[
  {
    "id": 1,
    "name": "Main Building",
    "address": "Example address",
    "description": "Main campus building"
  }
]
```

`address` and `description` may be `null` when not supplied. The response is an array; an empty array means no buildings are available.

## 4. Floors

### `GET /api/v1/buildings/{buildingId}/floors`

Returns floors belonging to the specified building, ordered by `level` ascending.

**Response — `200 OK`**

```json
[
  {
    "id": 11,
    "buildingId": 1,
    "level": 0,
    "label": "Ground Floor",
    "mapAssetUrl": null
  },
  {
    "id": 12,
    "buildingId": 1,
    "level": 1,
    "label": "Level 1",
    "mapAssetUrl": "https://example.invalid/maps/main-level-1.svg"
  }
]
```

`level` is the building's numeric floor level and may be negative for basement floors. `label` is the display label. `mapAssetUrl` is optional; map geometry from the map endpoint remains the API's structured data contract.

**Errors:** `404 Not Found` if `buildingId` does not exist.

## 5. Floor map

### `GET /api/v1/buildings/{buildingId}/floors/{floorId}/map`

Returns the floor's rooms, navigable nodes and same-floor edges needed to render the map. Cross-floor connections are represented in the graph by edges with a connector type; route results identify floor transitions.

**Response — `200 OK`**

```json
{
  "buildingId": 1,
  "floor": {
    "id": 11,
    "level": 0,
    "label": "Ground Floor",
    "mapAssetUrl": null
  },
  "rooms": [
    {
      "id": 501,
      "floorId": 11,
      "name": "Admissions",
      "number": "G-12",
      "category": "Office",
      "nodeId": 101,
      "mapX": 12.0,
      "mapY": 8.0
    }
  ],
  "nodes": [
    {
      "id": 101,
      "floorId": 11,
      "x": 12.0,
      "y": 8.0,
      "type": "ROOM_ENTRANCE"
    },
    {
      "id": 102,
      "floorId": 11,
      "x": 18.0,
      "y": 8.0,
      "type": "CORRIDOR"
    }
  ],
  "edges": [
    {
      "id": 9001,
      "fromNodeId": 101,
      "toNodeId": 102,
      "distanceMeters": 6.0,
      "accessible": true,
      "bidirectional": true,
      "connectorType": null
    }
  ]
}
```

Field notes:
- `rooms[].nodeId` identifies the navigable node associated with that room.
- `mapX` and `mapY` are optional room-label/map positions. They are not used as routing endpoints; route calculations use the linked node.
- `x` and `y` are floor-plan coordinate units defined consistently for a floor. The project must use the same coordinate mapping for map rendering and graph data.
- `type` is one of `ROOM_ENTRANCE`, `CORRIDOR`, `DOOR`, `LANDING`, or `LANDMARK`.
- `distanceMeters` is the persisted/engine edge weight expressed in metres.
- `connectorType` is `null`, `STAIRS`, `ELEVATOR`, or `RAMP`. A connector edge links nodes on different floors.
- `bidirectional` indicates whether the edge may be traversed in both directions.

**Errors:** `404 Not Found` if the building or floor does not exist, or if the floor does not belong to the building.

## 6. Room search

### `GET /api/v1/buildings/{buildingId}/rooms?query={query}`

Searches room names and numbers within the selected building. `query` is required after trimming and must contain at least one non-whitespace character. Matching behavior is case-insensitive. Results are ordered by room name, then number.

**Example:** `GET /api/v1/buildings/1/rooms?query=admissions`

**Response — `200 OK`**

```json
[
  {
    "id": 501,
    "buildingId": 1,
    "floorId": 11,
    "name": "Admissions",
    "number": "G-12",
    "category": "Office",
    "nodeId": 101,
    "mapX": 12.0,
    "mapY": 8.0
  }
]
```

`number`, `category`, `mapX`, and `mapY` may be `null`. An empty array means there are no matches.

**Errors:** `400 Bad Request` for a missing/blank query; `404 Not Found` if the building does not exist.

## 7. Route calculation

### `POST /api/v1/routes`

Computes the shortest usable route over the building graph, accounting for active obstacles and the requested accessibility constraint.

**Request body**

```json
{
  "buildingId": 1,
  "startNodeId": 101,
  "endNodeId": 342,
  "accessibleOnly": false
}
```

Validation:
- `buildingId`, `startNodeId`, and `endNodeId` are required positive IDs.
- `accessibleOnly` is optional and defaults to `false`.
- Both nodes must belong to the specified building.
- When `accessibleOnly` is `true`, edges marked `accessible: false` and `STAIRS` connector edges are excluded.
- Active obstacles block their referenced node or edge.
- A start node equal to the end node is a valid request and returns a one-node path with zero distance and estimated time.

**Response — `200 OK` when a route is found**

```json
{
  "found": true,
  "distanceMeters": 128.5,
  "estimatedSeconds": 110,
  "path": [
    { "nodeId": 101, "floorId": 11, "x": 12.0, "y": 8.0, "type": "ROOM_ENTRANCE" },
    { "nodeId": 124, "floorId": 11, "x": 30.0, "y": 8.0, "type": "LANDING" },
    { "nodeId": 342, "floorId": 13, "x": 30.0, "y": 8.0, "type": "LANDING" }
  ],
  "steps": [
    "Walk along the corridor to the elevator",
    "Take the elevator to floor 2",
    "Continue to the destination"
  ]
}
```

`path` is ordered from start to destination. `steps` is an ordered list of human-readable instructions. `estimatedSeconds` is an integer estimate. The route result uses the `found` field for successful responses; a request that cannot be routed uses `422` rather than returning `found: false`.

**Errors:**
- `400 Bad Request` — missing/invalid IDs or invalid body.
- `404 Not Found` — building or node does not exist, or a node does not belong to the specified building.
- `422 Unprocessable Entity` — the request is valid but no route satisfies the graph, obstacle and accessibility constraints.

## 8. Obstacles

An obstacle blocks exactly one graph target: either a node or an edge. Only active obstacles are returned by the list endpoint. Clearing an obstacle changes its state; it does not physically delete the row.

### `GET /api/v1/obstacles?buildingId={id}`

`buildingId` is required. Returns active obstacles that affect the specified building.

**Response — `200 OK`**

```json
[
  {
    "id": 7001,
    "buildingId": 1,
    "nodeId": null,
    "edgeId": 9001,
    "reason": "Corridor temporarily closed",
    "active": true,
    "createdAt": "2026-01-01T12:00:00Z",
    "clearedAt": null
  }
]
```

**Errors:** `400 Bad Request` if `buildingId` is missing or invalid; `404 Not Found` if the building does not exist.

### `POST /api/v1/obstacles`

Creates/reports an active obstacle. The caller must provide exactly one of `nodeId` or `edgeId`.

**Request body — block an edge**

```json
{
  "buildingId": 1,
  "nodeId": null,
  "edgeId": 9001,
  "reason": "Corridor temporarily closed"
}
```

**Response — `201 Created`**

```json
{
  "id": 7001,
  "buildingId": 1,
  "nodeId": null,
  "edgeId": 9001,
  "reason": "Corridor temporarily closed",
  "active": true,
  "createdAt": "2026-01-01T12:00:00Z",
  "clearedAt": null
}
```

`reason` is required and must not be blank. The referenced node/edge must belong to the specified building. Creating an obstacle invalidates any cached graph for that building.

**Errors:** `400 Bad Request` for invalid input or when both/neither target IDs are supplied; `404 Not Found` if the building or referenced target does not exist or belongs to another building.

### `DELETE /api/v1/obstacles/{obstacleId}`

Clears an obstacle by setting `active` to `false` and recording `clearedAt`. The operation is idempotent: clearing an already-cleared obstacle still returns `204 No Content`.

**Success:** `204 No Content`, with no response body. Clearing an obstacle invalidates the affected building's cached graph.

**Errors:** `404 Not Found` if the obstacle ID does not exist.

## 9. Error format

All API errors use the same JSON object shape:

```json
{
  "timestamp": "2026-01-01T12:00:00Z",
  "status": 422,
  "error": "ROUTE_NOT_FOUND",
  "message": "No route available with the given constraints.",
  "path": "/api/v1/routes"
}
```

| HTTP status | Error code examples | Meaning |
|---|---|---|
| `400` | `VALIDATION_ERROR`, `INVALID_REQUEST` | Request body, path parameter or query parameter is invalid |
| `404` | `BUILDING_NOT_FOUND`, `FLOOR_NOT_FOUND`, `NODE_NOT_FOUND`, `OBSTACLE_NOT_FOUND` | Requested resource or referenced graph item does not exist in the required scope |
| `422` | `ROUTE_NOT_FOUND` | Request is valid but no usable route exists |
| `500` | `INTERNAL_ERROR` | Unexpected server failure; do not expose stack traces or internal details |

`timestamp` is a UTC timestamp. `status` matches the HTTP status. `error` is a stable machine-readable code. `message` is safe for display/logging and must not include secrets. `path` is the request path, including `/api/v1`.

## 10. Caching, security and compatibility

- Graph data may be cached per building. Any node, edge or obstacle change must invalidate the affected building's cached graph.
- HTTPS is required outside local development. CORS must be restricted to configured client origins.
- The PRD identifies authentication for administrative obstacle operations as a recommended assumption. The authentication mechanism is not fixed by this contract; deployments must protect obstacle write/clear operations before production exposure.
- Breaking changes require a contract-change pull request, updates to this document and affected docs/tests, and review by each affected module owner. Add a new API version for incompatible changes.
