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
package org.hisp.dhis.tracker.imports.events;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonObject;
import java.io.File;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.metadata.MetadataActions;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.tracker.TrackerApiTest;
import org.hisp.dhis.tracker.imports.databuilder.EventDataBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class UserAssignmentTests extends TrackerApiTest {
  private static final String programStageId = "l8oDIfJJhtg";

  private static final String programId = "BJ42SUrAvHo";

  private ProgramActions programActions;

  private MetadataActions metadataActions;

  @BeforeAll
  public void beforeAll() {
    programActions = new ProgramActions();
    metadataActions = new MetadataActions();

    loginActions.loginAsSuperUser();
    metadataActions.importAndValidateMetadata(
        new File("src/test/resources/tracker/eventProgram.json"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"true", "false"})
  public void shouldImportEventWithUserAssignment(String userAssignmentEnabled) throws Exception {
    // arrange
    String loggedInUser = loginActions.getLoggedInUserId();

    programActions.programStageActions.enableUserAssignment(
        programStageId, Boolean.parseBoolean(userAssignmentEnabled));

    // act
    String eventId =
        createEvents(programId, programStageId, loggedInUser).extractImportedEvents().get(0);

    ApiResponse response = trackerImportExportActions.getEvent(eventId);
    if (!Boolean.parseBoolean(userAssignmentEnabled)) {
      response.validate().body("assignedUser.username", nullValue());

      return;
    }

    response.validate().body("assignedUser.uid", equalTo(loggedInUser));
  }

  @Test
  public void shouldRemoveUserAssignment() throws Exception {
    // arrange
    String loggedInUser = loginActions.getLoggedInUserId();

    programActions.programStageActions.enableUserAssignment(programStageId, true);
    createEvents(programId, programStageId, loggedInUser);

    JsonObject eventBody =
        trackerImportExportActions
            .get("/events?program=" + programId + "&assignedUserMode=CURRENT&ouMode=ACCESSIBLE")
            .validateStatus(200)
            .extractJsonObject("instances[0]");

    assertNotNull(eventBody, "no events matching the query.");
    String eventId = eventBody.get("event").getAsString();

    // act
    eventBody.add("assignedUser", null);

    trackerImportExportActions
        .postAndGetJobReport(
            new JsonObjectBuilder(eventBody).wrapIntoArray("events"),
            new QueryParamsBuilder().addAll("importStrategy=UPDATE"))
        .validateSuccessfulImport();

    // assert

    trackerImportExportActions
        .getEvent(eventId)
        .validate()
        .body("assignedUser.username", nullValue());
  }

  private TrackerApiResponse createEvents(
      String programId, String programStageId, String assignedUserId) {
    JsonObject jsonObject =
        new EventDataBuilder()
            .setAssignedUser(assignedUserId)
            .array(Constants.ORG_UNIT_IDS[0], programId, programStageId);

    TrackerApiResponse eventResponse = trackerImportExportActions.postAndGetJobReport(jsonObject);

    eventResponse.validateSuccessfulImport();

    return eventResponse;
  }
}
