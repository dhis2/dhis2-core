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
package org.hisp.dhis.tracker.export.enrollment;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.hisp.dhis.common.Pager.DEFAULT_PAGE_SIZE;
import static org.hisp.dhis.common.SlimPager.FIRST_PAGE;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.SlimPager;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentQueryParams;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.hibernate.HibernateEnrollmentStore;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service( "org.hisp.dhis.tracker.export.enrollment.EnrollmentService" )
public class DefaultEnrollmentService implements org.hisp.dhis.tracker.export.enrollment.EnrollmentService
{
    private final EnrollmentService enrollmentService;

    private final TrackerOwnershipManager trackerOwnershipAccessManager;

    private final TrackedEntityAttributeService trackedEntityAttributeService;

    private final CurrentUserService currentUserService;

    private final TrackerAccessManager trackerAccessManager;

    @Override
    public Enrollment getEnrollment( String uid, EnrollmentParams params )
    {
        Enrollment enrollment = enrollmentService.getEnrollment( uid );
        return enrollment != null ? getEnrollment( enrollment, params ) : null;
    }

    @Override
    public Enrollment getEnrollment( Enrollment enrollment, EnrollmentParams params )
    {
        return getEnrollment( currentUserService.getCurrentUser(), enrollment, params );
    }

    private Enrollment getEnrollment( User user, Enrollment enrollment, EnrollmentParams params )
    {
        List<String> errors = trackerAccessManager.canRead( user, enrollment, false );
        if ( !errors.isEmpty() )
        {
            throw new IllegalQueryException( errors.toString() );
        }

        Enrollment result = new Enrollment();
        result.setUid( enrollment.getUid() );

        if ( enrollment.getEntityInstance() != null )
        {
            TrackedEntity trackedEntity = new TrackedEntity();
            trackedEntity.setUid( enrollment.getEntityInstance().getUid() );
            result.setEntityInstance( trackedEntity );
        }
        result.setOrganisationUnit( enrollment.getOrganisationUnit() );
        result.setGeometry( enrollment.getGeometry() );
        result.setCreated( enrollment.getCreated() );
        result.setCreatedAtClient( enrollment.getCreatedAtClient() );
        result.setLastUpdated( enrollment.getLastUpdated() );
        result.setLastUpdatedAtClient( enrollment.getLastUpdatedAtClient() );
        result.setProgram( enrollment.getProgram() );
        result.setStatus( enrollment.getStatus() );
        result.setEnrollmentDate( enrollment.getEnrollmentDate() );
        result.setIncidentDate( enrollment.getIncidentDate() );
        result.setFollowup( enrollment.getFollowup() );
        result.setEndDate( enrollment.getEndDate() );
        result.setCompletedBy( enrollment.getCompletedBy() );
        result.setStoredBy( enrollment.getStoredBy() );
        result.setCreatedByUserInfo( enrollment.getCreatedByUserInfo() );
        result.setLastUpdatedByUserInfo( enrollment.getLastUpdatedByUserInfo() );
        result.setDeleted( enrollment.isDeleted() );
        result.setComments( enrollment.getComments() );
        if ( params.isIncludeEvents() )
        {
            result.setEvents( getEvents( user, enrollment, params ) );
        }
        if ( params.isIncludeRelationships() )
        {
            result.setRelationshipItems( getRelationshipItems( user, enrollment, params ) );
        }
        if ( params.isIncludeAttributes() )
        {
            result.getEntityInstance()
                .setTrackedEntityAttributeValues( getTrackedEntityAttributeValues( user, enrollment ) );
        }

        return result;
    }

    private Set<Event> getEvents( User user, Enrollment enrollment, EnrollmentParams params )
    {
        Set<Event> events = new HashSet<>();

        for ( Event event : enrollment.getEvents() )
        {
            if ( (params.isIncludeDeleted() || !event.isDeleted())
                && trackerAccessManager.canRead( user, event, true ).isEmpty() )
            {
                events.add( event );
            }
        }
        return events;
    }

