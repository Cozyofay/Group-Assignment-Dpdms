# DPDMS — REST API reference

Base URL for everything: **`http://localhost:8080`** (the gateway).
Interactive version: **http://localhost:8080/swagger-ui.html**

Every endpoint except `POST /api/auth/login` requires:

```
Authorization: Bearer <jwt>
```

Responses use `application/json`. Errors use a single shape (`ApiError`):

```json
{
  "timestamp": "2026-09-23T14:05:11.482Z",
  "status": 403,
  "error": "Forbidden",
  "message": "You may only work with FLOOD incidents in Ward 1",
  "path": "/api/floods/17",
  "fieldErrors": null
}
```

For a 400 caused by Bean Validation, `fieldErrors` carries one entry per bad
field:

```json
{
  "timestamp": "2026-09-23T14:06:02.119Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/floods",
  "fieldErrors": {
    "latitude": "must be within Zimbabwe (-22.5 to -15.5)",
    "riverBasin": "must not be blank"
  }
}
```

Every response also carries an **`X-Correlation-Id`** header. The same id appears
in every service's log lines for that request, so it is the fastest way to trace
a failure across services: copy the header value and grep the logs for it.

| Status | When |
|---|---|
| 400 | Bean Validation failed (field-level messages included) |
| 401 | Missing, malformed, expired or tampered token |
| 403 | Authenticated, but outside your hazard / ward / role scope |
| 404 | No such incident, or one you are not permitted to know exists |
| 409 | Illegal workflow transition, or editing an APPROVED record |
| 503 | Downstream service unavailable (gateway circuit breaker fallback) |

---

## 1. Authentication — `auth-service`

### `POST /api/auth/login`
Public.

```json
{ "username": "recorder.flood.ward1", "password": "Dpdms@2026" }
```

```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9...",
  "username": "recorder.flood.ward1",
  "fullName": "Flood Recorder Ward 1",
  "role": "WARD_RECORDER",
  "hazard": "FLOOD",
  "ward": "Ward 1",
  "expiresAt": "2026-09-23T22:05:11"
}
```

Five consecutive failures lock the account for 15 minutes. Further attempts
return `401` with a message giving the time the lock expires. An administrator
can clear it early with `POST /api/users/{id}/reset-password`.

### `GET /api/auth/me`
Returns the same profile shape for the current token. Useful for the UI and for
proving in a demo which scope a token actually carries.

### `POST /api/auth/change-password`

```json
{ "currentPassword": "Dpdms@2026", "newPassword": "NewPass@2026" }
```

---

## 2. User administration — `auth-service`

