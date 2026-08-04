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
package org.hisp.dhis.config;

import static org.hisp.dhis.external.conf.ConfigurationKey.CACHE_EHCACHE_CONFIG_FILE;
import static org.hisp.dhis.external.conf.ConfigurationKey.USE_QUERY_CACHE;
import static org.hisp.dhis.external.conf.ConfigurationKey.USE_SECOND_LEVEL_CACHE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import java.util.Properties;
import org.hibernate.cache.jcache.ConfigSettings;
import org.hibernate.cache.jcache.internal.JCacheRegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link HibernateConfig}, verifying that the second level cache and query cache settings
 * from dhis.conf are propagated explicitly to Hibernate. In particular, disabling the second level
 * cache must set 'hibernate.cache.use_second_level_cache' to false, otherwise Hibernate
 * auto-enables it with default JCache settings when a RegionFactory is present on the classpath.
 *
 * @author Morten Svanæs
 */
@ExtendWith(MockitoExtension.class)
class HibernateConfigTest {

  @Mock private DhisConfigurationProvider dhisConfig;

  @Test
  void secondLevelCacheEnabled() {
    when(dhisConfig.getProperty(USE_SECOND_LEVEL_CACHE)).thenReturn("true");
    when(dhisConfig.getProperty(USE_QUERY_CACHE)).thenReturn("true");
    when(dhisConfig.getProperty(CACHE_EHCACHE_CONFIG_FILE)).thenReturn("");

    Properties properties = HibernateConfig.getAdditionalProperties(dhisConfig);

    assertEquals("true", properties.get(AvailableSettings.USE_SECOND_LEVEL_CACHE));
    assertEquals("true", properties.get(AvailableSettings.USE_QUERY_CACHE));
    assertEquals(
        JCacheRegionFactory.class.getName(),
        properties.get(AvailableSettings.CACHE_REGION_FACTORY));
    assertFalse(properties.containsKey(ConfigSettings.CONFIG_URI));
  }

  @Test
  void secondLevelCacheEnabledWithQueryCacheDisabled() {
    when(dhisConfig.getProperty(USE_SECOND_LEVEL_CACHE)).thenReturn("true");
    when(dhisConfig.getProperty(USE_QUERY_CACHE)).thenReturn("false");
    when(dhisConfig.getProperty(CACHE_EHCACHE_CONFIG_FILE)).thenReturn("");

    Properties properties = HibernateConfig.getAdditionalProperties(dhisConfig);

    assertEquals("true", properties.get(AvailableSettings.USE_SECOND_LEVEL_CACHE));
    assertEquals("false", properties.get(AvailableSettings.USE_QUERY_CACHE));
  }

  @Test
  void secondLevelCacheEnabledWithEhcacheConfigFile() {
    when(dhisConfig.getProperty(USE_SECOND_LEVEL_CACHE)).thenReturn("true");
    when(dhisConfig.getProperty(USE_QUERY_CACHE)).thenReturn("true");
    when(dhisConfig.getProperty(CACHE_EHCACHE_CONFIG_FILE))
        .thenReturn("file:/opt/dhis2/ehcache.xml");

    Properties properties = HibernateConfig.getAdditionalProperties(dhisConfig);

    assertEquals("file:/opt/dhis2/ehcache.xml", properties.get(ConfigSettings.CONFIG_URI));
  }

  @ParameterizedTest
  @ValueSource(strings = {"false", "off", "FALSE", ""})
  void secondLevelCacheDisabledDisablesBothCachesExplicitly(String configValue) {
    when(dhisConfig.getProperty(USE_SECOND_LEVEL_CACHE)).thenReturn(configValue);

    Properties properties = HibernateConfig.getAdditionalProperties(dhisConfig);

    assertEquals("false", properties.get(AvailableSettings.USE_SECOND_LEVEL_CACHE));
    assertEquals("false", properties.get(AvailableSettings.USE_QUERY_CACHE));
    assertFalse(properties.containsKey(AvailableSettings.CACHE_REGION_FACTORY));
    assertFalse(properties.containsKey(ConfigSettings.CONFIG_URI));
  }
}
