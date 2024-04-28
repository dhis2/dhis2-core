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
package org.hisp.dhis.analytics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.setting.SystemSettingManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsExportSettingsTest {

  @Mock private DhisConfigurationProvider config;

  @Mock private SystemSettingManager systemSettings;

  private AnalyticsExportSettings settings;

  @BeforeEach
  public void before() {
    settings = new AnalyticsExportSettings(config, systemSettings);
  }

  @Test
  void testSkipIndexCategoryColumns() {
    when(config.isEnabled(ConfigurationKey.ANALYTICS_TABLE_INDEX_DATA_ELEMENT_GROUP_SET))
        .thenReturn(true);
    when(config.isEnabled(ConfigurationKey.ANALYTICS_TABLE_INDEX_CATEGORY)).thenReturn(true);
    when(config.isEnabled(ConfigurationKey.ANALYTICS_TABLE_INDEX_CATEGORY_OPTION_GROUP_SET))
        .thenReturn(false);
    when(config.isEnabled(ConfigurationKey.ANALYTICS_TABLE_INDEX_ORG_UNIT_GROUP_SET))
        .thenReturn(false);

    assertTrue(settings.skipIndexDataElementGroupSetColumns());
    assertTrue(settings.skipIndexCategoryColumns());
    assertFalse(settings.skipIndexCategoryOptionGroupSetColumns());
    assertFalse(settings.skipIndexOrgUnitGroupSetColumns());
  }
}
