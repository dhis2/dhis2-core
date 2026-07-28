/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.controller.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.security.oauth2.authorization.Dhis2OAuth2AuthorizationService;
import org.hisp.dhis.security.oauth2.client.Dhis2OAuth2ClientService;
import org.hisp.dhis.test.webapi.ControllerWithJwtTokenAuthTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Locks in the PKCE-by-default guarantee at {@code /oauth2/token}: since Spring Authorization
 * Server 7's {@code ClientSettings.builder()} itself now defaults {@code requireProofKey(true)}
 * (the DHIS2-side {@code requireProofKey(false)} bridge in {@code Dhis2OAuth2ClientServiceImpl} is
 * gone), any {@link RegisteredClient} built without an explicit {@code clientSettings(...)}
 * override REQUIRES S256 PKCE on the authorization_code grant.
 *
 * <p>Findings from decompiling {@code spring-security-oauth2-authorization-server-7.0.6.jar} and
 * {@code spring-security-oauth2-core-7.0.6.jar} (neither module has an attached sources jar in this
 * local repository; verified with {@code javap -p -c -s} against {@code CodeVerifierAuthenticator},
 * {@code ClientSecretAuthenticationProvider}, {@code PublicClientAuthenticationProvider} and {@code
 * ClientSettings}):
 *
 * <ul>
 *   <li>{@code ClientSettings.builder()} itself calls {@code requireProofKey(true)} (and {@code
 *       requireAuthorizationConsent(false)}) before returning, and {@code
 *       RegisteredClient.Builder.build()} substitutes {@code ClientSettings.builder().build()}
 *       whenever {@code clientSettings(...)} was never called. So simply omitting the {@code
 *       clientSettings(...)} call - as this test does - is enough to require PKCE; no explicit
 *       {@code requireProofKey(true)} needs to be set.
 *   <li>{@code CodeVerifierAuthenticator.authenticate(...)} reads {@code code_challenge} from the
 *       <em>stored</em> {@link OAuth2AuthorizationRequest}'s additional parameters (looked up via
 *       {@code authorizationService.findByToken(code, "code")}) and {@code code_verifier} from the
 *       incoming token request. If {@code code_challenge} is blank AND the client's {@code
 *       requireProofKey} is {@code true}, it throws {@code invalid_grant} UNCONDITIONALLY - even if
 *       no {@code code_verifier} was sent either. In other words: SAS 7 enforces {@code
 *       requireProofKey} at the {@code /oauth2/token} authorization_code exchange itself (not only
 *       by refusing to hand out a code at {@code /oauth2/authorize}), so a code stored without a
 *       challenge for a PKCE-required client can never be redeemed.
 *   <li>When {@code code_challenge} is present, {@code codeVerifierValid(...)} recomputes {@code
 *       Base64Url-no-padding(SHA-256(code_verifier))} for method {@code S256} and compares it
 *       against the stored challenge; a missing or mismatching {@code code_verifier} throws {@code
 *       invalid_grant} the same way. Non-{@code S256} methods always fail (no {@code plain}
 *       support).
 *   <li>{@code ClientSecretAuthenticationProvider} (confidential clients, e.g. {@code
 *       client_secret_basic} - what this test uses) calls {@code
 *       codeVerifierAuthenticator.authenticateIfAvailable(...)}, while {@code
 *       PublicClientAuthenticationProvider} (public, secret-less clients) calls {@code
 *       authenticateRequired(...)}. Both wrappers delegate to the same private {@code
 *       authenticate(...)} method, which throws internally whenever {@code requireProofKey} is
 *       {@code true} and the challenge/verifier pair does not check out - so a confidential client
 *       with {@code requireProofKey(true)} is enforced just as strictly as a public one. The {@code
 *       Required}/{@code IfAvailable} distinction only matters for the silent-skip case (blank
 *       challenge AND {@code requireProofKey=false}), which none of these tests exercise.
 * </ul>
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@ActiveProfiles("oauth2-authorization-server-test")
class OAuth2PkceEnforcementTest extends ControllerWithJwtTokenAuthTestBase {

