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
package org.hisp.dhis.webapi.controller.notification;

import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.notification.NotificationPagingParam;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateParam;
import org.springframework.stereotype.Component;

/**
 * @author Zubair Asghar
 */
@Component
@RequiredArgsConstructor
public class ProgramNotificationTemplateRequestParamsMapper {
  private final IdentifiableObjectManager manager;

  public ProgramNotificationTemplateParam validateAndMap(
      ProgramNotificationTemplateRequestParams requestParams)
      throws ConflictException, BadRequestException {

    validateRequestParams(requestParams);

    Program program = getEntity(requestParams.getProgram(), Program.class);
    ProgramStage programStage = getEntity(requestParams.getProgramStage(), ProgramStage.class);

    boolean isPaged = determinePaging(requestParams);

    return ProgramNotificationTemplateParam.builder()
        .program(program)
        .programStage(programStage)
        .skipPaging(!isPaged)
        .paged(isPaged)
        .page(
            isPaged
                ? Objects.requireNonNullElse(
                    requestParams.getPage(), NotificationPagingParam.DEFAULT_PAGE)
                : null)
        .pageSize(
            isPaged
                ? Objects.requireNonNullElse(
                    requestParams.getPageSize(), NotificationPagingParam.DEFAULT_PAGE_SIZE)
                : null)
        .build();
  }

  private void validateRequestParams(ProgramNotificationTemplateRequestParams requestParams)
      throws ConflictException, BadRequestException {

    if (requestParams.getProgram() == null && requestParams.getProgramStage() == null) {
      throw new ConflictException("Program or ProgramStage must be specified.");
    }

    if (requestParams.getProgram() != null && requestParams.getProgramStage() != null) {
      throw new ConflictException("Program and ProgramStage cannot be processed together.");
    }

    if (requestParams.getPaging() != null
        && requestParams.getSkipPaging() != null
        && requestParams.getPaging().equals(requestParams.getSkipPaging())) {
      throw new BadRequestException(
          "Paging can either be enabled or disabled. Prefer 'paging' as 'skipPaging' will be removed.");
    }
  }

  private <T extends BaseIdentifiableObject> T getEntity(UID objectId, Class<T> klass)
      throws IllegalQueryException {
    if (objectId == null) {
      return null;
    }

    return Optional.ofNullable(manager.get(klass, objectId.getValue()))
        .orElseThrow(
            () ->
                new IllegalQueryException(
                    String.format(
                        "%s with ID %s does not exist.",
                        klass.getSimpleName(), objectId.getValue())));
  }

  private boolean determinePaging(ProgramNotificationTemplateRequestParams requestParams) {
    Boolean paging = requestParams.getPaging();
    Boolean skipPaging = requestParams.getSkipPaging();

    if (paging != null) {
      return Boolean.TRUE.equals(paging);
    }

    if (skipPaging != null) {
      return Boolean.FALSE.equals(skipPaging);
    }

    return true;
  }
}
