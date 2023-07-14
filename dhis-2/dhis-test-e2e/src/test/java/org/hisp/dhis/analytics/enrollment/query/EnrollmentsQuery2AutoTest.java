/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.enrollment.query;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.actions.analytics.AnalyticsEnrollmentsActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/enrollments/query" endpoint. */
public class EnrollmentsQuery2AutoTest extends AnalyticsApiTest {
  private final AnalyticsEnrollmentsActions actions = new AnalyticsEnrollmentsActions();

  @Test
  public void queryRandom5() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=zyTL3AMIkf2")
            .add("includeMetadataDetails=true")
            .add("asc=lastupdated")
            .add(
                "headers=ouname,Bpx0589u8y0,lastupdated,Kv4fmHVAzwX,CH6wamtY9kK,uvMKOn1oWvd.pKj8YrNKVda,uvMKOn1oWvd.Y14cBKFUsg4,uvMKOn1oWvd.fADIatyOu2g,CWaAcQYKVpq.DanTR5x0WDK")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("enrollmentDate=2021")
            .add("outputType=ENROLLMENT")
            .add("pageSize=100")
            .add("page=1")
            .add(
                "dimension=ou:ImspTQPwCqd,Bpx0589u8y0,Kv4fmHVAzwX,CH6wamtY9kK,uvMKOn1oWvd.pKj8YrNKVda,uvMKOn1oWvd.Y14cBKFUsg4,uvMKOn1oWvd.fADIatyOu2g,CWaAcQYKVpq.DanTR5x0WDK")
            .add("relativePeriodDate=2023-07-14");

