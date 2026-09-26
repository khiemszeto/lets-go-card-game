# lets-go-card-game


Daily development loop

Three terminals, all left running.

**Terminal 1 — database:**
```bash
cd ~/card-game
docker compose up -d
```

MySQL takes a few seconds to accept connections after the container starts.
`docker compose ps` should say `healthy` before you start the backend.

**Terminal 2 — backend:**
```bash
cd ~/card-game/backend
export JWT_SECRET='(Base64-encoded HMAC key — never commit this)'
./mvnw spring-boot:run -Dspring-boot.run.profiles=calvin-dev
```

`jwt.secret` is required via env `JWT_SECRET` (see `application.properties`).  
Optional: copy `application-local.properties.example` → `application-local.properties` (gitignored) and run with profile `local`.

Use your own `application-<name>-dev.properties` profile. For the Docker setup:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/player_db
spring.datasource.username=root
spring.datasource.password=
```

Tables are created automatically due to JPA.

Requires a JDK 25 entry in `~/.m2/toolchains.xml`, otherwise the build fails with
"Cannot find matching toolchain definitions for jdk [version 25]".

**Terminal 3 — frontend:**
```bash
cd ~/card-game/frontend-rework/thirteencards
npm run dev
```

**Work at `localhost:5173`.**


Frontend edits hot-reload instantly. Backend edits trigger a DevTools restart
(a few seconds, automatic).

---

## Secrets (do not commit)

- `JWT_SECRET` — required at runtime (local export, or `.env` for Docker)
- `APP_COOKIE_SECURE=true` on HTTPS (Lightsail). Local HTTP can stay `false`
- Never put real secrets in `application.properties` or git; use env / gitignored `.env` / `application-local.properties`
- Copy `env.example` → `.env` for Compose. Do not commit `.env`

If a secret was ever committed or pasted into a chat, rotate it on the server and treat the old value as burned.

---

## Tests

```bash
cd ~/card-game/backend
./mvnw test                                        # unit + integration (needs Docker)

# E2E: start db + frontend, then backend with a fixed deal
docker compose up -d                                         # from ~/card-game
npm run dev                                                  # from ~/card-game/frontend-rework/thirteencards
export JWT_SECRET=$(openssl rand -base64 32)                                         # from ~/card-game/backend
APP_DECK_SHUFFLE=false ./mvnw spring-boot:run -Dspring-boot.run.profiles=calvin-dev   # from ~/card-game/backend
HEADLESS=false ./mvnw test -Dgroups=e2e -DexcludedGroups=none                        # from ~/card-game/backend, separate terminal
```

---

## Production build (monolith)

```bash
# 1. Build frontend to static files
cd ~/card-game/frontend-rework/thirteencards
npm run build

# 2. Copy into Spring's static folder (clear old hashed files first)
rm -rf ../../backend/src/main/resources/static/*
mkdir -p ../../backend/src/main/resources/static
cp -r dist/* ../../backend/src/main/resources/static/

# 3. Package the jar
cd ../../backend
./mvnw -DskipTests clean package

# 4. Run it (demo profile + secrets from env)
export JWT_SECRET='...'
export SPRING_PROFILES_ACTIVE=demo
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

The `Dockerfile` does the same steps (frontend build → static → JAR) inside the image.

---

## Docker (local)

```bash
cp env.example .env
# set JWT_SECRET (openssl rand -base64 32)
docker compose up -d --build
```

App: `http://127.0.0.1:8080`. MySQL stays on `127.0.0.1:3306`.  
`APP_COOKIE_SECURE` defaults to `false` when unset (see `docker-compose.yml`).

---

## CI / CD

| Workflow | When | What |
|---|---|---|
| CI | pull request, and push to `main` | frontend build + backend unit/integration tests (E2E excluded) |
| Deploy | CI on `main` finishes **successfully** | build image → push GHCR → SSH Lightsail `compose pull` + `up` |

Merge to `main` does not deploy if CI fails.

Lightsail `.env` (not in git) should include `JWT_SECRET`, `APP_COOKIE_SECURE=true`, `APP_IMAGE=ghcr.io/khiemszeto/thirteencards:latest`, and `COMPOSE_PROJECT_NAME=app` so Compose keeps the existing MySQL volume `app_db-data`.

Manual update on the server if CD is down:

```bash
cd ~/lets-go-card-game
git pull origin main
docker compose pull app
docker compose up -d app
```

Do not run `docker compose down -v` — that deletes database volumes.

---

## Rollback (Lightsail)

Images are tagged `latest` and the git commit SHA.

```bash
cd ~/lets-go-card-game
# point at a previous Actions SHA (full sha from the green deploy)
# edit .env: APP_IMAGE=ghcr.io/khiemszeto/thirteencards:<sha>
docker compose pull app
docker compose up -d app
```

Or revert the bad commit on `main` and let CI + Deploy publish a new `latest`.
