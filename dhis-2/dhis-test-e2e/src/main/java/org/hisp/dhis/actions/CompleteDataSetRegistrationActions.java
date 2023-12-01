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
package org.hisp.dhis.actions;

import io.restassured.http.ContentType;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;

/**
 * @author david mackessy
 */
public class CompleteDataSetRegistrationActions extends RestApiActions {

  public CompleteDataSetRegistrationActions() {
    super("/completeDataSetRegistrations");
  }

  public ApiResponse sendAsync(String body) {
    QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder();
    queryParamsBuilder.addAll("async=true");

    return post("", body, queryParamsBuilder);
  }

  public ApiResponse sendSync(String body) {
    QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder();

    return post("", body, queryParamsBuilder);
  }

  public ApiResponse getCompletedAsMediaType(
      String dataSet, String orgUnit, String period, ContentType mediaType) {
    QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder();
    addRequiredParams(queryParamsBuilder, dataSet, orgUnit, period);
    String contentType = mediaType != null ? mediaType.toString() : "application/json";
    return get("", "", contentType, queryParamsBuilder);
  }

  public ApiResponse getCompletedWithIdScheme(
      String dataSet, String orgUnit, String period, String idScheme) {
    QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder();
    addRequiredParams(queryParamsBuilder, dataSet, orgUnit, period);
    queryParamsBuilder.add("idScheme", idScheme);
    return get(queryParamsBuilder);
  }

  private void addRequiredParams(
      QueryParamsBuilder builder, String dataSet, String orgUnit, String period) {
    builder.add("dataSet", dataSet);
    builder.add("orgUnit", orgUnit);
    builder.add("period", period);
  }
}
