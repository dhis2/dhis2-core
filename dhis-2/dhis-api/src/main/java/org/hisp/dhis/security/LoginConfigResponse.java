package org.hisp.dhis.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class LoginConfigResponse {

  @JsonProperty private String applicationTitle;
  @JsonProperty private String applicationDescription;
  @JsonProperty private String applicationNotification;
  @JsonProperty private String applicationLeftSideFooter;
  @JsonProperty private String applicationRightSideFooter;
  @JsonProperty private String countryFlag;
  @JsonProperty private String uiLocale;
  @JsonProperty private String loginPageLogo;
  @JsonProperty private String topMenuLogo;
  @JsonProperty private String style;
  @JsonProperty private boolean emailConfigured;
  @JsonProperty private boolean selfRegistrationEnabled;
  @JsonProperty private boolean selfRegistrationNoRecaptcha;
}
