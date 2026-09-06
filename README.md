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

- `JWT_SECRET` — required at runtime (Lightsail systemd / local export)
- `APP_COOKIE_SECURE=true` on HTTPS demos
- Never put real secrets in `application.properties`; use env or gitignored `application-local.properties`

If a secret was ever committed, rotate it on the server and treat the old value as burned.

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
