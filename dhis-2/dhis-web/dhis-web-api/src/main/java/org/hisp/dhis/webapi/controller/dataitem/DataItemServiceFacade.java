package org.hisp.dhis.webapi.controller.dataitem;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.hisp.dhis.common.DataDimensionItem.DATA_DIMENSION_TYPE_CLASS_MAP;
import static org.hisp.dhis.webapi.utils.PaginationUtils.getPaginationData;
import static org.springframework.beans.BeanUtils.copyProperties;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.DataDimensionItemType;
import org.hisp.dhis.dxf2.common.OrderParams;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.query.QueryUtils;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.stereotype.Component;

/**
 * This class is tight to the controller layer and is responsible to encapsulate
 * logic that does not belong to the controller but does not belong to the
 * service layer either. In other words, these set of methods sit between the
 * controller and service layers. The main goal is to alleviate the controller
 * layer.
 */
@Component
class DataItemServiceFacade
{
    private final QueryService queryService;

    private final SchemaService schemaService;

    DataItemServiceFacade( final QueryService queryService, final SchemaService schemaService )
    {
        checkNotNull( queryService );
        checkNotNull( schemaService );

        this.queryService = queryService;
        this.schemaService = schemaService;
    }

    List<BaseDimensionalItemObject> retrieveDataItems(
        final List<Class<? extends BaseDimensionalItemObject>> targetEntities, final List<String> filters,
        final WebOptions options, final OrderParams orderParams )
    {
        final List<BaseDimensionalItemObject> convertedItems = new ArrayList<>( 0 );

        // TODO: Check if it can be executed in parallel
        for ( final Class<? extends BaseDimensionalItemObject> entity : targetEntities )
        {
            convertedItems.addAll( queryObjectsForEntity( entity, orderParams, filters, options ) );
        }

        return convertedItems;
    }

    private List<BaseDimensionalItemObject> queryObjectsForEntity(
        final Class<? extends BaseDimensionalItemObject> entity, final OrderParams orderParams,
        final List<String> filters, final WebOptions options )
    {
        final Query query = buildQueryForEntity( entity, orderParams, filters, options );

        final List<? extends BaseDimensionalItemObject> dimensionalItems = (List<? extends BaseDimensionalItemObject>) queryService
            .query( query );

        final List<BaseDimensionalItemObject> convertedItems = new ArrayList<>( 0 );

        if ( isNotEmpty( dimensionalItems ) )
        {
            convertedItems.addAll( convertToBaseDataObject( dimensionalItems ) );
        }

        return convertedItems;
    }

    /**
     * This method will build a Query object based on the provided arguments.
     * 
     * @param entity the BaseDimensionalItemObject class to be queried.
     * @param orderParams request ordering params
     * @param filters request filters
     * @param options request option
     * @return the built query
     * @throws org.hisp.dhis.query.QueryParserException if errors occur during the
     *         query creation
     */
    private Query buildQueryForEntity( final Class<? extends BaseDimensionalItemObject> entity,
        final OrderParams orderParams, final List<String> filters,
        final WebOptions options )
    {
        final Schema schema = schemaService.getDynamicSchema( entity );
        final List<Order> orders = orderParams.getOrders( schema );

        final Query query = queryService.getQueryFromUrl( entity, filters, orders,
            getPaginationData( options ), options.getRootJunction() );
        query.setDefaultOrder();

        return query;
    }

    /**
     * This method returns a list of BaseDimensionalItemObject's based on the
     * provided filters. It will also remove, from the filters, the objects found.
     * 
     * @param filters
     * @return the data items classes to be queried
     */
    List<Class<? extends BaseDimensionalItemObject>> extractTargetEntities( final List<String> filters )
    {
        final List<Class<? extends BaseDimensionalItemObject>> targetedEntities = new ArrayList<>( 0 );

        if ( isNotEmpty( filters ) )
        {
            final Iterator<String> iterator = filters.iterator();

            while ( iterator.hasNext() )
            {
                final Class<? extends BaseDimensionalItemObject> entity = extractEntityFromFilter( iterator.next() );
                final boolean entityWasFound = entity != null;

                if ( entityWasFound )
                {
                    targetedEntities.add( entity );
                    iterator.remove();
                }
            }
        }

        return targetedEntities;
    }

    /**
     * This method will return the respective BaseDimensionalItemObject class from
     * the filter provided.
     * 
     * @param filter has a format of "dimensionItemType:eq:INDICATOR", where
     *        INDICATOR represents the BaseDimensionalItemObject. It could be any
     *        value represented by
     *        {@link org.hisp.dhis.common.DataDimensionItemType}
     * @return the respective class associated with the given filter
     * @throws org.hisp.dhis.query.QueryParserException if the filter points to a
     *         non supported class/entity.
     */
    private Class<? extends BaseDimensionalItemObject> extractEntityFromFilter( final String filter )
    {
        final byte DIMENSION_TYPE = 2;
        Class<? extends BaseDimensionalItemObject> entity = null;

        if ( trimToEmpty( filter ).contains( "dimensionItemType:eq:" ) )
        {
            final String[] array = filter.split( ":" );
            final boolean hasDimensionType = array.length == 3;

            if ( hasDimensionType )
            {
                entity = (Class<? extends BaseDimensionalItemObject>) DATA_DIMENSION_TYPE_CLASS_MAP
                    .get( QueryUtils.getEnumValue( DataDimensionItemType.class, array[DIMENSION_TYPE] ) );
            }
        }
        return entity;
    }

    private List<BaseDimensionalItemObject> convertToBaseDataObject(
        final List<? extends BaseDimensionalItemObject> mainItems )
    {
        final List<BaseDimensionalItemObject> convertedItems = new ArrayList<>( 0 );

        for ( final BaseDimensionalItemObject baseDataItemObject : mainItems )
        {
            final BaseDimensionalItemObject baseItem = new BaseDimensionalItemObject();
            copyProperties( baseDataItemObject, baseItem );

            convertedItems.add( baseItem );
        }

        return convertedItems;
    }
}
