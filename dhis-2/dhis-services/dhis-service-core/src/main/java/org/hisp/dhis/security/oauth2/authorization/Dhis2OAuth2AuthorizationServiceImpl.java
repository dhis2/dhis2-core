/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.security.oauth2.authorization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.security.oauth2.client.Dhis2OAuth2Client;
import org.hisp.dhis.security.oauth2.client.Dhis2OAuth2ClientService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.SystemUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2DeviceCode;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.OAuth2UserCode;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * DHIS2 implementation of Spring Authorization Server's OAuth2AuthorizationService that uses
 * HibernateOAuth2AuthorizationStore for persistence.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Service
public class Dhis2OAuth2AuthorizationServiceImpl
    implements Dhis2OAuth2AuthorizationService, OAuth2AuthorizationService {
  private final Dhis2OAuth2AuthorizationStore authorizationStore;
  private final Dhis2OAuth2ClientService clientRepository;

  private final UserService userService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  private static final String UUID_REGEX =
      "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
  private static final Pattern UUID_PATTERN = Pattern.compile(UUID_REGEX);

  public Dhis2OAuth2AuthorizationServiceImpl(
      Dhis2OAuth2AuthorizationStore authorizationStore,
      Dhis2OAuth2ClientService clientRepository,
      UserService userService) {
    Assert.notNull(authorizationStore, "authorizationStore cannot be null");
    Assert.notNull(clientRepository, "clientRepository cannot be null");
    Assert.notNull(userService, "userService cannot be null");
    this.authorizationStore = authorizationStore;
    this.clientRepository = clientRepository;
    this.userService = userService;

    // Configure Jackson mapper with required modules
    ClassLoader classLoader = Dhis2OAuth2AuthorizationServiceImpl.class.getClassLoader();
    List<com.fasterxml.jackson.databind.Module> securityModules =
        SecurityJackson2Modules.getModules(classLoader);
    this.objectMapper.registerModules(securityModules);
    this.objectMapper.registerModule(new OAuth2AuthorizationServerJackson2Module());

    this.objectMapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  @Transactional
  @Override
  public void save(OAuth2Authorization authorization) {
    Assert.notNull(authorization, "authorization cannot be null");

    Dhis2OAuth2Authorization entity = toEntity(authorization);
    Dhis2OAuth2Authorization existing = this.authorizationStore.getByUidNoAcl(entity.getUid());
    if (existing != null) {
      log.info("Updating existing authorization with id: " + authorization.getId());
      entity.setId(existing.getId());
      this.authorizationStore.merge(entity, new SystemUser());
    } else {
      log.info("Creating new authorization with id: " + authorization.getId());

      Authentication authentication = CurrentUserUtil.getAuthentication();
      if (authentication.getPrincipal() instanceof Jwt jwt) {
        String username = jwt.getClaimAsString("sub");
        User user = userService.getUserByUsername(username);
        UserDetails userDetails = UserDetails.fromUserDontLoadOrgUnits(user);
        this.authorizationStore.save(entity, userDetails, true);
      } else if (authentication instanceof OAuth2ClientAuthenticationToken clientAuth) {
        String clientId = (String) clientAuth.getPrincipal();
        Dhis2OAuth2Client registeredClient =
            this.clientRepository.getAsDhis2OAuth2ClientByClientId(clientId);
        User owner = registeredClient.getCreatedBy();
        UserDetails entityUserDetails = UserDetails.fromUserDontLoadOrgUnits(owner);
        this.authorizationStore.save(entity, entityUserDetails, true);
      } else {
        this.authorizationStore.save(entity);
      }
    }
  }

  @Transactional
  @Override
  public void remove(OAuth2Authorization authorization) {
    Assert.notNull(authorization, "authorization cannot be null");
    this.authorizationStore.deleteByUID(authorization.getId());
  }

  @Transactional
  @Override
  public void delete(String uid) {
    Assert.hasText(uid, "uid cannot be empty");
    this.authorizationStore.deleteByUID(uid);
  }

  @Transactional(readOnly = true)
  @Override
  public List<Dhis2OAuth2Authorization> getAll() {
    return authorizationStore.getAll();
  }

  @Transactional(readOnly = true)
  @Override
  public OAuth2Authorization findById(String id) {
    Assert.hasText(id, "id cannot be empty");
    Dhis2OAuth2Authorization entity = this.authorizationStore.getByUid(id);
    return entity != null ? toObject(entity) : null;
  }

  @Transactional(readOnly = true)
  @Override
  public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
    Assert.hasText(token, "token cannot be empty");

    Dhis2OAuth2Authorization entity = null;

    if (tokenType == null) {
      entity = this.authorizationStore.getByToken(token);
    } else {
      String tokenTypeValue = tokenType.getValue();
      entity =
          switch (tokenTypeValue) {
            case OAuth2ParameterNames.STATE -> this.authorizationStore.getByState(token);
            case OAuth2ParameterNames.CODE -> this.authorizationStore.getByAuthorizationCode(token);
            case OAuth2ParameterNames.ACCESS_TOKEN ->
                this.authorizationStore.getByAccessToken(token);
            case OAuth2ParameterNames.REFRESH_TOKEN ->
                this.authorizationStore.getByRefreshToken(token);
            case OidcParameterNames.ID_TOKEN -> this.authorizationStore.getByOidcIdToken(token);
            case OAuth2ParameterNames.USER_CODE -> this.authorizationStore.getByUserCode(token);
            case OAuth2ParameterNames.DEVICE_CODE -> this.authorizationStore.getByDeviceCode(token);
            default -> entity;
          };
    }

    return entity != null ? toObject(entity) : null;
  }

  /**
   * Converts a DHIS2 OAuth2Authorization entity to Spring's OAuth2Authorization domain object.
   *
   * @param entity The DHIS2 OAuth2Authorization entity
   * @return The Spring OAuth2Authorization
   */
  private OAuth2Authorization toObject(Dhis2OAuth2Authorization entity) {
    RegisteredClient registeredClient =
        this.clientRepository.findByUID(entity.getRegisteredClientId());
    if (registeredClient == null) {
      throw new DataRetrievalFailureException(
          "The RegisteredClient with id '"
              + entity.getRegisteredClientId()
              + "' was not found in the RegisteredClientRepository.");
    }

    OAuth2Authorization.Builder builder =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .id(entity.getUid())
            .principalName(entity.getPrincipalName())
            .authorizationGrantType(
                resolveAuthorizationGrantType(entity.getAuthorizationGrantType()))
            .authorizedScopes(StringUtils.commaDelimitedListToSet(entity.getAuthorizedScopes()))
            .attributes(attributes -> attributes.putAll(parseMap(entity.getAttributes())));

    if (entity.getState() != null) {
      builder.attribute(OAuth2ParameterNames.STATE, entity.getState());
    }

    if (entity.getAuthorizationCodeValue() != null) {
      OAuth2AuthorizationCode authorizationCode =
          new OAuth2AuthorizationCode(
              entity.getAuthorizationCodeValue(),
              entity.getAuthorizationCodeIssuedAt() != null
                  ? entity.getAuthorizationCodeIssuedAt().toInstant()
                  : null,
              entity.getAuthorizationCodeExpiresAt() != null
                  ? entity.getAuthorizationCodeExpiresAt().toInstant()
                  : null);
      builder.token(
          authorizationCode,
          metadata -> metadata.putAll(parseMap(entity.getAuthorizationCodeMetadata())));
    }

    if (entity.getAccessTokenValue() != null) {
      OAuth2AccessToken accessToken =
          new OAuth2AccessToken(
              OAuth2AccessToken.TokenType.BEARER,
              entity.getAccessTokenValue(),
              entity.getAccessTokenIssuedAt() != null
                  ? entity.getAccessTokenIssuedAt().toInstant()
                  : null,
              entity.getAccessTokenExpiresAt() != null
                  ? entity.getAccessTokenExpiresAt().toInstant()
                  : null,
              StringUtils.commaDelimitedListToSet(entity.getAccessTokenScopes()));
      builder.token(
          accessToken, metadata -> metadata.putAll(parseMap(entity.getAccessTokenMetadata())));
    }

    if (entity.getRefreshTokenValue() != null) {
      OAuth2RefreshToken refreshToken =
          new OAuth2RefreshToken(
              entity.getRefreshTokenValue(),
              entity.getRefreshTokenIssuedAt() != null
                  ? entity.getRefreshTokenIssuedAt().toInstant()
                  : null,
              entity.getRefreshTokenExpiresAt() != null
                  ? entity.getRefreshTokenExpiresAt().toInstant()
                  : null);
      builder.token(
          refreshToken, metadata -> metadata.putAll(parseMap(entity.getRefreshTokenMetadata())));
    }

    if (entity.getOidcIdTokenValue() != null) {
      OidcIdToken idToken =
          new OidcIdToken(
              entity.getOidcIdTokenValue(),
              entity.getOidcIdTokenIssuedAt() != null
                  ? entity.getOidcIdTokenIssuedAt().toInstant()
                  : null,
              entity.getOidcIdTokenExpiresAt() != null
                  ? entity.getOidcIdTokenExpiresAt().toInstant()
                  : null,
              parseMap(entity.getOidcIdTokenClaims()));
      builder.token(
          idToken, metadata -> metadata.putAll(parseMap(entity.getOidcIdTokenMetadata())));
    }

    if (entity.getUserCodeValue() != null) {
      OAuth2UserCode userCode =
          new OAuth2UserCode(
              entity.getUserCodeValue(),
              entity.getUserCodeIssuedAt() != null
                  ? entity.getUserCodeIssuedAt().toInstant()
                  : null,
              entity.getUserCodeExpiresAt() != null
                  ? entity.getUserCodeExpiresAt().toInstant()
                  : null);
      builder.token(userCode, metadata -> metadata.putAll(parseMap(entity.getUserCodeMetadata())));
    }

    if (entity.getDeviceCodeValue() != null) {
      OAuth2DeviceCode deviceCode =
          new OAuth2DeviceCode(
              entity.getDeviceCodeValue(),
              entity.getDeviceCodeIssuedAt() != null
                  ? entity.getDeviceCodeIssuedAt().toInstant()
                  : null,
              entity.getDeviceCodeExpiresAt() != null
                  ? entity.getDeviceCodeExpiresAt().toInstant()
                  : null);
      builder.token(
          deviceCode, metadata -> metadata.putAll(parseMap(entity.getDeviceCodeMetadata())));
    }

    return builder.build();
  }

  /**
   * Converts a Spring OAuth2Authorization domain object to a DHIS2 OAuth2Authorization entity.
   *
   * @param authorization The Spring OAuth2Authorization
   * @return The DHIS2 OAuth2Authorization entity
   */
  private Dhis2OAuth2Authorization toEntity(OAuth2Authorization authorization) {
    Dhis2OAuth2Authorization entity = new Dhis2OAuth2Authorization();
    Dhis2OAuth2Authorization existingEntity =
        this.authorizationStore.getByUidNoAcl(authorization.getId());
    if (existingEntity != null) {
      entity.setUid(existingEntity.getUid());
      entity.setCreated(existingEntity.getCreated());
    } else {
      if (authorization.getId() != null) {
        boolean isUUID = UUID_PATTERN.matcher(authorization.getId()).matches();
        if (isUUID) {
          entity.setUid(CodeGenerator.generateUid());
        } else {
          entity.setUid(authorization.getId());
        }
      } else {
        entity.setUid(CodeGenerator.generateUid());
      }
    }

    entity.setRegisteredClientId(authorization.getRegisteredClientId());
    entity.setPrincipalName(authorization.getPrincipalName());
    entity.setAuthorizationGrantType(authorization.getAuthorizationGrantType().getValue());
    entity.setAuthorizedScopes(
        StringUtils.collectionToCommaDelimitedString(authorization.getAuthorizedScopes()));
    entity.setAttributes(writeMap(authorization.getAttributes()));
    entity.setState(authorization.getAttribute(OAuth2ParameterNames.STATE));

    OAuth2Authorization.Token<OAuth2AuthorizationCode> authorizationCode =
        authorization.getToken(OAuth2AuthorizationCode.class);
    setTokenValues(
        authorizationCode,
        entity::setAuthorizationCodeValue,
        entity::setAuthorizationCodeIssuedAt,
        entity::setAuthorizationCodeExpiresAt,
        entity::setAuthorizationCodeMetadata);

    OAuth2Authorization.Token<OAuth2AccessToken> accessToken =
        authorization.getToken(OAuth2AccessToken.class);
    setTokenValues(
        accessToken,
        entity::setAccessTokenValue,
        entity::setAccessTokenIssuedAt,
        entity::setAccessTokenExpiresAt,
        entity::setAccessTokenMetadata);
    if (accessToken != null && accessToken.getToken().getScopes() != null) {
      entity.setAccessTokenScopes(
          StringUtils.collectionToCommaDelimitedString(accessToken.getToken().getScopes()));
    }

    OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken =
        authorization.getToken(OAuth2RefreshToken.class);
    setTokenValues(
        refreshToken,
        entity::setRefreshTokenValue,
        entity::setRefreshTokenIssuedAt,
        entity::setRefreshTokenExpiresAt,
        entity::setRefreshTokenMetadata);

    OAuth2Authorization.Token<OidcIdToken> oidcIdToken = authorization.getToken(OidcIdToken.class);
    setTokenValues(
        oidcIdToken,
        entity::setOidcIdTokenValue,
        entity::setOidcIdTokenIssuedAt,
        entity::setOidcIdTokenExpiresAt,
        entity::setOidcIdTokenMetadata);
    if (oidcIdToken != null) {
      entity.setOidcIdTokenClaims(writeMap(oidcIdToken.getClaims()));
    }

    OAuth2Authorization.Token<OAuth2UserCode> userCode =
        authorization.getToken(OAuth2UserCode.class);
    setTokenValues(
        userCode,
        entity::setUserCodeValue,
        entity::setUserCodeIssuedAt,
        entity::setUserCodeExpiresAt,
        entity::setUserCodeMetadata);

    OAuth2Authorization.Token<OAuth2DeviceCode> deviceCode =
        authorization.getToken(OAuth2DeviceCode.class);
    setTokenValues(
        deviceCode,
        entity::setDeviceCodeValue,
        entity::setDeviceCodeIssuedAt,
        entity::setDeviceCodeExpiresAt,
        entity::setDeviceCodeMetadata);

    return entity;
  }

  /** Helper method to set token values on entity */
  private <T extends OAuth2Token> void setTokenValues(
      OAuth2Authorization.Token<T> token,
      Consumer<String> tokenValueConsumer,
      Consumer<Date> issuedAtConsumer,
      Consumer<Date> expiresAtConsumer,
      Consumer<String> metadataConsumer) {
    if (token != null) {
      OAuth2Token oAuth2Token = token.getToken();
      tokenValueConsumer.accept(oAuth2Token.getTokenValue());
      issuedAtConsumer.accept(
          oAuth2Token.getIssuedAt() != null ? Date.from(oAuth2Token.getIssuedAt()) : null);
      expiresAtConsumer.accept(
          oAuth2Token.getExpiresAt() != null ? Date.from(oAuth2Token.getExpiresAt()) : null);
      metadataConsumer.accept(writeMap(token.getMetadata()));
    }
  }

  /**
   * Parses a JSON string into a Map.
   *
   * @param data The JSON string
   * @return The parsed Map
   */
  private Map<String, Object> parseMap(String data) {
    if (data == null || data.isBlank()) {
      return Map.of();
    }

    try {
      return this.objectMapper.readValue(data, new TypeReference<>() {});
    } catch (Exception ex) {
      throw new IllegalArgumentException("Failed to parse JSON data: " + ex.getMessage(), ex);
    }
  }

  /**
   * Converts a Map to a JSON string.
   *
   * @param data The Map to convert
   * @return The JSON string
   */
  private String writeMap(Map<String, Object> data) {
    try {
      return this.objectMapper.writeValueAsString(data);
    } catch (Exception ex) {
      throw new IllegalArgumentException("Failed to write JSON data: " + ex.getMessage(), ex);
    }
  }

  /**
   * Resolves the AuthorizationGrantType from a string value.
   *
   * @param authorizationGrantType The string value
   * @return The corresponding AuthorizationGrantType
   */
  private static AuthorizationGrantType resolveAuthorizationGrantType(
      @Nonnull String authorizationGrantType) {
    if (AuthorizationGrantType.AUTHORIZATION_CODE.getValue().equals(authorizationGrantType)) {
      return AuthorizationGrantType.AUTHORIZATION_CODE;
    } else if (AuthorizationGrantType.CLIENT_CREDENTIALS
        .getValue()
        .equals(authorizationGrantType)) {
      return AuthorizationGrantType.CLIENT_CREDENTIALS;
    } else if (AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(authorizationGrantType)) {
      return AuthorizationGrantType.REFRESH_TOKEN;
    } else if (AuthorizationGrantType.DEVICE_CODE.getValue().equals(authorizationGrantType)) {
      return AuthorizationGrantType.DEVICE_CODE;
    }
    return new AuthorizationGrantType(authorizationGrantType); // Custom authorization grant type
  }
}
