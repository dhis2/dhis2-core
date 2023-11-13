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
package org.hisp.dhis.validation.outlierdetection;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.outlierdetection.OutlierDetectionAlgorithm;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class ValidationOutlierDetectionRequest {

  private final SystemSettingManager systemSettingManager;

  /**
   * Validates the given request.
   *
   * @param request the {@link OutlierDetectionRequest}.
   * @throws IllegalQueryException if request is invalid.
   */
  public void validate(OutlierDetectionRequest request, boolean isAnalytics)
      throws IllegalQueryException {
    ErrorMessage errorMessage = validateForErrorMessage(request, isAnalytics);

    if (errorMessage != null) {
      log.warn(
          String.format(
              "Outlier detection request validation failed, code: '%s', message: '%s'",
              errorMessage.getErrorCode(), errorMessage.getMessage()));

      throw new IllegalQueryException(errorMessage);
    }
  }

  private ErrorMessage validateForErrorMessage(
      OutlierDetectionRequest request, boolean isAnalytics) {

    int maxLimit =
        isAnalytics
            ? systemSettingManager.getSystemSetting(SettingKey.ANALYTICS_MAX_LIMIT, Integer.class)
            : 500;
    ErrorMessage errorMessage = getErrorMessage(request, maxLimit);

    if (errorMessage != null) {
      return errorMessage;
    }

    if (isAnalytics) {
      if (request.getDataStartDate() != null) {
        return new ErrorMessage(ErrorCode.E2209);
      }
      if (request.getDataEndDate() != null) {
        return new ErrorMessage(ErrorCode.E2210);
      }
      if (request.getAlgorithm() == OutlierDetectionAlgorithm.MIN_MAX) {
        return new ErrorMessage(ErrorCode.E2211);
      }
    }

    if (request.hasDataStartEndDate()
        && request.getDataStartDate().after(request.getDataEndDate())) {
      return new ErrorMessage(ErrorCode.E2207);
    }

    return null;
  }

  private ErrorMessage getErrorMessage(OutlierDetectionRequest request, int maxLimit) {
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
    } else if (request.getMaxResults() > maxLimit) {
      error = new ErrorMessage(ErrorCode.E2206, maxLimit);
    }

    return error;
  }
}
