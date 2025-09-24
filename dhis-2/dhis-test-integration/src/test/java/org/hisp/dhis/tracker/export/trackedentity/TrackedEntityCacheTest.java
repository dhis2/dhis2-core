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
package org.hisp.dhis.tracker.export.trackedentity;

import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.test.utils.Assertions;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwner;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.acl.TrackedEntityProgramOwnerStore;
import org.hisp.dhis.tracker.acl.TrackerOwnershipManager;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.sharing.Sharing;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

// Do not use the annotation @ActiveProfiles, this is an exception as for now it's the only way to
// use a cache in our integration tests
@ActiveProfiles("cache-test")
@Transactional
@TestInstance(Lifecycle.PER_CLASS)
class TrackedEntityCacheTest extends PostgresIntegrationTestBase {
  private final TestSetup testSetup;
  private final IdentifiableObjectManager manager;
  private final TrackedEntityService trackedEntityService;
  private final ProgramService programService;
  private final TrackerOwnershipManager trackerOwnershipManager;
  private final TrackerImportService trackerImportService;
  private final Cache<OrganisationUnit> ownerCache;
  private final TrackedEntityProgramOwnerStore trackedEntityProgramOwnerStore;

  private User regularUser;
  private Program programA;
  private String trackedEntityA;
  private String trackedEntityB;
  private String trackedEntityC;
  private PageParams defaultPageParams;

  @Autowired
  public TrackedEntityCacheTest(
      TestSetup testSetup,
      IdentifiableObjectManager manager,
      TrackedEntityService trackedEntityService,
      ProgramService programService,
      TrackerOwnershipManager trackerOwnershipManager,
      TrackerImportService trackerImportService,
      TrackedEntityProgramOwnerStore trackedEntityProgramOwnerStore,
      CacheProvider cacheProvider) {
    this.testSetup = testSetup;
    this.manager = manager;
    this.trackedEntityService = trackedEntityService;
    this.programService = programService;
    this.trackerOwnershipManager = trackerOwnershipManager;
    this.trackerImportService = trackerImportService;
    this.trackedEntityProgramOwnerStore = trackedEntityProgramOwnerStore;
    this.ownerCache = cacheProvider.createProgramOwnerCache();
  }

  @BeforeAll
  void setUp() throws IOException, BadRequestException, ForbiddenException, NotFoundException {
    testSetup.importMetadata();
    User importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);
    testSetup.importTrackerData();
    regularUser = userService.getUser("FIgVWzUCkpw");
    programA = manager.get(Program.class, "BFcipDERJnf");

    trackedEntityA = "dUE514NMOlo";
    trackedEntityB = "QS6w44flWAf";
    trackedEntityC = "H8732208127";
    String trackedEntityD = "mHWCacsGYYn";

    defaultPageParams = PageParams.of(1, 10, false);

    injectAdminIntoSecurityContext();
    makeProgramTheOnlyOneAccessible(programA);
    TrackedEntityOperationParams operationParams =
        createTrackedEntitiesOperationParams(trackedEntityD);
    injectSecurityContextUser(regularUser);

