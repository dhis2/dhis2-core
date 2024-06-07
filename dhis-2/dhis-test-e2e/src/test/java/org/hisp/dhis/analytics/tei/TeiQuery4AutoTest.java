/*
 * Copyright (c) 2004-2024, University of Oslo
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
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.actions.analytics.AnalyticsTeiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/trackedEntities/query" endpoint. */
public class TeiQuery4AutoTest extends AnalyticsApiTest {
  private AnalyticsTeiActions actions = new AnalyticsTeiActions();

  @Test
  public void sortByGlobalOuAsc() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=ouname")
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
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":false},\"items\":{\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"code\":\"MMD_PER_DOB\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"code\":\"MMD_PER_STA\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"GQY2lXrypjO\":{\"uid\":\"GQY2lXrypjO\",\"code\":\"DE_2006099\",\"name\":\"MCH Infant Weight  (g)\",\"description\":\"Infant weight in grams\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"IpHINAT79UW.ZzYYXq4fJie.GQY2lXrypjO\":{\"uid\":\"GQY2lXrypjO\",\"code\":\"DE_2006099\",\"name\":\"MCH Infant Weight  (g)\",\"description\":\"Infant weight in grams\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"GQY2lXrypjO\":[],\"Qo571yj6Zcn\":[],\"DODgdr5Oo2v\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"xs8A6tQJY0s\":[],\"A4xFHyieXys\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"GUOBQt5K2WI\":[],\"Agywv2JGwuq\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[\"wfkKVdPBzho\",\"Yjte6foKMny\"],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);

