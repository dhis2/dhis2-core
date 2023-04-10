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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.audit.payloads.TrackedEntityInstanceAudit;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.events.aggregates.TrackedEntityInstanceAggregate;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
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
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.tracker.relationship.RelationshipService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional( readOnly = true )
@Service( "org.hisp.dhis.tracker.trackedentity.TrackedEntityService" )
@RequiredArgsConstructor
public class DefaultTrackedEntityService implements TrackedEntityService
{
    private final org.hisp.dhis.trackedentity.TrackedEntityInstanceService teiService;

    private final RelationshipService relationshipService;

    private final TrackedEntityAttributeService trackedEntityAttributeService;

    private final TrackedEntityTypeService trackedEntityTypeService;

    private final TrackedEntityInstanceAuditService trackedEntityInstanceAuditService;

    private final CurrentUserService currentUserService;

    private final TrackerAccessManager trackerAccessManager;

    private final TrackedEntityInstanceAggregate trackedEntityInstanceAggregate;

    private final ProgramService programService;

    @Override
    public List<TrackedEntityInstance> getTrackedEntities( TrackedEntityInstanceQueryParams queryParams,
        TrackedEntityInstanceParams params )
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

        List<TrackedEntityInstance> trackedEntityInstances = this.trackedEntityInstanceAggregate.find( ids, params,
            queryParams ).stream().map( this::map ).collect( Collectors.toList() );

        addSearchAudit( trackedEntityInstances, queryParams.getUser() );

