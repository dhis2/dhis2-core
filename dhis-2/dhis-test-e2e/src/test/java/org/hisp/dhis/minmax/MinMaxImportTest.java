/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.minmax;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.io.File;
import java.io.IOException;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.test.e2e.actions.LoginActions;
import org.hisp.dhis.test.e2e.actions.aggregate.MinMaxValuesActions;
import org.hisp.dhis.test.e2e.actions.metadata.MetadataActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MinMaxImportTest extends ApiTest {
  private MinMaxValuesActions minMaxValuesActions;

  private MetadataActions metadataActions;
  private LoginActions loginActions;

  private final String dataSet = "nr5MC9XoK2n";
  private final String dataElement = "elD9B1HiTJO";
  private final String orgUnit = "fKiYlhodhB1";
  private final String defaultCOC = "HllvX50cXC0";

  @BeforeAll
  void before() {
    loginActions = new LoginActions();
    metadataActions = new MetadataActions();
    minMaxValuesActions = new MinMaxValuesActions();

    loginActions.loginAsSuperUser();
    metadataActions
        .importMetadata(new File("src/test/resources/minmax/metadata.json"), "async=false")
        .validate()
        .statusCode(200);
  }

  @BeforeEach
  void setup() {
    loginActions.loginAsSuperUser();
  }

  @Test
  void minMaxValuesCanBeImportedInBulk_JSON() {

    ApiResponse response = postMinMaxJSONFile(false);
    response
        .validate()
        .statusCode(200)
        .body("message", containsString("Successfully imported 1 min-max values"));
  }

  @Test
  void minMaxValuesCanBeDeletedInBulk_JSON() throws IOException {
    postMinMaxJSONFile(false);
    ApiResponse response = postMinMaxJSONFile(true);
    response
        .validate()
        .statusCode(200)
        .body("message", containsString("Successfully deleted 1 min-max values"));
  }

  private ApiResponse postMinMaxJSONFile(Boolean delete) {
    String payload =
        """
        { "dataset": "%s",
          "orgunit": "%s",
          "values" : [{
              "dataElement": "%s",
              "orgUnit": "%s",
              "categoryOptionCombo": "%s",
                "minValue": 10,
                "maxValue": 100
            }]
        }
        """
            .formatted(dataSet, orgUnit, dataElement, orgUnit, defaultCOC);

    QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder();
    if (delete) {
      queryParamsBuilder.add("importStrategy", "DELETE");
    }
    return minMaxValuesActions.post("", payload, queryParamsBuilder);
  }

  @Test
  void minMaxValueCanBeImportedInBulk_CSV() throws IOException {
    postMinMaxCSVFile(false);
  }

  @Test
  void minMaxValueCanBeDeletedInBulk_CSV() throws IOException {
    postMinMaxCSVFile(false);
    ApiResponse response = postMinMaxCSVFile(true);
    response
        .validate()
        .statusCode(200)
        .body("message", containsString("Successfully deleted 4 min-max values"));
  }

  private ApiResponse postMinMaxCSVFile(Boolean delete) {
    QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder();
    queryParamsBuilder.add("dataSet", dataSet).add("orgUnit", orgUnit);
    if (delete) {
      queryParamsBuilder.add("importStrategy", "DELETE");
    }

    return minMaxValuesActions.postMultiPartFile(
        new File("src/test/resources/minmax/minmax.csv"), "application/csv", queryParamsBuilder);
  }

  @Test
  void minMaxValuesCanErrorNoMaxValue_JSON() throws IOException {
    String payload =
        """
                { "dataset": "%s",
                    "orgunit": "%s",
                 "values" : [{
                    "dataElement": "%s",
                    "orgUnit": "%s",
                    "categoryOptionCombo": "%s",
                    "minValue": 10
                  }
                ]
                }"""
            .formatted(dataSet, orgUnit, dataElement, orgUnit, defaultCOC);
    ApiResponse response = minMaxValuesActions.post(payload);
    response
        .validate()
        .statusCode(400)
        .body("status", equalTo("ERROR"))
        .body("message", containsString("Missing required field"));
  }

  @AfterAll
  void tearDown() {
    loginActions.loginAsSuperUser();
    postMinMaxCSVFile(true);
    postMinMaxJSONFile(true);
    ApiResponse response =
        metadataActions.importMetadata(
            new File("src/test/resources/minmax/metadata.json"),
            "async=false&importStrategy=DELETE");
    response.validate().statusCode(200);
  }
}
