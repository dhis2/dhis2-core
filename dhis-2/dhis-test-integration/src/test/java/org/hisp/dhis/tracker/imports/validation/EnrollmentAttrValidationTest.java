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

import java.io.IOException;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnrollmentAttrValidationTest extends PostgresIntegrationTestBase {
  @Autowired private TestSetup testSetup;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TrackerImportService trackerImportService;

  private User importUser;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata("tracker/tracker_basic_metadata_mandatory_attr.json");

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    TrackerImportParams params = TrackerImportParams.builder().build();
    assertNoErrors(
        trackerImportService.importTracker(
            params, testSetup.fromJson("tracker/validations/enrollments_te_te-data_2.json")));
    manager.flush();
  }

  @Test
  void failValidationWhenTrackedEntityAttributeHasWrongOptionValue() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport =
        trackerImportService.importTracker(
            params,
            testSetup.fromJson(
                "tracker/validations/enrollments_te_with_invalid_option_value.json"));

    assertHasOnlyErrors(importReport, ValidationCode.E1125);
  }

  @Test
  void successValidationWhenTrackedEntityAttributeHasValidOptionValue() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport =
        trackerImportService.importTracker(
            params,
            testSetup.fromJson("tracker/validations/enrollments_te_with_valid_option_value.json"));

    assertNoErrors(importReport);
  }

  @Test
  void testAttributesMissingUid() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport =
        trackerImportService.importTracker(
            params,
            testSetup.fromJson("tracker/validations/enrollments_te_attr-missing-uuid.json"));

    assertHasOnlyErrors(importReport, ValidationCode.E1075);
  }

  @Test
  void testAttributesMissingValues() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport =
        trackerImportService.importTracker(
            params,
            testSetup.fromJson("tracker/validations/enrollments_te_attr-missing-value.json"));

    assertHasOnlyErrors(importReport, ValidationCode.E1076);
  }

  @Test
  void testAttributesMissingTeA() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport =
        trackerImportService.importTracker(
            params,
            testSetup.fromJson("tracker/validations/enrollments_te_attr-non-existing.json"));

    assertHasOnlyErrors(importReport, ValidationCode.E1006);
  }

  @Test
  void testAttributesMissingMandatory() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport =
        trackerImportService.importTracker(
            params,
            testSetup.fromJson("tracker/validations/enrollments_te_attr-missing-mandatory.json"));

    assertHasOnlyErrors(importReport, ValidationCode.E1018);
  }

  @Test
  void testAttributesUniquenessInSameTrackedEntity() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport =
        trackerImportService.importTracker(
            params,
            testSetup.fromJson("tracker/validations/enrollments_te_unique_attr_same_te.json"));

    assertNoErrors(importReport);
  }

  @Test
  void testAttributesUniquenessAlreadyInDB() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport =
        trackerImportService.importTracker(
            params, testSetup.fromJson("tracker/validations/enrollments_te_te-data_3.json"));

    assertNoErrors(importReport);

    manager.flush();
    manager.clear();

    importReport =
        trackerImportService.importTracker(
            params,
            testSetup.fromJson("tracker/validations/enrollments_te_unique_attr_same_te.json"));

    assertNoErrors(importReport);

    manager.flush();
    manager.clear();

    importReport =
        trackerImportService.importTracker(
            params,
            testSetup.fromJson("tracker/validations/enrollments_te_unique_attr_in_db.json"));

    assertHasOnlyErrors(importReport, ValidationCode.E1064);
  }

  @Test
  void testAttributesUniquenessInDifferentTrackedEntities() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();

    assertNoErrors(
        trackerImportService.importTracker(
            params, testSetup.fromJson("tracker/validations/enrollments_te_te-data_3.json")));
    manager.flush();
    manager.clear();

    ImportReport importReport =
        trackerImportService.importTracker(
            params, testSetup.fromJson("tracker/validations/enrollments_te_unique_attr.json"));

    assertHasErrors(importReport, 2, ValidationCode.E1064);
  }

  @Test
  void testAttributesOnlyProgramAttrAllowed() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport =
        trackerImportService.importTracker(
            params,
            testSetup.fromJson("tracker/validations/enrollments_te_attr-only-program-attr.json"));

    assertHasOnlyErrors(importReport, ValidationCode.E1019);
  }
}
