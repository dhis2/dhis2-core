# DHIS2 Core — Authorization Review Plan

> Sub-plan of [`security-review-plan.md`](./security-review-plan.md). Slots into Phase 1 (inventory) and Phase 3 (manual review) of the master plan.

## Why this is a separate document

DHIS2's authorization model has three stacked layers — role-based authorities, object-level sharing/ACL, and org-unit / program scoping — plus tracker-specific access rules and identity lifecycle concerns. Reviewing these properly needs its own inventory, matrices, and custom rules, which would swamp the master plan.

---

## Scope

Everything the master plan covers, narrowed to authorization flaws:

- Broken authentication (covered here under §5 Identity)
- Broken authorization (primary focus)
- Privilege escalation chains (§6)
- IDOR on object-UID endpoints
- Org-unit and program-scope bypasses
- PAT (personal access token) scoping — **raised-priority area**

Out of scope for this sub-plan (covered elsewhere):
- Injection flaws (SQL, expression, XML) — master plan §4 Phase 3
- Crypto and password storage — master plan §4 Phase 3
- Dependency CVEs — master plan Phase 2

---

## 1. Authority (RBAC) model — "what can this role do?"

### Inventory (Phase 1)

- **Enumerate every `F_*` authority** — confirmed decision: exhaustive, not sampled.
  - Sources: `Authorities` enum / constants classes, every string literal passed to `hasAuthority(...)` / `@PreAuthorize`, every `F_*` string seeded in Flyway migrations.
  - Output: [`docs/security-review/authorities-matrix.md`](./security-review/authorities-matrix.md) with columns: `authority | defined-in | checked-at (file:line) | granted-by-default-to | unlocks (one-line description)`.
- **Extract every auth-gate call site** into `docs/security-review/endpoints-auth.csv`:
  - `@PreAuthorize`, `@Secured`, `hasAuthority`, `hasAnyAuthority`, `isAuthorized`, `currentUserHasAuthority`, `canCurrentUser*` helpers.
- **Map UserRole → authorities** from seed data / migrations. Track historical additions by diffing Flyway migrations.

### What to look for (Phase 3)

