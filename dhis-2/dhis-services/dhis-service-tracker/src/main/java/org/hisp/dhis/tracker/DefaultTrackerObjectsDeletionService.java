package org.hisp.dhis.tracker;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.relationship.RelationshipService;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.converter.EnrollmentTrackerConverterService;
import org.hisp.dhis.tracker.converter.EventTrackerConverterService;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.TrackerObjectReport;
import org.hisp.dhis.tracker.report.TrackerTypeReport;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

/**
 * @author Zubair Asghar
 */

@Service
public class DefaultTrackerObjectsDeletionService
    implements TrackerObjectDeletionService
{
    private final ProgramInstanceService programInstanceService;

    private final TrackedEntityInstanceService teiService;

    private final ProgramStageInstanceService programStageInstanceService;

    private final RelationshipService relationshipService;

    private final TrackerAccessManager trackerAccessManager;

    private final EnrollmentTrackerConverterService enrollmentTrackerConverterService;

    private final EventTrackerConverterService eventTrackerConverterService;

    public DefaultTrackerObjectsDeletionService( ProgramInstanceService programInstanceService,
         TrackedEntityInstanceService entityInstanceService,
         ProgramStageInstanceService stageInstanceService,
         RelationshipService relationshipService,
         TrackerAccessManager trackerAccessManager,
         EnrollmentTrackerConverterService enrollmentTrackerConverterService,
         EventTrackerConverterService eventTrackerConverterService )
    {
        this.programInstanceService = programInstanceService;
        this.teiService = entityInstanceService;
        this.programStageInstanceService = stageInstanceService;
        this.relationshipService = relationshipService;
        this.trackerAccessManager = trackerAccessManager;
        this.enrollmentTrackerConverterService = enrollmentTrackerConverterService;
        this.eventTrackerConverterService = eventTrackerConverterService;
    }

    @Override
    public TrackerTypeReport deleteEnrollments( TrackerBundle bundle, TrackerType trackerType )
    {
        TrackerTypeReport typeReport = new TrackerTypeReport( trackerType );

        List<Enrollment> enrollments = bundle.getEnrollments();

        for ( int idx = 0; idx < enrollments.size(); idx++ )
        {
            String uid = enrollments.get( idx ).getEnrollment();

            TrackerObjectReport trackerObjectReport = new TrackerObjectReport( TrackerType.ENROLLMENT );

            ProgramInstance programInstance = programInstanceService.getProgramInstance( uid );

            if ( bundle.getUser() != null )
            {
                // TODO authority check should be part of validation phase. This check will be moved in validation hooks.
                List<TrackerErrorReport> trackerErrorReports = isAllowedToDeleteEnrollment( idx, bundle.getUser(), programInstance, bundle );

                if ( !trackerErrorReports.isEmpty() )
                {
                    return addErrorToTypeReport( typeReport, trackerObjectReport, trackerErrorReports, idx, uid );
                }
            }

            List<Event> events = eventTrackerConverterService.to( Lists.newArrayList( programInstance.getProgramStageInstances() ) );

            TrackerBundle trackerBundle = TrackerBundle.builder().events( events ).user( bundle.getUser() ).build();

            // Associated events should be deleted provided user has authority for that.
            deleteEvents( trackerBundle, TrackerType.EVENT );

            programInstanceService.deleteProgramInstance( programInstance );
            teiService.updateTrackedEntityInstance( programInstance.getEntityInstance() );

            typeReport.addObjectReport( trackerObjectReport );
            typeReport.getStats().incDeleted();
        }

        return typeReport;
    }

    @Override
    public TrackerTypeReport deleteEvents( TrackerBundle bundle, TrackerType trackerType )
    {
        TrackerTypeReport typeReport = new TrackerTypeReport( trackerType );

        List<Event> events = bundle.getEvents();

        for ( int idx = 0; idx < events.size(); idx++ )
        {
            String uid = events.get( idx ).getEvent();

            TrackerObjectReport trackerObjectReport = new TrackerObjectReport( TrackerType.EVENT );

            List<TrackerErrorReport> trackerErrorReports = new ArrayList<>();

            ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( uid );

            // TODO authority check should be part of validation phase. This check will be moved in validation hooks.
            List<String> errors = trackerAccessManager.canDelete( bundle.getUser(), programStageInstance, false );

            if ( !errors.isEmpty() )
            {
                errors.forEach( error -> trackerErrorReports.add( TrackerErrorReport.builder()
                    .errorMessage( error )
                    .build( bundle ) ) );

                trackerObjectReport.getErrorReports().addAll( trackerErrorReports );
                trackerObjectReport.setIndex( idx );
                trackerObjectReport.setUid( uid );

                typeReport.addObjectReport( trackerObjectReport );
                typeReport.getStats().incIgnored();
                return typeReport;
            }

            programStageInstanceService.deleteProgramStageInstance( programStageInstance );

            if ( programStageInstance.getProgramStage().getProgram().isRegistration() )
            {
                teiService.updateTrackedEntityInstance( programStageInstance.getProgramInstance().getEntityInstance() );
            }

            typeReport.addObjectReport( trackerObjectReport );
            typeReport.getStats().incDeleted();
        }

        return typeReport;
    }

    @Override
    public TrackerTypeReport deleteTrackedEntityInstances( TrackerBundle bundle, TrackerType trackerType )
    {
        TrackerTypeReport typeReport = new TrackerTypeReport( trackerType );

        List<TrackedEntity> trackedEntities = bundle.getTrackedEntities();

        for ( int idx = 0; idx < trackedEntities.size(); idx++ )
        {
            String uid = trackedEntities.get( idx ).getTrackedEntity();

            TrackerObjectReport trackerObjectReport = new TrackerObjectReport( TrackerType.TRACKED_ENTITY );

            org.hisp.dhis.trackedentity.TrackedEntityInstance daoEntityInstance = teiService
                    .getTrackedEntityInstance( uid );

            // TODO authority check should be part of validation phase. This check will be moved in validation hooks.
            if ( bundle.getUser() != null )
            {
                List<TrackerErrorReport> trackerErrorReports = isAllowedToDeleteTrackedEntity( idx, bundle.getUser(), daoEntityInstance, bundle );

                if ( !trackerErrorReports.isEmpty() )
                {
                    return addErrorToTypeReport( typeReport, trackerObjectReport, trackerErrorReports, idx, uid );
                }
            }

            Set<ProgramInstance> programInstances = daoEntityInstance.getProgramInstances();

            List<Enrollment> enrollments = enrollmentTrackerConverterService.to( Lists.newArrayList( programInstances ) );

            TrackerBundle trackerBundle = TrackerBundle.builder().enrollments( enrollments ).user( bundle.getUser() ).build();

            // Associated enrollments should be deleted provided user has authority for that.
            deleteEnrollments( trackerBundle, TrackerType.ENROLLMENT );

            teiService.deleteTrackedEntityInstance( daoEntityInstance );

            typeReport.addObjectReport( trackerObjectReport );
            typeReport.getStats().incDeleted();
        }

        return typeReport;
    }

    @Override
    public TrackerTypeReport deleteRelationShips( TrackerBundle bundle, TrackerType trackerType )
    {
        TrackerTypeReport typeReport = new TrackerTypeReport( trackerType );

        List<Relationship> relationships = bundle.getRelationships();

        for ( int idx = 0; idx < relationships.size(); idx++ )
        {
            String uid = relationships.get( idx ).getRelationship();

            TrackerObjectReport trackerObjectReport = new TrackerObjectReport( TrackerType.RELATIONSHIP );

            List<TrackerErrorReport> trackerErrorReports = new ArrayList<>();

            org.hisp.dhis.relationship.Relationship relationship = relationshipService.getRelationship( uid );

            // TODO authority check should be part of validation phase. This check will be moved in validation hooks.
            List<String> errors = trackerAccessManager.canWrite( bundle.getUser(), relationship );

            if ( !errors.isEmpty() )
            {
                errors.forEach( error -> trackerErrorReports.add( TrackerErrorReport.builder()
                    .errorMessage( error )
                    .build( bundle ) ) );

                trackerObjectReport.getErrorReports().addAll( trackerErrorReports );
                trackerObjectReport.setIndex( idx );
                trackerObjectReport.setUid( uid );

                typeReport.addObjectReport( trackerObjectReport );
                typeReport.getStats().incIgnored();
                return typeReport;
            }

            relationshipService.deleteRelationship( relationship );

            typeReport.getStats().incDeleted();
        }

        return typeReport;
    }

    private List<TrackerErrorReport> isAllowedToDeleteEnrollment( int index, User user, ProgramInstance pi, TrackerBundle bundle )
    {
        List<TrackerErrorReport> errorReports = new ArrayList<>();

        Set<ProgramStageInstance> notDeletedProgramStageInstances = pi.getProgramStageInstances().stream()
            .filter( psi -> !psi.isDeleted() )
            .collect( Collectors.toSet() );

        if ( !notDeletedProgramStageInstances.isEmpty() && !user.isAuthorized( Authorities.F_ENROLLMENT_CASCADE_DELETE.getAuthority() ) )
        {
            TrackerErrorReport trackerErrorReport = TrackerErrorReport.builder()
                .errorCode( TrackerErrorCode.E1091 )
                .addArg( bundle.getUser().getSurname() ).addArg( pi.getUid() )
                .build( bundle );

            errorReports.add( trackerErrorReport );
        }

        List<String> errors = trackerAccessManager.canDelete( user, pi, false );

        if ( !errors.isEmpty() )
        {
            errors.forEach( error -> errorReports.add( TrackerErrorReport.builder()
                .errorMessage( error )
                .build( bundle ) ) );
        }

        return errorReports;
    }

    private List<TrackerErrorReport> isAllowedToDeleteTrackedEntity( int index, User user,
        org.hisp.dhis.trackedentity.TrackedEntityInstance tei, TrackerBundle bundle )
    {
        List<TrackerErrorReport> errorReports = new ArrayList<>();

        Set<ProgramInstance> programInstances = tei.getProgramInstances().stream()
            .filter( pi -> !pi.isDeleted() )
            .collect( Collectors.toSet() );

        if ( !programInstances.isEmpty() && !user.isAuthorized( Authorities.F_TEI_CASCADE_DELETE.getAuthority() ) )
        {
            TrackerErrorReport trackerErrorReport = TrackerErrorReport.builder()
                .errorCode( TrackerErrorCode.E1100 )
                .addArg( bundle.getUser().getSurname() ).addArg( tei.getUid() )
                .build( bundle );

            errorReports.add( trackerErrorReport );
        }

        List<String> errors = trackerAccessManager.canWrite( user, tei );

        if ( !errors.isEmpty() )
        {
            errors.forEach( error -> errorReports.add( TrackerErrorReport.builder()
                .errorMessage( error )
                .build( bundle ) ) );
        }

        return errorReports;
    }

    private TrackerTypeReport addErrorToTypeReport( TrackerTypeReport typeReport, TrackerObjectReport trackerObjectReport,
        List<TrackerErrorReport> trackerErrorReports  ,int index, String uid )
    {
        trackerObjectReport.getErrorReports().addAll( trackerErrorReports );
        trackerObjectReport.setIndex( index );
        trackerObjectReport.setUid( uid );

        typeReport.addObjectReport( trackerObjectReport );
        typeReport.getStats().incIgnored();
        return typeReport;
    }
}
