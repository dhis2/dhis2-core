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

package org.hisp.dhis.tracker.converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.tracker.ImportException;
import org.hisp.dhis.tracker.bundle.TrackerBundleParams;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;

import com.fasterxml.jackson.databind.util.StdConverter;

/**
 * Converts a {@see TrackerBundleParams} containing a nested Tracked Entity structure into a "flat" structure and
 * verifies that all Enrollment have a valid TEI parent and all Events have a valid Enrollment parent.
 *
 * Assuming a structure like:
 * <pre>
 *
 * TrackerBundleParams
 *   |
 *   __TEI
 *      |_ENROLLMENT 1
 *      |      |
 *      |      |_ EVENT 1
 *      |      |
 *      |      |_ EVENT 2
 *      |
 *      |_ENROLLMENT 2
 *            |
 *            |_ EVENT 3
 *            |_ EVENT 4
 * </pre>
 *
 * This converter will transform the object into:
 *
 * <pre>
 *
 * TrackerBundleParams
 *   |
 *   __TEI
 *  |__ENROLLMENT 1, ENROLLMENT 2
 *  |
 *  |__EVENT 1, EVENT 2, EVENT 3, EVENT 4
 *
 * </pre>
 *
 *
 * @author Luciano Fiandesio
 */
public class TrackerBundleParamsConverter
    extends
    StdConverter<TrackerBundleParams, TrackerBundleParams>
{
    @Override
    public TrackerBundleParams convert( TrackerBundleParams bundle )
    {
        if ( hasNestedStructure( bundle ) )
        {
            // collect a list of TEI ids
            List<String> teiIds = bundle.getTrackedEntities().stream().map( TrackedEntity::getTrackedEntity )
                .collect( Collectors.toList() );

            // collect all the enrollments of the TEIs
            List<Enrollment> enrollments = bundle.getTrackedEntities().stream()
                .flatMap( l -> l.getEnrollments().stream() ).collect( Collectors.toList() );

            for ( Enrollment enrollment : enrollments )
            {
                if ( teiIds.contains( enrollment.getTrackedEntityInstance() ) )
                {
                    bundle.getEnrollments().add( enrollment );
                }
                else
                {
                    fail( "Enrollment", enrollment.getEnrollment(), "Tracked Entity Instance" );
                }
            }
            List<String> enrollmentIds = bundle.getEnrollments().stream().map( Enrollment::getEnrollment )
                .collect( Collectors.toList() );

            List<Event> events = bundle.getEnrollments().stream().flatMap( l -> l.getEvents().stream() )
                .collect( Collectors.toList() );
            for ( Event event : events )
            {
                if ( enrollmentIds.contains( event.getEnrollment() ) )
                {
                    bundle.getEvents().add( event );
                }
                else
                {
                    fail( "Event", event.getEvent(), "Enrollment" );
                }
            }
            // remove the nested structure
            for ( TrackedEntity tei : bundle.getTrackedEntities() )
            {
                tei.setEnrollments( Collections.emptyList()  );
            }
        }

        return bundle;
    }

    private boolean hasNestedStructure( TrackerBundleParams bundle )
    {
        return bundle.getEnrollments().isEmpty();
    }

    private void fail( String type, String uid, String parent )
    {
        throw new ImportException(
            "Invalid import payload. " + type + " with uid: " + uid + " is not a child of any " + parent );
    }
}
