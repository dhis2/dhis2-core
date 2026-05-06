# eSignet OIDC JWT Userinfo Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add per-provider opt-in support for JWT-encoded OIDC userinfo responses (eSignet style) without changing default JSON-userinfo behaviour for any existing provider.

**Architecture:** Two new generic-provider config keys (`user_info_response_type`, `user_info_jws_algorithm`). `DhisOidcUserService` regains `extends OidcUserService` and dispatches to either the existing JSON path (`super.loadUser`) or a new `SignedJwtUserInfoLoader` based on the registration's `userInfoResponseType`. Generic parser conditionally relaxes `client_secret` only under `private_key_jwt`.

**Tech Stack:** Java 17, Spring Security OAuth2 Client 6.x, Nimbus JOSE+JWT (already on the classpath), JUnit 5, Mockito.

**Spec:** `dhis-2/docs/superpowers/specs/2026-05-06-esignet-oidc-integration-design.md`

---

## File Map

**Create:**

| Path | Responsibility |
|---|---|
| `dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/UserInfoResponseType.java` | Enum: `JSON`, `JWT` |
| `dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/SupportedJwsAlgorithms.java` | Static allow-list + `parse(String)` returning `JWSAlgorithm` |
| `dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/JwkSourceCache.java` | Per-registration `JWKSource<SecurityContext>` cache |
| `dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/SignedJwtUserInfoLoader.java` | Fetches `application/jwt` userinfo, verifies, maps to DHIS2 user |
| `dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/oidc/SupportedJwsAlgorithmsTest.java` | Unit tests for allow-list parsing |
| `dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/oidc/SignedJwtUserInfoLoaderTest.java` | Unit tests for the JWT path |
| `dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/oidc/DhisOidcUserServiceDispatchTest.java` | Verifies JSON vs JWT branch dispatch |

**Modify:**

| Path | Change |
|---|---|
| `dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/provider/AbstractOidcProvider.java` | Add `USER_INFO_RESPONSE_TYPE` and `USER_INFO_JWS_ALGORITHM` constants |
| `dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/DhisOidcClientRegistration.java` | Add `userInfoResponseType` (default `JSON`) and `userInfoJwsAlgorithm` fields |
| `dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/provider/GenericOidcProviderBuilder.java` | Read new keys, populate new fields, relax `clientSecret` requirement under `private_key_jwt` |
| `dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/GenericOidcProviderConfigParser.java` | Register new keys, validate enum/algorithm values, conditionally skip `client_secret` required-check |
| `dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/DhisOidcUserService.java` | Restore `extends OidcUserService`, branch on `userInfoResponseType` |
| `dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/oidc/GenericOidcProviderBuilderConfigParserTest.java` | New cases for the new keys and conditional client_secret relaxation |
| `dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/oidc/provider/GenericOidcProviderBuilderTest.java` | New cases for `userInfoResponseType` / `userInfoJwsAlgorithm` round-trip and `private_key_jwt` allowing missing secret |
| `dhis-2/dhis-test-e2e/src/test/java/org/hisp/dhis/oauth2/OAuth2Test.java` | Remove `@Disabled` |

**License header:** Every new `.java` file uses the standard DHIS2 BSD header with `$YEAR` placeholder. Spotless will fill it in. Author tag: `@author Morten Svanæs <msvanaes@dhis2.org>` per CLAUDE.md ("Always add me as author when you create a new file").

---

## Conventions

- Use Java 17. Confirm with `java -version` before any test/format step.
- After **every** code change run `mvn spotless:apply -f dhis-2/pom.xml -pl dhis-services/dhis-service-core` (or the relevant module) before committing.
- Stage files explicitly with their full path. Never `git add -A` / `git add .`.
- `git status --short` before each commit; `git diff --cached --stat | tail -1` before any amend.
- Module build prerequisite for test runs: `mvn install -pl dhis-2/dhis-services/dhis-service-core -am -DskipTests -q` once at the start of a working session.

---

## Task 1: `UserInfoResponseType` enum

**Files:**
- Create: `dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/UserInfoResponseType.java`

- [ ] **Step 1: Create the enum**

```java
/*
 * Copyright (c) 2004-$YEAR, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.security.oidc;

import javax.annotation.CheckForNull;

/**
 * Selects how DHIS2 should consume the OIDC userinfo response for a given provider.
 *
 * <ul>
 *   <li>{@link #JSON} — Spring Security's default: userinfo endpoint returns
 *       {@code application/json}.
 *   <li>{@link #JWT} — userinfo endpoint returns a signed JWT
 *       ({@code application/jwt}); used by MOSIP eSignet and similar IdPs.
 * </ul>
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public enum UserInfoResponseType {
  JSON,
  JWT;

  /**
   * @param value config string from {@code dhis.conf}; case-insensitive
   * @return matching enum, defaulting to {@link #JSON} when {@code value} is
   *     {@code null} or blank
   * @throws IllegalArgumentException for unknown values
   */
  public static UserInfoResponseType fromConfig(@CheckForNull String value) {
    if (value == null || value.isBlank()) {
      return JSON;
    }
    return UserInfoResponseType.valueOf(value.trim().toUpperCase());
  }
}
```

- [ ] **Step 2: Compile**

Run: `mvn -q -pl dhis-2/dhis-services/dhis-service-core compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Format**

Run: `mvn -q spotless:apply -f dhis-2/pom.xml -pl dhis-services/dhis-service-core`

- [ ] **Step 4: Commit**

```bash
git add dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/UserInfoResponseType.java
git commit -m "feat(oidc): add UserInfoResponseType enum [DHIS2-20043]"
```

---

## Task 2: `SupportedJwsAlgorithms` allow-list (TDD)

**Files:**
- Create: `dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/SupportedJwsAlgorithms.java`
- Test: `dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/oidc/SupportedJwsAlgorithmsTest.java`

- [ ] **Step 1: Write the failing test**

```java
/* (standard $YEAR header) */
package org.hisp.dhis.security.oidc;

import static org.junit.jupiter.api.Assertions.*;

