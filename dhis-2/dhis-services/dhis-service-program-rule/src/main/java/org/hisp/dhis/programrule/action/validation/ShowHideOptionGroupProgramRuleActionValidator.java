/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.programrule.action.validation;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionGroup;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.programrule.ProgramRuleActionValidationResult;
import org.springframework.stereotype.Component;

/**
 * @author Zubair Asghar
 */
@Slf4j
@Component
public class ShowHideOptionGroupProgramRuleActionValidator extends BaseProgramRuleActionValidator {
  @Override
  public ProgramRuleActionValidationResult validate(
      ProgramRuleAction programRuleAction, ProgramRuleActionValidationContext validationContext) {
    ProgramRule rule = validationContext.getProgramRule();

    // First checking the validity of DataElement and TEA
    ProgramRuleActionValidationResult result = super.validate(programRuleAction, validationContext);

    if (!result.isValid()) {
      return result;
    }

    if (!programRuleAction.hasOptionGroup()) {
      log.debug(String.format("OptionGroup cannot be null for program rule: %s ", rule.getName()));

      return ProgramRuleActionValidationResult.builder()
          .valid(false)
          .errorReport(new ErrorReport(Option.class, ErrorCode.E4040, rule.getName()))
          .build();
    }

    OptionGroup optionGroup = validationContext.getOptionGroup();

    if (optionGroup == null) {
      optionGroup =
          validationContext
              .getProgramRuleActionValidationService()
              .getOptionService()
              .getOptionGroup(programRuleAction.getOptionGroup().getUid());
    }

    if (optionGroup == null) {
      log.debug(
          String.format(
              "OptionGroup: %s associated with program rule: %s does not exist",
              programRuleAction.getOptionGroup().getUid(), rule.getName()));

      return ProgramRuleActionValidationResult.builder()
          .valid(false)
          .errorReport(
              new ErrorReport(
                  Option.class,
                  ErrorCode.E4043,
                  programRuleAction.getOptionGroup().getUid(),
                  rule.getName()))
          .build();
    }

    if (ObjectUtils.anyNotNull(
        programRuleAction.getOption(),
        programRuleAction.getProgramStage(),
        programRuleAction.getProgramStageSection())) {
      return ProgramRuleActionValidationResult.builder()
          .valid(false)
          .errorReport(
              new ErrorReport(
                  Option.class,
                  ErrorCode.E4058,
                  rule.getName(),
                  ProgramRuleActionType.HIDEOPTIONGROUP.name()))
          .build();
    }

    return ProgramRuleActionValidationResult.builder().valid(true).build();
  }
}
