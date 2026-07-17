# Design: Soft-refresh immutable UserDetails (stop mass session kicks)

**Date:** 2026-07-18  
**Author:** Morten Svanæs  
**Status:** Proposal (for review with Stian)  
**Related:**
- PR https://github.com/dhis2/dhis2-core/pull/24477 (perf batching only; does not change semantics)
- DHIS2-16539 UserDetails for authority checks
- DHIS2-17838 inelegant logout when editing own role
- Consensus plan: `triagebot/.omc/plans/ralplan-userdetails-soft-refresh.md` (Critic APPROVED)
- Code: `UserRoleBundleHook`, `UserObjectBundleHook`, `DefaultUserService.invalidateUserSessions`, Redis Spring Session, `DhisOidcUser`

## Problem

After the User → `UserDetails` migration, authorization is snapshotted into an effectively immutable `UserDetailsImpl` stored as the Spring Security principal in the HTTP session.

When a `UserRole`'s authorities change, `UserRoleBundleHook.postUpdate` loops every member and calls `userService.invalidateUserSessions(username)`, which expires sessions via `SessionRegistry`.

That was a deliberate compromise to avoid mutating live principals. It is correct for security, but:

1. A role with ~22k users becomes a mass-kick + N×`createUserDetails` storm.
2. PR #24477 only collapses the N+1 lookups; users still get logged out.
3. Policy is inconsistent: user groups only soft-invalidate ACL cache (`userGroupInfoCache`) with TODOs to also invalidate sessions; JWT already rebuilds `UserDetails` per request; PAT can keep a cached principal.

## Goal

Keep the **immutable UserDetails value-object model** (no in-place mutation of authorities/groups/OUs on a live instance), but stop treating "authz changed" as "logout everyone".

## Design in one paragraph

Introduce dual generation stamps (`userAuthzGen`, `roleAuthzGen`) and a shared `AuthzService`. Role authority (or restriction) edits bump **one** role generation (O(1)). On the next request, a filter (and JWT/PAT paths) compares session gen to `max(userGen, max(roleGens))`; if stale, **rebuild** a new immutable `UserDetails` and replace the `Authentication` in the SecurityContext/session. Only password change, disable/lock/expiry, and admin force-logout keep hard `SessionRegistry.expireNow`.

## Why this does not abandon immutable UserDetails

Immutability means: treat the DTO as a value object; do not mutate fields after build.

It does **not** mean: the session must keep the same instance forever.

- Login builds snapshot V1.
- Authz change bumps a gen.
- Next request builds snapshot V2 and replaces the principal reference.
- Call sites still see an immutable object.

JWT already works this way today.

## Dual generation (the 22k case)

| Stamp | Bumped when | Write cost |
|-------|-------------|------------|
| `userAuthzGen` | user↔role membership, user↔group, user OU sets (orgUnits / dataView / teiSearch), user-level restrictions affecting the snapshot | O(1) per affected user |
| `roleAuthzGen` | role **authorities** or **restrictions** set changes | O(1) per role |

```text
effectiveGen = max(userAuthzGen(user), max(roleAuthzGen(r) for r in principal.roleIds))
```

Changing authorities on a role with 22k members updates **one counter**, not 22k sessions and not 22k user rows.

Membership add/remove bumps only the affected users' `userAuthzGen`. Source of truth is owning side `User.userRoles` (`UserRole.members` is Hibernate `inverse=true` — do not use it alone for deltas).

## Shared AuthzService (v1: all paths, same train)

One service used by:

- form / OIDC session soft-refresh filter
- JWT bearer converter
- PAT auth manager
- login `UserDetailsService` paths

```text
AuthzService
  currentUserGen / currentRoleGen / effectiveGen(principal)
  loadFreshUserDetails(username)   // wraps createUserDetails*
  ensureFresh(UserDetails current) // rebuild if stamp behind
  bumpUserAuthz / bumpRoleAuthz / bumpUsers
```

Short-TTL, generation-keyed cache + single-flight amortises concurrent rebuilds after a bump.

## Soft-refresh filter (sessions)

After session SecurityContext is loaded:

1. No DHIS user principal → skip.
2. Read session attr `DHIS2_AUTHZ_GEN` (missing ⇒ 0).
3. Compute `effectiveGen`.
4. If equal → continue.
5. Else rebuild via `AuthzService`, build a **new** `Authentication` with fresh authorities:
   - Form: new `UsernamePasswordAuthenticationToken` with fresh principal/authorities.
   - OIDC: rebuild a new `DhisOidcUser(freshUserDetails, oldAttributes, nameKey, oldIdToken)` — never only swap the nested user field (`DhisOidcUser` keeps authorities in both super and nested user).
