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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventSearchParams;
import org.hisp.dhis.dxf2.util.InputUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam.SortDirection;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParamsHelper;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.springframework.stereotype.Component;

/**
 * Maps query parameters from {@link TrackerEventsExportController} stored in
 * {@link TrackerEventCriteria} to {@link EventSearchParams} which is used to
 * fetch events from the DB.
 */
@Component( "org.hisp.dhis.webapi.controller.tracker.export.EventRequestToSearchParamsMapper" )
@RequiredArgsConstructor
class EventRequestToSearchParamsMapper
{

    /**
     * Properties other than the {@link Property#isSimple()} ones on
     * {@link org.hisp.dhis.dxf2.events.event.Event} which are valid order query
     * parameter property names. These need to be supported by the underlying
     * Event store like {@link org.hisp.dhis.dxf2.events.event.JdbcEventStore}
     * see QUERY_PARAM_COL_MAP.
     */
    private static final Set<String> NON_EVENT_SORTABLE_PROPERTIES = Set.of( "enrolledAt", "occurredAt" );

    private final CurrentUserService currentUserService;

    private final ProgramService programService;

    private final OrganisationUnitService organisationUnitService;

    private final ProgramStageService programStageService;

    private final AclService aclService;

    private final TrackedEntityInstanceService entityInstanceService;

    private final TrackedEntityAttributeService attributeService;

    private final DataElementService dataElementService;

    private final TrackedEntityAttributeService trackedEntityAttributeService;

    private final InputUtils inputUtils;

    private final SchemaService schemaService;

    private Schema schema;

    @PostConstruct
    void setSchema()
    {
        if ( schema == null )
        {
            schema = schemaService.getDynamicSchema( Event.class );
        }
    }

    public EventSearchParams map( TrackerEventCriteria eventCriteria )
    {
        Program program = applyIfNonEmpty( programService::getProgram, eventCriteria.getProgram() );
        validateProgram( eventCriteria.getProgram(), program );

        ProgramStage programStage = applyIfNonEmpty( programStageService::getProgramStage,
            eventCriteria.getProgramStage() );
        validateProgramStage( eventCriteria.getProgramStage(), programStage );

        OrganisationUnit orgUnit = applyIfNonEmpty( organisationUnitService::getOrganisationUnit,
            eventCriteria.getOrgUnit() );
        validateOrgUnit( eventCriteria.getOrgUnit(), orgUnit );

        User user = currentUserService.getCurrentUser();
        validateUser( user, program, programStage );

        TrackedEntityInstance trackedEntityInstance = applyIfNonEmpty( entityInstanceService::getTrackedEntityInstance,
            eventCriteria.getTrackedEntity() );
        validateTrackedEntity( eventCriteria.getTrackedEntity(), trackedEntityInstance );

        CategoryOptionCombo attributeOptionCombo = inputUtils.getAttributeOptionCombo(
            eventCriteria.getAttributeCc(),
            eventCriteria.getAttributeCos(),
            true );
        validateAttributeOptionCombo( attributeOptionCombo, user );

        Set<String> eventIds = parseUids( eventCriteria.getEvent() );
        validateFilter( eventCriteria.getFilter(), eventIds, eventCriteria.getProgramStage(), programStage );

        Set<String> assignedUserIds = parseUids( eventCriteria.getAssignedUser() );
        validateAssignedUsers( eventCriteria.getAssignedUserMode(), assignedUserIds );

        Map<String, SortDirection> dataElementOrders = getDataElementsFromOrder( eventCriteria.getOrder() );
        List<QueryItem> dataElements = dataElementOrders.keySet()
            .stream()
            .map( this::getQueryItem )
            .collect( Collectors.toList() );

        Map<String, SortDirection> attributeOrders = getAttributesFromOrder( eventCriteria.getOrder() );
        List<OrderParam> attributeOrderParams = mapToOrderParams( attributeOrders );
        List<OrderParam> dataElementOrderParams = mapToOrderParams( dataElementOrders );

        List<QueryItem> filterAttributes = parseFilterAttributes( eventCriteria.getFilterAttributes(),
            attributeOrderParams );
        validateFilterAttributes( filterAttributes );

        List<QueryItem> filters = eventCriteria.getFilter()
            .stream()
            .map( this::getQueryItem )
            .collect( Collectors.toList() );

        Set<String> programInstances = eventCriteria.getEnrollments().stream()
            .filter( CodeGenerator::isValidUid )
            .collect( Collectors.toSet() );

        EventSearchParams params = new EventSearchParams();

        return params.setProgram( program ).setProgramStage( programStage ).setOrgUnit( orgUnit )
            .setTrackedEntityInstance( trackedEntityInstance )
            .setProgramStatus( eventCriteria.getProgramStatus() ).setFollowUp( eventCriteria.getFollowUp() )
            .setOrgUnitSelectionMode( eventCriteria.getOuMode() )
            .setAssignedUserSelectionMode( eventCriteria.getAssignedUserMode() )
            .setAssignedUsers( assignedUserIds )
            .setStartDate( eventCriteria.getOccurredAfter() ).setEndDate( eventCriteria.getOccurredBefore() )
            .setDueDateStart( eventCriteria.getScheduledAfter() ).setDueDateEnd( eventCriteria.getScheduledBefore() )
            .setLastUpdatedStartDate( eventCriteria.getUpdatedAfter() )
            .setLastUpdatedEndDate( eventCriteria.getUpdatedBefore() )
            .setLastUpdatedDuration( eventCriteria.getUpdatedWithin() )
            .setEnrollmentEnrolledBefore( eventCriteria.getEnrollmentEnrolledBefore() )
            .setEnrollmentEnrolledAfter( eventCriteria.getEnrollmentEnrolledAfter() )
            .setEnrollmentOccurredBefore( eventCriteria.getEnrollmentOccurredBefore() )
            .setEnrollmentOccurredAfter( eventCriteria.getEnrollmentOccurredAfter() )
            .setEventStatus( eventCriteria.getStatus() )
            .setCategoryOptionCombo( attributeOptionCombo ).setIdSchemes( eventCriteria.getIdSchemes() )
            .setPage( eventCriteria.getPage() )
            .setPageSize( eventCriteria.getPageSize() ).setTotalPages( eventCriteria.isTotalPages() )
            .setSkipPaging( eventCriteria.isSkipPaging() )
            .setSkipEventId( eventCriteria.getSkipEventId() ).setIncludeAttributes( false )
            .setIncludeAllDataElements( false ).addDataElements( dataElements )
            .addFilters( filters ).addFilterAttributes( filterAttributes )
            .addOrders( getOrderParams( eventCriteria.getOrder() ) )
            .addGridOrders( dataElementOrderParams )
            .addAttributeOrders( attributeOrderParams )
            .setEvents( eventIds ).setProgramInstances( programInstances )
            .setIncludeDeleted( eventCriteria.isIncludeDeleted() );
    }

