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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.hisp.dhis.cache.ETagObservedEntityTypes;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Cardinality / memory-bound checks for {@link LocalETagService}: version maps grow by key count
 * (entity types + named keys), not by request or row volume.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class LocalETagServiceCardinalityTest {

  private LocalETagService service;

  @BeforeEach
  void setUp() throws ReflectiveOperationException {
    DhisConfigurationProvider config = mock(DhisConfigurationProvider.class);
    when(config.isEnabled(ConfigurationKey.CACHE_API_ETAG_ENABLED)).thenReturn(true);
    when(config.isEnabled(ConfigurationKey.MONITORING_CACHE_ETAG_ENABLED)).thenReturn(false);
    when(config.getPropertyOrDefault(
            ConfigurationKey.CACHE_API_ETAG_TTL_MINUTES,
            ConfigurationKey.CACHE_API_ETAG_TTL_MINUTES.getDefaultValue()))
        .thenReturn("10");
    when(config.getPropertyOrDefault(
            ConfigurationKey.CACHE_API_ETAG_STALE_SECONDS,
            ConfigurationKey.CACHE_API_ETAG_STALE_SECONDS.getDefaultValue()))
        .thenReturn("60");

    service = new LocalETagService();
    var field = LocalETagService.class.getDeclaredField("configurationProvider");
    field.setAccessible(true);
    field.set(service, config);
    service.afterPropertiesSet();
  }

  @Test
  @DisplayName("Repeated bumps of the same entity type do not grow the map")
  void sameEntityTypeDoesNotGrowMap() {
    for (int i = 0; i < 10_000; i++) {
      service.incrementEntityTypeVersion(DataElement.class);
    }
    assertEquals(1, service.entityTypeVersionMapSize());
    assertEquals(10_000L, service.getEntityTypeVersion(DataElement.class));
  }

  @Test
  @DisplayName("Entity map size equals number of distinct entity classes touched")
  void entityMapTracksDistinctTypesOnly() {
    service.incrementEntityTypeVersion(DataElement.class);
    service.incrementEntityTypeVersion(DataElement.class);
    service.incrementEntityTypeVersion(OrganisationUnit.class);
    service.incrementEntityTypeVersion(Indicator.class);
    service.incrementEntityTypeVersion(Indicator.class);

    assertEquals(3, service.entityTypeVersionMapSize());
    assertTrue(service.entityTypeVersionKeys().contains(DataElement.class.getName()));
    assertTrue(service.entityTypeVersionKeys().contains(OrganisationUnit.class.getName()));
    assertTrue(service.entityTypeVersionKeys().contains(Indicator.class.getName()));
  }

  @Test
  @DisplayName("Named keys live in a separate map and stay tiny")
  void namedKeysAreSeparateAndBounded() {
    service.incrementNamedVersion("installedApps");
    service.incrementNamedVersion("installedApps");
    service.incrementNamedVersion("staticContent");
    service.incrementEntityTypeVersion(DataElement.class);

    assertEquals(1, service.entityTypeVersionMapSize());
    assertEquals(2, service.namedVersionMapSize());
    assertEquals(2L, service.getNamedVersion("installedApps"));
    assertEquals(1L, service.getNamedVersion("staticContent"));
  }

  @Test
  @DisplayName("Entity keys for observed MetadataObject types are valid observed types")
  void entityKeysAreObservedTypes() {
    service.incrementEntityTypeVersion(DataElement.class);
    service.incrementEntityTypeVersion(OrganisationUnit.class);

    for (String key : service.entityTypeVersionKeys()) {
      try {
        Class<?> type = Class.forName(key);
        assertTrue(
            ETagObservedEntityTypes.isObservedType(type),
            "Unexpected non-observed type key in entity map: " + key);
      } catch (ClassNotFoundException e) {
        throw new AssertionError("Entity version key is not a loadable class: " + key, e);
      }
    }
  }
}
