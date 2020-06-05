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
import static org.hisp.dhis.webapi.utils.PaginationUtils.getPaginationData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dxf2.common.OrderParams;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeDimensionItem;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;

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

    private final ProgramService programService;

    private final SchemaService schemaService;

    /**
     * This Map holds the allowed data types to be queried.
     */
    // @formatter:off
    private static final Map<String, Class<? extends BaseDimensionalItemObject>> DATA_TYPE_ENTITY_MAP = ImmutableMap
        .<String, Class<? extends BaseDimensionalItemObject>> builder()
            .put( "INDICATOR", Indicator.class )
            .put( "DATA_ELEMENT", DataElement.class )
            .put( "DATA_ELEMENT_OPERAND", DataElementOperand.class )
            .put( "DATA_SET", DataSet.class )
            .put( "PROGRAM_INDICATOR", ProgramIndicator.class )
            .put( "PROGRAM_DATA_ELEMENT", ProgramDataElementDimensionItem.class )
            .put( "PROGRAM_ATTRIBUTE", ProgramTrackedEntityAttributeDimensionItem.class )
            .build();
    // @formatter:on

    DataItemServiceFacade( final QueryService queryService, final ProgramService programService,
        final SchemaService schemaService )
    {
        checkNotNull( queryService );
        checkNotNull( programService );
        checkNotNull( schemaService );

        this.queryService = queryService;
        this.programService = programService;
        this.schemaService = schemaService;
    }

    /**
     * This method will iterate through the list of target entities, and query each
     * one of them using the filters and params provided. The result list will bring
     * together the results of all target entities queried.
     * 
     * @param targetEntities the list of entities to be retrieved
     * @param orderParams request ordering params
     * @param filters request filters
     * @param options request options
     * @return the consolidated collection of entities found.
     */
    List<BaseDimensionalItemObject> retrieveDataItemEntities(
        final List<Class<? extends BaseDimensionalItemObject>> targetEntities, final List<String> filters,
        final WebOptions options, final OrderParams orderParams )
    {
        final List<BaseDimensionalItemObject> dataItemEntities = new ArrayList<>( 0 );

        if ( isNotEmpty( targetEntities ) )
        {
            for ( final Class<? extends BaseDimensionalItemObject> entity : targetEntities )
            {
                final Query query = buildQueryForEntity( entity, orderParams, filters, options );
                dataItemEntities.addAll( executeQuery( query ) );
            }
        }

        return dataItemEntities;
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
        else
        {
            // If no filter is set we search for all entities.
            targetedEntities.addAll( DATA_TYPE_ENTITY_MAP.values() );
        }

        return targetedEntities;
    }

    /**
     * Execute the given query.
     *
     * @param query the query to be executed
     * @return the list of entities found
     */
    private List<BaseDimensionalItemObject> executeQuery( final Query query )
    {
        final List<BaseDimensionalItemObject> dimensionalItems = (List<BaseDimensionalItemObject>) queryService
            .query( query );

        return dimensionalItems;
    }

    /**
     * This method will build a Query object based on the provided arguments.
     * 
     * @param entity the BaseDimensionalItemObject class to be queried.
     * @param orderParams request ordering params
     * @param filters request filters
     * @param options request options
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

        if ( options.contains( "program.id" ) )
        {
            final String programUid = options.get( "program.id" );
            final List<ProgramDataElementDimensionItem> programDataElements = programService
                .getGeneratedProgramDataElements( programUid );
            query.setObjects( programDataElements );
        }

        return query;
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
                // TODO: Add support all other missing Entities
                entity = DATA_TYPE_ENTITY_MAP.get( array[DIMENSION_TYPE] );

                if ( entity == null )
                {
                    throw new QueryParserException( "Unable to parse `" + array[DIMENSION_TYPE]
                        + "`, available values are: " + Arrays.toString( DATA_TYPE_ENTITY_MAP.keySet().toArray() ) );
                }
            }
        }

        return entity;
    }
}
