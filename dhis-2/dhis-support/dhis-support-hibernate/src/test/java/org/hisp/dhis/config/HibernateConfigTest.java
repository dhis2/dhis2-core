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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Properties;
import org.hibernate.cache.jcache.ConfigSettings;
import org.hibernate.cfg.AvailableSettings;
import org.hisp.dhis.cache.guard.GuardedJCacheRegionFactory;
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
 * from dhis.conf are propagated explicitly to Hibernate. In particular, disabling the second level
 * cache must set 'hibernate.cache.use_second_level_cache' to false, otherwise Hibernate
 * auto-enables it with default JCache settings when a RegionFactory is present on the classpath.
 * All boolean spellings allowed in dhis.conf (true/TRUE/on/ON) must be recognized.
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
    when(dhisConfig.getProperty(CACHE_EHCACHE_CONFIG_FILE)).thenReturn("");
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
        GuardedJCacheRegionFactory.class.getName(),
        properties.get(AvailableSettings.CACHE_REGION_FACTORY));
    assertFalse(properties.containsKey(ConfigSettings.CONFIG_URI));
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

  @Test
  void secondLevelCacheEnabledWithEhcacheConfigFile() {
    when(dhisConfig.getProperty(USE_SECOND_LEVEL_CACHE)).thenReturn("true");
    when(dhisConfig.getProperty(USE_QUERY_CACHE)).thenReturn("true");
    when(dhisConfig.getProperty(CACHE_EHCACHE_CONFIG_FILE))
        .thenReturn("file:/opt/dhis2/ehcache.xml");

    Properties properties = HibernateConfig.getAdditionalProperties(dhisConfig);

    assertEquals("file:/opt/dhis2/ehcache.xml", properties.get(ConfigSettings.CONFIG_URI));
  }

  /**
   * Hibernate's ClassLoaderService only strips the nonstandard 'classpath://' scheme, so both
   * classpath spellings must be normalized to a bare resource name before they are handed over,
   * otherwise the SessionFactory fails to boot with "Couldn't load URI".
   */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "classpath:ehcache.xml",
        "classpath://ehcache.xml",
        "CLASSPATH:ehcache.xml",
        " classpath:ehcache.xml "
      })
  void ehcacheConfigFileClasspathSpellingsAreNormalized(String configValue) {
    when(dhisConfig.getProperty(USE_SECOND_LEVEL_CACHE)).thenReturn("true");
    when(dhisConfig.getProperty(USE_QUERY_CACHE)).thenReturn("true");
    when(dhisConfig.getProperty(CACHE_EHCACHE_CONFIG_FILE)).thenReturn(configValue);

    Properties properties = HibernateConfig.getAdditionalProperties(dhisConfig);

    assertEquals("ehcache.xml", properties.get(ConfigSettings.CONFIG_URI));
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
    assertFalse(properties.containsKey(ConfigSettings.CONFIG_URI));
  }
}
