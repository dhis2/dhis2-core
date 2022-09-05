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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.hisp.dhis.query.QueryUtils;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam.SortDirection;
import org.hisp.dhis.webapi.controller.event.webrequest.EventCriteria;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.hisp.dhis.webapi.controller.event.webrequest.tracker.TrackerEventCriteria;
import org.hisp.dhis.webapi.controller.event.webrequest.tracker.mapper.TrackerEventCriteriaMapper;
import org.mapstruct.factory.Mappers;
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

    private static final TrackerEventCriteriaMapper TRACKER_EVENT_CRITERIA_MAPPER = Mappers
        .getMapper( TrackerEventCriteriaMapper.class );

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
        return map( TRACKER_EVENT_CRITERIA_MAPPER.toEventCriteria( eventCriteria ) );
    }

    private EventSearchParams map( EventCriteria eventCriteria )
    {
        CategoryOptionCombo attributeOptionCombo = inputUtils.getAttributeOptionCombo(
            eventCriteria.getAttributeCc(),
            eventCriteria.getAttributeCos(),
            true );

        Set<String> eventIds = eventCriteria.getEvents();
        Set<String> assignedUserIds = eventCriteria.getAssignedUsers();
        Map<String, SortDirection> dataElementOrders = getDataElementsFromOrder( eventCriteria.getOrder() );

        String program = eventCriteria.getProgram();
        String programStage = eventCriteria.getProgramStage();
        String orgUnit = eventCriteria.getOrgUnit();
        String trackedEntityInstance = eventCriteria.getTrackedEntityInstance();
        Date lastUpdatedStartDate = eventCriteria.getLastUpdatedStartDate() != null
            ? eventCriteria.getLastUpdatedStartDate()
            : eventCriteria.getLastUpdated();
        Set<String> programInstances = eventCriteria.getProgramInstances();
        AssignedUserSelectionMode assignedUserSelectionMode = eventCriteria.getAssignedUserMode();
        Set<String> filters = eventCriteria.getFilter();
        Set<String> dataElements = dataElementOrders.keySet();
        User user = currentUserService.getCurrentUser();

        EventSearchParams params = new EventSearchParams();

        Program pr = programService.getProgram( program );

        if ( !StringUtils.isEmpty( program ) && pr == null )
        {
            throw new IllegalQueryException( "Program is specified but does not exist: " + program );
        }

        ProgramStage ps = programStageService.getProgramStage( programStage );

        if ( !StringUtils.isEmpty( programStage ) && ps == null )
        {
            throw new IllegalQueryException( "Program stage is specified but does not exist: " + programStage );
        }

        OrganisationUnit ou = organisationUnitService.getOrganisationUnit( orgUnit );

        if ( !StringUtils.isEmpty( orgUnit ) && ou == null )
        {
            throw new IllegalQueryException( "Org unit is specified but does not exist: " + orgUnit );
        }

        if ( pr != null && !user.isSuper() && !aclService.canDataRead( user, pr ) )
        {
            throw new IllegalQueryException( "User has no access to program: " + pr.getUid() );
        }

        if ( ps != null && !user.isSuper() && !aclService.canDataRead( user, ps ) )
        {
            throw new IllegalQueryException( "User has no access to program stage: " + ps.getUid() );
        }

        TrackedEntityInstance tei = entityInstanceService.getTrackedEntityInstance( trackedEntityInstance );

        if ( !StringUtils.isEmpty( trackedEntityInstance ) && tei == null )
        {
            throw new IllegalQueryException(
                "Tracked entity instance is specified but does not exist: " + trackedEntityInstance );
        }

        if ( attributeOptionCombo != null && !user.isSuper()
            && !aclService.canDataRead( user, attributeOptionCombo ) )
        {
            throw new IllegalQueryException(
                "User has no access to attribute category option combo: " + attributeOptionCombo.getUid() );
        }

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

        if ( dataElements != null )
        {
            for ( String de : dataElements )
            {
                QueryItem dataElement = getQueryItem( de );

                params.getDataElements().add( dataElement );
            }
        }

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
            .setStartDate( eventCriteria.getStartDate() ).setEndDate( eventCriteria.getEndDate() )
            .setDueDateStart( eventCriteria.getDueDateStart() ).setDueDateEnd( eventCriteria.getDueDateEnd() )
            .setLastUpdatedStartDate( lastUpdatedStartDate )
            .setLastUpdatedEndDate( eventCriteria.getLastUpdatedEndDate() )
            .setLastUpdatedDuration( eventCriteria.getLastUpdatedDuration() )
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
        if ( order != null && !order.isEmpty() )
        {
            return QueryUtils.filteredBySchema( order, schema );
        }
        return Collections.emptyList();
    }

    private List<OrderParam> getGridOrderParams( List<OrderCriteria> order,
        Map<String, SortDirection> dataElementOrders )
    {
        return Optional.ofNullable( order )
            .orElse( Collections.emptyList() )
            .stream()
            .filter( Objects::nonNull )
            .filter( orderCriteria -> dataElementOrders.containsKey( orderCriteria.getField() ) )
            .map( orderCriteria -> OrderParam.builder()
                .field( orderCriteria.getField() )
                .direction( dataElementOrders.get( orderCriteria.getField() ) )
                .build() )
            .collect( Collectors.toList() );
    }
}
