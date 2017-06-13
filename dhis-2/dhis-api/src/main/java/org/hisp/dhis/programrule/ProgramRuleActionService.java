package org.hisp.dhis.programrule;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import java.util.List;

/**
 *
 * @author markusbekken
 */
public interface ProgramRuleActionService
{
    String ID = ProgramRuleActionService.class.getName();

    /**
     * Adds an {@link ProgramRuleAction}
     *
     * @param ProgramRuleAction The to ProgramRuleAction add.
     * @return A generated unique id of the added {@link ProgramRuleAction}.
     */
    int addProgramRuleAction( ProgramRuleAction programRuleAction );

    /**
     * Deletes a {@link ProgramRuleAction}
     *
     * @param ProgramRuleAction The ProgramRuleAction to delete.
     */
    void deleteProgramRuleAction( ProgramRuleAction programRuleAction );

    /**
     * Updates an {@link ProgramRuleAction}.
     *
     * @param ProgramRuleAction The ProgramRuleAction to update.
     */
    void updateProgramRuleAction( ProgramRuleAction programRuleAction );

    /**
     * Returns a {@link ProgramRuleAction}.
     *
     * @param id the id of the ProgramRuleAction to return.
     * @return the ProgramRuleAction with the given id
     */
    ProgramRuleAction getProgramRuleAction( int id );

    /**
     * Returns all {@link ProgramRuleAction}.
     *
     * @return a collection of all ProgramRuleAction, or an empty collection if
     * there are no ProgramRuleActions.
     */
    List<ProgramRuleAction> getAllProgramRuleAction();

    /**
     * Get validation by {@link ProgramRule}
     *
     * @param program Program
     * @return ProgramRuleAction list
     */
    List<ProgramRuleAction> getProgramRuleAction( ProgramRule programRule );
}
