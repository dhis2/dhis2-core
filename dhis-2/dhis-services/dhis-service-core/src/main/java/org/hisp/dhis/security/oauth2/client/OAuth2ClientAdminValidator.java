/*
 * Copyright (c) 2004-2026, University of Oslo
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

import static org.hisp.dhis.security.oauth2.OAuth2Constants.CLIENT_NAME_MAX_LENGTH;
import static org.hisp.dhis.security.oauth2.OAuth2Constants.SYSTEM_REGISTRAR_CLIENTID;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.setting.SystemSettingsService;
import org.jspecify.annotations.NonNull;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Component;

/**
 * Admin-input validation and defaulting for {@link Dhis2OAuth2Client} mutations. Reused from the
 * REST controller and from the metadata-import {@code ObjectBundleHook} so the same rules run on
 * every admin-facing entry point.
 *
 * <p>Deliberately scoped to admin-facing checks. The DHIS2 DCR bootstrap and Spring Authorization
 * Server's own client persistence paths must not run these validators: the system registrar
 * legitimately uses the reserved clientId and {@code client_credentials} grant, and DCR-registered
 * device clients register their own redirect URIs which Spring AS validates against the registrar's
 * allowlist rather than against ours.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
@RequiredArgsConstructor
public class OAuth2ClientAdminValidator {

  private final SystemSettingsService systemSettingsService;

  /**
   * Reject any admin attempt to use the reserved DCR system-registrar clientId. Without this, an
   * admin could squat the reserved clientId while the authorization server is disabled, and when
   * {@code OAuth2DcrService.init()} later runs, DCR would mint initial access tokens through a
   * client whose secret / redirect URIs the squatter controls.
   */
  public void rejectReservedClientId(String clientId, String operation) throws ConflictException {
    if (SYSTEM_REGISTRAR_CLIENTID.equals(clientId)) {
      throw new ConflictException(
          "Cannot "
              + operation
              + " the reserved DCR system-registrar clientId ("
              + SYSTEM_REGISTRAR_CLIENTID
              + ").");
    }
  }

  /**
   * Reject any mutation targeting the DCR system registrar client. The record is server-managed —
   * its clientId, grant types, and redirect URIs are bootstrap config for dynamic client
   * registration and must not drift from what the DCR service expects.
   */
  public void rejectIfSystemRegistrar(Dhis2OAuth2Client entity, String operation)
      throws ConflictException {
    if (entity != null && SYSTEM_REGISTRAR_CLIENTID.equals(entity.getClientId())) {
      throw new ConflictException(
          "Cannot "
              + operation
              + " the system-managed DCR registrar client ("
              + SYSTEM_REGISTRAR_CLIENTID
              + ").");
    }
  }

  /**
   * Grant-type allow-list for admin-created clients. Matches the set Spring Authorization Server
   * supports in this DHIS2 deployment: authorization_code, refresh_token, client_credentials.
   */
  public void validateGrantTypes(Dhis2OAuth2Client entity) throws ConflictException {
    if (entity.getAuthorizationGrantTypes() == null) {
      return;
    }
    String[] types = entity.getAuthorizationGrantTypes().split(",");
    for (String grantType : types) {
      String trimmed = grantType.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      if (!AuthorizationGrantType.AUTHORIZATION_CODE.getValue().equals(trimmed)
          && !AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(trimmed)
          && !AuthorizationGrantType.CLIENT_CREDENTIALS.getValue().equals(trimmed)) {
        throw new ConflictException(
            "Invalid authorization grant type: "
                + trimmed
                + ". Only authorization_code, refresh_token and client_credentials are allowed.");
      }
    }
  }

  /**
   * Redirect URI allow-list: http and https are always accepted; any other URI must match verbatim
   * an entry in the {@code deviceEnrollmentRedirectAllowlist} system setting. Default-to-deny
   * closes the stored-XSS / token-exfiltration surface that Spring Authorization Server leaves open
   * when it emits the {@code Location} header without scheme filtering.
   */
  public void validateRedirectUris(Dhis2OAuth2Client entity) throws ConflictException {
    if (entity.getRedirectUris() == null) {
      return;
    }
    Set<String> customSchemeAllowList = parseRedirectAllowList();
    String[] redirectUris = entity.getRedirectUris().split(",");
    for (String uri : redirectUris) {
      String trimmed = uri.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      String lowerScheme = getScheme(trimmed).toLowerCase(Locale.ROOT);
      if ("http".equals(lowerScheme) || "https".equals(lowerScheme)) {
        continue;
      }
      if (!customSchemeAllowList.contains(trimmed)) {
        throw new ConflictException(
            "Redirect URI not in deviceEnrollmentRedirectAllowlist: " + trimmed);
      }
    }
  }

  private static @NonNull String getScheme(String trimmed) throws ConflictException {
    URI parsed;
    try {
      parsed = new URI(trimmed);
    } catch (URISyntaxException e) {
      throw new ConflictException("Invalid redirect URI: " + trimmed);
    }
    String scheme = parsed.getScheme();
    if (scheme == null || scheme.isEmpty()) {
      throw new ConflictException("Invalid redirect URI: " + trimmed);
    }
    return scheme;
  }

  /**
   * Default the entity {@code name} to {@code clientId} when the caller didn't supply one. The
   * settings UI has no name field; {@code BaseIdentifiableObject} expects a non-empty display name.
   * Truncates to the column length to avoid persist-time overflows on long clientIds.
   */
  public void defaultNameFromClientId(Dhis2OAuth2Client entity) {
    if ((entity.getName() == null || entity.getName().isEmpty()) && entity.getClientId() != null) {
      entity.setName(truncateName(entity.getClientId()));
    }
  }

  /**
   * On update, if the caller didn't send a name, preserve the existing persisted name. Falls back
   * to clientId only when the existing record also lacks a name.
   */
  public void preserveNameOnUpdate(Dhis2OAuth2Client existing, Dhis2OAuth2Client newEntity) {
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
    return value.length() > CLIENT_NAME_MAX_LENGTH
        ? value.substring(0, CLIENT_NAME_MAX_LENGTH)
        : value;
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
