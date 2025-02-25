package org.hisp.dhis.security.oauth2.client;

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
@JacksonXmlRootElement(localName = "oauth2Client", namespace = DxfNamespaces.DXF_2_0)
public class OAuth2Client extends BaseIdentifiableObject implements MetadataObject {

  OAuth2Client() {}

  @JsonProperty private String clientId;
  @JsonProperty private String clientSecret;
  @JsonProperty private Instant clientIdIssuedAt;
  @JsonProperty private Instant clientSecretExpiresAt;
  @JsonProperty private String clientAuthenticationMethods;
  @JsonProperty private String authorizationGrantTypes;
  @JsonProperty private String redirectUris;
  @JsonProperty private String postLogoutRedirectUris;
  @JsonProperty private String scopes;
  @JsonProperty private String clientSettings;
  @JsonProperty private String tokenSettings;
}
