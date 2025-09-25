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

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.net.URL;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Service
public class DhisOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {
  @Autowired public UserService userService;

  @Autowired private DhisOidcProviderRepository clientRegistrationRepository;

  private final RestTemplate restTemplate = new RestTemplate();

  public OidcUser loadUserX(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
    OidcUser oidcUser = null; // = super.loadUser(userRequest);

    ClientRegistration clientRegistration = userRequest.getClientRegistration();

    DhisOidcClientRegistration oidcClientRegistration =
        clientRegistrationRepository.getDhisOidcClientRegistration(
            clientRegistration.getRegistrationId());

    String mappingClaimKey = oidcClientRegistration.getMappingClaimKey();
    Map<String, Object> attributes = oidcUser.getAttributes();
    Object claimValue = attributes.get(mappingClaimKey);
    OidcUserInfo userInfo = oidcUser.getUserInfo();
    if (claimValue == null && userInfo != null) {
      claimValue = userInfo.getClaim(mappingClaimKey);
    }

    if (log.isDebugEnabled()) {
      log.debug(
          String.format(
              "Trying to look up DHIS2 user with OidcUser mapping mappingClaimKey='%s', claim value='%s'",
              mappingClaimKey, claimValue));
    }

    if (claimValue != null) {
      User user = userService.getUserByOpenId((String) claimValue);
      if (user != null && user.isExternalAuth()) {
        if (user.isDisabled() || !user.isAccountNonExpired()) {
          throw new OAuth2AuthenticationException(
              new OAuth2Error("user_disabled"), "User is disabled");
        }

        UserDetails userDetails = userService.createUserDetails(user);

        return new DhisOidcUser(
            userDetails, attributes, IdTokenClaimNames.SUB, oidcUser.getIdToken());
      }
    }

    String errorMessage =
        String.format(
            "Failed to look up DHIS2 user with OidcUser mapping mapping; mappingClaimKey='%s', claimValue='%s'",
            mappingClaimKey, claimValue);

    if (log.isDebugEnabled()) {
      log.debug(errorMessage);
    }

    OAuth2Error oauth2Error =
        new OAuth2Error("could_not_map_oidc_user_to_dhis2_user", errorMessage, null);

    throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
  }

  @Override
  public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
    ClientRegistration clientRegistration = userRequest.getClientRegistration();

    DhisOidcClientRegistration dhisClientReg =
        clientRegistrationRepository.getDhisOidcClientRegistration(
            clientRegistration.getRegistrationId());

    String mappingClaimKey = dhisClientReg.getMappingClaimKey();

    // 1. Get the UserInfo endpoint URI from the client registration
    String userInfoUri =
        dhisClientReg.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri();

    if (userInfoUri == null || userInfoUri.isEmpty()) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error("missing_user_info_uri"),
          "Missing UserInfo Endpoint URI in ClientRegistration");
    }

    // 2. Make the request to the UserInfo endpoint
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(userRequest.getAccessToken().getTokenValue());
    headers.setAccept(
        java.util.Collections.singletonList(
            MediaType.valueOf("application/jwt"))); // Crucial: Ask for JWT
    HttpEntity<String> entity = new HttpEntity<>("", headers);

    ResponseEntity<String> response;
    try {
      response = restTemplate.exchange(userInfoUri, HttpMethod.GET, entity, String.class);
    } catch (Exception e) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error("invalid_user_info_response"), "Failed to fetch UserInfo response", e);
    }

    String signedJwt = response.getBody();

    try {
      // 3. Configure a JWT processor to verify the signature
      ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();

      //      JWK jwk = dhisClientReg.getJwk();
      //      if (jwk != null) {
      //        // If we have a static JWK, use it directly
      //        JWKSource<SecurityContext> keySource =
      //            (jwkSelector, context) -> java.util.Collections.singletonList(jwk);
      //        JWSKeySelector<SecurityContext> keySelector =
      //            new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource);
      //        jwtProcessor.setJWSKeySelector(keySelector);
      //      }

      // http://localhost:8088/v1/esignet/oauth/.well-known/jwks.json

      //      // 4. Fetch the JWK set from the IDP's jwks_uri
      String jwksUri = userRequest.getClientRegistration().getProviderDetails().getJwkSetUri();

      //      JWKSource<SecurityContext> keySource =
      //          new RemoteJWKSet<>(
      //              new URL("http://localhost:8088/v1/esignet/oauth/.well-known/jwks.json"));

      JWKSourceBuilder<SecurityContext> securityContextJWKSourceBuilder =
          JWKSourceBuilder.create(new URL(jwksUri));
      JWKSource<SecurityContext> keySource = securityContextJWKSourceBuilder.build();
      //
      //      // 5. Create a key selector to pick the correct key for verification
      //      // The JWS algorithm is taken from the ID token as a hint
      JWSKeySelector<SecurityContext> keySelector =
          new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource);

      jwtProcessor.setJWSKeySelector(keySelector);

      // 6. Process the JWT, which verifies the signature and decodes the payload
      JWTClaimsSet claimsSet = jwtProcessor.process(signedJwt, null);

      Map<String, Object> claims = claimsSet.toJSONObject();

      // You can map authorities from claims here if needed
      // For example: authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

      return new DhisOidcUser(null, claims, IdTokenClaimNames.SUB, userRequest.getIdToken());
      // The OidcUser requires the original ID Token and the new UserInfo claims
      //      return new DefaultOidcUser(authorities, userRequest.getIdToken(), claims);

    } catch (Exception e) {
      log.error("Error processing UserInfo JWT", e);
      throw new OAuth2AuthenticationException(
          new OAuth2Error("jwt_processing_error"), "Failed to process UserInfo JWT", e);
    }
  }
}
