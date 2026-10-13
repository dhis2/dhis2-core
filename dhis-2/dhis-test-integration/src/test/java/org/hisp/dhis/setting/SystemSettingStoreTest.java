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
package org.hisp.dhis.setting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Set;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.TextType;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Tests the persistence mapping of {@link SystemSetting} to the systemsetting table. */
@Transactional
class SystemSettingStoreTest extends PostgresIntegrationTestBase {

  @Autowired private SystemSettingStore store;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  @DisplayName("saving takes the id from hibernate_sequence")
  void testIdComesFromHibernateSequence() {
    long before = lastSequenceValue();
    SystemSetting first = save(new SystemSetting("keyFirst", "first"));
    SystemSetting second = save(new SystemSetting("keySecond", "second"));
    long after = lastSequenceValue();

    assertTrue(before < first.getId(), "first id must be allocated after " + before);
    assertTrue(first.getId() < second.getId());
    assertTrue(second.getId() <= after, "second id must not exceed the sequence value " + after);
  }

  @Test
  @DisplayName("name and value are written to the name and value columns and read back from them")
  void testColumnsRoundTrip() {
    SystemSetting saved = save(new SystemSetting("keyColumns", "valueColumns"));

    assertEquals(
        List.of("keyColumns", "valueColumns"),
        jdbcTemplate.queryForObject(
            "select name, value from systemsetting where systemsettingid = ?",
            (rs, row) -> List.of(rs.getString("name"), rs.getString("value")),
            saved.getId()));

    clearSession();

    SystemSetting reloaded = entityManager.find(SystemSetting.class, saved.getId());
    assertEquals("keyColumns", reloaded.getName());
    assertEquals("valueColumns", reloaded.getValue());
  }

  @Test
  @DisplayName("value is bound as text and keeps a value longer than 255 characters")
  void testValueIsText() {
    EntityPersister persister = persister();
    assertInstanceOf(TextType.class, persister.getPropertyType("value"));

    String longValue = "v".repeat(10_000);
    SystemSetting saved = save(new SystemSetting("keyLongValue", longValue));
    clearSession();

    assertEquals(longValue, entityManager.find(SystemSetting.class, saved.getId()).getValue());
  }

  @Test
  @DisplayName("name is mapped not-null and value is mapped nullable")
  void testNullability() {
    EntityPersister persister = persister();
    EntityMetamodel metamodel = persister.getEntityMetamodel();
    boolean[] nullability = persister.getPropertyNullability();

    assertFalse(nullability[metamodel.getPropertyIndex("name")]);
    assertTrue(nullability[metamodel.getPropertyIndex("value")]);
  }

  @Test
  @DisplayName("a null value is persisted and read back as null")
  void testNullValue() {
    SystemSetting saved = save(new SystemSetting("keyNullValue", null));
    clearSession();

    assertNull(entityManager.find(SystemSetting.class, saved.getId()).getValue());
  }

  @Test
  @DisplayName("updating the value of a loaded setting is persisted")
  void testUpdate() {
    SystemSetting saved = save(new SystemSetting("keyUpdate", "old"));
    clearSession();

    SystemSetting loaded = entityManager.find(SystemSetting.class, saved.getId());
    loaded.setValue("new");
    entityManager.flush();
    clearSession();

    assertEquals("new", entityManager.find(SystemSetting.class, saved.getId()).getValue());
  }

  @Test
  @DisplayName("the store resolves its table from the mapping and reads mapped rows")
  void testStoreReadsMappedRows() {
    save(new SystemSetting("keyStoreRead", "fromEntity"));
    clearSession();

    assertEquals("fromEntity", store.getAll().get("keyStoreRead"));
  }

  @Test
  @DisplayName("a setting inserted by the store loads as an entity with a sequence id")
  void testStoreInsertLoadsAsEntity() {
    long before = lastSequenceValue();
    store.put("keyStoreInsert", "fromStore");
    clearSession();

    SystemSetting loaded = findByName("keyStoreInsert");
    assertEquals("fromStore", loaded.getValue());
    assertTrue(before < loaded.getId(), "id must be allocated after " + before);
  }

  @Test
  @DisplayName("deleting through the store removes only the named rows")
  void testStoreDelete() {
    SystemSetting deleted = save(new SystemSetting("keyDeleted", "gone"));
    SystemSetting kept = save(new SystemSetting("keyKept", "kept"));
    clearSession();

    assertEquals(1, store.delete(Set.of("keyDeleted")));
    clearSession();

    assertNull(entityManager.find(SystemSetting.class, deleted.getId()));
    assertEquals("kept", entityManager.find(SystemSetting.class, kept.getId()).getValue());
  }

  @Test
  @DisplayName("the entity is not second-level cached")
  void testNotCached() {
    assertFalse(persister().canReadFromCache());
  }

  private EntityPersister persister() {
    return entityManagerFactory
        .unwrap(SessionFactoryImplementor.class)
        .getMetamodel()
        .entityPersister(SystemSetting.class);
  }

  private SystemSetting save(SystemSetting setting) {
    entityManager.persist(setting);
    entityManager.flush();
    return setting;
  }

  private SystemSetting findByName(String name) {
    return entityManager
        .createQuery("from SystemSetting where name = :name", SystemSetting.class)
        .setParameter("name", name)
        .getSingleResult();
  }

  private long lastSequenceValue() {
    return jdbcTemplate.queryForObject("select last_value from hibernate_sequence", Long.class);
  }
}
