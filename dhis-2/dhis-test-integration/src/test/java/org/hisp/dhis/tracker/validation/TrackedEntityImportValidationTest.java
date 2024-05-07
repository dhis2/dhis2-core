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
package org.hisp.dhis.tracker.validation;

import static org.hisp.dhis.tracker.Assertions.assertHasErrors;
import static org.hisp.dhis.tracker.Assertions.assertHasOnlyErrors;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1000;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1003;
import static org.hisp.dhis.tracker.validation.Users.USER_1;
import static org.hisp.dhis.tracker.validation.Users.USER_10;
import static org.hisp.dhis.tracker.validation.Users.USER_3;
import static org.hisp.dhis.tracker.validation.Users.USER_4;
import static org.hisp.dhis.tracker.validation.Users.USER_5;
import static org.hisp.dhis.tracker.validation.Users.USER_7;
import static org.hisp.dhis.tracker.validation.Users.USER_8;
import static org.hisp.dhis.tracker.validation.Users.USER_9;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.HashSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.tracker.AtomicMode;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class TrackedEntityImportValidationTest extends TrackerTest {
  @Autowired protected TrackedEntityInstanceService trackedEntityInstanceService;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private TrackerOwnershipManager trackerOwnershipManager;

  @Autowired protected UserService _userService;

  @Override
  protected void initTest() throws IOException {
    setUpMetadata("tracker/tracker_basic_metadata.json");
    injectAdminUser();
  }

  @Test
  void failValidationWhenTrackedEntityAttributeHasWrongOptionValue() throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/te-with_invalid_option_value.json");

    TrackerImportReport trackerImportReport = trackerImportService.importTracker(params);

    assertHasOnlyErrors(trackerImportReport, TrackerErrorCode.E1125);
  }

  @Test
  void successValidationWhenTrackedEntityAttributeHasValidOptionValue() throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/te-with_valid_option_value.json");

    TrackerImportReport trackerImportReport = trackerImportService.importTracker(params);

    assertNoErrors(trackerImportReport);
  }

  @Test
  void failValidationWhenTrackedEntityAttributesHaveSameUniqueValues() throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/te-with_unique_attributes.json");

    TrackerImportReport trackerImportReport = trackerImportService.importTracker(params);

    assertHasErrors(trackerImportReport, 2, TrackerErrorCode.E1064);
  }

  @Test
  void testTeValidationOkAll() throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/te-data_with_different_ou.json");

    TrackerImportReport trackerImportReport = trackerImportService.importTracker(params);

    assertNoErrors(trackerImportReport);
  }

  @Test
  void testNoCreateTeiAccessOutsideCaptureScopeOu() throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/te-data_with_different_ou.json");
    User user = userService.getUser(USER_7);
    params.setUser(user);
    params.setAtomicMode(AtomicMode.OBJECT);
    TrackerImportReport trackerImportReport = trackerImportService.importTracker(params);
    assertHasOnlyErrors(trackerImportReport, E1000);
    assertEquals(2, trackerImportReport.getStats().getCreated());
    assertEquals(1, trackerImportReport.getStats().getIgnored());
  }

  @Test
  void testUpdateAccessInSearchScopeOu() throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/te-data_with_different_ou.json");
    TrackerImportReport trackerImportReport = trackerImportService.importTracker(params);
    assertNoErrors(trackerImportReport);
    assertEquals(3, trackerImportReport.getStats().getCreated());
    // For some reason teiSearchOrgunits is not created properly from
    // metadata
    // Redoing the update here for the time being.
    User user = userService.getUser(USER_8);
    user.setTeiSearchOrganisationUnits(new HashSet<>(user.getDataViewOrganisationUnits()));
    userService.updateUser(user);
    dbmsManager.clearSession();
    params = fromJson("tracker/validations/te-data_with_different_ou.json");
    user = userService.getUser(USER_8);
    params.setUser(user);
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);
    params.setAtomicMode(AtomicMode.OBJECT);
    trackerImportReport = trackerImportService.importTracker(params);
    assertNoErrors(trackerImportReport);
    assertEquals(3, trackerImportReport.getStats().getUpdated());
  }

  @Test
  void testNoUpdateAccessOutsideSearchScopeOu() throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/te-data_with_different_ou.json");
    TrackerImportReport importReport = trackerImportService.importTracker(params);
    assertNoErrors(importReport);
    assertEquals(3, importReport.getStats().getCreated());

    params = fromJson("tracker/validations/te-data_with_different_ou.json");
    User user = userService.getUser(USER_5);
    params.setUserId(user.getUid());
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);
    params.setAtomicMode(AtomicMode.OBJECT);
    importReport = trackerImportService.importTracker(params);
    assertHasOnlyErrors(importReport, E1003);
    assertEquals(2, importReport.getStats().getUpdated());
    assertEquals(1, importReport.getStats().getIgnored());
  }

  @Test
  void testNoWriteAccessInAcl() throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/te-data_ok.json");
    User user = userService.getUser(USER_1);
    params.setUser(user);

    TrackerImportReport trackerImportReport = trackerImportService.importTracker(params);

    assertHasErrors(trackerImportReport, 13, TrackerErrorCode.E1001);
  }

  @Test
  void testWriteAccessInAclViaUserGroup() throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/te-data_ok.json");
    User user = userService.getUser(USER_3);
    params.setUserId(user.getUid());

    TrackerImportReport importReport = trackerImportService.importTracker(params);
    assertNoErrors(importReport);
  }

  @Test
  void testGeoOk() throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/te-data_error_geo-ok.json");
    TrackerImportReport trackerImportReport = trackerImportService.importTracker(params);
    assertNoErrors(trackerImportReport);
  }

  @Test
  void testTeAttrNonExistentAttr() throws IOException {
    TrackerImportParams params =
        fromJson("tracker/validations/te-data_error_attr-non-existing.json");
    TrackerImportReport trackerImportReport = trackerImportService.importTracker(params);

    assertHasErrors(trackerImportReport, 2, TrackerErrorCode.E1006);
  }

  @Test
  void testDeleteCascadeProgramInstances() throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/enrollments_te_te-data.json");
    assertNoErrors(trackerImportService.importTracker(params));
    importProgramInstances();
    manager.flush();
    manager.clear();
    params = fromJson("tracker/validations/enrollments_te_te-data.json");
    User user2 = userService.getUser(USER_4);
    params.setUser(user2);
    params.setImportStrategy(TrackerImportStrategy.DELETE);

    TrackerImportReport trackerImportReport = trackerImportService.importTracker(params);

    assertHasErrors(trackerImportReport, 2, TrackerErrorCode.E1100);
  }

  @Test
  void shouldFailToDeleteWhenUserHasAccessToRegistrationUnitAndTEWasTransferred()
      throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/enrollments_te_te-data.json");
    assertNoErrors(trackerImportService.importTracker(params));
    importProgramInstances();
    manager.flush();
    manager.clear();
    TrackedEntityInstance te = manager.get(TrackedEntityInstance.class, "Kj6vYde4LHh");
    OrganisationUnit orgUnit = manager.get(OrganisationUnit.class, "QfUVllTs6cW");
    Program program = manager.get(Program.class, "E8o1E9tAppy");
    trackerOwnershipManager.transferOwnership(te, program, orgUnit, true, false);
    manager.flush();
    manager.clear();
    TrackerImportReport importReport = deleteTransferredTrackedEntity(userService.getUser(USER_10));
    assertHasErrors(importReport, 1, E1003);
  }

  @Test
  void
      shouldFailToDeleteWhenTEWasTransferredAndUserHasAccessToTransferredOrgUnitAndTEOUIsNotInCaptureScope()
          throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/enrollments_te_te-data.json");
    assertNoErrors(trackerImportService.importTracker(params));
    importProgramInstances();
    manager.flush();
    manager.clear();
    TrackedEntityInstance te = manager.get(TrackedEntityInstance.class, "Kj6vYde4LHh");
    OrganisationUnit orgUnit = manager.get(OrganisationUnit.class, "QfUVllTs6cW");
    Program program = manager.get(Program.class, "E8o1E9tAppy");
    trackerOwnershipManager.transferOwnership(te, program, orgUnit, true, false);
    manager.flush();
    manager.clear();
    TrackerImportReport importReport = deleteTransferredTrackedEntity(userService.getUser(USER_9));
    assertHasErrors(importReport, 1, E1000);
  }

  @Test
  void shouldDeleteWhenTEWasTransferredAndUserHasAccessToTransferredOrgUnitAndTEOUIsInCaptureScope()
      throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/enrollments_te_te-data.json");
    assertNoErrors(trackerImportService.importTracker(params));
    importProgramInstances();
    manager.flush();
    manager.clear();
    TrackedEntityInstance te = manager.get(TrackedEntityInstance.class, "Kj6vYde4LHh");
    OrganisationUnit orgUnit = manager.get(OrganisationUnit.class, "QfUVllTs6cW");
    Program program = manager.get(Program.class, "E8o1E9tAppy");
    trackerOwnershipManager.transferOwnership(te, program, orgUnit, true, false);
    manager.flush();
    manager.clear();
    TrackerImportReport importReport = deleteTransferredTrackedEntity(userService.getUser(USER_7));
    assertNoErrors(importReport);
  }

  @Test
  void shouldFailToUpdateWhenUserHasAccessToRegistrationUnitAndTEWasTransferred()
      throws IOException {
    TrackerImportParams trackerObjects =
        fromJson("tracker/validations/enrollments_te_te-data.json");
    assertNoErrors(trackerImportService.importTracker(trackerObjects));
    trackerObjects = fromJson("tracker/validations/enrollments_te_enrollments-data_2.json");
    TrackerImportParams trackerImportParams = new TrackerImportParams();
    trackerImportParams.setUserId(USER_10);
    TrackerImportReport importReport = trackerImportService.importTracker(trackerImportParams);
    assertNoErrors(importReport);
    manager.flush();
    manager.clear();
    TrackedEntityInstance te = manager.get(TrackedEntityInstance.class, "KKKKj6vYdes");
    OrganisationUnit orgUnit = manager.get(OrganisationUnit.class, "QfUVllTs6cW");
    Program program = manager.get(Program.class, "E8o1E9tAppy");
    trackerOwnershipManager.transferOwnership(te, program, orgUnit, true, false);
    manager.flush();
    manager.clear();
    importReport = updateTransferredTrackedEntity(USER_10, "KKKKj6vYdes");
    assertHasErrors(importReport, 1, E1003);
  }

  @Test
  void shouldUpdateWhenTEWasTransferredAndUserHasAccessToTransferredOrgUnit() throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/enrollments_te_te-data.json");
    assertNoErrors(trackerImportService.importTracker(params));
    importProgramInstances();
    manager.flush();
    manager.clear();
    TrackedEntityInstance te = manager.get(TrackedEntityInstance.class, "Kj6vYde4LHh");
    OrganisationUnit orgUnit = manager.get(OrganisationUnit.class, "QfUVllTs6cW");
    Program program = manager.get(Program.class, "E8o1E9tAppy");
    trackerOwnershipManager.transferOwnership(te, program, orgUnit, true, false);
    manager.flush();
    manager.clear();
    TrackerImportReport importReport = updateTransferredTrackedEntity(USER_9, "Kj6vYde4LHh");
    assertNoErrors(importReport);
  }

  @Test
  void testTeDeleteOk() throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/te-data.json");
    TrackerImportReport trackerImportReport = trackerImportService.importTracker(params);
    assertNoErrors(trackerImportReport);

    TrackerImportParams paramsDelete = fromJson("tracker/validations/te-data-delete.json");
    paramsDelete.setImportStrategy(TrackerImportStrategy.DELETE);

    TrackerImportReport trackerImportReportDelete =
        trackerImportService.importTracker(paramsDelete);
    assertNoErrors(trackerImportReportDelete);
    assertEquals(1, trackerImportReportDelete.getStats().getDeleted());
  }

  protected void importProgramInstances() throws IOException {
    TrackerImportParams params =
        fromJson("tracker/validations/enrollments_te_enrollments-data.json");
    TrackerImportReport trackerImportReport = trackerImportService.importTracker(params);
    assertNoErrors(trackerImportReport);
  }

  protected TrackerImportReport deleteTransferredTrackedEntity(User user) throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/te-transferred-data-delete.json");
    params.setImportStrategy(TrackerImportStrategy.DELETE);
    params.setUserId(user.getUid());
    return trackerImportService.importTracker(params);
  }

  protected TrackerImportReport updateTransferredTrackedEntity(String userId, String trackedEntity)
      throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/te-transferred-data-update.json");
    params.getTrackedEntities().get(0).setTrackedEntity(trackedEntity);
    params.setImportStrategy(TrackerImportStrategy.UPDATE);
    params.setUserId(userId);
    return trackerImportService.importTracker(params);
  }
}
