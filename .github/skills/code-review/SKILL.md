
# DHIS2 Core Code Review

Review skill for the DHIS2 Core backend (Java 17, Spring Framework 6 without Spring Boot,
Hibernate 5.6/JPA, PostgreSQL, Maven multi-module). It combines a general engineering review
with the DHIS2-specific conventions and WOW rules that generic reviews miss.

Everything in this repository is open source. Reviews are read and judged by the community,
so hold changes to the standard: idiomatic, no shortcuts, done right the first time.

## Review Process

1. **Establish scope.** Get the full diff and its context:
   - `gh pr view <n>` and `gh pr diff <n>`, or `git diff master...HEAD`
   - Read the PR description and the linked Jira issue (`DHIS2-XXXX`). The Jira issue is the
     requirements source; the PR must actually solve it.
2. **Read the changed files, not just the hunks.** Open each touched class fully. A hunk that
   looks fine in isolation often violates layering, transaction, or ACL rules visible only in
   the surrounding class.
3. **General pass** (correctness, design, edge cases) using "What to Check" below.
4. **DHIS2 pass** using the DHIS2 checklist below. This is where most real findings live.
5. **Testing pass** against the platform team test goals.
6. **Hygiene pass** (PR size, commit messages, formatting, leftovers).
7. Produce the report in the Output Format. Never give feedback on code you did not read.

## What to Check (general)

**Plan alignment:**
- Does the implementation match the Jira issue / plan / requirements?
- Are deviations justified improvements, or problematic departures?
- Is all planned functionality present? Nothing half-done behind a TODO?

**Code quality:**
- Clean separation of concerns?
- Proper error handling (no swallowed exceptions, no leaking internals)?
- Edge cases handled (null, empty collections, missing UID, concurrent access)?
- DRY without premature abstraction? Prefer easy-to-read code over syntax tricks.
- Modern switch expressions over long if-else chains.

**Architecture:**
- Sound design decisions? Integrates cleanly with surrounding code and existing patterns?
- A second convention introduced next to an existing one is a defect: reuse the existing pattern.
- Reasonable scalability and performance (query counts, N+1, memory on large instances)?

**Production readiness:**
- Migration strategy if schema changed?
- Backward compatibility for API consumers and older data considered?
- Works on a fresh database AND on an upgraded database?

## DHIS2 Checklist

### Layering

- Request flow is `Controller -> Service -> Store -> Hibernate/PostgreSQL`. Controllers MUST NOT
  access stores directly, and MUST NOT contain business logic. Flag any logic in a controller
  that is not request/response mapping: push it into the service.
- Metadata CRUD endpoints extend `AbstractCrudController<T, P>` and customize via the
  `pre/postCreate/Update/DeleteEntity` lifecycle hooks, not by re-implementing CRUD.
- Metadata write operations go through the `MetadataImportService` pipeline, not direct DAO calls.
- Service interfaces live in `dhis-api`, implementations in `dhis-services` named
  `Default{Name}Service`. Bean name is the fully-qualified interface name.
- Service modules have dependency levels (1 to 9). A change that adds a dependency from a lower
  level module to a higher one is a design smell: flag it.
- Utilities operate on simple types only; they must not accept persisted objects or Spring beans
  (test helpers excepted). Operations needing complex types belong in a service.

### Transactions

- EVERY public method of a `@Service` bean must carry exactly one of:
  `@Transactional`, `@Transactional(readOnly = true)`, `@NonTransactional`, or
  `@IndirectTransactional`. Missing annotation = finding. Class-level `@Transactional` is not
  used: always method-level.
- Read paths use `readOnly = true`. A write inside a `readOnly` transaction is a bug.
- Do not rely on OSIV (Open Session In View). It is active but is an anti-pattern scheduled for
  removal: code must work with explicit transaction boundaries.
- `javax.transaction.Transactional` is banned; only
  `org.springframework.transaction.annotation.Transactional`.

### Security and ACL

- `@PreAuthorize` is banned (build-enforced). Authority gating uses
  `@RequiresAuthority(anyOf = ...)`, enforced by `AuthorityInterceptor`.
- New endpoints: is there an authority check or an ACL check? An unprotected new endpoint is
  Critical. Verify non-privileged callers get 403 with the standard
  `Access is denied, requires ...` message.
