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
package org.hisp.dhis.security.oauth2.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.NonTransactional;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * DHIS2 implementation of Spring Authorization Server's RegisteredClientRepository that uses
 * HibernateOAuth2ClientStore for persistence.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Service
public class Dhis2OAuth2ClientServiceImpl
    implements Dhis2OAuth2ClientService, RegisteredClientRepository {

  private final Dhis2OAuth2ClientStore clientStore;
  private final UserService userService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public Dhis2OAuth2ClientServiceImpl(Dhis2OAuth2ClientStore clientStore, UserService userService) {
    Assert.notNull(clientStore, "clientStore cannot be null");
    this.clientStore = clientStore;
    this.userService = userService;

    // Configure Jackson mapper with the required modules
    ClassLoader classLoader = Dhis2OAuth2ClientServiceImpl.class.getClassLoader();
    List<com.fasterxml.jackson.databind.Module> securityModules =
        SecurityJackson2Modules.getModules(classLoader);
    this.objectMapper.registerModules(securityModules);
    this.objectMapper.registerModule(new OAuth2AuthorizationServerJackson2Module());
  }

  @Transactional
  @Override
  public void save(RegisteredClient registeredClient) {
    Objects.requireNonNull(registeredClient, "registeredClient cannot be null");
    Dhis2OAuth2Client client = toEntity(registeredClient);

    // Check if we are doing a DCR request, then we are authenticated with the IAT token.
    if (CurrentUserUtil.getAuthentication().getPrincipal() instanceof Jwt jwt) {
      String username = jwt.getClaimAsString("sub");
      User user = userService.getUserByUsername(username);
      UserDetails userDetails = UserDetails.fromUserDontLoadOrgUnits(user);
      // Save with created by as 'sub'/username from IAT token
      this.clientStore.save(client, userDetails, true);
    } else {
      this.clientStore.save(client);
    }
  }

  @Transactional
  @Override
  public void save(RegisteredClient registeredClient, UserDetails userDetails) {
    Assert.notNull(registeredClient, "registeredClient cannot be null");
    Dhis2OAuth2Client client = toEntity(registeredClient);
    this.clientStore.save(client, userDetails, false);
  }

  @Transactional(readOnly = true)
  @Override
  @CheckForNull
  public RegisteredClient findByUID(String uid) {
    Assert.hasText(uid, "uid cannot be empty");
    Dhis2OAuth2Client client = this.clientStore.getByUidNoAcl(uid);
    return client != null ? toObject(client) : null;
  }

  @Transactional(readOnly = true)
  @Override
  @CheckForNull
  public RegisteredClient findById(String id) {
    Assert.hasText(id, "id cannot be empty");
    Dhis2OAuth2Client client = this.clientStore.getByClientId(id);
    return client != null ? toObject(client) : null;
  }

  @Transactional(readOnly = true)
  @Override
  @CheckForNull
  public RegisteredClient findByClientId(String clientId) {
    Assert.hasText(clientId, "clientId cannot be empty");
    Dhis2OAuth2Client client = this.clientStore.getByClientId(clientId);
    return client != null ? toObject(client) : null;
  }

  @Transactional(readOnly = true)
  @Override
  @CheckForNull
  public Dhis2OAuth2Client getAsDhis2OAuth2ClientByClientId(String clientId) {
    Assert.hasText(clientId, "clientId cannot be empty");
    return this.clientStore.getByClientId(clientId);
  }

  @Override
  @NonTransactional
  @Nonnull
  public RegisteredClient toObject(Dhis2OAuth2Client client) {
    Set<String> clientAuthenticationMethods =
        StringUtils.commaDelimitedListToSet(client.getClientAuthenticationMethods());
    Set<String> authorizationGrantTypes =
        StringUtils.commaDelimitedListToSet(client.getAuthorizationGrantTypes());
    Set<String> redirectUris = StringUtils.commaDelimitedListToSet(client.getRedirectUris());
    Set<String> postLogoutRedirectUris =
        StringUtils.commaDelimitedListToSet(client.getPostLogoutRedirectUris());
    Set<String> clientScopes = StringUtils.commaDelimitedListToSet(client.getScopes());

    RegisteredClient.Builder builder =
        RegisteredClient.withId(client.getUid())
            .clientId(client.getClientId())
            .clientIdIssuedAt(
                client.getClientIdIssuedAt() != null
                    ? client.getClientIdIssuedAt().toInstant()
                    : null)
            .clientSecret(client.getClientSecret())
            .clientSecretExpiresAt(
                client.getClientSecretExpiresAt() != null
                    ? client.getClientSecretExpiresAt().toInstant()
                    : null)
            .clientName(client.getName())
            .clientAuthenticationMethods(
                authenticationMethods ->
                    clientAuthenticationMethods.forEach(
                        authenticationMethod ->
                            authenticationMethods.add(
                                resolveClientAuthenticationMethod(authenticationMethod))))
            .authorizationGrantTypes(
                (grantTypes) ->
                    authorizationGrantTypes.forEach(
                        grantType -> grantTypes.add(resolveAuthorizationGrantType(grantType))))
            .redirectUris(uris -> uris.addAll(redirectUris))
            .postLogoutRedirectUris(uris -> uris.addAll(postLogoutRedirectUris))
            .scopes(scopes -> scopes.addAll(clientScopes));

    Map<String, Object> clientSettingsMap = parseMap(client.getClientSettings());
    builder.clientSettings(ClientSettings.withSettings(clientSettingsMap).build());
    Map<String, Object> tokenSettingsMap = parseMap(client.getTokenSettings());
    builder.tokenSettings(TokenSettings.withSettings(tokenSettingsMap).build());

    return builder.build();
  }

  @Override
  @NonTransactional
  @Nonnull
  public Dhis2OAuth2Client toEntity(RegisteredClient registeredClient) {
    List<String> clientAuthenticationMethods =
        new ArrayList<>(registeredClient.getClientAuthenticationMethods().size());
    registeredClient
        .getClientAuthenticationMethods()
        .forEach(
            clientAuthenticationMethod ->
                clientAuthenticationMethods.add(clientAuthenticationMethod.getValue()));

    List<String> authorizationGrantTypes =
        new ArrayList<>(registeredClient.getAuthorizationGrantTypes().size());
    registeredClient
        .getAuthorizationGrantTypes()
        .forEach(
            authorizationGrantType ->
                authorizationGrantTypes.add(authorizationGrantType.getValue()));

    Dhis2OAuth2Client entity = new Dhis2OAuth2Client();

    // Handle case when we're creating a new client vs updating existing
    Dhis2OAuth2Client existingClient =
        this.clientStore.getByClientId(registeredClient.getClientId());
    if (existingClient != null) {
      entity.setUid(existingClient.getUid());
      entity.setCreated(existingClient.getCreated());
    } else if (registeredClient.getId() != null
        && CodeGenerator.isValidUid(registeredClient.getId())) {
      entity.setUid(registeredClient.getId());
    } else {
      entity.setUid(CodeGenerator.generateUid());
    }

    entity.setName(registeredClient.getClientName());
    entity.setClientId(registeredClient.getClientId());
    entity.setClientIdIssuedAt(
        registeredClient.getClientIdIssuedAt() != null
            ? Date.from(registeredClient.getClientIdIssuedAt())
            : null);
    entity.setClientSecret(registeredClient.getClientSecret());
    entity.setClientSecretExpiresAt(
        registeredClient.getClientSecretExpiresAt() != null
            ? Date.from(registeredClient.getClientSecretExpiresAt())
            : null);
    entity.setClientAuthenticationMethods(
        StringUtils.collectionToCommaDelimitedString(clientAuthenticationMethods));
    entity.setAuthorizationGrantTypes(
        StringUtils.collectionToCommaDelimitedString(authorizationGrantTypes));
    entity.setRedirectUris(
        StringUtils.collectionToCommaDelimitedString(registeredClient.getRedirectUris()));
    entity.setPostLogoutRedirectUris(
        StringUtils.collectionToCommaDelimitedString(registeredClient.getPostLogoutRedirectUris()));
    entity.setScopes(StringUtils.collectionToCommaDelimitedString(registeredClient.getScopes()));

    Map<String, Object> settings = registeredClient.getClientSettings().getSettings();
    entity.setClientSettings(writeMap(settings));
    String tokenSettings = writeMap(registeredClient.getTokenSettings().getSettings());
    entity.setTokenSettings(tokenSettings);

    return entity;
  }

  private Map<String, Object> parseMap(String data) {
    if (data == null || data.isBlank()) {
      return Map.of();
    }
    try {
      return this.objectMapper.readValue(data, new TypeReference<Map<String, Object>>() {});
    } catch (Exception ex) {
      throw new IllegalArgumentException("Failed to parse JSON data: " + ex.getMessage(), ex);
    }
  }

  @Override
  @Nonnull
  public String writeMap(Map<String, Object> data) {
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

  /**
   * Resolves the ClientAuthenticationMethod from a string value.
   *
   * @param clientAuthenticationMethod The string value
   * @return The corresponding ClientAuthenticationMethod
   */
  private static ClientAuthenticationMethod resolveClientAuthenticationMethod(
      @Nonnull String clientAuthenticationMethod) {
    if (ClientAuthenticationMethod.CLIENT_SECRET_BASIC
        .getValue()
        .equals(clientAuthenticationMethod)) {
      return ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
    } else if (ClientAuthenticationMethod.CLIENT_SECRET_POST
        .getValue()
        .equals(clientAuthenticationMethod)) {
      return ClientAuthenticationMethod.CLIENT_SECRET_POST;
    } else if (ClientAuthenticationMethod.NONE.getValue().equals(clientAuthenticationMethod)) {
      return ClientAuthenticationMethod.NONE;
    }
    return new ClientAuthenticationMethod(
        clientAuthenticationMethod); // Custom client authentication method
  }

  @Override
  public List<Dhis2OAuth2Client> getAll() {
    return clientStore.getAll();
  }
}
