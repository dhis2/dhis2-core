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
import static org.hisp.dhis.external.conf.ConfigurationKey.SERVER_BASE_URL;
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
 * Builds the internal DHIS2-as-IdP {@link DhisOidcClientRegistration} with registration id {@code
 * dhis2-internal}. This provider is built-in and is auto-registered when {@code
 * oauth2.server.enabled=on} in {@code dhis.conf}; the {@code oidc.oauth2.login.enabled} flag does
 * NOT gate it.
 *
 * <p>All endpoint URIs ({@code /oauth2/authorize}, {@code /oauth2/token}, {@code /oauth2/jwks},
 * {@code /oauth2/userinfo}, issuer) are derived from {@code server.base.url} (or the explicit
 * {@code oidc.provider.dhis2.server_url} override), so the DHIS2 instance itself acts as both
 * authorization server and resource server.
 *
 * <p>The registration is intentionally hidden from the web login page ({@code
 * visibleOnLoginPage=false}); it is consumed only by the DHIS2 Android Capture app's {@code
 * authorization_code} flow against this DHIS2 instance as authorization server, and for
 * resource-server JWT validation of tokens it issues.
 *
 * <p>Defaults: {@code client_id=dhis2-internal}, {@code client_secret=secret}, {@code
 * mapping_claim=username}.
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

    String serverUrl = config.getProperty(OIDC_DHIS2_INTERNAL_SERVER_URL);
    if (Strings.isNullOrEmpty(serverUrl)) {
      serverUrl = config.getProperty(SERVER_BASE_URL);
    }
    if (serverUrl.endsWith("/")) {
      serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
    }

    ClientRegistration clientRegistration =
        buildClientRegistration(dhis2ClientId, dhis2ClientSecret, serverUrl);

    return DhisOidcClientRegistration.builder()
        .clientRegistration(clientRegistration)
        .mappingClaimKey(config.getProperty(OIDC_DHIS2_INTERNAL_MAPPING_CLAIM))
        .loginIcon("")
        .loginIconPadding("")
        .loginText("not visible")
        .visibleOnLoginPage(false)
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
    builder.issuerUri(providerBaseUrl + "/");
    builder.redirectUri(DEFAULT_REDIRECT_TEMPLATE_URL);
    builder.userInfoAuthenticationMethod(AuthenticationMethod.HEADER);
    builder.userNameAttributeName(IdTokenClaimNames.SUB);

    return builder.build();
  }
}
