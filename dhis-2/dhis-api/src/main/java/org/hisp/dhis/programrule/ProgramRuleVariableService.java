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

import java.util.List;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Program;

/**
 *
 * @author markusbekken
 */
public interface ProgramRuleVariableService
{
    /**
     * Adds an {@link ProgramRuleVariable}
     *
     * @param programRuleVariable The to ProgramRuleVariable add.
     * @return A generated unique id of the added {@link ProgramRuleVariable}.
     */
    long addProgramRuleVariable( ProgramRuleVariable programRuleVariable );

    /**
     * Deletes a {@link ProgramRuleVariable}
     *
     * @param programRuleVariable The ProgramRuleVariable to delete.
     */
    void deleteProgramRuleVariable( ProgramRuleVariable programRuleVariable );

    /**
     * Updates an {@link ProgramRuleVariable}.
     *
     * @param programRuleVariable The ProgramRuleVariable to update.
     */
    void updateProgramRuleVariable( ProgramRuleVariable programRuleVariable );

    /**
     * Returns a {@link ProgramRuleVariable}.
     *
     * @param id the id of the ProgramRuleVariable to return.
     * @return the ProgramRuleVariable with the given id
     */
    ProgramRuleVariable getProgramRuleVariable( long id );

    /**
     * Returns all {@link ProgramRuleVariable}.
     *
     * @return a List of all ProgramRuleVariable, or an empty List if there are
     *         no ProgramRuleVariables.
     */
    List<ProgramRuleVariable> getAllProgramRuleVariable();

    /**
     * Get validation by {@link Program}
     *
     * @param program Program
     * @return ProgramRuleVariable list
     */
    List<ProgramRuleVariable> getProgramRuleVariable( Program program );

    /**
     * @param program program.
     * @param dataElement to find association with.
     * @return true if dataElement is associated with any ProgramRuleVariable,
     *         false otherwise.
     */
    boolean isLinkedToProgramRuleVariableCached( Program program, DataElement dataElement );

    /**
     *
     * @return all ProgramRuleVariables which are linked to {@link DataElement}.
     */
    List<ProgramRuleVariable> getVariablesWithNoDataElement();

    /**
     *
     * @return all ProgramRuleVariables which are linked to
     *         {@link org.hisp.dhis.trackedentity.TrackedEntityAttribute}
     */
    List<ProgramRuleVariable> getVariablesWithNoAttribute();
}
