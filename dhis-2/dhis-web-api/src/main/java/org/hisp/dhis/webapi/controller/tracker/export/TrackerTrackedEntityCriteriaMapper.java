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
package org.hisp.dhis.webapi.controller.tracker.export;

import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.OrderColumn.findColumn;
import static org.hisp.dhis.webapi.controller.event.mapper.OrderParamsHelper.toOrderParams;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamUtils.applyIfNonEmpty;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamUtils.parseAndFilterUids;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamUtils.parseAttributeQueryItems;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamUtils.parseUids;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maps query parameters from {@link TrackerTrackedEntitiesExportController}
 * stored in {@link TrackerTrackedEntityCriteria} to
 * {@link TrackedEntityInstanceQueryParams} which is used to fetch tracked
 * entities from the DB.
 *
 * @author Luciano Fiandesio
 */
@Component( "org.hisp.dhis.webapi.controller.tracker.export.TrackedEntityCriteriaMapper" )
@RequiredArgsConstructor
public class TrackerTrackedEntityCriteriaMapper
{
    @Nonnull
    private final CurrentUserService currentUserService;

    @Nonnull
    private final OrganisationUnitService organisationUnitService;

    @Nonnull
    private final ProgramService programService;

    @Nonnull
    private final TrackedEntityTypeService trackedEntityTypeService;

    @Nonnull
    private final TrackedEntityAttributeService attributeService;

    @Transactional( readOnly = true )
    public TrackedEntityInstanceQueryParams map( TrackerTrackedEntityCriteria criteria )
        throws BadRequestException,
        ForbiddenException
    {
        Program program = applyIfNonEmpty( programService::getProgram, criteria.getProgram() );
        validateProgram( criteria.getProgram(), program );
        ProgramStage programStage = validateProgramStage( criteria, program );

        TrackedEntityType trackedEntityType = applyIfNonEmpty( trackedEntityTypeService::getTrackedEntityType,
            criteria.getTrackedEntityType() );
        validateTrackedEntityType( criteria.getTrackedEntityType(), trackedEntityType );

        Set<String> assignedUserIds = parseAndFilterUids( criteria.getAssignedUser() );

        User user = currentUserService.getCurrentUser();
        Set<String> orgUnitIds = parseUids( criteria.getOrgUnit() );
        Set<OrganisationUnit> orgUnits = validateOrgUnits( orgUnitIds, user );
        if ( criteria.getOuMode() == OrganisationUnitSelectionMode.CAPTURE && user != null )
        {
            orgUnits.addAll( user.getOrganisationUnits() );
        }

        QueryFilter queryFilter = getQueryFilter( criteria.getQuery() );

        Map<String, TrackedEntityAttribute> attributes = attributeService.getAllTrackedEntityAttributes()
            .stream().collect( Collectors.toMap( TrackedEntityAttribute::getUid, att -> att ) );

        List<QueryItem> attributeItems = parseAttributeQueryItems( criteria.getAttribute(), attributes );

        List<QueryItem> filters = parseAttributeQueryItems( criteria.getFilter(), attributes );

        validateDuplicatedAttributeFilters( filters );

        List<OrderParam> orderParams = toOrderParams( criteria.getOrder() );
        validateOrderParams( orderParams, attributes );

        Set<String> trackedEntities = parseUids( criteria.getTrackedEntity() );

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
        params.setQuery( queryFilter )
            .setProgram( program )
            .setProgramStage( programStage )
            .setProgramStatus( criteria.getProgramStatus() )
            .setFollowUp( criteria.getFollowUp() )
            .setLastUpdatedStartDate( criteria.getUpdatedAfter() )
            .setLastUpdatedEndDate( criteria.getUpdatedBefore() )
            .setLastUpdatedDuration( criteria.getUpdatedWithin() )
            .setProgramEnrollmentStartDate( criteria.getEnrollmentEnrolledAfter() )
            .setProgramEnrollmentEndDate( criteria.getEnrollmentEnrolledBefore() )
            .setProgramIncidentStartDate( criteria.getEnrollmentOccurredAfter() )
            .setProgramIncidentEndDate( criteria.getEnrollmentOccurredBefore() )
            .setTrackedEntityType( trackedEntityType )
            .addOrganisationUnits( orgUnits )
            .setOrganisationUnitMode( criteria.getOuMode() )
            .setEventStatus( criteria.getEventStatus() )
            .setEventStartDate( criteria.getEventOccurredAfter() )
            .setEventEndDate( criteria.getEventOccurredBefore() )
            .setUserWithAssignedUsers( criteria.getAssignedUserMode(), user, assignedUserIds )
            .setTrackedEntityInstanceUids( trackedEntities )
            .setAttributes( attributeItems )
            .setFilters( filters )
            .setSkipMeta( criteria.isSkipMeta() )
            .setPage( criteria.getPage() )
            .setPageSize( criteria.getPageSize() )
            .setTotalPages( criteria.isTotalPages() )
            .setSkipPaging( toBooleanDefaultIfNull( criteria.isSkipPaging(), false ) )
            .setIncludeDeleted( criteria.isIncludeDeleted() )
            .setIncludeAllAttributes( criteria.isIncludeAllAttributes() )
            .setPotentialDuplicate( criteria.getPotentialDuplicate() )
            .setOrders( orderParams );

        return params;
    }

