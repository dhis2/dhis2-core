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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Stian Strandli
 */
@Transactional
class SystemSettingStoreTest extends PostgresIntegrationTestBase {

  @Autowired private SystemSettingStore systemSettingStore;

  private SystemSetting settingA;

  private SystemSetting settingB;

  private SystemSetting settingC;

  @BeforeEach
  void setUp() {
    settingA = new SystemSetting("Setting1", "Value1");
    settingB = new SystemSetting("Setting2", "Value2");
    settingC = new SystemSetting("Setting3", "Value3");
  }

  @Test
  void testAddSystemSetting() {
    systemSettingStore.save(settingA);
    long idA = settingA.getId();
    systemSettingStore.save(settingB);
    systemSettingStore.save(settingC);
    settingA = systemSettingStore.get(idA);
    assertNotNull(settingA);
    assertEquals("Setting1", settingA.getName());
    assertEquals("Value1", settingA.getValue());
    systemSettingStore.save(new SystemSetting(settingA.getName(), "Value1.1"));
    settingA = systemSettingStore.get(idA);
    assertNotNull(settingA);
    assertEquals("Setting1", settingA.getName());
    assertEquals("Value1.1", settingA.getValue());
  }

  @Test
  void testUpdateSystemSetting() {
    systemSettingStore.save(settingA);
    long id = settingA.getId();
    settingA = systemSettingStore.get(id);
    assertEquals("Value1", settingA.getValue());
    systemSettingStore.save(new SystemSetting(settingA.getName(), "Value2"));
    settingA = systemSettingStore.get(id);
    assertEquals("Value2", settingA.getValue());
  }

  @Test
  void testDeleteSystemSetting() {
    systemSettingStore.save(settingA);
    long idA = settingA.getId();
    systemSettingStore.save(settingB);
    long idB = settingB.getId();
    systemSettingStore.save(settingC);
    systemSettingStore.delete(settingA);
    assertNull(systemSettingStore.get(idA));
    assertNotNull(systemSettingStore.get(idB));
  }

  @Test
  void testGetAllSystemSettings() {
    List<SystemSetting> settings = systemSettingStore.getAll();
    assertNotNull(settings);
    assertEquals(0, settings.size());
    systemSettingStore.save(settingA);
    systemSettingStore.save(settingB);
    settings = systemSettingStore.getAll();
    assertNotNull(settings);
    assertEquals(2, settings.size());
  }

  @Test
  void testGetAllSystemSettingsAsMap() {
    Map<String, String> settings = systemSettingStore.getAllSettings();
    assertNotNull(settings);
    assertEquals(0, settings.size());
    systemSettingStore.save(settingA);
    systemSettingStore.save(settingB);
    settings = systemSettingStore.getAllSettings();
    assertNotNull(settings);
    assertEquals(Map.of("Setting1", "Value1", "Setting2", "Value2"), settings);
  }
}
