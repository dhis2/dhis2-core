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

import static org.hisp.dhis.common.RequestTypeAware.EndpointAction.AGGREGATE;
import static org.hisp.dhis.common.RequestTypeAware.EndpointAction.QUERY;
import static org.hisp.dhis.period.PeriodDataProvider.DataSource.DATABASE;
import static org.hisp.dhis.period.PeriodDataProvider.DataSource.SYSTEM_DEFINED;
import static org.hisp.dhis.security.Authorities.F_PERFORM_ANALYTICS_EXPLAIN;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.dimensions.AnalyticsDimensionsPagingWrapper;
import org.hisp.dhis.analytics.event.EnrollmentAnalyticsDimensionsService;
import org.hisp.dhis.analytics.event.EnrollmentAnalyticsService;
import org.hisp.dhis.analytics.event.EventDataQueryService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableSettings;
import org.hisp.dhis.analytics.util.AnalyticsPeriodCriteriaUtils;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.DimensionsCriteria;
import org.hisp.dhis.common.EnrollmentAnalyticsQueryCriteria;
import org.hisp.dhis.common.EventDataQueryRequest;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.RequestTypeAware;
import org.hisp.dhis.common.RequestTypeAware.EndpointAction;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.grid.GridUtils;
import org.hisp.dhis.util.PeriodCriteriaUtils;
import org.hisp.dhis.webapi.dimension.DimensionFilteringAndPagingService;
import org.hisp.dhis.webapi.dimension.DimensionMapperService;
import org.hisp.dhis.webapi.dimension.EnrollmentAnalyticsPrefixStrategy;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Markus Bekken
 */
@OpenApi.Document(domain = DataValue.class)
@Controller
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@RequestMapping("/api/analytics/enrollments")
@AllArgsConstructor
public class EnrollmentAnalyticsController {
  @Nonnull private final EventDataQueryService eventDataQueryService;

  @Nonnull private final EnrollmentAnalyticsService analyticsService;

  @Nonnull private final ContextUtils contextUtils;

  @Nonnull private final ExecutionPlanStore executionPlanStore;

  @Nonnull private DimensionFilteringAndPagingService dimensionFilteringAndPagingService;

  @Nonnull private EnrollmentAnalyticsDimensionsService enrollmentAnalyticsDimensionsService;

  @Nonnull private DimensionMapperService dimensionMapperService;

  @Nonnull private final SystemSettingManager systemSettingManager;

  @Nonnull private final PeriodDataProvider periodDataProvider;

  @Nonnull private final AnalyticsTableSettings analyticsTableSettings;

  @RequiresAuthority(anyOf = F_PERFORM_ANALYTICS_EXPLAIN)
  @GetMapping(
      value = "/aggregate/{program}/explain",
      produces = {APPLICATION_JSON_VALUE, "application/javascript"})
  public @ResponseBody Grid getExplainAggregateJson( // JSON, JSONP
      @PathVariable String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    EventQueryParams params = getEventQueryParams(program, criteria, apiVersion, true, AGGREGATE);

    Grid grid = analyticsService.getEnrollments(params);
    contextUtils.configureResponse(
        response, ContextUtils.CONTENT_TYPE_JSON, CacheStrategy.RESPECT_SYSTEM_SETTING);

    if (params.analyzeOnly()) {
      String key = params.getExplainOrderId();
      grid.addPerformanceMetrics(executionPlanStore.getExecutionPlans(key));
    }

    return grid;
  }

  @GetMapping(
      value = "/aggregate/{program}",
      produces = {APPLICATION_JSON_VALUE, "application/javascript"})
  public @ResponseBody Grid getAggregateJson( // JSON, JSONP
      @PathVariable String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    EventQueryParams params = getEventQueryParams(program, criteria, apiVersion, false, AGGREGATE);

    contextUtils.configureResponse(
        response, ContextUtils.CONTENT_TYPE_JSON, CacheStrategy.RESPECT_SYSTEM_SETTING);

    return analyticsService.getEnrollments(params);
  }

  @OpenApi.Ignore
  @Deprecated(since = "2.42", forRemoval = true)
  @SneakyThrows
  @GetMapping("/aggregate/{program}.xml")
  public void getAggregateXmlOutdated(
      @PathVariable String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    getAggregateXml(program, criteria, apiVersion, response);
  }

