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
import static org.hisp.dhis.analytics.ValidationHelper.validateHeaderPropertiesByName;
import static org.hisp.dhis.analytics.ValidationHelper.validateResponseStructure;
import static org.hisp.dhis.analytics.ValidationHelper.validateRowValueByName;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsTrackedEntityActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TrackedEntityQuery10AutoTest extends AnalyticsApiTest {
  private final AnalyticsTrackedEntityActions actions = new AnalyticsTrackedEntityActions();

  @DisplayName(
      "Should return correct headers and values when using stage-specific dimension with header")
  @Test
  public void stageDimensionCheckMetadataItem() throws JSONException {

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=ur1Edk5Oe2n.enrollmentdate")
            .add("headers=A03MvHHogjR.a3kGcGDCuk6")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=10")
            .add("page=1")
            .add("dimension=A03MvHHogjR.ou:USER_ORGUNIT,A03MvHHogjR.a3kGcGDCuk6:GT:10");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":10,\"isLastPage\":true},\"items\":{\"zDhUuAYrxNC\":{\"uid\":\"zDhUuAYrxNC\",\"name\":\"Last name\",\"description\":\"Last name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"lw1SqmMlnfh\":{\"uid\":\"lw1SqmMlnfh\",\"code\":\"Height in cm\",\"name\":\"Height in cm\",\"description\":\"Height in cm\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"fDd25txQckK\":{\"uid\":\"fDd25txQckK\",\"name\":\"Provider Follow-up and Support Tool\"},\"DODgdr5Oo2v\":{\"uid\":\"DODgdr5Oo2v\",\"code\":\"Provider ID\",\"name\":\"Provider ID\",\"description\":\"Provider ID\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"Qo571yj6Zcn\":{\"uid\":\"Qo571yj6Zcn\",\"code\":\"Latitude\",\"name\":\"Latitude\",\"description\":\"Latitude\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"iESIqZ0R0R0\":{\"uid\":\"iESIqZ0R0R0\",\"name\":\"Date of birth\",\"description\":\"Date of birth\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"n9nUvfpTsxQ\":{\"uid\":\"n9nUvfpTsxQ\",\"code\":\"Zip code\",\"name\":\"Zip code\",\"description\":\"Zip code\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"kyIzQsj96BD\":{\"uid\":\"kyIzQsj96BD\",\"code\":\"Company\",\"name\":\"Company\",\"description\":\"Company\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"A4xFHyieXys\":{\"uid\":\"A4xFHyieXys\",\"code\":\"Occupation\",\"name\":\"Occupation\",\"description\":\"Occupation\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"xs8A6tQJY0s\":{\"uid\":\"xs8A6tQJY0s\",\"name\":\"TB identifier\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"OvY4VVhSDeJ\":{\"uid\":\"OvY4VVhSDeJ\",\"name\":\"Weight in kg\",\"description\":\"Weight in kg\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"IpHINAT79UW.A03MvHHogjR.ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"ur1Edk5Oe2n\":{\"uid\":\"ur1Edk5Oe2n\",\"name\":\"TB program\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"RG7uGl4w5Jq\":{\"uid\":\"RG7uGl4w5Jq\",\"code\":\"Longitude\",\"name\":\"Longitude\",\"description\":\"Longitude\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"spFvx9FndA4\":{\"uid\":\"spFvx9FndA4\",\"name\":\"Age\",\"description\":\"Age\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"Agywv2JGwuq\":{\"uid\":\"Agywv2JGwuq\",\"code\":\"MMD_PER_MOB\",\"name\":\"Mobile number\",\"description\":\"Mobile number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"GUOBQt5K2WI\":{\"uid\":\"GUOBQt5K2WI\",\"code\":\"State\",\"name\":\"State\",\"description\":\"State\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"lZGmxYbs97q\":{\"uid\":\"lZGmxYbs97q\",\"code\":\"MMD_PER_ID\",\"name\":\"Unique ID\",\"description\":\"Unique identiifer\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"A03MvHHogjR.a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"VqEFza8wbwA\":{\"uid\":\"VqEFza8wbwA\",\"code\":\"MMD_PER_ADR1\",\"name\":\"Address\",\"description\":\"Country\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"code\":\"MMD_PER_STA\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"w75KJ2mc4zz\":{\"uid\":\"w75KJ2mc4zz\",\"code\":\"MMD_PER_NAM\",\"name\":\"First name\",\"description\":\"First name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"KmEUg2hHEtx\":{\"uid\":\"KmEUg2hHEtx\",\"name\":\"Email address\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"G7vUx908SwP\":{\"uid\":\"G7vUx908SwP\",\"name\":\"Residence location\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"COORDINATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"o9odfev2Ty5\":{\"uid\":\"o9odfev2Ty5\",\"code\":\"Mother maiden name\",\"name\":\"Mother maiden name\",\"description\":\"Mother maiden name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"FO4sWYJ64LQ\":{\"uid\":\"FO4sWYJ64LQ\",\"code\":\"City\",\"name\":\"City\",\"description\":\"City\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"NDXw0cluzSw\":{\"uid\":\"NDXw0cluzSw\",\"name\":\"Email\",\"description\":\"Email address\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ruQQnf6rswq\":{\"uid\":\"ruQQnf6rswq\",\"name\":\"TB number\",\"description\":\"TB number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"cejWyOfXge6\":{\"uid\":\"cejWyOfXge6\",\"name\":\"Gender\",\"description\":\"Gender\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"P2cwLGskgxn\":{\"uid\":\"P2cwLGskgxn\",\"name\":\"Phone number\",\"description\":\"Phone number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"code\":\"MMD_PER_DOB\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ur1Edk5Oe2n.enrollmentdate\":{\"name\":\"Start of treatment date\",\"dimensionType\":\"PERIOD\"},\"uy2gU8kT1jF\":{\"uid\":\"uy2gU8kT1jF\",\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"VHfUeXpawmE\":{\"uid\":\"VHfUeXpawmE\",\"code\":\"Vehicle\",\"name\":\"Vehicle\",\"description\":\"Vehicle\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"AuPLng5hLbE\":{\"uid\":\"AuPLng5hLbE\",\"code\":\"National identifier\",\"name\":\"National identifier\",\"description\":\"National identifier\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ZcBPrXKahq2\":{\"uid\":\"ZcBPrXKahq2\",\"name\":\"Postal code\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"WSGAb5XwJ3Y\":{\"uid\":\"WSGAb5XwJ3Y\",\"name\":\"WHO RMNCH Tracker\"},\"H9IlTX2X6SL\":{\"uid\":\"H9IlTX2X6SL\",\"code\":\"Blood type\",\"name\":\"Blood type\",\"description\":\"Blood type\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"DODgdr5Oo2v\":[],\"Qo571yj6Zcn\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"A4xFHyieXys\":[],\"xs8A6tQJY0s\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"Agywv2JGwuq\":[],\"GUOBQt5K2WI\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[\"wfkKVdPBzho\",\"Yjte6foKMny\"],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"a3kGcGDCuk6\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.a3kGcGDCuk6",
        "MCH Apgar Score, Child Programme, Birth",
        "NUMBER",
        "java.lang.Double",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "A03MvHHogjR.a3kGcGDCuk6", "11");

    // Validate selected values for row index 1
    validateRowValueByName(response, actualHeaders, 1, "A03MvHHogjR.a3kGcGDCuk6", "11");
  }

  @Test
  @DisplayName(
      "Should fail when ENROLLMENT_DATE dimension is used with stage-specific prefix (not supported)")
  public void stageSpecificEnrollmentDateDimensionShouldFail() throws JSONException {

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=ur1Edk5Oe2n.enrollmentdate")
            .add("headers=ouname,ur1Edk5Oe2n.enrollmentdate")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=10")
            .add("page=1")
            .add("dimension=ZkbAXlQUYJG.ENROLLMENT_DATE:2021,ou:USER_ORGUNIT");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(409)
        .body("httpStatus", equalTo("Conflict"))
        .body("httpStatusCode", equalTo(409))
        .body("status", equalTo("ERROR"))
        .body(
            "message",
            equalTo(
                "Dimension `ENROLLMENT_DATE` is not supported for program stage `ZkbAXlQUYJG`. "
                    + "Only event-level dimensions are supported for stage-specific scope"))
        .body("errorCode", equalTo("E7253"));
  }

  @Test
  public void stageSpecificHeaderWithoutDimensionShouldWork() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=ouname")
            .add("headers=ouname,IpHINAT79UW.enrollmentdate,A03MvHHogjR.eventdate")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=10")
            .add("page=1")
            .add("relativePeriodDate=2026-01-30")
            .add("dimension=ou:USER_ORGUNIT");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        10,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":10,\"isLastPage\":false},\"items\":{\"lw1SqmMlnfh\":{\"uid\":\"lw1SqmMlnfh\",\"code\":\"Height in cm\",\"name\":\"Height in cm\",\"description\":\"Height in cm\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"jdRD35YwbRH\":{\"uid\":\"jdRD35YwbRH\",\"name\":\"Sputum smear microscopy test\",\"description\":\"Sputum smear microscopy test\"},\"IpHINAT79UW.enrollmentdate\":{\"name\":\"Date of enrollment\",\"dimensionType\":\"PERIOD\"},\"DODgdr5Oo2v\":{\"uid\":\"DODgdr5Oo2v\",\"code\":\"Provider ID\",\"name\":\"Provider ID\",\"description\":\"Provider ID\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"iESIqZ0R0R0\":{\"uid\":\"iESIqZ0R0R0\",\"name\":\"Date of birth\",\"description\":\"Date of birth\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"n9nUvfpTsxQ\":{\"uid\":\"n9nUvfpTsxQ\",\"code\":\"Zip code\",\"name\":\"Zip code\",\"description\":\"Zip code\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"PFDfvmGpsR3\":{\"uid\":\"PFDfvmGpsR3\",\"name\":\"Care at birth\",\"description\":\"Intrapartum care \\/ Childbirth \\/ Labour and delivery\"},\"lST1OZ5BDJ2\":{\"uid\":\"lST1OZ5BDJ2\",\"name\":\"Provider Follow-up and Support Tool\",\"description\":\"Provider Follow-up and Support Tool\"},\"PUZaKR0Jh2k\":{\"uid\":\"PUZaKR0Jh2k\",\"name\":\"Previous deliveries\",\"description\":\"Table for recording earlier deliveries\"},\"RG7uGl4w5Jq\":{\"uid\":\"RG7uGl4w5Jq\",\"code\":\"Longitude\",\"name\":\"Longitude\",\"description\":\"Longitude\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"WZbXY0S00lP\":{\"uid\":\"WZbXY0S00lP\",\"name\":\"First antenatal care visit\",\"description\":\"First antenatal care visit\"},\"Xgk8Wvl0jHr\":{\"uid\":\"Xgk8Wvl0jHr\",\"name\":\"Delivery\",\"description\":\"Delivery phase\"},\"GUOBQt5K2WI\":{\"uid\":\"GUOBQt5K2WI\",\"code\":\"State\",\"name\":\"State\",\"description\":\"State\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"VqEFza8wbwA\":{\"uid\":\"VqEFza8wbwA\",\"code\":\"MMD_PER_ADR1\",\"name\":\"Address\",\"description\":\"Country\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"w75KJ2mc4zz\":{\"uid\":\"w75KJ2mc4zz\",\"code\":\"MMD_PER_NAM\",\"name\":\"First name\",\"description\":\"First name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"KmEUg2hHEtx\":{\"uid\":\"KmEUg2hHEtx\",\"name\":\"Email address\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"G7vUx908SwP\":{\"uid\":\"G7vUx908SwP\",\"name\":\"Residence location\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"COORDINATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"o9odfev2Ty5\":{\"uid\":\"o9odfev2Ty5\",\"code\":\"Mother maiden name\",\"name\":\"Mother maiden name\",\"description\":\"Mother maiden name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"FO4sWYJ64LQ\":{\"uid\":\"FO4sWYJ64LQ\",\"code\":\"City\",\"name\":\"City\",\"description\":\"City\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"NDXw0cluzSw\":{\"uid\":\"NDXw0cluzSw\",\"name\":\"Email\",\"description\":\"Email address\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"oRySG82BKE6\":{\"uid\":\"oRySG82BKE6\",\"name\":\"PNC Visit\",\"description\":\"Post Natal Care Visit\"},\"ruQQnf6rswq\":{\"uid\":\"ruQQnf6rswq\",\"name\":\"TB number\",\"description\":\"TB number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"grIfo3oOf4Y\":{\"uid\":\"grIfo3oOf4Y\",\"name\":\"ANC Visit (2-4+)\",\"description\":\"ANC visits 2 to 4+\"},\"cejWyOfXge6\":{\"uid\":\"cejWyOfXge6\",\"name\":\"Gender\",\"description\":\"Gender\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"VHfUeXpawmE\":{\"uid\":\"VHfUeXpawmE\",\"code\":\"Vehicle\",\"name\":\"Vehicle\",\"description\":\"Vehicle\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ZcBPrXKahq2\":{\"uid\":\"ZcBPrXKahq2\",\"name\":\"Postal code\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"H9IlTX2X6SL\":{\"uid\":\"H9IlTX2X6SL\",\"code\":\"Blood type\",\"name\":\"Blood type\",\"description\":\"Blood type\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"zDhUuAYrxNC\":{\"uid\":\"zDhUuAYrxNC\",\"name\":\"Last name\",\"description\":\"Last name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"fDd25txQckK\":{\"uid\":\"fDd25txQckK\",\"name\":\"Provider Follow-up and Support Tool\"},\"Qo571yj6Zcn\":{\"uid\":\"Qo571yj6Zcn\",\"code\":\"Latitude\",\"name\":\"Latitude\",\"description\":\"Latitude\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"kyIzQsj96BD\":{\"uid\":\"kyIzQsj96BD\",\"code\":\"Company\",\"name\":\"Company\",\"description\":\"Company\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"A4xFHyieXys\":{\"uid\":\"A4xFHyieXys\",\"code\":\"Occupation\",\"name\":\"Occupation\",\"description\":\"Occupation\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"xs8A6tQJY0s\":{\"uid\":\"xs8A6tQJY0s\",\"name\":\"TB identifier\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"OvY4VVhSDeJ\":{\"uid\":\"OvY4VVhSDeJ\",\"name\":\"Weight in kg\",\"description\":\"Weight in kg\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"EPEcjy3FWmI\":{\"uid\":\"EPEcjy3FWmI\",\"name\":\"Lab monitoring\",\"description\":\"Laboratory monitoring\"},\"ur1Edk5Oe2n\":{\"uid\":\"ur1Edk5Oe2n\",\"name\":\"TB program\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"spFvx9FndA4\":{\"uid\":\"spFvx9FndA4\",\"name\":\"Age\",\"description\":\"Age\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"Agywv2JGwuq\":{\"uid\":\"Agywv2JGwuq\",\"code\":\"MMD_PER_MOB\",\"name\":\"Mobile number\",\"description\":\"Mobile number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"lZGmxYbs97q\":{\"uid\":\"lZGmxYbs97q\",\"code\":\"MMD_PER_ID\",\"name\":\"Unique ID\",\"description\":\"Unique identiifer\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"bbKtnxRZKEP\":{\"uid\":\"bbKtnxRZKEP\",\"name\":\"Postpartum care visit\",\"description\":\"Provision of care for the mother for some weeks after delivery\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"code\":\"MMD_PER_STA\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"edqlbukwRfQ\":{\"uid\":\"edqlbukwRfQ\",\"name\":\"Second antenatal care visit\",\"description\":\"Antenatal care visit\"},\"ZkbAXlQUYJG\":{\"uid\":\"ZkbAXlQUYJG\",\"name\":\"TB visit\",\"description\":\"Routine TB visit\"},\"P2cwLGskgxn\":{\"uid\":\"P2cwLGskgxn\",\"name\":\"Phone number\",\"description\":\"Phone number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"code\":\"MMD_PER_DOB\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"eaDHS084uMp\":{\"uid\":\"eaDHS084uMp\",\"name\":\"ANC 1st visit\",\"description\":\"ANC 1st visit\"},\"uy2gU8kT1jF\":{\"uid\":\"uy2gU8kT1jF\",\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"AuPLng5hLbE\":{\"uid\":\"AuPLng5hLbE\",\"code\":\"National identifier\",\"name\":\"National identifier\",\"description\":\"National identifier\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"WSGAb5XwJ3Y\":{\"uid\":\"WSGAb5XwJ3Y\",\"name\":\"WHO RMNCH Tracker\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"DODgdr5Oo2v\":[],\"Qo571yj6Zcn\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"A4xFHyieXys\":[],\"xs8A6tQJY0s\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"Agywv2JGwuq\":[],\"GUOBQt5K2WI\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[\"wfkKVdPBzho\",\"Yjte6foKMny\"],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "IpHINAT79UW.enrollmentdate",
        "Date of enrollment, Child Programme",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.eventdate",
        "Event Date, Child Programme, Birth",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "ouname", " Panderu MCHP");
    validateRowValueByName(response, actualHeaders, 0, "A03MvHHogjR.eventdate", "");

    // Validate selected values for row index 3
    validateRowValueByName(response, actualHeaders, 3, "ouname", " Panderu MCHP");
    validateRowValueByName(response, actualHeaders, 3, "A03MvHHogjR.eventdate", "");

    // Validate selected values for row index 6
    validateRowValueByName(response, actualHeaders, 6, "ouname", " Panderu MCHP");
    validateRowValueByName(response, actualHeaders, 6, "A03MvHHogjR.eventdate", "");

    // Validate selected values for row index 9
    validateRowValueByName(response, actualHeaders, 9, "ouname", " Panderu MCHP");
    validateRowValueByName(response, actualHeaders, 9, "A03MvHHogjR.eventdate", "");
  }

  @Test
  public void stageAndEventDateFailsWithInvalidDate() throws JSONException {

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=ur1Edk5Oe2n.enrollmentdate")
            .add("headers=ouname,ur1Edk5Oe2n.enrollmentdate,ZkbAXlQUYJG.eventdate")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=10")
            .add("page=1")
            .add("dimension=ZkbAXlQUYJG.EVENT_DATE:ACTIVE;COMPLETED,ou:USER_ORGUNIT");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(409)
        .body("httpStatus", equalTo("Conflict"))
        .body("httpStatusCode", equalTo(409))
        .body("status", equalTo("ERROR"))
        .body("message", equalTo("Date time is not parsable: `ACTIVE`"))
        .body("errorCode", equalTo("E7135"));
  }

  @Test
  public void stageAndEventDateFixedYear() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=ur1Edk5Oe2n.enrollmentdate")
            .add("headers=ouname,ur1Edk5Oe2n.enrollmentdate,ZkbAXlQUYJG.eventdate")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=10")
            .add("page=1")
            .add("dimension=ZkbAXlQUYJG.EVENT_DATE:2022,ou:USER_ORGUNIT");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        3,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":10,\"isLastPage\":true},\"items\":{\"zDhUuAYrxNC\":{\"uid\":\"zDhUuAYrxNC\",\"name\":\"Last name\",\"description\":\"Last name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"lw1SqmMlnfh\":{\"uid\":\"lw1SqmMlnfh\",\"code\":\"Height in cm\",\"name\":\"Height in cm\",\"description\":\"Height in cm\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"fDd25txQckK\":{\"uid\":\"fDd25txQckK\",\"name\":\"Provider Follow-up and Support Tool\"},\"DODgdr5Oo2v\":{\"uid\":\"DODgdr5Oo2v\",\"code\":\"Provider ID\",\"name\":\"Provider ID\",\"description\":\"Provider ID\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"Qo571yj6Zcn\":{\"uid\":\"Qo571yj6Zcn\",\"code\":\"Latitude\",\"name\":\"Latitude\",\"description\":\"Latitude\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"iESIqZ0R0R0\":{\"uid\":\"iESIqZ0R0R0\",\"name\":\"Date of birth\",\"description\":\"Date of birth\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"n9nUvfpTsxQ\":{\"uid\":\"n9nUvfpTsxQ\",\"code\":\"Zip code\",\"name\":\"Zip code\",\"description\":\"Zip code\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"kyIzQsj96BD\":{\"uid\":\"kyIzQsj96BD\",\"code\":\"Company\",\"name\":\"Company\",\"description\":\"Company\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"A4xFHyieXys\":{\"uid\":\"A4xFHyieXys\",\"code\":\"Occupation\",\"name\":\"Occupation\",\"description\":\"Occupation\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"xs8A6tQJY0s\":{\"uid\":\"xs8A6tQJY0s\",\"name\":\"TB identifier\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"OvY4VVhSDeJ\":{\"uid\":\"OvY4VVhSDeJ\",\"name\":\"Weight in kg\",\"description\":\"Weight in kg\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ur1Edk5Oe2n\":{\"uid\":\"ur1Edk5Oe2n\",\"name\":\"TB program\"},\"RG7uGl4w5Jq\":{\"uid\":\"RG7uGl4w5Jq\",\"code\":\"Longitude\",\"name\":\"Longitude\",\"description\":\"Longitude\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"spFvx9FndA4\":{\"uid\":\"spFvx9FndA4\",\"name\":\"Age\",\"description\":\"Age\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"Agywv2JGwuq\":{\"uid\":\"Agywv2JGwuq\",\"code\":\"MMD_PER_MOB\",\"name\":\"Mobile number\",\"description\":\"Mobile number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"GUOBQt5K2WI\":{\"uid\":\"GUOBQt5K2WI\",\"code\":\"State\",\"name\":\"State\",\"description\":\"State\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"lZGmxYbs97q\":{\"uid\":\"lZGmxYbs97q\",\"code\":\"MMD_PER_ID\",\"name\":\"Unique ID\",\"description\":\"Unique identiifer\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"VqEFza8wbwA\":{\"uid\":\"VqEFza8wbwA\",\"code\":\"MMD_PER_ADR1\",\"name\":\"Address\",\"description\":\"Country\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"code\":\"MMD_PER_STA\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"w75KJ2mc4zz\":{\"uid\":\"w75KJ2mc4zz\",\"code\":\"MMD_PER_NAM\",\"name\":\"First name\",\"description\":\"First name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"KmEUg2hHEtx\":{\"uid\":\"KmEUg2hHEtx\",\"name\":\"Email address\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"G7vUx908SwP\":{\"uid\":\"G7vUx908SwP\",\"name\":\"Residence location\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"COORDINATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"o9odfev2Ty5\":{\"uid\":\"o9odfev2Ty5\",\"code\":\"Mother maiden name\",\"name\":\"Mother maiden name\",\"description\":\"Mother maiden name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"FO4sWYJ64LQ\":{\"uid\":\"FO4sWYJ64LQ\",\"code\":\"City\",\"name\":\"City\",\"description\":\"City\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ZkbAXlQUYJG\":{\"uid\":\"ZkbAXlQUYJG\",\"name\":\"TB visit\",\"description\":\"Routine TB visit\"},\"NDXw0cluzSw\":{\"uid\":\"NDXw0cluzSw\",\"name\":\"Email\",\"description\":\"Email address\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ruQQnf6rswq\":{\"uid\":\"ruQQnf6rswq\",\"name\":\"TB number\",\"description\":\"TB number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"cejWyOfXge6\":{\"uid\":\"cejWyOfXge6\",\"name\":\"Gender\",\"description\":\"Gender\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"P2cwLGskgxn\":{\"uid\":\"P2cwLGskgxn\",\"name\":\"Phone number\",\"description\":\"Phone number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"code\":\"MMD_PER_DOB\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ur1Edk5Oe2n.enrollmentdate\":{\"name\":\"Start of treatment date\",\"dimensionType\":\"PERIOD\"},\"uy2gU8kT1jF\":{\"uid\":\"uy2gU8kT1jF\",\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"VHfUeXpawmE\":{\"uid\":\"VHfUeXpawmE\",\"code\":\"Vehicle\",\"name\":\"Vehicle\",\"description\":\"Vehicle\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"AuPLng5hLbE\":{\"uid\":\"AuPLng5hLbE\",\"code\":\"National identifier\",\"name\":\"National identifier\",\"description\":\"National identifier\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ZcBPrXKahq2\":{\"uid\":\"ZcBPrXKahq2\",\"name\":\"Postal code\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"WSGAb5XwJ3Y\":{\"uid\":\"WSGAb5XwJ3Y\",\"name\":\"WHO RMNCH Tracker\"},\"H9IlTX2X6SL\":{\"uid\":\"H9IlTX2X6SL\",\"code\":\"Blood type\",\"name\":\"Blood type\",\"description\":\"Blood type\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"DODgdr5Oo2v\":[],\"Qo571yj6Zcn\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"A4xFHyieXys\":[],\"xs8A6tQJY0s\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"Agywv2JGwuq\":[],\"GUOBQt5K2WI\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[\"wfkKVdPBzho\",\"Yjte6foKMny\"],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ur1Edk5Oe2n.enrollmentdate",
        "Start of treatment date, TB program",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ZkbAXlQUYJG.eventdate",
        "Event Date, TB program, TB visit",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Ngelehun CHC");
    validateRowValueByName(
        response, actualHeaders, 0, "ZkbAXlQUYJG.eventdate", "2022-07-03 00:00:00.0");

    // Validate selected values for row index 2
    validateRowValueByName(response, actualHeaders, 2, "ouname", "Ngelehun CHC");
    validateRowValueByName(
        response, actualHeaders, 2, "ZkbAXlQUYJG.eventdate", "2022-01-01 00:00:00.0");
  }

  @Test
  public void stageAndEventDateDateRange() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=ur1Edk5Oe2n.enrollmentdate")
            .add("headers=ouname,ur1Edk5Oe2n.enrollmentdate,ZkbAXlQUYJG.eventdate")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=10")
            .add("page=1")
            .add("dimension=ZkbAXlQUYJG.EVENT_DATE:2022-06-01_2022-09-30,ou:USER_ORGUNIT");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        2,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":10,\"isLastPage\":true},\"items\":{\"zDhUuAYrxNC\":{\"uid\":\"zDhUuAYrxNC\",\"name\":\"Last name\",\"description\":\"Last name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"lw1SqmMlnfh\":{\"uid\":\"lw1SqmMlnfh\",\"code\":\"Height in cm\",\"name\":\"Height in cm\",\"description\":\"Height in cm\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"fDd25txQckK\":{\"uid\":\"fDd25txQckK\",\"name\":\"Provider Follow-up and Support Tool\"},\"DODgdr5Oo2v\":{\"uid\":\"DODgdr5Oo2v\",\"code\":\"Provider ID\",\"name\":\"Provider ID\",\"description\":\"Provider ID\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"Qo571yj6Zcn\":{\"uid\":\"Qo571yj6Zcn\",\"code\":\"Latitude\",\"name\":\"Latitude\",\"description\":\"Latitude\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"iESIqZ0R0R0\":{\"uid\":\"iESIqZ0R0R0\",\"name\":\"Date of birth\",\"description\":\"Date of birth\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"n9nUvfpTsxQ\":{\"uid\":\"n9nUvfpTsxQ\",\"code\":\"Zip code\",\"name\":\"Zip code\",\"description\":\"Zip code\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"kyIzQsj96BD\":{\"uid\":\"kyIzQsj96BD\",\"code\":\"Company\",\"name\":\"Company\",\"description\":\"Company\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"A4xFHyieXys\":{\"uid\":\"A4xFHyieXys\",\"code\":\"Occupation\",\"name\":\"Occupation\",\"description\":\"Occupation\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"xs8A6tQJY0s\":{\"uid\":\"xs8A6tQJY0s\",\"name\":\"TB identifier\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"OvY4VVhSDeJ\":{\"uid\":\"OvY4VVhSDeJ\",\"name\":\"Weight in kg\",\"description\":\"Weight in kg\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ur1Edk5Oe2n\":{\"uid\":\"ur1Edk5Oe2n\",\"name\":\"TB program\"},\"RG7uGl4w5Jq\":{\"uid\":\"RG7uGl4w5Jq\",\"code\":\"Longitude\",\"name\":\"Longitude\",\"description\":\"Longitude\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"spFvx9FndA4\":{\"uid\":\"spFvx9FndA4\",\"name\":\"Age\",\"description\":\"Age\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"Agywv2JGwuq\":{\"uid\":\"Agywv2JGwuq\",\"code\":\"MMD_PER_MOB\",\"name\":\"Mobile number\",\"description\":\"Mobile number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"GUOBQt5K2WI\":{\"uid\":\"GUOBQt5K2WI\",\"code\":\"State\",\"name\":\"State\",\"description\":\"State\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"lZGmxYbs97q\":{\"uid\":\"lZGmxYbs97q\",\"code\":\"MMD_PER_ID\",\"name\":\"Unique ID\",\"description\":\"Unique identiifer\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"VqEFza8wbwA\":{\"uid\":\"VqEFza8wbwA\",\"code\":\"MMD_PER_ADR1\",\"name\":\"Address\",\"description\":\"Country\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"code\":\"MMD_PER_STA\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"w75KJ2mc4zz\":{\"uid\":\"w75KJ2mc4zz\",\"code\":\"MMD_PER_NAM\",\"name\":\"First name\",\"description\":\"First name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"KmEUg2hHEtx\":{\"uid\":\"KmEUg2hHEtx\",\"name\":\"Email address\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"G7vUx908SwP\":{\"uid\":\"G7vUx908SwP\",\"name\":\"Residence location\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"COORDINATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"o9odfev2Ty5\":{\"uid\":\"o9odfev2Ty5\",\"code\":\"Mother maiden name\",\"name\":\"Mother maiden name\",\"description\":\"Mother maiden name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"FO4sWYJ64LQ\":{\"uid\":\"FO4sWYJ64LQ\",\"code\":\"City\",\"name\":\"City\",\"description\":\"City\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ZkbAXlQUYJG\":{\"uid\":\"ZkbAXlQUYJG\",\"name\":\"TB visit\",\"description\":\"Routine TB visit\"},\"NDXw0cluzSw\":{\"uid\":\"NDXw0cluzSw\",\"name\":\"Email\",\"description\":\"Email address\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ruQQnf6rswq\":{\"uid\":\"ruQQnf6rswq\",\"name\":\"TB number\",\"description\":\"TB number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"cejWyOfXge6\":{\"uid\":\"cejWyOfXge6\",\"name\":\"Gender\",\"description\":\"Gender\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"P2cwLGskgxn\":{\"uid\":\"P2cwLGskgxn\",\"name\":\"Phone number\",\"description\":\"Phone number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"code\":\"MMD_PER_DOB\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ur1Edk5Oe2n.enrollmentdate\":{\"name\":\"Start of treatment date\",\"dimensionType\":\"PERIOD\"},\"uy2gU8kT1jF\":{\"uid\":\"uy2gU8kT1jF\",\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"VHfUeXpawmE\":{\"uid\":\"VHfUeXpawmE\",\"code\":\"Vehicle\",\"name\":\"Vehicle\",\"description\":\"Vehicle\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"AuPLng5hLbE\":{\"uid\":\"AuPLng5hLbE\",\"code\":\"National identifier\",\"name\":\"National identifier\",\"description\":\"National identifier\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ZcBPrXKahq2\":{\"uid\":\"ZcBPrXKahq2\",\"name\":\"Postal code\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"WSGAb5XwJ3Y\":{\"uid\":\"WSGAb5XwJ3Y\",\"name\":\"WHO RMNCH Tracker\"},\"H9IlTX2X6SL\":{\"uid\":\"H9IlTX2X6SL\",\"code\":\"Blood type\",\"name\":\"Blood type\",\"description\":\"Blood type\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"DODgdr5Oo2v\":[],\"Qo571yj6Zcn\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"A4xFHyieXys\":[],\"xs8A6tQJY0s\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"Agywv2JGwuq\":[],\"GUOBQt5K2WI\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[\"wfkKVdPBzho\",\"Yjte6foKMny\"],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ur1Edk5Oe2n.enrollmentdate",
        "Start of treatment date, TB program",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ZkbAXlQUYJG.eventdate",
        "Event Date, TB program, TB visit",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Ngelehun CHC");
    validateRowValueByName(
        response, actualHeaders, 0, "ZkbAXlQUYJG.eventdate", "2022-07-03 00:00:00.0");

    // Validate selected values for row index 1
    validateRowValueByName(response, actualHeaders, 1, "ouname", "Ngelehun CHC");
    validateRowValueByName(
        response, actualHeaders, 1, "ZkbAXlQUYJG.eventdate", "2022-06-03 00:00:00.0");
  }

  @Test
  public void stageAndEventStatus() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=ur1Edk5Oe2n.enrollmentdate")
            .add("headers=ouname,ur1Edk5Oe2n.enrollmentdate,ZkbAXlQUYJG.eventstatus")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=10")
            .add("page=1")
            .add("relativePeriodDate=2026-01-30")
            .add("dimension=ZkbAXlQUYJG.EVENT_STATUS:ACTIVE;COMPLETED,ou:USER_ORGUNIT");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        4,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":10,\"isLastPage\":true},\"items\":{\"zDhUuAYrxNC\":{\"uid\":\"zDhUuAYrxNC\",\"name\":\"Last name\",\"description\":\"Last name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"lw1SqmMlnfh\":{\"uid\":\"lw1SqmMlnfh\",\"code\":\"Height in cm\",\"name\":\"Height in cm\",\"description\":\"Height in cm\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"fDd25txQckK\":{\"uid\":\"fDd25txQckK\",\"name\":\"Provider Follow-up and Support Tool\"},\"DODgdr5Oo2v\":{\"uid\":\"DODgdr5Oo2v\",\"code\":\"Provider ID\",\"name\":\"Provider ID\",\"description\":\"Provider ID\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"Qo571yj6Zcn\":{\"uid\":\"Qo571yj6Zcn\",\"code\":\"Latitude\",\"name\":\"Latitude\",\"description\":\"Latitude\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"iESIqZ0R0R0\":{\"uid\":\"iESIqZ0R0R0\",\"name\":\"Date of birth\",\"description\":\"Date of birth\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"n9nUvfpTsxQ\":{\"uid\":\"n9nUvfpTsxQ\",\"code\":\"Zip code\",\"name\":\"Zip code\",\"description\":\"Zip code\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"kyIzQsj96BD\":{\"uid\":\"kyIzQsj96BD\",\"code\":\"Company\",\"name\":\"Company\",\"description\":\"Company\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"A4xFHyieXys\":{\"uid\":\"A4xFHyieXys\",\"code\":\"Occupation\",\"name\":\"Occupation\",\"description\":\"Occupation\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"xs8A6tQJY0s\":{\"uid\":\"xs8A6tQJY0s\",\"name\":\"TB identifier\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"OvY4VVhSDeJ\":{\"uid\":\"OvY4VVhSDeJ\",\"name\":\"Weight in kg\",\"description\":\"Weight in kg\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ur1Edk5Oe2n\":{\"uid\":\"ur1Edk5Oe2n\",\"name\":\"TB program\"},\"RG7uGl4w5Jq\":{\"uid\":\"RG7uGl4w5Jq\",\"code\":\"Longitude\",\"name\":\"Longitude\",\"description\":\"Longitude\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"spFvx9FndA4\":{\"uid\":\"spFvx9FndA4\",\"name\":\"Age\",\"description\":\"Age\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"Agywv2JGwuq\":{\"uid\":\"Agywv2JGwuq\",\"code\":\"MMD_PER_MOB\",\"name\":\"Mobile number\",\"description\":\"Mobile number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"GUOBQt5K2WI\":{\"uid\":\"GUOBQt5K2WI\",\"code\":\"State\",\"name\":\"State\",\"description\":\"State\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"lZGmxYbs97q\":{\"uid\":\"lZGmxYbs97q\",\"code\":\"MMD_PER_ID\",\"name\":\"Unique ID\",\"description\":\"Unique identiifer\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"VqEFza8wbwA\":{\"uid\":\"VqEFza8wbwA\",\"code\":\"MMD_PER_ADR1\",\"name\":\"Address\",\"description\":\"Country\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"code\":\"MMD_PER_STA\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"w75KJ2mc4zz\":{\"uid\":\"w75KJ2mc4zz\",\"code\":\"MMD_PER_NAM\",\"name\":\"First name\",\"description\":\"First name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"KmEUg2hHEtx\":{\"uid\":\"KmEUg2hHEtx\",\"name\":\"Email address\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"G7vUx908SwP\":{\"uid\":\"G7vUx908SwP\",\"name\":\"Residence location\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"COORDINATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"o9odfev2Ty5\":{\"uid\":\"o9odfev2Ty5\",\"code\":\"Mother maiden name\",\"name\":\"Mother maiden name\",\"description\":\"Mother maiden name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"FO4sWYJ64LQ\":{\"uid\":\"FO4sWYJ64LQ\",\"code\":\"City\",\"name\":\"City\",\"description\":\"City\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ZkbAXlQUYJG\":{\"uid\":\"ZkbAXlQUYJG\",\"name\":\"TB visit\",\"description\":\"Routine TB visit\"},\"NDXw0cluzSw\":{\"uid\":\"NDXw0cluzSw\",\"name\":\"Email\",\"description\":\"Email address\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ruQQnf6rswq\":{\"uid\":\"ruQQnf6rswq\",\"name\":\"TB number\",\"description\":\"TB number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"cejWyOfXge6\":{\"uid\":\"cejWyOfXge6\",\"name\":\"Gender\",\"description\":\"Gender\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"P2cwLGskgxn\":{\"uid\":\"P2cwLGskgxn\",\"name\":\"Phone number\",\"description\":\"Phone number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"code\":\"MMD_PER_DOB\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ur1Edk5Oe2n.enrollmentdate\":{\"name\":\"Start of treatment date\",\"dimensionType\":\"PERIOD\"},\"uy2gU8kT1jF\":{\"uid\":\"uy2gU8kT1jF\",\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"VHfUeXpawmE\":{\"uid\":\"VHfUeXpawmE\",\"code\":\"Vehicle\",\"name\":\"Vehicle\",\"description\":\"Vehicle\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"AuPLng5hLbE\":{\"uid\":\"AuPLng5hLbE\",\"code\":\"National identifier\",\"name\":\"National identifier\",\"description\":\"National identifier\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ZcBPrXKahq2\":{\"uid\":\"ZcBPrXKahq2\",\"name\":\"Postal code\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"WSGAb5XwJ3Y\":{\"uid\":\"WSGAb5XwJ3Y\",\"name\":\"WHO RMNCH Tracker\"},\"H9IlTX2X6SL\":{\"uid\":\"H9IlTX2X6SL\",\"code\":\"Blood type\",\"name\":\"Blood type\",\"description\":\"Blood type\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"DODgdr5Oo2v\":[],\"Qo571yj6Zcn\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"A4xFHyieXys\":[],\"xs8A6tQJY0s\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"Agywv2JGwuq\":[],\"GUOBQt5K2WI\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[\"wfkKVdPBzho\",\"Yjte6foKMny\"],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ur1Edk5Oe2n.enrollmentdate",
        "Start of treatment date, TB program",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ZkbAXlQUYJG.eventstatus",
        "Event Status, TB program, TB visit",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 0, "ZkbAXlQUYJG.eventstatus", "COMPLETED");

    // Validate selected values for row index 1
    validateRowValueByName(response, actualHeaders, 1, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 1, "ZkbAXlQUYJG.eventstatus", "ACTIVE");

    // Validate selected values for row index 2
    validateRowValueByName(response, actualHeaders, 2, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 2, "ZkbAXlQUYJG.eventstatus", "ACTIVE");

    // Validate selected values for row index 3
    validateRowValueByName(response, actualHeaders, 3, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 3, "ZkbAXlQUYJG.eventstatus", "COMPLETED");
  }

  @Test
  public void stageAndUserOu() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=ur1Edk5Oe2n.enrollmentdate")
            .add("headers=ouname,ur1Edk5Oe2n.enrollmentdate,ZkbAXlQUYJG.ou,ZkbAXlQUYJG.ouname")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=10")
            .add("page=1")
            .add("relativePeriodDate=2026-01-30")
            .add("dimension=ZkbAXlQUYJG.ou:USER_ORGUNIT");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        4,
        4,
        4); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":10,\"isLastPage\":true},\"items\":{\"zDhUuAYrxNC\":{\"uid\":\"zDhUuAYrxNC\",\"name\":\"Last name\",\"description\":\"Last name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"lw1SqmMlnfh\":{\"uid\":\"lw1SqmMlnfh\",\"code\":\"Height in cm\",\"name\":\"Height in cm\",\"description\":\"Height in cm\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"fDd25txQckK\":{\"uid\":\"fDd25txQckK\",\"name\":\"Provider Follow-up and Support Tool\"},\"DODgdr5Oo2v\":{\"uid\":\"DODgdr5Oo2v\",\"code\":\"Provider ID\",\"name\":\"Provider ID\",\"description\":\"Provider ID\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"Qo571yj6Zcn\":{\"uid\":\"Qo571yj6Zcn\",\"code\":\"Latitude\",\"name\":\"Latitude\",\"description\":\"Latitude\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"iESIqZ0R0R0\":{\"uid\":\"iESIqZ0R0R0\",\"name\":\"Date of birth\",\"description\":\"Date of birth\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"n9nUvfpTsxQ\":{\"uid\":\"n9nUvfpTsxQ\",\"code\":\"Zip code\",\"name\":\"Zip code\",\"description\":\"Zip code\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"kyIzQsj96BD\":{\"uid\":\"kyIzQsj96BD\",\"code\":\"Company\",\"name\":\"Company\",\"description\":\"Company\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"A4xFHyieXys\":{\"uid\":\"A4xFHyieXys\",\"code\":\"Occupation\",\"name\":\"Occupation\",\"description\":\"Occupation\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"xs8A6tQJY0s\":{\"uid\":\"xs8A6tQJY0s\",\"name\":\"TB identifier\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"OvY4VVhSDeJ\":{\"uid\":\"OvY4VVhSDeJ\",\"name\":\"Weight in kg\",\"description\":\"Weight in kg\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ur1Edk5Oe2n\":{\"uid\":\"ur1Edk5Oe2n\",\"name\":\"TB program\"},\"RG7uGl4w5Jq\":{\"uid\":\"RG7uGl4w5Jq\",\"code\":\"Longitude\",\"name\":\"Longitude\",\"description\":\"Longitude\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"spFvx9FndA4\":{\"uid\":\"spFvx9FndA4\",\"name\":\"Age\",\"description\":\"Age\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"Agywv2JGwuq\":{\"uid\":\"Agywv2JGwuq\",\"code\":\"MMD_PER_MOB\",\"name\":\"Mobile number\",\"description\":\"Mobile number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"GUOBQt5K2WI\":{\"uid\":\"GUOBQt5K2WI\",\"code\":\"State\",\"name\":\"State\",\"description\":\"State\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"lZGmxYbs97q\":{\"uid\":\"lZGmxYbs97q\",\"code\":\"MMD_PER_ID\",\"name\":\"Unique ID\",\"description\":\"Unique identiifer\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ur1Edk5Oe2n.ZkbAXlQUYJG.ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"VqEFza8wbwA\":{\"uid\":\"VqEFza8wbwA\",\"code\":\"MMD_PER_ADR1\",\"name\":\"Address\",\"description\":\"Country\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"code\":\"MMD_PER_STA\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"w75KJ2mc4zz\":{\"uid\":\"w75KJ2mc4zz\",\"code\":\"MMD_PER_NAM\",\"name\":\"First name\",\"description\":\"First name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"KmEUg2hHEtx\":{\"uid\":\"KmEUg2hHEtx\",\"name\":\"Email address\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"G7vUx908SwP\":{\"uid\":\"G7vUx908SwP\",\"name\":\"Residence location\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"COORDINATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"o9odfev2Ty5\":{\"uid\":\"o9odfev2Ty5\",\"code\":\"Mother maiden name\",\"name\":\"Mother maiden name\",\"description\":\"Mother maiden name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"FO4sWYJ64LQ\":{\"uid\":\"FO4sWYJ64LQ\",\"code\":\"City\",\"name\":\"City\",\"description\":\"City\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ZkbAXlQUYJG\":{\"uid\":\"ZkbAXlQUYJG\",\"name\":\"TB visit\",\"description\":\"Routine TB visit\"},\"NDXw0cluzSw\":{\"uid\":\"NDXw0cluzSw\",\"name\":\"Email\",\"description\":\"Email address\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ruQQnf6rswq\":{\"uid\":\"ruQQnf6rswq\",\"name\":\"TB number\",\"description\":\"TB number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"cejWyOfXge6\":{\"uid\":\"cejWyOfXge6\",\"name\":\"Gender\",\"description\":\"Gender\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"P2cwLGskgxn\":{\"uid\":\"P2cwLGskgxn\",\"name\":\"Phone number\",\"description\":\"Phone number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"code\":\"MMD_PER_DOB\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ur1Edk5Oe2n.enrollmentdate\":{\"name\":\"Start of treatment date\",\"dimensionType\":\"PERIOD\"},\"uy2gU8kT1jF\":{\"uid\":\"uy2gU8kT1jF\",\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"VHfUeXpawmE\":{\"uid\":\"VHfUeXpawmE\",\"code\":\"Vehicle\",\"name\":\"Vehicle\",\"description\":\"Vehicle\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"AuPLng5hLbE\":{\"uid\":\"AuPLng5hLbE\",\"code\":\"National identifier\",\"name\":\"National identifier\",\"description\":\"National identifier\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ZcBPrXKahq2\":{\"uid\":\"ZcBPrXKahq2\",\"name\":\"Postal code\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"WSGAb5XwJ3Y\":{\"uid\":\"WSGAb5XwJ3Y\",\"name\":\"WHO RMNCH Tracker\"},\"H9IlTX2X6SL\":{\"uid\":\"H9IlTX2X6SL\",\"code\":\"Blood type\",\"name\":\"Blood type\",\"description\":\"Blood type\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"DODgdr5Oo2v\":[],\"Qo571yj6Zcn\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"A4xFHyieXys\":[],\"xs8A6tQJY0s\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"Agywv2JGwuq\":[],\"GUOBQt5K2WI\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[\"wfkKVdPBzho\",\"Yjte6foKMny\"],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ur1Edk5Oe2n.enrollmentdate",
        "Start of treatment date, TB program",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ZkbAXlQUYJG.ou",
        "Organisation unit, TB program, TB visit",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ZkbAXlQUYJG.ouname",
        "Organisation Unit Name, TB program, TB visit",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 0, "ZkbAXlQUYJG.ou", "DiszpKrYNg8");

    // Validate selected values for row index 1
    validateRowValueByName(response, actualHeaders, 1, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 1, "ZkbAXlQUYJG.ou", "DiszpKrYNg8");

    // Validate selected values for row index 2
    validateRowValueByName(response, actualHeaders, 2, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 2, "ZkbAXlQUYJG.ou", "DiszpKrYNg8");

    // Validate selected values for row index 3
    validateRowValueByName(response, actualHeaders, 3, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 3, "ZkbAXlQUYJG.ou", "DiszpKrYNg8");
  }

  @Test
  public void stageAndMultipleOus() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=ur1Edk5Oe2n.enrollmentdate")
            .add("headers=ouname,ur1Edk5Oe2n.enrollmentdate,ZkbAXlQUYJG.ou")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=10")
            .add("page=1")
            .add("relativePeriodDate=2026-01-30")
            .add("dimension=ZkbAXlQUYJG.ou:O6uvpzGd5pu;DiszpKrYNg8");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        4,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":10,\"isLastPage\":true},\"items\":{\"zDhUuAYrxNC\":{\"uid\":\"zDhUuAYrxNC\",\"name\":\"Last name\",\"description\":\"Last name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"lw1SqmMlnfh\":{\"uid\":\"lw1SqmMlnfh\",\"code\":\"Height in cm\",\"name\":\"Height in cm\",\"description\":\"Height in cm\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"O6uvpzGd5pu\":{\"uid\":\"O6uvpzGd5pu\",\"code\":\"OU_264\",\"name\":\"Bo\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"fDd25txQckK\":{\"uid\":\"fDd25txQckK\",\"name\":\"Provider Follow-up and Support Tool\"},\"DODgdr5Oo2v\":{\"uid\":\"DODgdr5Oo2v\",\"code\":\"Provider ID\",\"name\":\"Provider ID\",\"description\":\"Provider ID\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"Qo571yj6Zcn\":{\"uid\":\"Qo571yj6Zcn\",\"code\":\"Latitude\",\"name\":\"Latitude\",\"description\":\"Latitude\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"iESIqZ0R0R0\":{\"uid\":\"iESIqZ0R0R0\",\"name\":\"Date of birth\",\"description\":\"Date of birth\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"n9nUvfpTsxQ\":{\"uid\":\"n9nUvfpTsxQ\",\"code\":\"Zip code\",\"name\":\"Zip code\",\"description\":\"Zip code\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"kyIzQsj96BD\":{\"uid\":\"kyIzQsj96BD\",\"code\":\"Company\",\"name\":\"Company\",\"description\":\"Company\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"A4xFHyieXys\":{\"uid\":\"A4xFHyieXys\",\"code\":\"Occupation\",\"name\":\"Occupation\",\"description\":\"Occupation\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"xs8A6tQJY0s\":{\"uid\":\"xs8A6tQJY0s\",\"name\":\"TB identifier\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"OvY4VVhSDeJ\":{\"uid\":\"OvY4VVhSDeJ\",\"name\":\"Weight in kg\",\"description\":\"Weight in kg\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ur1Edk5Oe2n\":{\"uid\":\"ur1Edk5Oe2n\",\"name\":\"TB program\"},\"RG7uGl4w5Jq\":{\"uid\":\"RG7uGl4w5Jq\",\"code\":\"Longitude\",\"name\":\"Longitude\",\"description\":\"Longitude\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"spFvx9FndA4\":{\"uid\":\"spFvx9FndA4\",\"name\":\"Age\",\"description\":\"Age\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"Agywv2JGwuq\":{\"uid\":\"Agywv2JGwuq\",\"code\":\"MMD_PER_MOB\",\"name\":\"Mobile number\",\"description\":\"Mobile number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"GUOBQt5K2WI\":{\"uid\":\"GUOBQt5K2WI\",\"code\":\"State\",\"name\":\"State\",\"description\":\"State\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"DiszpKrYNg8\":{\"uid\":\"DiszpKrYNg8\",\"code\":\"OU_559\",\"name\":\"Ngelehun CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"lZGmxYbs97q\":{\"uid\":\"lZGmxYbs97q\",\"code\":\"MMD_PER_ID\",\"name\":\"Unique ID\",\"description\":\"Unique identiifer\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ur1Edk5Oe2n.ZkbAXlQUYJG.ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"VqEFza8wbwA\":{\"uid\":\"VqEFza8wbwA\",\"code\":\"MMD_PER_ADR1\",\"name\":\"Address\",\"description\":\"Country\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"code\":\"MMD_PER_STA\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"w75KJ2mc4zz\":{\"uid\":\"w75KJ2mc4zz\",\"code\":\"MMD_PER_NAM\",\"name\":\"First name\",\"description\":\"First name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"KmEUg2hHEtx\":{\"uid\":\"KmEUg2hHEtx\",\"name\":\"Email address\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"G7vUx908SwP\":{\"uid\":\"G7vUx908SwP\",\"name\":\"Residence location\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"COORDINATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"o9odfev2Ty5\":{\"uid\":\"o9odfev2Ty5\",\"code\":\"Mother maiden name\",\"name\":\"Mother maiden name\",\"description\":\"Mother maiden name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"FO4sWYJ64LQ\":{\"uid\":\"FO4sWYJ64LQ\",\"code\":\"City\",\"name\":\"City\",\"description\":\"City\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ZkbAXlQUYJG\":{\"uid\":\"ZkbAXlQUYJG\",\"name\":\"TB visit\",\"description\":\"Routine TB visit\"},\"NDXw0cluzSw\":{\"uid\":\"NDXw0cluzSw\",\"name\":\"Email\",\"description\":\"Email address\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ruQQnf6rswq\":{\"uid\":\"ruQQnf6rswq\",\"name\":\"TB number\",\"description\":\"TB number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"cejWyOfXge6\":{\"uid\":\"cejWyOfXge6\",\"name\":\"Gender\",\"description\":\"Gender\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"P2cwLGskgxn\":{\"uid\":\"P2cwLGskgxn\",\"name\":\"Phone number\",\"description\":\"Phone number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"code\":\"MMD_PER_DOB\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ur1Edk5Oe2n.enrollmentdate\":{\"name\":\"Start of treatment date\",\"dimensionType\":\"PERIOD\"},\"uy2gU8kT1jF\":{\"uid\":\"uy2gU8kT1jF\",\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"VHfUeXpawmE\":{\"uid\":\"VHfUeXpawmE\",\"code\":\"Vehicle\",\"name\":\"Vehicle\",\"description\":\"Vehicle\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"AuPLng5hLbE\":{\"uid\":\"AuPLng5hLbE\",\"code\":\"National identifier\",\"name\":\"National identifier\",\"description\":\"National identifier\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ZcBPrXKahq2\":{\"uid\":\"ZcBPrXKahq2\",\"name\":\"Postal code\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"WSGAb5XwJ3Y\":{\"uid\":\"WSGAb5XwJ3Y\",\"name\":\"WHO RMNCH Tracker\"},\"H9IlTX2X6SL\":{\"uid\":\"H9IlTX2X6SL\",\"code\":\"Blood type\",\"name\":\"Blood type\",\"description\":\"Blood type\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"DODgdr5Oo2v\":[],\"Qo571yj6Zcn\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"A4xFHyieXys\":[],\"xs8A6tQJY0s\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"Agywv2JGwuq\":[],\"GUOBQt5K2WI\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[\"wfkKVdPBzho\",\"Yjte6foKMny\"],\"ou\":[\"O6uvpzGd5pu\",\"DiszpKrYNg8\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ur1Edk5Oe2n.enrollmentdate",
        "Start of treatment date, TB program",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ZkbAXlQUYJG.ou",
        "Organisation unit, TB program, TB visit",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 0, "ZkbAXlQUYJG.ou", "DiszpKrYNg8");

    // Validate selected values for row index 1
    validateRowValueByName(response, actualHeaders, 1, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 1, "ZkbAXlQUYJG.ou", "DiszpKrYNg8");

    // Validate selected values for row index 2
    validateRowValueByName(response, actualHeaders, 2, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 2, "ZkbAXlQUYJG.ou", "DiszpKrYNg8");

    // Validate selected values for row index 3
    validateRowValueByName(response, actualHeaders, 3, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 3, "ZkbAXlQUYJG.ou", "DiszpKrYNg8");
  }

  @Test
  public void stageAndLevelOu() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=ur1Edk5Oe2n.enrollmentdate")
            .add("headers=ouname,ur1Edk5Oe2n.enrollmentdate,ZkbAXlQUYJG.ou")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=10")
            .add("page=1")
            .add("relativePeriodDate=2026-01-30")
            .add("dimension=ZkbAXlQUYJG.ou:LEVEL-tTUf91fCytl");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        4,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":10,\"isLastPage\":true},\"items\":{\"KXSqt7jv6DU\":{\"uid\":\"KXSqt7jv6DU\",\"code\":\"OU_222627\",\"name\":\"Gorama Mende\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"yu4N82FFeLm\":{\"uid\":\"yu4N82FFeLm\",\"code\":\"OU_204910\",\"name\":\"Mandu\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"vULnao2hV5v\":{\"uid\":\"vULnao2hV5v\",\"code\":\"OU_247086\",\"name\":\"Fakunya\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"U6Kr7Gtpidn\":{\"uid\":\"U6Kr7Gtpidn\",\"code\":\"OU_546\",\"name\":\"Kakua\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"hjpHnHZIniP\":{\"uid\":\"hjpHnHZIniP\",\"code\":\"OU_204887\",\"name\":\"Kissi Tongi\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"iUauWFeH8Qp\":{\"uid\":\"iUauWFeH8Qp\",\"code\":\"OU_197402\",\"name\":\"Bum\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"EYt6ThQDagn\":{\"uid\":\"EYt6ThQDagn\",\"code\":\"OU_222642\",\"name\":\"Koya (kenema)\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"BmYyh9bZ0sr\":{\"uid\":\"BmYyh9bZ0sr\",\"code\":\"OU_268197\",\"name\":\"Kafe Simira\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"pk7bUK5c1Uf\":{\"uid\":\"pk7bUK5c1Uf\",\"code\":\"OU_260397\",\"name\":\"Ya Kpukumu Krim\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"RG7uGl4w5Jq\":{\"uid\":\"RG7uGl4w5Jq\",\"code\":\"Longitude\",\"name\":\"Longitude\",\"description\":\"Longitude\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ERmBhYkhV6Y\":{\"uid\":\"ERmBhYkhV6Y\",\"code\":\"OU_204877\",\"name\":\"Njaluahun\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"daJPPxtIrQn\":{\"uid\":\"daJPPxtIrQn\",\"code\":\"OU_545\",\"name\":\"Jaiama Bongor\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"VqEFza8wbwA\":{\"uid\":\"VqEFza8wbwA\",\"code\":\"MMD_PER_ADR1\",\"name\":\"Address\",\"description\":\"Country\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"r1RUyfVBkLp\":{\"uid\":\"r1RUyfVBkLp\",\"code\":\"OU_268169\",\"name\":\"Sambaia Bendugu\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"NNE0YMCDZkO\":{\"uid\":\"NNE0YMCDZkO\",\"code\":\"OU_268225\",\"name\":\"Yoni\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"jWSIbtKfURj\":{\"uid\":\"jWSIbtKfURj\",\"code\":\"OU_222751\",\"name\":\"Langrama\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"CF243RPvNY7\":{\"uid\":\"CF243RPvNY7\",\"code\":\"OU_233359\",\"name\":\"Fiama\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"cM2BKSrj9F9\":{\"uid\":\"cM2BKSrj9F9\",\"code\":\"OU_204894\",\"name\":\"Luawa\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"I4jWcnFmgEC\":{\"uid\":\"I4jWcnFmgEC\",\"code\":\"OU_549\",\"name\":\"Niawa Lenga\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"jPidqyo7cpF\":{\"uid\":\"jPidqyo7cpF\",\"code\":\"OU_247049\",\"name\":\"Bagruwa\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"VHfUeXpawmE\":{\"uid\":\"VHfUeXpawmE\",\"code\":\"Vehicle\",\"name\":\"Vehicle\",\"description\":\"Vehicle\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"g8DdBm7EmUt\":{\"uid\":\"g8DdBm7EmUt\",\"code\":\"OU_197397\",\"name\":\"Sittia\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"ZiOVcrSjSYe\":{\"uid\":\"ZiOVcrSjSYe\",\"code\":\"OU_254976\",\"name\":\"Dibia\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"j43EZb15rjI\":{\"uid\":\"j43EZb15rjI\",\"code\":\"OU_193285\",\"name\":\"Sella Limba\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"kyIzQsj96BD\":{\"uid\":\"kyIzQsj96BD\",\"code\":\"Company\",\"name\":\"Company\",\"description\":\"Company\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"fwxkctgmffZ\":{\"uid\":\"fwxkctgmffZ\",\"code\":\"OU_268163\",\"name\":\"Kholifa Mabang\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"QwMiPiME3bA\":{\"uid\":\"QwMiPiME3bA\",\"code\":\"OU_260400\",\"name\":\"Kpanga Kabonde\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"ur1Edk5Oe2n\":{\"uid\":\"ur1Edk5Oe2n\",\"name\":\"TB program\"},\"eV4cuxniZgP\":{\"uid\":\"eV4cuxniZgP\",\"code\":\"OU_193224\",\"name\":\"Magbaimba Ndowahun\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"xhyjU2SVewz\":{\"uid\":\"xhyjU2SVewz\",\"code\":\"OU_268217\",\"name\":\"Tane\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"spFvx9FndA4\":{\"uid\":\"spFvx9FndA4\",\"name\":\"Age\",\"description\":\"Age\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"TA7NvKjsn4A\":{\"uid\":\"TA7NvKjsn4A\",\"code\":\"OU_255041\",\"name\":\"Bureh Kasseh Maconteh\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"myQ4q1W6B4y\":{\"uid\":\"myQ4q1W6B4y\",\"code\":\"OU_222731\",\"name\":\"Dama\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"tTUf91fCytl\":{\"name\":\"Chiefdom\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"Pc3JTyqnsmL\":{\"uid\":\"Pc3JTyqnsmL\",\"code\":\"OU_255020\",\"name\":\"Buya Romende\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"hdEuw2ugkVF\":{\"uid\":\"hdEuw2ugkVF\",\"code\":\"OU_222652\",\"name\":\"Lower Bambara\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"GE25DpSrqpB\":{\"uid\":\"GE25DpSrqpB\",\"code\":\"OU_204869\",\"name\":\"Malema\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"uy2gU8kT1jF\":{\"uid\":\"uy2gU8kT1jF\",\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"ARZ4y5i4reU\":{\"uid\":\"ARZ4y5i4reU\",\"code\":\"OU_553\",\"name\":\"Wonde\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"uKC54fzxRzO\":{\"uid\":\"uKC54fzxRzO\",\"code\":\"OU_222648\",\"name\":\"Niawa\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"PrJQHI6q7w2\":{\"uid\":\"PrJQHI6q7w2\",\"code\":\"OU_255061\",\"name\":\"Tainkatopa Makama Safrokoh\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"nOYt1LtFSyU\":{\"uid\":\"nOYt1LtFSyU\",\"code\":\"OU_247025\",\"name\":\"Bumpeh\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"xGMGhjA3y6J\":{\"uid\":\"xGMGhjA3y6J\",\"code\":\"OU_211262\",\"name\":\"Mambolo\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"lw1SqmMlnfh\":{\"uid\":\"lw1SqmMlnfh\",\"code\":\"Height in cm\",\"name\":\"Height in cm\",\"description\":\"Height in cm\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"e1eIKM1GIF3\":{\"uid\":\"e1eIKM1GIF3\",\"code\":\"OU_193215\",\"name\":\"Gbanti Kamaranka\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"lYIM1MXbSYS\":{\"uid\":\"lYIM1MXbSYS\",\"code\":\"OU_204920\",\"name\":\"Dea\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"d9iMR1MpuIO\":{\"uid\":\"d9iMR1MpuIO\",\"code\":\"OU_260410\",\"name\":\"Soro-Gbeima\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"DODgdr5Oo2v\":{\"uid\":\"DODgdr5Oo2v\",\"code\":\"Provider ID\",\"name\":\"Provider ID\",\"description\":\"Provider ID\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"n9nUvfpTsxQ\":{\"uid\":\"n9nUvfpTsxQ\",\"code\":\"Zip code\",\"name\":\"Zip code\",\"description\":\"Zip code\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"U09TSwIjG0s\":{\"uid\":\"U09TSwIjG0s\",\"code\":\"OU_222617\",\"name\":\"Nomo\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"zFDYIgyGmXG\":{\"uid\":\"zFDYIgyGmXG\",\"code\":\"OU_542\",\"name\":\"Bargbo\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"xIKjidMrico\":{\"uid\":\"xIKjidMrico\",\"code\":\"OU_247033\",\"name\":\"Kowa\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"pRHGAROvuyI\":{\"uid\":\"pRHGAROvuyI\",\"code\":\"OU_254960\",\"name\":\"Koya\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"GWTIxJO9pRo\":{\"uid\":\"GWTIxJO9pRo\",\"code\":\"OU_233355\",\"name\":\"Gorama Kono\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"HWjrSuoNPte\":{\"uid\":\"HWjrSuoNPte\",\"code\":\"OU_254999\",\"name\":\"Sanda Magbolonthor\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"UhHipWG7J8b\":{\"uid\":\"UhHipWG7J8b\",\"code\":\"OU_193191\",\"name\":\"Sanda Tendaren\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"rXLor9Knq6l\":{\"uid\":\"rXLor9Knq6l\",\"code\":\"OU_268212\",\"name\":\"Kunike Barina\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"kU8vhUkAGaT\":{\"uid\":\"kU8vhUkAGaT\",\"code\":\"OU_548\",\"name\":\"Lugbu\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"ur1Edk5Oe2n.ZkbAXlQUYJG.ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"iEkBZnMDarP\":{\"uid\":\"iEkBZnMDarP\",\"code\":\"OU_226253\",\"name\":\"Folosaba Dembelia\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"KmEUg2hHEtx\":{\"uid\":\"KmEUg2hHEtx\",\"name\":\"Email address\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"JsxnA2IywRo\":{\"uid\":\"JsxnA2IywRo\",\"code\":\"OU_204875\",\"name\":\"Kissi Kama\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"G7vUx908SwP\":{\"uid\":\"G7vUx908SwP\",\"name\":\"Residence location\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"COORDINATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"g5ptsn0SFX8\":{\"uid\":\"g5ptsn0SFX8\",\"code\":\"OU_233365\",\"name\":\"Sandor\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"pmxZm7klXBy\":{\"uid\":\"pmxZm7klXBy\",\"code\":\"OU_204924\",\"name\":\"Peje West\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"YuQRtpLP10I\":{\"uid\":\"YuQRtpLP10I\",\"code\":\"OU_539\",\"name\":\"Badjia\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"cejWyOfXge6\":{\"uid\":\"cejWyOfXge6\",\"name\":\"Gender\",\"description\":\"Gender\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"KSdZwrU7Hh6\":{\"uid\":\"KSdZwrU7Hh6\",\"code\":\"OU_204861\",\"name\":\"Jawi\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"ajILkI0cfxn\":{\"uid\":\"ajILkI0cfxn\",\"code\":\"OU_233390\",\"name\":\"Gbane\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"y5X4mP5XylL\":{\"uid\":\"y5X4mP5XylL\",\"code\":\"OU_211270\",\"name\":\"Tonko Limba\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"fwH9ipvXde9\":{\"uid\":\"fwH9ipvXde9\",\"code\":\"OU_193228\",\"name\":\"Biriwa\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"ZcBPrXKahq2\":{\"uid\":\"ZcBPrXKahq2\",\"name\":\"Postal code\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"EjnIQNVAXGp\":{\"uid\":\"EjnIQNVAXGp\",\"code\":\"OU_233344\",\"name\":\"Mafindor\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"x4HaBHHwBML\":{\"uid\":\"x4HaBHHwBML\",\"code\":\"OU_222672\",\"name\":\"Malegohun\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"EZPwuUTeIIG\":{\"uid\":\"EZPwuUTeIIG\",\"code\":\"OU_226258\",\"name\":\"Wara Wara Yagala\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"Zoy23SSHCPs\":{\"uid\":\"Zoy23SSHCPs\",\"code\":\"OU_233311\",\"name\":\"Gbane Kandor\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"fRLX08WHWpL\":{\"uid\":\"fRLX08WHWpL\",\"code\":\"OU_254982\",\"name\":\"Lokomasama\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"LsYpCyYxSLY\":{\"uid\":\"LsYpCyYxSLY\",\"code\":\"OU_247008\",\"name\":\"Kamaje\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"JdqfYTIFZXN\":{\"uid\":\"JdqfYTIFZXN\",\"code\":\"OU_254946\",\"name\":\"Maforki\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"EB1zRKdYjdY\":{\"uid\":\"EB1zRKdYjdY\",\"code\":\"OU_197429\",\"name\":\"Bendu Cha\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"Qo571yj6Zcn\":{\"uid\":\"Qo571yj6Zcn\",\"code\":\"Latitude\",\"name\":\"Latitude\",\"description\":\"Latitude\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"CG4QD1HC3h4\":{\"uid\":\"CG4QD1HC3h4\",\"code\":\"OU_197436\",\"name\":\"Yawbeko\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"A4xFHyieXys\":{\"uid\":\"A4xFHyieXys\",\"code\":\"Occupation\",\"name\":\"Occupation\",\"description\":\"Occupation\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"RndxKqQGzUl\":{\"uid\":\"RndxKqQGzUl\",\"code\":\"OU_247018\",\"name\":\"Dasse\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"YpVol7asWvd\":{\"uid\":\"YpVol7asWvd\",\"code\":\"OU_260417\",\"name\":\"Kpanga Krim\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"Agywv2JGwuq\":{\"uid\":\"Agywv2JGwuq\",\"code\":\"MMD_PER_MOB\",\"name\":\"Mobile number\",\"description\":\"Mobile number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"lZGmxYbs97q\":{\"uid\":\"lZGmxYbs97q\",\"code\":\"MMD_PER_ID\",\"name\":\"Unique ID\",\"description\":\"Unique identiifer\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"Mr4au3jR9bt\":{\"uid\":\"Mr4au3jR9bt\",\"code\":\"OU_226214\",\"name\":\"Dembelia Sinkunia\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"DmaLM8WYmWv\":{\"uid\":\"DmaLM8WYmWv\",\"code\":\"OU_233394\",\"name\":\"Nimikoro\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"BD9gU0GKlr2\":{\"uid\":\"BD9gU0GKlr2\",\"code\":\"OU_260378\",\"name\":\"Makpele\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"GFk45MOxzJJ\":{\"uid\":\"GFk45MOxzJJ\",\"code\":\"OU_226275\",\"name\":\"Neya\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"P2cwLGskgxn\":{\"uid\":\"P2cwLGskgxn\",\"name\":\"Phone number\",\"description\":\"Phone number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"A3Fh37HWBWE\":{\"uid\":\"A3Fh37HWBWE\",\"code\":\"OU_222687\",\"name\":\"Simbaru\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"Lt8U7GVWvSR\":{\"uid\":\"Lt8U7GVWvSR\",\"code\":\"OU_226263\",\"name\":\"Diang\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"JdhagCUEMbj\":{\"uid\":\"JdhagCUEMbj\",\"code\":\"OU_547\",\"name\":\"Komboya\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"PQZJPIpTepd\":{\"uid\":\"PQZJPIpTepd\",\"code\":\"OU_268150\",\"name\":\"Kholifa Rowalla\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"EVkm2xYcf6Z\":{\"uid\":\"EVkm2xYcf6Z\",\"code\":\"OU_268184\",\"name\":\"Malal Mara\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"WXnNDWTiE9r\":{\"uid\":\"WXnNDWTiE9r\",\"code\":\"OU_193239\",\"name\":\"Sanda Loko\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"lY93YpCxJqf\":{\"uid\":\"lY93YpCxJqf\",\"code\":\"OU_193249\",\"name\":\"Makari Gbanti\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"eROJsBwxQHt\":{\"uid\":\"eROJsBwxQHt\",\"code\":\"OU_222743\",\"name\":\"Gaura\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"DxAPPqXvwLy\":{\"uid\":\"DxAPPqXvwLy\",\"code\":\"OU_204929\",\"name\":\"Peje Bongre\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"PaqugoqjRIj\":{\"uid\":\"PaqugoqjRIj\",\"code\":\"OU_226225\",\"name\":\"Sulima (Koinadugu)\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"gy8rmvYT4cj\":{\"uid\":\"gy8rmvYT4cj\",\"code\":\"OU_247037\",\"name\":\"Ribbi\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"RzKeCma9qb1\":{\"uid\":\"RzKeCma9qb1\",\"code\":\"OU_260428\",\"name\":\"Barri\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"EfWCa0Cc8WW\":{\"uid\":\"EfWCa0Cc8WW\",\"code\":\"OU_255030\",\"name\":\"Masimera\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"AovmOHadayb\":{\"uid\":\"AovmOHadayb\",\"code\":\"OU_247044\",\"name\":\"Timidale\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"iESIqZ0R0R0\":{\"uid\":\"iESIqZ0R0R0\",\"name\":\"Date of birth\",\"description\":\"Date of birth\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"Jiyc4ekaMMh\":{\"uid\":\"Jiyc4ekaMMh\",\"code\":\"OU_247080\",\"name\":\"Kongbora\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"FlBemv1NfEC\":{\"uid\":\"FlBemv1NfEC\",\"code\":\"OU_211256\",\"name\":\"Masungbala\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"XrF5AvaGcuw\":{\"uid\":\"XrF5AvaGcuw\",\"code\":\"OU_226240\",\"name\":\"Wara Wara Bafodia\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"LhaAPLxdSFH\":{\"uid\":\"LhaAPLxdSFH\",\"code\":\"OU_233348\",\"name\":\"Lei\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"zSNUViKdkk3\":{\"uid\":\"zSNUViKdkk3\",\"code\":\"OU_260440\",\"name\":\"Kpaka\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"r06ohri9wA9\":{\"uid\":\"r06ohri9wA9\",\"code\":\"OU_211243\",\"name\":\"Samu\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"Z9QaI6sxTwW\":{\"uid\":\"Z9QaI6sxTwW\",\"code\":\"OU_247068\",\"name\":\"Kargboro\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"W5fN3G6y1VI\":{\"uid\":\"W5fN3G6y1VI\",\"code\":\"OU_247012\",\"name\":\"Lower Banta\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"o9odfev2Ty5\":{\"uid\":\"o9odfev2Ty5\",\"code\":\"Mother maiden name\",\"name\":\"Mother maiden name\",\"description\":\"Mother maiden name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ENHOJz3UH5L\":{\"uid\":\"ENHOJz3UH5L\",\"code\":\"OU_197440\",\"name\":\"BMC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"QywkxFudXrC\":{\"uid\":\"QywkxFudXrC\",\"code\":\"OU_211227\",\"name\":\"Magbema\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"ruQQnf6rswq\":{\"uid\":\"ruQQnf6rswq\",\"name\":\"TB number\",\"description\":\"TB number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"J4GiUImJZoE\":{\"uid\":\"J4GiUImJZoE\",\"code\":\"OU_226269\",\"name\":\"Nieni\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"ur1Edk5Oe2n.enrollmentdate\":{\"name\":\"Start of treatment date\",\"dimensionType\":\"PERIOD\"},\"BGGmAwx33dj\":{\"uid\":\"BGGmAwx33dj\",\"code\":\"OU_543\",\"name\":\"Bumpe Ngao\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"kvkDWg42lHR\":{\"uid\":\"kvkDWg42lHR\",\"code\":\"OU_233339\",\"name\":\"Kamara\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"iGHlidSFdpu\":{\"uid\":\"iGHlidSFdpu\",\"code\":\"OU_233317\",\"name\":\"Soa\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"vn9KJsLyP5f\":{\"uid\":\"vn9KJsLyP5f\",\"code\":\"OU_255005\",\"name\":\"Kaffu Bullom\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"QlCIp2S9NHs\":{\"uid\":\"QlCIp2S9NHs\",\"code\":\"OU_222682\",\"name\":\"Dodo\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"zDhUuAYrxNC\":{\"uid\":\"zDhUuAYrxNC\",\"name\":\"Last name\",\"description\":\"Last name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"fDd25txQckK\":{\"uid\":\"fDd25txQckK\",\"name\":\"Provider Follow-up and Support Tool\"},\"bQiBfA2j5cw\":{\"uid\":\"bQiBfA2j5cw\",\"code\":\"OU_204857\",\"name\":\"Penguia\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"NqWaKXcg01b\":{\"uid\":\"NqWaKXcg01b\",\"code\":\"OU_260384\",\"name\":\"Sowa\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"VP397wRvePm\":{\"uid\":\"VP397wRvePm\",\"code\":\"OU_197445\",\"name\":\"Nongoba Bullum\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"OvY4VVhSDeJ\":{\"uid\":\"OvY4VVhSDeJ\",\"name\":\"Weight in kg\",\"description\":\"Weight in kg\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"Qhmi8IZyPyD\":{\"uid\":\"Qhmi8IZyPyD\",\"code\":\"OU_193245\",\"name\":\"Tambaka\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"OTFepb1k9Db\":{\"uid\":\"OTFepb1k9Db\",\"code\":\"OU_226244\",\"name\":\"Mongo\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"DBs6e2Oxaj1\":{\"uid\":\"DBs6e2Oxaj1\",\"code\":\"OU_247002\",\"name\":\"Upper Banta\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"vWbkYPRmKyS\":{\"uid\":\"vWbkYPRmKyS\",\"code\":\"OU_540\",\"name\":\"Baoma\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"dGheVylzol6\":{\"uid\":\"dGheVylzol6\",\"code\":\"OU_541\",\"name\":\"Bargbe\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"npWGUj37qDe\":{\"uid\":\"npWGUj37qDe\",\"code\":\"OU_552\",\"name\":\"Valunia\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"nV3OkyzF4US\":{\"uid\":\"nV3OkyzF4US\",\"code\":\"OU_246991\",\"name\":\"Kori\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"X7dWcGerQIm\":{\"uid\":\"X7dWcGerQIm\",\"code\":\"OU_222677\",\"name\":\"Wandor\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"kbPmt60yi0L\":{\"uid\":\"kbPmt60yi0L\",\"code\":\"OU_211220\",\"name\":\"Bramaia\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"qIRCo0MfuGb\":{\"uid\":\"qIRCo0MfuGb\",\"code\":\"OU_211213\",\"name\":\"Gbinleh Dixion\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"eNtRuQrrZeo\":{\"uid\":\"eNtRuQrrZeo\",\"code\":\"OU_260420\",\"name\":\"Galliness Perri\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"ZkbAXlQUYJG\":{\"uid\":\"ZkbAXlQUYJG\",\"name\":\"TB visit\",\"description\":\"Routine TB visit\"},\"HV8RTzgcFH3\":{\"uid\":\"HV8RTzgcFH3\",\"code\":\"OU_197432\",\"name\":\"Kwamabai Krim\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"KKkLOTpMXGV\":{\"uid\":\"KKkLOTpMXGV\",\"code\":\"OU_193198\",\"name\":\"Bombali Sebora\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"l7pFejMtUoF\":{\"uid\":\"l7pFejMtUoF\",\"code\":\"OU_222634\",\"name\":\"Tunkia\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"K1r3uF6eZ8n\":{\"uid\":\"K1r3uF6eZ8n\",\"code\":\"OU_222725\",\"name\":\"Kandu Lepiema\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"VGAFxBXz16y\":{\"uid\":\"VGAFxBXz16y\",\"code\":\"OU_226231\",\"name\":\"Sengbeh\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"byp7w6Xd9Df\":{\"uid\":\"byp7w6Xd9Df\",\"code\":\"OU_204933\",\"name\":\"Yawei\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"BXJdOLvUrZB\":{\"uid\":\"BXJdOLvUrZB\",\"code\":\"OU_193277\",\"name\":\"Gbendembu Ngowahun\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"WSGAb5XwJ3Y\":{\"uid\":\"WSGAb5XwJ3Y\",\"name\":\"WHO RMNCH Tracker\"},\"TQkG0sX9nca\":{\"uid\":\"TQkG0sX9nca\",\"code\":\"OU_233375\",\"name\":\"Gbense\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"hRZOIgQ0O1m\":{\"uid\":\"hRZOIgQ0O1m\",\"code\":\"OU_193302\",\"name\":\"Libeisaygahun\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"USQdmvrHh1Q\":{\"uid\":\"USQdmvrHh1Q\",\"code\":\"OU_247055\",\"name\":\"Kaiyamba\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"sxRd2XOzFbz\":{\"uid\":\"sxRd2XOzFbz\",\"code\":\"OU_551\",\"name\":\"Tikonko\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"qgQ49DH9a0v\":{\"uid\":\"qgQ49DH9a0v\",\"code\":\"OU_233332\",\"name\":\"Nimiyama\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"M2qEv692lS6\":{\"uid\":\"M2qEv692lS6\",\"code\":\"OU_233324\",\"name\":\"Tankoro\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"qtr8GGlm4gg\":{\"uid\":\"qtr8GGlm4gg\",\"code\":\"OU_278366\",\"name\":\"Rural Western Area\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"GUOBQt5K2WI\":{\"uid\":\"GUOBQt5K2WI\",\"code\":\"State\",\"name\":\"State\",\"description\":\"State\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"C9uduqDZr9d\":{\"uid\":\"C9uduqDZr9d\",\"code\":\"OU_278311\",\"name\":\"Freetown\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"YmmeuGbqOwR\":{\"uid\":\"YmmeuGbqOwR\",\"code\":\"OU_544\",\"name\":\"Gbo\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"w75KJ2mc4zz\":{\"uid\":\"w75KJ2mc4zz\",\"code\":\"MMD_PER_NAM\",\"name\":\"First name\",\"description\":\"First name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"nlt6j60tCHF\":{\"uid\":\"nlt6j60tCHF\",\"code\":\"OU_260437\",\"name\":\"Mano Sakrim\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"FO4sWYJ64LQ\":{\"uid\":\"FO4sWYJ64LQ\",\"code\":\"City\",\"name\":\"City\",\"description\":\"City\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"XG8HGAbrbbL\":{\"uid\":\"XG8HGAbrbbL\",\"code\":\"OU_193267\",\"name\":\"Safroko Limba\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"NDXw0cluzSw\":{\"uid\":\"NDXw0cluzSw\",\"name\":\"Email\",\"description\":\"Email address\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"l0ccv2yzfF3\":{\"uid\":\"l0ccv2yzfF3\",\"code\":\"OU_268174\",\"name\":\"Kunike\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"LfTkc0S4b5k\":{\"uid\":\"LfTkc0S4b5k\",\"code\":\"OU_204915\",\"name\":\"Upper Bambara\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"vEvs2ckGNQj\":{\"uid\":\"vEvs2ckGNQj\",\"code\":\"OU_226219\",\"name\":\"Kasonko\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"KIUCimTXf8Q\":{\"uid\":\"KIUCimTXf8Q\",\"code\":\"OU_222690\",\"name\":\"Nongowa\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"XEyIRFd9pct\":{\"uid\":\"XEyIRFd9pct\",\"code\":\"OU_197413\",\"name\":\"Imperi\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"cgOy0hRMGu9\":{\"uid\":\"cgOy0hRMGu9\",\"code\":\"OU_197408\",\"name\":\"Sogbini\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"H9IlTX2X6SL\":{\"uid\":\"H9IlTX2X6SL\",\"code\":\"Blood type\",\"name\":\"Blood type\",\"description\":\"Blood type\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"N233eZJZ1bh\":{\"uid\":\"N233eZJZ1bh\",\"code\":\"OU_260388\",\"name\":\"Pejeh\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"smoyi1iYNK6\":{\"uid\":\"smoyi1iYNK6\",\"code\":\"OU_268191\",\"name\":\"Kalansogoia\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"DNRAeXT9IwS\":{\"uid\":\"DNRAeXT9IwS\",\"code\":\"OU_197421\",\"name\":\"Dema\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"xs8A6tQJY0s\":{\"uid\":\"xs8A6tQJY0s\",\"name\":\"TB identifier\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"P69SId31eDp\":{\"uid\":\"P69SId31eDp\",\"code\":\"OU_268202\",\"name\":\"Gbonkonlenken\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"aWQTfvgPA5v\":{\"uid\":\"aWQTfvgPA5v\",\"code\":\"OU_197424\",\"name\":\"Kpanda Kemoh\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"KctpIIucige\":{\"uid\":\"KctpIIucige\",\"code\":\"OU_550\",\"name\":\"Selenga\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"j0Mtr3xTMjM\":{\"uid\":\"j0Mtr3xTMjM\",\"code\":\"OU_204939\",\"name\":\"Kissi Teng\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"L8iA6eLwKNb\":{\"uid\":\"L8iA6eLwKNb\",\"code\":\"OU_193295\",\"name\":\"Paki Masabong\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"DfUfwjM9am5\":{\"uid\":\"DfUfwjM9am5\",\"code\":\"OU_260392\",\"name\":\"Malen\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"code\":\"MMD_PER_STA\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"FRxcUEwktoV\":{\"uid\":\"FRxcUEwktoV\",\"code\":\"OU_233314\",\"name\":\"Toli\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"VCtF1DbspR5\":{\"uid\":\"VCtF1DbspR5\",\"code\":\"OU_197386\",\"name\":\"Jong\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"code\":\"MMD_PER_DOB\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"vzup1f6ynON\":{\"uid\":\"vzup1f6ynON\",\"code\":\"OU_222619\",\"name\":\"Small Bo\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"AuPLng5hLbE\":{\"uid\":\"AuPLng5hLbE\",\"code\":\"National identifier\",\"name\":\"National identifier\",\"description\":\"National identifier\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"RWvG1aFrr0r\":{\"uid\":\"RWvG1aFrr0r\",\"code\":\"OU_255053\",\"name\":\"Marampa\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"DODgdr5Oo2v\":[],\"Qo571yj6Zcn\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"A4xFHyieXys\":[],\"xs8A6tQJY0s\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"Agywv2JGwuq\":[],\"GUOBQt5K2WI\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[\"wfkKVdPBzho\",\"Yjte6foKMny\"],\"ou\":[\"YuQRtpLP10I\",\"jPidqyo7cpF\",\"vWbkYPRmKyS\",\"dGheVylzol6\",\"zFDYIgyGmXG\",\"RzKeCma9qb1\",\"EB1zRKdYjdY\",\"fwH9ipvXde9\",\"ENHOJz3UH5L\",\"KKkLOTpMXGV\",\"kbPmt60yi0L\",\"iUauWFeH8Qp\",\"BGGmAwx33dj\",\"nOYt1LtFSyU\",\"TA7NvKjsn4A\",\"Pc3JTyqnsmL\",\"myQ4q1W6B4y\",\"RndxKqQGzUl\",\"lYIM1MXbSYS\",\"DNRAeXT9IwS\",\"Mr4au3jR9bt\",\"Lt8U7GVWvSR\",\"ZiOVcrSjSYe\",\"QlCIp2S9NHs\",\"vULnao2hV5v\",\"CF243RPvNY7\",\"iEkBZnMDarP\",\"C9uduqDZr9d\",\"eNtRuQrrZeo\",\"eROJsBwxQHt\",\"ajILkI0cfxn\",\"Zoy23SSHCPs\",\"e1eIKM1GIF3\",\"BXJdOLvUrZB\",\"TQkG0sX9nca\",\"qIRCo0MfuGb\",\"YmmeuGbqOwR\",\"P69SId31eDp\",\"GWTIxJO9pRo\",\"KXSqt7jv6DU\",\"XEyIRFd9pct\",\"daJPPxtIrQn\",\"KSdZwrU7Hh6\",\"VCtF1DbspR5\",\"BmYyh9bZ0sr\",\"vn9KJsLyP5f\",\"USQdmvrHh1Q\",\"U6Kr7Gtpidn\",\"smoyi1iYNK6\",\"LsYpCyYxSLY\",\"kvkDWg42lHR\",\"K1r3uF6eZ8n\",\"Z9QaI6sxTwW\",\"vEvs2ckGNQj\",\"fwxkctgmffZ\",\"PQZJPIpTepd\",\"JsxnA2IywRo\",\"j0Mtr3xTMjM\",\"hjpHnHZIniP\",\"JdhagCUEMbj\",\"Jiyc4ekaMMh\",\"nV3OkyzF4US\",\"xIKjidMrico\",\"pRHGAROvuyI\",\"EYt6ThQDagn\",\"zSNUViKdkk3\",\"aWQTfvgPA5v\",\"QwMiPiME3bA\",\"YpVol7asWvd\",\"l0ccv2yzfF3\",\"rXLor9Knq6l\",\"HV8RTzgcFH3\",\"jWSIbtKfURj\",\"LhaAPLxdSFH\",\"hRZOIgQ0O1m\",\"fRLX08WHWpL\",\"hdEuw2ugkVF\",\"W5fN3G6y1VI\",\"cM2BKSrj9F9\",\"kU8vhUkAGaT\",\"EjnIQNVAXGp\",\"JdqfYTIFZXN\",\"eV4cuxniZgP\",\"QywkxFudXrC\",\"lY93YpCxJqf\",\"BD9gU0GKlr2\",\"EVkm2xYcf6Z\",\"x4HaBHHwBML\",\"GE25DpSrqpB\",\"DfUfwjM9am5\",\"xGMGhjA3y6J\",\"yu4N82FFeLm\",\"nlt6j60tCHF\",\"RWvG1aFrr0r\",\"EfWCa0Cc8WW\",\"FlBemv1NfEC\",\"OTFepb1k9Db\",\"GFk45MOxzJJ\",\"uKC54fzxRzO\",\"I4jWcnFmgEC\",\"J4GiUImJZoE\",\"DmaLM8WYmWv\",\"qgQ49DH9a0v\",\"ERmBhYkhV6Y\",\"U09TSwIjG0s\",\"VP397wRvePm\",\"KIUCimTXf8Q\",\"L8iA6eLwKNb\",\"DxAPPqXvwLy\",\"pmxZm7klXBy\",\"N233eZJZ1bh\",\"bQiBfA2j5cw\",\"gy8rmvYT4cj\",\"qtr8GGlm4gg\",\"XG8HGAbrbbL\",\"r1RUyfVBkLp\",\"r06ohri9wA9\",\"WXnNDWTiE9r\",\"HWjrSuoNPte\",\"UhHipWG7J8b\",\"g5ptsn0SFX8\",\"KctpIIucige\",\"j43EZb15rjI\",\"VGAFxBXz16y\",\"A3Fh37HWBWE\",\"g8DdBm7EmUt\",\"vzup1f6ynON\",\"iGHlidSFdpu\",\"cgOy0hRMGu9\",\"d9iMR1MpuIO\",\"NqWaKXcg01b\",\"PaqugoqjRIj\",\"PrJQHI6q7w2\",\"Qhmi8IZyPyD\",\"xhyjU2SVewz\",\"M2qEv692lS6\",\"sxRd2XOzFbz\",\"AovmOHadayb\",\"FRxcUEwktoV\",\"y5X4mP5XylL\",\"l7pFejMtUoF\",\"LfTkc0S4b5k\",\"DBs6e2Oxaj1\",\"npWGUj37qDe\",\"X7dWcGerQIm\",\"XrF5AvaGcuw\",\"EZPwuUTeIIG\",\"ARZ4y5i4reU\",\"pk7bUK5c1Uf\",\"CG4QD1HC3h4\",\"byp7w6Xd9Df\",\"NNE0YMCDZkO\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ur1Edk5Oe2n.enrollmentdate",
        "Start of treatment date, TB program",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ZkbAXlQUYJG.ou",
        "Organisation unit, TB program, TB visit",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 0, "ZkbAXlQUYJG.ou", "DiszpKrYNg8");

    // Validate selected values for row index 1
    validateRowValueByName(response, actualHeaders, 1, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 1, "ZkbAXlQUYJG.ou", "DiszpKrYNg8");

    // Validate selected values for row index 2
    validateRowValueByName(response, actualHeaders, 2, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 2, "ZkbAXlQUYJG.ou", "DiszpKrYNg8");

    // Validate selected values for row index 3
    validateRowValueByName(response, actualHeaders, 3, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 3, "ZkbAXlQUYJG.ou", "DiszpKrYNg8");
  }
}
