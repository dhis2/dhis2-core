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
package org.hisp.dhis.webapi.controller.tracker.export.trackedentity;

import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.hisp.dhis.trackedentity.TrackedEntityQueryParams.OrderColumn.findColumn;
import static org.hisp.dhis.webapi.controller.event.mapper.OrderParamsHelper.toOrderParams;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamUtils.applyIfNonEmpty;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamUtils.parseAttributeQueryItems;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamUtils.parseQueryFilter;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamUtils.validateDeprecatedUidsParameter;

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
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.common.UID;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maps query parameters from {@link TrackedEntitiesExportController} stored in
 * {@link RequestParams} to {@link TrackedEntityQueryParams} which is used to
 * fetch tracked entities from the DB.
 *
 * @author Luciano Fiandesio
 */
@Component
@RequiredArgsConstructor
public class TrackedEntityParamsMapper
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

    @Nonnull
    private final TrackerAccessManager trackerAccessManager;

    @Transactional( readOnly = true )
    public TrackedEntityQueryParams map( RequestParams requestParams )
        throws BadRequestException,
        ForbiddenException
    {
        Program program = applyIfNonEmpty( programService::getProgram, requestParams.getProgram() );
        validateProgram( requestParams.getProgram(), program );
        ProgramStage programStage = validateProgramStage( requestParams, program );

        TrackedEntityType trackedEntityType = applyIfNonEmpty( trackedEntityTypeService::getTrackedEntityType,
            requestParams.getTrackedEntityType() );
        validateTrackedEntityType( requestParams.getTrackedEntityType(), trackedEntityType );

        Set<UID> assignedUsers = validateDeprecatedUidsParameter( "assignedUser", requestParams.getAssignedUser(),
            "assignedUsers",
            requestParams.getAssignedUsers() );

        User user = currentUserService.getCurrentUser();
        Set<UID> orgUnitUids = validateDeprecatedUidsParameter( "orgUnit", requestParams.getOrgUnit(), "orgUnits",
            requestParams.getOrgUnits() );
        Set<OrganisationUnit> orgUnits = validateOrgUnits( user, orgUnitUids, program );
        if ( requestParams.getOuMode() == OrganisationUnitSelectionMode.CAPTURE && user != null )
        {
            orgUnits.addAll( user.getOrganisationUnits() );
        }

        QueryFilter queryFilter = parseQueryFilter( requestParams.getQuery() );

        Map<String, TrackedEntityAttribute> attributes = attributeService.getAllTrackedEntityAttributes()
            .stream().collect( Collectors.toMap( TrackedEntityAttribute::getUid, att -> att ) );

        List<QueryItem> attributeItems = parseAttributeQueryItems( requestParams.getAttribute(), attributes );

        List<QueryItem> filters = parseAttributeQueryItems( requestParams.getFilter(), attributes );

        validateDuplicatedAttributeFilters( filters );

        List<OrderParam> orderParams = toOrderParams( requestParams.getOrder() );
        validateOrderParams( orderParams, attributes );

        Set<UID> trackedEntities = validateDeprecatedUidsParameter( "trackedEntity", requestParams.getTrackedEntity(),
            "trackedEntities",
            requestParams.getTrackedEntities() );

        TrackedEntityQueryParams params = new TrackedEntityQueryParams();
        params.setQuery( queryFilter )
            .setProgram( program )
            .setProgramStage( programStage )
            .setProgramStatus( requestParams.getProgramStatus() )
            .setFollowUp( requestParams.getFollowUp() )
            .setLastUpdatedStartDate( requestParams.getUpdatedAfter() )
            .setLastUpdatedEndDate( requestParams.getUpdatedBefore() )
            .setLastUpdatedDuration( requestParams.getUpdatedWithin() )
            .setProgramEnrollmentStartDate( requestParams.getEnrollmentEnrolledAfter() )
            .setProgramEnrollmentEndDate( requestParams.getEnrollmentEnrolledBefore() )
            .setProgramIncidentStartDate( requestParams.getEnrollmentOccurredAfter() )
            .setProgramIncidentEndDate( requestParams.getEnrollmentOccurredBefore() )
            .setTrackedEntityType( trackedEntityType )
            .addOrganisationUnits( orgUnits )
            .setOrganisationUnitMode( requestParams.getOuMode() )
            .setEventStatus( requestParams.getEventStatus() )
            .setEventStartDate( requestParams.getEventOccurredAfter() )
            .setEventEndDate( requestParams.getEventOccurredBefore() )
            .setUserWithAssignedUsers( requestParams.getAssignedUserMode(), user,
                assignedUsers.stream().map( UID::getValue ).collect(
                    Collectors.toSet() ) )
            .setTrackedEntityUids( trackedEntities.stream().map( UID::getValue ).collect( Collectors.toSet() ) )
            .setAttributes( attributeItems )
            .setFilters( filters )
            .setSkipMeta( requestParams.isSkipMeta() )
            .setPage( requestParams.getPage() )
            .setPageSize( requestParams.getPageSize() )
            .setTotalPages( requestParams.isTotalPages() )
            .setSkipPaging( toBooleanDefaultIfNull( requestParams.isSkipPaging(), false ) )
            .setIncludeDeleted( requestParams.isIncludeDeleted() )
            .setIncludeAllAttributes( requestParams.isIncludeAllAttributes() )
            .setPotentialDuplicate( requestParams.getPotentialDuplicate() )
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
                List<String> duplicatedFilters = getDuplicatedFilters( attributeItems, duplicatedAttribute );
                String message = MessageFormat.format( "Filter for attribute {0} was specified more than once. " +
                    "Try to define a single filter with multiple operators [{0}:{1}]",
                    duplicatedAttribute.getUid(), StringUtils.join( duplicatedFilters, ':' ) );
                errorMessages.add( message );
            }

            throw new BadRequestException( StringUtils.join( errorMessages, ", " ) );
        }
    }

    private List<String> getDuplicatedFilters( List<QueryItem> attributeItems,
        DimensionalItemObject duplicatedAttribute )
    {
        return attributeItems.stream()
            .filter( q -> Objects.equals( q.getItem(), duplicatedAttribute ) )
            .flatMap( q -> q.getFilters().stream() )
            .map( f -> f.getOperator() + ":" + f.getFilter() )
            .toList();
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

    private Set<OrganisationUnit> validateOrgUnits( User user, Set<UID> orgUnitIds, Program program )
        throws BadRequestException,
        ForbiddenException
    {
        Set<OrganisationUnit> orgUnits = new HashSet<>();
        for ( UID orgUnitUid : orgUnitIds )
        {
            OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit( orgUnitUid.getValue() );

            if ( orgUnit == null )
            {
                throw new BadRequestException( "Organisation unit does not exist: " + orgUnitUid.getValue() );
            }

            if ( !trackerAccessManager.canAccess( user, program, orgUnit ) )
            {
                throw new ForbiddenException(
                    "User does not have access to organisation unit: " + orgUnitUid.getValue() );
            }

            orgUnits.add( orgUnit );
        }

        return orgUnits;
    }

    private static void validateProgram( String id, Program program )
        throws BadRequestException
    {
        if ( isNotEmpty( id ) && program == null )
        {
            throw new BadRequestException( "Program is specified but does not exist: " + id );
        }
    }

    private ProgramStage validateProgramStage( RequestParams requestParams, Program program )
        throws BadRequestException
    {

        final String programStage = requestParams.getProgramStage();

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
