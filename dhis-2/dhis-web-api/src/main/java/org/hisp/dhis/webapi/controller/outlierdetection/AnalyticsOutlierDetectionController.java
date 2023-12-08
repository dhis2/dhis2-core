/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.webapi.controller.outlierdetection;

import static org.hisp.dhis.common.cache.CacheStrategy.NO_CACHE;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_CSV;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_EXCEL;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_HTML;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_XML;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.outlierdetection.OutlierDetectionQuery;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.hisp.dhis.outlierdetection.parser.OutlierDetectionQueryParser;
import org.hisp.dhis.outlierdetection.service.AnalyticsOutlierDetectionService;
import org.hisp.dhis.validation.outlierdetection.ValidationOutlierDetectionRequest;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Analytics Outlier detection API controller.
 *
 * @author Dusan Bernat
 */
@OpenApi.Tags("analytics")
@RestController
@AllArgsConstructor
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@PreAuthorize("hasRole('ALL') or hasRole('F_RUN_VALIDATION')")
public class AnalyticsOutlierDetectionController {
  private static final String RESOURCE_PATH = "/analytics/outlierDetection";
  private final AnalyticsOutlierDetectionService outlierService;
  private final ContextUtils contextUtils;
  private final OutlierDetectionQueryParser queryParser;
  private final ValidationOutlierDetectionRequest validator;

  @GetMapping(value = RESOURCE_PATH, produces = APPLICATION_JSON_VALUE)
  public Grid getOutliersJson(OutlierDetectionQuery query) {
    OutlierDetectionRequest request = getFromQuery(query);

    Grid grid = outlierService.getOutlierValues(request);

    if (query.hasHeaders()) {
      grid.retainColumns(query.getHeaders());
    }

    return grid;
  }

  @GetMapping(value = RESOURCE_PATH + ".csv")
  public void getOutliersCsv(OutlierDetectionQuery query, HttpServletResponse response)
      throws IOException {
    OutlierDetectionRequest request = getFromQuery(query);
    contextUtils.configureResponse(response, CONTENT_TYPE_CSV, NO_CACHE, "outlierdata.csv", true);

    outlierService.getOutlierValuesAsCsv(request, response.getWriter());
  }

  @GetMapping(value = RESOURCE_PATH + ".xml")
  public void getOutliersXml(OutlierDetectionQuery query, HttpServletResponse response)
      throws IOException {
    OutlierDetectionRequest request = getFromQuery(query);
    contextUtils.configureResponse(response, CONTENT_TYPE_XML, NO_CACHE);

    outlierService.getOutlierValuesAsXml(request, response.getOutputStream());
  }

  @GetMapping(value = RESOURCE_PATH + ".xls")
  public void getOutliersXls(OutlierDetectionQuery query, HttpServletResponse response)
      throws IOException {
    OutlierDetectionRequest request = getFromQuery(query);
    contextUtils.configureResponse(response, CONTENT_TYPE_EXCEL, NO_CACHE, "outlierdata.xls", true);

    outlierService.getOutlierValuesAsXls(request, response.getOutputStream());
  }

  @GetMapping(value = RESOURCE_PATH + ".html")
  public void getOutliersHtml(OutlierDetectionQuery query, HttpServletResponse response)
      throws IOException {
    OutlierDetectionRequest request = getFromQuery(query);

    contextUtils.configureResponse(response, CONTENT_TYPE_HTML, NO_CACHE);

    outlierService.getOutlierValuesAsHtml(request, response.getWriter());
  }

  @GetMapping(value = RESOURCE_PATH + ".html+css")
  public void getOutliersHtmlCss(OutlierDetectionQuery query, HttpServletResponse response)
      throws IOException {
    OutlierDetectionRequest request = getFromQuery(query);
    contextUtils.configureResponse(response, CONTENT_TYPE_HTML, NO_CACHE);

    outlierService.getOutlierValuesAsHtmlCss(request, response.getWriter());
  }

  private OutlierDetectionRequest getFromQuery(OutlierDetectionQuery query) {
    OutlierDetectionRequest request = queryParser.getFromQuery(query);
    validator.validate(request, true);

    return request;
  }
}
