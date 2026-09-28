# DPDMS — Architecture

All diagrams below are [Mermaid](https://mermaid.js.org). GitHub renders them
automatically in the browser. To export one as an image for the report or the
slides, paste it into https://mermaid.live and click *Download PNG/SVG*.

---

## 1. System context

Who uses the system and what it talks to.

```mermaid
flowchart TB
    subgraph Users
        R["Ward Recorder<br/>one hazard, one ward"]
        S["Provincial Supervisor<br/>one hazard, all wards"]
        A["Provincial Administrator"]
        N["National Viewer<br/>read-only, approved only"]
    end

    R --> UI
    S --> UI
    A --> UI
    N --> UI

    UI["DPDMS Web UI<br/>Thymeleaf :8090"] --> GW["API Gateway :8080"]
    GW --> SYS["DPDMS micro-services"]

    SYS --> DB[("MySQL<br/>7 schemas")]
    SYS --> MQ["RabbitMQ"]
    SYS --> SMTP["SMTP server<br/>(Mailpit locally)"]
    SYS --> WA["WhatsApp Business<br/>Cloud API"]
    UI --> OSM["OpenStreetMap tiles<br/>(Leaflet)"]
```

---

## 2. Component / deployment view

Every box is a separately runnable Spring Boot application.

```mermaid
flowchart TB
    Browser(["Browser"])

    subgraph Edge
        UI["web-ui<br/>:8090<br/>Thymeleaf + Leaflet"]
        GW["gateway<br/>:8080<br/>Spring Cloud Gateway"]
    end

    EUREKA["discovery-service<br/>:8761<br/>Eureka"]

    subgraph Core["Core services"]
        AUTH["auth-service :8081"]
        ALERT["alert-service :8087"]
        REPORT["report-service :8088"]
        DASH["dashboard-service :8089"]
    end

    subgraph Hazards["Hazard services (one per hazard)"]
        FL["flood-service :8082"]
        DR["drought-service :8083"]
        FI["fire-service :8084"]
        ZO["zoonotic-disease-service :8085"]
        MI["mining-accident-service :8086"]
    end

    subgraph Data["Databases (one schema per service)"]
        DBA[("dpdms_auth")]
        DBF[("dpdms_flood")]
        DBD[("dpdms_drought")]
        DBI[("dpdms_fire")]
        DBZ[("dpdms_zoonotic")]
        DBM[("dpdms_mining")]
        DBL[("dpdms_alert")]
    end

    MQ{{"RabbitMQ<br/>exchange: dpdms.alerts"}}

    Browser --> UI --> GW
    GW --> AUTH & FL & DR & FI & ZO & MI & REPORT & DASH & ALERT

    AUTH --> DBA
    FL --> DBF
    DR --> DBD
    FI --> DBI
    ZO --> DBZ
    MI --> DBM
    ALERT --> DBL

    FL & DR & FI & ZO & MI -- "publish AlertEvent" --> MQ
    MQ -- "consume" --> ALERT
    ALERT -- "/internal/users/alert-recipients<br/>X-Internal-Api-Key" --> AUTH

    REPORT -- "read, forwarding caller JWT" --> GW
    DASH -- "read, forwarding caller JWT" --> GW

    UI -.register.-> EUREKA
    GW -.register.-> EUREKA
    AUTH -.register.-> EUREKA
    FL -.register.-> EUREKA
    DR -.register.-> EUREKA
    FI -.register.-> EUREKA
    ZO -.register.-> EUREKA
    MI -.register.-> EUREKA
    ALERT -.register.-> EUREKA
    REPORT -.register.-> EUREKA
    DASH -.register.-> EUREKA
```

Note the two directions of traffic:

- **Writes** go browser → web-ui → gateway → hazard service → its own database.
- **Cross-hazard reads** (reports, dashboard) never touch another service's
  database. They call the owning service's API, forwarding the caller's JWT, so
  access control is applied by the owner, not re-implemented by the reader.

---

## 3. Package structure of the shared `common` module

```mermaid
flowchart LR
    subgraph common["zw.ac.uz.dpdms.common"]
        direction TB
        DOM["domain<br/>HazardType, Role, Severity,<br/>IncidentStatus, BaseIncident,<br/>ScopedIncident"]
        DTO["dto<br/>BaseIncidentRequest,<br/>IncidentSummary, ReviewRequest,<br/>AlertRecipient"]
        SEC["security<br/>JwtService, JwtAuthenticationFilter,<br/>HazardScopeFilter, InternalApiKeyFilter,<br/>HazardAccessPolicy, CurrentUser"]
        WF["workflow<br/>ApprovalWorkflow, WorkflowAction"]
        AUD["audit<br/>AuditAction, IncidentAuditLog,<br/>AuditService, ChangeDiff"]
        MSG["messaging<br/>AlertEvent, AlertMessaging"]
        CFG["config<br/>DpdmsSecurityAutoConfiguration,<br/>DpdmsOpenApiAutoConfiguration"]
        ERR["error<br/>ApiError, GlobalExceptionHandler,<br/>CorrelationIdFilter"]
    end
```

`common` is packaged as a plain JAR with Spring Boot auto-configuration entries,
so every service gets the same security stack by adding one dependency. This is
the mechanism that makes "enforced in the backend, in every service" true rather
than aspirational.

---

## 4. Domain model

```mermaid
classDiagram
    class BaseIncident {
        <<MappedSuperclass>>
        +Long id
        +String ward
        +String district
        +String province
        +LocalDateTime occurredAt
        +String reporterUsername
        +Severity severity
        +IncidentStatus status
        +Double latitude
        +Double longitude
        +String reviewComment
        +String reviewedBy
        +LocalDateTime reviewedAt
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
        +Long version
    }

    class ScopedIncident {
        <<interface>>
        +getWard() String
        +getReporterUsername() String
        +getStatus() IncidentStatus
    }

    class FloodIncident {
        +Double peakWaterLevelMetres
        +String riverBasin
        +Integer householdsDisplaced
        +Double areaFloodedHectares
        +Integer inundationDurationDays
    }
    class DroughtIncident {
        +Double rainfallDeficitMm
        +Integer consecutiveDryDays
        +Double cropFailurePercentage
        +Integer peopleFacingWaterShortages
        +Integer livestockMortalityCount
    }
    class FireIncident {
        +Double areaBurnedHectares
        +FireCause suspectedCause
        +Integer injuriesOrFatalities
        +Integer structuresDestroyed
        +Boolean stillActive
    }
    class ZoonoticDiseaseIncident {
        +String pathogenName
        +String animalSpeciesAffected
        +Integer confirmedHumanCases
        +Integer confirmedAnimalCases
        +EventClassification classification
    }
    class MiningAccidentIncident {
        +String mineName
        +MineType mineType
        +AccidentType accidentType
        +Integer trappedOrInjuredMiners
        +Integer fatalities
        +Boolean rescueOngoing
    }

    class IncidentAuditLog {
        +Long id
        +HazardType hazard
        +Long incidentId
        +AuditAction action
        +String actorUsername
        +LocalDateTime occurredAt
        +String note
        +String changes
    }

    BaseIncident <|-- FloodIncident
    BaseIncident <|-- DroughtIncident
    BaseIncident <|-- FireIncident
    BaseIncident <|-- ZoonoticDiseaseIncident
    BaseIncident <|-- MiningAccidentIncident
    ScopedIncident <|.. BaseIncident
    BaseIncident "1" --> "*" IncidentAuditLog : audited by
```

This is the assignment's core OOP demonstration:

- **Inheritance** — `BaseIncident` holds the metadata every hazard shares
  (`@MappedSuperclass`, so each hazard still gets its own table in its own
  schema, no shared-table coupling).
- **Abstraction / interface** — `ScopedIncident` is all `HazardAccessPolicy`
  needs to know about an incident. The policy has no idea floods exist.
- **Polymorphism** — the report and dashboard services handle all five hazards
  through `IncidentSummary`, a single projection type.
- **Encapsulation** — status changes are only reachable through
  `ApprovalWorkflow`; there is no public setter that lets a caller jump from
  `PENDING` straight to `APPROVED` without an audit entry.

---

## 5. Sequence: recording an incident that triggers an alert

```mermaid
sequenceDiagram
    autonumber
    actor R as Ward Recorder
    participant UI as web-ui
    participant GW as gateway
    participant FS as flood-service
    participant DB as dpdms_flood
    participant MQ as RabbitMQ
    participant AL as alert-service
    participant AU as auth-service
    participant SMTP as SMTP / WhatsApp

    R->>UI: submit flood form
    UI->>GW: POST /api/floods (Bearer JWT from session)
    GW->>FS: route via lb://flood-service
    FS->>FS: JwtAuthenticationFilter → AuthenticatedUser
    FS->>FS: HazardScopeFilter (token hazard == FLOOD?)
    FS->>FS: HazardAccessPolicy.canCreate(user, FLOOD, ward)
    FS->>DB: INSERT incident (status = PENDING)
    FS->>DB: INSERT audit row (CREATED)
    FS->>FS: FloodAlertRules.alertReason(incident)
    FS-->>MQ: publish AlertEvent (async, failures swallowed)
    FS-->>GW: 201 Created
    GW-->>UI: 201
    UI-->>R: incident page, status PENDING

    MQ-->>AL: deliver AlertEvent
    AL->>AU: GET /internal/users/alert-recipients (X-Internal-Api-Key)
    AU-->>AL: supervisor + admin + ward recorders
    loop per recipient per channel
        AL->>SMTP: send message
        AL->>AL: write alert_log row (SENT / FAILED / SKIPPED)
    end
```

The recorder's request returns as soon as the row is committed. Everything after
step 12 happens on the broker's thread — an SMTP outage cannot slow down or fail
data capture.

---

## 6. Sequence: approval, with the cross-hazard refusal

```mermaid
sequenceDiagram
    autonumber
    actor SD as supervisor.drought
    actor SF as supervisor.flood
    participant GW as gateway
    participant FS as flood-service

    SD->>GW: POST /api/floods/17/approve
    GW->>FS: forward
    FS->>FS: HazardScopeFilter: token hazard = DROUGHT ≠ FLOOD
    FS-->>SD: 403 Forbidden

    SF->>GW: POST /api/floods/17/approve
    GW->>FS: forward
    FS->>FS: HazardScopeFilter: FLOOD == FLOOD ✔
    FS->>FS: HazardAccessPolicy.canReview(user, FLOOD) ✔
    FS->>FS: ApprovalWorkflow.apply(PENDING, APPROVE) → APPROVED
    FS->>FS: persist + audit (APPROVED, actor, note, diff)
    FS-->>SF: 200 OK, status APPROVED
```

---

## 7. Approval state machine

```mermaid
stateDiagram-v2
    [*] --> PENDING : recorder creates
    PENDING --> APPROVED : supervisor approves
    PENDING --> REJECTED : supervisor rejects
    PENDING --> CORRECTIONS_REQUESTED : supervisor requests corrections
    CORRECTIONS_REQUESTED --> PENDING : recorder resubmits
    CORRECTIONS_REQUESTED --> REJECTED : supervisor rejects
    APPROVED --> [*]
    REJECTED --> [*]

    note right of APPROVED
        Terminal and immutable.
        Any edit attempt → 409 Conflict.
        Only APPROVED records are
        visible to the national viewer.
    end note

    note left of CORRECTIONS_REQUESTED
        The only state besides PENDING
        in which the original recorder
        may edit the record.
    end note
```

---

## 8. Security layers

```mermaid
flowchart TB
    REQ["Incoming request"] --> L1

    L1["1. JwtAuthenticationFilter<br/>signature + expiry valid?<br/>builds AuthenticatedUser(role, hazard, ward)"]
    L1 -->|invalid| X1["401 Unauthorized"]
    L1 -->|valid| L2

    L2["2. HazardScopeFilter<br/>does the token's hazard match<br/>the hazard this service owns?"]
    L2 -->|no| X2["403 Forbidden"]
    L2 -->|yes| L3

    L3["3. Spring Security @PreAuthorize<br/>coarse role check on the endpoint"]
    L3 -->|no| X3["403 Forbidden"]
    L3 -->|yes| L4

    L4["4. HazardAccessPolicy<br/>canCreate / canView / canEdit /<br/>canReview / canDelete / canResubmit<br/>ward + ownership + status"]
    L4 -->|no| X4["403 Forbidden"]
    L4 -->|yes| L5

    L5["5. listScope → JPA Specification<br/>list queries filtered in SQL"]
    L5 --> OK["Handler executes"]
```

Layers 1, 2, 4 and 5 run **inside every service**, not at the gateway. The
gateway is a router and a convenience, not a security boundary: a request that
somehow reached `flood-service` directly on port 8082 is checked identically.

---

## 9. Alert dispatch detail

```mermaid
flowchart LR
    subgraph Producer["Hazard service"]
        RULES["*AlertRules.alertReason()"] -->|Optional present| PUB["AlertPublisher"]
        RULES -->|empty| NONE["no alert"]
    end

    PUB -->|"AmqpException → log & continue"| EX{{"exchange<br/>dpdms.alerts"}}
    EX --> Q[["queue<br/>dpdms.alerts.q"]]
    Q -->|listener throws| DLQ[["dead letter<br/>dpdms.alerts.dlq"]]
    Q --> LIS["AlertEventListener"]

    subgraph Consumer["alert-service"]
        LIS --> IDEM{"already dispatched<br/>this eventId?"}
        IDEM -->|yes| SKIP["stop — idempotent"]
        IDEM -->|no| LOOK["RecipientLookup<br/>→ auth-service /internal"]
        LOOK --> FACT["AlertMessageFactory<br/>subject + body"]
        FACT --> DISP["AlertDispatchService"]
        DISP --> EMAIL["EmailSender (JavaMail)"]
        DISP --> WAPP["WhatsAppSender<br/>(Meta Cloud API)"]
        EMAIL --> LOG[("alert_log<br/>SENT / FAILED / SKIPPED")]
        WAPP --> LOG
    end
```

---

## 10. Database schemas

One schema per service. No service reads another service's tables — ever.

```mermaid
erDiagram
    USER_ACCOUNT {
        bigint id PK
        varchar username UK
        varchar password_hash
        varchar full_name
        varchar email
        varchar phone_number
        varchar role
        varchar hazard "null = all hazards"
        varchar ward "null = all wards"
        boolean enabled
        int failed_login_attempts
        datetime locked_until
    }

    FLOOD_INCIDENT {
        bigint id PK
        varchar ward
        varchar district
        varchar province
        datetime occurred_at
        varchar reporter_username
        varchar severity
        varchar status
        double latitude
        double longitude
        double peak_water_level_metres
        varchar river_basin
        int households_displaced
        double area_flooded_hectares
        int inundation_duration_days
        varchar review_comment
        varchar reviewed_by
        datetime reviewed_at
        datetime created_at
        datetime updated_at
        bigint version
    }

    INCIDENT_AUDIT_LOG {
        bigint id PK
        varchar hazard
        bigint incident_id
        varchar action
        varchar from_status
        varchar to_status
        varchar actor_username
        varchar actor_role
        datetime acted_at
        text changes
        varchar comment
    }

    ALERT_LOG {
        bigint id PK
        varchar event_id
        varchar hazard
        bigint incident_id
        varchar recipient
        varchar channel
        varchar status
        varchar reason
        text error_message
        datetime dispatched_at
    }

    FLOOD_INCIDENT ||--o{ INCIDENT_AUDIT_LOG : "audited by (same schema)"
```

`incident_audit_log` exists **in each hazard schema**, so an audit record can
never be orphaned from the incident it describes. `alert_log` lives alone in
`dpdms_alert` and references incidents only by `(hazard, incident_id)`.

The other four hazard tables are structurally identical to `flood_incident`;
only the five indicator columns differ. See each service's
`src/main/resources/db/migration/V1__*.sql` for the exact DDL.

---

## 11. Build-time module graph

```mermaid
flowchart BT
    COMMON["common"]
    AUTH["auth-service"] --> COMMON
    FL["flood-service"] --> COMMON
    DR["drought-service"] --> COMMON
    FI["fire-service"] --> COMMON
    ZO["zoonotic-disease-service"] --> COMMON
    MI["mining-accident-service"] --> COMMON
    AL["alert-service"] --> COMMON
    RP["report-service"] --> COMMON
    DS["dashboard-service"] --> COMMON
    UI["web-ui"] --> COMMON
    GW["gateway"]
    EU["discovery-service"]
```

`gateway` and `discovery-service` deliberately do **not** depend on `common` —
they hold no business rules, so there is nothing for them to share. `web-ui`
depends on `common` for the enums and DTOs but explicitly **excludes**
`DpdmsSecurityAutoConfiguration`, because it authenticates with a server-side
session rather than a bearer token.