All of these require `PROVINCIAL_ADMIN`. Any other role gets 403.

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/users` | List all users |
| `GET` | `/api/users/{id}` | One user |
| `POST` | `/api/users` | Create a user |
| `PUT` | `/api/users/{id}` | Update role, hazard, ward, contact details, enabled flag |
| `DELETE` | `/api/users/{id}` | Delete a user |
| `POST` | `/api/users/{id}/reset-password` | Reset to a supplied password and clear any lockout |

Create/update body:

```json
{
  "username": "recorder.flood.ward3",
  "fullName": "Flood Recorder Ward 3",
  "email": "recorder.flood.ward3@dpdms.local",
  "phoneNumber": "+263771234567",
  "password": "Dpdms@2026",
  "role": "WARD_RECORDER",
  "hazard": "FLOOD",
  "ward": "Ward 3",
  "enabled": true
}
```

`hazard` and `ward` are validated against the role: `WARD_RECORDER` requires
both, `PROVINCIAL_SUPERVISOR` requires `hazard` and rejects `ward`,
`PROVINCIAL_ADMIN` and `NATIONAL_VIEWER` reject both. The rule lives in
`Role.requiresHazard()` / `Role.requiresWard()`.

### `GET /internal/users/alert-recipients`
Service-to-service only. Requires the header `X-Internal-Api-Key`; it is not
routed through the gateway and rejects any request carrying only a user JWT.

---

## 3. Hazard incidents

The five hazard services expose an identical API. Substitute the path:

| Hazard | Path prefix |
|---|---|
| Flood | `/api/floods` |
| Drought | `/api/droughts` |
| Fire | `/api/fires` |
| Zoonotic disease | `/api/zoonotic-diseases` |
| Mining accident | `/api/mining-accidents` |

| Method | Path | Who | Notes |
|---|---|---|---|
| `POST` | `/api/floods` | Ward recorder | Creates as `PENDING`; ward must match the token |
| `GET` | `/api/floods` | All roles | Automatically scope-filtered in SQL |
| `GET` | `/api/floods/{id}` | All roles | 403/404 if outside your scope |
| `PUT` | `/api/floods/{id}` | Owning recorder | Only while `PENDING` or `CORRECTIONS_REQUESTED` |
| `DELETE` | `/api/floods/{id}` | Owning recorder (pre-approval), admin, supervisor | |
| `POST` | `/api/floods/{id}/approve` | Supervisor of that hazard | |
| `POST` | `/api/floods/{id}/reject` | Supervisor of that hazard | Comment required |
| `POST` | `/api/floods/{id}/request-corrections` | Supervisor of that hazard | Comment required |
| `POST` | `/api/floods/{id}/resubmit` | Owning recorder | Only from `CORRECTIONS_REQUESTED` |
| `GET` | `/api/floods/{id}/audit` | Anyone who can view the incident | Full audit trail |
| `GET` | `/api/floods/approved` | All roles | Shared `IncidentSummary` shape, used by dashboard and reports |

### List filters

All optional, all combinable:

```
GET /api/floods?ward=Ward 1&district=Rushinga&status=PENDING&severity=HIGH
                &from=2026-09-01T00:00:00&to=2026-09-30T23:59:59
