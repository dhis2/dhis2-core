/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.analytics.tei;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import net.minidev.json.JSONObject;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.actions.analytics.AnalyticsTeiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Groups e2e tests for Tracked Entities "/query" endpoint.
 *
 * @author maikel arabori
 */
public class TrackedEntityQueryTest extends AnalyticsApiTest {
  private AnalyticsTeiActions analyticsTeiActions = new AnalyticsTeiActions();

  private QueryParamsBuilder withDefaultHeaders(QueryParamsBuilder queryParamsBuilder) {
    return queryParamsBuilder.add(
        "headers=trackedentityinstanceuid,"
            + "lastupdated,"
            + "createdbydisplayname,"
            + "lastupdatedbydisplayname,"
            + "geometry,"
            + "longitude,"
            + "latitude,"
            + "ouname,"
            + "oucode,"
            + "ounamehierarchy,"
            + "w75KJ2mc4zz,"
            + "zDhUuAYrxNC,"
            + "cejWyOfXge6,"
            + "lZGmxYbs97q");
  }

  @Test
  void queryWithProgramAndProgramStageWhenTotalPagesIsFalse() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=ou:ImspTQPwCqd")
            .add("program=IpHINAT79UW")
            .add("asc=w75KJ2mc4zz")
            .add("lastUpdated=LAST_YEAR")
            .add("totalPages=false")
            .add("pageSize=100")
            .add("page=1")
            .add("relativePeriodDate=2022-09-27");