  @OpenApi.Response(String.class)
  @SneakyThrows
  @GetMapping(value = "/aggregate/enrollments.xml")
  public void getAggregateXml(
      @OpenApi.Param({UID.class, Program.class}) @RequestParam String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    EventQueryParams params = getEventQueryParams(program, criteria, apiVersion, false, AGGREGATE);

    contextUtils.configureResponse(
        response,
        ContextUtils.CONTENT_TYPE_XML,
        CacheStrategy.RESPECT_SYSTEM_SETTING,
        "enrollments.xml",
        false);
    Grid grid = analyticsService.getEnrollments(params);
    GridUtils.toXml(grid, response.getOutputStream());
  }

  @OpenApi.Ignore
  @Deprecated(since = "2.42", forRemoval = true)
  @SneakyThrows
  @GetMapping("/aggregate/{program}.xls")
  public void getAggregateXlsOutdated(
      @PathVariable String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    getAggregateXls(program, criteria, apiVersion, response);
  }

  @OpenApi.Response(String.class)
  @SneakyThrows
  @GetMapping(value = "/aggregate/enrollments.xls")
  public void getAggregateXls(
      @OpenApi.Param({UID.class, Program.class}) @RequestParam String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    EventQueryParams params = getEventQueryParams(program, criteria, apiVersion, false, AGGREGATE);

    contextUtils.configureResponse(
        response,
        ContextUtils.CONTENT_TYPE_EXCEL,
        CacheStrategy.RESPECT_SYSTEM_SETTING,
        "enrollments.xls",
        true);
    Grid grid = analyticsService.getEnrollments(params);
    GridUtils.toXls(grid, response.getOutputStream());
  }

  @OpenApi.Ignore
  @Deprecated(since = "2.42", forRemoval = true)
  @SneakyThrows
  @GetMapping("/aggregate/{program}.csv")
  public void getAggregateCsvOutdated(
      @PathVariable String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    getAggregateCsv(program, criteria, apiVersion, response);
  }

  @OpenApi.Response(String.class)
  @SneakyThrows
  @GetMapping(value = "/aggregate/enrollments.csv")
  public void getAggregateCsv(
      @OpenApi.Param({UID.class, Program.class}) @RequestParam String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    EventQueryParams params = getEventQueryParams(program, criteria, apiVersion, false, AGGREGATE);

    contextUtils.configureResponse(
        response,
        ContextUtils.CONTENT_TYPE_CSV,
        CacheStrategy.RESPECT_SYSTEM_SETTING,
        "enrollments.csv",
        true);
    Grid grid = analyticsService.getEnrollments(params);
    GridUtils.toCsv(grid, response.getWriter());
  }

  @OpenApi.Ignore
  @Deprecated(since = "2.42", forRemoval = true)
  @SneakyThrows
  @GetMapping("/aggregate/{program}.html")
  public void getAggregateHtmlOutdated(
      @PathVariable String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    getAggregateHtml(program, criteria, apiVersion, response);
  }

  @OpenApi.Response(String.class)
  @SneakyThrows
  @GetMapping(value = "/aggregate/enrollments.html")
  public void getAggregateHtml(
      @OpenApi.Param({UID.class, Program.class}) @RequestParam String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    EventQueryParams params = getEventQueryParams(program, criteria, apiVersion, false, AGGREGATE);

    contextUtils.configureResponse(
        response,
        ContextUtils.CONTENT_TYPE_HTML,
        CacheStrategy.RESPECT_SYSTEM_SETTING,
        "enrollments.html",
        false);
    Grid grid = analyticsService.getEnrollments(params);
    GridUtils.toHtml(grid, response.getWriter());
  }

  @OpenApi.Ignore
  @Deprecated(since = "2.42", forRemoval = true)
  @SneakyThrows
  @GetMapping("/aggregate/{program}.html+css")
  public void getAggregateHtmlCssOutdated(
      @PathVariable String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    getAggregateHtmlCss(program, criteria, apiVersion, response);
  }

  @OpenApi.Response(String.class)
  @SneakyThrows
  @GetMapping(value = "/aggregate/enrollments.html+css")
  public void getAggregateHtmlCss(
      @OpenApi.Param({UID.class, Program.class}) @RequestParam String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    EventQueryParams params = getEventQueryParams(program, criteria, apiVersion, false, AGGREGATE);

    contextUtils.configureResponse(
        response,
        ContextUtils.CONTENT_TYPE_HTML,
        CacheStrategy.RESPECT_SYSTEM_SETTING,
        "enrollments.html",
        false);
    Grid grid = analyticsService.getEnrollments(params);
    GridUtils.toHtmlCss(grid, response.getWriter());
  }

