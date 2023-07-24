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
            .add("relativePeriodDate=2028-01-01")
            .add("desc=lastupdated");

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
            "Yonibana MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:29.299",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Jennifer",
            "2023-03-23 12:05:00.0"));
    validateRow(
        response,
        1,
        List.of(
            "Bontiwo MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:29.296",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Barbara",
            "2023-08-21 12:05:00.0"));
    validateRow(
        response,
        2,
        List.of(
            "Falaba MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:29.273",
            "3.0",
            "1.0",
            "1.0",
            "1.0",
            "Replacement",
            "Female",
            "Julie",
            "2023-02-19 12:05:00.0"));
    validateRow(
        response,
        3,
        List.of(
            "Kakoya MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:29.263",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Gloria",
            "2023-01-25 12:05:00.0"));
    validateRow(
        response,
        4,
        List.of(
            "Kalainkay MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:29.255",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Jean",
            "2023-03-05 12:05:00.0"));
    validateRow(
        response,
        5,
        List.of(
            "Mayakie MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:29.253",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Jacqueline",
            "2023-02-16 12:05:00.0"));
    validateRow(
        response,
        6,
        List.of(
            "Niahun Gboyama MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:29.244",
            "3.0",
            "1.0",
            "1.0",
            "1.0",
            "Exclusive",
            "Female",
            "Patricia",
            "2023-01-07 12:05:00.0"));
    validateRow(
        response,
        7,
        List.of(
            "Romeni MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:29.235",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Kelly",
            "2023-10-18 12:05:00.0"));
    validateRow(
        response,
        8,
        List.of(
            "Kakoya MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:29.232",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Lois",
            "2023-08-02 12:05:00.0"));
    validateRow(
        response,
        9,
        List.of(
            "M I Room (Military)",
            "oRVt7g429ZO",
            "2018-08-07 15:47:29.212",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Katherine",
            "2023-11-23 12:05:00.0"));
    validateRow(
        response,
        10,
        List.of(
            "Mabom CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:29.186",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Norma",
            "2023-08-30 12:05:00.0"));
    validateRow(
        response,
        11,
        List.of(
            "Katherie MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:29.186",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Wanda",
            "2023-02-25 12:05:00.0"));
    validateRow(
        response,
        12,
        List.of(
            "Mamaka MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:29.169",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Judith",
            "2023-08-09 12:05:00.0"));
    validateRow(
        response,
        13,
        List.of(
            "Kangahun CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:29.142",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Theresa",
            "2023-10-25 12:05:00.0"));
    validateRow(
        response,
        14,
        List.of(
            "Koindu-kuntey MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:29.137",
            "3.0",
            "1.0",
            "1.0",
            "1.0",
            "Exclusive",
            "Female",
            "Diane",
            "2023-02-25 12:05:00.0"));
    validateRow(
        response,
        15,
        List.of(
            "Makump Bana MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:29.133",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Kelly",
            "2023-05-07 12:05:00.0"));
    validateRow(
        response,
        16,
        List.of(
            "Rokimbi MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:29.126",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Laura",
            "2023-10-12 12:05:00.0"));
    validateRow(
        response,
        17,
        List.of(
            "Yiffin CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:29.106",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Laura",
            "2023-01-25 12:05:00.0"));
    validateRow(
        response,
        18,
        List.of(
            "Saahun (barri) MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:29.041",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Ann",
            "2023-11-23 12:05:00.0"));
    validateRow(
        response,
        19,
        List.of(
            "Mafaray CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.995",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Catherine",
            "2023-04-06 12:05:00.0"));
    validateRow(
        response,
        20,
        List.of(
            "Tambiama CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.995",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Kathy",
            "2023-10-18 12:05:00.0"));
    validateRow(
        response,
        21,
        List.of(
            "Komrabai Ngolla MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.976",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Rose",
            "2023-09-05 12:05:00.0"));
    validateRow(
        response,
        22,
        List.of(
            "Royeama CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.975",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Emily",
            "2023-01-01 12:05:00.0"));
    validateRow(
        response,
        23,
        List.of(
            "Taninahun MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.953",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Carolyn",
            "2023-05-17 12:05:00.0"));
    validateRow(
        response,
        24,
        List.of(
            "Karleh MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.938",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Sharon",
            "2023-01-26 12:05:00.0"));
    validateRow(
        response,
        25,
        List.of(
            "Maraka MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.927",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Betty",
            "2023-08-24 12:05:00.0"));
    validateRow(
        response,
        26,
        List.of(
            "Kagbanthama CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.906",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Janice",
            "2023-11-11 12:05:00.0"));
    validateRow(
        response,
        27,
        List.of(
            "Baoma (Luawa) MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.882",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Kathryn",
            "2023-07-16 12:05:00.0"));
    validateRow(
        response,
        28,
        List.of(
            "Njama CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.873",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Jane",
            "2023-06-09 12:05:00.0"));
    validateRow(
        response,
        29,
        List.of(
            "Mathamp MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.866",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Sara",
            "2023-11-08 12:05:00.0"));
    validateRow(
        response,
        30,
        List.of(
            "Sandayeima MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.844",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Elizabeth",
            "2023-07-10 12:05:00.0"));
    validateRow(
        response,
        31,
        List.of(
            "Kabati CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.792",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Sara",
            "2023-02-20 12:05:00.0"));
    validateRow(
        response,
        32,
        List.of(
            "Tokpombu MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.784",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Karen",
            "2023-07-29 12:05:00.0"));
    validateRow(
        response,
        33,
        List.of(
            "Koeyor MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.771",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Christina",
            "2023-08-04 12:05:00.0"));
    validateRow(
        response,
        34,
        List.of(
            "Kamboma MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.733",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Catherine",
            "2023-04-20 12:05:00.0"));
    validateRow(
        response,
        35,
        List.of(
            "Mafoimara MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.726",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Kathleen",
            "2023-02-09 12:05:00.0"));
    validateRow(
        response,
        36,
        List.of(
            "Sussex MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.704",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Jean",
            "2023-10-31 12:05:00.0"));
    validateRow(
        response,
        37,
        List.of(
            "Kono Bendu CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.632",
            "3.0",
            "1.0",
            "1.0",
            "1.0",
            "Exclusive",
            "Female",
            "Helen",
            "2023-01-25 12:05:00.0"));
    validateRow(
        response,
        38,
        List.of(
            "Hunduwa CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.594",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Angela",
            "2023-06-29 12:05:00.0"));
    validateRow(
        response,
        39,
        List.of(
            "Konjo (Dama) CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.588",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Judith",
            "2023-05-27 12:05:00.0"));
    validateRow(
        response,
        40,
        List.of(
            "Griema MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.578",
            "3.0",
            "1.0",
            "1.0",
            "1.0",
            "Exclusive",
            "Female",
            "Ann",
            "2023-02-27 12:05:00.0"));
    validateRow(
        response,
        41,
        List.of(
            "Mabain MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.565",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Shirley",
            "2023-08-26 12:05:00.0"));
    validateRow(
        response,
        42,
        List.of(
            "Ngogbebu MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.538",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Joan",
            "2023-05-21 12:05:00.0"));
    validateRow(
        response,
        43,
        List.of(
            "Warrima MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.529",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Norma",
            "2023-07-30 12:05:00.0"));
    validateRow(
        response,
        44,
        List.of(
            "Kensay MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.511",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Tammy",
            "2023-09-29 12:05:00.0"));
    validateRow(
        response,
        45,
        List.of(
            "Bomotoke CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.503",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Alice",
            "2023-03-13 12:05:00.0"));
    validateRow(
        response,
        46,
        List.of(
            "Talia CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.496",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Jacqueline",
            "2023-03-18 12:05:00.0"));
    validateRow(
        response,
        47,
        List.of(
            "Woama MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.494",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Bonnie",
            "2023-01-29 12:05:00.0"));
    validateRow(
        response,
        48,
        List.of(
            "Niahun Gboyama MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.492",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Rebecca",
            "2023-08-18 12:05:00.0"));
    validateRow(
        response,
        49,
        List.of(
            "London (Blama) MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.462",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Julie",
            "2023-04-09 12:05:00.0"));
    validateRow(
        response,
        50,
        List.of(
            "Mabayo MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.43",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Sharon",
            "2023-03-26 12:05:00.0"));
    validateRow(
        response,
        51,
        List.of(
            "Gondama (Kamaje) CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.425",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Nancy",
            "2023-03-28 12:05:00.0"));
    validateRow(
        response,
        52,
        List.of(
            "Kanikay MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.421",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Beverly",
            "2023-11-14 12:05:00.0"));
    validateRow(
        response,
        53,
        List.of(
            "Mansundu (Sandor) MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.418",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Sara",
            "2023-04-17 12:05:00.0"));
    validateRow(
        response,
        54,
        List.of(
            "Madina Loko CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.417",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Tina",
            "2023-04-22 12:05:00.0"));
    validateRow(
        response,
        55,
        List.of(
            "Sandayeima MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.415",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Kimberly",
            "2023-11-03 12:05:00.0"));
    validateRow(
        response,
        56,
        List.of(
            "Suga MCHP",
            "",
            "2018-08-07 15:47:28.404",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Stephanie",
            "2023-09-17 12:05:00.0"));
    validateRow(
        response,
        57,
        List.of(
            "Gbo-Lambayama 2 MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.404",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Margaret",
            "2023-08-13 12:05:00.0"));
    validateRow(
        response,
        58,
        List.of(
            "Bauya (Kongbora) CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.384",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Susan",
            "2023-10-18 12:05:00.0"));
    validateRow(
        response,
        59,
        List.of(
            "Batkanu CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.383",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Susan",
            "2023-10-04 12:05:00.0"));
    validateRow(
        response,
        60,
        List.of(
            "Serabu (Small Bo) CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.382",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Mary",
            "2023-05-15 12:05:00.0"));
    validateRow(
        response,
        61,
        List.of(
            "Mano Njeigbla CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.356",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Carolyn",
            "2023-10-01 12:05:00.0"));
    validateRow(
        response,
        62,
        List.of(
            "Kanga MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.354",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Alice",
            "2023-01-30 12:05:00.0"));
    validateRow(
        response,
        63,
        List.of(
            "Gbenikoro MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.348",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Alice",
            "2023-09-23 12:05:00.0"));
    validateRow(
        response,
        64,
        List.of(
            "Thellia CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.313",
            "3.0",
            "1.0",
            "1.0",
            "1.0",
            "Replacement",
            "Female",
            "Kimberly",
            "2023-07-26 12:05:00.0"));
    validateRow(
        response,
        65,
        List.of(
            "Gbogbodo MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.302",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Christina",
            "2023-01-03 12:05:00.0"));
    validateRow(
        response,
        66,
        List.of(
            "St. Joseph's Clinic",
            "",
            "2018-08-07 15:47:28.263",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Catherine",
            "2023-09-16 12:05:00.0"));
    validateRow(
        response,
        67,
        List.of(
            "Serabu (Koya) CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.239",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Theresa",
            "2023-11-01 12:05:00.0"));
    validateRow(
        response,
        68,
        List.of(
            "Senehun CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.231",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Emily",
            "2023-05-21 12:05:00.0"));
    validateRow(
        response,
        69,
        List.of(
            "Sebengu MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.229",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Nancy",
            "2023-05-20 12:05:00.0"));
    validateRow(
        response,
        70,
        List.of(
            "Koakoyima CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.2",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Deborah",
            "2023-07-03 12:05:00.0"));
    validateRow(
        response,
        71,
        List.of(
            "Kormende MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.186",
            "3.0",
            "1.0",
            "1.0",
            "1.0",
            "Exclusive",
            "Female",
            "Virginia",
            "2023-08-16 12:05:00.0"));
    validateRow(
        response,
        72,
        List.of(
            "Mamuntha MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.17",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Louise",
            "2023-11-27 12:05:00.0"));
    validateRow(
        response,
        73,
        List.of(
            "Tungie CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.117",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Irene",
            "2023-03-27 12:05:00.0"));
    validateRow(
        response,
        74,
        List.of(
            "Gbolon MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.115",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Paula",
            "2023-04-30 12:05:00.0"));
    validateRow(
        response,
        75,
        List.of(
            "Nyandeyaima MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.088",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Christina",
            "2023-02-01 12:05:00.0"));
    validateRow(
        response,
        76,
        List.of(
            "Fodaya MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.08",
            "3.0",
            "1.0",
            "1.0",
            "1.0",
            "Exclusive",
            "Female",
            "Kathy",
            "2023-03-06 12:05:00.0"));
    validateRow(
        response,
        77,
        List.of(
            "Fanima CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.057",
            "3.0",
            "1.0",
            "1.0",
            "1.0",
            "Exclusive",
            "Female",
            "Betty",
            "2023-09-11 12:05:00.0"));
    validateRow(
        response,
        78,
        List.of(
            "Rogbaneh MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.051",
            "3.0",
            "1.0",
            "1.0",
            "1.0",
            "Mixed",
            "Female",
            "Linda",
            "2023-04-29 12:05:00.0"));
    validateRow(
        response,
        79,
        List.of(
            "Semewebu MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:28.02",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Catherine",
            "2023-12-11 12:05:00.0"));
    validateRow(
        response,
        80,
        List.of(
            "Peyima CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:26.336",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Marie",
            "2023-06-17 12:05:00.0"));
    validateRow(
        response,
        81,
        List.of(
            "Royeiben MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:26.313",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Laura",
            "2023-07-05 12:05:00.0"));
    validateRow(
        response,
        82,
        List.of(
            "Walia MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:26.305",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Catherine",
            "2023-08-29 12:05:00.0"));
    validateRow(
        response,
        83,
        List.of(
            "Moyowa MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:26.292",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Evelyn",
            "2023-11-13 12:05:00.0"));
    validateRow(
        response,
        84,
        List.of(
            "Feuror MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:26.278",
            "3.0",
            "1.0",
            "1.0",
            "1.0",
            "Replacement",
            "Female",
            "Michelle",
            "2023-09-04 12:05:00.0"));
    validateRow(
        response,
        85,
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
    validateRow(
        response,
        86,
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
        87,
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
        88,
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
        89,
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
        90,
        List.of(
            "Gbanja Town MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:26.107",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Jessica",
            "2023-11-21 12:05:00.0"));
    validateRow(
        response,
        91,
        List.of(
            "Mabureh CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:26.071",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Exclusive",
            "Female",
            "Deborah",
            "2023-04-16 12:05:00.0"));
    validateRow(
        response,
        92,
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
        93,
        List.of(
            "Govt. Hospital Moyamba",
            "oRVt7g429ZO",
            "2018-08-07 15:47:26.036",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Replacement",
            "Female",
            "Tammy",
            "2023-05-02 12:05:00.0"));
    validateRow(
        response,
        94,
        List.of(
            "Thompson Bay MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:25.99",
            "3.0",
            "1.0",
            "1.0",
            "1.0",
            "Exclusive",
            "Female",
            "Anna",
            "2023-01-09 12:05:00.0"));
    validateRow(
        response,
        95,
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
        96,
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
        97,
        List.of(
            "St. John of God Catholic Clinic",
            "",
            "2018-08-07 15:47:25.877",
            "3.0",
            "1.0",
            "1.0",
            "0.0",
            "Mixed",
            "Female",
            "Carol",
            "2023-03-25 12:05:00.0"));
    validateRow(
        response,
        98,
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
        99,
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
  }
}
