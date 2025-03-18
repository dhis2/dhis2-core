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
package org.hisp.dhis.programrule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.notification.NotificationTrigger;
import org.hisp.dhis.program.notification.ProgramNotificationRecipient;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.programrule.action.validation.HideOptionProgramRuleActionValidator;
import org.hisp.dhis.programrule.action.validation.NotificationProgramRuleActionValidator;
import org.hisp.dhis.programrule.action.validation.ProgramRuleActionValidationContext;
import org.hisp.dhis.programrule.action.validation.ProgramRuleActionValidationService;
import org.hisp.dhis.test.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProgramRuleActionValidatorTest extends TestBase {
  @Mock private ProgramRuleActionValidationService programRuleActionValidationService;
  @Mock private ProgramService programService;

  private ProgramRuleActionValidationContext validationContext;

  private Program program;
  private ProgramStage programStage;
  private DataElement dataElement;

  @BeforeEach
  void setUp() {
    dataElement = createDataElement('A');
    program = createProgram('A');
    programStage = createProgramStage('A', program);
    programStage.addDataElement(dataElement, 0);
    program.getProgramStages().add(programStage);

    validationContext =
        ProgramRuleActionValidationContext.builder()
            .program(program)
            .programStages(List.of(programStage))
            .programRuleActionValidationService(programRuleActionValidationService)
            .build();
  }

  @Test
  @DisplayName("Should return valid when HideOptionProgramRule contains DataElement")
  void testHideOptionRuleValid() {
    when(programRuleActionValidationService.getProgramService()).thenReturn(programService);
    when(programService.getProgram(any())).thenReturn(program);
    Option option = new Option();
    ProgramRuleAction programRuleAction = new ProgramRuleAction();
    programRuleAction.setDataElement(dataElement);
    programRuleAction.setOption(option);
    ProgramRule programRule = new ProgramRule();
    programRule.setProgram(program);
    programRule.setName("Test Program Rule");
    validationContext.setProgramRule(programRule);
    validationContext.setOption(option);
    validationContext.setDataElement(dataElement);
    HideOptionProgramRuleActionValidator validator = new HideOptionProgramRuleActionValidator();
    ProgramRuleActionValidationResult result =
        validator.validate(programRuleAction, validationContext);
    assertTrue(result.isValid());
  }

  @Test
  @DisplayName("Should return error E4058 when HideOptionProgramRule contains ProgramStage")
  void testHideOptionInvalid() {
    when(programRuleActionValidationService.getProgramService()).thenReturn(programService);
    when(programService.getProgram(any())).thenReturn(program);
    Option option = new Option();
    ProgramRuleAction programRuleAction = new ProgramRuleAction();
    programRuleAction.setDataElement(dataElement);
    programRuleAction.setOption(option);
    programRuleAction.setProgramStage(programStage);
    ProgramRule programRule = new ProgramRule();
    programRule.setProgram(program);
    programRule.setName("Test Program Rule");
    validationContext.setProgramRule(programRule);
    validationContext.setOption(option);
    validationContext.setDataElement(dataElement);
    HideOptionProgramRuleActionValidator validator = new HideOptionProgramRuleActionValidator();
    ProgramRuleActionValidationResult result =
        validator.validate(programRuleAction, validationContext);
    assertFalse(result.isValid());
    assertEquals(ErrorCode.E4058, result.getErrorReport().getErrorCode());
  }

  @Test
  void testValidRuleActionSendMessage() {
    ProgramNotificationTemplate pnt =
        createProgramNotificationTemplate(
            "test", 1, NotificationTrigger.PROGRAM_RULE, ProgramNotificationRecipient.USER_GROUP);

    ProgramRuleAction programRuleAction = new ProgramRuleAction();
    programRuleAction.setTemplateUid(pnt.getUid());
    programRuleAction.setProgramRuleActionType(ProgramRuleActionType.SENDMESSAGE);

    ProgramRule programRule = new ProgramRule();
    programRule.setProgram(program);
    programRule.setName("Test Program Rule");
    programRule.getProgramRuleActions().add(programRuleAction);

    validationContext.setProgramRule(programRule);
    validationContext.setNotificationTemplate(pnt);

    NotificationProgramRuleActionValidator notificationValidator =
        new NotificationProgramRuleActionValidator();
    ProgramRuleActionValidationResult result =
        notificationValidator.validate(programRuleAction, validationContext);
    assertTrue(result.isValid());
  }

  @Test
  void testInValidRuleActionSendMessage() {
    ProgramRuleAction programRuleAction = new ProgramRuleAction();
    programRuleAction.setProgramRuleActionType(ProgramRuleActionType.SENDMESSAGE);

    ProgramRule programRule = new ProgramRule();
    programRule.setProgram(program);
    programRule.setName("Test Program Rule");
    programRule.getProgramRuleActions().add(programRuleAction);

    validationContext.setProgramRule(programRule);
    validationContext.setNotificationTemplate(null);

    NotificationProgramRuleActionValidator notificationValidator =
        new NotificationProgramRuleActionValidator();
    ProgramRuleActionValidationResult result =
        notificationValidator.validate(programRuleAction, validationContext);
    assertFalse(result.isValid());
    assertEquals(ErrorCode.E4035, result.getErrorReport().getErrorCode());
  }
}
