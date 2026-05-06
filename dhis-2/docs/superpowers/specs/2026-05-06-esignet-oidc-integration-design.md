# eSignet OIDC Integration — Design Spec

- Date: 2026-05-06
- Author: Morten Svanæs
- Status: Approved
- Related: PR [#22027](https://github.com/dhis2/dhis2-core/pull/22027), Spring Security issue [#9583](https://github.com/spring-projects/spring-security/issues/9583)

## 1. Background

PR #22027 was a POC integration of MOSIP eSignet for HISP Sri Lanka. eSignet
returns its OIDC userinfo response as a signed JWT (`Content-Type: application/jwt`).
Spring Security's `OidcUserService` only handles `application/json` userinfo;
the open Spring issue confirms this has not been addressed upstream.

The POC patched DHIS2's `DhisOidcUserService` to **always** treat userinfo as
a signed JWT. This works for eSignet but breaks every other configured OIDC
provider (Google, Azure AD, WSO2, generic OIDC, internal DHIS2-as-IdP), all
of which return JSON userinfo. The PR also `@Disabled`-ed the e2e
`OAuth2Test` to hide that regression and blanket-relaxed two required keys
in the generic-provider parser.

## 2. Goal

Land eSignet support on `master` so that:

1. The default behaviour (JSON userinfo) is preserved byte-for-byte for
   every existing provider.
2. The eSignet JWT-userinfo flow is reachable via `dhis.conf` configuration
   only — no code changes to switch a provider over.
3. The generic provider builder is the integration point — no bespoke
   `EsignetProvider` class — so future IdPs with the same userinfo pattern
   are configured the same way.

## 3. Non-goals

- **JWE (encrypted) userinfo responses.** eSignet does not use them. If a
  future IdP requires JWE, add `user_info_jwe_algorithm` + a key reference
  pointing at the existing private-key keystore the registration already
  loads. Out of scope here.
- **Signed authorization requests.** Not in PR #22027; separate feature.
- **Per-provider override of authorization-grant or scopes** beyond what the
  generic builder already supports.

## 4. Configuration

Two new keys in the existing `oidc.provider.<id>.*` namespace, declared as
constants on `AbstractOidcProvider` and consumed by
`GenericOidcProviderBuilder` and `GenericOidcProviderConfigParser`.

| Key | Values | Default | Required | Notes |
|---|---|---|---|---|
| `user_info_response_type` | `json`, `jwt` | `json` | no | `jwt` selects the eSignet path. |
| `user_info_jws_algorithm` | `RS256`, `RS384`, `RS512`, `PS256`, `PS384`, `PS512`, `ES256`, `ES384`, `ES512` | `RS256` | no | Consulted only when `user_info_response_type=jwt`. Validated at parse time against a fixed allow-list. |

Per-provider scope (no global flag) — confirmed.

### Sample `dhis.conf` block (eSignet via generic provider)

```properties
oidc.provider.esignet.client_id                    = dhis2-client
oidc.provider.esignet.authorization_uri            = https://esignet.example/authorize
oidc.provider.esignet.token_uri                    = https://esignet.example/oauth/token
oidc.provider.esignet.user_info_uri                = https://esignet.example/oidc/userinfo
oidc.provider.esignet.jwk_uri                      = https://esignet.example/.well-known/jwks.json
oidc.provider.esignet.client_authentication_method = private_key_jwt
oidc.provider.esignet.keystore_path                = /opt/dhis2/esignet.p12
oidc.provider.esignet.keystore_password            = ...
oidc.provider.esignet.key_alias                    = esignet
oidc.provider.esignet.key_password                 = ...
oidc.provider.esignet.user_info_response_type      = jwt
oidc.provider.esignet.user_info_jws_algorithm      = RS256
oidc.provider.esignet.mapping_claim                = sub
```

## 5. Component changes

### 5.1 `AbstractOidcProvider`

Add two `String` constants:

```java
public static final String USER_INFO_RESPONSE_TYPE = "user_info_response_type";
public static final String USER_INFO_JWS_ALGORITHM = "user_info_jws_algorithm";
```

### 5.2 `UserInfoResponseType` enum (new)

```java
public enum UserInfoResponseType { JSON, JWT }
```

Lives next to `DhisOidcClientRegistration` in `org.hisp.dhis.security.oidc`.

### 5.3 `DhisOidcClientRegistration`

Two new fields:

```java
@Builder.Default
private final UserInfoResponseType userInfoResponseType = UserInfoResponseType.JSON;

private final JWSAlgorithm userInfoJwsAlgorithm; // null when JSON
```

### 5.4 `GenericOidcProviderBuilder`

Read the two new keys from the provider config, default `JSON` / `RS256`,
populate the new `DhisOidcClientRegistration` fields. JWSAlgorithm parsed
from the validated allow-list.

### 5.5 `GenericOidcProviderConfigParser`

- Register `USER_INFO_RESPONSE_TYPE` and `USER_INFO_JWS_ALGORITHM` in
  `KEY_REQUIRED_MAP` as `Boolean.FALSE`.
- Keep `CLIENT_SECRET` and `USERINFO_URI` as `Boolean.TRUE` in the static
  map.
- Add a post-validation step in `validateConfig` that relaxes `CLIENT_SECRET`
  to optional **only when** `client_authentication_method = private_key_jwt`
  and the four `keystore_*` keys are present. Keeps "did you mean…"
  diagnostics for ordinary misconfig and only loosens rules where the
  alternative is genuinely satisfied.
- Validate `user_info_response_type` against the enum and
  `user_info_jws_algorithm` against the fixed algorithm allow-list at parse
  time.

`USERINFO_URI` stays required in both modes — the JWT path still calls it,
only the `Accept` header changes.

### 5.6 `DhisOidcUserService` — restore inheritance, branch on type

```java
@Service
public class DhisOidcUserService extends OidcUserService {

  @Autowired private DhisOidcProviderRepository repo;
  @Autowired private UserService userService;
  @Autowired private SignedJwtUserInfoLoader signedJwtLoader;

  @Override
  public OidcUser loadUser(OidcUserRequest req) throws OAuth2AuthenticationException {
    DhisOidcClientRegistration reg =
        repo.getDhisOidcClientRegistration(req.getClientRegistration().getRegistrationId());

    return switch (reg.getUserInfoResponseType()) {
      case JSON -> loadFromJsonUserInfo(req, reg);
      case JWT  -> signedJwtLoader.load(req, reg);
    };
  }

  // loadFromJsonUserInfo == today's logic, unchanged: super.loadUser()
  // -> resolve mappingClaimKey -> userService.getUserByOpenId -> DhisOidcUser
}
```

The JSON branch is the existing method body verbatim. **No behaviour change
for any existing provider.**

### 5.7 `SignedJwtUserInfoLoader` (new collaborator)

Encapsulates the eSignet-style fetch + verify + map-to-user. Roughly:

```java
@Component
@RequiredArgsConstructor
public class SignedJwtUserInfoLoader {

  private final UserService userService;
  private final RestTemplate restTemplate;          // shared bean
  private final JwkSourceCache jwkSourceCache;      // per-registration

  public OidcUser load(OidcUserRequest req, DhisOidcClientRegistration reg) {
    String jwt = fetchJwtUserInfo(req);             // GET userinfo, Accept: application/jwt
    JWTClaimsSet claims = verify(jwt, reg);         // Nimbus, alg from reg
    String mappingValue = requireMappingClaim(claims, reg);
    User user = requireExistingExternalAuthUser(mappingValue, reg);
    UserDetails details = userService.createUserDetails(user);
    return new DhisOidcUser(details, claims.toJSONObject(),
                            IdTokenClaimNames.SUB, req.getIdToken());
  }
}
```

Notes:

- `principalNameAttribute` stays `IdTokenClaimNames.SUB` in **both** JSON
  and JWT modes. The mapping claim is used only for DHIS2 user lookup, not
  as the principal name. This preserves audit-log behaviour.
- The `User` lookup applies the same `isExternalAuth() / isDisabled() /
  isAccountNonExpired()` checks the JSON path applies today.
- Fetch failure, JWS-verification failure, missing mapping claim, and
  user-not-found each raise `OAuth2AuthenticationException` with distinct
  `OAuth2Error` codes (`invalid_user_info_response`, `jwt_processing_error`,
  `missing_mapping_claim`, `user_not_found`).
- `RestTemplate` is the shared OIDC HTTP client (or a new properly
  configured bean wired into `OidcConfig`) — not `new RestTemplate()` per
  call.
- Algorithm is taken from `reg.getUserInfoJwsAlgorithm()`; not hard-coded.

### 5.8 `JwkSourceCache` (new)

Builds a Nimbus `JWKSource<SecurityContext>` once per registration id and
caches it in a `ConcurrentHashMap`. Constructed lazily on first JWT-mode
login (or eagerly when the registration is built — implementation choice
during the writing-plans pass). Nimbus's built-in remote-key cache and
refresh policy handles JWKS rotation; we just don't rebuild the source on
every call.

### 5.9 `OAuth2Test`

Remove `@Disabled`. The JSON e2e path is restored once `DhisOidcUserService`
keeps its `extends OidcUserService` contract; the test should pass without
further change.

### 5.10 `PublicKeysController`

Keep the PR's `x509CertSHA256Thumbprint` addition. Additive — only emits a
thumbprint when the underlying JWK has one.

## 6. Tests

- **Unit — `SignedJwtUserInfoLoaderTest`**: happy path, bad signature,
  algorithm mismatch (config says RS256, token signed with PS256), missing
  mapping claim, HTTP fetch failure, user-not-found. Each asserts the
  correct `OAuth2Error.errorCode`.
- **Unit — `GenericOidcProviderConfigParserTest`** (extend existing):
  - `user_info_response_type=jwt` round-trips to `UserInfoResponseType.JWT`.
  - `user_info_jws_algorithm=PS256` round-trips to `JWSAlgorithm.PS256`.
  - Invalid algorithm fails parse-time validation.
  - `client_secret` missing without `private_key_jwt` → invalid.
  - `client_secret` missing with `private_key_jwt` + keystore keys → valid.
  - `user_info_uri` always required regardless of `user_info_response_type`.
- **Unit — `DhisOidcUserServiceTest`**: dispatch test that JSON-mode
  registrations call the existing path and JWT-mode registrations call
  `SignedJwtUserInfoLoader`.
- **E2E — `OAuth2Test`**: re-enabled, no other change.

## 7. Documentation

Short addition to `oauth.md` (the OIDC reference chapter):

- Two-row table for the new keys.
- The eSignet `dhis.conf` example block from §4.
- One-paragraph note that JSON userinfo remains the default and existing
  provider configs need no changes.

## 8. Migration / coordination

- No Flyway migration.
- No backwards-incompatible config change (new keys are optional with
  defaults that match prior behaviour).
- No WOW coordination entry needed.

## 9. Risks

- **Algorithm allow-list drift.** If Nimbus introduces new JWS algorithms,
  the allow-list won't include them until updated. Acceptable — failing
  closed at parse time is preferable to surprising algorithm acceptance.
- **JWKS rotation latency.** Nimbus's default refresh policy applies; if an
  IdP rotates keys aggressively we may need to expose its cache TTL as
  config later. Not addressed in this spec.
- **`UserService.getUserByOpenId` mapping.** The JWT mode looks up the user
  by the configured mapping claim's value, same as the JSON mode. Confirmed
  no audit-log breakage because `principalNameAttribute` stays `sub`.
