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
package org.hisp.dhis.analytics.enrollment.query;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsEnrollmentsActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.Test;

public class EnrollmentsQueryDownloadTest extends AnalyticsApiTest {
  private AnalyticsEnrollmentsActions analyticsEnrollmentsActions =
      new AnalyticsEnrollmentsActions();

  @Test
  void queryWithXlsxDownload() {
    // Given
    final String TYPE = "application/vnd.ms-excel";
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=pe:LAST_12_MONTHS,ou:ImspTQPwCqd,A03MvHHogjR.UXz7xuGCEhU")
            .add("stage=A03MvHHogjR")
            .add("displayProperty=NAME")
            .add("outputType=ENROLLMENT")
            .add("asc=A03MvHHogjR.UXz7xuGCEhU,lastupdated")
            .add("totalPages=false")
            .add("pageSize=100")
            .add("page=1")
            .add("relativePeriodDate=2022-09-27");

    // When
    ApiResponse response =
        analyticsEnrollmentsActions.query().get("IpHINAT79UW.xlsx", TYPE, TYPE, params);

    // Then
    response.validate().statusCode(200).contentType(TYPE);

    assertTrue(isNotBlank(response.getAsString()));
  }
}
