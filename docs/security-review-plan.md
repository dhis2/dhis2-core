# DHIS2 Core — Security Review Plan

> **Related**: [`authorization-review-plan.md`](./authorization-review-plan.md) — dedicated sub-plan for the authorization model (RBAC authorities, object ACL/sharing, org-unit scoping, tracker access, PAT scoping, privilege-escalation ladders). Slots into Phase 1 and Phase 3 of this plan.

## 1. Context & Objectives

DHIS2 Core is a large, long-lived Java/Spring/Hibernate platform used worldwide for health data (including individual patient data via the Tracker). The review targets:

- **Confidentiality** of patient and aggregate health data.
- **Integrity** of data imports, metadata changes, and reporting pipelines.
- **Availability** of the API under adversarial input.
- **Compliance-relevant controls**: authentication, authorization, audit logging, cryptography.

Out of scope (unless explicitly added later): client/web apps shipped outside this repo, third-party plugins, infrastructure hardening of deployments.

---

## 2. Scope

| Layer | Module(s) | Priority |
|---|---|---|
| REST entry points | `dhis-web-api`, `dhis-tracker` | **High** |
| Business logic | `dhis-services/*` (especially `dhis-service-core`, `dhis-service-dxf2`, `dhis-service-analytics`, `dhis-service-acl`, `dhis-service-administration`) | **High** |
| Infrastructure | `dhis-support-hibernate`, `dhis-support-jdbc`, `dhis-support-expression-parser`, `dhis-support-artemis` | **Medium** |
| Domain/entities | `dhis-api` | **Low** (review only for serialization, JPA misuse) |
| Web server | `dhis-web-server` | **Medium** (Spring Security config, filters) |
| Docker / deployment | `docker/`, `docker-compose*.yml`, `Dockerfile` | **Medium** |
| Build / CI | `jenkinsfiles/`, `.github/`, `run-cve-patcher.sh` | **Medium** |
| Tests | `dhis-test-*` | **Excluded** (unless a test reveals real credentials / unsafe patterns used in prod code) |

---

## 3. Threat Model (top risks for DHIS2)

Ranked by likely blast radius for a health data platform:

1. **Broken authorization** — endpoint missing `@PreAuthorize`, ACL bypass, IDOR on tracked entity / data value endpoints.
2. **SQL injection** — raw JDBC, HQL string concatenation, dynamic analytics SQL.
3. **Expression / template injection** — `dhis-support-expression-parser`, indicator/validation rule expressions, notification templates.
4. **Insecure deserialization** — Jackson polymorphic types, Java deserialization in caching/messaging (Artemis).
5. **SSRF** — metadata import from URL, data exchange, event hooks (webhooks), app manager fetching remote apps.
6. **XXE / XML issues** — DXF2 import, SAML, SOAP-style integrations.
7. **Path traversal / arbitrary file write** — app manager (uploaded apps are zip archives), file resources, backup/restore.
8. **Secrets & crypto** — password hashing (`dhis-service-core` user service), OAuth2/JWT signing, credentials at rest, 2FA.
9. **Known-CVE dependencies** — very large transitive tree, legacy versions pinned in parent POMs.
10. **CSRF / session fixation / open redirect** — Spring Security config, login flow, SameSite cookies.
11. **Information disclosure** — stack traces in responses, verbose error handlers, debug endpoints.
12. **Audit / logging gaps** — missing audit records on privileged operations.

---

## 4. Methodology

The review runs in five phases. Each phase produces artefacts checked into `docs/security-review/` so findings accumulate incrementally rather than as one final dump.

### Phase 1 — Reconnaissance & inventory (~½ day)

- Map all HTTP endpoints: grep `@RequestMapping`, `@GetMapping`, `@PostMapping`, etc., and capture (path, method, controller, required authority). Output: `docs/security-review/endpoints.csv`.
- Enumerate Spring Security configuration(s): filter chain order, anonymous paths, permitAll, CSRF exclusions, CORS origins.
- Identify all `@PreAuthorize` / `@Secured` usage and conversely controllers missing auth annotations.
- Identify use of `EntityManager.createNativeQuery`, `JdbcTemplate`, `Session.createSQLQuery`, `String.format` → SQL.
- Identify Jackson configuration: `enableDefaultTyping`, custom `TypeResolver`, `@JsonTypeInfo`.
- Identify every place user input reaches: `Runtime.exec`, `ProcessBuilder`, `URL`, `HttpClient`, `RestTemplate`, `WebClient`, `new File(...)`, `FileInputStream`, `Files.*`, `XMLReader`, `DocumentBuilderFactory`, `SAXParser`, `Transformer`, `ObjectInputStream`.
- Result: a **"hot list"** of files/classes that get manual review in Phase 3.

### Phase 2 — Automated scanning (~1 day)

Run in this order; collect output under `docs/security-review/scans/`.

| Tool | Command / rule set | What it catches |
|---|---|---|
| **opengrep** (installed) | `opengrep scan --config p/java --config p/spring --config p/owasp-top-ten --config p/jwt --sarif -o scans/opengrep.sarif .` | SQLi, SSRF, XXE, path traversal, weak crypto, JWT misuse |
| **gitleaks** | `gitleaks detect --no-git --source . --report-path scans/gitleaks.json` | Secrets in working tree (history excluded per scope) |
| **OWASP Dependency-Check** | `mvn org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=7 -f dhis-2/pom.xml` | Known CVEs in dependencies |
| **Trivy (fs + config)** | `trivy fs --scanners vuln,secret,misconfig --format sarif -o scans/trivy.sarif .` | Dep CVEs, secrets, Dockerfile/compose misconfig |
| **SpotBugs + Find Security Bugs** | Already in build; ensure `findsecbugs` ruleset active and export XML | Deserialization, weak randomness, crypto |
| **Hadolint** | `hadolint .devcontainer/Dockerfile docker/*/Dockerfile` | Dockerfile issues |
| **Semgrep custom rules** | Rules for DHIS2-specific patterns (see Phase 3) | Project-specific sinks |