  @RequiresAuthority(anyOf = F_PERFORM_ANALYTICS_EXPLAIN)
  @GetMapping(
      value = "/query/{program}/explain",
      produces = {APPLICATION_JSON_VALUE, "application/javascript"})
  public @ResponseBody Grid getExplainQueryJson( // JSON, JSONP
      @PathVariable String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    EventQueryParams params = getEventQueryParams(program, criteria, apiVersion, true, QUERY);

    Grid grid = analyticsService.getEnrollments(params);
    contextUtils.configureResponse(
        response, ContextUtils.CONTENT_TYPE_JSON, CacheStrategy.RESPECT_SYSTEM_SETTING);

    if (params.analyzeOnly()) {
      String key = params.getExplainOrderId();
      grid.addPerformanceMetrics(executionPlanStore.getExecutionPlans(key));
    }

    return grid;
  }

  @GetMapping(
      value = "/query/{program}",
      produces = {APPLICATION_JSON_VALUE, "application/javascript"})
  public @ResponseBody Grid getQueryJson( // JSON, JSONP
      @PathVariable String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    EventQueryParams params = getEventQueryParams(program, criteria, apiVersion, false, QUERY);

    contextUtils.configureResponse(
        response, ContextUtils.CONTENT_TYPE_JSON, CacheStrategy.RESPECT_SYSTEM_SETTING);

    return analyticsService.getEnrollments(params);
  }

  @OpenApi.Ignore
  @Deprecated(since = "2.42", forRemoval = true)
  @SneakyThrows
  @GetMapping("/query/{program}.xml")
  public void getQueryXmlOutdated(
      @PathVariable String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    getQueryXml(program, criteria, apiVersion, response);
  }

  @OpenApi.Response(String.class)
  @SneakyThrows
  @GetMapping("/query/enrollments.xml")
  public void getQueryXml(
      @OpenApi.Param({UID.class, Program.class}) @RequestParam String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    EventQueryParams params = getEventQueryParams(program, criteria, apiVersion, false, QUERY);

    contextUtils.configureResponse(
        response,
        ContextUtils.CONTENT_TYPE_XML,
        CacheStrategy.RESPECT_SYSTEM_SETTING,
        "enrollments.xml",
        false);
    Grid grid = analyticsService.getEnrollments(params);
    GridUtils.toXml(grid, response.getOutputStream());
  }

  @OpenApi.Ignore
  @Deprecated(since = "2.42", forRemoval = true)
  @SneakyThrows
  @GetMapping("/query/{program}.xls")
  public void getQueryXlsOutdated(
      @PathVariable String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    getQueryXls(program, criteria, apiVersion, response);
  }

  @OpenApi.Response(String.class)
  @SneakyThrows
  @GetMapping("/query/enrollments.xls")
  public void getQueryXls(
      @OpenApi.Param({UID.class, Program.class}) @RequestParam String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    EventQueryParams params = getEventQueryParams(program, criteria, apiVersion, false, QUERY);

    contextUtils.configureResponse(
        response,
        ContextUtils.CONTENT_TYPE_EXCEL,
        CacheStrategy.RESPECT_SYSTEM_SETTING,
        "enrollments.xls",
        true);
    Grid grid = analyticsService.getEnrollments(params);
    GridUtils.toXls(grid, response.getOutputStream());
  }

  @OpenApi.Ignore
  @Deprecated(since = "2.42", forRemoval = true)
  @SneakyThrows
  @GetMapping("/query/{program}.csv")
  public void getQueryCsvOutdated(
      @PathVariable String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    getQueryCsv(program, criteria, apiVersion, response);
  }

  @OpenApi.Response(String.class)
  @SneakyThrows
  @GetMapping("/query/enrollments.csv")
  public void getQueryCsv(
      @OpenApi.Param({UID.class, Program.class}) @RequestParam String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    EventQueryParams params = getEventQueryParams(program, criteria, apiVersion, false, QUERY);

    contextUtils.configureResponse(
        response,
        ContextUtils.CONTENT_TYPE_CSV,
        CacheStrategy.RESPECT_SYSTEM_SETTING,
        "enrollments.csv",
        true);
    Grid grid = analyticsService.getEnrollments(params);
    GridUtils.toCsv(grid, response.getWriter());
  }

  @OpenApi.Ignore
  @Deprecated(since = "2.42", forRemoval = true)
  @SneakyThrows
  @GetMapping("/query/{program}.html")
  public void getQueryHtmlOutdated(
      @PathVariable String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    getQueryHtml(program, criteria, apiVersion, response);
  }

