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
package org.hisp.dhis.tracker.imports.bundle.persister;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.relationship.RelationshipService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.tracker.imports.TrackerType;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.converter.EnrollmentTrackerConverterService;
import org.hisp.dhis.tracker.imports.converter.EventTrackerConverterService;
import org.hisp.dhis.tracker.imports.domain.Relationship;
import org.hisp.dhis.tracker.imports.report.Entity;
import org.hisp.dhis.tracker.imports.report.TrackerTypeReport;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

/**
 * @author Zubair Asghar
 */
@Service
@RequiredArgsConstructor
public class DefaultTrackerObjectsDeletionService
    implements TrackerObjectDeletionService
{
    private final EnrollmentService enrollmentService;

    private final TrackedEntityService teiService;

    private final EventService eventService;

    private final RelationshipService relationshipService;

    private final EnrollmentTrackerConverterService enrollmentTrackerConverterService;

    private final EventTrackerConverterService eventTrackerConverterService;

    @Override
    public TrackerTypeReport deleteEnrollments( TrackerBundle bundle )
    {
        TrackerTypeReport typeReport = new TrackerTypeReport( TrackerType.ENROLLMENT );

        List<org.hisp.dhis.tracker.imports.domain.Enrollment> enrollments = bundle.getEnrollments();

        for ( int idx = 0; idx < enrollments.size(); idx++ )
        {
            String uid = enrollments.get( idx ).getEnrollment();

            Entity objectReport = new Entity( TrackerType.ENROLLMENT, uid, idx );

            Enrollment enrollment = enrollmentService.getEnrollment( uid );

            List<org.hisp.dhis.tracker.imports.domain.Event> events = eventTrackerConverterService
                .to( Lists.newArrayList( enrollment.getEvents()
                    .stream().filter( event -> !event.isDeleted() )
                    .collect( Collectors.toList() ) ) );

            TrackerBundle trackerBundle = TrackerBundle.builder().events( events ).user( bundle.getUser() )
                .build();

            deleteEvents( trackerBundle );

            TrackedEntity tei = enrollment.getTrackedEntity();
            tei.getEnrollments().remove( enrollment );

            enrollmentService.deleteEnrollment( enrollment );
            teiService.updateTrackedEntity( tei );

            typeReport.getStats().incDeleted();
            typeReport.addEntity( objectReport );
        }

        return typeReport;
    }

    @Override
    public TrackerTypeReport deleteEvents( TrackerBundle bundle )
    {
        TrackerTypeReport typeReport = new TrackerTypeReport( TrackerType.EVENT );

        List<org.hisp.dhis.tracker.imports.domain.Event> events = bundle.getEvents();

        for ( int idx = 0; idx < events.size(); idx++ )
        {
            String uid = events.get( idx ).getEvent();

            Entity objectReport = new Entity( TrackerType.EVENT, uid, idx );

            Event event = eventService.getEvent( uid );

            Enrollment enrollment = event.getEnrollment();

            eventService.deleteEvent( event );

            if ( event.getProgramStage().getProgram().isRegistration() )
            {
                teiService.updateTrackedEntity( event.getEnrollment().getTrackedEntity() );

                enrollment.getEvents().remove( event );
                enrollmentService.updateEnrollment( enrollment );
            }

            typeReport.getStats().incDeleted();
            typeReport.addEntity( objectReport );
        }

        return typeReport;
    }

    @Override
    public TrackerTypeReport deleteTrackedEntity( TrackerBundle bundle )
    {
        TrackerTypeReport typeReport = new TrackerTypeReport( TrackerType.TRACKED_ENTITY );

        List<org.hisp.dhis.tracker.imports.domain.TrackedEntity> trackedEntities = bundle.getTrackedEntities();

        for ( int idx = 0; idx < trackedEntities.size(); idx++ )
        {
            String uid = trackedEntities.get( idx ).getTrackedEntity();

            Entity objectReport = new Entity( TrackerType.TRACKED_ENTITY, uid, idx );

            TrackedEntity daoEntityInstance = teiService
                .getTrackedEntity( uid );

            Set<Enrollment> daoEnrollments = daoEntityInstance.getEnrollments();

            List<org.hisp.dhis.tracker.imports.domain.Enrollment> enrollments = enrollmentTrackerConverterService
                .to( Lists.newArrayList( daoEnrollments.stream()
                    .filter( enrollment -> !enrollment.isDeleted() )
                    .toList() ) );

            TrackerBundle trackerBundle = TrackerBundle.builder().enrollments( enrollments )
                .user( bundle.getUser() )
                .build();

            deleteEnrollments( trackerBundle );

            teiService.deleteTrackedEntity( daoEntityInstance );

            typeReport.getStats().incDeleted();
            typeReport.addEntity( objectReport );
        }

        return typeReport;
    }

    @Override
    public TrackerTypeReport deleteRelationships( TrackerBundle bundle )
    {
        TrackerTypeReport typeReport = new TrackerTypeReport( TrackerType.RELATIONSHIP );

        List<Relationship> relationships = bundle.getRelationships();

        for ( int idx = 0; idx < relationships.size(); idx++ )
        {
            String uid = relationships.get( idx ).getRelationship();

            Entity objectReport = new Entity( TrackerType.RELATIONSHIP, uid, idx );

            org.hisp.dhis.relationship.Relationship relationship = relationshipService.getRelationship( uid );

            relationshipService.deleteRelationship( relationship );

            typeReport.getStats().incDeleted();
            typeReport.addEntity( objectReport );
        }

        return typeReport;
    }
}
