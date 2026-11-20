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
package org.hisp.dhis.datastore;

import static org.hisp.dhis.config.HibernateEncryptionConfig.AES_128_STRING_ENCRYPTOR;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import org.hibernate.type.ManyToOneType;
import org.hisp.dhis.hibernate.encryption.type.EncryptedStringUserType;
import org.hisp.dhis.hibernate.jsonb.type.JsonBinaryPlainStringType;
import org.hisp.dhis.hibernate.jsonb.type.SharingJsonBinaryType;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Stian Sandvold.
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class DatastoreEntryStoreTest extends PostgresIntegrationTestBase {

  @Autowired private DatastoreStore store;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Autowired
  @Qualifier(AES_128_STRING_ENCRYPTOR)
  private PBEStringEncryptor aes128StringEncryptor;

  @Test
  void testAddKeyJsonValue() {
    DatastoreEntry entry = addEntry("A", "1");
    assertNotNull(entry);
    assertEquals(entry, store.get(entry.getId()));
  }

  @Test
  void testAddKeyJsonValuesAndGetNamespaces() {
    addEntry("A", "1");
    addEntry("B", "1");
    assertContainsOnly(List.of("A", "B"), store.getNamespaces());
  }

  @Test
  void testAddKeyJsonValuesAndGetKeysFromNamespace() {
    addEntry("A", "1");
    addEntry("A", "2");
    addEntry("B", "1");
    assertContainsOnly(List.of("1", "2"), store.getKeysInNamespace("A"));
  }

  @Test
  void testAddKeyJsonValueAndGetKeyJsonValue() {
    DatastoreEntry entryA = addEntry("A", "1");
    assertEquals(store.getEntry("A", "1"), entryA);
  }

  @Test
  void testGetKeyJsonValuesByNamespace() {
    DatastoreEntry entryA1 = addEntry("A", "1");
    DatastoreEntry entryA2 = addEntry("A", "2");
    DatastoreEntry entryA3 = addEntry("A", "3");
    DatastoreEntry entryB1 = addEntry("B", "1");
    assertContainsOnly(List.of(entryA1, entryA2, entryA3), store.getEntriesInNamespace("A"));
    assertFalse(store.getEntriesInNamespace("A").contains(entryB1));
  }

  @Test
  void testCountKeysInNamespace() {
    addEntry("A", "1");
    addEntry("A", "2");
    addEntry("A", "3");
    addEntry("B", "1");
    assertEquals(3, store.countKeysInNamespace("A"));
    assertEquals(1, store.countKeysInNamespace("B"));
    assertEquals(0, store.countKeysInNamespace("C"));
  }

  @Test
  void deleteNamespace() {
    addEntry("A", "1");
    addEntry("A", "3");
    addEntry("B", "1");
    addEntry("C", "1");
    store.deleteNamespace("A");
    assertContainsOnly(List.of("B", "C"), store.getNamespaces());
  }

  @Test
  void testEncryptedValueIsStoredEncryptedAndReadBackDecrypted() {
    String secret = "{\"password\": \"s3cr3t-value\"}";
    DatastoreEntry entry = new DatastoreEntry("enc", "secret", secret, true);
    store.save(entry);
    entityManager.flush();

    Map<String, Object> row = valueColumns(entry);
    String stored = (String) row.get("encrypted_value");
    assertNotNull(stored);
    assertNotEquals(secret, stored);
    assertFalse(stored.contains("s3cr3t-value"), "the secret must not be stored in plain text");
    assertEquals(secret, aes128StringEncryptor.decrypt(stored));
    assertNull(row.get("jbvalue"), "an encrypted value must not be written to the jsonb column");
    assertEquals(Boolean.TRUE, row.get("encrypted"));

    DatastoreEntry reloaded = reload(entry);
    assertTrue(reloaded.getEncrypted());
    assertEquals(secret, reloaded.getEncryptedValue());
    assertEquals(secret, reloaded.getValue());
    assertNull(reloaded.getJbPlainValue());
  }

  @Test
  void testPlainValueIsStoredAsJsonbAndNotEncrypted() {
    DatastoreEntry entry = new DatastoreEntry("plain", "key", "{\"a\": 1}", false);
    store.save(entry);
    entityManager.flush();

    Map<String, Object> row = valueColumns(entry);
    assertEquals("{\"a\": 1}", row.get("jbvalue"));
    assertNull(row.get("encrypted_value"));
    assertEquals(Boolean.FALSE, row.get("encrypted"));

    DatastoreEntry reloaded = reload(entry);
    assertFalse(reloaded.getEncrypted());
    assertEquals("{\"a\": 1}", reloaded.getJbPlainValue());
    assertEquals("{\"a\": 1}", reloaded.getValue());
    assertNull(reloaded.getEncryptedValue());
  }

  @Test
  void testNewValueOfLoadedEntryIsWrittenOnFlush() {
    DatastoreEntry plain = new DatastoreEntry("change", "plain", "{\"v\": 1}", false);
    DatastoreEntry encrypted = new DatastoreEntry("change", "enc", "\"old-secret\"", true);
    store.save(plain);
    store.save(encrypted);

    DatastoreEntry loadedPlain = reload(plain);
    DatastoreEntry loadedEncrypted = store.get(encrypted.getId());
    loadedPlain.setValue("{\"v\": 2}");
    loadedEncrypted.setValue("\"new-secret\"");
    entityManager.flush();

    assertEquals("{\"v\": 2}", valueColumns(plain).get("jbvalue"));
    Map<String, Object> encryptedRow = valueColumns(encrypted);
    String stored = (String) encryptedRow.get("encrypted_value");
    assertNotEquals("\"new-secret\"", stored);
    assertFalse(stored.contains("new-secret"), "the new secret must not be stored in plain text");
    assertEquals("\"new-secret\"", aes128StringEncryptor.decrypt(stored));
    assertNull(encryptedRow.get("jbvalue"), "an encrypted value must not be written to jsonb");
    assertEquals("\"new-secret\"", reload(encrypted).getEncryptedValue());
  }

  @Test
  void testSharingIsStoredInTheSharingColumnAndReadBack() {
    User user = createUserWithAuth("sharinguser");
    DatastoreEntry entry = new DatastoreEntry("shared", "key", "{}", false);
    entry.getSharing().setPublicAccess("r-------");
    entry.getSharing().addUserAccess(new UserAccess(user, "rw------"));
    store.save(entry, false);
    entityManager.flush();

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "select sharing->>'public' as public_access,"
                + " sharing->'users'->?->>'access' as user_access"
                + " from keyjsonvalue where keyjsonvalueid = ?",
            user.getUid(),
            entry.getId());
    assertEquals("r-------", row.get("public_access"));
    assertEquals("rw------", row.get("user_access"));

    Sharing reloaded = reload(entry).getSharing();
    assertEquals("r-------", reloaded.getPublicAccess());
    assertEquals("rw------", reloaded.getUsers().get(user.getUid()).getAccess());
  }

  @Test
  void testChangedSharingOfLoadedEntryIsWrittenOnFlush() {
    DatastoreEntry entry = new DatastoreEntry("shared", "change", "{}", false);
    entry.getSharing().setPublicAccess("rw------");
    store.save(entry, false);

    reload(entry).getSharing().setPublicAccess("--------");
    entityManager.flush();

    assertEquals(
        "--------",
        jdbcTemplate.queryForObject(
            "select sharing->>'public' from keyjsonvalue where keyjsonvalueid = ?",
            String.class,
            entry.getId()));
  }

  @Test
  void testColumnsRoundTrip() {
    User user = createUserWithAuth("columnsuser");
    User currentUser = getCurrentUser();
    DatastoreEntry entry = new DatastoreEntry("columns", "the-key", "[1, 2]", false);
    entry.setCode("DSE_CODE");
    entry.setCreatedBy(user);
    store.save(entry);
    entityManager.flush();

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "select uid, code, created, lastupdated, lastupdatedby, userid, namespace, namespacekey"
                + " from keyjsonvalue where keyjsonvalueid = ?",
            entry.getId());
    assertEquals(entry.getUid(), row.get("uid"));
    assertEquals("DSE_CODE", row.get("code"));
    assertNotNull(row.get("created"));
    assertNotNull(row.get("lastupdated"));
    assertEquals(currentUser.getId(), ((Number) row.get("lastupdatedby")).longValue());
    assertEquals(user.getId(), ((Number) row.get("userid")).longValue());
    assertEquals("columns", row.get("namespace"));
    assertEquals("the-key", row.get("namespacekey"));

    DatastoreEntry reloaded = reload(entry);
    assertEquals(entry.getUid(), reloaded.getUid());
    assertEquals("DSE_CODE", reloaded.getCode());
    assertEquals(entry.getCreated().getTime(), reloaded.getCreated().getTime());
    assertEquals(entry.getLastUpdated().getTime(), reloaded.getLastUpdated().getTime());
    assertEquals(currentUser.getUid(), reloaded.getLastUpdatedBy().getUid());
    assertEquals(user.getUid(), reloaded.getCreatedBy().getUid());
    assertEquals("columns", reloaded.getNamespace());
    assertEquals("the-key", reloaded.getKey());
  }

  @Test
  void testGetKeysInNamespaceFiltersByLastUpdated() {
    DatastoreEntry older = addEntry("since", "older");
    addEntry("since", "newer");
    entityManager.flush();
    jdbcTemplate.update(
        "update keyjsonvalue set lastupdated = '2000-01-01' where keyjsonvalueid = ?",
        older.getId());
    clearSession();

    assertContainsOnly(
        List.of("newer"),
        store.getKeysInNamespace("since", new Date(System.currentTimeMillis() - 60_000)));
    assertContainsOnly(List.of("older", "newer"), store.getKeysInNamespace("since", null));
  }

  @Test
  void testMappedUserTypes() {
    EntityPersister persister = persister();

    CustomType encryptedValue =
        assertInstanceOf(CustomType.class, persister.getPropertyType("encryptedValue"));
    assertInstanceOf(EncryptedStringUserType.class, encryptedValue.getUserType());
    CustomType jbPlainValue =
        assertInstanceOf(CustomType.class, persister.getPropertyType("jbPlainValue"));
    assertInstanceOf(JsonBinaryPlainStringType.class, jbPlainValue.getUserType());
    CustomType sharing = assertInstanceOf(CustomType.class, persister.getPropertyType("sharing"));
    assertInstanceOf(SharingJsonBinaryType.class, sharing.getUserType());
    assertEquals(Sharing.class, sharing.getReturnedClass());
  }

  @Test
  void testPersisterMetadata() {
    EntityPersister persister = persister();

    assertEquals(
        List.of(
            "code",
            "created",
            "createdBy",
            "encrypted",
            "encryptedValue",
            "jbPlainValue",
            "key",
            "lastUpdated",
            "lastUpdatedBy",
            "namespace",
            "sharing",
            "uid"),
        List.of(persister.getPropertyNames()).stream().sorted().toList());

    for (String property : List.of("uid", "created", "lastUpdated", "namespace", "key")) {
      assertFalse(isNullable(property), property + " is mapped nullable");
    }
    for (String property :
        List.of(
            "code",
            "createdBy",
            "lastUpdatedBy",
            "jbPlainValue",
            "encryptedValue",
            "encrypted",
            "sharing")) {
      assertTrue(isNullable(property), property + " is mapped not-null");
    }
    assertFalse(
        persister
            .getPropertyUpdateability()[persister.getEntityMetamodel().getPropertyIndex("created")],
        "created is updatable");

    for (String property : List.of("createdBy", "lastUpdatedBy")) {
      ManyToOneType type =
          assertInstanceOf(ManyToOneType.class, persister.getPropertyType(property));
      assertFalse(type.isEager(null), property + " is eager");
    }

    assertEquals(AccessType.READ_WRITE, persister.getCacheAccessStrategy().getAccessType());

    SequenceStyleGenerator generator =
        assertInstanceOf(SequenceStyleGenerator.class, persister.getIdentifierGenerator());
    assertEquals(
        "hibernate_sequence",
        generator.getDatabaseStructure().getPhysicalName().getObjectName().getText());
  }

  private Map<String, Object> valueColumns(DatastoreEntry entry) {
    return jdbcTemplate.queryForMap(
        "select encrypted_value, jbvalue::text as jbvalue, encrypted from keyjsonvalue"
            + " where keyjsonvalueid = ?",
        entry.getId());
  }

  private DatastoreEntry reload(DatastoreEntry entry) {
    entityManager.flush();
    clearSession();
    entityManagerFactory.getCache().evict(DatastoreEntry.class);
    return store.get(entry.getId());
  }

  private EntityPersister persister() {
    return entityManagerFactory
        .unwrap(SessionFactoryImplementor.class)
        .getMetamodel()
        .entityPersister(DatastoreEntry.class);
  }

  /** {@link EntityPersister#getPropertyNullability()} holds a nullable flag per property. */
  private boolean isNullable(String property) {
    EntityPersister persister = persister();
    return persister
        .getPropertyNullability()[persister.getEntityMetamodel().getPropertyIndex(property)];
  }

  private DatastoreEntry addEntry(String ns, String key) {
    DatastoreEntry entry = new DatastoreEntry(ns, key);
    store.save(entry);
    return entry;
  }
}
