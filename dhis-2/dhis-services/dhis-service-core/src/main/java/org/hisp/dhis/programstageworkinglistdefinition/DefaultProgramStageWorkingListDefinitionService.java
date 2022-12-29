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
package org.hisp.dhis.programstageworkinglistdefinition;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.springframework.stereotype.Service;

@Service( "org.hisp.dhis.programstageworkinglist.ProgramStageWorkingListDefinitionService" )
public class DefaultProgramStageWorkingListDefinitionService
    implements ProgramStageWorkingListDefinitionService
{

    private final ProgramStageWorkingListDefinitionStore programStageWorkingListDefinitionStore;

    private final ProgramService programService;

    private final ProgramStageService programStageService;

    public DefaultProgramStageWorkingListDefinitionService(
        ProgramStageWorkingListDefinitionStore programStageWorkingListDefinitionStore, ProgramService programService,
        ProgramStageService programStageService )
    {
        checkNotNull( programStageWorkingListDefinitionStore );
        checkNotNull( programService );
        checkNotNull( programStageService );

        this.programStageWorkingListDefinitionStore = programStageWorkingListDefinitionStore;
        this.programService = programService;
        this.programStageService = programStageService;
    }

    @Override
    public long add( ProgramStageWorkingListDefinition programStageWorkingListDefinition )
    {
        programStageWorkingListDefinitionStore.save( programStageWorkingListDefinition );
        return programStageWorkingListDefinition.getId();
    }

    @Override
    public void delete( ProgramStageWorkingListDefinition programStageWorkingListDefinition )
    {
        programStageWorkingListDefinitionStore.delete( programStageWorkingListDefinition );
    }

    @Override
    public void update( ProgramStageWorkingListDefinition programStageWorkingListDefinition )
    {
        programStageWorkingListDefinitionStore.update( programStageWorkingListDefinition );
    }

    @Override
    public ProgramStageWorkingListDefinition get( long id )
    {
        return programStageWorkingListDefinitionStore.get( id );
    }

    @Override
    public List<ProgramStageWorkingListDefinition> getAll()
    {
        return programStageWorkingListDefinitionStore.getAll();
    }

    @Override
    public List<String> validate( ProgramStageWorkingListDefinition programStageWorkingListDefinition )
    {
        List<String> errors = new ArrayList<>();

        if ( programStageWorkingListDefinition.getProgram() == null )
        {
            errors.add( "No program specified for the working list definition." );
        }
        else
        {
            Program pr = programService.getProgram( programStageWorkingListDefinition.getProgram().getUid() );

            if ( pr == null )
            {
                errors.add(
                    "Program is specified but does not exist: " + programStageWorkingListDefinition.getProgram() );
            }
        }

        if ( programStageWorkingListDefinition.getProgramStage() == null )
        {
            errors.add( "No program stage specified for the working list definition." );
        }
        else
        {
            ProgramStage ps = programStageService
                .getProgramStage( programStageWorkingListDefinition.getProgramStage().getUid() );

            if ( ps == null )
            {
                errors.add( "Program stage is specified but does not exist: "
                    + programStageWorkingListDefinition.getProgramStage() );
            }
        }

        if ( programStageWorkingListDefinition.getName() == null
            || programStageWorkingListDefinition.getName().isEmpty() )
        {
            errors.add( "No name specified for the working list definition." );
        }

        //TODO Add more validations

        return errors;
    }
}
