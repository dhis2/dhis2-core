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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hisp.dhis.analytics.AnalyticsTableHook;
import org.hisp.dhis.analytics.AnalyticsTablePhase;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.http.HttpClientAdapter.HttpResponse;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.resourcetable.ResourceTableType;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonErrorReport;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default dhis.conf has {@code analytics.table_hooks.enabled=off}. Create and update must be
 * rejected with {@link ErrorCode#E1007} for every user and import path, including superusers
 * importing with {@code skipValidation=true}. Deletion stays possible.
 *
 * @author Morten Svanæs
 */
@Transactional
class AnalyticsTableHookDisabledControllerTest extends H2ControllerIntegrationTestBase {

  private static final String ORIGINAL_SQL =
      "update analytics_rs_orgunitstructure set organisationunitid=1";

  private static String hookJson(String uid, String name, String sql) {
    return "{'id':'%s','name':'%s','phase':'RESOURCE_TABLE_POPULATED','resourceTableType':'ORG_UNIT_STRUCTURE','sql':'%s'}"
        .formatted(uid, name, sql);
  }

  private static String metadataJson(String hookJson) {
    return "{'analyticsTableHooks':[" + hookJson + "]}";
  }

  /** Stores a hook directly, as if it had been created before the upgrade. */
  private void createExistingHook(String uid) {
    AnalyticsTableHook hook =
        new AnalyticsTableHook(
            "Existing " + uid,
            AnalyticsTablePhase.RESOURCE_TABLE_POPULATED,
            ResourceTableType.ORG_UNIT_STRUCTURE,
            ORIGINAL_SQL);
    hook.setUid(uid);
    manager.save(hook);
  }

  private AnalyticsTableHook getHook(String uid) {
    manager.clear();
    return manager.get(AnalyticsTableHook.class, uid);
  }

  private static void assertImportRejected(HttpResponse response) {
    assertNotNull(
        response
            .content(HttpStatus.CONFLICT)
            .find(JsonErrorReport.class, report -> report.getErrorCode() == ErrorCode.E1007));
  }

  private static void assertCommitRejected(HttpResponse response) {
    assertEquals(
        ErrorCode.E1007,
        response.content(HttpStatus.CONFLICT).as(JsonWebMessage.class).getErrorCode());
  }

  @Test
  void cannotCreateHookWhenFeatureDisabled() {
    assertImportRejected(
        POST("/analyticsTableHooks/", hookJson("HookCreat01", "Blocked", ORIGINAL_SQL)));
    assertNull(getHook("HookCreat01"));
  }

  @Test
  void cannotCreateHookViaMetadataImportWhenFeatureDisabled() {
    assertImportRejected(
        POST("/metadata", metadataJson(hookJson("HookCreat02", "Blocked", ORIGINAL_SQL))));
    assertNull(getHook("HookCreat02"));
  }

  @Test
  void cannotCreateHookWithSkipValidationWhenFeatureDisabled() {
    assertCommitRejected(
        POST(
            "/metadata?skipValidation=true",
            metadataJson(hookJson("HookCreat03", "Blocked", ORIGINAL_SQL))));
    assertNull(getHook("HookCreat03"));

    assertCommitRejected(
        POST(
            "/analyticsTableHooks?skipValidation=true",
            hookJson("HookCreat04", "Blocked", ORIGINAL_SQL)));
    assertNull(getHook("HookCreat04"));
  }

  @Test
  void nonSuperuserWithHookAuthorityCannotCreateHookWhenFeatureDisabled() {
    switchToNewUser("hookuser", "F_ANALYTICSTABLEHOOK_ADD", "F_METADATA_IMPORT");

    assertImportRejected(
        POST("/analyticsTableHooks", hookJson("HookUser001", "Blocked", ORIGINAL_SQL)));
    assertImportRejected(
        POST(
            "/analyticsTableHooks?skipValidation=true",
            hookJson("HookUser002", "Blocked", ORIGINAL_SQL)));
    assertImportRejected(
        POST(
            "/metadata?skipValidation=true",
            metadataJson(hookJson("HookUser003", "Blocked", ORIGINAL_SQL))));

    switchToAdminUser();
    assertNull(getHook("HookUser001"));
    assertNull(getHook("HookUser002"));
    assertNull(getHook("HookUser003"));
  }

  @Test
  void cannotUpdateExistingHookWhenFeatureDisabled() {
    createExistingHook("HookUpdat01");

    assertImportRejected(
        PUT("/analyticsTableHooks/HookUpdat01", hookJson("HookUpdat01", "Changed", "select 2")));
    assertImportRejected(
        PATCH(
            "/analyticsTableHooks/HookUpdat01",
            "[{'op':'replace','path':'/sql','value':'select 3'}]"));
    assertImportRejected(
        POST(
            "/metadata?importStrategy=UPDATE",
            metadataJson(hookJson("HookUpdat01", "Changed", "select 4"))));

    assertEquals(ORIGINAL_SQL, getHook("HookUpdat01").getSql());
  }

  @Test
  void cannotUpdateExistingHookWithSkipValidationWhenFeatureDisabled() {
    createExistingHook("HookUpdat02");

    assertCommitRejected(
        POST(
            "/metadata?importStrategy=UPDATE&skipValidation=true",
            metadataJson(hookJson("HookUpdat02", "Changed", "select 5"))));

    assertEquals(ORIGINAL_SQL, getHook("HookUpdat02").getSql());
  }

  @Test
  void canDeleteHookWhileFeatureDisabled() {
    createExistingHook("HookDelet01");

    assertStatus(HttpStatus.OK, DELETE("/analyticsTableHooks/HookDelet01"));

    assertNull(getHook("HookDelet01"));
  }

  @Test
  void canDeleteHookViaMetadataImportWhileFeatureDisabled() {
    createExistingHook("HookDelet02");

    assertStatus(
        HttpStatus.OK,
        POST("/metadata?importStrategy=DELETE", "{'analyticsTableHooks':[{'id':'HookDelet02'}]}"));

    assertNull(getHook("HookDelet02"));
  }

  @Test
  void metadataImportWithoutAtomicityStillImportsOtherObjects() {
    HttpResponse response =
        POST(
            "/metadata?atomicMode=NONE",
            "{'dataElements':[{'id':'DeHookTst01','name':'DE hook test','shortName':'DE hook test',"
                + "'valueType':'NUMBER','domainType':'AGGREGATE','aggregationType':'SUM'}],"
                + metadataJson(hookJson("HookAtomi01", "Blocked", ORIGINAL_SQL)).substring(1));

    assertImportRejected(response);
    assertNull(getHook("HookAtomi01"));
    assertNotNull(manager.get(DataElement.class, "DeHookTst01"));
  }
}
