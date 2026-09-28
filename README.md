# DPDMS — Rushinga Provincial Disaster Monitoring and Management System

A Spring Boot micro-service system that lets ward-level officers record disaster
incidents, lets provincial supervisors approve them, pushes out email/WhatsApp
alerts when an incident crosses a danger threshold, and gives the Provincial
Office a live dashboard, an interactive map and downloadable reports.

Built for the 2026 Object-Oriented Programming group assignment
(HCS201 / HCC201 / HAI201), University of Zimbabwe.

---

## 1. Table of contents

1. [Table of contents](#1-table-of-contents)
2. [What the system does](#2-what-the-system-does)
3. [Architecture at a glance](#3-architecture-at-a-glance)
4. [Technology choices and why](#4-technology-choices-and-why)
5. [Prerequisites](#5-prerequisites)
6. [First-time setup](#6-first-time-setup)
7. [Building](#7-building)
8. [Running the system](#8-running-the-system)
9. [Demo accounts](#9-demo-accounts)
10. [Walkthrough: the full incident lifecycle](#10-walkthrough-the-full-incident-lifecycle)
11. [Roles and access control](#11-roles-and-access-control)
12. [How hazard + ward scoping is actually enforced](#12-how-hazard--ward-scoping-is-actually-enforced)
13. [The approval workflow](#13-the-approval-workflow)
14. [Hazard indicators and alert thresholds](#14-hazard-indicators-and-alert-thresholds)
15. [How alerts are dispatched](#15-how-alerts-are-dispatched)
16. [Reports](#16-reports)
17. [Dashboard and map](#17-dashboard-and-map)
18. [API documentation](#18-api-documentation)
19. [Testing](#19-testing)
20. [Project layout](#20-project-layout)
21. [Configuration reference](#21-configuration-reference)
22. [Troubleshooting](#22-troubleshooting)
23. [Team and division of work](#23-team-and-division-of-work)

---

## 2. What the system does

Rushinga District's Provincial Office monitors five hazard types. Each hazard is
handled by its own micro-service with its own database, its own five indicators
and its own supervisor:

| Hazard | Service | Port | Database |
|---|---|---|---|
| Flood | `flood-service` | 8082 | `dpdms_flood` |
| Drought | `drought-service` | 8083 | `dpdms_drought` |
| Veld / structural fire | `fire-service` | 8084 | `dpdms_fire` |
| Zoonotic disease | `zoonotic-disease-service` | 8085 | `dpdms_zoonotic` |
| Mining accident | `mining-accident-service` | 8086 | `dpdms_mining` |

Supporting services:

| Service | Port | Purpose | Database |
|---|---|---|---|
| `discovery-service` | 8761 | Eureka service registry | — |
| `gateway` | 8080 | Single entry point, routing, combined Swagger UI, circuit breakers | — |
| `auth-service` | 8081 | Login, JWT issuing, user administration | `dpdms_auth` |
| `alert-service` | 8087 | Consumes alert events, sends email + WhatsApp, keeps a delivery log | `dpdms_alert` |
| `report-service` | 8088 | PDF / DOCX / XLSX / CSV report generation | — (reads via API) |
| `dashboard-service` | 8089 | Aggregates statistics and map points across all hazards | — (reads via API) |
| `web-ui` | 8090 | Thymeleaf front end that users actually log into | — |

Everything a user touches goes through **http://localhost:8090** (front end) or
**http://localhost:8080** (API gateway). No service is called directly by a user.

---

## 3. Architecture at a glance

```
                      ┌──────────────────────────┐
  Browser  ─────────► │  web-ui  (Thymeleaf)     │  :8090
                      │  holds the JWT in the    │
                      │  server-side session     │
                      └───────────┬──────────────┘
                                  │ REST + Bearer token
                                  ▼
                      ┌──────────────────────────┐
                      │  gateway (Spring Cloud)  │  :8080
                      └───────────┬──────────────┘
                                  │ lb:// (resolved through Eureka)
        ┌────────────┬────────────┼─────────────┬─────────────┬──────────────┐
        ▼            ▼            ▼             ▼             ▼              ▼
   auth-service  flood  drought  fire  zoonotic  mining   report-service  dashboard-service
      :8081      :8082  :8083   :8084   :8085    :8086        :8088            :8089
        │           └──────┬───────┴────────┴────────┘            │                │
        │                  │ publishes AlertEvent                 └──── reads ─────┘
        │                  ▼        (RabbitMQ)                     the hazard APIs
        │          ┌───────────────┐
        │          │ alert-service │ :8087  ──► SMTP (email)  ──► WhatsApp Cloud API
        │          └───────┬───────┘
        └── /internal ─────┘  (recipient lookup, X-Internal-Api-Key)

   discovery-service (Eureka) :8761  — every service registers here
```

A fuller set of diagrams (component, sequence, state machine, deployment) lives in
[`docs/architecture.md`](docs/architecture.md).

---

## 4. Technology choices and why

| Choice | Why |
|---|---|
| **Java 21 + Spring Boot 3.3.5** | Current LTS; records, sealed types and pattern-matched `switch` are used throughout, which keeps the OOP model clean. |
| **Spring Cloud 2023.0.3** (Eureka + Gateway) | The brief asks for service discovery and a gateway; this is the reference implementation for Spring Boot 3. |
| **MySQL 8, one schema per service** | Micro-services must not share tables. Each hazard service owns its schema, so a change to the flood table cannot break drought. |
| **Flyway** | Versioned, repeatable schema migrations. `ddl-auto` is never used — the marker can read the exact SQL in `src/main/resources/db/migration`. |
| **JWT (HS384, jjwt 0.12.6)** | Stateless authentication; the token carries `role`, `hazard` and `ward` claims so every service can enforce scope without calling back to auth-service. |
| **RabbitMQ** | Alerts must be asynchronous — a slow SMTP server must never block a recorder's save. The hazard service publishes and returns immediately. |
| **Thymeleaf** | Chosen over Angular/React deliberately — see below. |
| **Apache POI + OpenPDF + Commons CSV** | Native generation of XLSX, DOCX, PDF and CSV without any external converter. |
| **springdoc-openapi** | Auto-generated OpenAPI 3 docs, aggregated into one Swagger UI at the gateway. |
| **Leaflet + OpenStreetMap** | Free, no API key, works offline-ish for a classroom demo, and satisfies "interactive map". |

### Why Thymeleaf and not Angular/React/Vue

The brief allows any of the four. Thymeleaf was chosen because:

1. **The JWT never reaches the browser.** `web-ui` keeps the token in the
   server-side HTTP session (`SessionUser`) and attaches it to outgoing calls
   itself. With a SPA the token would sit in `localStorage`, which is the
   classic XSS token-theft hole. For a system holding disaster and casualty
   data that matters.
2. **One build, one artefact.** No separate `npm` toolchain, no CORS
   configuration, no second deployment. `mvn clean install` produces everything.
3. **Server-side rendering makes the access rules visible.** The same
   `HazardAccessPolicy` that guards the API also decides which buttons render,
   so the UI and the backend can never disagree.
4. **It is still a real client.** The dashboard and map are dynamic: the map is
   Leaflet driven by `fetch()` against `/map-data`, and the dashboard charts
   refresh from JSON.

The UI is a convenience layer only. Every rule is enforced again in the backend
(see §12), so bypassing the UI with curl or Postman gains an attacker nothing.

---

## 5. Prerequisites

| Tool | Version | Notes |
|---|---|---|
| JDK | 21 (Temurin) | `java -version` must report 21. JDK 17 also works. |
| Maven | 3.9+ | Or use the IntelliJ-bundled Maven. |
| MySQL | 8.0 | Must be running and reachable on port 3306. |
| Docker Desktop | any recent | Only for RabbitMQ + Mailpit. Optional — see §8.4. |
| IntelliJ IDEA | Community is fine | Any IDE works; the project is plain Maven. |

---

## 6. First-time setup

### 6.1 Create the databases

Open MySQL Workbench (or `mysql -u root -p`) and run
`infrastructure/mysql/01-create-databases.sql`.

**Before running it, change `replace-me` in that file** to the password you want
the `dpdms` user to have. It creates seven schemas and grants the `dpdms` user
access to all of them.

### 6.2 Create your `.env`

```powershell
copy .env.example .env
```

Then open `.env` and set, at minimum:

```properties
JWT_SECRET=<48+ random characters — openssl rand -base64 48>
INTERNAL_API_KEY=<another long random string>
DB_PASSWORD=<the same password you put in the SQL script>
```

`.env` is git-ignored and must **never** be committed. Every service imports it
automatically via `spring.config.import=optional:file:.env[.properties]`, so
there is nothing else to wire up.

> If you delete or overwrite `.env` by accident, just copy `.env.example` again
> and re-enter the three values above.

### 6.3 Start RabbitMQ and Mailpit

```powershell
docker compose up -d
```

- RabbitMQ management UI: http://localhost:15672 (guest / guest)
- Mailpit inbox (where alert emails land): http://localhost:8025

If you cannot run Docker, see §8.4 — the system degrades gracefully.

---

## 7. Building

From the project root:

```powershell
mvn clean install
```

This compiles all 11 modules, runs every test, and installs `common` into your
local repository so the other services can use it. First run downloads
dependencies and takes a few minutes.

To build without running tests (faster when you just want to demo):

```powershell
mvn clean install -DskipTests
```

---

## 8. Running the system

### 8.1 Startup order

Discovery and the gateway must come first; the rest can start in any order, but
this order is the one to use in a demo because each service is up before
anything calls it:

```
1. discovery-service   :8761   ← wait until http://localhost:8761 shows the Eureka page
2. gateway             :8080
3. auth-service        :8081   ← seeds the demo users on first start
4. flood-service       :8082
5. drought-service     :8083
6. fire-service        :8084
7. zoonotic-disease-service :8085
8. mining-accident-service  :8086
9. alert-service       :8087
10. report-service     :8088
11. dashboard-service  :8089
12. web-ui             :8090   ← open http://localhost:8090
```

Give discovery-service about 20 seconds before starting the others, and allow
up to 30 seconds after the last service starts for Eureka registration to
propagate. Until it does, the gateway answers `503 Service Unavailable` — that
is normal, not a bug.

### 8.2 The easy way

```powershell
.\start-all.ps1          # Windows PowerShell — opens one window per service
```

```bash
./start-all.sh           # Linux/macOS — runs in the background, logs to ./logs
```

Stop everything with `.\stop-all.ps1` (Windows) or `./stop-all.sh`.

### 8.3 From IntelliJ

Right-click each `*Application.java` → *Run*, in the order above. IntelliJ will
create a run configuration for each; you can then use the *Services* tool window
(Alt+8) to start them all with one click next time.

### 8.4 Running without Docker

The system is designed to survive missing infrastructure:

- **No RabbitMQ**: hazard services log `Alert publishing skipped` and carry on.
  Incidents still save, approve and report. Only the alert e-mail is lost.
- **No SMTP server**: set `MAIL_ENABLED=false` in `.env`. The alert-service
  writes the message to its log and records `SKIPPED` in the delivery log
  instead of failing.
- **WhatsApp**: off by default (`WHATSAPP_ENABLED=false`). When off, the message
  body is logged so you can show it in a demo without a Meta account.

---

## 9. Demo accounts

`auth-service` seeds these on first start. The password for all of them is the
value of `SEED_DEFAULT_PASSWORD` in your `.env` (default `Dpdms@2026`).

| Username | Role | Hazard | Ward |
|---|---|---|---|
| `admin` | Provincial Administrator | all | all |
| `national` | National Viewer (read-only) | all | all |
| `supervisor.flood` | Provincial Supervisor | Flood | all wards |
| `supervisor.drought` | Provincial Supervisor | Drought | all wards |
| `supervisor.fire` | Provincial Supervisor | Fire | all wards |
| `supervisor.zoonotic` | Provincial Supervisor | Zoonotic disease | all wards |
| `supervisor.mining` | Provincial Supervisor | Mining accident | all wards |
| `recorder.flood.ward1` | Ward Recorder | Flood | Ward 1 |
| `recorder.flood.ward2` | Ward Recorder | Flood | Ward 2 |
| `recorder.drought.ward1` | Ward Recorder | Drought | Ward 1 |
| `recorder.fire.ward1` | Ward Recorder | Fire | Ward 1 |
| `recorder.zoonotic.ward1` | Ward Recorder | Zoonotic disease | Ward 1 |
| `recorder.mining.ward1` | Ward Recorder | Mining accident | Ward 1 |

Seeding is skipped if any user already exists, and can be disabled entirely with
`dpdms.seed.enabled=false`.

---

## 10. Walkthrough: the full incident lifecycle

This is the demo script. It exercises every requirement in about four minutes.

1. **Record.** Log in as `recorder.flood.ward1` at http://localhost:8090.
   Go to *Incidents → New*. The form only offers Flood, and the ward field is
   fixed to Ward 1. Enter a peak water level of `2.4` m and 60 households
   displaced. Save. Status is **PENDING**.
2. **Alert fires immediately.** 2.4 m ≥ the 2.0 m danger threshold, so the
   service publishes an `AlertEvent`. Open Mailpit (http://localhost:8025) — the
   supervisor's alert email is already there. Then open *Alerts* in the UI to
   show the delivery log row (`SENT`, with channel and timestamp).
3. **Scope check (the important one).** Still logged in as the Ward 1 recorder,
   try to open a Ward 2 incident by typing its URL directly. You get 403. Try
   `/api/droughts` from Swagger with the same token — 403 again. Nothing in the
   UI was relied on.
4. **Review.** Log out, log in as `supervisor.flood`. The incident is in the
   review queue. Click *Request corrections* with the note "confirm the river
   basin". Status becomes **CORRECTIONS_REQUESTED**.
5. **Correct and resubmit.** Back as the recorder, the record is editable again.
   Fix it, *Resubmit* — back to **PENDING**.
6. **Approve.** As `supervisor.flood`, *Approve*. Status **APPROVED**.
   Show the *Audit trail* tab on the incident: every transition with who, when,
   and what changed.
7. **Cross-hazard supervisor check.** Log in as `supervisor.drought` and try to
   approve the same flood incident — 403. Supervisors are hazard-bound.
8. **Dashboard.** Log in as `national`. Counts by hazard, by severity and by
   ward; the 12-month trend line; the Leaflet map with colour-coded markers.
   Click a marker to open the incident. Then try to edit anything — every write
   endpoint returns 403 for the national role.
9. **Reports.** *Reports* → filter to Flood, last 30 days → download PDF, then
   XLSX, then DOCX, then CSV. Open one to show the data matches the dashboard.
10. **Resilience.** Stop `report-service`. Click download again — the gateway's
    circuit breaker returns a friendly fallback instead of hanging. Restart it
    and show recovery.

---

## 11. Roles and access control

| Role | Create | View | Edit | Approve/Reject | Delete | Manage users |
|---|---|---|---|---|---|---|
| `WARD_RECORDER` | own hazard + own ward only | own hazard; own ward, plus APPROVED records in its hazard | own hazard + own ward, and only while PENDING or CORRECTIONS_REQUESTED | no | own records while not yet approved | no |
| `PROVINCIAL_SUPERVISOR` | no | its one hazard, all wards, all statuses | no | its one hazard only | its one hazard | no |
| `NATIONAL_VIEWER` | no | all hazards, all wards, APPROVED only | no | no | no | no |
| `PROVINCIAL_ADMIN` | no | everything | no | no | yes | yes |

Two rules that are easy to miss and are explicitly tested:

- A supervisor is bound to **one** hazard. `supervisor.flood` cannot even read a
  drought incident, let alone approve one.
- The national viewer is **read-only and approved-only**. Pending and rejected
  records never leave the province.

---

## 12. How hazard + ward scoping is actually enforced

This is the part the brief weights most heavily, so it is worth reading the code
itself: `common/src/main/java/zw/ac/uz/dpdms/common/security/`.

Enforcement happens in **four** layers, and the backend layers are the ones that
count:

**Layer 1 — the token.** `JwtService` mints a token whose claims include
`role`, `hazard` and `ward`. The claims are signed (HS384); changing `ward` in
the browser invalidates the signature.

**Layer 2 — `JwtAuthenticationFilter`.** Every service (not just the gateway)
validates the token on every request and builds an `AuthenticatedUser` record.
There is no "trusted internal network" assumption.

**Layer 3 — `HazardScopeFilter`.** Each hazard service declares which hazard it
serves. The filter rejects, with 403, any request whose token is not scoped to
that hazard — before the controller is even reached. This is why a valid flood
token gets 403 from drought-service.

**Layer 4 — `HazardAccessPolicy`.** A pure, side-effect-free class of static
predicates: `canCreate`, `canView`, `canEdit`, `canReview`, `canDelete`,
`canResubmit`, `listScope`. The service layer calls
`HazardAccessPolicy.require(...)` before every mutation, and `listScope(...)`
feeds a JPA `Specification` so list queries are filtered **in SQL** — a recorder
literally cannot receive another ward's rows, even paginated.

Because the policy is pure it is unit-tested exhaustively in
`common/src/test/java/.../HazardAccessPolicyTest.java` — every role × every
hazard × every ward × every status combination.

The UI then uses the same policy to decide which buttons to render. If you
delete the UI entirely, every rule still holds.

---

## 13. The approval workflow

```
                ┌──────────────────────────────────────────┐
                │                                          │
   create       ▼          request corrections             │
  ─────────► PENDING ───────────────────────────► CORRECTIONS_REQUESTED
                │  │                                       │
       approve  │  │ reject                       resubmit │
                ▼  ▼                                       │
           APPROVED  REJECTED ◄────────────────────────────┘
                                   (reject)
```

- Only a `PROVINCIAL_SUPERVISOR` **of that hazard** may approve, reject or
  request corrections.
- Only the original ward recorder may edit and resubmit, and only from
  `PENDING` or `CORRECTIONS_REQUESTED`.
- `APPROVED` and `REJECTED` are terminal. An approved record is immutable —
  attempting an edit returns 409.
- Every transition is written to `incident_audit_log` by `AuditService`, with
  the actor and their role, the action, the from/to status, the timestamp, the
  reviewer's comment and a field-level diff (`ChangeDiff`) of what changed.

The machine itself is `common/.../workflow/ApprovalWorkflow.java`, unit-tested in
`ApprovalWorkflowTest`. Illegal transitions throw, they are not silently ignored.

---

## 14. Hazard indicators and alert thresholds

Each service records the shared incident metadata (ward, district, province,
date/time of occurrence, reporter, severity, GPS latitude/longitude, status,
review comment / reviewer / review time, and created/updated timestamps)
plus **five hazard-specific indicators**:

### Flood
| Indicator | Type |
|---|---|
| Peak water level (m) | decimal |
| River basin | text |
| Households displaced | integer |
| Area flooded (hectares) | decimal |
| Inundation duration (days) | integer |

**Alerts when:** peak level ≥ 2.0 m, **or** ≥ 50 households displaced, **or**
severity is HIGH/CRITICAL.

### Drought
| Indicator | Type |
|---|---|
| Rainfall deficit (mm) | decimal |
| Consecutive dry days | integer |
| Crop failure (%) | decimal |
| People facing water shortages | integer |
| Livestock mortality count | integer |

**Alerts when:** crop failure ≥ 50%, **or** ≥ 1000 people short of water,
**or** ≥ 60 consecutive dry days, **or** severity HIGH/CRITICAL.

### Fire
| Indicator | Type |
|---|---|
| Area burned (hectares) | decimal |
| Suspected cause | enum (NATURAL, ACCIDENTAL, DELIBERATE) |
| Injuries or fatalities | integer |
| Structures destroyed | integer |
| Still active | boolean |

**Alerts when:** the fire is still burning, **or** any injuries/fatalities,
**or** ≥ 50 ha burned, **or** severity HIGH/CRITICAL.

### Zoonotic disease
| Indicator | Type |
|---|---|
| Pathogen name | text |
| Animal species affected | text |
| Confirmed human cases | integer |
| Confirmed animal cases | integer |
| Classification | enum (CLUSTER, OUTBREAK) |

**Alerts when:** classification is OUTBREAK, **or** any confirmed human case,
**or** ≥ 10 confirmed animal cases, **or** severity HIGH/CRITICAL.

### Mining accident
| Indicator | Type |
|---|---|
| Mine name | text |
| Mine type | enum (FORMAL, ARTISANAL) |
| Accident type | enum (COLLAPSE, GAS_EXPLOSION, FLOODING, FALL_OF_GROUND) |
| Trapped or injured miners | integer |
| Fatalities | integer |
| Rescue ongoing | boolean |

**Alerts when:** any fatality, **or** any trapped/injured miner, **or** a rescue
is ongoing, **or** severity HIGH/CRITICAL.

Each rule set lives in its own `*AlertRules` class with no Spring dependencies,
so it is trivially unit-testable and readable by a non-programmer.

---

## 15. How alerts are dispatched

1. A hazard service saves an incident, then asks its `*AlertRules` whether the
   incident deserves an alert. If yes, `AlertPublisher` publishes an
   `AlertEvent` (hazard, incident id, ward, severity, reason, summary, a unique
   `eventId`) to the `dpdms.alerts` exchange.
   The publish is wrapped so that an `AmqpException` is logged and swallowed —
   **a broker outage never fails a recorder's save**.
2. `alert-service` consumes the event with `@RabbitListener`. Failures go to a
   dead-letter queue rather than being retried forever.
3. `RecipientLookup` calls auth-service's `/internal/users/alert-recipients`
   (protected by `X-Internal-Api-Key`, load-balanced through Eureka) to find
   everyone who should be told: the supervisor for that hazard, the provincial
   admin, and the recorders of the affected ward.
4. `AlertMessageFactory` renders the subject and body.
5. `AlertDispatchService` sends per recipient, per channel, and records one
   `alert_log` row each with status `SENT`, `FAILED` or `SKIPPED`, the error if
   any, and the timestamp. Dispatch is **idempotent by `eventId`**, so a
   redelivered message does not double-send.
6. `/api/alerts` exposes the delivery log, hazard-scoped like everything else
   (a flood supervisor sees only flood alerts), plus `/api/alerts/statistics`.

Email goes through JavaMail (Mailpit locally, any SMTP in production). WhatsApp
goes through the Meta WhatsApp Business Cloud API; when disabled, the message is
logged instead so the flow is still demonstrable.

---

## 16. Reports

`report-service` calls the hazard services through the gateway, **forwarding the
caller's own JWT**, so a report can never contain data the requester could not
see in the UI. If a hazard service refuses (403) or is down, that hazard is
skipped and the rest of the report still generates.

```
GET /api/reports/{format}?hazard=FLOOD&ward=Ward 1&status=APPROVED&from=2026-01-01&to=2026-09-30
```

`{format}` is `pdf`, `docx`, `xlsx` or `csv` (case-insensitive). All filters are
optional. `GET /api/reports/preview` returns the same rows as JSON so the UI can
show a preview before downloading.

| Format | Library | Output |
|---|---|---|
| PDF | OpenPDF | Titled, dated document with a summary table |
| DOCX | Apache POI (XWPF) | Word document with heading and table |
| XLSX | Apache POI (XSSF) | Styled sheet, frozen header, auto-sized columns |
| CSV | Commons CSV | Plain export for Excel/R/Python |

---

## 17. Dashboard and map

`dashboard-service` fans out to all five hazard services in parallel and returns:

- `GET /api/dashboard/summary` — totals, counts by hazard, by severity, by ward,
  by status; a 12-month trend series; and the 10 most recent incidents.
- `GET /api/dashboard/map` — every incident that has coordinates, as map points
  with hazard, severity, ward and status.

The UI renders the summary as cards plus a trend chart, and the map with Leaflet
over OpenStreetMap tiles. Markers are colour-coded by severity and clicking one
opens the incident (subject to the same access rules — a recorder clicking an
out-of-ward marker gets the 403 page, because the marker list itself is already
scope-filtered server-side).

GPS coordinates are validated against Zimbabwe's bounding box
(latitude −22.5 to −15.5, longitude 25.0 to 33.1) at the DTO level, so a typo
cannot drop a Rushinga flood into the Atlantic.

---

## 18. API documentation

With the gateway running, the combined Swagger UI is at:

**http://localhost:8080/swagger-ui.html**

Use the dropdown at the top right to switch between the nine documented
services. To call a protected endpoint:

1. Expand `POST /api/auth/login`, *Try it out*, send
   `{"username":"admin","password":"Dpdms@2026"}`.
2. Copy the `token` from the response.
3. Click **Authorize** (top right), paste the token, *Authorize*.

Individual services also serve their own docs at
`http://localhost:<port>/swagger-ui.html` when run directly.

A written endpoint reference is in [`docs/api.md`](docs/api.md).

---

## 19. Testing

```powershell
mvn test                      # everything
mvn -pl common test           # just the security/workflow unit tests
mvn -pl flood-service test    # one service's integration tests
```

There are 18 test classes. The ones worth showing a marker:

| Test | What it proves |
|---|---|
| `HazardAccessPolicyTest` | Every role × hazard × ward × status combination resolves correctly. |
| `ApprovalWorkflowTest` | Legal transitions succeed, illegal ones throw. |
| `JwtServiceTest` | Tokens round-trip; tampered and expired tokens are rejected. |
| `AuthControllerIntegrationTest` (17 cases) | Login, lockout after 5 failed attempts, role-gated user administration. |
| `FloodIncidentIntegrationTest` (17 cases) | Full CRUD + workflow + cross-ward and cross-hazard 403s, end-to-end through MockMvc. |
| `*AlertRulesTest` (×5) | Each threshold fires and does not over-fire. |
| `AlertDispatchServiceTest` | Idempotency by `eventId`, per-channel logging, failure recorded not thrown. |
| `ReportGeneratorTest` | Generated bytes carry the correct file signatures (`%PDF-`, `PK`). |
| `DashboardServiceTest` | Aggregation maths and trend bucketing. |
| `HazardFormsTest` | Every hazard's UI form metadata matches its entity's indicators. |

Integration tests use H2 in PostgreSQL-compatibility mode with Flyway disabled,
so they run with no MySQL, no RabbitMQ and no network.

---

## 20. Project layout

```
dpdms/
├── pom.xml                        parent POM, dependency management, 13 modules
├── .env.example                   copy to .env and fill in
├── docker-compose.yml             RabbitMQ + Mailpit (+ optional MySQL)
├── start-all.ps1 / .sh            start every service in the right order
├── stop-all.ps1 / .sh
├── infrastructure/mysql/          database creation script
├── docs/
│   ├── architecture.md            all diagrams (Mermaid)
│   ├── api.md                     endpoint reference
│   ├── presentation.md            slide-by-slide outline + demo script
│   ├── peer-evaluation.md         form template
│   ├── git-workflow.md            getting the project into the group repo
│   └── troubleshooting.md         every error we actually hit, and the fix
├── common/                        shared library — security, workflow, audit, DTOs
├── discovery-service/             Eureka
├── gateway/                       Spring Cloud Gateway
├── auth-service/                  users, login, JWT
├── flood-service/                 ─┐
├── drought-service/                │
├── fire-service/                   ├ five hazard services, identical shape
├── zoonotic-disease-service/       │
├── mining-accident-service/       ─┘
├── alert-service/                 RabbitMQ consumer, email/WhatsApp, delivery log
├── report-service/                PDF/DOCX/XLSX/CSV
├── dashboard-service/             aggregation + map points
└── web-ui/                        Thymeleaf front end
```

Inside every hazard service the package structure is the same, which is the
point — a marker (or a teammate) who understands one understands all five:

```
zw.ac.uz.dpdms.<hazard>/
├── domain/       entity, repository, JPA specifications
├── dto/          request/response records with Bean Validation
├── alert/        *AlertRules, AlertPublisher, RabbitConfig
├── service/      transactional service, access checks, audit writes
└── web/          REST controller
```

---

## 21. Configuration reference

Everything below is read from `.env` (or real environment variables, which win).

| Variable | Default | Meaning |
|---|---|---|
| `JWT_SECRET` | — (required) | HS384 signing key, ≥ 32 chars |
| `JWT_EXPIRATION_MINUTES` | 480 | Token lifetime |
| `INTERNAL_API_KEY` | — (required) | Shared secret for `/internal/**` calls |
| `SEED_DEFAULT_PASSWORD` | `Dpdms@2026` | Password for the seeded demo users |
| `DB_HOST` / `DB_PORT` | localhost / 3306 | MySQL location |
| `DB_USERNAME` / `DB_PASSWORD` | dpdms / — | MySQL credentials |
| `RABBITMQ_HOST` / `PORT` | localhost / 5672 | Broker location |
| `RABBITMQ_USERNAME` / `PASSWORD` | guest / guest | Broker credentials |
| `MAIL_HOST` / `MAIL_PORT` | localhost / 1025 | SMTP (Mailpit by default) |
| `MAIL_FROM` | alerts@dpdms.local | Sender address |
| `WHATSAPP_ENABLED` | false | Turn on the Meta Cloud API sender |
| `WHATSAPP_API_URL` | graph.facebook.com/v21.0 | API base |
| `WHATSAPP_PHONE_NUMBER_ID` / `ACCESS_TOKEN` | — | Meta credentials |
| `EUREKA_URL` | http://localhost:8761/eureka | Registry location |
| `GATEWAY_URL` | http://localhost:8080 | Used by web-ui, report and dashboard services |

---

## 22. Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `503 Service Unavailable` from the gateway | Eureka has not propagated the registration yet | Wait ~30 s after the service starts, refresh http://localhost:8761 |
| Browser cannot reach `localhost` but `127.0.0.1` works | Windows IPv6/hosts resolution | Use `http://127.0.0.1:<port>` |
| `Access denied for user 'dpdms'@'localhost'` | `.env` `DB_PASSWORD` ≠ the password in the SQL script | Make them match, or re-run the SQL with the right password |
| `Connection reset` / `Permission denied: getsockopt` during `mvn` | Firewall or antivirus blocking Maven Central | Allow `java.exe` through the firewall, or run `mvn -U clean install` on a different network |
| `401` with `WWW-Authenticate: Basic` on a protected endpoint | Actuator's security auto-config registered its chain first | Already fixed — `DpdmsSecurityAutoConfiguration` declares `beforeName = ManagementWebSecurityAutoConfiguration` |
| YAML parse errors in the gateway | IntelliJ re-indents pasted YAML | Already fixed — the gateway uses flat `application.properties`, which cannot be broken by indentation |
| No alert email appears | RabbitMQ or Mailpit not running | `docker compose up -d`, then check http://localhost:15672 and http://localhost:8025 |
| `Table 'dpdms_x.flyway_schema_history' doesn't exist` loops | Database not created | Re-run `infrastructure/mysql/01-create-databases.sql` |

More detail, including the full stack traces we hit during development and what
each one actually meant, is in [`docs/troubleshooting.md`](docs/troubleshooting.md).

---

## 23. Team and division of work

Each member owns one hazard service end-to-end and must be able to explain, in
the presentation, how scoping and the approval workflow are enforced **in their
own service's code**.

| Member | Registration no. | Owns | Also responsible for |
|---|---|---|---|
| | | flood-service | |
| | | drought-service | |
| | | fire-service | |
| | | zoonotic-disease-service | |
| | | mining-accident-service | |

Shared modules (`common`, `gateway`, `auth-service`, `alert-service`,
`report-service`, `dashboard-service`, `web-ui`) were built jointly; fill in the
table above with who led each before submitting.

The peer evaluation form template is in [`docs/peer-evaluation.md`](docs/peer-evaluation.md).
The presentation plan and demo script are in [`docs/presentation.md`](docs/presentation.md),
and the Git workflow — including how to move this project into the group's
repository and build a real commit history — is in
[`docs/git-workflow.md`](docs/git-workflow.md).

---

*University of Zimbabwe — Department of Computer Science — 2026*
