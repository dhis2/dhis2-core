/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.webapi.controller.dataitem;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.webapi.controller.dataitem.helper.FilteringHelper.containsDimensionTypeFilter;
import static org.hisp.dhis.webapi.controller.dataitem.helper.FilteringHelper.extractEntitiesFromInFilter;
import static org.hisp.dhis.webapi.controller.dataitem.helper.FilteringHelper.extractEntityFromEqualFilter;
import static org.hisp.dhis.webapi.controller.dataitem.helper.OrderingHelper.sort;
import static org.hisp.dhis.webapi.controller.dataitem.helper.PaginationHelper.slice;
import static org.hisp.dhis.webapi.utils.PaginationUtils.NO_PAGINATION;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dxf2.common.OrderParams;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeDimensionItem;
import org.hisp.dhis.query.Pagination;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryService;
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
public class DataItemServiceFacade
{
    private final int PAGINATION_FIRST_RESULT = 0;

    private final QueryService queryService;

    /**
     * This Map holds the allowed data types to be queried.
     */
    // @formatter:off
    public static final Map<String, Class<? extends BaseDimensionalItemObject>> DATA_TYPE_ENTITY_MAP = ImmutableMap
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

    DataItemServiceFacade( final QueryService queryService )
    {
        checkNotNull( queryService );

        this.queryService = queryService;
    }

    /**
     * This method will iterate through the list of target entities, and query
     * each one of them using the filters and params provided. The result list
     * will bring together the results of all target entities queried.
     *
     * @param targetEntities the list of entities to be retrieved
     * @param orderParams request ordering params
     * @param filters request filters
     * @param options request options
     * @return the consolidated collection of entities found.
     */
    List<BaseDimensionalItemObject> retrieveDataItemEntities(
        final Set<Class<? extends BaseDimensionalItemObject>> targetEntities, final List<String> filters,
        final WebOptions options, final OrderParams orderParams )
    {
        List<BaseDimensionalItemObject> dimensionalItems = new ArrayList<>( 0 );

        if ( isNotEmpty( targetEntities ) )
        {
            // Retrieving all items for each entity type.
            for ( final Class<? extends BaseDimensionalItemObject> entity : targetEntities )
            {
                final Query query = buildQueryForEntity( entity, filters, options );
                dimensionalItems.addAll( executeQuery( query ) );
            }

            // In memory sorting
            sort( dimensionalItems, orderParams );

            // In memory pagination.
            dimensionalItems = slice( options, dimensionalItems );
        }

        return dimensionalItems;
    }

    /**
     * This method returns a set of BaseDimensionalItemObject's based on the
     * provided filters. It will also remove, from the filters, the objects
     * found.
     *
     * @param filters
     * @return the data items classes to be queried
     */
    Set<Class<? extends BaseDimensionalItemObject>> extractTargetEntities( final List<String> filters )
    {
        final Set<Class<? extends BaseDimensionalItemObject>> targetedEntities = new HashSet<>( 0 );

        if ( containsDimensionTypeFilter( filters ) )
        {
            final Iterator<String> iterator = filters.iterator();

            while ( iterator.hasNext() )
            {
                final String filter = iterator.next();
                final Class<? extends BaseDimensionalItemObject> entity = extractEntityFromEqualFilter( filter );
                final Set<Class<? extends BaseDimensionalItemObject>> entities = extractEntitiesFromInFilter( filter );

                if ( entity != null || isNotEmpty( entities ) )
                {
                    if ( entity != null )
                    {
                        targetedEntities.add( entity );
                    }
                    if ( isNotEmpty( entities ) )
                    {
                        targetedEntities.addAll( entities );
                    }

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
    @SuppressWarnings( "unchecked" )
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
     * @param filters request filters
     * @param options request options
     * @return the built query
     * @throws org.hisp.dhis.query.QueryParserException if errors occur during
     *         the query creation
     */
    private Query buildQueryForEntity( final Class<? extends BaseDimensionalItemObject> entity,
        final List<String> filters, final WebOptions options )
    {
        final int maxLimit = options.getPage() * options.getPageSize();
        final Pagination pagination = options.hasPaging()
            ? new Pagination( PAGINATION_FIRST_RESULT, maxLimit )
            : NO_PAGINATION;

        final Query query = queryService.getQueryFromUrl( entity, filters, emptyList(),
            pagination, options.getRootJunction() );
        query.setDefaultOrder();

        return query;
    }
}
