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

import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validatePaginationParameters;

import lombok.RequiredArgsConstructor;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateOperationParams;
import org.springframework.stereotype.Component;

/**
 * @author Zubair Asghar
 */
@Component
@RequiredArgsConstructor
public class ProgramNotificationTemplateRequestParamsMapper {
  public ProgramNotificationTemplateOperationParams map(
      ProgramNotificationTemplateRequestParams requestParams)
      throws ConflictException, BadRequestException {
    validateRequestParams(requestParams);

    return ProgramNotificationTemplateOperationParams.builder()
        .program(requestParams.getProgram())
        .programStage(requestParams.getProgramStage())
        .paged(requestParams.isPaging())
        .page(requestParams.getPage())
        .pageSize(requestParams.getPageSize())
        .build();
  }

  private void validateRequestParams(ProgramNotificationTemplateRequestParams requestParams)
      throws ConflictException, BadRequestException {
    if (requestParams.getProgram() == null && requestParams.getProgramStage() == null) {
      throw new ConflictException("`program` or `programStage` must be specified.");
    }

    if (requestParams.getProgram() != null && requestParams.getProgramStage() != null) {
      throw new ConflictException("`program` and `programStage` cannot be processed together.");
    }

    validatePaginationParameters(requestParams);
  }
}
