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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;

@Getter
@Setter
@JacksonXmlRootElement(localName = "oauth2Authorization", namespace = DxfNamespaces.DXF_2_0)
public class Dhis2OAuth2Authorization extends BaseIdentifiableObject implements MetadataObject {

  Dhis2OAuth2Authorization() {}

  @JsonProperty private String registeredClientId;
  @JsonProperty private String principalName;
  @JsonProperty private String authorizationGrantType;
  @JsonProperty private String authorizedScopes;
  @JsonProperty private String attributes;
  @JsonProperty private String state;

  @JsonProperty private String authorizationCodeValue;
  @JsonProperty private Date authorizationCodeIssuedAt;
  @JsonProperty private Date authorizationCodeExpiresAt;
  @JsonProperty private String authorizationCodeMetadata;

  @JsonProperty private String accessTokenValue;
  @JsonProperty private Date accessTokenIssuedAt;
  @JsonProperty private Date accessTokenExpiresAt;
  @JsonProperty private String accessTokenMetadata;
  @JsonProperty private String accessTokenType;
  @JsonProperty private String accessTokenScopes;

  @JsonProperty private String refreshTokenValue;
  @JsonProperty private Date refreshTokenIssuedAt;
  @JsonProperty private Date refreshTokenExpiresAt;
  @JsonProperty private String refreshTokenMetadata;

  @JsonProperty private String oidcIdTokenValue;
  @JsonProperty private Date oidcIdTokenIssuedAt;
  @JsonProperty private Date oidcIdTokenExpiresAt;
  @JsonProperty private String oidcIdTokenMetadata;
  @JsonProperty private String oidcIdTokenClaims;

  @JsonProperty private String userCodeValue;
  @JsonProperty private Date userCodeIssuedAt;
  @JsonProperty private Date userCodeExpiresAt;
  @JsonProperty private String userCodeMetadata;

  @JsonProperty private String deviceCodeValue;
  @JsonProperty private Date deviceCodeIssuedAt;
  @JsonProperty private Date deviceCodeExpiresAt;
  @JsonProperty private String deviceCodeMetadata;
}
