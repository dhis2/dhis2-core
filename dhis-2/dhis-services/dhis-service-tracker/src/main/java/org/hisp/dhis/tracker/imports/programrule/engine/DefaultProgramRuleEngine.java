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

import static org.hisp.dhis.programrule.ProgramRuleActionType.SERVER_SUPPORTED_TYPES;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.rules.api.RuleEngine;
import org.hisp.dhis.rules.api.RuleEngineContext;
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

  private final ProgramRuleService programRuleService;

  private final SupplementaryDataProvider supplementaryDataProvider;

  private final ProgramService programService;

  private final RuleEngine ruleEngine;

  public DefaultProgramRuleEngine(
      ProgramRuleEntityMapperService programRuleEntityMapperService,
      ProgramRuleVariableService programRuleVariableService,
      ConstantService constantService,
      ProgramRuleService programRuleService,
      SupplementaryDataProvider supplementaryDataProvider,
      ProgramService programService) {
    this.programRuleEntityMapperService = programRuleEntityMapperService;
    this.programRuleVariableService = programRuleVariableService;
    this.constantService = constantService;
    this.programRuleService = programRuleService;
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
  public RuleEngineEffects evaluateEnrollmentAndTrackerEvents(
      @Nonnull RuleEnrollment enrollment,
      @Nonnull List<RuleEvent> events,
      @Nonnull Program program,
      @Nonnull UserDetails user) {
    return evaluateEnrollmentAndEvents(enrollment, events, program, user);
  }

  @Override
  public RuleEngineEffects evaluateSingleEvents(
      @Nonnull List<RuleEvent> events, @Nonnull Program program, @Nonnull UserDetails user) {
    return evaluateEnrollmentAndEvents(null, events, program, user);
  }

  private RuleEngineEffects evaluateEnrollmentAndEvents(
      @CheckForNull RuleEnrollment enrollment,
      @Nonnull List<RuleEvent> events,
      @Nonnull Program program,
      @Nonnull UserDetails user) {
    List<ProgramRule> rules =
        programRuleService.getProgramRulesByActionTypes(program, SERVER_SUPPORTED_TYPES);

    if (rules.isEmpty()) {
      return RuleEngineEffects.of(Collections.emptyList());
    }

    List<RuleEffects> ruleEffects =
        evaluateProgramRulesForMultipleTrackerObjects(enrollment, program, events, rules, user);
    return RuleEngineEffects.of(ruleEffects);
  }

  private List<RuleEffects> evaluateProgramRulesForMultipleTrackerObjects(
      @CheckForNull RuleEnrollment ruleEnrollment,
      @Nonnull Program program,
      @Nonnull List<RuleEvent> ruleEvents,
      @Nonnull List<ProgramRule> rules,
      @Nonnull UserDetails user) {
    try {
      RuleEngineContext ruleEngineContext = getRuleEngineContext(program, rules, user);
      return ruleEngine.evaluateAll(ruleEnrollment, ruleEvents, ruleEngineContext);
    } catch (Exception e) {
      log.error(DebugUtils.getStackTrace(e));
      return Collections.emptyList();
    }
  }

  private RuleEngineContext getRuleEngineContext(
      @Nonnull Program program,
      @Nonnull List<ProgramRule> programRules,
      @Nonnull UserDetails user) {
    List<ProgramRuleVariable> programRuleVariables =
        programRuleVariableService.getProgramRuleVariable(program);

    Map<String, String> constantMap =
        constantService.getConstantMap().entrySet().stream()
            .collect(
                Collectors.toMap(Map.Entry::getKey, v -> Double.toString(v.getValue().getValue())));

    Map<String, List<String>> supplementaryData =
        supplementaryDataProvider.getSupplementaryData(programRules, user);

    return new RuleEngineContext(
        programRuleEntityMapperService.toRules(programRules),
        programRuleEntityMapperService.toRuleVariables(programRuleVariables),
        supplementaryData,
        constantMap);
  }
}
