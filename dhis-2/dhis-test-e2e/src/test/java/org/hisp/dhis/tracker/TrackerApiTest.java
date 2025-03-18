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
package org.hisp.dhis.tracker;

import com.google.gson.JsonObject;
import java.io.File;
import java.util.List;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.hisp.dhis.helpers.file.JsonFileReader;
import org.hisp.dhis.test.e2e.Constants;
import org.hisp.dhis.test.e2e.actions.LoginActions;
import org.hisp.dhis.test.e2e.actions.MaintenanceActions;
import org.hisp.dhis.test.e2e.actions.metadata.ProgramActions;
import org.hisp.dhis.test.e2e.actions.tracker.TrackerImportExportActions;
import org.hisp.dhis.test.e2e.dto.TrackerApiResponse;
import org.hisp.dhis.tracker.imports.databuilder.RelationshipDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
@Tag("category:tracker_nti")
public class TrackerApiTest extends ApiTest {
  protected static final String TRACKER_PROGRAM_STAGE_ID = "nlXNK4b7LVr";

  protected String TRACKER_PROGRAM_ID = Constants.TRACKER_PROGRAM_ID;

  protected LoginActions loginActions;

  protected ProgramActions programActions;

  protected TrackerImportExportActions trackerImportExportActions;

  @BeforeAll
  public void beforeTracker() {
    loginActions = new LoginActions();
    programActions = new ProgramActions();
    trackerImportExportActions = new TrackerImportExportActions();
  }

  protected String importEnrollment() throws Exception {
    JsonObject teBody =
        new FileReaderUtils()
            .readJsonAndGenerateData(
                new File(
                    "src/test/resources/tracker/importer/trackedEntities/trackedEntityWithEnrollments.json"));

    return trackerImportExportActions
        .postAndGetJobReport(teBody)
        .validateSuccessfulImport()
        .extractImportedEnrollments()
        .get(0);
  }

  protected String importTrackedEntity() throws Exception {
    JsonObject teBody =
        new FileReaderUtils()
            .readJsonAndGenerateData(
                new File("src/test/resources/tracker/importer/trackedEntities/trackedEntity.json"));

    return trackerImportExportActions
        .postAndGetJobReport(teBody)
        .validateSuccessfulImport()
        .extractImportedTrackedEntities()
        .get(0);
  }

  protected List<String> importEvents() throws Exception {
    JsonObject object =
        new FileReaderUtils()
            .read(new File("src/test/resources/tracker/importer/events/events.json"))
            .replacePropertyValuesWithIds("event")
            .get(JsonObject.class);

    return trackerImportExportActions
        .postAndGetJobReport(object)
        .validateSuccessfulImport()
        .extractImportedEvents();
  }

  protected TrackerApiResponse importTrackedEntityWithEnrollment(String programId)
      throws Exception {
    JsonObject teWithEnrollment =
        new FileReaderUtils()
            .read(
                new File(
                    "src/test/resources/tracker/importer/trackedEntities/trackedEntityWithEnrollments.json"))
            .replacePropertyValuesRecursivelyWith("program", programId)
            .get(JsonObject.class);

    return trackerImportExportActions
        .postAndGetJobReport(teWithEnrollment)
        .validateSuccessfulImport();
  }

  /*
   * Imports one new TE with enrollment and event
   */
  protected TrackerApiResponse importTrackedEntitiesWithEnrollmentAndEvent(
      String orgUnit, String programId, String programStageId) throws Exception {
    JsonObject teWithEnrollment =
        new FileReaderUtils()
            .read(
                new File(
                    "src/test/resources/tracker/importer/trackedEntities/trackedEntityWithEnrollmentAndEventsNested.json"))
            .replacePropertyValuesRecursivelyWith("orgUnit", orgUnit)
            .replacePropertyValuesRecursivelyWith("program", programId)
            .replacePropertyValuesRecursivelyWith("programStage", programStageId)
            .get(JsonObject.class);

    TrackerApiResponse response = trackerImportExportActions.postAndGetJobReport(teWithEnrollment);

    response.validateSuccessfulImport();
    return response;
  }

  /*
   * Imports new tracked entities, each having an enrollment and event.
   */
  protected TrackerApiResponse importTrackedEntitiesWithEnrollmentAndEvent() throws Exception {
    JsonObject object =
        new JsonFileReader(
                new File(
                    "src/test/resources/tracker/importer/trackedEntities/trackedEntitiesWithEnrollmentsAndEvents.json"))
            .replaceStringsWithIds(
                "Kj6vYde4LHh",
                "Nav6inZRw1u",
                "MNWZ6hnuhSw",
                "PuBvJxDB73z",
                "olfXZzSGacW",
                "ZwwuwNp6gVd")
            .get(JsonObject.class);

    return trackerImportExportActions.postAndGetJobReport(object).validateSuccessfulImport();
  }

  protected TrackerApiResponse importRelationshipBetweenTrackedEntities(String teA, String teB) {
    JsonObject payload =
        new RelationshipDataBuilder()
            .setFromTrackedEntity(teA)
            .setToTrackedEntity(teB)
            .setRelationshipType("xLmPUYJX8Ks")
            .array();

    return trackerImportExportActions.postAndGetJobReport(payload).validateSuccessfulImport();
  }

  protected TrackerApiResponse importRelationshipEnrollmentToTrackedEntity(
      String enrollment, String te) {
    JsonObject payload =
        new RelationshipDataBuilder()
            .setFromEntity("enrollment", enrollment)
            .setToTrackedEntity(te)
            .setRelationshipType("fdc6uOvgoji")
            .array();

    return trackerImportExportActions.postAndGetJobReport(payload).validateSuccessfulImport();
  }

  protected TrackerApiResponse importRelationshipEventToTrackedEntity(String event, String te) {
    JsonObject payload =
        new RelationshipDataBuilder()
            .setFromEntity("event", event)
            .setToTrackedEntity(te)
            .setRelationshipType("gdc6uOvgoji")
            .array();

    return trackerImportExportActions.postAndGetJobReport(payload).validateSuccessfulImport();
  }

  @AfterEach
  public void tearDown() {
    loginActions.loginAsSuperUser();
    new MaintenanceActions().removeSoftDeletedData();
  }
}
