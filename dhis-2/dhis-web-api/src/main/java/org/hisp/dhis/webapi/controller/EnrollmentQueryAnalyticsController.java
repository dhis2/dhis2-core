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

import static org.hisp.dhis.common.DhisApiVersion.ALL;
import static org.hisp.dhis.common.DhisApiVersion.DEFAULT;
import static org.hisp.dhis.common.RequestTypeAware.EndpointAction.QUERY;
import static org.hisp.dhis.common.RequestTypeAware.EndpointItem.ENROLLMENT;
import static org.hisp.dhis.common.cache.CacheStrategy.RESPECT_SYSTEM_SETTING;
import static org.hisp.dhis.period.PeriodDataProvider.DataSource.DATABASE;
import static org.hisp.dhis.period.PeriodDataProvider.DataSource.SYSTEM_DEFINED;
import static org.hisp.dhis.security.Authorities.F_PERFORM_ANALYTICS_EXPLAIN;
import static org.hisp.dhis.system.grid.GridUtils.toCsv;
import static org.hisp.dhis.system.grid.GridUtils.toHtml;
import static org.hisp.dhis.system.grid.GridUtils.toHtmlCss;
import static org.hisp.dhis.system.grid.GridUtils.toXls;
import static org.hisp.dhis.system.grid.GridUtils.toXlsx;
import static org.hisp.dhis.system.grid.GridUtils.toXml;
import static org.hisp.dhis.webapi.dimension.EnrollmentAnalyticsPrefixStrategy.INSTANCE;
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
import lombok.SneakyThrows;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.dimensions.AnalyticsDimensionsPagingWrapper;
import org.hisp.dhis.analytics.event.EnrollmentAnalyticsDimensionsService;
import org.hisp.dhis.analytics.event.EventDataQueryService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.EnrollmentQueryService;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableSettings;
import org.hisp.dhis.analytics.util.AnalyticsPeriodCriteriaUtils;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.DimensionsCriteria;
import org.hisp.dhis.common.EnrollmentAnalyticsQueryCriteria;
import org.hisp.dhis.common.EventDataQueryRequest;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.webapi.dimension.DimensionFilteringAndPagingService;
import org.hisp.dhis.webapi.dimension.DimensionMapperService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@OpenApi.Document(
    entity = Enrollment.class,
    classifiers = {"team:analytics", "purpose:analytics"})
@Controller
@ApiVersion({DEFAULT, ALL})
@RequestMapping("/api/analytics/enrollments/query")
@AllArgsConstructor
public class EnrollmentQueryAnalyticsController {
  @Nonnull private final EventDataQueryService eventDataQueryService;

  @Nonnull private final EnrollmentQueryService enrollmentQueryService;

  @Nonnull private final ContextUtils contextUtils;

  @Nonnull private final ExecutionPlanStore executionPlanStore;

  @Nonnull private DimensionFilteringAndPagingService dimensionFilteringAndPagingService;

  @Nonnull private EnrollmentAnalyticsDimensionsService enrollmentAnalyticsDimensionsService;

  @Nonnull private DimensionMapperService dimensionMapperService;

  @Nonnull private final SystemSettingsProvider settingsProvider;

  @Nonnull private final PeriodDataProvider periodDataProvider;

  @Nonnull private final AnalyticsTableSettings analyticsTableSettings;

  @RequiresAuthority(anyOf = F_PERFORM_ANALYTICS_EXPLAIN)
  @GetMapping(
      value = "/{program}/explain",
      produces = {APPLICATION_JSON_VALUE, "application/javascript"})
  public @ResponseBody Grid getExplainQueryJson( // JSON, JSONP
      @PathVariable String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    EventQueryParams params = getEventQueryParams(program, criteria, apiVersion, true);

    Grid grid = enrollmentQueryService.getEnrollments(params);
    contextUtils.configureResponse(response, CONTENT_TYPE_JSON, RESPECT_SYSTEM_SETTING);

    if (params.analyzeOnly()) {
      String key = params.getExplainOrderId();
      grid.addPerformanceMetrics(executionPlanStore.getExecutionPlans(key));
    }

    return grid;
  }

  @GetMapping(
      value = "/{program}",
      produces = {APPLICATION_JSON_VALUE, "application/javascript"})
  public @ResponseBody Grid getQueryJson( // JSON, JSONP
      @PathVariable String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    EventQueryParams params = getEventQueryParams(program, criteria, apiVersion, false);

    contextUtils.configureResponse(response, CONTENT_TYPE_JSON, RESPECT_SYSTEM_SETTING);

    return enrollmentQueryService.getEnrollments(params);
  }

