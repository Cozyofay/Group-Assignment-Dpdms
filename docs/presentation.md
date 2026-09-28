# DPDMS — Group presentation plan

A working structure for the group presentation and live demo. Adjust the timings
to whatever the lecturer allots; the proportions matter more than the minutes.

---

## Before the day

- [ ] Every service builds and starts on **at least two** laptops, not just one.
- [ ] `docker compose up -d` run beforehand; RabbitMQ and Mailpit confirmed up.
- [ ] Demo data seeded and eyeballed — enough incidents that the dashboard and
      map do not look empty.
- [ ] `.env` present on the demo machine with a working `DB_PASSWORD`.
- [ ] Browser tabs pre-opened: front end, Swagger UI, Eureka, Mailpit, RabbitMQ.
- [ ] Zoom the browser to ~125% so the back row can read it.
- [ ] A screen recording of the full demo saved as a fallback in case the
      network, MySQL or the projector misbehaves.
- [ ] Each member has re-read **their own** service's `service/` and
      `web/` packages and can explain them without notes.

---

## Slide outline (about 12 slides)

| # | Slide | Content | Who |
|---|---|---|---|
| 1 | Title | System name, module codes, group members and registration numbers | — |
| 2 | The problem | Rushinga's five hazards; what ward officers do today; why paper and WhatsApp groups fail (no audit trail, no scoping, no aggregation) | Member 1 |
| 3 | What we built | One screenshot of the dashboard; the list of eleven services | Member 1 |
| 4 | Architecture | The component diagram from `docs/architecture.md` §2 | Member 2 |
| 5 | Why micro-services | One database per hazard; a change to flood cannot break drought; services scale and fail independently; Eureka + gateway | Member 2 |
| 6 | The OOP core | The class diagram (§4): `BaseIncident` inheritance, `ScopedIncident` abstraction, `IncidentSummary` polymorphism, encapsulation of status changes | Member 3 |
| 7 | Security model | The four-layer diagram (§8); the sentence that matters: *"enforced in every service, not at the gateway"* | Member 3 |
| 8 | Approval workflow | The state machine (§7) and the audit trail | Member 4 |
| 9 | Alerts | The dispatch diagram (§9); thresholds per hazard; why it is asynchronous; the delivery log | Member 4 |
| 10 | Reports and dashboard | The four formats; the map; how cross-hazard reads forward the caller's JWT | Member 5 |
| 11 | Testing | 18 test classes; call out `HazardAccessPolicyTest` and the cross-scope 403 tests | Member 5 |
| 12 | Challenges and what we learned | Be specific and honest — the Actuator filter-chain ordering bug, the YAML corruption we solved by switching to flat properties, keeping five services consistent without copy-paste drift | All |

Keep slides to headings and diagrams. The demo does the talking.

---

## Live demo script (about 6 minutes)

Rehearse this until it runs without hesitation. The order is deliberate: each
step sets up the next.

**1. Record an incident (40 s)**
Log in as `recorder.flood.ward1`. New incident → the form offers **only** Flood
and the ward is fixed to Ward 1. Enter 2.4 m peak level, 60 households.
Save. Status **PENDING**.

> Say: *"Notice I was never offered another hazard or another ward. That is the
> UI being helpful — but it is not what stops me."*

**2. The alert already fired (30 s)**
Switch to the Mailpit tab, refresh. The supervisor's email is there. Then the
*Alerts* page in the UI: one `SENT` row, with the reason
"Peak water level of 2.4 m is at or above the 2.0 m danger threshold".

> Say: *"2.4 is above our 2.0 m threshold, so the service published an event to
> RabbitMQ. The save returned immediately — the email happened afterwards, on a
> different thread."*

**3. Break the UI's assumptions (60 s) — the most important 60 seconds**
Open Swagger UI. Log in via `POST /api/auth/login` as the same Ward 1 recorder,
authorize with the token, then:

- `GET /api/droughts` → **403**. Wrong hazard.
- `GET /api/floods?ward=Ward 2` → **empty list**, not someone else's data.
- `GET /api/floods/{a Ward 2 id}` → **403**.

