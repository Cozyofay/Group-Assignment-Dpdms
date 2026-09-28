# Getting DPDMS into the group's GitHub repository

The brief marks **commit history**, not just the final code. A repository with
one commit saying "final" scores badly no matter how good the code is. This
guide gets the project into the repo and gives the group a workflow that
produces a believable history in the time remaining.

---

## 1. Move the project into the repository folder

Right now the project lives outside the cloned repo. Fix that first.

Assume the repo was cloned to `C:\Users\Craig\Documents\GitHub\Group-Assignment`
and the project is at `C:\Users\Craig\Downloads\Documents\dpdms-step1-2\dpdms`.

```powershell
# 1. make sure nothing is running, then close IntelliJ

# 2. copy the project into the repo
Copy-Item -Recurse -Force `
  "C:\Users\Craig\Downloads\Documents\dpdms-step1-2\dpdms" `
  "C:\Users\Craig\Documents\GitHub\Group-Assignment\dpdms"

# 3. work from the repo from now on
cd "C:\Users\Craig\Documents\GitHub\Group-Assignment"
```

Then reopen the project in IntelliJ from the **new** location
(`File → Open` → `Group-Assignment\dpdms\pom.xml`). Close the old window so you
never edit the stale copy by accident.

> If you prefer the project at the repo root rather than in a `dpdms` subfolder,
> copy the *contents* of `dpdms\` instead. Either is fine; just be consistent.

---

## 2. Check what Git is about to include

```powershell
git status
```

Before the first commit, confirm two things:

```powershell
# .env must NOT appear here. If it does, your .gitignore is not being picked up.
git status --porcelain | Select-String "\.env$"

# target/ folders must not appear either
git status --porcelain | Select-String "target/"
```

The project's `.gitignore` already excludes `.env`, `target/`, `.idea/`,
`*.iml` and `logs/`. If those are showing up, the `.gitignore` is in the wrong
folder — it must sit beside `pom.xml`.

**If `.env` was ever committed, the JWT secret and database password are in the
history.** Remove it and rotate both values:

```powershell
git rm --cached dpdms/.env
git commit -m "Remove .env from version control"
```

---

## 3. The first commits

Do **not** make one giant commit. Stage the project in logical pieces so the
history reflects how the system is structured. This is honest — these really
are separable units of work — and it reads far better to a marker.

```powershell
cd "C:\Users\Craig\Documents\GitHub\Group-Assignment"

git add dpdms/pom.xml dpdms/.gitignore dpdms/.env.example dpdms/docker-compose.yml dpdms/infrastructure
git commit -m "Add Maven parent POM, database script and local infrastructure"

git add dpdms/common
git commit -m "Add shared module: security filters, access policy, approval workflow, audit"

git add dpdms/discovery-service dpdms/gateway
git commit -m "Add Eureka discovery service and Spring Cloud Gateway with combined Swagger UI"

git add dpdms/auth-service
git commit -m "Add auth-service: JWT login, account lockout, user administration, demo seeding"

git add dpdms/flood-service
git commit -m "Add flood-service: CRUD, five indicators, approval workflow, alert rules"

git add dpdms/drought-service
git commit -m "Add drought-service"

git add dpdms/fire-service
git commit -m "Add fire-service"

git add dpdms/zoonotic-disease-service
git commit -m "Add zoonotic-disease-service"

git add dpdms/mining-accident-service
git commit -m "Add mining-accident-service"

git add dpdms/alert-service
git commit -m "Add alert-service: RabbitMQ consumer, email and WhatsApp channels, delivery log"

git add dpdms/report-service
git commit -m "Add report-service: PDF, DOCX, XLSX and CSV generation"

git add dpdms/dashboard-service
git commit -m "Add dashboard-service: cross-hazard aggregation and map points"

git add dpdms/web-ui
git commit -m "Add Thymeleaf front end with Leaflet map"

git add dpdms/README.md dpdms/docs dpdms/start-all.ps1 dpdms/stop-all.ps1 dpdms/start-all.sh dpdms/stop-all.sh
git commit -m "Add README, architecture and API documentation, and startup scripts"

git push origin main
```

If your default branch is `master` rather than `main`, adjust the last line.
`git branch --show-current` tells you which you are on.

---

## 4. Getting the other four members into the history

The brief expects contributions from everyone, and a repository where one
account made every commit is a red flag in a *group* assignment. Two honest ways
to fix that:

**Best: each member does real work on their own service.** From here on, the
owner of each hazard service makes the commits for it. There is plenty left to
do that is genuinely theirs:

- write two or three extra tests for their own service
- tune their alert thresholds and justify the numbers in a comment
- fill in their row in the README's team table
- write their section of the presentation notes
- add Javadoc to their service class explaining the scoping calls

Each of those is a real commit under their own name.

**Also acceptable: pair work, recorded honestly.** If two of you sat at one
machine, record it:

```powershell
git commit -m "Tune drought alert thresholds against ZINWA guidance

Co-authored-by: Full Name <email@example.com>"
```

What you should *not* do is fake commits from accounts that did nothing. It is
easy to spot (a burst of commits at 2 a.m. the night before, all from one IP)
and it is exactly the thing the peer evaluation form exists to catch.

---

## 5. A workflow for the days that are left

Small, frequent, described commits beat a nightly dump.

```powershell
git pull --rebase origin main    # always, before you start
# ...work...
git add <specific files>         # not "git add ." — you will commit junk
git commit -m "Short description of what changed and why"
git push origin main
```

Branches are optional for a group this size and this deadline, but if you use
them:

```powershell
git checkout -b feature/drought-tests
# work, commit
git push -u origin feature/drought-tests
# open a pull request on GitHub, have a teammate review it, merge
```

Two or three reviewed pull requests in the history is strong evidence of
collaboration, and costs about ten minutes each.

### Commit message style

Good: `Reject cross-hazard approval attempts in FireIncidentService`
Good: `Add integration test for Ward 2 recorder reading Ward 1 flood records`
Bad: `update`, `fix`, `changes`, `final`, `final2`, `FINAL FINAL`

---

## 6. Before you submit

```powershell
# clone your own repo into a clean folder and build it from scratch
cd $env:TEMP
git clone https://github.com/<your-org>/<your-repo>.git dpdms-check
cd dpdms-check\dpdms
copy .env.example .env
# edit .env: JWT_SECRET, INTERNAL_API_KEY, DB_PASSWORD
mvn clean install
```

If that fails, the marker's checkout will fail too. The usual causes are a file
that was never `git add`-ed, or something the build needs that is sitting in
`.gitignore`.

Final checklist:

- [ ] `mvn clean install` succeeds from a fresh clone
- [ ] `.env` is **not** in the repository, `.env.example` **is**
- [ ] `README.md` renders correctly on GitHub (check the tables)
- [ ] The Mermaid diagrams in `docs/architecture.md` render on GitHub
- [ ] The team table in `README.md` §23 is filled in
- [ ] `git shortlog -sn --all` shows all five members
- [ ] All five members' peer evaluation forms are completed
- [ ] The repository is accessible to the lecturer (public, or the lecturer
      added as a collaborator)
