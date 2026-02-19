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
package org.hisp.dhis.security.oidc.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.oidc.DhisOidcClientRegistration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

@ExtendWith(MockitoExtension.class)
class Dhis2InternalOidcProviderTest {

  @Mock private DhisConfigurationProvider config;

  @Test
  void parseReturnsNullWhenClientIdIsEmpty() {
    when(config.getProperty(ConfigurationKey.OIDC_DHIS2_INTERNAL_CLIENT_ID)).thenReturn("");
    assertNull(Dhis2InternalOidcProvider.parse(config));
  }

  @Test
  void parseFallsBackToServerBaseUrlWhenServerUrlIsEmpty() {
    when(config.getProperty(ConfigurationKey.OIDC_DHIS2_INTERNAL_CLIENT_ID))
        .thenReturn("dhis2-internal");
    when(config.getProperty(ConfigurationKey.OIDC_DHIS2_INTERNAL_CLIENT_SECRET))
        .thenReturn("secret");
    when(config.getProperty(ConfigurationKey.OIDC_DHIS2_INTERNAL_SERVER_URL)).thenReturn("");
    when(config.getProperty(ConfigurationKey.SERVER_BASE_URL))
        .thenReturn("http://example.com:8080");
    when(config.getProperty(ConfigurationKey.OIDC_DHIS2_INTERNAL_MAPPING_CLAIM))
        .thenReturn("username");

    DhisOidcClientRegistration registration = Dhis2InternalOidcProvider.parse(config);

    assertNotNull(registration);
    ClientRegistration cr = registration.getClientRegistration();
    assertEquals(
        "http://example.com:8080/oauth2/authorize",
        cr.getProviderDetails().getAuthorizationUri());
    assertEquals("http://example.com:8080/oauth2/token", cr.getProviderDetails().getTokenUri());
    assertEquals("http://example.com:8080/oauth2/jwks", cr.getProviderDetails().getJwkSetUri());
    assertEquals("http://example.com:8080", cr.getProviderDetails().getIssuerUri());
  }

  @Test
  void parseUsesExplicitServerUrlWhenProvided() {
    when(config.getProperty(ConfigurationKey.OIDC_DHIS2_INTERNAL_CLIENT_ID))
        .thenReturn("dhis2-internal");
    when(config.getProperty(ConfigurationKey.OIDC_DHIS2_INTERNAL_CLIENT_SECRET))
        .thenReturn("secret");
    when(config.getProperty(ConfigurationKey.OIDC_DHIS2_INTERNAL_SERVER_URL))
        .thenReturn("http://custom-server:9090");
    when(config.getProperty(ConfigurationKey.OIDC_DHIS2_INTERNAL_MAPPING_CLAIM))
        .thenReturn("username");

    DhisOidcClientRegistration registration = Dhis2InternalOidcProvider.parse(config);

    assertNotNull(registration);
    ClientRegistration cr = registration.getClientRegistration();
    assertEquals(
        "http://custom-server:9090/oauth2/authorize",
        cr.getProviderDetails().getAuthorizationUri());
    assertEquals("http://custom-server:9090", cr.getProviderDetails().getIssuerUri());
  }

  @Test
  void parseStripsTrailingSlashFromServerBaseUrl() {
    when(config.getProperty(ConfigurationKey.OIDC_DHIS2_INTERNAL_CLIENT_ID))
        .thenReturn("dhis2-internal");
    when(config.getProperty(ConfigurationKey.OIDC_DHIS2_INTERNAL_CLIENT_SECRET))
        .thenReturn("secret");
    when(config.getProperty(ConfigurationKey.OIDC_DHIS2_INTERNAL_SERVER_URL)).thenReturn("");
    when(config.getProperty(ConfigurationKey.SERVER_BASE_URL))
        .thenReturn("http://example.com:8080/");
    when(config.getProperty(ConfigurationKey.OIDC_DHIS2_INTERNAL_MAPPING_CLAIM))
        .thenReturn("username");

    DhisOidcClientRegistration registration = Dhis2InternalOidcProvider.parse(config);

    assertNotNull(registration);
    ClientRegistration cr = registration.getClientRegistration();
    assertEquals(
        "http://example.com:8080/oauth2/authorize",
        cr.getProviderDetails().getAuthorizationUri());
    assertEquals("http://example.com:8080", cr.getProviderDetails().getIssuerUri());
  }

  @Test
  void parseSetVisibleOnLoginPageToFalse() {
    when(config.getProperty(ConfigurationKey.OIDC_DHIS2_INTERNAL_CLIENT_ID))
        .thenReturn("dhis2-internal");
    when(config.getProperty(ConfigurationKey.OIDC_DHIS2_INTERNAL_CLIENT_SECRET))
        .thenReturn("secret");
    when(config.getProperty(ConfigurationKey.OIDC_DHIS2_INTERNAL_SERVER_URL))
        .thenReturn("http://localhost:8080");
    when(config.getProperty(ConfigurationKey.OIDC_DHIS2_INTERNAL_MAPPING_CLAIM))
        .thenReturn("username");

    DhisOidcClientRegistration registration = Dhis2InternalOidcProvider.parse(config);

    assertNotNull(registration);
    assertFalse(registration.isVisibleOnLoginPage());
  }

  @Test
  void parseRegistrationIdIsDhis2Internal() {
    when(config.getProperty(ConfigurationKey.OIDC_DHIS2_INTERNAL_CLIENT_ID))
        .thenReturn("dhis2-internal");
    when(config.getProperty(ConfigurationKey.OIDC_DHIS2_INTERNAL_CLIENT_SECRET))
        .thenReturn("secret");
    when(config.getProperty(ConfigurationKey.OIDC_DHIS2_INTERNAL_SERVER_URL))
        .thenReturn("http://localhost:8080");
    when(config.getProperty(ConfigurationKey.OIDC_DHIS2_INTERNAL_MAPPING_CLAIM))
        .thenReturn("username");

    DhisOidcClientRegistration registration = Dhis2InternalOidcProvider.parse(config);

    assertNotNull(registration);
    assertEquals("dhis2-internal", registration.getClientRegistration().getRegistrationId());
  }
}
