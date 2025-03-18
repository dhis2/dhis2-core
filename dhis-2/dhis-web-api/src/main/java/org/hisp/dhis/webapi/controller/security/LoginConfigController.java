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
package org.hisp.dhis.webapi.controller.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.security.LoginConfigResponse;
import org.hisp.dhis.security.LoginOidcProvider;
import org.hisp.dhis.security.oidc.DhisOidcClientRegistration;
import org.hisp.dhis.security.oidc.DhisOidcProviderRepository;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.setting.SystemSettingsTranslationService;
import org.hisp.dhis.system.SystemService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@OpenApi.Document(
    entity = User.class,
    classifiers = {"team:platform", "purpose:support"})
@RestController
@RequestMapping("/api/loginConfig")
@RequiredArgsConstructor
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class LoginConfigController {

  private final SystemSettingsService settingsService;
  private final SystemSettingsTranslationService settingsTranslationService;
  private final ConfigurationService configurationService;
  private final SystemService systemService;
  private final DhisOidcProviderRepository oidcProviderRepository;

  private String getTranslatableString(String key, String locale) {
    return settingsTranslationService
        .getSystemSettingTranslation(key, locale)
        .orElseGet(() -> settingsService.getCurrentSettings().asString(key, ""));
  }

  @GetMapping()
  public LoginConfigResponse getConfig(
      @RequestParam(required = false, defaultValue = "en") String locale, SystemSettings settings) {
    return LoginConfigResponse.builder()
        .applicationTitle(getTranslatableString("applicationTitle", locale))
        .applicationDescription(getTranslatableString("keyApplicationIntro", locale))
        .applicationNotification(getTranslatableString("keyApplicationNotification", locale))
        .applicationLeftSideFooter(getTranslatableString("keyApplicationFooter", locale))
        .applicationRightSideFooter(getTranslatableString("keyApplicationRightFooter", locale))
        .loginPopup(getTranslatableString("loginPopup", locale))
        .countryFlag(settings.getFlag())
        .uiLocale(settings.getUiLocale().getLanguage())
        .loginPageLogo(
            settings.getUseCustomLogoFront() ? "/api/staticContent/logo_front.png" : null)
        .selfRegistrationNoRecaptcha(settings.getSelfRegistrationNoRecaptcha())
        .allowAccountRecovery(settings.getAccountRecoveryEnabled())
        .useCustomLogoFront(settings.getUseCustomLogoFront())
        .emailConfigured(settings.isEmailConfigured())
        .selfRegistrationEnabled(configurationService.getConfiguration().selfRegistrationAllowed())
        .apiVersion(systemService.getSystemInfoVersion())
        .recaptchaSite(settings.getRecaptchaSite())
        .loginPageLayout(settings.getLoginPageLayout().name())
        .loginPageTemplate(settings.getLoginPageTemplate())
        .minPasswordLength(String.valueOf(settings.getMinPasswordLength()))
        .maxPasswordLength(String.valueOf(settings.getMaxPasswordLength()))
        .oidcProviders(getRegisteredOidcProviders())
        .build();
  }

  private List<LoginOidcProvider> getRegisteredOidcProviders() {
    List<LoginOidcProvider> providers = new ArrayList<>();

    Set<String> allRegistrationIds = oidcProviderRepository.getAllRegistrationId();

    for (String registrationId : allRegistrationIds) {
      DhisOidcClientRegistration clientRegistration =
          oidcProviderRepository.getDhisOidcClientRegistration(registrationId);

      providers.add(
          LoginOidcProvider.builder()
              .id(registrationId)
              .icon(clientRegistration.getLoginIcon())
              .iconPadding(clientRegistration.getLoginIconPadding())
              .loginText(clientRegistration.getLoginText())
              .url("/oauth2/authorization/" + registrationId)
              .build());
    }

    return providers;
  }
}
