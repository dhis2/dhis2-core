/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.junit.jupiter.api.Assertions.*;

import org.hisp.dhis.http.HttpClientAdapter;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;

class DataSummaryControllerTest extends PostgresControllerIntegrationTestBase {

  @Test
  void canGetPrometheusMetrics() {
    // Send the GET request and check the status
    HttpResponse response = GET("/api/dataSummary/metrics", HttpClientAdapter.Accept("text/plain"));
    assertEquals(HttpStatus.OK, response.status());

    // Extract the response content
    String content = response.content("text/plain");
    assertFalse(content.isEmpty(), "Response content should not be empty");

    // Verify the presence of system information metrics
    assertTrue(
        content.contains("# HELP data_summary_system_info DHIS2 System information"),
        "System information help text is missing");
    assertTrue(
        content.contains("data_summary_system_info"),
        "Build time metrics are missing");

    // Verify active users metrics
    assertTrue(
        content.contains("# HELP data_summary_active_users Active users over days"),
        "Active users help text is missing");
    assertTrue(content.contains("data_summary_active_user"), "Active users metric is missing");

    // Verify object counts metrics
    assertTrue(
        content.contains("# HELP data_summary_object_counts Count of objects by type"),
        "Object counts help text is missing");
    assertTrue(content.contains("data_summary_object_counts"), "Object counts metric is missing");

    // Verify data value count metrics
    assertTrue(
        content.contains("# HELP data_summary_data_value_count Data value counts over time"),
        "Data value count help text is missing");
    assertTrue(
        content.contains("data_summary_data_value_count"), "Data value count metric is missing");

    // Verify event count metrics
    assertTrue(
        content.contains("# HELP data_summary_event_count Event counts over time"),
        "Event count help text is missing");
    assertTrue(content.contains("data_summary_event_count"), "Event count metric is missing");
  }
}
