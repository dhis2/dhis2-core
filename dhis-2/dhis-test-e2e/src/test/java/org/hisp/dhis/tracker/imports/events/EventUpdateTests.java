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

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

import com.google.gson.JsonObject;
import org.hamcrest.Matchers;
import org.hisp.dhis.test.e2e.Constants;
import org.hisp.dhis.test.e2e.helpers.JsonObjectBuilder;
import org.hisp.dhis.tracker.TrackerApiTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class EventUpdateTests extends TrackerApiTest {
  private String eventId;

  @BeforeAll
  public void beforeAll() throws Exception {
    loginActions.loginAsSuperUser();
    eventId = importTrackedEntitiesWithEnrollmentAndEvent().extractImportedEvents().get(0);
  }

  @Test
  public void shouldNotUpdateImmutableProperties() throws Exception {

    String enrollmentId = importEnrollment();
    JsonObject object = trackerImportExportActions.get("/events/" + eventId).getBody();

    object =
        JsonObjectBuilder.jsonObject(object)
            .addProperty("programStage", Constants.EVENT_PROGRAM_STAGE_ID)
            .addProperty("enrollment", enrollmentId)
            .wrapIntoArray("events");

    trackerImportExportActions
        .postAndGetJobReport(object)
        .validateErrorReport()
        .body("", hasSize(Matchers.greaterThanOrEqualTo(2)))
        .body("errorCode", hasItems("E1128", "E1128"))
        .body(
            "message",
            allOf(
                Matchers.hasItem(Matchers.containsString("programStage")),
                hasItem(Matchers.containsString("enrollment"))));
  }

  @Test
  public void shouldValidateWhenEnrollmentIsMissing() {
    JsonObject object = trackerImportExportActions.get("/events/" + eventId).getBody();

    object =
        JsonObjectBuilder.jsonObject(object)
            .addProperty("enrollment", null)
            .wrapIntoArray("events");

    trackerImportExportActions
        .postAndGetJobReport(object)
        .validateErrorReport()
        .body("", hasSize(Matchers.greaterThanOrEqualTo(1)))
        .body("errorCode", hasItems("E1123"))
        .body("message", Matchers.hasItem(Matchers.containsString("enrollment")));
  }
}
