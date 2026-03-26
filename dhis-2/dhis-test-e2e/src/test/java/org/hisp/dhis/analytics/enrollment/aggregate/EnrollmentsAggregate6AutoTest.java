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
package org.hisp.dhis.analytics.enrollment.aggregate;

import static org.hisp.dhis.analytics.ValidationHelper.validateHeaderExistence;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeaderPropertiesByName;
import static org.hisp.dhis.analytics.ValidationHelper.validateResponseStructure;
import static org.hisp.dhis.analytics.ValidationHelper.validateRowExists;
import static org.hisp.dhis.analytics.ValidationHelper.validateRowValueByName;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsEnrollmentsActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/enrollments/aggregate" endpoint. */
public class EnrollmentsAggregate6AutoTest extends AnalyticsApiTest {
  private final AnalyticsEnrollmentsActions actions = new AnalyticsEnrollmentsActions();

  @Nested
  @DisplayName("Last updated")
  class LastUpdated {
    @Test
    @DisplayName("Enrollments Aggregate - Financial Sep - Time field: lastUpdated")
    public void lastUpdatedWithFinancialSep() throws JSONException {
      // Read the 'expect.postgis' system property at runtime to adapt assertions.
      boolean expectPostgis = isPostgres();

      // Given
      QueryParamsBuilder params =
          new QueryParamsBuilder()
              .add("includeMetadataDetails=true")
              .add("lastUpdated=2018Sep")
              .add("displayProperty=NAME")
              .add("totalPages=false")
              .add("rowContext=true")
              .add("pageSize=100")
              .add("page=1")
              .add("dimension=ou:USER_ORGUNIT,GxdhnY5wmHq");

      // When
      ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

      // Then
      // 1. Validate Response Structure (Counts, Headers, Height/Width)
      //    This helper checks basic counts and dimensions, adapting based on the runtime
      // 'expectPostgis' flag.
      validateResponseStructure(
          response,
          expectPostgis,
          1,
          4,
          4); // Pass runtime flag, row count, and expected header counts

      // 2. Extract Headers into a List of Maps for easy access by name
      List<Map<String, Object>> actualHeaders =
          response.extractList("headers", Map.class).stream()
              .map(obj -> (Map<String, Object>) obj) // Ensure correct type
              .collect(Collectors.toList());

      // 3. Assert metaData.
      String expectedMetaData =
          "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"2018Sep\":{\"uid\":\"2018Sep\",\"code\":\"2018Sep\",\"name\":\"September 2018 - August 2019\",\"description\":\"2018Sep\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2018-09-01T00:00:00.000\",\"endDate\":\"2019-08-31T00:00:00.000\"},\"pe\":{\"uid\":\"pe\",\"dimensionType\":\"PERIOD\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"lastupdated\":{\"name\":\"Last updated\"},\"GxdhnY5wmHq\":{\"uid\":\"GxdhnY5wmHq\",\"name\":\"Average weight (g)\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"lastupdated\":[\"2018Sep\"],\"GxdhnY5wmHq\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
      String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
      assertEquals(expectedMetaData, actualMetaData, false);

      // 4. Validate Headers By Name (conditionally checking PostGIS headers).
      validateHeaderPropertiesByName(
          response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "ou",
          "Organisation unit",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "lastupdated",
          "Last updated",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "GxdhnY5wmHq",
          "Average weight (g)",
          "NUMBER",
          "java.lang.Double",
          false,
          true);

      // rowContext not found or empty in the response, skipping assertions.

      // 7. Assert row existence by value (unsorted results - validates all columns).
      // Validate row exists with values from original row index 0
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "1", "ou", "ImspTQPwCqd", "lastupdated", "2018Sep", "GxdhnY5wmHq", "3271"));
    }

    @Test
    @DisplayName("Enrollments Aggregate - Financial Sep - Time field: LAST_UPDATED as dimension")
    public void lastUpdatedWithFinancialSepAsDimension() throws JSONException {
      // Read the 'expect.postgis' system property at runtime to adapt assertions.
      boolean expectPostgis = isPostgres();

      // Given
      QueryParamsBuilder params =
          new QueryParamsBuilder()
              .add("includeMetadataDetails=true")
              .add("displayProperty=NAME")
              .add("totalPages=false")
              .add("rowContext=true")
              .add("pageSize=100")
              .add("outputType=ENROLLMENT")
              .add("page=1")
              .add("dimension=ou:USER_ORGUNIT,GxdhnY5wmHq,LAST_UPDATED:2018Sep");

      // When
      ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

      // Then
      // 1. Validate Response Structure (Counts, Headers, Height/Width)
      //    This helper checks basic counts and dimensions, adapting based on the runtime
      // 'expectPostgis' flag.
      validateResponseStructure(
          response,
          expectPostgis,
          1,
          4,
          4); // Pass runtime flag, row count, and expected header counts

      // 2. Extract Headers into a List of Maps for easy access by name
      List<Map<String, Object>> actualHeaders =
          response.extractList("headers", Map.class).stream()
              .map(obj -> (Map<String, Object>) obj) // Ensure correct type
              .collect(Collectors.toList());

      // 3. Assert metaData.
      String expectedMetaData =
          "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"2018Sep\":{\"uid\":\"2018Sep\",\"code\":\"2018Sep\",\"name\":\"September 2018 - August 2019\",\"description\":\"2018Sep\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2018-09-01T00:00:00.000\",\"endDate\":\"2019-08-31T00:00:00.000\"},\"pe\":{\"uid\":\"pe\",\"dimensionType\":\"PERIOD\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"lastupdated\":{\"name\":\"Last updated\"},\"GxdhnY5wmHq\":{\"uid\":\"GxdhnY5wmHq\",\"name\":\"Average weight (g)\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"lastupdated\":[\"2018Sep\"],\"GxdhnY5wmHq\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
      String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
      assertEquals(expectedMetaData, actualMetaData, false);

      // 4. Validate Headers By Name (conditionally checking PostGIS headers).
      validateHeaderPropertiesByName(
          response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "ou",
          "Organisation unit",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "lastupdated",
          "Last updated",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "GxdhnY5wmHq",
          "Average weight (g)",
          "NUMBER",
          "java.lang.Double",
          false,
          true);

      // rowContext not found or empty in the response, skipping assertions.

      // 7. Assert row existence by value (unsorted results - validates all columns).
      // Validate row exists with values from original row index 0
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "1", "ou", "ImspTQPwCqd", "lastupdated", "2018Sep", "GxdhnY5wmHq", "3271"));
    }
  }

  @Nested
  @DisplayName("Event date")
  class EventDate {
    @Test
    @DisplayName("Enrollments Aggregate - Financial Year 2022 Sep - Time field: eventDate")
    public void financialYear2022WithEventDate() throws JSONException {
      // Read the 'expect.postgis' system property at runtime to adapt assertions.
      boolean expectPostgis = isPostgres();

      // Given
      QueryParamsBuilder params =
          new QueryParamsBuilder()
              .add("includeMetadataDetails=true")
              .add("displayProperty=NAME")
              .add("totalPages=false")
              .add("rowContext=true")
              .add("pageSize=100")
              .add("outputType=ENROLLMENT")
              .add("page=1")
              .add("dimension=ou:USER_ORGUNIT,GxdhnY5wmHq")
              .add("eventDate=2022Sep");

      // When
      ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

      // Then
      // 1. Validate Response Structure (Counts, Headers, Height/Width)
      //    This helper checks basic counts and dimensions, adapting based on the runtime
      // 'expectPostgis' flag.
      validateResponseStructure(
          response,
          expectPostgis,
          422,
          4,
          4); // Pass runtime flag, row count, and expected header counts

      // 2. Extract Headers into a List of Maps for easy access by name
      List<Map<String, Object>> actualHeaders =
          response.extractList("headers", Map.class).stream()
              .map(obj -> (Map<String, Object>) obj) // Ensure correct type
              .collect(Collectors.toList());

      // 3. Assert metaData.
      String expectedMetaData =
          "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"pe\":{\"uid\":\"pe\",\"dimensionType\":\"PERIOD\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"2022Sep\":{\"uid\":\"2022Sep\",\"code\":\"2022Sep\",\"name\":\"September 2022 - August 2023\",\"description\":\"2022Sep\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-09-01T00:00:00.000\",\"endDate\":\"2023-08-31T00:00:00.000\"},\"GxdhnY5wmHq\":{\"uid\":\"GxdhnY5wmHq\",\"name\":\"Average weight (g)\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"eventdate\":{\"name\":\"Event date\"}},\"dimensions\":{\"GxdhnY5wmHq\":[],\"eventdate\":[\"2022Sep\"],\"ou\":[\"ImspTQPwCqd\"]}}";
      String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
      assertEquals(expectedMetaData, actualMetaData, false);

      // 4. Validate Headers By Name (conditionally checking PostGIS headers).
      validateHeaderPropertiesByName(
          response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "ou",
          "Organisation unit",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "GxdhnY5wmHq",
          "Average weight (g)",
          "NUMBER",
          "java.lang.Double",
          false,
          true);

      // rowContext not found or empty in the response, skipping assertions.
      // Validate row exists with values from original row index 0
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "2574"));

      // Validate row exists with values from original row index 21
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "2707"));

      // Validate row exists with values from original row index 42
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "2823.5"));

      // Validate row exists with values from original row index 63
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "2893.5"));

      // Validate row exists with values from original row index 84
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "2961"));

      // Validate row exists with values from original row index 105
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3014"));

      // Validate row exists with values from original row index 126
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3063"));

      // Validate row exists with values from original row index 147
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3100"));

      // Validate row exists with values from original row index 168
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3149"));

      // Validate row exists with values from original row index 189
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3197.5"));

      // Validate row exists with values from original row index 210
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3241"));

      // Validate row exists with values from original row index 231
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3277.5"));

      // Validate row exists with values from original row index 252
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3315.5"));

      // Validate row exists with values from original row index 273
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3346.5"));

      // Validate row exists with values from original row index 294
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3405"));

      // Validate row exists with values from original row index 315
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3467.5"));

      // Validate row exists with values from original row index 336
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3502"));

      // Validate row exists with values from original row index 357
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3567"));

      // Validate row exists with values from original row index 378
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3625"));

      // Validate row exists with values from original row index 399
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3700"));

      // Validate row exists with values from original row index 420
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3877"));

      // Validate row exists with values from original row index 421
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3908.5"));
    }

    @Test
    @DisplayName(
        "Enrollments Aggregate - Financial Year 2022 Sep - Time field: EVENT_DATE as dimension")
    public void financialYear2022WithEventDateAsDimension() throws JSONException {
      // Read the 'expect.postgis' system property at runtime to adapt assertions.
      boolean expectPostgis = isPostgres();

      // Given
      QueryParamsBuilder params =
          new QueryParamsBuilder()
              .add("includeMetadataDetails=true")
              .add("displayProperty=NAME")
              .add("totalPages=false")
              .add("rowContext=true")
              .add("pageSize=100")
              .add("page=1")
              .add("dimension=ou:USER_ORGUNIT,GxdhnY5wmHq,EVENT_DATE:2022Sep");

      // When
      ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

      // Then
      // 1. Validate Response Structure (Counts, Headers, Height/Width)
      //    This helper checks basic counts and dimensions, adapting based on the runtime
      // 'expectPostgis' flag.
      validateResponseStructure(
          response,
          expectPostgis,
          422,
          4,
          4); // Pass runtime flag, row count, and expected header counts

      // 2. Extract Headers into a List of Maps for easy access by name
      List<Map<String, Object>> actualHeaders =
          response.extractList("headers", Map.class).stream()
              .map(obj -> (Map<String, Object>) obj) // Ensure correct type
              .collect(Collectors.toList());

      // 3. Assert metaData.
      String expectedMetaData =
          "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"pe\":{\"uid\":\"pe\",\"dimensionType\":\"PERIOD\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"2022Sep\":{\"uid\":\"2022Sep\",\"code\":\"2022Sep\",\"name\":\"September 2022 - August 2023\",\"description\":\"2022Sep\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-09-01T00:00:00.000\",\"endDate\":\"2023-08-31T00:00:00.000\"},\"GxdhnY5wmHq\":{\"uid\":\"GxdhnY5wmHq\",\"name\":\"Average weight (g)\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"eventdate\":{\"name\":\"Event date\"}},\"dimensions\":{\"GxdhnY5wmHq\":[],\"eventdate\":[\"2022Sep\"],\"ou\":[\"ImspTQPwCqd\"]}}";
      String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
      assertEquals(expectedMetaData, actualMetaData, false);

      // 4. Validate Headers By Name (conditionally checking PostGIS headers).
      validateHeaderPropertiesByName(
          response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "ou",
          "Organisation unit",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "eventdate",
          "Event date",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "GxdhnY5wmHq",
          "Average weight (g)",
          "NUMBER",
          "java.lang.Double",
          false,
          true);

      // rowContext not found or empty in the response, skipping assertions.

      // 7. Assert row existence by value (unsorted results - validates all columns).
      // Validate row exists with values from original row index 0
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "2574"));

      // Validate row exists with values from original row index 21
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "2707"));

      // Validate row exists with values from original row index 42
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "2823.5"));

      // Validate row exists with values from original row index 63
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "2893.5"));

      // Validate row exists with values from original row index 84
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "2961"));

      // Validate row exists with values from original row index 105
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3014"));

      // Validate row exists with values from original row index 126
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3063"));

      // Validate row exists with values from original row index 147
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3100"));

      // Validate row exists with values from original row index 168
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3149"));

      // Validate row exists with values from original row index 189
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3197.5"));

      // Validate row exists with values from original row index 210
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3241"));

      // Validate row exists with values from original row index 231
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3277.5"));

      // Validate row exists with values from original row index 252
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3315.5"));

      // Validate row exists with values from original row index 273
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3346.5"));

      // Validate row exists with values from original row index 294
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3405"));

      // Validate row exists with values from original row index 315
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3467.5"));

      // Validate row exists with values from original row index 336
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3502"));

      // Validate row exists with values from original row index 357
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3567"));

      // Validate row exists with values from original row index 378
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3625"));

      // Validate row exists with values from original row index 399
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3700"));

      // Validate row exists with values from original row index 420
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3877"));

      // Validate row exists with values from original row index 421
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "1", "ou", "ImspTQPwCqd", "eventdate", "2022Sep", "GxdhnY5wmHq", "3908.5"));
    }
  }

  @Nested
  @DisplayName("Enrollment date")
  class EnrollmentDate {
    @Test
    @DisplayName("Enrollments Aggregate - Financial Year 2022 Sep - Time field: enrollmentDate")
    public void financialYear2022WithEnrollmentDate() throws JSONException {
      // Read the 'expect.postgis' system property at runtime to adapt assertions.
      boolean expectPostgis = isPostgres();

      // Given
      QueryParamsBuilder params =
          new QueryParamsBuilder()
              .add("includeMetadataDetails=true")
              .add("displayProperty=NAME")
              .add("totalPages=false")
              .add("enrollmentDate=2022Sep")
              .add("rowContext=true")
              .add("pageSize=100")
              .add("outputType=ENROLLMENT")
              .add("page=1")
              .add("dimension=ou:USER_ORGUNIT,GxdhnY5wmHq");

      // When
      ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

      // Then
      // 1. Validate Response Structure (Counts, Headers, Height/Width)
      //    This helper checks basic counts and dimensions, adapting based on the runtime
      // 'expectPostgis' flag.
      validateResponseStructure(
          response,
          expectPostgis,
          2497,
          4,
          4); // Pass runtime flag, row count, and expected header counts

      // 2. Extract Headers into a List of Maps for easy access by name
      List<Map<String, Object>> actualHeaders =
          response.extractList("headers", Map.class).stream()
              .map(obj -> (Map<String, Object>) obj) // Ensure correct type
              .collect(Collectors.toList());

      // 3. Assert metaData.
      String expectedMetaData =
          "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"pe\":{\"uid\":\"pe\",\"dimensionType\":\"PERIOD\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"2022Sep\":{\"uid\":\"2022Sep\",\"code\":\"2022Sep\",\"name\":\"September 2022 - August 2023\",\"description\":\"2022Sep\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-09-01T00:00:00.000\",\"endDate\":\"2023-08-31T00:00:00.000\"},\"GxdhnY5wmHq\":{\"uid\":\"GxdhnY5wmHq\",\"name\":\"Average weight (g)\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"enrollmentdate\":{\"name\":\"Date of enrollment\"}},\"dimensions\":{\"enrollmentdate\":[\"2022Sep\"],\"GxdhnY5wmHq\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
      String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
      assertEquals(expectedMetaData, actualMetaData, false);

      // 4. Validate Headers By Name (conditionally checking PostGIS headers).
      validateHeaderPropertiesByName(
          response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "ou",
          "Organisation unit",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "enrollmentdate",
          "Date of enrollment",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "GxdhnY5wmHq",
          "Average weight (g)",
          "NUMBER",
          "java.lang.Double",
          false,
          true);

      // rowContext not found or empty in the response, skipping assertions.

      // 7. Assert row existence by value (unsorted results - validates all columns).
      // Validate row exists with values from original row index 0
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "1", "ou", "ImspTQPwCqd", "enrollmentdate", "2022Sep", "GxdhnY5wmHq", "12"));

      // Validate row exists with values from original row index 86
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "1",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "2608.5"));

      // Validate row exists with values from original row index 172
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "2",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "2676"));

      // Validate row exists with values from original row index 258
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "4",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "2731.5"));

      // Validate row exists with values from original row index 344
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "1",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "2781.5"));

      // Validate row exists with values from original row index 430
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "3",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "2831"));

      // Validate row exists with values from original row index 516
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "4",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "2878"));

      // Validate row exists with values from original row index 602
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "6",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "2925"));

      // Validate row exists with values from original row index 688
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "7",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "2969.5"));

      // Validate row exists with values from original row index 774
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "4",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3013.5"));

      // Validate row exists with values from original row index 860
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "1",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3056.5"));

      // Validate row exists with values from original row index 946
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "5",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3099.5"));

      // Validate row exists with values from original row index 1032
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "5",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3142.5"));

      // Validate row exists with values from original row index 1118
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "5",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3187.5"));

      // Validate row exists with values from original row index 1204
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "10",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3230.5"));

      // Validate row exists with values from original row index 1290
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "4",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3273.5"));

      // Validate row exists with values from original row index 1376
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "4",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3316.5"));

      // Validate row exists with values from original row index 1462
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "7",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3360"));

      // Validate row exists with values from original row index 1548
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "5",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3403.5"));

      // Validate row exists with values from original row index 1634
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "5",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3447"));

      // Validate row exists with values from original row index 1720
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "5",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3491"));

      // Validate row exists with values from original row index 1806
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "2",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3534.5"));

      // Validate row exists with values from original row index 1892
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "1",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3578"));

      // Validate row exists with values from original row index 1978
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "4",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3623.5"));

      // Validate row exists with values from original row index 2064
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "3",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3668"));

      // Validate row exists with values from original row index 2150
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "2",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3715.5"));

      // Validate row exists with values from original row index 2236
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "2",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3768"));

      // Validate row exists with values from original row index 2322
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "1",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3824"));

      // Validate row exists with values from original row index 2408
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "2",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3891"));

      // Validate row exists with values from original row index 2494
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "1",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "4210"));

      // Validate row exists with values from original row index 2496
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "4", "ou", "ImspTQPwCqd", "enrollmentdate", "2022Sep", "GxdhnY5wmHq", ""));
    }
  }

  @Nested
  @DisplayName("Incident date")
  class IncidentDate {
    @Test
    @DisplayName("Enrollments Aggregate - Financial Year 2022 Sep - Time field: incidentDate")
    public void financialYear2022WithIncidentDate() throws JSONException {
      // Read the 'expect.postgis' system property at runtime to adapt assertions.
      boolean expectPostgis = isPostgres();

      // Given
      QueryParamsBuilder params =
          new QueryParamsBuilder()
              .add("includeMetadataDetails=true")
              .add("displayProperty=NAME")
              .add("totalPages=false")
              .add("rowContext=true")
              .add("pageSize=100")
              .add("outputType=ENROLLMENT")
              .add("page=1")
              .add("incidentDate=2022Sep")
              .add("dimension=ou:USER_ORGUNIT,GxdhnY5wmHq");

      // When
      ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);
      // Then
      // 1. Validate Response Structure (Counts, Headers, Height/Width)
      //    This helper checks basic counts and dimensions, adapting based on the runtime
      // 'expectPostgis' flag.
      validateResponseStructure(
          response,
          expectPostgis,
          2497,
          4,
          4); // Pass runtime flag, row count, and expected header counts

      // 2. Extract Headers into a List of Maps for easy access by name
      List<Map<String, Object>> actualHeaders =
          response.extractList("headers", Map.class).stream()
              .map(obj -> (Map<String, Object>) obj) // Ensure correct type
              .collect(Collectors.toList());

      // 3. Assert metaData.
      String expectedMetaData =
          "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"pe\":{\"uid\":\"pe\",\"dimensionType\":\"PERIOD\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"2022Sep\":{\"uid\":\"2022Sep\",\"code\":\"2022Sep\",\"name\":\"September 2022 - August 2023\",\"description\":\"2022Sep\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-09-01T00:00:00.000\",\"endDate\":\"2023-08-31T00:00:00.000\"},\"GxdhnY5wmHq\":{\"uid\":\"GxdhnY5wmHq\",\"name\":\"Average weight (g)\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"incidentdate\":{\"name\":\"Date of birth\"}},\"dimensions\":{\"incidentdate\":[\"2022Sep\"],\"GxdhnY5wmHq\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
      String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
      assertEquals(expectedMetaData, actualMetaData, false);

      // 4. Validate Headers By Name (conditionally checking PostGIS headers).
      validateHeaderPropertiesByName(
          response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "ou",
          "Organisation unit",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "incidentdate",
          "Date of birth",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "GxdhnY5wmHq",
          "Average weight (g)",
          "NUMBER",
          "java.lang.Double",
          false,
          true);

      // rowContext not found or empty in the response, skipping assertions.

      // 7. Assert row existence by value (unsorted results - validates all columns).
      // Validate row exists with values from original row index 0
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "1", "ou", "ImspTQPwCqd", "incidentdate", "2022Sep", "GxdhnY5wmHq", "12"));

      // Validate row exists with values from original row index 86
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "1",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "2608.5"));

      // Validate row exists with values from original row index 172
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "2", "ou", "ImspTQPwCqd", "incidentdate", "2022Sep", "GxdhnY5wmHq", "2676"));

      // Validate row exists with values from original row index 258
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "4",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "2731.5"));

      // Validate row exists with values from original row index 344
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "1",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "2781.5"));

      // Validate row exists with values from original row index 430
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "3", "ou", "ImspTQPwCqd", "incidentdate", "2022Sep", "GxdhnY5wmHq", "2831"));

      // Validate row exists with values from original row index 516
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "4", "ou", "ImspTQPwCqd", "incidentdate", "2022Sep", "GxdhnY5wmHq", "2878"));

      // Validate row exists with values from original row index 602
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "6", "ou", "ImspTQPwCqd", "incidentdate", "2022Sep", "GxdhnY5wmHq", "2925"));

      // Validate row exists with values from original row index 688
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "7",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "2969.5"));

      // Validate row exists with values from original row index 774
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "4",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3013.5"));

      // Validate row exists with values from original row index 860
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "1",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3056.5"));

      // Validate row exists with values from original row index 946
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "5",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3099.5"));

      // Validate row exists with values from original row index 1032
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "5",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3142.5"));

      // Validate row exists with values from original row index 1118
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "5",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3187.5"));

      // Validate row exists with values from original row index 1204
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "10",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3230.5"));

      // Validate row exists with values from original row index 1290
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "4",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3273.5"));

      // Validate row exists with values from original row index 1376
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "4",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3316.5"));

      // Validate row exists with values from original row index 1462
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "7", "ou", "ImspTQPwCqd", "incidentdate", "2022Sep", "GxdhnY5wmHq", "3360"));

      // Validate row exists with values from original row index 1548
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "5",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3403.5"));

      // Validate row exists with values from original row index 1634
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "5", "ou", "ImspTQPwCqd", "incidentdate", "2022Sep", "GxdhnY5wmHq", "3447"));

      // Validate row exists with values from original row index 1720
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "5", "ou", "ImspTQPwCqd", "incidentdate", "2022Sep", "GxdhnY5wmHq", "3491"));

      // Validate row exists with values from original row index 1806
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "2",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3534.5"));

      // Validate row exists with values from original row index 1892
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "1", "ou", "ImspTQPwCqd", "incidentdate", "2022Sep", "GxdhnY5wmHq", "3578"));

      // Validate row exists with values from original row index 1978
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "4",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3623.5"));

      // Validate row exists with values from original row index 2064
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "3", "ou", "ImspTQPwCqd", "incidentdate", "2022Sep", "GxdhnY5wmHq", "3668"));

      // Validate row exists with values from original row index 2150
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "2",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3715.5"));

      // Validate row exists with values from original row index 2236
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "2", "ou", "ImspTQPwCqd", "incidentdate", "2022Sep", "GxdhnY5wmHq", "3768"));

      // Validate row exists with values from original row index 2322
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "1", "ou", "ImspTQPwCqd", "incidentdate", "2022Sep", "GxdhnY5wmHq", "3824"));

      // Validate row exists with values from original row index 2408
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "2", "ou", "ImspTQPwCqd", "incidentdate", "2022Sep", "GxdhnY5wmHq", "3891"));

      // Validate row exists with values from original row index 2494
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "1", "ou", "ImspTQPwCqd", "incidentdate", "2022Sep", "GxdhnY5wmHq", "4210"));

      // Validate row exists with values from original row index 2496
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "4", "ou", "ImspTQPwCqd", "incidentdate", "2022Sep", "GxdhnY5wmHq", ""));
    }

    @Test
    @DisplayName("Enrollments Aggregate - Financial Year 2022 Sep - dimension=INCIDENT_DATE")
    public void financialYear2022WithIncidentDateDate_incidentDateAsDimension()
        throws JSONException {
      // Read the 'expect.postgis' system property at runtime to adapt assertions.
      boolean expectPostgis = isPostgres();

      // Given
      QueryParamsBuilder params =
          new QueryParamsBuilder()
              .add("includeMetadataDetails=true")
              .add("displayProperty=NAME")
              .add("totalPages=false")
              .add("rowContext=true")
              .add("pageSize=100")
              .add("outputType=ENROLLMENT")
              .add("page=1")
              .add("incidentDate=2022Sep")
              .add("dimension=ou:USER_ORGUNIT,GxdhnY5wmHq,INCIDENT_DATE:2022Sep");

      // When
      ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

      // Then
      // 1. Validate Response Structure (Counts, Headers, Height/Width)
      //    This helper checks basic counts and dimensions, adapting based on the runtime
      // 'expectPostgis' flag.
      validateResponseStructure(
          response,
          expectPostgis,
          2497,
          4,
          4); // Pass runtime flag, row count, and expected header counts

      // 2. Extract Headers into a List of Maps for easy access by name
      List<Map<String, Object>> actualHeaders =
          response.extractList("headers", Map.class).stream()
              .map(obj -> (Map<String, Object>) obj) // Ensure correct type
              .collect(Collectors.toList());

      // 3. Assert metaData.
      String expectedMetaData =
          "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"pe\":{\"uid\":\"pe\",\"dimensionType\":\"PERIOD\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"2022Sep\":{\"uid\":\"2022Sep\",\"code\":\"2022Sep\",\"name\":\"September 2022 - August 2023\",\"description\":\"2022Sep\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-09-01T00:00:00.000\",\"endDate\":\"2023-08-31T00:00:00.000\"},\"GxdhnY5wmHq\":{\"uid\":\"GxdhnY5wmHq\",\"name\":\"Average weight (g)\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"incidentdate\":{\"name\":\"Date of birth\"}},\"dimensions\":{\"incidentdate\":[\"2022Sep\"],\"GxdhnY5wmHq\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
      String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
      assertEquals(expectedMetaData, actualMetaData, false);

      // 4. Validate Headers By Name (conditionally checking PostGIS headers).
      validateHeaderPropertiesByName(
          response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "ou",
          "Organisation unit",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "incidentdate",
          "Date of birth",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "GxdhnY5wmHq",
          "Average weight (g)",
          "NUMBER",
          "java.lang.Double",
          false,
          true);

      // rowContext not found or empty in the response, skipping assertions.

      // 7. Assert row existence by value (unsorted results - validates all columns).
      // Validate row exists with values from original row index 0
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "1", "ou", "ImspTQPwCqd", "incidentdate", "2022Sep", "GxdhnY5wmHq", "12"));

      // Validate row exists with values from original row index 86
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "1",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "2608.5"));

      // Validate row exists with values from original row index 172
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "2", "ou", "ImspTQPwCqd", "incidentdate", "2022Sep", "GxdhnY5wmHq", "2676"));

      // Validate row exists with values from original row index 258
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "4",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "2731.5"));

      // Validate row exists with values from original row index 344
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "1",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "2781.5"));

      // Validate row exists with values from original row index 430
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "3", "ou", "ImspTQPwCqd", "incidentdate", "2022Sep", "GxdhnY5wmHq", "2831"));

      // Validate row exists with values from original row index 516
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "4", "ou", "ImspTQPwCqd", "incidentdate", "2022Sep", "GxdhnY5wmHq", "2878"));

      // Validate row exists with values from original row index 602
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "6", "ou", "ImspTQPwCqd", "incidentdate", "2022Sep", "GxdhnY5wmHq", "2925"));

      // Validate row exists with values from original row index 688
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "7",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "2969.5"));

      // Validate row exists with values from original row index 774
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "4",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3013.5"));

      // Validate row exists with values from original row index 860
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "1",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3056.5"));

      // Validate row exists with values from original row index 946
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "5",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3099.5"));

      // Validate row exists with values from original row index 1032
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "5",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3142.5"));

      // Validate row exists with values from original row index 1118
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "5",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3187.5"));

      // Validate row exists with values from original row index 1204
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "10",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3230.5"));

      // Validate row exists with values from original row index 1290
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "4",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3273.5"));

      // Validate row exists with values from original row index 1376
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "4",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3316.5"));

      // Validate row exists with values from original row index 1462
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "7", "ou", "ImspTQPwCqd", "incidentdate", "2022Sep", "GxdhnY5wmHq", "3360"));

      // Validate row exists with values from original row index 1548
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "5",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3403.5"));

      // Validate row exists with values from original row index 1634
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "5", "ou", "ImspTQPwCqd", "incidentdate", "2022Sep", "GxdhnY5wmHq", "3447"));

      // Validate row exists with values from original row index 1720
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "5", "ou", "ImspTQPwCqd", "incidentdate", "2022Sep", "GxdhnY5wmHq", "3491"));

      // Validate row exists with values from original row index 1806
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "2",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3534.5"));

      // Validate row exists with values from original row index 1892
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "1", "ou", "ImspTQPwCqd", "incidentdate", "2022Sep", "GxdhnY5wmHq", "3578"));

      // Validate row exists with values from original row index 1978
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "4",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3623.5"));

      // Validate row exists with values from original row index 2064
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "3", "ou", "ImspTQPwCqd", "incidentdate", "2022Sep", "GxdhnY5wmHq", "3668"));

      // Validate row exists with values from original row index 2150
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value",
              "2",
              "ou",
              "ImspTQPwCqd",
              "incidentdate",
              "2022Sep",
              "GxdhnY5wmHq",
              "3715.5"));

      // Validate row exists with values from original row index 2236
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "2", "ou", "ImspTQPwCqd", "incidentdate", "2022Sep", "GxdhnY5wmHq", "3768"));

      // Validate row exists with values from original row index 2322
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "1", "ou", "ImspTQPwCqd", "incidentdate", "2022Sep", "GxdhnY5wmHq", "3824"));

      // Validate row exists with values from original row index 2408
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "2", "ou", "ImspTQPwCqd", "incidentdate", "2022Sep", "GxdhnY5wmHq", "3891"));

      // Validate row exists with values from original row index 2494
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "1", "ou", "ImspTQPwCqd", "incidentdate", "2022Sep", "GxdhnY5wmHq", "4210"));

      // Validate row exists with values from original row index 2496
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "4", "ou", "ImspTQPwCqd", "incidentdate", "2022Sep", "GxdhnY5wmHq", ""));
    }
  }

  @Nested
  @DisplayName("Created")
  class Created {

    @Test
    @DisplayName("Enrollments Aggregate - Financial Year 2022 Sep - dimension=CREATED")
    public void financialYear2022WithCreated() throws JSONException {
      // Read the 'expect.postgis' system property at runtime to adapt assertions.
      boolean expectPostgis = isPostgres();

      // Given
      QueryParamsBuilder params =
          new QueryParamsBuilder()
              .add("includeMetadataDetails=true")
              .add("displayProperty=NAME")
              .add("totalPages=false")
              .add("rowContext=true")
              .add("pageSize=100")
              .add("outputType=ENROLLMENT")
              .add("page=1")
              .add("dimension=CREATED:2016Sep,ou:USER_ORGUNIT,GxdhnY5wmHq");

      // When
      ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

      // Then
      // 1. Validate Response Structure (Counts, Headers, Height/Width)
      //    This helper checks basic counts and dimensions, adapting based on the runtime
      // 'expectPostgis' flag.
      validateResponseStructure(
          response,
          expectPostgis,
          2805,
          4,
          4); // Pass runtime flag, row count, and expected header counts

      // 2. Extract Headers into a List of Maps for easy access by name
      List<Map<String, Object>> actualHeaders =
          response.extractList("headers", Map.class).stream()
              .map(obj -> (Map<String, Object>) obj) // Ensure correct type
              .collect(Collectors.toList());

      // 3. Assert metaData.
      String expectedMetaData =
          "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"pe\":{\"uid\":\"pe\",\"dimensionType\":\"PERIOD\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"created\":{\"name\":\"Created\"},\"2016Sep\":{\"uid\":\"2016Sep\",\"code\":\"2016Sep\",\"name\":\"September 2016 - August 2017\",\"description\":\"2016Sep\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2016-09-01T00:00:00.000\",\"endDate\":\"2017-08-31T00:00:00.000\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"GxdhnY5wmHq\":{\"uid\":\"GxdhnY5wmHq\",\"name\":\"Average weight (g)\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"GxdhnY5wmHq\":[],\"created\":[\"2016Sep\"],\"ou\":[\"ImspTQPwCqd\"]}}";
      String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
      assertEquals(expectedMetaData, actualMetaData, false);

      // 4. Validate Headers By Name (conditionally checking PostGIS headers).
      validateHeaderPropertiesByName(
          response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "ou",
          "Organisation unit",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response, actualHeaders, "created", "Created", "TEXT", "java.lang.String", false, true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "GxdhnY5wmHq",
          "Average weight (g)",
          "NUMBER",
          "java.lang.Double",
          false,
          true);

      // rowContext not found or empty in the response, skipping assertions.

      // 7. Assert row existence by value (unsorted results - validates all columns).
      // Validate row exists with values from original row index 0
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "created", "2016Sep", "GxdhnY5wmHq", "12"));

      // Validate row exists with values from original row index 96
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "2", "ou", "ImspTQPwCqd", "created", "2016Sep", "GxdhnY5wmHq", "2582.5"));

      // Validate row exists with values from original row index 192
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "created", "2016Sep", "GxdhnY5wmHq", "2636"));

      // Validate row exists with values from original row index 288
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "created", "2016Sep", "GxdhnY5wmHq", "2687.5"));

      // Validate row exists with values from original row index 384
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "4", "ou", "ImspTQPwCqd", "created", "2016Sep", "GxdhnY5wmHq", "2735.5"));

      // Validate row exists with values from original row index 480
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "5", "ou", "ImspTQPwCqd", "created", "2016Sep", "GxdhnY5wmHq", "2784"));

      // Validate row exists with values from original row index 576
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "11", "ou", "ImspTQPwCqd", "created", "2016Sep", "GxdhnY5wmHq", "2832"));

      // Validate row exists with values from original row index 672
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "5", "ou", "ImspTQPwCqd", "created", "2016Sep", "GxdhnY5wmHq", "2880.5"));

      // Validate row exists with values from original row index 768
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "11", "ou", "ImspTQPwCqd", "created", "2016Sep", "GxdhnY5wmHq", "2928.5"));

      // Validate row exists with values from original row index 864
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "7", "ou", "ImspTQPwCqd", "created", "2016Sep", "GxdhnY5wmHq", "2976.5"));

      // Validate row exists with values from original row index 960
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "6", "ou", "ImspTQPwCqd", "created", "2016Sep", "GxdhnY5wmHq", "3024.5"));

      // Validate row exists with values from original row index 1056
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "11", "ou", "ImspTQPwCqd", "created", "2016Sep", "GxdhnY5wmHq", "3072.5"));

      // Validate row exists with values from original row index 1152
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "6", "ou", "ImspTQPwCqd", "created", "2016Sep", "GxdhnY5wmHq", "3120.5"));

      // Validate row exists with values from original row index 1248
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "12", "ou", "ImspTQPwCqd", "created", "2016Sep", "GxdhnY5wmHq", "3168.5"));

      // Validate row exists with values from original row index 1344
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "13", "ou", "ImspTQPwCqd", "created", "2016Sep", "GxdhnY5wmHq", "3216.5"));

      // Validate row exists with values from original row index 1440
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "15", "ou", "ImspTQPwCqd", "created", "2016Sep", "GxdhnY5wmHq", "3264.5"));

      // Validate row exists with values from original row index 1536
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "10", "ou", "ImspTQPwCqd", "created", "2016Sep", "GxdhnY5wmHq", "3312.5"));

      // Validate row exists with values from original row index 1632
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "7", "ou", "ImspTQPwCqd", "created", "2016Sep", "GxdhnY5wmHq", "3360.5"));

      // Validate row exists with values from original row index 1728
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "15", "ou", "ImspTQPwCqd", "created", "2016Sep", "GxdhnY5wmHq", "3408.5"));

      // Validate row exists with values from original row index 1824
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "14", "ou", "ImspTQPwCqd", "created", "2016Sep", "GxdhnY5wmHq", "3456.5"));

      // Validate row exists with values from original row index 1920
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "5", "ou", "ImspTQPwCqd", "created", "2016Sep", "GxdhnY5wmHq", "3504.5"));

      // Validate row exists with values from original row index 2016
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "10", "ou", "ImspTQPwCqd", "created", "2016Sep", "GxdhnY5wmHq", "3552.5"));

      // Validate row exists with values from original row index 2112
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "15", "ou", "ImspTQPwCqd", "created", "2016Sep", "GxdhnY5wmHq", "3600.5"));

      // Validate row exists with values from original row index 2208
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "4", "ou", "ImspTQPwCqd", "created", "2016Sep", "GxdhnY5wmHq", "3648.5"));

      // Validate row exists with values from original row index 2304
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "6", "ou", "ImspTQPwCqd", "created", "2016Sep", "GxdhnY5wmHq", "3696.5"));

      // Validate row exists with values from original row index 2400
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "4", "ou", "ImspTQPwCqd", "created", "2016Sep", "GxdhnY5wmHq", "3745.5"));

      // Validate row exists with values from original row index 2496
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "created", "2016Sep", "GxdhnY5wmHq", "3794.5"));

      // Validate row exists with values from original row index 2592
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "3", "ou", "ImspTQPwCqd", "created", "2016Sep", "GxdhnY5wmHq", "3843.5"));

      // Validate row exists with values from original row index 2688
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "2", "ou", "ImspTQPwCqd", "created", "2016Sep", "GxdhnY5wmHq", "3899"));

      // Validate row exists with values from original row index 2784
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "2", "ou", "ImspTQPwCqd", "created", "2016Sep", "GxdhnY5wmHq", "3967"));

      // Validate row exists with values from original row index 2804
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "7", "ou", "ImspTQPwCqd", "created", "2016Sep", "GxdhnY5wmHq", ""));
    }
  }

  @Nested
  @DisplayName("Completed")
  class Completed {

    @Test
    @DisplayName("Enrollments Aggregate - Financial Year 2022 Sep - dimension=COMPLETED")
    public void financialYear2022WithCompleted() throws JSONException {
      // Read the 'expect.postgis' system property at runtime to adapt assertions.
      boolean expectPostgis = isPostgres();

      // Given
      QueryParamsBuilder params =
          new QueryParamsBuilder()
              .add("includeMetadataDetails=true")
              .add("displayProperty=NAME")
              .add("totalPages=false")
              .add("rowContext=true")
              .add("pageSize=100")
              .add("outputType=ENROLLMENT")
              .add("page=1")
              .add("dimension=COMPLETED:2022Sep,ou:USER_ORGUNIT,GxdhnY5wmHq");

      // When
      ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

      // Then
      // 1. Validate Response Structure (Counts, Headers, Height/Width)
      //    This helper checks basic counts and dimensions, adapting based on the runtime
      // 'expectPostgis' flag.
      validateResponseStructure(
          response,
          expectPostgis,
          5,
          4,
          4); // Pass runtime flag, row count, and expected header counts

      // 2. Extract Headers into a List of Maps for easy access by name
      List<Map<String, Object>> actualHeaders =
          response.extractList("headers", Map.class).stream()
              .map(obj -> (Map<String, Object>) obj) // Ensure correct type
              .collect(Collectors.toList());

      // 3. Assert metaData.
      String expectedMetaData =
          "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"pe\":{\"uid\":\"pe\",\"dimensionType\":\"PERIOD\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"2022Sep\":{\"uid\":\"2022Sep\",\"code\":\"2022Sep\",\"name\":\"September 2022 - August 2023\",\"description\":\"2022Sep\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-09-01T00:00:00.000\",\"endDate\":\"2023-08-31T00:00:00.000\"},\"GxdhnY5wmHq\":{\"uid\":\"GxdhnY5wmHq\",\"name\":\"Average weight (g)\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"completed\":{\"name\":\"Completed\"},\"completeddate\":{\"name\":\"Completed date\"}},\"dimensions\":{\"completed\":[\"2022Sep\"],\"GxdhnY5wmHq\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
      String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
      assertEquals(expectedMetaData, actualMetaData, false);

      // 4. Validate Headers By Name (conditionally checking PostGIS headers).
      validateHeaderPropertiesByName(
          response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "ou",
          "Organisation unit",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "completed",
          "Completed",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "GxdhnY5wmHq",
          "Average weight (g)",
          "NUMBER",
          "java.lang.Double",
          false,
          true);

      // rowContext not found or empty in the response, skipping assertions.

      // 7. Assert row existence by value (unsorted results - validates all columns).
      // Validate row exists with values from original row index 0
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "completed", "2022Sep", "GxdhnY5wmHq", "2327"));

      // Validate row exists with values from original row index 2
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "value", "1", "ou", "ImspTQPwCqd", "completed", "2022Sep", "GxdhnY5wmHq", "3387.5"));

      // Validate row exists with values from original row index 4
      validateRowExists(
          response,
          actualHeaders,
          Map.of("value", "1", "ou", "ImspTQPwCqd", "completed", "2022Sep", "GxdhnY5wmHq", "4817"));
    }
  }

  @Test
  public void dataElementOrgUnitTypeFilterDimensionAndOrgUnitAsFilter() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:USER_ORGUNIT,pe:202202")
            .add("includeMetadataDetails=true")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=100")
            .add("page=1")
            .add(
                "dimension=PFDfvmGpsR3.n1rtSHYf6O6:IN:g8upMTyEZGZ;USER_ORGUNIT;USER_ORGUNIT_GRANDCHILDREN");

    // When
    ApiResponse response = actions.aggregate().get("WSGAb5XwJ3Y", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"USER_ORGUNIT_GRANDCHILDREN\":{\"organisationUnits\":[\"nV3OkyzF4US\",\"r06ohri9wA9\",\"Z9QaI6sxTwW\",\"A3Fh37HWBWE\",\"DBs6e2Oxaj1\",\"sxRd2XOzFbz\",\"CG4QD1HC3h4\",\"j0Mtr3xTMjM\",\"YuQRtpLP10I\",\"QwMiPiME3bA\",\"iEkBZnMDarP\",\"KSdZwrU7Hh6\",\"g5ptsn0SFX8\",\"y5X4mP5XylL\",\"USQdmvrHh1Q\",\"KXSqt7jv6DU\",\"xGMGhjA3y6J\",\"yu4N82FFeLm\",\"vn9KJsLyP5f\",\"LsYpCyYxSLY\",\"EYt6ThQDagn\",\"npWGUj37qDe\",\"HWjrSuoNPte\",\"nlt6j60tCHF\",\"VCtF1DbspR5\",\"l7pFejMtUoF\",\"XEyIRFd9pct\",\"xhyjU2SVewz\",\"lYIM1MXbSYS\",\"pRHGAROvuyI\",\"NqWaKXcg01b\",\"BD9gU0GKlr2\",\"RzKeCma9qb1\",\"iUauWFeH8Qp\",\"ENHOJz3UH5L\",\"PrJQHI6q7w2\",\"HV8RTzgcFH3\",\"LfTkc0S4b5k\",\"NNE0YMCDZkO\",\"ARZ4y5i4reU\",\"iGHlidSFdpu\",\"DmaLM8WYmWv\",\"RWvG1aFrr0r\",\"QlCIp2S9NHs\",\"P69SId31eDp\",\"GWTIxJO9pRo\",\"M2qEv692lS6\",\"rXLor9Knq6l\",\"AovmOHadayb\",\"ajILkI0cfxn\",\"hjpHnHZIniP\",\"Qhmi8IZyPyD\",\"W5fN3G6y1VI\",\"GFk45MOxzJJ\",\"J4GiUImJZoE\",\"U09TSwIjG0s\",\"EjnIQNVAXGp\",\"JsxnA2IywRo\",\"Zoy23SSHCPs\",\"nOYt1LtFSyU\",\"vULnao2hV5v\",\"smoyi1iYNK6\",\"x4HaBHHwBML\",\"EVkm2xYcf6Z\",\"PaqugoqjRIj\",\"fwH9ipvXde9\",\"Lt8U7GVWvSR\",\"K1r3uF6eZ8n\",\"eV4cuxniZgP\",\"KIUCimTXf8Q\",\"hdEuw2ugkVF\",\"dGheVylzol6\",\"lY93YpCxJqf\",\"eROJsBwxQHt\",\"FRxcUEwktoV\",\"kvkDWg42lHR\",\"byp7w6Xd9Df\",\"vzup1f6ynON\",\"cM2BKSrj9F9\",\"l0ccv2yzfF3\",\"EfWCa0Cc8WW\",\"zSNUViKdkk3\",\"TQkG0sX9nca\",\"pmxZm7klXBy\",\"KctpIIucige\",\"C9uduqDZr9d\",\"XG8HGAbrbbL\",\"EB1zRKdYjdY\",\"gy8rmvYT4cj\",\"qgQ49DH9a0v\",\"hRZOIgQ0O1m\",\"daJPPxtIrQn\",\"pk7bUK5c1Uf\",\"qIRCo0MfuGb\",\"xIKjidMrico\",\"uKC54fzxRzO\",\"j43EZb15rjI\",\"TA7NvKjsn4A\",\"YpVol7asWvd\",\"BXJdOLvUrZB\",\"KKkLOTpMXGV\",\"YmmeuGbqOwR\",\"I4jWcnFmgEC\",\"fwxkctgmffZ\",\"jPidqyo7cpF\",\"r1RUyfVBkLp\",\"Mr4au3jR9bt\",\"U6Kr7Gtpidn\",\"EZPwuUTeIIG\",\"DfUfwjM9am5\",\"VGAFxBXz16y\",\"DxAPPqXvwLy\",\"QywkxFudXrC\",\"zFDYIgyGmXG\",\"qtr8GGlm4gg\",\"ERmBhYkhV6Y\",\"g8DdBm7EmUt\",\"CF243RPvNY7\",\"LhaAPLxdSFH\",\"N233eZJZ1bh\",\"JdhagCUEMbj\",\"WXnNDWTiE9r\",\"vWbkYPRmKyS\",\"XrF5AvaGcuw\",\"UhHipWG7J8b\",\"kbPmt60yi0L\",\"eNtRuQrrZeo\",\"Jiyc4ekaMMh\",\"L8iA6eLwKNb\",\"fRLX08WHWpL\",\"BmYyh9bZ0sr\",\"BGGmAwx33dj\",\"e1eIKM1GIF3\",\"bQiBfA2j5cw\",\"OTFepb1k9Db\",\"cgOy0hRMGu9\",\"FlBemv1NfEC\",\"RndxKqQGzUl\",\"vEvs2ckGNQj\",\"DNRAeXT9IwS\",\"aWQTfvgPA5v\",\"JdqfYTIFZXN\",\"myQ4q1W6B4y\",\"X7dWcGerQIm\",\"VP397wRvePm\",\"ZiOVcrSjSYe\",\"PQZJPIpTepd\",\"kU8vhUkAGaT\",\"Pc3JTyqnsmL\",\"GE25DpSrqpB\",\"d9iMR1MpuIO\",\"jWSIbtKfURj\"]},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"PFDfvmGpsR3\":{\"uid\":\"PFDfvmGpsR3\",\"name\":\"Care at birth\",\"description\":\"Intrapartum care \\/ Childbirth \\/ Labour and delivery\"},\"bbKtnxRZKEP\":{\"uid\":\"bbKtnxRZKEP\",\"name\":\"Postpartum care visit\",\"description\":\"Provision of care for the mother for some weeks after delivery\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"PUZaKR0Jh2k\":{\"uid\":\"PUZaKR0Jh2k\",\"name\":\"Previous deliveries\",\"description\":\"Table for recording earlier deliveries\"},\"edqlbukwRfQ\":{\"uid\":\"edqlbukwRfQ\",\"name\":\"Second antenatal care visit\",\"description\":\"Antenatal care visit\"},\"WZbXY0S00lP\":{\"uid\":\"WZbXY0S00lP\",\"name\":\"First antenatal care visit\",\"description\":\"First antenatal care visit\"},\"WSGAb5XwJ3Y\":{\"uid\":\"WSGAb5XwJ3Y\",\"name\":\"WHO RMNCH Tracker\"},\"PFDfvmGpsR3.n1rtSHYf6O6\":{\"uid\":\"n1rtSHYf6O6\",\"name\":\"WHOMCH Hospital \\/ Birth clinic\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"ou\":[\"ImspTQPwCqd\"],\"PFDfvmGpsR3.n1rtSHYf6O6\":[\"g8upMTyEZGZ\",\"ImspTQPwCqd\",\"YuQRtpLP10I\",\"vWbkYPRmKyS\",\"dGheVylzol6\",\"zFDYIgyGmXG\",\"BGGmAwx33dj\",\"YmmeuGbqOwR\",\"daJPPxtIrQn\",\"U6Kr7Gtpidn\",\"JdhagCUEMbj\",\"kU8vhUkAGaT\",\"I4jWcnFmgEC\",\"KctpIIucige\",\"sxRd2XOzFbz\",\"npWGUj37qDe\",\"ARZ4y5i4reU\",\"fwH9ipvXde9\",\"KKkLOTpMXGV\",\"e1eIKM1GIF3\",\"BXJdOLvUrZB\",\"hRZOIgQ0O1m\",\"eV4cuxniZgP\",\"lY93YpCxJqf\",\"L8iA6eLwKNb\",\"XG8HGAbrbbL\",\"WXnNDWTiE9r\",\"UhHipWG7J8b\",\"j43EZb15rjI\",\"Qhmi8IZyPyD\",\"ENHOJz3UH5L\",\"EB1zRKdYjdY\",\"iUauWFeH8Qp\",\"DNRAeXT9IwS\",\"XEyIRFd9pct\",\"VCtF1DbspR5\",\"aWQTfvgPA5v\",\"HV8RTzgcFH3\",\"VP397wRvePm\",\"g8DdBm7EmUt\",\"cgOy0hRMGu9\",\"CG4QD1HC3h4\",\"lYIM1MXbSYS\",\"KSdZwrU7Hh6\",\"JsxnA2IywRo\",\"j0Mtr3xTMjM\",\"hjpHnHZIniP\",\"cM2BKSrj9F9\",\"GE25DpSrqpB\",\"yu4N82FFeLm\",\"ERmBhYkhV6Y\",\"DxAPPqXvwLy\",\"pmxZm7klXBy\",\"bQiBfA2j5cw\",\"LfTkc0S4b5k\",\"byp7w6Xd9Df\",\"kbPmt60yi0L\",\"qIRCo0MfuGb\",\"QywkxFudXrC\",\"xGMGhjA3y6J\",\"FlBemv1NfEC\",\"r06ohri9wA9\",\"y5X4mP5XylL\",\"myQ4q1W6B4y\",\"QlCIp2S9NHs\",\"eROJsBwxQHt\",\"KXSqt7jv6DU\",\"K1r3uF6eZ8n\",\"EYt6ThQDagn\",\"jWSIbtKfURj\",\"hdEuw2ugkVF\",\"x4HaBHHwBML\",\"uKC54fzxRzO\",\"U09TSwIjG0s\",\"KIUCimTXf8Q\",\"A3Fh37HWBWE\",\"vzup1f6ynON\",\"l7pFejMtUoF\",\"X7dWcGerQIm\",\"Mr4au3jR9bt\",\"Lt8U7GVWvSR\",\"iEkBZnMDarP\",\"vEvs2ckGNQj\",\"OTFepb1k9Db\",\"GFk45MOxzJJ\",\"J4GiUImJZoE\",\"VGAFxBXz16y\",\"PaqugoqjRIj\",\"XrF5AvaGcuw\",\"EZPwuUTeIIG\",\"CF243RPvNY7\",\"ajILkI0cfxn\",\"Zoy23SSHCPs\",\"TQkG0sX9nca\",\"GWTIxJO9pRo\",\"kvkDWg42lHR\",\"LhaAPLxdSFH\",\"EjnIQNVAXGp\",\"DmaLM8WYmWv\",\"qgQ49DH9a0v\",\"g5ptsn0SFX8\",\"iGHlidSFdpu\",\"M2qEv692lS6\",\"FRxcUEwktoV\",\"jPidqyo7cpF\",\"nOYt1LtFSyU\",\"RndxKqQGzUl\",\"vULnao2hV5v\",\"USQdmvrHh1Q\",\"LsYpCyYxSLY\",\"Z9QaI6sxTwW\",\"Jiyc4ekaMMh\",\"nV3OkyzF4US\",\"xIKjidMrico\",\"W5fN3G6y1VI\",\"gy8rmvYT4cj\",\"AovmOHadayb\",\"DBs6e2Oxaj1\",\"TA7NvKjsn4A\",\"Pc3JTyqnsmL\",\"ZiOVcrSjSYe\",\"vn9KJsLyP5f\",\"pRHGAROvuyI\",\"fRLX08WHWpL\",\"JdqfYTIFZXN\",\"RWvG1aFrr0r\",\"EfWCa0Cc8WW\",\"HWjrSuoNPte\",\"PrJQHI6q7w2\",\"RzKeCma9qb1\",\"eNtRuQrrZeo\",\"zSNUViKdkk3\",\"QwMiPiME3bA\",\"YpVol7asWvd\",\"BD9gU0GKlr2\",\"DfUfwjM9am5\",\"nlt6j60tCHF\",\"N233eZJZ1bh\",\"d9iMR1MpuIO\",\"NqWaKXcg01b\",\"pk7bUK5c1Uf\",\"P69SId31eDp\",\"BmYyh9bZ0sr\",\"smoyi1iYNK6\",\"fwxkctgmffZ\",\"PQZJPIpTepd\",\"l0ccv2yzfF3\",\"rXLor9Knq6l\",\"EVkm2xYcf6Z\",\"r1RUyfVBkLp\",\"xhyjU2SVewz\",\"NNE0YMCDZkO\",\"C9uduqDZr9d\",\"qtr8GGlm4gg\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "PFDfvmGpsR3.n1rtSHYf6O6",
        "WHOMCH Hospital / Birth clinic",
        "ORGANISATION_UNIT",
        "org.hisp.dhis.organisationunit.OrganisationUnit",
        false,
        true);

    // Assert PostGIS-specific headers DO NOT exist if 'expectPostgis' is false
    if (!expectPostgis) {
      validateHeaderExistence(actualHeaders, "geometry", false);
      validateHeaderExistence(actualHeaders, "longitude", false);
      validateHeaderExistence(actualHeaders, "latitude", false);
    }

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name (sample validation: first/last row, key columns).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "value", "1");
    validateRowValueByName(response, actualHeaders, 0, "PFDfvmGpsR3.n1rtSHYf6O6", "g8upMTyEZGZ");
  }

  @Test
  public void orgUnitFilter() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:ImspTQPwCqd")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("dimension=pe:LAST_10_YEARS,A03MvHHogjR.a3kGcGDCuk6,ZzYYXq4fJie.pOe0ogW4OWd")
            .add("relativePeriodDate=2025-07-01");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        37,
        4,
        4); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"A03MvHHogjR.a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"2023\":{\"name\":\"2023\"},\"2022\":{\"name\":\"2022\"},\"2021\":{\"name\":\"2021\"},\"2020\":{\"name\":\"2020\"},\"2019\":{\"name\":\"2019\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"lbb3GURUxGo\":{\"code\":\"3\",\"name\":\"Dose 3\"},\"2018\":{\"name\":\"2018\"},\"2017\":{\"name\":\"2017\"},\"2016\":{\"name\":\"2016\"},\"2015\":{\"name\":\"2015\"},\"pe\":{},\"HEjqfmniZAr\":{\"code\":\"1\",\"name\":\"Dose 1\"},\"2024\":{\"name\":\"2024\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"ZzYYXq4fJie.pOe0ogW4OWd\":{\"name\":\"MCH DPT dose\"},\"RqLFM2C8RnE\":{\"code\":\"2\",\"name\":\"Dose 2\"}},\"dimensions\":{\"A03MvHHogjR.a3kGcGDCuk6\":[],\"pOe0ogW4OWd\":[\"HEjqfmniZAr\",\"RqLFM2C8RnE\",\"lbb3GURUxGo\"],\"pe\":[\"2015\",\"2016\",\"2017\",\"2018\",\"2019\",\"2020\",\"2021\",\"2022\",\"2023\",\"2024\"],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeaderPropertiesByName(
        response, actualHeaders, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.a3kGcGDCuk6",
        "MCH Apgar Score",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ZzYYXq4fJie.pOe0ogW4OWd",
        "MCH DPT dose",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // Assert PostGIS-specific headers DO NOT exist if 'expectPostgis' is false
    if (!expectPostgis) {
      validateHeaderExistence(actualHeaders, "geometry", false);
      validateHeaderExistence(actualHeaders, "longitude", false);
      validateHeaderExistence(actualHeaders, "latitude", false);
    }

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "1239",
            "A03MvHHogjR.a3kGcGDCuk6", "2",
            "ZzYYXq4fJie.pOe0ogW4OWd", "1"));
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "827",
            "A03MvHHogjR.a3kGcGDCuk6", "1",
            "ZzYYXq4fJie.pOe0ogW4OWd", "1"));
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "3",
            "A03MvHHogjR.a3kGcGDCuk6", "8",
            "ZzYYXq4fJie.pOe0ogW4OWd", "2"));
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "887",
            "A03MvHHogjR.a3kGcGDCuk6", "0",
            "ZzYYXq4fJie.pOe0ogW4OWd", "2"));
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "846",
            "A03MvHHogjR.a3kGcGDCuk6", "2",
            "ZzYYXq4fJie.pOe0ogW4OWd", "2"));
  }

  @Test
  public void dataElementOrgUnitTypeFilter() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=PFDfvmGpsR3.n1rtSHYf6O6:IN:ImspTQPwCqd")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("dimension=ou:ImspTQPwCqd,pe:LAST_5_YEARS")
            .add("relativePeriodDate=2025-07-01");

    // When
    ApiResponse response = actions.aggregate().get("WSGAb5XwJ3Y", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        0,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"bbKtnxRZKEP\":{\"name\":\"Postpartum care visit\"},\"ou\":{\"name\":\"Organisation unit\"},\"edqlbukwRfQ\":{\"name\":\"Second antenatal care visit\"},\"2023\":{\"name\":\"2023\"},\"2022\":{\"name\":\"2022\"},\"2021\":{\"name\":\"2021\"},\"2020\":{\"name\":\"2020\"},\"PFDfvmGpsR3.n1rtSHYf6O6\":{\"name\":\"WHOMCH Hospital \\/ Birth clinic\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"PFDfvmGpsR3\":{\"name\":\"Care at birth\"},\"pe\":{},\"2024\":{\"name\":\"2024\"},\"PUZaKR0Jh2k\":{\"name\":\"Previous deliveries\"},\"WZbXY0S00lP\":{\"name\":\"First antenatal care visit\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"}},\"dimensions\":{\"pe\":[\"2020\",\"2021\",\"2022\",\"2023\",\"2024\"],\"ou\":[\"ImspTQPwCqd\"],\"n1rtSHYf6O6\":[\"in ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ou",
        "Organisation unit",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "pe", "Period", "TEXT", "java.lang.String", false, true);

    // Assert PostGIS-specific headers DO NOT exist if 'expectPostgis' is false
    if (!expectPostgis) {
      validateHeaderExistence(actualHeaders, "geometry", false);
      validateHeaderExistence(actualHeaders, "longitude", false);
      validateHeaderExistence(actualHeaders, "latitude", false);
    }

    // No rows found in response, skipping row assertions.
  }
}
