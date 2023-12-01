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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionValidationResult;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.springframework.stereotype.Component;

/**
 * @author Zubair Asghar
 */
@Slf4j
@Component
public class BaseProgramRuleActionValidator implements ProgramRuleActionValidator {
  @Override
  public ProgramRuleActionValidationResult validate(
      ProgramRuleAction programRuleAction, ProgramRuleActionValidationContext validationContext) {
    ProgramRule rule = validationContext.getProgramRule();

    if (!programRuleAction.hasDataElement() && !programRuleAction.hasTrackedEntityAttribute()) {
      log.debug(
          String.format(
              "DataElement or TrackedEntityAttribute cannot be null for program rule: %s ",
              rule.getName()));

      return ProgramRuleActionValidationResult.builder()
          .valid(false)
          .errorReport(new ErrorReport(DataElement.class, ErrorCode.E4044, rule.getName()))
          .build();
    }

    Program program =
        Optional.ofNullable(validationContext.getProgram())
            .orElse(
                validationContext
                    .getProgramRuleActionValidationService()
                    .getProgramService()
                    .getProgram(rule.getProgram().getUid()));

    if (programRuleAction.hasDataElement()) {
      return handleDataElement(validationContext, programRuleAction, program);
    }

    if (programRuleAction.hasTrackedEntityAttribute()) {
      return handleTrackedEntityAttribute(validationContext, programRuleAction, program);
    }

    return ProgramRuleActionValidationResult.builder().valid(true).build();
  }

  private ProgramRuleActionValidationResult handleDataElement(
      ProgramRuleActionValidationContext validationContext,
      ProgramRuleAction programRuleAction,
      Program program) {
    ProgramRule rule = validationContext.getProgramRule();

    DataElement dataElement = validationContext.getDataElement();

    if (dataElement == null) {
      log.debug(
          String.format(
              "DataElement: %s associated with program rule: %s does not exist",
              programRuleAction.getDataElement().getUid(), rule.getName()));

      return ProgramRuleActionValidationResult.builder()
          .valid(false)
          .errorReport(
              new ErrorReport(
                  DataElement.class,
                  ErrorCode.E4045,
                  programRuleAction.getDataElement().getUid(),
                  rule.getName()))
          .build();
    }

    List<ProgramStage> stages = validationContext.getProgramStages();

    if (stages == null || stages.isEmpty()) {
      stages =
          validationContext
              .getProgramRuleActionValidationService()
              .getProgramStageService()
              .getProgramStagesByProgram(program);
    }

    Set<String> dataElements =
        stages.stream()
            .flatMap(s -> s.getDataElements().stream())
            .map(DataElement::getUid)
            .collect(Collectors.toSet());

    if (!dataElements.contains(dataElement.getUid())) {
      log.debug(
          String.format(
              "DataElement: %s is not linked to any ProgramStageDataElement",
              dataElement.getUid()));

      return ProgramRuleActionValidationResult.builder()
          .valid(false)
          .errorReport(
              new ErrorReport(
                  DataElement.class, ErrorCode.E4047, dataElement.getUid(), rule.getName()))
          .build();
    }

    return ProgramRuleActionValidationResult.builder().valid(true).build();
  }

  private ProgramRuleActionValidationResult handleTrackedEntityAttribute(
      ProgramRuleActionValidationContext validationContext,
      ProgramRuleAction programRuleAction,
      Program program) {
    ProgramRule rule = validationContext.getProgramRule();

    TrackedEntityAttribute attribute = validationContext.getTrackedEntityAttribute();

    if (attribute == null) {

      log.debug(
          String.format(
              "TrackedEntityAttribute: %s associated with program rule: %s does not exist",
              programRuleAction.getAttribute().getUid(), rule.getName()));

      return ProgramRuleActionValidationResult.builder()
          .valid(false)
          .errorReport(
              new ErrorReport(
                  TrackedEntityAttribute.class,
                  ErrorCode.E4046,
                  programRuleAction.getAttribute().getUid(),
                  rule.getName()))
          .build();
    }

    List<String> trackedEntityAttributes =
        program.getProgramAttributes().stream()
            .map(att -> att.getAttribute().getUid())
            .collect(Collectors.toList());

    if (!trackedEntityAttributes.contains(attribute.getUid())) {
      log.debug(
          String.format(
              "TrackedEntityAttribute: %s is not linked to any ProgramTrackedEntityAttribute",
              attribute.getUid()));

      return ProgramRuleActionValidationResult.builder()
          .valid(false)
          .errorReport(
              new ErrorReport(
                  TrackedEntityAttribute.class,
                  ErrorCode.E4048,
                  attribute.getUid(),
                  rule.getName()))
          .build();
    }

    return ProgramRuleActionValidationResult.builder().valid(true).build();
  }
}