    // Assert rows.
    validateRow(response, 0, List.of(" Panderu MCHP"));
    validateRow(response, 1, List.of(" Panderu MCHP"));
    validateRow(response, 2, List.of(" Panderu MCHP"));
    validateRow(response, 3, List.of(" Panderu MCHP"));
    validateRow(response, 4, List.of(" Panderu MCHP"));
    validateRow(response, 5, List.of(" Panderu MCHP"));
    validateRow(response, 6, List.of(" Panderu MCHP"));
    validateRow(response, 7, List.of(" Panderu MCHP"));
    validateRow(response, 8, List.of(" Panderu MCHP"));
    validateRow(response, 9, List.of(" Panderu MCHP"));
    validateRow(response, 10, List.of(" Panderu MCHP"));
    validateRow(response, 11, List.of(" Panderu MCHP"));
    validateRow(response, 12, List.of(" Panderu MCHP"));
    validateRow(response, 13, List.of(" Panderu MCHP"));
    validateRow(response, 14, List.of("Adonkia CHP"));
    validateRow(response, 15, List.of("Adonkia CHP"));
    validateRow(response, 16, List.of("Adonkia CHP"));
    validateRow(response, 17, List.of("Adonkia CHP"));
    validateRow(response, 18, List.of("Adonkia CHP"));
    validateRow(response, 19, List.of("Adonkia CHP"));
    validateRow(response, 20, List.of("Adonkia CHP"));
    validateRow(response, 21, List.of("Adonkia CHP"));
    validateRow(response, 22, List.of("Adonkia CHP"));
    validateRow(response, 23, List.of("Adonkia CHP"));
    validateRow(response, 24, List.of("Adonkia CHP"));
    validateRow(response, 25, List.of("Adonkia CHP"));
    validateRow(response, 26, List.of("Adonkia CHP"));
    validateRow(response, 27, List.of("Adonkia CHP"));
    validateRow(response, 28, List.of("Adonkia CHP"));
    validateRow(response, 29, List.of("Adonkia CHP"));
    validateRow(response, 30, List.of("Adonkia CHP"));
    validateRow(response, 31, List.of("Adonkia CHP"));
    validateRow(response, 32, List.of("Adonkia CHP"));
    validateRow(response, 33, List.of("Adonkia CHP"));
    validateRow(response, 34, List.of("Adonkia CHP"));
    validateRow(response, 35, List.of("Adonkia CHP"));
    validateRow(response, 36, List.of("Adonkia CHP"));
    validateRow(response, 37, List.of("Adonkia CHP"));
    validateRow(response, 38, List.of("Afro Arab Clinic"));
    validateRow(response, 39, List.of("Afro Arab Clinic"));
    validateRow(response, 40, List.of("Afro Arab Clinic"));
    validateRow(response, 41, List.of("Afro Arab Clinic"));
    validateRow(response, 42, List.of("Afro Arab Clinic"));
    validateRow(response, 43, List.of("Afro Arab Clinic"));
    validateRow(response, 44, List.of("Afro Arab Clinic"));
    validateRow(response, 45, List.of("Afro Arab Clinic"));
    validateRow(response, 46, List.of("Afro Arab Clinic"));
    validateRow(response, 47, List.of("Afro Arab Clinic"));
    validateRow(response, 48, List.of("Afro Arab Clinic"));
    validateRow(response, 49, List.of("Afro Arab Clinic"));
    validateRow(response, 50, List.of("Agape CHP"));
    validateRow(response, 51, List.of("Agape CHP"));
    validateRow(response, 52, List.of("Agape CHP"));
    validateRow(response, 53, List.of("Agape CHP"));
    validateRow(response, 54, List.of("Agape CHP"));
    validateRow(response, 55, List.of("Agape CHP"));
    validateRow(response, 56, List.of("Agape CHP"));
    validateRow(response, 57, List.of("Agape CHP"));
    validateRow(response, 58, List.of("Agape CHP"));
    validateRow(response, 59, List.of("Agape CHP"));
    validateRow(response, 60, List.of("Agape CHP"));
    validateRow(response, 61, List.of("Agape CHP"));
    validateRow(response, 62, List.of("Agape CHP"));
    validateRow(response, 63, List.of("Agape CHP"));
    validateRow(response, 64, List.of("Agape CHP"));
    validateRow(response, 65, List.of("Agape CHP"));
    validateRow(response, 66, List.of("Agape CHP"));
    validateRow(response, 67, List.of("Agape CHP"));
    validateRow(response, 68, List.of("Agape CHP"));
    validateRow(response, 69, List.of("Agape CHP"));
    validateRow(response, 70, List.of("Agape CHP"));
    validateRow(response, 71, List.of("Ahamadyya Mission Cl"));
    validateRow(response, 72, List.of("Ahamadyya Mission Cl"));
    validateRow(response, 73, List.of("Ahamadyya Mission Cl"));
    validateRow(response, 74, List.of("Ahamadyya Mission Cl"));
    validateRow(response, 75, List.of("Ahamadyya Mission Cl"));
    validateRow(response, 76, List.of("Ahamadyya Mission Cl"));
    validateRow(response, 77, List.of("Ahamadyya Mission Cl"));
    validateRow(response, 78, List.of("Ahamadyya Mission Cl"));
    validateRow(response, 79, List.of("Ahamadyya Mission Cl"));
    validateRow(response, 80, List.of("Ahamadyya Mission Cl"));
    validateRow(response, 81, List.of("Ahamadyya Mission Cl"));
    validateRow(response, 82, List.of("Ahamadyya Mission Cl"));
    validateRow(response, 83, List.of("Ahamadyya Mission Cl"));
    validateRow(response, 84, List.of("Ahamadyya Mission Cl"));
    validateRow(response, 85, List.of("Ahamadyya Mission Cl"));
    validateRow(response, 86, List.of("Ahamadyya Mission Cl"));
    validateRow(response, 87, List.of("Ahamadyya Mission Cl"));
    validateRow(response, 88, List.of("Ahmadiyya Muslim Hospital"));
    validateRow(response, 89, List.of("Ahmadiyya Muslim Hospital"));
    validateRow(response, 90, List.of("Ahmadiyya Muslim Hospital"));
    validateRow(response, 91, List.of("Ahmadiyya Muslim Hospital"));
    validateRow(response, 92, List.of("Ahmadiyya Muslim Hospital"));
    validateRow(response, 93, List.of("Ahmadiyya Muslim Hospital"));
    validateRow(response, 94, List.of("Ahmadiyya Muslim Hospital"));
    validateRow(response, 95, List.of("Ahmadiyya Muslim Hospital"));
    validateRow(response, 96, List.of("Ahmadiyya Muslim Hospital"));
    validateRow(response, 97, List.of("Ahmadiyya Muslim Hospital"));
    validateRow(response, 98, List.of("Ahmadiyya Muslim Hospital"));
    validateRow(response, 99, List.of("Ahmadiyya Muslim Hospital"));
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
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":false},\"items\":{\"IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"code\":\"MMD_PER_STA\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"cejWyOfXge6\":{\"uid\":\"cejWyOfXge6\",\"name\":\"Gender\",\"description\":\"Gender\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"code\":\"MMD_PER_DOB\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW.A03MvHHogjR.occurreddate\":{\"name\":\"Report date\",\"dimensionType\":\"PERIOD\"},\"IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"bx6fsa0t90x\":[],\"Qo571yj6Zcn\":[],\"DODgdr5Oo2v\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"xs8A6tQJY0s\":[],\"A4xFHyieXys\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"GUOBQt5K2WI\":[],\"Agywv2JGwuq\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"a3kGcGDCuk6\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
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
        "{\"pager\":{\"page\":1,\"pageSize\":10,\"isLastPage\":false},\"items\":{\"Mnp3oXrpAbK\":{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\",\"name\":\"Female\"},\"IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"xjA5E9MimMU\":{\"uid\":\"xjA5E9MimMU\",\"name\":\"Civil status\",\"options\":[{\"uid\":\"wfkKVdPBzho\",\"code\":\"Single or widow\"},{\"uid\":\"Yjte6foKMny\",\"code\":\"Married (conjugal cohabitation)\"}]},\"bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"code\":\"MMD_PER_STA\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"Yjte6foKMny\":{\"uid\":\"Yjte6foKMny\",\"code\":\"Married (conjugal cohabitation)\",\"name\":\"Married (conjugal cohabitation)\"},\"a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"cejWyOfXge6\":{\"uid\":\"cejWyOfXge6\",\"name\":\"Gender\",\"description\":\"Gender\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"code\":\"MMD_PER_DOB\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"wfkKVdPBzho\":{\"uid\":\"wfkKVdPBzho\",\"code\":\"Single or widow\",\"name\":\"Single or widow\"},\"pC3N9N77UmT\":{\"uid\":\"pC3N9N77UmT\",\"name\":\"Gender\",\"options\":[{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\"}]}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"bx6fsa0t90x\":[],\"Qo571yj6Zcn\":[],\"DODgdr5Oo2v\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"xs8A6tQJY0s\":[],\"A4xFHyieXys\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"GUOBQt5K2WI\":[],\"Agywv2JGwuq\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[\"wfkKVdPBzho\",\"Yjte6foKMny\"],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"a3kGcGDCuk6\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
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
        response,
        2,
        "lastupdatedbydisplayname",
        "Last updated by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response, 3, "created", "Created", "DATETIME", "java.time.LocalDateTime", false, true);
    validateHeader(
        response, 4, "createdbydisplayname", "Created by", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 5, "storedby", "Stored by", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 6, "geometry", "Geometry", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 7, "longitude", "Longitude", "NUMBER", "java.lang.Double", false, true);
    validateHeader(response, 8, "latitude", "Latitude", "NUMBER", "java.lang.Double", false, true);
    validateHeader(
        response, 9, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 10, "oucode", "Organisation unit code", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        11,
        "ounamehierarchy",
        "Organisation unit hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response, 12, "w75KJ2mc4zz", "First name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 13, "zDhUuAYrxNC", "Last name", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 14, "NDXw0cluzSw", "Email", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 15, "iESIqZ0R0R0", "Date of birth", "DATE", "java.time.LocalDate", false, true);
    validateHeader(response, 16, "VqEFza8wbwA", "Address", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 17, "OvY4VVhSDeJ", "Weight in kg", "NUMBER", "java.lang.Double", false, true);
    validateHeader(
        response, 18, "lw1SqmMlnfh", "Height in cm", "NUMBER", "java.lang.Double", false, true);
    validateHeader(response, 19, "cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 20, "xs8A6tQJY0s", "TB identifier", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 21, "spFvx9FndA4", "Age", "AGE", "java.util.Date", false, true);
    validateHeader(response, 22, "FO4sWYJ64LQ", "City", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 23, "GUOBQt5K2WI", "State", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 24, "n9nUvfpTsxQ", "Zip code", "NUMBER", "java.lang.Double", false, true);
    validateHeader(
        response,
        25,
        "P2cwLGskgxn",
        "Phone number",
        "PHONE_NUMBER",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        26,
        "G7vUx908SwP",
        "Residence location",
        "COORDINATE",
        "org.opengis.geometry.primitive.Point",
        false,
        true);
    validateHeader(
        response, 27, "o9odfev2Ty5", "Mother maiden name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        28,
        "AuPLng5hLbE",
        "National identifier",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response, 29, "A4xFHyieXys", "Occupation", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 30, "kyIzQsj96BD", "Company", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 31, "ruQQnf6rswq", "TB number", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 32, "VHfUeXpawmE", "Vehicle", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 33, "H9IlTX2X6SL", "Blood type", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 34, "Qo571yj6Zcn", "Latitude", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 35, "RG7uGl4w5Jq", "Longitude", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 36, "lZGmxYbs97q", "Unique ID", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 37, "DODgdr5Oo2v", "Provider ID", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 38, "ZcBPrXKahq2", "Postal code", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        39,
        "Agywv2JGwuq",
        "Mobile number",
        "PHONE_NUMBER",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response, 40, "KmEUg2hHEtx", "Email address", "EMAIL", "java.lang.String", false, true);
    validateHeader(
        response, 41, "gHGyrwKPzej", "Birth date", "DATE", "java.time.LocalDate", false, true);
    validateHeader(
        response, 42, "ciq2USN94oJ", "Civil status", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        43,
        "IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x",
        "MCH BCG dose, Child Programme, Birth",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        44,
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
            "",
            "",
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
            "",
            ""));
    validateRow(
        response,
        2,
        List.of(
            "vu9dsAuJ29q",
            "2017-05-22 22:34:09.669",
            "",
            "2015-10-14 14:14:21.384",
            "",
            "",
            "",
            "",
            "",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "Ava",
            "Didriksson",
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
            "9191132445122",
            "",
            "",
            "",
            "",
            "1985-10-01 00:00:00.0",
            "Married (conjugal cohabitation)",
            "",
            ""));
    validateRow(
        response,
        3,
        List.of(
            "JYWyAYTMdRv",
            "2017-01-26 13:48:13.371",
            "",
            "2017-01-26 13:48:13.37",
            "",
            "",
            "",
            "",
            "",
            "Blamawo MCHP",
            "OU_73727",
            "Sierra Leone / Bo / Baoma / Blamawo MCHP",
            "Rose",
            "Hudson",
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
            "",
            "",
            "",
            "",
            "1986-07-04 00:00:00.0",
            "",
            "",
            ""));
    validateRow(
        response,
        4,
        List.of(
            "LtbQLsx5zol",
            "2017-01-26 13:48:13.37",
            "",
            "2017-01-26 13:48:13.369",
            "",
            "",
            "",
            "",
            "",
            "Petifu Mayepoh MCHP",
            "OU_268211",
            "Sierra Leone / Tonkolili / Gbonkonlenken / Petifu Mayepoh MCHP",
            "Beverly",
            "Boyd",
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
            "",
            "",
            "",
            "",
            "1972-05-03 00:00:00.0",
            "",
            "",
            ""));
    validateRow(
        response,
        5,
        List.of(
            "GRxWzijJ5jt",
            "2017-01-26 13:48:13.368",
            "",
            "2017-01-26 13:48:13.367",
            "",
            "",
            "",
            "",
            "",
            "Kondewakoro CHP",
            "OU_233315",
            "Sierra Leone / Kono / Toli / Kondewakoro CHP",
            "Sandra",
            "Ferguson",
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
            "",
            "",
            "",
            "",
            "1982-03-04 00:00:00.0",
            "",
            "",
            ""));
    validateRow(
        response,
        6,
        List.of(
            "D8G28uT0rmc",
            "2017-01-26 13:48:13.366",
            "",
            "2017-01-26 13:48:13.365",
            "",
            "",
            "",
            "",
            "",
            "Mathen MCHP",
            "OU_254990",
            "Sierra Leone / Port Loko / Lokomasama / Mathen MCHP",
            "Heather",
            "Hughes",
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
            "",
            "",
            "",
            "",
            "1987-01-28 00:00:00.0",
            "",
            "",
            ""));
    validateRow(
        response,
        7,
        List.of(
            "IOR1AXXl24H",
            "2017-01-26 13:48:13.363",
            "",
            "2017-01-26 13:48:13.363",
            "",
            "",
            "",
            "",
            "",
            "Mbokie CHP",
            "OU_197401",
            "Sierra Leone / Bonthe / Sittia / Mbokie CHP",
            "Melissa",
            "Fuller",
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
            "",
            "",
            "",
            "",
            "1981-03-21 00:00:00.0",
            "",
            "",
            ""));
    validateRow(
        response,
        8,
        List.of(
            "Y8aEiTpHSSQ",
            "2017-01-26 13:48:13.36",
            "",
            "2017-01-26 13:48:13.36",
            "",
            "",
            "",
            "",
            "",
            "Kolisokor MCHP",
            "OU_193259",
            "Sierra Leone / Bombali / Makari Gbanti / Kolisokor MCHP",
            "Irene",
            "Jacobs",
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
            "",
            "",
            "",
            "",
            "1988-01-18 00:00:00.0",
            "",
            "",
            ""));
    validateRow(
        response,
        9,
        List.of(
            "wuDUAklkAHS",
            "2017-01-26 13:48:13.359",
            "",
            "2017-01-26 13:48:13.359",
            "",
            "",
            "",
            "",
            "",
            "Grey Bush CHC",
            "OU_651068",
            "Sierra Leone / Western Area / Freetown / Grey Bush CHC",
            "Lillian",
            "Butler",
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
            "",
            "",
            "",
            "",
            "1985-03-17 00:00:00.0",
            "",
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
            .add("relativePeriodDate=2022-01-27");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(7)))
        .body("rows", hasSize(equalTo(30)))
        .body("height", equalTo(30))
        .body("width", equalTo(7))
        .body("headerWidth", equalTo(7));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":30,\"isLastPage\":false},\"items\":{\"fTZFU8cWvb3\":{\"uid\":\"fTZFU8cWvb3\",\"code\":\"DE_860002\",\"name\":\"TB lab Hemoglobin\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"202109\":{\"uid\":\"202109\",\"code\":\"202109\",\"name\":\"September 2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-09-01T00:00:00.000\",\"endDate\":\"2021-09-30T00:00:00.000\"},\"202107\":{\"uid\":\"202107\",\"code\":\"202107\",\"name\":\"July 2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-07-01T00:00:00.000\",\"endDate\":\"2021-07-31T00:00:00.000\"},\"202108\":{\"uid\":\"202108\",\"code\":\"202108\",\"name\":\"August 2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-08-01T00:00:00.000\",\"endDate\":\"2021-08-31T00:00:00.000\"},\"202105\":{\"uid\":\"202105\",\"code\":\"202105\",\"name\":\"May 2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-05-01T00:00:00.000\",\"endDate\":\"2021-05-31T00:00:00.000\"},\"202106\":{\"uid\":\"202106\",\"code\":\"202106\",\"name\":\"June 2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-06-01T00:00:00.000\",\"endDate\":\"2021-06-30T00:00:00.000\"},\"202103\":{\"uid\":\"202103\",\"code\":\"202103\",\"name\":\"March 2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-03-01T00:00:00.000\",\"endDate\":\"2021-03-31T00:00:00.000\"},\"202104\":{\"uid\":\"202104\",\"code\":\"202104\",\"name\":\"April 2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-04-01T00:00:00.000\",\"endDate\":\"2021-04-30T00:00:00.000\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"IpHINAT79UW.ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"202112\":{\"uid\":\"202112\",\"code\":\"202112\",\"name\":\"December 2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-12-01T00:00:00.000\",\"endDate\":\"2021-12-31T00:00:00.000\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"202110\":{\"uid\":\"202110\",\"code\":\"202110\",\"name\":\"October 2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-10-01T00:00:00.000\",\"endDate\":\"2021-10-31T00:00:00.000\"},\"202111\":{\"uid\":\"202111\",\"code\":\"202111\",\"name\":\"November 2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-11-01T00:00:00.000\",\"endDate\":\"2021-11-30T00:00:00.000\"},\"EPEcjy3FWmI\":{\"uid\":\"EPEcjy3FWmI\",\"name\":\"Lab monitoring\",\"description\":\"Laboratory monitoring\"},\"ur1Edk5Oe2n\":{\"uid\":\"ur1Edk5Oe2n\",\"name\":\"TB program\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"pC3N9N77UmT\":{\"uid\":\"pC3N9N77UmT\",\"name\":\"Gender\",\"options\":[{\"uid\":\"rBvjJYbMCVx\",\"code\":\"Male\"},{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\"}]},\"Mnp3oXrpAbK\":{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\",\"name\":\"Female\"},\"rBvjJYbMCVx\":{\"uid\":\"rBvjJYbMCVx\",\"code\":\"Male\",\"name\":\"Male\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"w75KJ2mc4zz\":{\"uid\":\"w75KJ2mc4zz\",\"code\":\"MMD_PER_NAM\",\"name\":\"First name\",\"description\":\"First name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ur1Edk5Oe2n.pe\":{\"uid\":\"pe\",\"name\":\"Period\",\"dimensionType\":\"PERIOD\"},\"a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"2021\":{\"uid\":\"2021\",\"code\":\"2021\",\"name\":\"2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-01-01T00:00:00.000\",\"endDate\":\"2021-12-31T00:00:00.000\"},\"2020\":{\"uid\":\"2020\",\"code\":\"2020\",\"name\":\"2020\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2020-01-01T00:00:00.000\",\"endDate\":\"2020-12-31T00:00:00.000\"},\"LAST_5_YEARS\":{\"name\":\"Last 5 years\"},\"cejWyOfXge6\":{\"uid\":\"cejWyOfXge6\",\"name\":\"Gender\",\"description\":\"Gender\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"202101\":{\"uid\":\"202101\",\"code\":\"202101\",\"name\":\"January 2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-01-01T00:00:00.000\",\"endDate\":\"2021-01-31T00:00:00.000\"},\"202102\":{\"uid\":\"202102\",\"code\":\"202102\",\"name\":\"February 2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-02-01T00:00:00.000\",\"endDate\":\"2021-02-28T00:00:00.000\"},\"2019\":{\"uid\":\"2019\",\"code\":\"2019\",\"name\":\"2019\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2019-01-01T00:00:00.000\",\"endDate\":\"2019-12-31T00:00:00.000\"},\"ur1Edk5Oe2n.enrollmentdate\":{\"name\":\"Start of treatment date\",\"dimensionType\":\"PERIOD\"},\"2018\":{\"uid\":\"2018\",\"code\":\"2018\",\"name\":\"2018\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2018-01-01T00:00:00.000\",\"endDate\":\"2018-12-31T00:00:00.000\"},\"2017\":{\"uid\":\"2017\",\"code\":\"2017\",\"name\":\"2017\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2017-01-01T00:00:00.000\",\"endDate\":\"2017-12-31T00:00:00.000\"},\"pe\":{\"uid\":\"pe\",\"name\":\"Period\",\"dimensionType\":\"PERIOD\"},\"ur1Edk5Oe2n.EPEcjy3FWmI.fTZFU8cWvb3\":{\"uid\":\"fTZFU8cWvb3\",\"code\":\"DE_860002\",\"name\":\"TB lab Hemoglobin\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"fTZFU8cWvb3\":[],\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"Qo571yj6Zcn\":[],\"DODgdr5Oo2v\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"xs8A6tQJY0s\":[],\"A4xFHyieXys\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"GUOBQt5K2WI\":[],\"Agywv2JGwuq\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[\"wfkKVdPBzho\",\"Yjte6foKMny\"],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"a3kGcGDCuk6\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[\"2017\",\"2018\",\"2019\",\"2020\",\"2021\"],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
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
        List.of("Bai Largo MCHP", "Female", "Furuta", "", "", "", "2021-02-22 12:28:59.5"));
    validateRow(
        response,
        1,
        List.of("Konjo CHP", "Female", "Sesuna", "", "", "", "2021-02-22 12:29:10.226"));
    validateRow(
        response,
        2,
        List.of("Mokpende MCHP", "Female", "Simret", "", "", "", "2021-02-22 12:29:17.878"));
    validateRow(
        response,
        3,
        List.of("Konta Wallah MCHP", "Female", "Sarama", "", "", "", "2021-02-22 12:30:18.762"));
    validateRow(
        response,
        4,
        List.of("Magbaft MCHP", "Female", "Makda", "", "", "", "2021-02-22 12:36:38.143"));
    validateRow(
        response,
        5,
        List.of("Masoko MCHP", "Female", "Miniya", "", "", "", "2021-02-22 12:37:38.153"));
    validateRow(
        response,
        6,
        List.of("Mamalikie MCHP", "Male", "Iggi", "", "", "", "2021-02-22 12:37:43.562"));
    validateRow(
        response,
        7,
        List.of("Ninkikoro MCHP", "Male", "Medhanie", "", "", "", "2021-02-22 12:37:59.06"));
    validateRow(
        response,
        8,
        List.of("Seidu MCHP", "Male", "Daniel", "", "", "", "2021-02-22 12:38:16.207"));
    validateRow(
        response,
        9,
        List.of("Bangoma MCHP", "Female", "Sarama", "", "", "", "2021-02-22 12:42:36.654"));
    validateRow(
        response,
        10,
        List.of("Blamawo MCHP", "Male", "Abdullah", "", "", "", "2021-02-23 12:28:45.299"));
    validateRow(
        response,
        11,
        List.of(
            "Benguima Grassfield MCHP", "Female", "Almaz", "", "", "", "2021-02-23 12:32:07.441"));
    validateRow(
        response,
        12,
        List.of("Degbuama MCHP", "Male", "Amanuel", "", "", "", "2021-02-23 12:34:11.197"));
    validateRow(
        response,
        13,
        List.of("Suen CHP", "Female", "Mezan", "", "", "", "2021-02-23 12:40:28.099"));
    validateRow(
        response,
        14,
        List.of("Petifu CHC", "Male", "Medhane", "", "", "", "2021-02-23 12:42:51.379"));
    validateRow(
        response,
        15,
        List.of("Kpowubu MCHP", "Female", "Furuta", "", "", "", "2021-02-24 12:28:13.16"));
    validateRow(
        response,
        16,
        List.of("Massahun MCHP", "Female", "Luwam", "", "", "", "2021-02-24 12:29:44.902"));
    validateRow(
        response,
        17,
        List.of("Kawula CHP", "Male", "Kifle", "", "", "", "2021-02-24 12:30:06.041"));
    validateRow(
        response,
        18,
        List.of("Kamiendor MCHP", "Male", "Demsas", "", "", "", "2021-02-24 12:33:12.117"));
    validateRow(
        response,
        19,
        List.of("Mapaki CHC", "Female", "Demet", "", "", "", "2021-02-24 12:33:15.016"));
    validateRow(
        response,
        20,
        List.of("Makali CHC", "Male", "Yemane", "", "", "", "2021-02-24 12:39:47.712"));
    validateRow(
        response,
        21,
        List.of("Koindukura MCHP", "Male", "Brhane", "", "", "", "2021-02-24 12:40:06.942"));
    validateRow(
        response,
        22,
        List.of("Kamabai CHC", "Female", "Selamawit", "", "", "", "2021-02-25 12:28:25.771"));
    validateRow(
        response,
        23,
        List.of("Mafufuneh MCHP", "Male", "Massawa", "", "", "", "2021-02-25 12:29:02.87"));
    validateRow(
        response,
        24,
        List.of("St. Mary's Clinic", "Female", "Asmeret", "", "", "", "2021-02-25 12:29:39.291"));
    validateRow(
        response,
        25,
        List.of("Bomu Saamba CHP", "Female", "Yorda", "", "", "", "2021-02-25 12:29:52.461"));
    validateRow(
        response,
        26,
        List.of("Geoma Jagor CHC", "Female", "Bisirat", "", "", "", "2021-02-25 12:30:46.251"));
    validateRow(
        response,
        27,
        List.of("Makona MCHP", "Male", "Sebhat", "", "", "", "2021-02-25 12:31:45.792"));
    validateRow(
        response,
        28,
        List.of("Bo Govt. Hosp.", "Male", "Tewolde", "", "", "", "2021-02-25 12:33:20.889"));
    validateRow(
        response,
        29,
        List.of("Fairo CHC", "Male", "Petros", "", "", "", "2021-02-25 12:37:04.983"));
  }
}
