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
package org.hisp.dhis.metadata.version.hibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.CustomType;
import org.hibernate.type.EnumType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.TimestampType;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.metadata.version.MetadataVersionStore;
import org.hisp.dhis.metadata.version.VersionType;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author sultanm.
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class HibernateMetadataVersionStoreTest extends PostgresIntegrationTestBase {

  @Autowired private MetadataVersionStore metadataVersionStore;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private EntityManagerFactory entityManagerFactory;

  public void testGetInitialVersion() throws Exception {
    MetadataVersion metadataVersion1 = new MetadataVersion("version1", VersionType.ATOMIC);
    metadataVersion1.setHashCode("12232");
    metadataVersionStore.save(metadataVersion1);
    MetadataVersion metadataVersion2 = new MetadataVersion("version2", VersionType.ATOMIC);
    metadataVersion2.setHashCode("12222");
    metadataVersionStore.save(metadataVersion2);
    assertEquals(metadataVersion1, metadataVersionStore.getInitialVersion());
    metadataVersionStore.delete(metadataVersion1);
    metadataVersionStore.delete(metadataVersion2);
    assertNull(metadataVersionStore.getInitialVersion());
  }

  @Test
  void testGetVersionByKey() throws Exception {
    MetadataVersion metadataVersion = new MetadataVersion("version1", VersionType.ATOMIC);
    metadataVersion.setHashCode("12345");
    metadataVersionStore.save(metadataVersion);
    assertEquals(
        metadataVersion,
        metadataVersionStore.getVersionByKey(metadataVersionStore.getCurrentVersion().getId()));
    int nonExistingId = 9999;
    assertNull(metadataVersionStore.getVersionByKey(nonExistingId));
    metadataVersionStore.delete(metadataVersion);
  }

  @Test
  void testGetVersionByName() throws Exception {
    MetadataVersion metadataVersion = new MetadataVersion("version1", VersionType.ATOMIC);
    metadataVersion.setHashCode("12345");
    metadataVersionStore.save(metadataVersion);
    assertEquals(metadataVersion, metadataVersionStore.getVersionByName("version1"));
    assertNull(metadataVersionStore.getVersionByName("non_existing"));
    metadataVersionStore.delete(metadataVersion);
  }

  @Test
  void testGetAllVersionsInBetween() throws Exception {
    DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ssZ");
    org.joda.time.DateTime dateTime1 = dateTimeFormatter.parseDateTime("2016-06-20 10:45:50Z");
    org.joda.time.DateTime dateTime2 = dateTimeFormatter.parseDateTime("2016-06-21 10:45:50Z");
    org.joda.time.DateTime dateTime3 = dateTimeFormatter.parseDateTime("2016-06-22 10:45:50Z");
    assertEquals(0, metadataVersionStore.getAllVersionsInBetween(new Date(), new Date()).size());
    MetadataVersion metadataVersion2 = new MetadataVersion("version2", VersionType.ATOMIC);
    metadataVersion2.setHashCode("12222");
    metadataVersion2.setCreated(dateTime1.toDate());
    metadataVersionStore.save(metadataVersion2);
    MetadataVersion metadataVersion3 = new MetadataVersion("version3", VersionType.ATOMIC);
    metadataVersion3.setHashCode("12255");
    metadataVersion3.setCreated(dateTime2.toDate());
    metadataVersionStore.save(metadataVersion3);
    MetadataVersion metadataVersion4 = new MetadataVersion("version4", VersionType.ATOMIC);
    metadataVersion4.setHashCode("12267");
    metadataVersion4.setCreated(dateTime3.toDate());
    metadataVersionStore.save(metadataVersion4);
    List<MetadataVersion> allVersionsInBetween =
        metadataVersionStore.getAllVersionsInBetween(dateTime1.toDate(), dateTime2.toDate());
    assertEquals(2, allVersionsInBetween.size());
    assertEquals(metadataVersion2, allVersionsInBetween.get(0));
    assertEquals(metadataVersion3, allVersionsInBetween.get(1));
    assertEquals(0, metadataVersionStore.getAllVersionsInBetween(new Date(), new Date()).size());
    metadataVersionStore.delete(metadataVersion2);
    metadataVersionStore.delete(metadataVersion3);
    metadataVersionStore.delete(metadataVersion4);
  }

  @Test
  void testUpdate() throws Exception {
    MetadataVersion metadataVersion = new MetadataVersion("version1", VersionType.ATOMIC);
    metadataVersion.setHashCode("12345");
    metadataVersionStore.save(metadataVersion);
    metadataVersion.setName("newVersion");
    metadataVersionStore.update(metadataVersion);
    assertNotNull(metadataVersionStore.getVersionByName("newVersion"));
    metadataVersionStore.delete(metadataVersion);
  }

  @Test
  void testDelete() throws Exception {
    assertNull(metadataVersionStore.getVersionByName("version1"));
  }

  @Test
  void testIdGeneratorUsesHibernateSequence() {
    SequenceStyleGenerator generator =
        assertInstanceOf(SequenceStyleGenerator.class, persister().getIdentifierGenerator());

    assertEquals(
        "hibernate_sequence",
        generator.getDatabaseStructure().getPhysicalName().getObjectName().getText());
  }

  @Test
  void testColumnsRoundTrip() {
    User admin = getAdminUser();
    Date created = date("2016-06-20 10:45:50Z");
    Date importDate = date("2016-06-21 08:30:15Z");
    MetadataVersion version = new MetadataVersion("Version_RoundTrip", VersionType.BEST_EFFORT);
    version.setCode("CODE_ROUND_TRIP");
    version.setCreated(created);
    version.setImportDate(importDate);
    version.setHashCode("6cd02b2c1fe034c18ac0006bd87001ca");
    version.setLastUpdatedBy(admin);
    MetadataVersion saved = save(version);

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "select uid, code, name, versiontype, importdate, hashcode, created, lastupdated,"
                + " lastupdatedby from metadataversion where versionid = ?",
            saved.getId());
    assertEquals(saved.getUid(), row.get("uid"));
    assertEquals("CODE_ROUND_TRIP", row.get("code"));
    assertEquals("Version_RoundTrip", row.get("name"));
    assertEquals("BEST_EFFORT", row.get("versiontype"));
    assertEquals(importDate.getTime(), ((Date) row.get("importdate")).getTime());
    assertEquals("6cd02b2c1fe034c18ac0006bd87001ca", row.get("hashcode"));
    assertEquals(created.getTime(), ((Date) row.get("created")).getTime());
    assertEquals(saved.getLastUpdated().getTime(), ((Date) row.get("lastupdated")).getTime());
    assertEquals(admin.getId(), ((Number) row.get("lastupdatedby")).longValue());

    MetadataVersion reloaded = reload(saved);
    assertEquals(saved.getUid(), reloaded.getUid());
    assertEquals("CODE_ROUND_TRIP", reloaded.getCode());
    assertEquals("Version_RoundTrip", reloaded.getName());
    assertEquals(VersionType.BEST_EFFORT, reloaded.getType());
    assertEquals(importDate.getTime(), reloaded.getImportDate().getTime());
    assertEquals("6cd02b2c1fe034c18ac0006bd87001ca", reloaded.getHashCode());
    assertEquals(created.getTime(), reloaded.getCreated().getTime());
    assertEquals(saved.getLastUpdated().getTime(), reloaded.getLastUpdated().getTime());
    assertEquals(admin.getUid(), reloaded.getLastUpdatedBy().getUid());
  }

  @ParameterizedTest
  @EnumSource(VersionType.class)
  void testTypeIsReadFromItsEnumName(VersionType type) {
    MetadataVersion saved = save(newVersion("Version_" + type, "hash_" + type));
    jdbcTemplate.update(
        "update metadataversion set versiontype = ? where versionid = ?",
        type.name(),
        saved.getId());

    assertEquals(type, reload(saved).getType());
  }

  @Test
  void testTypeIsMappedAsNamedVarcharEnum() {
    CustomType customType = assertInstanceOf(CustomType.class, persister().getPropertyType("type"));
    EnumType<?> enumType = assertInstanceOf(EnumType.class, customType.getUserType());

    assertEquals(VersionType.class, enumType.returnedClass());
    assertFalse(enumType.isOrdinal());
    assertEquals(Types.VARCHAR, enumType.sqlTypes()[0]);
  }

  @ParameterizedTest
  @ValueSource(strings = {"created", "lastUpdated", "importDate"})
  void testDatesAreMappedAsTimestamps(String property) {
    assertInstanceOf(TimestampType.class, persister().getPropertyType(property));
  }

  @Test
  void testLastUpdatedByIsLazy() {
    ManyToOneType type =
        assertInstanceOf(ManyToOneType.class, persister().getPropertyType("lastUpdatedBy"));

    assertFalse(type.isEager(null));
  }

  @ParameterizedTest
  @ValueSource(strings = {"uid", "name", "hashCode", "created", "lastUpdated"})
  void testRequiredPropertiesAreMappedNotNull(String property) {
    assertFalse(isNullable(property), property + " is mapped nullable");
  }

  @ParameterizedTest
  @ValueSource(strings = {"code", "type", "importDate", "lastUpdatedBy"})
  void testOptionalPropertiesAreMappedNullable(String property) {
    assertTrue(isNullable(property), property + " is mapped not-null");
  }

  @Test
  void testCreatedIsNotUpdatable() {
    EntityPersister persister = persister();
    EntityMetamodel metamodel = persister.getEntityMetamodel();
    boolean[] updateability = persister.getPropertyUpdateability();

    assertFalse(updateability[metamodel.getPropertyIndex("created")]);
    assertTrue(updateability[metamodel.getPropertyIndex("lastUpdated")]);
  }

  @Test
  void testIsNotSecondLevelCached() {
    EntityPersister persister = persister();

    assertFalse(persister.canWriteToCache());
    assertNull(persister.getCacheAccessStrategy());
  }

  @Test
  void testOnlyTheTableColumnsAreMapped() {
    assertEquals(
        List.of(
            "code",
            "created",
            "hashCode",
            "importDate",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "type",
            "uid"),
        List.of(persister().getPropertyNames()).stream().sorted().toList());
  }

  @Test
  void testCreatedByAndSharingAreNotPersisted() {
    MetadataVersion saved = save(newVersion("Version_Owner", "hash_owner"));
    assertNotNull(saved.getCreatedBy());
    assertNotNull(saved.getSharing().getOwner());

    MetadataVersion reloaded = reload(saved);
    assertNull(reloaded.getCreatedBy());
    assertNull(reloaded.getSharing().getOwner());
    assertNull(reloaded.getSharing().getPublicAccess());
  }

  @Test
  void testQueriesReadPersistedVersions() {
    MetadataVersion first = newVersion("Version_First", "hash_first");
    first.setCreated(date("2016-06-20 10:45:50Z"));
    MetadataVersion second = newVersion("Version_Second", "hash_second");
    second.setCreated(date("2016-06-21 10:45:50Z"));
    MetadataVersion third = newVersion("Version_Third", "hash_third");
    third.setCreated(date("2016-06-22 10:45:50Z"));
    save(first);
    save(second);
    save(third);
    clearSession();
    entityManagerFactory.getCache().evict(MetadataVersion.class);

    assertEquals("hash_third", metadataVersionStore.getCurrentVersion().getHashCode());
    assertEquals("hash_first", metadataVersionStore.getInitialVersion().getHashCode());
    assertEquals(
        "hash_second", metadataVersionStore.getVersionByName("Version_Second").getHashCode());
    assertEquals("Version_Second", metadataVersionStore.getVersionByKey(second.getId()).getName());
    assertEquals(
        List.of("Version_First", "Version_Second"),
        metadataVersionStore
            .getAllVersionsInBetween(date("2016-06-20 00:00:00Z"), date("2016-06-21 23:59:59Z"))
            .stream()
            .map(MetadataVersion::getName)
            .sorted()
            .toList());
  }

  private static MetadataVersion newVersion(String name, String hashCode) {
    MetadataVersion version = new MetadataVersion(name, VersionType.ATOMIC);
    version.setHashCode(hashCode);
    return version;
  }

  private static Date date(String dateTime) {
    return DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ssZ").parseDateTime(dateTime).toDate();
  }

  private EntityPersister persister() {
    return entityManagerFactory
        .unwrap(SessionFactoryImplementor.class)
        .getMetamodel()
        .entityPersister(MetadataVersion.class);
  }

  /** {@link EntityPersister#getPropertyNullability()} holds a nullable flag per property. */
  private boolean isNullable(String property) {
    EntityPersister persister = persister();
    return persister
        .getPropertyNullability()[persister.getEntityMetamodel().getPropertyIndex(property)];
  }

  private MetadataVersion save(MetadataVersion version) {
    metadataVersionStore.save(version);
    entityManager.flush();
    return version;
  }

  /** Reloads the version from the database, bypassing the session and the second-level cache. */
  private MetadataVersion reload(MetadataVersion version) {
    clearSession();
    entityManagerFactory.getCache().evict(MetadataVersion.class);
    return metadataVersionStore.get(version.getId());
  }
}
