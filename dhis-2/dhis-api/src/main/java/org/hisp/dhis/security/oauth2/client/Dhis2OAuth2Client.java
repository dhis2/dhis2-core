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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;

/**
 * Persisted OAuth2 registered-client entity. Mirrors Spring Authorization Server's {@link
 * org.springframework.security.oauth2.server.authorization.client.RegisteredClient}, mapped to the
 * {@code oauth2_client} table, and is the DB row that authorizes an app to request tokens from
 * DHIS2 acting as an Authorization Server.
 *
 * <p>Exposed via the admin CRUD endpoints under {@code /api/oAuth2Clients}, gated by the {@code
 * F_OAUTH2_CLIENT_MANAGE} authority. Clients can also be created dynamically via the Dynamic Client
 * Registration (RFC 7591) endpoint {@code /connect/register}.
 *
 * <p>Several fields are stored as comma-separated strings (for example {@link
 * #authorizationGrantTypes}, {@link #clientAuthenticationMethods}, {@link #redirectUris}, {@link
 * #postLogoutRedirectUris}, {@link #scopes}) and are parsed into Spring AS's typed values (e.g.
 * {@link org.springframework.security.oauth2.core.AuthorizationGrantType}, {@link
 * org.springframework.security.oauth2.core.ClientAuthenticationMethod}) by {@code
 * Dhis2OAuth2ClientServiceImpl.toObject} when building a {@link
 * org.springframework.security.oauth2.server.authorization.client.RegisteredClient}.
 */
@Getter
@Setter
@JacksonXmlRootElement(localName = "oauth2Client", namespace = DxfNamespaces.DXF_2_0)
public class Dhis2OAuth2Client extends BaseIdentifiableObject implements MetadataObject {

  public Dhis2OAuth2Client() {}

  /**
   * Override so that the persisted {@code name} column is always populated even if the caller (the
   * settings UI, which has no name field) doesn't supply one. Hibernate uses property access for
   * this entity, so the value returned here is what gets written to the DB and what the schema
   * validator at {@code POST /api/schemas/oAuth2Client} reads via reflection, letting us keep
   * {@code not-null="true"} on the column without breaking UI pre-validation. Truncated to the
   * column length (230) so the schema-validator's {@code @PropertyRange} check on a long {@code
   * clientId} (max 255) doesn't reject the request.
   */
  @Override
  public String getName() {
    String name = super.getName();
    if (name != null && !name.isEmpty()) {
      return name;
    }
    if (clientId == null) {
      return null;
    }
    return clientId.length() > 230 ? clientId.substring(0, 230) : clientId;
  }

  /**
   * Returns the raw {@code name} field value without the {@link #getName()} fallback. Use this when
   * defaulting / preservation logic needs to distinguish "caller didn't supply a name" from "caller
   * supplied the same value as the fallback".
   */
  public String getRawName() {
    return super.getName();
  }

  /**
   * Public OAuth2 {@code client_id} presented by the client at the token and authorize endpoints.
   */
  @JsonProperty private String clientId;

  /**
   * Client secret used for the {@code client_secret_basic} / {@code client_secret_post}
   * authentication methods. Stored hashed; null for public clients and for clients that
   * authenticate via {@code private_key_jwt}.
   */
  @JsonProperty private String clientSecret;

  /** Timestamp at which {@link #clientId} was issued. */
  @JsonProperty private Date clientIdIssuedAt;

  /** Optional expiry for {@link #clientSecret}; null means the secret does not expire. */
  @JsonProperty private Date clientSecretExpiresAt;

  /**
   * Comma-separated list of OAuth2 client authentication methods the client may use at the token
   * endpoint (e.g. {@code client_secret_basic}, {@code client_secret_post}, {@code
   * private_key_jwt}, {@code none}). Parsed into Spring AS {@link
   * org.springframework.security.oauth2.core.ClientAuthenticationMethod} values at load time.
   */
  @JsonProperty private String clientAuthenticationMethods;

  /**
   * Comma-separated list of OAuth2 authorization grant types the client is permitted to use (e.g.
   * {@code authorization_code}, {@code client_credentials}, {@code refresh_token}, {@code
   * urn:ietf:params:oauth:grant-type:device_code}). Parsed into Spring AS {@link
   * org.springframework.security.oauth2.core.AuthorizationGrantType} values via {@link
   * org.hisp.dhis.security.oauth2.OAuth2GrantTypes#resolve(String)}.
   */
  @JsonProperty private String authorizationGrantTypes;

  /**
   * Comma-separated list of registered redirect URIs used by the authorization-code and device-code
   * flows; an incoming {@code redirect_uri} must match one of these exactly.
   */
  @JsonProperty private String redirectUris;

  /** Comma-separated list of post-logout redirect URIs allowed after OIDC RP-initiated logout. */
  @JsonProperty private String postLogoutRedirectUris;

  /**
   * Comma-separated list of OAuth2 / OpenID Connect scopes (e.g. {@code openid}, {@code profile},
   * {@code email}) the client is permitted to request.
   */
  @JsonProperty private String scopes;

  /**
   * JSON-encoded Spring AS {@code ClientSettings}; controls client-level options such as whether
   * user consent is required and PKCE requirements.
   */
  @JsonProperty private String clientSettings;

  /**
   * JSON-encoded Spring AS {@code TokenSettings}; controls token lifetimes, access-token format
   * (opaque vs JWT), refresh-token behavior and ID-token signature algorithm.
   */
  @JsonProperty private String tokenSettings;
}
