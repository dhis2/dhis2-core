/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.tracker.imports.programrule.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.rules.api.RuleEngine;
import org.hisp.dhis.rules.api.RuleEngineContext;
import org.hisp.dhis.rules.api.RuleSupplementaryData;
import org.hisp.dhis.rules.models.RuleEffects;
import org.hisp.dhis.rules.models.RuleEnrollment;
import org.hisp.dhis.rules.models.RuleEvent;
import org.hisp.dhis.rules.models.RuleValidationResult;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Component;

/**
 * @author Zubair Asghar
 */
@Slf4j
@Component
public class DefaultProgramRuleEngine implements ProgramRuleEngine {
  private final ProgramRuleEntityMapperService programRuleEntityMapperService;

  private final ProgramRuleVariableService programRuleVariableService;

  private final ConstantService constantService;

  private final SupplementaryDataProvider supplementaryDataProvider;

  private final ProgramService programService;

  private final RuleEngine ruleEngine;

  public DefaultProgramRuleEngine(
      ProgramRuleEntityMapperService programRuleEntityMapperService,
      ProgramRuleVariableService programRuleVariableService,
      ConstantService constantService,
      SupplementaryDataProvider supplementaryDataProvider,
      ProgramService programService) {
    this.programRuleEntityMapperService = programRuleEntityMapperService;
    this.programRuleVariableService = programRuleVariableService;
    this.constantService = constantService;
    this.supplementaryDataProvider = supplementaryDataProvider;
    this.programService = programService;
    this.ruleEngine = RuleEngine.getInstance();
  }

  @Override
  public RuleValidationResult getDescription(@Nonnull String condition, @Nonnull UID programUid)
      throws BadRequestException {
    Program program = programService.getProgram(programUid.getValue());

    if (program == null) {
      throw new BadRequestException("Program is specified but does not exist: " + programUid);
    }
    return ruleEngine.validate(
        condition,
        programRuleEntityMapperService.getItemStore(
            programRuleVariableService.getProgramRuleVariable(program),
            constantService.getAllConstants()));
  }

  @Override
  public RuleValidationResult getDataExpressionDescription(
      @Nonnull String dataExpression, @Nonnull UID programUid) throws BadRequestException {
    Program program = programService.getProgram(programUid.getValue());

    if (program == null) {
      throw new BadRequestException("Program is specified but does not exist: " + programUid);
    }
    return ruleEngine.validateDataFieldExpression(
        dataExpression,
        programRuleEntityMapperService.getItemStore(
            programRuleVariableService.getProgramRuleVariable(program),
            constantService.getAllConstants()));
  }

  @Override
  public RuleEngineEffects evaluateEnrollmentsAndTrackerEvents(
      @Nonnull Map<RuleEnrollment, List<RuleEvent>> enrollmentsWithEvents,
      @Nonnull UserDetails user,
      @Nonnull Map<String, String> constantMap,
      @Nonnull List<ProgramRule> rules,
      @Nonnull List<ProgramRuleVariable> variables) {
    if (enrollmentsWithEvents.isEmpty()) {
      return RuleEngineEffects.of(Collections.emptyList());
    }
    RuleEngineContext context = getRuleEngineContext(rules, variables, user, constantMap);
    List<RuleEffects> allEffects = new ArrayList<>();
    for (Map.Entry<RuleEnrollment, List<RuleEvent>> entry : enrollmentsWithEvents.entrySet()) {
      try {
        allEffects.addAll(ruleEngine.evaluateAll(entry.getKey(), entry.getValue(), context));
      } catch (Exception e) {
        log.error("Call to rule-engine failed", e);
      }
    }
    return RuleEngineEffects.of(allEffects);
  }

  @Override
  public RuleEngineEffects evaluateSingleEvents(
      @Nonnull List<RuleEvent> events,
      @Nonnull UserDetails user,
      @Nonnull Map<String, String> constantMap,
      @Nonnull List<ProgramRule> rules,
      @Nonnull List<ProgramRuleVariable> variables) {
    RuleEngineContext ruleEngineContext = getRuleEngineContext(rules, variables, user, constantMap);
    try {
      return RuleEngineEffects.of(ruleEngine.evaluateAll(null, events, ruleEngineContext));
    } catch (Exception e) {
      log.error("Call to rule-engine failed", e);
      return RuleEngineEffects.of(List.of());
    }
  }

  private RuleEngineContext getRuleEngineContext(
      @Nonnull List<ProgramRule> programRules,
      @Nonnull List<ProgramRuleVariable> programRuleVariables,
      @Nonnull UserDetails user,
      @Nonnull Map<String, String> constantMap) {
    RuleSupplementaryData supplementaryData =
        supplementaryDataProvider.getSupplementaryData(programRules, user);

    return new RuleEngineContext(
        programRuleEntityMapperService.toRules(programRules),
        programRuleEntityMapperService.toRuleVariables(programRuleVariables),
        supplementaryData,
        constantMap);
  }
}