- Object-level access uses `aclService.canRead/canWrite/canCreate/canDelete/canDataRead/canDataWrite`.
  Store queries must keep the sharing predicates from the store base classes
  (`getSharingPredicates`, `getDataSharingPredicates`). Raw HQL/SQL that bypasses sharing
  filtering is Critical.
- Current user is obtained via `@CurrentUser UserDetails` in controllers, never via static
  lookups in new code.
- Error responses must not leak internals: SQL/Hibernate exceptions are mapped to generic
  messages by `CrudControllerAdvice`. Flag any handler that returns raw exception text.
- Native SQL built with string concatenation of user input = SQL injection, Critical.
  Parameters must be bound.
- Security-sensitive fixes: no reference to draft GHSA/CVE ids or embargoed vulnerability
  details in public PRs, commits, or branch names.

### Exceptions and error handling

- Services fail with `org.hisp.dhis.feedback.*` exceptions: `NotFoundException` (404),
  `ForbiddenException` (403), `ConflictException` (409), `BadRequestException` (400).
  Do not invent new exception-to-status mappings; `CrudControllerAdvice` handles them globally.
- Use `ErrorCode` entries to identify errors; add a new code rather than a bare message string.
- `get*` store/service methods never return `null`: they throw if missing.
  `find*` methods return `Optional<X>`. Flag methods that violate this contract.

### Domain model and serialization

- Extend the right base: most metadata extends `BaseNameableObject`; analytics-dimensional
  entities extend `BaseDimensionalItemObject`; metadata entities implement `MetadataObject`.
- Every exposed getter carries the annotation stack: `@JsonProperty` +
  `@JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)`. Object references serialize as stubs
  via `@JsonSerialize(as = IdentifiableObject.class)`. User references use
  `UserPropertyTransformer`. Translatable display properties use `@Translatable`.
- A new persisted property needs ALL of: field + getter/setter, hbm.xml or JPA mapping,
  Flyway migration, schema/property annotations, and serialization annotations. A missing leg
  of this set is a common and easy-to-miss bug.
- Use `UID` (type-safe, 11 chars) over raw `String` for identifiers in new code.
- Deletion integrity is handled with `DeletionHandler` subclasses (`whenDeleting`, `whenVetoing`),
  not JPA cascades. A new entity with references usually needs one; check it exists.

### Stores and persistence

- Store interfaces `{Entity}Store`, implementations `Hibernate{Entity}Store` extending
  `HibernateIdentifiableObjectStore<T>` (or the appropriate base). Bean name = FQN of interface.
- Spring Data JPA repositories are never used.
- Query styles: JPA CriteriaBuilder with `newJpaParameters()`, HQL via `getQuery`, or native SQL
  via JdbcTemplate for performance paths. Entities with compound primary keys cannot use
  Criteria attribute lookups (hbm mapping limitation): use HQL, or an ID class for new entities.
- hbm.xml change without a matching Flyway migration will fail Hibernate schema validation at
  startup: always check both sides changed together.
- JSONB columns use the custom types from `dhis-support-hibernate` (`JsonBinaryType` family).

### Caching

- No `@Cacheable`/`@CacheEvict` anywhere. Caching is programmatic via `CacheProvider` factory
  methods, created in the constructor, used with `cache.get(key, () -> ...)`.
- Never cache Hibernate-managed (or detached) entities in `CacheProvider` caches: cluster
  serialization and stale-state bugs. Cache immutable value objects or ids instead.
- Cached state that must be coherent across a cluster needs invalidation
  (`dhis-support-cache-invalidation`); flag node-local caching of cluster-shared state.

### Configuration (dhis.conf)

- All config through `DhisConfigurationProvider` + `ConfigurationKey`.
- Booleans accept `true`, `TRUE`, `on`, `ON`. NEVER `"true".equals(getProperty(...))`:
  it silently misses `on`/`ON`. Always `isEnabled(key)` / `isDisabled(key)`. When forwarding a
  boolean to a third-party library, normalize with `String.valueOf(dhisConfig.isEnabled(key))`.
  An exact string compare on a boolean config key is a Critical config-handling bug.
- New keys: added to `ConfigurationKey` with a sane default and marked confidential if sensitive.

### Flyway migrations

- Location `dhis-support-db-migration`, naming `V2_{major}_{seq}__{Description_with_underscores}.sql`
  (e.g. `V2_43_56__add_default_order_indices.sql`); pick the next unused sequence number.
- Migrations MUST be idempotent (`IF NOT EXISTS` / `IF EXISTS`, drop-then-create for named
  constraints) and must work on a fresh, empty database. Non-idempotent migration = Critical.
