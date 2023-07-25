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
            "Ngolahun Jabaty MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:18.293",
            "3",
            "1",
            "1",
            "1",
            "Exclusive",
            "Female",
            "Theresa",
            "2023-05-09 12:05:00.0"));
    validateRow(
        response,
        1,
        List.of(
            "Samaia MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:18.418",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Evelyn",
            "2023-04-01 12:05:00.0"));
    validateRow(
        response,
        2,
        List.of(
            "Konjo (Dama) CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:18.464",
            "3",
            "1",
            "1",
            "0",
            "Mixed",
            "Female",
            "Wanda",
            "2023-08-26 12:05:00.0"));
    validateRow(
        response,
        3,
        List.of(
            "Rotifunk CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:18.614",
            "3",
            "1",
            "1",
            "1",
            "Mixed",
            "Female",
            "Jessica",
            "2023-10-02 12:05:00.0"));
    validateRow(
        response,
        4,
        List.of(
            "Praise Foundation CHC",
            "PVLOW4bCshG",
            "2018-08-07 15:47:18.717",
            "3",
            "1",
            "1",
            "0",
            "Mixed",
            "Female",
            "Amy",
            "2023-02-05 12:05:00.0"));
    validateRow(
        response,
        5,
        List.of(
            "Dodo Kortuma CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:18.897",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Alice",
            "2023-07-17 12:05:00.0"));
    validateRow(
        response,
        6,
        List.of(
            "Massahun MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:18.946",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Ruth",
            "2023-09-11 12:05:00.0"));
    validateRow(
        response,
        7,
        List.of(
            "Nyangbe-Bo MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:18.999",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Catherine",
            "2023-04-13 12:05:00.0"));
    validateRow(
        response,
        8,
        List.of(
            "Sandaru CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:19.077",
            "3",
            "1",
            "1",
            "0",
            "Exclusive",
            "Female",
            "Patricia",
            "2023-06-19 12:05:00.0"));
    validateRow(
        response,
        9,
        List.of(
            "Sawuria CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:19.16",
            "3",
            "1",
            "1",
            "0",
            "Mixed",
            "Female",
            "Betty",
            "2023-04-16 12:05:00.0"));
    validateRow(
        response,
        10,
        List.of(
            "Kissy Health Centre",
            "oRVt7g429ZO",
            "2018-08-07 15:47:19.173",
            "3",
            "1",
            "1",
            "0",
            "Exclusive",
            "Female",
            "Karen",
            "2023-02-14 12:05:00.0"));
    validateRow(
        response,
        11,
        List.of(
            "Mayossoh MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:19.557",
            "3",
            "1",
            "1",
            "0",
            "Exclusive",
            "Female",
            "Tammy",
            "2023-03-13 12:05:00.0"));
    validateRow(
        response,
        12,
        List.of(
            "Yakaji MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:19.588",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Cynthia",
            "2023-03-19 12:05:00.0"));
    validateRow(
        response,
        13,
        List.of(
            "Konia MCHP",
            "",
            "2018-08-07 15:47:19.601",
            "3",
            "1",
            "1",
            "0",
            "Mixed",
            "Female",
            "Diane",
            "2023-09-05 12:05:00.0"));
    validateRow(
        response,
        14,
        List.of(
            "Magbass MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:19.609",
            "3",
            "1",
            "1",
            "0",
            "Exclusive",
            "Female",
            "Michelle",
            "2023-05-15 12:05:00.0"));
    validateRow(
        response,
        15,
        List.of(
            "Rina Clinic",
            "",
            "2018-08-07 15:47:19.767",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Christina",
            "2023-04-26 12:05:00.0"));
    validateRow(
        response,
        16,
        List.of(
            "Kamba Mamudia MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:19.836",
            "3",
            "1",
            "1",
            "0",
            "Exclusive",
            "Female",
            "Brenda",
            "2023-11-15 12:05:00.0"));
    validateRow(
        response,
        17,
        List.of(
            "Rokel (Masimera) MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:19.847",
            "3",
            "1",
            "1",
            "0",
            "Mixed",
            "Female",
            "Donna",
            "2023-08-22 12:05:00.0"));
    validateRow(
        response,
        18,
        List.of(
            "Mayakie MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:19.869",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Sharon",
            "2023-08-18 12:05:00.0"));
    validateRow(
        response,
        19,
        List.of(
            "Makeni-Rokfullah MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:19.908",
            "3",
            "1",
            "1",
            "0",
            "Mixed",
            "Female",
            "Phyllis",
            "2023-01-16 12:05:00.0"));
    validateRow(
        response,
        20,
        List.of(
            "Follah MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:19.928",
            "3",
            "1",
            "1",
            "0",
            "Mixed",
            "Female",
            "Kathleen",
            "2023-04-11 12:05:00.0"));
    validateRow(
        response,
        21,
        List.of(
            "Sukudu Soa MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:19.941",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Evelyn",
            "2023-06-05 12:05:00.0"));
    validateRow(
        response,
        22,
        List.of(
            "Nyandeyaima MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:19.944",
            "3",
            "1",
            "1",
            "1",
            "Mixed",
            "Female",
            "Tammy",
            "2023-06-28 12:05:00.0"));
    validateRow(
        response,
        23,
        List.of(
            "Serabu (Bumpe Ngao) UFC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:19.956",
            "3",
            "1",
            "1",
            "1",
            "Mixed",
            "Female",
            "Jennifer",
            "2023-04-02 12:05:00.0"));
    validateRow(
        response,
        24,
        List.of(
            "Makobeh MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:20.022",
            "3",
            "1",
            "1",
            "0",
            "Exclusive",
            "Female",
            "Elizabeth",
            "2023-03-23 12:05:00.0"));
    validateRow(
        response,
        25,
        List.of(
            "Gbo-Lambayama 1 MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:20.031",
            "3",
            "1",
            "1",
            "0",
            "Mixed",
            "Female",
            "Paula",
            "2023-04-13 12:05:00.0"));
    validateRow(
        response,
        26,
        List.of(
            "Juma MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:20.037",
            "3",
            "1",
            "1",
            "0",
            "Mixed",
            "Female",
            "Lori",
            "2023-12-04 12:05:00.0"));
    validateRow(
        response,
        27,
        List.of(
            "Gbaa (Makpele) CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:20.09",
            "3",
            "1",
            "1",
            "0",
            "Exclusive",
            "Female",
            "Heather",
            "2023-09-25 12:05:00.0"));
    validateRow(
        response,
        28,
        List.of(
            "Rokolon MCHP",
            "",
            "2018-08-07 15:47:20.137",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Patricia",
            "2023-09-20 12:05:00.0"));
    validateRow(
        response,
        29,
        List.of(
            "John Thorpe MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:20.2",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Judith",
            "2023-01-28 12:05:00.0"));
    validateRow(
        response,
        30,
        List.of(
            "Koya MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:20.343",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Jacqueline",
            "2023-01-22 12:05:00.0"));
    validateRow(
        response,
        31,
        List.of(
            "Mabineh MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:20.371",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Judy",
            "2023-03-24 12:05:00.0"));
    validateRow(
        response,
        32,
        List.of(
            "Melekuray CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:20.42",
            "3",
            "1",
            "1",
            "1",
            "Replacement",
            "Female",
            "Jennifer",
            "2023-11-22 12:05:00.0"));
    validateRow(
        response,
        33,
        List.of(
            "MCH Static/U5",
            "oRVt7g429ZO",
            "2018-08-07 15:47:20.434",
            "3",
            "1",
            "1",
            "0",
            "Exclusive",
            "Female",
            "Carol",
            "2023-10-14 12:05:00.0"));
    validateRow(
        response,
        34,
        List.of(
            "Komende (Kaiyamba) MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:20.454",
            "3",
            "1",
            "1",
            "1",
            "Replacement",
            "Female",
            "Maria",
            "2023-06-26 12:05:00.0"));
    validateRow(
        response,
        35,
        List.of(
            "Maforay MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:20.623",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Stephanie",
            "2023-03-04 12:05:00.0"));
    validateRow(
        response,
        36,
        List.of(
            "Mawoma MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:20.774",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Annie",
            "2023-10-21 12:05:00.0"));
    validateRow(
        response,
        37,
        List.of(
            "Gboyama CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:20.858",
            "3",
            "1",
            "1",
            "0",
            "Mixed",
            "Female",
            "Irene",
            "2023-04-14 12:05:00.0"));
    validateRow(
        response,
        38,
        List.of(
            "Blessed Mokaba clinic",
            "oRVt7g429ZO",
            "2018-08-07 15:47:20.904",
            "3",
            "1",
            "1",
            "0",
            "Mixed",
            "Female",
            "Sarah",
            "2023-10-23 12:05:00.0"));
    validateRow(
        response,
        39,
        List.of(
            "Koidu Under Five Clinic",
            "oRVt7g429ZO",
            "2018-08-07 15:47:20.945",
            "3",
            "1",
            "1",
            "1",
            "Replacement",
            "Female",
            "Joyce",
            "2023-09-06 12:05:00.0"));
    validateRow(
        response,
        40,
        List.of(
            "Potoru CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:21.057",
            "3",
            "1",
            "1",
            "0",
            "Exclusive",
            "Female",
            "Nicole",
            "2023-04-14 12:05:00.0"));
    validateRow(
        response,
        41,
        List.of(
            "Minah MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:21.077",
            "3",
            "1",
            "1",
            "0",
            "Mixed",
            "Female",
            "Martha",
            "2023-07-30 12:05:00.0"));
    validateRow(
        response,
        42,
        List.of(
            "Saama (Lower Bamabara) CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:21.286",
            "3",
            "1",
            "1",
            "0",
            "Exclusive",
            "Female",
            "Norma",
            "2023-05-29 12:05:00.0"));
    validateRow(
        response,
        43,
        List.of(
            "Baptist Centre Kassirie",
            "",
            "2018-08-07 15:47:21.288",
            "3",
            "1",
            "1",
            "0",
            "Exclusive",
            "Female",
            "Joyce",
            "2023-11-14 12:05:00.0"));
    validateRow(
        response,
        44,
        List.of(
            "Jendema CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:21.363",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Cheryl",
            "2023-01-17 12:05:00.0"));
    validateRow(
        response,
        45,
        List.of(
            "Pejewa MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:21.399",
            "3",
            "1",
            "1",
            "0",
            "Exclusive",
            "Female",
            "Marie",
            "2023-03-06 12:05:00.0"));
    validateRow(
        response,
        46,
        List.of(
            "Kania MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:21.433",
            "3",
            "1",
            "1",
            "0",
            "Exclusive",
            "Female",
            "Diana",
            "2023-07-16 12:05:00.0"));
    validateRow(
        response,
        47,
        List.of(
            "Mamankie MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:21.443",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Katherine",
            "2023-05-01 12:05:00.0"));
    validateRow(
        response,
        48,
        List.of(
            "Banana Island MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:21.445",
            "3",
            "1",
            "1",
            "0",
            "Exclusive",
            "Female",
            "Catherine",
            "2023-05-18 12:05:00.0"));
    validateRow(
        response,
        49,
        List.of(
            "Makalie MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:21.449",
            "3",
            "1",
            "1",
            "0",
            "Mixed",
            "Female",
            "Julia",
            "2023-08-13 12:05:00.0"));
    validateRow(
        response,
        50,
        List.of(
            "Foindu MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:21.455",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Emily",
            "2023-08-03 12:05:00.0"));
    validateRow(
        response,
        51,
        List.of(
            "Hill Station MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:21.516",
            "3",
            "1",
            "1",
            "0",
            "Mixed",
            "Female",
            "Cynthia",
            "2023-09-19 12:05:00.0"));
    validateRow(
        response,
        52,
        List.of(
            "Bangoma MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:21.565",
            "3",
            "1",
            "1",
            "1",
            "Exclusive",
            "Female",
            "Judy",
            "2023-03-24 12:05:00.0"));
    validateRow(
        response,
        53,
        List.of(
            "Wai MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:21.605",
            "3",
            "1",
            "1",
            "0",
            "Exclusive",
            "Female",
            "Laura",
            "2023-09-18 12:05:00.0"));
    validateRow(
        response,
        54,
        List.of(
            "Mapailleh MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:21.617",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Diana",
            "2023-07-12 12:05:00.0"));
    validateRow(
        response,
        55,
        List.of(
            "Bradford CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:21.665",
            "3",
            "1",
            "1",
            "0",
            "Mixed",
            "Female",
            "Lisa",
            "2023-09-10 12:05:00.0"));
    validateRow(
        response,
        56,
        List.of(
            "Kantia CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:21.711",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Lori",
            "2023-09-29 12:05:00.0"));
    validateRow(
        response,
        57,
        List.of(
            "Kalangba MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:21.854",
            "3",
            "1",
            "1",
            "1",
            "Exclusive",
            "Female",
            "Maria",
            "2023-06-29 12:05:00.0"));
    validateRow(
        response,
        58,
        List.of(
            "Kambia CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:21.867",
            "3",
            "1",
            "1",
            "0",
            "Exclusive",
            "Female",
            "Jessica",
            "2023-07-18 12:05:00.0"));
    validateRow(
        response,
        59,
        List.of(
            "Sam Lean's MCHP",
            "MAs88nJc9nL",
            "2018-08-07 15:47:21.905",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Pamela",
            "2023-01-06 12:05:00.0"));
    validateRow(
        response,
        60,
        List.of(
            "Yoyema MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:21.944",
            "3",
            "1",
            "1",
            "0",
            "Exclusive",
            "Female",
            "Susan",
            "2023-01-09 12:05:00.0"));
    validateRow(
        response,
        61,
        List.of(
            "UFC Port Loko",
            "oRVt7g429ZO",
            "2018-08-07 15:47:21.95",
            "3",
            "1",
            "1",
            "0",
            "Exclusive",
            "Female",
            "Tammy",
            "2023-10-07 12:05:00.0"));
    validateRow(
        response,
        62,
        List.of(
            "Mano Sewallu CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:21.982",
            "3",
            "1",
            "1",
            "0",
            "Exclusive",
            "Female",
            "Karen",
            "2023-03-25 12:05:00.0"));
    validateRow(
        response,
        63,
        List.of(
            "Pelewahun (Baoma) MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:21.995",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Stephanie",
            "2023-01-01 12:05:00.0"));
    validateRow(
        response,
        64,
        List.of(
            "Ngiewahun CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.03",
            "3",
            "1",
            "1",
            "0",
            "Exclusive",
            "Female",
            "Angela",
            "2023-11-28 12:05:00.0"));
    validateRow(
        response,
        65,
        List.of(
            "Mangay Loko MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.124",
            "3",
            "1",
            "1",
            "0",
            "Exclusive",
            "Female",
            "Louise",
            "2023-03-15 12:05:00.0"));
    validateRow(
        response,
        66,
        List.of(
            "Taninahun (Malen) CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.133",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Amy",
            "2023-03-22 12:05:00.0"));
    validateRow(
        response,
        67,
        List.of(
            "Bandajuma Sinneh MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.159",
            "3",
            "1",
            "1",
            "0",
            "Mixed",
            "Female",
            "Brenda",
            "2023-03-19 12:05:00.0"));
    validateRow(
        response,
        68,
        List.of(
            "Mabonkanie MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.169",
            "3",
            "1",
            "1",
            "0",
            "Exclusive",
            "Female",
            "Katherine",
            "2023-07-09 12:05:00.0"));
    validateRow(
        response,
        69,
        List.of(
            "Baiama CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.196",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Judy",
            "2023-04-28 12:05:00.0"));
    validateRow(
        response,
        70,
        List.of(
            "Kumrabai Yoni MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.27",
            "3",
            "1",
            "1",
            "0",
            "Exclusive",
            "Female",
            "Nicole",
            "2023-07-12 12:05:00.0"));
    validateRow(
        response,
        71,
        List.of(
            "Gbonkoh Kareneh MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.283",
            "3",
            "1",
            "1",
            "0",
            "Mixed",
            "Female",
            "Anne",
            "2023-05-28 12:05:00.0"));
    validateRow(
        response,
        72,
        List.of(
            "Feuror MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.349",
            "3",
            "1",
            "1",
            "1",
            "Mixed",
            "Female",
            "Lillian",
            "2023-11-05 12:05:00.0"));
    validateRow(
        response,
        73,
        List.of(
            "Kpayama 1 MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.397",
            "3",
            "1",
            "1",
            "0",
            "Exclusive",
            "Female",
            "Debra",
            "2023-07-29 12:05:00.0"));
    validateRow(
        response,
        74,
        List.of(
            "Arab Clinic",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.484",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Lori",
            "2023-09-16 12:05:00.0"));
    validateRow(
        response,
        75,
        List.of(
            "Hamilton MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.489",
            "3",
            "1",
            "1",
            "0",
            "Exclusive",
            "Female",
            "Paula",
            "2023-02-07 12:05:00.0"));
    validateRow(
        response,
        76,
        List.of(
            "Njama MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.497",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Jane",
            "2023-09-09 12:05:00.0"));
    validateRow(
        response,
        77,
        List.of(
            "Yengema CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.509",
            "3",
            "1",
            "1",
            "0",
            "Mixed",
            "Female",
            "Rose",
            "2023-08-28 12:05:00.0"));
    validateRow(
        response,
        78,
        List.of(
            "Rapha Clinic",
            "",
            "2018-08-07 15:47:22.511",
            "3",
            "1",
            "1",
            "1",
            "Mixed",
            "Female",
            "Janet",
            "2023-11-17 12:05:00.0"));
    validateRow(
        response,
        79,
        List.of(
            "Approved School CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.539",
            "3",
            "1",
            "1",
            "0",
            "Mixed",
            "Female",
            "Amanda",
            "2023-05-18 12:05:00.0"));
    validateRow(
        response,
        80,
        List.of(
            "Bandajuma Kpolihun CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.575",
            "3",
            "1",
            "1",
            "1",
            "Exclusive",
            "Female",
            "Teresa",
            "2023-01-11 12:05:00.0"));
    validateRow(
        response,
        81,
        List.of(
            "Doujou CHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.592",
            "3",
            "1",
            "1",
            "0",
            "Exclusive",
            "Female",
            "Kimberly",
            "2023-08-15 12:05:00.0"));
    validateRow(
        response,
        82,
        List.of(
            "Philip Street Clinic",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.6",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Carolyn",
            "2023-11-30 12:05:00.0"));
    validateRow(
        response,
        83,
        List.of(
            "Yoyema MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.613",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Ruth",
            "2023-02-24 12:05:00.0"));
    validateRow(
        response,
        84,
        List.of(
            "Rofutha MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.658",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Julia",
            "2023-05-20 12:05:00.0"));
    validateRow(
        response,
        85,
        List.of(
            "Koindu-kuntey MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.684",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Emily",
            "2023-01-06 12:05:00.0"));
    validateRow(
        response,
        86,
        List.of(
            "Mathinkalol MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.824",
            "3",
            "1",
            "1",
            "0",
            "Mixed",
            "Female",
            "Kathy",
            "2023-09-05 12:05:00.0"));
    validateRow(
        response,
        87,
        List.of(
            "Gbaama MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.875",
            "3",
            "1",
            "1",
            "1",
            "Exclusive",
            "Female",
            "Kimberly",
            "2023-05-19 12:05:00.0"));
    validateRow(
        response,
        88,
        List.of(
            "Fothaneh Bana MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.883",
            "3",
            "1",
            "1",
            "0",
            "Exclusive",
            "Female",
            "Ann",
            "2023-06-20 12:05:00.0"));
    validateRow(
        response,
        89,
        List.of(
            "Kawaya MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.915",
            "3",
            "1",
            "1",
            "0",
            "Mixed",
            "Female",
            "Anna",
            "2023-05-06 12:05:00.0"));
    validateRow(
        response,
        90,
        List.of(
            "St Anthony clinic",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.916",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Janice",
            "2023-03-28 12:05:00.0"));
    validateRow(
        response,
        91,
        List.of(
            "Masofinia MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.956",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Rachel",
            "2023-09-12 12:05:00.0"));
    validateRow(
        response,
        92,
        List.of(
            "Holy Mary Hospital",
            "MAs88nJc9nL",
            "2018-08-07 15:47:22.963",
            "3",
            "1",
            "1",
            "1",
            "Replacement",
            "Female",
            "Kimberly",
            "2023-01-21 12:05:00.0"));
    validateRow(
        response,
        93,
        List.of(
            "Gbalan Thallan MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.967",
            "3",
            "1",
            "1",
            "0",
            "Mixed",
            "Female",
            "Linda",
            "2023-01-16 12:05:00.0"));
    validateRow(
        response,
        94,
        List.of(
            "Mateboi CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:22.976",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Joan",
            "2023-11-04 12:05:00.0"));
    validateRow(
        response,
        95,
        List.of(
            "Mokassie MCHP",
            "oRVt7g429ZO",
            "2018-08-07 15:47:23.113",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Deborah",
            "2023-02-14 12:05:00.0"));
    validateRow(
        response,
        96,
        List.of(
            "Leprosy & TB Hospital",
            "oRVt7g429ZO",
            "2018-08-07 15:47:23.124",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Sharon",
            "2023-05-31 12:05:00.0"));
    validateRow(
        response,
        97,
        List.of(
            "UNIMUS MCHP",
            "MAs88nJc9nL",
            "2018-08-07 15:47:23.184",
            "3",
            "1",
            "1",
            "0",
            "Replacement",
            "Female",
            "Karen",
            "2023-06-20 12:05:00.0"));
    validateRow(
        response,
        98,
        List.of(
            "Moriba Town CHC",
            "oRVt7g429ZO",
            "2018-08-07 15:47:23.194",
            "3",
            "1",
            "1",
            "0",
            "Exclusive",
            "Female",
            "Karen",
            "2023-10-06 12:05:00.0"));
    validateRow(
        response,
        99,
        List.of(
            "Rokolon MCHP",
            "",
            "2018-08-07 15:47:23.215",
            "3",
            "1",
            "1",
            "0",
            "Exclusive",
            "Female",
            "Nancy",
            "2023-10-02 12:05:00.0"));
  }
}