    params = withDefaultHeaders(params);

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(14)))
        .body("rows", hasSize(equalTo(0)))
        .body("metaData.pager.page", equalTo(1))
        .body("metaData.pager.pageSize", equalTo(100))
        .body("metaData.pager.isLastPage", is(false))
        .body("metaData.pager", not(hasKey("total")))
        .body("metaData.pager", not(hasKey("pageCount")))
        .body("metaData.items.ImspTQPwCqd.name", equalTo("Sierra Leone"))
        .body("metaData.items.lZGmxYbs97q.name", equalTo("Unique ID"))
        .body("metaData.items.zDhUuAYrxNC.name", equalTo("Last name"))
        .body("metaData.items.IpHINAT79UW.name", equalTo("Child Programme"))
        .body("metaData.items.ZzYYXq4fJie.name", equalTo("Baby Postnatal"))
        .body("metaData.items.w75KJ2mc4zz.name", equalTo("First name"))
        .body("metaData.items.A03MvHHogjR.name", equalTo("Birth"))
        .body("metaData.items.cejWyOfXge6.name", equalTo("Gender"))
        .body("metaData.items.ou.name", equalTo("Organisation unit"))
        .body("metaData.dimensions", hasKey("lZGmxYbs97q"))
        .body("metaData.dimensions", hasKey("zDhUuAYrxNC"))
        .body("metaData.dimensions", hasKey("pe"))
        .body("metaData.dimensions", hasKey("w75KJ2mc4zz"))
        .body("metaData.dimensions", hasKey("cejWyOfXge6"))
        .body("metaData.dimensions.ou", hasSize(equalTo(1)))
        .body("metaData.dimensions.ou", hasItem("ImspTQPwCqd"))
        .body("height", equalTo(0))
        .body("width", equalTo(0))
        .body("headerWidth", equalTo(14));

    // Validate headers
    validateHeader(
        response,
        0,
        "trackedentityinstanceuid",
        "Tracked entity instance",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        1,
        "lastupdated",
        "Last updated",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(
        response, 2, "createdbydisplayname", "Created by", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        3,
        "lastupdatedbydisplayname",
        "Last updated by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(response, 4, "geometry", "Geometry", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 5, "longitude", "Longitude", "NUMBER", "java.lang.Double", false, true);
    validateHeader(response, 6, "latitude", "Latitude", "NUMBER", "java.lang.Double", false, true);
    validateHeader(
        response, 7, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 8, "oucode", "Organisation unit code", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        9,
        "ounamehierarchy",
        "Organisation unit hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response, 10, "w75KJ2mc4zz", "First name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 11, "zDhUuAYrxNC", "Last name", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 12, "cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 13, "lZGmxYbs97q", "Unique ID", "TEXT", "java.lang.String", false, true);
  }

  @Test
  void queryWithProgramOnly() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=IpHINAT79UW")
            .add("lastUpdated=LAST_10_YEARS")
            .add("asc=lastupdated")
            .add("relativePeriodDate=2022-09-27");

    params = withDefaultHeaders(params);

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(14)))
        .body("rows", hasSize(equalTo(50)))
        .body("metaData.pager.page", equalTo(1))
        .body("metaData.pager.pageSize", equalTo(50))
        .body("metaData.pager.isLastPage", is(false))
        .body("metaData.pager", not(hasKey("total")))
        .body("metaData.pager", not(hasKey("pageCount")))
        .body("metaData.items.ImspTQPwCqd.name", equalTo(null))
        .body("metaData.items.lZGmxYbs97q.name", equalTo("Unique ID"))
        .body("metaData.items.zDhUuAYrxNC.name", equalTo("Last name"))
        .body("metaData.items.IpHINAT79UW.name", equalTo("Child Programme"))
        .body("metaData.items.ZzYYXq4fJie.name", equalTo("Baby Postnatal"))
        .body("metaData.items.w75KJ2mc4zz.name", equalTo("First name"))
        .body("metaData.items.A03MvHHogjR.name", equalTo("Birth"))
        .body("metaData.items.cejWyOfXge6.name", equalTo("Gender"))
        .body("metaData.items.ou.name", equalTo(null))
        .body("metaData.dimensions", hasKey("lZGmxYbs97q"))
        .body("metaData.dimensions", hasKey("zDhUuAYrxNC"))
        .body("metaData.dimensions", hasKey("pe"))
        .body("metaData.dimensions", hasKey("w75KJ2mc4zz"))
        .body("metaData.dimensions", hasKey("cejWyOfXge6"))
        .body("metaData.dimensions", not(hasKey("ou")))
        .body("height", equalTo(50))
        .body("width", equalTo(14))
        .body("headerWidth", equalTo(14));

    // Validate headers
    validateHeader(
        response,
        0,
        "trackedentityinstanceuid",
        "Tracked entity instance",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        1,
        "lastupdated",
        "Last updated",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(
        response, 2, "createdbydisplayname", "Created by", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        3,
        "lastupdatedbydisplayname",
        "Last updated by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(response, 4, "geometry", "Geometry", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 5, "longitude", "Longitude", "NUMBER", "java.lang.Double", false, true);
    validateHeader(response, 6, "latitude", "Latitude", "NUMBER", "java.lang.Double", false, true);
    validateHeader(
        response, 7, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 8, "oucode", "Organisation unit code", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        9,
        "ounamehierarchy",
        "Organisation unit hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response, 10, "w75KJ2mc4zz", "First name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 11, "zDhUuAYrxNC", "Last name", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 12, "cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 13, "lZGmxYbs97q", "Unique ID", "TEXT", "java.lang.String", false, true);

    // Validate the first three rows, as samples.
    validateRow(
        response,
        0,
        List.of(
            "oi3PMIGYJH8",
            "2014-07-23 12:45:49.787",
            "",
            "",
            "",
            "",
            "",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "Evelyn",
            "Jackson",
            "Female",
            ""));

    validateRow(
        response,
        1,
        List.of(
            "mYyHxkNAOr2",
            "2014-09-23 20:01:44.961",
            "",
            "",
            "",
            "",
            "",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "John",
            "Thomson",
            "Female",
            ""));

    validateRow(
        response,
        2,
        List.of(
            "SBjuNw0Xtkn",
            "2014-10-01 12:27:37.837",
            "",
            "",
            "",
            "",
            "",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "Tom",
            "Johson",
            "",
            ""));
  }

  @Test
  void queryWithProgramAndPagination() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=IpHINAT79UW")
            .add("lastUpdated=LAST_10_YEARS")
            .add("pageSize=10")
            .add("totalPages=true")
            .add("asc=lastupdated")
            .add("relativePeriodDate=2022-09-27");

    params = withDefaultHeaders(params);

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(14)))
        .body("rows", hasSize(equalTo(10)))
        .body("metaData.pager.page", equalTo(1))
        .body("metaData.pager.pageSize", equalTo(10))
        .body("metaData.pager", not(hasKey("isLastPage")))
        .body("metaData.pager.total", equalTo(19023))
        .body("metaData.pager.pageCount", equalTo(1903))
        .body("metaData.items.ImspTQPwCqd.name", equalTo(null))
        .body("metaData.items.lZGmxYbs97q.name", equalTo("Unique ID"))
        .body("metaData.items.zDhUuAYrxNC.name", equalTo("Last name"))
        .body("metaData.items.IpHINAT79UW.name", equalTo("Child Programme"))
        .body("metaData.items.ZzYYXq4fJie.name", equalTo("Baby Postnatal"))
        .body("metaData.items.w75KJ2mc4zz.name", equalTo("First name"))
        .body("metaData.items.A03MvHHogjR.name", equalTo("Birth"))
        .body("metaData.items.cejWyOfXge6.name", equalTo("Gender"))
        .body("metaData.items.ou.name", equalTo(null))
        .body("metaData.dimensions", hasKey("lZGmxYbs97q"))
        .body("metaData.dimensions", hasKey("zDhUuAYrxNC"))
        .body("metaData.dimensions", hasKey("pe"))
        .body("metaData.dimensions", hasKey("w75KJ2mc4zz"))
        .body("metaData.dimensions", hasKey("cejWyOfXge6"))
        .body("metaData.dimensions", not(hasKey("ou")))
        .body("height", equalTo(10))
        .body("width", equalTo(14))
        .body("headerWidth", equalTo(14));

    // Validate headers
    validateHeader(
        response,
        0,
        "trackedentityinstanceuid",
        "Tracked entity instance",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        1,
        "lastupdated",
        "Last updated",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(
        response, 2, "createdbydisplayname", "Created by", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        3,
        "lastupdatedbydisplayname",
        "Last updated by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(response, 4, "geometry", "Geometry", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 5, "longitude", "Longitude", "NUMBER", "java.lang.Double", false, true);
    validateHeader(response, 6, "latitude", "Latitude", "NUMBER", "java.lang.Double", false, true);
    validateHeader(
        response, 7, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 8, "oucode", "Organisation unit code", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        9,
        "ounamehierarchy",
        "Organisation unit hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response, 10, "w75KJ2mc4zz", "First name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 11, "zDhUuAYrxNC", "Last name", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 12, "cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 13, "lZGmxYbs97q", "Unique ID", "TEXT", "java.lang.String", false, true);

    // Validate the first three rows, as samples.
    validateRow(
        response,
        0,
        List.of(
            "oi3PMIGYJH8",
            "2014-07-23 12:45:49.787",
            "",
            "",
            "",
            "",
            "",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "Evelyn",
            "Jackson",
            "Female",
            ""));

    validateRow(
        response,
        1,
        List.of(
            "mYyHxkNAOr2",
            "2014-09-23 20:01:44.961",
            "",
            "",
            "",
            "",
            "",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "John",
            "Thomson",
            "Female",
            ""));

    validateRow(
        response,
        2,
        List.of(
            "SBjuNw0Xtkn",
            "2014-10-01 12:27:37.837",
            "",
            "",
            "",
            "",
            "",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "Tom",
            "Johson",
            "",
            ""));
  }

  @Test
  public void queryWithProgramAndManyParams() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=IpHINAT79UW")
            .add("displayProperty=NAME")
            .add("includeMetadataDetails=true")
            .add("showHierarchy=true")
            .add("hierarchyMeta=true")
            .add("dimension=cejWyOfXge6")
            .add("lastUpdated=LAST_10_YEARS")
            .add(
                "headers=ouname,cejWyOfXge6,w75KJ2mc4zz,trackedentityinstanceuid,lastupdated,oucode")
            .add("desc=lastupdated")
            .add("relativePeriodDate=2022-09-27");

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(6)))
        .body("rows", hasSize(equalTo(50)))
        .body("metaData.pager.page", equalTo(1))
        .body("metaData.pager.pageSize", equalTo(50))
        .body("metaData.pager.isLastPage", is(false))
        .body("metaData.pager", not(hasKey("total")))
        .body("metaData.pager", not(hasKey("pageCount")))
        .body("metaData.items.ImspTQPwCqd.name", equalTo(null))
        .body("metaData.items.lZGmxYbs97q.name", equalTo("Unique ID"))
        .body("metaData.items.zDhUuAYrxNC.name", equalTo("Last name"))
        .body("metaData.items.IpHINAT79UW.name", equalTo("Child Programme"))
        .body("metaData.items.ZzYYXq4fJie.name", equalTo("Baby Postnatal"))
        .body("metaData.items.w75KJ2mc4zz.name", equalTo("First name"))
        .body("metaData.items.A03MvHHogjR.name", equalTo("Birth"))
        .body("metaData.items.cejWyOfXge6.name", equalTo("Gender"))
        .body("metaData.items.ou.name", equalTo(null))
        .body("metaData.dimensions", hasKey("lZGmxYbs97q"))
        .body("metaData.dimensions", hasKey("zDhUuAYrxNC"))
        .body("metaData.dimensions", hasKey("pe"))
        .body("metaData.dimensions", hasKey("w75KJ2mc4zz"))
        .body("metaData.dimensions", hasKey("cejWyOfXge6"))
        .body("metaData.dimensions", not(hasKey("ou")))
        .body("height", equalTo(50))
        .body("width", equalTo(6))
        .body("headerWidth", equalTo(6));

    // Validate headers
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 2, "w75KJ2mc4zz", "First name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        3,
        "trackedentityinstanceuid",
        "Tracked entity instance",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        4,
        "lastupdated",
        "Last updated",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(
        response, 5, "oucode", "Organisation unit code", "TEXT", "java.lang.String", false, true);

    // Validate the first three rows, as samples.
    validateRow(
        response,
        0,
        List.of(
            "Ngelehun CHC",
            "Female",
            "Filona",
            "vOxUH373fy5",
            "2017-05-26 11:46:22.372",
            "OU_559"));

    validateRow(
        response,
        1,
        List.of(
            "Ngelehun CHC", "Male", "Frank", "lkuI9OgwfOc", "2017-01-20 10:41:45.624", "OU_559"));

    validateRow(
        response,
        2,
        List.of(
            "Ngelehun CHC",
            "Female",
            "Gertrude",
            "pybd813kIWx",
            "2017-01-20 10:40:31.913",
            "OU_559"));
  }

  @Test
  public void queryWithProgramDimensionAndFilter() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=IpHINAT79UW")
            .add("dimension=ouname,w75KJ2mc4zz:eq:James")
            .add("lastUpdated=LAST_10_YEARS")
            .add("includeMetadataDetails=false")
            .add("headers=ouname,w75KJ2mc4zz,lastupdated")
            .add("asc=lastupdated")
            .add("relativePeriodDate=2022-09-27");

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(50)))
        .body("metaData.pager.page", equalTo(1))
        .body("metaData.pager.pageSize", equalTo(50))
        .body("metaData.pager.isLastPage", is(false))
        .body("metaData.pager", not(hasKey("total")))
        .body("metaData.pager", not(hasKey("pageCount")))
        .body("metaData.items.ImspTQPwCqd.name", equalTo(null))
        .body("metaData.items.lZGmxYbs97q.name", equalTo("Unique ID"))
        .body("metaData.items.zDhUuAYrxNC.name", equalTo("Last name"))
        .body("metaData.items.IpHINAT79UW.name", equalTo("Child Programme"))
        .body("metaData.items.ZzYYXq4fJie.name", equalTo("Baby Postnatal"))
        .body("metaData.items.w75KJ2mc4zz.name", equalTo("First name"))
        .body("metaData.items.A03MvHHogjR.name", equalTo("Birth"))
        .body("metaData.items.cejWyOfXge6.name", equalTo("Gender"))
        .body("metaData.items.ou.name", equalTo(null))
        .body("metaData.dimensions", hasKey("lZGmxYbs97q"))
        .body("metaData.dimensions", hasKey("zDhUuAYrxNC"))
        .body("metaData.dimensions", hasKey("pe"))
        .body("metaData.dimensions", hasKey("w75KJ2mc4zz"))
        .body("metaData.dimensions", hasKey("cejWyOfXge6"))
        .body("metaData.dimensions", not(hasKey("ouname")))
        .body("height", equalTo(50))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Validate headers
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "w75KJ2mc4zz", "First name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        2,
        "lastupdated",
        "Last updated",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // Validate the first three rows, as samples.
    validateRow(response, 0, List.of("Ngelehun CHC", "James", "2014-11-15 21:19:09.35"));

    validateRow(response, 1, List.of("Bambara MCHP", "James", "2015-08-06 21:12:34.395"));

    validateRow(response, 2, List.of("Moyollo MCHP", "James", "2015-08-06 21:12:35.736"));
  }

  @Test
  public void queryWithProgramAndMultipleStaticDimOrdering() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=IpHINAT79UW")
            .add("desc=lastupdated,ouname")
            .add("headers=ouname,lastupdated")
            .add("lastUpdated=LAST_10_YEARS")
            .add("relativePeriodDate=2022-09-27");

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(2)))
        .body("rows", hasSize(equalTo(50)))
        .body("height", equalTo(50))
        .body("width", equalTo(2))
        .body("headerWidth", equalTo(2));

    // Validate headers
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        1,
        "lastupdated",
        "Last updated",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // Validate the first three rows, as samples.
    validateRow(response, 0, List.of("Ngelehun CHC", "2017-05-26 11:46:22.372"));

    validateRow(response, 1, List.of("Ngelehun CHC", "2017-01-20 10:41:45.624"));

    validateRow(response, 2, List.of("Ngelehun CHC", "2017-01-20 10:40:31.913"));
  }

  @Test
  public void queryWithProgramAndMultipleDynamicDimOrdering() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=IpHINAT79UW")
            .add("lastUpdated=LAST_10_YEARS")
            .add("desc=w75KJ2mc4zz,zDhUuAYrxNC")
            .add("headers=w75KJ2mc4zz,zDhUuAYrxNC")
            .add("relativePeriodDate=2022-09-27");

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(2)))
        .body("rows", hasSize(equalTo(50)))
        .body("height", equalTo(50))
        .body("width", equalTo(2))
        .body("headerWidth", equalTo(2));

    // Validate headers
    validateHeader(
        response, 0, "w75KJ2mc4zz", "First name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "zDhUuAYrxNC", "Last name", "TEXT", "java.lang.String", false, true);

    // Validate the first three rows, as samples.
    validateRow(response, 0, List.of("Willie", "Woods"));

    validateRow(response, 1, List.of("Willie", "Williams"));

    validateRow(response, 2, List.of("Willie", "Williams"));
  }

  @Test
  public void queryWithProgramAndEnrollmentStaticDimOrdering() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=IpHINAT79UW")
            .add("lastUpdated=LAST_10_YEARS")
            .add("desc=lastupdated,IpHINAT79UW.A03MvHHogjR.ouname")
            .add("headers=ouname,lZGmxYbs97q")
            .add("relativePeriodDate=2022-09-27");

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(2)))
        .body("rows", hasSize(equalTo(50)))
        .body("height", equalTo(50))
        .body("width", equalTo(2))
        .body("headerWidth", equalTo(2));

    // Validate headers
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "lZGmxYbs97q", "Unique ID", "TEXT", "java.lang.String", false, true);

    // Validate the first three rows, as samples.
    validateRow(response, 0, List.of("Ngelehun CHC", ""));

    validateRow(response, 1, List.of("Ngelehun CHC", ""));

    validateRow(response, 2, List.of("Ngelehun CHC", ""));
  }

  @Test
  public void queryWithProgramAndMultipleEventDimOrdering() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=IpHINAT79UW")
            .add("desc=IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6")
            .add("lastUpdated=LAST_10_YEARS")
            .add("headers=ouname,lZGmxYbs97q")
            .add("relativePeriodDate=2022-09-27");

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(2)))
        .body("rows", hasSize(equalTo(50)))
        .body("height", equalTo(50))
        .body("width", equalTo(2))
        .body("headerWidth", equalTo(2));

    // Validate headers
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "lZGmxYbs97q", "Unique ID", "TEXT", "java.lang.String", false, true);

    // Validate the first three rows, as samples.
    validateRow(response, 0, List.of("Ngelehun CHC", ""));

    validateRow(response, 1, List.of("Ngelehun CHC", ""));

    validateRow(response, 2, List.of("Ngelehun CHC", ""));
  }

  @Test
  public void queryWithProgramAndProgramIndicatorOrdering() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=IpHINAT79UW")
            .add("dimension=IpHINAT79UW.A03MvHHogjR.p2Zxg0wcPQ3")
            .add("lastUpdated=LAST_10_YEARS")
            .add("asc=IpHINAT79UW.A03MvHHogjR.p2Zxg0wcPQ3,zDhUuAYrxNC,w75KJ2mc4zz")
            .add("headers=ouname,lZGmxYbs97q,IpHINAT79UW.A03MvHHogjR.p2Zxg0wcPQ3")
            .add("relativePeriodDate=2022-09-27");

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(50)))
        .body("height", equalTo(50))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Validate headers
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "lZGmxYbs97q", "Unique ID", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        2,
        "IpHINAT79UW.A03MvHHogjR.p2Zxg0wcPQ3",
        "BCG doses",
        "NUMBER",
        "java.lang.Double",
        false,
        true);

    // Validate the first three rows, as samples.
    validateRow(response, 0, List.of("Tambaliabalia MCHP", "", "0.0"));

    validateRow(response, 1, List.of("Kathanta Bana MCHP", "", "0.0"));

    validateRow(response, 2, List.of("Sam Lean's MCHP", "", "0.0"));
  }

  @Test
  public void queryWithProgramAndFilterByOrgUnit() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=IpHINAT79UW")
            .add("dimension=ou:BV4IomHvri4")
            .add("lastUpdated=LAST_10_YEARS")
            .add("desc=lastupdated")
            .add("relativePeriodDate=2022-09-27");

    params = withDefaultHeaders(params);

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(14)))
        .body("rows", hasSize(equalTo(14)))
        .body("metaData.dimensions.ou", hasSize(equalTo(1)))
        .body("metaData.dimensions.ou", hasItem("BV4IomHvri4"))
        .body("height", equalTo(14))
        .body("width", equalTo(14))
        .body("headerWidth", equalTo(14));

    // Validate the first three rows, as samples.
    validateRow(
        response,
        0,
        List.of(
            "NYKMYcUHzSt",
            "2015-08-07 15:47:24.377",
            "",
            "",
            "",
            "",
            "",
            "Ahmadiyya Muslim Hospital",
            "OU_268246",
            "Sierra Leone / Tonkolili / Yoni / Ahmadiyya Muslim Hospital",
            "Angela",
            "Wright",
            "Female",
            ""));

    validateRow(
        response,
        1,
        List.of(
            "sM7XmpfgKFb",
            "2015-08-07 15:47:24.033",
            "",
            "",
            "",
            "",
            "",
            "Ahmadiyya Muslim Hospital",
            "OU_268246",
            "Sierra Leone / Tonkolili / Yoni / Ahmadiyya Muslim Hospital",
            "Brenda",
            "Morgan",
            "Female",
            ""));

    validateRow(
        response,
        2,
        List.of(
            "vFSQneulDLz",
            "2015-08-07 15:47:22.383",
            "",
            "",
            "",
            "",
            "",
            "Ahmadiyya Muslim Hospital",
            "OU_268246",
            "Sierra Leone / Tonkolili / Yoni / Ahmadiyya Muslim Hospital",
            "Edward",
            "Murray",
            "Male",
            ""));
  }

  @Test
  public void queryWithProgramAndFilterByMultipleOrgUnits() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=IpHINAT79UW")
            .add("dimension=ou:a04CZxe0PSe;a1dP5m3Clw4;a1E6QWBTEwX;a5glgtnXJRG")
            .add("lastUpdated=LAST_10_YEARS")
            .add("desc=lastupdated")
            .add("relativePeriodDate=2022-09-27");

    params = withDefaultHeaders(params);

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(14)))
        .body("rows", hasSize(equalTo(50)))
        .body("metaData.dimensions.ou", hasSize(equalTo(4)))
        .body("metaData.dimensions.ou", hasItem("a04CZxe0PSe"))
        .body("metaData.dimensions.ou", hasItem("a1dP5m3Clw4"))
        .body("metaData.dimensions.ou", hasItem("a1E6QWBTEwX"))
        .body("metaData.dimensions.ou", hasItem("a5glgtnXJRG"))
        .body("height", equalTo(50))
        .body("width", equalTo(14))
        .body("headerWidth", equalTo(14));

    // Validate the first three rows, as samples.
    validateRow(
        response,
        0,
        List.of(
            "giN9xZLKzOT",
            "2015-08-07 15:47:29.243",
            "",
            "",
            "",
            "",
            "",
            "Magbanabom MCHP",
            "OU_268177",
            "Sierra Leone / Tonkolili / Kunike / Magbanabom MCHP",
            "Jean",
            "Washington",
            "Female",
            ""));

    validateRow(
        response,
        1,
        List.of(
            "iP8ISoPBLaA",
            "2015-08-07 15:47:29.146",
            "",
            "",
            "",
            "",
            "",
            "Magbanabom MCHP",
            "OU_268177",
            "Sierra Leone / Tonkolili / Kunike / Magbanabom MCHP",
            "Brian",
            "Austin",
            "Male",
            ""));

    validateRow(
        response,
        2,
        List.of(
            "GZrFV0JMmSV",
            "2015-08-07 15:47:28.657",
            "",
            "",
            "",
            "",
            "",
            "Magbanabom MCHP",
            "OU_268177",
            "Sierra Leone / Tonkolili / Kunike / Magbanabom MCHP",
            "Robert",
            "Adams",
            "Male",
            ""));
  }

  @Test
  public void queryWithProgramAndFilterByMultipleOrgUnitsSelected() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=IpHINAT79UW")
            .add("dimension=ou:a04CZxe0PSe;a1dP5m3Clw4;a1E6QWBTEwX;a5glgtnXJRG")
            .add("lastUpdated=LAST_10_YEARS")
            .add("ouMode=SELECTED")
            .add("desc=lastupdated")
            .add("relativePeriodDate=2022-09-27");

    params = withDefaultHeaders(params);

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(14)))
        .body("rows", hasSize(equalTo(50)))
        .body("metaData.dimensions.ou", hasSize(equalTo(4)))
        .body("metaData.dimensions.ou", hasItem("a04CZxe0PSe"))
        .body("metaData.dimensions.ou", hasItem("a1dP5m3Clw4"))
        .body("metaData.dimensions.ou", hasItem("a1E6QWBTEwX"))
        .body("metaData.dimensions.ou", hasItem("a5glgtnXJRG"))
        .body("height", equalTo(50))
        .body("width", equalTo(14))
        .body("headerWidth", equalTo(14));

    // Validate the first three rows, as samples.
    validateRow(
        response,
        0,
        List.of(
            "giN9xZLKzOT",
            "2015-08-07 15:47:29.243",
            "",
            "",
            "",
            "",
            "",
            "Magbanabom MCHP",
            "OU_268177",
            "Sierra Leone / Tonkolili / Kunike / Magbanabom MCHP",
            "Jean",
            "Washington",
            "Female",
            ""));

    validateRow(
        response,
        1,
        List.of(
            "iP8ISoPBLaA",
            "2015-08-07 15:47:29.146",
            "",
            "",
            "",
            "",
            "",
            "Magbanabom MCHP",
            "OU_268177",
            "Sierra Leone / Tonkolili / Kunike / Magbanabom MCHP",
            "Brian",
            "Austin",
            "Male",
            ""));

    validateRow(
        response,
        2,
        List.of(
            "GZrFV0JMmSV",
            "2015-08-07 15:47:28.657",
            "",
            "",
            "",
            "",
            "",
            "Magbanabom MCHP",
            "OU_268177",
            "Sierra Leone / Tonkolili / Kunike / Magbanabom MCHP",
            "Robert",
            "Adams",
            "Male",
            ""));
  }

  @Test
  public void queryWithProgramAndFilterByMultipleOrgUnitsChildren() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=IpHINAT79UW")
            .add("dimension=ou:l0ccv2yzfF3;r06ohri9wA9;GWTIxJO9pRo")
            .add("lastUpdated=LAST_10_YEARS")
            .add("ouMode=CHILDREN")
            .add("desc=lastupdated")
            .add("relativePeriodDate=2022-09-27");

    params = withDefaultHeaders(params);

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(14)))
        .body("rows", hasSize(equalTo(50)))
        .body("metaData.dimensions.ou", hasSize(equalTo(3)))
        .body("metaData.dimensions.ou", hasItem("l0ccv2yzfF3"))
        .body("metaData.dimensions.ou", hasItem("r06ohri9wA9"))
        .body("metaData.dimensions.ou", hasItem("GWTIxJO9pRo"))
        .body("height", equalTo(50))
        .body("width", equalTo(14))
        .body("headerWidth", equalTo(14));

    // Validate the first three rows, as samples.
    validateRow(
        response,
        0,
        List.of(
            "FZUETGg4CqK",
            "2015-08-07 15:47:29.256",
            "",
            "",
            "",
            "",
            "",
            "Matholey MCHP",
            "OU_268180",
            "Sierra Leone / Tonkolili / Kunike / Matholey MCHP",
            "Willie",
            "Bailey",
            "Male",
            ""));

    validateRow(
        response,
        1,
        List.of(
            "giN9xZLKzOT",
            "2015-08-07 15:47:29.243",
            "",
            "",
            "",
            "",
            "",
            "Magbanabom MCHP",
            "OU_268177",
            "Sierra Leone / Tonkolili / Kunike / Magbanabom MCHP",
            "Jean",
            "Washington",
            "Female",
            ""));

    validateRow(
        response,
        2,
        List.of(
            "BQuVMcT4dcI",
            "2015-08-07 15:47:29.152",
            "",
            "",
            "",
            "",
            "",
            "Moribaya MCHP",
            "OU_211254",
            "Sierra Leone / Kambia / Samu / Moribaya MCHP",
            "Louise",
            "Reed",
            "Female",
            ""));
  }

  @Test
  public void queryWithProgramAndFilterByEnrollmentOrgUnit() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=IpHINAT79UW")
            .add("lastUpdated=LAST_10_YEARS")
            .add("dimension=IpHINAT79UW.ou:BV4IomHvri4")
            .add("desc=lastupdated")
            .add("relativePeriodDate=2022-09-27");

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(17)))
        .body("rows", hasSize(equalTo(14)))
        .body("metaData.dimensions.ou", hasSize(equalTo(1)))
        .body("metaData.dimensions.ou", hasItem("BV4IomHvri4"))
        .body("height", equalTo(14))
        .body("width", equalTo(17))
        .body("headerWidth", equalTo(17));

    // Validate the first three rows, as samples.
    validateRow(
        response,
        0,
        List.of(
            "NYKMYcUHzSt",
            "2015-08-07 15:47:24.377",
            "",
            "2015-08-07 15:47:24.376",
            "",
            "",
            "",
            "",
            "",
            "Ahmadiyya Muslim Hospital",
            "OU_268246",
            "Sierra Leone / Tonkolili / Yoni / Ahmadiyya Muslim Hospital",
            "Angela",
            "Wright",
            "Female",
            "",
            "BV4IomHvri4"));

    validateRow(
        response,
        1,
        List.of(
            "sM7XmpfgKFb",
            "2015-08-07 15:47:24.033",
            "",
            "2015-08-07 15:47:24.032",
            "",
            "",
            "",
            "",
            "",
            "Ahmadiyya Muslim Hospital",
            "OU_268246",
            "Sierra Leone / Tonkolili / Yoni / Ahmadiyya Muslim Hospital",
            "Brenda",
            "Morgan",
            "Female",
            "",
            "BV4IomHvri4"));

    validateRow(
        response,
        2,
        List.of(
            "vFSQneulDLz",
            "2015-08-07 15:47:22.383",
            "",
            "2015-08-07 15:47:22.383",
            "",
            "",
            "",
            "",
            "",
            "Ahmadiyya Muslim Hospital",
            "OU_268246",
            "Sierra Leone / Tonkolili / Yoni / Ahmadiyya Muslim Hospital",
            "Edward",
            "Murray",
            "Male",
            "",
            "BV4IomHvri4"));
  }

  @Test
  public void queryWithProgramAndFilterByEventOrgUnit() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=IpHINAT79UW")
            .add("dimension=IpHINAT79UW.A03MvHHogjR.ou:BV4IomHvri4")
            .add("lastUpdated=LAST_10_YEARS")
            .add("desc=lastupdated")
            .add("relativePeriodDate=2022-09-27");

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(17)))
        .body("rows", hasSize(equalTo(14)))
        .body("metaData.dimensions.ou", hasSize(equalTo(1)))
        .body("metaData.dimensions.ou", hasItem("BV4IomHvri4"))
        .body("height", equalTo(14))
        .body("width", equalTo(17))
        .body("headerWidth", equalTo(17));

    // Validate the first three rows, as samples.
    validateRow(
        response,
        0,
        List.of(
            "NYKMYcUHzSt",
            "2015-08-07 15:47:24.377",
            "",
            "2015-08-07 15:47:24.376",
            "",
            "",
            "",
            "",
            "",
            "Ahmadiyya Muslim Hospital",
            "OU_268246",
            "Sierra Leone / Tonkolili / Yoni / Ahmadiyya Muslim Hospital",
            "Angela",
            "Wright",
            "Female",
            "",
            "BV4IomHvri4"));

    validateRow(
        response,
        1,
        List.of(
            "sM7XmpfgKFb",
            "2015-08-07 15:47:24.033",
            "",
            "2015-08-07 15:47:24.032",
            "",
            "",
            "",
            "",
            "",
            "Ahmadiyya Muslim Hospital",
            "OU_268246",
            "Sierra Leone / Tonkolili / Yoni / Ahmadiyya Muslim Hospital",
            "Brenda",
            "Morgan",
            "Female",
            "",
            "BV4IomHvri4"));

    validateRow(
        response,
        2,
        List.of(
            "vFSQneulDLz",
            "2015-08-07 15:47:22.383",
            "",
            "2015-08-07 15:47:22.383",
            "",
            "",
            "",
            "",
            "",
            "Ahmadiyya Muslim Hospital",
            "OU_268246",
            "Sierra Leone / Tonkolili / Yoni / Ahmadiyya Muslim Hospital",
            "Edward",
            "Murray",
            "Male",
            "",
            "BV4IomHvri4"));
  }

  @Test
  public void queryWithProgramAndFilterByEventDate() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=IpHINAT79UW")
            .add("eventDate=IpHINAT79UW.A03MvHHogjR.LAST_YEAR")
            .add("desc=lastupdated,oucode")
            .add("relativePeriodDate=2022-09-27");

    params = withDefaultHeaders(params);

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(14)))
        .body("rows", hasSize(equalTo(50)))
        .body("metaData.dimensions", not(hasKey("ou")))
        .body("metaData.dimensions", hasKey("pe"))
        .body("metaData.dimensions.pe", hasSize(equalTo(1)))
        .body("metaData.dimensions.pe", hasItem("2021"))
        .body("metaData.items.pe.name", equalTo("Period"))
        .body("metaData.items.2021.name", equalTo("2021"))
        .body("metaData.items.LAST_YEAR.name", equalTo("Last year"))
        .body("height", equalTo(50))
        .body("width", equalTo(14))
        .body("headerWidth", equalTo(14));

    // Validate the first three rows, as samples.
    validateRow(
        response,
        0,
        List.of(
            "EaOyKGOIGRp",
            "2016-08-03 23:47:14.517",
            "",
            "",
            "",
            "",
            "",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "Anna",
            "Jones",
            "Female",
            ""));

    validateRow(
        response,
        1,
        List.of(
            "lSxhGlVaTvy",
            "2016-04-21 16:01:20.435",
            "",
            "",
            "",
            "",
            "",
            "Masoko MCHP",
            "OU_268158",
            "Sierra Leone / Tonkolili / Kholifa Rowalla / Masoko MCHP",
            "Diane",
            "Bryant",
            "Female",
            ""));

    validateRow(
        response,
        2,
        List.of(
            "xgrOXoHRoZC",
            "2015-08-06 21:20:52.781",
            "",
            "",
            "",
            "",
            "",
            "Govt. Hospital Moyamba",
            "OU_247056",
            "Sierra Leone / Moyamba / Kaiyamba / Govt. Hospital Moyamba",
            "Randy",
            "Reyes",
            "Male",
            ""));
  }

  @Test
  public void queryWithProgramAndFilterByEnrollmentDate() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=IpHINAT79UW")
            .add("enrollmentDate=IpHINAT79UW.LAST_5_YEARS")
            .add("desc=lastupdated")
            .add("relativePeriodDate=2023-09-27");

    params = withDefaultHeaders(params);

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(14)))
        .body("rows", hasSize(equalTo(50)))
        .body("metaData.dimensions", not(hasKey("ou")))
        .body("metaData.dimensions", hasKey("pe"))
        .body("metaData.dimensions.pe", hasSize(equalTo(5)))
        .body("metaData.dimensions.pe", hasItem("2018"))
        .body("metaData.dimensions.pe", hasItem("2019"))
        .body("metaData.dimensions.pe", hasItem("2020"))
        .body("metaData.dimensions.pe", hasItem("2021"))
        .body("metaData.dimensions.pe", hasItem("2022"))
        .body("metaData.items.pe.name", equalTo("Period"))
        .body("metaData.items.2018.name", equalTo("2018"))
        .body("metaData.items.2019.name", equalTo("2019"))
        .body("metaData.items.2020.name", equalTo("2020"))
        .body("metaData.items.2021.name", equalTo("2021"))
        .body("metaData.items.2022.name", equalTo("2022"))
        .body("metaData.items.LAST_5_YEARS.name", equalTo("Last 5 years"))
        .body("height", equalTo(50))
        .body("width", equalTo(14))
        .body("headerWidth", equalTo(14));

    // Validate the first three rows, as samples.
    validateRow(
        response,
        0,
        List.of(
            "PQfMcpmXeFE",
            "2016-08-03 23:49:43.309",
            "",
            "",
            "",
            "",
            "",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "John",
            "Kelly",
            "Male",
            ""));

    validateRow(
        response,
        1,
        List.of(
            "EaOyKGOIGRp",
            "2016-08-03 23:47:14.517",
            "",
            "",
            "",
            "",
            "",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "Anna",
            "Jones",
            "Female",
            ""));

    validateRow(
        response,
        2,
        List.of(
            "lSxhGlVaTvy",
            "2016-04-21 16:01:20.435",
            "",
            "",
            "",
            "",
            "",
            "Masoko MCHP",
            "OU_268158",
            "Sierra Leone / Tonkolili / Kholifa Rowalla / Masoko MCHP",
            "Diane",
            "Bryant",
            "Female",
            ""));
  }

  @Test
  public void queryWithProgramAndFilterLastUpdatedDate() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=IpHINAT79UW")
            .add("lastUpdated=LAST_5_YEARS")
            .add("desc=lastupdated")
            .add("relativePeriodDate=2022-09-27");

    params = withDefaultHeaders(params);

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(14)))
        .body("rows", hasSize(equalTo(3)))
        .body("metaData.dimensions", not(hasKey("ou")))
        .body("metaData.dimensions", hasKey("pe"))
        .body("metaData.dimensions.pe", hasSize(equalTo(5)))
        .body("metaData.dimensions.pe", hasItem("2017"))
        .body("metaData.dimensions.pe", hasItem("2018"))
        .body("metaData.dimensions.pe", hasItem("2019"))
        .body("metaData.dimensions.pe", hasItem("2020"))
        .body("metaData.dimensions.pe", hasItem("2021"))
        .body("metaData.items.pe.name", equalTo("Period"))
        .body("metaData.items.2017.name", equalTo("2017"))
        .body("metaData.items.2018.name", equalTo("2018"))
        .body("metaData.items.2019.name", equalTo("2019"))
        .body("metaData.items.2020.name", equalTo("2020"))
        .body("metaData.items.2021.name", equalTo("2021"))
        .body("metaData.items.LAST_5_YEARS.name", equalTo("Last 5 years"))
        .body("height", equalTo(3))
        .body("width", equalTo(14))
        .body("headerWidth", equalTo(14));

    // Validate the first three rows, as samples.
    validateRow(
        response,
        0,
        List.of(
            "vOxUH373fy5",
            "2017-05-26 11:46:22.372",
            "",
            "",
            "",
            "",
            "",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "Filona",
            "Ryder",
            "Female",
            ""));

    validateRow(
        response,
        1,
        List.of(
            "lkuI9OgwfOc",
            "2017-01-20 10:41:45.624",
            "",
            "",
            "",
            "",
            "",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "Frank",
            "Fjordsen",
            "Male",
            ""));

    validateRow(
        response,
        2,
        List.of(
            "pybd813kIWx",
            "2017-01-20 10:40:31.913",
            "",
            "",
            "",
            "",
            "",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "Gertrude",
            "Fjordsen",
            "Female",
            ""));
  }

  @Test
  public void queryWithProgramAndFilterByEventDataValue() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=IpHINAT79UW")
            .add("dimension=IpHINAT79UW.ZzYYXq4fJie.GQY2lXrypjO:eq:12")
            .add("lastUpdated=LAST_10_YEARS")
            .add("asc=lastupdated")
            .add("relativePeriodDate=2022-09-27");

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(17)))
        .body("rows", hasSize(equalTo(1)))
        .body("metaData.pager.page", equalTo(1))
        .body("metaData.pager.pageSize", equalTo(50))
        .body("metaData.pager.isLastPage", is(true))
        .body("metaData.pager", not(hasKey("total")))
        .body("metaData.pager", not(hasKey("pageCount")))
        .body("metaData.dimensions", not(hasKey("ou")))
        .body("metaData.dimensions", hasKey("pe"))
        .body("metaData.items.GQY2lXrypjO.name", equalTo("MCH Infant Weight  (g)"))
        .body("height", equalTo(1))
        .body("width", equalTo(17))
        .body("headerWidth", equalTo(17));

    validateRow(
        response,
        0,
        List.of(
            "SBjuNw0Xtkn",
            "2014-10-01 12:27:37.837",
            "",
            "2014-10-01 12:27:35.417",
            "",
            "",
            "",
            "",
            "",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "Tom",
            "Johson",
            "",
            "",
            "12"));
  }

  @Test
  public void queryWithProgramAndEnrollmentDateAndNegativeEnrollmentOffset() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=IpHINAT79UW")
            .add("enrollmentDate=IpHINAT79UW[-1].LAST_YEAR")
            .add("desc=lastupdated")
            .add("relativePeriodDate=2023-04-03")
            .add("headers=ouname,w75KJ2mc4zz,zDhUuAYrxNC");

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(50)))
        .body("height", equalTo(50))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Validate headers
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "w75KJ2mc4zz", "First name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 2, "zDhUuAYrxNC", "Last name", "TEXT", "java.lang.String", false, true);

    // Validate the first three rows, as samples.
    validateRow(response, 0, List.of("Ngelehun CHC", "John", "Kelly"));

    validateRow(response, 1, List.of("Ngelehun CHC", "Anna", "Jones"));

    validateRow(response, 2, List.of("Masoko MCHP", "Diane", "Bryant"));
  }

  @Test
  @Disabled("Fails in CI. Remove this annotation when test will be fixed")
  public void queryWithProgramAndEnrollmentDateAndPositiveEnrollmentOffset() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=IpHINAT79UW")
            .add("enrollmentDate=IpHINAT79UW[1].LAST_MONTH")
            .add("desc=lastupdated")
            .add("headers=ouname,IpHINAT79UW.w75KJ2mc4zz,IpHINAT79UW.zDhUuAYrxNC");

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(50)))
        .body("height", equalTo(50))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Validate headers
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        1,
        "IpHINAT79UW.w75KJ2mc4zz",
        "First name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        2,
        "IpHINAT79UW.zDhUuAYrxNC",
        "Last name",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // Validate the first three rows, as samples.
    validateRow(response, 0, List.of("Ngelehun CHC", "John", "Kelly"));

    validateRow(response, 1, List.of("Jangalor MCHP", "Antonio", "Ruiz"));

    validateRow(response, 2, List.of("Bureh MCHP", "Ralph", "Smith"));
  }

  @Test
  public void queryWithProgramAndEnrollmentDateOffset() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("headers=ouname,w75KJ2mc4zz,zDhUuAYrxNC")
            .add("enrollmentDate=IpHINAT79UW[0].LAST_YEAR")
            .add("program=IpHINAT79UW")
            .add("relativePeriodDate=2023-01-01");

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(50)))
        .body("height", equalTo(50))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":false},\"items\":{\"lZGmxYbs97q\":{\"name\":\"Unique ID\"},\"zDhUuAYrxNC\":{\"name\":\"Last name\"},\"pe\":{\"name\":\"Period\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"w75KJ2mc4zz\":{\"name\":\"First name\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"2022\":{\"name\":\"2022\"},\"LAST_YEAR\":{\"name\":\"Last year\"},\"cejWyOfXge6\":{\"name\":\"Gender\"}},\"dimensions\":{\"lZGmxYbs97q\":[],\"zDhUuAYrxNC\":[],\"pe\":[\"2022\"],\"w75KJ2mc4zz\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "w75KJ2mc4zz", "First name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 2, "zDhUuAYrxNC", "Last name", "TEXT", "java.lang.String", false, true);

    // Assert rows.
    validateRow(response, 0, List.of("Rokolon MCHP", "Justin", "Hayes"));
    validateRow(response, 1, List.of("Pejewa CHC", "Nancy", "Jones"));
    validateRow(response, 2, List.of("Gbangba MCHP", "James", "Jordan"));
    validateRow(response, 3, List.of("Massabendu CHP", "Jane", "Cox"));
    validateRow(response, 4, List.of("Mansundu MCHP", "Martha", "Harvey"));
    validateRow(response, 5, List.of("Ola During Clinic", "Gloria", "Campbell"));
    validateRow(response, 6, List.of("Hamdalai MCHP", "Nancy", "Garza"));
    validateRow(response, 7, List.of("SLRCS (Nongowa) clinic", "James", "Jones"));
    validateRow(response, 8, List.of("Kassama MCHP", "Alan", "Turner"));
    validateRow(response, 9, List.of("Yoyema MCHP", "Dennis", "Washington"));
    validateRow(response, 10, List.of("Mbaoma CHP", "Joseph", "Powell"));
    validateRow(response, 11, List.of("Kamakwie MCHP", "Paul", "Harper"));
    validateRow(response, 12, List.of("Yakaji MCHP", "Maria", "Hughes"));
    validateRow(response, 13, List.of("Futa CHC", "Diana", "Montgomery"));
    validateRow(response, 14, List.of("Njala University Hospital", "Carl", "Robertson"));
    validateRow(response, 15, List.of("Salima MCHP", "Anne", "Frazier"));
    validateRow(response, 16, List.of("Magbil MCHP", "Anthony", "Cook"));
    validateRow(response, 17, List.of("Ginger Hall Health Centre", "Evelyn", "Cook"));
    validateRow(response, 18, List.of("Kareneh MCHP", "Helen", "Wagner"));
    validateRow(response, 19, List.of("Gbonkomaria CHP", "Heather", "Fowler"));
    validateRow(response, 20, List.of("Sengama MCHP", "Andrea", "Tucker"));
    validateRow(response, 21, List.of("Jojoima CHC", "Diana", "Montgomery"));
    validateRow(response, 22, List.of("Futa CHC", "Phillip", "Mcdonald"));
    validateRow(response, 23, List.of("Deima MCHP", "Rachel", "Lopez"));
    validateRow(response, 24, List.of("Fullah Town (B.Sebora) MCHP", "Samuel", "Palmer"));
    validateRow(response, 25, List.of("Potoru CHC", "Russell", "Bailey"));
    validateRow(response, 26, List.of("Mafoimara MCHP", "Amanda", "Gordon"));
    validateRow(response, 27, List.of("Kamagbewu MCHP", "Lois", "Garcia"));
    validateRow(response, 28, List.of("St. Joseph's Clinic", "Randy", "Reyes"));
    validateRow(response, 29, List.of("Rokolon MCHP", "Carol", "Myers"));
    validateRow(response, 30, List.of("New Maforkie CHP", "Gerald", "Hill"));
    validateRow(response, 31, List.of("Rogbere CHC", "Daniel", "Turner"));
    validateRow(response, 32, List.of("Mambiama CHP", "Diana", "Jenkins"));
    validateRow(response, 33, List.of("Boajibu CHC", "Phillip", "Lewis"));
    validateRow(response, 34, List.of("Gbeworbu-Gao CHP", "Melissa", "Williamson"));
    validateRow(response, 35, List.of("Romeni MCHP", "Henry", "Cox"));
    validateRow(response, 36, List.of("Bathurst MCHP", "Benjamin", "Little"));
    validateRow(response, 37, List.of("Teko Barracks Clinic", "Ruby", "Dixon"));
    validateRow(response, 38, List.of("Jormu CHP", "Willie", "Little"));
    validateRow(response, 39, List.of("Kuranko MCHP", "Shawn", "Lewis"));
    validateRow(response, 40, List.of("Yorgbofore MCHP", "Ruby", "Burns"));
    validateRow(response, 41, List.of("Wallehun MCHP", "Patricia", "Grant"));
    validateRow(response, 42, List.of("Koindukura MCHP", "Catherine", "Parker"));
    validateRow(response, 43, List.of("Guala MCHP", "Patricia", "West"));
    validateRow(response, 44, List.of("Levuma Nyomeh CHP", "Angela", "Edwards"));
    validateRow(response, 45, List.of("Mogbuama MCHP", "Angela", "Roberts"));
    validateRow(response, 46, List.of("Approved School CHP", "William", "Ross"));
    validateRow(response, 47, List.of("Fadugu CHC", "Janice", "Ramos"));
    validateRow(response, 48, List.of("Hamdalai MCHP", "Keith", "Harris"));
    validateRow(response, 49, List.of("Mattru Jong MCHP", "Amy", "Stanley"));
  }

  @Test
  public void queryWithProgramAndDimensionFilterUsingIdSchemeCode() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=IpHINAT79UW")
            .add("dimension=IpHINAT79UW.ZzYYXq4fJie.cYGaxwK615G:IN:Negative-Conf")
            .add("desc=w75KJ2mc4zz,zDhUuAYrxNC")
            .add("relativePeriodDate=2016-01-01")
            .add("outputIdScheme=CODE");

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("rows", hasSize(equalTo(50)))
        .body("height", equalTo(50))
        .body("width", equalTo(17))
        .body("headerWidth", equalTo(17))
        .body("headers", hasSize(equalTo(17)))
        .body("metaData.pager.page", equalTo(1))
        .body("metaData.pager.pageSize", equalTo(50))
        .body("metaData.pager.isLastPage", is(false))
        .body("metaData.pager", not(hasKey("total")))
        .body("metaData.pager", not(hasKey("pageCount")))
        .body("metaData.dimensions", not(hasKey("ou")))
        .body("metaData.dimensions", hasKey("pe"));

    // Validate the first three rows, as samples.

    validateRow(
        response,
        0,
        List.of(
            "acCGrc3qlji",
            "2015-08-06 21:12:36.226",
            "",
            "2015-08-06 21:12:36.226",
            "",
            "",
            "",
            "",
            "",
            "Plantain Island MCHP",
            "OU_247071",
            "Sierra Leone / Moyamba / Kargboro / Plantain Island MCHP",
            "Willie",
            "Wallace",
            "Male",
            "",
            "Negative-Conf"));

    validateRow(
        response,
        1,
        List.of(
            "yG1PQX6xCkK",
            "2015-08-07 15:47:23.061",
            "",
            "2015-08-07 15:47:23.06",
            "",
            "",
            "",
            "",
            "",
            "Yankasa MCHP",
            "OU_193257",
            "Sierra Leone / Bombali / Makari Gbanti / Yankasa MCHP",
            "Willie",
            "Stewart",
            "Male",
            "",
            "Negative-Conf"));

    validateRow(
        response,
        2,
        List.of(
            "cr0DjId1xhO",
            "2015-08-06 21:20:47.468",
            "",
            "2015-08-06 21:20:47.467",
            "",
            "",
            "",
            "",
            "",
            "Bumpeh Perri CHC",
            "OU_260423",
            "Sierra Leone / Pujehun / Galliness Perri / Bumpeh Perri CHC",
            "Willie",
            "Stevens",
            "Male",
            "",
            "Negative-Conf"));
  }

  @Test
  public void queryWithProgramAndDimensionFilterUsingIdSchemeName() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=IpHINAT79UW")
            .add("dimension=IpHINAT79UW.ZzYYXq4fJie.cYGaxwK615G:IN:Negative-Conf")
            .add("desc=w75KJ2mc4zz,zDhUuAYrxNC")
            .add("relativePeriodDate=2016-01-01")
            .add("outputIdScheme=NAME");

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("rows", hasSize(equalTo(50)))
        .body("height", equalTo(50))
        .body("width", equalTo(17))
        .body("headerWidth", equalTo(17))
        .body("headers", hasSize(equalTo(17)))
        .body("metaData.pager.page", equalTo(1))
        .body("metaData.pager.pageSize", equalTo(50))
        .body("metaData.pager.isLastPage", is(false))
        .body("metaData.pager", not(hasKey("total")))
        .body("metaData.pager", not(hasKey("pageCount")))
        .body("metaData.dimensions", not(hasKey("ou")))
        .body("metaData.dimensions", hasKey("pe"));

    // Validate the first three rows, as samples.

    validateRow(
        response,
        0,
        List.of(
            "acCGrc3qlji",
            "2015-08-06 21:12:36.226",
            "",
            "2015-08-06 21:12:36.226",
            "",
            "",
            "",
            "",
            "",
            "Plantain Island MCHP",
            "OU_247071",
            "Sierra Leone / Moyamba / Kargboro / Plantain Island MCHP",
            "Willie",
            "Wallace",
            "Male",
            "",
            "Negative (Confirmed)"));

    validateRow(
        response,
        1,
        List.of(
            "yG1PQX6xCkK",
            "2015-08-07 15:47:23.061",
            "",
            "2015-08-07 15:47:23.06",
            "",
            "",
            "",
            "",
            "",
            "Yankasa MCHP",
            "OU_193257",
            "Sierra Leone / Bombali / Makari Gbanti / Yankasa MCHP",
            "Willie",
            "Stewart",
            "Male",
            "",
            "Negative (Confirmed)"));

    validateRow(
        response,
        2,
        List.of(
            "cr0DjId1xhO",
            "2015-08-06 21:20:47.468",
            "",
            "2015-08-06 21:20:47.467",
            "",
            "",
            "",
            "",
            "",
            "Bumpeh Perri CHC",
            "OU_260423",
            "Sierra Leone / Pujehun / Galliness Perri / Bumpeh Perri CHC",
            "Willie",
            "Stevens",
            "Male",
            "",
            "Negative (Confirmed)"));
  }

  @Test
  public void queryWithCoordinatesOnly() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=qDkgAbB5Jlk")
            .add("coordinatesOnly=true")
            .add("desc=lastUpdated")
            .add("relativePeriodDate=2020-01-01");

    // When
    ApiResponse response = analyticsTeiActions.query().get("Zy2SEgA61ys", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("rows", hasSize(equalTo(15)))
        .body("height", equalTo(15))
        .body("width", equalTo(23))
        .body("headerWidth", equalTo(23))
        .body("headers", hasSize(equalTo(23)))
        .body("metaData.pager.page", equalTo(1))
        .body("metaData.pager.pageSize", equalTo(50))
        .body("metaData.pager.isLastPage", is(true))
        .body("metaData.pager", not(hasKey("total")))
        .body("metaData.pager", not(hasKey("pageCount")))
        .body("metaData.dimensions", not(hasKey("ou")))
        .body("metaData.dimensions", hasKey("pe"));

    // Validate the first three rows, as samples.

    validateRow(
        response,
        0,
        List.of(
            "F8yKM85NbxW",
            "2019-08-21 13:31:33.41",
            "",
            "2019-08-21 13:25:38.022",
            "",
            "",
            "POINT(-11.7896 8.2593)",
            "-11.7896",
            "8.2593",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "ABC123456",
            "PID0001",
            "Johnson",
            "Sarah",
            "1988-07-10",
            "",
            "30",
            "FEMALE",
            "",
            "FR",
            ""));

    validateRow(
        response,
        1,
        List.of(
            "DsSlC54GNXy",
            "2019-08-21 13:31:27.995",
            "",
            "2019-08-21 13:25:29.756",
            "",
            "",
            "POINT(-11.773 8.3201)",
            "-11.773",
            "8.3201",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "LBO315445",
            "7hdjdj",
            "Martin",
            "Steve",
            "1976-02-03",
            "",
            "43",
            "FEMALE",
            "",
            "",
            ""));

    validateRow(
        response,
        2,
        List.of(
            "AuAWm61eD0X",
            "2019-08-21 13:31:09.399",
            "",
            "2019-08-21 13:24:59.811",
            "",
            "",
            "POINT(-11.7809 8.3373)",
            "-11.7809",
            "8.3373",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "YOH335093",
            "",
            "",
            "",
            "1998-02-04",
            "",
            "21",
            "",
            "",
            "",
            "[40.41441,-3.71542]"));
  }

  @Test
  public void queryWithGeometryOnly() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=qDkgAbB5Jlk")
            .add("geometryOnly=true")
            .add("desc=lastUpdated")
            .add("relativePeriodDate=2020-01-01");

    // When
    ApiResponse response = analyticsTeiActions.query().get("Zy2SEgA61ys", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("rows", hasSize(equalTo(15)))
        .body("height", equalTo(15))
        .body("width", equalTo(23))
        .body("headerWidth", equalTo(23))
        .body("headers", hasSize(equalTo(23)))
        .body("metaData.pager.page", equalTo(1))
        .body("metaData.pager.pageSize", equalTo(50))
        .body("metaData.pager.isLastPage", is(true))
        .body("metaData.pager", not(hasKey("total")))
        .body("metaData.pager", not(hasKey("pageCount")))
        .body("metaData.dimensions", not(hasKey("ou")))
        .body("metaData.dimensions", hasKey("pe"));

    // Validate the first three rows, as samples.

    validateRow(
        response,
        0,
        List.of(
            "F8yKM85NbxW",
            "2019-08-21 13:31:33.41",
            "",
            "2019-08-21 13:25:38.022",
            "",
            "",
            "POINT(-11.7896 8.2593)",
            "-11.7896",
            "8.2593",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "ABC123456",
            "PID0001",
            "Johnson",
            "Sarah",
            "1988-07-10",
            "",
            "30",
            "FEMALE",
            "",
            "FR",
            ""));

    validateRow(
        response,
        1,
        List.of(
            "DsSlC54GNXy",
            "2019-08-21 13:31:27.995",
            "",
            "2019-08-21 13:25:29.756",
            "",
            "",
            "POINT(-11.773 8.3201)",
            "-11.773",
            "8.3201",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "LBO315445",
            "7hdjdj",
            "Martin",
            "Steve",
            "1976-02-03",
            "",
            "43",
            "FEMALE",
            "",
            "",
            ""));

    validateRow(
        response,
        2,
        List.of(
            "AuAWm61eD0X",
            "2019-08-21 13:31:09.399",
            "",
            "2019-08-21 13:24:59.811",
            "",
            "",
            "POINT(-11.7809 8.3373)",
            "-11.7809",
            "8.3373",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "YOH335093",
            "",
            "",
            "",
            "1998-02-04",
            "",
            "21",
            "",
            "",
            "",
            "[40.41441,-3.71542]"));
  }

  @Test
  public void queryWithProgramAndDimensionFilterUsingDataIdSchemeName() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=IpHINAT79UW")
            .add(
                "dimension=IpHINAT79UW.ZzYYXq4fJie.GQY2lXrypjO:gt:12,IpHINAT79UW.ZzYYXq4fJie.cYGaxwK615G:in:Positive")
            .add("desc=lastupdated")
            .add("relativePeriodDate=2022-01-01")
            .add("eventDate=IpHINAT79UW.ZzYYXq4fJie.LAST_YEAR")
            .add("dataIdScheme=NAME");

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("rows", hasSize(equalTo(50)))
        .body("height", equalTo(50))
        .body("width", equalTo(18))
        .body("headerWidth", equalTo(18))
        .body("headers", hasSize(equalTo(18)))
        .body("metaData.pager.page", equalTo(1))
        .body("metaData.pager.pageSize", equalTo(50))
        .body("metaData.pager.isLastPage", is(false))
        .body("metaData.pager", not(hasKey("total")))
        .body("metaData.pager", not(hasKey("pageCount")))
        .body("metaData.dimensions", not(hasKey("ou")))
        .body("metaData.dimensions", hasKey("pe"));

    // Validate the first three rows, as samples.

    validateRow(
        response,
        0,
        List.of(
            "YiKaRIm5IUj",
            "2015-08-06 21:20:52.78",
            "",
            "2015-08-06 21:20:52.78",
            "",
            "",
            "",
            "",
            "",
            "Jangalor MCHP",
            "OU_543020",
            "Sierra Leone / Bonthe / Imperi / Jangalor MCHP",
            "Antonio",
            "Ruiz",
            "Male",
            "",
            "3681",
            "Positive"));

    validateRow(
        response,
        1,
        List.of(
            "ApUIfbrXE0G",
            "2015-08-06 21:20:52.777",
            "",
            "2015-08-06 21:20:52.777",
            "",
            "",
            "",
            "",
            "",
            "Mattru Jong MCHP",
            "OU_197389",
            "Sierra Leone / Bonthe / Jong / Mattru Jong MCHP",
            "Julia",
            "Gardner",
            "Female",
            "",
            "3945",
            "Positive"));

    validateRow(
        response,
        2,
        List.of(
            "NiuDa8jIu4J",
            "2015-08-06 21:20:52.776",
            "",
            "2015-08-06 21:20:52.776",
            "",
            "",
            "",
            "",
            "",
            "Ngaiya MCHP",
            "OU_233404",
            "Sierra Leone / Kono / Nimikoro / Ngaiya MCHP",
            "Beverly",
            "Hart",
            "Female",
            "",
            "3104",
            "Positive"));
  }

  @Test
  public void queryWithProgramAndDimensionFilterUsingDataIdSchemeUid() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=IpHINAT79UW")
            .add(
                "dimension=IpHINAT79UW.ZzYYXq4fJie.GQY2lXrypjO:gt:12,IpHINAT79UW.ZzYYXq4fJie.cYGaxwK615G:in:Positive")
            .add("desc=lastupdated")
            .add("relativePeriodDate=2022-01-01")
            .add("eventDate=IpHINAT79UW.ZzYYXq4fJie.LAST_YEAR")
            .add("dataIdScheme=UID");

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("rows", hasSize(equalTo(50)))
        .body("height", equalTo(50))
        .body("width", equalTo(18))
        .body("headerWidth", equalTo(18))
        .body("headers", hasSize(equalTo(18)))
        .body("metaData.pager.page", equalTo(1))
        .body("metaData.pager.pageSize", equalTo(50))
        .body("metaData.pager.isLastPage", is(false))
        .body("metaData.pager", not(hasKey("total")))
        .body("metaData.pager", not(hasKey("pageCount")))
        .body("metaData.dimensions", not(hasKey("ou")))
        .body("metaData.dimensions", hasKey("pe"));

    // Validate the first three rows, as samples.

    validateRow(
        response,
        0,
        List.of(
            "YiKaRIm5IUj",
            "2015-08-06 21:20:52.78",
            "",
            "2015-08-06 21:20:52.78",
            "",
            "",
            "",
            "",
            "",
            "Jangalor MCHP",
            "OU_543020",
            "Sierra Leone / Bonthe / Imperi / Jangalor MCHP",
            "Antonio",
            "Ruiz",
            "rBvjJYbMCVx",
            "",
            "3681",
            "fWI0UiNZgMy"));

    validateRow(
        response,
        1,
        List.of(
            "ApUIfbrXE0G",
            "2015-08-06 21:20:52.777",
            "",
            "2015-08-06 21:20:52.777",
            "",
            "",
            "",
            "",
            "",
            "Mattru Jong MCHP",
            "OU_197389",
            "Sierra Leone / Bonthe / Jong / Mattru Jong MCHP",
            "Julia",
            "Gardner",
            "Mnp3oXrpAbK",
            "",
            "3945",
            "fWI0UiNZgMy"));

    validateRow(
        response,
        2,
        List.of(
            "NiuDa8jIu4J",
            "2015-08-06 21:20:52.776",
            "",
            "2015-08-06 21:20:52.776",
            "",
            "",
            "",
            "",
            "",
            "Ngaiya MCHP",
            "OU_233404",
            "Sierra Leone / Kono / Nimikoro / Ngaiya MCHP",
            "Beverly",
            "Hart",
            "Mnp3oXrpAbK",
            "",
            "3104",
            "fWI0UiNZgMy"));
  }

  @Test
  public void queryWithProgramStatus() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=IpHINAT79UW")
            .add("programStatus=IpHINAT79UW.COMPLETED")
            .add("lastUpdated=LAST_YEAR")
            .add("desc=lastupdated")
            .add("relativePeriodDate=2018-01-01");

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("rows", hasSize(equalTo(3)))
        .body("height", equalTo(3))
        .body("width", equalTo(17))
        .body("headerWidth", equalTo(17))
        .body("headers", hasSize(equalTo(17)))
        .body("metaData.pager.page", equalTo(1))
        .body("metaData.pager.pageSize", equalTo(50))
        .body("metaData.pager.isLastPage", is(true))
        .body("metaData.pager", not(hasKey("total")))
        .body("metaData.pager", not(hasKey("pageCount")))
        .body("metaData.dimensions", not(hasKey("ou")))
        .body("metaData.dimensions", hasKey("pe"));

    // Validate the first row, as samples.

    validateRow(
        response,
        0,
        List.of(
            "vOxUH373fy5",
            "2017-05-26 11:46:22.372",
            "",
            "2017-01-20 10:44:02.77",
            "",
            "",
            "",
            "",
            "",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "Filona",
            "Ryder",
            "Female",
            "",
            "COMPLETED"));
  }

  @Test
  public void queryWithEnrollmentStatus() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=IpHINAT79UW")
            .add("enrollmentStatus=IpHINAT79UW.COMPLETED")
            .add("lastUpdated=LAST_YEAR")
            .add("desc=lastupdated")
            .add("relativePeriodDate=2018-01-01");

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("rows", hasSize(equalTo(3)))
        .body("height", equalTo(3))
        .body("width", equalTo(17))
        .body("headerWidth", equalTo(17))
        .body("headers", hasSize(equalTo(17)))
        .body("metaData.pager.page", equalTo(1))
        .body("metaData.pager.pageSize", equalTo(50))
        .body("metaData.pager.isLastPage", is(true))
        .body("metaData.pager", not(hasKey("total")))
        .body("metaData.pager", not(hasKey("pageCount")))
        .body("metaData.dimensions", not(hasKey("ou")))
        .body("metaData.dimensions", hasKey("pe"));

    // Validate the first row, as samples.

    validateRow(
        response,
        0,
        List.of(
            "vOxUH373fy5",
            "2017-05-26 11:46:22.372",
            "",
            "2017-01-20 10:44:02.77",
            "",
            "",
            "",
            "",
            "",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "Filona",
            "Ryder",
            "Female",
            "",
            "COMPLETED"));
  }

  @Test
  public void queryWithEventStatus() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=IpHINAT79UW")
            .add("eventStatus=IpHINAT79UW.A03MvHHogjR.COMPLETED")
            .add("lastUpdated=LAST_YEAR")
            .add("desc=lastupdated")
            .add("relativePeriodDate=2018-01-01");

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("rows", hasSize(equalTo(3)))
        .body("height", equalTo(3))
        .body("width", equalTo(17))
        .body("headerWidth", equalTo(17))
        .body("headers", hasSize(equalTo(17)))
        .body("metaData.pager.page", equalTo(1))
        .body("metaData.pager.pageSize", equalTo(50))
        .body("metaData.pager.isLastPage", is(true))
        .body("metaData.pager", not(hasKey("total")))
        .body("metaData.pager", not(hasKey("pageCount")))
        .body("metaData.dimensions", not(hasKey("ou")))
        .body("metaData.dimensions", hasKey("pe"));

    // Validate the first row, as samples.

    validateRow(
        response,
        0,
        List.of(
            "vOxUH373fy5",
            "2017-05-26 11:46:22.372",
            "",
            "2017-01-20 10:44:02.77",
            "",
            "",
            "",
            "",
            "",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "Filona",
            "Ryder",
            "Female",
            "",
            "COMPLETED"));
  }

  @Test
  public void queryProgramIndicator() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=IpHINAT79UW")
            .add(
                "dimension=IpHINAT79UW.GxdhnY5wmHq,w75KJ2mc4zz:eq:Justin,zDhUuAYrxNC:eq:Hayes,ou:eqPIdr5yD1Q")
            .add("desc=IpHINAT79UW.GxdhnY5wmHq")
            .add("lastUpdated=LAST_YEAR")
            .add("relativePeriodDate=2016-01-01");

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("rows", hasSize(equalTo(1)))
        .body("height", equalTo(1))
        .body("width", equalTo(17))
        .body("headerWidth", equalTo(17))
        .body("headers", hasSize(equalTo(17)))
        .body("metaData.pager.page", equalTo(1))
        .body("metaData.pager.pageSize", equalTo(50))
        .body("metaData.pager.isLastPage", is(true))
        .body("metaData.pager", not(hasKey("total")))
        .body("metaData.pager", not(hasKey("pageCount")))
        .body("metaData.items.GxdhnY5wmHq.name", equalTo("Average weight (g)"))
        .body("metaData.dimensions", hasKey("pe"));

    validateHeader(
        response,
        16,
        "IpHINAT79UW.GxdhnY5wmHq",
        "Average weight (g)",
        "NUMBER",
        "java.lang.Double",
        false,
        true);

    validateRow(
        response,
        0,
        List.of(
            "a04hYxjC8lM",
            "2015-08-06 21:20:52.547",
            "",
            "2015-08-06 21:20:52.547",
            "",
            "",
            "",
            "",
            "",
            "Rokolon MCHP",
            "OU_707826",
            "Sierra Leone / Moyamba / Ribbi / Rokolon MCHP",
            "Justin",
            "Hayes",
            "Male",
            "",
            "2994.5"));
  }
}
