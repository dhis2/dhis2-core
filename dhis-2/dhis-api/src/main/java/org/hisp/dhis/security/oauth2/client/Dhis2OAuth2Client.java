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

@Getter
@Setter
@JacksonXmlRootElement(localName = "oauth2Client", namespace = DxfNamespaces.DXF_2_0)
public class Dhis2OAuth2Client extends BaseIdentifiableObject implements MetadataObject {

  public Dhis2OAuth2Client() {}

  /**
   * Override so that the persisted {@code name} column is always populated even if the caller (the
   * settings UI, which has no name field) doesn't supply one. Hibernate uses property access for
   * this entity, so the value returned here is what gets written to the DB and what the schema
   * validator at {@code POST /api/schemas/oAuth2Client} reads via reflection — letting us keep
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

  @JsonProperty private String clientId;
  @JsonProperty private String clientSecret;
  @JsonProperty private Date clientIdIssuedAt;
  @JsonProperty private Date clientSecretExpiresAt;
  @JsonProperty private String clientAuthenticationMethods;
  @JsonProperty private String authorizationGrantTypes;
  @JsonProperty private String redirectUris;
  @JsonProperty private String postLogoutRedirectUris;
  @JsonProperty private String scopes;
  @JsonProperty private String clientSettings;
  @JsonProperty private String tokenSettings;
}