```

Filters narrow what you can already see; they never widen it. A Ward 1 recorder
asking for `ward=Ward 2` gets an empty list, not someone else's data.

### Create / update body

Shared metadata (all hazards):

```json
{
  "ward": "Ward 1",
  "district": "Rushinga",
  "province": "Mashonaland Central",
  "occurredAt": "2026-09-22T06:30:00",
  "severity": "HIGH",
  "latitude": -16.7421,
  "longitude": 32.1189
}
```

`occurredAt` must not be in the future. Latitude must be between −22.5 and
−15.5, longitude between 25.0 and 33.1 (Zimbabwe's bounding box). All text
fields reject angle brackets and control characters.

Plus the five indicators for the hazard:

<details>
<summary><b>Flood</b> — <code>POST /api/floods</code></summary>

```json
{
  "...": "shared metadata above",
  "peakWaterLevelMetres": 2.4,
  "riverBasin": "Mazowe",
  "householdsDisplaced": 60,
  "areaFloodedHectares": 180.5,
  "inundationDurationDays": 4
}
```
Limits: level 0–50 m, households 0–1 000 000, duration 0–3650 days.
</details>

<details>
<summary><b>Drought</b> — <code>POST /api/droughts</code></summary>

```json
{
  "...": "shared metadata above",
  "rainfallDeficitMm": 210.0,
  "consecutiveDryDays": 72,
  "cropFailurePercentage": 65.0,
  "peopleFacingWaterShortages": 1400,
  "livestockMortalityCount": 230
}
```
`cropFailurePercentage` is 0–100.
</details>

<details>
<summary><b>Fire</b> — <code>POST /api/fires</code></summary>

```json
{
  "...": "shared metadata above",
  "areaBurnedHectares": 95.0,
  "suspectedCause": "DELIBERATE",
  "injuriesOrFatalities": 2,
  "structuresDestroyed": 7,
  "stillActive": true
}
```
`suspectedCause`: `NATURAL` | `ACCIDENTAL` | `DELIBERATE`.
</details>

<details>
<summary><b>Zoonotic disease</b> — <code>POST /api/zoonotic-diseases</code></summary>

```json
{
  "...": "shared metadata above",
  "pathogenName": "Anthrax",
  "animalSpeciesAffected": "Cattle",
  "confirmedHumanCases": 3,
  "confirmedAnimalCases": 24,
  "classification": "OUTBREAK"
}
```
`classification`: `CLUSTER` | `OUTBREAK`.
</details>

<details>
<summary><b>Mining accident</b> — <code>POST /api/mining-accidents</code></summary>

```json
{
  "...": "shared metadata above",
  "mineName": "Kachere Shaft 2",
  "mineType": "ARTISANAL",
  "accidentType": "COLLAPSE",
  "trappedOrInjuredMiners": 5,
  "fatalities": 1,
  "rescueOngoing": true
}
```
`mineType`: `FORMAL` | `ARTISANAL`.
`accidentType`: `COLLAPSE` | `GAS_EXPLOSION` | `FLOODING` | `FALL_OF_GROUND`.
</details>

### Review body

`reject` and `request-corrections` require a comment; `approve` accepts an
optional one:

```json
{ "comment": "Confirm the river basin with the district office." }
```

### Audit trail

```
GET /api/floods/17/audit
```

```json
[
  { "id": 41, "action": "CREATED", "fromStatus": null, "toStatus": "PENDING",
    "actorUsername": "recorder.flood.ward1", "actorRole": "WARD_RECORDER",
    "actedAt": "2026-09-22T07:10:03", "changes": null, "comment": null },
  { "id": 44, "action": "CORRECTIONS_REQUESTED", "fromStatus": "PENDING",
    "toStatus": "CORRECTIONS_REQUESTED", "actorUsername": "supervisor.flood",
    "actorRole": "PROVINCIAL_SUPERVISOR", "actedAt": "2026-09-22T09:31:44",
    "changes": null, "comment": "Confirm the river basin." },
  { "id": 47, "action": "UPDATED", "fromStatus": "CORRECTIONS_REQUESTED",
    "toStatus": "CORRECTIONS_REQUESTED", "actorUsername": "recorder.flood.ward1",
    "actorRole": "WARD_RECORDER", "actedAt": "2026-09-22T10:02:19",
    "changes": "riverBasin: 'Mazoe' -> 'Mazowe'", "comment": null },
  { "id": 48, "action": "RESUBMITTED", "fromStatus": "CORRECTIONS_REQUESTED",
    "toStatus": "PENDING", "actorUsername": "recorder.flood.ward1",
    "actorRole": "WARD_RECORDER", "actedAt": "2026-09-22T10:02:20",
    "changes": null, "comment": null },
  { "id": 52, "action": "APPROVED", "fromStatus": "PENDING", "toStatus": "APPROVED",
    "actorUsername": "supervisor.flood", "actorRole": "PROVINCIAL_SUPERVISOR",
    "actedAt": "2026-09-22T11:15:08", "changes": null, "comment": "Verified." }
]
```

---

## 4. Dashboard — `dashboard-service`

### `GET /api/dashboard/summary`

Optional params: `ward`, `district`, `severity`, `from`, `to`.

```json
{
  "totalIncidents": 42,
  "countsByHazard":   { "FLOOD": 12, "DROUGHT": 9, "FIRE": 11, "ZOONOTIC_DISEASE": 5, "MINING_ACCIDENT": 5 },
  "countsBySeverity": { "LOW": 8, "MODERATE": 14, "HIGH": 15, "CRITICAL": 5 },
  "countsByWard":     { "Ward 1": 18, "Ward 2": 13, "Ward 3": 11 },
  "countsByStatus":   { "PENDING": 6, "APPROVED": 31, "REJECTED": 2, "CORRECTIONS_REQUESTED": 3 },
  "trend": [ { "month": "2025-10", "count": 2 }, { "month": "2025-11", "count": 5 } ],
  "recent": [ { "hazard": "FLOOD", "id": 17, "ward": "Ward 1", "severity": "HIGH",
                "status": "APPROVED", "occurredAt": "2026-09-22T06:30:00" } ]
}
```

### `GET /api/dashboard/map`

Same params. Returns only incidents that carry coordinates:

```json
[
  { "hazard": "FLOOD", "id": 17, "ward": "Ward 1", "district": "Rushinga",
    "severity": "HIGH", "status": "APPROVED",
    "latitude": -16.7421, "longitude": 32.1189,
    "occurredAt": "2026-09-22T06:30:00",
    "summary": "Peak water level 2.4 m, 60 households displaced" }
]
```

Both endpoints fan out to the hazard services with **your** token, so the
numbers you see are the numbers you are allowed to see.

---

## 5. Reports — `report-service`

### `GET /api/reports/{format}`

`{format}` = `pdf` | `docx` | `xlsx` | `csv` (case-insensitive).

Optional params: `hazards` (repeatable), `ward`, `district`, `severity`,
`from`, `to`.

```
GET /api/reports/pdf?hazards=FLOOD&hazards=DROUGHT&ward=Ward 1
                    &from=2026-09-01T00:00:00&to=2026-09-30T23:59:59
