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
package org.hisp.dhis.webapi.controller.dataintegrity;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.dataintegrity.DataIntegrityCheckType;
import org.hisp.dhis.http.HttpClientAdapter;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonMap;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.json.domain.JsonDataIntegritySummary;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.hisp.dhis.webapi.controller.DataIntegrityController;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link DataIntegrityController} API with focus API returning {@link
 * org.hisp.dhis.dataintegrity.DataIntegritySummary}.
 *
 * @author Jan Bernitt
 */
class DataIntegritySummaryControllerTest extends AbstractDataIntegrityIntegrationTest {

  @Test
  void testLegacyChecksHaveSummary() {
    for (DataIntegrityCheckType type : DataIntegrityCheckType.values()) {
      String check = type.getName();
      postSummary(check);
      JsonDataIntegritySummary summary = getSummary(check);
      assertTrue(summary.getCount() >= 0, "summary threw an exception");
    }

    // check if the summary map returns results for the programmatic checks
    JsonMap<JsonDataIntegritySummary> checksByName =
        GET("/dataIntegrity/summary?timeout=1000").content().asMap(JsonDataIntegritySummary.class);
    assertFalse(checksByName.isEmpty());
    int checked = 0;
    for (DataIntegrityCheckType type : DataIntegrityCheckType.values()) {
      String name = type.getName().replace("-", "_");
      JsonDataIntegritySummary summary = checksByName.get(name);
      if (summary.exists()) { // not all checks might be done by now
        assertTrue(summary.getIsSlow());
        checked++;
      }
    }
    assertTrue(checked > 0, "at least one of the slow test should have been completed");
  }

  @Test
  void testSingleCheckByPath() {
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/categories",
            "{'name': 'CatDog', 'shortName': 'CD', 'dataDimensionType': 'ATTRIBUTE'}"));

    postSummary("categories-no-options");
    JsonDataIntegritySummary summary =
        GET("/dataIntegrity/categories-no-options/summary")
            .content()
            .as(JsonDataIntegritySummary.class);
    assertTrue(summary.exists());
    assertTrue(summary.isObject());
    assertEquals(1, summary.getCount());
    assertEquals(50, summary.getPercentage().intValue());
    assertNotNull(summary.getStartTime());
    assertNotNull(summary.getCode());
    assertFalse(summary.getStartTime().isAfter(summary.getFinishedTime()));
  }

  @Test
  void testCompletedChecks() {
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/categories",
            "{'name': 'CatDog', 'shortName': 'CD', 'dataDimensionType': 'ATTRIBUTE'}"));

    postSummary("categories-no-options");
    JsonDataIntegritySummary summary =
        GET("/dataIntegrity/categories-no-options/summary")
            .content()
            .as(JsonDataIntegritySummary.class);
    assertNotNull(summary);

    // OBS! The result is based on application scoped map so there might be other values from other
    // tests
    assertTrue(
        GET("/dataIntegrity/summary/completed")
            .content()
            .stringValues()
            .contains("categories_no_options"));
  }

  @Test
  void testRunSummaryCheck_WithBody() {
    JsonObject trigger =
        POST("/dataIntegrity/summary", "['INA']").content(); // indicator_no_analysis
    assertTrue(trigger.isA(JsonWebMessage.class));

    // wait for check to complete
    JsonDataIntegritySummary details =
        GET("/dataIntegrity/IN/summary?timeout=1000").content().as(JsonDataIntegritySummary.class);
    assertTrue(details.isObject());

    assertTrue(
        GET("/dataIntegrity/summary/completed")
            .content()
            .stringValues()
            .contains("indicator_no_analysis"));
  }

  @Test
  void testGetSummaryMetrics() {
    postSummary("categories-no-options");
    HttpResponse response =
        GET("/api/dataIntegrity/metrics", HttpClientAdapter.Accept("text/plain"));
    assertEquals(HttpStatus.OK, response.status());
    String content = response.content("text/plain");
    assertFalse(content.isEmpty(), "Response content should not be empty");
    assertTrue(
        content.contains(
            "# HELP dhis_data_integrity_issues_count_total Data integrity check counts"),
        "Data integrity check help text is missing");
    assertTrue(
        content.contains("# TYPE dhis_data_integrity_issues_count_total gauge"),
        "Data integrity check type is missing");
    assertTrue(
        content.contains(
            "dhis_data_integrity_issues_count_total{check=\"categories_no_options\",severity=\"WARNING\",object_type=\"categories\"}"),
        "Data integrity check count is missing");
    assertTrue(
        content.contains(
            "dhis_data_integrity_issues_percentage{check=\"categories_no_options\",severity=\"WARNING\",object_type=\"categories\"}"),
        "Data integrity check percentage is missing");
    assertTrue(
        content.contains(
            "dhis_data_integrity_check_duration_milliseconds{check=\"categories_no_options\",severity=\"WARNING\",object_type=\"categories\"}"),
        "Data integrity check duration is missing");
  }
}
