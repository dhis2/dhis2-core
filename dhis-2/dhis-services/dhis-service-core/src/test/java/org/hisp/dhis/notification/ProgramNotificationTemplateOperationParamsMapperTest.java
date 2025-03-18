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
package org.hisp.dhis.notification;

import static org.hisp.dhis.test.TestBase.injectSecurityContextNoSettings;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateOperationParams;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateOperationParamsMapper;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateQueryParams;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProgramNotificationTemplateOperationParamsMapperTest {
  private static final UID PROGRAM = UID.generate();
  private static final UID PROGRAM_STAGE = UID.generate();

  @Mock private IdentifiableObjectManager manager;

  @InjectMocks private ProgramNotificationTemplateOperationParamsMapper mapper;

  private Program program;
  private ProgramStage programStage;
  private User user;

  @BeforeEach
  public void setUp() {
    user = new User();
    user.setUsername("admin");

    injectSecurityContextNoSettings(UserDetails.fromUser(user));

    program = new Program();
    program.setName("TB-Program");
    program.setUid(PROGRAM.getValue());

    programStage = new ProgramStage();
    programStage.setName("TB-Program-Stage-1");
    programStage.setUid(PROGRAM_STAGE.getValue());
  }

  @Test
  void shouldMapValidProgram() {
    when(manager.get(Program.class, PROGRAM.getValue())).thenReturn(program);
    ProgramNotificationTemplateOperationParams operationParams =
        ProgramNotificationTemplateOperationParams.builder()
            .program(PROGRAM)
            .programStage(null)
            .page(1)
            .pageSize(10)
            .paged(true)
            .build();

    ProgramNotificationTemplateQueryParams result = mapper.map(operationParams);

    assertNotNull(result);
    assertEquals(program, result.getProgram());
    assertNull(result.getProgramStage());
    assertEquals(1, result.getPage());
    assertEquals(10, result.getPageSize());
    assertTrue(result.isPaging());

    verify(manager).get(Program.class, PROGRAM.getValue());
    verifyNoMoreInteractions(manager);
  }

  @Test
  void shouldMapValidProgramStage() {
    when(manager.get(ProgramStage.class, PROGRAM_STAGE.getValue())).thenReturn(programStage);
    ProgramNotificationTemplateOperationParams operationParams =
        ProgramNotificationTemplateOperationParams.builder().programStage(PROGRAM_STAGE).build();

    ProgramNotificationTemplateQueryParams result = mapper.map(operationParams);

    assertNotNull(result);
    assertNull(result.getProgram());
    assertEquals(programStage, result.getProgramStage());
  }

  @Test
  void shouldThrowWhenProgramIdIsInvalid() {
    UID invalidProgram = UID.generate();
    ProgramNotificationTemplateOperationParams operationParams =
        ProgramNotificationTemplateOperationParams.builder().program(invalidProgram).build();
    when(manager.get(Program.class, invalidProgram.getValue())).thenReturn(null);

    IllegalQueryException exception =
        assertThrows(IllegalQueryException.class, () -> mapper.map(operationParams));

    assertEquals(
        "Program with UID %s does not exist.".formatted(invalidProgram.getValue()),
        exception.getMessage());
    verify(manager).get(Program.class, invalidProgram.getValue());
    verifyNoMoreInteractions(manager);
  }

  @Test
  void shouldThrowWhenProgramStageIdIsInvalid() {
    UID invalidProgramStage = UID.generate();
    ProgramNotificationTemplateOperationParams operationParams =
        ProgramNotificationTemplateOperationParams.builder()
            .programStage(invalidProgramStage)
            .build();

    when(manager.get(ProgramStage.class, invalidProgramStage.getValue())).thenReturn(null);

    IllegalQueryException exception =
        assertThrows(IllegalQueryException.class, () -> mapper.map(operationParams));

    assertEquals(
        "ProgramStage with UID %s does not exist.".formatted(invalidProgramStage.getValue()),
        exception.getMessage());
    verify(manager).get(ProgramStage.class, invalidProgramStage.getValue());
    verifyNoMoreInteractions(manager);
  }
}
