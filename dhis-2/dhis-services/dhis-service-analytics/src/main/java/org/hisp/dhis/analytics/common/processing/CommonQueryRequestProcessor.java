/*
 * Copyright (c) 2004-2004, University of Oslo
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
package org.hisp.dhis.analytics.common.processing;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension.ENROLLMENT_STATUS;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension.EVENT_STATUS;
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_IDENTIFIER_SEP;
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_NAME_SEP;
import static org.hisp.dhis.feedback.ErrorCode.E7140;
import static org.hisp.dhis.feedback.ErrorCode.E7141;
import static org.hisp.dhis.setting.SettingKey.ANALYTICS_MAX_LIMIT;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.analytics.common.CommonQueryRequest;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dxf2.deprecated.tracker.enrollment.EnrollmentStatus;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.setting.SystemSettingManager;
import org.springframework.stereotype.Component;

/** Processor class for CommonQueryRequest objects. */
@Component
@RequiredArgsConstructor
public class CommonQueryRequestProcessor implements Processor<CommonQueryRequest> {
  private final SystemSettingManager systemSettingManager;

  private final List<Function<CommonQueryRequest, CommonQueryRequest>> processors =
      List.of(this::computePagingParams, this::computeEnrollmentStatus, this::computeEventStatus);

  /**
   * Based on the given query request object {@link CommonQueryRequest}, this method will
   * process/compute existing values in the request object, and populated all necessary attributes
   * of this same object.
   *
   * @param commonQueryRequest the {@link CommonQueryRequest} to process.
   * @return the processed {@link CommonQueryRequest}.
   */
  @Nonnull
  @Override
  public CommonQueryRequest process(@Nonnull CommonQueryRequest commonQueryRequest) {
    /* applies all processors */
    return processors.stream()
        .reduce(Function::andThen)
        .orElse(Function.identity())
        .apply(commonQueryRequest);
  }

  /**
   * Apply paging parameters to the given {@link CommonQueryRequest} object, taking into account the
   * system setting for the maximum limit and the ignoreLimit flag.
   *
   * @param commonQueryRequest the {@link CommonQueryRequest} used to compute the paging.
   * @return the computed {@link CommonQueryRequest}.
   */
  private CommonQueryRequest computePagingParams(CommonQueryRequest commonQueryRequest) {
    int maxLimit = systemSettingManager.getIntSetting(ANALYTICS_MAX_LIMIT);
    boolean unlimited = maxLimit == 0;
    boolean ignoreLimit = commonQueryRequest.isIgnoreLimit();
    boolean hasMaxLimit = !unlimited && !ignoreLimit;

    if (commonQueryRequest.isPaging()) {
      boolean pageSizeOverMaxLimit = commonQueryRequest.getPageSize() > maxLimit;

      if (hasMaxLimit && pageSizeOverMaxLimit) {
        return commonQueryRequest.withPageSize(maxLimit);
      }

      return commonQueryRequest;
    } else {
      if (unlimited) {
        return commonQueryRequest.withIgnoreLimit(true);
      }

      return commonQueryRequest.withPageSize(maxLimit);
    }
  }

  /**
   * Converts the program status into a static dimension example:
   * enrollmentStatus=IpHINAT79UW.COMPLETED;IpHINAT79UW.ACTIVE becomes
   * dimension=IpHINAT79UW.PROGRAM_STATUS:COMPLETED;ACTIVE
   *
   * @param commonQueryRequest the {@link CommonQueryRequest} to transform
   * @return the transformed {@link CommonQueryRequest}
   */
  private CommonQueryRequest computeEnrollmentStatus(CommonQueryRequest commonQueryRequest) {
    if (commonQueryRequest.hasProgramStatus() || commonQueryRequest.hasEnrollmentStatus()) {
      // merging programStatus and enrollmentStatus into a single set
      Set<String> enrollmentStatuses = new LinkedHashSet<>();
      enrollmentStatuses.addAll(commonQueryRequest.getProgramStatus());
      enrollmentStatuses.addAll(commonQueryRequest.getEnrollmentStatus());

      commonQueryRequest.getDimension().addAll(enrollmentStatusAsDimension(enrollmentStatuses));
    }
    return commonQueryRequest;
  }

