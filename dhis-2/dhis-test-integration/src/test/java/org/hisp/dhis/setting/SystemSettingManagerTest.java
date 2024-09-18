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
import static org.hisp.dhis.setting.SettingKey.EMAIL_PORT;
import static org.hisp.dhis.setting.SettingKey.HELP_PAGE_LINK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Stian Strandli
 * @author Lars Helge Overland
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class SystemSettingManagerTest extends PostgresIntegrationTestBase {

  @Autowired private SystemSettingManager settingManager;

  @Test
  void testSaveGetSetting() {
    settingManager.saveSystemSetting(APPLICATION_INTRO, "valueA");
    settingManager.saveSystemSetting(APPLICATION_NOTIFICATION, "valueB");
    assertEquals("valueA", settingManager.getStringSetting(APPLICATION_INTRO));
    assertEquals("valueB", settingManager.getStringSetting(APPLICATION_NOTIFICATION));
  }

  @Test
  void testSaveGetSettingWithDefault() {
    assertEquals(EMAIL_PORT.getDefaultValue(), settingManager.getCurrentSettings().getEmailPort());
  }

  @Test
  void testSaveGetDeleteSetting() {
    assertNull(settingManager.getStringSetting(APPLICATION_INTRO));
    assertEquals(
        HELP_PAGE_LINK.getDefaultValue(), settingManager.getStringSetting(HELP_PAGE_LINK));
    settingManager.saveSystemSetting(APPLICATION_INTRO, "valueA");
    settingManager.saveSystemSetting(HELP_PAGE_LINK, "valueB");
    assertEquals("valueA", settingManager.getStringSetting(APPLICATION_INTRO));
    assertEquals("valueB", settingManager.getStringSetting(HELP_PAGE_LINK));
    settingManager.deleteSystemSetting(APPLICATION_INTRO);
    assertNull(settingManager.getStringSetting(APPLICATION_INTRO));
    assertEquals("valueB", settingManager.getStringSetting(HELP_PAGE_LINK));
    settingManager.deleteSystemSetting(HELP_PAGE_LINK);
    assertNull(settingManager.getStringSetting(APPLICATION_INTRO));
    assertEquals(
        HELP_PAGE_LINK.getDefaultValue(), settingManager.getStringSetting(HELP_PAGE_LINK));
  }

  @Test
  void testGetBoolean() {
    settingManager.saveSystemSetting(
        SettingKey.CAN_GRANT_OWN_USER_ROLES, Boolean.valueOf("true"));
    assertTrue(settingManager.getBoolSetting(SettingKey.CAN_GRANT_OWN_USER_ROLES));
  }
}
