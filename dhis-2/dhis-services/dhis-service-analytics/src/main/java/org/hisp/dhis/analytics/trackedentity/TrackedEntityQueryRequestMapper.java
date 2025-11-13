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
package org.hisp.dhis.analytics.trackedentity;

import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.analytics.trackedentity.query.TrackedEntityFields.getTrackedEntityAttributes;
import static org.hisp.dhis.feedback.ErrorCode.E7125;
import static org.hisp.dhis.feedback.ErrorCode.E7142;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Strings;
import org.hisp.dhis.analytics.common.CommonRequestParams;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.springframework.stereotype.Component;

/**
 * This component maps creates a @{TrackedEntityQueryParams} query params objects. The query params
 * objects represents the preparation of dimensions and elements that are ready to be queried at
 * database level.
 */
@Component
@RequiredArgsConstructor
public class TrackedEntityQueryRequestMapper {
  private final TrackedEntityTypeService trackedEntityTypeService;

  private final ProgramService programService;

  /**
   * Maps incoming query requests into a valid and usable {@link TrackedEntityQueryParams}. Be aware
   * that it changes the state of the given {@link CommonRequestParams} in specific cases.
   *
   * @param requestParams the {@link CommonRequestParams}.
   * @return the populated {@link TrackedEntityQueryParams}.
   * @throws IllegalQueryException if the current TrackedEntityType specified in the given request
   *     is invalid or non-existent.
   */
  public TrackedEntityQueryParams map(
      String trackedEntityTypeUid, CommonRequestParams requestParams) {
    TrackedEntityType trackedEntityType = getTrackedEntityType(trackedEntityTypeUid);

    if (!requestParams.hasPrograms()) {
      requestParams.setProgram(getProgramUidsFromTrackedEntityType(trackedEntityType));
      requestParams.getInternal().setRequestPrograms(false);
    } else {
      checkTetForPrograms(requestParams.getProgram(), trackedEntityType);
      requestParams.getInternal().setRequestPrograms(true);
    }

    // Adding tracked entity type attributes to the list of dimensions.
    requestParams
        .getInternal()
        .getEntityTypeAttributes()
        .addAll(
            getTrackedEntityAttributes(trackedEntityType).map(IdentifiableObject::getUid).toList());

    return TrackedEntityQueryParams.builder().trackedEntityType(trackedEntityType).build();
  }

  private Set<String> getProgramUidsFromTrackedEntityType(TrackedEntityType trackedEntityType) {
    return programService.getAllPrograms().stream()
        .filter(program -> matchesTet(program, trackedEntityType))
        .map(Program::getUid)
        .collect(toSet());
  }

  /**
   * Checks if the given programs are valid for the given tracked entity type.
   *
   * @param programs the collection of programs.
   * @param trackedEntityType the {@link TrackedEntityType}.
   */
  private void checkTetForPrograms(Set<String> programs, TrackedEntityType trackedEntityType) {
    Set<String> nonMatchingProgramUids =
        programService.getPrograms(programs).stream()
            .filter(program -> !matchesTet(program, trackedEntityType))
            .map(program -> program.getName() + " (" + program.getUid() + ")")
            .collect(toSet());

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
    return Objects.nonNull(program.getTrackedEntityType())
        && Strings.CS.equals(program.getTrackedEntityType().getUid(), trackedEntityType.getUid());
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
