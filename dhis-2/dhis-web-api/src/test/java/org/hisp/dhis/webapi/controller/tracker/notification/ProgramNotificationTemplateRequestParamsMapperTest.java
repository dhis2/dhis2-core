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
package org.hisp.dhis.webapi.controller.tracker.notification;

import static org.junit.jupiter.api.Assertions.*;

import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateOperationParams;
import org.hisp.dhis.webapi.controller.notification.ProgramNotificationTemplateRequestParams;
import org.hisp.dhis.webapi.controller.notification.ProgramNotificationTemplateRequestParamsMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Zubair Asghar
 */
@ExtendWith(MockitoExtension.class)
class ProgramNotificationTemplateRequestParamsMapperTest {
  private static final UID PROGRAM = UID.generate();
  private static final UID PROGRAM_STAGE = UID.generate();

  @InjectMocks private ProgramNotificationTemplateRequestParamsMapper mapper;

  @Test
  void shouldMapWithValidProgram() throws ConflictException, BadRequestException {
    ProgramNotificationTemplateRequestParams requestParams =
        new ProgramNotificationTemplateRequestParams();
    requestParams.setProgram(PROGRAM);

    ProgramNotificationTemplateOperationParams result = mapper.map(requestParams);

    assertNotNull(result);
    assertEquals(PROGRAM, result.getProgram());
    assertNull(result.getProgramStage());
  }

  @Test
  void shouldMapWithValidProgramStage() throws ConflictException, BadRequestException {
    ProgramNotificationTemplateRequestParams requestParams =
        new ProgramNotificationTemplateRequestParams();
    requestParams.setProgramStage(PROGRAM_STAGE);

    ProgramNotificationTemplateOperationParams result = mapper.map(requestParams);

    assertNotNull(result);
    assertNull(result.getProgram());
    assertEquals(PROGRAM_STAGE, result.getProgramStage());
  }

  @Test
  void shouldThrowWhenBothProgramAndProgramStageArePresent() {
    ProgramNotificationTemplateRequestParams requestParams =
        new ProgramNotificationTemplateRequestParams();
    requestParams.setProgram(PROGRAM);
    requestParams.setProgramStage(PROGRAM_STAGE);

    ConflictException thrown =
        assertThrows(
            ConflictException.class,
            () -> {
              mapper.map(requestParams);
            });

    assertEquals("`program` and `programStage` cannot be processed together.", thrown.getMessage());
  }

  @Test
  void shouldThrowWhenBothProgramAndProgramStageAreMissing() {
    ProgramNotificationTemplateRequestParams requestParams =
        new ProgramNotificationTemplateRequestParams();

    ConflictException thrown =
        assertThrows(
            ConflictException.class,
            () -> {
              mapper.map(requestParams);
            });

    assertEquals("`program` or `programStage` must be specified.", thrown.getMessage());
  }

  @Test
  void shouldMapWithDefaultPagingValues() throws ConflictException, BadRequestException {
    ProgramNotificationTemplateRequestParams requestParams =
        new ProgramNotificationTemplateRequestParams();
    requestParams.setProgram(PROGRAM);
    requestParams.setPaging(true);

    ProgramNotificationTemplateOperationParams result = mapper.map(requestParams);

    assertNotNull(result);
    assertTrue(result.isPaging());
    assertEquals(1, result.getPage());
    assertEquals(50, result.getPageSize());
  }

  @Test
  void shouldMapWithCustomPagingValues() throws ConflictException, BadRequestException {
    ProgramNotificationTemplateRequestParams requestParams =
        new ProgramNotificationTemplateRequestParams();
    requestParams.setProgram(PROGRAM);
    requestParams.setPaging(true);
    requestParams.setPage(1);
    requestParams.setPageSize(10);

    ProgramNotificationTemplateOperationParams result = mapper.map(requestParams);

    assertNotNull(result);
    assertTrue(result.isPaging());
    assertEquals(1, result.getPage());
    assertEquals(10, result.getPageSize());
  }
}
