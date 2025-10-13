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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Stream;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests the {@link SqlViewController} using (mocked) REST requests. Using Postgres DB to test
 * actual views with criteria, filter & field queries.
 *
 * @author David Mackessy
 */
class SqlViewControllerIntegrationTest extends PostgresControllerIntegrationTestBase {

  private static final String QUERY_PATH = "/sqlViews/sqlViewUid1/data";

  @BeforeEach
  void importMetadataAndCreateSqlView() {
    POST("/metadata?async=false", metadata()).content(HttpStatus.OK);
    POST("/sqlViews/", sqlView()).content(HttpStatus.CREATED);
  }

  @Test
  void sqlInjectionTest() {
    HttpResponse response =
        GET(
            QUERY_PATH
                + "?filter=name:ilike:\"test' AND CAST((SELECT password FROM userinfo WHERE username='admin') AS INTEGER) = 1 AND name ILIKE '\"");
    assertEquals(
        0, response.content(HttpStatus.OK).getObject("pager").getNumber("total").intValue());
  }

  @ParameterizedTest
  @MethodSource("sqlViewFilterQueries")
  void queryFilterTest(String query, Set<String> expectedUids, int expectedResults) {
    JsonMixed content = GET(query).content(HttpStatus.OK);
    assertResponse(content, expectedResults, expectedUids);
  }

  public static Stream<Arguments> sqlViewFilterQueries() {
    return Stream.of(
        Arguments.of(QUERY_PATH, Set.of("optionUidz1", "optionUidz2", "optionUid11"), 3),
        Arguments.of(QUERY_PATH + "?filter=uid:eq:optionUidz2", Set.of("optionUidz2"), 1),
        Arguments.of(QUERY_PATH + "?filter=uid:ieq:optionuidz2", Set.of("optionUidz2"), 1),
        Arguments.of(
            QUERY_PATH + "?filter=uid:!eq:optionUidz2", Set.of("optionUidz1", "optionUid11"), 2),
        Arguments.of(
            QUERY_PATH + "?filter=sort_order:gt:1", Set.of("optionUidz2", "optionUid11"), 2),
        Arguments.of(QUERY_PATH + "?filter=sort_order:lt:2", Set.of("optionUidz1"), 1),
        Arguments.of(
            QUERY_PATH + "?filter=sort_order:gte:2", Set.of("optionUidz2", "optionUid11"), 2),
        Arguments.of(
            QUERY_PATH + "?filter=sort_order:lte:2", Set.of("optionUidz2", "optionUidz1"), 2),
        Arguments.of(
            QUERY_PATH + "?filter=uid:like:optionUid",
            Set.of("optionUidz1", "optionUidz2", "optionUid11"),
            3),
        Arguments.of(
            QUERY_PATH + "?filter=uid:like:optionUid&filter=name:like:option",
            Set.of("optionUidz1", "optionUidz2", "optionUid11"),
            3),
        Arguments.of(
            QUERY_PATH + "?filter=uid:in:[optionUidz2,optionUidz1]&filter=name:like:1",
            Set.of("optionUidz1"),
            1),
        Arguments.of(QUERY_PATH + "?filter=uid:!like:optionUidz", Set.of("optionUid11"), 1),
        Arguments.of(
            QUERY_PATH + "?filter=uid:^like:optionUidz", Set.of("optionUidz1", "optionUidz2"), 2),
        Arguments.of(QUERY_PATH + "?filter=uid:!^like:optionUidz", Set.of("optionUid11"), 1),
        Arguments.of(QUERY_PATH + "?filter=uid:$like:optionUidz1", Set.of("optionUidz1"), 1),
        Arguments.of(
            QUERY_PATH + "?filter=uid:!$like:optionUidz1", Set.of("optionUidz2", "optionUid11"), 2),
        Arguments.of(
            QUERY_PATH + "?filter=uid:ilike:optionuid",
            Set.of("optionUidz1", "optionUidz2", "optionUid11"),
            3),
        Arguments.of(
            QUERY_PATH + "?filter=uid:!ilike:optionuidz2", Set.of("optionUidz1", "optionUid11"), 2),
        Arguments.of(
            QUERY_PATH + "?filter=uid:^ilike:optionuidz", Set.of("optionUidz2", "optionUidz1"), 2),
        Arguments.of(QUERY_PATH + "?filter=uid:!^ilike:optionuidz", Set.of("optionUid11"), 1),
        Arguments.of(QUERY_PATH + "?filter=uid:$ilike:optionuidz", Set.of(), 0),
        Arguments.of(
            QUERY_PATH + "?filter=uid:!$ilike:optionuidz",
            Set.of("optionUidz1", "optionUidz2", "optionUid11"),
            3),
        Arguments.of(
            QUERY_PATH + "?filter=uid:in:[optionUidz2,optionUidz1]",
            Set.of("optionUidz2", "optionUidz1"),
            2),
        Arguments.of(
            QUERY_PATH + "?filter=uid:!in:[optionUidz2,optionUidz1]", Set.of("optionUid11"), 1),
        Arguments.of(
            QUERY_PATH + "?filter=description:null", Set.of("optionUidz2", "optionUid11"), 2),
        Arguments.of(QUERY_PATH + "?filter=description:!null", Set.of("optionUidz1"), 1));
  }

  private void assertResponse(JsonMixed content, int expectedSize, Set<String> uids) {
    assertEquals(expectedSize, content.getObject("pager").getNumber("total").intValue());

    JsonArray rows = content.getObject("listGrid").getArray("rows");
    for (int i = 0; i < rows.size(); i++) {
      JsonList<JsonString> list = rows.get(i).asList(JsonString.class);
      assertTrue(list.stream().anyMatch(s -> uids.contains(s.string())));
    }
  }

  private String sqlView() {
    return """
            {
                "id": "sqlViewUid1",
                "sqlQuery": "SELECT uid, name, description, sort_order FROM optionvalue",
                "type": "QUERY",
                "name": "test-sql-view1",
                "cacheStrategy": "NO_CACHE"
            }""";
  }

  private String metadata() {
    return """
        {
            "options": [
                {
                    "id": "optionUidz1",
                    "name": "test option 1",
                    "code": "test option 1",
                    "sortOrder": 1,
                    "description": "not null"
                 },
                 {
                    "id": "optionUidz2",
                    "name": "test option 2",
                    "code": "test option 2",
                    "sortOrder": 2
                 },
                 {
                    "id": "optionUid11",
                    "name": "test option 11",
                    "code": "test option 11",
                    "sortOrder": 11
                 }
            ]
        }
        """;
  }
}