        return trackedEntityInstances;
    }

    private TrackedEntityInstance map( org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance tei )
    {
        if ( tei == null )
        {
            return null;
        }

        TrackedEntityInstance result = new TrackedEntityInstance();
        result.setUid( tei.getTrackedEntityInstance() );
        OrganisationUnit orgUnit = new OrganisationUnit();
        orgUnit.setUid( tei.getOrgUnit() );
        result.setOrganisationUnit( orgUnit );
        TrackedEntityType trackedEntityType = new TrackedEntityType();
        trackedEntityType.setUid( tei.getTrackedEntityType() );
        result.setTrackedEntityType( trackedEntityType );
        result.setCreated( DateUtils.parseDate( tei.getCreated() ) );
        result.setCreatedAtClient( DateUtils.parseDate( tei.getCreatedAtClient() ) );
        result.setLastUpdated( DateUtils.parseDate( tei.getLastUpdated() ) );
        result.setLastUpdatedAtClient( DateUtils.parseDate( tei.getLastUpdatedAtClient() ) );
        result.setInactive( tei.isInactive() );
        result.setGeometry( tei.getGeometry() );
        result.setDeleted( BooleanUtils.toBoolean( tei.isDeleted() ) );
        result.setPotentialDuplicate( BooleanUtils.toBoolean( tei.isPotentialDuplicate() ) );
        result.setStoredBy( tei.getStoredBy() );
        result.setCreatedByUserInfo( tei.getCreatedByUserInfo() );
        result.setLastUpdatedByUserInfo( tei.getLastUpdatedByUserInfo() );

        result.setRelationshipItems(
            mapTrackedEntityRelationshipItem( tei.getTrackedEntityInstance(), tei.getRelationships() ) );
        result.setProgramInstances( Optional.ofNullable( tei.getEnrollments() ).orElseGet( ArrayList::new ).stream()
            .map( this::map ).collect( Collectors.toSet() ) );
        result.setProgramOwners( Optional.ofNullable( tei.getProgramOwners() ).orElseGet( ArrayList::new ).stream()
            .map( this::map ).collect( Collectors.toSet() ) );
        result.setTrackedEntityAttributeValues(
            Optional.ofNullable( tei.getAttributes() ).orElseGet( ArrayList::new ).stream().map( this::map )
                .collect( Collectors.toSet() ) );

        return result;
    }

    private TrackedEntityProgramOwner map( org.hisp.dhis.dxf2.events.trackedentity.ProgramOwner programOwner )
    {
        if ( programOwner == null )
        {
            return null;
        }

        TrackedEntityInstance trackedEntityInstance = new TrackedEntityInstance();
        trackedEntityInstance.setUid( programOwner.getTrackedEntityInstance() );

        Program program = new Program();
        program.setUid( programOwner.getProgram() );

        OrganisationUnit organisationUnit = new OrganisationUnit();
        organisationUnit.setUid( programOwner.getOwnerOrgUnit() );

        return new TrackedEntityProgramOwner( trackedEntityInstance, program, organisationUnit );
    }

    private TrackedEntityAttributeValue map( org.hisp.dhis.dxf2.events.trackedentity.Attribute att )
    {
        if ( att == null )
        {
            return null;
        }

        TrackedEntityAttribute attribute = new TrackedEntityAttribute();
        attribute.setUid( att.getAttribute() );
        attribute.setValueType( att.getValueType() );
        // note: dxf2 only stores the displayName which is cannot be set on TEA as it's computed from the name
        // this mapping is going to be removed once we have migrated the aggregate code (very soon!)
        attribute.setName( att.getDisplayName() );
        attribute.setCode( att.getCode() );
        attribute.setSkipSynchronization( att.isSkipSynchronization() );
        TrackedEntityAttributeValue result = new TrackedEntityAttributeValue();
        result.setAttribute( attribute );
        result.setCreated( DateUtils.parseDate( att.getCreated() ) );
        result.setLastUpdated( DateUtils.parseDate( att.getLastUpdated() ) );
        result.setValue( att.getValue() );
        result.setStoredBy( att.getStoredBy() );
        return result;
    }

    private ProgramInstance map( Enrollment enrollment )
    {
        if ( enrollment == null )
        {
            return null;
        }

        ProgramInstance result = new ProgramInstance();
        result.setUid( enrollment.getEnrollment() );

        if ( StringUtils.isNotEmpty( enrollment.getTrackedEntityInstance() ) )
        {
            TrackedEntityInstance tei = new TrackedEntityInstance();
            TrackedEntityType type = new TrackedEntityType();
            type.setUid( enrollment.getTrackedEntityType() );
            tei.setTrackedEntityType( type );
            tei.setUid( enrollment.getTrackedEntityInstance() );

            // tei owns all attributes in trackedEntityAttributeValues while programs only present a subset of them
            // the program attributes are the ones attached to the enrollment
            tei.setTrackedEntityAttributeValues(
                Optional.ofNullable( enrollment.getAttributes() ).orElseGet( ArrayList::new ).stream().map( this::map )
                    .collect( Collectors.toSet() ) );

            result.setEntityInstance( tei );
        }

        if ( StringUtils.isNotEmpty( enrollment.getOrgUnit() ) )
        {
            OrganisationUnit orgUnit = new OrganisationUnit();
            orgUnit.setUid( enrollment.getOrgUnit() );
            orgUnit.setName( enrollment.getOrgUnitName() );
            result.setOrganisationUnit( orgUnit );
        }

        result.setGeometry( enrollment.getGeometry() );
        result.setCreated( DateUtils.parseDate( enrollment.getCreated() ) );
        result.setCreatedAtClient( DateUtils.parseDate( enrollment.getCreatedAtClient() ) );
        result.setLastUpdated( DateUtils.parseDate( enrollment.getLastUpdated() ) );
        result.setLastUpdatedAtClient( DateUtils.parseDate( enrollment.getLastUpdatedAtClient() ) );

        Program program = new Program();
        program.setUid( enrollment.getProgram() );
        result.setProgram( program );

        if ( enrollment.getStatus() != null )
        {
            result.setStatus( enrollment.getStatus().getProgramStatus() );
        }
        result.setEnrollmentDate( enrollment.getEnrollmentDate() );
        result.setIncidentDate( enrollment.getIncidentDate() );
        result.setFollowup( enrollment.getFollowup() );
        result.setEndDate( enrollment.getCompletedDate() );
        result.setCompletedBy( enrollment.getCompletedBy() );
        result.setStoredBy( enrollment.getStoredBy() );
        result.setCreatedByUserInfo( enrollment.getCreatedByUserInfo() );
        result.setLastUpdatedByUserInfo( enrollment.getLastUpdatedByUserInfo() );
        result.setDeleted( BooleanUtils.toBoolean( enrollment.isDeleted() ) );

        result.setRelationshipItems(
            mapEnrollmentRelationshipItem( enrollment.getEnrollment(), enrollment.getRelationships() ) );
        result
            .setProgramStageInstances( Optional.ofNullable( enrollment.getEvents() ).orElseGet( ArrayList::new )
                .stream().map( this::map ).collect( Collectors.toSet() ) );
        result.setComments( Optional.ofNullable( enrollment.getNotes() ).orElseGet( ArrayList::new ).stream()
            .map( this::map ).collect( Collectors.toList() ) );

        return result;
    }

    private TrackedEntityComment map( org.hisp.dhis.dxf2.events.event.Note note )
    {
        if ( note == null )
        {
            return null;
        }

        TrackedEntityComment result = new TrackedEntityComment();
        result.setUid( note.getNote() );
        result.setCommentText( note.getValue() );
        result.setCreator( note.getStoredBy() );
        result.setCreated( DateUtils.parseDate( note.getStoredDate() ) );
        result.setLastUpdated( note.getLastUpdated() );
        if ( note.getLastUpdatedBy() != null )
        {
            User user = new User();
            user.setId( note.getLastUpdatedBy().getId() );
            user.setUid( note.getLastUpdatedBy().getUid() );
            user.setCode( note.getLastUpdatedBy().getCode() );
            user.setUsername( note.getLastUpdatedBy().getUsername() );
            user.setFirstName( note.getLastUpdatedBy().getFirstName() );
            user.setSurname( note.getLastUpdatedBy().getSurname() );
            result.setLastUpdatedBy( user );
        }
        return result;
    }

    private ProgramStageInstance map( org.hisp.dhis.dxf2.events.event.Event event )
    {
        if ( event == null )
        {
            return null;
        }

        ProgramStageInstance result = new ProgramStageInstance();
        result.setUid( event.getEvent() );
        result.setStatus( event.getStatus() );
        result.setExecutionDate( DateUtils.parseDate( event.getEventDate() ) );
        result.setDueDate( DateUtils.parseDate( event.getDueDate() ) );
        result.setStoredBy( event.getStoredBy() );
        result.setCompletedBy( event.getCompletedBy() );
        result.setCompletedDate( DateUtils.parseDate( event.getCompletedDate() ) );
        result.setCreated( DateUtils.parseDate( event.getCreated() ) );
        result.setCreatedByUserInfo( event.getCreatedByUserInfo() );
        result.setLastUpdatedByUserInfo( event.getLastUpdatedByUserInfo() );
        result.setCreatedAtClient( DateUtils.parseDate( event.getCreatedAtClient() ) );
        result.setLastUpdated( DateUtils.parseDate( event.getLastUpdated() ) );
        result.setLastUpdatedAtClient( DateUtils.parseDate( event.getLastUpdatedAtClient() ) );
        result.setGeometry( event.getGeometry() );
        result.setDeleted( BooleanUtils.toBoolean( event.isDeleted() ) );

        if ( StringUtils.isNotEmpty( event.getAssignedUser() ) )
        {
            User assignedUser = new User();
            assignedUser.setUid( event.getAssignedUser() );
            assignedUser.setUsername( event.getAssignedUserUsername() );
            assignedUser.setName( event.getAssignedUserDisplayName() );
            assignedUser.setFirstName( event.getAssignedUserFirstName() );
            assignedUser.setSurname( event.getAssignedUserSurname() );
            result.setAssignedUser( assignedUser );
        }

        if ( StringUtils.isNotEmpty( event.getOrgUnit() ) )
        {
            OrganisationUnit orgUnit = new OrganisationUnit();
            orgUnit.setUid( event.getOrgUnit() );
            orgUnit.setName( event.getOrgUnitName() );
            result.setOrganisationUnit( orgUnit );
        }

        ProgramInstance programInstance = new ProgramInstance();
        result.setProgramInstance( programInstance );
        programInstance.setUid( event.getEnrollment() );
        programInstance.setFollowup( event.getFollowup() );
        if ( event.getEnrollmentStatus() != null )
        {
            programInstance.setStatus( event.getEnrollmentStatus().getProgramStatus() );
        }

        Program program = new Program();
        program.setUid( event.getProgram() );
        programInstance.setProgram( program );

        if ( StringUtils.isNotEmpty( event.getTrackedEntityInstance() ) )
        {
            TrackedEntityInstance tei = new TrackedEntityInstance();
            tei.setUid( event.getTrackedEntityInstance() );
            programInstance.setEntityInstance( tei );
        }

        ProgramStage programStage = new ProgramStage();
        programStage.setUid( event.getProgramStage() );
        result.setProgramStage( programStage );

        if ( StringUtils.isNotEmpty( event.getAttributeOptionCombo() ) )
        {
            CategoryOptionCombo coc = new CategoryOptionCombo();
            coc.setUid( event.getAttributeOptionCombo() );
            result.setAttributeOptionCombo( coc );
            if ( StringUtils.isNotEmpty( event.getAttributeCategoryOptions() ) )
            {
                Set<CategoryOption> cos = TextUtils
                    .splitToSet( event.getAttributeCategoryOptions(), TextUtils.SEMICOLON ).stream().map( o -> {
                        CategoryOption co = new CategoryOption();
                        co.setUid( o );
                        return co;
                    } ).collect( Collectors.toSet() );
                coc.setCategoryOptions( cos );
            }
        }

        result.setRelationshipItems( mapEventRelationshipItem( event.getEvent(), event.getRelationships() ) );
        result.setEventDataValues(
            Optional.ofNullable( event.getDataValues() ).orElseGet( HashSet::new ).stream().map( this::map )
                .collect( Collectors.toSet() ) );
        result.setComments(
            Optional.ofNullable( event.getNotes() ).orElseGet( ArrayList::new ).stream().map( this::map )
                .collect( Collectors.toList() ) );

        return result;
    }

    private EventDataValue map( org.hisp.dhis.dxf2.events.event.DataValue dataValue )
    {
        if ( dataValue == null )
        {
            return null;
        }

        EventDataValue result = new EventDataValue();
        result.setCreated( DateUtils.parseDate( dataValue.getCreated() ) );
        result.setCreatedByUserInfo( dataValue.getCreatedByUserInfo() );
        result.setLastUpdated( DateUtils.parseDate( dataValue.getLastUpdated() ) );
        result.setLastUpdatedByUserInfo( dataValue.getLastUpdatedByUserInfo() );
        result.setDataElement( dataValue.getDataElement() );
        result.setValue( dataValue.getValue() );
        result.setProvidedElsewhere( dataValue.getProvidedElsewhere() );
        result.setStoredBy( dataValue.getStoredBy() );
        return result;
    }

    private Set<RelationshipItem> mapTrackedEntityRelationshipItem( String uid,
        Collection<org.hisp.dhis.dxf2.events.trackedentity.Relationship> relationships )
    {
        Set<RelationshipItem> result = new HashSet<>();

        for ( org.hisp.dhis.dxf2.events.trackedentity.Relationship relationship : relationships )
        {
            Relationship rel = relationshipService.findRelationshipByUid( relationship.getRelationship() ).get();
            if ( rel.getFrom().getTrackedEntityInstance() != null
                && uid.equals( rel.getFrom().getTrackedEntityInstance().getUid() ) )
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
        return result;
    }

    private Set<RelationshipItem> mapEnrollmentRelationshipItem( String uid,
        Collection<org.hisp.dhis.dxf2.events.trackedentity.Relationship> relationships )
    {
        Set<RelationshipItem> result = new HashSet<>();

        for ( org.hisp.dhis.dxf2.events.trackedentity.Relationship relationship : relationships )
        {
            Relationship rel = relationshipService.findRelationshipByUid( relationship.getRelationship() ).get();
            if ( rel.getFrom().getProgramInstance() != null
                && uid.equals( rel.getFrom().getProgramInstance().getUid() ) )
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
        return result;
    }

    private Set<RelationshipItem> mapEventRelationshipItem( String uid,
        Collection<org.hisp.dhis.dxf2.events.trackedentity.Relationship> relationships )
    {
        Set<RelationshipItem> result = new HashSet<>();

        for ( org.hisp.dhis.dxf2.events.trackedentity.Relationship relationship : relationships )
        {
            Relationship rel = relationshipService.findRelationshipByUid( relationship.getRelationship() ).get();
            if ( rel.getFrom().getProgramStageInstance() != null
                && uid.equals( rel.getFrom().getProgramStageInstance().getUid() ) )
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
        return result;
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
        TrackedEntityInstanceParams params )
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
        TrackedEntityInstanceParams params, User user )
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