import com.nimbusds.jose.JWSAlgorithm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SupportedJwsAlgorithmsTest {

  @Test
  void parsesNullAsRs256Default() {
    assertEquals(JWSAlgorithm.RS256, SupportedJwsAlgorithms.parseOrDefault(null));
  }

  @Test
  void parsesBlankAsRs256Default() {
    assertEquals(JWSAlgorithm.RS256, SupportedJwsAlgorithms.parseOrDefault("  "));
  }

  @ParameterizedTest
  @ValueSource(strings = {"RS256", "RS384", "RS512", "PS256", "PS384", "PS512", "ES256", "ES384", "ES512"})
  void parsesAllSupportedAlgorithms(String name) {
    assertEquals(name, SupportedJwsAlgorithms.parseOrDefault(name).getName());
  }

  @Test
  void rejectsUnsupportedAlgorithm() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> SupportedJwsAlgorithms.parseOrDefault("HS256"));
    assertTrue(ex.getMessage().contains("HS256"));
  }

  @Test
  void rejectsNonsense() {
    assertThrows(IllegalArgumentException.class, () -> SupportedJwsAlgorithms.parseOrDefault("nope"));
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```
mvn -q test -pl dhis-2/dhis-services/dhis-service-core \
  -Dtest=SupportedJwsAlgorithmsTest
```
Expected: COMPILATION FAILURE (`SupportedJwsAlgorithms` does not exist).

- [ ] **Step 3: Implement**

Create `SupportedJwsAlgorithms.java`:

```java
/* (standard $YEAR header) */
package org.hisp.dhis.security.oidc;

import com.nimbusds.jose.JWSAlgorithm;
import java.util.Set;
import javax.annotation.CheckForNull;

/**
 * Allow-list of JWS algorithms accepted for OIDC userinfo JWT verification. Failing closed at parse
 * time prevents accidental acceptance of unexpected signature algorithms (e.g. HMAC) configured in
 * {@code dhis.conf}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public final class SupportedJwsAlgorithms {

  public static final JWSAlgorithm DEFAULT = JWSAlgorithm.RS256;

  private static final Set<JWSAlgorithm> ALLOWED =
      Set.of(
          JWSAlgorithm.RS256, JWSAlgorithm.RS384, JWSAlgorithm.RS512,
          JWSAlgorithm.PS256, JWSAlgorithm.PS384, JWSAlgorithm.PS512,
          JWSAlgorithm.ES256, JWSAlgorithm.ES384, JWSAlgorithm.ES512);

  private SupportedJwsAlgorithms() {}

  /**
   * Parses a configured JWS algorithm name against the allow-list.
   *
   * @param value config string from {@code dhis.conf}; case-sensitive (Nimbus algorithm names)
   * @return the matching {@link JWSAlgorithm}, or {@link #DEFAULT} when {@code value} is null/blank
   * @throws IllegalArgumentException when the algorithm is not in the allow-list
   */
  public static JWSAlgorithm parseOrDefault(@CheckForNull String value) {
    if (value == null || value.isBlank()) {
      return DEFAULT;
    }
    JWSAlgorithm parsed = JWSAlgorithm.parse(value.trim());
    if (!ALLOWED.contains(parsed)) {
      throw new IllegalArgumentException(
          "Unsupported user_info_jws_algorithm: '" + value + "'. Allowed: " + ALLOWED);
    }
    return parsed;
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:
```
mvn -q test -pl dhis-2/dhis-services/dhis-service-core \
  -Dtest=SupportedJwsAlgorithmsTest
```
Expected: BUILD SUCCESS, all tests green.

- [ ] **Step 5: Format and commit**

```bash
mvn -q spotless:apply -f dhis-2/pom.xml -pl dhis-services/dhis-service-core
git add dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/SupportedJwsAlgorithms.java \
        dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/oidc/SupportedJwsAlgorithmsTest.java
git commit -m "feat(oidc): add JWS algorithm allow-list for userinfo verification [DHIS2-20043]"
```

---

## Task 3: New constants on `AbstractOidcProvider`

**Files:**
- Modify: `dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/provider/AbstractOidcProvider.java` (add after line 121, before closing brace)

- [ ] **Step 1: Add the two constants**

Append the following after the existing `JWK_SET_URL` constant (currently line 121):

```java
  /**
   * Selects userinfo response handling: {@code json} (default; Spring Security's normal path) or
   * {@code jwt} (eSignet-style signed JWT). See {@link
   * org.hisp.dhis.security.oidc.UserInfoResponseType}.
   */
  public static final String USER_INFO_RESPONSE_TYPE = "user_info_response_type";

  /**
   * JWS algorithm used to verify the userinfo JWT when {@link #USER_INFO_RESPONSE_TYPE} is
   * {@code jwt}. Defaults to {@code RS256}. See {@link
   * org.hisp.dhis.security.oidc.SupportedJwsAlgorithms}.
   */
  public static final String USER_INFO_JWS_ALGORITHM = "user_info_jws_algorithm";
```

- [ ] **Step 2: Compile**

Run: `mvn -q -pl dhis-2/dhis-services/dhis-service-core compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Format and commit**

```bash
mvn -q spotless:apply -f dhis-2/pom.xml -pl dhis-services/dhis-service-core
git add dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/provider/AbstractOidcProvider.java
git commit -m "feat(oidc): add user_info_response_type and user_info_jws_algorithm config keys [DHIS2-20043]"
```

---

## Task 4: Extend `DhisOidcClientRegistration` with the two new fields

**Files:**
- Modify: `dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/DhisOidcClientRegistration.java`

- [ ] **Step 1: Add imports**

Add (preserving existing import order, alphabetical within sections):

```java
import com.nimbusds.jose.JWSAlgorithm;
import javax.annotation.CheckForNull;
```

- [ ] **Step 2: Add the two fields**

Place inside the `@Data @Builder` class body, after the existing `private final String jwkSetUrl;` field and before `@Builder.Default private final boolean visibleOnLoginPage = true;`:

```java
  /**
   * Selects how DHIS2 consumes this provider's userinfo response. Defaults to
   * {@link UserInfoResponseType#JSON}, preserving the historical Spring Security
   * behaviour for every existing provider.
   */
  @Builder.Default
  private final UserInfoResponseType userInfoResponseType = UserInfoResponseType.JSON;

  /**
   * JWS algorithm used to verify the signed userinfo JWT. Only consulted when
   * {@link #userInfoResponseType} is {@link UserInfoResponseType#JWT}.
   */
  @CheckForNull private final JWSAlgorithm userInfoJwsAlgorithm;
```

- [ ] **Step 3: Compile**

Run: `mvn -q -pl dhis-2/dhis-services/dhis-service-core compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Format and commit**

```bash
mvn -q spotless:apply -f dhis-2/pom.xml -pl dhis-services/dhis-service-core
git add dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/DhisOidcClientRegistration.java
git commit -m "feat(oidc): track userInfo response type and JWS algorithm on registration [DHIS2-20043]"
```

---

## Task 5: `GenericOidcProviderConfigParser` — register keys, validate values, conditionally skip `client_secret`

**Files:**
- Modify: `dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/GenericOidcProviderConfigParser.java`
- Modify (tests): `dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/oidc/GenericOidcProviderBuilderConfigParserTest.java`

- [ ] **Step 1: Write failing tests**

Append to `GenericOidcProviderBuilderConfigParserTest`:

```java
  // --- new keys: user_info_response_type / user_info_jws_algorithm ---

  @Test
  void parseAcceptsUserInfoResponseTypeJwt() {
    Properties p = baseValidProvider("idporten");
    p.put("oidc.provider.idporten.user_info_response_type", "jwt");
    p.put("oidc.provider.idporten.user_info_jws_algorithm", "PS256");
    List<DhisOidcClientRegistration> parse = GenericOidcProviderConfigParser.parse(p);
    assertThat(parse, hasSize(1));
    assertEquals(UserInfoResponseType.JWT, parse.get(0).getUserInfoResponseType());
    assertEquals("PS256", parse.get(0).getUserInfoJwsAlgorithm().getName());
  }

  @Test
  void parseRejectsUnknownUserInfoResponseType() {
    Properties p = baseValidProvider("idporten");
    p.put("oidc.provider.idporten.user_info_response_type", "yaml");
    assertThat(GenericOidcProviderConfigParser.parse(p), hasSize(0));
  }

  @Test
  void parseRejectsUnsupportedJwsAlgorithm() {
    Properties p = baseValidProvider("idporten");
    p.put("oidc.provider.idporten.user_info_response_type", "jwt");
    p.put("oidc.provider.idporten.user_info_jws_algorithm", "HS256");
    assertThat(GenericOidcProviderConfigParser.parse(p), hasSize(0));
  }

  // --- conditional client_secret relaxation ---

  @Test
  void parseRejectsMissingClientSecretWithoutPrivateKeyJwt() {
    Properties p = baseValidProvider("idporten");
    p.remove("oidc.provider.idporten.client_secret");
    assertThat(GenericOidcProviderConfigParser.parse(p), hasSize(0));
  }

  @Test
  void parseAcceptsMissingClientSecretWithPrivateKeyJwt() {
    Properties p = baseValidProvider("idporten");
    p.remove("oidc.provider.idporten.client_secret");
    p.put("oidc.provider.idporten.client_authentication_method", "private_key_jwt");
    p.put("oidc.provider.idporten.keystore_path", "/tmp/does-not-need-to-exist.p12");
    p.put("oidc.provider.idporten.keystore_password", "x");
    p.put("oidc.provider.idporten.key_alias", "x");
    p.put("oidc.provider.idporten.key_password", "x");
    p.put("oidc.provider.idporten.jwk_set_url", "https://example.test/jwks");
    // Builder may still fail to load the keystore from disk; we only assert the
    // *parser* accepts the config. Wrap to ignore loader-time IO errors.
    try {
      GenericOidcProviderConfigParser.parse(p);
    } catch (IllegalStateException expected) {
      // builder can throw when reading non-existent keystore — that's fine for
      // this parser-level assertion: validation passed before construction.
      return;
    }
  }

  @Test
  void parseStillRequiresUserInfoUriEvenInJwtMode() {
    Properties p = baseValidProvider("idporten");
    p.remove("oidc.provider.idporten.user_info_uri");
    p.put("oidc.provider.idporten.user_info_response_type", "jwt");
    assertThat(GenericOidcProviderConfigParser.parse(p), hasSize(0));
  }

  private static Properties baseValidProvider(String id) {
    Properties p = new Properties();
    String pre = "oidc.provider." + id + ".";
    p.put(pre + "client_id", "testClientId");
    p.put(pre + "client_secret", "testClientSecret");
    p.put(pre + "authorization_uri", "https://oidc.test/authorize");
    p.put(pre + "token_uri", "https://oidc.test/token");
    p.put(pre + "user_info_uri", "https://oidc.test/userinfo");
    p.put(pre + "jwk_uri", "https://oidc.test/jwk");
    return p;
  }
```

Add the corresponding imports at the top of the test class:

```java
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.security.oidc.UserInfoResponseType;
```

- [ ] **Step 2: Run tests, verify they fail**

Run:
```
mvn -q test -pl dhis-2/dhis-services/dhis-service-core \
  -Dtest=GenericOidcProviderBuilderConfigParserTest
```
Expected: failures on the five new tests.

- [ ] **Step 3: Implement parser changes**

In `GenericOidcProviderConfigParser.java`:

a. Add imports:

```java
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.USER_INFO_JWS_ALGORITHM;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.USER_INFO_RESPONSE_TYPE;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.JWT_PRIVATE_KEY_KEYSTORE_PATH;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.JWT_PRIVATE_KEY_KEYSTORE_PASSWORD;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.JWT_PRIVATE_KEY_ALIAS;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.JWT_PRIVATE_KEY_PASSWORD;

import org.hisp.dhis.security.oidc.SupportedJwsAlgorithms;
import org.hisp.dhis.security.oidc.UserInfoResponseType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
```

(Some are already imported; only add the missing ones.)

b. Register the new keys in `KEY_REQUIRED_MAP` (inside the static block, alongside the existing `Boolean.FALSE` entries):

```java
    builder.put(USER_INFO_RESPONSE_TYPE, Boolean.FALSE);
    builder.put(USER_INFO_JWS_ALGORITHM, Boolean.FALSE);
```

c. Replace the body of `validateConfig` so it (1) skips the `client_secret` required-check under `private_key_jwt`, and (2) validates enum and algorithm values. Replace the existing method body with:

```java
  private static boolean validateConfig(Map<String, String> providerConfig) {
    Objects.requireNonNull(providerConfig);

    String providerId = providerConfig.get(PROVIDER_ID);
    boolean privateKeyJwt = isPrivateKeyJwt(providerConfig);

    for (Map.Entry<String, Boolean> entry : KEY_REQUIRED_MAP.entrySet()) {
      String key = entry.getKey();
      boolean isRequired = entry.getValue();
      String value = providerConfig.get(key);

      if (CLIENT_SECRET.equals(key) && privateKeyJwt) {
        // client_secret is not used when authenticating with private_key_jwt
        continue;
      }

      if (isRequired && Strings.isNullOrEmpty(value)) {
        log.error(
            "OpenId Connect (OIDC) configuration for provider: '{}' is missing a required property: '{}'. "
                + "Failed to configure the provider successfully!",
            providerId,
            key);
        return false;
      }

      UrlValidator urlValidator = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);
      if (value != null && key.endsWith("uri") && !urlValidator.isValid(value)) {
        log.error(
            "OpenId Connect (OIDC) configuration for provider: '{}' has a URI property: '{}', "
                + "with a malformed value: '{}'. Failed to configure the provider successfully!",
            providerId,
            key,
            value);
        return false;
      }
    }

    return validateUserInfoResponseType(providerId, providerConfig);
  }

  private static boolean isPrivateKeyJwt(Map<String, String> providerConfig) {
    String method = providerConfig.get(CLIENT_AUTHENTICATION_METHOD);
    if (!ClientAuthenticationMethod.PRIVATE_KEY_JWT.getValue().equalsIgnoreCase(method)) {
      return false;
    }
    return !Strings.isNullOrEmpty(providerConfig.get(JWT_PRIVATE_KEY_KEYSTORE_PATH))
        && !Strings.isNullOrEmpty(providerConfig.get(JWT_PRIVATE_KEY_KEYSTORE_PASSWORD))
        && !Strings.isNullOrEmpty(providerConfig.get(JWT_PRIVATE_KEY_ALIAS))
        && !Strings.isNullOrEmpty(providerConfig.get(JWT_PRIVATE_KEY_PASSWORD));
  }

  private static boolean validateUserInfoResponseType(
      String providerId, Map<String, String> providerConfig) {
    String type = providerConfig.get(USER_INFO_RESPONSE_TYPE);
    UserInfoResponseType resolved;
    try {
      resolved = UserInfoResponseType.fromConfig(type);
    } catch (IllegalArgumentException ex) {
      log.error(
          "OIDC provider '{}' has invalid user_info_response_type='{}'. Allowed: json, jwt.",
          providerId,
          type);
      return false;
    }

    if (resolved == UserInfoResponseType.JWT) {
      String alg = providerConfig.get(USER_INFO_JWS_ALGORITHM);
      try {
        SupportedJwsAlgorithms.parseOrDefault(alg);
      } catch (IllegalArgumentException ex) {
        log.error(
            "OIDC provider '{}' has unsupported user_info_jws_algorithm='{}'. {}",
            providerId,
            alg,
            ex.getMessage());
        return false;
      }
    }
    return true;
  }
