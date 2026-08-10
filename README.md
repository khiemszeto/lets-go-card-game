# lets-go-card-game


Daily development loop

Two terminals, both left running.

**Terminal 1 — backend:**
```bash
cd ~/card-game/backend
./mvnw spring-boot:run
```

**Terminal 2 — frontend:**
```bash
cd ~/card-game/frontend
npm run dev
```

**Work at `localhost:5173`.**


Frontend edits hot-reload instantly. Backend edits trigger a DevTools restart
(a few seconds, automatic).

Verify the full path end to end — browser console at 5173:

```js
await fetch('/api/tasks').then(r => r.json())
```

An empty array proves browser → Vite → Spring → back.

---

## Production build (monolith)

```bash
# 1. Build frontend to static files
cd ~/card-game/frontend
npm run build

# 2. Copy into Spring's static folder (clear old hashed files first)
rm -rf ../backend/src/main/resources/static/*
cp -r dist/* ../backend/src/main/resources/static/

# 3. Package the jar
cd ../backend
./mvnw clean package

# 4. Run it
java25 -jar target/backend-0.0.1-SNAPSHOT.jar
```