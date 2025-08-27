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

import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration.ProviderDetails;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthenticationMethod;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.converter.ClaimConversionService;
import org.springframework.security.oauth2.core.converter.ClaimTypeConverter;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownContentTypeException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Service
public class DhisOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {
  @Autowired public UserService userService;

  @Autowired private DhisOidcProviderRepository clientRegistrationRepository;


  private static final String MISSING_USER_INFO_URI_ERROR_CODE = "missing_user_info_uri";

  private static final String MISSING_USER_NAME_ATTRIBUTE_ERROR_CODE = "missing_user_name_attribute";

  private static final String INVALID_USER_INFO_RESPONSE_ERROR_CODE = "invalid_user_info_response";

  private static final ParameterizedTypeReference<Map<String, Object>> PARAMETERIZED_RESPONSE_TYPE = new ParameterizedTypeReference<>() {
  };

  private Converter<OAuth2UserRequest, RequestEntity<?>> requestEntityConverter;// = new OAuth2UserRequestEntityConverter();

  private Converter<OAuth2UserRequest, Converter<Map<String, Object>, Map<String, Object>>> attributesConverter = (
      request) -> (attributes) -> attributes;

  private RestOperations restOperations;




  private static final Converter<Map<String, Object>, Map<String, Object>>
      DEFAULT_CLAIM_TYPE_CONVERTER = new ClaimTypeConverter(createDefaultClaimTypeConverters());

  private Set<String> accessibleScopes =
      new HashSet<>(
          Arrays.asList(
              OidcScopes.PROFILE, OidcScopes.EMAIL, OidcScopes.ADDRESS, OidcScopes.PHONE));


  private Function<ClientRegistration, Converter<Map<String, Object>, Map<String, Object>>>
      claimTypeConverterFactory = (clientRegistration) -> DEFAULT_CLAIM_TYPE_CONVERTER;


   static class DHIS2OAuth2UserRequestEntityConverter implements Converter<OAuth2UserRequest, RequestEntity<?>> {

    private static final MediaType DEFAULT_CONTENT_TYPE = MediaType
        .valueOf(MediaType.APPLICATION_FORM_URLENCODED_VALUE + ";charset=UTF-8");

    /**
     * Returns the {@link RequestEntity} used for the UserInfo Request.
     * @param userRequest the user request
     * @return the {@link RequestEntity} used for the UserInfo Request
     */
    @Override
    public RequestEntity<?> convert(OAuth2UserRequest userRequest) {
      ClientRegistration clientRegistration = userRequest.getClientRegistration();
      HttpMethod httpMethod = getHttpMethod(clientRegistration);
      HttpHeaders headers = new HttpHeaders();
//      headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
//      headers.setAccept(Collections.singletonList(MediaType.ALL));
      headers.add("Accept","application/jwt");
      URI uri = UriComponentsBuilder
          .fromUriString(clientRegistration.getProviderDetails().getUserInfoEndpoint().getUri())
          .build()
          .toUri();

      RequestEntity<?> request;
      if (HttpMethod.POST.equals(httpMethod)) {
        headers.setContentType(DEFAULT_CONTENT_TYPE);
        MultiValueMap<String, String> formParameters = new LinkedMultiValueMap<>();
        formParameters.add(OAuth2ParameterNames.ACCESS_TOKEN, userRequest.getAccessToken().getTokenValue());
        request = new RequestEntity<>(formParameters, headers, httpMethod, uri);
      }
      else {
        headers.setBearerAuth(userRequest.getAccessToken().getTokenValue());
        request = new RequestEntity<>(headers, httpMethod, uri);
      }

      return request;
    }

    private HttpMethod getHttpMethod(ClientRegistration clientRegistration) {
      if (AuthenticationMethod.FORM
          .equals(clientRegistration.getProviderDetails().getUserInfoEndpoint().getAuthenticationMethod())) {
        return HttpMethod.POST;
      }
      return HttpMethod.GET;
    }

  }


  @PostConstruct
  private void init() {
    RestTemplate restTemplate = new RestTemplate();
//    restTemplate.setRequestFactory(new StrippingRequestFactory(restTemplate.getRequestFactory()));
    restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());

    this.restOperations = restTemplate;

    //    restTemplate
    //        .getInterceptors()
    //        .add(
    //            (req, body, ex) -> {
    //              req.getHeaders().remove(HttpHeaders.ACCEPT);
    //              // or:
    // req.getHeaders().setAccept(List.of(MediaType.valueOf("application/jwt")));
    //              return ex.execute(req, body);
    //            });

