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
package org.hisp.dhis.webapi.controller.tracker.export.event;

import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamUtils.applyIfNonEmpty;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamUtils.parseAndFilterUids;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamUtils.parseAttributeQueryItems;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamUtils.parseQueryItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.tracker.export.event.EventSearchParams;
import org.hisp.dhis.tracker.export.event.JdbcEventStore;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParamsHelper;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.springframework.stereotype.Component;

/**
 * Maps query parameters from {@link EventsExportController} stored in
 * {@link EventCriteria} to {@link EventSearchParams} which is used to fetch
 * events from the DB.
 */
@Component( "org.hisp.dhis.webapi.controller.tracker.export.EventCriteriaMapper" )
@RequiredArgsConstructor
class EventCriteriaMapper
{
    private static final Set<String> SORTABLE_PROPERTIES = JdbcEventStore.QUERY_PARAM_COL_MAP.keySet();

    private final CurrentUserService currentUserService;

    private final ProgramService programService;

    private final OrganisationUnitService organisationUnitService;

    private final ProgramStageService programStageService;

    private final AclService aclService;

    private final TrackedEntityInstanceService entityInstanceService;

    private final TrackedEntityAttributeService attributeService;

    private final DataElementService dataElementService;

    private final TrackedEntityAttributeService trackedEntityAttributeService;

    private final CategoryOptionComboService categoryOptionComboService;

    public EventSearchParams map( EventCriteria criteria )
        throws BadRequestException,
        ForbiddenException
    {
        Program program = applyIfNonEmpty( programService::getProgram, criteria.getProgram() );
        validateProgram( criteria.getProgram(), program );

        ProgramStage programStage = applyIfNonEmpty( programStageService::getProgramStage,
            criteria.getProgramStage() );
        validateProgramStage( criteria.getProgramStage(), programStage );

        OrganisationUnit orgUnit = applyIfNonEmpty( organisationUnitService::getOrganisationUnit,
            criteria.getOrgUnit() );
        validateOrgUnit( criteria.getOrgUnit(), orgUnit );

        User user = currentUserService.getCurrentUser();
        validateUser( user, program, programStage );

        TrackedEntity trackedEntity = applyIfNonEmpty( entityInstanceService::getTrackedEntityInstance,
            criteria.getTrackedEntity() );
        validateTrackedEntity( criteria.getTrackedEntity(), trackedEntity );

        CategoryOptionCombo attributeOptionCombo = categoryOptionComboService.getAttributeOptionCombo(
            criteria.getAttributeCc(),
            criteria.getAttributeCos(),
            true );
        validateAttributeOptionCombo( attributeOptionCombo, user );

        Set<String> eventIds = parseAndFilterUids( criteria.getEvent() );
        validateFilter( criteria.getFilter(), eventIds, criteria.getProgramStage(), programStage );

        Set<String> assignedUserIds = parseAndFilterUids( criteria.getAssignedUser() );

        Map<String, SortDirection> dataElementOrders = getDataElementsFromOrder( criteria.getOrder() );

        List<QueryItem> dataElements = new ArrayList<>();
        for ( String order : dataElementOrders.keySet() )
        {
            dataElements.add( parseQueryItem( order, this::dataElementToQueryItem ) );
        }

        Map<String, SortDirection> attributeOrders = getAttributesFromOrder( criteria.getOrder() );
        List<OrderParam> attributeOrderParams = mapToOrderParams( attributeOrders );
        List<OrderParam> dataElementOrderParams = mapToOrderParams( dataElementOrders );

        List<QueryItem> filterAttributes = parseFilterAttributes( criteria.getFilterAttributes(),
            attributeOrderParams );
        validateFilterAttributes( filterAttributes );

        List<QueryItem> filters = new ArrayList<>();
        for ( String eventCriteria : criteria.getFilter() )
        {
            filters.add( parseQueryItem( eventCriteria, this::dataElementToQueryItem ) );
        }

        Set<String> enrollments = criteria.getEnrollments().stream()
            .filter( CodeGenerator::isValidUid )
            .collect( Collectors.toSet() );

        EventSearchParams params = new EventSearchParams();

        return params.setProgram( program ).setProgramStage( programStage ).setOrgUnit( orgUnit )
            .setTrackedEntity( trackedEntity )
            .setProgramStatus( criteria.getProgramStatus() ).setFollowUp( criteria.getFollowUp() )
            .setOrgUnitSelectionMode( criteria.getOuMode() )
            .setUserWithAssignedUsers( criteria.getAssignedUserMode(), user, assignedUserIds )
            .setStartDate( criteria.getOccurredAfter() ).setEndDate( criteria.getOccurredBefore() )
            .setScheduleAtStartDate( criteria.getScheduledAfter() )
            .setScheduleAtEndDate( criteria.getScheduledBefore() )
            .setUpdatedAtStartDate( criteria.getUpdatedAfter() )
            .setUpdatedAtEndDate( criteria.getUpdatedBefore() )
            .setUpdatedAtDuration( criteria.getUpdatedWithin() )
            .setEnrollmentEnrolledBefore( criteria.getEnrollmentEnrolledBefore() )
            .setEnrollmentEnrolledAfter( criteria.getEnrollmentEnrolledAfter() )
            .setEnrollmentOccurredBefore( criteria.getEnrollmentOccurredBefore() )
            .setEnrollmentOccurredAfter( criteria.getEnrollmentOccurredAfter() )
            .setEventStatus( criteria.getStatus() )
            .setCategoryOptionCombo( attributeOptionCombo ).setIdSchemes( criteria.getIdSchemes() )
            .setPage( criteria.getPage() )
            .setPageSize( criteria.getPageSize() ).setTotalPages( criteria.isTotalPages() )
            .setSkipPaging( toBooleanDefaultIfNull( criteria.isSkipPaging(), false ) )
            .setSkipEventId( criteria.getSkipEventId() ).setIncludeAttributes( false )
            .setIncludeAllDataElements( false ).addDataElements( dataElements )
            .addFilters( filters ).addFilterAttributes( filterAttributes )
            .addOrders( getOrderParams( criteria.getOrder() ) )
            .addGridOrders( dataElementOrderParams )
            .addAttributeOrders( attributeOrderParams )
            .setEvents( eventIds ).setEnrollments( enrollments )
            .setIncludeDeleted( criteria.isIncludeDeleted() );
    }

