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
import org.hisp.dhis.feedback.ForbiddenException;
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
import org.hisp.dhis.tracker.report.ImportReport;
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

    ImportReport importReport = trackerImportService.importTracker(params);

    assertHasOnlyErrors(importReport, ValidationCode.E1125);
  }

  @Test
  void successValidationWhenTrackedEntityAttributeHasValidOptionValue() throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/te-with_valid_option_value.json");

    ImportReport importReport = trackerImportService.importTracker(params);

    assertNoErrors(importReport);
  }

  @Test
  void failValidationWhenTrackedEntityAttributesHaveSameUniqueValues() throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/te-with_unique_attributes.json");

    ImportReport importReport = trackerImportService.importTracker(params);

    assertHasErrors(importReport, 2, ValidationCode.E1064);
  }

  @Test
  void testTeValidationOkAll() throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/te-data_with_different_ou.json");

    ImportReport importReport = trackerImportService.importTracker(params);

    assertNoErrors(importReport);
  }

  @Test
  void testNoCreateTeiAccessOutsideCaptureScopeOu() throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/te-data_with_different_ou.json");
    User user = userService.getUser(USER_7);
    params.setUser(user);
    params.setAtomicMode(AtomicMode.OBJECT);
    ImportReport importReport = trackerImportService.importTracker(params);
    assertHasOnlyErrors(importReport, ValidationCode.E1000);
    assertEquals(2, importReport.getStats().getCreated());
    assertEquals(1, importReport.getStats().getIgnored());
  }

  @Test
  void testUpdateAccessInSearchScopeOu() throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/te-data_with_different_ou.json");
    ImportReport importReport = trackerImportService.importTracker(params);
    assertNoErrors(importReport);
    assertEquals(3, importReport.getStats().getCreated());
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
    importReport = trackerImportService.importTracker(params);
    assertNoErrors(importReport);
    assertEquals(3, importReport.getStats().getUpdated());
  }

  @Test
  void testNoUpdateAccessOutsideSearchScopeOu() throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/te-data_with_different_ou.json");
    ImportReport importReport = trackerImportService.importTracker(params);
    assertNoErrors(importReport);
    assertEquals(3, importReport.getStats().getCreated());

    params = fromJson("tracker/validations/te-data_with_different_ou.json");
    User user = userService.getUser(USER_5);
    params.setUserId(user.getUid());
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);
    params.setAtomicMode(AtomicMode.OBJECT);
    importReport = trackerImportService.importTracker(params);
    assertHasOnlyErrors(importReport, ValidationCode.E1003);
    assertEquals(2, importReport.getStats().getUpdated());
    assertEquals(1, importReport.getStats().getIgnored());
  }

  @Test
  void testNoWriteAccessInAcl() throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/te-data_ok.json");
    User user = userService.getUser(USER_1);
    params.setUser(user);

    ImportReport importReport = trackerImportService.importTracker(params);

    assertHasErrors(importReport, 13, ValidationCode.E1001);
  }

  @Test
  void testWriteAccessInAclViaUserGroup() throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/te-data_ok.json");
    User user = userService.getUser(USER_3);
    params.setUserId(user.getUid());

    ImportReport importReport = trackerImportService.importTracker(params);
    assertNoErrors(importReport);
  }

  @Test
  void testGeoOk() throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/te-data_error_geo-ok.json");
    ImportReport importReport = trackerImportService.importTracker(params);
    assertNoErrors(importReport);
  }

  @Test
  void testTeAttrNonExistentAttr() throws IOException {
    TrackerImportParams params =
        fromJson("tracker/validations/te-data_error_attr-non-existing.json");
    ImportReport importReport = trackerImportService.importTracker(params);

    assertHasErrors(importReport, 2, ValidationCode.E1006);
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

    ImportReport importReport = trackerImportService.importTracker(params);

    assertHasErrors(importReport, 2, ValidationCode.E1100);
  }

  @Test
  void shouldFailToDeleteWhenUserHasAccessToRegistrationUnitAndTEWasTransferred()
      throws IOException, ForbiddenException {
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
    ImportReport importReport = deleteTransferredTrackedEntity(userService.getUser(USER_10));
    assertHasErrors(importReport, 1, ValidationCode.E1003);
  }

  @Test
  void
      shouldFailToDeleteWhenTEWasTransferredAndUserHasAccessToTransferredOrgUnitAndTEOUIsNotInCaptureScope()
          throws IOException, ForbiddenException {
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
    ImportReport importReport = deleteTransferredTrackedEntity(userService.getUser(USER_9));
    assertHasErrors(importReport, 1, ValidationCode.E1000);
  }

  @Test
  void shouldDeleteWhenTEWasTransferredAndUserHasAccessToTransferredOrgUnitAndTEOUIsInCaptureScope()
      throws IOException, ForbiddenException {
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
    ImportReport importReport = deleteTransferredTrackedEntity(userService.getUser(USER_7));
    assertNoErrors(importReport);
  }

  @Test
  void shouldFailToUpdateWhenUserHasAccessToRegistrationUnitAndTEWasTransferred()
      throws IOException, ForbiddenException {
    TrackerImportParams trackerObjects =
        fromJson("tracker/validations/enrollments_te_te-data.json");
    assertNoErrors(trackerImportService.importTracker(trackerObjects));
    TrackerImportParams trackerImportParams = new TrackerImportParams();
    trackerImportParams.setUserId(USER_8);
    ImportReport importReport = trackerImportService.importTracker(trackerImportParams);
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
    assertHasErrors(importReport, 1, ValidationCode.E1003);
  }

  @Test
  void shouldUpdateWhenTEWasTransferredAndUserHasAccessToTransferredOrgUnit()
      throws IOException, ForbiddenException {
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
    ImportReport importReport = updateTransferredTrackedEntity(USER_9, "Kj6vYde4LHh");
    assertNoErrors(importReport);
  }

  @Test
  void testTeDeleteOk() throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/te-data.json");
    ImportReport importReport = trackerImportService.importTracker(params);
    assertNoErrors(importReport);

    TrackerImportParams paramsDelete = fromJson("tracker/validations/te-data-delete.json");
    paramsDelete.setImportStrategy(TrackerImportStrategy.DELETE);

    ImportReport importReportDelete = trackerImportService.importTracker(paramsDelete);
    assertNoErrors(importReportDelete);
    assertEquals(1, importReportDelete.getStats().getDeleted());
  }

  protected void importProgramInstances() throws IOException {
    TrackerImportParams params =
        fromJson("tracker/validations/enrollments_te_enrollments-data.json");
    ImportReport importReport = trackerImportService.importTracker(params);
    assertNoErrors(importReport);
  }

  protected ImportReport deleteTransferredTrackedEntity(User user) throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/te-transferred-data-delete.json");
    params.setImportStrategy(TrackerImportStrategy.DELETE);
    params.setUserId(user.getUid());
    return trackerImportService.importTracker(params);
  }

  protected ImportReport updateTransferredTrackedEntity(String userId, String trackedEntity)
      throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/te-transferred-data-update.json");
    params.getTrackedEntities().get(0).setTrackedEntity(trackedEntity);
    params.setImportStrategy(TrackerImportStrategy.UPDATE);
    params.setUserId(userId);
    return trackerImportService.importTracker(params);
  }
}
