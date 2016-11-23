package org.hisp.dhis.query.planner;

/*
 * Copyright (c) 2004-2016, University of Oslo
 *  All rights reserved.
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

import org.hisp.dhis.query.Conjunction;
import org.hisp.dhis.query.Criterion;
import org.hisp.dhis.query.Disjunction;
import org.hisp.dhis.query.Junction;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.Restriction;
import org.hisp.dhis.schema.Property;

import java.util.Iterator;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class DefaultQueryPlanner implements QueryPlanner
{
    @Override
    public QueryPlan planQuery( Query query )
    {
        Query npQuery = Query.from( query );
        Query pQuery = getPersistedQuery( npQuery );

        // if there are any non persisted criterions left, we leave the paging to the in-memory engine
        if ( !npQuery.getCriterions().isEmpty() )
        {
            pQuery.setSkipPaging( true );
        }
        else
        {
            pQuery.setFirstResult( npQuery.getFirstResult() );
            pQuery.setMaxResults( npQuery.getMaxResults() );
        }

        return new QueryPlan( pQuery, npQuery );
    }

    /**
     * Remove criterions that can be applied by criteria engine, and return those.
     *
     * @param query Query
     * @return Query instance
     */
    private Query getPersistedQuery( Query query )
    {
        Query pQuery = Query.from( query.getSchema(), query.getRootJunction().getType() );
        Iterator<Criterion> iterator = query.getCriterions().iterator();

        while ( iterator.hasNext() )
        {
            org.hisp.dhis.query.Criterion criterion = iterator.next();

            if ( Junction.class.isInstance( criterion ) )
            {
                Junction junction = handleJunctionCriteriaQuery( pQuery, (Junction) criterion );

                if ( !junction.getCriterions().isEmpty() )
                {
                    pQuery.add( junction );
                }

                if ( ((Junction) criterion).getCriterions().isEmpty() )
                {
                    iterator.remove();
                }
            }
            else if ( Restriction.class.isInstance( criterion ) )
            {
                Restriction restriction = (Restriction) criterion;

                if ( !restriction.getPath().contains( "\\." ) )
                {
                    if ( pQuery.getSchema().haveProperty( restriction.getPath() ) )
                    {
                        Property property = query.getSchema().getProperty( restriction.getPath() );

                        if ( property.isSimple() && property.isPersisted() )
                        {
                            pQuery.getCriterions().add( criterion );
                            iterator.remove();
                        }
                    }
                }
            }
        }

        if ( query.ordersPersisted() )
        {
            pQuery.addOrders( query.getOrders() );
            query.clearOrders();
        }

        return pQuery;
    }

    private Junction handleJunctionCriteriaQuery( Query query, Junction queryJunction )
    {
        Iterator<org.hisp.dhis.query.Criterion> iterator = queryJunction.getCriterions().iterator();
        Junction criteriaJunction = Disjunction.class.isInstance( queryJunction ) ?
            new Disjunction( query.getSchema() ) : new Conjunction( query.getSchema() );

        while ( iterator.hasNext() )
        {
            org.hisp.dhis.query.Criterion criterion = iterator.next();

            if ( Junction.class.isInstance( criterion ) )
            {
                Junction junction = handleJunctionCriteriaQuery( query, (Junction) criterion );

                if ( !junction.getCriterions().isEmpty() )
                {
                    criteriaJunction.add( junction );
                }

                if ( ((Junction) criterion).getCriterions().isEmpty() )
                {
                    iterator.remove();
                }
            }
            else if ( Restriction.class.isInstance( criterion ) )
            {
                Restriction restriction = (Restriction) criterion;

                if ( !restriction.getPath().contains( "\\." ) )
                {
                    if ( query.getSchema().haveProperty( restriction.getPath() ) )
                    {
                        Property property = query.getSchema().getProperty( restriction.getPath() );

                        if ( property.isSimple() && property.isPersisted() )
                        {
                            criteriaJunction.getCriterions().add( criterion );
                            iterator.remove();
                        }
                    }
                }
            }
        }

        return criteriaJunction;
    }
}
