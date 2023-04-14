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
package org.hisp.dhis.tracker.trackedentity;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.audit.payloads.TrackedEntityInstanceAudit;
import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceAuditService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwner;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.enrollment.EnrollmentParams;
import org.hisp.dhis.tracker.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.event.EventParams;
import org.hisp.dhis.tracker.event.EventService;
import org.hisp.dhis.tracker.trackedentity.aggregates.TrackedEntityAggregate;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional( readOnly = true )
@Service( "org.hisp.dhis.tracker.trackedentity.TrackedEntityService" )
@RequiredArgsConstructor
public class DefaultTrackedEntityService implements TrackedEntityService
{
    private final org.hisp.dhis.trackedentity.TrackedEntityInstanceService teiService;

    private final TrackedEntityAttributeService trackedEntityAttributeService;

    private final TrackedEntityTypeService trackedEntityTypeService;

    private final TrackedEntityInstanceAuditService trackedEntityInstanceAuditService;

    private final CurrentUserService currentUserService;

    private final TrackerAccessManager trackerAccessManager;

    private final TrackedEntityAggregate trackedEntityAggregate;

    private final ProgramService programService;

    private final EnrollmentService enrollmentService;

    private final EventService eventService;

    @Override
    public List<TrackedEntityInstance> getTrackedEntities( TrackedEntityInstanceQueryParams queryParams,
        TrackedEntityParams params )
        throws ForbiddenException,
        NotFoundException
    {
        if ( queryParams == null )
        {
            return Collections.emptyList();
        }

        final List<Long> ids = teiService.getTrackedEntityInstanceIds( queryParams, false, false );

        if ( ids.isEmpty() )
        {
            return Collections.emptyList();
        }

        List<TrackedEntityInstance> trackedEntityInstances = this.trackedEntityAggregate.find( ids, params,
            queryParams );
        // We need to return the full models for relationship items (i.e. trackedEntity, enrollment and event) in our API. The aggregate stores currently do not support that, so we need to fetch the entities individually.
        if ( params.isIncludeRelationships() )
        {
            for ( TrackedEntityInstance trackedEntity : trackedEntityInstances )
            {
                mapRelationshipItem( trackedEntity );
            }
        }
        if ( params.getEnrollmentParams().isIncludeRelationships() )
        {
            for ( TrackedEntityInstance trackedEntity : trackedEntityInstances )
            {
                for ( ProgramInstance programInstance : trackedEntity.getProgramInstances() )
                {
                    mapRelationshipItem( trackedEntity, programInstance );
                }
            }
        }
        if ( params.getEventParams().isIncludeRelationships() )
        {
            for ( TrackedEntityInstance trackedEntity : trackedEntityInstances )
            {
                for ( ProgramInstance enrollment : trackedEntity.getProgramInstances() )
                {
                    for ( ProgramStageInstance event : enrollment.getProgramStageInstances() )
                    {
                        mapRelationshipItem( trackedEntity, event );
                    }
                }
            }
        }

        addSearchAudit( trackedEntityInstances, queryParams.getUser() );

        return trackedEntityInstances;
    }

    private void mapRelationshipItem( TrackedEntityInstance trackedEntity )
        throws ForbiddenException,
        NotFoundException
    {
        Set<RelationshipItem> result = new HashSet<>();

        for ( RelationshipItem item : trackedEntity.getRelationshipItems() )
        {
            Relationship rel = item.getRelationship();
            RelationshipItem from = withNestedEntity( trackedEntity, rel.getFrom() );
            from.setRelationship( rel );
            rel.setFrom( from );
            RelationshipItem to = withNestedEntity( trackedEntity, rel.getTo() );
            to.setRelationship( rel );
            rel.setTo( to );

            if ( rel.getFrom().getTrackedEntityInstance() != null
                && trackedEntity.getUid().equals( rel.getFrom().getTrackedEntityInstance().getUid() ) )
            {
                result.add( from );
            }
            else
            {
                result.add( to );
            }
        }

        trackedEntity.setRelationshipItems( result );
    }

