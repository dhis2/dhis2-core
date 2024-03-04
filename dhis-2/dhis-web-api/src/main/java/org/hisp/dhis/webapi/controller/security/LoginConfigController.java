/*
 * Copyright (c) 2004-2024, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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

import java.util.Locale;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.security.LoginConfigResponse;
import org.hisp.dhis.security.LoginConfigResponse.LoginConfigResponseBuilder;
import org.hisp.dhis.security.LoginPageLayout;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.SystemService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@OpenApi.Tags({"login"})
@RestController
@RequestMapping("/loginConfig")
@RequiredArgsConstructor
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class LoginConfigController {

  private final SystemSettingManager manager;
  private final ConfigurationService configurationService;
  private final SystemService systemService;

  @Getter
  private enum KEYS {
    APPLICATION_TITLE(),
    APPLICATION_INTRO(),
    APPLICATION_NOTIFICATION(),
    APPLICATION_FOOTER(),
    APPLICATION_RIGHT_FOOTER(),
    FLAG(),
    CUSTOM_LOGIN_PAGE_LOGO("/api/staticContent/logo_front.png"),
    UI_LOCALE(),
    LOGIN_POPUP(),
    SELF_REGISTRATION_NO_RECAPTCHA(),
    USE_CUSTOM_LOGO_FRONT(),
    ACCOUNT_RECOVERY(),
    /** The layout to be used for displaying LoginPage. Value is the enum {@link LoginPageLayout} */
    LOGIN_PAGE_LAYOUT(LoginPageLayout.DEFAULT.name()),

    /**
     * The HTML string which is used for displaying LoginPage if selected {@link LoginPageLayout} is
     * CUSTOM.
     */
    LOGIN_PAGE_TEMPLATE();
    ;

    private final String defaultValue;

    KEYS() {
      this.defaultValue = null;
    }

    KEYS(String defaultValue) {
      this.defaultValue = defaultValue;
    }
  }

  private String getTranslatableString(KEYS key, String locale) {
    Optional<String> translated =
        manager.getSystemSettingTranslation(SettingKey.valueOf(key.name()), locale);

    return translated.orElseGet(() -> manager.getStringSetting(SettingKey.valueOf(key.name())));
  }

  @GetMapping()
  public LoginConfigResponse getConfig(
      @RequestParam(required = false, defaultValue = "en") String locale) {
    LoginConfigResponseBuilder builder = LoginConfigResponse.builder();

    builder.applicationTitle(getTranslatableString(KEYS.APPLICATION_TITLE, locale));
    builder.applicationDescription(getTranslatableString(KEYS.APPLICATION_INTRO, locale));
    builder.applicationNotification(getTranslatableString(KEYS.APPLICATION_NOTIFICATION, locale));
    builder.applicationLeftSideFooter(getTranslatableString(KEYS.APPLICATION_FOOTER, locale));
    builder.applicationRightSideFooter(
        getTranslatableString(KEYS.APPLICATION_RIGHT_FOOTER, locale));
    builder.loginPopup(getTranslatableString(KEYS.LOGIN_POPUP, locale));

    builder.countryFlag(manager.getStringSetting(SettingKey.valueOf(KEYS.FLAG.name())));

    builder.uiLocale(
        manager
            .getSystemSetting(SettingKey.valueOf(KEYS.UI_LOCALE.name()), Locale.class)
            .getLanguage());

    builder.loginPageLogo(
        manager.getBoolSetting(SettingKey.valueOf(KEYS.USE_CUSTOM_LOGO_FRONT.name()))
            ? KEYS.CUSTOM_LOGIN_PAGE_LOGO.defaultValue
            : null);

    builder.selfRegistrationNoRecaptcha(
        manager.getBoolSetting(SettingKey.valueOf(KEYS.SELF_REGISTRATION_NO_RECAPTCHA.name())));
    builder.allowAccountRecovery(
        manager.getBoolSetting(SettingKey.valueOf(KEYS.ACCOUNT_RECOVERY.name())));
    builder.useCustomLogoFront(
        manager.getBoolSetting(SettingKey.valueOf(KEYS.USE_CUSTOM_LOGO_FRONT.name())));

    builder.emailConfigured(manager.emailConfigured());

    builder.selfRegistrationEnabled(
        configurationService.getConfiguration().selfRegistrationAllowed());

    builder.apiVersion(systemService.getSystemInfo().getVersion());

    return builder.build();
  }
}