    private void validateDuplicatedAttributeFilters( List<QueryItem> attributeItems )
        throws BadRequestException
    {
        Set<DimensionalItemObject> duplicatedAttributes = getDuplicatedAttributes( attributeItems );

        if ( !duplicatedAttributes.isEmpty() )
        {
            List<String> errorMessages = new ArrayList<>();
            for ( DimensionalItemObject duplicatedAttribute : duplicatedAttributes )
            {
                List<String> duplicateDFilters = getDuplicateDFilters( attributeItems, duplicatedAttribute );
                String message = MessageFormat.format( "Filter for attribute {0} was specified more than once. " +
                    "Try to define a single filter with multiple operators [{0}:{1}]",
                    duplicatedAttribute.getUid(), StringUtils.join( duplicateDFilters, ':' ) );
                errorMessages.add( message );
            }

            throw new BadRequestException( StringUtils.join( errorMessages, ", " ) );
        }
    }

    private List<String> getDuplicateDFilters( List<QueryItem> attributeItems,
        DimensionalItemObject duplicatedAttribute )
    {
        return attributeItems.stream()
            .filter( q -> Objects.equals( q.getItem(), duplicatedAttribute ) )
            .flatMap( q -> q.getFilters().stream() )
            .map( f -> f.getOperator() + ":" + f.getFilter() )
            .collect( Collectors.toList() );
    }

    private Set<DimensionalItemObject> getDuplicatedAttributes( List<QueryItem> attributeItems )
    {
        return attributeItems.stream()
            .collect( Collectors.groupingBy( QueryItem::getItem, Collectors.counting() ) )
            .entrySet().stream()
            .filter( m -> m.getValue() > 1 )
            .map( Map.Entry::getKey )
            .collect( Collectors.toSet() );
    }

    private Set<OrganisationUnit> validateOrgUnits( Set<String> orgUnitIds, User user )
        throws BadRequestException,
        ForbiddenException
    {

        Set<OrganisationUnit> orgUnits = new HashSet<>();
        for ( String orgUnitId : orgUnitIds )
        {
            OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit( orgUnitId );

            if ( orgUnit == null )
            {
                throw new BadRequestException( "Organisation unit does not exist: " + orgUnitId );
            }

            if ( user != null && !user.isSuper()
                && !organisationUnitService.isInUserHierarchy( orgUnit.getUid(),
                    user.getTeiSearchOrganisationUnitsWithFallback() ) )
            {
                throw new ForbiddenException( "Organisation unit is not part of the search scope: " + orgUnitId );
            }
            orgUnits.add( orgUnit );
        }

        return orgUnits;
    }

    /**
     * Creates a QueryFilter from the given query string. Query is on format
     * {operator}:{filter-value}. Only the filter-value is mandatory. The EQ
     * QueryOperator is used as operator if not specified.
     */
    private QueryFilter getQueryFilter( String query )
        throws BadRequestException
    {
        if ( query == null || query.isEmpty() )
        {
            return null;
        }

        if ( !query.contains( DimensionalObject.DIMENSION_NAME_SEP ) )
        {
            return new QueryFilter( QueryOperator.EQ, query );
        }
        else
        {
            String[] split = query.split( DimensionalObject.DIMENSION_NAME_SEP );

            if ( split.length != 2 )
            {
                throw new BadRequestException( "Query has invalid format: " + query );
            }

            QueryOperator op = QueryOperator.fromString( split[0] );

            return new QueryFilter( op, split[1] );
        }
    }

    private static void validateProgram( String id, Program program )
        throws BadRequestException
    {
        if ( isNotEmpty( id ) && program == null )
        {
            throw new BadRequestException( "Program is specified but does not exist: " + id );
        }
    }

    private ProgramStage validateProgramStage( TrackerTrackedEntityCriteria criteria, Program program )
        throws BadRequestException
    {

        final String programStage = criteria.getProgramStage();

        ProgramStage ps = programStage != null ? getProgramStageFromProgram( program, programStage ) : null;

        if ( programStage != null && ps == null )
        {
            throw new BadRequestException( "Program does not contain the specified programStage: " + programStage );
        }
        return ps;
    }

    private ProgramStage getProgramStageFromProgram( Program program, String programStage )
    {
        if ( program == null )
        {
            return null;
        }

        return program.getProgramStages().stream().filter( ps -> ps.getUid().equals( programStage ) ).findFirst()
            .orElse( null );
    }

    private void validateTrackedEntityType( String id, TrackedEntityType trackedEntityType )
        throws BadRequestException
    {
        if ( isNotEmpty( id ) && trackedEntityType == null )
        {
            throw new BadRequestException( "Tracked entity type does not exist: " + id );
        }
    }

    private void validateOrderParams( List<OrderParam> orderParams, Map<String, TrackedEntityAttribute> attributes )
        throws BadRequestException
    {
        if ( orderParams != null && !orderParams.isEmpty() )
        {
            for ( OrderParam orderParam : orderParams )
            {
                if ( findColumn( orderParam.getField() ).isEmpty() && !attributes.containsKey( orderParam.getField() ) )
                {
                    throw new BadRequestException( "Invalid order property: " + orderParam.getField() );
                }
            }
        }
    }
}
