/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.webapi.controller.dataintegrity;

import java.util.Properties;
import java.util.Set;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.setting.SystemSettingStore;
import org.hisp.dhis.setting.SystemSettingsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Integrity checks for {@code server.base.url} configuration:
 *
 * <ol>
 *   <li>{@code server_base_url_invalid} — detects when {@code server.base.url} is absent from
 *       {@code dhis.conf}, is not a valid absolute HTTP or HTTPS URL, or ends with a trailing
 *       slash.
 *   <li>{@code server_base_url_mismatch} — detects when {@code server.base.url} in {@code
 *       dhis.conf} and {@code keyInstanceBaseUrl} in the database system settings have different
 *       values.
 * </ol>
 *
 * @author Jason P. Pickering
 */
class DataIntegrityConfigurationServerBaseUrlControllerTest
    extends AbstractDataIntegrityIntegrationTest {

  private static final String CHECK_INVALID = "server_base_url_invalid";
  private static final String CHECK_MISMATCH = "server_base_url_mismatch";

  private static final String INSTANCE_URL = "https://dhis2.example.org/dhis";
  private static final String OTHER_URL = "https://other.example.org/dhis";

  @Autowired private DhisConfigurationProvider dhisConfigurationProvider;
  @Autowired private SystemSettingStore systemSettingStore;
  @Autowired private SystemSettingsService systemSettingsService;

  @AfterEach
  void resetServerBaseUrl() {
    Properties props =
        (Properties) ReflectionTestUtils.getField(dhisConfigurationProvider, "properties");
    if (props != null) {
      props.remove(ConfigurationKey.SERVER_BASE_URL.getKey());
    }
    systemSettingStore.delete(Set.of("keyInstanceBaseUrl"));
    invalidateSettingsCache();
  }

  // --- server_base_url_invalid ---

  @Test
  void testServerBaseUrlNotSetDetected() {
    assertHasDataIntegrityIssues(
        null, CHECK_INVALID, 0, (String) null, "server.base.url", null, false);
  }

  @Test
  void testServerBaseUrlNoSchemeDetected() {
    setConfigUrl("dhis2.example.org/dhis");
    assertHasDataIntegrityIssues(
        null, CHECK_INVALID, 0, (String) null, "dhis2.example.org/dhis", null, false);
  }

  @Test
  void testServerBaseUrlPlainStringDetected() {
    setConfigUrl("123456");
    assertHasDataIntegrityIssues(null, CHECK_INVALID, 0, (String) null, "123456", null, false);
  }

  @Test
  void testServerBaseUrlNonHttpSchemeDetected() {
    setConfigUrl("ftp://dhis2.example.org/dhis");
    assertHasDataIntegrityIssues(
        null, CHECK_INVALID, 0, (String) null, "ftp://dhis2.example.org/dhis", null, false);
  }

  @Test
  void testServerBaseUrlTrailingSlashDetected() {
    setConfigUrl(INSTANCE_URL + "/");
    assertHasDataIntegrityIssues(
        null, CHECK_INVALID, 0, (String) null, INSTANCE_URL + "/", null, false);
  }

  @Test
  void testServerBaseUrlValidHttpsNoIssue() {
    setConfigUrl(INSTANCE_URL);
    assertHasNoDataIntegrityIssues(null, CHECK_INVALID, false);
  }

  @Test
  void testServerBaseUrlValidHttpNoIssue() {
    setConfigUrl("http://dhis2.example.org/dhis");
    assertHasNoDataIntegrityIssues(null, CHECK_INVALID, false);
  }

  // --- server_base_url_mismatch ---

  @Test
  void testServerBaseUrlMismatchDetected() {
    setConfigUrl(INSTANCE_URL);
    setDbUrl(OTHER_URL);
    assertHasDataIntegrityIssues(null, CHECK_MISMATCH, 0, (String) null, OTHER_URL, null, false);
  }

  @Test
  void testServerBaseUrlMatchNoIssue() {
    setConfigUrl(INSTANCE_URL);
    setDbUrl(INSTANCE_URL);
    assertHasNoDataIntegrityIssues(null, CHECK_MISMATCH, false);
  }

  @Test
  void testServerBaseUrlMismatchTrailingSlashIgnored() {
    setConfigUrl(INSTANCE_URL + "/");
    setDbUrl(INSTANCE_URL);
    assertHasNoDataIntegrityIssues(null, CHECK_MISMATCH, false);
  }

  @Test
  void testServerBaseUrlMismatchNoDbSettingNoIssue() {
    setConfigUrl(INSTANCE_URL);
    // keyInstanceBaseUrl absent from DB — nothing to mismatch against
    assertHasNoDataIntegrityIssues(null, CHECK_MISMATCH, false);
  }

  @Test
  void testServerBaseUrlMismatchNoConfigNoIssue() {
    // server.base.url not configured — the not-set check covers this case
    setDbUrl(OTHER_URL);
    assertHasNoDataIntegrityIssues(null, CHECK_MISMATCH, false);
  }

  // --- helpers ---

  private void setConfigUrl(String url) {
    Properties props =
        (Properties) ReflectionTestUtils.getField(dhisConfigurationProvider, "properties");
    props.setProperty(ConfigurationKey.SERVER_BASE_URL.getKey(), url);
  }

  private void setDbUrl(String url) {
    systemSettingStore.put("keyInstanceBaseUrl", url);
    invalidateSettingsCache();
  }

  /**
   * {@code systemSettingStore.put()} bypasses the service and does not null out the {@code
   * allSettings} long-lived cache in {@link org.hisp.dhis.setting.DefaultSystemSettingsService}. We
   * must do it explicitly so that the next call to {@code getCurrentSettings()} reloads from the
   * database.
   */
  private void invalidateSettingsCache() {
    ReflectionTestUtils.setField(systemSettingsService, "allSettings", null);
    systemSettingsService.clearCurrentSettings();
  }
}
