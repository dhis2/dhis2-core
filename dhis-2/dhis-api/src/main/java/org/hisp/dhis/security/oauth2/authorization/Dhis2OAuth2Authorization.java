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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.SecondaryMetadataObject;

/**
 * DHIS2 persistence representation of a Spring Authorization Server {@code OAuth2Authorization}.
 *
 * <p>An {@code OAuth2Authorization} is a single authorization grant and its associated tokens for
 * one principal authenticating against one registered client. It holds the authorization code,
 * access token, refresh token, OIDC id-token, device code and user code, each with its own
 * issued/expires timestamps and opaque metadata blob. Spring Authorization Server operates
 * exclusively on the framework type {@code
 * org.springframework.security.oauth2.server.authorization.OAuth2Authorization}, which is an
 * immutable, builder-constructed value object not wired for JPA or DHIS2 ACL.
 *
 * <p>This class is the mutable DHIS2-side mirror: a {@link BaseIdentifiableObject} with a Hibernate
 * mapping and a JSON/XML view, so the authorization can live in the DHIS2 database alongside all
 * other identifiable objects and be inspected through the admin REST surface. Conversion between
 * the two representations happens in {@code Dhis2OAuth2AuthorizationServiceImpl}: {@code
 * toEntity(OAuth2Authorization)} builds a {@link Dhis2OAuth2Authorization} for persistence, and
 * {@code toObject(Dhis2OAuth2Authorization)} reconstructs the Spring-side {@code
 * OAuth2Authorization} for the authorization-server runtime.
 *
 * <p>Marked {@link SecondaryMetadataObject} so the type is excluded from the default {@code
 * /api/metadata} export; token-bearing fields are additionally {@link JsonIgnore}'d so no REST
 * surface can leak them even on explicit requests. Persistence uses Hibernate field access ({@code
 * Dhis2OAuth2Authorization.hbm.xml}) and is independent of the JSON annotations.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Getter
@Setter
@JacksonXmlRootElement(localName = "oauth2Authorization", namespace = DxfNamespaces.DXF_2_0)
@SuppressWarnings("java:S2160") // Identity is uid, handled by BaseIdentifiableObject.equals.
public class Dhis2OAuth2Authorization extends BaseIdentifiableObject
    implements SecondaryMetadataObject {

  /** Required by Hibernate + Jackson for reflective instantiation. */
  public Dhis2OAuth2Authorization() {}

  /**
   * Reference to the {@link org.hisp.dhis.security.oauth2.client.Dhis2OAuth2Client} this grant was
   * issued to. Holds the internal id of the registered client, not its public {@code clientId}.
   */
  @JsonProperty private String registeredClientId;

  /**
   * Name of the resource owner the grant is tied to. For user-delegated flows this is the DHIS2
   * username; for {@code client_credentials} it is the client itself.
   */
  @JsonProperty private String principalName;

  /**
   * The grant type that produced this authorization (e.g. {@code authorization_code}, {@code
   * client_credentials}, {@code refresh_token}, {@code
   * urn:ietf:params:oauth:grant-type:device_code}).
   */
  @JsonProperty private String authorizationGrantType;

  /** Comma-separated list of scopes that were actually granted for this authorization. */
  @JsonProperty private String authorizedScopes;

  /** JSON-encoded Spring AS attributes map (authenticated principal, request metadata, etc.). */
  @JsonIgnore private String attributes;

  /** Opaque {@code state} value used by Spring AS for OAuth2 CSRF protection during the flow. */
  @JsonIgnore private String state;

  @JsonIgnore private String authorizationCodeValue;
  @JsonProperty private Date authorizationCodeIssuedAt;
  @JsonProperty private Date authorizationCodeExpiresAt;
  @JsonIgnore private String authorizationCodeMetadata;

  @JsonIgnore private String accessTokenValue;
  @JsonProperty private Date accessTokenIssuedAt;
  @JsonProperty private Date accessTokenExpiresAt;
  @JsonIgnore private String accessTokenMetadata;
  @JsonProperty private String accessTokenType;
  @JsonProperty private String accessTokenScopes;

  @JsonIgnore private String refreshTokenValue;
  @JsonProperty private Date refreshTokenIssuedAt;
  @JsonProperty private Date refreshTokenExpiresAt;
  @JsonIgnore private String refreshTokenMetadata;

  @JsonIgnore private String oidcIdTokenValue;
  @JsonProperty private Date oidcIdTokenIssuedAt;
  @JsonProperty private Date oidcIdTokenExpiresAt;
  @JsonIgnore private String oidcIdTokenMetadata;
  @JsonIgnore private String oidcIdTokenClaims;

  @JsonIgnore private String userCodeValue;
  @JsonProperty private Date userCodeIssuedAt;
  @JsonProperty private Date userCodeExpiresAt;
  @JsonIgnore private String userCodeMetadata;

  @JsonIgnore private String deviceCodeValue;
  @JsonProperty private Date deviceCodeIssuedAt;
  @JsonProperty private Date deviceCodeExpiresAt;
  @JsonIgnore private String deviceCodeMetadata;
}
