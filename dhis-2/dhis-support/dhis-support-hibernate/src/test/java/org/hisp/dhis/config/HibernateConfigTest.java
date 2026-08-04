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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.Properties;
import org.hibernate.cache.jcache.internal.JCacheRegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HibernateConfigTest {

  @Mock private DhisConfigurationProvider config;

  @Test
  void shouldEnableSecondLevelCacheWhenTrueInConf() {
    when(config.getProperty(USE_SECOND_LEVEL_CACHE)).thenReturn("true");
    when(config.getProperty(USE_QUERY_CACHE)).thenReturn("true");
    when(config.getProperty(CACHE_EHCACHE_CONFIG_FILE)).thenReturn("");

    Properties properties = HibernateConfig.getAdditionalProperties(config);

    assertEquals("true", properties.get(AvailableSettings.USE_SECOND_LEVEL_CACHE));
    assertEquals(
        JCacheRegionFactory.class.getName(),
        properties.get(AvailableSettings.CACHE_REGION_FACTORY));
  }

  @Test
  void shouldDisableSecondLevelCacheWhenFalseInConf() {
    when(config.getProperty(USE_SECOND_LEVEL_CACHE)).thenReturn("false");

    Properties properties = HibernateConfig.getAdditionalProperties(config);

    assertEquals("false", properties.get(AvailableSettings.USE_SECOND_LEVEL_CACHE));
    assertNull(properties.get(AvailableSettings.CACHE_REGION_FACTORY));
  }

  @Test
  void shouldEnableSecondLevelCacheWhenNotPresentInConf() {
    when(config.getProperty(USE_SECOND_LEVEL_CACHE))
        .thenReturn(USE_SECOND_LEVEL_CACHE.getDefaultValue());
    when(config.getProperty(USE_QUERY_CACHE)).thenReturn(USE_QUERY_CACHE.getDefaultValue());
    when(config.getProperty(CACHE_EHCACHE_CONFIG_FILE))
        .thenReturn(CACHE_EHCACHE_CONFIG_FILE.getDefaultValue());

    Properties properties = HibernateConfig.getAdditionalProperties(config);

    assertEquals("true", properties.get(AvailableSettings.USE_SECOND_LEVEL_CACHE));
    assertEquals(
        JCacheRegionFactory.class.getName(),
        properties.get(AvailableSettings.CACHE_REGION_FACTORY));
  }
}