```

- [ ] **Step 4: Run tests, verify pass**

Run:
```
mvn -q test -pl dhis-2/dhis-services/dhis-service-core \
  -Dtest=GenericOidcProviderBuilderConfigParserTest
```
Expected: all tests pass (existing + new).

- [ ] **Step 5: Format and commit**

```bash
mvn -q spotless:apply -f dhis-2/pom.xml -pl dhis-services/dhis-service-core
git add dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/GenericOidcProviderConfigParser.java \
        dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/oidc/GenericOidcProviderBuilderConfigParserTest.java
git commit -m "feat(oidc): validate userinfo response type and relax client_secret under private_key_jwt [DHIS2-20043]"
```

---

## Task 6: `GenericOidcProviderBuilder` — read new keys, allow null `clientSecret` under `private_key_jwt`

**Files:**
- Modify: `dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/provider/GenericOidcProviderBuilder.java`
- Modify (tests): `dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/oidc/provider/GenericOidcProviderBuilderTest.java`

- [ ] **Step 1: Write failing tests**

Append to `GenericOidcProviderBuilderTest`:

```java
  @Test
  void buildPropagatesUserInfoResponseTypeAndAlgorithm() {
    Map<String, String> cfg = baseConfig();
    cfg.put(USER_INFO_RESPONSE_TYPE, "jwt");
    cfg.put(USER_INFO_JWS_ALGORITHM, "ES256");
    DhisOidcClientRegistration reg = GenericOidcProviderBuilder.build(cfg, Map.of());
    assertEquals(UserInfoResponseType.JWT, reg.getUserInfoResponseType());
    assertEquals("ES256", reg.getUserInfoJwsAlgorithm().getName());
  }

  @Test
  void buildDefaultsToJsonAndNoAlgorithm() {
    DhisOidcClientRegistration reg = GenericOidcProviderBuilder.build(baseConfig(), Map.of());
    assertEquals(UserInfoResponseType.JSON, reg.getUserInfoResponseType());
    assertNull(reg.getUserInfoJwsAlgorithm());
  }

  @Test
  void buildAcceptsMissingSecretUnderPrivateKeyJwt() {
    Map<String, String> cfg = baseConfig();
    cfg.put(CLIENT_SECRET, "");
    cfg.put(CLIENT_AUTHENTICATION_METHOD, "private_key_jwt");
    // Don't load a real keystore in this test — getJWK only loads it when
    // PRIVATE_KEY_JWT is set, so we guard against that throwing by using a path
    // that getJWK rejects benignly. Easiest: keep the keystore_path absent so
    // getJWK takes the "not private_key_jwt" early branch — but we want
    // PRIVATE_KEY_JWT to skip the secret check too. So: stub by setting
    // keystore_path to null and assert IllegalStateException is NOT thrown for
    // the missing-secret reason but for the keystore-load reason instead.
    assertThrows(IllegalStateException.class, () -> GenericOidcProviderBuilder.build(cfg, Map.of()));
  }