> Say: *"That is the backend refusing, with the front end completely out of the
> picture. Scoping is a filter plus a policy class inside every service, and the
> list query is filtered in SQL — I cannot receive another ward's rows at all."*

**4. Review and correct (60 s)**
Log in as `supervisor.flood`. The incident is in the queue. *Request
corrections* with a note. Back as the recorder: the record is editable again,
fix a field, *Resubmit* → **PENDING**.

**5. Approve, then show the audit trail (45 s)**
As `supervisor.flood`, *Approve* → **APPROVED**. Open the audit tab: created,
corrections requested with the note, updated with the field-level diff,
resubmitted, approved — each with who and when.

**6. A supervisor from the wrong hazard (20 s)**
Log in as `supervisor.drought`, try to approve the same flood incident → **403**.

> Say: *"Supervisors are bound to exactly one hazard. The drought supervisor
> cannot even read this record, let alone sign it off."*

**7. National view (40 s)**
Log in as `national`. Dashboard: counts by hazard, severity and ward; the trend
line; the map. Click a marker. Then attempt any edit → **403** everywhere, and
note that only APPROVED records are visible.

**8. Reports (40 s)**
*Reports* → filter to Flood → download PDF, then XLSX. Open one; the numbers
match the dashboard.

**9. Resilience (25 s)**
Stop `report-service`. Click download again → the gateway's circuit breaker
returns a friendly message instead of hanging. Restart it, show recovery.

---

## Questions to be ready for

| Likely question | The answer |
|---|---|
| *Why not one database for everything?* | Shared tables couple services: a schema change to floods would force a redeploy of drought. One schema per service is the standard micro-service boundary, and it is why we can develop five hazards in parallel. |
| *Isn't the gateway doing your security?* | No — it routes and load-balances. The JWT filter, the hazard filter and the access policy run inside every service. A request hitting port 8082 directly is checked identically. Slide 7 and the Swagger demo prove it. |
| *What stops someone editing their ward in the token?* | The token is signed with HS384. Change one character of the payload and the signature no longer verifies, so the request is rejected as unauthenticated. |
| *Why Thymeleaf and not React?* | The JWT never reaches the browser — it lives in the server-side session — so there is no `localStorage` token to steal via XSS. Also one build, no CORS, and the UI uses the same access policy class as the backend so they cannot disagree. |
| *How is this object-oriented and not just Spring?* | `BaseIncident` (inheritance), `ScopedIncident` (abstraction — the policy knows nothing about floods), `IncidentSummary` (polymorphic handling of five types in the report and dashboard services), and encapsulation: there is no setter that moves a record to APPROVED without going through `ApprovalWorkflow` and writing an audit row. |
| *What happens if RabbitMQ is down?* | Nothing breaks. `AlertPublisher` catches the exception and logs it; the incident still saves. Data capture is never held hostage to the messaging layer. |
| *Could a report leak data?* | No. `report-service` has no database. It calls each hazard service forwarding **your** token, so the owning service applies your scope. If it refuses, that hazard is skipped. |
| *How do you know the scoping works?* | `HazardAccessPolicyTest` covers every role × hazard × ward × status combination, and each hazard service has integration tests asserting 403 for cross-ward and cross-hazard access. Plus the live demo in step 3. |
| *What would you do differently?* | Honest answer: generate the five hazard services from one template earlier instead of after hand-writing flood; add refresh tokens; add rate limiting at the gateway; put the five services behind a shared integration-test harness. |

---

## Division of labour

Each member owns one hazard service end-to-end and **must** be able to walk
through their own `service/` class and explain the scoping and workflow calls in
it. Expect the examiner to ask.

| Member | Reg. no. | Hazard service owned | Shared area led | Slides |
|---|---|---|---|---|
| | | flood-service | | 2, 3 |
| | | drought-service | | 4, 5 |
| | | fire-service | | 6, 7 |
| | | zoonotic-disease-service | | 8, 9 |
| | | mining-accident-service | | 10, 11 |

Fill this in before submission — it is the table the peer evaluation is checked
against.
