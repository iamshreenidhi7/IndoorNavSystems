# IndoorNavSystems

Indoor navigation and path optimization platform. See `docs/` for the product, architecture, database and API documents, and `docs/AGENTS.md` for the working rules.

## Backend

Requires Java 21, Maven 3.9 or later, and Docker for the PostgreSQL-backed persistence tests.

### Local containers

Copy `.env.example` to `.env` and replace at least `POSTGRES_PASSWORD`. Then start the complete
local environment:

```bash
docker compose up --build
```

This starts PostgreSQL with a named volume and waits for it to become healthy before starting the
backend. Flyway applies the migrations and loads the demo seed data; Swagger UI is available at
`http://localhost:8080/swagger-ui.html`. The database is intentionally not published to the host.
Use `docker compose down` to stop the environment without deleting its database volume.

Run the tests from the repository root:

```bash
mvn test
```

`DatabaseIntegrityTest` starts a clean PostgreSQL 17 container, applies Flyway migrations and
the repeatable demo seeds, then validates the schema and data constraints. Docker must be
running for that test; the remaining unit and web tests do not require a database.

GitHub Actions runs the same backend and engine test suite, builds the backend container image,
validates the Compose configuration, and starts the complete stack through its database-backed
health endpoint for changes to the application, database, container, or governing-contract files.

### API collection

Import [the Postman collection](backend/postman/indoor-navigation.postman_collection.json) and
[the local environment](backend/postman/local.postman_environment.json) to exercise every API
endpoint and the five seeded demonstration scenarios. Start the seeded local stack first, then
follow the collection description; do not commit a real administrator password.

To start the backend you need PostgreSQL and the migrations in `database/migrations`. Copy `.env.example` to `.env`, fill in the values and export them into your shell, then run:

```bash
mvn -pl backend spring-boot:run
```

The `dev` profile is the default. It serves Swagger UI at `/swagger-ui.html`. Administrator sign-in stays disabled until `ADMIN_PASSWORD_HASH` is set.
