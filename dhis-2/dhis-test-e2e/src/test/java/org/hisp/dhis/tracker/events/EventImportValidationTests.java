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
package org.hisp.dhis.tracker.events;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonObject;
import java.util.stream.Stream;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.metadata.OrgUnitActions;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.tracker.TrackerApiTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class EventImportValidationTests extends TrackerApiTest {
  private static String ouId = Constants.ORG_UNIT_IDS[0];

  private static String eventProgramId = Constants.EVENT_PROGRAM_ID;

  private static String eventProgramStageId;

  private static String trackerProgramId = Constants.TRACKER_PROGRAM_ID;

  private static String ouIdWithoutAccess;

  private static Stream<Arguments> provideValidationArguments() {
    return Stream.of(
        Arguments.arguments(
            null,
            eventProgramId,
            eventProgramStageId,
            "Event.orgUnit does not point to a valid organisation unit"),
        Arguments.arguments(
            ouIdWithoutAccess,
            eventProgramId,
            eventProgramStageId,
            "Program is not assigned to this organisation unit"),
        Arguments.arguments(
            ouId, null, eventProgramStageId, "Event.program does not point to a valid program"),
        Arguments.arguments(
            ouId,
            trackerProgramId,
            null,
            "Event.programStage does not point to a valid programStage"));
  }

  @BeforeAll
  public void beforeAll() {
    loginActions.loginAsAdmin();

    setupData();
  }

  @ParameterizedTest
  @MethodSource("provideValidationArguments")
  public void eventImportShouldValidateReferences(
      String ouId, String programId, String programStageId, String message) {
    JsonObject jsonObject = eventActions.createEventBody(ouId, programId, programStageId);

    eventActions
        .post(jsonObject)
        .validate()
        .statusCode(409)
        .body("status", equalTo("ERROR"))
        .rootPath("response")
        .body("ignored", equalTo(1))
        .body("importSummaries.description[0]", containsStringIgnoringCase(message));
  }

  @Test
  public void eventImportShouldValidateEventDate() {
    JsonObject object = eventActions.createEventBody(ouId, eventProgramId, eventProgramStageId);

    object.addProperty("eventDate", "");
    object.addProperty("status", "ACTIVE");

    eventActions
        .post(object)
        .validate()
        .statusCode(409)
        .body("status", equalTo("ERROR"))
        .rootPath("response")
        .body("ignored", equalTo(1))
        .body("importSummaries.description[0]", containsString("Event date is required"));
  }

  private void setupData() {
    eventProgramStageId =
        programActions
            .programStageActions
            .get("", new QueryParamsBuilder().add("filter=program.id:eq:" + eventProgramId))
            .extractString("programStages.id[0]");

    assertNotNull(eventProgramStageId, "Failed to find a program stage");

    ouIdWithoutAccess = new OrgUnitActions().createOrgUnit();
  }
}
