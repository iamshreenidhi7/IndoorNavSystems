# Demo and recovery checklist

| Field | Value |
|---|---|
| Owner | Person 4 (database and DevOps), with route outcomes confirmed by Person 3 and Person 2 |
| Scope | Seeded demo scenarios, local environment smoke test, and hosted restore drill |
| Source requirements | PRD.md 8.3, DATABASE.md 5.3 and 13.4, ARCHITECTURE.md 7.6 and 17.2 |

## 1. Local environment smoke test

Run this on a fresh clone with Docker running.

1. Copy `.env.example` to `.env` and replace `POSTGRES_PASSWORD` with a local-only value.
2. Run `docker compose up --build`.
3. Wait for the backend to start after the database health check succeeds.
4. Open `http://localhost:8080/swagger-ui.html` and verify the API documentation loads.
5. Request `GET /api/health`; the response reports the database as `UP`.
6. Request `GET /api/buildings`; the response includes Sample Mall and Sample Library.
7. Stop the stack with `docker compose down`; do not remove the named volume during this smoke test.
8. Start it again with `docker compose up`; confirm the backend starts cleanly and the building list remains available.

Expected result: Flyway applies the versioned migrations once, repeatable seeds are safe to re-run,
and the named volume preserves local database state between normal stops.

## 2. Seeded route-scenario checklist

Run these scenarios through the mobile client or the route API once route orchestration is available.
The IDs below are stable seed references, not visitor-facing values.

| Scenario | Setup | Expected result |
|---|---|---|
| S1: blocked edge detour | Route through the Sample Mall ground floor, then create an active obstacle on edge `2123`. | A different, longer route remains available through the loop. |
| S2: shortest versus fastest | Route from ground-floor stairs node `1005` to level-one stairs node `1105`, comparing `SHORTEST` and `FASTEST`. | The shortest route uses stairs; the fastest route uses the elevator alternative. |
| S3: accessible differs | Route from step-free ground node `1001` to step-free level-one node `1102`, comparing `SHORTEST` and `ACCESSIBLE`. | The shortest route uses stairs; accessible routing excludes the non-accessible stair landings and uses the elevator. |
| S4: broken elevator | Create active obstacles on elevator edges `2002` and `2004`; request an accessible route from ground to level two. | Accessible routing returns no route; shortest routing can still use stairs. |
| S5: unreachable destination | Create an active obstacle on edge `2120`, the only connection to node `1020`. | Quiet corner is unreachable and the result is a normal no-route response. |

After each scenario, resolve or delete the obstacle so the next scenario starts from the clean seeded graph.
The repeatable seed scripts must never create or modify runtime obstacle rows.

## 3. Hosted restore drill

Do this once before the final demonstration and after changing hosted backup tooling. Do not run it
against the live database.

1. Take an on-demand logical backup using the hosted platform’s approved backup mechanism.
2. Restore it into a separate scratch PostgreSQL database with restricted access.
3. Start one backend instance against the restored database, with the same intended seed setting.
4. Confirm Flyway starts without modifying applied migrations and Hibernate schema validation succeeds.
5. Run `DatabaseIntegrityTest` in an environment where Docker/Testcontainers is available.
6. Check `GET /api/health`, the building list, and the obstacle list against the restored instance.
7. Record the backup timestamp, restore completion time, test result, and any follow-up action in the team’s demo notes.
8. Delete the scratch database through the hosting platform’s approved, recoverable procedure.

Expected result: the database is restored within the one-hour target and loses no more than the
documented 24-hour recovery point. Never commit backup archives, credentials, or restore output.
