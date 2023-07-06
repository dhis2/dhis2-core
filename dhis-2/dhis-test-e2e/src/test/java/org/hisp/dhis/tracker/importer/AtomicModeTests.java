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
package org.hisp.dhis.tracker.importer;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.google.gson.JsonObject;
import java.io.File;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.file.JsonFileReader;
import org.hisp.dhis.tracker.TrackerNtiApiTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class AtomicModeTests extends TrackerNtiApiTest {
  @BeforeAll
  public void beforeAll() {
    loginActions.loginAsSuperUser();
  }

  @Test
  public void shouldNotImportWhenErrorsWithoutAtomicMode() throws Exception {
    TrackerApiResponse response =
        trackerActions.postAndGetJobReport(
            createWrongPayload(), new QueryParamsBuilder().add("atomicMode=ALL"));

    response.validate().body("status", equalTo("ERROR")).body("stats.ignored", equalTo(3));

    response
        .validateErrorReport()
        .body("", hasSize(2))
        .body("errorCode", containsInAnyOrder("E1121", "E4014"));
  }

  @Test
  public void shouldImportWhenErrorsWithAtomicMode() throws Exception {
    TrackerApiResponse response =
        trackerActions.postAndGetJobReport(
            createWrongPayload(), new QueryParamsBuilder().addAll("atomicMode=OBJECT"));

    response
        .validate()
        .body("status", equalTo("OK"))
        .body("stats.ignored", equalTo(2))
        .body("stats.created", equalTo(1));

    response
        .validateErrorReport()
        .body("", hasSize(2))
        .body("errorCode", containsInAnyOrder("E1121", "E4014"));
  }

  private JsonObject createWrongPayload() throws Exception {
    JsonObject object =
        new JsonFileReader(
                new File("src/test/resources/tracker/importer/teis/teisAndRelationship.json"))
            .replaceStringsWithIds("JjZ2Nwds92v", "JjZ2Nwds93v")
            .get(JsonObject.class);

    object =
        JsonObjectBuilder.jsonObject(object)
            .addPropertyByJsonPath("trackedEntities[0].trackedEntityType", "")
            .build();

    return object;
  }
}
