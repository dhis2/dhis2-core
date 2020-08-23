package org.hisp.dhis.tracker;

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

import com.google.common.collect.Lists;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.relationship.RelationshipService;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.converter.EnrollmentTrackerConverterService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    private final Notifier notifier;

    public DefaultTrackerObjectsDeletionService( ProgramInstanceService programInstanceService,
         TrackedEntityInstanceService entityInstanceService,
         ProgramStageInstanceService stageInstanceService,
         RelationshipService relationshipService,
         TrackerAccessManager trackerAccessManager,
         EnrollmentTrackerConverterService enrollmentTrackerConverterService,
         Notifier notifier )
    {
        this.programInstanceService = programInstanceService;
        this.teiService = entityInstanceService;
        this.programStageInstanceService = stageInstanceService;
        this.relationshipService = relationshipService;
        this.trackerAccessManager = trackerAccessManager;
        this.enrollmentTrackerConverterService = enrollmentTrackerConverterService;
        this.notifier = notifier;
    }

    @Override
    public TrackerTypeReport deleteEnrollments( TrackerBundle bundle, TrackerType trackerType )
    {
        TrackerTypeReport typeReport = new TrackerTypeReport( trackerType );

        List<Enrollment> enrollments = bundle.getEnrollments();

        for ( int idx = 0; idx < enrollments.size(); idx++ )
        {
            String uid = enrollments.get( idx ).getEnrollment();

            boolean existsEnrollment = programInstanceService.programInstanceExists( uid );

            TrackerObjectReport trackerObjectReport = new TrackerObjectReport( TrackerType.ENROLLMENT );

            if ( existsEnrollment )
            {
                ProgramInstance programInstance = programInstanceService.getProgramInstance( uid );

                if ( bundle.getUser() != null )
                {
                    List<TrackerErrorReport> trackerErrorReports = isAllowedToDeleteEnrollment( idx, bundle.getUser(), programInstance, bundle );

                    if ( !trackerErrorReports.isEmpty() )
                    {
                        trackerObjectReport.getErrorReports().addAll( trackerErrorReports );
                        trackerObjectReport.setIndex( idx );
                        trackerObjectReport.setUid( uid );

                        typeReport.addObjectReport( trackerObjectReport );
                        typeReport.getStats().incIgnored();
                        return typeReport;
                    }
                }

                programInstanceService.deleteProgramInstance( programInstance );
                teiService.updateTrackedEntityInstance( programInstance.getEntityInstance() );

                typeReport.addObjectReport( trackerObjectReport );
                typeReport.getStats().incDeleted();
            }
            else
            {
                trackerObjectReport.getErrorReports().add( TrackerErrorReport.builder()
                    .mainKlass( ProgramInstance.class )
                    .listIndex( idx )
                    .errorCode( TrackerErrorCode.E1081 )
                    .addArg( uid )
                    .build( bundle ) );

                trackerObjectReport.setIndex( idx );
                trackerObjectReport.setUid( uid );

                typeReport.addObjectReport( trackerObjectReport );
                typeReport.getStats().incIgnored();
            }
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

            boolean existsEvent = programStageInstanceService.programStageInstanceExists( uid );

            TrackerObjectReport trackerObjectReport = new TrackerObjectReport( TrackerType.EVENT );

            if ( existsEvent )
            {
                List<TrackerErrorReport> trackerErrorReports = new ArrayList<>();

                ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( uid );

                List<String> errors = trackerAccessManager.canDelete( bundle.getUser(), programStageInstance, false );

                if ( !errors.isEmpty() )
                {
                    errors.forEach( error -> trackerErrorReports.add( TrackerErrorReport.builder()
                            .mainKlass( ProgramStageInstance.class )
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
            else
            {
                trackerObjectReport.getErrorReports()
                    .add( TrackerErrorReport.builder().listIndex( idx ).errorCode( TrackerErrorCode.E1032 ).addArg( uid )
                    .mainKlass( ProgramStageInstance.class )
                    .build( bundle ) );

                trackerObjectReport.setIndex( idx );
                trackerObjectReport.setUid( uid );

                typeReport.addObjectReport( trackerObjectReport );
                typeReport.getStats().incIgnored();
            }
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

            boolean teiExists = teiService.trackedEntityInstanceExists( uid );

            TrackerObjectReport trackerObjectReport = new TrackerObjectReport( TrackerType.TRACKED_ENTITY );

            if ( teiExists )
            {
                org.hisp.dhis.trackedentity.TrackedEntityInstance daoEntityInstance = teiService
                        .getTrackedEntityInstance( uid );

                if ( bundle.getUser() != null )
                {
                    List<TrackerErrorReport> trackerErrorReports = isAllowedToDeleteTrackedEntity( idx, bundle.getUser(), daoEntityInstance, bundle );

                    if ( !trackerErrorReports.isEmpty() )
                    {
                        trackerObjectReport.getErrorReports().addAll( trackerErrorReports );
                        trackerObjectReport.setIndex( idx );
                        trackerObjectReport.setUid( uid );

                        typeReport.addObjectReport( trackerObjectReport );
                        typeReport.getStats().incIgnored();
                        return typeReport;
                    }
                }

                Set<ProgramInstance> programInstances = daoEntityInstance.getProgramInstances();

                List<Enrollment> enrollments = enrollmentTrackerConverterService.to( Lists.newArrayList( programInstances ) );

                TrackerBundle trackerBundle = TrackerBundle.builder().enrollments( enrollments ).user( bundle.getUser() ).build();

                // Associated enrollments should be deleted provided user has authority for that.
                TrackerTypeReport enrollmentReport = deleteEnrollments( trackerBundle, TrackerType.ENROLLMENT );

                if ( !enrollmentReport.isEmpty() )
                {
                    trackerObjectReport.getErrorReports().addAll( enrollmentReport.getErrorReports() );
                }

                teiService.deleteTrackedEntityInstance( daoEntityInstance );

                typeReport.addObjectReport( trackerObjectReport );
                typeReport.getStats().incDeleted();
            }
            else
            {
                trackerObjectReport.getErrorReports()
                    .add( TrackerErrorReport.builder().listIndex( idx ).errorCode( TrackerErrorCode.E1063 ).addArg( uid )
                    .mainKlass( TrackedEntityInstance.class )
                    .build( bundle ) );

                trackerObjectReport.setIndex( idx );
                trackerObjectReport.setUid( uid );

                typeReport.addObjectReport( trackerObjectReport );
                typeReport.getStats().incIgnored();
            }
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

            boolean existsRelationShip = relationshipService.relationshipExists( uid );

            TrackerObjectReport trackerObjectReport = new TrackerObjectReport( TrackerType.RELATIONSHIP );

            if ( existsRelationShip )
            {
                List<TrackerErrorReport> trackerErrorReports = new ArrayList<>();

                org.hisp.dhis.relationship.Relationship relationship = relationshipService.getRelationship( uid );

                List<String> errors = trackerAccessManager.canWrite( bundle.getUser(), relationship );

                if ( !errors.isEmpty() )
                {
                    errors.forEach( error -> trackerErrorReports.add( TrackerErrorReport.builder()
                        .mainKlass( org.hisp.dhis.relationship.Relationship.class )
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
            else
            {
                trackerObjectReport.getErrorReports()
                    .add( TrackerErrorReport.builder().listIndex( idx ).errorCode( TrackerErrorCode.E4005 ).addArg( uid )
                    .mainKlass( org.hisp.dhis.relationship.Relationship.class )
                    .build( bundle ) );

                trackerObjectReport.setIndex( idx );
                trackerObjectReport.setUid( uid );

                typeReport.addObjectReport( trackerObjectReport );
                typeReport.getStats().incIgnored();
            }
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
                .mainKlass( ProgramInstance.class )
                .listIndex( index )
                .errorCode( TrackerErrorCode.E1091 )
                .addArg( bundle.getUser().getSurname() ).addArg( pi.getUid() )
                .build( bundle );

            errorReports.add( trackerErrorReport );
        }

        List<String> errors = trackerAccessManager.canDelete( user, pi, false );

        if ( !errors.isEmpty() )
        {
            errors.forEach( error -> errorReports.add( TrackerErrorReport.builder()
                .mainKlass( ProgramInstance.class )
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
                .mainKlass( TrackedEntityInstance.class )
                .listIndex( index )
                .errorCode( TrackerErrorCode.E1100 )
                .addArg( bundle.getUser().getSurname() ).addArg( tei.getUid() )
                .build( bundle );

            errorReports.add( trackerErrorReport );
        }

        List<String> errors = trackerAccessManager.canWrite( user, tei );

        if ( !errors.isEmpty() )
        {
            errors.forEach( error -> errorReports.add( TrackerErrorReport.builder()
                .mainKlass( TrackedEntityInstance.class )
                .errorMessage( error )
                .build( bundle ) ) );
        }

        return errorReports;
    }
}
