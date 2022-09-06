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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.commons.collection.CollectionUtils;
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
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam.SortDirection;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParamsHelper;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.springframework.stereotype.Component;

@Component( "org.hisp.dhis.webapi.controller.tracker.export.EventRequestToSearchParamsMapper" )
@RequiredArgsConstructor
class EventRequestToSearchParamsMapper
{

    private final CurrentUserService currentUserService;

    private final ProgramService programService;

    private final OrganisationUnitService organisationUnitService;

    private final ProgramStageService programStageService;

    private final AclService aclService;

    private final TrackedEntityInstanceService entityInstanceService;

    private final DataElementService dataElementService;

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
        EventSearchParams params = new EventSearchParams();

        String program = eventCriteria.getProgram();
        Program pr = programService.getProgram( program );
        if ( !StringUtils.isEmpty( program ) && pr == null )
        {
            throw new IllegalQueryException( "Program is specified but does not exist: " + program );
        }

        String programStage = eventCriteria.getProgramStage();
        ProgramStage ps = programStageService.getProgramStage( programStage );
        if ( !StringUtils.isEmpty( programStage ) && ps == null )
        {
            throw new IllegalQueryException( "Program stage is specified but does not exist: " + programStage );
        }

        String orgUnit = eventCriteria.getOrgUnit();
        OrganisationUnit ou = organisationUnitService.getOrganisationUnit( orgUnit );
        if ( !StringUtils.isEmpty( orgUnit ) && ou == null )
        {
            throw new IllegalQueryException( "Org unit is specified but does not exist: " + orgUnit );
        }

        User user = currentUserService.getCurrentUser();
        if ( pr != null && !user.isSuper() && !aclService.canDataRead( user, pr ) )
        {
            throw new IllegalQueryException( "User has no access to program: " + pr.getUid() );
        }

        if ( ps != null && !user.isSuper() && !aclService.canDataRead( user, ps ) )
        {
            throw new IllegalQueryException( "User has no access to program stage: " + ps.getUid() );
        }

        String trackedEntityInstance = eventCriteria.getTrackedEntity();
        TrackedEntityInstance tei = entityInstanceService.getTrackedEntityInstance( trackedEntityInstance );
        if ( !StringUtils.isEmpty( trackedEntityInstance ) && tei == null )
        {
            throw new IllegalQueryException(
                "Tracked entity instance is specified but does not exist: " + trackedEntityInstance );
        }

        CategoryOptionCombo attributeOptionCombo = inputUtils.getAttributeOptionCombo(
            eventCriteria.getAttributeCc(),
            eventCriteria.getAttributeCos(),
            true );
        if ( attributeOptionCombo != null && !user.isSuper()
            && !aclService.canDataRead( user, attributeOptionCombo ) )
        {
            throw new IllegalQueryException(
                "User has no access to attribute category option combo: " + attributeOptionCombo.getUid() );
        }

        Set<String> eventIds = eventCriteria.getEvents();
        Set<String> filters = eventCriteria.getFilter();
        if ( !CollectionUtils.isEmpty( eventIds ) && !CollectionUtils.isEmpty( filters ) )
        {
            throw new IllegalQueryException( "Event UIDs and filters can not be specified at the same time" );
        }
        if ( eventIds == null )
        {
            eventIds = new HashSet<>();
        }

        if ( filters != null )
        {
            if ( !StringUtils.isEmpty( programStage ) && ps == null )
            {
                throw new IllegalQueryException( "ProgramStage needs to be specified for event filtering to work" );
            }

            for ( String filter : filters )
            {
                QueryItem item = getQueryItem( filter );
                params.getFilters().add( item );
            }
        }

        Map<String, SortDirection> dataElementOrders = getDataElementsFromOrder( eventCriteria.getOrder() );
        Set<String> dataElements = dataElementOrders.keySet();
        if ( dataElements != null )
        {
            for ( String de : dataElements )
            {
                QueryItem dataElement = getQueryItem( de );

                params.getDataElements().add( dataElement );
            }
        }

        AssignedUserSelectionMode assignedUserSelectionMode = eventCriteria.getAssignedUserMode();
        Set<String> assignedUserIds = eventCriteria.getAssignedUsers();
        if ( assignedUserSelectionMode != null && assignedUserIds != null && !assignedUserIds.isEmpty()
            && !assignedUserSelectionMode.equals( AssignedUserSelectionMode.PROVIDED ) )
        {
            throw new IllegalQueryException(
                "Assigned User uid(s) cannot be specified if selectionMode is not PROVIDED" );
        }

