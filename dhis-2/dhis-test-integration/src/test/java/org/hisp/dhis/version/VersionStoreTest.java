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
package org.hisp.dhis.version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import java.util.List;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Tests the persistence mapping of {@link Version} to the version table. */
@Transactional
class VersionStoreTest extends PostgresIntegrationTestBase {

  @Autowired private VersionStore store;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  @DisplayName("saving takes the id from hibernate_sequence")
  void testIdComesFromHibernateSequence() {
    long before = lastSequenceValue();
    Version first = save(new Version("keyFirst", "first"));
    Version second = save(new Version("keySecond", "second"));
    long after = lastSequenceValue();

    assertTrue(before < first.getId(), "first id must be allocated after " + before);
    assertTrue(first.getId() < second.getId());
    assertTrue(second.getId() <= after, "second id must not exceed the sequence value " + after);
  }

  @Test
  @DisplayName("key and value are written to the versionkey and versionvalue columns")
  void testColumnsAreWritten() {
    Version saved = save(new Version("keyColumns", "valueColumns"));

    assertEquals(
        List.of("keyColumns", "valueColumns"),
        jdbcTemplate.queryForObject(
            "select versionkey, versionvalue from version where versionid = ?",
            (rs, row) -> List.of(rs.getString("versionkey"), rs.getString("versionvalue")),
            saved.getId()));
  }

  @Test
  @DisplayName("key and value round-trip after the session is cleared")
  void testReload() {
    Version saved = save(new Version("keyReload", "valueReload"));

    clearSession();

    Version reloaded = store.get(saved.getId());
    assertEquals("keyReload", reloaded.getKey());
    assertEquals("valueReload", reloaded.getValue());
  }

  @Test
  @DisplayName("key round-trips a value of the full 230 character column length")
  void testKeyHoldsMaxLength() {
    String key = "k".repeat(230);
    Version saved = save(new Version(key, "long key"));

    clearSession();

    assertEquals(key, store.get(saved.getId()).getKey());
  }

  @Test
  @DisplayName("value round-trips a value of the full 255 character column length")
  void testValueHoldsMaxLength() {
    String value = "v".repeat(255);
    Version saved = save(new Version("keyLongValue", value));

    clearSession();

    assertEquals(value, store.get(saved.getId()).getValue());
  }

  @Test
  @DisplayName("value is nullable")
  void testValueIsNullable() {
    Version saved = save(new Version("keyNullValue", null));

    clearSession();

    assertNull(store.get(saved.getId()).getValue());
  }

  @Test
  @DisplayName("key is required")
  void testKeyIsRequired() {
    Version version = new Version(null, "no key");

    assertThrows(PersistenceException.class, () -> save(version));
  }

  @Test
  @DisplayName("key is unique")
  void testKeyIsUnique() {
    save(new Version("keyUnique", "first"));
    Version duplicate = new Version("keyUnique", "second");

    assertThrows(PersistenceException.class, () -> save(duplicate));
  }

  @Test
  @DisplayName("a version is found by its key")
  void testGetVersionByKey() {
    Version saved = save(new Version("keyLookup", "valueLookup"));
    save(new Version("keyOther", "valueOther"));

    clearSession();

    Version found = store.getVersionByKey("keyLookup");
    assertEquals(saved.getId(), found.getId());
    assertEquals("valueLookup", found.getValue());
    assertNull(store.getVersionByKey("keyMissing"));
  }

  @Test
  @DisplayName("updating the value of a loaded version is persisted")
  void testUpdate() {
    Version saved = save(new Version("keyUpdate", "old"));

    clearSession();

    Version loaded = store.get(saved.getId());
    loaded.setValue("new");
    store.update(loaded);
    clearSession();

    assertEquals("new", store.get(saved.getId()).getValue());
  }

  @Test
  @DisplayName("deleting a version removes only its row")
  void testDelete() {
    Version deleted = save(new Version("keyDeleted", "gone"));
    Version kept = save(new Version("keyKept", "kept"));

    store.delete(store.get(deleted.getId()));
    clearSession();

    assertNull(store.get(deleted.getId()));
    assertEquals(0, countRows(deleted.getId()));
    assertEquals(1, countRows(kept.getId()));
  }

  @Test
  @DisplayName("the entity is second-level cached with the read-write strategy")
  void testReadWriteEntityCache() {
    EntityPersister persister =
        entityManagerFactory
            .unwrap(SessionFactoryImplementor.class)
            .getMetamodel()
            .entityPersister(Version.class);

    assertTrue(persister.canReadFromCache());
    assertEquals(AccessType.READ_WRITE, persister.getCacheAccessStrategy().getAccessType());
  }

  private Version save(Version version) {
    store.save(version);
    entityManager.flush();
    return version;
  }

  private long lastSequenceValue() {
    return jdbcTemplate.queryForObject("select last_value from hibernate_sequence", Long.class);
  }

  private int countRows(long id) {
    return jdbcTemplate.queryForObject(
        "select count(*) from version where versionid = ?", Integer.class, id);
  }
}