  @OpenApi.Response(String.class)
  @SneakyThrows
  @GetMapping("/query/enrollments.html")
  public void getQueryHtml(
      @OpenApi.Param({UID.class, Program.class}) @RequestParam String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    EventQueryParams params = getEventQueryParams(program, criteria, apiVersion, false, QUERY);

    contextUtils.configureResponse(
        response,
        ContextUtils.CONTENT_TYPE_HTML,
        CacheStrategy.RESPECT_SYSTEM_SETTING,
        "enrollments.html",
        false);
    Grid grid = analyticsService.getEnrollments(params);
    GridUtils.toHtml(grid, response.getWriter());
  }

  @OpenApi.Ignore
  @Deprecated(since = "2.42", forRemoval = true)
  @SneakyThrows
  @GetMapping("/query/{program}.html+css")
  public void getQueryHtmlCssOutdated(
      @PathVariable String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    getQueryHtmlCss(program, criteria, apiVersion, response);
  }

  @OpenApi.Response(String.class)
  @SneakyThrows
  @GetMapping("/query/enrollments.html+css")
  public void getQueryHtmlCss(
      @OpenApi.Param({UID.class, Program.class}) @RequestParam String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    EventQueryParams params = getEventQueryParams(program, criteria, apiVersion, false, QUERY);

    contextUtils.configureResponse(
        response,
        ContextUtils.CONTENT_TYPE_HTML,
        CacheStrategy.RESPECT_SYSTEM_SETTING,
        "enrollments.html",
        false);
    Grid grid = analyticsService.getEnrollments(params);
    GridUtils.toHtmlCss(grid, response.getWriter());
  }

  @ResponseBody
  @GetMapping("/query/dimensions")
  public AnalyticsDimensionsPagingWrapper<ObjectNode> getQueryDimensions(
      @RequestParam String programId,
      @RequestParam(defaultValue = "*") List<String> fields,
      DimensionsCriteria dimensionsCriteria,
      HttpServletResponse response) {
    contextUtils.configureResponse(
        response, ContextUtils.CONTENT_TYPE_JSON, CacheStrategy.RESPECT_SYSTEM_SETTING);
    return dimensionFilteringAndPagingService.pageAndFilter(
        dimensionMapperService.toDimensionResponse(
            enrollmentAnalyticsDimensionsService.getQueryDimensionsByProgramId(programId),
            EnrollmentAnalyticsPrefixStrategy.INSTANCE),
        dimensionsCriteria,
        fields);
  }

  @ResponseBody
  @GetMapping("/aggregate/dimensions")
  public AnalyticsDimensionsPagingWrapper<ObjectNode> getAggregateDimensions(
      @RequestParam String programId,
      @RequestParam(defaultValue = "*") List<String> fields,
      DimensionsCriteria dimensionsCriteria,
      HttpServletResponse response) {
    contextUtils.configureResponse(
        response, ContextUtils.CONTENT_TYPE_JSON, CacheStrategy.RESPECT_SYSTEM_SETTING);
    return dimensionFilteringAndPagingService.pageAndFilter(
        dimensionMapperService.toDimensionResponse(
            enrollmentAnalyticsDimensionsService.getAggregateDimensionsByProgramStageId(programId),
            EnrollmentAnalyticsPrefixStrategy.INSTANCE),
        dimensionsCriteria,
        fields);
  }

  private EventQueryParams getEventQueryParams(
      @PathVariable String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      boolean analyzeOnly,
      EndpointAction endpointAction) {
    criteria.definePageSize(systemSettingManager.getIntSetting(SettingKey.ANALYTICS_MAX_LIMIT));

    if (endpointAction == QUERY) {
      AnalyticsPeriodCriteriaUtils.defineDefaultPeriodForCriteria(
          criteria,
          periodDataProvider,
          analyticsTableSettings.getMaxPeriodYearsOffset() == null ? SYSTEM_DEFINED : DATABASE);
    } else {
      PeriodCriteriaUtils.defineDefaultPeriodForCriteria(
          criteria,
          systemSettingManager.getSystemSetting(
              SettingKey.ANALYSIS_RELATIVE_PERIOD, RelativePeriodEnum.class));
    }

    EventDataQueryRequest request =
        EventDataQueryRequest.builder()
            .fromCriteria(
                (EnrollmentAnalyticsQueryCriteria)
                    criteria
                        .withEndpointAction(endpointAction)
                        .withEndpointItem(RequestTypeAware.EndpointItem.ENROLLMENT))
            .program(program)
            .apiVersion(apiVersion)
            .build();

    return eventDataQueryService.getFromRequest(request, analyzeOnly);
  }
}
