package org.hisp.dhis.tracker.validation.hooks;

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

import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstanceQueryParams;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentitycomment.TrackedEntityCommentService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class EventCountValidationHook
    extends AbstractTrackerDtoValidationHook
{
    protected final ProgramInstanceService programInstanceService;

    public EventCountValidationHook( TrackedEntityAttributeService teAttrService,
        TrackedEntityCommentService commentService, ProgramInstanceService programInstanceService )
    {
        super( Event.class, TrackerImportStrategy.CREATE_AND_UPDATE, teAttrService, commentService );
        checkNotNull( programInstanceService );
        this.programInstanceService = programInstanceService;
    }

    @Override
    public void validateEvent( ValidationErrorReporter reporter, Event event )
    {
        TrackerImportValidationContext validationContext = reporter.getValidationContext();

        Program program = validationContext.getProgram( event.getProgram() );

        if ( program.isRegistration() )
        {
            if ( !hasProgramInstance( event, validationContext ) )
            {
                TrackedEntityInstance tei = validationContext.getTrackedEntityInstance( event.getTrackedEntity() );
                final int count = validationContext.getEventToProgramInstancesMap()
                    .getOrDefault( event.getUid(), new ArrayList<>() ).size();

                if ( count == 0 )
                {
                    reporter.addError( newReport( TrackerErrorCode.E1037 )
                        .addArg( tei )
                        .addArg( program ) );
                }
                else if ( count > 1 )
                {
                    reporter.addError( newReport( TrackerErrorCode.E1038 )
                        .addArg( tei )
                        .addArg( program ) );
                }
            }
        }
        else
        {
            User user = validationContext.getBundle().getUser();

            ProgramInstanceQueryParams params = new ProgramInstanceQueryParams();
            params.setProgram( program );
            params.setOrganisationUnitMode( OrganisationUnitSelectionMode.ALL );
            params.setUser( user );

            params.setTrackedEntityInstance( null );

            int count = programInstanceService.countProgramInstances( params );

            if ( count > 1 )
            {
                // TODO: Can't provoke this error on the new importer, the preheater? inserts a
                // program instance.
                reporter.addError( newReport( TrackerErrorCode.E1040 ).addArg( program ) );
            }
        }
    }

    private boolean hasProgramInstance( Event event, TrackerImportValidationContext ctx )
    {
        return ctx.getProgramInstance( event.getUid() ) != null;
    }
}
