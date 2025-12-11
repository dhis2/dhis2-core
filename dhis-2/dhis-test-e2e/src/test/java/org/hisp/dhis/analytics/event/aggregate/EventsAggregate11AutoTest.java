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
package org.hisp.dhis.analytics.event.aggregate;

import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsEventActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/events/aggregate" endpoint. */
public class EventsAggregate11AutoTest extends AnalyticsApiTest {
  private final AnalyticsEventActions actions = new AnalyticsEventActions();

  @Test
  public void stageAndEventDateThisYear() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add("dimension=Zj7UnCAulEk.EVENT_DATE:THIS_YEAR")
            .add("dimension=pe:LAST_YEAR")
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);
  }

  @Test
  public void stageAndEventDateSpecificYear() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add("dimension=Zj7UnCAulEk.EVENT_DATE:2021")
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // sql:
    //        and (ax."occurreddate" >= '2021-01-01'
    //                and ax."occurreddate" <= '2021-12-31'
    //                and ax."ps" = 'Zj7UnCAulEk')
  }

  @Test
  public void stageAndEventDateRange() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add("dimension=Zj7UnCAulEk.EVENT_DATE:2021-03-01_2021-05-31")
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // sql:
    //        and (ax."occurreddate" >= '2021-01-01'
    //                and ax."occurreddate" <= '2021-12-31'
    //                and ax."ps" = 'Zj7UnCAulEk')
  }

  @Test
  public void stageAndEventGreaterThan() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add("dimension=Zj7UnCAulEk.EVENT_DATE:GT:2023-05-01")
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // sql:
    //        and (ax."occurreddate" > '2023-05-01'
    //                and ax."ps" = 'Zj7UnCAulEk')
  }

  @Test
  public void stageAndEventLowerThan() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add("dimension=Zj7UnCAulEk.EVENT_DATE:LE:2023-05-01")
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // sql:
    //        and (ax."occurreddate" <= '2023-05-01'
    //                and ax."ps" = 'Zj7UnCAulEk')
  }

  @Test
  public void stageAndEventDateMultipleStages() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add("dimension=ZkbAXlQUYJG.EVENT_DATE:THIS_YEAR")
            .add("dimension=jdRD35YwbRH.EVENT_DATE:2023")
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.aggregate().get("ur1Edk5Oe2n", JSON, JSON, params);

    // sql:
    //        and (ax."occurreddate" >= '2023-01-01'
    //                and ax."occurreddate" <= '2023-12-31'
    //                and ax."ps" = 'ZkbAXlQUYJG')
    //        and (ax."occurreddate" >= '2023-01-01'
    //                and ax."occurreddate" <= '2023-12-31'
    //                and ax."ps" = 'jdRD35YwbRH')

  }

  @Test
  public void stageAndInvalidOu() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add("dimension=ZkbAXlQUYJG.ou:THIS_YEAR") // TODO this should fail
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);
  }

  @Test
  public void stageAndSimpleOu() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add("dimension=pe:2023")
            .add("dimension=ZkbAXlQUYJG.ou:ImspTQPwCqd")
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.aggregate().get("ur1Edk5Oe2n", JSON, JSON, params);

    // sql:
    //        and (ax."uidlevel1" in ('ImspTQPwCqd')
    //                and ax."ps" = 'ZkbAXlQUYJG')
  }

  @Test
  public void stageAndOuUserOrgUnit() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add("dimension=pe:2023")
            .add("dimension=ZkbAXlQUYJG.ou:USER_ORGUNIT")
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.aggregate().get("ur1Edk5Oe2n", JSON, JSON, params);

    // sql:
    //        and (ax."uidlevel1" in ('ImspTQPwCqd')
    //                and ax."ps" = 'ZkbAXlQUYJG')
  }

  @Test
  public void stageAndOuUserLevel() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add("dimension=pe:2023")
            .add("dimension=ZkbAXlQUYJG.ou:LEVEL-3")
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.aggregate().get("ur1Edk5Oe2n", JSON, JSON, params);

    // sql:
    //        and (ax."uidlevel3" in ('EYt6ThQDagn', 'DNRAeXT9IwS', ...)
    //                and ax."ps" = 'ZkbAXlQUYJG')
  }

  @Test
  public void stageAndOuMultipleOus() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add("dimension=pe:2023")
            .add("dimension=ZkbAXlQUYJG.ou:WjO2puYKysP;eIQbndfxQMb")
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.aggregate().get("ur1Edk5Oe2n", JSON, JSON, params);

    // sql:
    //        and (ax."uidlevel2" in ('eIQbndfxQMb')
    //                and ax."uidlevel4" in ('WjO2puYKysP')
    //                and ax."ps" = 'ZkbAXlQUYJG')
  }

  @Test
  public void stageAndScheduledDate() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            // .add("dimension=pe:2023") // TODO this should throw error
            .add("dimension=ZkbAXlQUYJG.SCHEDULED_DATE:201910")
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.aggregate().get("ur1Edk5Oe2n", JSON, JSON, params);

    // sql:
    //        and (ax."scheduleddate" >= '2019-10-01'
    //                and ax."scheduleddate" <= '2019-10-31'
    //                and ax."ps" = 'ZkbAXlQUYJG')
  }

  @Test
  public void stageAndStatus() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            // .add("dimension=pe:2023") // TODO this should throw error
            .add("dimension=ZkbAXlQUYJG.EVENT_STATUS:ACTIVE")
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.aggregate().get("ur1Edk5Oe2n", JSON, JSON, params);

    // sql:
    // and (ax."eventstatus" in ('ACTIVE')
    //		and ax."ps" = 'ZkbAXlQUYJG')
  }
}
