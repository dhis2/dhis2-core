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
package org.hisp.dhis.tracker.audit;

import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CustomType;
import org.hibernate.type.EnumType;
import org.hibernate.type.TimestampType;
import org.hisp.dhis.audit.AuditOperationType;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAuditQueryParams;
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
class TrackedEntityAuditStoreTest extends PostgresIntegrationTestBase {
  private final Date created = getDate(2022, 3, 1);

  @Autowired private TrackedEntityAuditStore store;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private EntityManagerFactory entityManagerFactory;

  private final TrackedEntityAudit auditA =
      new TrackedEntityAudit("WGW7UnVcIIb", "Access", created, "userA", AuditOperationType.CREATE);
  private final TrackedEntityAudit auditB =
      new TrackedEntityAudit("WGW7UnVcIIb", "Access", created, "userB", AuditOperationType.UPDATE);
  private final TrackedEntityAudit auditC =
      new TrackedEntityAudit("zIAwTY3Drrn", "Access", created, "userA", AuditOperationType.UPDATE);
  private final TrackedEntityAudit auditD =
      new TrackedEntityAudit("zIAwTY3Drrn", "Access", created, "userB", AuditOperationType.DELETE);

  @Test
  void shouldAuditTrackedEntity_whenAddAuditList() {
    List<TrackedEntityAudit> trackedEntityAuditInput = List.of(auditA, auditB);

    store.addTrackedEntityAudit(trackedEntityAuditInput);

    TrackedEntityAuditQueryParams params =
        new TrackedEntityAuditQueryParams().setTrackedEntities(List.of("WGW7UnVcIIb"));

    List<TrackedEntityAudit> trackedEntityAudits = store.getTrackedEntityAudit(params);

    assertEquals(trackedEntityAuditInput.size(), trackedEntityAudits.size());
    TrackedEntityAudit entityAudit =
        filterByAuditType(trackedEntityAudits, AuditOperationType.CREATE);

    assertNotNull(entityAudit);
    assertEquals("userA", entityAudit.getAccessedBy());
    assertEquals("WGW7UnVcIIb", entityAudit.getTrackedEntity());

    entityAudit = filterByAuditType(trackedEntityAudits, AuditOperationType.UPDATE);

    assertNotNull(entityAudit);
    assertEquals("userB", entityAudit.getAccessedBy());
    assertEquals("WGW7UnVcIIb", entityAudit.getTrackedEntity());
  }

  private static TrackedEntityAudit filterByAuditType(
      List<TrackedEntityAudit> trackedEntityAuditsStore, AuditOperationType auditOperationType) {
    return trackedEntityAuditsStore.stream()
        .filter(a -> a.getAuditType() == auditOperationType)
        .findFirst()
        .orElse(null);
  }

  @Test
  void testGetAuditsByParams() {

    store.addTrackedEntityAudit(auditA);
    store.addTrackedEntityAudit(auditB);
    store.addTrackedEntityAudit(auditC);
    store.addTrackedEntityAudit(auditD);

    TrackedEntityAuditQueryParams params =
        new TrackedEntityAuditQueryParams().setTrackedEntities(List.of("WGW7UnVcIIb"));

    assertContainsOnly(List.of(auditA, auditB), store.getTrackedEntityAudit(params));

    params = new TrackedEntityAuditQueryParams().setUsers(List.of("userA"));

    assertContainsOnly(List.of(auditA, auditC), store.getTrackedEntityAudit(params));

    params = new TrackedEntityAuditQueryParams().setAuditTypes(List.of(AuditOperationType.UPDATE));

    assertContainsOnly(List.of(auditB, auditC), store.getTrackedEntityAudit(params));

    params =
        new TrackedEntityAuditQueryParams()
            .setAuditTypes(List.of(AuditOperationType.CREATE, AuditOperationType.DELETE))
            .setUsers(List.of("userA", "userB", "userC", "userD"));

    assertContainsOnly(List.of(auditA, auditD), store.getTrackedEntityAudit(params));

    params =
        new TrackedEntityAuditQueryParams()
            .setTrackedEntities(List.of("WGW7UnVcIIb"))
            .setUsers(List.of("userA"));

    assertContainsOnly(List.of(auditA), store.getTrackedEntityAudit(params));
  }