    private void mapRelationshipItem( TrackedEntityInstance trackedEntity, ProgramInstance enrollment )
        throws ForbiddenException,
        NotFoundException
    {
        Set<RelationshipItem> result = new HashSet<>();

        for ( RelationshipItem item : enrollment.getRelationshipItems() )
        {
            Relationship rel = item.getRelationship();
            RelationshipItem from = withNestedEntity( trackedEntity, rel.getFrom() );
            from.setRelationship( rel );
            rel.setFrom( from );
            RelationshipItem to = withNestedEntity( trackedEntity, rel.getTo() );
            to.setRelationship( rel );
            rel.setTo( to );

            if ( rel.getFrom().getTrackedEntityInstance() != null
                && enrollment.getUid().equals( rel.getFrom().getTrackedEntityInstance().getUid() ) )
            {
                result.add( from );
            }
            else
            {
                result.add( to );
            }
        }

        enrollment.setRelationshipItems( result );
    }

    private void mapRelationshipItem( TrackedEntityInstance trackedEntity, ProgramStageInstance event )
        throws ForbiddenException,
        NotFoundException
    {
        Set<RelationshipItem> result = new HashSet<>();

        for ( RelationshipItem item : event.getRelationshipItems() )
        {
            Relationship rel = item.getRelationship();
            RelationshipItem from = withNestedEntity( trackedEntity, rel.getFrom() );
            from.setRelationship( rel );
            rel.setFrom( from );
            RelationshipItem to = withNestedEntity( trackedEntity, rel.getTo() );
            to.setRelationship( rel );
            rel.setTo( to );

            if ( rel.getFrom().getTrackedEntityInstance() != null
                && event.getUid().equals( rel.getFrom().getTrackedEntityInstance().getUid() ) )
            {
                result.add( from );
            }
            else
            {
                result.add( to );
            }
        }

        event.setRelationshipItems( result );
    }

    private void addSearchAudit( List<TrackedEntityInstance> trackedEntityInstances, User user )
    {
        if ( trackedEntityInstances.isEmpty() )
        {
            return;
        }
        final String accessedBy = user != null ? user.getUsername() : currentUserService.getCurrentUsername();
        Map<String, TrackedEntityType> tetMap = trackedEntityTypeService.getAllTrackedEntityType().stream()
            .collect( Collectors.toMap( TrackedEntityType::getUid, t -> t ) );

        List<TrackedEntityInstanceAudit> auditable = trackedEntityInstances
            .stream()
            .filter( Objects::nonNull )
            .filter( tei -> tei.getTrackedEntityType() != null )
            .filter( tei -> tetMap.get( tei.getTrackedEntityType().getUid() ).isAllowAuditLog() )
            .map(
                tei -> new TrackedEntityInstanceAudit( tei.getUid(), accessedBy, AuditType.SEARCH ) )
            .collect( Collectors.toList() );

        if ( !auditable.isEmpty() )
        {
            trackedEntityInstanceAuditService.addTrackedEntityInstanceAudit( auditable );
        }
    }

    @Override
    public int getTrackedEntityCount( TrackedEntityInstanceQueryParams params, boolean skipAccessValidation,
        boolean skipSearchScopeValidation )
    {
        return teiService.getTrackedEntityInstanceCount( params, skipAccessValidation, skipSearchScopeValidation );
    }

    @Override
    public TrackedEntityInstance getTrackedEntity( String uid, TrackedEntityParams params )
        throws NotFoundException,
        ForbiddenException
    {
        TrackedEntityInstance daoTrackedEntityInstance = teiService.getTrackedEntityInstance( uid );
        if ( daoTrackedEntityInstance == null )
        {
            throw new NotFoundException( TrackedEntityInstance.class, uid );
        }

        User user = currentUserService.getCurrentUser();
        List<String> errors = trackerAccessManager.canRead( user, daoTrackedEntityInstance );

        if ( !errors.isEmpty() )
        {
            throw new ForbiddenException( errors.toString() );
        }

        return getTrackedEntity( daoTrackedEntityInstance, params, user );
    }

