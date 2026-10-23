/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.TEXT_HTML_VALUE;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link ReportController} using (mocked) REST requests.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class ReportControllerTest extends H2ControllerIntegrationTestBase {

  private static final Pattern PERIODS =
      Pattern.compile("dhis2\\.report\\.periods = \\[(.*?)];", Pattern.DOTALL);

  private static final Pattern QUOTED = Pattern.compile("\"([^\"]+)\"");

  @Test
  void testGetReportAsHtmlExposesRelativePeriods() {
    String id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/reports",
                """
                {'name':'HTML report','type':'HTML','designContent':'<p>report</p>',
                'relativePeriods':{'thisMonth':true,'lastMonth':true}}
                """));

    String html = GET("/reports/{id}/data.html?date=2026-09-15", id).content(TEXT_HTML_VALUE);

    assertEquals(Set.of("202609", "202608"), periodsOf(html));
  }

  @Test
  void testGetReportAsHtmlWithoutRelativePeriods() {
    String id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/reports",
                "{'name':'HTML report','type':'HTML','designContent':'<p>report</p>'}"));

    String html = GET("/reports/{id}/data.html?date=2026-09-15", id).content(TEXT_HTML_VALUE);

    assertEquals(Set.of(), periodsOf(html));
  }

  private static Set<String> periodsOf(String html) {
    Matcher periods = PERIODS.matcher(html);
    assertTrue(periods.find(), "dhis2.report.periods missing from rendered report");
    return QUOTED
        .matcher(periods.group(1))
        .results()
        .map(m -> m.group(1))
        .collect(Collectors.toSet());
  }
}
