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

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.tracker.bundle.TrackerBundleParams;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.util.StdConverter;

/**
 * Converts a {@see TrackerBundleParams} containing a nested Tracked Entity structure into a "flat" structure
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
 *  |
 *  |___TEI
 *  |___ENROLLMENT 1, ENROLLMENT 2
 *  |
 *  |___EVENT 1, EVENT 2, EVENT 3, EVENT 4
 *
 * </pre>
 *
 * This converter also assigns UIDs to Tracked Entities, Enrollment and Events if the payload does not contain UIDs
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
            // pre assign UIDs to entities, if UIDs are missing
            generateUid( bundle );

            flattenPayload( bundle );
        }

        return bundle;
    }

    private void flattenPayload( TrackerBundleParams bundle ) {

        List<Event> events = new ArrayList<>();

        List<Enrollment> enrollments = new ArrayList<>();

        // Iterate over **all** enrollments
        for ( Enrollment enrollment : bundle.getTrackedEntities().stream()
                .flatMap( l -> l.getEnrollments().stream() ).collect( Collectors.toList() ) )
        {
            // collect all events from enrollemts and add them to the flattened events collection
            events.addAll( enrollment.getEvents().stream().map( e -> addParent( e, enrollment.getEnrollment() ) )
                .collect( Collectors.toList() ) );

            // remove events from enrollment
            enrollment.setEvents( Collections.emptyList() );
        }

        for ( TrackedEntity trackedEntity : bundle.getTrackedEntities() )
        {
            enrollments.addAll( trackedEntity.getEnrollments().stream()
                .map( e -> addParent( e, trackedEntity.getTrackedEntity() ) ).collect( Collectors.toList() ) );

            // remove enrollments from tracked entities
            trackedEntity.setEnrollments( Collections.emptyList() );
        }

        bundle.getEvents().addAll( events );
        bundle.getEnrollments().addAll( enrollments );
    }

    /**
     * Make sure that the Enrollment has the parent ID correctly set
     *
     */
    private Enrollment addParent( Enrollment enrollment, String trackerEntityId )
    {
        if ( StringUtils.isEmpty( enrollment.getTrackedEntity() ) )
        {
            enrollment.setTrackedEntity( trackerEntityId );
        }
        return enrollment;
    }

    /**
     * Make sure that the Event has the parent ID correctly set
     *
     */
    private Event addParent( Event event, String enrollmentId )
    {

        if ( StringUtils.isEmpty( event.getEnrollment() ) )
        {
            event.setEnrollment( enrollmentId );
        }
        return event;
    }

    /**
     * Check if the {@see TrackerBundleParams} has a nested structure
     *
     * @param bundle a {@see TrackerBundleParams}
     * @return true, if TEIs have nested enrollments
     */
    private boolean hasNestedStructure( TrackerBundleParams bundle )
    {
        return bundle.getEnrollments().isEmpty();
    }

    private void generateUid( TrackerBundleParams params )
    {
        // Assign an UID to Tracked Entities if no UID is present
        params.getTrackedEntities().stream()
            .filter( o -> StringUtils.isEmpty( o.getTrackedEntity() ) )
            .forEach( o -> o.setTrackedEntity( CodeGenerator.generateUid() ) );

        List<TrackedEntity> trackedEntities = params.getTrackedEntities();

        for ( TrackedEntity trackedEntity : trackedEntities )
        {
            List<Enrollment> enrollments = trackedEntity.getEnrollments();

            for ( Enrollment enrollment : enrollments )
            {
                // Assign an UID to Enrollment if no UID is present
                if (StringUtils.isEmpty( enrollment.getEnrollment() )) {
                    enrollment.setEnrollment(CodeGenerator.generateUid() );
                }
                // Assign an UID to Events if no UID is present
                enrollment.getEvents().stream().filter( e -> StringUtils.isEmpty( e.getEvent() ) )
                    .forEach( e -> e.setEvent( CodeGenerator.generateUid() ) );
            }

        }

    }
}
