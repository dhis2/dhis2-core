/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.cache;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.ComponentType;
import org.hibernate.type.CustomType;
import org.hibernate.type.Type;
import org.hibernate.usertype.UserType;
import org.hisp.dhis.cache.CachedEntityJsonbSerializableTest.DhisConfig;
import org.hisp.dhis.hibernate.jsonb.type.JsonBinaryType;
import org.hisp.dhis.render.type.RenderingObject;
import org.hisp.dhis.test.config.PostgresTestConfigOverride;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;

/**
 * Guard test for the Ehcache L2 second-level cache.
 *
 * <p>Since the Ehcache bump to 3.12 (see #23834 / #24002) the on-heap store uses {@code
 * SerializingCopier}, which performs real Java serialization of cached entity state. JSONB columns
 * are mapped with {@link JsonBinaryType} and its subclasses, whose {@code disassemble()} stores the
 * deep-copied Java value object (or, for list/set types, its elements) directly in the cache entry.
 * Ehcache then serializes that object graph, so every JSONB value type living on an L2-cached
 * entity MUST implement {@link Serializable} (a missing one throws {@code NotSerializableException}
 * at runtime, e.g. the {@code ApiTokenAttribute} regression fixed in #24002).
 *
 * <p>This test walks the live Hibernate metamodel rather than a hard-coded list, so any JSONB
 * column added to a cached entity in the future is checked automatically.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@ContextConfiguration(classes = {DhisConfig.class})
class CachedEntityJsonbSerializableTest extends PostgresIntegrationTestBase {

  /**
   * Enables the L2 cache so {@link EntityPersister#hasCache()} reflects the mapped cache regions.
   */
  static class DhisConfig {
    @Bean
    public PostgresTestConfigOverride postgresTestConfigOverride() {
      PostgresTestConfigOverride override = new PostgresTestConfigOverride();
      override.put(AvailableSettings.USE_SECOND_LEVEL_CACHE, "true");
      override.put("cache.ehcache.config.file", "");
      return override;
    }
  }

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  @DisplayName("Every JSONB value type on an L2-cached entity must be java.io.Serializable")
  void jsonbValueTypesOnCachedEntitiesAreSerializable() {
    MetamodelImplementor metamodel =
        entityManagerFactory.unwrap(SessionFactoryImplementor.class).getMetamodel();

    Set<Class<?>> inspected = new TreeSet<>(Comparator.comparing(Class::getName));
    // value class -> list of "entity#property" locations where it is used
    Map<String, List<String>> violations = new TreeMap<>();

    for (EntityPersister persister : metamodel.entityPersisters().values()) {
      if (!persister.hasCache()) {
        continue;
      }
      String entity = persister.getEntityName();
      String[] names = persister.getPropertyNames();
      Type[] types = persister.getPropertyTypes();
      for (int i = 0; i < types.length; i++) {
        inspect(entity, names[i], types[i], inspected, violations);
      }
    }

    assertFalse(
        inspected.isEmpty(),
        "Sanity check failed: no JSONB value types were inspected on any cached entity. "
            + "Is the second-level cache enabled for this context?");
    assertTrue(violations.isEmpty(), () -> buildFailureMessage(violations));
  }

  @Test
  @DisplayName("RenderingObject must extend Serializable (cached as DeviceRenderTypeMap values)")
  void renderingObjectIsSerializable() {
    // DeviceRenderTypeMap (a LinkedHashMap) is itself Serializable, so the metamodel check above
    // cannot see its RenderingObject values. ProgramSection and ProgramStageSection are L2-cached
    // and store these maps, so the interface must extend Serializable for ehcache to serialize
    // them.
    assertTrue(
        Serializable.class.isAssignableFrom(RenderingObject.class),
        "RenderingObject is stored as the values of DeviceRenderTypeMap JSONB columns on L2-cached "
            + "entities and must extend java.io.Serializable so the ehcache L2 cache can serialize it.");
  }

  /**
   * Recursively collects the value types of JSONB ({@link JsonBinaryType}) properties, descending
   * into embedded components. For list/set JSONB types {@link UserType#returnedClass()} is the
   * element type, which is exactly what must be serializable.
   */
  private static void inspect(
      String entity,
      String property,
      Type type,
      Set<Class<?>> inspected,
      Map<String, List<String>> violations) {
    if (type instanceof CustomType) {
      UserType userType = ((CustomType) type).getUserType();
      if (userType instanceof JsonBinaryType) {
        Class<?> valueType = userType.returnedClass();
        // Untyped JSONB columns (the "jbObject" type, returnedClass == java.lang.Object) are
        // deserialized by Jackson into plain JDK types (LinkedHashMap, ArrayList, String, Number,
        // Boolean), all of which are Serializable, so they are safe for the ehcache L2 cache. The
        // declared Object type cannot be statically proven Serializable, so skip it to avoid a
        // false positive (e.g. MapView#styleDataItem).
        if (valueType == Object.class) {
          return;
        }
        inspected.add(valueType);
        if (!Serializable.class.isAssignableFrom(valueType)) {
          violations
              .computeIfAbsent(valueType.getName(), k -> new ArrayList<>())
              .add(entity + "#" + property);
        }
      }
    } else if (type instanceof ComponentType) {
      ComponentType component = (ComponentType) type;
      String[] subNames = component.getPropertyNames();
      Type[] subTypes = component.getSubtypes();
      for (int i = 0; i < subTypes.length; i++) {
        inspect(entity, property + "." + subNames[i], subTypes[i], inspected, violations);
      }
    }
  }

  private static String buildFailureMessage(Map<String, List<String>> violations) {
    StringBuilder sb =
        new StringBuilder(
            "Found JSONB value type(s) on L2-cached entities that do NOT implement "
                + "java.io.Serializable.\nEhcache 3.12 (SerializingCopier) serializes cached entity "
                + "state, so each of these (and its whole object graph) must implement Serializable:\n");
    violations.forEach(
        (valueType, locations) ->
            sb.append("  - ")
                .append(valueType)
                .append("  used by: ")
                .append(String.join(", ", locations))
                .append('\n'));
    return sb.toString();
  }
}
