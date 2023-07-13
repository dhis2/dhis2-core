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
    validateRow(response, List.of("Male", "202210", "TEQlaapDQoK", "51"));
    validateRow(response, List.of("Male", "202212", "at6UHUQatSo", "46"));
    validateRow(response, List.of("Male", "202212", "kJq2mPyFEHo", "45"));
    validateRow(response, List.of("Male", "202208", "TEQlaapDQoK", "45"));
    validateRow(response, List.of("Male", "202207", "kJq2mPyFEHo", "43"));
    validateRow(response, List.of("Male", "202210", "kJq2mPyFEHo", "41"));
    validateRow(response, List.of("Male", "202212", "O6uvpzGd5pu", "39"));
    validateRow(response, List.of("Male", "202209", "O6uvpzGd5pu", "37"));
    validateRow(response, List.of("Male", "202207", "O6uvpzGd5pu", "37"));
    validateRow(response, List.of("Male", "202211", "O6uvpzGd5pu", "37"));
    validateRow(response, List.of("Male", "202211", "kJq2mPyFEHo", "36"));
    validateRow(response, List.of("Male", "202208", "O6uvpzGd5pu", "35"));
    validateRow(response, List.of("Male", "202208", "kJq2mPyFEHo", "35"));
    validateRow(response, List.of("Male", "202207", "at6UHUQatSo", "35"));
    validateRow(response, List.of("Male", "202207", "TEQlaapDQoK", "35"));
    validateRow(response, List.of("Male", "202210", "eIQbndfxQMb", "35"));
    validateRow(response, List.of("Male", "202211", "TEQlaapDQoK", "34"));
    validateRow(response, List.of("Male", "202209", "kJq2mPyFEHo", "33"));
    validateRow(response, List.of("Male", "202210", "O6uvpzGd5pu", "32"));
    validateRow(response, List.of("Male", "202209", "eIQbndfxQMb", "31"));
    validateRow(response, List.of("Male", "202209", "TEQlaapDQoK", "31"));
    validateRow(response, List.of("Male", "202211", "eIQbndfxQMb", "30"));
    validateRow(response, List.of("Male", "202210", "jmIPBj66vD6", "30"));
    validateRow(response, List.of("Male", "202207", "Vth0fbpFcsO", "29"));
    validateRow(response, List.of("Male", "202207", "bL4ooGhyHRQ", "28"));
    validateRow(response, List.of("Male", "202208", "jmIPBj66vD6", "28"));
    validateRow(response, List.of("Male", "202208", "eIQbndfxQMb", "28"));
    validateRow(response, List.of("Male", "202208", "at6UHUQatSo", "28"));
    validateRow(response, List.of("Male", "202209", "fdc6uOvgoji", "27"));
    validateRow(response, List.of("Male", "202211", "jmIPBj66vD6", "27"));
    validateRow(response, List.of("Male", "202212", "jUb8gELQApl", "27"));
    validateRow(response, List.of("Male", "202209", "Vth0fbpFcsO", "27"));
    validateRow(response, List.of("Male", "202208", "Vth0fbpFcsO", "27"));
    validateRow(response, List.of("Male", "202210", "at6UHUQatSo", "26"));
    validateRow(response, List.of("Male", "202208", "jUb8gELQApl", "26"));
    validateRow(response, List.of("Male", "202208", "qhqAxPSTUXp", "26"));
    validateRow(response, List.of("Male", "202211", "fdc6uOvgoji", "25"));
    validateRow(response, List.of("Male", "202212", "eIQbndfxQMb", "25"));
    validateRow(response, List.of("Male", "202210", "qhqAxPSTUXp", "25"));
    validateRow(response, List.of("Male", "202211", "at6UHUQatSo", "25"));
    validateRow(response, List.of("Male", "202207", "jmIPBj66vD6", "25"));
    validateRow(response, List.of("Male", "202212", "fdc6uOvgoji", "24"));
    validateRow(response, List.of("Male", "202210", "Vth0fbpFcsO", "24"));
    validateRow(response, List.of("Male", "202212", "qhqAxPSTUXp", "24"));
    validateRow(response, List.of("Male", "202207", "eIQbndfxQMb", "24"));
    validateRow(response, List.of("Male", "202209", "jmIPBj66vD6", "24"));
    validateRow(response, List.of("Male", "202209", "at6UHUQatSo", "24"));
    validateRow(response, List.of("Male", "202207", "fdc6uOvgoji", "23"));
    validateRow(response, List.of("Male", "202212", "PMa2VCrupOd", "23"));
    validateRow(response, List.of("Male", "202210", "bL4ooGhyHRQ", "23"));
    validateRow(response, List.of("Male", "202208", "fdc6uOvgoji", "23"));
    validateRow(response, List.of("Male", "202212", "lc3eMKXaEfw", "23"));
    validateRow(response, List.of("Male", "202211", "jUb8gELQApl", "22"));
    validateRow(response, List.of("Male", "202212", "jmIPBj66vD6", "22"));
    validateRow(response, List.of("Male", "202211", "qhqAxPSTUXp", "22"));
    validateRow(response, List.of("Male", "202212", "TEQlaapDQoK", "20"));
    validateRow(response, List.of("Male", "202211", "bL4ooGhyHRQ", "20"));
    validateRow(response, List.of("Male", "202210", "fdc6uOvgoji", "20"));
    validateRow(response, List.of("Male", "202208", "bL4ooGhyHRQ", "20"));
    validateRow(response, List.of("Male", "202207", "jUb8gELQApl", "20"));
    validateRow(response, List.of("Male", "202208", "PMa2VCrupOd", "19"));
    validateRow(response, List.of("Male", "202211", "Vth0fbpFcsO", "19"));
    validateRow(response, List.of("Male", "202212", "Vth0fbpFcsO", "19"));
    validateRow(response, List.of("Male", "202209", "jUb8gELQApl", "19"));
    validateRow(response, List.of("Male", "202209", "qhqAxPSTUXp", "18"));
    validateRow(response, List.of("Male", "202209", "bL4ooGhyHRQ", "18"));
    validateRow(response, List.of("Male", "202209", "PMa2VCrupOd", "18"));
    validateRow(response, List.of("Male", "202212", "bL4ooGhyHRQ", "17"));
    validateRow(response, List.of("Male", "202211", "lc3eMKXaEfw", "17"));
    validateRow(response, List.of("Male", "202210", "PMa2VCrupOd", "17"));
    validateRow(response, List.of("Male", "202207", "lc3eMKXaEfw", "16"));
    validateRow(response, List.of("Male", "202208", "lc3eMKXaEfw", "15"));
    validateRow(response, List.of("Male", "202210", "jUb8gELQApl", "14"));
    validateRow(response, List.of("Male", "202210", "lc3eMKXaEfw", "13"));
    validateRow(response, List.of("Male", "202207", "PMa2VCrupOd", "11"));
    validateRow(response, List.of("Male", "202207", "qhqAxPSTUXp", "10"));
    validateRow(response, List.of("Male", "202209", "lc3eMKXaEfw", "9"));
    validateRow(response, List.of("Male", "202211", "PMa2VCrupOd", "9"));
    validateRow(response, List.of("Female", "202209", "kJq2mPyFEHo", "47"));
    validateRow(response, List.of("Female", "202212", "O6uvpzGd5pu", "46"));
    validateRow(response, List.of("Female", "202211", "TEQlaapDQoK", "44"));
    validateRow(response, List.of("Female", "202207", "TEQlaapDQoK", "39"));
    validateRow(response, List.of("Female", "202208", "at6UHUQatSo", "39"));
    validateRow(response, List.of("Female", "202211", "kJq2mPyFEHo", "38"));
    validateRow(response, List.of("Female", "202208", "O6uvpzGd5pu", "37"));
    validateRow(response, List.of("Female", "202208", "TEQlaapDQoK", "36"));
    validateRow(response, List.of("Female", "202209", "TEQlaapDQoK", "35"));
    validateRow(response, List.of("Female", "202210", "kJq2mPyFEHo", "35"));
    validateRow(response, List.of("Female", "202208", "jmIPBj66vD6", "35"));
    validateRow(response, List.of("Female", "202207", "O6uvpzGd5pu", "35"));
    validateRow(response, List.of("Female", "202210", "at6UHUQatSo", "33"));
    validateRow(response, List.of("Female", "202211", "O6uvpzGd5pu", "33"));
    validateRow(response, List.of("Female", "202209", "Vth0fbpFcsO", "32"));
    validateRow(response, List.of("Female", "202210", "fdc6uOvgoji", "32"));
    validateRow(response, List.of("Female", "202208", "jUb8gELQApl", "32"));
    validateRow(response, List.of("Female", "202210", "TEQlaapDQoK", "31"));
    validateRow(response, List.of("Female", "202210", "O6uvpzGd5pu", "31"));
    validateRow(response, List.of("Female", "202208", "kJq2mPyFEHo", "31"));
    validateRow(response, List.of("Female", "202212", "fdc6uOvgoji", "30"));
    validateRow(response, List.of("Female", "202211", "eIQbndfxQMb", "30"));
    validateRow(response, List.of("Female", "202210", "jmIPBj66vD6", "30"));
    validateRow(response, List.of("Female", "202212", "at6UHUQatSo", "30"));
    validateRow(response, List.of("Female", "202207", "jmIPBj66vD6", "30"));
    validateRow(response, List.of("Female", "202212", "TEQlaapDQoK", "29"));
    validateRow(response, List.of("Female", "202209", "fdc6uOvgoji", "29"));
    validateRow(response, List.of("Female", "202212", "kJq2mPyFEHo", "28"));
    validateRow(response, List.of("Female", "202211", "fdc6uOvgoji", "28"));
    validateRow(response, List.of("Female", "202210", "eIQbndfxQMb", "28"));
    validateRow(response, List.of("Female", "202207", "kJq2mPyFEHo", "28"));
    validateRow(response, List.of("Female", "202207", "fdc6uOvgoji", "27"));
    validateRow(response, List.of("Female", "202207", "at6UHUQatSo", "27"));
    validateRow(response, List.of("Female", "202208", "eIQbndfxQMb", "26"));
    validateRow(response, List.of("Female", "202211", "qhqAxPSTUXp", "26"));
    validateRow(response, List.of("Female", "202207", "Vth0fbpFcsO", "26"));
    validateRow(response, List.of("Female", "202209", "O6uvpzGd5pu", "26"));
    validateRow(response, List.of("Female", "202211", "at6UHUQatSo", "25"));
    validateRow(response, List.of("Female", "202208", "bL4ooGhyHRQ", "25"));
    validateRow(response, List.of("Female", "202212", "jmIPBj66vD6", "24"));
    validateRow(response, List.of("Female", "202209", "qhqAxPSTUXp", "24"));
    validateRow(response, List.of("Female", "202207", "jUb8gELQApl", "24"));
    validateRow(response, List.of("Female", "202209", "eIQbndfxQMb", "24"));
    validateRow(response, List.of("Female", "202207", "eIQbndfxQMb", "24"));
    validateRow(response, List.of("Female", "202210", "Vth0fbpFcsO", "24"));
    validateRow(response, List.of("Female", "202211", "jmIPBj66vD6", "24"));
    validateRow(response, List.of("Female", "202211", "Vth0fbpFcsO", "23"));
    validateRow(response, List.of("Female", "202209", "jmIPBj66vD6", "23"));
    validateRow(response, List.of("Female", "202211", "bL4ooGhyHRQ", "23"));
    validateRow(response, List.of("Female", "202208", "Vth0fbpFcsO", "23"));
    validateRow(response, List.of("Female", "202208", "fdc6uOvgoji", "23"));
    validateRow(response, List.of("Female", "202210", "PMa2VCrupOd", "22"));
    validateRow(response, List.of("Female", "202209", "at6UHUQatSo", "20"));
    validateRow(response, List.of("Female", "202210", "bL4ooGhyHRQ", "20"));
    validateRow(response, List.of("Female", "202208", "PMa2VCrupOd", "20"));
    validateRow(response, List.of("Female", "202210", "qhqAxPSTUXp", "19"));
    validateRow(response, List.of("Female", "202207", "bL4ooGhyHRQ", "19"));
    validateRow(response, List.of("Female", "202212", "Vth0fbpFcsO", "19"));
    validateRow(response, List.of("Female", "202212", "jUb8gELQApl", "19"));
    validateRow(response, List.of("Female", "202208", "qhqAxPSTUXp", "19"));
    validateRow(response, List.of("Female", "202209", "bL4ooGhyHRQ", "18"));
    validateRow(response, List.of("Female", "202209", "jUb8gELQApl", "18"));
    validateRow(response, List.of("Female", "202210", "jUb8gELQApl", "18"));
    validateRow(response, List.of("Female", "202211", "PMa2VCrupOd", "18"));
    validateRow(response, List.of("Female", "202212", "PMa2VCrupOd", "18"));
    validateRow(response, List.of("Female", "202212", "eIQbndfxQMb", "17"));
    validateRow(response, List.of("Female", "202209", "lc3eMKXaEfw", "16"));
    validateRow(response, List.of("Female", "202207", "qhqAxPSTUXp", "16"));
    validateRow(response, List.of("Female", "202209", "PMa2VCrupOd", "15"));
    validateRow(response, List.of("Female", "202212", "qhqAxPSTUXp", "15"));
    validateRow(response, List.of("Female", "202210", "lc3eMKXaEfw", "15"));
    validateRow(response, List.of("Female", "202212", "bL4ooGhyHRQ", "15"));
    validateRow(response, List.of("Female", "202211", "lc3eMKXaEfw", "14"));
    validateRow(response, List.of("Female", "202207", "lc3eMKXaEfw", "13"));
    validateRow(response, List.of("Female", "202207", "PMa2VCrupOd", "13"));
    validateRow(response, List.of("Female", "202212", "lc3eMKXaEfw", "12"));
    validateRow(response, List.of("Female", "202208", "lc3eMKXaEfw", "12"));
    validateRow(response, List.of("Female", "202211", "jUb8gELQApl", "9"));
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
    validateRow(response, List.of("Male", "NVP only", "TEQlaapDQoK", "202210", "32"));
    validateRow(response, List.of("Male", "NVP only", "kJq2mPyFEHo", "202212", "31"));
    validateRow(response, List.of("Male", "Others", "O6uvpzGd5pu", "202211", "26"));
    validateRow(response, List.of("Male", "NVP only", "at6UHUQatSo", "202212", "23"));
    validateRow(response, List.of("Male", "Others", "at6UHUQatSo", "202212", "23"));
    validateRow(response, List.of("Male", "Others", "TEQlaapDQoK", "202208", "23"));
    validateRow(response, List.of("Male", "NVP only", "kJq2mPyFEHo", "202207", "22"));
    validateRow(response, List.of("Male", "NVP only", "TEQlaapDQoK", "202208", "22"));
    validateRow(response, List.of("Male", "Others", "kJq2mPyFEHo", "202211", "21"));
    validateRow(response, List.of("Male", "Others", "at6UHUQatSo", "202207", "21"));
    validateRow(response, List.of("Male", "NVP only", "kJq2mPyFEHo", "202210", "21"));
    validateRow(response, List.of("Male", "Others", "kJq2mPyFEHo", "202207", "21"));
    validateRow(response, List.of("Male", "NVP only", "O6uvpzGd5pu", "202212", "21"));
    validateRow(response, List.of("Male", "Others", "kJq2mPyFEHo", "202208", "20"));
    validateRow(response, List.of("Male", "Others", "kJq2mPyFEHo", "202210", "20"));
    validateRow(response, List.of("Male", "Others", "TEQlaapDQoK", "202209", "19"));
    validateRow(response, List.of("Male", "NVP only", "O6uvpzGd5pu", "202207", "19"));
    validateRow(response, List.of("Male", "Others", "kJq2mPyFEHo", "202209", "19"));
    validateRow(response, List.of("Male", "Others", "TEQlaapDQoK", "202210", "19"));
    validateRow(response, List.of("Male", "NVP only", "O6uvpzGd5pu", "202209", "19"));
    validateRow(response, List.of("Male", "NVP only", "eIQbndfxQMb", "202210", "19"));
    validateRow(response, List.of("Male", "Others", "TEQlaapDQoK", "202207", "19"));
    validateRow(response, List.of("Male", "Others", "Vth0fbpFcsO", "202209", "19"));
    validateRow(response, List.of("Male", "Others", "O6uvpzGd5pu", "202207", "18"));
    validateRow(response, List.of("Male", "Others", "O6uvpzGd5pu", "202212", "18"));
    validateRow(response, List.of("Male", "NVP only", "O6uvpzGd5pu", "202208", "18"));
    validateRow(response, List.of("Male", "NVP only", "jmIPBj66vD6", "202207", "18"));
    validateRow(response, List.of("Male", "NVP only", "eIQbndfxQMb", "202208", "18"));
    validateRow(response, List.of("Male", "Others", "TEQlaapDQoK", "202211", "17"));
    validateRow(response, List.of("Male", "Others", "O6uvpzGd5pu", "202210", "17"));
    validateRow(response, List.of("Male", "Others", "jUb8gELQApl", "202212", "17"));
    validateRow(response, List.of("Male", "NVP only", "TEQlaapDQoK", "202211", "17"));
    validateRow(response, List.of("Male", "Others", "O6uvpzGd5pu", "202209", "17"));
    validateRow(response, List.of("Male", "Others", "Vth0fbpFcsO", "202210", "17"));
    validateRow(response, List.of("Male", "Others", "eIQbndfxQMb", "202212", "16"));
    validateRow(response, List.of("Male", "Others", "eIQbndfxQMb", "202210", "16"));
    validateRow(response, List.of("Male", "Others", "at6UHUQatSo", "202208", "16"));
    validateRow(response, List.of("Male", "Others", "jUb8gELQApl", "202208", "16"));
    validateRow(response, List.of("Male", "Others", "O6uvpzGd5pu", "202208", "16"));
    validateRow(response, List.of("Male", "NVP only", "jmIPBj66vD6", "202208", "16"));
    validateRow(response, List.of("Male", "NVP only", "TEQlaapDQoK", "202207", "16"));
    validateRow(response, List.of("Male", "NVP only", "jmIPBj66vD6", "202211", "16"));
    validateRow(response, List.of("Male", "Others", "eIQbndfxQMb", "202209", "16"));
    validateRow(response, List.of("Male", "NVP only", "lc3eMKXaEfw", "202212", "16"));
    validateRow(response, List.of("Male", "Others", "jmIPBj66vD6", "202210", "16"));
    validateRow(response, List.of("Male", "Others", "Vth0fbpFcsO", "202207", "15"));
    validateRow(response, List.of("Male", "NVP only", "Vth0fbpFcsO", "202208", "15"));
    validateRow(response, List.of("Male", "NVP only", "at6UHUQatSo", "202209", "15"));
    validateRow(response, List.of("Male", "NVP only", "eIQbndfxQMb", "202211", "15"));
    validateRow(response, List.of("Male", "NVP only", "jUb8gELQApl", "202207", "15"));
    validateRow(response, List.of("Male", "NVP only", "O6uvpzGd5pu", "202210", "15"));
    validateRow(response, List.of("Male", "NVP only", "jUb8gELQApl", "202211", "15"));
    validateRow(response, List.of("Male", "NVP only", "fdc6uOvgoji", "202210", "15"));
    validateRow(response, List.of("Male", "NVP only", "kJq2mPyFEHo", "202208", "15"));
    validateRow(response, List.of("Male", "Others", "PMa2VCrupOd", "202212", "15"));
    validateRow(response, List.of("Male", "NVP only", "kJq2mPyFEHo", "202211", "15"));
    validateRow(response, List.of("Male", "NVP only", "eIQbndfxQMb", "202209", "15"));
    validateRow(response, List.of("Male", "Others", "eIQbndfxQMb", "202211", "15"));
    validateRow(response, List.of("Male", "NVP only", "bL4ooGhyHRQ", "202207", "15"));
    validateRow(response, List.of("Male", "NVP only", "fdc6uOvgoji", "202209", "14"));
    validateRow(response, List.of("Male", "NVP only", "kJq2mPyFEHo", "202209", "14"));
    validateRow(response, List.of("Male", "Others", "fdc6uOvgoji", "202208", "14"));
    validateRow(response, List.of("Male", "Others", "at6UHUQatSo", "202211", "14"));
    validateRow(response, List.of("Male", "Others", "fdc6uOvgoji", "202212", "14"));
    validateRow(response, List.of("Male", "NVP only", "at6UHUQatSo", "202210", "14"));
    validateRow(response, List.of("Male", "Others", "kJq2mPyFEHo", "202212", "14"));
    validateRow(response, List.of("Male", "Others", "jmIPBj66vD6", "202212", "14"));
    validateRow(response, List.of("Male", "NVP only", "jmIPBj66vD6", "202210", "14"));
    validateRow(response, List.of("Male", "NVP only", "Vth0fbpFcsO", "202207", "14"));
    validateRow(response, List.of("Male", "NVP only", "at6UHUQatSo", "202207", "14"));
    validateRow(response, List.of("Male", "NVP only", "bL4ooGhyHRQ", "202210", "13"));
    validateRow(response, List.of("Male", "NVP only", "qhqAxPSTUXp", "202210", "13"));
    validateRow(response, List.of("Male", "Others", "qhqAxPSTUXp", "202208", "13"));
    validateRow(response, List.of("Male", "Others", "fdc6uOvgoji", "202209", "13"));
    validateRow(response, List.of("Male", "NVP only", "jmIPBj66vD6", "202209", "13"));
    validateRow(response, List.of("Male", "Others", "bL4ooGhyHRQ", "202207", "13"));
    validateRow(response, List.of("Male", "NVP only", "eIQbndfxQMb", "202207", "13"));
    validateRow(response, List.of("Male", "NVP only", "fdc6uOvgoji", "202211", "13"));
    validateRow(response, List.of("Male", "NVP only", "qhqAxPSTUXp", "202212", "13"));
    validateRow(response, List.of("Male", "NVP only", "qhqAxPSTUXp", "202208", "13"));
    validateRow(response, List.of("Male", "NVP only", "qhqAxPSTUXp", "202211", "13"));
    validateRow(response, List.of("Male", "Others", "bL4ooGhyHRQ", "202211", "13"));
    validateRow(response, List.of("Male", "Others", "jUb8gELQApl", "202209", "12"));
    validateRow(response, List.of("Male", "Others", "jmIPBj66vD6", "202208", "12"));
    validateRow(response, List.of("Male", "NVP only", "TEQlaapDQoK", "202209", "12"));
    validateRow(response, List.of("Male", "Others", "at6UHUQatSo", "202210", "12"));
    validateRow(response, List.of("Male", "Others", "Vth0fbpFcsO", "202208", "12"));
    validateRow(response, List.of("Male", "Others", "qhqAxPSTUXp", "202210", "12"));
    validateRow(response, List.of("Male", "NVP only", "at6UHUQatSo", "202208", "12"));
    validateRow(response, List.of("Male", "NVP only", "fdc6uOvgoji", "202207", "12"));
    validateRow(response, List.of("Male", "Others", "fdc6uOvgoji", "202211", "12"));
    validateRow(response, List.of("Male", "NVP only", "at6UHUQatSo", "202211", "11"));
    validateRow(response, List.of("Male", "Others", "jmIPBj66vD6", "202209", "11"));
    validateRow(response, List.of("Male", "Others", "Vth0fbpFcsO", "202212", "11"));
    validateRow(response, List.of("Male", "NVP only", "O6uvpzGd5pu", "202211", "11"));
    validateRow(response, List.of("Male", "Others", "bL4ooGhyHRQ", "202209", "11"));
    validateRow(response, List.of("Male", "NVP only", "Vth0fbpFcsO", "202211", "11"));
    validateRow(response, List.of("Male", "Others", "jmIPBj66vD6", "202211", "11"));
    validateRow(response, List.of("Male", "NVP only", "qhqAxPSTUXp", "202209", "11"));
    validateRow(response, List.of("Male", "Others", "fdc6uOvgoji", "202207", "11"));
    validateRow(response, List.of("Male", "NVP only", "PMa2VCrupOd", "202208", "11"));
    validateRow(response, List.of("Male", "Others", "qhqAxPSTUXp", "202212", "11"));
    validateRow(response, List.of("Male", "Others", "eIQbndfxQMb", "202207", "11"));
    validateRow(response, List.of("Male", "Others", "lc3eMKXaEfw", "202208", "11"));
    validateRow(response, List.of("Male", "Others", "TEQlaapDQoK", "202212", "10"));
    validateRow(response, List.of("Male", "NVP only", "TEQlaapDQoK", "202212", "10"));
    validateRow(response, List.of("Male", "Others", "eIQbndfxQMb", "202208", "10"));
    validateRow(response, List.of("Male", "NVP only", "fdc6uOvgoji", "202212", "10"));
    validateRow(response, List.of("Male", "Others", "bL4ooGhyHRQ", "202210", "10"));
    validateRow(response, List.of("Male", "NVP only", "bL4ooGhyHRQ", "202208", "10"));
    validateRow(response, List.of("Male", "Others", "bL4ooGhyHRQ", "202208", "10"));
    validateRow(response, List.of("Male", "NVP only", "lc3eMKXaEfw", "202207", "10"));
    validateRow(response, List.of("Male", "NVP only", "jUb8gELQApl", "202212", "10"));
    validateRow(response, List.of("Male", "NVP only", "jUb8gELQApl", "202208", "10"));
    validateRow(response, List.of("Male", "Others", "qhqAxPSTUXp", "202211", "9"));
    validateRow(response, List.of("Male", "NVP only", "PMa2VCrupOd", "202209", "9"));
    validateRow(response, List.of("Male", "NVP only", "lc3eMKXaEfw", "202211", "9"));
    validateRow(response, List.of("Male", "NVP only", "fdc6uOvgoji", "202208", "9"));
    validateRow(response, List.of("Male", "NVP only", "eIQbndfxQMb", "202212", "9"));
    validateRow(response, List.of("Male", "Others", "PMa2VCrupOd", "202210", "9"));
    validateRow(response, List.of("Male", "Others", "bL4ooGhyHRQ", "202212", "9"));
    validateRow(response, List.of("Male", "Others", "at6UHUQatSo", "202209", "9"));
    validateRow(response, List.of("Male", "Others", "PMa2VCrupOd", "202209", "9"));
    validateRow(response, List.of("Male", "NVP only", "Vth0fbpFcsO", "202209", "8"));
    validateRow(response, List.of("Male", "Others", "Vth0fbpFcsO", "202211", "8"));
    validateRow(response, List.of("Male", "NVP only", "Vth0fbpFcsO", "202212", "8"));
    validateRow(response, List.of("Male", "Others", "lc3eMKXaEfw", "202211", "8"));
    validateRow(response, List.of("Male", "NVP only", "PMa2VCrupOd", "202212", "8"));
    validateRow(response, List.of("Male", "NVP only", "jmIPBj66vD6", "202212", "8"));
    validateRow(response, List.of("Male", "Others", "PMa2VCrupOd", "202208", "8"));
    validateRow(response, List.of("Male", "NVP only", "bL4ooGhyHRQ", "202212", "8"));
    validateRow(response, List.of("Male", "NVP only", "PMa2VCrupOd", "202210", "8"));
    validateRow(response, List.of("Male", "NVP only", "jUb8gELQApl", "202209", "7"));
    validateRow(response, List.of("Male", "Others", "jUb8gELQApl", "202210", "7"));
    validateRow(response, List.of("Male", "Others", "lc3eMKXaEfw", "202212", "7"));
    validateRow(response, List.of("Male", "Others", "PMa2VCrupOd", "202207", "7"));
    validateRow(response, List.of("Male", "Others", "jUb8gELQApl", "202211", "7"));
    validateRow(response, List.of("Male", "Others", "jmIPBj66vD6", "202207", "7"));
    validateRow(response, List.of("Male", "NVP only", "jUb8gELQApl", "202210", "7"));
    validateRow(response, List.of("Male", "NVP only", "Vth0fbpFcsO", "202210", "7"));
    validateRow(response, List.of("Male", "NVP only", "lc3eMKXaEfw", "202210", "7"));
    validateRow(response, List.of("Male", "NVP only", "bL4ooGhyHRQ", "202211", "7"));
    validateRow(response, List.of("Male", "NVP only", "bL4ooGhyHRQ", "202209", "7"));
    validateRow(response, List.of("Male", "Others", "qhqAxPSTUXp", "202207", "7"));
    validateRow(response, List.of("Male", "Others", "qhqAxPSTUXp", "202209", "7"));
    validateRow(response, List.of("Male", "Others", "lc3eMKXaEfw", "202210", "6"));
    validateRow(response, List.of("Male", "Others", "lc3eMKXaEfw", "202207", "6"));
    validateRow(response, List.of("Male", "Others", "PMa2VCrupOd", "202211", "6"));
    validateRow(response, List.of("Male", "Others", "jUb8gELQApl", "202207", "5"));
    validateRow(response, List.of("Male", "Others", "fdc6uOvgoji", "202210", "5"));
    validateRow(response, List.of("Male", "Others", "lc3eMKXaEfw", "202209", "5"));
    validateRow(response, List.of("Male", "NVP only", "lc3eMKXaEfw", "202208", "4"));
    validateRow(response, List.of("Male", "NVP only", "lc3eMKXaEfw", "202209", "4"));
    validateRow(response, List.of("Male", "NVP only", "PMa2VCrupOd", "202207", "4"));
    validateRow(response, List.of("Male", "NVP only", "PMa2VCrupOd", "202211", "3"));
    validateRow(response, List.of("Male", "NVP only", "qhqAxPSTUXp", "202207", "3"));
    validateRow(response, List.of("Male", "", "O6uvpzGd5pu", "202208", "1"));
    validateRow(response, List.of("Female", "Others", "kJq2mPyFEHo", "202209", "27"));
    validateRow(response, List.of("Female", "Others", "O6uvpzGd5pu", "202212", "26"));
    validateRow(response, List.of("Female", "NVP only", "at6UHUQatSo", "202212", "24"));
    validateRow(response, List.of("Female", "Others", "TEQlaapDQoK", "202211", "23"));
    validateRow(response, List.of("Female", "NVP only", "kJq2mPyFEHo", "202211", "22"));
    validateRow(response, List.of("Female", "NVP only", "TEQlaapDQoK", "202208", "22"));
    validateRow(response, List.of("Female", "Others", "TEQlaapDQoK", "202207", "22"));
    validateRow(response, List.of("Female", "NVP only", "at6UHUQatSo", "202208", "22"));
    validateRow(response, List.of("Female", "NVP only", "TEQlaapDQoK", "202211", "21"));
    validateRow(response, List.of("Female", "Others", "kJq2mPyFEHo", "202210", "21"));
    validateRow(response, List.of("Female", "NVP only", "O6uvpzGd5pu", "202207", "21"));
    validateRow(response, List.of("Female", "Others", "fdc6uOvgoji", "202210", "20"));
    validateRow(response, List.of("Female", "NVP only", "O6uvpzGd5pu", "202212", "20"));
    validateRow(response, List.of("Female", "NVP only", "kJq2mPyFEHo", "202209", "20"));
    validateRow(response, List.of("Female", "Others", "jmIPBj66vD6", "202208", "20"));
    validateRow(response, List.of("Female", "NVP only", "kJq2mPyFEHo", "202208", "20"));
    validateRow(response, List.of("Female", "Others", "jmIPBj66vD6", "202210", "19"));
    validateRow(response, List.of("Female", "NVP only", "qhqAxPSTUXp", "202211", "19"));
    validateRow(response, List.of("Female", "NVP only", "O6uvpzGd5pu", "202208", "19"));
    validateRow(response, List.of("Female", "NVP only", "eIQbndfxQMb", "202208", "18"));
    validateRow(response, List.of("Female", "Others", "O6uvpzGd5pu", "202208", "18"));
    validateRow(response, List.of("Female", "Others", "at6UHUQatSo", "202210", "18"));
    validateRow(response, List.of("Female", "NVP only", "fdc6uOvgoji", "202209", "18"));
    validateRow(response, List.of("Female", "Others", "TEQlaapDQoK", "202209", "18"));
    validateRow(response, List.of("Female", "NVP only", "O6uvpzGd5pu", "202211", "18"));
    validateRow(response, List.of("Female", "Others", "jUb8gELQApl", "202208", "17"));
    validateRow(response, List.of("Female", "NVP only", "TEQlaapDQoK", "202207", "17"));
    validateRow(response, List.of("Female", "NVP only", "jmIPBj66vD6", "202207", "17"));
    validateRow(response, List.of("Female", "NVP only", "Vth0fbpFcsO", "202209", "17"));
    validateRow(response, List.of("Female", "Others", "at6UHUQatSo", "202208", "17"));
    validateRow(response, List.of("Female", "NVP only", "eIQbndfxQMb", "202211", "17"));
    validateRow(response, List.of("Female", "NVP only", "TEQlaapDQoK", "202209", "17"));
    validateRow(response, List.of("Female", "Others", "Vth0fbpFcsO", "202210", "17"));
    validateRow(response, List.of("Female", "NVP only", "at6UHUQatSo", "202207", "17"));
    validateRow(response, List.of("Female", "NVP only", "O6uvpzGd5pu", "202210", "16"));
    validateRow(response, List.of("Female", "NVP only", "fdc6uOvgoji", "202212", "16"));
    validateRow(response, List.of("Female", "NVP only", "TEQlaapDQoK", "202212", "16"));
    validateRow(response, List.of("Female", "Others", "kJq2mPyFEHo", "202212", "16"));
    validateRow(response, List.of("Female", "Others", "kJq2mPyFEHo", "202211", "16"));
    validateRow(response, List.of("Female", "NVP only", "Vth0fbpFcsO", "202211", "16"));
    validateRow(response, List.of("Female", "Others", "TEQlaapDQoK", "202210", "16"));
    validateRow(response, List.of("Female", "Others", "eIQbndfxQMb", "202210", "15"));
    validateRow(response, List.of("Female", "NVP only", "O6uvpzGd5pu", "202209", "15"));
    validateRow(response, List.of("Female", "Others", "PMa2VCrupOd", "202210", "15"));
    validateRow(response, List.of("Female", "Others", "Vth0fbpFcsO", "202209", "15"));
    validateRow(response, List.of("Female", "Others", "O6uvpzGd5pu", "202210", "15"));
    validateRow(response, List.of("Female", "NVP only", "at6UHUQatSo", "202210", "15"));
    validateRow(response, List.of("Female", "Others", "jUb8gELQApl", "202207", "15"));
    validateRow(response, List.of("Female", "Others", "fdc6uOvgoji", "202207", "15"));
    validateRow(response, List.of("Female", "Others", "O6uvpzGd5pu", "202211", "15"));
    validateRow(response, List.of("Female", "NVP only", "TEQlaapDQoK", "202210", "15"));
    validateRow(response, List.of("Female", "NVP only", "jUb8gELQApl", "202208", "15"));
    validateRow(response, List.of("Female", "NVP only", "jmIPBj66vD6", "202208", "15"));
    validateRow(response, List.of("Female", "Others", "kJq2mPyFEHo", "202207", "15"));
    validateRow(response, List.of("Female", "Others", "Vth0fbpFcsO", "202207", "15"));
    validateRow(response, List.of("Female", "NVP only", "at6UHUQatSo", "202211", "14"));
    validateRow(response, List.of("Female", "Others", "fdc6uOvgoji", "202208", "14"));
    validateRow(response, List.of("Female", "Others", "TEQlaapDQoK", "202208", "14"));
    validateRow(response, List.of("Female", "NVP only", "eIQbndfxQMb", "202207", "14"));
    validateRow(response, List.of("Female", "Others", "fdc6uOvgoji", "202212", "14"));
    validateRow(response, List.of("Female", "NVP only", "kJq2mPyFEHo", "202210", "14"));
    validateRow(response, List.of("Female", "NVP only", "fdc6uOvgoji", "202211", "14"));
    validateRow(response, List.of("Female", "Others", "fdc6uOvgoji", "202211", "14"));
    validateRow(response, List.of("Female", "Others", "O6uvpzGd5pu", "202207", "14"));
    validateRow(response, List.of("Female", "Others", "eIQbndfxQMb", "202209", "13"));
    validateRow(response, List.of("Female", "Others", "TEQlaapDQoK", "202212", "13"));
    validateRow(response, List.of("Female", "Others", "jmIPBj66vD6", "202211", "13"));
    validateRow(response, List.of("Female", "NVP only", "qhqAxPSTUXp", "202209", "13"));
    validateRow(response, List.of("Female", "Others", "Vth0fbpFcsO", "202208", "13"));
    validateRow(response, List.of("Female", "NVP only", "kJq2mPyFEHo", "202207", "13"));
    validateRow(response, List.of("Female", "Others", "eIQbndfxQMb", "202211", "13"));
    validateRow(response, List.of("Female", "NVP only", "eIQbndfxQMb", "202210", "13"));
    validateRow(response, List.of("Female", "NVP only", "bL4ooGhyHRQ", "202208", "13"));
    validateRow(response, List.of("Female", "NVP only", "jmIPBj66vD6", "202212", "13"));
    validateRow(response, List.of("Female", "Others", "jmIPBj66vD6", "202207", "13"));
    validateRow(response, List.of("Female", "Others", "bL4ooGhyHRQ", "202208", "12"));
    validateRow(response, List.of("Female", "NVP only", "jUb8gELQApl", "202210", "12"));
    validateRow(response, List.of("Female", "NVP only", "bL4ooGhyHRQ", "202211", "12"));
    validateRow(response, List.of("Female", "NVP only", "fdc6uOvgoji", "202210", "12"));
    validateRow(response, List.of("Female", "NVP only", "PMa2VCrupOd", "202211", "12"));
    validateRow(response, List.of("Female", "NVP only", "bL4ooGhyHRQ", "202207", "12"));
    validateRow(response, List.of("Female", "NVP only", "jmIPBj66vD6", "202209", "12"));
    validateRow(response, List.of("Female", "NVP only", "Vth0fbpFcsO", "202212", "12"));
    validateRow(response, List.of("Female", "NVP only", "kJq2mPyFEHo", "202212", "12"));
    validateRow(response, List.of("Female", "Others", "bL4ooGhyHRQ", "202210", "12"));
    validateRow(response, List.of("Female", "NVP only", "fdc6uOvgoji", "202207", "12"));
    validateRow(response, List.of("Female", "Others", "kJq2mPyFEHo", "202208", "11"));
    validateRow(response, List.of("Female", "Others", "jmIPBj66vD6", "202209", "11"));
    validateRow(response, List.of("Female", "Others", "bL4ooGhyHRQ", "202211", "11"));
    validateRow(response, List.of("Female", "Others", "at6UHUQatSo", "202211", "11"));
    validateRow(response, List.of("Female", "NVP only", "jUb8gELQApl", "202212", "11"));
    validateRow(response, List.of("Female", "NVP only", "Vth0fbpFcsO", "202207", "11"));
    validateRow(response, List.of("Female", "Others", "qhqAxPSTUXp", "202210", "11"));
    validateRow(response, List.of("Female", "Others", "fdc6uOvgoji", "202209", "11"));
    validateRow(response, List.of("Female", "NVP only", "jmIPBj66vD6", "202211", "11"));
    validateRow(response, List.of("Female", "NVP only", "bL4ooGhyHRQ", "202209", "11"));
    validateRow(response, List.of("Female", "NVP only", "jmIPBj66vD6", "202210", "11"));
    validateRow(response, List.of("Female", "Others", "lc3eMKXaEfw", "202209", "11"));
    validateRow(response, List.of("Female", "Others", "qhqAxPSTUXp", "202209", "11"));
    validateRow(response, List.of("Female", "NVP only", "eIQbndfxQMb", "202209", "11"));
    validateRow(response, List.of("Female", "Others", "O6uvpzGd5pu", "202209", "11"));
    validateRow(response, List.of("Female", "Others", "jmIPBj66vD6", "202212", "11"));
    validateRow(response, List.of("Female", "Others", "at6UHUQatSo", "202207", "10"));
    validateRow(response, List.of("Female", "Others", "qhqAxPSTUXp", "202208", "10"));
    validateRow(response, List.of("Female", "Others", "eIQbndfxQMb", "202207", "10"));
    validateRow(response, List.of("Female", "Others", "jUb8gELQApl", "202209", "10"));
    validateRow(response, List.of("Female", "Others", "at6UHUQatSo", "202209", "10"));
    validateRow(response, List.of("Female", "Others", "eIQbndfxQMb", "202212", "10"));
    validateRow(response, List.of("Female", "Others", "qhqAxPSTUXp", "202212", "10"));
    validateRow(response, List.of("Female", "Others", "qhqAxPSTUXp", "202207", "10"));
    validateRow(response, List.of("Female", "Others", "lc3eMKXaEfw", "202210", "10"));
    validateRow(response, List.of("Female", "Others", "PMa2VCrupOd", "202208", "10"));
    validateRow(response, List.of("Female", "NVP only", "PMa2VCrupOd", "202208", "10"));
    validateRow(response, List.of("Female", "NVP only", "Vth0fbpFcsO", "202208", "10"));
    validateRow(response, List.of("Female", "NVP only", "at6UHUQatSo", "202209", "10"));
    validateRow(response, List.of("Female", "Others", "PMa2VCrupOd", "202212", "9"));
    validateRow(response, List.of("Female", "NVP only", "lc3eMKXaEfw", "202207", "9"));
    validateRow(response, List.of("Female", "NVP only", "jUb8gELQApl", "202207", "9"));
    validateRow(response, List.of("Female", "NVP only", "PMa2VCrupOd", "202212", "9"));
    validateRow(response, List.of("Female", "NVP only", "fdc6uOvgoji", "202208", "9"));
    validateRow(response, List.of("Female", "Others", "PMa2VCrupOd", "202207", "9"));
    validateRow(response, List.of("Female", "Others", "PMa2VCrupOd", "202209", "9"));
    validateRow(response, List.of("Female", "NVP only", "qhqAxPSTUXp", "202208", "9"));
    validateRow(response, List.of("Female", "Others", "eIQbndfxQMb", "202208", "8"));
    validateRow(response, List.of("Female", "NVP only", "jUb8gELQApl", "202209", "8"));
    validateRow(response, List.of("Female", "NVP only", "bL4ooGhyHRQ", "202210", "8"));
    validateRow(response, List.of("Female", "Others", "lc3eMKXaEfw", "202212", "8"));
    validateRow(response, List.of("Female", "NVP only", "bL4ooGhyHRQ", "202212", "8"));
    validateRow(response, List.of("Female", "NVP only", "qhqAxPSTUXp", "202210", "8"));
    validateRow(response, List.of("Female", "Others", "jUb8gELQApl", "202212", "8"));
    validateRow(response, List.of("Female", "NVP only", "PMa2VCrupOd", "202210", "7"));
    validateRow(response, List.of("Female", "NVP only", "lc3eMKXaEfw", "202211", "7"));
    validateRow(response, List.of("Female", "Others", "Vth0fbpFcsO", "202212", "7"));
    validateRow(response, List.of("Female", "Others", "bL4ooGhyHRQ", "202212", "7"));
    validateRow(response, List.of("Female", "Others", "Vth0fbpFcsO", "202211", "7"));
    validateRow(response, List.of("Female", "Others", "lc3eMKXaEfw", "202211", "7"));
    validateRow(response, List.of("Female", "Others", "bL4ooGhyHRQ", "202207", "7"));
    validateRow(response, List.of("Female", "NVP only", "eIQbndfxQMb", "202212", "7"));
    validateRow(response, List.of("Female", "NVP only", "Vth0fbpFcsO", "202210", "7"));
    validateRow(response, List.of("Female", "Others", "qhqAxPSTUXp", "202211", "7"));
    validateRow(response, List.of("Female", "Others", "bL4ooGhyHRQ", "202209", "7"));
    validateRow(response, List.of("Female", "NVP only", "qhqAxPSTUXp", "202207", "6"));
    validateRow(response, List.of("Female", "NVP only", "PMa2VCrupOd", "202209", "6"));
    validateRow(response, List.of("Female", "Others", "at6UHUQatSo", "202212", "6"));
    validateRow(response, List.of("Female", "Others", "PMa2VCrupOd", "202211", "6"));
    validateRow(response, List.of("Female", "NVP only", "lc3eMKXaEfw", "202208", "6"));
    validateRow(response, List.of("Female", "Others", "jUb8gELQApl", "202210", "6"));
    validateRow(response, List.of("Female", "Others", "lc3eMKXaEfw", "202208", "6"));
    validateRow(response, List.of("Female", "NVP only", "qhqAxPSTUXp", "202212", "5"));
    validateRow(response, List.of("Female", "NVP only", "lc3eMKXaEfw", "202210", "5"));
    validateRow(response, List.of("Female", "NVP only", "lc3eMKXaEfw", "202209", "5"));
    validateRow(response, List.of("Female", "NVP only", "jUb8gELQApl", "202211", "5"));
    validateRow(response, List.of("Female", "Others", "lc3eMKXaEfw", "202207", "4"));
    validateRow(response, List.of("Female", "NVP only", "PMa2VCrupOd", "202207", "4"));
    validateRow(response, List.of("Female", "Others", "jUb8gELQApl", "202211", "4"));
    validateRow(response, List.of("Female", "NVP only", "lc3eMKXaEfw", "202212", "4"));
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
    validateRow(response, List.of("3", "1", "ImspTQPwCqd", "202208", "101"));
    validateRow(response, List.of("3", "0", "ImspTQPwCqd", "202210", "92"));
    validateRow(response, List.of("3", "1", "ImspTQPwCqd", "202209", "89"));
    validateRow(response, List.of("3", "1", "ImspTQPwCqd", "202211", "89"));
    validateRow(response, List.of("3", "1", "ImspTQPwCqd", "202212", "86"));
    validateRow(response, List.of("3", "0", "ImspTQPwCqd", "202208", "86"));
    validateRow(response, List.of("3", "0", "ImspTQPwCqd", "202209", "83"));
    validateRow(response, List.of("3", "1", "ImspTQPwCqd", "202207", "81"));
    validateRow(response, List.of("3", "0", "ImspTQPwCqd", "202212", "78"));
    validateRow(response, List.of("3", "0", "ImspTQPwCqd", "202211", "77"));
    validateRow(response, List.of("3", "1", "ImspTQPwCqd", "202210", "77"));
    validateRow(response, List.of("3", "0", "ImspTQPwCqd", "202207", "71"));
    validateRow(response, List.of("3", "0", "ImspTQPwCqd", "202301", "3"));
    validateRow(response, List.of("3", "1", "ImspTQPwCqd", "202301", "2"));
    validateRow(response, List.of("2", "0", "ImspTQPwCqd", "202210", "99"));
    validateRow(response, List.of("2", "0", "ImspTQPwCqd", "202212", "99"));
    validateRow(response, List.of("2", "0", "ImspTQPwCqd", "202208", "94"));
    validateRow(response, List.of("2", "0", "ImspTQPwCqd", "202209", "91"));
    validateRow(response, List.of("2", "1", "ImspTQPwCqd", "202211", "84"));
    validateRow(response, List.of("2", "1", "ImspTQPwCqd", "202210", "84"));
    validateRow(response, List.of("2", "1", "ImspTQPwCqd", "202209", "83"));
    validateRow(response, List.of("2", "1", "ImspTQPwCqd", "202212", "83"));
    validateRow(response, List.of("2", "0", "ImspTQPwCqd", "202211", "78"));
    validateRow(response, List.of("2", "1", "ImspTQPwCqd", "202207", "75"));
    validateRow(response, List.of("2", "0", "ImspTQPwCqd", "202207", "71"));
    validateRow(response, List.of("2", "1", "ImspTQPwCqd", "202208", "69"));
    validateRow(response, List.of("2", "0", "ImspTQPwCqd", "202301", "4"));
    validateRow(response, List.of("1", "0", "ImspTQPwCqd", "202212", "101"));
    validateRow(response, List.of("1", "1", "ImspTQPwCqd", "202207", "101"));
    validateRow(response, List.of("1", "1", "ImspTQPwCqd", "202212", "97"));
    validateRow(response, List.of("1", "0", "ImspTQPwCqd", "202211", "93"));
    validateRow(response, List.of("1", "1", "ImspTQPwCqd", "202209", "92"));
    validateRow(response, List.of("1", "0", "ImspTQPwCqd", "202208", "92"));
    validateRow(response, List.of("1", "1", "ImspTQPwCqd", "202210", "84"));
    validateRow(response, List.of("1", "0", "ImspTQPwCqd", "202210", "83"));
    validateRow(response, List.of("1", "0", "ImspTQPwCqd", "202207", "81"));
    validateRow(response, List.of("1", "1", "ImspTQPwCqd", "202208", "74"));
    validateRow(response, List.of("1", "0", "ImspTQPwCqd", "202209", "74"));
    validateRow(response, List.of("1", "1", "ImspTQPwCqd", "202211", "72"));
    validateRow(response, List.of("1", "0", "ImspTQPwCqd", "202301", "4"));
    validateRow(response, List.of("1", "1", "ImspTQPwCqd", "202301", "2"));
    validateRow(response, List.of("0", "1", "ImspTQPwCqd", "202208", "91"));
    validateRow(response, List.of("0", "0", "ImspTQPwCqd", "202210", "88"));
    validateRow(response, List.of("0", "1", "ImspTQPwCqd", "202212", "86"));
    validateRow(response, List.of("0", "1", "ImspTQPwCqd", "202209", "85"));
    validateRow(response, List.of("0", "0", "ImspTQPwCqd", "202208", "82"));
    validateRow(response, List.of("0", "0", "ImspTQPwCqd", "202212", "80"));
    validateRow(response, List.of("0", "1", "ImspTQPwCqd", "202210", "78"));
    validateRow(response, List.of("0", "1", "ImspTQPwCqd", "202207", "73"));
    validateRow(response, List.of("0", "0", "ImspTQPwCqd", "202209", "71"));
    validateRow(response, List.of("0", "0", "ImspTQPwCqd", "202211", "70"));
    validateRow(response, List.of("0", "1", "ImspTQPwCqd", "202211", "70"));
    validateRow(response, List.of("0", "0", "ImspTQPwCqd", "202207", "66"));
    validateRow(response, List.of("0", "0", "ImspTQPwCqd", "202301", "3"));
    validateRow(response, List.of("0", "1", "ImspTQPwCqd", "202301", "1"));
    validateRow(response, List.of("", "", "ImspTQPwCqd", "202208", "1"));
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
    validateRow(response, List.of("MODTRANS", "", "2022Q3", "ImspTQPwCqd", "55.0"));
    validateRow(response, List.of("MODTRANS", "Female", "2022Q4", "ImspTQPwCqd", "49.6"));
    validateRow(response, List.of("MODTRANS", "Male", "2022Q4", "ImspTQPwCqd", "49.5"));
    validateRow(response, List.of("MODTRANS", "Male", "2022Q3", "ImspTQPwCqd", "49.0"));
    validateRow(response, List.of("MODTRANS", "Female", "2022Q3", "ImspTQPwCqd", "49.0"));
    validateRow(response, List.of("MODDISCH", "Male", "2022Q3", "ImspTQPwCqd", "49.6"));
    validateRow(response, List.of("MODDISCH", "Female", "2022Q3", "ImspTQPwCqd", "49.5"));
    validateRow(response, List.of("MODDISCH", "Female", "2022Q4", "ImspTQPwCqd", "49.4"));
    validateRow(response, List.of("MODDISCH", "Male", "2022Q4", "ImspTQPwCqd", "49.0"));
    validateRow(response, List.of("MODDIED", "Male", "2022Q3", "ImspTQPwCqd", "50.3"));
    validateRow(response, List.of("MODDIED", "Male", "2022Q4", "ImspTQPwCqd", "50.1"));
    validateRow(response, List.of("MODDIED", "Female", "2022Q3", "ImspTQPwCqd", "49.8"));
    validateRow(response, List.of("MODDIED", "Female", "2022Q4", "ImspTQPwCqd", "49.5"));
    validateRow(response, List.of("MODABSC", "Male", "2022Q4", "ImspTQPwCqd", "49.5"));
    validateRow(response, List.of("MODABSC", "Male", "2022Q3", "ImspTQPwCqd", "49.5"));
    validateRow(response, List.of("MODABSC", "Female", "2022Q4", "ImspTQPwCqd", "49.2"));
    validateRow(response, List.of("MODABSC", "Female", "2022Q3", "ImspTQPwCqd", "49.1"));
  }
}
