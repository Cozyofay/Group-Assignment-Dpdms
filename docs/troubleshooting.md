# DPDMS — Troubleshooting

Every problem in this file was actually hit while building the system. Each
entry says what you see, what is really happening, and what to do.

---

## Build problems

### `Connection reset` or `Permission denied: getsockopt` during `mvn`

```
Could not transfer artifact org.springframework.boot:spring-boot-starter-parent:pom:3.3.5
from/to central (https://repo.maven.apache.org/maven2): Connection reset
```

Maven cannot reach Maven Central. It is almost never a Maven problem — it is a
firewall, antivirus or captive-portal problem.

1. Allow `java.exe` (the JDK you are using, not just any Java) through Windows
   Defender Firewall, both private and public networks.
2. Temporarily disable "web shield" / "HTTPS scanning" in third-party antivirus.
3. Retry with `mvn -U clean install` (forces a re-check of failed downloads).
4. If you are on a university network behind a proxy, add it to
   `~/.m2/settings.xml`.
5. Last resort: build once on a different network (mobile hotspot). After the
   first successful build the dependencies are cached in `~/.m2` and you can
   build offline with `mvn -o clean install`.

### `Could not resolve dependencies ... common:jar:1.0.0`

You built a single module without installing `common` first. Always run
`mvn clean install` from the **project root**, not from inside a service folder.

### `invalid target release: 21`

IntelliJ is using a different JDK from the one on your PATH.
*File → Project Structure → Project SDK* → set to Temurin 21, and
*Settings → Build Tools → Maven → Importing → JDK for importer* → the same.

---

## Startup problems

### `503 Service Unavailable` from the gateway

The service is running but Eureka has not propagated the registration yet.
Open http://localhost:8761 and wait until the service appears in the
*Instances currently registered* table — usually 30 seconds after startup.
This is expected behaviour, not a bug. Only worry if it never appears.

### The browser cannot reach `localhost` but `127.0.0.1` works

A Windows name-resolution quirk (IPv6 `::1` resolving first while the service is
bound to IPv4). Use `http://127.0.0.1:8090` and friends. Nothing in the
application needs changing.

### `Web server failed to start. Port 8082 was already in use.`

A previous run is still alive. Run `.\stop-all.ps1`, or find it manually:

```powershell
Get-NetTCPConnection -LocalPort 8082 -State Listen | Select-Object OwningProcess
Stop-Process -Id <pid> -Force
```

### `Access denied for user 'dpdms'@'localhost'`

The `DB_PASSWORD` in `.env` does not match the password you gave the `dpdms`
user when you ran `infrastructure/mysql/01-create-databases.sql`. Either fix
`.env`, or reset the MySQL password:

```sql
ALTER USER 'dpdms'@'localhost' IDENTIFIED BY 'the-password-in-your-env';
ALTER USER 'dpdms'@'%'         IDENTIFIED BY 'the-password-in-your-env';
FLUSH PRIVILEGES;
```

### `Unknown database 'dpdms_flood'`

The creation script was not run, or was run against a different MySQL instance
(for example the Docker one while your app points at a local install). Run
`infrastructure/mysql/01-create-databases.sql` against the server named by
`DB_HOST`/`DB_PORT` in `.env`.

### `.env` disappeared / got overwritten

It is git-ignored and easy to lose. Recreate it:

```powershell
copy .env.example .env
```

Then re-enter `JWT_SECRET`, `INTERNAL_API_KEY` and `DB_PASSWORD`. Keep a copy of
those three values somewhere outside the repo (not in Git).

---

## Security and authentication problems

### `401` with header `WWW-Authenticate: Basic`, on an endpoint that should be JWT-protected

**This one is subtle and it bit us during development.** Spring Boot Actuator
contributes its own `SecurityFilterChain` through
`ManagementWebSecurityAutoConfiguration`. If that chain is registered *before*
ours, it wins for some paths and falls back to HTTP Basic, so a valid JWT is
ignored and the browser gets a Basic-auth challenge.

Fixed permanently in `common`:

```java
@AutoConfiguration(beforeName =
    "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration")
public class DpdmsSecurityAutoConfiguration { ... }
```

If you ever see a Basic-auth prompt again, check that this annotation is still
intact and that the service really has `common` on its classpath.

### `403` when you expect `200`

Work down the four layers in order (see `docs/architecture.md` §8):

1. Decode your token at https://jwt.io and read the `role`, `hazard` and `ward`
   claims. Is the token really the one you think it is?
2. Are you calling the right hazard service? A `FLOOD` token gets 403 from
   `/api/droughts` **by design**.
3. Is your ward right? A `Ward 1` recorder cannot touch `Ward 2`.
4. Is the status right? An `APPROVED` record cannot be edited by anyone.

If all four look correct and you still get 403, check the `correlationId` in the
error body and grep the service log for it — the log line says which check
failed.

### `401 Unauthorized` immediately after a service restart

Services were started with different `JWT_SECRET` values (one of them did not
pick up `.env`). Tokens signed by one are rejected by the other. Make sure you
start every service from the project root so `spring.config.import` finds the
same `.env`, then log in again.

