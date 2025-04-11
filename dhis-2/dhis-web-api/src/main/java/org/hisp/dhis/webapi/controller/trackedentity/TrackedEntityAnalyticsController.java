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
package org.hisp.dhis.webapi.controller.trackedentity;

import static org.hisp.dhis.common.DhisApiVersion.ALL;
import static org.hisp.dhis.common.DhisApiVersion.DEFAULT;
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
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.common.CommonRequestParams;
import org.hisp.dhis.analytics.common.ContextParams;
import org.hisp.dhis.analytics.common.params.CommonParsedParams;
import org.hisp.dhis.analytics.common.processing.CommonQueryRequestValidator;
import org.hisp.dhis.analytics.common.processing.Parser;
import org.hisp.dhis.analytics.common.processing.Validator;
import org.hisp.dhis.analytics.dimensions.AnalyticsDimensionsPagingWrapper;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityAnalyticsDimensionsService;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityAnalyticsQueryService;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityQueryParams;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityQueryRequestMapper;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityRequestParams;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.webapi.dimension.DimensionFilteringAndPagingService;
import org.hisp.dhis.webapi.dimension.DimensionMapperService;
import org.hisp.dhis.webapi.dimension.DimensionsCriteria;
import org.hisp.dhis.webapi.dimension.TrackedEntityAnalyticsPrefixStrategy;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller class responsible exclusively for querying operations on top of tracker entity
 * instances objects. Methods in this controller should not change any state.
 */
@OpenApi.Document(
    entity = DataValue.class,
    classifiers = {"team:analytics", "purpose:analytics"})
@ApiVersion({DEFAULT, ALL})
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analytics/trackedEntities")
class TrackedEntityAnalyticsController {
  @Nonnull private final TrackedEntityAnalyticsQueryService trackedEntityAnalyticsQueryService;

  @Nonnull private final Parser<CommonRequestParams, CommonParsedParams> commonRequestParamsParser;

  @Nonnull private final CommonQueryRequestValidator commonQueryRequestValidator;

  @Nonnull private final Validator<TrackedEntityRequestParams> trackedEntityQueryRequestValidator;

  @Nonnull private final DimensionFilteringAndPagingService dimensionFilteringAndPagingService;

  @Nonnull private final DimensionMapperService dimensionMapperService;

  @Nonnull
  private final TrackedEntityAnalyticsDimensionsService trackedEntityAnalyticsDimensionsService;

  @Nonnull private final TrackedEntityQueryRequestMapper mapper;

  @Nonnull private final ContextUtils contextUtils;

  @GetMapping(
      value = "/query/{trackedEntityType}",
      produces = {APPLICATION_JSON_VALUE, "application/javascript"})
  Grid query(
      @PathVariable String trackedEntityType,
      TrackedEntityRequestParams trackedEntityRequestParams,
      CommonRequestParams commonRequestParams) {
    return getGrid(
        trackedEntityRequestParams.withTrackedEntityType(trackedEntityType),
        commonRequestParams,
        trackedEntityAnalyticsQueryService::getGrid);
  }

  @RequiresAuthority(anyOf = F_PERFORM_ANALYTICS_EXPLAIN)
  @GetMapping(
      value = "/query/{trackedEntityType}/explain",
      produces = {APPLICATION_JSON_VALUE, "application/javascript"})
  Grid queryExplain(
      @PathVariable String trackedEntityType,
      TrackedEntityRequestParams trackedEntityRequestParams,
      CommonRequestParams commonRequestParams) {
    return getGrid(
        trackedEntityRequestParams.withTrackedEntityType(trackedEntityType),
        commonRequestParams,
        trackedEntityAnalyticsQueryService::getGridExplain);
  }

  @GetMapping(value = "/query/{trackedEntityType}.xml")
  public void queryXml(
      @PathVariable String trackedEntityType,
      TrackedEntityRequestParams trackedEntityRequestParams,
      CommonRequestParams commonRequestParams,
      HttpServletResponse response)
      throws IOException {
    prepareForDownload(response, CONTENT_TYPE_XML, "trackedEntities.xml");
    toXml(
        getGrid(
            trackedEntityRequestParams.withTrackedEntityType(trackedEntityType),
            commonRequestParams,
            trackedEntityAnalyticsQueryService::getGrid),
        response.getOutputStream());
  }

  @GetMapping(value = "/query/{trackedEntityType}.xls")
  public void queryXls(
      @PathVariable String trackedEntityType,
      TrackedEntityRequestParams trackedEntityRequestParams,
      CommonRequestParams commonRequestParams,
      HttpServletResponse response)
      throws IOException {
    prepareForDownload(response, CONTENT_TYPE_EXCEL, "trackedEntities.xls");
    toXls(
        getGrid(
            trackedEntityRequestParams.withTrackedEntityType(trackedEntityType),
            commonRequestParams,
            trackedEntityAnalyticsQueryService::getGrid),
        response.getOutputStream());
  }

