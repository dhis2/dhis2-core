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
package org.hisp.dhis.analytics.enrollment.aggregate;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.actions.analytics.AnalyticsEnrollmentsActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/enrollments/aggregate" endpoint. */
public class EnrollmentsAggregate3AutoTest extends AnalyticsApiTest {
  private final AnalyticsEnrollmentsActions actions = new AnalyticsEnrollmentsActions();

  @Test
  public void queryAggregatedenrollmentsbirthgenderthisyearlast12monthslevel2twice()
      throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=A03MvHHogjR")
            .add("displayProperty=SHORTNAME")
            .add("totalPages=false")
            .add("outputType=ENROLLMENT")
            .add(
                "dimension=ou:kJq2mPyFEHo;O6uvpzGd5pu,pe:THIS_YEAR;LAST_12_MONTHS,A03MvHHogjR.cejWyOfXge6")
            .add("relativePeriodDate=2023-08-01");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(4)))
        .body("rows", hasSize(equalTo(59)))
        .body("height", equalTo(59))
        .body("width", equalTo(4))
        .body("headerWidth", equalTo(4));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"rBvjJYbMCVx\":{\"code\":\"Male\",\"name\":\"Male\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"202208\":{\"name\":\"August 2022\"},\"202307\":{\"name\":\"July 2023\"},\"2023\":{\"name\":\"2023\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"202209\":{\"name\":\"September 2022\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"202305\":{\"name\":\"May 2023\"},\"202306\":{\"name\":\"June 2023\"},\"202303\":{\"name\":\"March 2023\"},\"202304\":{\"name\":\"April 2023\"},\"202301\":{\"name\":\"January 2023\"},\"202302\":{\"name\":\"February 2023\"},\"cejWyOfXge6\":{\"name\":\"Gender\"},\"202211\":{\"name\":\"November 2022\"},\"202212\":{\"name\":\"December 2022\"},\"202210\":{\"name\":\"October 2022\"},\"pe\":{},\"A03MvHHogjR\":{\"name\":\"Birth\"}},\"dimensions\":{\"pe\":[\"202208\",\"202209\",\"202210\",\"202211\",\"202212\",\"202301\",\"202302\",\"202303\",\"202304\",\"202305\",\"202306\",\"202307\",\"2023\"],\"ou\":[\"kJq2mPyFEHo\",\"O6uvpzGd5pu\"],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 1, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 3, "A03MvHHogjR.cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true);

    // Assert rows.
    validateRow(response, List.of("53", "O6uvpzGd5pu", "202208", "Female"));
    validateRow(response, List.of("51", "O6uvpzGd5pu", "202208", "Male"));
    validateRow(response, List.of("1", "O6uvpzGd5pu", "202208", ""));
    validateRow(response, List.of("48", "O6uvpzGd5pu", "202209", "Female"));
    validateRow(response, List.of("53", "O6uvpzGd5pu", "202209", "Male"));
    validateRow(response, List.of("1", "O6uvpzGd5pu", "202209", ""));
    validateRow(response, List.of("55", "O6uvpzGd5pu", "202210", "Female"));
    validateRow(response, List.of("57", "O6uvpzGd5pu", "202210", "Male"));
    validateRow(response, List.of("63", "O6uvpzGd5pu", "202211", "Female"));
    validateRow(response, List.of("63", "O6uvpzGd5pu", "202211", "Male"));
    validateRow(response, List.of("43", "O6uvpzGd5pu", "202212", "Female"));
    validateRow(response, List.of("41", "O6uvpzGd5pu", "202212", "Male"));
    validateRow(response, List.of("40", "O6uvpzGd5pu", "202301", "Female"));
    validateRow(response, List.of("45", "O6uvpzGd5pu", "202301", "Male"));
    validateRow(response, List.of("34", "O6uvpzGd5pu", "202302", "Female"));
    validateRow(response, List.of("32", "O6uvpzGd5pu", "202302", "Male"));
    validateRow(response, List.of("31", "O6uvpzGd5pu", "202303", "Female"));
    validateRow(response, List.of("46", "O6uvpzGd5pu", "202303", "Male"));
    validateRow(response, List.of("38", "O6uvpzGd5pu", "202304", "Female"));
    validateRow(response, List.of("35", "O6uvpzGd5pu", "202304", "Male"));
    validateRow(response, List.of("37", "O6uvpzGd5pu", "202305", "Female"));
    validateRow(response, List.of("29", "O6uvpzGd5pu", "202305", "Male"));
    validateRow(response, List.of("33", "O6uvpzGd5pu", "202306", "Female"));
    validateRow(response, List.of("39", "O6uvpzGd5pu", "202306", "Male"));
    validateRow(response, List.of("35", "O6uvpzGd5pu", "202307", "Female"));
    validateRow(response, List.of("37", "O6uvpzGd5pu", "202307", "Male"));
    validateRow(response, List.of("37", "O6uvpzGd5pu", "202308", "Female"));
    validateRow(response, List.of("34", "O6uvpzGd5pu", "202308", "Male"));
    validateRow(response, List.of("26", "O6uvpzGd5pu", "202309", "Female"));
    validateRow(response, List.of("36", "O6uvpzGd5pu", "202309", "Male"));
    validateRow(response, List.of("31", "O6uvpzGd5pu", "202310", "Female"));
    validateRow(response, List.of("32", "O6uvpzGd5pu", "202310", "Male"));
    validateRow(response, List.of("33", "O6uvpzGd5pu", "202311", "Female"));
    validateRow(response, List.of("37", "O6uvpzGd5pu", "202311", "Male"));
    validateRow(response, List.of("46", "O6uvpzGd5pu", "202312", "Female"));
    validateRow(response, List.of("39", "O6uvpzGd5pu", "202312", "Male"));
    validateRow(response, List.of("60", "kJq2mPyFEHo", "202208", "Female"));
    validateRow(response, List.of("59", "kJq2mPyFEHo", "202208", "Male"));
    validateRow(response, List.of("45", "kJq2mPyFEHo", "202209", "Female"));
    validateRow(response, List.of("31", "kJq2mPyFEHo", "202209", "Male"));
    validateRow(response, List.of("57", "kJq2mPyFEHo", "202210", "Female"));
    validateRow(response, List.of("34", "kJq2mPyFEHo", "202210", "Male"));
    validateRow(response, List.of("40", "kJq2mPyFEHo", "202211", "Female"));
    validateRow(response, List.of("42", "kJq2mPyFEHo", "202211", "Male"));
    validateRow(response, List.of("46", "kJq2mPyFEHo", "202212", "Female"));
    validateRow(response, List.of("46", "kJq2mPyFEHo", "202212", "Male"));
    validateRow(response, List.of("30", "kJq2mPyFEHo", "202301", "Female"));
    validateRow(response, List.of("33", "kJq2mPyFEHo", "202301", "Male"));
    validateRow(response, List.of("37", "kJq2mPyFEHo", "202302", "Female"));
    validateRow(response, List.of("33", "kJq2mPyFEHo", "202302", "Male"));
    validateRow(response, List.of("421", "O6uvpzGd5pu", "2023", "Female"));
    validateRow(response, List.of("452", "kJq2mPyFEHo", "2023", "Male"));
    validateRow(response, List.of("248", "kJq2mPyFEHo", "2022", "Female"));
    validateRow(response, List.of("441", "O6uvpzGd5pu", "2023", "Male"));
    validateRow(response, List.of("421", "kJq2mPyFEHo", "2023", "Female"));
    validateRow(response, List.of("265", "O6uvpzGd5pu", "2022", "Male"));
    validateRow(response, List.of("262", "O6uvpzGd5pu", "2022", "Female"));
    validateRow(response, List.of("212", "kJq2mPyFEHo", "2022", "Male"));
    validateRow(response, List.of("2", "O6uvpzGd5pu", "2022", ""));
  }

  @Test
  public void queryAggregatedenrollmentsbirthbcgdosegenderthismonthlastmonthlevel4org()
      throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=A03MvHHogjR")
            .add("displayProperty=SHORTNAME")
            .add("totalPages=false")
            .add("outputType=ENROLLMENT")
            .add(
                "dimension=ou:DiszpKrYNg8,pe:THIS_MONTH;LAST_MONTH,A03MvHHogjR.bx6fsa0t90x,A03MvHHogjR.cejWyOfXge6")
            .add("relativePeriodDate=2023-08-01");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(5)))
        .body("rows", hasSize(equalTo(0)))
        .body("height", equalTo(0))
        .body("width", equalTo(0))
        .body("headerWidth", equalTo(5));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"rBvjJYbMCVx\":{\"code\":\"Male\",\"name\":\"Male\"},\"bx6fsa0t90x\":{\"name\":\"MCH BCG dose\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"202307\":{\"name\":\"July 2023\"},\"202308\":{\"name\":\"August 2023\"},\"cejWyOfXge6\":{\"name\":\"Gender\"}},\"dimensions\":{\"pe\":[\"202307\",\"202308\"],\"ou\":[\"DiszpKrYNg8\"],\"A03MvHHogjR.bx6fsa0t90x\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 1, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 3, "A03MvHHogjR.cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        4,
        "A03MvHHogjR.bx6fsa0t90x",
        "BCG dose",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);

    // Assert rows.
  }

  @Test
  public void queryAggregatedenrollmentsbirthbcgdosegenderthisyearlastyearlevel4org()
      throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=A03MvHHogjR")
            .add("displayProperty=SHORTNAME")
            .add("totalPages=false")
            .add("outputType=ENROLLMENT")
            .add(
                "dimension=ou:DiszpKrYNg8,pe:THIS_YEAR;LAST_YEAR,A03MvHHogjR.bx6fsa0t90x,A03MvHHogjR.cejWyOfXge6");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(5)))
        .body("rows", hasSize(equalTo(13)))
        .body("height", equalTo(13))
        .body("width", equalTo(5))
        .body("headerWidth", equalTo(5));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"rBvjJYbMCVx\":{\"code\":\"Male\",\"name\":\"Male\"},\"bx6fsa0t90x\":{\"name\":\"MCH BCG dose\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"2023\":{\"name\":\"2023\"},\"2022\":{\"name\":\"2022\"},\"cejWyOfXge6\":{\"name\":\"Gender\"}},\"dimensions\":{\"pe\":[\"2022\",\"2023\"],\"ou\":[\"DiszpKrYNg8\"],\"A03MvHHogjR.bx6fsa0t90x\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 1, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 3, "A03MvHHogjR.cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        4,
        "A03MvHHogjR.bx6fsa0t90x",
        "BCG dose",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);

    // Assert rows.
    validateRow(response, List.of("2", "DiszpKrYNg8", "2022", "Female", "0"));
    validateRow(response, List.of("3", "DiszpKrYNg8", "2022", "Female", "1"));
    validateRow(response, List.of("9", "DiszpKrYNg8", "2022", "Female", ""));
    validateRow(response, List.of("4", "DiszpKrYNg8", "2022", "Male", "0"));
    validateRow(response, List.of("1", "DiszpKrYNg8", "2022", "Male", "1"));
    validateRow(response, List.of("5", "DiszpKrYNg8", "2022", "Male", ""));
    validateRow(response, List.of("2", "DiszpKrYNg8", "2022", "", ""));
    validateRow(response, List.of("1", "DiszpKrYNg8", "2023", "Female", "0"));
    validateRow(response, List.of("3", "DiszpKrYNg8", "2023", "Female", "1"));
    validateRow(response, List.of("1", "DiszpKrYNg8", "2023", "Female", ""));
    validateRow(response, List.of("2", "DiszpKrYNg8", "2023", "Male", "0"));
    validateRow(response, List.of("3", "DiszpKrYNg8", "2023", "Male", "1"));
    validateRow(response, List.of("1", "DiszpKrYNg8", "2023", "Male", ""));
  }

  @Test
  public void queryAggregatedenrollmentsbirthgenderthisyearlevel4orgwithheaders()
      throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("headers=pe,A03MvHHogjR.cejWyOfXge6,value")
            .add("stage=A03MvHHogjR")
            .add("displayProperty=SHORTNAME")
            .add("totalPages=false")
            .add("outputType=ENROLLMENT")
            .add("dimension=ou:DiszpKrYNg8,pe:THIS_YEAR,A03MvHHogjR.cejWyOfXge6");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(2)))
        .body("height", equalTo(2))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"rBvjJYbMCVx\":{\"code\":\"Male\",\"name\":\"Male\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"2023\":{\"name\":\"2023\"},\"cejWyOfXge6\":{\"name\":\"Gender\"}},\"dimensions\":{\"pe\":[\"2023\"],\"ou\":[\"DiszpKrYNg8\"],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "A03MvHHogjR.cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("2023", "Female", "5"));
    validateRow(response, List.of("2023", "Male", "6"));
  }
}