    // When
    ApiResponse response = actions.query().get("M3xtLkYBlKI", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(9)))
        .body("rows", hasSize(equalTo(8)))
        .body("height", equalTo(8))
        .body("width", equalTo(9))
        .body("headerWidth", equalTo(9));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"Kv4fmHVAzwX\":{\"uid\":\"Kv4fmHVAzwX\",\"name\":\"Focus Name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"Y14cBKFUsg4\":{\"uid\":\"Y14cBKFUsg4\",\"name\":\"Follow-up vector control action details 2\",\"description\":\"Follow-up vector control action details\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"fADIatyOu2g\":{\"uid\":\"fADIatyOu2g\",\"name\":\"LLIN coverage (%)\",\"description\":\"LLIN coverage following a foci response\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"CH6wamtY9kK\":{\"uid\":\"CH6wamtY9kK\",\"name\":\"Foci investigations performed & classified\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"COUNT\",\"totalAggregationType\":\"SUM\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"FZwy4OoRZtV\":{\"uid\":\"FZwy4OoRZtV\",\"code\":\"RESIDENT_IN_THE_FOCUS\",\"name\":\"Resident in the focus\"},\"2021\":{\"name\":\"2021\"},\"MAs88nJc9nL\":{\"uid\":\"MAs88nJc9nL\",\"code\":\"Private Clinic\",\"name\":\"Private Clinic\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"uvMKOn1oWvd.Y14cBKFUsg4\":{\"uid\":\"Y14cBKFUsg4\",\"name\":\"Follow-up vector control action details 2\",\"description\":\"Follow-up vector control action details\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"uvMKOn1oWvd.fADIatyOu2g\":{\"uid\":\"fADIatyOu2g\",\"name\":\"LLIN coverage (%)\",\"description\":\"LLIN coverage following a foci response\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"CWaAcQYKVpq\":{\"uid\":\"CWaAcQYKVpq\",\"name\":\"Foci investigation & classification\",\"description\":\"Includes the details on the foci investigation (including information on households, population, geography, breeding sites, species types, vector behaviour) as well as its final classification at the time of the investigation. This is a repeatable stage as foci can be investigated more than once and may change their classification as time goes on. \"},\"CWaAcQYKVpq.DanTR5x0WDK\":{\"uid\":\"DanTR5x0WDK\",\"name\":\"Residence of the malaria case/s  that prompted the current case investigation\",\"description\":\"Residence of the malaria case/s  that prompted the current case investigation\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"PVLOW4bCshG\":{\"uid\":\"PVLOW4bCshG\",\"code\":\"NGO\",\"name\":\"NGO\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"oRVt7g429ZO\":{\"uid\":\"oRVt7g429ZO\",\"code\":\"Public facilities\",\"name\":\"Public facilities\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"Bpx0589u8y0\":{\"uid\":\"Bpx0589u8y0\",\"name\":\"Facility Ownership\",\"dimensionType\":\"ORGANISATION_UNIT_GROUP_SET\"},\"uvMKOn1oWvd\":{\"uid\":\"uvMKOn1oWvd\",\"name\":\"Foci response\",\"description\":\"Details the public health response conducted within the foci  (including diagnosis and treatment activities, vector control actions and the effectiveness/results of the response). This is a repeatable stage as multiple public health responses for the same foci can occur depending on its classification at the time of investigation.\"},\"zyTL3AMIkf2\":{\"uid\":\"zyTL3AMIkf2\",\"name\":\"Foci classified as active\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"COUNT\",\"totalAggregationType\":\"SUM\"},\"w0gFTTmsUcF\":{\"uid\":\"w0gFTTmsUcF\",\"code\":\"Mission\",\"name\":\"Mission\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"M3xtLkYBlKI\":{\"uid\":\"M3xtLkYBlKI\",\"name\":\"Malaria focus investigation\",\"description\":\"It allows to register new focus areas in the system. Each focus area needs to be investigated and classified. Includes the relevant identifiers for the foci including the name and geographical details including the locality and its area. \"},\"uvMKOn1oWvd.pKj8YrNKVda\":{\"uid\":\"pKj8YrNKVda\",\"name\":\"Follow-up vector control action details 3\",\"description\":\"Follow-up vector control action details\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"DanTR5x0WDK\":{\"uid\":\"DanTR5x0WDK\",\"name\":\"Residence of the malaria case/s  that prompted the current case investigation\",\"description\":\"Residence of the malaria case/s  that prompted the current case investigation\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"pKj8YrNKVda\":{\"uid\":\"pKj8YrNKVda\",\"name\":\"Follow-up vector control action details 3\",\"description\":\"Follow-up vector control action details\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"Kv4fmHVAzwX\":[],\"uvMKOn1oWvd.Y14cBKFUsg4\":[],\"uvMKOn1oWvd.fADIatyOu2g\":[],\"CH6wamtY9kK\":[],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"CWaAcQYKVpq.DanTR5x0WDK\":[\"FZwy4OoRZtV\"],\"Bpx0589u8y0\":[\"MAs88nJc9nL\",\"PVLOW4bCshG\",\"w0gFTTmsUcF\",\"oRVt7g429ZO\"],\"zyTL3AMIkf2\":[],\"uvMKOn1oWvd.pKj8YrNKVda\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "Bpx0589u8y0", "Facility Ownership", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 2, "lastupdated", "Last updated on", "DATE", "java.time.LocalDate", false, true);
    validateHeader(
        response, 3, "Kv4fmHVAzwX", "Focus Name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        4,
        "CH6wamtY9kK",
        "Foci investigations performed & classified",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        5,
        "uvMKOn1oWvd.pKj8YrNKVda",
        "Follow-up vector control action details 3",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        6,
        "uvMKOn1oWvd.Y14cBKFUsg4",
        "Follow-up vector control action details 2",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        7,
        "uvMKOn1oWvd.fADIatyOu2g",
        "LLIN coverage (%)",
        "PERCENTAGE",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        8,
        "CWaAcQYKVpq.DanTR5x0WDK",
        "Residence of the malaria case/s  that prompted the current case investigation",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of(
            "Njandama MCHP",
            "oRVt7g429ZO",
            "2019-08-21 13:29:14.578",
            "Village 32 Sarah Baartman",
            "1",
            "",
            "",
            "",
            ""));
    validateRow(
        response,
        1,
        List.of(
            "Ngelehun CHC",
            "oRVt7g429ZO",
            "2019-08-21 13:29:21.574",
            "Focus A focus",
            "1",
            "",
            "",
            "",
            "RESIDENT_IN_THE_FOCUS"));
    validateRow(
        response,
        2,
        List.of(
            "Njandama MCHP",
            "oRVt7g429ZO",
            "2019-08-21 13:29:24.678",
            "Village 28 Oliver Tambo District",
            "1",
            "",
            "",
            "",
            "RESIDENT_IN_THE_FOCUS"));
    validateRow(
        response,
        3,
        List.of(
            "Ngelehun CHC",
            "oRVt7g429ZO",
            "2019-08-21 13:29:28.064",
            "Village 5 Alfred Nzo Municipality",
            "0",
            "",
            "",
            "",
            ""));
    validateRow(
        response,
        4,
        List.of(
            "Njandama MCHP",
            "oRVt7g429ZO",
            "2019-08-21 13:29:31.708",
            "Village C",
            "1",
            "",
            "",
            "",
            ""));
    validateRow(
        response,
        5,
        List.of(
            "Njandama MCHP",
            "oRVt7g429ZO",
            "2019-08-21 13:29:37.117",
            "Village B",
            "1",
            "",
            "",
            "",
            "RESIDENT_IN_THE_FOCUS"));
    validateRow(
        response,
        6,
        List.of(
            "Ngelehun CHC",
            "oRVt7g429ZO",
            "2019-08-21 13:29:39.311",
            "Village 17 Alfred Nzo Municipality",
            "0",
            "",
            "",
            "",
            ""));
    validateRow(
        response,
        7,
        List.of(
            "Ngelehun CHC",
            "oRVt7g429ZO",
            "2019-08-21 13:29:44.942",
            "Test focus",
            "1",
            "",
            "",
            "",
            ""));
  }

  @Test
  public void queryRandom6() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=uvMKOn1oWvd.fADIatyOu2g:EQ:NV")
            .add("includeMetadataDetails=true")
            .add(
                "headers=ouname,Bpx0589u8y0,lastupdated,coaSpbzZiTB,d6Sr0B2NJYv,uvMKOn1oWvd.Qvb7NExMqjZ")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("enrollmentDate=2021")
            .add("outputType=ENROLLMENT")
            .add("pageSize=100")
            .add("page=1")
            .add(
                "dimension=ou:ImspTQPwCqd,Bpx0589u8y0,coaSpbzZiTB,d6Sr0B2NJYv,uvMKOn1oWvd.Qvb7NExMqjZ")
            .add("relativePeriodDate=2023-07-14");

    // When
    ApiResponse response = actions.query().get("M3xtLkYBlKI", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(6)))
        .body("rows", hasSize(equalTo(8)))
        .body("height", equalTo(8))
        .body("width", equalTo(6))
        .body("headerWidth", equalTo(6));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"uvMKOn1oWvd.Qvb7NExMqjZ\":{\"uid\":\"Qvb7NExMqjZ\",\"name\":\"Follow-up vector control actions 5\",\"description\":\"Follow-up vector control actions\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"fADIatyOu2g\":{\"uid\":\"fADIatyOu2g\",\"name\":\"LLIN coverage (%)\",\"description\":\"LLIN coverage following a foci response\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"coaSpbzZiTB\":{\"uid\":\"coaSpbzZiTB\",\"name\":\"System Focus ID\",\"description\":\"System Focus ID\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"2021\":{\"name\":\"2021\"},\"MAs88nJc9nL\":{\"uid\":\"MAs88nJc9nL\",\"code\":\"Private Clinic\",\"name\":\"Private Clinic\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"uvMKOn1oWvd.fADIatyOu2g\":{\"uid\":\"fADIatyOu2g\",\"name\":\"LLIN coverage (%)\",\"description\":\"LLIN coverage following a foci response\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"d6Sr0B2NJYv\":{\"uid\":\"d6Sr0B2NJYv\",\"name\":\"Foci investigations performed\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"COUNT\",\"totalAggregationType\":\"SUM\"},\"Qvb7NExMqjZ\":{\"uid\":\"Qvb7NExMqjZ\",\"name\":\"Follow-up vector control actions 5\",\"description\":\"Follow-up vector control actions\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"CWaAcQYKVpq\":{\"uid\":\"CWaAcQYKVpq\",\"name\":\"Foci investigation & classification\",\"description\":\"Includes the details on the foci investigation (including information on households, population, geography, breeding sites, species types, vector behaviour) as well as its final classification at the time of the investigation. This is a repeatable stage as foci can be investigated more than once and may change their classification as time goes on. \"},\"PVLOW4bCshG\":{\"uid\":\"PVLOW4bCshG\",\"code\":\"NGO\",\"name\":\"NGO\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"oRVt7g429ZO\":{\"uid\":\"oRVt7g429ZO\",\"code\":\"Public facilities\",\"name\":\"Public facilities\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"Bpx0589u8y0\":{\"uid\":\"Bpx0589u8y0\",\"name\":\"Facility Ownership\",\"dimensionType\":\"ORGANISATION_UNIT_GROUP_SET\"},\"uvMKOn1oWvd\":{\"uid\":\"uvMKOn1oWvd\",\"name\":\"Foci response\",\"description\":\"Details the public health response conducted within the foci  (including diagnosis and treatment activities, vector control actions and the effectiveness/results of the response). This is a repeatable stage as multiple public health responses for the same foci can occur depending on its classification at the time of investigation.\"},\"w0gFTTmsUcF\":{\"uid\":\"w0gFTTmsUcF\",\"code\":\"Mission\",\"name\":\"Mission\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"M3xtLkYBlKI\":{\"uid\":\"M3xtLkYBlKI\",\"name\":\"Malaria focus investigation\",\"description\":\"It allows to register new focus areas in the system. Each focus area needs to be investigated and classified. Includes the relevant identifiers for the foci including the name and geographical details including the locality and its area. \"}},\"dimensions\":{\"d6Sr0B2NJYv\":[],\"uvMKOn1oWvd.Qvb7NExMqjZ\":[],\"fADIatyOu2g\":[\"= NV\"],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"coaSpbzZiTB\":[],\"Bpx0589u8y0\":[\"MAs88nJc9nL\",\"PVLOW4bCshG\",\"w0gFTTmsUcF\",\"oRVt7g429ZO\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "Bpx0589u8y0", "Facility Ownership", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 2, "lastupdated", "Last updated on", "DATE", "java.time.LocalDate", false, true);
    validateHeader(
        response, 3, "coaSpbzZiTB", "System Focus ID", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        4,
        "d6Sr0B2NJYv",
        "Foci investigations performed",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        5,
        "uvMKOn1oWvd.Qvb7NExMqjZ",
        "Follow-up vector control actions 5",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of("Ngelehun CHC", "oRVt7g429ZO", "2019-08-21 13:29:21.574", "FSL054948", "1", ""));
    validateRow(
        response,
        1,
        List.of("Njandama MCHP", "oRVt7g429ZO", "2019-08-21 13:29:14.578", "FJY949720", "2", ""));
    validateRow(
        response,
        2,
        List.of("Ngelehun CHC", "oRVt7g429ZO", "2019-08-21 13:29:39.311", "UGB082875", "1", ""));
    validateRow(
        response,
        3,
        List.of("Njandama MCHP", "oRVt7g429ZO", "2019-08-21 13:29:37.117", "WQQ003161", "1", ""));
    validateRow(
        response,
        4,
        List.of("Njandama MCHP", "oRVt7g429ZO", "2019-08-21 13:29:24.678", "VLI743922", "1", ""));
    validateRow(
        response,
        5,
        List.of("Ngelehun CHC", "oRVt7g429ZO", "2019-08-21 13:29:28.064", "ZDA984904", "1", ""));
    validateRow(
        response,
        6,
        List.of("Njandama MCHP", "oRVt7g429ZO", "2019-08-21 13:29:31.708", "MCY630042", "1", ""));
    validateRow(
        response,
        7,
        List.of("Ngelehun CHC", "oRVt7g429ZO", "2019-08-21 13:29:44.942", "MGC694579", "2", ""));
  }

  @Test
  public void queryRandom7() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=uvMKOn1oWvd.fADIatyOu2g:EQ:NV,CWaAcQYKVpq.XjcDg2kOmqf:GT:1")
            .add("includeMetadataDetails=true")
            .add(
                "headers=ouname,Bpx0589u8y0,lastupdated,coaSpbzZiTB,d6Sr0B2NJYv,uvMKOn1oWvd.Qvb7NExMqjZ,uvMKOn1oWvd.DX4LVYeP7bw,ffbaoqebOT3")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("enrollmentDate=2021")
            .add("outputType=ENROLLMENT")
            .add("pageSize=100")
            .add("page=1")
            .add(
                "dimension=ou:ImspTQPwCqd,Bpx0589u8y0,coaSpbzZiTB,d6Sr0B2NJYv,uvMKOn1oWvd.Qvb7NExMqjZ,uvMKOn1oWvd.DX4LVYeP7bw,ffbaoqebOT3")
            .add("relativePeriodDate=2023-07-14");

    // When
    ApiResponse response = actions.query().get("M3xtLkYBlKI", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(8)))
        .body("rows", hasSize(equalTo(1)))
        .body("height", equalTo(1))
        .body("width", equalTo(8))
        .body("headerWidth", equalTo(8));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"uvMKOn1oWvd.Qvb7NExMqjZ\":{\"uid\":\"Qvb7NExMqjZ\",\"name\":\"Follow-up vector control actions 5\",\"description\":\"Follow-up vector control actions\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"fADIatyOu2g\":{\"uid\":\"fADIatyOu2g\",\"name\":\"LLIN coverage (%)\",\"description\":\"LLIN coverage following a foci response\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"coaSpbzZiTB\":{\"uid\":\"coaSpbzZiTB\",\"name\":\"System Focus ID\",\"description\":\"System Focus ID\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"XjcDg2kOmqf\":{\"uid\":\"XjcDg2kOmqf\",\"name\":\"Malaria confirmed Pv (foci)\",\"description\":\"The number of Pv malariacases  identified in the a foci during foci investigation\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"2021\":{\"name\":\"2021\"},\"MAs88nJc9nL\":{\"uid\":\"MAs88nJc9nL\",\"code\":\"Private Clinic\",\"name\":\"Private Clinic\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"CWaAcQYKVpq.XjcDg2kOmqf\":{\"uid\":\"XjcDg2kOmqf\",\"name\":\"Malaria confirmed Pv (foci)\",\"description\":\"The number of Pv malariacases  identified in the a foci during foci investigation\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"uvMKOn1oWvd.fADIatyOu2g\":{\"uid\":\"fADIatyOu2g\",\"name\":\"LLIN coverage (%)\",\"description\":\"LLIN coverage following a foci response\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"d6Sr0B2NJYv\":{\"uid\":\"d6Sr0B2NJYv\",\"name\":\"Foci investigations performed\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"COUNT\",\"totalAggregationType\":\"SUM\"},\"Qvb7NExMqjZ\":{\"uid\":\"Qvb7NExMqjZ\",\"name\":\"Follow-up vector control actions 5\",\"description\":\"Follow-up vector control actions\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"CWaAcQYKVpq\":{\"uid\":\"CWaAcQYKVpq\",\"name\":\"Foci investigation & classification\",\"description\":\"Includes the details on the foci investigation (including information on households, population, geography, breeding sites, species types, vector behaviour) as well as its final classification at the time of the investigation. This is a repeatable stage as foci can be investigated more than once and may change their classification as time goes on. \"},\"PVLOW4bCshG\":{\"uid\":\"PVLOW4bCshG\",\"code\":\"NGO\",\"name\":\"NGO\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"ffbaoqebOT3\":{\"uid\":\"ffbaoqebOT3\",\"name\":\"Name of health facility catchment area\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"DX4LVYeP7bw\":{\"uid\":\"DX4LVYeP7bw\",\"name\":\"People included\",\"description\":\"Number of people included\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"oRVt7g429ZO\":{\"uid\":\"oRVt7g429ZO\",\"code\":\"Public facilities\",\"name\":\"Public facilities\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"Bpx0589u8y0\":{\"uid\":\"Bpx0589u8y0\",\"name\":\"Facility Ownership\",\"dimensionType\":\"ORGANISATION_UNIT_GROUP_SET\"},\"uvMKOn1oWvd\":{\"uid\":\"uvMKOn1oWvd\",\"name\":\"Foci response\",\"description\":\"Details the public health response conducted within the foci  (including diagnosis and treatment activities, vector control actions and the effectiveness/results of the response). This is a repeatable stage as multiple public health responses for the same foci can occur depending on its classification at the time of investigation.\"},\"w0gFTTmsUcF\":{\"uid\":\"w0gFTTmsUcF\",\"code\":\"Mission\",\"name\":\"Mission\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"M3xtLkYBlKI\":{\"uid\":\"M3xtLkYBlKI\",\"name\":\"Malaria focus investigation\",\"description\":\"It allows to register new focus areas in the system. Each focus area needs to be investigated and classified. Includes the relevant identifiers for the foci including the name and geographical details including the locality and its area. \"},\"uvMKOn1oWvd.DX4LVYeP7bw\":{\"uid\":\"DX4LVYeP7bw\",\"name\":\"People included\",\"description\":\"Number of people included\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"d6Sr0B2NJYv\":[],\"uvMKOn1oWvd.Qvb7NExMqjZ\":[],\"fADIatyOu2g\":[\"= NV\"],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"coaSpbzZiTB\":[],\"XjcDg2kOmqf\":[\"> 1\"],\"ffbaoqebOT3\":[],\"Bpx0589u8y0\":[\"MAs88nJc9nL\",\"PVLOW4bCshG\",\"w0gFTTmsUcF\",\"oRVt7g429ZO\"],\"uvMKOn1oWvd.DX4LVYeP7bw\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "Bpx0589u8y0", "Facility Ownership", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 2, "lastupdated", "Last updated on", "DATE", "java.time.LocalDate", false, true);
    validateHeader(
        response, 3, "coaSpbzZiTB", "System Focus ID", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        4,
        "d6Sr0B2NJYv",
        "Foci investigations performed",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        5,
        "uvMKOn1oWvd.Qvb7NExMqjZ",
        "Follow-up vector control actions 5",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        6,
        "uvMKOn1oWvd.DX4LVYeP7bw",
        "People included",
        "INTEGER_POSITIVE",
        "java.lang.Integer",
        false,
        true);
    validateHeader(
        response,
        7,
        "ffbaoqebOT3",
        "Name of health facility catchment area",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of(
            "Ngelehun CHC",
            "oRVt7g429ZO",
            "2019-08-21 13:29:21.574",
            "FSL054948",
            "1",
            "",
            "20",
            "Focus A"));
  }

  @Test
  public void queryRandom8() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add(
                "headers=ouname,Bpx0589u8y0,lastupdated,rXoaHGAXWy9,fM7RZGVndZE,eo73fim1b2i,tt54DiKuQ9c,A03MvHHogjR.X8zyunlgUfM,cejWyOfXge6,w75KJ2mc4zz,enrollmentdate")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("enrollmentDate=LAST_5_YEARS")
            .add("outputType=ENROLLMENT")
            .add("pageSize=100")
            .add("page=1")
            .add("incidentDate=2018")
            .add(
                "dimension=ou:ImspTQPwCqd,Bpx0589u8y0,rXoaHGAXWy9:GE:3,fM7RZGVndZE:GT:0.2,eo73fim1b2i,tt54DiKuQ9c,A03MvHHogjR.X8zyunlgUfM,cejWyOfXge6:IN:Female,w75KJ2mc4zz")
            .add("relativePeriodDate=2028-01-01");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(11)))
        .body("rows", hasSize(equalTo(100)))
        .body("height", equalTo(100))
        .body("width", equalTo(11))
        .body("headerWidth", equalTo(11));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":false},\"items\":{\"X8zyunlgUfM\":{\"uid\":\"X8zyunlgUfM\",\"code\":\"DE_2006103\",\"name\":\"MCH Infant Feeding\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"Mnp3oXrpAbK\":{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\",\"name\":\"Female\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"w75KJ2mc4zz\":{\"uid\":\"w75KJ2mc4zz\",\"code\":\"MMD_PER_NAM\",\"name\":\"First name\",\"description\":\"First name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"LAST_5_YEARS\":{\"name\":\"Last 5 years\"},\"MAs88nJc9nL\":{\"uid\":\"MAs88nJc9nL\",\"code\":\"Private Clinic\",\"name\":\"Private Clinic\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"eo73fim1b2i\":{\"uid\":\"eo73fim1b2i\",\"name\":\"Measles + Yellow fever doses female\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"bS16xfd2E1F\":{\"uid\":\"bS16xfd2E1F\",\"code\":\"Exclusive\",\"name\":\"Exclusive\"},\"cejWyOfXge6\":{\"uid\":\"cejWyOfXge6\",\"name\":\"Gender\",\"description\":\"Gender\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"tt54DiKuQ9c\":{\"uid\":\"tt54DiKuQ9c\",\"name\":\"Measles + Yellow fever doses low infant weight\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"2018\":{\"name\":\"2018\"},\"rXoaHGAXWy9\":{\"uid\":\"rXoaHGAXWy9\",\"name\":\"Health immunization score\",\"description\":\"Sum of BCG doses, measles doses and yellow fever doses. If Apgar score over or equal to 2, multiply by 2.\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"odMfnhhpjUj\":{\"uid\":\"odMfnhhpjUj\",\"code\":\"Mixed\",\"name\":\"Mixed\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"fLCgjvxrw4c\":{\"uid\":\"fLCgjvxrw4c\",\"code\":\"Replacement\",\"name\":\"Replacement\"},\"A03MvHHogjR.X8zyunlgUfM\":{\"uid\":\"X8zyunlgUfM\",\"code\":\"DE_2006103\",\"name\":\"MCH Infant Feeding\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"fM7RZGVndZE\":{\"uid\":\"fM7RZGVndZE\",\"name\":\"Measles + Yellow fever doses\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"PVLOW4bCshG\":{\"uid\":\"PVLOW4bCshG\",\"code\":\"NGO\",\"name\":\"NGO\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"oRVt7g429ZO\":{\"uid\":\"oRVt7g429ZO\",\"code\":\"Public facilities\",\"name\":\"Public facilities\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"Bpx0589u8y0\":{\"uid\":\"Bpx0589u8y0\",\"name\":\"Facility Ownership\",\"dimensionType\":\"ORGANISATION_UNIT_GROUP_SET\"},\"w0gFTTmsUcF\":{\"uid\":\"w0gFTTmsUcF\",\"code\":\"Mission\",\"name\":\"Mission\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"tt54DiKuQ9c\":[],\"rXoaHGAXWy9\":[],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"A03MvHHogjR.X8zyunlgUfM\":[\"bS16xfd2E1F\",\"fLCgjvxrw4c\",\"odMfnhhpjUj\"],\"fM7RZGVndZE\":[],\"Bpx0589u8y0\":[\"MAs88nJc9nL\",\"PVLOW4bCshG\",\"w0gFTTmsUcF\",\"oRVt7g429ZO\"],\"eo73fim1b2i\":[],\"cejWyOfXge6\":[\"Mnp3oXrpAbK\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "Bpx0589u8y0", "Facility Ownership", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 2, "lastupdated", "Last updated on", "DATE", "java.time.LocalDate", false, true);
    validateHeader(
        response,
        3,
        "rXoaHGAXWy9",
        "Health immunization score",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        4,
        "fM7RZGVndZE",
        "Measles + Yellow fever doses",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        5,
        "eo73fim1b2i",
        "Measles + Yellow fever doses female",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        6,
        "tt54DiKuQ9c",
        "Measles + Yellow fever doses low infant weight",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        7,
        "A03MvHHogjR.X8zyunlgUfM",
        "MCH Infant Feeding",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(response, 8, "cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 9, "w75KJ2mc4zz", "First name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        10,
        "enrollmentdate",
        "Date of enrollment",
        "DATE",
        "java.time.LocalDate",
        false,
        true);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of(
            "Rina Clinic",
            "",
            "2018-08-07 15:47:19.767",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Christina",
            "2023-04-26 12:05:00.0"));
    validateRow(
        response,
        1,
        List.of(
            "Juma MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:20.037",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Lori",
            "2023-12-04 12:05:00.0"));
    validateRow(
        response,
        2,
        List.of(
            "Koidu Under Five Clinic",
            "oRVt7g429ZO",
            "2018-08-07 15:47:20.945",
            "3.0",
            "1.0",
            "1.0",
            "1.0",
            "Replacement",
            "Female",
            "Joyce",
            "2023-09-06 12:05:00.0"));
    validateRow(
        response,
        3,
        List.of(
            "Pejewa MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:21.399",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Marie",
            "2023-03-06 12:05:00.0"));
    validateRow(
        response,
        4,
        List.of(
            "Mamankie MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:21.443",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Katherine",
            "2023-05-01 12:05:00.0"));
    validateRow(
        response,
        5,
        List.of(
            "Foindu MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:21.455",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Emily",
            "2023-08-03 12:05:00.0"));
    validateRow(
        response,
        6,
        List.of(
            "Bangoma MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:21.565",
            "3.0",
            "1.0",
            "1.0",
            "1.0",
            "Exclusive",
            "Female",
            "Judy",
            "2023-03-24 12:05:00.0"));
    validateRow(
        response,
        7,
        List.of(
            "Mapailleh MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:21.617",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Diana",
            "2023-07-12 12:05:00.0"));
    validateRow(
        response,
        8,
        List.of(
            "UFC Port Loko",
            "oRVt7g429ZO",
            "2018-08-07 15:47:21.95",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Tammy",
            "2023-10-07 12:05:00.0"));
    validateRow(
        response,
        9,
        List.of(
            "Mano Sewallu CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:21.982",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Karen",
            "2023-03-25 12:05:00.0"));
    validateRow(
        response,
        10,
        List.of(
            "Pelewahun (Baoma) MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:21.995",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Stephanie",
            "2023-01-01 12:05:00.0"));
    validateRow(
        response,
        11,
        List.of(
            "Ngiewahun CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.03",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Angela",
            "2023-11-28 12:05:00.0"));
    validateRow(
        response,
        12,
        List.of(
            "Mangay Loko MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.124",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Louise",
            "2023-03-15 12:05:00.0"));
    validateRow(
        response,
        13,
        List.of(
            "Bandajuma Sinneh MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.159",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Brenda",
            "2023-03-19 12:05:00.0"));
    validateRow(
        response,
        14,
        List.of(
            "Mabonkanie MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.169",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Katherine",
            "2023-07-09 12:05:00.0"));
    validateRow(
        response,
        15,
        List.of(
            "Kumrabai Yoni MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.27",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Nicole",
            "2023-07-12 12:05:00.0"));
    validateRow(
        response,
        16,
        List.of(
            "Kpayama 1 MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.397",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Debra",
            "2023-07-29 12:05:00.0"));
    validateRow(
        response,
        17,
        List.of(
            "Arab Clinic",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.484",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Lori",
            "2023-09-16 12:05:00.0"));
    validateRow(
        response,
        18,
        List.of(
            "Hamilton MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.489",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Paula",
            "2023-02-07 12:05:00.0"));
    validateRow(
        response,
        19,
        List.of(
            "Rapha Clinic",
            "",
            "2018-08-07 15:47:22.511",
            "3.0",
            "1.0",
            "1.0",
            "1.0",
            "Mixed",
            "Female",
            "Janet",
            "2023-11-17 12:05:00.0"));
    validateRow(
        response,
        20,
        List.of(
            "Approved School CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.539",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Amanda",
            "2023-05-18 12:05:00.0"));
    validateRow(
        response,
        21,
        List.of(
            "Bandajuma Kpolihun CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.575",
            "3.0",
            "1.0",
            "1.0",
            "1.0",
            "Exclusive",
            "Female",
            "Teresa",
            "2023-01-11 12:05:00.0"));
    validateRow(
        response,
        22,
        List.of(
            "Doujou CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.592",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Kimberly",
            "2023-08-15 12:05:00.0"));
    validateRow(
        response,
        23,
        List.of(
            "Rofutha MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.658",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Julia",
            "2023-05-20 12:05:00.0"));
    validateRow(
        response,
        24,
        List.of(
            "Mathinkalol MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.824",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Kathy",
            "2023-09-05 12:05:00.0"));
    validateRow(
        response,
        25,
        List.of(
            "Fothaneh Bana MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.883",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Ann",
            "2023-06-20 12:05:00.0"));
    validateRow(
        response,
        26,
        List.of(
            "Kawaya MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.915",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Anna",
            "2023-05-06 12:05:00.0"));
    validateRow(
        response,
        27,
        List.of(
            "St Anthony clinic",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.916",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Janice",
            "2023-03-28 12:05:00.0"));
    validateRow(
        response,
        28,
        List.of(
            "Masofinia MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.956",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Rachel",
            "2023-09-12 12:05:00.0"));
    validateRow(
        response,
        29,
        List.of(
            "Holy Mary Hospital",
            "MAs88nJc9nL",
            "2018-08-07 15:47:22.963",
            "3.0",
            "1.0",
            "1.0",
            "1.0",
            "Replacement",
            "Female",
            "Kimberly",
            "2023-01-21 12:05:00.0"));
    validateRow(
        response,
        30,
        List.of(
            "Mateboi CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.976",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Joan",
            "2023-11-04 12:05:00.0"));
    validateRow(
        response,
        31,
        List.of(
            "Mokassie MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:23.113",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Deborah",
            "2023-02-14 12:05:00.0"));
    validateRow(
        response,
        32,
        List.of(
            "Mafaray CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:23.37",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Judy",
            "2023-02-11 12:05:00.0"));
    validateRow(
        response,
        33,
        List.of(
            "Kagbaneh CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:23.455",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Barbara",
            "2023-03-05 12:05:00.0"));
    validateRow(
        response,
        34,
        List.of(
            "Bundulai MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:23.461",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Ruby",
            "2023-02-24 12:05:00.0"));
    validateRow(
        response,
        35,
        List.of(
            "Mafoimara MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:23.51",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Jane",
            "2023-03-14 12:05:00.0"));
    validateRow(
        response,
        36,
        List.of(
            "Ngogbebu MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:23.535",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Rebecca",
            "2023-04-05 12:05:00.0"));
    validateRow(
        response,
        37,
        List.of(
            "Sinkunia CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:23.579",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Michelle",
            "2023-08-29 12:05:00.0"));
    validateRow(
        response,
        38,
        List.of(
            "Bendoma (Malegohun) MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:23.61",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Angela",
            "2023-01-28 12:05:00.0"));
    validateRow(
        response,
        39,
        List.of(
            "Makaiba MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:23.744",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Bonnie",
            "2023-05-03 12:05:00.0"));
    validateRow(
        response,
        40,
        List.of(
            "Rosinor CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:23.756",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Dorothy",
            "2023-10-24 12:05:00.0"));
    validateRow(
        response,
        41,
        List.of(
            "Sierra Rutile Clinic",
            "oRVt7g429ZO",
            "2018-08-07 15:47:23.776",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Melissa",
            "2023-05-07 12:05:00.0"));
    validateRow(
        response,
        42,
        List.of(
            "kamba mamudia",
            "",
            "2018-08-07 15:47:23.839",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Paula",
            "2023-04-02 12:05:00.0"));
    validateRow(
        response,
        43,
        List.of(
            "Taiama (Kori) CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:23.874",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Sharon",
            "2023-05-02 12:05:00.0"));
    validateRow(
        response,
        44,
        List.of(
            "Bomie MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:23.897",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Beverly",
            "2023-10-10 12:05:00.0"));
    validateRow(
        response,
        45,
        List.of(
            "Tugbebu CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:23.92",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Joyce",
            "2023-02-03 12:05:00.0"));
    validateRow(
        response,
        46,
        List.of(
            "Tokpombu MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:23.938",
            "3.0",
            "1.0",
            "1.0",
            "1.0",
            "Exclusive",
            "Female",
            "Norma",
            "2023-10-11 12:05:00.0"));
    validateRow(
        response,
        47,
        List.of(
            "Banka Makuloh MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:23.979",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Ann",
            "2023-08-28 12:05:00.0"));
    validateRow(
        response,
        48,
        List.of(
            "Kalangba BKM MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:24.079",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Mildred",
            "2023-01-10 12:05:00.0"));
    validateRow(
        response,
        49,
        List.of(
            "Karlu CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:24.09",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Christina",
            "2023-02-28 12:05:00.0"));
    validateRow(
        response,
        50,
        List.of(
            "Fintonia CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:24.184",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Robin",
            "2023-09-25 12:05:00.0"));
    validateRow(
        response,
        51,
        List.of(
            "Boroma MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:24.231",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Joan",
            "2023-02-20 12:05:00.0"));
    validateRow(
        response,
        52,
        List.of(
            "Calaba town CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:24.257",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Marilyn",
            "2023-08-03 12:05:00.0"));
    validateRow(
        response,
        53,
        List.of(
            "New Maforkie CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:24.279",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Joan",
            "2023-09-22 12:05:00.0"));
    validateRow(
        response,
        54,
        List.of(
            "Pepel CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:24.4",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Christina",
            "2023-07-19 12:05:00.0"));
    validateRow(
        response,
        55,
        List.of(
            "Koidu Under Five Clinic",
            "oRVt7g429ZO",
            "2018-08-07 15:47:24.414",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Patricia",
            "2023-08-18 12:05:00.0"));
    validateRow(
        response,
        56,
        List.of(
            "Gbenikoro MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:24.423",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Jean",
            "2023-04-08 12:05:00.0"));
    validateRow(
        response,
        57,
        List.of(
            "Vaama MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:24.504",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Julia",
            "2023-10-25 12:05:00.0"));
    validateRow(
        response,
        58,
        List.of(
            "Saiama MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:24.518",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Shirley",
            "2023-09-28 12:05:00.0"));
    validateRow(
        response,
        59,
        List.of(
            "Bapuya MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:24.524",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Julia",
            "2023-12-05 12:05:00.0"));
    validateRow(
        response,
        60,
        List.of(
            "PCM Hospital",
            "oRVt7g429ZO",
            "2018-08-07 15:47:24.619",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Evelyn",
            "2023-06-19 12:05:00.0"));
    validateRow(
        response,
        61,
        List.of(
            "Royeiben MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:24.625",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Margaret",
            "2023-05-19 12:05:00.0"));
    validateRow(
        response,
        62,
        List.of(
            "Makump Bana MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:24.626",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Kathy",
            "2023-08-19 12:05:00.0"));
    validateRow(
        response,
        63,
        List.of(
            "Seria MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:24.717",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Christine",
            "2023-05-15 12:05:00.0"));
    validateRow(
        response,
        64,
        List.of(
            "Ola During Clinic",
            "oRVt7g429ZO",
            "2018-08-07 15:47:24.777",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Dorothy",
            "2023-12-02 12:05:00.0"));
    validateRow(
        response,
        65,
        List.of(
            "Kondeya (Sandor) MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:24.905",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Donna",
            "2023-10-28 12:05:00.0"));
    validateRow(
        response,
        66,
        List.of(
            "Peya MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:24.922",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Marilyn",
            "2023-04-04 12:05:00.0"));
    validateRow(
        response,
        67,
        List.of(
            "Nasarah Clinic",
            "oRVt7g429ZO",
            "2018-08-07 15:47:24.947",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Lois",
            "2023-10-03 12:05:00.0"));
    validateRow(
        response,
        68,
        List.of(
            "Bumpeh Perri CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:25.059",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Dorothy",
            "2023-01-16 12:05:00.0"));
    validateRow(
        response,
        69,
        List.of(
            "Makoba Bana MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:25.068",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Jane",
            "2023-08-08 12:05:00.0"));
    validateRow(
        response,
        70,
        List.of(
            "Mattru Jong MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:25.131",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Lisa",
            "2023-01-05 12:05:00.0"));
    validateRow(
        response,
        71,
        List.of(
            "Goderich Health Centre",
            "oRVt7g429ZO",
            "2018-08-07 15:47:25.15",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Carolyn",
            "2023-03-15 12:05:00.0"));
    validateRow(
        response,
        72,
        List.of(
            "Mbowohun CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:25.18",
            "3.0",
            "1.0",
            "1.0",
            "1.0",
            "Mixed",
            "Female",
            "Ann",
            "2023-08-06 12:05:00.0"));
    validateRow(
        response,
        73,
        List.of(
            "Mange CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:25.183",
            "3.0",
            "1.0",
            "1.0",
            "1.0",
            "Mixed",
            "Female",
            "Sara",
            "2023-09-20 12:05:00.0"));
    validateRow(
        response,
        74,
        List.of(
            "Sembehun Mamagewor MCHP",
            "",
            "2018-08-07 15:47:25.212",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Jennifer",
            "2023-09-24 12:05:00.0"));
    validateRow(
        response,
        75,
        List.of(
            "Gbaiima CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:25.224",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Frances",
            "2023-06-24 12:05:00.0"));
    validateRow(
        response,
        76,
        List.of(
            "Ngegbwema CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:25.334",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Paula",
            "2023-05-24 12:05:00.0"));
    validateRow(
        response,
        77,
        List.of(
            "Ngogbebu MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:25.337",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Dorothy",
            "2023-11-18 12:05:00.0"));
    validateRow(
        response,
        78,
        List.of(
            "Royeama CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:25.379",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Bonnie",
            "2023-03-21 12:05:00.0"));
    validateRow(
        response,
        79,
        List.of(
            "Hinistas CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:25.652",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Angela",
            "2023-09-24 12:05:00.0"));
    validateRow(
        response,
        80,
        List.of(
            "Rochen Malal MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:25.686",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Susan",
            "2023-10-02 12:05:00.0"));
    validateRow(
        response,
        81,
        List.of(
            "Makonthanday MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:25.707",
            "3.0",
            "1.0",
            "1.0",
            "1.0",
            "Mixed",
            "Female",
            "Emily",
            "2023-08-18 12:05:00.0"));
    validateRow(
        response,
        82,
        List.of(
            "Gbentu CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:25.709",
            "3.0",
            "1.0",
            "1.0",
            "1.0",
            "Exclusive",
            "Female",
            "Alice",
            "2023-10-09 12:05:00.0"));
    validateRow(
        response,
        83,
        List.of(
            "Serabu (Koya) CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:25.721",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Sharon",
            "2023-10-29 12:05:00.0"));
    validateRow(
        response,
        84,
        List.of(
            "Kalangba BKM MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:25.725",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Jennifer",
            "2023-11-09 12:05:00.0"));
    validateRow(
        response,
        85,
        List.of(
            "Mambolo CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:25.818",
            "3.0",
            "1.0",
            "1.0",
            "1.0",
            "Replacement",
            "Female",
            "Martha",
            "2023-08-15 12:05:00.0"));
    validateRow(
        response,
        86,
        List.of(
            "St. John of God Catholic Hospital",
            "",
            "2018-08-07 15:47:25.822",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Shirley",
            "2023-11-29 12:05:00.0"));
    validateRow(
        response,
        87,
        List.of(
            "Rochen Malal MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:25.835",
            "3.0",
            "1.0",
            "1.0",
            "1.0",
            "Mixed",
            "Female",
            "Catherine",
            "2023-05-22 12:05:00.0"));
    validateRow(
        response,
        88,
        List.of(
            "Walia MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:25.844",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Diana",
            "2023-07-30 12:05:00.0"));
    validateRow(
        response,
        89,
        List.of(
            "Njagbwema CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:25.851",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Helen",
            "2023-03-22 12:05:00.0"));
    validateRow(
        response,
        90,
        List.of(
            "Fanima CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:25.865",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Betty",
            "2023-06-23 12:05:00.0"));
    validateRow(
        response,
        91,
        List.of(
            "Malema (Yawei) CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:25.876",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Sara",
            "2023-07-15 12:05:00.0"));
    validateRow(
        response,
        92,
        List.of(
            "Rosint Buya MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:25.887",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Linda",
            "2023-07-02 12:05:00.0"));
    validateRow(
        response,
        93,
        List.of(
            "Makonthanday MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:25.928",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Alice",
            "2023-04-24 12:05:00.0"));
    validateRow(
        response,
        94,
        List.of(
            "Makundu MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:26.065",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Anne",
            "2023-03-09 12:05:00.0"));
    validateRow(
        response,
        95,
        List.of(
            "Gbongongor CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:26.204",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Elizabeth",
            "2023-02-05 12:05:00.0"));
    validateRow(
        response,
        96,
        List.of(
            "Lengekoro MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:26.244",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Jessica",
            "2023-01-28 12:05:00.0"));
    validateRow(
        response,
        97,
        List.of(
            "Calaba town CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:26.259",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Teresa",
            "2023-11-03 12:05:00.0"));
    validateRow(
        response,
        98,
        List.of(
            "Gbomsamba MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:26.268",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Lois",
            "2023-03-14 12:05:00.0"));
    validateRow(
        response,
        99,
        List.of(
            "MCH (Kakua) Static",
            "oRVt7g429ZO",
            "2018-08-07 15:47:26.271",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Irene",
            "2023-02-27 12:05:00.0"));
  }
}
