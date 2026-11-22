/*
 * Copyright (c) 2004-2026, University of Oslo
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

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hisp.dhis.analytics.AnalyticsTableHook;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.web.WebClient.HttpResponse;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonErrorReport;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * Analytics table hooks enabled (analytics.table_hooks.enabled=on).
 *
 * @author Morten Svanæs
 */
@ContextConfiguration(classes = AnalyticsTableHookEnabledConfig.class)
@ActiveProfiles("analytics-table-hooks-enabled-test")
class AnalyticsTableHookControllerTest extends DhisControllerConvenienceTest {

  private static final String HOOK_SQL =
      "update analytics_rs_orgunitstructure set organisationunitid=1";

  private static String hookJson(String uid, String sql) {
    return "{'id':'%s','name':'Hook %s','phase':'RESOURCE_TABLE_POPULATED','resourceTableType':'ORG_UNIT_STRUCTURE','sql':'%s'}"
        .formatted(uid, uid, sql);
  }

  private static void assertRejected(ErrorCode expected, HttpResponse response) {
    assertNotNull(
        response
            .content(HttpStatus.CONFLICT)
            .find(JsonErrorReport.class, report -> report.getErrorCode() == expected));
  }

  private AnalyticsTableHook getHook(String uid) {
    manager.clear();
    return manager.get(AnalyticsTableHook.class, uid);
  }

  @Test
  void testImportAnalyticsTableHook() {
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/analyticsTableHooks/",
            "{'name':'NameA', 'phase':'RESOURCE_TABLE_POPULATED', 'resourceTableType':'ORG_UNIT_STRUCTURE', 'sql':'update analytics_rs_orgunitstructure set organisationunitid=3'}"));
  }

  @Test
  void canUpdateHookName() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/analyticsTableHooks/",
                "{'name':'OriginalName', 'phase':'RESOURCE_TABLE_POPULATED', 'resourceTableType':'ORG_UNIT_STRUCTURE', 'sql':'update analytics_rs_orgunitstructure set organisationunitid=1'}"));
    assertStatus(
        HttpStatus.OK,
        PUT(
            "/analyticsTableHooks/" + uid,
            "{'name':'NewName', 'phase':'RESOURCE_TABLE_POPULATED', 'resourceTableType':'ORG_UNIT_STRUCTURE', 'sql':'update analytics_rs_orgunitstructure set organisationunitid=1'}"));
  }

  @Test
  void canDeleteHook() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/analyticsTableHooks/",
                "{'name':'ToDelete', 'phase':'RESOURCE_TABLE_POPULATED', 'resourceTableType':'ORG_UNIT_STRUCTURE', 'sql':'update analytics_rs_orgunitstructure set organisationunitid=9'}"));
    assertStatus(HttpStatus.OK, DELETE("/analyticsTableHooks/" + uid));
  }

  @Test
  void nonSuperuserWithHookAuthorityCannotCreateHook() {
    switchToNewUser("hookuser", "F_ANALYTICSTABLEHOOK_ADD", "F_METADATA_IMPORT");

    assertRejected(
        ErrorCode.E3000, POST("/analyticsTableHooks/", hookJson("HookUser001", HOOK_SQL)));
    assertRejected(
        ErrorCode.E3000,
        POST("/analyticsTableHooks?skipValidation=true", hookJson("HookUser002", HOOK_SQL)));
    assertRejected(
        ErrorCode.E3000,
        POST(
            "/metadata?skipValidation=true",
            "{'analyticsTableHooks':[" + hookJson("HookUser003", HOOK_SQL) + "]}"));

    switchToSuperuser();
    assertNull(getHook("HookUser001"));
    assertNull(getHook("HookUser002"));
    assertNull(getHook("HookUser003"));
  }

  @Test
  void nonSuperuserWithHookAuthorityCannotUpdateHook() {
    assertStatus(
        HttpStatus.CREATED, POST("/analyticsTableHooks/", hookJson("HookUser004", HOOK_SQL)));
    switchToNewUser("hookuser", "F_ANALYTICSTABLEHOOK_ADD", "F_METADATA_IMPORT");

    assertRejected(
        ErrorCode.E3001,
        PUT("/analyticsTableHooks/HookUser004", hookJson("HookUser004", "select 2")));
    assertRejected(
        ErrorCode.E3001,
        PATCH(
            "/analyticsTableHooks/HookUser004",
            "[{'op':'replace','path':'/sql','value':'select 3'}]"));
    assertRejected(
        ErrorCode.E3001,
        POST(
            "/metadata?importStrategy=UPDATE",
            "{'analyticsTableHooks':[" + hookJson("HookUser004", "select 4") + "]}"));

    switchToSuperuser();
    assertEquals(HOOK_SQL, getHook("HookUser004").getSql());
  }
}
