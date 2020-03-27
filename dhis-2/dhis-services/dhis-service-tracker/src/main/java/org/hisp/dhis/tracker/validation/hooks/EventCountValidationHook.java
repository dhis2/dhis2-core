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
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceQueryParams;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.PreheatHelper;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class EventCountValidationHook
    extends AbstractTrackerValidationHook
{
    @Override
    public int getOrder()
    {
        return 305;
    }

    @Override
    public List<TrackerErrorReport> validate( TrackerBundle bundle )
    {
        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle, this.getClass() );

        for ( Event event : bundle.getEvents() )
        {
            reporter.increment( event );

            ProgramStage programStage = PreheatHelper.getProgramStage( bundle, event.getProgramStage() );
            ProgramStageInstance programStageInstance = PreheatHelper
                .getProgramStageInstance( bundle, event.getEvent() );
            Program program = PreheatHelper.getProgram( bundle, event.getProgram() );
            ProgramInstance programInstance = PreheatHelper.getProgramInstance( bundle, event.getEnrollment() );

            TrackedEntityInstance trackedEntityInstance = PreheatHelper
                .getTei( bundle, event.getTrackedEntity() );

            if ( program.isRegistration() )
            {
                ProgramInstanceQueryParams params = new ProgramInstanceQueryParams();
                params.setProgram( program );
                params.setTrackedEntityInstance( trackedEntityInstance );
                params.setOrganisationUnitMode( OrganisationUnitSelectionMode.ALL );
                params.setUser( bundle.getUser() );

                int count = programInstanceService.countProgramInstances( params );

                //TODO: I can't provoke this state where programInstance == NULL && program.isRegistration(),
                // the meta check will complain about that "is a registration but its program stage is not valid or missing." (E1086)
                if ( programInstance == null && trackedEntityInstance != null )
                {
                    if ( count == 0 )
                    {
                        reporter.addError( newReport( TrackerErrorCode.E1037 )
                            .addArg( trackedEntityInstance )
                            .addArg( program ) );
                    }
                    else if ( count > 1 )
                    {
                        reporter.addError( newReport( TrackerErrorCode.E1038 )
                            .addArg( trackedEntityInstance )
                            .addArg( program ) );
                    }
                }
            }
            else
            {
                //TODO: I don't understand the purpose of this here. This could be moved to preheater?
                ProgramInstanceQueryParams params = new ProgramInstanceQueryParams();
                params.setProgram( program );
                params.setOrganisationUnitMode( OrganisationUnitSelectionMode.ALL );
                params.setUser( bundle.getUser() );

                int count = programInstanceService.countProgramInstances( params );

                if ( count > 1 )
                {
                    reporter.addError( newReport( TrackerErrorCode.E1040 )
                        .addArg( program ) );
                }
            }

        }

        return reporter.getReportList();
    }
}
