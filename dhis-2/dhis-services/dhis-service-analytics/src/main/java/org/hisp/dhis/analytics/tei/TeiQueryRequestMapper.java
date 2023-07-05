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
package org.hisp.dhis.analytics.tei;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.analytics.tei.query.TeiFields.getTrackedEntityAttributes;
import static org.hisp.dhis.feedback.ErrorCode.E7125;
import static org.hisp.dhis.feedback.ErrorCode.E7142;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.common.QueryRequest;
import org.hisp.dhis.analytics.common.processing.CommonQueryRequestMapper;
import org.hisp.dhis.analytics.common.processing.Processor;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.springframework.stereotype.Component;

/**
 * This component maps {@link QueryRequest} objects into query params objects. The query params
 * objects represents the preparation of dimensions and elements that are ready to be queried at
 * database level.
 */
@Component
@RequiredArgsConstructor
public class TeiQueryRequestMapper {
  private final CommonQueryRequestMapper commonQueryRequestMapper;

  private final TrackedEntityTypeService trackedEntityTypeService;

  private final Processor<TeiQueryParams> teiQueryParamPostProcessor;

  private final ProgramService programService;

  /**
   * Maps incoming query requests into a valid and usable {@link TeiQueryParams}.
   *
   * @param queryRequest the {@link QueryRequest} of type {@link TeiQueryRequest}.
   * @return the populated {@link TeiQueryParams}.
   * @throws IllegalQueryException if the current TrackedEntityType specified in the given request
   *     is invalid or non-existent.
   */
  public TeiQueryParams map(QueryRequest<TeiQueryRequest> queryRequest) {
    TrackedEntityType trackedEntityType =
        getTrackedEntityType(queryRequest.getRequest().getTrackedEntityType());

    checkTetForPrograms(queryRequest, trackedEntityType);

    // Adding tracked entity type attributes to the list of dimensions.
    queryRequest
        .getCommonQueryRequest()
        .getDimension()
        .addAll(
            getTrackedEntityAttributes(trackedEntityType)
                .map(IdentifiableObject::getUid)
                .collect(toList()));

    return teiQueryParamPostProcessor.process(
        TeiQueryParams.builder()
            .trackedEntityType(trackedEntityType)
            .commonParams(commonQueryRequestMapper.map(queryRequest.getCommonQueryRequest()))
            .build());
  }

  /**
   * Checks if the given programs are valid for the given tracked entity type.
   *
   * @param queryRequest the query request.
   * @param trackedEntityType the tracked entity type.
   */
  private void checkTetForPrograms(
      QueryRequest<TeiQueryRequest> queryRequest, TrackedEntityType trackedEntityType) {
    Set<String> programUids = queryRequest.getCommonQueryRequest().getProgram();

    Set<String> nonMatchingProgramUids =
        programService.getPrograms(programUids).stream()
            .filter(program -> !matchesTet(program, trackedEntityType))
            .map(program -> program.getName() + " (" + program.getUid() + ")")
            .collect(Collectors.toSet());

    if (isNotEmpty(nonMatchingProgramUids)) {
      throw new IllegalQueryException(
          E7142,
          nonMatchingProgramUids,
          trackedEntityType.getName() + " (" + trackedEntityType.getUid() + ")");
    }
  }

  /**
   * Checks if the given program TET is equals to tracked entity type.
   *
   * @param program the program.
   * @param trackedEntityType the tracked entity type uid.
   * @return true if the program matches the tracked entity type.
   */
  private boolean matchesTet(Program program, TrackedEntityType trackedEntityType) {
    return StringUtils.equals(program.getTrackedEntityType().getUid(), trackedEntityType.getUid());
  }

  /**
   * Simply loads the given tracked entity type. If nothing is found, it throws an exception.
   *
   * @param trackedEntityTypeUid the tracked entity type uid.
   * @throws IllegalQueryException if the tracked entity type specified is invalid or non-existent.
   */
  private TrackedEntityType getTrackedEntityType(String trackedEntityTypeUid) {
    return Optional.of(trackedEntityTypeUid)
        .map(trackedEntityTypeService::getTrackedEntityType)
        .orElseThrow(() -> new IllegalQueryException(E7125, trackedEntityTypeUid));
  }
}
