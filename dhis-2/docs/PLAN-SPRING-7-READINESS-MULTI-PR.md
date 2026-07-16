<!--
Author: Morten Svanæs
Created: 2026-07-16
AI Assisted
-->

# Spring Framework / Security 7 readiness — multi-PR plan

**Source spike:** [PR #24087](https://github.com/dhis2/dhis2-core/pull/24087) (opened 2026-06-01, author Stian Sandvold) — DRAFT all-in-one Spring 7 upgrade branch. Not mergeable as-is; used as a complete blast-radius map.

**Goal:** Land as much **forward-compatible** Framework/Security migration work as possible on current Spring 6.2 / Security 6.5, so the eventual Spring 7 bump is small, mechanical, and reviewable.

**Non-goal of the early PRs:** shipping Spring Framework 7 itself. That is one atomic later PR after Phase 0 is green.


## Locked decisions (2026-07-16 interview)

| Decision | Choice |
|---|---|
| Start order | **PR-A + PR-D + PR-G in parallel** first |
| Security matcher strategy | **Custom AntPathRequestMatcher drop-in** (PR-E), then PathPattern migration later (PR-K) |
| PKCE after SAS 7 | **Adopt PKCE-by-default** (PR-H / MAJOR) — do **not** ship `requireProofKey(false)` as the long-term default; BC opt-out only if a temporary bridge is required for e2e while clients migrate |

### Prep PRs (Phase 0) — MERGED 2026-07-16

| Plan ID | PR | Merged | Notes |
|---|---|---|---|
| PR-A | [#24457](https://github.com/dhis2/dhis2-core/pull/24457) (merged 2026-07-16) | `73acf5734ae` | jakarta lifecycle |
| PR-D | [#24458](https://github.com/dhis2/dhis2-core/pull/24458) (merged 2026-07-16) | `61b06ce26d8` | mechanical FW APIs |
| PR-G | [#24459](https://github.com/dhis2/dhis2-core/pull/24459) (merged 2026-07-16) | `ec638e78ef2` | drop spring-mobile-device |
| PR-B | [#24460](https://github.com/dhis2/dhis2-core/pull/24460) (merged 2026-07-16) | `5ea7503cc90` | method security / 2FA / dead voter |
| PR-E | [#24461](https://github.com/dhis2/dhis2-core/pull/24461) (merged 2026-07-16) | `271844633ff` | custom AntPathRequestMatcher |
| PR-C | [#24462](https://github.com/dhis2/dhis2-core/pull/24462) (merged 2026-07-16) | landed via #24461 | lambda DSL (stacked on E; no `.and()` on master) |
| PR-F | [#24463](https://github.com/dhis2/dhis2-core/pull/24463) (merged 2026-07-16) | `f729c3c16cf` | content negotiation / trailing slash |

**Phase 0 complete on master.**

**PR-MAJOR in flight (2026-07-16):** branch `spring7-pr-major` — FW 7.0.8 / Sec 7.0.6 / AS 7.0.6 / SDR 4.1.0 / Session 4.1.0 / JUnit 6.0.3 + remaining 7-only APIs. Temporary PKCE `requireProofKey(false)` bridge for e2e; adopt PKCE-by-default tracked as PR-H. (atomic Spring Framework 7 + Security 7 + SDR/Session/AS/JUnit 6).





---

## Current baseline (master, 2026-07-16)

| Component | Master | Spike PR #24087 |
|---|---|---|
| Spring Framework | **6.2.18** | 7.0.7 |
| Spring Security | **6.5.10** | 7.0.5 |
| Spring Authorization Server | **1.5.2** | 7.0.5 (merged into Security generation) |
| Spring Data Redis | **2.7.18** | 4.0.1 |
| Spring Session Data Redis | **2.7.4** | 4.0.3 |
| Spring Session Core | **4.1.0** | (aligned) |
| JUnit | **5.12.2** | 6.0.3 |
| `javax.annotation-api` | **1.3.2** (explicit) | `jakarta.annotation-api` 3.0.0 |
| Jackson | **2.21.2** | still 2.x on spike (FW7 prefers 3.x) |
| spring-retry | **2.0.12** | untouched on spike |
| spring-mobile-device | **1.1.5.RELEASE** (BOM only) | removed |

Still present on master (must migrate for Spring 7):

- **42 files** import `javax.annotation.PostConstruct` / `PreDestroy`
- `WebMvcConfig` still uses `@EnableGlobalMethodSecurity` (tests already use `@EnableMethodSecurity`)
- `DhisWebApiWebSecurityConfig` still uses `.and()` chaining + Spring `AntPathRequestMatcher`
- `TwoFactorAuthenticationProvider` still calls `setUserDetailsService(...)` (removed no-arg ctor path in Security 7)
- dead `AllRequiredRoleVoter` (extends removed `RoleVoter`)
- `UriComponentsBuilder.fromHttpUrl` in SMS gateway
- `AsyncListenableTaskExecutor` / `ListenableFutureCallback`
- `favorPathExtension(true)` content negotiation
- Hibernate property string `org.springframework.orm.hibernate5.SpringSessionContext`

---

## Official upgrade signals (research summary)

### Spring Framework 7.0 ([release notes](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-7.0-Release-Notes))

Hard removals that hit DHIS2:

1. **`javax.annotation` / `javax.inject` no longer honoured** — only `jakarta.*`. Silent lifecycle failure if left on `javax`.
2. **Path suffix / trailing-slash MVC options removed** — `favorPathExtension`, `setUseSuffixPatternMatch`, `setUseTrailingSlashMatch`, `PathExtensionContentNegotiationStrategy`.
3. **`ListenableFuture` removed** → `CompletableFuture`.
4. **`HttpHeaders` no longer implements `Map`/`MultiValueMap`** — use header-specific APIs (`containsHeader`, `headerNames`, `headerSet`).
5. **`SpringExtension` default scope = test method** — restore class scope via `spring.test.extension.context.scope=test_class` if fixtures rely on class lifecycle.
6. **JUnit 6 baseline** for Spring Test.
7. **Hibernate package move** — `orm.hibernate5` → `orm.jpa.hibernate` (`SpringSessionContext` class-name string).
8. **PathMatcher deprecated for removal** — PathPattern is the long-term model; mid-path `/**` support improved in FW7.
9. **Jackson 2.x support deprecated** (prefer Jackson 3.x / `tools.jackson`).
10. **spring-retry folded into `org.springframework.core.retry`** (`RetryTemplate`, `@Retryable`).

### Spring Security 7.0 ([migration guide](https://docs.spring.io/spring-security/reference/7.0/migration/index.html) + 6.5 prep)

Prep path: **stay on Security 6.5 and migrate deprecated APIs first**, then bump.

Hard removals / required rewrites:

1. **HttpSecurity `.and()` removed** → lambda / `Customizer` DSL only.
2. **`AntPathRequestMatcher` / `MvcRequestMatcher` → `PathPatternRequestMatcher`** (or a temporary local Ant-backed matcher if mid-pattern `/**/` cannot be expressed yet).
3. **`AccessDecisionVoter` / `RoleVoter` model removed** → `AuthorizationManager`.
4. **`DaoAuthenticationProvider` no-arg + setters removed** → constructor `super(UserDetailsService)`.
5. **`@EnableGlobalMethodSecurity` infrastructure removed** → `@EnableMethodSecurity`.
6. **Authorization Server packages merged** into Spring Security config packages; versioned with Security 7.
7. **SAS 7 default `requireProofKey=true` (PKCE-by-default)** — behaviour change for OAuth clients.
8. **Jackson 3 modules** for Security serialization (`SecurityJacksonModules`).

Recommended official order: **Security 6.5 prep → Framework/Security 7 atomic bump**.

---

## What PR #24087 already proved (keep / extract)

| Area | Verdict | Land early? |
|---|---|---|
| `javax` → `jakarta` lifecycle annotations | Highest-impact runtime fix | **Yes — PR-A** |
| `@EnableMethodSecurity` | Required for boot on Sec 7; FC on 6.5 | **Yes — PR-B** |
| Security lambda DSL rewrite | Mechanical + security-sensitive | **Yes — PR-C** (with review) |
| Delete `AllRequiredRoleVoter` | Dead code | **Yes — PR-B** |
| `fromHttpUrl` / ListenableFuture / async executor | Mechanical FW prep | **Yes — PR-D** |
| Custom `AntPathRequestMatcher` drop-in | Works on 6.5 and 7; preserves mid-`/**/` | **Yes — PR-C or PR-E** |
| `TwoFactorAuthenticationProvider` ctor | FC on 6.5 if ctor still accepts UDS | **Yes — PR-B** |
| `HibernateEncryptionConfig` ctor init | Defensive, good on 6.2 | **Yes — PR-A companion** |
| Suffix content negotiation redesign | Behavioural public API | **Yes, gated — PR-F** |
| Keep AntPathMatcher via `setPatternParser(null)` | Interim only | **PR-F or major bump** |
| Full version bump FW7+Sec7+SDR4+JUnit6 | Atomic | **Later — PR-MAJOR** |
| OAuth token client RestClient rewrite | Sec 7 only / mostly | **PR-MAJOR** |
| AS package moves | Sec 7 only | **PR-MAJOR** |
| `HttpHeaders` Map removal | FW7 only | **PR-MAJOR** |
| PKCE-by-default adoption (locked 2026-07-16; temp `requireProofKey(false)` bridge only while e2e clients migrate) | Sec/SAS 7 only | **PR-MAJOR + PR-H rollout** |
| DCR scope policy | Sec/SAS 7 behaviour | **PR-MAJOR follow-up** |
| Jackson 3 | Not done on spike | **Separate track** |
| spring-retry → core retry | Not done on spike | **Separate track** |
| Lettuce 7 | Needs SDR 4.1+ | **After major** |

---

## Multi-PR roadmap

Each PR is independently shippable on **current master (Spring 6.2 / Security 6.5)** unless marked otherwise.

```text
Parallel prep lanes on Spring 6.2 / Security 6.5 (independently shippable):

  PR-A  jakarta lifecycle (+ encryption ctor)
  PR-B  method security + dead voter + 2FA provider ctor
  PR-D  Framework mechanical: URI builder, async, ListenableFuture
  PR-G  (optional) spring-mobile-device BOM removal + small dead deps
  PR-E  RequestMatcher abstraction (custom Ant drop-in)
    └─► PR-C  Security filter-chain lambda DSL  [security review]  (E lands first)
  PR-F  Content negotiation / trailing-slash / PathPattern prep  [api-test gate]

All of A–G green
  │
PR-MAJOR  atomic FW7 + Sec7 + SDR4 + Session4 + AS + JUnit6 + remaining 7-only APIs
  │
PR-H  security follow-through: PKCE-by-default rollout, DCR scopes, OIDC RestClient review
  │
PR-I  Jackson 3 track
  │
PR-J  spring-retry → spring-core retry
  │
PR-K  PathPattern controller migration (drop AntPathMatcher interim)
  │
PR-L  Lettuce 7 after SDR 4.1+
```

Dependency note: A–G can largely land in parallel **except** C depends on E if matcher type changes at the same time. Prefer **E before C**, or land custom matcher inside C as one security PR.

---

### PR-A — `javax.annotation` → `jakarta.annotation` lifecycle (do first)

**Why first:** Spring 7 silently ignores `javax` `@PostConstruct`/`@PreDestroy`. On the spike this broke encryption init and most integration tests. Spring 6.2 already honours `jakarta`.

**Scope:**
- Replace imports in all **42** files using `javax.annotation.PostConstruct` / `PreDestroy`
- Swap module deps: `javax.annotation:javax.annotation-api` → `jakarta.annotation:jakarta.annotation-api` (parent BOM + ~16 module poms)
- Keep `javax.annotation.Nonnull` / `CheckForNull` **unless** a later nullness PR migrates to JSpecify (do **not** mix that into this PR)
- Optional companion: `HibernateEncryptionConfig` constructor init (password before `@Bean` methods)

**Out of scope:** any version bumps.

**Tests:** unit + a small integration context that asserts a known `@PostConstruct` ran (e.g. encryption config or `TestBase` static init path).

**Risk:** low. Forward-compatible.

**Files (representative):**
- `pom.xml` (+ module poms declaring annotation-api)
- `HibernateEncryptionConfig`, `TestBase`, merge services, OIDC handlers, etc. (full 42-file list from inventory)

---

### PR-B — Method security + dead voter + 2FA provider ctor

**Why:** Security 7 removes global-method-security infrastructure and voter model; 6.5 already has the replacements.

**Scope:**
1. `WebMvcConfig`: `@EnableGlobalMethodSecurity(prePostEnabled = true)` → `@EnableMethodSecurity` (align with `MvcTestConfig`)
2. Delete unused `AllRequiredRoleVoter` (+ package if empty)
3. `TwoFactorAuthenticationProvider`: pass `UserDetailsService` via `super(detailsService)` instead of `setUserDetailsService` (keep `setPasswordEncoder` if still valid on 6.5)

**Out of scope:** full filter-chain rewrite.

**Tests:** method-security controller tests; login/2FA web-api tests.

**Risk:** low–medium. Method security annotation swap is the boot-blocker on Sec 7.

---

### PR-C — Security filter-chain lambda DSL (security review required)

**Why:** `.and()` is removed in Security 7; already deprecated on 6.5. Spike rewrote `DhisWebApiWebSecurityConfig` (and AS config style).

**Scope:**
- Rewrite `DhisWebApiWebSecurityConfig` to lambda/`Customizer` style **without** changing permit/auth rules
- Same for any remaining `.and()` in `AuthorizationServerConfig` headers/session blocks if present
- Diff should be structural only; produce a side-by-side rule inventory in the PR description

**Security review checklist (must be in PR body):**
- [ ] Every `permitAll` matcher preserved (esp. mid-pattern `/**/loginConfig`, account/auth endpoints)
- [ ] Final `/**` → `authenticated` still last
- [ ] CSRF, CORS, headers, session creation, OIDC login/logout, basic auth, API token filter order unchanged
- [ ] No accidental `authorizeRequests` reintroduction

**Tests:** existing security/web-api auth suites + smoke of public endpoints (`/api/ping`, loginConfig, OAuth authorize).

**Risk:** high blast radius, low intended behaviour change. Review is the gate, not compile.

**Prefer:** land **PR-E matcher abstraction first**, then C only changes DSL shape.

---

### PR-D — Framework mechanical deprecations (non-security)

**Scope:**
- `UriComponentsBuilder.fromHttpUrl` → `fromUriString` (`SimplisticHttpGetGateWay` + tests)
- `AsyncListenableTaskExecutor` → `AsyncTaskExecutor` + `submitCompletable` (`DefaultAsyncTaskExecutor`)
- `ListenableFutureCallback` → `BiConsumer` / `CompletableFuture` (`MessageSendingCallback`)
- Grep for any other `ListenableFuture` / `fromHttpUrl` leftovers

**Risk:** low. Ideal small PR.

**Collision note (Fable review 2026-07-16):** `SimplisticHttpGetGateWay` is also the F5 SMS-gateway SSRF call site
(`security-reports/incoming-2026-06-17/F5-sms-gateway-ssrf.md`, GHSA-hx9p-89pf-3xm5 / `DHIS2-21660`).
Status as of 2026-07-16:
- master still uses `fromHttpUrl` (no SSRF fix landed yet)
- PR-D branch only renames to `fromUriString` — same lines, no host validation
- F5 remediation will almost certainly touch the same `send()` path (egress filter / URL validation)

Coordinate: if F5 lands first, re-base PR-D on the rewritten gateway; if PR-D merges first, F5 must edit
`fromUriString` call sites (not `fromHttpUrl`). Do **not** mix the SSRF fix into PR-D.


---

### PR-E — RequestMatcher abstraction for Security 7

**Problem:** Security 7 removes `AntPathRequestMatcher`. Official replacement is `PathPatternRequestMatcher`, but DHIS2 uses mid-pattern `/**/` (e.g. `/api/**/loginConfig`). Spike introduced a local Ant-backed `org.hisp.dhis.webapi.security.AntPathRequestMatcher`.

**Recommended approach (interim, matches spike):**
1. Add DHIS2 `AntPathRequestMatcher` (AntPathMatcher-backed, servletPath+pathInfo, case-sensitive)
2. Switch all Security call sites + test bases to it
3. Add **unit tests** for every public matcher pattern used in `DhisWebApiWebSecurityConfig` (table-driven: path → match/no-match)

**Follow-up (PR-K):** migrate matchers that PathPattern can express to `PathPatternRequestMatcher`; keep custom only for true mid-`/**/` cases until PathPattern coverage is complete (FW7 improved leading `/**`).

**Risk:** security-critical. Tests are mandatory.

---

### PR-F — Content negotiation + trailing slash (behavioural, api-test gate)

**Why:** Spring 7 removes path-extension negotiation and trailing-slash match flags. DHIS2 public API still uses `/api/x.json` / `.xml` / `.json.zip`.

**Scope (prefer landing on Spring 6 first to isolate behaviour):**
- Replace `favorPathExtension` / `CustomPathExtensionContentNegotiationStrategy` with:
  - handler-mapping literal-first + suffix/trailing-slash fallback (spike final design), **or**
  - servlet filter + strategy (earlier spike design) — **prefer handler-mapping** (spike found filter-only breaks literal `.json` mappings like OpenAPI)
- Wire MockMvc test setups the same way as production
- Document residual cases (`DataSetControllerTest` Content-Disposition) if not fully fixed

**Tests:** `integration-h2` OpenApi + DataSet export + api-test sample of `.json` endpoints.

**Risk:** medium–high public API surface. Do **not** combine with version bump.

---

### PR-G — Dead dependency cleanup

**Scope:**
- Remove unused `spring-mobile-device` from dependencyManagement (no code usage)
- Any other unused Spring-era BOM entries discovered while grepping

**Risk:** trivial.

---

### PR-MAJOR — Atomic Spring 7 generation bump (after A–G)

**Must move together** (validated by spike):

| Property | To |
|---|---|
| `spring.version` | latest 7.0.x patch |
| `spring-security.version` | matching 7.0.x |
| `spring-authorization-server.version` | Security-aligned 7.0.x (or drop separate if fully merged) |
| `spring-data-redis.version` | 4.0.x (or current 4.x line compatible with FW7) |
| `spring-session-core.version` + `spring-session-data-redis.version` | **same spring-session generation / BOM release** (do **not** hardcode data-redis 4.0.x while core stays 4.1.0 — master already has session-core **4.1.0** vs session-data-redis **2.7.4**; MAJOR must realign both) |
| `junit.version` | 6.0.x |
| surefire | set `spring.test.extension.context.scope=test_class` via `systemPropertyVariables` (Spring property / JVM system property — not a surefire arg alone) |

**Spring-7-only code that cannot live on 6.2:**
- `HttpHeaders` non-Map API (`AuthScheme.apply`, `RouteService`, tests)
- OAuth2 `RestClientAuthorizationCodeTokenResponseClient` migration
- Authorization Server package moves + ctor changes
- `ObjectPostProcessor` package move
- `PatternsRequestCondition` boolean-arg removal leftovers
- Hibernate `SpringSessionContext` FQCN → `org.springframework.orm.jpa.hibernate.SpringSessionContext`
- Redis session `RedisTemplate<String,Object>` generics
- Any remaining compile breaks from BOM

**Validation gate (from spike, must re-prove on latest master):**
- unit-test green
- integration-test (Postgres) green
- integration-h2 residual inventory zero or filed
- api-test e2e green
- app boots; method security active
- OIDC smoke against real provider (private_key_jwt)

**Do not merge** until security review of C/E/H is signed off.


---

### PR-H — Security / OAuth behaviour decisions (post-MAJOR or with MAJOR)

Work deferred by the spike (PKCE policy itself is **locked**, see 2026-07-16 interview):

1. **PKCE-by-default rollout (decision locked):** adopt SAS 7 `requireProofKey=true` and migrate clients/login-app flows; a temporary `requireProofKey(false)` bridge is allowed only while e2e clients migrate and must carry a removal task
2. **DCR scopes:** SAS 7 rejects `openid profile username` — define allowed scope set + tests
3. **OIDC token client:** review RestClient path + private_key_jwt
4. **Optional:** migrate more matchers to PathPatternRequestMatcher

Owner: security-aware reviewer (Platform) + OIDC product input for PKCE.

---

### PR-I — Jackson 3 track (separate)

FW7/Sec7 default toward Jackson 3 (`tools.jackson`). Master is Jackson 2.21.2. Spike did not complete this.

**Scope sketch:**
- Jackson BOM 3.x
- Replace `ObjectMapper` builder patterns with `JsonMapper.builder()` where needed
- Spring Security `SecurityJacksonModules` (not Jackson2 modules)
- GeoJSON / JTS datatype compatibility check
- Session/redis serialization compatibility

Can start inventory on Spring 6 if Jackson 3 is supported; otherwise after MAJOR.

---

### PR-J — spring-retry → Spring Framework core retry

FW7 merges retry into `org.springframework.core.retry`. Inventory current `@Retryable` / `RetryTemplate` usages of `spring-retry` and migrate.

Independent of security work.

---

### PR-K — PathPattern migration (drop Ant interim)

After MAJOR is stable:
- Migrate controller `@RequestMapping` patterns that depend on Ant mid-`**` semantics
- Remove `setPatternParser(null)` interim
- Shrink/remove custom Security `AntPathRequestMatcher` where PathPatternRequestMatcher suffices

This is the long-term correctness path; AntPathMatcher is deprecated for removal.

---

### PR-L — Lettuce 7

Only after Spring Data Redis **4.1+**. Out of scope for the first MAJOR.

---

## Suggested landing order (calendar)

| Week | PRs | Notes |
|---|---|---|
| 1 | **A**, **D**, **G** | pure prep, parallelizable |
| 1–2 | **B** | method security + 2FA ctor |
| 2 | **E** then **C** | matcher tests, then DSL rewrite + security review |
| 2–3 | **F** | content negotiation, api-test gate |
| 3–4 | **MAJOR** | version bump only after A–F green |
| +1 | **H** | PKCE/DCR decisions |
| later | **I**, **J**, **K**, **L** | modernization tracks |

---

## Explicit non-goals for early PRs

- Do **not** bump `spring.version` before A–F.
- Do **not** mix Jackson 3 into security DSL PRs.
- Do **not** change public OAuth client defaults (PKCE) without a product/security decision.
- Do **not** broaden nullness migration (`javax.annotation.Nonnull` → JSpecify) inside the jakarta lifecycle PR.
- Do **not** merge Stian's all-in-one PR as-is.

---

## Extraction map from PR #24087 commits

Useful cherry-pick / reimplementation guide (commit subjects from the spike):

| Theme | Spike commits (approx) | Target PR |
|---|---|---|
| HttpHeaders / mechanical FW | early "mechanical API migrations" | PR-D + MAJOR |
| Suffix content negotiation | MediaTypeSuffixFilter / Suffix strategy / handler-mapping | PR-F |
| Security 7 deps + DSL | Security bump commits + lambda rewrite | PR-C + MAJOR |
| Custom AntPathRequestMatcher | "Security config package moves + custom AntPathRequestMatcher" | PR-E |
| 2FA provider ctor | TwoFactorAuthenticationProvider commit | PR-B |
| Delete AllRequiredRoleVoter | dead voter commit | PR-B |
| OAuth RestClient token client | DhisAuthorizationCodeTokenResponseClient | MAJOR |
| JUnit 6 | junit-bom bump | MAJOR |
| Encryption ctor | HibernateEncryptionConfig | PR-A |
| setPatternParser(null) | AntPathMatcher keep | PR-F / MAJOR |
| jakarta PostConstruct | "migrate javax.annotation" + dep swap | PR-A |
| EnableMethodSecurity + SpringSessionContext | later fix commit | PR-B + MAJOR |
| PKCE default (adopt `requireProofKey=true`; temp opt-out bridge only) | OAuth2 client defaults | MAJOR / PR-H |

Re-implement on latest master rather than blind cherry-pick (master has moved since 2026-06-02).

---

## Verification commands (per PR)

```bash
# format
mvn spotless:apply -f ./dhis-2/pom.xml

# compile + unit
cd dhis-2 && mvn -Punit-test test -q

# targeted web-api auth/security
cd dhis-2 && mvn test -pl dhis-test-web-api -Dtest='*Authentication*,*Security*,*Login*'

# after PR-F / MAJOR
# full integration-h2 + api-test e2e as in CI
```

---

## Open decisions (need human input)

1. **DCR allowed scopes** under SAS 7.
2. **Who owns security review** of PR-C/E/H (Platform security reviewer name).
3. **Target DHIS2 version** for MAJOR (master only vs also 2.42?).

Decided 2026-07-16 (see Locked decisions): PKCE-by-default adoption; PR-E matcher strategy (custom Ant drop-in first, PathPattern in PR-K).

---

## References

- Spike PR: https://github.com/dhis2/dhis2-core/pull/24087 (2026-06-01)
- Spring Framework 7.0 Release Notes: https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-7.0-Release-Notes
- Spring Security 7 migration: https://docs.spring.io/spring-security/reference/7.0/migration/index.html
- Spring Security 6.5 preparing for 7: https://docs.spring.io/spring-security/reference/6.5/migration-7/index.html
- Prior related DHIS2 work: PR #21233 (merged 2025-06-17) Spring Auth Server / Security minor alignment