  @Test
  void shouldGetAuditsCreatedWithinStartAndEndDate() {
    TrackedEntityAudit march = audit("userA", getDate(2022, 3, 1));
    TrackedEntityAudit april = audit("userA", getDate(2022, 4, 1));
    TrackedEntityAudit may = audit("userA", getDate(2022, 5, 1));
    List.of(march, april, may).forEach(store::addTrackedEntityAudit);

    TrackedEntityAuditQueryParams params =
        new TrackedEntityAuditQueryParams()
            .setTrackedEntities(List.of("ZtIhaTkP3pL"))
            .setStartDate(getDate(2022, 4, 1))
            .setEndDate(getDate(2022, 5, 1));

    assertContainsOnly(List.of(april, may), store.getTrackedEntityAudit(params));
  }

  @Test
  void shouldGetPageOfAuditsOrderedByCreatedDescending() {
    TrackedEntityAudit first = audit("userA", getDate(2022, 3, 1));
    TrackedEntityAudit second = audit("userA", getDate(2022, 3, 2));
    TrackedEntityAudit third = audit("userA", getDate(2022, 3, 3));
    TrackedEntityAudit fourth = audit("userA", getDate(2022, 3, 4));
    List.of(second, fourth, first, third).forEach(store::addTrackedEntityAudit);

    TrackedEntityAuditQueryParams params =
        new TrackedEntityAuditQueryParams()
            .setTrackedEntities(List.of("ZtIhaTkP3pL"))
            .setPager(new Pager(2, 4, 2));

    assertEquals(List.of(second, first), store.getTrackedEntityAudit(params));
  }

  @Test
  void shouldCountAuditsMatchingParams() {
    store.addTrackedEntityAudit(auditA);
    store.addTrackedEntityAudit(auditB);
    store.addTrackedEntityAudit(auditC);
    store.addTrackedEntityAudit(auditD);

    TrackedEntityAuditQueryParams params =
        new TrackedEntityAuditQueryParams()
            .setTrackedEntities(List.of("WGW7UnVcIIb", "zIAwTY3Drrn"))
            .setAuditTypes(List.of(AuditOperationType.UPDATE, AuditOperationType.DELETE));

    assertEquals(3, store.getTrackedEntityAuditCount(params));
  }

  // -------------------------------------------------------------------------
  // Mapping
  // -------------------------------------------------------------------------

  @Test
  @DisplayName(
      "the id is generated by a sequence generator on trackedentityinstanceaudit_sequence, one"
          + " nextval per id")
  void shouldGenerateIdFromTrackedEntityInstanceAuditSequence() {
    SequenceStyleGenerator generator =
        assertInstanceOf(SequenceStyleGenerator.class, persister().getIdentifierGenerator());

    assertEquals(
        "trackedentityinstanceaudit_sequence",
        generator.getDatabaseStructure().getPhysicalName().getObjectName().getText());
    assertEquals(1, generator.getDatabaseStructure().getIncrementSize());
  }