### The account is locked

Five failed logins lock it for 15 minutes. Wait, or as `admin` call
`POST /api/users/{id}/reset-password`, which also clears the lockout.

---

## Configuration problems

### `ParserException` / `ScannerException` in the gateway at startup

```
while parsing a block mapping ... expected <block end>, but found '<scalar>'
```

IntelliJ re-indents YAML when you paste into it, and Spring Cloud Gateway's
route lists are indentation-sensitive. We hit this three times in a row, in
three different ways (truncation, re-indentation, then silently empty
`predicates`).

**Fixed by removing YAML from the gateway entirely.** The gateway now uses a
flat `application.properties` with indexed keys:

```properties
spring.cloud.gateway.routes[0].id=auth-service
spring.cloud.gateway.routes[0].uri=lb://auth-service
spring.cloud.gateway.routes[0].predicates[0]=Path=/api/auth/**,/api/users/**
```

There is no indentation to corrupt. **Do not convert it back to YAML.**

### `ConverterNotFoundException: No converter found capable of converting from type java.lang.String to Duration`

A Resilience4j timeout was written as a bare number. Durations need a unit:
`timeout-duration=60s`, not `timeout-duration=60`.

### Swagger UI at the gateway shows only one service, or an empty dropdown

`springdoc.swagger-ui.urls-primary-name` must match one of the
`springdoc.swagger-ui.urls[n].name` values exactly. If it names a service that
is not in the list, the dropdown renders empty.

### Report downloads time out at exactly one second

The gateway's circuit breaker uses a `TimeLimiter` that defaults to 1 second.
Report generation is slower than that. Already configured:

```properties
resilience4j.timelimiter.configs.default.timeout-duration=60s
```

---

## Messaging and alert problems

### No alert email arrives

Work through the chain in order:

1. Did the incident actually meet a threshold? Check the hazard's `*AlertRules`.
   A `LOW` severity flood at 0.4 m is **supposed** to produce nothing.
2. Is RabbitMQ up? http://localhost:15672 (guest/guest). Look at the
   `dpdms.alerts` exchange — is the message count rising?
3. Is `alert-service` running and consuming? Its log prints each received event.
4. Is Mailpit up? http://localhost:8025.
5. Check `GET /api/alerts` — a `FAILED` row with an error message tells you
   exactly which channel broke.

### `Alert publishing skipped` in a hazard service log

RabbitMQ is not reachable. This is deliberate, not an error: `AlertPublisher`
catches `AmqpException` so a broker outage can never fail a recorder's save.
Start RabbitMQ (`docker compose up -d`) and the next incident will publish.

### Messages piling up in `dpdms.alerts.dlq`

The listener is throwing. Read `alert-service`'s log for the stack trace — the
usual cause is auth-service being down so `RecipientLookup` cannot resolve
recipients. Fix that, then re-publish from the RabbitMQ management UI if you
want the dead-lettered alerts delivered.

### Duplicate alert emails

Should not happen — dispatch is idempotent by `eventId`. If it does, check that
you are not running two `alert-service` instances against the same database
with the idempotency check disabled.

---

## Data problems

### `Table 'dpdms_flood.flyway_schema_history' doesn't exist` in a loop

The schema exists but Flyway cannot write to it: the `dpdms` user is missing
`CREATE` privileges. Re-run the `GRANT` statements in
`infrastructure/mysql/01-create-databases.sql`.

### `Validate failed: Migration checksum mismatch for version 1`

You edited a migration file that had already been applied. Never edit an applied
migration — add `V3__...sql` instead. To reset a demo database:

```sql
DROP DATABASE dpdms_flood;
CREATE DATABASE dpdms_flood CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON dpdms_flood.* TO 'dpdms'@'localhost', 'dpdms'@'%';
```

### `ObjectOptimisticLockingFailureException`

Two people edited the same incident at once; `@Version` caught it and the second
save was rejected. Reload the record and re-apply the change. This is correct
behaviour — it is what stops a supervisor's approval from being silently
overwritten by a recorder's edit.

### The demo users did not appear

`DataSeeder` skips seeding if any user already exists. To reseed, empty the
table and restart `auth-service`:

```sql
DELETE FROM dpdms_auth.user_account;
```

---

## Test problems

### Tests pass locally but fail with `Connection refused` on another machine

They should not — integration tests use H2 with Flyway disabled and never touch
MySQL or RabbitMQ. If one does, it is missing the test profile. Check that the
test class carries the same `@ActiveProfiles("test")` as the ones in
`flood-service`.

### `mvn test` is slow

Run one module at a time: `mvn -pl flood-service test`.

---

## Shell / tooling gotcha

While generating this project we created a literal directory called
`{domain,dto,alert,service,web}` because brace expansion is a bash feature and
the shell in use was `sh`. If you ever see a folder with braces in its name,
delete it and use an explicit loop:

```bash
for d in domain dto alert service web; do mkdir -p "src/main/java/.../$d"; done
```
