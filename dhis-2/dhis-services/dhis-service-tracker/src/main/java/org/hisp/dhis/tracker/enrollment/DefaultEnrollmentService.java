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
package org.hisp.dhis.tracker.enrollment;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.hisp.dhis.common.Pager.DEFAULT_PAGE_SIZE;
import static org.hisp.dhis.common.SlimPager.FIRST_PAGE;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.SlimPager;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceQueryParams;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.relationship.RelationshipItem;
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
@Service( "org.hisp.dhis.tracker.enrollment.EnrollmentService" )
public class DefaultEnrollmentService implements EnrollmentService
{
    private final ProgramInstanceService programInstanceService;

    private final TrackerOwnershipManager trackerOwnershipAccessManager;

    private final TrackedEntityAttributeService trackedEntityAttributeService;

    private final CurrentUserService currentUserService;

    private final TrackerAccessManager trackerAccessManager;

    @Override
    public ProgramInstance getEnrollment( String uid, EnrollmentParams params )
    {
        ProgramInstance programInstance = programInstanceService.getProgramInstance( uid );
        return programInstance != null ? getEnrollment( programInstance, params ) : null;
    }

    private ProgramInstance getEnrollment( ProgramInstance programInstance, EnrollmentParams params )
    {
        return getEnrollment( currentUserService.getCurrentUser(), programInstance, params, false );
    }

    private ProgramInstance getEnrollment( User user, ProgramInstance programInstance, EnrollmentParams params,
        boolean skipOwnershipCheck )
    {
        List<String> errors = trackerAccessManager.canRead( user, programInstance, skipOwnershipCheck );
        if ( !errors.isEmpty() )
        {
            throw new IllegalQueryException( errors.toString() );
        }

        ProgramInstance result = new ProgramInstance();
        result.setUid( programInstance.getUid() );
        result.setEntityInstance( programInstance.getEntityInstance() );
        result.setOrganisationUnit( programInstance.getOrganisationUnit() );
        result.setGeometry( programInstance.getGeometry() );
        result.setCreated( programInstance.getCreated() );
        result.setCreatedAtClient( programInstance.getCreatedAtClient() );
        result.setLastUpdated( programInstance.getLastUpdated() );
        result.setLastUpdatedAtClient( programInstance.getLastUpdatedAtClient() );
        result.setProgram( programInstance.getProgram() );
        result.setStatus( programInstance.getStatus() );
        result.setEnrollmentDate( programInstance.getEnrollmentDate() );
        result.setIncidentDate( programInstance.getIncidentDate() );
        result.setFollowup( programInstance.getFollowup() );
        result.setEndDate( programInstance.getEndDate() );
        result.setCompletedBy( programInstance.getCompletedBy() );
        result.setStoredBy( programInstance.getStoredBy() );
        result.setCreatedByUserInfo( programInstance.getCreatedByUserInfo() );
        result.setLastUpdatedByUserInfo( programInstance.getLastUpdatedByUserInfo() );
        result.setDeleted( programInstance.isDeleted() );
        result.setComments( programInstance.getComments() );

        if ( params.isIncludeEvents() )
        {
            Set<ProgramStageInstance> programStageInstances = new HashSet<>();

            for ( ProgramStageInstance programStageInstance : programInstance.getProgramStageInstances() )
            {
                if ( (params.isIncludeDeleted() || !programStageInstance.isDeleted())
                    && trackerAccessManager.canRead( user, programStageInstance, true ).isEmpty() )
                {
                    programStageInstances.add( programStageInstance );
                }
            }

            result.setProgramStageInstances( programStageInstances );
        }

        if ( params.isIncludeRelationships() )
        {
            Set<RelationshipItem> relationshipItems = new HashSet<>();

            for ( RelationshipItem relationshipItem : programInstance.getRelationshipItems() )
            {
                org.hisp.dhis.relationship.Relationship daoRelationship = relationshipItem.getRelationship();
                if ( trackerAccessManager.canRead( user, daoRelationship ).isEmpty()
                    && (params.isIncludeDeleted() || !daoRelationship.isDeleted()) )
                {
                    relationshipItems.add( relationshipItem );
                }
            }

            result.setRelationshipItems( relationshipItems );
        }

        if ( params.isIncludeAttributes() )
        {
            Set<TrackedEntityAttribute> readableAttributes = trackedEntityAttributeService
                .getAllUserReadableTrackedEntityAttributes( user, List.of( programInstance.getProgram() ), null );
            Set<TrackedEntityAttributeValue> attributeValues = new HashSet<>();

            for ( TrackedEntityAttributeValue trackedEntityAttributeValue : programInstance.getEntityInstance()
                .getTrackedEntityAttributeValues() )
            {
                if ( readableAttributes.contains( trackedEntityAttributeValue.getAttribute() ) )
                {
                    attributeValues.add( trackedEntityAttributeValue );
                }
            }
            result.getEntityInstance().setTrackedEntityAttributeValues( attributeValues );
        }
        return result;
    }

