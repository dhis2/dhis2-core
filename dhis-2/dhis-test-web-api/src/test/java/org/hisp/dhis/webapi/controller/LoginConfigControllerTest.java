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

import java.io.IOException;
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
  void shouldGetLoginConfig() throws IOException {
    systemSettingManager.saveSystemSetting(SettingKey.LOGIN_POPUP, "<html>TEXT</html>");
    systemSettingManager.saveSystemSetting(SettingKey.APPLICATION_FOOTER, "APPLICATION_FOOTER");
    systemSettingManager.saveSystemSetting(SettingKey.APPLICATION_INTRO, "APPLICATION_INTRO");
    systemSettingManager.saveSystemSetting(
        SettingKey.APPLICATION_NOTIFICATION, "APPLICATION_NOTIFICATION");
    systemSettingManager.saveSystemSetting(SettingKey.APPLICATION_FOOTER, "APPLICATION_FOOTER");
    systemSettingManager.saveSystemSetting(SettingKey.FLAG_IMAGE, "FLAG_IMAGE");
    systemSettingManager.saveSystemSetting(SettingKey.CUSTOM_LOGIN_PAGE_LOGO, true);
    systemSettingManager.saveSystemSetting(SettingKey.CUSTOM_TOP_MENU_LOGO, true);

    JsonObject response = GET("/loginConfig").content();
    assertEquals("DHIS 2", response.getString("applicationTitle").string());
    assertEquals("APPLICATION_INTRO", response.getString("applicationDescription").string());
    assertEquals(
        "APPLICATION_NOTIFICATION", response.getString("applicationNotification").string());
    assertEquals("APPLICATION_FOOTER", response.getString("applicationLeftSideFooter").string());
    assertEquals("FLAG_IMAGE", response.getString("countryFlag").string());
    assertEquals("en", response.getString("uiLocale").string());
    assertEquals("/api/staticContent/logo_front.png", response.getString("loginPageLogo").string());
    assertEquals("/external-static/logo_banner.png", response.getString("topMenuLogo").string());
    assertEquals("light_blue/light_blue.css", response.getString("style").string());
    assertEquals("<html>TEXT</html>", response.getString("loginPopup").string());

    assertFalse(response.getBoolean("selfRegistrationNoRecaptcha").booleanValue());
    assertFalse(response.getBoolean("selfRegistrationEnabled").booleanValue());
    assertFalse(response.getBoolean("emailConfigured").booleanValue());
  }
}
