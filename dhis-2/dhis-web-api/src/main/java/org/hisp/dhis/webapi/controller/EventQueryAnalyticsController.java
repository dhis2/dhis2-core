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

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.hisp.dhis.common.RequestTypeAware.EndpointAction.OTHER;
import static org.hisp.dhis.common.RequestTypeAware.EndpointAction.QUERY;
import static org.hisp.dhis.common.RequestTypeAware.EndpointItem.EVENT;
import static org.hisp.dhis.common.cache.CacheStrategy.RESPECT_SYSTEM_SETTING;
import static org.hisp.dhis.feedback.ErrorCode.E7235;
import static org.hisp.dhis.security.Authorities.F_PERFORM_ANALYTICS_EXPLAIN;
import static org.hisp.dhis.system.grid.GridUtils.toCsv;
import static org.hisp.dhis.system.grid.GridUtils.toHtml;
import static org.hisp.dhis.system.grid.GridUtils.toHtmlCss;
import static org.hisp.dhis.system.grid.GridUtils.toXls;
import static org.hisp.dhis.system.grid.GridUtils.toXlsx;
import static org.hisp.dhis.system.grid.GridUtils.toXml;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_CSV;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_EXCEL;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_HTML;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_JSON;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_XML;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import org.hisp.dhis.analytics.Rectangle;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.dimensions.AnalyticsDimensionsPagingWrapper;
import org.hisp.dhis.analytics.event.EventAnalyticsDimensionsService;
import org.hisp.dhis.analytics.event.EventDataQueryService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.EventQueryService;
import org.hisp.dhis.common.EventDataQueryRequest;
import org.hisp.dhis.common.EventsAnalyticsQueryCriteria;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.PrefixedDimension;
import org.hisp.dhis.common.RequestTypeAware.EndpointAction;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.util.PeriodCriteriaUtils;
import org.hisp.dhis.webapi.dimension.DimensionFilteringAndPagingService;
import org.hisp.dhis.webapi.dimension.DimensionMapperService;
import org.hisp.dhis.webapi.dimension.DimensionResponse;
import org.hisp.dhis.webapi.dimension.DimensionsCriteria;
import org.hisp.dhis.webapi.dimension.EventAnalyticsPrefixStrategy;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@OpenApi.Document(
    entity = TrackerEvent.class,
    classifiers = {"team:analytics", "purpose:analytics"})
@Controller
@AllArgsConstructor
@RequestMapping("/api/analytics/events")
public class EventQueryAnalyticsController {

  @Nonnull private final EventDataQueryService eventDataService;

  @Nonnull private final EventQueryService eventQueryService;

  @Nonnull private final ContextUtils contextUtils;

  @Nonnull private final DimensionFilteringAndPagingService dimensionFilteringAndPagingService;

  @Nonnull private final EventAnalyticsDimensionsService eventAnalyticsDimensionsService;

  @Nonnull private final ExecutionPlanStore executionPlanStore;

  @Nonnull private final DimensionMapperService dimensionMapperService;

  @Nonnull private final SystemSettingsProvider settingsProvider;

  @GetMapping(
      value = "/count/{program}",
      produces = {APPLICATION_JSON_VALUE, "application/javascript"})
  public @ResponseBody Rectangle getCountJson( // JSON, JSONP
      @PathVariable String program,
      EventsAnalyticsQueryCriteria criteria,
      HttpServletResponse response) {
    EventQueryParams params = getEventQueryParams(program, criteria, false, OTHER);

    configResponseForJson(response);

    return eventQueryService.getRectangle(params);
  }

  @GetMapping(
      value = "/cluster/{program}",
      produces = {APPLICATION_JSON_VALUE, "application/javascript"})
  public @ResponseBody Grid getClusterJson( // JSON, JSONP
      @PathVariable String program,
      EventsAnalyticsQueryCriteria criteria,
      @RequestParam Long clusterSize,
      @RequestParam String bbox,
      @RequestParam(required = false) boolean includeClusterPoints,
      HttpServletResponse response) {
    EventQueryParams params = getEventQueryParams(program, criteria, false, OTHER);

    params =
        new EventQueryParams.Builder(params)
            .withClusterSize(clusterSize)
            .withBbox(bbox)
            .withIncludeClusterPoints(includeClusterPoints)
            .build();

    configResponseForJson(response);

    return eventQueryService.getEventClusters(params);
  }

  @RequiresAuthority(anyOf = F_PERFORM_ANALYTICS_EXPLAIN)
  @GetMapping(
      value = "/query/{program}/explain",
      produces = {APPLICATION_JSON_VALUE, "application/javascript"})
  public @ResponseBody Grid getExplainQueryJson( // JSON, JSONP
      @PathVariable String program,
      EventsAnalyticsQueryCriteria criteria,
      HttpServletResponse response) {
    EventQueryParams params = getEventQueryParams(program, criteria, true, QUERY);

    configResponseForJson(response);

    Grid grid = eventQueryService.getEvents(params);

    if (params.analyzeOnly()) {
      grid.addPerformanceMetrics(executionPlanStore.getExecutionPlans(params.getExplainOrderId()));
    }

    return grid;
  }

