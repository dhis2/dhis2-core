package org.hisp.dhis.tracker.preprocess;

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

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.ReferenceTrackerEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.springframework.stereotype.Component;

import static java.util.Collections.*;

/**
 * This preprocessor is responsible for setting the TrackedEntityInstance UID on
 * an Event, inferring it from the Event's parent enrollment. If the Event
 * already has a TrackedEntityInstance UID set, this preprocessor does not
 * process the Event
 *
 * @author Luciano Fiandesio
 */
@Component
public class EventDefaultEnrollmentPreProcessor implements BundlePreProcessor
{
    @Override
    public void process( TrackerBundle bundle )
    {
        for ( Event event : bundle.getEvents() )
        {
            // If the event enrollment is missing, it will be captured later by validation
            if ( StringUtils.isEmpty( event.getTrackedEntity() ) && StringUtils.isNotEmpty( event.getEnrollment() ) )
            {
                event.setTrackedEntity(
                    getFromPreheat( bundle.getPreheat(), event )
                        .orElseGet( () -> getFromRef( bundle, event ) ) );
            }
        }
    }

    private String getFromRef( TrackerBundle bundle, Event event )
    {
        final Optional<ReferenceTrackerEntity> ref = bundle.getPreheat().getReference( event.getEvent() );

        return ref
            .map( rte -> getTrackedEntityFromEnrollment( bundle.getEnrollments(), rte.getParentUid() ) )
            .orElse( null );
    }

    private Optional<String> getFromPreheat( TrackerPreheat preheat, Event event )
    {
        return Optional.ofNullable( preheat.getEnrollment( TrackerIdScheme.UID, event.getEnrollment() ) )
            .map( e -> {
                if ( e.getEntityInstance() != null )
                {
                    // The Tracked Entity has to be added to the preheat, otherwise validation will fail downstream
                    preheat.putTrackedEntities( TrackerIdScheme.UID, singletonList( e.getEntityInstance() ) );
                    return e.getEntityInstance().getUid();
                }
                return null;
            } );
    }

    private String getTrackedEntityFromEnrollment( List<Enrollment> enrollments, String enrollment )
    {
        return enrollments.stream().filter( e -> e.getEnrollment().equals( enrollment ) ).findFirst()
            .map( Enrollment::getTrackedEntity ).orElse( null );
    }

}
