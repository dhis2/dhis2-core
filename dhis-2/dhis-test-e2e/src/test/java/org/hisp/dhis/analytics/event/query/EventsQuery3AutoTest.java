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
package org.hisp.dhis.analytics.event.query;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsEventActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class EventsQuery3AutoTest extends AnalyticsApiTest {
  private final AnalyticsEventActions actions = new AnalyticsEventActions();

  @Test
  public void multiPeriodMultiProgramStatusPagingFalse() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("lastUpdated=LAST_12_MONTHS,LAST_5_YEARS,LAST_10_YEARS")
            .add(
                "headers=ouname,A03MvHHogjR.UXz7xuGCEhU,enrollmentdate,scheduleddate,incidentdate,programstatus,eventdate,eventstatus,p2Zxg0wcPQ3,lastupdated,cejWyOfXge6")
            .add("stage=A03MvHHogjR")
            .add("outputIdScheme=CODE")
            .add("eventStatus=SCHEDULE")
            .add("enrollmentDate=THIS_MONTH")
            .add("outputType=EVENT")
            .add("paging=false")
            .add("dimension=ou:USER_ORGUNIT,A03MvHHogjR.UXz7xuGCEhU,p2Zxg0wcPQ3,cejWyOfXge6")
            .add("programStatus=ACTIVE,COMPLETED")
            .add("eventDate=LAST_MONTH,LAST_12_MONTHS")
            .add("relativePeriodDate=2022-07-01");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(11)))
        .body("rows", hasSize(equalTo(1)))
        .body("height", equalTo(1))
        .body("width", equalTo(11))
        .body("headerWidth", equalTo(11));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"name\":\"Organisation unit\"},\"LAST_5_YEARS\":{\"name\":\"Last 5 years\"},\"LAST_MONTH\":{\"name\":\"Last month\"},\"THIS_MONTH\":{\"name\":\"This month\"},\"cejWyOfXge6\":{\"name\":\"Gender\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"LAST_10_YEARS\":{\"name\":\"Last 10 years\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"A03MvHHogjR.UXz7xuGCEhU\":{\"name\":\"MCH Weight (g)\"},\"pC3N9N77UmT\":{\"uid\":\"pC3N9N77UmT\",\"name\":\"Gender\",\"options\":[{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\"}]},\"p2Zxg0wcPQ3\":{\"name\":\"BCG doses\"},\"UXz7xuGCEhU\":{\"name\":\"MCH Weight (g)\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"A03MvHHogjR.UXz7xuGCEhU\":[],\"p2Zxg0wcPQ3\":[],\"cejWyOfXge6\":[\"Mnp3oXrpAbK\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        1,
        "A03MvHHogjR.UXz7xuGCEhU",
        "MCH Weight (g)",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        2,
        "enrollmentdate",
        "Date of enrollment",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(
        response,
        3,
        "scheduleddate",
        "Scheduled date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(
        response,
        4,
        "incidentdate",
        "Date of birth",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(
        response, 5, "programstatus", "Program status", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        6,
        "eventdate",
        "Report date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(
        response, 7, "eventstatus", "Event status", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 8, "p2Zxg0wcPQ3", "BCG doses", "NUMBER", "java.lang.Double", false, true);
    validateHeader(
        response,
        9,
        "lastupdated",
        "Last updated on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(response, 10, "cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of(
            "Ngelehun CHC",
            "1231",
            "2022-07-02 02:00:00.0",
            "2021-07-23 12:46:11.472",
            "2022-07-08 02:00:00.0",
            "ACTIVE",
            "2021-07-03 00:00:00.0",
            "SCHEDULE",
            "0",
            "2017-07-23 12:46:11.472",
            "Female"));
  }

  @Test
  public void someDimensionsWithFilter() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add(
                "headers=eventdate,ouname,eMyVanycQSC,qrur9Dvnyt5,tUdBD1JDxpn,sGna2pquXOO,Kswd1r4qWLh,gWxh7DiRmG7,x7PaHGvgWY2,XCMi7Wvnplm,hlPt8H4bUOQ,Thkx2BnO5Kq,Y7hKDSuqEtH,K6uUAvq500H,msodh3rEMJa,oZg33kd9taw,GieVkTxp4HH,HS8QXAJtuKV,fWIAEtYVEGk,SWfdB5lX0fk,vV9UWAZohSf")
            .add("stage=Zj7UnCAulEk")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=100")
            .add("outputType=EVENT")
            .add("page=1")
            .add(
                "dimension=ou:O6uvpzGd5pu,eMyVanycQSC,qrur9Dvnyt5-Yf6UHoPkdS6:IN:TvM2MQgD7Jd,tUdBD1JDxpn:GT:21,sGna2pquXOO,Kswd1r4qWLh,gWxh7DiRmG7,x7PaHGvgWY2:GT:20,XCMi7Wvnplm:GE:22,hlPt8H4bUOQ,Thkx2BnO5Kq,Y7hKDSuqEtH,K6uUAvq500H:IN:D303,msodh3rEMJa,oZg33kd9taw,GieVkTxp4HH-TBxGTceyzwy:IN:wgbW2ZQnlIc,HS8QXAJtuKV,fWIAEtYVEGk,SWfdB5lX0fk,vV9UWAZohSf-OrkEzxZEH4X")
            .add("eventDate=THIS_YEAR")
            .add("relativePeriodDate=2022-07-01");

    // When
    ApiResponse response = actions.query().get("eBAyeGv0exc", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(21)))
        .body("rows", hasSize(equalTo(1)))
        .body("height", equalTo(1))
        .body("width", equalTo(21))
        .body("headerWidth", equalTo(21));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"lnccUWrmqL0\":{\"uid\":\"lnccUWrmqL0\",\"name\":\"80 - 90\"},\"GWuQsWJDGvN\":{\"uid\":\"GWuQsWJDGvN\",\"name\":\"140 - 160\"},\"iDFPKpFTiVw\":{\"uid\":\"iDFPKpFTiVw\",\"name\":\"Mode of discharge\",\"options\":[{\"uid\":\"gj2fKKyp8OH\",\"code\":\"MODDIED\"}]},\"qrur9Dvnyt5\":{\"uid\":\"qrur9Dvnyt5\",\"code\":\"DE_3000003\",\"name\":\"Age in years\",\"description\":\"Age of person in years.\",\"legendSet\":\"Yf6UHoPkdS6\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"pZzk1L4Blf1\":{\"uid\":\"pZzk1L4Blf1\",\"name\":\"0 - 10\"},\"gj2fKKyp8OH\":{\"uid\":\"gj2fKKyp8OH\",\"code\":\"MODDIED\",\"name\":\"Died\"},\"NxQrJ3icPkE\":{\"uid\":\"NxQrJ3icPkE\",\"name\":\"0 - 20\"},\"xVezsaEXU3k\":{\"uid\":\"xVezsaEXU3k\",\"name\":\"70 - 80\"},\"Zj7UnCAulEk\":{\"uid\":\"Zj7UnCAulEk\",\"name\":\"Inpatient morbidity and mortality\",\"description\":\"Anonymous and ICD-10 coded inpatient data\"},\"b7MCpzqJaR2\":{\"uid\":\"b7MCpzqJaR2\",\"name\":\"70 - 80\"},\"CivTksSoCt0\":{\"uid\":\"CivTksSoCt0\",\"name\":\"100 - 120\"},\"tUdBD1JDxpn\":{\"uid\":\"tUdBD1JDxpn\",\"name\":\"Average age of deaths\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"XCMi7Wvnplm\":{\"uid\":\"XCMi7Wvnplm\",\"name\":\"BMI female\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"Thkx2BnO5Kq\":{\"uid\":\"Thkx2BnO5Kq\",\"name\":\"BMI male\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"f3prvzpfniC\":{\"uid\":\"f3prvzpfniC\",\"name\":\"100+\"},\"sxFVvKLpE0y\":{\"uid\":\"sxFVvKLpE0y\",\"name\":\"0 - 100\"},\"Mnp3oXrpAbK\":{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\",\"name\":\"Female\"},\"B1X4JyH4Mdw\":{\"uid\":\"B1X4JyH4Mdw\",\"name\":\"180 - 200\"},\"CpP5yzbgfHo\":{\"uid\":\"CpP5yzbgfHo\",\"name\":\"40 - 50\"},\"scvmgP9F9rn\":{\"uid\":\"scvmgP9F9rn\",\"name\":\"90 - 100\"},\"gWxh7DiRmG7\":{\"uid\":\"gWxh7DiRmG7\",\"name\":\"Average height of girls at 5 years old\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"GieVkTxp4HH\":{\"uid\":\"GieVkTxp4HH\",\"code\":\"DE_240794\",\"name\":\"Height in cm\",\"legendSet\":\"TBxGTceyzwy\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"puI3YpLJ3fC\":{\"uid\":\"puI3YpLJ3fC\",\"name\":\"80 - 90\"},\"ZUUGJnvX40X\":{\"uid\":\"ZUUGJnvX40X\",\"name\":\"30 - 40\"},\"XKEvGfAkh3R\":{\"uid\":\"XKEvGfAkh3R\",\"name\":\"90 - 100\"},\"eySqrYxteI7\":{\"uid\":\"eySqrYxteI7\",\"name\":\"200+\"},\"hlPt8H4bUOQ\":{\"uid\":\"hlPt8H4bUOQ\",\"name\":\"BMI female under 5 y\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"BHlWGFLIU20\":{\"uid\":\"BHlWGFLIU20\",\"name\":\"120 - 140\"},\"GDFw7T4aFGz\":{\"uid\":\"GDFw7T4aFGz\",\"name\":\"60 - 70\"},\"OyVUzWsX8UF\":{\"uid\":\"OyVUzWsX8UF\",\"name\":\"10 - 20\"},\"THIS_YEAR\":{\"name\":\"This year\"},\"O6uvpzGd5pu\":{\"uid\":\"O6uvpzGd5pu\",\"code\":\"OU_264\",\"name\":\"Bo\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"URkaDRoGnQT\":{\"uid\":\"URkaDRoGnQT\",\"code\":\"D303\",\"name\":\"D303 Bladder\"},\"b9UzeWaSs2u\":{\"uid\":\"b9UzeWaSs2u\",\"name\":\"20 - 40\"},\"Kswd1r4qWLh\":{\"uid\":\"Kswd1r4qWLh\",\"name\":\"Average height of boys at 10 years old\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"Tq4NYCn9eNH\":{\"uid\":\"Tq4NYCn9eNH\",\"name\":\"60 - 70\"},\"eBAyeGv0exc\":{\"uid\":\"eBAyeGv0exc\",\"name\":\"Inpatient morbidity and mortality\"},\"eUZ79clX7y1\":{\"uid\":\"eUZ79clX7y1\",\"name\":\"Diagnosis ICD10\",\"options\":[{\"uid\":\"URkaDRoGnQT\",\"code\":\"D303\"}]},\"sGna2pquXOO\":{\"uid\":\"sGna2pquXOO\",\"name\":\"Average age of female discharges\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"x7PaHGvgWY2\":{\"uid\":\"x7PaHGvgWY2\",\"code\":\"BMI\",\"name\":\"BMI\",\"description\":\"Body Mass Index. Weight in kg / height in m square.\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"AD5jueZTZSK\":{\"uid\":\"AD5jueZTZSK\",\"name\":\"40 - 50\"},\"pC3N9N77UmT\":{\"uid\":\"pC3N9N77UmT\",\"name\":\"Gender\",\"options\":[{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\"}]},\"msodh3rEMJa\":{\"uid\":\"msodh3rEMJa\",\"code\":\"DE_3000006\",\"name\":\"Discharge Date\",\"description\":\"Date of discharge of patient.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"DATE\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"fWIAEtYVEGk\":{\"uid\":\"fWIAEtYVEGk\",\"code\":\"DE_3000009\",\"name\":\"Mode of Discharge\",\"description\":\"How the patient was discharged.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"vV9UWAZohSf\":{\"uid\":\"vV9UWAZohSf\",\"code\":\"DE_240795\",\"name\":\"Weight in kg\",\"legendSet\":\"OrkEzxZEH4X\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"K6uUAvq500H\":{\"uid\":\"K6uUAvq500H\",\"code\":\"DE_3000010\",\"name\":\"Diagnosis (ICD-10)\",\"description\":\"ICD-10 coded diagnosis.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"Sjp6IB3gthI\":{\"uid\":\"Sjp6IB3gthI\",\"name\":\"50 - 60\"},\"SWfdB5lX0fk\":{\"uid\":\"SWfdB5lX0fk\",\"code\":\"DE_423442\",\"name\":\"Pregnant\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"Y7hKDSuqEtH\":{\"uid\":\"Y7hKDSuqEtH\",\"name\":\"BMI male under 5 y\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"cbPqyIAFw9u\":{\"uid\":\"cbPqyIAFw9u\",\"name\":\"50 - 60\"},\"TvM2MQgD7Jd\":{\"uid\":\"TvM2MQgD7Jd\",\"name\":\"20 - 30\"},\"eMyVanycQSC\":{\"uid\":\"eMyVanycQSC\",\"code\":\"DE_3000005\",\"name\":\"Admission Date\",\"description\":\"Date of Admission of patient.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"DATE\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"wgbW2ZQnlIc\":{\"uid\":\"wgbW2ZQnlIc\",\"name\":\"160 - 180\"},\"oZg33kd9taw\":{\"uid\":\"oZg33kd9taw\",\"code\":\"DE_3000004\",\"name\":\"Gender\",\"description\":\"Gender of patient.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"HS8QXAJtuKV\":{\"uid\":\"HS8QXAJtuKV\",\"name\":\"Inpatient bed days average\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"hlPt8H4bUOQ\":[],\"ou\":[\"O6uvpzGd5pu\"],\"fWIAEtYVEGk\":[\"gj2fKKyp8OH\"],\"vV9UWAZohSf\":[\"NxQrJ3icPkE\",\"b9UzeWaSs2u\",\"AD5jueZTZSK\",\"Sjp6IB3gthI\",\"GDFw7T4aFGz\",\"xVezsaEXU3k\",\"lnccUWrmqL0\",\"XKEvGfAkh3R\",\"f3prvzpfniC\"],\"qrur9Dvnyt5\":[\"TvM2MQgD7Jd\"],\"Kswd1r4qWLh\":[],\"K6uUAvq500H\":[\"URkaDRoGnQT\"],\"SWfdB5lX0fk\":[],\"Y7hKDSuqEtH\":[],\"gWxh7DiRmG7\":[],\"GieVkTxp4HH\":[\"wgbW2ZQnlIc\"],\"eMyVanycQSC\":[],\"pe\":[],\"tUdBD1JDxpn\":[],\"sGna2pquXOO\":[],\"XCMi7Wvnplm\":[],\"x7PaHGvgWY2\":[],\"oZg33kd9taw\":[\"Mnp3oXrpAbK\"],\"Thkx2BnO5Kq\":[],\"HS8QXAJtuKV\":[],\"msodh3rEMJa\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response,
        0,
        "eventdate",
        "Report date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(
        response, 1, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 2, "eMyVanycQSC", "Admission Date", "DATE", "java.time.LocalDate", false, true);
    validateHeader(
        response, 3, "qrur9Dvnyt5", "Age in years", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        4,
        "tUdBD1JDxpn",
        "Average age of deaths",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        5,
        "sGna2pquXOO",
        "Average age of female discharges",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        6,
        "Kswd1r4qWLh",
        "Average height of boys at 10 years old",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        7,
        "gWxh7DiRmG7",
        "Average height of girls at 5 years old",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(response, 8, "x7PaHGvgWY2", "BMI", "NUMBER", "java.lang.Double", false, true);
    validateHeader(
        response, 9, "XCMi7Wvnplm", "BMI female", "NUMBER", "java.lang.Double", false, true);
    validateHeader(
        response,
        10,
        "hlPt8H4bUOQ",
        "BMI female under 5 y",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response, 11, "Thkx2BnO5Kq", "BMI male", "NUMBER", "java.lang.Double", false, true);
    validateHeader(
        response,
        12,
        "Y7hKDSuqEtH",
        "BMI male under 5 y",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response, 13, "K6uUAvq500H", "Diagnosis (ICD-10)", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 14, "msodh3rEMJa", "Discharge Date", "DATE", "java.time.LocalDate", false, true);
    validateHeader(response, 15, "oZg33kd9taw", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 16, "GieVkTxp4HH", "Height in cm", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        17,
        "HS8QXAJtuKV",
        "Inpatient bed days average",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response, 18, "fWIAEtYVEGk", "Mode of Discharge", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 19, "SWfdB5lX0fk", "Pregnant", "BOOLEAN", "java.lang.Boolean", false, true);
    validateHeader(
        response, 20, "vV9UWAZohSf", "Weight in kg", "TEXT", "java.lang.String", false, true);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of(
            "2022-04-28 00:00:00.0",
            "New Police Barracks CHC",
            "2018-04-14 00:00:00.0",
            "TvM2MQgD7Jd",
            "23",
            "",
            "",
            "",
            "30.12",
            "30.12",
            "",
            "",
            "",
            "D303",
            "2018-04-28 00:00:00.0",
            "Female",
            "wgbW2ZQnlIc",
            "14",
            "MODDIED",
            "",
            "lnccUWrmqL0"));
  }
}