  @Autowired private Dhis2OAuth2ClientService oAuth2ClientService;
  @Autowired private Dhis2OAuth2AuthorizationService authorizationService;
  @Autowired private AuthorizationServerSettings authorizationServerSettings;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  @DisplayName(
      "PKCE-required client: token exchange without a code_verifier fails with invalid_grant")
  void tokenExchangeWithoutVerifierFailsForPkceRequiredClient() throws Exception {
    // Given: a client with NO explicit clientSettings(...) - so requireProofKey defaults to
    // true - and a stored authorization whose authorization request carries a code_challenge.
    String codeVerifier = generateCodeVerifier();
    String codeChallenge = generateCodeChallenge(codeVerifier);
    Fixture fixture = registerClientAndAuthorization(codeChallenge);

    // When: the client exchanges the code without sending a code_verifier at all.
    // Then: CodeVerifierAuthenticator.codeVerifierValid(...) sees a blank verifier and fails.
    mvc.perform(tokenRequest(fixture, null))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("invalid_grant"));
  }

  @Test
  @DisplayName(
      "PKCE-required client: token exchange with a wrong code_verifier fails with invalid_grant")
  void tokenExchangeWithWrongVerifierFails() throws Exception {
    // Given: same PKCE-required client/authorization setup as above.
    String codeVerifier = generateCodeVerifier();
    String codeChallenge = generateCodeChallenge(codeVerifier);
    Fixture fixture = registerClientAndAuthorization(codeChallenge);

    // When: the client sends a code_verifier that does not hash to the stored code_challenge.
    // Then: the SHA-256(verifier) vs. code_challenge comparison fails.
    mvc.perform(tokenRequest(fixture, "wrong-" + codeVerifier))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("invalid_grant"));
  }

  @Test
  @DisplayName(
      "PKCE-required client: token exchange for a code stored without a code_challenge fails"
          + " with invalid_grant, even without a code_verifier")
  void authorizationWithoutChallengeFailsForPkceRequiredClient() throws Exception {
    // Given: the stored authorization request carries NO code_challenge/code_challenge_method at
    // all (as if /oauth2/authorize had been reached by a legacy caller that never sent one).
    Fixture fixture = registerClientAndAuthorization(null);

    // When: the client exchanges the code without a code_verifier.
    // Then: per the decompiled CodeVerifierAuthenticator (see class javadoc), a blank
    // code_challenge with requireProofKey=true throws invalid_grant unconditionally - SAS 7
    // enforces requireProofKey at /oauth2/token itself for the authorization_code grant, not
    // only by withholding the code at /oauth2/authorize.
    mvc.perform(tokenRequest(fixture, null))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("invalid_grant"));
  }

  /** Fixture data for a single token-exchange attempt. */
  private record Fixture(String clientId, String clientSecret, String redirectUri, String code) {}

  /**
   * Registers a confidential authorization_code client with NO explicit {@code clientSettings(...)}
   * (so {@code requireProofKey} defaults to {@code true}), and stores an authorization for it
   * carrying an authorization code and the given {@code codeChallenge} (or no challenge at all when
   * {@code codeChallenge} is {@code null}).
   */
  private Fixture registerClientAndAuthorization(String codeChallenge) {
    String suffix = CodeGenerator.generateUid();
    String clientId = "pkce-test-client-" + suffix;
    String clientSecret = "pkce-test-secret";
    String redirectUri = "https://app.example.org/callback";
    RegisteredClient client =
        RegisteredClient.withId(CodeGenerator.generateUid())
            .clientId(clientId)
            .clientSecret(passwordEncoder.encode(clientSecret))
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri(redirectUri)
            .scope("openid")
            // No .clientSettings(...) call: SAS 7's ClientSettings.builder() default is
            // requireProofKey(true), so this client REQUIRES PKCE.
            .build();
    oAuth2ClientService.save(client);

    Instant now = Instant.now();
    String codeValue = "pkce-code-" + suffix;
    OAuth2AuthorizationCode authorizationCode =
        new OAuth2AuthorizationCode(codeValue, now, now.plus(5, ChronoUnit.MINUTES));

    OAuth2AuthorizationRequest.Builder authorizationRequestBuilder =
        OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri(authorizationServerSettings.getIssuer() + "oauth2/authorize")
            .clientId(clientId)
            .redirectUri(redirectUri)
            .scopes(Set.of("openid"))
            .state("state-" + suffix);
    if (codeChallenge != null) {
      authorizationRequestBuilder.additionalParameters(
          Map.of(
              PkceParameterNames.CODE_CHALLENGE,
              codeChallenge,
              PkceParameterNames.CODE_CHALLENGE_METHOD,
              "S256"));
    }
    OAuth2AuthorizationRequest authorizationRequest = authorizationRequestBuilder.build();

    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(client)
            .id(CodeGenerator.generateUid())
            .principalName("pkce-test-user")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizedScopes(Set.of("openid"))
            .token(authorizationCode)
            .attribute(OAuth2AuthorizationRequest.class.getName(), authorizationRequest)
            .build();
    authorizationService.save(authorization);

    return new Fixture(clientId, clientSecret, redirectUri, codeValue);
  }

  /**
   * Builds the {@code /oauth2/token} POST for the given fixture. {@code codeVerifier} is omitted
   * from the request entirely when {@code null} (as opposed to sent empty).
   */
  private MockHttpServletRequestBuilder tokenRequest(Fixture fixture, String codeVerifier) {
    String basicAuth =
        Base64.getEncoder()
            .encodeToString(
                (fixture.clientId() + ":" + fixture.clientSecret())
                    .getBytes(StandardCharsets.UTF_8));
    MockHttpServletRequestBuilder request =
        post("/oauth2/token")
            .header(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("grant_type", "authorization_code")
            .param("code", fixture.code())
            .param("redirect_uri", fixture.redirectUri());
    if (codeVerifier != null) {
      request.param(PkceParameterNames.CODE_VERIFIER, codeVerifier);
    }
    return request;
  }

  /** Generates a fresh S256 PKCE code_verifier: Base64URL-no-padding of 32 random bytes. */
  private static String generateCodeVerifier() {
    byte[] randomBytes = new byte[32];
    new SecureRandom().nextBytes(randomBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
  }

  /** Derives the S256 code_challenge: Base64URL-no-padding of SHA-256(verifier ASCII bytes). */
  private static String generateCodeChallenge(String codeVerifier) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 must be available on any supported JVM", e);
    }
  }
}
