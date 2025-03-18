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
package org.hisp.dhis.webapi.controller.outlierdetection;

import static org.hisp.dhis.security.Authorities.F_RUN_VALIDATION;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_CSV;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.AllArgsConstructor;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.outlierdetection.OutlierDetectionQuery;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.hisp.dhis.outlierdetection.OutlierDetectionResponse;
import org.hisp.dhis.outlierdetection.parser.OutlierDetectionQueryParser;
import org.hisp.dhis.outlierdetection.service.DefaultOutlierDetectionService;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.validation.outlierdetection.ValidationOutlierDetectionRequest;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Outlier detection API controller.
 *
 * @author Lars Helge Overland
 */
@OpenApi.Document(
    entity = DataValue.class,
    classifiers = {"team:platform", "purpose:data"})
@RestController
@AllArgsConstructor
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@RequiresAuthority(anyOf = F_RUN_VALIDATION)
public class OutlierDetectionController {

  private final DefaultOutlierDetectionService outlierService;
  private final ContextUtils contextUtils;
  private final OutlierDetectionQueryParser queryParser;
  private final ValidationOutlierDetectionRequest validator;

  @GetMapping(value = "/api/outlierDetection", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody OutlierDetectionResponse getOutliersJson(OutlierDetectionQuery query) {
    OutlierDetectionRequest request = getFromQuery(query);

    return outlierService.getOutlierValues(request);
  }

  @GetMapping(value = "/api/outlierDetection.csv")
  public void getOutliersCsv(OutlierDetectionQuery query, HttpServletResponse response)
      throws IOException {
    OutlierDetectionRequest request = getFromQuery(query);
    contextUtils.configureResponse(
        response, CONTENT_TYPE_CSV, CacheStrategy.NO_CACHE, "outlierdata.csv", true);

    outlierService.getOutlierValuesAsCsv(request, response.getWriter());
  }

  private OutlierDetectionRequest getFromQuery(OutlierDetectionQuery query) {
    OutlierDetectionRequest request = queryParser.getFromQuery(query);
    validator.validate(request, false);

    return request;
  }
}