    private static <T extends BaseIdentifiableObject> T applyIfNonEmpty( Function<String, T> func, String arg )
    {
        if ( StringUtils.isEmpty( arg ) )
        {
            return null;
        }

        return func.apply( arg );
    }

    private static void validateProgram( String program, Program pr )
    {
        if ( !StringUtils.isEmpty( program ) && pr == null )
        {
            throw new IllegalQueryException( "Program is specified but does not exist: " + program );
        }
    }

    private static void validateProgramStage( String programStage, ProgramStage ps )
    {
        if ( !StringUtils.isEmpty( programStage ) && ps == null )
        {
            throw new IllegalQueryException( "Program stage is specified but does not exist: " + programStage );
        }
    }

    private static void validateOrgUnit( String orgUnit, OrganisationUnit ou )
    {
        if ( !StringUtils.isEmpty( orgUnit ) && ou == null )
        {
            throw new IllegalQueryException( "Org unit is specified but does not exist: " + orgUnit );
        }
    }

    private void validateUser( User user, Program pr, ProgramStage ps )
    {
        if ( pr != null && !user.isSuper() && !aclService.canDataRead( user, pr ) )
        {
            throw new IllegalQueryException( "User has no access to program: " + pr.getUid() );
        }

        if ( ps != null && !user.isSuper() && !aclService.canDataRead( user, ps ) )
        {
            throw new IllegalQueryException( "User has no access to program stage: " + ps.getUid() );
        }
    }

    private void validateTrackedEntity( String trackedEntity, TrackedEntityInstance trackedEntityInstance )
    {
        if ( !StringUtils.isEmpty( trackedEntity ) && trackedEntityInstance == null )
        {
            throw new IllegalQueryException(
                "Tracked entity instance is specified but does not exist: " + trackedEntity );
        }
    }

    private void validateAttributeOptionCombo( CategoryOptionCombo attributeOptionCombo, User user )
    {
        if ( attributeOptionCombo != null && !user.isSuper()
            && !aclService.canDataRead( user, attributeOptionCombo ) )
        {
            throw new IllegalQueryException(
                "User has no access to attribute category option combo: " + attributeOptionCombo.getUid() );
        }
    }

    private static void validateFilter( Set<String> filters, Set<String> eventIds, String programStage,
        ProgramStage ps )
    {
        if ( !CollectionUtils.isEmpty( eventIds ) && !CollectionUtils.isEmpty( filters ) )
        {
            throw new IllegalQueryException( "Event UIDs and filters can not be specified at the same time" );
        }
        if ( !CollectionUtils.isEmpty( filters ) && !StringUtils.isEmpty( programStage ) && ps == null )
        {
            throw new IllegalQueryException( "ProgramStage needs to be specified for event filtering to work" );
        }
    }

