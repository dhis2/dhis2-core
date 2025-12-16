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
package org.hisp.dhis.analytics.enrollment.query;

import static org.hisp.dhis.analytics.ValidationHelper.*;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsEnrollmentsActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Groups e2e tests for "/enrollments/query" endpoint. */
public class EnrollmentsQuery7AutoTest extends AnalyticsApiTest {
  private final AnalyticsEnrollmentsActions actions = new AnalyticsEnrollmentsActions();

    @Test
    public void stageAndEventDate2021() throws JSONException {
        // Read the 'expect.postgis' system property at runtime to adapt assertions.
        boolean expectPostgis = isPostgres();

        // Given
        QueryParamsBuilder params = new QueryParamsBuilder().add("includeMetadataDetails=true")
                .add("headers=ouname,enrollmentdate,")
                .add("displayProperty=NAME")
                .add("totalPages=false")
                .add("outputType=ENROLLMENT")
                .add("pageSize=100")
                .add("page=1")
                .add("dimension=ou:ImspTQPwCqd,A03MvHHogjR.EVENT_DATE:2021")
                .add("desc=enrollmentdate")
                ;

        // When
        ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

        // Then
        // 1. Validate Response Structure (Counts, Headers, Height/Width)
        //    This helper checks basic counts and dimensions, adapting based on the runtime 'expectPostgis' flag.
        //validateResponseStructure(response, expectPostgis, 100, 5, 5); // Pass runtime flag, row count, and expected header counts

        // 2. Extract Headers into a List of Maps for easy access by name
        List<Map<String, Object>> actualHeaders = response.extractList("headers", Map.class).stream()
                .map(obj -> (Map<String, Object>) obj) // Ensure correct type
                .collect(Collectors.toList());


        // 3. Assert metaData.
        String expectedMetaData = "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":false},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"A03MvHHogjR.EVENT_DATE\":{\"name\":\"Report date, Birth\"}},\"dimensions\":{\"A03MvHHogjR.occurreddate\":[],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
        String actualMetaData = new JSONObject((Map)response.extract("metaData")).toString();
        assertEquals(expectedMetaData, actualMetaData, false);

        // 4. Validate Headers By Name (conditionally checking PostGIS headers).
        validateHeaderPropertiesByName(response, actualHeaders,"ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
        validateHeaderPropertiesByName(response, actualHeaders,"enrollmentdate", "Date of enrollment", "DATETIME", "java.time.LocalDateTime", false, true);

        // Assert PostGIS-specific headers DO NOT exist if 'expectPostgis' is false
        if (!expectPostgis) {
            validateHeaderExistence(actualHeaders, "geometry", false);
            validateHeaderExistence(actualHeaders, "longitude", false);
            validateHeaderExistence(actualHeaders, "latitude", false);
        }

        // rowContext not found or empty in the response, skipping assertions.

        // 7. Assert row values by name (sample validation: first/last row, key columns).
        // Validate selected values for row index 0
        validateRowValueByName(response, actualHeaders, 0, "ouname", "Mambiama CHP");
        validateRowValueByName(response, actualHeaders, 0, "enrollmentdate", "2022-12-29 12:05:00.0");

        // Validate selected values for row index 99
        validateRowValueByName(response, actualHeaders, 99, "ouname", "Geima CHP");
        validateRowValueByName(response, actualHeaders, 99, "enrollmentdate", "2022-12-26 12:05:00.0");
    }


