package org.hisp.dhis.dxf2.events.event.preprocess;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.validation.ValidationContext;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceStore;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;

/**
 * @author Luciano Fiandesio
 */
public class ProgramInstancePreProcessor
    implements
    PreProcessor
{
    @Override
    public void process( Event event, ValidationContext ctx )
    {
        // TODO can we skip this if enrollment property is not null? Can the enrollment property be set by the client?
        ProgramInstanceStore programInstanceStore = ctx.getProgramInstanceStore();
        ImportOptions importOptions = ctx.getImportOptions();

        Program program = ctx.getProgramsMap().get( event.getProgram() );
        ProgramInstance programInstance = ctx.getProgramInstanceMap().get( event.getUid() );
        TrackedEntityInstance trackedEntityInstance = ctx.getTrackedEntityInstanceMap().get( event.getUid() );

        if ( program.isRegistration() && programInstance == null )
        {
            List<ProgramInstance> programInstances = new ArrayList<>(
                    programInstanceStore.get( trackedEntityInstance, program, ProgramStatus.ACTIVE ) );

            if ( programInstances.size() == 1 )
            {
                event.setEnrollment( programInstances.get( 0 ).getUid() );
                ctx.getProgramInstanceMap().put( event.getUid(), programInstances.get( 0 ) );
            }
        } else {

            List<ProgramInstance> programInstances = programInstanceStore.get( program, ProgramStatus.ACTIVE );

            if ( programInstances.isEmpty() )
            {
                // Create PI if it doesn't exist (should only be one)
                ProgramInstance pi = new ProgramInstance();
                pi.setUid( CodeGenerator.generateUid() );
                pi.setEnrollmentDate( new Date() );
                pi.setIncidentDate( new Date() );
                pi.setProgram( program );
                pi.setStatus( ProgramStatus.ACTIVE );
                pi.setStoredBy( event.getStoredBy() );

                // Persist Program Instance
                ctx.getProgramInstanceStore().save( pi, ctx.getImportOptions().getUser() );

                programInstances.add( pi );

                // Add PI to caches //
                event.setEnrollment( pi.getUid() );
                ctx.getProgramInstanceMap().put( event.getUid(), pi );
            }
        }
    }
}
