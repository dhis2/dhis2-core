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
package org.hisp.dhis.program;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.dataentryform.DataEntryForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Abyot Asalefew
 */
@RequiredArgsConstructor
@Service( "org.hisp.dhis.program.ProgramStageService" )
public class DefaultProgramStageService
    implements ProgramStageService
{
    private final ProgramStageStore programStageStore;

    // -------------------------------------------------------------------------
    // ProgramStage implementation
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long saveProgramStage( ProgramStage programStage )
    {
        programStageStore.save( programStage );
        return programStage.getId();
    }

    @Override
    @Transactional
    public void deleteProgramStage( ProgramStage programStage )
    {
        programStageStore.delete( programStage );
    }

    @Override
    @Transactional( readOnly = true )
    public ProgramStage getProgramStage( long id )
    {
        return programStageStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public ProgramStage getProgramStage( String uid )
    {
        return programStageStore.getByUid( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public List<ProgramStage> getProgramStagesByDataEntryForm( DataEntryForm dataEntryForm )
    {
        return programStageStore.getByDataEntryForm( dataEntryForm );
    }

    @Override
    @Transactional
    public void updateProgramStage( ProgramStage programStage )
    {
        programStageStore.update( programStage );
    }

    @Override
    @Transactional( readOnly = true )
    public List<ProgramStage> getProgramStagesByProgram( Program program )
    {
        return programStageStore.getByProgram( program );
    }
}
