package org.hisp.dhis.programrule;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.program.Program;

/**
 *
 * @author markusbekken
 */
public interface ProgramRuleService
{
    String ID = ProgramRuleService.class.getName();

    /**
     * Adds an {@link ProgramRule}
     *
     * @param ProgramRule The to ProgramRule add.
     * @return A generated unique id of the added {@link ProgramRule}.
     */
    int addProgramRule( ProgramRule programRule );

    /**
     * Deletes a {@link ProgramRule}
     *
     * @param ProgramRule The ProgramRule to delete.
     */
    void deleteProgramRule( ProgramRule programRule );

    /**
     * Updates an {@link ProgramRule}.
     *
     * @param ProgramRule The ProgramRule to update.
     */
    void updateProgramRule( ProgramRule programRule );

    /**
     * Returns a {@link ProgramRule}.
     *
     * @param id the id of the ProgramRule to return.
     * @return the ProgramRule with the given id
     */
    ProgramRule getProgramRule( int id );

    /**
     * Returns a {@link ProgramRule}.
     *
     * @param name the name of the ProgramRule to return.
     * @param program {@link Program}.
     * @return the ProgramRule with the given name
     */
    ProgramRule getProgramRuleByName( String name, Program program );

    /**
     * Returns all {@link ProgramRule}.
     *
     * @return a collection of all ProgramRule, or an empty collection if
     * there are no ProgramRules.
     */
    List<ProgramRule> getAllProgramRule();

    /**
     * Get validation by {@link Program}
     *
     * @param program Program
     * @return ProgramRule list
     */
    List<ProgramRule> getProgramRule( Program program );

    /**
     * Get validation by {@link Program}
     *
     * @param program Program
     * @param key Search Program Rule by key
     * @return ProgramRule list
     */
    List<ProgramRule> getProgramRules( Program program, String key );
}
