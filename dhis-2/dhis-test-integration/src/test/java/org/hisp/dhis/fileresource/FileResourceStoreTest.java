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
package org.hisp.dhis.fileresource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.ManyToOneType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.document.Document;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationStore;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.SchedulingType;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.joda.time.DateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class FileResourceStoreTest extends PostgresIntegrationTestBase {

  @Autowired FileResourceStore fileResourceStore;
  @Autowired JobConfigurationStore jobConfigurationStore;
  @Autowired IdentifiableObjectManager manager;
  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired EntityManagerFactory entityManagerFactory;

  @Test
  @DisplayName(
      "Getting all unassigned file resources with JOB_DATA domain should retrieve expected results")
  void getAllUnassignedWithJobDataTest() {
    // given 5 file resources exist with a mix of domains
    // 3 JOB_DATA (2 unassigned, 1 assigned)
    // 1 DATA_VALUE
    // 1 ICON
    FileResource fr0 =
        new FileResource(
            "file resource 0", "application/json", 100L, "20", FileResourceDomain.JOB_DATA);
    fr0.setAssigned(true);
    FileResource fr1 =
        new FileResource(
            "file resource 1", "application/json", 100L, "20", FileResourceDomain.JOB_DATA);
    fr1.setAssigned(false);
    FileResource fr2 =
        new FileResource(
            "file resource 2", "application/json", 200L, "30", FileResourceDomain.JOB_DATA);
    fr2.setAssigned(false);
    FileResource fr3 =
        new FileResource("file resource 3", "image/svg+xml", 100L, "20", FileResourceDomain.ICON);
    fr3.setAssigned(false);
    FileResource fr4 =
        new FileResource(
            "file resource 4", "application/pdf", 100L, "20", FileResourceDomain.DATA_VALUE);
    fr4.setAssigned(false);

    fileResourceStore.save(fr0);
    fileResourceStore.save(fr1);
    fileResourceStore.save(fr2);
    fileResourceStore.save(fr3);
    fileResourceStore.save(fr4);

    // and job config exists for only 1 of the unassigned file resources
    JobConfiguration config = new JobConfiguration(JobType.METADATA_IMPORT);
    config.setSchedulingType(SchedulingType.ONCE_ASAP);

    // simulate file resource having the same UID as its job, which is how it works for JOB_DATA
    config.setUid(fr1.getUid());
    jobConfigurationStore.save(config);

    entityManager.flush();

    // when retrieving by unassigned and JOB_DATA domain and no job config
    List<FileResource> allUnassignedByDomain =
        fileResourceStore.getAllUnassignedByJobDataDomainWithNoJobConfig();

    // then 1 unassigned file resources (JOB_DATA), with no job config is returned
    assertEquals(1, allUnassignedByDomain.size());
    assertTrue(allUnassignedByDomain.contains(fr2));
  }

  @Test
  @DisplayName("the id is generated by a sequence generator on hibernate_sequence")
  void shouldGenerateIdFromHibernateSequence() {
    SequenceStyleGenerator generator =
        assertInstanceOf(SequenceStyleGenerator.class, persister().getIdentifierGenerator());

    assertEquals(
        "hibernate_sequence",
        generator.getDatabaseStructure().getPhysicalName().getObjectName().getText());
  }

  @Test
  @DisplayName("every mapped property is written to its column and read back from it")
  void shouldWriteAndReadBackEveryMappedColumn() {
    User admin = getAdminUser();
    FileResource fileResource =
        new FileResource("photo.png", "image/png", 1234L, "md5-a", FileResourceDomain.USER_AVATAR);
    fileResource.setCode("FR_A");
    fileResource.setAssigned(true);
    fileResource.setHasMultipleStorageFiles(true);
    fileResource.setFileResourceOwner("owner-a");
    fileResourceStore.save(fileResource);
    fileResource.setLastUpdatedBy(admin);
    entityManager.flush();

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "select code, name, contenttype, contentlength, contentmd5, storagekey, isassigned,"
                + " hasmultiplestoragefiles, fileresourceowner, domain, userid, lastupdatedby"
                + " from fileresource where fileresourceid = ?",
            fileResource.getId());
    assertEquals("FR_A", row.get("code"));
    assertEquals("photo.png", row.get("name"));
    assertEquals("image/png", row.get("contenttype"));
    assertEquals(1234L, ((Number) row.get("contentlength")).longValue());
    assertEquals("md5-a", row.get("contentmd5"));
    assertEquals(fileResource.getStorageKey(), row.get("storagekey"));
    assertTrue((Boolean) row.get("isassigned"));
    assertTrue((Boolean) row.get("hasmultiplestoragefiles"));
    assertEquals("owner-a", row.get("fileresourceowner"));
    assertEquals("USER_AVATAR", row.get("domain"));
    assertEquals(admin.getId(), ((Number) row.get("userid")).longValue());
    assertEquals(admin.getId(), ((Number) row.get("lastupdatedby")).longValue());

    FileResource reloaded = reload(fileResource);
    assertEquals("FR_A", reloaded.getCode());
    assertEquals("photo.png", reloaded.getName());
    assertEquals("image/png", reloaded.getContentType());
    assertEquals(1234L, reloaded.getContentLength());
    assertEquals("md5-a", reloaded.getContentMd5());
    assertEquals(fileResource.getStorageKey(), reloaded.getStorageKey());
    assertTrue(reloaded.isAssigned());
    assertTrue(reloaded.isHasMultipleStorageFiles());
    assertEquals("owner-a", reloaded.getFileResourceOwner());
    assertEquals(FileResourceDomain.USER_AVATAR, reloaded.getDomain());
    assertEquals(admin.getUid(), reloaded.getCreatedBy().getUid());
    assertEquals(admin.getUid(), reloaded.getLastUpdatedBy().getUid());
  }

  @Test
  @DisplayName("storage status is not persisted and loads as NONE")
  void shouldNotPersistStorageStatus() {
    FileResource fileResource = saveFileResource('S', FileResourceDomain.DOCUMENT);
    fileResource.setStorageStatus(FileResourceStorageStatus.STORED);
    entityManager.flush();

    assertEquals(FileResourceStorageStatus.NONE, reload(fileResource).getStorageStatus());
  }

  @Test
  @DisplayName("created by and last updated by are lazy many-to-ones")
  void shouldMapUserReferencesLazily() {
    assertFalse(
        assertInstanceOf(ManyToOneType.class, persister().getPropertyType("createdBy"))
            .isEager(null));
    assertFalse(
        assertInstanceOf(ManyToOneType.class, persister().getPropertyType("lastUpdatedBy"))
            .isEager(null));
  }

  @Test
  @DisplayName("file and identity properties are mapped not-null, the others nullable")
  void shouldMapNullability() {
    for (String property :
        List.of(
            "uid",
            "created",
            "lastUpdated",
            "name",
            "contentType",
            "contentLength",
            "contentMd5",
            "storageKey",
            "assigned")) {
      assertFalse(isNullable(property), property + " is mapped nullable");
    }
    for (String property :
        List.of(
            "code",
            "domain",
            "hasMultipleStorageFiles",
            "fileResourceOwner",
            "createdBy",
            "lastUpdatedBy")) {
      assertTrue(isNullable(property), property + " is mapped not-null");
    }
  }

  @Test
  @DisplayName("the entity is second-level cached with read-write")
  void shouldCacheEntityReadWrite() {
    EntityPersister persister = persister();

    assertTrue(persister.canReadFromCache());
    assertEquals(AccessType.READ_WRITE, persister.getCacheAccessStrategy().getAccessType());
  }

  @Test
  @DisplayName("unprocessed images are images of a multiple-image domain without extra files")
  void shouldGetAllUnProcessedImages() {
    FileResource image = saveFileResource('A', FileResourceDomain.ORG_UNIT, "image/png");
    FileResource processed = saveFileResource('B', FileResourceDomain.ORG_UNIT, "image/png");
    processed.setHasMultipleStorageFiles(true);
    saveFileResource('C', FileResourceDomain.ORG_UNIT, "application/pdf");
    saveFileResource('D', FileResourceDomain.ICON, "image/png");
    entityManager.flush();

    assertEquals(List.of(image), fileResourceStore.getAllUnProcessedImages());
  }

  @Test
  @DisplayName("expired data value file resources are unassigned and older than both periods")
  void shouldGetExpiredDataValueFileResources() {
    FileResource expired = saveFileResource('A', FileResourceDomain.DATA_VALUE);
    FileResource recent = saveFileResource('B', FileResourceDomain.DATA_VALUE);
    FileResource assigned = saveFileResource('C', FileResourceDomain.DATA_VALUE);
    assigned.setAssigned(true);
    FileResource document = saveFileResource('D', FileResourceDomain.DOCUMENT);
    entityManager.flush();
    setLastUpdatedDaysAgo(10, expired, assigned, document);
    setLastUpdatedDaysAgo(1, recent);

    DateTime threeDaysAgo = DateTime.now().minusDays(3);
    assertEquals(
        List.of(expired.getUid()),
        uids(fileResourceStore.getExpiredDataValueFileResources(threeDaysAgo, threeDaysAgo)));
  }

  @Test
  @DisplayName("unassigned file resources of the given domains past the grace period are found")
  void shouldGetUnassignedPassedGracePeriod() {
    FileResource expired = saveFileResource('A', FileResourceDomain.ICON);
    FileResource recent = saveFileResource('B', FileResourceDomain.ICON);
    FileResource otherDomain = saveFileResource('C', FileResourceDomain.DOCUMENT);
    entityManager.flush();
    setLastUpdatedDaysAgo(10, expired, otherDomain);
    setLastUpdatedDaysAgo(1, recent);

    assertEquals(
        List.of(expired.getUid()),
        uids(
            fileResourceStore.getUnassignedPassedGracePeriod(
                Set.of(FileResourceDomain.ICON), DateTime.now().minusDays(3))));
  }

  @Test
  @DisplayName("a file resource is found by its storage key, and by uid within its domain")
  void shouldFindByStorageKeyAndByUidAndDomain() {
    FileResource fileResource = saveFileResource('A', FileResourceDomain.MESSAGE_ATTACHMENT);
    entityManager.flush();

    assertEquals(
        fileResource.getUid(),
        fileResourceStore.findByStorageKey(fileResource.getStorageKey()).orElseThrow().getUid());
    assertTrue(fileResourceStore.findByStorageKey("unknown/key").isEmpty());
    assertEquals(
        fileResource.getUid(),
        fileResourceStore
            .findByUidAndDomain(fileResource.getUid(), FileResourceDomain.MESSAGE_ATTACHMENT)
            .orElseThrow()
            .getUid());
    assertTrue(
        fileResourceStore
            .findByUidAndDomain(fileResource.getUid(), FileResourceDomain.DATA_VALUE)
            .isEmpty());
  }

  @Test
  @DisplayName("update assignment writes the assigned flag and the owner")
  void shouldUpdateAssignment() {
    FileResource fileResource = saveFileResource('A', FileResourceDomain.DATA_VALUE);
    entityManager.flush();

    fileResourceStore.updateAssignment(fileResource.getUid(), true, "owner-b");

    FileResource reloaded = reload(fileResource);
    assertTrue(reloaded.isAssigned());
    assertEquals("owner-b", reloaded.getFileResourceOwner());
  }

  @Test
  @DisplayName("the owners of a file resource are found through their own tables")
  void shouldFindOwnersOfFileResource() {
    FileResource image = saveFileResource('A', FileResourceDomain.ORG_UNIT);
    OrganisationUnit unit = createOrganisationUnit('A');
    unit.setImage(image);
    manager.save(unit);

    FileResource avatar = saveFileResource('B', FileResourceDomain.USER_AVATAR);
    User user = makeUser("A");
    user.setAvatar(avatar);
    userService.addUser(user);

    FileResource file = saveFileResource('C', FileResourceDomain.DOCUMENT);
    Document document = new Document("Document A", file.getUid(), false, null);
    document.setFileResource(file);
    manager.save(document);

    entityManager.flush();

    assertEquals(
        List.of(unit.getUid()),
        fileResourceStore.findOrganisationUnitsByImageFileResource(image.getUid()));
    assertEquals(
        List.of(user.getUid()), fileResourceStore.findUsersByAvatarFileResource(avatar.getUid()));
    assertEquals(
        List.of(document.getUid()), fileResourceStore.findDocumentsByFileResource(file.getUid()));
    assertEquals(List.of(), fileResourceStore.findMessagesByFileResource(file.getUid()));
    assertEquals(List.of(), fileResourceStore.findDataValuesByFileResourceValue(file.getUid()));
  }

  private FileResource saveFileResource(char uniqueChar, FileResourceDomain domain) {
    return saveFileResource(uniqueChar, domain, "application/octet-stream");
  }

  private FileResource saveFileResource(
      char uniqueChar, FileResourceDomain domain, String contentType) {
    FileResource fileResource =
        createFileResource(uniqueChar, ("content " + uniqueChar).getBytes(StandardCharsets.UTF_8));
    fileResource.setDomain(domain);
    fileResource.setContentType(contentType);
    fileResourceStore.save(fileResource);
    return fileResource;
  }

  private void setLastUpdatedDaysAgo(int days, FileResource... fileResources) {
    for (FileResource fileResource : fileResources) {
      jdbcTemplate.update(
          "update fileresource set lastupdated = now() - make_interval(days => ?)"
              + " where fileresourceid = ?",
          days,
          fileResource.getId());
    }
    clearSession();
    entityManagerFactory.getCache().evict(FileResource.class);
  }

  private static List<String> uids(List<FileResource> fileResources) {
    return fileResources.stream().map(FileResource::getUid).toList();
  }

  private EntityPersister persister() {
    return entityManagerFactory
        .unwrap(SessionFactoryImplementor.class)
        .getMetamodel()
        .entityPersister(FileResource.class);
  }

  /** {@link EntityPersister#getPropertyNullability()} holds a nullable flag per property. */
  private boolean isNullable(String property) {
    EntityPersister persister = persister();
    return persister
        .getPropertyNullability()[persister.getEntityMetamodel().getPropertyIndex(property)];
  }

  /** Reloads the file resource from the database, bypassing the session and the L2 cache. */
  private FileResource reload(FileResource fileResource) {
    clearSession();
    entityManagerFactory.getCache().evict(FileResource.class);
    return fileResourceStore.get(fileResource.getId());
  }
}
