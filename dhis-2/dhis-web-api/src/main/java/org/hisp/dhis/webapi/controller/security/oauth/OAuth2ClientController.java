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
package org.hisp.dhis.webapi.controller.security.oauth;

import static org.hisp.dhis.security.Authorities.F_OAUTH2_CLIENT_MANAGE;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.query.Filter;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.operators.NotEqualOperator;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.security.oauth2.client.Dhis2OAuth2Client;
import org.hisp.dhis.security.oauth2.client.Dhis2OAuth2ClientService;
import org.hisp.dhis.security.oauth2.dcr.OAuth2DcrService;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for managing OAuth2 clients for the DHIS2 OAuth2 authorization server.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Controller
@RequestMapping({"/api/oAuth2Clients"})
@RequiredArgsConstructor
@RequiresAuthority(anyOf = F_OAUTH2_CLIENT_MANAGE)
public class OAuth2ClientController
    extends AbstractCrudController<Dhis2OAuth2Client, GetObjectListParams> {

  /** Max length of the persisted `name` column; keep in sync with the Hibernate mapping. */
  private static final int NAME_MAX_LENGTH = 230;

  private final Dhis2OAuth2ClientService clientService;
  private final SystemSettingsService systemSettingsService;

  @Override
  protected void preCreateEntity(Dhis2OAuth2Client entity) throws ConflictException {
    validateAuthorizationGrantTypes(entity);
    validateRedirectUris(entity);
    defaultNameFromClientId(entity);

    if (entity.getClientSettings() == null) {
      ClientSettings.Builder builder = ClientSettings.builder();
      builder.requireAuthorizationConsent(true);
      ClientSettings clientSettings = builder.build();
      entity.setClientSettings(clientService.writeMap(clientSettings.getSettings()));
    }
    if (entity.getTokenSettings() == null) {
      TokenSettings tokenSettings = TokenSettings.builder().build();
      entity.setTokenSettings(clientService.writeMap(tokenSettings.getSettings()));
    }
  }

  @Override
  protected void preUpdateEntity(Dhis2OAuth2Client entity, Dhis2OAuth2Client newEntity)
      throws ConflictException {
    rejectIfSystemRegistrar(entity, "update");
    validateAuthorizationGrantTypes(newEntity);
    validateRedirectUris(newEntity);
    preserveNameOnUpdate(entity, newEntity);

    super.preUpdateEntity(entity, newEntity);
  }

  @Override
  protected void preDeleteEntity(Dhis2OAuth2Client entity) throws ConflictException {
    rejectIfSystemRegistrar(entity, "delete");
  }

  /**
   * Hide the DCR system registrar client from list queries. It's created and owned by the server
   * itself to bootstrap dynamic client registration; admins should never touch it through the
   * settings UI. Direct-by-uid fetches are left alone since DCR / other server code may need to
   * read it, and the UI only discovers client uids via this list.
   */
  @Override
  protected void modifyGetObjectList(GetObjectListParams params, Query<Dhis2OAuth2Client> query) {
    query.add(
        new Filter("clientId", new NotEqualOperator<>(OAuth2DcrService.SYSTEM_REGISTRAR_CLIENTID)));
  }

  /**
   * Reject any mutation targeting the DCR system registrar client. The record is server-managed —
   * its clientId, grant types, and redirect URIs are bootstrap config for dynamic client
   * registration and must not drift from what {@link OAuth2DcrService} expects.
   */
  private static void rejectIfSystemRegistrar(Dhis2OAuth2Client entity, String operation)
      throws ConflictException {
    if (entity != null && OAuth2DcrService.SYSTEM_REGISTRAR_CLIENTID.equals(entity.getClientId())) {
      throw new ConflictException(
          "Cannot "
              + operation
              + " the system-managed DCR registrar client ("
              + OAuth2DcrService.SYSTEM_REGISTRAR_CLIENTID
              + ").");
    }
  }

  /**
   * Default the entity name to the clientId when the caller didn't supply one. The settings UI
   * currently has no name field, so POSTs omit it; BaseIdentifiableObject requires a non-null name
   * after 2.44, so fall back to the technical clientId for display.
   */
  private void defaultNameFromClientId(Dhis2OAuth2Client entity) {
    if ((entity.getName() == null || entity.getName().isEmpty()) && entity.getClientId() != null) {
      entity.setName(truncateName(entity.getClientId()));
    }
  }

  /**
   * On update, if the caller didn't send a name (the settings UI has no name field), preserve the
   * existing persisted name rather than clobbering it via REPLACE merge. Fall back to clientId only
   * if the existing record also lacks a name.
   */
  private void preserveNameOnUpdate(Dhis2OAuth2Client existing, Dhis2OAuth2Client newEntity) {
    if (newEntity.getName() != null && !newEntity.getName().isEmpty()) {
      return;
    }
    if (existing != null && existing.getName() != null && !existing.getName().isEmpty()) {
      newEntity.setName(existing.getName());
    } else if (newEntity.getClientId() != null) {
      newEntity.setName(truncateName(newEntity.getClientId()));
    }
  }

  private static String truncateName(String value) {
    return value.length() > NAME_MAX_LENGTH ? value.substring(0, NAME_MAX_LENGTH) : value;
  }

  /**
   * Validates that the authorization grant types in the entity contain only values supported by the
   * Spring Authorization Server: authorization_code, refresh_token, client_credentials.
   * client_credentials is required by the internal DCR system registrar client.
   *
   * @param entity the OAuth2 client entity to validate
   * @throws ConflictException if any invalid grant type is found
   */
  private void validateAuthorizationGrantTypes(Dhis2OAuth2Client entity) throws ConflictException {
    if (entity.getAuthorizationGrantTypes() != null) {
      String[] grantTypes = entity.getAuthorizationGrantTypes().split(",");
      for (String grantType : grantTypes) {
        String trimmedGrantType = grantType.trim();
        if (trimmedGrantType.isEmpty()) {
          continue;
        }
        if (!AuthorizationGrantType.AUTHORIZATION_CODE.getValue().equals(trimmedGrantType)
            && !AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(trimmedGrantType)
            && !AuthorizationGrantType.CLIENT_CREDENTIALS.getValue().equals(trimmedGrantType)) {
          throw new ConflictException(
              "Invalid authorization grant type: "
                  + trimmedGrantType
                  + ". Only authorization_code, refresh_token and client_credentials are allowed.");
        }
      }
    }
  }

  /**
   * Validates redirect URIs against an explicit allow-list. http and https schemes are always
   * accepted. Any other URI must match verbatim one of the entries in the {@code
   * deviceEnrollmentRedirectAllowlist} system setting (same allow-list used by DCR device
   * enrollment, default {@code dhis2oauth://oauth}).
   *
   * <p>Spring Authorization Server does exact-string matching of the stored redirect URI at
   * authorize time and then emits a {@code Location} header with no scheme filtering, so any stored
   * {@code javascript:} / {@code data:} / OS-scheme URI would fire in a victim's browser after a
   * completed flow. Defaulting to deny and letting the admin curate the allow-list via a single
   * system setting closes that surface without hardcoding a blocklist we'd have to keep chasing.
   *
   * @param entity the OAuth2 client entity to validate
   * @throws ConflictException if any redirect URI is malformed or not allowed
   */
  private void validateRedirectUris(Dhis2OAuth2Client entity) throws ConflictException {
    if (entity.getRedirectUris() == null) {
      return;
    }
    Set<String> customSchemeAllowList = null; // lazy — only parse when we hit a non-http(s) URI
    for (String uri : entity.getRedirectUris().split(",")) {
      String trimmedUri = uri.trim();
      if (trimmedUri.isEmpty()) {
        continue;
      }
      URI parsed;
      try {
        parsed = new URI(trimmedUri);
      } catch (URISyntaxException e) {
        throw new ConflictException("Invalid redirect URI: " + trimmedUri);
      }
      String scheme = parsed.getScheme();
      if (scheme == null || scheme.isEmpty()) {
        throw new ConflictException("Invalid redirect URI: " + trimmedUri);
      }
      String lowerScheme = scheme.toLowerCase(Locale.ROOT);
      if ("http".equals(lowerScheme) || "https".equals(lowerScheme)) {
        continue;
      }
      if (customSchemeAllowList == null) {
        customSchemeAllowList = parseRedirectAllowList();
      }
      if (!customSchemeAllowList.contains(trimmedUri)) {
        throw new ConflictException(
            "Redirect URI not in deviceEnrollmentRedirectAllowlist: " + trimmedUri);
      }
    }
  }

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
}
