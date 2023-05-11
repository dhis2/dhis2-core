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
package org.hisp.dhis.dxf2.events.importer.shared.validation;

import static org.hisp.dhis.dxf2.importsummary.ImportSummary.error;
import static org.hisp.dhis.dxf2.importsummary.ImportSummary.success;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hisp.dhis.dxf2.events.importer.Checker;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;
import org.hisp.dhis.dxf2.events.importer.shared.ImmutableEvent;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component
public class EnrollmentCheck implements Checker
{
    @Override
    public ImportSummary check( ImmutableEvent event, WorkContext ctx )
    {
        Program program = ctx.getProgramsMap().get( event.getProgram() );
        Enrollment enrollment = ctx.getProgramInstanceMap().get( event.getUid() );
        final Optional<TrackedEntity> trackedEntityInstance = ctx.getTrackedEntityInstance( event.getUid() );

        String teiUid = "";
        if ( trackedEntityInstance.isPresent() )
        {
            teiUid = trackedEntityInstance.get().getUid();
        }

        List<Enrollment> enrollments;

        if ( enrollment == null ) // Enrollment should be NOT null,
                                 // after the pre-processing stage
        {
            if ( program.isRegistration() )
            {
                enrollments = new ArrayList<>( ctx.getServiceDelegator().getEnrollmentStore()
                    .get( trackedEntityInstance.orElse( null ), program, ProgramStatus.ACTIVE ) );

                if ( enrollments.isEmpty() )
                {
                    return error( "Tracked entity instance: "
                        + teiUid + " is not enrolled in program: " + program.getUid(),
                        event.getEvent() );
                }
                else if ( enrollments.size() > 1 )
                {
                    return error( "Tracked entity instance: " + teiUid
                        + " has multiple active enrollments in program: " + program.getUid(),
                        event.getEvent() );
                }
            }
            else
            {
                enrollments = ctx.getServiceDelegator().getEnrollmentStore().get( program,
                    ProgramStatus.ACTIVE );

                if ( enrollments.size() > 1 )
                {
                    return error( "Multiple active enrollments exists for program: " + program.getUid(),
                        event.getEvent() );
                }
            }
        }

        return success();
    }
}
