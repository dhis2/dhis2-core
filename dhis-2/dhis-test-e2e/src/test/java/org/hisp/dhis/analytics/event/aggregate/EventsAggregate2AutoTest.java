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
    validateRow(response, List.of("puI3YpLJ3fC", "O6uvpzGd5pu", "202208", "59"));
    validateRow(response, List.of("puI3YpLJ3fC", "O6uvpzGd5pu", "202207", "52"));
    validateRow(response, List.of("puI3YpLJ3fC", "O6uvpzGd5pu", "202209", "50"));
    validateRow(response, List.of("puI3YpLJ3fC", "fdc6uOvgoji", "202207", "45"));
    validateRow(response, List.of("puI3YpLJ3fC", "O6uvpzGd5pu", "202210", "45"));
    validateRow(response, List.of("puI3YpLJ3fC", "O6uvpzGd5pu", "202212", "44"));
    validateRow(response, List.of("puI3YpLJ3fC", "O6uvpzGd5pu", "202211", "44"));
    validateRow(response, List.of("puI3YpLJ3fC", "fdc6uOvgoji", "202210", "40"));
    validateRow(response, List.of("puI3YpLJ3fC", "fdc6uOvgoji", "202209", "39"));
    validateRow(response, List.of("puI3YpLJ3fC", "fdc6uOvgoji", "202212", "37"));
    validateRow(response, List.of("puI3YpLJ3fC", "jUb8gELQApl", "202208", "35"));
    validateRow(response, List.of("puI3YpLJ3fC", "fdc6uOvgoji", "202208", "34"));
    validateRow(response, List.of("puI3YpLJ3fC", "jUb8gELQApl", "202211", "32"));
    validateRow(response, List.of("puI3YpLJ3fC", "jUb8gELQApl", "202209", "30"));
    validateRow(response, List.of("puI3YpLJ3fC", "jUb8gELQApl", "202207", "30"));
    validateRow(response, List.of("puI3YpLJ3fC", "lc3eMKXaEfw", "202208", "28"));
    validateRow(response, List.of("puI3YpLJ3fC", "fdc6uOvgoji", "202211", "26"));
    validateRow(response, List.of("puI3YpLJ3fC", "lc3eMKXaEfw", "202210", "25"));
    validateRow(response, List.of("puI3YpLJ3fC", "jUb8gELQApl", "202212", "24"));
    validateRow(response, List.of("puI3YpLJ3fC", "lc3eMKXaEfw", "202209", "23"));
    validateRow(response, List.of("puI3YpLJ3fC", "lc3eMKXaEfw", "202207", "22"));
    validateRow(response, List.of("puI3YpLJ3fC", "lc3eMKXaEfw", "202212", "21"));
    validateRow(response, List.of("puI3YpLJ3fC", "jUb8gELQApl", "202210", "20"));
    validateRow(response, List.of("puI3YpLJ3fC", "lc3eMKXaEfw", "202211", "15"));
    validateRow(response, List.of("pZzk1L4Blf1", "O6uvpzGd5pu", "202207", "60"));
    validateRow(response, List.of("pZzk1L4Blf1", "O6uvpzGd5pu", "202211", "57"));
    validateRow(response, List.of("pZzk1L4Blf1", "O6uvpzGd5pu", "202209", "56"));
    validateRow(response, List.of("pZzk1L4Blf1", "O6uvpzGd5pu", "202210", "54"));
    validateRow(response, List.of("pZzk1L4Blf1", "fdc6uOvgoji", "202207", "51"));
    validateRow(response, List.of("pZzk1L4Blf1", "jUb8gELQApl", "202209", "46"));
    validateRow(response, List.of("pZzk1L4Blf1", "fdc6uOvgoji", "202209", "46"));
    validateRow(response, List.of("pZzk1L4Blf1", "O6uvpzGd5pu", "202208", "46"));
    validateRow(response, List.of("pZzk1L4Blf1", "fdc6uOvgoji", "202211", "45"));
    validateRow(response, List.of("pZzk1L4Blf1", "O6uvpzGd5pu", "202212", "43"));
    validateRow(response, List.of("pZzk1L4Blf1", "fdc6uOvgoji", "202210", "42"));
    validateRow(response, List.of("pZzk1L4Blf1", "fdc6uOvgoji", "202212", "41"));
    validateRow(response, List.of("pZzk1L4Blf1", "jUb8gELQApl", "202212", "34"));
    validateRow(response, List.of("pZzk1L4Blf1", "fdc6uOvgoji", "202208", "33"));
    validateRow(response, List.of("pZzk1L4Blf1", "jUb8gELQApl", "202208", "32"));
    validateRow(response, List.of("pZzk1L4Blf1", "jUb8gELQApl", "202207", "29"));
    validateRow(response, List.of("pZzk1L4Blf1", "jUb8gELQApl", "202210", "28"));
    validateRow(response, List.of("pZzk1L4Blf1", "jUb8gELQApl", "202211", "28"));
    validateRow(response, List.of("pZzk1L4Blf1", "lc3eMKXaEfw", "202209", "25"));
    validateRow(response, List.of("pZzk1L4Blf1", "lc3eMKXaEfw", "202208", "23"));
    validateRow(response, List.of("pZzk1L4Blf1", "lc3eMKXaEfw", "202212", "23"));
    validateRow(response, List.of("pZzk1L4Blf1", "lc3eMKXaEfw", "202211", "21"));
    validateRow(response, List.of("pZzk1L4Blf1", "lc3eMKXaEfw", "202207", "18"));
    validateRow(response, List.of("pZzk1L4Blf1", "lc3eMKXaEfw", "202210", "18"));
    validateRow(response, List.of("cbPqyIAFw9u", "O6uvpzGd5pu", "202208", "66"));
    validateRow(response, List.of("cbPqyIAFw9u", "O6uvpzGd5pu", "202209", "64"));
    validateRow(response, List.of("cbPqyIAFw9u", "O6uvpzGd5pu", "202210", "62"));
    validateRow(response, List.of("cbPqyIAFw9u", "O6uvpzGd5pu", "202207", "59"));
    validateRow(response, List.of("cbPqyIAFw9u", "O6uvpzGd5pu", "202212", "57"));
    validateRow(response, List.of("cbPqyIAFw9u", "O6uvpzGd5pu", "202211", "52"));
    validateRow(response, List.of("cbPqyIAFw9u", "fdc6uOvgoji", "202208", "51"));
    validateRow(response, List.of("cbPqyIAFw9u", "fdc6uOvgoji", "202210", "48"));
    validateRow(response, List.of("cbPqyIAFw9u", "fdc6uOvgoji", "202209", "44"));
    validateRow(response, List.of("cbPqyIAFw9u", "fdc6uOvgoji", "202211", "44"));
    validateRow(response, List.of("cbPqyIAFw9u", "fdc6uOvgoji", "202207", "43"));
    validateRow(response, List.of("cbPqyIAFw9u", "jUb8gELQApl", "202209", "37"));
    validateRow(response, List.of("cbPqyIAFw9u", "fdc6uOvgoji", "202212", "35"));
    validateRow(response, List.of("cbPqyIAFw9u", "jUb8gELQApl", "202211", "35"));
    validateRow(response, List.of("cbPqyIAFw9u", "jUb8gELQApl", "202207", "31"));
    validateRow(response, List.of("cbPqyIAFw9u", "lc3eMKXaEfw", "202207", "29"));
    validateRow(response, List.of("cbPqyIAFw9u", "jUb8gELQApl", "202208", "29"));
    validateRow(response, List.of("cbPqyIAFw9u", "jUb8gELQApl", "202212", "28"));
    validateRow(response, List.of("cbPqyIAFw9u", "jUb8gELQApl", "202210", "28"));
    validateRow(response, List.of("cbPqyIAFw9u", "lc3eMKXaEfw", "202211", "26"));
    validateRow(response, List.of("cbPqyIAFw9u", "lc3eMKXaEfw", "202208", "22"));
    validateRow(response, List.of("cbPqyIAFw9u", "lc3eMKXaEfw", "202210", "20"));
    validateRow(response, List.of("cbPqyIAFw9u", "lc3eMKXaEfw", "202209", "16"));
    validateRow(response, List.of("cbPqyIAFw9u", "lc3eMKXaEfw", "202212", "15"));
    validateRow(response, List.of("b7MCpzqJaR2", "O6uvpzGd5pu", "202208", "74"));
    validateRow(response, List.of("b7MCpzqJaR2", "O6uvpzGd5pu", "202212", "66"));
    validateRow(response, List.of("b7MCpzqJaR2", "O6uvpzGd5pu", "202209", "62"));
    validateRow(response, List.of("b7MCpzqJaR2", "O6uvpzGd5pu", "202210", "57"));
    validateRow(response, List.of("b7MCpzqJaR2", "O6uvpzGd5pu", "202207", "56"));
    validateRow(response, List.of("b7MCpzqJaR2", "O6uvpzGd5pu", "202211", "55"));
    validateRow(response, List.of("b7MCpzqJaR2", "fdc6uOvgoji", "202208", "52"));
    validateRow(response, List.of("b7MCpzqJaR2", "jUb8gELQApl", "202208", "49"));
    validateRow(response, List.of("b7MCpzqJaR2", "fdc6uOvgoji", "202210", "48"));
    validateRow(response, List.of("b7MCpzqJaR2", "fdc6uOvgoji", "202207", "47"));
    validateRow(response, List.of("b7MCpzqJaR2", "fdc6uOvgoji", "202209", "46"));
    validateRow(response, List.of("b7MCpzqJaR2", "fdc6uOvgoji", "202211", "45"));
    validateRow(response, List.of("b7MCpzqJaR2", "jUb8gELQApl", "202211", "36"));
    validateRow(response, List.of("b7MCpzqJaR2", "fdc6uOvgoji", "202212", "33"));
    validateRow(response, List.of("b7MCpzqJaR2", "jUb8gELQApl", "202207", "32"));
    validateRow(response, List.of("b7MCpzqJaR2", "lc3eMKXaEfw", "202211", "31"));
    validateRow(response, List.of("b7MCpzqJaR2", "jUb8gELQApl", "202210", "29"));
    validateRow(response, List.of("b7MCpzqJaR2", "lc3eMKXaEfw", "202209", "28"));
    validateRow(response, List.of("b7MCpzqJaR2", "lc3eMKXaEfw", "202210", "28"));
    validateRow(response, List.of("b7MCpzqJaR2", "jUb8gELQApl", "202212", "27"));
    validateRow(response, List.of("b7MCpzqJaR2", "lc3eMKXaEfw", "202208", "27"));
    validateRow(response, List.of("b7MCpzqJaR2", "jUb8gELQApl", "202209", "26"));
    validateRow(response, List.of("b7MCpzqJaR2", "lc3eMKXaEfw", "202207", "20"));
    validateRow(response, List.of("b7MCpzqJaR2", "lc3eMKXaEfw", "202212", "19"));
    validateRow(response, List.of("ZUUGJnvX40X", "O6uvpzGd5pu", "202210", "63"));
    validateRow(response, List.of("ZUUGJnvX40X", "O6uvpzGd5pu", "202207", "60"));
    validateRow(response, List.of("ZUUGJnvX40X", "O6uvpzGd5pu", "202209", "56"));
    validateRow(response, List.of("ZUUGJnvX40X", "fdc6uOvgoji", "202210", "56"));
    validateRow(response, List.of("ZUUGJnvX40X", "O6uvpzGd5pu", "202208", "55"));
    validateRow(response, List.of("ZUUGJnvX40X", "fdc6uOvgoji", "202208", "48"));
    validateRow(response, List.of("ZUUGJnvX40X", "fdc6uOvgoji", "202212", "47"));
    validateRow(response, List.of("ZUUGJnvX40X", "fdc6uOvgoji", "202211", "47"));
    validateRow(response, List.of("ZUUGJnvX40X", "O6uvpzGd5pu", "202212", "46"));
    validateRow(response, List.of("ZUUGJnvX40X", "O6uvpzGd5pu", "202211", "42"));
    validateRow(response, List.of("ZUUGJnvX40X", "fdc6uOvgoji", "202207", "40"));
    validateRow(response, List.of("ZUUGJnvX40X", "fdc6uOvgoji", "202209", "40"));
    validateRow(response, List.of("ZUUGJnvX40X", "jUb8gELQApl", "202207", "32"));
    validateRow(response, List.of("ZUUGJnvX40X", "jUb8gELQApl", "202212", "30"));
    validateRow(response, List.of("ZUUGJnvX40X", "jUb8gELQApl", "202210", "29"));
    validateRow(response, List.of("ZUUGJnvX40X", "jUb8gELQApl", "202208", "29"));
    validateRow(response, List.of("ZUUGJnvX40X", "lc3eMKXaEfw", "202211", "29"));
    validateRow(response, List.of("ZUUGJnvX40X", "jUb8gELQApl", "202211", "28"));
    validateRow(response, List.of("ZUUGJnvX40X", "jUb8gELQApl", "202209", "26"));
    validateRow(response, List.of("ZUUGJnvX40X", "lc3eMKXaEfw", "202208", "25"));
    validateRow(response, List.of("ZUUGJnvX40X", "lc3eMKXaEfw", "202209", "25"));
    validateRow(response, List.of("ZUUGJnvX40X", "lc3eMKXaEfw", "202210", "20"));
    validateRow(response, List.of("ZUUGJnvX40X", "lc3eMKXaEfw", "202207", "19"));
    validateRow(response, List.of("ZUUGJnvX40X", "lc3eMKXaEfw", "202212", "18"));
    validateRow(response, List.of("TvM2MQgD7Jd", "fdc6uOvgoji", "202210", "67"));
    validateRow(response, List.of("TvM2MQgD7Jd", "O6uvpzGd5pu", "202209", "65"));
    validateRow(response, List.of("TvM2MQgD7Jd", "O6uvpzGd5pu", "202211", "59"));
    validateRow(response, List.of("TvM2MQgD7Jd", "O6uvpzGd5pu", "202207", "58"));
    validateRow(response, List.of("TvM2MQgD7Jd", "O6uvpzGd5pu", "202212", "48"));
    validateRow(response, List.of("TvM2MQgD7Jd", "O6uvpzGd5pu", "202210", "48"));
    validateRow(response, List.of("TvM2MQgD7Jd", "O6uvpzGd5pu", "202208", "45"));
    validateRow(response, List.of("TvM2MQgD7Jd", "fdc6uOvgoji", "202209", "44"));
    validateRow(response, List.of("TvM2MQgD7Jd", "fdc6uOvgoji", "202212", "42"));
    validateRow(response, List.of("TvM2MQgD7Jd", "fdc6uOvgoji", "202211", "41"));
    validateRow(response, List.of("TvM2MQgD7Jd", "fdc6uOvgoji", "202207", "40"));
    validateRow(response, List.of("TvM2MQgD7Jd", "jUb8gELQApl", "202210", "36"));
    validateRow(response, List.of("TvM2MQgD7Jd", "lc3eMKXaEfw", "202207", "36"));
    validateRow(response, List.of("TvM2MQgD7Jd", "jUb8gELQApl", "202212", "35"));
    validateRow(response, List.of("TvM2MQgD7Jd", "jUb8gELQApl", "202208", "32"));
    validateRow(response, List.of("TvM2MQgD7Jd", "jUb8gELQApl", "202211", "32"));
    validateRow(response, List.of("TvM2MQgD7Jd", "jUb8gELQApl", "202209", "30"));
    validateRow(response, List.of("TvM2MQgD7Jd", "fdc6uOvgoji", "202208", "29"));
    validateRow(response, List.of("TvM2MQgD7Jd", "jUb8gELQApl", "202207", "27"));
    validateRow(response, List.of("TvM2MQgD7Jd", "lc3eMKXaEfw", "202209", "26"));
    validateRow(response, List.of("TvM2MQgD7Jd", "lc3eMKXaEfw", "202210", "25"));
    validateRow(response, List.of("TvM2MQgD7Jd", "lc3eMKXaEfw", "202212", "22"));
    validateRow(response, List.of("TvM2MQgD7Jd", "lc3eMKXaEfw", "202211", "18"));
    validateRow(response, List.of("TvM2MQgD7Jd", "lc3eMKXaEfw", "202208", "14"));
    validateRow(response, List.of("Tq4NYCn9eNH", "fdc6uOvgoji", "202210", "59"));
    validateRow(response, List.of("Tq4NYCn9eNH", "O6uvpzGd5pu", "202208", "57"));
    validateRow(response, List.of("Tq4NYCn9eNH", "fdc6uOvgoji", "202211", "57"));
    validateRow(response, List.of("Tq4NYCn9eNH", "O6uvpzGd5pu", "202212", "57"));
    validateRow(response, List.of("Tq4NYCn9eNH", "O6uvpzGd5pu", "202209", "48"));
    validateRow(response, List.of("Tq4NYCn9eNH", "O6uvpzGd5pu", "202207", "48"));
    validateRow(response, List.of("Tq4NYCn9eNH", "O6uvpzGd5pu", "202210", "47"));
    validateRow(response, List.of("Tq4NYCn9eNH", "fdc6uOvgoji", "202207", "47"));
    validateRow(response, List.of("Tq4NYCn9eNH", "O6uvpzGd5pu", "202211", "45"));
    validateRow(response, List.of("Tq4NYCn9eNH", "jUb8gELQApl", "202208", "40"));
    validateRow(response, List.of("Tq4NYCn9eNH", "jUb8gELQApl", "202210", "38"));
    validateRow(response, List.of("Tq4NYCn9eNH", "fdc6uOvgoji", "202212", "38"));
    validateRow(response, List.of("Tq4NYCn9eNH", "jUb8gELQApl", "202212", "36"));
    validateRow(response, List.of("Tq4NYCn9eNH", "fdc6uOvgoji", "202208", "35"));
    validateRow(response, List.of("Tq4NYCn9eNH", "jUb8gELQApl", "202207", "35"));
    validateRow(response, List.of("Tq4NYCn9eNH", "fdc6uOvgoji", "202209", "32"));
    validateRow(response, List.of("Tq4NYCn9eNH", "lc3eMKXaEfw", "202211", "31"));
    validateRow(response, List.of("Tq4NYCn9eNH", "lc3eMKXaEfw", "202212", "28"));
    validateRow(response, List.of("Tq4NYCn9eNH", "lc3eMKXaEfw", "202208", "26"));
    validateRow(response, List.of("Tq4NYCn9eNH", "jUb8gELQApl", "202211", "24"));
    validateRow(response, List.of("Tq4NYCn9eNH", "lc3eMKXaEfw", "202210", "23"));
    validateRow(response, List.of("Tq4NYCn9eNH", "lc3eMKXaEfw", "202207", "22"));
    validateRow(response, List.of("Tq4NYCn9eNH", "jUb8gELQApl", "202209", "22"));
    validateRow(response, List.of("Tq4NYCn9eNH", "lc3eMKXaEfw", "202209", "13"));
    validateRow(response, List.of("OyVUzWsX8UF", "O6uvpzGd5pu", "202208", "59"));
    validateRow(response, List.of("OyVUzWsX8UF", "O6uvpzGd5pu", "202212", "59"));
    validateRow(response, List.of("OyVUzWsX8UF", "O6uvpzGd5pu", "202209", "58"));
    validateRow(response, List.of("OyVUzWsX8UF", "O6uvpzGd5pu", "202211", "56"));
    validateRow(response, List.of("OyVUzWsX8UF", "O6uvpzGd5pu", "202207", "54"));
    validateRow(response, List.of("OyVUzWsX8UF", "fdc6uOvgoji", "202210", "53"));
    validateRow(response, List.of("OyVUzWsX8UF", "O6uvpzGd5pu", "202210", "46"));
    validateRow(response, List.of("OyVUzWsX8UF", "fdc6uOvgoji", "202212", "46"));
    validateRow(response, List.of("OyVUzWsX8UF", "fdc6uOvgoji", "202208", "44"));
    validateRow(response, List.of("OyVUzWsX8UF", "jUb8gELQApl", "202210", "42"));
    validateRow(response, List.of("OyVUzWsX8UF", "fdc6uOvgoji", "202211", "39"));
    validateRow(response, List.of("OyVUzWsX8UF", "fdc6uOvgoji", "202209", "38"));
    validateRow(response, List.of("OyVUzWsX8UF", "fdc6uOvgoji", "202207", "37"));
    validateRow(response, List.of("OyVUzWsX8UF", "jUb8gELQApl", "202211", "37"));
    validateRow(response, List.of("OyVUzWsX8UF", "jUb8gELQApl", "202212", "34"));
    validateRow(response, List.of("OyVUzWsX8UF", "jUb8gELQApl", "202208", "32"));
    validateRow(response, List.of("OyVUzWsX8UF", "jUb8gELQApl", "202207", "32"));
    validateRow(response, List.of("OyVUzWsX8UF", "lc3eMKXaEfw", "202209", "30"));
    validateRow(response, List.of("OyVUzWsX8UF", "jUb8gELQApl", "202209", "24"));
    validateRow(response, List.of("OyVUzWsX8UF", "lc3eMKXaEfw", "202208", "24"));
    validateRow(response, List.of("OyVUzWsX8UF", "lc3eMKXaEfw", "202211", "24"));
    validateRow(response, List.of("OyVUzWsX8UF", "lc3eMKXaEfw", "202212", "22"));
    validateRow(response, List.of("OyVUzWsX8UF", "lc3eMKXaEfw", "202210", "21"));
    validateRow(response, List.of("OyVUzWsX8UF", "lc3eMKXaEfw", "202207", "20"));
    validateRow(response, List.of("CpP5yzbgfHo", "O6uvpzGd5pu", "202207", "68"));
    validateRow(response, List.of("CpP5yzbgfHo", "O6uvpzGd5pu", "202211", "62"));
    validateRow(response, List.of("CpP5yzbgfHo", "O6uvpzGd5pu", "202210", "58"));
    validateRow(response, List.of("CpP5yzbgfHo", "O6uvpzGd5pu", "202212", "55"));
    validateRow(response, List.of("CpP5yzbgfHo", "O6uvpzGd5pu", "202209", "52"));
    validateRow(response, List.of("CpP5yzbgfHo", "fdc6uOvgoji", "202207", "50"));
    validateRow(response, List.of("CpP5yzbgfHo", "fdc6uOvgoji", "202212", "48"));
    validateRow(response, List.of("CpP5yzbgfHo", "fdc6uOvgoji", "202208", "47"));
    validateRow(response, List.of("CpP5yzbgfHo", "O6uvpzGd5pu", "202208", "45"));
    validateRow(response, List.of("CpP5yzbgfHo", "fdc6uOvgoji", "202210", "41"));
    validateRow(response, List.of("CpP5yzbgfHo", "fdc6uOvgoji", "202211", "38"));
    validateRow(response, List.of("CpP5yzbgfHo", "jUb8gELQApl", "202209", "35"));
    validateRow(response, List.of("CpP5yzbgfHo", "jUb8gELQApl", "202210", "34"));
    validateRow(response, List.of("CpP5yzbgfHo", "jUb8gELQApl", "202212", "34"));
    validateRow(response, List.of("CpP5yzbgfHo", "jUb8gELQApl", "202207", "34"));
    validateRow(response, List.of("CpP5yzbgfHo", "jUb8gELQApl", "202208", "30"));
    validateRow(response, List.of("CpP5yzbgfHo", "lc3eMKXaEfw", "202207", "29"));
    validateRow(response, List.of("CpP5yzbgfHo", "jUb8gELQApl", "202211", "28"));
    validateRow(response, List.of("CpP5yzbgfHo", "fdc6uOvgoji", "202209", "27"));
    validateRow(response, List.of("CpP5yzbgfHo", "lc3eMKXaEfw", "202210", "26"));
    validateRow(response, List.of("CpP5yzbgfHo", "lc3eMKXaEfw", "202209", "24"));
    validateRow(response, List.of("CpP5yzbgfHo", "lc3eMKXaEfw", "202212", "23"));
    validateRow(response, List.of("CpP5yzbgfHo", "lc3eMKXaEfw", "202208", "20"));
    validateRow(response, List.of("CpP5yzbgfHo", "lc3eMKXaEfw", "202211", "16"));
    validateRow(response, List.of("", "O6uvpzGd5pu", "202208", "3"));
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
    validateRow(response, List.of("xpC4lomA8aD", "O6uvpzGd5pu", "202208", "96"));
    validateRow(response, List.of("xpC4lomA8aD", "O6uvpzGd5pu", "202212", "82"));
    validateRow(response, List.of("xpC4lomA8aD", "fdc6uOvgoji", "202211", "78"));
    validateRow(response, List.of("xpC4lomA8aD", "fdc6uOvgoji", "202210", "78"));
    validateRow(response, List.of("xpC4lomA8aD", "O6uvpzGd5pu", "202207", "76"));
    validateRow(response, List.of("xpC4lomA8aD", "O6uvpzGd5pu", "202210", "75"));
    validateRow(response, List.of("xpC4lomA8aD", "O6uvpzGd5pu", "202211", "75"));
    validateRow(response, List.of("xpC4lomA8aD", "O6uvpzGd5pu", "202209", "74"));
    validateRow(response, List.of("xpC4lomA8aD", "fdc6uOvgoji", "202207", "71"));
    validateRow(response, List.of("xpC4lomA8aD", "jUb8gELQApl", "202208", "65"));
    validateRow(response, List.of("xpC4lomA8aD", "fdc6uOvgoji", "202208", "63"));
    validateRow(response, List.of("xpC4lomA8aD", "fdc6uOvgoji", "202209", "53"));
    validateRow(response, List.of("xpC4lomA8aD", "jUb8gELQApl", "202207", "51"));
    validateRow(response, List.of("xpC4lomA8aD", "fdc6uOvgoji", "202212", "51"));
    validateRow(response, List.of("xpC4lomA8aD", "jUb8gELQApl", "202210", "50"));
    validateRow(response, List.of("xpC4lomA8aD", "jUb8gELQApl", "202212", "49"));
    validateRow(response, List.of("xpC4lomA8aD", "lc3eMKXaEfw", "202211", "47"));
    validateRow(response, List.of("xpC4lomA8aD", "jUb8gELQApl", "202211", "45"));
    validateRow(response, List.of("xpC4lomA8aD", "lc3eMKXaEfw", "202208", "40"));
    validateRow(response, List.of("xpC4lomA8aD", "jUb8gELQApl", "202209", "38"));
    validateRow(response, List.of("xpC4lomA8aD", "lc3eMKXaEfw", "202212", "37"));
    validateRow(response, List.of("xpC4lomA8aD", "lc3eMKXaEfw", "202207", "36"));
    validateRow(response, List.of("xpC4lomA8aD", "lc3eMKXaEfw", "202210", "32"));
    validateRow(response, List.of("xpC4lomA8aD", "lc3eMKXaEfw", "202209", "27"));
    validateRow(response, List.of("rlXteEDaTpt", "O6uvpzGd5pu", "202211", "89"));
    validateRow(response, List.of("rlXteEDaTpt", "O6uvpzGd5pu", "202209", "87"));
    validateRow(response, List.of("rlXteEDaTpt", "O6uvpzGd5pu", "202207", "85"));
    validateRow(response, List.of("rlXteEDaTpt", "O6uvpzGd5pu", "202208", "74"));
    validateRow(response, List.of("rlXteEDaTpt", "O6uvpzGd5pu", "202212", "72"));
    validateRow(response, List.of("rlXteEDaTpt", "O6uvpzGd5pu", "202210", "70"));
    validateRow(response, List.of("rlXteEDaTpt", "fdc6uOvgoji", "202207", "70"));
    validateRow(response, List.of("rlXteEDaTpt", "fdc6uOvgoji", "202210", "69"));
    validateRow(response, List.of("rlXteEDaTpt", "fdc6uOvgoji", "202211", "68"));
    validateRow(response, List.of("rlXteEDaTpt", "fdc6uOvgoji", "202209", "67"));
    validateRow(response, List.of("rlXteEDaTpt", "fdc6uOvgoji", "202212", "63"));
    validateRow(response, List.of("rlXteEDaTpt", "jUb8gELQApl", "202209", "62"));
    validateRow(response, List.of("rlXteEDaTpt", "jUb8gELQApl", "202210", "57"));
    validateRow(response, List.of("rlXteEDaTpt", "fdc6uOvgoji", "202208", "50"));
    validateRow(response, List.of("rlXteEDaTpt", "jUb8gELQApl", "202212", "49"));
    validateRow(response, List.of("rlXteEDaTpt", "jUb8gELQApl", "202208", "49"));
    validateRow(response, List.of("rlXteEDaTpt", "jUb8gELQApl", "202207", "45"));
    validateRow(response, List.of("rlXteEDaTpt", "jUb8gELQApl", "202211", "44"));
    validateRow(response, List.of("rlXteEDaTpt", "lc3eMKXaEfw", "202209", "39"));
    validateRow(response, List.of("rlXteEDaTpt", "lc3eMKXaEfw", "202212", "33"));
    validateRow(response, List.of("rlXteEDaTpt", "lc3eMKXaEfw", "202208", "33"));
    validateRow(response, List.of("rlXteEDaTpt", "lc3eMKXaEfw", "202211", "32"));
    validateRow(response, List.of("rlXteEDaTpt", "lc3eMKXaEfw", "202210", "30"));
    validateRow(response, List.of("rlXteEDaTpt", "lc3eMKXaEfw", "202207", "28"));
    validateRow(response, List.of("kEf6QhFVMab", "fdc6uOvgoji", "202210", "93"));
    validateRow(response, List.of("kEf6QhFVMab", "O6uvpzGd5pu", "202209", "92"));
    validateRow(response, List.of("kEf6QhFVMab", "O6uvpzGd5pu", "202207", "87"));
    validateRow(response, List.of("kEf6QhFVMab", "O6uvpzGd5pu", "202211", "83"));
    validateRow(response, List.of("kEf6QhFVMab", "O6uvpzGd5pu", "202210", "78"));
    validateRow(response, List.of("kEf6QhFVMab", "O6uvpzGd5pu", "202212", "78"));
    validateRow(response, List.of("kEf6QhFVMab", "O6uvpzGd5pu", "202208", "76"));
    validateRow(response, List.of("kEf6QhFVMab", "fdc6uOvgoji", "202212", "66"));
    validateRow(response, List.of("kEf6QhFVMab", "fdc6uOvgoji", "202209", "61"));
    validateRow(response, List.of("kEf6QhFVMab", "fdc6uOvgoji", "202207", "58"));
    validateRow(response, List.of("kEf6QhFVMab", "fdc6uOvgoji", "202211", "57"));
    validateRow(response, List.of("kEf6QhFVMab", "fdc6uOvgoji", "202208", "56"));
    validateRow(response, List.of("kEf6QhFVMab", "jUb8gELQApl", "202212", "54"));
    validateRow(response, List.of("kEf6QhFVMab", "jUb8gELQApl", "202211", "53"));
    validateRow(response, List.of("kEf6QhFVMab", "jUb8gELQApl", "202210", "49"));
    validateRow(response, List.of("kEf6QhFVMab", "jUb8gELQApl", "202208", "47"));
    validateRow(response, List.of("kEf6QhFVMab", "lc3eMKXaEfw", "202207", "46"));
    validateRow(response, List.of("kEf6QhFVMab", "jUb8gELQApl", "202207", "43"));
    validateRow(response, List.of("kEf6QhFVMab", "lc3eMKXaEfw", "202209", "42"));
    validateRow(response, List.of("kEf6QhFVMab", "jUb8gELQApl", "202209", "38"));
    validateRow(response, List.of("kEf6QhFVMab", "lc3eMKXaEfw", "202210", "34"));
    validateRow(response, List.of("kEf6QhFVMab", "lc3eMKXaEfw", "202212", "34"));
    validateRow(response, List.of("kEf6QhFVMab", "lc3eMKXaEfw", "202211", "31"));
    validateRow(response, List.of("kEf6QhFVMab", "lc3eMKXaEfw", "202208", "28"));
    validateRow(response, List.of("FWciVWWrPMr", "O6uvpzGd5pu", "202208", "94"));
    validateRow(response, List.of("FWciVWWrPMr", "O6uvpzGd5pu", "202209", "86"));
    validateRow(response, List.of("FWciVWWrPMr", "O6uvpzGd5pu", "202212", "85"));
    validateRow(response, List.of("FWciVWWrPMr", "O6uvpzGd5pu", "202207", "80"));
    validateRow(response, List.of("FWciVWWrPMr", "O6uvpzGd5pu", "202210", "74"));
    validateRow(response, List.of("FWciVWWrPMr", "fdc6uOvgoji", "202210", "69"));
    validateRow(response, List.of("FWciVWWrPMr", "O6uvpzGd5pu", "202211", "69"));
    validateRow(response, List.of("FWciVWWrPMr", "fdc6uOvgoji", "202207", "68"));
    validateRow(response, List.of("FWciVWWrPMr", "fdc6uOvgoji", "202209", "64"));
    validateRow(response, List.of("FWciVWWrPMr", "jUb8gELQApl", "202208", "59"));
    validateRow(response, List.of("FWciVWWrPMr", "fdc6uOvgoji", "202208", "58"));
    validateRow(response, List.of("FWciVWWrPMr", "fdc6uOvgoji", "202212", "57"));
    validateRow(response, List.of("FWciVWWrPMr", "fdc6uOvgoji", "202211", "50"));
    validateRow(response, List.of("FWciVWWrPMr", "jUb8gELQApl", "202211", "47"));
    validateRow(response, List.of("FWciVWWrPMr", "jUb8gELQApl", "202207", "46"));
    validateRow(response, List.of("FWciVWWrPMr", "lc3eMKXaEfw", "202210", "44"));
    validateRow(response, List.of("FWciVWWrPMr", "lc3eMKXaEfw", "202208", "41"));
    validateRow(response, List.of("FWciVWWrPMr", "jUb8gELQApl", "202209", "40"));
    validateRow(response, List.of("FWciVWWrPMr", "jUb8gELQApl", "202212", "38"));
    validateRow(response, List.of("FWciVWWrPMr", "jUb8gELQApl", "202210", "37"));
    validateRow(response, List.of("FWciVWWrPMr", "lc3eMKXaEfw", "202209", "37"));
    validateRow(response, List.of("FWciVWWrPMr", "lc3eMKXaEfw", "202212", "31"));
    validateRow(response, List.of("FWciVWWrPMr", "lc3eMKXaEfw", "202211", "30"));
    validateRow(response, List.of("FWciVWWrPMr", "lc3eMKXaEfw", "202207", "28"));
    validateRow(response, List.of("ETdvuOmTpc6", "O6uvpzGd5pu", "202207", "97"));
    validateRow(response, List.of("ETdvuOmTpc6", "O6uvpzGd5pu", "202210", "88"));
    validateRow(response, List.of("ETdvuOmTpc6", "fdc6uOvgoji", "202210", "80"));
    validateRow(response, List.of("ETdvuOmTpc6", "O6uvpzGd5pu", "202208", "75"));
    validateRow(response, List.of("ETdvuOmTpc6", "fdc6uOvgoji", "202212", "73"));
    validateRow(response, List.of("ETdvuOmTpc6", "O6uvpzGd5pu", "202209", "72"));
    validateRow(response, List.of("ETdvuOmTpc6", "O6uvpzGd5pu", "202211", "71"));
    validateRow(response, List.of("ETdvuOmTpc6", "fdc6uOvgoji", "202208", "69"));
    validateRow(response, List.of("ETdvuOmTpc6", "fdc6uOvgoji", "202211", "68"));
    validateRow(response, List.of("ETdvuOmTpc6", "O6uvpzGd5pu", "202212", "66"));
    validateRow(response, List.of("ETdvuOmTpc6", "fdc6uOvgoji", "202207", "63"));
    validateRow(response, List.of("ETdvuOmTpc6", "fdc6uOvgoji", "202209", "59"));
    validateRow(response, List.of("ETdvuOmTpc6", "jUb8gELQApl", "202209", "50"));
    validateRow(response, List.of("ETdvuOmTpc6", "jUb8gELQApl", "202207", "48"));
    validateRow(response, List.of("ETdvuOmTpc6", "jUb8gELQApl", "202210", "47"));
    validateRow(response, List.of("ETdvuOmTpc6", "jUb8gELQApl", "202212", "47"));
    validateRow(response, List.of("ETdvuOmTpc6", "jUb8gELQApl", "202208", "44"));
    validateRow(response, List.of("ETdvuOmTpc6", "jUb8gELQApl", "202211", "41"));
    validateRow(response, List.of("ETdvuOmTpc6", "lc3eMKXaEfw", "202211", "38"));
    validateRow(response, List.of("ETdvuOmTpc6", "lc3eMKXaEfw", "202209", "38"));
    validateRow(response, List.of("ETdvuOmTpc6", "lc3eMKXaEfw", "202207", "35"));
    validateRow(response, List.of("ETdvuOmTpc6", "lc3eMKXaEfw", "202208", "34"));
    validateRow(response, List.of("ETdvuOmTpc6", "lc3eMKXaEfw", "202210", "31"));
    validateRow(response, List.of("ETdvuOmTpc6", "lc3eMKXaEfw", "202212", "28"));
    validateRow(response, List.of("BzQkRWHS7lu", "O6uvpzGd5pu", "202209", "100"));
    validateRow(response, List.of("BzQkRWHS7lu", "O6uvpzGd5pu", "202210", "95"));
    validateRow(response, List.of("BzQkRWHS7lu", "O6uvpzGd5pu", "202212", "92"));
    validateRow(response, List.of("BzQkRWHS7lu", "O6uvpzGd5pu", "202208", "91"));
    validateRow(response, List.of("BzQkRWHS7lu", "O6uvpzGd5pu", "202207", "90"));
    validateRow(response, List.of("BzQkRWHS7lu", "O6uvpzGd5pu", "202211", "85"));
    validateRow(response, List.of("BzQkRWHS7lu", "fdc6uOvgoji", "202208", "77"));
    validateRow(response, List.of("BzQkRWHS7lu", "fdc6uOvgoji", "202207", "70"));
    validateRow(response, List.of("BzQkRWHS7lu", "fdc6uOvgoji", "202210", "65"));
    validateRow(response, List.of("BzQkRWHS7lu", "fdc6uOvgoji", "202211", "61"));
    validateRow(response, List.of("BzQkRWHS7lu", "fdc6uOvgoji", "202212", "57"));
    validateRow(response, List.of("BzQkRWHS7lu", "fdc6uOvgoji", "202209", "52"));
    validateRow(response, List.of("BzQkRWHS7lu", "jUb8gELQApl", "202211", "50"));
    validateRow(response, List.of("BzQkRWHS7lu", "jUb8gELQApl", "202207", "49"));
    validateRow(response, List.of("BzQkRWHS7lu", "jUb8gELQApl", "202209", "48"));
    validateRow(response, List.of("BzQkRWHS7lu", "jUb8gELQApl", "202212", "45"));
    validateRow(response, List.of("BzQkRWHS7lu", "jUb8gELQApl", "202210", "44"));
    validateRow(response, List.of("BzQkRWHS7lu", "jUb8gELQApl", "202208", "44"));
    validateRow(response, List.of("BzQkRWHS7lu", "lc3eMKXaEfw", "202207", "42"));
    validateRow(response, List.of("BzQkRWHS7lu", "lc3eMKXaEfw", "202210", "35"));
    validateRow(response, List.of("BzQkRWHS7lu", "lc3eMKXaEfw", "202211", "33"));
    validateRow(response, List.of("BzQkRWHS7lu", "lc3eMKXaEfw", "202208", "33"));
    validateRow(response, List.of("BzQkRWHS7lu", "lc3eMKXaEfw", "202212", "28"));
    validateRow(response, List.of("BzQkRWHS7lu", "lc3eMKXaEfw", "202209", "27"));
    validateRow(response, List.of("", "O6uvpzGd5pu", "202208", "3"));
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
    validateRow(response, List.of("9", "ImspTQPwCqd", "202212", "59"));
    validateRow(response, List.of("9", "ImspTQPwCqd", "202207", "55"));
    validateRow(response, List.of("9", "ImspTQPwCqd", "202208", "49"));
    validateRow(response, List.of("9", "ImspTQPwCqd", "202210", "43"));
    validateRow(response, List.of("9", "ImspTQPwCqd", "202211", "36"));
    validateRow(response, List.of("9", "ImspTQPwCqd", "202209", "35"));
    validateRow(response, List.of("88", "ImspTQPwCqd", "202211", "58"));
    validateRow(response, List.of("88", "ImspTQPwCqd", "202208", "55"));
    validateRow(response, List.of("88", "ImspTQPwCqd", "202212", "54"));
    validateRow(response, List.of("88", "ImspTQPwCqd", "202210", "50"));
    validateRow(response, List.of("88", "ImspTQPwCqd", "202207", "50"));
    validateRow(response, List.of("88", "ImspTQPwCqd", "202209", "46"));
    validateRow(response, List.of("87", "ImspTQPwCqd", "202212", "54"));
    validateRow(response, List.of("87", "ImspTQPwCqd", "202209", "53"));
    validateRow(response, List.of("87", "ImspTQPwCqd", "202210", "53"));
    validateRow(response, List.of("87", "ImspTQPwCqd", "202207", "52"));
    validateRow(response, List.of("87", "ImspTQPwCqd", "202211", "49"));
    validateRow(response, List.of("87", "ImspTQPwCqd", "202208", "46"));
    validateRow(response, List.of("86", "ImspTQPwCqd", "202207", "66"));
    validateRow(response, List.of("86", "ImspTQPwCqd", "202208", "55"));
    validateRow(response, List.of("86", "ImspTQPwCqd", "202209", "47"));
    validateRow(response, List.of("86", "ImspTQPwCqd", "202210", "46"));
    validateRow(response, List.of("86", "ImspTQPwCqd", "202211", "39"));
    validateRow(response, List.of("86", "ImspTQPwCqd", "202212", "36"));
    validateRow(response, List.of("85", "ImspTQPwCqd", "202210", "62"));
    validateRow(response, List.of("85", "ImspTQPwCqd", "202211", "59"));
    validateRow(response, List.of("85", "ImspTQPwCqd", "202208", "52"));
    validateRow(response, List.of("85", "ImspTQPwCqd", "202209", "51"));
    validateRow(response, List.of("85", "ImspTQPwCqd", "202212", "45"));
    validateRow(response, List.of("85", "ImspTQPwCqd", "202207", "39"));
    validateRow(response, List.of("84", "ImspTQPwCqd", "202207", "65"));
    validateRow(response, List.of("84", "ImspTQPwCqd", "202211", "63"));
    validateRow(response, List.of("84", "ImspTQPwCqd", "202209", "54"));
    validateRow(response, List.of("84", "ImspTQPwCqd", "202210", "52"));
    validateRow(response, List.of("84", "ImspTQPwCqd", "202212", "50"));
    validateRow(response, List.of("84", "ImspTQPwCqd", "202208", "47"));
    validateRow(response, List.of("83", "ImspTQPwCqd", "202209", "66"));
    validateRow(response, List.of("83", "ImspTQPwCqd", "202210", "60"));
    validateRow(response, List.of("83", "ImspTQPwCqd", "202207", "57"));
    validateRow(response, List.of("83", "ImspTQPwCqd", "202211", "54"));
    validateRow(response, List.of("83", "ImspTQPwCqd", "202208", "52"));
    validateRow(response, List.of("83", "ImspTQPwCqd", "202212", "51"));
    validateRow(response, List.of("82", "ImspTQPwCqd", "202207", "63"));
    validateRow(response, List.of("82", "ImspTQPwCqd", "202210", "61"));
    validateRow(response, List.of("82", "ImspTQPwCqd", "202208", "52"));
    validateRow(response, List.of("82", "ImspTQPwCqd", "202212", "44"));
    validateRow(response, List.of("82", "ImspTQPwCqd", "202211", "43"));
    validateRow(response, List.of("82", "ImspTQPwCqd", "202209", "43"));
    validateRow(response, List.of("81", "ImspTQPwCqd", "202207", "50"));
    validateRow(response, List.of("81", "ImspTQPwCqd", "202208", "48"));
    validateRow(response, List.of("81", "ImspTQPwCqd", "202211", "45"));
    validateRow(response, List.of("81", "ImspTQPwCqd", "202209", "44"));
    validateRow(response, List.of("81", "ImspTQPwCqd", "202210", "44"));
    validateRow(response, List.of("81", "ImspTQPwCqd", "202212", "39"));
    validateRow(response, List.of("80", "ImspTQPwCqd", "202208", "57"));
    validateRow(response, List.of("80", "ImspTQPwCqd", "202209", "54"));
    validateRow(response, List.of("80", "ImspTQPwCqd", "202211", "52"));
    validateRow(response, List.of("80", "ImspTQPwCqd", "202210", "50"));
    validateRow(response, List.of("80", "ImspTQPwCqd", "202207", "49"));
    validateRow(response, List.of("80", "ImspTQPwCqd", "202212", "44"));
    validateRow(response, List.of("8", "ImspTQPwCqd", "202210", "67"));
    validateRow(response, List.of("8", "ImspTQPwCqd", "202211", "62"));
    validateRow(response, List.of("8", "ImspTQPwCqd", "202207", "61"));
    validateRow(response, List.of("8", "ImspTQPwCqd", "202209", "56"));
    validateRow(response, List.of("8", "ImspTQPwCqd", "202208", "50"));
    validateRow(response, List.of("8", "ImspTQPwCqd", "202212", "45"));
    validateRow(response, List.of("79", "ImspTQPwCqd", "202208", "60"));
    validateRow(response, List.of("79", "ImspTQPwCqd", "202210", "57"));
    validateRow(response, List.of("79", "ImspTQPwCqd", "202212", "56"));
    validateRow(response, List.of("79", "ImspTQPwCqd", "202207", "55"));
    validateRow(response, List.of("79", "ImspTQPwCqd", "202209", "55"));
    validateRow(response, List.of("79", "ImspTQPwCqd", "202211", "44"));
    validateRow(response, List.of("78", "ImspTQPwCqd", "202207", "60"));
    validateRow(response, List.of("78", "ImspTQPwCqd", "202209", "58"));
    validateRow(response, List.of("78", "ImspTQPwCqd", "202208", "56"));
    validateRow(response, List.of("78", "ImspTQPwCqd", "202212", "51"));
    validateRow(response, List.of("78", "ImspTQPwCqd", "202211", "50"));
    validateRow(response, List.of("78", "ImspTQPwCqd", "202210", "41"));
    validateRow(response, List.of("77", "ImspTQPwCqd", "202210", "61"));
    validateRow(response, List.of("77", "ImspTQPwCqd", "202211", "54"));
    validateRow(response, List.of("77", "ImspTQPwCqd", "202209", "54"));
    validateRow(response, List.of("77", "ImspTQPwCqd", "202207", "47"));
    validateRow(response, List.of("77", "ImspTQPwCqd", "202212", "46"));
    validateRow(response, List.of("77", "ImspTQPwCqd", "202208", "44"));
    validateRow(response, List.of("76", "ImspTQPwCqd", "202207", "64"));
    validateRow(response, List.of("76", "ImspTQPwCqd", "202211", "58"));
    validateRow(response, List.of("76", "ImspTQPwCqd", "202209", "51"));
    validateRow(response, List.of("76", "ImspTQPwCqd", "202210", "49"));
    validateRow(response, List.of("76", "ImspTQPwCqd", "202208", "49"));
    validateRow(response, List.of("76", "ImspTQPwCqd", "202212", "49"));
    validateRow(response, List.of("75", "ImspTQPwCqd", "202208", "71"));
    validateRow(response, List.of("75", "ImspTQPwCqd", "202210", "61"));
    validateRow(response, List.of("75", "ImspTQPwCqd", "202212", "53"));
    validateRow(response, List.of("75", "ImspTQPwCqd", "202211", "51"));
    validateRow(response, List.of("75", "ImspTQPwCqd", "202207", "49"));
    validateRow(response, List.of("75", "ImspTQPwCqd", "202209", "47"));
    validateRow(response, List.of("74", "ImspTQPwCqd", "202208", "77"));
    validateRow(response, List.of("74", "ImspTQPwCqd", "202207", "57"));
    validateRow(response, List.of("74", "ImspTQPwCqd", "202210", "55"));
    validateRow(response, List.of("74", "ImspTQPwCqd", "202211", "51"));
    validateRow(response, List.of("74", "ImspTQPwCqd", "202209", "48"));
    validateRow(response, List.of("74", "ImspTQPwCqd", "202212", "38"));
    validateRow(response, List.of("73", "ImspTQPwCqd", "202208", "67"));
    validateRow(response, List.of("73", "ImspTQPwCqd", "202207", "57"));
    validateRow(response, List.of("73", "ImspTQPwCqd", "202211", "52"));
    validateRow(response, List.of("73", "ImspTQPwCqd", "202209", "47"));
    validateRow(response, List.of("73", "ImspTQPwCqd", "202212", "44"));
    validateRow(response, List.of("73", "ImspTQPwCqd", "202210", "43"));
    validateRow(response, List.of("72", "ImspTQPwCqd", "202212", "59"));
    validateRow(response, List.of("72", "ImspTQPwCqd", "202207", "58"));
    validateRow(response, List.of("72", "ImspTQPwCqd", "202208", "57"));
    validateRow(response, List.of("72", "ImspTQPwCqd", "202211", "55"));
    validateRow(response, List.of("72", "ImspTQPwCqd", "202209", "50"));
    validateRow(response, List.of("72", "ImspTQPwCqd", "202210", "44"));
    validateRow(response, List.of("71", "ImspTQPwCqd", "202208", "61"));
    validateRow(response, List.of("71", "ImspTQPwCqd", "202210", "52"));
    validateRow(response, List.of("71", "ImspTQPwCqd", "202211", "51"));
    validateRow(response, List.of("71", "ImspTQPwCqd", "202207", "51"));
    validateRow(response, List.of("71", "ImspTQPwCqd", "202212", "39"));
    validateRow(response, List.of("71", "ImspTQPwCqd", "202209", "38"));
    validateRow(response, List.of("70", "ImspTQPwCqd", "202211", "69"));
    validateRow(response, List.of("70", "ImspTQPwCqd", "202207", "53"));
    validateRow(response, List.of("70", "ImspTQPwCqd", "202212", "46"));
    validateRow(response, List.of("70", "ImspTQPwCqd", "202208", "46"));
    validateRow(response, List.of("70", "ImspTQPwCqd", "202210", "45"));
    validateRow(response, List.of("70", "ImspTQPwCqd", "202209", "43"));
    validateRow(response, List.of("7", "ImspTQPwCqd", "202209", "56"));
    validateRow(response, List.of("7", "ImspTQPwCqd", "202208", "53"));
    validateRow(response, List.of("7", "ImspTQPwCqd", "202211", "53"));
    validateRow(response, List.of("7", "ImspTQPwCqd", "202207", "51"));
    validateRow(response, List.of("7", "ImspTQPwCqd", "202212", "45"));
    validateRow(response, List.of("7", "ImspTQPwCqd", "202210", "38"));
    validateRow(response, List.of("69", "ImspTQPwCqd", "202210", "66"));
    validateRow(response, List.of("69", "ImspTQPwCqd", "202207", "53"));
    validateRow(response, List.of("69", "ImspTQPwCqd", "202209", "51"));
    validateRow(response, List.of("69", "ImspTQPwCqd", "202212", "48"));
    validateRow(response, List.of("69", "ImspTQPwCqd", "202211", "45"));
    validateRow(response, List.of("69", "ImspTQPwCqd", "202208", "43"));
    validateRow(response, List.of("68", "ImspTQPwCqd", "202211", "67"));
    validateRow(response, List.of("68", "ImspTQPwCqd", "202210", "54"));
    validateRow(response, List.of("68", "ImspTQPwCqd", "202212", "53"));
    validateRow(response, List.of("68", "ImspTQPwCqd", "202207", "51"));
    validateRow(response, List.of("68", "ImspTQPwCqd", "202209", "49"));
    validateRow(response, List.of("68", "ImspTQPwCqd", "202208", "40"));
    validateRow(response, List.of("67", "ImspTQPwCqd", "202208", "63"));
    validateRow(response, List.of("67", "ImspTQPwCqd", "202211", "53"));
    validateRow(response, List.of("67", "ImspTQPwCqd", "202212", "51"));
    validateRow(response, List.of("67", "ImspTQPwCqd", "202209", "50"));
    validateRow(response, List.of("67", "ImspTQPwCqd", "202207", "49"));
    validateRow(response, List.of("67", "ImspTQPwCqd", "202210", "45"));
    validateRow(response, List.of("66", "ImspTQPwCqd", "202212", "57"));
    validateRow(response, List.of("66", "ImspTQPwCqd", "202207", "54"));
    validateRow(response, List.of("66", "ImspTQPwCqd", "202208", "49"));
    validateRow(response, List.of("66", "ImspTQPwCqd", "202209", "46"));
    validateRow(response, List.of("66", "ImspTQPwCqd", "202210", "43"));
    validateRow(response, List.of("66", "ImspTQPwCqd", "202211", "39"));
    validateRow(response, List.of("65", "ImspTQPwCqd", "202208", "67"));
    validateRow(response, List.of("65", "ImspTQPwCqd", "202212", "58"));
    validateRow(response, List.of("65", "ImspTQPwCqd", "202210", "52"));
    validateRow(response, List.of("65", "ImspTQPwCqd", "202211", "52"));
    validateRow(response, List.of("65", "ImspTQPwCqd", "202209", "49"));
    validateRow(response, List.of("65", "ImspTQPwCqd", "202207", "43"));
    validateRow(response, List.of("64", "ImspTQPwCqd", "202208", "58"));
    validateRow(response, List.of("64", "ImspTQPwCqd", "202211", "56"));
    validateRow(response, List.of("64", "ImspTQPwCqd", "202210", "56"));
    validateRow(response, List.of("64", "ImspTQPwCqd", "202207", "54"));
    validateRow(response, List.of("64", "ImspTQPwCqd", "202209", "52"));
    validateRow(response, List.of("64", "ImspTQPwCqd", "202212", "41"));
    validateRow(response, List.of("63", "ImspTQPwCqd", "202210", "62"));
    validateRow(response, List.of("63", "ImspTQPwCqd", "202209", "56"));
    validateRow(response, List.of("63", "ImspTQPwCqd", "202207", "56"));
    validateRow(response, List.of("63", "ImspTQPwCqd", "202211", "45"));
    validateRow(response, List.of("63", "ImspTQPwCqd", "202212", "45"));
    validateRow(response, List.of("63", "ImspTQPwCqd", "202208", "44"));
    validateRow(response, List.of("62", "ImspTQPwCqd", "202209", "64"));
    validateRow(response, List.of("62", "ImspTQPwCqd", "202211", "52"));
    validateRow(response, List.of("62", "ImspTQPwCqd", "202210", "49"));
    validateRow(response, List.of("62", "ImspTQPwCqd", "202207", "49"));
    validateRow(response, List.of("62", "ImspTQPwCqd", "202212", "45"));
    validateRow(response, List.of("62", "ImspTQPwCqd", "202208", "45"));
    validateRow(response, List.of("61", "ImspTQPwCqd", "202210", "54"));
    validateRow(response, List.of("61", "ImspTQPwCqd", "202207", "51"));
    validateRow(response, List.of("61", "ImspTQPwCqd", "202212", "50"));
    validateRow(response, List.of("61", "ImspTQPwCqd", "202211", "49"));
    validateRow(response, List.of("61", "ImspTQPwCqd", "202209", "49"));
    validateRow(response, List.of("61", "ImspTQPwCqd", "202208", "47"));
    validateRow(response, List.of("60", "ImspTQPwCqd", "202207", "64"));
    validateRow(response, List.of("60", "ImspTQPwCqd", "202208", "52"));
    validateRow(response, List.of("60", "ImspTQPwCqd", "202209", "49"));
    validateRow(response, List.of("60", "ImspTQPwCqd", "202210", "43"));
    validateRow(response, List.of("60", "ImspTQPwCqd", "202212", "42"));
    validateRow(response, List.of("60", "ImspTQPwCqd", "202211", "41"));
    validateRow(response, List.of("6", "ImspTQPwCqd", "202210", "61"));
    validateRow(response, List.of("6", "ImspTQPwCqd", "202209", "56"));
    validateRow(response, List.of("6", "ImspTQPwCqd", "202211", "52"));
    validateRow(response, List.of("6", "ImspTQPwCqd", "202208", "50"));
    validateRow(response, List.of("6", "ImspTQPwCqd", "202212", "49"));
    validateRow(response, List.of("6", "ImspTQPwCqd", "202207", "46"));
    validateRow(response, List.of("59", "ImspTQPwCqd", "202208", "68"));
    validateRow(response, List.of("59", "ImspTQPwCqd", "202209", "60"));
    validateRow(response, List.of("59", "ImspTQPwCqd", "202207", "57"));
    validateRow(response, List.of("59", "ImspTQPwCqd", "202211", "52"));
    validateRow(response, List.of("59", "ImspTQPwCqd", "202210", "51"));
    validateRow(response, List.of("59", "ImspTQPwCqd", "202212", "48"));
    validateRow(response, List.of("58", "ImspTQPwCqd", "202207", "49"));
    validateRow(response, List.of("58", "ImspTQPwCqd", "202208", "49"));
    validateRow(response, List.of("58", "ImspTQPwCqd", "202210", "48"));
    validateRow(response, List.of("58", "ImspTQPwCqd", "202212", "48"));
    validateRow(response, List.of("58", "ImspTQPwCqd", "202209", "47"));
    validateRow(response, List.of("58", "ImspTQPwCqd", "202211", "46"));
    validateRow(response, List.of("57", "ImspTQPwCqd", "202211", "64"));
    validateRow(response, List.of("57", "ImspTQPwCqd", "202209", "58"));
    validateRow(response, List.of("57", "ImspTQPwCqd", "202208", "57"));
    validateRow(response, List.of("57", "ImspTQPwCqd", "202207", "55"));
    validateRow(response, List.of("57", "ImspTQPwCqd", "202212", "53"));
    validateRow(response, List.of("57", "ImspTQPwCqd", "202210", "43"));
    validateRow(response, List.of("56", "ImspTQPwCqd", "202209", "64"));
    validateRow(response, List.of("56", "ImspTQPwCqd", "202208", "62"));
    validateRow(response, List.of("56", "ImspTQPwCqd", "202207", "51"));
    validateRow(response, List.of("56", "ImspTQPwCqd", "202211", "50"));
    validateRow(response, List.of("56", "ImspTQPwCqd", "202212", "47"));
    validateRow(response, List.of("56", "ImspTQPwCqd", "202210", "43"));
    validateRow(response, List.of("55", "ImspTQPwCqd", "202209", "59"));
    validateRow(response, List.of("55", "ImspTQPwCqd", "202211", "57"));
    validateRow(response, List.of("55", "ImspTQPwCqd", "202210", "54"));
    validateRow(response, List.of("55", "ImspTQPwCqd", "202207", "54"));
    validateRow(response, List.of("55", "ImspTQPwCqd", "202212", "48"));
    validateRow(response, List.of("55", "ImspTQPwCqd", "202208", "46"));
    validateRow(response, List.of("54", "ImspTQPwCqd", "202211", "64"));
    validateRow(response, List.of("54", "ImspTQPwCqd", "202207", "63"));
    validateRow(response, List.of("54", "ImspTQPwCqd", "202212", "55"));
    validateRow(response, List.of("54", "ImspTQPwCqd", "202209", "54"));
    validateRow(response, List.of("54", "ImspTQPwCqd", "202208", "50"));
    validateRow(response, List.of("54", "ImspTQPwCqd", "202210", "50"));
    validateRow(response, List.of("53", "ImspTQPwCqd", "202209", "55"));
    validateRow(response, List.of("53", "ImspTQPwCqd", "202211", "53"));
    validateRow(response, List.of("53", "ImspTQPwCqd", "202207", "52"));
    validateRow(response, List.of("53", "ImspTQPwCqd", "202212", "50"));
    validateRow(response, List.of("53", "ImspTQPwCqd", "202210", "47"));
    validateRow(response, List.of("53", "ImspTQPwCqd", "202208", "46"));
    validateRow(response, List.of("52", "ImspTQPwCqd", "202207", "53"));
    validateRow(response, List.of("52", "ImspTQPwCqd", "202212", "50"));
    validateRow(response, List.of("52", "ImspTQPwCqd", "202208", "50"));
    validateRow(response, List.of("52", "ImspTQPwCqd", "202209", "42"));
    validateRow(response, List.of("52", "ImspTQPwCqd", "202210", "41"));
    validateRow(response, List.of("52", "ImspTQPwCqd", "202211", "41"));
    validateRow(response, List.of("51", "ImspTQPwCqd", "202208", "59"));
    validateRow(response, List.of("51", "ImspTQPwCqd", "202209", "51"));
    validateRow(response, List.of("51", "ImspTQPwCqd", "202207", "49"));
    validateRow(response, List.of("51", "ImspTQPwCqd", "202212", "46"));
    validateRow(response, List.of("51", "ImspTQPwCqd", "202211", "41"));
    validateRow(response, List.of("51", "ImspTQPwCqd", "202210", "39"));
    validateRow(response, List.of("50", "ImspTQPwCqd", "202208", "57"));
    validateRow(response, List.of("50", "ImspTQPwCqd", "202211", "47"));
    validateRow(response, List.of("50", "ImspTQPwCqd", "202212", "47"));
    validateRow(response, List.of("50", "ImspTQPwCqd", "202209", "46"));
    validateRow(response, List.of("50", "ImspTQPwCqd", "202210", "46"));
    validateRow(response, List.of("50", "ImspTQPwCqd", "202207", "44"));
    validateRow(response, List.of("5", "ImspTQPwCqd", "202207", "56"));
    validateRow(response, List.of("5", "ImspTQPwCqd", "202209", "54"));
    validateRow(response, List.of("5", "ImspTQPwCqd", "202211", "48"));
    validateRow(response, List.of("5", "ImspTQPwCqd", "202208", "46"));
    validateRow(response, List.of("5", "ImspTQPwCqd", "202210", "44"));
    validateRow(response, List.of("5", "ImspTQPwCqd", "202212", "36"));
    validateRow(response, List.of("49", "ImspTQPwCqd", "202212", "64"));
    validateRow(response, List.of("49", "ImspTQPwCqd", "202211", "59"));
    validateRow(response, List.of("49", "ImspTQPwCqd", "202209", "55"));
    validateRow(response, List.of("49", "ImspTQPwCqd", "202207", "55"));
    validateRow(response, List.of("49", "ImspTQPwCqd", "202210", "49"));
    validateRow(response, List.of("49", "ImspTQPwCqd", "202208", "49"));
    validateRow(response, List.of("48", "ImspTQPwCqd", "202212", "66"));
    validateRow(response, List.of("48", "ImspTQPwCqd", "202211", "58"));
    validateRow(response, List.of("48", "ImspTQPwCqd", "202210", "54"));
    validateRow(response, List.of("48", "ImspTQPwCqd", "202207", "54"));
    validateRow(response, List.of("48", "ImspTQPwCqd", "202209", "48"));
    validateRow(response, List.of("48", "ImspTQPwCqd", "202208", "41"));
    validateRow(response, List.of("47", "ImspTQPwCqd", "202208", "58"));
    validateRow(response, List.of("47", "ImspTQPwCqd", "202207", "53"));
    validateRow(response, List.of("47", "ImspTQPwCqd", "202212", "52"));
    validateRow(response, List.of("47", "ImspTQPwCqd", "202209", "49"));
    validateRow(response, List.of("47", "ImspTQPwCqd", "202210", "49"));
    validateRow(response, List.of("47", "ImspTQPwCqd", "202211", "40"));
    validateRow(response, List.of("46", "ImspTQPwCqd", "202208", "57"));
    validateRow(response, List.of("46", "ImspTQPwCqd", "202211", "55"));
    validateRow(response, List.of("46", "ImspTQPwCqd", "202210", "54"));
    validateRow(response, List.of("46", "ImspTQPwCqd", "202212", "52"));
    validateRow(response, List.of("46", "ImspTQPwCqd", "202209", "46"));
    validateRow(response, List.of("46", "ImspTQPwCqd", "202207", "41"));
    validateRow(response, List.of("45", "ImspTQPwCqd", "202207", "66"));
    validateRow(response, List.of("45", "ImspTQPwCqd", "202210", "58"));
    validateRow(response, List.of("45", "ImspTQPwCqd", "202209", "53"));
    validateRow(response, List.of("45", "ImspTQPwCqd", "202208", "50"));
    validateRow(response, List.of("45", "ImspTQPwCqd", "202211", "45"));
    validateRow(response, List.of("45", "ImspTQPwCqd", "202212", "44"));
    validateRow(response, List.of("44", "ImspTQPwCqd", "202207", "58"));
    validateRow(response, List.of("44", "ImspTQPwCqd", "202210", "57"));
    validateRow(response, List.of("44", "ImspTQPwCqd", "202208", "55"));
    validateRow(response, List.of("44", "ImspTQPwCqd", "202212", "54"));
    validateRow(response, List.of("44", "ImspTQPwCqd", "202211", "50"));
    validateRow(response, List.of("44", "ImspTQPwCqd", "202209", "40"));
    validateRow(response, List.of("43", "ImspTQPwCqd", "202210", "60"));
    validateRow(response, List.of("43", "ImspTQPwCqd", "202207", "60"));
    validateRow(response, List.of("43", "ImspTQPwCqd", "202212", "50"));
    validateRow(response, List.of("43", "ImspTQPwCqd", "202209", "49"));
    validateRow(response, List.of("43", "ImspTQPwCqd", "202211", "47"));
    validateRow(response, List.of("43", "ImspTQPwCqd", "202208", "35"));
    validateRow(response, List.of("42", "ImspTQPwCqd", "202209", "59"));
    validateRow(response, List.of("42", "ImspTQPwCqd", "202210", "54"));
    validateRow(response, List.of("42", "ImspTQPwCqd", "202208", "52"));
    validateRow(response, List.of("42", "ImspTQPwCqd", "202212", "50"));
    validateRow(response, List.of("42", "ImspTQPwCqd", "202211", "45"));
    validateRow(response, List.of("42", "ImspTQPwCqd", "202207", "42"));
    validateRow(response, List.of("41", "ImspTQPwCqd", "202212", "52"));
    validateRow(response, List.of("41", "ImspTQPwCqd", "202210", "51"));
    validateRow(response, List.of("41", "ImspTQPwCqd", "202209", "51"));
    validateRow(response, List.of("41", "ImspTQPwCqd", "202211", "47"));
    validateRow(response, List.of("41", "ImspTQPwCqd", "202208", "47"));
    validateRow(response, List.of("41", "ImspTQPwCqd", "202207", "45"));
    validateRow(response, List.of("40", "ImspTQPwCqd", "202207", "58"));
    validateRow(response, List.of("40", "ImspTQPwCqd", "202212", "56"));
    validateRow(response, List.of("40", "ImspTQPwCqd", "202208", "52"));
    validateRow(response, List.of("40", "ImspTQPwCqd", "202209", "51"));
    validateRow(response, List.of("40", "ImspTQPwCqd", "202210", "50"));
    validateRow(response, List.of("40", "ImspTQPwCqd", "202211", "47"));
    validateRow(response, List.of("4", "ImspTQPwCqd", "202210", "60"));
    validateRow(response, List.of("4", "ImspTQPwCqd", "202212", "57"));
    validateRow(response, List.of("4", "ImspTQPwCqd", "202208", "56"));
    validateRow(response, List.of("4", "ImspTQPwCqd", "202211", "55"));
    validateRow(response, List.of("4", "ImspTQPwCqd", "202209", "53"));
    validateRow(response, List.of("4", "ImspTQPwCqd", "202207", "37"));
    validateRow(response, List.of("39", "ImspTQPwCqd", "202207", "61"));
    validateRow(response, List.of("39", "ImspTQPwCqd", "202211", "60"));
    validateRow(response, List.of("39", "ImspTQPwCqd", "202208", "48"));
    validateRow(response, List.of("39", "ImspTQPwCqd", "202209", "42"));
    validateRow(response, List.of("39", "ImspTQPwCqd", "202210", "40"));
    validateRow(response, List.of("39", "ImspTQPwCqd", "202212", "38"));
    validateRow(response, List.of("38", "ImspTQPwCqd", "202207", "54"));
    validateRow(response, List.of("38", "ImspTQPwCqd", "202212", "54"));
    validateRow(response, List.of("38", "ImspTQPwCqd", "202209", "53"));
    validateRow(response, List.of("38", "ImspTQPwCqd", "202210", "48"));
    validateRow(response, List.of("38", "ImspTQPwCqd", "202211", "47"));
    validateRow(response, List.of("38", "ImspTQPwCqd", "202208", "42"));
    validateRow(response, List.of("37", "ImspTQPwCqd", "202211", "65"));
    validateRow(response, List.of("37", "ImspTQPwCqd", "202207", "60"));
    validateRow(response, List.of("37", "ImspTQPwCqd", "202212", "54"));
    validateRow(response, List.of("37", "ImspTQPwCqd", "202210", "52"));
    validateRow(response, List.of("37", "ImspTQPwCqd", "202208", "48"));
    validateRow(response, List.of("37", "ImspTQPwCqd", "202209", "47"));
    validateRow(response, List.of("36", "ImspTQPwCqd", "202210", "61"));
    validateRow(response, List.of("36", "ImspTQPwCqd", "202207", "50"));
    validateRow(response, List.of("36", "ImspTQPwCqd", "202208", "48"));
    validateRow(response, List.of("36", "ImspTQPwCqd", "202209", "48"));
    validateRow(response, List.of("36", "ImspTQPwCqd", "202212", "48"));
    validateRow(response, List.of("36", "ImspTQPwCqd", "202211", "37"));
    validateRow(response, List.of("35", "ImspTQPwCqd", "202209", "56"));
    validateRow(response, List.of("35", "ImspTQPwCqd", "202208", "55"));
    validateRow(response, List.of("35", "ImspTQPwCqd", "202210", "51"));
    validateRow(response, List.of("35", "ImspTQPwCqd", "202212", "48"));
    validateRow(response, List.of("35", "ImspTQPwCqd", "202207", "47"));
    validateRow(response, List.of("35", "ImspTQPwCqd", "202211", "37"));
    validateRow(response, List.of("34", "ImspTQPwCqd", "202210", "61"));
    validateRow(response, List.of("34", "ImspTQPwCqd", "202207", "58"));
    validateRow(response, List.of("34", "ImspTQPwCqd", "202208", "50"));
    validateRow(response, List.of("34", "ImspTQPwCqd", "202212", "45"));
    validateRow(response, List.of("34", "ImspTQPwCqd", "202211", "45"));
    validateRow(response, List.of("34", "ImspTQPwCqd", "202209", "45"));
    validateRow(response, List.of("33", "ImspTQPwCqd", "202210", "54"));
    validateRow(response, List.of("33", "ImspTQPwCqd", "202207", "51"));
    validateRow(response, List.of("33", "ImspTQPwCqd", "202212", "47"));
    validateRow(response, List.of("33", "ImspTQPwCqd", "202211", "46"));
    validateRow(response, List.of("33", "ImspTQPwCqd", "202208", "42"));
    validateRow(response, List.of("33", "ImspTQPwCqd", "202209", "41"));
    validateRow(response, List.of("32", "ImspTQPwCqd", "202207", "58"));
    validateRow(response, List.of("32", "ImspTQPwCqd", "202210", "54"));
    validateRow(response, List.of("32", "ImspTQPwCqd", "202208", "51"));
    validateRow(response, List.of("32", "ImspTQPwCqd", "202211", "49"));
    validateRow(response, List.of("32", "ImspTQPwCqd", "202209", "47"));
    validateRow(response, List.of("32", "ImspTQPwCqd", "202212", "43"));
    validateRow(response, List.of("31", "ImspTQPwCqd", "202208", "64"));
    validateRow(response, List.of("31", "ImspTQPwCqd", "202207", "60"));
    validateRow(response, List.of("31", "ImspTQPwCqd", "202210", "51"));
    validateRow(response, List.of("31", "ImspTQPwCqd", "202211", "47"));
    validateRow(response, List.of("31", "ImspTQPwCqd", "202209", "46"));
    validateRow(response, List.of("31", "ImspTQPwCqd", "202212", "34"));
    validateRow(response, List.of("30", "ImspTQPwCqd", "202209", "67"));
    validateRow(response, List.of("30", "ImspTQPwCqd", "202207", "60"));
    validateRow(response, List.of("30", "ImspTQPwCqd", "202208", "58"));
    validateRow(response, List.of("30", "ImspTQPwCqd", "202210", "54"));
    validateRow(response, List.of("30", "ImspTQPwCqd", "202212", "49"));
    validateRow(response, List.of("30", "ImspTQPwCqd", "202211", "49"));
    validateRow(response, List.of("3", "ImspTQPwCqd", "202211", "63"));
    validateRow(response, List.of("3", "ImspTQPwCqd", "202208", "57"));
    validateRow(response, List.of("3", "ImspTQPwCqd", "202210", "46"));
    validateRow(response, List.of("3", "ImspTQPwCqd", "202212", "46"));
    validateRow(response, List.of("3", "ImspTQPwCqd", "202209", "42"));
    validateRow(response, List.of("3", "ImspTQPwCqd", "202207", "41"));
    validateRow(response, List.of("29", "ImspTQPwCqd", "202210", "62"));
    validateRow(response, List.of("29", "ImspTQPwCqd", "202209", "57"));
    validateRow(response, List.of("29", "ImspTQPwCqd", "202211", "55"));
    validateRow(response, List.of("29", "ImspTQPwCqd", "202207", "52"));
    validateRow(response, List.of("29", "ImspTQPwCqd", "202212", "52"));
    validateRow(response, List.of("29", "ImspTQPwCqd", "202208", "44"));
    validateRow(response, List.of("28", "ImspTQPwCqd", "202211", "60"));
    validateRow(response, List.of("28", "ImspTQPwCqd", "202210", "50"));
    validateRow(response, List.of("28", "ImspTQPwCqd", "202209", "48"));
    validateRow(response, List.of("28", "ImspTQPwCqd", "202208", "45"));
    validateRow(response, List.of("28", "ImspTQPwCqd", "202207", "44"));
    validateRow(response, List.of("28", "ImspTQPwCqd", "202212", "43"));
    validateRow(response, List.of("27", "ImspTQPwCqd", "202207", "57"));
    validateRow(response, List.of("27", "ImspTQPwCqd", "202210", "53"));
    validateRow(response, List.of("27", "ImspTQPwCqd", "202211", "52"));
    validateRow(response, List.of("27", "ImspTQPwCqd", "202209", "52"));
    validateRow(response, List.of("27", "ImspTQPwCqd", "202212", "41"));
    validateRow(response, List.of("27", "ImspTQPwCqd", "202208", "35"));
    validateRow(response, List.of("26", "ImspTQPwCqd", "202210", "57"));
    validateRow(response, List.of("26", "ImspTQPwCqd", "202212", "54"));
    validateRow(response, List.of("26", "ImspTQPwCqd", "202207", "49"));
    validateRow(response, List.of("26", "ImspTQPwCqd", "202208", "45"));
    validateRow(response, List.of("26", "ImspTQPwCqd", "202211", "41"));
    validateRow(response, List.of("26", "ImspTQPwCqd", "202209", "36"));
    validateRow(response, List.of("25", "ImspTQPwCqd", "202210", "59"));
    validateRow(response, List.of("25", "ImspTQPwCqd", "202209", "59"));
    validateRow(response, List.of("25", "ImspTQPwCqd", "202207", "53"));
    validateRow(response, List.of("25", "ImspTQPwCqd", "202211", "52"));
    validateRow(response, List.of("25", "ImspTQPwCqd", "202212", "49"));
    validateRow(response, List.of("25", "ImspTQPwCqd", "202208", "48"));
    validateRow(response, List.of("24", "ImspTQPwCqd", "202208", "64"));
    validateRow(response, List.of("24", "ImspTQPwCqd", "202209", "60"));
    validateRow(response, List.of("24", "ImspTQPwCqd", "202210", "54"));
    validateRow(response, List.of("24", "ImspTQPwCqd", "202211", "50"));
    validateRow(response, List.of("24", "ImspTQPwCqd", "202207", "49"));
    validateRow(response, List.of("24", "ImspTQPwCqd", "202212", "47"));
    validateRow(response, List.of("234", "ImspTQPwCqd", "202208", "1"));
    validateRow(response, List.of("23", "ImspTQPwCqd", "202209", "74"));
    validateRow(response, List.of("23", "ImspTQPwCqd", "202210", "54"));
    validateRow(response, List.of("23", "ImspTQPwCqd", "202211", "52"));
    validateRow(response, List.of("23", "ImspTQPwCqd", "202207", "52"));
    validateRow(response, List.of("23", "ImspTQPwCqd", "202212", "45"));
    validateRow(response, List.of("23", "ImspTQPwCqd", "202208", "35"));
    validateRow(response, List.of("22", "ImspTQPwCqd", "202211", "58"));
    validateRow(response, List.of("22", "ImspTQPwCqd", "202210", "54"));
    validateRow(response, List.of("22", "ImspTQPwCqd", "202208", "52"));
    validateRow(response, List.of("22", "ImspTQPwCqd", "202207", "50"));
    validateRow(response, List.of("22", "ImspTQPwCqd", "202212", "45"));
    validateRow(response, List.of("22", "ImspTQPwCqd", "202209", "39"));
    validateRow(response, List.of("21", "ImspTQPwCqd", "202209", "58"));
    validateRow(response, List.of("21", "ImspTQPwCqd", "202210", "58"));
    validateRow(response, List.of("21", "ImspTQPwCqd", "202207", "55"));
    validateRow(response, List.of("21", "ImspTQPwCqd", "202211", "52"));
    validateRow(response, List.of("21", "ImspTQPwCqd", "202208", "46"));
    validateRow(response, List.of("21", "ImspTQPwCqd", "202212", "46"));
    validateRow(response, List.of("20", "ImspTQPwCqd", "202210", "62"));
    validateRow(response, List.of("20", "ImspTQPwCqd", "202207", "60"));
    validateRow(response, List.of("20", "ImspTQPwCqd", "202211", "53"));
    validateRow(response, List.of("20", "ImspTQPwCqd", "202212", "51"));
    validateRow(response, List.of("20", "ImspTQPwCqd", "202208", "43"));
    validateRow(response, List.of("20", "ImspTQPwCqd", "202209", "43"));
    validateRow(response, List.of("2", "ImspTQPwCqd", "202210", "56"));
    validateRow(response, List.of("2", "ImspTQPwCqd", "202208", "52"));
    validateRow(response, List.of("2", "ImspTQPwCqd", "202209", "50"));
    validateRow(response, List.of("2", "ImspTQPwCqd", "202212", "48"));
    validateRow(response, List.of("2", "ImspTQPwCqd", "202207", "43"));
    validateRow(response, List.of("2", "ImspTQPwCqd", "202211", "43"));
    validateRow(response, List.of("19", "ImspTQPwCqd", "202208", "63"));
    validateRow(response, List.of("19", "ImspTQPwCqd", "202207", "58"));
    validateRow(response, List.of("19", "ImspTQPwCqd", "202210", "55"));
    validateRow(response, List.of("19", "ImspTQPwCqd", "202211", "54"));
    validateRow(response, List.of("19", "ImspTQPwCqd", "202212", "53"));
    validateRow(response, List.of("19", "ImspTQPwCqd", "202209", "47"));
    validateRow(response, List.of("18", "ImspTQPwCqd", "202210", "67"));
    validateRow(response, List.of("18", "ImspTQPwCqd", "202208", "60"));
    validateRow(response, List.of("18", "ImspTQPwCqd", "202209", "56"));
    validateRow(response, List.of("18", "ImspTQPwCqd", "202211", "49"));
    validateRow(response, List.of("18", "ImspTQPwCqd", "202207", "49"));
    validateRow(response, List.of("18", "ImspTQPwCqd", "202212", "35"));
    validateRow(response, List.of("17", "ImspTQPwCqd", "202210", "69"));
    validateRow(response, List.of("17", "ImspTQPwCqd", "202211", "62"));
    validateRow(response, List.of("17", "ImspTQPwCqd", "202212", "50"));
    validateRow(response, List.of("17", "ImspTQPwCqd", "202208", "47"));
    validateRow(response, List.of("17", "ImspTQPwCqd", "202207", "47"));
    validateRow(response, List.of("17", "ImspTQPwCqd", "202209", "39"));
    validateRow(response, List.of("16", "ImspTQPwCqd", "202212", "64"));
    validateRow(response, List.of("16", "ImspTQPwCqd", "202211", "56"));
    validateRow(response, List.of("16", "ImspTQPwCqd", "202209", "49"));
    validateRow(response, List.of("16", "ImspTQPwCqd", "202208", "48"));
    validateRow(response, List.of("16", "ImspTQPwCqd", "202207", "47"));
    validateRow(response, List.of("16", "ImspTQPwCqd", "202210", "39"));
    validateRow(response, List.of("15", "ImspTQPwCqd", "202211", "59"));
    validateRow(response, List.of("15", "ImspTQPwCqd", "202209", "57"));
    validateRow(response, List.of("15", "ImspTQPwCqd", "202212", "54"));
    validateRow(response, List.of("15", "ImspTQPwCqd", "202207", "50"));
    validateRow(response, List.of("15", "ImspTQPwCqd", "202208", "46"));
    validateRow(response, List.of("15", "ImspTQPwCqd", "202210", "42"));
    validateRow(response, List.of("14", "ImspTQPwCqd", "202211", "65"));
    validateRow(response, List.of("14", "ImspTQPwCqd", "202209", "63"));
    validateRow(response, List.of("14", "ImspTQPwCqd", "202208", "62"));
    validateRow(response, List.of("14", "ImspTQPwCqd", "202210", "51"));
    validateRow(response, List.of("14", "ImspTQPwCqd", "202212", "47"));
    validateRow(response, List.of("14", "ImspTQPwCqd", "202207", "46"));
    validateRow(response, List.of("13", "ImspTQPwCqd", "202210", "61"));
    validateRow(response, List.of("13", "ImspTQPwCqd", "202212", "59"));
    validateRow(response, List.of("13", "ImspTQPwCqd", "202207", "52"));
    validateRow(response, List.of("13", "ImspTQPwCqd", "202208", "49"));
    validateRow(response, List.of("13", "ImspTQPwCqd", "202209", "46"));
    validateRow(response, List.of("13", "ImspTQPwCqd", "202211", "43"));
    validateRow(response, List.of("12", "ImspTQPwCqd", "202210", "58"));
    validateRow(response, List.of("12", "ImspTQPwCqd", "202208", "53"));
    validateRow(response, List.of("12", "ImspTQPwCqd", "202212", "51"));
    validateRow(response, List.of("12", "ImspTQPwCqd", "202209", "43"));
    validateRow(response, List.of("12", "ImspTQPwCqd", "202211", "42"));
    validateRow(response, List.of("12", "ImspTQPwCqd", "202207", "40"));
    validateRow(response, List.of("11", "ImspTQPwCqd", "202211", "70"));
    validateRow(response, List.of("11", "ImspTQPwCqd", "202209", "60"));
    validateRow(response, List.of("11", "ImspTQPwCqd", "202212", "51"));
    validateRow(response, List.of("11", "ImspTQPwCqd", "202208", "49"));
    validateRow(response, List.of("11", "ImspTQPwCqd", "202207", "48"));
    validateRow(response, List.of("11", "ImspTQPwCqd", "202210", "41"));
    validateRow(response, List.of("10", "ImspTQPwCqd", "202207", "68"));
    validateRow(response, List.of("10", "ImspTQPwCqd", "202208", "58"));
    validateRow(response, List.of("10", "ImspTQPwCqd", "202209", "56"));
    validateRow(response, List.of("10", "ImspTQPwCqd", "202210", "54"));
    validateRow(response, List.of("10", "ImspTQPwCqd", "202211", "48"));
    validateRow(response, List.of("10", "ImspTQPwCqd", "202212", "44"));
    validateRow(response, List.of("1", "ImspTQPwCqd", "202207", "55"));
    validateRow(response, List.of("1", "ImspTQPwCqd", "202209", "52"));
    validateRow(response, List.of("1", "ImspTQPwCqd", "202210", "50"));
    validateRow(response, List.of("1", "ImspTQPwCqd", "202212", "50"));
    validateRow(response, List.of("1", "ImspTQPwCqd", "202211", "50"));
    validateRow(response, List.of("1", "ImspTQPwCqd", "202208", "48"));
    validateRow(response, List.of("0", "ImspTQPwCqd", "202208", "52"));
    validateRow(response, List.of("0", "ImspTQPwCqd", "202210", "50"));
    validateRow(response, List.of("0", "ImspTQPwCqd", "202209", "50"));
    validateRow(response, List.of("0", "ImspTQPwCqd", "202207", "45"));
    validateRow(response, List.of("0", "ImspTQPwCqd", "202211", "43"));
    validateRow(response, List.of("0", "ImspTQPwCqd", "202212", "40"));
    validateRow(response, List.of("", "ImspTQPwCqd", "202208", "2"));
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
    validateRow(response, List.of("qhqAxPSTUXp", "202209", "289"));
    validateRow(response, List.of("qhqAxPSTUXp", "202207", "280"));
    validateRow(response, List.of("qhqAxPSTUXp", "202212", "271"));
    validateRow(response, List.of("qhqAxPSTUXp", "202208", "269"));
    validateRow(response, List.of("qhqAxPSTUXp", "202211", "268"));
    validateRow(response, List.of("qhqAxPSTUXp", "202210", "261"));
    validateRow(response, List.of("lc3eMKXaEfw", "202207", "215"));
    validateRow(response, List.of("lc3eMKXaEfw", "202211", "211"));
    validateRow(response, List.of("lc3eMKXaEfw", "202209", "210"));
    validateRow(response, List.of("lc3eMKXaEfw", "202208", "209"));
    validateRow(response, List.of("lc3eMKXaEfw", "202210", "206"));
    validateRow(response, List.of("lc3eMKXaEfw", "202212", "191"));
    validateRow(response, List.of("kJq2mPyFEHo", "202210", "521"));
    validateRow(response, List.of("kJq2mPyFEHo", "202208", "496"));
    validateRow(response, List.of("kJq2mPyFEHo", "202212", "485"));
    validateRow(response, List.of("kJq2mPyFEHo", "202211", "482"));
    validateRow(response, List.of("kJq2mPyFEHo", "202207", "468"));
    validateRow(response, List.of("kJq2mPyFEHo", "202209", "460"));
    validateRow(response, List.of("jmIPBj66vD6", "202207", "405"));
    validateRow(response, List.of("jmIPBj66vD6", "202211", "400"));
    validateRow(response, List.of("jmIPBj66vD6", "202209", "374"));
    validateRow(response, List.of("jmIPBj66vD6", "202210", "373"));
    validateRow(response, List.of("jmIPBj66vD6", "202212", "357"));
    validateRow(response, List.of("jmIPBj66vD6", "202208", "348"));
    validateRow(response, List.of("jUb8gELQApl", "202208", "308"));
    validateRow(response, List.of("jUb8gELQApl", "202210", "284"));
    validateRow(response, List.of("jUb8gELQApl", "202212", "282"));
    validateRow(response, List.of("jUb8gELQApl", "202207", "282"));
    validateRow(response, List.of("jUb8gELQApl", "202211", "280"));
    validateRow(response, List.of("jUb8gELQApl", "202209", "276"));
    validateRow(response, List.of("fdc6uOvgoji", "202210", "454"));
    validateRow(response, List.of("fdc6uOvgoji", "202207", "400"));
    validateRow(response, List.of("fdc6uOvgoji", "202211", "382"));
    validateRow(response, List.of("fdc6uOvgoji", "202208", "373"));
    validateRow(response, List.of("fdc6uOvgoji", "202212", "367"));
    validateRow(response, List.of("fdc6uOvgoji", "202209", "356"));
    validateRow(response, List.of("eIQbndfxQMb", "202210", "397"));
    validateRow(response, List.of("eIQbndfxQMb", "202207", "387"));
    validateRow(response, List.of("eIQbndfxQMb", "202211", "379"));
    validateRow(response, List.of("eIQbndfxQMb", "202209", "375"));
    validateRow(response, List.of("eIQbndfxQMb", "202212", "334"));
    validateRow(response, List.of("eIQbndfxQMb", "202208", "331"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202208", "287"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202210", "276"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202211", "272"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202207", "264"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202209", "249"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202212", "247"));
    validateRow(response, List.of("at6UHUQatSo", "202211", "435"));
    validateRow(response, List.of("at6UHUQatSo", "202209", "428"));
    validateRow(response, List.of("at6UHUQatSo", "202208", "423"));
    validateRow(response, List.of("at6UHUQatSo", "202207", "413"));
    validateRow(response, List.of("at6UHUQatSo", "202210", "387"));
    validateRow(response, List.of("at6UHUQatSo", "202212", "364"));
    validateRow(response, List.of("Vth0fbpFcsO", "202208", "364"));
    validateRow(response, List.of("Vth0fbpFcsO", "202210", "349"));
    validateRow(response, List.of("Vth0fbpFcsO", "202207", "342"));
    validateRow(response, List.of("Vth0fbpFcsO", "202212", "329"));
    validateRow(response, List.of("Vth0fbpFcsO", "202211", "318"));
    validateRow(response, List.of("Vth0fbpFcsO", "202209", "313"));
    validateRow(response, List.of("TEQlaapDQoK", "202209", "458"));
    validateRow(response, List.of("TEQlaapDQoK", "202207", "455"));
    validateRow(response, List.of("TEQlaapDQoK", "202208", "442"));
    validateRow(response, List.of("TEQlaapDQoK", "202211", "431"));
    validateRow(response, List.of("TEQlaapDQoK", "202210", "413"));
    validateRow(response, List.of("TEQlaapDQoK", "202212", "399"));
    validateRow(response, List.of("PMa2VCrupOd", "202207", "274"));
    validateRow(response, List.of("PMa2VCrupOd", "202208", "255"));
    validateRow(response, List.of("PMa2VCrupOd", "202210", "248"));
    validateRow(response, List.of("PMa2VCrupOd", "202209", "240"));
    validateRow(response, List.of("PMa2VCrupOd", "202212", "235"));
    validateRow(response, List.of("PMa2VCrupOd", "202211", "234"));
    validateRow(response, List.of("O6uvpzGd5pu", "202207", "515"));
    validateRow(response, List.of("O6uvpzGd5pu", "202209", "511"));
    validateRow(response, List.of("O6uvpzGd5pu", "202208", "509"));
    validateRow(response, List.of("O6uvpzGd5pu", "202210", "480"));
    validateRow(response, List.of("O6uvpzGd5pu", "202212", "475"));
    validateRow(response, List.of("O6uvpzGd5pu", "202211", "472"));
  }
}
