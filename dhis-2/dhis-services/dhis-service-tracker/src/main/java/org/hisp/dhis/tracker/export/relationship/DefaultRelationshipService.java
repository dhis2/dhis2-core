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
package org.hisp.dhis.tracker.export.relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentParams;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.export.event.EventParams;
import org.hisp.dhis.tracker.export.event.EventService;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingAndSortingCriteriaAdapter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service( "org.hisp.dhis.tracker.export.relationship.RelationshipService" )
@Transactional( readOnly = true )
@RequiredArgsConstructor
public class DefaultRelationshipService implements RelationshipService
{
    private final CurrentUserService currentUserService;

    private final TrackerAccessManager trackerAccessManager;

    private final org.hisp.dhis.relationship.RelationshipStore relationshipStore;

    private final TrackedEntityService trackedEntityService;

    private final EnrollmentService enrollmentService;

    private final EventService eventService;

    @Override
    public Relationship getRelationship( String uid )
        throws ForbiddenException,
        NotFoundException
    {
        Relationship relationship = relationshipStore.getByUid( uid );

        if ( relationship == null )
        {
            throw new NotFoundException( Relationship.class, uid );
        }

        User user = currentUserService.getCurrentUser();
        List<String> errors = trackerAccessManager.canRead( user, relationship );
        if ( !errors.isEmpty() )
        {
            throw new ForbiddenException( errors.toString() );
        }

        return map( relationship );
    }

    @Override
    public Optional<Relationship> findRelationshipByUid( String uid )
        throws ForbiddenException,
        NotFoundException
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

    @Override
    public List<Relationship> getRelationshipsByTrackedEntity(
        TrackedEntity trackedEntity,
        PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter )
        throws ForbiddenException,
        NotFoundException
    {

        List<Relationship> relationships = relationshipStore
            .getByTrackedEntity( trackedEntity, pagingAndSortingCriteriaAdapter )
            .stream()
            .filter( r -> trackerAccessManager.canRead( currentUserService.getCurrentUser(), r ).isEmpty() )
            .collect( Collectors.toList() );
        return map( relationships );
    }

    @Override
    public List<Relationship> getRelationshipsByEnrollment( Enrollment enrollment,
        PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter )
        throws ForbiddenException,
        NotFoundException
    {

        List<Relationship> relationships = relationshipStore
            .getByEnrollment( enrollment, pagingAndSortingCriteriaAdapter ).stream()
            .filter( r -> trackerAccessManager.canRead( currentUserService.getCurrentUser(), r ).isEmpty() )
            .collect( Collectors.toList() );
        return map( relationships );
    }

    @Override
    public List<Relationship> getRelationshipsByEvent( Event event,
        PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter )
        throws ForbiddenException,
        NotFoundException
    {
        List<Relationship> relationships = relationshipStore
            .getByEvent( event, pagingAndSortingCriteriaAdapter )
            .stream()
            .filter( r -> trackerAccessManager.canRead( currentUserService.getCurrentUser(), r ).isEmpty() )
            .collect( Collectors.toList() );
        return map( relationships );
    }

    /**
     * Map to a non-proxied Relationship to prevent hibernate exceptions.
     */
    private List<Relationship> map( List<Relationship> relationships )
        throws ForbiddenException,
        NotFoundException
    {
        List<Relationship> result = new ArrayList<>( relationships.size() );
        for ( Relationship relationship : relationships )
        {
            result.add( map( relationship ) );
        }
        return result;
    }

    private Relationship map( Relationship relationship )
        throws ForbiddenException,
        NotFoundException
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
        throws ForbiddenException,
        NotFoundException
    {
        // relationships of relationship items are not mapped to JSON so there is no need to fetch them
        RelationshipItem result = new RelationshipItem();

        // the call to the individual services is to detach and apply some logic like filtering out attribute values
        // for tracked entity type attributes from enrollment.trackedEntity. Enrollment attributes are actually
        // owned by the TEI and cannot be set on the Enrollment. When returning enrollments in our API an enrollment
        // should only have the program tracked entity attributes.
        if ( item.getTrackedEntity() != null )
        {
            result.setTrackedEntity( trackedEntityService
                .getTrackedEntity( item.getTrackedEntity(),
                    TrackedEntityParams.TRUE.withIncludeRelationships( false ) ) );
        }
        else if ( item.getEnrollment() != null )
        {
            result.setEnrollment(
                enrollmentService.getEnrollment( item.getEnrollment(),
                    EnrollmentParams.TRUE.withIncludeRelationships( false ), false ) );
        }
        else if ( item.getEvent() != null )
        {
            result.setEvent(
                eventService.getEvent( item.getEvent(),
                    EventParams.TRUE.withIncludeRelationships( false ) ) );
        }

        return result;
    }
}
