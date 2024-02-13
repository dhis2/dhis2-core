/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class LoginConfigControllerTest extends DhisControllerIntegrationTest {

  @Autowired SystemSettingManager systemSettingManager;

  @Test
  void shouldGetLoginConfig() {
    systemSettingManager.saveSystemSetting(SettingKey.APPLICATION_TITLE, "DHIS2");
    systemSettingManager.saveSystemSettingTranslation(
        SettingKey.APPLICATION_TITLE, "no", "Distrikstshelsesinformasjonssystem versjon 2");

    systemSettingManager.saveSystemSetting(SettingKey.LOGIN_POPUP, "<html>TEXT</html>");
    systemSettingManager.saveSystemSettingTranslation(
        SettingKey.LOGIN_POPUP, "no", "<html>tekst</html>");

    systemSettingManager.saveSystemSetting(SettingKey.APPLICATION_FOOTER, "APPLICATION_FOOTER");
    systemSettingManager.saveSystemSettingTranslation(
        SettingKey.APPLICATION_FOOTER, "no", "Søknadsbunntekst");

    systemSettingManager.saveSystemSetting(SettingKey.APPLICATION_INTRO, "APPLICATION_INTRO");
    systemSettingManager.saveSystemSettingTranslation(
        SettingKey.APPLICATION_INTRO, "no", "Søknadsintroduksjon");

    systemSettingManager.saveSystemSetting(
        SettingKey.APPLICATION_NOTIFICATION, "APPLICATION_NOTIFICATION");
    systemSettingManager.saveSystemSettingTranslation(
        SettingKey.APPLICATION_NOTIFICATION, "no", "Søknadsmelding");

    systemSettingManager.saveSystemSetting(SettingKey.FLAG_IMAGE, "FLAG_IMAGE");
    systemSettingManager.saveSystemSetting(SettingKey.CUSTOM_LOGIN_PAGE_LOGO, true);
    systemSettingManager.saveSystemSetting(SettingKey.CUSTOM_TOP_MENU_LOGO, true);

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

    assertEquals("FLAG_IMAGE", responseDefaultLocale.getString("countryFlag").string());
    assertEquals("en", responseDefaultLocale.getString("uiLocale").string());
    assertEquals(
        "/api/staticContent/logo_front.png",
        responseDefaultLocale.getString("loginPageLogo").string());
    assertEquals("<html>TEXT</html>", responseDefaultLocale.getString("loginPopup").string());

    assertFalse(responseDefaultLocale.getBoolean("selfRegistrationNoRecaptcha").booleanValue());
    assertFalse(responseDefaultLocale.getBoolean("selfRegistrationEnabled").booleanValue());
    assertFalse(responseDefaultLocale.getBoolean("emailConfigured").booleanValue());
  }
}
