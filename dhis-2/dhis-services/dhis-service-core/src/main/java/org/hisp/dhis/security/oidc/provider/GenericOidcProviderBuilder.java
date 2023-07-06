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

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.oidc.DhisOidcClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthenticationMethod;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class GenericOidcProviderBuilder extends AbstractOidcProvider {
  private GenericOidcProviderBuilder() {
    throw new IllegalStateException("Utility class");
  }

  public static DhisOidcClientRegistration build(
      Map<String, String> config, Map<String, Map<String, String>> externalClients) {
    Objects.requireNonNull(config, "DhisConfigurationProvider is missing!");

    String providerId = config.get(PROVIDER_ID);
    String clientId = config.get(CLIENT_ID);
    String clientSecret = config.get(CLIENT_SECRET);

    if (providerId.isEmpty() || clientId.isEmpty()) {
      return null;
    }

    if (clientSecret.isEmpty()) {
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
        .build();
  }

  private static ClientRegistration buildClientRegistration(
      Map<String, String> config, String providerId, String clientId, String clientSecret) {
    ClientRegistration.Builder builder = ClientRegistration.withRegistrationId(providerId);
    builder.clientName(providerId);
    builder.clientId(clientId);
    builder.clientSecret(clientSecret);
    builder.clientAuthenticationMethod(ClientAuthenticationMethod.BASIC);
    builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
    builder.authorizationUri(config.get(AUTHORIZATION_URI));
    builder.tokenUri(config.get(TOKEN_URI));
    builder.jwkSetUri(config.get(JWK_URI));
    builder.issuerUri(config.get(ISSUER_URI));
    builder.userInfoUri(config.get(USERINFO_URI));
    builder.redirectUri(
        StringUtils.defaultIfEmpty(config.get(REDIRECT_URL), DEFAULT_REDIRECT_TEMPLATE_URL));
    builder.userInfoAuthenticationMethod(AuthenticationMethod.HEADER);
    builder.userNameAttributeName(IdTokenClaimNames.SUB);
    builder.scope(
        ImmutableList.<String>builder()
            .add(DEFAULT_SCOPE)
            .add(StringUtils.defaultIfEmpty(config.get(SCOPES), "").split(" "))
            .build());

    builder.providerConfigurationMetadata(parseMetaData(config));

    return builder.build();
  }

  private static Map<String, Object> parseMetaData(Map<String, String> config) {
    final Map<String, Object> metadata = new HashMap<>();

    metadata.put(EXTRA_REQUEST_PARAMETERS, getExtraRequestParameters(config));

    if (DhisConfigurationProvider.isOn(config.get(ENABLE_LOGOUT))) {
      metadata.put(
          END_SESSION_ENDPOINT, StringUtils.defaultIfEmpty(config.get(END_SESSION_ENDPOINT), ""));
    }

    if (DhisConfigurationProvider.isOn(config.get(ENABLE_PKCE))) {
      metadata.put(ENABLE_PKCE, Boolean.TRUE.toString());
    }

    return metadata;
  }

  /**
   * Extra req. params has to be in this form: acr_value 4, test_param five (trailing (PARAM1_NAME
   * VALUE1,PARAM2_NAME VALUE2...)
   *
   * @param config
   * @return
   */
  public static Map<String, String> getExtraRequestParameters(Map<String, String> config) {
    String params = StringUtils.defaultIfEmpty(config.get(EXTRA_REQUEST_PARAMETERS), "");

    return Arrays.stream(params.split(","))
        .filter(s -> s.trim().split("\\s+").length == 2)
        .map(s -> Pair.of(s.trim().split("\\s+")[0], s.trim().split("\\s+")[1]))
        .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
  }
}
