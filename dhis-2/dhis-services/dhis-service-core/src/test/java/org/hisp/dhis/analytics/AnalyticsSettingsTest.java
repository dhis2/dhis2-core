/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics;

import static org.hisp.dhis.external.conf.ConfigurationKey.CITUS_EXTENSION_DISABLED;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.hisp.dhis.analytics.AnalyticsSettings.PgExtension;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class AnalyticsSettingsTest {

  @Mock private DhisConfigurationProvider dhisConfigurationProvider;
  private AnalyticsSettings analyticsSettings;

  @BeforeEach
  public void setUp() {
    analyticsSettings = new AnalyticsSettings(dhisConfigurationProvider);
  }

  @Test
  void testIsCitusEnabledWhenDisabledByConfig() {
    when(dhisConfigurationProvider.isEnabled(CITUS_EXTENSION_DISABLED)).thenReturn(true);
    assertFalse(analyticsSettings.isCitusEnabled(null));
  }

  @Test
  void testIsCitusEnabledWhenNoCitusExtensionInstalled() {
    when(dhisConfigurationProvider.isEnabled(CITUS_EXTENSION_DISABLED)).thenReturn(false);
    JdbcTemplate mockedJdbcTemplate = mockTemplate(Collections.emptyList());
    assertFalse(analyticsSettings.isCitusEnabled(mockedJdbcTemplate));
  }

  @Test
  void testIsCitusEnabledWhenInstalledButNotCreated() {
    when(dhisConfigurationProvider.isEnabled(CITUS_EXTENSION_DISABLED)).thenReturn(false);
    JdbcTemplate mockedJdbcTemplate = mockTemplate(List.of(new PgExtension("citus", null)));
    assertFalse(analyticsSettings.isCitusEnabled(mockedJdbcTemplate));
  }

  @Test
  void testIsCitusEnabledWhenInstalledAndCreated() {
    when(dhisConfigurationProvider.isEnabled(CITUS_EXTENSION_DISABLED)).thenReturn(false);
    JdbcTemplate mockedJdbcTemplate = mockTemplate(List.of(new PgExtension("citus", "V1.0")));
    assertTrue(analyticsSettings.isCitusEnabled(mockedJdbcTemplate));
  }

  private JdbcTemplate mockTemplate(List<PgExtension> objects) {
    JdbcTemplate mockedJdbcTemplate = mock(JdbcTemplate.class);
    when(mockedJdbcTemplate.queryForList(any(String.class), any(Class.class))).thenReturn(objects);
    return mockedJdbcTemplate;
  }
}
