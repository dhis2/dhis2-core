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
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outlierdetection.OutlierDetectionMetadata;
import org.hisp.dhis.outlierdetection.OutlierDetectionQuery;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.hisp.dhis.outlierdetection.OutlierDetectionResponse;
import org.hisp.dhis.outlierdetection.OutlierDetectionService;
import org.hisp.dhis.outlierdetection.OutlierValue;
import org.hisp.dhis.system.util.JacksonCsvUtils;
import org.springframework.stereotype.Service;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@Service
@AllArgsConstructor
public class DefaultOutlierDetectionService implements OutlierDetectionService {
  private static final int MAX_LIMIT = 10_000;

  private final IdentifiableObjectManager idObjectManager;

  private final ZScoreOutlierDetectionManager zScoreOutlierDetection;

  private final MinMaxOutlierDetectionManager minMaxOutlierDetection;

  @Override
  public void validate(OutlierDetectionRequest request) throws IllegalQueryException {
    ErrorMessage error = validateForErrorMessage(request);

    if (error != null) {
      log.warn(
          String.format(
              "Outlier detection request validation failed, code: '%s', message: '%s'",
              error.getErrorCode(), error.getMessage()));

      throw new IllegalQueryException(error);
    }
  }

  @Override
  public ErrorMessage validateForErrorMessage(OutlierDetectionRequest request) {
    ErrorMessage error = null;

    if (request.getDataElements().isEmpty()) {
      error = new ErrorMessage(ErrorCode.E2200);
    } else if (request.getStartDate() == null || request.getEndDate() == null) {
      error = new ErrorMessage(ErrorCode.E2201);
    } else if (request.getStartDate().after(request.getEndDate())) {
      error = new ErrorMessage(ErrorCode.E2202);
    } else if (request.getOrgUnits().isEmpty()) {
      error = new ErrorMessage(ErrorCode.E2203);
    } else if (request.getThreshold() <= 0) {
      error = new ErrorMessage(ErrorCode.E2204);
    } else if (request.getMaxResults() <= 0) {
      error = new ErrorMessage(ErrorCode.E2205);
    } else if (request.getMaxResults() > MAX_LIMIT) {
      error = new ErrorMessage(ErrorCode.E2206, MAX_LIMIT);
    } else if (request.hasDataStartEndDate()
        && request.getDataStartDate().after(request.getDataEndDate())) {
      error = new ErrorMessage(ErrorCode.E2207);
    }

    return error;
  }

  @Override
  public OutlierDetectionRequest getFromQuery(OutlierDetectionQuery query) {
    OutlierDetectionRequest.Builder request = new OutlierDetectionRequest.Builder();

    List<DataSet> dataSets = idObjectManager.getByUid(DataSet.class, query.getDs());

    // Re-fetch data elements to maintain access control

    List<String> de =
        dataSets.stream()
            .map(DataSet::getDataElements)
            .flatMap(Collection::stream)
            .filter(d -> d.getValueType().isNumeric())
            .map(DataElement::getUid)
            .collect(Collectors.toList());

    de.addAll(query.getDe());

    List<DataElement> dataElements = idObjectManager.getByUid(DataElement.class, de);
    List<OrganisationUnit> orgUnits =
        idObjectManager.getByUid(OrganisationUnit.class, query.getOu());

    request
        .withDataElements(dataElements)
        .withStartEndDate(query.getStartDate(), query.getEndDate())
        .withOrgUnits(orgUnits)
        .withDataStartDate(query.getDataStartDate())
        .withDataEndDate(query.getDataEndDate());

    if (query.getAlgorithm() != null) {
      request.withAlgorithm(query.getAlgorithm());
    }

    if (query.getThreshold() != null) {
      request.withThreshold(query.getThreshold());
    }

    if (query.getOrderBy() != null) {
      request.withOrderBy(query.getOrderBy());
    }

    if (query.getMaxResults() != null) {
      request.withMaxResults(query.getMaxResults());
    }

    return request.build();
  }

  @Override
  public OutlierDetectionResponse getOutlierValues(OutlierDetectionRequest request)
      throws IllegalQueryException {
    validate(request);

    final OutlierDetectionResponse response = new OutlierDetectionResponse();
    response.setOutlierValues(getOutliers(request));
    response.setMetadata(getMetadata(request, response.getOutlierValues()));
    return response;
  }

  @Override
  public void getOutlierValuesAsCsv(OutlierDetectionRequest request, OutputStream out)
      throws IllegalQueryException, IOException {
    JacksonCsvUtils.toCsv(getOutlierValues(request).getOutlierValues(), OutlierValue.class, out);
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
    switch (request.getAlgorithm()) {
      case Z_SCORE:
      case MOD_Z_SCORE:
        return zScoreOutlierDetection.getOutlierValues(request);
      case MIN_MAX:
        return minMaxOutlierDetection.getOutlierValues(request);
      default:
        throw new IllegalStateException(
            String.format("Outlier detection algorithm not supported: %s", request.getAlgorithm()));
    }
  }
}