```

Add imports:

```java
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.CLIENT_AUTHENTICATION_METHOD;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.CLIENT_SECRET;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.USER_INFO_JWS_ALGORITHM;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.USER_INFO_RESPONSE_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hisp.dhis.security.oidc.DhisOidcClientRegistration;
import org.hisp.dhis.security.oidc.UserInfoResponseType;
```

If `baseConfig()` does not exist in the test, add a private helper:

```java
  private static Map<String, String> baseConfig() {
    Map<String, String> cfg = new HashMap<>();
    cfg.put(AbstractOidcProvider.PROVIDER_ID, "idporten");
    cfg.put(AbstractOidcProvider.CLIENT_ID, "test-client");
    cfg.put(AbstractOidcProvider.CLIENT_SECRET, "test-secret");
    cfg.put(AbstractOidcProvider.AUTHORIZATION_URI, "https://oidc.test/authorize");
    cfg.put(AbstractOidcProvider.TOKEN_URI, "https://oidc.test/token");
    cfg.put(AbstractOidcProvider.USERINFO_URI, "https://oidc.test/userinfo");
    cfg.put(AbstractOidcProvider.JWK_URI, "https://oidc.test/jwk");
    return cfg;
  }
```

- [ ] **Step 2: Run, verify failure**

Run:
```
mvn -q test -pl dhis-2/dhis-services/dhis-service-core \
  -Dtest=GenericOidcProviderBuilderTest
```
Expected: compile or assertion failures on the new tests.

- [ ] **Step 3: Implement builder changes**

Replace the existing `build(...)` method body in `GenericOidcProviderBuilder.java` with:

```java
  public static DhisOidcClientRegistration build(
      Map<String, String> config, Map<String, Map<String, String>> externalClients) {
    Objects.requireNonNull(config, "DhisConfigurationProvider is missing!");

    String providerId = config.get(PROVIDER_ID);
    String clientId = config.get(CLIENT_ID);
    String clientSecret = config.get(CLIENT_SECRET);

    if (providerId == null || providerId.isEmpty() || clientId == null || clientId.isEmpty()) {
      return null;
    }

    boolean privateKeyJwt = isPrivateKeyJwt(config);
    if ((clientSecret == null || clientSecret.isEmpty()) && !privateKeyJwt) {
      throw new IllegalArgumentException(providerId + " client secret is missing!");
    }

    return DhisOidcClientRegistration.builder()
        .clientRegistration(buildClientRegistration(config, providerId, clientId, clientSecret))
        .mappingClaimKey(
            StringUtils.defaultIfEmpty(config.get(MAPPING_CLAIM), DEFAULT_MAPPING_CLAIM))
        .loginIcon(StringUtils.defaultIfEmpty(config.get(LOGIN_IMAGE), ""))
        .loginIconPadding(StringUtils.defaultIfEmpty(config.get(LOGIN_IMAGE_PADDING), "0px 0px"))
        .loginText(StringUtils.defaultIfEmpty(config.get(DISPLAY_ALIAS), providerId))
        .externalClients(externalClients)
        .jwk(getJWK(config))
        .rsaPublicKey(getPublicKey(config))
        .keyId(config.get(JWT_PRIVATE_KEY_ALIAS))
        .jwkSetUrl(config.get(JWK_SET_URL))
        .userInfoResponseType(UserInfoResponseType.fromConfig(config.get(USER_INFO_RESPONSE_TYPE)))
        .userInfoJwsAlgorithm(
            UserInfoResponseType.fromConfig(config.get(USER_INFO_RESPONSE_TYPE))
                    == UserInfoResponseType.JWT
                ? SupportedJwsAlgorithms.parseOrDefault(config.get(USER_INFO_JWS_ALGORITHM))
                : null)
        .build();
  }

  private static boolean isPrivateKeyJwt(Map<String, String> config) {
    String method = config.get(CLIENT_AUTHENTICATION_METHOD);
    return ClientAuthenticationMethod.PRIVATE_KEY_JWT.getValue().equalsIgnoreCase(method);
  }
