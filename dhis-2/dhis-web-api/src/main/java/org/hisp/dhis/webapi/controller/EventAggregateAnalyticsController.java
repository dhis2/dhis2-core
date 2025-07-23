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

import static org.hisp.dhis.common.DimensionalObjectUtils.getItemsFromParam;
import static org.hisp.dhis.common.RequestTypeAware.EndpointAction.AGGREGATE;
import static org.hisp.dhis.common.RequestTypeAware.EndpointItem.EVENT;
import static org.hisp.dhis.common.cache.CacheStrategy.RESPECT_SYSTEM_SETTING;
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
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.dimensions.AnalyticsDimensionsPagingWrapper;
import org.hisp.dhis.analytics.event.EventAnalyticsDimensionsService;
import org.hisp.dhis.analytics.event.EventDataQueryService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.EventAggregateService;
import org.hisp.dhis.common.EventDataQueryRequest;
import org.hisp.dhis.common.EventsAnalyticsQueryCriteria;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.PrefixedDimension;
import org.hisp.dhis.common.RequestTypeAware.EndpointAction;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.setting.SystemSettings;
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
@RequestMapping("/api/analytics/events/aggregate")
public class EventAggregateAnalyticsController {

  @Nonnull private final EventDataQueryService eventDataService;

  @Nonnull private final EventAggregateService eventAggregationService;

  @Nonnull private final ContextUtils contextUtils;

  @Nonnull private final DimensionFilteringAndPagingService dimensionFilteringAndPagingService;

  @Nonnull private final EventAnalyticsDimensionsService eventAnalyticsDimensionsService;

  @Nonnull private final ExecutionPlanStore executionPlanStore;

  @Nonnull private final DimensionMapperService dimensionMapperService;

  @Nonnull private final SystemSettingsProvider settingsProvider;

  @RequiresAuthority(anyOf = F_PERFORM_ANALYTICS_EXPLAIN)
  @GetMapping(
      value = "/{program}/explain",
      produces = {APPLICATION_JSON_VALUE, "application/javascript"})
  public @ResponseBody Grid getExplainAggregateJson( // JSON, JSONP
      @PathVariable String program,
      EventsAnalyticsQueryCriteria criteria,
      HttpServletResponse response) {
    EventQueryParams params = getEventQueryParams(program, criteria, true, AGGREGATE);

    configResponseForJson(response);

    Grid grid =
        eventAggregationService.getAggregatedData(
            params,
            getItemsFromParam(criteria.getColumns()),
            getItemsFromParam(criteria.getRows()));

    if (params.analyzeOnly()) {
      grid.addPerformanceMetrics(executionPlanStore.getExecutionPlans(params.getExplainOrderId()));
    }

    return grid;
  }

  @GetMapping(
      value = "/{program}",
      produces = {APPLICATION_JSON_VALUE, "application/javascript"})
  public @ResponseBody Grid getAggregateJson( // JSON, JSONP
      @PathVariable String program,
      EventsAnalyticsQueryCriteria criteria,
      HttpServletResponse response) {
    EventQueryParams params = getEventQueryParams(program, criteria, false, AGGREGATE);

    configResponseForJson(response);

    return eventAggregationService.getAggregatedData(
        params, getItemsFromParam(criteria.getColumns()), getItemsFromParam(criteria.getRows()));
  }

  @GetMapping(value = "/{program}.xml")
  public void getAggregateXml(
      @PathVariable String program,
      EventsAnalyticsQueryCriteria criteria,
      HttpServletResponse response)
      throws Exception {
    toXml(
        getAggregatedGridWithAttachment(
            criteria, program, CONTENT_TYPE_XML, "events.xml", response),
        response.getOutputStream());
  }

  @GetMapping(value = "/{program}.xls")
  public void getAggregateXls(
      @PathVariable String program,
      EventsAnalyticsQueryCriteria criteria,
      HttpServletResponse response)
      throws Exception {
    toXls(
        getAggregatedGridWithAttachment(
            criteria, program, CONTENT_TYPE_EXCEL, "events.xls", response),
        response.getOutputStream());
  }

  @GetMapping(value = "/{program}.xlsx")
  public void getAggregateXlsx(
      @PathVariable String program,
      EventsAnalyticsQueryCriteria criteria,
      HttpServletResponse response)
      throws Exception {
    toXlsx(
        getAggregatedGridWithAttachment(
            criteria, program, CONTENT_TYPE_EXCEL, "events.xlsx", response),
        response.getOutputStream());
  }

  @GetMapping(value = "/{program}.csv")
  public void getAggregateCsv(
      @PathVariable String program,
      EventsAnalyticsQueryCriteria criteria,
      HttpServletResponse response)
      throws Exception {
    toCsv(
        getAggregatedGridWithAttachment(
            criteria, program, CONTENT_TYPE_CSV, "events.csv", response),
        response.getWriter());
  }

  @GetMapping(value = "/{program}.html")
  public void getAggregateHtml(
      @PathVariable String program,
      EventsAnalyticsQueryCriteria criteria,
      HttpServletResponse response)
      throws Exception {
    toHtml(
        getAggregatedGridWithAttachment(
            criteria, program, CONTENT_TYPE_HTML, "events.html", response),
        response.getWriter());
  }

  @GetMapping(value = "/{program}.html+css")
  public void getAggregateHtmlCss(
      @PathVariable String program,
      EventsAnalyticsQueryCriteria criteria,
      HttpServletResponse response)
      throws Exception {
    toHtmlCss(
        getAggregatedGridWithAttachment(
            criteria, program, CONTENT_TYPE_HTML, "events.html", response),
        response.getWriter());
  }

  @ResponseBody
  @GetMapping(
      value = "/dimensions",
      produces = {APPLICATION_JSON_VALUE, "application/javascript"})
  public AnalyticsDimensionsPagingWrapper<ObjectNode> getAggregateDimensions(
      @RequestParam String programStageId,
      @RequestParam(defaultValue = "*") List<String> fields,
      DimensionsCriteria dimensionsCriteria,
      HttpServletResponse response) {
    configResponseForJson(response);

    List<PrefixedDimension> dimensions =
        eventAnalyticsDimensionsService.getAggregateDimensionsByProgramStageId(programStageId);

    List<DimensionResponse> dimResponse =
        dimensionMapperService.toDimensionResponse(
            dimensions, EventAnalyticsPrefixStrategy.of(programStageId));

    return dimensionFilteringAndPagingService.pageAndFilter(
        dimResponse, dimensionsCriteria, fields);
  }

  private Grid getAggregatedGridWithAttachment(
      EventsAnalyticsQueryCriteria criteria,
      String program,
      String contentType,
      String file,
      HttpServletResponse response) {
    EventQueryParams params = getEventQueryParams(program, criteria, false, AGGREGATE);

    contextUtils.configureResponse(response, contentType, RESPECT_SYSTEM_SETTING, file, false);

    return eventAggregationService.getAggregatedData(
        params, getItemsFromParam(criteria.getColumns()), getItemsFromParam(criteria.getRows()));
  }

  private EventQueryParams getEventQueryParams(
      String program,
      EventsAnalyticsQueryCriteria criteria,
      boolean analyzeOnly,
      EndpointAction endpointAction) {
    SystemSettings settings = settingsProvider.getCurrentSettings();
    criteria.definePageSize(settings.getAnalyticsMaxLimit());

    PeriodCriteriaUtils.addDefaultPeriodIfAbsent(criteria, settings.getAnalysisRelativePeriod());

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
