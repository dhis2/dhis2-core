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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.security.LoginConfigResponse;
import org.hisp.dhis.security.LoginConfigResponse.LoginConfigResponseBuilder;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@OpenApi.Tags({"login"})
@Controller
@RequestMapping("/loginConfig")
@RequiredArgsConstructor
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class LoginConfigController {

  private final SystemSettingManager manager;
  private final ConfigurationService configurationService;

  @Getter
  private enum KEYS {
    APPLICATION_TITLE("applicationTitle"),
    APPLICATION_INTRO("applicationDescription"),
    APPLICATION_NOTIFICATION("applicationNotification"),
    APPLICATION_FOOTER("applicationLeftSideFooter"),
    APPLICATION_RIGHT_FOOTER("applicationRightSideFooter"),
    FLAG_IMAGE("countryFlag"),
    UI_LOCALE("uiLocale"),
    CUSTOM_LOGIN_PAGE_LOGO("loginPageLogo", "/api/staticContent/logo_front.png"),
    CUSTOM_TOP_MENU_LOGO("topMenuLogo", "/external-static/logo_banner.png"),
    STYLE("style"),
    LOGIN_POPUP("loginPopup"),
    SELF_REGISTRATION_NO_RECAPTCHA("selfRegistrationNoRecaptcha");

    private final String keyName;
    private final String defaultValue;

    KEYS(String keyName) {
      this.keyName = keyName;
      this.defaultValue = null;
    }

    KEYS(String keyName, String defaultValue) {
      this.keyName = keyName;
      this.defaultValue = defaultValue;
    }
  }

  @GetMapping()
  public @ResponseBody LoginConfigResponse getConfig() {
    LoginConfigResponseBuilder builder = LoginConfigResponse.builder();
    builder.applicationTitle(
        manager.getStringSetting(SettingKey.valueOf(KEYS.APPLICATION_TITLE.name())));

    builder.applicationDescription(
        manager.getStringSetting(SettingKey.valueOf(KEYS.APPLICATION_INTRO.name())));

    builder.applicationNotification(
        manager.getStringSetting(SettingKey.valueOf(KEYS.APPLICATION_NOTIFICATION.name())));

    builder.applicationLeftSideFooter(
        manager.getStringSetting(SettingKey.valueOf(KEYS.APPLICATION_FOOTER.name())));

    builder.applicationRightSideFooter(
        manager.getStringSetting(SettingKey.valueOf(KEYS.APPLICATION_RIGHT_FOOTER.name())));

    builder.countryFlag(manager.getStringSetting(SettingKey.valueOf(KEYS.FLAG_IMAGE.name())));

    builder.uiLocale(
        manager
            .getSystemSetting(SettingKey.valueOf(KEYS.UI_LOCALE.name()), Locale.class)
            .getLanguage());

    builder.loginPageLogo(
        manager.getBoolSetting(SettingKey.valueOf(KEYS.CUSTOM_LOGIN_PAGE_LOGO.name()))
            ? KEYS.CUSTOM_LOGIN_PAGE_LOGO.defaultValue
            : null);

    builder.topMenuLogo(
        manager.getBoolSetting(SettingKey.valueOf(KEYS.CUSTOM_TOP_MENU_LOGO.name()))
            ? KEYS.CUSTOM_TOP_MENU_LOGO.defaultValue
            : null);

    builder.style(manager.getStringSetting(SettingKey.valueOf(KEYS.STYLE.name())));

    builder.loginPopup(manager.getStringSetting(SettingKey.valueOf(KEYS.LOGIN_POPUP.name())));

    builder.emailConfigured(manager.emailConfigured());

    builder.selfRegistrationEnabled(
        configurationService.getConfiguration().selfRegistrationAllowed());

    builder.selfRegistrationNoRecaptcha(
        manager.getBoolSetting(SettingKey.valueOf(KEYS.SELF_REGISTRATION_NO_RECAPTCHA.name())));

    return builder.build();
  }
}
