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
package org.hisp.dhis.user;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Kiran Prakash
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class UserSettingServiceTest extends PostgresIntegrationTestBase {

  @Autowired private UserSettingService userSettingService;

  @Autowired private SystemSettingManager systemSettingManager;

  private User userA;

  @BeforeAll
  void setUp() {
    userSettingService.invalidateCache();
    userA = makeUser("A");
    userA.setUsername("usernameA");
    userService.addUser(userA);
  }

  @Test
  void testSaveGetDeleteUserSetting() {
    assertEquals(
        SettingKey.ANALYSIS_DISPLAY_PROPERTY.getDefaultValue(),
        userSettingService.getUserSetting(
            UserSettingKey.ANALYSIS_DISPLAY_PROPERTY, userA.getUsername()));
    assertEquals(
        SettingKey.STYLE.getDefaultValue(),
        userSettingService.getUserSetting(UserSettingKey.STYLE, userA.getUsername()));
    userSettingService.saveUserSetting(
        UserSettingKey.ANALYSIS_DISPLAY_PROPERTY, "shortName", userA);
    userSettingService.saveUserSetting(UserSettingKey.STYLE, "blue", userA);
    assertEquals(
        "shortName",
        userSettingService.getUserSetting(
            UserSettingKey.ANALYSIS_DISPLAY_PROPERTY, userA.getUsername()));
    assertEquals(
        "blue", userSettingService.getUserSetting(UserSettingKey.STYLE, userA.getUsername()));
    userSettingService.deleteUserSetting(
        UserSettingKey.ANALYSIS_DISPLAY_PROPERTY, userA.getUsername());
    assertEquals(
        SettingKey.ANALYSIS_DISPLAY_PROPERTY.getDefaultValue(),
        userSettingService.getUserSetting(
            UserSettingKey.ANALYSIS_DISPLAY_PROPERTY, userA.getUsername()));
    assertEquals(
        "blue", userSettingService.getUserSetting(UserSettingKey.STYLE, userA.getUsername()));
    userSettingService.deleteUserSetting(UserSettingKey.STYLE, userA.getUsername());
    assertEquals(
        SettingKey.ANALYSIS_DISPLAY_PROPERTY.getDefaultValue(),
        userSettingService.getUserSetting(
            UserSettingKey.ANALYSIS_DISPLAY_PROPERTY, userA.getUsername()));
    assertEquals(
        SettingKey.STYLE.getDefaultValue(),
        userSettingService.getUserSetting(UserSettingKey.STYLE, userA.getUsername()));
  }

  @Test
  void testSaveOrUpdateUserSetting() {
    userSettingService.saveUserSetting(UserSettingKey.ANALYSIS_DISPLAY_PROPERTY, "name", userA);
    userSettingService.saveUserSetting(UserSettingKey.STYLE, "blue", userA);
    assertEquals(
        "name",
        userSettingService.getUserSetting(
            UserSettingKey.ANALYSIS_DISPLAY_PROPERTY, userA.getUsername()));
    assertEquals(
        "blue", userSettingService.getUserSetting(UserSettingKey.STYLE, userA.getUsername()));
    userSettingService.saveUserSetting(
        UserSettingKey.ANALYSIS_DISPLAY_PROPERTY, "shortName", userA);
    userSettingService.saveUserSetting(UserSettingKey.STYLE, "green", userA);
    assertEquals(
        "shortName",
        userSettingService.getUserSetting(
            UserSettingKey.ANALYSIS_DISPLAY_PROPERTY, userA.getUsername()));
    assertEquals(
        "green", userSettingService.getUserSetting(UserSettingKey.STYLE, userA.getUsername()));
  }

  @Test
  void testGetUserSettingsByUser() {
    userSettingService.saveUserSetting(UserSettingKey.ANALYSIS_DISPLAY_PROPERTY, "name", userA);
    userSettingService.saveUserSetting(UserSettingKey.STYLE, "blue", userA);
    userSettingService.saveUserSetting(UserSettingKey.MESSAGE_SMS_NOTIFICATION, false, userA);
    userSettingService.saveUserSetting(UserSettingKey.MESSAGE_EMAIL_NOTIFICATION, false, userA);
    assertEquals(4, userSettingService.getUserSettings(userA).size());
  }

  @Test
  void testFallbackToDefaultValue() {
    Boolean emailNotification =
        (Boolean)
            userSettingService.getUserSetting(
                UserSettingKey.MESSAGE_EMAIL_NOTIFICATION, userA.getUsername());
    assertEquals(UserSettingKey.MESSAGE_EMAIL_NOTIFICATION.getDefaultValue(), emailNotification);
    userSettingService.saveUserSetting(
        UserSettingKey.MESSAGE_EMAIL_NOTIFICATION, Boolean.FALSE, userA);
    emailNotification =
        (Boolean)
            userSettingService.getUserSetting(
                UserSettingKey.MESSAGE_EMAIL_NOTIFICATION, userA.getUsername());
    assertEquals(Boolean.FALSE, emailNotification);
  }

  @Test
  void testFallbackToSystemSetting() {
    Locale expected = Locale.FRANCE;
    systemSettingManager.saveSystemSetting(SettingKey.UI_LOCALE, expected);
    Locale locale =
        (Locale) userSettingService.getUserSetting(UserSettingKey.UI_LOCALE, userA.getUsername());
    assertEquals(expected, locale);
  }
}
