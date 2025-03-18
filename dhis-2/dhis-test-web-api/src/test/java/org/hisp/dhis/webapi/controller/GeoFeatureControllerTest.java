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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author viet@dhis2.org
 */
@Transactional
class GeoFeatureControllerTest extends H2ControllerIntegrationTestBase {
  @Test
  void testGetWithCoordinateField() {
    @Language("json")
    String json =
        """
        {"organisationUnits": [
          {"id":"rXnqqH2Pu6N","name": "My Unit 1","shortName": "OU1","openingDate": "2020-01-01",
            "attributeValues": [
              {"value":  "{\\"type\\": \\"Polygon\\",\\"coordinates\\":  [[[100,0],[101,0],[101,1],[100,1],[100,0]]] }","attribute": {"id": "RRH9IFiZZYN"}}
            ]
          },
          {"id":"NBfMnCrwlQc","name": "My Unit 3","shortName": "OU3","openingDate": "2020-01-01"}
        ],
        "attributes":[
          {"id":"RRH9IFiZZYN","valueType":"GEOJSON","organisationUnitAttribute":true,"name":"testgeojson"}
        ]}""";
    POST("/metadata", json).content(HttpStatus.OK);

    JsonArray response =
        GET("/geoFeatures?ou=ou:LEVEL-1&&coordinateField=RRH9IFiZZYN").content(HttpStatus.OK);
    assertEquals(1, response.size());
    assertEquals(
        "[[[100.0,0.0],[101.0,0.0],[101.0,1.0],[100.0,1.0],[100.0,0.0]]]",
        response.getObject(0).get("co").node().value().toString());
  }
}
