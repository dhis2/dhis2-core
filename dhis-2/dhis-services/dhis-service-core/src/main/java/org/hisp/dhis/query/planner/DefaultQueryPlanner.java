package org.hisp.dhis.query.planner;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.hisp.dhis.query.Conjunction;
import org.hisp.dhis.query.Criterion;
import org.hisp.dhis.query.Disjunction;
import org.hisp.dhis.query.Junction;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.Restriction;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class DefaultQueryPlanner implements QueryPlanner
{
    private final SchemaService schemaService;

    public DefaultQueryPlanner( SchemaService schemaService )
    {
        this.schemaService = schemaService;
    }

    @Override
    public QueryPlan planQuery( Query query )
    {
        return planQuery( query, false );
    }

    @Override
    public QueryPlan planQuery( Query query, boolean persistedOnly )
    {
        if ( Junction.Type.OR == query.getRootJunctionType() && !persistedOnly )
        {
            return new QueryPlan(
                Query.from( query.getSchema() ).setPlannedQuery( true ),
                Query.from( query ).setPlannedQuery( true )
            );
        }

        Query npQuery = Query.from( query ).setPlannedQuery( true );
        Query pQuery = getQuery( npQuery, persistedOnly )
            .setUser( query.getUser() ).setPlannedQuery( true );

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

    @Override
    public QueryPath getQueryPath( Schema schema, String path )
    {
        Schema curSchema = schema;
        Property curProperty = null;
        boolean persisted = true;
        List<String> alias = new ArrayList<>();
        String[] pathComponents = path.split( "\\." );

        if ( pathComponents.length == 0 )
        {
            return null;
        }

        for ( int idx = 0; idx < pathComponents.length; idx++ )
        {
            String name = pathComponents[idx];
            curProperty = curSchema.getProperty( name );

            if ( curProperty == null )
            {
                throw new RuntimeException( "Invalid path property: " + name );
            }

            if ( !curProperty.isPersisted() )
            {
                persisted = false;
            }

            if ( (!curProperty.isSimple() && idx == pathComponents.length - 1) )
            {
                return new QueryPath( curProperty, persisted, alias.toArray( new String[]{} ) );
            }

            if ( curProperty.isCollection() )
            {
                curSchema = schemaService.getDynamicSchema( curProperty.getItemKlass() );
                alias.add( curProperty.getFieldName() );
            }
            else if ( !curProperty.isSimple() )
            {
                curSchema = schemaService.getDynamicSchema( curProperty.getKlass() );
                alias.add( curProperty.getFieldName() );
            }
            else
            {
                return new QueryPath( curProperty, persisted, alias.toArray( new String[]{} ) );
            }
        }

        return new QueryPath( curProperty, persisted, alias.toArray( new String[]{} ) );
    }

    /**
     * @param query Query
     * @return Query instance
     */
    private Query getQuery( Query query, boolean persistedOnly )
    {
        Query pQuery = Query.from( query.getSchema(), query.getRootJunctionType() );
        Iterator<Criterion> iterator = query.getCriterions().iterator();

        while ( iterator.hasNext() )
        {
            org.hisp.dhis.query.Criterion criterion = iterator.next();

            if ( Junction.class.isInstance( criterion ) )
            {
                Junction junction = handleJunction( pQuery, (Junction) criterion, persistedOnly );

                if ( !junction.getCriterions().isEmpty() )
                {
                    pQuery.getAliases().addAll( junction.getAliases() );
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
                restriction.setQueryPath( getQueryPath( query.getSchema(), restriction.getPath() ) );

                if ( restriction.getQueryPath().isPersisted() && !restriction.getQueryPath().haveAlias() )
                {
                    pQuery.getAliases().addAll( Arrays.asList( ((Restriction) criterion).getQueryPath().getAlias() ) );
                    pQuery.getCriterions().add( criterion );
                    iterator.remove();
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

    private Junction handleJunction( Query query, Junction queryJunction, boolean persistedOnly )
    {
        Iterator<org.hisp.dhis.query.Criterion> iterator = queryJunction.getCriterions().iterator();
        Junction criteriaJunction = Disjunction.class.isInstance( queryJunction ) ?
            new Disjunction( query.getSchema() ) : new Conjunction( query.getSchema() );

        while ( iterator.hasNext() )
        {
            org.hisp.dhis.query.Criterion criterion = iterator.next();

            if ( Junction.class.isInstance( criterion ) )
            {
                Junction junction = handleJunction( query, (Junction) criterion, persistedOnly );

                if ( !junction.getCriterions().isEmpty() )
                {
                    criteriaJunction.getAliases().addAll( junction.getAliases() );
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
                restriction.setQueryPath( getQueryPath( query.getSchema(), restriction.getPath() ) );

                if ( restriction.getQueryPath().isPersisted() && !restriction.getQueryPath().haveAlias( 1 ) )
                {
                    criteriaJunction.getAliases().addAll( Arrays.asList( ((Restriction) criterion).getQueryPath().getAlias() ) );
                    criteriaJunction.getCriterions().add( criterion );
                    iterator.remove();
                }
                else if ( persistedOnly )
                {
                    throw new RuntimeException( "Path " + restriction.getQueryPath().getPath() +
                        " is not fully persisted, unable to build persisted only query plan." );
                }
            }
        }

        return criteriaJunction;
    }
}
