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
package org.hisp.dhis.tracker.export.trackedentity;

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
import org.hisp.dhis.audit.payloads.TrackedEntityAudit;
import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityAuditService;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwner;
import org.hisp.dhis.trackedentity.TrackedEntityQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentParams;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.export.event.EventParams;
import org.hisp.dhis.tracker.export.event.EventService;
import org.hisp.dhis.tracker.export.trackedentity.aggregates.TrackedEntityAggregate;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional( readOnly = true )
@Service( "org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService" )
@RequiredArgsConstructor
public class DefaultTrackedEntityService implements TrackedEntityService
{
    private final org.hisp.dhis.trackedentity.TrackedEntityService teiService;

    private final TrackedEntityAttributeService trackedEntityAttributeService;

    private final TrackedEntityTypeService trackedEntityTypeService;

    private final TrackedEntityAuditService trackedEntityAuditService;

    private final CurrentUserService currentUserService;

    private final TrackerAccessManager trackerAccessManager;

    private final TrackedEntityAggregate trackedEntityAggregate;

    private final ProgramService programService;

    private final EnrollmentService enrollmentService;

    private final EventService eventService;

    @Override
    public TrackedEntity getTrackedEntity( String uid, TrackedEntityParams params )
        throws NotFoundException,
        ForbiddenException
    {
        TrackedEntity daoTrackedEntity = teiService.getTrackedEntity( uid );
        if ( daoTrackedEntity == null )
        {
            throw new NotFoundException( TrackedEntity.class, uid );
        }

        User user = currentUserService.getCurrentUser();
        List<String> errors = trackerAccessManager.canRead( user, daoTrackedEntity );

        if ( !errors.isEmpty() )
        {
            throw new ForbiddenException( errors.toString() );
        }

        return getTrackedEntity( daoTrackedEntity, params, user );
    }

    @Override
    public TrackedEntity getTrackedEntity( String uid, String programIdentifier, TrackedEntityParams params )
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

        TrackedEntity trackedEntity = getTrackedEntity( uid, params );

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
    public TrackedEntity getTrackedEntity( TrackedEntity trackedEntity, TrackedEntityParams params )
    {
        return getTrackedEntity( trackedEntity, params, currentUserService.getCurrentUser() );
    }

    private TrackedEntity getTrackedEntity( TrackedEntity trackedEntity, TrackedEntityParams params,
        User user )
    {
        if ( trackedEntity == null )
        {
            return null;
        }

        TrackedEntity result = new TrackedEntity();
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
            result.setEnrollments( getEnrollments( trackedEntity, params, user ) );
        }
        if ( params.isIncludeProgramOwners() )
        {
            result.setProgramOwners( trackedEntity.getProgramOwners() );
        }
        result.setTrackedEntityAttributeValues( getTrackedEntityAttributeValues( trackedEntity, user ) );

