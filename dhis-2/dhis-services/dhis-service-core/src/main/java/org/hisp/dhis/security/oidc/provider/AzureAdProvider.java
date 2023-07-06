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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.oidc.DhisOidcClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthenticationMethod;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 *     <p>Well known url for reference. In a perfect world we would dynamically parse this.
 *     https://login.microsoftonline.com/"+tenant+"/v2.0/.well-known/openid-configuration
 */
public class AzureAdProvider extends AbstractOidcProvider {
  public static final int MAX_AZURE_TENANTS = 10;

  public static final String PROVIDER_PREFIX = "oidc.provider.azure.";

  public static final String AZURE_TENANT = "tenant";

  public static final String PROVIDER_STATIC_ISSUER_URI_START =
      "https://login.microsoftonline.com/";

  private AzureAdProvider() {
    throw new IllegalStateException("Utility class");
  }

  public static List<DhisOidcClientRegistration> parse(Properties config) {
    Objects.requireNonNull(config, "DhisConfigurationProvider is missing!");

    final ImmutableList.Builder<DhisOidcClientRegistration> clients = ImmutableList.builder();

    for (int i = 0; i < MAX_AZURE_TENANTS; i++) {
      String propertyPrefix = PROVIDER_PREFIX + i + '.';

      String tenant = config.getProperty(propertyPrefix + AZURE_TENANT, "");
      if (tenant.isEmpty()) {
        continue;
      }

      DhisOidcClientRegistration dhisOidcClientRegistration =
          DhisOidcClientRegistration.builder()
              .clientRegistration(buildClientRegistration(config, tenant, propertyPrefix))
              .mappingClaimKey(
                  MoreObjects.firstNonNull(
                      config.getProperty(propertyPrefix + MAPPING_CLAIM), DEFAULT_MAPPING_CLAIM))
              .loginIcon("../oidc/btn_azure_login.svg")
              .loginIconPadding("13px 13px")
              .loginText(config.getProperty(propertyPrefix + DISPLAY_ALIAS, "login_with_azure"))
              .build();

      clients.add(dhisOidcClientRegistration);
    }

    return clients.build();
  }

  private static ClientRegistration buildClientRegistration(
      Properties properties, String tenant, String propertyPrefix) {
    String clientId = properties.getProperty(propertyPrefix + CLIENT_ID, "");
    String clientSecret = properties.getProperty(propertyPrefix + CLIENT_SECRET);

    if (clientId.isEmpty()) {
      throw new IllegalArgumentException("Azure client id is missing! tenant=" + tenant);
    }

    if (clientSecret.isEmpty()) {
      throw new IllegalArgumentException("Azure client secret is missing! tenant=" + tenant);
    }

    String tenantUriStart = PROVIDER_STATIC_ISSUER_URI_START + tenant;

    ClientRegistration.Builder builder = ClientRegistration.withRegistrationId(tenant);
    builder.clientName(tenant);
    builder.clientId(clientId);
    builder.clientSecret(clientSecret);
    builder.clientAuthenticationMethod(ClientAuthenticationMethod.BASIC);
    builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
    builder.scope("openid", "profile", DEFAULT_MAPPING_CLAIM);
    builder.authorizationUri(tenantUriStart + "/oauth2/v2.0/authorize");
    builder.tokenUri(tenantUriStart + "/oauth2/v2.0/token");
    builder.jwkSetUri(tenantUriStart + "/discovery/v2.0/keys");
    builder.userInfoUri("https://graph.microsoft.com/oidc/userinfo");
    builder.redirectUri(
        StringUtils.firstNonBlank(
            properties.getProperty(propertyPrefix + REDIRECT_URL), DEFAULT_REDIRECT_TEMPLATE_URL));
    builder.userInfoAuthenticationMethod(AuthenticationMethod.HEADER);
    builder.userNameAttributeName(IdTokenClaimNames.SUB);

    boolean supportLogout =
        DhisConfigurationProvider.isOn(
            MoreObjects.firstNonNull(
                properties.getProperty(propertyPrefix + ENABLE_LOGOUT), "TRUE"));
    if (supportLogout) {
      builder.providerConfigurationMetadata(
          ImmutableMap.of("end_session_endpoint", tenantUriStart + "/oauth2/v2.0/logout"));
    }

    return builder.build();
  }
}
