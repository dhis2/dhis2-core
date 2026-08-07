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
package org.hisp.dhis.metadata.metadata_import;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.test.e2e.actions.LoginActions;
import org.hisp.dhis.test.e2e.actions.metadata.MetadataActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MapWithMapViewProgramImportTest extends ApiTest {

  private MetadataActions metadataActions;

  private static final String ORG_UNIT_UID = "OrgUnit1608";
  private static final String PROGRAM_UID = "Prog2160801";
  private static final String PROGRAM_STAGE_UID = "Stage216080";

  @BeforeAll
  void before() {
    metadataActions = new MetadataActions();
    new LoginActions().loginAsSuperUser();

    // Create our own org unit + event program + program stage to reference from the map.
    // categoryCombo is intentionally omitted so the import assigns the system default.
    String setup =
        """
        {
          "organisationUnits": [
            {
              "id": "%1$s",
              "name": "DHIS2-21608 org unit",
              "shortName": "DHIS2-21608 OU",
              "openingDate": "2020-01-01"
            }
          ],
          "programs": [
            {
              "id": "%2$s",
              "name": "DHIS2-21608 event program",
              "shortName": "DHIS2-21608 EP",
              "programType": "WITHOUT_REGISTRATION",
              "organisationUnits": [ { "id": "%1$s" } ],
              "programStages": [ { "id": "%3$s" } ]
            }
          ],
          "programStages": [
            {
              "id": "%3$s",
              "name": "DHIS2-21608 stage",
              "program": { "id": "%2$s" }
            }
          ]
        }
        """
            .formatted(ORG_UNIT_UID, PROGRAM_UID, PROGRAM_STAGE_UID);

    metadataActions.importMetadata(setup, "importStrategy=CREATE_AND_UPDATE").validateStatus(200);
  }

  @Test
  @DisplayName("importing a Map whose MapView references a program/programStage should succeed")
  void shouldImportMapWithProgramReference() {
    String map =
        """
        {
          "maps": [
            {
              "name": "DHIS2-21608 event map",
              "mapViews": [
                {
                  "name": "Triage",
                  "layer": "event",
                  "renderingStrategy": "SINGLE",
                  "organisationUnits": [ { "id": "%s" } ],
                  "program": { "id": "%s" },
                  "programStage": { "id": "%s" }
                }
              ]
            }
          ]
        }
        """
            .formatted(ORG_UNIT_UID, PROGRAM_UID, PROGRAM_STAGE_UID);

    ApiResponse response =
        metadataActions.post(
            new Gson().fromJson(map, JsonObject.class),
            new QueryParamsBuilder()
                .addAll(
                    "async=false", "importReportMode=FULL", "importStrategy=CREATE_AND_UPDATE"));

    response
        .validate()
        .statusCode(200)
        .rootPath("response")
        .body("status", is(oneOf("SUCCESS", "OK")))
        .body("stats.total", equalTo(1))
        .body("stats.ignored", equalTo(0));
  }
}