- **Controllers with no auth annotation** that aren't explicitly `permitAll` in Spring Security config → implicit SUPERUSER-only in intent, often unguarded in practice.
- **Inconsistent enforcement** across sibling endpoints (one checks, the other doesn't; create vs update drift).
- **SUPERUSER bypass abuse**: `currentUser.isSuper()` shortcut used where it shouldn't be (e.g., returning sharing metadata or audit data to superusers that should be gated differently).
- **Authority self-grant** chains — see §6.
- **Authority string collisions / typos** (`F_DATAELEMENT_PUBLIC_ADD` vs `F_DATAELEMENT_ADD` used interchangeably).
- **Dead authorities** — declared but never checked. Dead authorities can still be *granted*, giving a false sense of restriction.
- **Shadow gates**: authority checked in the controller but not in the service, so other code paths (scheduler, import, analytics) can reach the same operation without the check.

---

## 2. Object-level access (Sharing / ACL)

**Primary module**: `dhis-service-acl` — `AclService`, `SharingService`, `IdentifiableObjectManager`.

### Inventory

- Every `aclService.canRead / canWrite / canManage / canDataRead / canDataWrite` call site.
- Every `IdentifiableObjectManager.get/save/update/delete` call — does the caller check ACL first, or does the manager enforce?
- Default sharing per schema (`dhis-service-schema`).
- Every endpoint where `Sharing` is deserialized from request body.

### What to look for

- **IDOR**: controller endpoints that look up by UID (`/api/<type>/{uid}`) without an ACL gate.
- **TOCTOU**: ACL checked on object A, then object B loaded and mutated.
- **Batch endpoints** (`/api/metadata`, bulk import, `/api/dataValueSets`): ACL enforced per item or only on the request envelope?
- **Sharing self-escalation**: `PUT` a `Sharing` payload on an object to grant yourself write. (`SharingController`, schema-driven sharing updates.)
- **Public-access defaults**: object types that default to `rw------` public — confirm each is intentional.
- **Gist / fields filter leakage**: `fields=*` or the gist API returning objects the user shouldn't see; existence leak via 404 vs 403 timing.
- **Search endpoints**: ACL-filtered at the SQL layer, or post-hoc in Java (enumeration-by-filter possible)?
- **Sharing-bypassed paths**: analytics, aggregate exports, DXF2 — do they check sharing per object or only on the top-level request?
- **Translation / i18n endpoints**: sometimes a back-door to read metadata without ACL.

---

## 3. Org-unit scoping

### Inventory

- User ↔ org-unit sets: `dataCaptureOrganisationUnits`, `dataViewOrganisationUnits`, `teiSearchOrganisationUnits`.
- Every query filtered by org unit — `DataValueStore`, `EventStore`, `TrackedEntityStore`, analytics SQL builders.
- Every call to `CurrentUserUtil.getUserOrgUnits()` / similar helpers.

### What to look for

- **Hierarchy traversal bugs**: user scoped to a sub-OU reads data from a sibling or parent OU.
- **Search scope bypass**: `ou=` query parameter accepted as-is without intersecting with user's allowed set.
- **Capture vs view confusion**: an endpoint accepts capture scope when it should accept view scope, or vice versa.
- **"ALL" org-unit authority**: `F_ORG_UNIT_ADD` / `F_VIEW_ALL_ORGUNITS` semantics — any controller that reads the authority and skips hierarchy checks.
- **Tracker ownership transfer**: `TrackedEntityProgramOwner` reassignment — who can move a TEI into their capture scope, exposing prior data?

---

## 4. Program / Tracker access

**Primary modules**: `dhis-tracker`, `dhis-service-core/program`.

### Inventory

- Program access levels: `OPEN`, `AUDITED`, `PROTECTED`, `CLOSED` — enforcement points.
- Program stage sharing.
- `TrackedEntityAttribute` confidentiality flag usages.
- Ownership model: `TrackedEntityProgramOwner`, temporary ownership (break-the-glass).

### What to look for

- **`PROTECTED` / `CLOSED` bypass**: non-owner accessing enrolled TEIs.
- **Break-the-glass audit gap**: access granted but no audit record written (`TrackedEntityAudit`).
- **Attribute confidentiality**: confidential attribute returned by `/api/trackedEntities?fields=*`, analytics, or CSV export.
- **Program stage completion workflow**: can a user at a wrong stage post data?
- **Event / enrollment ownership vs TEI ownership** — two checks that can drift.
- **Relationship endpoints**: accessing TEI A via a relationship to TEI B the user has access to.

---

## 5. Identity lifecycle & session integrity

**Files**: Spring Security config in `dhis-web-server`, `dhis-web-api` security filters, OAuth2/OIDC config, **PAT (personal access token)** module.

### 5a. PAT (Personal Access Tokens) — **RAISED PRIORITY**

Confirmed focus area. Work items:

- **Locate the PAT module**: identify the service, store, controller, and issuing endpoint (expected under `dhis-service-core` or similar — confirmed in Phase 1).
- **Token format & entropy**: issuer, length, `SecureRandom`, prefix/last-4 exposure, storage form (raw? hash? which hash?).
- **Scope model**: does a PAT carry all the issuing user's authorities, or a reduced scope? If reduced, how is the scope enforced on every request (filter, interceptor)?
- **Scope escalation**: can a PAT created with scope X later be used to access endpoints requiring scope Y because the filter doesn't check scope per request?
- **Authority drift**: if the issuing user gains a new authority after the PAT was minted, does the PAT inherit it?
- **Expiry & revocation**: default TTL, max TTL, revocation on user disable / password change / role change / logout; per-device session vs PAT semantics.
- **Rate limiting & lockout**: different from password auth?
- **Audit**: does every PAT-authenticated request log the PAT id (not the token), so misuse is attributable?
- **IP / referrer binding**: supported? enforced?
- **2FA interaction**: is a PAT a 2FA bypass? If the user has 2FA enabled, can a pre-2FA-created PAT still be used?
- **Listing / disclosure**: the "list my tokens" endpoint — returns hashes only? A partial prefix? Who can list whose?
- **Impersonation interaction**: while a superuser impersonates another user, can they mint a PAT as that user?

PAT findings go in a dedicated block of the report so they're easy to triage.

### 5b. Sessions, JWT, OAuth2, remember-me

- **Session fixation** on login.
- **Remember-me token** storage, rotation, revocation.
- **Logout**: server-side invalidation (not just cookie clear), SSO single-logout behaviour.
- **JWT**: algorithm allow-list (no `none`, no RS↔HS confusion), key rotation, `aud`/`iss` validation, clock skew tolerance.
- **2FA bypass**: endpoints reachable in the half-authenticated state between password and TOTP.
- **CSRF** applicability for non-GET state-changing endpoints (often disabled globally — verify the scope is intentional and alternative protections exist).
- **Impersonation** (`switchUser`, `userOverride`): who can use it? Is the real-user identity preserved in audit?

---

## 6. Privilege-escalation ladders

After §§1–5 produce findings, walk these ladders. Each ladder = one finding even if spanning multiple files — the *chain* is the bug.

- **Low → admin via user update**: any `F_USER_*` authority that lets a user edit a higher-privileged user.
- **Low → admin via role/group update**: `F_USERROLE_UPDATE` to add `ALL` to one's own role; UserGroup membership self-add.
- **Low → data access via sharing change**: `F_METADATA_*` to flip public access on sensitive metadata.
- **Low → data access via org-unit remap**: reassigning org units to include restricted OUs.
- **Low → tracker access via ownership transfer**: moving a TEI into one's scope.
- **Low → code execution via app upload**: `F_APP_MANAGEMENT` — zip-slip, JS served from same origin, CSP scope.
- **PAT → outlives privilege**: PAT minted while user was admin, user demoted, PAT still grants admin (see §5a).

---

## Techniques

- **Endpoint × check matrix**: join `@RequestMapping` paths with `@PreAuthorize` and every `aclService.can*` call in the same method. Gaps = suspect.
- **Custom opengrep rules** under `docs/security-review/rules/authz/`:
  - `controller-uid-no-acl.yml` — controller method taking a UID path variable but never calling `aclService`.
  - `sharing-setter-weak-gate.yml` — sharing setter reachable from a non-admin-guarded endpoint.
  - `super-user-shortcut.yml` — `currentUser.isSuper()` used as a gate without a fallback ACL check.
  - `pat-scope-missing.yml` — PAT-issuing or PAT-validating code paths that bypass scope enforcement.
- **Seed-data diff**: compare `UserRole` seed between Flyway migrations — when were authorities added/removed?
- **Negative-test spec**: for each suspected flaw, document the PoC request steps so a later engineer can confirm (dynamic testing is out of scope in this review).

---

## Deliverables

```
docs/security-review/
├── authorities-matrix.md       # every F_* × where-checked × what-it-unlocks (exhaustive)
├── endpoints-auth.csv          # path, method, controller, @PreAuthorize, ACL call, OU scope, program access
├── sharing-defaults.md         # schema → default publicAccess → implication
├── pat-review.md               # PAT-specific findings & matrix (raised priority)
└── findings/authz-NNN-*.md     # one file per finding
```

The final `report.md` includes an **Authorization** section that summarises the matrix, top risks, and PAT findings as a sub-block.

---

## Confirmed parameters

| Parameter | Decision |
|---|---|
| Authority enumeration depth | **Exhaustive** — every `F_*` catalogued |
| Raised-priority area | **PAT scoping** (see §5a) |
| Document location | Separate file (this one), referenced from master plan |
| Remediation | Flag only, per master plan |

---

## Open questions

None blocking — ready to execute alongside Phase 1 of the master plan.
