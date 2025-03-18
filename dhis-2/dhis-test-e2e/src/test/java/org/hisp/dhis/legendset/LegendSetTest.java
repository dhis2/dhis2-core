/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.legendset;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;

import org.hisp.dhis.ApiTest;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.actions.metadata.MetadataActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LegendSetTest extends ApiTest {

  private MetadataActions metadataActions;
  private RestApiActions legendSetActions;
  private RestApiActions dataSetActions;

  @BeforeAll
  public void init() {
    metadataActions = new MetadataActions();
    legendSetActions = new RestApiActions("legendSets");
    dataSetActions = new RestApiActions("dataSets");
  }

  @Test
  @DisplayName("Deleting a legend set which is referenced by a data set is successful")
  void deleteLegendSetTest() {
    metadataActions.importMetadata(dataSetWithLegendSet()).validateStatus(200);

    // confirm data set has legend set
    dataSetActions
        .get("DsUid000001")
        .validate()
        .body("legendSets", hasItem(allOf(hasEntry("id", "LegSetUid01"))));

    // delete legend set
    ApiResponse response = legendSetActions.delete("LegSetUid01");
    response.validate().body("httpStatus", equalTo("OK")).body("httpStatusCode", equalTo(200));

    // confirm data set no longer has legend set
    dataSetActions.get("DsUid000001").validate().body("legendSets", empty());
  }

  private String dataSetWithLegendSet() {
    return """
      {
        "dataSets": [
          {
            "name": "ds 1",
            "id": "DsUid000001",
            "shortName": "ds 1",
            "periodType": "Monthly",
            "legendSets": [
              {
                "id": "LegSetUid01"
              }
            ]
          }
        ],
        "legendSets": [
          {
            "name": "Test legend11",
            "legends": [
              {
                "name": "45 - 60",
                "startValue": 45.0,
                "endValue": 60.0,
                "displayName": "45 - 60",
                "id": "LegUid00001"
              }
            ],
            "displayName": "Test legend",
            "id": "LegSetUid01"
            }
        ]
      }
      """;
  }
}
