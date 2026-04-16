---
title: Personal access tokens (PAT)
type: concept
status: verified
last_verified:
  commit: 0fcfa39f1cbe7c3e5912fdb12e55eb924b3b398a
  date: 2026-04-16
  by: claude-sonnet-4-6
sources:
  - path: dhis-2/dhis-api/src/main/java/org/hisp/dhis/security/apikey/ApiTokenType.java
    lines: 40-85
    role: primary
  - path: dhis-2/dhis-api/src/main/java/org/hisp/dhis/security/apikey/ApiKeyTokenGenerator.java
    lines: 46-221
    role: primary
  - path: dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenResolver.java
    lines: 47-106
    role: primary
  - path: dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/Dhis2ApiTokenFilter.java
    lines: 62-227
    role: primary
  - path: dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManager.java
    lines: 60-162
    role: primary
  - path: dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/security/ApiTokenController.java
    lines: 72-169
    role: primary
  - path: dhis-2/dhis-services/dhis-service-core/src/main/resources/org/hisp/dhis/security.hibernate/ApiToken.hbm.xml
    lines: 1-36
    role: primary
  - path: dhis-2/dhis-support/dhis-support-system/src/main/java/org/hisp/dhis/cache/DefaultCacheProvider.java
    lines: 363-372
    role: primary
see_also:
  - authn/session.md
  - authn/impersonation.md
  - authz/overview.md
