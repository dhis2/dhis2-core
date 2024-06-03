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
package org.hisp.dhis.programrule.engine;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.rules.api.RuleEngine;
import org.hisp.dhis.rules.api.RuleEngineContext;
import org.hisp.dhis.rules.models.RuleEffects;
import org.hisp.dhis.rules.models.RuleEnrollment;
import org.hisp.dhis.rules.models.RuleEvent;
import org.hisp.dhis.rules.models.RuleValidationResult;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.springframework.stereotype.Component;

/**
 * @author Zubair Asghar
 */
@Slf4j
@Component
public class DefaultProgramRuleEngine implements ProgramRuleEngine {
  private static final String ERROR = "Program cannot be null";

  private final ProgramRuleEntityMapperService programRuleEntityMapperService;

  private final ProgramRuleVariableService programRuleVariableService;

  private final ConstantService constantService;

  private final ImplementableRuleService implementableRuleService;

  private final SupplementaryDataProvider supplementaryDataProvider;

  private final RuleEngine ruleEngine;

  public DefaultProgramRuleEngine(
      ProgramRuleEntityMapperService programRuleEntityMapperService,
      ProgramRuleVariableService programRuleVariableService,
      ConstantService constantService,
      ImplementableRuleService implementableRuleService,
      SupplementaryDataProvider supplementaryDataProvider) {
    this.programRuleEntityMapperService = programRuleEntityMapperService;
    this.programRuleVariableService = programRuleVariableService;
    this.constantService = constantService;
    this.implementableRuleService = implementableRuleService;
    this.supplementaryDataProvider = supplementaryDataProvider;
    this.ruleEngine = RuleEngine.getInstance();
  }

  @Override
  public List<RuleEffects> evaluateEnrollmentAndEvents(
      Enrollment enrollment,
      Set<Event> events,
      List<TrackedEntityAttributeValue> trackedEntityAttributeValues) {
    List<ProgramRule> rules =
        getProgramRules(
            enrollment.getProgram(),
            events.stream().map(Event::getProgramStage).distinct().toList());
    return evaluateProgramRulesForMultipleTrackerObjects(
        getRuleEnrollment(enrollment, trackedEntityAttributeValues),
        enrollment.getProgram(),
        getRuleEvents(events),
        rules);
  }

  @Override
  public List<RuleEffects> evaluateProgramEvents(Set<Event> events, Program program) {
    List<ProgramRule> rules = implementableRuleService.getProgramRules(program, null);
    return evaluateProgramRulesForMultipleTrackerObjects(
        null, program, getRuleEvents(events), rules);
  }

  @Override
  public RuleValidationResult getDescription(String condition, Program program) {
    if (program == null) {
      log.error(ERROR);
      return RuleValidationResult.invalid(ERROR);
    }

    return ruleEngine.validate(
        condition,
        programRuleEntityMapperService.getItemStore(
            programRuleVariableService.getProgramRuleVariable(program)));
  }

  @Override
  public RuleValidationResult getDataExpressionDescription(String dataExpression, Program program) {
    if (program == null) {
      log.error(ERROR);
      return RuleValidationResult.invalid(ERROR);
    }

    return ruleEngine.validateDataFieldExpression(
        dataExpression,
        programRuleEntityMapperService.getItemStore(
            programRuleVariableService.getProgramRuleVariable(program)));
  }

  private List<RuleEffects> evaluateProgramRulesForMultipleTrackerObjects(
      RuleEnrollment ruleEnrollment,
      Program program,
      List<RuleEvent> ruleEvents,
      List<ProgramRule> rules) {
    try {
      RuleEngineContext ruleEngineContext = getRuleEngineContext(program, rules);
      return ruleEngine.evaluateAll(ruleEnrollment, ruleEvents, ruleEngineContext);
    } catch (Exception e) {
      log.error(DebugUtils.getStackTrace(e));
      return Collections.emptyList();
    }
  }

  private List<ProgramRule> getProgramRules(Program program, List<ProgramStage> programStage) {
    if (programStage.isEmpty()) {
      return implementableRuleService.getProgramRules(program, null);
    }

    Set<ProgramRule> programRules =
        programStage.stream()
            .flatMap(ps -> implementableRuleService.getProgramRules(program, ps.getUid()).stream())
            .collect(Collectors.toSet());

    return List.copyOf(programRules);
  }

  private RuleEngineContext getRuleEngineContext(Program program, List<ProgramRule> programRules) {
    List<ProgramRuleVariable> programRuleVariables =
        programRuleVariableService.getProgramRuleVariable(program);

    Map<String, String> constantMap =
        constantService.getConstantMap().entrySet().stream()
            .collect(
                Collectors.toMap(Map.Entry::getKey, v -> Double.toString(v.getValue().getValue())));

    Map<String, List<String>> supplementaryData =
        supplementaryDataProvider.getSupplementaryData(programRules);

    return new RuleEngineContext(
        programRuleEntityMapperService.toMappedProgramRules(programRules),
        programRuleEntityMapperService.toMappedProgramRuleVariables(programRuleVariables),
        supplementaryData,
        constantMap);
  }

  private List<RuleEvent> getRuleEvents(Set<Event> events) {
    return programRuleEntityMapperService.toMappedRuleEvents(events);
  }

  private RuleEnrollment getRuleEnrollment(
      Enrollment enrollment, List<TrackedEntityAttributeValue> trackedEntityAttributeValues) {
    return programRuleEntityMapperService.toMappedRuleEnrollment(
        enrollment, trackedEntityAttributeValues);
  }
}
