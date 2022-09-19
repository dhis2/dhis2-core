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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.OrderColumn.isStaticColumn;
import static org.hisp.dhis.webapi.controller.event.mapper.OrderParamsHelper.toOrderParams;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
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
import org.hisp.dhis.webapi.controller.event.webrequest.TrackedEntityInstanceCriteria;
import org.mapstruct.factory.Mappers;
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
public class TrackedEntityCriteriaMapper
{
    private final CurrentUserService currentUserService;

    private final OrganisationUnitService organisationUnitService;

    private final ProgramService programService;

    private final TrackedEntityTypeService trackedEntityTypeService;

    private final TrackedEntityAttributeService attributeService;

    private static final TrackerTrackedEntityCriteriaMapper TRACKER_TRACKED_ENTITY_CRITERIA_MAPPER = Mappers
        .getMapper( TrackerTrackedEntityCriteriaMapper.class );

    public TrackedEntityCriteriaMapper( CurrentUserService currentUserService,
        OrganisationUnitService organisationUnitService, ProgramService programService,
        TrackedEntityAttributeService attributeService, TrackedEntityTypeService trackedEntityTypeService )
    {
        checkNotNull( currentUserService );
        checkNotNull( organisationUnitService );
        checkNotNull( programService );
        checkNotNull( attributeService );
        checkNotNull( trackedEntityTypeService );

        this.currentUserService = currentUserService;
        this.organisationUnitService = organisationUnitService;
        this.programService = programService;
        this.attributeService = attributeService;
        this.trackedEntityTypeService = trackedEntityTypeService;
    }

    @Transactional( readOnly = true )
    public TrackedEntityInstanceQueryParams map( TrackerTrackedEntityCriteria criteria )
    {
        return map( TRACKER_TRACKED_ENTITY_CRITERIA_MAPPER.toTrackedEntityInstanceCriteria( criteria ) );
    }

    @Transactional( readOnly = true )
    public TrackedEntityInstanceQueryParams map( TrackedEntityInstanceCriteria criteria )
    {
        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();

        final Date programEnrollmentStartDate = ObjectUtils.firstNonNull( criteria.getProgramEnrollmentStartDate(),
            criteria.getProgramStartDate() );

        final Date programEnrollmentEndDate = ObjectUtils.firstNonNull( criteria.getProgramEnrollmentEndDate(),
            criteria.getProgramEndDate() );

        Set<OrganisationUnit> possibleSearchOrgUnits = new HashSet<>();

        User user = currentUserService.getCurrentUser();

        if ( user != null )
        {
            possibleSearchOrgUnits = user.getTeiSearchOrganisationUnitsWithFallback();
        }

        QueryFilter queryFilter = getQueryFilter( criteria.getQuery() );

        Map<String, TrackedEntityAttribute> attributes = attributeService.getAllTrackedEntityAttributes()
            .stream().collect( Collectors.toMap( TrackedEntityAttribute::getUid, att -> att ) );

        if ( criteria.getAttribute() != null )
        {
            for ( String attr : criteria.getAttribute() )
            {
                QueryItem it = getQueryItem( attr, attributes );

                params.getAttributes().add( it );
            }
        }

        if ( criteria.getFilter() != null )
        {
            for ( String filt : criteria.getFilter() )
            {
                QueryItem it = getQueryItem( filt, attributes );

                params.getFilters().add( it );
            }
        }

        for ( String orgUnit : criteria.getOrgUnits() )
        {
            OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( orgUnit );

            if ( organisationUnit == null )
            {
                throw new IllegalQueryException( "Organisation unit does not exist: " + orgUnit );
            }

            if ( user != null && !user.isSuper()
                && !organisationUnitService.isInUserHierarchy( organisationUnit.getUid(), possibleSearchOrgUnits ) )
            {
                throw new IllegalQueryException( "Organisation unit is not part of the search scope: " + orgUnit );
            }

            params.getOrganisationUnits().add( organisationUnit );
        }

        validateAssignedUser( criteria );

        if ( criteria.getOuMode() == OrganisationUnitSelectionMode.CAPTURE && user != null )
        {
            params.getOrganisationUnits().addAll( user.getOrganisationUnits() );
        }

        Program program = validateProgram( criteria );

        List<OrderParam> orderParams = toOrderParams( criteria.getOrder() );

        validateOrderParams( program, orderParams, attributes );

        params.setQuery( queryFilter )
            .setProgram( program )
            .setProgramStage( validateProgramStage( criteria, program ) )
            .setProgramStatus( criteria.getProgramStatus() )
            .setFollowUp( criteria.getFollowUp() )
            .setLastUpdatedStartDate( criteria.getLastUpdatedStartDate() )
            .setLastUpdatedEndDate( criteria.getLastUpdatedEndDate() )
            .setLastUpdatedDuration( criteria.getLastUpdatedDuration() )
            .setProgramEnrollmentStartDate( programEnrollmentStartDate )
            .setProgramEnrollmentEndDate( programEnrollmentEndDate )
            .setProgramIncidentStartDate( criteria.getProgramIncidentStartDate() )
            .setProgramIncidentEndDate( criteria.getProgramIncidentEndDate() )
            .setTrackedEntityType( validateTrackedEntityType( criteria ) )
            .setOrganisationUnitMode( criteria.getOuMode() )
            .setEventStatus( criteria.getEventStatus() )
            .setEventStartDate( criteria.getEventStartDate() )
            .setEventEndDate( criteria.getEventEndDate() )
            .setAssignedUserSelectionMode( criteria.getAssignedUserMode() )
            .setAssignedUsers( criteria.getAssignedUsers() )
            .setTrackedEntityInstanceUids( criteria.getTrackedEntityInstances() )
            .setSkipMeta( criteria.isSkipMeta() )
            .setPage( criteria.getPage() )
            .setPageSize( criteria.getPageSize() )
            .setTotalPages( criteria.isTotalPages() )
            .setSkipPaging( criteria.isSkipPaging() )
            .setIncludeDeleted( criteria.isIncludeDeleted() )
            .setIncludeAllAttributes( criteria.isIncludeAllAttributes() )
            .setUser( user )
            .setOrders( orderParams );

        return params;

    }

