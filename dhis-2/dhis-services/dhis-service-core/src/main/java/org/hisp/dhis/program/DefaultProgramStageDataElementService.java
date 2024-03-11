package org.hisp.dhis.program;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.dataelement.DataElement;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Viet Nguyen
 */
@Transactional
public class DefaultProgramStageDataElementService
    implements ProgramStageDataElementService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ProgramStageDataElementStore programStageDataElementStore;

    public void setProgramStageDataElementStore( ProgramStageDataElementStore programStageDataElementStore )
    {
        this.programStageDataElementStore = programStageDataElementStore;
    }

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    @Override
    public void addProgramStageDataElement( ProgramStageDataElement programStageDataElement )
    {
        programStageDataElementStore.save( programStageDataElement );
    }

    @Override
    public void deleteProgramStageDataElement( ProgramStageDataElement programStageDataElement )
    {
        programStageDataElementStore.delete( programStageDataElement );
    }

    @Override
    public List<ProgramStageDataElement> getAllProgramStageDataElements()
    {
        return programStageDataElementStore.getAll();
    }
    
    @Override
    public ProgramStageDataElement get( ProgramStage programStage, DataElement dataElement )
    {
        return programStageDataElementStore.get( programStage, dataElement );
    }

    @Override
    public void updateProgramStageDataElement( ProgramStageDataElement programStageDataElement )
    {
        programStageDataElementStore.update( programStageDataElement );
    }
}
