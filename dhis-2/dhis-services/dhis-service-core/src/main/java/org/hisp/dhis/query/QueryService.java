package org.hisp.dhis.query;

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

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.query.Junction.Type;

import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface QueryService
{
    /**
     * Return objects matching given query, T typed according to QueryEngine
     * implementation.
     *
     * @param query Query instance to use
     * @return Matching objects
     */
    List<? extends IdentifiableObject> query( Query query );

    /**
     * Return objects matching given query, T typed according to QueryEngine
     * implementation.
     *
     * @param query       Query instance to use
     * @param transformer ResultTransformer to use for mutating the result
     * @return Matching objects
     */
    @SuppressWarnings( "rawtypes" )
    List<? extends IdentifiableObject> query( Query query, ResultTransformer transformer );

    /**
     * Returns how many objects matches the given query.
     *
     * @param query Query instance to use
     * @return N number of matching objects
     */
    int count( Query query );

    /**
     * Create a query instance from a given set of filters (property:operator:value), and
     * a list of orders.
     *
     * @param klass        Type of object you want to query
     * @param filters      List of filters to use as basis for query instance
     * @param orders       List of orders to use for query
     * @param rootJunction Root junction (defaults to AND)
     * @return New query instance using provided filters/orders
     */
    Query getQueryFromUrl(Class<?> klass, List<String> filters, List<Order> orders, Pagination pagination, Junction.Type rootJunction ) throws QueryParserException;

    Query getQueryFromUrl( Class<?> klass, List<String> filters, List<Order> orders, Pagination pagination) throws QueryParserException;

    Query getQueryFromUrl( Class<?> klass, List<String> filters, List<Order> orders ) throws QueryParserException;

    Query getQueryFromUrl( Class<?> klass, List<String> filters, List<Order> orders, Pagination pagination, Type rootJunction, boolean restrictToCaptureScope )
        throws QueryParserException;
}
