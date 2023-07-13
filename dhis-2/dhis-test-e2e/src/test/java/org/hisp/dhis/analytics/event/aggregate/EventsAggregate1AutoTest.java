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
package org.hisp.dhis.analytics.event.aggregate;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.actions.analytics.AnalyticsEventActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/events/aggregate" endpoint. */
public class EventsAggregate1AutoTest extends AnalyticsApiTest {
  private final AnalyticsEventActions actions = new AnalyticsEventActions();

  @Test
  public void queryActiveStatusCasesByGenderLast12Months() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=A03MvHHogjR")
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add(
                "dimension=pe:LAST_12_MONTHS,cejWyOfXge6,ou:O6uvpzGd5pu;fdc6uOvgoji;lc3eMKXaEfw;jUb8gELQApl;PMa2VCrupOd;kJq2mPyFEHo;qhqAxPSTUXp;Vth0fbpFcsO;jmIPBj66vD6;TEQlaapDQoK;bL4ooGhyHRQ;eIQbndfxQMb;at6UHUQatSo")
            .add("programStatus=ACTIVE")
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);
    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(4)))
        .body("rows", hasSize(equalTo(156)))
        .body("height", equalTo(156))
        .body("width", equalTo(4))
        .body("headerWidth", equalTo(4));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"202208\":{\"name\":\"August 2022\"},\"202209\":{\"name\":\"September 2022\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"202305\":{\"name\":\"May 2023\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"202207\":{\"name\":\"July 2022\"},\"202306\":{\"name\":\"June 2023\"},\"202303\":{\"name\":\"March 2023\"},\"202304\":{\"name\":\"April 2023\"},\"202301\":{\"name\":\"January 2023\"},\"202302\":{\"name\":\"February 2023\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"202211\":{\"name\":\"November 2022\"},\"202212\":{\"name\":\"December 2022\"},\"202210\":{\"name\":\"October 2022\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"rBvjJYbMCVx\":{\"code\":\"Male\",\"name\":\"Male\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"ou\":{\"name\":\"Organisation unit\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"cejWyOfXge6\":{\"name\":\"Gender\"},\"pe\":{\"name\":\"Period\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"}},\"dimensions\":{\"pe\":[\"202207\",\"202208\",\"202209\",\"202210\",\"202211\",\"202212\",\"202301\",\"202302\",\"202303\",\"202304\",\"202305\",\"202306\"],\"ou\":[\"O6uvpzGd5pu\",\"fdc6uOvgoji\",\"lc3eMKXaEfw\",\"jUb8gELQApl\",\"PMa2VCrupOd\",\"kJq2mPyFEHo\",\"qhqAxPSTUXp\",\"Vth0fbpFcsO\",\"jmIPBj66vD6\",\"TEQlaapDQoK\",\"bL4ooGhyHRQ\",\"eIQbndfxQMb\",\"at6UHUQatSo\"],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, 0, List.of("Male", "202210", "TEQlaapDQoK", "51"));
    validateRow(response, 1, List.of("Male", "202212", "at6UHUQatSo", "46"));
    validateRow(response, 2, List.of("Male", "202212", "kJq2mPyFEHo", "45"));
    validateRow(response, 3, List.of("Male", "202208", "TEQlaapDQoK", "45"));
    validateRow(response, 4, List.of("Male", "202207", "kJq2mPyFEHo", "43"));
    validateRow(response, 5, List.of("Male", "202210", "kJq2mPyFEHo", "41"));
    validateRow(response, 6, List.of("Male", "202212", "O6uvpzGd5pu", "39"));
    validateRow(response, 7, List.of("Male", "202209", "O6uvpzGd5pu", "37"));
    validateRow(response, 8, List.of("Male", "202207", "O6uvpzGd5pu", "37"));
    validateRow(response, 9, List.of("Male", "202211", "O6uvpzGd5pu", "37"));
    validateRow(response, 10, List.of("Male", "202211", "kJq2mPyFEHo", "36"));
    validateRow(response, 11, List.of("Male", "202208", "O6uvpzGd5pu", "35"));
    validateRow(response, 12, List.of("Male", "202208", "kJq2mPyFEHo", "35"));
    validateRow(response, 13, List.of("Male", "202207", "at6UHUQatSo", "35"));
    validateRow(response, 14, List.of("Male", "202207", "TEQlaapDQoK", "35"));
    validateRow(response, 15, List.of("Male", "202210", "eIQbndfxQMb", "35"));
    validateRow(response, 16, List.of("Male", "202211", "TEQlaapDQoK", "34"));
    validateRow(response, 17, List.of("Male", "202209", "kJq2mPyFEHo", "33"));
    validateRow(response, 18, List.of("Male", "202210", "O6uvpzGd5pu", "32"));
    validateRow(response, 19, List.of("Male", "202209", "eIQbndfxQMb", "31"));
    validateRow(response, 20, List.of("Male", "202209", "TEQlaapDQoK", "31"));
    validateRow(response, 21, List.of("Male", "202211", "eIQbndfxQMb", "30"));
    validateRow(response, 22, List.of("Male", "202210", "jmIPBj66vD6", "30"));
    validateRow(response, 23, List.of("Male", "202207", "Vth0fbpFcsO", "29"));
    validateRow(response, 24, List.of("Male", "202207", "bL4ooGhyHRQ", "28"));
    validateRow(response, 25, List.of("Male", "202208", "jmIPBj66vD6", "28"));
    validateRow(response, 26, List.of("Male", "202208", "eIQbndfxQMb", "28"));
    validateRow(response, 27, List.of("Male", "202208", "at6UHUQatSo", "28"));
    validateRow(response, 28, List.of("Male", "202209", "fdc6uOvgoji", "27"));
    validateRow(response, 29, List.of("Male", "202211", "jmIPBj66vD6", "27"));
    validateRow(response, 30, List.of("Male", "202212", "jUb8gELQApl", "27"));
    validateRow(response, 31, List.of("Male", "202209", "Vth0fbpFcsO", "27"));
    validateRow(response, 32, List.of("Male", "202208", "Vth0fbpFcsO", "27"));
    validateRow(response, 33, List.of("Male", "202210", "at6UHUQatSo", "26"));
    validateRow(response, 34, List.of("Male", "202208", "jUb8gELQApl", "26"));
    validateRow(response, 35, List.of("Male", "202208", "qhqAxPSTUXp", "26"));
    validateRow(response, 36, List.of("Male", "202211", "fdc6uOvgoji", "25"));
    validateRow(response, 37, List.of("Male", "202212", "eIQbndfxQMb", "25"));
    validateRow(response, 38, List.of("Male", "202210", "qhqAxPSTUXp", "25"));
    validateRow(response, 39, List.of("Male", "202211", "at6UHUQatSo", "25"));
    validateRow(response, 40, List.of("Male", "202207", "jmIPBj66vD6", "25"));
    validateRow(response, 41, List.of("Male", "202212", "fdc6uOvgoji", "24"));
    validateRow(response, 42, List.of("Male", "202210", "Vth0fbpFcsO", "24"));
    validateRow(response, 43, List.of("Male", "202212", "qhqAxPSTUXp", "24"));
    validateRow(response, 44, List.of("Male", "202207", "eIQbndfxQMb", "24"));
    validateRow(response, 45, List.of("Male", "202209", "jmIPBj66vD6", "24"));
    validateRow(response, 46, List.of("Male", "202209", "at6UHUQatSo", "24"));
    validateRow(response, 47, List.of("Male", "202207", "fdc6uOvgoji", "23"));
    validateRow(response, 48, List.of("Male", "202212", "PMa2VCrupOd", "23"));
    validateRow(response, 49, List.of("Male", "202210", "bL4ooGhyHRQ", "23"));
    validateRow(response, 50, List.of("Male", "202208", "fdc6uOvgoji", "23"));
    validateRow(response, 51, List.of("Male", "202212", "lc3eMKXaEfw", "23"));
    validateRow(response, 52, List.of("Male", "202211", "jUb8gELQApl", "22"));
    validateRow(response, 53, List.of("Male", "202212", "jmIPBj66vD6", "22"));
    validateRow(response, 54, List.of("Male", "202211", "qhqAxPSTUXp", "22"));
    validateRow(response, 55, List.of("Male", "202212", "TEQlaapDQoK", "20"));
    validateRow(response, 56, List.of("Male", "202211", "bL4ooGhyHRQ", "20"));
    validateRow(response, 57, List.of("Male", "202210", "fdc6uOvgoji", "20"));
    validateRow(response, 58, List.of("Male", "202208", "bL4ooGhyHRQ", "20"));
    validateRow(response, 59, List.of("Male", "202207", "jUb8gELQApl", "20"));
    validateRow(response, 60, List.of("Male", "202208", "PMa2VCrupOd", "19"));
    validateRow(response, 61, List.of("Male", "202211", "Vth0fbpFcsO", "19"));
    validateRow(response, 62, List.of("Male", "202212", "Vth0fbpFcsO", "19"));
    validateRow(response, 63, List.of("Male", "202209", "jUb8gELQApl", "19"));
    validateRow(response, 64, List.of("Male", "202209", "qhqAxPSTUXp", "18"));
    validateRow(response, 65, List.of("Male", "202209", "bL4ooGhyHRQ", "18"));
    validateRow(response, 66, List.of("Male", "202209", "PMa2VCrupOd", "18"));
    validateRow(response, 67, List.of("Male", "202212", "bL4ooGhyHRQ", "17"));
    validateRow(response, 68, List.of("Male", "202211", "lc3eMKXaEfw", "17"));
    validateRow(response, 69, List.of("Male", "202210", "PMa2VCrupOd", "17"));
    validateRow(response, 70, List.of("Male", "202207", "lc3eMKXaEfw", "16"));
    validateRow(response, 71, List.of("Male", "202208", "lc3eMKXaEfw", "15"));
    validateRow(response, 72, List.of("Male", "202210", "jUb8gELQApl", "14"));
    validateRow(response, 73, List.of("Male", "202210", "lc3eMKXaEfw", "13"));
    validateRow(response, 74, List.of("Male", "202207", "PMa2VCrupOd", "11"));
    validateRow(response, 75, List.of("Male", "202207", "qhqAxPSTUXp", "10"));
    validateRow(response, 76, List.of("Male", "202209", "lc3eMKXaEfw", "9"));
    validateRow(response, 77, List.of("Male", "202211", "PMa2VCrupOd", "9"));
    validateRow(response, 78, List.of("Female", "202209", "kJq2mPyFEHo", "47"));
    validateRow(response, 79, List.of("Female", "202212", "O6uvpzGd5pu", "46"));
    validateRow(response, 80, List.of("Female", "202211", "TEQlaapDQoK", "44"));
    validateRow(response, 81, List.of("Female", "202207", "TEQlaapDQoK", "39"));
    validateRow(response, 82, List.of("Female", "202208", "at6UHUQatSo", "39"));
    validateRow(response, 83, List.of("Female", "202211", "kJq2mPyFEHo", "38"));
    validateRow(response, 84, List.of("Female", "202208", "O6uvpzGd5pu", "37"));
    validateRow(response, 85, List.of("Female", "202208", "TEQlaapDQoK", "36"));
    validateRow(response, 86, List.of("Female", "202209", "TEQlaapDQoK", "35"));
    validateRow(response, 87, List.of("Female", "202210", "kJq2mPyFEHo", "35"));
    validateRow(response, 88, List.of("Female", "202208", "jmIPBj66vD6", "35"));
    validateRow(response, 89, List.of("Female", "202207", "O6uvpzGd5pu", "35"));
    validateRow(response, 90, List.of("Female", "202210", "at6UHUQatSo", "33"));
    validateRow(response, 91, List.of("Female", "202211", "O6uvpzGd5pu", "33"));
    validateRow(response, 92, List.of("Female", "202209", "Vth0fbpFcsO", "32"));
    validateRow(response, 93, List.of("Female", "202210", "fdc6uOvgoji", "32"));
    validateRow(response, 94, List.of("Female", "202208", "jUb8gELQApl", "32"));
    validateRow(response, 95, List.of("Female", "202210", "TEQlaapDQoK", "31"));
    validateRow(response, 96, List.of("Female", "202210", "O6uvpzGd5pu", "31"));
    validateRow(response, 97, List.of("Female", "202208", "kJq2mPyFEHo", "31"));
    validateRow(response, 98, List.of("Female", "202212", "fdc6uOvgoji", "30"));
    validateRow(response, 99, List.of("Female", "202211", "eIQbndfxQMb", "30"));
    validateRow(response, 100, List.of("Female", "202210", "jmIPBj66vD6", "30"));
    validateRow(response, 101, List.of("Female", "202212", "at6UHUQatSo", "30"));
    validateRow(response, 102, List.of("Female", "202207", "jmIPBj66vD6", "30"));
    validateRow(response, 103, List.of("Female", "202212", "TEQlaapDQoK", "29"));
    validateRow(response, 104, List.of("Female", "202209", "fdc6uOvgoji", "29"));
    validateRow(response, 105, List.of("Female", "202212", "kJq2mPyFEHo", "28"));
    validateRow(response, 106, List.of("Female", "202211", "fdc6uOvgoji", "28"));
    validateRow(response, 107, List.of("Female", "202210", "eIQbndfxQMb", "28"));
    validateRow(response, 108, List.of("Female", "202207", "kJq2mPyFEHo", "28"));
    validateRow(response, 109, List.of("Female", "202207", "fdc6uOvgoji", "27"));
    validateRow(response, 110, List.of("Female", "202207", "at6UHUQatSo", "27"));
    validateRow(response, 111, List.of("Female", "202208", "eIQbndfxQMb", "26"));
    validateRow(response, 112, List.of("Female", "202211", "qhqAxPSTUXp", "26"));
    validateRow(response, 113, List.of("Female", "202207", "Vth0fbpFcsO", "26"));
    validateRow(response, 114, List.of("Female", "202209", "O6uvpzGd5pu", "26"));
    validateRow(response, 115, List.of("Female", "202211", "at6UHUQatSo", "25"));
    validateRow(response, 116, List.of("Female", "202208", "bL4ooGhyHRQ", "25"));
    validateRow(response, 117, List.of("Female", "202212", "jmIPBj66vD6", "24"));
    validateRow(response, 118, List.of("Female", "202209", "qhqAxPSTUXp", "24"));
    validateRow(response, 119, List.of("Female", "202207", "jUb8gELQApl", "24"));
    validateRow(response, 120, List.of("Female", "202209", "eIQbndfxQMb", "24"));
    validateRow(response, 121, List.of("Female", "202207", "eIQbndfxQMb", "24"));
    validateRow(response, 122, List.of("Female", "202210", "Vth0fbpFcsO", "24"));
    validateRow(response, 123, List.of("Female", "202211", "jmIPBj66vD6", "24"));
    validateRow(response, 124, List.of("Female", "202211", "Vth0fbpFcsO", "23"));
    validateRow(response, 125, List.of("Female", "202209", "jmIPBj66vD6", "23"));
    validateRow(response, 126, List.of("Female", "202211", "bL4ooGhyHRQ", "23"));
    validateRow(response, 127, List.of("Female", "202208", "Vth0fbpFcsO", "23"));
    validateRow(response, 128, List.of("Female", "202208", "fdc6uOvgoji", "23"));
    validateRow(response, 129, List.of("Female", "202210", "PMa2VCrupOd", "22"));
    validateRow(response, 130, List.of("Female", "202209", "at6UHUQatSo", "20"));
    validateRow(response, 131, List.of("Female", "202210", "bL4ooGhyHRQ", "20"));
    validateRow(response, 132, List.of("Female", "202208", "PMa2VCrupOd", "20"));
    validateRow(response, 133, List.of("Female", "202210", "qhqAxPSTUXp", "19"));
    validateRow(response, 134, List.of("Female", "202207", "bL4ooGhyHRQ", "19"));
    validateRow(response, 135, List.of("Female", "202212", "Vth0fbpFcsO", "19"));
    validateRow(response, 136, List.of("Female", "202212", "jUb8gELQApl", "19"));
    validateRow(response, 137, List.of("Female", "202208", "qhqAxPSTUXp", "19"));
    validateRow(response, 138, List.of("Female", "202209", "bL4ooGhyHRQ", "18"));
    validateRow(response, 139, List.of("Female", "202209", "jUb8gELQApl", "18"));
    validateRow(response, 140, List.of("Female", "202210", "jUb8gELQApl", "18"));
    validateRow(response, 141, List.of("Female", "202211", "PMa2VCrupOd", "18"));
    validateRow(response, 142, List.of("Female", "202212", "PMa2VCrupOd", "18"));
    validateRow(response, 143, List.of("Female", "202212", "eIQbndfxQMb", "17"));
    validateRow(response, 144, List.of("Female", "202209", "lc3eMKXaEfw", "16"));
    validateRow(response, 145, List.of("Female", "202207", "qhqAxPSTUXp", "16"));
    validateRow(response, 146, List.of("Female", "202209", "PMa2VCrupOd", "15"));
    validateRow(response, 147, List.of("Female", "202212", "qhqAxPSTUXp", "15"));
    validateRow(response, 148, List.of("Female", "202210", "lc3eMKXaEfw", "15"));
    validateRow(response, 149, List.of("Female", "202212", "bL4ooGhyHRQ", "15"));
    validateRow(response, 150, List.of("Female", "202211", "lc3eMKXaEfw", "14"));
    validateRow(response, 151, List.of("Female", "202207", "lc3eMKXaEfw", "13"));
    validateRow(response, 152, List.of("Female", "202207", "PMa2VCrupOd", "13"));
    validateRow(response, 153, List.of("Female", "202212", "lc3eMKXaEfw", "12"));
    validateRow(response, 154, List.of("Female", "202208", "lc3eMKXaEfw", "12"));
    validateRow(response, 155, List.of("Female", "202211", "jUb8gELQApl", "9"));
  }

  @Test
  public void queryArvAtBirthByGenderActiveCasesLast12Months() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=A03MvHHogjR")
            .add("displayProperty=NAME")
            .add("eventStatus=ACTIVE")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add(
                "dimension=pe:LAST_12_MONTHS,A03MvHHogjR.cejWyOfXge6,A03MvHHogjR.wQLfBvPrXqq,ou:jUb8gELQApl;TEQlaapDQoK;eIQbndfxQMb;Vth0fbpFcsO;PMa2VCrupOd;O6uvpzGd5pu;bL4ooGhyHRQ;kJq2mPyFEHo;fdc6uOvgoji;at6UHUQatSo;lc3eMKXaEfw;qhqAxPSTUXp;jmIPBj66vD6")
            .add("programStatus=ACTIVE")
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);
    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(5)))
        .body("rows", hasSize(equalTo(313)))
        .body("height", equalTo(313))
        .body("width", equalTo(5))
        .body("headerWidth", equalTo(5));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"202208\":{\"name\":\"August 2022\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"202209\":{\"name\":\"September 2022\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"202305\":{\"name\":\"May 2023\"},\"202207\":{\"name\":\"July 2022\"},\"202306\":{\"name\":\"June 2023\"},\"202303\":{\"name\":\"March 2023\"},\"202304\":{\"name\":\"April 2023\"},\"202301\":{\"name\":\"January 2023\"},\"202302\":{\"name\":\"February 2023\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"202211\":{\"name\":\"November 2022\"},\"ww8JVblo4SI\":{\"code\":\"Others\",\"name\":\"Others\"},\"202212\":{\"name\":\"December 2022\"},\"Cd0gtHGmlwS\":{\"code\":\"NVP only\",\"name\":\"NVP only\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"202210\":{\"name\":\"October 2022\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"rBvjJYbMCVx\":{\"code\":\"Male\",\"name\":\"Male\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"ou\":{\"name\":\"Organisation unit\"},\"wQLfBvPrXqq\":{\"name\":\"MCH ARV at birth\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"cejWyOfXge6\":{\"name\":\"Gender\"},\"pe\":{\"name\":\"Period\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"}},\"dimensions\":{\"pe\":[\"202207\",\"202208\",\"202209\",\"202210\",\"202211\",\"202212\",\"202301\",\"202302\",\"202303\",\"202304\",\"202305\",\"202306\"],\"ou\":[\"jUb8gELQApl\",\"TEQlaapDQoK\",\"eIQbndfxQMb\",\"Vth0fbpFcsO\",\"PMa2VCrupOd\",\"O6uvpzGd5pu\",\"bL4ooGhyHRQ\",\"kJq2mPyFEHo\",\"fdc6uOvgoji\",\"at6UHUQatSo\",\"lc3eMKXaEfw\",\"qhqAxPSTUXp\",\"jmIPBj66vD6\"],\"wQLfBvPrXqq\":[\"Cd0gtHGmlwS\",\"ww8JVblo4SI\"],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "wQLfBvPrXqq", "MCH ARV at birth", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 4, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, 0, List.of("Male", "NVP only", "TEQlaapDQoK", "202210", "32"));
    validateRow(response, 1, List.of("Male", "NVP only", "kJq2mPyFEHo", "202212", "31"));
    validateRow(response, 2, List.of("Male", "Others", "O6uvpzGd5pu", "202211", "26"));
    validateRow(response, 3, List.of("Male", "NVP only", "at6UHUQatSo", "202212", "23"));
    validateRow(response, 4, List.of("Male", "Others", "at6UHUQatSo", "202212", "23"));
    validateRow(response, 5, List.of("Male", "Others", "TEQlaapDQoK", "202208", "23"));
    validateRow(response, 6, List.of("Male", "NVP only", "kJq2mPyFEHo", "202207", "22"));
    validateRow(response, 7, List.of("Male", "NVP only", "TEQlaapDQoK", "202208", "22"));
    validateRow(response, 8, List.of("Male", "Others", "kJq2mPyFEHo", "202211", "21"));
    validateRow(response, 9, List.of("Male", "Others", "at6UHUQatSo", "202207", "21"));
    validateRow(response, 10, List.of("Male", "NVP only", "kJq2mPyFEHo", "202210", "21"));
    validateRow(response, 11, List.of("Male", "Others", "kJq2mPyFEHo", "202207", "21"));
    validateRow(response, 12, List.of("Male", "NVP only", "O6uvpzGd5pu", "202212", "21"));
    validateRow(response, 13, List.of("Male", "Others", "kJq2mPyFEHo", "202208", "20"));
    validateRow(response, 14, List.of("Male", "Others", "kJq2mPyFEHo", "202210", "20"));
    validateRow(response, 15, List.of("Male", "Others", "TEQlaapDQoK", "202209", "19"));
    validateRow(response, 16, List.of("Male", "NVP only", "O6uvpzGd5pu", "202207", "19"));
    validateRow(response, 17, List.of("Male", "Others", "kJq2mPyFEHo", "202209", "19"));
    validateRow(response, 18, List.of("Male", "Others", "TEQlaapDQoK", "202210", "19"));
    validateRow(response, 19, List.of("Male", "NVP only", "O6uvpzGd5pu", "202209", "19"));
    validateRow(response, 20, List.of("Male", "NVP only", "eIQbndfxQMb", "202210", "19"));
    validateRow(response, 21, List.of("Male", "Others", "TEQlaapDQoK", "202207", "19"));
    validateRow(response, 22, List.of("Male", "Others", "Vth0fbpFcsO", "202209", "19"));
    validateRow(response, 23, List.of("Male", "Others", "O6uvpzGd5pu", "202207", "18"));
    validateRow(response, 24, List.of("Male", "Others", "O6uvpzGd5pu", "202212", "18"));
    validateRow(response, 25, List.of("Male", "NVP only", "O6uvpzGd5pu", "202208", "18"));
    validateRow(response, 26, List.of("Male", "NVP only", "jmIPBj66vD6", "202207", "18"));
    validateRow(response, 27, List.of("Male", "NVP only", "eIQbndfxQMb", "202208", "18"));
    validateRow(response, 28, List.of("Male", "Others", "TEQlaapDQoK", "202211", "17"));
    validateRow(response, 29, List.of("Male", "Others", "O6uvpzGd5pu", "202210", "17"));
    validateRow(response, 30, List.of("Male", "Others", "jUb8gELQApl", "202212", "17"));
    validateRow(response, 31, List.of("Male", "NVP only", "TEQlaapDQoK", "202211", "17"));
    validateRow(response, 32, List.of("Male", "Others", "O6uvpzGd5pu", "202209", "17"));
    validateRow(response, 33, List.of("Male", "Others", "Vth0fbpFcsO", "202210", "17"));
    validateRow(response, 34, List.of("Male", "Others", "eIQbndfxQMb", "202212", "16"));
    validateRow(response, 35, List.of("Male", "Others", "eIQbndfxQMb", "202210", "16"));
    validateRow(response, 36, List.of("Male", "Others", "at6UHUQatSo", "202208", "16"));
    validateRow(response, 37, List.of("Male", "Others", "jUb8gELQApl", "202208", "16"));
    validateRow(response, 38, List.of("Male", "Others", "O6uvpzGd5pu", "202208", "16"));
    validateRow(response, 39, List.of("Male", "NVP only", "jmIPBj66vD6", "202208", "16"));
    validateRow(response, 40, List.of("Male", "NVP only", "TEQlaapDQoK", "202207", "16"));
    validateRow(response, 41, List.of("Male", "NVP only", "jmIPBj66vD6", "202211", "16"));
    validateRow(response, 42, List.of("Male", "Others", "eIQbndfxQMb", "202209", "16"));
    validateRow(response, 43, List.of("Male", "NVP only", "lc3eMKXaEfw", "202212", "16"));
    validateRow(response, 44, List.of("Male", "Others", "jmIPBj66vD6", "202210", "16"));
    validateRow(response, 45, List.of("Male", "Others", "Vth0fbpFcsO", "202207", "15"));
    validateRow(response, 46, List.of("Male", "NVP only", "Vth0fbpFcsO", "202208", "15"));
    validateRow(response, 47, List.of("Male", "NVP only", "at6UHUQatSo", "202209", "15"));
    validateRow(response, 48, List.of("Male", "NVP only", "eIQbndfxQMb", "202211", "15"));
    validateRow(response, 49, List.of("Male", "NVP only", "jUb8gELQApl", "202207", "15"));
    validateRow(response, 50, List.of("Male", "NVP only", "O6uvpzGd5pu", "202210", "15"));
    validateRow(response, 51, List.of("Male", "NVP only", "jUb8gELQApl", "202211", "15"));
    validateRow(response, 52, List.of("Male", "NVP only", "fdc6uOvgoji", "202210", "15"));
    validateRow(response, 53, List.of("Male", "NVP only", "kJq2mPyFEHo", "202208", "15"));
    validateRow(response, 54, List.of("Male", "Others", "PMa2VCrupOd", "202212", "15"));
    validateRow(response, 55, List.of("Male", "NVP only", "kJq2mPyFEHo", "202211", "15"));
    validateRow(response, 56, List.of("Male", "NVP only", "eIQbndfxQMb", "202209", "15"));
    validateRow(response, 57, List.of("Male", "Others", "eIQbndfxQMb", "202211", "15"));
    validateRow(response, 58, List.of("Male", "NVP only", "bL4ooGhyHRQ", "202207", "15"));
    validateRow(response, 59, List.of("Male", "NVP only", "fdc6uOvgoji", "202209", "14"));
    validateRow(response, 60, List.of("Male", "NVP only", "kJq2mPyFEHo", "202209", "14"));
    validateRow(response, 61, List.of("Male", "Others", "fdc6uOvgoji", "202208", "14"));
    validateRow(response, 62, List.of("Male", "Others", "at6UHUQatSo", "202211", "14"));
    validateRow(response, 63, List.of("Male", "Others", "fdc6uOvgoji", "202212", "14"));
    validateRow(response, 64, List.of("Male", "NVP only", "at6UHUQatSo", "202210", "14"));
    validateRow(response, 65, List.of("Male", "Others", "kJq2mPyFEHo", "202212", "14"));
    validateRow(response, 66, List.of("Male", "Others", "jmIPBj66vD6", "202212", "14"));
    validateRow(response, 67, List.of("Male", "NVP only", "jmIPBj66vD6", "202210", "14"));
    validateRow(response, 68, List.of("Male", "NVP only", "Vth0fbpFcsO", "202207", "14"));
    validateRow(response, 69, List.of("Male", "NVP only", "at6UHUQatSo", "202207", "14"));
    validateRow(response, 70, List.of("Male", "NVP only", "bL4ooGhyHRQ", "202210", "13"));
    validateRow(response, 71, List.of("Male", "NVP only", "qhqAxPSTUXp", "202210", "13"));
    validateRow(response, 72, List.of("Male", "Others", "qhqAxPSTUXp", "202208", "13"));
    validateRow(response, 73, List.of("Male", "Others", "fdc6uOvgoji", "202209", "13"));
    validateRow(response, 74, List.of("Male", "NVP only", "jmIPBj66vD6", "202209", "13"));
    validateRow(response, 75, List.of("Male", "Others", "bL4ooGhyHRQ", "202207", "13"));
    validateRow(response, 76, List.of("Male", "NVP only", "eIQbndfxQMb", "202207", "13"));
    validateRow(response, 77, List.of("Male", "NVP only", "fdc6uOvgoji", "202211", "13"));
    validateRow(response, 78, List.of("Male", "NVP only", "qhqAxPSTUXp", "202212", "13"));
    validateRow(response, 79, List.of("Male", "NVP only", "qhqAxPSTUXp", "202208", "13"));
    validateRow(response, 80, List.of("Male", "NVP only", "qhqAxPSTUXp", "202211", "13"));
    validateRow(response, 81, List.of("Male", "Others", "bL4ooGhyHRQ", "202211", "13"));
    validateRow(response, 82, List.of("Male", "Others", "jUb8gELQApl", "202209", "12"));
    validateRow(response, 83, List.of("Male", "Others", "jmIPBj66vD6", "202208", "12"));
    validateRow(response, 84, List.of("Male", "NVP only", "TEQlaapDQoK", "202209", "12"));
    validateRow(response, 85, List.of("Male", "Others", "at6UHUQatSo", "202210", "12"));
    validateRow(response, 86, List.of("Male", "Others", "Vth0fbpFcsO", "202208", "12"));
    validateRow(response, 87, List.of("Male", "Others", "qhqAxPSTUXp", "202210", "12"));
    validateRow(response, 88, List.of("Male", "NVP only", "at6UHUQatSo", "202208", "12"));
    validateRow(response, 89, List.of("Male", "NVP only", "fdc6uOvgoji", "202207", "12"));
    validateRow(response, 90, List.of("Male", "Others", "fdc6uOvgoji", "202211", "12"));
    validateRow(response, 91, List.of("Male", "NVP only", "at6UHUQatSo", "202211", "11"));
    validateRow(response, 92, List.of("Male", "Others", "jmIPBj66vD6", "202209", "11"));
    validateRow(response, 93, List.of("Male", "Others", "Vth0fbpFcsO", "202212", "11"));
    validateRow(response, 94, List.of("Male", "NVP only", "O6uvpzGd5pu", "202211", "11"));
    validateRow(response, 95, List.of("Male", "Others", "bL4ooGhyHRQ", "202209", "11"));
    validateRow(response, 96, List.of("Male", "NVP only", "Vth0fbpFcsO", "202211", "11"));
    validateRow(response, 97, List.of("Male", "Others", "jmIPBj66vD6", "202211", "11"));
    validateRow(response, 98, List.of("Male", "NVP only", "qhqAxPSTUXp", "202209", "11"));
    validateRow(response, 99, List.of("Male", "Others", "fdc6uOvgoji", "202207", "11"));
    validateRow(response, 100, List.of("Male", "NVP only", "PMa2VCrupOd", "202208", "11"));
    validateRow(response, 101, List.of("Male", "Others", "qhqAxPSTUXp", "202212", "11"));
    validateRow(response, 102, List.of("Male", "Others", "eIQbndfxQMb", "202207", "11"));
    validateRow(response, 103, List.of("Male", "Others", "lc3eMKXaEfw", "202208", "11"));
    validateRow(response, 104, List.of("Male", "Others", "TEQlaapDQoK", "202212", "10"));
    validateRow(response, 105, List.of("Male", "NVP only", "TEQlaapDQoK", "202212", "10"));
    validateRow(response, 106, List.of("Male", "Others", "eIQbndfxQMb", "202208", "10"));
    validateRow(response, 107, List.of("Male", "NVP only", "fdc6uOvgoji", "202212", "10"));
    validateRow(response, 108, List.of("Male", "Others", "bL4ooGhyHRQ", "202210", "10"));
    validateRow(response, 109, List.of("Male", "NVP only", "bL4ooGhyHRQ", "202208", "10"));
    validateRow(response, 110, List.of("Male", "Others", "bL4ooGhyHRQ", "202208", "10"));
    validateRow(response, 111, List.of("Male", "NVP only", "lc3eMKXaEfw", "202207", "10"));
    validateRow(response, 112, List.of("Male", "NVP only", "jUb8gELQApl", "202212", "10"));
    validateRow(response, 113, List.of("Male", "NVP only", "jUb8gELQApl", "202208", "10"));
    validateRow(response, 114, List.of("Male", "Others", "qhqAxPSTUXp", "202211", "9"));
    validateRow(response, 115, List.of("Male", "NVP only", "PMa2VCrupOd", "202209", "9"));
    validateRow(response, 116, List.of("Male", "NVP only", "lc3eMKXaEfw", "202211", "9"));
    validateRow(response, 117, List.of("Male", "NVP only", "fdc6uOvgoji", "202208", "9"));
    validateRow(response, 118, List.of("Male", "NVP only", "eIQbndfxQMb", "202212", "9"));
    validateRow(response, 119, List.of("Male", "Others", "PMa2VCrupOd", "202210", "9"));
    validateRow(response, 120, List.of("Male", "Others", "bL4ooGhyHRQ", "202212", "9"));
    validateRow(response, 121, List.of("Male", "Others", "at6UHUQatSo", "202209", "9"));
    validateRow(response, 122, List.of("Male", "Others", "PMa2VCrupOd", "202209", "9"));
    validateRow(response, 123, List.of("Male", "NVP only", "Vth0fbpFcsO", "202209", "8"));
    validateRow(response, 124, List.of("Male", "Others", "Vth0fbpFcsO", "202211", "8"));
    validateRow(response, 125, List.of("Male", "NVP only", "Vth0fbpFcsO", "202212", "8"));
    validateRow(response, 126, List.of("Male", "Others", "lc3eMKXaEfw", "202211", "8"));
    validateRow(response, 127, List.of("Male", "NVP only", "PMa2VCrupOd", "202212", "8"));
    validateRow(response, 128, List.of("Male", "NVP only", "jmIPBj66vD6", "202212", "8"));
    validateRow(response, 129, List.of("Male", "Others", "PMa2VCrupOd", "202208", "8"));
    validateRow(response, 130, List.of("Male", "NVP only", "bL4ooGhyHRQ", "202212", "8"));
    validateRow(response, 131, List.of("Male", "NVP only", "PMa2VCrupOd", "202210", "8"));
    validateRow(response, 132, List.of("Male", "NVP only", "jUb8gELQApl", "202209", "7"));
    validateRow(response, 133, List.of("Male", "Others", "jUb8gELQApl", "202210", "7"));
    validateRow(response, 134, List.of("Male", "Others", "lc3eMKXaEfw", "202212", "7"));
    validateRow(response, 135, List.of("Male", "Others", "PMa2VCrupOd", "202207", "7"));
    validateRow(response, 136, List.of("Male", "Others", "jUb8gELQApl", "202211", "7"));
    validateRow(response, 137, List.of("Male", "Others", "jmIPBj66vD6", "202207", "7"));
    validateRow(response, 138, List.of("Male", "NVP only", "jUb8gELQApl", "202210", "7"));
    validateRow(response, 139, List.of("Male", "NVP only", "Vth0fbpFcsO", "202210", "7"));
    validateRow(response, 140, List.of("Male", "NVP only", "lc3eMKXaEfw", "202210", "7"));
    validateRow(response, 141, List.of("Male", "NVP only", "bL4ooGhyHRQ", "202211", "7"));
    validateRow(response, 142, List.of("Male", "NVP only", "bL4ooGhyHRQ", "202209", "7"));
    validateRow(response, 143, List.of("Male", "Others", "qhqAxPSTUXp", "202207", "7"));
    validateRow(response, 144, List.of("Male", "Others", "qhqAxPSTUXp", "202209", "7"));
    validateRow(response, 145, List.of("Male", "Others", "lc3eMKXaEfw", "202210", "6"));
    validateRow(response, 146, List.of("Male", "Others", "lc3eMKXaEfw", "202207", "6"));
    validateRow(response, 147, List.of("Male", "Others", "PMa2VCrupOd", "202211", "6"));
    validateRow(response, 148, List.of("Male", "Others", "jUb8gELQApl", "202207", "5"));
    validateRow(response, 149, List.of("Male", "Others", "fdc6uOvgoji", "202210", "5"));
    validateRow(response, 150, List.of("Male", "Others", "lc3eMKXaEfw", "202209", "5"));
    validateRow(response, 151, List.of("Male", "NVP only", "lc3eMKXaEfw", "202208", "4"));
    validateRow(response, 152, List.of("Male", "NVP only", "lc3eMKXaEfw", "202209", "4"));
    validateRow(response, 153, List.of("Male", "NVP only", "PMa2VCrupOd", "202207", "4"));
    validateRow(response, 154, List.of("Male", "NVP only", "PMa2VCrupOd", "202211", "3"));
    validateRow(response, 155, List.of("Male", "NVP only", "qhqAxPSTUXp", "202207", "3"));
    validateRow(response, 156, List.of("Male", "", "O6uvpzGd5pu", "202208", "1"));
    validateRow(response, 157, List.of("Female", "Others", "kJq2mPyFEHo", "202209", "27"));
    validateRow(response, 158, List.of("Female", "Others", "O6uvpzGd5pu", "202212", "26"));
    validateRow(response, 159, List.of("Female", "NVP only", "at6UHUQatSo", "202212", "24"));
    validateRow(response, 160, List.of("Female", "Others", "TEQlaapDQoK", "202211", "23"));
    validateRow(response, 161, List.of("Female", "NVP only", "kJq2mPyFEHo", "202211", "22"));
    validateRow(response, 162, List.of("Female", "NVP only", "TEQlaapDQoK", "202208", "22"));
    validateRow(response, 163, List.of("Female", "Others", "TEQlaapDQoK", "202207", "22"));
    validateRow(response, 164, List.of("Female", "NVP only", "at6UHUQatSo", "202208", "22"));
    validateRow(response, 165, List.of("Female", "NVP only", "TEQlaapDQoK", "202211", "21"));
    validateRow(response, 166, List.of("Female", "Others", "kJq2mPyFEHo", "202210", "21"));
    validateRow(response, 167, List.of("Female", "NVP only", "O6uvpzGd5pu", "202207", "21"));
    validateRow(response, 168, List.of("Female", "Others", "fdc6uOvgoji", "202210", "20"));
    validateRow(response, 169, List.of("Female", "NVP only", "O6uvpzGd5pu", "202212", "20"));
    validateRow(response, 170, List.of("Female", "NVP only", "kJq2mPyFEHo", "202209", "20"));
    validateRow(response, 171, List.of("Female", "Others", "jmIPBj66vD6", "202208", "20"));
    validateRow(response, 172, List.of("Female", "NVP only", "kJq2mPyFEHo", "202208", "20"));
    validateRow(response, 173, List.of("Female", "Others", "jmIPBj66vD6", "202210", "19"));
    validateRow(response, 174, List.of("Female", "NVP only", "qhqAxPSTUXp", "202211", "19"));
    validateRow(response, 175, List.of("Female", "NVP only", "O6uvpzGd5pu", "202208", "19"));
    validateRow(response, 176, List.of("Female", "NVP only", "eIQbndfxQMb", "202208", "18"));
    validateRow(response, 177, List.of("Female", "Others", "O6uvpzGd5pu", "202208", "18"));
    validateRow(response, 178, List.of("Female", "Others", "at6UHUQatSo", "202210", "18"));
    validateRow(response, 179, List.of("Female", "NVP only", "fdc6uOvgoji", "202209", "18"));
    validateRow(response, 180, List.of("Female", "Others", "TEQlaapDQoK", "202209", "18"));
    validateRow(response, 181, List.of("Female", "NVP only", "O6uvpzGd5pu", "202211", "18"));
    validateRow(response, 182, List.of("Female", "Others", "jUb8gELQApl", "202208", "17"));
    validateRow(response, 183, List.of("Female", "NVP only", "TEQlaapDQoK", "202207", "17"));
    validateRow(response, 184, List.of("Female", "NVP only", "jmIPBj66vD6", "202207", "17"));
    validateRow(response, 185, List.of("Female", "NVP only", "Vth0fbpFcsO", "202209", "17"));
    validateRow(response, 186, List.of("Female", "Others", "at6UHUQatSo", "202208", "17"));
    validateRow(response, 187, List.of("Female", "NVP only", "eIQbndfxQMb", "202211", "17"));
    validateRow(response, 188, List.of("Female", "NVP only", "TEQlaapDQoK", "202209", "17"));
    validateRow(response, 189, List.of("Female", "Others", "Vth0fbpFcsO", "202210", "17"));
    validateRow(response, 190, List.of("Female", "NVP only", "at6UHUQatSo", "202207", "17"));
    validateRow(response, 191, List.of("Female", "NVP only", "O6uvpzGd5pu", "202210", "16"));
    validateRow(response, 192, List.of("Female", "NVP only", "fdc6uOvgoji", "202212", "16"));
    validateRow(response, 193, List.of("Female", "NVP only", "TEQlaapDQoK", "202212", "16"));
    validateRow(response, 194, List.of("Female", "Others", "kJq2mPyFEHo", "202212", "16"));
    validateRow(response, 195, List.of("Female", "Others", "kJq2mPyFEHo", "202211", "16"));
    validateRow(response, 196, List.of("Female", "NVP only", "Vth0fbpFcsO", "202211", "16"));
    validateRow(response, 197, List.of("Female", "Others", "TEQlaapDQoK", "202210", "16"));
    validateRow(response, 198, List.of("Female", "Others", "eIQbndfxQMb", "202210", "15"));
    validateRow(response, 199, List.of("Female", "NVP only", "O6uvpzGd5pu", "202209", "15"));
    validateRow(response, 200, List.of("Female", "Others", "PMa2VCrupOd", "202210", "15"));
    validateRow(response, 201, List.of("Female", "Others", "Vth0fbpFcsO", "202209", "15"));
    validateRow(response, 202, List.of("Female", "Others", "O6uvpzGd5pu", "202210", "15"));
    validateRow(response, 203, List.of("Female", "NVP only", "at6UHUQatSo", "202210", "15"));
    validateRow(response, 204, List.of("Female", "Others", "jUb8gELQApl", "202207", "15"));
    validateRow(response, 205, List.of("Female", "Others", "fdc6uOvgoji", "202207", "15"));
    validateRow(response, 206, List.of("Female", "Others", "O6uvpzGd5pu", "202211", "15"));
    validateRow(response, 207, List.of("Female", "NVP only", "TEQlaapDQoK", "202210", "15"));
    validateRow(response, 208, List.of("Female", "NVP only", "jUb8gELQApl", "202208", "15"));
    validateRow(response, 209, List.of("Female", "NVP only", "jmIPBj66vD6", "202208", "15"));
    validateRow(response, 210, List.of("Female", "Others", "kJq2mPyFEHo", "202207", "15"));
    validateRow(response, 211, List.of("Female", "Others", "Vth0fbpFcsO", "202207", "15"));
    validateRow(response, 212, List.of("Female", "NVP only", "at6UHUQatSo", "202211", "14"));
    validateRow(response, 213, List.of("Female", "Others", "fdc6uOvgoji", "202208", "14"));
    validateRow(response, 214, List.of("Female", "Others", "TEQlaapDQoK", "202208", "14"));
    validateRow(response, 215, List.of("Female", "NVP only", "eIQbndfxQMb", "202207", "14"));
    validateRow(response, 216, List.of("Female", "Others", "fdc6uOvgoji", "202212", "14"));
    validateRow(response, 217, List.of("Female", "NVP only", "kJq2mPyFEHo", "202210", "14"));
    validateRow(response, 218, List.of("Female", "NVP only", "fdc6uOvgoji", "202211", "14"));
    validateRow(response, 219, List.of("Female", "Others", "fdc6uOvgoji", "202211", "14"));
    validateRow(response, 220, List.of("Female", "Others", "O6uvpzGd5pu", "202207", "14"));
    validateRow(response, 221, List.of("Female", "Others", "eIQbndfxQMb", "202209", "13"));
    validateRow(response, 222, List.of("Female", "Others", "TEQlaapDQoK", "202212", "13"));
    validateRow(response, 223, List.of("Female", "Others", "jmIPBj66vD6", "202211", "13"));
    validateRow(response, 224, List.of("Female", "NVP only", "qhqAxPSTUXp", "202209", "13"));
    validateRow(response, 225, List.of("Female", "Others", "Vth0fbpFcsO", "202208", "13"));
    validateRow(response, 226, List.of("Female", "NVP only", "kJq2mPyFEHo", "202207", "13"));
    validateRow(response, 227, List.of("Female", "Others", "eIQbndfxQMb", "202211", "13"));
    validateRow(response, 228, List.of("Female", "NVP only", "eIQbndfxQMb", "202210", "13"));
    validateRow(response, 229, List.of("Female", "NVP only", "bL4ooGhyHRQ", "202208", "13"));
    validateRow(response, 230, List.of("Female", "NVP only", "jmIPBj66vD6", "202212", "13"));
    validateRow(response, 231, List.of("Female", "Others", "jmIPBj66vD6", "202207", "13"));
    validateRow(response, 232, List.of("Female", "Others", "bL4ooGhyHRQ", "202208", "12"));
    validateRow(response, 233, List.of("Female", "NVP only", "jUb8gELQApl", "202210", "12"));
    validateRow(response, 234, List.of("Female", "NVP only", "bL4ooGhyHRQ", "202211", "12"));
    validateRow(response, 235, List.of("Female", "NVP only", "fdc6uOvgoji", "202210", "12"));
    validateRow(response, 236, List.of("Female", "NVP only", "PMa2VCrupOd", "202211", "12"));
    validateRow(response, 237, List.of("Female", "NVP only", "bL4ooGhyHRQ", "202207", "12"));
    validateRow(response, 238, List.of("Female", "NVP only", "jmIPBj66vD6", "202209", "12"));
    validateRow(response, 239, List.of("Female", "NVP only", "Vth0fbpFcsO", "202212", "12"));
    validateRow(response, 240, List.of("Female", "NVP only", "kJq2mPyFEHo", "202212", "12"));
    validateRow(response, 241, List.of("Female", "Others", "bL4ooGhyHRQ", "202210", "12"));
    validateRow(response, 242, List.of("Female", "NVP only", "fdc6uOvgoji", "202207", "12"));
    validateRow(response, 243, List.of("Female", "Others", "kJq2mPyFEHo", "202208", "11"));
    validateRow(response, 244, List.of("Female", "Others", "jmIPBj66vD6", "202209", "11"));
    validateRow(response, 245, List.of("Female", "Others", "bL4ooGhyHRQ", "202211", "11"));
    validateRow(response, 246, List.of("Female", "Others", "at6UHUQatSo", "202211", "11"));
    validateRow(response, 247, List.of("Female", "NVP only", "jUb8gELQApl", "202212", "11"));
    validateRow(response, 248, List.of("Female", "NVP only", "Vth0fbpFcsO", "202207", "11"));
    validateRow(response, 249, List.of("Female", "Others", "qhqAxPSTUXp", "202210", "11"));
    validateRow(response, 250, List.of("Female", "Others", "fdc6uOvgoji", "202209", "11"));
    validateRow(response, 251, List.of("Female", "NVP only", "jmIPBj66vD6", "202211", "11"));
    validateRow(response, 252, List.of("Female", "NVP only", "bL4ooGhyHRQ", "202209", "11"));
    validateRow(response, 253, List.of("Female", "NVP only", "jmIPBj66vD6", "202210", "11"));
    validateRow(response, 254, List.of("Female", "Others", "lc3eMKXaEfw", "202209", "11"));
    validateRow(response, 255, List.of("Female", "Others", "qhqAxPSTUXp", "202209", "11"));
    validateRow(response, 256, List.of("Female", "NVP only", "eIQbndfxQMb", "202209", "11"));
    validateRow(response, 257, List.of("Female", "Others", "O6uvpzGd5pu", "202209", "11"));
    validateRow(response, 258, List.of("Female", "Others", "jmIPBj66vD6", "202212", "11"));
    validateRow(response, 259, List.of("Female", "Others", "at6UHUQatSo", "202207", "10"));
    validateRow(response, 260, List.of("Female", "Others", "qhqAxPSTUXp", "202208", "10"));
    validateRow(response, 261, List.of("Female", "Others", "eIQbndfxQMb", "202207", "10"));
    validateRow(response, 262, List.of("Female", "Others", "jUb8gELQApl", "202209", "10"));
    validateRow(response, 263, List.of("Female", "Others", "at6UHUQatSo", "202209", "10"));
    validateRow(response, 264, List.of("Female", "Others", "eIQbndfxQMb", "202212", "10"));
    validateRow(response, 265, List.of("Female", "Others", "qhqAxPSTUXp", "202212", "10"));
    validateRow(response, 266, List.of("Female", "Others", "qhqAxPSTUXp", "202207", "10"));
    validateRow(response, 267, List.of("Female", "Others", "lc3eMKXaEfw", "202210", "10"));
    validateRow(response, 268, List.of("Female", "Others", "PMa2VCrupOd", "202208", "10"));
    validateRow(response, 269, List.of("Female", "NVP only", "PMa2VCrupOd", "202208", "10"));
    validateRow(response, 270, List.of("Female", "NVP only", "Vth0fbpFcsO", "202208", "10"));
    validateRow(response, 271, List.of("Female", "NVP only", "at6UHUQatSo", "202209", "10"));
    validateRow(response, 272, List.of("Female", "Others", "PMa2VCrupOd", "202212", "9"));
    validateRow(response, 273, List.of("Female", "NVP only", "lc3eMKXaEfw", "202207", "9"));
    validateRow(response, 274, List.of("Female", "NVP only", "jUb8gELQApl", "202207", "9"));
    validateRow(response, 275, List.of("Female", "NVP only", "PMa2VCrupOd", "202212", "9"));
    validateRow(response, 276, List.of("Female", "NVP only", "fdc6uOvgoji", "202208", "9"));
    validateRow(response, 277, List.of("Female", "Others", "PMa2VCrupOd", "202207", "9"));
    validateRow(response, 278, List.of("Female", "Others", "PMa2VCrupOd", "202209", "9"));
    validateRow(response, 279, List.of("Female", "NVP only", "qhqAxPSTUXp", "202208", "9"));
    validateRow(response, 280, List.of("Female", "Others", "eIQbndfxQMb", "202208", "8"));
    validateRow(response, 281, List.of("Female", "NVP only", "jUb8gELQApl", "202209", "8"));
    validateRow(response, 282, List.of("Female", "NVP only", "bL4ooGhyHRQ", "202210", "8"));
    validateRow(response, 283, List.of("Female", "Others", "lc3eMKXaEfw", "202212", "8"));
    validateRow(response, 284, List.of("Female", "NVP only", "bL4ooGhyHRQ", "202212", "8"));
    validateRow(response, 285, List.of("Female", "NVP only", "qhqAxPSTUXp", "202210", "8"));
    validateRow(response, 286, List.of("Female", "Others", "jUb8gELQApl", "202212", "8"));
    validateRow(response, 287, List.of("Female", "NVP only", "PMa2VCrupOd", "202210", "7"));
    validateRow(response, 288, List.of("Female", "NVP only", "lc3eMKXaEfw", "202211", "7"));
    validateRow(response, 289, List.of("Female", "Others", "Vth0fbpFcsO", "202212", "7"));
    validateRow(response, 290, List.of("Female", "Others", "bL4ooGhyHRQ", "202212", "7"));
    validateRow(response, 291, List.of("Female", "Others", "Vth0fbpFcsO", "202211", "7"));
    validateRow(response, 292, List.of("Female", "Others", "lc3eMKXaEfw", "202211", "7"));
    validateRow(response, 293, List.of("Female", "Others", "bL4ooGhyHRQ", "202207", "7"));
    validateRow(response, 294, List.of("Female", "NVP only", "eIQbndfxQMb", "202212", "7"));
    validateRow(response, 295, List.of("Female", "NVP only", "Vth0fbpFcsO", "202210", "7"));
    validateRow(response, 296, List.of("Female", "Others", "qhqAxPSTUXp", "202211", "7"));
    validateRow(response, 297, List.of("Female", "Others", "bL4ooGhyHRQ", "202209", "7"));
    validateRow(response, 298, List.of("Female", "NVP only", "qhqAxPSTUXp", "202207", "6"));
    validateRow(response, 299, List.of("Female", "NVP only", "PMa2VCrupOd", "202209", "6"));
    validateRow(response, 300, List.of("Female", "Others", "at6UHUQatSo", "202212", "6"));
    validateRow(response, 301, List.of("Female", "Others", "PMa2VCrupOd", "202211", "6"));
    validateRow(response, 302, List.of("Female", "NVP only", "lc3eMKXaEfw", "202208", "6"));
    validateRow(response, 303, List.of("Female", "Others", "jUb8gELQApl", "202210", "6"));
    validateRow(response, 304, List.of("Female", "Others", "lc3eMKXaEfw", "202208", "6"));
    validateRow(response, 305, List.of("Female", "NVP only", "qhqAxPSTUXp", "202212", "5"));
    validateRow(response, 306, List.of("Female", "NVP only", "lc3eMKXaEfw", "202210", "5"));
    validateRow(response, 307, List.of("Female", "NVP only", "lc3eMKXaEfw", "202209", "5"));
    validateRow(response, 308, List.of("Female", "NVP only", "jUb8gELQApl", "202211", "5"));
    validateRow(response, 309, List.of("Female", "Others", "lc3eMKXaEfw", "202207", "4"));
    validateRow(response, 310, List.of("Female", "NVP only", "PMa2VCrupOd", "202207", "4"));
    validateRow(response, 311, List.of("Female", "Others", "jUb8gELQApl", "202211", "4"));
    validateRow(response, 312, List.of("Female", "NVP only", "lc3eMKXaEfw", "202212", "4"));
  }

  @Test
  public void queryPentaAndMeaslesDoseActiveEventsLast12Months() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=ZzYYXq4fJie")
            .add("displayProperty=NAME")
            .add("eventStatus=ACTIVE")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add(
                "dimension=ou:ImspTQPwCqd,pe:LAST_12_MONTHS,ZzYYXq4fJie.vTUhAUZFoys,ZzYYXq4fJie.FqlgKAG8HOu")
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);
    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(5)))
        .body("rows", hasSize(equalTo(56)))
        .body("height", equalTo(56))
        .body("width", equalTo(5))
        .body("headerWidth", equalTo(5));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"sXfZuRdvhl5\":{\"code\":\"0\",\"name\":\"Dose 0\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"VBGXfSXgJzv\":{\"code\":\"3\",\"name\":\"Dose 3\"},\"202208\":{\"name\":\"August 2022\"},\"202209\":{\"name\":\"September 2022\"},\"202305\":{\"name\":\"May 2023\"},\"202207\":{\"name\":\"July 2022\"},\"202306\":{\"name\":\"June 2023\"},\"202303\":{\"name\":\"March 2023\"},\"FqlgKAG8HOu\":{\"name\":\"MCH Measles dose\"},\"202304\":{\"name\":\"April 2023\"},\"202301\":{\"name\":\"January 2023\"},\"202302\":{\"name\":\"February 2023\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"202211\":{\"name\":\"November 2022\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"202212\":{\"name\":\"December 2022\"},\"202210\":{\"name\":\"October 2022\"},\"pe\":{\"name\":\"Period\"},\"vTUhAUZFoys\":{\"name\":\"MCH Penta dose\"},\"Xr0M5yEhtpT\":{\"code\":\"2\",\"name\":\"Dose 2\"},\"lFFqylGiWLk\":{\"code\":\"1\",\"name\":\"Dose 1\"}},\"dimensions\":{\"pe\":[\"202207\",\"202208\",\"202209\",\"202210\",\"202211\",\"202212\",\"202301\",\"202302\",\"202303\",\"202304\",\"202305\",\"202306\"],\"vTUhAUZFoys\":[\"sXfZuRdvhl5\",\"lFFqylGiWLk\",\"Xr0M5yEhtpT\",\"VBGXfSXgJzv\"],\"ou\":[\"ImspTQPwCqd\"],\"ZzYYXq4fJie.FqlgKAG8HOu\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "vTUhAUZFoys", "MCH Penta dose", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        1,
        "FqlgKAG8HOu",
        "MCH Measles dose",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(response, 2, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 4, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, 0, List.of("3", "1", "ImspTQPwCqd", "202208", "101"));
    validateRow(response, 1, List.of("3", "0", "ImspTQPwCqd", "202210", "92"));
    validateRow(response, 2, List.of("3", "1", "ImspTQPwCqd", "202209", "89"));
    validateRow(response, 3, List.of("3", "1", "ImspTQPwCqd", "202211", "89"));
    validateRow(response, 4, List.of("3", "1", "ImspTQPwCqd", "202212", "86"));
    validateRow(response, 5, List.of("3", "0", "ImspTQPwCqd", "202208", "86"));
    validateRow(response, 6, List.of("3", "0", "ImspTQPwCqd", "202209", "83"));
    validateRow(response, 7, List.of("3", "1", "ImspTQPwCqd", "202207", "81"));
    validateRow(response, 8, List.of("3", "0", "ImspTQPwCqd", "202212", "78"));
    validateRow(response, 9, List.of("3", "0", "ImspTQPwCqd", "202211", "77"));
    validateRow(response, 10, List.of("3", "1", "ImspTQPwCqd", "202210", "77"));
    validateRow(response, 11, List.of("3", "0", "ImspTQPwCqd", "202207", "71"));
    validateRow(response, 12, List.of("3", "0", "ImspTQPwCqd", "202301", "3"));
    validateRow(response, 13, List.of("3", "1", "ImspTQPwCqd", "202301", "2"));
    validateRow(response, 14, List.of("2", "0", "ImspTQPwCqd", "202210", "99"));
    validateRow(response, 15, List.of("2", "0", "ImspTQPwCqd", "202212", "99"));
    validateRow(response, 16, List.of("2", "0", "ImspTQPwCqd", "202208", "94"));
    validateRow(response, 17, List.of("2", "0", "ImspTQPwCqd", "202209", "91"));
    validateRow(response, 18, List.of("2", "1", "ImspTQPwCqd", "202211", "84"));
    validateRow(response, 19, List.of("2", "1", "ImspTQPwCqd", "202210", "84"));
    validateRow(response, 20, List.of("2", "1", "ImspTQPwCqd", "202209", "83"));
    validateRow(response, 21, List.of("2", "1", "ImspTQPwCqd", "202212", "83"));
    validateRow(response, 22, List.of("2", "0", "ImspTQPwCqd", "202211", "78"));
    validateRow(response, 23, List.of("2", "1", "ImspTQPwCqd", "202207", "75"));
    validateRow(response, 24, List.of("2", "0", "ImspTQPwCqd", "202207", "71"));
    validateRow(response, 25, List.of("2", "1", "ImspTQPwCqd", "202208", "69"));
    validateRow(response, 26, List.of("2", "0", "ImspTQPwCqd", "202301", "4"));
    validateRow(response, 27, List.of("1", "0", "ImspTQPwCqd", "202212", "101"));
    validateRow(response, 28, List.of("1", "1", "ImspTQPwCqd", "202207", "101"));
    validateRow(response, 29, List.of("1", "1", "ImspTQPwCqd", "202212", "97"));
    validateRow(response, 30, List.of("1", "0", "ImspTQPwCqd", "202211", "93"));
    validateRow(response, 31, List.of("1", "1", "ImspTQPwCqd", "202209", "92"));
    validateRow(response, 32, List.of("1", "0", "ImspTQPwCqd", "202208", "92"));
    validateRow(response, 33, List.of("1", "1", "ImspTQPwCqd", "202210", "84"));
    validateRow(response, 34, List.of("1", "0", "ImspTQPwCqd", "202210", "83"));
    validateRow(response, 35, List.of("1", "0", "ImspTQPwCqd", "202207", "81"));
    validateRow(response, 36, List.of("1", "1", "ImspTQPwCqd", "202208", "74"));
    validateRow(response, 37, List.of("1", "0", "ImspTQPwCqd", "202209", "74"));
    validateRow(response, 38, List.of("1", "1", "ImspTQPwCqd", "202211", "72"));
    validateRow(response, 39, List.of("1", "0", "ImspTQPwCqd", "202301", "4"));
    validateRow(response, 40, List.of("1", "1", "ImspTQPwCqd", "202301", "2"));
    validateRow(response, 41, List.of("0", "1", "ImspTQPwCqd", "202208", "91"));
    validateRow(response, 42, List.of("0", "0", "ImspTQPwCqd", "202210", "88"));
    validateRow(response, 43, List.of("0", "1", "ImspTQPwCqd", "202212", "86"));
    validateRow(response, 44, List.of("0", "1", "ImspTQPwCqd", "202209", "85"));
    validateRow(response, 45, List.of("0", "0", "ImspTQPwCqd", "202208", "82"));
    validateRow(response, 46, List.of("0", "0", "ImspTQPwCqd", "202212", "80"));
    validateRow(response, 47, List.of("0", "1", "ImspTQPwCqd", "202210", "78"));
    validateRow(response, 48, List.of("0", "1", "ImspTQPwCqd", "202207", "73"));
    validateRow(response, 49, List.of("0", "0", "ImspTQPwCqd", "202209", "71"));
    validateRow(response, 50, List.of("0", "0", "ImspTQPwCqd", "202211", "70"));
    validateRow(response, 51, List.of("0", "1", "ImspTQPwCqd", "202211", "70"));
    validateRow(response, 52, List.of("0", "0", "ImspTQPwCqd", "202207", "66"));
    validateRow(response, 53, List.of("0", "0", "ImspTQPwCqd", "202301", "3"));
    validateRow(response, 54, List.of("0", "1", "ImspTQPwCqd", "202301", "1"));
    validateRow(response, 55, List.of("", "", "ImspTQPwCqd", "202208", "1"));
  }

  @Test
  public void queryAverageWeightByModeOfDischargeAndGenderLast4Q() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("aggregationType=AVERAGE")
            .add("stage=Zj7UnCAulEk")
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add(
                "dimension=ou:ImspTQPwCqd,pe:LAST_4_QUARTERS,Zj7UnCAulEk.fWIAEtYVEGk,Zj7UnCAulEk.oZg33kd9taw")
            .add("value=vV9UWAZohSf")
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.aggregate().get("eBAyeGv0exc", JSON, JSON, params);
    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(5)))
        .body("rows", hasSize(equalTo(17)))
        .body("height", equalTo(17))
        .body("width", equalTo(5))
        .body("headerWidth", equalTo(5));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"rBvjJYbMCVx\":{\"code\":\"Male\",\"name\":\"Male\"},\"ou\":{\"name\":\"Organisation unit\"},\"Fhbf4aKpZmZ\":{\"code\":\"MODABSC\",\"name\":\"Absconded\"},\"fWIAEtYVEGk\":{\"name\":\"Mode of Discharge\"},\"vV9UWAZohSf\":{\"code\":\"DE_240795\",\"name\":\"Weight in kg\"},\"gj2fKKyp8OH\":{\"code\":\"MODDIED\",\"name\":\"Died\"},\"2022Q3\":{\"name\":\"July - September 2022\"},\"2023Q2\":{\"name\":\"April - June 2023\"},\"2022Q4\":{\"name\":\"October - December 2022\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"},\"2023Q1\":{\"name\":\"January - March 2023\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"pe\":{\"name\":\"Period\"},\"LAST_4_QUARTERS\":{\"name\":\"Last 4 quarters\"},\"oZg33kd9taw\":{\"name\":\"Gender\"},\"fShHdgT7XGb\":{\"code\":\"MODTRANS\",\"name\":\"Transferred\"},\"yeod5tOXpkP\":{\"code\":\"MODDISCH\",\"name\":\"Discharged\"}},\"dimensions\":{\"pe\":[\"2022Q3\",\"2022Q4\",\"2023Q1\",\"2023Q2\"],\"ou\":[\"ImspTQPwCqd\"],\"fWIAEtYVEGk\":[\"yeod5tOXpkP\",\"gj2fKKyp8OH\",\"fShHdgT7XGb\",\"Fhbf4aKpZmZ\"],\"oZg33kd9taw\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "fWIAEtYVEGk", "Mode of Discharge", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "oZg33kd9taw", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 4, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, 0, List.of("MODTRANS", "", "2022Q3", "ImspTQPwCqd", "55.0"));
    validateRow(response, 1, List.of("MODTRANS", "Female", "2022Q4", "ImspTQPwCqd", "49.6"));
    validateRow(response, 2, List.of("MODTRANS", "Male", "2022Q4", "ImspTQPwCqd", "49.5"));
    validateRow(response, 3, List.of("MODTRANS", "Male", "2022Q3", "ImspTQPwCqd", "49.0"));
    validateRow(response, 4, List.of("MODTRANS", "Female", "2022Q3", "ImspTQPwCqd", "49.0"));
    validateRow(response, 5, List.of("MODDISCH", "Male", "2022Q3", "ImspTQPwCqd", "49.6"));
    validateRow(response, 6, List.of("MODDISCH", "Female", "2022Q3", "ImspTQPwCqd", "49.5"));
    validateRow(response, 7, List.of("MODDISCH", "Female", "2022Q4", "ImspTQPwCqd", "49.4"));
    validateRow(response, 8, List.of("MODDISCH", "Male", "2022Q4", "ImspTQPwCqd", "49.0"));
    validateRow(response, 9, List.of("MODDIED", "Male", "2022Q3", "ImspTQPwCqd", "50.3"));
    validateRow(response, 10, List.of("MODDIED", "Male", "2022Q4", "ImspTQPwCqd", "50.1"));
    validateRow(response, 11, List.of("MODDIED", "Female", "2022Q3", "ImspTQPwCqd", "49.8"));
    validateRow(response, 12, List.of("MODDIED", "Female", "2022Q4", "ImspTQPwCqd", "49.5"));
    validateRow(response, 13, List.of("MODABSC", "Male", "2022Q4", "ImspTQPwCqd", "49.5"));
    validateRow(response, 14, List.of("MODABSC", "Male", "2022Q3", "ImspTQPwCqd", "49.5"));
    validateRow(response, 15, List.of("MODABSC", "Female", "2022Q4", "ImspTQPwCqd", "49.2"));
    validateRow(response, 16, List.of("MODABSC", "Female", "2022Q3", "ImspTQPwCqd", "49.1"));
  }
}
