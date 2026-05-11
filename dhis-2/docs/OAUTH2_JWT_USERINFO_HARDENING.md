# OAuth2 / OIDC JWT UserInfo — Hardening Follow-ups

Defense-in-depth items for the signed-JWT userinfo path added in PR #23839
(`feat/DHIS2-20043-esignet-oidc-jwt-userinfo`). None of these are exploitable
vulnerabilities under the current threat model — operator-trusted dhis.conf,
operator-trusted IdP, server-to-server userinfo fetch over TLS — but they
close gaps that would matter if any of those assumptions weaken (multi-tenant
IdP, hostile RP on shared issuer, IdP compromise, lax operator config).

Reference:
- Implementation: `dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/SignedJwtUserInfoLoader.java`
- Spec: `dhis-2/docs/superpowers/specs/2026-05-06-esignet-oidc-integration-design.md`
- OIDC Core §5.3.2 — UserInfo Response

---

## 1. Bind UserInfo `sub` to ID-token `sub`  (OIDC §5.3.2)

OIDC Core §5.3.2 mandates:

> "The sub Claim in the UserInfo Response MUST be verified to exactly match
> the sub Claim in the ID Token; if they do not match, the UserInfo Response
> values MUST NOT be used."

Spring Security's stock `OidcUserService` enforces this for the JSON path.
`SignedJwtUserInfoLoader.load()` currently skips it.

**Action.** In `SignedJwtUserInfoLoader.load()`, after `verify(...)` returns
the `JWTClaimsSet`, compare its `sub` to `userRequest.getIdToken().getSubject()`
and throw an `OAuth2AuthenticationException` with error code
`invalid_user_info_response` when they differ.

```java
String userInfoSub = claims.getSubject();
String idTokenSub  = userRequest.getIdToken().getSubject();
if (userInfoSub == null || !userInfoSub.equals(idTokenSub)) {
  throw new OAuth2AuthenticationException(
      new OAuth2Error("invalid_user_info_response"),
      "UserInfo sub does not match ID Token sub");
}
```

Why it matters when the IdP is shared: an attacker controlling a second RP on
the same IdP cannot replay a JWT into our flow today (it's a server-to-server
fetch bound to our access token), but this check makes the binding explicit
and matches Spring's behavior on the JSON path.

## 2. Verify `iss` and `aud` on the UserInfo JWT

`new DefaultJWTProcessor<>()` is used without an explicit
`setJWTClaimsSetVerifier(...)`. Nimbus's default verifier does enforce
`exp`/`nbf`, so expired tokens are rejected — but `iss` and `aud` are not
checked.

**Action.** Pin issuer and audience:

```java
DefaultJWTClaimsVerifier<SecurityContext> verifier =
    new DefaultJWTClaimsVerifier<>(
        new JWTClaimsSet.Builder()
            .issuer(reg.getClientRegistration().getProviderDetails().getIssuerUri())
            .audience(reg.getClientRegistration().getClientId())
            .build(),
        Set.of("sub", "iss", "aud", "exp"));
processor.setJWTClaimsSetVerifier(verifier);
```

On a multi-tenant IdP (e.g. national eSignet deployments) this prevents a JWT
minted for any other RP from being accepted here even if it leaks into the
flow somehow.

## 3. Hard-fail `private_key_jwt` parser when keystore is partial

`GenericOidcProviderConfigParser.isPrivateKeyJwt(...)` returns `true` only
when all four keystore properties are present and non-empty. If the operator
sets `client_authentication_method=private_key_jwt` but forgets, say,
`keystore_password`, the helper returns `false` and the parser then complains
about the missing `client_secret` instead — masking the real misconfig. The
builder can also produce a half-configured registration with no JWK and no
secret.

**Action.** When `client_authentication_method=private_key_jwt`:

- At parse time, reject the provider unless all four keystore properties
  (`keystore_path`, `keystore_password`, `key_alias`, `key_password`) plus
  `jwk_set_url` are present.
- Log the specific missing key so operators see the real cause.

Not a vulnerability (dhis.conf is operator-trusted), but a UX trap worth
closing.

## 4. Strip claim values and upstream messages from `OAuth2Error` descriptions

Several error paths in `SignedJwtUserInfoLoader` embed `mappingValue`,
`mappingClaimKey`, or `ex.getMessage()` into the `OAuth2Error` description.
Spring renders those in the failure-redirect URL.

**Action.** Keep upstream details in server-side logs (`log.debug`/`warn`);
return generic descriptions to the client:

```java
log.warn("UserInfo JWT verification failed for registration {}", registrationId, ex);
throw new OAuth2AuthenticationException(
    new OAuth2Error("jwt_processing_error"), "Failed to verify UserInfo JWT");
```

Matches the JSON path's existing, terser failure responses.

---

## What we already do right (worth keeping)

- `SupportedJwsAlgorithms` allow-list excludes HMAC and `none`, blocking
  algorithm-confusion attacks at config-load time.
- `JwkSourceCache` keys by `registrationId`, not by URL, so one registration
  cannot poison another's cache.
- Nimbus `JWKSourceBuilder` retains its built-in remote-key refresh policy
  across logins (we cache the `JWKSource`, not the keys).
- Default `UserInfoResponseType` is `JSON`, so every existing provider keeps
  Spring Security's standard path unchanged.

## Suggested rollout

1. Land items 1, 2, 4 together as one PR — small, mechanical, fully
   unit-testable against `SignedJwtUserInfoLoaderTest`.
2. Land item 3 separately — it touches the parser and warrants its own
   regression tests covering each "missing one keystore key" permutation.
3. Before enabling eSignet in any production deployment, verify items 1–4 are
   in place.
