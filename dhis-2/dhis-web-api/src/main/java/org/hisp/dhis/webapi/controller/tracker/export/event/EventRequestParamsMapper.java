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
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamUtils.parseQueryItem;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamUtils.validateDeprecatedUidParameter;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamUtils.validateDeprecatedUidsParameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.tracker.export.event.EventOperationParams;
import org.hisp.dhis.tracker.export.event.JdbcEventStore;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.webapi.common.UID;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParamsHelper;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.springframework.stereotype.Component;

/**
 * Maps query parameters from {@link EventsExportController} stored in
 * {@link RequestParams} to {@link EventOperationParams} which is used to fetch
 * events from the DB.
 */
@Component
@RequiredArgsConstructor
class EventRequestParamsMapper
{
    private static final Set<String> SORTABLE_PROPERTIES = JdbcEventStore.QUERY_PARAM_COL_MAP.keySet();

    private final DataElementService dataElementService;

    public EventOperationParams map( RequestParams requestParams )
        throws BadRequestException
    {
        UID attributeCategoryCombo = validateDeprecatedUidParameter( "attributeCc", requestParams.getAttributeCc(),
            "attributeCategoryCombo", requestParams.getAttributeCategoryCombo() );

        Set<UID> attributeCategoryOptions = validateDeprecatedUidsParameter( "attributeCos",
            requestParams.getAttributeCos(),
            "attributeCategoryOptions",
            requestParams.getAttributeCategoryOptions() );

        Set<UID> eventUids = validateDeprecatedUidsParameter( "event", requestParams.getEvent(),
            "events",
            requestParams.getEvents() );

        validateFilter( requestParams.getFilter(), eventUids );

        Set<UID> assignedUsers = validateDeprecatedUidsParameter( "assignedUser", requestParams.getAssignedUser(),
            "assignedUsers",
            requestParams.getAssignedUsers() );

        Map<String, SortDirection> dataElementOrders = getDataElementsFromOrder( requestParams.getOrder() );

        List<QueryItem> dataElements = new ArrayList<>();
        for ( String order : dataElementOrders.keySet() )
        {
            dataElements.add( parseQueryItem( order, this::dataElementToQueryItem ) );
        }

        List<OrderParam> dataElementOrderParams = mapToOrderParams( dataElementOrders );

        List<QueryItem> filters = new ArrayList<>();
        for ( String eventCriteria : requestParams.getFilter() )
        {
            filters.add( parseQueryItem( eventCriteria, this::dataElementToQueryItem ) );
        }

        validateUpdateDurationParams( requestParams );

        return EventOperationParams.builder()
            .programUid( requestParams.getProgram() != null ? requestParams.getProgram().getValue() : null )
            .programStageUid(
                requestParams.getProgramStage() != null ? requestParams.getProgramStage().getValue() : null )
            .orgUnitUid( requestParams.getOrgUnit() != null ? requestParams.getOrgUnit().getValue() : null )
            .trackedEntityUid(
                requestParams.getTrackedEntity() != null ? requestParams.getTrackedEntity().getValue() : null )
            .programStatus( requestParams.getProgramStatus() )
            .followUp( requestParams.getFollowUp() )
            .orgUnitSelectionMode( requestParams.getOuMode() )
            .assignedUserMode( requestParams.getAssignedUserMode() )
            .assignedUsers( UID.toValueSet( assignedUsers ) )
            .startDate( requestParams.getOccurredAfter() )
            .endDate( requestParams.getOccurredBefore() )
            .scheduledAfter( requestParams.getScheduledAfter() )
            .scheduledBefore( requestParams.getScheduledBefore() )
            .updatedAfter( requestParams.getUpdatedAfter() )
            .updatedBefore( requestParams.getUpdatedBefore() )
            .updatedWithin( requestParams.getUpdatedWithin() )
            .enrollmentEnrolledBefore( requestParams.getEnrollmentEnrolledBefore() )
            .enrollmentEnrolledAfter( requestParams.getEnrollmentEnrolledAfter() )
            .enrollmentOccurredBefore( requestParams.getEnrollmentOccurredBefore() )
            .enrollmentOccurredAfter( requestParams.getEnrollmentOccurredAfter() )
            .eventStatus( requestParams.getStatus() )
            .attributeCategoryCombo( attributeCategoryCombo != null ? attributeCategoryCombo.getValue() : null )
            .attributeCategoryOptions( UID.toValueSet( attributeCategoryOptions ) )
            .idSchemes( requestParams.getIdSchemes() )
            .page( requestParams.getPage() )
            .pageSize( requestParams.getPageSize() )
            .totalPages( requestParams.isTotalPages() )
            .skipPaging( toBooleanDefaultIfNull( requestParams.isSkipPaging(), false ) )
            .skipEventId( requestParams.getSkipEventId() )
            .includeAttributes( false )
            .includeAllDataElements( false )
            .dataElements( new HashSet<>( dataElements ) )
            .filters( filters )
            .filterAttributes( requestParams.getFilterAttributes() )
            .orders( getOrderParams( requestParams.getOrder() ) )
            .gridOrders( dataElementOrderParams )
            .attributeOrders( requestParams.getOrder() )
            .events( UID.toValueSet( eventUids ) )
            .enrollments( UID.toValueSet( requestParams.getEnrollments() ) )
            .includeDeleted( requestParams.isIncludeDeleted() ).build();
    }

    private static void validateFilter( Set<String> filters, Set<UID> eventIds )
        throws BadRequestException
    {
        if ( !CollectionUtils.isEmpty( eventIds ) && !CollectionUtils.isEmpty( filters ) )
        {
            throw new BadRequestException( "Event UIDs and filters can not be specified at the same time" );
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

    private void validateUpdateDurationParams( RequestParams requestParams )
        throws BadRequestException
    {
        if ( requestParams.getUpdatedWithin() != null
            && (requestParams.getUpdatedAfter() != null || requestParams.getUpdatedBefore() != null) )
        {
            throw new BadRequestException(
                "Last updated from and/or to and last updated duration cannot be specified simultaneously" );
        }

        if ( requestParams.getUpdatedWithin() != null
            && DateUtils.getDuration( requestParams.getUpdatedWithin() ) == null )
        {
            throw new BadRequestException( "Duration is not valid: " + requestParams.getUpdatedWithin() );
        }
    }
}