- Every new migration MUST get an entry in the WOW coordination document
  (`wow-backend/coordination/flyway_versioning.md`). Missing entry = must fix before merge.
- Lowercase SQL in migration files. No explicit BEGIN/COMMIT (Flyway owns the transaction);
  Java migrations must not close the connection.
- Backports: copy the migration under the next free number for EACH target branch and add
  coordination entries per version. Only idempotent scripts may be backported.
- Released migration files are immutable: fix-forward with a new script, never edit in place.
- Many-to-many link tables use the `{fromtable}_{totable}` convention with the linked side in
  plural (e.g. `event` + `note` gives `event_notes`).
  Single-column index naming: `in_{table}_{column}`.

### HTTP endpoint and API design

- Reads return `ResponseEntity<StreamingJsonRoot<T>>` (lists) or typed JSON; writes return
  `WebMessage` built by `WebMessageUtils` (`ok()`, `created()`, `conflict()`, ...).
- One media type and one unique path per method; avoid header/parameter-based mapping.
- Dedicated `Params`/`Request`/`Response` record-like classes for parameters and bodies, with
  defaults in the params object. `UID` over `String`, enums over `String`.
- Use `204 NO_CONTENT` where there is nothing to return.
- OpenAPI is custom (`@OpenApi.*` annotations, not springdoc): new/changed endpoints must keep
  annotations accurate; add `@OpenApi.Document` classifiers (`team:...`, `purpose:...`).
- Field filtering (`?fields=`) and object filtering (`?filter=`) come from the framework; do not
  hand-roll partial responses.
- API compatibility: DHIS2 values stability. Breaking changes to response shape, defaults, or
  semantics of existing endpoints need explicit justification and deprecation consideration.
  (Exact 4xx/2xx code shifts and error message rephrasing are tolerated by contract; shape and
  semantics are not.)

### Java conventions

Banned imports (build-enforced; their presence means the author never built):

| Banned | Use instead |
|---|---|
| JUnit 4 (`org.junit.Test`) | JUnit 5 `org.junit.jupiter.api` |
| `org.json.*`, `net.minidev.json.*` | `org.hisp.dhis.jsontree` |
| `org.jetbrains.annotations.*` | `javax.annotation` |
| `lombok.NonNull` | `javax.annotation.Nonnull` / `@CheckForNull` |
| `javax.transaction.Transactional` | Spring `@Transactional` |
| `@PreAuthorize` | `@RequiresAuthority` |
| `commons-lang`, `commons-collections` v1 | `commons-lang3`, `commons-collections4` |

- Nullability: ONLY `javax.annotation.Nonnull` and `javax.annotation.CheckForNull`. Never return
  `null` from public methods; return empty collections; accept `null` parameters only as an
  explicit default-behaviour signal.
- Lombok: `@RequiredArgsConstructor` + `private final` for DI. Domain models use field-level
  `@Setter` with manual getters (the annotation stacks live on getters); never class-level
  `@Data`/`@Getter` on domain models. `@SneakyThrows` is forbidden. `@EqualsAndHashCode` and
  `@ToString` require deliberate include/exclude choices.
- Collections: JDK collections only; `List.of`/`Set.of`/`Map.of`/`copyOf` where possible, but
  remember they reject `null` elements and `contains(null)`; `Set.of` rejects duplicates;
  `Map.of` loses order. Stream results via `toList()`. Avoid nested streams.
- Records over Lombok value classes where possible. Immutable inputs; never mutate method inputs.
- Javadoc on interfaces: brief, `{@link}`/`{@code}`, intent and background, no restating code.
- Naming: `{Entity}Service` / `Default{Entity}Service` / `{Entity}Store` /
  `Hibernate{Entity}Store` / `{Entity}Controller` / `{Class}Test` / `{X}TestBase`.

## Testing Requirements (platform team goals)

- Minimum 1 test per PR. Mockito-only tests do NOT count toward this.
- Bugfix PRs MUST include a test proving the bug is gone. A bugfix PR without a test (or an
  explicit justification) must not be approved.
- New REST endpoints: every request parameter covered by at least one test.
- New store methods: at least one test each.
- `chore:` PRs are exempt (they must not change behaviour; if they do, they are `refactor:`).
- Right base class: `H2ControllerIntegrationTestBase` (fast, default) or
  `PostgresControllerIntegrationTestBase` for controller tests in `dhis-test-web-api`;
  `PostgresIntegrationTestBase` for service tests in `dhis-test-integration`; plain JUnit 5 +
  Mockito for unit tests. H2 tests that need Postgres features (JSONB ops, Postgis) are wrong.