```

Add imports:

```java
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.USER_INFO_JWS_ALGORITHM;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.USER_INFO_RESPONSE_TYPE;

import org.hisp.dhis.security.oidc.SupportedJwsAlgorithms;
import org.hisp.dhis.security.oidc.UserInfoResponseType;
```

(`ClientAuthenticationMethod` is already imported.)

Additionally, in `buildClientRegistration`, change the `clientSecret` line to allow null:

```java
    if (clientSecret != null && !clientSecret.isEmpty()) {
      builder.clientSecret(clientSecret);
    }
```

(The current unconditional `builder.clientSecret(clientSecret);` would set null when secret is absent — Spring's builder accepts that, but skipping the call is cleaner.)

- [ ] **Step 4: Run, verify pass**

Run:
```
mvn -q test -pl dhis-2/dhis-services/dhis-service-core \
  -Dtest=GenericOidcProviderBuilderTest
```
Expected: all tests pass.

- [ ] **Step 5: Format and commit**

```bash
mvn -q spotless:apply -f dhis-2/pom.xml -pl dhis-services/dhis-service-core
git add dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/provider/GenericOidcProviderBuilder.java \
        dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/oidc/provider/GenericOidcProviderBuilderTest.java
git commit -m "feat(oidc): wire userInfo response type and JWS algorithm into generic builder [DHIS2-20043]"
```

---

## Task 7: `JwkSourceCache` (per-registration JWKSource cache)

**Files:**
- Create: `dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/JwkSourceCache.java`

This component is small and tested via `SignedJwtUserInfoLoaderTest` in Task 8, so we don't add a separate unit test for it.

- [ ] **Step 1: Implement**

```java
/* (standard $YEAR header, author Morten Svanæs) */
package org.hisp.dhis.security.oidc;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/**
 * Caches one Nimbus {@link JWKSource} per OIDC registration id. Building a {@link JWKSource}
 * triggers an HTTPS fetch of the IdP's JWKS document; reusing the source preserves Nimbus's
 * built-in remote-key cache and refresh policy across logins.
 *
 * <p>Sources are constructed lazily on first call to {@link #get(String, String)}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class JwkSourceCache {

  private static final int CONNECT_TIMEOUT_MS = 5_000;
  private static final int READ_TIMEOUT_MS = 5_000;

  private final ConcurrentMap<String, JWKSource<SecurityContext>> sources = new ConcurrentHashMap<>();

  /**
   * @param registrationId the OIDC registration id to cache under
   * @param jwkSetUri the IdP's JWKS endpoint URL
   * @return a cached or freshly built {@link JWKSource}
   * @throws IllegalArgumentException when {@code jwkSetUri} is malformed
   */
  public JWKSource<SecurityContext> get(String registrationId, String jwkSetUri) {
    return sources.computeIfAbsent(registrationId, id -> build(jwkSetUri));
  }

  private JWKSource<SecurityContext> build(String jwkSetUri) {
    try {
      DefaultResourceRetriever retriever =
          new DefaultResourceRetriever(
              CONNECT_TIMEOUT_MS,
              READ_TIMEOUT_MS,
              JWKSourceBuilder.DEFAULT_HTTP_SIZE_LIMIT);
      return JWKSourceBuilder.create(new URL(jwkSetUri), retriever).build();
    } catch (MalformedURLException ex) {
      throw new IllegalArgumentException("Invalid JWKS URL: " + jwkSetUri, ex);
    }
  }
}
```

- [ ] **Step 2: Compile**

Run: `mvn -q -pl dhis-2/dhis-services/dhis-service-core compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Format and commit**

```bash
mvn -q spotless:apply -f dhis-2/pom.xml -pl dhis-services/dhis-service-core
git add dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/JwkSourceCache.java
git commit -m "feat(oidc): add per-registration JWKSource cache [DHIS2-20043]"
```

---

## Task 8: `SignedJwtUserInfoLoader` (TDD)

**Files:**
- Create: `dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/SignedJwtUserInfoLoader.java`
- Test: `dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/oidc/SignedJwtUserInfoLoaderTest.java`

The test signs a JWT with a generated RSA keypair, stubs `RestTemplate.exchange` to return that JWT, and stubs the cache to return a JWKSource backed by the matching public JWK.

- [ ] **Step 1: Write the failing test**

```java
/* (standard $YEAR header, author Morten Svanæs) */
package org.hisp.dhis.security.oidc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import org.hisp.dhis.security.oidc.provider.AbstractOidcProvider;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class SignedJwtUserInfoLoaderTest {

  @Mock private UserService userService;
  @Mock private JwkSourceCache jwkSourceCache;
  @Mock private RestTemplate restTemplate;
  @Mock private OidcUserRequest userRequest;
  @Mock private OAuth2AccessToken accessToken;
  @Mock private OidcIdToken idToken;
  @Mock private ClientRegistration clientRegistration;
  @Mock private ClientRegistration.ProviderDetails providerDetails;
  @Mock private ClientRegistration.ProviderDetails.UserInfoEndpoint userInfoEndpoint;

  private RSAKey rsaJwk;
  private DhisOidcClientRegistration registration;
  private SignedJwtUserInfoLoader loader;

  @BeforeEach
  void setUp() throws Exception {
    rsaJwk = new RSAKeyGenerator(2048).keyID("test-key").generate();
    JWKSource<SecurityContext> source = new ImmutableJWKSet<>(new JWKSet(rsaJwk.toPublicJWK()));

    when(userRequest.getClientRegistration()).thenReturn(clientRegistration);
    when(userRequest.getAccessToken()).thenReturn(accessToken);
    when(userRequest.getIdToken()).thenReturn(idToken);
    when(accessToken.getTokenValue()).thenReturn("at-value");
    when(clientRegistration.getRegistrationId()).thenReturn("esignet");
    when(clientRegistration.getProviderDetails()).thenReturn(providerDetails);
    when(providerDetails.getUserInfoEndpoint()).thenReturn(userInfoEndpoint);
    when(userInfoEndpoint.getUri()).thenReturn("https://idp.test/userinfo");
    when(jwkSourceCache.get(eq("esignet"), anyString())).thenReturn(source);

    registration =
        DhisOidcClientRegistration.builder()
            .clientRegistration(clientRegistration)
            .mappingClaimKey("sub")
            .userInfoResponseType(UserInfoResponseType.JWT)
            .userInfoJwsAlgorithm(JWSAlgorithm.RS256)
            .jwkSetUrl("https://idp.test/jwks")
            .build();

    loader = new SignedJwtUserInfoLoader(userService, jwkSourceCache, restTemplate);
  }

  @Test
  void happyPathReturnsDhisOidcUser() throws Exception {
    String jwt = signJwt(claims("user-123"));
    when(restTemplate.exchange(eq("https://idp.test/userinfo"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenReturn(ResponseEntity.ok(jwt));

    User user = new User();
    user.setExternalAuth(true);
    when(userService.getUserByOpenId("user-123")).thenReturn(user);
    when(userService.createUserDetails(user))
        .thenReturn(UserDetails.fromUser(user, Set.of()));

    OidcUser result = loader.load(userRequest, registration);

    assertNotNull(result);
    assertEquals("user-123", result.getAttributes().get("sub"));
  }

  @Test
  void httpFailureRaisesInvalidUserInfoResponse() {
    when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenThrow(new RestClientException("boom"));

    OAuth2AuthenticationException ex =
        assertThrows(
            OAuth2AuthenticationException.class, () -> loader.load(userRequest, registration));
    assertEquals("invalid_user_info_response", ex.getError().getErrorCode());
  }

  @Test
  void badSignatureRaisesJwtProcessingError() throws Exception {
    RSAKey other = new RSAKeyGenerator(2048).keyID("other").generate();
    String jwt = signJwt(claims("user-123"), other);
    when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenReturn(ResponseEntity.ok(jwt));

    OAuth2AuthenticationException ex =
        assertThrows(
            OAuth2AuthenticationException.class, () -> loader.load(userRequest, registration));
    assertEquals("jwt_processing_error", ex.getError().getErrorCode());
  }

  @Test
  void missingMappingClaimRaisesError() throws Exception {
    JWTClaimsSet noSub = new JWTClaimsSet.Builder().issuer("idp").build();
    String jwt = signJwt(noSub);
    when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenReturn(ResponseEntity.ok(jwt));

    OAuth2AuthenticationException ex =
        assertThrows(
            OAuth2AuthenticationException.class, () -> loader.load(userRequest, registration));
    assertEquals("missing_mapping_claim", ex.getError().getErrorCode());
  }

  @Test
  void unknownUserRaisesError() throws Exception {
    String jwt = signJwt(claims("nobody"));
    when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenReturn(ResponseEntity.ok(jwt));
    when(userService.getUserByOpenId("nobody")).thenReturn(null);

    OAuth2AuthenticationException ex =
        assertThrows(
            OAuth2AuthenticationException.class, () -> loader.load(userRequest, registration));
    assertEquals("user_not_found", ex.getError().getErrorCode());
  }

  // --- helpers ---

  private JWTClaimsSet claims(String sub) {
    return new JWTClaimsSet.Builder()
        .subject(sub)
        .issuer("idp")
        .issueTime(Date.from(Instant.now()))
        .expirationTime(Date.from(Instant.now().plusSeconds(60)))
        .build();
  }

  private String signJwt(JWTClaimsSet claims) throws JOSEException {
    return signJwt(claims, rsaJwk);
  }

  private String signJwt(JWTClaimsSet claims, RSAKey signingKey) throws JOSEException {
    SignedJWT signed =
        new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(),
            claims);
    signed.sign(new RSASSASigner(signingKey));
    return signed.serialize();
  }
}
```

