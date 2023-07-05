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

import static org.hisp.dhis.external.conf.ConfigurationKey.SYSTEM_PROGRAM_RULE_SERVER_EXECUTION;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.rules.models.RuleValidationResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Zubair Asghar
 */
@Slf4j
@RequiredArgsConstructor
@Service("org.hisp.dhis.programrule.engine.ProgramRuleEngineService")
public class DefaultProgramRuleEngineService implements ProgramRuleEngineService {
  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  @Qualifier("notificationRuleEngine")
  private final ProgramRuleEngine programRuleEngine;

  private final List<RuleActionImplementer> ruleActionImplementers;

  private final EnrollmentService enrollmentService;

  private final EventService eventService;

  private final ProgramService programService;

  private final DhisConfigurationProvider config;

  @Override
  @Transactional
  public List<RuleEffect> evaluateEnrollmentAndRunEffects(long enrollmentId) {
    if (config.isDisabled(SYSTEM_PROGRAM_RULE_SERVER_EXECUTION)) {
      return List.of();
    }

    Enrollment enrollment = enrollmentService.getEnrollment(enrollmentId);

    if (enrollment == null) {
      return List.of();
    }

    List<ProgramRule> programRules = programRuleEngine.getProgramRules(enrollment.getProgram());

    if (programRules.isEmpty()) {
      return List.of();
    }

    List<RuleEffect> ruleEffects =
        programRuleEngine.evaluate(enrollment, enrollment.getEvents(), programRules);

    for (RuleEffect effect : ruleEffects) {
      ruleActionImplementers.stream()
          .filter(i -> i.accept(effect.ruleAction()))
          .forEach(
              i -> {
                log.debug(
                    String.format("Invoking action implementer: %s", i.getClass().getSimpleName()));

                i.implement(effect, enrollment);
              });
    }

    return ruleEffects;
  }

  @Override
  @Transactional
  public List<RuleEffect> evaluateEventAndRunEffects(String event) {
    if (config.isDisabled(SYSTEM_PROGRAM_RULE_SERVER_EXECUTION)) {
      return Lists.newArrayList();
    }

    return evaluateEventAndRunEffects(eventService.getEvent(event));
  }

  @Override
  public RuleValidationResult getDescription(String condition, String programId) {
    Program program = programService.getProgram(programId);

    return programRuleEngine.getDescription(condition, program);
  }

  @Override
  public RuleValidationResult getDataExpressionDescription(
      String dataExpression, String programId) {
    Program program = programService.getProgram(programId);

    return programRuleEngine.getDataExpressionDescription(dataExpression, program);
  }

  private List<RuleEffect> evaluateEventAndRunEffects(Event event) {
    if (event == null) {
      return Lists.newArrayList();
    }

    Program program = event.getProgramStage().getProgram();
    List<ProgramRule> programRules =
        programRuleEngine.getProgramRules(program, List.of(event.getProgramStage()));

    if (programRules.isEmpty()) {
      return List.of();
    }

    List<RuleEffect> ruleEffects;

    if (program.isWithoutRegistration()) {
      ruleEffects = programRuleEngine.evaluateProgramEvent(event, program, programRules);
    } else {
      Enrollment enrollment = enrollmentService.getEnrollment(event.getEnrollment().getId());

      ruleEffects =
          programRuleEngine.evaluate(enrollment, event, enrollment.getEvents(), programRules);
    }

    for (RuleEffect effect : ruleEffects) {
      ruleActionImplementers.stream()
          .filter(i -> i.accept(effect.ruleAction()))
          .forEach(
              i -> {
                log.debug(
                    String.format("Invoking action implementer: %s", i.getClass().getSimpleName()));

                i.implement(effect, event);
              });
    }

    return ruleEffects;
  }
}
