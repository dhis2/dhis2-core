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
package org.hisp.dhis.query;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.fieldfilter.Defaults;
import org.hisp.dhis.preheat.Preheat;
import org.hisp.dhis.query.planner.QueryPlan;
import org.hisp.dhis.query.planner.QueryPlanner;
import org.springframework.stereotype.Component;

/**
 * Default implementation of QueryService which works with IdObjects.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class DefaultQueryService
    implements QueryService
{
    private static final Junction.Type DEFAULT_JUNCTION_TYPE = Junction.Type.AND;

    private final QueryParser queryParser;

    private final QueryPlanner queryPlanner;

    private final JpaCriteriaQueryEngine<? extends IdentifiableObject> criteriaQueryEngine;

    private final InMemoryQueryEngine<? extends IdentifiableObject> inMemoryQueryEngine;

    @Override
    public List<? extends IdentifiableObject> query( Query query )
    {
        return queryObjects( query );
    }

    @Override
    @SuppressWarnings( { "unchecked", "rawtypes" } )
    public List<? extends IdentifiableObject> query( Query query, ResultTransformer transformer )
    {
        List<? extends IdentifiableObject> objects = queryObjects( query );

        if ( transformer != null )
        {
            return transformer.transform( objects );
        }

        return objects;
    }

    @Override
    public long count( Query query )
    {
        Query cloned = Query.from( query );

        cloned.clearOrders();
        cloned.setFirstResult( 0 );
        cloned.setMaxResults( Integer.MAX_VALUE );

        return countObjects( cloned );
    }

    @Override
    public Query getQueryFromUrl( Class<?> klass, List<String> filters, List<Order> orders, Pagination pagination )
        throws QueryParserException
    {
        return getQueryFromUrl( klass, filters, orders, pagination, DEFAULT_JUNCTION_TYPE );
    }

    @Override
    public Query getQueryFromUrl( Class<?> klass, List<String> filters, List<Order> orders, Pagination pagination,
        Junction.Type rootJunction )
        throws QueryParserException
    {
        Query query = queryParser.parse( klass, filters, rootJunction );
        query.addOrders( orders );

        if ( pagination.hasPagination() )
        {
            query.setFirstResult( pagination.getFirstResult() );
            query.setMaxResults( pagination.getSize() );
        }

        return query;
    }

    @Override
    public Query getQueryFromUrl( Class<?> klass, List<String> filters, List<Order> orders )
        throws QueryParserException
    {
        return getQueryFromUrl( klass, filters, orders, new Pagination(), DEFAULT_JUNCTION_TYPE );
    }

    // ---------------------------------------------------------------------------------------------
    // Helper methods
    // ---------------------------------------------------------------------------------------------

    private long countObjects( Query query )
    {
        List<? extends IdentifiableObject> objects;
        QueryPlan queryPlan = queryPlanner.planQuery( query );
        Query pQuery = queryPlan.getPersistedQuery();
        Query npQuery = queryPlan.getNonPersistedQuery();
        if ( !npQuery.isEmpty() )
        {
            npQuery.setObjects( criteriaQueryEngine.query( pQuery ) );
            objects = inMemoryQueryEngine.query( npQuery );
            return objects.size();
        }
        return criteriaQueryEngine.count( pQuery );
    }

    private List<? extends IdentifiableObject> queryObjects( Query query )
    {
        List<? extends IdentifiableObject> objects = query.getObjects();

        if ( objects != null )
        {
            objects = inMemoryQueryEngine.query( query.setObjects( objects ) );
            clearDefaults( query.getSchema().getKlass(), objects, query.getDefaults() );

            return objects;
        }

        QueryPlan queryPlan = queryPlanner.planQuery( query );

        Query pQuery = queryPlan.getPersistedQuery();
        Query npQuery = queryPlan.getNonPersistedQuery();

        objects = criteriaQueryEngine.query( pQuery );

        if ( !npQuery.isEmpty() )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Doing in-memory for " + npQuery.getCriterions().size() + " criterions and "
                    + npQuery.getOrders().size() + " orders." );
            }

            npQuery.setObjects( objects );

            objects = inMemoryQueryEngine.query( npQuery );
        }

        clearDefaults( query.getSchema().getKlass(), objects, query.getDefaults() );

        return objects;
    }

    private void clearDefaults( Class<?> klass, List<? extends IdentifiableObject> objects, Defaults defaults )
    {
        if ( Defaults.INCLUDE == defaults || !Preheat.isDefaultClass( klass ) )
        {
            return;
        }

        objects.removeIf( object -> "default".equals( object.getName() ) );
    }
}
