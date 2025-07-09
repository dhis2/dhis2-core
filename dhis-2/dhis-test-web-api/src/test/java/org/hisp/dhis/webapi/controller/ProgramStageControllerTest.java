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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonTypeReport;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author viet@dhis2.org
 */
@Transactional
class ProgramStageControllerTest extends H2ControllerIntegrationTestBase {

  @Test
  void testCreateProgramStageWithoutProgram() {
    JsonWebMessage message =
        POST("/programStages/", "{'name':'test programStage'}")
            .content(HttpStatus.CONFLICT)
            .as(JsonWebMessage.class);
    JsonTypeReport response = message.get("response", JsonTypeReport.class);
    assertEquals(2, response.getErrorReports().size());
    //    assertEquals(ErrorCode.E4053, response.getErrorReports().get(0).getErrorCode());
  }

  @Test
  void testCreateProgramStageOk() {
    POST(
            "/programs/",
            """
        {'name':'test program', 'id':'VoZMWi7rBgj', 'shortName':'test program','programType':'WITH_REGISTRATION'}""")
        .content(HttpStatus.CREATED);
    String programStageId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/programStages/",
                """
        {'name':'test programStage', 'program':{'id':'VoZMWi7rBgj'}
        ,'executionDateLabel':'executiondatelabel', 'dueDateLabel':'duedatelabel', 'programStageLabel':'programstagelabel', 'eventLabel':'eventlabel' }"""));
    JsonObject programStage = GET("/programStages/{id}", programStageId).content();
    assertEquals("VoZMWi7rBgj", programStage.getString("program.id").string());
    JsonObject program = GET("/programs/{id}", "VoZMWi7rBgj").content();
    assertEquals(programStageId, program.getString("programStages[0].id").string());
    assertEquals("executiondatelabel", programStage.getString("executionDateLabel").string());
    assertEquals("duedatelabel", programStage.getString("dueDateLabel").string());
    assertEquals("programstagelabel", programStage.getString("programStageLabel").string());
    assertEquals("eventlabel", programStage.getString("eventLabel").string());
  }
}