    @Override
    public TrackedEntityInstance getTrackedEntity( String uid, String programIdentifier, TrackedEntityParams params )
        throws NotFoundException,
        ForbiddenException
    {
        Program program = null;

        if ( StringUtils.isNotEmpty( programIdentifier ) )
        {
            program = programService.getProgram( programIdentifier );

            if ( program == null )
            {
                throw new NotFoundException( Program.class, programIdentifier );
            }
        }

        TrackedEntityInstance trackedEntity = getTrackedEntity( uid, params );

        if ( program != null )
        {
            if ( !trackerAccessManager.canRead( currentUserService.getCurrentUser(), trackedEntity, program, false )
                .isEmpty() )
            {
                if ( program.getAccessLevel() == AccessLevel.CLOSED )
                {
                    throw new ForbiddenException( TrackerOwnershipManager.PROGRAM_ACCESS_CLOSED );
                }
                throw new ForbiddenException( TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED );
            }

            if ( params.isIncludeProgramOwners() )
            {
                Set<TrackedEntityProgramOwner> filteredProgramOwners = trackedEntity.getProgramOwners()
                    .stream()
                    .filter( tei -> tei.getProgram().getUid().equals( programIdentifier ) )
                    .collect( Collectors.toSet() );
                trackedEntity.setProgramOwners( filteredProgramOwners );
            }
        }
        else
        {
            // return only tracked entity type attributes
            TrackedEntityType trackedEntityType = trackedEntity.getTrackedEntityType();
            if ( trackedEntityType != null )
            {
                Set<String> tetAttributes = trackedEntityType.getTrackedEntityAttributes().stream()
                    .map( TrackedEntityAttribute::getUid ).collect( Collectors.toSet() );
                Set<TrackedEntityAttributeValue> tetAttributeValues = trackedEntity.getTrackedEntityAttributeValues()
                    .stream()
                    .filter( att -> tetAttributes.contains( att.getAttribute().getUid() ) )
                    .collect( Collectors.toCollection( LinkedHashSet::new ) );
                trackedEntity.setTrackedEntityAttributeValues( tetAttributeValues );
            }
        }

        return trackedEntity;
    }

    @Override
    public TrackedEntityInstance getTrackedEntity( TrackedEntityInstance trackedEntity, TrackedEntityParams params )
    {
        return getTrackedEntity( trackedEntity, params, currentUserService.getCurrentUser() );
    }

    private TrackedEntityInstance getTrackedEntity( TrackedEntityInstance trackedEntity, TrackedEntityParams params,
        User user )
    {
        if ( trackedEntity == null )
        {
            return null;
        }

        TrackedEntityInstance result = new TrackedEntityInstance();
        result.setUid( trackedEntity.getUid() );
        result.setOrganisationUnit( trackedEntity.getOrganisationUnit() );
        result.setTrackedEntityType( trackedEntity.getTrackedEntityType() );
        result.setCreated( trackedEntity.getCreated() );
        result.setCreatedAtClient( trackedEntity.getCreatedAtClient() );
        result.setLastUpdated( trackedEntity.getLastUpdated() );
        result.setLastUpdatedAtClient( trackedEntity.getLastUpdatedAtClient() );
        result.setInactive( trackedEntity.isInactive() );
        result.setGeometry( trackedEntity.getGeometry() );
        result.setDeleted( trackedEntity.isDeleted() );
        result.setPotentialDuplicate( trackedEntity.isPotentialDuplicate() );
        result.setStoredBy( trackedEntity.getStoredBy() );
        result.setCreatedByUserInfo( trackedEntity.getCreatedByUserInfo() );
        result.setLastUpdatedByUserInfo( trackedEntity.getLastUpdatedByUserInfo() );
        result.setGeometry( trackedEntity.getGeometry() );
        if ( params.isIncludeRelationships() )
        {
            result.setRelationshipItems( getRelationshipItems( trackedEntity, params, user ) );
        }
        if ( params.isIncludeEnrollments() )
        {
            result.setProgramInstances( getProgramInstances( trackedEntity, params, user ) );
        }
        if ( params.isIncludeProgramOwners() )
        {
            result.setProgramOwners( trackedEntity.getProgramOwners() );
        }
        result.setTrackedEntityAttributeValues( getTrackedEntityAttributeValues( trackedEntity, user ) );

        return result;
    }

