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
package org.hisp.dhis.metadata.programs;

import static org.hamcrest.Matchers.*;

import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.helpers.ResponseValidationHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class ProgramStagesTest extends ApiTest {
  private LoginActions loginActions;

  private ProgramActions programActions;

  private RestApiActions programStageActions;

  private String programId;

  private String programStageId;

  @BeforeAll
  public void beforeAll() {
    loginActions = new LoginActions();
    programActions = new ProgramActions();
    programStageActions = programActions.programStageActions;

    loginActions.loginAsSuperUser();
    programId = programActions.createTrackerProgram(null).extractUid();
    programStageId = programActions.createProgramStage(programId, "Tracker program stage 1");
  }

  @Test
  public void shouldAddProgramStageToProgram() {
    // arrange

    JsonObject programBody =
        programActions
            .get(programId)
            .getBodyAsJsonBuilder()
            .addArray(
                "programStages", new JsonObjectBuilder().addProperty("id", programStageId).build())
            .build();

    // act
    ApiResponse response = programActions.update(programId, programBody);

    // assert
    ResponseValidationHelper.validateObjectUpdate(response, 200);

    response = programActions.get(programId);
    response
        .validate()
        .statusCode(200)
        .body("programStages", not(emptyArray()))
        .body("programStages.id", not(emptyArray()))
        .body("programStages.id", hasItem(programStageId));

    response = programStageActions.get(programStageId);
    response
        .validate()
        .statusCode(200)
        .body("program", notNullValue())
        .body("program.id", equalTo(programId));
  }
}
