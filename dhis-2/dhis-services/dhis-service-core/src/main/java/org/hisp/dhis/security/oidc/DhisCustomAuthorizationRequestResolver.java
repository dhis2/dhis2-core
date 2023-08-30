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

import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.ENABLE_PKCE;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.EXTRA_REQUEST_PARAMETERS;
import static org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.stereotype.Component;

@Component
public class DhisCustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {
  public static final String PKCE_CHALLENGE_METHOD = "S256";

  public static final String HASH_DIGEST_ALGORITHM = "SHA-256";

  @Autowired private DhisOidcProviderRepository clientRegistrationRepository;

  private DefaultOAuth2AuthorizationRequestResolver defaultResolver;

  private final StringKeyGenerator secureKeyGenerator =
      new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 96);

  @PostConstruct
  public void init() {
    defaultResolver =
        new DefaultOAuth2AuthorizationRequestResolver(
            clientRegistrationRepository, DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
  }

  @Override
  public OAuth2AuthorizationRequest resolve(HttpServletRequest servletRequest) {
    String requestURI = servletRequest.getRequestURI();
    if (requestURI.contains(DEFAULT_AUTHORIZATION_REQUEST_BASE_URI)) {
      String[] split = requestURI.split("/");
      String clientRegistrationId = split[split.length - 1];
      OAuth2AuthorizationRequest req =
          defaultResolver.resolve(servletRequest, clientRegistrationId);
      return customizeAuthorizationRequest(req, clientRegistrationId);
    } else {
      return this.defaultResolver.resolve(servletRequest);
    }
  }

  @Override
  public OAuth2AuthorizationRequest resolve(
      HttpServletRequest servletRequest, String clientRegistrationId) {
    OAuth2AuthorizationRequest req = defaultResolver.resolve(servletRequest, clientRegistrationId);
    return customizeAuthorizationRequest(req, clientRegistrationId);
  }

  private OAuth2AuthorizationRequest customizeAuthorizationRequest(
      OAuth2AuthorizationRequest req, String clientRegistrationId) {
    if (req == null) {
      return null;
    }

    Map<String, Object> attributes = new HashMap<>(req.getAttributes());
    Map<String, Object> additionalParameters = new HashMap<>(req.getAdditionalParameters());

    ClientRegistration clientRegistration =
        clientRegistrationRepository.findByRegistrationId(clientRegistrationId);

    if (clientRegistration == null) {
      return null;
    }

    Map<String, Object> configurationMetadata =
        clientRegistration.getProviderDetails().getConfigurationMetadata();

    boolean enablePkce =
        DhisConfigurationProvider.isOn((String) configurationMetadata.get(ENABLE_PKCE));
    if (enablePkce) {
      addPkceParameters(attributes, additionalParameters);
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> extraRequestParameters =
        (Map<String, Object>) configurationMetadata.get(EXTRA_REQUEST_PARAMETERS);

    if (extraRequestParameters != null && !extraRequestParameters.isEmpty()) {
      for (String key : extraRequestParameters.keySet()) {
        String value = (String) extraRequestParameters.get(key);
        additionalParameters.put(key, value);
      }
    }

    return OAuth2AuthorizationRequest.from(req)
        .attributes(attributes)
        .additionalParameters(additionalParameters)
        .build();
  }

  private void addPkceParameters(
      Map<String, Object> attributes, Map<String, Object> additionalParameters) {
    String codeVerifier = this.secureKeyGenerator.generateKey();

    attributes.put(PkceParameterNames.CODE_VERIFIER, codeVerifier);

    try {
      String codeChallenge = createHash(codeVerifier);
      additionalParameters.put(PkceParameterNames.CODE_CHALLENGE, codeChallenge);
      additionalParameters.put(PkceParameterNames.CODE_CHALLENGE_METHOD, PKCE_CHALLENGE_METHOD);
    } catch (NoSuchAlgorithmException e) {
      additionalParameters.put(PkceParameterNames.CODE_CHALLENGE, codeVerifier);
    }
  }

  private static String createHash(String value) throws NoSuchAlgorithmException {
    byte[] digest =
        MessageDigest.getInstance(HASH_DIGEST_ALGORITHM)
            .digest(value.getBytes(StandardCharsets.US_ASCII));

    return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
  }
}
