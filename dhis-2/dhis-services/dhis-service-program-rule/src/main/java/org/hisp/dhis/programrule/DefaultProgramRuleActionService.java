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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author markusbekken
 */
@Service("org.hisp.dhis.programrule.ProgramRuleActionService")
public class DefaultProgramRuleActionService implements ProgramRuleActionService {
  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private ProgramRuleActionStore programRuleActionStore;

  public DefaultProgramRuleActionService(ProgramRuleActionStore programRuleActionStore) {
    checkNotNull(programRuleActionStore);

    this.programRuleActionStore = programRuleActionStore;
  }

  // -------------------------------------------------------------------------
  // ProgramRuleAction implementation
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public long addProgramRuleAction(ProgramRuleAction programRuleAction) {
    programRuleActionStore.save(programRuleAction);

    return programRuleAction.getId();
  }

  @Override
  @Transactional
  public void deleteProgramRuleAction(ProgramRuleAction programRuleAction) {
    programRuleActionStore.delete(programRuleAction);
  }

  @Override
  @Transactional
  public void updateProgramRuleAction(ProgramRuleAction programRuleAction) {
    programRuleActionStore.update(programRuleAction);
  }

  @Override
  @Transactional(readOnly = true)
  public ProgramRuleAction getProgramRuleAction(long id) {
    return programRuleActionStore.get(id);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRuleAction> getAllProgramRuleAction() {
    return programRuleActionStore.getAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRuleAction> getProgramRuleAction(ProgramRule programRule) {
    return programRuleActionStore.get(programRule);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRuleAction> getProgramActionsWithNoLinkToDataObject() {
    return programRuleActionStore.getProgramActionsWithNoDataObject();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRuleAction> getProgramActionsWithNoLinkToNotification() {
    return programRuleActionStore.getProgramActionsWithNoNotification();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRuleAction> getProgramRuleActionsWithNoSectionId() {
    return programRuleActionStore.getMalFormedRuleActionsByType(ProgramRuleActionType.HIDESECTION);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRuleAction> getProgramRuleActionsWithNoStageId() {
    return programRuleActionStore.getMalFormedRuleActionsByType(
        ProgramRuleActionType.HIDEPROGRAMSTAGE);
  }
}
