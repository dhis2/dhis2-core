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

/**
 * @author markusbekken
 */
public interface ProgramRuleActionService {
  /**
   * Adds a {@link ProgramRuleAction}.
   *
   * @param programRuleAction The to ProgramRuleAction add.
   * @return A generated unique id of the added {@link ProgramRuleAction}.
   */
  long addProgramRuleAction(ProgramRuleAction programRuleAction);

  /**
   * Deletes a {@link ProgramRuleAction}
   *
   * @param programRuleAction The ProgramRuleAction to delete.
   */
  void deleteProgramRuleAction(ProgramRuleAction programRuleAction);

  /**
   * Updates an {@link ProgramRuleAction}.
   *
   * @param programRuleAction The ProgramRuleAction to update.
   */
  void updateProgramRuleAction(ProgramRuleAction programRuleAction);

  /**
   * Returns a {@link ProgramRuleAction}.
   *
   * @param id the id of the ProgramRuleAction to return.
   * @return the ProgramRuleAction with the given id
   */
  ProgramRuleAction getProgramRuleAction(long id);

  List<ProgramRuleAction> getProgramActionsWithNoLinkToDataObject();

  List<ProgramRuleAction> getProgramActionsWithNoLinkToNotification();

  List<ProgramRuleAction> getProgramRuleActionsWithNoSectionId();

  List<ProgramRuleAction> getProgramRuleActionsWithNoStageId();

  /**
   * Retrieves all {@link org.hisp.dhis.program.ProgramStage} stages that are used in {@link
   * ProgramRuleActionType#SCHEDULEEVENT} actions within a given program.
   *
   * <p>These program stages are referenced as targets for automatically scheduled events based on
   * evaluated program rule conditions.
   */
  List<String> getProgramStagesUsedInScheduleEventActions();

  /**
   * Retrieves all {@link org.hisp.dhis.dataelement.DataElement} data elements that are referenced
   * in {@link ProgramRuleAction} objects.
   *
   * <p>These data elements are used in program rule actions such as assigning values, showing
   * warnings/errors.
   */
  List<String> getDataElementsPresentInProgramRuleActions();

  /**
   * Retrieves all {@link org.hisp.dhis.trackedentity.TrackedEntityAttribute} attributes that are
   * referenced in {@link ProgramRuleAction} objects.
   *
   * <p>These attributes are used in program rule actions such as assigning values, showing
   * warnings/errors.
   */
  List<String> getTrackedEntityAttributesPresentInProgramRuleActions();
}
