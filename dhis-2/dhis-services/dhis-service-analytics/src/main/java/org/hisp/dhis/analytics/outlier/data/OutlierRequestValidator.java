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
package org.hisp.dhis.analytics.outlier.data;

import static org.hisp.dhis.analytics.OutlierDetectionAlgorithm.MIN_MAX;
import static org.hisp.dhis.feedback.ErrorCode.E2200;
import static org.hisp.dhis.feedback.ErrorCode.E2201;
import static org.hisp.dhis.feedback.ErrorCode.E2202;
import static org.hisp.dhis.feedback.ErrorCode.E2204;
import static org.hisp.dhis.feedback.ErrorCode.E2205;
import static org.hisp.dhis.feedback.ErrorCode.E2206;
import static org.hisp.dhis.feedback.ErrorCode.E2207;
import static org.hisp.dhis.feedback.ErrorCode.E2209;
import static org.hisp.dhis.feedback.ErrorCode.E2210;
import static org.hisp.dhis.feedback.ErrorCode.E2211;
import static org.hisp.dhis.feedback.ErrorCode.E2212;
import static org.hisp.dhis.feedback.ErrorCode.E2213;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.OutlierDetectionAlgorithm;
import org.hisp.dhis.analytics.outlier.Order;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.springframework.stereotype.Component;

/** OutlierDetectionRequest validator. */
@Component
@AllArgsConstructor
@Slf4j
public class OutlierRequestValidator {

  public static final int DEFAULT_LIMIT = 500;

  private final SystemSettingsProvider settingsProvider;

  /**
   * Validates the given request.
   *
   * @param request the {@link OutlierRequest}.
   * @throws IllegalQueryException if request is invalid.
   */
  public void validate(OutlierRequest request, boolean isAnalytics) throws IllegalQueryException {
    ErrorMessage errorMessage = validateForErrorMessage(request, isAnalytics);

    if (errorMessage != null) {
      log.warn(
          "Outlier detection request validation failed, code: '{}', message: '{}'",
          errorMessage.getErrorCode(),
          errorMessage.getMessage());

      throw new IllegalQueryException(errorMessage);
    }
  }

  private ErrorMessage validateForErrorMessage(OutlierRequest request, boolean isAnalytics) {
    int maxLimit =
        isAnalytics ? settingsProvider.getCurrentSettings().getAnalyticsMaxLimit() : DEFAULT_LIMIT;
    ErrorMessage errorMessage = getErrorMessage(request, maxLimit);

    if (errorMessage != null) {
      return errorMessage;
    }

    if (isAnalytics) {
      if (request.getDataStartDate() != null) {
        return new ErrorMessage(E2209);
      }
      if (request.getDataEndDate() != null) {
        return new ErrorMessage(E2210);
      }
      if (request.getAlgorithm() == MIN_MAX) {
        return new ErrorMessage(E2211);
      }
    }

    if (request.hasDataStartEndDate()
        && request.getDataStartDate().after(request.getDataEndDate())) {
      return new ErrorMessage(E2207);
    }

    return null;
  }

  private ErrorMessage getErrorMessage(OutlierRequest request, int maxLimit) {
    ErrorMessage error = null;

    if (request.getDataDimensions().isEmpty()) {
      error = new ErrorMessage(E2200);
    } else if (!request.hasStartEndDate() && !request.hasPeriods()) {
      error = new ErrorMessage(E2201);
    } else if (request.hasStartEndDate() && request.hasPeriods()) {
      error = new ErrorMessage(E2212);
    } else if (request.hasStartEndDate() && request.getStartDate().after(request.getEndDate())) {
      error = new ErrorMessage(E2202);
    } else if (request.getThreshold() <= 0) {
      error = new ErrorMessage(E2204);
    } else if (request.getMaxResults() <= 0) {
      error = new ErrorMessage(E2205);
    } else if (request.getMaxResults() > maxLimit) {
      error = new ErrorMessage(E2206, maxLimit);
    } else if (request.getAlgorithm() == OutlierDetectionAlgorithm.MODIFIED_Z_SCORE
        && (request.getOrderBy() == Order.Z_SCORE
            || request.getOrderBy() == Order.MEAN
            || request.getOrderBy() == Order.STD_DEV)) {
      error = new ErrorMessage(E2213, request.getAlgorithm());
    } else if (request.getAlgorithm() == OutlierDetectionAlgorithm.Z_SCORE
        && (request.getOrderBy() == Order.MODIFIED_Z_SCORE
            || request.getOrderBy() == Order.MEDIAN
            || request.getOrderBy() == Order.MEDIAN_ABS_DEV)) {
      error = new ErrorMessage(E2213, request.getAlgorithm());
    }

    return error;
  }
}
