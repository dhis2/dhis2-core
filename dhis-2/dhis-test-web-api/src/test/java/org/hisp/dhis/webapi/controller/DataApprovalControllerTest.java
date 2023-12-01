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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonDataApprovalPermissions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests the {@link DataApprovalController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
class DataApprovalControllerTest extends DhisControllerConvenienceTest {

  private String ouId;

  private String wfId;

  private String dsId;

  @Autowired private PeriodService periodService;

  @Autowired private CategoryService categoryService;

  @BeforeEach
  void setUp() {
    periodService.addPeriod(createPeriod("202101"));
    String ou1Id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits/",
                "{'name':'My Root Unit', 'shortName':'OU0', 'openingDate': '2020-01-01'}"));
    ouId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits/",
                "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01', "
                    + "'parent':{'id': '"
                    + ou1Id
                    + "'}}"));
    assertStatus(
        HttpStatus.OK, POST("/users/" + getCurrentUser().getUid() + "/organisationUnits/" + ou1Id));
    String level1Id =
        assertStatus(
            HttpStatus.CREATED,
            POST("/dataApprovalLevels/", "{'name':'L1', 'level': 1, 'orgUnitLevel': 1}"));
    String level2Id =
        assertStatus(
            HttpStatus.CREATED,
            POST("/dataApprovalLevels/", "{'name':'L2', 'level': 2, 'orgUnitLevel': 2}"));
    wfId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataApprovalWorkflows/",
                "{'name':'W1', 'periodType':'Monthly', "
                    + "'dataApprovalLevels':[{'id':'"
                    + level1Id
                    + "'}, {'id':'"
                    + level2Id
                    + "'}]}"));
    dsId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                "{'name':'My data set', 'shortName': 'MDS', 'periodType':'Monthly', "
                    + "'workflow': {'id':'"
                    + wfId
                    + "'},"
                    + "'organisationUnits':[{'id':'"
                    + ou1Id
                    + "'},{'id':'"
                    + ouId
                    + "'}]"
                    + "}"));

    getSuperUser().addOrganisationUnit(manager.get(OrganisationUnit.class, ouId));
  }

  @Test
  void testGetApprovalPermissions() {
    JsonDataApprovalPermissions permissions =
        GET("/dataApprovals?ou={ou}&pe=202101&wf={wf}", ouId, wfId)
            .content(HttpStatus.OK)
            .as(JsonDataApprovalPermissions.class);
    assertEquals("UNAPPROVED_READY", permissions.getState());
    assertTrue(permissions.isMayReadData());
    assertFalse(permissions.isMayAccept());
    assertFalse(permissions.isMayUnaccept());
    assertTrue(permissions.isMayApprove());
    assertFalse(permissions.isMayUnapprove());
  }

  @Test
  void testGetMultipleApprovalPermissions_Multiple() {
    JsonArray statuses =
        GET("/dataApprovals/multiple?ou={ou}&pe=202101&wf={wf}", ouId, wfId).content(HttpStatus.OK);
    assertEquals(1, statuses.size());
    JsonObject status = statuses.getObject(0);
    assertTrue(status.has("wf", "pe", "ou", "aoc"));
    assertEquals(ouId, status.getString("ou").string());
    assertEquals(wfId, status.getString("wf").string());
    assertEquals("202101", status.getString("pe").string());
  }

  @Test
  void testGetMultipleApprovalPermissions_Approvals() {
    JsonArray statuses =
        GET("/dataApprovals/approvals?ou={ou}&pe=202101&wf={wf}", ouId, wfId)
            .content(HttpStatus.OK);
    assertEquals(1, statuses.size());
    JsonObject status = statuses.getObject(0);
    assertTrue(status.has("wf", "pe", "ou", "aoc"));
    assertEquals(ouId, status.getString("ou").string());
    assertEquals(wfId, status.getString("wf").string());
    assertEquals("202101", status.getString("pe").string());
  }

  @Test
  void testGetApprovalByCategoryOptionCombos() {
    JsonArray statuses =
        GET("/dataApprovals/categoryOptionCombos?ou={ou}&pe=202101&wf={wf}", ouId, wfId)
            .content(HttpStatus.OK);
    assertTrue(statuses.isArray());
    assertEquals(1, statuses.size());

    statuses =
        GET(
                "/dataApprovals/categoryOptionCombos?ou={ou}&pe=202101&wf={wf}&ouFilter={ou}",
                ouId,
                wfId,
                ouId)
            .content(HttpStatus.OK);
    assertTrue(statuses.isArray());
    assertEquals(1, statuses.size());

    String aocId = categoryService.getDefaultCategoryOptionCombo().getUid();

    statuses =
        GET(
                "/dataApprovals/categoryOptionCombos?ou={ou}&pe=202101&wf={wf}&aoc={ou}",
                ouId,
                wfId,
                aocId)
            .content(HttpStatus.OK);
    assertTrue(statuses.isArray());
    assertEquals(1, statuses.size());
  }

  @Test
  void testGetApproval() {
    JsonArray statuses =
        GET("/dataApprovals/status?ou={ou}&pe=202101&wf={wf}&ds={ds}", ouId, wfId, dsId)
            .content(HttpStatus.OK)
            .getArray("dataApprovalStateResponses");
    assertEquals(1, statuses.size());
    JsonObject status_t0 = statuses.getObject(0);
    assertEquals("UNAPPROVED_READY", status_t0.getString("state").string());
    JsonObject permissions = status_t0.getObject("permissions");
    assertFalse(permissions.getString("acceptedBy").exists());
    assertFalse(permissions.getString("acceptedAt").exists());
    // now create an approval (approve it)
    assertStatus(
        HttpStatus.NO_CONTENT, POST("/dataApprovals?ou={ou}&pe=202101&wf={wf}", ouId, wfId));
    JsonObject status_t1 =
        GET("/dataApprovals/status?ou={ou}&pe=202101&wf={wf}&ds={ds}", ouId, wfId, dsId)
            .content(HttpStatus.OK)
            .getArray("dataApprovalStateResponses")
            .getObject(0);
    assertEquals("APPROVED_HERE", status_t1.getString("state").string());
    permissions = status_t1.getObject("permissions");
    assertFalse(permissions.getBoolean("mayReadAcceptedBy").exists());
    assertTrue(permissions.getString("acceptedAt").exists());
  }
}
