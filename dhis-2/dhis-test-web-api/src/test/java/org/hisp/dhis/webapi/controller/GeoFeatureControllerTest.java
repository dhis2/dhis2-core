/*
 * Copyright (c) 2004-2026, University of Oslo
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

import static org.hisp.dhis.test.config.QueryCountDataSourceProxy.clearCapturedSql;
import static org.hisp.dhis.test.config.QueryCountDataSourceProxy.countCapturedSqlMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.config.QueryCountDataSourceProxy;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author viet@dhis2.org
 */
@Transactional
@ContextConfiguration(classes = {QueryCountDataSourceProxy.class})
class GeoFeatureControllerTest extends PostgresControllerIntegrationTestBase {
  @Autowired private DbmsManager dbmsManager;

  @Autowired private EntityManager entityManager;

  /**
   * Hierarchy (G = has geometry):
   *
   * <pre>
   * A (G)
   * ├── B  (G) ── C  (G)
   * ├── B2 (G)              no children
   * └── B3     ── C3 (G)    B3 itself has no geometry
   * </pre>
   */
  @Language("json")
  private static final String HIERARCHY =
      """
      {"organisationUnits":[
        {"id":"ouAAAAAAAAA","name":"A","shortName":"A","openingDate":"2020-01-01",
          "geometry":{"type":"Point","coordinates":[1.0,1.0]}},
        {"id":"ouBBBBBBBBB","name":"B","shortName":"B","openingDate":"2020-01-01",
          "parent":{"id":"ouAAAAAAAAA"},"geometry":{"type":"Point","coordinates":[2.0,2.0]}},
        {"id":"ouBBBBBBBB2","name":"B2","shortName":"B2","openingDate":"2020-01-01",
          "parent":{"id":"ouAAAAAAAAA"},"geometry":{"type":"Point","coordinates":[2.5,2.5]}},
        {"id":"ouBBBBBBBB3","name":"B3","shortName":"B3","openingDate":"2020-01-01",
          "parent":{"id":"ouAAAAAAAAA"}},
        {"id":"ouCCCCCCCCC","name":"C","shortName":"C","openingDate":"2020-01-01",
          "parent":{"id":"ouBBBBBBBBB"},"geometry":{"type":"Point","coordinates":[3.0,3.0]}},
        {"id":"ouCCCCCCCC3","name":"C3","shortName":"C3","openingDate":"2020-01-01",
          "parent":{"id":"ouBBBBBBBB3"},"geometry":{"type":"Point","coordinates":[3.5,3.5]}}
      ]}""";

  @Test
  void testHasCoordinatesDownAndUpPerLevel() {
    POST("/metadata", HIERARCHY).content(HttpStatus.OK);

    Map<String, JsonObject> level1 = getFeaturesById("ou:LEVEL-1");
    assertEquals(1, level1.size());
    assertTrue(level1.get("ouAAAAAAAAA").getBoolean("hcd").booleanValue());
    assertFalse(level1.get("ouAAAAAAAAA").getBoolean("hcu").booleanValue());

    // B3 has no geometry so is not a feature
    Map<String, JsonObject> level2 = getFeaturesById("ou:LEVEL-2");
    assertEquals(2, level2.size());
    assertTrue(level2.get("ouBBBBBBBBB").getBoolean("hcd").booleanValue());
    assertFalse(level2.get("ouBBBBBBBB2").getBoolean("hcd").booleanValue());
    assertFalse(level2.get("ouBBBBBBBBB").getBoolean("hcu").booleanValue());

    // deepest level: no children; grandparent A has a child with geometry (B)
    Map<String, JsonObject> level3 = getFeaturesById("ou:LEVEL-3");
    assertEquals(2, level3.size());
    for (String uid : new String[] {"ouCCCCCCCCC", "ouCCCCCCCC3"}) {
      assertFalse(level3.get(uid).getBoolean("hcd").booleanValue());
      assertTrue(level3.get(uid).getBoolean("hcu").booleanValue());
      assertEquals(3, level3.get(uid).getNumber("le").intValue());
    }
    assertEquals("ouBBBBBBBBB", level3.get("ouCCCCCCCCC").getString("pi").string());
    assertEquals("B3", level3.get("ouCCCCCCCC3").getString("pn").string());
  }

  /** Resolving hcd/hcu must not lazily load each org unit's children, one query per unit. */
  @Test
  void testNoPerUnitChildrenQueries() {
    POST("/metadata", HIERARCHY).content(HttpStatus.OK);
    dbmsManager.flushSession();
    dbmsManager.clearSession();
    entityManager.getEntityManagerFactory().getCache().evictAll();

    for (String level : new String[] {"ou:LEVEL-1", "ou:LEVEL-2", "ou:LEVEL-3"}) {
      clearCapturedSql();
      assertFalse(getFeaturesById(level).isEmpty());
      assertEquals(0, countCapturedSqlMatching("children0_.parentid=?"), level);
    }
  }

  @Test
  void testGroupSetDimensions() {
    POST("/metadata", HIERARCHY).content(HttpStatus.OK);
    @Language("json")
    String groups =
        """
        {"organisationUnitGroups":[
          {"id":"ougGGGGGGGG","name":"G","shortName":"G",
            "organisationUnits":[{"id":"ouBBBBBBBBB"}]}
        ],
        "organisationUnitGroupSets":[
          {"id":"ougsSSSSSSS","name":"S","shortName":"S",
            "organisationUnitGroups":[{"id":"ougGGGGGGGG"}]}
        ]}""";
    POST("/metadata", groups).content(HttpStatus.OK);

    Map<String, JsonObject> withGroupSets = getFeaturesById("ou:LEVEL-2&includeGroupSets=true");
    assertEquals(
        "ougGGGGGGGG",
        withGroupSets.get("ouBBBBBBBBB").getObject("dimensions").getString("ougsSSSSSSS").string());
    assertTrue(withGroupSets.get("ouBBBBBBBB2").getObject("dimensions").isEmpty());

    Map<String, JsonObject> withoutGroupSets = getFeaturesById("ou:LEVEL-2");
    assertTrue(withoutGroupSets.get("ouBBBBBBBBB").getObject("dimensions").isEmpty());
  }

  private Map<String, JsonObject> getFeaturesById(String ou) {
    JsonArray features = GET("/geoFeatures?ou=" + ou).content(HttpStatus.OK);
    return features.asList(JsonObject.class).stream()
        .collect(Collectors.toMap(f -> f.getString("id").string(), Function.identity()));
  }

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

  /**
   * When no organisation unit at the requested level has a geometry, geoFeatures must still return
   * an empty array with 200, not raise E7143. The geometry pre-filter resolves the org unit
   * dimension to an empty set, which previously would have tripped the empty-dimension guard.
   */
  @Test
  void testGeoFeaturesReturnsEmptyWhenNoOrgUnitHasGeometry() {
    @Language("json")
    String json =
        """
        {"organisationUnits":[
          {"id":"rXnqqH2Pu6N","name":"No Geometry","shortName":"NG","openingDate":"2020-01-01"}
        ]}""";
    POST("/metadata", json).content(HttpStatus.OK);

    JsonArray response = GET("/geoFeatures?ou=ou:LEVEL-1").content(HttpStatus.OK);
    assertEquals(0, response.size());
  }
}
