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
package org.hisp.dhis.programrule;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.program.Program;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author markusbekken
 */
@Service("org.hisp.dhis.programrule.ProgramRuleService")
public class DefaultProgramRuleService implements ProgramRuleService {
  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private ProgramRuleStore programRuleStore;

  public DefaultProgramRuleService(ProgramRuleStore programRuleStore) {
    checkNotNull(programRuleStore);

    this.programRuleStore = programRuleStore;
  }

  // -------------------------------------------------------------------------
  // ProgramRule implementation
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public long addProgramRule(ProgramRule programRule) {
    programRuleStore.save(programRule);
    return programRule.getId();
  }

  @Override
  @Transactional
  public void deleteProgramRule(ProgramRule programRule) {
    programRuleStore.delete(programRule);
  }

  @Override
  @Transactional
  public void updateProgramRule(ProgramRule programRule) {
    programRuleStore.update(programRule);
  }

  @Override
  @Transactional(readOnly = true)
  public ProgramRule getProgramRule(long id) {
    return programRuleStore.get(id);
  }

  @Override
  @Transactional(readOnly = true)
  public ProgramRule getProgramRule(String uid) {
    return programRuleStore.getByUid(uid);
  }

  @Override
  @Transactional(readOnly = true)
  public ProgramRule getProgramRuleByName(String name, Program program) {
    return programRuleStore.getByName(name, program);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRule> getProgramRulesLinkedToTeaOrDe() {
    return programRuleStore.getProgramRulesLinkedToTeaOrDe();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRule> getAllProgramRule() {
    return programRuleStore.getAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRule> getProgramRulesByActionTypes(
      Program program, Set<ProgramRuleActionType> actionTypes) {
    return programRuleStore.getProgramRulesByActionTypes(program, actionTypes);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRule> getProgramRulesByActionTypes(
      Program program, Set<ProgramRuleActionType> serverSupportedTypes, String programStageUid) {
    return programRuleStore.getProgramRulesByActionTypes(
        program, serverSupportedTypes, programStageUid);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRule> getProgramRule(Program program) {
    return programRuleStore.get(program);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRule> getProgramRules(Program program, String key) {
    return programRuleStore.get(program, key);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRule> getProgramRulesWithNoPriority() {
    return programRuleStore.getProgramRulesWithNoPriority();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRule> getProgramRulesWithNoCondition() {
    return programRuleStore.getProgramRulesWithNoCondition();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRule> getProgramRulesByEvaluationTime(
      ProgramRuleActionEvaluationTime evaluationTime) {
    return programRuleStore.getProgramRulesByEvaluationTime(evaluationTime);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRule> getProgramRulesByEvaluationEnvironment(
      ProgramRuleActionEvaluationEnvironment evaluationEnvironment) {
    return programRuleStore.getProgramRulesByEvaluationEnvironment(evaluationEnvironment);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRule> getProgramRulesWithNoAction() {
    return programRuleStore.getProgramRulesWithNoAction();
  }
}
