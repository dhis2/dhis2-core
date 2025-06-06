/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
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
  @JsonProperty private String apiVersion;
  @JsonProperty private String applicationTitle;
  @JsonProperty private String applicationDescription;
  @JsonProperty private String applicationNotification;
  @JsonProperty private String applicationLeftSideFooter;
  @JsonProperty private String applicationRightSideFooter;
  @JsonProperty private String countryFlag;
  @JsonProperty private String uiLocale;
  @JsonProperty private String loginPageLogo;
  @JsonProperty private String loginPopup;
  @JsonProperty private String loginPageLayout;
  @JsonProperty private String loginPageTemplate;
  @JsonProperty private String recaptchaSite;
  @JsonProperty private String minPasswordLength;
  @JsonProperty private String maxPasswordLength;

  @JsonProperty private boolean emailConfigured;
  @JsonProperty private boolean selfRegistrationEnabled;
  @JsonProperty private boolean selfRegistrationNoRecaptcha;
  @JsonProperty private boolean allowAccountRecovery;
  @JsonProperty private boolean useCustomLogoFront;

  @JsonProperty private List<LoginOidcProvider> oidcProviders;
}
