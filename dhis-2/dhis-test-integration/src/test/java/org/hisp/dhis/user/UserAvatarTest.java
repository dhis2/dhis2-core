/*
 * Copyright (c) 2004-2025, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.user;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.Hibernate;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportService;
import org.hisp.dhis.dxf2.metadata.MetadataObjects;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.TestTransaction;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class UserAvatarTest extends SingleSetupIntegrationTestBase {
  @Autowired private MetadataImportService metadataImportService;
  private final List<Long> users = new ArrayList<>();
  private final List<Long> resources = new ArrayList<>();

  @BeforeEach
  void setUp() {
    // The 2.41 base injects the admin once in @BeforeAll; re-assert it for every test method.
    reLoginAdminUser();
  }

  @AfterEach
  void cleanUpCommittedFixtures() {
    // These tests cross real commit boundaries; the normal test rollback is not sufficient.
    if (!TestTransaction.isActive()) TestTransaction.start();
    for (Long id : users) {
      User user = entityManager.find(User.class, id);
      if (user == null) continue;
      user.setAvatar(null);
      // On 2.41 User.userRoles is mapped with cascade="all", so removing a fixture user cascades
      // the delete onto the UserRole rows it references. The import case reuses the admin user's
      // roles, and deleting those breaks fk_userrolemembers_userroleid for the remaining members.
      // Drop the memberships first so only the fixture user itself is deleted.
      user.getUserRoles().clear();
    }
    entityManager.flush();
    for (Long id : resources) {
      FileResource resource = entityManager.find(FileResource.class, id);
      if (resource != null) entityManager.remove(resource);
    }
    entityManager.flush();
    for (Long id : users) {
      User user = entityManager.find(User.class, id);
      if (user != null) entityManager.remove(user);
    }
    commitAndClear();
    users.clear();
    resources.clear();
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void loadingUserDoesNotInitializeOrAssignAvatar(boolean assigned) {
    User owner = persistUser('O');
    FileResource ownerAvatar = persistAvatar('O');
    owner.setAvatar(ownerAvatar);
    FileResource avatar = persistAvatar('A');
    avatar.setCreatedBy(owner);
    User user = persistUser('A');
    user.setAvatar(avatar);
    // Hydration must restore even inconsistent persisted state, not silently repair it.
    avatar.setAssigned(assigned);
    long userId = user.getId();
    long avatarId = avatar.getId();
    commitAndClear();
    entityManager.getEntityManagerFactory().getCache().evictAll();

    // Cold load followed by a warm-cache load, each in a fresh persistence context.
    for (int load = 0; load < 2; load++) {
      User loaded = entityManager.find(User.class, userId);
      boolean initializedByUserLoad = Hibernate.isInitialized(loaded.getAvatar());
      assertAll(
          () -> assertFalse(initializedByUserLoad, "loading User must leave avatar lazy"),
          () -> assertEquals(assigned, loaded.getAvatar().isAssigned()),
          () -> assertEquals("filenameA", loaded.getAvatar().getName()));
      commitAndClear();
      assertEquals(
          assigned,
          entityManager
              .createNativeQuery("select isassigned from fileresource where fileresourceid = :id")
              .setParameter("id", avatarId)
              .getSingleResult());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"managed", "merge", "import"})
  void avatarMutationsSurviveCommitAndMerge(String mutation) {
    User user = persistUser('A');
    FileResource first = persistAvatar('A');
    FileResource second = persistAvatar('B');
    long userId = user.getId();
    long firstId = first.getId();
    long secondId = second.getId();
    commitAndClear();

    // null-to-null, assign, same, replace, remove, then null-to-null again.
    Long[] assignments = {null, firstId, firstId, secondId, null, null};
    for (Long avatarId : assignments) {
      User loaded = entityManager.find(User.class, userId);
      FileResource next =
          avatarId == null ? null : entityManager.find(FileResource.class, avatarId);
      switch (mutation) {
        case "managed" -> loaded.setAvatar(next);
        case "merge" -> {
          entityManager.detach(loaded);
          loaded.setAvatar(next);
          entityManager.merge(loaded);
        }
        case "import" -> {
          User input = makeUser("A");
          input.setEmail("avatar@example.org");
          input.setPassword(null);
          // The 2.41 base creates the admin user once in @BeforeAll; re-load it here because
          // commitAndClear() detaches everything from the persistence context.
          User admin = entityManager.find(User.class, getAdminUser().getId());
          input.getUserRoles().addAll(admin.getUserRoles());
          if (next != null) {
            FileResource reference = new FileResource();
            reference.setUid(next.getUid());
            input.setAvatar(reference);
          }
          var report =
              metadataImportService.importMetadata(
                  new MetadataImportParams()
                      .setImportMode(ObjectBundleMode.COMMIT)
                      .setImportStrategy(ImportStrategy.UPDATE),
                  new MetadataObjects().addObject(input));
          assertEquals(Status.OK, report.getStatus(), report.toString());
        }
        default -> throw new IllegalArgumentException(mutation);
      }
      commitAndClear();
      entityManager.getEntityManagerFactory().getCache().evictAll();

      User reloaded = entityManager.find(User.class, userId);
      if (avatarId == null) assertNull(reloaded.getAvatar());
      else assertEquals(avatarId.longValue(), reloaded.getAvatar().getId());
      assertEquals(
          Long.valueOf(firstId).equals(avatarId),
          entityManager.find(FileResource.class, firstId).isAssigned());
      assertEquals(
          Long.valueOf(secondId).equals(avatarId),
          entityManager.find(FileResource.class, secondId).isAssigned());
      commitAndClear();
    }
  }

  private User persistUser(char suffix) {
    User user = makeUser(String.valueOf(suffix));
    entityManager.persist(user);
    users.add(user.getId());
    return user;
  }

  private FileResource persistAvatar(char suffix) {
    FileResource resource = createFileResource(suffix, new byte[] {1});
    resource.setDomain(FileResourceDomain.USER_AVATAR);
    entityManager.persist(resource);
    resources.add(resource.getId());
    return resource;
  }

  private void commitAndClear() {
    TestTransaction.flagForCommit();
    TestTransaction.end();
    TestTransaction.start();
    entityManager.clear();
  }
}
