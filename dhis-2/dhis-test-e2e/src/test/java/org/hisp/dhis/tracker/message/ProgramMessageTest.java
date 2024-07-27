/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.tracker.message;

import static org.hamcrest.Matchers.equalTo;

import io.restassured.path.json.JsonPath;
import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.hisp.dhis.test.e2e.Constants;
import org.hisp.dhis.test.e2e.actions.metadata.MetadataActions;
import org.hisp.dhis.test.e2e.actions.tracker.message.ProgramMessageActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.hisp.dhis.tracker.TrackerApiTest;
import org.hisp.dhis.tracker.imports.databuilder.EnrollmentDataBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Zubair Asghar
 */
class ProgramMessageTest extends TrackerApiTest {
  private String programMessageUid;
  private String enrollmentUid;
  private ProgramMessageActions programMessageActions;

  @BeforeAll
  public void beforeAll() throws Exception {
    loginActions.loginAsSuperUser();
    programMessageActions = new ProgramMessageActions();
    new MetadataActions()
        .importAndValidateMetadata(
            new File("src/test/resources/tracker/programs_with_program_rules.json"));

    String trackerProgramId = "U5HE4IRrZ7S";
    enrollmentUid =
        trackerImportExportActions
            .postAndGetJobReport(
                new EnrollmentDataBuilder()
                    .setTrackedEntity(importTrackedEntity())
                    .setEnrollmentDate(Instant.now().plus(1, ChronoUnit.DAYS).toString())
                    .array(trackerProgramId, Constants.ORG_UNIT_IDS[0]))
            .extractImportedEnrollments()
            .get(0);

    String programOrgUnit = "g8upMTyEZGZ";
    programMessageUid = programMessageActions.sendProgramMessage(enrollmentUid, programOrgUnit);
  }

  @Test
  void shouldGetProgramMessageForEnrollment() {
    QueryParamsBuilder params = new QueryParamsBuilder().add("enrollment", enrollmentUid);

    ApiResponse response = programMessageActions.get("", JSON, JSON, params);

    JsonPath jsonPath = new JsonPath(response.getAsString());
    String question = jsonPath.getString("[0].enrollment.id");

    Assertions.assertEquals(enrollmentUid, question);
  }

  @Test
  void shouldGetErrorWhenEnrollmentDoesNotExist() {
    String invalidEnrollment = "g8upMTyEeee";
    QueryParamsBuilder params = new QueryParamsBuilder().add("enrollment=" + invalidEnrollment);

    ApiResponse response = programMessageActions.get("", JSON, JSON, params);

    response
        .validate()
        .statusCode(409)
        .body("httpStatus", equalTo("Conflict"))
        .body("status", equalTo("ERROR"))
        .body(
            "message", equalTo(String.format("Enrollment: %s does not exist.", invalidEnrollment)));
  }

  @Test
  void shouldUpdateProgramMessage() {
    ApiResponse response = programMessageActions.get(programMessageUid);
    response.validate().statusCode(200).body("messageStatus", equalTo("FAILED"));

    response = programMessageActions.updateNoBody(programMessageUid + "?status=SENT");

    response
        .validate()
        .statusCode(200)
        .body("httpStatus", equalTo("OK"))
        .body("status", equalTo("OK"))
        .body(
            "message",
            equalTo(String.format("ProgramMessage with id %s updated", programMessageUid)));

    response = programMessageActions.get(programMessageUid);
    response.validate().statusCode(200).body("messageStatus", equalTo("SENT"));
  }
}
