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
package org.hisp.dhis.tracker;

import com.google.gson.JsonObject;
import java.io.File;
import java.util.List;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.MaintenanceActions;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.actions.tracker.TrackerImportExportActions;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.hisp.dhis.helpers.file.JsonFileReader;
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
    JsonObject teiBody =
        new FileReaderUtils()
            .readJsonAndGenerateData(
                new File("src/test/resources/tracker/importer/teis/teiAndEnrollment.json"));

    return trackerImportExportActions
        .postAndGetJobReport(teiBody)
        .validateSuccessfulImport()
        .extractImportedEnrollments()
        .get(0);
  }

  protected String importTei() throws Exception {
    JsonObject teiBody =
        new FileReaderUtils()
            .readJsonAndGenerateData(new File("src/test/resources/tracker/importer/teis/tei.json"));

    return trackerImportExportActions
        .postAndGetJobReport(teiBody)
        .validateSuccessfulImport()
        .extractImportedTeis()
        .get(0);
  }

  protected String importTei(String orgUnit) throws Exception {
    JsonObject teiBody =
        new FileReaderUtils()
            .read(new File("src/test/resources/tracker/importer/teis/tei.json"))
            .replacePropertyValuesRecursivelyWith("orgUnit", orgUnit)
            .get(JsonObject.class);

    return trackerImportExportActions
        .postAndGetJobReport(teiBody)
        .validateSuccessfulImport()
        .extractImportedTeis()
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

  protected TrackerApiResponse importTeiWithEnrollment(String programId) throws Exception {
    JsonObject teiWithEnrollment =
        new FileReaderUtils()
            .read(new File("src/test/resources/tracker/importer/teis/teiWithEnrollments.json"))
            .replacePropertyValuesRecursivelyWith("program", programId)
            .get(JsonObject.class);

    return trackerImportExportActions
        .postAndGetJobReport(teiWithEnrollment)
        .validateSuccessfulImport();
  }

  protected TrackerApiResponse importTeiWithEnrollment(String ouId, String programId)
      throws Exception {
    JsonObject teiWithEnrollment =
        new FileReaderUtils()
            .read(new File("src/test/resources/tracker/importer/teis/teiWithEnrollments.json"))
            .replacePropertyValuesRecursivelyWith("program", programId)
            .replacePropertyValuesRecursivelyWith("orgUnit", ouId)
            .get(JsonObject.class);

    return trackerImportExportActions
        .postAndGetJobReport(teiWithEnrollment)
        .validateSuccessfulImport();
  }

  /*
   * Imports one new TEI with enrollment and event
   */
  protected TrackerApiResponse importTeisWithEnrollmentAndEvent(
      String orgUnit, String programId, String programStageId) throws Exception {
    JsonObject teiWithEnrollment =
        new FileReaderUtils()
            .read(
                new File(
                    "src/test/resources/tracker/importer/teis/teiWithEnrollmentAndEventsNested.json"))
            .replacePropertyValuesRecursivelyWith("orgUnit", orgUnit)
            .replacePropertyValuesRecursivelyWith("program", programId)
            .replacePropertyValuesRecursivelyWith("programStage", programStageId)
            .get(JsonObject.class);

    TrackerApiResponse response = trackerImportExportActions.postAndGetJobReport(teiWithEnrollment);

    response.validateSuccessfulImport();
    return response;
  }

  /*
   * Imports new tracked entities, each having an enrollment and event.
   */
  protected TrackerApiResponse importTeisWithEnrollmentAndEvent() throws Exception {
    JsonObject object =
        new JsonFileReader(
                new File(
                    "src/test/resources/tracker/importer/teis/teisWithEnrollmentsAndEvents.json"))
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

  protected TrackerApiResponse importRelationshipBetweenTeis(String teiA, String teiB) {
    JsonObject payload =
        new RelationshipDataBuilder()
            .setFromTrackedEntity(teiA)
            .setToTrackedEntity(teiB)
            .setRelationshipType("xLmPUYJX8Ks")
            .array();

    return trackerImportExportActions.postAndGetJobReport(payload).validateSuccessfulImport();
  }

  protected TrackerApiResponse importRelationshipEnrollmentToTei(String enrollment, String teiB) {
    JsonObject payload =
        new RelationshipDataBuilder()
            .setFromEntity("enrollment", enrollment)
            .setToTrackedEntity(teiB)
            .setRelationshipType("fdc6uOvgoji")
            .array();

    return trackerImportExportActions.postAndGetJobReport(payload).validateSuccessfulImport();
  }

  @AfterEach
  public void tearDown() {
    loginActions.loginAsSuperUser();
    new MaintenanceActions().removeSoftDeletedData();
  }
}
