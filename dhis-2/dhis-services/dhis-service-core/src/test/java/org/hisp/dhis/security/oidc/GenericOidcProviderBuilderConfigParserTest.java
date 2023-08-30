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
package org.hisp.dhis.security.oidc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class GenericOidcProviderBuilderConfigParserTest {

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
    p.put("oidc.provider.idporten.login_image", "../oidc/idporten-logo.svg");
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
}