        if ( assignedUserIds != null )
        {
            assignedUserIds = assignedUserIds.stream()
                .filter( CodeGenerator::isValidUid )
                .collect( Collectors.toSet() );
        }

        Set<String> programInstances = eventCriteria.getEnrollments();
        if ( programInstances != null )
        {
            programInstances = programInstances.stream()
                .filter( CodeGenerator::isValidUid )
                .collect( Collectors.toSet() );
        }

        return params.setProgram( pr ).setProgramStage( ps ).setOrgUnit( ou ).setTrackedEntityInstance( tei )
            .setProgramStatus( eventCriteria.getProgramStatus() ).setFollowUp( eventCriteria.getFollowUp() )
            .setOrgUnitSelectionMode( eventCriteria.getOuMode() )
            .setAssignedUserSelectionMode( assignedUserSelectionMode ).setAssignedUsers( assignedUserIds )
            .setStartDate( eventCriteria.getOccurredAfter() ).setEndDate( eventCriteria.getOccurredBefore() )
            .setDueDateStart( eventCriteria.getScheduledAfter() ).setDueDateEnd( eventCriteria.getScheduledBefore() )
            .setLastUpdatedStartDate( eventCriteria.getUpdatedAfter() )
            .setLastUpdatedEndDate( eventCriteria.getUpdatedBefore() )
            .setLastUpdatedDuration( eventCriteria.getUpdatedWithin() )
            .setEnrollmentEnrolledAfter( eventCriteria.getEnrollmentEnrolledAfter() )
            .setEventStatus( eventCriteria.getStatus() )
            .setCategoryOptionCombo( attributeOptionCombo ).setIdSchemes( eventCriteria.getIdSchemes() )
            .setPage( eventCriteria.getPage() )
            .setPageSize( eventCriteria.getPageSize() ).setTotalPages( eventCriteria.isTotalPages() )
            .setSkipPaging( eventCriteria.isSkipPaging() )
            .setSkipEventId( eventCriteria.getSkipEventId() ).setIncludeAttributes( false )
            .setIncludeAllDataElements( false ).setOrders( getOrderParams( eventCriteria.getOrder() ) )
            .setGridOrders( getGridOrderParams( eventCriteria.getOrder(), dataElementOrders ) )
            .setEvents( eventIds ).setProgramInstances( programInstances )
            .setIncludeDeleted( eventCriteria.isIncludeDeleted() );
    }

    private Map<String, SortDirection> getDataElementsFromOrder( List<OrderCriteria> allOrders )
    {
        Map<String, SortDirection> dataElements = new HashMap<>();

        if ( allOrders != null )
        {
            for ( OrderCriteria orderCriteria : allOrders )
            {
                DataElement de = dataElementService.getDataElement( orderCriteria.getField() );
                if ( de != null )
                {
                    dataElements.put( orderCriteria.getField(), orderCriteria.getDirection() );
                }
            }
        }
        return dataElements;
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

    private List<OrderParam> getOrderParams( List<OrderCriteria> order )
    {
        if ( order == null || order.isEmpty() )
        {
            return Collections.emptyList();
        }
        validateOrderParams( order );

        return OrderParamsHelper.toOrderParams( order );
    }

    private void validateOrderParams( List<OrderCriteria> order )
    {
        Set<String> requestProperties = order.stream().map( OrderCriteria::getField ).collect( Collectors.toSet() );

        Stream<String> eventProperties = schema.getProperties().stream().filter( Property::isSimple )
            .map( Property::getName );
        // Other properties that we allow to order by that are not in the Event
        // schema have to be specified in JdbcEventStore.QUERY_PARAM_COL_MAP
        Stream<String> nonEventProperties = Stream.of( "enrolledAt" );
        Set<String> allowedProperties = Stream.concat( eventProperties, nonEventProperties )
            .collect( Collectors.toSet() );

        requestProperties.removeAll( allowedProperties );
        if ( !requestProperties.isEmpty() )
        {
            throw new IllegalQueryException(
                String.format( "Order by property `%s` is not supported. Supported are `%s`",
                    String.join( ", ", requestProperties ), String.join( ", ", allowedProperties ) ) );
        }
    }

    private List<OrderParam> getGridOrderParams( List<OrderCriteria> order,
        Map<String, SortDirection> dataElementOrders )
    {
        return Optional.ofNullable( order )
            .orElse( Collections.emptyList() )
            .stream()
            .filter( Objects::nonNull )
            .filter( orderCriteria -> dataElementOrders.containsKey( orderCriteria.getField() ) )
            .map( orderCriteria -> new OrderParam(
                orderCriteria.getField(),
                dataElementOrders.get( orderCriteria.getField() ) ) )
            .collect( Collectors.toList() );
    }
}
