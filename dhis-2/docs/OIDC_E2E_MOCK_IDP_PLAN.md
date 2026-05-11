# OIDC End-to-End Testing with a Mock IdP — Implementation Plan

**Status:** proposal — not implemented yet.
**Owner:** Morten Svanæs (`@netroms`).
**Target version:** 2.44+ (initial framework); features added incrementally.
**Related:** PR #23839 (DHIS2-20043 eSignet signed-JWT userinfo).

---

## Goals

1. Be able to run a real end-to-end test against DHIS2's **Relying-Party** OIDC
   stack — i.e. `oauth2.login.enabled=on` + `oidc.provider.<id>.*` configs —
   without depending on any third-party IdP (Google, Azure, eSignet sandbox).
2. Cover **every OIDC feature DHIS2 supports as an RP**, not just the
   signed-JWT-userinfo path being shipped now: standard JSON userinfo,
   signed-JWT userinfo, `private_key_jwt` client auth, PKCE, scope/claim
   variations, logout/end-session, error paths.
3. Keep it cheap to add coverage: one mock-IdP service, multiple test classes,
   per-test runtime configuration of how the IdP responds.

This plan **does not** cover DHIS2-as-Authorization-Server testing. That is
already done by `dhis-2/dhis-test-e2e/.../oauth2/OAuth2Test.java` against the
Spring Authorization Server.

## Non-goals

- Running a real eSignet stack in CI — too heavy (Postgres + Kafka + mock
  identity system).
- Testing real Google / Azure / Keycloak. Those are integration concerns,
  out of scope for unit-level CI.
- UI/UX testing of the login button rendering — already covered separately
  via the `uitests` profile.

---

## Why a custom mock IdP

No off-the-shelf mock OIDC IdP supports signed-JWT userinfo
(`application/jwt`):

| Project | Image | Signed-JWT userinfo | private_key_jwt | License |
|---|---|---|---|---|
| navikt/mock-oauth2-server | `ghcr.io/navikt/mock-oauth2-server` | ❌ JSON only | ✅ | MIT |
| Soluto/oidc-server-mock (IS4) | `ghcr.io/soluto/oidc-server-mock` | ❌ (upstream IS4 lacks it) | ✅ | Apache-2 |
| oauth2-proxy/mockoidc | — | ❌ | partial | MIT |
| MOSIP eSignet | `mosipid/esignet` | ✅ (signed-then-encrypted) | ✅ | MPL-2.0 |
| WireMock + init script | `wiremock/wiremock` | ✅ (DIY) | ✅ (DIY) | Apache-2 |

Real eSignet works but pulls in Postgres + Kafka + the mock-identity-system —
two orders of magnitude too heavy for a CI job.

WireMock works but cannot sign at request time, so still needs an init
script and shell glue. Net cost is similar to writing it in Java.

**Decision: write a small standalone Java service.** It reuses Nimbus
JOSE+JWT 10.9 (already on the e2e classpath, same lib DHIS2 itself verifies
with — so signing semantics match exactly). About 150–250 lines.

---

## Mock IdP design

### Surface

The service exposes the standard OIDC endpoints:

| Path | Method | Purpose |
|---|---|---|
| `/.well-known/openid-configuration` | GET | Discovery document |
| `/jwks` | GET | Public keys (auto-rotated optionally) |
| `/authorize` | GET | 302-redirects to `redirect_uri` with `?code=…&state=…` |
| `/token` | POST | Exchanges code → access_token + id_token (+ refresh_token) |
| `/userinfo` | GET | Returns JSON **or** signed-JWT based on `Accept` header / per-realm config |
| `/end_session` | GET | Spec-compliant end-session endpoint |
| `/admin/scenario` | POST | **Test-only** — set the next response shape per realm |
| `/admin/keys/rotate` | POST | **Test-only** — force key rotation |

The `/admin/*` endpoints are how tests reconfigure the IdP at runtime
without restarting the container. Each scenario covers one realm (a
provider id), so multiple tests can run against the same image with
isolated config.