    private static void validateProgram( String program, Program pr )
        throws BadRequestException
    {
        if ( !StringUtils.isEmpty( program ) && pr == null )
        {
            throw new BadRequestException( "Program is specified but does not exist: " + program );
        }
    }

    private static void validateProgramStage( String programStage, ProgramStage ps )
        throws BadRequestException
    {
        if ( !StringUtils.isEmpty( programStage ) && ps == null )
        {
            throw new BadRequestException( "Program stage is specified but does not exist: " + programStage );
        }
    }

    private static void validateOrgUnit( String orgUnit, OrganisationUnit ou )
        throws BadRequestException
    {
        if ( !StringUtils.isEmpty( orgUnit ) && ou == null )
        {
            throw new BadRequestException( "Org unit is specified but does not exist: " + orgUnit );
        }
    }

    private void validateUser( User user, Program pr, ProgramStage ps )
        throws ForbiddenException
    {
        if ( pr != null && !user.isSuper() && !aclService.canDataRead( user, pr ) )
        {
            throw new ForbiddenException( "User has no access to program: " + pr.getUid() );
        }

        if ( ps != null && !user.isSuper() && !aclService.canDataRead( user, ps ) )
        {
            throw new ForbiddenException( "User has no access to program stage: " + ps.getUid() );
        }
    }

    private void validateTrackedEntity( String trackedEntity, TrackedEntity trackedEntityInstance )
        throws BadRequestException
    {
        if ( !StringUtils.isEmpty( trackedEntity ) && trackedEntityInstance == null )
        {
            throw new BadRequestException(
                "Tracked entity instance is specified but does not exist: " + trackedEntity );
        }
    }

    private void validateAttributeOptionCombo( CategoryOptionCombo attributeOptionCombo, User user )
        throws ForbiddenException
    {
        if ( attributeOptionCombo != null && !user.isSuper()
            && !aclService.canDataRead( user, attributeOptionCombo ) )
        {
            throw new ForbiddenException(
                "User has no access to attribute category option combo: " + attributeOptionCombo.getUid() );
        }
    }

    private static void validateFilter( Set<String> filters, Set<String> eventIds, String programStage,
        ProgramStage ps )
        throws BadRequestException
    {
        if ( !CollectionUtils.isEmpty( eventIds ) && !CollectionUtils.isEmpty( filters ) )
        {
            throw new BadRequestException( "Event UIDs and filters can not be specified at the same time" );
        }
        if ( !CollectionUtils.isEmpty( filters ) && !StringUtils.isEmpty( programStage ) && ps == null )
        {
            throw new BadRequestException( "ProgramStage needs to be specified for event filtering to work" );
        }
    }

