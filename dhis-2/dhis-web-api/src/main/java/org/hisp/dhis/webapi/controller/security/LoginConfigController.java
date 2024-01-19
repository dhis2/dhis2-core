package org.hisp.dhis.webapi.controller.security;

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
import org.springframework.web.bind.annotation.PathVariable;
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

  private enum KEYS {
    APPLICATION_TITLE("applicationTitle"),
    APPLICATION_INTRO("applicationDescription"),
    APPLICATION_RIGHT_FOOTER(""),
    APPLICATION_FOOTER(""),
    APPLICATION_NOTIFICATION(""),
    FLAG_IMAGE(""),
    CUSTOM_LOGIN_PAGE_LOGO(""),
    STYLE(""),
    CUSTOM_TOP_MENU_LOGO(""),
    SYSTEM_NOTIFICATIONS_EMAIL(""),
    SELF_REGISTRATION_NO_RECAPTCHA(""),
    UI_LOCALE("");

    private String keyName;

    KEYS(String keyName) {
      this.keyName = keyName;
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

    builder.uiLocale(manager.getStringSetting(SettingKey.valueOf(KEYS.UI_LOCALE.name())));

    builder.loginPageLogo(
        manager.getStringSetting(SettingKey.valueOf(KEYS.CUSTOM_LOGIN_PAGE_LOGO.name())));

    builder.topMenuLogo(
        manager.getStringSetting(SettingKey.valueOf(KEYS.CUSTOM_TOP_MENU_LOGO.name())));

    builder.style(manager.getStringSetting(SettingKey.valueOf(KEYS.STYLE.name())));

    builder.emailConfigured(manager.emailConfigured());

    builder.selfRegistrationEnabled(
        configurationService.getConfiguration().selfRegistrationAllowed());

    builder.selfRegistrationNoRecaptcha(
        manager.getBoolSetting(SettingKey.valueOf(KEYS.SELF_REGISTRATION_NO_RECAPTCHA.name())));

    return builder.build();
  }

  @GetMapping("/{configKey}")
  public @ResponseBody String getConfigKey(@PathVariable String configKey) {
    KEYS validKey = KEYS.valueOf(configKey);
    return manager.getStringSetting(SettingKey.valueOf(validKey.name()));
  }
}
