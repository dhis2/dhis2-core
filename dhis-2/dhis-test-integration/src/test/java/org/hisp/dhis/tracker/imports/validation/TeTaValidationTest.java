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

import static org.hisp.dhis.tracker.Assertions.assertHasOnlyErrors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.trackedentityattributevalue.TrackedEntityAttributeValueService;
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
class TeTaValidationTest extends PostgresIntegrationTestBase {
  @Autowired private TestSetup testSetup;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private FileResourceService fileResourceService;

  private User importUser;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata("tracker/validations/te-program_with_tea_fileresource_metadata.json");

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);
  }

  @Test
  void testTrackedEntityProgramAttributeFileResourceValue() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    FileResource fileResource =
        new FileResource(
            "test.pdf",
            "application/pdf",
            0,
            "d41d8cd98f00b204e9800998ecf8427e",
            FileResourceDomain.DOCUMENT);
    fileResource.setUid("Jzf6hHNP7jx");
    File file = File.createTempFile("file-resource", "test");
    fileResourceService.asyncSaveFileResource(fileResource, file);
    assertFalse(fileResource.isAssigned());
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/te-program_with_tea_fileresource_data.json");

    trackerImportService.importTracker(params, trackerObjects);

    List<TrackedEntity> trackedEntities = manager.getAll(TrackedEntity.class);
    assertEquals(1, trackedEntities.size());
    TrackedEntity trackedEntity = trackedEntities.get(0);
    List<TrackedEntityAttributeValue> attributeValues =
        trackedEntityAttributeValueService.getTrackedEntityAttributeValues(trackedEntity);
    assertEquals(1, attributeValues.size());
    fileResource = fileResourceService.getFileResource(fileResource.getUid());
    assertTrue(fileResource.isAssigned());
  }

  @Test
  void testFileAlreadyAssign() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    FileResource fileResource =
        new FileResource(
            "test.pdf",
            "application/pdf",
            0,
            "d41d8cd98f00b204e9800998ecf8427e",
            FileResourceDomain.DOCUMENT);
    fileResource.setUid("Jzf6hHNP7jx");
    File file = File.createTempFile("file-resource", "test");
    fileResourceService.asyncSaveFileResource(fileResource, file);
    assertFalse(fileResource.isAssigned());
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/te-program_with_tea_fileresource_data.json");

    trackerImportService.importTracker(params, trackerObjects);

    List<TrackedEntity> trackedEntities = manager.getAll(TrackedEntity.class);
    assertEquals(1, trackedEntities.size());
    TrackedEntity trackedEntity = trackedEntities.get(0);
    List<TrackedEntityAttributeValue> attributeValues =
        trackedEntityAttributeValueService.getTrackedEntityAttributeValues(trackedEntity);
    assertEquals(1, attributeValues.size());
    fileResource = fileResourceService.getFileResource(fileResource.getUid());
    assertTrue(fileResource.isAssigned());
    trackerObjects =
        testSetup.fromJson("tracker/validations/te-program_with_tea_fileresource_data2.json");

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasOnlyErrors(importReport, ValidationCode.E1009);
  }

  @Test
  void testNoFileRef() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/te-program_with_tea_fileresource_data.json");
    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasOnlyErrors(importReport, ValidationCode.E1084);
    List<TrackedEntity> trackedEntities = manager.getAll(TrackedEntity.class);
    assertEquals(0, trackedEntities.size());
  }

  @Test
  void testTeaMaxTextValueLength() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/te-program_with_tea_too_long_text_value.json");

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasOnlyErrors(importReport, ValidationCode.E1077);
  }

  @Test
  void testTeaInvalidFormat() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/te-program_with_tea_invalid_format_value.json");

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasOnlyErrors(importReport, ValidationCode.E1085);
  }

  @Test
  void testTeaInvalidImage() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/te-program_with_tea_invalid_image_value.json");

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasOnlyErrors(importReport, ValidationCode.E1085, ValidationCode.E1007);
  }

  @Test
  void testTeaIsNull() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/te-program_with_tea_invalid_value_isnull.json");

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasOnlyErrors(importReport, ValidationCode.E1076);
  }
}
