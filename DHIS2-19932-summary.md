# DHIS2-19932: Hide Internal OIDC Provider from Login + Simplify Android OAuth2 Config

## Problem

When the Spring Authorization Server is enabled (`oauth2.server.enabled = on`), DHIS2 becomes its own Identity Provider. Two problems arise:

1. **Unwanted "Sign in with DHIS2" button on the web login page.** The `oidc.provider.dhis2.*` properties are parsed by `GenericOidcProviderConfigParser` as a generic visible provider (since `"dhis2"` was not in its reserved list), creating a duplicate registration that renders on the login screen.

2. **Verbose, redundant configuration.** Enabling OAuth2 for Android required ~15 lines of config, most of which duplicate `server.base.url` across various URI properties.

## Solution

### Minimal Config (after this change)

```properties
server.base.url = http://10.0.2.2:8080
oauth2.server.enabled = on
```

That's it. Two lines. Everything else is auto-configured.

### What Changed (7 modified files, 1 new file)

#### 1. `GenericOidcProviderConfigParser.java`
Added `"dhis2"` to `RESERVED_PROVIDER_IDS`:
```java
Set.of("azure", "google", "wso2", "dhis2")
```
Prevents the generic parser from creating a duplicate visible provider from `oidc.provider.dhis2.*` properties.

#### 2. `DhisOidcClientRegistration.java`
Added a new boolean field:
```java
@Builder.Default private final boolean visibleOnLoginPage = true;
```
All existing providers default to visible (backward compatible). The internal provider sets this to `false`.

#### 3. `Dhis2InternalOidcProvider.java`
Two changes:
- **Set `visibleOnLoginPage(false)`** so the internal provider never appears on the web login page.
- **Fall back to `SERVER_BASE_URL`** when `oidc.provider.dhis2.server_url` is not explicitly set. This eliminates the need to manually configure all OAuth2 endpoint URIs.

#### 4. `ConfigurationKey.java`
Changed `OIDC_DHIS2_INTERNAL_SERVER_URL` default from `"http://localhost:8080"` to `""`. Combined with the fallback in step 3, an unset `server_url` now derives all URIs from `server.base.url`.

#### 5. `LoginConfigController.java`
Added filtering in `getRegisteredOidcProviders()` to skip providers where `visibleOnLoginPage == false`. The internal provider no longer appears in the `/api/loginConfig` response.

#### 6. `DhisWebApiWebSecurityConfig.java`
Modified `configureOAuthTokenFilters()` to also enable the JWT bearer token filter when `OAUTH2_SERVER_ENABLED` is on:
```java
if (dhisConfig.isEnabled(ConfigurationKey.ENABLE_JWT_OIDC_TOKEN_AUTHENTICATION)
    || dhisConfig.isEnabled(ConfigurationKey.OAUTH2_SERVER_ENABLED)) {
```
Android clients can now authenticate with JWT bearer tokens without needing `oidc.jwt.token.authentication.enabled = on`.

#### 7. `GenericOidcProviderBuilderConfigParserTest.java`
Added 3 new tests:
- `parseDhis2ProviderIsReservedAndSkipped` -- `oidc.provider.dhis2.*` properties are skipped by generic parser
- `parseDhis2ProviderSkippedButOthersStillParsed` -- other providers work alongside `dhis2`
- `parseSkipsAllReservedProviderIds` -- all reserved IDs are skipped

#### 8. `Dhis2InternalOidcProviderTest.java` (new)
6 unit tests:
- Returns `null` when client ID is empty
- Falls back to `SERVER_BASE_URL` when server URL is empty
- Uses explicit server URL when provided
- Strips trailing slash from base URL
- Sets `visibleOnLoginPage` to `false`
- Registration ID is `dhis2-internal`

## Coexistence with External Providers

- External providers (Google, Azure, custom) have `visibleOnLoginPage = true` (default) and continue to appear on the login page
- The internal provider (`dhis2-internal`) has `visibleOnLoginPage = false` and is hidden
- `GenericOidcProviderConfigParser` skips the `"dhis2"` prefix, preventing duplicate registrations
- JWT token validation via `Dhis2JwtAuthenticationManagerResolver` finds the internal provider by issuer URI
- `DhisOidcProviderRepository.addRegistration()` uses `putIfAbsent`, so there are no ordering conflicts

## What Stays Unchanged

- `oidc.oauth2.login.enabled` -- still only needed for external OIDC providers with proper OIDC logout
- `oidc.logout.redirect_url` -- only needed for external OIDC providers
- All existing external OIDC provider configs -- untouched
- The authorization server config (`AuthorizationServerConfig`) -- untouched
- DCR flow for Android client registration -- untouched

## Test Results

All 16 tests pass (6 new + 3 new + 7 existing):
```
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```