  @SneakyThrows
  @GetMapping("/{program}.xml")
  public void getQueryXml(
      @PathVariable String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    EventQueryParams params = getEventQueryParams(program, criteria, apiVersion, false);

    contextUtils.configureResponse(
        response, CONTENT_TYPE_XML, RESPECT_SYSTEM_SETTING, "enrollments.xml", false);
    Grid grid = enrollmentQueryService.getEnrollments(params);
    toXml(grid, response.getOutputStream());
  }

  @SneakyThrows
  @GetMapping("/{program}.xls")
  public void getQueryXls(
      @PathVariable String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    EventQueryParams params = getEventQueryParams(program, criteria, apiVersion, false);

    contextUtils.configureResponse(
        response, CONTENT_TYPE_EXCEL, RESPECT_SYSTEM_SETTING, "enrollments.xls", true);
    Grid grid = enrollmentQueryService.getEnrollments(params);
    toXls(grid, response.getOutputStream());
  }

  @SneakyThrows
  @GetMapping("/{program}.xlsx")
  public void getQueryXlsx(
      @PathVariable String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    EventQueryParams params = getEventQueryParams(program, criteria, apiVersion, false);

    contextUtils.configureResponse(
        response, CONTENT_TYPE_EXCEL, RESPECT_SYSTEM_SETTING, "enrollments.xlsx", true);
    Grid grid = enrollmentQueryService.getEnrollments(params);
    toXlsx(grid, response.getOutputStream());
  }

  @SneakyThrows
  @GetMapping("/{program}.csv")
  public void getQueryCsv(
      @PathVariable String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    EventQueryParams params = getEventQueryParams(program, criteria, apiVersion, false);

    contextUtils.configureResponse(
        response, CONTENT_TYPE_CSV, RESPECT_SYSTEM_SETTING, "enrollments.csv", true);
    Grid grid = enrollmentQueryService.getEnrollments(params);
    toCsv(grid, response.getWriter());
  }

  @SneakyThrows
  @GetMapping("/{program}.html")
  public void getQueryHtml(
      @PathVariable String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    EventQueryParams params = getEventQueryParams(program, criteria, apiVersion, false);

    contextUtils.configureResponse(
        response, CONTENT_TYPE_HTML, RESPECT_SYSTEM_SETTING, "enrollments.html", false);
    Grid grid = enrollmentQueryService.getEnrollments(params);
    toHtml(grid, response.getWriter());
  }

  @SneakyThrows
  @GetMapping("/{program}.html+css")
  public void getQueryHtmlCss(
      @PathVariable String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      HttpServletResponse response) {
    EventQueryParams params = getEventQueryParams(program, criteria, apiVersion, false);

    contextUtils.configureResponse(
        response, CONTENT_TYPE_HTML, RESPECT_SYSTEM_SETTING, "enrollments.html", false);
    Grid grid = enrollmentQueryService.getEnrollments(params);
    toHtmlCss(grid, response.getWriter());
  }

  @ResponseBody
  @GetMapping("/dimensions")
  public AnalyticsDimensionsPagingWrapper<ObjectNode> getQueryDimensions(
      @RequestParam String programId,
      @RequestParam(defaultValue = "*") List<String> fields,
      DimensionsCriteria dimensionsCriteria,
      HttpServletResponse response) {
    contextUtils.configureResponse(response, CONTENT_TYPE_JSON, RESPECT_SYSTEM_SETTING);
    return dimensionFilteringAndPagingService.pageAndFilter(
        dimensionMapperService.toDimensionResponse(
            enrollmentAnalyticsDimensionsService.getQueryDimensionsByProgramId(programId),
            INSTANCE),
        dimensionsCriteria,
        fields);
  }

  private EventQueryParams getEventQueryParams(
      @PathVariable String program,
      EnrollmentAnalyticsQueryCriteria criteria,
      DhisApiVersion apiVersion,
      boolean analyzeOnly) {
    criteria.definePageSize(settingsProvider.getCurrentSettings().getAnalyticsMaxLimit());

    AnalyticsPeriodCriteriaUtils.defineDefaultPeriodForCriteria(
        criteria,
        periodDataProvider,
        analyticsTableSettings.getMaxPeriodYearsOffset() == null ? SYSTEM_DEFINED : DATABASE);

    EventDataQueryRequest request =
        EventDataQueryRequest.builder()
            .fromCriteria(
                (EnrollmentAnalyticsQueryCriteria)
                    criteria.withEndpointAction(QUERY).withEndpointItem(ENROLLMENT))
            .program(program)
            .apiVersion(apiVersion)
            .build();

    return eventDataQueryService.getFromRequest(request, analyzeOnly);
  }
}
