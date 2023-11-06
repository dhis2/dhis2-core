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
            .add("relativePeriodDate=2023-07-14")
            .add("desc=lastupdated");

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
        List.of("Ngelehun CHC", "oRVt7g429ZO", "2019-08-21 13:29:44.942", "MGC694579", "2", ""));
    validateRow(
        response,
        1,
        List.of("Ngelehun CHC", "oRVt7g429ZO", "2019-08-21 13:29:39.311", "UGB082875", "1", ""));
    validateRow(
        response,
        2,
        List.of("Njandama MCHP", "oRVt7g429ZO", "2019-08-21 13:29:37.117", "WQQ003161", "1", ""));
    validateRow(
        response,
        3,
        List.of("Njandama MCHP", "oRVt7g429ZO", "2019-08-21 13:29:31.708", "MCY630042", "1", ""));
    validateRow(
        response,
        4,
        List.of("Ngelehun CHC", "oRVt7g429ZO", "2019-08-21 13:29:28.064", "ZDA984904", "1", ""));
    validateRow(
        response,
        5,
        List.of("Njandama MCHP", "oRVt7g429ZO", "2019-08-21 13:29:24.678", "VLI743922", "1", ""));
    validateRow(
        response,
        6,
        List.of("Ngelehun CHC", "oRVt7g429ZO", "2019-08-21 13:29:21.574", "FSL054948", "1", ""));
    validateRow(
        response,
        7,
        List.of("Njandama MCHP", "oRVt7g429ZO", "2019-08-21 13:29:14.578", "FJY949720", "2", ""));
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
            .add("asc=lastupdated")
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
                "dimension=ou:ImspTQPwCqd,Bpx0589u8y0,rXoaHGAXWy9:GE:3,fM7RZGVndZE:GT:0.7,eo73fim1b2i,tt54DiKuQ9c,A03MvHHogjR.X8zyunlgUfM,cejWyOfXge6:IN:Female,w75KJ2mc4zz")
            .add("relativePeriodDate=2028-01-01");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(11)))
        .body("rows", hasSize(equalTo(0)))
        .body("height", equalTo(0))
        .body("width", equalTo(0))
        .body("headerWidth", equalTo(11));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"2018\":{\"name\":\"2018\"},\"X8zyunlgUfM\":{\"uid\":\"X8zyunlgUfM\",\"code\":\"DE_2006103\",\"name\":\"MCH Infant Feeding\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"Mnp3oXrpAbK\":{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\",\"name\":\"Female\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"w75KJ2mc4zz\":{\"uid\":\"w75KJ2mc4zz\",\"code\":\"MMD_PER_NAM\",\"name\":\"First name\",\"description\":\"First name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"LAST_5_YEARS\":{\"name\":\"Last 5 years\"},\"MAs88nJc9nL\":{\"uid\":\"MAs88nJc9nL\",\"code\":\"Private Clinic\",\"name\":\"Private Clinic\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"eo73fim1b2i\":{\"uid\":\"eo73fim1b2i\",\"name\":\"Measles + Yellow fever doses female\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"cejWyOfXge6\":{\"uid\":\"cejWyOfXge6\",\"name\":\"Gender\",\"description\":\"Gender\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"tt54DiKuQ9c\":{\"uid\":\"tt54DiKuQ9c\",\"name\":\"Measles + Yellow fever doses low infant weight\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"rXoaHGAXWy9\":{\"uid\":\"rXoaHGAXWy9\",\"name\":\"Health immunization score\",\"description\":\"Sum of BCG doses, measles doses and yellow fever doses. If Apgar score over or equal to 2, multiply by 2.\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"pC3N9N77UmT\":{\"uid\":\"pC3N9N77UmT\",\"name\":\"Gender\",\"options\":[{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\"}]},\"A03MvHHogjR.X8zyunlgUfM\":{\"uid\":\"X8zyunlgUfM\",\"code\":\"DE_2006103\",\"name\":\"MCH Infant Feeding\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"fM7RZGVndZE\":{\"uid\":\"fM7RZGVndZE\",\"name\":\"Measles + Yellow fever doses\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"PVLOW4bCshG\":{\"uid\":\"PVLOW4bCshG\",\"code\":\"NGO\",\"name\":\"NGO\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"oRVt7g429ZO\":{\"uid\":\"oRVt7g429ZO\",\"code\":\"Public facilities\",\"name\":\"Public facilities\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"Bpx0589u8y0\":{\"uid\":\"Bpx0589u8y0\",\"name\":\"Facility Ownership\",\"dimensionType\":\"ORGANISATION_UNIT_GROUP_SET\"},\"w0gFTTmsUcF\":{\"uid\":\"w0gFTTmsUcF\",\"code\":\"Mission\",\"name\":\"Mission\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"tt54DiKuQ9c\":[],\"rXoaHGAXWy9\":[],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"A03MvHHogjR.X8zyunlgUfM\":[\"Mnp3oXrpAbK\"],\"fM7RZGVndZE\":[],\"Bpx0589u8y0\":[\"MAs88nJc9nL\",\"PVLOW4bCshG\",\"oRVt7g429ZO\",\"w0gFTTmsUcF\"],\"eo73fim1b2i\":[],\"cejWyOfXge6\":[\"Mnp3oXrpAbK\"]}}";
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
  }
}
