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
public class TrackedEntityQuery5AutoTest extends AnalyticsApiTest {
  private AnalyticsTrackedEntityActions actions = new AnalyticsTrackedEntityActions();

  @Test
  public void randomQueryWithTeCreated() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add(
                "headers=ouname,gHGyrwKPzej,ciq2USN94oJ,cejWyOfXge6,IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6,created")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("pageSize=20")
            .add("page=1")
            .add(
                "dimension=ou:USER_ORGUNIT,gHGyrwKPzej,ciq2USN94oJ,cejWyOfXge6,IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6")
            .add("desc=created")
            .add("relativePeriodDate=2017-01-27");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(7)))
        .body("rows", hasSize(equalTo(20)))
        .body("height", equalTo(20))
        .body("width", equalTo(7))
        .body("headerWidth", equalTo(7));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":20,\"isLastPage\":false},\"items\":{\"IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"code\":\"MMD_PER_STA\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"cejWyOfXge6\":{\"uid\":\"cejWyOfXge6\",\"name\":\"Gender\",\"description\":\"Gender\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"code\":\"MMD_PER_DOB\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"bx6fsa0t90x\":[],\"Qo571yj6Zcn\":[],\"DODgdr5Oo2v\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"xs8A6tQJY0s\":[],\"A4xFHyieXys\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"GUOBQt5K2WI\":[],\"Agywv2JGwuq\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"a3kGcGDCuk6\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "gHGyrwKPzej", "Birth date", "DATE", "java.time.LocalDate", false, true);
    validateHeader(
        response, 2, "ciq2USN94oJ", "Civil status", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        4,
        "IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x",
        "MCH BCG dose, Child Programme, Birth",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        5,
        "IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6",
        "MCH Apgar Score, Child Programme, Birth",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response, 6, "created", "Created", "DATETIME", "java.time.LocalDateTime", false, true);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of("Blamawo MCHP", "1986-07-04 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.37"));
    validateRow(
        response,
        1,
        List.of(
            "Petifu Mayepoh MCHP",
            "1972-05-03 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:48:13.369"));
    validateRow(
        response,
        2,
        List.of(
            "Kondewakoro CHP", "1982-03-04 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.367"));
    validateRow(
        response,
        3,
        List.of("Mathen MCHP", "1987-01-28 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.365"));
    validateRow(
        response,
        4,
        List.of("Mbokie CHP", "1981-03-21 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.363"));
    validateRow(
        response,
        5,
        List.of(
            "Kolisokor MCHP", "1988-01-18 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.36"));
    validateRow(
        response,
        6,
        List.of(
            "Grey Bush CHC", "1985-03-17 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.359"));
    validateRow(
        response,
        7,
        List.of(
            "Dankawalia MCHP", "1978-03-30 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.357"));
    validateRow(
        response,
        8,
        List.of(
            "Kenema Gbandoma MCHP",
            "1984-09-14 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:48:13.355"));
    validateRow(
        response,
        9,
        List.of(
            "Bangoma MCHP", "1989-02-22 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.353"));
    validateRow(
        response,
        10,
        List.of(
            "Blessed Mokaka East Clinic",
            "1979-11-04 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:48:13.35"));
    validateRow(
        response,
        11,
        List.of(
            "MCH Static/U5", "1981-10-03 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.349"));
    validateRow(
        response,
        12,
        List.of(
            "Sembehun CHC", "1973-05-17 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.347"));
    validateRow(
        response,
        13,
        List.of(
            "Baoma (Koya) CHC",
            "1976-07-07 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:48:13.346"));
    validateRow(
        response,
        14,
        List.of("Konjo MCHP", "1980-09-30 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.344"));
    validateRow(
        response,
        15,
        List.of("Fogbo CHP", "1985-08-09 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.343"));
    validateRow(
        response,
        16,
        List.of(
            "Lakka/Ogoo Farm CHC",
            "1986-05-10 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:48:13.34"));
    validateRow(
        response,
        17,
        List.of(
            "Moriba Town CHC", "1980-03-18 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.339"));
    validateRow(
        response,
        18,
        List.of(
            "Makonkorie MCHP", "1978-08-12 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.336"));
    validateRow(
        response,
        19,
        List.of(
            "Gbo-Kakajama 1 MCHP",
            "1975-09-20 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:48:13.334"));
  }

  @Test
  public void sortByEnrollmentOuAsc() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=IpHINAT79UW.oucode,lastupdated")
            .add("headers=ouname,w75KJ2mc4zz,zDhUuAYrxNC,IpHINAT79UW.ouname,lastupdated")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("pageSize=20")
            .add("page=1")
            .add("dimension=ou:USER_ORGUNIT,gHGyrwKPzej,ciq2USN94oJ")
            .add("relativePeriodDate=2017-01-27");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(5)))
        .body("rows", hasSize(equalTo(20)))
        .body("height", equalTo(20))
        .body("width", equalTo(5))
        .body("headerWidth", equalTo(5));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"isLastPage\":false,\"pageSize\":20,\"page\":1},\"items\":{\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"aggregationType\":\"NONE\",\"code\":\"MMD_PER_DOB\",\"valueType\":\"DATE\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"totalAggregationType\":\"NONE\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"valueType\":\"TEXT\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"totalAggregationType\":\"SUM\"},\"zDhUuAYrxNC\":{\"uid\":\"zDhUuAYrxNC\",\"aggregationType\":\"NONE\",\"valueType\":\"TEXT\",\"name\":\"Last name\",\"description\":\"Last name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"totalAggregationType\":\"NONE\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"aggregationType\":\"NONE\",\"code\":\"MMD_PER_STA\",\"valueType\":\"TEXT\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"totalAggregationType\":\"NONE\"},\"ou\":{\"uid\":\"ou\",\"dimensionType\":\"ORGANISATION_UNIT\",\"name\":\"Organisation unit\"},\"w75KJ2mc4zz\":{\"uid\":\"w75KJ2mc4zz\",\"aggregationType\":\"NONE\",\"code\":\"MMD_PER_NAM\",\"valueType\":\"TEXT\",\"name\":\"First name\",\"description\":\"First name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"totalAggregationType\":\"NONE\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"ouname\":{\"dimensionType\":\"ORGANISATION_UNIT\",\"name\":\"Organisation Unit Name\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"Qo571yj6Zcn\":[],\"DODgdr5Oo2v\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"xs8A6tQJY0s\":[],\"A4xFHyieXys\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"GUOBQt5K2WI\":[],\"Agywv2JGwuq\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[\"wfkKVdPBzho\",\"Yjte6foKMny\"],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "w75KJ2mc4zz", "First name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 2, "zDhUuAYrxNC", "Last name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        3,
        "IpHINAT79UW.ouname",
        "Organisation Unit Name, Child Programme",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of(
            "Praise Foundation CHC",
            "Beverly",
            "Armstrong",
            "Praise Foundation CHC",
            "2015-08-06 21:20:47.477"));
    validateRow(
        response,
        1,
        List.of(
            "Praise Foundation CHC",
            "Mark",
            "Long",
            "Praise Foundation CHC",
            "2015-08-06 21:20:50.688"));
    validateRow(
        response,
        2,
        List.of(
            "Praise Foundation CHC",
            "Evelyn",
            "Smith",
            "Praise Foundation CHC",
            "2015-08-06 21:20:50.693"));

    validateRow( // last row
        response,
        19,
        List.of("Bucksal Clinic", "Jeremy", "Graham", "Bucksal Clinic", "2015-08-06 21:20:49.387"));
  }

  @Test
  public void sortByProgramStatusAsc() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=IpHINAT79UW.programstatus")
            .add("headers=IpHINAT79UW.programstatus")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("pageSize=10")
            .add("page=1")
            .add("dimension=ou:USER_ORGUNIT,gHGyrwKPzej,ciq2USN94oJ")
            .add("relativePeriodDate=2017-01-27");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(1)))
        .body("rows", hasSize(equalTo(10)))
        .body("height", equalTo(10))
        .body("width", equalTo(1))
        .body("headerWidth", equalTo(1));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":10,\"isLastPage\":false},\"items\":{\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"code\":\"MMD_PER_DOB\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"code\":\"MMD_PER_STA\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"Qo571yj6Zcn\":[],\"DODgdr5Oo2v\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"xs8A6tQJY0s\":[],\"A4xFHyieXys\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"GUOBQt5K2WI\":[],\"Agywv2JGwuq\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[\"wfkKVdPBzho\",\"Yjte6foKMny\"],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response,
        0,
        "IpHINAT79UW.programstatus",
        "Program Status, Child Programme",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // Assert rows.
    validateRow(response, 0, List.of("ACTIVE"));
    validateRow(response, 1, List.of("ACTIVE"));
    validateRow(response, 2, List.of("ACTIVE"));
    validateRow(response, 3, List.of("ACTIVE"));
    validateRow(response, 4, List.of("ACTIVE"));
    validateRow(response, 5, List.of("ACTIVE"));
    validateRow(response, 6, List.of("ACTIVE"));
    validateRow(response, 7, List.of("ACTIVE"));
    validateRow(response, 8, List.of("ACTIVE"));
    validateRow(response, 9, List.of("ACTIVE"));
  }

  @Test
  public void randomWithFilterSortByProgramEnrollmentDateAsc() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=qDkgAbB5Jlk.enrollmentdate")
            .add(
                "headers=ouname,B6TnnFMgmCk,qDkgAbB5Jlk.programstatus,qDkgAbB5Jlk.enrollmentdate,qDkgAbB5Jlk.ouname,qDkgAbB5Jlk.C0aLZo75dgJ.JuTpJ2Ywq5b,qDkgAbB5Jlk.hYyB7FUS5eR.fazCI2ygYkq,qDkgAbB5Jlk.wYTF0YCHMWr.rzhHSqK3lQq,qDkgAbB5Jlk.hYyB7FUS5eR.GyJHQUWZ9Rl,qDkgAbB5Jlk.hYyB7FUS5eR.JINgGHgqzSN")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("enrollmentDate=qDkgAbB5Jlk.LAST_YEAR,qDkgAbB5Jlk.LAST_5_YEARS")
            .add("rowContext=true")
            .add("pageSize=100")
            .add("page=1")
            .add(
                "dimension=ou:ImspTQPwCqd;O6uvpzGd5pu;fdc6uOvgoji,B6TnnFMgmCk,qDkgAbB5Jlk.ou:USER_ORGUNIT,qDkgAbB5Jlk.C0aLZo75dgJ.JuTpJ2Ywq5b,qDkgAbB5Jlk.hYyB7FUS5eR.fazCI2ygYkq:IN:PASSIVE;PROACTIVE;REACTIVE,qDkgAbB5Jlk.wYTF0YCHMWr.rzhHSqK3lQq:IN:1,qDkgAbB5Jlk.hYyB7FUS5eR.GyJHQUWZ9Rl,qDkgAbB5Jlk.hYyB7FUS5eR.JINgGHgqzSN:GT:1")
            .add("relativePeriodDate=2023-01-01");

    // When
    ApiResponse response = actions.query().get("Zy2SEgA61ys", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(10)))
        .body("rows", hasSize(equalTo(3)))
        .body("height", equalTo(3))
        .body("width", equalTo(10))
        .body("headerWidth", equalTo(10));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"qDkgAbB5Jlk.hYyB7FUS5eR.GyJHQUWZ9Rl\":{\"uid\":\"GyJHQUWZ9Rl\",\"name\":\"GPS DataElement\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"COORDINATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"qDkgAbB5Jlk.wYTF0YCHMWr.rzhHSqK3lQq\":{\"uid\":\"rzhHSqK3lQq\",\"name\":\"Fever\",\"description\":\"Does the case have fever or not\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"GyJHQUWZ9Rl\":{\"uid\":\"GyJHQUWZ9Rl\",\"name\":\"GPS DataElement\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"COORDINATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"JINgGHgqzSN\":{\"uid\":\"JINgGHgqzSN\",\"code\":\"CS004\",\"name\":\"Weight\",\"description\":\"The weight of the case in kg at the time of diagnosis\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"D9yTOOLGz0j\":{\"uid\":\"D9yTOOLGz0j\",\"name\":\"Malaria case detection type\",\"options\":[{\"uid\":\"SepVHxunjMN\",\"code\":\"PASSIVE\"},{\"uid\":\"fa1IdKtq4VX\",\"code\":\"REACTIVE\"}]},\"O6uvpzGd5pu\":{\"uid\":\"O6uvpzGd5pu\",\"code\":\"OU_264\",\"name\":\"Bo\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"qDkgAbB5Jlk.hYyB7FUS5eR.JINgGHgqzSN\":{\"uid\":\"JINgGHgqzSN\",\"code\":\"CS004\",\"name\":\"Weight\",\"description\":\"The weight of the case in kg at the time of diagnosis\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"qDkgAbB5Jlk.pe\":{\"uid\":\"pe\",\"name\":\"Period\",\"dimensionType\":\"PERIOD\"},\"JuTpJ2Ywq5b\":{\"uid\":\"JuTpJ2Ywq5b\",\"name\":\"Age of LLINs\",\"description\":\"The age of the LLINs within a cases household in years\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"qDkgAbB5Jlk.hYyB7FUS5eR.fazCI2ygYkq\":{\"uid\":\"fazCI2ygYkq\",\"name\":\"Case detection\",\"description\":\"Determines the method that was used to detect the case\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"COUNT\",\"totalAggregationType\":\"SUM\"},\"SepVHxunjMN\":{\"uid\":\"SepVHxunjMN\",\"code\":\"PASSIVE\",\"name\":\"Passive\"},\"fazCI2ygYkq\":{\"uid\":\"fazCI2ygYkq\",\"name\":\"Case detection\",\"description\":\"Determines the method that was used to detect the case\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"COUNT\",\"totalAggregationType\":\"SUM\"},\"rzhHSqK3lQq\":{\"uid\":\"rzhHSqK3lQq\",\"name\":\"Fever\",\"description\":\"Does the case have fever or not\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"qDkgAbB5Jlk.C0aLZo75dgJ.JuTpJ2Ywq5b\":{\"uid\":\"JuTpJ2Ywq5b\",\"name\":\"Age of LLINs\",\"description\":\"The age of the LLINs within a cases household in years\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"LAST_YEAR\":{\"name\":\"Last year\"},\"C0aLZo75dgJ\":{\"uid\":\"C0aLZo75dgJ\",\"name\":\"Household investigation\",\"description\":\"Nearby household investigations occur when an index case is identified within a specific geographical area.\"},\"qDkgAbB5Jlk.ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"B6TnnFMgmCk\":{\"uid\":\"B6TnnFMgmCk\",\"name\":\"Age (years)\",\"description\":\"Age in years\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"2022\":{\"uid\":\"2022\",\"code\":\"2022\",\"name\":\"2022\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-01-01T00:00:00.000\",\"endDate\":\"2022-12-31T00:00:00.000\"},\"2021\":{\"uid\":\"2021\",\"code\":\"2021\",\"name\":\"2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-01-01T00:00:00.000\",\"endDate\":\"2021-12-31T00:00:00.000\"},\"2020\":{\"uid\":\"2020\",\"code\":\"2020\",\"name\":\"2020\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2020-01-01T00:00:00.000\",\"endDate\":\"2020-12-31T00:00:00.000\"},\"LAST_5_YEARS\":{\"name\":\"Last 5 years\"},\"hYyB7FUS5eR\":{\"uid\":\"hYyB7FUS5eR\",\"name\":\"Diagnosis & treatment\",\"description\":\"This stage is used to identify initial diagnosis and treatment. This includes the method of case detection, information about the case include travel history, method of diagnosis, malaria species type and treatment details. \"},\"fdc6uOvgoji\":{\"uid\":\"fdc6uOvgoji\",\"code\":\"OU_193190\",\"name\":\"Bombali\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"2019\":{\"uid\":\"2019\",\"code\":\"2019\",\"name\":\"2019\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2019-01-01T00:00:00.000\",\"endDate\":\"2019-12-31T00:00:00.000\"},\"2018\":{\"uid\":\"2018\",\"code\":\"2018\",\"name\":\"2018\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2018-01-01T00:00:00.000\",\"endDate\":\"2018-12-31T00:00:00.000\"},\"pe\":{\"uid\":\"pe\",\"name\":\"Period\",\"dimensionType\":\"PERIOD\"},\"qDkgAbB5Jlk\":{\"uid\":\"qDkgAbB5Jlk\",\"name\":\"Malaria case diagnosis, treatment and investigation\",\"description\":\"All cases in an elimination setting should be registered in this program. Includes relevant case identifiers/details including the ID, Name, Index, Age, Gender, Location,etc..\"},\"qDkgAbB5Jlk.enrollmentdate\":{\"name\":\"Enrollment date\",\"dimensionType\":\"PERIOD\"},\"wYTF0YCHMWr\":{\"uid\":\"wYTF0YCHMWr\",\"name\":\"Case investigation & classification\",\"description\":\"This includes the investigation of the index case (including the confirmation of symptoms, previous malaria history, LLIN usage details, IRS details), and the summary of the results for the case investigation including the final case classification (both the species type and the case classification). \"},\"fa1IdKtq4VX\":{\"uid\":\"fa1IdKtq4VX\",\"code\":\"REACTIVE\",\"name\":\"Reactive (ACD)\"},\"qDkgAbB5Jlk.ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"B6TnnFMgmCk\":[],\"BiTsLcJQ95V\":[],\"GyJHQUWZ9Rl\":[],\"JINgGHgqzSN\":[],\"ou\":[\"ImspTQPwCqd\"],\"flGbXLXCrEo\":[],\"Z1rLc1rVHK8\":[],\"JuTpJ2Ywq5b\":[],\"CklPZdOd6H1\":[\"AZK4rjJCss5\",\"UrUdMteQzlT\"],\"h5FuguPFF2j\":[],\"spkM2E9dn2J\":[\"yyeQNBfmO7g\",\"CNnT7FC710W\",\"wuS7cVSEiYA\",\"ALoq1vKJCDr\",\"OvCy05DV6kt\",\"aslBaQPVe9V\",\"rpzJ5jGkUAn\",\"LMzYJEbDEN6\",\"mQ5FOz8JXKs\",\"TRETd1l7n1N\",\"xvYNdt7dLiM\",\"CzdkfAxkAqe\",\"LoTxSO186BO\",\"omWzNDmT2t7\",\"zTtjy8I0bcu\",\"FXk1MDI7CEJ\",\"GGWWOucyQ5L\",\"cc1JEMv0suu\",\"t0fFxYw3Cg4\",\"uM1WgdIueNA\",\"dUEKKaPFcVU\",\"zsS0Xx2iUV6\",\"MdSOAa4C4gW\",\"fEV7BkjJi8V\",\"zITeQ1j7Jmz\",\"RNUEujTD4AN\",\"WIleZf4Cua4\",\"vuiZtzbuwWx\",\"Yf0Gb9nZiQ1\",\"ga22tvzYHEZ\",\"k9qiH4Z3K6m\",\"ZqOOqkOV8Zm\",\"JUY113J4COL\",\"AlKNJVD0Bqv\",\"cGLoDkT864j\",\"OIXRi2caf6J\",\"CIYwznedTto\",\"phN4setkIfq\",\"l8WR0m3GGuB\",\"JqPKssESKSC\",\"zonuQ6g4FFh\",\"pQhtDfYHXlQ\",\"bANs6w1wFgV\",\"rBktLnj3vUY\",\"bQX37dUQTAr\",\"CQR9IijKrgo\",\"m9vCfHK0sLC\",\"Rb9W87URnVe\",\"e4CswJZAFBR\",\"LRjLr9oMe1M\",\"Cy4TaW1hskg\",\"VnmVMbf4mwL\",\"Rx05JdBEHIW\",\"WBvTizbhaXP\",\"iwk6djvOBNV\",\"tLn4hW3TbNZ\",\"DEO4vDvhNEv\",\"C5DXQidiMMc\",\"ccs1kikyZS3\",\"Scdk35fgY12\",\"t0mq3u8SNgz\",\"PMSl473rekw\",\"MpwuGzXBpAk\",\"CedH1TzSPgO\",\"mJexWvdoaXE\",\"RkxuXQTQjxk\",\"CDBeT1lT7n2\",\"KNU7Tm8S245\",\"v16FQ3xwnGN\",\"aOX23O03bBw\",\"W4KroB1nw6P\",\"CWhuePZuC9y\",\"upfjuKBGHq9\",\"wVl5DoJmza2\",\"fQzvkEY9chs\",\"pBNkmU3hDoT\",\"otDBqUSWuzE\",\"THPEMRSnC4G\",\"MKmqLvOYWos\",\"hF9y363enrH\",\"EQULu0IwQNE\",\"I9v1TBhT3OV\",\"CxKFBwhGuJr\",\"N9bYrawJaqR\",\"riIXFPTUnZX\",\"QyJHXS44Xj9\",\"dMRNgoCtogj\",\"gDARdk8cZ3H\",\"AneyNa28ceQ\",\"ELm3SnuBHJZ\",\"Oh3CJhGeaoi\",\"l69MO3y6LuS\",\"T0gujEdp3Z6\",\"I8A7Q4zi1YI\",\"TfyHeFLDOKu\",\"ZyGPejjzvGD\",\"OC0K30ETDLD\",\"FmIGl5AnbxN\",\"ALX1BnV0GrW\",\"S3Dt4ozhM8X\",\"eGQJGkiLamm\",\"vzzPNV6Wu0J\",\"AObPqV4cHPb\",\"kPQes5oG21J\",\"bEj6P1jqHje\",\"UXyOlL9FJ5o\",\"tiwJrxfBoHT\",\"ecANXpkkcPT\",\"VQkdjFxCLNH\",\"QIjTIxTedos\",\"etZCdyFxz4w\",\"H65niFKFuSs\",\"JwslMKjECF2\",\"IqyWsh1pbYf\",\"JWIEjkUmsWH\",\"UJppzPKIQRv\",\"BFMEIXmaqFE\",\"i0Dl3gB8WuY\",\"G9rNnfnVNcB\",\"mCnaSMEODSz\",\"LjRX17TMcTX\",\"SN9NeGsvfmM\",\"CkE4sCvC7zj\",\"THKtWeVTuBk\",\"PFq4nWHt0fM\",\"LfjyJpiu8dL\",\"p0vsNlHuo7N\",\"XQZko5dUFGU\",\"SXD2EhrNaQu\",\"M2XM0PR40oH\",\"IyBPcxO7hfB\",\"cxvpqSjkTjP\",\"kaf9448wuv0\",\"ApCSe2JdIUw\",\"KtR12m8FoT0\",\"Qtp6HW63yqV\",\"nOMNxq2fHGq\",\"C2Ws5NctBqi\",\"BpqJwhPqI9O\",\"V1nDCD6QvPs\",\"HScqQPe1X9u\",\"RmjKEjs388f\",\"jQ3mYwytyZn\",\"sK6lzdZiwIg\",\"nDTKZYmGEvT\",\"EABP62Ce29b\",\"QT9Erxe7UaX\",\"UmW7wmw0AX9\",\"lCww3l79Wem\",\"q36uKrRjq1P\",\"MBVq67Tm1wK\",\"OvzdfV1qrvP\",\"ElBqHdoLnsc\",\"e4HCJJlYOQP\",\"rbvEOlaNkUU\",\"LRsQdDERp1f\",\"ztgG5fQPur9\",\"fg3tUp5fFH1\",\"lBZHFe8qxEL\",\"Y2ze1UBngud\",\"UzC7hScynz0\",\"xsNVICc3jPD\",\"ibFgvQscr1i\",\"jmZqHjwAKxJ\",\"KGoEICL7PmU\",\"JpY27WXUqOI\",\"yxbHuzqF6VS\",\"eHKdzDVgEuj\",\"xuUrPK5b7MP\",\"Publ7A7E6r3\",\"Lyu0GDGZLU3\",\"ovWqm6wr1dP\",\"UvnjescFIbU\",\"hUQDnRc6BKw\",\"nTQRDVcTHr0\",\"oMLQaRXOs1B\",\"mTmjaDWUfDG\",\"NlVuvr5WbKy\",\"ThLyMbT1IvD\",\"zooDgQrnVkm\",\"D9h2axpYFx9\",\"a5ayhZAtUe4\",\"xF48L1VlFrZ\",\"gSPpYw9P7YO\",\"lufRQXPgcJ8\",\"CCzBKhjSPFo\",\"uJH3wMNmMNO\",\"x4Ohep6Q85T\",\"rJTXccXhtTG\",\"hIGURTVsclf\",\"L1FmwJC0u3z\",\"w0S8ngASwmq\",\"CysWbM6JFgj\",\"AMFu6DFAqll\",\"p8Tgra7YJ7h\",\"sPFJiL1BLTy\",\"QtjohlzA8cl\",\"Oprr3by24Zt\",\"rXsByduwzcw\",\"loTOnrxGmCv\",\"Kjon60bpcC4\",\"kdX0FFam8Vz\",\"Jl2tnw6dutF\",\"sQNVDVNINvY\",\"JYfvnvIzM84\",\"OozIOFXlUQB\",\"vMAK8GtCXzE\",\"bhfjgX8aEJQ\",\"SXeG3KaIkU8\",\"qpv7jbqwNTN\",\"ELZXJ7i1DKL\",\"IfwaYvRaIhp\",\"kNow7wcQT6z\",\"HafX2zWjutb\",\"ban72xOWClE\",\"ZcaE9JG9xrr\",\"LuMJaVKadYM\",\"jLIOTZIi0Ou\",\"yxvBpzHn9VN\",\"hB9kKEo5Jav\",\"lGIM5L1ldVZ\",\"txfMqMjfmGK\",\"HOiHQKzyA2h\",\"roEWVrnX17w\",\"vjJWFh1j9U5\",\"zJCV30f9Pix\",\"uNt8kKM8azp\",\"fmGjlRnf0AW\",\"vPk3xrZvimA\",\"xIvocxUNJvn\",\"aGozKfvhGv6\",\"NV1MNzAfPWE\",\"E68TvHpnyp5\",\"qCmj8zWn2RQ\",\"ZOGqtsOOfdP\",\"JwhF38ZDa7Y\",\"RvhGvhkGceD\",\"CzJoxdewhsm\",\"WrnwBUl0Vzt\",\"yqQ0IiG9maE\",\"C3Mf3a5OJa3\",\"CndbUcexjyJ\",\"VEulszgYUL2\",\"sgvVI1jilCg\",\"oZPItrH57Zf\",\"hbiFi85xO2g\",\"lJBsaRbZLZP\",\"WPR1XicphAd\",\"r15gweUYYrk\"],\"pe\":[\"2018\",\"2019\",\"2020\",\"2021\",\"2022\"],\"fazCI2ygYkq\":[\"SepVHxunjMN\",\"fa1IdKtq4VX\",\"xod2M9f6Jgo\"],\"rzhHSqK3lQq\":[],\"bJeK4FaRKDS\":[],\"vTKipVM0GsX\":[],\"aW66s2QSosT\":[],\"TfdH5KvFmMy\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        1,
        "B6TnnFMgmCk",
        "Age (years)",
        "INTEGER_ZERO_OR_POSITIVE",
        "java.lang.Integer",
        false,
        true);
    validateHeader(
        response,
        2,
        "qDkgAbB5Jlk.programstatus",
        "Program Status, Malaria case diagnosis, treatment and investigation",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        3,
        "qDkgAbB5Jlk.enrollmentdate",
        "Enrollment date, Malaria case diagnosis, treatment and investigation",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(
        response,
        4,
        "qDkgAbB5Jlk.ouname",
        "Organisation Unit Name, Malaria case diagnosis, treatment and investigation",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        5,
        "qDkgAbB5Jlk.C0aLZo75dgJ.JuTpJ2Ywq5b",
        "Age of LLINs, Malaria case diagnosis, treatment and investigation, Household investigation",
        "INTEGER_ZERO_OR_POSITIVE",
        "java.lang.Integer",
        false,
        true);
    validateHeader(
        response,
        6,
        "qDkgAbB5Jlk.hYyB7FUS5eR.fazCI2ygYkq",
        "Case detection, Malaria case diagnosis, treatment and investigation, Diagnosis & treatment",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        7,
        "qDkgAbB5Jlk.wYTF0YCHMWr.rzhHSqK3lQq",
        "Fever, Malaria case diagnosis, treatment and investigation, Case investigation & classification",
        "TRUE_ONLY",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        8,
        "qDkgAbB5Jlk.hYyB7FUS5eR.GyJHQUWZ9Rl",
        "GPS DataElement, Malaria case diagnosis, treatment and investigation, Diagnosis & treatment",
        "COORDINATE",
        "org.opengis.geometry.primitive.Point",
        false,
        true);
    validateHeader(
        response,
        9,
        "qDkgAbB5Jlk.hYyB7FUS5eR.JINgGHgqzSN",
        "Weight, Malaria case diagnosis, treatment and investigation, Diagnosis & treatment",
        "INTEGER_POSITIVE",
        "java.lang.Integer",
        false,
        true);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of(
            "Ngelehun CHC",
            "30",
            "ACTIVE",
            "2021-07-26 00:00:00.0",
            "Ngelehun CHC",
            "",
            "REACTIVE",
            "1",
            "",
            "50"));
    validateRow(
        response,
        1,
        List.of(
            "Ngelehun CHC",
            "0",
            "ACTIVE",
            "2021-10-02 00:00:00.0",
            "Ngelehun CHC",
            "",
            "PASSIVE",
            "1",
            "",
            "75"));
    validateRow(
        response,
        2,
        List.of(
            "Ngelehun CHC",
            "9",
            "COMPLETED",
            "2022-03-10 00:00:00.0",
            "Ngelehun CHC",
            "",
            "PASSIVE",
            "1",
            "",
            "62"));
  }
}