    private Set<RelationshipItem> getRelationshipItems( TrackedEntityInstance trackedEntity, TrackedEntityParams params,
        User user )
    {
        Set<RelationshipItem> items = new HashSet<>();

        for ( RelationshipItem relationshipItem : trackedEntity.getRelationshipItems() )
        {
            Relationship daoRelationship = relationshipItem.getRelationship();

            if ( trackerAccessManager.canRead( user, daoRelationship ).isEmpty()
                && (params.isIncludeDeleted() || !daoRelationship.isDeleted()) )
            {
                items.add( relationshipItem );
            }
        }
        return items;
    }

    private Set<ProgramInstance> getProgramInstances( TrackedEntityInstance trackedEntity, TrackedEntityParams params,
        User user )
    {
        Set<ProgramInstance> programInstances = new HashSet<>();

        for ( ProgramInstance programInstance : trackedEntity.getProgramInstances() )
        {
            if ( trackerAccessManager.canRead( user, programInstance, false ).isEmpty()
                && (params.isIncludeDeleted() || !programInstance.isDeleted()) )
            {
                Set<ProgramStageInstance> events = new HashSet<>();
                for ( ProgramStageInstance programStageInstance : programInstance.getProgramStageInstances() )
                {
                    if ( params.isIncludeDeleted() || !programStageInstance.isDeleted() )
                    {
                        events.add( programStageInstance );
                    }
                }
                programInstance.setProgramStageInstances( events );
                programInstances.add( programInstance );
            }
        }
        return programInstances;
    }

    private Set<TrackedEntityAttributeValue> getTrackedEntityAttributeValues( TrackedEntityInstance trackedEntity,
        User user )
    {
        Set<TrackedEntityAttribute> readableAttributes = trackedEntityAttributeService
            .getAllUserReadableTrackedEntityAttributes( user );
        return trackedEntity.getTrackedEntityAttributeValues()
            .stream()
            .filter( av -> readableAttributes.contains( av.getAttribute() ) )
            .collect( Collectors.toCollection( LinkedHashSet::new ) );
    }

    private RelationshipItem withNestedEntity( TrackedEntityInstance trackedEntity, RelationshipItem item )
        throws ForbiddenException,
        NotFoundException
    {
        // relationships of relationship items are not mapped to JSON so there is no need to fetch them
        RelationshipItem result = new RelationshipItem();

        if ( item.getTrackedEntityInstance() != null )
        {
            if ( trackedEntity.getUid().equals( item.getTrackedEntityInstance().getUid() ) )
            {
                // only fetch the TEI if we do not already have access to it. meaning the TEI owns the item
                // this is just mapping the TEI
                result.setTrackedEntityInstance( trackedEntity );
            }
            else
            {
                result.setTrackedEntityInstance( getTrackedEntity( item.getTrackedEntityInstance().getUid(),
                    TrackedEntityParams.TRUE.withIncludeRelationships( false ) ) );
            }
        }
        else if ( item.getProgramInstance() != null )
        {
            result.setProgramInstance(
                enrollmentService.getEnrollment( item.getProgramInstance().getUid(),
                    EnrollmentParams.TRUE.withIncludeRelationships( false ) ) );
        }
        else if ( item.getProgramStageInstance() != null )
        {
            result.setProgramStageInstance(
                eventService.getEvent( item.getProgramStageInstance(),
                    EventParams.TRUE.withIncludeRelationships( false ) ) );
        }

        return result;
    }
}
