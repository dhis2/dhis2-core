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
public class TeiQuery5AutoTest extends AnalyticsApiTest {
  private AnalyticsTeiActions actions = new AnalyticsTeiActions();

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
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":false},\"items\":{\"Mnp3oXrpAbK\":{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\",\"name\":\"Female\"},\"IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"rBvjJYbMCVx\":{\"uid\":\"rBvjJYbMCVx\",\"code\":\"Male\",\"name\":\"Male\"},\"bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"code\":\"MMD_PER_STA\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"cejWyOfXge6\":{\"uid\":\"cejWyOfXge6\",\"name\":\"Gender\",\"description\":\"Gender\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"code\":\"MMD_PER_DOB\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"pC3N9N77UmT\":{\"uid\":\"pC3N9N77UmT\",\"name\":\"Gender\",\"options\":[{\"uid\":\"rBvjJYbMCVx\",\"code\":\"Male\"},{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\"}]},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"bx6fsa0t90x\":[],\"Qo571yj6Zcn\":[],\"DODgdr5Oo2v\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"xs8A6tQJY0s\":[],\"A4xFHyieXys\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"GUOBQt5K2WI\":[],\"Agywv2JGwuq\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"a3kGcGDCuk6\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
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
        response, 0, List.of("Condama MCHP", "", "", "Male", "", "", "2014-03-26 15:49:59.804"));
    validateRow(
        response,
        1,
        List.of("Taiama (Kori) CHC", "", "", "Female", "", "", "2014-03-26 15:40:13.031"));
    validateRow(
        response,
        2,
        List.of("Rokolon MCHP", "", "", "Male", "1", "0.0", "2015-08-06 21:20:52.547"));
    validateRow(
        response,
        3,
        List.of("Mutual Faith Clinic", "", "", "Male", "", "", "2014-03-26 15:48:20.653"));
    validateRow(
        response, 4, List.of("Griema MCHP", "", "", "Male", "", "", "2014-03-26 15:53:03.052"));
    validateRow(
        response, 5, List.of("Jui CHP", "", "", "Female", "", "", "2014-03-26 15:50:31.121"));
    validateRow(
        response, 6, List.of("Warrima MCHP", "", "", "Female", "", "", "2014-03-26 15:47:37.857"));
    validateRow(
        response, 7, List.of("Fadugu CHC", "", "", "Male", "", "", "2014-03-26 15:52:25.22"));
    validateRow(
        response, 8, List.of("Gbindi CHP", "", "", "Female", "1", "2.0", "2015-08-07 15:47:21.57"));
    validateRow(
        response, 9, List.of("Jormu MCHP", "", "", "Female", "", "", "2014-03-26 15:47:39.93"));
    validateRow(
        response, 10, List.of("Hima MCHP", "", "", "Female", "", "", "2014-03-26 15:43:04.976"));
    validateRow(
        response,
        11,
        List.of("Taninahun MCHP", "", "", "Female", "", "", "2014-03-26 15:43:09.475"));
    validateRow(
        response,
        12,
        List.of(
            "Senjekoro MCHP", "1973-04-29 00:00:00.0", "", "", "", "", "2017-01-26 13:48:09.055"));
    validateRow(
        response, 13, List.of("Joru CHC", "", "", "Female", "", "", "2014-03-26 15:40:34.32"));
    validateRow(
        response,
        14,
        List.of(
            "Lungi Govt. Hospital, Port Loko",
            "",
            "",
            "Female",
            "",
            "",
            "2014-03-26 15:49:48.303"));
    validateRow(
        response,
        15,
        List.of(
            "Catholic Clinic", "1990-08-15 00:00:00.0", "", "", "", "", "2017-01-26 13:48:11.257"));
    validateRow(
        response,
        16,
        List.of("Pejewa CHC", "", "", "Female", "1", "0.0", "2015-08-06 21:20:48.576"));
    validateRow(
        response, 17, List.of("EDC Unit CHP", "", "", "Male", "", "", "2014-03-26 15:50:15.704"));
    validateRow(
        response,
        18,
        List.of(
            "Marie Stopes (Kakua) Clinic", "", "", "Female", "", "", "2014-03-26 15:47:34.565"));
    validateRow(
        response, 19, List.of("Gondama MCHP", "", "", "Female", "", "", "2014-03-26 15:45:07.058"));
    validateRow(
        response,
        20,
        List.of("kamba mamudia", "", "", "Female", "", "", "2014-03-26 15:50:51.038"));
    validateRow(
        response,
        21,
        List.of("Gbangba MCHP", "", "", "Male", "1", "2.0", "2015-08-06 21:15:40.013"));
    validateRow(
        response,
        22,
        List.of("SLC. RHC Port Loko", "", "", "Male", "", "", "2014-03-26 15:50:40.948"));
    validateRow(
        response,
        23,
        List.of("Rotaimbana MCHP", "", "", "Female", "", "", "2014-03-26 15:43:33.439"));
    validateRow(
        response,
        24,
        List.of(
            "Lengekoro MCHP", "1989-11-07 00:00:00.0", "", "", "", "", "2017-01-26 13:48:10.705"));
    validateRow(
        response, 25, List.of("Kochero MCHP", "", "", "Male", "", "", "2014-03-26 15:45:55.089"));
    validateRow(
        response,
        26,
        List.of("Bonkababay MCHP", "", "", "Male", "", "", "2014-03-26 15:46:29.378"));
    validateRow(
        response, 27, List.of("Bayama MCHP", "", "", "Male", "", "", "2014-03-26 15:46:18.399"));
    validateRow(
        response,
        28,
        List.of("Govt. Hosp. Makeni", "", "", "Male", "", "", "2014-03-26 15:51:07.57"));
    validateRow(
        response,
        29,
        List.of("MCH Static Pujehun", "", "", "Male", "", "", "2014-03-26 15:47:52.85"));
    validateRow(
        response, 30, List.of("Tagrin CHC", "", "", "Female", "", "", "2014-03-26 15:41:19.623"));
    validateRow(
        response,
        31,
        List.of("Makoni Line MCHP", "", "", "Female", "", "", "2014-03-26 15:50:47.986"));
    validateRow(
        response,
        32,
        List.of("Bomu Saamba CHP", "", "", "Male", "", "", "2014-03-26 15:49:13.166"));
    validateRow(
        response, 33, List.of("Kholifaga MCHP", "", "", "Male", "", "", "2014-03-26 15:50:54.773"));
    validateRow(
        response, 34, List.of("Seidu MCHP", "", "", "Male", "", "", "2014-03-26 15:45:36.871"));
    validateRow(
        response, 35, List.of("Zimmi CHC", "", "", "Male", "", "", "2014-03-26 15:41:26.459"));
    validateRow(
        response, 36, List.of("Maraka MCHP", "", "", "Male", "", "", "2014-03-26 15:40:33.043"));
    validateRow(
        response,
        37,
        List.of("Masumana MCHP", "", "", "Female", "", "", "2014-03-26 15:48:02.749"));
    validateRow(
        response,
        38,
        List.of(
            "Senjekoro MCHP", "1986-09-27 00:00:00.0", "", "", "", "", "2017-01-26 13:48:12.908"));
    validateRow(
        response, 39, List.of("Mokelleh CHC", "", "", "Female", "", "", "2014-03-26 15:52:34.317"));
    validateRow(
        response,
        40,
        List.of("Massabendu CHP", "", "", "Female", "1", "0.0", "2015-08-06 21:20:51.879"));
    validateRow(
        response, 41, List.of("Mabunduka CHC", "", "", "Male", "", "", "2014-03-26 15:48:02.003"));
    validateRow(
        response,
        42,
        List.of("Sumbuya Bessima CHP", "", "", "Female", "", "", "2014-03-26 15:48:55.924"));
    validateRow(
        response,
        43,
        List.of("Mansundu MCHP", "", "", "Female", "1", "0.0", "2015-08-06 21:15:41.922"));
    validateRow(
        response,
        44,
        List.of("Condama MCHP", "", "", "Male", "0", "0.0", "2015-08-07 15:47:19.067"));
    validateRow(
        response,
        45,
        List.of("Ola During Clinic", "", "", "Female", "1", "1.0", "2015-08-06 21:15:41.253"));
    validateRow(
        response, 46, List.of("Liya MCHP", "", "", "Male", "0", "0.0", "2015-08-07 15:47:21.212"));
    validateRow(
        response, 47, List.of("Laleihun CHP", "", "", "Female", "", "", "2014-03-26 15:48:42.707"));
    validateRow(
        response,
        48,
        List.of("Kissy Town CHP", "", "", "Female", "", "", "2014-03-26 15:52:36.963"));
    validateRow(
        response, 49, List.of("Makabo MCHP", "", "", "Female", "", "", "2014-03-26 15:45:54.214"));
    validateRow(
        response, 50, List.of("Tonkomba MCHP", "", "", "Male", "", "", "2014-03-26 15:49:20.496"));
    validateRow(
        response, 51, List.of("Kalangba CHC", "", "", "Male", "", "", "2014-03-26 15:49:26.816"));
    validateRow(
        response,
        52,
        List.of("Malema (Yawei) CHP", "", "", "Male", "", "", "2014-03-26 15:42:50.011"));
    validateRow(
        response, 53, List.of("Gbalamuya MCHP", "", "", "Male", "", "", "2014-03-26 15:45:37.103"));
    validateRow(
        response,
        54,
        List.of("Hamdalai MCHP", "", "", "Female", "1", "1.0", "2015-08-06 21:20:46.938"));
    validateRow(
        response,
        55,
        List.of("Kagbere CHC", "", "", "Female", "0", "2.0", "2015-08-07 15:47:22.339"));
    validateRow(
        response, 56, List.of("Masofinia MCHP", "", "", "Male", "", "", "2014-03-26 15:48:59.519"));
    validateRow(
        response,
        57,
        List.of("SLRCS (Nongowa) clinic", "", "", "Male", "1", "1.0", "2015-08-06 21:20:51.006"));
    validateRow(
        response,
        58,
        List.of("Wallehun MCHP", "", "", "Female", "", "", "2014-03-26 15:50:07.295"));
    validateRow(
        response,
        59,
        List.of("Mabonkanie MCHP", "", "", "Male", "0", "0.0", "2015-08-07 15:47:24.147"));
    validateRow(
        response,
        60,
        List.of("Kassama MCHP", "", "", "Male", "0", "0.0", "2015-08-06 21:20:41.855"));
    validateRow(
        response, 61, List.of("Kortuma MCHP", "", "", "Male", "", "", "2014-03-26 15:41:02.112"));
    validateRow(
        response,
        62,
        List.of("Mabineh MCHP", "", "", "Male", "0", "1.0", "2015-08-07 15:47:19.801"));
    validateRow(
        response,
        63,
        List.of("Yoyema MCHP", "", "", "Male", "0", "0.0", "2015-08-06 21:20:52.061"));
    validateRow(
        response,
        64,
        List.of("Modia MCHP", "", "", "Female", "1", "1.0", "2015-08-07 15:47:21.606"));
    validateRow(
        response, 65, List.of("Mbaoma CHP", "", "", "Male", "0", "1.0", "2015-08-06 21:20:50.663"));
    validateRow(
        response,
        66,
        List.of("St. Luke's Wellington", "", "", "Male", "", "", "2014-03-26 15:40:33.445"));
    validateRow(
        response, 67, List.of("Alkalia CHP", "", "", "Female", "", "", "2014-03-26 15:43:12.559"));
    validateRow(
        response,
        68,
        List.of("Kamakwie MCHP", "", "", "Male", "1", "1.0", "2015-08-06 21:20:51.799"));
    validateRow(
        response,
        69,
        List.of("Ngiehun (Lower Bambara) MCHP", "", "", "Male", "", "", "2014-03-26 15:52:28.262"));
    validateRow(
        response,
        70,
        List.of("EPI Headquarter", "", "", "Male", "", "", "2014-03-26 15:49:30.674"));
    validateRow(
        response, 71, List.of("Morfindor CHP", "", "", "Male", "", "", "2014-03-26 15:44:12.232"));
    validateRow(
        response, 72, List.of("Needy CHC", "", "", "Female", "", "", "2014-03-26 15:47:26.487"));
    validateRow(
        response,
        73,
        List.of("Tombo Wallah CHP", "", "", "Female", "", "", "2014-03-26 15:40:21.491"));
    validateRow(
        response, 74, List.of("Yiffin CHC", "", "", "Female", "", "", "2014-03-26 15:44:03.487"));
    validateRow(
        response,
        75,
        List.of("Yakaji MCHP", "", "", "Female", "0", "1.0", "2015-08-06 21:12:34.355"));
    validateRow(
        response, 76, List.of("Weima CHC", "", "", "Female", "", "", "2014-03-26 15:52:33.417"));
    validateRow(
        response,
        77,
        List.of(
            "Taninihun Mboka MCHP",
            "1984-05-19 00:00:00.0",
            "",
            "",
            "",
            "",
            "2017-01-26 13:43:30.3"));
    validateRow(
        response, 78, List.of("Sahn CHC", "", "", "Female", "", "", "2014-03-26 15:51:20.521"));
    validateRow(
        response,
        79,
        List.of("Mofombo MCHP", "", "", "Male", "1", "1.0", "2015-08-07 15:47:21.849"));
    validateRow(
        response,
        80,
        List.of("Giema (Luawa) MCHP", "", "", "Male", "", "", "2014-03-26 15:43:43.451"));
    validateRow(
        response,
        81,
        List.of("Vaama  (kpanga krim) MCHP", "", "", "Male", "", "", "2014-03-26 15:40:59.801"));
    validateRow(
        response,
        82,
        List.of("Magbengberah MCHP", "", "", "Female", "", "", "2014-03-26 15:43:58.36"));
    validateRow(
        response, 83, List.of("Futa CHC", "", "", "Female", "1", "2.0", "2015-08-06 21:20:50.245"));
    validateRow(
        response,
        84,
        List.of(
            "Njala University Hospital", "", "", "Male", "0", "2.0", "2015-08-06 21:20:41.503"));
    validateRow(
        response, 85, List.of("Guala MCHP", "", "", "Male", "", "", "2014-03-26 15:47:05.757"));
    validateRow(
        response, 86, List.of("Dankawalia MCHP", "", "", "Male", "", "", "2014-03-26 15:44:19.72"));
    validateRow(
        response,
        87,
        List.of("Salima MCHP", "", "", "Female", "0", "2.0", "2015-08-06 21:20:51.366"));
    validateRow(
        response,
        88,
        List.of("Dankawalia MCHP", "", "", "Male", "1", "1.0", "2015-08-07 15:47:25.494"));
    validateRow(
        response, 89, List.of("Wordu CHP", "", "", "Female", "", "", "2014-03-26 15:50:02.758"));
    validateRow(
        response,
        90,
        List.of("Bendu Mameima CHC", "", "", "Male", "0", "1.0", "2015-08-07 15:47:23.128"));
    validateRow(
        response, 91, List.of("Bapuya MCHP", "", "", "Female", "", "", "2014-03-26 15:43:18.287"));
    validateRow(
        response, 92, List.of("Gbenikoro MCHP", "", "", "Male", "", "", "2014-03-26 15:40:46.436"));
    validateRow(
        response,
        93,
        List.of("Air Port Centre, Lungi", "", "", "Male", "", "", "2014-03-26 15:46:03.781"));
    validateRow(
        response,
        94,
        List.of("Gbandiwulo CHP", "", "", "Female", "", "", "2014-03-26 15:46:27.134"));
    validateRow(
        response, 95, List.of("Jojoima CHC", "", "", "Female", "", "", "2014-03-26 15:50:40.89"));
    validateRow(
        response,
        96,
        List.of("Koinadugu II CHP", "", "", "Male", "", "", "2014-03-26 15:44:13.785"));
    validateRow(
        response,
        97,
        List.of("Mathufulie MCHP", "", "", "Female", "", "", "2014-03-26 15:42:59.669"));
    validateRow(
        response,
        98,
        List.of("Dankawalia MCHP", "", "", "Female", "", "", "2014-03-26 15:45:59.432"));
    validateRow(
        response, 99, List.of("Pehala MCHP", "", "", "Female", "", "", "2014-03-26 15:43:12.268"));
  }

  @Test
  public void sortByEnrollmentOuAsc() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=IpHINAT79UW.enrollmentdate")
            .add(
                "headers=ouname,gHGyrwKPzej,ciq2USN94oJ,IpHINAT79UW.ouname,IpHINAT79UW.enrollmentdate")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("pageSize=100")
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
        .body("rows", hasSize(equalTo(100)))
        .body("height", equalTo(100))
        .body("width", equalTo(5))
        .body("headerWidth", equalTo(5));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":false},\"items\":{\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"code\":\"MMD_PER_DOB\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"code\":\"MMD_PER_STA\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"IpHINAT79UW.enrollmentdate\":{\"name\":\"Date of enrollment\",\"dimensionType\":\"PERIOD\"},\"IpHINAT79UW.ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"Qo571yj6Zcn\":[],\"DODgdr5Oo2v\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"xs8A6tQJY0s\":[],\"A4xFHyieXys\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"GUOBQt5K2WI\":[],\"Agywv2JGwuq\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
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
        "IpHINAT79UW.ouname",
        "Organisation Unit Name, Child Programme",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        4,
        "IpHINAT79UW.enrollmentdate",
        "Date of enrollment, Child Programme",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of("Tombo Wallah CHP", "", "", "Tombo Wallah CHP", "2022-01-01 12:05:00.0"));
    validateRow(
        response,
        1,
        List.of("Conakry Dee CHC", "", "", "Conakry Dee CHC", "2022-01-01 12:05:00.0"));
    validateRow(
        response,
        2,
        List.of("Dankawalie MCHP", "", "", "Dankawalie MCHP", "2022-01-01 12:05:00.0"));
    validateRow(
        response, 3, List.of("Niagorehun CHP", "", "", "Niagorehun CHP", "2022-01-01 12:05:00.0"));
    validateRow(
        response,
        4,
        List.of("Banka Makuloh MCHP", "", "", "Banka Makuloh MCHP", "2022-01-01 12:05:00.0"));
    validateRow(
        response,
        5,
        List.of("Holy Mary Clinic", "", "", "Holy Mary Clinic", "2022-01-01 12:05:00.0"));
    validateRow(
        response,
        6,
        List.of("Bandajuma Sinneh MCHP", "", "", "Bandajuma Sinneh MCHP", "2022-01-01 12:05:00.0"));
    validateRow(
        response,
        7,
        List.of("Gbainty Wallah CHP", "", "", "Gbainty Wallah CHP", "2022-01-01 12:05:00.0"));
    validateRow(
        response, 8, List.of("Menika MCHP", "", "", "Menika MCHP", "2022-01-01 12:05:00.0"));
    validateRow(
        response, 9, List.of("Maselleh MCHP", "", "", "Maselleh MCHP", "2022-01-01 12:05:00.0"));
    validateRow(
        response, 10, List.of("Mokotawa CHP", "", "", "Mokotawa CHP", "2022-01-01 12:05:00.0"));
    validateRow(
        response,
        11,
        List.of("Yorgbofore MCHP", "", "", "Yorgbofore MCHP", "2022-01-01 12:05:00.0"));
    validateRow(
        response,
        12,
        List.of("Looking Town MCHP", "", "", "Looking Town MCHP", "2022-01-01 12:05:00.0"));
    validateRow(response, 13, List.of("Seria MCHP", "", "", "Seria MCHP", "2022-01-01 12:05:00.0"));
    validateRow(
        response,
        14,
        List.of(
            "Mosenessie Junction MCHP",
            "",
            "",
            "Mosenessie Junction MCHP",
            "2022-01-01 12:05:00.0"));
    validateRow(
        response, 15, List.of("Masankorie CHP", "", "", "Masankorie CHP", "2022-01-01 12:05:00.0"));
    validateRow(response, 16, List.of("Sienga CHP", "", "", "Sienga CHP", "2022-01-01 12:05:00.0"));
    validateRow(response, 17, List.of("Feiba CHP", "", "", "Feiba CHP", "2022-01-01 12:05:00.0"));
    validateRow(
        response, 18, List.of("Baiwala CHP", "", "", "Baiwala CHP", "2022-01-01 12:05:00.0"));
    validateRow(
        response, 19, List.of("Yoyema MCHP", "", "", "Yoyema MCHP", "2022-01-01 12:05:00.0"));
    validateRow(
        response, 20, List.of("Ngiewahun CHP", "", "", "Ngiewahun CHP", "2022-01-01 12:05:00.0"));
    validateRow(
        response, 21, List.of("Kuntorloh CHP", "", "", "Kuntorloh CHP", "2022-01-01 12:05:00.0"));
    validateRow(response, 22, List.of("Gao MCHP", "", "", "Gao MCHP", "2022-01-01 12:05:00.0"));
    validateRow(response, 23, List.of("Woroma CHP", "", "", "Woroma CHP", "2022-01-01 12:05:00.0"));
    validateRow(
        response, 24, List.of("Kochero MCHP", "", "", "Kochero MCHP", "2022-01-01 12:05:00.0"));
    validateRow(response, 25, List.of("Jormu MCHP", "", "", "Jormu MCHP", "2022-01-01 12:05:00.0"));
    validateRow(
        response, 26, List.of("Mamusa MCHP", "", "", "Mamusa MCHP", "2022-01-01 12:05:00.0"));
    validateRow(
        response,
        27,
        List.of("Kangama (Kangama) CHP", "", "", "Kangama (Kangama) CHP", "2022-01-01 12:05:00.0"));
    validateRow(
        response,
        28,
        List.of("Niagorehun MCHP", "", "", "Niagorehun MCHP", "2022-01-01 12:05:00.0"));
    validateRow(
        response,
        29,
        List.of("Mano Menima CHP", "", "", "Mano Menima CHP", "2022-01-01 12:05:00.0"));
    validateRow(response, 30, List.of("Woama MCHP", "", "", "Woama MCHP", "2022-01-01 12:05:00.0"));
    validateRow(
        response,
        31,
        List.of("SLC. RHC Port Loko", "", "", "SLC. RHC Port Loko", "2022-01-01 12:05:00.0"));
    validateRow(
        response, 32, List.of("Masuba MCHP", "", "", "Masuba MCHP", "2022-01-02 12:05:00.0"));
    validateRow(
        response,
        33,
        List.of("Mangay Loko MCHP", "", "", "Mangay Loko MCHP", "2022-01-02 12:05:00.0"));
    validateRow(
        response,
        34,
        List.of("Sembehun 17 CHP", "", "", "Sembehun 17 CHP", "2022-01-02 12:05:00.0"));
    validateRow(
        response,
        35,
        List.of("Magbengbeh MCHP", "", "", "Magbengbeh MCHP", "2022-01-02 12:05:00.0"));
    validateRow(
        response, 36, List.of("Benduma MCHP", "", "", "Benduma MCHP", "2022-01-02 12:05:00.0"));
    validateRow(
        response,
        37,
        List.of("Ngiehun Kongo CHP", "", "", "Ngiehun Kongo CHP", "2022-01-02 12:05:00.0"));
    validateRow(
        response,
        38,
        List.of("Mokongbetty MCHP", "", "", "Mokongbetty MCHP", "2022-01-02 12:05:00.0"));
    validateRow(
        response, 39, List.of("Kormende MCHP", "", "", "Kormende MCHP", "2022-01-02 12:05:00.0"));
    validateRow(response, 40, List.of("Ganya MCHP", "", "", "Ganya MCHP", "2022-01-02 12:05:00.0"));
    validateRow(
        response, 41, List.of("Boajibu CHC", "", "", "Boajibu CHC", "2022-01-02 12:05:00.0"));
    validateRow(
        response, 42, List.of("Kareneh MCHP", "", "", "Kareneh MCHP", "2022-01-02 12:05:00.0"));
    validateRow(
        response, 43, List.of("Massaba MCHP", "", "", "Massaba MCHP", "2022-01-02 12:05:00.0"));
    validateRow(
        response, 44, List.of("Kemedugu MCHP", "", "", "Kemedugu MCHP", "2022-01-02 12:05:00.0"));
    validateRow(
        response,
        45,
        List.of("Kabba Ferry MCHP", "", "", "Kabba Ferry MCHP", "2022-01-02 12:05:00.0"));
    validateRow(
        response,
        46,
        List.of("Madina (Malema) MCHP", "", "", "Madina (Malema) MCHP", "2022-01-02 12:05:00.0"));
    validateRow(
        response, 47, List.of("Gbamgbama CHC", "", "", "Gbamgbama CHC", "2022-01-02 12:05:00.0"));
    validateRow(
        response, 48, List.of("Jangalor MCHP", "", "", "Jangalor MCHP", "2022-01-02 12:05:00.0"));
    validateRow(
        response,
        49,
        List.of(
            "Maforay (B. Sebora) MCHP",
            "",
            "",
            "Maforay (B. Sebora) MCHP",
            "2022-01-02 12:05:00.0"));
    validateRow(
        response, 50, List.of("Fengehun MCHP", "", "", "Fengehun MCHP", "2022-01-02 12:05:00.0"));
    validateRow(
        response, 51, List.of("Mabain MCHP", "", "", "Mabain MCHP", "2022-01-02 12:05:00.0"));
    validateRow(
        response,
        52,
        List.of("Kissy Koya MCHP", "", "", "Kissy Koya MCHP", "2022-01-02 12:05:00.0"));
    validateRow(
        response, 53, List.of("Mayepoh CHC", "", "", "Mayepoh CHC", "2022-01-02 12:05:00.0"));
    validateRow(response, 54, List.of("Gao MCHP", "", "", "Gao MCHP", "2022-01-02 12:05:00.0"));
    validateRow(
        response,
        55,
        List.of("Taninahun (Malen) CHP", "", "", "Taninahun (Malen) CHP", "2022-01-02 12:05:00.0"));
    validateRow(
        response,
        56,
        List.of("Bandajuma Sinneh MCHP", "", "", "Bandajuma Sinneh MCHP", "2022-01-02 12:05:00.0"));
    validateRow(
        response,
        57,
        List.of(
            "Sandia (Kissi Tongi) CHP",
            "",
            "",
            "Sandia (Kissi Tongi) CHP",
            "2022-01-02 12:05:00.0"));
    validateRow(
        response, 58, List.of("Mbundorbu MCHP", "", "", "Mbundorbu MCHP", "2022-01-02 12:05:00.0"));
    validateRow(response, 59, List.of("Juma MCHP", "", "", "Juma MCHP", "2022-01-02 12:05:00.0"));
    validateRow(response, 60, List.of("Kagbo MCHP", "", "", "Kagbo MCHP", "2022-01-02 12:05:00.0"));
    validateRow(response, 61, List.of("Konjo MCHP", "", "", "Konjo MCHP", "2022-01-02 12:05:00.0"));
    validateRow(
        response, 62, List.of("Mapotolon CHC", "", "", "Mapotolon CHC", "2022-01-02 12:05:00.0"));
    validateRow(
        response,
        63,
        List.of(
            "Manjama Shellmingo CHC", "", "", "Manjama Shellmingo CHC", "2022-01-02 12:05:00.0"));
    validateRow(
        response,
        64,
        List.of(
            "Ahmadiyya Muslim Hospital",
            "",
            "",
            "Ahmadiyya Muslim Hospital",
            "2022-01-03 12:05:00.0"));
    validateRow(
        response, 65, List.of("Kemedugu MCHP", "", "", "Kemedugu MCHP", "2022-01-03 12:05:00.0"));
    validateRow(
        response, 66, List.of("Mokorewa MCHP", "", "", "Mokorewa MCHP", "2022-01-03 12:05:00.0"));
    validateRow(
        response,
        67,
        List.of(
            "Mabenteh Community Hospital",
            "",
            "",
            "Mabenteh Community Hospital",
            "2022-01-03 12:05:00.0"));
    validateRow(
        response,
        68,
        List.of("Leprosy & TB Hospital", "", "", "Leprosy & TB Hospital", "2022-01-03 12:05:00.0"));
    validateRow(
        response, 69, List.of("Gorahun CHC", "", "", "Gorahun CHC", "2022-01-03 12:05:00.0"));
    validateRow(
        response,
        70,
        List.of("Fotaneh Junction MCHP", "", "", "Fotaneh Junction MCHP", "2022-01-03 12:05:00.0"));
    validateRow(
        response,
        71,
        List.of("Afro Arab Clinic", "", "", "Afro Arab Clinic", "2022-01-03 12:05:00.0"));
    validateRow(
        response,
        72,
        List.of("Senekedugu MCHP", "", "", "Senekedugu MCHP", "2022-01-03 12:05:00.0"));
    validateRow(response, 73, List.of("Karlu CHC", "", "", "Karlu CHC", "2022-01-03 12:05:00.0"));
    validateRow(
        response, 74, List.of("Maharie MCHP", "", "", "Maharie MCHP", "2022-01-03 12:05:00.0"));
    validateRow(
        response, 75, List.of("Mambolo CHC", "", "", "Mambolo CHC", "2022-01-03 12:05:00.0"));
    validateRow(
        response, 76, List.of("Kayongoro MCHP", "", "", "Kayongoro MCHP", "2022-01-03 12:05:00.0"));
    validateRow(
        response, 77, List.of("Lowoma MCHP", "", "", "Lowoma MCHP", "2022-01-03 12:05:00.0"));
    validateRow(response, 78, List.of("Makali CHC", "", "", "Makali CHC", "2022-01-03 12:05:00.0"));
    validateRow(
        response, 79, List.of("Benkia MCHP", "", "", "Benkia MCHP", "2022-01-03 12:05:00.0"));
    validateRow(response, 80, List.of("Agape CHP", "", "", "Agape CHP", "2022-01-03 12:05:00.0"));
    validateRow(response, 81, List.of("Mobai CHC", "", "", "Mobai CHC", "2022-01-03 12:05:00.0"));
    validateRow(
        response,
        82,
        List.of("Magbengbeh MCHP", "", "", "Magbengbeh MCHP", "2022-01-03 12:05:00.0"));
    validateRow(
        response,
        83,
        List.of("St. Mary's Clinic", "", "", "St. Mary's Clinic", "2022-01-03 12:05:00.0"));
    validateRow(
        response, 84, List.of("Masoko MCHP", "", "", "Masoko MCHP", "2022-01-03 12:05:00.0"));
    validateRow(
        response, 85, List.of("Kpetewoma CHP", "", "", "Kpetewoma CHP", "2022-01-03 12:05:00.0"));
    validateRow(
        response,
        86,
        List.of("Madina Gbonkobor MCHP", "", "", "Madina Gbonkobor MCHP", "2022-01-03 12:05:00.0"));
    validateRow(
        response,
        87,
        List.of("Njagbwema Fiama CHC", "", "", "Njagbwema Fiama CHC", "2022-01-03 12:05:00.0"));
    validateRow(
        response,
        88,
        List.of("St. Joseph's Clinic", "", "", "St. Joseph's Clinic", "2022-01-03 12:05:00.0"));
    validateRow(
        response, 89, List.of("Bumbanday MCHP", "", "", "Bumbanday MCHP", "2022-01-03 12:05:00.0"));
    validateRow(
        response,
        90,
        List.of(
            "Bendoma (Malegohun) MCHP",
            "",
            "",
            "Bendoma (Malegohun) MCHP",
            "2022-01-03 12:05:00.0"));
    validateRow(response, 91, List.of("Baama CHC", "", "", "Baama CHC", "2022-01-03 12:05:00.0"));
    validateRow(response, 92, List.of("Samaya CHP", "", "", "Samaya CHP", "2022-01-03 12:05:00.0"));
    validateRow(
        response, 93, List.of("Bucksal Clinic", "", "", "Bucksal Clinic", "2022-01-04 12:05:00.0"));
    validateRow(response, 94, List.of("Falaba CHP", "", "", "Falaba CHP", "2022-01-04 12:05:00.0"));
    validateRow(
        response,
        95,
        List.of(
            "Serabu Hospital Mission", "", "", "Serabu Hospital Mission", "2022-01-04 12:05:00.0"));
    validateRow(
        response,
        96,
        List.of(
            "Kpetema (Lower Bambara) CHP",
            "",
            "",
            "Kpetema (Lower Bambara) CHP",
            "2022-01-04 12:05:00.0"));
    validateRow(
        response, 97, List.of("Makalie MCHP", "", "", "Makalie MCHP", "2022-01-04 12:05:00.0"));
    validateRow(
        response, 98, List.of("Koeyor MCHP", "", "", "Koeyor MCHP", "2022-01-04 12:05:00.0"));
    validateRow(
        response, 99, List.of("Mabunduka CHC", "", "", "Mabunduka CHC", "2022-01-04 12:05:00.0"));
  }

  @Test
  public void sortByProgramStatusAsc() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=IpHINAT79UW.programstatus")
            .add(
                "headers=ouname,gHGyrwKPzej,ciq2USN94oJ,IpHINAT79UW.ouname,IpHINAT79UW.enrollmentdate,IpHINAT79UW.programstatus")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=ou:USER_ORGUNIT,gHGyrwKPzej,ciq2USN94oJ")
            .add("relativePeriodDate=2017-01-27");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(6)))
        .body("rows", hasSize(equalTo(100)))
        .body("height", equalTo(100))
        .body("width", equalTo(6))
        .body("headerWidth", equalTo(6));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":false},\"items\":{\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"code\":\"MMD_PER_DOB\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"code\":\"MMD_PER_STA\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"IpHINAT79UW.enrollmentdate\":{\"name\":\"Date of enrollment\",\"dimensionType\":\"PERIOD\"},\"IpHINAT79UW.ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"Qo571yj6Zcn\":[],\"DODgdr5Oo2v\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"xs8A6tQJY0s\":[],\"A4xFHyieXys\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"GUOBQt5K2WI\":[],\"Agywv2JGwuq\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
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
        "IpHINAT79UW.ouname",
        "Organisation Unit Name, Child Programme",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        4,
        "IpHINAT79UW.enrollmentdate",
        "Date of enrollment, Child Programme",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(
        response,
        5,
        "IpHINAT79UW.programstatus",
        "Program Status, Child Programme",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of("Makaba MCHP", "", "", "Makaba MCHP", "2022-09-21 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        1,
        List.of(
            "Levuma Nyomeh CHP", "", "", "Levuma Nyomeh CHP", "2022-12-15 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        2,
        List.of("Yengema CHP", "", "", "Yengema CHP", "2022-08-20 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        3,
        List.of("Kpumbu MCHP", "", "", "Kpumbu MCHP", "2022-07-15 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        4,
        List.of("Baomahun CHC", "", "", "Baomahun CHC", "2022-07-31 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        5,
        List.of(
            "Deep Eye water MCHP",
            "",
            "",
            "Deep Eye water MCHP",
            "2022-10-28 12:05:00.0",
            "ACTIVE"));
    validateRow(
        response,
        6,
        List.of("Maguama CHP", "", "", "Maguama CHP", "2022-11-29 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        7,
        List.of("Tassoh MCHP", "", "", "Tassoh MCHP", "2022-06-16 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        8,
        List.of("Kpayama 1 MCHP", "", "", "Kpayama 1 MCHP", "2022-03-23 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        9,
        List.of("Kokoru CHP", "", "", "Kokoru CHP", "2022-05-15 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        10,
        List.of("Perrie MCHP", "", "", "Perrie MCHP", "2022-11-07 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        11,
        List.of("Falaba CHP", "", "", "Falaba CHP", "2022-01-04 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        12,
        List.of("Damballa CHC", "", "", "Damballa CHC", "2022-08-13 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        13,
        List.of("Njama MCHP", "", "", "Njama MCHP", "2022-11-28 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        14,
        List.of("Ngelehun CHC", "", "", "Ngelehun CHC", "2022-11-03 01:00:00.0", "ACTIVE"));
    validateRow(
        response,
        15,
        List.of("Gbangeima MCHP", "", "", "Gbangeima MCHP", "2022-03-12 12:05:00.0", "ACTIVE"));
    validateRow(
        response, 16, List.of("Agape CHP", "", "", "Agape CHP", "2022-09-26 12:05:00.0", "ACTIVE"));
    validateRow(
        response, 17, List.of("Blama CHC", "", "", "Blama CHC", "2022-07-21 12:05:00.0", "ACTIVE"));
    validateRow(
        response, 18, List.of("Liya MCHP", "", "", "Liya MCHP", "2022-10-26 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        19,
        List.of("Rothatha MCHP", "", "", "Rothatha MCHP", "2022-10-17 12:05:00.0", "ACTIVE"));
    validateRow(
        response, 20, List.of("Yoni CHC", "", "", "Yoni CHC", "2022-12-15 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        21,
        List.of(
            "Plantain Island MCHP",
            "",
            "",
            "Plantain Island MCHP",
            "2022-11-15 12:05:00.0",
            "ACTIVE"));
    validateRow(
        response,
        22,
        List.of(
            "Magbengberah MCHP", "", "", "Magbengberah MCHP", "2022-04-09 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        23,
        List.of(
            "Gbendembu Wesleyan CHC",
            "",
            "",
            "Gbendembu Wesleyan CHC",
            "2022-09-18 12:05:00.0",
            "ACTIVE"));
    validateRow(
        response,
        24,
        List.of("Ngelehun CHC", "", "", "Ngelehun CHC", "2022-11-14 01:00:00.0", "ACTIVE"));
    validateRow(
        response,
        25,
        List.of("Kiampkakolo MCHP", "", "", "Kiampkakolo MCHP", "2022-08-30 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        26,
        List.of("Karina MCHP", "", "", "Karina MCHP", "2022-10-19 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        27,
        List.of(
            "Ngiehun Kongo CHP", "", "", "Ngiehun Kongo CHP", "2022-11-04 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        28,
        List.of(
            "Marie Stopes Clinic (Abedeen R)",
            "",
            "",
            "Marie Stopes Clinic (Abedeen R)",
            "2022-12-21 12:05:00.0",
            "ACTIVE"));
    validateRow(
        response,
        29,
        List.of("Patama MCHP", "", "", "Patama MCHP", "2022-02-18 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        30,
        List.of("Ngelehun CHC", "", "", "Ngelehun CHC", "2022-11-03 01:00:00.0", "ACTIVE"));
    validateRow(
        response,
        31,
        List.of("Ngelehun CHC", "", "", "Ngelehun CHC", "2022-11-01 01:00:00.0", "ACTIVE"));
    validateRow(
        response,
        32,
        List.of("Ngelehun CHC", "", "", "Ngelehun CHC", "2022-11-13 01:00:00.0", "ACTIVE"));
    validateRow(
        response,
        33,
        List.of("Seidu MCHP", "", "", "Seidu MCHP", "2022-09-15 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        34,
        List.of("Ngelehun CHC", "", "", "Ngelehun CHC", "2022-03-06 00:00:00.0", "ACTIVE"));
    validateRow(
        response,
        35,
        List.of("Mayossoh MCHP", "", "", "Mayossoh MCHP", "2022-12-13 12:05:00.0", "ACTIVE"));
    validateRow(
        response, 36, List.of("Foya CHP", "", "", "Foya CHP", "2022-10-27 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        37,
        List.of("Tugbebu CHP", "", "", "Tugbebu CHP", "2022-06-25 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        38,
        List.of(
            "Bandajuma Sinneh MCHP",
            "",
            "",
            "Bandajuma Sinneh MCHP",
            "2022-03-14 12:05:00.0",
            "ACTIVE"));
    validateRow(
        response,
        39,
        List.of("Mbowohun CHP", "", "", "Mbowohun CHP", "2022-05-08 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        40,
        List.of("Fatibra CHP", "", "", "Fatibra CHP", "2022-03-08 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        41,
        List.of("Dankawalia MCHP", "", "", "Dankawalia MCHP", "2022-11-02 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        42,
        List.of("Manjama MCHP", "", "", "Manjama MCHP", "2022-01-17 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        43,
        List.of("Massam MCHP", "", "", "Massam MCHP", "2022-09-23 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        44,
        List.of("Makeni-Lol MCHP", "", "", "Makeni-Lol MCHP", "2022-01-15 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        45,
        List.of("sonkoya MCHP", "", "", "sonkoya MCHP", "2022-03-21 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        46,
        List.of("Tambiama CHC", "", "", "Tambiama CHC", "2022-03-26 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        47,
        List.of("Nagbena CHP", "", "", "Nagbena CHP", "2022-11-20 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        48,
        List.of("Sumbuya CHC", "", "", "Sumbuya CHC", "2022-05-06 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        49,
        List.of("Gbongboma MCHP", "", "", "Gbongboma MCHP", "2022-06-01 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        50,
        List.of("Ngelehun CHC", "", "", "Ngelehun CHC", "2022-09-01 02:00:00.0", "ACTIVE"));
    validateRow(
        response,
        51,
        List.of("Ngelehun CHC", "", "", "Ngelehun CHC", "2022-07-02 02:00:00.0", "ACTIVE"));
    validateRow(
        response,
        52,
        List.of("Ngelehun CHC", "", "", "Ngelehun CHC", "2022-09-04 02:00:00.0", "ACTIVE"));
    validateRow(
        response,
        53,
        List.of(
            "Magbolonthor MCHP", "", "", "Magbolonthor MCHP", "2022-06-04 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        54,
        List.of("Seidu MCHP", "", "", "Seidu MCHP", "2022-09-05 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        55,
        List.of("Levuma CHP", "", "", "Levuma CHP", "2022-06-03 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        56,
        List.of(
            "Baoma Oil Mill CHC", "", "", "Baoma Oil Mill CHC", "2022-10-25 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        57,
        List.of("Moriba Town CHC", "", "", "Moriba Town CHC", "2022-01-06 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        58,
        List.of("Gbuihun MCHP", "", "", "Gbuihun MCHP", "2022-03-07 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        59,
        List.of("Mabora MCHP", "", "", "Mabora MCHP", "2022-11-15 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        60,
        List.of(
            "Blessed Mokaba clinic",
            "",
            "",
            "Blessed Mokaba clinic",
            "2022-07-14 12:05:00.0",
            "ACTIVE"));
    validateRow(
        response,
        61,
        List.of("Foakor MCHP", "", "", "Foakor MCHP", "2022-01-27 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        62,
        List.of("Konia MCHP", "", "", "Konia MCHP", "2022-04-29 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        63,
        List.of("Ngelehun CHC", "", "", "Ngelehun CHC", "2022-06-01 02:00:00.0", "ACTIVE"));
    validateRow(
        response,
        64,
        List.of("Ngelehun CHC", "", "", "Ngelehun CHC", "2022-11-03 01:00:00.0", "ACTIVE"));
    validateRow(
        response,
        65,
        List.of("Ngelehun CHC", "", "", "Ngelehun CHC", "2022-11-12 01:00:00.0", "ACTIVE"));
    validateRow(
        response,
        66,
        List.of("Ngelehun CHC", "", "", "Ngelehun CHC", "2023-01-01 01:00:00.0", "ACTIVE"));
    validateRow(
        response,
        67,
        List.of("Ngelehun CHC", "", "", "Ngelehun CHC", "2022-11-02 01:00:00.0", "ACTIVE"));
    validateRow(
        response,
        68,
        List.of("Bum Kaku MCHP", "", "", "Bum Kaku MCHP", "2022-08-09 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        69,
        List.of("Ngelehun CHC", "", "", "Ngelehun CHC", "2023-05-01 02:00:00.0", "ACTIVE"));
    validateRow(
        response,
        70,
        List.of("Rokolon MCHP", "", "", "Rokolon MCHP", "2022-05-21 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        71,
        List.of("Tungie CHC", "", "", "Tungie CHC", "2022-01-13 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        72,
        List.of("Rotifunk CHC", "", "", "Rotifunk CHC", "2022-08-14 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        73,
        List.of(
            "Komrabai Ngolla MCHP",
            "",
            "",
            "Komrabai Ngolla MCHP",
            "2022-01-28 12:05:00.0",
            "ACTIVE"));
    validateRow(
        response,
        74,
        List.of("Koyagbema MCHP", "", "", "Koyagbema MCHP", "2022-11-24 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        75,
        List.of("Mid Land MCHP", "", "", "Mid Land MCHP", "2022-07-14 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        76,
        List.of("Tobanda CHC", "", "", "Tobanda CHC", "2022-10-12 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        77,
        List.of("Tokpombu MCHP", "", "", "Tokpombu MCHP", "2022-04-24 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        78,
        List.of(
            "Petifu Fulamasa MCHP",
            "",
            "",
            "Petifu Fulamasa MCHP",
            "2022-04-22 12:05:00.0",
            "ACTIVE"));
    validateRow(
        response, 79, List.of("Juma MCHP", "", "", "Juma MCHP", "2022-03-02 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        80,
        List.of("Yamandu CHC", "", "", "Yamandu CHC", "2022-12-16 12:05:00.0", "ACTIVE"));
    validateRow(
        response, 81, List.of("Golu MCHP", "", "", "Golu MCHP", "2022-07-28 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        82,
        List.of("Perrie MCHP", "", "", "Perrie MCHP", "2022-01-05 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        83,
        List.of(
            "MCH (Kakua) Static", "", "", "MCH (Kakua) Static", "2022-04-02 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        84,
        List.of("Baoma-Peje CHP", "", "", "Baoma-Peje CHP", "2022-02-21 12:05:00.0", "ACTIVE"));
    validateRow(
        response, 85, List.of("Loppa CHP", "", "", "Loppa CHP", "2022-01-19 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        86,
        List.of("Kensay MCHP", "", "", "Kensay MCHP", "2022-12-03 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        87,
        List.of("Yengema CHC", "", "", "Yengema CHC", "2022-12-09 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        88,
        List.of("Gendema MCHP", "", "", "Gendema MCHP", "2022-02-16 12:05:00.0", "ACTIVE"));
    validateRow(
        response, 89, List.of("Jembe CHC", "", "", "Jembe CHC", "2022-12-03 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        90,
        List.of("Kpewama MCHP", "", "", "Kpewama MCHP", "2022-01-16 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        91,
        List.of(
            "Sembehun Mamagewor MCHP",
            "",
            "",
            "Sembehun Mamagewor MCHP",
            "2022-01-16 12:05:00.0",
            "ACTIVE"));
    validateRow(
        response,
        92,
        List.of("Gelehun MCHP", "", "", "Gelehun MCHP", "2022-01-17 12:05:00.0", "ACTIVE"));
    validateRow(
        response, 93, List.of("Telu CHP", "", "", "Telu CHP", "2022-03-28 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        94,
        List.of("Rogballan MCHP", "", "", "Rogballan MCHP", "2022-08-06 12:05:00.0", "ACTIVE"));
    validateRow(
        response, 95, List.of("Joru CHC", "", "", "Joru CHC", "2022-03-06 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        96,
        List.of("UFC Port Loko", "", "", "UFC Port Loko", "2022-06-20 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        97,
        List.of(
            "Sembehun Mamagewor MCHP",
            "",
            "",
            "Sembehun Mamagewor MCHP",
            "2022-05-12 12:05:00.0",
            "ACTIVE"));
    validateRow(
        response,
        98,
        List.of("Mbaoma CHP", "", "", "Mbaoma CHP", "2022-08-07 12:05:00.0", "ACTIVE"));
    validateRow(
        response,
        99,
        List.of("Gbongongor CHP", "", "", "Gbongongor CHP", "2022-09-11 12:05:00.0", "ACTIVE"));
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
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"qDkgAbB5Jlk.hYyB7FUS5eR.GyJHQUWZ9Rl\":{\"uid\":\"GyJHQUWZ9Rl\",\"name\":\"GPS DataElement\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"COORDINATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"qDkgAbB5Jlk.wYTF0YCHMWr.rzhHSqK3lQq\":{\"uid\":\"rzhHSqK3lQq\",\"name\":\"Fever\",\"description\":\"Does the case have fever or not\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"GyJHQUWZ9Rl\":{\"uid\":\"GyJHQUWZ9Rl\",\"name\":\"GPS DataElement\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"COORDINATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"JINgGHgqzSN\":{\"uid\":\"JINgGHgqzSN\",\"code\":\"CS004\",\"name\":\"Weight\",\"description\":\"The weight of the case in kg at the time of diagnosis\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"D9yTOOLGz0j\":{\"uid\":\"D9yTOOLGz0j\",\"name\":\"Malaria case detection type\",\"options\":[{\"uid\":\"SepVHxunjMN\",\"code\":\"PASSIVE\"},{\"uid\":\"fa1IdKtq4VX\",\"code\":\"REACTIVE\"}]},\"O6uvpzGd5pu\":{\"uid\":\"O6uvpzGd5pu\",\"code\":\"OU_264\",\"name\":\"Bo\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"qDkgAbB5Jlk.hYyB7FUS5eR.JINgGHgqzSN\":{\"uid\":\"JINgGHgqzSN\",\"code\":\"CS004\",\"name\":\"Weight\",\"description\":\"The weight of the case in kg at the time of diagnosis\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"qDkgAbB5Jlk.pe\":{\"uid\":\"pe\",\"name\":\"Period\",\"dimensionType\":\"PERIOD\"},\"JuTpJ2Ywq5b\":{\"uid\":\"JuTpJ2Ywq5b\",\"name\":\"Age of LLINs\",\"description\":\"The age of the LLINs within a cases household in years\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"qDkgAbB5Jlk.hYyB7FUS5eR.fazCI2ygYkq\":{\"uid\":\"fazCI2ygYkq\",\"name\":\"Case detection\",\"description\":\"Determines the method that was used to detect the case\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"COUNT\",\"totalAggregationType\":\"SUM\"},\"SepVHxunjMN\":{\"uid\":\"SepVHxunjMN\",\"code\":\"PASSIVE\",\"name\":\"Passive\"},\"fazCI2ygYkq\":{\"uid\":\"fazCI2ygYkq\",\"name\":\"Case detection\",\"description\":\"Determines the method that was used to detect the case\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"COUNT\",\"totalAggregationType\":\"SUM\"},\"rzhHSqK3lQq\":{\"uid\":\"rzhHSqK3lQq\",\"name\":\"Fever\",\"description\":\"Does the case have fever or not\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"qDkgAbB5Jlk.C0aLZo75dgJ.JuTpJ2Ywq5b\":{\"uid\":\"JuTpJ2Ywq5b\",\"name\":\"Age of LLINs\",\"description\":\"The age of the LLINs within a cases household in years\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"LAST_YEAR\":{\"name\":\"Last year\"},\"C0aLZo75dgJ\":{\"uid\":\"C0aLZo75dgJ\",\"name\":\"Household investigation\",\"description\":\"Nearby household investigations occur when an index case is identified within a specific geographical area.\"},\"qDkgAbB5Jlk.ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"B6TnnFMgmCk\":{\"uid\":\"B6TnnFMgmCk\",\"name\":\"Age (years)\",\"description\":\"Age in years\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"2022\":{\"uid\":\"2022\",\"code\":\"2022\",\"name\":\"2022\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-01-01T00:00:00.000\",\"endDate\":\"2022-12-31T00:00:00.000\"},\"2021\":{\"uid\":\"2021\",\"code\":\"2021\",\"name\":\"2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-01-01T00:00:00.000\",\"endDate\":\"2021-12-31T00:00:00.000\"},\"2020\":{\"uid\":\"2020\",\"code\":\"2020\",\"name\":\"2020\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2020-01-01T00:00:00.000\",\"endDate\":\"2020-12-31T00:00:00.000\"},\"LAST_5_YEARS\":{\"name\":\"Last 5 years\"},\"hYyB7FUS5eR\":{\"uid\":\"hYyB7FUS5eR\",\"name\":\"Diagnosis & treatment\",\"description\":\"This stage is used to identify initial diagnosis and treatment. This includes the method of case detection, information about the case include travel history, method of diagnosis, malaria species type and treatment details. \"},\"fdc6uOvgoji\":{\"uid\":\"fdc6uOvgoji\",\"code\":\"OU_193190\",\"name\":\"Bombali\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"2019\":{\"uid\":\"2019\",\"code\":\"2019\",\"name\":\"2019\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2019-01-01T00:00:00.000\",\"endDate\":\"2019-12-31T00:00:00.000\"},\"2018\":{\"uid\":\"2018\",\"code\":\"2018\",\"name\":\"2018\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2018-01-01T00:00:00.000\",\"endDate\":\"2018-12-31T00:00:00.000\"},\"pe\":{\"uid\":\"pe\",\"name\":\"Period\",\"dimensionType\":\"PERIOD\"},\"qDkgAbB5Jlk\":{\"uid\":\"qDkgAbB5Jlk\",\"name\":\"Malaria case diagnosis, treatment and investigation\",\"description\":\"All cases in an elimination setting should be registered in this program. Includes relevant case identifiers/details including the ID, Name, Index, Age, Gender, Location,etc..\"},\"qDkgAbB5Jlk.enrollmentdate\":{\"name\":\"Enrollment date\",\"dimensionType\":\"PERIOD\"},\"wYTF0YCHMWr\":{\"uid\":\"wYTF0YCHMWr\",\"name\":\"Case investigation & classification\",\"description\":\"This includes the investigation of the index case (including the confirmation of symptoms, previous malaria history, LLIN usage details, IRS details), and the summary of the results for the case investigation including the final case classification (both the species type and the case classification). \"},\"fa1IdKtq4VX\":{\"uid\":\"fa1IdKtq4VX\",\"code\":\"REACTIVE\",\"name\":\"Reactive (ACD)\"},\"qDkgAbB5Jlk.ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"B6TnnFMgmCk\":[],\"BiTsLcJQ95V\":[],\"GyJHQUWZ9Rl\":[],\"JINgGHgqzSN\":[],\"ou\":[\"ImspTQPwCqd\"],\"flGbXLXCrEo\":[],\"Z1rLc1rVHK8\":[],\"JuTpJ2Ywq5b\":[],\"CklPZdOd6H1\":[\"AZK4rjJCss5\",\"UrUdMteQzlT\"],\"h5FuguPFF2j\":[],\"spkM2E9dn2J\":[\"yyeQNBfmO7g\",\"CNnT7FC710W\",\"wuS7cVSEiYA\",\"ALoq1vKJCDr\",\"OvCy05DV6kt\",\"aslBaQPVe9V\",\"rpzJ5jGkUAn\",\"LMzYJEbDEN6\",\"mQ5FOz8JXKs\",\"TRETd1l7n1N\",\"xvYNdt7dLiM\",\"CzdkfAxkAqe\",\"LoTxSO186BO\",\"omWzNDmT2t7\",\"zTtjy8I0bcu\",\"FXk1MDI7CEJ\",\"GGWWOucyQ5L\",\"cc1JEMv0suu\",\"t0fFxYw3Cg4\",\"uM1WgdIueNA\",\"dUEKKaPFcVU\",\"zsS0Xx2iUV6\",\"MdSOAa4C4gW\",\"fEV7BkjJi8V\",\"zITeQ1j7Jmz\",\"RNUEujTD4AN\",\"WIleZf4Cua4\",\"vuiZtzbuwWx\",\"Yf0Gb9nZiQ1\",\"ga22tvzYHEZ\",\"k9qiH4Z3K6m\",\"ZqOOqkOV8Zm\",\"JUY113J4COL\",\"AlKNJVD0Bqv\",\"cGLoDkT864j\",\"OIXRi2caf6J\",\"CIYwznedTto\",\"phN4setkIfq\",\"l8WR0m3GGuB\",\"JqPKssESKSC\",\"zonuQ6g4FFh\",\"pQhtDfYHXlQ\",\"bANs6w1wFgV\",\"rBktLnj3vUY\",\"bQX37dUQTAr\",\"CQR9IijKrgo\",\"m9vCfHK0sLC\",\"Rb9W87URnVe\",\"e4CswJZAFBR\",\"LRjLr9oMe1M\",\"Cy4TaW1hskg\",\"VnmVMbf4mwL\",\"Rx05JdBEHIW\",\"WBvTizbhaXP\",\"iwk6djvOBNV\",\"tLn4hW3TbNZ\",\"DEO4vDvhNEv\",\"C5DXQidiMMc\",\"ccs1kikyZS3\",\"Scdk35fgY12\",\"t0mq3u8SNgz\",\"PMSl473rekw\",\"MpwuGzXBpAk\",\"CedH1TzSPgO\",\"mJexWvdoaXE\",\"RkxuXQTQjxk\",\"CDBeT1lT7n2\",\"KNU7Tm8S245\",\"v16FQ3xwnGN\",\"aOX23O03bBw\",\"W4KroB1nw6P\",\"CWhuePZuC9y\",\"upfjuKBGHq9\",\"wVl5DoJmza2\",\"fQzvkEY9chs\",\"pBNkmU3hDoT\",\"otDBqUSWuzE\",\"THPEMRSnC4G\",\"MKmqLvOYWos\",\"hF9y363enrH\",\"EQULu0IwQNE\",\"I9v1TBhT3OV\",\"CxKFBwhGuJr\",\"N9bYrawJaqR\",\"riIXFPTUnZX\",\"QyJHXS44Xj9\",\"dMRNgoCtogj\",\"gDARdk8cZ3H\",\"AneyNa28ceQ\",\"ELm3SnuBHJZ\",\"Oh3CJhGeaoi\",\"l69MO3y6LuS\",\"T0gujEdp3Z6\",\"I8A7Q4zi1YI\",\"TfyHeFLDOKu\",\"ZyGPejjzvGD\",\"OC0K30ETDLD\",\"FmIGl5AnbxN\",\"ALX1BnV0GrW\",\"S3Dt4ozhM8X\",\"eGQJGkiLamm\",\"vzzPNV6Wu0J\",\"AObPqV4cHPb\",\"kPQes5oG21J\",\"bEj6P1jqHje\",\"UXyOlL9FJ5o\",\"tiwJrxfBoHT\",\"ecANXpkkcPT\",\"VQkdjFxCLNH\",\"QIjTIxTedos\",\"etZCdyFxz4w\",\"H65niFKFuSs\",\"JwslMKjECF2\",\"IqyWsh1pbYf\",\"JWIEjkUmsWH\",\"UJppzPKIQRv\",\"BFMEIXmaqFE\",\"i0Dl3gB8WuY\",\"G9rNnfnVNcB\",\"mCnaSMEODSz\",\"LjRX17TMcTX\",\"SN9NeGsvfmM\",\"CkE4sCvC7zj\",\"THKtWeVTuBk\",\"PFq4nWHt0fM\",\"LfjyJpiu8dL\",\"p0vsNlHuo7N\",\"XQZko5dUFGU\",\"SXD2EhrNaQu\",\"M2XM0PR40oH\",\"IyBPcxO7hfB\",\"cxvpqSjkTjP\",\"kaf9448wuv0\",\"ApCSe2JdIUw\",\"KtR12m8FoT0\",\"Qtp6HW63yqV\",\"nOMNxq2fHGq\",\"C2Ws5NctBqi\",\"BpqJwhPqI9O\",\"V1nDCD6QvPs\",\"HScqQPe1X9u\",\"RmjKEjs388f\",\"jQ3mYwytyZn\",\"sK6lzdZiwIg\",\"nDTKZYmGEvT\",\"EABP62Ce29b\",\"QT9Erxe7UaX\",\"UmW7wmw0AX9\",\"lCww3l79Wem\",\"q36uKrRjq1P\",\"MBVq67Tm1wK\",\"OvzdfV1qrvP\",\"ElBqHdoLnsc\",\"e4HCJJlYOQP\",\"rbvEOlaNkUU\",\"LRsQdDERp1f\",\"ztgG5fQPur9\",\"fg3tUp5fFH1\",\"lBZHFe8qxEL\",\"Y2ze1UBngud\",\"UzC7hScynz0\",\"xsNVICc3jPD\",\"ibFgvQscr1i\",\"jmZqHjwAKxJ\",\"KGoEICL7PmU\",\"JpY27WXUqOI\",\"yxbHuzqF6VS\",\"eHKdzDVgEuj\",\"xuUrPK5b7MP\",\"Publ7A7E6r3\",\"Lyu0GDGZLU3\",\"ovWqm6wr1dP\",\"UvnjescFIbU\",\"hUQDnRc6BKw\",\"nTQRDVcTHr0\",\"oMLQaRXOs1B\",\"mTmjaDWUfDG\",\"NlVuvr5WbKy\",\"ThLyMbT1IvD\",\"zooDgQrnVkm\",\"D9h2axpYFx9\",\"a5ayhZAtUe4\",\"xF48L1VlFrZ\",\"gSPpYw9P7YO\",\"lufRQXPgcJ8\",\"CCzBKhjSPFo\",\"uJH3wMNmMNO\",\"x4Ohep6Q85T\",\"rJTXccXhtTG\",\"hIGURTVsclf\",\"L1FmwJC0u3z\",\"w0S8ngASwmq\",\"CysWbM6JFgj\",\"AMFu6DFAqll\",\"p8Tgra7YJ7h\",\"sPFJiL1BLTy\",\"QtjohlzA8cl\",\"Oprr3by24Zt\",\"rXsByduwzcw\",\"loTOnrxGmCv\",\"Kjon60bpcC4\",\"kdX0FFam8Vz\",\"Jl2tnw6dutF\",\"sQNVDVNINvY\",\"JYfvnvIzM84\",\"OozIOFXlUQB\",\"vMAK8GtCXzE\",\"bhfjgX8aEJQ\",\"SXeG3KaIkU8\",\"qpv7jbqwNTN\",\"ELZXJ7i1DKL\",\"IfwaYvRaIhp\",\"kNow7wcQT6z\",\"HafX2zWjutb\",\"ban72xOWClE\",\"ZcaE9JG9xrr\",\"LuMJaVKadYM\",\"jLIOTZIi0Ou\",\"yxvBpzHn9VN\",\"hB9kKEo5Jav\",\"lGIM5L1ldVZ\",\"txfMqMjfmGK\",\"HOiHQKzyA2h\",\"roEWVrnX17w\",\"vjJWFh1j9U5\",\"zJCV30f9Pix\",\"uNt8kKM8azp\",\"fmGjlRnf0AW\",\"vPk3xrZvimA\",\"xIvocxUNJvn\",\"aGozKfvhGv6\",\"NV1MNzAfPWE\",\"E68TvHpnyp5\",\"qCmj8zWn2RQ\",\"ZOGqtsOOfdP\",\"JwhF38ZDa7Y\",\"RvhGvhkGceD\",\"CzJoxdewhsm\",\"WrnwBUl0Vzt\",\"yqQ0IiG9maE\",\"C3Mf3a5OJa3\",\"CndbUcexjyJ\",\"VEulszgYUL2\",\"sgvVI1jilCg\",\"oZPItrH57Zf\",\"hbiFi85xO2g\",\"lJBsaRbZLZP\",\"WPR1XicphAd\",\"r15gweUYYrk\"],\"pe\":[\"2018\",\"2019\",\"2020\",\"2021\",\"2022\"],\"fazCI2ygYkq\":[\"SepVHxunjMN\",\"fa1IdKtq4VX\",\"xod2M9f6Jgo\"],\"rzhHSqK3lQq\":[],\"bJeK4FaRKDS\":[],\"vTKipVM0GsX\":[],\"aW66s2QSosT\":[],\"TfdH5KvFmMy\":[]}}";
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