  /**
   * Converts the event status into a static dimension example:
   * eventStatus=IpHINAT79UW.A03MvHHogjR.SCHEDULE;IpHINAT79UW.A03MvHHogjR.ACTIVE becomes
   * dimension=IpHINAT79UW.A03MvHHogjR.EVENT_STATUS:SCHEDULE;ACTIVE
   *
   * @param commonQueryRequest the {@link CommonQueryRequest} to transform
   * @return the transformed {@link CommonQueryRequest}
   */
  private CommonQueryRequest computeEventStatus(CommonQueryRequest commonQueryRequest) {
    if (commonQueryRequest.hasEventStatus()) {
      commonQueryRequest
          .getDimension()
          .addAll(eventStatusAsDimension(commonQueryRequest.getEventStatus()));
    }
    return commonQueryRequest;
  }

  private Collection<String> eventStatusAsDimension(Set<String> eventStatuses) {
    // builds a map of [program,program stage] with a list of event statuses
    Map<Pair<String, String>, List<EventStatus>> statusesByProgramAndProgramStage =
        eventStatuses.stream()
            .map(eventStatus -> splitAndValidate(eventStatus, 3, E7141))
            .map(parts -> Pair.of(Pair.of(parts[0], parts[1]), EventStatus.valueOf(parts[2])))
            .collect(groupingBy(Pair::getLeft, mapping(Pair::getRight, Collectors.toList())));

    return statusesByProgramAndProgramStage.keySet().stream()
        .map(
            programWithStage ->
                programWithStage.getLeft()
                    + // program
                    DIMENSION_IDENTIFIER_SEP
                    + programWithStage.getRight()
                    + // program stage
                    DIMENSION_IDENTIFIER_SEP
                    + EVENT_STATUS.name()
                    + // "EVENT_STATUS"
                    DIMENSION_NAME_SEP
                    +
                    // ";" concatenated values - for example "COMPLETED;SKIPPED"
                    statusesByProgramAndProgramStage.get(programWithStage).stream()
                        .map(EventStatus::name)
                        .collect(Collectors.joining(";")))
        .collect(Collectors.toList());
  }

  private List<String> enrollmentStatusAsDimension(Set<String> enrollmentStatuses) {
    // builds a map of [program] with a list of program (enrollment) statuses
    Map<String, List<EnrollmentStatus>> statusesByProgram =
        enrollmentStatuses.stream()
            .map(enrollmentStatus -> splitAndValidate(enrollmentStatus, 2, E7140))
            .map(parts -> Pair.of(parts[0], EnrollmentStatus.valueOf(parts[1])))
            .collect(groupingBy(Pair::getLeft, mapping(Pair::getRight, Collectors.toList())));

    return statusesByProgram.keySet().stream()
        .map(
            program ->
                program
                    + DIMENSION_IDENTIFIER_SEP
                    + ENROLLMENT_STATUS.name()
                    + DIMENSION_NAME_SEP
                    + statusesByProgram.get(program).stream()
                        .map(EnrollmentStatus::name)
                        .collect(Collectors.joining(";")))
        .collect(Collectors.toList());
  }

  /**
   * Splits the given parameter by the dot character and validates that the resulting array has the
   * given length. If the length is not correct, an {@link IllegalQueryException} is thrown.
   *
   * @param parameter the parameter to split
   * @param allowedLength the allowed length of the resulting array
   * @param errorCode the error code to use in case of error
   * @return the resulting array
   */
  private String[] splitAndValidate(String parameter, int allowedLength, ErrorCode errorCode) {
    String[] parts = parameter.split("\\.");
    if (parts.length != allowedLength) {
      throw new IllegalQueryException(new ErrorMessage(errorCode));
    }
    return parts;
  }
}
