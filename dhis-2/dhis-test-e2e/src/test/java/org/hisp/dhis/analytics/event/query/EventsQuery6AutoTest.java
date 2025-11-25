/*
 * Copyright (c) 2004-2023, University of Oslo
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

import static org.hisp.dhis.analytics.ValidationHelper.setRowData;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeaderPropertiesByName;
import static org.hisp.dhis.analytics.ValidationHelper.validateResponseStructure;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.analytics.ValidationHelper;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsEventActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/events/query" endpoint. */
public class EventsQuery6AutoTest extends AnalyticsApiTest {
  private final AnalyticsEventActions actions = new AnalyticsEventActions();

  @Test
  public void stageAndEventDateThisYear() throws JSONException {

      boolean expectPostgis = isPostgres();

      // Given
      QueryParamsBuilder params =
              new QueryParamsBuilder()
                      //.add("stage=Zj7UnCAulEk")
                      .add("displayProperty=NAME")
                      .add("outputType=EVENT")
                      .add("pageSize=100")
                      .add("page=1")
                      .add("dimension=Zj7UnCAulEk.EVENT_DATE:THIS_YEAR")
                      .add("desc=eventdate,lastupdated")
                      .add("relativePeriodDate=2022-12-31");

      // When
      ApiResponse response = actions.query().get("eBAyeGv0exc", JSON, JSON, params);

      // where:
      // (ax."occurreddate" >= '2022-01-01' and ax."occurreddate" <= '2022-12-31' and ax."ps" = 'Zj7UnCAulEk')
  }

    @Test
    public void stageAndEventDateSpecificYear() throws JSONException {

        boolean expectPostgis = isPostgres();

        // Given
        QueryParamsBuilder params =
                new QueryParamsBuilder()
                        //.add("stage=Zj7UnCAulEk")
                        .add("displayProperty=NAME")
                        .add("outputType=EVENT")
                        .add("pageSize=100")
                        .add("page=1")
                        .add("dimension=Zj7UnCAulEk.EVENT_DATE:2021")
                        .add("desc=eventdate,lastupdated");

        // When
        ApiResponse response = actions.query().get("eBAyeGv0exc", JSON, JSON, params);

        // where:
        // (((ax."occurreddate" >= '2024-11-01' and ax."occurreddate" < '2025-11-01'))) <- error
        // and ax."uidlevel1" in ('ImspTQPwCqd')
        // and (ax."occurreddate" >= '2021-01-01' and ax."occurreddate" <= '2021-12-31' and ax."ps" = 'Zj7UnCAulEk')

    }

    @Test
    public void stageAndEventDateRange() throws JSONException {

        boolean expectPostgis = isPostgres();

        // Given
        QueryParamsBuilder params =
                new QueryParamsBuilder()
                        //.add("stage=Zj7UnCAulEk")
                        .add("displayProperty=NAME")
                        .add("outputType=EVENT")
                        .add("pageSize=100")
                        .add("page=1")
                        .add("dimension=Zj7UnCAulEk.EVENT_DATE:2021-03-01_2021-05-31")
                        .add("desc=eventdate,lastupdated");

        // When
        ApiResponse response = actions.query().get("eBAyeGv0exc", JSON, JSON, params);
    }

    @Test
    public void stageAndEventGreaterThan() throws JSONException {

        boolean expectPostgis = isPostgres();

        // Given
        QueryParamsBuilder params =
                new QueryParamsBuilder()
                        //.add("stage=Zj7UnCAulEk")
                        .add("displayProperty=NAME")
                        .add("outputType=EVENT")
                        .add("pageSize=100")
                        .add("page=1")
                        .add("dimension=Zj7UnCAulEk.EVENT_DATE:GT:2023-05-01")
                        .add("desc=eventdate,lastupdated");

        // When
        ApiResponse response = actions.query().get("eBAyeGv0exc", JSON, JSON, params);
    }

    @Test
    public void stageAndEventLowerThan() throws JSONException {

        boolean expectPostgis = isPostgres();

        // Given
        QueryParamsBuilder params =
                new QueryParamsBuilder()
                        //.add("stage=Zj7UnCAulEk")
                        .add("displayProperty=NAME")
                        .add("outputType=EVENT")
                        .add("pageSize=100")
                        .add("page=1")
                        .add("dimension=Zj7UnCAulEk.EVENT_DATE:LE:2023-05-01")
                        .add("desc=eventdate,lastupdated");

        // When
        ApiResponse response = actions.query().get("eBAyeGv0exc", JSON, JSON, params);
    }


  private void dumpHeaders(List<Map<String, Object>> actualHeaders) {
    // Utility method to help debug test failures
    for (int i = 0; i < actualHeaders.size(); i++) {
      Map<String, Object> header = actualHeaders.get(i);
      System.out.println(i + ": " + header.get("name") + " (" + header.get("type") + ")");
    }
  }
}
