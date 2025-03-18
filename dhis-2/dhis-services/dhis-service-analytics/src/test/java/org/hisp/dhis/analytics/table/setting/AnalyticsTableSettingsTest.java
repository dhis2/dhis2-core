/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics.table.setting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.hisp.dhis.analytics.table.model.Skip;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.setting.SystemSettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsTableSettingsTest {
  @Mock private DhisConfigurationProvider config;

  @Mock private SystemSettingsService systemSettings;

  @InjectMocks private AnalyticsTableSettings settings;

  @Test
  void testGetSkipIndexDimensionsDefault() {
    when(config.getProperty(ConfigurationKey.ANALYTICS_TABLE_SKIP_INDEX))
        .thenReturn(ConfigurationKey.ANALYTICS_TABLE_SKIP_INDEX.getDefaultValue());

    assertEquals(Set.of(), settings.getSkipIndexDimensions());
  }

  @Test
  void testGetSkipIndexDimensions() {
    when(config.getProperty(ConfigurationKey.ANALYTICS_TABLE_SKIP_INDEX))
        .thenReturn("kJ7yGrfR413, Hg5tGfr2fas  , Ju71jG19Kaq,b5TgfRL9pUq");

    assertEquals(
        Set.of("kJ7yGrfR413", "Hg5tGfr2fas", "Ju71jG19Kaq", "b5TgfRL9pUq"),
        settings.getSkipIndexDimensions());
  }

  @Test
  void testGetSkipColumnDimensions() {
    when(config.getProperty(ConfigurationKey.ANALYTICS_TABLE_SKIP_COLUMN))
        .thenReturn("sixmonthlyapril, financialapril  , financialjuly,financialnov");

    assertEquals(
        Set.of("sixmonthlyapril", "financialapril", "financialjuly", "financialnov"),
        settings.getSkipColumnDimensions());
  }

  @Test
  void testToSet() {
    Set<String> expected = Set.of("kJ7yGrfR413", "Hg5tGfr2fas", "Ju71jG19Kaq", "b5TgfRL9pUq");
    assertEquals(expected, settings.toSet("kJ7yGrfR413, Hg5tGfr2fas  , Ju71jG19Kaq,b5TgfRL9pUq"));
  }

  @Test
  void testToSkip() {
    assertEquals(Skip.INCLUDE, settings.toSkip(true));
    assertEquals(Skip.SKIP, settings.toSkip(false));
  }
}
