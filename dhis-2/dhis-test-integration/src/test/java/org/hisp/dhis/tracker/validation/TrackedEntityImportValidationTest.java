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
import static org.hisp.dhis.tracker.validation.Users.USER_3;
import static org.hisp.dhis.tracker.validation.Users.USER_4;
import static org.hisp.dhis.tracker.validation.Users.USER_7;
import static org.hisp.dhis.tracker.validation.Users.USER_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.HashSet;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.tracker.AtomicMode;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class TrackedEntityImportValidationTest extends TrackerTest {
  @Autowired protected TrackedEntityInstanceService trackedEntityInstanceService;

  @Autowired private TrackerImportService trackerImportService;

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
    assertHasOnlyErrors(trackerImportReport, TrackerErrorCode.E1000);
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
    TrackerImportReport trackerImportReport = trackerImportService.importTracker(params);
    assertNoErrors(trackerImportReport);
    assertEquals(3, trackerImportReport.getStats().getCreated());
    dbmsManager.clearSession();
    params = fromJson("tracker/validations/te-data_with_different_ou.json");
    User user = userService.getUser(USER_7);
    params.setUser(user);
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);
    params.setAtomicMode(AtomicMode.OBJECT);
    trackerImportReport = trackerImportService.importTracker(params);
    assertHasOnlyErrors(trackerImportReport, TrackerErrorCode.E1003);
    assertEquals(2, trackerImportReport.getStats().getUpdated());
    assertEquals(1, trackerImportReport.getStats().getIgnored());
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
    params.setUser(user);
    user.setPassword("user4password");
    injectSecurityContext(user);
    TrackerImportReport trackerImportReport = trackerImportService.importTracker(params);
    assertNoErrors(trackerImportReport);
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
  void testTeDeleteOk() throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/te-data.json");
    TrackerImportReport trackerImportReport = trackerImportService.importTracker(params);
    assertNoErrors(trackerImportReport);

    manager.flush();
    manager.clear();

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
}
