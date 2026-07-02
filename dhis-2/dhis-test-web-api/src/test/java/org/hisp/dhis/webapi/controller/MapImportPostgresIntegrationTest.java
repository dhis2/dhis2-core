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

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonImportSummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Reproduces the {@code TransientPropertyValueException: DataDimensionItem.indicator -> Indicator}
 * that occurs when a {@link org.hisp.dhis.mapping.Map} is exported and re-imported through the
 * {@code POST /metadata} import.
 *
 * <p>This test deliberately does <b>not</b> use {@code @Transactional}: the metadata import runs in
 * its own transaction with a fresh Hibernate session, which is required to surface the bug (a
 * {@code @Transactional} test shares the session, so the referenced {@code Indicator} is already
 * managed and the bug is masked).
 *
 * @author vietnguyen
 */
class MapImportPostgresIntegrationTest extends PostgresControllerIntegrationTestBase {

  private String mapId;

  @AfterEach
  void tearDownMap() {
    // mapview has a FK to maplegendset, so the map (and its map views) must be removed before the
    // test framework empties the database table-by-table.
    if (mapId != null) {
      DELETE("/maps/" + mapId);
    }
  }

  @Test
  void testReimportExportedMapWithIndicator() {
    String ouId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits/",
                "{'name':'OU1','shortName':'OU1','openingDate':'2020-01-01'}"));
    String indicatorTypeId =
        assertStatus(
            HttpStatus.CREATED,
            POST("/indicatorTypes", "{'name':'Per cent','factor':100,'number':false}"));
    String indicatorId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicators",
                "{'name':'Ind1','shortName':'Ind1','indicatorType':{'id':'"
                    + indicatorTypeId
                    + "'},'numerator':'1','denominator':'1'}"));
    String legendSetId = assertStatus(HttpStatus.CREATED, POST("/legendSets/", "{'name':'LS'}"));

    mapId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/maps/",
                "{'name':'My map','mapViews':[{"
                    + "'layer':'thematic1','renderingStrategy':'SINGLE',"
                    + "'legendSet':{'id':'"
                    + legendSetId
                    + "'},"
                    + "'columns':[{'dimension':'dx','items':[{'id':'"
                    + indicatorId
                    + "'}]}],"
                    + "'rows':[{'dimension':'ou','items':[{'id':'"
                    + ouId
                    + "'}]}],"
                    + "'filters':[{'dimension':'pe','items':[{'id':'THIS_YEAR'}]}]"
                    + "}]}]}"));

    JsonObject exported = GET("/maps/{uid}?fields=:owner", mapId).content();

    // Re-import the exported map
    JsonImportSummary summary =
        POST("/metadata", "{\"maps\":[" + exported.toString() + "]}")
            .content(HttpStatus.OK)
            .get("response")
            .as(JsonImportSummary.class);

    // the import must not have failed with TransientPropertyValueException on DataDimensionItem
    assertEquals("OK", summary.getStatus(), "metadata import failed: " + summary.getTypeReports());

    JsonObject importedView =
        GET("/maps/{uid}", mapId).content().getArray("mapViews").get(0).as(JsonObject.class);
    assertEquals(legendSetId, importedView.getObject("legendSet").getString("id").string());
    assertEquals(
        indicatorId,
        importedView
            .getArray("dataDimensionItems")
            .get(0)
            .as(JsonObject.class)
            .getObject("indicator")
            .getString("id")
            .string());
  }
}