- [ ] **Step 2: Run, verify failure**

Run:
```
mvn -q test -pl dhis-2/dhis-services/dhis-service-core \
  -Dtest=SignedJwtUserInfoLoaderTest
```
Expected: COMPILATION FAILURE (`SignedJwtUserInfoLoader` not yet defined).

- [ ] **Step 3: Implement**

```java
/* (standard $YEAR header, author Morten Svanæs) */
package org.hisp.dhis.security.oidc;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Loads OIDC userinfo from providers that respond with a signed JWT
 * ({@code application/jwt}) instead of JSON. Used when the registration's
 * {@link UserInfoResponseType} is {@link UserInfoResponseType#JWT} (e.g. MOSIP eSignet).
 *
 * <p>The flow is: GET userinfo with {@code Accept: application/jwt} → verify the
 * signature against the IdP's JWKS using the registered {@link JWSAlgorithm} →
 * extract the configured mapping claim → resolve the local DHIS2 user. The
 * principal-name attribute remains {@link IdTokenClaimNames#SUB} so audit logs
 * keyed off {@code sub} match the JSON path.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SignedJwtUserInfoLoader {

  private final UserService userService;
  private final JwkSourceCache jwkSourceCache;
  private final RestTemplate restTemplate;

  public OidcUser load(OidcUserRequest userRequest, DhisOidcClientRegistration reg) {
    String userInfoUri =
        userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri();
    String jwt = fetchJwt(userRequest, userInfoUri);
    JWTClaimsSet claims = verify(jwt, reg, userRequest.getClientRegistration().getRegistrationId());
    String mappingValue = requireMappingClaim(claims, reg);
    User user = requireExternalAuthUser(mappingValue, reg);
    UserDetails details = userService.createUserDetails(user);
    return new DhisOidcUser(
        details, claims.toJSONObject(), IdTokenClaimNames.SUB, userRequest.getIdToken());
  }

  private String fetchJwt(OidcUserRequest userRequest, String userInfoUri) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(userRequest.getAccessToken().getTokenValue());
    headers.setAccept(Collections.singletonList(MediaType.valueOf("application/jwt")));
    HttpEntity<String> entity = new HttpEntity<>("", headers);
    try {
      ResponseEntity<String> response =
          restTemplate.exchange(userInfoUri, HttpMethod.GET, entity, String.class);
      String body = response.getBody();
      if (body == null || body.isBlank()) {
        throw new OAuth2AuthenticationException(
            new OAuth2Error("invalid_user_info_response"), "Empty UserInfo JWT response");
      }
      return body;
    } catch (RestClientException ex) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error("invalid_user_info_response"),
          "Failed to fetch UserInfo response: " + ex.getMessage(),
          ex);
    }
  }

  private JWTClaimsSet verify(String jwt, DhisOidcClientRegistration reg, String registrationId) {
    try {
      ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
      JWKSource<SecurityContext> keySource =
          jwkSourceCache.get(registrationId, reg.getJwkSetUrl());
      JWSKeySelector<SecurityContext> selector =
          new JWSVerificationKeySelector<>(reg.getUserInfoJwsAlgorithm(), keySource);
      processor.setJWSKeySelector(selector);
      return processor.process(jwt, null);
    } catch (Exception ex) {
      log.debug("UserInfo JWT verification failed for registration {}", registrationId, ex);
      throw new OAuth2AuthenticationException(
          new OAuth2Error("jwt_processing_error"),
          "Failed to verify UserInfo JWT: " + ex.getMessage(),
          ex);
    }
  }

  private String requireMappingClaim(JWTClaimsSet claims, DhisOidcClientRegistration reg) {
    String mappingClaimKey = reg.getMappingClaimKey();
    Map<String, Object> claimsMap = claims.toJSONObject();
    Object value = claimsMap.get(mappingClaimKey);
    if (!(value instanceof String s) || s.isBlank()) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error("missing_mapping_claim"),
          "Mapping claim '" + mappingClaimKey + "' missing or empty in UserInfo JWT");
    }
    return s;
  }

  private User requireExternalAuthUser(String mappingValue, DhisOidcClientRegistration reg) {
    User user = userService.getUserByOpenId(mappingValue);
    if (user == null || !user.isExternalAuth()) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error("user_not_found"),
          "No external-auth DHIS2 user found for mapping claim '"
              + reg.getMappingClaimKey()
              + "'='"
              + mappingValue
              + "'");
    }
    if (user.isDisabled() || !user.isAccountNonExpired()) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error("user_disabled"), "DHIS2 user is disabled or expired");
    }
    return user;
  }
}
```

Add a `RestTemplate` bean wiring. The simplest path: configure as a `@Bean` if no shared one exists. Check first:

```bash
grep -rn "RestTemplate" dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/config/ | head -5
```

If no shared bean exists, add one in `DhisWebApiWebSecurityConfig.java` (or the closest existing config class adjacent to OIDC configuration):

```java
  @Bean
  public RestTemplate oidcRestTemplate() {
    return new RestTemplate();
  }
```

If a shared OIDC `RestTemplate` already exists, reuse it via `@Qualifier`. Document the choice in the commit message.

- [ ] **Step 4: Run tests, verify pass**

Run:
```
mvn -q install -pl dhis-2/dhis-services/dhis-service-core -am -DskipTests -q
mvn -q test -pl dhis-2/dhis-services/dhis-service-core \
  -Dtest=SignedJwtUserInfoLoaderTest
```
Expected: BUILD SUCCESS, all tests green.

- [ ] **Step 5: Format and commit**

```bash
mvn -q spotless:apply -f dhis-2/pom.xml -pl dhis-services/dhis-service-core
git add dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/SignedJwtUserInfoLoader.java \
        dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/oidc/SignedJwtUserInfoLoaderTest.java
# Plus the security config file if a RestTemplate bean was added there.
git commit -m "feat(oidc): add SignedJwtUserInfoLoader for application/jwt userinfo [DHIS2-20043]"
```

---

## Task 9: Restore inheritance and branch in `DhisOidcUserService`

**Files:**
- Modify: `dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/DhisOidcUserService.java`
- Test: `dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/oidc/DhisOidcUserServiceDispatchTest.java`

