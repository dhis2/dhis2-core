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
package org.hisp.dhis.outlierdetection.service;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import lombok.AllArgsConstructor;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.outlierdetection.OutlierDetectionMetadata;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.hisp.dhis.outlierdetection.OutlierDetectionResponse;
import org.hisp.dhis.outlierdetection.OutlierValue;
import org.hisp.dhis.system.util.JacksonCsvUtils;
import org.springframework.stereotype.Service;

/**
 * @author Lars Helge Overland
 */
@AllArgsConstructor
@Service
public class DefaultOutlierDetectionService {

  private final ZScoreOutlierDetectionManager zScoreOutlierDetection;
  private final MinMaxOutlierDetectionManager minMaxOutlierDetection;

  /**
   * Transform the incoming request into api response (json).
   *
   * @param request the {@link OutlierDetectionRequest}.
   * @return the {@link OutlierDetectionResponse}.
   */
  public OutlierDetectionResponse getOutlierValues(OutlierDetectionRequest request)
      throws IllegalQueryException {
    final OutlierDetectionResponse response = new OutlierDetectionResponse();
    response.setOutlierValues(getOutliers(request));
    response.setMetadata(getMetadata(request, response.getOutlierValues()));
    return response;
  }

  /**
   * Transform the incoming request into api response (csv download).
   *
   * @param request the {@link OutlierDetectionRequest}.
   * @return the {@link OutlierDetectionResponse}.
   */
  public void getOutlierValuesAsCsv(OutlierDetectionRequest request, Writer writer)
      throws IllegalQueryException, IOException {
    JacksonCsvUtils.toCsv(getOutlierValues(request).getOutlierValues(), OutlierValue.class, writer);
  }

  /**
   * Returns metadata for the given request.
   *
   * @param request the {@link OutlierDetectionRequest}.
   * @param outlierValues the list of {@link OutlierValue}.
   * @return a {@link OutlierDetectionMetadata} instance.
   */
  private OutlierDetectionMetadata getMetadata(
      OutlierDetectionRequest request, List<OutlierValue> outlierValues) {
    final OutlierDetectionMetadata metadata = new OutlierDetectionMetadata();
    metadata.setCount(outlierValues.size());
    metadata.setAlgorithm(request.getAlgorithm());
    metadata.setThreshold(request.getThreshold());
    metadata.setDataStartDate(request.getDataStartDate());
    metadata.setDataEndDate(request.getDataEndDate());
    metadata.setOrderBy(request.getOrderBy());
    metadata.setMaxResults(request.getMaxResults());
    return metadata;
  }

  /**
   * Returns outlier values using the algorithm defined in the request.
   *
   * @param request the {@link OutlierDetectionRequest}.
   * @return a list of {@link OutlierValue}.
   */
  private List<OutlierValue> getOutliers(OutlierDetectionRequest request) {
    return switch (request.getAlgorithm()) {
      case Z_SCORE, MOD_Z_SCORE -> zScoreOutlierDetection.getOutlierValues(request);
      case MIN_MAX -> minMaxOutlierDetection.getOutlierValues(request);
    };
  }
}