    private List<QueryItem> parseFilterAttributes( Set<String> filterAttributes, List<OrderParam> attributeOrderParams )
        throws BadRequestException
    {
        Map<String, TrackedEntityAttribute> attributes = attributeService.getAllTrackedEntityAttributes()
            .stream()
            .collect( Collectors.toMap( TrackedEntityAttribute::getUid, att -> att ) );

        List<QueryItem> filterItems = parseAttributeQueryItems( filterAttributes, attributes );
        List<QueryItem> orderItems = attributeQueryItemsFromOrder( filterItems, attributes, attributeOrderParams );

        return Stream.concat( filterItems.stream(), orderItems.stream() ).toList();
    }

    private List<QueryItem> attributeQueryItemsFromOrder( List<QueryItem> filterAttributes,
        Map<String, TrackedEntityAttribute> attributes, List<OrderParam> attributeOrderParams )
    {
        return attributeOrderParams.stream()
            .map( OrderParam::getField )
            .filter( att -> !containsAttributeFilter( filterAttributes, att ) )
            .map( attributes::get )
            .map( at -> new QueryItem( at, null, at.getValueType(), at.getAggregationType(), at.getOptionSet() ) )
            .toList();
    }

    private boolean containsAttributeFilter( List<QueryItem> attributeFilters, String attributeUid )
    {
        for ( QueryItem item : attributeFilters )
        {
            if ( Objects.equals( item.getItem().getUid(), attributeUid ) )
            {
                return true;
            }
        }
        return false;
    }

    private void validateFilterAttributes( List<QueryItem> queryItems )
        throws BadRequestException
    {
        Set<String> attributes = new HashSet<>();
        Set<String> duplicates = new HashSet<>();
        for ( QueryItem item : queryItems )
        {
            if ( !attributes.add( item.getItemId() ) )
            {
                duplicates.add( item.getItemId() );
            }
        }

        if ( !duplicates.isEmpty() )
        {
            throw new BadRequestException( String.format(
                "filterAttributes contains duplicate tracked entity attribute (TEA): %s. Multiple filters for the same TEA can be specified like 'uid:gt:2:lt:10'",
                String.join( ", ", duplicates ) ) );
        }
    }

    private Map<String, SortDirection> getDataElementsFromOrder( List<OrderCriteria> allOrders )
    {
        if ( allOrders == null )
        {
            return Collections.emptyMap();
        }

        Map<String, SortDirection> dataElements = new HashMap<>();
        for ( OrderCriteria orderCriteria : allOrders )
        {
            DataElement de = dataElementService.getDataElement( orderCriteria.getField() );
            if ( de != null )
            {
                dataElements.put( orderCriteria.getField(), orderCriteria.getDirection() );
            }
        }
        return dataElements;
    }

    private Map<String, SortDirection> getAttributesFromOrder( List<OrderCriteria> allOrders )
    {
        if ( allOrders == null )
        {
            return Collections.emptyMap();
        }

        Map<String, SortDirection> attributes = new HashMap<>();
        for ( OrderCriteria orderCriteria : allOrders )
        {
            TrackedEntityAttribute attribute = trackedEntityAttributeService
                .getTrackedEntityAttribute( orderCriteria.getField() );
            if ( attribute != null )
            {
                attributes.put( orderCriteria.getField(), orderCriteria.getDirection() );
            }
        }
        return attributes;
    }

    private QueryItem dataElementToQueryItem( String item )
        throws BadRequestException
    {
        DataElement de = dataElementService.getDataElement( item );

        if ( de == null )
        {
            throw new BadRequestException( "Data element does not exist: " + item );
        }

        return new QueryItem( de, null, de.getValueType(), de.getAggregationType(), de.getOptionSet() );
    }

    private List<OrderParam> getOrderParams( List<OrderCriteria> order )
        throws BadRequestException
    {
        if ( order == null || order.isEmpty() )
        {
            return Collections.emptyList();
        }
        validateOrderParams( order );

        return OrderParamsHelper.toOrderParams( order );
    }

    private void validateOrderParams( List<OrderCriteria> order )
        throws BadRequestException
    {
        Set<String> requestProperties = order.stream()
            .map( OrderCriteria::getField )
            .filter( field -> !CodeGenerator.isValidUid( field ) )
            .collect( Collectors.toSet() );

        requestProperties.removeAll( SORTABLE_PROPERTIES );
        if ( !requestProperties.isEmpty() )
        {
            throw new BadRequestException(
                String.format( "Order by property `%s` is not supported. Supported are `%s`",
                    String.join( ", ", requestProperties ), String.join( ", ", SORTABLE_PROPERTIES ) ) );
        }
    }

    private List<OrderParam> mapToOrderParams( Map<String, SortDirection> orders )
    {
        return orders.entrySet().stream()
            .map( e -> new OrderParam( e.getKey(), e.getValue() ) )
            .toList();
    }
}