  @Test
  @DisplayName("single saves and the native batch insert draw ascending ids from the same sequence")
  void shouldDrawIdsForSingleAndBatchInsertsFromTheSameSequence() {
    TrackedEntityAudit firstSingle = audit("userA", getDate(2022, 3, 1));
    store.addTrackedEntityAudit(firstSingle);
    store.addTrackedEntityAudit(
        List.of(audit("userB", getDate(2022, 3, 2)), audit("userC", getDate(2022, 3, 3))));
    TrackedEntityAudit secondSingle = audit("userD", getDate(2022, 3, 4));
    store.addTrackedEntityAudit(secondSingle);
    entityManager.flush();

    List<Map<String, Object>> rows =
        jdbcTemplate.queryForList(
            """
            select trackedentityauditid, accessedby from trackedentityaudit
            where trackedentity = 'ZtIhaTkP3pL' order by trackedentityauditid""");

    assertEquals(
        List.of("userA", "userB", "userC", "userD"),
        rows.stream().map(row -> row.get("accessedby")).toList());
    assertEquals(firstSingle.getId(), rows.get(0).get("trackedentityauditid"));
    assertEquals(secondSingle.getId(), rows.get(3).get("trackedentityauditid"));
  }

  @Test
  @DisplayName("every property is written to its column and read back from it")
  void shouldWriteEveryPropertyToItsColumnAndReadItBack() {
    Timestamp created = Timestamp.valueOf("2022-06-15 13:45:30.123");
    TrackedEntityAudit audit =
        new TrackedEntityAudit(
            "ZtIhaTkP3pL", "mapping comment", created, "mappingUser", AuditOperationType.SEARCH);
    store.addTrackedEntityAudit(audit);
    entityManager.flush();

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            """
            select trackedentity, comment, created, accessedby, audittype
            from trackedentityaudit where trackedentityauditid = ?""",
            audit.getId());
    assertEquals("ZtIhaTkP3pL", row.get("trackedentity"));
    assertEquals("mapping comment", row.get("comment"));
    assertEquals(created.getTime(), ((Date) row.get("created")).getTime());
    assertEquals("mappingUser", row.get("accessedby"));
    assertEquals("SEARCH", row.get("audittype"));

    TrackedEntityAudit reloaded = reload(audit);
    assertEquals(created.getTime(), reloaded.getCreated().getTime());
    assertSame(AuditOperationType.SEARCH, reloaded.getAuditType());
  }

  @Test
  @DisplayName(
      "the audit type is a not-null named varchar enum, created a timestamp, the other columns"
          + " nullable, no cache, no version")
  void shouldMapPersisterMetadata() {
    EntityPersister persister = persister();

    CustomType type = assertInstanceOf(CustomType.class, persister.getPropertyType("auditType"));
    EnumType<?> enumType = assertInstanceOf(EnumType.class, type.getUserType());
    assertFalse(enumType.isOrdinal());
    assertSame(AuditOperationType.class, enumType.returnedClass());
    assertArrayEquals(new int[] {Types.VARCHAR}, enumType.sqlTypes());

    assertInstanceOf(TimestampType.class, persister.getPropertyType("created"));

    assertEquals(5, persister.getPropertyNullability().length);
    assertFalse(isNullable(persister, "auditType"));
    for (String property : List.of("trackedEntity", "comment", "created", "accessedBy")) {
      assertTrue(isNullable(persister, property), property);
    }

    assertFalse(persister.canReadFromCache());
    assertFalse(persister.isVersioned());
  }

  private TrackedEntityAudit audit(String accessedBy, Date created) {
    return new TrackedEntityAudit(
        "ZtIhaTkP3pL", "Access", created, accessedBy, AuditOperationType.READ);
  }

  /** Reloads from the database, not the session or the second-level cache. */
  private TrackedEntityAudit reload(TrackedEntityAudit audit) {
    clearSession();
    entityManagerFactory.getCache().evictAll();
    return entityManager.find(TrackedEntityAudit.class, audit.getId());
  }

  private static boolean isNullable(EntityPersister persister, String property) {
    return persister
        .getPropertyNullability()[persister.getEntityMetamodel().getPropertyIndex(property)];
  }

  private EntityPersister persister() {
    return entityManagerFactory
        .unwrap(SessionFactoryImplementor.class)
        .getMetamodel()
        .entityPersister(TrackedEntityAudit.class);
  }
}
