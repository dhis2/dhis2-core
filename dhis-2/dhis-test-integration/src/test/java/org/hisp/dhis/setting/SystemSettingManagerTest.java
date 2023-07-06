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
package org.hisp.dhis.setting;

import static org.hisp.dhis.setting.SettingKey.APPLICATION_INTRO;
import static org.hisp.dhis.setting.SettingKey.APPLICATION_NOTIFICATION;
import static org.hisp.dhis.setting.SettingKey.APPLICATION_TITLE;
import static org.hisp.dhis.setting.SettingKey.EMAIL_HOST_NAME;
import static org.hisp.dhis.setting.SettingKey.EMAIL_PASSWORD;
import static org.hisp.dhis.setting.SettingKey.EMAIL_PORT;
import static org.hisp.dhis.setting.SettingKey.HELP_PAGE_LINK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Stian Strandli
 * @author Lars Helge Overland
 */
class SystemSettingManagerTest extends SingleSetupIntegrationTestBase {

  @Autowired private SystemSettingManager systemSettingManager;

  @Override
  public void setUpTest() {
    systemSettingManager.invalidateCache();
  }

  @Test
  void testSaveGetSetting() {
    systemSettingManager.saveSystemSetting(APPLICATION_INTRO, "valueA");
    systemSettingManager.saveSystemSetting(APPLICATION_NOTIFICATION, "valueB");
    assertEquals("valueA", systemSettingManager.getStringSetting(APPLICATION_INTRO));
    assertEquals("valueB", systemSettingManager.getStringSetting(APPLICATION_NOTIFICATION));
  }

  @Test
  void testSaveGetSettingWithDefault() {
    assertEquals(EMAIL_PORT.getDefaultValue(), systemSettingManager.getIntegerSetting(EMAIL_PORT));
  }

  @Test
  void testSaveGetDeleteSetting() {
    assertNull(systemSettingManager.getStringSetting(APPLICATION_INTRO));
    assertEquals(
        HELP_PAGE_LINK.getDefaultValue(), systemSettingManager.getStringSetting(HELP_PAGE_LINK));
    systemSettingManager.saveSystemSetting(APPLICATION_INTRO, "valueA");
    systemSettingManager.saveSystemSetting(HELP_PAGE_LINK, "valueB");
    assertEquals("valueA", systemSettingManager.getStringSetting(APPLICATION_INTRO));
    assertEquals("valueB", systemSettingManager.getStringSetting(HELP_PAGE_LINK));
    systemSettingManager.deleteSystemSetting(APPLICATION_INTRO);
    assertNull(systemSettingManager.getStringSetting(APPLICATION_INTRO));
    assertEquals("valueB", systemSettingManager.getStringSetting(HELP_PAGE_LINK));
    systemSettingManager.deleteSystemSetting(HELP_PAGE_LINK);
    assertNull(systemSettingManager.getStringSetting(APPLICATION_INTRO));
    assertEquals(
        HELP_PAGE_LINK.getDefaultValue(), systemSettingManager.getStringSetting(HELP_PAGE_LINK));
  }

  @Test
  void testGetAllSystemSettings() {
    systemSettingManager.saveSystemSetting(APPLICATION_INTRO, "valueA");
    systemSettingManager.saveSystemSetting(APPLICATION_NOTIFICATION, "valueB");
    List<SystemSetting> settings = systemSettingManager.getAllSystemSettings();
    assertNotNull(settings);
    assertEquals(2, settings.size());
  }

  @Test
  void testGetSystemSettingsAsMap() {
    systemSettingManager.saveSystemSetting(SettingKey.APPLICATION_TITLE, "valueA");
    systemSettingManager.saveSystemSetting(SettingKey.APPLICATION_NOTIFICATION, "valueB");
    Map<String, Serializable> settingsMap = systemSettingManager.getSystemSettingsAsMap();
    assertTrue(settingsMap.containsKey(SettingKey.APPLICATION_TITLE.getName()));
    assertTrue(settingsMap.containsKey(SettingKey.APPLICATION_NOTIFICATION.getName()));
    assertEquals("valueA", settingsMap.get(SettingKey.APPLICATION_TITLE.getName()));
    assertEquals("valueB", settingsMap.get(SettingKey.APPLICATION_NOTIFICATION.getName()));
    assertEquals(
        SettingKey.CACHE_STRATEGY.getDefaultValue(),
        settingsMap.get(SettingKey.CACHE_STRATEGY.getName()));
    assertEquals(
        SettingKey.CREDENTIALS_EXPIRES.getDefaultValue(),
        settingsMap.get(SettingKey.CREDENTIALS_EXPIRES.getName()));
  }

  @Test
  void testGetSystemSettingsByCollection() {
    Collection<SettingKey> keys =
        ImmutableSet.of(SettingKey.APPLICATION_TITLE, SettingKey.APPLICATION_INTRO);
    systemSettingManager.saveSystemSetting(APPLICATION_TITLE, "valueA");
    systemSettingManager.saveSystemSetting(APPLICATION_INTRO, "valueB");
    assertEquals(2, systemSettingManager.getSystemSettings(keys).size());
  }

  @Test
  void testIsConfidential() {
    assertTrue(EMAIL_PASSWORD.isConfidential());
    assertTrue(systemSettingManager.isConfidential(EMAIL_PASSWORD.getName()));
    assertFalse(EMAIL_HOST_NAME.isConfidential());
    assertFalse(systemSettingManager.isConfidential(EMAIL_HOST_NAME.getName()));
  }

  @Test
  void testGetBoolean() {
    systemSettingManager.saveSystemSetting(
        SettingKey.CAN_GRANT_OWN_USER_ROLES, Boolean.valueOf("true"));
    assertTrue(systemSettingManager.getBoolSetting(SettingKey.CAN_GRANT_OWN_USER_ROLES));
  }
}
