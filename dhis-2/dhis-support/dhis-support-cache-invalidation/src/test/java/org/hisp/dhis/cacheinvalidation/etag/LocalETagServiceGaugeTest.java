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
package org.hisp.dhis.cacheinvalidation.etag;

import static org.hisp.dhis.dml.DmlETagMetrics.ETAG_ENTITY_VERSIONS_SIZE;
import static org.hisp.dhis.dml.DmlETagMetrics.ETAG_NAMED_VERSIONS_SIZE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Micrometer gauge registration for {@link LocalETagService} memory bounds.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class LocalETagServiceGaugeTest {

  @Test
  @DisplayName("Gauges track map sizes when monitoring.cache.etag.enabled=on")
  void gaugesTrackMapSizesWhenMonitoringOn() throws Exception {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    LocalETagService service = newService(true, registry);

    Gauge entityGauge = registry.find(ETAG_ENTITY_VERSIONS_SIZE).gauge();
    Gauge namedGauge = registry.find(ETAG_NAMED_VERSIONS_SIZE).gauge();
    assertNotNull(entityGauge);
    assertNotNull(namedGauge);
    assertEquals(0.0, entityGauge.value());
    assertEquals(0.0, namedGauge.value());

    service.incrementEntityTypeVersion(DataElement.class);
    service.incrementEntityTypeVersion(DataElement.class);
    service.incrementEntityTypeVersion(OrganisationUnit.class);
    service.incrementNamedVersion("installedApps");
    service.incrementNamedVersion("staticContent");

    assertEquals(2.0, entityGauge.value());
    assertEquals(2.0, namedGauge.value());
  }

  @Test
  @DisplayName("Gauges are absent when monitoring.cache.etag.enabled=off")
  void gaugesAbsentWhenMonitoringOff() throws Exception {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    newService(false, registry);

    assertNull(registry.find(ETAG_ENTITY_VERSIONS_SIZE).gauge());
    assertNull(registry.find(ETAG_NAMED_VERSIONS_SIZE).gauge());
  }

  @Test
  @DisplayName("Gauges are absent when MeterRegistry is null")
  void gaugesAbsentWhenRegistryNull() throws Exception {
    LocalETagService service = newService(true, null);
    service.incrementEntityTypeVersion(DataElement.class);
    assertEquals(1, service.entityTypeVersionMapSize());
  }

  @Test
  @DisplayName("Calling afterPropertiesSet twice does not blow up meter registration")
  void doubleAfterPropertiesSetDoesNotThrow() throws Exception {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    LocalETagService service = newService(true, registry);

    assertDoesNotThrow(service::afterPropertiesSet);
    assertDoesNotThrow(service::afterPropertiesSet);

    service.incrementNamedVersion("installedApps");
    Gauge namedGauge = registry.find(ETAG_NAMED_VERSIONS_SIZE).gauge();
    assertNotNull(namedGauge);
    assertEquals(1.0, namedGauge.value());
  }

  private static LocalETagService newService(boolean monitoringOn, MeterRegistry registry)
      throws Exception {
    DhisConfigurationProvider config = mock(DhisConfigurationProvider.class);
    when(config.isEnabled(ConfigurationKey.CACHE_API_ETAG_ENABLED)).thenReturn(true);
    when(config.isEnabled(ConfigurationKey.MONITORING_CACHE_ETAG_ENABLED)).thenReturn(monitoringOn);
    when(config.getPropertyOrDefault(
            ConfigurationKey.CACHE_API_ETAG_TTL_MINUTES,
            ConfigurationKey.CACHE_API_ETAG_TTL_MINUTES.getDefaultValue()))
        .thenReturn("10");
    when(config.getPropertyOrDefault(
            ConfigurationKey.CACHE_API_ETAG_STALE_SECONDS,
            ConfigurationKey.CACHE_API_ETAG_STALE_SECONDS.getDefaultValue()))
        .thenReturn("60");

    LocalETagService service = new LocalETagService();
    setField(service, "configurationProvider", config);
    setField(service, "meterRegistry", registry);
    service.afterPropertiesSet();
    return service;
  }

  private static void setField(Object target, String name, Object value) throws Exception {
    var field = LocalETagService.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }
}
