/*
 * Copyright (c) 2004-2024, University of Oslo
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

import static org.hisp.dhis.test.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.test.web.HttpStatus;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonErrorReport;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class AnalyticsTableHookControllerTest extends H2ControllerIntegrationTestBase {

  @Test
  void testImportAnalyticsTableHook() {
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/analyticsTableHooks/",
            "{'name':'NameA', 'phase':'RESOURCE_TABLE_POPULATED', 'resourceTableType':'ORG_UNIT_STRUCTURE', 'sql':'update analytics_rs_orgunitstructure set organisationunitid=3'}"));
  }

  @Test
  void cannotImportDuplicateTableHook() {

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/analyticsTableHooks/",
            "{'name':'NameA', 'phase':'RESOURCE_TABLE_POPULATED', 'resourceTableType':'ORG_UNIT_STRUCTURE', 'sql':'update analytics_rs_orgunitstructure set organisationunitid=1'}"));
    // Exact duplicate
    assertStatus(
        HttpStatus.CONFLICT,
        POST(
            "/analyticsTableHooks/",
            "{'name':'NameA', 'phase':'RESOURCE_TABLE_POPULATED', 'resourceTableType':'ORG_UNIT_STRUCTURE', 'sql':'update analytics_rs_orgunitstructure set organisationunitid=1'}"));
    // Different name but otherwise equal

    JsonErrorReport error =
        POST(
                "/analyticsTableHooks/",
                "{'name':'NameB', 'phase':'RESOURCE_TABLE_POPULATED', 'resourceTableType':'ORG_UNIT_STRUCTURE', 'sql':'update analytics_rs_orgunitstructure set organisationunitid=1'}")
            .content(HttpStatus.CONFLICT)
            .find(JsonErrorReport.class, report -> report.getErrorCode() == ErrorCode.E6400);
    assertNotNull(error);
    assertEquals("Analytics table hook `NameB` is a duplicate of `NameA`", error.getMessage());
  }
}
