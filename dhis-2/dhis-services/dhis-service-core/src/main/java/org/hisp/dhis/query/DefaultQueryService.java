package org.hisp.dhis.query;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.IdentifiableObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Default implementation of QueryService which works with IdObjects.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class DefaultQueryService
    implements QueryService
{
    private static final Log log = LogFactory.getLog( DefaultQueryService.class );

    @Autowired
    private QueryParser queryParser;

    @Autowired
    private CriteriaQueryEngine<? extends IdentifiableObject> criteriaQueryEngine;

    @Autowired
    private InMemoryQueryEngine<? extends IdentifiableObject> inMemoryQueryEngine;

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
    public int count( Query query )
    {
        query.setFirstResult( 0 );
        query.setMaxResults( Integer.MAX_VALUE );

        return queryObjects( query ).size();
    }

    @Override
    public Query getQueryFromUrl( Class<?> klass, List<String> filters, List<Order> orders ) throws QueryParserException
    {
        return getQueryFromUrl( klass, filters, orders, Junction.Type.AND );
    }

    @Override
    public Query getQueryFromUrl( Class<?> klass, List<String> filters, List<Order> orders, Junction.Type rootJunction ) throws QueryParserException
    {
        Query query = queryParser.parse( klass, filters, rootJunction );
        query.addOrders( orders );

        return query;
    }

    //---------------------------------------------------------------------------------------------
    // Helper methods
    //---------------------------------------------------------------------------------------------

    private List<? extends IdentifiableObject> queryObjects( Query query )
    {
        List<? extends IdentifiableObject> objects = query.getObjects();

        if ( objects == null )
        {
            objects = criteriaQueryEngine.query( query );

            if ( query.isEmpty() )
            {
                return objects;
            }
        }

        if ( log.isDebugEnabled() )
        {
            log.debug( "Doing in-memory for " + query.getCriterions().size() + " criterions and " + query.getOrders().size() + " orders." );
        }

        query.setObjects( objects );

        return inMemoryQueryEngine.query( query );
    }
}
