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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.cache.ETagObservedEntityTypes;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataValue;
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

  /**
   * Production named-version keys only (compile-time constants at call sites). A new production
   * {@code incrementNamedVersion} call site must update this set deliberately.
   */
  private static final Set<String> PRODUCTION_NAMED_VERSION_KEYS =
      Set.of("installedApps", "staticContent");

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

  @Test
  @DisplayName(
      "Production gate (ETagObservedEntityTypes) only lands observed types; map size <= closed universe")
  void productionObservedGateAndCeiling() {
    // Same gate as DmlObserverListener.bumpVersionForEvent before incrementEntityTypeVersion.
    List<Class<?>> candidates =
        List.of(
            DataElement.class,
            OrganisationUnit.class,
            Indicator.class,
            Configuration.class,
            DataValue.class,
            String.class);

    int expectedObserved = 0;
    for (Class<?> type : candidates) {
      if (bumpThroughProductionGate(type)) {
        expectedObserved++;
      }
    }

    assertFalse(
        service.entityTypeVersionKeys().contains(DataValue.class.getName()),
        "Non-observed DataValue must not create a map entry");
    assertFalse(
        service.entityTypeVersionKeys().contains(String.class.getName()),
        "Non-entity String must not create a map entry");
    assertEquals(expectedObserved, service.entityTypeVersionMapSize());
    assertTrue(service.entityTypeVersionKeys().contains(DataElement.class.getName()));
    assertTrue(service.entityTypeVersionKeys().contains(Configuration.class.getName()));

    // Closed additional-observed universe from ETagObservedEntityTypes (not a magic number).
    Set<String> additionalUniverse =
        ETagObservedEntityTypes.getAdditionalObservedTypeNames();
    int additionalLoaded = 0;
    for (String fqcn : additionalUniverse) {
      try {
        Class<?> type = Class.forName(fqcn);
        if (bumpThroughProductionGate(type)) {
          additionalLoaded++;
        }
      } catch (ClassNotFoundException e) {
        // Reflective SystemSetting may be missing on slim classpaths; skip.
      }
    }

    // Ceiling: every key is observed, and keys that are "additional" cannot exceed the
    // ETagObservedEntityTypes additional set size.
    long additionalKeysInMap =
        service.entityTypeVersionKeys().stream().filter(additionalUniverse::contains).count();
    assertTrue(
        additionalKeysInMap <= additionalUniverse.size(),
        "additional keys in map ("
            + additionalKeysInMap
            + ") exceed ETagObservedEntityTypes universe ("
            + additionalUniverse.size()
            + ")");

    for (String key : service.entityTypeVersionKeys()) {
      try {
        Class<?> type = Class.forName(key);
        assertTrue(
            ETagObservedEntityTypes.isObservedType(type),
            "Map key bypassed production gate: " + key);
        assertTrue(
            MetadataObject.class.isAssignableFrom(type) || additionalUniverse.contains(key),
            "Key is neither MetadataObject nor in additional observed set: " + key);
      } catch (ClassNotFoundException e) {
        throw new AssertionError("Entity version key is not a loadable class: " + key, e);
      }
    }

    // Re-bumping the same observed set must not grow the map (dedup by type key).
    int sizeAfter = service.entityTypeVersionMapSize();
    for (Class<?> type : candidates) {
      bumpThroughProductionGate(type);
    }
    for (String fqcn : additionalUniverse) {
      try {
        bumpThroughProductionGate(Class.forName(fqcn));
      } catch (ClassNotFoundException ignored) {
        // same as above
      }
    }
    assertEquals(sizeAfter, service.entityTypeVersionMapSize());
    assertTrue(additionalLoaded >= 1 || !additionalUniverse.isEmpty());
  }

  @Test
  @DisplayName("Production named-version key set is pinned (installedApps, staticContent)")
  void productionNamedVersionKeySetIsPinned() {
    assertEquals(
        Set.of("installedApps", "staticContent"),
        PRODUCTION_NAMED_VERSION_KEYS,
        "Update PRODUCTION_NAMED_VERSION_KEYS when adding a production incrementNamedVersion call site");

    for (String key : PRODUCTION_NAMED_VERSION_KEYS) {
      service.incrementNamedVersion(key);
    }
    assertEquals(PRODUCTION_NAMED_VERSION_KEYS.size(), service.namedVersionMapSize());
    assertEquals(PRODUCTION_NAMED_VERSION_KEYS, service.namedVersionKeys());
  }

  /**
   * Mirrors {@code DmlObserverListener.bumpVersionForEvent}: only observed types reach the ETag
   * service.
   *
   * @return true if a version bump was performed
   */
  private boolean bumpThroughProductionGate(Class<?> entityClass) {
    if (!ETagObservedEntityTypes.isObservedType(entityClass)) {
      return false;
    }
    service.incrementEntityTypeVersion(entityClass);
    return true;
  }
}