### Realms / providers

The mock IdP supports multiple realms in one process. Each test class
"owns" a realm name (e.g. `esignet`, `azure-fake`, `google-fake`) and POSTs
a scenario to `/admin/scenario/{realm}` in `@BeforeEach`. DHIS2 sees them as
independent providers via separate `oidc.provider.<realm>.*` configs.

Realm config controls:

- `userinfo_response_type`: `json` or `jwt`
- `userinfo_signing_alg`: `RS256` / `PS256` / `ES256` / …
- `userinfo_claims`: the JSON object the userinfo returns (so tests pick
  the mapping value)
- `id_token_claims`: extra claims merged into the id_token (so tests can
  validate `sub`, `email_verified`, `aud`, `iss`, custom claims)
- `id_token_signing_alg`: keep the id_token alg the same as userinfo by
  default; allow override for negative tests
- `require_pkce`: true/false
- `require_private_key_jwt`: true/false. When true, `/token` rejects
  `client_secret_*` and verifies the client's `private_key_jwt` assertion
  against the client's registered public JWK.
- `client_secret`: optional shared secret for `client_secret_basic` /
  `client_secret_post`
- `client_jwks_uri`: optional URL to fetch the client's public JWKS from
  (so the IdP can verify `private_key_jwt` assertions issued by DHIS2)
- `next_error`: inject a specific error response (HTTP 400/500, malformed
  JWT, expired JWT, bad-key-signed JWT, missing claim …) to drive the
  negative-path tests

### Keys

- The mock IdP generates RSA-2048 keys on boot. JWKS exposes the public
  key.
