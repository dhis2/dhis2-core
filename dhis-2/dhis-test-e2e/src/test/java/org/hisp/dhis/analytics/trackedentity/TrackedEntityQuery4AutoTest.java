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
package org.hisp.dhis.analytics.trackedentity;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsTrackedEntityActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/trackedEntities/query" endpoint. */
public class TrackedEntityQuery4AutoTest extends AnalyticsApiTest {
  private AnalyticsTrackedEntityActions actions = new AnalyticsTrackedEntityActions();

  @Test
  public void sortByGlobalOuAsc() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=oucode")
            .add("headers=ouname")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("pageSize=100")
            .add("page=1")
            .add(
                "dimension=gHGyrwKPzej,ciq2USN94oJ,IpHINAT79UW.ZzYYXq4fJie.GQY2lXrypjO,ou:USER_ORGUNIT")
            .add("programStatus=IpHINAT79UW.ACTIVE")
            .add("relativePeriodDate=2017-01-27");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(1)))
        .body("rows", hasSize(equalTo(100)))
        .body("height", equalTo(100))
        .body("width", equalTo(1))
        .body("headerWidth", equalTo(1));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":false},\"items\":{\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"code\":\"MMD_PER_DOB\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"code\":\"MMD_PER_STA\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"GQY2lXrypjO\":{\"uid\":\"GQY2lXrypjO\",\"code\":\"DE_2006099\",\"name\":\"MCH Infant Weight  (g)\",\"description\":\"Infant weight in grams\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"IpHINAT79UW.ZzYYXq4fJie.GQY2lXrypjO\":{\"uid\":\"GQY2lXrypjO\",\"code\":\"DE_2006099\",\"name\":\"MCH Infant Weight  (g)\",\"description\":\"Infant weight in grams\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"GQY2lXrypjO\":[],\"Qo571yj6Zcn\":[],\"DODgdr5Oo2v\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"xs8A6tQJY0s\":[],\"A4xFHyieXys\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"GUOBQt5K2WI\":[],\"Agywv2JGwuq\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[\"wfkKVdPBzho\",\"Yjte6foKMny\"],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);

    // Assert rows.
    validateRow(response, 0, List.of("Praise Foundation CHC"));
    validateRow(response, 10, List.of("Praise Foundation CHC"));
    validateRow(response, 11, List.of("Bucksal Clinic"));
    validateRow(response, 26, List.of("Bucksal Clinic"));
    validateRow(response, 27, List.of("Harvest Time MCHP"));
    validateRow(response, 35, List.of("Harvest Time MCHP"));
    validateRow(response, 36, List.of("Yemoh Town CHC"));
    validateRow(response, 48, List.of("Yemoh Town CHC"));
    validateRow(response, 49, List.of("Bandajuma MCHP"));
  }

  @Test
  public void sortByEventDateDesc() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("headers=gHGyrwKPzej,ciq2USN94oJ,IpHINAT79UW.A03MvHHogjR.occurreddate")
            .add("IpHINAT79UW.A03MvHHogjR.occurreddate=LAST_YEAR")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("pageSize=50")
            .add("page=1")
            .add(
                "dimension=ou:USER_ORGUNIT,gHGyrwKPzej,ciq2USN94oJ,cejWyOfXge6,IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6")
            .add("desc=IpHINAT79UW.A03MvHHogjR.occurreddate")
            .add("relativePeriodDate=2018-01-28");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

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
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":false},\"items\":{\"IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"code\":\"MMD_PER_STA\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"cejWyOfXge6\":{\"uid\":\"cejWyOfXge6\",\"name\":\"Gender\",\"description\":\"Gender\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"code\":\"MMD_PER_DOB\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW.A03MvHHogjR.occurreddate\":{\"name\":\"Report date\",\"dimensionType\":\"PERIOD\"},\"IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"bx6fsa0t90x\":[],\"Qo571yj6Zcn\":[],\"DODgdr5Oo2v\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"xs8A6tQJY0s\":[],\"A4xFHyieXys\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"GUOBQt5K2WI\":[],\"Agywv2JGwuq\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"a3kGcGDCuk6\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "gHGyrwKPzej", "Birth date", "DATE", "java.time.LocalDate", false, true);
    validateHeader(
        response, 1, "ciq2USN94oJ", "Civil status", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        2,
        "IpHINAT79UW.A03MvHHogjR.occurreddate",
        "Report date, Child Programme, Birth",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // Assert rows.
    validateRow(response, 0, List.of("", "", "2022-12-29 00:00:00.0"));
    validateRow(response, 1, List.of("", "", "2022-12-29 00:00:00.0"));
    validateRow(response, 2, List.of("", "", "2022-12-29 00:00:00.0"));
    validateRow(response, 3, List.of("", "", "2022-12-29 00:00:00.0"));
    validateRow(response, 4, List.of("", "", "2022-12-29 00:00:00.0"));
    validateRow(response, 5, List.of("", "", "2022-12-29 00:00:00.0"));
    validateRow(response, 6, List.of("", "", "2022-12-29 00:00:00.0"));
    validateRow(response, 7, List.of("", "", "2022-12-29 00:00:00.0"));
    validateRow(response, 8, List.of("", "", "2022-12-29 00:00:00.0"));
    validateRow(response, 9, List.of("", "", "2022-12-29 00:00:00.0"));
    validateRow(response, 10, List.of("", "", "2022-12-29 00:00:00.0"));
    validateRow(response, 11, List.of("", "", "2022-12-29 00:00:00.0"));
    validateRow(response, 12, List.of("", "", "2022-12-29 00:00:00.0"));
    validateRow(response, 13, List.of("", "", "2022-12-29 00:00:00.0"));
    validateRow(response, 14, List.of("", "", "2022-12-29 00:00:00.0"));
    validateRow(response, 15, List.of("", "", "2022-12-29 00:00:00.0"));
    validateRow(response, 16, List.of("", "", "2022-12-29 00:00:00.0"));
    validateRow(response, 17, List.of("", "", "2022-12-29 00:00:00.0"));
    validateRow(response, 18, List.of("", "", "2022-12-29 00:00:00.0"));
    validateRow(response, 19, List.of("", "", "2022-12-29 00:00:00.0"));
    validateRow(response, 20, List.of("", "", "2022-12-29 00:00:00.0"));
    validateRow(response, 21, List.of("", "", "2022-12-29 00:00:00.0"));
    validateRow(response, 22, List.of("", "", "2022-12-29 00:00:00.0"));
    validateRow(response, 23, List.of("", "", "2022-12-29 00:00:00.0"));
    validateRow(response, 24, List.of("", "", "2022-12-28 00:00:00.0"));
    validateRow(response, 25, List.of("", "", "2022-12-28 00:00:00.0"));
    validateRow(response, 26, List.of("", "", "2022-12-28 00:00:00.0"));
    validateRow(response, 27, List.of("", "", "2022-12-28 00:00:00.0"));
    validateRow(response, 28, List.of("", "", "2022-12-28 00:00:00.0"));
    validateRow(response, 29, List.of("", "", "2022-12-28 00:00:00.0"));
    validateRow(response, 30, List.of("", "", "2022-12-28 00:00:00.0"));
    validateRow(response, 31, List.of("", "", "2022-12-28 00:00:00.0"));
    validateRow(response, 32, List.of("", "", "2022-12-28 00:00:00.0"));
    validateRow(response, 33, List.of("", "", "2022-12-28 00:00:00.0"));
    validateRow(response, 34, List.of("", "", "2022-12-28 00:00:00.0"));
    validateRow(response, 35, List.of("", "", "2022-12-28 00:00:00.0"));
    validateRow(response, 36, List.of("", "", "2022-12-28 00:00:00.0"));
    validateRow(response, 37, List.of("", "", "2022-12-28 00:00:00.0"));
    validateRow(response, 38, List.of("", "", "2022-12-28 00:00:00.0"));
    validateRow(response, 39, List.of("", "", "2022-12-28 00:00:00.0"));
    validateRow(response, 40, List.of("", "", "2022-12-28 00:00:00.0"));
    validateRow(response, 41, List.of("", "", "2022-12-28 00:00:00.0"));
    validateRow(response, 42, List.of("", "", "2022-12-28 00:00:00.0"));
    validateRow(response, 43, List.of("", "", "2022-12-28 00:00:00.0"));
    validateRow(response, 44, List.of("", "", "2022-12-28 00:00:00.0"));
    validateRow(response, 45, List.of("", "", "2022-12-28 00:00:00.0"));
    validateRow(response, 46, List.of("", "", "2022-12-27 00:00:00.0"));
    validateRow(response, 47, List.of("", "", "2022-12-27 00:00:00.0"));
    validateRow(response, 48, List.of("", "", "2022-12-27 00:00:00.0"));
    validateRow(response, 49, List.of("", "", "2022-12-27 00:00:00.0"));
  }

  @Test
  public void sortByEventDateWithOffsetsDesc() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("IpHINAT79UW[-1].A03MvHHogjR[-1].occurreddate=LAST_YEAR")
            .add("pageSize=10")
            .add("page=1")
            .add(
                "dimension=ou:USER_ORGUNIT,gHGyrwKPzej,ciq2USN94oJ,cejWyOfXge6,IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6")
            .add("desc=lastupdated")
            .add("relativePeriodDate=2018-01-28");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(45)))
        .body("rows", hasSize(equalTo(10)))
        .body("height", equalTo(10))
        .body("width", equalTo(45))
        .body("headerWidth", equalTo(45));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":10,\"isLastPage\":false},\"items\":{\"Mnp3oXrpAbK\":{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\",\"name\":\"Female\"},\"IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"xjA5E9MimMU\":{\"uid\":\"xjA5E9MimMU\",\"name\":\"Civil status\",\"options\":[{\"uid\":\"wfkKVdPBzho\",\"code\":\"Single or widow\"},{\"uid\":\"Yjte6foKMny\",\"code\":\"Married (conjugal cohabitation)\"}]},\"bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"code\":\"MMD_PER_STA\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"Yjte6foKMny\":{\"uid\":\"Yjte6foKMny\",\"code\":\"Married (conjugal cohabitation)\",\"name\":\"Married (conjugal cohabitation)\"},\"a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"cejWyOfXge6\":{\"uid\":\"cejWyOfXge6\",\"name\":\"Gender\",\"description\":\"Gender\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"code\":\"MMD_PER_DOB\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"wfkKVdPBzho\":{\"uid\":\"wfkKVdPBzho\",\"code\":\"Single or widow\",\"name\":\"Single or widow\"},\"pC3N9N77UmT\":{\"uid\":\"pC3N9N77UmT\",\"name\":\"Gender\",\"options\":[{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\"}]}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"bx6fsa0t90x\":[],\"Qo571yj6Zcn\":[],\"DODgdr5Oo2v\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"xs8A6tQJY0s\":[],\"A4xFHyieXys\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"GUOBQt5K2WI\":[],\"Agywv2JGwuq\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[\"wfkKVdPBzho\",\"Yjte6foKMny\"],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"a3kGcGDCuk6\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    int headerIndex = 0;

    validateHeader(
        response,
        (headerIndex++),
        "trackedentity",
        "Tracked entity",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "lastupdated",
        "Last updated",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "lastupdatedbydisplayname",
        "Last updated by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "created",
        "Created",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "createdbydisplayname",
        "Created by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "storedby",
        "Stored by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response, (headerIndex++), "geometry", "Geometry", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        (headerIndex++),
        "longitude",
        "Longitude",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "latitude",
        "Latitude",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "oucode",
        "Organisation unit code",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "ounamehierarchy",
        "Organisation unit hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "A4xFHyieXys",
        "Occupation",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "Agywv2JGwuq",
        "Mobile number",
        "PHONE_NUMBER",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "AuPLng5hLbE",
        "National identifier",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "cejWyOfXge6",
        "Gender",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "ciq2USN94oJ",
        "Civil status",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "DODgdr5Oo2v",
        "Provider ID",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response, (headerIndex++), "FO4sWYJ64LQ", "City", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        (headerIndex++),
        "G7vUx908SwP",
        "Residence location",
        "COORDINATE",
        "org.opengis.geometry.primitive.Point",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "gHGyrwKPzej",
        "Birth date",
        "DATE",
        "java.time.LocalDate",
        false,
        true);
    validateHeader(
        response, (headerIndex++), "GUOBQt5K2WI", "State", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        (headerIndex++),
        "H9IlTX2X6SL",
        "Blood type",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "iESIqZ0R0R0",
        "Date of birth",
        "DATE",
        "java.time.LocalDate",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "KmEUg2hHEtx",
        "Email address",
        "EMAIL",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "kyIzQsj96BD",
        "Company",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "lw1SqmMlnfh",
        "Height in cm",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "lZGmxYbs97q",
        "Unique ID",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "n9nUvfpTsxQ",
        "Zip code",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response, (headerIndex++), "NDXw0cluzSw", "Email", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        (headerIndex++),
        "o9odfev2Ty5",
        "Mother maiden name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "OvY4VVhSDeJ",
        "Weight in kg",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "P2cwLGskgxn",
        "Phone number",
        "PHONE_NUMBER",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "Qo571yj6Zcn",
        "Latitude",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "RG7uGl4w5Jq",
        "Longitude",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "ruQQnf6rswq",
        "TB number",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response, (headerIndex++), "spFvx9FndA4", "Age", "AGE", "java.util.Date", false, true);
    validateHeader(
        response,
        (headerIndex++),
        "VHfUeXpawmE",
        "Vehicle",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "VqEFza8wbwA",
        "Address",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "w75KJ2mc4zz",
        "First name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "xs8A6tQJY0s",
        "TB identifier",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "ZcBPrXKahq2",
        "Postal code",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "zDhUuAYrxNC",
        "Last name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x",
        "MCH BCG dose, Child Programme, Birth",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        (headerIndex++),
        "IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6",
        "MCH Apgar Score, Child Programme, Birth",
        "NUMBER",
        "java.lang.Double",
        false,
        true);

    // Assert rows.
    validateRow(
        response,
        1,
        List.of(
            "vOxUH373fy5",
            "2017-05-26 11:46:22.372",
            ",  ()",
            "2017-01-20 10:44:02.77",
            ",  ()",
            "",
            "",
            "",
            "",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "",
            "",
            "",
            "Female",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "Filona",
            "",
            "",
            "Ryder",
            "",
            ""));
    validateRow(
        response,
        2,
        List.of(
            "vu9dsAuJ29q",
            "2017-05-22 22:34:09.669",
            ",  ()",
            "2015-10-14 14:14:21.384",
            ",  ()",
            "",
            "",
            "",
            "",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "",
            "",
            "",
            "",
            "Married (conjugal cohabitation)",
            "",
            "",
            "",
            "1985-10-01 00:00:00.0",
            "",
            "",
            "",
            "",
            "",
            "",
            "9191132445122",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "Ava",
            "",
            "",
            "Didriksson",
            "",
            ""));
    validateRow(
        response,
        3,
        List.of(
            "JYWyAYTMdRv",
            "2017-01-26 13:48:13.371",
            ",  ()",
            "2017-01-26 13:48:13.37",
            ",  ()",
            "",
            "",
            "",
            "",
            "Blamawo MCHP",
            "OU_73727",
            "Sierra Leone / Bo / Baoma / Blamawo MCHP",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "1986-07-04 00:00:00.0",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "Rose",
            "",
            "",
            "Hudson",
            "",
            ""));
    validateRow(
        response,
        4,
        List.of(
            "LtbQLsx5zol",
            "2017-01-26 13:48:13.37",
            ",  ()",
            "2017-01-26 13:48:13.369",
            ",  ()",
            "",
            "",
            "",
            "",
            "Petifu Mayepoh MCHP",
            "OU_268211",
            "Sierra Leone / Tonkolili / Gbonkonlenken / Petifu Mayepoh MCHP",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "1972-05-03 00:00:00.0",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "Beverly",
            "",
            "",
            "Boyd",
            "",
            ""));

    validateRow(
        response,
        5,
        List.of(
            "GRxWzijJ5jt",
            "2017-01-26 13:48:13.368",
            ",  ()",
            "2017-01-26 13:48:13.367",
            ",  ()",
            "",
            "",
            "",
            "",
            "Kondewakoro CHP",
            "OU_233315",
            "Sierra Leone / Kono / Toli / Kondewakoro CHP",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "1982-03-04 00:00:00.0",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "Sandra",
            "",
            "",
            "Ferguson",
            "",
            ""));
    validateRow(
        response,
        6,
        List.of(
            "D8G28uT0rmc",
            "2017-01-26 13:48:13.366",
            ",  ()",
            "2017-01-26 13:48:13.365",
            ",  ()",
            "",
            "",
            "",
            "",
            "Mathen MCHP",
            "OU_254990",
            "Sierra Leone / Port Loko / Lokomasama / Mathen MCHP",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "1987-01-28 00:00:00.0",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "Heather",
            "",
            "",
            "Hughes",
            "",
            ""));

    validateRow(
        response,
        7,
        List.of(
            "IOR1AXXl24H",
            "2017-01-26 13:48:13.363",
            ",  ()",
            "2017-01-26 13:48:13.363",
            ",  ()",
            "",
            "",
            "",
            "",
            "Mbokie CHP",
            "OU_197401",
            "Sierra Leone / Bonthe / Sittia / Mbokie CHP",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "1981-03-21 00:00:00.0",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "Melissa",
            "",
            "",
            "Fuller",
            "",
            ""));

    validateRow(
        response,
        8,
        List.of(
            "Y8aEiTpHSSQ",
            "2017-01-26 13:48:13.36",
            ",  ()",
            "2017-01-26 13:48:13.36",
            ",  ()",
            "",
            "",
            "",
            "",
            "Kolisokor MCHP",
            "OU_193259",
            "Sierra Leone / Bombali / Makari Gbanti / Kolisokor MCHP",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "1988-01-18 00:00:00.0",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "Irene",
            "",
            "",
            "Jacobs",
            "",
            ""));
    validateRow(
        response,
        9,
        List.of(
            "wuDUAklkAHS",
            "2017-01-26 13:48:13.359",
            ",  ()",
            "2017-01-26 13:48:13.359",
            ",  ()",
            "",
            "",
            "",
            "",
            "Grey Bush CHC",
            "OU_651068",
            "Sierra Leone / Western Area / Freetown / Grey Bush CHC",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "1985-03-17 00:00:00.0",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "Lillian",
            "",
            "",
            "Butler",
            "",
            ""));
  }

  @Test
  public void sortByEnrollmentDateAscMultiProgram() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=ur1Edk5Oe2n.enrollmentdate")
            .add(
                "headers=ouname,cejWyOfXge6,w75KJ2mc4zz,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6,IpHINAT79UW.ouname,ur1Edk5Oe2n.EPEcjy3FWmI.fTZFU8cWvb3,ur1Edk5Oe2n.enrollmentdate")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("enrollmentDate=ur1Edk5Oe2n.LAST_12_MONTHS,ur1Edk5Oe2n.LAST_5_YEARS")
            .add("rowContext=true")
            .add("pageSize=30")
            .add("page=1")
            .add(
                "dimension=ou:USER_ORGUNIT,cejWyOfXge6,w75KJ2mc4zz,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6,ur1Edk5Oe2n.EPEcjy3FWmI.fTZFU8cWvb3")
            .add("relativePeriodDate=2028-01-27");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(7)))
        .body("rows", hasSize(equalTo(2)))
        .body("height", equalTo(2))
        .body("width", equalTo(7))
        .body("headerWidth", equalTo(7));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":30,\"isLastPage\":true},\"items\":{\"fTZFU8cWvb3\":{\"uid\":\"fTZFU8cWvb3\",\"code\":\"DE_860002\",\"name\":\"TB lab Hemoglobin\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"lw1SqmMlnfh\":{\"uid\":\"lw1SqmMlnfh\",\"code\":\"Height in cm\",\"name\":\"Height in cm\",\"description\":\"Height in cm\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"DODgdr5Oo2v\":{\"uid\":\"DODgdr5Oo2v\",\"code\":\"Provider ID\",\"name\":\"Provider ID\",\"description\":\"Provider ID\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"iESIqZ0R0R0\":{\"uid\":\"iESIqZ0R0R0\",\"name\":\"Date of birth\",\"description\":\"Date of birth\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"n9nUvfpTsxQ\":{\"uid\":\"n9nUvfpTsxQ\",\"code\":\"Zip code\",\"name\":\"Zip code\",\"description\":\"Zip code\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"2027\":{\"uid\":\"2027\",\"code\":\"2027\",\"name\":\"2027\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2027-01-01T00:00:00.000\",\"endDate\":\"2027-12-31T00:00:00.000\"},\"2026\":{\"uid\":\"2026\",\"code\":\"2026\",\"name\":\"2026\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2026-01-01T00:00:00.000\",\"endDate\":\"2026-12-31T00:00:00.000\"},\"2025\":{\"uid\":\"2025\",\"code\":\"2025\",\"name\":\"2025\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2025-01-01T00:00:00.000\",\"endDate\":\"2025-12-31T00:00:00.000\"},\"2024\":{\"uid\":\"2024\",\"code\":\"2024\",\"name\":\"2024\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2024-01-01T00:00:00.000\",\"endDate\":\"2024-12-31T00:00:00.000\"},\"RG7uGl4w5Jq\":{\"uid\":\"RG7uGl4w5Jq\",\"code\":\"Longitude\",\"name\":\"Longitude\",\"description\":\"Longitude\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"GUOBQt5K2WI\":{\"uid\":\"GUOBQt5K2WI\",\"code\":\"State\",\"name\":\"State\",\"description\":\"State\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"Mnp3oXrpAbK\":{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\",\"name\":\"Female\"},\"VqEFza8wbwA\":{\"uid\":\"VqEFza8wbwA\",\"code\":\"MMD_PER_ADR1\",\"name\":\"Address\",\"description\":\"Country\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"w75KJ2mc4zz\":{\"uid\":\"w75KJ2mc4zz\",\"code\":\"MMD_PER_NAM\",\"name\":\"First name\",\"description\":\"First name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"KmEUg2hHEtx\":{\"uid\":\"KmEUg2hHEtx\",\"name\":\"Email address\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"G7vUx908SwP\":{\"uid\":\"G7vUx908SwP\",\"name\":\"Residence location\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"COORDINATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"o9odfev2Ty5\":{\"uid\":\"o9odfev2Ty5\",\"code\":\"Mother maiden name\",\"name\":\"Mother maiden name\",\"description\":\"Mother maiden name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"FO4sWYJ64LQ\":{\"uid\":\"FO4sWYJ64LQ\",\"code\":\"City\",\"name\":\"City\",\"description\":\"City\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"NDXw0cluzSw\":{\"uid\":\"NDXw0cluzSw\",\"name\":\"Email\",\"description\":\"Email address\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ruQQnf6rswq\":{\"uid\":\"ruQQnf6rswq\",\"name\":\"TB number\",\"description\":\"TB number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"cejWyOfXge6\":{\"uid\":\"cejWyOfXge6\",\"name\":\"Gender\",\"description\":\"Gender\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ur1Edk5Oe2n.enrollmentdate\":{\"name\":\"Start of treatment date\",\"dimensionType\":\"PERIOD\"},\"VHfUeXpawmE\":{\"uid\":\"VHfUeXpawmE\",\"code\":\"Vehicle\",\"name\":\"Vehicle\",\"description\":\"Vehicle\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ZcBPrXKahq2\":{\"uid\":\"ZcBPrXKahq2\",\"name\":\"Postal code\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"H9IlTX2X6SL\":{\"uid\":\"H9IlTX2X6SL\",\"code\":\"Blood type\",\"name\":\"Blood type\",\"description\":\"Blood type\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"zDhUuAYrxNC\":{\"uid\":\"zDhUuAYrxNC\",\"name\":\"Last name\",\"description\":\"Last name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"202709\":{\"uid\":\"202709\",\"code\":\"202709\",\"name\":\"September 2027\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2027-09-01T00:00:00.000\",\"endDate\":\"2027-09-30T00:00:00.000\"},\"IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"202707\":{\"uid\":\"202707\",\"code\":\"202707\",\"name\":\"July 2027\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2027-07-01T00:00:00.000\",\"endDate\":\"2027-07-31T00:00:00.000\"},\"202708\":{\"uid\":\"202708\",\"code\":\"202708\",\"name\":\"August 2027\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2027-08-01T00:00:00.000\",\"endDate\":\"2027-08-31T00:00:00.000\"},\"202705\":{\"uid\":\"202705\",\"code\":\"202705\",\"name\":\"May 2027\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2027-05-01T00:00:00.000\",\"endDate\":\"2027-05-31T00:00:00.000\"},\"202706\":{\"uid\":\"202706\",\"code\":\"202706\",\"name\":\"June 2027\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2027-06-01T00:00:00.000\",\"endDate\":\"2027-06-30T00:00:00.000\"},\"202703\":{\"uid\":\"202703\",\"code\":\"202703\",\"name\":\"March 2027\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2027-03-01T00:00:00.000\",\"endDate\":\"2027-03-31T00:00:00.000\"},\"202704\":{\"uid\":\"202704\",\"code\":\"202704\",\"name\":\"April 2027\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2027-04-01T00:00:00.000\",\"endDate\":\"2027-04-30T00:00:00.000\"},\"202701\":{\"uid\":\"202701\",\"code\":\"202701\",\"name\":\"January 2027\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2027-01-01T00:00:00.000\",\"endDate\":\"2027-01-31T00:00:00.000\"},\"fDd25txQckK\":{\"uid\":\"fDd25txQckK\",\"name\":\"Provider Follow-up and Support Tool\"},\"Qo571yj6Zcn\":{\"uid\":\"Qo571yj6Zcn\",\"code\":\"Latitude\",\"name\":\"Latitude\",\"description\":\"Latitude\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"202702\":{\"uid\":\"202702\",\"code\":\"202702\",\"name\":\"February 2027\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2027-02-01T00:00:00.000\",\"endDate\":\"2027-02-28T00:00:00.000\"},\"kyIzQsj96BD\":{\"uid\":\"kyIzQsj96BD\",\"code\":\"Company\",\"name\":\"Company\",\"description\":\"Company\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"IpHINAT79UW.ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A4xFHyieXys\":{\"uid\":\"A4xFHyieXys\",\"code\":\"Occupation\",\"name\":\"Occupation\",\"description\":\"Occupation\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"xs8A6tQJY0s\":{\"uid\":\"xs8A6tQJY0s\",\"name\":\"TB identifier\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"OvY4VVhSDeJ\":{\"uid\":\"OvY4VVhSDeJ\",\"name\":\"Weight in kg\",\"description\":\"Weight in kg\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"EPEcjy3FWmI\":{\"uid\":\"EPEcjy3FWmI\",\"name\":\"Lab monitoring\",\"description\":\"Laboratory monitoring\"},\"ur1Edk5Oe2n\":{\"uid\":\"ur1Edk5Oe2n\",\"name\":\"TB program\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"pC3N9N77UmT\":{\"uid\":\"pC3N9N77UmT\",\"name\":\"Gender\",\"options\":[{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\"}]},\"spFvx9FndA4\":{\"uid\":\"spFvx9FndA4\",\"name\":\"Age\",\"description\":\"Age\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"Agywv2JGwuq\":{\"uid\":\"Agywv2JGwuq\",\"code\":\"MMD_PER_MOB\",\"name\":\"Mobile number\",\"description\":\"Mobile number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"lZGmxYbs97q\":{\"uid\":\"lZGmxYbs97q\",\"code\":\"MMD_PER_ID\",\"name\":\"Unique ID\",\"description\":\"Unique identiifer\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"code\":\"MMD_PER_STA\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"2023\":{\"uid\":\"2023\",\"code\":\"2023\",\"name\":\"2023\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2023-01-01T00:00:00.000\",\"endDate\":\"2023-12-31T00:00:00.000\"},\"ur1Edk5Oe2n.pe\":{\"uid\":\"pe\",\"name\":\"Period\",\"dimensionType\":\"PERIOD\"},\"202712\":{\"uid\":\"202712\",\"code\":\"202712\",\"name\":\"December 2027\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2027-12-01T00:00:00.000\",\"endDate\":\"2027-12-31T00:00:00.000\"},\"202710\":{\"uid\":\"202710\",\"code\":\"202710\",\"name\":\"October 2027\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2027-10-01T00:00:00.000\",\"endDate\":\"2027-10-31T00:00:00.000\"},\"LAST_5_YEARS\":{\"name\":\"Last 5 years\"},\"202711\":{\"uid\":\"202711\",\"code\":\"202711\",\"name\":\"November 2027\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2027-11-01T00:00:00.000\",\"endDate\":\"2027-11-30T00:00:00.000\"},\"P2cwLGskgxn\":{\"uid\":\"P2cwLGskgxn\",\"name\":\"Phone number\",\"description\":\"Phone number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"code\":\"MMD_PER_DOB\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"pe\":{\"uid\":\"pe\",\"name\":\"Period\",\"dimensionType\":\"PERIOD\"},\"uy2gU8kT1jF\":{\"uid\":\"uy2gU8kT1jF\",\"name\":\"MNCH / PNC (Adult Woman)\"},\"AuPLng5hLbE\":{\"uid\":\"AuPLng5hLbE\",\"code\":\"National identifier\",\"name\":\"National identifier\",\"description\":\"National identifier\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"WSGAb5XwJ3Y\":{\"uid\":\"WSGAb5XwJ3Y\",\"name\":\"WHO RMNCH Tracker\"},\"ur1Edk5Oe2n.EPEcjy3FWmI.fTZFU8cWvb3\":{\"uid\":\"fTZFU8cWvb3\",\"code\":\"DE_860002\",\"name\":\"TB lab Hemoglobin\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"fTZFU8cWvb3\":[],\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"DODgdr5Oo2v\":[],\"Qo571yj6Zcn\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"A4xFHyieXys\":[],\"xs8A6tQJY0s\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"Agywv2JGwuq\":[],\"GUOBQt5K2WI\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[\"wfkKVdPBzho\",\"Yjte6foKMny\"],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"a3kGcGDCuk6\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[\"202701\",\"202702\",\"202703\",\"202704\",\"202705\",\"202706\",\"202707\",\"202708\",\"202709\",\"202710\",\"202711\",\"202712\",\"2023\",\"2024\",\"2025\",\"2026\",\"2027\"],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 2, "w75KJ2mc4zz", "First name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        3,
        "IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6",
        "MCH Apgar Score, Child Programme, Birth",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        4,
        "IpHINAT79UW.ouname",
        "Organisation Unit Name, Child Programme",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        5,
        "ur1Edk5Oe2n.EPEcjy3FWmI.fTZFU8cWvb3",
        "TB lab Hemoglobin, TB program, Lab monitoring",
        "TRUE_ONLY",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        6,
        "ur1Edk5Oe2n.enrollmentdate",
        "Start of treatment date, TB program",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of("Ngelehun CHC", "Female", "Geraldine", "", "", "", "2023-01-15 01:00:00.0"));
    validateRow(
        response,
        1,
        List.of("Ngelehun CHC", "FEMALE", "Inés", "", "", "", "2023-03-03 01:00:00.0"));
  }
}
