/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.programrule.action.validation;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionValidationResult;
import org.springframework.stereotype.Component;

/**
 * @author Zubair Asghar
 */
@Slf4j
@Component
public class ScheduleEventProgramRuleActionValidator implements ProgramRuleActionValidator {
  @Override
  public ProgramRuleActionValidationResult validate(
      ProgramRuleAction programRuleAction, ProgramRuleActionValidationContext validationContext) {

    ProgramRule rule = validationContext.getProgramRule();
    Program program = validationContext.getProgram();
    ProgramStage programStage = validationContext.getProgramStage();

    if (programStage == null && programRuleAction.hasProgramStage()) {
      programStage =
          validationContext
              .getProgramRuleActionValidationService()
              .getProgramStageService()
              .getProgramStage(programRuleAction.getProgramStage().getUid());
    }

    if (programStage == null) {
      return ProgramRuleActionValidationResult.builder()
          .valid(false)
          .errorReport(new ErrorReport(ProgramStage.class, ErrorCode.E4038, rule.getUid()))
          .build();
    }

    if (!program.isRegistration()) {
      return ProgramRuleActionValidationResult.builder()
          .valid(false)
          .errorReport(new ErrorReport(Program.class, ErrorCode.E4083, rule.getUid()))
          .build();
    }

    if (!Objects.equals(programStage.getProgram().getUid(), program.getUid())) {
      return ProgramRuleActionValidationResult.builder()
          .valid(false)
          .errorReport(
              new ErrorReport(
                  ProgramStage.class, ErrorCode.E4084, programStage.getUid(), program.getUid()))
          .build();
    }

    return ProgramRuleActionValidationResult.builder().valid(true).build();
  }
}
