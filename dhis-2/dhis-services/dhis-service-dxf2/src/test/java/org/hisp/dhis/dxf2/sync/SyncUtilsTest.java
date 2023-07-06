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
package org.hisp.dhis.dxf2.sync;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dxf2.synch.SystemInstance;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David Katuscak <katuscak.d@gmail.com>
 */
class SyncUtilsTest extends DhisSpringTest {

  private static final String USERNAME = "user";

  private static final String PASSWORD = "pass";

  private static final String URL = "https://localhost:8080";

  private static final String EVENTS_URL = URL + SyncEndpoint.EVENTS.getPath();

  private static final String EVENTS_URL_WITH_SYNC_STRATEGY =
      EVENTS_URL + SyncUtils.IMPORT_STRATEGY_SYNC_SUFFIX;

  @Autowired SystemSettingManager systemSettingManager;

  @Test
  void getRemoteInstanceTest() {
    systemSettingManager.saveSystemSetting(SettingKey.REMOTE_INSTANCE_USERNAME, USERNAME);
    systemSettingManager.saveSystemSetting(SettingKey.REMOTE_INSTANCE_PASSWORD, PASSWORD);
    systemSettingManager.saveSystemSetting(SettingKey.REMOTE_INSTANCE_URL, URL);
    SystemInstance systemInstance =
        SyncUtils.getRemoteInstance(systemSettingManager, SyncEndpoint.EVENTS);
    assertThat(systemInstance.getUsername(), is(USERNAME));
    assertThat(systemInstance.getPassword(), is(PASSWORD));
    assertThat(systemInstance.getUrl(), is(EVENTS_URL));
  }

  @Test
  void getRemoteInstanceWithSyncImportStrategyTest() {
    systemSettingManager.saveSystemSetting(SettingKey.REMOTE_INSTANCE_URL, URL);
    SystemInstance systemInstance =
        SyncUtils.getRemoteInstanceWithSyncImportStrategy(
            systemSettingManager, SyncEndpoint.EVENTS);
    assertThat(systemInstance.getUrl(), is(EVENTS_URL_WITH_SYNC_STRATEGY));
  }
}
