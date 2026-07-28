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

import static org.hisp.dhis.security.oauth2.OAuth2Constants.ALLOWED_CLIENT_SCOPES;
import static org.hisp.dhis.security.oauth2.OAuth2Constants.CLIENT_NAME_MAX_LENGTH;
import static org.hisp.dhis.security.oauth2.OAuth2Constants.SYSTEM_REGISTRAR_CLIENTID;
import static org.hisp.dhis.security.oauth2.OAuth2Constants.isAllowedClientScope;
import static org.hisp.dhis.security.oauth2.OAuth2Constants.isReservedScope;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.NonTransactional;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.security.oauth2.OAuth2GrantTypes;
import org.hisp.dhis.setting.SystemSettingsService;
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
import org.springframework.security.oauth2.server.authorization.settings.ConfigurationSettingNames;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Spring-bean implementation of {@link Dhis2OAuth2ClientService}. Also implements Spring
 * Authorization Server's {@link RegisteredClientRepository}, so Spring AS reads clients through
 * this bean on every token-endpoint authentication. Persistence is delegated to {@link
 * Dhis2OAuth2ClientStore} ({@link HibernateDhis2OAuth2ClientStore}).
 *
 * <p>The Jackson {@link ObjectMapper} is preloaded with {@link SecurityJackson2Modules} and {@link
 * OAuth2AuthorizationServerJackson2Module} so the {@code clientSettings} / {@code tokenSettings}
 * JSON round-trips correctly against Spring AS's internal types.
 *
 * <p>{@link #save(RegisteredClient)} detects a DCR call by looking for a {@link Jwt} principal in
 * the security context (i.e. the Initial Access Token); when present, the JWT's {@code sub} claim
 * is resolved to a DHIS2 user and recorded as {@code createdBy} on the new client.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Service
public class Dhis2OAuth2ClientServiceImpl
    implements Dhis2OAuth2ClientService, RegisteredClientRepository {

  /**
   * Grant types an admin-facing entry point (REST CRUD, bulk metadata import) is allowed to set on
   * a client. {@code client_credentials} is deliberately excluded; the only legitimate user of it
   * in this deployment is the DCR system registrar, which is created server-side via {@link
   * Dhis2OAuth2ClientStore#save} and bypasses these validators. See {@link #validateGrantTypes} for
   * the one exception that keeps metadata round-trips working.
   */
  private static final Set<AuthorizationGrantType> ALLOWED_ADMIN_GRANT_TYPES =
      Set.of(AuthorizationGrantType.AUTHORIZATION_CODE, AuthorizationGrantType.REFRESH_TOKEN);

  private final Dhis2OAuth2ClientStore clientStore;
  private final UserService userService;
  private final SystemSettingsService systemSettingsService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public Dhis2OAuth2ClientServiceImpl(
      Dhis2OAuth2ClientStore clientStore,
      UserService userService,
      SystemSettingsService systemSettingsService) {
    Assert.notNull(clientStore, "clientStore cannot be null");
    this.clientStore = clientStore;
    this.userService = userService;
    this.systemSettingsService = systemSettingsService;

    // Configure Jackson mapper with the required modules
    ClassLoader classLoader = Dhis2OAuth2ClientServiceImpl.class.getClassLoader();
    List<com.fasterxml.jackson.databind.Module> securityModules =
        SecurityJackson2Modules.getModules(classLoader);
    this.objectMapper.registerModules(securityModules);
    this.objectMapper.registerModule(new OAuth2AuthorizationServerJackson2Module());
  }

  /**
   * {@inheritDoc}
   *
   * <p>If the current security context's principal is a {@link Jwt} (the DCR Initial Access Token),
   * the token's {@code sub} claim is resolved to a DHIS2 user and passed as {@code createdBy} so
   * the new client is owned by the enrolling user.
   */
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
                        grantType -> grantTypes.add(OAuth2GrantTypes.resolve(grantType))))
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

    // Spring's RegisteredClient.Builder.build() defaults clientName to the
    // internal id when not set explicitly, so getClientName() is never null.
    String clientName = registeredClient.getClientName();
    entity.setName(
        clientName != null && !clientName.equals(registeredClient.getId())
            ? clientName
            : registeredClient.getClientId());
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
   * Resolves the {@link ClientAuthenticationMethod} from its string value, returning one of the
   * Spring AS constants when possible and constructing a custom instance otherwise.
   *
   * @param clientAuthenticationMethod the string value
   * @return the corresponding {@link ClientAuthenticationMethod}
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

  // ---------------------------------------------------------------------------
  // Admin-input validation and defaulting
  // ---------------------------------------------------------------------------

  @Override
  public void validateCreate(Dhis2OAuth2Client entity, Consumer<ErrorReport> errors) {
    checkClientIdNotReserved(entity.getClientId(), "create a client with", errors);
    validateGrantTypes(entity, errors);
    validateScopes(entity, errors);
    validateRedirectUris(entity, errors);
    validateSettings(entity, errors);
  }

  @Override
  public void validateUpdate(
      Dhis2OAuth2Client persisted, Dhis2OAuth2Client newEntity, Consumer<ErrorReport> errors) {
    if (persisted == null || !persisted.getClientId().equals(newEntity.getClientId())) {
      checkClientIdNotReserved(newEntity.getClientId(), "rename a client to", errors);
    }
    validateGrantTypes(newEntity, errors);
    validateScopes(newEntity, errors);
    validateRedirectUris(newEntity, errors);
    validateSettings(newEntity, errors);
  }

  @Override
  public void applyCreateDefaults(Dhis2OAuth2Client entity) {
    // Use getRawName() so the field is set even when getName()'s fallback would already return
    // a non-null value derived from clientId.
    if (entity.getRawName() == null || entity.getRawName().isEmpty()) {
      entity.setName(truncateName(entity.getClientId()));
    }
    // PKCE-by-default (PR-H): new admin/metadata-created clients require S256 PKCE on the
    // authorization-code flow. Both flags are set explicitly and deliberately — SAS 7's bare
    // builder defaults are requireProofKey(true) but requireAuthorizationConsent(false), so
    // relying on the builder would silently drop the consent screen for admin clients.
    if (entity.getClientSettings() == null || entity.getClientSettings().isBlank()) {
      ClientSettings defaults =
          ClientSettings.builder().requireAuthorizationConsent(true).requireProofKey(true).build();
      entity.setClientSettings(writeMap(defaults.getSettings()));
    }
    if (entity.getTokenSettings() == null || entity.getTokenSettings().isBlank()) {
      entity.setTokenSettings(writeMap(TokenSettings.builder().build().getSettings()));
    }
  }

  @Override
  public void applyUpdateDefaults(Dhis2OAuth2Client persisted, Dhis2OAuth2Client newEntity) {
    // Caller explicitly sent a non-empty name -> keep it.
    if (newEntity.getRawName() != null && !newEntity.getRawName().isEmpty()) {
      return;
    }
    // Settings UI has no name field; preserve the existing persisted name rather than letting
    // REPLACE merge clobber it.
    if (persisted != null && persisted.getRawName() != null && !persisted.getRawName().isEmpty()) {
      newEntity.setName(persisted.getRawName());
    } else if (newEntity.getClientId() != null) {
      newEntity.setName(truncateName(newEntity.getClientId()));
    }
  }

  @Override
  public Set<AuthorizationGrantType> getAuthorizationGrantTypesSet(Dhis2OAuth2Client entity) {
    String raw = entity.getAuthorizationGrantTypes();
    if (raw == null || raw.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(OAuth2GrantTypes::resolve)
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Reject any attempt to use the reserved DCR system-registrar clientId via admin paths. Without
   * this, an admin could squat the reserved clientId while the authorization server is disabled,
   * and when {@code OAuth2DcrService.init()} later runs DCR would mint Initial Access Tokens
   * through a client whose secret and redirect URIs the squatter controls.
   */
  private static void checkClientIdNotReserved(
      String clientId, String operation, Consumer<ErrorReport> errors) {
    if (SYSTEM_REGISTRAR_CLIENTID.equals(clientId)) {
      errors.accept(
          new ErrorReport(
              Dhis2OAuth2Client.class,
              ErrorCode.E4000,
              "Cannot "
                  + operation
                  + " the reserved DCR system-registrar clientId ("
                  + SYSTEM_REGISTRAR_CLIENTID
                  + ")."));
    }
  }

  /**
   * Enforce the grant-type admin rule: only {@code authorization_code} and {@code refresh_token}
   * are accepted on admin-facing paths. The DCR system registrar is the sole legitimate holder of
   * {@code client_credentials} in this deployment; its re-import from a metadata bundle is allowed
   * through because admin-initiated creates cannot reach this branch (the reserved clientId is
   * rejected earlier by {@link #checkClientIdNotReserved}).
   */
  private void validateGrantTypes(Dhis2OAuth2Client entity, Consumer<ErrorReport> errors) {
    // The DCR system registrar legitimately uses client_credentials; skip the check so a full
    // metadata round-trip can re-import it. Admin-initiated creates can't hit this branch because
    // the reserved clientId is rejected earlier.
    boolean isSystemRegistrar = SYSTEM_REGISTRAR_CLIENTID.equals(entity.getClientId());
    for (AuthorizationGrantType type : getAuthorizationGrantTypesSet(entity)) {
      if (isSystemRegistrar && AuthorizationGrantType.CLIENT_CREDENTIALS.equals(type)) {
        continue;
      }
      if (!ALLOWED_ADMIN_GRANT_TYPES.contains(type)) {
        errors.accept(
            new ErrorReport(
                Dhis2OAuth2Client.class,
                ErrorCode.E4000,
                "Invalid authorization grant type: "
                    + type.getValue()
                    + ". Only authorization_code and refresh_token are allowed."));
      }
    }
  }

  /**
   * Enforce the admin scope policy: reserved scopes ({@code client.create} and anything under the
   * {@code client.} prefix) can never be assigned through admin paths — a client carrying them
   * could mint new clients via {@code /connect/register}. All other scopes must be members of
   * {@link org.hisp.dhis.security.oauth2.OAuth2Constants#ALLOWED_CLIENT_SCOPES}. The DCR system
   * registrar is exempt so a full metadata round-trip can re-import it (same exception as {@link
   * #validateGrantTypes}).
   */
  private void validateScopes(Dhis2OAuth2Client entity, Consumer<ErrorReport> errors) {
    if (SYSTEM_REGISTRAR_CLIENTID.equals(entity.getClientId())) {
      return;
    }
    String raw = entity.getScopes();
    if (raw == null || raw.isBlank()) {
      return;
    }
    for (String scope : raw.split(",")) {
      String trimmed = scope.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      if (isReservedScope(trimmed)) {
        errors.accept(
            new ErrorReport(
                Dhis2OAuth2Client.class,
                ErrorCode.E4000,
                "Scope '"
                    + trimmed
                    + "' is reserved for the system DCR registrar and cannot be assigned to"
                    + " clients."));
      } else if (!isAllowedClientScope(trimmed)) {
        errors.accept(
            new ErrorReport(
                Dhis2OAuth2Client.class,
                ErrorCode.E4000,
                "Invalid scope: '"
                    + trimmed
                    + "'. Allowed scopes are: "
                    + String.join(", ", ALLOWED_CLIENT_SCOPES.stream().sorted().toList())
                    + "."));
      }
    }
  }

  /**
   * Validate admin-supplied {@code clientSettings} / {@code tokenSettings} JSON at write time, so a
   * row that would later fail (or surprise) {@link #toObject} is rejected up front instead of
   * breaking the authorization server at runtime.
   *
   * <p>{@code clientSettings} must parse with the same Jackson configuration {@link #toObject}
   * uses, and must carry both {@code settings.client.require-proof-key} and {@code
   * settings.client.require-authorization-consent} as booleans — Spring AS unboxes these on every
   * authorize call, so a partial map would NPE at runtime. An explicit {@code
   * require-proof-key:false} remains a supported (temporary) opt-out for legacy integrations, but
   * is logged so the downgrade is auditable.
   */
  private void validateSettings(Dhis2OAuth2Client entity, Consumer<ErrorReport> errors) {
    String clientSettings = entity.getClientSettings();
    if (clientSettings != null && !clientSettings.isBlank()) {
      Map<String, Object> settings = tryParseSettings("clientSettings", clientSettings, errors);
      if (settings != null) {
        Boolean proofKey =
            checkRequiredBooleanSetting(
                settings, ConfigurationSettingNames.Client.REQUIRE_PROOF_KEY, errors);
        checkRequiredBooleanSetting(
            settings, ConfigurationSettingNames.Client.REQUIRE_AUTHORIZATION_CONSENT, errors);
        if (Boolean.FALSE.equals(proofKey)) {
          log.warn(
              "OAuth2 client '{}' explicitly opts out of PKCE (require-proof-key=false). This is a"
                  + " temporary escape hatch for legacy integrations; new authorization-code"
                  + " clients should use S256 PKCE.",
              entity.getClientId());
        }
      }
    }
    String tokenSettings = entity.getTokenSettings();
    if (tokenSettings != null && !tokenSettings.isBlank()) {
      tryParseSettings("tokenSettings", tokenSettings, errors);
    }
  }

  /**
   * Parse a settings JSON string with the same mapper {@link #toObject} uses at read time; reports
   * an {@link ErrorCode#E4000} and returns {@code null} when unreadable.
   */
  @CheckForNull
  private Map<String, Object> tryParseSettings(
      String field, String json, Consumer<ErrorReport> errors) {
    try {
      return this.objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    } catch (Exception ex) {
      errors.accept(
          new ErrorReport(
              Dhis2OAuth2Client.class,
              ErrorCode.E4000,
              "Invalid " + field + " JSON: " + ex.getMessage()));
      return null;
    }
  }

  /**
   * Require the given settings key to be present and boolean; reports an error and returns {@code
   * null} otherwise.
   */
  @CheckForNull
  private static Boolean checkRequiredBooleanSetting(
      Map<String, Object> settings, String key, Consumer<ErrorReport> errors) {
    if (settings.get(key) instanceof Boolean value) {
      return value;
    }
    errors.accept(
        new ErrorReport(
            Dhis2OAuth2Client.class,
            ErrorCode.E4000,
            "Invalid clientSettings: setting '" + key + "' must be present and a boolean."));
    return null;
  }

  /**
   * Redirect URI allow-list: http and https are always accepted; any other URI (e.g. a custom
   * scheme like {@code dhis2oauth://oauth}) must match verbatim an entry in the {@code
   * deviceEnrollmentRedirectAllowlist} system setting. Default-to-deny closes the stored-XSS and
   * token-exfiltration surface that Spring Authorization Server leaves open when it emits the
   * {@code Location} header without scheme filtering.
   */
  private void validateRedirectUris(Dhis2OAuth2Client entity, Consumer<ErrorReport> errors) {
    if (entity.getRedirectUris() == null) {
      return;
    }
    Set<String> customSchemeAllowList = parseRedirectAllowList();
    for (String uri : entity.getRedirectUris().split(",")) {
      String trimmed = uri.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      String scheme = getScheme(errors, trimmed);
      if (scheme == null) continue;
      String lowerScheme = scheme.toLowerCase(Locale.ROOT);
      if ("http".equals(lowerScheme) || "https".equals(lowerScheme)) {
        continue;
      }
      if (!customSchemeAllowList.contains(trimmed)) {
        errors.accept(
            new ErrorReport(
                Dhis2OAuth2Client.class,
                ErrorCode.E4000,
                "Redirect URI not in deviceEnrollmentRedirectAllowlist: " + trimmed));
      }
    }
  }

  /**
   * Extract and return the URI scheme from the given redirect URI. Reports an {@link ErrorReport}
   * and returns {@code null} if the URI is malformed or has no scheme.
   */
  @CheckForNull
  private static String getScheme(Consumer<ErrorReport> errors, String trimmed) {
    String scheme;
    try {
      scheme = new URI(trimmed).getScheme();
    } catch (URISyntaxException e) {
      errors.accept(
          new ErrorReport(
              Dhis2OAuth2Client.class, ErrorCode.E4000, "Invalid redirect URI: " + trimmed));
      return null;
    }
    if (scheme == null || scheme.isEmpty()) {
      errors.accept(
          new ErrorReport(
              Dhis2OAuth2Client.class, ErrorCode.E4000, "Invalid redirect URI: " + trimmed));
      return null;
    }
    return scheme;
  }

  /**
   * Parse the {@code deviceEnrollmentRedirectAllowlist} system setting into a set of verbatim
   * redirect-URI strings.
   */
  private Set<String> parseRedirectAllowList() {
    String raw = systemSettingsService.getCurrentSettings().getDeviceEnrollmentRedirectAllowlist();
    if (raw == null || raw.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toUnmodifiableSet());
  }

  /** Truncate to {@code CLIENT_NAME_MAX_LENGTH}; returns {@code null} unchanged. */
  private static String truncateName(String value) {
    if (value == null) {
      return null;
    }
    return value.length() > CLIENT_NAME_MAX_LENGTH
        ? value.substring(0, CLIENT_NAME_MAX_LENGTH)
        : value;
  }
}