    /**
     * Creates a QueryFilter from the given query string. Query is on format
     * {operator}:{filter-value}. Only the filter-value is mandatory. The EQ
     * QueryOperator is used as operator if not specified.
     */
    private QueryFilter getQueryFilter( String query )
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
                throw new IllegalQueryException( "Query has invalid format: " + query );
            }

            QueryOperator op = QueryOperator.fromString( split[0] );

            return new QueryFilter( op, split[1] );
        }
    }

    /**
     * Creates a QueryItem from the given item string. Item is on format
     * {attribute-id}:{operator}:{filter-value}[:{operator}:{filter-value}].
     * Only the attribute-id is mandatory.
     */
    private QueryItem getQueryItem( String item, Map<String, TrackedEntityAttribute> attributes )
    {
        String[] split = item.split( DimensionalObject.DIMENSION_NAME_SEP );

        if ( split.length % 2 != 1 )
        {
            throw new IllegalQueryException( "Query item or filter is invalid: " + item );
        }

        QueryItem queryItem = getItem( split[0], attributes );

        if ( split.length > 1 ) // Filters specified
        {
            for ( int i = 1; i < split.length; i += 2 )
            {
                QueryOperator operator = QueryOperator.fromString( split[i] );
                queryItem.getFilters().add( new QueryFilter( operator, split[i + 1] ) );
            }
        }

        return queryItem;
    }

    private QueryItem getItem( String item, Map<String, TrackedEntityAttribute> attributes )
    {
        if ( attributes.isEmpty() )
        {
            throw new IllegalQueryException( "Attribute does not exist: " + item );
        }

        TrackedEntityAttribute at = attributes.get( item );

        if ( at == null )
        {
            throw new IllegalQueryException( "Attribute does not exist: " + item );
        }

        return new QueryItem( at, null, at.getValueType(), at.getAggregationType(), at.getOptionSet(), at.isUnique() );
    }

    private Program validateProgram( TrackedEntityInstanceCriteria criteria )
    {
        Function<String, Program> getProgram = uid -> {
            if ( isNotEmpty( uid ) )
            {
                return programService.getProgram( uid );
            }
            return null;
        };

        final Program program = getProgram.apply( criteria.getProgram() );
        if ( isNotEmpty( criteria.getProgram() ) && program == null )
        {
            throw new IllegalQueryException( "Program does not exist: " + criteria.getProgram() );
        }
        return program;
    }

    private ProgramStage validateProgramStage( TrackedEntityInstanceCriteria criteria, Program program )
    {

        final String programStage = criteria.getProgramStage();

        ProgramStage ps = programStage != null ? getProgramStageFromProgram( program, programStage ) : null;

        if ( programStage != null && ps == null )
        {
            throw new IllegalQueryException( "Program does not contain the specified programStage: " + programStage );
        }
        return ps;
    }

    private TrackedEntityType validateTrackedEntityType( TrackedEntityInstanceCriteria criteria )
    {
        Function<String, TrackedEntityType> getTeiType = uid -> {
            if ( isNotEmpty( uid ) )
            {
                return trackedEntityTypeService.getTrackedEntityType( uid );
            }
            return null;
        };

        final TrackedEntityType trackedEntityType = getTeiType.apply( criteria.getTrackedEntityType() );

        if ( isNotEmpty( criteria.getTrackedEntityType() ) && trackedEntityType == null )
        {
            throw new IllegalQueryException( "Tracked entity type does not exist: " + criteria.getTrackedEntityType() );
        }
        return trackedEntityType;
    }

    private void validateAssignedUser( TrackedEntityInstanceCriteria criteria )
    {
        if ( criteria.getAssignedUserMode() != null && !criteria.getAssignedUsers().isEmpty()
            && !criteria.getAssignedUserMode().equals( AssignedUserSelectionMode.PROVIDED ) )
        {
            throw new IllegalQueryException(
                "Assigned User uid(s) cannot be specified if selectionMode is not PROVIDED" );
        }
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

    private void validateOrderParams( Program program, List<OrderParam> orderParams,
        Map<String, TrackedEntityAttribute> attributes )
    {
        if ( orderParams != null && !orderParams.isEmpty() )
        {
            for ( OrderParam orderParam : orderParams )
            {
                if ( !isStaticColumn( orderParam.getField() ) && !attributes.containsKey( orderParam.getField() ) )
                {
                    throw new IllegalQueryException( "Invalid order property: " + orderParam.getField() );
                }
            }
        }
    }
}