    List<String> trackedEntities =
        extractUIDs(trackedEntityService.findTrackedEntities(operationParams, defaultPageParams));
    Assertions.assertContainsOnly(List.of(trackedEntityD), trackedEntities);
  }

  @Test
  void shouldFindTrackedEntityWhenOwnerAlreadyInCache()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams operationParams = createProgramOperationParams();
    injectSecurityContextUser(regularUser);

    List<String> trackedEntities =
        extractUIDs(trackedEntityService.findTrackedEntities(operationParams, defaultPageParams));
    Assertions.assertContainsOnly(List.of(trackedEntityA, trackedEntityB), trackedEntities);

    trackedEntities =
        extractUIDs(trackedEntityService.findTrackedEntities(operationParams, defaultPageParams));
    Assertions.assertContainsOnly(List.of(trackedEntityA, trackedEntityB), trackedEntities);
  }

  @Test
  void shouldFindTrackedEntityWhenAnotherOwnerAlreadyInCache()
      throws ForbiddenException, BadRequestException, NotFoundException {
    User user = userService.getUser("o1HMTIzBGo7");
    injectSecurityContextUser(user);

    List<String> trackedEntities =
        extractUIDs(
            trackedEntityService.findTrackedEntities(
                createTrackedEntitiesOperationParams(trackedEntityC), defaultPageParams));

    Assertions.assertContainsOnly(List.of(trackedEntityC), trackedEntities);
  }

  @Test
  void shouldHaveAccessAfterEnrollmentPersistedAndOwnershipCreatedAndCacheInvalidated() {
    TrackedEntity trackedEntity = manager.get(TrackedEntity.class, "H8732208127");
    assertNotNull(trackedEntity);
    OrganisationUnit organisationUnit = manager.get(OrganisationUnit.class, "h4w96yEMlzO");
    assertNotNull(organisationUnit);
    User user = manager.get(User.class, "lPaILkLkgOM");

    injectSecurityContextUser(user);

    // At this point there's no owner for this te-program and the cache is empty, so the te
    // registering org unit will be used and added to the cache
    assertFalse(
        trackerOwnershipManager.hasAccess(
            UserDetails.fromUser(regularUser), trackedEntity, programA));

    Enrollment enrollment =
        createEnrollment(trackedEntity.getUid(), programA.getUid(), organisationUnit.getUid());

    // As soon as the enrollment is persisted, an owner will be present in the DB and the cache will
    // be invalidated
    assertNoErrors(
        trackerImportService.importTracker(
            TrackerImportParams.builder().importStrategy(TrackerImportStrategy.CREATE).build(),
            TrackerObjects.builder().enrollments(List.of(enrollment)).build()));

    // The cache is empty again, but this time, the owner should be found in the DB, so the user has
    // access to the te-program combination
    assertTrue(
        trackerOwnershipManager.hasAccess(
            UserDetails.fromUser(regularUser), trackedEntity, programA));
  }

  @ParameterizedTest
  @MethodSource("programOwnerConsumers")
  void shouldInvalidateCacheAfterSavingOrUpdatingProgramOwner(
      Consumer<TrackedEntityProgramOwner> programOwnerConsumer) {
    TrackedEntity trackedEntity = manager.get(TrackedEntity.class, trackedEntityA);
    OrganisationUnit orgUnit = manager.get(OrganisationUnit.class, "h4w96yEMlzO");
    assertNotNull(orgUnit);
    injectSecurityContextUser(regularUser);

    assertTrue(
        trackerOwnershipManager.hasAccess(
            UserDetails.fromUser(regularUser), trackedEntity, programA));
    assertTrue(ownerCache.get(trackedEntityA + "_" + programA.getUid()).isPresent());
    assertEquals(
        orgUnit.getUid(), ownerCache.get(trackedEntityA + "_" + programA.getUid()).get().getUid());

    OrganisationUnit ownerOrgUnit = manager.get(OrganisationUnit.class, "tSsGrtfRzjY");
    TrackedEntityProgramOwner trackedEntityProgramOwner =
        createProgramOwner(trackedEntity, programA, ownerOrgUnit);

    programOwnerConsumer.accept(trackedEntityProgramOwner);

    assertTrue(ownerCache.get(trackedEntityA + "_" + programA.getUid()).isEmpty());
  }

  private Stream<Consumer<TrackedEntityProgramOwner>> programOwnerConsumers() {
    return Stream.of(trackedEntityProgramOwnerStore::save, trackedEntityProgramOwnerStore::update);
  }

  private TrackedEntityOperationParams createProgramOperationParams() {
    return TrackedEntityOperationParams.builder()
        .program(programA)
        .trackedEntities(UID.of(trackedEntityA, trackedEntityB))
        .build();
  }

  private TrackedEntityOperationParams createTrackedEntitiesOperationParams(String trackedEntity) {
    return TrackedEntityOperationParams.builder()
        .trackedEntities(Set.of(UID.of(trackedEntity)))
        .build();
  }

  private void makeProgramTheOnlyOneAccessible(Program program) {
    programService.getAllPrograms().stream()
        .filter(p -> !p.getUid().equalsIgnoreCase(program.getUid()))
        .forEach(
            p -> {
              p.setSharing(Sharing.builder().publicAccess(AccessStringHelper.DEFAULT).build());
              manager.update(p);
            });
  }

  private List<String> extractUIDs(Page<TrackedEntity> trackedEntities) {
    return trackedEntities.getItems().stream().map(TrackedEntity::getUid).toList();
  }

  private Enrollment createEnrollment(String trackedEntity, String program, String orgUnit) {
    return Enrollment.builder()
        .enrollment(UID.generate())
        .trackedEntity(UID.of(trackedEntity))
        .program(MetadataIdentifier.ofUid(program))
        .orgUnit(MetadataIdentifier.ofUid(orgUnit))
        .enrolledAt(Instant.now())
        .occurredAt(Instant.now())
        .status(EnrollmentStatus.ACTIVE)
        .build();
  }

  private TrackedEntityProgramOwner createProgramOwner(
      TrackedEntity trackedEntity, Program program, OrganisationUnit orgUnit) {
    TrackedEntityProgramOwner trackedEntityProgramOwner =
        new TrackedEntityProgramOwner(trackedEntity, program, orgUnit);
    trackedEntityProgramOwner.setCreated(Date.from(Instant.now()));
    trackedEntityProgramOwner.setLastUpdated(Date.from(Instant.now()));

    return trackedEntityProgramOwner;
  }
}
