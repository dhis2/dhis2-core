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
package org.hisp.dhis.dxf2.sync;

import static java.util.Map.entry;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Map;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author David Katuscak <katuscak.d@gmail.com>
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class SyncUtilsTest extends PostgresIntegrationTestBase {

  private static final String USERNAME = "user";

  private static final String PASSWORD = "pass";

  private static final String URL = "https://localhost:8080";

  @Autowired SystemSettingsService settingsService;

  @Test
  void getRemoteInstanceTest() throws Exception {
    settingsService.putAll(
        Map.ofEntries(
            entry("keyRemoteInstanceUsername", USERNAME),
            entry("keyRemoteInstancePassword", PASSWORD),
            entry("keyRemoteInstanceUrl", URL)));
    settingsService.clearCurrentSettings();
    SystemInstance systemInstance =
        SyncUtils.getRemoteInstance(
            settingsService.getCurrentSettings(), SyncEndpoint.DATA_VALUE_SETS);
    assertThat(systemInstance.getUsername(), is(USERNAME));
    assertThat(systemInstance.getPassword(), is(PASSWORD));
    assertThat(systemInstance.getUrl(), is(URL + SyncEndpoint.DATA_VALUE_SETS.getPath()));
  }
}
