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

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hisp.dhis.common.DimensionalObjectUtils.getItemsFromParam;
import static org.hisp.dhis.security.Authorities.F_PERFORM_ANALYTICS_EXPLAIN;
import static org.hisp.dhis.system.grid.GridUtils.error;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_EXCEL;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_HTML;
import static org.hisp.dhis.webapi.utils.ContextUtils.HEADER_CONTENT_DISPOSITION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_HTML_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.analytics.util.AnalyticsUtils;
import org.hisp.dhis.common.AggregateAnalyticsQueryCriteria;
import org.hisp.dhis.common.DataQueryRequest;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.system.grid.GridUtils;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Lars Helge Overland
 */
@OpenApi.Document(
    entity = DataValue.class,
    classifiers = {"team:analytics", "purpose:analytics"})
@Controller
@AllArgsConstructor
public class AnalyticsController {
  private static final String RESOURCE_PATH = "/api/analytics";

  private static final String EXPLAIN_PATH = "/explain";

  private static final String DATA_VALUE_SET_PATH = "/dataValueSet";

  private static final String RAW_DATA_PATH = "/rawData";

  @Nonnull private final DataQueryService dataQueryService;

  @Nonnull private final AnalyticsService analyticsService;

  @Nonnull private final ContextUtils contextUtils;

  @Nonnull private final DhisConfigurationProvider configurationProvider;

  // -------------------------------------------------------------------------
  // Resources
  // -------------------------------------------------------------------------

  @RequiresAuthority(anyOf = F_PERFORM_ANALYTICS_EXPLAIN)
  @GetMapping(
      value = RESOURCE_PATH + EXPLAIN_PATH,
      produces = {APPLICATION_JSON_VALUE, "application/javascript"})
  public @ResponseBody Grid getExplainJson( // JSON
      AggregateAnalyticsQueryCriteria criteria, HttpServletResponse response) {
    return getGrid(criteria, ContextUtils.CONTENT_TYPE_JSON, response, true);
  }

  @GetMapping(
      value = RESOURCE_PATH,
      produces = {APPLICATION_JSON_VALUE, "application/javascript"})
  public @ResponseBody Grid getJson( // JSON, JSONP
      AggregateAnalyticsQueryCriteria criteria, HttpServletResponse response) {
    return getGrid(criteria, ContextUtils.CONTENT_TYPE_JSON, response);
  }

  @GetMapping(value = RESOURCE_PATH + ".xml")
  public void getXml(AggregateAnalyticsQueryCriteria criteria, HttpServletResponse response)
      throws Exception {
    try {
      GridUtils.toXml(
          getGrid(criteria, ContextUtils.CONTENT_TYPE_XML, response), response.getOutputStream());
    } catch (IllegalQueryException e) {
      sendErrorResponse(response, e);
    }
  }

  @GetMapping(value = RESOURCE_PATH + ".html")
  public void getHtml(AggregateAnalyticsQueryCriteria criteria, HttpServletResponse response)
      throws Exception {
    try {
      GridUtils.toHtml(getGrid(criteria, CONTENT_TYPE_HTML, response), response.getWriter());
    } catch (IllegalQueryException e) {
      sendErrorResponse(response, e);
    }
  }

  @GetMapping(value = RESOURCE_PATH + ".html+css")
  public void getHtmlCss(AggregateAnalyticsQueryCriteria criteria, HttpServletResponse response)
      throws Exception {
    try {
      GridUtils.toHtmlCss(getGrid(criteria, CONTENT_TYPE_HTML, response), response.getWriter());
    } catch (IllegalQueryException e) {
      sendErrorResponse(response, e);
    }
  }

  @GetMapping(value = RESOURCE_PATH + ".csv")
  public void getCsv(AggregateAnalyticsQueryCriteria criteria, HttpServletResponse response)
      throws Exception {
    try {
      GridUtils.toCsv(
          getGridWithAttachment(criteria, ContextUtils.CONTENT_TYPE_CSV, "data.csv", response),
          response.getWriter());
    } catch (IllegalQueryException e) {
      sendErrorResponse(response, e);
    }
  }

  @GetMapping(value = RESOURCE_PATH + ".xls")
  public void getXls(AggregateAnalyticsQueryCriteria criteria, HttpServletResponse response)
      throws Exception {
    try {
      GridUtils.toXls(
          getGridWithAttachment(criteria, CONTENT_TYPE_EXCEL, "data.xls", response),
          response.getOutputStream());
    } catch (IllegalQueryException e) {
      sendErrorResponse(response, e);
    }
  }

