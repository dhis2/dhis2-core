/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.http.HttpClientAdapter.Body;
import static org.hisp.dhis.http.HttpClientAdapter.ContentType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Properties;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonMap;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.security.LoginPageLayout;
import org.hisp.dhis.security.oidc.DhisOidcClientRegistration;
import org.hisp.dhis.security.oidc.DhisOidcProviderRepository;
import org.hisp.dhis.security.oidc.provider.GoogleProvider;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.setting.SystemSettingsTranslationService;
import org.hisp.dhis.system.SystemService;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Transactional
class LoginConfigControllerTest extends PostgresControllerIntegrationTestBase {

  @Autowired SystemSettingsService settingsService;
  @Autowired SystemSettingsTranslationService settingsTranslationService;
  @Autowired SystemService systemService;
  @Autowired DhisOidcProviderRepository dhisOidcProviderRepository;

  private void addGoogleProvider(String clientId) {
    Properties config = new Properties();
    config.put(ConfigurationKey.OIDC_PROVIDER_GOOGLE_CLIENT_ID.getKey(), clientId);
    config.put(ConfigurationKey.OIDC_PROVIDER_GOOGLE_CLIENT_SECRET.getKey(), "secret");
    DhisOidcClientRegistration parse = GoogleProvider.parse(config);
    dhisOidcProviderRepository.addRegistration(parse);
  }

  @Test
  void shouldGetLoginConfig() throws Exception {

    addGoogleProvider("testClientId");

    settingsService.put("applicationTitle", "DHIS2");
    settingsTranslationService.putSystemSettingTranslation(
        "applicationTitle", "no", "Distrikstshelsesinformasjonssystem versjon 2");

    settingsService.put("loginPopup", "<html>TEXT</html>");
    settingsTranslationService.putSystemSettingTranslation(
        "loginPopup", "no", "<html>tekst</html>");

    settingsService.put("keyApplicationFooter", "APPLICATION_FOOTER");
    settingsTranslationService.putSystemSettingTranslation(
        "keyApplicationFooter", "no", "Søknadsbunntekst");

    settingsService.put("keyApplicationRightFooter", "APPLICATION_RIGHT_FOOTER");
    settingsTranslationService.putSystemSettingTranslation(
        "keyApplicationRightFooter", "no", "Høyre søknadsbunntekst");

    settingsService.put("keyApplicationIntro", "APPLICATION_INTRO");
    settingsTranslationService.putSystemSettingTranslation(
        "keyApplicationIntro", "no", "Søknadsintroduksjon");

    settingsService.put("keyApplicationNotification", "APPLICATION_NOTIFICATION");
    settingsTranslationService.putSystemSettingTranslation(
        "keyApplicationNotification", "no", "Søknadsmelding");

    settingsService.put("keyFlag", "FLAG_IMAGE");
    settingsService.put("keyUseCustomLogoFront", true);
    settingsService.put("keyCustomTopMenuLogo", true);

    JsonObject responseDefaultLocale = GET("/loginConfig").content();
    JsonObject responseNorwegianLocale = GET("/loginConfig?locale=no").content();

    assertEquals("DHIS2", responseDefaultLocale.getString("applicationTitle").string());
    assertEquals(
        "Distrikstshelsesinformasjonssystem versjon 2",
        responseNorwegianLocale.getString("applicationTitle").string());

    assertEquals(
        "APPLICATION_INTRO", responseDefaultLocale.getString("applicationDescription").string());
    assertEquals(
        "Søknadsintroduksjon",
        responseNorwegianLocale.getString("applicationDescription").string());

    assertEquals(
        "APPLICATION_NOTIFICATION",
        responseDefaultLocale.getString("applicationNotification").string());
    assertEquals(
        "Søknadsmelding", responseNorwegianLocale.getString("applicationNotification").string());

    assertEquals(
        "APPLICATION_FOOTER",
        responseDefaultLocale.getString("applicationLeftSideFooter").string());
    assertEquals(
        "Søknadsbunntekst",
        responseNorwegianLocale.getString("applicationLeftSideFooter").string());

    assertEquals(
        "APPLICATION_RIGHT_FOOTER",
        responseDefaultLocale.getString("applicationRightSideFooter").string());
    assertEquals(
        "Høyre søknadsbunntekst",
        responseNorwegianLocale.getString("applicationRightSideFooter").string());

    assertEquals("FLAG_IMAGE", responseDefaultLocale.getString("countryFlag").string());
    assertEquals("en", responseDefaultLocale.getString("uiLocale").string());
    assertEquals(
        "/api/staticContent/logo_front.png",
        responseDefaultLocale.getString("loginPageLogo").string());
    assertEquals("<html>TEXT</html>", responseDefaultLocale.getString("loginPopup").string());

    assertFalse(responseDefaultLocale.getBoolean("selfRegistrationNoRecaptcha").booleanValue());
    assertFalse(responseDefaultLocale.getBoolean("selfRegistrationEnabled").booleanValue());
    assertFalse(responseDefaultLocale.getBoolean("emailConfigured").booleanValue());
    assertEquals(
        systemService.getSystemInfoVersion(),
        responseDefaultLocale.getString("apiVersion").string());

    assertEquals(
        LoginPageLayout.DEFAULT.name(),
        responseDefaultLocale.getString("loginPageLayout").string());

    JsonArray oidcProviders = responseDefaultLocale.getArray("oidcProviders");
    assertEquals(1, oidcProviders.size());
    for (JsonValue provider : oidcProviders) {
      JsonMap<JsonObject> map = provider.asMap(JsonObject.class);
      assertEquals("\"google\"", map.get("id").toString());
      assertEquals(
          "\"/dhis-web-commons/oidc/btn_google_light_normal_ios.svg\"", map.get("icon").toString());
      assertEquals("\"0px 0px\"", map.get("iconPadding").toString());
      assertEquals("\"login_with_google\"", map.get("loginText").toString());
      assertEquals("\"/oauth2/authorization/google\"", map.get("url").toString());
    }
  }

