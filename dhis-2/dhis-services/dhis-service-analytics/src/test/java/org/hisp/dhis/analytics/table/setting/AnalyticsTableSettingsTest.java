/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics.table.setting;

import static org.hisp.dhis.external.conf.ConfigurationKey.CITUS_EXTENSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.configuration.CitusSettings;
import org.hisp.dhis.configuration.CitusSettings.PgExtension;
import org.hisp.dhis.db.model.Database;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.setting.SystemSettingManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class AnalyticsTableSettingsTest {
  @Mock private DhisConfigurationProvider config;

  @Mock private JdbcTemplate jdbcTemplate;

  @Mock private SystemSettingManager systemSettings;

  private AnalyticsTableSettings settings;

  private CitusSettings citusSettings;

  @BeforeEach
  public void before() {
    citusSettings = new CitusSettings(config, jdbcTemplate);
    settings = new AnalyticsTableSettings(config, systemSettings, citusSettings);
  }

  @Test
  void testGetAndValidateDatabase() {
    assertEquals(Database.POSTGRESQL, settings.getAndValidateDatabase("POSTGRESQL"));
  }

  @Test
  void testGetAndValidateInvalidDatabase() {
    assertThrows(IllegalArgumentException.class, () -> settings.getAndValidateDatabase("ORACLE"));
  }

  @Test
  void testGetAnalyticsDatabase() {
    when(config.getProperty(ConfigurationKey.ANALYTICS_DATABASE))
        .thenReturn(ConfigurationKey.ANALYTICS_DATABASE.getDefaultValue());

    assertEquals(Database.POSTGRESQL, settings.getAnalyticsDatabase());
  }

  @Test
  void testIsCitusEnabledWhenDisabledByConfig() {
    when(config.isEnabled(CITUS_EXTENSION)).thenReturn(false);
    assertFalse(citusSettings.isCitusExtensionEnabled());
  }

  @Test
  void testIsCitusEnabledWhenNoCitusExtensionInstalled() {
    when(config.isEnabled(CITUS_EXTENSION)).thenReturn(true);
    mockTemplate(List.of());
    assertFalse(citusSettings.isCitusExtensionEnabled());
  }

  @Test
  void testIsCitusEnabledWhenInstalledButNotCreated() {
    when(config.isEnabled(CITUS_EXTENSION)).thenReturn(true);
    mockTemplate(List.of(new PgExtension("citus", null)));
    assertFalse(citusSettings.isCitusExtensionEnabled());
  }

  @Test
  void testIsCitusEnabledWhenInstalledAndCreated() {
    when(config.isEnabled(CITUS_EXTENSION)).thenReturn(true);
    mockTemplate(List.of(new PgExtension("citus", "V1.0")));
    assertTrue(citusSettings.isCitusExtensionEnabled());
  }

  private void mockTemplate(List<PgExtension> objects) {
    when(jdbcTemplate.query(any(String.class), any(RowMapper.class))).thenReturn(objects);
  }
}
