/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.webapi.controller.outlierdetection;

import static org.hisp.dhis.common.cache.CacheStrategy.NO_CACHE;
import static org.hisp.dhis.security.Authorities.F_PERFORM_ANALYTICS_EXPLAIN;
import static org.hisp.dhis.security.Authorities.F_RUN_VALIDATION;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_CSV;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_EXCEL;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_HTML;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_XML;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.AllArgsConstructor;
import org.hisp.dhis.analytics.outlier.data.OutlierQueryParams;
import org.hisp.dhis.analytics.outlier.data.OutlierQueryParser;
import org.hisp.dhis.analytics.outlier.data.OutlierRequest;
import org.hisp.dhis.analytics.outlier.data.OutlierRequestValidator;
import org.hisp.dhis.analytics.outlier.service.AnalyticsOutlierService;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Analytics Outlier detection API controller.
 *
 * @author Dusan Bernat
 */
@OpenApi.Document(
    entity = DataValue.class,
    classifiers = {"team:analytics", "purpose:data"})
@RestController
@AllArgsConstructor
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@RequiresAuthority(anyOf = F_RUN_VALIDATION)
public class AnalyticsOutlierDetectionController {
  private static final String RESOURCE_PATH = "/api/analytics/outlierDetection";
  private final AnalyticsOutlierService outlierService;
  private final ContextUtils contextUtils;
  private final OutlierQueryParser queryParser;
  private final OutlierRequestValidator validator;

  @RequiresAuthority(anyOf = F_PERFORM_ANALYTICS_EXPLAIN)
  @GetMapping(
      value = RESOURCE_PATH + "/explain",
      produces = {APPLICATION_JSON_VALUE, "application/javascript"})
  public @ResponseBody Grid getExplainOutliersJson(OutlierQueryParams query) {
    outlierService.checkAnalyticsTableForOutliers();
    OutlierRequest request = getFromQuery(query, true);

    return outlierService.getOutliersPerformanceMetrics(request);
  }

  @GetMapping(value = RESOURCE_PATH, produces = APPLICATION_JSON_VALUE)
  public Grid getOutliersJson(OutlierQueryParams queryParams) {
    outlierService.checkAnalyticsTableForOutliers();
    OutlierRequest request = getFromQuery(queryParams, false);

    Grid grid = outlierService.getOutliers(request);

    if (queryParams.hasHeaders()) {
      grid.retainColumns(queryParams.getHeaders());
    }

    return grid;
  }

  @GetMapping(value = RESOURCE_PATH + ".csv")
  public void getOutliersCsv(OutlierQueryParams queryParams, HttpServletResponse response)
      throws IOException {
    outlierService.checkAnalyticsTableForOutliers();
    OutlierRequest request = getFromQuery(queryParams, false);
    contextUtils.configureResponse(response, CONTENT_TYPE_CSV, NO_CACHE, "outlierdata.csv", true);

    outlierService.getOutlierAsCsv(request, response.getWriter());
  }

  @GetMapping(value = RESOURCE_PATH + ".xml")
  public void getOutliersXml(OutlierQueryParams queryParams, HttpServletResponse response)
      throws IOException {
    outlierService.checkAnalyticsTableForOutliers();
    OutlierRequest request = getFromQuery(queryParams, false);
    contextUtils.configureResponse(response, CONTENT_TYPE_XML, NO_CACHE);

    outlierService.getOutliersAsXml(request, response.getOutputStream());
  }

  @GetMapping(value = RESOURCE_PATH + ".xls")
  public void getOutliersXls(OutlierQueryParams queryParams, HttpServletResponse response)
      throws IOException {
    outlierService.checkAnalyticsTableForOutliers();
    OutlierRequest request = getFromQuery(queryParams, false);
    contextUtils.configureResponse(response, CONTENT_TYPE_EXCEL, NO_CACHE, "outlierdata.xls", true);

    outlierService.getOutliersAsXls(request, response.getOutputStream());
  }

  @GetMapping(value = RESOURCE_PATH + ".xlsx")
  public void getOutliersXlsx(OutlierQueryParams queryParams, HttpServletResponse response)
      throws IOException {
    outlierService.checkAnalyticsTableForOutliers();
    OutlierRequest request = getFromQuery(queryParams, false);
    contextUtils.configureResponse(
        response, CONTENT_TYPE_EXCEL, NO_CACHE, "outlierdata.xlsx", true);

    outlierService.getOutliersAsXlsx(request, response.getOutputStream());
  }

  @GetMapping(value = RESOURCE_PATH + ".html")
  public void getOutliersHtml(OutlierQueryParams queryParams, HttpServletResponse response)
      throws IOException {
    outlierService.checkAnalyticsTableForOutliers();
    OutlierRequest request = getFromQuery(queryParams, false);

    contextUtils.configureResponse(response, CONTENT_TYPE_HTML, NO_CACHE);

    outlierService.getOutliersAsHtml(request, response.getWriter());
  }

  @GetMapping(value = RESOURCE_PATH + ".html+css")
  public void getOutliersHtmlCss(OutlierQueryParams queryParams, HttpServletResponse response)
      throws IOException {
    outlierService.checkAnalyticsTableForOutliers();
    OutlierRequest request = getFromQuery(queryParams, false);
    contextUtils.configureResponse(response, CONTENT_TYPE_HTML, NO_CACHE);

    outlierService.getOutliersAsHtmlCss(request, response.getWriter());
  }

  private OutlierRequest getFromQuery(OutlierQueryParams queryParams, boolean analyzeOnly) {
    OutlierRequest request = queryParser.getFromQuery(queryParams, analyzeOnly);
    validator.validate(request, true);

    return request;
  }
}
