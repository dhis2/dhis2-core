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
package org.hisp.dhis.notification;

import static org.hisp.dhis.test.TestBase.injectSecurityContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.hisp.dhis.common.CodeGenerator;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class ProgramNotificationTemplateOperationParamsMapperTest {
  private static UID PROGRAM = UID.of(CodeGenerator.generateUid());
  private static UID PROGRAM_STAGE = UID.of(CodeGenerator.generateUid());

  @Mock private IdentifiableObjectManager manager;

  @InjectMocks private ProgramNotificationTemplateOperationParamsMapper mapper;

  private Program program;
  private ProgramStage programStage;
  private User user;

  @BeforeEach
  public void setUp() {
    user = new User();
    user.setUsername("admin");

    injectSecurityContext(UserDetails.fromUser(user));

    program = new Program();
    program.setName("TB-Program");
    program.setUid(PROGRAM.getValue());

    programStage = new ProgramStage();
    programStage.setName("TB-Program-Stage-1");
    programStage.setUid(PROGRAM_STAGE.getValue());

    when(manager.get(Program.class, PROGRAM.getValue())).thenReturn(program);
    when(manager.get(ProgramStage.class, PROGRAM_STAGE.getValue())).thenReturn(programStage);
  }

  @Test
  void shouldMapValidProgram() {
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
    assertTrue(result.isPaged());
  }

  @Test
  void shouldMapValidProgramStage() {
    ProgramNotificationTemplateOperationParams operationParams =
        ProgramNotificationTemplateOperationParams.builder().programStage(PROGRAM_STAGE).build();

    ProgramNotificationTemplateQueryParams result = mapper.map(operationParams);

    assertNotNull(result);
    assertNull(result.getProgram());
    assertEquals(programStage, result.getProgramStage());
  }

  @Test
  void shouldMapInvalidProgramId() {
    UID invalidProgram = UID.of(CodeGenerator.generateUid());
    ProgramNotificationTemplateOperationParams operationParams =
        ProgramNotificationTemplateOperationParams.builder().program(invalidProgram).build();
    operationParams.setProgram(invalidProgram);

    IllegalQueryException exception =
        assertThrows(
            IllegalQueryException.class,
            () -> {
              mapper.map(operationParams);
            });
    assertEquals(
        "Program with ID %s does not exist.".formatted(invalidProgram.getValue()),
        exception.getMessage());
  }

  @Test
  void shouldMapInvalidProgramStageId() {
    UID invalidProgramStage = UID.of(CodeGenerator.generateUid());
    ProgramNotificationTemplateOperationParams operationParams =
        ProgramNotificationTemplateOperationParams.builder()
            .programStage(invalidProgramStage)
            .build();

    IllegalQueryException exception =
        assertThrows(
            IllegalQueryException.class,
            () -> {
              mapper.map(operationParams);
            });
    assertEquals(
        "ProgramStage with ID %s does not exist.".formatted(invalidProgramStage.getValue()),
        exception.getMessage());
  }
}
