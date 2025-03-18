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
package org.hisp.dhis.dxf2.metadata.sync;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import org.hisp.dhis.dxf2.metadata.systemsettings.DefaultMetadataSystemSettingService;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author anilkumk
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class MetadataSystemSettingServiceTest extends PostgresIntegrationTestBase {
  @Autowired SystemSettingsService settingsService;

  @Autowired DefaultMetadataSystemSettingService metadataSystemSettingService;

  @BeforeEach
  public void setup() throws Exception {
    settingsService.putAll(
        Map.ofEntries(
            entry("keyRemoteInstanceUrl", "http://localhost:9080"),
            entry("keyRemoteInstanceUsername", "username"),
            entry("keyRemoteInstancePassword", "password"),
            entry("keyStopMetadataSync", "true")));
    settingsService.clearCurrentSettings();
  }

  @Test
  void testShouldGetRemoteUserName() {
    String remoteInstanceUserName = metadataSystemSettingService.getRemoteInstanceUserName();

    assertEquals("username", remoteInstanceUserName);
  }

  @Test
  void testShouldGetRemotePassword() {
    String remoteInstancePassword = metadataSystemSettingService.getRemoteInstancePassword();

    assertEquals("password", remoteInstancePassword);
  }

  @Test
  void testShouldDownloadMetadataVersionForGivenVersionName() {
    String downloadVersionUrl = metadataSystemSettingService.getVersionDetailsUrl("Version_Name");

    assertEquals(
        "http://localhost:9080/api/metadata/version?versionName=Version_Name", downloadVersionUrl);
  }

  @Test
  void testShouldDownloadMetadataVersionSnapshotForGivenVersionName() {
    String downloadVersionUrl =
        metadataSystemSettingService.getDownloadVersionSnapshotURL("Version_Name");

    assertEquals(
        "http://localhost:9080/api/metadata/version/Version_Name/data.gz", downloadVersionUrl);
  }

  @Test
  void testShouldGetAllVersionsCreatedAfterTheGivenVersionName() {
    String metadataDifferenceUrl =
        metadataSystemSettingService.getMetaDataDifferenceURL("Version_Name");

    assertEquals(
        "http://localhost:9080/api/metadata/version/history?baseline=Version_Name",
        metadataDifferenceUrl);
  }

  @Test
  void testShouldGetEntireVersionHistoryWhenNoVersionNameIsGiven() {
    String versionHistoryUrl = metadataSystemSettingService.getEntireVersionHistory();

    assertEquals("http://localhost:9080/api/metadata/version/history", versionHistoryUrl);
  }

  @Test
  void testShouldGetStopMetadataSyncSettingValue() {
    boolean stopMetadataSync = metadataSystemSettingService.getStopMetadataSyncSetting();

    assertTrue(stopMetadataSync);
  }

  @Test
  void testShouldReturnFalseIfStopMetadataSyncSettingValueIsNull() {
    settingsService.deleteAll(Set.of("keyStopMetadataSync"));
    boolean stopMetadataSync = metadataSystemSettingService.getStopMetadataSyncSetting();

    assertFalse(stopMetadataSync);
  }
}