- Use the harness idioms: `assertStatus(...)`, `response.content(OK)`, `response.error(...)`,
  `switchToNewUser(...)` / `switchToAdminUser()` for auth scenarios, `createDataElement('A')`
  factory conventions. Auth-gated endpoints need a negative (403) test.
- Prefer `@ParameterizedTest` when cases differ only by input values.
- Assertions: JUnit 5 first, then DHIS2's own `Assertions` class; add new shared assertions
  there (expected, actual, message order). No third-party assertion types in signatures.
- Tests verify real behaviour, not mock choreography. Deterministic, isolated, full-suite safe.

## PR and Commit Hygiene

- Small PRs: one feature/fix per PR, aim for max ~15 files. Unrelated clean-up goes in its own PR.
- Commit/PR title: `prefix: Imperative subject [DHIS2-XXXX]`, capitalized, max 50 chars, no
  trailing period. Prefixes: `feat:` `fix:` `chore:` `ci:` `docs:` `refactor:` `perf:` `test:`.
  Body explains what and why, wrapped at 72 chars. No generic "Minor fix" messages.
- Formatting: Google Java Format via Spotless. `mvn spotless:check -f dhis-2/pom.xml` must pass.
- No unused imports, no commented-out code, no dead code, license header present on new files.
- No stray markdown in the product diff: no `*.md` files other than an intentional `README.md`
  (no plans, notes, changelogs, agent artifacts). Their presence blocks merge.
- Deprecation warnings introduced by dependency bumps are fixed in the same PR, not left behind.
- New files staged and included; generated/IDE files excluded.

## Severity Calibration (DHIS2-tuned)

Categorize by actual impact, not by count. Typical mapping:

- **Critical (must fix):** missing auth/ACL on an endpoint; sharing predicates bypassed;
  SQL injection; secrets/data leaks in errors or logs; non-idempotent or missing Flyway
  migration; hbm/schema drift; exact-string boolean config compare; data loss; broken
  backward compatibility; bugfix that does not actually fix the reproduction.
- **Important (should fix):** missing/wrong `@Transactional`; business logic or store access in
  a controller; missing tests per platform goals; banned imports; wrong base class or duplicate
  convention; N+1 or unbounded queries on hot paths; missing DeletionHandler; missing WOW
  coordination entry; missing OpenAPI/serialization annotations on exposed properties.
- **Minor (nice to have):** naming, Javadoc gaps, non-blocking style issues, missed
  `@ParameterizedTest` consolidation, small readability improvements.

If a finding concerns the plan/Jira issue itself rather than the implementation, say so
explicitly. If the implementation deviates from the plan, flag it so the author can confirm
whether the deviation was intentional.

Acknowledge what was done well before listing issues: accurate praise helps the author trust
the rest of the feedback.

## Output Format

### Strengths
[What is well done? Be specific: file/class references.]

### Issues

#### Critical (Must Fix)
[Bugs, security/ACL issues, data loss risks, migration hazards, broken functionality]

#### Important (Should Fix)
[Architecture/layering problems, transaction gaps, test gaps, convention violations]

#### Minor (Nice to Have)
[Style, naming, documentation polish]

For each issue:
- `File:line` reference
- What is wrong
- Why it matters (tie it to the rule or the failure mode)
- How to fix (if not obvious)

### Recommendations
[Non-blocking improvements for code quality, architecture, or process]

### Assessment

**Ready to merge?** [Yes | No | With fixes]

**Reasoning:** [1-2 sentence technical assessment]

## Critical Rules

**DO:**
- Read every changed file before judging it
- Categorize by actual severity; tie findings to concrete DHIS2 rules
- Be specific (`File:line`, exact symbol), and explain WHY each issue matters
- Check the test goals: bugfix without regression test blocks approval
- Check migrations for idempotency and coordination entries
- Acknowledge strengths and give a clear verdict

**DON'T:**
- Say "looks good" without checking the DHIS2 checklist
- Mark nitpicks as Critical, or bury a Critical among nitpicks
- Give feedback on code you did not actually read
- Be vague ("improve error handling")
- Approve a PR with "request changes" feedback outstanding, even with two approvals
- Avoid giving a verdict

