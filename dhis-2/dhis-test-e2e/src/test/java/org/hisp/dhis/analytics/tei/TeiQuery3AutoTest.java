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
public class TeiQuery3AutoTest extends AnalyticsApiTest {
  private AnalyticsTeiActions actions = new AnalyticsTeiActions();

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
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":false},\"items\":{\"IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"code\":\"MMD_PER_STA\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"cejWyOfXge6\":{\"uid\":\"cejWyOfXge6\",\"name\":\"Gender\",\"description\":\"Gender\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"code\":\"MMD_PER_DOB\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"2017\":{\"uid\":\"2017\",\"code\":\"2017\",\"name\":\"2017\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2017-01-01T00:00:00.000\",\"endDate\":\"2017-12-31T00:00:00.000\"},\"IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"pe\":{\"uid\":\"pe\",\"name\":\"Period\",\"dimensionType\":\"PERIOD\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"LAST_YEAR\":{\"name\":\"Last year\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"bx6fsa0t90x\":[],\"Qo571yj6Zcn\":[],\"DODgdr5Oo2v\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"xs8A6tQJY0s\":[],\"A4xFHyieXys\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"GUOBQt5K2WI\":[],\"Agywv2JGwuq\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"a3kGcGDCuk6\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[\"2017\"],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
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
    validateRow(
        response,
        35,
        List.of(
            "Sumbuya MCHP", "1981-06-12 00:00:00.0", "", "", "", "", "2017-01-26 13:48:13.302"));
    validateRow(
        response,
        36,
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
            .add("asc=created")
            .add(
                "headers=ouname,gHGyrwKPzej,ciq2USN94oJ,cejWyOfXge6,IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6,created")
            .add("created=YESTERDAY")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("pageSize=100")
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
        .body("headers", hasSize(equalTo(7)))
        .body("rows", hasSize(equalTo(100)))
        .body("height", equalTo(100))
        .body("width", equalTo(7))
        .body("headerWidth", equalTo(7));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":false},\"items\":{\"IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"code\":\"MMD_PER_STA\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"YESTERDAY\":{\"name\":\"Yesterday\"},\"20170126\":{\"uid\":\"20170126\",\"code\":\"20170126\",\"name\":\"2017-01-26\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2017-01-26T00:00:00.000\",\"endDate\":\"2017-01-26T00:00:00.000\"},\"a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"cejWyOfXge6\":{\"uid\":\"cejWyOfXge6\",\"name\":\"Gender\",\"description\":\"Gender\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"code\":\"MMD_PER_DOB\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"pe\":{\"uid\":\"pe\",\"name\":\"Period\",\"dimensionType\":\"PERIOD\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"bx6fsa0t90x\":[],\"Qo571yj6Zcn\":[],\"DODgdr5Oo2v\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"xs8A6tQJY0s\":[],\"A4xFHyieXys\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"GUOBQt5K2WI\":[],\"Agywv2JGwuq\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"a3kGcGDCuk6\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[\"20170126\"],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
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
        List.of(
            "Kaliyereh MCHP", "1979-05-20 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.374"));
    validateRow(
        response,
        1,
        List.of(
            "Mapamurie MCHP", "1978-05-19 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.5"));
    validateRow(
        response,
        2,
        List.of("Bayama MCHP", "1974-08-09 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.513"));
    validateRow(
        response,
        3,
        List.of(
            "Semewebu MCHP", "1972-03-24 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.541"));
    validateRow(
        response,
        4,
        List.of(
            "Konta-Line MCHP", "1977-08-07 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.555"));
    validateRow(
        response,
        5,
        List.of(
            "Kundorma CHP", "1989-04-08 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.569"));
    validateRow(
        response,
        6,
        List.of(
            "Mayogbor MCHP", "1984-06-26 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.583"));
    validateRow(
        response,
        7,
        List.of(
            "Masofinia MCHP", "1978-12-27 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.592"));
    validateRow(
        response,
        8,
        List.of("Mabora MCHP", "1982-04-25 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.601"));
    validateRow(
        response,
        9,
        List.of(
            "Mayogbor MCHP", "1974-10-23 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.61"));
    validateRow(
        response,
        10,
        List.of(
            "Mayossoh MCHP", "1975-03-31 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.616"));
    validateRow(
        response,
        11,
        List.of(
            "Yankasa MCHP", "1972-03-22 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.619"));
    validateRow(
        response,
        12,
        List.of("Sawuria CHP", "1975-02-24 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.625"));
    validateRow(
        response,
        13,
        List.of(
            "Baoma (Koya) CHC",
            "1985-08-22 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:29.632"));
    validateRow(
        response,
        14,
        List.of(
            "SLRCS (Nongowa) clinic",
            "1987-05-07 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:29.639"));
    validateRow(
        response,
        15,
        List.of(
            "Mabai (Kholifa Rowalla) MCHP",
            "1974-04-02 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:29.644"));
    validateRow(
        response,
        16,
        List.of("Foakor MCHP", "1986-08-22 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.657"));
    validateRow(
        response,
        17,
        List.of(
            "Masimera CHC", "1979-11-24 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.667"));
    validateRow(
        response,
        18,
        List.of("Worreh MCHP", "1988-06-13 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.674"));
    validateRow(
        response,
        19,
        List.of(
            "Konta-Line MCHP", "1979-02-16 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.68"));
    validateRow(
        response,
        20,
        List.of("Dibia MCHP", "1973-12-26 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.687"));
    validateRow(
        response,
        21,
        List.of(
            "Taninahun MCHP", "1975-08-14 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.693"));
    validateRow(
        response,
        22,
        List.of(
            "Ngelehun MCHP", "1972-07-26 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.703"));
    validateRow(
        response,
        23,
        List.of("Sendugu CHC", "1990-06-18 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.707"));
    validateRow(
        response,
        24,
        List.of(
            "Kondiama MCHP", "1988-06-22 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.713"));
    validateRow(
        response,
        25,
        List.of("Gbentu CHP", "1990-11-08 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.719"));
    validateRow(
        response,
        26,
        List.of(
            "Potehun MCHP", "1971-10-04 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.722"));
    validateRow(
        response,
        27,
        List.of(
            "Koakoyima CHC", "1977-05-27 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.727"));
    validateRow(
        response,
        28,
        List.of(
            "Rogbangba MCHP", "1978-01-15 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.733"));
    validateRow(
        response,
        29,
        List.of(
            "Bendu Mameima CHC",
            "1987-07-29 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:29.737"));
    validateRow(
        response,
        30,
        List.of("Quarry MCHP", "1978-11-19 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.746"));
    validateRow(
        response,
        31,
        List.of(
            "Rosengbeh MCHP", "1975-04-29 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.754"));
    validateRow(
        response,
        32,
        List.of(
            "Bandajuma Sinneh MCHP",
            "1983-11-02 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:29.758"));
    validateRow(
        response,
        33,
        List.of(
            "Sahn Bumpe MCHP", "1988-02-11 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.762"));
    validateRow(
        response,
        34,
        List.of(
            "Bandasuma CHP", "1982-06-14 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.768"));
    validateRow(
        response,
        35,
        List.of("Manna MCHP", "1974-04-13 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.775"));
    validateRow(
        response,
        36,
        List.of(
            "Hill Station MCHP",
            "1986-11-24 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:29.781"));
    validateRow(
        response,
        37,
        List.of("Kensay MCHP", "1989-10-02 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.785"));
    validateRow(
        response,
        38,
        List.of("Mabom CHP", "1972-05-23 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.791"));
    validateRow(
        response,
        39,
        List.of(
            "Mamalikie MCHP", "1982-09-02 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.797"));
    validateRow(
        response,
        40,
        List.of("Masory MCHP", "1979-11-12 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.802"));
    validateRow(
        response,
        41,
        List.of(
            "Hill Station MCHP",
            "1983-02-11 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:29.807"));
    validateRow(
        response,
        42,
        List.of("Lowoma MCHP", "1978-12-31 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.81"));
    validateRow(
        response,
        43,
        List.of(
            "UFC Magburaka", "1984-07-12 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.816"));
    validateRow(
        response,
        44,
        List.of(
            "Fintonia CHC", "1971-12-09 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.823"));
    validateRow(
        response,
        45,
        List.of(
            "Kaimunday CHP", "1981-12-10 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.828"));
    validateRow(
        response,
        46,
        List.of(
            "Fulamansa MCHP", "1975-10-19 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.833"));
    validateRow(
        response,
        47,
        List.of("Samaia MCHP", "1971-03-16 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.835"));
    validateRow(
        response,
        48,
        List.of(
            "Magbolonthor MCHP",
            "1984-09-27 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:29.839"));
    validateRow(
        response,
        49,
        List.of(
            "Gondama MCHP", "1977-03-27 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.843"));
    validateRow(
        response,
        50,
        List.of(
            "Kamiendor MCHP", "1972-06-19 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.847"));
    validateRow(
        response,
        51,
        List.of("Kaniya MCHP", "1979-05-12 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.851"));
    validateRow(
        response,
        52,
        List.of(
            "Kpandebu CHP", "1977-02-20 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.855"));
    validateRow(
        response,
        53,
        List.of(
            "Tambiama CHC", "1987-06-05 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.859"));
    validateRow(
        response,
        54,
        List.of(
            "Levuma (Kandu Lep) CHC",
            "1983-08-28 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:29.867"));
    validateRow(
        response,
        55,
        List.of("Rokonta CHC", "1971-12-24 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.873"));
    validateRow(
        response,
        56,
        List.of(
            "Kangama (Kangama) CHP",
            "1971-08-03 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:29.875"));
    validateRow(
        response,
        57,
        List.of(
            "Sellah Kafta MCHP",
            "1971-09-03 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:29.881"));
    validateRow(
        response,
        58,
        List.of(
            "Heremakono MCHP", "1987-11-02 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.885"));
    validateRow(
        response,
        59,
        List.of("Tobanda CHC", "1990-04-15 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.889"));
    validateRow(
        response,
        60,
        List.of("Yabaima CHP", "1970-02-25 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.894"));
    validateRow(
        response,
        61,
        List.of(
            "Kasongha MCHP", "1977-02-13 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.902"));
    validateRow(
        response,
        62,
        List.of(
            "Benguema MI Room",
            "1989-07-02 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:29.907"));
    validateRow(
        response,
        63,
        List.of(
            "Govt. Hosp. Makeni",
            "1985-04-11 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:29.91"));
    validateRow(
        response,
        64,
        List.of("Rokulan CHC", "1989-10-27 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.914"));
    validateRow(
        response,
        65,
        List.of(
            "Blamawo MCHP", "1979-12-28 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.919"));
    validateRow(
        response,
        66,
        List.of(
            "Mano Menima CHP", "1980-08-14 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.923"));
    validateRow(
        response,
        67,
        List.of("UNIMUS MCHP", "1986-02-06 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.928"));
    validateRow(
        response,
        68,
        List.of(
            "Koidu Under Five Clinic",
            "1974-05-16 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:29.932"));
    validateRow(
        response,
        69,
        List.of(
            "Leicester (RWA) CHP",
            "1978-02-28 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:29.937"));
    validateRow(
        response,
        70,
        List.of(
            "Wilberforce CHC", "1985-07-07 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.942"));
    validateRow(
        response,
        71,
        List.of("Vaahun MCHP", "1985-06-08 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.948"));
    validateRow(
        response,
        72,
        List.of("Gbamani CHP", "1980-12-08 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.954"));
    validateRow(
        response,
        73,
        List.of(
            "Kanekor MCHP", "1988-12-24 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.958"));
    validateRow(
        response,
        74,
        List.of(
            "Kpowubu MCHP", "1978-06-10 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.962"));
    validateRow(
        response,
        75,
        List.of("Mbaoma CHP", "1971-10-28 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.966"));
    validateRow(
        response,
        76,
        List.of(
            "Nomo Faama CHP", "1990-08-16 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.971"));
    validateRow(
        response,
        77,
        List.of("Rotawa CHP", "1984-03-01 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.978"));
    validateRow(
        response,
        78,
        List.of("Bumbeh MCHP", "1979-02-15 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.982"));
    validateRow(
        response,
        79,
        List.of(
            "Baoma (Luawa) MCHP",
            "1985-06-23 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:29.99"));
    validateRow(
        response,
        80,
        List.of(
            "Maharie MCHP", "1979-03-17 00:00:00.0", "", "", "", "", "2017-01-26 13:43:29.995"));
    validateRow(
        response,
        81,
        List.of("Bafodia CHC", "1979-05-31 00:00:00.0", "", "", "", "", "2017-01-26 13:43:30.003"));
    validateRow(
        response,
        82,
        List.of(
            "Bundulai MCHP", "1971-01-29 00:00:00.0", "", "", "", "", "2017-01-26 13:43:30.009"));
    validateRow(
        response,
        83,
        List.of(
            "Gbahama (P. Bongre) CHP",
            "1985-11-26 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:30.014"));
    validateRow(
        response,
        84,
        List.of(
            "Manjama Shellmingo CHC",
            "1978-09-30 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:30.021"));
    validateRow(
        response,
        85,
        List.of(
            "Rokupa Govt. Hospital",
            "1978-06-02 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:30.028"));
    validateRow(
        response,
        86,
        List.of("Alkalia CHP", "1984-01-06 00:00:00.0", "", "", "", "", "2017-01-26 13:43:30.031"));
    validateRow(
        response,
        87,
        List.of("Laiya CHP", "1980-03-11 00:00:00.0", "", "", "", "", "2017-01-26 13:43:30.037"));
    validateRow(
        response,
        88,
        List.of("Yambama MCHP", "1985-02-04 00:00:00.0", "", "", "", "", "2017-01-26 13:43:30.04"));
    validateRow(
        response,
        89,
        List.of(
            "Bendu (Kowa) MCHP",
            "1970-12-11 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:30.047"));
    validateRow(
        response,
        90,
        List.of(
            "Mosenegor MCHP", "1973-07-27 00:00:00.0", "", "", "", "", "2017-01-26 13:43:30.052"));
    validateRow(
        response,
        91,
        List.of(
            "Kambia Makama CHP",
            "1984-11-11 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:30.056"));
    validateRow(
        response,
        92,
        List.of(
            "Sumbuya Bessima CHP",
            "1987-01-26 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:30.06"));
    validateRow(
        response,
        93,
        List.of(
            "SLC. RHC Port Loko",
            "1971-05-28 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:30.065"));
    validateRow(
        response,
        94,
        List.of(
            "Kagbulor CHP", "1984-01-23 00:00:00.0", "", "", "", "", "2017-01-26 13:43:30.071"));
    validateRow(
        response,
        95,
        List.of(
            "Mangay Loko MCHP",
            "1980-02-21 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:30.074"));
    validateRow(
        response,
        96,
        List.of(
            "Bandasuma Fiama MCHP",
            "1984-08-02 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:30.079"));
    validateRow(
        response,
        97,
        List.of(
            "Mangay Loko MCHP",
            "1975-09-09 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:30.084"));
    validateRow(
        response,
        98,
        List.of(
            "Barmoi Luma MCHP",
            "1988-06-20 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:30.089"));
    validateRow(
        response,
        99,
        List.of(
            "Mayakie MCHP", "1985-08-31 00:00:00.0", "", "", "", "", "2017-01-26 13:43:30.095"));
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
        "{\"pager\":{\"page\":1,\"pageSize\":8,\"isLastPage\":false},\"items\":{\"IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"xjA5E9MimMU\":{\"uid\":\"xjA5E9MimMU\",\"name\":\"Civil status\",\"options\":[{\"uid\":\"wfkKVdPBzho\",\"code\":\"Single or widow\"}]},\"bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"code\":\"MMD_PER_STA\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"cejWyOfXge6\":{\"uid\":\"cejWyOfXge6\",\"name\":\"Gender\",\"description\":\"Gender\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"code\":\"MMD_PER_DOB\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"wfkKVdPBzho\":{\"uid\":\"wfkKVdPBzho\",\"code\":\"Single or widow\",\"name\":\"Single or widow\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"bx6fsa0t90x\":[],\"Qo571yj6Zcn\":[],\"DODgdr5Oo2v\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"xs8A6tQJY0s\":[],\"A4xFHyieXys\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"GUOBQt5K2WI\":[],\"Agywv2JGwuq\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[\"wfkKVdPBzho\"],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"a3kGcGDCuk6\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
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
        "{\"pager\":{\"page\":1,\"pageSize\":10,\"isLastPage\":false},\"items\":{\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"code\":\"MMD_PER_DOB\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"code\":\"MMD_PER_STA\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"bx6fsa0t90x\":[],\"Qo571yj6Zcn\":[],\"DODgdr5Oo2v\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"xs8A6tQJY0s\":[],\"A4xFHyieXys\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"GUOBQt5K2WI\":[],\"Agywv2JGwuq\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"a3kGcGDCuk6\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
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