    private static void validateAssignedUsers( AssignedUserSelectionMode assignedUserSelectionMode,
        Set<String> assignedUserIds )
    {
        if ( !assignedUserIds.isEmpty() && AssignedUserSelectionMode.PROVIDED == assignedUserSelectionMode )
        {
            throw new IllegalQueryException(
                "Assigned User uid(s) cannot be specified if selectionMode is not PROVIDED" );
        }
    }

    private List<QueryItem> parseFilterAttributes( Set<String> filterAttributes, List<OrderParam> attributeOrderParams )
    {
        Map<String, TrackedEntityAttribute> attributes = attributeService.getAllTrackedEntityAttributes()
            .stream()
            .collect( Collectors.toMap( TrackedEntityAttribute::getUid, att -> att ) );

        List<QueryItem> result = new ArrayList<>();
        for ( String filter : filterAttributes )
        {
            result.add( getQueryItem( filter, attributes ) );
        }
        addAttributeQueryItemsFromOrder( result, attributes, attributeOrderParams );

        return result;
    }

    private void addAttributeQueryItemsFromOrder( List<QueryItem> filterAttributes,
        Map<String, TrackedEntityAttribute> attributes, List<OrderParam> attributeOrderParams )
    {
        List<QueryItem> orderQueryItems = attributeOrderParams.stream()
            .map( OrderParam::getField )
            .filter( att -> !containsAttributeFilter( filterAttributes, att ) )
            .map( attributes::get )
            .map( at -> new QueryItem( at, null, at.getValueType(), at.getAggregationType(), at.getOptionSet() ) )
            .collect( Collectors.toList() );

        filterAttributes.addAll( orderQueryItems );
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
            throw new IllegalQueryException( String.format(
                "filterAttributes can only have one filter per tracked entity attribute (TEA). The following TEA have more than one: %s",
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

    private QueryItem getQueryItem( String item )
    {
        String[] split = item.split( DimensionalObject.DIMENSION_NAME_SEP );

        if ( split == null || split.length % 2 != 1 )
        {
            throw new IllegalQueryException( "Query item or filter is invalid: " + item );
        }

        QueryItem queryItem = getItem( split[0] );

        if ( split.length > 1 )
        {
            for ( int i = 1; i < split.length; i += 2 )
            {
                QueryOperator operator = QueryOperator.fromString( split[i] );
                queryItem.getFilters().add( new QueryFilter( operator, split[i + 1] ) );
            }
        }

        return queryItem;
    }

    private QueryItem getItem( String item )
    {
        DataElement de = dataElementService.getDataElement( item );

        if ( de == null )
        {
            throw new IllegalQueryException( "Dataelement does not exist: " + item );
        }

        return new QueryItem( de, null, de.getValueType(), de.getAggregationType(), de.getOptionSet() );
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

    private List<OrderParam> getOrderParams( List<OrderCriteria> order )
    {
        if ( order == null || order.isEmpty() )
        {
            return Collections.emptyList();
        }
        validateOrderParams( order );

        return OrderParamsHelper.toOrderParams( order );
    }

    private Set<String> parseUids( String input )
    {
        return CollectionUtils.emptyIfNull( TextUtils.splitToSet( input, TextUtils.SEMICOLON ) )
            .stream()
            .filter( CodeGenerator::isValidUid )
            .collect( Collectors.toUnmodifiableSet() );
    }

    private void validateOrderParams( List<OrderCriteria> order )
    {
        Set<String> requestProperties = order.stream()
            .map( OrderCriteria::getField )
            .filter( field -> !CodeGenerator.isValidUid( field ) )
            .collect( Collectors.toSet() );

        Set<String> allowedProperties = schema.getProperties().stream().filter( Property::isSimple )
            .map( Property::getName ).collect( Collectors.toSet() );
        allowedProperties.addAll( NON_EVENT_SORTABLE_PROPERTIES );

        requestProperties.removeAll( allowedProperties );
        if ( !requestProperties.isEmpty() )
        {
            throw new IllegalQueryException(
                String.format( "Order by property `%s` is not supported. Supported are `%s`",
                    String.join( ", ", requestProperties ), String.join( ", ", allowedProperties ) ) );
        }
    }

    private List<OrderParam> mapToOrderParams( Map<String, SortDirection> orders )
    {
        return orders.entrySet().stream()
            .map( e -> new OrderParam( e.getKey(), e.getValue() ) )
            .collect( Collectors.toList() );
    }
}
