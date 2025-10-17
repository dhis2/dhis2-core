/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.web.HttpStatus.CONFLICT;
import static org.hisp.dhis.web.HttpStatus.CREATED;
import static org.hisp.dhis.web.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonResponse;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests the {@link SqlViewController} using (mocked) REST requests. Using Postgres DB to test
 * actual views with criteria, filter & field queries.
 *
 * @author David Mackessy
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SqlViewControllerIntegrationTest extends DhisControllerConvenienceTest {

  private static final String QUERY_PATH = "/sqlViews/sqlViewUid1/data";
  private static final String VIEW_PATH = "/sqlViews/sqlViewUid2/data";
  private static boolean setupComplete = false;

  @BeforeEach
  void beforeEach() {
    setupMetadataAndSqlViews();
  }

  @Test
  void sqlInjectionFilterTest() {
    HttpResponse response =
        GET(
            QUERY_PATH
                + "?filter=name:ilike:\"test' AND CAST((SELECT password FROM userinfo WHERE username='admin') AS INTEGER) = 1 AND name ILIKE '\"");
    assertEquals(0, response.content(OK).getObject("pager").getNumber("total").intValue());
  }

  @Test
  void sqlInjectionFieldTest() {
    HttpResponse response =
        GET(
            QUERY_PATH
                + "?fields=*,(SELECT password FROM userinfo WHERE username='admin') AS admin_hash"
                + "&filter=name:ilike:test");
    assertTrue(
        response
            .content(CONFLICT)
            .getObject("message")
            .toString()
            .contains("Query failed because of a syntax error"));
  }

  @Test
  void filterCriteriaFieldsQueryTest() {
    HttpResponse response =
        GET(QUERY_PATH + "?fields=uid&filter=uid:ilike:uid&criteria=sort_order:2");
    assertFilterResponse(response.content(OK), 2, Set.of("optionUidz3", "optionUidz2"));
  }

  @Test
  void filterCriteriaFieldsViewTest() {
    HttpResponse response =
        GET(VIEW_PATH + "?fields=uid&filter=uid:ilike:uid&criteria=sort_order:2");
    assertFilterResponse(response.content(OK), 2, Set.of("optionUidz3", "optionUidz2"));
  }

  @ParameterizedTest
  @MethodSource("sqlViewFilterQueries")
  void queryFilterTest(String query, Set<String> expectedUids, int expectedNumResults) {
    JsonResponse content = GET(query).content(OK);
    assertFilterResponse(content, expectedNumResults, expectedUids);
  }

  @ParameterizedTest
  @MethodSource("sqlViewFieldQueries")
  void queryFieldsTest(
      String query,
      Set<String> expectedFields,
      Set<String> expectedValues,
      int expectedNumResults) {
    JsonResponse content = GET(query).content(OK);
    assertFieldsResponse(content, expectedNumResults, expectedFields, expectedValues);
  }

  @ParameterizedTest
  @MethodSource("sqlViewCriteriaQueries")
  void queryCriteriaTest(String query, Set<String> expectedValues, int expectedNumResults) {
    JsonResponse content = GET(query).content(OK);
    assertFilterResponse(content, expectedNumResults, expectedValues);
  }

  public static Stream<Arguments> sqlViewFilterQueries() {
    return Stream.of(
        // -------------
        // QUERY TYPE
        // -------------
        Arguments.of(
            QUERY_PATH, Set.of("optionUidz1", "optionUidz2", "optionUidz3", "optionUid11"), 4),

        // single filter
        Arguments.of(QUERY_PATH + "?filter=uid:eq:optionUidz2", Set.of("optionUidz2"), 1),
        Arguments.of(QUERY_PATH + "?filter=uid:ieq:optionuidz2", Set.of("optionUidz2"), 1),
        Arguments.of(
            QUERY_PATH + "?filter=uid:!eq:optionUidz2",
            Set.of("optionUidz1", "optionUidz3", "optionUid11"),
            3),
        Arguments.of(
            QUERY_PATH + "?filter=sort_order:gt:1",
            Set.of("optionUidz2", "optionUidz3", "optionUid11"),
            3),
        Arguments.of(QUERY_PATH + "?filter=sort_order:lt:2", Set.of("optionUidz1"), 1),
        Arguments.of(
            QUERY_PATH + "?filter=sort_order:gte:2",
            Set.of("optionUidz2", "optionUidz3", "optionUid11"),
            3),
        Arguments.of(
            QUERY_PATH + "?filter=sort_order:lte:2",
            Set.of("optionUidz2", "optionUidz3", "optionUidz1"),
            3),
        Arguments.of(
            QUERY_PATH + "?filter=uid:like:optionUid",
            Set.of("optionUidz1", "optionUidz2", "optionUidz3", "optionUid11"),
            4),
        Arguments.of(QUERY_PATH + "?filter=uid:!like:optionUidz", Set.of("optionUid11"), 1),
        Arguments.of(
            QUERY_PATH + "?filter=uid:^like:optionUidz",
            Set.of("optionUidz1", "optionUidz2", "optionUidz3"),
            3),
        Arguments.of(QUERY_PATH + "?filter=uid:!^like:optionUidz", Set.of("optionUid11"), 1),
        Arguments.of(QUERY_PATH + "?filter=uid:$like:optionUidz1", Set.of("optionUidz1"), 1),
        Arguments.of(
            QUERY_PATH + "?filter=uid:!$like:optionUidz1",
            Set.of("optionUidz2", "optionUidz3", "optionUid11"),
            3),
        Arguments.of(
            QUERY_PATH + "?filter=uid:ilike:optionuid",
            Set.of("optionUidz1", "optionUidz2", "optionUidz3", "optionUid11"),
            4),
        Arguments.of(
            QUERY_PATH + "?filter=uid:!ilike:optionuidz2",
            Set.of("optionUidz1", "optionUidz3", "optionUid11"),
            3),
        Arguments.of(
            QUERY_PATH + "?filter=uid:^ilike:optionuidz",
            Set.of("optionUidz2", "optionUidz3", "optionUidz1"),
            3),
        Arguments.of(QUERY_PATH + "?filter=uid:!^ilike:optionuidz", Set.of("optionUid11"), 1),
        Arguments.of(QUERY_PATH + "?filter=uid:$ilike:optionuidz", Set.of(), 0),
        Arguments.of(
            QUERY_PATH + "?filter=uid:!$ilike:optionuidz",
            Set.of("optionUidz1", "optionUidz2", "optionUidz3", "optionUid11"),
            4),
        Arguments.of(
            QUERY_PATH + "?filter=uid:in:[optionUidz2,optionUidz1]",
            Set.of("optionUidz2", "optionUidz1"),
            2),
        Arguments.of(
            QUERY_PATH + "?filter=uid:!in:[optionUidz2,optionUidz1]",
            Set.of("optionUidz3", "optionUid11"),
            2),
        Arguments.of(
            QUERY_PATH + "?filter=description:null",
            Set.of("optionUidz2", "optionUidz3", "optionUid11"),
            3),
        Arguments.of(QUERY_PATH + "?filter=description:!null", Set.of("optionUidz1"), 1),

        // multiple filters
        Arguments.of(
            QUERY_PATH + "?filter=uid:like:optionUidz&filter=name:like:option",
            Set.of("optionUidz1", "optionUidz2", "optionUidz3"),
            3),
        Arguments.of(
            QUERY_PATH + "?filter=uid:in:[optionUidz2,optionUidz1]&filter=name:like:1",
            Set.of("optionUidz1"),
            1),

        // -------------
        // VIEW TYPE
        // -------------
        Arguments.of(
            VIEW_PATH, Set.of("optionUidz1", "optionUidz2", "optionUidz3", "optionUid11"), 4),

        // single filter
        Arguments.of(VIEW_PATH + "?filter=uid:eq:optionUidz2", Set.of("optionUidz2"), 1),
        Arguments.of(VIEW_PATH + "?filter=uid:ieq:optionuidz2", Set.of("optionUidz2"), 1),
        Arguments.of(
            VIEW_PATH + "?filter=uid:!eq:optionUidz2",
            Set.of("optionUidz1", "optionUidz3", "optionUid11"),
            3),
        Arguments.of(
            VIEW_PATH + "?filter=sort_order:gt:1",
            Set.of("optionUidz2", "optionUidz3", "optionUid11"),
            3),
        Arguments.of(VIEW_PATH + "?filter=sort_order:lt:2", Set.of("optionUidz1"), 1),
        Arguments.of(
            VIEW_PATH + "?filter=sort_order:gte:2",
            Set.of("optionUidz2", "optionUidz3", "optionUid11"),
            3),
        Arguments.of(
            VIEW_PATH + "?filter=sort_order:lte:2",
            Set.of("optionUidz2", "optionUidz3", "optionUidz1"),
            3),
        Arguments.of(
            VIEW_PATH + "?filter=uid:like:optionUid",
            Set.of("optionUidz1", "optionUidz2", "optionUidz3", "optionUid11"),
            4),
        Arguments.of(VIEW_PATH + "?filter=uid:!like:optionUidz", Set.of("optionUid11"), 1),
        Arguments.of(
            VIEW_PATH + "?filter=uid:^like:optionUidz",
            Set.of("optionUidz1", "optionUidz2", "optionUidz3"),
            3),
        Arguments.of(VIEW_PATH + "?filter=uid:!^like:optionUidz", Set.of("optionUid11"), 1),
        Arguments.of(VIEW_PATH + "?filter=uid:$like:optionUidz1", Set.of("optionUidz1"), 1),
        Arguments.of(
            VIEW_PATH + "?filter=uid:!$like:optionUidz1",
            Set.of("optionUidz2", "optionUidz3", "optionUid11"),
            3),
        Arguments.of(
            VIEW_PATH + "?filter=uid:ilike:optionuid",
            Set.of("optionUidz1", "optionUidz2", "optionUidz3", "optionUid11"),
            4),
        Arguments.of(
            VIEW_PATH + "?filter=uid:!ilike:optionuidz2",
            Set.of("optionUidz1", "optionUidz3", "optionUid11"),
            3),
        Arguments.of(
            VIEW_PATH + "?filter=uid:^ilike:optionuidz",
            Set.of("optionUidz2", "optionUidz3", "optionUidz1"),
            3),
        Arguments.of(VIEW_PATH + "?filter=uid:!^ilike:optionuidz", Set.of("optionUid11"), 1),
        Arguments.of(VIEW_PATH + "?filter=uid:$ilike:optionuidz", Set.of(), 0),
        Arguments.of(
            VIEW_PATH + "?filter=uid:!$ilike:optionuidz",
            Set.of("optionUidz1", "optionUidz2", "optionUidz3", "optionUid11"),
            4),
        Arguments.of(
            VIEW_PATH + "?filter=uid:in:[optionUidz2,optionUidz1]",
            Set.of("optionUidz2", "optionUidz1"),
            2),
        Arguments.of(
            VIEW_PATH + "?filter=uid:!in:[optionUidz2,optionUidz1]",
            Set.of("optionUidz3", "optionUid11"),
            2),
        Arguments.of(
            VIEW_PATH + "?filter=description:null",
            Set.of("optionUidz2", "optionUidz3", "optionUid11"),
            3),
        Arguments.of(VIEW_PATH + "?filter=description:!null", Set.of("optionUidz1"), 1),

        // multiple filters
        Arguments.of(
            VIEW_PATH + "?filter=uid:like:optionUidz&filter=name:like:option",
            Set.of("optionUidz1", "optionUidz2", "optionUidz3"),
            3),
        Arguments.of(
            VIEW_PATH + "?filter=uid:in:[optionUidz2,optionUidz1]&filter=name:like:1",
            Set.of("optionUidz1"),
            1));
  }

  /**
   * Fields queries also contain a filter condition as views of type QUERY only apply the fields
   * param when with a filter or criteria param
   */
  public static Stream<Arguments> sqlViewFieldQueries() {
    return Stream.of(
        // -------------
        // QUERY TYPE
        // -------------
        Arguments.of(
            QUERY_PATH + "?fields=*&filter=uid:ilike:uid",
            Set.of("uid", "name", "description", "sort_order"),
            Set.of("optionUidz1", "test option 1", "test description", "1"),
            4),
        Arguments.of(
            QUERY_PATH + "?fields=uid&filter=uid:ilike:uid",
            Set.of("uid"),
            Set.of("optionUidz2"),
            4),
        Arguments.of(
            QUERY_PATH + "?fields=name,description&filter=name:ilike:test",
            Set.of("name", "description"),
            Set.of("test option 3", ""),
            4),

        // -------------
        // VIEW TYPE
        // -------------
        Arguments.of(
            VIEW_PATH + "?fields=*&filter=uid:ilike:uid",
            Set.of("uid", "name", "description", "sort_order"),
            Set.of("optionUidz1", "test option 1", "test description", "1"),
            4),
        Arguments.of(
            VIEW_PATH + "?fields=uid&filter=uid:ilike:uid",
            Set.of("uid"),
            Set.of("optionUidz2"),
            4),
        Arguments.of(
            VIEW_PATH + "?fields=name,description&filter=name:ilike:test",
            Set.of("name", "description"),
            Set.of("test option 3", ""),
            4));
  }

  public static Stream<Arguments> sqlViewCriteriaQueries() {
    return Stream.of(
        // -------------
        // QUERY TYPE
        // -------------
        Arguments.of(QUERY_PATH + "?criteria=name:test option 1", Set.of("optionUidz1"), 1),
        Arguments.of(
            QUERY_PATH + "?criteria=sort_order:2", Set.of("optionUidz2", "optionUidz3"), 2),
        Arguments.of(
            QUERY_PATH + "?criteria=sort_order:2&criteria=uid:optionUidz2",
            Set.of("optionUidz2"),
            1),

        // -------------
        // VIEW TYPE
        // -------------
        Arguments.of(QUERY_PATH + "?criteria=name:test option 1", Set.of("optionUidz1"), 1),
        Arguments.of(
            QUERY_PATH + "?criteria=sort_order:2", Set.of("optionUidz2", "optionUidz3"), 2),
        Arguments.of(
            QUERY_PATH + "?criteria=sort_order:2&criteria=uid:optionUidz2",
            Set.of("optionUidz2"),
            1));
  }

  private void assertFilterResponse(
      JsonResponse content, int expectedSize, Set<String> expectedUids) {
    assertEquals(expectedSize, content.getObject("pager").getNumber("total").intValue());

    JsonArray rows = content.getObject("listGrid").getArray("rows");
    for (int i = 0; i < rows.size(); i++) {
      JsonList<JsonString> rowData = rows.get(i).asList(JsonString.class);
      assertTrue(rowData.stream().anyMatch(row -> expectedUids.contains(row.string())));
    }
  }

  private void assertFieldsResponse(
      JsonResponse content,
      int expectedSize,
      Set<String> expectedFields,
      Set<String> expectedValues) {
    assertEquals(expectedSize, content.getObject("pager").getNumber("total").intValue());

    JsonArray headers = content.getObject("listGrid").getArray("headers");
    JsonArray rows = content.getObject("listGrid").getArray("rows");
    for (int i = 0; i < headers.size(); i++) {
      JsonObject header = headers.get(i).asObject();
      assertTrue(expectedFields.contains(header.getString("column").string()));
    }

    assertTrue(
        rows.asList(JsonString.class).stream()
            .anyMatch(
                row ->
                    expectedValues.containsAll(
                        row.asList(JsonString.class).stream()
                            .map(JsonString::string)
                            .collect(Collectors.toSet()))));
  }

  void setupMetadataAndSqlViews() {
    if (!setupComplete) {
      POST("/metadata?async=false", metadata()).content(OK);
      POST("/sqlViews/", sqlQueryView()).content(CREATED);
      POST("/sqlViews/", sqlView()).content(CREATED);
      POST("/sqlViews/sqlViewUid2/execute").content(CREATED);
      setupComplete = true;
    }
  }

  private String sqlQueryView() {
    return "{\n"
        + "    \"id\": \"sqlViewUid1\",\n"
        + "    \"sqlQuery\": \"SELECT uid, name, description, sort_order FROM optionvalue\",\n"
        + "    \"type\": \"QUERY\",\n"
        + "    \"name\": \"test-sql-view1\",\n"
        + "    \"cacheStrategy\": \"NO_CACHE\"\n"
        + "}";
  }

  private String sqlView() {
    return "{\n"
        + "    \"id\": \"sqlViewUid2\",\n"
        + "    \"sqlQuery\": \"SELECT uid, name, description, sort_order FROM optionvalue\",\n"
        + "    \"type\": \"VIEW\",\n"
        + "    \"name\": \"test-sql-view2\",\n"
        + "    \"cacheStrategy\": \"NO_CACHE\"\n"
        + "}";
  }

  private String metadata() {
    return "{\n"
        + "    \"options\": [\n"
        + "        {\n"
        + "            \"id\": \"optionUidz1\",\n"
        + "            \"name\": \"test option 1\",\n"
        + "            \"code\": \"test option 1\",\n"
        + "            \"sortOrder\": 1,\n"
        + "            \"description\": \"test description\"\n"
        + "        },\n"
        + "        {\n"
        + "            \"id\": \"optionUidz2\",\n"
        + "            \"name\": \"test option 2\",\n"
        + "            \"code\": \"test option 2\",\n"
        + "            \"sortOrder\": 2\n"
        + "        },\n"
        + "        {\n"
        + "            \"id\": \"optionUidz3\",\n"
        + "            \"name\": \"test option 3\",\n"
        + "            \"code\": \"test option 3\",\n"
        + "            \"sortOrder\": 2\n"
        + "        },\n"
        + "        {\n"
        + "            \"id\": \"optionUid11\",\n"
        + "            \"name\": \"test option 11\",\n"
        + "            \"code\": \"test option 11\",\n"
        + "            \"sortOrder\": 11\n"
        + "        }\n"
        + "    ]\n"
        + "}";
  }
}
