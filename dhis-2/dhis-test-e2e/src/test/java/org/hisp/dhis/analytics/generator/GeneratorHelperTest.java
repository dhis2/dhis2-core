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
package org.hisp.dhis.analytics.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Verifies the header assertions generated for PostGIS headers, which only some endpoints return on
 * Postgres alone.
 */
class GeneratorHelperTest {

  private static final List<Map<String, Object>> EVENT_AGGREGATE_HEADERS =
      headers("A03MvHHogjR.ou", "A03MvHHogjR.eventdate", "value");

  private static final List<Map<String, Object>> EVENT_QUERY_HEADERS =
      headers(
          "psi",
          "ps",
          "eventdate",
          "createdbydisplayname",
          "lastupdatedbydisplayname",
          "lastupdated",
          "created",
          "completed",
          "scheduleddate",
          "enrollmentdate",
          "incidentdate",
          "tei",
          "pi",
          "geometry",
          "enrollmentgeometry",
          "longitude",
          "latitude",
          "ouname",
          "ounamehierarchy",
          "oucode",
          "programstatus",
          "eventstatus",
          "ou");

  private static final List<Map<String, Object>> EVENT_QUERY_GEOMETRY_SOURCE_HEADERS =
      headers("geometry", "geometrySource");

  private static final List<Map<String, Object>> TRACKED_ENTITY_QUERY_HEADERS =
      headers(
          "trackedentity",
          "lastupdated",
          "lastupdatedbydisplayname",
          "created",
          "createdbydisplayname",
          "geometry",
          "longitude",
          "latitude",
          "ouname",
          "oucode",
          "ounamehierarchy",
          "cejWyOfXge6",
          "lZGmxYbs97q",
          "w75KJ2mc4zz",
          "zDhUuAYrxNC");

  @Test
  void aggregateHeadersHaveSameCountOnEveryDatabase() {
    List<String> databaseDependentHeaders =
        GeneratorHelper.getDatabaseDependentHeaders(EVENT_AGGREGATE_HEADERS, false);

    String structure = GeneratorHelper.buildStructureAssertion(3, 3, databaseDependentHeaders);
    String headersAssertion =
        GeneratorHelper.buildHeadersAssertion(EVENT_AGGREGATE_HEADERS, databaseDependentHeaders);

    assertTrue(structure.contains("validateResponseStructure(response, expectPostgis, 3, 3, 3);"));
    assertFalse(headersAssertion.contains("expectPostgis"));
    assertFalse(headersAssertion.contains("validateHeaderExistence"));
  }

  @Test
  void eventQueryPostgisHeadersAreExpectedOnPostgresOnly() {
    List<String> databaseDependentHeaders =
        GeneratorHelper.getDatabaseDependentHeaders(EVENT_QUERY_HEADERS, true);

    String structure = GeneratorHelper.buildStructureAssertion(1, 23, databaseDependentHeaders);
    String headersAssertion =
        GeneratorHelper.buildHeadersAssertion(EVENT_QUERY_HEADERS, databaseDependentHeaders);

    assertEquals(
        List.of("geometry", "enrollmentgeometry", "longitude", "latitude"),
        databaseDependentHeaders);
    assertTrue(
        structure.contains("validateResponseStructure(response, expectPostgis, 1, 23, 19);"));
    assertEquals(
        """
            if (expectPostgis) {
              validateHeaderExistence(actualHeaders, "geometry", true);
              validateHeaderExistence(actualHeaders, "enrollmentgeometry", true);
              validateHeaderExistence(actualHeaders, "longitude", true);
              validateHeaderExistence(actualHeaders, "latitude", true);
            } else {
              validateHeaderExistence(actualHeaders, "geometry", false);
              validateHeaderExistence(actualHeaders, "enrollmentgeometry", false);
              validateHeaderExistence(actualHeaders, "longitude", false);
              validateHeaderExistence(actualHeaders, "latitude", false);
            }
        """,
        headersAssertion.substring(headersAssertion.lastIndexOf("    if (expectPostgis) {\n")));
    assertTrue(
        headersAssertion.contains(
            "    if (expectPostgis) {\n      validateHeaderPropertiesByName(response, actualHeaders,\"enrollmentgeometry\""));
  }

  @Test
  void onlyRequestedPostgisHeadersAreAsserted() {
    List<String> databaseDependentHeaders =
        GeneratorHelper.getDatabaseDependentHeaders(EVENT_QUERY_GEOMETRY_SOURCE_HEADERS, true);

    String structure = GeneratorHelper.buildStructureAssertion(5, 2, databaseDependentHeaders);
    String headersAssertion =
        GeneratorHelper.buildHeadersAssertion(
            EVENT_QUERY_GEOMETRY_SOURCE_HEADERS, databaseDependentHeaders);

    assertTrue(structure.contains("validateResponseStructure(response, expectPostgis, 5, 2, 0);"));
    assertTrue(
        headersAssertion.contains(
            "validateHeaderExistence(actualHeaders, \"geometrySource\", true);"));
    assertFalse(headersAssertion.contains("\"longitude\""));
    assertFalse(headersAssertion.contains("\"latitude\""));
  }

  @Test
  void trackedEntityQueryPostgisHeadersAreExpectedOnEveryDatabase() {
    List<String> databaseDependentHeaders =
        GeneratorHelper.getDatabaseDependentHeaders(TRACKED_ENTITY_QUERY_HEADERS, false);

    String structure = GeneratorHelper.buildStructureAssertion(1, 15, databaseDependentHeaders);
    String headersAssertion =
        GeneratorHelper.buildHeadersAssertion(
            TRACKED_ENTITY_QUERY_HEADERS, databaseDependentHeaders);

    assertTrue(
        structure.contains("validateResponseStructure(response, expectPostgis, 1, 15, 15);"));
    assertFalse(headersAssertion.contains("expectPostgis"));
    assertFalse(headersAssertion.contains("validateHeaderExistence"));
    assertTrue(
        headersAssertion.contains(
            "    validateHeaderPropertiesByName(response, actualHeaders,\"geometry\""));
  }

  private static List<Map<String, Object>> headers(String... names) {
    return Arrays.stream(names)
        .map(
            name ->
                Map.<String, Object>of(
                    "name",
                    name,
                    "column",
                    name,
                    "valueType",
                    "TEXT",
                    "type",
                    "java.lang.String",
                    "hidden",
                    false,
                    "meta",
                    true))
        .toList();
  }
}
