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
package org.hisp.dhis.analytics.tei;

import static io.restassured.http.ContentType.HTML;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.actions.analytics.AnalyticsTeiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.Test;

/**
 * Groups e2e tests for Tracked Entities "/query" endpoint for download.
 *
 * @author maikel arabori
 */
public class TrackedEntityQueryDownloadTest extends AnalyticsApiTest {
  private AnalyticsTeiActions analyticsTeiActions = new AnalyticsTeiActions();

  @Test
  void queryWithHtmlDownload() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=ou:ImspTQPwCqd")
            .add("program=IpHINAT79UW")
            .add("relativePeriodDate=2016-01-01");

    // When
    ApiResponse response =
        analyticsTeiActions
            .query()
            .get("nEenWmSyUEp.html", HTML.toString(), HTML.toString(), params);

    // Then
    response.validate().statusCode(200).contentType(HTML);

    assertTrue(isNotBlank(response.getAsString()));
  }

  @Test
  void queryWithHtmlCssDownload() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=ou:ImspTQPwCqd")
            .add("program=IpHINAT79UW")
            .add("relativePeriodDate=2016-01-01");

    // When
    ApiResponse response =
        analyticsTeiActions
            .query()
            .get("nEenWmSyUEp.html+css", HTML.toString(), HTML.toString(), params);

    // Then
    response.validate().statusCode(200).contentType(HTML);

    assertTrue(isNotBlank(response.getAsString()));
  }

  @Test
  void queryWithXlsDownload() {
    // Given
    final String TYPE = "application/vnd.ms-excel";
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=ou:ImspTQPwCqd")
            .add("program=IpHINAT79UW")
            .add("relativePeriodDate=2016-01-01");

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp.xls", TYPE, TYPE, params);

    // Then
    response.validate().statusCode(200).contentType(TYPE);

    assertTrue(isNotBlank(response.getAsString()));
  }

  @Test
  void queryWithCsvDownload() {
    // Given
    final String TYPE = "application/csv";
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=ou:ImspTQPwCqd")
            .add("program=IpHINAT79UW")
            .add("relativePeriodDate=2016-01-01");

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp.csv", TYPE, TYPE, params);

    // Then
    response.validate().statusCode(200).contentType(TYPE);

    assertTrue(isNotBlank(response.getAsString()));
  }

  @Test
  void queryWithXmlDownload() {
    // Given
    final String TYPE = "application/xml";
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=ou:ImspTQPwCqd")
            .add("program=IpHINAT79UW")
            .add("relativePeriodDate=2016-01-01");

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp.xml", TYPE, TYPE, params);

    // Then
    response.validate().statusCode(200).contentType(TYPE);

    assertTrue(isNotBlank(response.getAsString()));
  }
}
