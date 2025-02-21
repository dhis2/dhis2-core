package org.hisp.dhis.security.oauth2.consent;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;

@Getter
@Setter
@JacksonXmlRootElement(localName = "oauth2AuthorizationConsent", namespace = DxfNamespaces.DXF_2_0)
public class OAuth2AuthorizationConsent extends BaseIdentifiableObject implements MetadataObject {

  OAuth2AuthorizationConsent() {}

  @JsonProperty private String registeredClientId;
  @JsonProperty private String principalName;
  @JsonProperty private String authorities;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    OAuth2AuthorizationConsent that = (OAuth2AuthorizationConsent) o;
    return Objects.equals(registeredClientId, that.registeredClientId)
        && Objects.equals(principalName, that.principalName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), registeredClientId, principalName);
  }
}
