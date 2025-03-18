/*
 * Copyright (c) 2004-2004, University of Oslo
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
package org.hisp.dhis.analytics.common.processing;

import static org.apache.commons.lang3.StringUtils.countMatches;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.throwIllegalQueryEx;
import static org.hisp.dhis.common.CodeGenerator.isValidUid;
import static org.hisp.dhis.feedback.ErrorCode.E4014;
import static org.hisp.dhis.feedback.ErrorCode.E7139;
import static org.hisp.dhis.feedback.ErrorCode.E7222;

import java.util.Set;
import java.util.function.Consumer;
import org.hisp.dhis.analytics.common.CommonRequestParams;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.collection.CollectionUtils;
import org.hisp.dhis.feedback.ErrorMessage;
import org.springframework.stereotype.Component;

/**
 * Component responsible for generic validations on top of a {@link CommonRequestParams} object. It
 * must act only on top of raw incoming requests params.
 */
@Component
public class CommonQueryRequestValidator implements Validator<CommonRequestParams> {
  /**
   * Runs a validation on the given query request object {@link CommonRequestParams}, preventing
   * basic syntax and consistency issues.
   *
   * @param commonRequestParams the {@link CommonRequestParams}.
   * @throws IllegalQueryException is some invalid state is found.
   */
  @Override
  public void validate(CommonRequestParams commonRequestParams) {
    for (String programUid : commonRequestParams.getProgram()) {
      if (!isValidUid(programUid)) {
        throw new IllegalQueryException(new ErrorMessage(E4014, programUid, "program"));
      }
    }

    validateEnrollmentDate(commonRequestParams.getEnrollmentDate());
    validateEventDate(commonRequestParams.getEventDate());

    if (commonRequestParams.hasProgramStatus() && commonRequestParams.hasEnrollmentStatus()) {
      throw new IllegalQueryException(new ErrorMessage(E7139));
    }

    checkAllowedDimensions(commonRequestParams.getAllDimensions());
  }

  /**
   * Validates the given list of dates using the given validator.
   *
   * @param dates the list of dates to validate.
   * @param validator the validator to use.
   */
  private void validateDates(Set<String> dates, Consumer<String> validator) {
    CollectionUtils.emptyIfNull(dates).forEach(validator);
  }

  /**
   * Validates the given list of event dates.
   *
   * @param eventDates the list of event dates to validate.
   */
  private void validateEventDate(Set<String> eventDates) {
    validateDates(eventDates, this::validateEventDate);
  }

  /**
   * The event date should have a format like: "IpHINAT79UW.A03MvHHogjR.LAST_YEAR"
   *
   * @param eventDate the date to validate.
   * @throws IllegalQueryException if the format is invalid.
   */
  private void validateEventDate(String eventDate) {
    if (isNotBlank(eventDate)) {
      boolean invalidPeriodValue = countMatches(eventDate, ".") != 2;

      if (invalidPeriodValue) {
        throw new IllegalQueryException(new ErrorMessage(E4014, eventDate, "eventDate"));
      }
    }
  }

  /**
   * Validates the given list of event dates.
   *
   * @param enrollmentDates the list of event dates to validate.
   */
  private void validateEnrollmentDate(Set<String> enrollmentDates) {
    validateDates(enrollmentDates, this::validateEnrollmentDate);
  }

  /**
   * The event date should have a format like: "IpHINAT79UW.LAST_YEAR".
   *
   * @param enrollmentDate the date to validate.
   * @throws IllegalQueryException if the format is invalid.
   */
  private void validateEnrollmentDate(String enrollmentDate) {
    if (isNotBlank(enrollmentDate)) {
      boolean invalidPeriodValue = countMatches(enrollmentDate, ".") != 1;

      if (invalidPeriodValue) {
        throw new IllegalQueryException(new ErrorMessage(E4014, enrollmentDate, "enrollmentDate"));
      }
    }
  }

  /**
   * Looks for invalid or unsupported queries/filters/dimensions.
   *
   * @param dimensions the collection of dimensions to check.
   * @throws IllegalQueryException if some invalid scenario is found.
   */
  private void checkAllowedDimensions(Set<String> dimensions) {
    dimensions.forEach(
        dim -> {
          // The "pe" dimension is not supported for TE queries.
          if (containsPe(dim)) {
            throwIllegalQueryEx(E7222, dim);
          }
        });
  }

  private boolean containsPe(String dimension) {
    return dimension.contains("pe:");
  }
}