    private Set<RelationshipItem> getRelationshipItems( User user, Enrollment enrollment,
        EnrollmentParams params )
    {
        Set<RelationshipItem> relationshipItems = new HashSet<>();

        for ( RelationshipItem relationshipItem : enrollment.getRelationshipItems() )
        {
            org.hisp.dhis.relationship.Relationship daoRelationship = relationshipItem.getRelationship();
            if ( trackerAccessManager.canRead( user, daoRelationship ).isEmpty()
                && (params.isIncludeDeleted() || !daoRelationship.isDeleted()) )
            {
                relationshipItems.add( relationshipItem );
            }
        }

        return relationshipItems;
    }

    private Set<TrackedEntityAttributeValue> getTrackedEntityAttributeValues( User user,
        Enrollment enrollment )
    {
        Set<TrackedEntityAttribute> readableAttributes = trackedEntityAttributeService
            .getAllUserReadableTrackedEntityAttributes( user, List.of( enrollment.getProgram() ), null );
        Set<TrackedEntityAttributeValue> attributeValues = new LinkedHashSet<>();

        for ( TrackedEntityAttributeValue trackedEntityAttributeValue : enrollment.getEntityInstance()
            .getTrackedEntityAttributeValues() )
        {
            if ( readableAttributes.contains( trackedEntityAttributeValue.getAttribute() ) )
            {
                attributeValues.add( trackedEntityAttributeValue );
            }
        }

        return attributeValues;
    }

    @Override
    public Enrollments getEnrollments( EnrollmentQueryParams params )
    {
        Enrollments enrollments = new Enrollments();

        if ( !params.isPaging() && !params.isSkipPaging() )
        {
            params.setDefaultPaging();
        }

        List<Enrollment> enrollmentList = new ArrayList<>(
            enrollmentService.getEnrollments( params ) );
        if ( !params.isSkipPaging() )
        {
            Pager pager;

            if ( params.isTotalPages() )
            {
                int count = enrollmentService.countEnrollments( params );
                pager = new Pager( params.getPageWithDefault(), count, params.getPageSizeWithDefault() );
            }
            else
            {
                pager = handleLastPageFlag( params, enrollmentList );
            }

            enrollments.setPager( pager );
        }

        enrollments.setEnrollments( getEnrollments( enrollmentList ) );

        return enrollments;
    }

    /**
     * This method will apply the logic related to the parameter
     * 'totalPages=false'. This works in conjunction with the method:
     * {@link HibernateEnrollmentStore#getEnrollments(EnrollmentQueryParams)}
     *
     * This is needed because we need to query (pageSize + 1) at DB level. The
     * resulting query will allow us to evaluate if we are in the last page or
     * not. And this is what his method does, returning the respective Pager
     * object.
     *
     * @param params the request params
     * @param enrollments the reference to the list of Enrollment
     * @return the populated SlimPager instance
     */
    private Pager handleLastPageFlag( EnrollmentQueryParams params,
        List<Enrollment> enrollments )
    {
        Integer originalPage = defaultIfNull( params.getPage(), FIRST_PAGE );
        Integer originalPageSize = defaultIfNull( params.getPageSize(), DEFAULT_PAGE_SIZE );
        boolean isLastPage = false;

        if ( isNotEmpty( enrollments ) )
        {
            isLastPage = enrollments.size() <= originalPageSize;
            if ( !isLastPage )
            {
                // Get the same number of elements of the pageSize, forcing
                // the removal of the last additional element added at querying
                // time.
                enrollments.retainAll( enrollments.subList( 0, originalPageSize ) );
            }
        }

        return new SlimPager( originalPage, originalPageSize, isLastPage );
    }

    private List<Enrollment> getEnrollments( Iterable<Enrollment> enrollments )
    {
        List<Enrollment> enrollmentList = new ArrayList<>();
        User user = currentUserService.getCurrentUser();

        for ( Enrollment enrollment : enrollments )
        {
            if ( enrollment != null && trackerOwnershipAccessManager
                .hasAccess( user, enrollment.getEntityInstance(), enrollment.getProgram() ) )
            {
                enrollmentList.add( getEnrollment( user, enrollment, EnrollmentParams.FALSE ) );
            }
        }

        return enrollmentList;
    }
}
