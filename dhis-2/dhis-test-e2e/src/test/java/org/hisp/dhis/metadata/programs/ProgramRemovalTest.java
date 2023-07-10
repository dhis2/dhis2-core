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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonObject;
import java.io.File;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class ProgramRemovalTest extends ApiTest {
  private ProgramActions programActions;

  private RestApiActions relationshipTypeActions;

  private String programId;

  private String programStageId;

  private String relationshipTypeId;

  @BeforeEach
  public void beforeEach() throws Exception {
    programActions = new ProgramActions();
    relationshipTypeActions = new RestApiActions("/relationshipTypes");

    new LoginActions().loginAsSuperUser();
    setupData();
  }

  @Test
  public void shouldRemoveRelationshipTypesWhenProgramIsRemoved() {
    programActions.delete(programId).validate().statusCode(200);

    relationshipTypeActions.get(relationshipTypeId).validate().statusCode(404);
  }

  private void setupData() throws Exception {
    programId = programActions.createProgram("WITH_REGISTRATION").extractUid();
    assertNotNull(programId, "Failed to create program");

    programStageId = programActions.createProgramStage(programId, "stage1");
    assertNotNull(programStageId, "Failed to create programStage");

    JsonObject relationshipType =
        new FileReaderUtils()
            .read(new File("src/test/resources/tracker/relationshipTypes.json"))
            .replacePropertyValuesWithIds("id")
            .get(JsonObject.class)
            .getAsJsonArray("relationshipTypes")
            .get(0)
            .getAsJsonObject();

    new JsonObjectBuilder(relationshipType)
        .addObject(
            "toConstraint",
            new JsonObjectBuilder()
                .addProperty("relationshipEntity", "PROGRAM_STAGE_INSTANCE")
                .addObject("program", new JsonObjectBuilder().addProperty("id", programId))
                .addObject(
                    "programStage", new JsonObjectBuilder().addProperty("id", programStageId)))
        .build();

    relationshipTypeId = relationshipTypeActions.create(relationshipType);
    assertNotNull(relationshipTypeId, "Failed to create relationshipType");
  }
}
