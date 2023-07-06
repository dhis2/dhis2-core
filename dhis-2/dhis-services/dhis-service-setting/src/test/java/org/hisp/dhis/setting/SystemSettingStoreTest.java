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

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.hisp.dhis.DhisSpringTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Stian Strandli
 */
class SystemSettingStoreTest extends DhisSpringTest {

  @Autowired private SystemSettingStore systemSettingStore;

  private SystemSetting settingA;

  private SystemSetting settingB;

  private SystemSetting settingC;

  @Override
  public void setUpTest() throws Exception {
    settingA = new SystemSetting();
    settingA.setName("Setting1");
    settingA.setDisplayValue("Value1");
    settingB = new SystemSetting();
    settingB.setName("Setting2");
    settingB.setDisplayValue("Value2");
    settingC = new SystemSetting();
    settingC.setName("Setting3");
    settingC.setDisplayValue("Value3");
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
    assertEquals("Value1", settingA.getDisplayValue());
    settingA.setDisplayValue("Value1.1");
    systemSettingStore.update(settingA);
    settingA = systemSettingStore.get(idA);
    assertNotNull(settingA);
    assertEquals("Setting1", settingA.getName());
    assertEquals("Value1.1", settingA.getDisplayValue());
  }

  @Test
  void testUpdateSystemSetting() {
    systemSettingStore.save(settingA);
    long id = settingA.getId();
    settingA = systemSettingStore.get(id);
    assertEquals("Value1", settingA.getDisplayValue());
    settingA.setDisplayValue("Value2");
    systemSettingStore.update(settingA);
    settingA = systemSettingStore.get(id);
    assertEquals("Value2", settingA.getDisplayValue());
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
  void testGetSystemSetting() {
    systemSettingStore.save(settingA);
    systemSettingStore.save(settingB);
    SystemSetting s = systemSettingStore.getByName("Setting1");
    assertNotNull(s);
    assertEquals("Setting1", s.getName());
    assertEquals("Value1", s.getDisplayValue());
    s = systemSettingStore.getByName("Setting3");
    assertNull(s);
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
}