6. Set SecurityContext, then **explicitly** `SecurityContextRepository.saveContext` (`requireExplicitSave(true)` in web security config).
7. Update `DHIS2_AUTHZ_GEN`. Do **not** re-register with SessionRegistry (principal equals is username-only; hard-kick still works).

No cookie change. No `expireNow`.

## Policy matrix

| Event | Action |
|-------|--------|
| Password change | HARD invalidate sessions |
| Account disable / lock / expiry | HARD |
| Admin force-logout / DELETE sessions API | HARD |
| UserRole authorities **or restrictions** change | SOFT `bumpRoleAuthz` |
| User role membership change (owning side `User.userRoles`) | SOFT `bumpUserAuthz` |
| UserGroup membership / group update | SOFT bump affected users + existing ACL group cache invalidate |
| User org-unit assignment change (orgUnits / dataView / teiSearch) | SOFT `bumpUserAuthz` |

## Hook changes (semantic)

**`UserRoleBundleHook`**
- Today: authorities change → invalidate every member.
- New: authorities **or restrictions** change → `bumpRoleAuthz(roleUid)`.
- Member list delta → `bumpUserAuthz` only for added/removed users (owning-side / join-table SoT, not inverse collection alone).

**`UserObjectBundleHook`**
- Today: role set change → hard invalidate that user.
- New: role/OU change → `bumpUserAuthz`.
- Password remains hard via controller paths.

**UserGroup**
- Keep ACL `invalidateCurrentUserGroupInfoCache`.
- Add soft bumps for affected members (addresses MAS TODOs without mass logout).

## Storage

- Prefer Redis when enabled (same deployments that already use Spring Session Redis).
- **Hard rule:** if Redis sessions are enabled, gen store MUST be shared Redis (or shared DB visible to all nodes). Node-local gens are not allowed; misconfiguration fails closed.
- DB fallback for non-Redis single-node.
- Document a small best-effort window only for **shared-store** replication lag, not missing shared store.

### Redis keys (v1)
- `dhis2:authz:user:{username}` / `dhis2:authz:role:{roleUid}` — `INCR` on bump
- `dhis2:authz:ud:{username}:{effectiveGen}` — short-TTL rebuild cache
- `dhis2:authz:rebuild:{username}` — single-flight

### DB fallback
`authz_version(scope, key_name, gen, updated_at)` PK `(scope, key_name)`. Missing row = 0. Do not put gen columns on hot user/role entities in v1.

### Session embedding
Session attribute `DHIS2_AUTHZ_GEN` (missing ⇒ 0 ⇒ one soft-refresh). Prefer not adding a mandatory new field on `UserDetailsImpl` in the first PR (JDK Redis session serialization).

## Freshness guarantee

**Product decision:** privilege revoke is **next-request best-effort** on a shared gen store. A short shared-store propagation window is acceptable if documented. Not "eventually within minutes", not "only on re-login", and not "node-local is fine".

## Non-goals

- Mutable `UserDetailsImpl` setters for authorities/groups/OUs
- Eager rewrite of every Redis session document on role change
- Rebuild UserDetails on every form request unconditionally
- Replacing PR #24477 (it can still land as a bandage; this design supersedes the semantic)

## Acceptance criteria (high level)

1. Role authority/restriction edit with large membership does not call `expireNow` for members.
2. Revoked authority is gone for an already-logged-in form session on the next API request (shared-store best-effort window).
3. Granted authority appears on next request without re-login.
4. Password / disable / lock / expiry still hard-kick.
5. Group and OU changes refresh the session principal on next request without logout.
6. Role authority/restriction edit write path is O(1) (role gen bump).
7. JWT and PAT cannot stay more privileged than form sessions after a bump (same release train).
8. `UserDetailsImpl` remains immutable as a value object; OIDC rebuilds full wrapper.
9. Explicit session save after soft-refresh; no SessionRegistry re-register.

## Suggested implementation order

1. `AuthzVersionStore` + `AuthzService` + single-flight cache + tests
2. Session attr gen at login; soft-refresh filter + explicit `saveContext` + metrics
3. Switch role/user/group hooks from hard invalidate to bumps (restrictions + OU + membership SoT)
4. Route JWT + PAT through `AuthzService` / gen-aware cache (same train)
5. Keep hard invalidate only on credential/liveness/admin logout paths
6. Docs + fail-closed Redis-sessions config check

## Bottom line for review

We keep the migration principle (immutable UserDetails, no authz mutation soup). We stop using logout as a poor man's cache invalidation. Mass role edits become O(1) writes + lazy per-active-user rebuild on next request — the same mental model JWT already has.
