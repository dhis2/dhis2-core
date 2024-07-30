/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.trackedentity;

import static java.util.Collections.emptyList;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertNotEmpty;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.imports.TrackerIdScheme;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Zubair Asghar
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class TrackedEntityQueryLimitTest extends PostgresIntegrationTestBase {

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private TrackedEntityTypeService trackedEntityTypeService;

  @Autowired private TrackedEntityService trackedEntityService;

  @Autowired private ProgramService programService;

  @Autowired private SystemSettingManager systemSettingManager;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TrackerImportService trackerImportService;

  private OrganisationUnit orgUnitA;

  private Program program;

  private TrackedEntity trackedEntity1;

  private TrackedEntity trackedEntity2;

  private TrackedEntity trackedEntity3;

  private TrackedEntity trackedEntity4;

  private User user;

  @BeforeAll
  void setUp() {
    TrackedEntityType trackedEntityType;

    user = getAdminUser();

    orgUnitA = createOrganisationUnit("A");
    organisationUnitService.addOrganisationUnit(orgUnitA);

    user.getOrganisationUnits().add(orgUnitA);

    trackedEntityType = createTrackedEntityType('P');
    trackedEntityTypeService.addTrackedEntityType(trackedEntityType);

    program = createProgram('P');
    program.setOrganisationUnits(Set.of(orgUnitA));
    programService.addProgram(program);

    trackedEntity1 = createTrackedEntity(orgUnitA);
    trackedEntity2 = createTrackedEntity(orgUnitA);
    trackedEntity3 = createTrackedEntity(orgUnitA);
    trackedEntity4 = createTrackedEntity(orgUnitA);
    trackedEntity1.setTrackedEntityType(trackedEntityType);
    trackedEntity2.setTrackedEntityType(trackedEntityType);
    trackedEntity3.setTrackedEntityType(trackedEntityType);
    trackedEntity4.setTrackedEntityType(trackedEntityType);

    manager.save(trackedEntity1);
    manager.save(trackedEntity2);
    manager.save(trackedEntity3);
    manager.save(trackedEntity4);

    User admin = getAdminUser();
    admin.addOrganisationUnit(orgUnitA);
    manager.update(admin);
    String enrollmentUid1 = CodeGenerator.generateUid();
    String enrollmentUid2 = CodeGenerator.generateUid();
    String enrollmentUid3 = CodeGenerator.generateUid();
    String enrollmentUid4 = CodeGenerator.generateUid();
    TrackerImportParams params = TrackerImportParams.builder().userId(admin.getUid()).build();
    List<org.hisp.dhis.tracker.imports.domain.Enrollment> enrollments =
        List.of(
            createEnrollment(
                enrollmentUid1,
                trackedEntity1.getUid(),
                program.getUid(),
                orgUnitA,
                new Date(),
                new Date()),
            createEnrollment(
                enrollmentUid2,
                trackedEntity2.getUid(),
                program.getUid(),
                orgUnitA,
                new Date(),
                new Date()),
            createEnrollment(
                enrollmentUid3,
                trackedEntity3.getUid(),
                program.getUid(),
                orgUnitA,
                new Date(),
                new Date()),
            createEnrollment(
                enrollmentUid4,
                trackedEntity4.getUid(),
                program.getUid(),
                orgUnitA,
                new Date(),
                new Date()));
    TrackerObjects trackerObjects =
        new TrackerObjects(emptyList(), enrollments, emptyList(), emptyList());
    assertNoErrors(trackerImportService.importTracker(params, trackerObjects));

    userService.addUser(user);
  }

  @Test
  void testConfiguredDifferentPositiveMaxTrackedEntityLimit() {
    systemSettingManager.saveSystemSetting(SettingKey.DEPRECATED_TRACKED_ENTITY_MAX_LIMIT, 4);
    systemSettingManager.saveSystemSetting(SettingKey.TRACKED_ENTITY_MAX_LIMIT, 3);

    TrackedEntityQueryParams params = new TrackedEntityQueryParams();
    params.setProgram(program);
    params.setOrgUnits(Set.of(orgUnitA));
    params.setOrgUnitMode(OrganisationUnitSelectionMode.ALL);
    params.setUserWithAssignedUsers(null, user, null);
    params.setSkipPaging(true);

    assertThrows(
        IllegalQueryException.class,
        () -> trackedEntityService.getTrackedEntityIds(params, false, false),
        String.format(
            "Only one parameter of '%s' and '%s' must be specified. Prefer '%s' as '%s' will be removed.",
            SettingKey.TRACKED_ENTITY_MAX_LIMIT.getName(),
            SettingKey.DEPRECATED_TRACKED_ENTITY_MAX_LIMIT.getName(),
            SettingKey.TRACKED_ENTITY_MAX_LIMIT.getName(),
            SettingKey.DEPRECATED_TRACKED_ENTITY_MAX_LIMIT.getName()));
  }

  @Test
  void testConfiguredSamePositiveMaxTrackedEntityLimit() {
    systemSettingManager.saveSystemSetting(SettingKey.DEPRECATED_TRACKED_ENTITY_MAX_LIMIT, 2);
    systemSettingManager.saveSystemSetting(SettingKey.TRACKED_ENTITY_MAX_LIMIT, 2);

    TrackedEntityQueryParams params = new TrackedEntityQueryParams();
    params.setProgram(program);
    params.setOrgUnits(Set.of(orgUnitA));
    params.setOrgUnitMode(OrganisationUnitSelectionMode.ALL);
    params.setUserWithAssignedUsers(null, user, null);
    params.setSkipPaging(true);

    List<Long> trackedEntities = trackedEntityService.getTrackedEntityIds(params, false, false);

    assertNotEmpty(trackedEntities);
    assertEquals(2, trackedEntities.size());
  }

  @Test
  void testConfiguredNegativeMaxTrackedEntityLimit() {
    systemSettingManager.saveSystemSetting(SettingKey.DEPRECATED_TRACKED_ENTITY_MAX_LIMIT, -1);
    systemSettingManager.saveSystemSetting(SettingKey.TRACKED_ENTITY_MAX_LIMIT, -1);

    TrackedEntityQueryParams params = new TrackedEntityQueryParams();
    params.setProgram(program);
    params.setOrgUnits(Set.of(orgUnitA));
    params.setOrgUnitMode(OrganisationUnitSelectionMode.ALL);
    params.setUserWithAssignedUsers(null, user, null);
    params.setSkipPaging(true);

    List<Long> trackedEntities = trackedEntityService.getTrackedEntityIds(params, false, false);

    assertContainsOnly(
        List.of(
            trackedEntity1.getId(),
            trackedEntity2.getId(),
            trackedEntity3.getId(),
            trackedEntity4.getId()),
        trackedEntities);
  }

  @Test
  void testDefaultMaxTrackedEntityLimit() {
    TrackedEntityQueryParams params = new TrackedEntityQueryParams();
    params.setProgram(program);
    params.setOrgUnits(Set.of(orgUnitA));
    params.setOrgUnitMode(OrganisationUnitSelectionMode.ALL);
    params.setUserWithAssignedUsers(null, user, null);
    params.setSkipPaging(true);

    List<Long> trackedEntities = trackedEntityService.getTrackedEntityIds(params, false, false);

    assertContainsOnly(
        List.of(
            trackedEntity1.getId(),
            trackedEntity2.getId(),
            trackedEntity3.getId(),
            trackedEntity4.getId()),
        trackedEntities);
  }

  @Test
  void testDisabledMaxTrackedEntityLimit() {
    systemSettingManager.saveSystemSetting(SettingKey.DEPRECATED_TRACKED_ENTITY_MAX_LIMIT, 0);
    systemSettingManager.saveSystemSetting(SettingKey.TRACKED_ENTITY_MAX_LIMIT, 0);

    TrackedEntityQueryParams params = new TrackedEntityQueryParams();
    params.setProgram(program);
    params.setOrgUnits(Set.of(orgUnitA));
    params.setOrgUnitMode(OrganisationUnitSelectionMode.ALL);
    params.setUserWithAssignedUsers(null, user, null);
    params.setSkipPaging(true);

    List<Long> trackedEntities = trackedEntityService.getTrackedEntityIds(params, false, false);

    assertContainsOnly(
        List.of(
            trackedEntity1.getId(),
            trackedEntity2.getId(),
            trackedEntity3.getId(),
            trackedEntity4.getId()),
        trackedEntities);
  }

  @Test
  void testConfiguredNewTrackedEntityMaxLimit() {
    systemSettingManager.saveSystemSetting(SettingKey.DEPRECATED_TRACKED_ENTITY_MAX_LIMIT, -1);
    systemSettingManager.saveSystemSetting(SettingKey.TRACKED_ENTITY_MAX_LIMIT, 2);

    TrackedEntityQueryParams params = new TrackedEntityQueryParams();
    params.setProgram(program);
    params.setOrgUnits(Set.of(orgUnitA));
    params.setOrgUnitMode(OrganisationUnitSelectionMode.ALL);
    params.setUserWithAssignedUsers(null, user, null);
    params.setSkipPaging(true);

    List<Long> trackedEntities = trackedEntityService.getTrackedEntityIds(params, false, false);

    assertNotEmpty(trackedEntities);
    assertEquals(2, trackedEntities.size());
  }

  @Test
  void testConfiguredDeprecatedTrackedEntityMaxLimit() {
    systemSettingManager.saveSystemSetting(SettingKey.DEPRECATED_TRACKED_ENTITY_MAX_LIMIT, 2);
    systemSettingManager.saveSystemSetting(SettingKey.TRACKED_ENTITY_MAX_LIMIT, -1);

    TrackedEntityQueryParams params = new TrackedEntityQueryParams();
    params.setProgram(program);
    params.setOrgUnits(Set.of(orgUnitA));
    params.setOrgUnitMode(OrganisationUnitSelectionMode.ALL);
    params.setUserWithAssignedUsers(null, user, null);
    params.setSkipPaging(true);

    List<Long> trackedEntities = trackedEntityService.getTrackedEntityIds(params, false, false);

    assertNotEmpty(trackedEntities);
    assertEquals(2, trackedEntities.size());
  }

  private org.hisp.dhis.tracker.imports.domain.Enrollment createEnrollment(
      String enrollmentUid,
      String trackedEntityUid,
      String programUid,
      OrganisationUnit organisationUnit,
      Date enrolledAt,
      Date occurredAt) {

    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        new org.hisp.dhis.tracker.imports.domain.Enrollment();
    enrollment.setEnrollment(enrollmentUid);
    enrollment.setProgram(MetadataIdentifier.of(TrackerIdScheme.UID, programUid, null));
    enrollment.setOrgUnit(MetadataIdentifier.ofUid(organisationUnit));
    enrollment.setTrackedEntity(trackedEntityUid);
    enrollment.setEnrolledAt(enrolledAt.toInstant());
    enrollment.setOccurredAt(occurredAt.toInstant());

    return enrollment;
  }
}