        return result;
    }

    private Set<RelationshipItem> getRelationshipItems( TrackedEntity trackedEntity, TrackedEntityParams params,
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

    private Set<Enrollment> getEnrollments( TrackedEntity trackedEntity, TrackedEntityParams params,
        User user )
    {
        Set<Enrollment> enrollments = new HashSet<>();

        for ( Enrollment enrollment : trackedEntity.getEnrollments() )
        {
            if ( trackerAccessManager.canRead( user, enrollment, false ).isEmpty()
                && (params.isIncludeDeleted() || !enrollment.isDeleted()) )
            {
                Set<Event> events = new HashSet<>();
                for ( Event event : enrollment.getEvents() )
                {
                    if ( params.isIncludeDeleted() || !event.isDeleted() )
                    {
                        events.add( event );
                    }
                }
                enrollment.setEvents( events );
                enrollments.add( enrollment );
            }
        }
        return enrollments;
    }

    private Set<TrackedEntityAttributeValue> getTrackedEntityAttributeValues( TrackedEntity trackedEntity,
        User user )
    {
        Set<TrackedEntityAttribute> readableAttributes = trackedEntityAttributeService
            .getAllUserReadableTrackedEntityAttributes( user );
        return trackedEntity.getTrackedEntityAttributeValues()
            .stream()
            .filter( av -> readableAttributes.contains( av.getAttribute() ) )
            .collect( Collectors.toCollection( LinkedHashSet::new ) );
    }

    private RelationshipItem withNestedEntity( TrackedEntity trackedEntity, RelationshipItem item )
        throws ForbiddenException,
        NotFoundException
    {
        // relationships of relationship items are not mapped to JSON so there is no need to fetch them
        RelationshipItem result = new RelationshipItem();

        if ( item.getTrackedEntity() != null )
        {
            if ( trackedEntity.getUid().equals( item.getTrackedEntity().getUid() ) )
            {
                // only fetch the TEI if we do not already have access to it. meaning the TEI owns the item
                // this is just mapping the TEI
                result.setTrackedEntity( trackedEntity );
            }
            else
            {
                result.setTrackedEntity( getTrackedEntity( item.getTrackedEntity().getUid(),
                    TrackedEntityParams.TRUE.withIncludeRelationships( false ) ) );
            }
        }
        else if ( item.getEnrollment() != null )
        {
            result.setEnrollment(
                enrollmentService.getEnrollment( item.getEnrollment().getUid(),
                    EnrollmentParams.TRUE.withIncludeRelationships( false ) ) );
        }
        else if ( item.getEvent() != null )
        {
            result.setEvent(
                eventService.getEvent( item.getEvent(),
                    EventParams.TRUE.withIncludeRelationships( false ) ) );
        }

        return result;
    }

    @Override
    public List<TrackedEntity> getTrackedEntities( TrackedEntityQueryParams queryParams,
        TrackedEntityParams params )
        throws ForbiddenException,
        NotFoundException
    {
        if ( queryParams == null )
        {
            return Collections.emptyList();
        }

        final List<Long> ids = teiService.getTrackedEntityIds( queryParams, false, false );

        if ( ids.isEmpty() )
        {
            return Collections.emptyList();
        }

        List<TrackedEntity> trackedEntities = this.trackedEntityAggregate.find( ids, params,
            queryParams );

        mapRelationshipItems( trackedEntities, params );

        addSearchAudit( trackedEntities, queryParams.getUser() );

        return trackedEntities;
    }

    /**
     * We need to return the full models for relationship items (i.e.
     * trackedEntity, enrollment and event) in our API. The aggregate stores
     * currently do not support that, so we need to fetch the entities
     * individually.
     */
    private void mapRelationshipItems( List<TrackedEntity> trackedEntities, TrackedEntityParams params )
        throws ForbiddenException,
        NotFoundException
    {
        if ( params.isIncludeRelationships() )
        {
            for ( TrackedEntity trackedEntity : trackedEntities )
            {
                mapRelationshipItems( trackedEntity );
            }
        }
        if ( params.getEnrollmentParams().isIncludeRelationships() )
        {
            for ( TrackedEntity trackedEntity : trackedEntities )
            {
                for ( Enrollment enrollment : trackedEntity.getEnrollments() )
                {
                    mapRelationshipItems( enrollment, trackedEntity );
                }
            }
        }
        if ( params.getEventParams().isIncludeRelationships() )
        {
            for ( TrackedEntity trackedEntity : trackedEntities )
            {
                for ( Enrollment enrollment : trackedEntity.getEnrollments() )
                {
                    for ( Event event : enrollment.getEvents() )
                    {
                        mapRelationshipItems( event, trackedEntity );
                    }
                }
            }
        }
    }

    private void mapRelationshipItems( TrackedEntity trackedEntity )
        throws ForbiddenException,
        NotFoundException
    {
        Set<RelationshipItem> result = new HashSet<>();

        for ( RelationshipItem item : trackedEntity.getRelationshipItems() )
        {
            result.add( mapRelationshipItem( item, trackedEntity, trackedEntity ) );
        }

        trackedEntity.setRelationshipItems( result );
    }

    private void mapRelationshipItems( Enrollment enrollment, TrackedEntity trackedEntity )
        throws ForbiddenException,
        NotFoundException
    {
        Set<RelationshipItem> result = new HashSet<>();

        for ( RelationshipItem item : enrollment.getRelationshipItems() )
        {
            result.add( mapRelationshipItem( item, enrollment, trackedEntity ) );
        }

        enrollment.setRelationshipItems( result );
    }

    private void mapRelationshipItems( Event event, TrackedEntity trackedEntity )
        throws ForbiddenException,
        NotFoundException
    {
        Set<RelationshipItem> result = new HashSet<>();

        for ( RelationshipItem item : event.getRelationshipItems() )
        {
            result.add( mapRelationshipItem( item, event, trackedEntity ) );
        }

        event.setRelationshipItems( result );
    }

    private RelationshipItem mapRelationshipItem( RelationshipItem item, BaseIdentifiableObject itemOwner,
        TrackedEntity trackedEntity )
        throws ForbiddenException,
        NotFoundException
    {
        Relationship rel = item.getRelationship();
        RelationshipItem from = withNestedEntity( trackedEntity, rel.getFrom() );
        from.setRelationship( rel );
        rel.setFrom( from );
        RelationshipItem to = withNestedEntity( trackedEntity, rel.getTo() );
        to.setRelationship( rel );
        rel.setTo( to );

        if ( rel.getFrom().getTrackedEntity() != null
            && itemOwner.getUid().equals( rel.getFrom().getTrackedEntity().getUid() ) )
        {
            return from;
        }

        return to;
    }

    private void addSearchAudit( List<TrackedEntity> trackedEntities, User user )
    {
        if ( trackedEntities.isEmpty() )
        {
            return;
        }
        final String accessedBy = user != null ? user.getUsername() : currentUserService.getCurrentUsername();
        Map<String, TrackedEntityType> tetMap = trackedEntityTypeService.getAllTrackedEntityType().stream()
            .collect( Collectors.toMap( TrackedEntityType::getUid, t -> t ) );

        List<TrackedEntityAudit> auditable = trackedEntities
            .stream()
            .filter( Objects::nonNull )
            .filter( tei -> tei.getTrackedEntityType() != null )
            .filter( tei -> tetMap.get( tei.getTrackedEntityType().getUid() ).isAllowAuditLog() )
            .map(
                tei -> new TrackedEntityAudit( tei.getUid(), accessedBy, AuditType.SEARCH ) )
            .collect( Collectors.toList() );

        if ( !auditable.isEmpty() )
        {
            trackedEntityAuditService.addTrackedEntityAudit( auditable );
        }
    }

    @Override
    public int getTrackedEntityCount( TrackedEntityQueryParams params, boolean skipAccessValidation,
        boolean skipSearchScopeValidation )
    {
        return teiService.getTrackedEntityCount( params, skipAccessValidation, skipSearchScopeValidation );
    }
}