    @Override
    public Enrollments getEnrollments( ProgramInstanceQueryParams params )
    {
        Enrollments enrollments = new Enrollments();
        List<ProgramInstance> programInstances = new ArrayList<>();

        if ( !params.isPaging() && !params.isSkipPaging() )
        {
            params.setDefaultPaging();
        }

        programInstances.addAll( programInstanceService.getProgramInstances( params ) );

        if ( !params.isSkipPaging() )
        {
            Pager pager;

            if ( params.isTotalPages() )
            {
                int count = programInstanceService.countProgramInstances( params );
                pager = new Pager( params.getPageWithDefault(), count, params.getPageSizeWithDefault() );
            }
            else
            {
                pager = handleLastPageFlag( params, programInstances );
            }

            enrollments.setPager( pager );
        }

        enrollments.setEnrollments( getEnrollments( programInstances ) );

        return enrollments;
    }

    /**
     * This method will apply the logic related to the parameter
     * 'totalPages=false'. This works in conjunction with the method:
     * {@link org.hisp.dhis.program.hibernate.HibernateProgramInstanceStore#getProgramInstances(ProgramInstanceQueryParams)}
     *
     * This is needed because we need to query (pageSize + 1) at DB level. The
     * resulting query will allow us to evaluate if we are in the last page or
     * not. And this is what his method does, returning the respective Pager
     * object.
     *
     * @param params the request params
     * @param programInstances the reference to the list of ProgramInstance
     * @return the populated SlimPager instance
     */
    private Pager handleLastPageFlag( ProgramInstanceQueryParams params,
        List<ProgramInstance> programInstances )
    {
        Integer originalPage = defaultIfNull( params.getPage(), FIRST_PAGE );
        Integer originalPageSize = defaultIfNull( params.getPageSize(), DEFAULT_PAGE_SIZE );
        boolean isLastPage = false;

        if ( isNotEmpty( programInstances ) )
        {
            isLastPage = programInstances.size() <= originalPageSize;
            if ( !isLastPage )
            {
                // Get the same number of elements of the pageSize, forcing
                // the removal of the last additional element added at querying
                // time.
                programInstances.retainAll( programInstances.subList( 0, originalPageSize ) );
            }
        }

        return new SlimPager( originalPage, originalPageSize, isLastPage );
    }

    private List<ProgramInstance> getEnrollments( Iterable<ProgramInstance> programInstances )
    {
        List<ProgramInstance> enrollments = new ArrayList<>();
        User user = currentUserService.getCurrentUser();

        for ( ProgramInstance programInstance : programInstances )
        {
            if ( programInstance != null && trackerOwnershipAccessManager
                .hasAccess( user, programInstance.getEntityInstance(), programInstance.getProgram() ) )
            {
                enrollments.add( getEnrollment( user, programInstance, EnrollmentParams.FALSE, true ) );
            }
        }

        return enrollments;
    }
}
