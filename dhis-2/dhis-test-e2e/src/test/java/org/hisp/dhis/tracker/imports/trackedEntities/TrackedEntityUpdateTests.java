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
package org.hisp.dhis.tracker.imports.trackedEntities;

import static org.hamcrest.CoreMatchers.containsStringIgnoringCase;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

import com.google.gson.JsonObject;
import java.io.File;
import org.hamcrest.Matchers;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.hisp.dhis.test.e2e.actions.metadata.TrackedEntityTypeActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.JsonObjectBuilder;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.hisp.dhis.tracker.TrackerApiTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TrackedEntityUpdateTests extends TrackerApiTest {
  @BeforeAll
  public void beforeAll() {

    loginActions.loginAsSuperUser();
  }

  @Test
  public void shouldNotUpdateImmutableProperties() throws Exception {
    // arrange
    String te = importTrackedEntity();
    String trackedEntityType = new TrackedEntityTypeActions().create();
    JsonObject body =
        trackerImportExportActions
            .getTrackedEntity(te)
            .getBodyAsJsonBuilder()
            .addProperty("trackedEntity", te)
            .addProperty("trackedEntityType", trackedEntityType)
            .wrapIntoArray("trackedEntities");

    // assert
    trackerImportExportActions
        .postAndGetJobReport(body, new QueryParamsBuilder().add("importStrategy=UPDATE"))
        .validateErrorReport()
        .body("errorCode", Matchers.hasItem("E1126"))
        .body("message", Matchers.hasItem(Matchers.containsString("trackedEntityType")));
  }

  @Test
  public void shouldUpdateWithUpdateStrategy() throws Exception {
    // arrange
    String teId = importTrackedEntity();

    JsonObject teBody = trackerImportExportActions.getTrackedEntity(teId).getBody();
    teBody =
        JsonObjectBuilder.jsonObject(teBody)
            .addProperty("trackedEntity", teId)
            .wrapIntoArray("trackedEntities");

    // act
    ApiResponse response =
        trackerImportExportActions.postAndGetJobReport(
            teBody, new QueryParamsBuilder().add("importStrategy=UPDATE"));

    // assert
    response
        .validate()
        .statusCode(200)
        .body("status", equalTo("OK"))
        .body("stats.updated", equalTo(1));
  }

  @ParameterizedTest
  @ValueSource(strings = {"UPDATE", "DELETE"})
  public void shouldReturnErrorWhenTrackedEntityDoesntExist(String importStrategy)
      throws Exception {
    JsonObject teBody =
        new FileReaderUtils()
            .readJsonAndGenerateData(
                new File("src/test/resources/tracker/importer/trackedEntities/trackedEntity.json"));

    ApiResponse response =
        trackerImportExportActions.postAndGetJobReport(
            teBody,
            new QueryParamsBuilder().add(String.format("importStrategy=%s", importStrategy)));

    response
        .validate()
        .statusCode(200)
        .body("status", equalTo("ERROR"))
        .body("stats.ignored", equalTo(1))
        .body("validationReport.errorReports", notNullValue())
        .rootPath("validationReport.errorReports[0]")
        .body("errorCode", equalTo("E1063"))
        .body("message", containsStringIgnoringCase("does not exist"));
  }

  @Test
  public void shouldUpdateExportedTrackedEntity() throws Exception {
    String teUID = importTrackedEntity();
    JsonObjectBuilder trackedEntities =
        trackerImportExportActions
            .getTrackedEntities(
                new QueryParamsBuilder().add("fields", "*").add("trackedEntity", teUID))
            .getBodyAsJsonBuilder()
            .addPropertyByJsonPath("trackedEntities[0].attributes[0].value", "Rabbit");

    ApiResponse response =
        trackerImportExportActions.postAndGetJobReport(
            trackedEntities.build(), new QueryParamsBuilder().add("importStrategy=UPDATE"));

    response
        .validate()
        .statusCode(200)
        .body("status", equalTo("OK"))
        .body("stats.updated", equalTo(1));
  }
}
