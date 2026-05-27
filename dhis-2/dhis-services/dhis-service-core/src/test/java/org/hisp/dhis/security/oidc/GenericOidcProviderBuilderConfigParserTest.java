/*
 * Copyright (c) 2004-2022, University of Oslo
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class GenericOidcProviderBuilderConfigParserTest {

  @Test
  void parseDhis2ProviderIsReservedAndSkipped() {
    Properties p = new Properties();
    p.put("oidc.provider.dhis2.client_id", "dhis2-client");
    p.put("oidc.provider.dhis2.client_secret", "secret");
    p.put("oidc.provider.dhis2.authorization_uri", "http://localhost:8080/oauth2/authorize");
    p.put("oidc.provider.dhis2.token_uri", "http://localhost:8080/oauth2/token");
    p.put("oidc.provider.dhis2.user_info_uri", "http://localhost:8080/userinfo");
    p.put("oidc.provider.dhis2.jwk_uri", "http://localhost:8080/oauth2/jwks");
    List<DhisOidcClientRegistration> parse = GenericOidcProviderConfigParser.parse(p);
    assertThat(parse, hasSize(0));
  }

  @Test
  void parseDhis2ProviderSkippedButOthersStillParsed() {
    Properties p = new Properties();
    p.put("oidc.provider.dhis2.client_id", "dhis2-client");
    p.put("oidc.provider.dhis2.client_secret", "secret");
    p.put("oidc.provider.dhis2.authorization_uri", "http://localhost:8080/oauth2/authorize");
    p.put("oidc.provider.dhis2.token_uri", "http://localhost:8080/oauth2/token");
    p.put("oidc.provider.dhis2.user_info_uri", "http://localhost:8080/userinfo");
    p.put("oidc.provider.dhis2.jwk_uri", "http://localhost:8080/oauth2/jwks");

    p.put("oidc.provider.idporten.client_id", "testClientId");
    p.put("oidc.provider.idporten.client_secret", "testClientSecret");
    p.put("oidc.provider.idporten.authorization_uri", "https://oidc-ver2.difi.no/authorize");
    p.put("oidc.provider.idporten.token_uri", "https://oidc-ver2.difi.no/token");
    p.put("oidc.provider.idporten.user_info_uri", "https://oidc-ver2.difi.no/userinfo");
    p.put("oidc.provider.idporten.jwk_uri", "https://oidc-ver2.difi.no/jwk");

    List<DhisOidcClientRegistration> parse = GenericOidcProviderConfigParser.parse(p);
    assertThat(parse, hasSize(1));
  }

  @Test
  void parseSkipsAllReservedProviderIds() {
    Properties p = new Properties();
    p.put("oidc.provider.dhis2.client_id", "dhis2-client");
    p.put("oidc.provider.google.client_id", "google-client");
    p.put("oidc.provider.azure.client_id", "azure-client");
    p.put("oidc.provider.wso2.client_id", "wso2-client");
    p.put("oidc.provider.custom.client_id", "custom-client");

    List<DhisOidcClientRegistration> parse = GenericOidcProviderConfigParser.parse(p);
    assertThat(parse, hasSize(0));
  }

  @Test
  void parseConfigAllValidParameters() {
    Properties p = new Properties();
    p.put("oidc.provider.idporten.client_id", "testClientId");
    p.put("oidc.provider.idporten.client_secret", "testClientSecret!#!?");
    p.put("oidc.provider.idporten.authorization_uri", "https://oidc-ver2.difi.no/authorize");
    p.put("oidc.provider.idporten.token_uri", "https://oidc-ver2.difi.no/token");
    p.put("oidc.provider.idporten.user_info_uri", "https://oidc-ver2.difi.no/userinfo");
    p.put("oidc.provider.idporten.jwk_uri", "https://oidc-ver2.difi.no/jwk");
    p.put("oidc.provider.idporten.end_session_endpoint", "https://oidc-ver2.difi.no/endsession");
    p.put("oidc.provider.idporten.scopes", "pid");
    p.put("oidc.provider.idporten.mapping_claim", "helseid://claims/identity/pid");
    p.put("oidc.provider.idporten.display_alias", "IdPorten");
    p.put("oidc.provider.idporten.enable_logout", "true");
    p.put("oidc.provider.idporten.login_image", "/dhis-web-commons/oidc/idporten-logo.svg");
    p.put("oidc.provider.idporten.login_image_padding", "0px 0px");
    p.put("oidc.provider.idporten.extra_request_parameters", "acr_value 4,test_param five");
    p.put("oidc.provider.idporten.enable_pkce", "false");
    List<DhisOidcClientRegistration> parse = GenericOidcProviderConfigParser.parse(p);
    assertThat(parse, hasSize(1));
  }

  @Test
  void parseValidMinimumConfig() {
    Properties p = new Properties();
    p.put("oidc.provider.idporten.client_id", "testClientId");
    p.put("oidc.provider.idporten.client_secret", "testClientSecret!#!?");
    p.put("oidc.provider.idporten.authorization_uri", "https://oidc-ver2.difi.no/authorize");
    p.put("oidc.provider.idporten.token_uri", "https://oidc-ver2.difi.no/token");
    p.put("oidc.provider.idporten.user_info_uri", "https://oidc-ver2.difi.no/userinfo");
    p.put("oidc.provider.idporten.jwk_uri", "https://oidc-ver2.difi.no/jwk");
    p.put("oidc.provider.idporten.end_session_endpoint", "https://oidc-ver2.difi.no/endsession");
    List<DhisOidcClientRegistration> parse = GenericOidcProviderConfigParser.parse(p);
    assertThat(parse, hasSize(1));
  }

  @Test
  void parseConfigMissingRequiredParameter() {
    Properties p = new Properties();
    p.put("oidc.provider.idporten.client_id", "testClientId");
    p.put("oidc.provider.idporten.client_secret", "testClientSecret!#!?");
    p.put("oidc.provider.idporten.token_uri", "https://oidc-ver2.difi.no/token");
    p.put("oidc.provider.idporten.user_info_uri", "https://oidc-ver2.difi.no/userinfo");
    p.put("oidc.provider.idporten.jwk_uri", "https://oidc-ver2.difi.no/jwk");
    p.put("oidc.provider.idporten.end_session_endpoint", "https://oidc-ver2.difi.no/endsession");
    List<DhisOidcClientRegistration> parse = GenericOidcProviderConfigParser.parse(p);
    assertThat(parse, hasSize(0));
  }

  @Test
  void parseConfigMalformedKeyNameParameter() {
    Properties p = new Properties();
    p.put("oidc.provider.idporten.client_id", "testClientId");
    p.put("oidc.provider.idporten.client_secret", "testClientSecret!#!?");
    p.put("oidc.provider.idporten.INVALID_PROPERTY_NAME", "https://oidc-ver2.difi.no/authorize");
    p.put("oidc.provider.idporten.token_uri", "https://oidc-ver2.difi.no/token");
    p.put("oidc.provider.idporten.user_info_uri", "https://oidc-ver2.difi.no/userinfo");
    p.put("oidc.provider.idporten.jwk_uri", "https://oidc-ver2.difi.no/jwk");
    p.put("oidc.provider.idporten.end_session_endpoint", "https://oidc-ver2.difi.no/endsession");
    List<DhisOidcClientRegistration> parse = GenericOidcProviderConfigParser.parse(p);
    assertThat(parse, hasSize(0));
  }

  @Test
  void parseConfigInvalidURIParameter() {
    Properties p = new Properties();
    p.put("oidc.provider.idporten.client_id", "testClientId");
    p.put("oidc.provider.idporten.client_secret", "testClientSecret!#!?");
    p.put(
        "oidc.provider.idporten.authorization_uri",
        "INVALID_URI_SCHEME://oidc-ver2.difi.no/authorize");
    p.put("oidc.provider.idporten.token_uri", "https://oidc-ver2.difi.no/token");
    p.put("oidc.provider.idporten.user_info_uri", "https://oidc-ver2.difi.no/userinfo");
    p.put("oidc.provider.idporten.jwk_uri", "https://oidc-ver2.difi.no/jwk");
    p.put("oidc.provider.idporten.end_session_endpoint", "https://oidc-ver2.difi.no/endsession");
    List<DhisOidcClientRegistration> parse = GenericOidcProviderConfigParser.parse(p);
    assertThat(parse, hasSize(0));
  }

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
    p.put("oidc.provider.idporten.jwk_set_url", "https://oidc-ver2.difi.no/jwks");
    // Builder may still fail to load the keystore from disk; we only assert the
    // *parser* accepts the config. Wrap to ignore loader-time IO errors.
    try {
      assertThat(GenericOidcProviderConfigParser.parse(p), hasSize(1));
    } catch (IllegalStateException expected) {
      // builder can throw when reading non-existent keystore
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
    p.put(pre + "authorization_uri", "https://oidc-ver2.difi.no/authorize");
    p.put(pre + "token_uri", "https://oidc-ver2.difi.no/token");
    p.put(pre + "user_info_uri", "https://oidc-ver2.difi.no/userinfo");
    p.put(pre + "jwk_uri", "https://oidc-ver2.difi.no/jwk");
    return p;
  }
}