- Test code that needs to issue *its own* keys (e.g. to validate
  DHIS2's `private_key_jwt`) does so in the e2e module and registers the
  public JWK with the mock IdP via `/admin/scenario`.
- A `/admin/keys/rotate` endpoint generates a new keypair and serves the
  union of old+new from `/jwks` for one minute, then drops the old one —
  lets tests assert DHIS2 handles JWKS rotation correctly (Nimbus's
  built-in remote-key cache should pick it up without restart).

### Why test-only admin endpoints over startup config

Easier to write fast, hermetic tests. A scenario PUT per test class is
~10 ms and keeps the IdP container reused across the suite. The
alternative (env var per realm, restart container per test class) is
slow and brittle.

---

## Test framework

### Base class

```java
abstract class OidcRpE2ETest extends BaseE2ETest {

  protected MockIdpClient idp;   // talks to /admin/* on the mock-idp service
  protected String realm;        // unique per test class

  @BeforeEach void setUpRealm() {
    realm = "realm-" + getClass().getSimpleName().toLowerCase();
    idp.resetRealm(realm);
  }

  protected void givenUserInfoJwt(String sub, Map<String, Object> claims) { ... }
  protected void givenUserInfoJson(Map<String, Object> claims) { ... }
  protected void givenIdToken(Map<String, Object> claims) { ... }
  protected void rotateJwks() { idp.post("/admin/keys/rotate"); }

  protected String performAuthCodeFlow(String dhis2Username) { ... }
  // ↑ returns the bearer cookie / JWT for /api/me asserts
}
```

`performAuthCodeFlow` follows the redirects without rendering a UI: hit
`<dhis2>/oauth2/authorization/<realm>` → follow 302 to mock-idp → mock-idp
auto-redirects back with a code (no login UI needed since the scenario
already says "who" is logging in) → DHIS2 completes the code-for-token
exchange, calls `/userinfo`, resolves the local user.

### Test classes

One class per OIDC feature. Each owns its realm and config.

| Test class | Realm | Asserts |
|---|---|---|
| `OidcJsonUserInfoTest` | `realm-jsonuserinfo` | Standard `application/json` userinfo path. Sub-class verification, mapping claim, disabled-user error. |
| `OidcSignedJwtUserInfoTest` | `realm-jwtuserinfo` | This PR's path. `Accept: application/jwt`, signature verified, mapping claim resolved. Includes bad-key / expired-JWT / missing-claim negatives. |
| `OidcPrivateKeyJwtTest` | `realm-pkj` | `client_authentication_method=private_key_jwt`. DHIS2 issues a signed client assertion, mock IdP verifies it against DHIS2's `/api/publicKeys/.../jwks.json`. |
| `OidcPkceTest` | `realm-pkce` | `enable_pkce=on`. Mock IdP rejects the `/token` call without a `code_verifier`. |
| `OidcJwksRotationTest` | `realm-rotate` | Rotate IdP keys mid-session; assert next login succeeds without restarting DHIS2. |
| `OidcEndSessionTest` | `realm-logout` | Login, hit DHIS2 logout, assert browser redirects to `end_session_endpoint` and cookies are cleared. |
| `OidcLinkedAccountsTest` | `realm-linked` | Single IdP sub → two DHIS2 accounts (linked_accounts feature). |
| `OidcMappingClaimVariationsTest` | `realm-mapping` | `mapping_claim=email`, `=sub`, `=preferred_username`. Asserts the right local lookup for each. |
| `OidcErrorPathsTest` | `realm-errors` | Mock IdP responds 500 / malformed JWT / missing iss / wrong aud. Each maps to the documented DHIS2 error code. |

All tagged `@Tag("oauth2tests")` so they run as part of the default
api-tests job. `@Tag("oauth2tests")` is currently not gated by any
surefire profile, so registering the tests is enough — no pom changes.

---

## Repository layout

```
dhis-2/
├── dhis-test-e2e/
│   ├── mock-idp/                                           # NEW
│   │   ├── pom.xml                                         # tiny module, Jib for image build
│   │   ├── Dockerfile                                      # (or Jib-only)
│   │   └── src/main/java/org/hisp/dhis/mockidp/
│   │       ├── MockIdpApplication.java                     # main() + HttpServer wiring
│   │       ├── Realm.java                                  # per-realm state + keys
│   │       ├── ScenarioStore.java                          # concurrent-safe in-memory store
│   │       ├── handler/AuthorizeHandler.java
│   │       ├── handler/TokenHandler.java
│   │       ├── handler/UserInfoHandler.java                # JSON vs JWT branch
│   │       ├── handler/JwksHandler.java
│   │       ├── handler/DiscoveryHandler.java
│   │       ├── handler/EndSessionHandler.java
│   │       └── handler/AdminHandler.java                   # /admin/scenario, /admin/keys/rotate
│   ├── docker-compose.e2e-oidc.yml                         # NEW — adds mock-idp service + alt dhis.conf
│   ├── config/dhis2_home/dhis-oidc.conf                    # NEW — oidc.provider.* for every realm
│   └── src/test/java/org/hisp/dhis/oidc/                   # NEW package
│       ├── OidcRpE2ETest.java                              # base class
│       ├── MockIdpClient.java                              # talks to /admin/*
│       ├── OidcJsonUserInfoTest.java
│       ├── OidcSignedJwtUserInfoTest.java
│       ├── OidcPrivateKeyJwtTest.java
│       ├── OidcPkceTest.java
│       ├── OidcJwksRotationTest.java
│       ├── OidcEndSessionTest.java
│       ├── OidcLinkedAccountsTest.java
│       ├── OidcMappingClaimVariationsTest.java
│       └── OidcErrorPathsTest.java
└── .github/workflows/run-api-tests.yml                     # MODIFIED — add OIDC overlay job
```

---

## CI wiring

Two options:

1. **Reuse the existing `api-test` job** by always adding the OIDC compose
   overlay. Cost: one extra container in every api-test run (~30 s image
   build + ~5 s startup + ~30 s mock-idp tests). Net delta on a 12-minute
   suite: small.
2. **Separate job** that reuses the built `CORE_IMAGE_NAME` and only adds
   the mock-idp container. Slightly faster feedback per job; doubles the
   matrix complexity.

**Recommendation: option 1.** Simpler, no matrix changes, and the new
tests are useful as a smoke test for every PR anyway (they touch
authentication).

```yaml
# .github/workflows/run-api-tests.yml — modified step
- name: Run api e2e tests with OIDC overlay
  run: |
    docker compose \
      -f dhis-2/dhis-test-e2e/docker-compose.yml \
      -f dhis-2/dhis-test-e2e/docker-compose.e2e.yml \
      -f dhis-2/dhis-test-e2e/docker-compose.e2e-oidc.yml \
      up --exit-code-from test
```

Mock-idp image is built via Jib at the same time as the core image
(parallel goals in the Maven reactor).

---

## `dhis.conf` fragment

Realm-per-provider, one block per test class. Excerpt:

```properties
oidc.oauth2.login.enabled = on

# Standard JSON userinfo realm
oidc.provider.realm-jsonuserinfo.client_id = dhis2-rp
oidc.provider.realm-jsonuserinfo.client_secret = test-secret
oidc.provider.realm-jsonuserinfo.mapping_claim = email
oidc.provider.realm-jsonuserinfo.authorization_uri = http://mock-idp:9000/realm-jsonuserinfo/authorize
oidc.provider.realm-jsonuserinfo.token_uri        = http://mock-idp:9000/realm-jsonuserinfo/token
oidc.provider.realm-jsonuserinfo.user_info_uri    = http://mock-idp:9000/realm-jsonuserinfo/userinfo
oidc.provider.realm-jsonuserinfo.jwk_uri          = http://mock-idp:9000/realm-jsonuserinfo/jwks
oidc.provider.realm-jsonuserinfo.issuer_uri       = http://mock-idp:9000/realm-jsonuserinfo/
oidc.provider.realm-jsonuserinfo.scopes = openid,email

# Signed-JWT userinfo realm (this PR's path)
oidc.provider.realm-jwtuserinfo.client_id = dhis2-rp
oidc.provider.realm-jwtuserinfo.client_secret = test-secret
oidc.provider.realm-jwtuserinfo.mapping_claim = sub
oidc.provider.realm-jwtuserinfo.authorization_uri = http://mock-idp:9000/realm-jwtuserinfo/authorize
oidc.provider.realm-jwtuserinfo.token_uri        = http://mock-idp:9000/realm-jwtuserinfo/token
oidc.provider.realm-jwtuserinfo.user_info_uri    = http://mock-idp:9000/realm-jwtuserinfo/userinfo
oidc.provider.realm-jwtuserinfo.jwk_uri          = http://mock-idp:9000/realm-jwtuserinfo/jwks
oidc.provider.realm-jwtuserinfo.issuer_uri       = http://mock-idp:9000/realm-jwtuserinfo/
oidc.provider.realm-jwtuserinfo.scopes = openid
oidc.provider.realm-jwtuserinfo.user_info_response_type = jwt
oidc.provider.realm-jwtuserinfo.user_info_jws_algorithm = RS256

# private_key_jwt realm
oidc.provider.realm-pkj.client_id = dhis2-rp
oidc.provider.realm-pkj.client_authentication_method = private_key_jwt
oidc.provider.realm-pkj.keystore_path     = /opt/dhis2/test-keystores/realm-pkj.p12
oidc.provider.realm-pkj.keystore_password = test
oidc.provider.realm-pkj.key_alias         = realm-pkj
oidc.provider.realm-pkj.key_password      = test
oidc.provider.realm-pkj.jwk_set_url       = http://web:8080/api/publicKeys/dhis2-rp/jwks.json
# ... etc
```

Test keystore generation lives in `dhis-test-e2e/scripts/gen-test-keys.sh`
and runs at image-build time (so the keystore is baked into the `web`
image alongside `dhis-oidc.conf`).

---

## Phased rollout

| Phase | Scope | Effort |
|---|---|---|
| **0** | Land this plan doc + the in-process WireMock-style integration test (`SignedJwtUserInfoLoaderHttpIntegrationTest`) already done in PR #23839. | done |
| **1** | Build the mock-idp module (skeleton: `/authorize`, `/token`, `/userinfo` JSON path, `/jwks`, `/admin/scenario`). Wire compose overlay. Land `OidcJsonUserInfoTest` (golden path). | ~1 day |
| **2** | Add the JWT userinfo branch to `/userinfo` + the negative-path support (bad key, expired, missing claim). Land `OidcSignedJwtUserInfoTest`. | ~½ day |
| **3** | Add `private_key_jwt` verification at `/token`. Generate test keystore at image-build. Land `OidcPrivateKeyJwtTest`. | ~½ day |
| **4** | PKCE, JWKS rotation, end-session, mapping-claim variations, linked accounts, error paths. One test class at a time. | ~2–3 days total |
| **5** | (Optional) JWE / nested signed-then-encrypted userinfo, if/when DHIS2 grows support for it. | TBD |

Each phase is independently shippable. Phase 1 alone justifies the
infrastructure — it gives us a real RP smoke test that currently doesn't
exist anywhere.

---

## Open design decisions

1. **Per-realm path prefixing.** Above examples use
   `http://mock-idp:9000/<realm>/authorize`. Cleaner than query-param
   routing. Discovery doc at `…/<realm>/.well-known/openid-configuration`
   needs to surface the realm-prefixed URLs.
2. **Selenium or no Selenium?** Mock IdP auto-redirects, no UI clicks
   required. Recommend pure REST flow following 302s with
   `RestTemplate(disableRedirect=true)`. Selenium only useful if we want
   to assert browser-cookie state. Defer that to a later UI-test pass.
3. **State across redirects.** DHIS2's `oauth2Login` uses session cookies
   to carry `state`/`nonce`. The test client needs to preserve cookies
   across the 302-chain. `RestTemplate` with a `BasicCookieStore`-backed
   Apache HttpClient handles this.
4. **`@Tag("oauth2tests")` vs new tag.** Reusing the existing tag keeps
   things simple; the existing `OAuth2Test` is fast and runs in the same
   job. If the OIDC suite grows beyond ~30 seconds, split into
   `@Tag("oidc")` and add a profile.
5. **Image size / cold-start.** Aim for <50 MB image (Alpine + JRE
   slim + ~250 KB jar). Mock IdP cold-start should be <2 s.

---

## What this does NOT change in the existing codebase

- `OAuth2Test.java` (DHIS2-as-AS test) is unaffected. Different flow,
  different concerns; keep it as-is.
- Production code in `dhis-services/dhis-service-core/security/oidc/*`
  needs no changes. The mock IdP speaks standard OIDC; DHIS2 doesn't
  know it's a mock.
- The new `SignedJwtUserInfoLoaderHttpIntegrationTest` (PR #23839) stays
  — it's faster per-PR feedback at the unit level. The e2e suite is
  additive.

---

## Acceptance criteria for the framework (phase 1 completion)

1. `docker compose -f docker-compose.yml -f docker-compose.e2e.yml -f docker-compose.e2e-oidc.yml up`
   starts cleanly on a developer laptop and in CI.
2. `OidcJsonUserInfoTest` passes against the mock IdP without using any
   real external service.
3. The mock-idp container starts in <3 s, image is <50 MB.
4. Adding a new test class for a new OIDC feature requires: one test
   class, one `oidc.provider.<realm>.*` block, zero changes to the mock
   IdP code (everything driven via `/admin/scenario`).

---

## References

- PR #23839 — DHIS2-20043 — signed-JWT userinfo (this branch)
- Plan: `dhis-2/docs/superpowers/plans/2026-05-06-esignet-oidc-jwt-userinfo.md`
- Spec: `dhis-2/docs/superpowers/specs/2026-05-06-esignet-oidc-integration-design.md`
- Hardening follow-ups: `dhis-2/docs/OAUTH2_JWT_USERINFO_HARDENING.md`
- Existing RP code: `dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/`
- Existing AS-side test (reference style): `dhis-2/dhis-test-e2e/src/test/java/org/hisp/dhis/oauth2/OAuth2Test.java`
- Survey notes: see PR thread #23839 comments
