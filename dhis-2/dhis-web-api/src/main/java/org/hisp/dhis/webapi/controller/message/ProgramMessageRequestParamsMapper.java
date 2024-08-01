/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.controller.message;

import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamsValidator.validateDeprecatedParameter;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.message.ProgramMessageQueryParams;
import org.hisp.dhis.util.ObjectUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Zubair Asghar
 */
@Component
@RequiredArgsConstructor
public class ProgramMessageRequestParamsMapper {
  private final IdentifiableObjectManager manager;

  @Transactional(readOnly = true)
  public ProgramMessageQueryParams ValidateAndMap(ProgramMessageRequestParams params)
      throws ConflictException, BadRequestException {
    UID enrollmentUid =
        validateDeprecatedParameter(
            "programInstance", params.getProgramInstance(), "enrollment", params.getEnrollment());
    UID eventUid =
        validateDeprecatedParameter(
            "programStageInstance", params.getProgramStageInstance(), "event", params.getEvent());

    if (enrollmentUid == null && eventUid == null) {
      throw new ConflictException("Enrollment or Event must be specified.");
    }
    if (enrollmentUid != null && eventUid != null) {
      throw new ConflictException("Enrollment and Event cannot be processed together.");
    }

    UID entity = ObjectUtils.firstNonNull(enrollmentUid, eventUid);

    Enrollment enrollment = null;
    Event event = null;

    if (enrollmentUid != null) {
      enrollment =
          Optional.ofNullable(manager.get(Enrollment.class, entity.getValue()))
              .orElseThrow(
                  () ->
                      new IllegalQueryException(
                          String.format("Enrollment: %s does not exist.", entity.getValue())));
    }

    if (eventUid != null) {
      event =
          Optional.ofNullable(manager.get(Event.class, entity.getValue()))
              .orElseThrow(
                  () ->
                      new IllegalQueryException(
                          String.format("Event: %s does not exist.", entity.getValue())));
    }

    return ProgramMessageQueryParams.builder()
        .enrollment(enrollment)
        .event(event)
        .messageStatus(params.getMessageStatus())
        .afterDate(params.getAfterDate())
        .beforeDate(params.getBeforeDate())
        .page(params.getPage())
        .pageSize(params.getPageSize())
        .organisationUnit(params.getOrganisationUnit())
        .build();
  }
}
