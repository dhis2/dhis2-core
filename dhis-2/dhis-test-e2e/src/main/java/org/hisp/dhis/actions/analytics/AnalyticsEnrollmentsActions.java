/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.actions.analytics;

import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class AnalyticsEnrollmentsActions extends RestApiActions {
  public AnalyticsEnrollmentsActions() {
    super("/analytics/enrollments");
  }

  public AnalyticsEnrollmentsActions(String endpoint) {
    super("/analytics/enrollments" + endpoint);
  }

  public AnalyticsEnrollmentsActions query() {
    return new AnalyticsEnrollmentsActions("/query");
  }

  public AnalyticsEnrollmentsActions aggregate() {
    return new AnalyticsEnrollmentsActions("/aggregate");
  }

  public ApiResponse getDimensions(String programId) {
    return this.get("/dimensions", new QueryParamsBuilder().add("programId", programId))
        .validateStatus(200);
  }

  public ApiResponse getDimensions(String programId, QueryParamsBuilder queryParamsBuilder) {
    queryParamsBuilder.add("programId", programId);
    return this.get("/dimensions", queryParamsBuilder).validateStatus(200);
  }

  public ApiResponse getDimensionsByDimensionType(String programId, String dimensionType) {
    return this.get(
            "/dimensions",
            new QueryParamsBuilder()
                .add("programId", programId)
                .add("filter", "dimensionType:eq:" + dimensionType))
        .validateStatus(200);
  }
}
