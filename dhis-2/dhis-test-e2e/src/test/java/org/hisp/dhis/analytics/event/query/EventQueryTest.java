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
package org.hisp.dhis.analytics.event.query;

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
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.actions.analytics.AnalyticsEventActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/**
 * Groups e2e tests for Events "/query" endpoint.
 *
 * @author maikel arabori
 */
public class EventQueryTest extends AnalyticsApiTest {
  private final AnalyticsEventActions analyticsEventActions = new AnalyticsEventActions();

  @Test
  public void queryWithProgramAndProgramStageWhenTotalPagesIsFalse() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=pe:LAST_12_MONTHS,ou:ImspTQPwCqd")
            .add("stage=dBwrot7S420")
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("totalPages=false")
            .add("desc=lastupdated")
            .add("relativePeriodDate=2022-09-27");

    // When
    ApiResponse response = analyticsEventActions.query().get("lxAQ7Zs9VYR", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(17)))
        .body("rows", hasSize(equalTo(3)))
        .body("metaData.pager.page", equalTo(1))
        .body("metaData.pager.pageSize", equalTo(50))
        .body("metaData.pager.isLastPage", is(true))
        .body("metaData.pager", not(hasKey("total")))
        .body("metaData.pager", not(hasKey("pageCount")))
        .body("metaData.items.ImspTQPwCqd.name", equalTo("Sierra Leone"))
        .body("metaData.items.dBwrot7S420.name", equalTo("Antenatal care visit"))
        .body("metaData.items.ou.name", equalTo("Organisation unit"))
        .body("metaData.items.lxAQ7Zs9VYR.name", equalTo("Antenatal care visit"))
        .body("metaData.items.LAST_12_MONTHS.name", equalTo("Last 12 months"))
        .body("metaData.dimensions.pe", hasSize(equalTo(0)))
        .body("metaData.dimensions.ou", hasSize(equalTo(1)))
        .body("metaData.dimensions.ou", hasItem("ImspTQPwCqd"))
        .body("height", equalTo(3))
        .body("width", equalTo(17))
        .body("headerWidth", equalTo(17));

    // Validate headers
    validateHeader(response, 0, "psi", "Event", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "ps", "Program stage", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 2, "eventdate", "Visit date", "DATE", "java.time.LocalDate", false, true);
    validateHeader(response, 3, "storedby", "Stored by", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 4, "createdbydisplayname", "Created by", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        5,
        "lastupdatedbydisplayname",
        "Last updated by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response, 6, "lastupdated", "Last updated on", "DATE", "java.time.LocalDate", false, true);
    validateHeader(
        response, 7, "scheduleddate", "Scheduled date", "DATE", "java.time.LocalDate", false, true);
    validateHeader(response, 8, "geometry", "Geometry", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 9, "longitude", "Longitude", "NUMBER", "java.lang.Double", false, true);
    validateHeader(response, 10, "latitude", "Latitude", "NUMBER", "java.lang.Double", false, true);
    validateHeader(
        response, 11, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        12,
        "ounamehierarchy",
        "Organisation unit name hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response, 13, "oucode", "Organisation unit code", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 14, "programstatus", "Program status", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 15, "eventstatus", "Event status", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 16, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);

    // Validate the first three rows, as samples.
    validateRow(
        response,
        0,
        List.of(
            "ohAH6BXIMad",
            "dBwrot7S420",
            "2022-04-07 00:00:00.0",
            "",
            "",
            "",
            "2018-04-12 16:05:41.933",
            "",
            "",
            "0.0",
            "0.0",
            "Ngelehun CHC",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "OU_559",
            "ACTIVE",
            "ACTIVE",
            "DiszpKrYNg8"));

    validateRow(
        response,
        1,
        List.of(
            "onXW2DQHRGS",
            "dBwrot7S420",
            "2022-04-01 00:00:00.0",
            "",
            "",
            "",
            "2018-04-12 16:05:28.015",
            "",
            "",
            "0.0",
            "0.0",
            "Ngelehun CHC",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "OU_559",
            "ACTIVE",
            "ACTIVE",
            "DiszpKrYNg8"));

    validateRow(
        response,
        2,
        List.of(
            "A7vnB73x5Xw",
            "dBwrot7S420",
            "2022-04-01 00:00:00.0",
            "",
            "",
            "",
            "2018-04-12 16:05:16.957",
            "",
            "",
            "0.0",
            "0.0",
            "Ngelehun CHC",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "OU_559",
            "ACTIVE",
            "ACTIVE",
            "DiszpKrYNg8"));
  }

  @Test
  public void queryWithProgramAndProgramStageWhenTotalPagesIsTrueByDefault() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=pe:LAST_12_MONTHS,ou:ImspTQPwCqd")
            .add("stage=dBwrot7S420")
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("desc=lastupdated")
            .add("relativePeriodDate=2022-09-22");

    // When
    ApiResponse response = analyticsEventActions.query().get("lxAQ7Zs9VYR", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(17)))
        .body("rows", hasSize(equalTo(3)))
        .body("metaData.pager.page", equalTo(1))
        .body("metaData.pager.pageSize", equalTo(50))
        .body("metaData.pager.total", equalTo(3))
        .body("metaData.pager.pageCount", equalTo(1))
        .body("metaData.pager", not(hasKey("isLastPage")))
        .body("metaData.items.ImspTQPwCqd.name", equalTo("Sierra Leone"))
        .body("metaData.items.dBwrot7S420.name", equalTo("Antenatal care visit"))
        .body("metaData.items.ou.name", equalTo("Organisation unit"))
        .body("metaData.items.lxAQ7Zs9VYR.name", equalTo("Antenatal care visit"))
        .body("metaData.items.LAST_12_MONTHS.name", equalTo("Last 12 months"))
        .body("metaData.dimensions.pe", hasSize(equalTo(0)))
        .body("metaData.dimensions.ou", hasSize(equalTo(1)))
        .body("metaData.dimensions.ou", hasItem("ImspTQPwCqd"))
        .body("height", equalTo(3))
        .body("width", equalTo(17))
        .body("headerWidth", equalTo(17));

    // Validate headers
    validateHeader(response, 0, "psi", "Event", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "ps", "Program stage", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 2, "eventdate", "Visit date", "DATE", "java.time.LocalDate", false, true);
    validateHeader(response, 3, "storedby", "Stored by", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 4, "createdbydisplayname", "Created by", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        5,
        "lastupdatedbydisplayname",
        "Last updated by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response, 6, "lastupdated", "Last updated on", "DATE", "java.time.LocalDate", false, true);
    validateHeader(
        response, 7, "scheduleddate", "Scheduled date", "DATE", "java.time.LocalDate", false, true);
    validateHeader(response, 8, "geometry", "Geometry", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 9, "longitude", "Longitude", "NUMBER", "java.lang.Double", false, true);
    validateHeader(response, 10, "latitude", "Latitude", "NUMBER", "java.lang.Double", false, true);
    validateHeader(
        response, 11, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        12,
        "ounamehierarchy",
        "Organisation unit name hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response, 13, "oucode", "Organisation unit code", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 14, "programstatus", "Program status", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 15, "eventstatus", "Event status", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 16, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);

    // Validate the first three rows, as samples.
    validateRow(
        response,
        0,
        List.of(
            "ohAH6BXIMad",
            "dBwrot7S420",
            "2022-04-07 00:00:00.0",
            "",
            "",
            "",
            "2018-04-12 16:05:41.933",
            "",
            "",
            "0.0",
            "0.0",
            "Ngelehun CHC",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "OU_559",
            "ACTIVE",
            "ACTIVE",
            "DiszpKrYNg8"));

    validateRow(
        response,
        1,
        List.of(
            "onXW2DQHRGS",
            "dBwrot7S420",
            "2022-04-01 00:00:00.0",
            "",
            "",
            "",
            "2018-04-12 16:05:28.015",
            "",
            "",
            "0.0",
            "0.0",
            "Ngelehun CHC",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "OU_559",
            "ACTIVE",
            "ACTIVE",
            "DiszpKrYNg8"));

    validateRow(
        response,
        2,
        List.of(
            "A7vnB73x5Xw",
            "dBwrot7S420",
            "2022-04-01 00:00:00.0",
            "",
            "",
            "",
            "2018-04-12 16:05:16.957",
            "",
            "",
            "0.0",
            "0.0",
            "Ngelehun CHC",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "OU_559",
            "ACTIVE",
            "ACTIVE",
            "DiszpKrYNg8"));
  }

  @Test
  void testMetadataInfoForOptionSetForQuery() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add(
                "dimension=ou:ImspTQPwCqd,pe:LAST_12_MONTHS,C0aLZo75dgJ.B6TnnFMgmCk,C0aLZo75dgJ.Z1rLc1rVHK8,C0aLZo75dgJ.CklPZdOd6H1")
            .add("filter=C0aLZo75dgJ.vTKipVM0GsX,C0aLZo75dgJ.h5FuguPFF2j,C0aLZo75dgJ.aW66s2QSosT")
            .add("stage=C0aLZo75dgJ")
            .add("displayProperty=NAME")
            .add("outputType=ENROLLMENT")
            .add("totalPages=false");

    // When
    ApiResponse response = analyticsEventActions.query().get("qDkgAbB5Jlk", JSON, JSON, params);
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(24)))
        .body("height", equalTo(0))
        .body("width", equalTo(0))
        .body("rows", hasSize(equalTo(0)))
        .body("metaData.items", hasKey("CklPZdOd6H1"))
        .body("metaData.items", not(hasKey("AZK4rjJCss5")))
        .body("metaData.items", not(hasKey("UrUdMteQzlT")));

    validateHeader(
        response,
        22,
        "C0aLZo75dgJ.CklPZdOd6H1",
        "Sex",
        "TEXT",
        "java.lang.String",
        false,
        true,
        "hiQ3QFheQ3O");
  }

  @Test
  public void queryMetadataInfoForOptionSetAndOptionsWhenNoData() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=lastupdated")
            .add("headers=ouname,A03MvHHogjR.ebaJjqltK5N")
            .add("lastUpdated=LAST_5_YEARS")
            .add("stage=A03MvHHogjR")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add("pageSize=0")
            .add("page=1")
            .add("dimension=ou:USER_ORGUNIT,A03MvHHogjR.ebaJjqltK5N:IN:1;2")
            .add("relativePeriodDate=2022-10-01");

    // When
    ApiResponse response = analyticsEventActions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(2)))
        .body("rows", hasSize(equalTo(0)))
        .body("height", equalTo(0))
        .body("width", equalTo(0))
        .body("headerWidth", equalTo(2));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":0,\"isLastPage\":false},\"items\":{\"kzgQRhOCadd\":{\"uid\":\"kzgQRhOCadd\",\"name\":\"MNCH Polio doses (0-3)\",\"options\":[{\"code\":\"2\",\"uid\":\"Xr0M5yEhtpT\"},{\"code\":\"1\",\"uid\":\"lFFqylGiWLk\"}]},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"ebaJjqltK5N\":{\"uid\":\"ebaJjqltK5N\",\"code\":\"DE_2006104\",\"name\":\"MCH OPV dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"Xr0M5yEhtpT\":{\"uid\":\"Xr0M5yEhtpT\",\"code\":\"2\",\"name\":\"Dose 2\"},\"A03MvHHogjR.ebaJjqltK5N\":{\"uid\":\"ebaJjqltK5N\",\"code\":\"DE_2006104\",\"name\":\"MCH OPV dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"LAST_5_YEARS\":{\"name\":\"Last 5 years\"},\"lFFqylGiWLk\":{\"uid\":\"lFFqylGiWLk\",\"code\":\"1\",\"name\":\"Dose 1\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"A03MvHHogjR.ebaJjqltK5N\":[\"lFFqylGiWLk\",\"Xr0M5yEhtpT\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        1,
        "A03MvHHogjR.ebaJjqltK5N",
        "MCH OPV dose",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // Assert rows.
  }

  @Test
  public void queryMetadataInfoForOptionSetAndOptionsWhenOneRows() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=lastupdated")
            .add("headers=ouname,A03MvHHogjR.ebaJjqltK5N")
            .add("lastUpdated=LAST_5_YEARS")
            .add("stage=A03MvHHogjR")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add("pageSize=1")
            .add("page=1")
            .add("dimension=ou:USER_ORGUNIT,A03MvHHogjR.ebaJjqltK5N:IN:1;2")
            .add("relativePeriodDate=2022-10-01");

    // When
    ApiResponse response = analyticsEventActions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(2)))
        .body("rows", hasSize(equalTo(1)))
        .body("height", equalTo(1))
        .body("width", equalTo(2))
        .body("headerWidth", equalTo(2));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":1,\"isLastPage\":false},\"items\":{\"kzgQRhOCadd\":{\"uid\":\"kzgQRhOCadd\",\"name\":\"MNCH Polio doses (0-3)\",\"options\":[{\"code\":\"1\",\"uid\":\"lFFqylGiWLk\"}]},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"ebaJjqltK5N\":{\"uid\":\"ebaJjqltK5N\",\"code\":\"DE_2006104\",\"name\":\"MCH OPV dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"A03MvHHogjR.ebaJjqltK5N\":{\"uid\":\"ebaJjqltK5N\",\"code\":\"DE_2006104\",\"name\":\"MCH OPV dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"LAST_5_YEARS\":{\"name\":\"Last 5 years\"},\"lFFqylGiWLk\":{\"uid\":\"lFFqylGiWLk\",\"code\":\"1\",\"name\":\"Dose 1\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"A03MvHHogjR.ebaJjqltK5N\":[\"lFFqylGiWLk\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        1,
        "A03MvHHogjR.ebaJjqltK5N",
        "MCH OPV dose",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // Assert rows.
    validateRow(response, 0, List.of("Ngelehun CHC", "1"));
  }

  @Test
  public void queryMetadataInfoForOptionSetAndOptionsWhenTenRows() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=lastupdated")
            .add("headers=ouname,A03MvHHogjR.ebaJjqltK5N")
            .add("lastUpdated=LAST_5_YEARS")
            .add("stage=A03MvHHogjR")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add("pageSize=1")
            .add("page=10")
            .add("dimension=ou:USER_ORGUNIT,A03MvHHogjR.ebaJjqltK5N:IN:1;2")
            .add("relativePeriodDate=2022-10-01");

    // When
    ApiResponse response = analyticsEventActions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(2)))
        .body("rows", hasSize(equalTo(1)))
        .body("height", equalTo(1))
        .body("width", equalTo(2))
        .body("headerWidth", equalTo(2));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":10,\"pageSize\":1,\"isLastPage\":false},\"items\":{\"kzgQRhOCadd\":{\"uid\":\"kzgQRhOCadd\",\"name\":\"MNCH Polio doses (0-3)\",\"options\":[{\"code\":\"1\",\"uid\":\"lFFqylGiWLk\"}]},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"ebaJjqltK5N\":{\"uid\":\"ebaJjqltK5N\",\"code\":\"DE_2006104\",\"name\":\"MCH OPV dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"A03MvHHogjR.ebaJjqltK5N\":{\"uid\":\"ebaJjqltK5N\",\"code\":\"DE_2006104\",\"name\":\"MCH OPV dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"LAST_5_YEARS\":{\"name\":\"Last 5 years\"},\"lFFqylGiWLk\":{\"uid\":\"lFFqylGiWLk\",\"code\":\"1\",\"name\":\"Dose 1\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"A03MvHHogjR.ebaJjqltK5N\":[\"lFFqylGiWLk\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        1,
        "A03MvHHogjR.ebaJjqltK5N",
        "MCH OPV dose",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // Assert rows.
    validateRow(response, 0, List.of("Ngelehun CHC", "1"));
  }

  @Test
  public void eventQueryWithProgramAndRepeatableProgramStage() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=edqlbukwRfQ.vANAXwtLwcT,ou:ImspTQPwCqd")
            .add("headers=ou,ounamehierarchy,edqlbukwRfQ.vANAXwtLwcT")
            .add("stage=edqlbukwRfQ")
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("desc=incidentdate")
            .add("totalPages=false")
            .add("pageSize=100")
            .add("page=1")
            .add("rowContext=true");

    // When
    ApiResponse response = analyticsEventActions.query().get("WSGAb5XwJ3Y", JSON, JSON, params);
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(100)));

    validateHeader(response, 0, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        1,
        "ounamehierarchy",
        "Organisation unit name hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        2,
        "edqlbukwRfQ.vANAXwtLwcT",
        "WHOMCH Hemoglobin value",
        "NUMBER",
        "java.lang.Double",
        false,
        true);

    validateRow(
        response,
        List.of(
            "NjyJYiIuKIG", "Sierra Leone / Bombali / Sella Limba / Kathanta Yimbor CHC", "14.0"));
    validateRow(
        response,
        List.of("xATvj8pdYoT", "Sierra Leone / Kailahun / Peje Bongre / Grima Jou MCHP", "24.0"));
  }
}