```

Returns the file with `Content-Disposition: attachment`. A hazard that refuses
(403) or is unavailable is skipped, and the report still generates for the rest.

### `GET /api/reports/preview`

Identical parameters, returns the rows as JSON so the UI can show a preview
before the user commits to a download.

---

## 6. Alerts — `alert-service`

### `GET /api/alerts`

Optional params: `hazard`, `channel` (`EMAIL` | `WHATSAPP`), `status`
(`SENT` | `FAILED` | `SKIPPED`), `from`, `to`. Hazard-scoped: a flood supervisor
sees only flood alerts; a recorder sees only alerts for their own hazard.

```json
[
  { "id": 91, "eventId": "5f2c...", "hazard": "FLOOD", "incidentId": 17,
    "recipient": "supervisor.flood@dpdms.local", "channel": "EMAIL",
    "status": "SENT", "reason": "Peak water level of 2.4 m is at or above the 2.0 m danger threshold",
    "errorMessage": null, "dispatchedAt": "2026-09-22T07:10:06" }
]
```

### `GET /api/alerts/statistics`

```json
{ "total": 128, "sent": 119, "failed": 3, "skipped": 6, "byChannel": { "EMAIL": 118, "WHATSAPP": 10 } }
```

---

## 7. Quick curl session

```bash
# 1. log in
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"recorder.flood.ward1","password":"Dpdms@2026"}' \
  | python -c "import sys,json;print(json.load(sys.stdin)['token'])")

# 2. record a flood that will trigger an alert
curl -X POST http://localhost:8080/api/floods \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"ward":"Ward 1","district":"Rushinga","province":"Mashonaland Central",
       "occurredAt":"2026-09-22T06:30:00","severity":"HIGH",
       "latitude":-16.7421,"longitude":32.1189,
       "peakWaterLevelMetres":2.4,"riverBasin":"Mazowe","householdsDisplaced":60,
       "areaFloodedHectares":180.5,"inundationDurationDays":4}'

# 3. prove cross-hazard scoping: same token, drought service → 403
curl -i http://localhost:8080/api/droughts -H "Authorization: Bearer $TOKEN"

# 4. prove cross-ward scoping: Ward 2 filter returns [] not other people's rows
curl -s "http://localhost:8080/api/floods?ward=Ward%202" -H "Authorization: Bearer $TOKEN"

# 5. approve it as the flood supervisor
SUP=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"supervisor.flood","password":"Dpdms@2026"}' \
  | python -c "import sys,json;print(json.load(sys.stdin)['token'])")
curl -X POST http://localhost:8080/api/floods/1/approve \
  -H "Authorization: Bearer $SUP" -H "Content-Type: application/json" \
  -d '{"comment":"Verified with the district office."}'

# 6. download a PDF report
curl -o report.pdf "http://localhost:8080/api/reports/pdf?hazards=FLOOD" \
  -H "Authorization: Bearer $SUP"
```

Steps 3 and 4 are the ones to run in front of the class: they prove the scoping
holds with the UI entirely out of the picture.