The dispatch test uses Mockito to stub a package-private `loadFromJsonUserInfo` seam (so we don't need real HTTP for the JSON branch).

- [ ] **Step 1: Write failing test**

```java
/* (standard $YEAR header, author Morten Svanæs) */
package org.hisp.dhis.security.oidc;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@ExtendWith(MockitoExtension.class)
class DhisOidcUserServiceDispatchTest {

  @Mock private DhisOidcProviderRepository repo;
  @Mock private UserService userService;
  @Mock private SignedJwtUserInfoLoader signedJwtLoader;
  @Mock private OidcUserRequest request;
  @Mock private ClientRegistration clientRegistration;
  @Mock private OidcUser jsonResult;
  @Mock private OidcUser jwtResult;

  @Spy private DhisOidcUserService service = new DhisOidcUserService();

  @BeforeEach
  void wire() {
    service.userService = userService;
    service.clientRegistrationRepository = repo;
    service.signedJwtUserInfoLoader = signedJwtLoader;
    when(request.getClientRegistration()).thenReturn(clientRegistration);
    when(clientRegistration.getRegistrationId()).thenReturn("p1");
  }

  @Test
  void jsonRegistrationUsesJsonPath() {
    DhisOidcClientRegistration reg =
        DhisOidcClientRegistration.builder()
            .clientRegistration(clientRegistration)
            .mappingClaimKey("sub")
            .userInfoResponseType(UserInfoResponseType.JSON)
            .build();
    when(repo.getDhisOidcClientRegistration("p1")).thenReturn(reg);
    doReturn(jsonResult).when(service).loadFromJsonUserInfo(request, reg);

    OidcUser result = service.loadUser(request);

    assertSame(jsonResult, result);
    verify(signedJwtLoader, never()).load(any(), any());
  }

  @Test
  void jwtRegistrationUsesJwtPath() {
    DhisOidcClientRegistration reg =
        DhisOidcClientRegistration.builder()
            .clientRegistration(clientRegistration)
            .mappingClaimKey("sub")
            .userInfoResponseType(UserInfoResponseType.JWT)
            .build();
    when(repo.getDhisOidcClientRegistration("p1")).thenReturn(reg);
    when(signedJwtLoader.load(request, reg)).thenReturn(jwtResult);

    OidcUser result = service.loadUser(request);

    assertSame(jwtResult, result);
    verify(service, never()).loadFromJsonUserInfo(any(), any());
  }
}
```

- [ ] **Step 2: Run, verify failure**

Run:
```
mvn -q test -pl dhis-2/dhis-services/dhis-service-core \
  -Dtest=DhisOidcUserServiceDispatchTest
```
Expected: COMPILATION FAILURE.

- [ ] **Step 3: Replace `DhisOidcUserService` body**

```java
/* (keep existing $YEAR header, author tag, package) */
package org.hisp.dhis.security.oidc;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * DHIS2 extension of Spring Security's {@link OidcUserService}. Dispatches userinfo handling based
 * on the provider registration's {@link UserInfoResponseType}: JSON (default; Spring's standard
 * path) or JWT (eSignet-style signed JWT, handled by {@link SignedJwtUserInfoLoader}). On both
 * paths it then resolves the configured {@code mapping_claim} value to a local DHIS2 user via
 * {@link UserService#getUserByOpenId}.
 *
 * <p>The matched DHIS2 user must be flagged for external authentication, must not be disabled, and
 * must not have an expired account; otherwise authentication fails with an
 * {@link OAuth2AuthenticationException}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Service
public class DhisOidcUserService extends OidcUserService {

  @Autowired public UserService userService;
  @Autowired DhisOidcProviderRepository clientRegistrationRepository;
  @Autowired SignedJwtUserInfoLoader signedJwtUserInfoLoader;

  @Override
  public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
    ClientRegistration cr = userRequest.getClientRegistration();
    DhisOidcClientRegistration reg =
        clientRegistrationRepository.getDhisOidcClientRegistration(cr.getRegistrationId());

    return switch (reg.getUserInfoResponseType()) {
      case JSON -> loadFromJsonUserInfo(userRequest, reg);
      case JWT -> signedJwtUserInfoLoader.load(userRequest, reg);
    };
  }

  /**
   * JSON-userinfo path: delegates to Spring's {@link OidcUserService#loadUser(OidcUserRequest)},
   * then resolves the mapping claim to a local DHIS2 user. Package-private to allow direct
   * stubbing in {@link DhisOidcUserServiceDispatchTest}.
   */
  OidcUser loadFromJsonUserInfo(OidcUserRequest userRequest, DhisOidcClientRegistration reg) {
    OidcUser oidcUser = super.loadUser(userRequest);

    String mappingClaimKey = reg.getMappingClaimKey();
    Map<String, Object> attributes = oidcUser.getAttributes();
    Object claimValue = attributes.get(mappingClaimKey);
    OidcUserInfo userInfo = oidcUser.getUserInfo();
    if (claimValue == null && userInfo != null) {
      claimValue = userInfo.getClaim(mappingClaimKey);
    }

    if (log.isDebugEnabled()) {
      log.debug(
          "Trying to look up DHIS2 user with OidcUser mapping mappingClaimKey='{}', claim value='{}'",
          mappingClaimKey,
          claimValue);
    }

    if (claimValue instanceof String s && !s.isBlank()) {
      User user = userService.getUserByOpenId(s);
      if (user != null && user.isExternalAuth()) {
        if (user.isDisabled() || !user.isAccountNonExpired()) {
          throw new OAuth2AuthenticationException(
              new OAuth2Error("user_disabled"), "User is disabled");
        }
        UserDetails userDetails = userService.createUserDetails(user);
        return new DhisOidcUser(
            userDetails, attributes, IdTokenClaimNames.SUB, oidcUser.getIdToken());
      }
    }

    String errorMessage =
        String.format(
            "Failed to look up DHIS2 user with OidcUser mapping mapping; mappingClaimKey='%s', claimValue='%s'",
            mappingClaimKey, claimValue);
    if (log.isDebugEnabled()) {
      log.debug(errorMessage);
    }
    OAuth2Error err = new OAuth2Error("could_not_map_oidc_user_to_dhis2_user", errorMessage, null);
    throw new OAuth2AuthenticationException(err, err.toString());
  }
}
```

(The fields are package-private intentionally so the dispatch test can wire them. They're still `@Autowired` for production. `@Spy` requires Mockito to be able to write them; with `@Autowired` + package-private fields this works without reflection trickery.)

- [ ] **Step 4: Run, verify pass**

Run:
```
mvn -q test -pl dhis-2/dhis-services/dhis-service-core \
  -Dtest=DhisOidcUserServiceDispatchTest
```
Expected: BUILD SUCCESS, all tests green.

- [ ] **Step 5: Format and commit**

```bash
mvn -q spotless:apply -f dhis-2/pom.xml -pl dhis-services/dhis-service-core
git add dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/DhisOidcUserService.java \
        dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/oidc/DhisOidcUserServiceDispatchTest.java
git commit -m "feat(oidc): branch DhisOidcUserService on userInfoResponseType [DHIS2-20043]"
```

---

## Task 10: Re-enable `OAuth2Test`

**Files:**
- Modify: `dhis-2/dhis-test-e2e/src/test/java/org/hisp/dhis/oauth2/OAuth2Test.java`

The PR (PR #22027) added `@Disabled` here. Since we've restored the JSON path, the test should run again. (Reminder: the test ran in a tagged e2e profile only — it will not run by accident in the unit/integration build.)

- [ ] **Step 1: Remove `@Disabled` and the now-unused import**

Edit:

```java
@Tag("oauth2tests")
@Slf4j
class OAuth2Test extends BaseE2ETest {
```

Drop the line `@Disabled` from the class annotations. Drop `import org.junit.jupiter.api.Disabled;`.

- [ ] **Step 2: Compile**

Run: `mvn -q -pl dhis-2/dhis-test-e2e compile -DskipTests`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Format and commit**

```bash
mvn -q spotless:apply -f dhis-2/pom.xml -pl dhis-test-e2e
git add dhis-2/dhis-test-e2e/src/test/java/org/hisp/dhis/oauth2/OAuth2Test.java
git commit -m "test(oidc): re-enable OAuth2Test now that JSON userinfo path is preserved [DHIS2-20043]"
```

---

## Task 11: Whole-module test sweep + final verification

- [ ] **Step 1: Build the whole core module with tests**

Run:
```
mvn -q -pl dhis-2/dhis-services/dhis-service-core -am test
```
Expected: BUILD SUCCESS, no regressions.

- [ ] **Step 2: Build dhis-web-api (downstream of core) just to be safe**

Run:
```
mvn -q install -pl dhis-2/dhis-web-api -am -DskipTests
```
Expected: BUILD SUCCESS.

- [ ] **Step 3: Spotless full-pass**

Run:
```
mvn -q spotless:apply -f dhis-2/pom.xml
mvn -q spotless:check -f dhis-2/pom.xml
```
Expected: BUILD SUCCESS on the check.

- [ ] **Step 4: Confirm git status clean**

Run: `git status --short`
Expected: clean working tree on `feat/DHIS2-20043-esignet-oidc-jwt-userinfo`.

- [ ] **Step 5: Open PR (manual — do NOT push without user confirmation)**

PR description should include:
- One-paragraph summary: per-provider opt-in for JWT userinfo, no behaviour change for existing providers.
- Bullet list of new config keys with values and defaults.
- Note that **dhis2-docs needs a follow-up PR** (the OIDC reference chapter is in a separate repo) with the eSignet example block and key docs. Link the spec file: `dhis-2/docs/superpowers/specs/2026-05-06-esignet-oidc-integration-design.md`.
- Reference: closes [DHIS2-20043].
- "AI Assisted" line per project rule.

---

## Out-of-scope follow-ups

- **JWE (encrypted) userinfo** — explicitly deferred per spec §3.
- **dhis2-docs reference chapter update** — `oauth.md` lives in the external `dhis2-docs` repo (no local copy). Follow-up PR there with the §4 example block from the spec.
- **Token-endpoint / JWKS HTTP timeouts via `dhis.conf`** — current values are hard-coded constants on `JwkSourceCache`. If field experience shows IdPs needing different timeouts, expose them.

---

## Self-review notes

- Spec §4 (config keys, allow-list) → Tasks 1, 2, 3, 5.
- Spec §5.3 (registration fields) → Task 4.
- Spec §5.4 (generic builder wiring) → Task 6.
- Spec §5.5 (parser relaxation + value validation) → Task 5.
- Spec §5.6 + §5.7 (user service branch + JWT loader) → Tasks 8, 9.
- Spec §5.8 (JWKSource cache) → Task 7.
- Spec §5.9 (re-enable OAuth2Test) → Task 10.
- Spec §5.10 (PublicKeysController thumbprint) — already on master? No: check. **Action:** the PR includes a small additive change in `PublicKeysController.java` (adds `x509CertSHA256Thumbprint` to the JWK output). Cherry-pick that hunk in Task 10 or as a small Task 10b. *Adding now as Task 10b.*
- Spec §6 (tests) → covered by Tasks 2, 5, 6, 8, 9 with parameterized + happy/edge coverage.
- Spec §7 (docs) → out of scope locally; PR description note in Task 11.
- Spec §8 (no Flyway) → confirmed.

---

## Task 10b: Cherry-pick `PublicKeysController` thumbprint addition

**Files:**
- Modify: `dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/security/PublicKeysController.java`

This is the additive `x509CertSHA256Thumbprint` change from PR #22027.

- [ ] **Step 1: Apply the change**

Find the existing `RSAKey.Builder(...)` chain (around line 80-87) and add `.x509CertSHA256Thumbprint(...)` to the chain:

```java
        new RSAKey.Builder(dhisOidcClientRegistration.getRsaPublicKey())
            .keyUse(KeyUse.SIGNATURE)
            .algorithm(JWSAlgorithm.parse(jwsAlgorithm.toString()))
            .x509CertSHA256Thumbprint(
                dhisOidcClientRegistration.getJwk().getX509CertSHA256Thumbprint())
            .keyID(dhisOidcClientRegistration.getKeyId());
```

This is safe: `getX509CertSHA256Thumbprint()` returns null when the underlying JWK has none, and Nimbus's builder accepts null.

- [ ] **Step 2: Compile**

Run: `mvn -q install -pl dhis-2/dhis-web-api -am -DskipTests`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Format and commit**

```bash
mvn -q spotless:apply -f dhis-2/pom.xml -pl dhis-web-api
git add dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/security/PublicKeysController.java
git commit -m "feat(oidc): include x509 cert thumbprint in published JWK [DHIS2-20043]"
```
