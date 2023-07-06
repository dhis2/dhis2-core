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
package org.hisp.dhis.tracker.importer.events;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.hasSize;

import com.google.gson.JsonObject;
import org.hamcrest.Matchers;
import org.hisp.dhis.Constants;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.tracker.TrackerNtiApiTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class EventUpdateTests extends TrackerNtiApiTest {
  private String eventId;

  @BeforeAll
  public void beforeAll() throws Exception {
    loginActions.loginAsSuperUser();
    eventId = importTeisWithEnrollmentAndEvent().extractImportedEvents().get(0);
  }

  @Test
  public void shouldNotUpdateImmutableProperties() throws Exception {

    String enrollmentId = importEnrollment();
    JsonObject object = trackerActions.get("/events/" + eventId).getBody();

    object =
        JsonObjectBuilder.jsonObject(object)
            .addProperty("programStage", Constants.EVENT_PROGRAM_STAGE_ID)
            .addProperty("enrollment", enrollmentId)
            .wrapIntoArray("events");

    trackerActions
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
    JsonObject object = trackerActions.get("/events/" + eventId).getBody();

    object =
        JsonObjectBuilder.jsonObject(object)
            .addProperty("enrollment", null)
            .wrapIntoArray("events");

    trackerActions
        .postAndGetJobReport(object)
        .validateErrorReport()
        .body("", hasSize(Matchers.greaterThanOrEqualTo(1)))
        .body("errorCode", hasItems("E1033"))
        .body("message", Matchers.hasItem(Matchers.containsString("Enrollment")));
  }
}
