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
public class TrackedEntityQuery3AutoTest extends AnalyticsApiTest {
  private AnalyticsTrackedEntityActions actions = new AnalyticsTrackedEntityActions();

  @Test
  public void sortByTeDateDesc() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add(
                "headers=ouname,gHGyrwKPzej,ciq2USN94oJ,cejWyOfXge6,IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6,created")
            .add("created=LAST_YEAR")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("pageSize=100")
            .add("page=1")
            .add(
                "dimension=ou:USER_ORGUNIT,gHGyrwKPzej,ciq2USN94oJ,cejWyOfXge6,IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6")
            .add("desc=created")
            .add("relativePeriodDate=2018-01-28");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(7)))
        .body("rows", hasSize(equalTo(100)))
        .body("height", equalTo(100))
        .body("width", equalTo(7))
        .body("headerWidth", equalTo(7));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":false},\"items\":{\"IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"code\":\"MMD_PER_STA\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"cejWyOfXge6\":{\"uid\":\"cejWyOfXge6\",\"name\":\"Gender\",\"description\":\"Gender\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"code\":\"MMD_PER_DOB\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"2017\":{\"uid\":\"2017\",\"code\":\"2017\",\"name\":\"2017\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2017-01-01T00:00:00.000\",\"endDate\":\"2017-12-31T00:00:00.000\"},\"IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"pe\":{\"uid\":\"pe\",\"name\":\"Period\",\"dimensionType\":\"PERIOD\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"LAST_YEAR\":{\"name\":\"Last year\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"bx6fsa0t90x\":[],\"Qo571yj6Zcn\":[],\"DODgdr5Oo2v\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"xs8A6tQJY0s\":[],\"A4xFHyieXys\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"GUOBQt5K2WI\":[],\"Agywv2JGwuq\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"a3kGcGDCuk6\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[\"2017\"],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
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
    validateRow(
        response,
        20,
        List.of("Babara CHC", "1976-12-16 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.332"));
    validateRow(
        response,
        21,
        List.of("Mamaka MCHP", "1984-03-14 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.33"));
    validateRow(
        response,
        22,
        List.of(
            "Siama (U. Bamabara) MCHP",
            "1989-03-25 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:48:13.329"));
    validateRow(
        response,
        23,
        List.of(
            "Kissy Koya MCHP", "1985-08-24 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.327"));
    validateRow(
        response,
        24,
        List.of(
            "Kongoifeh MCHP", "1987-08-24 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.324"));
    validateRow(
        response,
        25,
        List.of(
            "Benguema MI Room",
            "1970-11-16 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:48:13.322"));
    validateRow(
        response,
        26,
        List.of("Nyeama CHP", "1974-07-31 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.32"));
    validateRow(
        response,
        27,
        List.of("Tagrin CHC", "1980-05-01 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.318"));
    validateRow(
        response,
        28,
        List.of(
            "Magbeni MCHP", "1973-09-21 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.317"));
    validateRow(
        response,
        29,
        List.of(
            "Holy Mary Hospital",
            "1984-06-09 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:48:13.314"));
    validateRow(
        response,
        30,
        List.of(
            "Taninahun MCHP", "1980-07-01 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.313"));
    validateRow(
        response,
        31,
        List.of("Lungi UFC", "1981-10-03 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.311"));
    validateRow(
        response,
        32,
        List.of("Gambia CHP", "1976-03-03 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.309"));
    validateRow(
        response,
        33,
        List.of("Kambia CHP", "1984-02-07 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.307"));
    validateRow(
        response,
        34,
        List.of("Nonkoba CHP", "1983-02-23 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.305"));
    // The following two have the same dates, hence the sort is interchangeable.
    validateRow(
        response,
        List.of(
            "Sumbuya MCHP", "1981-06-12 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.302"));
    validateRow(
        response,
        List.of(
            "Mokpanabom MCHP", "1973-06-26 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.302"));
    validateRow(
        response,
        37,
        List.of("Njala CHP", "1973-07-13 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.3"));
    validateRow(
        response,
        38,
        List.of(
            "Gandorhun CHC", "1987-01-13 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.297"));
    validateRow(
        response,
        39,
        List.of(
            "St Monica's Clinic",
            "1988-12-08 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:48:13.296"));
    validateRow(
        response,
        40,
        List.of("Masoko MCHP", "1979-12-14 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.294"));
    validateRow(
        response,
        41,
        List.of(
            "Swarray Town MCHP",
            "1971-09-24 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:48:13.291"));
    validateRow(
        response,
        42,
        List.of("Nyeama CHP", "1980-04-24 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.289"));
    validateRow(
        response,
        43,
        List.of("Nekabo CHC", "1987-11-06 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.286"));
    validateRow(
        response,
        44,
        List.of(
            "Makalie MCHP", "1980-06-03 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.284"));
    validateRow(
        response,
        45,
        List.of("Gerehun CHC", "1984-02-04 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.281"));
    validateRow(
        response,
        46,
        List.of(
            "Murray Town CHC", "1978-05-08 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.279"));
    validateRow(
        response,
        47,
        List.of(
            "Philip Street Clinic",
            "1978-02-09 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:48:13.277"));
    validateRow(
        response,
        48,
        List.of(
            "Durukoro MCHP", "1990-04-27 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.274"));
    validateRow(
        response,
        49,
        List.of(
            "Malambay CHP", "1977-01-21 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.272"));
    validateRow(
        response,
        50,
        List.of(
            "Kumrabai Yoni MCHP",
            "1988-05-06 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:48:13.268"));
    validateRow(
        response,
        51,
        List.of(
            "Tonkomba MCHP", "1972-05-23 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.267"));
    validateRow(
        response,
        52,
        List.of("Mano CHC", "1985-02-21 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.265"));
    validateRow(
        response,
        53,
        List.of("Samaya CHP", "1974-01-28 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.264"));
    validateRow(
        response,
        54,
        List.of(
            "Kuranko MCHP", "1977-12-08 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.262"));
    validateRow(
        response,
        55,
        List.of("Barlie MCHP", "1984-10-09 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.261"));
    validateRow(
        response,
        56,
        List.of(
            "Taninihun Kapuima MCHP",
            "1978-10-29 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:48:13.258"));
    validateRow(
        response,
        57,
        List.of(
            "Gendema MCHP", "1989-02-13 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.257"));
    validateRow(
        response,
        58,
        List.of("Yoyema MCHP", "1975-03-17 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.254"));
    validateRow(
        response,
        59,
        List.of("Kamba MCHP", "1974-04-21 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.252"));
    validateRow(
        response,
        60,
        List.of(
            "Gbonkonka CHP", "1978-01-23 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.251"));
    validateRow(
        response,
        61,
        List.of(
            "Njagbahun (L.Banta) MCHP",
            "1989-05-24 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:48:13.248"));
    validateRow(
        response,
        62,
        List.of(
            "Masseseh MCHP", "1977-02-09 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.246"));
    validateRow(
        response,
        63,
        List.of(
            "Kaponkie MCHP", "1973-09-20 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.243"));
    validateRow(
        response,
        64,
        List.of(
            "Komende Luyaima MCHP",
            "1971-11-08 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:48:13.24"));
    validateRow(
        response,
        65,
        List.of(
            "Mokellay MCHP", "1985-03-01 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.238"));
    validateRow(
        response,
        66,
        List.of("Manewa MCHP", "1988-12-03 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.237"));
    validateRow(
        response,
        67,
        List.of("Konjo MCHP", "1987-01-16 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.235"));
    validateRow(
        response,
        68,
        List.of(
            "Mayolla MCHP", "1990-08-24 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.233"));
    validateRow(
        response,
        69,
        List.of(
            "Gondama MCHP", "1988-08-27 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.231"));
    validateRow(
        response,
        70,
        List.of(
            "SLRCS (Koinadugu) Clinic",
            "1981-10-28 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:48:13.229"));
    validateRow(
        response,
        71,
        List.of("Kambia GH", "1979-02-03 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.227"));
    validateRow(
        response,
        72,
        List.of("Katick MCHP", "1979-03-02 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.225"));
    validateRow(
        response,
        73,
        List.of(
            "Madina Gbonkobor MCHP",
            "1983-05-13 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:48:13.223"));
    validateRow(
        response,
        74,
        List.of("MCH Static", "1985-11-06 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.221"));
    validateRow(
        response,
        75,
        List.of(
            "New Police Barracks CHC",
            "1971-03-14 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:48:13.219"));
    validateRow(
        response,
        76,
        List.of("Tei CHP", "1978-10-02 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.217"));
    validateRow(
        response,
        77,
        List.of(
            "Petifu Mayepoh MCHP",
            "1974-02-23 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:48:13.215"));
    validateRow(
        response,
        78,
        List.of("Massam MCHP", "1978-10-22 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.213"));
    validateRow(
        response,
        79,
        List.of(
            "Masseseh MCHP", "1983-11-08 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.211"));
    validateRow(
        response,
        80,
        List.of(
            "Makonthanday MCHP",
            "1986-05-19 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:48:13.208"));
    validateRow(
        response,
        81,
        List.of("Deima MCHP", "1977-12-04 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.207"));
    validateRow(
        response,
        82,
        List.of(
            "Kpandebu MCHP", "1971-01-20 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.206"));
    validateRow(
        response,
        83,
        List.of(
            "Kroo Bay CHC", "1978-12-09 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.203"));
    validateRow(
        response,
        84,
        List.of(
            "Malenkie MCHP", "1980-09-22 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.201"));
    validateRow(
        response,
        85,
        List.of("Boroma MCHP", "1983-11-09 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.198"));
    validateRow(
        response,
        86,
        List.of("Foakor MCHP", "1986-02-13 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.196"));
    validateRow(
        response,
        87,
        List.of(
            "Blama Massaquoi CHP",
            "1986-11-03 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:48:13.194"));
    validateRow(
        response,
        88,
        List.of(
            "Sumbuya MCHP", "1985-10-25 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.192"));
    validateRow(
        response,
        89,
        List.of(
            "Kindoyal Hospital",
            "1973-11-06 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:48:13.191"));
    validateRow(
        response,
        90,
        List.of(
            "Madina (BUM) CHC",
            "1970-09-06 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:48:13.188"));
    validateRow(
        response,
        91,
        List.of(
            "Holy Mary Hospital",
            "1985-03-03 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:48:13.186"));
    validateRow(
        response,
        92,
        List.of(
            "Mosenessie Junction MCHP",
            "1985-12-19 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:48:13.184"));
    validateRow(
        response,
        93,
        List.of("Gbanti CHC", "1977-10-12 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.182"));
    validateRow(
        response,
        94,
        List.of("Juma MCHP", "1982-05-06 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.181"));
    validateRow(
        response,
        95,
        List.of(
            "Massah Memorial Maternity MCHP",
            "1985-02-01 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:48:13.179"));
    validateRow(
        response,
        96,
        List.of(
            "Marie Stopes (Kakua) Clinic",
            "1976-09-30 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:48:13.176"));
    validateRow(
        response,
        97,
        List.of(
            "Rosengbeh MCHP", "1975-09-27 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.174"));
    validateRow(
        response,
        98,
        List.of("Rogbere CHC", "1988-11-30 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.171"));
    validateRow(
        response,
        99,
        List.of(
            "Sendumei CHC", "1987-06-16 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.166"));
  }

  @Test
  public void sortByTeDateAsc() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add(
                "headers=ouname,gHGyrwKPzej,ciq2USN94oJ,cejWyOfXge6,IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6,created")
            .add("created=LAST_YEAR")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("pageSize=100")
            .add("page=1")
            .add(
                "dimension=ou:USER_ORGUNIT,gHGyrwKPzej,ciq2USN94oJ,cejWyOfXge6,IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6")
            .add("desc=created")
            .add("relativePeriodDate=2018-01-28");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(7)))
        .body("rows", hasSize(equalTo(100)))
        .body("height", equalTo(100))
        .body("width", equalTo(7))
        .body("headerWidth", equalTo(7));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":false},\"items\":{\"zDhUuAYrxNC\":{\"uid\":\"zDhUuAYrxNC\",\"name\":\"Last name\",\"description\":\"Last name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"lw1SqmMlnfh\":{\"uid\":\"lw1SqmMlnfh\",\"code\":\"Height in cm\",\"name\":\"Height in cm\",\"description\":\"Height in cm\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"fDd25txQckK\":{\"uid\":\"fDd25txQckK\",\"name\":\"Provider Follow-up and Support Tool\"},\"DODgdr5Oo2v\":{\"uid\":\"DODgdr5Oo2v\",\"code\":\"Provider ID\",\"name\":\"Provider ID\",\"description\":\"Provider ID\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"Qo571yj6Zcn\":{\"uid\":\"Qo571yj6Zcn\",\"code\":\"Latitude\",\"name\":\"Latitude\",\"description\":\"Latitude\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"iESIqZ0R0R0\":{\"uid\":\"iESIqZ0R0R0\",\"name\":\"Date of birth\",\"description\":\"Date of birth\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"n9nUvfpTsxQ\":{\"uid\":\"n9nUvfpTsxQ\",\"code\":\"Zip code\",\"name\":\"Zip code\",\"description\":\"Zip code\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"kyIzQsj96BD\":{\"uid\":\"kyIzQsj96BD\",\"code\":\"Company\",\"name\":\"Company\",\"description\":\"Company\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"A4xFHyieXys\":{\"uid\":\"A4xFHyieXys\",\"code\":\"Occupation\",\"name\":\"Occupation\",\"description\":\"Occupation\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"xs8A6tQJY0s\":{\"uid\":\"xs8A6tQJY0s\",\"name\":\"TB identifier\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"OvY4VVhSDeJ\":{\"uid\":\"OvY4VVhSDeJ\",\"name\":\"Weight in kg\",\"description\":\"Weight in kg\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"ur1Edk5Oe2n\":{\"uid\":\"ur1Edk5Oe2n\",\"name\":\"TB program\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"RG7uGl4w5Jq\":{\"uid\":\"RG7uGl4w5Jq\",\"code\":\"Longitude\",\"name\":\"Longitude\",\"description\":\"Longitude\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"LAST_YEAR\":{\"name\":\"Last year\"},\"spFvx9FndA4\":{\"uid\":\"spFvx9FndA4\",\"name\":\"Age\",\"description\":\"Age\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"Agywv2JGwuq\":{\"uid\":\"Agywv2JGwuq\",\"code\":\"MMD_PER_MOB\",\"name\":\"Mobile number\",\"description\":\"Mobile number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"GUOBQt5K2WI\":{\"uid\":\"GUOBQt5K2WI\",\"code\":\"State\",\"name\":\"State\",\"description\":\"State\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"lZGmxYbs97q\":{\"uid\":\"lZGmxYbs97q\",\"code\":\"MMD_PER_ID\",\"name\":\"Unique ID\",\"description\":\"Unique identiifer\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"VqEFza8wbwA\":{\"uid\":\"VqEFza8wbwA\",\"code\":\"MMD_PER_ADR1\",\"name\":\"Address\",\"description\":\"Country\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"code\":\"MMD_PER_STA\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"w75KJ2mc4zz\":{\"uid\":\"w75KJ2mc4zz\",\"code\":\"MMD_PER_NAM\",\"name\":\"First name\",\"description\":\"First name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"KmEUg2hHEtx\":{\"uid\":\"KmEUg2hHEtx\",\"name\":\"Email address\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"G7vUx908SwP\":{\"uid\":\"G7vUx908SwP\",\"name\":\"Residence location\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"COORDINATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"o9odfev2Ty5\":{\"uid\":\"o9odfev2Ty5\",\"code\":\"Mother maiden name\",\"name\":\"Mother maiden name\",\"description\":\"Mother maiden name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"FO4sWYJ64LQ\":{\"uid\":\"FO4sWYJ64LQ\",\"code\":\"City\",\"name\":\"City\",\"description\":\"City\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"NDXw0cluzSw\":{\"uid\":\"NDXw0cluzSw\",\"name\":\"Email\",\"description\":\"Email address\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ruQQnf6rswq\":{\"uid\":\"ruQQnf6rswq\",\"name\":\"TB number\",\"description\":\"TB number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"cejWyOfXge6\":{\"uid\":\"cejWyOfXge6\",\"name\":\"Gender\",\"description\":\"Gender\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"P2cwLGskgxn\":{\"uid\":\"P2cwLGskgxn\",\"name\":\"Phone number\",\"description\":\"Phone number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"code\":\"MMD_PER_DOB\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"2017\":{\"uid\":\"2017\",\"code\":\"2017\",\"name\":\"2017\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2017-01-01T00:00:00.000\",\"endDate\":\"2017-12-31T00:00:00.000\"},\"pe\":{\"uid\":\"pe\",\"name\":\"Period\",\"dimensionType\":\"PERIOD\"},\"uy2gU8kT1jF\":{\"uid\":\"uy2gU8kT1jF\",\"name\":\"MNCH / PNC (Adult Woman)\"},\"VHfUeXpawmE\":{\"uid\":\"VHfUeXpawmE\",\"code\":\"Vehicle\",\"name\":\"Vehicle\",\"description\":\"Vehicle\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"AuPLng5hLbE\":{\"uid\":\"AuPLng5hLbE\",\"code\":\"National identifier\",\"name\":\"National identifier\",\"description\":\"National identifier\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ZcBPrXKahq2\":{\"uid\":\"ZcBPrXKahq2\",\"name\":\"Postal code\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"WSGAb5XwJ3Y\":{\"uid\":\"WSGAb5XwJ3Y\",\"name\":\"WHO RMNCH Tracker\"},\"H9IlTX2X6SL\":{\"uid\":\"H9IlTX2X6SL\",\"code\":\"Blood type\",\"name\":\"Blood type\",\"description\":\"Blood type\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"bx6fsa0t90x\":[],\"DODgdr5Oo2v\":[],\"Qo571yj6Zcn\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"A4xFHyieXys\":[],\"xs8A6tQJY0s\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"Agywv2JGwuq\":[],\"GUOBQt5K2WI\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"a3kGcGDCuk6\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[\"2017\"],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
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

    // Assert only 20 rows, as samples.
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
    validateRow(
        response,
        20,
        List.of("Babara CHC", "1976-12-16 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.332"));
  }

  @Test
  public void sortByTeAttributeAsc() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=gHGyrwKPzej")
            .add(
                "headers=ouname,gHGyrwKPzej,ciq2USN94oJ,cejWyOfXge6,IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("pageSize=8")
            .add("page=1")
            .add(
                "dimension=ou:USER_ORGUNIT,gHGyrwKPzej,ciq2USN94oJ,cejWyOfXge6,IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6")
            .add("relativePeriodDate=2017-01-27");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

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
        "{\"pager\":{\"page\":1,\"pageSize\":8,\"isLastPage\":false},\"items\":{\"IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"xjA5E9MimMU\":{\"uid\":\"xjA5E9MimMU\",\"name\":\"Civil status\",\"options\":[{\"uid\":\"wfkKVdPBzho\",\"code\":\"Single or widow\"}]},\"bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"code\":\"MMD_PER_STA\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"cejWyOfXge6\":{\"uid\":\"cejWyOfXge6\",\"name\":\"Gender\",\"description\":\"Gender\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"code\":\"MMD_PER_DOB\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"wfkKVdPBzho\":{\"uid\":\"wfkKVdPBzho\",\"code\":\"Single or widow\",\"name\":\"Single or widow\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"bx6fsa0t90x\":[],\"Qo571yj6Zcn\":[],\"DODgdr5Oo2v\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"xs8A6tQJY0s\":[],\"A4xFHyieXys\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"GUOBQt5K2WI\":[],\"Agywv2JGwuq\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[\"wfkKVdPBzho\"],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"a3kGcGDCuk6\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
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

    // Assert rows.
    validateRow(
        response,
        0,
        List.of("Ngelehun CHC", "1956-10-17 00:00:00.0", "Single or widow", "", "", ""));
    validateRow(response, 1, List.of("Benduma MCHP", "1970-01-04 00:00:00.0", "", "", "", ""));
    validateRow(response, 2, List.of("Mosagbe MCHP", "1970-01-05 00:00:00.0", "", "", "", ""));
    validateRow(response, 3, List.of("Korgbotuma MCHP", "1970-01-07 00:00:00.0", "", "", "", ""));
    validateRow(response, 4, List.of("Fogbo (WAR) MCHP", "1970-01-14 00:00:00.0", "", "", "", ""));
    validateRow(response, 5, List.of("MadaKa MCHP", "1970-01-21 00:00:00.0", "", "", "", ""));
    validateRow(response, 6, List.of("Kuntorloh CHP", "1970-01-24 00:00:00.0", "", "", "", ""));
    validateRow(response, 7, List.of("Kagbulor CHP", "1970-01-26 00:00:00.0", "", "", "", ""));
  }

  @Test
  public void sortByTeStageElementAsc() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("headers=ouname,gHGyrwKPzej,ciq2USN94oJ,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("pageSize=10")
            .add("page=1")
            .add(
                "dimension=ou:USER_ORGUNIT,gHGyrwKPzej,ciq2USN94oJ,IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6")
            .add("desc=IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6")
            .add("relativePeriodDate=2017-01-27");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(4)))
        .body("rows", hasSize(equalTo(10)))
        .body("height", equalTo(10))
        .body("width", equalTo(4))
        .body("headerWidth", equalTo(4));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":10,\"isLastPage\":false},\"items\":{\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"code\":\"MMD_PER_DOB\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"code\":\"MMD_PER_STA\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"bx6fsa0t90x\":[],\"Qo571yj6Zcn\":[],\"DODgdr5Oo2v\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"xs8A6tQJY0s\":[],\"A4xFHyieXys\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"GUOBQt5K2WI\":[],\"Agywv2JGwuq\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"a3kGcGDCuk6\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "gHGyrwKPzej", "Birth date", "DATE", "java.time.LocalDate", false, true);
    validateHeader(
        response, 2, "ciq2USN94oJ", "Civil status", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        3,
        "IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6",
        "MCH Apgar Score, Child Programme, Birth",
        "NUMBER",
        "java.lang.Double",
        false,
        true);

    // Assert rows.
    validateRow(response, 0, List.of("Ngelehun CHC", "", "", "11.0"));
    validateRow(response, 1, List.of("Ngelehun CHC", "", "", "11.0"));
    validateRow(response, 2, List.of("Ngelehun CHC", "", "", "10.0"));
    validateRow(response, 3, List.of("Ngelehun CHC", "", "", "10.0"));
    validateRow(response, 4, List.of("Ngelehun CHC", "", "", "8.0"));
    validateRow(response, 5, List.of("Ngelehun CHC", "", "", "8.0"));
    validateRow(response, 6, List.of("Ngelehun CHC", "", "", "8.0"));
    validateRow(response, 7, List.of("Ngelehun CHC", "", "", "8.0"));
    validateRow(response, 8, List.of("Ngelehun CHC", "", "", "8.0"));
    validateRow(response, 9, List.of("Ngelehun CHC", "", "", "8.0"));
  }
}