  private static void sendErrorResponse(HttpServletResponse response, IllegalQueryException e)
      throws IOException {
    response.setHeader(
        HEADER_CONTENT_DISPOSITION, "inline" + "; filename=\"" + "error.html" + "\"");
    error(e.getErrorCode(), response.getOutputStream());
  }

  @GetMapping(value = RESOURCE_PATH + ".xlsx")
  public void getXlsx(AggregateAnalyticsQueryCriteria criteria, HttpServletResponse response)
      throws Exception {
    try {
      GridUtils.toXlsx(
          getGridWithAttachment(criteria, CONTENT_TYPE_EXCEL, "data.xlsx", response),
          response.getOutputStream());
    } catch (IllegalQueryException e) {
      sendErrorResponse(response, e);
    }
  }

  @GetMapping(value = RESOURCE_PATH + ".jrxml")
  public void getJrxml(AggregateAnalyticsQueryCriteria criteria, HttpServletResponse response)
      throws Exception {
    DataQueryRequest request =
        DataQueryRequest.newBuilder().fromCriteria(criteria).skipMeta(true).build();

    DataQueryParams params = dataQueryService.getFromRequest(request);

    try {
      contextUtils.configureAnalyticsResponse(
          response,
          ContextUtils.CONTENT_TYPE_XML,
          CacheStrategy.RESPECT_SYSTEM_SETTING,
          "data.jrxml",
          false,
          params.getLatestEndDate());
      Grid grid = analyticsService.getAggregatedDataValues(params);

      GridUtils.toJrxml(grid, null, response.getWriter());
    } catch (IllegalQueryException e) {
      sendErrorResponse(response, e);
    }
  }

  @GetMapping(
      value = RESOURCE_PATH + "/debug/sql",
      produces = {TEXT_HTML_VALUE, TEXT_PLAIN_VALUE})
  public @ResponseBody String getDebugSql(
      AggregateAnalyticsQueryCriteria criteria, HttpServletResponse response) {
    DataQueryParams params = dataQueryService.getFromRequest(fromCriteria(criteria));

    contextUtils.configureAnalyticsResponse(
        response,
        ContextUtils.CONTENT_TYPE_TEXT,
        CacheStrategy.NO_CACHE,
        "debug.sql",
        false,
        params.getLatestEndDate());

    return AnalyticsUtils.getDebugDataSql(params);
  }

  // -------------------------------------------------------------------------
  // Raw data
  // -------------------------------------------------------------------------

  @GetMapping(value = RESOURCE_PATH + RAW_DATA_PATH + ".json")
  public @ResponseBody Grid getRawDataJson(
      AggregateAnalyticsQueryCriteria criteria, HttpServletResponse response) {
    DataQueryRequest request = DataQueryRequest.newBuilder().fromCriteria(criteria).build();

    DataQueryParams params = dataQueryService.getFromRequest(request);

    contextUtils.configureAnalyticsResponse(
        response,
        ContextUtils.CONTENT_TYPE_JSON,
        CacheStrategy.RESPECT_SYSTEM_SETTING,
        null,
        false,
        params.getLatestEndDate());

    return analyticsService.getRawDataValues(params);
  }

  @GetMapping(value = RESOURCE_PATH + RAW_DATA_PATH + ".csv")
  public void getRawDataCsv(AggregateAnalyticsQueryCriteria criteria, HttpServletResponse response)
      throws Exception {
    DataQueryRequest request = DataQueryRequest.newBuilder().fromCriteria(criteria).build();

    DataQueryParams params = dataQueryService.getFromRequest(request);

    contextUtils.configureAnalyticsResponse(
        response,
        ContextUtils.CONTENT_TYPE_CSV,
        CacheStrategy.RESPECT_SYSTEM_SETTING,
        null,
        false,
        params.getLatestEndDate());

    Grid grid = analyticsService.getRawDataValues(params);

    GridUtils.toCsv(grid, response.getWriter());
  }

  // -------------------------------------------------------------------------
  // Data value set
  // -------------------------------------------------------------------------

