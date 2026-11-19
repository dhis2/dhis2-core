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
package org.hisp.dhis.startup;

import static org.hisp.dhis.period.PeriodTypeEnum.MONTHLY;
import static org.hisp.dhis.period.PeriodTypeEnum.QUARTERLY;
import static org.hisp.dhis.period.PeriodTypeEnum.YEARLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.encryption.EncryptionStatus;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.period.PeriodTypeStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link ConfigurationPopulator}.
 *
 * <p>The startup routine only logs warnings for misconfigured properties. These tests verify that
 * each validation path completes without error and does not modify the existing configuration.
 */
@ExtendWith(MockitoExtension.class)
class ConfigurationPopulatorTest {

  @Mock private ConfigurationService configurationService;
  @Mock private DhisConfigurationProvider dhisConfigurationProvider;
  @Mock private PeriodTypeStore periodTypeStore;

  private ConfigurationPopulator populator;

  private Configuration config;

  @BeforeEach
  void setUp() {
    populator =
        new ConfigurationPopulator(
            configurationService, dhisConfigurationProvider, periodTypeStore);

    // Stubs required by executeInTransaction() regardless of the test scenario
    when(dhisConfigurationProvider.getEncryptionStatus()).thenReturn(EncryptionStatus.OK);
    config = new Configuration();
    config.setSystemId("existing-id");
    config.setDataOutputPeriodTypes(types(YEARLY));
    when(configurationService.getConfiguration()).thenReturn(config);
    lenient()
        .when(periodTypeStore.getAll())
        .thenReturn(List.copyOf(types(MONTHLY, QUARTERLY, YEARLY)));
  }

  private static Set<PeriodType> types(PeriodTypeEnum... types) {
    Set<PeriodType> set = new LinkedHashSet<>();
    for (PeriodTypeEnum type : types) {
      set.add(PeriodType.getPeriodType(type));
    }
    return set;
  }

  private Configuration captureSavedConfiguration() {
    ArgumentCaptor<Configuration> captor = ArgumentCaptor.forClass(Configuration.class);
    verify(configurationService).setConfiguration(captor.capture());
    return captor.getValue();
  }

  @Test
  void testBlankDatabaseEnablesAllDataOutputPeriodTypes() {
    config.setDataOutputPeriodTypes(new LinkedHashSet<>());

    populator.executeInTransaction();

    assertEquals(
        types(MONTHLY, QUARTERLY, YEARLY), captureSavedConfiguration().getDataOutputPeriodTypes());
  }

  @Test
  void testYearlyIsAddedWhenMissing() {
    config.setDataOutputPeriodTypes(types(MONTHLY));

    populator.executeInTransaction();

    Set<PeriodType> saved = captureSavedConfiguration().getDataOutputPeriodTypes();
    assertEquals(2, saved.size());
    assertTrue(saved.contains(PeriodType.getPeriodType(MONTHLY)));
    assertTrue(saved.contains(PeriodType.getPeriodType(YEARLY)));
  }

  @Test
  void testConfiguredTypesWithYearlyAreLeftUntouched() {
    config.setDataOutputPeriodTypes(types(MONTHLY, YEARLY));

    populator.executeInTransaction();

    verify(configurationService, never()).setConfiguration(any());
  }

  static Stream<Arguments> baseUrlScenarios() {
    return Stream.of(
        Arguments.of("valid URL", "https://dhis2.example.org"),
        Arguments.of("valid URL with path", "https://dhis2.example.org/dhis"),
        Arguments.of("missing URL", null),
        Arguments.of("invalid URL", "not-a-url"),
        Arguments.of("URL with trailing slash", "https://dhis2.example.org/dhis/"));
  }

  @ParameterizedTest(name = "{0}: {1}")
  @MethodSource("baseUrlScenarios")
  void testBaseUrlValidation(String scenario, String baseUrl) {
    when(dhisConfigurationProvider.getServerBaseUrl()).thenReturn(baseUrl);

    populator.executeInTransaction();

    verify(dhisConfigurationProvider).getServerBaseUrl();
    verify(configurationService, never()).setConfiguration(any());
  }
}