  @GetMapping(value = "/query/{trackedEntityType}.xlsx")
  public void queryXlsx(
      @PathVariable String trackedEntityType,
      TrackedEntityRequestParams trackedEntityRequestParams,
      CommonRequestParams commonRequestParams,
      HttpServletResponse response)
      throws IOException {
    prepareForDownload(response, CONTENT_TYPE_EXCEL, "trackedEntities.xlsx");
    toXlsx(
        getGrid(
            trackedEntityRequestParams.withTrackedEntityType(trackedEntityType),
            commonRequestParams,
            trackedEntityAnalyticsQueryService::getGrid),
        response.getOutputStream());
  }

  @GetMapping(value = "/query/{trackedEntityType}.csv")
  public void queryCsv(
      @PathVariable String trackedEntityType,
      TrackedEntityRequestParams trackedEntityRequestParams,
      CommonRequestParams commonRequestParams,
      HttpServletResponse response)
      throws IOException {
    prepareForDownload(response, CONTENT_TYPE_CSV, "trackedEntities.csv");
    toCsv(
        getGrid(
            trackedEntityRequestParams.withTrackedEntityType(trackedEntityType),
            commonRequestParams,
            trackedEntityAnalyticsQueryService::getGrid),
        response.getWriter());
  }

  @GetMapping(value = "/query/{trackedEntityType}.html")
  public void queryHtml(
      @PathVariable String trackedEntityType,
      TrackedEntityRequestParams trackedEntityRequestParams,
      CommonRequestParams commonRequestParams,
      HttpServletResponse response)
      throws IOException {
    prepareForDownload(response, CONTENT_TYPE_HTML, "trackedEntities.html");
    toHtml(
        getGrid(
            trackedEntityRequestParams.withTrackedEntityType(trackedEntityType),
            commonRequestParams,
            trackedEntityAnalyticsQueryService::getGrid),
        response.getWriter());
  }

  @GetMapping(value = "/query/{trackedEntityType}.html+css")
  public void queryHtmlCss(
      @PathVariable String trackedEntityType,
      TrackedEntityRequestParams trackedEntityRequestParams,
      CommonRequestParams commonRequestParams,
      HttpServletResponse response)
      throws IOException {
    prepareForDownload(response, CONTENT_TYPE_HTML, "trackedEntities.html");
    toHtmlCss(
        getGrid(
            trackedEntityRequestParams.withTrackedEntityType(trackedEntityType),
            commonRequestParams,
            trackedEntityAnalyticsQueryService::getGrid),
        response.getWriter());
  }

  private Grid getGrid(
      TrackedEntityRequestParams trackedEntityRequestParams,
      CommonRequestParams commonRequestParams,
      Function<ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams>, Grid>
          executor) {

    // This happens first because of the particularity of TE, where the programs be loaded come from
    // the TE and not from the request param.
    TrackedEntityQueryParams trackedEntityQueryParams =
        mapper.map(trackedEntityRequestParams.getTrackedEntityType(), commonRequestParams);

    commonQueryRequestValidator.validate(commonRequestParams);
    trackedEntityQueryRequestValidator.validate(trackedEntityRequestParams);

    CommonParsedParams commonParsedParams = commonRequestParamsParser.parse(commonRequestParams);

    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams =
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .typedRaw(trackedEntityRequestParams)
            .typedParsed(trackedEntityQueryParams)
            .commonRaw(commonRequestParams)
            .commonParsed(commonParsedParams)
            .build();

    return executor.apply(contextParams);
  }

  /**
   * Simply defines a common response object to in download requests.
   *
   * @param response the current {@link HttpServletResponse}.
   * @param contentType the content type of the download file.
   * @param fileName the name of the file.
   */
  private void prepareForDownload(
      HttpServletResponse response, String contentType, String fileName) {
    contextUtils.configureResponse(response, contentType, RESPECT_SYSTEM_SETTING, fileName, false);
  }

  /**
   * This method returns the collection of all possible dimensions that can be applied for the given
   * "trackedEntityType".
   */
  @GetMapping("/query/dimensions")
  public AnalyticsDimensionsPagingWrapper<ObjectNode> getQueryDimensions(
      @RequestParam String trackedEntityType,
      @RequestParam(required = false) Set<String> program,
      @RequestParam(defaultValue = "*") List<String> fields,
      DimensionsCriteria dimensionsCriteria,
      HttpServletResponse response) {
    contextUtils.configureResponse(response, CONTENT_TYPE_JSON, RESPECT_SYSTEM_SETTING);

    return dimensionFilteringAndPagingService.pageAndFilter(
        dimensionMapperService.toDimensionResponse(
            trackedEntityAnalyticsDimensionsService.getQueryDimensionsByTrackedEntityTypeId(
                trackedEntityType, program),
            TrackedEntityAnalyticsPrefixStrategy.INSTANCE,
            true),
        dimensionsCriteria,
        fields);
  }
}
