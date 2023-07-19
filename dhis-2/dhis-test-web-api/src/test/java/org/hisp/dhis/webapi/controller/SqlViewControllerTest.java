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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonResponse;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.sqlview.SqlView;
import org.hisp.dhis.sqlview.SqlViewQuery;
import org.hisp.dhis.sqlview.SqlViewService;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests the {@link SqlViewController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
@ExtendWith(MockitoExtension.class)
class SqlViewControllerTest extends DhisControllerConvenienceTest {

  @Mock private SqlViewService sqlViewService;

  @Mock private JobConfigurationService jobConfigurationService;

  @Mock private ContextUtils contextUtils;
  @Mock private ContextService contextService;

  @Mock private DhisConfigurationProvider config;

  @InjectMocks private SqlViewController controller;

  @Test
  void testExecuteView_NoSuchView() {
    assertWebMessage(
        "Not Found",
        404,
        "ERROR",
        "SqlView with id xyz could not be found.",
        POST("/sqlViews/xyz/execute").content(HttpStatus.NOT_FOUND));
  }

  @Test
  void testExecuteView_ValidationError() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST("/sqlViews/", "{'name':'My SQL View','sqlQuery':'select 1 from userinfo'}"));
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "SQL query contains references to protected tables",
        POST("/sqlViews/" + uid + "/execute").content(HttpStatus.CONFLICT));
  }

  @Test
  void testRefreshMaterializedView() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST("/sqlViews/", "{'name':'My SQL View','sqlQuery':'select 1 from userinfo'}"));
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "View could not be refreshed",
        POST("/sqlViews/" + uid + "/refresh").content(HttpStatus.CONFLICT));
  }

  @Test
  void testRefreshMaterializedView_NoSuchView() {
    assertWebMessage(
        "Not Found",
        404,
        "ERROR",
        "SqlView with id xyz could not be found.",
        POST("/sqlViews/xyz/refresh").content(HttpStatus.NOT_FOUND));
  }

  @Test
  void testCreateWithDefaultValues() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST("/sqlViews/", "{'name':'My SQL View','sqlQuery':'select 1 from userinfo'}"));

    JsonResponse sqlView = GET("/sqlViews/{uid}", uid).content();
    assertEquals("VIEW", sqlView.getString("type").string());
    assertEquals("RESPECT_SYSTEM_SETTING", sqlView.getString("cacheStrategy").string());
  }

  @Test
  void testUpdate_MaterializedViewWithUpdate() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/sqlViews/",
                "{'name':'users_exist','type':'MATERIALIZED_VIEW','sqlQuery':'select 1 from userinfo'}"));

    String jobId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/jobConfigurations",
                "{'name':'update-sql','jobType':'MATERIALIZED_SQL_VIEW_UPDATE','cronExpression':'0 0 1 ? * *'}"));

    String updatePayload =
        GET("/sqlViews/" + uid)
            .content()
            .node()
            .addMember("updateJobId", "\"" + jobId + "\"")
            .getDeclaration();
    assertStatus(HttpStatus.OK, PUT("/sqlViews/" + uid, updatePayload));

    JsonObject params = GET("/jobConfigurations/{id}", jobId).content().getObject("jobParameters");
    assertEquals(List.of(uid), params.getArray("sqlViews").stringValues());

    assertStatus(HttpStatus.OK, DELETE("/sqlViews/" + uid));

    params = GET("/jobConfigurations/{id}", jobId).content().getObject("jobParameters");
    assertEquals(List.of(), params.getArray("sqlViews").stringValues());
  }

  /**
   * This test is purely to check the correct control flow is followed based on the value of the
   * config property SYSTEM_SQL_VIEW_WRITE_ENABLED
   */
  @Test
  void testCorrectServiceMethodCalledWhenSqlViewWritesEnabled() throws NotFoundException {
    SqlViewQuery query = new SqlViewQuery();
    query.setCriteria(Set.of("select", "createatable();"));

    when(sqlViewService.getSqlViewByUid("123")).thenReturn(new SqlView());
    when(contextService.getParameterValues("filter")).thenReturn(List.of());
    when(config.isEnabled(ConfigurationKey.SYSTEM_SQL_VIEW_WRITE_ENABLED)).thenReturn(true);
    when(sqlViewService.getSqlViewGridWritesAllowed(any(), any(), any(), any(), any()))
        .thenReturn(new ListGrid());

    controller.getViewJson("123", query, null);
    verify(sqlViewService, times(1)).getSqlViewGridWritesAllowed(any(), any(), any(), any(), any());
    verify(sqlViewService, never()).getSqlViewGridReadOnly(any(), any(), any(), any(), any());
  }

  /**
   * This test is purely to check the correct control flow is followed based on the value of the
   * config property SYSTEM_SQL_VIEW_WRITE_ENABLED
   */
  @Test
  void testCorrectServiceMethodCalledWhenSqlViewWritesDisabled() throws NotFoundException {
    SqlViewQuery query = new SqlViewQuery();
    query.setCriteria(Set.of("select", "createatable();"));

    when(sqlViewService.getSqlViewByUid("123")).thenReturn(new SqlView());
    when(contextService.getParameterValues("filter")).thenReturn(List.of());
    when(config.isEnabled(ConfigurationKey.SYSTEM_SQL_VIEW_WRITE_ENABLED)).thenReturn(false);
    when(sqlViewService.getSqlViewGridReadOnly(any(), any(), any(), any(), any()))
        .thenReturn(new ListGrid());

    controller.getViewJson("123", query, null);
    verify(sqlViewService, times(1)).getSqlViewGridReadOnly(any(), any(), any(), any(), any());
    verify(sqlViewService, never()).getSqlViewGridWritesAllowed(any(), any(), any(), any(), any());
  }
}
