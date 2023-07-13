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
public class EventsAggregate4AutoTest extends AnalyticsApiTest {
  private final AnalyticsEventActions actions = new AnalyticsEventActions();

  @Test
  public void queryGenderAndModeOfDischargePlainTableLast4Quarters() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=Zj7UnCAulEk")
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add(
                "dimension=ou:O6uvpzGd5pu;lc3eMKXaEfw;fdc6uOvgoji,pe:LAST_4_QUARTERS,Zj7UnCAulEk.fWIAEtYVEGk,Zj7UnCAulEk.oZg33kd9taw")
            .add("collapseDataDimensions=true")
            .add("relativePeriodDate=2022-01-01");

    // When
    ApiResponse response = actions.aggregate().get("eBAyeGv0exc", JSON, JSON, params);
    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(4)))
        .body("rows", hasSize(equalTo(75)))
        .body("height", equalTo(75))
        .body("width", equalTo(4))
        .body("headerWidth", equalTo(4));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"rBvjJYbMCVx\":{\"code\":\"Male\",\"name\":\"Male\"},\"ou\":{\"name\":\"Organisation unit\"},\"Fhbf4aKpZmZ\":{\"code\":\"MODABSC\",\"name\":\"Absconded\"},\"fWIAEtYVEGk\":{\"name\":\"Mode of Discharge\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"gj2fKKyp8OH\":{\"code\":\"MODDIED\",\"name\":\"Died\"},\"2021Q4\":{\"name\":\"October - December 2021\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"},\"2021Q2\":{\"name\":\"April - June 2021\"},\"2021Q3\":{\"name\":\"July - September 2021\"},\"2021Q1\":{\"name\":\"January - March 2021\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"pe\":{\"name\":\"Period\"},\"LAST_4_QUARTERS\":{\"name\":\"Last 4 quarters\"},\"oZg33kd9taw\":{\"name\":\"Gender\"},\"fShHdgT7XGb\":{\"code\":\"MODTRANS\",\"name\":\"Transferred\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"yeod5tOXpkP\":{\"code\":\"MODDISCH\",\"name\":\"Discharged\"}},\"dimensions\":{\"pe\":[\"2021Q1\",\"2021Q2\",\"2021Q3\",\"2021Q4\"],\"ou\":[\"O6uvpzGd5pu\",\"lc3eMKXaEfw\",\"fdc6uOvgoji\"],\"fWIAEtYVEGk\":[\"yeod5tOXpkP\",\"gj2fKKyp8OH\",\"fShHdgT7XGb\",\"Fhbf4aKpZmZ\"],\"oZg33kd9taw\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dy", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, 0, List.of("Mode of Discharge: [N/A]", "2021Q2", "O6uvpzGd5pu", "13"));
    validateRow(
        response, 1, List.of("Mode of Discharge: Transferred", "2021Q3", "O6uvpzGd5pu", "389"));
    validateRow(
        response, 2, List.of("Mode of Discharge: Transferred", "2021Q4", "O6uvpzGd5pu", "367"));
    validateRow(
        response, 3, List.of("Mode of Discharge: Transferred", "2021Q1", "O6uvpzGd5pu", "336"));
    validateRow(
        response, 4, List.of("Mode of Discharge: Transferred", "2021Q2", "O6uvpzGd5pu", "325"));
    validateRow(
        response, 5, List.of("Mode of Discharge: Transferred", "2021Q1", "fdc6uOvgoji", "305"));
    validateRow(
        response, 6, List.of("Mode of Discharge: Transferred", "2021Q3", "fdc6uOvgoji", "295"));
    validateRow(
        response, 7, List.of("Mode of Discharge: Transferred", "2021Q4", "fdc6uOvgoji", "290"));
    validateRow(
        response, 8, List.of("Mode of Discharge: Transferred", "2021Q2", "fdc6uOvgoji", "281"));
    validateRow(
        response, 9, List.of("Mode of Discharge: Transferred", "2021Q1", "lc3eMKXaEfw", "168"));
    validateRow(
        response, 10, List.of("Mode of Discharge: Transferred", "2021Q3", "lc3eMKXaEfw", "163"));
    validateRow(
        response, 11, List.of("Mode of Discharge: Transferred", "2021Q2", "lc3eMKXaEfw", "160"));
    validateRow(
        response, 12, List.of("Mode of Discharge: Transferred", "2021Q4", "lc3eMKXaEfw", "156"));
    validateRow(
        response, 13, List.of("Mode of Discharge: Discharged", "2021Q4", "O6uvpzGd5pu", "384"));
    validateRow(
        response, 14, List.of("Mode of Discharge: Discharged", "2021Q3", "O6uvpzGd5pu", "383"));
    validateRow(
        response, 15, List.of("Mode of Discharge: Discharged", "2021Q2", "O6uvpzGd5pu", "345"));
    validateRow(
        response, 16, List.of("Mode of Discharge: Discharged", "2021Q1", "O6uvpzGd5pu", "333"));
    validateRow(
        response, 17, List.of("Mode of Discharge: Discharged", "2021Q2", "fdc6uOvgoji", "332"));
    validateRow(
        response, 18, List.of("Mode of Discharge: Discharged", "2021Q1", "fdc6uOvgoji", "298"));
    validateRow(
        response, 19, List.of("Mode of Discharge: Discharged", "2021Q3", "fdc6uOvgoji", "297"));
    validateRow(
        response, 20, List.of("Mode of Discharge: Discharged", "2021Q4", "fdc6uOvgoji", "272"));
    validateRow(
        response, 21, List.of("Mode of Discharge: Discharged", "2021Q2", "lc3eMKXaEfw", "174"));
    validateRow(
        response, 22, List.of("Mode of Discharge: Discharged", "2021Q3", "lc3eMKXaEfw", "170"));
    validateRow(
        response, 23, List.of("Mode of Discharge: Discharged", "2021Q1", "lc3eMKXaEfw", "152"));
    validateRow(
        response, 24, List.of("Mode of Discharge: Discharged", "2021Q4", "lc3eMKXaEfw", "152"));
    validateRow(response, 25, List.of("Mode of Discharge: Died", "2021Q3", "O6uvpzGd5pu", "378"));
    validateRow(response, 26, List.of("Mode of Discharge: Died", "2021Q1", "O6uvpzGd5pu", "375"));
    validateRow(response, 27, List.of("Mode of Discharge: Died", "2021Q2", "O6uvpzGd5pu", "361"));
    validateRow(response, 28, List.of("Mode of Discharge: Died", "2021Q4", "O6uvpzGd5pu", "353"));
    validateRow(response, 29, List.of("Mode of Discharge: Died", "2021Q2", "fdc6uOvgoji", "294"));
    validateRow(response, 30, List.of("Mode of Discharge: Died", "2021Q3", "fdc6uOvgoji", "290"));
    validateRow(response, 31, List.of("Mode of Discharge: Died", "2021Q4", "fdc6uOvgoji", "285"));
    validateRow(response, 32, List.of("Mode of Discharge: Died", "2021Q1", "fdc6uOvgoji", "272"));
    validateRow(response, 33, List.of("Mode of Discharge: Died", "2021Q1", "lc3eMKXaEfw", "178"));
    validateRow(response, 34, List.of("Mode of Discharge: Died", "2021Q4", "lc3eMKXaEfw", "171"));
    validateRow(response, 35, List.of("Mode of Discharge: Died", "2021Q3", "lc3eMKXaEfw", "168"));
    validateRow(response, 36, List.of("Mode of Discharge: Died", "2021Q2", "lc3eMKXaEfw", "149"));
    validateRow(
        response, 37, List.of("Mode of Discharge: Absconded", "2021Q3", "O6uvpzGd5pu", "378"));
    validateRow(
        response, 38, List.of("Mode of Discharge: Absconded", "2021Q2", "O6uvpzGd5pu", "373"));
    validateRow(
        response, 39, List.of("Mode of Discharge: Absconded", "2021Q1", "O6uvpzGd5pu", "349"));
    validateRow(
        response, 40, List.of("Mode of Discharge: Absconded", "2021Q4", "O6uvpzGd5pu", "344"));
    validateRow(
        response, 41, List.of("Mode of Discharge: Absconded", "2021Q2", "fdc6uOvgoji", "313"));
    validateRow(
        response, 42, List.of("Mode of Discharge: Absconded", "2021Q3", "fdc6uOvgoji", "287"));
    validateRow(
        response, 43, List.of("Mode of Discharge: Absconded", "2021Q1", "fdc6uOvgoji", "280"));
    validateRow(
        response, 44, List.of("Mode of Discharge: Absconded", "2021Q4", "fdc6uOvgoji", "259"));
    validateRow(
        response, 45, List.of("Mode of Discharge: Absconded", "2021Q2", "lc3eMKXaEfw", "205"));
    validateRow(
        response, 46, List.of("Mode of Discharge: Absconded", "2021Q3", "lc3eMKXaEfw", "181"));
    validateRow(
        response, 47, List.of("Mode of Discharge: Absconded", "2021Q4", "lc3eMKXaEfw", "171"));
    validateRow(
        response, 48, List.of("Mode of Discharge: Absconded", "2021Q1", "lc3eMKXaEfw", "163"));
    validateRow(response, 49, List.of("Gender: [N/A]", "2021Q2", "O6uvpzGd5pu", "1"));
    validateRow(response, 50, List.of("Gender: [N/A]", "2021Q4", "O6uvpzGd5pu", "1"));
    validateRow(response, 51, List.of("Gender: Male", "2021Q3", "O6uvpzGd5pu", "796"));
    validateRow(response, 52, List.of("Gender: Male", "2021Q4", "O6uvpzGd5pu", "742"));
    validateRow(response, 53, List.of("Gender: Male", "2021Q2", "O6uvpzGd5pu", "692"));
    validateRow(response, 54, List.of("Gender: Male", "2021Q1", "O6uvpzGd5pu", "679"));
    validateRow(response, 55, List.of("Gender: Male", "2021Q2", "fdc6uOvgoji", "618"));
    validateRow(response, 56, List.of("Gender: Male", "2021Q1", "fdc6uOvgoji", "596"));
    validateRow(response, 57, List.of("Gender: Male", "2021Q4", "fdc6uOvgoji", "560"));
    validateRow(response, 58, List.of("Gender: Male", "2021Q3", "fdc6uOvgoji", "560"));
    validateRow(response, 59, List.of("Gender: Male", "2021Q3", "lc3eMKXaEfw", "352"));
    validateRow(response, 60, List.of("Gender: Male", "2021Q2", "lc3eMKXaEfw", "347"));
    validateRow(response, 61, List.of("Gender: Male", "2021Q4", "lc3eMKXaEfw", "324"));
    validateRow(response, 62, List.of("Gender: Male", "2021Q1", "lc3eMKXaEfw", "311"));
    validateRow(response, 63, List.of("Gender: Female", "2021Q3", "O6uvpzGd5pu", "732"));
    validateRow(response, 64, List.of("Gender: Female", "2021Q2", "O6uvpzGd5pu", "724"));
    validateRow(response, 65, List.of("Gender: Female", "2021Q1", "O6uvpzGd5pu", "714"));
    validateRow(response, 66, List.of("Gender: Female", "2021Q4", "O6uvpzGd5pu", "705"));
    validateRow(response, 67, List.of("Gender: Female", "2021Q3", "fdc6uOvgoji", "609"));
    validateRow(response, 68, List.of("Gender: Female", "2021Q2", "fdc6uOvgoji", "602"));
    validateRow(response, 69, List.of("Gender: Female", "2021Q1", "fdc6uOvgoji", "559"));
    validateRow(response, 70, List.of("Gender: Female", "2021Q4", "fdc6uOvgoji", "546"));
    validateRow(response, 71, List.of("Gender: Female", "2021Q1", "lc3eMKXaEfw", "350"));
    validateRow(response, 72, List.of("Gender: Female", "2021Q2", "lc3eMKXaEfw", "341"));
    validateRow(response, 73, List.of("Gender: Female", "2021Q3", "lc3eMKXaEfw", "330"));
    validateRow(response, 74, List.of("Gender: Female", "2021Q4", "lc3eMKXaEfw", "326"));
  }

  @Test
  public void queryHeightAndWeightLast12Months() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=Zj7UnCAulEk")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add(
                "dimension=ou:ImspTQPwCqd,pe:LAST_12_MONTHS,Zj7UnCAulEk.vV9UWAZohSf-OrkEzxZEH4X,Zj7UnCAulEk.GieVkTxp4HH-TBxGTceyzwy")
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.aggregate().get("eBAyeGv0exc", JSON, JSON, params);
    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(5)))
        .body("rows", hasSize(equalTo(254)))
        .body("height", equalTo(254))
        .body("width", equalTo(5))
        .body("headerWidth", equalTo(5));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"lnccUWrmqL0\":{\"name\":\"80 - 90\"},\"eySqrYxteI7\":{\"name\":\"200+\"},\"BHlWGFLIU20\":{\"name\":\"120 - 140\"},\"202208\":{\"name\":\"August 2022\"},\"GWuQsWJDGvN\":{\"name\":\"140 - 160\"},\"GDFw7T4aFGz\":{\"name\":\"60 - 70\"},\"202209\":{\"name\":\"September 2022\"},\"202305\":{\"name\":\"May 2023\"},\"202207\":{\"name\":\"July 2022\"},\"202306\":{\"name\":\"June 2023\"},\"202303\":{\"name\":\"March 2023\"},\"202304\":{\"name\":\"April 2023\"},\"202301\":{\"name\":\"January 2023\"},\"NxQrJ3icPkE\":{\"name\":\"0 - 20\"},\"b9UzeWaSs2u\":{\"name\":\"20 - 40\"},\"xVezsaEXU3k\":{\"name\":\"70 - 80\"},\"202302\":{\"name\":\"February 2023\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"},\"202211\":{\"name\":\"November 2022\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"202212\":{\"name\":\"December 2022\"},\"CivTksSoCt0\":{\"name\":\"100 - 120\"},\"202210\":{\"name\":\"October 2022\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"AD5jueZTZSK\":{\"name\":\"40 - 50\"},\"f3prvzpfniC\":{\"name\":\"100+\"},\"sxFVvKLpE0y\":{\"name\":\"0 - 100\"},\"B1X4JyH4Mdw\":{\"name\":\"180 - 200\"},\"ou\":{\"name\":\"Organisation unit\"},\"vV9UWAZohSf\":{\"name\":\"Weight in kg\"},\"Sjp6IB3gthI\":{\"name\":\"50 - 60\"},\"GieVkTxp4HH\":{\"name\":\"Height in cm\"},\"pe\":{\"name\":\"Period\"},\"wgbW2ZQnlIc\":{\"name\":\"160 - 180\"},\"XKEvGfAkh3R\":{\"name\":\"90 - 100\"}},\"dimensions\":{\"pe\":[\"202207\",\"202208\",\"202209\",\"202210\",\"202211\",\"202212\",\"202301\",\"202302\",\"202303\",\"202304\",\"202305\",\"202306\"],\"Zj7UnCAulEk.vV9UWAZohSf\":[\"NxQrJ3icPkE\",\"b9UzeWaSs2u\",\"AD5jueZTZSK\",\"Sjp6IB3gthI\",\"GDFw7T4aFGz\",\"xVezsaEXU3k\",\"lnccUWrmqL0\",\"XKEvGfAkh3R\",\"f3prvzpfniC\"],\"ou\":[\"ImspTQPwCqd\"],\"Zj7UnCAulEk.GieVkTxp4HH\":[\"sxFVvKLpE0y\",\"CivTksSoCt0\",\"BHlWGFLIU20\",\"GWuQsWJDGvN\",\"wgbW2ZQnlIc\",\"B1X4JyH4Mdw\",\"eySqrYxteI7\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "vV9UWAZohSf", "Weight in kg", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "GieVkTxp4HH", "Height in cm", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 4, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, 0, List.of("Sjp6IB3gthI", "CivTksSoCt0", "ImspTQPwCqd", "202210", "89"));
    validateRow(response, 1, List.of("b9UzeWaSs2u", "BHlWGFLIU20", "ImspTQPwCqd", "202208", "160"));
    validateRow(response, 2, List.of("NxQrJ3icPkE", "wgbW2ZQnlIc", "ImspTQPwCqd", "202212", "27"));
    validateRow(response, 3, List.of("Sjp6IB3gthI", "wgbW2ZQnlIc", "ImspTQPwCqd", "202212", "81"));
    validateRow(response, 4, List.of("NxQrJ3icPkE", "GWuQsWJDGvN", "ImspTQPwCqd", "202208", "38"));
    validateRow(response, 5, List.of("xVezsaEXU3k", "GWuQsWJDGvN", "ImspTQPwCqd", "202208", "82"));
    validateRow(response, 6, List.of("GDFw7T4aFGz", "BHlWGFLIU20", "ImspTQPwCqd", "202211", "84"));
    validateRow(response, 7, List.of("GDFw7T4aFGz", "CivTksSoCt0", "ImspTQPwCqd", "202210", "106"));
    validateRow(response, 8, List.of("Sjp6IB3gthI", "B1X4JyH4Mdw", "ImspTQPwCqd", "202207", "42"));
    validateRow(response, 9, List.of("NxQrJ3icPkE", "sxFVvKLpE0y", "ImspTQPwCqd", "202207", "131"));
    validateRow(response, 10, List.of("AD5jueZTZSK", "B1X4JyH4Mdw", "ImspTQPwCqd", "202207", "43"));
    validateRow(
        response, 11, List.of("AD5jueZTZSK", "CivTksSoCt0", "ImspTQPwCqd", "202209", "105"));
    validateRow(response, 12, List.of("xVezsaEXU3k", "BHlWGFLIU20", "ImspTQPwCqd", "202211", "86"));
    validateRow(response, 13, List.of("AD5jueZTZSK", "CivTksSoCt0", "ImspTQPwCqd", "202212", "72"));
    validateRow(response, 14, List.of("lnccUWrmqL0", "CivTksSoCt0", "ImspTQPwCqd", "202211", "52"));
    validateRow(response, 15, List.of("NxQrJ3icPkE", "wgbW2ZQnlIc", "ImspTQPwCqd", "202210", "41"));
    validateRow(response, 16, List.of("xVezsaEXU3k", "BHlWGFLIU20", "ImspTQPwCqd", "202212", "78"));
    validateRow(response, 17, List.of("NxQrJ3icPkE", "B1X4JyH4Mdw", "ImspTQPwCqd", "202207", "21"));
    validateRow(response, 18, List.of("lnccUWrmqL0", "CivTksSoCt0", "ImspTQPwCqd", "202209", "55"));
    validateRow(response, 19, List.of("xVezsaEXU3k", "wgbW2ZQnlIc", "ImspTQPwCqd", "202212", "90"));
    validateRow(response, 20, List.of("b9UzeWaSs2u", "B1X4JyH4Mdw", "ImspTQPwCqd", "202212", "77"));
    validateRow(response, 21, List.of("lnccUWrmqL0", "GWuQsWJDGvN", "ImspTQPwCqd", "202208", "39"));
    validateRow(
        response, 22, List.of("Sjp6IB3gthI", "sxFVvKLpE0y", "ImspTQPwCqd", "202209", "238"));
    validateRow(response, 23, List.of("AD5jueZTZSK", "B1X4JyH4Mdw", "ImspTQPwCqd", "202208", "47"));
    validateRow(
        response, 24, List.of("Sjp6IB3gthI", "sxFVvKLpE0y", "ImspTQPwCqd", "202210", "258"));
    validateRow(
        response, 25, List.of("Sjp6IB3gthI", "sxFVvKLpE0y", "ImspTQPwCqd", "202211", "243"));
    validateRow(
        response, 26, List.of("xVezsaEXU3k", "wgbW2ZQnlIc", "ImspTQPwCqd", "202208", "109"));
    validateRow(
        response, 27, List.of("GDFw7T4aFGz", "sxFVvKLpE0y", "ImspTQPwCqd", "202210", "279"));
    validateRow(
        response, 28, List.of("b9UzeWaSs2u", "sxFVvKLpE0y", "ImspTQPwCqd", "202212", "501"));
    validateRow(response, 29, List.of("GDFw7T4aFGz", "BHlWGFLIU20", "ImspTQPwCqd", "202210", "87"));
    validateRow(response, 30, List.of("lnccUWrmqL0", "GWuQsWJDGvN", "ImspTQPwCqd", "202210", "36"));
    validateRow(response, 31, List.of("NxQrJ3icPkE", "GWuQsWJDGvN", "ImspTQPwCqd", "202212", "48"));
    validateRow(response, 32, List.of("lnccUWrmqL0", "CivTksSoCt0", "ImspTQPwCqd", "202210", "39"));
    validateRow(response, 33, List.of("Sjp6IB3gthI", "CivTksSoCt0", "ImspTQPwCqd", "202209", "90"));
    validateRow(response, 34, List.of("Sjp6IB3gthI", "GWuQsWJDGvN", "ImspTQPwCqd", "202210", "98"));
    validateRow(response, 35, List.of("GDFw7T4aFGz", "BHlWGFLIU20", "ImspTQPwCqd", "202207", "88"));
    validateRow(response, 36, List.of("NxQrJ3icPkE", "CivTksSoCt0", "ImspTQPwCqd", "202211", "50"));
    validateRow(response, 37, List.of("AD5jueZTZSK", "wgbW2ZQnlIc", "ImspTQPwCqd", "202208", "79"));
    validateRow(response, 38, List.of("xVezsaEXU3k", "wgbW2ZQnlIc", "ImspTQPwCqd", "202207", "83"));
    validateRow(response, 39, List.of("NxQrJ3icPkE", "wgbW2ZQnlIc", "ImspTQPwCqd", "202208", "40"));
    validateRow(response, 40, List.of("Sjp6IB3gthI", "GWuQsWJDGvN", "ImspTQPwCqd", "202211", "87"));
    validateRow(response, 41, List.of("AD5jueZTZSK", "GWuQsWJDGvN", "ImspTQPwCqd", "202209", "91"));
    validateRow(response, 42, List.of("Sjp6IB3gthI", "BHlWGFLIU20", "ImspTQPwCqd", "202207", "79"));
    validateRow(response, 43, List.of("AD5jueZTZSK", "GWuQsWJDGvN", "ImspTQPwCqd", "202211", "83"));
    validateRow(response, 44, List.of("b9UzeWaSs2u", "B1X4JyH4Mdw", "ImspTQPwCqd", "202210", "95"));
    validateRow(
        response, 45, List.of("GDFw7T4aFGz", "GWuQsWJDGvN", "ImspTQPwCqd", "202211", "100"));
    validateRow(response, 46, List.of("lnccUWrmqL0", "wgbW2ZQnlIc", "ImspTQPwCqd", "202209", "37"));
    validateRow(response, 47, List.of("Sjp6IB3gthI", "wgbW2ZQnlIc", "ImspTQPwCqd", "202210", "80"));
    validateRow(response, 48, List.of("lnccUWrmqL0", "B1X4JyH4Mdw", "ImspTQPwCqd", "202211", "18"));
    validateRow(response, 49, List.of("GDFw7T4aFGz", "GWuQsWJDGvN", "ImspTQPwCqd", "202208", "59"));
    validateRow(response, 50, List.of("NxQrJ3icPkE", "BHlWGFLIU20", "ImspTQPwCqd", "202207", "51"));
    validateRow(response, 51, List.of("lnccUWrmqL0", "GWuQsWJDGvN", "ImspTQPwCqd", "202209", "43"));
    validateRow(response, 52, List.of("xVezsaEXU3k", "CivTksSoCt0", "ImspTQPwCqd", "202212", "79"));
    validateRow(
        response, 53, List.of("b9UzeWaSs2u", "sxFVvKLpE0y", "ImspTQPwCqd", "202207", "538"));
    validateRow(response, 54, List.of("GDFw7T4aFGz", "wgbW2ZQnlIc", "ImspTQPwCqd", "202210", "96"));
    validateRow(response, 55, List.of("AD5jueZTZSK", "B1X4JyH4Mdw", "ImspTQPwCqd", "202211", "36"));
    validateRow(response, 56, List.of("xVezsaEXU3k", "CivTksSoCt0", "ImspTQPwCqd", "202207", "97"));
    validateRow(
        response, 57, List.of("xVezsaEXU3k", "GWuQsWJDGvN", "ImspTQPwCqd", "202207", "100"));
    validateRow(response, 58, List.of("lnccUWrmqL0", "B1X4JyH4Mdw", "ImspTQPwCqd", "202212", "19"));
    validateRow(response, 59, List.of("Sjp6IB3gthI", "GWuQsWJDGvN", "ImspTQPwCqd", "202212", "91"));
    validateRow(
        response, 60, List.of("lnccUWrmqL0", "sxFVvKLpE0y", "ImspTQPwCqd", "202209", "127"));
    validateRow(
        response, 61, List.of("AD5jueZTZSK", "sxFVvKLpE0y", "ImspTQPwCqd", "202207", "286"));
    validateRow(response, 62, List.of("lnccUWrmqL0", "B1X4JyH4Mdw", "ImspTQPwCqd", "202207", "20"));
    validateRow(response, 63, List.of("xVezsaEXU3k", "GWuQsWJDGvN", "ImspTQPwCqd", "202212", "69"));
    validateRow(response, 64, List.of("Sjp6IB3gthI", "B1X4JyH4Mdw", "ImspTQPwCqd", "202208", "42"));
    validateRow(
        response, 65, List.of("b9UzeWaSs2u", "CivTksSoCt0", "ImspTQPwCqd", "202212", "155"));
    validateRow(
        response, 66, List.of("GDFw7T4aFGz", "sxFVvKLpE0y", "ImspTQPwCqd", "202211", "255"));
    validateRow(
        response, 67, List.of("b9UzeWaSs2u", "CivTksSoCt0", "ImspTQPwCqd", "202209", "189"));
    validateRow(response, 68, List.of("b9UzeWaSs2u", "B1X4JyH4Mdw", "ImspTQPwCqd", "202208", "80"));
    validateRow(response, 69, List.of("xVezsaEXU3k", "CivTksSoCt0", "ImspTQPwCqd", "202209", "81"));
    validateRow(
        response, 70, List.of("Sjp6IB3gthI", "sxFVvKLpE0y", "ImspTQPwCqd", "202207", "280"));
    validateRow(response, 71, List.of("xVezsaEXU3k", "B1X4JyH4Mdw", "ImspTQPwCqd", "202208", "57"));
    validateRow(response, 72, List.of("NxQrJ3icPkE", "CivTksSoCt0", "ImspTQPwCqd", "202209", "40"));
    validateRow(response, 73, List.of("b9UzeWaSs2u", "B1X4JyH4Mdw", "ImspTQPwCqd", "202207", "86"));
    validateRow(response, 74, List.of("NxQrJ3icPkE", "GWuQsWJDGvN", "ImspTQPwCqd", "202211", "42"));
    validateRow(response, 75, List.of("lnccUWrmqL0", "CivTksSoCt0", "ImspTQPwCqd", "202207", "49"));
    validateRow(
        response, 76, List.of("AD5jueZTZSK", "sxFVvKLpE0y", "ImspTQPwCqd", "202210", "255"));
    validateRow(response, 77, List.of("lnccUWrmqL0", "wgbW2ZQnlIc", "ImspTQPwCqd", "202211", "36"));
    validateRow(response, 78, List.of("lnccUWrmqL0", "wgbW2ZQnlIc", "ImspTQPwCqd", "202210", "38"));
    validateRow(response, 79, List.of("NxQrJ3icPkE", "wgbW2ZQnlIc", "ImspTQPwCqd", "202207", "57"));
    validateRow(response, 80, List.of("AD5jueZTZSK", "BHlWGFLIU20", "ImspTQPwCqd", "202210", "98"));
    validateRow(
        response, 81, List.of("GDFw7T4aFGz", "sxFVvKLpE0y", "ImspTQPwCqd", "202208", "283"));
    validateRow(
        response, 82, List.of("NxQrJ3icPkE", "sxFVvKLpE0y", "ImspTQPwCqd", "202209", "129"));
    validateRow(response, 83, List.of("Sjp6IB3gthI", "wgbW2ZQnlIc", "ImspTQPwCqd", "202209", "82"));
    validateRow(response, 84, List.of("xVezsaEXU3k", "GWuQsWJDGvN", "ImspTQPwCqd", "202211", "91"));
    validateRow(
        response, 85, List.of("b9UzeWaSs2u", "GWuQsWJDGvN", "ImspTQPwCqd", "202210", "156"));
    validateRow(
        response, 86, List.of("b9UzeWaSs2u", "wgbW2ZQnlIc", "ImspTQPwCqd", "202211", "163"));
    validateRow(
        response, 87, List.of("b9UzeWaSs2u", "CivTksSoCt0", "ImspTQPwCqd", "202207", "171"));
    validateRow(response, 88, List.of("AD5jueZTZSK", "CivTksSoCt0", "ImspTQPwCqd", "202211", "80"));
    validateRow(response, 89, List.of("GDFw7T4aFGz", "CivTksSoCt0", "ImspTQPwCqd", "202208", "87"));
    validateRow(
        response, 90, List.of("GDFw7T4aFGz", "sxFVvKLpE0y", "ImspTQPwCqd", "202212", "252"));
    validateRow(
        response, 91, List.of("b9UzeWaSs2u", "wgbW2ZQnlIc", "ImspTQPwCqd", "202207", "165"));
    validateRow(
        response, 92, List.of("lnccUWrmqL0", "sxFVvKLpE0y", "ImspTQPwCqd", "202207", "107"));
    validateRow(response, 93, List.of("NxQrJ3icPkE", "BHlWGFLIU20", "ImspTQPwCqd", "202209", "42"));
    validateRow(response, 94, List.of("GDFw7T4aFGz", "CivTksSoCt0", "ImspTQPwCqd", "202209", "72"));
    validateRow(response, 95, List.of("lnccUWrmqL0", "wgbW2ZQnlIc", "ImspTQPwCqd", "202212", "36"));
    validateRow(
        response, 96, List.of("xVezsaEXU3k", "sxFVvKLpE0y", "ImspTQPwCqd", "202211", "250"));
    validateRow(response, 97, List.of("Sjp6IB3gthI", "BHlWGFLIU20", "ImspTQPwCqd", "202209", "82"));
    validateRow(response, 98, List.of("AD5jueZTZSK", "GWuQsWJDGvN", "ImspTQPwCqd", "202212", "93"));
    validateRow(response, 99, List.of("b9UzeWaSs2u", "B1X4JyH4Mdw", "ImspTQPwCqd", "202211", "98"));
    validateRow(
        response, 100, List.of("NxQrJ3icPkE", "BHlWGFLIU20", "ImspTQPwCqd", "202210", "36"));
    validateRow(
        response, 101, List.of("Sjp6IB3gthI", "CivTksSoCt0", "ImspTQPwCqd", "202207", "100"));
    validateRow(
        response, 102, List.of("NxQrJ3icPkE", "BHlWGFLIU20", "ImspTQPwCqd", "202208", "44"));
    validateRow(
        response, 103, List.of("Sjp6IB3gthI", "wgbW2ZQnlIc", "ImspTQPwCqd", "202208", "99"));
    validateRow(
        response, 104, List.of("lnccUWrmqL0", "BHlWGFLIU20", "ImspTQPwCqd", "202209", "40"));
    validateRow(
        response, 105, List.of("NxQrJ3icPkE", "wgbW2ZQnlIc", "ImspTQPwCqd", "202209", "47"));
    validateRow(
        response, 106, List.of("xVezsaEXU3k", "B1X4JyH4Mdw", "ImspTQPwCqd", "202212", "38"));
    validateRow(
        response, 107, List.of("NxQrJ3icPkE", "B1X4JyH4Mdw", "ImspTQPwCqd", "202212", "25"));
    validateRow(
        response, 108, List.of("Sjp6IB3gthI", "wgbW2ZQnlIc", "ImspTQPwCqd", "202211", "85"));
    validateRow(response, 109, List.of("", "", "ImspTQPwCqd", "202208", "2"));
    validateRow(
        response, 110, List.of("xVezsaEXU3k", "BHlWGFLIU20", "ImspTQPwCqd", "202208", "83"));
    validateRow(
        response, 111, List.of("AD5jueZTZSK", "GWuQsWJDGvN", "ImspTQPwCqd", "202208", "76"));
    validateRow(
        response, 112, List.of("AD5jueZTZSK", "BHlWGFLIU20", "ImspTQPwCqd", "202209", "97"));
    validateRow(
        response, 113, List.of("b9UzeWaSs2u", "CivTksSoCt0", "ImspTQPwCqd", "202208", "168"));
    validateRow(
        response, 114, List.of("b9UzeWaSs2u", "GWuQsWJDGvN", "ImspTQPwCqd", "202212", "156"));
    validateRow(
        response, 115, List.of("lnccUWrmqL0", "CivTksSoCt0", "ImspTQPwCqd", "202212", "44"));
    validateRow(
        response, 116, List.of("Sjp6IB3gthI", "sxFVvKLpE0y", "ImspTQPwCqd", "202208", "268"));
    validateRow(
        response, 117, List.of("NxQrJ3icPkE", "sxFVvKLpE0y", "ImspTQPwCqd", "202210", "142"));
    validateRow(
        response, 118, List.of("xVezsaEXU3k", "sxFVvKLpE0y", "ImspTQPwCqd", "202210", "275"));
    validateRow(
        response, 119, List.of("lnccUWrmqL0", "BHlWGFLIU20", "ImspTQPwCqd", "202210", "47"));
    validateRow(
        response, 120, List.of("Sjp6IB3gthI", "BHlWGFLIU20", "ImspTQPwCqd", "202212", "97"));
    validateRow(
        response, 121, List.of("AD5jueZTZSK", "sxFVvKLpE0y", "ImspTQPwCqd", "202209", "272"));
    validateRow(
        response, 122, List.of("Sjp6IB3gthI", "GWuQsWJDGvN", "ImspTQPwCqd", "202208", "88"));
    validateRow(
        response, 123, List.of("Sjp6IB3gthI", "GWuQsWJDGvN", "ImspTQPwCqd", "202209", "93"));
    validateRow(
        response, 124, List.of("xVezsaEXU3k", "sxFVvKLpE0y", "ImspTQPwCqd", "202207", "266"));
    validateRow(
        response, 125, List.of("NxQrJ3icPkE", "B1X4JyH4Mdw", "ImspTQPwCqd", "202211", "18"));
    validateRow(
        response, 126, List.of("Sjp6IB3gthI", "CivTksSoCt0", "ImspTQPwCqd", "202208", "89"));
    validateRow(
        response, 127, List.of("GDFw7T4aFGz", "GWuQsWJDGvN", "ImspTQPwCqd", "202209", "96"));
    validateRow(
        response, 128, List.of("lnccUWrmqL0", "GWuQsWJDGvN", "ImspTQPwCqd", "202212", "41"));
    validateRow(
        response, 129, List.of("xVezsaEXU3k", "wgbW2ZQnlIc", "ImspTQPwCqd", "202209", "90"));
    validateRow(
        response, 130, List.of("b9UzeWaSs2u", "GWuQsWJDGvN", "ImspTQPwCqd", "202207", "168"));
    validateRow(
        response, 131, List.of("NxQrJ3icPkE", "wgbW2ZQnlIc", "ImspTQPwCqd", "202211", "46"));
    validateRow(
        response, 132, List.of("Sjp6IB3gthI", "BHlWGFLIU20", "ImspTQPwCqd", "202210", "71"));
    validateRow(
        response, 133, List.of("AD5jueZTZSK", "GWuQsWJDGvN", "ImspTQPwCqd", "202207", "85"));
    validateRow(
        response, 134, List.of("AD5jueZTZSK", "sxFVvKLpE0y", "ImspTQPwCqd", "202208", "270"));
    validateRow(
        response, 135, List.of("b9UzeWaSs2u", "BHlWGFLIU20", "ImspTQPwCqd", "202211", "204"));
    validateRow(
        response, 136, List.of("lnccUWrmqL0", "GWuQsWJDGvN", "ImspTQPwCqd", "202207", "47"));
    validateRow(
        response, 137, List.of("AD5jueZTZSK", "B1X4JyH4Mdw", "ImspTQPwCqd", "202212", "47"));
    validateRow(
        response, 138, List.of("AD5jueZTZSK", "CivTksSoCt0", "ImspTQPwCqd", "202207", "92"));
    validateRow(
        response, 139, List.of("AD5jueZTZSK", "sxFVvKLpE0y", "ImspTQPwCqd", "202212", "232"));
    validateRow(
        response, 140, List.of("GDFw7T4aFGz", "sxFVvKLpE0y", "ImspTQPwCqd", "202209", "263"));
    validateRow(
        response, 141, List.of("GDFw7T4aFGz", "B1X4JyH4Mdw", "ImspTQPwCqd", "202211", "46"));
    validateRow(
        response, 142, List.of("Sjp6IB3gthI", "CivTksSoCt0", "ImspTQPwCqd", "202211", "77"));
    validateRow(
        response, 143, List.of("xVezsaEXU3k", "CivTksSoCt0", "ImspTQPwCqd", "202211", "73"));
    validateRow(
        response, 144, List.of("xVezsaEXU3k", "GWuQsWJDGvN", "ImspTQPwCqd", "202210", "90"));
    validateRow(
        response, 145, List.of("NxQrJ3icPkE", "CivTksSoCt0", "ImspTQPwCqd", "202212", "44"));
    validateRow(
        response, 146, List.of("lnccUWrmqL0", "sxFVvKLpE0y", "ImspTQPwCqd", "202212", "121"));
    validateRow(
        response, 147, List.of("lnccUWrmqL0", "B1X4JyH4Mdw", "ImspTQPwCqd", "202208", "32"));
    validateRow(
        response, 148, List.of("Sjp6IB3gthI", "B1X4JyH4Mdw", "ImspTQPwCqd", "202212", "41"));
    validateRow(
        response, 149, List.of("xVezsaEXU3k", "wgbW2ZQnlIc", "ImspTQPwCqd", "202210", "96"));
    validateRow(
        response, 150, List.of("GDFw7T4aFGz", "wgbW2ZQnlIc", "ImspTQPwCqd", "202212", "94"));
    validateRow(
        response, 151, List.of("b9UzeWaSs2u", "sxFVvKLpE0y", "ImspTQPwCqd", "202209", "501"));
    validateRow(
        response, 152, List.of("GDFw7T4aFGz", "B1X4JyH4Mdw", "ImspTQPwCqd", "202207", "56"));
    validateRow(
        response, 153, List.of("b9UzeWaSs2u", "sxFVvKLpE0y", "ImspTQPwCqd", "202208", "559"));
    validateRow(
        response, 154, List.of("xVezsaEXU3k", "B1X4JyH4Mdw", "ImspTQPwCqd", "202207", "42"));
    validateRow(
        response, 155, List.of("AD5jueZTZSK", "sxFVvKLpE0y", "ImspTQPwCqd", "202211", "250"));
    validateRow(
        response, 156, List.of("b9UzeWaSs2u", "wgbW2ZQnlIc", "ImspTQPwCqd", "202212", "164"));
    validateRow(
        response, 157, List.of("b9UzeWaSs2u", "sxFVvKLpE0y", "ImspTQPwCqd", "202210", "506"));
    validateRow(
        response, 158, List.of("lnccUWrmqL0", "sxFVvKLpE0y", "ImspTQPwCqd", "202210", "146"));
    validateRow(
        response, 159, List.of("GDFw7T4aFGz", "B1X4JyH4Mdw", "ImspTQPwCqd", "202210", "48"));
    validateRow(
        response, 160, List.of("xVezsaEXU3k", "B1X4JyH4Mdw", "ImspTQPwCqd", "202210", "54"));
    validateRow(
        response, 161, List.of("GDFw7T4aFGz", "wgbW2ZQnlIc", "ImspTQPwCqd", "202209", "81"));
    validateRow(
        response, 162, List.of("GDFw7T4aFGz", "GWuQsWJDGvN", "ImspTQPwCqd", "202207", "82"));
    validateRow(
        response, 163, List.of("lnccUWrmqL0", "B1X4JyH4Mdw", "ImspTQPwCqd", "202210", "20"));
    validateRow(
        response, 164, List.of("AD5jueZTZSK", "B1X4JyH4Mdw", "ImspTQPwCqd", "202209", "42"));
    validateRow(
        response, 165, List.of("GDFw7T4aFGz", "GWuQsWJDGvN", "ImspTQPwCqd", "202210", "95"));
    validateRow(
        response, 166, List.of("Sjp6IB3gthI", "sxFVvKLpE0y", "ImspTQPwCqd", "202212", "257"));
    validateRow(
        response, 167, List.of("b9UzeWaSs2u", "CivTksSoCt0", "ImspTQPwCqd", "202210", "186"));
    validateRow(
        response, 168, List.of("lnccUWrmqL0", "BHlWGFLIU20", "ImspTQPwCqd", "202211", "51"));
    validateRow(
        response, 169, List.of("lnccUWrmqL0", "CivTksSoCt0", "ImspTQPwCqd", "202208", "38"));
    validateRow(response, 170, List.of("f3prvzpfniC", "BHlWGFLIU20", "ImspTQPwCqd", "202208", "1"));
    validateRow(
        response, 171, List.of("AD5jueZTZSK", "BHlWGFLIU20", "ImspTQPwCqd", "202207", "94"));
    validateRow(
        response, 172, List.of("AD5jueZTZSK", "wgbW2ZQnlIc", "ImspTQPwCqd", "202209", "90"));
    validateRow(
        response, 173, List.of("AD5jueZTZSK", "BHlWGFLIU20", "ImspTQPwCqd", "202208", "75"));
    validateRow(
        response, 174, List.of("GDFw7T4aFGz", "wgbW2ZQnlIc", "ImspTQPwCqd", "202208", "82"));
    validateRow(
        response, 175, List.of("b9UzeWaSs2u", "GWuQsWJDGvN", "ImspTQPwCqd", "202211", "185"));
    validateRow(
        response, 176, List.of("b9UzeWaSs2u", "sxFVvKLpE0y", "ImspTQPwCqd", "202211", "553"));
    validateRow(
        response, 177, List.of("b9UzeWaSs2u", "BHlWGFLIU20", "ImspTQPwCqd", "202210", "170"));
    validateRow(
        response, 178, List.of("b9UzeWaSs2u", "B1X4JyH4Mdw", "ImspTQPwCqd", "202209", "89"));
    validateRow(
        response, 179, List.of("GDFw7T4aFGz", "GWuQsWJDGvN", "ImspTQPwCqd", "202212", "85"));
    validateRow(
        response, 180, List.of("GDFw7T4aFGz", "BHlWGFLIU20", "ImspTQPwCqd", "202208", "75"));
    validateRow(
        response, 181, List.of("NxQrJ3icPkE", "GWuQsWJDGvN", "ImspTQPwCqd", "202210", "43"));
    validateRow(
        response, 182, List.of("AD5jueZTZSK", "wgbW2ZQnlIc", "ImspTQPwCqd", "202212", "78"));
    validateRow(
        response, 183, List.of("xVezsaEXU3k", "GWuQsWJDGvN", "ImspTQPwCqd", "202209", "74"));
    validateRow(
        response, 184, List.of("lnccUWrmqL0", "GWuQsWJDGvN", "ImspTQPwCqd", "202211", "44"));
    validateRow(
        response, 185, List.of("xVezsaEXU3k", "CivTksSoCt0", "ImspTQPwCqd", "202210", "86"));
    validateRow(
        response, 186, List.of("lnccUWrmqL0", "BHlWGFLIU20", "ImspTQPwCqd", "202207", "43"));
    validateRow(
        response, 187, List.of("xVezsaEXU3k", "sxFVvKLpE0y", "ImspTQPwCqd", "202209", "237"));
    validateRow(
        response, 188, List.of("Sjp6IB3gthI", "BHlWGFLIU20", "ImspTQPwCqd", "202211", "85"));
    validateRow(
        response, 189, List.of("GDFw7T4aFGz", "BHlWGFLIU20", "ImspTQPwCqd", "202209", "95"));
    validateRow(
        response, 190, List.of("lnccUWrmqL0", "B1X4JyH4Mdw", "ImspTQPwCqd", "202209", "22"));
    validateRow(
        response, 191, List.of("b9UzeWaSs2u", "wgbW2ZQnlIc", "ImspTQPwCqd", "202208", "167"));
    validateRow(
        response, 192, List.of("NxQrJ3icPkE", "CivTksSoCt0", "ImspTQPwCqd", "202210", "52"));
    validateRow(
        response, 193, List.of("AD5jueZTZSK", "wgbW2ZQnlIc", "ImspTQPwCqd", "202210", "77"));
    validateRow(
        response, 194, List.of("lnccUWrmqL0", "sxFVvKLpE0y", "ImspTQPwCqd", "202211", "128"));
    validateRow(
        response, 195, List.of("lnccUWrmqL0", "BHlWGFLIU20", "ImspTQPwCqd", "202208", "54"));
    validateRow(
        response, 196, List.of("GDFw7T4aFGz", "B1X4JyH4Mdw", "ImspTQPwCqd", "202209", "42"));
    validateRow(
        response, 197, List.of("AD5jueZTZSK", "BHlWGFLIU20", "ImspTQPwCqd", "202211", "89"));
    validateRow(
        response, 198, List.of("Sjp6IB3gthI", "BHlWGFLIU20", "ImspTQPwCqd", "202208", "102"));
    validateRow(
        response, 199, List.of("GDFw7T4aFGz", "wgbW2ZQnlIc", "ImspTQPwCqd", "202207", "95"));
    validateRow(
        response, 200, List.of("b9UzeWaSs2u", "wgbW2ZQnlIc", "ImspTQPwCqd", "202210", "170"));
    validateRow(
        response, 201, List.of("xVezsaEXU3k", "sxFVvKLpE0y", "ImspTQPwCqd", "202212", "244"));
    validateRow(
        response, 202, List.of("b9UzeWaSs2u", "CivTksSoCt0", "ImspTQPwCqd", "202211", "180"));
    validateRow(
        response, 203, List.of("xVezsaEXU3k", "BHlWGFLIU20", "ImspTQPwCqd", "202207", "94"));
    validateRow(
        response, 204, List.of("GDFw7T4aFGz", "CivTksSoCt0", "ImspTQPwCqd", "202212", "82"));
    validateRow(
        response, 205, List.of("AD5jueZTZSK", "CivTksSoCt0", "ImspTQPwCqd", "202208", "78"));
    validateRow(
        response, 206, List.of("xVezsaEXU3k", "B1X4JyH4Mdw", "ImspTQPwCqd", "202209", "52"));
    validateRow(
        response, 207, List.of("GDFw7T4aFGz", "wgbW2ZQnlIc", "ImspTQPwCqd", "202211", "90"));
    validateRow(
        response, 208, List.of("Sjp6IB3gthI", "wgbW2ZQnlIc", "ImspTQPwCqd", "202207", "94"));
    validateRow(
        response, 209, List.of("xVezsaEXU3k", "B1X4JyH4Mdw", "ImspTQPwCqd", "202211", "32"));
    validateRow(
        response, 210, List.of("b9UzeWaSs2u", "GWuQsWJDGvN", "ImspTQPwCqd", "202209", "184"));
    validateRow(
        response, 211, List.of("AD5jueZTZSK", "GWuQsWJDGvN", "ImspTQPwCqd", "202210", "88"));
    validateRow(
        response, 212, List.of("NxQrJ3icPkE", "sxFVvKLpE0y", "ImspTQPwCqd", "202208", "149"));
    validateRow(
        response, 213, List.of("lnccUWrmqL0", "sxFVvKLpE0y", "ImspTQPwCqd", "202208", "114"));
    validateRow(
        response, 214, List.of("xVezsaEXU3k", "CivTksSoCt0", "ImspTQPwCqd", "202208", "97"));
    validateRow(
        response, 215, List.of("xVezsaEXU3k", "sxFVvKLpE0y", "ImspTQPwCqd", "202208", "276"));
    validateRow(
        response, 216, List.of("Sjp6IB3gthI", "B1X4JyH4Mdw", "ImspTQPwCqd", "202211", "42"));
    validateRow(
        response, 217, List.of("xVezsaEXU3k", "BHlWGFLIU20", "ImspTQPwCqd", "202210", "103"));
    validateRow(
        response, 218, List.of("Sjp6IB3gthI", "GWuQsWJDGvN", "ImspTQPwCqd", "202207", "74"));
    validateRow(
        response, 219, List.of("AD5jueZTZSK", "CivTksSoCt0", "ImspTQPwCqd", "202210", "89"));
    validateRow(
        response, 220, List.of("lnccUWrmqL0", "wgbW2ZQnlIc", "ImspTQPwCqd", "202208", "48"));
    validateRow(
        response, 221, List.of("NxQrJ3icPkE", "CivTksSoCt0", "ImspTQPwCqd", "202207", "45"));
    validateRow(
        response, 222, List.of("AD5jueZTZSK", "wgbW2ZQnlIc", "ImspTQPwCqd", "202207", "86"));
    validateRow(
        response, 223, List.of("NxQrJ3icPkE", "B1X4JyH4Mdw", "ImspTQPwCqd", "202209", "28"));
    validateRow(
        response, 224, List.of("xVezsaEXU3k", "BHlWGFLIU20", "ImspTQPwCqd", "202209", "90"));
    validateRow(
        response, 225, List.of("b9UzeWaSs2u", "BHlWGFLIU20", "ImspTQPwCqd", "202207", "168"));
    validateRow(
        response, 226, List.of("AD5jueZTZSK", "B1X4JyH4Mdw", "ImspTQPwCqd", "202210", "41"));
    validateRow(
        response, 227, List.of("NxQrJ3icPkE", "GWuQsWJDGvN", "ImspTQPwCqd", "202207", "56"));
    validateRow(
        response, 228, List.of("b9UzeWaSs2u", "GWuQsWJDGvN", "ImspTQPwCqd", "202208", "171"));
    validateRow(
        response, 229, List.of("NxQrJ3icPkE", "sxFVvKLpE0y", "ImspTQPwCqd", "202212", "142"));
    validateRow(
        response, 230, List.of("NxQrJ3icPkE", "B1X4JyH4Mdw", "ImspTQPwCqd", "202210", "16"));
    validateRow(
        response, 231, List.of("NxQrJ3icPkE", "CivTksSoCt0", "ImspTQPwCqd", "202208", "52"));
    validateRow(
        response, 232, List.of("Sjp6IB3gthI", "CivTksSoCt0", "ImspTQPwCqd", "202212", "81"));
    validateRow(
        response, 233, List.of("NxQrJ3icPkE", "BHlWGFLIU20", "ImspTQPwCqd", "202212", "45"));
    validateRow(
        response, 234, List.of("xVezsaEXU3k", "wgbW2ZQnlIc", "ImspTQPwCqd", "202211", "78"));
    validateRow(
        response, 235, List.of("b9UzeWaSs2u", "BHlWGFLIU20", "ImspTQPwCqd", "202209", "164"));
    validateRow(
        response, 236, List.of("b9UzeWaSs2u", "wgbW2ZQnlIc", "ImspTQPwCqd", "202209", "155"));
    validateRow(
        response, 237, List.of("lnccUWrmqL0", "wgbW2ZQnlIc", "ImspTQPwCqd", "202207", "49"));
    validateRow(
        response, 238, List.of("GDFw7T4aFGz", "B1X4JyH4Mdw", "ImspTQPwCqd", "202208", "36"));
    validateRow(
        response, 239, List.of("Sjp6IB3gthI", "B1X4JyH4Mdw", "ImspTQPwCqd", "202210", "51"));
    validateRow(
        response, 240, List.of("AD5jueZTZSK", "BHlWGFLIU20", "ImspTQPwCqd", "202212", "88"));
    validateRow(
        response, 241, List.of("GDFw7T4aFGz", "sxFVvKLpE0y", "ImspTQPwCqd", "202207", "268"));
    validateRow(
        response, 242, List.of("b9UzeWaSs2u", "BHlWGFLIU20", "ImspTQPwCqd", "202212", "154"));
    validateRow(
        response, 243, List.of("NxQrJ3icPkE", "B1X4JyH4Mdw", "ImspTQPwCqd", "202208", "19"));
    validateRow(
        response, 244, List.of("lnccUWrmqL0", "BHlWGFLIU20", "ImspTQPwCqd", "202212", "39"));
    validateRow(
        response, 245, List.of("NxQrJ3icPkE", "BHlWGFLIU20", "ImspTQPwCqd", "202211", "43"));
    validateRow(
        response, 246, List.of("GDFw7T4aFGz", "BHlWGFLIU20", "ImspTQPwCqd", "202212", "83"));
    validateRow(
        response, 247, List.of("Sjp6IB3gthI", "B1X4JyH4Mdw", "ImspTQPwCqd", "202209", "44"));
    validateRow(
        response, 248, List.of("GDFw7T4aFGz", "CivTksSoCt0", "ImspTQPwCqd", "202211", "92"));
    validateRow(
        response, 249, List.of("GDFw7T4aFGz", "CivTksSoCt0", "ImspTQPwCqd", "202207", "102"));
    validateRow(
        response, 250, List.of("NxQrJ3icPkE", "sxFVvKLpE0y", "ImspTQPwCqd", "202211", "143"));
    validateRow(
        response, 251, List.of("NxQrJ3icPkE", "GWuQsWJDGvN", "ImspTQPwCqd", "202209", "48"));
    validateRow(
        response, 252, List.of("GDFw7T4aFGz", "B1X4JyH4Mdw", "ImspTQPwCqd", "202212", "46"));
    validateRow(
        response, 253, List.of("AD5jueZTZSK", "wgbW2ZQnlIc", "ImspTQPwCqd", "202211", "76"));
  }

  @Test
  public void queryInpatientCasesLast14Days() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=Zj7UnCAulEk")
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add(
                "dimension=ou:ImspTQPwCqd,Zj7UnCAulEk.oZg33kd9taw,Zj7UnCAulEk.fWIAEtYVEGk,pe:LAST_14_DAYS")
            .add("relativePeriodDate=2022-01-01");

    // When
    ApiResponse response = actions.aggregate().get("eBAyeGv0exc", JSON, JSON, params);
    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(5)))
        .body("rows", hasSize(equalTo(96)))
        .body("height", equalTo(96))
        .body("width", equalTo(5))
        .body("headerWidth", equalTo(5));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"LAST_14_DAYS\":{\"name\":\"Last 14 days\"},\"20211218\":{\"name\":\"2021-12-18\"},\"20211219\":{\"name\":\"2021-12-19\"},\"gj2fKKyp8OH\":{\"code\":\"MODDIED\",\"name\":\"Died\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"20211221\":{\"name\":\"2021-12-21\"},\"20211220\":{\"name\":\"2021-12-20\"},\"20211223\":{\"name\":\"2021-12-23\"},\"20211222\":{\"name\":\"2021-12-22\"},\"20211225\":{\"name\":\"2021-12-25\"},\"20211224\":{\"name\":\"2021-12-24\"},\"20211227\":{\"name\":\"2021-12-27\"},\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"20211226\":{\"name\":\"2021-12-26\"},\"20211229\":{\"name\":\"2021-12-29\"},\"rBvjJYbMCVx\":{\"code\":\"Male\",\"name\":\"Male\"},\"20211228\":{\"name\":\"2021-12-28\"},\"ou\":{\"name\":\"Organisation unit\"},\"Fhbf4aKpZmZ\":{\"code\":\"MODABSC\",\"name\":\"Absconded\"},\"fWIAEtYVEGk\":{\"name\":\"Mode of Discharge\"},\"pe\":{\"name\":\"Period\"},\"oZg33kd9taw\":{\"name\":\"Gender\"},\"20211230\":{\"name\":\"2021-12-30\"},\"fShHdgT7XGb\":{\"code\":\"MODTRANS\",\"name\":\"Transferred\"},\"20211231\":{\"name\":\"2021-12-31\"},\"yeod5tOXpkP\":{\"code\":\"MODDISCH\",\"name\":\"Discharged\"}},\"dimensions\":{\"pe\":[\"20211218\",\"20211219\",\"20211220\",\"20211221\",\"20211222\",\"20211223\",\"20211224\",\"20211225\",\"20211226\",\"20211227\",\"20211228\",\"20211229\",\"20211230\",\"20211231\"],\"ou\":[\"ImspTQPwCqd\"],\"oZg33kd9taw\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"fWIAEtYVEGk\":[\"yeod5tOXpkP\",\"gj2fKKyp8OH\",\"fShHdgT7XGb\",\"Fhbf4aKpZmZ\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "oZg33kd9taw", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "fWIAEtYVEGk", "Mode of Discharge", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 4, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, 0, List.of("Male", "MODDIED", "ImspTQPwCqd", "20211218", "30"));
    validateRow(response, 1, List.of("Male", "MODTRANS", "ImspTQPwCqd", "20211221", "27"));
    validateRow(response, 2, List.of("Male", "MODTRANS", "ImspTQPwCqd", "20211226", "26"));
    validateRow(response, 3, List.of("Male", "MODTRANS", "ImspTQPwCqd", "20211229", "25"));
    validateRow(response, 4, List.of("Male", "MODTRANS", "ImspTQPwCqd", "20211220", "25"));
    validateRow(response, 5, List.of("Male", "MODDIED", "ImspTQPwCqd", "20211219", "25"));
    validateRow(response, 6, List.of("Male", "MODTRANS", "ImspTQPwCqd", "20211228", "24"));
    validateRow(response, 7, List.of("Male", "MODDIED", "ImspTQPwCqd", "20211228", "23"));
    validateRow(response, 8, List.of("Male", "MODDIED", "ImspTQPwCqd", "20211220", "22"));
    validateRow(response, 9, List.of("Male", "MODDISCH", "ImspTQPwCqd", "20211218", "22"));
    validateRow(response, 10, List.of("Male", "MODABSC", "ImspTQPwCqd", "20211222", "22"));
    validateRow(response, 11, List.of("Male", "MODABSC", "ImspTQPwCqd", "20211218", "21"));
    validateRow(response, 12, List.of("Male", "MODDISCH", "ImspTQPwCqd", "20211226", "21"));
    validateRow(response, 13, List.of("Male", "MODDISCH", "ImspTQPwCqd", "20211221", "20"));
    validateRow(response, 14, List.of("Male", "MODDIED", "ImspTQPwCqd", "20211222", "19"));
    validateRow(response, 15, List.of("Male", "MODDIED", "ImspTQPwCqd", "20211223", "19"));
    validateRow(response, 16, List.of("Male", "MODABSC", "ImspTQPwCqd", "20211225", "19"));
    validateRow(response, 17, List.of("Male", "MODDISCH", "ImspTQPwCqd", "20211223", "19"));
    validateRow(response, 18, List.of("Male", "MODDISCH", "ImspTQPwCqd", "20211227", "19"));
    validateRow(response, 19, List.of("Male", "MODDISCH", "ImspTQPwCqd", "20211222", "19"));
    validateRow(response, 20, List.of("Male", "MODABSC", "ImspTQPwCqd", "20211219", "19"));
    validateRow(response, 21, List.of("Male", "MODTRANS", "ImspTQPwCqd", "20211218", "19"));
    validateRow(response, 22, List.of("Male", "MODDISCH", "ImspTQPwCqd", "20211228", "18"));
    validateRow(response, 23, List.of("Male", "MODABSC", "ImspTQPwCqd", "20211224", "18"));
    validateRow(response, 24, List.of("Male", "MODABSC", "ImspTQPwCqd", "20211220", "18"));
    validateRow(response, 25, List.of("Male", "MODABSC", "ImspTQPwCqd", "20211227", "18"));
    validateRow(response, 26, List.of("Male", "MODTRANS", "ImspTQPwCqd", "20211227", "18"));
    validateRow(response, 27, List.of("Male", "MODABSC", "ImspTQPwCqd", "20211226", "18"));
    validateRow(response, 28, List.of("Male", "MODTRANS", "ImspTQPwCqd", "20211219", "17"));
    validateRow(response, 29, List.of("Male", "MODDIED", "ImspTQPwCqd", "20211224", "17"));
    validateRow(response, 30, List.of("Male", "MODABSC", "ImspTQPwCqd", "20211223", "17"));
    validateRow(response, 31, List.of("Male", "MODDISCH", "ImspTQPwCqd", "20211219", "17"));
    validateRow(response, 32, List.of("Male", "MODDIED", "ImspTQPwCqd", "20211221", "16"));
    validateRow(response, 33, List.of("Male", "MODDISCH", "ImspTQPwCqd", "20211220", "16"));
    validateRow(response, 34, List.of("Male", "MODDIED", "ImspTQPwCqd", "20211226", "16"));
    validateRow(response, 35, List.of("Male", "MODABSC", "ImspTQPwCqd", "20211229", "15"));
    validateRow(response, 36, List.of("Male", "MODDISCH", "ImspTQPwCqd", "20211225", "15"));
    validateRow(response, 37, List.of("Male", "MODDIED", "ImspTQPwCqd", "20211227", "15"));
    validateRow(response, 38, List.of("Male", "MODABSC", "ImspTQPwCqd", "20211221", "15"));
    validateRow(response, 39, List.of("Male", "MODDIED", "ImspTQPwCqd", "20211229", "15"));
    validateRow(response, 40, List.of("Male", "MODDISCH", "ImspTQPwCqd", "20211224", "14"));
    validateRow(response, 41, List.of("Male", "MODDIED", "ImspTQPwCqd", "20211225", "14"));
    validateRow(response, 42, List.of("Male", "MODTRANS", "ImspTQPwCqd", "20211222", "13"));
    validateRow(response, 43, List.of("Male", "MODABSC", "ImspTQPwCqd", "20211228", "13"));
    validateRow(response, 44, List.of("Male", "MODDISCH", "ImspTQPwCqd", "20211229", "12"));
    validateRow(response, 45, List.of("Male", "MODTRANS", "ImspTQPwCqd", "20211223", "12"));
    validateRow(response, 46, List.of("Male", "MODTRANS", "ImspTQPwCqd", "20211224", "11"));
    validateRow(response, 47, List.of("Male", "MODTRANS", "ImspTQPwCqd", "20211225", "10"));
    validateRow(response, 48, List.of("Female", "MODDISCH", "ImspTQPwCqd", "20211219", "29"));
    validateRow(response, 49, List.of("Female", "MODTRANS", "ImspTQPwCqd", "20211224", "26"));
    validateRow(response, 50, List.of("Female", "MODABSC", "ImspTQPwCqd", "20211220", "26"));
    validateRow(response, 51, List.of("Female", "MODDISCH", "ImspTQPwCqd", "20211221", "26"));
    validateRow(response, 52, List.of("Female", "MODTRANS", "ImspTQPwCqd", "20211220", "25"));
    validateRow(response, 53, List.of("Female", "MODDISCH", "ImspTQPwCqd", "20211225", "25"));
    validateRow(response, 54, List.of("Female", "MODDIED", "ImspTQPwCqd", "20211221", "24"));
    validateRow(response, 55, List.of("Female", "MODDIED", "ImspTQPwCqd", "20211219", "24"));
    validateRow(response, 56, List.of("Female", "MODABSC", "ImspTQPwCqd", "20211219", "23"));
    validateRow(response, 57, List.of("Female", "MODDISCH", "ImspTQPwCqd", "20211224", "22"));
    validateRow(response, 58, List.of("Female", "MODDISCH", "ImspTQPwCqd", "20211222", "22"));
    validateRow(response, 59, List.of("Female", "MODABSC", "ImspTQPwCqd", "20211218", "22"));
    validateRow(response, 60, List.of("Female", "MODTRANS", "ImspTQPwCqd", "20211219", "22"));
    validateRow(response, 61, List.of("Female", "MODDIED", "ImspTQPwCqd", "20211229", "22"));
    validateRow(response, 62, List.of("Female", "MODABSC", "ImspTQPwCqd", "20211222", "22"));
    validateRow(response, 63, List.of("Female", "MODABSC", "ImspTQPwCqd", "20211226", "21"));
    validateRow(response, 64, List.of("Female", "MODTRANS", "ImspTQPwCqd", "20211227", "21"));
    validateRow(response, 65, List.of("Female", "MODDIED", "ImspTQPwCqd", "20211218", "21"));
    validateRow(response, 66, List.of("Female", "MODTRANS", "ImspTQPwCqd", "20211228", "20"));
    validateRow(response, 67, List.of("Female", "MODTRANS", "ImspTQPwCqd", "20211229", "20"));
    validateRow(response, 68, List.of("Female", "MODDIED", "ImspTQPwCqd", "20211227", "20"));
    validateRow(response, 69, List.of("Female", "MODDIED", "ImspTQPwCqd", "20211224", "20"));
    validateRow(response, 70, List.of("Female", "MODDIED", "ImspTQPwCqd", "20211226", "19"));
    validateRow(response, 71, List.of("Female", "MODDISCH", "ImspTQPwCqd", "20211220", "19"));
    validateRow(response, 72, List.of("Female", "MODDISCH", "ImspTQPwCqd", "20211228", "19"));
    validateRow(response, 73, List.of("Female", "MODDISCH", "ImspTQPwCqd", "20211227", "19"));
    validateRow(response, 74, List.of("Female", "MODDIED", "ImspTQPwCqd", "20211222", "19"));
    validateRow(response, 75, List.of("Female", "MODTRANS", "ImspTQPwCqd", "20211225", "18"));
    validateRow(response, 76, List.of("Female", "MODABSC", "ImspTQPwCqd", "20211223", "18"));
    validateRow(response, 77, List.of("Female", "MODABSC", "ImspTQPwCqd", "20211224", "18"));
    validateRow(response, 78, List.of("Female", "MODDISCH", "ImspTQPwCqd", "20211218", "18"));
    validateRow(response, 79, List.of("Female", "MODDISCH", "ImspTQPwCqd", "20211229", "18"));
    validateRow(response, 80, List.of("Female", "MODTRANS", "ImspTQPwCqd", "20211221", "17"));
    validateRow(response, 81, List.of("Female", "MODTRANS", "ImspTQPwCqd", "20211218", "17"));
    validateRow(response, 82, List.of("Female", "MODTRANS", "ImspTQPwCqd", "20211222", "17"));
    validateRow(response, 83, List.of("Female", "MODDIED", "ImspTQPwCqd", "20211223", "17"));
    validateRow(response, 84, List.of("Female", "MODDISCH", "ImspTQPwCqd", "20211226", "17"));
    validateRow(response, 85, List.of("Female", "MODABSC", "ImspTQPwCqd", "20211221", "17"));
    validateRow(response, 86, List.of("Female", "MODABSC", "ImspTQPwCqd", "20211229", "17"));
    validateRow(response, 87, List.of("Female", "MODDIED", "ImspTQPwCqd", "20211225", "16"));
    validateRow(response, 88, List.of("Female", "MODTRANS", "ImspTQPwCqd", "20211223", "16"));
    validateRow(response, 89, List.of("Female", "MODABSC", "ImspTQPwCqd", "20211227", "16"));
    validateRow(response, 90, List.of("Female", "MODABSC", "ImspTQPwCqd", "20211228", "15"));
    validateRow(response, 91, List.of("Female", "MODABSC", "ImspTQPwCqd", "20211225", "15"));
    validateRow(response, 92, List.of("Female", "MODDIED", "ImspTQPwCqd", "20211228", "14"));
    validateRow(response, 93, List.of("Female", "MODDISCH", "ImspTQPwCqd", "20211223", "14"));
    validateRow(response, 94, List.of("Female", "MODDIED", "ImspTQPwCqd", "20211220", "14"));
    validateRow(response, 95, List.of("Female", "MODTRANS", "ImspTQPwCqd", "20211226", "14"));
  }

  @Test
  public void queryModeOfDischargeByDistrictsThisYear() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=pe:THIS_YEAR")
            .add("stage=Zj7UnCAulEk")
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add(
                "dimension=Zj7UnCAulEk.fWIAEtYVEGk,ou:jUb8gELQApl;eIQbndfxQMb;TEQlaapDQoK;Vth0fbpFcsO;PMa2VCrupOd;bL4ooGhyHRQ;O6uvpzGd5pu;kJq2mPyFEHo;fdc6uOvgoji;at6UHUQatSo;lc3eMKXaEfw;jmIPBj66vD6;qhqAxPSTUXp")
            .add("relativePeriodDate=2021-01-01");

    // When
    ApiResponse response = actions.aggregate().get("eBAyeGv0exc", JSON, JSON, params);
    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(53)))
        .body("height", equalTo(53))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"ou\":{\"name\":\"Organisation unit\"},\"Fhbf4aKpZmZ\":{\"code\":\"MODABSC\",\"name\":\"Absconded\"},\"fWIAEtYVEGk\":{\"name\":\"Mode of Discharge\"},\"THIS_YEAR\":{\"name\":\"This year\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"2021\":{\"name\":\"2021\"},\"gj2fKKyp8OH\":{\"code\":\"MODDIED\",\"name\":\"Died\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"pe\":{\"name\":\"Period\"},\"fShHdgT7XGb\":{\"code\":\"MODTRANS\",\"name\":\"Transferred\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"yeod5tOXpkP\":{\"code\":\"MODDISCH\",\"name\":\"Discharged\"}},\"dimensions\":{\"pe\":[\"2021\"],\"ou\":[\"jUb8gELQApl\",\"eIQbndfxQMb\",\"TEQlaapDQoK\",\"Vth0fbpFcsO\",\"PMa2VCrupOd\",\"bL4ooGhyHRQ\",\"O6uvpzGd5pu\",\"kJq2mPyFEHo\",\"fdc6uOvgoji\",\"at6UHUQatSo\",\"lc3eMKXaEfw\",\"jmIPBj66vD6\",\"qhqAxPSTUXp\"],\"fWIAEtYVEGk\":[\"yeod5tOXpkP\",\"gj2fKKyp8OH\",\"fShHdgT7XGb\",\"Fhbf4aKpZmZ\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "fWIAEtYVEGk", "Mode of Discharge", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, 0, List.of("MODTRANS", "O6uvpzGd5pu", "1417"));
    validateRow(response, 1, List.of("MODTRANS", "kJq2mPyFEHo", "1361"));
    validateRow(response, 2, List.of("MODTRANS", "TEQlaapDQoK", "1332"));
    validateRow(response, 3, List.of("MODTRANS", "at6UHUQatSo", "1207"));
    validateRow(response, 4, List.of("MODTRANS", "fdc6uOvgoji", "1171"));
    validateRow(response, 5, List.of("MODTRANS", "eIQbndfxQMb", "1082"));
    validateRow(response, 6, List.of("MODTRANS", "jmIPBj66vD6", "1041"));
    validateRow(response, 7, List.of("MODTRANS", "Vth0fbpFcsO", "946"));
    validateRow(response, 8, List.of("MODTRANS", "jUb8gELQApl", "871"));
    validateRow(response, 9, List.of("MODTRANS", "qhqAxPSTUXp", "781"));
    validateRow(response, 10, List.of("MODTRANS", "bL4ooGhyHRQ", "737"));
    validateRow(response, 11, List.of("MODTRANS", "PMa2VCrupOd", "713"));
    validateRow(response, 12, List.of("MODTRANS", "lc3eMKXaEfw", "647"));
    validateRow(response, 13, List.of("MODDISCH", "O6uvpzGd5pu", "1445"));
    validateRow(response, 14, List.of("MODDISCH", "kJq2mPyFEHo", "1368"));
    validateRow(response, 15, List.of("MODDISCH", "TEQlaapDQoK", "1318"));
    validateRow(response, 16, List.of("MODDISCH", "fdc6uOvgoji", "1199"));
    validateRow(response, 17, List.of("MODDISCH", "at6UHUQatSo", "1142"));
    validateRow(response, 18, List.of("MODDISCH", "eIQbndfxQMb", "1114"));
    validateRow(response, 19, List.of("MODDISCH", "jmIPBj66vD6", "1067"));
    validateRow(response, 20, List.of("MODDISCH", "Vth0fbpFcsO", "1007"));
    validateRow(response, 21, List.of("MODDISCH", "jUb8gELQApl", "926"));
    validateRow(response, 22, List.of("MODDISCH", "qhqAxPSTUXp", "790"));
    validateRow(response, 23, List.of("MODDISCH", "bL4ooGhyHRQ", "749"));
    validateRow(response, 24, List.of("MODDISCH", "PMa2VCrupOd", "654"));
    validateRow(response, 25, List.of("MODDISCH", "lc3eMKXaEfw", "648"));
    validateRow(response, 26, List.of("MODDIED", "O6uvpzGd5pu", "1467"));
    validateRow(response, 27, List.of("MODDIED", "TEQlaapDQoK", "1390"));
    validateRow(response, 28, List.of("MODDIED", "kJq2mPyFEHo", "1368"));
    validateRow(response, 29, List.of("MODDIED", "at6UHUQatSo", "1178"));
    validateRow(response, 30, List.of("MODDIED", "fdc6uOvgoji", "1141"));
    validateRow(response, 31, List.of("MODDIED", "eIQbndfxQMb", "1045"));
    validateRow(response, 32, List.of("MODDIED", "jmIPBj66vD6", "1019"));
    validateRow(response, 33, List.of("MODDIED", "Vth0fbpFcsO", "972"));
    validateRow(response, 34, List.of("MODDIED", "jUb8gELQApl", "889"));
    validateRow(response, 35, List.of("MODDIED", "qhqAxPSTUXp", "808"));
    validateRow(response, 36, List.of("MODDIED", "bL4ooGhyHRQ", "762"));
    validateRow(response, 37, List.of("MODDIED", "PMa2VCrupOd", "707"));
    validateRow(response, 38, List.of("MODDIED", "lc3eMKXaEfw", "666"));
    validateRow(response, 39, List.of("MODABSC", "O6uvpzGd5pu", "1444"));
    validateRow(response, 40, List.of("MODABSC", "kJq2mPyFEHo", "1428"));
    validateRow(response, 41, List.of("MODABSC", "TEQlaapDQoK", "1329"));
    validateRow(response, 42, List.of("MODABSC", "at6UHUQatSo", "1263"));
    validateRow(response, 43, List.of("MODABSC", "fdc6uOvgoji", "1139"));
    validateRow(response, 44, List.of("MODABSC", "jmIPBj66vD6", "1111"));
    validateRow(response, 45, List.of("MODABSC", "eIQbndfxQMb", "990"));
    validateRow(response, 46, List.of("MODABSC", "Vth0fbpFcsO", "984"));
    validateRow(response, 47, List.of("MODABSC", "jUb8gELQApl", "921"));
    validateRow(response, 48, List.of("MODABSC", "qhqAxPSTUXp", "791"));
    validateRow(response, 49, List.of("MODABSC", "bL4ooGhyHRQ", "757"));
    validateRow(response, 50, List.of("MODABSC", "PMa2VCrupOd", "733"));
    validateRow(response, 51, List.of("MODABSC", "lc3eMKXaEfw", "720"));
    validateRow(response, 52, List.of("", "O6uvpzGd5pu", "13"));
  }
}
