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
package org.hisp.dhis.tracker.relationship;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dxf2.events.EnrollmentParams;
import org.hisp.dhis.dxf2.events.EventParams;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentService;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwner;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingAndSortingCriteriaAdapter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service( "org.hisp.dhis.tracker.relationship.RelationshipService" )
@Transactional( readOnly = true )
@RequiredArgsConstructor
public class DefaultRelationshipService implements RelationshipService
{
    private final CurrentUserService currentUserService;

    private final TrackerAccessManager trackerAccessManager;

    private final org.hisp.dhis.relationship.RelationshipStore relationshipStore;

    private final TrackedEntityInstanceService trackedEntityInstanceService;

    private final EnrollmentService enrollmentService;

    private final EventService eventService;

    @Override
    public List<Relationship> getRelationshipsByTrackedEntityInstance(
        TrackedEntityInstance tei,
        PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter )
    {
        User user = currentUserService.getCurrentUser();

        return relationshipStore
            .getByTrackedEntityInstance( tei, pagingAndSortingCriteriaAdapter )
            .stream()
            .filter( r -> trackerAccessManager.canRead( user, r ).isEmpty() )
            .map( this::map )
            .collect( Collectors.toList() );
    }

    @Override
    public List<Relationship> getRelationshipsByProgramInstance( ProgramInstance pi,
        PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter )
    {
        User user = currentUserService.getCurrentUser();

        return relationshipStore
            .getByProgramInstance( pi, pagingAndSortingCriteriaAdapter ).stream()
            .filter( r -> trackerAccessManager.canRead( user, r ).isEmpty() )
            .map( this::map )
            .collect( Collectors.toList() );
    }

    @Override
    public List<Relationship> getRelationshipsByProgramStageInstance( ProgramStageInstance psi,
        PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter )
    {
        User user = currentUserService.getCurrentUser();

        return relationshipStore
            .getByProgramStageInstance( psi, pagingAndSortingCriteriaAdapter )
            .stream()
            .filter( r -> trackerAccessManager.canRead( user, r ).isEmpty() )
            .map( this::map )
            .collect( Collectors.toList() );
    }

    @Override
    public Optional<Relationship> findRelationshipByUid( String uid )
    {
        Relationship relationship = relationshipStore.getByUid( uid );

        if ( relationship == null )
        {
            return Optional.empty();
        }

        User user = currentUserService.getCurrentUser();
        List<String> errors = trackerAccessManager.canRead( user, relationship );

        if ( !errors.isEmpty() )
        {
            return Optional.empty();
        }

        return Optional.of( map( relationship ) );
    }

    /**
     * Map to a non-proxied Relationship to prevent hibernate exceptions.
     */
    private Relationship map( Relationship relationship )
    {
        Relationship result = new Relationship();
        result.setUid( relationship.getUid() );
        result.setCreated( relationship.getCreated() );
        result.setCreatedBy( relationship.getCreatedBy() );
        result.setLastUpdated( relationship.getLastUpdated() );
        result.setLastUpdatedBy( relationship.getLastUpdatedBy() );
        RelationshipType type = new RelationshipType();
        type.setUid( relationship.getRelationshipType().getUid() );
        result.setRelationshipType( relationship.getRelationshipType() );
        result.setFrom( withNestedEntity( relationship.getFrom() ) );
        result.setTo( withNestedEntity( relationship.getTo() ) );
        return result;
    }

    private RelationshipItem withNestedEntity( RelationshipItem item )
    {
        RelationshipItem result = new RelationshipItem();

        if ( item.getTrackedEntityInstance() != null )
        {
            result.setTrackedEntityInstance( map( trackedEntityInstanceService
                .getTrackedEntityInstance( item.getTrackedEntityInstance(), TrackedEntityInstanceParams.TRUE ) ) );
        }
        else if ( item.getProgramInstance() != null )
        {
            result.setProgramInstance(
                map( enrollmentService.getEnrollment( item.getProgramInstance(), EnrollmentParams.TRUE ) ) );
        }
        else if ( item.getProgramStageInstance() != null )
        {
            result.setProgramStageInstance(
                map( eventService.getEvent( item.getProgramStageInstance(), EventParams.FALSE ) ) );
        }

        return result;
    }

    /**
     * Reverse mapping of the one done in
     * {@link org.hisp.dhis.dxf2.events.trackedentity.AbstractTrackedEntityInstanceService#getTei}.
     * NOTE: remove once we have a new tracker service returning
     * org.hisp.dhis.trackedentity.TrackedEntityInstance while providing the
     * logic (ACL checks, ...) we have in the dxf2 services.
     */
    private TrackedEntityInstance map( org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance tei )
    {
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
        result.setDeleted( tei.isDeleted() );
        result.setPotentialDuplicate( tei.isPotentialDuplicate() );
        result.setStoredBy( tei.getStoredBy() );
        result.setCreatedByUserInfo( tei.getCreatedByUserInfo() );
        result.setLastUpdatedByUserInfo( tei.getLastUpdatedByUserInfo() );

        // NOTE: skipping mapping of trackedEntity.relationships as /tracker/relationships models do not include the
        // relationships fields of each of its nested entities as relationship items would then end in an infinite recursion

        result.setProgramInstances( tei.getEnrollments().stream().map( this::map ).collect( Collectors.toSet() ) );
        result.setProgramOwners( tei.getProgramOwners().stream().map( this::map ).collect( Collectors.toSet() ) );
        result.setTrackedEntityAttributeValues(
            tei.getAttributes().stream().map( this::map ).collect( Collectors.toSet() ) );

        return result;
    }

    private TrackedEntityProgramOwner map( org.hisp.dhis.dxf2.events.trackedentity.ProgramOwner programOwner )
    {
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

    private ProgramInstance map( org.hisp.dhis.dxf2.events.enrollment.Enrollment enrollment )
    {
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
                enrollment.getAttributes().stream().map( this::map )
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

        result.setStatus( enrollment.getStatus().getProgramStatus() );
        result.setEnrollmentDate( enrollment.getEnrollmentDate() );
        result.setIncidentDate( enrollment.getIncidentDate() );
        result.setFollowup( enrollment.getFollowup() );
        result.setEndDate( enrollment.getCompletedDate() );
        result.setCompletedBy( enrollment.getCompletedBy() );
        result.setStoredBy( enrollment.getStoredBy() );
        result.setCreatedByUserInfo( enrollment.getCreatedByUserInfo() );
        result.setLastUpdatedByUserInfo( enrollment.getLastUpdatedByUserInfo() );
        result.setDeleted( enrollment.isDeleted() );

        result
            .setProgramStageInstances( enrollment.getEvents().stream().map( this::map ).collect( Collectors.toSet() ) );
        result.setComments( enrollment.getNotes().stream().map( this::map ).collect( Collectors.toList() ) );

        return result;
    }

    private TrackedEntityComment map( org.hisp.dhis.dxf2.events.event.Note note )
    {
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
        result.setDeleted( event.isDeleted() );

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
        programInstance.setStatus( event.getEnrollmentStatus().getProgramStatus() );

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

        // NOTE: skipping mapping of event.relationships as /tracker/relationships models do not include the
        // relationships fields of each of its nested entities as relationship items would then end in an infinite recursion

        result.setEventDataValues(
            event.getDataValues().stream().map( this::map ).collect( Collectors.toSet() ) );
        result.setComments(
            event.getNotes().stream().map( this::map ).collect( Collectors.toList() ) );

        return result;
    }

    private EventDataValue map( org.hisp.dhis.dxf2.events.event.DataValue dataValue )
    {
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
}
