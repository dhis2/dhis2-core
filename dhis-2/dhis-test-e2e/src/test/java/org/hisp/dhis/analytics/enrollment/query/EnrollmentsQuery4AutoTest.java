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
package org.hisp.dhis.analytics.enrollment.query;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsEnrollmentsActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/enrollments/query" endpoint. */
public class EnrollmentsQuery4AutoTest extends AnalyticsApiTest {
  private final AnalyticsEnrollmentsActions actions = new AnalyticsEnrollmentsActions();

  @Test
  public void someDimensionsWithFilter() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add(
                "headers=ouname,J5jldMd8OHv,w75KJ2mc4zz,cejWyOfXge6,GUOBQt5K2WI,ZkbAXlQUYJG.U5ubm6PPYrM,lZGmxYbs97q,OvY4VVhSDeJ,lastupdated,createdbydisplayname,lastupdatedbydisplayname,enrollmentdate,programstatus")
            .add("displayProperty=NAME")
            .add("rowContext=true")
            .add("pageSize=100")
            .add("outputType=ENROLLMENT")
            .add("relativePeriodDate=2022-07-01")
            .add("includeMetadataDetails=true")
            .add("asc=OvY4VVhSDeJ")
            .add("lastUpdated=LAST_10_YEARS")
            .add("totalPages=false")
            .add("page=1")
            .add(
                "dimension=ou:O6uvpzGd5pu,J5jldMd8OHv,w75KJ2mc4zz,cejWyOfXge6:IN:Female,GUOBQt5K2WI:LIKE:Cape,ZkbAXlQUYJG.U5ubm6PPYrM,lZGmxYbs97q,OvY4VVhSDeJ:LE:45:NE:NV:!EQ:44")
            .add("programStatus=ACTIVE");

    // When
    ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(13)))
        .body("rows", hasSize(equalTo(1)))
        .body("height", equalTo(1))
        .body("width", equalTo(13))
        .body("headerWidth", equalTo(13));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"lZGmxYbs97q\":{\"uid\":\"lZGmxYbs97q\",\"code\":\"MMD_PER_ID\",\"name\":\"Unique ID\",\"description\":\"Unique identiifer\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"RXL3lPSK8oG\":{\"uid\":\"RXL3lPSK8oG\",\"code\":\"Clinic\",\"name\":\"Clinic\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"Mnp3oXrpAbK\":{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\",\"name\":\"Female\"},\"J5jldMd8OHv\":{\"uid\":\"J5jldMd8OHv\",\"name\":\"Facility Type\",\"dimensionType\":\"ORGANISATION_UNIT_GROUP_SET\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"jdRD35YwbRH\":{\"uid\":\"jdRD35YwbRH\",\"name\":\"Sputum smear microscopy test\",\"description\":\"Sputum smear microscopy test\"},\"w75KJ2mc4zz\":{\"uid\":\"w75KJ2mc4zz\",\"code\":\"MMD_PER_NAM\",\"name\":\"First name\",\"description\":\"First name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"CXw2yu5fodb\":{\"uid\":\"CXw2yu5fodb\",\"code\":\"CHC\",\"name\":\"CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"O6uvpzGd5pu\":{\"uid\":\"O6uvpzGd5pu\",\"code\":\"OU_264\",\"name\":\"Bo\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"U5ubm6PPYrM\":{\"uid\":\"U5ubm6PPYrM\",\"code\":\"DE_860610\",\"name\":\"TB HIV testing done\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ZkbAXlQUYJG\":{\"uid\":\"ZkbAXlQUYJG\",\"name\":\"TB visit\",\"description\":\"Routine TB visit\"},\"uYxK4wmcPqA\":{\"uid\":\"uYxK4wmcPqA\",\"code\":\"CHP\",\"name\":\"CHP\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"cejWyOfXge6\":{\"uid\":\"cejWyOfXge6\",\"name\":\"Gender\",\"description\":\"Gender\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"tDZVQ1WtwpA\":{\"uid\":\"tDZVQ1WtwpA\",\"code\":\"Hospital\",\"name\":\"Hospital\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"OvY4VVhSDeJ\":{\"uid\":\"OvY4VVhSDeJ\",\"name\":\"Weight in kg\",\"description\":\"Weight in kg\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"LAST_10_YEARS\":{\"name\":\"Last 10 years\"},\"EPEcjy3FWmI\":{\"uid\":\"EPEcjy3FWmI\",\"name\":\"Lab monitoring\",\"description\":\"Laboratory monitoring\"},\"ur1Edk5Oe2n\":{\"uid\":\"ur1Edk5Oe2n\",\"name\":\"TB program\"},\"EYbopBOJWsW\":{\"uid\":\"EYbopBOJWsW\",\"code\":\"MCHP\",\"name\":\"MCHP\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"pC3N9N77UmT\":{\"uid\":\"pC3N9N77UmT\",\"name\":\"Gender\",\"options\":[{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\"}]},\"ZkbAXlQUYJG.U5ubm6PPYrM\":{\"uid\":\"U5ubm6PPYrM\",\"code\":\"DE_860610\",\"name\":\"TB HIV testing done\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"GUOBQt5K2WI\":{\"uid\":\"GUOBQt5K2WI\",\"code\":\"State\",\"name\":\"State\",\"description\":\"State\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"}},\"dimensions\":{\"lZGmxYbs97q\":[],\"OvY4VVhSDeJ\":[],\"pe\":[],\"J5jldMd8OHv\":[\"CXw2yu5fodb\",\"tDZVQ1WtwpA\",\"RXL3lPSK8oG\",\"uYxK4wmcPqA\",\"EYbopBOJWsW\"],\"ou\":[\"O6uvpzGd5pu\"],\"w75KJ2mc4zz\":[],\"ZkbAXlQUYJG.U5ubm6PPYrM\":[],\"GUOBQt5K2WI\":[],\"cejWyOfXge6\":[\"Mnp3oXrpAbK\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "J5jldMd8OHv", "Facility Type", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 2, "w75KJ2mc4zz", "First name", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 4, "GUOBQt5K2WI", "State", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        5,
        "ZkbAXlQUYJG.U5ubm6PPYrM",
        "TB HIV testing done",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response, 6, "lZGmxYbs97q", "Unique ID", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 7, "OvY4VVhSDeJ", "Weight in kg", "NUMBER", "java.lang.Double", false, true);
    validateHeader(
        response,
        8,
        "lastupdated",
        "Last updated on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(
        response, 9, "createdbydisplayname", "Created by", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        10,
        "lastupdatedbydisplayname",
        "Last updated by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        11,
        "enrollmentdate",
        "Start of treatment date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(
        response, 12, "programstatus", "Program status", "TEXT", "java.lang.String", false, true);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of(
            "Needy CHC",
            "CXw2yu5fodb",
            "Hiwet",
            "Female",
            "Eastern Cape",
            "",
            "",
            "44.5",
            "2017-03-28 12:33:55.9",
            ",  ()",
            ",  ()",
            "2022-02-24 12:33:55.887",
            "ACTIVE"));
  }
}