Tools **not yet installed** in the devcontainer — will be added via a PR before Phase 2:

- `gitleaks`, `trivy`, `hadolint`, `dependency-check` CLI (or use the Maven plugin only).

### Phase 3 — Manual / assisted code review (~3–5 days)

Work through the hot list from Phase 1, driven by the threat model. For each area:

1. Read the code with the threat in mind.
2. Write a custom opengrep rule when a pattern repeats (store under `docs/security-review/rules/`).
3. Document each finding in `docs/security-review/findings/NNN-short-title.md` with: location (`file:line`), CWE, severity (CVSS 3.1), exploit sketch, suggested fix.

Target areas and what to look for:

- **AuthN / AuthZ** — see [`authorization-review-plan.md`](./authorization-review-plan.md) for the full sub-plan (authorities matrix, sharing/ACL, org-unit scoping, tracker access, PAT scoping, escalation ladders).
- **SQL (`dhis-service-analytics`, `dhis-service-dxf2`, any `*Store` using raw SQL)**: grep for `+ ` inside query strings; look for ORDER BY / column names taken from user input (Hibernate `@Query` with `nativeQuery=true`).
- **Expression parser (`dhis-support-expression-parser`)**: what functions are exposed? sandboxing? used anywhere on untrusted input (notification templates, indicators submitted by non-admins)?
- **Deserialization**: Jackson `@JsonTypeInfo` with `Id.CLASS`; `ObjectInputStream` anywhere; Artemis message payload handling.
- **File handling (`dhis-service-core/appmanager`, file resources)**: zip-slip on app upload; MIME sniffing; content-disposition; stored path vs served path.
- **SSRF**: metadata import URL, data exchange target URL, event hook target URL, app store URL, OIDC discovery URL. Check for IP/host allow-listing and redirect handling.
- **XML / XXE**: DXF2 parsing, SAML, any `DocumentBuilderFactory` / `SAXParserFactory` — confirm `FEATURE_SECURE_PROCESSING` and external entity disabling.
- **Crypto**: `PBKDF2`, `BCrypt`, `Argon2` for passwords; random token generation (`SecureRandom`?); JWT algorithm allow-list (no `none`, no RS↔HS confusion); symmetric keys not committed.
- **Session / CSRF**: session fixation on login; `SameSite`; CSRF token scope; logout invalidation; remember-me token storage.
- **Logging**: does anything log full request bodies, JWTs, passwords, or patient identifiers?
- **Flyway migrations (`dhis-support-db-migration`)**: any migration that changes privileges / weakens constraints / seeds default credentials?

### Phase 4 — Dynamic validation — **OUT OF SCOPE**

Skipped per scoping decision. Findings that can only be confirmed dynamically will be marked as such in the report ("unconfirmed — requires dynamic validation") so they can be followed up separately.

### Phase 5 — Triage & reporting (~1 day)

- Consolidate findings into `docs/security-review/report.md` with an executive summary, risk matrix, and remediation priorities.
- De-duplicate (opengrep often fires on the same root cause in 20 files).
- Assign severity using CVSS 3.1; group by CWE for patterns.
- Produce a remediation backlog as GitHub issues (draft locally, create on request).

---

## 5. Deliverables

```
docs/security-review/
├── endpoints.csv                 # all routes + auth requirements
├── hotlist.md                    # files selected for manual review
├── scans/                        # raw tool output (SARIF/JSON)
├── rules/                        # custom opengrep rules written during review
├── findings/                     # one file per finding, CWE + CVSS + PoC + fix
├── report.md                     # final executive report
└── README.md                     # index
```

---

## 6. Tooling Summary

**Already in devcontainer**: opengrep 1.19.0, Maven, Java 17, jq, ripgrep, PostgreSQL, gh.

**To add** (small PR before Phase 2):

- `gitleaks` — git history secret scan
- `trivy` — dep + secret + Dockerfile scanner (single binary)
- `hadolint` — Dockerfile linter

**Rely on Maven plugin, no binary install needed**:

- `dependency-check-maven`, SpotBugs + Find Security Bugs (already wired).

---

## 7. Ground Rules

- **Never run destructive tests** against any environment you didn't explicitly stand up for this review.
- **Every finding must include a concrete `file:line` reference** and — where feasible — a minimal reproducer.
- **Prefer fixing in config over code** when both are possible (e.g., Jackson global settings vs per-class annotations).
- **Do not commit PoC exploits** to the main branch; keep them under `docs/security-review/findings/` and clearly labelled.
- **Flag but don't fix** during this review — remediation PRs happen after triage, so severity and scope aren't pre-decided by whoever fixes first.

---

## 8. Confirmed Parameters

| Parameter | Decision |
|---|---|
| Scope | **Full repo** — all modules listed in §2 |
| Branch | **`master`** |
| Git history secret scan | Out of scope — scan working tree only |
| Dynamic testing (Phase 4) | **Out of scope** — static analysis only |
| Reporting format | Markdown in-repo (`docs/security-review/`), no SARIF export, no PDF |
| Remediation | **Flag only** — no fixes committed during the review; remediation happens in follow-up PRs after triage |
