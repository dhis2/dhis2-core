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

import org.hisp.dhis.system.deletion.DeletionHandler;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author Quang Nguyen
 */
public class ProgramInstanceDeletionHandler
    extends DeletionHandler
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ProgramInstanceService programInstanceService;

    public void setProgramInstanceService( ProgramInstanceService programInstanceService )
    {
        this.programInstanceService = programInstanceService;
    }

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    @Override
    public String getClassName()
    {
        return ProgramInstance.class.getSimpleName();
    }

    @Override
    public void deleteTrackedEntityInstance( TrackedEntityInstance trackedEntityInstance )
    {
        for ( ProgramInstance programInstance : trackedEntityInstance.getProgramInstances() )
        {
            programInstanceService.deleteProgramInstance( programInstance, false );
        }
    }   

    @Override
    public void deleteProgram( Program program )
    {
        Collection<ProgramInstance> programInstances = programInstanceService.getProgramInstances( program );

        if ( programInstances != null )
        {
            Iterator<ProgramInstance> iterator = programInstances.iterator();
            while ( iterator.hasNext() )
            {
                ProgramInstance programInstance = iterator.next();
                iterator.remove();
                programInstanceService.deleteProgramInstance( programInstance, true );
            }
        }
    }
}
