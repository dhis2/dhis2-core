package org.hisp.dhis.query;

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

import com.google.common.collect.Lists;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.PagerUtils;
import org.hisp.dhis.hibernate.HibernateUtils;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class InMemoryQueryEngine<T extends IdentifiableObject>
    implements QueryEngine<T>
{
    private final SchemaService schemaService;

    @Autowired
    public InMemoryQueryEngine( SchemaService schemaService )
    {
        this.schemaService = schemaService;
    }

    @Override
    public List<T> query( Query query )
    {
        validateQuery( query );
        List<T> list = runQuery( query );
        list = runSorter( query, list );

        return PagerUtils.pageCollection( list, query.getFirstResult(), query.getMaxResults() );
    }

    @Override
    public int count( Query query )
    {
        validateQuery( query );
        List<T> list = runQuery( query );

        return list.size();
    }

    private void validateQuery( Query query )
    {
        if ( query.getSchema() == null )
        {
            throw new QueryException( "Invalid Query object, does not contain Schema" );
        }

        if ( query.getObjects() == null )
        {
            throw new QueryException( "InMemoryQueryEngine requires an existing object list to work on." );
        }
    }

    @SuppressWarnings( "unchecked" )
    private List<T> runQuery( Query query )
    {
        return query.getObjects().stream()
            .filter( object -> test( query, (T) object ) )
            .map( object -> (T) object )
            .collect( Collectors.toList() );
    }

    private List<T> runSorter( Query query, List<T> objects )
    {
        List<T> sorted = new ArrayList<>( objects );

        sorted.sort( ( o1, o2 ) ->
        {
            for ( Order order : query.getOrders() )
            {
                int result = order.compare( o1, o2 );
                if ( result != 0 ) return result;
            }

            return 0;
        } );

        return sorted;
    }

    private boolean test( Query query, T object )
    {
        List<Boolean> testResults = new ArrayList<>();

        for ( Criterion criterion : query.getCriterions() )
        {
            Boolean testResult = false;

            // normal Restriction, just assume Conjunction
            if ( Restriction.class.isInstance( criterion ) )
            {
                Restriction restriction = (Restriction) criterion;
                testResult = testAnd( query, object, Lists.newArrayList( restriction ) );
            }
            else if ( Conjunction.class.isInstance( criterion ) )
            {
                Conjunction conjunction = (Conjunction) criterion;
                testResult = testAnd( query, object, conjunction.getCriterions() );
            }
            else if ( Disjunction.class.isInstance( criterion ) )
            {
                Disjunction disjunction = (Disjunction) criterion;
                testResult = testOr( query, object, disjunction.getCriterions() );
            }

            testResults.add( testResult );
        }

        if ( query.getRootJunctionType() == Junction.Type.OR )
        {
            return testResults.contains( Boolean.TRUE );
        }

        return !testResults.contains( Boolean.FALSE );
    }

    private boolean testAnd( Query query, T object, List<Criterion> criterions )
    {
        for ( Criterion criterion : criterions )
        {
            if ( Restriction.class.isInstance( criterion ) )
            {
                Restriction restriction = (Restriction) criterion;
                Object value = getValue( query, object, restriction.getPath() );

                if ( !Collection.class.isInstance( value ) )
                {
                    if ( !restriction.getOperator().test( value ) )
                    {
                        return false;
                    }
                }
                else
                {
                    Collection<?> collection = (Collection<?>) value;

                    for ( Object item : collection )
                    {
                        if ( restriction.getOperator().test( item ) )
                        {
                            return true;
                        }
                    }

                    return false;
                }
            }
        }

        return true;
    }

    private boolean testOr( Query query, T object, List<Criterion> criterions )
    {
        for ( Criterion criterion : criterions )
        {
            if ( Restriction.class.isInstance( criterion ) )
            {
                Restriction restriction = (Restriction) criterion;
                Object value = getValue( query, object, restriction.getPath() );

                if ( !Collection.class.isInstance( value ) )
                {
                    if ( restriction.getOperator().test( value ) )
                    {
                        return true;
                    }
                }
                else
                {
                    Collection<?> collection = (Collection<?>) value;

                    for ( Object item : collection )
                    {
                        if ( restriction.getOperator().test( item ) )
                        {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private Object getValue( Query query, Object object, String path )
    {
        String[] paths = path.split( "\\." );
        Schema currentSchema = query.getSchema();

        for ( int i = 0; i < paths.length; i++ )
        {
            Property property = currentSchema.getProperty( paths[i] );

            if ( property == null )
            {
                throw new QueryException( "No property found for path " + path );
            }

            if ( property.isCollection() )
            {
                currentSchema = schemaService.getDynamicSchema( property.getItemKlass() );
            }
            else
            {
                currentSchema = schemaService.getDynamicSchema( property.getKlass() );
            }

            object = collect( object, property );

            if ( i == (paths.length - 1) )
            {
                if ( property.isCollection() )
                {
                    return Lists.newArrayList( object );
                }

                return object;
            }

        }

        throw new QueryException( "No values found for path " + path );
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    private Object collect( Object object, Property property )
    {
        object = HibernateUtils.unwrap( object );

        if ( Collection.class.isInstance( object ) )
        {
            Collection<?> collection = (Collection<?>) object;
            List<Object> items = new ArrayList<>();

            for ( Object item : collection )
            {
                Object collect = collect( item, property );

                if ( Collection.class.isInstance( collect ) )
                {
                    items.addAll( ((Collection) collect) );
                }
                else
                {
                    items.add( collect );
                }
            }

            return items;
        }

        return ReflectionUtils.invokeMethod( object, property.getGetterMethod() );
    }
}
