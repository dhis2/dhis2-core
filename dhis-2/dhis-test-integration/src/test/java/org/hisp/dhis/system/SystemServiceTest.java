/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hisp.dhis.system.SystemInfo.SystemInfoForAppCacheFilter;
import org.hisp.dhis.system.SystemInfo.SystemInfoForDataStats;
import org.hisp.dhis.system.SystemInfo.SystemInfoForMetadataExport;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class SystemServiceTest extends PostgresIntegrationTestBase {

  @Autowired private SystemService systemService;

  @Test
  @DisplayName("System info for metadata export has expected values")
  void systemInfoForMetadataTest() {
    // when
    SystemInfoForMetadataExport info = systemService.getSystemInfoForMetadataExport();

    // then
    assertEquals("123", info.version());
    assertEquals("abc1234", info.revision());
    assertNotNull(info.serverDate().toString());
    // assert for system id left out, can't populate correctly due to how we load at startup
  }

  @Test
  @DisplayName("System info for data stats has expected values")
  void systemInfoForDataStatsTest() {
    // when
    SystemInfoForDataStats info = systemService.getSystemInfoForDataStats();

    // then
    assertEquals("123", info.version());
    assertEquals("abc1234", info.revision());
    assertNotNull(info.serverDate());
    assertNotNull(info.buildTime());
    // assert for system id left out, can't populate correctly due to how we load at startup
  }

  @Test
  @DisplayName("System info for app cache filter has expected values")
  void systemInfoForAppCacheFilterTest() {
    // when
    SystemInfoForAppCacheFilter info = systemService.getSystemInfoForAppCacheFilter();

    // then
    assertEquals("123", info.version());
    assertEquals("abc1234", info.revision());
    assertEquals("iso8601", info.calendar());
  }

  @Test
  @DisplayName("System info version has expected value")
  void systemInfoVersionTest() {
    String infoVersion = systemService.getSystemInfoVersion();
    assertEquals("123", infoVersion);
  }
}
