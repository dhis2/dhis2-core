/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CustomType;
import org.hibernate.type.EnumType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.StringType;
import org.hibernate.type.TextType;
import org.hisp.dhis.resourcetable.ResourceTableType;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class AnalyticsTableHookStoreTest extends PostgresIntegrationTestBase {
  @Autowired private AnalyticsTableHookStore sqlHookStore;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private EntityManagerFactory entityManagerFactory;

  private final String sql = "update analytics_rs_orgunitstructure set organisationunitid=1";

  @Test
  void testGetByType() {
    AnalyticsTableHook hookA =
        new AnalyticsTableHook(
            "NameA",
            AnalyticsTablePhase.RESOURCE_TABLE_POPULATED,
            ResourceTableType.ORG_UNIT_STRUCTURE,
            sql);
    AnalyticsTableHook hookB =
        new AnalyticsTableHook(
            "NameB",
            AnalyticsTablePhase.ANALYTICS_TABLE_POPULATED,
            AnalyticsTableType.DATA_VALUE,
            sql);
    AnalyticsTableHook hookC =
        new AnalyticsTableHook(
            "NameC",
            AnalyticsTablePhase.ANALYTICS_TABLE_POPULATED,
            AnalyticsTableType.DATA_VALUE,
            sql);
    AnalyticsTableHook hookD =
        new AnalyticsTableHook(
            "NameD", AnalyticsTablePhase.ANALYTICS_TABLE_POPULATED, AnalyticsTableType.EVENT, sql);
    sqlHookStore.save(hookA);
    sqlHookStore.save(hookB);
    sqlHookStore.save(hookC);
    sqlHookStore.save(hookD);
    List<AnalyticsTableHook> hooks =
        sqlHookStore.getByPhaseAndAnalyticsTableType(
            AnalyticsTablePhase.ANALYTICS_TABLE_POPULATED, AnalyticsTableType.DATA_VALUE);
    assertEquals(2, hooks.size());
    hooks.forEach(
        hook -> {
          assertNotNull(hook.getName());
          assertNotNull(hook.getPhase());
          assertEquals(AnalyticsTableType.DATA_VALUE, hook.getAnalyticsTableType());
        });
  }

  @Test
  @DisplayName("hooks are found by phase and by phase and resource table type after a reload")
  void testGetByPhaseAndResourceTableType() {
    save(
        new AnalyticsTableHook(
            "Org unit hook",
            AnalyticsTablePhase.RESOURCE_TABLE_POPULATED,
            ResourceTableType.ORG_UNIT_STRUCTURE,
            sql));
    save(
        new AnalyticsTableHook(
            "Period hook",
            AnalyticsTablePhase.RESOURCE_TABLE_POPULATED,
            ResourceTableType.PERIOD_STRUCTURE,
            sql));
    save(
        new AnalyticsTableHook(
            "Event hook",
            AnalyticsTablePhase.ANALYTICS_TABLE_POPULATED,
            AnalyticsTableType.EVENT,
            sql));
    clearSession();
    entityManagerFactory.getCache().evict(AnalyticsTableHook.class);

    assertEquals(
        List.of("Org unit hook", "Period hook"),
        names(sqlHookStore.getByPhase(AnalyticsTablePhase.RESOURCE_TABLE_POPULATED)));
    assertEquals(
        List.of("Org unit hook"),
        names(
            sqlHookStore.getByPhaseAndResourceTableType(
                AnalyticsTablePhase.RESOURCE_TABLE_POPULATED,
                ResourceTableType.ORG_UNIT_STRUCTURE)));
  }

  @Test
  @DisplayName("the id is generated by a sequence generator on hibernate_sequence")
  void testIdGeneratorUsesHibernateSequence() {
    SequenceStyleGenerator generator =
        assertInstanceOf(SequenceStyleGenerator.class, persister().getIdentifierGenerator());

    assertEquals(
        "hibernate_sequence",
        generator.getDatabaseStructure().getPhysicalName().getObjectName().getText());
  }

  @Test
  @DisplayName("the persister maps every property with its type, nullability and cache strategy")
  void testPersisterMetadata() {
    EntityPersister persister = persister();
    assertEquals(
        List.of(
            "analyticsTableType",
            "code",
            "created",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "phase",
            "resourceTableType",
            "sql",
            "uid"),
        List.of(persister.getPropertyNames()).stream().sorted().toList());

    assertInstanceOf(TextType.class, persister.getPropertyType("sql"));
    assertInstanceOf(StringType.class, persister.getPropertyType("name"));
    assertNamedEnum(AnalyticsTablePhase.class, "phase");
    assertNamedEnum(ResourceTableType.class, "resourceTableType");
    assertNamedEnum(AnalyticsTableType.class, "analyticsTableType");
    ManyToOneType lastUpdatedBy =
        assertInstanceOf(ManyToOneType.class, persister.getPropertyType("lastUpdatedBy"));
    assertFalse(lastUpdatedBy.isEager(null));

    for (String property : List.of("uid", "name", "created", "lastUpdated", "phase", "sql")) {
      assertFalse(isNullable(property), property + " is mapped nullable");
    }
    for (String property :
        List.of("code", "resourceTableType", "analyticsTableType", "lastUpdatedBy")) {
      assertTrue(isNullable(property), property + " is mapped not-null");
    }
    assertFalse(
        persister
            .getPropertyUpdateability()[
            persister.getEntityMetamodel().getPropertyIndex("created")]);

    assertTrue(persister.canReadFromCache());
    assertEquals(AccessType.READ_WRITE, persister.getCacheAccessStrategy().getAccessType());
  }

  @Test
  @DisplayName(
      "every mapped property of a resource table hook is written to its column and read back")
  void testResourceTableHookColumnsRoundTrip() {
    User admin = getAdminUser();
    String longSql = "update analytics_rs_orgunitstructure set level = 1 -- " + "x".repeat(10_000);
    AnalyticsTableHook hook =
        new AnalyticsTableHook(
            "Resource hook",
            AnalyticsTablePhase.RESOURCE_TABLE_POPULATED,
            ResourceTableType.ORG_UNIT_STRUCTURE,
            longSql);
    hook.setCode("HOOK_RESOURCE");
    hook.setLastUpdatedBy(admin);
    AnalyticsTableHook saved = save(hook);

    Map<String, Object> row = row(saved);
    assertEquals(saved.getUid(), row.get("uid"));
    assertEquals("HOOK_RESOURCE", row.get("code"));
    assertEquals("Resource hook", row.get("name"));
    assertEquals("RESOURCE_TABLE_POPULATED", row.get("analyticstablephase"));
    assertEquals("ORG_UNIT_STRUCTURE", row.get("resourcetabletype"));
    assertNull(row.get("analyticstabletype"));
    assertEquals(longSql, row.get("sql"));
    assertEquals(saved.getCreated().getTime(), ((Date) row.get("created")).getTime());
    assertEquals(saved.getLastUpdated().getTime(), ((Date) row.get("lastupdated")).getTime());
    assertEquals(admin.getId(), ((Number) row.get("lastupdatedby")).longValue());

    AnalyticsTableHook reloaded = reload(saved);
    assertEquals(saved.getUid(), reloaded.getUid());
    assertEquals("HOOK_RESOURCE", reloaded.getCode());
    assertEquals("Resource hook", reloaded.getName());
    assertEquals(AnalyticsTablePhase.RESOURCE_TABLE_POPULATED, reloaded.getPhase());
    assertEquals(ResourceTableType.ORG_UNIT_STRUCTURE, reloaded.getResourceTableType());
    assertNull(reloaded.getAnalyticsTableType());
    assertEquals(longSql, reloaded.getSql());
    assertEquals(saved.getCreated().getTime(), reloaded.getCreated().getTime());
    assertEquals(saved.getLastUpdated().getTime(), reloaded.getLastUpdated().getTime());
    assertEquals(admin.getUid(), reloaded.getLastUpdatedBy().getUid());
  }

  @Test
  @DisplayName("an analytics table hook writes its table type and leaves the optional columns null")
  void testAnalyticsTableHookColumnsRoundTrip() {
    AnalyticsTableHook saved =
        save(
            new AnalyticsTableHook(
                "Analytics hook",
                AnalyticsTablePhase.ANALYTICS_TABLE_POPULATED,
                AnalyticsTableType.ENROLLMENT,
                sql));

    Map<String, Object> row = row(saved);
    assertNull(row.get("code"));
    assertEquals("ANALYTICS_TABLE_POPULATED", row.get("analyticstablephase"));
    assertNull(row.get("resourcetabletype"));
    assertEquals("ENROLLMENT", row.get("analyticstabletype"));

    AnalyticsTableHook reloaded = reload(saved);
    assertNull(reloaded.getCode());
    assertEquals(AnalyticsTablePhase.ANALYTICS_TABLE_POPULATED, reloaded.getPhase());
    assertNull(reloaded.getResourceTableType());
    assertEquals(AnalyticsTableType.ENROLLMENT, reloaded.getAnalyticsTableType());
    assertEquals(sql, reloaded.getSql());
  }

  private void assertNamedEnum(Class<?> enumClass, String property) {
    CustomType type = assertInstanceOf(CustomType.class, persister().getPropertyType(property));
    EnumType<?> enumType = assertInstanceOf(EnumType.class, type.getUserType());
    assertEquals(enumClass, enumType.returnedClass(), property);
    assertFalse(enumType.isOrdinal(), property + " is mapped by ordinal");
  }

  private EntityPersister persister() {
    return entityManagerFactory
        .unwrap(SessionFactoryImplementor.class)
        .getMetamodel()
        .entityPersister(AnalyticsTableHook.class);
  }

  /** {@link EntityPersister#getPropertyNullability()} holds a nullable flag per property. */
  private boolean isNullable(String property) {
    EntityPersister persister = persister();
    return persister
        .getPropertyNullability()[persister.getEntityMetamodel().getPropertyIndex(property)];
  }

  private Map<String, Object> row(AnalyticsTableHook hook) {
    return jdbcTemplate.queryForMap(
        "select uid, code, name, analyticstablephase, resourcetabletype, analyticstabletype, sql,"
            + " created, lastupdated, lastupdatedby from tablehook where analyticstablehookid = ?",
        hook.getId());
  }

  private AnalyticsTableHook save(AnalyticsTableHook hook) {
    sqlHookStore.save(hook);
    entityManager.flush();
    return hook;
  }

  /** Reloads the hook from the database, bypassing the session and the second-level cache. */
  private AnalyticsTableHook reload(AnalyticsTableHook hook) {
    clearSession();
    entityManagerFactory.getCache().evict(AnalyticsTableHook.class);
    return sqlHookStore.get(hook.getId());
  }

  private static List<String> names(List<AnalyticsTableHook> hooks) {
    return hooks.stream().map(AnalyticsTableHook::getName).sorted().toList();
  }
}
