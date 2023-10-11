/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.metadata.metadata_import;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.SystemActions;
import org.hisp.dhis.dto.ApiResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author david mackessy
 */
class GeoJsonImportTest extends ApiTest {
  private LoginActions loginActions;
  private SystemActions systemActions;
  private RestApiActions restApiActions;

  @BeforeAll
  public void before() {
    loginActions = new LoginActions();
    systemActions = new SystemActions();
    restApiActions = new RestApiActions("organisationUnits");
  }

  @Test
  void geoJsonImportAsync() {
    loginActions.loginAsSuperUser();

    // create geo json
    String geoJson = geoJson();

    // get org unit geometry to show currently empty
    ApiResponse getOrgUnitResponse = restApiActions.get("/ImspTQPwCqd");
    assertNull(getOrgUnitResponse.getBody().get("geometry"));

    // post geo json async
    ApiResponse postGeoJsonAsyncResponse = restApiActions.post("/geometry?async=true", geoJson);
    assertEquals("200", postGeoJsonAsyncResponse.getAsString());
    assertEquals(200, postGeoJsonAsyncResponse.statusCode());

    assertTrue(
        postGeoJsonAsyncResponse
            .getBody()
            .get("message")
            .getAsString()
            .contains("Initiated GeoJSON import"));

    String taskId =
        postGeoJsonAsyncResponse.getBody().getAsJsonObject("response").get("id").getAsString();
    assertEquals(11, taskId.length());

    // wait for job to be completed (24 seconds used as the job schedule loop is 20 seconds)
    ApiResponse taskStatus = systemActions.waitUntilTaskCompleted("GEOJSON_IMPORT", taskId, 24);
    assertTrue(taskStatus.getAsString().contains("\"completed\":true"));

    // get org unit again which should now contain geometry property
    ApiResponse getUpdatedOrgUnit = restApiActions.get("/ImspTQPwCqd");

    // validate async-completed geo json import
    getUpdatedOrgUnit
        .validate()
        .statusCode(200)
        .body("geometry.type", equalTo("Point"))
        .body("geometry.coordinates.size()", equalTo(2));
  }

  private String geoJson() {
    return """
        { 
          "type": "FeatureCollection",
          "features":[
            {
              "type": "Feature",
              "id": "ImspTQPwCqd",
              "geometry": {
                  "type": "Point",
                  "coordinates": [-12.56262192106476,7.376283621891673]
              },
              "properties": {
                  "Shape_Leng": 20.8503761122,
                  "Shape_Area": 5.96427565724,
                  "shapeName": "Republic of Sierra Leone",
                  "Level": "ADM0",
                  "shapeISO": "SLE",
                  "shapeID": "SLE-ADM0-89611731B79725766",
                  "shapeGroup": "SLE",
                  "shapeType": "ADM0",
                  "code": "OU_525"
              }
            }
          ]
        }
        """;
  }
}
