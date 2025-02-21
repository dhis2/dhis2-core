package org.hisp.dhis.security.oauth2.authorization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;

@Getter
@Setter
@JacksonXmlRootElement(localName = "oauth2Authorization", namespace = DxfNamespaces.DXF_2_0)
public class OAuth2Authorization extends BaseIdentifiableObject implements MetadataObject {

  OAuth2Authorization() {}

  @JsonProperty private String registeredClientId;
  @JsonProperty private String principalName;
  @JsonProperty private String authorizationGrantType;
  @JsonProperty private String authorizedScopes;
  @JsonProperty private String attributes;
  @JsonProperty private String state;

  @JsonProperty private String authorizationCodeValue;
  @JsonProperty private Instant authorizationCodeIssuedAt;
  @JsonProperty private Instant authorizationCodeExpiresAt;
  @JsonProperty private String authorizationCodeMetadata;

  @JsonProperty private String accessTokenValue;
  @JsonProperty private Instant accessTokenIssuedAt;
  @JsonProperty private Instant accessTokenExpiresAt;
  @JsonProperty private String accessTokenMetadata;
  @JsonProperty private String accessTokenType;
  @JsonProperty private String accessTokenScopes;

  @JsonProperty private String refreshTokenValue;
  @JsonProperty private Instant refreshTokenIssuedAt;
  @JsonProperty private Instant refreshTokenExpiresAt;
  @JsonProperty private String refreshTokenMetadata;

  @JsonProperty private String oidcIdTokenValue;
  @JsonProperty private Instant oidcIdTokenIssuedAt;
  @JsonProperty private Instant oidcIdTokenExpiresAt;
  @JsonProperty private String oidcIdTokenMetadata;
  @JsonProperty private String oidcIdTokenClaims;

  @JsonProperty private String userCodeValue;
  @JsonProperty private Instant userCodeIssuedAt;
  @JsonProperty private Instant userCodeExpiresAt;
  @JsonProperty private String userCodeMetadata;

  @JsonProperty private String deviceCodeValue;
  @JsonProperty private Instant deviceCodeIssuedAt;
  @JsonProperty private Instant deviceCodeExpiresAt;
  @JsonProperty private String deviceCodeMetadata;
}
