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

import org.hisp.dhis.program.Program;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author markusbekken
 */
@Transactional
public class DefaultProgramRuleVariableService
    implements ProgramRuleVariableService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ProgramRuleVariableStore programRuleVariableStore;

    public void setProgramRuleVariableStore( ProgramRuleVariableStore programRuleVariableStore )
    {
        this.programRuleVariableStore = programRuleVariableStore;
    }

    // -------------------------------------------------------------------------
    // ProgramRuleVariable implementation
    // -------------------------------------------------------------------------

    @Override
    public int addProgramRuleVariable( ProgramRuleVariable programRuleVariable )
    {
        programRuleVariableStore.save( programRuleVariable );
        return programRuleVariable.getId();
    }

    @Override
    public void deleteProgramRuleVariable( ProgramRuleVariable programRuleVariable )
    {
        programRuleVariableStore.delete( programRuleVariable );
    }

    @Override
    public void updateProgramRuleVariable( ProgramRuleVariable programRuleVariable )
    {
        programRuleVariableStore.update( programRuleVariable );
    }

    @Override
    public ProgramRuleVariable getProgramRuleVariable( int id )
    {
        return programRuleVariableStore.get( id );
    }

    @Override
    public List<ProgramRuleVariable> getAllProgramRuleVariable()
    {
        return programRuleVariableStore.getAll();
    }

    @Override
    public List<ProgramRuleVariable> getProgramRuleVariable( Program program )
    {
        return programRuleVariableStore.get( program );
    }
}
