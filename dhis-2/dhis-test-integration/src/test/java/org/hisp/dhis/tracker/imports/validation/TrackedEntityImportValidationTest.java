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
package org.hisp.dhis.tracker.imports.validation;

import static org.hisp.dhis.tracker.Assertions.assertHasErrors;
import static org.hisp.dhis.tracker.Assertions.assertHasOnlyErrors;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.hisp.dhis.tracker.imports.validation.Users.USER_1;
import static org.hisp.dhis.tracker.imports.validation.Users.USER_10;
import static org.hisp.dhis.tracker.imports.validation.Users.USER_3;
import static org.hisp.dhis.tracker.imports.validation.Users.USER_4;
import static org.hisp.dhis.tracker.imports.validation.Users.USER_5;
import static org.hisp.dhis.tracker.imports.validation.Users.USER_7;
import static org.hisp.dhis.tracker.imports.validation.Users.USER_8;
import static org.hisp.dhis.tracker.imports.validation.Users.USER_9;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.acl.TrackedEntityProgramOwnerService;
import org.hisp.dhis.tracker.imports.AtomicMode;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrackedEntityImportValidationTest extends PostgresIntegrationTestBase {
  @Autowired private TestSetup testSetup;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;

  private User importUser;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata("tracker/tracker_basic_metadata.json");

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);
  }

  @BeforeEach
  void setUpUser() {
    injectSecurityContextUser(importUser);
  }

  @Test
  void failValidationWhenTrackedEntityAttributeHasWrongOptionValue() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/te-with_invalid_option_value.json");

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasOnlyErrors(importReport, ValidationCode.E1125);
  }

  @Test
  void successValidationWhenTrackedEntityAttributeHasValidOptionValue() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/te-with_valid_option_value.json");

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(importReport);
  }

  @Test
  void failValidationWhenTrackedEntityAttributesHaveSameUniqueValues() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/te-with_unique_attributes.json");

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasErrors(importReport, 2, ValidationCode.E1064);
  }

  @Test
  void testTeValidationOkAll() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/te-data_with_different_ou.json");

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(importReport);
  }

  @Test
  void testNoCreateTeAccessOutsideCaptureScopeOu() throws IOException {
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/te-data_with_different_ou.json");
    TrackerImportParams params = new TrackerImportParams();
    injectSecurityContextUser(userService.getUser(USER_7));
    params.setAtomicMode(AtomicMode.OBJECT);
    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);
    assertHasOnlyErrors(importReport, ValidationCode.E1000);
    assertEquals(2, importReport.getStats().getCreated());
    assertEquals(1, importReport.getStats().getIgnored());
  }

  @Test
  void testUpdateAccessInSearchScopeOu() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/te-data_with_different_ou.json");

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(importReport);
    assertEquals(3, importReport.getStats().getCreated());

    trackerObjects = testSetup.fromJson("tracker/validations/te-data_with_different_ou.json");
    injectSecurityContextUser(userService.getUser(USER_8));
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);
    params.setAtomicMode(AtomicMode.OBJECT);
    importReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(importReport);
    assertEquals(3, importReport.getStats().getUpdated());
  }

  @Test
  void testNoUpdateAccessOutsideSearchScopeOu() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/te-data_with_different_ou.json");

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(importReport);
    assertEquals(3, importReport.getStats().getCreated());

    trackerObjects = testSetup.fromJson("tracker/validations/te-data_with_different_ou.json");
    injectSecurityContextUser(userService.getUser(USER_5));
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);
    params.setAtomicMode(AtomicMode.OBJECT);

    importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasOnlyErrors(importReport, ValidationCode.E1003);
    assertEquals(2, importReport.getStats().getUpdated());
    assertEquals(1, importReport.getStats().getIgnored());
  }

  @Test
  void testNoWriteAccessInAcl() throws IOException {
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/validations/te-data_ok.json");
    TrackerImportParams params = new TrackerImportParams();
    injectSecurityContextUser(userService.getUser(USER_1));

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasErrors(importReport, 13, ValidationCode.E1001);
  }

  @Test
  void testWriteAccessInAclViaUserGroup() throws IOException {
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/validations/te-data_ok.json");
    TrackerImportParams params = new TrackerImportParams();
    injectSecurityContextUser(userService.getUser(USER_3));

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(importReport);
  }

  @Test
  void testGeoOk() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/te-data_error_geo-ok.json");

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(importReport);
  }

  @Test
  void testTeAttrNonExistentAttr() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/te-data_error_attr-non-existing.json");
    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasErrors(importReport, 2, ValidationCode.E1006);
  }

  @Test
  void testDeleteCascadeEnrollments() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/enrollments_te_te-data.json");

    assertNoErrors(trackerImportService.importTracker(params, trackerObjects));

    importEnrollments();
    manager.flush();
    manager.clear();
    trackerObjects = testSetup.fromJson("tracker/validations/enrollments_te_te-data.json");
    injectSecurityContextUser(userService.getUser(USER_4));
    params.setImportStrategy(TrackerImportStrategy.DELETE);

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasErrors(importReport, 2, ValidationCode.E1100);
  }

  @Test
  void shouldFailToDeleteWhenUserHasAccessToRegistrationUnitAndTEWasTransferred()
      throws IOException, BadRequestException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/enrollments_te_te-data.json");

    assertNoErrors(trackerImportService.importTracker(params, trackerObjects));
    importEnrollments();
    manager.flush();
    manager.clear();
    TrackedEntity trackedEntity = manager.get(TrackedEntity.class, "Kj6vYde4LHh");
    Program program = manager.get(Program.class, "E8o1E9tAppy");
    OrganisationUnit orgUnit = manager.get(OrganisationUnit.class, "QfUVllTs6cS");
    trackedEntityProgramOwnerService.updateTrackedEntityProgramOwner(
        trackedEntity, program, orgUnit);
    manager.flush();
    manager.clear();

    ImportReport importReport = deleteTransferredTrackedEntity(userService.getUser(USER_10));
    assertHasErrors(importReport, 1, ValidationCode.E1003);
  }

  @Test
  void
      shouldFailToDeleteWhenTEWasTransferredAndUserHasAccessToTransferredOrgUnitAndTEOUIsNotInCaptureScope()
          throws IOException, BadRequestException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/enrollments_te_te-data.json");

    assertNoErrors(trackerImportService.importTracker(params, trackerObjects));
    importEnrollments();
    manager.flush();
    manager.clear();
    TrackedEntity trackedEntity = manager.get(TrackedEntity.class, "Kj6vYde4LHh");
    Program program = manager.get(Program.class, "E8o1E9tAppy");
    OrganisationUnit orgUnit = manager.get(OrganisationUnit.class, "QfUVllTs6cW");
    trackedEntityProgramOwnerService.updateTrackedEntityProgramOwner(
        trackedEntity, program, orgUnit);
    manager.flush();
    manager.clear();

    ImportReport importReport = deleteTransferredTrackedEntity(userService.getUser(USER_9));
    assertHasErrors(importReport, 1, ValidationCode.E1000);
  }

  @Test
  void shouldDeleteWhenTEWasTransferredAndUserHasAccessToTransferredOrgUnitAndTEOUIsInCaptureScope()
      throws IOException, BadRequestException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/enrollments_te_te-data.json");

    assertNoErrors(trackerImportService.importTracker(params, trackerObjects));
    importEnrollments();
    manager.flush();
    manager.clear();
    TrackedEntity trackedEntity = manager.get(TrackedEntity.class, "Kj6vYde4LHh");
    Program program = manager.get(Program.class, "E8o1E9tAppy");
    OrganisationUnit orgUnit = manager.get(OrganisationUnit.class, "QfUVllTs6cW");
    trackedEntityProgramOwnerService.updateTrackedEntityProgramOwner(
        trackedEntity, program, orgUnit);
    manager.flush();
    manager.clear();
    ImportReport importReport = deleteTransferredTrackedEntity(userService.getUser(USER_7));
    assertNoErrors(importReport);
  }

  @Test
  void shouldFailToUpdateWhenUserHasAccessToRegistrationUnitAndTEWasTransferred()
      throws IOException, BadRequestException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/enrollments_te_te-data.json");

    assertNoErrors(trackerImportService.importTracker(params, trackerObjects));

    trackerObjects =
        testSetup.fromJson("tracker/validations/enrollments_te_enrollments-data_2.json");
    TrackerImportParams trackerImportParams = new TrackerImportParams();
    ImportReport importReport =
        trackerImportService.importTracker(trackerImportParams, trackerObjects);
    assertNoErrors(importReport);
    manager.flush();
    manager.clear();
    TrackedEntity trackedEntity = manager.get(TrackedEntity.class, "KKKKj6vYdes");
    Program program = manager.get(Program.class, "E8o1E9tAppy");
    OrganisationUnit orgUnit = manager.get(OrganisationUnit.class, "QfUVllTs6cW");
    trackedEntityProgramOwnerService.updateTrackedEntityProgramOwner(
        trackedEntity, program, orgUnit);
    manager.flush();
    manager.clear();
    importReport = updateTransferredTrackedEntity(USER_10, UID.of("KKKKj6vYdes"));
    assertHasErrors(importReport, 1, ValidationCode.E1003);
  }

  @Test
  void shouldUpdateWhenTEWasTransferredAndUserHasAccessToTransferredOrgUnit()
      throws IOException, BadRequestException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/enrollments_te_te-data.json");

    assertNoErrors(trackerImportService.importTracker(params, trackerObjects));

    importEnrollments();
    manager.flush();
    manager.clear();
    TrackedEntity trackedEntity = manager.get(TrackedEntity.class, "Kj6vYde4LHh");
    Program program = manager.get(Program.class, "E8o1E9tAppy");
    OrganisationUnit orgUnit = manager.get(OrganisationUnit.class, "QfUVllTs6cW");
    trackedEntityProgramOwnerService.updateTrackedEntityProgramOwner(
        trackedEntity, program, orgUnit);
    manager.flush();
    manager.clear();
    ImportReport importReport = updateTransferredTrackedEntity(USER_9, UID.of("Kj6vYde4LHh"));
    assertNoErrors(importReport);
  }

  @Test
  void testTeDeleteOk() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/validations/te-data.json");

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(importReport);

    TrackerObjects deleteTrackerObjects =
        testSetup.fromJson("tracker/validations/te-data-delete.json");
    params.setImportStrategy(TrackerImportStrategy.DELETE);

    ImportReport importReportDelete =
        trackerImportService.importTracker(params, deleteTrackerObjects);

    assertNoErrors(importReportDelete);
    assertEquals(1, importReportDelete.getStats().getDeleted());
  }

  protected void importEnrollments() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/enrollments_te_enrollments-data.json");

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(importReport);
  }

  protected ImportReport deleteTransferredTrackedEntity(User user) throws IOException {
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/te-transferred-data-delete.json");
    TrackerImportParams params = new TrackerImportParams();
    params.setImportStrategy(TrackerImportStrategy.DELETE);
    injectSecurityContextUser(user);
    return trackerImportService.importTracker(params, trackerObjects);
  }

  protected ImportReport updateTransferredTrackedEntity(String userId, UID trackedEntity)
      throws IOException {
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/te-transferred-data-update.json");
    trackerObjects.getTrackedEntities().get(0).setTrackedEntity(trackedEntity);
    TrackerImportParams params = new TrackerImportParams();
    params.setImportStrategy(TrackerImportStrategy.UPDATE);
    injectSecurityContextUser(userService.getUser(userId));
    return trackerImportService.importTracker(params, trackerObjects);
  }
}