  @Test
  void testLoginPageLayout() {
    @Language("html")
    String template =
        """
            <!DOCTYPE HTML>
            <html class="loginPage" dir="ltr" lang="en">
            <head>
            <title>DHIS 2 Demo</title>
            <meta name="description" content="DHIS 2">
            <meta name="keywords" content="DHIS 2">
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
            </head>
            <body>test</body>
            </html>""";
    POST("/systemSettings/loginPageTemplate", Body(template), ContentType(MediaType.TEXT_PLAIN))
        .content(HttpStatus.OK);
    POST("/systemSettings/loginPageLayout", Body("CUSTOM"), ContentType(MediaType.TEXT_PLAIN))
        .content(HttpStatus.OK);
    JsonObject response = GET("/loginConfig").content();
    String savedTemplate = response.getString("loginPageTemplate").string();
    assertFalse(savedTemplate.isEmpty());
    assertEquals(template, HtmlUtils.htmlUnescape(savedTemplate));
  }

  @Test
  void testRecaptchaSite() {
    JsonObject response = GET("/loginConfig").content();
    assertEquals(
        "6LcVwT0UAAAAAAkO_EGPiYOiymIszZUeHfqWIYX5", response.getString("recaptchaSite").string());
    POST("/systemSettings/recaptchaSite", "test_recaptcha_stie").content(HttpStatus.OK);
    response = GET("/loginConfig").content();
    assertEquals("test_recaptcha_stie", response.getString("recaptchaSite").string());
  }

  @Test
  void testMinPasswordLength() {
    JsonObject response = GET("/loginConfig").content();
    assertEquals("8", response.getString("minPasswordLength").string());
    POST("/systemSettings/minPasswordLength", "10").content(HttpStatus.OK);
    response = GET("/loginConfig").content();
    assertEquals("10", response.getString("minPasswordLength").string());
  }

  @Test
  void testMaxPasswordLength() {
    JsonObject response = GET("/loginConfig").content();
    assertEquals("72", response.getString("maxPasswordLength").string());
    POST("/systemSettings/maxPasswordLength", "100").content(HttpStatus.OK);
    response = GET("/loginConfig").content();
    assertEquals("100", response.getString("maxPasswordLength").string());
  }
}
