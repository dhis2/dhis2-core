/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.config;

import static org.hisp.dhis.external.conf.ConfigurationKey.USE_QUERY_CACHE;
import static org.hisp.dhis.external.conf.ConfigurationKey.USE_SECOND_LEVEL_CACHE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Properties;
import org.hibernate.cache.ehcache.internal.EhcacheRegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Tests for {@link HibernateConfig}, verifying that the second level cache and query cache settings
 * from dhis.conf are propagated explicitly to Hibernate, and that all boolean spellings allowed in
 * dhis.conf (true/TRUE/on/ON) are recognized. Before this was in place, only the exact spelling
 * 'true' enabled the second level cache, so admins using TRUE/on/ON silently ran without it.
 *
 * @author Morten Svanæs
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HibernateConfigTest {

  @Mock private DhisConfigurationProvider dhisConfig;

  @BeforeEach
  void setUp() {
    // Use the real isEnabled()/isDisabled() default methods so the dhis.conf boolean
    // spellings are interpreted exactly as in production
    when(dhisConfig.isEnabled(any())).thenCallRealMethod();
  }

  @ParameterizedTest
  @ValueSource(strings = {"true", "TRUE", "on", "ON"})
  void secondLevelCacheEnabledForAllAllowedSpellings(String configValue) {
    when(dhisConfig.getProperty(USE_SECOND_LEVEL_CACHE)).thenReturn(configValue);
    when(dhisConfig.getProperty(USE_QUERY_CACHE)).thenReturn("true");

    Properties properties = HibernateConfig.getAdditionalProperties(dhisConfig);

    assertEquals("true", properties.get(AvailableSettings.USE_SECOND_LEVEL_CACHE));
    assertEquals("true", properties.get(AvailableSettings.USE_QUERY_CACHE));
    assertEquals(
        EhcacheRegionFactory.class.getName(),
        properties.get(AvailableSettings.CACHE_REGION_FACTORY));
  }

  @ParameterizedTest
  @ValueSource(strings = {"true", "TRUE", "on", "ON"})
  void queryCacheEnabledForAllAllowedSpellings(String configValue) {
    when(dhisConfig.getProperty(USE_SECOND_LEVEL_CACHE)).thenReturn("true");
    when(dhisConfig.getProperty(USE_QUERY_CACHE)).thenReturn(configValue);

    Properties properties = HibernateConfig.getAdditionalProperties(dhisConfig);

    // Hibernate parses this value itself, so it must be normalized to true/false
    assertEquals("true", properties.get(AvailableSettings.USE_QUERY_CACHE));
  }

  @Test
  void queryCacheDisabledWhileSecondLevelCacheEnabled() {
    when(dhisConfig.getProperty(USE_SECOND_LEVEL_CACHE)).thenReturn("true");
    when(dhisConfig.getProperty(USE_QUERY_CACHE)).thenReturn("false");

    Properties properties = HibernateConfig.getAdditionalProperties(dhisConfig);

    assertEquals("true", properties.get(AvailableSettings.USE_SECOND_LEVEL_CACHE));
    assertEquals("false", properties.get(AvailableSettings.USE_QUERY_CACHE));
  }

  @ParameterizedTest
  @ValueSource(strings = {"false", "FALSE", "off", "OFF", ""})
  void secondLevelCacheDisabledDisablesBothCachesExplicitly(String configValue) {
    when(dhisConfig.getProperty(USE_SECOND_LEVEL_CACHE)).thenReturn(configValue);
    // An enabled query cache must not survive the second level cache being disabled
    when(dhisConfig.getProperty(USE_QUERY_CACHE)).thenReturn("true");

    Properties properties = HibernateConfig.getAdditionalProperties(dhisConfig);

    assertEquals("false", properties.get(AvailableSettings.USE_SECOND_LEVEL_CACHE));
    assertEquals("false", properties.get(AvailableSettings.USE_QUERY_CACHE));
    assertFalse(properties.containsKey(AvailableSettings.CACHE_REGION_FACTORY));
  }
}
