/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.tracker.preprocess;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.springframework.stereotype.Component;

/**
 * This preprocessor is responsible for setting the ProgramInstance UID on an
 * Event if the Program that the event belongs is of type 'WITHOUT_REGISTRATION'
 *
 * @author Enrico Colasante
 */
@Component
public class EventWithoutRegistrationPreProcessor
    implements BundlePreProcessor
{
    @Override
    public void process( TrackerBundle bundle )
    {
        for ( Event event : bundle.getEvents() )
        {
            if ( StringUtils.isNotEmpty( event.getProgramStage() ) )
            {
                ProgramStage programStage = bundle.getPreheat().get( ProgramStage.class, event.getProgramStage() );

                if ( programStage != null )
                {
                    ProgramInstance enrollment = bundle.getPreheat()
                        .getProgramInstancesWithoutRegistration( programStage.getProgram().getUid() );

                    if ( enrollment != null )
                    {
                        event.setEnrollment( enrollment.getUid() );
                    }
                }
            }
        }
    }
}
