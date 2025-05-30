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
package org.hisp.dhis.tracker.imports.events;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import com.google.gson.JsonObject;
import org.hisp.dhis.test.e2e.Constants;
import org.hisp.dhis.test.e2e.actions.IdGenerator;
import org.hisp.dhis.test.e2e.dto.TrackerApiResponse;
import org.hisp.dhis.test.e2e.utils.DataGenerator;
import org.hisp.dhis.tracker.TrackerApiTest;
import org.hisp.dhis.tracker.imports.databuilder.EventDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class EventNotesTests extends TrackerApiTest {
  @BeforeEach
  public void beforeAll() {
    loginActions.loginAsAdmin();
  }

  @Test
  public void shouldAddAnotherNoteWhenUpdatingEvent() {
    // arrange
    String id = new IdGenerator().generateUniqueId();
    JsonObject payload = buildEventWithNote(id);

    trackerImportExportActions.postAndGetJobReport(payload).validateSuccessfulImport();

    // act
    TrackerApiResponse response = trackerImportExportActions.postAndGetJobReport(payload);

    // assert
    response.validateSuccessfulImport().validate().body("stats.updated", equalTo(1));

    trackerImportExportActions
        .getEvent(id + "?fields=notes")
        .validate()
        .statusCode(200)
        .body("notes", hasSize(2))
        .rootPath("notes.createdBy")
        .body("username", everyItem(equalTo("taadmin")))
        .body("uid", everyItem(notNullValue()))
        .body("firstName", everyItem(notNullValue()))
        .body("surname", everyItem(notNullValue()));
  }

  @Test
  public void shouldNotUpdateExistingNote() {
    // arrange
    String eventId = new IdGenerator().generateUniqueId();

    JsonObject payload = buildEventWithNote(eventId);

    trackerImportExportActions.postAndGetJobReport(payload).validateSuccessfulImport();

    payload =
        trackerImportExportActions.getEvent(eventId).getBodyAsJsonBuilder().wrapIntoArray("events");

    // act

    TrackerApiResponse response = trackerImportExportActions.postAndGetJobReport(payload);

    // assert
    response
        .validateSuccessfulImport()
        .validateWarningReport()
        .body("trackerType", everyItem(equalTo("EVENT")))
        .body("warningCode", hasItem("E1119"));
  }

  private JsonObject buildEventWithNote(String id) {
    JsonObject ob =
        new EventDataBuilder()
            .setOrgUnit(Constants.ORG_UNIT_IDS[0])
            .setId(id)
            .setProgram(Constants.EVENT_PROGRAM_ID)
            .setProgramStage(Constants.EVENT_PROGRAM_STAGE_ID)
            .addNote(DataGenerator.randomString())
            .array();
    return ob;
  }
}
