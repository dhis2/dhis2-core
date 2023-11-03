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
package org.hisp.dhis.tracker.imports.validation;

import static org.hisp.dhis.tracker.Assertions.assertHasErrors;
import static org.hisp.dhis.tracker.Assertions.assertHasOnlyErrors;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.hisp.dhis.tracker.imports.validation.Users.USER_1;
import static org.hisp.dhis.tracker.imports.validation.Users.USER_3;
import static org.hisp.dhis.tracker.imports.validation.Users.USER_4;
import static org.hisp.dhis.tracker.imports.validation.Users.USER_7;
import static org.hisp.dhis.tracker.imports.validation.Users.USER_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.HashSet;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.imports.AtomicMode;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class TrackedEntityImportValidationTest extends TrackerTest {
  @Autowired protected TrackedEntityService trackedEntityService;

  @Autowired private TrackerImportService trackerImportService;

  @Override
  protected void initTest() throws IOException {
    setUpMetadata("tracker/tracker_basic_metadata.json");
    injectAdminUser();
  }

  @Test
  void failValidationWhenTrackedEntityAttributeHasWrongOptionValue() throws IOException {
    TrackerObjects trackerObjects =
        fromJson("tracker/validations/te-with_invalid_option_value.json");

    ImportReport importReport =
        trackerImportService.importTracker(new TrackerImportParams(), trackerObjects);

    assertHasOnlyErrors(importReport, ValidationCode.E1125);
  }

  @Test
  void successValidationWhenTrackedEntityAttributeHasValidOptionValue() throws IOException {
    TrackerObjects trackerObjects = fromJson("tracker/validations/te-with_valid_option_value.json");

    ImportReport importReport =
        trackerImportService.importTracker(new TrackerImportParams(), trackerObjects);

    assertNoErrors(importReport);
  }

  @Test
  void failValidationWhenTrackedEntityAttributesHaveSameUniqueValues() throws IOException {
    TrackerObjects trackerObjects = fromJson("tracker/validations/te-with_unique_attributes.json");

    ImportReport importReport =
        trackerImportService.importTracker(new TrackerImportParams(), trackerObjects);

    assertHasErrors(importReport, 2, ValidationCode.E1064);
  }

  @Test
  void testTeValidationOkAll() throws IOException {
    TrackerObjects trackerObjects = fromJson("tracker/validations/te-data_with_different_ou.json");

    ImportReport importReport =
        trackerImportService.importTracker(new TrackerImportParams(), trackerObjects);

    assertNoErrors(importReport);
  }

  @Test
  void testNoCreateTeiAccessOutsideCaptureScopeOu() throws IOException {
    TrackerObjects trackerObjects = fromJson("tracker/validations/te-data_with_different_ou.json");
    TrackerImportParams params = new TrackerImportParams();
    User user = userService.getUser(USER_7);
    params.setUserId(user.getUid());
    params.setAtomicMode(AtomicMode.OBJECT);
    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);
    assertHasOnlyErrors(importReport, ValidationCode.E1000);
    assertEquals(2, importReport.getStats().getCreated());
    assertEquals(1, importReport.getStats().getIgnored());
  }

  @Test
  void testUpdateAccessInSearchScopeOu() throws IOException {
    TrackerObjects trackerObjects = fromJson("tracker/validations/te-data_with_different_ou.json");
    TrackerImportParams params = new TrackerImportParams();
    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(importReport);
    assertEquals(3, importReport.getStats().getCreated());
    // For some reason teiSearchOrgunits is not created properly from
    // metadata
    // Redoing the update here for the time being.
    User user = userService.getUser(USER_8);
    user.setTeiSearchOrganisationUnits(new HashSet<>(user.getDataViewOrganisationUnits()));
    userService.updateUser(user);
    dbmsManager.clearSession();
    trackerObjects = fromJson("tracker/validations/te-data_with_different_ou.json");
    user = userService.getUser(USER_8);
    params.setUserId(user.getUid());
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);
    params.setAtomicMode(AtomicMode.OBJECT);
    importReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(importReport);
    assertEquals(3, importReport.getStats().getUpdated());
  }

  @Test
  void testNoUpdateAccessOutsideSearchScopeOu() throws IOException {
    TrackerObjects trackerObjects = fromJson("tracker/validations/te-data_with_different_ou.json");
    TrackerImportParams params = new TrackerImportParams();
    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(importReport);
    assertEquals(3, importReport.getStats().getCreated());
    dbmsManager.clearSession();
    trackerObjects = fromJson("tracker/validations/te-data_with_different_ou.json");
    User user = userService.getUser(USER_7);
    params.setUserId(user.getUid());
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);
    params.setAtomicMode(AtomicMode.OBJECT);
    importReport = trackerImportService.importTracker(params, trackerObjects);
    assertHasOnlyErrors(importReport, ValidationCode.E1003);
    assertEquals(2, importReport.getStats().getUpdated());
    assertEquals(1, importReport.getStats().getIgnored());
  }

  @Test
  void testNoWriteAccessInAcl() throws IOException {
    TrackerObjects trackerObjects = fromJson("tracker/validations/te-data_ok.json");
    TrackerImportParams params = new TrackerImportParams();
    User user = userService.getUser(USER_1);
    params.setUserId(user.getUid());

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasErrors(importReport, 13, ValidationCode.E1001);
  }

  @Test
  void testWriteAccessInAclViaUserGroup() throws IOException {
    TrackerObjects trackerObjects = fromJson("tracker/validations/te-data_ok.json");
    TrackerImportParams params = new TrackerImportParams();
    User user = userService.getUser(USER_3);
    params.setUserId(user.getUid());
    params.setUserId(user.getUid());
    user.setPassword("user4password");
    injectSecurityContext(user);
    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(importReport);
  }

  @Test
  void testGeoOk() throws IOException {
    TrackerObjects trackerObjects = fromJson("tracker/validations/te-data_error_geo-ok.json");
    TrackerImportParams params = new TrackerImportParams();
    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(importReport);
  }

  @Test
  void testTeAttrNonExistentAttr() throws IOException {
    TrackerObjects trackerObjects =
        fromJson("tracker/validations/te-data_error_attr-non-existing.json");
    TrackerImportParams params = new TrackerImportParams();
    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasErrors(importReport, 2, ValidationCode.E1006);
  }

  @Test
  void testDeleteCascadeEnrollments() throws IOException {
    TrackerObjects trackerObjects = fromJson("tracker/validations/enrollments_te_te-data.json");
    TrackerImportParams params = new TrackerImportParams();
    assertNoErrors(trackerImportService.importTracker(params, trackerObjects));
    importEnrollments();
    manager.flush();
    manager.clear();
    trackerObjects = fromJson("tracker/validations/enrollments_te_te-data.json");
    User user2 = userService.getUser(USER_4);
    params.setUserId(user2.getUid());
    params.setImportStrategy(TrackerImportStrategy.DELETE);

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasErrors(importReport, 2, ValidationCode.E1100);
  }

  @Test
  void testTeDeleteOk() throws IOException {
    TrackerObjects trackerObjects = fromJson("tracker/validations/te-data.json");
    TrackerImportParams params = new TrackerImportParams();
    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(importReport);

    manager.flush();
    manager.clear();

    TrackerObjects deleteTrackerObjects = fromJson("tracker/validations/te-data-delete.json");
    params.setImportStrategy(TrackerImportStrategy.DELETE);

    ImportReport importReportDelete =
        trackerImportService.importTracker(params, deleteTrackerObjects);
    assertNoErrors(importReportDelete);
    assertEquals(1, importReportDelete.getStats().getDeleted());
  }

  protected void importEnrollments() throws IOException {
    TrackerObjects trackerObjects =
        fromJson("tracker/validations/enrollments_te_enrollments-data.json");
    ImportReport importReport =
        trackerImportService.importTracker(new TrackerImportParams(), trackerObjects);
    assertNoErrors(importReport);
  }
}