    //    this.oauth2UserService = new DefaultOAuth2UserService();
    //    ((DefaultOAuth2UserService) this.oauth2UserService).setRestOperations(restTemplate);

    DHIS2OAuth2UserRequestEntityConverter converter = new DHIS2OAuth2UserRequestEntityConverter();

//
//    ((DefaultOAuth2UserService) this.oauth2UserService)
//        .setRequestEntityConverter(converter);

    this.requestEntityConverter = converter;
  }

  //  private Predicate<OidcUserRequest> retrieveUserInfo = this::shouldRetrieveUserInfo;

  //  private BiFunction<OidcUserRequest, OidcUserInfo, OidcUser> oidcUserMapper =
  // OidcUserRequestUtils::getUser;

  /**
   * Returns the default {@link Converter}'s used for type conversion of claim values for an {@link
   * OidcUserInfo}.
   *
   * @return a {@link Map} of {@link Converter}'s keyed by {@link StandardClaimNames claim name}
   * @since 5.2
   */
  public static Map<String, Converter<Object, ?>> createDefaultClaimTypeConverters() {
    Converter<Object, ?> booleanConverter = getConverter(TypeDescriptor.valueOf(Boolean.class));
    Converter<Object, ?> instantConverter = getConverter(TypeDescriptor.valueOf(Instant.class));
    Map<String, Converter<Object, ?>> claimTypeConverters = new HashMap<>();
    claimTypeConverters.put(StandardClaimNames.EMAIL_VERIFIED, booleanConverter);
    claimTypeConverters.put(StandardClaimNames.PHONE_NUMBER_VERIFIED, booleanConverter);
    claimTypeConverters.put(StandardClaimNames.UPDATED_AT, instantConverter);
    return claimTypeConverters;
  }

  private static Converter<Object, ?> getConverter(TypeDescriptor targetDescriptor) {
    TypeDescriptor sourceDescriptor = TypeDescriptor.valueOf(Object.class);
    return (source) ->
        ClaimConversionService.getSharedInstance()
            .convert(source, sourceDescriptor, targetDescriptor);
  }

  private Map<String, Object> getClaims(OidcUserRequest userRequest, OAuth2User oauth2User) {
    Converter<Map<String, Object>, Map<String, Object>> converter =
        this.claimTypeConverterFactory.apply(userRequest.getClientRegistration());
    if (converter != null) {
      return converter.convert(oauth2User.getAttributes());
    }
    return DEFAULT_CLAIM_TYPE_CONVERTER.convert(oauth2User.getAttributes());
  }

  private boolean shouldRetrieveUserInfo(OidcUserRequest userRequest) {
    // Auto-disabled if UserInfo Endpoint URI is not provided
    ProviderDetails providerDetails = userRequest.getClientRegistration().getProviderDetails();
    if (!StringUtils.hasLength(providerDetails.getUserInfoEndpoint().getUri())) {
      return false;
    }
    // The Claims requested by the profile, email, address, and phone scope values
    // are returned from the UserInfo Endpoint (as described in Section 5.3.2),
    // when a response_type value is used that results in an Access Token being
    // issued.
    // However, when no Access Token is issued, which is the case for the
    // response_type=id_token,
    // the resulting Claims are returned in the ID Token.
    // The Authorization Code Grant Flow, which is response_type=code, results in an
    // Access Token being issued.
    if (AuthorizationGrantType.AUTHORIZATION_CODE.equals(
        userRequest.getClientRegistration().getAuthorizationGrantType())) {
      // Return true if there is at least one match between the authorized scope(s)
      // and accessible scope(s)
      //
      // Also return true if authorized scope(s) is empty, because the provider has
      // not indicated which scopes are accessible via the access token
      // @formatter:off
      return this.accessibleScopes.isEmpty()
          || CollectionUtils.isEmpty(userRequest.getAccessToken().getScopes())
          || CollectionUtils.containsAny(
              userRequest.getAccessToken().getScopes(), this.accessibleScopes);
      // @formatter:on
    }
    return false;
  }

