/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.webapi.security.csp;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.http.HttpStatus.CREATED;
import static org.hisp.dhis.http.HttpStatus.OK;
import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * The CSP of rendered HTML standard reports: {@code GET /api/reports/{uid}/data.html} starts with
 * the inline script of the report template, so only that endpoint allows inline scripts. Every
 * other report endpoint keeps the default policy of the baseline filter (no handler policy).
 *
 * @author Morten Svanæs
 */
@Transactional
class CspHtmlReportControllerTest extends H2ControllerIntegrationTestBase {
  private static final String CSP = "Content-Security-Policy";

  private String uid;

  @BeforeEach
  void createReport() {
    uid =
        assertStatus(
            CREATED,
            POST(
                "/reports/",
                """
                {"name":"Scripted","type":"HTML","designContent":"<p>report</p>"}"""));
  }

  @Test
  void renderedHtmlReport_allowsInlineScript_andKeepsHardening() {
    HttpResponse response = GET("/reports/" + uid + "/data.html");

    assertContains("<script type=\"text/javascript\">", response.content("text/html"));
    Map<String, List<String>> csp = directives(response.header(CSP));
    assertEquals(List.of("'self'", "'unsafe-inline'"), csp.get("script-src"));
    assertEquals(List.of("'self'"), csp.get("default-src"));
    assertEquals(List.of("'self'", "'unsafe-inline'"), csp.get("style-src"));
    assertEquals(List.of("'none'"), csp.get("object-src"));
    assertEquals(List.of("'self'"), csp.get("base-uri"));
    assertEquals(List.of("'self'"), csp.get("form-action"));
    assertEquals(List.of("'self'"), csp.get("frame-ancestors"));
  }

  @Test
  void otherReportEndpoints_keepDefaultPolicy() {
    HttpResponse design = GET("/reports/" + uid + "/design");
    assertEquals(OK, design.status());
    assertNull(design.header(CSP));

    HttpResponse metadata = GET("/reports/" + uid);
    assertEquals(OK, metadata.status());
    assertNull(metadata.header(CSP));
  }

  private static Map<String, List<String>> directives(String policy) {
    assertNotNull(policy, "Content-Security-Policy header should be present");
    Map<String, List<String>> result = new HashMap<>();
    for (String directive : policy.split(";")) {
      List<String> tokens = List.of(directive.trim().split("\\s+"));
      if (!tokens.get(0).isEmpty()) {
        result.put(tokens.get(0), tokens.subList(1, tokens.size()));
      }
    }
    return result;
  }
}