  @GetMapping(value = RESOURCE_PATH + DATA_VALUE_SET_PATH + ".xml")
  public @ResponseBody DataValueSet getDataValueSetXml(
      AggregateAnalyticsQueryCriteria criteria, HttpServletResponse response) {
    DataQueryParams params = dataQueryService.getFromRequest(fromCriteria(criteria));

    contextUtils.configureAnalyticsResponse(
        response,
        ContextUtils.CONTENT_TYPE_XML,
        CacheStrategy.RESPECT_SYSTEM_SETTING,
        null,
        false,
        params.getLatestEndDate());

    return analyticsService.getAggregatedDataValueSet(params);
  }

  @GetMapping(value = RESOURCE_PATH + DATA_VALUE_SET_PATH + ".json")
  public @ResponseBody DataValueSet getDataValueSetJson(
      AggregateAnalyticsQueryCriteria criteria, HttpServletResponse response) {
    DataQueryParams params = dataQueryService.getFromRequest(fromCriteria(criteria));

    contextUtils.configureAnalyticsResponse(
        response,
        ContextUtils.CONTENT_TYPE_JSON,
        CacheStrategy.RESPECT_SYSTEM_SETTING,
        null,
        false,
        params.getLatestEndDate());

    return analyticsService.getAggregatedDataValueSet(params);
  }

  @GetMapping(value = RESOURCE_PATH + DATA_VALUE_SET_PATH + ".csv")
  public void getDataValueSetCsv(
      AggregateAnalyticsQueryCriteria criteria, HttpServletResponse response) throws Exception {
    DataQueryParams params = dataQueryService.getFromRequest(fromCriteria(criteria));

    contextUtils.configureAnalyticsResponse(
        response,
        ContextUtils.CONTENT_TYPE_CSV,
        CacheStrategy.RESPECT_SYSTEM_SETTING,
        "data.csv",
        true,
        params.getLatestEndDate());

    Grid grid = analyticsService.getAggregatedDataValueSetAsGrid(params);

    GridUtils.toCsv(grid, response.getWriter());
  }

  @GetMapping(
      value = RESOURCE_PATH + "/tableTypes",
      produces = {APPLICATION_JSON_VALUE, "application/javascript"})
  public @ResponseBody AnalyticsTableType[] getTableTypes() {
    return AnalyticsTableType.values();
  }

  // -------------------------------------------------------------------------
  // Private methods
  // -------------------------------------------------------------------------

  private Grid getGrid(
      AggregateAnalyticsQueryCriteria criteria, String contentType, HttpServletResponse response) {
    return getGrid(criteria, contentType, response, false);
  }

  private Grid getGrid(
      AggregateAnalyticsQueryCriteria criteria,
      String contentType,
      HttpServletResponse response,
      boolean analyzeOnly) {
    DataQueryParams params = dataQueryService.getFromRequest(fromCriteria(criteria));
    params.setDownloadFlag(!ContextUtils.CONTENT_TYPE_JSON.equals(contentType));

    if (isNotBlank(configurationProvider.getServerBaseUrl())) {
      params =
          DataQueryParams.newBuilder(params)
              .withServerBaseUrl(configurationProvider.getServerBaseUrl())
              .build();
    }

    if (analyzeOnly) {
      params = DataQueryParams.newBuilder(params).withSkipData(false).withAnalyzeOrderId().build();
    }

    contextUtils.configureAnalyticsResponse(
        response,
        contentType,
        CacheStrategy.RESPECT_SYSTEM_SETTING,
        null,
        false,
        params.getLatestEndDate());

    return analyticsService.getAggregatedDataValues(
        params, getItemsFromParam(criteria.getColumns()), getItemsFromParam(criteria.getRows()));
  }

  private Grid getGridWithAttachment(
      AggregateAnalyticsQueryCriteria criteria,
      String contentType,
      String file,
      HttpServletResponse response) {
    DataQueryParams params = dataQueryService.getFromRequest(fromCriteria(criteria));
    params.setDownloadFlag(!ContextUtils.CONTENT_TYPE_JSON.equals(contentType));

    contextUtils.configureAnalyticsResponse(
        response,
        contentType,
        CacheStrategy.RESPECT_SYSTEM_SETTING,
        file,
        true,
        params.getLatestEndDate());

    return analyticsService.getAggregatedDataValues(
        params, getItemsFromParam(criteria.getColumns()), getItemsFromParam(criteria.getRows()));
  }

  private DataQueryRequest fromCriteria(AggregateAnalyticsQueryCriteria criteria) {
    return DataQueryRequest.newBuilder().fromCriteria(criteria).build();
  }
}
