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
import org.hisp.dhis.tracker.relationship.RelationshipService;
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

    private final RelationshipService relationshipService;

    @Override
    public List<TrackedEntityInstance> getTrackedEntities( TrackedEntityInstanceQueryParams queryParams,
        TrackedEntityParams params )
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
        // We need to return the full models for relationship items (i.e. trackedEntity, enrollment and event). The
        // getRelationship() implementations of the stores do not provide that functionality right now.
        if ( params.isIncludeRelationships() )
        {
            trackedEntityInstances.forEach( this::mapRelationshipItem );
        }
        if ( params.getEnrollmentParams().isIncludeRelationships() )
        {
            trackedEntityInstances.forEach( t -> t.getProgramInstances().forEach( this::mapRelationshipItem ) );
        }
        if ( params.getEventParams().isIncludeRelationships() )
        {
            trackedEntityInstances.forEach( t -> t.getProgramInstances()
                .forEach( e -> e.getProgramStageInstances().forEach( this::mapRelationshipItem ) ) );
        }

        addSearchAudit( trackedEntityInstances, queryParams.getUser() );

        return trackedEntityInstances;
    }

    private void mapRelationshipItem( TrackedEntityInstance trackedEntity )
    {
        Set<RelationshipItem> result = new HashSet<>();

        for ( RelationshipItem item : trackedEntity.getRelationshipItems() )
        {
            Relationship rel = relationshipService.findRelationshipByUid( item.getRelationship().getUid() ).get();
            if ( rel.getFrom().getTrackedEntityInstance() != null
                && trackedEntity.getUid().equals( rel.getFrom().getTrackedEntityInstance().getUid() ) )
            {
                RelationshipItem from = rel.getFrom();
                from.setRelationship( rel );
                result.add( from );
            }
            else
            {
                RelationshipItem to = rel.getTo();
                to.setRelationship( rel );
                result.add( to );
            }
        }

        trackedEntity.setRelationshipItems( result );
    }

    private void mapRelationshipItem( ProgramInstance enrollment )
    {
        Set<RelationshipItem> result = new HashSet<>();

        for ( RelationshipItem item : enrollment.getRelationshipItems() )
        {
            Relationship rel = relationshipService.findRelationshipByUid( item.getRelationship().getUid() ).get();
            if ( rel.getFrom().getTrackedEntityInstance() != null
                && enrollment.getUid().equals( rel.getFrom().getTrackedEntityInstance().getUid() ) )
            {
                RelationshipItem from = rel.getFrom();
                from.setRelationship( rel );
                result.add( from );
            }
            else
            {
                RelationshipItem to = rel.getTo();
                to.setRelationship( rel );
                result.add( to );
            }
        }

        enrollment.setRelationshipItems( result );
    }

    private void mapRelationshipItem( ProgramStageInstance event )
    {
        Set<RelationshipItem> result = new HashSet<>();

        for ( RelationshipItem item : event.getRelationshipItems() )
        {
            Relationship rel = relationshipService.findRelationshipByUid( item.getRelationship().getUid() ).get();
            if ( rel.getFrom().getTrackedEntityInstance() != null
                && event.getUid().equals( rel.getFrom().getTrackedEntityInstance().getUid() ) )
            {
                RelationshipItem from = rel.getFrom();
                from.setRelationship( rel );
                result.add( from );
            }
            else
            {
                RelationshipItem to = rel.getTo();
                to.setRelationship( rel );
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
    public TrackedEntityInstance getTrackedEntity( String uid, String programIdentifier,
        TrackedEntityParams params )
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

        if ( StringUtils.isNotEmpty( programIdentifier ) )
        {
            Program program = programService.getProgram( programIdentifier );

            if ( program == null )
            {
                throw new NotFoundException( Program.class, programIdentifier );
            }

            if ( !trackerAccessManager.canRead( user, daoTrackedEntityInstance, program, false ).isEmpty() )
            {
                if ( program.getAccessLevel() == AccessLevel.CLOSED )
                {
                    throw new ForbiddenException( TrackerOwnershipManager.PROGRAM_ACCESS_CLOSED );
                }
                throw new ForbiddenException( TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED );
            }
        }
        return getTei( daoTrackedEntityInstance, programIdentifier, params, user );
    }

    private TrackedEntityInstance getTei( TrackedEntityInstance daoTrackedEntityInstance,
        String programIdentifier,
        TrackedEntityParams params, User user )
    {
        if ( daoTrackedEntityInstance == null )
        {
            return null;
        }

        TrackedEntityInstance result = new TrackedEntityInstance();
        result.setUid( daoTrackedEntityInstance.getUid() );
        result.setOrganisationUnit( daoTrackedEntityInstance.getOrganisationUnit() );
        result.setTrackedEntityType( daoTrackedEntityInstance.getTrackedEntityType() );
        result.setCreated( daoTrackedEntityInstance.getCreated() );
        result.setCreatedAtClient( daoTrackedEntityInstance.getCreatedAtClient() );
        result.setLastUpdated( daoTrackedEntityInstance.getLastUpdated() );
        result.setLastUpdatedAtClient( daoTrackedEntityInstance.getLastUpdatedAtClient() );
        result.setInactive( daoTrackedEntityInstance.isInactive() );
        result.setGeometry( daoTrackedEntityInstance.getGeometry() );
        result.setDeleted( daoTrackedEntityInstance.isDeleted() );
        result.setPotentialDuplicate( daoTrackedEntityInstance.isPotentialDuplicate() );
        result.setStoredBy( daoTrackedEntityInstance.getStoredBy() );
        result.setCreatedByUserInfo( daoTrackedEntityInstance.getCreatedByUserInfo() );
        result.setLastUpdatedByUserInfo( daoTrackedEntityInstance.getLastUpdatedByUserInfo() );
        result.setGeometry( daoTrackedEntityInstance.getGeometry() );

        if ( params.isIncludeRelationships() )
        {
            Set<RelationshipItem> items = new HashSet<>();

            for ( RelationshipItem relationshipItem : daoTrackedEntityInstance.getRelationshipItems() )
            {
                org.hisp.dhis.relationship.Relationship daoRelationship = relationshipItem.getRelationship();

                if ( trackerAccessManager.canRead( user, daoRelationship ).isEmpty()
                    && (params.isIncludeDeleted() || !daoRelationship.isDeleted()) )
                {
                    items.add( relationshipItem );
                }
            }

            result.setRelationshipItems( items );
        }

        if ( params.isIncludeEnrollments() )
        {
            Set<ProgramInstance> programInstances = new HashSet<>();

            for ( ProgramInstance programInstance : daoTrackedEntityInstance.getProgramInstances() )
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

            result.setProgramInstances( programInstances );
        }

        if ( params.isIncludeProgramOwners() )
        {
            if ( StringUtils.isNotEmpty( programIdentifier ) )
            {
                Set<TrackedEntityProgramOwner> filteredProgramOwners = daoTrackedEntityInstance.getProgramOwners()
                    .stream()
                    .filter( tei -> tei.getProgram().getUid().equals( programIdentifier ) )
                    .collect( Collectors.toSet() );
                result.setProgramOwners( filteredProgramOwners );
            }
            else
            {
                result.setProgramOwners( daoTrackedEntityInstance.getProgramOwners() );
            }
        }

        Set<TrackedEntityAttribute> readableAttributes = trackedEntityAttributeService
            .getAllUserReadableTrackedEntityAttributes( user );
        Set<TrackedEntityAttributeValue> attributeValues = new HashSet<>();

        for ( TrackedEntityAttributeValue attributeValue : daoTrackedEntityInstance.getTrackedEntityAttributeValues() )
        {
            if ( readableAttributes.contains( attributeValue.getAttribute() ) )
            {
                attributeValues.add( attributeValue );
            }
        }
        result.setTrackedEntityAttributeValues( attributeValues );

        if ( StringUtils.isEmpty( programIdentifier ) )
        {
            // return only tracked entity type attributes
            TrackedEntityType trackedEntityType = daoTrackedEntityInstance.getTrackedEntityType();
            if ( trackedEntityType != null )
            {
                Set<String> tetAttributes = trackedEntityType.getTrackedEntityAttributes().stream()
                    .map( TrackedEntityAttribute::getUid ).collect( Collectors.toSet() );
                Set<TrackedEntityAttributeValue> tetAttributeValues = result.getTrackedEntityAttributeValues().stream()
                    .filter( att -> tetAttributes.contains( att.getAttribute().getUid() ) )
                    .collect( Collectors.toSet() );
                result.setTrackedEntityAttributeValues( tetAttributeValues );
            }
        }

        return result;
    }
}
