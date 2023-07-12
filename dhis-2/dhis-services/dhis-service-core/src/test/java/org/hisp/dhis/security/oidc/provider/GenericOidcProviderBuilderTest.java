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
package org.hisp.dhis.security.oidc.provider;

import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.EXTRA_REQUEST_PARAMETERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.hisp.dhis.security.oidc.DhisOidcClientRegistration;
import org.hisp.dhis.security.oidc.GenericOidcProviderConfigParser;
import org.junit.jupiter.api.Test;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class GenericOidcProviderBuilderTest {

  @Test
  @SuppressWarnings("unchecked")
  void testBuildSuccessfully() {
    Properties p = new Properties();
    p.put("oidc.provider.idporten.client_id", "testClientId");
    p.put("oidc.provider.idporten.client_secret", "testClientSecret!#!?");
    p.put("oidc.provider.idporten.ext_client.android.client_id", "externalClientId");
    p.put("oidc.provider.idporten.authorization_uri", "https://oidc-ver2.difi.no/authorize");
    p.put("oidc.provider.idporten.token_uri", "https://oidc-ver2.difi.no/token");
    p.put("oidc.provider.idporten.user_info_uri", "https://oidc-ver2.difi.no/userinfo");
    p.put("oidc.provider.idporten.jwk_uri", "https://oidc-ver2.difi.no/jwk");
    p.put("oidc.provider.idporten.issuer_uri", "https://oidc-ver2.difi.no");
    p.put("oidc.provider.idporten.end_session_endpoint", "https://oidc-ver2.difi.no/endsession");
    p.put("oidc.provider.idporten.scopes", "pid");
    p.put("oidc.provider.idporten.mapping_claim", "helseid://claims/identity/pid");
    p.put("oidc.provider.idporten.display_alias", "IdPorten");
    p.put("oidc.provider.idporten.enable_logout", "true");
    p.put("oidc.provider.idporten.login_image", "../oidc/idporten-logo.svg");
    p.put("oidc.provider.idporten.login_image_padding", "0px 0px");
    p.put("oidc.provider.idporten.extra_request_parameters", "acr_value 4 , test_param five");
    p.put("oidc.provider.idporten.enable_pkce", "true");
    List<DhisOidcClientRegistration> providerConfigList = GenericOidcProviderConfigParser.parse(p);
    assertEquals(providerConfigList.size(), 1);
    DhisOidcClientRegistration r = providerConfigList.get(0);
    assertNotNull(r);
    final String registrationId = r.getClientRegistration().getRegistrationId();
    assertEquals(registrationId, "idporten");
    assertEquals("helseid://claims/identity/pid", r.getMappingClaimKey());
    assertEquals("../oidc/idporten-logo.svg", r.getLoginIcon());
    assertEquals("0px 0px", r.getLoginIconPadding());
    assertEquals("IdPorten", r.getLoginText());
    assertEquals("testClientId", r.getClientRegistration().getClientId());
    assertEquals("testClientSecret!#!?", r.getClientRegistration().getClientSecret());
    assertTrue(r.getClientRegistration().getScopes().contains("pid"));
    assertEquals(
        "https://oidc-ver2.difi.no/token",
        r.getClientRegistration().getProviderDetails().getTokenUri());
    assertEquals(
        "https://oidc-ver2.difi.no/authorize",
        r.getClientRegistration().getProviderDetails().getAuthorizationUri());
    assertEquals(
        "https://oidc-ver2.difi.no/jwk",
        r.getClientRegistration().getProviderDetails().getJwkSetUri());
    assertEquals(
        "https://oidc-ver2.difi.no/userinfo",
        r.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri());
    assertEquals(
        "https://oidc-ver2.difi.no", r.getClientRegistration().getProviderDetails().getIssuerUri());
    assertEquals(
        "true",
        r.getClientRegistration()
            .getProviderDetails()
            .getConfigurationMetadata()
            .get("enable_pkce"));
    Object parameters =
        r.getClientRegistration()
            .getProviderDetails()
            .getConfigurationMetadata()
            .get("extra_request_parameters");
    Map<String, String> extraRequestParams = (Map<String, String>) parameters;
    assertEquals("4", extraRequestParams.get("acr_value"));
    Map<String, Map<String, String>> externalClients = r.getExternalClients();
    assertNotNull(externalClients);
    Map<String, String> android = externalClients.get("android");
    assertNotNull(externalClients);
    String client_id = android.get("client_id");
    assertEquals("externalClientId", client_id);
  }

  @Test
  void testParseExtraRequestParameters() {
    HashMap<String, String> hashMap = new HashMap<>();
    hashMap.put(
        EXTRA_REQUEST_PARAMETERS,
        "   acr_value    4    ,    test_param five,    test_param2  six ");
    Map<String, String> params = GenericOidcProviderBuilder.getExtraRequestParameters(hashMap);
    assertEquals("4", params.get("acr_value"));
    assertEquals("five", params.get("test_param"));
    assertEquals("six", params.get("test_param2"));
  }
}
