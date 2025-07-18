/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.program.message;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.util.ObjectUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Zubair Asghar
 */
@Component
@RequiredArgsConstructor
public class ProgramMessageOperationParamMapper {
  private final IdentifiableObjectManager manager;

  private final ProgramService programService;

  @Transactional(readOnly = true)
  public ProgramMessageQueryParams map(ProgramMessageOperationParams operationParams)
      throws NotFoundException {
    Enrollment enrollment = getEntity(operationParams.getEnrollment(), Enrollment.class);
    TrackerEvent event = getEntity(operationParams.getEvent(), TrackerEvent.class);

    currentUserHasAccess(enrollment, event);

    return ProgramMessageQueryParams.builder()
        .enrollment(enrollment)
        .event(event)
        .afterDate(operationParams.getAfterDate())
        .messageStatus(operationParams.getMessageStatus())
        .beforeDate(operationParams.getBeforeDate())
        .page(operationParams.getPage())
        .pageSize(operationParams.getPageSize())
        .organisationUnit(operationParams.getOu())
        .build();
  }

  private <T extends BaseIdentifiableObject> T getEntity(UID entity, Class<T> klass)
      throws NotFoundException {
    if (entity == null) {
      return null;
    }

    return Optional.ofNullable(manager.get(klass, entity.getValue()))
        .orElseThrow(
            () ->
                new NotFoundException(
                    String.format(
                        "%s: %s does not exist.", klass.getSimpleName(), entity.getValue())));
  }

  private void currentUserHasAccess(Enrollment enrollment, TrackerEvent event) {
    Enrollment entity =
        ObjectUtils.firstNonNull(
            enrollment, Optional.ofNullable(event).map(TrackerEvent::getEnrollment).orElse(null));

    if (entity == null) {
      throw new IllegalQueryException("Enrollment or Event has to be provided");
    }

    List<Program> programs = programService.getCurrentUserPrograms();
    String currentUser = CurrentUserUtil.getCurrentUsername();

    if (!programs.contains(entity.getProgram())) {
      throw new IllegalQueryException(
          String.format(
              "User:%s does not have access to the required program:%s",
              currentUser, entity.getProgram().getName()));
    }
  }
}