  @GetMapping(
      value = "/query/{program}",
      produces = {APPLICATION_JSON_VALUE, "application/javascript"})
  public @ResponseBody Grid getQueryJson( // JSON, JSONP
      @PathVariable String program,
      EventsAnalyticsQueryCriteria criteria,
      HttpServletResponse response) {
    EventQueryParams params = getEventQueryParams(program, criteria, false, QUERY);

    configResponseForJson(response);

    return eventQueryService.getEvents(params);
  }

  @GetMapping(value = "/query/{program}.xml")
  public void getQueryXml(
      @PathVariable String program,
      EventsAnalyticsQueryCriteria criteria,
      HttpServletResponse response)
      throws Exception {
    toXml(
        getListGridWithAttachment(
            criteria, program, CONTENT_TYPE_XML, "events.xml", false, response),
        response.getOutputStream());
  }

  @GetMapping(value = "/query/{program}.xls")
  public void getQueryXls(
      @PathVariable String program,
      EventsAnalyticsQueryCriteria criteria,
      HttpServletResponse response)
      throws Exception {
    toXls(
        getListGridWithAttachment(
            criteria, program, CONTENT_TYPE_EXCEL, "events.xls", true, response),
        response.getOutputStream());
  }

  @GetMapping(value = "/query/{program}.xlsx")
  public void getQueryXlsx(
      @PathVariable String program,
      EventsAnalyticsQueryCriteria criteria,
      HttpServletResponse response)
      throws Exception {
    toXlsx(
        getListGridWithAttachment(
            criteria, program, CONTENT_TYPE_EXCEL, "events.xlsx", true, response),
        response.getOutputStream());
  }

  @GetMapping(value = "/query/{program}.csv")
  public void getQueryCsv(
      @PathVariable String program,
      EventsAnalyticsQueryCriteria criteria,
      HttpServletResponse response)
      throws Exception {
    toCsv(
        getListGridWithAttachment(
            criteria, program, CONTENT_TYPE_CSV, "events.csv", true, response),
        response.getWriter());
  }

  @GetMapping(value = "/query/{program}.html")
  public void getQueryHtml(
      @PathVariable String program,
      EventsAnalyticsQueryCriteria criteria,
      HttpServletResponse response)
      throws Exception {
    toHtml(
        getListGridWithAttachment(
            criteria, program, CONTENT_TYPE_HTML, "events.html", false, response),
        response.getWriter());
  }

  @GetMapping(value = "/query/{program}.html+css")
  public void getQueryHtmlCss(
      @PathVariable String program,
      EventsAnalyticsQueryCriteria criteria,
      HttpServletResponse response)
      throws Exception {
    toHtmlCss(
        getListGridWithAttachment(
            criteria, program, CONTENT_TYPE_HTML, "events.html", false, response),
        response.getWriter());
  }

  @ResponseBody
  @GetMapping(
      value = "/query/dimensions",
      produces = {APPLICATION_JSON_VALUE, "application/javascript"})
  public AnalyticsDimensionsPagingWrapper<ObjectNode> getQueryDimensions(
      @RequestParam(required = false) String programId,
      @RequestParam(required = false) String programStageId,
      @RequestParam(defaultValue = "*") List<String> fields,
      DimensionsCriteria dimensionsCriteria,
      HttpServletResponse response) {
    validateRequest(programId, programStageId);

    configResponseForJson(response);

    List<PrefixedDimension> dimensions =
        eventAnalyticsDimensionsService.getQueryDimensionsByProgramStageId(
            programId, programStageId);

    List<DimensionResponse> dimResponse =
        dimensionMapperService.toDimensionResponse(
            dimensions, EventAnalyticsPrefixStrategy.of(programStageId));

    return dimensionFilteringAndPagingService.pageAndFilter(
        dimResponse, dimensionsCriteria, fields);
  }

  private void validateRequest(String programId, String programStageId)
      throws IllegalQueryException {
    if (isBlank(programId) && isBlank(programStageId)) {
      throw new IllegalQueryException(new ErrorMessage(E7235));
    }
  }

  private Grid getListGridWithAttachment(
      EventsAnalyticsQueryCriteria criteria,
      String program,
      String contentType,
      String file,
      boolean attachment,
      HttpServletResponse response) {
    EventQueryParams params = getEventQueryParams(program, criteria, false, QUERY);

    contextUtils.configureResponse(response, contentType, RESPECT_SYSTEM_SETTING, file, attachment);

    return eventQueryService.getEvents(params);
  }

  private EventQueryParams getEventQueryParams(
      String program,
      EventsAnalyticsQueryCriteria criteria,
      boolean analyzeOnly,
      EndpointAction endpointAction) {
    criteria.definePageSize(settingsProvider.getCurrentSettings().getAnalyticsMaxLimit());

    PeriodCriteriaUtils.addDefaultPeriodIfAbsent(
        criteria, settingsProvider.getCurrentSettings().getAnalysisRelativePeriod());

    EventDataQueryRequest request =
        EventDataQueryRequest.builder()
            .fromCriteria(
                (EventsAnalyticsQueryCriteria)
                    criteria.withEndpointAction(endpointAction).withEndpointItem(EVENT))
            .program(program)
            .build();

    return eventDataService.getFromRequest(request, analyzeOnly);
  }

  private void configResponseForJson(HttpServletResponse response) {
    contextUtils.configureResponse(response, CONTENT_TYPE_JSON, RESPECT_SYSTEM_SETTING);
  }
}
