/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.webapi.security;

import static org.hisp.dhis.web.WebClient.JwtTokenHeader;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.util.List;
import java.util.Properties;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.security.jwt.Dhis2JwtAuthenticationManagerResolver;
import org.hisp.dhis.security.oidc.DhisOidcClientRegistration;
import org.hisp.dhis.security.oidc.DhisOidcProviderRepository;
import org.hisp.dhis.security.oidc.GenericOidcProviderConfigParser;
import org.hisp.dhis.security.oidc.provider.GoogleProvider;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerWithJwtTokenAuthTest;
import org.hisp.dhis.webapi.json.domain.JsonError;
import org.hisp.dhis.webapi.json.domain.JsonUser;
import org.hisp.dhis.webapi.security.config.DhisWebApiWebSecurityConfig;
import org.hisp.dhis.webapi.security.utils.JoseHeader;
import org.hisp.dhis.webapi.security.utils.JoseHeaderNames;
import org.hisp.dhis.webapi.security.utils.JwtClaimsSet;
import org.hisp.dhis.webapi.security.utils.JwtUtils;
import org.hisp.dhis.webapi.security.utils.TestJoseHeaders;
import org.hisp.dhis.webapi.security.utils.TestJwks;
import org.hisp.dhis.webapi.security.utils.TestJwtClaimsSets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class JwtBearerTokenTest extends DhisControllerWithJwtTokenAuthTest {
  // @formatter:off
  public static final String EXPIRED_GOOGLE_JWT_TOKEN =
      "eyJhbGciOiJSUzI1NiIsImtpZCI6ImU4NzMyZGIwNjI4NzUxNTU1NjIx"
          + "M2I4MGFjYmNmZDA4Y2ZiMzAyYTkiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJhenAiO"
          + "iIxMDE5NDE3MDAyNTQ0LW1xYTdmbGs0bWpvaHJnc2JnOWJ0YTlidmx1b2o4NW8wLmFwcHMuZ29vZ2xldXNlcmNvbnRlbnQuY29tIiwiY"
          + "XVkIjoiMTAxOTQxNzAwMjU0NC1tcWE3ZmxrNG1qb2hyZ3NiZzlidGE5YnZsdW9qODVvMC5hcHBzLmdvb2dsZXVzZXJjb250ZW50LmNvb"
          + "SIsInN1YiI6IjExMDk3ODA1MDEyNzk2NzA2NTUwNiIsImVtYWlsIjoiZGhpczJvaWRjdXNlckBnbWFpbC5jb20iLCJlbWFpbF92ZXJpZ"
          + "mllZCI6dHJ1ZSwiYXRfaGFzaCI6IkhXbTNXcXphM2p5TEFUZjNlU1pBNVEiLCJuYW1lIjoiZGhpczJvaWRjdXNlciBUZXN0ZXIiLCJwa"
          + "WN0dXJlIjoiaHR0cHM6Ly9saDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tLy1oRmptUnhOQkJTWS9BQUFBQUFBQUFBSS9BQUFBQUFBQUFBQ"
          + "S9BTVp1dWNuQkdYVTF5X05fV25qSXJndHBpSXFWMl9ndll3L3M5Ni1jL3Bob3RvLmpwZyIsImdpdmVuX25hbWUiOiJkaGlzMm9pZGN1c"
          + "2VyIiwiZmFtaWx5X25hbWUiOiJUZXN0ZXIiLCJsb2NhbGUiOiJlbiIsImlhdCI6MTYxNDk1NzU5MCwiZXhwIjoxNjE0OTYxMTkwfQ.OC"
          + "m7hj4H-UqRpM_Xrfq58U3ZGI3k7-S3c4AslVAaMxKsNitsPDZ7oxs-FJT-E7uDqnp1wW5LyBLj8jfJZ4JnvuiNGZrvCCpR3m70_4mSgP"
          + "8VTjFFEijgfW1IIy_BWI8gDY6iCK7qgOATdYnCyJteWBMKRPr5wVSN05TT3xxLzsE7C5ViOzHAm2v6XrrsEhfcjNmwKmlljjpImTwtUS"
          + "TBS3DWoWsHaNqXfE3rO0M7231FWl2X0vk5oO-KycNoS1vDZLAvdf6QRJVnPMkQ6Cx5XSMSYEmUmFqM3Sj2ip0Q48hAe4ydzIgRWdGbzG"
          + "nMH3euqGWr4_G_EBvVqfVPnBF0YA";
  // @formatter:on

  public static final String GOOGLE_CLIENT_ID =
      "1019417002544-mqa7flk4mjohrgsbg9bta9bvluoj85o0.apps.googleusercontent.com";

  public static final String TEST_PROVIDER_ONE_URI = "testproviderone.com";

  public static final String TEST_PROVIDER_ONE_NAME = "testproviderone";

  public static final String CLIENT_ID_1 = "client-1";

  public static final String DEFAULT_EMAIL = "admin@dhis2.org";

  public static final String DEFAULT_MAPPING_CLAIM = "email";

  private static final RSAKey RSA_KEY = TestJwks.DEFAULT_RSA_JWK;

  @Autowired private DhisOidcProviderRepository dhisOidcProviderRepository;

  @Autowired private Dhis2JwtAuthenticationManagerResolver dhis2JwtAuthenticationManagerResolver;

  private static JwtUtils jwsEncoder;

  private static NimbusJwtDecoder jwtDecoder;

  @BeforeAll
  static void setUpClass() throws JOSEException {
    DhisWebApiWebSecurityConfig.setApiContextPath("");
    JWKSource<SecurityContext> jwkSource =
        (jwkSelector, securityContext) -> jwkSelector.select(new JWKSet(List.of(RSA_KEY)));
    jwsEncoder = new JwtUtils(jwkSource);
    jwtDecoder = NimbusJwtDecoder.withPublicKey(RSA_KEY.toRSAPublicKey()).build();
  }

  @BeforeEach
  void setUp() {
    dhis2JwtAuthenticationManagerResolver.setJwtDecoder(jwtDecoder);
    dhisOidcProviderRepository.clear();
  }

  private Jwt createJwt(String provider, String clientId, String mappingKey, String mappingValue) {
    JoseHeader joseHeader = TestJoseHeaders.joseHeader(provider).build();
    JwtClaimsSet jwtClaimsSet =
        TestJwtClaimsSets.jwtClaimsSet(provider, clientId, mappingKey, mappingValue).build();
    return jwsEncoder.encode(joseHeader, jwtClaimsSet);
  }

  @Test
  void testJwkEncodeEndDecode() throws JOSEException {
    Jwt encodedJws =
        createJwt(TEST_PROVIDER_ONE_URI, CLIENT_ID_1, DEFAULT_MAPPING_CLAIM, DEFAULT_EMAIL);
    assertEquals("JWT", encodedJws.getHeaders().get(JoseHeaderNames.TYP));
    assertEquals(RSA_KEY.getKeyID(), encodedJws.getHeaders().get(JoseHeaderNames.KID));
    assertNotNull(encodedJws.getId());
    String tokenValue = encodedJws.getTokenValue();
    jwtDecoder.decode(tokenValue);
  }

  @Test
  void testSuccessfulRequest() {
    setupTestingProvider(CLIENT_ID_1, TEST_PROVIDER_ONE_NAME, TEST_PROVIDER_ONE_URI);
    User openIDUser = createOpenIDUser("openiduser", "openiduser@oidc.org");
    String tokenValue =
        createJwt(TEST_PROVIDER_ONE_URI, CLIENT_ID_1, "email", "openiduser@oidc.org")
            .getTokenValue();
    JsonUser user =
        GET("/me?fields=settings,id", JwtTokenHeader(tokenValue)).content().as(JsonUser.class);
    assertEquals(openIDUser.getUid(), user.getId());
  }

  @Test
  void testMalformedToken() {
    assertInvalidTokenError(
        "Invalid JWT serialization: Missing dot delimiter(s)",
        GET("/me", JwtTokenHeader("NOT_A_JWT_TOKEN")));
  }

  @Test
  void testExpiredToken() {
    dhis2JwtAuthenticationManagerResolver.setJwtDecoder(null);
    setupGoogleProvider(GOOGLE_CLIENT_ID);
    assertInvalidTokenError(
        "An error occurred while attempting to decode the Jwt: Signed JWT rejected: Another algorithm expected, or no matching key(s) found",
        GET("/me", JwtTokenHeader(EXPIRED_GOOGLE_JWT_TOKEN)));
  }

  @Test
  void testMissingUser() {
    setupTestingProvider(CLIENT_ID_1, TEST_PROVIDER_ONE_NAME, TEST_PROVIDER_ONE_URI);
    String tokenValue =
        createJwt(TEST_PROVIDER_ONE_URI, CLIENT_ID_1, DEFAULT_MAPPING_CLAIM, DEFAULT_EMAIL)
            .getTokenValue();
    assertInvalidTokenError(
        "Found no matching DHIS2 user for the mapping claim:'email' with the value:'admin@dhis2.org'",
        GET("/me", JwtTokenHeader(tokenValue)));
  }

  @Test
  void testNoClientMatch() {
    String providerURI = "testprovidertwo.com";
    setupTestingProvider("client-2", "testprovidertwo", providerURI);
    String tokenValue =
        createJwt(providerURI, CLIENT_ID_1, DEFAULT_MAPPING_CLAIM, DEFAULT_EMAIL).getTokenValue();
    assertInvalidTokenError("Invalid audience", GET("/me", JwtTokenHeader(tokenValue)));
  }

  private void assertInvalidTokenError(String expected, HttpResponse response) {
    JsonError error = response.error(HttpStatus.UNAUTHORIZED);
    assertEquals("invalid_token", error.getMessage());
    assertEquals(expected, error.getDevMessage());
  }

  private void setupGoogleProvider(String clientId) {
    Properties config = new Properties();
    config.put(ConfigurationKey.OIDC_PROVIDER_GOOGLE_CLIENT_ID.getKey(), clientId);
    config.put(ConfigurationKey.OIDC_PROVIDER_GOOGLE_CLIENT_SECRET.getKey(), "secret");
    DhisOidcClientRegistration parse = GoogleProvider.parse(config);
    dhisOidcProviderRepository.addRegistration(parse);
  }

  private void setupTestingProvider(
      String clientId, String providerName, final String providerURI) {
    Properties config = new Properties();
    config.put("oidc.provider." + providerName + ".client_id", clientId);
    config.put("oidc.provider." + providerName + ".client_secret", "secret");
    config.put("oidc.provider." + providerName + ".issuer_uri", "https://" + providerURI);
    config.put(
        "oidc.provider." + providerName + ".authorization_uri",
        "https://" + providerURI + "/authorize");
    config.put("oidc.provider." + providerName + ".token_uri", "https://" + providerURI + "/token");
    config.put(
        "oidc.provider." + providerName + ".user_info_uri", "https://" + providerURI + "/userinfo");
    config.put("oidc.provider." + providerName + ".jwk_uri", "https://" + providerURI + "/jwk");
    GenericOidcProviderConfigParser.parse(config)
        .forEach(dhisOidcProviderRepository::addRegistration);
  }
}