//  /**
//   * Sets the {@link OAuth2UserService} used when requesting the user info resource.
//   *
//   * @param oauth2UserService the {@link OAuth2UserService} used when requesting the user info
//   *     resource.
//   * @since 5.1
//   */
//  public final void setOauth2UserService(
//      OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService) {
//    Assert.notNull(oauth2UserService, "oauth2UserService cannot be null");
//    this.oauth2UserService = oauth2UserService;
//  }

  /**
   * Sets the factory that provides a {@link Converter} used for type conversion of claim values for
   * an {@link OidcUserInfo}. The default is {@link ClaimTypeConverter} for all {@link
   * ClientRegistration clients}.
   *
   * @param claimTypeConverterFactory the factory that provides a {@link Converter} used for type
   *     conversion of claim values for a specific {@link ClientRegistration client}
   * @since 5.2
   */
  public final void setClaimTypeConverterFactory(
      Function<ClientRegistration, Converter<Map<String, Object>, Map<String, Object>>>
          claimTypeConverterFactory) {
    Assert.notNull(claimTypeConverterFactory, "claimTypeConverterFactory cannot be null");
    this.claimTypeConverterFactory = claimTypeConverterFactory;
  }

  /**
   * Sets the scope(s) that allow access to the user info resource. The default is {@link
   * OidcScopes#PROFILE profile}, {@link OidcScopes#EMAIL email}, {@link OidcScopes#ADDRESS address}
   * and {@link OidcScopes#PHONE phone}. The scope(s) are checked against the "granted" scope(s)
   * associated to the {@link OidcUserRequest#getAccessToken() access token} to determine if the
   * user info resource is accessible or not. If there is at least one match, the user info resource
   * will be requested, otherwise it will not.
   *
   * @param accessibleScopes the scope(s) that allow access to the user info resource
   * @since 5.2
   */
  @Deprecated(since = "6.3", forRemoval = true)
  public final void setAccessibleScopes(Set<String> accessibleScopes) {
    Assert.notNull(accessibleScopes, "accessibleScopes cannot be null");
    this.accessibleScopes = accessibleScopes;
  }

  public OAuth2User __loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    Assert.notNull(userRequest, "userRequest cannot be null");
    String userNameAttributeName = getUserNameAttributeName(userRequest);
    RequestEntity<?> request = this.requestEntityConverter.convert(userRequest);
    ResponseEntity<Map<String, Object>> response = getResponse(userRequest, request);
    OAuth2AccessToken token = userRequest.getAccessToken();
    Map<String, Object> attributes = this.attributesConverter.convert(userRequest).convert(response.getBody());
    Collection<GrantedAuthority> authorities = getAuthorities(token, attributes, userNameAttributeName);
    return new DefaultOAuth2User(authorities, attributes, userNameAttributeName);
  }


  public OidcUser _loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
    Assert.notNull(userRequest, "userRequest cannot be null");
    OidcUserInfo userInfo = null;
    if (shouldRetrieveUserInfo(userRequest)) {
      OAuth2User oauth2User = __loadUser(userRequest);
      Map<String, Object> claims = getClaims(userRequest, oauth2User);
      userInfo = new OidcUserInfo(claims);
      // https://openid.net/specs/openid-connect-core-1_0.html#UserInfoResponse
      // 1) The sub (subject) Claim MUST always be returned in the UserInfo Response
      if (userInfo.getSubject() == null) {
        OAuth2Error oauth2Error = new OAuth2Error(INVALID_USER_INFO_RESPONSE_ERROR_CODE);
        throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
      }
      // 2) Due to the possibility of token substitution attacks (see Section
      // 16.11),
      // the UserInfo Response is not guaranteed to be about the End-User
      // identified by the sub (subject) element of the ID Token.
      // The sub Claim in the UserInfo Response MUST be verified to exactly match
      // the sub Claim in the ID Token; if they do not match,
      // the UserInfo Response values MUST NOT be used.
      if (!userInfo.getSubject().equals(userRequest.getIdToken().getSubject())) {
        OAuth2Error oauth2Error = new OAuth2Error(INVALID_USER_INFO_RESPONSE_ERROR_CODE);
        throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
      }
    }

    return getUser(userRequest, userInfo);
  }

  static OidcUser getUser(OidcUserRequest userRequest, OidcUserInfo userInfo) {
    Set<GrantedAuthority> authorities = new LinkedHashSet<>();
    ClientRegistration.ProviderDetails providerDetails =
        userRequest.getClientRegistration().getProviderDetails();
    String userNameAttributeName = providerDetails.getUserInfoEndpoint().getUserNameAttributeName();
    if (StringUtils.hasText(userNameAttributeName)) {
      authorities.add(
          new OidcUserAuthority(userRequest.getIdToken(), userInfo, userNameAttributeName));
    } else {
      authorities.add(new OidcUserAuthority(userRequest.getIdToken(), userInfo));
    }
    OAuth2AccessToken token = userRequest.getAccessToken();
    for (String scope : token.getScopes()) {
      authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
    }
    if (StringUtils.hasText(userNameAttributeName)) {
      return new DefaultOidcUser(
          authorities, userRequest.getIdToken(), userInfo, userNameAttributeName);
    }
    return new DefaultOidcUser(authorities, userRequest.getIdToken(), userInfo);
  }

  @Override
  public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {

//    this.oauth2UserService = new DefaultOAuth2UserService();

    OidcUser oidcUser = _loadUser(userRequest);

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


  private ResponseEntity<Map<String, Object>> getResponse(OAuth2UserRequest userRequest, RequestEntity<?> request) {
    try {
      return this.restOperations.exchange(request, PARAMETERIZED_RESPONSE_TYPE);
    }
    catch (OAuth2AuthorizationException ex) {
      OAuth2Error oauth2Error = ex.getError();
      StringBuilder errorDetails = new StringBuilder();
      errorDetails.append("Error details: [");
      errorDetails.append("UserInfo Uri: ")
          .append(userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri());
      errorDetails.append(", Error Code: ").append(oauth2Error.getErrorCode());
      if (oauth2Error.getDescription() != null) {
        errorDetails.append(", Error Description: ").append(oauth2Error.getDescription());
      }
      errorDetails.append("]");
      oauth2Error = new OAuth2Error(INVALID_USER_INFO_RESPONSE_ERROR_CODE,
          "An error occurred while attempting to retrieve the UserInfo Resource: " + errorDetails.toString(),
          null);
      throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString(), ex);
    }
    catch (UnknownContentTypeException ex) {
      String errorMessage = "An error occurred while attempting to retrieve the UserInfo Resource from '"
          + userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri()
          + "': response contains invalid content type '" + ex.getContentType().toString() + "'. "
          + "The UserInfo Response should return a JSON object (content type 'application/json') "
          + "that contains a collection of name and value pairs of the claims about the authenticated End-User. "
          + "Please ensure the UserInfo Uri in UserInfoEndpoint for Client Registration '"
          + userRequest.getClientRegistration().getRegistrationId() + "' conforms to the UserInfo Endpoint, "
          + "as defined in OpenID Connect 1.0: 'https://openid.net/specs/openid-connect-core-1_0.html#UserInfo'";
      OAuth2Error oauth2Error = new OAuth2Error(INVALID_USER_INFO_RESPONSE_ERROR_CODE, errorMessage, null);
      throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString(), ex);
    }
    catch (RestClientException ex) {
      OAuth2Error oauth2Error = new OAuth2Error(INVALID_USER_INFO_RESPONSE_ERROR_CODE,
          "An error occurred while attempting to retrieve the UserInfo Resource: " + ex.getMessage(), null);
      throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString(), ex);
    }
  }

  private String getUserNameAttributeName(OAuth2UserRequest userRequest) {
    if (!StringUtils
        .hasText(userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri())) {
      OAuth2Error oauth2Error = new OAuth2Error(MISSING_USER_INFO_URI_ERROR_CODE,
          "Missing required UserInfo Uri in UserInfoEndpoint for Client Registration: "
              + userRequest.getClientRegistration().getRegistrationId(),
          null);
      throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
    }
    String userNameAttributeName = userRequest.getClientRegistration()
        .getProviderDetails()
        .getUserInfoEndpoint()
        .getUserNameAttributeName();
    if (!StringUtils.hasText(userNameAttributeName)) {
      OAuth2Error oauth2Error = new OAuth2Error(MISSING_USER_NAME_ATTRIBUTE_ERROR_CODE,
          "Missing required \"user name\" attribute name in UserInfoEndpoint for Client Registration: "
              + userRequest.getClientRegistration().getRegistrationId(),
          null);
      throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
    }
    return userNameAttributeName;
  }

  private Collection<GrantedAuthority> getAuthorities(OAuth2AccessToken token, Map<String, Object> attributes,
      String userNameAttributeName) {
    Collection<GrantedAuthority> authorities = new LinkedHashSet<>();
    authorities.add(new OAuth2UserAuthority(attributes, userNameAttributeName));
    for (String authority : token.getScopes()) {
      authorities.add(new SimpleGrantedAuthority("SCOPE_" + authority));
    }
    return authorities;
  }
}