tags: [authn, pat, tokens]
confidence: high
open_questions:
  - What is the behaviour when a PAT is used by a user who has been disabled or had 2FA enabled *after* the token entered the cache? The disable/2FA check is in `validateAndCreateUserDetails` ([`ApiTokenAuthManager.java:115-139`](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManager.java#L115-L139)) which only runs on cache miss — so a cached token may keep working for up to 1h after the user is disabled.
  - Same concern for authority / org-unit changes — they are snapshotted into the cached `UserDetails` and not refreshed until cache eviction.
  - Password change — is there a cache-eviction hook? No event listener was found. Verify.
  - `api_token.sharing` column is present ([`ApiToken.hbm.xml:34`](../../../dhis-2/dhis-services/dhis-service-core/src/main/resources/org/hisp/dhis/security.hibernate/ApiToken.hbm.xml#L34)). Can a user share their PAT with another user/group? What does shared access mean — does the sharee see metadata only, or can they use the token?
  - Impersonation + PAT — no code found handling PAT creation or use while `switchUser` is active. Behaviour undefined.
  - The `hashToken` switch supports `SHA-512` ([`ApiKeyTokenGenerator.java:202`](../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/security/apikey/ApiKeyTokenGenerator.java#L202)) but no `ApiTokenType` currently declares that hash type ([`ApiTokenType.java:41-42`](../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/security/apikey/ApiTokenType.java#L41-L42)). Dead code or reserved for a future token type?
  - `AbstractCrudController` inherits list/get/delete. Does the list endpoint restrict results to tokens created by the current user, or does a non-superuser with `F_METADATA_*`-style authorities see others' tokens?
  - Rate-limiting on PAT authentication attempts — no code in the filter. Confirm whether a global rate-limiter applies.
---

# Personal access tokens (PAT)

## Summary

PATs are long-lived bearer tokens identified by a DHIS2-specific prefix, stored hashed in `api_token`, and validated by a dedicated Spring Security filter that runs ahead of form-login and basic-auth. A PAT authenticates *as* its creating user and inherits that user's authorities, org-unit sets, and sharing context — no independent scope mechanism exists. Constraints (IP, referer, HTTP method allow-lists) are attached to a PAT and enforced per request, but authority/user-state changes are hidden behind a 1-hour in-memory cache.

## How it works

- **Two token versions are defined**: `PERSONAL_ACCESS_TOKEN_V1` (prefix `d2pat`, 32-char body, `CRC32` decimal checksum) and `PERSONAL_ACCESS_TOKEN_V2` (prefix `d2p`, 44-char body, `CRC32_B62` checksum). V2 is the default for new tokens ([`ApiTokenType.java:41-63`](../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/security/apikey/ApiTokenType.java#L41-L63)).
- **Both versions declare `SHA-256` as the hash type** ([`ApiTokenType.java:41-42`](../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/security/apikey/ApiTokenType.java#L41-L42)). The `SHA-512` branch in [`ApiKeyTokenGenerator.hashToken`](../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/security/apikey/ApiKeyTokenGenerator.java#L195-L206) is currently unreachable.
- **Body entropy** comes from `CodeGenerator.generateSecureRandomCode(type.getLength())` ([`ApiKeyTokenGenerator.java:78`](../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/security/apikey/ApiKeyTokenGenerator.java#L78)). Wire format is `prefix + "_" + body + checksum` ([`ApiKeyTokenGenerator.java:82-94`](../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/security/apikey/ApiKeyTokenGenerator.java#L82-L94)).
- **Checksum is tamper/typo detection only** — CRC32 over the body, not a MAC. It short-circuits bad tokens before any DB lookup ([`ApiKeyTokenGenerator.java:97-167`](../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/security/apikey/ApiKeyTokenGenerator.java#L97-L167)).
- **The token is hashed before storage** ([`ApiKeyTokenGenerator.java:195-206`](../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/security/apikey/ApiKeyTokenGenerator.java#L195-L206)). The raw token exists in plaintext only in the creation response, which overwrites the char array with zeros after the response is serialised ([`ApiTokenController.java:127`](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/security/ApiTokenController.java#L127)).
- **Storage schema** (`api_token` table): unique `key` column holds the SHA-256 hex hash (length=128 in the mapping — sized for SHA-512), `version` int, `type` enum, `expire` long ms-since-epoch, `attributes` jsonb (IP/method/referer allow-lists), FK `createdby` → `User`, plus a `sharing` jsonb column ([`ApiToken.hbm.xml:9-35`](../../../dhis-2/dhis-services/dhis-service-core/src/main/resources/org/hisp/dhis/security.hibernate/ApiToken.hbm.xml#L9-L35)).

## Entry points

- **Filter**: `Dhis2ApiTokenFilter` is an `OncePerRequestFilter` placed in the Spring Security chain for `/api/*` ([`Dhis2ApiTokenFilter.java:62`](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/Dhis2ApiTokenFilter.java#L62)). It extracts, validates, authenticates, applies constraints, and places an `ApiTokenAuthenticationToken` in the `SecurityContext` before delegating to the rest of the chain ([`Dhis2ApiTokenFilter.java:91-130`](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/Dhis2ApiTokenFilter.java#L91-L130)).
- **Header**: `Authorization: ApiToken <token>`, case-insensitive scheme, regex `^ApiToken (?<token>[a-z0-9-._~+/]+=*)$` (also case-insensitive flag) ([`ApiTokenResolver.java:49-50`](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenResolver.java#L49-L50)). Regex character class is limited — uppercase letters in the token body would fail the pattern (note the `CASE_INSENSITIVE` flag covers this, but only once the regex engine is used; the alphabet nominally written in lowercase).
- **Controller**: [`ApiTokenController`](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/security/ApiTokenController.java) at paths `/api/apiToken` and `/api/apiTokens` extends `AbstractCrudController<ApiToken,…>`. It overrides only `postJsonObject`; list / get / delete / update come from the base class ([`ApiTokenController.java:73-78,85-133`](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/security/ApiTokenController.java#L73-L133)).
- **Creation authority gate**: `aclService.canCreate(currentUser, ApiToken.class)` ([`ApiTokenController.java:91-93`](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/security/ApiTokenController.java#L91-L93)). There is no dedicated `F_*` authority for PAT creation in [`Authorities.java`](../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/security/Authorities.java); the check is schema-ACL only.

## Invariants

- **Tokens are always stored hashed.** The raw token never touches the database; the hash column is unique and not-null ([`ApiToken.hbm.xml:19`](../../../dhis-2/dhis-services/dhis-service-core/src/main/resources/org/hisp/dhis/security.hibernate/ApiToken.hbm.xml#L19), [`ApiKeyTokenGenerator.java:70`](../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/security/apikey/ApiKeyTokenGenerator.java#L70)).
- **A PAT always authenticates as its `createdBy` user.** The `ApiTokenAuthManager` derives `UserDetails` from the token's `createdBy` field ([`ApiTokenAuthManager.java:104,115-121`](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManager.java#L104-L121)). There is no scope field on the token that could narrow this.
- **Authorities carried by a PAT request equal the user's full current authorities**, minus cache delay. Once authenticated, `@PreAuthorize` / ACL checks run identically to a session-based request.
- **IP / referer / HTTP-method allow-lists are re-evaluated on every request** (not cached), in `Dhis2ApiTokenFilter.validateRequestRules` ([`Dhis2ApiTokenFilter.java:117,143-162`](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/Dhis2ApiTokenFilter.java#L117-L162)). Empty allow-list = no restriction ([`Dhis2ApiTokenFilter.java:167,178,191`](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/Dhis2ApiTokenFilter.java#L167-L191)).
- **Token expiry is re-checked on every request** ([`ApiTokenAuthManager.java:93,157-161`](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManager.java#L93-L161)) — both on cache hit and cache miss.
- **Default expiry is 30 days from creation** if the request omits an `expire` field ([`ApiTokenController.java:83,136-138`](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/security/ApiTokenController.java#L83-L138)). No maximum expiry is enforced server-side — a client can mint a token with expiry arbitrarily far in the future.

## Edge cases

- **1-hour authority cache on cache hit**. [`ApiTokenAuthManager.authenticate`](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManager.java#L86-L113) checks `apiTokenCache.getIfPresent(tokenKey)`; on hit, it re-validates **expiry only** and returns the cached `ApiTokenAuthenticationToken` ([`ApiTokenAuthManager.java:90-94`](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManager.java#L90-L94)). The cache is `expireAfterWrite(1, HOURS)`, max size 10 000, in-memory ([`DefaultCacheProvider.java:364-372`](../../../dhis-2/dhis-support/dhis-support-system/src/main/java/org/hisp/dhis/cache/DefaultCacheProvider.java#L364-L372)). Consequences:
  - User disabled → PAT still valid up to ~1h.
  - User enables 2FA → PAT still valid up to ~1h, despite the intended 2FA block (see below).
  - User locked → PAT still valid up to ~1h.
  - User's authorities changed → PAT carries stale authorities up to ~1h.
  - User's credentials expired → PAT still valid up to ~1h.
  - Password change does not invalidate the cache entry (no event listener observed).
- **2FA blocks PAT use entirely** (on cache miss). If `user.isTwoFactorEnabled() == true`, authentication fails with message *"The API token is disabled, locked or 2FA is enabled"* ([`ApiTokenAuthManager.java:127,134-138`](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManager.java#L127-L138)). So PATs are not usable by 2FA-enabled users — this is the intended posture, but it is subverted by the cache for up to 1h after enabling 2FA on an already-authenticated PAT.
- **Cache invalidation on deletion**. `@EventListener` on `ApiTokenDeletedEvent` calls `apiTokenCache.invalidate(tokenHash)` ([`ApiTokenAuthManager.java:81-84`](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManager.java#L81-L84)). Explicit DELETE of a PAT therefore evicts immediately.
- **Owner-deletion cleanup** happens via `ApiTokenDeletionHandler.deleteUser` — all tokens owned by the user are deleted when the owner is deleted (see `dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/user/ApiTokenDeletionHandler.java`, to be documented separately).
- **Creation flow goes through the metadata import pipeline** (not a direct `store.save`). The controller wraps the new `ApiToken` in a `MetadataObjects` container and calls `importService.importMetadata(…)` with `ImportStrategy.CREATE` ([`ApiTokenController.java:108-118`](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/security/ApiTokenController.java#L108-L118)). Implication: any cross-cutting concern that intercepts metadata imports (audit, schema validation, sharing defaults) applies here too.
- **Return payload exposes the plaintext token exactly once** via `ApiTokenCreationResponse` ([`ApiTokenController.java:125-127`](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/security/ApiTokenController.java#L125-L127)). After the response is built, the in-memory char array is zeroed.

## Gotchas

- **The cache is the weak point for revocation semantics.** Any user-state check implemented on the cache-miss path (`validateAndCreateUserDetails`) can be bypassed for the cache's TTL. This includes `isDisabled`, `isTwoFactorEnabled`, `userNonExpired`, `isLocked`, `isAccountNonExpired` — all in [`ApiTokenAuthManager.java:127-139`](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManager.java#L127-L139).
- **IP allow-list uses `X-Forwarded-For` *before* `request.getRemoteAddr()`**, with `ObjectUtils.firstNonNull` ([`Dhis2ApiTokenFilter.java:192-194`](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/Dhis2ApiTokenFilter.java#L192-L194)). If a proxy is not guaranteed to strip/normalise `X-Forwarded-For`, a client can supply the header itself.
- **Referer check compares case-insensitive stored values against the raw `Referer` header** after lowercasing ([`Dhis2ApiTokenFilter.java:179-182`](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/Dhis2ApiTokenFilter.java#L179-L182)). No URL normalisation — trailing slashes, query strings, or port inclusions will mismatch.
- **`sharing` on `api_token`**. The entity has a `sharing` jsonb column ([`ApiToken.hbm.xml:34`](../../../dhis-2/dhis-services/dhis-service-core/src/main/resources/org/hisp/dhis/security.hibernate/ApiToken.hbm.xml#L34)). Semantics not yet mapped — see open questions.
- **Filter extracts the token *before* any authentication**. Attackers can probe the filter with arbitrary input; failures throw `ApiTokenAuthenticationException` and call the entry point, but a malformed `ApiToken` scheme still forces the filter to respond. No rate-limit observed.
- **`validateChecksum` catches `Exception` and re-wraps as `CHECKSUM_VALIDATION_FAILED`** ([`ApiTokenResolver.java:90-95`](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenResolver.java#L90-L95)). Any parser bug becomes an opaque 400.

## Error surface

- `ApiTokenAuthenticationException` → 401 via `ApiTokenErrors.invalidToken` ([`ApiTokenErrors.java` — to be documented]).
- `ApiTokenExpiredException` (subclass) → 401 on `expire <= now` ([`ApiTokenAuthManager.java:157-160`](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManager.java#L157-L160)).
- `ApiTokenConstraintsValidationFailedException` (subclass) → 401 on IP/referer/method violation ([`Dhis2ApiTokenFilter.java:160-162`](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/Dhis2ApiTokenFilter.java#L160-L162)).
- Malformed `ApiToken` scheme → 400 via `ApiTokenErrors.invalidRequest` ([`ApiTokenResolver.java:77-78`](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenResolver.java#L77-L78)).

## Open questions

- What is the behaviour when a PAT is used by a user who has been disabled or had 2FA enabled *after* the token entered the cache? The disable/2FA check is in `validateAndCreateUserDetails` ([`ApiTokenAuthManager.java:115-139`](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManager.java#L115-L139)) which only runs on cache miss — so a cached token may keep working for up to 1h after the user is disabled.
- Same concern for authority / org-unit changes — they are snapshotted into the cached `UserDetails` and not refreshed until cache eviction.
- Password change — is there a cache-eviction hook? No event listener was found. Verify.
- `api_token.sharing` column is present ([`ApiToken.hbm.xml:34`](../../../dhis-2/dhis-services/dhis-service-core/src/main/resources/org/hisp/dhis/security.hibernate/ApiToken.hbm.xml#L34)). Can a user share their PAT with another user/group? What does shared access mean — does the sharee see metadata only, or can they use the token?
- Impersonation + PAT — no code found handling PAT creation or use while `switchUser` is active. Behaviour undefined.
- The `hashToken` switch supports `SHA-512` ([`ApiKeyTokenGenerator.java:202`](../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/security/apikey/ApiKeyTokenGenerator.java#L202)) but no `ApiTokenType` currently declares that hash type ([`ApiTokenType.java:41-42`](../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/security/apikey/ApiTokenType.java#L41-L42)). Dead code or reserved for a future token type?
- `AbstractCrudController` inherits list/get/delete. Does the list endpoint restrict results to tokens created by the current user, or does a non-superuser with metadata-read authorities see others' tokens?
- Rate-limiting on PAT authentication attempts — no code in the filter. Confirm whether a global rate-limiter applies.
