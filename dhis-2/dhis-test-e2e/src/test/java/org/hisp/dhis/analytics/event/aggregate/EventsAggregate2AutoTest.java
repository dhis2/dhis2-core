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
public class EventsAggregate2AutoTest extends AnalyticsApiTest {
  private final AnalyticsEventActions actions = new AnalyticsEventActions();

  @Test
  public void queryCasesByAge10YearIntervalsLast12Months() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=Zj7UnCAulEk")
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add(
                "dimension=pe:LAST_12_MONTHS,ou:jUb8gELQApl;O6uvpzGd5pu;lc3eMKXaEfw;fdc6uOvgoji,Zj7UnCAulEk.qrur9Dvnyt5-Yf6UHoPkdS6")
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.aggregate().get("eBAyeGv0exc", JSON, JSON, params);
    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(4)))
        .body("rows", hasSize(equalTo(217)))
        .body("height", equalTo(217))
        .body("width", equalTo(4))
        .body("headerWidth", equalTo(4));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"202208\":{\"name\":\"August 2022\"},\"OyVUzWsX8UF\":{\"name\":\"10 - 20\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"202209\":{\"name\":\"September 2022\"},\"202305\":{\"name\":\"May 2023\"},\"qrur9Dvnyt5\":{\"name\":\"Age in years\"},\"202207\":{\"name\":\"July 2022\"},\"202306\":{\"name\":\"June 2023\"},\"pZzk1L4Blf1\":{\"name\":\"0 - 10\"},\"202303\":{\"name\":\"March 2023\"},\"202304\":{\"name\":\"April 2023\"},\"202301\":{\"name\":\"January 2023\"},\"202302\":{\"name\":\"February 2023\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"},\"b7MCpzqJaR2\":{\"name\":\"70 - 80\"},\"202211\":{\"name\":\"November 2022\"},\"202212\":{\"name\":\"December 2022\"},\"202210\":{\"name\":\"October 2022\"},\"Tq4NYCn9eNH\":{\"name\":\"60 - 70\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"ou\":{\"name\":\"Organisation unit\"},\"CpP5yzbgfHo\":{\"name\":\"40 - 50\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"scvmgP9F9rn\":{\"name\":\"90 - 100\"},\"cbPqyIAFw9u\":{\"name\":\"50 - 60\"},\"TvM2MQgD7Jd\":{\"name\":\"20 - 30\"},\"pe\":{\"name\":\"Period\"},\"puI3YpLJ3fC\":{\"name\":\"80 - 90\"},\"ZUUGJnvX40X\":{\"name\":\"30 - 40\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"}},\"dimensions\":{\"pe\":[\"202207\",\"202208\",\"202209\",\"202210\",\"202211\",\"202212\",\"202301\",\"202302\",\"202303\",\"202304\",\"202305\",\"202306\"],\"Zj7UnCAulEk.qrur9Dvnyt5\":[\"pZzk1L4Blf1\",\"OyVUzWsX8UF\",\"TvM2MQgD7Jd\",\"ZUUGJnvX40X\",\"CpP5yzbgfHo\",\"cbPqyIAFw9u\",\"Tq4NYCn9eNH\",\"b7MCpzqJaR2\",\"puI3YpLJ3fC\",\"scvmgP9F9rn\"],\"ou\":[\"jUb8gELQApl\",\"O6uvpzGd5pu\",\"lc3eMKXaEfw\",\"fdc6uOvgoji\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "qrur9Dvnyt5", "Age in years", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, 0, List.of("puI3YpLJ3fC", "O6uvpzGd5pu", "202208", "59"));
    validateRow(response, 1, List.of("puI3YpLJ3fC", "O6uvpzGd5pu", "202207", "52"));
    validateRow(response, 2, List.of("puI3YpLJ3fC", "O6uvpzGd5pu", "202209", "50"));
    validateRow(response, 3, List.of("puI3YpLJ3fC", "fdc6uOvgoji", "202207", "45"));
    validateRow(response, 4, List.of("puI3YpLJ3fC", "O6uvpzGd5pu", "202210", "45"));
    validateRow(response, 5, List.of("puI3YpLJ3fC", "O6uvpzGd5pu", "202212", "44"));
    validateRow(response, 6, List.of("puI3YpLJ3fC", "O6uvpzGd5pu", "202211", "44"));
    validateRow(response, 7, List.of("puI3YpLJ3fC", "fdc6uOvgoji", "202210", "40"));
    validateRow(response, 8, List.of("puI3YpLJ3fC", "fdc6uOvgoji", "202209", "39"));
    validateRow(response, 9, List.of("puI3YpLJ3fC", "fdc6uOvgoji", "202212", "37"));
    validateRow(response, 10, List.of("puI3YpLJ3fC", "jUb8gELQApl", "202208", "35"));
    validateRow(response, 11, List.of("puI3YpLJ3fC", "fdc6uOvgoji", "202208", "34"));
    validateRow(response, 12, List.of("puI3YpLJ3fC", "jUb8gELQApl", "202211", "32"));
    validateRow(response, 13, List.of("puI3YpLJ3fC", "jUb8gELQApl", "202209", "30"));
    validateRow(response, 14, List.of("puI3YpLJ3fC", "jUb8gELQApl", "202207", "30"));
    validateRow(response, 15, List.of("puI3YpLJ3fC", "lc3eMKXaEfw", "202208", "28"));
    validateRow(response, 16, List.of("puI3YpLJ3fC", "fdc6uOvgoji", "202211", "26"));
    validateRow(response, 17, List.of("puI3YpLJ3fC", "lc3eMKXaEfw", "202210", "25"));
    validateRow(response, 18, List.of("puI3YpLJ3fC", "jUb8gELQApl", "202212", "24"));
    validateRow(response, 19, List.of("puI3YpLJ3fC", "lc3eMKXaEfw", "202209", "23"));
    validateRow(response, 20, List.of("puI3YpLJ3fC", "lc3eMKXaEfw", "202207", "22"));
    validateRow(response, 21, List.of("puI3YpLJ3fC", "lc3eMKXaEfw", "202212", "21"));
    validateRow(response, 22, List.of("puI3YpLJ3fC", "jUb8gELQApl", "202210", "20"));
    validateRow(response, 23, List.of("puI3YpLJ3fC", "lc3eMKXaEfw", "202211", "15"));
    validateRow(response, 24, List.of("pZzk1L4Blf1", "O6uvpzGd5pu", "202207", "60"));
    validateRow(response, 25, List.of("pZzk1L4Blf1", "O6uvpzGd5pu", "202211", "57"));
    validateRow(response, 26, List.of("pZzk1L4Blf1", "O6uvpzGd5pu", "202209", "56"));
    validateRow(response, 27, List.of("pZzk1L4Blf1", "O6uvpzGd5pu", "202210", "54"));
    validateRow(response, 28, List.of("pZzk1L4Blf1", "fdc6uOvgoji", "202207", "51"));
    validateRow(response, 29, List.of("pZzk1L4Blf1", "jUb8gELQApl", "202209", "46"));
    validateRow(response, 30, List.of("pZzk1L4Blf1", "fdc6uOvgoji", "202209", "46"));
    validateRow(response, 31, List.of("pZzk1L4Blf1", "O6uvpzGd5pu", "202208", "46"));
    validateRow(response, 32, List.of("pZzk1L4Blf1", "fdc6uOvgoji", "202211", "45"));
    validateRow(response, 33, List.of("pZzk1L4Blf1", "O6uvpzGd5pu", "202212", "43"));
    validateRow(response, 34, List.of("pZzk1L4Blf1", "fdc6uOvgoji", "202210", "42"));
    validateRow(response, 35, List.of("pZzk1L4Blf1", "fdc6uOvgoji", "202212", "41"));
    validateRow(response, 36, List.of("pZzk1L4Blf1", "jUb8gELQApl", "202212", "34"));
    validateRow(response, 37, List.of("pZzk1L4Blf1", "fdc6uOvgoji", "202208", "33"));
    validateRow(response, 38, List.of("pZzk1L4Blf1", "jUb8gELQApl", "202208", "32"));
    validateRow(response, 39, List.of("pZzk1L4Blf1", "jUb8gELQApl", "202207", "29"));
    validateRow(response, 40, List.of("pZzk1L4Blf1", "jUb8gELQApl", "202210", "28"));
    validateRow(response, 41, List.of("pZzk1L4Blf1", "jUb8gELQApl", "202211", "28"));
    validateRow(response, 42, List.of("pZzk1L4Blf1", "lc3eMKXaEfw", "202209", "25"));
    validateRow(response, 43, List.of("pZzk1L4Blf1", "lc3eMKXaEfw", "202208", "23"));
    validateRow(response, 44, List.of("pZzk1L4Blf1", "lc3eMKXaEfw", "202212", "23"));
    validateRow(response, 45, List.of("pZzk1L4Blf1", "lc3eMKXaEfw", "202211", "21"));
    validateRow(response, 46, List.of("pZzk1L4Blf1", "lc3eMKXaEfw", "202207", "18"));
    validateRow(response, 47, List.of("pZzk1L4Blf1", "lc3eMKXaEfw", "202210", "18"));
    validateRow(response, 48, List.of("cbPqyIAFw9u", "O6uvpzGd5pu", "202208", "66"));
    validateRow(response, 49, List.of("cbPqyIAFw9u", "O6uvpzGd5pu", "202209", "64"));
    validateRow(response, 50, List.of("cbPqyIAFw9u", "O6uvpzGd5pu", "202210", "62"));
    validateRow(response, 51, List.of("cbPqyIAFw9u", "O6uvpzGd5pu", "202207", "59"));
    validateRow(response, 52, List.of("cbPqyIAFw9u", "O6uvpzGd5pu", "202212", "57"));
    validateRow(response, 53, List.of("cbPqyIAFw9u", "O6uvpzGd5pu", "202211", "52"));
    validateRow(response, 54, List.of("cbPqyIAFw9u", "fdc6uOvgoji", "202208", "51"));
    validateRow(response, 55, List.of("cbPqyIAFw9u", "fdc6uOvgoji", "202210", "48"));
    validateRow(response, 56, List.of("cbPqyIAFw9u", "fdc6uOvgoji", "202209", "44"));
    validateRow(response, 57, List.of("cbPqyIAFw9u", "fdc6uOvgoji", "202211", "44"));
    validateRow(response, 58, List.of("cbPqyIAFw9u", "fdc6uOvgoji", "202207", "43"));
    validateRow(response, 59, List.of("cbPqyIAFw9u", "jUb8gELQApl", "202209", "37"));
    validateRow(response, 60, List.of("cbPqyIAFw9u", "fdc6uOvgoji", "202212", "35"));
    validateRow(response, 61, List.of("cbPqyIAFw9u", "jUb8gELQApl", "202211", "35"));
    validateRow(response, 62, List.of("cbPqyIAFw9u", "jUb8gELQApl", "202207", "31"));
    validateRow(response, 63, List.of("cbPqyIAFw9u", "lc3eMKXaEfw", "202207", "29"));
    validateRow(response, 64, List.of("cbPqyIAFw9u", "jUb8gELQApl", "202208", "29"));
    validateRow(response, 65, List.of("cbPqyIAFw9u", "jUb8gELQApl", "202212", "28"));
    validateRow(response, 66, List.of("cbPqyIAFw9u", "jUb8gELQApl", "202210", "28"));
    validateRow(response, 67, List.of("cbPqyIAFw9u", "lc3eMKXaEfw", "202211", "26"));
    validateRow(response, 68, List.of("cbPqyIAFw9u", "lc3eMKXaEfw", "202208", "22"));
    validateRow(response, 69, List.of("cbPqyIAFw9u", "lc3eMKXaEfw", "202210", "20"));
    validateRow(response, 70, List.of("cbPqyIAFw9u", "lc3eMKXaEfw", "202209", "16"));
    validateRow(response, 71, List.of("cbPqyIAFw9u", "lc3eMKXaEfw", "202212", "15"));
    validateRow(response, 72, List.of("b7MCpzqJaR2", "O6uvpzGd5pu", "202208", "74"));
    validateRow(response, 73, List.of("b7MCpzqJaR2", "O6uvpzGd5pu", "202212", "66"));
    validateRow(response, 74, List.of("b7MCpzqJaR2", "O6uvpzGd5pu", "202209", "62"));
    validateRow(response, 75, List.of("b7MCpzqJaR2", "O6uvpzGd5pu", "202210", "57"));
    validateRow(response, 76, List.of("b7MCpzqJaR2", "O6uvpzGd5pu", "202207", "56"));
    validateRow(response, 77, List.of("b7MCpzqJaR2", "O6uvpzGd5pu", "202211", "55"));
    validateRow(response, 78, List.of("b7MCpzqJaR2", "fdc6uOvgoji", "202208", "52"));
    validateRow(response, 79, List.of("b7MCpzqJaR2", "jUb8gELQApl", "202208", "49"));
    validateRow(response, 80, List.of("b7MCpzqJaR2", "fdc6uOvgoji", "202210", "48"));
    validateRow(response, 81, List.of("b7MCpzqJaR2", "fdc6uOvgoji", "202207", "47"));
    validateRow(response, 82, List.of("b7MCpzqJaR2", "fdc6uOvgoji", "202209", "46"));
    validateRow(response, 83, List.of("b7MCpzqJaR2", "fdc6uOvgoji", "202211", "45"));
    validateRow(response, 84, List.of("b7MCpzqJaR2", "jUb8gELQApl", "202211", "36"));
    validateRow(response, 85, List.of("b7MCpzqJaR2", "fdc6uOvgoji", "202212", "33"));
    validateRow(response, 86, List.of("b7MCpzqJaR2", "jUb8gELQApl", "202207", "32"));
    validateRow(response, 87, List.of("b7MCpzqJaR2", "lc3eMKXaEfw", "202211", "31"));
    validateRow(response, 88, List.of("b7MCpzqJaR2", "jUb8gELQApl", "202210", "29"));
    validateRow(response, 89, List.of("b7MCpzqJaR2", "lc3eMKXaEfw", "202209", "28"));
    validateRow(response, 90, List.of("b7MCpzqJaR2", "lc3eMKXaEfw", "202210", "28"));
    validateRow(response, 91, List.of("b7MCpzqJaR2", "jUb8gELQApl", "202212", "27"));
    validateRow(response, 92, List.of("b7MCpzqJaR2", "lc3eMKXaEfw", "202208", "27"));
    validateRow(response, 93, List.of("b7MCpzqJaR2", "jUb8gELQApl", "202209", "26"));
    validateRow(response, 94, List.of("b7MCpzqJaR2", "lc3eMKXaEfw", "202207", "20"));
    validateRow(response, 95, List.of("b7MCpzqJaR2", "lc3eMKXaEfw", "202212", "19"));
    validateRow(response, 96, List.of("ZUUGJnvX40X", "O6uvpzGd5pu", "202210", "63"));
    validateRow(response, 97, List.of("ZUUGJnvX40X", "O6uvpzGd5pu", "202207", "60"));
    validateRow(response, 98, List.of("ZUUGJnvX40X", "O6uvpzGd5pu", "202209", "56"));
    validateRow(response, 99, List.of("ZUUGJnvX40X", "fdc6uOvgoji", "202210", "56"));
    validateRow(response, 100, List.of("ZUUGJnvX40X", "O6uvpzGd5pu", "202208", "55"));
    validateRow(response, 101, List.of("ZUUGJnvX40X", "fdc6uOvgoji", "202208", "48"));
    validateRow(response, 102, List.of("ZUUGJnvX40X", "fdc6uOvgoji", "202212", "47"));
    validateRow(response, 103, List.of("ZUUGJnvX40X", "fdc6uOvgoji", "202211", "47"));
    validateRow(response, 104, List.of("ZUUGJnvX40X", "O6uvpzGd5pu", "202212", "46"));
    validateRow(response, 105, List.of("ZUUGJnvX40X", "O6uvpzGd5pu", "202211", "42"));
    validateRow(response, 106, List.of("ZUUGJnvX40X", "fdc6uOvgoji", "202207", "40"));
    validateRow(response, 107, List.of("ZUUGJnvX40X", "fdc6uOvgoji", "202209", "40"));
    validateRow(response, 108, List.of("ZUUGJnvX40X", "jUb8gELQApl", "202207", "32"));
    validateRow(response, 109, List.of("ZUUGJnvX40X", "jUb8gELQApl", "202212", "30"));
    validateRow(response, 110, List.of("ZUUGJnvX40X", "jUb8gELQApl", "202210", "29"));
    validateRow(response, 111, List.of("ZUUGJnvX40X", "jUb8gELQApl", "202208", "29"));
    validateRow(response, 112, List.of("ZUUGJnvX40X", "lc3eMKXaEfw", "202211", "29"));
    validateRow(response, 113, List.of("ZUUGJnvX40X", "jUb8gELQApl", "202211", "28"));
    validateRow(response, 114, List.of("ZUUGJnvX40X", "jUb8gELQApl", "202209", "26"));
    validateRow(response, 115, List.of("ZUUGJnvX40X", "lc3eMKXaEfw", "202208", "25"));
    validateRow(response, 116, List.of("ZUUGJnvX40X", "lc3eMKXaEfw", "202209", "25"));
    validateRow(response, 117, List.of("ZUUGJnvX40X", "lc3eMKXaEfw", "202210", "20"));
    validateRow(response, 118, List.of("ZUUGJnvX40X", "lc3eMKXaEfw", "202207", "19"));
    validateRow(response, 119, List.of("ZUUGJnvX40X", "lc3eMKXaEfw", "202212", "18"));
    validateRow(response, 120, List.of("TvM2MQgD7Jd", "fdc6uOvgoji", "202210", "67"));
    validateRow(response, 121, List.of("TvM2MQgD7Jd", "O6uvpzGd5pu", "202209", "65"));
    validateRow(response, 122, List.of("TvM2MQgD7Jd", "O6uvpzGd5pu", "202211", "59"));
    validateRow(response, 123, List.of("TvM2MQgD7Jd", "O6uvpzGd5pu", "202207", "58"));
    validateRow(response, 124, List.of("TvM2MQgD7Jd", "O6uvpzGd5pu", "202212", "48"));
    validateRow(response, 125, List.of("TvM2MQgD7Jd", "O6uvpzGd5pu", "202210", "48"));
    validateRow(response, 126, List.of("TvM2MQgD7Jd", "O6uvpzGd5pu", "202208", "45"));
    validateRow(response, 127, List.of("TvM2MQgD7Jd", "fdc6uOvgoji", "202209", "44"));
    validateRow(response, 128, List.of("TvM2MQgD7Jd", "fdc6uOvgoji", "202212", "42"));
    validateRow(response, 129, List.of("TvM2MQgD7Jd", "fdc6uOvgoji", "202211", "41"));
    validateRow(response, 130, List.of("TvM2MQgD7Jd", "fdc6uOvgoji", "202207", "40"));
    validateRow(response, 131, List.of("TvM2MQgD7Jd", "jUb8gELQApl", "202210", "36"));
    validateRow(response, 132, List.of("TvM2MQgD7Jd", "lc3eMKXaEfw", "202207", "36"));
    validateRow(response, 133, List.of("TvM2MQgD7Jd", "jUb8gELQApl", "202212", "35"));
    validateRow(response, 134, List.of("TvM2MQgD7Jd", "jUb8gELQApl", "202208", "32"));
    validateRow(response, 135, List.of("TvM2MQgD7Jd", "jUb8gELQApl", "202211", "32"));
    validateRow(response, 136, List.of("TvM2MQgD7Jd", "jUb8gELQApl", "202209", "30"));
    validateRow(response, 137, List.of("TvM2MQgD7Jd", "fdc6uOvgoji", "202208", "29"));
    validateRow(response, 138, List.of("TvM2MQgD7Jd", "jUb8gELQApl", "202207", "27"));
    validateRow(response, 139, List.of("TvM2MQgD7Jd", "lc3eMKXaEfw", "202209", "26"));
    validateRow(response, 140, List.of("TvM2MQgD7Jd", "lc3eMKXaEfw", "202210", "25"));
    validateRow(response, 141, List.of("TvM2MQgD7Jd", "lc3eMKXaEfw", "202212", "22"));
    validateRow(response, 142, List.of("TvM2MQgD7Jd", "lc3eMKXaEfw", "202211", "18"));
    validateRow(response, 143, List.of("TvM2MQgD7Jd", "lc3eMKXaEfw", "202208", "14"));
    validateRow(response, 144, List.of("Tq4NYCn9eNH", "fdc6uOvgoji", "202210", "59"));
    validateRow(response, 145, List.of("Tq4NYCn9eNH", "O6uvpzGd5pu", "202208", "57"));
    validateRow(response, 146, List.of("Tq4NYCn9eNH", "fdc6uOvgoji", "202211", "57"));
    validateRow(response, 147, List.of("Tq4NYCn9eNH", "O6uvpzGd5pu", "202212", "57"));
    validateRow(response, 148, List.of("Tq4NYCn9eNH", "O6uvpzGd5pu", "202209", "48"));
    validateRow(response, 149, List.of("Tq4NYCn9eNH", "O6uvpzGd5pu", "202207", "48"));
    validateRow(response, 150, List.of("Tq4NYCn9eNH", "O6uvpzGd5pu", "202210", "47"));
    validateRow(response, 151, List.of("Tq4NYCn9eNH", "fdc6uOvgoji", "202207", "47"));
    validateRow(response, 152, List.of("Tq4NYCn9eNH", "O6uvpzGd5pu", "202211", "45"));
    validateRow(response, 153, List.of("Tq4NYCn9eNH", "jUb8gELQApl", "202208", "40"));
    validateRow(response, 154, List.of("Tq4NYCn9eNH", "jUb8gELQApl", "202210", "38"));
    validateRow(response, 155, List.of("Tq4NYCn9eNH", "fdc6uOvgoji", "202212", "38"));
    validateRow(response, 156, List.of("Tq4NYCn9eNH", "jUb8gELQApl", "202212", "36"));
    validateRow(response, 157, List.of("Tq4NYCn9eNH", "fdc6uOvgoji", "202208", "35"));
    validateRow(response, 158, List.of("Tq4NYCn9eNH", "jUb8gELQApl", "202207", "35"));
    validateRow(response, 159, List.of("Tq4NYCn9eNH", "fdc6uOvgoji", "202209", "32"));
    validateRow(response, 160, List.of("Tq4NYCn9eNH", "lc3eMKXaEfw", "202211", "31"));
    validateRow(response, 161, List.of("Tq4NYCn9eNH", "lc3eMKXaEfw", "202212", "28"));
    validateRow(response, 162, List.of("Tq4NYCn9eNH", "lc3eMKXaEfw", "202208", "26"));
    validateRow(response, 163, List.of("Tq4NYCn9eNH", "jUb8gELQApl", "202211", "24"));
    validateRow(response, 164, List.of("Tq4NYCn9eNH", "lc3eMKXaEfw", "202210", "23"));
    validateRow(response, 165, List.of("Tq4NYCn9eNH", "lc3eMKXaEfw", "202207", "22"));
    validateRow(response, 166, List.of("Tq4NYCn9eNH", "jUb8gELQApl", "202209", "22"));
    validateRow(response, 167, List.of("Tq4NYCn9eNH", "lc3eMKXaEfw", "202209", "13"));
    validateRow(response, 168, List.of("OyVUzWsX8UF", "O6uvpzGd5pu", "202208", "59"));
    validateRow(response, 169, List.of("OyVUzWsX8UF", "O6uvpzGd5pu", "202212", "59"));
    validateRow(response, 170, List.of("OyVUzWsX8UF", "O6uvpzGd5pu", "202209", "58"));
    validateRow(response, 171, List.of("OyVUzWsX8UF", "O6uvpzGd5pu", "202211", "56"));
    validateRow(response, 172, List.of("OyVUzWsX8UF", "O6uvpzGd5pu", "202207", "54"));
    validateRow(response, 173, List.of("OyVUzWsX8UF", "fdc6uOvgoji", "202210", "53"));
    validateRow(response, 174, List.of("OyVUzWsX8UF", "O6uvpzGd5pu", "202210", "46"));
    validateRow(response, 175, List.of("OyVUzWsX8UF", "fdc6uOvgoji", "202212", "46"));
    validateRow(response, 176, List.of("OyVUzWsX8UF", "fdc6uOvgoji", "202208", "44"));
    validateRow(response, 177, List.of("OyVUzWsX8UF", "jUb8gELQApl", "202210", "42"));
    validateRow(response, 178, List.of("OyVUzWsX8UF", "fdc6uOvgoji", "202211", "39"));
    validateRow(response, 179, List.of("OyVUzWsX8UF", "fdc6uOvgoji", "202209", "38"));
    validateRow(response, 180, List.of("OyVUzWsX8UF", "fdc6uOvgoji", "202207", "37"));
    validateRow(response, 181, List.of("OyVUzWsX8UF", "jUb8gELQApl", "202211", "37"));
    validateRow(response, 182, List.of("OyVUzWsX8UF", "jUb8gELQApl", "202212", "34"));
    validateRow(response, 183, List.of("OyVUzWsX8UF", "jUb8gELQApl", "202208", "32"));
    validateRow(response, 184, List.of("OyVUzWsX8UF", "jUb8gELQApl", "202207", "32"));
    validateRow(response, 185, List.of("OyVUzWsX8UF", "lc3eMKXaEfw", "202209", "30"));
    validateRow(response, 186, List.of("OyVUzWsX8UF", "jUb8gELQApl", "202209", "24"));
    validateRow(response, 187, List.of("OyVUzWsX8UF", "lc3eMKXaEfw", "202208", "24"));
    validateRow(response, 188, List.of("OyVUzWsX8UF", "lc3eMKXaEfw", "202211", "24"));
    validateRow(response, 189, List.of("OyVUzWsX8UF", "lc3eMKXaEfw", "202212", "22"));
    validateRow(response, 190, List.of("OyVUzWsX8UF", "lc3eMKXaEfw", "202210", "21"));
    validateRow(response, 191, List.of("OyVUzWsX8UF", "lc3eMKXaEfw", "202207", "20"));
    validateRow(response, 192, List.of("CpP5yzbgfHo", "O6uvpzGd5pu", "202207", "68"));
    validateRow(response, 193, List.of("CpP5yzbgfHo", "O6uvpzGd5pu", "202211", "62"));
    validateRow(response, 194, List.of("CpP5yzbgfHo", "O6uvpzGd5pu", "202210", "58"));
    validateRow(response, 195, List.of("CpP5yzbgfHo", "O6uvpzGd5pu", "202212", "55"));
    validateRow(response, 196, List.of("CpP5yzbgfHo", "O6uvpzGd5pu", "202209", "52"));
    validateRow(response, 197, List.of("CpP5yzbgfHo", "fdc6uOvgoji", "202207", "50"));
    validateRow(response, 198, List.of("CpP5yzbgfHo", "fdc6uOvgoji", "202212", "48"));
    validateRow(response, 199, List.of("CpP5yzbgfHo", "fdc6uOvgoji", "202208", "47"));
    validateRow(response, 200, List.of("CpP5yzbgfHo", "O6uvpzGd5pu", "202208", "45"));
    validateRow(response, 201, List.of("CpP5yzbgfHo", "fdc6uOvgoji", "202210", "41"));
    validateRow(response, 202, List.of("CpP5yzbgfHo", "fdc6uOvgoji", "202211", "38"));
    validateRow(response, 203, List.of("CpP5yzbgfHo", "jUb8gELQApl", "202209", "35"));
    validateRow(response, 204, List.of("CpP5yzbgfHo", "jUb8gELQApl", "202210", "34"));
    validateRow(response, 205, List.of("CpP5yzbgfHo", "jUb8gELQApl", "202212", "34"));
    validateRow(response, 206, List.of("CpP5yzbgfHo", "jUb8gELQApl", "202207", "34"));
    validateRow(response, 207, List.of("CpP5yzbgfHo", "jUb8gELQApl", "202208", "30"));
    validateRow(response, 208, List.of("CpP5yzbgfHo", "lc3eMKXaEfw", "202207", "29"));
    validateRow(response, 209, List.of("CpP5yzbgfHo", "jUb8gELQApl", "202211", "28"));
    validateRow(response, 210, List.of("CpP5yzbgfHo", "fdc6uOvgoji", "202209", "27"));
    validateRow(response, 211, List.of("CpP5yzbgfHo", "lc3eMKXaEfw", "202210", "26"));
    validateRow(response, 212, List.of("CpP5yzbgfHo", "lc3eMKXaEfw", "202209", "24"));
    validateRow(response, 213, List.of("CpP5yzbgfHo", "lc3eMKXaEfw", "202212", "23"));
    validateRow(response, 214, List.of("CpP5yzbgfHo", "lc3eMKXaEfw", "202208", "20"));
    validateRow(response, 215, List.of("CpP5yzbgfHo", "lc3eMKXaEfw", "202211", "16"));
    validateRow(response, 216, List.of("", "O6uvpzGd5pu", "202208", "3"));
  }

  @Test
  public void queryCasesByAge15YearIntervalsLast12Months() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=Zj7UnCAulEk")
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add(
                "dimension=pe:LAST_12_MONTHS,ou:jUb8gELQApl;O6uvpzGd5pu;lc3eMKXaEfw;fdc6uOvgoji,Zj7UnCAulEk.qrur9Dvnyt5-TiOkbpGEud4")
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.aggregate().get("eBAyeGv0exc", JSON, JSON, params);
    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(4)))
        .body("rows", hasSize(equalTo(145)))
        .body("height", equalTo(145))
        .body("width", equalTo(4))
        .body("headerWidth", equalTo(4));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"kEf6QhFVMab\":{\"name\":\"15 - 30\"},\"aeCp6thd8zL\":{\"name\":\"90 - 105\"},\"202208\":{\"name\":\"August 2022\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"202209\":{\"name\":\"September 2022\"},\"202305\":{\"name\":\"May 2023\"},\"qrur9Dvnyt5\":{\"name\":\"Age in years\"},\"202207\":{\"name\":\"July 2022\"},\"202306\":{\"name\":\"June 2023\"},\"202303\":{\"name\":\"March 2023\"},\"202304\":{\"name\":\"April 2023\"},\"202301\":{\"name\":\"January 2023\"},\"202302\":{\"name\":\"February 2023\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"},\"202211\":{\"name\":\"November 2022\"},\"202212\":{\"name\":\"December 2022\"},\"ETdvuOmTpc6\":{\"name\":\"30 - 45\"},\"202210\":{\"name\":\"October 2022\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"FWciVWWrPMr\":{\"name\":\"75 - 90\"},\"xpC4lomA8aD\":{\"name\":\"60 - 75\"},\"rlXteEDaTpt\":{\"name\":\"0 - 15\"},\"ou\":{\"name\":\"Organisation unit\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"BzQkRWHS7lu\":{\"name\":\"45 - 60\"},\"pe\":{\"name\":\"Period\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"}},\"dimensions\":{\"pe\":[\"202207\",\"202208\",\"202209\",\"202210\",\"202211\",\"202212\",\"202301\",\"202302\",\"202303\",\"202304\",\"202305\",\"202306\"],\"Zj7UnCAulEk.qrur9Dvnyt5\":[\"rlXteEDaTpt\",\"kEf6QhFVMab\",\"ETdvuOmTpc6\",\"BzQkRWHS7lu\",\"xpC4lomA8aD\",\"FWciVWWrPMr\",\"aeCp6thd8zL\"],\"ou\":[\"jUb8gELQApl\",\"O6uvpzGd5pu\",\"lc3eMKXaEfw\",\"fdc6uOvgoji\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "qrur9Dvnyt5", "Age in years", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, 0, List.of("xpC4lomA8aD", "O6uvpzGd5pu", "202208", "96"));
    validateRow(response, 1, List.of("xpC4lomA8aD", "O6uvpzGd5pu", "202212", "82"));
    validateRow(response, 2, List.of("xpC4lomA8aD", "fdc6uOvgoji", "202211", "78"));
    validateRow(response, 3, List.of("xpC4lomA8aD", "fdc6uOvgoji", "202210", "78"));
    validateRow(response, 4, List.of("xpC4lomA8aD", "O6uvpzGd5pu", "202207", "76"));
    validateRow(response, 5, List.of("xpC4lomA8aD", "O6uvpzGd5pu", "202210", "75"));
    validateRow(response, 6, List.of("xpC4lomA8aD", "O6uvpzGd5pu", "202211", "75"));
    validateRow(response, 7, List.of("xpC4lomA8aD", "O6uvpzGd5pu", "202209", "74"));
    validateRow(response, 8, List.of("xpC4lomA8aD", "fdc6uOvgoji", "202207", "71"));
    validateRow(response, 9, List.of("xpC4lomA8aD", "jUb8gELQApl", "202208", "65"));
    validateRow(response, 10, List.of("xpC4lomA8aD", "fdc6uOvgoji", "202208", "63"));
    validateRow(response, 11, List.of("xpC4lomA8aD", "fdc6uOvgoji", "202209", "53"));
    validateRow(response, 12, List.of("xpC4lomA8aD", "jUb8gELQApl", "202207", "51"));
    validateRow(response, 13, List.of("xpC4lomA8aD", "fdc6uOvgoji", "202212", "51"));
    validateRow(response, 14, List.of("xpC4lomA8aD", "jUb8gELQApl", "202210", "50"));
    validateRow(response, 15, List.of("xpC4lomA8aD", "jUb8gELQApl", "202212", "49"));
    validateRow(response, 16, List.of("xpC4lomA8aD", "lc3eMKXaEfw", "202211", "47"));
    validateRow(response, 17, List.of("xpC4lomA8aD", "jUb8gELQApl", "202211", "45"));
    validateRow(response, 18, List.of("xpC4lomA8aD", "lc3eMKXaEfw", "202208", "40"));
    validateRow(response, 19, List.of("xpC4lomA8aD", "jUb8gELQApl", "202209", "38"));
    validateRow(response, 20, List.of("xpC4lomA8aD", "lc3eMKXaEfw", "202212", "37"));
    validateRow(response, 21, List.of("xpC4lomA8aD", "lc3eMKXaEfw", "202207", "36"));
    validateRow(response, 22, List.of("xpC4lomA8aD", "lc3eMKXaEfw", "202210", "32"));
    validateRow(response, 23, List.of("xpC4lomA8aD", "lc3eMKXaEfw", "202209", "27"));
    validateRow(response, 24, List.of("rlXteEDaTpt", "O6uvpzGd5pu", "202211", "89"));
    validateRow(response, 25, List.of("rlXteEDaTpt", "O6uvpzGd5pu", "202209", "87"));
    validateRow(response, 26, List.of("rlXteEDaTpt", "O6uvpzGd5pu", "202207", "85"));
    validateRow(response, 27, List.of("rlXteEDaTpt", "O6uvpzGd5pu", "202208", "74"));
    validateRow(response, 28, List.of("rlXteEDaTpt", "O6uvpzGd5pu", "202212", "72"));
    validateRow(response, 29, List.of("rlXteEDaTpt", "O6uvpzGd5pu", "202210", "70"));
    validateRow(response, 30, List.of("rlXteEDaTpt", "fdc6uOvgoji", "202207", "70"));
    validateRow(response, 31, List.of("rlXteEDaTpt", "fdc6uOvgoji", "202210", "69"));
    validateRow(response, 32, List.of("rlXteEDaTpt", "fdc6uOvgoji", "202211", "68"));
    validateRow(response, 33, List.of("rlXteEDaTpt", "fdc6uOvgoji", "202209", "67"));
    validateRow(response, 34, List.of("rlXteEDaTpt", "fdc6uOvgoji", "202212", "63"));
    validateRow(response, 35, List.of("rlXteEDaTpt", "jUb8gELQApl", "202209", "62"));
    validateRow(response, 36, List.of("rlXteEDaTpt", "jUb8gELQApl", "202210", "57"));
    validateRow(response, 37, List.of("rlXteEDaTpt", "fdc6uOvgoji", "202208", "50"));
    validateRow(response, 38, List.of("rlXteEDaTpt", "jUb8gELQApl", "202212", "49"));
    validateRow(response, 39, List.of("rlXteEDaTpt", "jUb8gELQApl", "202208", "49"));
    validateRow(response, 40, List.of("rlXteEDaTpt", "jUb8gELQApl", "202207", "45"));
    validateRow(response, 41, List.of("rlXteEDaTpt", "jUb8gELQApl", "202211", "44"));
    validateRow(response, 42, List.of("rlXteEDaTpt", "lc3eMKXaEfw", "202209", "39"));
    validateRow(response, 43, List.of("rlXteEDaTpt", "lc3eMKXaEfw", "202212", "33"));
    validateRow(response, 44, List.of("rlXteEDaTpt", "lc3eMKXaEfw", "202208", "33"));
    validateRow(response, 45, List.of("rlXteEDaTpt", "lc3eMKXaEfw", "202211", "32"));
    validateRow(response, 46, List.of("rlXteEDaTpt", "lc3eMKXaEfw", "202210", "30"));
    validateRow(response, 47, List.of("rlXteEDaTpt", "lc3eMKXaEfw", "202207", "28"));
    validateRow(response, 48, List.of("kEf6QhFVMab", "fdc6uOvgoji", "202210", "93"));
    validateRow(response, 49, List.of("kEf6QhFVMab", "O6uvpzGd5pu", "202209", "92"));
    validateRow(response, 50, List.of("kEf6QhFVMab", "O6uvpzGd5pu", "202207", "87"));
    validateRow(response, 51, List.of("kEf6QhFVMab", "O6uvpzGd5pu", "202211", "83"));
    validateRow(response, 52, List.of("kEf6QhFVMab", "O6uvpzGd5pu", "202210", "78"));
    validateRow(response, 53, List.of("kEf6QhFVMab", "O6uvpzGd5pu", "202212", "78"));
    validateRow(response, 54, List.of("kEf6QhFVMab", "O6uvpzGd5pu", "202208", "76"));
    validateRow(response, 55, List.of("kEf6QhFVMab", "fdc6uOvgoji", "202212", "66"));
    validateRow(response, 56, List.of("kEf6QhFVMab", "fdc6uOvgoji", "202209", "61"));
    validateRow(response, 57, List.of("kEf6QhFVMab", "fdc6uOvgoji", "202207", "58"));
    validateRow(response, 58, List.of("kEf6QhFVMab", "fdc6uOvgoji", "202211", "57"));
    validateRow(response, 59, List.of("kEf6QhFVMab", "fdc6uOvgoji", "202208", "56"));
    validateRow(response, 60, List.of("kEf6QhFVMab", "jUb8gELQApl", "202212", "54"));
    validateRow(response, 61, List.of("kEf6QhFVMab", "jUb8gELQApl", "202211", "53"));
    validateRow(response, 62, List.of("kEf6QhFVMab", "jUb8gELQApl", "202210", "49"));
    validateRow(response, 63, List.of("kEf6QhFVMab", "jUb8gELQApl", "202208", "47"));
    validateRow(response, 64, List.of("kEf6QhFVMab", "lc3eMKXaEfw", "202207", "46"));
    validateRow(response, 65, List.of("kEf6QhFVMab", "jUb8gELQApl", "202207", "43"));
    validateRow(response, 66, List.of("kEf6QhFVMab", "lc3eMKXaEfw", "202209", "42"));
    validateRow(response, 67, List.of("kEf6QhFVMab", "jUb8gELQApl", "202209", "38"));
    validateRow(response, 68, List.of("kEf6QhFVMab", "lc3eMKXaEfw", "202210", "34"));
    validateRow(response, 69, List.of("kEf6QhFVMab", "lc3eMKXaEfw", "202212", "34"));
    validateRow(response, 70, List.of("kEf6QhFVMab", "lc3eMKXaEfw", "202211", "31"));
    validateRow(response, 71, List.of("kEf6QhFVMab", "lc3eMKXaEfw", "202208", "28"));
    validateRow(response, 72, List.of("FWciVWWrPMr", "O6uvpzGd5pu", "202208", "94"));
    validateRow(response, 73, List.of("FWciVWWrPMr", "O6uvpzGd5pu", "202209", "86"));
    validateRow(response, 74, List.of("FWciVWWrPMr", "O6uvpzGd5pu", "202212", "85"));
    validateRow(response, 75, List.of("FWciVWWrPMr", "O6uvpzGd5pu", "202207", "80"));
    validateRow(response, 76, List.of("FWciVWWrPMr", "O6uvpzGd5pu", "202210", "74"));
    validateRow(response, 77, List.of("FWciVWWrPMr", "fdc6uOvgoji", "202210", "69"));
    validateRow(response, 78, List.of("FWciVWWrPMr", "O6uvpzGd5pu", "202211", "69"));
    validateRow(response, 79, List.of("FWciVWWrPMr", "fdc6uOvgoji", "202207", "68"));
    validateRow(response, 80, List.of("FWciVWWrPMr", "fdc6uOvgoji", "202209", "64"));
    validateRow(response, 81, List.of("FWciVWWrPMr", "jUb8gELQApl", "202208", "59"));
    validateRow(response, 82, List.of("FWciVWWrPMr", "fdc6uOvgoji", "202208", "58"));
    validateRow(response, 83, List.of("FWciVWWrPMr", "fdc6uOvgoji", "202212", "57"));
    validateRow(response, 84, List.of("FWciVWWrPMr", "fdc6uOvgoji", "202211", "50"));
    validateRow(response, 85, List.of("FWciVWWrPMr", "jUb8gELQApl", "202211", "47"));
    validateRow(response, 86, List.of("FWciVWWrPMr", "jUb8gELQApl", "202207", "46"));
    validateRow(response, 87, List.of("FWciVWWrPMr", "lc3eMKXaEfw", "202210", "44"));
    validateRow(response, 88, List.of("FWciVWWrPMr", "lc3eMKXaEfw", "202208", "41"));
    validateRow(response, 89, List.of("FWciVWWrPMr", "jUb8gELQApl", "202209", "40"));
    validateRow(response, 90, List.of("FWciVWWrPMr", "jUb8gELQApl", "202212", "38"));
    validateRow(response, 91, List.of("FWciVWWrPMr", "jUb8gELQApl", "202210", "37"));
    validateRow(response, 92, List.of("FWciVWWrPMr", "lc3eMKXaEfw", "202209", "37"));
    validateRow(response, 93, List.of("FWciVWWrPMr", "lc3eMKXaEfw", "202212", "31"));
    validateRow(response, 94, List.of("FWciVWWrPMr", "lc3eMKXaEfw", "202211", "30"));
    validateRow(response, 95, List.of("FWciVWWrPMr", "lc3eMKXaEfw", "202207", "28"));
    validateRow(response, 96, List.of("ETdvuOmTpc6", "O6uvpzGd5pu", "202207", "97"));
    validateRow(response, 97, List.of("ETdvuOmTpc6", "O6uvpzGd5pu", "202210", "88"));
    validateRow(response, 98, List.of("ETdvuOmTpc6", "fdc6uOvgoji", "202210", "80"));
    validateRow(response, 99, List.of("ETdvuOmTpc6", "O6uvpzGd5pu", "202208", "75"));
    validateRow(response, 100, List.of("ETdvuOmTpc6", "fdc6uOvgoji", "202212", "73"));
    validateRow(response, 101, List.of("ETdvuOmTpc6", "O6uvpzGd5pu", "202209", "72"));
    validateRow(response, 102, List.of("ETdvuOmTpc6", "O6uvpzGd5pu", "202211", "71"));
    validateRow(response, 103, List.of("ETdvuOmTpc6", "fdc6uOvgoji", "202208", "69"));
    validateRow(response, 104, List.of("ETdvuOmTpc6", "fdc6uOvgoji", "202211", "68"));
    validateRow(response, 105, List.of("ETdvuOmTpc6", "O6uvpzGd5pu", "202212", "66"));
    validateRow(response, 106, List.of("ETdvuOmTpc6", "fdc6uOvgoji", "202207", "63"));
    validateRow(response, 107, List.of("ETdvuOmTpc6", "fdc6uOvgoji", "202209", "59"));
    validateRow(response, 108, List.of("ETdvuOmTpc6", "jUb8gELQApl", "202209", "50"));
    validateRow(response, 109, List.of("ETdvuOmTpc6", "jUb8gELQApl", "202207", "48"));
    validateRow(response, 110, List.of("ETdvuOmTpc6", "jUb8gELQApl", "202210", "47"));
    validateRow(response, 111, List.of("ETdvuOmTpc6", "jUb8gELQApl", "202212", "47"));
    validateRow(response, 112, List.of("ETdvuOmTpc6", "jUb8gELQApl", "202208", "44"));
    validateRow(response, 113, List.of("ETdvuOmTpc6", "jUb8gELQApl", "202211", "41"));
    validateRow(response, 114, List.of("ETdvuOmTpc6", "lc3eMKXaEfw", "202211", "38"));
    validateRow(response, 115, List.of("ETdvuOmTpc6", "lc3eMKXaEfw", "202209", "38"));
    validateRow(response, 116, List.of("ETdvuOmTpc6", "lc3eMKXaEfw", "202207", "35"));
    validateRow(response, 117, List.of("ETdvuOmTpc6", "lc3eMKXaEfw", "202208", "34"));
    validateRow(response, 118, List.of("ETdvuOmTpc6", "lc3eMKXaEfw", "202210", "31"));
    validateRow(response, 119, List.of("ETdvuOmTpc6", "lc3eMKXaEfw", "202212", "28"));
    validateRow(response, 120, List.of("BzQkRWHS7lu", "O6uvpzGd5pu", "202209", "100"));
    validateRow(response, 121, List.of("BzQkRWHS7lu", "O6uvpzGd5pu", "202210", "95"));
    validateRow(response, 122, List.of("BzQkRWHS7lu", "O6uvpzGd5pu", "202212", "92"));
    validateRow(response, 123, List.of("BzQkRWHS7lu", "O6uvpzGd5pu", "202208", "91"));
    validateRow(response, 124, List.of("BzQkRWHS7lu", "O6uvpzGd5pu", "202207", "90"));
    validateRow(response, 125, List.of("BzQkRWHS7lu", "O6uvpzGd5pu", "202211", "85"));
    validateRow(response, 126, List.of("BzQkRWHS7lu", "fdc6uOvgoji", "202208", "77"));
    validateRow(response, 127, List.of("BzQkRWHS7lu", "fdc6uOvgoji", "202207", "70"));
    validateRow(response, 128, List.of("BzQkRWHS7lu", "fdc6uOvgoji", "202210", "65"));
    validateRow(response, 129, List.of("BzQkRWHS7lu", "fdc6uOvgoji", "202211", "61"));
    validateRow(response, 130, List.of("BzQkRWHS7lu", "fdc6uOvgoji", "202212", "57"));
    validateRow(response, 131, List.of("BzQkRWHS7lu", "fdc6uOvgoji", "202209", "52"));
    validateRow(response, 132, List.of("BzQkRWHS7lu", "jUb8gELQApl", "202211", "50"));
    validateRow(response, 133, List.of("BzQkRWHS7lu", "jUb8gELQApl", "202207", "49"));
    validateRow(response, 134, List.of("BzQkRWHS7lu", "jUb8gELQApl", "202209", "48"));
    validateRow(response, 135, List.of("BzQkRWHS7lu", "jUb8gELQApl", "202212", "45"));
    validateRow(response, 136, List.of("BzQkRWHS7lu", "jUb8gELQApl", "202210", "44"));
    validateRow(response, 137, List.of("BzQkRWHS7lu", "jUb8gELQApl", "202208", "44"));
    validateRow(response, 138, List.of("BzQkRWHS7lu", "lc3eMKXaEfw", "202207", "42"));
    validateRow(response, 139, List.of("BzQkRWHS7lu", "lc3eMKXaEfw", "202210", "35"));
    validateRow(response, 140, List.of("BzQkRWHS7lu", "lc3eMKXaEfw", "202211", "33"));
    validateRow(response, 141, List.of("BzQkRWHS7lu", "lc3eMKXaEfw", "202208", "33"));
    validateRow(response, 142, List.of("BzQkRWHS7lu", "lc3eMKXaEfw", "202212", "28"));
    validateRow(response, 143, List.of("BzQkRWHS7lu", "lc3eMKXaEfw", "202209", "27"));
    validateRow(response, 144, List.of("", "O6uvpzGd5pu", "202208", "3"));
  }

  @Test
  public void queryCasesByAge5To25YearsLast12Monthsagg() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=Zj7UnCAulEk")
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add("dimension=ou:ImspTQPwCqd,pe:LAST_12_MONTHS,Zj7UnCAulEk.qrur9Dvnyt5")
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.aggregate().get("eBAyeGv0exc", JSON, JSON, params);
    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(4)))
        .body("rows", hasSize(equalTo(536)))
        .body("height", equalTo(536))
        .body("width", equalTo(4))
        .body("headerWidth", equalTo(4));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"ou\":{\"name\":\"Organisation unit\"},\"202208\":{\"name\":\"August 2022\"},\"202209\":{\"name\":\"September 2022\"},\"202305\":{\"name\":\"May 2023\"},\"qrur9Dvnyt5\":{\"name\":\"Age in years\"},\"202207\":{\"name\":\"July 2022\"},\"202306\":{\"name\":\"June 2023\"},\"202303\":{\"name\":\"March 2023\"},\"202304\":{\"name\":\"April 2023\"},\"202301\":{\"name\":\"January 2023\"},\"202302\":{\"name\":\"February 2023\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"},\"202211\":{\"name\":\"November 2022\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"202212\":{\"name\":\"December 2022\"},\"202210\":{\"name\":\"October 2022\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"pe\":{\"name\":\"Period\"}},\"dimensions\":{\"pe\":[\"202207\",\"202208\",\"202209\",\"202210\",\"202211\",\"202212\",\"202301\",\"202302\",\"202303\",\"202304\",\"202305\",\"202306\"],\"Zj7UnCAulEk.qrur9Dvnyt5\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "qrur9Dvnyt5", "Age in years", "INTEGER", "java.lang.Integer", false, true);
    validateHeader(response, 1, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, 0, List.of("9", "ImspTQPwCqd", "202212", "59"));
    validateRow(response, 1, List.of("9", "ImspTQPwCqd", "202207", "55"));
    validateRow(response, 2, List.of("9", "ImspTQPwCqd", "202208", "49"));
    validateRow(response, 3, List.of("9", "ImspTQPwCqd", "202210", "43"));
    validateRow(response, 4, List.of("9", "ImspTQPwCqd", "202211", "36"));
    validateRow(response, 5, List.of("9", "ImspTQPwCqd", "202209", "35"));
    validateRow(response, 6, List.of("88", "ImspTQPwCqd", "202211", "58"));
    validateRow(response, 7, List.of("88", "ImspTQPwCqd", "202208", "55"));
    validateRow(response, 8, List.of("88", "ImspTQPwCqd", "202212", "54"));
    validateRow(response, 9, List.of("88", "ImspTQPwCqd", "202210", "50"));
    validateRow(response, 10, List.of("88", "ImspTQPwCqd", "202207", "50"));
    validateRow(response, 11, List.of("88", "ImspTQPwCqd", "202209", "46"));
    validateRow(response, 12, List.of("87", "ImspTQPwCqd", "202212", "54"));
    validateRow(response, 13, List.of("87", "ImspTQPwCqd", "202209", "53"));
    validateRow(response, 14, List.of("87", "ImspTQPwCqd", "202210", "53"));
    validateRow(response, 15, List.of("87", "ImspTQPwCqd", "202207", "52"));
    validateRow(response, 16, List.of("87", "ImspTQPwCqd", "202211", "49"));
    validateRow(response, 17, List.of("87", "ImspTQPwCqd", "202208", "46"));
    validateRow(response, 18, List.of("86", "ImspTQPwCqd", "202207", "66"));
    validateRow(response, 19, List.of("86", "ImspTQPwCqd", "202208", "55"));
    validateRow(response, 20, List.of("86", "ImspTQPwCqd", "202209", "47"));
    validateRow(response, 21, List.of("86", "ImspTQPwCqd", "202210", "46"));
    validateRow(response, 22, List.of("86", "ImspTQPwCqd", "202211", "39"));
    validateRow(response, 23, List.of("86", "ImspTQPwCqd", "202212", "36"));
    validateRow(response, 24, List.of("85", "ImspTQPwCqd", "202210", "62"));
    validateRow(response, 25, List.of("85", "ImspTQPwCqd", "202211", "59"));
    validateRow(response, 26, List.of("85", "ImspTQPwCqd", "202208", "52"));
    validateRow(response, 27, List.of("85", "ImspTQPwCqd", "202209", "51"));
    validateRow(response, 28, List.of("85", "ImspTQPwCqd", "202212", "45"));
    validateRow(response, 29, List.of("85", "ImspTQPwCqd", "202207", "39"));
    validateRow(response, 30, List.of("84", "ImspTQPwCqd", "202207", "65"));
    validateRow(response, 31, List.of("84", "ImspTQPwCqd", "202211", "63"));
    validateRow(response, 32, List.of("84", "ImspTQPwCqd", "202209", "54"));
    validateRow(response, 33, List.of("84", "ImspTQPwCqd", "202210", "52"));
    validateRow(response, 34, List.of("84", "ImspTQPwCqd", "202212", "50"));
    validateRow(response, 35, List.of("84", "ImspTQPwCqd", "202208", "47"));
    validateRow(response, 36, List.of("83", "ImspTQPwCqd", "202209", "66"));
    validateRow(response, 37, List.of("83", "ImspTQPwCqd", "202210", "60"));
    validateRow(response, 38, List.of("83", "ImspTQPwCqd", "202207", "57"));
    validateRow(response, 39, List.of("83", "ImspTQPwCqd", "202211", "54"));
    validateRow(response, 40, List.of("83", "ImspTQPwCqd", "202208", "52"));
    validateRow(response, 41, List.of("83", "ImspTQPwCqd", "202212", "51"));
    validateRow(response, 42, List.of("82", "ImspTQPwCqd", "202207", "63"));
    validateRow(response, 43, List.of("82", "ImspTQPwCqd", "202210", "61"));
    validateRow(response, 44, List.of("82", "ImspTQPwCqd", "202208", "52"));
    validateRow(response, 45, List.of("82", "ImspTQPwCqd", "202212", "44"));
    validateRow(response, 46, List.of("82", "ImspTQPwCqd", "202211", "43"));
    validateRow(response, 47, List.of("82", "ImspTQPwCqd", "202209", "43"));
    validateRow(response, 48, List.of("81", "ImspTQPwCqd", "202207", "50"));
    validateRow(response, 49, List.of("81", "ImspTQPwCqd", "202208", "48"));
    validateRow(response, 50, List.of("81", "ImspTQPwCqd", "202211", "45"));
    validateRow(response, 51, List.of("81", "ImspTQPwCqd", "202209", "44"));
    validateRow(response, 52, List.of("81", "ImspTQPwCqd", "202210", "44"));
    validateRow(response, 53, List.of("81", "ImspTQPwCqd", "202212", "39"));
    validateRow(response, 54, List.of("80", "ImspTQPwCqd", "202208", "57"));
    validateRow(response, 55, List.of("80", "ImspTQPwCqd", "202209", "54"));
    validateRow(response, 56, List.of("80", "ImspTQPwCqd", "202211", "52"));
    validateRow(response, 57, List.of("80", "ImspTQPwCqd", "202210", "50"));
    validateRow(response, 58, List.of("80", "ImspTQPwCqd", "202207", "49"));
    validateRow(response, 59, List.of("80", "ImspTQPwCqd", "202212", "44"));
    validateRow(response, 60, List.of("8", "ImspTQPwCqd", "202210", "67"));
    validateRow(response, 61, List.of("8", "ImspTQPwCqd", "202211", "62"));
    validateRow(response, 62, List.of("8", "ImspTQPwCqd", "202207", "61"));
    validateRow(response, 63, List.of("8", "ImspTQPwCqd", "202209", "56"));
    validateRow(response, 64, List.of("8", "ImspTQPwCqd", "202208", "50"));
    validateRow(response, 65, List.of("8", "ImspTQPwCqd", "202212", "45"));
    validateRow(response, 66, List.of("79", "ImspTQPwCqd", "202208", "60"));
    validateRow(response, 67, List.of("79", "ImspTQPwCqd", "202210", "57"));
    validateRow(response, 68, List.of("79", "ImspTQPwCqd", "202212", "56"));
    validateRow(response, 69, List.of("79", "ImspTQPwCqd", "202207", "55"));
    validateRow(response, 70, List.of("79", "ImspTQPwCqd", "202209", "55"));
    validateRow(response, 71, List.of("79", "ImspTQPwCqd", "202211", "44"));
    validateRow(response, 72, List.of("78", "ImspTQPwCqd", "202207", "60"));
    validateRow(response, 73, List.of("78", "ImspTQPwCqd", "202209", "58"));
    validateRow(response, 74, List.of("78", "ImspTQPwCqd", "202208", "56"));
    validateRow(response, 75, List.of("78", "ImspTQPwCqd", "202212", "51"));
    validateRow(response, 76, List.of("78", "ImspTQPwCqd", "202211", "50"));
    validateRow(response, 77, List.of("78", "ImspTQPwCqd", "202210", "41"));
    validateRow(response, 78, List.of("77", "ImspTQPwCqd", "202210", "61"));
    validateRow(response, 79, List.of("77", "ImspTQPwCqd", "202211", "54"));
    validateRow(response, 80, List.of("77", "ImspTQPwCqd", "202209", "54"));
    validateRow(response, 81, List.of("77", "ImspTQPwCqd", "202207", "47"));
    validateRow(response, 82, List.of("77", "ImspTQPwCqd", "202212", "46"));
    validateRow(response, 83, List.of("77", "ImspTQPwCqd", "202208", "44"));
    validateRow(response, 84, List.of("76", "ImspTQPwCqd", "202207", "64"));
    validateRow(response, 85, List.of("76", "ImspTQPwCqd", "202211", "58"));
    validateRow(response, 86, List.of("76", "ImspTQPwCqd", "202209", "51"));
    validateRow(response, 87, List.of("76", "ImspTQPwCqd", "202210", "49"));
    validateRow(response, 88, List.of("76", "ImspTQPwCqd", "202208", "49"));
    validateRow(response, 89, List.of("76", "ImspTQPwCqd", "202212", "49"));
    validateRow(response, 90, List.of("75", "ImspTQPwCqd", "202208", "71"));
    validateRow(response, 91, List.of("75", "ImspTQPwCqd", "202210", "61"));
    validateRow(response, 92, List.of("75", "ImspTQPwCqd", "202212", "53"));
    validateRow(response, 93, List.of("75", "ImspTQPwCqd", "202211", "51"));
    validateRow(response, 94, List.of("75", "ImspTQPwCqd", "202207", "49"));
    validateRow(response, 95, List.of("75", "ImspTQPwCqd", "202209", "47"));
    validateRow(response, 96, List.of("74", "ImspTQPwCqd", "202208", "77"));
    validateRow(response, 97, List.of("74", "ImspTQPwCqd", "202207", "57"));
    validateRow(response, 98, List.of("74", "ImspTQPwCqd", "202210", "55"));
    validateRow(response, 99, List.of("74", "ImspTQPwCqd", "202211", "51"));
    validateRow(response, 100, List.of("74", "ImspTQPwCqd", "202209", "48"));
    validateRow(response, 101, List.of("74", "ImspTQPwCqd", "202212", "38"));
    validateRow(response, 102, List.of("73", "ImspTQPwCqd", "202208", "67"));
    validateRow(response, 103, List.of("73", "ImspTQPwCqd", "202207", "57"));
    validateRow(response, 104, List.of("73", "ImspTQPwCqd", "202211", "52"));
    validateRow(response, 105, List.of("73", "ImspTQPwCqd", "202209", "47"));
    validateRow(response, 106, List.of("73", "ImspTQPwCqd", "202212", "44"));
    validateRow(response, 107, List.of("73", "ImspTQPwCqd", "202210", "43"));
    validateRow(response, 108, List.of("72", "ImspTQPwCqd", "202212", "59"));
    validateRow(response, 109, List.of("72", "ImspTQPwCqd", "202207", "58"));
    validateRow(response, 110, List.of("72", "ImspTQPwCqd", "202208", "57"));
    validateRow(response, 111, List.of("72", "ImspTQPwCqd", "202211", "55"));
    validateRow(response, 112, List.of("72", "ImspTQPwCqd", "202209", "50"));
    validateRow(response, 113, List.of("72", "ImspTQPwCqd", "202210", "44"));
    validateRow(response, 114, List.of("71", "ImspTQPwCqd", "202208", "61"));
    validateRow(response, 115, List.of("71", "ImspTQPwCqd", "202210", "52"));
    validateRow(response, 116, List.of("71", "ImspTQPwCqd", "202211", "51"));
    validateRow(response, 117, List.of("71", "ImspTQPwCqd", "202207", "51"));
    validateRow(response, 118, List.of("71", "ImspTQPwCqd", "202212", "39"));
    validateRow(response, 119, List.of("71", "ImspTQPwCqd", "202209", "38"));
    validateRow(response, 120, List.of("70", "ImspTQPwCqd", "202211", "69"));
    validateRow(response, 121, List.of("70", "ImspTQPwCqd", "202207", "53"));
    validateRow(response, 122, List.of("70", "ImspTQPwCqd", "202212", "46"));
    validateRow(response, 123, List.of("70", "ImspTQPwCqd", "202208", "46"));
    validateRow(response, 124, List.of("70", "ImspTQPwCqd", "202210", "45"));
    validateRow(response, 125, List.of("70", "ImspTQPwCqd", "202209", "43"));
    validateRow(response, 126, List.of("7", "ImspTQPwCqd", "202209", "56"));
    validateRow(response, 127, List.of("7", "ImspTQPwCqd", "202208", "53"));
    validateRow(response, 128, List.of("7", "ImspTQPwCqd", "202211", "53"));
    validateRow(response, 129, List.of("7", "ImspTQPwCqd", "202207", "51"));
    validateRow(response, 130, List.of("7", "ImspTQPwCqd", "202212", "45"));
    validateRow(response, 131, List.of("7", "ImspTQPwCqd", "202210", "38"));
    validateRow(response, 132, List.of("69", "ImspTQPwCqd", "202210", "66"));
    validateRow(response, 133, List.of("69", "ImspTQPwCqd", "202207", "53"));
    validateRow(response, 134, List.of("69", "ImspTQPwCqd", "202209", "51"));
    validateRow(response, 135, List.of("69", "ImspTQPwCqd", "202212", "48"));
    validateRow(response, 136, List.of("69", "ImspTQPwCqd", "202211", "45"));
    validateRow(response, 137, List.of("69", "ImspTQPwCqd", "202208", "43"));
    validateRow(response, 138, List.of("68", "ImspTQPwCqd", "202211", "67"));
    validateRow(response, 139, List.of("68", "ImspTQPwCqd", "202210", "54"));
    validateRow(response, 140, List.of("68", "ImspTQPwCqd", "202212", "53"));
    validateRow(response, 141, List.of("68", "ImspTQPwCqd", "202207", "51"));
    validateRow(response, 142, List.of("68", "ImspTQPwCqd", "202209", "49"));
    validateRow(response, 143, List.of("68", "ImspTQPwCqd", "202208", "40"));
    validateRow(response, 144, List.of("67", "ImspTQPwCqd", "202208", "63"));
    validateRow(response, 145, List.of("67", "ImspTQPwCqd", "202211", "53"));
    validateRow(response, 146, List.of("67", "ImspTQPwCqd", "202212", "51"));
    validateRow(response, 147, List.of("67", "ImspTQPwCqd", "202209", "50"));
    validateRow(response, 148, List.of("67", "ImspTQPwCqd", "202207", "49"));
    validateRow(response, 149, List.of("67", "ImspTQPwCqd", "202210", "45"));
    validateRow(response, 150, List.of("66", "ImspTQPwCqd", "202212", "57"));
    validateRow(response, 151, List.of("66", "ImspTQPwCqd", "202207", "54"));
    validateRow(response, 152, List.of("66", "ImspTQPwCqd", "202208", "49"));
    validateRow(response, 153, List.of("66", "ImspTQPwCqd", "202209", "46"));
    validateRow(response, 154, List.of("66", "ImspTQPwCqd", "202210", "43"));
    validateRow(response, 155, List.of("66", "ImspTQPwCqd", "202211", "39"));
    validateRow(response, 156, List.of("65", "ImspTQPwCqd", "202208", "67"));
    validateRow(response, 157, List.of("65", "ImspTQPwCqd", "202212", "58"));
    validateRow(response, 158, List.of("65", "ImspTQPwCqd", "202210", "52"));
    validateRow(response, 159, List.of("65", "ImspTQPwCqd", "202211", "52"));
    validateRow(response, 160, List.of("65", "ImspTQPwCqd", "202209", "49"));
    validateRow(response, 161, List.of("65", "ImspTQPwCqd", "202207", "43"));
    validateRow(response, 162, List.of("64", "ImspTQPwCqd", "202208", "58"));
    validateRow(response, 163, List.of("64", "ImspTQPwCqd", "202211", "56"));
    validateRow(response, 164, List.of("64", "ImspTQPwCqd", "202210", "56"));
    validateRow(response, 165, List.of("64", "ImspTQPwCqd", "202207", "54"));
    validateRow(response, 166, List.of("64", "ImspTQPwCqd", "202209", "52"));
    validateRow(response, 167, List.of("64", "ImspTQPwCqd", "202212", "41"));
    validateRow(response, 168, List.of("63", "ImspTQPwCqd", "202210", "62"));
    validateRow(response, 169, List.of("63", "ImspTQPwCqd", "202209", "56"));
    validateRow(response, 170, List.of("63", "ImspTQPwCqd", "202207", "56"));
    validateRow(response, 171, List.of("63", "ImspTQPwCqd", "202211", "45"));
    validateRow(response, 172, List.of("63", "ImspTQPwCqd", "202212", "45"));
    validateRow(response, 173, List.of("63", "ImspTQPwCqd", "202208", "44"));
    validateRow(response, 174, List.of("62", "ImspTQPwCqd", "202209", "64"));
    validateRow(response, 175, List.of("62", "ImspTQPwCqd", "202211", "52"));
    validateRow(response, 176, List.of("62", "ImspTQPwCqd", "202210", "49"));
    validateRow(response, 177, List.of("62", "ImspTQPwCqd", "202207", "49"));
    validateRow(response, 178, List.of("62", "ImspTQPwCqd", "202212", "45"));
    validateRow(response, 179, List.of("62", "ImspTQPwCqd", "202208", "45"));
    validateRow(response, 180, List.of("61", "ImspTQPwCqd", "202210", "54"));
    validateRow(response, 181, List.of("61", "ImspTQPwCqd", "202207", "51"));
    validateRow(response, 182, List.of("61", "ImspTQPwCqd", "202212", "50"));
    validateRow(response, 183, List.of("61", "ImspTQPwCqd", "202211", "49"));
    validateRow(response, 184, List.of("61", "ImspTQPwCqd", "202209", "49"));
    validateRow(response, 185, List.of("61", "ImspTQPwCqd", "202208", "47"));
    validateRow(response, 186, List.of("60", "ImspTQPwCqd", "202207", "64"));
    validateRow(response, 187, List.of("60", "ImspTQPwCqd", "202208", "52"));
    validateRow(response, 188, List.of("60", "ImspTQPwCqd", "202209", "49"));
    validateRow(response, 189, List.of("60", "ImspTQPwCqd", "202210", "43"));
    validateRow(response, 190, List.of("60", "ImspTQPwCqd", "202212", "42"));
    validateRow(response, 191, List.of("60", "ImspTQPwCqd", "202211", "41"));
    validateRow(response, 192, List.of("6", "ImspTQPwCqd", "202210", "61"));
    validateRow(response, 193, List.of("6", "ImspTQPwCqd", "202209", "56"));
    validateRow(response, 194, List.of("6", "ImspTQPwCqd", "202211", "52"));
    validateRow(response, 195, List.of("6", "ImspTQPwCqd", "202208", "50"));
    validateRow(response, 196, List.of("6", "ImspTQPwCqd", "202212", "49"));
    validateRow(response, 197, List.of("6", "ImspTQPwCqd", "202207", "46"));
    validateRow(response, 198, List.of("59", "ImspTQPwCqd", "202208", "68"));
    validateRow(response, 199, List.of("59", "ImspTQPwCqd", "202209", "60"));
    validateRow(response, 200, List.of("59", "ImspTQPwCqd", "202207", "57"));
    validateRow(response, 201, List.of("59", "ImspTQPwCqd", "202211", "52"));
    validateRow(response, 202, List.of("59", "ImspTQPwCqd", "202210", "51"));
    validateRow(response, 203, List.of("59", "ImspTQPwCqd", "202212", "48"));
    validateRow(response, 204, List.of("58", "ImspTQPwCqd", "202207", "49"));
    validateRow(response, 205, List.of("58", "ImspTQPwCqd", "202208", "49"));
    validateRow(response, 206, List.of("58", "ImspTQPwCqd", "202210", "48"));
    validateRow(response, 207, List.of("58", "ImspTQPwCqd", "202212", "48"));
    validateRow(response, 208, List.of("58", "ImspTQPwCqd", "202209", "47"));
    validateRow(response, 209, List.of("58", "ImspTQPwCqd", "202211", "46"));
    validateRow(response, 210, List.of("57", "ImspTQPwCqd", "202211", "64"));
    validateRow(response, 211, List.of("57", "ImspTQPwCqd", "202209", "58"));
    validateRow(response, 212, List.of("57", "ImspTQPwCqd", "202208", "57"));
    validateRow(response, 213, List.of("57", "ImspTQPwCqd", "202207", "55"));
    validateRow(response, 214, List.of("57", "ImspTQPwCqd", "202212", "53"));
    validateRow(response, 215, List.of("57", "ImspTQPwCqd", "202210", "43"));
    validateRow(response, 216, List.of("56", "ImspTQPwCqd", "202209", "64"));
    validateRow(response, 217, List.of("56", "ImspTQPwCqd", "202208", "62"));
    validateRow(response, 218, List.of("56", "ImspTQPwCqd", "202207", "51"));
    validateRow(response, 219, List.of("56", "ImspTQPwCqd", "202211", "50"));
    validateRow(response, 220, List.of("56", "ImspTQPwCqd", "202212", "47"));
    validateRow(response, 221, List.of("56", "ImspTQPwCqd", "202210", "43"));
    validateRow(response, 222, List.of("55", "ImspTQPwCqd", "202209", "59"));
    validateRow(response, 223, List.of("55", "ImspTQPwCqd", "202211", "57"));
    validateRow(response, 224, List.of("55", "ImspTQPwCqd", "202210", "54"));
    validateRow(response, 225, List.of("55", "ImspTQPwCqd", "202207", "54"));
    validateRow(response, 226, List.of("55", "ImspTQPwCqd", "202212", "48"));
    validateRow(response, 227, List.of("55", "ImspTQPwCqd", "202208", "46"));
    validateRow(response, 228, List.of("54", "ImspTQPwCqd", "202211", "64"));
    validateRow(response, 229, List.of("54", "ImspTQPwCqd", "202207", "63"));
    validateRow(response, 230, List.of("54", "ImspTQPwCqd", "202212", "55"));
    validateRow(response, 231, List.of("54", "ImspTQPwCqd", "202209", "54"));
    validateRow(response, 232, List.of("54", "ImspTQPwCqd", "202208", "50"));
    validateRow(response, 233, List.of("54", "ImspTQPwCqd", "202210", "50"));
    validateRow(response, 234, List.of("53", "ImspTQPwCqd", "202209", "55"));
    validateRow(response, 235, List.of("53", "ImspTQPwCqd", "202211", "53"));
    validateRow(response, 236, List.of("53", "ImspTQPwCqd", "202207", "52"));
    validateRow(response, 237, List.of("53", "ImspTQPwCqd", "202212", "50"));
    validateRow(response, 238, List.of("53", "ImspTQPwCqd", "202210", "47"));
    validateRow(response, 239, List.of("53", "ImspTQPwCqd", "202208", "46"));
    validateRow(response, 240, List.of("52", "ImspTQPwCqd", "202207", "53"));
    validateRow(response, 241, List.of("52", "ImspTQPwCqd", "202212", "50"));
    validateRow(response, 242, List.of("52", "ImspTQPwCqd", "202208", "50"));
    validateRow(response, 243, List.of("52", "ImspTQPwCqd", "202209", "42"));
    validateRow(response, 244, List.of("52", "ImspTQPwCqd", "202210", "41"));
    validateRow(response, 245, List.of("52", "ImspTQPwCqd", "202211", "41"));
    validateRow(response, 246, List.of("51", "ImspTQPwCqd", "202208", "59"));
    validateRow(response, 247, List.of("51", "ImspTQPwCqd", "202209", "51"));
    validateRow(response, 248, List.of("51", "ImspTQPwCqd", "202207", "49"));
    validateRow(response, 249, List.of("51", "ImspTQPwCqd", "202212", "46"));
    validateRow(response, 250, List.of("51", "ImspTQPwCqd", "202211", "41"));
    validateRow(response, 251, List.of("51", "ImspTQPwCqd", "202210", "39"));
    validateRow(response, 252, List.of("50", "ImspTQPwCqd", "202208", "57"));
    validateRow(response, 253, List.of("50", "ImspTQPwCqd", "202211", "47"));
    validateRow(response, 254, List.of("50", "ImspTQPwCqd", "202212", "47"));
    validateRow(response, 255, List.of("50", "ImspTQPwCqd", "202209", "46"));
    validateRow(response, 256, List.of("50", "ImspTQPwCqd", "202210", "46"));
    validateRow(response, 257, List.of("50", "ImspTQPwCqd", "202207", "44"));
    validateRow(response, 258, List.of("5", "ImspTQPwCqd", "202207", "56"));
    validateRow(response, 259, List.of("5", "ImspTQPwCqd", "202209", "54"));
    validateRow(response, 260, List.of("5", "ImspTQPwCqd", "202211", "48"));
    validateRow(response, 261, List.of("5", "ImspTQPwCqd", "202208", "46"));
    validateRow(response, 262, List.of("5", "ImspTQPwCqd", "202210", "44"));
    validateRow(response, 263, List.of("5", "ImspTQPwCqd", "202212", "36"));
    validateRow(response, 264, List.of("49", "ImspTQPwCqd", "202212", "64"));
    validateRow(response, 265, List.of("49", "ImspTQPwCqd", "202211", "59"));
    validateRow(response, 266, List.of("49", "ImspTQPwCqd", "202209", "55"));
    validateRow(response, 267, List.of("49", "ImspTQPwCqd", "202207", "55"));
    validateRow(response, 268, List.of("49", "ImspTQPwCqd", "202210", "49"));
    validateRow(response, 269, List.of("49", "ImspTQPwCqd", "202208", "49"));
    validateRow(response, 270, List.of("48", "ImspTQPwCqd", "202212", "66"));
    validateRow(response, 271, List.of("48", "ImspTQPwCqd", "202211", "58"));
    validateRow(response, 272, List.of("48", "ImspTQPwCqd", "202210", "54"));
    validateRow(response, 273, List.of("48", "ImspTQPwCqd", "202207", "54"));
    validateRow(response, 274, List.of("48", "ImspTQPwCqd", "202209", "48"));
    validateRow(response, 275, List.of("48", "ImspTQPwCqd", "202208", "41"));
    validateRow(response, 276, List.of("47", "ImspTQPwCqd", "202208", "58"));
    validateRow(response, 277, List.of("47", "ImspTQPwCqd", "202207", "53"));
    validateRow(response, 278, List.of("47", "ImspTQPwCqd", "202212", "52"));
    validateRow(response, 279, List.of("47", "ImspTQPwCqd", "202209", "49"));
    validateRow(response, 280, List.of("47", "ImspTQPwCqd", "202210", "49"));
    validateRow(response, 281, List.of("47", "ImspTQPwCqd", "202211", "40"));
    validateRow(response, 282, List.of("46", "ImspTQPwCqd", "202208", "57"));
    validateRow(response, 283, List.of("46", "ImspTQPwCqd", "202211", "55"));
    validateRow(response, 284, List.of("46", "ImspTQPwCqd", "202210", "54"));
    validateRow(response, 285, List.of("46", "ImspTQPwCqd", "202212", "52"));
    validateRow(response, 286, List.of("46", "ImspTQPwCqd", "202209", "46"));
    validateRow(response, 287, List.of("46", "ImspTQPwCqd", "202207", "41"));
    validateRow(response, 288, List.of("45", "ImspTQPwCqd", "202207", "66"));
    validateRow(response, 289, List.of("45", "ImspTQPwCqd", "202210", "58"));
    validateRow(response, 290, List.of("45", "ImspTQPwCqd", "202209", "53"));
    validateRow(response, 291, List.of("45", "ImspTQPwCqd", "202208", "50"));
    validateRow(response, 292, List.of("45", "ImspTQPwCqd", "202211", "45"));
    validateRow(response, 293, List.of("45", "ImspTQPwCqd", "202212", "44"));
    validateRow(response, 294, List.of("44", "ImspTQPwCqd", "202207", "58"));
    validateRow(response, 295, List.of("44", "ImspTQPwCqd", "202210", "57"));
    validateRow(response, 296, List.of("44", "ImspTQPwCqd", "202208", "55"));
    validateRow(response, 297, List.of("44", "ImspTQPwCqd", "202212", "54"));
    validateRow(response, 298, List.of("44", "ImspTQPwCqd", "202211", "50"));
    validateRow(response, 299, List.of("44", "ImspTQPwCqd", "202209", "40"));
    validateRow(response, 300, List.of("43", "ImspTQPwCqd", "202210", "60"));
    validateRow(response, 301, List.of("43", "ImspTQPwCqd", "202207", "60"));
    validateRow(response, 302, List.of("43", "ImspTQPwCqd", "202212", "50"));
    validateRow(response, 303, List.of("43", "ImspTQPwCqd", "202209", "49"));
    validateRow(response, 304, List.of("43", "ImspTQPwCqd", "202211", "47"));
    validateRow(response, 305, List.of("43", "ImspTQPwCqd", "202208", "35"));
    validateRow(response, 306, List.of("42", "ImspTQPwCqd", "202209", "59"));
    validateRow(response, 307, List.of("42", "ImspTQPwCqd", "202210", "54"));
    validateRow(response, 308, List.of("42", "ImspTQPwCqd", "202208", "52"));
    validateRow(response, 309, List.of("42", "ImspTQPwCqd", "202212", "50"));
    validateRow(response, 310, List.of("42", "ImspTQPwCqd", "202211", "45"));
    validateRow(response, 311, List.of("42", "ImspTQPwCqd", "202207", "42"));
    validateRow(response, 312, List.of("41", "ImspTQPwCqd", "202212", "52"));
    validateRow(response, 313, List.of("41", "ImspTQPwCqd", "202210", "51"));
    validateRow(response, 314, List.of("41", "ImspTQPwCqd", "202209", "51"));
    validateRow(response, 315, List.of("41", "ImspTQPwCqd", "202211", "47"));
    validateRow(response, 316, List.of("41", "ImspTQPwCqd", "202208", "47"));
    validateRow(response, 317, List.of("41", "ImspTQPwCqd", "202207", "45"));
    validateRow(response, 318, List.of("40", "ImspTQPwCqd", "202207", "58"));
    validateRow(response, 319, List.of("40", "ImspTQPwCqd", "202212", "56"));
    validateRow(response, 320, List.of("40", "ImspTQPwCqd", "202208", "52"));
    validateRow(response, 321, List.of("40", "ImspTQPwCqd", "202209", "51"));
    validateRow(response, 322, List.of("40", "ImspTQPwCqd", "202210", "50"));
    validateRow(response, 323, List.of("40", "ImspTQPwCqd", "202211", "47"));
    validateRow(response, 324, List.of("4", "ImspTQPwCqd", "202210", "60"));
    validateRow(response, 325, List.of("4", "ImspTQPwCqd", "202212", "57"));
    validateRow(response, 326, List.of("4", "ImspTQPwCqd", "202208", "56"));
    validateRow(response, 327, List.of("4", "ImspTQPwCqd", "202211", "55"));
    validateRow(response, 328, List.of("4", "ImspTQPwCqd", "202209", "53"));
    validateRow(response, 329, List.of("4", "ImspTQPwCqd", "202207", "37"));
    validateRow(response, 330, List.of("39", "ImspTQPwCqd", "202207", "61"));
    validateRow(response, 331, List.of("39", "ImspTQPwCqd", "202211", "60"));
    validateRow(response, 332, List.of("39", "ImspTQPwCqd", "202208", "48"));
    validateRow(response, 333, List.of("39", "ImspTQPwCqd", "202209", "42"));
    validateRow(response, 334, List.of("39", "ImspTQPwCqd", "202210", "40"));
    validateRow(response, 335, List.of("39", "ImspTQPwCqd", "202212", "38"));
    validateRow(response, 336, List.of("38", "ImspTQPwCqd", "202207", "54"));
    validateRow(response, 337, List.of("38", "ImspTQPwCqd", "202212", "54"));
    validateRow(response, 338, List.of("38", "ImspTQPwCqd", "202209", "53"));
    validateRow(response, 339, List.of("38", "ImspTQPwCqd", "202210", "48"));
    validateRow(response, 340, List.of("38", "ImspTQPwCqd", "202211", "47"));
    validateRow(response, 341, List.of("38", "ImspTQPwCqd", "202208", "42"));
    validateRow(response, 342, List.of("37", "ImspTQPwCqd", "202211", "65"));
    validateRow(response, 343, List.of("37", "ImspTQPwCqd", "202207", "60"));
    validateRow(response, 344, List.of("37", "ImspTQPwCqd", "202212", "54"));
    validateRow(response, 345, List.of("37", "ImspTQPwCqd", "202210", "52"));
    validateRow(response, 346, List.of("37", "ImspTQPwCqd", "202208", "48"));
    validateRow(response, 347, List.of("37", "ImspTQPwCqd", "202209", "47"));
    validateRow(response, 348, List.of("36", "ImspTQPwCqd", "202210", "61"));
    validateRow(response, 349, List.of("36", "ImspTQPwCqd", "202207", "50"));
    validateRow(response, 350, List.of("36", "ImspTQPwCqd", "202208", "48"));
    validateRow(response, 351, List.of("36", "ImspTQPwCqd", "202209", "48"));
    validateRow(response, 352, List.of("36", "ImspTQPwCqd", "202212", "48"));
    validateRow(response, 353, List.of("36", "ImspTQPwCqd", "202211", "37"));
    validateRow(response, 354, List.of("35", "ImspTQPwCqd", "202209", "56"));
    validateRow(response, 355, List.of("35", "ImspTQPwCqd", "202208", "55"));
    validateRow(response, 356, List.of("35", "ImspTQPwCqd", "202210", "51"));
    validateRow(response, 357, List.of("35", "ImspTQPwCqd", "202212", "48"));
    validateRow(response, 358, List.of("35", "ImspTQPwCqd", "202207", "47"));
    validateRow(response, 359, List.of("35", "ImspTQPwCqd", "202211", "37"));
    validateRow(response, 360, List.of("34", "ImspTQPwCqd", "202210", "61"));
    validateRow(response, 361, List.of("34", "ImspTQPwCqd", "202207", "58"));
    validateRow(response, 362, List.of("34", "ImspTQPwCqd", "202208", "50"));
    validateRow(response, 363, List.of("34", "ImspTQPwCqd", "202212", "45"));
    validateRow(response, 364, List.of("34", "ImspTQPwCqd", "202211", "45"));
    validateRow(response, 365, List.of("34", "ImspTQPwCqd", "202209", "45"));
    validateRow(response, 366, List.of("33", "ImspTQPwCqd", "202210", "54"));
    validateRow(response, 367, List.of("33", "ImspTQPwCqd", "202207", "51"));
    validateRow(response, 368, List.of("33", "ImspTQPwCqd", "202212", "47"));
    validateRow(response, 369, List.of("33", "ImspTQPwCqd", "202211", "46"));
    validateRow(response, 370, List.of("33", "ImspTQPwCqd", "202208", "42"));
    validateRow(response, 371, List.of("33", "ImspTQPwCqd", "202209", "41"));
    validateRow(response, 372, List.of("32", "ImspTQPwCqd", "202207", "58"));
    validateRow(response, 373, List.of("32", "ImspTQPwCqd", "202210", "54"));
    validateRow(response, 374, List.of("32", "ImspTQPwCqd", "202208", "51"));
    validateRow(response, 375, List.of("32", "ImspTQPwCqd", "202211", "49"));
    validateRow(response, 376, List.of("32", "ImspTQPwCqd", "202209", "47"));
    validateRow(response, 377, List.of("32", "ImspTQPwCqd", "202212", "43"));
    validateRow(response, 378, List.of("31", "ImspTQPwCqd", "202208", "64"));
    validateRow(response, 379, List.of("31", "ImspTQPwCqd", "202207", "60"));
    validateRow(response, 380, List.of("31", "ImspTQPwCqd", "202210", "51"));
    validateRow(response, 381, List.of("31", "ImspTQPwCqd", "202211", "47"));
    validateRow(response, 382, List.of("31", "ImspTQPwCqd", "202209", "46"));
    validateRow(response, 383, List.of("31", "ImspTQPwCqd", "202212", "34"));
    validateRow(response, 384, List.of("30", "ImspTQPwCqd", "202209", "67"));
    validateRow(response, 385, List.of("30", "ImspTQPwCqd", "202207", "60"));
    validateRow(response, 386, List.of("30", "ImspTQPwCqd", "202208", "58"));
    validateRow(response, 387, List.of("30", "ImspTQPwCqd", "202210", "54"));
    validateRow(response, 388, List.of("30", "ImspTQPwCqd", "202212", "49"));
    validateRow(response, 389, List.of("30", "ImspTQPwCqd", "202211", "49"));
    validateRow(response, 390, List.of("3", "ImspTQPwCqd", "202211", "63"));
    validateRow(response, 391, List.of("3", "ImspTQPwCqd", "202208", "57"));
    validateRow(response, 392, List.of("3", "ImspTQPwCqd", "202210", "46"));
    validateRow(response, 393, List.of("3", "ImspTQPwCqd", "202212", "46"));
    validateRow(response, 394, List.of("3", "ImspTQPwCqd", "202209", "42"));
    validateRow(response, 395, List.of("3", "ImspTQPwCqd", "202207", "41"));
    validateRow(response, 396, List.of("29", "ImspTQPwCqd", "202210", "62"));
    validateRow(response, 397, List.of("29", "ImspTQPwCqd", "202209", "57"));
    validateRow(response, 398, List.of("29", "ImspTQPwCqd", "202211", "55"));
    validateRow(response, 399, List.of("29", "ImspTQPwCqd", "202207", "52"));
    validateRow(response, 400, List.of("29", "ImspTQPwCqd", "202212", "52"));
    validateRow(response, 401, List.of("29", "ImspTQPwCqd", "202208", "44"));
    validateRow(response, 402, List.of("28", "ImspTQPwCqd", "202211", "60"));
    validateRow(response, 403, List.of("28", "ImspTQPwCqd", "202210", "50"));
    validateRow(response, 404, List.of("28", "ImspTQPwCqd", "202209", "48"));
    validateRow(response, 405, List.of("28", "ImspTQPwCqd", "202208", "45"));
    validateRow(response, 406, List.of("28", "ImspTQPwCqd", "202207", "44"));
    validateRow(response, 407, List.of("28", "ImspTQPwCqd", "202212", "43"));
    validateRow(response, 408, List.of("27", "ImspTQPwCqd", "202207", "57"));
    validateRow(response, 409, List.of("27", "ImspTQPwCqd", "202210", "53"));
    validateRow(response, 410, List.of("27", "ImspTQPwCqd", "202211", "52"));
    validateRow(response, 411, List.of("27", "ImspTQPwCqd", "202209", "52"));
    validateRow(response, 412, List.of("27", "ImspTQPwCqd", "202212", "41"));
    validateRow(response, 413, List.of("27", "ImspTQPwCqd", "202208", "35"));
    validateRow(response, 414, List.of("26", "ImspTQPwCqd", "202210", "57"));
    validateRow(response, 415, List.of("26", "ImspTQPwCqd", "202212", "54"));
    validateRow(response, 416, List.of("26", "ImspTQPwCqd", "202207", "49"));
    validateRow(response, 417, List.of("26", "ImspTQPwCqd", "202208", "45"));
    validateRow(response, 418, List.of("26", "ImspTQPwCqd", "202211", "41"));
    validateRow(response, 419, List.of("26", "ImspTQPwCqd", "202209", "36"));
    validateRow(response, 420, List.of("25", "ImspTQPwCqd", "202210", "59"));
    validateRow(response, 421, List.of("25", "ImspTQPwCqd", "202209", "59"));
    validateRow(response, 422, List.of("25", "ImspTQPwCqd", "202207", "53"));
    validateRow(response, 423, List.of("25", "ImspTQPwCqd", "202211", "52"));
    validateRow(response, 424, List.of("25", "ImspTQPwCqd", "202212", "49"));
    validateRow(response, 425, List.of("25", "ImspTQPwCqd", "202208", "48"));
    validateRow(response, 426, List.of("24", "ImspTQPwCqd", "202208", "64"));
    validateRow(response, 427, List.of("24", "ImspTQPwCqd", "202209", "60"));
    validateRow(response, 428, List.of("24", "ImspTQPwCqd", "202210", "54"));
    validateRow(response, 429, List.of("24", "ImspTQPwCqd", "202211", "50"));
    validateRow(response, 430, List.of("24", "ImspTQPwCqd", "202207", "49"));
    validateRow(response, 431, List.of("24", "ImspTQPwCqd", "202212", "47"));
    validateRow(response, 432, List.of("234", "ImspTQPwCqd", "202208", "1"));
    validateRow(response, 433, List.of("23", "ImspTQPwCqd", "202209", "74"));
    validateRow(response, 434, List.of("23", "ImspTQPwCqd", "202210", "54"));
    validateRow(response, 435, List.of("23", "ImspTQPwCqd", "202211", "52"));
    validateRow(response, 436, List.of("23", "ImspTQPwCqd", "202207", "52"));
    validateRow(response, 437, List.of("23", "ImspTQPwCqd", "202212", "45"));
    validateRow(response, 438, List.of("23", "ImspTQPwCqd", "202208", "35"));
    validateRow(response, 439, List.of("22", "ImspTQPwCqd", "202211", "58"));
    validateRow(response, 440, List.of("22", "ImspTQPwCqd", "202210", "54"));
    validateRow(response, 441, List.of("22", "ImspTQPwCqd", "202208", "52"));
    validateRow(response, 442, List.of("22", "ImspTQPwCqd", "202207", "50"));
    validateRow(response, 443, List.of("22", "ImspTQPwCqd", "202212", "45"));
    validateRow(response, 444, List.of("22", "ImspTQPwCqd", "202209", "39"));
    validateRow(response, 445, List.of("21", "ImspTQPwCqd", "202209", "58"));
    validateRow(response, 446, List.of("21", "ImspTQPwCqd", "202210", "58"));
    validateRow(response, 447, List.of("21", "ImspTQPwCqd", "202207", "55"));
    validateRow(response, 448, List.of("21", "ImspTQPwCqd", "202211", "52"));
    validateRow(response, 449, List.of("21", "ImspTQPwCqd", "202208", "46"));
    validateRow(response, 450, List.of("21", "ImspTQPwCqd", "202212", "46"));
    validateRow(response, 451, List.of("20", "ImspTQPwCqd", "202210", "62"));
    validateRow(response, 452, List.of("20", "ImspTQPwCqd", "202207", "60"));
    validateRow(response, 453, List.of("20", "ImspTQPwCqd", "202211", "53"));
    validateRow(response, 454, List.of("20", "ImspTQPwCqd", "202212", "51"));
    validateRow(response, 455, List.of("20", "ImspTQPwCqd", "202208", "43"));
    validateRow(response, 456, List.of("20", "ImspTQPwCqd", "202209", "43"));
    validateRow(response, 457, List.of("2", "ImspTQPwCqd", "202210", "56"));
    validateRow(response, 458, List.of("2", "ImspTQPwCqd", "202208", "52"));
    validateRow(response, 459, List.of("2", "ImspTQPwCqd", "202209", "50"));
    validateRow(response, 460, List.of("2", "ImspTQPwCqd", "202212", "48"));
    validateRow(response, 461, List.of("2", "ImspTQPwCqd", "202207", "43"));
    validateRow(response, 462, List.of("2", "ImspTQPwCqd", "202211", "43"));
    validateRow(response, 463, List.of("19", "ImspTQPwCqd", "202208", "63"));
    validateRow(response, 464, List.of("19", "ImspTQPwCqd", "202207", "58"));
    validateRow(response, 465, List.of("19", "ImspTQPwCqd", "202210", "55"));
    validateRow(response, 466, List.of("19", "ImspTQPwCqd", "202211", "54"));
    validateRow(response, 467, List.of("19", "ImspTQPwCqd", "202212", "53"));
    validateRow(response, 468, List.of("19", "ImspTQPwCqd", "202209", "47"));
    validateRow(response, 469, List.of("18", "ImspTQPwCqd", "202210", "67"));
    validateRow(response, 470, List.of("18", "ImspTQPwCqd", "202208", "60"));
    validateRow(response, 471, List.of("18", "ImspTQPwCqd", "202209", "56"));
    validateRow(response, 472, List.of("18", "ImspTQPwCqd", "202211", "49"));
    validateRow(response, 473, List.of("18", "ImspTQPwCqd", "202207", "49"));
    validateRow(response, 474, List.of("18", "ImspTQPwCqd", "202212", "35"));
    validateRow(response, 475, List.of("17", "ImspTQPwCqd", "202210", "69"));
    validateRow(response, 476, List.of("17", "ImspTQPwCqd", "202211", "62"));
    validateRow(response, 477, List.of("17", "ImspTQPwCqd", "202212", "50"));
    validateRow(response, 478, List.of("17", "ImspTQPwCqd", "202208", "47"));
    validateRow(response, 479, List.of("17", "ImspTQPwCqd", "202207", "47"));
    validateRow(response, 480, List.of("17", "ImspTQPwCqd", "202209", "39"));
    validateRow(response, 481, List.of("16", "ImspTQPwCqd", "202212", "64"));
    validateRow(response, 482, List.of("16", "ImspTQPwCqd", "202211", "56"));
    validateRow(response, 483, List.of("16", "ImspTQPwCqd", "202209", "49"));
    validateRow(response, 484, List.of("16", "ImspTQPwCqd", "202208", "48"));
    validateRow(response, 485, List.of("16", "ImspTQPwCqd", "202207", "47"));
    validateRow(response, 486, List.of("16", "ImspTQPwCqd", "202210", "39"));
    validateRow(response, 487, List.of("15", "ImspTQPwCqd", "202211", "59"));
    validateRow(response, 488, List.of("15", "ImspTQPwCqd", "202209", "57"));
    validateRow(response, 489, List.of("15", "ImspTQPwCqd", "202212", "54"));
    validateRow(response, 490, List.of("15", "ImspTQPwCqd", "202207", "50"));
    validateRow(response, 491, List.of("15", "ImspTQPwCqd", "202208", "46"));
    validateRow(response, 492, List.of("15", "ImspTQPwCqd", "202210", "42"));
    validateRow(response, 493, List.of("14", "ImspTQPwCqd", "202211", "65"));
    validateRow(response, 494, List.of("14", "ImspTQPwCqd", "202209", "63"));
    validateRow(response, 495, List.of("14", "ImspTQPwCqd", "202208", "62"));
    validateRow(response, 496, List.of("14", "ImspTQPwCqd", "202210", "51"));
    validateRow(response, 497, List.of("14", "ImspTQPwCqd", "202212", "47"));
    validateRow(response, 498, List.of("14", "ImspTQPwCqd", "202207", "46"));
    validateRow(response, 499, List.of("13", "ImspTQPwCqd", "202210", "61"));
    validateRow(response, 500, List.of("13", "ImspTQPwCqd", "202212", "59"));
    validateRow(response, 501, List.of("13", "ImspTQPwCqd", "202207", "52"));
    validateRow(response, 502, List.of("13", "ImspTQPwCqd", "202208", "49"));
    validateRow(response, 503, List.of("13", "ImspTQPwCqd", "202209", "46"));
    validateRow(response, 504, List.of("13", "ImspTQPwCqd", "202211", "43"));
    validateRow(response, 505, List.of("12", "ImspTQPwCqd", "202210", "58"));
    validateRow(response, 506, List.of("12", "ImspTQPwCqd", "202208", "53"));
    validateRow(response, 507, List.of("12", "ImspTQPwCqd", "202212", "51"));
    validateRow(response, 508, List.of("12", "ImspTQPwCqd", "202209", "43"));
    validateRow(response, 509, List.of("12", "ImspTQPwCqd", "202211", "42"));
    validateRow(response, 510, List.of("12", "ImspTQPwCqd", "202207", "40"));
    validateRow(response, 511, List.of("11", "ImspTQPwCqd", "202211", "70"));
    validateRow(response, 512, List.of("11", "ImspTQPwCqd", "202209", "60"));
    validateRow(response, 513, List.of("11", "ImspTQPwCqd", "202212", "51"));
    validateRow(response, 514, List.of("11", "ImspTQPwCqd", "202208", "49"));
    validateRow(response, 515, List.of("11", "ImspTQPwCqd", "202207", "48"));
    validateRow(response, 516, List.of("11", "ImspTQPwCqd", "202210", "41"));
    validateRow(response, 517, List.of("10", "ImspTQPwCqd", "202207", "68"));
    validateRow(response, 518, List.of("10", "ImspTQPwCqd", "202208", "58"));
    validateRow(response, 519, List.of("10", "ImspTQPwCqd", "202209", "56"));
    validateRow(response, 520, List.of("10", "ImspTQPwCqd", "202210", "54"));
    validateRow(response, 521, List.of("10", "ImspTQPwCqd", "202211", "48"));
    validateRow(response, 522, List.of("10", "ImspTQPwCqd", "202212", "44"));
    validateRow(response, 523, List.of("1", "ImspTQPwCqd", "202207", "55"));
    validateRow(response, 524, List.of("1", "ImspTQPwCqd", "202209", "52"));
    validateRow(response, 525, List.of("1", "ImspTQPwCqd", "202210", "50"));
    validateRow(response, 526, List.of("1", "ImspTQPwCqd", "202212", "50"));
    validateRow(response, 527, List.of("1", "ImspTQPwCqd", "202211", "50"));
    validateRow(response, 528, List.of("1", "ImspTQPwCqd", "202208", "48"));
    validateRow(response, 529, List.of("0", "ImspTQPwCqd", "202208", "52"));
    validateRow(response, 530, List.of("0", "ImspTQPwCqd", "202210", "50"));
    validateRow(response, 531, List.of("0", "ImspTQPwCqd", "202209", "50"));
    validateRow(response, 532, List.of("0", "ImspTQPwCqd", "202207", "45"));
    validateRow(response, 533, List.of("0", "ImspTQPwCqd", "202211", "43"));
    validateRow(response, 534, List.of("0", "ImspTQPwCqd", "202212", "40"));
    validateRow(response, 535, List.of("", "ImspTQPwCqd", "202208", "2"));
  }

  @Test
  public void queryDischargedLast12MonthsByDistricts() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=Zj7UnCAulEk.fWIAEtYVEGk")
            .add("stage=Zj7UnCAulEk")
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add(
                "dimension=pe:LAST_12_MONTHS,ou:jUb8gELQApl;TEQlaapDQoK;eIQbndfxQMb;Vth0fbpFcsO;PMa2VCrupOd;O6uvpzGd5pu;bL4ooGhyHRQ;kJq2mPyFEHo;fdc6uOvgoji;at6UHUQatSo;lc3eMKXaEfw;qhqAxPSTUXp;jmIPBj66vD6")
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.aggregate().get("eBAyeGv0exc", JSON, JSON, params);
    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(78)))
        .body("height", equalTo(78))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"202208\":{\"name\":\"August 2022\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"202209\":{\"name\":\"September 2022\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"202305\":{\"name\":\"May 2023\"},\"202207\":{\"name\":\"July 2022\"},\"202306\":{\"name\":\"June 2023\"},\"202303\":{\"name\":\"March 2023\"},\"202304\":{\"name\":\"April 2023\"},\"gj2fKKyp8OH\":{\"code\":\"MODDIED\",\"name\":\"Died\"},\"202301\":{\"name\":\"January 2023\"},\"202302\":{\"name\":\"February 2023\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"},\"202211\":{\"name\":\"November 2022\"},\"202212\":{\"name\":\"December 2022\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"202210\":{\"name\":\"October 2022\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"ou\":{\"name\":\"Organisation unit\"},\"Fhbf4aKpZmZ\":{\"code\":\"MODABSC\",\"name\":\"Absconded\"},\"fWIAEtYVEGk\":{\"name\":\"Mode of Discharge\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"pe\":{\"name\":\"Period\"},\"fShHdgT7XGb\":{\"code\":\"MODTRANS\",\"name\":\"Transferred\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"},\"yeod5tOXpkP\":{\"code\":\"MODDISCH\",\"name\":\"Discharged\"}},\"dimensions\":{\"pe\":[\"202207\",\"202208\",\"202209\",\"202210\",\"202211\",\"202212\",\"202301\",\"202302\",\"202303\",\"202304\",\"202305\",\"202306\"],\"ou\":[\"jUb8gELQApl\",\"TEQlaapDQoK\",\"eIQbndfxQMb\",\"Vth0fbpFcsO\",\"PMa2VCrupOd\",\"O6uvpzGd5pu\",\"bL4ooGhyHRQ\",\"kJq2mPyFEHo\",\"fdc6uOvgoji\",\"at6UHUQatSo\",\"lc3eMKXaEfw\",\"qhqAxPSTUXp\",\"jmIPBj66vD6\"],\"fWIAEtYVEGk\":[\"yeod5tOXpkP\",\"gj2fKKyp8OH\",\"fShHdgT7XGb\",\"Fhbf4aKpZmZ\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, 0, List.of("qhqAxPSTUXp", "202209", "289"));
    validateRow(response, 1, List.of("qhqAxPSTUXp", "202207", "280"));
    validateRow(response, 2, List.of("qhqAxPSTUXp", "202212", "271"));
    validateRow(response, 3, List.of("qhqAxPSTUXp", "202208", "269"));
    validateRow(response, 4, List.of("qhqAxPSTUXp", "202211", "268"));
    validateRow(response, 5, List.of("qhqAxPSTUXp", "202210", "261"));
    validateRow(response, 6, List.of("lc3eMKXaEfw", "202207", "215"));
    validateRow(response, 7, List.of("lc3eMKXaEfw", "202211", "211"));
    validateRow(response, 8, List.of("lc3eMKXaEfw", "202209", "210"));
    validateRow(response, 9, List.of("lc3eMKXaEfw", "202208", "209"));
    validateRow(response, 10, List.of("lc3eMKXaEfw", "202210", "206"));
    validateRow(response, 11, List.of("lc3eMKXaEfw", "202212", "191"));
    validateRow(response, 12, List.of("kJq2mPyFEHo", "202210", "521"));
    validateRow(response, 13, List.of("kJq2mPyFEHo", "202208", "496"));
    validateRow(response, 14, List.of("kJq2mPyFEHo", "202212", "485"));
    validateRow(response, 15, List.of("kJq2mPyFEHo", "202211", "482"));
    validateRow(response, 16, List.of("kJq2mPyFEHo", "202207", "468"));
    validateRow(response, 17, List.of("kJq2mPyFEHo", "202209", "460"));
    validateRow(response, 18, List.of("jmIPBj66vD6", "202207", "405"));
    validateRow(response, 19, List.of("jmIPBj66vD6", "202211", "400"));
    validateRow(response, 20, List.of("jmIPBj66vD6", "202209", "374"));
    validateRow(response, 21, List.of("jmIPBj66vD6", "202210", "373"));
    validateRow(response, 22, List.of("jmIPBj66vD6", "202212", "357"));
    validateRow(response, 23, List.of("jmIPBj66vD6", "202208", "348"));
    validateRow(response, 24, List.of("jUb8gELQApl", "202208", "308"));
    validateRow(response, 25, List.of("jUb8gELQApl", "202210", "284"));
    validateRow(response, 26, List.of("jUb8gELQApl", "202212", "282"));
    validateRow(response, 27, List.of("jUb8gELQApl", "202207", "282"));
    validateRow(response, 28, List.of("jUb8gELQApl", "202211", "280"));
    validateRow(response, 29, List.of("jUb8gELQApl", "202209", "276"));
    validateRow(response, 30, List.of("fdc6uOvgoji", "202210", "454"));
    validateRow(response, 31, List.of("fdc6uOvgoji", "202207", "400"));
    validateRow(response, 32, List.of("fdc6uOvgoji", "202211", "382"));
    validateRow(response, 33, List.of("fdc6uOvgoji", "202208", "373"));
    validateRow(response, 34, List.of("fdc6uOvgoji", "202212", "367"));
    validateRow(response, 35, List.of("fdc6uOvgoji", "202209", "356"));
    validateRow(response, 36, List.of("eIQbndfxQMb", "202210", "397"));
    validateRow(response, 37, List.of("eIQbndfxQMb", "202207", "387"));
    validateRow(response, 38, List.of("eIQbndfxQMb", "202211", "379"));
    validateRow(response, 39, List.of("eIQbndfxQMb", "202209", "375"));
    validateRow(response, 40, List.of("eIQbndfxQMb", "202212", "334"));
    validateRow(response, 41, List.of("eIQbndfxQMb", "202208", "331"));
    validateRow(response, 42, List.of("bL4ooGhyHRQ", "202208", "287"));
    validateRow(response, 43, List.of("bL4ooGhyHRQ", "202210", "276"));
    validateRow(response, 44, List.of("bL4ooGhyHRQ", "202211", "272"));
    validateRow(response, 45, List.of("bL4ooGhyHRQ", "202207", "264"));
    validateRow(response, 46, List.of("bL4ooGhyHRQ", "202209", "249"));
    validateRow(response, 47, List.of("bL4ooGhyHRQ", "202212", "247"));
    validateRow(response, 48, List.of("at6UHUQatSo", "202211", "435"));
    validateRow(response, 49, List.of("at6UHUQatSo", "202209", "428"));
    validateRow(response, 50, List.of("at6UHUQatSo", "202208", "423"));
    validateRow(response, 51, List.of("at6UHUQatSo", "202207", "413"));
    validateRow(response, 52, List.of("at6UHUQatSo", "202210", "387"));
    validateRow(response, 53, List.of("at6UHUQatSo", "202212", "364"));
    validateRow(response, 54, List.of("Vth0fbpFcsO", "202208", "364"));
    validateRow(response, 55, List.of("Vth0fbpFcsO", "202210", "349"));
    validateRow(response, 56, List.of("Vth0fbpFcsO", "202207", "342"));
    validateRow(response, 57, List.of("Vth0fbpFcsO", "202212", "329"));
    validateRow(response, 58, List.of("Vth0fbpFcsO", "202211", "318"));
    validateRow(response, 59, List.of("Vth0fbpFcsO", "202209", "313"));
    validateRow(response, 60, List.of("TEQlaapDQoK", "202209", "458"));
    validateRow(response, 61, List.of("TEQlaapDQoK", "202207", "455"));
    validateRow(response, 62, List.of("TEQlaapDQoK", "202208", "442"));
    validateRow(response, 63, List.of("TEQlaapDQoK", "202211", "431"));
    validateRow(response, 64, List.of("TEQlaapDQoK", "202210", "413"));
    validateRow(response, 65, List.of("TEQlaapDQoK", "202212", "399"));
    validateRow(response, 66, List.of("PMa2VCrupOd", "202207", "274"));
    validateRow(response, 67, List.of("PMa2VCrupOd", "202208", "255"));
    validateRow(response, 68, List.of("PMa2VCrupOd", "202210", "248"));
    validateRow(response, 69, List.of("PMa2VCrupOd", "202209", "240"));
    validateRow(response, 70, List.of("PMa2VCrupOd", "202212", "235"));
    validateRow(response, 71, List.of("PMa2VCrupOd", "202211", "234"));
    validateRow(response, 72, List.of("O6uvpzGd5pu", "202207", "515"));
    validateRow(response, 73, List.of("O6uvpzGd5pu", "202209", "511"));
    validateRow(response, 74, List.of("O6uvpzGd5pu", "202208", "509"));
    validateRow(response, 75, List.of("O6uvpzGd5pu", "202210", "480"));
    validateRow(response, 76, List.of("O6uvpzGd5pu", "202212", "475"));
    validateRow(response, 77, List.of("O6uvpzGd5pu", "202211", "472"));
  }
}
