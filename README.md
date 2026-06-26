# IncidentPulse

Problem:
Imaging your api or a critical service goes down. 
if you can not detect the issue, creat an incident, and assign on call engineer quickly. The business will lose thousands or even
millions of dollars.


Solution:
Incident Pulse will allow client, admins, leaders, or non-on call engineer to create incident immediately when something goes down.
The system will automatically assign the current on-call engineer and send an email notification, so issues can be addressed as fast as possible and downtime is minimized.  

Flow:
client, admin, lead, other engineer create an incident (
monitor, and autodetect in the future
) -> system will automatically assign on-call engineer and send an email notification 
-> on-call engineer will log in and change status from open to investigating,
multiple engineer can solve the incident but if one engineer post completely, no one can not change the status 
excluding admin, and lead team. 

Another feature: admin, leader, and engineer can get all incidents history to help them investigate a root to cause these incidents.
Java email sender and using algorithm to calculate who will on-call engineer.

ERD diagram:
![img.png](img.png)


System Design:
![alt text](image.png)


Email Send in Laptop:
![alt text](image-1.png)
Email Send in Phone:
![alt text](emailinphone.jpg)

---

## Tech stack

| Layer | Choice |
|-------|--------|
| Backend | Java 21, Spring Boot 3.5, PostgreSQL, Redis, Flyway |
| Frontend | React 19, TypeScript, Vite |
| Real-time | STOMP over WebSocket (SockJS) |
| Auth | JWT (access + refresh) |
| CI | GitHub Actions (`mvn verify`) |

---

## Run locally

Three options — pick one.

### Option A — nginx dev proxy (recommended, single origin)

Everything on **http://localhost** via nginx; no CORS issues.

**All-in Docker** (db + redis + app + Vite + nginx):

```bash
docker compose -f docker-compose.dev.yml up -d --build
# open http://localhost   ← use this (nginx on port 80)
# NOT http://localhost:5173 alone unless you only want the raw Vite UI
```

> **Important:** With Docker Compose, the main URL is **http://localhost** (port **80**).  
> Port **5173** is the Vite dev server inside Docker — it is optional for debugging.  
> If `:5173` fails, you were likely hitting a port that was not published to your machine.

**Hybrid** (Spring Boot + Vite on host, nginx in Docker):

```bash
# Terminal 1
docker compose up -d db redis
./mvnw spring-boot:run

# Terminal 2
cd frontend && npm install && npm run dev

# Terminal 3
docker compose -f docker-compose.nginx-dev.local.yml up -d
# open http://localhost:8888
```

nginx routes:

| Path | Target |
|------|--------|
| `/api/*` | Spring Boot `:8080` |
| `/ws` | Spring Boot (SockJS + STOMP) |
| `/swagger-ui/*`, `/actuator/*` | Spring Boot |
| everything else | Vite `:5173` (dev) or static build (prod) |

Config files: [`nginx/nginx.dev.conf`](nginx/nginx.dev.conf), [`nginx/nginx.dev.local.conf`](nginx/nginx.dev.local.conf), [`nginx/nginx.conf`](nginx/nginx.conf).

### Option B — separate ports (Vite proxy)

```bash
docker compose up -d db redis
./mvnw spring-boot:run

cd frontend && npm install && npm run dev
# open http://localhost:5173  (Vite proxies /api and /ws to :8080)
```

### Option C — backend only

```bash
docker compose up -d db redis
./mvnw spring-boot:run
```

Default admin (dev): `admin` / `admin123`

Swagger UI: http://localhost:8080/swagger-ui.html (or http://localhost/swagger-ui.html when using nginx)

### CORS

When using nginx as a single entry point, the browser sees one origin — CORS is not needed. If you run frontend and backend on different ports (Option B), set `APP_CORS_ORIGINS` (default includes `http://localhost:5173`).

---

## Frontend pages

| Route | Purpose |
|-------|---------|
| `/login` | JWT sign-in |
| `/incidents` | List + filter incidents, **live WebSocket updates** |
| `/incidents/new` | Create incident (auto-assigns on-call) |
| `/incidents/:id` | Detail, audit history, status transitions |
| `/my-incident` | Current assignment for the logged-in engineer |
| `/integrations` | Webhook docs + admin simulate form |
| `/admin/users` | User CRUD (ADMIN) |
| `/admin/on-call` | On-call shift management |

Theme: black & white ops dashboard.

---

## Webhook + WebSocket flow

1. **External monitor** → `POST /api/v1/webhook/alert` with `X-API-Key` → incident created
2. **Admin UI simulate** → `POST /api/v1/webhook/simulate` with JWT → same ingestion path, no API key in browser
3. **All clients** subscribed to `/topic/incidents` receive live `CREATED` / `STATUS_CHANGED` events

---

## Deploy

Production uses **nginx on port 80** — it serves the built React app and proxies `/api` and `/ws` to Spring Boot internally.

```bash
cp .env.example .env   # fill in secrets
docker compose -f docker-compose.prod.yml up -d --build
# open http://<host>/   (not :8080)
curl http://<host>/actuator/health
```

See [`docker-compose.prod.yml`](docker-compose.prod.yml), [`.env.example`](.env.example), and [`scripts/deploy.sh`](scripts/deploy.sh) for AWS EC2 single-host deployment.

On EC2, open inbound port **80** (and 22 for SSH) in the security group instead of 8080.

---

## API docs

Interactive docs at `/swagger-ui.html` when the backend is running.
