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
package org.hisp.dhis.programrule;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.program.Program;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author markusbekken
 */
@RequiredArgsConstructor
@Service("org.hisp.dhis.programrule.ProgramRuleService")
public class DefaultProgramRuleService implements ProgramRuleService {
  private final ProgramRuleStore programRuleStore;

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
  public List<String> getDataElementsPresentInProgramRules() {
    return programRuleStore.getDataElementsPresentInProgramRules(
        ProgramRuleActionType.SERVER_SUPPORTED_TYPES);
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> getTrackedEntityAttributesPresentInProgramRules() {
    return programRuleStore.getTrackedEntityAttributesPresentInProgramRules(
        ProgramRuleActionType.SERVER_SUPPORTED_TYPES);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRule> getProgramRulesByActionTypes(
      Program program, Set<ProgramRuleActionType> actionTypes) {
    return programRuleStore.getProgramRulesByActionTypes(program, actionTypes);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRule> getProgramRule(Program program) {
    return programRuleStore.get(program);
  }
}
