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

import static org.hisp.dhis.external.conf.ConfigurationKey.OIDC_DHIS2_INTERNAL_CLIENT_ID;
import static org.hisp.dhis.external.conf.ConfigurationKey.OIDC_DHIS2_INTERNAL_CLIENT_SECRET;
import static org.hisp.dhis.external.conf.ConfigurationKey.OIDC_DHIS2_INTERNAL_MAPPING_CLAIM;
import static org.hisp.dhis.external.conf.ConfigurationKey.OIDC_DHIS2_INTERNAL_SERVER_URL;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.DEFAULT_MAPPING_CLAIM;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.DEFAULT_REDIRECT_TEMPLATE_URL;

import com.google.common.base.Strings;
import java.util.Objects;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.oidc.DhisOidcClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthenticationMethod;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

/**
 * Internal only OIDC provider, used when authorization server is enabled in dhis.conf with
 * (OAUTH2_SERVER_ENABLED).
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class Dhis2InternalOidcProvider {
  public static final String REGISTRATION_ID = "dhis2-internal";

  private Dhis2InternalOidcProvider() {
    throw new IllegalStateException("Utility class");
  }

  public static DhisOidcClientRegistration parse(DhisConfigurationProvider config) {
    Objects.requireNonNull(config, "DhisConfigurationProvider is missing!");

    String dhis2ClientId = config.getProperty(OIDC_DHIS2_INTERNAL_CLIENT_ID);
    String dhis2ClientSecret = config.getProperty(OIDC_DHIS2_INTERNAL_CLIENT_SECRET);

    if (Strings.isNullOrEmpty(dhis2ClientId)) {
      return null;
    }

    if (Strings.isNullOrEmpty(dhis2ClientSecret)) {
      throw new IllegalArgumentException("DHIS2 internal client secret is missing!");
    }

    ClientRegistration clientRegistration =
        buildClientRegistration(
            dhis2ClientId, dhis2ClientSecret, config.getProperty(OIDC_DHIS2_INTERNAL_SERVER_URL));

    return DhisOidcClientRegistration.builder()
        .clientRegistration(clientRegistration)
        .mappingClaimKey(config.getProperty(OIDC_DHIS2_INTERNAL_MAPPING_CLAIM))
        .loginIcon("")
        .loginIconPadding("")
        .loginText("not visible")
        .build();
  }

  private static ClientRegistration buildClientRegistration(
      String clientId, String clientSecret, String providerBaseUrl) {
    ClientRegistration.Builder builder =
        ClientRegistration.withRegistrationId(Dhis2InternalOidcProvider.REGISTRATION_ID);
    builder.clientName(clientId);
    builder.clientId(clientId);
    builder.clientSecret(clientSecret);
    builder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
    builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
    builder.scope("openid", "profile", DEFAULT_MAPPING_CLAIM);
    builder.authorizationUri(providerBaseUrl + "/oauth2/authorize");
    builder.tokenUri(providerBaseUrl + "/oauth2/token");
    builder.jwkSetUri(providerBaseUrl + "/oauth2/jwks");
    builder.userInfoUri(providerBaseUrl + "/oauth2/userinfo");
    builder.issuerUri(providerBaseUrl);
    builder.redirectUri(DEFAULT_REDIRECT_TEMPLATE_URL);
    builder.userInfoAuthenticationMethod(AuthenticationMethod.HEADER);
    builder.userNameAttributeName(IdTokenClaimNames.SUB);

    return builder.build();
  }
}
