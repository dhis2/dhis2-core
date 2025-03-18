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
package org.hisp.dhis.program.notification;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author rajazubair
 */
@Component
@RequiredArgsConstructor
public class ProgramNotificationTemplateOperationParamsMapper {

  private final IdentifiableObjectManager manager;

  @Transactional
  public ProgramNotificationTemplateQueryParams map(
      ProgramNotificationTemplateOperationParams operationParams) {
    Program program = getEntity(operationParams.getProgram(), Program.class);
    ProgramStage programStage = getEntity(operationParams.getProgramStage(), ProgramStage.class);

    return ProgramNotificationTemplateQueryParams.builder()
        .program(program)
        .programStage(programStage)
        .page(operationParams.getPage())
        .pageSize(operationParams.getPageSize())
        .paging(operationParams.isPaging())
        .build();
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
                    "%s with UID %s does not exist."
                        .formatted(klass.getSimpleName(), objectId.getValue())));
  }
}