  @Test
  public void queryRandom2() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("headers=ouname,enrollmentdate,uIuxlbV1vRT,Bpx0589u8y0")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("enrollmentDate=2021")
            .add("outputType=ENROLLMENT")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=ou:ImspTQPwCqd")
            .add("dimension=A03MvHHogjR.SCHEDULED_DATE:THIS_YEAR")
            // .add("relativePeriodDate=2023-07-14")
            .add("desc=enrollmentdate");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    /**
     * query with A03MvHHogjR_scheduleddate as ( select enrollment, "scheduleddate" as value,
     * row_number() over ( partition by enrollment order by occurreddate desc, created desc ) as rn
     * from analytics_event_IpHINAT79UW where eventstatus != 'SCHEDULE' and ps = 'A03MvHHogjR' and
     * "scheduleddate" >= '2025-01-01' and "scheduleddate" <= '2025-12-31' ) select ax.enrollment,
     * ... ax."ou", trsqu_0.value as "A03MvHHogjR.scheduleddate" from
     * analytics_enrollment_iphinat79uw as ax left join A03MvHHogjR_scheduleddate trsqu_0 on
     * trsqu_0.enrollment = ax.enrollment and trsqu_0.rn = 1 where (ax."uidlevel1" = 'ImspTQPwCqd' )
     * and trsqu_0.value >= '2025-01-01' and trsqu_0.value <= '2025-12-31' order by "enrollmentdate"
     * desc nulls last limit 101 offset 0
     */
  }

  @Test
  public void queryRandom3() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("headers=ouname,enrollmentdate,uIuxlbV1vRT,Bpx0589u8y0")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("enrollmentDate=2021")
            .add("outputType=ENROLLMENT")
            .add("pageSize=100")
            .add("page=1")
            // .add("dimension=ou:ImspTQPwCqd")
            .add("dimension=ZkbAXlQUYJG.ou:ImspTQPwCqd,pe:2022")
            // .add("relativePeriodDate=2023-07-14")
            .add("desc=enrollmentdate");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    /**
     * query with A03MvHHogjR_scheduleddate as ( select enrollment, "scheduleddate" as value,
     * row_number() over ( partition by enrollment order by occurreddate desc, created desc ) as rn
     * from analytics_event_IpHINAT79UW where eventstatus != 'SCHEDULE' and ps = 'A03MvHHogjR' and
     * "scheduleddate" >= '2025-01-01' and "scheduleddate" <= '2025-12-31' ) select ax.enrollment,
     * ... ax.enrollmentstatus, ax."ou", trsqu_0.value as "A03MvHHogjR.scheduleddate" from
     * analytics_enrollment_iphinat79uw as ax left join A03MvHHogjR_scheduleddate trsqu_0 on
     * trsqu_0.enrollment = ax.enrollment and trsqu_0.rn = 1 where (ax."uidlevel1" = 'ImspTQPwCqd' )
     * and trsqu_0.value >= '2025-01-01' and trsqu_0.value <= '2025-12-31' order by "enrollmentdate"
     * desc nulls last limit 101 offset 0
     */
  }

  @Test
  public void queryRandom4() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("headers=ouname,enrollmentdate,uIuxlbV1vRT,Bpx0589u8y0")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("enrollmentDate=2021")
            .add("outputType=ENROLLMENT")
            .add("pageSize=100")
            .add("page=1")
            // .add("dimension=ou:ImspTQPwCqd")
            .add("dimension=ZkbAXlQUYJG.EVENT_STATUS:ACTIVE,pe:2022")
            // .add("relativePeriodDate=2023-07-14")
            .add("desc=enrollmentdate");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    /**
     * query with ZkbAXlQUYJG_eventstatus as ( select enrollment, "eventstatus" as value,
     * row_number() over ( partition by enrollment order by occurreddate desc, created desc ) as rn
     * from analytics_event_IpHINAT79UW where eventstatus != 'SCHEDULE' and ps = 'ZkbAXlQUYJG' and
     * "eventstatus" in ('ACTIVE') ) select ax.enrollment, .. ax.enrollmentstatus, rlzhw_0.value as
     * "ZkbAXlQUYJG.eventstatus" from analytics_enrollment_iphinat79uw as ax left join
     * ZkbAXlQUYJG_eventstatus rlzhw_0 on rlzhw_0.enrollment = ax.enrollment and rlzhw_0.rn = 1
     * where (((enrollmentdate >= '2022-01-01' and enrollmentdate < '2023-01-01') or (enrollmentdate
     * >= '2021-01-01' and enrollmentdate < '2022-01-01'))) and (ax."uidlevel1" = 'ImspTQPwCqd' )
     * and rlzhw_0.value in ('ACTIVE') order by "enrollmentdate" desc nulls last limit 101 offset 0
     */
  }
}
